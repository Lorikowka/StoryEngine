package com.storyengine.interaction.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.storyengine.StoryEngineMod;
import com.storyengine.interaction.client.InteractionClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Подсветка целевого блока триггера в виде светящегося билборда («партикла»),
 * который парит чуть спереди блока (со стороны игрока).
 *
 * Рисуется в RenderLevelStageEvent поверх мира (после твёрдых блоков) с
 * отключённым тестом глубины, поэтому маркер виден сквозь преграды. Аддитивное
 * свечение обеспечивает RenderType.lightning() (формат позиция+цвет, треугольники).
 * Цвет берётся из trigger.outlineColor.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TriggerMarkerRenderer {

    private TriggerMarkerRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        if (!InteractionClientState.hasActiveTarget()) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        BlockPos pos = InteractionClientState.getActivePos();
        if (pos == null) {
            return;
        }

        int color = InteractionClientState.getActiveTarget().parseOutlineColor();
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        var cam = event.getCamera().getPosition();
        pose.translate(-cam.x, -cam.y, -cam.z);

        // Центр блока, смещённый к игроку (чуть спереди блока).
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3 toCam = cam.subtract(center).normalize();
        center = center.add(toCam.scale(0.6));

        // Камера-ориентированный билборд.
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = toCam.cross(up).normalize();
        Vec3 trueUp = right.cross(toCam).normalize();

        double t = Minecraft.getInstance().level != null
                ? (Minecraft.getInstance().level.getGameTime() % 1000) / 4.0 : 0.0;
        double pulse = 1.0 + 0.18 * Math.sin(t);
        double half = 0.11 * pulse;

        Vec3 c1 = center.add(right.scale(-half)).add(trueUp.scale(-half));
        Vec3 c2 = center.add(right.scale(half)).add(trueUp.scale(-half));
        Vec3 c3 = center.add(right.scale(half)).add(trueUp.scale(half));
        Vec3 c4 = center.add(right.scale(-half)).add(trueUp.scale(half));

        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.disableDepthTest();
        try {
            var consumer = buffers.getBuffer(RenderType.lightning());
            // Два треугольника (лицом к камере).
            vertex(consumer, c1, r, g, b, a);
            vertex(consumer, c2, r, g, b, a);
            vertex(consumer, c3, r, g, b, a);
            vertex(consumer, c1, r, g, b, a);
            vertex(consumer, c3, r, g, b, a);
            vertex(consumer, c4, r, g, b, a);
            buffers.endBatch(RenderType.lightning());
        } finally {
            RenderSystem.enableDepthTest();
        }

        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, Vec3 p, float r, float g, float b, float a) {
        consumer.vertex((float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).endVertex();
    }
}
