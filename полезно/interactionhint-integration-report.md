# Отчёт: интеграция interactionhint-1.0.2 в StoryEngine

## Исходник

- Jar: `полезно/interactionhint-1.0.2.jar` (мод vaxxdev/InteractionHint, Forge 1.19.x).
- Лицензия: «All Rights Reserved» → **нельзя** копировать классы/текстуры 1:1.
  Логика переписана под собственный пакет `com.storyengine.interaction` своими
  классами и именами; текстуры оригинала не используются (HUD и мировые маркеры
  полностью процедурные).
- Декомпиляция: CFR 0.152. Исходники сохранены в
  `полезно/interactionhint-1.0.2-decompiled/` (9 классов, пакет `com.vaxxdev.interactionhint`).

## Что взято из оригинала (как концепция)

| Класс оригинала | Что переняли | Где в StoryEngine |
|---|---|---|
| `ActionResolver` | классификация блоков/сущностей → ключи действий | `interaction/client/hint/VanillaActionResolver.java` |
| `HudRenderer` | HUD-подсказка «[F] действие» | `interaction/client/hint/render/InteractionHintHud.java` |
| `CrosshairRenderer` | мировые кольца-маркеры у целей | `interaction/client/hint/render/InteractionCrosshairMarker.java` |
| `ClientEvents` / `KeyHandler` | клиентский скан окрестности + клавиша | `InteractionHintScanner.java`, `InteractionInputHandler.java` |
| `ServerEvents` | серверный подхват предметов из рамки | `interaction/server/ItemFramePickupHandler.java` |
| `InteractionManager` / `Utils` / `InteractionHint` | утилиты/рег-хелперы | переписано на месте (дистанции, анимация ease) |

Отличия от оригинала: детекция «взглядом» расширена до JSON-триггеров
StoryEngine (Interaction System), F-фолбэк делает ванильный use, маркеры
процедурные, конфиг — клиентский (секции в `story_engine-client.toml`).

## Новые файлы Interaction Hint System

- `interaction/client/hint/VanillaActionResolver.java` — `getBlockAction`/`getEntityAction`
  (open/use/press/toggle/sleep/ring/take/trade/sit).
- `interaction/client/hint/InteractionHintState.java` — static-состояние: прицел,
  позиция, буфер целей.
- `interaction/client/hint/InteractionHintScanner.java` — скан куба (радиус из
  конфига, каждые 4 тика), лимит маркеров, центрирование дверей/двойных сундуков,
  пропуск JSON-триггеров (нет двойного показа).
- `interaction/client/hint/render/InteractionHintHud.java` — «[F] Действие»,
  анимация progress/ease, заливка+рамка, slide+scale.
- `interaction/client/hint/render/InteractionCrosshairMarker.java` — кольца через
  `RenderType.lightning()`, billboard от камеры, aimed-кольцо+диск.
- `interaction/server/ItemFramePickupHandler.java` — `EntityInteractSpecific`,
  cancel + `spawnAtLocation` + `setItem(EMPTY)`, только сервер.
- `InteractionInputHandler` — F-фолбэк на ванильный `useItemOn`/`interactAt`/`interact`.
- `MenuCustomizationConfig` → секция `interactionHintCustomization`.
- lang en/ru: `story_engine.action.*`.
- Конфиг `interactionHintCustomization`: `enabled`, `scanRadius`, `maxMarkers`,
  `rightOffset`, `bottomOffset`, `fill`, `border`, `text`, `marker`, `markerAimed`.

## Ревью Interaction System — найденные и исправленные баги

1. **Окклюзия рейкаста** (`InteractionRaycastHandler`) — луч проходил сквозь
   стены. Теперь `break` на первом нетриггерном блоке с коллизией.
2. **Нет проверки измерения** (`C2SExecuteActionPacket`) — добавлена
   `trigger.getDimensionRL().equals(player.level.dimension().location())`.
3. **Утечка `LAST_EXECUTE`** (`InteractionNetworking`) — добавлена очистка
   устаревших записей при размере карты > 128.
4. **Размер S2C-пакета** — синхронизация триггеров разбита на порции по 64
   (`chunkIndex`/`chunkCount`), клиент накапливает и применяет на последней;
   триггеры с JSON > 30К символов пропускаются с warning.
5. **Мёртвые конфиг-ключи** `headerFill`/`headerText` — удалены.
6. **Висячие Javadoc-ссылки** на «спецификацию Interaction System §...» —
   создан `docs/INTERACTION_SYSTEM.md` (§2–§7 с совпадающей нумерацией).

При оформлении найден ещё один дефект интеграции: `sendSync`/`sendSyncToAll`
вызывали `CHANNEL.send(...)` (void) как аргумент — исправлено на передачу
`PacketTarget`.

## Сборка

`.\gradlew.bat compileJava` — чисто (только pre-existing deprecation warnings,
без errors).

## Как проверить в игре

1. `/story trigger reload` с JSON-триггерами в `config/story_engine/triggers/`.
2. Взгляд на ванильную дверь/сундук → правая нижняя подсказка «[F] Действие» +
   кольцо на блоке; F закрывает/открывает.
3. Фрейм с предметом: взгляд → F/K подхватывает предмет рамки (сервер).
4. Триггер: ПКМ открывает меню внизу-слева, выбор исполняет действие.
5. Настройки: `config/story_engine-client.toml` → `interactionHintCustomization`.