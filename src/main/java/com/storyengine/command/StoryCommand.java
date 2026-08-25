package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.storyengine.StoryEngineMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Единая точка входа для всех команд мода: корень {@code /story}.
 *
 *   /story dialogue ...   (бывш. /dialogue)
 *   /story tell ...       (бывш. /storytell)
 *   /story quest ...      (бывш. /quest)
 *   /story trigger ...    (бывш. /trigger)
 *   /story menu ...       (бывш. /storymenu)
 *   /story help [area]
 *
 * Старые корни оставлены работающими алиасами (см. onRegisterCommands).
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StoryCommand {

    private StoryCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Новый единый корень /story
        dispatcher.register(Commands.literal("story")
                .then(DialogueCommand.build("dialogue"))
                .then(TellCommand.build("tell"))
                .then(QuestCommand.build("quest"))
                .then(TriggerCommand.build("trigger"))
                .then(MenuCustomizationCommand.build("menu"))
                .then(buildHelp()));

        // Обратно совместимые алиасы (старые имена корней)
        dispatcher.register(DialogueCommand.build("dialogue"));
        dispatcher.register(TellCommand.build("storytell"));
        dispatcher.register(QuestCommand.build("quest"));
        dispatcher.register(TriggerCommand.build("trigger"));
        dispatcher.register(MenuCustomizationCommand.build("storymenu"));
    }

    // ============================================================
    // /story help [area]
    // ============================================================
    private static LiteralArgumentBuilder<CommandSourceStack> buildHelp() {
        return Commands.literal("help")
                .executes(StoryCommand::help)
                .then(Commands.argument("area", StringArgumentType.word())
                        .executes(StoryCommand::help));
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String area = null;
        try {
            area = StringArgumentType.getString(ctx, "area");
        } catch (IllegalArgumentException ignored) {
            area = null;
        }

        if (area == null) {
            CommandFeedback.info(source, "StoryEngine — команды (корень /story):");
            CommandFeedback.info(source, "  dialogue — сюжетные диалоги с NPC");
            CommandFeedback.info(source, "  tell     — сюжетные реплики (Narrative HUD)");
            CommandFeedback.info(source, "  quest    — квесты и подзадачи");
            CommandFeedback.info(source, "  trigger  — интерактивные триггеры");
            CommandFeedback.info(source, "  menu     — текстуры меню");
            CommandFeedback.info(source, "Подробнее: /story help <раздел>. Старые имена (/dialogue, /quest, /storytell, /trigger, /storymenu) работают как алиасы.");
            return 0;
        }

        switch (area) {
            case "dialogue":
                CommandFeedback.info(source, "Команды раздела dialogue:");
                CommandFeedback.info(source, "  /story dialogue create <id> [title]");
                CommandFeedback.info(source, "  /story dialogue reload");
                CommandFeedback.info(source, "  /story dialogue list");
                CommandFeedback.info(source, "  /story dialogue start <player> <id> [nodeId] [npcSelector]");
                CommandFeedback.info(source, "  /story dialogue stop <player>");
                break;
            case "tell":
                CommandFeedback.info(source, "Команды раздела tell:");
                CommandFeedback.info(source, "  /story tell <targets> <speaker> <icon> [color <hex>] <message>");
                CommandFeedback.info(source, "    speaker — одно слово без кавычек, либо \"Голос за кадром\" в кавычках");
                CommandFeedback.info(source, "    message — JSON, как в /tellraw: {\"text\":\"...\"}, последний аргумент");
                CommandFeedback.info(source, "  /story tell defaultcolor <hex>");
                break;
            case "quest":
                CommandFeedback.info(source, "Команды раздела quest:");
                CommandFeedback.info(source, "  create <id> [title] | delete <id> | reload | list | notify <title> <text>");
                CommandFeedback.info(source, "  start|complete|fail|reset <player> <id>");
                CommandFeedback.info(source, "  task complete|remove|edit|add ... (см. /story quest в игре по Tab)");
                CommandFeedback.info(source, "  edit <id> title|description <text>");
                break;
            case "trigger":
                CommandFeedback.info(source, "Команды раздела trigger:");
                CommandFeedback.info(source, "  create <id> [name] | reload | list | enable <id> | disable <id>");
                break;
            case "menu":
                CommandFeedback.info(source, "Команды раздела menu:");
                CommandFeedback.info(source, "  reset  — вернуть PNG меню к исходным");
                CommandFeedback.info(source, "  reload — сбросить кэш текстур (подхватить ручные правки)");
                break;
            default:
                return CommandFeedback.fail(source, "Неизвестный раздел '" + area + "'. Доступны: dialogue, tell, quest, trigger, menu.");
        }
        return 0;
    }
}
