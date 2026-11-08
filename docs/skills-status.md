# Skills status: what works and what's missing

Checked 2026-11-08 on `main` (b54812b5d). This compares each skill with the main training methods
listed on the OSRS wiki.

**How this was checked.** Each skill's code was read to see what it handles: which items, rocks,
spots and NPCs it reacts to, and which cache tables it reads. Every skill module's tests were run.
**It was not play-tested.** The game client was running but sat at the login screen, so the
in-client tools couldn't act. "Works" here means the code path exists and its tests pass.
Treat it as "should work" until a live pass confirms it.

`PROGRESS.md` counts modules, not features, and marks Farming and Construction as done when they
aren't. Use this file instead.

## Summary

| Skill | Status | Biggest gaps | Upstream PR that would fill it |
|---|---|---|---|
| Attack / Strength / Defence / Hitpoints | ✅ Works | - | #313 (special attacks, polish) |
| Ranged | ✅ Works | Dwarf multicannon | - |
| Prayer | ✅ Works | POH gilded altar (needs Construction) | - |
| Magic | 🟡 Partly | Enchant, Superheat, Bones to Bananas/Peaches, Telegrab, Charge orb, whole Lunar spellbook | - |
| Cooking | ✅ Works | Gnome cooking, potato toppings | - |
| Woodcutting | ✅ Works | Forestry events | - |
| Fletching | ✅ Works | - | (#262, #418 already merged) |
| Fishing | ✅ Works | Tempoross, aerial, drift net, sacred eels, Fishing Trawler | - |
| Firemaking | ✅ Works | Wintertodt | #458 (draft) |
| Crafting | ✅ Works | - | - |
| Smithing | ✅ Works | Blast Furnace, Superheat Item | - |
| Mining | ✅ Works | Volcanic Mine, Zalcano, dense essence, Blast mine | #453 (Motherlode fixes) |
| Herblore | ✅ Works | Mastering Mixology | - |
| Agility | 🟡 Mostly | Agility Pyramid, Penguin, Brimhaven Arena, Hallowed Sepulchre, Dorgesh-Kaan | (#227 already merged) |
| Thieving | 🟡 Partly | 27 of ~30 pickpocket targets, most stalls, all chests, blackjacking | **#310** |
| Slayer | ✅ Works | - | - |
| Runecraft | 🟡 Partly | Abyss, blood and soul runes (dense essence), Guardians of the Rift | - |
| Farming | ❌ Missing | Everything | **#440** (ready), #405 (draft) |
| Hunter | ❌ Missing | Everything | **#408** (draft) |
| Construction | ❌ Missing | Everything | **#228** |
| Sailing (24th skill) | ❌ Missing | Everything | #182 (draft, helm only) |

## Test results

Every skill module with tests was run (`test`, plus `integration` where the module has it).

| Module | Tests | Result |
|---|---|---|
| agility (+ pack) | 21 | pass |
| crafting | 19 | pass |
| fishing (unit + integration) | 9 | pass |
| fletching | 6 | pass |
| herblore | 6 | pass |
| magic: spell-attacks / arceuus | 99 + | pass |
| magic: rune-pouch | 26 | pass |
| **magic: spell-teleports** | 11 | **1 fails** (see below) |
| prayer | 4 | pass |
| slayer (unit + integration) | 21 | pass |
| smithing | 3 | pass |
| thieving | 6 | pass |
| woodcutting | 5 | pass |
| cooking, firemaking, mining, runecrafting | 0 | no tests exist |

Problems the test run turned up:

1. **`PouchRuneTeleportTest > a teleport paid entirely from a divine pouch casts` fails.** After the
   cast and 10 ticks the player is still where they started, not in Varrock. The pouch-paid
   teleport doesn't fire, or the recent longer cast (b54812b5d / 32bc81a78) now takes more than
   10 ticks. Worth a quick look, since the divine pouch is recent work.
2. **Woodcutting's integration tests never run.** `content/skills/woodcutting/src/integration`
   holds `WoodcuttingConfigTest` and `WoodcuttingScriptTest`, but the module's
   `build.gradle.kts` doesn't apply `integration-test-suite`, so Gradle has no task for them.
3. Running every suite in parallel on this machine ran out of memory ("paging file is too small")
   while IntelliJ and the server were running. Run them with `--max-workers=1`, or a few at a time.

## Per skill

### Combat: Attack, Strength, Defence, Hitpoints, Ranged — ✅ works

`api/combat` (formulas, manager, PvN/PvP/NvP scripts, weapons) is RSMod's mature core.
`content/other/special-attacks` and `special-weapons` add specs. Accuracy and max-hit maths have
their own tests in `api/combat`.

Gaps:
- **Dwarf multicannon**: Nulodion exists and cannonballs can be smithed, but you can't set up or
  fire a cannon.
- Chinchompas: the fuse accuracy modifier is in, but splash damage across several targets wasn't
  confirmed. Check it in game.

### Prayer — ✅ works

Burying bones and scattering ashes, offering on altars, the Ectofuntus (grinder, bonemeal, slime,
worship), the Chaos Temple altar in the Wilderness (gilded-altar rules), Varlamore blessed bone
shards, the libation bowl and sunfire wine, plus bonecrusher, bonesack, ash sanctifier, zealot robes
and the Catacombs prayer restore.

Gap: the **POH gilded altar**, the fastest normal method, needs Construction.

### Magic — 🟡 partly works

Works: standard elemental combat spells, all Ancient Magicks, Arceuus combat spells (Grasp,
Demonbane, Dark Lure), all teleports and tablets, home and minigame teleports, High and Low Alchemy,
the Arceuus utility spells (reanimation, offerings, Degrime, Shadow Veil, Ward, Vile Vigour,
Death Charge, Mark of Darkness, corruption), the Ancient altar and rune pouches.

Missing (none of these spells do anything yet):
- **Enchant jewellery / bolts.** Jagex's `magic_enchant` dbtable is already in the cache
  (`MagicEnchantRow`) and nothing reads it, which makes this the cheapest big win.
- **Superheat Item.** `SmeltingScript` already has an `isSuperHeat` flag, but no spell calls it.
- Bones to Bananas / Peaches, Telekinetic Grab, Charge Water/Earth/Fire/Air Orb.
- **The whole Lunar spellbook.** There's no Lunar altar, and none of its utility spells (Plank
  Make, String Jewellery, Humidify, Spin Flax, NPC Contact, Bake Pie) exist.
- Magic Training Arena.

### Cooking — ✅ works

Driven by Jagex's own cooking dbtables: all the normal raw fish, meat and bread on fires and ranges
(with burn chances and the Cooking gauntlets / Hosidius surface bonuses), plus pies, pizza, cakes,
stew, dough, wine and ales. The Cooking Guild door and head chef are in.

Gaps: gnome cooking (battas, crunchies, gnomebowls) and potato toppings. Neither is a main
training method.

### Woodcutting — ✅ works

Every tree that has woodcutting parameters in the cache can be chopped: normal to magic, redwood
and the rest. Axes and level boosts, bird nests and other side drops, and both random and timed
tree depletion are in.

Gaps: **Forestry** events (the `GatheringEvent*` tables are in the cache but nothing uses them).
The integration tests don't run (see Test results).

### Fletching — ✅ works

Merged from upstream #262 plus celastrus bark (#418): cutting, stringing, arrows, bolts, darts,
gem tips, assembly and blowpipe crafting, all read from cache tables.

### Fishing — ✅ works

Net/bait, lure/bait, cage/harpoon, big net/harpoon, barbarian rod, monkfish, karambwanji,
karambwan, minnows (Kylie and the rowboat), anglerfish, dark crab, lava and infernal eels, cave and
slimy eels. Also the tackle box, fish barrel, infernal harpoon, angler outfit and level boosts.

Missing: **Tempoross** (a skilling boss and one of the main training methods), aerial fishing
(Molch), drift net fishing, sacred eels (Zul-Andra), Fishing Trawler.

### Firemaking — ✅ works

Every log in Jagex's `firemaking_logs` table, coloured logs via firelighters, Forestry campfires
(lighting and tending them), light sources, and the pyromancer outfit's xp bonus.

Missing: **Wintertodt**, the main mid-level method. Upstream #458 (draft) adds it.

### Crafting — ✅ works

Around 240 recipe rows: leather and dragonhide, spinning, weaving, pottery, glassblowing, gold and
silver jewellery, gem cutting, battlestaves, birdhouses, drift nets, the tanner, the sandpit, and
the Crafting Guild with its bank and door.

No real gaps for normal training.

### Smithing — ✅ works

Smelting at furnaces (including Lovakengj), anvil smithing for every bar, cannonballs, the dragon
forge, Prifddinas crystal singing, godsword assembly, the coal bag, smiths' uniform xp, and Giants'
Foundry (in `content/minigames`).

Missing: **Blast Furnace** (the main high-level method), Superheat Item (see Magic).

### Mining — ✅ works

Every rock in Jagex's `mining_rocks` dbtable: ores, gem rocks, amethyst, sandstone and granite.
Pickaxes and boosts, prospector xp, gem side drops, Motherlode Mine
(`content/areas/misc/motherlode-mine`), the Mining Guild, shooting stars, and Blessed sand
(`other/sandstorm`).

Missing: Volcanic Mine, Zalcano, Blast mine, and **dense essence** (Arceuus), which blood and soul
runecrafting also need. Upstream #453 brings Motherlode Mine closer to OSRS.

### Herblore — ✅ works

All six Jagex herblore tables: cleaning herbs, unfinished and finished potions, crushing, swamp
tar, barbarian mixes. Also the amulet of chemistry and the herblore goggles.

Missing: Mastering Mixology (minigame). Not needed for normal training.

### Agility — 🟡 mostly works

19 courses: the Gnome Stronghold, Draynor, Al Kharid, Varrock, Canifis, Falador, Seers', Pollnivneach,
Rellekka and Ardougne rooftops, Barbarian Outpost, Ape Atoll, Wilderness, Werewolf, Prifddinas,
Shayzien basic and advanced, and Colossal Wyrm basic and advanced. Also marks of grace and about
3,600 lines of shortcut data. `docs/agility-verification.md` has an in-client pass of six courses.
Ape Atoll still hasn't been run in game.

Missing: **Agility Pyramid** (the usual level 30-50 method), Penguin course, Brimhaven Agility
Arena, Hallowed Sepulchre, Dorgesh-Kaan.

### Thieving — 🟡 partly works

`ThievingScript` + `ThievingTables` only cover:
- stalls: the Draynor seed stall and wine stall;
- pickpocketing: men and women, Al Kharid warriors, Master Farmers (with coin pouches).

Missing: every other stall (Ardougne bakery, silk, fur, silver, spice and gem stalls, the tea
stall, Hosidius fruit stalls), every other
pickpocket target (farmers, H.A.M., guards, Knights of Ardougne, paladins, heroes, elves, vyres,
TzHaar…), all chests (Jagex's `ThievingChestRow` is in the cache and nothing reads it), Varlamore
house thieving (`VarlamoreThievingHouseRow`, likewise unused), blackjacking, Pyramid Plunder,
Rogues' Den, Sorceress's Garden. The loot is also hardcoded in Kotlin, which `AGENTS.md` says
belongs in cache tables.

**Upstream #310** fixes most of this: 29 pickpocket targets, stalls, data moved into dbtables,
loot through the shared drop-table system. It's marked "dirty" (it has merge conflicts with main)
and hasn't been updated since 2026-10-13.

### Slayer — ✅ works

The largest skill module (52 files): every main slayer master (Turael/Aya through Duradel and
Nieve/Steve, Konar with her areas, Krystilia, Mortimer, Spria), assigning and rolling tasks,
streak points, the rewards shop with unlocks and blocks, boss tasks, superior monsters, finishing
items (salt, ice coolers and so on), equipment requirements, the enchanted gem, and the Bracelet
of Slaughter / Expeditious Bracelet.

### Runecraft — 🟡 partly works

Works: every normal altar through the ruins (talisman or tiara), combination runes, the Ourania
(ZMI) altar, Daeyalt essence, the Rune Essence mine teleport, essence pouches up to the colossal
pouch plus repairs (Dark Mage and Cordelia), tiaras, Magic Imbue, the binding necklace, blood
essence, Raiments of the Eye, and aether runes at the soul altar.

Missing:
- **The Abyss**, the classic fast route to every altar.
- **Blood and soul runes from dense essence**: mining dense essence, venerating it at the Dark
  Altar, and chiselling it into fragments. This is the AFK high-level method.
- **Guardians of the Rift**, the main training minigame since 2022.

### Farming — ❌ missing

No patches, growth, compost or tools. All that exists is farming-related NPC dialogue and seeds
dropped by the seed stall and Master Farmers.

- **Upstream #440** — allotments, flowers and herbs on the normal patches: weeds, compost,
  planting, watering, plant cure, growth on the farming clock (including while logged off),
  disease and death, harvesting. It's "clean" (no conflicts) and isn't a draft, so it's the
  easiest merge on this list.
- Upstream #405 (draft, 23k lines) — the full skill (trees, fruit trees, hops, bushes, special
  patches). Pieces of it are being split out into PRs like #440.

### Hunter — ❌ missing

Nothing in `content/`. **Upstream #408** (draft, 16.7k lines, updated today) adds bird snares, box
traps and chinchompas, magic boxes, rabbit snares, net traps (all five salamanders), deadfalls and
more, with its data in dbtables.

### Construction — ❌ missing

No player-owned house. The cache already has the house tables (`PohRoom`, `PohHotspot`,
`Furniture`). Only Daddy's Home uses `FurnitureRow`, to build a few fixed pieces.
**Upstream #228** adds a buildable house: rooms from doorways, furniture from hotspots, removal,
portals, the portal nexus and the restore pool. It's "unstable" (CI not green) and not a draft.

### Sailing (24th skill) — ❌ missing

`docs/missing-content.md` guessed this needed a cache upgrade. It doesn't: the revision 241 cache
already has more than 60 `sailing_*` dbtables (boats, facilities, shoals, charting, seas) and the
client shows the skill. Nothing in `content/` uses them. Upstream #182 (draft) only covers the
helm. This is a large system and should be the last thing tackled.

## Recommended order

Cheapest and most useful first:

1. **Fix the failing divine-pouch teleport test**, and turn on woodcutting's integration tests (one
   line in its `build.gradle.kts`). Both are small, and they protect work you just merged.
2. **Farming: merge upstream #440.** It has no conflicts and isn't a draft, and it covers the
   core of Farming (herbs and allotments). Hespori and other farming content depend on it.
3. **Thieving: merge upstream #310.** It turns a token skill into a real one, but expect merge
   conflicts with your current `ThievingScript`.
4. **Magic utility spells**, self-contained work in this repo: Enchant (its cache table already
   exists), Superheat (half-wired), Bones to Bananas/Peaches, Telegrab, Charge orbs. Then a Lunar
   altar plus the Lunar utility spells.
5. **Construction: merge upstream #228.** It also enables the gilded altar for Prayer and a house
   portal for travel.
6. **Hunter: merge upstream #408** once its author takes it out of draft. It's the biggest of the
   PRs, so wait until it settles.
7. **Runecraft:** dense essence (Mining) → Dark Altar → blood and soul runes; then the Abyss; then
   Guardians of the Rift.
8. **Agility:** run Ape Atoll in game, then add the Agility Pyramid, then Penguin, Brimhaven and
   Sepulchre.
9. **Skilling bosses and minigames:** Wintertodt (#458), Tempoross, Blast Furnace, Motherlode
   fixes (#453), Volcanic Mine.
10. **Sailing** last.

A live pass in the client would confirm the "works" rows. For each skill: set the level, spawn the
tools, do one action and check the xp.
