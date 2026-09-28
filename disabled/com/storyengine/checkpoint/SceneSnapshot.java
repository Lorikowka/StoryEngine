package com.storyengine.checkpoint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Один снапшот сцены: состояние блоков, сюжетных сущностей и игрока.
 *
 * Неявного rollback всего мира нет — только то, что попало в снапшот.
 * Создаётся через {@link SceneSnapshotManager#capture}, применяется через
 * {@link SceneSnapshotManager#restore}.
 *
 * @param id           уникальный id (имя файла {@code config/story_engine/checkpoints/<id>.json})
 * @param dimension    измерение сцены (все позиции — в координатах этого измерения)
 * @param regionCenter центр региона отката {@code [x, y, z]} (нужно для поиска
 *                     «устаревших» дубликатов сущностей при restore); null — регион не задан
 * @param regionRadius радиус региона отката вокруг {@code regionCenter}
 * @param blocks       блоки сцены: ключи — позиции (упорядочены)
 * @param entities     сюжетные сущности (NPC/декор)
 * @param player       снимок инициировавшего сохранение игрока; пусто — не сохранять
 */
public record SceneSnapshot(
        String id,
        ResourceKey<Level> dimension,
        @Nullable double[] regionCenter,
        double regionRadius,
        Map<BlockPos, BlockSnapshot> blocks,
        List<EntitySnapshot> entities,
        Optional<PlayerSnapshot> player) {
}