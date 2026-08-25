package com.storyengine.player;

import com.storyengine.StoryEngineMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DialogueCapabilities {

    private DialogueCapabilities() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(PlayerDialogueDataProvider.ID, new PlayerDialogueDataProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(PlayerDialogueDataCapability.DIALOGUE_DATA).ifPresent(oldData ->
                event.getEntity().getCapability(PlayerDialogueDataCapability.DIALOGUE_DATA).ifPresent(newData ->
                        newData.copyFrom(oldData)
                )
        );
    }
}
