package com.storyengine.narrative;

import com.storyengine.client.MenuCustomizationConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;

/**
 * Клиентская очередь сообщений сюжетного чата - чтобы сообщения от сервера
 * не накладывались друг на друга, если их отправили сразу несколько подряд.
 *
 * Логика тиков: считает, сколько символов текущего сообщения должно быть
 * видно (эффект печатной машинки), держит сообщение на экране некоторое
 * время после полной отрисовки, затем переходит к следующему в очереди.
 * Сам вызов tick() происходит из TickEvent.ClientTickEvent в NarrativeOverlay.
 *
 * Также хранит ограниченную историю уже показанных сообщений - чтобы игрок,
 * который отвлёкся или пропустил реплику, мог открыть NarrativeLogScreen
 * и прочитать её заново.
 */
public final class NarrativeChatManager {

    private static final Queue<NarrativeMessage> QUEUE = new ArrayDeque<>();

    /** Сколько тиков показывать один символ. 1 = 20 символов/сек. */
    private static final int TICKS_PER_CHAR = 1;
    /** Минимальное время показа после полной отрисовки, в тиках (1.5 сек). */
    private static final int MIN_HOLD_TICKS = 30;

    /** Символов в секунду из клиентского конфига (0 = мгновенно). */
    private static int charsPerSecond() {
        int cps = MenuCustomizationConfig.narrativeHudTextSpeed();
        return cps < 0 ? 0 : cps;
    }
    /** Дополнительное время показа на символ сообщения, в тиках. */
    private static final int HOLD_TICKS_PER_CHAR = 2;
    /** Сколько последних сообщений хранить в истории для NarrativeLogScreen. */
    private static final int MAX_HISTORY = 100;

    private static final Deque<NarrativeMessage> HISTORY = new ArrayDeque<>();

    private static NarrativeMessage current;
    private static int elapsedTicks;
    private static int holdTicksRemaining = -1;

    private NarrativeChatManager() {
    }

    public static void enqueue(NarrativeMessage message) {
        QUEUE.add(message);
    }

    public static Queue<NarrativeMessage> getQueue() {
        return QUEUE;
    }

    /** Сообщение, которое сейчас нужно рисовать на экране, либо null. */
    public static NarrativeMessage getCurrent() {
        return current;
    }

    /** История уже показанных сообщений, от старых к новым, не более MAX_HISTORY штук. */
    public static List<NarrativeMessage> getHistory() {
        return new ArrayList<>(HISTORY);
    }

    public static void tick() {
        if (current == null) {
            current = QUEUE.poll();
            elapsedTicks = 0;
            holdTicksRemaining = -1;
            if (current != null) {
                HISTORY.addLast(current);
                while (HISTORY.size() > MAX_HISTORY) {
                    HISTORY.removeFirst();
                }
            }
            return;
        }

        elapsedTicks++;

        if (!isFullyRevealed()) {
            return;
        }

        if (holdTicksRemaining < 0) {
            int length = plainLength();
            holdTicksRemaining = Math.max(MIN_HOLD_TICKS, length * HOLD_TICKS_PER_CHAR);
        } else if (holdTicksRemaining == 0) {
            current = null;
        } else {
            holdTicksRemaining--;
        }
    }

    public static boolean isFullyRevealed() {
        return current != null && getVisibleCharCount() >= plainLength();
    }

    public static int getVisibleCharCount() {
        if (current == null) {
            return 0;
        }
        int cps = charsPerSecond();
        if (cps <= 0) {
            return plainLength();
        }
        return Math.min(plainLength(), (int) (elapsedTicks * cps / 20.0));
    }

    /** Немедленно завершает анимацию печати текущего сообщения (например, по клику). */
    public static void skipTyping() {
        if (current != null) {
            int cps = charsPerSecond();
            elapsedTicks = cps <= 0 ? 1 : (int) Math.ceil(plainLength() * 20.0 / cps);
        }
    }

    private static int plainLength() {
        return current == null ? 0 : current.getText().getString().length();
    }
}

