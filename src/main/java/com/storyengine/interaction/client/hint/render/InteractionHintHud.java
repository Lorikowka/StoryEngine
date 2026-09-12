package com.storyengine.interaction.client.hint.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.storyengine.StoryEngineMod;
import com.storyengine.client.MenuAssetsManager;
import com.storyengine.client.MenuCustomizationConfig;
import com.storyengine.client.TextureBlitHelper;
import com.storyengine.interaction.client.hint.InteractionHintState;
import com.storyengine.interaction.client.hint.TriggerHintClientState;
import com.storyengine.interaction.client.hint.VanillaActionResolver;
import com.storyengine.interaction.client.input.InteractionInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * HUD «[F] Действие» в левом нижнем углу экрана (порт HudRenderer из Interaction Hint):
 * показывает название действия под крестовиной, когда игрок смотрит на «интерактибль».
 *
 * Анимация: плавный slide-in слева + масштабный pulse + плавное затухание
 * при потере цели. Фон - плашка interaction_hint.png (9-slice, без искажения
 * скруглённых углов) либо сплошная заливка + рамка (переключается конфигом
 * interactionHintCustomization.useTexture).
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class InteractionHintHud {

    /** Текстура плашки: 64x32, скруглённый прямоугольник с рамкой. */
    private static final int TEX_W = 64;
    private static final int TEX_H = 32;
    private static final int TEX_BORDER = 8;

    private static final int PADDING_X = 26;
    private static final int PADDING_Y = 8;
    private static float progress = 0.0f;
    private static boolean visibleTarget = false;
    private static String displayedAction = "";
    private static Vec3 displayedTargetPos = null;
    private static long lastNanoTime = System.nanoTime();

    private InteractionHintHud() {
    }

    /** Для маркера-кольца (InteractionCrosshairMarker): текущая анимация 0..1. */
    public static float getAnim() {
        return ease(progress);
    }

    /** Позиция последней отрисованной цели (для маркера-кольца). */
    public static Vec3 getDisplayedTargetPos() {
        return displayedTargetPos;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || mc.level == null) {
            return;
        }
        if (!MenuCustomizationConfig.interactionHintEnabled()) {
            return;
        }

        tick();

        if (progress <= 0.001f || displayedAction.isEmpty()) {
            return;
        }

        Font font = mc.font;
        Component text = null;
        String serverLabel = TriggerHintClientState.hintLabel();
        if (TriggerHintClientState.hasActive() && serverLabel != null && !serverLabel.isBlank()) {
            text = Component.literal("[")
                    .append(InteractionInputHandler.INTERACT_KEY.getTranslatedKeyMessage())
                    .append("] ")
                    .append(Component.literal(serverLabel));
        } else if (TriggerHintClientState.hasActive()) {
            text = Component.literal("[")
                    .append(InteractionInputHandler.INTERACT_KEY.getTranslatedKeyMessage())
                    .append("] ")
                    .append(Component.translatable(VanillaActionResolver.USE));
        }
        if (text == null) {
            text = Component.literal("[")
                    .append(InteractionInputHandler.INTERACT_KEY.getTranslatedKeyMessage())
                    .append("] ")
                    .append(Component.translatable(displayedAction));
        }
        int textWidth = font.width(text);
        int textHeight = font.lineHeight;

        float anim = ease(progress);
        float slide = (1.0f - anim) * 8.0f;
        float scale = 0.97f + anim * 0.03f;

        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float x = MenuCustomizationConfig.hintLeftOffset() - slide;
        float y = screenHeight - MenuCustomizationConfig.hintBottomOffset() - textHeight;

        int bgX = (int) x - PADDING_X;
        int bgY = (int) y - PADDING_Y;
        int bgWidth = textWidth + 2 * PADDING_X;
        int bgHeight = textHeight + 2 * PADDING_Y;

        PoseStack pose = event.getPoseStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int fill = MenuCustomizationConfig.hintFill();
        int border = MenuCustomizationConfig.hintBorder();
        int textArgb = MenuCustomizationConfig.hintText();

        int fillAlphaInt = (int) (((fill >>> 24) & 0xFF) * anim);
        int fillColor = (fillAlphaInt << 24) | (fill & 0x00FFFFFF);
        int borderAlphaInt = (int) (((border >>> 24) & 0xFF) * anim);
        int borderColor = (borderAlphaInt << 24) | (border & 0x00FFFFFF);
        int textAlphaInt = (int) (((textArgb >>> 24) & 0xFF) * anim);
        int textColor = (textAlphaInt << 24) | (textArgb & 0x00FFFFFF);

        pose.pushPose();
        pose.translate(x + textWidth / 2.0F, y + textHeight / 2.0F, 0.0);
        pose.scale(scale, scale, 1.0f);
        pose.translate(-(x + textWidth / 2.0F), -(y + textHeight / 2.0F), 0.0);

        if (MenuCustomizationConfig.hintUseTexture()) {
            ResourceLocation tex = MenuAssetsManager.get("interaction_hint");
            if (tex != null) {
                TextureBlitHelper.blitNineSlice(pose, tex, bgX, bgY, bgWidth, bgHeight,
                        0, 0, TEX_W, TEX_H, TEX_BORDER, TEX_W, TEX_H);
            } else {
                TextureBlitHelper.fillBox(pose, bgX, bgY, bgX + bgWidth, bgY + bgHeight, fillColor);
            }
        } else {
            TextureBlitHelper.fillBox(pose, bgX, bgY, bgX + bgWidth, bgY + bgHeight, fillColor);
        }

        if (!MenuCustomizationConfig.hintUseTexture()) {
            TextureBlitHelper.fillBox(pose, bgX, bgY, bgX + bgWidth, bgY + 1, borderColor);
            TextureBlitHelper.fillBox(pose, bgX, bgY + bgHeight - 1, bgX + bgWidth, bgY + bgHeight, borderColor);
            TextureBlitHelper.fillBox(pose, bgX, bgY, bgX + 1, bgY + bgHeight, borderColor);
            TextureBlitHelper.fillBox(pose, bgX + bgWidth - 1, bgY, bgX + bgWidth, bgY + bgHeight, borderColor);
        }

        font.draw(pose, text, x, y, textColor);

        pose.popPose();

        RenderSystem.disableBlend();
    }

    private static void tick() {
        String action = InteractionHintState.getAimedAction();
        Vec3 targetPos = InteractionHintState.getAimedPos();
        boolean serverActive = TriggerHintClientState.hasActive();
        boolean visible = serverActive || (action != null && !action.isEmpty());

        if (visible) {
            if (!serverActive) {
                if (visibleTarget && displayedTargetPos != null && targetPos != null
                        && displayedTargetPos.distanceToSqr(targetPos) > 1.0E-4) {
                    progress = 0.0f;
                }
                displayedAction = action;
                displayedTargetPos = targetPos;
            } else if (displayedTargetPos == null) {
                displayedTargetPos = InteractionHintState.getAimedPos();
            }
        }
        visibleTarget = visible;

        long now = System.nanoTime();
        float delta = (now - lastNanoTime) / 1_000_000_000f;
        lastNanoTime = now;
        if (delta > 0.1f) {
            delta = 0.1f;
        }
        float step = delta / 0.25f;
        progress += visibleTarget ? step : -step;
        progress = Mth.clamp(progress, 0.0f, 1.0f);
    }

    private static float ease(float x) {
        return 1.0f - (float) Math.pow(1.0f - x, 3);
    }
}