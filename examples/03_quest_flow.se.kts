// Пример 3 — квестовый поток: стадии, сохранение прогресса, условия и циклы.
// Показывает: saveable/global/runtime, addStage, If/Elif/Else, While, Break, предметы.
@file:EntryPoint

package storyengine.examples

import ru.hollowhorizon.hollowengine.common.scripting.addStage
import ru.hollowhorizon.hollowengine.common.scripting.hasStage
import ru.hollowhorizon.hollowengine.common.scripting.item
import ru.hollowhorizon.hollowengine.common.scripting.players
import ru.hollowhorizon.hollowengine.common.scripting.removeStage
import ru.hollowhorizon.hollowengine.common.scripting.runtime
import ru.hollowhorizon.hollowengine.common.scripting.story.global
import ru.hollowhorizon.hollowengine.common.scripting.story.progressManager
import ru.hollowhorizon.hollowengine.common.scripting.story.saveable
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.Break
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.DoWhile
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.Else
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.If
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.While
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.wait

val players by server.players
val pm = players.progressManager

// saveable — переживает перезапуск сервера, привязан к игроку.
// global   — одно значение на весь сюжет.
// runtime  — только на время текущего запуска.
var kills by saveable { 0 }
var chapter by global { 1 }
var spawned by runtime { false }

// Прогресс-бар HUD: видно игроку, что делать.
+pm.addMessage { "Убейте 3 крипов" }

// Условие с ветками. `If` возвращает узел, `Else` — top-level extension
// (нужен импорт), `Elif` — member, импортировать НЕ нужно.
val branch = If({ kills >= 3 }) {
    +pm.removeMessage { "Убейте 3 крипов" }
    +pm.addMessage { "Поговорите с NPC" }
}

branch.Elif({ kills >= 1 }) {
    wait { 1.sec }
}

branch.Else {
    wait { 1.sec }
}

// Цикл с меткой: `Break { "метка" }` выходит из цикла с такой же меткой.
While({ kills < 3 }, { "hunt" }) {
    wait { 20 }
    +Break { "hunt" }
}

// Обычный цикл без метки — просто повтор, пока условие истинно.
var spin = 0
While({ spin < 2 }) {
    spin++
    wait { 5 }
}

// `DoWhile` выполняет тело минимум один раз.
DoWhile({ spin < 5 }) {
    spin++
    wait { 5 }
}

// Стадии выдаёт NPC или скрипт; проверяются в любом месте.
val first = players.invoke().first()
first.addStage("quest_started")

if (first.hasStage("quest_started")) {
    first.removeStage("quest_started")
}

// Прогресс выполнен: снимаем задачу и отмечаем главу.
+pm.clear()
chapter = 2
spawned = true
wait { 1.sec }
