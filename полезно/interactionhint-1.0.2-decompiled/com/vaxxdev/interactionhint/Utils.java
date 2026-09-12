/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 */
package com.vaxxdev.interactionhint;

import net.minecraft.client.Minecraft;

public class Utils {
    public static Minecraft mc() {
        return Minecraft.m_91087_();
    }

    public static boolean canUseHud() {
        Minecraft mc = Utils.mc();
        return mc.f_91074_ != null && mc.f_91073_ != null && mc.f_91080_ == null;
    }
}
