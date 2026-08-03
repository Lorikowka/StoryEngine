package com.storyengine.network;

import com.storyengine.StoryEngineMod;
import com.storyengine.player.PlayerQuestDataCapability;
import com.storyengine.quest.QuestData;
import com.storyengine.quest.QuestProgressTracker;
import com.storyengine.quest.QuestStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class QuestNetworking {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StoryEngineMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private QuestNetworking() {
    }

    public static void register() {
        CHANNEL.registerMessage(packetId++, S2CSyncQuestDataPacket.class, S2CSyncQuestDataPacket::encode, S2CSyncQuestDataPacket::decode, S2CSyncQuestDataPacket::handle);
        CHANNEL.registerMessage(packetId++, S2CQuestStatusMessagePacket.class, S2CQuestStatusMessagePacket::encode, S2CQuestStatusMessagePacket::decode, S2CQuestStatusMessagePacket::handle);
    }

    public static void sendQuestStatusMessage(ServerPlayer player, String text) {
        if (player == null || player.level.isClientSide || text == null || text.isBlank()) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CQuestStatusMessagePacket(text));
    }

    public static void syncToPlayer(ServerPlayer player) {
        Map<String, QuestStatus> statuses = new LinkedHashMap<>();
        Map<String, List<String>> completedTasks = new LinkedHashMap<>();

        player.getCapability(PlayerQuestDataCapability.QUEST_DATA).ifPresent(data -> {
            for (QuestData quest : StoryEngineMod.QUEST_MANAGER.getAllQuests()) {
                String questId = quest.getId();
                statuses.put(questId, data.getStatus(questId));
                completedTasks.put(questId, new ArrayList<>(data.getCompletedTasks(questId)));
            }
        });

        Map<String, Map<String, Integer>> taskProgress = new LinkedHashMap<>();
        taskProgress.putAll(QuestProgressTracker.getProgress(player));

        List<QuestData> questData = StoryEngineMod.QUEST_MANAGER.getAllQuests().stream().toList();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CSyncQuestDataPacket(statuses, completedTasks, taskProgress, questData));
    }

    public static final class S2CQuestStatusMessagePacket {
        public final String text;

        public S2CQuestStatusMessagePacket(String text) {
            this.text = text;
        }

        public static void encode(S2CQuestStatusMessagePacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.text == null ? "" : packet.text);
        }

        public static S2CQuestStatusMessagePacket decode(FriendlyByteBuf buffer) {
            return new S2CQuestStatusMessagePacket(buffer.readUtf());
        }

        public static void handle(S2CQuestStatusMessagePacket packet, Supplier<net.minecraftforge.network.NetworkEvent.Context> contextSupplier) {
            net.minecraftforge.network.NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    QuestClientState.setLastNotification(packet.text);
                    Minecraft.getInstance().gui.setOverlayMessage(Component.literal(packet.text), false);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static final class S2CSyncQuestDataPacket {
        public final Map<String, QuestStatus> statuses;
        public final Map<String, List<String>> completedTasks;
        public final Map<String, Map<String, Integer>> taskProgress;
        public final List<QuestData> questData;

        public S2CSyncQuestDataPacket(Map<String, QuestStatus> statuses, Map<String, List<String>> completedTasks, Map<String, Map<String, Integer>> taskProgress, List<QuestData> questData) {
            this.statuses = statuses;
            this.completedTasks = completedTasks;
            this.taskProgress = taskProgress;
            this.questData = questData;
        }

        public static void encode(S2CSyncQuestDataPacket packet, FriendlyByteBuf buffer) {
            buffer.writeInt(packet.statuses.size());
            for (Map.Entry<String, QuestStatus> entry : packet.statuses.entrySet()) {
                buffer.writeUtf(entry.getKey());
                buffer.writeUtf(entry.getValue().name());
            }
            buffer.writeInt(packet.completedTasks.size());
            for (Map.Entry<String, List<String>> entry : packet.completedTasks.entrySet()) {
                buffer.writeUtf(entry.getKey());
                buffer.writeInt(entry.getValue().size());
                for (String taskId : entry.getValue()) {
                    buffer.writeUtf(taskId);
                }
            }
            buffer.writeInt(packet.taskProgress.size());
            for (Map.Entry<String, Map<String, Integer>> entry : packet.taskProgress.entrySet()) {
                buffer.writeUtf(entry.getKey());
                buffer.writeInt(entry.getValue().size());
                for (Map.Entry<String, Integer> taskEntry : entry.getValue().entrySet()) {
                    buffer.writeUtf(taskEntry.getKey());
                    buffer.writeInt(taskEntry.getValue());
                }
            }
            buffer.writeInt(packet.questData.size());
            for (QuestData quest : packet.questData) {
                buffer.writeUtf(quest.getId());
                buffer.writeUtf(quest.getTitle());
                buffer.writeUtf(quest.getDescription());
                buffer.writeInt(quest.getTasks().size());
                for (var task : quest.getTasks()) {
                    buffer.writeUtf(task.getId());
                    buffer.writeUtf(task.getTitle());
                    buffer.writeUtf(task.getDescription());
                }
            }
        }

        public static S2CSyncQuestDataPacket decode(FriendlyByteBuf buffer) {
            int statusCount = buffer.readInt();
            Map<String, QuestStatus> statuses = new LinkedHashMap<>();
            for (int i = 0; i < statusCount; i++) {
                String questId = buffer.readUtf();
                statuses.put(questId, QuestStatus.valueOf(buffer.readUtf()));
            }
            int taskCount = buffer.readInt();
            Map<String, List<String>> completedTasks = new LinkedHashMap<>();
            for (int i = 0; i < taskCount; i++) {
                String questId = buffer.readUtf();
                int taskSize = buffer.readInt();
                List<String> tasks = new ArrayList<>();
                for (int j = 0; j < taskSize; j++) {
                    tasks.add(buffer.readUtf());
                }
                completedTasks.put(questId, tasks);
            }
            int progressCount = buffer.readInt();
            Map<String, Map<String, Integer>> taskProgress = new LinkedHashMap<>();
            for (int i = 0; i < progressCount; i++) {
                String questId = buffer.readUtf();
                int taskProgressCount = buffer.readInt();
                Map<String, Integer> progress = new LinkedHashMap<>();
                for (int j = 0; j < taskProgressCount; j++) {
                    progress.put(buffer.readUtf(), buffer.readInt());
                }
                taskProgress.put(questId, progress);
            }
            int questCount = buffer.readInt();
            List<QuestData> questData = new ArrayList<>();
            for (int i = 0; i < questCount; i++) {
                QuestData quest = new QuestData();
                quest.setId(buffer.readUtf());
                quest.setTitle(buffer.readUtf());
                quest.setDescription(buffer.readUtf());
                int taskSize = buffer.readInt();
                List<com.storyengine.quest.QuestTask> tasks = new ArrayList<>();
                for (int j = 0; j < taskSize; j++) {
                    tasks.add(new com.storyengine.quest.QuestTask(buffer.readUtf(), buffer.readUtf(), buffer.readUtf()));
                }
                quest.setTasks(tasks);
                questData.add(quest);
            }
            return new S2CSyncQuestDataPacket(statuses, completedTasks, taskProgress, questData);
        }

        public static void handle(S2CSyncQuestDataPacket packet, Supplier<net.minecraftforge.network.NetworkEvent.Context> contextSupplier) {
            net.minecraftforge.network.NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    QuestClientState.apply(packet);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
