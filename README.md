# Voltex

Voltex is a tech mod for Minecraft 1.20.1 (Forge). It adds machines, FE energy generation and storage, cables and fluid handling, item and fluid logistics, a heat network, a multiblock miner, a multiblock nuclear reactor, a particle accelerator, a full line of solar panels, two armor sets and a number of optional machines.

Downloads, screenshots and the full description are on CurseForge:
https://www.curseforge.com/minecraft/mc-mods/voltex

## Dependencies

- Minecraft 1.20.1, Forge 47.x
- JEI (optional, recipe viewer)
- KubeJS (optional, recipe integration for pack makers)
- Jade (optional, machine status in the block tooltip)

The in-game guide is built into the mod and needs nothing extra.

## For pack makers

Machine recipes use the `industrial_core:machine_recipe` type with a `machine_type` field, plus `processingTime` and `energyCost`. Those two numbers are what JEI shows and what the machine uses, so changing them in the recipe is all that is needed to retune a machine.

Ore generation is defined in JSON under `data/industrial_core/worldgen` and can be overridden with a datapack. KubeJS integration lives in `compat/kubejs`, and `api/` holds the extension points for solar panel tiers and Restructor recipes.

## Building

Standard ForgeGradle. From the project root:

    ./gradlew build

The built jar is placed in `build/libs/`.

Jade is a compile-only dependency loaded from `libs/`, which is not part of this repository. Without it, remove the `compileOnly` line for Jade from `build.gradle` and delete `compat/jade`; nothing else depends on either.

## License

The source is published for viewing and learning. All rights reserved. See [LICENSE.txt](LICENSE.txt).

You may not redistribute, reupload or reuse the code, assets or textures in other projects without written permission. Including the mod in a modpack is fine when it is obtained from the official CurseForge page and credited. Bug reports and feedback are welcome.
