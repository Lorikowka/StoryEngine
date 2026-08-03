package com.storyengine.player;

import com.storyengine.network.QuestNetworking;
import com.storyengine.quest.QuestStatus;
import net.minecraft.server.level.ServerPlayer;

/**
 * Современный слой доступа к прогрессу квестов игрока через capability.
 * Сохраняет совместимость с существующим API команд /quest.
 */
public final class PlayerQuestDataHelper {

    private PlayerQuestDataHelper() {
    }

    public static QuestStatus getStatus(ServerPlayer player, String questId) {
        return getData(player).getStatus(questId);
    }

    public static void setStatus(ServerPlayer player, String questId, QuestStatus status) {
        getData(player).setStatus(questId, status);
        sync(player);
    }

    public static void reset(ServerPlayer player, String questId) {
        getData(player).reset(questId);
        sync(player);
    }

    public static boolean isTaskCompleted(ServerPlayer player, String questId, String taskId) {
        return getData(player).isTaskCompleted(questId, taskId);
    }

    public static void completeTask(ServerPlayer player, String questId, String taskId) {
        getData(player).completeTask(questId, taskId);
        sync(player);
    }

    public static IPlayerQuestData getData(ServerPlayer player) {
        return player.getCapability(PlayerQuestDataCapability.QUEST_DATA)
                .orElseThrow(() -> new IllegalStateException("Quest data capability is missing for player " + player.getName().getString()));
    }

    private static void sync(ServerPlayer player) {
        if (player.level.isClientSide) {
            return;
        }
        QuestNetworking.syncToPlayer(player);
    }
}
