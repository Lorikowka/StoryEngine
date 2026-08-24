package com.storyengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;

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

    /** Заливка прямоугольника сплошным/полупрозрачным цветом (ARGB, как в fill()). */
    public static void fillBox(PoseStack poseStack, int x1, int y1, int x2, int y2, int argbColor) {
        fill(poseStack, x1, y1, x2, y2, argbColor);
    }
}
