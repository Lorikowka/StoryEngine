package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.storyengine.StoryEngineMod;
import com.storyengine.dialogue.DialogueManager;
import com.storyengine.dialogue.DialogueMeta;
import com.storyengine.dialogue.DialogueNode;
import com.storyengine.network.dialogue.DialogueNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Подкоманда {@code /story dialogue} и алиас {@code /dialogue}:
 *
 *   /story dialogue create <id> [title]
 *   /story dialogue reload
 *   /story dialogue list
 *   /story dialogue start <player> <id> [nodeId] [npcSelector]
 *   /story dialogue stop  <player>
 *
 * Все команды требуют permission level 2. id диалогов и node id поддерживают
 * автодополнение по Tab.
 */
public final class DialogueCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    private DialogueCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build(String literal) {
        return Commands.literal(literal)
                .requires(source -> source.hasPermission(2))

                // /story dialogue create <id> [title]
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> create(ctx, null))
                                .then(Commands.argument("title", StringArgumentType.greedyString())
                                        .executes(ctx -> create(ctx, StringArgumentType.getString(ctx, "title"))))))

                // /story dialogue reload
                .then(Commands.literal("reload")
                        .executes(DialogueCommand::reload))

                // /story dialogue list
                .then(Commands.literal("list")
                        .executes(DialogueCommand::list))

                // /story dialogue start <player> <id> [nodeId] [npcSelector]
                .then(Commands.literal("start")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(DialogueCommand::suggestDialogueIds)
                                        .executes(ctx -> start(ctx, null, null))
                                        .then(Commands.argument("nodeId", StringArgumentType.word())
                                                .suggests(DialogueCommand::suggestNodeIds)
                                                .executes(ctx -> start(ctx, StringArgumentType.getString(ctx, "nodeId"), null))
                                                .then(Commands.argument("npcSelector", StringArgumentType.word())
                                                        .executes(ctx -> start(ctx,
                                                                StringArgumentType.getString(ctx, "nodeId"),
                                                                StringArgumentType.getString(ctx, "npcSelector"))))))))

                // /story dialogue stop <player>
                .then(Commands.literal("stop")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(DialogueCommand::stop)));
    }

    // ----------------------------------------------------------------
    // /story dialogue create
    // ----------------------------------------------------------------
    private static int create(CommandContext<CommandSourceStack> ctx, String title) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        if (StoryEngineMod.DIALOGUE_MANAGER.dialogueExists(id)) {
            return CommandFeedback.fail(source, "Диалог с id '" + id + "' уже существует.");
        }
        DialogueMeta meta = StoryEngineMod.DIALOGUE_MANAGER.createTemplate(id, title);
        return CommandFeedback.success(source,
                "Создан шаблон диалога '" + id + "' (" + meta.getTitle() + ") в config/story_engine/dialogues/");
    }

    // ----------------------------------------------------------------
    // /story dialogue reload
    // ----------------------------------------------------------------
    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        StoryEngineMod.DIALOGUE_MANAGER.reload();
        return CommandFeedback.success(source,
                "Диалоги перезагружены. Загружено папок: " + StoryEngineMod.DIALOGUE_MANAGER.listDialogueIds().size());
    }

    // ----------------------------------------------------------------
    // /story dialogue list
    // ----------------------------------------------------------------
    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var ids = StoryEngineMod.DIALOGUE_MANAGER.listDialogueIds();
        if (ids.isEmpty()) {
            CommandFeedback.info(source, "Нет загруженных диалогов.");
            return 0;
        }
        CommandFeedback.info(source, "Загруженные диалоги (" + ids.size() + "):");
        for (String id : ids) {
            CommandFeedback.info(source, " - " + id);
        }
        return ids.size();
    }

    // ----------------------------------------------------------------
    // /story dialogue start
    // ----------------------------------------------------------------
    private static int start(CommandContext<CommandSourceStack> ctx, @Nullable String nodeId, @Nullable String npcSelector) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String id = StringArgumentType.getString(ctx, "id");

        // Резолвим опциональный селектор NPC. Если не передан, не резолвится
        // или сущность отсутствует - стартуем диалог без привязки камеры
        // (по дизайн-документу DIALOGUE_CAMERA.md §4, без падения команды).
        UUID npcId = null;
        if (npcSelector != null && !npcSelector.isBlank()) {
            try {
                EntitySelector selector = new EntitySelectorParser(new com.mojang.brigadier.StringReader(npcSelector)).parse();
                Entity entity = selector.findSingleEntity(source);
                if (entity != null) {
                    npcId = entity.getUUID();
                }
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                LOGGER.warn("[StoryEngine] npcSelector '{}' не резолвится в живую сущность, диалог стартует без камеры: {}",
                        npcSelector, e.getMessage());
            }
        }

        if (!StoryEngineMod.DIALOGUE_MANAGER.dialogueExists(id)) {
            return CommandFeedback.fail(source, "Диалог '" + id + "' не найден.");
        }

        DialogueManager manager = StoryEngineMod.DIALOGUE_MANAGER;
        if (manager.start(player, id, nodeId, npcId) == null) {
            return CommandFeedback.fail(source, "Не удалось начать диалог '" + id + "' (узел не найден).");
        }

        DialogueMeta meta = manager.loadDialogue(id).orElse(null);
        DialogueNode node = manager.loadNode(id, manager.getSession(player).getCurrentNodeId()).orElse(null);
        if (node == null) {
            return CommandFeedback.fail(source, "Стартовый узел диалога '" + id + "' не найден.");
        }

        DialogueNetworking.sendOpen(player, id, node, meta);
        return CommandFeedback.success(source,
                "Диалог '" + id + "' начат у игрока " + player.getName().getString());
    }

    // ----------------------------------------------------------------
    // /story dialogue stop
    // ----------------------------------------------------------------
    private static int stop(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        StoryEngineMod.DIALOGUE_MANAGER.stop(player);
        DialogueNetworking.sendClose(player);
        return CommandFeedback.success(source,
                "Активный диалог прерван у игрока " + player.getName().getString());
    }

    // ----------------------------------------------------------------
    // Автодополнение
    // ----------------------------------------------------------------
    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestDialogueIds(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                StoryEngineMod.DIALOGUE_MANAGER.listDialogueIds().stream(), builder);
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestNodeIds(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String id;
        try {
            id = StringArgumentType.getString(ctx, "id");
        } catch (IllegalArgumentException e) {
            return SharedSuggestionProvider.suggest(java.util.stream.Stream.<String>empty(), builder);
        }
        return SharedSuggestionProvider.suggest(
                StoryEngineMod.DIALOGUE_MANAGER.listNodeIds(id).stream(), builder);
    }
}
