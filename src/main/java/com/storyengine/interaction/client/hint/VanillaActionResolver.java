package com.storyengine.interaction.client.hint;

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

/**
 * Маппинг ванильных блоков/сущностей на строку действия (ключ перевода
 * {@code story_engine.action.*}). Аналог ActionResolver из Interaction Hint,
 * но без привязки к чужим lang-ключам: HUD показывает «[F] Действие» для
 * обычных блоков, у которых нет зарегистрированного JSON-триггера.
 *
 * Только классификация - без окклюзии и рейкаста (это у
 * {@link com.storyengine.interaction.client.hint.InteractionHintScanner}).
 */
public final class VanillaActionResolver {

    public static final String OPEN = "story_engine.action.open";
    public static final String USE = "story_engine.action.use";
    public static final String PRESS = "story_engine.action.press";
    public static final String TOGGLE = "story_engine.action.toggle";
    public static final String SLEEP = "story_engine.action.sleep";
    public static final String RING = "story_engine.action.ring";
    public static final String TAKE = "story_engine.action.take";
    public static final String TRADE = "story_engine.action.trade";
    public static final String SIT = "story_engine.action.sit";

    private VanillaActionResolver() {
    }

    /** Действие для блока или null, если блок не «интерактиблен» по классификации. */
    public static String getBlockAction(Block block) {
        if (block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock) {
            return OPEN;
        }
        if (block instanceof ChestBlock || block instanceof EnderChestBlock
                || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock) {
            return OPEN;
        }
        if (block instanceof CraftingTableBlock || block instanceof AnvilBlock
                || block instanceof SmithingTableBlock || block instanceof CartographyTableBlock
                || block instanceof GrindstoneBlock || block instanceof StonecutterBlock
                || block instanceof LoomBlock) {
            return USE;
        }
        if (block instanceof FurnaceBlock || block instanceof BlastFurnaceBlock || block instanceof SmokerBlock) {
            return OPEN;
        }
        if (block instanceof BedBlock) {
            return SLEEP;
        }
        if (block instanceof BellBlock) {
            return RING;
        }
        if (block instanceof ButtonBlock) {
            return PRESS;
        }
        if (block instanceof LeverBlock) {
            return TOGGLE;
        }
        return null;
    }

    /** Действие для сущности или null (пока только ItemFrame с предметом). */
    public static String getEntityAction(Entity entity) {
        if (entity instanceof ItemFrame frame) {
            return frame.getItem().isEmpty() ? null : TAKE;
        }
        return null;
    }

    public static boolean canInteract(Entity entity) {
        return getEntityAction(entity) != null;
    }
}