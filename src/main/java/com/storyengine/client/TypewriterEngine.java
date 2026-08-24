package com.storyengine.client;

/**
 * Лёгкий класс арифметики печатной машинки для DialogueScreen.
 * Не шарит NarrativeChatManager (тот привязан к очереди фоновых реплик,
 * а DialogueScreen - модальное окно с собственным текущим узлом).
 *
 * Работает с длиной строки: сколько символов должно быть видно на
 * текущем тике. Сбрасывается при смене узла.
 */
public final class TypewriterEngine {

    private static final double TICKS_PER_SECOND = 20.0;

    private int elapsedTicks = 0;
    /** Символов в секунду. 0 = мгновенно. */
    private final int charsPerSecond;

    public TypewriterEngine(int charsPerSecond) {
        this.charsPerSecond = Math.max(0, charsPerSecond);
    }

    /** Сброс к началу анимации (при открытии/смене узла). */
    public void reset() {
        this.elapsedTicks = 0;
    }

    /** Вызывается каждый тик отрисовки. */
    public void tick() {
        elapsedTicks++;
    }

    public boolean isFullyRevealed(int fullLength) {
        return getVisibleCharCount(fullLength) >= fullLength;
    }

    public int getVisibleCharCount(int fullLength) {
        if (charsPerSecond == 0) {
            return fullLength;
        }
        int visible = (int) (elapsedTicks / TICKS_PER_SECOND * charsPerSecond);
        return Math.min(fullLength, visible);
    }

    /** Мгновенно допечатать весь текст (по клику/нажатию). */
    public void skip(int fullLength) {
        if (charsPerSecond == 0) {
            return;
        }
        this.elapsedTicks = (int) (fullLength / (double) charsPerSecond * TICKS_PER_SECOND);
    }
}
