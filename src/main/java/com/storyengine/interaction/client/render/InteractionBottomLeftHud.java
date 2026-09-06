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
 * interactionCustomization.useTexture). Внутри — только список доступных
 * вариантов действий. Подсветка выбранного пункта анимированная: скользит
 * между пунктами с гауссовым затуханием (плавный переход, без резких
 * переключений), текст и заливка плавно интерполируются между idle-серым и
 * активным жёлтым #FFFF55 (см. конфиг). Шапка/заголовок не рисуются.
 * Ширина подстраивается под самую длинную строку (+ паддинг 8px).
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class InteractionBottomLeftHud {

    /** Встроенный размер дефолтной текстуры (см. interaction_menu.png). */
    private static final int TEX_W = 210;
    private static final int TEX_H = 160;

    /** Анимация подсветки выбранного пункта: скользящий центр + время последнего кадра. */
    private static float animCenter = Float.NaN;
    private static boolean animValid;
    private static long lastFrameNanos = -1;

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

        int pad = 8;
        int x = MenuCustomizationConfig.interactionPanelX();
        int itemH = MenuCustomizationConfig.interactionItemHeight();
        int gap = MenuCustomizationConfig.interactionItemGap();

        var actions = trigger.getActions();
        int count = actions.size();
        int totalH = count > 0 ? count * itemH + (count - 1) * gap : 0;

        // Ширина плотно под самую длинную строку действия + паддинг. panelWidth в
        // конфиге задаёт минимальную ширину; 0 = авто (по тексту). Заголовок меню
        // не рисуется: в панели остаётся только список доступных вариантов.
        int maxTextW = 0;
        for (int i = 0; i < count; i++) {
            TriggerAction a = actions.get(i);
            String label = a != null && a.getLabel() != null ? a.getLabel() : "";
            maxTextW = Math.max(maxTextW, font.width(label));
        }
        int configuredW = MenuCustomizationConfig.interactionPanelWidth();
        int panelW = Math.max(configuredW > 0 ? configuredW : 0, maxTextW + pad * 2);

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

        // Пункты действий: подсветка «скользит» от старого к новому выбранному
        // пункту с плавным гауссовым затуханием (без резкого переключения).
        int sel = InteractionClientState.getSelectedIndex();

        long nowNanos = System.nanoTime();
        if (lastFrameNanos < 0) {
            lastFrameNanos = nowNanos;
        }
        float rawDelta = (nowNanos - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = nowNanos;
        float delta = Math.min(Math.max(rawDelta, 0f), 0.05f);

        float target = sel >= 0 ? panelTop + sel * (itemH + gap) + itemH / 2f : Float.NaN;
        if (Float.isNaN(target)) {
            animValid = false;
        } else if (!animValid || rawDelta > 0.25f) {
            // Первый кадр после паузы/возврата/смены триггера — мгновенный сброс.
            animCenter = target;
            animValid = true;
        } else if (delta > 0f) {
            animCenter += (target - animCenter) * (1f - (float) Math.exp(-14.0 * delta));
        }

        for (int i = 0; i < count; i++) {
            int iy = panelTop + i * (itemH + gap);
            TriggerAction a = actions.get(i);
            boolean available = player != null && a.isAvailable(player);

            if (!available) {
                // Заблокированный пункт — статичный приглушённый вид.
                TextureBlitHelper.fillBox(pose, x, iy, x + panelW, iy + itemH,
                        MenuCustomizationConfig.interactionItemLockedFill());
                String lockedLabel = a.getLabel() != null ? a.getLabel() : "";
                font.draw(pose, Component.literal(lockedLabel), x + pad,
                        iy + (itemH - font.lineHeight) / 2,
                        MenuCustomizationConfig.interactionItemLockedText());
                continue;
            }

            // Степень подсветки пункта: гауссово падение от «скользящего» центра.
            float f = animValid ? glowStrength(iy + itemH / 2f, itemH + gap) : 0f;
            // Текстура сама несёт idle-вид строки; в сплошном режиме idle-заливка нужна.
            if (!useTex || f >= 0.02f) {
                int fill = lerpColor(MenuCustomizationConfig.interactionItemIdleFill(),
                        MenuCustomizationConfig.interactionItemActiveFill(), f);
                TextureBlitHelper.fillBox(pose, x, iy, x + panelW, iy + itemH, fill);
            }
            // Акцентная полоса слева двигается вместе с подсветкой, затухая по краям.
            if (f >= 0.02f) {
                int focus = MenuCustomizationConfig.interactionFocus();
                int focusAlpha = Math.round(((focus >>> 24) & 0xFF) * Math.min(1f, f * 1.5f));
                TextureBlitHelper.fillBox(pose, x, iy, x + 2, iy + itemH,
                        (focusAlpha << 24) | (focus & 0x00FFFFFF));
            }

            int textColor = lerpColor(MenuCustomizationConfig.interactionItemIdleText(),
                    MenuCustomizationConfig.interactionItemActiveText(), f);
            String label = a.getLabel() != null ? a.getLabel() : "";
            font.draw(pose, Component.literal(label), x + pad, iy + (itemH - font.lineHeight) / 2, textColor);
        }
    }

    /** Гауссова «сила подсветки» в точке centerY относительно скользящего центра. */
    private static float glowStrength(float centerY, float spread) {
        float d = Math.abs(animCenter - centerY);
        float sigma = Math.max(1f, spread * 0.45f);
        return (float) Math.exp(-(d * d) / (2f * sigma * sigma));
    }

    /** Линейная интерполяция цвета ARGB (включая альфа-канал). */
    private static int lerpColor(int from, int to, float t) {
        if (t <= 0f) {
            return from;
        }
        if (t >= 1f) {
            return to;
        }
        int fa = (from >>> 24) & 0xFF, fr = (from >>> 16) & 0xFF, fg = (from >>> 8) & 0xFF, fb = from & 0xFF;
        int ta = (to >>> 24) & 0xFF, tr = (to >>> 16) & 0xFF, tg = (to >>> 8) & 0xFF, tb = to & 0xFF;
        return (Math.round(fa + (ta - fa) * t) << 24)
                | (Math.round(fr + (tr - fr) * t) << 16)
                | (Math.round(fg + (tg - fg) * t) << 8)
                | Math.round(fb + (tb - fb) * t);
    }
}