package com.storyengine.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.storyengine.StoryEngineMod;
import com.storyengine.network.QuestClientState;
import com.storyengine.quest.ItemQuestTask;
import com.storyengine.quest.LocationQuestTask;
import com.storyengine.quest.QuestData;
import com.storyengine.quest.QuestStatus;
import com.storyengine.quest.QuestTask;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class QuestScreen extends Screen {

    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/quest_menu.png");
    private static final ResourceLocation WIDGETS_TEXTURE =
            new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/quest_widgets.png");
    private static final int TEXTURE_SIZE = 256;

    /**
     * Null-безопасный порядок сортировки по названию: квест с битым JSON
     * (без "title") не должен ронять экран NullPointerException'ом.
     */
    private static final Comparator<QuestData> QUEST_ORDER =
            Comparator.comparing(QuestData::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private static final int PANEL_X = 18;
    private static final int PANEL_Y = 18;

    private static final int CLOSE_SIZE = 20;
    private static final int TAB_HEIGHT = 24;
    private static final int TAB_WIDTH = 80;
    private static final int TAB_GAP = 6;
    private static final int ROW_HEIGHT = 22;
    private static final int LEFT_WIDTH = 180;
    private static final int DETAILS_X = 240;
    private static final int SCROLLBAR_WIDTH = 3;

    private static final long ROW_ANIM_STAGGER_MS = 25;
    private static final long ROW_ANIM_DURATION_MS = 180;
    private static final int ROW_ANIM_SLIDE_PX = 8;

    private QuestData selectedQuest;
    private QuestTab currentTab = QuestTab.ACTIVE;
    private final Map<String, Boolean> expandedDescriptions = new HashMap<>();
    private final Map<QuestTab, PlateButton> tabButtons = new EnumMap<>(QuestTab.class);

    private final long openedAt = Util.getMillis();
    private final boolean prevHideGui;

    private int listScroll;
    private int detailsScroll;

    public QuestScreen() {
        super(Component.literal("Квесты"));
        // Прячем хотбар/крестик прицела, пока открыт журнал квестов - для
        // ощущения полноэкранного "дневника", а не наложенного окна поверх
        // обычного HUD. Возвращаем как было в removed().
        this.prevHideGui = Minecraft.getInstance().options.hideGui;
        Minecraft.getInstance().options.hideGui = true;
    }

    @Override
    public void removed() {
        Minecraft.getInstance().options.hideGui = this.prevHideGui;
        super.removed();
    }

    @Override
    protected void init() {
        super.init();
        tabButtons.clear();

        int panelWidth = this.width - PANEL_X * 2;

        // Кнопка закрытия - привязана к правому верхнему углу ПАНЕЛИ,
        // а не экрана, поэтому не "уезжает" на широких мониторах.
        this.addRenderableWidget(new PlateButton(
                PANEL_X + panelWidth - CLOSE_SIZE - 8, PANEL_Y + 14, CLOSE_SIZE, CLOSE_SIZE,
                Component.literal("×"), button -> this.onClose(),
                0, 0, 20, 0, null, null, 20, 20
        ));

        // Вкладки - настоящие Button-виджеты с текстурными состояниями
        // normal/hover/selected, а не ручная отрисовка + отдельный hit-test.
        int tabX = PANEL_X + 12;
        int tabY = PANEL_Y + 44;
        for (QuestTab tab : QuestTab.values()) {
            int width = Math.max(TAB_WIDTH, this.font.width(tab.title) + 20);
            PlateButton tabButton = new PlateButton(
                    tabX, tabY, width, TAB_HEIGHT,
                    Component.literal(tab.title), button -> switchTab(tab),
                    0, 20, 80, 20, 160, 20, 80, 24
            );
            tabButton.setSelected(tab == currentTab);
            tabButtons.put(tab, tabButton);
            this.addRenderableWidget(tabButton);
            tabX += width + TAB_GAP;
        }
    }

    private void switchTab(QuestTab tab) {
        this.currentTab = tab;
        tabButtons.forEach((t, btn) -> btn.setSelected(t == tab));
        this.listScroll = 0;
        this.detailsScroll = 0;
        selectFirstVisibleQuest();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        drawPanel(poseStack, PANEL_X, PANEL_Y, this.width - PANEL_X * 2, this.height - PANEL_Y * 2);
        drawHeader(poseStack);
        drawQuestList(poseStack, mouseX, mouseY);
        drawQuestDetails(poseStack);
        // Кнопки (закрытие, вкладки) рендерятся последними, поверх панели.
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int listX = PANEL_X + 12;
            int listY = PANEL_Y + 72;
            int listHeight = this.height - 130;
            if (mouseX >= listX && mouseX <= listX + LEFT_WIDTH && mouseY >= listY && mouseY <= listY + listHeight) {
                List<QuestData> filtered = getFilteredQuests();
                int index = (int) ((mouseY - listY - 8 + listScroll) / ROW_HEIGHT);
                if (index >= 0 && index < filtered.size()) {
                    selectedQuest = filtered.get(index);
                    this.detailsScroll = 0;
                    return true;
                }
            }

            if (selectedQuest != null) {
                int x2 = DETAILS_X + 10;
                int y2 = PANEL_Y + 72 + 60 - detailsScroll;
                int width = this.width - DETAILS_X - 24;
                for (int i = 0; i < selectedQuest.getTasks().size(); i++) {
                    QuestTask task = selectedQuest.getTasks().get(i);
                    int taskY = y2 + i * 34;
                    int taskTitleX = x2;
                    int taskTitleY = taskY;
                    int taskTitleWidth = width - 30;
                    int taskTitleHeight = 12;
                    int toggleX = x2 + width - 34;
                    int toggleY = taskY;
                    int toggleWidth = 18;
                    int toggleHeight = 12;
                    boolean toggleHit = mouseX >= toggleX && mouseX <= toggleX + toggleWidth && mouseY >= toggleY && mouseY <= toggleY + toggleHeight;
                    boolean taskLineHit = mouseX >= taskTitleX && mouseX <= taskTitleX + taskTitleWidth && mouseY >= taskTitleY && mouseY <= taskTitleY + taskTitleHeight;
                    if (toggleHit || taskLineHit) {
                        expandedDescriptions.put(task.getId(), !Boolean.TRUE.equals(expandedDescriptions.get(task.getId())));
                        return true;
                    }
                }
            }
        }
        // Клики по кнопкам (закрытие, вкладки) обрабатываются здесь -
        // они зарегистрированы через addRenderableWidget в init().
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int listX = PANEL_X + 12;
        int listY = PANEL_Y + 72;
        int listHeight = this.height - 130;
        if (mouseX >= listX && mouseX <= listX + LEFT_WIDTH && mouseY >= listY && mouseY <= listY + listHeight) {
            int contentHeight = getFilteredQuests().size() * ROW_HEIGHT + 8;
            int maxScroll = Math.max(0, contentHeight - listHeight);
            listScroll = clamp(listScroll - (int) (delta * ROW_HEIGHT * 2), 0, maxScroll);
            return true;
        }

        int detailsY = PANEL_Y + 72;
        int detailsHeight = this.height - 130;
        if (selectedQuest != null && mouseX >= DETAILS_X && mouseX <= this.width - 24 && mouseY >= detailsY && mouseY <= detailsY + detailsHeight) {
            int width = this.width - DETAILS_X - 24;
            int contentHeight = measureDetailsHeight(selectedQuest, width);
            int maxScroll = Math.max(0, contentHeight - detailsHeight);
            detailsScroll = clamp(detailsScroll - (int) (delta * ROW_HEIGHT * 2), 0, maxScroll);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 257) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float easeOutCubic(float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        float inv = 1.0F - clamped;
        return 1.0F - inv * inv * inv;
    }

    private void drawPanel(PoseStack poseStack, int x, int y, int width, int height) {
        int inset = 8;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        // Текстура растягивается на всю панель одним вызовом - надёжнее,
        // чем нарезка на 9 частей вручную.
        blit(poseStack, x, y, width, height, 0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

        GuiComponent.fill(poseStack, x + inset, y + inset, x + width - inset, y + height - inset, 0xCC202020);
    }

    private void drawHeader(PoseStack poseStack) {
        this.font.draw(poseStack, Component.literal("Квесты"), PANEL_X + 24, PANEL_Y + 22, 0xFFFFFF);
    }

    private void drawQuestList(PoseStack poseStack, int mouseX, int mouseY) {
        int x = PANEL_X + 12;
        int y = PANEL_Y + 72;
        int width = LEFT_WIDTH;
        int height = this.height - 130;
        GuiComponent.fill(poseStack, x, y, x + width, y + height, 0xFF1A1A1A);

        List<QuestData> filtered = getFilteredQuests();
        int contentHeight = filtered.size() * ROW_HEIGHT + 8;
        int maxScroll = Math.max(0, contentHeight - height);
        listScroll = clamp(listScroll, 0, maxScroll);

        GuiComponent.enableScissor(x, y, x + width, y + height);
        long elapsed = Util.getMillis() - openedAt;
        for (int i = 0; i < filtered.size(); i++) {
            QuestData quest = filtered.get(i);
            int rowY = y + 8 + i * ROW_HEIGHT - listScroll;
            if (rowY > y + height) {
                break;
            }
            if (rowY < y - ROW_HEIGHT) {
                continue;
            }

            // Ступенчатая анимация появления строк (fade + сдвиг снизу вверх).
            float t = easeOutCubic((float) (elapsed - i * ROW_ANIM_STAGGER_MS) / ROW_ANIM_DURATION_MS);
            if (t <= 0.0F) {
                continue;
            }
            int animatedY = rowY + Math.round((1.0F - t) * ROW_ANIM_SLIDE_PX);

            boolean selected = selectedQuest != null && Objects.equals(selectedQuest.getId(), quest.getId());
            boolean hovered = !selected
                    && mouseX >= x + 4 && mouseX <= x + width - 4
                    && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2;

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, t);
            RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
            int u = selected ? 64 : (hovered ? 32 : 0);
            blit(poseStack, x + 4, animatedY, width - 8, ROW_HEIGHT - 2, u, 44, 32, 22, TEXTURE_SIZE, TEXTURE_SIZE);

            int textColor = withAlpha(0xFFFFFF, t);
            drawScrollableText(poseStack, quest.getTitle(), x + 10, animatedY + 6, x + width - 14, textColor);
        }
        GuiComponent.disableScissor();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        drawScrollbar(poseStack, x + width - SCROLLBAR_WIDTH - 2, y, height, contentHeight, listScroll);
    }

    private void drawQuestDetails(PoseStack poseStack) {
        if (selectedQuest == null) {
            return;
        }
        int x = DETAILS_X;
        int y = PANEL_Y + 72;
        int width = this.width - DETAILS_X - 24;
        int height = this.height - 130;
        GuiComponent.fill(poseStack, x, y, x + width, y + height, 0xFF1A1A1A);

        int contentHeight = measureDetailsHeight(selectedQuest, width);
        int maxScroll = Math.max(0, contentHeight - height);
        detailsScroll = clamp(detailsScroll, 0, maxScroll);

        GuiComponent.enableScissor(x, y, x + width, y + height);
        int oy = y - detailsScroll;

        drawScrollableText(poseStack, selectedQuest.getTitle(), x + 10, oy + 10, x + width - 10, 0xFFFFFF);

        int descriptionY = oy + 32;
        if (selectedQuest.getTasks().stream().anyMatch(task -> task instanceof LocationQuestTask)) {
            this.font.draw(poseStack, Component.literal("Маркер: идите к указанной точке"), x + 10, descriptionY, 0xFFDDAA00);
            descriptionY += 14;
        }
        drawWrappedText(poseStack, selectedQuest.getDescription(), x + 10, descriptionY, width - 20, 0xBBBBBB);
        int conditionY = descriptionY + getWrappedTextHeight(selectedQuest.getDescription(), width - 20) + 10;
        this.font.draw(poseStack, Component.literal("Условие: выполните все задачи"), x + 10, conditionY, 0xFFFFFF);
        int taskY = conditionY + 18;
        for (QuestTask task : selectedQuest.getTasks()) {
            boolean completed = isTaskCompleted(selectedQuest.getId(), task.getId());
            boolean expanded = Boolean.TRUE.equals(expandedDescriptions.get(task.getId()));
            this.font.draw(poseStack, Component.literal((completed ? "[x]" : "[ ]") + " " + task.getTitle()), x + 10, taskY, 0xFFFFFF);
            int toggleX = x + width - 34;
            int toggleY = taskY;
            GuiComponent.fill(poseStack, toggleX, toggleY, toggleX + 18, toggleY + 12, 0xFF2A2A2A);
            this.font.draw(poseStack, Component.literal(expanded ? "▼" : "▶"), toggleX + 4, toggleY + 1, 0xFF66CCFF);
            if (expanded) {
                drawWrappedText(poseStack, task.getDescription(), x + 10, taskY + 10, width - 20, 0xBBBBBB);
            }
            if (task instanceof LocationQuestTask locationTask) {
                this.font.draw(poseStack, Component.literal("→ " + formatLocation(locationTask)), x + 24, taskY + 10, 0xFF93C5FD);
            }
            if (task instanceof ItemQuestTask itemTask) {
                int current = getTaskProgress(selectedQuest.getId(), task.getId());
                this.font.draw(poseStack, Component.literal("Собрано: " + current + " / " + itemTask.getCount()), x + 24, taskY + 20, 0xFF90EE90);
            }
            taskY += 34;
        }
        GuiComponent.disableScissor();

        drawScrollbar(poseStack, x + width - SCROLLBAR_WIDTH, y, height, contentHeight, detailsScroll);
    }

    /**
     * Считает итоговую высоту содержимого панели описания (без отрисовки),
     * той же арифметикой, что и drawQuestDetails - чтобы знать, докуда можно
     * скроллить и какого размера рисовать бегунок скроллбара.
     */
    private int measureDetailsHeight(QuestData quest, int width) {
        int descriptionY = 32;
        if (quest.getTasks().stream().anyMatch(task -> task instanceof LocationQuestTask)) {
            descriptionY += 14;
        }
        int conditionY = descriptionY + getWrappedTextHeight(quest.getDescription(), width - 20) + 10;
        int taskY = conditionY + 18;
        return taskY + quest.getTasks().size() * 34 + 10;
    }

    /** Простой вертикальный скроллбар справа от панели - трек + пропорциональный бегунок. */
    private void drawScrollbar(PoseStack poseStack, int x, int y, int viewHeight, int contentHeight, int scroll) {
        if (contentHeight <= viewHeight) {
            return;
        }
        GuiComponent.fill(poseStack, x, y, x + SCROLLBAR_WIDTH, y + viewHeight, 0x30FFFFFF);

        int maxScroll = contentHeight - viewHeight;
        int thumbHeight = Math.max(16, viewHeight * viewHeight / contentHeight);
        int thumbY = y + (scroll * (viewHeight - thumbHeight)) / Math.max(1, maxScroll);
        GuiComponent.fill(poseStack, x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0x90FFFFFF);
    }

    private void selectFirstVisibleQuest() {
        List<QuestData> filtered = getFilteredQuests();
        selectedQuest = filtered.isEmpty() ? null : filtered.get(0);
    }

    private void drawWrappedText(PoseStack poseStack, String text, int x, int y, int width, int color) {
        if (text == null || text.isBlank()) {
            return;
        }
        List<String> lines = wrapText(text, width);
        int lineHeight = 9;
        for (int i = 0; i < lines.size(); i++) {
            this.font.draw(poseStack, Component.literal(lines.get(i)), x, y + i * lineHeight, color);
        }
    }

    /**
     * Прокручивающийся текст (marquee) для длинных названий квестов.
     * Обрезается по границам колонки через enableScissor/disableScissor,
     * иначе прокручиваемый текст просто вылезал бы за пределы своей области.
     * Рисуется в два прохода подряд (текущий + следующий виток), чтобы
     * прокрутка была бесшовной, без "прыжка" в конце цикла.
     */
    private void drawScrollableText(PoseStack poseStack, String text, int startX, int y, int endX, int color) {
        if (text == null || text.isBlank()) {
            return;
        }
        int maxWidth = Math.max(0, endX - startX);
        int textWidth = this.font.width(text);

        if (textWidth <= maxWidth) {
            this.font.draw(poseStack, Component.literal(text), startX, y, color);
            return;
        }

        int gap = 30;
        int loopWidth = textWidth + gap;
        int pixelsPerSecond = 25;
        long millis = Util.getMillis();
        int offset = (int) ((millis / 1000.0 * pixelsPerSecond) % loopWidth);

        GuiComponent.enableScissor(startX, y - 2, endX, y + this.font.lineHeight + 2);
        int drawX = startX - offset;
        this.font.draw(poseStack, Component.literal(text), drawX, y, color);
        this.font.draw(poseStack, Component.literal(text), drawX + loopWidth, y, color);
        GuiComponent.disableScissor();
    }

    private List<String> wrapText(String text, int width) {
        List<String> lines = new ArrayList<>();
        if (width <= 0) {
            return lines;
        }
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (this.font.width(candidate) <= width) {
                current = new StringBuilder(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private int getWrappedTextHeight(String text, int width) {
        return wrapText(text, width).size() * 9;
    }

    private List<QuestData> getFilteredQuests() {
        return getQuestList().stream()
                .filter(quest -> currentTab.matches(quest))
                .toList();
    }

    private List<QuestData> getQuestList() {
        List<QuestData> quests = QuestClientState.getQuests().values().stream()
                .sorted(QUEST_ORDER)
                .collect(Collectors.toCollection(ArrayList::new));
        if (selectedQuest != null && quests.stream().noneMatch(quest -> Objects.equals(quest.getId(), selectedQuest.getId()))) {
            selectedQuest = quests.isEmpty() ? null : quests.get(0);
        }
        return quests;
    }

    private boolean isTaskCompleted(String questId, String taskId) {
        return QuestClientState.getCompletedTasks().getOrDefault(questId, List.of()).contains(taskId);
    }

    private int getTaskProgress(String questId, String taskId) {
        return QuestClientState.getTaskProgress().getOrDefault(questId, new HashMap<>()).getOrDefault(taskId, 0);
    }

    private String formatLocation(LocationQuestTask task) {
        return String.format("dim=%s x=%.1f y=%.1f z=%.1f r=%.1f", task.getDimension(), task.getX(), task.getY(), task.getZ(), task.getRadius());
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Math.round(255 * Math.max(0.0F, Math.min(1.0F, alpha)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private enum QuestTab {
        ACTIVE("Активные"),
        COMPLETED("Завершенные"),
        FAILED("Проваленные");

        private final String title;

        QuestTab(String title) {
            this.title = title;
        }

        private boolean matches(QuestData quest) {
            QuestStatus status = QuestClientState.getStatuses().getOrDefault(quest.getId(), QuestStatus.NOT_STARTED);
            return switch (this) {
                case ACTIVE -> status == QuestStatus.ACTIVE;
                case COMPLETED -> status == QuestStatus.COMPLETED;
                case FAILED -> status == QuestStatus.FAILED;
            };
        }
    }

    /**
     * Кнопка с текстурным фоном из quest_widgets.png вместо ванильного
     * серого 9-slice. Поддерживает 2 состояния (normal/hover) или 3
     * (normal/hover/selected - для вкладок). Чтобы задать свою текстуру -
     * поменяй assets/story_engine/textures/gui/quest_widgets.png и/или
     * U/V-координаты состояний, переданные в конструктор.
     */
    private static final class PlateButton extends Button {

        private final int normalU;
        private final int normalV;
        private final int hoverU;
        private final int hoverV;
        private final Integer selectedU;
        private final Integer selectedV;
        private final int stateWidth;
        private final int stateHeight;
        private boolean selected;

        PlateButton(int x, int y, int width, int height, Component message, OnPress onPress,
                    int normalU, int normalV, int hoverU, int hoverV,
                    Integer selectedU, Integer selectedV, int stateWidth, int stateHeight) {
            super(x, y, width, height, message, onPress, Button.NO_TOOLTIP);
            this.normalU = normalU;
            this.normalV = normalV;
            this.hoverU = hoverU;
            this.hoverV = hoverV;
            this.selectedU = selectedU;
            this.selectedV = selectedV;
            this.stateWidth = stateWidth;
            this.stateHeight = stateHeight;
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            int u = normalU;
            int v = normalV;
            if (selected && selectedU != null) {
                u = selectedU;
                v = selectedV;
            } else if (this.isHovered) {
                u = hoverU;
                v = hoverV;
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
            blit(poseStack, this.x, this.y, this.width, this.height,
                    u, v, stateWidth, stateHeight, TEXTURE_SIZE, TEXTURE_SIZE);

            if (!this.getMessage().getString().isEmpty()) {
                int color = selected ? 0xFFF6D57A : (this.isHovered ? 0xFFFFFFFF : 0xFFE0E0E0);
                drawCenteredString(poseStack, Minecraft.getInstance().font, this.getMessage(),
                        this.x + this.width / 2, this.y + (this.height - 8) / 2, color);
            }
        }
    }
}
