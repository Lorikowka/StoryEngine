package com.storyengine.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.storyengine.StoryEngineMod;
import com.storyengine.player.PlayerDialogueData;
import com.storyengine.player.PlayerQuestDataHelper;
import com.storyengine.trigger.TriggerHintService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /story reset <player>} — полный сброс прогресса StoryEngine для игрока
 * (новое прохождение карты):
 *  - все one_time-флаги триггеров ({@code trigger_done:&lt;id&gt;});
 *  - все флаги диалогов (action {@code set_flag});
 *  - все квесты, подзадачи и прогресс;
 *  - активная диалоговая сессия останавливается.
 *
 * Инвентарь/мир не трогаются — это забота автора карты. Требуется permission level 2.
 */
public final class ResetCommand {

    private ResetCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build(String literal) {
        return Commands.literal(literal)
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ResetCommand::reset));
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

        StoryEngineMod.DIALOGUE_MANAGER.stop(player);

        PlayerDialogueData data = PlayerDialogueData.get(player);
        int flagCount = data.getFlags().size();
        data.clearFlags();

        PlayerQuestDataHelper.clearAll(player);

        ServerLevel level = (ServerLevel) player.level;
        TriggerHintService.get().resync(player, level);

        return CommandFeedback.success(source, "Прогресс StoryEngine у " + player.getName().getString()
                + " сброшен: флагов " + flagCount + " очищено, квесты и one_time-триггеры снова доступны (новое прохождение).");
    }
}