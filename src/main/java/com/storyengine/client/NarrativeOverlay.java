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
 * Рендерит текущее сообщение сюжетного чата по центру-низу экрана.
 *
 * Сюжетный чат - интерфейс ТОЛЬКО для чтения: он наполняется исключительно
 * пакетом S2CStoryChatPacket (команда /storytell). Перехват ванильного чата
 * (ClientChatReceivedEvent) отключён - обычные сообщения игроков в сюжетный
 * оверлей не попадают; поле ввода в NarrativeLogScreen также убрано.
 *
 * Окно подложки имеет фиксированную ширину, текст переносится в пределах
 * симметричных полей, всё содержимое (иконка, [Имя], строки текста)
 * центрировано по горизонтали и вертикали. Оформление - стилизованная
 * панель в едином стиле мода (текстура рамки как у меню квестов).
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NarrativeOverlay {

    /** Текстура панели в едином стиле мода - та же, что у меню квестов. */
    private static final ResourceLocation PANEL_TEXTURE =
            new ResourceLocation(StoryEngineMod.MOD_ID, "textures/gui/quest_menu.png");
    private static final int TEXTURE_SIZE = 256;

    /** Фиксированная ширина окна подложки - текст не может растянуть её. */
    private static final int BOX_WIDTH = 300;
    /** Внутренний отступ контента от краёв окна (симметрично слева/справа). */
    private static final int PADDING = 10;
    /** Отступ тёмной заливки от края текстурированной рамки. */
    private static final int INNER_FILL_INSET = 4;
    /** Отступ рамки от нижнего края экрана. */
    private static final int BOTTOM_OFFSET = 60;

    private static final int LINE_HEIGHT = 10;
    /** Вертикальный зазор между блоками: иконка -> имя -> текст. */
    private static final int BLOCK_GAP = 4;
    private static final int ICON_SIZE = 16;
    /** Минимальная высота контентной зоны - чтобы окно не схлопывалось. */
    private static final int MIN_CONTENT_HEIGHT = 24;

    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int INNER_FILL_COLOR = 0xCC202020;

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

    /*
     * Перехват ClientChatReceivedEvent полностью убран: обычные сообщения
     * игроков уходят в ванильный чат и больше не дублируются в очередь
     * NarrativeChatManager / сюжетный оверлей.
     */

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

        boolean hasIcon = icon != null;
        boolean hasName = !speaker.isBlank();

        // Ширина переноса = фиксированная ширина окна минус два симметричных поля.
        int wrapWidth = BOX_WIDTH - PADDING * 2;

        // Габариты окна считаются по ПОЛНОМУ тексту, поэтому во время анимации
        // печати окно не "дышит"; печатаемая часть центрируется внутри готовых границ.
        List<FormattedCharSequence> fullLines = font.split(message.getText(), wrapWidth);

        int gapsCount = (hasIcon ? 1 : 0) + (hasName ? 1 : 0);
        int stackHeight = (hasIcon ? ICON_SIZE + BLOCK_GAP : 0)
                + (hasName ? font.lineHeight + BLOCK_GAP : 0)
                + Math.max(1, fullLines.size()) * LINE_HEIGHT;
        int contentHeight = Math.max(MIN_CONTENT_HEIGHT, stackHeight);

        int boxWidth = BOX_WIDTH;
        int boxHeight = PADDING * 2 + contentHeight;
        int boxLeft = screenWidth / 2 - boxWidth / 2;
        int boxTop = screenHeight - BOTTOM_OFFSET - boxHeight;
        int boxBottom = boxTop + boxHeight;
        int centerX = screenWidth / 2;

        // 1. Стилизованная панель: рамка в едином стиле мода + затемнение изнутри.
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, PANEL_TEXTURE);
        TextureBlitHelper.blitStretched(poseStack, boxLeft, boxTop, boxWidth, boxHeight, TEXTURE_SIZE);
        TextureBlitHelper.fillBox(poseStack,
                boxLeft + INNER_FILL_INSET, boxTop + INNER_FILL_INSET,
                boxLeft + boxWidth - INNER_FILL_INSET, boxBottom - INNER_FILL_INSET,
                INNER_FILL_COLOR);

        // Контентная зона: весь стек вертикально отцентрирован внутри окна.
        int contentTop = boxTop + PADDING + (contentHeight - stackHeight) / 2;

        int cursorY = contentTop;

        // 2. Иконка - строго по центру горизонтали.
        if (hasIcon) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, icon);
            TextureBlitHelper.blitFull(poseStack, centerX - ICON_SIZE / 2, cursorY, ICON_SIZE, ICON_SIZE);
            cursorY += ICON_SIZE + BLOCK_GAP;
        }

        // 3. Имя автора в скобках [Имя] - строго по центру горизонтали.
        if (hasName) {
            int nameX = centerX - font.width(speaker) / 2;
            font.drawShadow(poseStack, speaker, nameX, cursorY, message.getNameColor());
            cursorY += font.lineHeight + BLOCK_GAP;
        }

        // 4. Строки текста - каждая по центру горизонтали, блок целиком
        //    отцентрирован по вертикали в пределах своей зоны.
        Component visibleText = buildVisibleText(message.getText());
        List<FormattedCharSequence> visibleLines = font.split(visibleText, wrapWidth);

        int fullTextHeight = Math.max(1, fullLines.size()) * LINE_HEIGHT;
        int visibleTextHeight = Math.max(1, visibleLines.size()) * LINE_HEIGHT;
        int lineY = cursorY + (fullTextHeight - visibleTextHeight) / 2;

        for (FormattedCharSequence line : visibleLines) {
            int lineX = centerX - font.width(line) / 2;
            font.drawShadow(poseStack, line, lineX, lineY, TEXT_COLOR);
            lineY += LINE_HEIGHT;
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
