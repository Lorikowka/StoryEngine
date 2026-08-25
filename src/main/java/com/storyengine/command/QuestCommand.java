package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.storyengine.StoryEngineMod;
import com.storyengine.player.PlayerQuestDataHelper;
import com.storyengine.quest.BlockBreakQuestTask;
import com.storyengine.quest.ItemQuestTask;
import com.storyengine.quest.KillEntityQuestTask;
import com.storyengine.quest.LocationQuestTask;
import com.storyengine.quest.ManualQuestTask;
import com.storyengine.quest.QuestData;
import com.storyengine.quest.QuestManager;
import com.storyengine.quest.QuestStatus;
import com.storyengine.quest.QuestTask;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.concurrent.CompletableFuture;

/**
 * Подкоманда {@code /story quest} и алиас {@code /quest}:
 *
 *   /story quest create <id> [title]
 *   /story quest delete <id>
 *   /story quest reload
 *   /story quest list
 *   /story quest notify <title> <text>
 *   /story quest start <player> <id>
 *   /story quest complete <player> <id>
 *   /story quest fail <player> <id>
 *   /story quest reset <player> <id>
 *   /story quest task complete|remove|edit|add ...
 *   /story quest edit <id> title|description <text>
 *
 * Все команды требуют permission level 2.
 */
public final class QuestCommand {

    private QuestCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build(String literal) {
        return Commands.literal(literal)
                .requires(source -> source.hasPermission(2))

                // /story quest create <id> [title]
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> createQuest(ctx, null))
                                .then(Commands.argument("title", StringArgumentType.greedyString())
                                        .executes(ctx -> createQuest(ctx, StringArgumentType.getString(ctx, "title"))))))

                // /story quest delete <id> - полностью удаляет квест (кэш + файл на диске)
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(QuestCommand::suggestQuestIds)
                                .executes(QuestCommand::deleteQuest)))

                // /story quest reload
                .then(Commands.literal("reload")
                        .executes(QuestCommand::reload))

                // /story quest list
                .then(Commands.literal("list")
                        .executes(QuestCommand::list))

                // /story quest notify <title> <text> - тост всем игрокам, квесты не трогает
                .then(Commands.literal("notify")
                        .then(Commands.argument("title", StringArgumentType.string())
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(QuestCommand::notify))))

                // /story quest start <player> <id>
                .then(Commands.literal("start")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(QuestCommand::suggestQuestIds)
                                        .executes(ctx -> startQuest(ctx)))))

                // /story quest complete <player> <id>
                .then(Commands.literal("complete")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(QuestCommand::suggestQuestIds)
                                        .executes(ctx -> completeQuest(ctx)))))

                // /story quest fail <player> <id>
                .then(Commands.literal("fail")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(QuestCommand::suggestQuestIds)
                                        .executes(ctx -> failQuest(ctx)))))

                // /story quest reset <player> <id>
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(QuestCommand::suggestQuestIds)
                                        .executes(ctx -> resetQuest(ctx)))))

                // /story quest task complete <player> <questId> <taskId>
                .then(Commands.literal("task")
                        .then(Commands.literal("complete")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("questId", StringArgumentType.word())
                                                .suggests(QuestCommand::suggestQuestIds)
                                                .then(Commands.argument("taskId", StringArgumentType.word())
                                                        .suggests(QuestCommand::suggestTaskIds)
                                                        .executes(QuestCommand::completeTask)))))

                        // /story quest task remove <questId> <taskId>
                        .then(Commands.literal("remove")
                                .then(Commands.argument("questId", StringArgumentType.word())
                                        .suggests(QuestCommand::suggestQuestIds)
                                        .then(Commands.argument("taskId", StringArgumentType.word())
                                                .suggests(QuestCommand::suggestTaskIds)
                                                .executes(QuestCommand::removeTask))))

                        // /story quest task edit <questId> <taskId> title|description <text>
                        .then(Commands.literal("edit")
                                .then(Commands.argument("questId", StringArgumentType.word())
                                        .suggests(QuestCommand::suggestQuestIds)
                                        .then(Commands.argument("taskId", StringArgumentType.word())
                                                .suggests(QuestCommand::suggestTaskIds)
                                                .then(Commands.literal("title")
                                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                                .executes(ctx -> editTask(ctx, true))))
                                                .then(Commands.literal("description")
                                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                                .executes(ctx -> editTask(ctx, false)))))))

                        // /story quest task add manual <questId> <taskId> <title>
                        .then(Commands.literal("add")
                                .then(Commands.literal("manual")
                                        .then(Commands.argument("questId", StringArgumentType.word())
                                                .suggests(QuestCommand::suggestQuestIds)
                                                .then(Commands.argument("taskId", StringArgumentType.word())
                                                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                                                .executes(QuestCommand::addManualTask)))))

                                // /story quest task add location <questId> <taskId> <dimension> <x> <y> <z> <radius> <title...>
                                .then(Commands.literal("location")
                                        .then(Commands.argument("questId", StringArgumentType.word())
                                                .suggests(QuestCommand::suggestQuestIds)
                                                .then(Commands.argument("taskId", StringArgumentType.word())
                                                        .then(Commands.argument("dimension", ResourceLocationArgument.id())
                                                                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                                                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                                                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                                                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.5))
                                                                                                .then(Commands.argument("title", StringArgumentType.greedyString())
                                                                                                        .executes(QuestCommand::addLocationTask))))))))))

                                // /story quest task add item <questId> <taskId> <itemId> <count> <title...>
                                .then(Commands.literal("item")
                                        .then(Commands.argument("questId", StringArgumentType.word())
                                                .suggests(QuestCommand::suggestQuestIds)
                                                .then(Commands.argument("taskId", StringArgumentType.word())
                                                        .then(Commands.argument("itemId", ResourceLocationArgument.id())
                                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                                                                .executes(QuestCommand::addItemTask)))))))

                                // /story quest task add block <questId> <taskId> <blockId> <count> <title...>
                                .then(Commands.literal("block")
                                        .then(Commands.argument("questId", StringArgumentType.word())
                                                .suggests(QuestCommand::suggestQuestIds)
                                                .then(Commands.argument("taskId", StringArgumentType.word())
                                                        .then(Commands.argument("blockId", ResourceLocationArgument.id())
                                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                                                                .executes(QuestCommand::addBlockTask)))))))

                                // /story quest task add kill <questId> <taskId> <entityType> <count> <title...>
                                .then(Commands.literal("kill")
                                        .then(Commands.argument("questId", StringArgumentType.word())
                                                .suggests(QuestCommand::suggestQuestIds)
                                                .then(Commands.argument("taskId", StringArgumentType.word())
                                                        .then(Commands.argument("entityType", ResourceLocationArgument.id())
                                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                                                                .executes(QuestCommand::addKillTask)))))))))

                // /story quest edit <id> title <text...>
                // /story quest edit <id> description <text...>
                .then(Commands.literal("edit")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(QuestCommand::suggestQuestIds)
                                .then(Commands.literal("title")
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(ctx -> editQuest(ctx, true))))
                                .then(Commands.literal("description")
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(ctx -> editQuest(ctx, false))))));
    }

    // ----------------------------------------------------------------
    // /story quest create
    // ----------------------------------------------------------------
    private static int createQuest(CommandContext<CommandSourceStack> ctx, String title) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        QuestManager manager = StoryEngineMod.QUEST_MANAGER;

        if (manager.exists(id)) {
            return CommandFeedback.fail(source, "Квест с id '" + id + "' уже существует.");
        }

        QuestData data = manager.createTemplate(id, title);
        if (!data.getPrerequisites().isEmpty()) {
            return CommandFeedback.success(source,
                    "Создан шаблон квеста '" + data.getId() + "' (" + data.getTitle() + ") с примером prerequisites и несколькими типами задач.");
        } else {
            return CommandFeedback.success(source,
                    "Создан шаблон квеста '" + data.getId() + "' (" + data.getTitle() + ") в config/story_engine/quests/");
        }
    }

    // ----------------------------------------------------------------
    // /story quest delete <id>
    // ----------------------------------------------------------------
    private static int deleteQuest(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        QuestManager manager = StoryEngineMod.QUEST_MANAGER;

        if (!manager.exists(id)) {
            return CommandFeedback.fail(source, "Квест с id '" + id + "' не найден.");
        }

        String title = manager.getQuest(id).map(QuestData::getTitle).orElse(id);
        manager.delete(id);

        // Прогресс/статус по этому квесту у игроков, что сейчас онлайн, тоже
        // сбрасываем - иначе он останется "висячей" записью без смысла
        // (сам квест для игрока больше нигде не отобразится, но статус в
        // Capability формально сохранится, пока не будет перезаписан).
        for (ServerPlayer online : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            PlayerQuestDataHelper.reset(online, id);
            com.storyengine.network.QuestNetworking.syncToPlayer(online);
        }

        return CommandFeedback.success(source,
                "Квест '" + id + "' (" + title + ") удалён полностью (кэш + файл на диске).");
    }

    // ----------------------------------------------------------------
    // /story quest notify <title> <text> - тост всем игрокам, квестов не касается
    // ----------------------------------------------------------------
    private static int notify(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String title = StringArgumentType.getString(ctx, "title");
        String text = StringArgumentType.getString(ctx, "text");

        com.storyengine.network.QuestNetworking.sendToastToAll(title, text);

        return CommandFeedback.success(source, "Уведомление отправлено всем игрокам онлайн.");
    }

    // ----------------------------------------------------------------
    // /story quest reload
    // ----------------------------------------------------------------
    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        StoryEngineMod.QUEST_MANAGER.reload();
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            com.storyengine.network.QuestNetworking.syncToPlayer(player);
        }
        int size = StoryEngineMod.QUEST_MANAGER.size();
        CommandFeedback.success(source, "Квесты перезагружены. Загружено: " + size);
        return size;
    }

    // ----------------------------------------------------------------
    // /story quest list
    // ----------------------------------------------------------------
    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        QuestManager manager = StoryEngineMod.QUEST_MANAGER;

        if (manager.getAllQuests().isEmpty()) {
            CommandFeedback.info(source, "Нет загруженных квестов.");
            return 0;
        }

        CommandFeedback.info(source, "Загруженные квесты (" + manager.size() + "):");
        for (QuestData quest : manager.getAllQuests()) {
            CommandFeedback.info(source, " - " + quest.getId() + ": " + quest.getTitle());
        }
        return manager.size();
    }

    // ----------------------------------------------------------------
    // /story quest start
    // ----------------------------------------------------------------
    private static int startQuest(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String questId = StringArgumentType.getString(ctx, "id");

        if (!StoryEngineMod.QUEST_MANAGER.exists(questId)) {
            return CommandFeedback.fail(source, "Квест '" + questId + "' не найден.");
        }

        PlayerQuestDataHelper.setStatus(player, questId, QuestStatus.ACTIVE);
        com.storyengine.network.QuestNetworking.syncToPlayer(player);
        return CommandFeedback.success(source,
                "Квест '" + questId + "' переведён в статус ACTIVE у игрока " + player.getName().getString());
    }

    // ----------------------------------------------------------------
    // /story quest complete
    // ----------------------------------------------------------------
    private static int completeQuest(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String questId = StringArgumentType.getString(ctx, "id");

        var questOpt = StoryEngineMod.QUEST_MANAGER.getQuest(questId);
        if (questOpt.isEmpty()) {
            return CommandFeedback.fail(source, "Квест '" + questId + "' не найден.");
        }

        QuestData quest = questOpt.get();
        PlayerQuestDataHelper.setStatus(player, questId, QuestStatus.COMPLETED);
        com.storyengine.network.QuestNetworking.sendQuestStatusMessage(player, "Квест '" + quest.getTitle() + "' выполнен");
        com.storyengine.network.QuestNetworking.syncToPlayer(player);

        // Выполняем команды награды от имени сервера, в контексте игрока
        // (чтобы селекторы вроде @p корректно указывали на этого игрока).
        CommandSourceStack rewardSource = source.getServer().createCommandSourceStack()
                .withEntity(player)
                .withPosition(player.position())
                .withLevel((ServerLevel) player.getLevel())
                .withPermission(4)
                .withSuppressedOutput();

        for (String command : quest.getRewards().getCommands()) {
            source.getServer().getCommands().performPrefixedCommand(rewardSource, command);
        }

        return CommandFeedback.success(source,
                "Квест '" + questId + "' завершён (COMPLETED) у игрока " + player.getName().getString()
                        + ". Выполнено команд награды: " + quest.getRewards().getCommands().size());
    }

    // ----------------------------------------------------------------
    // /story quest fail
    // ----------------------------------------------------------------
    private static int failQuest(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String questId = StringArgumentType.getString(ctx, "id");

        var questOpt = StoryEngineMod.QUEST_MANAGER.getQuest(questId);
        if (questOpt.isEmpty()) {
            return CommandFeedback.fail(source, "Квест '" + questId + "' не найден.");
        }
        QuestData quest = questOpt.get();

        PlayerQuestDataHelper.setStatus(player, questId, QuestStatus.FAILED);
        com.storyengine.network.QuestNetworking.sendQuestStatusMessage(player, "Квест '" + quest.getTitle() + "' провален");
        com.storyengine.network.QuestNetworking.syncToPlayer(player);
        return CommandFeedback.success(source,
                "Квест '" + questId + "' переведён в статус FAILED у игрока " + player.getName().getString());
    }

    // ----------------------------------------------------------------
    // /story quest task complete
    // ----------------------------------------------------------------
    private static int completeTask(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String questId = StringArgumentType.getString(ctx, "questId");
        String taskId = StringArgumentType.getString(ctx, "taskId");

        var questOpt = StoryEngineMod.QUEST_MANAGER.getQuest(questId);
        if (questOpt.isEmpty()) {
            return CommandFeedback.fail(source, "Квест '" + questId + "' не найден.");
        }

        QuestData quest = questOpt.get();
        boolean taskExists = quest.getTasks().stream().anyMatch(task -> task.getId().equals(taskId));
        if (!taskExists) {
            return CommandFeedback.fail(source, "Подзадача '" + taskId + "' не найдена в квесте '" + questId + "'.");
        }

        if (PlayerQuestDataHelper.getStatus(player, questId) != QuestStatus.ACTIVE) {
            return CommandFeedback.fail(source, "Квест '" + questId + "' не активен (ACTIVE) у игрока " + player.getName().getString() + ".");
        }

        PlayerQuestDataHelper.completeTask(player, questId, taskId);
        String taskTitle = quest.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .map(com.storyengine.quest.QuestTask::getTitle)
                .findFirst().orElse(taskId);
        com.storyengine.network.QuestNetworking.sendQuestStatusMessage(player, "Подзадача '" + taskTitle + "' выполнена");

        // Если это была последняя незавершённая задача - завершаем и сам квест.
        // Трекер сделал бы то же самое на следующем тике, но не будем ждать.
        boolean allDone = quest.getTasks().stream()
                .allMatch(task -> PlayerQuestDataHelper.isTaskCompleted(player, questId, task.getId()));
        if (allDone) {
            PlayerQuestDataHelper.setStatus(player, questId, QuestStatus.COMPLETED);
            com.storyengine.network.QuestNetworking.sendQuestStatusMessage(player, "Квест '" + quest.getTitle() + "' выполнен");
        }

        return CommandFeedback.success(source,
                "Подзадача '" + taskId + "' квеста '" + questId + "' отмечена выполненной у игрока " + player.getName().getString());
    }

    // ----------------------------------------------------------------
    // /story quest edit <id> title|description <text>
    // ----------------------------------------------------------------
    private static int editQuest(CommandContext<CommandSourceStack> ctx, boolean editTitle) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        String text = StringArgumentType.getString(ctx, "text");

        var questOpt = StoryEngineMod.QUEST_MANAGER.getQuest(id);
        if (questOpt.isEmpty()) {
            return CommandFeedback.fail(source, "Квест '" + id + "' не найден.");
        }

        QuestData quest = questOpt.get();
        if (editTitle) {
            quest.setTitle(text);
        } else {
            quest.setDescription(text);
        }
        // Сохраняем в JSON, чтобы правка пережила /quest reload и рестарт сервера,
        // а не осталась только в памяти на текущую сессию.
        StoryEngineMod.QUEST_MANAGER.save(quest);

        // Название/описание квеста рассылается всем, а не одному игроку -
        // рассинхронизируем всех, кто сейчас на сервере, как и при /quest reload.
        for (ServerPlayer online : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            com.storyengine.network.QuestNetworking.syncToPlayer(online);
        }

        return CommandFeedback.success(source,
                (editTitle ? "Название" : "Описание") + " квеста '" + id + "' обновлено: " + text);
    }

    // ----------------------------------------------------------------
    // /story quest task remove <questId> <taskId>
    // ----------------------------------------------------------------
    private static int removeTask(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String questId = StringArgumentType.getString(ctx, "questId");
        String taskId = StringArgumentType.getString(ctx, "taskId");

        var questOpt = StoryEngineMod.QUEST_MANAGER.getQuest(questId);
        if (questOpt.isEmpty()) {
            return CommandFeedback.fail(source, "Квест '" + questId + "' не найден.");
        }

        QuestData quest = questOpt.get();
        boolean removed = quest.getTasks().removeIf(task -> task.getId().equals(taskId));
        if (!removed) {
            return CommandFeedback.fail(source, "Подзадача '" + taskId + "' не найдена в квесте '" + questId + "'.");
        }

        applyTaskListChange(quest);
        return CommandFeedback.success(source,
                "Подзадача '" + taskId + "' удалена из квеста '" + questId + "'.");
    }

    // ----------------------------------------------------------------
    // /story quest task edit <questId> <taskId> title|description <text>
    // ----------------------------------------------------------------
    private static int editTask(CommandContext<CommandSourceStack> ctx, boolean editTitle) {
        CommandSourceStack source = ctx.getSource();
        String questId = StringArgumentType.getString(ctx, "questId");
        String taskId = StringArgumentType.getString(ctx, "taskId");
        String text = StringArgumentType.getString(ctx, "text");

        var questOpt = StoryEngineMod.QUEST_MANAGER.getQuest(questId);
        if (questOpt.isEmpty()) {
            return CommandFeedback.fail(source, "Квест '" + questId + "' не найден.");
        }

        QuestData quest = questOpt.get();
        var taskOpt = quest.getTasks().stream().filter(task -> task.getId().equals(taskId)).findFirst();
        if (taskOpt.isEmpty()) {
            return CommandFeedback.fail(source, "Подзадача '" + taskId + "' не найдена в квесте '" + questId + "'.");
        }

        QuestTask task = taskOpt.get();
        if (editTitle) {
            task.setTitle(text);
        } else {
            task.setDescription(text);
        }

        applyTaskListChange(quest);
        return CommandFeedback.success(source,
                (editTitle ? "Название" : "Описание") + " подзадачи '" + taskId + "' обновлено: " + text);
    }

    // ----------------------------------------------------------------
    // /story quest task add manual|location|item|block|kill
    // ----------------------------------------------------------------
    private static int addManualTask(CommandContext<CommandSourceStack> ctx) {
        String title = StringArgumentType.getString(ctx, "title");
        ManualQuestTask task = new ManualQuestTask(
                StringArgumentType.getString(ctx, "taskId"), title, ""
        );
        return finishAddTask(ctx, task, title);
    }

    private static int addLocationTask(CommandContext<CommandSourceStack> ctx) {
        String title = StringArgumentType.getString(ctx, "title");
        LocationQuestTask task = new LocationQuestTask();
        task.setId(StringArgumentType.getString(ctx, "taskId"));
        task.setTitle(title);
        task.setDescription("");
        task.setDimension(net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "dimension").toString());
        task.setX(DoubleArgumentType.getDouble(ctx, "x"));
        task.setY(DoubleArgumentType.getDouble(ctx, "y"));
        task.setZ(DoubleArgumentType.getDouble(ctx, "z"));
        task.setRadius(DoubleArgumentType.getDouble(ctx, "radius"));
        return finishAddTask(ctx, task, title);
    }

    private static int addItemTask(CommandContext<CommandSourceStack> ctx) {
        String title = StringArgumentType.getString(ctx, "title");
        ItemQuestTask task = new ItemQuestTask();
        task.setId(StringArgumentType.getString(ctx, "taskId"));
        task.setTitle(title);
        task.setDescription("");
        task.setTarget(net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "itemId").toString());
        task.setCount(IntegerArgumentType.getInteger(ctx, "count"));
        return finishAddTask(ctx, task, title);
    }

    private static int addBlockTask(CommandContext<CommandSourceStack> ctx) {
        String title = StringArgumentType.getString(ctx, "title");
        BlockBreakQuestTask task = new BlockBreakQuestTask();
        task.setId(StringArgumentType.getString(ctx, "taskId"));
        task.setTitle(title);
        task.setDescription("");
        task.setBlockId(net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "blockId").toString());
        task.setCount(IntegerArgumentType.getInteger(ctx, "count"));
        return finishAddTask(ctx, task, title);
    }

    private static int addKillTask(CommandContext<CommandSourceStack> ctx) {
        String title = StringArgumentType.getString(ctx, "title");
        KillEntityQuestTask task = new KillEntityQuestTask();
        task.setId(StringArgumentType.getString(ctx, "taskId"));
        task.setTitle(title);
        task.setDescription("");
        task.setEntityType(net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "entityType").toString());
        task.setCount(IntegerArgumentType.getInteger(ctx, "count"));
        return finishAddTask(ctx, task, title);
    }

    /**
     * Общая часть всех "/story quest task add ..." - проверка квеста/дубликата id,
     * добавление в список задач, сохранение и рассылка изменений.
     */
    private static int finishAddTask(CommandContext<CommandSourceStack> ctx, QuestTask task, String title) {
        CommandSourceStack source = ctx.getSource();
        String questId = StringArgumentType.getString(ctx, "questId");

        var questOpt = StoryEngineMod.QUEST_MANAGER.getQuest(questId);
        if (questOpt.isEmpty()) {
            return CommandFeedback.fail(source, "Квест '" + questId + "' не найден.");
        }

        QuestData quest = questOpt.get();
        boolean duplicate = quest.getTasks().stream().anyMatch(existing -> existing.getId().equals(task.getId()));
        if (duplicate) {
            return CommandFeedback.fail(source, "Подзадача с id '" + task.getId() + "' уже существует в квесте '" + questId + "'.");
        }

        quest.getTasks().add(task);
        applyTaskListChange(quest);

        return CommandFeedback.success(source,
                "Подзадача '" + task.getId() + "' (" + title + ") добавлена в квест '" + questId + "'.");
    }

    /**
     * Сохраняет квест на диск и рассылает синхронизацию всем игрокам онлайн -
     * общий хвост для add/remove/edit подзадач, т.к. список задач квеста
     * общий (не персональный для игрока), как и его title/description.
     */
    private static void applyTaskListChange(QuestData quest) {
        StoryEngineMod.QUEST_MANAGER.save(quest);
        for (ServerPlayer online : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            com.storyengine.network.QuestNetworking.syncToPlayer(online);
        }
    }

    // ----------------------------------------------------------------
    // /story quest reset
    // ----------------------------------------------------------------
    private static int resetQuest(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String questId = StringArgumentType.getString(ctx, "id");

        PlayerQuestDataHelper.reset(player, questId);
        com.storyengine.network.QuestNetworking.syncToPlayer(player);
        return CommandFeedback.success(source,
                "Прогресс квеста '" + questId + "' сброшен у игрока " + player.getName().getString());
    }

    // ----------------------------------------------------------------
    // Автодополнение id квестов
    // ----------------------------------------------------------------
    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestQuestIds(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                StoryEngineMod.QUEST_MANAGER.getAllQuests().stream().map(QuestData::getId),
                builder
        );
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestTaskIds(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String questId;
        try {
            questId = StringArgumentType.getString(ctx, "questId");
        } catch (IllegalArgumentException e) {
            return SharedSuggestionProvider.suggest(java.util.stream.Stream.<String>empty(), builder);
        }
        return SharedSuggestionProvider.suggest(
                StoryEngineMod.QUEST_MANAGER.getQuest(questId)
                        .map(quest -> quest.getTasks().stream().map(QuestTask::getId))
                        .orElse(java.util.stream.Stream.empty()),
                builder
        );
    }
}
