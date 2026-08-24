package com.storyengine.dialogue;

import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.storyengine.narrative.NarrativeMessage;
import com.storyengine.network.NarrativeNetworking;
import com.storyengine.network.QuestNetworking;
import com.storyengine.player.PlayerDialogueData;
import com.storyengine.player.PlayerQuestDataHelper;
import com.storyengine.quest.QuestStatus;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Collections;
import java.util.Map;

/**
 * Выполняет "плоские" действия ответа (DialogueResponse) на сервере.
 * Порядок строго по спецификации §5:
 *   1) command
 *   2) startQuest / completeTask
 *   3) give / xp / setFlag
 *   4) storytell
 * (next / close обрабатывает DialogueManager - здесь не делаем).
 */
public final class DialogueActionExecutor {

    private static final Logger LOGGER = LogUtils.getLogger();

    private DialogueActionExecutor() {
    }

    public static void execute(ServerPlayer player, DialogueResponse response) {
        if (response.getCommand() != null && !response.getCommand().isBlank()) {
            runCommand(player, response.getCommand());
        }

        if (response.getStartQuest() != null && !response.getStartQuest().isBlank()) {
            PlayerQuestDataHelper.setStatus(player, response.getStartQuest(), QuestStatus.ACTIVE);
        }

        if (response.getCompleteTask() != null && !response.getCompleteTask().isBlank()) {
            completeTask(player, response.getCompleteTask());
        }

        if (response.getGive() != null) {
            give(player, response.getGive());
        }

        if (response.getXp() != null && response.getXp() > 0) {
            player.giveExperiencePoints(response.getXp());
        }

        if (response.getSetFlag() != null && !response.getSetFlag().isBlank()) {
            setFlag(player, response.getSetFlag());
        }

        if (response.getStorytell() != null) {
            storytell(player, response.getStorytell());
        }
    }

    /** Запуск команды от имени игрока (переиспользуется Interaction System). */
    public static void runCommand(ServerPlayer player, String command) {
        if (player.getServer() == null) {
            return;
        }
        // Запускаем от имени игрока, чтобы селекторы вроде @p резолвились в него.
        CommandSourceStack source = player.getServer().createCommandSourceStack()
                .withEntity(player)
                .withPosition(player.position())
                .withLevel((ServerLevel) player.level)
                .withPermission(4)
                .withSuppressedOutput();
        try {
            player.getServer().getCommands().performPrefixedCommand(source, command);
        } catch (RuntimeException e) {
            LOGGER.error("[StoryEngine] Ошибка выполнения команды диалога '{}': {}", command, e.getMessage());
        }
    }

    /** Завершение подзадачи (переиспользуется Interaction System). */
    public static void completeTask(ServerPlayer player, String value) {
        String[] parts = value.split(" ", 2);
        if (parts.length != 2) {
            LOGGER.warn("[StoryEngine] Некорректный completeTask: '{}'", value);
            return;
        }
        String questId = parts[0];
        String taskId = parts[1];

        if (PlayerQuestDataHelper.getStatus(player, questId) != QuestStatus.ACTIVE) {
            PlayerQuestDataHelper.setStatus(player, questId, QuestStatus.ACTIVE);
        }
        PlayerQuestDataHelper.completeTask(player, questId, taskId);

        // Если все подзадачи квеста выполнены - завершаем квест (как в /quest task complete).
        var questOpt = com.storyengine.StoryEngineMod.QUEST_MANAGER.getQuest(questId);
        if (questOpt.isPresent()) {
            boolean done = questOpt.get().getTasks().stream()
                    .allMatch(t -> PlayerQuestDataHelper.isTaskCompleted(player, questId, t.getId()));
            if (done) {
                PlayerQuestDataHelper.setStatus(player, questId, QuestStatus.COMPLETED);
                QuestNetworking.sendQuestStatusMessage(player, "Квест '" + questOpt.get().getTitle() + "' выполнен");
            }
        }
    }

    private static void give(ServerPlayer player, Object give) {
        ItemStack stack = parseGive(give);
        if (stack == null) {
            return;
        }
        if (!player.addItem(stack)) {
            // не влезло в инвентарь - дропаем рядом
            player.drop(stack, false);
        }
    }

    /** Разбирает "id count" / "id{nbt} count" или объект {id, count, nbt}. */
    static ItemStack parseGive(Object give) {
        String itemId;
        int count = 1;
        String nbt = null;

        if (give instanceof String s) {
            int space = s.lastIndexOf(' ');
            String itemPart;
            if (space >= 0) {
                itemPart = s.substring(0, space);
                try {
                    count = Integer.parseInt(s.substring(space + 1).trim());
                } catch (NumberFormatException e) {
                    itemPart = s; // нет числа - считаем весь как id
                }
            } else {
                itemPart = s;
            }
            int brace = itemPart.indexOf('{');
            if (brace >= 0) {
                itemId = itemPart.substring(0, brace);
                nbt = itemPart.substring(brace);
            } else {
                itemId = itemPart;
            }
        } else if (give instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) give;
            Object idObj = map.get("id");
            if (idObj == null) {
                return null;
            }
            itemId = idObj.toString();
            Object countObj = map.get("count");
            if (countObj instanceof Number) {
                count = ((Number) countObj).intValue();
            }
            Object nbtObj = map.get("nbt");
            if (nbtObj != null) {
                nbt = nbtObj.toString();
            }
        } else {
            LOGGER.warn("[StoryEngine] Неподдерживаемый формат give: {}", give.getClass());
            return null;
        }

        Item item = Registry.ITEM.get(new ResourceLocation(itemId));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            LOGGER.warn("[StoryEngine] Неизвестный предмет в give: '{}'", itemId);
            return null;
        }

        ItemStack stack = new ItemStack(item, Math.max(1, count));
        if (nbt != null && !nbt.isBlank()) {
            try {
                CompoundTag tag = TagParser.parseTag(nbt);
                stack.setTag(tag);
            } catch (CommandSyntaxException e) {
                LOGGER.warn("[StoryEngine] Некорректный NBT в give '{}': {}", nbt, e.getMessage());
            }
        }
        return stack;
    }

    /** Установка флага (переиспользуется Interaction System). */
    public static void setFlag(ServerPlayer player, String value) {
        String[] parts = value.split(" ", 2);
        if (parts.length != 2) {
            LOGGER.warn("[StoryEngine] Некорректный setFlag: '{}'", value);
            return;
        }
        PlayerDialogueData.get(player).setFlag(parts[0], Boolean.parseBoolean(parts[1].trim()));
    }

    private static void storytell(ServerPlayer player, DialogueResponse.Storytell data) {
        if (data.getMessage() == null) {
            return;
        }
        Component message = Component.Serializer.fromJson(data.getMessage());
        if (message == null) {
            return;
        }
        String speaker = data.getSpeaker() != null ? data.getSpeaker() : "";
        String icon = data.getIcon() != null ? data.getIcon() : "none";
        NarrativeNetworking.sendToPlayers(Collections.singletonList(player), speaker, icon, message, NarrativeMessage.DEFAULT_NAME_COLOR);
    }
}
