package com.industrialage.ia2.content;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ordered item families for the native IC2 creative tabs. */
public enum CreativeCategory {
    MACHINES(
            "machines",
            "industrialcraft",
            "macerator",
            """
            macerator extractor compressor iron_furnace electric_furnace induction_furnace metal_former
            ore_washing_plant centrifuge canner fluid_bottler industrial_workbench
            advanced_ejector_upgrade advanced_miner advanced_pulling_upgrade batch_crafter blast_furnace
            block_cutter classic_canner classic_mass_fabricator coke_kiln coke_kiln_grate
            coke_kiln_hatch ejector_upgrade electrolyzer energy_storage_upgrade fermenter
            fluid_ejector_upgrade fluid_pulling_upgrade mass_fabricator matter_generator miner
            overclocker_upgrade pattern_storage pulling_upgrade recycler redstone_inverter_upgrade
            remote_interface_upgrade replicator solar_distiller solid_canner transformer_upgrade
            uu_assembly_bench uu_scanner
            """),
    POWER(
            "power",
            "power",
            "batbox",
            """
            generator solar_generator geo_generator wind_generator water_generator batbox cesu mfe mfsu
            lv_transformer mv_transformer hv_transformer ev_transformer copper_cable
            insulated_copper_cable tin_cable insulated_tin_cable gold_cable iron_cable glass_fibre_cable
            re_battery advanced_re_battery energy_crystal lapotron_crystal advanced_charging_re_battery
            batbox_chargepad bronze_rotor bronze_rotor_blade bronze_shaft carbon_rotor
            carbon_rotor_blade cesu_chargepad charging_energy_crystal charging_lapotron_crystal
            charging_re_battery classic_electrolyzer classic_mfe classic_mfsu condenser
            creative_generator detector_cable double_insulated_gold_cable double_insulated_iron_cable
            electric_heat_generator electric_kinetic_generator energy_o_mat fluid_heat_generator
            insulated_gold_cable insulated_iron_cable iron_rotor iron_rotor_blade iron_shaft
            kinetic_generator liquid_heat_exchanger magnetizer manual_kinetic_generator mfe_chargepad
            mfsu_chargepad semifluid_generator single_use_battery solid_heat_generator splitter_cable
            steam_generator steam_kinetic_generator steam_repressurizer steam_turbine
            steam_turbine_blade steel_rotor steel_rotor_blade steel_shaft stirling_generator
            stirling_kinetic_generator tesla_coil triple_insulated_iron_cable water_kinetic_generator
            wind_kinetic_generator wooden_rotor wooden_rotor_blade
            """),
    NUCLEAR(
            "nuclear",
            "nuclear",
            "uranium_fuel_rod",
            """
            nuclear_reactor reactor_chamber reactor_vessel reactor_access_hatch reactor_fluid_port
            reactor_redstone_port uranium_fuel_rod dual_uranium_fuel_rod quad_uranium_fuel_rod
            mox_fuel_rod dual_mox_fuel_rod quad_mox_fuel_rod advanced_heat_exchanger advanced_heat_vent
            classic_nuke component_heat_exchanger component_heat_vent containment_reactor_plating
            crushed_uranium deepslate_uranium_ore depleted_dual_mox_fuel_rod
            depleted_dual_uranium_fuel_rod depleted_isotope_fuel_rod depleted_mox_fuel_rod
            depleted_quad_mox_fuel_rod depleted_quad_uranium_fuel_rod depleted_uranium_fuel_rod fuel_rod
            heat_exchanger heat_vent iodine iodine_tablet iridium_neutron_reflector lithium_fuel_rod
            lzh_condensator mox mox_pellet near_depleted_uranium neutron_reflector nuke
            overclocked_heat_vent plutonium purified_uranium raw_uranium raw_uranium_block rci_lzh
            rci_rsh re_enriched_uranium reactor_coolant_cell reactor_heat_exchanger reactor_heat_plating
            reactor_heat_vent reactor_plating rsh_condensator rt_generator rt_heat_generator rtg_pellet
            sextuple_reactor_coolant_cell small_plutonium small_uranium_235 small_uranium_238
            thick_neutron_reflector triple_reactor_coolant_cell uranium uranium_235 uranium_238
            uranium_block uranium_ingot uranium_ore uranium_pellet
            """),
    EQUIPMENT(
            "equipment",
            "equipment",
            "forge_hammer",
            """
            treetap forge_hammer cutter wrench electric_wrench meter wind_meter cropnalyzer scanner
            advanced_scanner drill diamond_drill iridium_drill chainsaw mining_laser nano_saber
            bronze_helmet bronze_chestplate bronze_leggings bronze_boots nano_helmet nano_chestplate
            nano_leggings nano_boots quantum_helmet quantum_chestplate quantum_leggings quantum_boots
            advanced_batpack alloy_chestplate batpack black_painter blue_painter bronze_axe bronze_hoe
            bronze_pickaxe bronze_shovel bronze_sword brown_painter cf_pack crowbar cyan_painter
            debug_item electric_jetpack electric_treetap energy_pack foam_sprayer frequency_transmitter
            gray_painter green_painter hazmat_chestplate hazmat_helmet hazmat_leggings heatpack jetpack
            jetpack_attachment_plate light_blue_painter light_gray_painter lime_painter magenta_painter
            night_vision_goggles obscurator orange_painter painter pink_painter purple_painter
            red_painter rubber_boots solar_helmet static_boots white_painter yellow_painter
            """),
    MATERIALS(
            "materials",
            "materials",
            "tin_ingot",
            """
            resin rubber raw_tin tin_ingot bronze_ingot steel_ingot lead_ingot silver_ingot iron_plate
            copper_plate tin_plate iron_casing copper_casing tin_casing circuit advanced_circuit machine
            advanced_machine alloy ashes bio_chaff bronze_block bronze_casing bronze_dust bronze_plate
            carbon_fibre carbon_mesh carbon_plate clay_dust coal_ball coal_block coal_chunk coal_dust
            coal_fuel_dust coil coin coke compressed_hydrated_coal compressed_plants copper_boiler
            copper_dust crushed_copper crushed_gold crushed_iron crushed_lead
            crushed_silver crushed_tin crystal_memory deepslate_lead_ore deepslate_tin_ore
            dense_bronze_plate dense_copper_plate dense_gold_plate dense_iron_plate dense_lapis_plate
            dense_lead_plate dense_obsidian_plate dense_steel_plate dense_tin_plate
            diamond_cutting_blade diamond_dust electric_motor emerald_dust ender_pearl_dust
            energium_dust gold_casing gold_dust gold_plate grin_powder heat_conductor hydrated_tin_dust
            industrial_diamond iridium iridium_ore iridium_shard iron_cutting_blade iron_dust lapis_dust
            lapis_plate lead_block lead_casing lead_dust lead_ore lead_plate lithium_dust
            mixed_metal_ingot netherrack_dust obsidian_dust obsidian_plate pellet plant_ball power_unit
            purified_copper purified_gold purified_iron purified_lead purified_silver purified_tin
            raw_crystal_memory raw_lead raw_lead_block raw_tin_block refined_iron_ingot scrap scrap_box
            silicon_dioxide_dust silver_block silver_dust slag small_bronze_dust small_copper_dust
            small_diamond_dust small_emerald_dust small_gold_dust small_iron_dust small_lapis_dust
            small_lead_dust small_lithium_dust small_obsidian_dust small_power_unit small_silver_dust
            small_sulfur_dust small_tin_dust steel_block steel_casing steel_cutting_blade steel_plate
            stone_dust sulfur_dust tin_block tin_dust tin_ore
            """),
    FLUIDS(
            "fluids",
            "fluids",
            "fluid_cell",
            """
            empty_cell basic_water_cell basic_lava_cell fluid_cell water_cell lava_cell air_cell
            biogas_cell electrolyzed_water_cell tin_can filled_tin_can empty_fuel_can filled_fuel_can air_bucket
            bio_cell biofuel_cell biogas_bucket biomass_bucket coalfuel_cell construction_foam_bucket
            coolant_bucket creosote_bucket distilled_water_bucket heavy_water_bucket hot_coolant_bucket
            hot_water_bucket hydrated_coal_cell hydration_cell hydrogen_bucket oxygen_bucket
            pahoehoe_lava_bucket steam_bucket superheated_steam_bucket uu_matter uu_matter_bucket
            weed_ex_bucket weed_ex_cell
            """),
    LOGISTICS(
            "logistics",
            "logistics",
            "bronze_storage_box",
            """
            wooden_storage_box bronze_storage_box iron_storage_box steel_storage_box iridium_storage_box
            personal_chest tank bronze_tank iron_tank steel_tank iridium_tank item_buffer pump
            broken_rubber_boat bronze_fluid_pipe_large bronze_fluid_pipe_medium bronze_fluid_pipe_small
            bronze_fluid_pipe_tiny bronze_item_pipe_large bronze_item_pipe_medium bronze_item_pipe_small
            bronze_item_pipe_tiny carbon_boat chunk_loader containment_box electric_boat
            fluid_distributor fluid_regulator mining_pipe pipe_extraction_nozzle pipe_injection_nozzle
            pump_cover_lv pump_cover_mv rubber_boat
            sorting_machine steel_fluid_pipe_large steel_fluid_pipe_medium steel_fluid_pipe_small
            steel_fluid_pipe_tiny steel_item_pipe_large steel_item_pipe_medium steel_item_pipe_small
            steel_item_pipe_tiny teleporter tool_box trade_o_mat weighted_fluid_distributor
            weighted_item_distributor
            """),
    WORLD(
            "world",
            "world",
            "rubber_sapling",
            """
            rubber_sapling rubber_log rubber_wood rubber_leaves rubber_planks crop_stick crop_seed_bag
            fertilizer cropmatron crop_harvester wooden_scaffold iron_scaffold reinforced_stone
            reinforced_glass basalt black_wall blank_tfbp blue_wall bobs_yer_uncle_ranks_berry
            brown_wall cf_powder chilling_tfbp classic_cropmatron coffee_beans coffee_mug coffee_powder
            cold_coffee_mug cultivation_tfbp cyan_wall dark_coffee_mug desertification_tfbp empty_mug
            flatification_tfbp foam gray_wall green_wall hops iron_fence irrigation_tfbp itnt lava_sheet
            light_blue_wall light_gray_wall lime_wall magenta_wall milk_wart mushroom_tfbp oil_berry
            orange_wall pink_wall purple_wall red_wall refractory_bricks reinforced_door
            reinforced_iron_scaffold reinforced_wooden_scaffold resin_sheet rubber_button rubber_door
            rubber_fence rubber_fence_gate rubber_pressure_plate rubber_sheet rubber_sign rubber_slab
            rubber_stairs rubber_trapdoor stripped_rubber_log stripped_rubber_wood terra_wart
            terraformer water_sheet weed white_wall wool_sheet yellow_wall
            """);

    private static final Map<String, CreativeCategory> BY_ITEM = index();

    private final String group;
    private final String registryPath;
    private final String icon;
    private final List<String> entries;

    CreativeCategory(String group, String registryPath, String icon, String entries) {
        this.group = group;
        this.registryPath = registryPath;
        this.icon = icon;
        this.entries = List.of(entries.strip().split("\\s+"));
    }

    /** Forge 1.19 tab label, translated via the native itemGroup prefix. */
    public String label() {
        return "ic2." + group;
    }

    /** Native tab registration path on platforms with a creative-tab registry. */
    public String registryPath() {
        return registryPath;
    }

    /** Registry path of the item used for the tab icon. */
    public String icon() {
        return icon;
    }

    /** Ordered IC2 item paths that belong to the tab. */
    public List<String> entries() {
        return entries;
    }

    /** Finds the creative tab for a registered IC2 item path. */
    public static CreativeCategory forItem(String path) {
        var group = BY_ITEM.get(path);
        if (group == null)
            throw new IllegalArgumentException("No IC2 creative category for " + path);
        return group;
    }

    private static Map<String, CreativeCategory> index() {
        Map<String, CreativeCategory> groups = new HashMap<>();
        for (var category : values()) {
            for (var path : category.entries) {
                if (groups.putIfAbsent(path, category) != null) {
                    throw new IllegalStateException("Duplicate IC2 creative entry " + path);
                }
            }
        }
        return Map.copyOf(groups);
    }
}
