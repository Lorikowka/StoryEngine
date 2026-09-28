# 02 — Узлы и управление потоком

Версия: **HollowEngine 1.19.2-1.6.2a**. Все фрагменты скомпилированы.

## Модель исполнения

Скрипт — это не набор команд, а **список узлов** (`Node`). Каждый узел умеет
`tick()` и возвращает `true`, когда он завершён. Машина состояний
(`StoryStateMachine`) по очереди опрашивает узлы каждый игровой тик.

Отсюда главное правило:

| Что делает вызов                              | Нужен ли `+` |
|------------------------------------------------|--------------|
| Регистрирует узел сам (most DSL-функции)       | Нет          |
| Только возвращает `Node`/`SimpleNode`          | **Да**       |

## Правило `+`

`+` — это `IContextBuilder.unaryPlus(node: T): T`: он **регистрирует** узел в
истории. Отсюда одно жёсткое, проверяемое компилятором правило:

> **`+` нельзя ставить перед вызовом, который возвращает `void`.**
> Это ошибка компиляции, а не runtime-проблема.

Проверенные нарушители: `dialogue`, `fadeInOut`, `pauseTime`, `resumeTime`,
`waitClick()`, `gui`, `keybind`, `test`, `players.seat`, `players.tpTo`,
`players.waitPos`, `npc.play`, `npc.playLooped`, `npc.playOnce`,
`npc.playFreeze`, `npc.stop`, `npc.replay`, `npc.replayAndWait`,
`npc.useBlock`, `npc.destroyBlock`, `npc.waitInteract`, `npc.lookAtEntityType`,
`npc.setPose`, `npc.clearPose`, `npc.giveLeftHand`, `npc.giveRightHand`,
`npc.moveTo`, `npc.lookAlwaysAt`, `npc.addTrade`, `npc.clearTrades`,
`npc.clearTradeUses`.

**Важная оговорка про остальные.** Для всех функций, возвращающих `Node`,
компилятор принимает вызов **и с `+`, и без него**. То есть забытый `+` — это не
ошибка компиляции, а молчаливое отсутствие узла в истории: код собирается, но
сценарий работает не так, как ожидается. Компилятором это не поймать — проверять
нужно смыслом. В списках ниже `+` расставлен так, как в официальном справочнике.

## Вызовы, возвращающие `void` — всегда без `+`

```kotlin
wait { 1.sec }
npc.say { "привет" }
npc.moveTo<Vec3> { pos(1.0, 64.0, 1.0) }
npc.giveRightHand { item("minecraft:stone", 1) }
players.tpTo { pos = pos(1.0, 64.0, 1.0) }
players.seat { rot = north }
npc.waitInteract()
npc.despawn()
pauseTime()
resumeTime()
```

## Вызовы, возвращающие `Node` — `+` допустим

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.If
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.While
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.DoWhile
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.next
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.await
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.fadeIn
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.fadeOut
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.playSound
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.stopSound
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.camera.camera
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.particles.effekseer
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.dialogue
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.choices
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.npcs.collectItems
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.world.waitStorage
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.send
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.startScript
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.stopScript
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.restartScript
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.execute

// возвращают Node — "+" допустим
+If({ kills >= 3 }) { wait { 1 } }
+While({ spin < 5 }) { spin++ }
+DoWhile({ spin < 5 }) { spin++ }
+next { }
+await { done }
+fadeIn { text = "Глава 1" }
+fadeOut { text = "Конец" }
+playSound { sound = "hollowengine:effects/bell" }
+stopSound { "hollowengine:effects/bell" }
+camera { static(40, pos(0.0, 0.0, 0.0), pos(1.0, 1.0, 1.0)) }
+effekseer { path = "hollowengine:particles/smoke" }
+choices { text = "Выбери" }
+collectItems { text = "Отдай" }
+waitStorage { pos = pos(1.0, 64.0, 1.0) }
+send("сообщение".bold)
+startScript { "other.se.kts" }
+stopScript { "other.se.kts" }
+restartScript()
+execute { "/say hello" }
+players.saveInventory()
+players.loadInventory()
+players.clearInventory()
+pm.addMessage { "шаг 1" }
+pm.removeMessage { "шаг 1" }
+pm.clear()

// возвращают void — "+" здесь ОШИБКА КОМПИЛЯЦИИ
dialogue { dialogueNpc = npc }
fadeInOut { text = "Загрузка" }
pauseTime()
resumeTime()
waitClick()
gui { close() }
keybind { Keybind.A }
```

`unaryPlus` (`+`) определён и на `IContextBuilder`, и на `NodeContextBuilder`,
поэтому работает на обоих уровнях вложенности.

## `wait` и время

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.wait

wait { 20 }        // тики
wait { 1.sec }     // 20 тиков
wait { 1.min }     // 1200 тиков
wait { 1.hours }   // 72000 тиков
```

`sec`, `min`, `hours` — **свойства-расширения на `Int`**, а не функции.
`sec(2)` не компилируется. В JVM это `IContextBuilder.getSec(I)I`.

## `await` — ждать событие

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.await

val node = players.input("Войти", "Уйти")
+await { node.values.isNotEmpty() }
println(node.values)
println(node.message)
```

`await` — узел, который блокирует поток, пока лямбда не вернёт `true`.

## Условия

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.If
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.Else
```

`If` возвращает `ConditionNode`. У узла есть **две** ветки: `ifTasks` и
`elseTasks` — цепочки `Elif` нет как отдельной структуры.

```kotlin
val branch = If({ kills >= 3 }) {
    +pm.removeMessage { "Убейте 3 крипов" }
}

branch.Elif({ kills >= 1 }) {   // member — импорт НЕ нужен
    wait { 1.sec }
}

branch.Else {                   // top-level extension — импорт НУЖЕН
    wait { 1.sec }
}
```

- `Elif` — member-функция на `ConditionNode` внутри `IContextBuilder`.
  Импортировать нельзя и не нужно.
- `Else` — **top-level extension** `fun ConditionNode.Else(tasks: …)`.
  Без `import ...base.Else` получаете `unresolved reference 'Else'`.
- `Else` **заменяет** `elseTasks` (в реализации `setElseTasks` делает `clear()`
  и `addAll`). Вызывать `Else` дважды перезапишет ветку, а не добавит новую.
- `If` также имеет перегрузку с `elseTasks` третьим аргументом:
  `If(condition) { ifTasks } Else { elseTasks }` — здесь `Else` передаётся как
  аргумент, и `import` всё равно нужен.

## Циклы

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.While
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.DoWhile
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.Break
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.Continue
```

`While`, `DoWhile`, `Break`, `Continue` возвращают узлы → `+` допустим.

**`Break` и `Continue` требуют метку** — это `Function0<String>`, а не пустой блок.
`+Break { }` не компилируется (`no value passed for parameter 'tag'`):

```kotlin
// Простой цикл
+While({ spin < 2 }) {
    spin++
    wait { 5 }
}

// Цикл с меткой: Break/Continue с такой же меткой управляют этим циклом.
// Метка — второй аргумент, до тела цикла.
+While({ kills < 3 }, { "hunt" }) {
    wait { 20 }
    +Break { "hunt" }
}

// Тело выполняется минимум один раз
+DoWhile({ spin < 5 }) { spin++ }
+DoWhile({ spin < 5 }, { "tag" }) { spin++ }
```

> `+Break { "метка" }` внутри цикла **без** такой же метки ничего не делает —
> цикл будет крутиться вечно.

Отрицание условия:

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.not

val neg = { false }.not()
```

## Управление сюжетом

```kotlin
+next { }                    // перейти к следующему узлу, не дожидаясь конца потока
+startScript { "chapter2.se.kts" }
+stopScript { "chapter2.se.kts" }
+restartScript()
+execute { "/say Привет" }
```

## Асинхронность

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.wait

val bg = async {                // НЕ async-функция Kotlin, а DSL-нода
    wait { 2.sec }
}
+bg.stop()
+bg.resume()
+bg.join()
```

`async` возвращает `AsyncProperty`. `stop`, `resume`, `join` — extension на
`AsyncProperty`. `stop`/`resume` возвращают узлы → `+`.

## Форматирование блоков

> Не сворачивайте builder-блоки в одну строку через `;`.
> Однострочное `NPCEntity.creating { name = "T"; model = "m" }` ломает вывод
> типов, и компилятор сообщает `cannot infer type for this parameter` на строке
> `with(__builder) {` — совсем не там, где проблема.
> Держите каждый builder-блок многострочным.

## Ссылки

- `StoryEngine/examples/03_quest_flow.se.kts` — условия и циклы
- `StoryEngine/examples/04_cutscene.se.kts` — `next`, `async`
- [01 — Основы](01_BASICS_RU.md) · [07 — Грабли](07_GOTCHAS_RU.md)
