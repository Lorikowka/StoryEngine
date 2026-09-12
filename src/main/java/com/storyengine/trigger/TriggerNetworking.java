package com.storyengine.trigger;

import com.storyengine.StoryEngineMod;
import com.storyengine.network.QuestNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Сетевые пакеты Trigger System на общем канале QuestNetworking.CHANNEL.
 *
 * Пакеты:
 *   13 - S2CSyncTriggerHintsPacket (сервер → клиент, серверное состояние подсказки)
 *   14 - C2SCrosshairPacket (клиент → сервер, цель под прицелом)
 *
 * Клиент шлёт цель (сущность или блок) под прицелом; сервер резолвит
 * триггер и отвечает подсказкой (см. {@link TriggerHintService}).
 */
public final class TriggerNetworking {

    private static final int SYNC_HINTS_PACKET_ID = 13;
    private static final int CROSSHAIR_PACKET_ID = 14;

    private TriggerNetworking() {
    }

    public static void register() {
        QuestNetworking.CHANNEL.registerMessage(SYNC_HINTS_PACKET_ID, S2CSyncTriggerHintsPacket.class,
                S2CSyncTriggerHintsPacket::encode, S2CSyncTriggerHintsPacket::decode, S2CSyncTriggerHintsPacket::handle);
        QuestNetworking.CHANNEL.registerMessage(CROSSHAIR_PACKET_ID, C2SCrosshairPacket.class,
                C2SCrosshairPacket::encode, C2SCrosshairPacket::decode, C2SCrosshairPacket::handle);
    }

    /** Отправка серверного состояния подсказки игроку (null = подсказку скрыть). */
    public static void sendHints(ServerPlayer player, TriggerHintService.TriggerHint hint) {
        if (player == null || player.level.isClientSide) {
            return;
        }
        QuestNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CSyncTriggerHintsPacket(hint));
    }

    // ============================================================
    // S2CSyncTriggerHintsPacket
    // ============================================================
    public static final class S2CSyncTriggerHintsPacket {
        /** null = подскажки для текущего прицела нет. */
        public final TriggerHintService.TriggerHint hint;

        public S2CSyncTriggerHintsPacket(TriggerHintService.TriggerHint hint) {
            this.hint = hint;
        }

        public static void encode(S2CSyncTriggerHintsPacket packet, FriendlyByteBuf buffer) {
            boolean has = packet.hint != null;
            buffer.writeBoolean(has);
            if (has) {
                buffer.writeUtf(packet.hint.triggerId() == null ? "" : packet.hint.triggerId());
                buffer.writeUtf(packet.hint.targetTag() == null ? "" : packet.hint.targetTag());
                buffer.writeUtf(packet.hint.hintLabel() == null ? "" : packet.hint.hintLabel());
                buffer.writeBoolean(packet.hint.cancelVanilla());
            }
        }

        public static S2CSyncTriggerHintsPacket decode(FriendlyByteBuf buffer) {
            boolean has = buffer.readBoolean();
            if (!has) {
                return new S2CSyncTriggerHintsPacket(null);
            }
            String triggerId = buffer.readUtf();
            String targetTag = buffer.readUtf();
            String hintLabel = buffer.readUtf();
            boolean cancelVanilla = buffer.readBoolean();
            return new S2CSyncTriggerHintsPacket(
                    new TriggerHintService.TriggerHint(
                            triggerId.isEmpty() ? null : triggerId,
                            targetTag.isEmpty() ? null : targetTag,
                            hintLabel.isEmpty() ? null : hintLabel,
                            cancelVanilla));
        }

        public static void handle(S2CSyncTriggerHintsPacket packet, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    com.storyengine.interaction.client.hint.TriggerHintClientState.apply(packet.hint);
                }
            });
            context.setPacketHandled(true);
        }
    }

    // ============================================================
    // C2SCrosshairPacket (клиент → сервер)
    // ============================================================
    public static final class C2SCrosshairPacket {
        /** id сущности под прицелом или null. */
        public final Integer entityId;
        /** позиция блока под прицелом или null. */
        public final net.minecraft.core.BlockPos blockPos;

        public C2SCrosshairPacket(Integer entityId, net.minecraft.core.BlockPos blockPos) {
            this.entityId = entityId;
            this.blockPos = blockPos;
        }

        public static void encode(C2SCrosshairPacket packet, FriendlyByteBuf buffer) {
            buffer.writeBoolean(packet.entityId != null);
            if (packet.entityId != null) {
                buffer.writeInt(packet.entityId);
            }
            buffer.writeBoolean(packet.blockPos != null);
            if (packet.blockPos != null) {
                buffer.writeBlockPos(packet.blockPos);
            }
        }

        public static C2SCrosshairPacket decode(FriendlyByteBuf buffer) {
            Integer entityId = null;
            if (buffer.readBoolean()) {
                entityId = buffer.readInt();
            }
            net.minecraft.core.BlockPos blockPos = null;
            if (buffer.readBoolean()) {
                blockPos = buffer.readBlockPos();
            }
            return new C2SCrosshairPacket(entityId, blockPos);
        }

        public static void handle(C2SCrosshairPacket packet, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                if (player.level.isClientSide) {
                    return;
                }
                TriggerHintService.get().updateTarget(player,
                        (net.minecraft.server.level.ServerLevel) player.level,
                        packet.entityId, packet.blockPos);
            });
            context.setPacketHandled(true);
        }
    }
}