package com.storyengine.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * GuiComponent.blit(...) - protected метод, доступный только из подклассов
 * GuiComponent (как Screen). У RenderGuiOverlayEvent-слушателя такого
 * наследования нет, поэтому пробрасываем публичную обёртку через этот
 * маленький подкласс.
 */
public final class TextureBlitHelper extends GuiComponent {

    private TextureBlitHelper() {
    }

    /** Рисует всю текстуру (0,0)-(width,height) как есть, без атласа/UV-сдвигов. */
    public static void blitFull(PoseStack poseStack, int x, int y, int width, int height) {
        blit(poseStack, x, y, 0, 0, width, height, width, height);
    }

    /** Растягивает текстуру размера (texW,texH) в прямоугольник (x,y,width,height). */
    public static void blitStretch(PoseStack poseStack, int x, int y, int width, int height, int texW, int texH) {
        blit(poseStack, x, y, 0, 0, width, height, texW, texH);
    }

    /**
     * Рисует спрайт (u,v)-(u+frameW,v+frameH) текстурной карты как 9-slice (9-patch):
     * углы 1:1, рёбра и центр растягиваются. Позволяет масштабировать кнопки/панели
     * без искажения скруглённых углов и рамок.
     *
     * @param sheetW полная ширина текстурной карты в пикселях (для нормализации UV);
     * @param sheetH полная высота текстурной карты в пикселях;
     * @param border толщина неизменяемой части (утолщение рамки/углов) в пикселях спрайта.
     *               UV нарезаются строго внутри спрайта (u..u+frameW, v..v+frameH).
     */
    public static void blitNineSlice(PoseStack poseStack, ResourceLocation texture,
                                     int x, int y, int width, int height,
                                     int u, int v, int frameW, int frameH,
                                     int border, int sheetW, int sheetH) {
        RenderSystem.setShaderTexture(0, texture);
        int b = Math.max(1, Math.min(border, Math.min(frameW, frameH) / 2 - 1));
        int midW = frameW - 2 * b;
        int midH = frameH - 2 * b;

        // Углы 1:1 (не растягиваются).
        blit(poseStack, x, y, b, b, u, v, b, b, sheetW, sheetH);
        blit(poseStack, x + width - b, y, b, b, u + frameW - b, v, b, b, sheetW, sheetH);
        blit(poseStack, x, y + height - b, b, b, u, v + frameH - b, b, b, sheetW, sheetH);
        blit(poseStack, x + width - b, y + height - b, b, b, u + frameW - b, v + frameH - b, b, b, sheetW, sheetH);

        if (width > 2 * b) {
            // Верхнее и нижнее рёбра (растягиваются по горизонтали).
            blit(poseStack, x + b, y, width - 2 * b, b, u + b, v, midW, b, sheetW, sheetH);
            blit(poseStack, x + b, y + height - b, width - 2 * b, b, u + b, v + frameH - b, midW, b, sheetW, sheetH);
        }
        if (height > 2 * b) {
            // Левое и правое рёбра (растягиваются по вертикали).
            blit(poseStack, x, y + b, b, height - 2 * b, u, v + b, b, midH, sheetW, sheetH);
            blit(poseStack, x + width - b, y + b, b, height - 2 * b, u + frameW - b, v + b, b, midH, sheetW, sheetH);
        }
        if (width > 2 * b && height > 2 * b) {
            // Центр (растягивается в обе стороны).
            blit(poseStack, x + b, y + b, width - 2 * b, height - 2 * b, u + b, v + b, midW, midH, sheetW, sheetH);
        }
    }

    /** Заливка прямоугольника сплошным/полупрозрачным цветом (ARGB, как в fill()). */
    public static void fillBox(PoseStack poseStack, int x1, int y1, int x2, int y2, int argbColor) {
        fill(poseStack, x1, y1, x2, y2, argbColor);
    }
}
