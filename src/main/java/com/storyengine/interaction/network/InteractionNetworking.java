package com.storyengine.interaction.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import com.storyengine.interaction.data.InteractionTrigger;
import com.storyengine.interaction.data.TriggerAction;
import com.storyengine.interaction.server.TriggerActionExecutor;
import com.storyengine.interaction.server.TriggerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Регистрация и отправка пакетов Interaction System.
 * Переиспользует ОБЩИЙ канал QuestNetworking.CHANNEL (см. DialogueNetworking).
 *
 * Пакеты (id на общем канале):
 *   10 - S2CSyncTriggersPacket  (сервер -> клиент: все триггеры)
 *   11 - C2SExecuteActionPacket (клиент -> сервер: выполнить действие)
 *
 * Защита на сервере (см. спецификацию §6): дистанция глаз->блок <= maxDistance+1.0,
 * повторная валидация условия if, cooldown 250 мс против флуда.
 */
public final class InteractionNetworking {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int SYNC_PACKET_ID = 10;
    private static final int EXECUTE_PACKET_ID = 11;

    /** Cooldown на выполнение действия одним игроком (мс). */
    private static final long EXECUTE_COOLDOWN_MS = 250;

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    /** Последнее время выполнения действия по игроку (защита от флуда). */
    private static final Map<UUID, Long> LAST_EXECUTE = new ConcurrentHashMap<>();

    private InteractionNetworking() {
    }

    public static void register() {
        com.storyengine.network.QuestNetworking.CHANNEL.registerMessage(SYNC_PACKET_ID,
                S2CSyncTriggersPacket.class, S2CSyncTriggersPacket::encode, S2CSyncTriggersPacket::decode, S2CSyncTriggersPacket::handle);
        com.storyengine.network.QuestNetworking.CHANNEL.registerMessage(EXECUTE_PACKET_ID,
                C2SExecuteActionPacket.class, C2SExecuteActionPacket::encode, C2SExecuteActionPacket::decode, C2SExecuteActionPacket::handle);
    }

    /** Отправить все триггеры конкретному игроку (при входе). */
    public static void sendSync(ServerPlayer player) {
        Collection<InteractionTrigger> all = StoryEngineMod.TRIGGER_MANAGER.getAll();
        com.storyengine.network.QuestNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CSyncTriggersPacket(new ArrayList<>(all)));
    }

    /** Переотправить все триггеры всем игрокам (после /trigger reload). */
    public static void sendSyncToAll() {
        Collection<InteractionTrigger> all = StoryEngineMod.TRIGGER_MANAGER.getAll();
        com.storyengine.network.QuestNetworking.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new S2CSyncTriggersPacket(new ArrayList<>(all)));
    }

    // ============================================================
    // S2CSyncTriggersPacket
    // ============================================================
    public static final class S2CSyncTriggersPacket {
        public final List<InteractionTrigger> triggers;

        public S2CSyncTriggersPacket(List<InteractionTrigger> triggers) {
            this.triggers = triggers;
        }

        public static void encode(S2CSyncTriggersPacket packet, FriendlyByteBuf buffer) {
            buffer.writeInt(packet.triggers.size());
            for (InteractionTrigger t : packet.triggers) {
                buffer.writeUtf(GSON.toJson(t));
            }
        }

        public static S2CSyncTriggersPacket decode(FriendlyByteBuf buffer) {
            int count = buffer.readInt();
            List<InteractionTrigger> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String json = buffer.readUtf();
                try {
                    list.add(GSON.fromJson(json, InteractionTrigger.class));
                } catch (RuntimeException e) {
                    LOGGER.warn("[StoryEngine] Ошибка декода триггера из пакета: {}", e.getMessage());
                }
            }
            return new S2CSyncTriggersPacket(list);
        }

        public static void handle(S2CSyncTriggersPacket packet, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    com.storyengine.interaction.client.InteractionClientState.setTriggers(packet.triggers);
                }
            });
            context.setPacketHandled(true);
        }
    }

    // ============================================================
    // C2SExecuteActionPacket
    // ============================================================
    public static final class C2SExecuteActionPacket {
        public final String triggerId;
        public final int actionIndex;

        public C2SExecuteActionPacket(String triggerId, int actionIndex) {
            this.triggerId = triggerId;
            this.actionIndex = actionIndex;
        }

        public static void encode(C2SExecuteActionPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.triggerId == null ? "" : packet.triggerId);
            buffer.writeInt(packet.actionIndex);
        }

        public static C2SExecuteActionPacket decode(FriendlyByteBuf buffer) {
            return new C2SExecuteActionPacket(buffer.readUtf(), buffer.readInt());
        }

        public static void handle(C2SExecuteActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }

                long now = System.currentTimeMillis();
                Long last = LAST_EXECUTE.get(player.getUUID());
                if (last != null && now - last < EXECUTE_COOLDOWN_MS) {
                    return; // rate limit
                }

                TriggerManager manager = StoryEngineMod.TRIGGER_MANAGER;
                InteractionTrigger trigger = manager.getTriggerById(packet.triggerId);
                if (trigger == null) {
                    return;
                }

                // Защита от читов на дистанцию.
                Vec3 eye = player.getEyePosition(1.0f);
                Vec3 target = Vec3.atCenterOf(trigger.getBlockPos());
                double dist = Math.sqrt(eye.distanceToSqr(target));
                if (dist > trigger.getMaxDistance() + 1.0) {
                    LOGGER.warn("[StoryEngine] Игрок {} слишком далеко от триггера {} ({} > {})",
                            player.getName().getString(), packet.triggerId, dist, trigger.getMaxDistance() + 1.0);
                    return;
                }

                if (packet.actionIndex < 0 || packet.actionIndex >= trigger.getActions().size()) {
                    return;
                }
                TriggerAction action = trigger.getActions().get(packet.actionIndex);

                // Повторная валидация условия if на сервере.
                if (!action.isAvailable(player)) {
                    return;
                }

                LAST_EXECUTE.put(player.getUUID(), now);
                TriggerActionExecutor.execute(player, trigger, action);
            });
            context.setPacketHandled(true);
        }
    }
}
