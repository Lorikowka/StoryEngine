package com.storyengine.interaction.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.storyengine.StoryEngineMod;
import com.storyengine.interaction.client.InteractionClientState;
import com.storyengine.interaction.network.InteractionNetworking;
import com.storyengine.network.QuestNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Ввод Interaction System (см. спецификацию §4.3 и выбор пользователя):
 *  - клавиша действия (по умолчанию F, настраивается) -> отправить
 *    C2SExecuteActionPacket(triggerId, selectedIndex);
 *  - колесо мыши при ActiveTarget -> смена selectedIndex и блокировка
 *    переключения хотбара (event.setCanceled(true)).
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class InteractionInputHandler {

    public static final KeyMapping INTERACT_KEY = new KeyMapping(
            "key.story_engine.interact",
            GLFW.GLFW_KEY_F,
            "key.categories.story_engine"
    );

    private InteractionInputHandler() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(INTERACT_KEY);
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
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                return;
            }
            if (!InteractionClientState.hasActiveTarget()) {
                return;
            }
            if (!INTERACT_KEY.isActiveAndMatches(InputConstants.getKey(event.getKey(), event.getScanCode()))) {
                return;
            }
            var trigger = InteractionClientState.getActiveTarget();
            int index = InteractionClientState.getSelectedIndex();
            QuestNetworking.CHANNEL.sendToServer(
                    new InteractionNetworking.C2SExecuteActionPacket(trigger.getId(), index));
        }

        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                return;
            }
            if (!InteractionClientState.hasActiveTarget()) {
                return;
            }
            var trigger = InteractionClientState.getActiveTarget();
            int count = trigger.getActions().size();
            if (count <= 0) {
                return;
            }
            // Прокрутка вверх (delta > 0) -> предыдущий пункт, вниз -> следующий.
            int direction = event.getScrollDelta() > 0 ? -1 : 1;
            InteractionClientState.scrollSelection(direction, count);
            event.setCanceled(true); // блокируем переключение хотбара
        }
    }
}
