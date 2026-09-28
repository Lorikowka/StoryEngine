package com.storyengine.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.storyengine.StoryEngineMod;
import com.storyengine.narrative.NarrativeChatManager;
import com.storyengine.narrative.NarrativeMessage;
import com.storyengine.narrative.SpeakerInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

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

    /** Расстояние между последней иконкой спикера и его именем. */
    private static final int ICON_NAME_GAP = 6;

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

        List<String> icons = new ArrayList<>();
        int iconAreaWidth = 0;
        for (SpeakerInfo speaker : message.getSpeakers()) {
            icons.add(speaker.iconId());
            // Зазор добавляется только между иконками: раньше он добавлялся и после
            // последней, из-за чего имя спикера уезжало вправо на ICON_GAP лишних px.
            if (iconAreaWidth > 0) {
                iconAreaWidth += ICON_GAP;
            }
            iconAreaWidth += ICON_SIZE;
        }

        Component fullText = message.getText();
        List<FormattedCharSequence> fullLines = font.split(fullText, TEXT_WRAP_WIDTH);

        MutableComponent speakerComponent = null;
        int nameWidth = 0;
        boolean hasSpeaker = message.getSpeakers().stream()
                .anyMatch(speaker -> speaker.name() != null && !speaker.name().trim().isEmpty());
        if (hasSpeaker) {
            Style textStyle = Style.EMPTY.withColor(TextColor.fromRgb(TEXT_COLOR));
            // Скобки красим в цвет первого спикера. Корневой стиль компонента тоже
            // берёт этот цвет: иначе символы без явного стиля наследуют белый
            // цвет родителя, и "[Name]:" выглядит покрашенным не полностью.
            Style bracketStyle = Style.EMPTY.withColor(
                    TextColor.fromRgb(message.getSpeakers().get(0).nameColor()));
            speakerComponent = Component.literal("[").withStyle(bracketStyle);
            for (int i = 0; i < message.getSpeakers().size(); i++) {
                SpeakerInfo speaker = message.getSpeakers().get(i);
                String name = speaker.name() == null ? "" : speaker.name().trim();
                if (i > 0) {
                    speakerComponent = speakerComponent.append(Component.literal(", ").withStyle(textStyle));
                }
                Style nameStyle = Style.EMPTY.withColor(TextColor.fromRgb(speaker.nameColor()));
                speakerComponent = speakerComponent.append(Component.literal(name).withStyle(nameStyle));
            }
            speakerComponent = speakerComponent
                    .append(Component.literal("]").withStyle(bracketStyle))
                    .append(Component.literal(":").withStyle(textStyle));
            nameWidth = font.width(speakerComponent);
        }

        // Имя отодвинуто от иконок отдельным зазором (ICON_NAME_GAP), чтобы
        // подписи не примыкали к последней иконке впритык.
        int iconNameGap = (hasSpeaker && !icons.isEmpty()) ? ICON_NAME_GAP : 0;

        int textContentWidth = 0;
        for (FormattedCharSequence line : fullLines) {
            textContentWidth = Math.max(textContentWidth, font.width(line));
        }
        textContentWidth = Math.max(textContentWidth, MIN_TEXT_WIDTH);

        // Расчет общей ширины плашки
        int boxWidth =
                iconAreaWidth
                + iconNameGap
                + nameWidth
                + (hasSpeaker ? NAME_GAP : 0)
                + textContentWidth
                + PADDING * 2;

        int boxLeft = screenWidth / 2 - boxWidth / 2 + RIGHT_SHIFT;

        int textX =
                boxLeft
                + PADDING
                + iconAreaWidth
                + iconNameGap
                + nameWidth
                + (hasSpeaker ? NAME_GAP : 0);

        // Реальная высота блока текста (высота строки майна 9px + интервалы)
        int textHeight = (fullLines.size() - 1) * LINE_HEIGHT + 9;
        int contentHeight = Math.max(!icons.isEmpty() ? ICON_SIZE : 0, textHeight);

        int boxHeight = contentHeight + PADDING * 2;
        int boxTop = (screenHeight - 60) - boxHeight;
        int boxBottom = boxTop + boxHeight;

        int contentTop = boxTop + PADDING;

        // Вертикальное центрирование элементов внутри контентной зоны
        int iconTop = contentTop + (contentHeight - ICON_SIZE) / 2;
        int textStartY = contentTop + (contentHeight - textHeight) / 2;
        // Имя спикера — на первой (верхней) строке, без вертикального центрирования
        int nameY = textStartY;

        // 1. Отрисовка подложки
        TextureBlitHelper.fillBox(poseStack, boxLeft, boxTop, boxLeft + boxWidth, boxBottom, BACKGROUND_COLOR);

        // 2. Отрисовка иконок (по центру Y)
        if (!icons.isEmpty()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            int iconX = boxLeft + PADDING;
            for (String iconId : icons) {
                DynamicHeadManager.blitIcon(poseStack, iconId, iconX, iconTop, ICON_SIZE);
                iconX += ICON_SIZE + ICON_GAP;
            }
        }

        // 3. Отрисовка имени ([Name]: — двоеточие в цвете текста, по первой строке)
        if (hasSpeaker) {
            int nameX = boxLeft + PADDING + iconAreaWidth + iconNameGap;
            font.drawShadow(poseStack, speakerComponent, nameX, nameY, TEXT_COLOR);
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

    /**
     * Строит текст, видимый на текущем шаге эффекта печатной машинки.
     *
     * Раньше здесь было full.getString().substring(0, visible) + setStyle(root),
     * что давало два бага:
     *  1) getString() "выравнивает" все вложенные компоненты в одну строку, поэтому
     *     индивидуальные стили (цвета реплик NPC, начертания) терялись - оставался
     *     только стиль корня. Сейчас обходим дерево через visit(), который
     *     отдаёт уже разрешённый стиль для каждого куска.
     *  2) substring() мог разрезать сурогатную пару (эмодзи) пополам, из-за чего
     *     последний символ мигал квадратом. Теперь не отдаём high-surrogate
     *     без пары - символ просто появится на следующем шаге.
     */
    private static Component buildVisibleText(Component full) {
        if (NarrativeChatManager.isFullyRevealed()) {
            return full;
        }
        int[] budget = {NarrativeChatManager.getVisibleCharCount()};
        MutableComponent result = Component.empty();
        full.visit((style, text) -> {
            if (budget[0] <= 0 || text.isEmpty()) {
                return Optional.empty();
            }
            int take = Math.min(budget[0], text.length());
            if (take < text.length() && Character.isHighSurrogate(text.charAt(take - 1))) {
                take--;
            }
            if (take <= 0) {
                return Optional.empty();
            }
            budget[0] -= take;
            result.append(Component.literal(text.substring(0, take)).setStyle(style));
            // Бюджет исчерпан - visit() останавливается, вернув "остаток" текста.
            return budget[0] > 0 ? Optional.empty() : Optional.of(text.substring(take));
        }, Style.EMPTY);
        return result;
    }
}
