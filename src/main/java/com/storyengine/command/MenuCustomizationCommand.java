package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.storyengine.StoryEngineMod;
import com.storyengine.client.MenuAssetsManager;
import com.storyengine.network.QuestNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Подкоманда {@code /story menu} и алиас {@code /storymenu}:
 *
 *   /story menu reset  - перезаписывает PNG в config/story_engine/menu/ исходниками
 *                        из jar и сбрасывает кэш текстур на клиентах.
 *   /story menu reload - только сбрасывает кэш текстур на клиентах (без перезаписи
 *                        файлов), чтобы подхватить ручные правки PNG на диске.
 */
public final class MenuCustomizationCommand {

    private MenuCustomizationCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build(String literal) {
        return Commands.literal(literal)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reset")
                        .executes(MenuCustomizationCommand::reset))
                .then(Commands.literal("reload")
                        .executes(MenuCustomizationCommand::reload));
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MenuAssetsManager.resetDefaults();
        QuestNetworking.sendMenuAssetsReset();
        return CommandFeedback.success(source,
                "Текстуры меню сброшены к исходным (config/story_engine/menu/).");
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MenuAssetsManager.clearCache();
        QuestNetworking.sendMenuAssetsReset();
        return CommandFeedback.success(source,
                "Кэш текстур меню сброшен. Ручные правки PNG в config/story_engine/menu/ подхвачены.");
    }
}
