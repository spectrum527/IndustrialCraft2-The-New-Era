# IndustrialCraft²: The New Era 1.0.0 player guide

[简体中文](first-playable.md) · **English**

## Installing the mod

Pick the loader build that matches your Minecraft version, put the same JAR into `mods/` on both the client and the dedicated server, and start a new world. Single-player only needs the client JAR. The original IC2, the older repair project, and this rebuild all use the `ic2` mod ID, so give each one its own instance and save folder.

| Minecraft | Tested loader | Java | File |
| --- | --- | ---: | --- |
| 1.19.2 | Forge 43.5.0 | 17 | `ic2-forge-1.19.2-1.0.0.jar` |
| 1.21.1 | NeoForge 21.1.265 | 21 | `ic2-neoforge-1.21.1-1.0.0.jar` |
| 26.1.2 | NeoForge 26.1.2.128 | 25 | `ic2-neoforge-26.1.2-1.0.0.jar` |
| 26.2 | NeoForge 26.2.0.95 | 25 | `ic2-neoforge-26.2-1.0.0.jar` |

Two optional mods fit well alongside IC2. The One Probe displays a machine's stored EU and tank fluid levels when you look at it, which makes wiring and pipe checks faster. Curios adds a back slot where you can wear a battery pack or electric jetpack without giving up your chestplate, and those packs still charge when you stand on a matching-tier charge pad.

You can check individual crafting grids in the in-game recipe book or on MC百科. Machine parameters, formulas, and recipes here follow public MC百科 documentation and were checked against running builds for each version. [MC百科 IndustrialCraft² page](https://www.mcmod.cn/class/2.html) · [Macerator recipes](https://www.mcmod.cn/item/241.html)

All machine timers run in server game ticks. On an unlagged server, 20 ticks equal 1 second; server lag stretches real wait times.

## Starting out: rubber, hand tools, and your first power line

### Finding rubber trees and early ores

Start by looking through forests and swamps for rubber trees. They grow slightly narrower than oaks and often have a short spire of leaves on top. Look for orange resin spots along the trunk.

Craft a Treetap from 5 wooden planks in a faucet shape, then right-click the orange resin spots to collect Sticky Resin. Leave a few rubber trees standing near your base so you can tap them again later, and replant saplings in a small grove. You will go through a lot of rubber.

Smelt Sticky Resin in a vanilla furnace for 1 Rubber per resin while you are getting started. Once you build an Extractor, 1 Sticky Resin gives 3 Rubber.

When you go mining, gather iron, copper, coal, redstone, and flint along with the mod's Tin Ore and Lead Ore. Iron, copper, tin, and rubber are your main early bottlenecks. If you find Uranium Ore deep underground, store it in a chest for later; processed nuclear fuel causes radiation damage if you carry it loose in your inventory.

### Hand tools and insulated cables

Early IC2 parts rely on two hand tools in a crafting grid. Each craft uses 1 durability.

The Forge Hammer (5 iron ingots and 2 sticks) hammers metal ingots into plates (1 ingot gives 1 plate) and plates into item casings (1 plate gives 2 casings, such as 1 Tin Plate into 2 Tin Item Casings). The Cutter (3 iron plates and 2 iron ingots) cuts 1 Copper Plate into 2 Copper Cables or 1 Tin Plate into 3 Tin Cables.

Bare live copper and tin cables shock any player or mob inside a 3x3x3 area around the wire. Craft bare cable with Rubber in any grid slot to make Insulated Copper Cable or Insulated Tin Cable. One layer of rubber stops shocks on copper and tin cables, and insulation adds zero extra energy loss.

If hand-crafting plates and wires gets tedious, craft an Industrial Workbench (1 crafting table, 1 Forge Hammer, 1 Basic Machine Casing, and 1 Cutter). Its top slots hold a Forge Hammer and Cutter, the lower section stores 18 stacks of materials, and the red X button returns items from the 3x3 grid to your inventory.

### Building your first Generator, BatBox, and Macerator

Craft these five pieces in order to get your first ore-doubling line running.

1. RE-Battery: 1 Insulated Tin Cable, 4 Tin Item Casings, and 2 redstone.
2. Iron Furnace: 5 Iron Plates around 1 vanilla furnace. It smelts faster than a stone furnace and is needed to craft a Generator.
3. Generator: 1 RE-Battery, 3 Iron Plates, and 1 Iron Furnace. One piece of coal burns for 400 ticks (20 seconds) at 10 EU/t, producing 4,000 EU total. Its internal buffer holds 4,000 EU and all six faces output power. It rejects lava, and once a piece of coal starts burning, it keeps burning even after the buffer fills up, so any extra EU is lost.
4. BatBox: 5 wooden planks, 1 Insulated Tin Cable, and 3 RE-Batteries. Its 40,000 EU buffer catches surplus power from the Generator. Watch the facing: the single side with a dot marking is the only output face, while the other five sides accept input. When you place a BatBox, the output face points toward you.
5. Basic Machine Casing (8 Iron Plates in a ring) and Electronic Circuit (6 Insulated Copper Cables, 2 redstone, and 1 Iron Plate), combined with 3 flint and 2 cobblestone to craft your first Macerator.

Wire them up like this:

```text
[Generator] -- Insulated Copper Cable --> [BatBox] (dotted output face) -- Insulated Copper Cable --> [Macerator]
                                                                                                           |
                                                                                                    [Hopper / Chest]
```

Load coal into the Generator and place 1 raw ore (raw iron, raw copper, raw tin, raw gold, or raw lead) into the Macerator. Running steadily at 2 EU/t for 300 ticks (15 seconds, 600 EU total), the Macerator outputs 2 Crushed Ore. Smelt both crushed ores into 2 ingots to double your yield. Coal macerates 1-to-1 into Coal Dust (also 600 EU), which you will need later for carbon plates and industrial diamonds.

If power cuts out mid-job, the Macerator, Electric Furnace, Extractor, and Compressor pause without losing paid progress and resume once power returns. Leave space in the output slot; when the full batch cannot fit, the machine waits without consuming input or EU.

### Craft a Wrench first, then finish the starter quartet

Make a Wrench before you move any machine. Breaking a basic IC2 machine with a pickaxe drops only a Basic Machine Casing, so you lose the circuit and the rest of the machine.

A Wrench takes 6 Bronze Ingots. Run raw copper and raw tin through your new Macerator, then combine 3 Crushed Copper Ore with 1 Crushed Tin Ore in any crafting grid to make 4 Bronze Dust. Smelt the dust into Bronze Ingots.

The Wrench has two actions. Right-clicking a face of a machine, storage block, transformer, or item pipe turns its front or output face to that side. Sneak-right-clicking dismantles the block intact. A wrenched storage block keeps 80% of its stored EU by default; regular machines drop their inventory separately without keeping energy or progress. Later, an Electric Wrench uses EU and never loses tool durability.

Once the Macerator and Wrench are ready, add the other three starter LV machines directly behind your BatBox.

- Extractor (4 Treetaps, 1 Basic Machine Casing, 1 Electronic Circuit; 2 EU/t, 300 ticks): turns 1 Sticky Resin into 3 Rubber, and converts Rubber Wood or Rubber Tree Saplings into 1 Rubber each. Build this early so rubber stops slowing you down.
- Electric Furnace (1 Electronic Circuit, 2 redstone, 1 Iron Furnace; 3 EU/t, 100 ticks, 300 EU total): smelts an item in 5 seconds (twice as fast as a vanilla furnace) on electricity, with no coal needed.
- Compressor (6 stone, 1 Basic Machine Casing, 1 Electronic Circuit; 2 EU/t, 300 ticks): turns Empty Cells into Compressed Air Cells for steelmaking, compresses 9 tiny piles of dust into 1 full dust, and presses carbon mesh, alloys, and energy crystals.

## Understanding EU: voltage tiers, cables, and explosions

IC2 power tracks two separate things: total energy (EU) and packet size (voltage tier, measured in EU per packet). A machine blows up when a single incoming packet exceeds its tier rating. Under default settings, hooking a 128 EU CESU straight to a 32 EU Macerator destroys the Macerator, while feeding that same Macerator from several LV generators at once is safe because each packet stays at or below 32 EU.

### Storage tiers and redstone modes

Storage blocks come in four voltage tiers. Every tier outputs only through its dotted front face and accepts power on the other five faces.

| Block | Tier | Packet limit (EU/packet) | Capacity (EU) | Typical stage |
| --- | --- | ---: | ---: | --- |
| BatBox | Tier 1 (LV) | 32 | 40,000 | Early game and starter machines |
| CESU | Tier 2 (MV) | 128 | 300,000 | Ore washing, Metal Former, expansion |
| MFE | Tier 3 (HV) | 512 | 4,000,000 | Thermal Centrifuge, NanoSuit, mid-sized factory |
| MFSU | Tier 4 (EV) | 2,048 | 40,000,000 | UU production, nuclear grid, QuantumSuit |

Storage block GUIs include a charge slot, a discharge slot, and four armor slots for charging and discharging gear of matching tier. Electric machines also have a lightning-bolt discharge slot that takes batteries or single pieces of redstone (800 EU each) when you want to run a machine without wiring it up. The Semifluid Generator can charge items in its slot as well.

Storage blocks have seven redstone modes (0 through 6), cycled from the GUI button or by sneak-right-clicking with an empty hand. You can emit a redstone signal when nearly full to shut off a generator and save coal, or block power output whenever a redstone signal is active to wire up a master switch. A redstone comparator reads fill level from 0 to 15.

### Stepping voltage down and up with transformers

When you need to connect different voltage tiers, either add Transformer Upgrades to the machine (each upgrade raises its safe tier by 1) or put a transformer between the main line and the branch line.

All four transformers (LV, MV, HV, and EV) have a marked front face. That front face is always the high-voltage side, and the other five faces are the low-voltage side.

By default, transformers follow redstone. With no redstone signal, the transformer steps power down from the high-voltage face to the five low-voltage faces, splitting each high packet into up to four low packets per tick. An LV-Transformer, for instance, takes 128 EU packets and outputs up to four 32 EU packets per tick, so total throughput stays at 128 EU/t. Apply redstone and it switches to step-up mode, combining low-voltage input into one high-voltage packet out the front face to cut line loss over long cable runs. You can also switch between redstone control, fixed step-down, and fixed step-up in the GUI or by sneak-right-clicking with an empty hand.

### Cables, line loss, and color isolation

Each cable type carries a different packet limit and per-block energy loss. Pushing packets past a cable's rating melts the wire.

| Cable | Packet rating (EU/packet) | Loss per block (EU) | Insulation layers |
| --- | ---: | ---: | --- |
| Tin Cable | 32 | 0.2 | 1 rubber |
| Copper Cable | 128 | 0.2 | 1 rubber |
| Gold Cable | 512 | 0.4 | 2 rubber |
| HV Cable (iron) | 2,048 | 0.8 | 3 rubber |
| Glass Fibre Cable | 8,192 | 0.025 | No insulation needed, shock-free |
| EU-Detector / EU-Splitter Cable | 8,192 | 0.5 | No insulation needed, shock-free |

With default rounding enabled (`misc/roundEnetLoss = true`), the network sums the full path loss and rounds down once. Four blocks of copper cable total 0.802 EU raw loss and round down to 0 EU; the fifth block is where you lose 1 EU. Glass Fibre Cable loses only 1 EU across 40 blocks, which is why it works well for long high-voltage runs later on.

To run two lines side by side without connecting them, dye them different colors with a Painter. Different colors stay electrically separate; black dye reconnects painted branches. Spraying Construction Foam over painted cables keeps their color separation intact.

Carry an EU-Reader and right-click live cables or machines to read their real average EU/t, min/max range, and packet voltage.

### Expanding your power generation

Coal generators eventually fall behind mid-game demand. As you expand, mix in other power sources depending on your base location and fuel supply.

- Solar Panel: outputs a steady 1 EU/t under daylight with no fuel upkeep, handy for small passive setups.
- Water Mill: generates low-level power from water buckets or surrounding water blocks.
- Wind Mill: produces more power higher up and away from nearby solid blocks. Check a spot with a Wind Meter before building a tall tower.
- Geothermal Generator: burns lava at a steady 20 EU/t (10,000 EU per bucket), strong once you pump lava from the Nether.
- Semifluid Generator: burns Biogas at 16 EU/t or Creosote from a Coke Kiln at 8 EU/t.
- Radioisotope Thermoelectric Generator (RTG): uses RTG pellets from spent nuclear fuel reprocessing for fuel-free output around the clock (1, 2, or 4 EU/t for 1, 2, or 3 pellets, and more as you add pellets).

## Mid-game processing: Metal Former, ore chains, steel, canning, and electrolysis

### Metal Former

Build a Metal Former as soon as you can spare the materials. Every job costs 2,000 EU (10 EU/t for 200 ticks) and saves you from burning through Forge Hammers and Cutters. Click the center button in its GUI to switch modes.

- Extruding: turns 1 Copper Ingot into 3 Copper Cables (hand cutting gives 2 per plate), which saves a third of your copper. It also extrudes shafts, tin cans, and empty fuel rods.
- Rolling: presses 1 ingot into 1 plate, or 1 plate into 2 item casings. Steel ingots cannot be hammered into plates by hand, so rolling in a Metal Former (or slicing a Steel Block in a Block Cutting Machine) is how you produce Steel Plates and Steel Item Casings.
- Cutting: slices plates into cables and turns Iron Item Casings into industrial coins.

### Higher ore yields: Ore Washing Plant and Thermal Centrifuge

Macerating raw ore doubles your ingots right away. Sending Crushed Ore through an Ore Washing Plant and a Thermal Centrifuge pushes yields higher and recovers secondary metals you cannot mine directly.

An Ore Washing Plant takes 1 Crushed Ore, 1,000 mB of water, and 8,000 EU (16 EU/t for 500 ticks). Each run produces 1 Purified Crushed Ore, 2 tiny piles of the matching dust, and 1 Stone Dust; Crushed Lead gives 3 tiny piles of sulfur dust, and Crushed Uranium gives 2 tiny piles of lead dust. Put water buckets in the upper-right water slot and take empty buckets from the return slot below, or pipe water in. All three output slots need open space, because if even one fills up, the machine halts without spending input or water.

A Thermal Centrifuge processes Crushed Ore or Purified Crushed Ore into main dust plus secondary tiny dusts. Copper yields tiny tin dust, iron yields tiny gold dust, gold yields tiny silver dust, and uranium splits into Tiny Piles of Uranium 235 alongside Uranium 238. You also use it to reprocess depleted nuclear fuel rods into plutonium and Uranium 238.

Watch the Thermal Centrifuge's preheat requirement. Each material needs a minimum heat level before processing starts: 500 for copper, 1,000 for tin, 1,500 for iron, 2,000 for gold, lead, or silver, 3,000 for uranium, and 4,000 to 5,000 for depleted fuel rods. From cold, the machine spends EU heating up, then draws 48 EU/t during processing plus upkeep energy to hold temperature. If it loses power mid-batch, progress resets to zero and heat drops off, so feed it from a reliable grid and process ores in large batches.

### Making steel in a Blast Furnace

Steel pipes, advanced tools, and several late-game machines require Steel Ingots from a Blast Furnace.

1. Aim a heat source at it: place a Solid Fuel Firebox (burns coal for +20 net heat per tick) or an Electric Heater right next to the Blast Furnace, and use a Wrench so the heat source's orange output face presses directly against the Blast Furnace's heat-receiving face. Misaligned heat faces are the most common reason a Blast Furnace stays cold.
2. Supply compressed air: compress Empty Cells into Compressed Air Cells and load them into the Blast Furnace. Its internal air tank holds 8,000 mB and automatically ejects empty cells into the return slot. Each steel ingot uses 6,000 mB of air (6 cells).
3. Feed iron: iron ingots, iron dust, crushed iron, purified iron, or iron ore all work.

From cold, the Blast Furnace heats up to 50,000 heat before counting progress, then works for 6,000 ticks (5 minutes) per ingot to produce 1 Steel Ingot and 1 Slag. Keep both output slots from filling up; if either slot blocks output, progress resets. Do not throw away Slag, because centrifuging 1 Slag gives 1 Tiny Pile of Gold Dust and 5 Coal Dust.

### Canning machines and the Electrolyzer

The Fluid/Solid Canning Machine places containers in the top-left slot, ingredients in the center slot, and outputs on the right. Its mode button cycles four modes: canning solids (packing food into tin cans or loading uranium and MOX into empty fuel rods), filling fluid from its tank into cells or buckets, draining fluid from containers into the tank, and Fluid Enriching. Fluid Enriching makes Coolant (1,000 mB water plus 8 Lapis Dust, or 1,000 mB Distilled Water plus just 1 Lapis Dust), Biomass (water plus Bio Chaff), and Construction Foam (water plus CF Powder). A standalone Solid Canning Machine is also available if you only need solid canning.

The Electrolyzer splits 40 mB of water over 200 ticks using 6,400 EU (32 EU/t), sending 26 mB of Hydrogen downward and 13 mB of Oxygen upward. Paid progress survives power cuts. Its four right-hand upgrade slots accept Fluid Pulling Upgrades to draw water from an adjacent tank; it rejects overclocker, transformer, energy storage, and item upgrades.

### Machine upgrades

Basic electric machines have four upgrade slots along the right edge of their GUI.

- Overclocker Upgrade: each one multiplies duration by 0.7 and energy per tick by 1.6. One Overclocker speeds a Macerator from 300 ticks at 2 EU/t to 210 ticks at 3 EU/t. Check your power generation before stacking several.
- Energy Storage Upgrade: adds 10,000 EU to the machine's internal buffer per upgrade. When stacked Overclockers push EU/t high, add at least one storage upgrade so the internal buffer does not fall below a single tick's cost.
- Transformer Upgrade: raises the machine's safe input tier by 1 per upgrade so you can hook LV machines straight onto MV or HV lines.
- Ejector, Pulling, Fluid Ejector, and Fluid Pulling Upgrades: automatically push outputs to or pull inputs from adjacent inventories and tanks. Right-click a block face while holding an upgrade to lock it to that direction, stack multiples in one slot to move more items per tick, or upgrade to the Advanced Ejector and Advanced Pulling Upgrades for item filtering.

## Logistics, pipes, sorting, and storage

### Bronze and steel pipes

To connect machines across a factory floor, craft Bronze or Steel pipes in four diameters: tiny, small, medium, and large.

Fluid pipes take 6 parts of the matching material: casings for tiny and small pipes, plates for medium and large pipes. Fill the top and bottom rows of the crafting grid for tiny or medium pipes, or the left and right columns for small or large pipes. Combine any fluid pipe with a vanilla Hopper in any grid slot to make the matching item pipe.

| Pipe size | Bronze fluid capacity | Steel fluid capacity | Item pipe throughput | Fluid pipes per craft |
| --- | ---: | ---: | ---: | ---: |
| Tiny | 400 mB | 800 mB | 1 item/tick | 6 |
| Small | 800 mB | 1,600 mB | 4 items/tick | 3 |
| Medium | 2,400 mB | 4,800 mB | 16 items/tick | 2 |
| Large | 4,800 mB | 9,600 mB | 64 items/tick | 1 |

Three habits prevent most pipe headaches.

1. Item pipes pull from the inventory behind them and push items out their aimed face. Right-click a face with a Wrench to steer the output.
2. Fluid pipes need a pump attachment on the face touching a source tank to pull liquid out: Pump Attachment (LV) pumps 32 mB/t (crafted from a Medium Bronze Fluid Pipe, a piston, and an Insulated Copper Cable), and Pump Attachment (MV) pumps 128 mB/t (from a Medium Steel Fluid Pipe, a piston, and an Insulated Gold Cable). Right-click a pipe face with a Wrench to close or reopen that connection, and pry off pump attachments cleanly with a Crowbar.
3. Test pipe direction with a bucket of water or a stack of empty cells before hooking up ore washers, canners, or electrolyzers. Give every byproduct (stone dust, slag, empty cells) a destination so a single full slot cannot stall the line.

### Tanks, batch crafting, sorting, and portable storage

Bronze, Iron, Steel, Iridium, and general Tanks store liquids. In a tank's GUI, click the center fluid slot with a bucket or cell to fill or drain it; the general Tank also has four upgrade slots on the right for directional Fluid Pulling and Ejector upgrades. Place a powered Pump over a water source to fill adjacent tanks or empty buckets inside its GUI.

A Batch Crafter saves a recipe template for automated crafting. An Electric Sorting Machine routes items to chosen output sides by filter, while Weighted Item Distributors and Weighted Fluid Distributors send items or fluids by side priority (sneak-right-click a side with an empty hand to adjust its priority).

Wooden, Bronze, Iron, Steel, and Iridium Storage Boxes keep all their contents when broken and carried. The Iridium Storage Box displays all 126 slots on an 18-by-7 grid. Page buttons on sorters, distributors, and storage boxes switch views inside the same container without losing items.

## Coke Kiln, heat networks, and closed-loop steam power

### Coke Kiln

To produce slower-burning Coke and liquid Creosote, build a 3x3x3 hollow multiblock out of Refractory Bricks.

1. Build a 3x3x3 cube of Refractory Bricks with the single center block left empty (air).
2. Replace the front center brick with the Coke Kiln controller, the top center brick with a Coke Kiln Hatch facing up, and the bottom center brick with a Coke Kiln Grate facing down.

Feed coal or logs into the Coke Kiln or through the top hatch. Coal converts into Coke plus 500 mB of Creosote; logs convert into charcoal plus 250 mB of Creosote. The bottom grate pushes Creosote downward into pipes or tanks, ready for a Semifluid Generator or Liquid Fuel Firebox.

### Closed-loop steam turbine plant

When you want to convert steady heat (from fireboxes, an Electric Heater, or a fluid nuclear reactor's Liquid Heat Exchanger) into higher EU output, connect a closed-loop steam plant:

```text
[Heat Source] -- heat --> [Steam Boiler] -- steam --> [Steam Turbine] -- kinetic --> [Kinetic Generator] --> EU grid
                               ^                             |
                      Distilled Water return           Exhaust steam
                               |                             v
                               +----------------------- [Condenser]
```

Feed steady heat and water into a Steam Boiler. Once it warms up to 20,000 heat (standard steam mode) or 60,000 heat (superheated steam mode), it pushes steam into a Steam Turbine, which spins a Kinetic Generator to produce EU. Plain water slowly calcifies the boiler and stops production at 10,000 cycles, so start with Distilled Water from a Solar Distiller and route exhaust steam through a Condenser to send Distilled Water back into the boiler.

## Crops, automated mining, electric gear, and teleporters

### Crop breeding and farm automation

Place Crop Sticks on hydrated farmland to plant vanilla and IC2 crops. Place double Crop Sticks on an empty tilled block between two mature crops to crossbreed new varieties (such as coffee, stickreed, or metal-bearing crops) and improve stats.

Scan unknown seed bags in a Cropnalyzer to read their species along with Growth, Gain, and Resistance stats. Place a Crop-Matron (supplied with water, Fertilizer, and Weed-EX) alongside a Crop Harvester to water, fertilize, weed, and harvest your plots automatically.

### Automated mining

A standard Miner takes a Mining Drill or Diamond Drill, Mining Pipes, an OD or OV Scanner, EU, and an adjacent chest for ore drops, then drills straight down and collects nearby ores. An Advanced Miner covers a wider area without mining pipes, supports whitelist and blacklist filtering, and responds to redstone signals (including Redstone Signal Inverter Upgrades).

### Electric tools, armor, and charging

Mining Drills, Diamond Drills, Iridium Drills, Chainsaws, Electric Wrenches, Mining Lasers (which cycle eight beam modes), Nano Sabers, Electric Jetpacks, NanoSuit armor, and QuantumSuit armor all charge in the charging slots of a matching-tier storage block or by standing on a matching Charge Pad.

Wearing a BatPack, Advanced BatPack, or Energy Pack recharges handheld electric tools automatically as you mine. With Curios installed, battery packs and jetpacks work from the back slot and charge on Charge Pads; if both your chestplate slot and back slot hold compatible gear, the chestplate takes priority.

### Teleporters and trading machines

Link two Teleporters by right-clicking both in sequence with the same Frequency Transmitter. Put a charged storage block next to the departure Teleporter and apply a redstone signal to jump to the destination. Energy cost scales with distance and what you carry.

For multiplayer shops, a Trade-O-Mat pulls goods from an adjacent chest and deposits payments back into it. An Energy-O-Mat sells EU in batches; the owner can sneak-right-click it with an empty hand to cycle between 1,000, 5,000, and 10,000 EU per sale.

## In-game nuclear reactors and radiation safety

### Radiation safety first

Processed Enriched Uranium Nuclear Fuel, MOX, plutonium, and fuel rods inflict radiation damage if carried loose in your inventory. Take two precautions before handling nuclear fuel.

1. Keep radioactive items inside a Containment Box when storing or carrying them.
2. Wear all four pieces of hazmat gear together: Scuba Helmet, Hazmat Suit, Hazmat Suit Leggings, and Rubber Boots. Missing even one piece leaves you exposed.

### Testing your first electric reactor

A bare Nuclear Reactor opens 3 columns (18 slots). Attaching Reactor Chambers to its six sides opens 1 extra column per chamber, up to 9 columns (54 slots), and cables attached to any chamber draw EU from the reactor.

A reactor runs only while powered by redstone, ticking fuel burn, heat, and EU once every 20 game ticks (1 second).

For a safe first test, put 1 Fuel Rod (Uranium) right next to a Heat Vent or Reactor Heat Vent and apply redstone. The reactor holds a steady 5 EU/t with 0 hull heat.

When you build larger designs, placing two single uranium rods side by side (or using a Dual Fuel Rod) pushes output from 5 EU/t to 20 EU/t while raising heat to 24 per cycle. Putting a neutron reflector next to a single rod doubles it to 10 EU/t and 12 heat per cycle.

Four component families control that heat. Coolant Cells (10k, 30k, and 60k) absorb heat from adjacent rods until they hit their limit and disappear on the next heat pulse. Heat Vents (standard, Advanced, Reactor, Overclocked, and Component) vent heat from themselves, the reactor hull, or adjacent parts, and keep cooling the hull even after you cut redstone. Heat Exchangers move heat between components and the hull. Reactor Plating raises maximum hull heat above the default 10,000 (+1,000 for standard plating, +2,000 for Heat-Capacity Reactor Plating, and +500 for Containment Reactor Plating).

If hull heat reaches the cap, the reactor melts down even with no fuel inside and no redstone signal applied. Check hull temperature against the cap inside the GUI before leaving a new design unattended.

### Fluid nuclear reactors

To feed a large steam turbine plant from nuclear heat, switch the reactor into fluid mode.

1. Attach all 6 Reactor Chambers to the Nuclear Reactor core.
2. Build a complete 5x5x5 hollow shell around the core (leaving a 3x3x3 interior space) out of Reactor Pressure Vessels.
3. Swap three port types into the outer walls as needed: a Reactor Access Hatch to open the reactor grid, Reactor Fluid Ports to pump in Coolant and extract Hot Coolant, and a Reactor Redstone Port to turn the reactor on with redstone.

In fluid mode, the reactor stops outputting EU directly and transfers its heat into Coolant. A single uranium rod next to a Reactor Heat Vent converts 48 mB of Coolant into Hot Coolant over six cycles. Pipe Hot Coolant into a Liquid Heat Exchanger to heat a Steam Boiler, then pump cooled Coolant back into the reactor.

## UU-Matter and replication

Late in a world, you can spend high-voltage EU to synthesize materials across four machines.

1. Recycler: feed surplus cobblestone or junk blocks to produce Scrap (9 Scrap craft into 1 Scrap Box).
2. Matter Fabricator: hook up high-voltage power and feed Scrap or Scrap Boxes as amplifier. Without amplifier, 1 mB of liquid UU-Matter takes 1,000,000 EU; Scrap boosts effective EU conversion 6x and cuts the energy bill sharply.
3. Scanner and Pattern Storage: place them directly next to each other. Scan a supported sample item in the Scanner (you can test the full scan, store, and replicate workflow with cobblestone), then save the pattern into the neighboring Pattern Storage. You can also copy a pattern onto a Crystal Memory by right-clicking the Scanner, then transfer it to another Pattern Storage.
4. Replicator: place it next to the Pattern Storage, supply EU and liquid UU-Matter, pick a stored pattern, and run either Single Run or Repeat mode.

You can also find Iridium Shards and whole Iridium Ore in dungeon and structure loot chests (compress 9 shards into 1 Iridium Ore) for QuantumSuit armor and late-game recipes.

## Classic machines and rebuild survival recipes

Several Classic-style machines coexist alongside the Experimental lineup: Classic Canner, Classic Electrolyzer, Classic Mass Fabricator, Classic MFE, Classic MFSU, Classic Crop-Matron, and Classic Nuke.

Electrolyzer, Matter Fabricator, and MFE share identical original crafting layouts between Experimental and Classic. To craft the Classic version of those three, make the Experimental machine first and place it alone in a crafting grid. The other Classic devices use their own layouts from the Classic recipe profile. Both Classic fuel lines (Plantball and Hydrated Coal Dust) work through the Compressor, cell recipes, Extractor, and Classic Canner.

This rebuild also adds several survival crafting recipes.

- Tank, UU Assembly Bench, Advanced Ejector Upgrade, and Advanced Pulling Upgrade each have a standalone survival recipe.
- Bronze Dust, Lapis Dust, Diamond Dust, Obsidian Dust, and Uranium 238 split 1-to-9 into tiny piles on the crafting table.
- Universal Fluid Cell uses a glass block in the center, while Empty Cell keeps a glass pane in the center, so both containers are craftable side by side.

See the full list in [Classic and supplemental recipes](../spec/registry/classic-playable-recipes.json).

### Survival recipes for compatibility materials

A small set of legacy and cross-mod materials remains registered. The rebuild adds crafting-table entries for them while keeping normal machine recipes working.

| Item | How to obtain |
| --- | --- |
| IC2 Copper Ingot | Place a vanilla Copper Ingot in a crafting grid. |
| Lithium Fuel Rod | Combine an empty Fuel Rod with Lithium Dust in a crafting grid. |
| Uranium pellets, MOX pellets, CF pellets | Place Enriched Uranium Nuclear Fuel, MOX, or CF Powder in a crafting grid to press pellets, or place pellets back to recover the original material. |
| Weed-EX Bucket | Combine a Weed-EX cell with an empty bucket; buckets can fill a Crop-Matron directly. |
| Heavy Water Bucket, Crushed Silver Ore | Craft Heavy Water Bucket from solid UU-Matter, a Water Cell, and an empty bucket. Craft Crushed Silver Ore from solid UU-Matter and Silver Dust (collected as a byproduct from centrifuging gold ore); Crushed Silver Ore then washes normally in an Ore Washing Plant. |

## Specifications and test references

Machine recipes, energy networking, and in-game radiation are independent implementations. For tick-by-tick measurements and edge cases, see [Advanced processing and Blast Furnace reference](../spec/machines/advanced-processing.md), [Nuclear reactor reference readings](../parity/evidence/reference-1.2.4/README.md), and [World, agriculture, and upgrade tests](../parity/evidence/world-program/README.md).
