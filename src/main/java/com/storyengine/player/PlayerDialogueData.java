package com.storyengine.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Данные диалоговой сессии игрока на стороне сервера:
 *  - flags - именованные булевы флаги (персистентные, переживают смерть/перезагрузку);
 *  - активная сессия (DialogueSession) НЕ хранится здесь - она эфемерна
 *    и лежит в Map в DialogueManager.
 *
 * Зеркало PlayerQuestData, но минимальное (только флаги).
 */
public class PlayerDialogueData implements INBTSerializable<CompoundTag> {

    private final Map<String, Boolean> flags = new LinkedHashMap<>();

    public boolean getFlag(String name) {
        return flags.getOrDefault(name, false);
    }

    public void setFlag(String name, boolean value) {
        if (!value) {
            flags.remove(name);
        } else {
            flags.put(name, true);
        }
    }

    public Map<String, Boolean> getFlags() {
        return Collections.unmodifiableMap(flags);
    }

    public void copyFrom(PlayerDialogueData other) {
        flags.clear();
        flags.putAll(other.flags);
    }

    /** Без синхронизации: флаги читаются/пишутся сервером, клиенту не нужны напрямую. */
    public static PlayerDialogueData get(Player player) {
        return player.getCapability(PlayerDialogueDataCapability.DIALOGUE_DATA)
                .orElseThrow(() -> new IllegalStateException(
                        "Dialogue data capability is missing for player " + player.getName().getString()));
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag root = new CompoundTag();
        ListTag flagList = new ListTag();
        flags.forEach((name, value) -> {
            if (value) {
                flagList.add(StringTag.valueOf(name));
            }
        });
        root.put("flags", flagList);
        return root;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        flags.clear();
        if (nbt.contains("flags")) {
            ListTag flagList = nbt.getList("flags", Tag.TAG_STRING);
            for (int i = 0; i < flagList.size(); i++) {
                flags.put(flagList.getString(i), true);
            }
        }
    }
}
