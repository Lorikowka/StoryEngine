package com.storyengine.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.storyengine.narrative.NarrativeChatManager;
import com.storyengine.narrative.NarrativeMessage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Экран истории сюжетного чата ("что я пропустил?"). Строго ТОЛЬКО для
 * чтения: никаких полей ввода, кнопок отправки - сюжетный чат наполняется
 * исключительно сервером (пакет S2CStoryChatPacket, команда /storytell).
 * Игрок может только листать историю колёсиком мыши.
 *
 * Оформление: узкое (~360px) центрированное окно-лента. Синие элементы -
 * только сверху (шапка с заголовком и кнопкой закрытия) и снизу (подвал с
 * подсказкой). Между ними - тёмная прокручиваемая лента сообщений со
 * скроллбаром и строками в стиле обычного чата. При открытии окно мягко
 * выезжает снизу и проявляется.
 */
public class NarrativeLogScreen extends Screen {

    private static final int WIN_W = 360;
    private static final int HEADER_H = 30;
    private static final int FOOTER_H = 26;
    private static final int SCROLLBAR_W = 4;

    private static final int ICON_SIZE = 20;
    private static final int ROW_GAP = 10;
    private static final int LINE_HEIGHT = 10;
    private static final int NAME_LINE_HEIGHT = 11;

    private static final int ANIM_MS = 350;

    // Цвета шапки/подвала/ленты/скроллбара берутся из MenuCustomizationConfig
    // (секция narrativeLogCustomization), как и у остальных GUI мода.

    /** Прокрутка в пикселях от самого низа истории (0 = видим последнее сообщение). */
    private int scrollOffset;
    private long openedAt;

    // Геометрия окна в GUI-координатах (пересчитывается каждый кадр).
    private int winX;
    private int winY;
    private int winW = WIN_W;
    private int winH;
    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;
    private int feedLeft;
    private int feedRight;
    private int feedTop;
    private int feedBottom;
    private int scrollbarX;

    public NarrativeLogScreen() {
        super(Component.literal("Сюжетный чат"));
    }

    @Override
    protected void init() {
        this.scrollOffset = 0;
        this.openedAt = Util.getMillis();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        computeGeometry(0);
        int feedHeight = feedBottom - feedTop;
        int maxScroll = Math.max(0, estimateTotalHistoryHeight() - feedHeight);
        this.scrollOffset = Mth.clamp(this.scrollOffset - (int) (delta * LINE_HEIGHT * 3), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            computeGeometry(0);
            int bx = winX + winW - 26;
            int by = winY + 7;
            if (mouseX >= bx && mouseX <= bx + 16 && mouseY >= by && mouseY <= by + 16) {
                Minecraft.getInstance().setScreen(null);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);

        long elapsed = Util.getMillis() - this.openedAt;
        float p = clamp01(elapsed / (float) ANIM_MS);
        float ease = easeOutCubic(p);
        float alpha = ease;
        float slide = (1f - ease) * 16f;

        computeGeometry(slide);

        drawHeader(poseStack, alpha);
        drawFooter(poseStack, alpha);

        TextureBlitHelper.fillBox(poseStack, panelLeft, panelTop, panelRight, panelBottom, fade(MenuCustomizationConfig.logFeedFill(), alpha));

        enableScissor(feedLeft, feedTop, feedRight, feedBottom);
        drawHistory(poseStack, alpha);
        disableScissor();

        int feedHeight = feedBottom - feedTop;
        int maxScroll = Math.max(0, estimateTotalHistoryHeight() - feedHeight);
        if (maxScroll > 0) {
            drawScrollbar(poseStack, alpha, feedHeight, maxScroll);
        }

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Пересчитывает прямоугольники окна в GUI-координатах. slide - смещение
     * по Y для анимации появления (положительное = окно начинает ниже и
     * поднимается на место).
     */
    private void computeGeometry(float slide) {
        this.winW = WIN_W;
        this.winH = Mth.clamp((int) (this.height * 0.8f), 320, 560);
        this.winX = (this.width - this.winW) / 2;
        this.winY = (this.height - this.winH) / 2 + (int) slide;

        this.panelLeft = this.winX + 6;
        this.panelRight = this.winX + this.winW - 6;
        this.panelTop = this.winY + HEADER_H;
        this.panelBottom = this.winY + this.winH - FOOTER_H;

        this.scrollbarX = this.panelRight - 6 - SCROLLBAR_W;
        this.feedRight = this.scrollbarX - 2;
        this.feedLeft = this.panelLeft + 6;
        this.feedTop = this.panelTop + 6;
        this.feedBottom = this.panelBottom - 6;
    }

    private void drawHeader(PoseStack poseStack, float alpha) {
        TextureBlitHelper.fillBox(poseStack, winX, winY, winX + winW, winY + HEADER_H, fade(MenuCustomizationConfig.logHeaderFill(), alpha));
        TextureBlitHelper.fillBox(poseStack, winX, winY, winX + winW, winY + 2, fade(MenuCustomizationConfig.logAccentLine(), alpha));

        String title = "Сюжетный чат";
        int tx = winX + (winW - this.font.width(title)) / 2;
        this.font.drawShadow(poseStack, title, tx, winY + (HEADER_H - 9) / 2, fade(MenuCustomizationConfig.logTitleColor(), alpha));

        this.font.drawShadow(poseStack, "×", winX + winW - 22, winY + (HEADER_H - 9) / 2, fade(MenuCustomizationConfig.logTitleColor(), alpha));
    }

    private void drawFooter(PoseStack poseStack, float alpha) {
        int fy = winY + winH - FOOTER_H;
        TextureBlitHelper.fillBox(poseStack, winX, fy, winX + winW, winY + winH, fade(MenuCustomizationConfig.logFooterFill(), alpha));
        TextureBlitHelper.fillBox(poseStack, winX, winY + winH - 2, winX + winW, winY + winH, fade(MenuCustomizationConfig.logAccentLine(), alpha));

        String hint = "Колесо мыши — прокрутка";
        int hx = winX + (winW - this.font.width(hint)) / 2;
        this.font.drawShadow(poseStack, hint, hx, fy + (FOOTER_H - 9) / 2, fade(MenuCustomizationConfig.logHintColor(), alpha));
    }

    /**
     * Грубая оценка суммарной высоты всей истории (для ограничения скролла).
     */
    private int estimateTotalHistoryHeight() {
        List<NarrativeMessage> history = NarrativeChatManager.getHistory();
        int panelWidth = feedRight - feedLeft;
        int total = 0;
        for (NarrativeMessage message : history) {
            total += entryHeight(message, panelWidth);
        }
        return total;
    }

    private int entryHeight(NarrativeMessage message, int panelWidth) {
        int textAreaWidth = Math.max(20, panelWidth - ICON_SIZE - 8);
        List<FormattedCharSequence> lines = this.font.split(message.getText(), textAreaWidth);
        String speaker = message.getSpeaker() == null ? "" : message.getSpeaker();
        int nameHeight = speaker.isBlank() ? 0 : NAME_LINE_HEIGHT;
        int textHeight = Math.max(1, lines.size()) * LINE_HEIGHT;
        return Math.max(ICON_SIZE, nameHeight + textHeight) + ROW_GAP;
    }

    private void drawHistory(PoseStack poseStack, float alpha) {
        List<NarrativeMessage> history = NarrativeChatManager.getHistory();
        if (history.isEmpty()) {
            String msg = "Пока пусто — тут появится история сюжетного чата.";
            int msgW = this.font.width(msg);
            int sx = Math.max(feedLeft, feedLeft + (feedRight - feedLeft - msgW) / 2);
            int sy = (feedTop + feedBottom - 9) / 2;
            this.font.drawShadow(poseStack, msg, sx, sy, fade(MenuCustomizationConfig.logEmptyColor(), alpha));
            return;
        }

        int textAreaWidth = Math.max(20, (feedRight - feedLeft) - ICON_SIZE - 8);

        // Рисуем снизу вверх (новые сообщения внизу), с учётом scrollOffset.
        int cursorY = feedBottom - 4 + scrollOffset;
        for (int i = history.size() - 1; i >= 0; i--) {
            NarrativeMessage message = history.get(i);
            int height = entryHeight(message, feedRight - feedLeft);
            cursorY -= height;

            if (cursorY + height < feedTop) {
                break; // всё, что выше - за пределами видимой ленты
            }
            if (cursorY > feedBottom) {
                continue; // ниже видимой области (проскроллено вниз)
            }

            drawEntry(poseStack, message, cursorY, textAreaWidth, alpha);
        }
    }

    private void drawEntry(PoseStack poseStack, NarrativeMessage message, int y, int textAreaWidth, float alpha) {
        ResourceLocation icon = DynamicHeadManager.getOrLoad(message.getIconId());
        String speaker = message.getSpeaker() == null ? "" : message.getSpeaker();

        List<FormattedCharSequence> lines = this.font.split(message.getText(), textAreaWidth);

        int x = feedLeft;
        if (icon != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            RenderSystem.setShaderTexture(0, icon);
            TextureBlitHelper.blitFull(poseStack, x, y, ICON_SIZE, ICON_SIZE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        int textX = x + ICON_SIZE + 8;
        int lineY = y;
        if (!speaker.isBlank()) {
            drawChatLine(poseStack, Component.literal(speaker).getVisualOrderText(), textX, lineY, NAME_LINE_HEIGHT, message.getNameColor(), alpha);
            lineY += NAME_LINE_HEIGHT;
        }

        for (FormattedCharSequence line : lines) {
            drawChatLine(poseStack, line, textX, lineY, LINE_HEIGHT, MenuCustomizationConfig.logBodyColor(), alpha);
            lineY += LINE_HEIGHT;
        }
    }

    /**
     * Рисует одну строку как в обычном чате: полупрозрачная тёмная подложка
     * за текстом (как ванильный ChatComponent) + сам текст с тенью.
     */
    private void drawChatLine(PoseStack poseStack, FormattedCharSequence line, int x, int y, int lineH, int color, float alpha) {
        int w = this.font.width(line);
        int pad = 2;
        TextureBlitHelper.fillBox(poseStack, x - pad, y - 1, x + w + pad + 1, y + lineH, fade(MenuCustomizationConfig.logChatLineBackground(), alpha));
        this.font.drawShadow(poseStack, line, x, y, fade(color, alpha));
    }

    private void drawScrollbar(PoseStack poseStack, float alpha, int viewHeight, int maxScroll) {
        int trackH = viewHeight;
        int thumbH = Math.max(16, trackH * trackH / (trackH + maxScroll));
        // Бегунок внизу = смотрим самые новые (scrollOffset=0), вверху = старая история.
        int thumbY = feedTop + ((maxScroll - scrollOffset) * (trackH - thumbH)) / Math.max(1, maxScroll);
        TextureBlitHelper.fillBox(poseStack, scrollbarX, feedTop, scrollbarX + SCROLLBAR_W, feedBottom, fade(MenuCustomizationConfig.logScrollbarTrack(), alpha));
        TextureBlitHelper.fillBox(poseStack, scrollbarX, thumbY, scrollbarX + SCROLLBAR_W, thumbY + thumbH, fade(MenuCustomizationConfig.logScrollbarThumb(), alpha));
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static float easeOutCubic(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    /** Умножает альфа-канал цвета ARGB на множитель (для анимации проявления). */
    private static int fade(int argb, float factor) {
        int alpha = (argb >>> 24) & 0xFF;
        int newAlpha = (int) (alpha * factor) & 0xFF;
        return (newAlpha << 24) | (argb & 0x00FFFFFF);
    }
}
