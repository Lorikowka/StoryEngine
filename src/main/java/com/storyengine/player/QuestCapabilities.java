package com.storyengine.player;

import com.storyengine.StoryEngineMod;
import com.storyengine.network.QuestNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestCapabilities {

    private QuestCapabilities() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(PlayerQuestDataProvider.ID, new PlayerQuestDataProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            QuestNetworking.syncToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(PlayerQuestDataCapability.QUEST_DATA).ifPresent(oldData ->
                event.getEntity().getCapability(PlayerQuestDataCapability.QUEST_DATA).ifPresent(newData -> {
                    if (oldData instanceof PlayerQuestData oldQuestData && newData instanceof PlayerQuestData newQuestData) {
                        newQuestData.copyFrom(oldQuestData);
                    }
                })
        );
    }
}
