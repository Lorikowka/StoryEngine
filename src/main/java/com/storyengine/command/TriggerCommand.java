package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

/**
 * /trigger create <id> [name]
 * /trigger reload
 * /trigger list
 *
 * Аналог DialogueCommand для интерактивных триггеров. Все команды требуют
 * permission level 2. После create/reload триггеры переотправляются клиентам.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TriggerCommand {

    private TriggerCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trigger")
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
                                .executes(ctx -> setEnabled(ctx, false))))
        );
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
            source.sendFailure(Component.literal("Триггер с id '" + id + "' уже существует."));
            return 0;
        }
        StoryEngineMod.TRIGGER_MANAGER.createTemplate(id, name);
        InteractionNetworking.sendSyncToAll();
        source.sendSuccess(Component.literal(
                "Создан шаблон триггера '" + id + "' в config/story_engine/triggers/"
        ), true);
        return 1;
    }

    private static int setEnabled(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        if (!StoryEngineMod.TRIGGER_MANAGER.triggerExists(id)) {
            source.sendFailure(Component.literal("Триггер с id '" + id + "' не найден."));
            return 0;
        }
        boolean ok = StoryEngineMod.TRIGGER_MANAGER.setEnabled(id, enabled);
        if (!ok) {
            source.sendFailure(Component.literal("Не удалось переключить триггер '" + id + "'."));
            return 0;
        }
        InteractionNetworking.sendSyncToAll();
        source.sendSuccess(Component.literal(
                "Триггер '" + id + "' " + (enabled ? "включён" : "выключен") + "."
        ), true);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        StoryEngineMod.TRIGGER_MANAGER.reload();
        InteractionNetworking.sendSyncToAll();
        source.sendSuccess(Component.literal(
                "Триггеры перезагружены. Загружено: " + StoryEngineMod.TRIGGER_MANAGER.listIds().size()
        ), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var ids = StoryEngineMod.TRIGGER_MANAGER.listIds();
        if (ids.isEmpty()) {
            source.sendSuccess(Component.literal("Нет загруженных триггеров."), false);
            return 0;
        }
        source.sendSuccess(Component.literal("Загруженные триггеры (" + ids.size() + "):"), false);
        for (String id : ids) {
            InteractionTrigger t = StoryEngineMod.TRIGGER_MANAGER.getTriggerById(id);
            String mark = (t != null && !t.isEnabled()) ? " [off]" : "";
            source.sendSuccess(Component.literal(" - " + id + mark), false);
        }
        return ids.size();
    }
}
