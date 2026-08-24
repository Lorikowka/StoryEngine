package com.storyengine;

import com.mojang.logging.LogUtils;
import com.storyengine.client.MenuCustomizationConfig;
import com.storyengine.dialogue.DialogueManager;
import com.storyengine.interaction.network.InteractionNetworking;
import com.storyengine.interaction.server.TriggerManager;
import com.storyengine.network.NarrativeNetworking;
import com.storyengine.network.QuestNetworking;
import com.storyengine.network.dialogue.DialogueNetworking;
import com.storyengine.quest.QuestManager;
import com.storyengine.quest.QuestProgressTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Story Engine - сюжетный движок для Minecraft Forge 1.19.2.
 */
@Mod(StoryEngineMod.MOD_ID)
public class StoryEngineMod {

    public static final String MOD_ID = "story_engine";

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Единый экземпляр менеджера квестов на весь мод.
     * Используется командами, сетевым слоем и GUI.
     */
    public static final QuestManager QUEST_MANAGER = new QuestManager();

    /**
     * Единый экземпляр менеджера диалогов (Dialogue System) на весь мод.
     * Ленивая загрузка папок/узлов из config/story_engine/dialogues/.
     */
    public static final DialogueManager DIALOGUE_MANAGER = new DialogueManager();

    /**
     * Единый экземпляр менеджера интерактивных триггеров (Interaction System).
     * Загрузка точек из config/story_engine/triggers/ на старте сервера.
     */
    public static final TriggerManager TRIGGER_MANAGER = new TriggerManager();

    public StoryEngineMod() {
        QuestNetworking.register();
        NarrativeNetworking.register();
        DialogueNetworking.register();
        InteractionNetworking.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, MenuCustomizationConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new QuestProgressTracker());
        LOGGER.info("[StoryEngine] Мод инициализирован, модуль квестов активен.");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[StoryEngine] Загрузка квестов из config/story_engine/quests/ ...");
        QUEST_MANAGER.loadAll();
        LOGGER.info("[StoryEngine] Подготовка директории диалогов config/story_engine/dialogues/ ...");
        DIALOGUE_MANAGER.getDialoguesDirectory();
        LOGGER.info("[StoryEngine] Загрузка интерактивных триггеров из config/story_engine/triggers/ ...");
        TRIGGER_MANAGER.loadAll();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            InteractionNetworking.sendSync(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("[StoryEngine] Остановка сервера, состояние квестов сохранено в файлах.");
    }
}
