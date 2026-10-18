package com.storyengine.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.storyengine.StoryEngineMod;
import com.storyengine.narrative.NarrativeChatManager;
import com.storyengine.narrative.NarrativeMessage;
import com.storyengine.narrative.NarrativeStyle;
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

        boolean hasSpeaker = message.getSpeakers().stream()
                .anyMatch(speaker -> speaker.name() != null && !speaker.name().trim().isEmpty());
        Component prefix = Component.empty();
        if (hasSpeaker) {
            Style textStyle = Style.EMPTY.withColor(TextColor.fromRgb(TEXT_COLOR));
            int firstColor = message.getSpeakers().get(0).nameColor();
            Style firstStyle = Style.EMPTY.withColor(TextColor.fromRgb(firstColor));

            MutableComponent speakerComponent;
            if (message.getStyle() == NarrativeStyle.WHISPER) {
                // "[Имя] [шепотом]" - всё одним цветом (цвет первого спикера)
                StringBuilder names = new StringBuilder();
                for (SpeakerInfo speaker : message.getSpeakers()) {
                    if (names.length() > 0)
                        names.append(", ");
                    names.append(speaker.name() == null ? "" : speaker.name().trim());
                }
                speakerComponent = Component.literal("[" + names + "] [шепотом]").withStyle(firstStyle);
            } else {
                // Обычный формат: [Имя1, Имя2] - как раньше
                speakerComponent = Component.literal("[").withStyle(firstStyle);
                for (int i = 0; i < message.getSpeakers().size(); i++) {
                    SpeakerInfo speaker = message.getSpeakers().get(i);
                    String name = speaker.name() == null ? "" : speaker.name().trim();
                    if (i > 0) {
                        speakerComponent.append(Component.literal(", ").withStyle(textStyle));
                    }
                    speakerComponent.append(Component.literal(name)
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(speaker.nameColor()))));
                }
                speakerComponent.append(Component.literal("]").withStyle(firstStyle));
            }
            speakerComponent.append(Component.literal(": ").withStyle(textStyle));
            prefix = speakerComponent;
        }
        int prefixLength = prefix.getString().length();

        // Имя + реплика - единый компонент, поэтому перенос идёт под имя
        MutableComponent combined = Component.empty().append(prefix).append(fullText);
        List<FormattedCharSequence> fullLines = font.split(combined, TEXT_WRAP_WIDTH);

        // Имя отодвинуто от иконок отдельным зазором (ICON_NAME_GAP), чтобы
        // подписи не примыкали к последней иконке впритык.
        int iconNameGap = (hasSpeaker && !icons.isEmpty()) ? ICON_NAME_GAP : 0;

        int textContentWidth = 0;
        for (FormattedCharSequence line : fullLines) {
            textContentWidth = Math.max(textContentWidth, font.width(line));
        }
        textContentWidth = Math.max(textContentWidth, MIN_TEXT_WIDTH);

        // Расчет общей ширины плашки
        int boxWidth = iconAreaWidth
            + iconNameGap
            + textContentWidth
            + PADDING * 2;

        int boxLeft = screenWidth / 2 - boxWidth / 2 + RIGHT_SHIFT;

        int textX = boxLeft
            + PADDING
            + iconAreaWidth
            + iconNameGap;

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

        // 1. Отрисовка подложки
        TextureBlitHelper.fillBox(poseStack, boxLeft, boxTop, boxLeft + boxWidth, boxBottom, BACKGROUND_COLOR);

        // 2. Отрисовка иконок (по центру Y)
        if (!icons.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            int iconX = boxLeft + PADDING;
            for (String iconId : icons) {
                if (message.getStyle() == NarrativeStyle.THOUGHT) {
                    RenderSystem.setShaderColor(0.72F, 0.42F, 1.0F, 0.52F);
                } else {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
                DynamicHeadManager.blitIcon(poseStack, iconId, iconX, iconTop, ICON_SIZE);
                iconX += ICON_SIZE + ICON_GAP;
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        // 3. Отрисовка: имя видно сразу, реплика печатается
        boolean revealed = NarrativeChatManager.isFullyRevealed();
        int[] budget = { NarrativeChatManager.getVisibleCharCount() + prefixLength };

        int currentLineY = textStartY;
        for (FormattedCharSequence line : fullLines) {
            if (revealed) {
                font.drawShadow(poseStack, line, textX, currentLineY, TEXT_COLOR);
            } else {
                if (budget[0] <= 0) break;
                font.drawShadow(poseStack, truncate(line, budget), textX, currentLineY, TEXT_COLOR);
            }
            currentLineY += LINE_HEIGHT;
        }
    }

    /**
     * Строит текст, видимый на текущем шаге эффекта печатной машинки.
     *
     * Раньше здесь было full.getString().substring(0, visible) + setStyle(root),
     * что давало два бага:
     * 1) getString() "выравнивает" все вложенные компоненты в одну строку, поэтому
     * индивидуальные стили (цвета реплик NPC, начертания) терялись - оставался
     * только стиль корня. Сейчас обходим дерево через visit(), который
     * отдаёт уже разрешённый стиль для каждого куска.
     * 2) substring() мог разрезать сурогатную пару (эмодзи) пополам, из-за чего
     * последний символ мигал квадратом. Теперь не отдаём high-surrogate
     * без пары - символ просто появится на следующем шаге.
     */
    private static Component truncate(FormattedCharSequence line, int[] budget) {
        MutableComponent out = Component.empty();
        StringBuilder sb = new StringBuilder();
        Style[] cur = { null };

        line.accept((index, style, codePoint) -> {
            if (budget[0] <= 0) {
                return false;
            }
            budget[0] -= Character.charCount(codePoint);
            if (cur[0] != null && !cur[0].equals(style)) {
                out.append(Component.literal(sb.toString()).setStyle(cur[0]));
                sb.setLength(0);
            }
            cur[0] = style;
            sb.appendCodePoint(codePoint);
            return true;
        });

        if (sb.length() > 0) {  
            out.append(Component.literal(sb.toString()).setStyle(cur[0]));
        }
        return out;
    }
}
