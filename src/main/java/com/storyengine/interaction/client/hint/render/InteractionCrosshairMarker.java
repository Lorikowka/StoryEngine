package com.storyengine.interaction.client.hint.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.storyengine.StoryEngineMod;
import com.storyengine.client.MenuCustomizationConfig;
import com.storyengine.interaction.client.hint.InteractionHintState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Мировые кольца-маркеры у близких «интерактиблов» (порт CrosshairRenderer из
 * Interaction Hint, но без текстур-оригинала: кольцо и «заливка» рисуются
 * процедурно, треугольными секторами формата POSITION_COLOR).
 *
 * Все кольца — билборды, аддитивное свечение через {@link RenderType#lightning()}
 * с отключённым тестом глубины (видны сквозь преграды — как в оригинале).
 * Кольцо под крестовиной подсвечивается, и его центр заполняется диском,
 * «растущим» вместе с анимацией HUD ({@link InteractionHintHud#getAnim()}).
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class InteractionCrosshairMarker {

    private static final float RING_OUTER = 0.42f;
    private static final float RING_INNER = 0.36f;
    private static final float FILL_MAX = 0.30f;
    private static final float FILL_START_SCALE = 0.01f;
    private static final int SEGMENTS = 24;

    private InteractionCrosshairMarker() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!MenuCustomizationConfig.interactionHintEnabled()) {
            return;
        }
        List<Vec3> targets = InteractionHintState.getNearbyTargets();
        if (targets.isEmpty()) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        Vec3 aimedTarget = InteractionHintHud.getDisplayedTargetPos();
        float anim = InteractionHintHud.getAnim();

        int idleArgb = MenuCustomizationConfig.hintMarker();
        int aimedArgb = MenuCustomizationConfig.hintMarkerAimed();
        float idleA = ((idleArgb >>> 24) & 0xFF) / 255.0f;
        float idR = ((idleArgb >>> 16) & 0xFF) / 255.0f;
        float idG = ((idleArgb >>> 8) & 0xFF) / 255.0f;
        float idB = (idleArgb & 0xFF) / 255.0f;
        float aimA0 = ((aimedArgb >>> 24) & 0xFF) / 255.0f;
        float aiR = ((aimedArgb >>> 16) & 0xFF) / 255.0f;
        float aiG = ((aimedArgb >>> 8) & 0xFF) / 255.0f;
        float aiB = (aimedArgb & 0xFF) / 255.0f;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.disableDepthTest();
        try {
            var consumer = buffers.getBuffer(RenderType.lightning());
            for (Vec3 target : targets) {
                boolean isAimed = aimedTarget != null
                        && target.distanceToSqr(aimedTarget) < 1.0E-4;

                Vec3 up = new Vec3(0, 1, 0);
                Vec3 toCam = cam.subtract(target).normalize();
                Vec3 right = toCam.cross(up).normalize();
                Vec3 trueUp = right.cross(toCam).normalize();

                if (isAimed) {
                    float ringAlpha = Math.min(1.0f, 0.55f + anim * 0.45f) * aimA0;
                    ring(consumer, target, right, trueUp, RING_INNER, RING_OUTER,
                            aiR, aiG, aiB, ringAlpha, SEGMENTS);
                    float fillScale = FILL_START_SCALE + (1.0f - FILL_START_SCALE) * anim;
                    disc(consumer, target, right, trueUp, FILL_MAX * fillScale,
                            aiR, aiG, aiB, anim, SEGMENTS);
                } else {
                    ring(consumer, target, right, trueUp, RING_INNER, RING_OUTER,
                            idR, idG, idB, idleA, SEGMENTS);
                }
            }
            buffers.endBatch(RenderType.lightning());
        } finally {
            RenderSystem.enableDepthTest();
        }

        pose.popPose();
    }

    /** Кольцо (труба между inner и outer радиусами), UV-независимая геометрия. */
    private static void ring(VertexConsumer consumer, Vec3 center, Vec3 right, Vec3 up,
                             float inner, float outer,
                             float r, float g, float b, float a, int segments) {
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments * (float) (Math.PI * 2);
            float t2 = (float) (i + 1) / segments * (float) (Math.PI * 2);
            float c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
            float c2 = (float) Math.cos(t2), s2 = (float) Math.sin(t2);

            Vec3 p1i = center.add(right.scale(c1 * inner)).add(up.scale(s1 * inner));
            Vec3 p1o = center.add(right.scale(c1 * outer)).add(up.scale(s1 * outer));
            Vec3 p2o = center.add(right.scale(c2 * outer)).add(up.scale(s2 * outer));
            Vec3 p2i = center.add(right.scale(c2 * inner)).add(up.scale(s2 * inner));

            vertex(consumer, p1i, r, g, b, a);
            vertex(consumer, p1o, r, g, b, a);
            vertex(consumer, p2o, r, g, b, a);
            vertex(consumer, p1i, r, g, b, a);
            vertex(consumer, p2o, r, g, b, a);
            vertex(consumer, p2i, r, g, b, a);
        }
    }

    /** Заполненный диск (центр-заливка прицельной цели). */
    private static void disc(VertexConsumer consumer, Vec3 center, Vec3 right, Vec3 up,
                             float radius, float r, float g, float b, float a, int segments) {
        if (radius <= 0.0f) {
            return;
        }
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments * (float) (Math.PI * 2);
            float t2 = (float) (i + 1) / segments * (float) (Math.PI * 2);
            float c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
            float c2 = (float) Math.cos(t2), s2 = (float) Math.sin(t2);

            Vec3 p1 = center.add(right.scale(c1 * radius)).add(up.scale(s1 * radius));
            Vec3 p2 = center.add(right.scale(c2 * radius)).add(up.scale(s2 * radius));

            vertex(consumer, center, r, g, b, a);
            vertex(consumer, p1, r, g, b, a);
            vertex(consumer, p2, r, g, b, a);
        }
    }

    private static void vertex(VertexConsumer consumer, Vec3 p, float r, float g, float b, float a) {
        consumer.vertex((float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).endVertex();
    }
}