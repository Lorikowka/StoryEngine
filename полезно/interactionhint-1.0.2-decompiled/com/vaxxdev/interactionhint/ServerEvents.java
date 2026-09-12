/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.decoration.ItemFrame
 *  net.minecraft.world.item.ItemStack
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$EntityInteractSpecific
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.vaxxdev.interactionhint;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="interactionhint", bus=Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {
    @SubscribeEvent
    public static void onItemFramePickup(PlayerInteractEvent.EntityInteractSpecific event) {
        ItemFrame frame;
        ItemStack stack;
        Entity entity = event.getTarget();
        if (entity instanceof ItemFrame && !(stack = (frame = (ItemFrame)entity).m_31822_()).m_41619_()) {
            event.setCanceled(true);
            frame.m_19983_(stack);
            frame.m_31805_(ItemStack.f_41583_);
        }
    }
}
