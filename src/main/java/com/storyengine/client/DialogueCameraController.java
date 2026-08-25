package com.storyengine.client;

import com.storyengine.StoryEngineMod;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

import javax.annotation.Nullable;

/**
 * Подмена поворота камеры на время открытого {@link DialogueScreen}.
 *
 * Камера чисто клиентская: мы не двигаем игрока, только плавно интерполируем
 * поворот (yaw/pitch) камеры в сторону NPC. При закрытии диалога (или пропаже
 * NPC) поворот плавно возвращается к обычному "из-за спины игрока".
 *
 * См. дизайн-документ DIALOGUE_CAMERA.md §7.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DialogueCameraController {

    private static Vec3 targetPosition; // null = нет привязки к NPC
    private static float blend; // 0.0 = обычная камера, 1.0 = полностью на NPC
    private static final float BLEND_SPEED = 0.08F; // за кадр, подобрать на глаз

    private DialogueCameraController() {
    }

    public static void setTarget(@Nullable Vec3 npcPosition) {
        targetPosition = npcPosition;
    }

    public static void clear() {
        targetPosition = null;
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        boolean dialogueOpen = mc.screen instanceof DialogueScreen;
        float targetBlend = (dialogueOpen && targetPosition != null) ? 1.0F : 0.0F;

        if (targetBlend > blend) {
            blend = Math.min(1.0F, blend + BLEND_SPEED);
        } else {
            blend = Math.max(0.0F, blend - BLEND_SPEED);
        }
        blend = Mth.clamp(blend, 0.0F, 1.0F);

        if (blend <= 0.0F || targetPosition == null) {
            return; // обычная камера, ничего не трогаем
        }

        Vec3 camPos = event.getCamera().getPosition();
        double dx = targetPosition.x - camPos.x;
        double dy = targetPosition.y - camPos.y;
        double dz = targetPosition.z - camPos.z;

        // Направление от камеры к NPC (в радианах, конвенция Minecraft).
        float targetYaw = (float) Math.atan2(-dx, -dz);
        float targetPitch = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));

        float currentYaw = event.getYaw();
        float currentPitch = event.getPitch();

        // Кратчайший путь по yaw, чтобы не было лишних оборотов через границу.
        float deltaYaw = wrapAngleRad(targetYaw - currentYaw);
        float blendedYaw = currentYaw + deltaYaw * blend;
        float blendedPitch = currentPitch + (targetPitch - currentPitch) * blend;

        event.setYaw(blendedYaw);
        event.setPitch(blendedPitch);
    }

    /** Нормализует угол в радианах в диапазон (-pi, pi]. */
    private static float wrapAngleRad(float angle) {
        float pi = (float) Math.PI;
        float a = (angle + pi) % (2.0F * pi);
        if (a < 0.0F) {
            a += 2.0F * pi;
        }
        return a - pi;
    }
}
