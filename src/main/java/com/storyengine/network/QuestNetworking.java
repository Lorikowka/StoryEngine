package com.storyengine.network;

import com.storyengine.StoryEngineMod;
import com.storyengine.client.MenuAssetsManager;
import com.storyengine.player.PlayerQuestDataCapability;
import com.storyengine.quest.BlockBreakQuestTask;
import com.storyengine.quest.ItemQuestTask;
import com.storyengine.quest.KillEntityQuestTask;
import com.storyengine.quest.LocationQuestTask;
import com.storyengine.quest.ManualQuestTask;
import com.storyengine.quest.QuestData;
import com.storyengine.quest.QuestProgressTracker;
import com.storyengine.quest.QuestStatus;
import com.storyengine.quest.QuestTask;
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

    private static final String PROTOCOL_VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StoryEngineMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    // Id 2 и 3 заняты в NarrativeNetworking.java (тот же CHANNEL) - см. тот класс.
    private static final int TOAST_PACKET_ID = 4;
    private static final int MENU_RESET_PACKET_ID = 5;

    private QuestNetworking() {
    }

    public static void register() {
        CHANNEL.registerMessage(packetId++, S2CSyncQuestDataPacket.class, S2CSyncQuestDataPacket::encode, S2CSyncQuestDataPacket::decode, S2CSyncQuestDataPacket::handle);
        CHANNEL.registerMessage(packetId++, S2CQuestStatusMessagePacket.class, S2CQuestStatusMessagePacket::encode, S2CQuestStatusMessagePacket::decode, S2CQuestStatusMessagePacket::handle);
        CHANNEL.registerMessage(TOAST_PACKET_ID, S2CQuestToastPacket.class, S2CQuestToastPacket::encode, S2CQuestToastPacket::decode, S2CQuestToastPacket::handle);
        CHANNEL.registerMessage(MENU_RESET_PACKET_ID, S2CMenuAssetsResetPacket.class, S2CMenuAssetsResetPacket::encode, S2CMenuAssetsResetPacket::decode, S2CMenuAssetsResetPacket::handle);
    }

    /** Сброс кэша текстур меню на всех клиентах (после /storymenu reset|reload). */
    public static void sendMenuAssetsReset() {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new S2CMenuAssetsResetPacket());
    }

    public static void sendQuestStatusMessage(ServerPlayer player, String text) {
        if (player == null || player.level.isClientSide || text == null || text.isBlank()) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CQuestStatusMessagePacket(text));
        sendToast(player, "Квест", text);
    }

    /** Тост одному игроку (например, из /quest complete/fail/task complete). */
    public static void sendToast(ServerPlayer player, String title, String text) {
        if (player == null || player.level.isClientSide || text == null || text.isBlank()) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CQuestToastPacket(title, text));
    }

    /** Тост всем игрокам онлайн сразу (используется /quest notify). */
    public static void sendToastToAll(String title, String text) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new S2CQuestToastPacket(title, text));
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

    public static final class S2CQuestToastPacket {
        public final String title;
        public final String text;

        public S2CQuestToastPacket(String title, String text) {
            this.title = title;
            this.text = text;
        }

        public static void encode(S2CQuestToastPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.title == null ? "" : packet.title);
            buffer.writeUtf(packet.text == null ? "" : packet.text);
        }

        public static S2CQuestToastPacket decode(FriendlyByteBuf buffer) {
            return new S2CQuestToastPacket(buffer.readUtf(), buffer.readUtf());
        }

        public static void handle(S2CQuestToastPacket packet, Supplier<net.minecraftforge.network.NetworkEvent.Context> contextSupplier) {
            net.minecraftforge.network.NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    com.storyengine.client.QuestToastOverlay.add(packet.title, packet.text);
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

        private static String orEmpty(String s) {
            return s == null ? "" : s;
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
                buffer.writeUtf(orEmpty(quest.getId()));
                buffer.writeUtf(orEmpty(quest.getTitle()));
                buffer.writeUtf(orEmpty(quest.getDescription()));
                String author = quest.getAuthor();
                boolean hasAuthor = author != null && !author.isEmpty();
                buffer.writeBoolean(hasAuthor);
                if (hasAuthor) {
                    buffer.writeUtf(author);
                }
                buffer.writeInt(quest.getTasks().size());
                for (QuestTask task : quest.getTasks()) {
                    encodeTask(buffer, task);
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
                quest.setTitle(orEmpty(buffer.readUtf()));
                quest.setDescription(orEmpty(buffer.readUtf()));
                if (buffer.readBoolean()) {
                    quest.setAuthor(buffer.readUtf());
                }
                int taskSize = buffer.readInt();
                List<QuestTask> tasks = new ArrayList<>();
                for (int j = 0; j < taskSize; j++) {
                    tasks.add(decodeTask(buffer));
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

        // ============================================================
        // Полиморфная сериализация/десериализация QuestTask.
        //
        // ВАЖНО: используется обычный switch со строковыми case-метками
        // и явным break, а НЕ arrow-switch с "case X, default ->" -
        // последнее в принципе не компилируется в Java (default нельзя
        // комбинировать с обычной меткой в одной ветке ни в каком
        // источнике/версии) - на этом уже спотыкались раньше.
        // ============================================================
        private static void encodeTask(FriendlyByteBuf buffer, QuestTask task) {
            String type = task.getType() == null ? "MANUAL" : task.getType().toUpperCase(java.util.Locale.ROOT);
            buffer.writeUtf(type);
            buffer.writeUtf(orEmpty(task.getId()));
            buffer.writeUtf(orEmpty(task.getTitle()));
            buffer.writeUtf(orEmpty(task.getDescription()));

            switch (type) {
                case "LOCATION": {
                    LocationQuestTask lt = (LocationQuestTask) task;
                    buffer.writeUtf(orEmpty(lt.getDimension()));
                    buffer.writeDouble(lt.getX());
                    buffer.writeDouble(lt.getY());
                    buffer.writeDouble(lt.getZ());
                    buffer.writeDouble(lt.getRadius());
                    break;
                }
                case "ITEM": {
                    ItemQuestTask it = (ItemQuestTask) task;
                    buffer.writeUtf(orEmpty(it.getTarget()));
                    buffer.writeInt(it.getCount());
                    buffer.writeBoolean(it.isConsume());
                    break;
                }
                case "BLOCK_BREAK": {
                    BlockBreakQuestTask bt = (BlockBreakQuestTask) task;
                    buffer.writeUtf(orEmpty(bt.getBlockId()));
                    buffer.writeInt(bt.getCount());
                    break;
                }
                case "KILL_ENTITY": {
                    KillEntityQuestTask kt = (KillEntityQuestTask) task;
                    buffer.writeUtf(orEmpty(kt.getEntityType()));
                    buffer.writeInt(kt.getCount());
                    break;
                }
                default:
                    // MANUAL и любой неизвестный тип - дополнительных полей нет.
                    break;
            }
        }

        private static QuestTask decodeTask(FriendlyByteBuf buffer) {
            String type = buffer.readUtf();
            String id = buffer.readUtf();
            String title = buffer.readUtf();
            String description = buffer.readUtf();

            switch (type) {
                case "LOCATION": {
                    LocationQuestTask lt = new LocationQuestTask();
                    lt.setId(id);
                    lt.setTitle(title);
                    lt.setDescription(description);
                    lt.setDimension(buffer.readUtf());
                    lt.setX(buffer.readDouble());
                    lt.setY(buffer.readDouble());
                    lt.setZ(buffer.readDouble());
                    lt.setRadius(buffer.readDouble());
                    return lt;
                }
                case "ITEM": {
                    ItemQuestTask it = new ItemQuestTask();
                    it.setId(id);
                    it.setTitle(title);
                    it.setDescription(description);
                    it.setTarget(buffer.readUtf());
                    it.setCount(buffer.readInt());
                    it.setConsume(buffer.readBoolean());
                    return it;
                }
                case "BLOCK_BREAK": {
                    BlockBreakQuestTask bt = new BlockBreakQuestTask();
                    bt.setId(id);
                    bt.setTitle(title);
                    bt.setDescription(description);
                    bt.setBlockId(buffer.readUtf());
                    bt.setCount(buffer.readInt());
                    return bt;
                }
                case "KILL_ENTITY": {
                    KillEntityQuestTask kt = new KillEntityQuestTask();
                    kt.setId(id);
                    kt.setTitle(title);
                    kt.setDescription(description);
                    kt.setEntityType(buffer.readUtf());
                    kt.setCount(buffer.readInt());
                    return kt;
                }
                default:
                    // MANUAL и любой неизвестный/будущий тип - как ManualQuestTask,
                    // чтобы декодирование не падало на новых типах со старого клиента.
                    return new ManualQuestTask(id, title, description);
            }
        }
    }

    /**
     * S2C-пакет сброса кэша текстур меню на клиенте. Отправляется командой
     * /storymenu reset|reload, чтобы изменения PNG в config/story_engine/menu/
     * подхватились без перезапуска клиента.
     */
    public static final class S2CMenuAssetsResetPacket {
        public static void encode(S2CMenuAssetsResetPacket packet, FriendlyByteBuf buffer) {
            // Пакет без полезной нагрузки.
        }

        public static S2CMenuAssetsResetPacket decode(FriendlyByteBuf buffer) {
            return new S2CMenuAssetsResetPacket();
        }

        public static void handle(S2CMenuAssetsResetPacket packet, Supplier<net.minecraftforge.network.NetworkEvent.Context> contextSupplier) {
            net.minecraftforge.network.NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    MenuAssetsManager.clearCache();
                }
            });
            context.setPacketHandled(true);
        }
    }
}
