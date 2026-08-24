package com.storyengine.player;

import com.storyengine.StoryEngineMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.resources.ResourceLocation;

public class PlayerDialogueDataProvider implements ICapabilitySerializable<CompoundTag> {

    public static final ResourceLocation ID = new ResourceLocation(StoryEngineMod.MOD_ID, "player_dialogue_data");

    private final PlayerDialogueData data = new PlayerDialogueData();
    private final LazyOptional<PlayerDialogueData> optional = LazyOptional.of(() -> data);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        return PlayerDialogueDataCapability.DIALOGUE_DATA.orEmpty(capability, optional.cast());
    }

    @Override
    public CompoundTag serializeNBT() {
        return ((INBTSerializable<CompoundTag>) data).serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        ((INBTSerializable<CompoundTag>) data).deserializeNBT(nbt);
    }
}
