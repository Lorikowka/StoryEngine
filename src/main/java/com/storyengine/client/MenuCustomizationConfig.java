package com.storyengine.client;

import com.storyengine.StoryEngineMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge client-конфиг кастомизации меню квестов.
 *
 * Все цвета хранятся в формате ARGB (0xAARRGGBB, 8 hex-цифр), как в
 * {@link QuestMenuPalette}. Дефолтные значения берутся оттуда же, поэтому
 * правка цветов в quest_menu через код больше не нужна - всё правится тут
 * или в игре: Моды → Story Engine → Config (файл config/story_engine-client.toml).
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MenuCustomizationConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec SPEC;

    /** Использовать кастомные текстуры из config/story_engine/menu/ и цвета ниже. */
    public static final ForgeConfigSpec.BooleanValue ENABLED;

    // === Цвета (ARGB) ===
    public static final ForgeConfigSpec.IntValue WINDOW_FILL;
    public static final ForgeConfigSpec.IntValue TEXT_PRIMARY;
    public static final ForgeConfigSpec.IntValue TEXT_LABEL;
    public static final ForgeConfigSpec.IntValue TEXT_AUTHOR;
    public static final ForgeConfigSpec.IntValue TEXT_EMPTY_STATE;
    public static final ForgeConfigSpec.IntValue ACCENT;
    public static final ForgeConfigSpec.IntValue TAB_HOVER;
    public static final ForgeConfigSpec.IntValue TAB_IDLE;
    public static final ForgeConfigSpec.IntValue BUTTON_HOVER;
    public static final ForgeConfigSpec.IntValue BUTTON_IDLE;
    public static final ForgeConfigSpec.IntValue TASK_NORMAL;
    public static final ForgeConfigSpec.IntValue TASK_DONE;
    public static final ForgeConfigSpec.IntValue TASK_FAILED;
    public static final ForgeConfigSpec.IntValue TASK_DESCRIPTION;
    public static final ForgeConfigSpec.IntValue TASK_LOCATION;
    public static final ForgeConfigSpec.IntValue PILL_BACKGROUND;
    public static final ForgeConfigSpec.IntValue SCROLLBAR_TRACK;
    public static final ForgeConfigSpec.IntValue SCROLLBAR_THUMB;

    // === Окно истории сюжетного чата (NarrativeLogScreen) ===
    public static final ForgeConfigSpec.IntValue LOG_HEADER_FILL;
    public static final ForgeConfigSpec.IntValue LOG_FOOTER_FILL;
    public static final ForgeConfigSpec.IntValue LOG_ACCENT_LINE;
    public static final ForgeConfigSpec.IntValue LOG_TITLE_COLOR;
    public static final ForgeConfigSpec.IntValue LOG_HINT_COLOR;
    public static final ForgeConfigSpec.IntValue LOG_FEED_FILL;
    public static final ForgeConfigSpec.IntValue LOG_CHAT_LINE_BG;
    public static final ForgeConfigSpec.IntValue LOG_BODY_COLOR;
    public static final ForgeConfigSpec.IntValue LOG_EMPTY_COLOR;
    public static final ForgeConfigSpec.IntValue LOG_SCROLLBAR_TRACK;
    public static final ForgeConfigSpec.IntValue LOG_SCROLLBAR_THUMB;

    // === Масштаб/шрифт ===
    public static final ForgeConfigSpec.DoubleValue UI_SCALE_OVERRIDE;
    public static final ForgeConfigSpec.DoubleValue FONT_SCALE;

    // === Dialogue System (окно диалогов, v4 — без иконок) ===
    public static final ForgeConfigSpec.BooleanValue DIALOGUE_ENABLED;
    public static final ForgeConfigSpec.IntValue DIALOGUE_BAR_HEIGHT;
    public static final ForgeConfigSpec.IntValue DIALOGUE_TEXT_SPEED;
    public static final ForgeConfigSpec.IntValue DIALOGUE_BAR_FILL;
    public static final ForgeConfigSpec.IntValue DIALOGUE_DIVIDER_COLOR;
    public static final ForgeConfigSpec.IntValue DIALOGUE_SPEAKER_PLATE_FILL;
    public static final ForgeConfigSpec.IntValue DIALOGUE_SPEAKER_PLATE_BORDER;
    public static final ForgeConfigSpec.IntValue DIALOGUE_SPEAKER_ACCENT;
    public static final ForgeConfigSpec.IntValue DIALOGUE_SPEAKER_NAME_COLOR;
    public static final ForgeConfigSpec.IntValue DIALOGUE_TEXT_COLOR;
    public static final ForgeConfigSpec.IntValue DIALOGUE_TEXT_LEFT_INDENT;
    public static final ForgeConfigSpec.IntValue DIALOGUE_TEXT_RIGHT_INDENT;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_BOX_WIDTH;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_H_PADDING;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_BOX_HEIGHT;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_ROW_GAP;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_X;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_Y;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_IDLE_FILL;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_IDLE_BORDER;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_IDLE_TEXT;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_HOVER_FILL;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_HOVER_BORDER;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_HOVER_TEXT;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_DISABLED_FILL;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_DISABLED_BORDER;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_DISABLED_TEXT;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_PRESSED_FILL;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_PRESSED_BORDER;
    public static final ForgeConfigSpec.IntValue DIALOGUE_RESPONSE_PRESSED_TEXT;

    // === Interaction System (меню взаимодействия, левый нижний угол) ===
    public static final ForgeConfigSpec.BooleanValue INTERACTION_ENABLED;

    // === Narrative HUD (сюжетный чат, центр-низ экрана) ===
    public static final ForgeConfigSpec.IntValue NARRATIVE_TEXT_SPEED;
    public static final ForgeConfigSpec.BooleanValue INTERACTION_USE_TEXTURE;
    public static final ForgeConfigSpec.IntValue INTERACTION_PANEL_X;
    public static final ForgeConfigSpec.IntValue INTERACTION_PANEL_BOTTOM_OFFSET;
    public static final ForgeConfigSpec.IntValue INTERACTION_PANEL_WIDTH;
    public static final ForgeConfigSpec.IntValue INTERACTION_ITEM_HEIGHT;
    public static final ForgeConfigSpec.IntValue INTERACTION_ITEM_GAP;
    public static final ForgeConfigSpec.IntValue INTERACTION_PANEL_FILL;
    public static final ForgeConfigSpec.IntValue INTERACTION_PANEL_BORDER;
    public static final ForgeConfigSpec.IntValue INTERACTION_FOCUS;
    public static final ForgeConfigSpec.IntValue INTERACTION_HEADER_FILL;
    public static final ForgeConfigSpec.IntValue INTERACTION_HEADER_TEXT;
    public static final ForgeConfigSpec.IntValue INTERACTION_ITEM_ACTIVE_FILL;
    public static final ForgeConfigSpec.IntValue INTERACTION_ITEM_ACTIVE_TEXT;
    public static final ForgeConfigSpec.IntValue INTERACTION_ITEM_IDLE_FILL;
    public static final ForgeConfigSpec.IntValue INTERACTION_ITEM_IDLE_TEXT;
    public static final ForgeConfigSpec.IntValue INTERACTION_ITEM_LOCKED_FILL;
    public static final ForgeConfigSpec.IntValue INTERACTION_ITEM_LOCKED_TEXT;

    static {
        BUILDER.comment("Настройки кастомизации меню квестов (текстуры, цвета, масштаб).")
                .push("menuCustomization");

        ENABLED = BUILDER.comment(
                        "true - использовать кастомные текстуры из config/story_engine/menu/ и цвета из этого файла.",
                        "false - рисовать меню встроенными текстурами и цветами (игнорировать папку и правки цветов).")
                .define("enabled", true);

        BUILDER.comment("Цвета в формате ARGB (0xAARRGGBB, 8 hex-цифр). Пример акцента: 0xFFF6D57A")
                .push("colors");
        WINDOW_FILL = color("windowFill", QuestMenuPalette.WINDOW_FILL, "Тёмная подложка внутри рамки окна.");
        TEXT_PRIMARY = color("textPrimary", QuestMenuPalette.TEXT_PRIMARY, "Основной текст (заголовки, список, описание).");
        TEXT_LABEL = color("textLabel", QuestMenuPalette.TEXT_LABEL, "Метки-подписи: 'Автор:', 'Статус:', 'Цели:'.");
        TEXT_AUTHOR = color("textAuthor", QuestMenuPalette.TEXT_AUTHOR, "Имя автора в строках списка.");
        TEXT_EMPTY_STATE = color("textEmptyState", QuestMenuPalette.TEXT_EMPTY_STATE, "Сообщение пустого состояния.");
        ACCENT = color("accent", QuestMenuPalette.TAB_ACTIVE, "Акцентный цвет (текущая вкладка, выбранная кнопка).");
        TAB_HOVER = color("tabHover", QuestMenuPalette.TAB_HOVER, "Подпись вкладки под курсором.");
        TAB_IDLE = color("tabIdle", QuestMenuPalette.TAB_IDLE, "Подпись неактивной вкладки.");
        BUTTON_HOVER = color("buttonHover", QuestMenuPalette.BUTTON_HOVER, "Подпись кнопки под курсором.");
        BUTTON_IDLE = color("buttonIdle", QuestMenuPalette.BUTTON_IDLE, "Подпись кнопки в покое.");
        TASK_NORMAL = color("taskNormal", QuestMenuPalette.TASK_NORMAL, "Обычный текст задачи.");
        TASK_DONE = color("taskDone", QuestMenuPalette.TASK_DONE, "Выполненная задача (зелёная).");
        TASK_FAILED = color("taskFailed", QuestMenuPalette.TASK_FAILED, "Задача проваленного квеста (красная).");
        TASK_DESCRIPTION = color("taskDescription", QuestMenuPalette.TASK_DESCRIPTION, "Развёрнутое описание задачи.");
        TASK_LOCATION = color("taskLocation", QuestMenuPalette.TASK_LOCATION, "Строка с координатами локации.");
        PILL_BACKGROUND = color("pillBackground", QuestMenuPalette.PILL_BACKGROUND, "Полупрозрачная 'пилюля' под строкой задачи.");
        SCROLLBAR_TRACK = color("scrollbarTrack", QuestMenuPalette.SCROLLBAR_TRACK, "Трек скроллбара.");
        SCROLLBAR_THUMB = color("scrollbarThumb", QuestMenuPalette.SCROLLBAR_THUMB, "Бегунок скроллбара.");
        BUILDER.pop();

        BUILDER.push("layout");
        UI_SCALE_OVERRIDE = BUILDER.comment(
                        "Фиксированный масштаб меню. 0.0 = авто-подгонка под размер окна (как раньше).",
                        "Допустимо 0.0 .. 1.5.")
                .defineInRange("uiScaleOverride", 0.0, 0.0, 1.5);
        FONT_SCALE = BUILDER.comment(
                        "Множитель масштаба заголовка квеста в панели деталей.",
                        "1.0 = базовый, допустимо 0.5 .. 2.0.")
                .defineInRange("fontScale", 1.0, 0.5, 2.0);
        BUILDER.pop();

        BUILDER.comment("Настройки кастомизации окна диалогов (Dialogue System, v4 — без иконок).")
                .push("dialogueCustomization");

        DIALOGUE_ENABLED = BUILDER.comment(
                        "true - использовать кастомные цвета/размеры окна диалогов из этого раздела.",
                        "false - рисовать окно диалогов встроенными значениями по умолчанию.")
                .define("enabled", true);

        DIALOGUE_BAR_HEIGHT = BUILDER.comment("Высота нижней панели реплики в пикселях.")
                .defineInRange("barHeight", 68, 32, 400);

        DIALOGUE_TEXT_SPEED = BUILDER.comment(
                        "Скорость печатной машинки: символов в секунду.",
                        "0 = мгновенно, 25 ~ эталонная скорость из спецификации v4.")
                .defineInRange("charsPerSecond", 25, 0, 200);

        BUILDER.comment("Цвета в формате ARGB (0xAARRGGBB, 8 hex-цифр).").push("colors");
        DIALOGUE_BAR_FILL = color("barFill", 0xEA0E1117, "Фон нижней панели реплики.");
        DIALOGUE_DIVIDER_COLOR = color("divider", 0x604A5568, "Верхняя разделительная линия панели (1px).");
        DIALOGUE_SPEAKER_PLATE_FILL = color("speakerPlateFill", 0xEA0E1117, "Фон плашки имени спикера.");
        DIALOGUE_SPEAKER_PLATE_BORDER = color("speakerPlateBorder", 0x604A5568, "Рамка плашки имени спикера.");
        DIALOGUE_SPEAKER_ACCENT = color("speakerAccent", 0xFF38BDF8, "Верхняя акцентная полоса плашки имени.");
        DIALOGUE_SPEAKER_NAME_COLOR = color("speakerName", 0xFFE066, "Имя спикера (золотой).");
        DIALOGUE_TEXT_COLOR = color("text", 0xFFE8E8E8, "Текст реплики NPC.");
        DIALOGUE_TEXT_LEFT_INDENT = BUILDER.comment(
                        "Отступ текста реплики слева в пикселях (от внутренней границы панели).",
                        "Комфортное значение 12-16 пикселей.")
                .defineInRange("textLeftIndent", 14, 0, 200);
        DIALOGUE_TEXT_RIGHT_INDENT = BUILDER.comment("Отступ текста реплики справа в пикселях.")
                .defineInRange("textRightIndent", 32, 0, 200);
        BUILDER.pop();

        BUILDER.comment("Варианты ответа (прижаты к верхнему левому углу).").push("responses");
        DIALOGUE_RESPONSE_BOX_WIDTH = BUILDER.comment(
                        "Минимальная ширина (min-width) кнопки варианта ответа в пикселях.",
                        "Порог, чтобы короткие ответы ('Да', 'Нет') не превращались в обрубки;",
                        "при длинном тексте кнопка автоматически расширяется под самый длинный ответ.")
                .defineInRange("boxWidth", 120, 40, 2000);
        DIALOGUE_RESPONSE_H_PADDING = BUILDER.comment(
                        "Горизонтальный паддинг текста внутри кнопки в пикселях.",
                        "Ширина кнопки = ширина текста + horizontalPadding * 2.")
                .defineInRange("horizontalPadding", 16, 4, 80);
        DIALOGUE_RESPONSE_BOX_HEIGHT = BUILDER.comment("Высота кнопки варианта ответа в пикселях.")
                .defineInRange("boxHeight", 28, 12, 200);
        DIALOGUE_RESPONSE_ROW_GAP = BUILDER.comment("Расстояние между кнопками вариантов по вертикали.")
                .defineInRange("rowGap", 6, 4, 100);
        DIALOGUE_RESPONSE_X = BUILDER.comment("Отступ блока вариантов от левого края экрана.")
                .defineInRange("posX", 8, 0, 2000);
        DIALOGUE_RESPONSE_Y = BUILDER.comment("Отступ первой кнопки варианта от верхнего края экрана.")
                .defineInRange("posY", 8, 0, 2000);

        BUILDER.comment("Состояние покоя.").push("idle");
        DIALOGUE_RESPONSE_IDLE_FILL = color("fill", 0x8010141D, "Фон доступного варианта в покое.");
        DIALOGUE_RESPONSE_IDLE_BORDER = color("border", 0x604A5568, "Рамка доступного варианта в покое.");
        DIALOGUE_RESPONSE_IDLE_TEXT = color("text", 0xCCCCCC, "Текст доступного варианта в покое.");
        BUILDER.pop();

        BUILDER.comment("Состояние под курсором.").push("hover");
        DIALOGUE_RESPONSE_HOVER_FILL = color("fill", 0xD81E293B, "Фон варианта под курсором.");
        DIALOGUE_RESPONSE_HOVER_BORDER = color("border", 0xFF38BDF8, "Рамка варианта под курсором.");
        DIALOGUE_RESPONSE_HOVER_TEXT = color("text", 0xFFFFFF, "Текст варианта под курсором.");
        BUILDER.pop();

        BUILDER.comment("Заблокированное состояние (условие if не выполнено).").push("disabled");
        DIALOGUE_RESPONSE_DISABLED_FILL = color("fill", 0x40000000, "Фон заблокированного варианта.");
        DIALOGUE_RESPONSE_DISABLED_BORDER = color("border", 0x30FFFFFF, "Рамка заблокированного варианта.");
        DIALOGUE_RESPONSE_DISABLED_TEXT = color("text", 0x777777, "Текст заблокированного варианта (серый).");
        BUILDER.pop();

        BUILDER.comment("Нажатое состояние (кнопка мыши удерживается на варианте).").push("pressed");
        DIALOGUE_RESPONSE_PRESSED_FILL = color("fill", 0xFF10273F, "Фон нажатого варианта (тёмный синий).");
        DIALOGUE_RESPONSE_PRESSED_BORDER = color("border", 0xFF9BE1FF, "Рамка нажатого варианта (светлый голубой).");
        DIALOGUE_RESPONSE_PRESSED_TEXT = color("text", 0xFFFFFF, "Текст нажатого варианта.");
        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("Настройки окна истории сюжетного чата (NarrativeLogScreen): шапка/подвал, лента, скроллбар.").push("narrativeLogCustomization");
        LOG_HEADER_FILL = color("headerFill", 0xFF27406B, "Заливка синей шапки сверху (ARGB).");
        LOG_FOOTER_FILL = color("footerFill", 0xFF27406B, "Заливка синего подвала снизу (ARGB).");
        LOG_ACCENT_LINE = color("accentLine", 0xFF4F7BC4, "Тонкая акцентная линия под шапкой и над подвалом.");
        LOG_TITLE_COLOR = color("titleColor", 0xFFFFFFFF, "Цвет заголовка 'Сюжетный чат'.");
        LOG_HINT_COLOR = color("hintColor", 0xBFD4EE, "Цвет подсказки прокрутки в подвале.");
        LOG_FEED_FILL = color("feedFill", 0xCC0E1218, "Фон ленты сообщений.");
        LOG_CHAT_LINE_BG = color("chatLineBackground", 0x80000000, "Подложка за строкой сообщения (как в обычном чате).");
        LOG_BODY_COLOR = color("bodyColor", 0xFFFFFFFF, "Цвет текста сообщения.");
        LOG_EMPTY_COLOR = color("emptyColor", 0x88A6C8, "Цвет сообщения пустого состояния.");
        LOG_SCROLLBAR_TRACK = color("scrollbarTrack", 0x30FFFFFF, "Трек скроллбара.");
        LOG_SCROLLBAR_THUMB = color("scrollbarThumb", 0x90FFFFFF, "Бегунок скроллбара.");
        BUILDER.pop();

        BUILDER.comment("Настройки меню интерактивного взаимодействия (Interaction System, левый нижний угол).")
                .push("interactionCustomization");

        INTERACTION_ENABLED = BUILDER.comment(
                        "true - использовать кастомные цвета/геометрию/текстуру меню взаимодействия из этого раздела.",
                        "false - рисовать меню взаимодействия встроенными значениями по умолчанию.")
                .define("enabled", true);

        INTERACTION_USE_TEXTURE = BUILDER.comment(
                        "true - рисовать панель поверх кастомной текстуры config/story_engine/menu/interaction_menu.png.",
                        "false - рисовать панель сплошной заливкой (цвета ниже).")
                .define("useTexture", true);

        BUILDER.comment("Геометрия панели в пикселях (см. спецификацию Interaction System §2).").push("layout");
        INTERACTION_PANEL_X = BUILDER.comment("Отступ панели от левого края экрана.")
                .defineInRange("panelX", 18, 0, 2000);
        INTERACTION_PANEL_BOTTOM_OFFSET = BUILDER.comment("Отступ панели от нижнего края (чтобы не залезать в самый угол).")
                .defineInRange("panelBottomOffset", 44, 0, 2000);
        INTERACTION_PANEL_WIDTH = BUILDER.comment(
                        "Минимальная ширина панели. 0 = авто: ширина плотно подстраивается под",
                        "самую длинную строку + паддинг 8px слева и справа.")
                .defineInRange("panelWidth", 0, 0, 2000);
        INTERACTION_ITEM_HEIGHT = BUILDER.comment("Высота одного пункта списка.")
                .defineInRange("itemHeight", 18, 8, 200);
        INTERACTION_ITEM_GAP = BUILDER.comment("Расстояние между пунктами списка.")
                .defineInRange("itemGap", 4, 0, 100);
        BUILDER.pop();

        BUILDER.comment("Цвета в формате ARGB (0xAARRGGBB, 8 hex-цифр). Палитра изумрудно-зелёная.").push("colors");
        INTERACTION_PANEL_FILL = color("panelFill", 0xEA06140D, "Фон панели (глубокий тёмно-изумрудный графит).");
        INTERACTION_PANEL_BORDER = color("panelBorder", 0x8010B981, "Основная рамка панели (полупрозрачный изумруд).");
        INTERACTION_FOCUS = color("focus", 0xFF22C55E, "Акцентная подсветка фокуса/наведения (яркий неоново-зелёный).");
        INTERACTION_HEADER_FILL = color("headerFill", 0xEA0A2A18, "Фон шапки панели (имя объекта).");
        INTERACTION_HEADER_TEXT = color("headerText", 0xA7F3D0, "Текст шапки (светло-мятный).");
        INTERACTION_ITEM_ACTIVE_FILL = color("itemActiveFill", 0xD00F3D24, "Фон активного пункта (насыщенный зелёный).");
        INTERACTION_ITEM_ACTIVE_TEXT = color("itemActiveText", 0xFFFFFF55, "Текст активного пункта (ярко-жёлтый #FFFF55).");
        INTERACTION_ITEM_IDLE_FILL = color("itemIdleFill", 0x70092315, "Фон неактивного пункта (приглушённый болотный).");
        INTERACTION_ITEM_IDLE_TEXT = color("itemIdleText", 0xFF8D989F, "Текст неактивного пункта (приглушённый серый).");
        INTERACTION_ITEM_LOCKED_FILL = color("itemLockedFill", 0x5006140D, "Фон заблокированного пункта.");
        INTERACTION_ITEM_LOCKED_TEXT = color("itemLockedText", 0x556B5F, "Текст заблокированного пункта (приглушённый серый).");
        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("Настройки сюжетного чата (Narrative HUD): скорость печатной машинки.").push("narrativeHudCustomization");
        NARRATIVE_TEXT_SPEED = BUILDER.comment(
                        "Скорость печатной машинки сюжетного чата: символов в секунду.",
                        "0 = мгновенно, 20 ~ эталонная скорость.")
                .defineInRange("charsPerSecond", 20, 0, 200);
        BUILDER.pop();

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    /** defineInRange для ARGB-int (диапазон - всё int-пространство, т.к. альфа может быть отрицательной). */
    private static ForgeConfigSpec.IntValue color(String name, int defaultValue, String comment) {
        return BUILDER.comment(comment).defineInRange(name, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static boolean enabled() {
        return ENABLED.get();
    }

    /**
     * Значение кастомизации, пока её раздел включён; иначе встроенный дефолт.
     * Это и есть обещанное флагами "false - рисовать встроенными значениями":
     * раньше enabled=false скрывал только текстуры, но правки цветов всё равно
     * применялись.
     */
    private static int gated(ForgeConfigSpec.IntValue value, boolean enabled) {
        return enabled ? value.get() : value.getDefault();
    }

    public static int windowFill() {
        return gated(WINDOW_FILL, enabled());
    }

    public static int textPrimary() {
        return gated(TEXT_PRIMARY, enabled());
    }

    public static int textLabel() {
        return gated(TEXT_LABEL, enabled());
    }

    public static int textAuthor() {
        return gated(TEXT_AUTHOR, enabled());
    }

    public static int textEmptyState() {
        return gated(TEXT_EMPTY_STATE, enabled());
    }

    public static int accent() {
        return gated(ACCENT, enabled());
    }

    public static int tabHover() {
        return gated(TAB_HOVER, enabled());
    }

    public static int tabIdle() {
        return gated(TAB_IDLE, enabled());
    }

    public static int buttonHover() {
        return gated(BUTTON_HOVER, enabled());
    }

    public static int buttonIdle() {
        return gated(BUTTON_IDLE, enabled());
    }

    public static int taskNormal() {
        return gated(TASK_NORMAL, enabled());
    }

    public static int taskDone() {
        return gated(TASK_DONE, enabled());
    }

    public static int taskFailed() {
        return gated(TASK_FAILED, enabled());
    }

    public static int taskDescription() {
        return gated(TASK_DESCRIPTION, enabled());
    }

    public static int taskLocation() {
        return gated(TASK_LOCATION, enabled());
    }

    public static int pillBackground() {
        return gated(PILL_BACKGROUND, enabled());
    }

    public static int scrollbarTrack() {
        return gated(SCROLLBAR_TRACK, enabled());
    }

    public static int scrollbarThumb() {
        return gated(SCROLLBAR_THUMB, enabled());
    }

    public static double uiScaleOverride() {
        return UI_SCALE_OVERRIDE.get();
    }

    public static double fontScale() {
        return FONT_SCALE.get();
    }

    public static boolean dialogueEnabled() {
        return DIALOGUE_ENABLED.get();
    }

    public static int dialogueBarHeight() {
        return gated(DIALOGUE_BAR_HEIGHT, dialogueEnabled());
    }

    /** Символов в секунду (0 = мгновенно). */
    public static int dialogueTextSpeed() {
        return gated(DIALOGUE_TEXT_SPEED, dialogueEnabled());
    }

    /** Символов в секунду для сюжетного чата (Narrative HUD). 0 = мгновенно. */
    public static int narrativeHudTextSpeed() {
        return NARRATIVE_TEXT_SPEED.get();
    }

    public static int dialogueBarFill() {
        return gated(DIALOGUE_BAR_FILL, dialogueEnabled());
    }

    public static int dialogueDividerColor() {
        return gated(DIALOGUE_DIVIDER_COLOR, dialogueEnabled());
    }

    public static int dialogueSpeakerPlateFill() {
        return gated(DIALOGUE_SPEAKER_PLATE_FILL, dialogueEnabled());
    }

    public static int dialogueSpeakerPlateBorder() {
        return gated(DIALOGUE_SPEAKER_PLATE_BORDER, dialogueEnabled());
    }

    public static int dialogueSpeakerAccent() {
        return gated(DIALOGUE_SPEAKER_ACCENT, dialogueEnabled());
    }

    public static int dialogueSpeakerNameColor() {
        return gated(DIALOGUE_SPEAKER_NAME_COLOR, dialogueEnabled());
    }

    public static int dialogueTextColor() {
        return gated(DIALOGUE_TEXT_COLOR, dialogueEnabled());
    }

    public static int dialogueTextLeftIndent() {
        return gated(DIALOGUE_TEXT_LEFT_INDENT, dialogueEnabled());
    }

    public static int dialogueTextRightIndent() {
        return gated(DIALOGUE_TEXT_RIGHT_INDENT, dialogueEnabled());
    }

    public static int dialogueResponseBoxWidth() {
        return gated(DIALOGUE_RESPONSE_BOX_WIDTH, dialogueEnabled());
    }

    public static int dialogueResponseHorizontalPadding() {
        return gated(DIALOGUE_RESPONSE_H_PADDING, dialogueEnabled());
    }

    public static int dialogueResponseBoxHeight() {
        return gated(DIALOGUE_RESPONSE_BOX_HEIGHT, dialogueEnabled());
    }

    public static int dialogueResponseRowGap() {
        return gated(DIALOGUE_RESPONSE_ROW_GAP, dialogueEnabled());
    }

    public static int dialogueResponseX() {
        return gated(DIALOGUE_RESPONSE_X, dialogueEnabled());
    }

    public static int dialogueResponseY() {
        return gated(DIALOGUE_RESPONSE_Y, dialogueEnabled());
    }

    public static int dialogueResponseIdleFill() {
        return gated(DIALOGUE_RESPONSE_IDLE_FILL, dialogueEnabled());
    }

    public static int dialogueResponseIdleBorder() {
        return gated(DIALOGUE_RESPONSE_IDLE_BORDER, dialogueEnabled());
    }

    public static int dialogueResponseIdleText() {
        return gated(DIALOGUE_RESPONSE_IDLE_TEXT, dialogueEnabled());
    }

    public static int dialogueResponseHoverFill() {
        return gated(DIALOGUE_RESPONSE_HOVER_FILL, dialogueEnabled());
    }

    public static int dialogueResponseHoverBorder() {
        return gated(DIALOGUE_RESPONSE_HOVER_BORDER, dialogueEnabled());
    }

    public static int dialogueResponseHoverText() {
        return gated(DIALOGUE_RESPONSE_HOVER_TEXT, dialogueEnabled());
    }

    public static int dialogueResponseDisabledFill() {
        return gated(DIALOGUE_RESPONSE_DISABLED_FILL, dialogueEnabled());
    }

    public static int dialogueResponseDisabledBorder() {
        return gated(DIALOGUE_RESPONSE_DISABLED_BORDER, dialogueEnabled());
    }

    public static int dialogueResponseDisabledText() {
        return gated(DIALOGUE_RESPONSE_DISABLED_TEXT, dialogueEnabled());
    }

    public static int dialogueResponsePressedFill() {
        return gated(DIALOGUE_RESPONSE_PRESSED_FILL, dialogueEnabled());
    }

    public static int dialogueResponsePressedBorder() {
        return gated(DIALOGUE_RESPONSE_PRESSED_BORDER, dialogueEnabled());
    }

    public static int dialogueResponsePressedText() {
        return gated(DIALOGUE_RESPONSE_PRESSED_TEXT, dialogueEnabled());
    }

    // === Окно истории сюжетного чата (NarrativeLogScreen) ===
    public static int logHeaderFill() {
        return gated(LOG_HEADER_FILL, enabled());
    }

    public static int logFooterFill() {
        return gated(LOG_FOOTER_FILL, enabled());
    }

    public static int logAccentLine() {
        return gated(LOG_ACCENT_LINE, enabled());
    }

    public static int logTitleColor() {
        return gated(LOG_TITLE_COLOR, enabled());
    }

    public static int logHintColor() {
        return gated(LOG_HINT_COLOR, enabled());
    }

    public static int logFeedFill() {
        return gated(LOG_FEED_FILL, enabled());
    }

    public static int logChatLineBackground() {
        return gated(LOG_CHAT_LINE_BG, enabled());
    }

    public static int logBodyColor() {
        return gated(LOG_BODY_COLOR, enabled());
    }

    public static int logEmptyColor() {
        return gated(LOG_EMPTY_COLOR, enabled());
    }

    public static int logScrollbarTrack() {
        return gated(LOG_SCROLLBAR_TRACK, enabled());
    }

    public static int logScrollbarThumb() {
        return gated(LOG_SCROLLBAR_THUMB, enabled());
    }

    // === Interaction System (меню взаимодействия) ===
    public static boolean interactionEnabled() {
        return INTERACTION_ENABLED.get();
    }

    public static boolean interactionUseTexture() {
        return INTERACTION_USE_TEXTURE.get();
    }

    public static int interactionPanelX() {
        return gated(INTERACTION_PANEL_X, interactionEnabled());
    }

    public static int interactionPanelBottomOffset() {
        return gated(INTERACTION_PANEL_BOTTOM_OFFSET, interactionEnabled());
    }

    public static int interactionPanelWidth() {
        return gated(INTERACTION_PANEL_WIDTH, interactionEnabled());
    }

    public static int interactionItemHeight() {
        return gated(INTERACTION_ITEM_HEIGHT, interactionEnabled());
    }

    public static int interactionItemGap() {
        return gated(INTERACTION_ITEM_GAP, interactionEnabled());
    }

    public static int interactionPanelFill() {
        return gated(INTERACTION_PANEL_FILL, interactionEnabled());
    }

    public static int interactionPanelBorder() {
        return gated(INTERACTION_PANEL_BORDER, interactionEnabled());
    }

    public static int interactionFocus() {
        return gated(INTERACTION_FOCUS, interactionEnabled());
    }

    public static int interactionHeaderFill() {
        return gated(INTERACTION_HEADER_FILL, interactionEnabled());
    }

    public static int interactionHeaderText() {
        return gated(INTERACTION_HEADER_TEXT, interactionEnabled());
    }

    public static int interactionItemActiveFill() {
        return gated(INTERACTION_ITEM_ACTIVE_FILL, interactionEnabled());
    }

    public static int interactionItemActiveText() {
        return gated(INTERACTION_ITEM_ACTIVE_TEXT, interactionEnabled());
    }

    public static int interactionItemIdleFill() {
        return gated(INTERACTION_ITEM_IDLE_FILL, interactionEnabled());
    }

    public static int interactionItemIdleText() {
        return gated(INTERACTION_ITEM_IDLE_TEXT, interactionEnabled());
    }

    public static int interactionItemLockedFill() {
        return gated(INTERACTION_ITEM_LOCKED_FILL, interactionEnabled());
    }

    public static int interactionItemLockedText() {
        return gated(INTERACTION_ITEM_LOCKED_TEXT, interactionEnabled());
    }

    private MenuCustomizationConfig() {
    }
}
