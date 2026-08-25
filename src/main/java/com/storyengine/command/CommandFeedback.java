package com.storyengine.command;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * Единообразное оформление ответов команд модa: золотой префикс
 * {@code [StoryEngine]} + цвет по смыслу (зелёный — успех, серый — инфо,
 * красный — ошибка). Используется всеми командами для «красивых» сообщений.
 */
public final class CommandFeedback {

    private CommandFeedback() {
    }

    private static final String PREFIX = "[StoryEngine] ";

    private static Component prefixed(String msg, ChatFormatting color) {
        return Component.literal(PREFIX).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(msg).withStyle(color));
    }

    /** Успех (возвращает 1 — стандартный «успешный» код команды). */
    public static int success(CommandSourceStack source, String msg) {
        source.sendSuccess(prefixed(msg, ChatFormatting.GREEN), true);
        return 1;
    }

    /** Информационное сообщение без пометки об успехе (broadcast=false). */
    public static void info(CommandSourceStack source, String msg) {
        source.sendSuccess(prefixed(msg, ChatFormatting.GRAY), false);
    }

    /** Ошибка/отказ (возвращает 0). */
    public static int fail(CommandSourceStack source, String msg) {
        source.sendFailure(prefixed(msg, ChatFormatting.RED));
        return 0;
    }
}
