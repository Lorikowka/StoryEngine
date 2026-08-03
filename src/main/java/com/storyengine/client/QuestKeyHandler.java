package com.storyengine.client;

import com.storyengine.StoryEngineMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class QuestKeyHandler {

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

    /**
     * Стандартный и надёжный способ ловить нажатие KeyMapping - через
     * consumeClick() на каждом клиентском тике. В отличие от ручного
     * разбора InputEvent по GLFW-коду, это учитывает переназначение
     * клавиши игроком в настройках управления.
     */
    @Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            while (QUEST_KEY.consumeClick()) {
                if (minecraft.player == null) {
                    continue;
                }
                if (minecraft.screen instanceof QuestScreen) {
                    minecraft.setScreen(null);
                } else if (minecraft.screen == null) {
                    minecraft.setScreen(new QuestScreen());
                }
            }
        }
    }
}
