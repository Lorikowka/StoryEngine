package com.storyengine.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.storyengine.StoryEngineMod;
import com.storyengine.narrative.NarrativeChatManager;
import com.storyengine.narrative.NarrativeMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Рендерит текущее сообщение сюжетного чата по центру-низу экрана
 * с тёмной подложкой под иконкой/именем/текстом.
 * Иконка и имя вертикально центрируются относительно высоты всего текста.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NarrativeOverlay {

    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int TEXT_WRAP_WIDTH = 260;
    private static final int RIGHT_SHIFT = 0;
    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int MIN_TEXT_WIDTH = 20;

    /** Расстояние между именем и текстом сообщения. */
    private static final int NAME_GAP = 4;

    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int BACKGROUND_COLOR = 0xB0101010;

    private NarrativeOverlay() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        NarrativeChatManager.tick();
    }

    @SubscribeEvent
    public static void onPlayerChat(ClientChatReceivedEvent event) {
        if (event.isSystem()) {
            return;
        }
        NarrativeChatManager.enqueue(new NarrativeMessage("", "none", event.getMessage()));
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        NarrativeMessage message = NarrativeChatManager.getCurrent();
        if (message == null) {
            return;
        }

        render(event.getPoseStack(), event.getWindow(), message);
    }

    private static void render(PoseStack poseStack, Window window, NarrativeMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        ResourceLocation icon = DynamicHeadManager.getOrLoad(message.getIconId());
        String rawSpeaker = message.getSpeaker() == null ? "" : message.getSpeaker().trim();
        String speaker = rawSpeaker.isEmpty() ? "" : "[" + rawSpeaker + "]";
        int iconAreaWidth = icon != null ? ICON_SIZE + ICON_GAP : 0;

        Component fullText = message.getText();
        List<FormattedCharSequence> fullLines = font.split(fullText, TEXT_WRAP_WIDTH);

        int nameWidth = speaker.isBlank() ? 0 : font.width(speaker);

        int textContentWidth = 0;
        for (FormattedCharSequence line : fullLines) {
            textContentWidth = Math.max(textContentWidth, font.width(line));
        }
        textContentWidth = Math.max(textContentWidth, MIN_TEXT_WIDTH);

        // Расчет общей ширины плашки
        int boxWidth =
                iconAreaWidth
                + nameWidth
                + (speaker.isBlank() ? 0 : NAME_GAP)
                + textContentWidth
                + PADDING * 2;

        int boxLeft = screenWidth / 2 - boxWidth / 2 + RIGHT_SHIFT;

        int textX =
                boxLeft
                + PADDING
                + iconAreaWidth
                + nameWidth
                + (speaker.isBlank() ? 0 : NAME_GAP);

        // Реальная высота блока текста (высота строки майна 9px + интервалы)
        int textHeight = (fullLines.size() - 1) * LINE_HEIGHT + 9;
        int contentHeight = Math.max(icon != null ? ICON_SIZE : 0, textHeight);

        int boxHeight = contentHeight + PADDING * 2;
        int boxTop = (screenHeight - 60) - boxHeight;
        int boxBottom = boxTop + boxHeight;

        int contentTop = boxTop + PADDING;

        // Вертикальное центрирование элементов внутри контентной зоны
        int iconTop = contentTop + (contentHeight - ICON_SIZE) / 2;
        int nameY = contentTop + (contentHeight - 9) / 2;
        int textStartY = contentTop + (contentHeight - textHeight) / 2;

        // 1. Отрисовка подложки
        TextureBlitHelper.fillBox(poseStack, boxLeft, boxTop, boxLeft + boxWidth, boxBottom, BACKGROUND_COLOR);

        // 2. Отрисовка иконки (по центру Y)
        if (icon != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, icon);
            TextureBlitHelper.blitFull(poseStack, boxLeft + PADDING, iconTop, ICON_SIZE, ICON_SIZE);
        }

        // 3. Отрисовка имени (по центру Y)
        if (!speaker.isBlank()) {
            int nameX = boxLeft + PADDING + iconAreaWidth;
            font.drawShadow(poseStack, speaker, nameX, nameY, message.getNameColor());
        }

        // 4. Отрисовка текста (построчно от textStartY)
        Component visibleText = buildVisibleText(fullText);
        List<FormattedCharSequence> visibleLines = font.split(visibleText, TEXT_WRAP_WIDTH);

        int currentLineY = textStartY;
        for (FormattedCharSequence line : visibleLines) {
            font.drawShadow(poseStack, line, textX, currentLineY, TEXT_COLOR);
            currentLineY += LINE_HEIGHT;
        }
    }

    private static Component buildVisibleText(Component full) {
        if (NarrativeChatManager.isFullyRevealed()) {
            return full;
        }
        String plain = full.getString();
        int visible = Math.min(NarrativeChatManager.getVisibleCharCount(), plain.length());
        return Component.literal(plain.substring(0, visible)).setStyle(full.getStyle());
    }
}
