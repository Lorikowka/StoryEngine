package com.storyengine.player;

import com.storyengine.StoryEngineMod;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Регистрирует Capability PlayerDialogueData (хранит флаги диалогов).
 * Зеркало QuestCapabilityRegistrar.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DialogueCapabilityRegistrar {

    private DialogueCapabilityRegistrar() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerDialogueData.class);
    }
}
