package com.storyengine.interaction.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.storyengine.StoryEngineMod;
import com.storyengine.client.MenuCustomizationConfig;
import com.storyengine.interaction.client.hint.VanillaActionResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Ввод Interaction Hint System: клавиша действия (по умолчанию F, настраивается)
 * работает как «воздействие» (аналог правого клика) по текущей цели взгляда -
 * любым интерактиблом, на который указывает HUD «[F] Действие».
 *
 * ПКМ по интерактиблу в режимах БЕЗ креатива подавляется: взаимодействовать
 * с дверями/сундуками/кнопками и т.п. можно только через F (и креативом,
 * чтобы не мешать сборке). F-фолбэк идёт через {@code MultiPlayerGameMode}
 * напрямую и это событие не триггерит, поэтому блокировка его не касается.
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
            if (!INTERACT_KEY.isActiveAndMatches(InputConstants.getKey(event.getKey(), event.getScanCode()))) {
                return;
            }
            if (!MenuCustomizationConfig.interactionHintEnabled()) {
                return;
            }
            performVanillaUse(mc);
        }

        /**
         * Полная ванильная логика «use» (аналог правого клика) по текущему
         * hitResult: блок -> useItemOn, сущность -> interactAt, затем interact.
         * Повторяет ветку startUseItem() шаблона Minecraft.java (Forge).
         */
        private static void performVanillaUse(Minecraft mc) {
            LocalPlayer player = mc.player;
            HitResult hit = mc.hitResult;
            if (player == null || hit == null) {
                return;
            }
            InteractionHand hand = InteractionHand.MAIN_HAND;
            switch (hit.getType()) {
                case BLOCK -> {
                    BlockHitResult blockHit = (BlockHitResult) hit;
                    InteractionResult result = mc.gameMode.useItemOn(player, hand, blockHit);
                    if (result.consumesAction() && result.shouldSwing()) {
                        player.swing(hand);
                    }
                }
                case ENTITY -> {
                    EntityHitResult entityHit = (EntityHitResult) hit;
                    Entity entity = entityHit.getEntity();
                    if (!player.canInteractWith(entity, 0)) {
                        return;
                    }
                    InteractionResult result = mc.gameMode.interactAt(player, entity, entityHit, hand);
                    if (!result.consumesAction()) {
                        result = mc.gameMode.interact(player, entity, hand);
                    }
                    if (result.consumesAction() && result.shouldSwing()) {
                        player.swing(hand);
                    }
                }
                default -> {
                }
            }
        }

        /**
         * ПКМ в креативе работает как обычно; в остальных режимах «use» по
         * интерактиблу (цель классифицирована VanillaActionResolver или имеет
         * MenuProvider) подавляется — только через F.
         */
        @SubscribeEvent
        public static void onInteractionKeyTriggered(InputEvent.InteractionKeyMappingTriggered event) {
            if (!event.isUseItem()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.screen != null) {
                return;
            }
            if (!MenuCustomizationConfig.interactionHintEnabled()) {
                return;
            }
            if (mc.player.isCreative()) {
                return;
            }
            HitResult hit = mc.hitResult;
            if (hit == null) {
                return;
            }
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos pos = blockHit.getBlockPos();
                BlockState state = mc.level.getBlockState(pos);
                boolean interactable = VanillaActionResolver.getBlockAction(state.getBlock()) != null
                        || state.getMenuProvider(mc.level, pos) != null;
                if (interactable) {
                    event.setCanceled(true);
                }
            } else if (hit.getType() == HitResult.Type.ENTITY) {
                Entity entity = ((EntityHitResult) hit).getEntity();
                if (VanillaActionResolver.getEntityAction(entity) != null) {
                    event.setCanceled(true);
                }
            }
        }
    }
}
