# 04 — Игроки: сообщения, позиции, инвентарь, стадии

Версия: **HollowEngine 1.19.2-1.6.2a**. Все фрагменты скомпилированы.

## Получение списка игроков

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.players

val players by server.players   // Safe<List<ServerPlayer>>
val first = players.invoke().first()   // ServerPlayer
```

`Safe<V>` реализует `Function0<V>`, поэтому `invoke()` возвращает реальное
значение, а обращение к свойствам делегирует через него.

В официальной документации это называется `team`. **В 1.6.2a `team` нет.**

## `@JvmName` — главная ловушка раздела

Внутри `IContextBuilder` часть функций помечена `@JvmName`. В байткоде они
называются `playerSend`, `playerSay` и т.п., но **в скриптах эти имена не
работают** — `unresolved reference`. В Kotlin у них обычные имена, как у
аналогов для NPC:

| JVM-имя (`@JvmName`) | Правильно в скрипте    | Возвращает   |
|----------------------|------------------------|--------------|
| `playerSend`         | `players.send { }`     | `SimpleNode` |
| `playerSendComponent`| `players.sendComponent { }` | `SimpleNode` |
| `playerSay`          | `players.say { }`      | `SimpleNode` |
| `playerSayComponent` | `players.sayComponent { }` | `SimpleNode` |
| `playerTpTo`         | `players.tpTo { }`     | `void`       |
| `playerPlay`         | `players.play { }`     | `SimpleNode` |
| `playerPlayOnce`     | `players.playOnce { }` | `SimpleNode` |
| `playerPlayLooped`   | `players.playLooped { }` | `SimpleNode` |
| `playerPlayFreeze`   | `players.playFreeze { }` | `SimpleNode` |
| `playerStop`         | `players.stop { }`     | `SimpleNode` |
| `playerConfigure`    | `players.configure { }` | `SimpleNode` |

Логика простая: у NPC и у игрокого списка **одинаковые имена методов**,
различается только receiver. Сравните `npc.say { }` и `players.say { }`.

`players.seat { }` псевдонима не имеет.

## Сообщения

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.color
import ru.hollowhorizon.hollowengine.common.scripting.italic
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.send

players.send { "привет" }                            // String
players.sendComponent { "привет".color(0xFFAA00) }  // Component
players.say { "хмм" }                               // в чат
players.sayComponent { "хмм".italic }
+send("Literal".bold)                                 // всем, кто видит скрипт
```

| Вызов                | Аргумент   | Куда                     |
|----------------------|------------|--------------------------|
| `players.send`       | `String`   | ActionBar/сообщение      |
| `players.say`        | `String`   | Чат                      |
| `players.sendComponent` | `Component` | Сообщение             |
| `players.sayComponent`   | `Component` | Чат                   |
| `send(Component)`        | `Component` | Всем зрителям        |

`send` — top-level extension, **требует импорта** `...base.send`. Аргумент
обязательно `Component`, поэтому `send("строка")` не компилируется — нужен
стиль: `+send("строка".bold)`. Возвращает `SimpleNode`, поэтому `+` допустим,
а вот без `+` узел в историю не попадёт.

## Прогресс-бар

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.progressManager

val pm = players.progressManager

+pm.addMessage { "Убейте 3 крипов" }
+pm.removeMessage { "Убейте 3 крипов" }
+pm.clear()
```

`players.progressManager` — member-extension на `Safe<List<ServerPlayer>>`,
возвращает `ProgressManager`. `addMessage`/`removeMessage`/`clear` возвращают
узлы → нужен `+`.

`ProgressManager.addMessage` из документации — это другой API. Здесь прогресс
берётся **из списка игроков**, а не из глобального синглтона.

## Позиция и телепорт

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.north
import ru.hollowhorizon.hollowengine.common.scripting.south

players.tpTo {
    pos = pos(0.0, 64.0, 0.0)
    vec = vec(0.0, 0.0)
    world = "minecraft:overworld"
}

players.seat {
    pos = pos(0.0, 64.0, 0.0)
    offset = 0.0
    rot = north
}
players.seat { rot = south }
```

`TeleportContainer`: `pos` (`Vec3`), `vec` (`Vec2`), `world` (`String`).
`SeatContainer`: `pos` (`Vec3`), `offset` (`Double`), `rot` (`Direction`).
`north`/`south`/`east`/`west` — extension на `SeatContainer`, дающие `Direction`.

## Ожидание позиции

```kotlin
players.waitPos {
    pos = pos(0.0, 64.0, 0.0)
    radius = 3.0
    world = "minecraft:overworld"
    inverse = false
    ignoreY = true
}
```

## Анимации игрока

```kotlin
players.play { animation = "Walk" }
players.playOnce { "Attack" }
players.playLooped { "Walk" }
players.playFreeze { "Idle" }
players.stop { "Idle" }
players.configure { skin("default") }
```

## Инвентарь

```kotlin
+players.saveInventory()
+players.loadInventory()
+players.clearInventory()
```

Все три возвращают `SimpleNode` → `+` обязателен.

## Эффекты

```kotlin
import net.minecraft.resources.ResourceLocation

+players.postEffect { ResourceLocation("hollowengine", "effects/confusion") }
+players.clearPostEffects()
+players.removeMark { pos(0.0, 0.0, 0.0) }
```

Все три возвращают `SimpleNode` → `+` допустим.

`PostEffect` лежит в пакете узлов, а не в `common.util`:

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.util.PostEffect
```

Это `object` с готовыми `ResourceLocation`: `BLUR`, `GRAY`, `SEPIA`, `SHAKE`,
`VIGNETTE`, `GLITCH`, плюс `PostEffect.create("путь")`.

## Стадии игрока

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.addStage
import ru.hollowhorizon.hollowengine.common.scripting.hasStage
import ru.hollowhorizon.hollowengine.common.scripting.removeStage
import ru.hollowhorizon.hollowengine.common.scripting.stages
import ru.hollowhorizon.hollowengine.common.scripting.stringName

val p = players.invoke().first()
println(p.stringName)
p.addStage("intro")
val has = p.hasStage("intro")
p.removeStage("intro")
val all: Collection<String> = p.stages
```

Все это extension-функции на `ServerPlayer`.

## Ввод

```kotlin
import ru.hollowhorizon.hollowengine.common.util.Keybind
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.await
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.waitForgeEvent
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.events.keybind
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.events.waitClick
import net.minecraftforge.event.level.BlockEvent

waitClick()                                    // лямбды НЕ принимает
keybind { Keybind.A }                          // возвращает void — "+" нельзя
+waitForgeEvent<BlockEvent.BreakEvent> { it.state.isAir }

val node = players.input("Войти", "Уйти")
println(node.values)
println(node.message)
+await { node.values.isNotEmpty() }
```

`players.input(vararg args)` возвращает `InputNode` (это `ReadWriteProperty<String>`).
Поля: `values` (выбранные варианты) и `message`.

`Keybind` — enum с GLFW-именами (`A`, `ESCAPE`, `SPACE`, …).
`waitForgeEvent` требует, чтобы скрипт был запущен через
`IForgeEventScriptSupport`.

> `gui { }` — member-extension на `Widget`, импортируется как
> `...story.nodes.base.events.gui`. Внутри доступны `close()` и
> `runSingleCommandInGui("команда")`.

## Ссылки

- `StoryEngine/examples/01_first_join.se.kts` — сообщения
- `StoryEngine/examples/05_items_and_storage.se.kts` — инвентарь
- `StoryEngine/examples/06_choice_screen.se.kts` — ввод и GUI
- [03 — NPC](03_NPC_RU.md) · [07 — Грабли](07_GOTCHAS_RU.md)
