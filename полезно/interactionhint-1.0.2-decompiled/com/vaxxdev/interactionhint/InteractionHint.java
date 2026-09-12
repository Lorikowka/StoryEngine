/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.fml.common.Mod
 */
package com.vaxxdev.interactionhint;

import com.vaxxdev.interactionhint.ServerEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(value="interactionhint")
public class InteractionHint {
    public static final String MOD_ID = "interactionhint";

    public InteractionHint() {
        MinecraftForge.EVENT_BUS.register((Object)new ServerEvents());
    }
}
