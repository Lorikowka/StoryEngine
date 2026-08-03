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
public final class NarrativeLogKeyHandler {

    public static final KeyMapping LOG_KEY = new KeyMapping(
            "key.story_engine.open_narrative_log",
            GLFW.GLFW_KEY_L,
            "key.categories.story_engine"
    );

    private NarrativeLogKeyHandler() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(LOG_KEY);
    }

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
            while (LOG_KEY.consumeClick()) {
                if (minecraft.player == null) {
                    continue;
                }
                if (minecraft.screen instanceof NarrativeLogScreen) {
                    minecraft.setScreen(null);
                } else if (minecraft.screen == null) {
                    minecraft.setScreen(new NarrativeLogScreen());
                }
            }
        }
    }
}
