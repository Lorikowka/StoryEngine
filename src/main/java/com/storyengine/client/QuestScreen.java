package com.storyengine.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.storyengine.StoryEngineMod;
import com.storyengine.network.QuestClientState;
import com.storyengine.quest.BlockBreakQuestTask;
import com.storyengine.quest.ItemQuestTask;
import com.storyengine.quest.KillEntityQuestTask;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Журнал квестов в виде компактного "дневника": центрированное окно
 * фиксированного размера 520x340 (не растягивается на весь экран),
 * вкладки со статус-иконками, список квестов с автором, панель деталей
 * с увеличенным заголовком и цветовым статусом.
 *
 * Визуальные решения:
 * - каскадные анимации появления (общий computeAnim, easeOutCubic,
 *   мягкая альфа от 0.35, кап каскада на 10 элементах, ре-анимация
 *   списка при переключении вкладки);
 * - "пилюли"-подложки под строками задач;
 * - обрезка длинных текстов многоточием (без бегущих строк);
 * - пустые состояния для каждой вкладки;
 * - высота панели деталей замеряется тем же кодом, который её рисует
 *   (dryRun-проход) - скроллбар не может разойтись с рендером.
 */
public class QuestScreen extends Screen {

    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/quest_menu.png");
    private static final ResourceLocation WIDGETS_TEXTURE =
            new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/quest_widgets.png");
    private static final ResourceLocation TITLE_ICON_TEXTURE =
            new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/quest_icon.png");
    private static final ResourceLocation STATUS_ACTIVE_ICON =
            new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/status_active.png");
    private static final ResourceLocation STATUS_COMPLETED_ICON =
            new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/status_completed.png");
    private static final ResourceLocation STATUS_FAILED_ICON =
            new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/status_failed.png");
    private static final int TEXTURE_SIZE = 256;

    /** Null-безопасный порядок сортировки по названию: квест с битым JSON
     * (без "title") не должен ронять экран NullPointerException'ом. */
    private static final Comparator<QuestData> QUEST_ORDER =
            Comparator.comparing(QuestData::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    // Габариты окна (в GUI-пикселях).
    private static final int WIN_W = 520;
    private static final int WIN_H = 340;
    private static final int INSET = 8;

    private static final int CLOSE_SIZE = 20;
    private static final int SCROLLBAR_WIDTH = 3;

    // Список слева.
    private static final int LIST_X = 12;
    private static final int LIST_Y = 64;
    private static final int LIST_W = 166;
    private static final int LIST_BOTTOM_INSET = 12;
    private static final int ROW_HEIGHT = 30;

    // Детали справа.
    private static final int DETAILS_X = 198;
    private static final int DETAILS_GAP_TOP = 54;
    private static final int DETAILS_BOTTOM_INSET = 12;

    // Вкладки.
    private static final int TAB_Y = 30;
    private static final int TAB_H = 24;
    private static final int TAB_W = 150;
    private static final int TAB_GAP = 8;
    private static final int TAB_ICON_SIZE = 12;

    // Анимации.
    private static final long ANIM_DURATION_MS = 400;
    private static final long TAB_ANIM_STAGGER_MS = 50;
    private static final int TAB_ANIM_SLIDE_PX = 14;
    private static final long LIST_ANIM_STAGGER_MS = 35;
    private static final int LIST_ANIM_SLIDE_PX = 12;
    /** Каскад не дольше чем на 10 элементах - хвост списка появляется сразу. */
    private static final int LIST_ANIM_STAGGER_CAP = 10;
    /** Начальная непрозрачность анимированных элементов - они не исчезают полностью. */
    private static final float ANIM_MIN_ALPHA = 0.35F;

    private static final String STRIKETHROUGH = "\u00a7m";

    private QuestData selectedQuest;
    private QuestTab currentTab = QuestTab.ACTIVE;
    private final Map<String, Boolean> expandedDescriptions = new HashMap<>();

    private final long openedAt = Util.getMillis();
    private long listAnimStart = openedAt;
    private final boolean prevHideGui;

    private int winLeft;
    private int winTop;
    private int listScroll;
    private int detailsScroll;

    public QuestScreen() {
        super(Component.literal("Квесты"));
        // Прячем хотбар/крестик прицела, пока открыт журнал квестов - для
        // ощущения полноэкранного "дневника". Возвращаем как было в removed().
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
        this.winLeft = (this.width - WIN_W) / 2;
        this.winTop = (this.height - WIN_H) / 2;

        this.addRenderableWidget(new PlateButton(
                this.winLeft + WIN_W - CLOSE_SIZE - 6, this.winTop + 6, CLOSE_SIZE, CLOSE_SIZE,
                Component.literal("×"), button -> this.onClose(),
                0, 0, 20, 0, null, null, 20, 20
        ));
    }

    private void switchTab(QuestTab tab) {
        if (this.currentTab == tab) {
            return;
        }
        this.currentTab = tab;
        this.listScroll = 0;
        this.detailsScroll = 0;
        // Ре-анимация списка при смене вкладки - переключение ощущается живым.
        this.listAnimStart = Util.getMillis();
        selectFirstVisibleQuest();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        drawWindow(poseStack);
        drawTitle(poseStack);
        drawTabs(poseStack, mouseX, mouseY);
        drawQuestList(poseStack, mouseX, mouseY);
        drawQuestDetails(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void drawWindow(PoseStack poseStack) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        blit(poseStack, this.winLeft, this.winTop, WIN_W, WIN_H,
                0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

        GuiComponent.fill(poseStack,
                this.winLeft + INSET, this.winTop + INSET,
                this.winLeft + WIN_W - INSET, this.winTop + WIN_H - INSET,
                0xCC202020);
    }

    private void drawTitle(PoseStack poseStack) {
        String title = "Квесты";
        int x = this.winLeft + (WIN_W - this.font.width(title)) / 2;
        this.font.drawShadow(poseStack, title, x, this.winTop + 9, 0xFFFFFF);
    }

    private void drawTabs(PoseStack poseStack, int mouseX, int mouseY) {
        int tabX = this.winLeft + (WIN_W - (TAB_W * 3 + TAB_GAP * 2)) / 2;
        int tabY = this.winTop + TAB_Y;
        long elapsed = Util.getMillis() - this.openedAt;

        QuestTab[] tabs = QuestTab.values();
        for (int i = 0; i < tabs.length; i++) {
            QuestTab tab = tabs[i];
            AnimState anim = computeAnim(elapsed, i, TAB_ANIM_STAGGER_MS, TAB_ANIM_SLIDE_PX);
            int x = tabX + i * (TAB_W + TAB_GAP);
            int y = tabY - anim.offsetY;
            boolean hovered = mouseX >= x && mouseX <= x + TAB_W && mouseY >= y && mouseY <= y + TAB_H;

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, anim.alpha);
            RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
            int u = tab == currentTab ? 160 : (hovered ? 80 : 0);
            blit(poseStack, x, y, TAB_W, TAB_H, u, 20, 80, 24, TEXTURE_SIZE, TEXTURE_SIZE);

            // Иконка статуса слева от подписи.
            RenderSystem.setShaderTexture(0, tab.icon());
            blit(poseStack, x + 10, y + (TAB_H - TAB_ICON_SIZE) / 2, TAB_ICON_SIZE, TAB_ICON_SIZE,
                    0, 0, TAB_ICON_SIZE, TAB_ICON_SIZE, TAB_ICON_SIZE, TAB_ICON_SIZE);

            int textColor = tab == currentTab ? 0xFFF6D57A : (hovered ? 0xFFFFFF : 0xA0A0A0);
            int labelX = x + 26 + (TAB_W - 32 - this.font.width(tab.title)) / 2;
            int labelY = y + (TAB_H - 8) / 2;
            this.font.drawShadow(poseStack, tab.title, labelX, labelY, withAlpha(textColor, anim.alpha));
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private int listViewportHeight() {
        return WIN_H - LIST_Y - LIST_BOTTOM_INSET;
    }

    private void drawQuestList(PoseStack poseStack, int mouseX, int mouseY) {
        int x = this.winLeft + LIST_X;
        int y = this.winTop + LIST_Y;
        int viewH = listViewportHeight();

        List<QuestData> filtered = getFilteredQuests();
        int contentHeight = filtered.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, contentHeight - viewH);
        listScroll = clamp(listScroll, 0, maxScroll);

        GuiComponent.enableScissor(x, y, x + LIST_W, y + viewH);
        long elapsed = Util.getMillis() - this.listAnimStart;

        for (int i = 0; i < filtered.size(); i++) {
            QuestData quest = filtered.get(i);
            int rowY = y + i * ROW_HEIGHT - listScroll;
            if (rowY > y + viewH || rowY + ROW_HEIGHT < y) {
                continue;
            }

            int staggerIndex = Math.min(i, LIST_ANIM_STAGGER_CAP);
            AnimState anim = computeAnim(elapsed, staggerIndex, LIST_ANIM_STAGGER_MS, LIST_ANIM_SLIDE_PX);
            int animatedY = rowY - anim.offsetY;

            boolean selected = selectedQuest != null && Objects.equals(selectedQuest.getId(), quest.getId());
            boolean hovered = !selected
                    && mouseX >= x && mouseX <= x + LIST_W
                    && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2;

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, anim.alpha);
            RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
            int u = selected ? 64 : (hovered ? 32 : 0);
            blit(poseStack, x + 2, animatedY, LIST_W - 4, ROW_HEIGHT - 2, u, 44, 32, 22, TEXTURE_SIZE, TEXTURE_SIZE);

            int textColor = withAlpha(0xFFFFFF, anim.alpha);
            int authorColor = withAlpha(0x909090, anim.alpha);
            this.font.drawShadow(poseStack, truncate(quest.getTitle(), LIST_W - 34), x + 26, animatedY + 5, textColor);
            String author = quest.getAuthor();
            if (author != null && !author.isBlank()) {
                this.font.drawShadow(poseStack, truncate(author, LIST_W - 34), x + 26, animatedY + 17, authorColor);
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        GuiComponent.disableScissor();

        if (filtered.isEmpty()) {
            String msg = currentTab.emptyMessage();
            int msgX = x + (LIST_W - this.font.width(msg)) / 2;
            int msgY = y + (viewH - 9) / 2;
            this.font.drawShadow(poseStack, msg, msgX, msgY, 0x888888);
        } else if (maxScroll > 0) {
            drawScrollbar(poseStack, x + LIST_W - SCROLLBAR_WIDTH - 1, y, viewH, maxScroll, listScroll);
        }
    }

    private void drawQuestDetails(PoseStack poseStack) {
        if (selectedQuest == null) {
            return;
        }
        QuestData quest = selectedQuest;
        int dx = this.winLeft + DETAILS_X;
        int dy = this.winTop + LIST_Y;
        int dw = this.winLeft + WIN_W - 12 - dx;

        // Заголовок: иконка + текст в масштабе 1.15x, блок центрируется как целое.
        float titleScale = 1.15F;
        int iconSize = 16;
        String titleText = truncate(quest.getTitle(), (int) ((dw - iconSize - 8) / titleScale));
        int titleTextW = (int) (this.font.width(titleText) * titleScale);
        int blockW = iconSize + 6 + titleTextW;
        int blockX = dx + (dw - blockW) / 2;
        int titleY = dy - 2;

        RenderSystem.setShaderTexture(0, TITLE_ICON_TEXTURE);
        blit(poseStack, blockX, titleY + 1, iconSize, iconSize, 0, 0, 18, 18, 18, 18);

        poseStack.pushPose();
        poseStack.translate(blockX + iconSize + 6, titleY + 2, 0);
        poseStack.scale(titleScale, titleScale, 1.0F);
        this.font.drawShadow(poseStack, titleText, 0, 0, 0xFFFFFF);
        poseStack.popPose();

        // Автор и статус.
        int infoY = dy + 22;
        String author = quest.getAuthor();
        if (author != null && !author.isBlank()) {
            this.font.drawShadow(poseStack, "Автор:", dx, infoY, 0xAAAAAA);
            this.font.drawShadow(poseStack, truncate(author, dw - 50), dx + this.font.width("Автор:") + 4, infoY, 0xFFFFFF);
        }
        QuestStatus st = QuestClientState.getStatuses().getOrDefault(quest.getId(), QuestStatus.NOT_STARTED);
        this.font.drawShadow(poseStack, "Статус:", dx, infoY + 12, 0xAAAAAA);
        this.font.drawShadow(poseStack, st.displayName(), dx + this.font.width("Статус:") + 4, infoY + 12, st.displayColor());

        // Тело панели: тот же метод считает высоту (dryRun) и рисует контент.
        int bodyTop = dy + DETAILS_GAP_TOP;
        int bodyBottom = this.winTop + WIN_H - DETAILS_BOTTOM_INSET;
        int bodyH = bodyBottom - bodyTop;
        int totalH = renderDetailsBody(null, quest, st, dx, dw, bodyTop, 0, true);
        int maxScroll = Math.max(0, totalH - bodyH);
        detailsScroll = clamp(detailsScroll, 0, maxScroll);

        GuiComponent.enableScissor(dx, bodyTop, dx + dw, bodyBottom);
        renderDetailsBody(poseStack, quest, st, dx, dw, bodyTop, detailsScroll, false);
        GuiComponent.disableScissor();

        if (maxScroll > 0) {
            drawScrollbar(poseStack, dx + dw - SCROLLBAR_WIDTH, bodyTop, bodyH, maxScroll, detailsScroll);
        }
    }

    /**
     * Рисует тело панели деталей. При dryRun=true ничего не рисует и только
     * возвращает итоговую высоту - замер идёт тем же кодом, что и отрисовка,
     * поэтому скроллбар всегда совпадает с реальным содержимым.
     */
    private int renderDetailsBody(PoseStack poseStack, QuestData quest, QuestStatus st,
                                 int dx, int dw, int startY, int scrollOffset, boolean dryRun) {
        int y = startY - scrollOffset;
        List<QuestTask> tasks = quest.getTasks();

        if (!tasks.isEmpty()) {
            y = drawLabeledBlock(poseStack, "Цели:", dx, dw, y, dryRun);
            for (QuestTask task : tasks) {
                boolean completed = isTaskCompleted(quest.getId(), task.getId());
                int amount = taskAmount(task);
                int progress = getTaskProgress(quest.getId(), task.getId());

                String prefix;
                int color;
                if (st == QuestStatus.FAILED) {
                    prefix = "";
                    color = 0xFF5555;
                } else if (completed) {
                    prefix = STRIKETHROUGH;
                    color = 0x55FF55;
                } else {
                    prefix = "";
                    color = 0xDDDDDD;
                }

                StringBuilder line = new StringBuilder(prefix)
                        .append(completed ? "[x] " : "[ ] ")
                        .append(task.getTitle());
                if (amount > 0) {
                    line.append(" (").append(Math.min(progress, amount)).append("/").append(amount).append(")");
                }

                y = drawWrappedLineWithPill(poseStack, line.toString(), dx + 4, y, dw - 20, color, dryRun);
                y += 2;
                if (Boolean.TRUE.equals(expandedDescriptions.get(task.getId()))
                        && task.getDescription() != null && !task.getDescription().isBlank()) {
                    y = drawWrappedText(poseStack, task.getDescription(), dx + 12, y, dw - 32, 0x999999, dryRun);
                    y += 2;
                }
                if (task instanceof LocationQuestTask locationTask) {
                    y = drawWrappedLineWithPill(poseStack, "→ " + formatLocation(locationTask),
                            dx + 12, y, dw - 32, 0x93C5FD, dryRun);
                    y += 2;
                }
                y += 4;
            }
            y += 6;
        }

        y = drawLabeledBlock(poseStack, "Описание:", dx, dw, y, dryRun);
        y = drawWrappedText(poseStack, quest.getDescription(), dx, y, dw - 12, 0xFFFFFF, dryRun);
        return y + 8;
    }

    /** Серый заголовок блока ("Цели:", "Описание:") с отбивкой снизу. */
    private int drawLabeledBlock(PoseStack poseStack, String label, int dx, int dw, int y, boolean dryRun) {
        if (!dryRun) {
            this.font.drawShadow(poseStack, label, dx, y, 0xAAAAAA);
        }
        return y + 14;
    }

    /** Одна строка с тёмной "пилюлей"-подложкой (перенос слов не выполняется). */
    private int drawWrappedLineWithPill(PoseStack poseStack, String text, int x, int y, int maxWidth, int color, boolean dryRun) {
        String clipped = truncatePlain(text, maxWidth);
        if (clipped.isEmpty()) {
            return y + 12;
        }
        if (!dryRun) {
            int w = this.font.width(clipped);
            GuiComponent.fill(poseStack, x - 3, y - 2, x + w + 5, y + 11, 0x26FFFFFF);
            this.font.drawShadow(poseStack, clipped, x, y, color);
        }
        return y + 12;
    }

    /** Перенос по словам; при dryRun только накапливает высоту. */
    private int drawWrappedText(PoseStack poseStack, String text, int x, int y, int width, int color, boolean dryRun) {
        if (text == null || text.isBlank() || width <= 0) {
            return y;
        }
        int lineHeight = 10;
        for (String rawLine : wrapByWords(text, width)) {
            if (!dryRun) {
                this.font.drawShadow(poseStack, rawLine, x, y, color);
            }
            y += lineHeight;
        }
        return y;
    }

    private List<String> wrapByWords(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (this.font.width(candidate) <= width) {
                current = new StringBuilder(candidate);
                continue;
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder();
            }
            if (this.font.width(word) > width) {
                // Слово шире строки - режем посимвольно.
                StringBuilder part = new StringBuilder();
                for (char c : word.toCharArray()) {
                    if (this.font.width(part.toString() + c) > width && !part.isEmpty()) {
                        lines.add(part.toString());
                        part = new StringBuilder();
                    }
                    part.append(c);
                }
                current = part;
            } else {
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    /** Обрезка с многоточием (учитывает §-коды префикса по фактической ширине). */
    private String truncate(String text, int maxWidth) {
        if (text == null || this.font.width(text) <= maxWidth) {
            return text == null ? "" : text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - this.font.width("..."))) + "...";
    }

    private String truncatePlain(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        return truncate(text, maxWidth);
    }

    private void drawScrollbar(PoseStack poseStack, int x, int y, int viewHeight, int maxScroll, int scroll) {
        GuiComponent.fill(poseStack, x, y, x + SCROLLBAR_WIDTH, y + viewHeight, 0x30FFFFFF);
        int trackH = viewHeight;
        int thumbHeight = Math.max(16, trackH * trackH / (trackH + maxScroll));
        int thumbY = y + (scroll * (trackH - thumbHeight)) / Math.max(1, maxScroll);
        GuiComponent.fill(poseStack, x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0x90FFFFFF);
    }

    private void selectFirstVisibleQuest() {
        List<QuestData> filtered = getFilteredQuests();
        selectedQuest = filtered.isEmpty() ? null : filtered.get(0);
    }

    private int taskAmount(QuestTask task) {
        if (task instanceof ItemQuestTask itemTask) {
            return itemTask.getCount();
        }
        if (task instanceof BlockBreakQuestTask blockTask) {
            return blockTask.getCount();
        }
        if (task instanceof KillEntityQuestTask killTask) {
            return killTask.getCount();
        }
        return -1;
    }

    private boolean isTaskCompleted(String questId, String taskId) {
        return QuestClientState.getCompletedTasks().getOrDefault(questId, List.of()).contains(taskId);
    }

    private int getTaskProgress(String questId, String taskId) {
        return QuestClientState.getTaskProgress().getOrDefault(questId, new HashMap<>()).getOrDefault(taskId, 0);
    }

    private String formatLocation(LocationQuestTask task) {
        return String.format("%s %.0f %.0f %.0f r%.0f",
                task.getDimension(), task.getX(), task.getY(), task.getZ(), task.getRadius());
    }

    private List<QuestData> getFilteredQuests() {
        return getQuestList().stream()
                .filter(quest -> currentTab.matches(quest))
                .collect(Collectors.toCollection(ArrayList::new));
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Переключение вкладок.
            int tabX = this.winLeft + (WIN_W - (TAB_W * 3 + TAB_GAP * 2)) / 2;
            int tabY = this.winTop + TAB_Y;
            if (mouseY >= tabY && mouseY <= tabY + TAB_H) {
                QuestTab[] tabs = QuestTab.values();
                for (int i = 0; i < tabs.length; i++) {
                    int x = tabX + i * (TAB_W + TAB_GAP);
                    if (mouseX >= x && mouseX <= x + TAB_W) {
                        switchTab(tabs[i]);
                        return true;
                    }
                }
            }

            // Выбор квеста в списке.
            int listX = this.winLeft + LIST_X;
            int listY = this.winTop + LIST_Y;
            int viewH = listViewportHeight();
            if (mouseX >= listX && mouseX <= listX + LIST_W && mouseY >= listY && mouseY <= listY + viewH) {
                List<QuestData> filtered = getFilteredQuests();
                int index = (int) ((mouseY - listY + listScroll) / ROW_HEIGHT);
                if (index >= 0 && index < filtered.size()) {
                    selectedQuest = filtered.get(index);
                    this.detailsScroll = 0;
                    return true;
                }
            }

            // Клик по задаче - развернуть/свернуть описание.
            if (selectedQuest != null) {
                Integer clickedIndex = hitTestDetailsBody(mouseX, mouseY, selectedQuest);
                if (clickedIndex != null && clickedIndex >= 0 && clickedIndex < selectedQuest.getTasks().size()) {
                    QuestTask task = selectedQuest.getTasks().get(clickedIndex);
                    expandedDescriptions.put(task.getId(),
                            !Boolean.TRUE.equals(expandedDescriptions.get(task.getId())));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Определяет индекс задачи из тела деталей по координатам мыши,
     * используя тот же dryRun-проход раскладки (строки задач помечаются
     * во время замера).
     */
    private Integer hitTestDetailsBody(double mouseX, double mouseY, QuestData quest) {
        int dx = this.winLeft + DETAILS_X;
        int dy = this.winTop + LIST_Y;
        int dw = this.winLeft + WIN_W - 12 - dx;
        int bodyTop = dy + DETAILS_GAP_TOP;
        int bodyBottom = this.winTop + WIN_H - DETAILS_BOTTOM_INSET;
        if (mouseX < dx || mouseX > dx + dw || mouseY < bodyTop || mouseY > bodyBottom) {
            return null;
        }

        List<QuestTask> tasks = quest.getTasks();
        int y = bodyTop - detailsScroll;
        y += 14; // "Цели:"
        for (int i = 0; i < tasks.size(); i++) {
            QuestTask task = tasks.get(i);
            int lineTop = y - 2;
            int lineBottom = y + 12;
            if (mouseY >= lineTop && mouseY <= lineBottom) {
                return i;
            }
            y += 12 + 2;
            if (Boolean.TRUE.equals(expandedDescriptions.get(task.getId()))
                    && task.getDescription() != null && !task.getDescription().isBlank()) {
                int lines = wrapByWords(task.getDescription(), dw - 32).size();
                y += lines * 10 + 2;
            }
            if (task instanceof LocationQuestTask) {
                y += 12 + 2;
            }
            y += 4;
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int listX = this.winLeft + LIST_X;
        int listY = this.winTop + LIST_Y;
        int viewH = listViewportHeight();
        if (mouseX >= listX && mouseX <= listX + LIST_W && mouseY >= listY && mouseY <= listY + viewH) {
            int contentHeight = getFilteredQuests().size() * ROW_HEIGHT;
            int maxScroll = Math.max(0, contentHeight - viewH);
            listScroll = clamp(listScroll - (int) (delta * ROW_HEIGHT * 1.5), 0, maxScroll);
            return true;
        }

        if (selectedQuest != null) {
            int dx = this.winLeft + DETAILS_X;
            int dw = this.winLeft + WIN_W - 12 - dx;
            int bodyTop = this.winTop + LIST_Y + DETAILS_GAP_TOP;
            int bodyBottom = this.winTop + WIN_H - DETAILS_BOTTOM_INSET;
            if (mouseX >= dx && mouseX <= dx + dw && mouseY >= bodyTop && mouseY <= bodyBottom) {
                QuestStatus st = QuestClientState.getStatuses()
                        .getOrDefault(selectedQuest.getId(), QuestStatus.NOT_STARTED);
                int bodyH = bodyBottom - bodyTop;
                int totalH = renderDetailsBody(null, selectedQuest, st, dx, dw, bodyTop, 0, true);
                int maxScroll = Math.max(0, totalH - bodyH);
                detailsScroll = clamp(detailsScroll - (int) (delta * ROW_HEIGHT * 1.2), 0, maxScroll);
                return true;
            }
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Math.round(255 * Math.max(0.0F, Math.min(1.0F, alpha)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static float easeOutCubic(float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        float inv = 1.0F - clamped;
        return 1.0F - inv * inv * inv;
    }

    /**
     * Каскадная анимация появления: альфа от ANIM_MIN_ALPHA до 1 и сдвиг
     * снизу вверх на slidePx, с задержкой index * staggerMs. Индекс выше
     * капа анимируется без дополнительной задержки.
     */
    private static AnimState computeAnim(long elapsedMs, int index, long staggerMs, int slidePx) {
        long delayed = elapsedMs - (long) Math.min(index, LIST_ANIM_STAGGER_CAP) * staggerMs;
        float t = delayed <= 0 ? 0.0F : Math.min(1.0F, (float) delayed / ANIM_DURATION_MS);
        float eased = easeOutCubic(t);
        float alpha = ANIM_MIN_ALPHA + (1.0F - ANIM_MIN_ALPHA) * eased;
        int offsetY = Math.round((1.0F - eased) * slidePx);
        return new AnimState(alpha, offsetY);
    }

    private static final class AnimState {
        final float alpha;
        final int offsetY;

        AnimState(float alpha, int offsetY) {
            this.alpha = alpha;
            this.offsetY = offsetY;
        }
    }

    private enum QuestTab {
        ACTIVE("Активные", QuestStatus.ACTIVE, STATUS_ACTIVE_ICON, "Нет активных квестов"),
        COMPLETED("Выполненные", QuestStatus.COMPLETED, STATUS_COMPLETED_ICON, "Нет выполненных квестов"),
        FAILED("Проваленные", QuestStatus.FAILED, STATUS_FAILED_ICON, "Нет проваленных квестов");

        private final String title;
        private final QuestStatus status;
        private final ResourceLocation icon;
        private final String emptyMessage;

        QuestTab(String title, QuestStatus status, ResourceLocation icon, String emptyMessage) {
            this.title = title;
            this.status = status;
            this.icon = icon;
            this.emptyMessage = emptyMessage;
        }

        private boolean matches(QuestData quest) {
            QuestStatus st = QuestClientState.getStatuses().getOrDefault(quest.getId(), QuestStatus.NOT_STARTED);
            return st == this.status;
        }

        private ResourceLocation icon() {
            return icon;
        }

        private String emptyMessage() {
            return emptyMessage;
        }
    }

    /**
     * Кнопка с текстурным фоном из quest_widgets.png вместо ванильного
     * серого 9-slice (используется для кнопки закрытия окна).
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
