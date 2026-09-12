package com.storyengine.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.storyengine.StoryEngineMod;
import com.storyengine.trigger.TriggerDefinition;
import com.storyengine.trigger.TriggerEvent;
import com.storyengine.trigger.TriggerExecutor;
import com.storyengine.trigger.TriggerHintService;
import com.storyengine.trigger.TriggerManager;
import com.storyengine.trigger.TriggerContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Подкоманда {@code /story trigger} (этап 7 ТЗ §17):
 *
 *   /story trigger reload                 — перечитать JSON-триггеры с диска
 *   /story trigger list [event]           — список загруженных триггеров
 *   /story trigger info <id>              — детали конкретного триггера
 *   /story trigger create <id> [event]    — создать шаблон-файл триггера
 *   /story trigger delete <id>            — удалить триггер (кэш + файл)
 *   /story trigger enable <id>            — включить триггер
 *   /story trigger disable <id>           — отключить триггер
 *   /story trigger test <triggerId> <player> — проверить доступность для игрока
 *   /story trigger debug <player>         — что сейчас под прицелом у игрока
 *
 * Все команды требуют permission level 2.
 */
public final class TriggerCommand {

    private TriggerCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build(String literal) {
        return Commands.literal(literal)
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(TriggerCommand::reload))

                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx, null))
                        .then(Commands.argument("event", StringArgumentType.word())
                                .suggests(TriggerCommand::suggestEvents)
                                .executes(ctx -> list(ctx, StringArgumentType.getString(ctx, "event")))))

                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TriggerCommand::suggestTriggerIds)
                                .executes(TriggerCommand::info)))

                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> create(ctx, defaultEvent()))
                                .then(Commands.argument("event", StringArgumentType.word())
                                        .suggests(TriggerCommand::suggestEvents)
                                        .executes(ctx -> create(ctx, StringArgumentType.getString(ctx, "event"))))))

                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TriggerCommand::suggestTriggerIds)
                                .executes(TriggerCommand::delete)))

                .then(Commands.literal("enable")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TriggerCommand::suggestTriggerIds)
                                .executes(ctx -> setEnabled(ctx, true))))

                .then(Commands.literal("disable")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TriggerCommand::suggestTriggerIds)
                                .executes(ctx -> setEnabled(ctx, false))))

                .then(Commands.literal("test")
                        .then(Commands.argument("triggerId", StringArgumentType.word())
                                .suggests(TriggerCommand::suggestTriggerIds)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(TriggerCommand::test))))

                .then(Commands.literal("debug")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(TriggerCommand::debug)));
    }

    // ----------------------------------------------------------------
    // reload
    // ----------------------------------------------------------------
    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        TriggerManager manager = StoryEngineMod.TRIGGER_MANAGER;
        manager.reload();
        CommandFeedback.success(source, "Триггеры перезагружены: " + manager.size() + " активных, "
                + manager.getLastErrors().size() + " ошибок. Директория: "
                + manager.getTriggersDirectory().toAbsolutePath());
        manager.getLastErrors().forEach(e -> CommandFeedback.fail(source, "  " + e));
        return 0;
    }

    // ----------------------------------------------------------------
    // create <id> [event]
    // ----------------------------------------------------------------
    private static int create(CommandContext<CommandSourceStack> ctx, String eventName) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        TriggerManager manager = StoryEngineMod.TRIGGER_MANAGER;

        TriggerEvent event = parseEvent(eventName);
        if (event == null) {
            return CommandFeedback.fail(source, "Неизвестное событие '" + eventName
                    + "'. Доступны: " + String.join(", ", suggestEventsList()));
        }
        if (manager.exists(id)) {
            return CommandFeedback.fail(source, "Триггер с id '" + id + "' уже загружен.");
        }
        java.util.Optional<TriggerDefinition> created = manager.createTemplate(id, event);
        if (created.isEmpty()) {
            return CommandFeedback.fail(source, "Файл триггера '"
                    + manager.fileFor(id).toAbsolutePath() + "' уже существует на диске.");
        }
        return CommandFeedback.success(source, "Создан триггер '" + id + "' (событие "
                + event.name() + ") в " + manager.fileFor(id).toAbsolutePath());
    }

    // ----------------------------------------------------------------
    // delete <id>
    // ----------------------------------------------------------------
    private static int delete(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        TriggerManager manager = StoryEngineMod.TRIGGER_MANAGER;
        if (!manager.delete(id)) {
            return CommandFeedback.fail(source, "Триггер '" + id + "' не найден (кэш + файл).");
        }
        return CommandFeedback.success(source, "Триггер '" + id + "' удалён (кэш + файл на диске).");
    }

    // ----------------------------------------------------------------
    // enable <id> / disable <id>
    // ----------------------------------------------------------------
    private static int setEnabled(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        TriggerManager manager = StoryEngineMod.TRIGGER_MANAGER;
        if (!manager.setEnabled(id, enabled)) {
            return CommandFeedback.fail(source, "Триггер '" + id + "' не найден на диске.");
        }
        return CommandFeedback.success(source, "Триггер '" + id + "' "
                + (enabled ? "ВКЛЮЧЁН" : "ОТКЛЮЧЁН") + " (поле enabled перезаписано в файле, кэш обновлён).");
    }

    // ----------------------------------------------------------------
    // list
    // ----------------------------------------------------------------
    private static int list(CommandContext<CommandSourceStack> ctx, String eventFilter) {
        CommandSourceStack source = ctx.getSource();
        TriggerManager manager = StoryEngineMod.TRIGGER_MANAGER;
        Collection<TriggerDefinition> defs = manager.getAllDefinitions();
        long count = defs.size();
        if (eventFilter != null && !eventFilter.isBlank()) {
            TriggerEvent event = parseEvent(eventFilter);
            if (event == null) {
                return CommandFeedback.fail(source, "Неизвестное событие '" + eventFilter
                        + "'. Доступны: " + String.join(", ", suggestEventsList()));
            }
            defs = manager.byEvent(event);
            count = defs.size();
        }
        if (defs.isEmpty()) {
            CommandFeedback.info(source, "Триггеров " + count + " "
                    + (eventFilter != null ? "события " + eventFilter : "всего") + ". Для деталей: /story trigger info <id>.");
            return 0;
        }
        source.sendSuccess(Component.literal("Триггеры (" + count + "):"
                + "\n" + defs.stream()
                .map(def -> "  " + def.id()
                        + " [" + def.event().name() + ']'
                        + " prio=" + def.priority()
                        + (def.targetTag() != null ? " tag=" + def.targetTag() : "")
                        + (def.oneTime() ? " one_time" : "")
                        + (!def.enabled() ? " DISABLED" : ""))
                .collect(java.util.stream.Collectors.joining("\n"))), false);
        return 0;
    }

    // ----------------------------------------------------------------
    // info <id>
    // ----------------------------------------------------------------
    private static int info(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        java.util.Optional<TriggerDefinition> opt = StoryEngineMod.TRIGGER_MANAGER.getTrigger(id);
        if (opt.isEmpty()) {
            return CommandFeedback.fail(source, "Триггер '" + id + "' не найден.");
        }
        TriggerDefinition def = opt.get();
        source.sendSuccess(Component.literal("Триггер '" + id + "':\n  событие="
                + def.event().name()
                + "\n  target_tag=" + (def.targetTag() == null ? "-" : def.targetTag())
                + "\n  priority=" + def.priority()
                + "  one_time=" + def.oneTime()
                + "  enabled=" + def.enabled()
                + "\n  cancel_vanilla=" + def.cancelVanilla()
                + (def.hintLabel() != null && !def.hintLabel().isBlank() ? "  hint=" + def.hintLabel() : "")
                + (def.conditions().isEmpty() ? "" : "\n  условия: " + String.join("; ", def.conditions()))
                + (def.actions().isEmpty() ? "\n  (действия отсутствуют)" : "")
                + (def.actions().isEmpty() ? "" : "\n  действия: " + def.actions().stream()
                        .map(a -> a.type() + " " + a.params())
                        .collect(java.util.stream.Collectors.joining("\n             ")))), false);
        return 0;
    }

    // ----------------------------------------------------------------
    // test <triggerId> <player>
    // ----------------------------------------------------------------
    private static int test(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "triggerId");
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        TriggerManager manager = StoryEngineMod.TRIGGER_MANAGER;
        java.util.Optional<TriggerDefinition> opt = manager.getTrigger(id);
        if (opt.isEmpty()) {
            return CommandFeedback.fail(source, "Триггер '" + id + "' не найден.");
        }
        TriggerDefinition def = opt.get();

        // Non-interaction (state) events: доступность оцениваем по игроку без цели.
        boolean available = TriggerExecutor.isAvailable(
                TriggerContext.builder(
                        player, (net.minecraft.server.level.ServerLevel) player.level, def.event()).build(),
                def);
        return CommandFeedback.success(source,
                "Триггер '" + id + "' у игрока " + player.getName().getString() + ": "
                        + (available ? "ДОСТУПЕН" : "недоступен (условия/one_time/отключён)"));
    }

    // ----------------------------------------------------------------
    // debug <player>
    // ----------------------------------------------------------------
    private static int debug(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        TriggerHintService.TriggerHint hint = TriggerHintService.get().resolveCurrent(
                player, (net.minecraft.server.level.ServerLevel) player.level);
        if (hint == null) {
            CommandFeedback.info(source, "У игрока " + player.getName().getString()
                    + " нет активной серверной подсказки триггера (пусто или цель не под триггером).");
            return 0;
        }
        return CommandFeedback.success(source, "Под прицелом у "
                + player.getName().getString() + " триггер '" + hint.triggerId()
                + "' (tag=" + hint.targetTag() + ", label=" + hint.hintLabel()
                + ", cancel_vanilla=" + hint.cancelVanilla() + ")");
    }

    // ----------------------------------------------------------------
    // Автодополнение
    // ----------------------------------------------------------------
    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestTriggerIds(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                StoryEngineMod.TRIGGER_MANAGER.getAllDefinitions().stream().map(TriggerDefinition::id),
                builder);
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestEvents(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(suggestEventsList(), builder);
    }

    private static java.util.List<String> suggestEventsList() {
        return java.util.stream.Stream.of(TriggerEvent.values())
                .map(Enum::name).collect(java.util.stream.Collectors.toList());
    }

    private static TriggerEvent parseEvent(String raw) {
        for (TriggerEvent event : TriggerEvent.values()) {
            if (event.name().equalsIgnoreCase(raw)) {
                return event;
            }
        }
        return null;
    }

    /** Событие по умолчанию для {@code /story trigger create <id>} (без аргумента event). */
    private static String defaultEvent() {
        return TriggerEvent.INTERACT_ENTITY.name();
    }
}