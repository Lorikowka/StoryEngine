package com.storyengine.trigger;

import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Серверное состояние подсказки (ТЗ §10.3):
 * какой интеррактибль игрок «держит на прицеле» (блок или сущность) и
 * какой триггер этому соответствует.
 *
 * Сервер НЕ решает, что видит клиент (это передаёт клиент через
 * C2SCrosshairPacket); сервер решает, какой триггер применим к цели и
 * синхронизирует подсказку (S2CSyncTriggerHintsPacket).
 *
 * Пересинхронизация (resync) вызывается при:
 * - смене цели под прицелом (клиент шлёт C2SCrosshairPacket);
 * - изменении квеста/флагов/завершении диалога (актуализация §10.3);
 * - перезагрузке триггеров (этап 7, команда reload).
 */
public final class TriggerHintService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Последняя известная цель игрока (UUID → цель), для актуализации. */
    private final Map<java.util.UUID, CrosshairTarget> lastTargets = new HashMap<>();

    /** Радиус поиска сущности-маркера у блока под прицелом. */
    private static final double MARKER_SEARCH_RADIUS = 3.0D;

    private TriggerHintService() {
    }

    private static final TriggerHintService INSTANCE = new TriggerHintService();

    public static TriggerHintService get() {
        return INSTANCE;
    }

    /**
     * Обновление цели игрока с клиента (C2SCrosshairPacket). Клиент шлёт
     * либо id сущности, либо позицию блока под прицелом. {@code null}-целью
     * (оба поля пусты) означает «крестовина не на интерактибле».
     */
    public void updateTarget(ServerPlayer player, ServerLevel level,
                             @Nullable Integer entityId, @Nullable BlockPos blockPos) {
        CrosshairTarget target = new CrosshairTarget(entityId, blockPos);
        if (entityId == null && blockPos == null) {
            lastTargets.remove(player.getUUID());
        } else {
            lastTargets.put(player.getUUID(), target);
        }
        resync(player, level);
    }

    /**
     * Пересинхронизировать подсказку по последней известной цели игрока.
     * Вызывается серверными состояниями (квест/флаги/диалог) и reload.
     */
    public void resync(ServerPlayer player, ServerLevel level) {
        TriggerHint hint = resolveCurrent(player, level);
        TriggerNetworking.sendHints(player, hint);
    }

    /**
     * Текущая подсказка по последней известной цели игрока без отправки пакета
     * (для команды диагностики {@code /story trigger debug}).
     */
    @Nullable
    public TriggerHint resolveCurrent(ServerPlayer player, ServerLevel level) {
        CrosshairTarget target = lastTargets.get(player.getUUID());
        if (target == null) {
            return null;
        }
        return resolve(level, player, target);
    }

    /** Все включённые триггеры INTERACT_* (уже отсортированы по priority). */
    private List<TriggerDefinition> interactionTriggers() {
        List<TriggerDefinition> all = new java.util.ArrayList<>();
        all.addAll(StoryEngineMod.TRIGGER_MANAGER.byEvent(TriggerEvent.INTERACT_BLOCK));
        all.addAll(StoryEngineMod.TRIGGER_MANAGER.byEvent(TriggerEvent.INTERACT_ENTITY));
        return all;
    }

    /**
     * Подбор одного самого приоритетного подходящего триггера для цели
     * (правило §9.3 для interaction). {@code null}, если ни один триггер
     * не подходит или недоступен.
     */
    @Nullable
    private TriggerHint resolve(ServerLevel level, ServerPlayer player, CrosshairTarget target) {
        // Для INTERACT_BLOCK цель — маркер-сущность рядом с кликнутым блоком.
        if (target.blockPos != null) {
            List<TriggerDefinition> blockTriggers = StoryEngineMod.TRIGGER_MANAGER.byEvent(TriggerEvent.INTERACT_BLOCK);
            for (TriggerDefinition def : blockTriggers) {
                net.minecraft.world.entity.Entity marker = StoryEngineMod.TRIGGER_MANAGER.findTargetNear(
                        level, net.minecraft.world.phys.Vec3.atCenterOf(target.blockPos), def.targetTag(), MARKER_SEARCH_RADIUS);
                if (marker != null) {
                    if (TriggerExecutor.isAvailable(TriggerContext.builder(player, level, TriggerEvent.INTERACT_BLOCK).build(), def)) {
                        return TriggerHint.ok(def, marker);
                    }
                    return null;
                }
            }
        }
        // Для INTERACT_ENTITY цель — кликнутая сущность.
        if (target.entityId != null) {
            net.minecraft.world.entity.Entity clicked = level.getEntity(target.entityId);
            if (clicked != null) {
                List<TriggerDefinition> entityTriggers = StoryEngineMod.TRIGGER_MANAGER.byEvent(TriggerEvent.INTERACT_ENTITY);
                for (TriggerDefinition def : entityTriggers) {
                    if (def.targetTag() != null && clicked.getTags().contains(def.targetTag())) {
                        if (TriggerExecutor.isAvailable(TriggerContext.builder(player, level, TriggerEvent.INTERACT_ENTITY).build(), def)) {
                            return TriggerHint.ok(def, clicked);
                        }
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /** Цель под прицелом игрока. */
    private record CrosshairTarget(@Nullable Integer entityId, @Nullable BlockPos blockPos) {
    }

    /** Результат резолва для S2C: триггер и тег, чтобы клиент сверял. */
    public record TriggerHint(String triggerId, String targetTag, String hintLabel,
                             boolean cancelVanilla) {
        static TriggerHint ok(TriggerDefinition def, @Nullable net.minecraft.world.entity.Entity target) {
            return new TriggerHint(def.id(), def.targetTag(), def.hintLabel(), def.cancelVanilla());
        }
    }
}