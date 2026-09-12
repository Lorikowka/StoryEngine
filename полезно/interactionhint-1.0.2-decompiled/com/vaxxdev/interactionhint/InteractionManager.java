/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.ChestBlock
 *  net.minecraft.world.level.block.DoorBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.ChestType
 *  net.minecraft.world.level.block.state.properties.DoubleBlockHalf
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.EntityHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package com.vaxxdev.interactionhint;

import com.vaxxdev.interactionhint.ActionResolver;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class InteractionManager {
    public static String currentAction = null;
    public static Vec3 currentTargetPos = null;
    public static List<Vec3> nearbyTargets = new ArrayList<Vec3>();
    private static final int SCAN_RADIUS = 5;
    private static int scanCounter = 0;
    private static final int SCAN_INTERVAL_TICKS = 4;

    public static void tick(Minecraft mc) {
        currentAction = null;
        currentTargetPos = null;
        if (mc.f_91077_ != null) {
            EntityHitResult hit;
            if (mc.f_91077_.m_6662_() == HitResult.Type.BLOCK) {
                BlockHitResult hit2 = (BlockHitResult)mc.f_91077_;
                BlockPos pos = hit2.m_82425_();
                BlockState state = mc.f_91073_.m_8055_(pos);
                String action = ActionResolver.getBlockAction(state.m_60734_());
                if (action != null) {
                    currentAction = action;
                    currentTargetPos = InteractionManager.getTargetCenter(mc, pos, state);
                } else if (state.m_60750_((Level)mc.f_91073_, pos) != null) {
                    currentAction = "interactionhint.action.use";
                    currentTargetPos = InteractionManager.getTargetCenter(mc, pos, state);
                } else if (mc.f_91073_.m_7702_(pos) instanceof MenuProvider) {
                    currentAction = "interactionhint.action.use";
                    currentTargetPos = InteractionManager.getTargetCenter(mc, pos, state);
                }
            } else if (mc.f_91077_.m_6662_() == HitResult.Type.ENTITY && (currentAction = ActionResolver.getEntityAction((hit = (EntityHitResult)mc.f_91077_).m_82443_())) != null) {
                currentTargetPos = hit.m_82443_().m_20191_().m_82399_();
            }
        }
        if (scanCounter++ % 4 == 0) {
            nearbyTargets = InteractionManager.scanNearbyTargets(mc);
        }
    }

    private static List<Vec3> scanNearbyTargets(Minecraft mc) {
        ArrayList<Vec3> result = new ArrayList<Vec3>();
        if (mc.f_91074_ == null || mc.f_91073_ == null) {
            return result;
        }
        BlockPos playerPos = mc.f_91074_.m_20183_();
        for (int dx = -5; dx <= 5; ++dx) {
            for (int dy = -5; dy <= 5; ++dy) {
                for (int dz = -5; dz <= 5; ++dz) {
                    boolean interactable;
                    BlockPos pos = playerPos.m_7918_(dx, dy, dz);
                    BlockState state = mc.f_91073_.m_8055_(pos);
                    Block block = state.m_60734_();
                    boolean bl = interactable = ActionResolver.getBlockAction(block) != null || state.m_60750_((Level)mc.f_91073_, pos) != null || mc.f_91073_.m_7702_(pos) instanceof MenuProvider;
                    if (!interactable) continue;
                    if (block instanceof DoorBlock) {
                        if (state.m_61143_((Property)DoorBlock.f_52730_) != DoubleBlockHalf.LOWER) continue;
                        result.add(InteractionManager.getTargetCenter(mc, pos, state));
                        continue;
                    }
                    if (block instanceof ChestBlock) {
                        if (state.m_61143_((Property)ChestBlock.f_51479_) == ChestType.RIGHT) continue;
                        result.add(InteractionManager.getTargetCenter(mc, pos, state));
                        continue;
                    }
                    result.add(InteractionManager.getTargetCenter(mc, pos, state));
                }
            }
        }
        AABB scanArea = new AABB(playerPos).m_82400_(5.0);
        for (Entity entity : mc.f_91073_.m_6249_((Entity)mc.f_91074_, scanArea, ActionResolver::canInteract)) {
            result.add(entity.m_20191_().m_82399_());
        }
        return result;
    }

    private static Vec3 getTargetCenter(Minecraft mc, BlockPos pos, BlockState state) {
        ChestType type;
        if (state.m_60734_() instanceof DoorBlock) {
            BlockPos lowerPos = state.m_61143_((Property)DoorBlock.f_52730_) == DoubleBlockHalf.LOWER ? pos : pos.m_7495_();
            BlockPos upperPos = lowerPos.m_7494_();
            BlockState lowerState = mc.f_91073_.m_8055_(lowerPos);
            BlockState upperState = mc.f_91073_.m_8055_(upperPos);
            return InteractionManager.combineCenter(InteractionManager.shapeBox(mc, lowerPos, lowerState), InteractionManager.shapeBox(mc, upperPos, upperState));
        }
        if (state.m_60734_() instanceof ChestBlock && (type = (ChestType)state.m_61143_((Property)ChestBlock.f_51479_)) != ChestType.SINGLE) {
            Direction toOtherHalf = ChestBlock.m_51584_((BlockState)state);
            BlockPos otherPos = pos.m_121945_(toOtherHalf);
            BlockState otherState = mc.f_91073_.m_8055_(otherPos);
            return InteractionManager.combineCenter(InteractionManager.shapeBox(mc, pos, state), InteractionManager.shapeBox(mc, otherPos, otherState));
        }
        return InteractionManager.shapeBox(mc, pos, state).m_82399_();
    }

    private static Vec3 combineCenter(AABB a, AABB b) {
        double minX = Math.min(a.f_82288_, b.f_82288_);
        double minY = Math.min(a.f_82289_, b.f_82289_);
        double minZ = Math.min(a.f_82290_, b.f_82290_);
        double maxX = Math.max(a.f_82291_, b.f_82291_);
        double maxY = Math.max(a.f_82292_, b.f_82292_);
        double maxZ = Math.max(a.f_82293_, b.f_82293_);
        return new Vec3((minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0);
    }

    private static AABB shapeBox(Minecraft mc, BlockPos pos, BlockState state) {
        VoxelShape shape = state.m_60808_((BlockGetter)mc.f_91073_, pos);
        return shape.m_83281_() ? new AABB(pos) : shape.m_83215_().m_82386_((double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_());
    }

    public static void interact(Minecraft mc) {
        if (mc.f_91077_ == null) {
            return;
        }
        if (mc.f_91074_ == null) {
            return;
        }
        if (mc.f_91073_ == null) {
            return;
        }
        HitResult hitResult = mc.f_91077_;
        if (hitResult instanceof BlockHitResult) {
            BlockHitResult blockHit = (BlockHitResult)hitResult;
            BlockState state = mc.f_91073_.m_8055_(blockHit.m_82425_());
            if (!ActionResolver.canInteract(state.m_60734_()) && state.m_60750_((Level)mc.f_91073_, blockHit.m_82425_()) == null && !(mc.f_91073_.m_7702_(blockHit.m_82425_()) instanceof MenuProvider)) {
                return;
            }
            mc.f_91072_.m_233732_(mc.f_91074_, InteractionHand.MAIN_HAND, blockHit);
            return;
        }
        hitResult = mc.f_91077_;
        if (hitResult instanceof EntityHitResult) {
            EntityHitResult entityHit = (EntityHitResult)hitResult;
            if (!ActionResolver.canInteract(entityHit.m_82443_())) {
                return;
            }
            mc.f_91072_.m_105230_((Player)mc.f_91074_, entityHit.m_82443_(), entityHit, InteractionHand.MAIN_HAND);
        }
    }
}
