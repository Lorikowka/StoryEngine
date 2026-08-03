package com.storyengine.player;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class PlayerQuestDataCapability {

    public static final Capability<IPlayerQuestData> QUEST_DATA = CapabilityManager.get(new CapabilityToken<>() {
    });

    private PlayerQuestDataCapability() {
    }
}
