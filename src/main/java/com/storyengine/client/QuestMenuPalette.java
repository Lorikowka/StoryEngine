package com.storyengine.client;

/**
 * Единая палитра цветов журнала квестов (QuestScreen).
 *
 * Все цвета записаны в формате ARGB (0xAARRGGBB): первые два разряда -
 * альфа-канал (FF = полностью непрозрачный), остальные - RGB.
 * Если альфа не указана явно (6 разрядов), она добавляется через
 * QuestScreen.withAlpha либо равна полной непрозрачности.
 *
 * Правило правки: менять цвета ТОЛЬКО здесь - QuestScreen ссылается
 * исключительно на константы этой палитры.
 */
public final class QuestMenuPalette {

    // === Окно ===

    /** Тёмная подложка внутри рамки окна (поверх текстуры quest_menu.png). */
    public static final int WINDOW_FILL = 0xCC202020;

    // === Основной текст ===

    /** Заголовок окна, заголовок квеста, значения полей, описание квеста,
     * текст строк списка, подпись наведённой вкладки. */
    public static final int TEXT_PRIMARY = 0xFFFFFF;

    /** Подписи-метки: "Автор:", "Статус:", заголовки блоков "Цели:"/"Описание:". */
    public static final int TEXT_LABEL = 0xAAAAAA;

    /** Имя автора в строках списка квестов. */
    public static final int TEXT_AUTHOR = 0x909090;

    /** Сообщение пустого состояния ("Нет активных квестов" и т.п.). */
    public static final int TEXT_EMPTY_STATE = 0x888888;

    // === Вкладки ===

    /** Подпись текущей вкладки (тёплый золотистый акцент мода). */
    public static final int TAB_ACTIVE = 0xFFF6D57A;

    /** Подпись вкладки под курсором. */
    public static final int TAB_HOVER = 0xFFFFFF;

    /** Подпись неактивной вкладки. */
    public static final int TAB_IDLE = 0xA0A0A0;

    // === Кнопки (кнопка закрытия окна) ===

    /** Подпись выбранной/нажатой кнопки. */
    public static final int BUTTON_SELECTED = 0xFFF6D57A;

    /** Подпись кнопки под курсором. */
    public static final int BUTTON_HOVER = 0xFFFFFFFF;

    /** Подпись кнопки в покое (чуть темнее чистого белого). */
    public static final int BUTTON_IDLE = 0xFFE0E0E0;

    // === Строки задач в панели деталей ===

    /** Обычный текст задачи. */
    public static final int TASK_NORMAL = 0xDDDDDD;

    /** Выполненная задача (зачёркнутая, зелёная). */
    public static final int TASK_DONE = 0x55FF55;

    /** Задача проваленного квеста (красная). */
    public static final int TASK_FAILED = 0xFF5555;

    /** Развёрнутое описание задачи. */
    public static final int TASK_DESCRIPTION = 0x999999;

    /** Строка с координатами локации задачи. */
    public static final int TASK_LOCATION = 0x93C5FD;

    // === Подложки и скроллбары ===

    /** Полупрозрачная "пилюля" под строкой задачи. */
    public static final int PILL_BACKGROUND = 0x26FFFFFF;

    /** Бледный трек скроллбара. */
    public static final int SCROLLBAR_TRACK = 0x30FFFFFF;

    /** Яркий бегунок скроллбара. */
    public static final int SCROLLBAR_THUMB = 0x90FFFFFF;

    private QuestMenuPalette() {
    }
}
