package com.storyengine.interaction.server;

import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import com.storyengine.dialogue.DialogueActionExecutor;
import com.storyengine.dialogue.DialogueManager;
import com.storyengine.dialogue.DialogueMeta;
import com.storyengine.dialogue.DialogueNode;
import com.storyengine.interaction.data.InteractionTrigger;
import com.storyengine.interaction.data.TriggerAction;
import com.storyengine.network.NarrativeNetworking;
import com.storyengine.network.dialogue.DialogueNetworking;
import com.storyengine.narrative.NarrativeMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.UUID;

/**
 * Серверное исполнение действия триггера (см. спецификацию §6).
 *
 * Порядок (близок к DialogueActionExecutor §5):
 *   1) command
 *   2) dialogue (открыть диалог) / completeTask
 *   3) sound
 *   4) setFlag
 *   5) storytell
 *
 * command/completeTask/setFlag переиспользуют DialogueActionExecutor, чтобы
 * не дублировать логику квестов/флагов. dialogue и sound — специфичны для
 * Interaction System.
 */
public final class TriggerActionExecutor {

    private static final Logger LOGGER = LogUtils.getLogger();

    private TriggerActionExecutor() {
    }

    public static void execute(ServerPlayer player, InteractionTrigger trigger, TriggerAction action) {
        // Блочные действия выполняем через реальное взаимодействие с блоком сервера,
        // чтобы звуки/двойные двери/компараторы работали как при правом клике.
        if (action.getBlockAction() != null && !action.getBlockAction().isBlank()) {
            applyBlockAction(player, trigger, action.getBlockAction());
        }

        if (action.isOpenStorage()) {
            openStorage(player, trigger);
        }

        if (action.getCommand() != null && !action.getCommand().isBlank()) {
            DialogueActionExecutor.runCommand(player, action.getCommand());
        }

        if (action.getDialogue() != null && !action.getDialogue().isBlank()) {
            openDialogue(player, action.getDialogue(), action.getNpc());
        }

        if (action.getCompleteTask() != null && !action.getCompleteTask().isBlank()) {
            DialogueActionExecutor.completeTask(player, action.getCompleteTask());
        }

        if (action.getSound() != null && !action.getSound().isBlank()) {
            playSound(player, action.getSound());
        }

        if (action.getSetFlag() != null && !action.getSetFlag().isBlank()) {
            DialogueActionExecutor.setFlag(player, action.getSetFlag());
        }

        if (action.getStorytell() != null) {
            storytell(player, action.getStorytell());
        }
    }

    /**
     * Изменение состояния OPEN блока (двери/люки/калитки): "open"/"close"/"toggle".
     *
     * Используется тот же путь, что и у ванильного правого клика: BlockState.use()
     * вызывается на сервере с верными координатами блока (BlockPos) и рукой
     * MAIN_HAND, поэтому срабатывают звуки, синхронизация двойных дверей и
     * компараторы. Для направления open/close блок открывается/закрывается только
     * если его текущее состояние не совпадает с целевым.
     */
    private static void applyBlockAction(ServerPlayer player, InteractionTrigger trigger, String action) {
        if (player.level == null || player.level.isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level;
        BlockPos pos = trigger.getBlockPos();
        if (!level.isLoaded(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockStateProperties.OPEN)) {
            LOGGER.debug("[StoryEngine] Блок триггера '{}' не поддерживает open/close (нет свойства OPEN).",
                    trigger.getId());
            return;
        }

        boolean open = state.getValue(BlockStateProperties.OPEN);
        boolean needOpen;
        switch (action.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "open":
                needOpen = true;
                break;
            case "close":
                needOpen = false;
                break;
            case "toggle":
            case "switch":
                needOpen = !open;
                break;
            default:
                LOGGER.warn("[StoryEngine] Неизвестный blockAction '{}' в триггере '{}'.",
                        action, trigger.getId());
                return;
        }

        if (open != needOpen) {
            state.use(level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        }
    }

    /** Открыть контейнер на блоке триггера (сундук/бочка/печь и т.п.) через vanilla MenuProvider. */
    private static void openStorage(ServerPlayer player, InteractionTrigger trigger) {
        if (player.level == null || player.level.isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level;
        for (BlockPos pos : trigger.getBlockPoses()) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            MenuProvider provider = state.getMenuProvider(level, pos);
            if (provider == null && state.hasBlockEntity()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof MenuProvider) {
                    provider = (MenuProvider) be;
                }
            }
            if (provider != null) {
                player.openMenu(provider);
                return;
            }
        }
        LOGGER.debug("[StoryEngine] На блоках триггера '{}' нет контейнера для открытия.", trigger.getId());
    }

    private static void openDialogue(ServerPlayer player, String dialogueId, String npcSelector) {
        UUID npcId = resolveNpcSelector(player, npcSelector);
        DialogueManager manager = StoryEngineMod.DIALOGUE_MANAGER;
        if (manager.start(player, dialogueId, null, npcId) == null) {
            LOGGER.warn("[StoryEngine] Не удалось начать диалог '{}' из триггера.", dialogueId);
            return;
        }
        DialogueMeta meta = manager.loadDialogue(dialogueId).orElse(null);
        DialogueNode node = manager.loadNode(dialogueId, manager.getSession(player).getCurrentNodeId()).orElse(null);
        if (node != null) {
            DialogueNetworking.sendOpen(player, dialogueId, node, meta);
        }
    }

    /** Резолвит опциональный npc-селектор в UUID (null-безопасно, см. DIALOGUE_CAMERA.md §4). */
    @Nullable
    private static UUID resolveNpcSelector(ServerPlayer player, String npcSelector) {
        if (npcSelector == null || npcSelector.isBlank() || player.getServer() == null) {
            return null;
        }
        try {
            EntitySelector selector = new EntitySelectorParser(new com.mojang.brigadier.StringReader(npcSelector)).parse();
            CommandSourceStack source = player.getServer().createCommandSourceStack()
                    .withEntity(player)
                    .withPosition(player.position())
                    .withLevel((ServerLevel) player.level);
            Entity entity = selector.findSingleEntity(source);
            return entity != null ? entity.getUUID() : null;
        } catch (CommandSyntaxException e) {
            LOGGER.warn("[StoryEngine] npcSelector '{}' в триггере не резолвится, диалог без камеры: {}", npcSelector, e.getMessage());
            return null;
        }
    }

    private static void playSound(ServerPlayer player, String soundId) {
        ResourceLocation loc = ResourceLocation.tryParse(soundId);
        if (loc == null) {
            LOGGER.warn("[StoryEngine] Некорректный sound id в триггере: '{}'", soundId);
            return;
        }
        SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(loc);
        if (event == null) {
            LOGGER.warn("[StoryEngine] Неизвестный звук в триггере: '{}'", soundId);
            return;
        }
        player.playSound(event, 1.0f, 1.0f);
    }

    private static void storytell(ServerPlayer player, TriggerAction.Storytell data) {
        JsonObject message = data.getMessage();
        if (message == null) {
            return;
        }
        Component component;
        try {
            component = Component.Serializer.fromJson(message);
        } catch (RuntimeException e) {
            LOGGER.warn("[StoryEngine] Некорректный storytell JSON в триггере: {}", message);
            return;
        }
        if (component == null) {
            return;
        }
        String speaker = data.getSpeaker() != null ? data.getSpeaker() : "";
        NarrativeNetworking.sendToPlayers(Collections.singletonList(player), speaker, "none", component, NarrativeMessage.DEFAULT_NAME_COLOR);
    }
}
