package com.storyengine.client;

import com.storyengine.network.QuestNetworking;
import com.storyengine.network.dialogue.DialogueNetworking;
import com.storyengine.network.dialogue.DialogueNetworking.DialogueNodePayload;
import com.storyengine.network.dialogue.DialogueNetworking.ResponsePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Полноэкранный GUI диалога (v4 — без иконок). Открывается по
 * S2COpenDialoguePacket, обновляется по S2CUpdateDialoguePacket, закрывается
 * по S2CCloseDialoguePacket.
 *
 * Макет (см. спецификацию v4 §7):
 *  - мир продолжает рендериться (isPauseScreen() = false), без затемнения;
 *  - нижняя панель реплики во всю ширину экрана (barHeight px);
 *  - выступающая плашка имени спикера (внахлёст на 1px, акцентная полоса);
 *  - текст реплики с эффектом печатной машинки (собственный TypewriterEngine),
 *    вертикально центрируется внутри панели;
 *  - варианты ответа блоком слева-вверху, 3 состояния (idle/hover/disabled);
 *  - навигация мышью или цифрами 1–9.
 */
public class DialogueScreen extends Screen {

    private DialogueNodePayload payload;
    private final TypewriterEngine typewriter;
    private final List<ButtonRect> responseRects = new ArrayList<>();
    private boolean prevHideGui;

    private static class ButtonRect {
        final int x, y, w, h;
        final int index;
        final boolean available;

        ButtonRect(int x, int y, int w, int h, int index, boolean available) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.index = index;
            this.available = available;
        }
    }

    private DialogueScreen(DialogueNodePayload payload) {
        super(Component.literal("Dialogue"));
        this.payload = payload;
        this.typewriter = new TypewriterEngine(Math.max(0, MenuCustomizationConfig.dialogueTextSpeed()));
    }

    /** Открыть (или обновить, если окно уже открыто). */
    public static void open(DialogueNodePayload payload) {
        if (payload != null) {
            DialogueCameraController.setTarget(payload.npcPosition);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DialogueScreen screen) {
            screen.update(payload);
        } else {
            mc.setScreen(new DialogueScreen(payload));
        }
    }

    /** Заменить содержимое (переход к следующему узлу). */
    public static void update(DialogueNodePayload payload) {
        if (payload != null) {
            DialogueCameraController.setTarget(payload.npcPosition);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DialogueScreen screen) {
            screen.doUpdate(payload);
        } else {
            mc.setScreen(new DialogueScreen(payload));
        }
    }

    /** Закрыть окно. */
    public static void close() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DialogueScreen) {
            mc.setScreen(null);
        }
    }

    private void doUpdate(DialogueNodePayload payload) {
        this.payload = payload;
        this.typewriter.reset();
        this.responseRects.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.responseRects.clear();
        layoutResponses();
        // Скрываем HUD (в т.ч. хотбар) на время диалога; восстановим в onClose().
        if (this.minecraft != null) {
            this.prevHideGui = this.minecraft.options.hideGui;
            this.minecraft.options.hideGui = true;
        }
    }

    @Override
    public void onClose() {
        DialogueCameraController.clear();
        if (this.minecraft != null) {
            this.minecraft.options.hideGui = this.prevHideGui;
        }
        super.onClose();
    }

    private int barTop() {
        return height - Math.max(0, MenuCustomizationConfig.dialogueBarHeight());
    }

    private void layoutResponses() {
        if (payload == null) {
            return;
        }
        int bw = MenuCustomizationConfig.dialogueResponseBoxWidth();
        int bh = MenuCustomizationConfig.dialogueResponseBoxHeight();
        int gap = MenuCustomizationConfig.dialogueResponseRowGap();
        int x = MenuCustomizationConfig.dialogueResponseX();
        int y = MenuCustomizationConfig.dialogueResponseY();
        int count = payload.responses.size();
        for (int i = 0; i < count; i++) {
            ResponsePayload r = payload.responses.get(i);
            responseRects.add(new ButtonRect(x, y + i * (bh + gap), bw, bh, i, r.available));
        }
    }

    @Override
    public void tick() {
        typewriter.tick();
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (payload == null) {
            return;
        }

        int barTop = barTop();
        int barH = MenuCustomizationConfig.dialogueBarHeight();

        // Нижняя панель реплики (во всю ширину)
        this.fill(poseStack, 0, barTop, width, barTop + barH, MenuCustomizationConfig.dialogueBarFill());
        // Верхняя разделительная линия (1px)
        this.fill(poseStack, 0, barTop, width, barTop + 1, MenuCustomizationConfig.dialogueDividerColor());

        drawSpeakerPlate(poseStack, barTop);
        drawReplyText(poseStack, barTop, barH);
        drawResponses(poseStack, mouseX, mouseY);
    }

    private void drawSpeakerPlate(com.mojang.blaze3d.vertex.PoseStack poseStack, int barTop) {
        String speaker = payload.speaker == null ? "" : payload.speaker;
        int plateH = 16;
        int plateW = font.width(speaker) + 20;
        int plateX = MenuCustomizationConfig.dialogueTextLeftIndent();
        int plateY = barTop - (plateH - 1); // внахлёст на 1px снизу

        // Фон плашки
        this.fill(poseStack, plateX, plateY, plateX + plateW, plateY + plateH, MenuCustomizationConfig.dialogueSpeakerPlateFill());
        // Рамка (лево/право/низ)
        int border = MenuCustomizationConfig.dialogueSpeakerPlateBorder();
        this.fill(poseStack, plateX, plateY, plateX + 1, plateY + plateH, border);
        this.fill(poseStack, plateX + plateW - 1, plateY, plateX + plateW, plateY + plateH, border);
        this.fill(poseStack, plateX, plateY + plateH - 1, plateX + plateW, plateY + plateH, border);
        // Верхняя акцентная полоса
        this.fill(poseStack, plateX, plateY, plateX + plateW, plateY + 1, MenuCustomizationConfig.dialogueSpeakerAccent());

        // Имя спикера
        int nameY = plateY + (plateH - font.lineHeight) / 2;
        font.draw(poseStack, Component.literal(speaker), plateX + 10, nameY, MenuCustomizationConfig.dialogueSpeakerNameColor());
    }

    private void drawReplyText(com.mojang.blaze3d.vertex.PoseStack poseStack, int barTop, int barH) {
        String full = payload.text == null ? "" : payload.text.getString();
        int visible = typewriter.getVisibleCharCount(full.length());
        String shown = full.substring(0, Math.max(0, Math.min(full.length(), visible)));

        int baseX = MenuCustomizationConfig.dialogueTextLeftIndent();
        int rightIndent = MenuCustomizationConfig.dialogueTextRightIndent();
        int textW = Math.max(10, width - baseX - rightIndent);

        List<FormattedCharSequence> lines = font.split(Component.literal(shown), textW);
        int lineH = font.lineHeight;
        int totalH = lines.size() * lineH;
        int startY = barTop + Math.max(4, (barH - totalH) / 2);
        for (int i = 0; i < lines.size(); i++) {
            font.draw(poseStack, lines.get(i), baseX, startY + i * lineH, MenuCustomizationConfig.dialogueTextColor());
        }
    }

    private void drawResponses(com.mojang.blaze3d.vertex.PoseStack poseStack, int mouseX, int mouseY) {
        for (ButtonRect rect : responseRects) {
            boolean hover = rect.available
                    && mouseX >= rect.x && mouseX <= rect.x + rect.w
                    && mouseY >= rect.y && mouseY <= rect.y + rect.h;

            int fill, border, textColor;
            if (!rect.available) {
                fill = MenuCustomizationConfig.dialogueResponseDisabledFill();
                border = MenuCustomizationConfig.dialogueResponseDisabledBorder();
                textColor = MenuCustomizationConfig.dialogueResponseDisabledText();
            } else if (hover) {
                fill = MenuCustomizationConfig.dialogueResponseHoverFill();
                border = MenuCustomizationConfig.dialogueResponseHoverBorder();
                textColor = MenuCustomizationConfig.dialogueResponseHoverText();
            } else {
                fill = MenuCustomizationConfig.dialogueResponseIdleFill();
                border = MenuCustomizationConfig.dialogueResponseIdleBorder();
                textColor = MenuCustomizationConfig.dialogueResponseIdleText();
            }

            // Фон
            this.fill(poseStack, rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, fill);
            // Рамка 1px
            this.fill(poseStack, rect.x, rect.y, rect.x + rect.w, rect.y + 1, border);
            this.fill(poseStack, rect.x, rect.y + rect.h - 1, rect.x + rect.w, rect.y + rect.h, border);
            this.fill(poseStack, rect.x, rect.y, rect.x + 1, rect.y + rect.h, border);
            this.fill(poseStack, rect.x + rect.w - 1, rect.y, rect.x + rect.w, rect.y + rect.h, border);

            String prefix = (rect.index + 1) + ". ";
            Component label = Component.literal(prefix).append(payload.responses.get(rect.index).text);
            int textY = rect.y + (rect.h - font.lineHeight) / 2;
            font.draw(poseStack, label, rect.x + 8, textY, textColor);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        for (ButtonRect rect : responseRects) {
            if (!rect.available) {
                continue;
            }
            if (mouseX >= rect.x && mouseX <= rect.x + rect.w && mouseY >= rect.y && mouseY <= rect.y + rect.h) {
                select(rect.index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (payload != null) {
            if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
                int index = keyCode - GLFW.GLFW_KEY_1;
                if (index < payload.responses.size() && payload.responses.get(index).available) {
                    select(index);
                    return true;
                }
            }
            // Пробел — мгновенно допечатать текст
            if (keyCode == GLFW.GLFW_KEY_SPACE) {
                String full = payload.text == null ? "" : payload.text.getString();
                typewriter.skip(full.length());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void select(int index) {
        if (payload == null) {
            return;
        }
        if (index < 0 || index >= payload.responses.size()) {
            return;
        }
        // Сервер валидирует (сессия, индекс, условие if, rate limit) и пришлёт
        // S2CUpdate (следующий узел) или S2CClose.
        QuestNetworking.CHANNEL.sendToServer(new DialogueNetworking.C2SSelectResponsePacket(index));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // не ставит игру на паузу (живой мир продолжает рендериться)
    }
}
