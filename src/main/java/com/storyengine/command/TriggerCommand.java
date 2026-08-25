package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.storyengine.StoryEngineMod;
import com.storyengine.interaction.data.InteractionTrigger;
import com.storyengine.interaction.network.InteractionNetworking;
import com.storyengine.interaction.server.TriggerManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Подкоманда {@code /story trigger} и алиас {@code /trigger}:
 *
 *   /story trigger create <id> [name]
 *   /story trigger reload
 *   /story trigger list
 *   /story trigger enable  <id>
 *   /story trigger disable <id>
 *
 * Аналог DialogueCommand для интерактивных триггеров. Все команды требуют
 * permission level 2. После create/reload триггеры переотправляются клиентам.
 */
public final class TriggerCommand {

    private TriggerCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build(String literal) {
        return Commands.literal(literal)
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> create(ctx, null))
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> create(ctx, StringArgumentType.getString(ctx, "name"))))))

                .then(Commands.literal("reload")
                        .executes(TriggerCommand::reload))

                .then(Commands.literal("list")
                        .executes(TriggerCommand::list))

                .then(Commands.literal("enable")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TriggerCommand::suggestIds)
                                .executes(ctx -> setEnabled(ctx, true))))

                .then(Commands.literal("disable")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TriggerCommand::suggestIds)
                                .executes(ctx -> setEnabled(ctx, false))));
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestIds(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        for (String id : StoryEngineMod.TRIGGER_MANAGER.listIds()) {
            builder.suggest(id);
        }
        return builder.buildFuture();
    }

    private static int create(CommandContext<CommandSourceStack> ctx, String name) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        if (StoryEngineMod.TRIGGER_MANAGER.triggerExists(id)) {
            return CommandFeedback.fail(source, "Триггер с id '" + id + "' уже существует.");
        }
        StoryEngineMod.TRIGGER_MANAGER.createTemplate(id, name);
        InteractionNetworking.sendSyncToAll();
        return CommandFeedback.success(source,
                "Создан шаблон триггера '" + id + "' в config/story_engine/triggers/");
    }

    private static int setEnabled(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        if (!StoryEngineMod.TRIGGER_MANAGER.triggerExists(id)) {
            return CommandFeedback.fail(source, "Триггер с id '" + id + "' не найден.");
        }
        boolean ok = StoryEngineMod.TRIGGER_MANAGER.setEnabled(id, enabled);
        if (!ok) {
            return CommandFeedback.fail(source, "Не удалось переключить триггер '" + id + "'.");
        }
        InteractionNetworking.sendSyncToAll();
        return CommandFeedback.success(source,
                "Триггер '" + id + "' " + (enabled ? "включён" : "выключен") + ".");
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        StoryEngineMod.TRIGGER_MANAGER.reload();
        InteractionNetworking.sendSyncToAll();
        return CommandFeedback.success(source,
                "Триггеры перезагружены. Загружено: " + StoryEngineMod.TRIGGER_MANAGER.listIds().size());
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var ids = StoryEngineMod.TRIGGER_MANAGER.listIds();
        if (ids.isEmpty()) {
            CommandFeedback.info(source, "Нет загруженных триггеров.");
            return 0;
        }
        CommandFeedback.info(source, "Загруженные триггеры (" + ids.size() + "):");
        for (String id : ids) {
            InteractionTrigger t = StoryEngineMod.TRIGGER_MANAGER.getTriggerById(id);
            String mark = (t != null && !t.isEnabled()) ? " [off]" : "";
            CommandFeedback.info(source, " - " + id + mark);
        }
        return ids.size();
    }
}
