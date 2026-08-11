# KitPvP

A kit-based PvP arena plugin for Paper 1.16.5+. KitPvP delivers class-based combat with fully configurable kits, persistent SQLite statistics, killstreak rewards, spawn protection, and an optional integration with EconomyPlus for kit pricing.

---

## Introduction

KitPvP turns any arena into a competitive kit fight. Players receive a default kit on join, choose additional kits from an interactive GUI or by command, and earn persistent stats that are displayed on a live scoreboard. Admins define kits entirely in `config.yml` — items, armor, potion effects, cooldowns, prices, and per-kit permissions — without touching code.

## Features

- **Kits with cooldowns, prices, and effects** — each kit defines its own inventory, armor set, potion effects, cooldown, optional price, and optional permission
- **Kit GUI** — `/kit` with no arguments opens a clickable kit selection inventory; `/kits` lists everything available
- **SQLite statistics** — kills, deaths, KDR, and best killstreak persisted in `kitpvp.db`
- **Killstreak rewards** — configurable milestone announcements and console commands at streak thresholds
- **Spawn protection** — a configurable radius around the arena spawn blocks building and PvP damage
- **Scoreboard** — a live scoreboard showing the player's stats and top killers
- **Combo action bar** — consecutive hits within a configurable time window show a combo counter on the action bar
- **Arena bounds protection** — block breaking and placing are restricted to the configured arena region

## Commands

| Command | Description | Permission |
|---|---|---|
| `/kit [name]` | Apply a kit by name, or open the kit GUI when no name is given | `crystalox.kitpvp.command.kit` |
| `/kits` | List all available kits | `crystalox.kitpvp.command.kit` |
| `/stats [player]` | Show your statistics, or another player's | `crystalox.kitpvp.command.stats` |
| `/kitpvp pos1` | Set the first arena corner at your position | `crystalox.kitpvp.admin` |
| `/kitpvp pos2` | Set the second arena corner at your position | `crystalox.kitpvp.admin` |
| `/kitpvp savearena` | Persist the arena bounds from pos1/pos2 | `crystalox.kitpvp.admin` |
| `/kitpvp setspawn` | Set the arena spawn at your position | `crystalox.kitpvp.admin` |
| `/kitpvp reload` | Reload kits, arena, and messages from disk | `crystalox.kitpvp.admin` |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `crystalox.kitpvp.admin` | Full access to `/kitpvp` administration commands | op |
| `crystalox.kitpvp.command.kit` | Use `/kit` and `/kits` | true |
| `crystalox.kitpvp.command.stats` | Use `/stats` | true |

Kit access itself is controlled per kit through the `permission` key in `config.yml`; set it to `none` (the default) to make a kit available to everyone.

## Configuration

Kits are defined under the `kits` section of `config.yml`. This is one complete kit:

```yaml
kits:
  warrior:
    display-name: '&cWarrior'
    icon: IRON_SWORD
    description:
      - '&7A balanced melee kit'
    cooldown-seconds: 30
    permission: 'none'
    price: 0
    items:
      - material: IRON_SWORD
        name: '&cWarrior Sword'
        enchants:
          SHARPNESS: 2
      - material: GOLDEN_APPLE
        amount: 2
        name: '&6Apple'
    armor:
      helmet:
        material: IRON_HELMET
        name: '&cWarrior Helmet'
      chestplate:
        material: IRON_CHESTPLATE
        name: '&cWarrior Chestplate'
      leggings:
        material: IRON_LEGGINGS
        name: '&cWarrior Leggings'
      boots:
        material: IRON_BOOTS
        name: '&cWarrior Boots'
    effects:
      - SPEED:0:0
```

Other top-level settings include `spawn-location`, `arena` (bounds), `spawn-protection-radius`, `default-kit`, `enable-combo` / `combo-window-ms`, `killstreak-rewards` (messages or `cmd:` actions), and `scoreboard-title`. Killstreak rewards and message formats support color codes and `%player%`, `%kit%`, `%kills%` placeholders.

## EconomyPlus Integration

When EconomyPlus is present, kits with a `price` greater than zero are purchased on selection. The cost is withdrawn from the player's balance in EconomyPlus' default currency; players without sufficient funds are refused the kit. If EconomyPlus is absent, priced kits are granted for free, so the plugin works standalone.

## Build

Requirements: JDK 8+ and a Gradle wrapper.

```bash
./gradlew build
```

The shaded jar is written to `build/libs/KitPvP-1.0.0.jar` and includes the SQLite driver. Drop it into `plugins/` and restart your Paper 1.16.5+ server. EconomyPlus is optional and can be added at any time.

## License

MIT License — Copyright (c) 2026 CrystalOx Portfolio. See [LICENSE](LICENSE).
