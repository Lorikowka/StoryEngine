package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.storyengine.StoryEngineMod;
import com.storyengine.narrative.NarrativeMessage;
import com.storyengine.network.NarrativeNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Подкоманда {@code /story tell} и алиас {@code /storytell}:
 *
 *   /story tell <targets> <speaker> <icon> [color <hex>] <message>
 *   /story tell defaultcolor <hex>
 *
 * Аналог /tellraw, но с именем говорящего и иконкой NPC для Narrative HUD.
 *  - <targets> - игроки-получатели.
 *  - <speaker> - имя говорящего (string: одно слово без кавычек, либо
 *                "Голос за кадром" в кавычках, т.к. содержит пробелы).
 *  - <icon>    - имя файла иконки без .png ("old_man"), "none" - без иконки.
 *  - <hex>     - (опционально) цвет имени спикера в HEX, формат RRGGBB.
 *  - <message> - JSON-компонент текста, как в /tellraw (последний аргумент).
 */
public final class TellCommand {

    private TellCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build(String literal) {
        return Commands.literal(literal)
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("speaker", StringArgumentType.string())
                                // без цвета - используется жёлтый по умолчанию
                                .then(Commands.argument("icon", StringArgumentType.word())
                                        .then(Commands.argument("message", ComponentArgument.textComponent())
                                                .executes(ctx -> run(ctx, NarrativeMessage.DEFAULT_NAME_COLOR))))
                                // /story tell ... color <hex> <message>
                                .then(Commands.literal("color")
                                        .then(Commands.argument("hex", StringArgumentType.word())
                                                .then(Commands.argument("message", ComponentArgument.textComponent())
                                                        .executes(TellCommand::runWithColor)))))))
                // /story tell defaultcolor <hex> - цвет реплик игрока в NarrativeLogScreen
                .then(Commands.literal("defaultcolor")
                        .then(Commands.argument("hex", StringArgumentType.word())
                                .executes(TellCommand::setDefaultColor))));
    }

    private static int setDefaultColor(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String hex = StringArgumentType.getString(ctx, "hex");
        Integer color = parseHexColor(hex);
        if (color == null) {
            return CommandFeedback.fail(source,
                    "Некорректный HEX-цвет '" + hex + "'. Ожидается формат RRGGBB, например FFAA00.");
        }

        com.storyengine.narrative.NarrativeConfigManager.get().setDefaultPlayerColor(color);
        com.storyengine.narrative.NarrativeConfigManager.save();

        return CommandFeedback.success(source,
                "Цвет реплик игрока по умолчанию (в NarrativeLogScreen) установлен: " + hex);
    }

    private static int runWithColor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String hex = StringArgumentType.getString(ctx, "hex");
        Integer color = parseHexColor(hex);
        if (color == null) {
            return CommandFeedback.fail(ctx.getSource(),
                    "Некорректный HEX-цвет '" + hex + "'. Ожидается формат RRGGBB, например FFAA00 (без пробелов, '#' необязателен).");
        }
        return run(ctx, color);
    }

    private static int run(CommandContext<CommandSourceStack> ctx, int nameColor) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String speaker = StringArgumentType.getString(ctx, "speaker").trim();
        String icon = StringArgumentType.getString(ctx, "icon");
        Component message = ComponentArgument.getComponent(ctx, "message");

        if (targets.isEmpty()) {
            return CommandFeedback.fail(source, "Не найдено ни одного целевого игрока.");
        }

        NarrativeNetworking.sendToPlayers(targets, speaker, icon, message, nameColor);

        return CommandFeedback.success(source,
                "Сообщение от '" + speaker + "' отправлено игрокам: " + targets.size());
    }

    /** Принимает "RRGGBB" или "#RRGGBB". Возвращает null, если формат некорректный. */
    private static Integer parseHexColor(String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        if (clean.length() != 6) {
            return null;
        }
        try {
            return 0xFF000000 | Integer.parseInt(clean, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
