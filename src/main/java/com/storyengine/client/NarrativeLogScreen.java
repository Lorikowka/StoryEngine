package com.storyengine.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.storyengine.narrative.NarrativeChatManager;
import com.storyengine.narrative.NarrativeMessage;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Экран истории сюжетного чата ("что я пропустил?"). Строго ТОЛЬКО для
 * чтения: никаких полей ввода, кнопок и отправки реплик - сюжетный чат
 * наполняется исключительно сервером (пакет S2CStoryChatPacket,
 * команда /storytell). Игрок может только листать историю колёсиком мыши.
 */
public class NarrativeLogScreen extends Screen {

    private static final int MARGIN = 32;
    private static final int ICON_SIZE = 20;
    private static final int ROW_GAP = 10;
    private static final int LINE_HEIGHT = 10;
    private static final int NAME_LINE_HEIGHT = 11;
    private static final int BACKGROUND_COLOR = 0xC0101010;

    /** Прокрутка в пикселях от самого низа истории (0 = видим последнее сообщение). */
    private int scrollOffset;

    public NarrativeLogScreen() {
        super(Component.literal("Сюжетный чат"));
    }

    @Override
    protected void init() {
        this.scrollOffset = 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int panelTop = MARGIN;
        int panelBottom = this.height - MARGIN;
        int maxScroll = Math.max(0, estimateTotalHistoryHeight() - (panelBottom - panelTop));
        this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int) (delta * LINE_HEIGHT * 3)));
        return true;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        drawHistory(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Грубая оценка суммарной высоты всей истории (для ограничения скролла).
     * Не пересчитывает переносы точно посимвольно - этого достаточно, чтобы
     * прокрутка не убегала в пустоту сильно дальше последнего сообщения.
     */
    private int estimateTotalHistoryHeight() {
        List<NarrativeMessage> history = NarrativeChatManager.getHistory();
        int panelWidth = this.width - MARGIN * 2;
        int total = 0;
        for (NarrativeMessage message : history) {
            total += entryHeight(message, panelWidth);
        }
        return total;
    }

    private int entryHeight(NarrativeMessage message, int panelWidth) {
        int textAreaWidth = panelWidth - 16 - ICON_SIZE - 8;
        List<FormattedCharSequence> lines = this.font.split(message.getText(), Math.max(20, textAreaWidth));
        String speaker = message.getSpeaker() == null ? "" : message.getSpeaker();
        int nameHeight = speaker.isBlank() ? 0 : NAME_LINE_HEIGHT;
        int textHeight = Math.max(1, lines.size()) * LINE_HEIGHT;
        return Math.max(ICON_SIZE, nameHeight + textHeight) + ROW_GAP;
    }

    private void drawHistory(PoseStack poseStack) {
        int panelLeft = MARGIN;
        int panelWidth = this.width - MARGIN * 2;
        int panelTop = MARGIN;
        int panelBottom = this.height - MARGIN;

        TextureBlitHelper.fillBox(poseStack, panelLeft, panelTop, panelLeft + panelWidth, panelBottom, BACKGROUND_COLOR);

        List<NarrativeMessage> history = NarrativeChatManager.getHistory();
        if (history.isEmpty()) {
            drawString(poseStack, this.font, Component.literal("Пока пусто - тут появится история сюжетного чата."),
                    panelLeft + 8, panelTop + 8, 0xA0A0A0);
            return;
        }

        int textAreaWidth = panelWidth - 16 - ICON_SIZE - 8;

        // Рисуем снизу вверх (новые сообщения внизу), с учётом scrollOffset.
        int cursorY = panelBottom - 4 + scrollOffset;
        for (int i = history.size() - 1; i >= 0; i--) {
            NarrativeMessage message = history.get(i);
            int height = entryHeight(message, panelWidth);
            cursorY -= height;

            if (cursorY + height < panelTop) {
                break; // всё, что выше - за пределами видимой панели, дальше не рисуем
            }
            if (cursorY > panelBottom) {
                continue; // ниже видимой области (проскроллено вниз) - пропускаем
            }

            drawEntry(poseStack, message, panelLeft + 8, cursorY, textAreaWidth);
        }
    }

    private void drawEntry(PoseStack poseStack, NarrativeMessage message, int x, int y, int textAreaWidth) {
        ResourceLocation icon = DynamicHeadManager.getOrLoad(message.getIconId());
        int textX = x;

        if (icon != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, icon);
            TextureBlitHelper.blitFull(poseStack, x, y, ICON_SIZE, ICON_SIZE);
            textX = x + ICON_SIZE + 8;
        }

        String speaker = message.getSpeaker() == null ? "" : message.getSpeaker();
        int lineY = y;
        if (!speaker.isBlank()) {
            this.font.drawShadow(poseStack, speaker, textX, lineY, message.getNameColor());
            lineY += NAME_LINE_HEIGHT;
        }

        List<FormattedCharSequence> lines = this.font.split(message.getText(), Math.max(20, textAreaWidth));
        for (FormattedCharSequence line : lines) {
            this.font.drawShadow(poseStack, line, textX, lineY, 0xFFFFFF);
            lineY += LINE_HEIGHT;
        }
    }
}
