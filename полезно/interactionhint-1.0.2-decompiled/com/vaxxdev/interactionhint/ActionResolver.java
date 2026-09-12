/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.decoration.ItemFrame
 *  net.minecraft.world.level.block.AnvilBlock
 *  net.minecraft.world.level.block.BarrelBlock
 *  net.minecraft.world.level.block.BedBlock
 *  net.minecraft.world.level.block.BellBlock
 *  net.minecraft.world.level.block.BlastFurnaceBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.ButtonBlock
 *  net.minecraft.world.level.block.CartographyTableBlock
 *  net.minecraft.world.level.block.ChestBlock
 *  net.minecraft.world.level.block.CraftingTableBlock
 *  net.minecraft.world.level.block.DoorBlock
 *  net.minecraft.world.level.block.EnderChestBlock
 *  net.minecraft.world.level.block.FenceGateBlock
 *  net.minecraft.world.level.block.FurnaceBlock
 *  net.minecraft.world.level.block.GrindstoneBlock
 *  net.minecraft.world.level.block.LeverBlock
 *  net.minecraft.world.level.block.LoomBlock
 *  net.minecraft.world.level.block.ShulkerBoxBlock
 *  net.minecraft.world.level.block.SmithingTableBlock
 *  net.minecraft.world.level.block.SmokerBlock
 *  net.minecraft.world.level.block.StonecutterBlock
 *  net.minecraft.world.level.block.TrapDoorBlock
 */
package com.vaxxdev.interactionhint;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;

public class ActionResolver {
    public static String getBlockAction(Block block) {
        if (block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock) {
            return "interactionhint.action.open";
        }
        if (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock) {
            return "interactionhint.action.open";
        }
        if (block instanceof CraftingTableBlock || block instanceof AnvilBlock || block instanceof SmithingTableBlock || block instanceof CartographyTableBlock || block instanceof GrindstoneBlock || block instanceof StonecutterBlock || block instanceof LoomBlock) {
            return "interactionhint.action.use";
        }
        if (block instanceof FurnaceBlock || block instanceof BlastFurnaceBlock || block instanceof SmokerBlock) {
            return "interactionhint.action.open";
        }
        if (block instanceof BedBlock) {
            return "interactionhint.action.sleep";
        }
        if (block instanceof BellBlock) {
            return "interactionhint.action.ring";
        }
        if (block instanceof ButtonBlock) {
            return "interactionhint.action.press";
        }
        if (block instanceof LeverBlock) {
            return "interactionhint.action.toggle";
        }
        return null;
    }

    public static String getEntityAction(Entity entity) {
        if (entity instanceof ItemFrame) {
            ItemFrame frame = (ItemFrame)entity;
            if (frame.m_31822_().m_41619_()) {
                return null;
            }
            return "interactionhint.action.take";
        }
        return null;
    }

    public static boolean canInteract(Block block) {
        return ActionResolver.getBlockAction(block) != null;
    }

    public static boolean canInteract(Entity entity) {
        return ActionResolver.getEntityAction(entity) != null;
    }
}
