# 03 — NPC: создание, свойства, движение, анимации

Версия: **HollowEngine 1.19.2-1.6.2a**. Все фрагменты скомпилированы.

## Создание NPC

```kotlin
import ru.hollowhorizon.hollowengine.common.entities.NPCEntity

val npc by NPCEntity.creating {
    name = "Боб"
    model = "hollowengine:models/entity/player_model.gltf"
    world = "minecraft:overworld"
    pos = pos(0.5, 64.0, 0.5)
    rotation = vec(0.0, 180.0)
    showName = true
    switchHeadRot = true
    size = 0.6f to 2.0f
    animations[AnimationType.IDLE] = "Idle"
    textures["default"] = "textures/entity/bob.png"
    skin("default")
}
```

`val npc by NPCEntity.creating { }` даёт `Safe<NPCEntity>` — «ленивый» узел-делегат.
Сам спавн происходит при загрузке скрипта в машину состояний.

`creating` — member-extension на `NPCEntity.Companion`, поэтому пишется
`NPCEntity.creating { }`, а не `creating { }`.

### Поля `NpcContainer`

| Поле           | Тип                              | По умолчанию              |
|----------------|----------------------------------|---------------------------|
| `name`         | `String`                         | `"Неизвестный"`           |
| `model`        | `String`                         | `player_model.gltf`       |
| `world`        | `String`                         | `minecraft:overworld`     |
| `pos`          | `Vec3`                           | `(0,0,0)`                 |
| `rotation`     | `Vec2`                           | `Vec2.ZERO`               |
| `showName`     | `Boolean`                        | `true`                    |
| `switchHeadRot`| `Boolean`                        | `false`                   |
| `size`         | `Pair<Float, Float>`             | `0.6f to 1.8f`            |
| `transform`    | `Transform`                      | все нули, `texWidth=511`  |
| `attributes`   | `Attributes`                     | пустой                    |
| `animations`   | `HashMap<AnimationType, String>` | пустая                    |
| `textures`     | `HashMap<String, String>`        | пустая                    |

`pos(...)` и `vec(...)` внутри блока — **члены `IContextBuilder`**, они видны и
внутри вложенной лямбды. Импортировать `pos` не нужно.

### `Attributes`

Поле `attributes` существует только **внутри** `NPCEntity.creating { }`:

```kotlin
import ru.hollowhorizon.hollowengine.common.npcs.Attributes

val npc by NPCEntity.creating {
    name = "Боб"
    model = "hollowengine:models/entity/player_model.gltf"
    pos = pos(0.0, 64.0, 0.0)
    attributes = Attributes(
        "minecraft:generic.max_health" to 20f,
        "minecraft:generic.movement_speed" to 0.3f,
    )
}
```

`Attributes(vararg Pair<String, Float>)` — просто набор мод-атрибутов.

### `Transform`

`transform` — тоже поле блока `creating`:

```kotlin
import ru.hollowhorizon.hc.client.models.gltf.Transform

val npc by NPCEntity.creating {
    name = "Боб"
    model = "hollowengine:models/entity/player_model.gltf"
    pos = pos(0.0, 64.0, 0.0)
    transform = Transform(tX = 1.5f)
}
```

Поля: `tX..tZ` (смещение), `rX..rZ` (поворот), `sX..sZ` (масштаб), `texWidth`.

## Поиск и сабмодели

```kotlin
val found by NPCEntity.finding {
    name = "Боб"
    world = "minecraft:overworld"
}

val sub by NPCEntity.fromSubModel {
    subModel { model = "npc/sub.json" }
}
```

## Свойства NPC (member-extension на `Safe<NPCEntity>`)

```kotlin
import ru.hollowhorizon.hollowengine.common.npcs.HitboxMode
import ru.hollowhorizon.hollowengine.common.npcs.NpcIcon

npc.name = "Боб"
npc.flying = true
npc.invulnerable = true
npc.isRunning = true
npc.hitboxMode = HitboxMode.PULLING
npc.icon = NpcIcon.DIALOGUE

val title: String = npc.name
npc.despawn()
npc.configure { skin("default") }
```

| Свойство      | Тип                              | Значения                                            |
|---------------|----------------------------------|-----------------------------------------------------|
| `name`        | `String`                         | —                                                   |
| `flying`      | `Boolean`                        | —                                                   |
| `invulnerable`| `Boolean`                        | —                                                   |
| `isRunning`   | `Boolean`                        | `npc.running = …` **не существует**                  |
| `hitboxMode`  | `HitboxMode`                     | `BLOCKING`, `PULLING`, `EMPTY`                      |
| `icon`        | `NpcIcon`                        | `EMPTY`, `DIALOGUE`, `QUESTION`, `WARN`              |

> `NpcIcon` — **не enum**. Это data class, константы лежат на `NpcIcon.Companion`.

> У `NpcIcon` нет константы для NPC. Для своей иконки нужен свой `ResourceLocation`.

## Движение и цели

`moveTo`, `moveAlwaysTo`, `setTarget`, `lookAt`, `lookAlwaysAt` — **reified**
extension-функции. Тип цели задаётся явно, иначе компилятор не выведет `T`:

```kotlin
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

npc.moveTo<Vec3> { pos(10.0, 64.0, 10.0) }                    // ОК
npc.moveTo<LivingEntity> { players.invoke().first() }          // ОК
npc.moveTo<Collection<Vec3>> { listOf(pos(1.0, 1.0, 1.0)) }   // ОК
npc.moveTo { pos(1.0, 1.0, 1.0) }                              // ОШИБКА: cannot infer
```

`NpcTarget<T>` — интерфейс-тайп с `Function0<T>`, поэтому лямбда-таргеты
SAM-конвертируются автоматически.

### Допустимые типы цели

| Тип `T`                        | Смысл                          |
|--------------------------------|--------------------------------|
| `Vec3`                         | Точка в мире                   |
| `Entity` / `LivingEntity`      | Сущность                       |
| `Collection<Vec3>` / `List<…>` | Набор точек (путь)            |
| что-то другое                  | `IllegalArgumentException: Can't move to … target!` |

Для `setTarget` допустим **только** `LivingEntity`, иначе
`Can't attack a block!`.

`moveTo` блокирует поток до прибытия. `moveAlwaysTo` — постоянное движение,
останавливается через `stopMoveAlways()`.

### Биом и структура

```kotlin
npc.moveToBiome { "minecraft:plains" }
npc.moveToStructure({ "minecraft:village" })
```

> `moveToStructure(structureName, offset)` — `offset` не-null и имеет значение
> по умолчанию. **Не передавайте trailing lambda**: она привяжется к `offset`.
> Всегда пишите `moveToStructure({ "…" })`.
>
> `team.randomPos()` из документации в 1.6.2a отсутствует.

### Взгляд

```kotlin
npc.lookAt<Vec3> { pos(0.0, 0.0, 0.0) }
npc.lookAlwaysAt<LivingEntity> { players.invoke().first() }
npc.stopLookAlways()
npc.lookAtEntityType { "minecraft:zombie" }
npc.setPose { "minecraft:sitting" }
npc.clearPose()
```

## Реплики

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.color
import ru.hollowhorizon.hollowengine.common.scripting.italic

npc.say { "привет" }                                   // String
npc.sayComponent { "привет".color(0xFFFFFF) }          // Component
npc.sayComponent { "привет".italic }
```

`say` принимает `String`, `sayComponent` — `Component`. Форматирование:
`color(0xFFFFFF)` или `color("#FFFFFF")`, `bold`, `italic`, `obfuscated`,
`strikethrough`. **`underline` возвращает `String`, а не `Component`** —
это исключение в API.

Обе функции возвращают узлы, но регистрируют себя сами → `+` не нужен.

## Анимации

```kotlin
import ru.hollowhorizon.hc.client.models.gltf.animations.AnimationType
import ru.hollowhorizon.hc.client.models.gltf.animations.PlayMode
import ru.hollowhorizon.hc.client.models.gltf.manager.LayerMode

npc.play { animation = "Walk"; playType = PlayMode.LOOPED; layerMode = LayerMode.OVERWRITE; speed = 1.0f }
npc.playOnce { "Attack" }
npc.playLooped { "Walk" }
npc.playFreeze { "Idle" }
npc.stop { "Idle" }
npc.replay { "Idle" }
npc.replayAndWait { "Idle" }
```

`AnimationContainer`: `animation` (String), `layerMode` (`LayerMode`, дефолт `ADD`),
`playType` (`PlayMode`, дефолт `LOOPED`), `speed` (`Float`, дефолт `1.0f`).

`npc.play { }` — **без `+`**: возвращает `void`. Остальные (`playOnce`,
`playLooped`, `playFreeze`, `stop`) возвращают `SimpleNode` → пишутся без `+`,
потому что регистрируют себя сами.

В `creating { }` анимации задаются как карта:

```kotlin
val npc by NPCEntity.creating {
    name = "Боб"
    model = "hollowengine:models/entity/player_model.gltf"
    pos = pos(0.0, 64.0, 0.0)
    animations[AnimationType.IDLE] = "Idle"
    animations[AnimationType.WALK] = "Walk"
}
```

`AnimationType` — enum из `hc`, 15 значений (`IDLE`, `WALK`, `ATTACK`, …).

## Прочее

```kotlin
npc.tpTo {
    pos = pos(0.0, 64.0, 0.0)
    vec = vec(0.0, 0.0)
    world = "minecraft:overworld"
}
npc.useBlock { pos(1.0, 1.0, 1.0) }
npc.destroyBlock { pos(1.0, 1.0, 1.0) }
npc.dropItem { item("minecraft:diamond", 1) }
npc.giveLeftHand { item("minecraft:paper", 3) }
npc.giveRightHand { item("minecraft:diamond", 1) }
npc.waitInteract()
npc.requestItems {
    text = "Отдай 3 яблока"
    +item("minecraft:apple", 3)
}
npc.addTrade { MerchantOffer(item("minecraft:paper", 1), item("minecraft:apple", 1), 1, 1, 1.0f) }
npc.clearTrades()
npc.clearTradeUses()
```

`tpTo` пишется и как `npc.tpTo { }` (JVM-имя `playerTpTo` у игроков).

## Ссылки

- `StoryEngine/examples/01_first_join.se.kts` — создание и реплики
- `StoryEngine/examples/02_npc_dialogue.se.kts` — диалоги
- `StoryEngine/examples/05_items_and_storage.se.kts` — торговля и предметы
- [02 — Узлы](02_NODES_RU.md) · [07 — Грабли](07_GOTCHAS_RU.md)
