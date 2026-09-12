/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiComponent
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.event.RenderGuiOverlayEvent$Post
 *  net.minecraftforge.client.gui.overlay.VanillaGuiOverlay
 */
package com.vaxxdev.interactionhint.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vaxxdev.interactionhint.InteractionManager;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

public class HudRenderer {
    private static final int OFFSET_X = 540;
    private static final int OFFSET_Y = 20;
    private static final int PADDING_X = 26;
    private static final int PADDING_Y = 8;
    private static final double REFERENCE_GUI_SCALE = 3.0;
    private static final float SIZE_SCALE = 0.85f;
    private static final float ANIM_DURATION = 0.25f;
    public static int backgroundColorRGB = 0;
    public static float backgroundMaxAlpha = 0.85f;
    public static float textMaxAlpha = 1.0f;
    public static boolean useTextureBackground = true;
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("interactionhint", "textures/gui/background_ih.png");
    private static float progress = 0.0f;
    private static boolean visibleTarget = false;
    private static String displayedAction = "";
    private static Vec3 displayedTargetPos = null;
    private static long lastNanoTime = System.nanoTime();

    public static void tick() {
        boolean visible;
        String action = InteractionManager.currentAction;
        Vec3 targetPos = InteractionManager.currentTargetPos;
        boolean bl = visible = action != null && !action.isEmpty();
        if (visible) {
            if (visibleTarget && displayedTargetPos != null && targetPos != null && displayedTargetPos.m_82557_(targetPos) > 1.0E-4) {
                progress = 0.0f;
            }
            displayedAction = action;
            displayedTargetPos = targetPos;
        }
        visibleTarget = visible;
    }

    private static float ease(float x) {
        return 1.0f - (float)Math.pow(1.0f - x, 3.0);
    }

    public static float getAnim() {
        return HudRenderer.ease(progress);
    }

    public static Vec3 getDisplayedTargetPos() {
        return displayedTargetPos;
    }

    public static boolean isActive() {
        return progress > 0.001f && !displayedAction.isEmpty();
    }

    private static void updateProgress() {
        long now = System.nanoTime();
        float delta = (float)(now - lastNanoTime) / 1.0E9f;
        lastNanoTime = now;
        if (delta > 0.1f) {
            delta = 0.1f;
        }
        float step = delta / 0.25f;
        progress += visibleTarget ? step : -step;
        progress = Mth.m_14036_((float)progress, (float)0.0f, (float)1.0f);
    }

    public static void render(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null || mc.f_91073_ == null) {
            return;
        }
        HudRenderer.updateProgress();
        if (progress <= 0.001f || displayedAction.isEmpty()) {
            return;
        }
        float anim = HudRenderer.ease(progress);
        Font font = mc.f_91062_;
        MutableComponent text = Component.m_237113_((String)"[").m_7220_((Component)Component.m_237113_((String)"F").m_130948_(Style.f_131099_.m_131136_(Boolean.valueOf(true)))).m_130946_("] ").m_7220_((Component)Component.m_237115_((String)displayedAction));
        int textWidth = font.m_92852_((FormattedText)text);
        Objects.requireNonNull(font);
        int textHeight = 9;
        float scale = 0.97f + anim * 0.03f;
        float slide = (1.0f - anim) * 8.0f;
        int screenWidth = mc.m_91268_().m_85445_();
        int screenHeight = mc.m_91268_().m_85446_();
        double currentGuiScale = mc.m_91268_().m_85449_();
        float guiScaleCompensation = (float)(3.0 / currentGuiScale) * 0.85f;
        float safeMargin = 4.0f;
        float maxCompensationX = ((float)screenWidth - safeMargin) / (float)(textWidth + 540 + 26);
        float maxCompensationY = ((float)screenHeight - safeMargin) / 28.0f;
        float maxCompensation = Math.min(maxCompensationX, maxCompensationY);
        if (maxCompensation > 0.0f) {
            guiScaleCompensation = Math.min(guiScaleCompensation, maxCompensation);
        }
        guiScaleCompensation = Math.max(guiScaleCompensation, 0.05f);
        float x = (float)(screenWidth - textWidth - 540) + slide;
        float y = screenHeight - 20;
        int bgX = (int)x - 26;
        int bgY = (int)y - 8;
        int bgWidth = textWidth + 52;
        int bgHeight = textHeight + 16;
        PoseStack pose = event.getPoseStack();
        pose.m_85836_();
        pose.m_85837_((double)screenWidth, (double)screenHeight, 0.0);
        pose.m_85841_(guiScaleCompensation, guiScaleCompensation, 1.0f);
        pose.m_85837_((double)(-screenWidth), (double)(-screenHeight), 0.0);
        pose.m_85836_();
        pose.m_85837_((double)(x + (float)textWidth / 2.0f), (double)(y + (float)textHeight / 2.0f), 0.0);
        pose.m_85841_(scale, scale, 1.0f);
        pose.m_85837_((double)(-(x + (float)textWidth / 2.0f)), (double)(-(y + (float)textHeight / 2.0f)), 0.0);
        RenderSystem.m_69478_();
        RenderSystem.m_69453_();
        RenderSystem.m_69465_();
        if (useTextureBackground) {
            float bgAlpha = Mth.m_14036_((float)(backgroundMaxAlpha * anim), (float)0.0f, (float)1.0f);
            RenderSystem.m_157427_(GameRenderer::m_172817_);
            RenderSystem.m_157456_((int)0, (ResourceLocation)BACKGROUND_TEXTURE);
            RenderSystem.m_157429_((float)1.0f, (float)1.0f, (float)1.0f, (float)bgAlpha);
            GuiComponent.m_93160_((PoseStack)pose, (int)bgX, (int)bgY, (int)bgWidth, (int)bgHeight, (float)0.0f, (float)0.0f, (int)1, (int)1, (int)1, (int)1);
            RenderSystem.m_157429_((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        } else {
            int bgAlphaInt = (int)(64.0f * backgroundMaxAlpha * anim);
            int bgColor = bgAlphaInt << 24 | backgroundColorRGB & 0xFFFFFF;
            GuiComponent.m_93172_((PoseStack)pose, (int)bgX, (int)bgY, (int)(bgX + bgWidth), (int)(bgY + bgHeight), (int)bgColor);
        }
        RenderSystem.m_69478_();
        RenderSystem.m_69453_();
        float textAlpha = textMaxAlpha * anim;
        RenderSystem.m_157429_((float)1.0f, (float)1.0f, (float)1.0f, (float)textAlpha);
        font.m_92889_(pose, (Component)text, x, y, -1);
        RenderSystem.m_157429_((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.m_69482_();
        RenderSystem.m_69461_();
        pose.m_85849_();
        pose.m_85849_();
    }
}
