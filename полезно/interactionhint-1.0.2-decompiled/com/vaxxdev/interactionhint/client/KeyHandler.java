/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.Minecraft
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RegisterKeyMappingsEvent
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.vaxxdev.interactionhint.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.vaxxdev.interactionhint.InteractionManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public class KeyHandler {
    public static final KeyMapping INTERACT_KEY = new KeyMapping("key.interactionhint.interact", InputConstants.Type.KEYSYM, 70, "key.categories.gameplay");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(INTERACT_KEY);
    }

    @Mod.EventBusSubscriber(value={Dist.CLIENT})
    public static class ClientEvents {
        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            Minecraft mc = Minecraft.m_91087_();
            if (mc.f_91074_ == null || mc.f_91073_ == null) {
                return;
            }
            while (INTERACT_KEY.m_90859_()) {
                InteractionManager.interact(mc);
            }
        }
    }
}
