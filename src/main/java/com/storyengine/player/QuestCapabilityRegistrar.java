package com.storyengine.player;

import com.storyengine.StoryEngineMod;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Регистрирует тип Capability IPlayerQuestData через RegisterCapabilitiesEvent.
 * Требуется на Forge 43.2.0+, где эта регистрация заменила устаревший
 * статический @CapabilityInject. Без неё капабилити может работать
 * нестабильно/не резолвиться корректно у ICapabilityProvider.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class QuestCapabilityRegistrar {

    private QuestCapabilityRegistrar() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerQuestData.class);
    }
}
