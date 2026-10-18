package com.storyengine.interaction.server;

import com.storyengine.StoryEngineMod;
import com.storyengine.interaction.InteractionBlockLists;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Серверная отмена взаимодействия для блоков из списка {@code no_interact}
 * файла {@code config/story_engine/interaction.json}: ПКМ и F по этим блокам
 * не дают эффекта (событие отменяется до ванильной обработки).
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InteractionBlockCancelHandler {

    private InteractionBlockCancelHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        if (InteractionBlockLists.isNoInteract(state.getBlock(), pos)) {
            event.setCanceled(true);
        }
    }
}
