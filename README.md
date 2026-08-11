# KitPvP

Мини-игра KitPvP для Spigot/Paper 1.16.5: арены, наборы экипировки (киты), статистика боя, комбо-серии и скорборды.

## Возможности

- Арены: выделение зоны (`pos1`/`pos2`), защита от разрушения блоков
- Киты: экипировка, броня, эффекты, кулдауны, покупка за деньги (через EconomyPlus)
- Дефолтный кит при спавне и телепорт на спавн арены
- Статистика: убийства, смерти, KDR, лучший киллстрик — с сохранением
- Скорборд с боевой статистикой
- Комбо-система с настраиваемым окном (`combo-window-ms`)
- Награды за киллстрики: сообщения и консольные команды
- Soft-depend: EconomyPlus

## Команды

| Команда | Описание | Право |
|---|---|---|
| `/kit [name]` | Выдать себе кит (с кулдауном) | `crystalox.kitpvp.command.kit` |
| `/kits` | Список доступных китов | — |
| `/stats [player]` | Статистика игрока / топ | `crystalox.kitpvp.command.stats` |
| `/kitpvp pos1` / `pos2` | Выделить зону арены | `crystalox.kitpvp.admin` |
| `/kitpvp savearena` | Сохранить арену | `crystalox.kitpvp.admin` |
| `/kitpvp setspawn` | Точка спавна | `crystalox.kitpvp.admin` |
| `/kitpvp reload` | Перезагрузить конфиг | `crystalox.kitpvp.admin` |

## Права

- `crystalox.kitpvp.admin` — админ-команды (default: op)
- `crystalox.kitpvp.command.kit` — `/kit` (default: true)
- `crystalox.kitpvp.command.stats` — `/stats` (default: true)

## Конфиг (`config.yml`)

```yaml
spawn-location: { world: world, x: 0, y: 64, z: 0 }
arena:
  enabled: false
  world: world
  min: { x: -50, y: 0, z: -50 }
  max: { x: 50, y: 256, z: 50 }

default-kit: 'warrior'
spawn-protection-radius: 10
enable-combo: true
combo-window-ms: 3000

killstreak-rewards:
  3: '&e%player% &7is on a 3 killstreak!'
  5: 'cmd:give %player% diamond 1'
  10: '&6%player% &eis unstoppable!'

kits:
  warrior:
    display-name: '&cWarrior'
    icon: IRON_SWORD
    cooldown-seconds: 30
    price: 0            # цена покупки (0 = бесплатно), через EconomyPlus
    items:
      - material: IRON_SWORD
        name: '&cWarrior Sword'
        enchants: { SHARPNESS: 2 }
    armor:
      helmet: { material: IRON_HELMET }
      chestplate: { material: IRON_CHESTPLATE }
    effects:
      - SPEED:0:0
```

## Зависимости

- Paper/Spigot 1.16.5+
- `EconomyPlus` (soft-depend) — покупка китов

## Сборка

```bash
gradle build
# результат: build/libs/KitPvP-1.0.0.jar
```
