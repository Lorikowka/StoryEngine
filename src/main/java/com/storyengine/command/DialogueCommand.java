package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * /dialogue create <id> [title]
 * /dialogue reload
 * /dialogue list
 * /dialogue start <player> <id> [nodeId]
 * /dialogue stop  <player>
 *
 * Все команды требуют permission level 2 (как /gamemode).
 * id диалогов и node id поддерживают автодополнение по Tab.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DialogueCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    private DialogueCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dialogue")
                .requires(source -> source.hasPermission(2))

                // /dialogue create <id> [title]
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> create(ctx, null))
                                .then(Commands.argument("title", StringArgumentType.greedyString())
                                        .executes(ctx -> create(ctx, StringArgumentType.getString(ctx, "title"))))))

                // /dialogue reload
                .then(Commands.literal("reload")
                        .executes(DialogueCommand::reload))

                // /dialogue list
                .then(Commands.literal("list")
                        .executes(DialogueCommand::list))

                // /dialogue start <player> <id> [nodeId] [npcSelector]
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

                // /dialogue stop <player>
                .then(Commands.literal("stop")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(DialogueCommand::stop)))
        );
    }

    // ----------------------------------------------------------------
    // /dialogue create
    // ----------------------------------------------------------------
    private static int create(CommandContext<CommandSourceStack> ctx, String title) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        if (StoryEngineMod.DIALOGUE_MANAGER.dialogueExists(id)) {
            source.sendFailure(Component.literal("Диалог с id '" + id + "' уже существует."));
            return 0;
        }
        DialogueMeta meta = StoryEngineMod.DIALOGUE_MANAGER.createTemplate(id, title);
        source.sendSuccess(Component.literal(
                "Создан шаблон диалога '" + id + "' (" + meta.getTitle() + ") в config/story_engine/dialogues/"
        ), true);
        return 1;
    }

    // ----------------------------------------------------------------
    // /dialogue reload
    // ----------------------------------------------------------------
    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        StoryEngineMod.DIALOGUE_MANAGER.reload();
        source.sendSuccess(Component.literal(
                "Диалоги перезагружены. Загружено папок: " + StoryEngineMod.DIALOGUE_MANAGER.listDialogueIds().size()
        ), true);
        return 1;
    }

    // ----------------------------------------------------------------
    // /dialogue list
    // ----------------------------------------------------------------
    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var ids = StoryEngineMod.DIALOGUE_MANAGER.listDialogueIds();
        if (ids.isEmpty()) {
            source.sendSuccess(Component.literal("Нет загруженных диалогов."), false);
            return 0;
        }
        source.sendSuccess(Component.literal("Загруженные диалоги (" + ids.size() + "):"), false);
        for (String id : ids) {
            source.sendSuccess(Component.literal(" - " + id), false);
        }
        return ids.size();
    }

    // ----------------------------------------------------------------
    // /dialogue start
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
            source.sendFailure(Component.literal("Диалог '" + id + "' не найден."));
            return 0;
        }

        DialogueManager manager = StoryEngineMod.DIALOGUE_MANAGER;
        if (manager.start(player, id, nodeId, npcId) == null) {
            source.sendFailure(Component.literal("Не удалось начать диалог '" + id + "' (узел не найден)."));
            return 0;
        }

        DialogueMeta meta = manager.loadDialogue(id).orElse(null);
        DialogueNode node = manager.loadNode(id, manager.getSession(player).getCurrentNodeId()).orElse(null);
        if (node == null) {
            source.sendFailure(Component.literal("Стартовый узел диалога '" + id + "' не найден."));
            return 0;
        }

        DialogueNetworking.sendOpen(player, id, node, meta);
        source.sendSuccess(Component.literal(
                "Диалог '" + id + "' начат у игрока " + player.getName().getString()
        ), true);
        return 1;
    }

    // ----------------------------------------------------------------
    // /dialogue stop
    // ----------------------------------------------------------------
    private static int stop(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        StoryEngineMod.DIALOGUE_MANAGER.stop(player);
        DialogueNetworking.sendClose(player);
        source.sendSuccess(Component.literal(
                "Активный диалог прерван у игрока " + player.getName().getString()
        ), true);
        return 1;
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
