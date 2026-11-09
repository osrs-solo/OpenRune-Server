# Missing content: OpenRune vs OSRS Wiki

Compared on 2026-11-04. The wiki lists were fetched live from oldschool.runescape.wiki.
Boss and minigame gaps come from `PROGRESS.md`, whose wiki data is from 2026-11-01.
"Missing" means no module or script exists for it in `content/`. It doesn't mean
something that exists matches the wiki.

Rank each section, or pull items into one combined list.

---

## 1. Skills (4 missing, 1 partly there)

- [ ] **Hunter**: nothing yet
- [ ] **Farming**: shown as done in PROGRESS.md, but it's only mentioned in NPC dialogue in `content/areas/city`. No patch or growth system.
- [ ] **Construction**: shown as done in PROGRESS.md, but it's only mentioned in NPC dialogue. No player-owned house.
- [ ] **Sailing**: on the live wiki but not in the tracker. The cache is revision 241, so it probably needs a cache upgrade first.
- [~] **Agility**: the module now exists (merged in PR #227; PROGRESS.md is out of date). Courses are loaded from cache data, so check which of the wiki's 27 courses work in game.

Fletching is done now (13 source files). PROGRESS.md still lists it as missing.

## 2. Free-to-play quests (13 of 26 missing)

Done: Cook's Assistant, Doric's Quest, Ernest the Chicken, Imp Catcher, The Knight's Sword,
Misthalin Mystery, The Restless Ghost, Rune Mysteries, Sheep Shearer, Witch's Potion,
X Marks the Spot, The Ides of Milk

- [ ] Below Ice Mountain
- [ ] Black Knights' Fortress (the area module exists, the quest doesn't)
- [ ] The Corsair Curse
- [ ] Demon Slayer
- [ ] Dragon Slayer I
- [ ] Goblin Diplomacy
- [ ] Learning the Ropes
- [ ] Mudfoot's Big Adventure
- [ ] Pirate's Treasure
- [ ] Prince Ali Rescue
- [ ] Romeo & Juliet
- [ ] Shield of Arrav
- [ ] Vampyre Slayer

## 3. Members quests (166 missing)

Done: Gertrude's Cat, Waterfall Quest, Children of the Sun, Daddy's Home

A Kingdom Divided · A Night at the Theatre · A Porcine of Interest · A Ruff Situation · A Soul's Bane ·
A Tail of Two Cats · A Taste of Hope · An Existential Crisis · Animal Magnetism · Another Slice of H.A.M. ·
At First Light · Beneath Cursed Sands · Between a Rock... · Big Chompy Bird Hunting · Biohazard ·
Bone Voyage · Burial at Sea · Cabin Fever · Client of Kourend · Clock Tower · Cold War · Contact! ·
Crab Quest · Creature of Fenkenstrain · Current Affairs · Darkness of Hallowvale · Death Plateau ·
Death on the Isle · Death to the Dorgeshuun · Defender of Varrock · Desert Treasure I ·
Desert Treasure II - The Fallen Empire · Devious Minds · Dragon Slayer II · Dream Mentor ·
Druidic Ritual · Dwarf Cannon · Eadgar's Ruse · Eagles' Peak · Elemental Workshop I ·
Elemental Workshop II · Enakhra's Lament · Enlightened Journey · Ethically Acquired Antiquities ·
Fairytale I - Growing Pains · Fairytale II - Cure a Queen · Fairytale III - Nexus of Power ·
Fallen From Grace · Family Crest · Fight Arena · Fishing Contest · Fool's Gold · Forgettable Tale... ·
Garden of Tranquillity · Getting Ahead · Ghosts Ahoy · Grim Tales · Haunted Mine · Hazeel Cult ·
Heroes' Quest · Holy Grail · Horror from the Deep · Icthlarin's Little Helper · Impending Chaos ·
In Aid of the Myreque · In Search of the Myreque · Jungle Potion · King's Ransom · Land of the Goblins ·
Legends' Quest · Lost City · Lunar Diplomacy · Making Friends with My Arm · Making History ·
Meat and Greet · Merlin's Crystal · Monk's Friend · Monkey Madness I · Monkey Madness II ·
Mountain Daughter · Mourning's End Part I · Mourning's End Part II · Murder Mystery ·
My Arm's Big Adventure · Nature Spirit · Observatory Quest · Olaf's Quest · One Small Favour ·
Pandemonium · Perilous Moons · Plague City · Priest in Peril · Prying Times · Rag and Bone Man I ·
Rag and Bone Man II · Ratcatchers · Recipe for Disaster · Recruitment Drive · Regicide · Rocking Out ·
Roving Elves · Royal Trouble · Rum Deal · Scorpion Catcher · Scrambled! · Sea Slug · Secrets of the North ·
Shades of Mort'ton · Shadow of the Storm · Shadows of Custodia · Sheep Herder · Shilo Village ·
Sins of the Father · Sleeping Giants · Song of the Elves · Spirits of the Elid · Swan Song ·
Tai Bwo Wannai Trio · Tale of the Righteous · Tears of Guthix · Temple of Ikov · Temple of the Eye ·
The Ascent of Arceuus · The Blood Moon Rises · The Chosen Commander · The Curse of Arrav ·
The Depths of Despair · The Dig Site · The Eyes of Glouphrie · The Feud · The Final Dawn ·
The Forsaken Tower · The Fremennik Exiles · The Fremennik Isles · The Fremennik Trials ·
The Garden of Death · The Giant Dwarf · The Golem · The Grand Tree · The Graveyard ·
The Great Brain Robbery · The Hand in the Sand · The Heart of Darkness · The Lost Tribe ·
The Path of Glouphrie · The Queen of Thieves · The Red Reef · The Ribbiting Tale of a Lily Pad Labour Dispute ·
The Slug Menace · The Tourist Trap · The Writing's on the Wall · Throne of Miscellania · Tower of Life ·
Tree Gnome Village · Tribal Totem · Troll Romance · Troll Stronghold · Troubled Tortugans ·
Twilight's Promise · Underground Pass · Wanted! · Watchtower · What Lies Below · While Guthix Sleeps ·
Witch's House · Zogre Flesh Eaters

## 4. Miniquests (25 missing)

Done: His Faithful Servants

Alfred Grimhand's Barcrawl · Architectural Alliance · Barbarian Training · Bear Your Soul ·
Curse of the Empty Lord · Enter the Abyss · Family Pest · Hopespear's Will · In Search of Knowledge ·
Into the Tombs · Knight Waves Training Ground · Lair of Tarn Razorlor · Lost Lover · Mage Arena I ·
Mage Arena II · Rogue Trader · Sacred Bone Burner · Skippy and the Mogres · Spirit of Adventure ·
The Enchanted Key · The Fractured Archive · The Frozen Door · The General's Shadow ·
Tutorial Island · Vale Totems

## 5. Raids (4 of 4 missing)

- [ ] Chambers of Xeric
- [ ] Theatre of Blood
- [ ] Tombs of Amascut
- [ ] The Fractured Archive

## 6. Bosses (154 missing, plus 2 marked done by mistake)

PROGRESS.md marks **Kraken** and **Vanguard** as done, but the only code for them is the pet code
in `content/other/pets`. Treat both as missing.

**Drop table already written, only the fight needs scripting (74).** These are the quickest wins:
Abyssal Sire · Ahrim the Blighted · Akkha · Alchemical Hydra · Araxxor · Arrg · Ba-Ba · Black Knight Titan ·
Branda the Fire Queen · Calvar'ion · Cerberus · Chaos Elemental · Chaos Fanatic · Chronozon ·
Corporeal Beast · Crazy archaeologist · Culinaromancer · Dad · Dagannoth Prime · Dagannoth Rex ·
Dagannoth Supreme · Dagannoth mother · Damis · Deranged archaeologist · Dharok the Wretched ·
Eldric the Ice King · Elidinis' Warden · Giant Mole · Giant Roc · Giant Scarab · Giant Sea Snake · Glod ·
Guthan the Infested · Hespori · Ice demon · Kalphite Queen · Kamil · Karil the Tainted · Kephri ·
Melzar the Mad · Moss Guardian · Muttadile · Nex · Nylocas Vasilias · Pestilent Bloat ·
Phosani's Nightmare · Salarin the twisted · Sarachnis · Scorpia · Shellbane gryphon · Skotizo ·
Slagilith · Slash Bash · Sotetseg · Tekton · The Hueycoatl · The Maiden of Sugadinti · The Nightmare ·
Thermonuclear smoke devil · Torag the Corrupted · Treus Dayth · Tumeken's Warden · TzTok-Jad ·
Vasa Nistirio · Verac the Defiled · Verzik Vitur · Vespula · Vet'ion · Vorkath · Xarpus · Yama ·
Zalcano · Zebak · Zulrah

(The Barrows brothers on this list probably come with the existing Barrows module. Check before counting them as separate work.)

**Nothing yet (80):**
Agrith Naar · Agrith-Na-Na · Archive Guardian · Arzinian Avatars/Being · Barrelchest · Black demon ·
Black/Grey/White golem · Blood/Blue/Eclipse Moon (Moons of Peril) · Bork · Bouncer · Bryophyta ·
Count Draynor · Dagon'hai Elder/Monk · Dawn & Dusk (Grotesque Guardians) · Delrith · Dessourt · Dessous ·
Dreadnoth the Jellied · Elvarg · Evil spirit · Ezekiel · Fareed · Flambeed · Gadderanks · Galvek ·
Gelatinnoth Mother · General Khazard · Glough · Great Olm · Ice Troll King · Judge of Yama ·
Jungle Demon · Karamel · Koschei the deathless · Lowerniel Drakan · Mad Angel · Maggot King · Me ·
Moia · Nezikchened · Obor · Ocellus · Penance Queen · Revenant maledictus · Royal Titans ·
Sea Troll Queen · Sigmund · Sir Leye · Sir Mordred · Slug Prince · Sol Heredit · Tarn · Tempoross ·
The Draugen · The Everlasting · The Illusive · The Inadequacy · The Mimic · The Smorks ·
The Untouchable · Tolna · Tree spirit · TzKal-Zuk · Ulfric · Wintertodt · Wrathmaw · Xamphur · Xarth

Most of these are quest bosses and come with their quests (Elvarg with Dragon Slayer I, Delrith with Demon Slayer, and so on).
Standalone ones worth flagging: **Obor and Bryophyta** (F2P), **Wintertodt and Tempoross** (skilling bosses),
Grotesque Guardians, Royal Titans, Moons of Peril, TzKal-Zuk (Inferno), Sol Heredit (Colosseum).

## 7. Minigames (44 of 46 missing)

Done: Giants' Foundry, The Gauntlet

Barbarian Assault · Barracuda Trials · Blast Furnace · Bounty Hunter · Brimhaven Agility Arena ·
Burthorpe Games Room · Castle Wars · Clan Wars · Duel Arena · Emir's Arena · Fishing Trawler ·
Fortis Colosseum · Games Zone · Gnome Ball · Gnome Restaurant · Guardians of the Rift ·
Hallowed Sepulchre · Impetuous Impulses · Inferno · Intelligence Gathering · Last Man Standing ·
Mage Arena · Mage Training Arena · Mahogany Homes · Mastering Mixology · Mess · Nightmare Zone ·
Pest Control · Pyramid Plunder · Rat Pits · Rogues' Den · Shades of Mort'ton · Sorceress's Garden ·
Soul Wars · Tai Bwo Wannai Cleanup · Tears of Guthix · Temple Trekking · Tithe Farm · Trouble Brewing ·
TzHaar Fight Cave · TzHaar Fight Pit · TzHaar-Ket-Rak's Challenges · Vale Totems · Volcanic Mine

## 8. Core systems (keyword search only, confirm before ranking)

The tracker doesn't cover these. A keyword search of `content/` and `api/` found no system for them:

- [x] **Grand Exchange**: `content/other/grand-exchange`; see `docs/grand-exchange.md`. Item sets, buy limits and the tax are not done.
- [ ] **Clue scrolls / Treasure Trails**: clue drops exist (`content/drops`), but there's no way to solve the steps.
- [ ] **Achievement Diaries**: nothing found.
- [ ] **Fairy rings**: nothing found.
- [ ] **Gnome gliders**: only the pilot NPC exists, no travel network.
- [ ] **Combat Achievements**: only a few references.
- [ ] **Random events**: only the Genie is done (the wiki has about 30).
