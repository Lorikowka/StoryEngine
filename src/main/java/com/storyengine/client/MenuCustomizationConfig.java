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

    // === Масштаб/шрифт ===
    public static final ForgeConfigSpec.DoubleValue UI_SCALE_OVERRIDE;
    public static final ForgeConfigSpec.DoubleValue FONT_SCALE;

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

    public static int windowFill() {
        return WINDOW_FILL.get();
    }

    public static int textPrimary() {
        return TEXT_PRIMARY.get();
    }

    public static int textLabel() {
        return TEXT_LABEL.get();
    }

    public static int textAuthor() {
        return TEXT_AUTHOR.get();
    }

    public static int textEmptyState() {
        return TEXT_EMPTY_STATE.get();
    }

    public static int accent() {
        return ACCENT.get();
    }

    public static int tabHover() {
        return TAB_HOVER.get();
    }

    public static int tabIdle() {
        return TAB_IDLE.get();
    }

    public static int buttonHover() {
        return BUTTON_HOVER.get();
    }

    public static int buttonIdle() {
        return BUTTON_IDLE.get();
    }

    public static int taskNormal() {
        return TASK_NORMAL.get();
    }

    public static int taskDone() {
        return TASK_DONE.get();
    }

    public static int taskFailed() {
        return TASK_FAILED.get();
    }

    public static int taskDescription() {
        return TASK_DESCRIPTION.get();
    }

    public static int taskLocation() {
        return TASK_LOCATION.get();
    }

    public static int pillBackground() {
        return PILL_BACKGROUND.get();
    }

    public static int scrollbarTrack() {
        return SCROLLBAR_TRACK.get();
    }

    public static int scrollbarThumb() {
        return SCROLLBAR_THUMB.get();
    }

    public static double uiScaleOverride() {
        return UI_SCALE_OVERRIDE.get();
    }

    public static double fontScale() {
        return FONT_SCALE.get();
    }

    private MenuCustomizationConfig() {
    }
}
