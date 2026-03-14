# MahjongPaper

`MahjongPaper` is a Paper plugin rewrite scaffold for `MahjongCraft` that uses:

- Paper `ItemDisplay` and `TextDisplay`
- `ItemMeta#setItemModel(...)` with a matching resource pack
- PacketEvents entity interaction packets for tile clicking

## Status

This repository currently includes a playable Paper-based Riichi Mahjong port foundation:

- table creation and joining
- 4-seat riichi wall generation with red fives
- dealing, drawing and discarding
- riichi, tsumo, ron, chii, pon, minkan, ankan and kakan flows
- yaku / fu / han / score settlement through `mahjong4j`
- furiten checks, chankan, rinshan kaihou, suufon renda, suucha riichi, suukaikan and kyuushu kyuuhai
- nagashi mangan handling on exhaustive draw
- display-entity table rendering with hidden-information hands
- owner-only face-up hand rendering plus public face-down hand rendering
- PacketEvents click-to-discard interaction
- automatic settlement inventory UI and clickable reaction prompts
- pre-round rule configuration and lobby summaries
- local filler bots for unattended seats
- MiniMessage-based player messaging with `zh-CN` / fallback English localization
- locale-aware number formatting in command and settlement surfaces
- MariaDB or H2-backed round history persistence with startup schema creation

It is still not fully at feature parity with upstream `MahjongCraft`. The largest remaining gaps are long-match UX polish, richer per-player HUD hints, more refined board choreography, spectator/private packet sync beyond Bukkit visibility, and the original mod's broader non-plugin UX surface.

## Commands

- `/mahjong create`
- `/mahjong join <tableId>`
- `/mahjong list`
- `/mahjong addbot`
- `/mahjong removebot`
- `/mahjong rule [key] [value]`
- `/mahjong start`
- `/mahjong state`
- `/mahjong riichi <index>`
- `/mahjong tsumo`
- `/mahjong ron`
- `/mahjong pon`
- `/mahjong minkan`
- `/mahjong chii <tileA> <tileB>`
- `/mahjong kan <tile>`
- `/mahjong skip`
- `/mahjong kyuushu`
- `/mahjong settlement`
- `/mahjong render`
- `/mahjong clear`
- `/mahjong leave`

## Build

```powershell
.\gradlew.bat build
```

The plugin expects the PacketEvents plugin to be installed on the server because it is declared as a dependency in `plugin.yml`.
The project now also ships [`paper-plugin.yml`](./src/main/resources/paper-plugin.yml) for Paper-native plugin metadata and dependency loading; `plugin.yml` is still kept for command and permission registration.
Database settings live in [`config.yml`](./src/main/resources/config.yml); set `database.type` to `mariadb` or `h2`. Round settlements are written asynchronously to `round_history` and `round_player_result`.

## Resource Pack

The matching pack is in [`resourcepack`](./resourcepack).

For the current prototype the plugin uses the `mahjongcraft` namespace and binds tile displays to paths like:

- `mahjongcraft:mahjong_tile/m1`
- `mahjongcraft:mahjong_tile/east`
- `mahjongcraft:mahjong_tile/back`

## Upstream References

- `MahjongCraft`: gameplay and assets source
- `Paper`: display entities and `item_model` API
- `PacketEvents`: interaction packet bridge
