package com.storyengine.client;

import com.storyengine.StoryEngineMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Прячет первую руку/предмет в руке, пока открыт DialogueScreen (кинематографичный вид).
 * Хотбар и остальной HUD скрываются самим DialogueScreen через options.hideGui.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DialogueHudHandler {

    private DialogueHudHandler() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (Minecraft.getInstance().screen instanceof DialogueScreen) {
            event.setCanceled(true);
        }
    }
}
