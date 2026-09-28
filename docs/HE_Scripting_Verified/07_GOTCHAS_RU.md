# 07 — Грабли: что в документации не совпадает с 1.6.2a

Версия: **HollowEngine 1.19.2-1.6.2a**.

Официальная документация на <https://hollowengine-docs.readthedocs.io/en/latest/>
написана для более новой версии. Ниже — расхождения, которые ломают компиляцию.
Эталонный файл, где каждая строка скомпилирована:
`StoryEngine/examples/99_full_api_reference.se.kts`.

## 1. `team` не существует

В документации: `val team by ...`, `team.send { }`, `team.randomPos()`.
В 1.6.2a идентификатора `team` нет вообще.

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.players
val players by server.players
```

## 2. `@JvmName`-имена не работают в скриптах

Самая частая причина `unresolved reference`. Подробно — в
[04_PLAYERS_RU.md](04_PLAYERS_RU.md).

```kotlin
players.playerSend { "x" }   // ОШИБКА
players.send { "x" }        // ПРАВИЛЬНО
```

## 3. `moveToPos` / `setMovingPos` / `moveToTeam` не существуют

В документации движение описано неформально. Реальные имена:

| В документации       | В 1.6.2a                    |
|----------------------|-----------------------------|
| `moveToPos { }`      | `moveTo<Vec3> { }`          |
| `setMovingPos { }`   | `moveAlwaysTo<Vec3> { }`    |
| `moveToTeam { }`     | `moveTo<Collection<Vec3>> { }` |
| `setTargetTeam { }`  | `setTarget<LivingEntity> { }` |
| `stopMoving`         | `stopMoveAlways()`          |
| `setTargetPos`       | `clearTarget()`             |

И **все** они reified: тип цели указывается явно.

## 4. `particles` → `effekseer`

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.particles.effekseer
+effekseer { path = "hollowengine:particles/smoke" }
```

## 5. `hour` → `hours`

```kotlin
wait { 1.hours }   // 1.hour не компилируется
```

## 6. `ProgressManager` — глобальный синглтон → `players.progressManager`

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.progressManager
val pm = players.progressManager
+pm.addMessage { "шаг 1" }
```

## 7. `Int.hour`, `1.hour(s)` и другие числовые помощники — свойства, не функции

`sec`, `min`, `hours` — extension-**свойства** на `Int`.
`sec(2)`, `wait { 2.sec() }` не компилируются.

## 8. Точки в конце — `.invoke { }`

Кнопки в `choices`, `sonharDialogue` и в `choice` для строковых вариантов
добавляются так:

```kotlin
choices {
    text = "Выбери"
    "Текст".invoke { }   // кнопка со String — только тут
}
```

## 9. `addChoice` ждёт `Component`, а `choices.text` — `String`

```kotlin
dialogue {
    dialogueNpc = npc
    choice(ChoiceAction.SCREEN) {
        addChoice("Да".color(0x00FF00)) { }   // Component — обязательно
        "Да".invoke { }                      // либо так, со String
    }
}
```

`choices.text` — это **`String`**, а не `Component`. У `ChoiceScreenNode` нет
поля `options`: поля реплик (`name`/`text` как `Component`) живут внутри
`dialogue { options { } }`. Поэтому так не пишется:

```kotlin
choices {
    text = "Выбери"                  // String — верно
    options {                        // ОШИБКА: поля options у choices нет
        name = "Боб".color(0xFFFFFF)  // name/text здесь — Component
        text = "Что тебе?".color(0xFFFFFF)
    }
}
```

Правильно — `Component` только внутри `dialogue`:

```kotlin
dialogue {
    options {
        name = "Боб".color(0xFFFFFF)
        text = "Что тебе?".color(0xFFFFFF)
    }
}
```

## 10. `spline` ждёт `String`, а не список точек

`spline` — член `CameraNode`, поэтому вызывается внутри `camera { }`, и путь
сцены передаётся **строкой**:

```kotlin
camera {
    spline(
        60, "my_path",
        Interpolation.SINE_IN_OUT, true,
        Interpolation.SINE_IN_OUT, Interpolation.SINE_IN_OUT,
    )
}
```

Список `Vec3` не подойдёт — `path: String`.

## 11. У `shake` нет поля `intensity`

Есть `intensity1`, `intensity2`, `intensity3`.

## 12. `npc.running` не существует

Kotlin-имя — `npc.isRunning` (JVM: `getIsRunning`/`setRunning`).

## 13. `NpcIcon` — не enum

`NpcIcon.EMPTY/DIALOGUE/QUESTION/WARN` — свойства на `NpcIcon.Companion`.

## 14. Стилевые расширения нельзя произвольно компоновать

Возвращаемые типы разные, и это ломает цепочки:

| Вызов                  | Возвращает   | Можно продолжить цепочку? |
|------------------------|--------------|---------------------------|
| `"x".underline`        | `String`     | **да**                    |
| `"x".bold`             | `Component`  | нет                       |
| `"x".italic`           | `Component`  | нет                       |
| `"x".obfuscated`       | `Component`  | нет                       |
| `"x".strikethrough`    | `Component`  | нет                       |
| `"x".color(0xFFAA00)`  | `Component`  | нет                       |

```kotlin
println("x".underline.bold)        // OK — underline вернул String
println("x".bold.color(0xFFAA00))  // ОШИБКА — bold вернул Component
players.sayComponent { "x".color("#FFAA00") }
```

Важно: это **не** свойство. `underline` — обычное extension-свойство `String`,
поэтому пишется `"x".underline`, а не `underline` в одиночку.

Ещё нюанс: `color` принимает и `Int`, и `String` (`"#RRGGBB"`).

## 16. `Elif` — member, `Else` — top-level extension

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.Else
// Elif импортировать НЕ нужно и нельзя

val flag = true
val branch = If({ flag }) { wait { 1 } }
branch.Elif({ !flag }) { wait { 2 } }
branch.Else { wait { 3 } }
```

`Else` перезаписывает `elseTasks`, а не добавляет ветку.

## 17. Не сокращайте builder-блоки в одну строку

```kotlin
val npc by NPCEntity.creating { name = "T"; model = "m" }   // ломает вывод типов
```

Ошибка приходит как `cannot infer type for this parameter` на строке
`with(__builder) {` — то есть совсем не на том месте.

## 18. `pos(...)` и `vec(...)` — члены, импорт не нужен

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.pos   // ОШИБКА: нет такого класса
```

## 19. `moveToStructure` — не передавайте trailing lambda

`offset` имеет значение по умолчанию, и trailing lambda привяжется к нему:

```kotlin
npc.moveToStructure({ "minecraft:village" })   // ПРАВИЛЬНО
```

## 20. Явных импортов «из коробки» нет

`ScriptJvmCompilerImplsMixin` только рассылает `AfterCodeAnalysisEvent`, чтобы
другие моды могли добавить свои исходники. Никаких неявных `import *` нет —
все top-level расширения импортируются вручную.

## 21. `send { }` vs `send(Component)`

`send` в `SimpleNodeKt` принимает **`Component`**, поэтому `send("строка")`
→ `argument type mismatch: actual type is String, but Component was expected`.
Строковый вариант — только `players.send { }` / `players.say { }`.

## 22. `+` нельзя ставить перед вызовом, возвращающим `void`

`+` — это `IContextBuilder.unaryPlus(node: T): T`, и применять его к `Unit`
нельзя: **ошибка компиляции**.

```kotlin
+gui { close() }            // ОШИБКА
gui { close() }             // ПРАВИЛЬНО
+waitClick()                // ОШИБКА
waitClick()                 // ПРАВИЛЬНО
+keybind { Keybind.A }      // ОШИБКА
+dialogue { dialogueNpc = npc }   // ПРАВИЛЬНО, без "+"
```

Полный список нарушителей — в [02 — Узлы](02_NODES_RU.md#правило-).

Обратная сторона: для функций, возвращающих `Node`, компилятор принимает **и с
`+`, и без**. Забытый `+` не ловится компилятором — узел просто не добавится в
историю.

## 23. `Break` и `Continue` требуют метку

```kotlin
+Break { }          // ОШИБКА: no value passed for parameter 'tag'
+Break { "hunt" }   // ПРАВИЛЬНО
```

Метка — второй аргумент цикла, до тела:

```kotlin
+While({ kills < 3 }, { "hunt" }) {
    wait { 20 }
    +Break { "hunt" }
}
```

## 24. `collectItems` и `waitStorage` — receiver-лямбды, `it` не работает

```kotlin
collectItems { it.items.add(item("minecraft:stone", 8)) }   // ОШИБКА: unresolved 'it'
collectItems { items.add(item("minecraft:stone", 8)) }     // ПРАВИЛЬНО
```

`StorageItemList` наследует `GiveItemList`, поэтому доступны поля `world`, `pos`,
`consumeItems`, `text`, `items`.

## 25. `waitClick()` не принимает лямбду

```kotlin
+waitClick { }        // ОШИБКА
waitClick()           // ПРАВИЛЬНО
```

## 26. `north`/`south`/`east`/`west` — не константы `Direction`

Это extension-свойства на `SeatContainer`, а не свободные значения:

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.north

players.seat { rot = north }                          // ПРАВИЛЬНО
val dirs = listOf(north, south, east, west)           // ОШИБКА
val dirs: List<Direction> = listOf(Direction.NORTH)   // ПРАВИЛЬНО
```

## 27. Стадии игрока — расширения на `ServerPlayer`

`addStage`, `removeStage`, `hasStage`, `stages`, `stringName` висят на
`ServerPlayer`, а не на `Safe<List<ServerPlayer>>`:

```kotlin
val p = players.invoke().first()      // Safe<List<ServerPlayer>> -> ServerPlayer
p.addStage("intro")
val all: Collection<String> = p.stages
```

## 28. Формат файла

Расширение `<имя>.<se|mod|content>.kts` определяет receiver. Стори-скрипт в
`.mod.kts` не скомпилируется: `.mod.kts` получает `ModScript`, где нет
`wait`, `If`, `NPCEntity.creating`.

## Ссылки

- `StoryEngine/examples/99_full_api_reference.se.kts` — эталон
- `C:\Users\goumi\AppData\Local\Temp\opencode\he\verify\FINDINGS.md` — рабочие заметки
