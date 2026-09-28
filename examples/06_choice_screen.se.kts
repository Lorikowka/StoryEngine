// Пример 6 — ввод от игрока: GUI-кнопки, клавиши, окно выбора, sonhar-диалог.
// Показывает: gui, waitClick, keybind, players.input, choices, sonharDialogue, await.
@file:EntryPoint

package storyengine.examples

import ru.hollowhorizon.hollowengine.common.util.Keybind
import ru.hollowhorizon.hollowengine.common.scripting.color
import ru.hollowhorizon.hollowengine.common.scripting.players
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.await
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.events.gui
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.events.keybind
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.events.waitClick
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.wait
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.choices
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.sonhar.sonharDialogue

val players by server.players

// `gui { }` — обычный Minecraft-гуи. Внутри доступны `close()` и
// `runSingleCommandInGui("команда")` — они работают как extension на Widget.
+gui {
    runSingleCommandInGui("time set day")
    close()
}

// Ждём, пока игрок кликнет в любом месте экрана.
+waitClick()

// Или нажмёт конкретную клавишу. Keybind — enum с GLFW-именами (Keybind.A, Keybind.ESCAPE ...).
+keybind { Keybind.A }

// Свой виджет выбора: `input` возвращает InputNode, у которого есть `values` и `message`.
// Пока игрок не выберет вариант, сюжет стоит на `await`.
val node = players.input("Войти в город", "Отойти")
+await { node.values.isNotEmpty() }

val chosen = node.values.firstOrNull()

// Окно выбора без NPC. В `choices` поле `text` — обычный String.
choices {
    text = "Куда идём?"
    background = "hollowengine:textures/gui/choices.png"
    textX = 10
    textY = 20
    buttonX = 20
    buttonY = 60
    entityX = 120
    entityY = 120
    entitySize = 2.0f
    open = true
    "В город".invoke { players.send { "Идём в город." } }
    "В лес".invoke { players.send { "Идём в лес." } }
}

// sonhar-диалог: список реплик с кнопкой «Далее» и своим звуком.
+sonharDialogue {
    open = true
    text.add("Ты готов к пути?")
    text.add("Тогда не задерживайся.")
    sounds.add("hollowengine:effects/bell")
    "Готов".invoke { }
    "Не готов".invoke { }
}

players.sendComponent {
    ("Выбрано: " + (chosen ?: "ничего")).color(0x55FF55)
}

wait { 1.sec }
