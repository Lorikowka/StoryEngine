package com.storyengine.checkpoint;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Снимок сюжетного состояния игрока: измерение, позиция, поворот и
 * сериализованные капабилити игрока (флаги диалогов/триггеров
 * {@code PlayerDialogueData.serializeNBT()} и прогресс квестов
 * {@code PlayerQuestDataHelper.serializeNbt()}).
 *
 * При restore применяется только к игроку, инициировавшему откат.
 *
 * @param dimension измерение, в котором находился игрок
 * @param pos       позиция {@code [x, y, z]}
 * @param rot       поворот {@code [yaw, pitch]}
 * @param flags     NBT флагов {@code PlayerDialogueData} (никогда не null)
 * @param quests    NBT прогресса квестов (nullable — только если капабилити доступна)
 */
public record PlayerSnapshot(
        ResourceKey<Level> dimension,
        double[] pos,
        float[] rot,
        CompoundTag flags,
        @Nullable CompoundTag quests) {
}