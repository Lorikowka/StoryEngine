package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.storyengine.StoryEngineMod;
import com.storyengine.client.MenuAssetsManager;
import com.storyengine.network.QuestNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /storymenu reset  - перезаписывает PNG в config/story_engine/menu/ исходниками
 *                     из jar и сбрасывает кэш текстур на клиентах.
 * /storymenu reload - только сбрасывает кэш текстур на клиентах (без перезаписи
 *                     файлов), чтобы подхватить ручные правки PNG на диске.
 *
 * Примечание: на выделенном сервере config/story_engine/menu/ — это серверная
 * папка, поэтому команда актуальна прежде всего для одиночной игры (где папка
 * конфига общая для сервера и клиента). На клиентах кэш всё равно сбрасывается
 * через сетевой пакет.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MenuCustomizationCommand {

    private MenuCustomizationCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("storymenu")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reset")
                        .executes(MenuCustomizationCommand::reset))
                .then(Commands.literal("reload")
                        .executes(MenuCustomizationCommand::reload)));
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MenuAssetsManager.resetDefaults();
        QuestNetworking.sendMenuAssetsReset();
        source.sendSuccess(Component.literal(
                "Текстуры меню сброшены к исходным (config/story_engine/menu/)."
        ), true);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MenuAssetsManager.clearCache();
        QuestNetworking.sendMenuAssetsReset();
        source.sendSuccess(Component.literal(
                "Кэш текстур меню сброшен. Ручные правки PNG в config/story_engine/menu/ подхвачены."
        ), true);
        return 1;
    }
}
