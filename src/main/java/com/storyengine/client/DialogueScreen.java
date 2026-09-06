package com.storyengine.client;

import com.storyengine.network.QuestNetworking;
import com.storyengine.network.dialogue.DialogueNetworking;
import com.storyengine.network.dialogue.DialogueNetworking.DialogueNodePayload;
import com.storyengine.network.dialogue.DialogueNetworking.ResponsePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
     *  - выступающая плашка имени спикера (внахлёст на 1px, без акцентной полосы);
 *  - текст реплики с эффектом печатной машинки (собственный TypewriterEngine),
 *    вертикально центрируется внутри панели;
 *  - варианты ответа прижаты к верхнему левому углу (X=8, Y=8), 4 состояния
 *    (idle/hover/pressed/disabled);
 *  - весь интерфейс рисуется текстурной картой textures/gui/dialogue_box.png
 *    (9-slice: панель, плашка имени, кнопки) — навигация мышью или цифрами 1–9.
 */
public class DialogueScreen extends Screen {

    private DialogueNodePayload payload;
    private final TypewriterEngine typewriter;
    private final List<ButtonRect> responseRects = new ArrayList<>();
    private boolean prevHideGui;
    private boolean hideGuiCaptured;
    private boolean cleanupDone;

    /** Фреймы текстурной карты dialogue_box.png (см. MenuAssetsManager.DIALOGUE_REGIONS). */
    private static final int FRAME_W = 32;
    private static final int FRAME_H = 32;
    /** Полный размер текстурной карты dialogue_box.png (5 спрайтов × 32px). */
    private static final int SHEET_W = 160;
    private static final int SHEET_H = 32;
    /** Толщина неизменяемой кромки 9-slice (совпадает с art'ом: скругление 4-6px). */
    private static final int FRAME_SLICE = 6;

    /** Индекс кнопки, на которой сейчас удерживается кнопка мыши (-1 = нет). */
    private int pressedIndex = -1;

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
        // Скрываем HUD (в т.ч. хотбар) на время диалога; восстановим в cleanup().
        // prevHideGui запоминаем один раз: re-init() при ресайзе окна не должен
        // затирать оригинальное значение hideGui тем, что уже true.
        if (this.minecraft != null) {
            if (!this.hideGuiCaptured) {
                this.prevHideGui = this.minecraft.options.hideGui;
                this.hideGuiCaptured = true;
            }
            this.minecraft.options.hideGui = true;
        }
    }

    @Override
    public void onClose() {
        cleanup();
        super.onClose();
    }

    /**
     * setScreen(null) и setScreen(другой экран) вызывают только removed(),
     * а НЕ onClose() - поэтому восстановление HUD/камеры и C2SStop вынесены
     * сюда, иначе hideGui оставался true, а серверная сессия диалога "висела"
     * (см. кейс обычного закрытия по S2CCloseDialoguePacket.close()).
     */
    @Override
    public void removed() {
        cleanup();
        super.removed();
    }

    private void cleanup() {
        if (this.cleanupDone) {
            return;
        }
        this.cleanupDone = true;
        DialogueCameraController.clear();
        if (this.minecraft != null) {
            this.minecraft.options.hideGui = this.prevHideGui;
            if (this.minecraft.getConnection() != null) {
                DialogueNetworking.sendStop();
            }
        }
    }

    private int barTop() {
        return height - Math.max(0, MenuCustomizationConfig.dialogueBarHeight());
    }

    private void layoutResponses() {
        if (payload == null) {
            return;
        }
        int bh = Math.max(16, MenuCustomizationConfig.dialogueResponseBoxHeight());
        int gap = MenuCustomizationConfig.dialogueResponseRowGap();
        int x = MenuCustomizationConfig.dialogueResponseX();
        int y = MenuCustomizationConfig.dialogueResponseY();
        int count = payload.responses.size();

        // Динамическая ширина под самую длинную строку ответа + горизонтальный паддинг,
        // с порогом минимальной ширины (boxWidth) чтобы короткие "Да"/"Нет" не обрезались.
        int hPad = MenuCustomizationConfig.dialogueResponseHorizontalPadding();
        int bw = MenuCustomizationConfig.dialogueResponseBoxWidth();
        for (int i = 0; i < count; i++) {
            bw = Math.max(bw, font.width(responseLabel(i)) + hPad * 2);
        }

        responseRects.clear();
        for (int i = 0; i < count; i++) {
            ResponsePayload r = payload.responses.get(i);
            responseRects.add(new ButtonRect(x, y + i * (bh + gap), bw, bh, i, r.available));
        }
    }

    /** Текст кнопки ответа: "N. <реплика>". */
    private Component responseLabel(int index) {
        String prefix = (index + 1) + ". ";
        return Component.literal(prefix).append(payload.responses.get(index).text);
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
        ResourceLocation sheet = MenuAssetsManager.get("dialogue_box");

        if (sheet != null) {
            // Нижняя панель реплики из текстурной карты (9-slice без искажения рамок).
            int[] bar = region("bar");
            TextureBlitHelper.blitNineSlice(poseStack, sheet, 0, barTop, width, barH,
                    bar[0], bar[1], FRAME_W, FRAME_H, FRAME_SLICE, SHEET_W, SHEET_H);
            // Разделительная линия поверх верхней кромки панели (1px).
            this.fill(poseStack, 0, barTop, width, barTop + 1, MenuCustomizationConfig.dialogueDividerColor());
        } else {
            // Fallback: сплошная заливка (текстура карты отсутствует).
            this.fill(poseStack, 0, barTop, width, barTop + barH, MenuCustomizationConfig.dialogueBarFill());
            this.fill(poseStack, 0, barTop, width, barTop + 1, MenuCustomizationConfig.dialogueDividerColor());
        }

        drawSpeakerPlate(poseStack, sheet, barTop);
        drawReplyText(poseStack, barTop, barH);
        drawResponses(poseStack, sheet, mouseX, mouseY);
    }

    private static int[] region(String id) {
        int[] r = MenuAssetsManager.getDialogueRegion(id);
        return r != null ? r : new int[]{0, 0, FRAME_W, FRAME_H};
    }

    private void drawSpeakerPlate(com.mojang.blaze3d.vertex.PoseStack poseStack, ResourceLocation sheet, int barTop) {
        String speaker = payload.speaker == null ? "" : payload.speaker;
        int plateH = 16;
        int plateW = font.width(speaker) + 20;
        int plateX = MenuCustomizationConfig.dialogueTextLeftIndent();
        int plateY = barTop - (plateH - 1); // внахлёст на 1px снизу

        if (sheet != null) {
            int[] r = region("plate");
            TextureBlitHelper.blitNineSlice(poseStack, sheet, plateX, plateY, plateW, plateH,
                    r[0], r[1], FRAME_W, FRAME_H, FRAME_SLICE, SHEET_W, SHEET_H);
        } else {
            // Фон плашки
            this.fill(poseStack, plateX, plateY, plateX + plateW, plateY + plateH, MenuCustomizationConfig.dialogueSpeakerPlateFill());
            // Рамка (лево/право/низ) — без верхней акцентной полосы.
            int border = MenuCustomizationConfig.dialogueSpeakerPlateBorder();
            this.fill(poseStack, plateX, plateY, plateX + 1, plateY + plateH, border);
            this.fill(poseStack, plateX + plateW - 1, plateY, plateX + plateW, plateY + plateH, border);
            this.fill(poseStack, plateX, plateY + plateH - 1, plateX + plateW, plateY + plateH, border);
        }

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
        // Текст не прижимается к нижней кромке панели: гарантированный отступ снизу.
        int bottomPad = 14;
        int topPad = 8;
        int availTop = barTop + topPad;
        int availBottom = barTop + barH - bottomPad;
        int availH = Math.max(0, availBottom - availTop);
        int startY = availTop + Math.max(0, (availH - totalH) / 2);
        for (int i = 0; i < lines.size(); i++) {
            font.draw(poseStack, lines.get(i), baseX, startY + i * lineH, MenuCustomizationConfig.dialogueTextColor());
        }
    }

    private void drawResponses(com.mojang.blaze3d.vertex.PoseStack poseStack, ResourceLocation sheet, int mouseX, int mouseY) {
        for (ButtonRect rect : responseRects) {
            boolean hover = rect.available
                    && mouseX >= rect.x && mouseX <= rect.x + rect.w
                    && mouseY >= rect.y && mouseY <= rect.y + rect.h;
            boolean pressed = rect.available && pressedIndex == rect.index;

            int fill, border, textColor;
            String frameId = "button_idle";
            if (!rect.available) {
                fill = MenuCustomizationConfig.dialogueResponseDisabledFill();
                border = MenuCustomizationConfig.dialogueResponseDisabledBorder();
                textColor = MenuCustomizationConfig.dialogueResponseDisabledText();
            } else if (pressed) {
                fill = MenuCustomizationConfig.dialogueResponsePressedFill();
                border = MenuCustomizationConfig.dialogueResponsePressedBorder();
                textColor = MenuCustomizationConfig.dialogueResponsePressedText();
                frameId = "button_pressed";
            } else if (hover) {
                fill = MenuCustomizationConfig.dialogueResponseHoverFill();
                border = MenuCustomizationConfig.dialogueResponseHoverBorder();
                textColor = MenuCustomizationConfig.dialogueResponseHoverText();
                frameId = "button_hover";
            } else {
                fill = MenuCustomizationConfig.dialogueResponseIdleFill();
                border = MenuCustomizationConfig.dialogueResponseIdleBorder();
                textColor = MenuCustomizationConfig.dialogueResponseIdleText();
            }

            if (sheet != null) {
                // Текстурная кнопка (9-slice): idle/hover/pressed фреймы из dialogue_box.png.
                int[] r = region(frameId);
                TextureBlitHelper.blitNineSlice(poseStack, sheet, rect.x, rect.y, rect.w, rect.h,
                        r[0], r[1], FRAME_W, FRAME_H, FRAME_SLICE, SHEET_W, SHEET_H);
            } else {
                // Fallback: заливка + рамка 1px.
                this.fill(poseStack, rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, fill);
                this.fill(poseStack, rect.x, rect.y, rect.x + rect.w, rect.y + 1, border);
                this.fill(poseStack, rect.x, rect.y + rect.h - 1, rect.x + rect.w, rect.y + rect.h, border);
                this.fill(poseStack, rect.x, rect.y, rect.x + 1, rect.y + rect.h, border);
                this.fill(poseStack, rect.x + rect.w - 1, rect.y, rect.x + rect.w, rect.y + rect.h, border);
            }

            // Текст: строго по центру кнопки (горизонтально и вертикально).
            Component label = responseLabel(rect.index);
            int textX = rect.x + Math.max(0, (rect.w - font.width(label)) / 2);
            int textY = rect.y + (rect.h - font.lineHeight) / 2;
            font.draw(poseStack, label, textX, textY, textColor);
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
                this.pressedIndex = rect.index;
                select(rect.index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.pressedIndex = -1;
        return super.mouseReleased(mouseX, mouseY, button);
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
