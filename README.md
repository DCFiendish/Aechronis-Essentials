not helpful anymore cuz the aechronis devs are tryhards
all features besides hitboxes were ported over to map overlay 🙃
# Aechronis Essentials

A Fabric client mod for Minecraft 26.2 that colors player and entity hitboxes by
relationship - town, nation, ally, enemy, or neutral - using
[Aechronis](https://map.aechronis.net)'s public Nodes map API.

This is a **from-source, GPLv3 port** of the hitbox-coloring feature from
[`oreotrollturbo`'s CrusalisUtils](https://github.com/oreotrollturbo/Crusalis-utils-)
(itself a fork of the mod "Hitbox+"), retargeted from Crusalis's Dynmap/Towny API to
Aechronis's own Nodes API. It is **not** a full port of CrusalisUtils - see
"What's intentionally left out" below.

## What this mod does

- Recolors vanilla's F3+B debug hitboxes (auto-enabled on startup, no need to press F3+B
  yourself) for players and other entities, based on a configurable priority chain:
  1. Your own hitbox - configurable self color.
  2. Players on your manual friend list - configurable friend color.
  3. Players on your manual enemy list - configurable enemy color.
  4. Automatic relation lookup against Aechronis's live town/nation data
     (`https://map.aechronis.net/nodes/towns.json`): same town, same nation, allied nation,
     enemy nation, or neutral - each with its own configurable color.
  5. Vanilla scoreboard teammates - configurable friend color, as a fallback.
  6. Everyone else - configurable neutral color.
- Non-player entities (hostile/passive mobs, projectiles, decorations, vehicles, the Ender
  Dragon, and misc entities like item frames/TNT/XP orbs) get their own configurable
  enable/color/alpha toggles, independent of the relation system.
- Recolors the F3+G chunk-border debug lines by territory ownership, using the same
  relation pipeline (so ally/enemy/town land shows up at a glance), driven by
  `https://map.aechronis.net/nodes/world.json`.
- Full settings screen via Mod Menu + Cloth Config.
- Optional: automatically sends `/t spawn` in chat the instant you respawn after dying
  (off by default - toggle in config).

The mod only activates when you're connected to an Aechronis server (`*.aechronis.net`) -
it's inert everywhere else.

## What's intentionally left out

CrusalisUtils bundles a lot of unrelated quality-of-life features alongside hitbox
coloring. None of the following were ported - they're listed here because upstream itself
isn't well documented, so if you're used to CrusalisUtils, this is what you'd be missing:

- **Waypoint / navigation** - pathfinding to towns/ports/territories (`/navigateTo`,
  `/findTown`, `/findPort`, `/findNode`), auto-creating and advancing Xaero minimap
  waypoints as you travel.
- **Flag-to-waypoint war tracking** - parsing war/Towny chat messages (attacked, captured,
  liberated, defended) for towns on a watchlist, auto-placing/removing waypoints at the
  fighting.
- **Player coordinate/location "ping" sharing** - broadcasting your position or a
  raycast-targeted block via chat (with optional XOR+Base64 "encryption"), auto-parsed by
  other users of the original mod into temporary waypoints.
- **Automatic nation detection & watchlist population** - auto-detecting your nation on
  login and filling the war-flag watchlist with your nation's towns.
- **Rally point markers** - `/rallyPoint x y z time name` broadcasting a timed waypoint via
  chat.
- **Scoreboard rotation** - rotating the vanilla scoreboard sidebar to face your look
  direction, as a pseudo-compass.
- **Port travel eligibility filtering** - restricting navigation routing to
  nation/ally-owned ports.
- **Player "glow" highlighting** - a chat-protocol-based feature to make a specific ally
  glow.
- **Chat/announcement utilities** - special hotbar-styled announcement/DM display tied to
  the original mod author's account; join/leave message hiding.
- **Remote feature kill-switch** - a hidden server-sent chat string that could disable
  pings/sharing/hitbox-colors/flag-waypoints at runtime.
- **Misc commands** - `/ores` (inventory mining-yield stats), `/setPlayerRelation`,
  `/pinLocationPing`, an easter-egg command. (The underlying friend/enemy name-list
  *config* is kept - it's just config-file/GUI-only in this port, with no command or
  keybind to populate it.)
- **Update checker** - pinging Modrinth on the title screen for update notifications.
- **JourneyMap integration.**

## Changes from upstream behavior

- Only `MAP_DATA` relation detection is supported (pure Aechronis Nodes lookup). Upstream
  also had modes that parsed color codes out of nametag/tab-list prefixes; those aren't
  ported since Aechronis's nametag/tab convention isn't confirmed the way the map API is.
- The manual "Register Team" keybind and per-team-name friend/enemy mapping system was cut;
  only the plain friend/enemy player-name lists remain, edited via the config screen.
- **Bug fix**: upstream force-set every hitbox's alpha to a hardcoded `1.0` after computing
  the real per-relation alpha, so the alpha/transparency sliders in the config never
  actually reached the renderer. This port applies the real computed alpha instead.
- Dropped a no-op mixin wrapper and an unused, dead `BoxRenderUtil.drawBox()` method that
  never issued any actual draw calls in upstream - hitbox rendering runs entirely through
  vanilla's own debug-hitbox line renderer, recolored via Mixin; nothing in this mod builds
  its own vertex mesh.
- `towns.json` is polled every 2 minutes (upstream: 1 hour); `world.json` (needed for chunk
  borders, ~16MB) is polled every 10 minutes since land claims change far less often than
  town/nation membership.
- **Bug fix**: upstream's auto-`/t spawn`-on-respawn feature latched a "just respawned" flag
  that was never reset, so it only ever fired once per game session (the first death only).
  This port resets the flag on every death, so it fires on every respawn as intended.

## Building

Requires JDK 25. Uses Fabric Loom 1.17 / Gradle 9.7 / Fabric Loader 0.19.3 / Fabric API
0.157.0+26.2, targeting Minecraft 26.2.

```
./gradlew build
```

## License

GPLv3 (see `LICENSE`), matching the license on
[oreotrollturbo/Crusalis-utils-](https://github.com/oreotrollturbo/Crusalis-utils-)
(`master` branch). Credit to `oreotrollturbo` for CrusalisUtils, and to the original
author(s) of "Hitbox+" that CrusalisUtils itself was forked from.
