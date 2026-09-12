package com.storyengine.trigger.event;

import com.storyengine.StoryEngineMod;
import com.storyengine.trigger.TriggerContext;
import com.storyengine.trigger.TriggerDefinition;
import com.storyengine.trigger.TriggerEvent;
import com.storyengine.trigger.TriggerExecutor;
import com.storyengine.trigger.TriggerManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Слушатель события {@code INTERACT_ENTITY} (ТЗ §4.2, §9.3, §10.4).
 *
 * На правый клик по сущности ищет триггеры INTERACT_ENTITY, у которых
 * {@code target_tag} присутствует на самой кликнутой сущности. Выполняется
 * ТОЛЬКО один триггер (правило §9.3): наибольший {@code priority}; список
 * от {@link TriggerManager#byEvent} уже отсортирован.
 *
 * При {@code cancel_vanilla: true} ванильное взаимодействие с сущностью
 * (например, седло/поводок/открытие сундука-вьючера) отменяется через
 * {@code PlayerInteractEvent.EntityInteract}.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntityInteractionTriggerListener {

    private EntityInteractionTriggerListener() {
    }

    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        Entity target = event.getTarget();

        TriggerManager manager = StoryEngineMod.TRIGGER_MANAGER;
        List<TriggerDefinition> candidates = new ArrayList<>(manager.byEvent(TriggerEvent.INTERACT_ENTITY));
        if (candidates.isEmpty() || target == null) {
            return;
        }

        candidates.removeIf(def -> def.targetTag() == null || !target.getTags().contains(def.targetTag()));
        if (candidates.isEmpty()) {
            return;
        }

        TriggerContext ctx = TriggerContext.builder(player, level, TriggerEvent.INTERACT_ENTITY)
                .targetEntity(target)
                .targetPos(target.blockPosition())
                .sourceEvent(event)
                .build();

        TriggerExecutor.executeFirstAvailable(ctx, candidates);
    }
}