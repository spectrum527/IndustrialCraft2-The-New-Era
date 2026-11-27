"""Acceptance checks for physical fluid, heat and kinetic transitions on a release-only Forge server."""

from packaged_playable import block, energy, feed, items, merge, seed_eu


def kinetic_routes(server):
    server.send("forceload add 0 0 31 15")
    for z, facing, xs, cable in (
        (3, "east", (2, 3, 5), "4 150 3"),
        (7, "west", (22, 21, 20), None),
    ):
        motor, converter, storage = (f"{x} 150 {z}" for x in xs)
        intake = "west" if facing == "east" else "east"
        server.send(block(motor, f"ic2:electric_kinetic_generator[facing={facing}]"),
                    block(converter, f"ic2:kinetic_generator[facing={intake}]"),
                    block(storage, "ic2:mfe"))
        if cable:
            server.send(block(cable, "ic2:gold_cable"))
        server.send(merge(motor, '{ic2_energy:800.0d,Items:[{Slot:0b,id:"ic2:electric_motor",Count:1b}]}'))
        server.require("motor_paid_and_mfe_received_800_" + facing,
                       energy(motor, 0), energy(converter, 0), energy(storage, 800))
        server.state(motor, converter, storage)
        if cable:
            server.send(block(motor, f"ic2:manual_kinetic_generator[facing={facing}]"),
                        merge(motor, "{ic2_heat:40}"))
            server.require("one_crank_added_10_eu_on_same_cable", energy(storage, 810))


def thermal_lines(server):
    server.send(
        "forceload add 0 0 47 47",
        "fill 1 157 0 15 157 14 minecraft:stone",
        block("2 158 3", "minecraft:barrel"),
        block("3 158 3", "ic2:bronze_item_pipe_small[facing=east]"),
        block("4 158 3", "ic2:solid_heat_generator[facing=east]"),
        block("5 158 3", "ic2:stirling_generator[facing=west]"),
        block("6 158 3", "ic2:insulated_copper_cable"),
        block("7 158 3", "ic2:batbox"),
        "item replace block 2 158 3 container.0 with minecraft:coal 1",
        block("5 158 10", "ic2:solid_heat_generator[facing=east]"),
        block("6 158 10", "ic2:fermenter[facing=west]"),
        block("7 158 10", "ic2:bronze_fluid_pipe_medium"),
        block("8 158 10", "ic2:bronze_tank"),
        block("6 158 9", "ic2:bronze_fluid_pipe_medium"),
        block("6 158 8", "ic2:bronze_tank"),
        merge("6 158 9", "{ic2_pump_mv:4}"),
        merge("6 158 8", '{ic2_fluid:{FluidName:"ic2:biomass",Amount:1000}}'),
        *feed("5 158 10", "minecraft:coal", 8),
    )

    server.send(
        block("3 158 18", "ic2:generator"),
        *(block(f"{x} 158 18", "ic2:insulated_copper_cable") for x in range(4, 9)),
        block("4 158 19", "ic2:pump[facing=down]"),
        block("4 157 19", "ic2:bronze_tank"),
        merge("4 157 19", '{ic2_fluid:{FluidName:"minecraft:water",Amount:40}}'),
        block("5 158 19", "ic2:fluid_regulator[facing=east]"),
        seed_eu("5 158 19", 10000),
        block("6 158 19", "ic2:bronze_fluid_pipe_medium"),
        block("7 158 19", "ic2:bronze_fluid_pipe_medium"),
        block("8 158 19", "ic2:electrolyzer"),
        block("8 157 19", "ic2:bronze_tank"),
        block("8 159 19", "ic2:bronze_tank"),
        *feed("3 158 18", "minecraft:coal", 6),
    )

    server.send(
        "fill 31 186 24 34 194 26 minecraft:air",
        block("32 190 25", "ic2:wind_kinetic_generator[facing=south]"),
        block("32 190 26", "ic2:kinetic_generator[facing=north]"),
        block("33 190 26", "ic2:glass_fibre_cable"),
        block("34 190 26", "ic2:mfe"),
    )
    server.require("wind_without_rotor_yields_no_eu", energy("34 190 26", 0),
                   f"if block 32 190 25 ic2:wind_kinetic_generator[active=false]")
    server.send(*feed("32 190 25", "ic2:carbon_rotor", 1))
    server.require("rotor_supplied_actual_kinetic_eu", f"if block 32 190 25 ic2:wind_kinetic_generator[active=true]",
                   f"unless data block 34 190 26 {{ic2_energy:0.0d}}")
    server.send(block("32 190 24", "minecraft:stone"))
    server.require("wind_obstruction_stopped_rotor", f"if block 32 190 25 ic2:wind_kinetic_generator[active=false]")
    server.require("solid_heat_stirling_actually_supplied_storage", f"unless data block 7 158 3 {{ic2_energy:0.0d}}",
                   "unless data block 2 158 3 Items[0]")
    server.require("pump_and_regulator_delivered_distinct_gases_to_real_tanks",
                   'if data block 8 157 19 {ic2_fluid:{FluidName:"ic2:hydrogen",Amount:26}}',
                   'if data block 8 159 19 {ic2_fluid:{FluidName:"ic2:oxygen",Amount:13}}',
                   "unless data block 4 157 19 ic2_fluid",
                   "unless data block 4 158 19 ic2_input_fluid",
                   "unless data block 5 158 19 ic2_input_fluid",
                   "unless data block 8 158 19 ic2_input_fluid",
                   seconds=60)
    server.require("heated_fermenter_made_20000_mb_biogas_and_two_fertilizer",
                   'if data block 8 158 10 {ic2_fluid:{FluidName:"ic2:biogas",Amount:20000}}',
                   items("6 158 10", "ic2:fertilizer", 2, 9),
                   "unless data block 6 158 10 ic2_input_fluid",
                   seconds=150)

    server.send(
        block("9 158 10", "ic2:bronze_fluid_pipe_medium"),
        merge("9 158 10", "{ic2_pump_mv:16}"),
        block("10 158 10", "ic2:semifluid_generator"),
        block("11 158 10", "ic2:insulated_copper_cable"),
        block("12 158 10", "ic2:fluid_bottler"),
        block("13 158 10", "ic2:bronze_item_pipe_small[facing=east]"),
        block("14 158 10", "minecraft:barrel"),
        block("12 158 9", "ic2:bronze_fluid_pipe_medium"),
        block("12 158 8", "ic2:bronze_tank"),
        merge("12 158 9", "{ic2_pump_mv:4}"),
        merge("12 158 8", '{ic2_fluid:{FluidName:"minecraft:water",Amount:2000}}'),
        *feed("12 158 10", "ic2:empty_cell", 2),
    )
    server.require("semifluid_pumped_fermentation_gas_powered_bottler",
                   items("14 158 10", "ic2:basic_water_cell", 2), "unless data block 12 158 10 ic2_input_fluid",
                   "unless data block 12 158 8 ic2_fluid")
    server.state("7 158 3", "8 157 19", "8 159 19", "6 158 10", "8 158 10", "10 158 10", "14 158 10")


def metal_lines(server):
    server.send(
        "forceload add 0 16 47 63",
        "time set noon",
        "gamerule doDaylightCycle false",
        "weather clear",
        block("3 160 41", "ic2:generator"),
        *feed("3 160 41", "minecraft:coal", 8),
        *(block(f"{x} 160 41", "ic2:insulated_copper_cable") for x in range(4, 11)),
        block("3 160 42", "minecraft:barrel"),
        "item replace block 3 160 42 container.0 with ic2:raw_tin 1",
        *(block(f"{x} 160 42", "ic2:bronze_item_pipe_small[facing=east]") for x in (4, 6, 7, 9, 10)),
        block("5 160 42", "ic2:macerator"),
        block("8 160 42", "ic2:ore_washing_plant"),
        block("11 160 42", "minecraft:barrel"),
        block("8 160 43", "ic2:bronze_fluid_pipe_medium"),
        block("8 160 44", "ic2:bronze_fluid_pipe_medium"),
        block("8 160 45", "ic2:bronze_tank"),
        merge("8 160 44", "{ic2_pump_mv:8}"),
        merge("8 160 45", '{ic2_fluid:{FluidName:"minecraft:water",Amount:2000}}'),
    )
    server.send(
        *(block(f"{x} 158 {z}", "ic2:solar_generator") for x in range(2, 6) for z in (31, 33)),
        *(block(f"{x} 158 32", "ic2:tin_cable") for x in range(2, 6)),
        block("6 158 32", "ic2:batbox[facing=east]"),
        *(block(f"{x} 158 32", "ic2:insulated_copper_cable") for x in range(7, 13)),
        block("7 158 33", "minecraft:barrel"),
        "item replace block 7 158 33 container.0 with minecraft:raw_copper 1",
        *(block(f"{x} 158 33", "ic2:bronze_item_pipe_small[facing=east]") for x in (8, 10, 11, 13)),
        block("9 158 33", "ic2:electric_furnace"),
        block("12 158 33", "ic2:metal_former"),
        block("14 158 33", "minecraft:barrel"),
        block("18 158 29", "ic2:pump[facing=down]"),
        block("18 157 29", "ic2:bronze_tank"),
        merge("18 157 29", '{ic2_fluid:{FluidName:"minecraft:water",Amount:1000}}'),
        block("20 158 29", "ic2:generator"),
        block("20 158 30", "ic2:insulated_copper_cable"),
        block("19 158 30", "ic2:insulated_copper_cable"),
        block("18 158 30", "ic2:insulated_copper_cable"),
        block("19 158 29", "ic2:compressor"),
        *feed("20 158 29", "minecraft:coal", 1),
    )
    server.require("solar_array_storage_furnace_former_exported_copper_cable",
                   items("14 158 33", "ic2:copper_cable", 3),
                   "unless data block 7 158 33 Items[0]", seconds=100)
    server.require("pump_from_tank_paired_compressor_made_snowball",
                   items("19 158 29", "minecraft:snowball", 1, 2),
                   "unless data block 18 157 29 ic2_fluid",
                   "unless data block 18 158 29 ic2_input_fluid", seconds=80)

    barrel = "11 160 42"
    server.require("macerator_washer_and_pipes_exported_every_product",
                   f'if data block {barrel} Items[{{id:"ic2:purified_tin",Count:2b}}]',
                   f'if data block {barrel} Items[{{id:"ic2:small_tin_dust",Count:4b}}]',
                   f'if data block {barrel} Items[{{id:"ic2:stone_dust",Count:2b}}]',
                   "unless data block 8 160 45 ic2_fluid",
                   'if data block 8 160 42 {ic2_input_fluid:{FluidName:"minecraft:water",Amount:1800}}',
                   "unless data block 3 160 42 Items[0]", seconds=130)
    server.state("6 158 32", "14 158 33", "11 160 42", "19 158 29")


def advanced_lines(server):
    server.send("forceload add 32 32 63 79", "time set noon", "gamerule doDaylightCycle false", "weather clear")
    server.send(
        "setblock 61 172 76 minecraft:dispenser[facing=up]",
        "item replace block 61 172 76 container.0 with ic2:scrap_box 2",
        "setblock 61 171 76 minecraft:redstone_block",
    )
    scrap_reward = "type=minecraft:item,x=61,y=173,z=76,distance=..5"
    server.require(
        "powered_dispenser_consumed_one_scrap_box_and_ejected_one_reward",
        items("61 172 76", "ic2:scrap_box", 1, 0),
        f"if entity @e[{scrap_reward},nbt={{Item:{{Count:1b}}}}]",
        f'unless entity @e[{scrap_reward},nbt={{Item:{{id:"ic2:scrap_box"}}}}]',
    )
    assembly = "40 155 47"
    canner = "40 155 43"
    for pos, path in ((assembly, "uu_assembly_bench"), (canner, "classic_canner")):
        x, y, z = map(int, pos.split())
        source = f"{x-2} {y} {z}"
        server.send(block(source, "ic2:batbox[facing=east]"), seed_eu(source, 1200),
                    block(f"{x-1} {y} {z}", "ic2:insulated_copper_cable"), block(pos, "ic2:" + path))
    server.send(
        merge(assembly, '{Items:[{Slot:0b,id:"ic2:uu_matter",Count:1b},{Slot:1b,id:"ic2:uu_matter",Count:1b}]}'),
        merge(canner, '{Items:[{Slot:0b,id:"ic2:coalfuel_cell",Count:6b},{Slot:1b,id:"ic2:empty_fuel_can",Count:1b}]}'),
        block("40 155 52", "ic2:classic_electrolyzer[facing=east]"),
        block("41 155 52", "ic2:mfsu[facing=east]"),
        seed_eu("41 155 52", 3500000),
        *feed("40 155 52", "ic2:water_cell", 1),
        block("40 155 55", "ic2:classic_electrolyzer[facing=east]"),
        block("41 155 55", "ic2:mfsu[facing=east]"),
        merge("40 155 55", "{ic2_mode:1}"),
        *feed("40 155 55", "ic2:electrolyzed_water_cell", 1),
        block("43 155 47", "ic2:liquid_heat_exchanger[facing=east]"),
        block("44 155 47", "ic2:stirling_generator[facing=west]"),
        block("45 155 47", "ic2:insulated_copper_cable"),
        block("46 155 47", "ic2:batbox"),
        merge("43 155 47", '{ic2_input_fluid:{FluidName:"ic2:hot_coolant",Amount:100},'
                               'ic2_output_fluid:{FluidName:"ic2:coolant",Amount:2000}}'),
        block("38 157 52", "ic2:solar_distiller"),
        merge("38 157 52", '{ic2_input_fluid:{FluidName:"minecraft:water",Amount:100}}'),
    )
    kiln = "40 150 64"
    grate = "40 149 65"
    shell = "39 150 66"
    server.send(
        "fill 39 149 64 41 151 66 ic2:refractory_bricks",
        block("40 150 65", "minecraft:air"),
        block(kiln, "ic2:coke_kiln[facing=north]"),
        block(grate, "ic2:coke_kiln_grate[facing=down]"),
        block("40 151 65", "ic2:coke_kiln_hatch[facing=up]"),
        block("40 148 65", "ic2:bronze_fluid_pipe_medium"),
        block("40 147 65", "ic2:bronze_tank"),
        merge("40 151 65", '{Items:[{Slot:0b,id:"minecraft:oak_log",Count:1b}]}'),
        block("41 152 65", "minecraft:barrel"),
    )
    server.require("full_coolant_output_blocked_heat_and_conserved_100_mb",
                   energy("46 155 47", 0),
                   'if data block 43 155 47 {ic2_input_fluid:{FluidName:"ic2:hot_coolant",Amount:100}}',
                   'if data block 43 155 47 {ic2_output_fluid:{FluidName:"ic2:coolant",Amount:2000}}',
                   "if block 40 150 64 ic2:coke_kiln[active=true]")
    server.send(merge("43 155 47", '{ic2_output_fluid:{FluidName:"ic2:coolant",Amount:1999}}'), block(shell, "minecraft:air"))
    server.require("breaking_refractory_formation_pauses_kiln_without_losing_the_log",
                   "if block 40 150 64 ic2:coke_kiln[active=false]",
                   items(kiln, "minecraft:oak_log", 1), "unless data block 40 147 65 ic2_fluid")
    server.send(block(shell, "ic2:refractory_bricks"))
    server.require("exchanger_makes_10_hu_from_exactly_one_available_mb",
                   energy("46 155 47", 5),
                   'if data block 43 155 47 {ic2_input_fluid:{FluidName:"ic2:hot_coolant",Amount:99}}')
    server.require("classic_canner_preserved_measured_15288_fuel_value",
                   'if data block 40 155 43 Items[{Slot:9b,id:"ic2:filled_fuel_can",Count:1b,tag:{value:15288}}]',
                   "unless data block 40 155 43 Items[{Slot:0b}]", seconds=30)
    server.require("uu_assembly_kept_template_and_made_eight_logs",
                   items(assembly, "minecraft:oak_log", 8, 9), items(assembly, "ic2:uu_matter", 1),
                   f"unless data block {assembly} Items[{{Slot:1b}}]")
    server.require("classic_electrolyzer_stored_cell_removed_exactly_20000_eu",
                   items("40 155 52", "ic2:electrolyzed_water_cell", 1, 9), energy("41 155 52", 3480000))
    server.require("classic_electrolyzer_restored_cell_returned_exactly_18000_eu",
                   items("40 155 55", "ic2:water_cell", 1, 9), energy("41 155 55", 18000))
    server.require("daylight_preserved_exact_water_sum",
                   'if data block 38 157 52 {ic2_input_fluid:{FluidName:"minecraft:water",Amount:94},'
                   'ic2_output_fluid:{FluidName:"ic2:distilled_water",Amount:6}}', seconds=45)
    server.require("refitted_coke_kiln_made_charcoal_and_250_mb_creosote",
                   items(kiln, "minecraft:charcoal", 1, 9),
                   'if data block 40 147 65 {ic2_fluid:{FluidName:"ic2:creosote",Amount:250}}', seconds=90)
    server.send(merge(kiln, "{Items:[]}"), merge("40 151 65", '{Items:[{Slot:0b,id:"minecraft:coal",Count:1b}]}'))
    server.require("coke_kiln_made_coke_and_500_additional_mb_creosote",
                   items(kiln, "ic2:coke", 1, 9),
                   'if data block 40 147 65 {ic2_fluid:{FluidName:"ic2:creosote",Amount:750}}', seconds=90)
    server.state(kiln, "40 147 65", '43 155 47', canner, assembly, "38 157 52", "41 155 52", "41 155 55")


def steam_lines(server):
    server.send(
        "forceload add 48 32 79 79",
        block("50 153 54", "ic2:solid_heat_generator[facing=east]"),
        *feed("50 153 54", "minecraft:coal", 4),
        block("51 153 54", "ic2:steam_generator[facing=west]"),
        block("52 153 54", "ic2:bronze_tank"),
        merge("51 153 54", '{ic2_input_fluid:{FluidName:"ic2:distilled_water",Amount:50}}'),
        block("50 153 58", "ic2:solid_heat_generator[facing=east]"),
        *feed("50 153 58", "minecraft:coal", 10),
        block("51 153 58", "ic2:steam_generator[facing=west]"),
        block("52 153 58", "ic2:bronze_tank"),
        merge("51 153 58", '{ic2_mode:1,ic2_input_fluid:{FluidName:"ic2:distilled_water",Amount:50}}'),
        block("53 153 36", "ic2:batbox[facing=east]"),
        seed_eu("53 153 36", 1400),
        block("54 153 36", "ic2:insulated_copper_cable"),
        block("55 153 36", "ic2:steam_repressurizer[facing=east]"),
        block("56 153 36", "ic2:bronze_tank"),
        merge("55 153 36", '{ic2_input_fluid:{FluidName:"ic2:superheated_steam",Amount:1600}}'),
    )

    for z, gas, amount in ((43, "steam", 10000), (47, "superheated_steam", 1000)):
        server.send(
            block(f"56 153 {z}", "ic2:steam_kinetic_generator[facing=east]"),
            block(f"56 152 {z}", "ic2:bronze_tank"),
            block(f"57 153 {z}", "ic2:kinetic_generator[facing=west]"),
            block(f"58 153 {z}", "ic2:glass_fibre_cable"),
            block(f"59 153 {z}", "ic2:mfe"),
            merge(f"56 153 {z}", f'{{ic2_input_fluid:{{FluidName:"ic2:{gas}",Amount:{amount}}}}}'),
            *feed(f"56 153 {z}", "ic2:steam_turbine", 1),
        )
    server.send(block("55 153 43", "ic2:condenser"))
    server.require("superheated_turbine_requires_an_exhaust_receiver",
                   energy("59 153 47", 0),
                   'if data block 56 153 47 {ic2_input_fluid:{FluidName:"ic2:superheated_steam",Amount:1000}}',
                   items("56 153 47", "ic2:steam_turbine", 1))
    server.send(block("55 153 47", "ic2:condenser"))
    server.require("powered_repressurizer_moved_3200_mb_steam",
                   'if data block 56 153 36 {ic2_fluid:{FluidName:"ic2:steam",Amount:3200}}',
                   "unless data block 55 153 36 ic2_input_fluid")
    server.require("turbine_delivered_distilled_water_and_real_eu",
                   "unless data block 55 153 43 ic2_input_fluid",
                   'if data block 56 152 43 {ic2_fluid:{FluidName:"ic2:distilled_water",Amount:10}}',
                   "unless data block 59 153 43 {ic2_energy:0.0d}",
                   "unless data block 56 153 43 ic2_input_fluid")
    server.require("superheated_turbine_continued_when_condenser_connected",
                   'if data block 55 153 47 {ic2_input_fluid:{FluidName:"ic2:steam",Amount:900}}',
                   "unless data block 59 153 47 {ic2_energy:0.0d}",
                   "unless data block 56 153 47 ic2_input_fluid")

    for z, gas in ((54, "steam"), (58, "superheated_steam")):
        server.require(f"cold_boiler_made_exactly_5000_mb_{gas}",
                       f'if data block 52 153 {z} {{ic2_fluid:{{FluidName:"ic2:{gas}",Amount:5000}}}}',
                       f"unless data block 51 153 {z} ic2_input_fluid",
                       f"if data block 51 153 {z} {{ic2_cycle_count:0}}", seconds=190)
    server.state("51 153 54", "52 153 54", "51 153 58", "52 153 58", "55 153 43", "56 152 43")


def matter_lines(server):
    server.send("forceload add 64 48 95 79")
    for z, machine in ((55, "mass_fabricator"), (58, "classic_mass_fabricator"), (61, "matter_generator")):
        x = 70
        source = f"{x-2} 154 {z}"
        device = f"{x} 154 {z}"
        server.send(
            block(source, "ic2:mfsu[facing=east]"),
            seed_eu(source, 1_200_000),
            block(f"{x-1} 154 {z}", "ic2:glass_fibre_cable"),
            block(device, f"ic2:{machine}[facing=east]"),
            *feed(device, "ic2:scrap", 1),
        )
    server.send(
        block("71 154 61", "ic2:bronze_tank"),
        block("70 154 64", "ic2:mass_fabricator"),
        merge("70 154 64", '{ic2_energy:1000000.0d,Items:[{Slot:9b,id:"ic2:uu_matter",Count:64b}]}'),
    )
    server.require("fabricator_does_not_spend_one_million_with_full_output",
                   energy("70 154 64", 1000000),
                   items("70 154 64", "ic2:uu_matter", 64, 9))
    server.send(merge("70 154 64", "{Items:[]}"))
    server.require("completed_fabricator_charges_exactly_one_million_after_clear",
                   energy("70 154 64", 0),
                   items("70 154 64", "ic2:uu_matter", 1, 9))
    for z in (55, 58):
        server.require(f"real_glass_grid_and_scrap_powered_uu_item_at_{z}",
                       items(f"70 154 {z}", "ic2:uu_matter", 1, 9),
                       f"unless data block 70 154 {z} Items[{{Slot:0b}}]",
                       seconds=50)
    server.require("real_glass_grid_and_scrap_powered_one_mb_of_uu_fluid",
                   'if data block 71 154 61 {ic2_fluid:{FluidName:"ic2:uu_matter",Amount:1}}',
                   "unless data block 70 154 61 Items[{Slot:0b}]", seconds=50)
    server.state("68 154 55", "70 154 55", "70 154 58", "70 154 61", "71 154 61", "70 154 64")
