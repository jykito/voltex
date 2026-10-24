# Voltex API

Voltex 3.0.4 opens a set of extension points for add-ons. Everything below stays compatible within Voltex 3.x.
The API version is `com.jykito.industrialcore.api.VoltexApi.VERSION` (1 for Voltex 3.0.4).

This is the NeoForge 1.21.1 edition. The Forge 1.20.1 build of Voltex has the same API with the 1.20.1 calls.

Add Voltex as a required dependency in `neoforge.mods.toml` (`modId = "industrial_core"`, `type = "required"`,
`versionRange = "[3.0.4,4)"`) and compile against the Voltex jar.
Call the registration methods during mod setup (common side) unless noted otherwise.

## Open without any API

- **Recipes.** All Voltex machine recipe types load from data packs (`industrial_core:machine_recipe` with a
  `machine_type`, `industrial_core:assembling`, and the others). KubeJS schemas exist for all of them.
- **Restructor catalysts.** The catalyst slot accepts any `item.custom.CatalystItem`; a restructor recipe names its
  catalyst by item id in the `catalyst` field. An add-on registers its own `CatalystItem` and puts recipes in a data pack.
- **Heat.** `heat.HeatCapability.HEAT` is a sided `BlockCapability` for `heat.IHeatHandler`. Register it for your block
  entity type in `RegisterCapabilitiesEvent`, and Voltex heat pipes connect to the block:
  `event.registerBlockEntity(HeatCapability.HEAT, MY_BE.get(), (be, side) -> be.heat())`.
- **Guide chapters.** Each `assets/industrial_core/guide/<lang>/<file>.json` from any mod is a chapter of the Voltex guide
  panel; chapters go in file name order (Voltex uses `01_` to `11_`). Start the file name with a number and your mod id,
  for example `50_mymod_reactor.json`, so it never replaces a chapter of Voltex or another add-on. Put
  `"addon": "My Mod"` into the chapter: its tab gets a mark and that name, and the name heads the chapter's entry list. A chapter missing in the
  player's language is taken from `en_us`, so an English-only chapter is seen by everyone.
- **Energy, items, fluids.** Standard NeoForge capabilities (`Capabilities.EnergyStorage.BLOCK`, `ItemHandler.BLOCK`,
  `FluidHandler.BLOCK`), registered by the add-on for its own block entity types: Voltex cables, hubs and ports work
  with add-on blocks.

## Nanites: `api.VoltexNanites`

**New nanite classes**, the way add-ons bring new bee species. A class says what feed makes a strain of it, what that
feed selects for, how far and how fast the selection goes. Voltex breeds it in the evolution chamber and lists it in the
atlas; the strain works in the add-on's own machines.

```java
NaniteGenome.Kind WATCHER = VoltexNanites.kind(ResourceLocation.fromNamespaceAndPath("mymod", "watcher"))
        .cap(1.0f)                        // cap on the sum of affinities
        .step(0.03f, 0.04f)               // selection step, plus this much per unit of chamber heat
        .knowledge()                      // affinity reads as 0..100% instead of +%
        .keepsFeed()                      // the sample is not eaten
        .feed(item -> item == MyItems.CORE_SAMPLE.get() ? "core" : null)
        .targetName(t -> Component.translatable("mymod.watcher_target." + t))
        .atlas(() -> List.of("core"), t -> MyItems.CORE_SAMPLE.get())
        .register();
```

Lang keys of a class mirror the built-in ones in the add-on namespace, for example `nanite_species.mymod.watcher`,
`tooltip.mymod.nanite_strain.kind.watcher`, `tooltip.mymod.nanite_strain.hint.watcher`,
`tooltip.mymod.nanite_strain.effect.watcher` (`%s` = affinity in percent), `gui.mymod.atlas.how.watcher`.

**Scientist topics.** `VoltexNanites.scientistTopic("reactor", 0.6f, ResourceLocation.fromNamespaceAndPath("mymod", "data_card_reactor"), 0x9BE57A)`.
Samples go into the item tag `industrial_core:nanite_research/reactor`, the name into `nanite_theme.industrial_core.reactor`.
The card item is an add-on `DataCardItem("reactor", props)`.

**Rare traits and chamber surroundings.**

```java
ResourceLocation NEAR_REACTOR = ResourceLocation.fromNamespaceAndPath("mymod", "near_reactor");
VoltexNanites.environment(NEAR_REACTOR, (level, chamber) -> MyReactors.runningNear(level, chamber, 8));
VoltexNanites.trait(ResourceLocation.fromNamespaceAndPath("mymod", "radioresistant"), 0xFF9BE57A, (genome, kind, target, env, counters) -> {
    if (env.has(NEAR_REACTOR.toString())) counters.putInt("gens", counters.getInt("gens") + 1);
    return counters.getInt("gens") >= 20;
});
```

Names come from `nanite_trait.mymod.radioresistant` plus `.how`, `.hint` and `.effect` for the atlas page.
Check a strain with `NaniteGenome.read(stack).hasTrait("mymod:radioresistant")`; the effect lives in the add-on's machines.

## Multiblocks

**Structure wand.** Implement `api.WandAssemblable` on the core block; the wand calls `buildWithWand` and reports what is
missing. Place blocks through `WandBuild.place`.

**Machines on the late multiblock logic.** Extend `LateMachineCoreBlock` and `LateMachineCoreBlockEntity`:

- the block returns its own block entity type from `blockEntityType()` and `newBlockEntity`;
- the block entity calls `super(MY_TYPE.get(), pos, state)` and gets ports, batches, recipes, the core GUI and the wand for free.
- the core itself exposes no capabilities (energy and items go through the Voltex ports), so nothing is registered for
  its block entity type.

`LateMultiblockPattern` takes the schema layer by layer: `C` casing (or any port), `G` glass, `M` mechanism,
`K` core, `.` anything, and `P` socket (a port, or casing as a blank plug).

- `.withBlocks(MyBlocks.FRAME, null)` uses the add-on's own frame block instead of the Voltex casing.
- `.concealed()` marks a machine drawn by one model from the core, like the industrial miner. Ports then go into sockets
  only and are hidden when the structure forms (`MultiblockPortBlock.CONCEALED`): not drawn, but solid, clickable and
  working. The core shows them again when the structure breaks or the core is removed.
- Override `onFormedChanged(level, formed)` in the block entity to hide or show the add-on's own frame blocks.
- The renderer reads `isFormedForRender()` and `socketPorts()` (the port block id in each socket in schema order, or `""`)
  to draw a matching hatch for each port.

**Guide panel models (client).** `api.client.GuideModels.register("mymod:reactor", coreBlockId, (pose, buffers, ticks) -> ...)`
and use `"model": "mymod:reactor"` in the structure element of the add-on's guide chapter.

## Equipment and machines

**Power suit.** Implement `api.PowerSuitArmor` on armour drawn with `client.armor.PowerSuitModel.forSlot(slot)`:
Voltex hides the player's head under the helmet and draws the suit arm in first person. Energy shows in the armour HUD by
itself for any armour with the item energy capability (`Capabilities.EnergyStorage.ITEM`, registered by the add-on).

**Shared upgrades.** `VoltexUpgrades.allow(UpgradeTargets.Shared.ACCELERATOR, MyBlocks.PRESS)`: the machine's upgrade
slots check `UpgradeTargets.works`, the machine applies the effect itself (for example with `MachineUpgrades.scan`),
and the upgrade tooltip lists the machine.

**Wind turbine rotors.** Voltex has one rotor; any item can become another. It goes into the nacelle the same way, gets
a tooltip with its numbers and is drawn on the turbine with the Voltex blades, so no model is needed.

```java
VoltexWind.rotor(ResourceLocation.fromNamespaceAndPath("mymod", "steel_rotor"), 1.5f, 3f, 0xB0C4DE);
```

From a pack, in a KubeJS startup script (the item itself can be a KubeJS item):

```js
ICStartupEvents.windRotor(event => {
  event.register('kubejs:steel_rotor', { output: 1.5, radius: 3, tint: 0xB0C4DE })
})
```

- `output` multiplies the turbine output and its cap (the basic rotor 1, cap 500 FE/t). Clamped to 0.01..1000.
- `radius` is the blade length in blocks (the basic rotor 2.5). The free room the nacelle needs and the drawn blades grow
  with it: up to 2.5 needs 5×5, 3.5 needs 7×7 and so on. Clamped to 1..6.
- `tint` is an RGB colour over the blades; `texture` (optional) is a full texture path with the layout of
  `industrial_core:textures/block/wind_turbine.png` for own blade colours.

The base grows its buffer and output with the inflow, so strong rotors do not lose energy there.

**Covers.** A block that should hide inside a cover like Voltex cables adds the `Covers.COVERED` property, a block entity
implementing `CoverHolder`, and the `Covers` helpers for shape, light and mining. The client model wrapper and the tint
handler pick such blocks up by the property.
