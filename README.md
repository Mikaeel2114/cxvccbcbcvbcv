# ApexBan

Enterprise moderation core for **Bukkit / Spigot / Paper (1.8 - 1.21+)**, **BungeeCord** and **Velocity**.
Bans, mutes and kicks are stored in one database (SQLite or MySQL), checked asynchronously, and synchronised across every server that shares it.

## Modules

| Module     | Purpose                                                                  | Output jar                    |
|------------|--------------------------------------------------------------------------|-------------------------------|
| `core`     | Platform independent logic: commands, parser, storage, config, caches    | bundled into every jar        |
| `bukkit`   | Spigot / Paper adapter (1.8 - latest)                                    | `ApexBan-Bukkit-1.0.0.jar`    |
| `bungee`   | BungeeCord (and Waterfall) adapter                                       | `ApexBan-Bungee-1.0.0.jar`    |
| `velocity` | Velocity 3.x adapter (Java 17)                                           | `ApexBan-Velocity-1.0.0.jar`  |

## Commands

| Command                                  | Permission      | Notes                                                        |
|------------------------------------------|-----------------|--------------------------------------------------------------|
| `/ban <player> [duration] [reason...]`   | `apexban.ban`   | No duration = permanent ban. `7d`, `2h`, `30m`, `1d12h` ...  |
| `/mute <player> [duration] [reason...]`  | `apexban.mute`  | No duration = permanent mute                                 |
| `/kick <player> [reason...]`             | `apexban.kick`  | Player must be online                                        |
| `/unban <player>` (`/pardon`)            | `apexban.unban` |                                                              |
| `/unmute <player>`                       | `apexban.unmute`|                                                              |
| `/apexban [reload\|version\|help]`        | `apexban.admin` |                                                              |

Other permissions: `apexban.notify` (see announcements), `apexban.exempt` (cannot be punished).
Duration units: `s`, `m`, `h`, `d`, `w`, `mo`, `y`, combinable (`1d12h30m`). `perm` / `forever` are accepted as explicit permanent keywords.

## Build

Requires JDK 17.

```bash
gradle clean dist        # or ./gradlew clean dist after running `gradle wrapper` once
```

The three jars end up in `build/dist/`. Every push to GitHub builds them through `.github/workflows/build.yml`
and uploads them as workflow artifacts. Pushing a tag such as `v1.0.0` also creates a GitHub release.

## Network setup

1. Put the matching jar on every proxy and every backend server.
2. In each `plugins/ApexBan/config.yml` set `database.type: MYSQL` with the same credentials, and a **unique** `server-id`.
3. Restart. Punishments issued anywhere are enforced everywhere within `sync.interval-seconds` (default 15 s);
   bans are additionally checked live at every login.

SQLite is only meant for a single standalone server.

## Notes

- **Velocity / BungeeCord permissions**: neither proxy ships a permission system. Install LuckPerms (or equivalent) so staff holding `apexban.*` are recognised. The console always has access.
- **Muting on modern clients**: on Velocity, signed chat of 1.19.1+ clients can not always be denied by a proxy. Install the Bukkit jar on your backends as well; it cancels chat there with no such limitation.
- All database access runs on ApexBan's own worker pool; no query ever runs on the main/network thread.
- Configuration: `config.yml`, `messages.yml`, `layout.yml` (disconnect screens). Text supports MiniMessage and `&` codes. `/apexban reload` re-reads them (database settings need a restart).

## Project layout

```
ApexBan/
├── .github/workflows/build.yml
├── build.gradle.kts  settings.gradle.kts  gradle.properties
├── core/      (commands, config, model, service, storage, text, util, platform SPI)
├── bukkit/    (plugin.yml + adapter)
├── bungee/    (bungee.yml + adapter)
└── velocity/  (@Plugin adapter)
```

## License

MIT
