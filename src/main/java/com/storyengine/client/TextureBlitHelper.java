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
 *
 * Все методы ниже работают с РАЗМЕРАМИ ТЕКСТУРЫ, а не с координатами
 * экрана: blit(pose, tex, x, y, w, h, u, v, regW, regH, texW, texH) читает
 * прямоугольник (u,v)-(u+regW,v+regH) пикселей текстуры размера texW x texH
 * и рисует его в прямоугольник (x,y,w,h). Регион всегда обрезается по
 * границам текстуры, поэтому «съехавшая» UV-раскладка не превращается в
 * мусор на экране.
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
     * Рисует один спрайт (u,v)-(u+regW,v+regH) текстуры размера (texW,texH)
     * в прямоугольник (x,y,width,height), растягивая его (или сжимая, если
     * прямоугольник меньше спрайта).
     *
     * Регион жёстко обрезается по границам текстуры: если раскладка атласа
     * «уехала» за пределы карты, берётся только видимая часть, а не
     * произвольный кусок соседних спрайтов. Пустой регион не рисуется.
     */
    public static void blitRegion(PoseStack poseStack, ResourceLocation texture,
                                  int x, int y, int width, int height,
                                  int u, int v, int regW, int regH,
                                  int texW, int texH) {
        if (texture == null || width <= 0 || height <= 0 || texW <= 0 || texH <= 0) {
            return;
        }
        int cu = Math.max(0, Math.min(u, texW));
        int cv = Math.max(0, Math.min(v, texH));
        int cw = Math.min(regW, texW - cu);
        int ch = Math.min(regH, texH - cv);
        if (cw <= 0 || ch <= 0) {
            return;
        }
        RenderSystem.setShaderTexture(0, texture);
        blit(poseStack, x, y, width, height, (float) cu, (float) cv, cw, ch, texW, texH);
    }

    /**
     * Рисует спрайт (u,v)-(u+frameW,v+frameH) текстурной карты как 9-slice (9-patch):
     * углы 1:1, рёбра и центр растягиваются. Позволяет масштабировать кнопки/панели
     * без искажения скруглённых углов и рамок.
     *
     * Используется вместо растяжения всего спрайта на прямоугольник: без него
     * панель шириной 150px из спрайта 80px растягивает рамку в 1.9 раза, а
     * скруглённые углы превращаются в овалы.
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
        if (texture == null || width <= 0 || height <= 0 || sheetW <= 0 || sheetH <= 0) {
            return;
        }
        if (u < 0 || v < 0 || frameW <= 0 || frameH <= 0
                || u + frameW > sheetW || v + frameH > sheetH) {
            // Раскладка вышла за пределы карты - рисуем как есть, но хотя бы без
            // выхода за текстуру (blitRegion обрежет регион по границам).
            blitRegion(poseStack, texture, x, y, width, height, u, v, frameW, frameH, sheetW, sheetH);
            return;
        }
        // Рамка не может занимать больше половины спрайта, иначе центр схлопнется
        // в ноль/минус и uv-quad выродится (инвертированные треугольники).
        int maxBorder = Math.max(0, Math.min(frameW, frameH) / 2 - 1);
        if (maxBorder <= 0) {
            blitRegion(poseStack, texture, x, y, width, height, u, v, frameW, frameH, sheetW, sheetH);
            return;
        }
        int b = Math.max(1, Math.min(border, maxBorder));
        if (width <= 2 * b || height <= 2 * b) {
            // Целиком в углы не влезает - просто растягиваем спрайт.
            blitRegion(poseStack, texture, x, y, width, height, u, v, frameW, frameH, sheetW, sheetH);
            return;
        }

        int midW = frameW - 2 * b;
        int midH = frameH - 2 * b;
        RenderSystem.setShaderTexture(0, texture);

        // Углы 1:1 (не растягиваются).
        blit(poseStack, x, y, b, b, u, v, b, b, sheetW, sheetH);
        blit(poseStack, x + width - b, y, b, b, u + frameW - b, v, b, b, sheetW, sheetH);
        blit(poseStack, x, y + height - b, b, b, u, v + frameH - b, b, b, sheetW, sheetH);
        blit(poseStack, x + width - b, y + height - b, b, b, u + frameW - b, v + frameH - b, b, b, sheetW, sheetH);

        // Верхнее и нижнее рёбра (растягиваются по горизонтали).
        blit(poseStack, x + b, y, width - 2 * b, b, u + b, v, midW, b, sheetW, sheetH);
        blit(poseStack, x + b, y + height - b, width - 2 * b, b, u + b, v + frameH - b, midW, b, sheetW, sheetH);
        // Левое и правое рёбра (растягиваются по вертикали).
        blit(poseStack, x, y + b, b, height - 2 * b, u, v + b, b, midH, sheetW, sheetH);
        blit(poseStack, x + width - b, y + b, b, height - 2 * b, u + frameW - b, v + b, b, midH, sheetW, sheetH);
        // Центр (растягивается в обе стороны).
        blit(poseStack, x + b, y + b, width - 2 * b, height - 2 * b, u + b, v + b, midW, midH, sheetW, sheetH);
    }

    /**
     * Рисует квадрат size x size по центру-обрезке (aspect fill): непропорциональная
     * текстура (например, default_head.png 418x394) не вытягивается в квадрат, а
     * обрезается по длинной оси, как это делает ванильный рендер аватаров.
     */
    public static void blitCover(PoseStack poseStack, ResourceLocation texture, int texW, int texH,
                                 int x, int y, int size) {
        if (texture == null || size <= 0 || texW <= 0 || texH <= 0) {
            return;
        }
        RenderSystem.setShaderTexture(0, texture);
        int crop = Math.min(texW, texH);
        int u = texW > texH ? (texW - crop) / 2 : 0;
        int v = texH > texW ? (texH - crop) / 2 : 0;
        // 11-аргументный blit - единственный, который масштабирует спрайт
        // в произвольный прямоугольник (остальные рисуют 1:1).
        blit(poseStack, x, y, size, size, (float) u, (float) v, crop, crop, texW, texH);
    }

    /** Заливка прямоугольника сплошным/полупрозрачным цветом (ARGB, как в fill()). */
    public static void fillBox(PoseStack poseStack, int x1, int y1, int x2, int y2, int argbColor) {
        fill(poseStack, x1, y1, x2, y2, argbColor);
    }
}
