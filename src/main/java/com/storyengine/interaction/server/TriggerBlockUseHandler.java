package com.storyengine.interaction.server;

import com.storyengine.StoryEngineMod;
import com.storyengine.interaction.data.InteractionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Блокирует обычное (ванильное) использование блоков-триггеров во ВСЕХ
 * игровых режимах. Правый клик по такому блоку отменяется, поэтому дверь/сундук/
 * калитка и т.п. активируются только через интерактивный триггер (клавиша F),
 * а не прямым кликом.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = { Dist.CLIENT, Dist.DEDICATED_SERVER })
public final class TriggerBlockUseHandler {

    private TriggerBlockUseHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel)) {
            return;
        }
        BlockPos pos = event.getPos();
        ResourceLocation dim = event.getLevel().dimension().location();
        InteractionTrigger trigger = StoryEngineMod.TRIGGER_MANAGER.getTrigger(dim, pos);
        if (trigger != null && trigger.isEnabled()) {
            event.setCanceled(true);
        }
    }
}
