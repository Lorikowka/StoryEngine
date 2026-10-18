package com.storyengine.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.storyengine.StoryEngineMod;
import com.storyengine.narrative.NarrativeMessage;
import com.storyengine.narrative.NarrativeStyle;
import com.storyengine.network.NarrativeNetworking;
import com.storyengine.network.QuestNetworking;
import com.storyengine.player.PlayerQuestDataHelper;
import com.storyengine.quest.LocationQuestTask;
import com.storyengine.quest.ManualQuestTask;
import com.storyengine.quest.QuestData;
import com.storyengine.quest.QuestManager;
import com.storyengine.quest.QuestRewards;
import com.storyengine.quest.QuestStatus;
import com.storyengine.quest.QuestTask;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.List;

/**
 * Публичный фасад Story Engine для вызова из внешнего кода: других модов
 * и скриптов HollowEngine (.se.kts).
 *
 * <h2>Как это связано с HollowEngine</h2>
 * Скрипты HollowEngine компилируются с classpath, в который входят <b>все
 * установленные моды</b> (см. {@code AbstractHollowScriptConfiguration}), поэтому
 * из скрипта достаточно обычного импорта:
 *
 * <pre>{@code
 * import com.storyengine.api.StoryApi
 *
 * // ... где-то в теле скрипта:
 * StoryApi.tell(server, "Орен", "oren", "Текст", 0x36612F)
 * }</pre>
 *
 * <b>Важно про момент срабатывания.</b> Тело скрипта HollowEngine исполняется
 * один раз — в момент сборки дерева узлов, а не по ходу сюжета. Поэтому
 * «немедленные» методы этого класса ({@link #tell}, {@link #quest} и т.п.),
 * вызванные прямо в теле, сработают на старте скрипта. Чтобы реплика/квест
 * сработали в нужной точке сюжета, оборачивайте вызов в узел:
 *
 * <pre>{@code
 * execute { StoryApi.tellCommand("Орен", "oren", "36612F", "Текст") }
 * execute { StoryApi.questStartCommand("@a", "routine_work") }
 * }</pre>
 *
 * Для сюжетных квестов предпочтителен <b>прямой API</b> (раздел «Прямой API
 * квестов»): он не идёт через Brigadier, идемпотентен (повторный запуск сюжета
 * безопасен) и не засоряет лог сообщениями «execution failed!». Такие методы
 * возвращают {@code void}, поэтому вызывайте их из узла side-effect {@code next { }}:
 *
 * <pre>{@code
 * next { StoryApi.ensureQuest("routine_work", "Дом - работа - дом.", "...") }
 * next { StoryApi.addManualTask("routine_work", "exit_corridor", "Выйти в коридор") }
 * next { StoryApi.startQuestAll("routine_work") }
 * }</pre>
 *
 * Для «прямой» отправки (без строк-команд) есть {@link #tell(Collection, String, String, String, int, NarrativeStyle)};
 * он уместен из сетевого кода, обработчиков событий и других модов.
 *
 * <p>Все методы рассчитаны на вызов из серверного потока.</p>
 */
public final class StoryApi {

    private StoryApi() {
    }

    // ==================================================================
    //  Сюжетный чат (Narrative HUD) — прямая отправка
    // ==================================================================

    /** Отправить реплику всем игрокам сервера. */
    public static void tell(MinecraftServer server, String speaker, String icon, String text) {
        tell(server, speaker, icon, text, NarrativeMessage.DEFAULT_NAME_COLOR);
    }

    /** Отправить реплику всем игрокам сервера с заданным цветом имени. */
    public static void tell(MinecraftServer server, String speaker, String icon, String text, int nameColor) {
        tell(server, speaker, icon, text, nameColor, NarrativeStyle.NORMAL);
    }

    /** Отправить реплику всем игрокам сервера с цветом и стилем. */
    public static void tell(MinecraftServer server, String speaker, String icon, String text,
                            int nameColor, NarrativeStyle style) {
        if (server == null) {
            return;
        }
        tell(server.getPlayerList().getPlayers(), speaker, icon, text, nameColor, style);
    }

    /** Отправить реплику списку игроков. */
    public static void tell(Collection<ServerPlayer> players, String speaker, String icon, String text) {
        tell(players, speaker, icon, text, NarrativeMessage.DEFAULT_NAME_COLOR, NarrativeStyle.NORMAL);
    }

    /** Отправить реплику списку игроков с заданным цветом имени. */
    public static void tell(Collection<ServerPlayer> players, String speaker, String icon, String text, int nameColor) {
        tell(players, speaker, icon, text, nameColor, NarrativeStyle.NORMAL);
    }

    /** Отправить реплику списку игроков с цветом и стилем. */
    public static void tell(Collection<ServerPlayer> players, String speaker, String icon, String text,
                            int nameColor, NarrativeStyle style) {
        if (players == null || players.isEmpty()) {
            return;
        }
        NarrativeNetworking.sendToPlayers(players, speaker, icon, Component.literal(text == null ? "" : text),
                nameColor, style == null ? NarrativeStyle.NORMAL : style);
    }

    /** Отправить реплику одному игроку. */
    public static void tellTo(ServerPlayer player, String speaker, String icon, String text, int nameColor) {
        if (player == null) {
            return;
        }
        tell(List.of(player), speaker, icon, text, nameColor, NarrativeStyle.NORMAL);
    }

    /** Реплика в режиме «мыслей» (курсив + особый оттенок иконки). */
    public static void thought(MinecraftServer server, String speaker, String icon, String text, int nameColor) {
        tell(server, speaker, icon, text, nameColor, NarrativeStyle.THOUGHT);
    }

    /** Реплика в режиме «шёпота» (добавляет «(шепотом)»). */
    public static void whisper(MinecraftServer server, String speaker, String icon, String text, int nameColor) {
        tell(server, speaker, icon, text, nameColor, NarrativeStyle.WHISPER);
    }

    // ==================================================================
    //  Команды /story (немедленное исполнение)
    // ==================================================================

    /** Выполнить произвольную команду от имени консоли сервера (без ведущего слэша). */
    public static int command(MinecraftServer server, String command) {
        if (server == null || command == null || command.isBlank()) {
            return 0;
        }
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        return server.getCommands().performPrefixedCommand(source, command);
    }

    /** {@code /story quest ...} */
    public static int quest(MinecraftServer server, String args) {
        return command(server, "story quest " + args);
    }

    /** {@code /story dialogue ...} */
    public static int dialogue(MinecraftServer server, String args) {
        return command(server, "story dialogue " + args);
    }

    /** {@code /story trigger ...} */
    public static int trigger(MinecraftServer server, String args) {
        return command(server, "story trigger " + args);
    }

    /** {@code /story menu ...} */
    public static int menu(MinecraftServer server, String args) {
        return command(server, "story menu " + args);
    }

    // ---- Типизированные обёртки квестов ----

    /** {@code /story quest start <player> <id>} */
    public static int questStart(MinecraftServer server, ServerPlayer player, String questId) {
        return quest(server, "start " + name(player) + " " + questId);
    }

    /** {@code /story quest complete <player> <id>} */
    public static int questComplete(MinecraftServer server, ServerPlayer player, String questId) {
        return quest(server, "complete " + name(player) + " " + questId);
    }

    /** {@code /story quest fail <player> <id>} */
    public static int questFail(MinecraftServer server, ServerPlayer player, String questId) {
        return quest(server, "fail " + name(player) + " " + questId);
    }

    /** {@code /story quest reset <player> <id>} */
    public static int questReset(MinecraftServer server, ServerPlayer player, String questId) {
        return quest(server, "reset " + name(player) + " " + questId);
    }

    /** {@code /story quest task complete <player> <questId> <taskId>} */
    public static int questTaskComplete(MinecraftServer server, ServerPlayer player, String questId, String taskId) {
        return quest(server, "task complete " + name(player) + " " + questId + " " + taskId);
    }

    /** {@code /story quest create <id> <title>} */
    public static int questCreate(MinecraftServer server, String questId, String title) {
        return quest(server, "create " + questId + " " + title);
    }

    /** {@code /story quest notify "<title>" <text>} — тост всем игрокам. */
    public static int questNotify(MinecraftServer server, String title, String text) {
        return quest(server, "notify " + new JsonPrimitive(title == null ? "" : title) + " " + text);
    }

    // ---- Типизированные обёртки диалогов ----

    /** {@code /story dialogue start <player> <id> [nodeId]} */
    public static int dialogueStart(MinecraftServer server, ServerPlayer player, String dialogueId, String nodeId) {
        String cmd = "start " + name(player) + " " + dialogueId;
        if (nodeId != null && !nodeId.isBlank()) {
            cmd = cmd + " " + nodeId;
        }
        return dialogue(server, cmd);
    }

    /** {@code /story dialogue stop <player>} */
    public static int dialogueStop(MinecraftServer server, ServerPlayer player) {
        return dialogue(server, "stop " + name(player));
    }

    // ==================================================================
    //  Строители строк-команд для узла execute { } в HollowEngine
    // ==================================================================

    /**
     * Собрать команду {@code story tell @a "speaker" icon color <hex> <json>}
     * с корректным экранированием текста — для узла {@code execute { ... }}.
     */
    public static String tellCommand(String speaker, String icon, String colorHex, String text) {
        return tellCommand("@a", speaker, icon, colorHex, text, false, NarrativeStyle.NORMAL);
    }

    /** То же, но с курсивом (мысли). */
    public static String tellThoughtCommand(String speaker, String icon, String colorHex, String text) {
        return tellCommand("@a", speaker, icon, colorHex, text, true, NarrativeStyle.THOUGHT);
    }

    /** Полная версия строителя команды реплики. */
    public static String tellCommand(String targets, String speaker, String icon, String colorHex,
                                     String text, boolean italic, NarrativeStyle style) {
        JsonObject json = new JsonObject();
        json.addProperty("text", text == null ? "" : text);
        if (italic) {
            json.addProperty("italic", true);
        }

        StringBuilder sb = new StringBuilder("story tell ");
        sb.append(targets == null || targets.isBlank() ? "@a" : targets).append(' ');
        sb.append(new JsonPrimitive(speaker == null ? "" : speaker)).append(' ');
        sb.append(icon == null || icon.isBlank() ? "none" : icon).append(' ');

        if (style == NarrativeStyle.WHISPER) {
            sb.append("whisper ");
        } else if (style == NarrativeStyle.THOUGHT) {
            sb.append("thought ");
        }

        String hex = normalizeHex(colorHex);
        if (hex != null) {
            sb.append("color ").append(hex).append(' ');
        }

        sb.append(json);
        return sb.toString();
    }

    /** {@code story quest start <targets> <id>}. */
    public static String questStartCommand(String targets, String questId) {
        return "story quest start " + orDefault(targets, "@a") + " " + questId;
    }

    /** {@code story quest complete <targets> <id>}. */
    public static String questCompleteCommand(String targets, String questId) {
        return "story quest complete " + orDefault(targets, "@a") + " " + questId;
    }

    /** {@code story quest fail <targets> <id>}. */
    public static String questFailCommand(String targets, String questId) {
        return "story quest fail " + orDefault(targets, "@a") + " " + questId;
    }

    /** {@code story quest reset <targets> <id>}. */
    public static String questResetCommand(String targets, String questId) {
        return "story quest reset " + orDefault(targets, "@a") + " " + questId;
    }

    /** {@code story quest task complete <targets> <questId> <taskId>}. */
    public static String questTaskCompleteCommand(String targets, String questId, String taskId) {
        return "story quest task complete " + orDefault(targets, "@a") + " " + questId + " " + taskId;
    }

    // ==================================================================
    //  Прямой API квестов (без Brigadier-команд)
    // ==================================================================
    //
    //  Эти методы работают с QuestManager/PlayerQuestDataHelper напрямую и
    //  специально сделаны идемпотентными: повторный запуск сюжета не ломает
    //  квесты и не сыпет в лог «execution failed!» (это сообщение HollowEngine
    //  печатает, когда команда возвращает 0, что для нас норма при ре-ране).
    //
    //  Вызывать из скриптов через узел side-effect next { }:
    //      next { StoryApi.ensureQuest("routine_work", "Дом - работа - дом.", "...") }
    //      next { StoryApi.addManualTask("routine_work", "exit_corridor", "Выйти в коридор") }
    //      next { StoryApi.startQuestAll("routine_work") }
    //
    //  Сервер берётся из ServerLifecycleHooks.getCurrentServer(): в скриптовых
    //  extension-функциях с receiver IContextBuilder нет доступа к server
    //  (он член StoryStateMachine/StoryScript), поэтому передавать его не нужно.

    /** Идентификаторы дефолтных задач, которые createTemplate кладёт в шаблон. */
    private static final String DEFAULT_TASK_EXAMPLE = "example_task";
    private static final String DEFAULT_TASK_LOCATION = "location_task";

    /**
     * Идемпотентно готовит квест: создаёт, если его ещё нет, иначе обновляет
     * title/description. Награды всегда очищаются — в сюжете они не нужны
     * (createTemplate по умолчанию кладёт изумруды/алмазы/опыт, их быть не должно).
     *
     * @return сам квест или {@code null}, если что-то не так с аргументами.
     */
    public static QuestData ensureQuest(String questId, String title, String description) {
        if (questId == null || questId.isBlank()) {
            return null;
        }
        QuestManager manager = StoryEngineMod.QUEST_MANAGER;
        QuestData quest = manager.getQuest(questId).orElse(null);
        if (quest == null) {
            // createTemplate сразу сохраняет файл и кладёт квест в кэш.
            quest = manager.createTemplate(questId, title);
        }
        if (title != null && !title.isBlank()) {
            quest.setTitle(title);
        }
        if (description != null) {
            quest.setDescription(description);
        }
        // Никаких наград: даже если ранее квест создавался через /story quest create,
        // дефолтные изумруды/алмазы/опыт убираем при каждом вызове.
        quest.setRewards(new QuestRewards());
        manager.save(quest);
        syncAll();
        return quest;
    }

    /** Обновить только описание существующего квеста. */
    public static void editQuestDescription(String questId, String description) {
        if (questId == null || description == null) {
            return;
        }
        QuestManager manager = StoryEngineMod.QUEST_MANAGER;
        QuestData quest = manager.getQuest(questId).orElse(null);
        if (quest == null) {
            return;
        }
        quest.setDescription(description);
        manager.save(quest);
        syncAll();
    }

    /**
     * Добавить MANUAL-подзадачу. Если задача с таким id уже есть — обновляет её
     * заголовок (идемпотентно). Заодно вычищает дефолтные задачи шаблона
     * (example_task/location_task), но ТОЛЬКО когда в квесте уже есть настоящая
     * задача — иначе квест на миг остался бы без задач и трекер мгновенно его
     * завершил бы.
     */
    public static void addManualTask(String questId, String taskId, String title) {
        if (questId == null || taskId == null) {
            return;
        }
        QuestManager manager = StoryEngineMod.QUEST_MANAGER;
        QuestData quest = manager.getQuest(questId).orElse(null);
        if (quest == null) {
            return;
        }
        QuestTask existing = findTask(quest, taskId);
        if (existing != null) {
            existing.setTitle(title);
        } else {
            quest.getTasks().add(new ManualQuestTask(taskId, title, ""));
        }
        stripDefaultTasks(quest);
        manager.save(quest);
        syncAll();
    }

    /**
     * Добавить LOCATION-подзадачу (выполняется автоматически при подходе игрока).
     * Идемпотентно по taskId, дефолтные задачи шаблона вычищаются там же.
     */
    public static void addLocationTask(String questId, String taskId,
                                       String dimension, double x, double y, double z, double radius, String title) {
        if (questId == null || taskId == null) {
            return;
        }
        QuestManager manager = StoryEngineMod.QUEST_MANAGER;
        QuestData quest = manager.getQuest(questId).orElse(null);
        if (quest == null) {
            return;
        }
        QuestTask existing = findTask(quest, taskId);
        LocationQuestTask task = existing instanceof LocationQuestTask location ? location : new LocationQuestTask();
        if (existing == null) {
            quest.getTasks().add(task);
        }
        task.setId(taskId);
        task.setTitle(title);
        if (task.getDescription() == null) {
            task.setDescription("");
        }
        task.setDimension(dimension);
        task.setX(x);
        task.setY(y);
        task.setZ(z);
        task.setRadius(radius);
        stripDefaultTasks(quest);
        manager.save(quest);
        syncAll();
    }

    /** Удалить подзадачу, если она есть (идемпотентно). */
    public static void removeTask(String questId, String taskId) {
        if (questId == null || taskId == null) {
            return;
        }
        QuestManager manager = StoryEngineMod.QUEST_MANAGER;
        QuestData quest = manager.getQuest(questId).orElse(null);
        if (quest == null) {
            return;
        }
        if (!quest.getTasks().removeIf(task -> taskId.equals(task.getId()))) {
            return;
        }
        manager.save(quest);
        syncAll();
    }

    // ---- Игрок и прогресс ----

    /** Перевести квест в ACTIVE у всех игроков онлайн. */
    public static void startQuestAll(String questId) {
        for (ServerPlayer player : online()) {
            PlayerQuestDataHelper.setStatus(player, questId, QuestStatus.ACTIVE);
        }
    }

    /** Перевести квест в ACTIVE у игрока. */
    public static void startQuest(ServerPlayer player, String questId) {
        if (player == null || questId == null) {
            return;
        }
        PlayerQuestDataHelper.setStatus(player, questId, QuestStatus.ACTIVE);
    }

    /** Сбросить прогресс квеста у всех игроков онлайн. */
    public static void resetQuestAll(String questId) {
        for (ServerPlayer player : online()) {
            PlayerQuestDataHelper.reset(player, questId);
        }
    }

    /** Сбросить прогресс квеста у игрока. */
    public static void resetQuest(ServerPlayer player, String questId) {
        if (player == null || questId == null) {
            return;
        }
        PlayerQuestDataHelper.reset(player, questId);
    }

    /** Отметить подзадачу выполненной у всех игроков онлайн. */
    public static void completeTaskAll(String questId, String taskId) {
        for (ServerPlayer player : online()) {
            completeTask(player, questId, taskId);
        }
    }

    /**
     * Отметить подзадачу выполненной. Если после этого не осталось незавершённых
     * задач — квест сразу переводится в COMPLETED. Награды не выдаются.
     */
    public static void completeTask(ServerPlayer player, String questId, String taskId) {
        if (player == null || questId == null || taskId == null) {
            return;
        }
        QuestData quest = StoryEngineMod.QUEST_MANAGER.getQuest(questId).orElse(null);
        if (quest == null || findTask(quest, taskId) == null) {
            return;
        }
        PlayerQuestDataHelper.completeTask(player, questId, taskId);
        QuestNetworking.sendQuestStatusMessage(player, "Подзадача '" + taskTitle(quest, taskId) + "' выполнена");
        boolean allDone = quest.getTasks().stream()
                .allMatch(task -> PlayerQuestDataHelper.isTaskCompleted(player, questId, task.getId()));
        if (allDone) {
            PlayerQuestDataHelper.setStatus(player, questId, QuestStatus.COMPLETED);
            QuestNetworking.sendQuestStatusMessage(player, "Квест '" + quest.getTitle() + "' выполнен");
        }
    }

    /** Принудительно завершить квест у всех игроков онлайн. */
    public static void completeQuestAll(String questId) {
        for (ServerPlayer player : online()) {
            completeQuest(player, questId);
        }
    }

    /** Принудительно завершить квест у игрока. Награды не выдаются. */
    public static void completeQuest(ServerPlayer player, String questId) {
        if (player == null || questId == null) {
            return;
        }
        QuestData quest = StoryEngineMod.QUEST_MANAGER.getQuest(questId).orElse(null);
        if (quest == null) {
            return;
        }
        PlayerQuestDataHelper.setStatus(player, questId, QuestStatus.COMPLETED);
        QuestNetworking.sendQuestStatusMessage(player, "Квест '" + quest.getTitle() + "' выполнен");
    }

    /** Провалить квест у всех игроков онлайн. */
    public static void failQuestAll(String questId) {
        for (ServerPlayer player : online()) {
            failQuest(player, questId);
        }
    }

    /** Провалить квест у игрока. */
    public static void failQuest(ServerPlayer player, String questId) {
        if (player == null || questId == null) {
            return;
        }
        QuestData quest = StoryEngineMod.QUEST_MANAGER.getQuest(questId).orElse(null);
        if (quest == null) {
            return;
        }
        PlayerQuestDataHelper.setStatus(player, questId, QuestStatus.FAILED);
        QuestNetworking.sendQuestStatusMessage(player, "Квест '" + quest.getTitle() + "' провален");
    }

    /** Тост-уведомление всем игрокам онлайн (аналог {@code /story quest notify}). */
    public static void notify(String title, String text) {
        QuestNetworking.sendToastToAll(title == null ? "Квест" : title, text == null ? "" : text);
    }

    // ==================================================================
    //  Мини-игра: документ в бочке — проверка и потребление ПО ИМЕНИ
    // ==================================================================

    /**
     * Лежит ли в контейнере (бочке) на позиции предмет указанного типа с точным
     * display-именем. Это аналог {@code waitStorage}, но с проверкой имени:
     * документ с другим именем (хоть и того же типа) принят не будет.
     *
     * @param world  идентификатор измерения, напр. {@code minecraft:overworld}
     * @param itemId идентификатор предмета: {@code minecraft:paper} или {@code paper}
     * @param name   точное display-имя документа (то, что задано в GiveDocument/GiveBook)
     */
    public static boolean barrelHasNamedItem(String world, double x, double y, double z, String itemId, String name) {
        IItemHandler handler = barrelHandler(world, x, y, z);
        if (handler == null) {
            return false;
        }
        Item expected = itemById(itemId);
        if (expected == null) {
            return false;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (matches(handler.getStackInSlot(slot), expected, name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Забрать ОДИН указанный документ из бочки (по типу + точному имени).
     * Возвращает {@code true}, только если документ был найден и извлечён.
     * Удобно гонять из узла ожидания — вернёт истину в тик, когда игрок положил
     * нужный документ, и сразу заберёт его:
     *
     * <pre>{@code
     * await { StoryApi.consumeNamedItem("minecraft:overworld", -54.0, 56.0, 6.0, "minecraft:paper", "Папка №13-А") }
     * }</pre>
     */
    public static boolean consumeNamedItem(String world, double x, double y, double z, String itemId, String name) {
        IItemHandler handler = barrelHandler(world, x, y, z);
        if (handler == null) {
            return false;
        }
        Item expected = itemById(itemId);
        if (expected == null) {
            return false;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (matches(handler.getStackInSlot(slot), expected, name)) {
                handler.extractItem(slot, 1, false);
                return true;
            }
        }
        return false;
    }

    private static boolean matches(ItemStack stack, Item expected, String name) {
        return !stack.isEmpty()
                && stack.getItem() == expected
                && stack.getHoverName().getString().equals(name);
    }

    private static Item itemById(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        ResourceLocation id = itemId.contains(":")
                ? new ResourceLocation(itemId)
                : new ResourceLocation("minecraft", itemId);
        return ForgeRegistries.ITEMS.getValue(id);
    }

    private static IItemHandler barrelHandler(String world, double x, double y, double z) {
        MinecraftServer server = currentServer();
        if (server == null) {
            return null;
        }
        ResourceLocation rl = world == null || world.isEmpty()
                ? new ResourceLocation("minecraft", "overworld")
                : new ResourceLocation(world);
        ServerLevel level = server.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, rl));
        if (level == null) {
            return null;
        }
        BlockPos pos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
    }

    // ==================================================================
    //  Выдача книг напрямую (без /give)
    // ==================================================================

    /**
     * Выдать книгу всем игрокам онлайн: подписанную ({@code written_book}) или с пером
     * ({@code writable_book}). Страницы — обычные текстовые строки, переводы строк внутри
     * страницы сохраняются (абзацы, списки). Не идёт через команду {@code /give}, поэтому
     * не ломается на спецсимволах и переносах.
     *
     * <p>Удобно гонять из узла {@code next { }} со случайным целым вариантом книги:
     * <pre>{@code
     * next { StoryApi.giveBook("Отчёт за квартал", "Отчёт", "Бухгалтерия", variants.random(), true) }
     * }</pre>
     *
     * @param name   display-имя предмета — по нему бочка принимает документ
     *               ({@link #consumeNamedItem}); точное имя из сюжета.
     * @param title  заголовок подписанной книги (для writable_book игнорируется).
     * @param author автор подписанной книги (для writable_book игнорируется).
     * @param pages  список страниц; каждая строка — содержимое одной страницы.
     * @param signed {@code true} — подписанная книга, {@code false} — книга с пером.
     */
    public static void giveBook(String name, String title, String author, List<String> pages, boolean signed) {
        MinecraftServer server = currentServer();
        if (server == null) {
            return;
        }
        giveBook(server.getPlayerList().getPlayers(), name, title, author, pages, signed);
    }

    /** Выдать книгу заданному списку игроков. */
    public static void giveBook(Collection<ServerPlayer> players, String name, String title, String author,
                                List<String> pages, boolean signed) {
        if (players == null || players.isEmpty()) {
            return;
        }
        ItemStack stack = makeBook(name, title, author, pages, signed);
        if (stack.isEmpty()) {
            return;
        }
        for (ServerPlayer player : players) {
            if (!player.getInventory().add(stack.copy())) {
                player.drop(stack.copy(), false);
            }
        }
    }

    /** Собрать ItemStack книги с нужным display-именем и страницами. */
    private static ItemStack makeBook(String name, String title, String author, List<String> pages, boolean signed) {
        ItemStack stack = new ItemStack(signed ? Items.WRITTEN_BOOK : Items.WRITABLE_BOOK);
        CompoundTag tag = stack.getOrCreateTag();

        // display.Name — JSON-компонент, чтобы имя отображалось как задумано.
        if (name != null && !name.isEmpty()) {
            CompoundTag display = new CompoundTag();
            display.putString("Name", Component.Serializer.toJson(Component.literal(name)));
            tag.put("display", display);
        }

        // Страницы: каждая строка превращается в JSON-компонент (переводы строк рендерятся).
        ListTag pageList = new ListTag();
        if (pages != null) {
            for (String page : pages) {
                pageList.add(StringTag.valueOf(
                        Component.Serializer.toJson(Component.literal(page == null ? "" : page))));
            }
        }
        tag.put("pages", pageList);

        if (signed) {
            tag.putString("title", title == null ? "" : title);
            tag.putString("author", author == null ? "" : author);
            tag.putInt("generation", 0);
            tag.putBoolean("resolved", true);
        }
        return stack;
    }

    // ---- Внутренние помощники прямого API ----

    private static MinecraftServer currentServer() {
        return net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    private static List<ServerPlayer> online() {
        MinecraftServer server = currentServer();
        return server == null ? List.of() : server.getPlayerList().getPlayers();
    }

    private static void syncAll() {
        for (ServerPlayer player : online()) {
            QuestNetworking.syncToPlayer(player);
        }
    }

    private static QuestTask findTask(QuestData quest, String taskId) {
        return quest.getTasks().stream().filter(task -> taskId.equals(task.getId())).findFirst().orElse(null);
    }

    private static String taskTitle(QuestData quest, String taskId) {
        QuestTask task = findTask(quest, taskId);
        return task == null || task.getTitle() == null ? taskId : task.getTitle();
    }

    /**
     * Убирает дефолтные задачи шаблона, но не оставляет квест пустым:
     * удаление происходит, только если в квесте уже есть «настоящая» задача.
     */
    private static void stripDefaultTasks(QuestData quest) {
        boolean hasRealTask = quest.getTasks().stream()
                .anyMatch(task -> !isDefaultTask(task.getId()));
        if (!hasRealTask) {
            return;
        }
        quest.getTasks().removeIf(task -> isDefaultTask(task.getId()));
    }

    private static boolean isDefaultTask(String taskId) {
        return DEFAULT_TASK_EXAMPLE.equals(taskId) || DEFAULT_TASK_LOCATION.equals(taskId);
    }

    // ==================================================================
    //  Утилиты
    // ==================================================================

    /** Разобрать hex-цвет вида «36612F» / «#36612F» в int 0xRRGGBB. Неверный формат → жёлтый. */
    public static int hexColor(String hex) {
        String normalized = normalizeHex(hex);
        return normalized == null ? NarrativeMessage.DEFAULT_NAME_COLOR : Integer.parseInt(normalized, 16);
    }

    /** Нормализовать hex-цвет к 6 цифрам без «#», либо null. */
    private static String normalizeHex(String hex) {
        if (hex == null) {
            return null;
        }
        String value = hex.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() == 8) {
            // ARGB -> RGB (старшая альфа отбрасывается, как в команде /story tell color)
            value = value.substring(2);
        }
        if (value.length() != 6) {
            return null;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return null;
            }
        }
        return value.toUpperCase();
    }

    private static String name(ServerPlayer player) {
        return player == null ? "@a" : player.getGameProfile().getName();
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
