package com.storyengine.quest;

import com.storyengine.StoryEngineMod;
import com.storyengine.network.QuestNetworking;
import com.storyengine.player.PlayerQuestDataHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;

public class QuestProgressTracker {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.player;
        for (QuestData quest : StoryEngineMod.QUEST_MANAGER.getAllQuests()) {
            if (PlayerQuestDataHelper.getStatus(player, quest.getId()) != QuestStatus.ACTIVE) {
                continue;
            }

            boolean hasIncompleteTrackableTask = false;
            for (QuestTask task : quest.getTasks()) {
                if (PlayerQuestDataHelper.isTaskCompleted(player, quest.getId(), task.getId())) {
                    continue;
                }

                if (task instanceof ManualQuestTask) {
                    // ManualQuestTask завершается только явным внешним действием
                    // (например, /quest complete или будущей командой завершения
                    // конкретной подзадачи), а НЕ автоматически трекером на тике.
                    // Раньше здесь был completeTask() без всякого условия - из-за
                    // этого квест с такой задачей завершался сам через тик после
                    // /quest start, даже без участия игрока.
                    hasIncompleteTrackableTask = true;
                    continue;
                }

                if (task instanceof LocationQuestTask locationTask) {
                    if (isAtLocation(player, locationTask)) {
                        PlayerQuestDataHelper.completeTask(player, quest.getId(), task.getId());
                        QuestNetworking.sendQuestStatusMessage(player, "Подзадача '" + task.getTitle() + "' выполнена");
                    } else {
                        hasIncompleteTrackableTask = true;
                    }
                    continue;
                }

                if (task instanceof ItemQuestTask itemTask) {
                    int count = countItemsInInventory(player, itemTask.getTarget());
                    setProgress(player, quest.getId(), task.getId(), count);
                    if (count >= itemTask.getCount()) {
                        PlayerQuestDataHelper.completeTask(player, quest.getId(), task.getId());
                        QuestNetworking.sendQuestStatusMessage(player, "Подзадача '" + task.getTitle() + "' выполнена");
                    } else {
                        hasIncompleteTrackableTask = true;
                    }
                    continue;
                }

                if (task instanceof BlockBreakQuestTask blockTask) {
                    int count = getProgress(player, quest.getId(), task.getId());
                    setProgress(player, quest.getId(), task.getId(), count);
                    if (count >= blockTask.getCount()) {
                        PlayerQuestDataHelper.completeTask(player, quest.getId(), task.getId());
                        QuestNetworking.sendQuestStatusMessage(player, "Подзадача '" + task.getTitle() + "' выполнена");
                    } else {
                        hasIncompleteTrackableTask = true;
                    }
                    continue;
                }

                if (task instanceof KillEntityQuestTask killTask) {
                    int count = getProgress(player, quest.getId(), task.getId());
                    setProgress(player, quest.getId(), task.getId(), count);
                    if (count >= killTask.getCount()) {
                        PlayerQuestDataHelper.completeTask(player, quest.getId(), task.getId());
                        QuestNetworking.sendQuestStatusMessage(player, "Подзадача '" + task.getTitle() + "' выполнена");
                    } else {
                        hasIncompleteTrackableTask = true;
                    }
                    continue;
                }

                hasIncompleteTrackableTask = true;
            }

            if (!hasIncompleteTrackableTask && quest.getTasks().stream().allMatch(task -> PlayerQuestDataHelper.isTaskCompleted(player, quest.getId(), task.getId()))) {
                PlayerQuestDataHelper.setStatus(player, quest.getId(), QuestStatus.COMPLETED);
                QuestNetworking.sendQuestStatusMessage(player, "Квест '" + quest.getTitle() + "' выполнен");
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        for (QuestData quest : StoryEngineMod.QUEST_MANAGER.getAllQuests()) {
            if (PlayerQuestDataHelper.getStatus(player, quest.getId()) != QuestStatus.ACTIVE) {
                continue;
            }
            for (QuestTask task : quest.getTasks()) {
                if (PlayerQuestDataHelper.isTaskCompleted(player, quest.getId(), task.getId()) || !(task instanceof BlockBreakQuestTask blockTask)) {
                    continue;
                }
                String blockId = blockTask.getBlockId();
                if (blockId != null && !blockId.isBlank() && blockId.equals(Registry.BLOCK.getKey(event.getState().getBlock()).toString())) {
                    incrementProgress(player, quest.getId(), task.getId(), 1);
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        for (QuestData quest : StoryEngineMod.QUEST_MANAGER.getAllQuests()) {
            if (PlayerQuestDataHelper.getStatus(player, quest.getId()) != QuestStatus.ACTIVE) {
                continue;
            }
            for (QuestTask task : quest.getTasks()) {
                if (PlayerQuestDataHelper.isTaskCompleted(player, quest.getId(), task.getId()) || !(task instanceof KillEntityQuestTask killTask)) {
                    continue;
                }
                String entityType = killTask.getEntityType();
                if (entityType != null && !entityType.isBlank() && entityType.equals(Registry.ENTITY_TYPE.getKey(event.getEntity().getType()).toString())) {
                    incrementProgress(player, quest.getId(), task.getId(), 1);
                }
            }
        }
    }

    private boolean isAtLocation(ServerPlayer player, LocationQuestTask task) {
        if (task.getDimension() != null && !task.getDimension().isBlank() && !task.getDimension().equals(player.level.dimension().location().toString())) {
            return false;
        }

        double dx = player.getX() - task.getX();
        double dy = player.getY() - task.getY();
        double dz = player.getZ() - task.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance <= task.getRadius();
    }

    private int countItemsInInventory(ServerPlayer player, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return 0;
        }
        ResourceLocation itemLocation = ResourceLocation.tryParse(itemId);
        if (itemLocation == null) {
            return 0;
        }

        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                continue;
            }
            if (Registry.ITEM.getKey(stack.getItem()).equals(itemLocation)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.isEmpty()) {
                continue;
            }
            if (Registry.ITEM.getKey(stack.getItem()).equals(itemLocation)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isEmpty()) {
                continue;
            }
            if (Registry.ITEM.getKey(stack.getItem()).equals(itemLocation)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Прогресс всех отслеживаемых задач игрока по всем квестам - теперь
     * читается из Capability (NBT), а не из статической HashMap в памяти,
     * поэтому переживает рестарт сервера (раньше обнулялся).
     */
    public static Map<String, Map<String, Integer>> getProgress(ServerPlayer player) {
        return PlayerQuestDataHelper.getAllTaskProgress(player);
    }

    private void setProgress(ServerPlayer player, String questId, String taskId, int value) {
        int clamped = Math.max(0, value);
        int previous = PlayerQuestDataHelper.getTaskProgress(player, questId, taskId);
        PlayerQuestDataHelper.setTaskProgressQuiet(player, questId, taskId, clamped);
        if (previous != clamped) {
            // Синхронизируем только при реальном изменении прогресса,
            // а не на каждом тике - иначе клиенту летит полный пакет
            // квестов 20 раз в секунду на каждую отслеживаемую задачу.
            QuestNetworking.syncToPlayer(player);
        }
    }

    private void incrementProgress(ServerPlayer player, String questId, String taskId, int delta) {
        int current = getProgress(player, questId, taskId);
        setProgress(player, questId, taskId, current + delta);
    }

    private int getProgress(ServerPlayer player, String questId, String taskId) {
        return PlayerQuestDataHelper.getTaskProgress(player, questId, taskId);
    }
}
