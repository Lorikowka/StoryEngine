package com.storyengine.trigger.action;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Общий исполнитель команд для действий триггеров: команда выполняется
 * от имени сервера с правами level 4 в контексте игрока, вызвавшего триггер
 * (селектор {@code @p} = этот игрок), вывод в чат подавлен.
 * Механика идентична наградам квестов {@link com.storyengine.quest.QuestRewards}.
 */
final class ActionCommands {

    private ActionCommands() {
    }

    /**
     * Выполнить команду от имени игрока (ведущий слэш необязателен).
     *
     * @return результат диспетчера команд, либо {@code -1}, если контекст
     *         недоступен (игрока/сервера нет, команда пуста)
     */
    static int runAsPlayer(ServerPlayer player, String command) {
        if (player == null || command == null || command.isBlank()) {
            return -1;
        }
        if (player.getServer() == null) {
            return -1;
        }
        CommandSourceStack source = player.getServer().createCommandSourceStack()
                .withEntity(player)
                .withPosition(player.position())
                .withLevel((ServerLevel) player.getLevel())
                .withPermission(4)
                .withSuppressedOutput();
        return player.getServer().getCommands().performPrefixedCommand(source, command);
    }
}
