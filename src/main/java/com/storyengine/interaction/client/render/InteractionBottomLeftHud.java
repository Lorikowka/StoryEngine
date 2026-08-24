package com.storyengine.interaction.client.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.storyengine.StoryEngineMod;
import com.storyengine.client.MenuAssetsManager;
import com.storyengine.client.MenuCustomizationConfig;
import com.storyengine.client.TextureBlitHelper;
import com.storyengine.interaction.client.InteractionClientState;
import com.storyengine.interaction.data.InteractionTrigger;
import com.storyengine.interaction.data.TriggerAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Рендер меню взаимодействия в левом нижнем углу экрана (см. спецификацию §2).
 *
 * Панель либо сплошная заливка (цвета из конфига), либо поверх кастомной
 * текстуры config/story_engine/menu/interaction_menu.png (переключается
 * interactionCustomization.useTexture). Шапка = имя объекта, ниже — пункты
 * действий с индикаторами [>]/[ ]/[x] и подсветкой выбранного (selectedIndex).
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class InteractionBottomLeftHud {

    /** Встроенный размер дефолтной текстуры (см. interaction_menu.png). */
    private static final int TEX_W = 210;
    private static final int TEX_H = 160;

    private InteractionBottomLeftHud() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        if (!InteractionClientState.hasActiveTarget()) {
            return;
        }
        InteractionTrigger trigger = InteractionClientState.getActiveTarget();
        if (trigger == null) {
            return;
        }
        render(event.getPoseStack(), event.getWindow(), trigger, mc.player);
    }

    private static void render(PoseStack pose, Window window, InteractionTrigger trigger, Player player) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int x = MenuCustomizationConfig.interactionPanelX();
        int panelW = MenuCustomizationConfig.interactionPanelWidth();
        int itemH = MenuCustomizationConfig.interactionItemHeight();
        int gap = MenuCustomizationConfig.interactionItemGap();
        int headerH = itemH;

        var actions = trigger.getActions();
        int count = actions.size();
        int itemsH = count > 0 ? count * itemH + (count - 1) * gap : 0;
        int totalH = headerH + itemsH;

        int screenH = window.getGuiScaledHeight();
        int panelTop = screenH - MenuCustomizationConfig.interactionPanelBottomOffset() - totalH;

        boolean useTex = MenuCustomizationConfig.interactionEnabled()
                && MenuCustomizationConfig.interactionUseTexture();
        ResourceLocation tex = MenuAssetsManager.get("interaction_menu");

        // Фон панели (текстура либо заливка).
        if (useTex && tex != null) {
            RenderSystem.setShaderTexture(0, tex);
            TextureBlitHelper.blitStretch(pose, x, panelTop, panelW, totalH, TEX_W, TEX_H);
        } else {
            TextureBlitHelper.fillBox(pose, x, panelTop, x + panelW, panelTop + totalH,
                    MenuCustomizationConfig.interactionPanelFill());
        }

        // Рамка 1px.
        int border = MenuCustomizationConfig.interactionPanelBorder();
        TextureBlitHelper.fillBox(pose, x, panelTop, x + panelW, panelTop + 1, border);
        TextureBlitHelper.fillBox(pose, x, panelTop + totalH - 1, x + panelW, panelTop + totalH, border);
        TextureBlitHelper.fillBox(pose, x, panelTop, x + 1, panelTop + totalH, border);
        TextureBlitHelper.fillBox(pose, x + panelW - 1, panelTop, x + panelW, panelTop + totalH, border);

        // Шапка (имя объекта).
        if (!useTex) {
            TextureBlitHelper.fillBox(pose, x, panelTop, x + panelW, panelTop + headerH,
                    MenuCustomizationConfig.interactionHeaderFill());
        }
        String headerText = "[F] " + (trigger.getName() == null ? "" : trigger.getName());
        font.draw(pose, Component.literal(headerText), x + 6,
                panelTop + (headerH - font.lineHeight) / 2,
                MenuCustomizationConfig.interactionHeaderText());

        // Пункты действий.
        int sel = InteractionClientState.getSelectedIndex();
        for (int i = 0; i < count; i++) {
            int iy = panelTop + headerH + i * (itemH + gap);
            TriggerAction a = actions.get(i);
            boolean available = player != null && a.isAvailable(player);
            boolean active = i == sel;

            int fill;
            int textColor;
            if (!available) {
                fill = MenuCustomizationConfig.interactionItemLockedFill();
                textColor = MenuCustomizationConfig.interactionItemLockedText();
            } else if (active) {
                fill = MenuCustomizationConfig.interactionItemActiveFill();
                textColor = MenuCustomizationConfig.interactionItemActiveText();
            } else {
                fill = MenuCustomizationConfig.interactionItemIdleFill();
                textColor = MenuCustomizationConfig.interactionItemIdleText();
            }

            if (!useTex) {
                TextureBlitHelper.fillBox(pose, x, iy, x + panelW, iy + itemH, fill);
            }
            // Акцентная полоса слева у активного пункта.
            if (active) {
                TextureBlitHelper.fillBox(pose, x, iy, x + 2, iy + itemH,
                        MenuCustomizationConfig.interactionFocus());
            }

            String indicator = !available ? "[x] " : (active ? "[>] " : "[ ] ");
            Component label = Component.literal(indicator).append(a.getLabel() == null ? "" : a.getLabel());
            font.draw(pose, label, x + 6, iy + (itemH - font.lineHeight) / 2, textColor);
        }

        // Подсказка управления.
        String hint = "Колесо — выбор · [F] — действие";
        font.draw(pose, Component.literal(hint), x,
                panelTop + totalH + 2, MenuCustomizationConfig.interactionItemIdleText());
    }
}
