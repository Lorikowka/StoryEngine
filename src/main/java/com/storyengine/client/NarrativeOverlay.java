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
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Рендерит текущее сообщение сюжетного чата по центру-низу экрана
 * (со сдвигом чуть вправо), с тёмной подложкой под иконкой/именем/текстом.
 * Подложка подстраивается под фактическую ширину текста, а не занимает
 * фиксированную ширину. Тикает NarrativeChatManager, чтобы двигать эффект
 * печатной машинки.
 *
 * Обычные сообщения игроков перехватом не затрагиваются - они уходят
 * в ванильный чат; сюжетный оверлей наполняется только пакетом
 * S2CStoryChatPacket (команда /storytell).
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NarrativeOverlay {

    private static final int ICON_SIZE = 24;
    private static final int ICON_GAP = 8;
    /** Максимальная ширина текста ДО переноса строк - не ширина всего блока. */
    private static final int TEXT_WRAP_WIDTH = 260;
    /** Насколько сдвигаем блок вправо от идеального центра экрана. */
    private static final int RIGHT_SHIFT = 30;
    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int NAME_LINE_HEIGHT = 12;
    /** Минимальная ширина текстовой части блока, чтобы он не схлопывался на короткой строке. */
    private static final int MIN_TEXT_WIDTH = 24;

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
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        // HOTBAR рендерится каждый кадр ровно один раз - используем как якорь,
        // чтобы не рисовать сообщение по разу на каждый из overlay'ев подряд.
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            // Не рисуем поверх открытых экранов (инвентарь, меню квестов и т.п.)
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
        String speaker = message.getSpeaker() == null ? "" : message.getSpeaker();
        int iconAreaWidth = icon != null ? ICON_SIZE + ICON_GAP : 0;

        Component fullText = message.getText();
        List<FormattedCharSequence> fullLines = font.split(fullText, TEXT_WRAP_WIDTH);

        // Размер подложки считаем по ПОЛНОМУ тексту, а не по текущему кадру
        // печатной машинки - иначе рамка растёт/дёргается каждый тик по мере
        // печати. Сам эффект печати анимирует только то, что рисуется внутри
        // уже стабильного по размеру блока.
        int textContentWidth = speaker.isBlank() ? 0 : font.width(speaker);
        for (FormattedCharSequence line : fullLines) {
            textContentWidth = Math.max(textContentWidth, font.width(line));
        }
        textContentWidth = Math.max(textContentWidth, MIN_TEXT_WIDTH);

        int boxWidth = iconAreaWidth + textContentWidth + PADDING * 2;
        int boxLeft = screenWidth / 2 - boxWidth / 2 + RIGHT_SHIFT;
        int textX = boxLeft + PADDING + iconAreaWidth;

        // Центр-низ экрана: Y = высота экрана - 60, как задано в ТЗ.
        int baseY = screenHeight - 60;

        int nameHeight = speaker.isBlank() ? 0 : NAME_LINE_HEIGHT;
        int textHeight = fullLines.size() * LINE_HEIGHT;
        int contentHeight = Math.max(icon != null ? ICON_SIZE : 0, nameHeight + textHeight);

        int iconTop = baseY - ICON_SIZE + 8;
        int boxTop = iconTop - PADDING;
        int boxBottom = boxTop + contentHeight + PADDING * 2;

        // Тёмная подложка под всем блоком (иконка + имя + текст), размер под контент.
        TextureBlitHelper.fillBox(poseStack, boxLeft, boxTop, boxLeft + boxWidth, boxBottom, BACKGROUND_COLOR);

        if (icon != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, icon);
            TextureBlitHelper.blitFull(poseStack, boxLeft + PADDING, iconTop, ICON_SIZE, ICON_SIZE);
        }

        int lineY = boxTop + PADDING;
        if (!speaker.isBlank()) {
            font.drawShadow(poseStack, speaker, textX, lineY, message.getNameColor());
            lineY += NAME_LINE_HEIGHT;
        }
        Component visibleText = buildVisibleText(fullText);
        List<FormattedCharSequence> visibleLines = font.split(visibleText, TEXT_WRAP_WIDTH);
        for (FormattedCharSequence line : visibleLines) {
            font.drawShadow(poseStack, line, textX, lineY, TEXT_COLOR);
            lineY += LINE_HEIGHT;
        }
    }

    /**
     * Возвращает текст, обрезанный под текущее число видимых символов
     * (эффект печатной машинки). Пока строка печатается, форматирование
     * упрощается до единого стиля корневого компонента - посимвольно
     * сохранять несколько цветовых "прогонов" внутри Component в 1.19.2
     * без специального API накладно, а после полной отрисовки показывается
     * оригинальный Component целиком, со всем форматированием как в /tellraw.
     */
    private static Component buildVisibleText(Component full) {
        if (NarrativeChatManager.isFullyRevealed()) {
            return full;
        }
        String plain = full.getString();
        int visible = Math.min(NarrativeChatManager.getVisibleCharCount(), plain.length());
        return Component.literal(plain.substring(0, visible)).setStyle(full.getStyle());
    }
}
