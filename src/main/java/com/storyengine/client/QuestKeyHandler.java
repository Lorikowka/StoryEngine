package com.storyengine.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * Логика открывания меню квестов по клавише (по умолчанию J).
 *
 * Нажатия ловятся через InputEvent.Key - сырое GLFW-событие, которое
 * срабатывает синхронно в момент нажатия клавиши и не зависит от фаз
 * клиентского тика (раньше использовался опрос consumeClick() в конце
 * тика, из-за чего нажатия могли молча теряться). Переназначение клавиши
 * игроком учитывается через KeyMapping.isActiveAndMatches.
 *
 * Поведение:
 * - нет открытого экрана  -> открыть меню квестов;
 * - открыто меню квестов  -> закрыть его той же клавишей;
 * - открыт любой другой экран -> не вмешиваемся (не мешаем вводу текста).
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class QuestKeyHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final KeyMapping QUEST_KEY = new KeyMapping(
            "key.story_engine.open_quest_menu",
            GLFW.GLFW_KEY_J,
            "key.categories.story_engine"
    );

    private QuestKeyHandler() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(QUEST_KEY);
    }

    @Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onKey(InputEvent.Key event) {
            if (event.getAction() != GLFW.GLFW_PRESS) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }

            Screen current = minecraft.screen;
            if (current != null && !(current instanceof QuestScreen)) {
                return;
            }

            if (!QUEST_KEY.isActiveAndMatches(InputConstants.getKey(event.getKey(), event.getScanCode()))) {
                return;
            }

            if (current instanceof QuestScreen) {
                LOGGER.info("[StoryEngine] Меню квестов закрыто клавишей.");
                minecraft.setScreen(null);
            } else {
                LOGGER.info("[StoryEngine] Открываю меню квестов по клавише...");
                minecraft.setScreen(new QuestScreen());
            }
        }
    }
}
