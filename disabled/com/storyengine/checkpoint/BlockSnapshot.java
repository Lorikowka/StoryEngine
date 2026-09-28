package com.storyengine.checkpoint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/**
 * Снимок одного блока сцены: состояние блока ({@code NbtUtils.writeBlockState})
 * и, если есть, полные данные BlockEntity ({@code saveWithId()}).
 *
 * Позиция хранится отдельно, чтобы блоки была просто отобрать по ключу
 * {@code Map<BlockPos, BlockSnapshot>} (см. {@link SceneSnapshot}).
 */
public record BlockSnapshot(
        BlockPos pos,
        CompoundTag state,
        @Nullable CompoundTag blockEntity) {
}