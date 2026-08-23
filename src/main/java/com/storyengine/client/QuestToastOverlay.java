package com.storyengine.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.storyengine.StoryEngineMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Тост-уведомления (заголовок + текст), которые появляются в правом
 * верхнем углу экрана, задерживаются на пару секунд и плавно исчезают.
 * Несколько уведомлений подряд складываются в стопку.
 *
 * Используется для статусов квестов ("Квест начат/выполнен/провален",
 * "Подзадача выполнена") и для /quest notify.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class QuestToastOverlay {

    private static final int WIDTH = 220;
    private static final int HEIGHT = 36;
    private static final int MARGIN = 8;
    private static final int GAP = 4;

    // Фазы как в KtsQuestMod: плавный въезд, удержание, затухание.
    private static final long SLIDE_IN_MS = 600;
    private static final long HOLD_MS = 1600;
    private static final long FADE_OUT_MS = 600;
    private static final long TOTAL_MS = SLIDE_IN_MS + HOLD_MS + FADE_OUT_MS;

    private static final int BACKGROUND_RGB = 0x101018;
    private static final int ACCENT_RGB = 0xF6D57A;
    private static final int TITLE_RGB = 0xFFFFFF;
    private static final int TEXT_RGB = 0xAAAAAA;

    private static final List<Entry> ACTIVE = new ArrayList<>();

    private QuestToastOverlay() {
    }

    public static void add(String title, String text) {
        ACTIVE.add(new Entry(title == null ? "" : title, text == null ? "" : text));
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        // HOTBAR рендерится ровно один раз за кадр - используем как якорь.
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        if (ACTIVE.isEmpty()) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        render(event.getPoseStack(), event.getWindow());
    }

    private static void render(PoseStack poseStack, Window window) {
        long now = System.currentTimeMillis();
        Font font = Minecraft.getInstance().font;
        int screenWidth = window.getGuiScaledWidth();

        int stackY = MARGIN;
        Iterator<Entry> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            if (entry.startTime < 0) {
                entry.startTime = now;
            }
            long elapsed = now - entry.startTime;
            if (elapsed >= TOTAL_MS) {
                it.remove();
                continue;
            }

            float slide;
            float alpha;
            if (elapsed < SLIDE_IN_MS) {
                float t = easeOutCubic((float) elapsed / SLIDE_IN_MS);
                slide = t;
                alpha = t;
            } else if (elapsed > SLIDE_IN_MS + HOLD_MS) {
                slide = 1.0F;
                alpha = 1.0F - (float) (elapsed - SLIDE_IN_MS - HOLD_MS) / FADE_OUT_MS;
            } else {
                slide = 1.0F;
                alpha = 1.0F;
            }

            // Выезжает справа налево, как ванильные тосты достижений.
            int targetX = screenWidth - MARGIN - WIDTH;
            int startX = screenWidth;
            int curX = startX - Math.round((startX - targetX) * slide);

            drawToast(poseStack, font, entry.title, entry.text, curX, stackY, alpha);
            stackY += HEIGHT + GAP;
        }
    }

    private static void drawToast(PoseStack poseStack, Font font, String title, String text, int x, int y, float alpha) {
        TextureBlitHelper.fillBox(poseStack, x, y, x + WIDTH, y + HEIGHT, withAlpha(BACKGROUND_RGB, alpha * 0.85F));
        TextureBlitHelper.fillBox(poseStack, x, y, x + 3, y + HEIGHT, withAlpha(ACCENT_RGB, alpha));

        int titleColor = withAlpha(TITLE_RGB, alpha);
        int textColor = withAlpha(TEXT_RGB, alpha);
        font.drawShadow(poseStack, title, x + 10, y + 7, titleColor);
        font.drawShadow(poseStack, trimToWidth(font, text, WIDTH - 16), x + 10, y + 19, textColor);
    }

    private static String trimToWidth(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    private static float easeOutCubic(float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        float inv = 1.0F - clamped;
        return 1.0F - inv * inv * inv;
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Math.round(255 * Math.max(0.0F, Math.min(1.0F, alpha)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static final class Entry {
        final String title;
        final String text;
        long startTime = -1;

        Entry(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }
}
