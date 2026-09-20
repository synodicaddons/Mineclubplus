# MineClub+

A LabyMod 4 addon that puts the MineClub transporter, economy and statistics behind one key.

Press **V** in game (rebindable, and only rebindable in the settings) to open the window.
`/mineclub` and `/mcp` do the same; `/mineclub refresh` forces a new request.

## Supported versions

Every Minecraft version LabyMod 4 ships a game-runner for, from `1.8.9` up to the current one —
the list in `net.labymod.minecraft-versions` in `gradle.properties`, declared to the store as
`1.8.9<*`.

The addon contains no version specific code, so all of them get the same classes. Adding a new
Minecraft version is a matter of appending it to that property and creating the matching
`game-runner/src/vX_Y_Z/resources/assets/mineclubplus/` folder with an empty `.assetsroot` file.

Items the running version does not have — copper on 1.8.9, for instance — render as a coloured
chip with a padlock, and the tooltip names the version that does have them. That comes from
LabyMod's own per-item version data, so it stays correct as versions are added.

## Build

```bash
./gradlew createReleaseJar
```

The installable addon ends up in `build/libs/mineclubplus-release.jar` — copy that one into
`%APPDATA%/.minecraft/labymod-neo/addons/`, not the 611-byte `mineclubplus-1.0.0.jar` next to it,
which is the empty root-project jar and has no `addon.json`.

Since LabyGradle 0.9.0, plain `./gradlew build` compiles everything but does **not** refresh the
release jar; `createReleaseJar` is the task that merges the modules into it.

To try it in a dev client: `./gradlew :game-runner:client_v1.21.1` (one task per supported
version).

## Data sources

### Live today

`GET https://api.mineclub.dk/v2/player/<uuid>` — the public player endpoint of `api-v2`. It needs
no token (its controller guard only demands one for handlers marked `@AdminPermission()`), and it
supplies the profile card and the sidebar:

| Window | Field from the endpoint |
|---|---|
| Name | `data.username` |
| Rank | `data.role` |
| Member since | year of `data.firstSeen` |
| Online / current server | `data.online`, `data.currentServer` |

The base URL is a setting, so a staging host is a one-field change.

### Not live yet

The transporter contents, the coin balance, market prices and mcMMO have **no endpoint**. Those
sections render sample numbers and the status bar says `TRANSPORTER + ØKONOMI: DEMODATA`, so
nobody mistakes them for real.

To make them real, add an endpoint that returns the shape in `api/model/MineClubSnapshot` and
point `MineClubApi#load` at it — that method is the only place that maps JSON onto the window.
The transporter data itself lives in the lobby plugin
(`dk.mineclub.lobbyplugin.transporter`, backed by `PlayerStorageData`), so the endpoint is a read
of that storage keyed by UUID. The pieces worth exposing:

```jsonc
{
  "transporter": { "items": [ { "id": "DIAMOND", "displayName": "Diamant",
                                "category": "mines",    // transporter.yml category, lower case
                                "amount": 384, "unitValue": 1240, "change24h": 6.2 } ] },
  "economy":     { "balance": 1248532, "balanceChange24h": 18420,
                   "netWorth": 3912740, "netWorthChange24h": 124300,
                   "transactions": [ { "timestamp": 1758300000000, "label": "...",
                                       "source": "shop", "amount": 79360 } ] },
  "skills":      [ { "id": "mining", "displayName": "Minedrift", "level": 742,
                     "experience": 184320, "experienceToNextLevel": 240000 } ]
}
```

The transporter is unlimited, so the shape carries no slot count: the window reports what is
in there, never how full it is.

`category` accepts the `transporter.yml` names — `mines`, `farming`, `wood`, `mobs`, `tools`,
`machinery`, `others` — and anything else lands in `others`. A transaction `amount` is signed:
positive is income, negative is spending.

## Transporter actions

The addon never moves items itself. Every action sends the matching sub-command from
`dk.mineclub.lobbyplugin.transporter.commands`, so the plugin stays the single authority over
inventories:

| Action | Command |
|---|---|
| Take to inventory | `/transporter get <MATERIAL> <amount>` |
| Store everything | `/transporter putall` |
| Open in game | `/transporter open` |
| Show contents in chat | `/transporter list` |
| Balance | `/balance` |

Material names are sent upper case, the way the plugin's suggestions expect them. The commands
live in `service/ServerCommands`.

## How it stays cheap

* One request per refresh, for the whole window.
* A 30 second TTL cache; a stale value keeps rendering while the refresh is in flight.
* Concurrent refreshes collapse into one request.
* Nothing polls in the background — the window fetches when it opens, on *Refresh*, and on an
  interval only while it is open and only if you turned that on.
* Rebuilds are batched: a click or an API response marks the window dirty and it rebuilds once on
  the next tick, never mid-frame.
* Listeners are registered on open and removed on close.

## Layout of the source

```
core/src/main/java/dk/mineclub/plus/core/
  MineClubPlusAddon          entry point, server detection
  MineClubPlusConfiguration  every setting
  api/                       HTTP client, models, demo data
  cache/CachedResource       TTL cache with single-flight and render-thread callbacks
  listener/                  hotkey, server join/switch
  service/                   the server commands and quick actions
  ui/Layout                  every breakpoint, measured on the window
  ui/                        the window, the pages and the shared widgets
  util/                      da-DK formatting, item colours, avatars
core/src/main/resources/assets/mineclubplus/
  themes/vanilla/lss/mineclubplus.lss   the entire look
  i18n/                                 da_dk and en_us
```
