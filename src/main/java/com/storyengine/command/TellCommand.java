package com.storyengine.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.storyengine.StoryEngineMod;
import com.storyengine.narrative.NarrativeMessage;
import com.storyengine.narrative.NarrativeStyle;
import com.storyengine.narrative.SpeakerInfo;
import com.storyengine.network.NarrativeNetworking;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

/**
 * Подкоманда {@code /story tell} и алиас {@code /storytell}:
 *
 *   /story tell <targets> <speaker> <icon> [whisper|thought] [color <hex>] <message>
 *   /story tell defaultcolor <hex>
 *
 * Аналог /tellraw, но с именем говорящего и иконкой NPC для Narrative HUD.
 *  - <targets> - игроки-получатели.
 *  - <speaker> - имя говорящего (string: одно слово без кавычек, либо
 *                "Голос за кадром" в кавычках, т.к. содержит пробелы).
 *  - <icon>    - имя файла иконки без .png ("old_man"), "none" - без иконки.
 *  - whisper   - пометить реплику как шёпот; thought - оформить как мысль.
 *  - <hex>     - (опционально) цвет имени спикера в HEX, формат RRGGBB.
 *  - <message> - JSON-компонент текста, как в /tellraw (последний аргумент).
 */
public final class TellCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    private TellCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build(String literal) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
                .requires(source -> source.hasPermission(2));

        // /story tell <targets> <speaker> <icon> <message>
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> targets =
                Commands.argument("targets", EntityArgument.players());

        // без цвета - используется жёлтый по умолчанию
        RequiredArgumentBuilder<CommandSourceStack, Component> message =
                Commands.argument("message", ComponentArgument.textComponent())
                        .executes(ctx -> run(ctx, NarrativeMessage.DEFAULT_NAME_COLOR));

        // /story tell ... color <hex> <message>
        RequiredArgumentBuilder<CommandSourceStack, Component> colorMessage =
                Commands.argument("message", ComponentArgument.textComponent())
                        .executes(TellCommand::runWithColor);
        RequiredArgumentBuilder<CommandSourceStack, String> hex =
                Commands.argument("hex", StringArgumentType.word()).then(colorMessage);
        LiteralArgumentBuilder<CommandSourceStack> colorBranch =
                Commands.literal("color").then(hex);

        RequiredArgumentBuilder<CommandSourceStack, String> icon =
                Commands.argument("icon", StringArgumentType.word())
                        .then(message)
                        .then(colorBranch)
                        .then(styleBranch("whisper", NarrativeStyle.WHISPER))
                        .then(styleBranch("thought", NarrativeStyle.THOUGHT));

        RequiredArgumentBuilder<CommandSourceStack, String> speaker =
                Commands.argument("speaker", StringArgumentType.string()).then(icon);

        targets.then(speaker);
        targets.then(Commands.literal("chorus")
                .then(Commands.argument("speakers", StringArgumentType.string())
                        .then(Commands.argument("message", ComponentArgument.textComponent())
                                .executes(TellCommand::runChorus))));
        root.then(targets);

        // /story tell defaultcolor <hex> - цвет реплик игрока в NarrativeLogScreen
        root.then(Commands.literal("defaultcolor")
                .then(Commands.argument("hex", StringArgumentType.word())
                        .executes(TellCommand::setDefaultColor)));

        return root;
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

    private static LiteralArgumentBuilder<CommandSourceStack> styleBranch(
            String keyword, NarrativeStyle style) {
        return Commands.literal(keyword)
                .then(Commands.argument("message", ComponentArgument.textComponent())
                        .executes(ctx -> run(ctx, NarrativeMessage.DEFAULT_NAME_COLOR, style)))
                .then(Commands.literal("color")
                        .then(Commands.argument("hex", StringArgumentType.word())
                                .then(Commands.argument("message", ComponentArgument.textComponent())
                                        .executes(ctx -> runWithColor(ctx, style)))));
    }

    private static int runWithColor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return runWithColor(ctx, NarrativeStyle.NORMAL);
    }

    private static int runWithColor(CommandContext<CommandSourceStack> ctx, NarrativeStyle style)
            throws CommandSyntaxException {
        String hex = StringArgumentType.getString(ctx, "hex");
        Integer color = parseHexColor(hex);
        if (color == null) {
            return CommandFeedback.fail(ctx.getSource(),
                    "Некорректный HEX-цвет '" + hex + "'. Ожидается формат RRGGBB, например FFAA00 (без пробелов, '#' необязателен).");
        }
        return run(ctx, color, style);
    }

    private static int run(CommandContext<CommandSourceStack> ctx, int nameColor) throws CommandSyntaxException {
        return run(ctx, nameColor, NarrativeStyle.NORMAL);
    }

    private static int run(CommandContext<CommandSourceStack> ctx, int nameColor, NarrativeStyle style)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String speaker = StringArgumentType.getString(ctx, "speaker").trim();
        String icon = StringArgumentType.getString(ctx, "icon");
        Component message = ComponentArgument.getComponent(ctx, "message");

        if (targets.isEmpty()) {
            return CommandFeedback.fail(source, "Не найдено ни одного целевого игрока.");
        }

        NarrativeNetworking.sendToPlayers(targets, speaker, icon, message, nameColor, style);

        return CommandFeedback.success(source,
                "Сообщение от '" + speaker + "' отправлено игрокам: " + targets.size());
    }

    private static int runChorus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    CommandSourceStack source = ctx.getSource();
    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
    Component message = ComponentArgument.getComponent(ctx, "message");
    List<SpeakerInfo> speakers;
    try {
        JsonElement parsed = JsonParser.parseString(StringArgumentType.getString(ctx, "speakers"));
        if (!parsed.isJsonArray()) {
            throw new IllegalArgumentException("speakers must be an array");
        }
        JsonArray array = parsed.getAsJsonArray();
        if (array.isEmpty()) {
            throw new IllegalArgumentException("speakers must not be empty");
        }
        speakers = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("each speaker must be an object");
            }
            JsonObject object = element.getAsJsonObject();
            String name = object.has("name") ? object.get("name").getAsString() : "";
            String icon = object.has("icon") ? object.get("icon").getAsString() : "none";
            Integer color = object.has("name_color")
                    ? parseSpeakerColor(object.get("name_color"))
                    : NarrativeMessage.DEFAULT_NAME_COLOR;
            if (color == null) {
                throw new IllegalArgumentException("invalid name_color");
            }
            speakers.add(new SpeakerInfo(name, icon, color));
        }
    } catch (RuntimeException exception) {
        LOGGER.warn("[StoryEngine] Некорректный speakers JSON в /story tell chorus: {}",
                exception.getMessage());
        return CommandFeedback.fail(source,
                "Некорректный JSON speakers. Ожидается массив объектов name/icon/name_color.");
    }

    if (targets.isEmpty()) {
        return CommandFeedback.fail(source, "Не найдено ни одного целевого игрока.");
    }
    NarrativeNetworking.sendToPlayers(targets, speakers, message);
    return CommandFeedback.success(source, "Хоровая реплика отправлена игрокам: " + targets.size());
}

    /**
     * Цвет имени спикера для «chorus»: число ARGB либо hex-строка ({@code RRGGBB} /
     * {@code #RRGGBB}). Принимает те же форматы, что и действие триггера
     * {@code story_tell}. {@code null}, если значение некорректное.
     */
    private static Integer parseSpeakerColor(JsonElement colorElement) {
        if (colorElement.isJsonPrimitive() && colorElement.getAsJsonPrimitive().isNumber()) {
            return colorElement.getAsInt();
        }
        return parseHexColor(colorElement.getAsString());
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
