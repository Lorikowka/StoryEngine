package com.storyengine.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.storyengine.StoryEngineMod;
import com.storyengine.checkpoint.CheckpointRegionSpec;
import com.storyengine.checkpoint.SceneSnapshotManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Подкоманда {@code /story checkpoint} — управление чекпойнтами сцен:
 *
 *   /story checkpoint save <id> [player] [radius] — снять снапшот и сохранить
 *   /story checkpoint restore <id> [player]        — применить снапшот
 *   /story checkpoint list                         — список сохранённых чекпойнтов
 *   /story checkpoint delete <id>                  — удалить чекпойнт
 *
 * Все команды требуют permission level 2.
 */
public final class CheckpointCommand {

    private CheckpointCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build(String literal) {
        return Commands.literal(literal)
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("save")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(CheckpointCommand::save)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(CheckpointCommand::save)
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                                                .executes(CheckpointCommand::save)))))

                .then(Commands.literal("restore")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CheckpointCommand::suggestIds)
                                .executes(CheckpointCommand::restore)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(CheckpointCommand::restore))))

                .then(Commands.literal("list")
                        .executes(CheckpointCommand::list))

                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CheckpointCommand::suggestIds)
                                .executes(CheckpointCommand::delete)));
    }

    // ----------------------------------------------------------------
    // save <id> [player] [radius]
    // ----------------------------------------------------------------
    private static int save(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        ServerPlayer player = resolveOptionalPlayer(ctx, source);
        CheckpointRegionSpec region = CheckpointRegionSpec.forManual(resolveRadius(ctx));

        boolean captured = StoryEngineMod.SNAPSHOT_MANAGER
                .capture(id, (ServerLevel) player.level, player, region).isPresent();
        if (!captured) {
            return CommandFeedback.fail(source, "Не удалось снять чекпойнт '" + id + "'.");
        }
        return CommandFeedback.success(source, "Чекпойнт '" + id + "' сохранён "
                + "(измерение " + player.level.dimension().location() + ", регион вокруг "
                + player.getName().getString() + ", радиус " + region.radius() + ").");
    }

    // ----------------------------------------------------------------
    // restore <id> [player]
    // ----------------------------------------------------------------
    private static int restore(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        ServerPlayer player = resolveOptionalPlayer(ctx, source);

        if (!StoryEngineMod.SNAPSHOT_MANAGER.restore(id, player)) {
            return CommandFeedback.fail(source, "Чекпойнт '" + id + "' не найден.");
        }
        return CommandFeedback.success(source, "Чекпойнт '" + id + "' применён к "
                + player.getName().getString() + ".");
    }

    // ----------------------------------------------------------------
    // list
    // ----------------------------------------------------------------
    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Collection<String> ids = StoryEngineMod.SNAPSHOT_MANAGER.listIds();
        if (ids.isEmpty()) {
            return CommandFeedback.success(source, "Сохранённых чекпойнтов нет.");
        }
        source.sendSuccess(Component.literal("Чекпойнты (" + ids.size() + "):\n  "
                + String.join("\n  ", ids)), false);
        return 0;
    }

    // ----------------------------------------------------------------
    // delete <id>
    // ----------------------------------------------------------------
    private static int delete(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        if (!StoryEngineMod.SNAPSHOT_MANAGER.delete(id)) {
            return CommandFeedback.fail(source, "Чекпойнт '" + id + "' не найден.");
        }
        return CommandFeedback.success(source, "Чекпойнт '" + id + "' удалён.");
    }

    // ----------------------------------------------------------------
    // Опциональные аргументы (более глубокие the-ветки переиспользуют handlers)
    // ----------------------------------------------------------------
    private static ServerPlayer resolveOptionalPlayer(CommandContext<CommandSourceStack> ctx,
                                                      CommandSourceStack source)
            throws CommandSyntaxException {
        try {
            return EntityArgument.getPlayer(ctx, "player");
        } catch (IllegalArgumentException ignored) {
            return source.getPlayerOrException();
        }
    }

    private static int resolveRadius(CommandContext<CommandSourceStack> ctx) {
        try {
            return IntegerArgumentType.getInteger(ctx, "radius");
        } catch (IllegalArgumentException ignored) {
            return 8;
        }
    }

    // ----------------------------------------------------------------
    // Автодополнение
    // ----------------------------------------------------------------
    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestIds(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                StoryEngineMod.SNAPSHOT_MANAGER.listIds(), builder);
    }
}