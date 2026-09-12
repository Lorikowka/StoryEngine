package com.storyengine.interaction.client.hint;

import com.storyengine.StoryEngineMod;
import com.storyengine.client.MenuCustomizationConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Обнаружение «интерактиблов» (порт InteractionManager из Interaction Hint):
 *  - цель взгляда: из {@code mc.hitResult} (окклюзия уже учтена ванильным
 *    рейкастом), классификация через {@link VanillaActionResolver};
 *  - близкие цели: куб (радиус из конфига) вокруг игрока, обновляется раз в
 *    {@value SCAN_INTERVAL_TICKS} тика для мировых маркеров.
 *
 * Пары блоков (двери, двойные сундуки) центрируются как единая цель.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class InteractionHintScanner {

    private static final int SCAN_INTERVAL_TICKS = 4;
    /** Период отправки C2SCrosshairPacket на сервер (для подсказок триггеров). */
    private static final int CROSSHAIR_INTERVAL_TICKS = 10;
    private static int scanCounter = 0;

    private InteractionHintScanner() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!MenuCustomizationConfig.interactionHintEnabled()) {
            InteractionHintState.clear();
            return;
        }
        tick(mc);
    }

    private static void tick(Minecraft mc) {
        InteractionHintState.clearAimed();

        if (scanCounter++ % CROSSHAIR_INTERVAL_TICKS == 0) {
            sendCrosshairTarget(mc);
        }

        HitResult hit = mc.hitResult;
        if (hit != null) {
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos pos = blockHit.getBlockPos();
                BlockState state = mc.level.getBlockState(pos);
                String action = VanillaActionResolver.getBlockAction(state.getBlock());
                if (action == null && state.getMenuProvider(mc.level, pos) != null) {
                    action = VanillaActionResolver.USE;
                }
                if (action != null) {
                    InteractionHintState.setAimed(action, getTargetCenter(mc, pos, state));
                }
            } else if (hit.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                String action = VanillaActionResolver.getEntityAction(entity);
                if (action != null) {
                    InteractionHintState.setAimed(action, entity.getBoundingBox().getCenter());
                }
            }
        }

        if (scanCounter++ % SCAN_INTERVAL_TICKS == 0) {
            InteractionHintState.setNearbyTargets(scanNearbyTargets(mc));
        }
    }

    private static void sendCrosshairTarget(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null) {
            com.storyengine.trigger.TriggerNetworking.C2SCrosshairPacket packet =
                    new com.storyengine.trigger.TriggerNetworking.C2SCrosshairPacket(null, null);
            com.storyengine.network.QuestNetworking.CHANNEL.sendToServer(packet);
            TriggerHintClientState.clear();
            return;
        }
        if (hit.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) hit).getEntity();
            if (entity != null) {
                com.storyengine.trigger.TriggerNetworking.C2SCrosshairPacket packet =
                        new com.storyengine.trigger.TriggerNetworking.C2SCrosshairPacket(entity.getId(), null);
                com.storyengine.network.QuestNetworking.CHANNEL.sendToServer(packet);
                return;
            }
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            if (pos != null) {
                com.storyengine.trigger.TriggerNetworking.C2SCrosshairPacket packet =
                        new com.storyengine.trigger.TriggerNetworking.C2SCrosshairPacket(null, pos);
                com.storyengine.network.QuestNetworking.CHANNEL.sendToServer(packet);
                return;
            }
        }
        com.storyengine.trigger.TriggerNetworking.C2SCrosshairPacket packet =
                new com.storyengine.trigger.TriggerNetworking.C2SCrosshairPacket(null, null);
        com.storyengine.network.QuestNetworking.CHANNEL.sendToServer(packet);
    }

    private static List<Vec3> scanNearbyTargets(Minecraft mc) {
        List<Vec3> result = new ArrayList<>();
        int radius = MenuCustomizationConfig.hintScanRadius();
        BlockPos center = mc.player.blockPosition();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!mc.level.isLoaded(pos)) {
                        continue;
                    }
                    BlockState state = mc.level.getBlockState(pos);
                    boolean interactable = VanillaActionResolver.getBlockAction(state.getBlock()) != null
                            || state.getMenuProvider(mc.level, pos) != null;
                    if (!interactable) {
                        continue;
                    }
                    if (state.getBlock() instanceof DoorBlock) {
                        if (state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                            continue;
                        }
                        result.add(getTargetCenter(mc, pos, state));
                    } else if (state.getBlock() instanceof ChestBlock) {
                        if (state.getValue(ChestBlock.TYPE) == ChestType.RIGHT) {
                            continue;
                        }
                        result.add(getTargetCenter(mc, pos, state));
                    } else {
                        result.add(getTargetCenter(mc, pos, state));
                    }
                }
            }
        }

        int maxMarkers = MenuCustomizationConfig.hintMaxMarkers();
        if (maxMarkers >= 0 && result.size() > maxMarkers) {
            result.sort((a, b) -> Double.compare(
                    a.distanceToSqr(mc.player.getX(), mc.player.getY(), mc.player.getZ()),
                    b.distanceToSqr(mc.player.getX(), mc.player.getY(), mc.player.getZ())));
            result = new ArrayList<>(result.subList(0, maxMarkers));
        }

        AABB scanArea = new AABB(center).inflate(radius);
        for (Entity entity : mc.level.getEntities(mc.player, scanArea, VanillaActionResolver::canInteract)) {
            result.add(entity.getBoundingBox().getCenter());
        }
        return result;
    }

    /** Центр цели: для дверей/двойных сундуков - общий центр обеих половин. */
    private static Vec3 getTargetCenter(Minecraft mc, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock) {
            BlockPos lowerPos = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    ? pos : pos.below();
            BlockPos upperPos = lowerPos.above();
            return combineCenter(shapeBox(mc, lowerPos, mc.level.getBlockState(lowerPos)),
                    shapeBox(mc, upperPos, mc.level.getBlockState(upperPos)));
        }
        if (state.getBlock() instanceof ChestBlock
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            Direction toOtherHalf = ChestBlock.getConnectedDirection(state);
            BlockPos otherPos = pos.relative(toOtherHalf);
            return combineCenter(shapeBox(mc, pos, state),
                    shapeBox(mc, otherPos, mc.level.getBlockState(otherPos)));
        }
        return shapeBox(mc, pos, state).getCenter();
    }

    private static Vec3 combineCenter(AABB a, AABB b) {
        double minX = Math.min(a.minX, b.minX);
        double minY = Math.min(a.minY, b.minY);
        double minZ = Math.min(a.minZ, b.minZ);
        double maxX = Math.max(a.maxX, b.maxX);
        double maxY = Math.max(a.maxY, b.maxY);
        double maxZ = Math.max(a.maxZ, b.maxZ);
        return new Vec3((minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0);
    }

    private static AABB shapeBox(Minecraft mc, BlockPos pos, BlockState state) {
        VoxelShape shape = state.getShape(mc.level, pos);
        return shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos.getX(), pos.getY(), pos.getZ());
    }
}