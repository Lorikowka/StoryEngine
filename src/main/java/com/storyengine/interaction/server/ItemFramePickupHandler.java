package com.storyengine.interaction.server;

import com.storyengine.StoryEngineMod;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Съём предмета из рамки правым кликом (порт ServerEvents из Interaction Hint).
 *
 * Ванильный ItemFrame в 1.19.2 кликом только кладёт предмет или поворачивает
 * его; «снятие» — поведение мода. Обработчик срабатывает на сервере, отменяет
 * ванильное взаимодействие и выкидывает предмет сущностью (spawnAtLocation),
 * после чего очищает рамку.
 *
 * В этот класс нельзя читать client-конфиг (MenuCustomizationConfig) - он живёт
 * только на клиенте; на dedicated server обращение упало бы. Поэтому здесь
 * безусловная серверная логика без каких-либо гейтов на конфиг.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemFramePickupHandler {

    private ItemFramePickupHandler() {
    }

    @SubscribeEvent
    public static void onItemFramePickup(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getTarget() instanceof ItemFrame frame)) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            return;
        }
        ItemStack stack = frame.getItem();
        if (stack.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        frame.spawnAtLocation(stack);
        frame.setItem(ItemStack.EMPTY);
    }
}