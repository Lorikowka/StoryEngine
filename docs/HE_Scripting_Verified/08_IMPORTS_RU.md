# 08 — Полный список импортов

Версия: **HollowEngine 1.19.2-1.6.2a**.

Список получен из скомпилированного файла
`StoryEngine/examples/99_full_api_reference.se.kts` — 78 импортов, каждый
проверен компилятором Kotlin 2.0.21 против `hc-1.19.2-1.6.3.jar`.

## Правило

Явных невидимых импортов в HollowEngine **нет**. Член-функции текущего receiver
(`StoryScript` / `IContextBuilder` / `NodeContextBuilder`) доступны без импорта:
`pos`, `vec`, `server`, `stateMachine`, `test`, `unaryPlus`, `Elif`.

Всё, что ниже, — top-level расширения, их нужно импортировать явно.

## Аннотации файла

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.AfterScript
import ru.hollowhorizon.hollowengine.common.scripting.story.EntryPoint
import ru.hollowhorizon.hollowengine.common.scripting.story.JoinScript
import ru.hollowhorizon.hollowengine.common.scripting.story.RepeatableScript
```

## Основное (`...common.scripting`)

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.RUNTIME_PROPERTIES
import ru.hollowhorizon.hollowengine.common.scripting.addStage
import ru.hollowhorizon.hollowengine.common.scripting.bold
import ru.hollowhorizon.hollowengine.common.scripting.color
import ru.hollowhorizon.hollowengine.common.scripting.east
import ru.hollowhorizon.hollowengine.common.scripting.hasStage
import ru.hollowhorizon.hollowengine.common.scripting.item
import ru.hollowhorizon.hollowengine.common.scripting.italic
import ru.hollowhorizon.hollowengine.common.scripting.north
import ru.hollowhorizon.hollowengine.common.scripting.obfuscated
import ru.hollowhorizon.hollowengine.common.scripting.players
import ru.hollowhorizon.hollowengine.common.scripting.removeStage
import ru.hollowhorizon.hollowengine.common.scripting.runtime
import ru.hollowhorizon.hollowengine.common.scripting.skin
import ru.hollowhorizon.hollowengine.common.scripting.south
import ru.hollowhorizon.hollowengine.common.scripting.stages
import ru.hollowhorizon.hollowengine.common.scripting.strikethrough
import ru.hollowhorizon.hollowengine.common.scripting.stringName
import ru.hollowhorizon.hollowengine.common.scripting.tag
import ru.hollowhorizon.hollowengine.common.scripting.test
import ru.hollowhorizon.hollowengine.common.scripting.underline
import ru.hollowhorizon.hollowengine.common.scripting.west
```

> `pos` и `vec` здесь **отсутствуют намеренно** — это члены `IContextBuilder`.

## Переменные (`...scripting.story`)

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.global
import ru.hollowhorizon.hollowengine.common.scripting.story.progressManager
import ru.hollowhorizon.hollowengine.common.scripting.story.saveable
```

## Базовые узлы (`...nodes.base`)

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.Break
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.Continue
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.DoWhile
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.Else
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.If
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.While
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.await
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.execute
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.fadeIn
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.fadeInOut
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.fadeOut
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.next
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.not
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.playSound
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.restartScript
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.send
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.startScript
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.stopScript
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.stopSound
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.wait
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.waitForgeEvent
```

> `Elif` и `unaryPlus` импортировать нельзя — это члены `IContextBuilder`.

## События и ввод (`...nodes.base.events`)

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.events.gui
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.events.keybind
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.events.waitClick
```

## Камера, частицы, мир, NPC, диалоги

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.camera.camera
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.camera.shake
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.ChoiceAction
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.choices
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.dialogue
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.sonhar.sonharDialogue
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.npcs.asSubModel
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.npcs.collectItems
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.npcs.distanceToXZ
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.npcs.subModel
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.npcs.subModels
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.particles.effekseer
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.world.pauseTime
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.world.resumeTime
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.world.waitStorage
```

## Классы из HollowEngine и `hc`

```kotlin
import ru.hollowhorizon.hollowengine.common.entities.NPCEntity
import ru.hollowhorizon.hollowengine.common.npcs.Attributes
import ru.hollowhorizon.hollowengine.common.npcs.HitboxMode
import ru.hollowhorizon.hollowengine.common.npcs.NpcIcon
import ru.hollowhorizon.hollowengine.common.util.Keybind
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.util.PostEffect
import ru.hollowhorizon.hc.client.models.gltf.Transform
import ru.hollowhorizon.hc.client.models.gltf.animations.AnimationType
import ru.hollowhorizon.hc.client.models.gltf.animations.PlayMode
import ru.hollowhorizon.hc.client.models.gltf.manager.LayerMode
import ru.hollowhorizon.hc.client.utils.math.Interpolation
```

> `PostEffect` лежит в `...scripting.story.nodes.util`, **не** в
> `...common.util`. Пакет `nodes.util` содержит и `PostEffect`, и другие
> вспомогательные классы узлов.

## Классы Minecraft

```kotlin
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
```

## `@file:Import`

Документация описывает аннотацию для подключения внешних скриптов:

```kotlin
@file:Import("../path/to/file.kts")
```

**Эта функция не проверялась компилятором**: она требует внешний пакет
`ru.hollowhorizon.kotlinscript`, которого нет среди зависимостей HollowEngine.
Импортируемые переменные пересоздаются заново, кроме runtime-переменных.

## Быстрая проверка

```powershell
python C:\Users\goumi\AppData\Local\Temp\opencode\he\verify\wrap.py <out-dir> <file.se.kts>
C:\Users\goumi\AppData\Local\Temp\opencode\he\verify\kcheck.ps1 -Files <out-dir\file.se.kt>
```

`wrap.py` оборачивает скрипт в `fun __script(__builder: StoryScript)` и
подставляет импорты file-аннотаций; `kcheck.ps1` запускает `kotlinc`.

## Импорт есть, но вызов всё равно не компилируется

Импорт — только половина дела. Частые ошибки после правильного импорта:

| Ошибка | Причина |
|--------|---------|
| `unaryPlus` не применяется к `Unit` | вызов возвращает `void` — уберите `+` |
| `actual type is String, but Component was expected` | `send` ждёт `Component`: `+send("x".bold)` |
| `no value passed for parameter 'tag'` | `Break`/`Continue` требуют метку: `+Break { "hunt" }` |
| `unresolved reference 'it'` | лямбда — receiver-стиля: `collectItems { items.add(…) }` |
| `function invocation 'waitClick()' expected` | `waitClick` не принимает лямбду |
| `receiver type mismatch` на `north` | это свойство `SeatContainer`, не константа `Direction` |
| `cannot infer type for this parameter` | builder-блок записан в одну строку с `;` |

Подробности — в [07 — Грабли](07_GOTCHAS_RU.md).

## Ссылки

- `StoryEngine/examples/99_full_api_reference.se.kts` — все 78 импортов в одном файле
- [07 — Грабли](07_GOTCHAS_RU.md)
