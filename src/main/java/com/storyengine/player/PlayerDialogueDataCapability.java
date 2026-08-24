package com.storyengine.player;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class PlayerDialogueDataCapability {

    public static final Capability<PlayerDialogueData> DIALOGUE_DATA = CapabilityManager.get(new CapabilityToken<>() {
    });

    private PlayerDialogueDataCapability() {
    }
}
