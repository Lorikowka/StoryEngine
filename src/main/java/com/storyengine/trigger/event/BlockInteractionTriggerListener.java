package com.storyengine.trigger.event;

import com.storyengine.StoryEngineMod;
import com.storyengine.trigger.TriggerContext;
import com.storyengine.trigger.TriggerDefinition;
import com.storyengine.trigger.TriggerEvent;
import com.storyengine.trigger.TriggerExecutor;
import com.storyengine.trigger.TriggerManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Слушатель события {@code INTERACT_BLOCK} (ТЗ §4.2, §9.3, §10.4).
 *
 * На правый клик по блоку ищет триггеры INTERACT_BLOCK, у которых сущность
 * с {@code target_tag} есть в радиусе от кликнутой позиции (маркер у блока,
 * §5.2). Выполняется ТОЛЬКО один триггер (правило §9.3): наибольший
 * {@code priority}, при равенстве — порядок по {@code id} (список от
 * {@link TriggerManager#byEvent} уже отсортирован).
 *
 * При {@code cancel_vanilla: true} ванильное взаимодействие с блоком
 * отменяется через {@code PlayerInteractEvent.RightClickBlock}.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlockInteractionTriggerListener {

    /** Радиус поиска сущности-цели (маркера) от кликнутой позиции. */
    private static final double TARGET_SEARCH_RADIUS = 3.0D;

    private BlockInteractionTriggerListener() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        Vec3 center = Vec3.atCenterOf(pos);

        TriggerManager manager = StoryEngineMod.TRIGGER_MANAGER;
        List<TriggerDefinition> candidates = new ArrayList<>(manager.byEvent(TriggerEvent.INTERACT_BLOCK));
        if (candidates.isEmpty()) {
            return;
        }

        Entity targetEntity = null;
        candidates = new ArrayList<>(candidates);
        var it = candidates.iterator();
        while (it.hasNext()) {
            TriggerDefinition def = it.next();
            Entity target = manager.findTargetNear(level, center, def.targetTag(), TARGET_SEARCH_RADIUS);
            if (target == null) {
                it.remove();
                continue;
            }
            if (targetEntity == null) {
                targetEntity = target;
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        TriggerContext ctx = TriggerContext.builder(player, level, TriggerEvent.INTERACT_BLOCK)
                .targetPos(pos)
                .targetEntity(targetEntity)
                .sourceEvent(event)
                .build();

        TriggerExecutor.executeFirstAvailable(ctx, candidates);
    }
}