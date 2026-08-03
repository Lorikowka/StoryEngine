package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

/**
 * /storytell <targets> <speaker> <icon> <message>
 * /storytell <targets> <speaker> <icon> color <hex> <message>
 *
 * Аналог /tellraw, но с именем говорящего и иконкой NPC для Narrative HUD.
 *  - <targets> - игроки-получатели.
 *  - <speaker> - имя говорящего, например "Староста" (в кавычках, если с пробелами).
 *  - <icon>    - имя файла иконки без .png (например "old_man"), "none" - без иконки.
 *  - <hex>     - (опционально) цвет имени спикера в HEX, формат RRGGBB, например FFAA00.
 *                Без этой ветки цвет по умолчанию - жёлтый (как раньше).
 *  - <message> - JSON-компонент текста, как в /tellraw.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StoryTellCommand {

    private StoryTellCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("storytell")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("speaker", StringArgumentType.string())
                                .then(Commands.argument("icon", StringArgumentType.word())
                                        // без цвета - используется жёлтый по умолчанию
                                        .then(Commands.argument("message", ComponentArgument.textComponent())
                                                .executes(ctx -> run(ctx, NarrativeMessage.DEFAULT_NAME_COLOR)))
                                        // /storytell ... <icon> color <hex> <message>
                                        .then(Commands.literal("color")
                                                .then(Commands.argument("hex", StringArgumentType.word())
                                                        .then(Commands.argument("message", ComponentArgument.textComponent())
                                                                .executes(StoryTellCommand::runWithColor)))))))
                // /storytell defaultcolor <hex> - цвет реплик игрока в NarrativeLogScreen
                .then(Commands.literal("defaultcolor")
                        .then(Commands.argument("hex", StringArgumentType.word())
                                .executes(StoryTellCommand::setDefaultColor))));
    }

    private static int setDefaultColor(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String hex = StringArgumentType.getString(ctx, "hex");
        Integer color = parseHexColor(hex);
        if (color == null) {
            source.sendFailure(Component.literal(
                    "Некорректный HEX-цвет '" + hex + "'. Ожидается формат RRGGBB, например FFAA00."
            ));
            return 0;
        }

        com.storyengine.narrative.NarrativeConfigManager.get().setDefaultPlayerColor(color);
        com.storyengine.narrative.NarrativeConfigManager.save();

        source.sendSuccess(Component.literal(
                "Цвет реплик игрока по умолчанию (в NarrativeLogScreen) установлен: " + hex
        ), true);
        return 1;
    }

    private static int runWithColor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String hex = StringArgumentType.getString(ctx, "hex");
        Integer color = parseHexColor(hex);
        if (color == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "Некорректный HEX-цвет '" + hex + "'. Ожидается формат RRGGBB, например FFAA00 (без пробелов, '#' необязателен)."
            ));
            return 0;
        }
        return run(ctx, color);
    }

    private static int run(CommandContext<CommandSourceStack> ctx, int nameColor) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String speaker = StringArgumentType.getString(ctx, "speaker");
        String icon = StringArgumentType.getString(ctx, "icon");
        Component message = ComponentArgument.getComponent(ctx, "message");

        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("Не найдено ни одного целевого игрока."));
            return 0;
        }

        NarrativeNetworking.sendToPlayers(targets, speaker, icon, message, nameColor);

        source.sendSuccess(Component.literal(
                "Сообщение от '" + speaker + "' отправлено игрокам: " + targets.size()
        ), true);
        return targets.size();
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
