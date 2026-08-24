package com.storyengine.interaction.client;

import com.storyengine.interaction.data.InteractionTrigger;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Отслеживание взгляда игрока в ClientTick (см. спецификацию §4.1).
 *
 * Рейкаст выполняется вручную от глаз игрока вдоль вектора взгляда с шагом 0.1
 * на дистанцию до 8 блоков (покрывает trigger.maxDistance, который может быть
 * больше обычного радиуса взаимодействия). Первый попавший под прицел блок с
 * зарегистрированным InteractionTrigger и в пределах maxDistance становится
 * ActiveTarget. При отсутствии цели — ActiveTarget сбрасывается.
 */
@Mod.EventBusSubscriber(modid = com.storyengine.StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class InteractionRaycastHandler {

    /** Максимальная длина луча (блоков). */
    private static final double MAX_RAY = 8.0;
    /** Шаг рейкаста (блоков), по спецификации §4.1. */
    private static final double STEP = 0.1;

    private InteractionRaycastHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            InteractionClientState.clearActiveTarget();
            return;
        }
        // Меню взаимодействия не показываем поверх других экранов (диалог, меню квестов).
        if (mc.screen != null) {
            InteractionClientState.clearActiveTarget();
            return;
        }

        Player player = mc.player;
        Level level = mc.level;
        ResourceLocation dim = level.dimension().location();

        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);

        InteractionTrigger found = null;
        BlockPos foundPos = null;
        double foundDist = Double.MAX_VALUE;

        for (double t = 0.0; t <= MAX_RAY; t += STEP) {
            Vec3 point = eye.add(look.scale(t));
            BlockPos pos = new BlockPos((int) Math.floor(point.x), (int) Math.floor(point.y), (int) Math.floor(point.z));
            InteractionTrigger trigger = InteractionClientState.findAt(dim, pos);
            if (trigger != null) {
                // Проверяем, что блок не занавешен: учитываем только первое попадание
                // (луч идёт изнутри-наружу, первый триггер и есть ближайший).
                double dist = Math.sqrt(eye.distanceToSqr(Vec3.atCenterOf(pos)));
                if (dist <= trigger.getMaxDistance() + 0.6) {
                    found = trigger;
                    foundPos = pos;
                    foundDist = dist;
                }
                break;
            }
        }

        if (found != null) {
            InteractionClientState.setActiveTarget(found, foundPos);
        } else {
            InteractionClientState.clearActiveTarget();
        }
    }

    /** Перезагрузка локальных триггеров при (пере)входе на сервер (клиент). */
    @SubscribeEvent
    public static void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        InteractionClientState.loadFromConfig();
    }
}
