package com.storyengine.checkpoint;

import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * Снимок сюжетной сущности сцены (NPC и декорации).
 *
 * Полный NBT сущности взят через {@code Entity.saveWithoutId()} (включает
 * Pos/Rotation/Tags/UUID/данные конкретного типа). Позиция и поворот дублируются
 * в полях {@code pos}/{@code rot} только для читабельности JSON-файла —
 * восстановление идёт исключительно из {@code nbt}.
 *
 * @param typeId зарегистрированное имя типа (например {@code minecraft:armor_stand})
 * @param tags   scoreboard-теги сущности (извлечены из NBT для удобства)
 * @param pos    позиция {@code [x, y, z]}
 * @param rot    поворот {@code [yaw, pitch]}
 * @param nbt    полное состояние сущности {@code saveWithoutId()}
 */
public record EntitySnapshot(
        String typeId,
        List<String> tags,
        double[] pos,
        float[] rot,
        CompoundTag nbt) {
}