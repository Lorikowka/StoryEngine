package com.storyengine.interaction.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.storyengine.StoryEngineMod;
import com.storyengine.interaction.client.InteractionClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3D-подсветка контура целевого блока (см. спецификацию §4.2).
 *
 * Рисуется через RenderLevelStageEvent поверх мира (после твёрдых блоков),
 * чтобы контур был виден даже сквозь преграды (мягкая неоновая подсветка).
 * Цвет берётся из trigger.outlineColor.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TriggerOutlineRenderer {

    private TriggerOutlineRenderer() {
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

        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)
                .inflate(0.005);

        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        var consumer = buffers.getBuffer(RenderType.lines());
        // Толщина линии (на большинстве GPU игнорируется для lines(), но задаём).
        com.mojang.blaze3d.systems.RenderSystem.lineWidth(2.0f);
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        LevelRenderer.renderLineBox(pose, consumer, box, r, g, b, a);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        buffers.endBatch(RenderType.lines());

        pose.popPose();
    }
}
