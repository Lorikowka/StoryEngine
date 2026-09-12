/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.math.Matrix4f
 *  net.minecraft.client.Camera
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 */
package com.vaxxdev.interactionhint.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.vaxxdev.interactionhint.InteractionManager;
import com.vaxxdev.interactionhint.client.HudRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

public class CrosshairRenderer {
    private static final float RING_SIZE = 0.5f;
    private static final float FILL_SIZE = 0.3f;
    private static final float FILL_START_SCALE = 0.01f;
    private static final ResourceLocation RING_TEXTURE = new ResourceLocation("interactionhint", "textures/gui/crosshair_ring_ih.png");
    private static final ResourceLocation FILL_TEXTURE = new ResourceLocation("interactionhint", "textures/gui/crosshair_fill_ih.png");
    private static final float IDLE_RING_ALPHA = 0.55f;

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (InteractionManager.nearbyTargets.isEmpty()) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 camPos = camera.m_90583_();
        Vec3 aimedTarget = HudRenderer.getDisplayedTargetPos();
        float anim = HudRenderer.getAnim();
        PoseStack pose = event.getPoseStack();
        RenderSystem.m_69478_();
        RenderSystem.m_69453_();
        RenderSystem.m_69465_();
        RenderSystem.m_69464_();
        RenderSystem.m_157427_(GameRenderer::m_172817_);
        for (Vec3 target : InteractionManager.nearbyTargets) {
            boolean isAimed = aimedTarget != null && target.m_82557_(aimedTarget) < 1.0E-4;
            pose.m_85836_();
            pose.m_85837_(target.f_82479_ - camPos.f_82479_, target.f_82480_ - camPos.f_82480_, target.f_82481_ - camPos.f_82481_);
            pose.m_85845_(camera.m_90591_());
            if (isAimed) {
                float ringAlpha = Mth.m_14036_((float)(0.55f + anim * 0.45f), (float)0.0f, (float)1.0f);
                CrosshairRenderer.drawBillboardQuad(pose, RING_TEXTURE, 0.5f, ringAlpha);
                float fillScale = 0.01f + 0.99f * anim;
                CrosshairRenderer.drawBillboardQuad(pose, FILL_TEXTURE, 0.3f * fillScale, anim);
            } else {
                CrosshairRenderer.drawBillboardQuad(pose, RING_TEXTURE, 0.5f, 0.55f);
            }
            pose.m_85849_();
        }
        RenderSystem.m_157429_((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.m_69481_();
        RenderSystem.m_69482_();
    }

    private static void drawBillboardQuad(PoseStack pose, ResourceLocation texture, float size, float alpha) {
        if (alpha <= 0.001f || size <= 0.0f) {
            return;
        }
        RenderSystem.m_157456_((int)0, (ResourceLocation)texture);
        RenderSystem.m_157429_((float)1.0f, (float)1.0f, (float)1.0f, (float)Mth.m_14036_((float)alpha, (float)0.0f, (float)1.0f));
        float half = size / 2.0f;
        Matrix4f matrix = pose.m_85850_().m_85861_();
        BufferBuilder buffer = Tesselator.m_85913_().m_85915_();
        buffer.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85817_);
        buffer.m_85982_(matrix, -half, -half, 0.0f).m_7421_(0.0f, 1.0f).m_5752_();
        buffer.m_85982_(matrix, half, -half, 0.0f).m_7421_(1.0f, 1.0f).m_5752_();
        buffer.m_85982_(matrix, half, half, 0.0f).m_7421_(1.0f, 0.0f).m_5752_();
        buffer.m_85982_(matrix, -half, half, 0.0f).m_7421_(0.0f, 0.0f).m_5752_();
        Tesselator.m_85913_().m_85914_();
    }
}
