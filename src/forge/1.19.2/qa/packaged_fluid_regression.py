"""Exercise cell consumption and returned containers on an installed Forge production JAR."""

import argparse
from hashlib import sha256
import json
from pathlib import Path
import re
import shutil
import time
from zipfile import ZipFile

from packaged_playable import boot, items


def digest(path):
    return sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('jar', 'libraries', 'eula', 'output'):
        parser.add_argument('--' + name, required=True, type=Path)
    parser.add_argument('--java', default='java')
    parser.add_argument('--sha256', required=True, help='Expected SHA-256 of the frozen production JAR')
    args = parser.parse_args()
    expected = args.sha256.lower()
    if not re.fullmatch(r'[0-9a-f]{64}', expected):
        parser.error('--sha256 must be a 64-character SHA-256 digest')
    if digest(args.jar) != expected:
        parser.error('Production JAR does not match --sha256')
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    if 'eula=true' not in args.eula.read_text().splitlines():
        parser.error('An accepted Forge EULA is required')
    with ZipFile(args.jar) as archive:
        names = archive.namelist()
        if any('/forge/qa/' in name for name in names):
            parser.error('Use the production JAR, not the QA fixture')
        labels = json.loads(archive.read('assets/ic2/lang/zh_cn.json'))
        for key, value in {'item.ic2.water_cell':'水单元', 'item.ic2.lava_cell':'熔岩单元',
                           'item.ic2.air_cell':'压缩空气单元', 'item.ic2.fluid_cell':'通用流体单元'}.items():
            if labels.get(key) != value:
                raise AssertionError(f'Incorrect packaged Chinese label {key}: {labels.get(key)}')
    (output/'libraries').symlink_to(args.libraries.resolve(), target_is_directory=True)
    shutil.copy2(args.eula, output/'eula.txt')
    (output/'mods').mkdir()
    installed = output/'mods'/args.jar.name
    shutil.copy2(args.jar, installed)
    fingerprint = digest(installed)
    if fingerprint != expected or digest(args.jar) != expected:
        raise AssertionError('Installed JAR differs from candidate')
    (output/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\n'
        'level-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\n')
    command = [args.java, '-Xmx1400M', '-XX:ActiveProcessorCount=4',
               '@libraries/net/minecraftforge/forge/1.19.2-43.5.0/unix_args.txt', 'nogui']
    single, queued, no_power = '2 150 2', '4 150 2', '6 150 2'
    wrong, blocked, partial, geo7000, geo7001 = ('2 150 6', '4 150 6', '6 150 6', '8 150 6', '10 150 6')
    semi, heater, furnace = '14 150 8', '18 150 8', '19 150 8'
    idle_heater, idle_furnace, full_semi = '22 150 8', '24 150 8', '26 150 8'
    with boot(output, command, 'fluid-regression') as server:
        server.send('gamerule doMobSpawning false', 'forceload add 0 0 31 31',
            'fill 0 149 0 22 149 11 minecraft:stone',
            f'setblock {single} ic2:ore_washing_plant',
            f'data merge block {single} {{ic2_energy:16000.0d,Items:[{{Slot:0b,id:"ic2:crushed_copper",Count:11b}},{{Slot:6b,id:"ic2:water_cell",Count:1b}}]}}',
            f'setblock {queued} ic2:ore_washing_plant',
            f'data merge block {queued} {{Items:[{{Slot:6b,id:"ic2:water_cell",Count:9b}}]}}',
            f'setblock {no_power} ic2:ore_washing_plant',
            f'data merge block {no_power} {{Items:[{{Slot:0b,id:"ic2:crushed_copper",Count:1b}},{{Slot:6b,id:"ic2:water_cell",Count:1b}}]}}',
            f'setblock {wrong} ic2:geo_generator',
            f'data merge block {wrong} {{Items:[{{Slot:0b,id:"ic2:water_cell",Count:1b}}]}}',
            f'setblock {blocked} ic2:geo_generator',
            f'data merge block {blocked} {{Items:[{{Slot:0b,id:"ic2:lava_cell",Count:1b}},{{Slot:1b,id:"minecraft:stone",Count:64b}}]}}',
            f'setblock {partial} ic2:geo_generator',
            f'data merge block {partial} {{Items:[{{Slot:0b,id:"ic2:fluid_cell",Count:1b,tag:{{Fluid:{{FluidName:"minecraft:lava",Amount:250}}}}}}]}}',
            f'setblock {geo7000} ic2:geo_generator',
            f'data merge block {geo7000} {{eu:2400.0d,millibuckets:7000,Items:[{{Slot:0b,id:"ic2:lava_cell",Count:1b}}]}}',
            f'setblock {geo7001} ic2:geo_generator',
            f'data merge block {geo7001} {{eu:2400.0d,millibuckets:7001,Items:[{{Slot:0b,id:"ic2:lava_cell",Count:1b}}]}}',
            f'setblock {semi} ic2:semifluid_generator',
            f'data merge block {semi} {{ic2_input_fluid:{{FluidName:"ic2:biogas",Amount:1000}},Items:[{{Slot:0b,id:"ic2:fluid_cell",Count:1b,tag:{{Fluid:{{FluidName:"ic2:biogas",Amount:1000}}}}}}]}}',
            f'setblock {heater} ic2:fluid_heat_generator[facing=east]',
            f'data merge block {heater} {{Items:[{{Slot:0b,id:"ic2:lava_cell",Count:2b}}]}}',
            f'setblock {furnace} ic2:blast_furnace[facing=west]',
            f'data merge block {furnace} {{ic2_heat:60000,Items:[{{Slot:0b,id:"minecraft:iron_ingot",Count:1b}},{{Slot:4b,id:"ic2:air_cell",Count:2b}}]}}',
            f'setblock {idle_heater} ic2:fluid_heat_generator',
            f'data merge block {idle_heater} {{Items:[{{Slot:0b,id:"ic2:lava_cell",Count:11b}}]}}',
            f'setblock {idle_furnace} ic2:blast_furnace',
            f'data merge block {idle_furnace} {{Items:[{{Slot:4b,id:"ic2:air_cell",Count:9b}}]}}',
            f'setblock {full_semi} ic2:semifluid_generator',
            f'data merge block {full_semi} {{ic2_energy:16000.0d,ic2_input_fluid:{{FluidName:"ic2:biogas",Amount:9000}},'
            'Items:[{Slot:0b,id:"ic2:fluid_cell",Count:1b,tag:{Fluid:{FluidName:"ic2:biogas",Amount:1000}}}]}',
            'scoreboard objectives add ic2fluid dummy', 'say IC2_FLUID_FIXTURE_READY')
        if not server.wait('[Server] IC2_FLUID_FIXTURE_READY', 20):
            raise AssertionError('World setup did not finish')
        time.sleep(3)
        server.send(f'execute store result score #geo250 ic2fluid run data get block {partial} millibuckets',
                    f'execute store result score #geoEU ic2fluid run data get block {partial} eu',
                    f'execute store result score #semi ic2fluid run data get block {semi} ic2_input_fluid.Amount',
                    f'execute store result score #semiEU ic2fluid run data get block {semi} ic2_energy',
                    f'execute store result score #air ic2fluid run data get block {furnace} ic2_input_fluid.Amount',
                    f'execute store result score #heat ic2fluid run data get block {heater} ic2_input_fluid.Amount')
        server.require('idle_unpowered_washer_prefills_eight_of_nine', items(queued, 'ic2:water_cell', 1, 6),
                       items(queued, 'ic2:fluid_cell', 8, 14),
                       f'if data block {queued} {{ic2_input_fluid:{{Amount:8000}}}}',
                       f'unless data block {no_power} Items[{{Slot:6b}}]', items(no_power, 'ic2:fluid_cell', 1, 14),
                       f'if data block {no_power} {{ic2_input_fluid:{{Amount:1000}}}}')
        server.require('geothermal_wrong_and_blocked_do_not_consume', items(wrong, 'ic2:water_cell', 1),
                       items(blocked, 'ic2:lava_cell', 1), f'if data block {wrong} {{millibuckets:0}}',
                       f'if data block {blocked} {{millibuckets:0}}')
        server.require('saturated_geothermal_7000_fills_but_7001_retains_cell',
                       f'unless data block {geo7000} Items[{{Slot:0b}}]', items(geo7000, 'ic2:fluid_cell', 1, 1),
                       f'if data block {geo7000} {{millibuckets:8000}}', items(geo7001, 'ic2:lava_cell', 1),
                       f'if data block {geo7001} {{millibuckets:7001}}', f'unless data block {geo7001} Items[{{Slot:1b}}]')
        server.require('partial_lava_cell_returns_and_generates', f'unless data block {partial} Items[{{Slot:0b}}]',
                       items(partial, 'ic2:fluid_cell', 1, 1), 'if score #geo250 ic2fluid matches 1..249',
                       'if score #geoEU ic2fluid matches 1..')
        server.require('semifluid_prefills_queued_cell_and_generates', f'unless data block {semi} Items[{{Slot:0b}}]',
                       items(semi, 'ic2:fluid_cell', 1, 9), 'if score #semi ic2fluid matches 1001..1999',
                       'if score #semiEU ic2fluid matches 1..')
        server.require('heated_furnace_and_heater_prefill_both_cells', f'unless data block {furnace} Items[{{Slot:4b}}]',
                       items(furnace, 'ic2:fluid_cell', 2, 13), 'if score #air ic2fluid matches 1001..1999',
                       f'unless data block {heater} Items[{{Slot:0b}}]', items(heater, 'ic2:fluid_cell', 2, 9),
                       'if score #heat ic2fluid matches 1001..1999')
        server.require('unconnected_heater_fills_10000_and_keeps_eleventh',
                       f'if data block {idle_heater} {{ic2_input_fluid:{{Amount:10000}}}}',
                       items(idle_heater, 'ic2:lava_cell', 1, 0), items(idle_heater, 'ic2:fluid_cell', 10, 9))
        server.require('cold_furnace_fills_8000_and_keeps_ninth',
                       f'if data block {idle_furnace} {{ic2_input_fluid:{{Amount:8000}}}}',
                       items(idle_furnace, 'ic2:air_cell', 1, 4), items(idle_furnace, 'ic2:fluid_cell', 8, 13))
        server.require('saturated_semifluid_fills_to_10000',
                       f'if data block {full_semi} {{ic2_energy:16000.0d,ic2_input_fluid:{{Amount:10000}}}}',
                       f'unless data block {full_semi} Items[{{Slot:0b}}]', items(full_semi, 'ic2:fluid_cell', 1, 9))
        server.send(f'data merge block {full_semi} {{Items:[{{Slot:0b,id:"ic2:fluid_cell",Count:1b,'
                    'tag:{Fluid:{FluidName:"ic2:biogas",Amount:1000}}}]}')
        time.sleep(1)
        server.require('full_semifluid_preserves_next_whole_cell',
                       f'if data block {full_semi} {{ic2_input_fluid:{{Amount:10000}},Items:[{{Slot:0b,'
                       'id:"ic2:fluid_cell",tag:{Fluid:{FluidName:"ic2:biogas",Amount:1000}}}]}',
                       f'unless data block {full_semi} Items[{{Slot:9b}}]')
        server.send(f'data merge block {queued} {{ic2_energy:16000.0d,Items:[{{Slot:0b,id:"ic2:crushed_copper",Count:1b}},'
                    '{Slot:6b,id:"ic2:water_cell",Count:1b},{Slot:14b,id:"ic2:fluid_cell",Count:8b}]}')
        server.require('full_washer_starts_work_with_ninth_cell',
                       items(queued, 'ic2:water_cell', 1, 6), items(queued, 'ic2:fluid_cell', 8, 14),
                       f'if data block {queued} {{ic2_input_fluid:{{Amount:8000}}}}',
                       f'unless data block {queued} {{ic2_progress:0}}')
        server.send(f'data merge block {queued} {{ic2_progress:499}}')
        server.require('full_washer_one_job_leaves_7900_and_ninth_cell',
                       items(queued, 'ic2:purified_copper', 1, 9), items(queued, 'ic2:water_cell', 1, 6),
                       items(queued, 'ic2:fluid_cell', 8, 14),
                       f'if data block {queued} {{ic2_input_fluid:{{Amount:7900}}}}')
        server.require('single_cell_starts_real_processing',
                       items(single, 'ic2:fluid_cell', 1, 14),
                       f'if data block {single} {{ic2_input_fluid:{{FluidName:"minecraft:water",Amount:1000}}}}',
                       f'unless data block {single} {{ic2_progress:0}}')
        for completed in range(1, 11):
            # Existing GameTest and Neo native tests verify natural cycle duration separately.
            server.send(f'data merge block {single} {{ic2_energy:16000.0d,ic2_progress:499}}')
            fluid = (f'if data block {single} {{ic2_input_fluid:{{FluidName:"minecraft:water",Amount:{1000-completed*100}}}}}'
                     if completed < 10 else f'unless data block {single} ic2_input_fluid')
            server.require(f'washer_batch_{completed}_uses_100mb',
                           items(single, 'ic2:purified_copper', completed, 9),
                           items(single, 'ic2:fluid_cell', 1, 14), fluid)
        time.sleep(1)
        server.require('eleventh_job_waits_without_water', items(single, 'ic2:crushed_copper', 1, 0),
                       f'if data block {single} {{ic2_progress:0}}', f'unless data block {single} ic2_input_fluid')
        # Use an actual fluid-mode reactor shell and six chambers.  Its dedicated coolant
        # input must preserve the full universal cell when the cold tank lacks one mB.
        core, port, bottler = '48 150 48', '48 152 48', '52 150 48'
        server.send('forceload add 32 32 63 63',
                    'fill 46 148 46 50 152 50 ic2:reactor_vessel hollow',
                    f'setblock {core} ic2:nuclear_reactor',
                    'setblock 47 150 48 ic2:reactor_chamber', 'setblock 49 150 48 ic2:reactor_chamber',
                    'setblock 48 149 48 ic2:reactor_chamber', 'setblock 48 151 48 ic2:reactor_chamber',
                    'setblock 48 150 47 ic2:reactor_chamber', 'setblock 48 150 49 ic2:reactor_chamber',
                    f'setblock {port} ic2:reactor_fluid_port',
                    f'setblock {bottler} ic2:fluid_bottler',
                    f'data merge block {bottler} {{ic2_energy:400.0d,ic2_input_fluid:{{FluidName:"minecraft:water",Amount:7751}},'
                    'Items:[{Slot:1b,id:"ic2:fluid_cell",Count:1b,tag:{Fluid:{FluidName:"minecraft:water",Amount:250}}}]}')
        time.sleep(2)
        server.send(f'data merge block {core} {{coolant:9751,Items:[{{Slot:54b,id:"ic2:fluid_cell",Count:1b,'
                    'tag:{Fluid:{FluidName:"ic2:coolant",Amount:250}}}]}')
        time.sleep(1)
        server.state(core, bottler)
        server.require('reactor_249_gap_preserves_250_coolant',
                       f'if data block {core} {{coolant:9751,Items:[{{Slot:54b,id:"ic2:fluid_cell",'
                       'tag:{Fluid:{FluidName:"ic2:coolant",Amount:250}}}]}',
                       f'unless data block {core} Items[{{Slot:55b}}]')
        server.require('bottler_249_gap_preserves_250_water',
                       f'if data block {bottler} {{ic2_input_fluid:{{Amount:7751}},Items:[{{Slot:1b,'
                       'id:"ic2:fluid_cell",tag:{Fluid:{FluidName:"minecraft:water",Amount:250}}}]}',
                       f'unless data block {bottler} Items[{{Slot:9b}}]')
        server.send(f'data merge block {core} {{coolant:9750}}',
                    f'data merge block {bottler} {{ic2_input_fluid:{{FluidName:"minecraft:water",Amount:7750}}}}')
        server.require('reactor_250_gap_accepts_250_coolant',
                       f'if data block {core} {{coolant:10000}}', f'unless data block {core} Items[{{Slot:54b}}]',
                       items(core, 'ic2:fluid_cell', 1, 55))
        # The bottler is a timed job; retain that machinery and let it complete normally.
        server.require('bottler_250_gap_completes_emptying_job',
                       f'if data block {bottler} {{ic2_input_fluid:{{Amount:8000}}}}',
                       f'unless data block {bottler} Items[{{Slot:1b}}]', items(bottler, 'ic2:fluid_cell', 1, 9))
        server.send(f'data merge block {core} {{coolant:9001,Items:[{{Slot:54b,id:"ic2:fluid_cell",Count:1b,'
                    'tag:{Fluid:{FluidName:"ic2:coolant",Amount:1000}}}]}')
        time.sleep(1)
        server.require('reactor_999_gap_preserves_1000_coolant',
                       f'if data block {core} {{coolant:9001,Items:[{{Slot:54b,id:"ic2:fluid_cell",'
                       'tag:{Fluid:{FluidName:"ic2:coolant",Amount:1000}}}]}',
                       f'unless data block {core} Items[{{Slot:55b}}]')
        server.send(f'data merge block {core} {{coolant:9000}}')
        server.require('reactor_1000_gap_accepts_1000_coolant',
                       f'if data block {core} {{coolant:10000}}', f'unless data block {core} Items[{{Slot:54b}}]',
                       items(core, 'ic2:fluid_cell', 1, 55))
        server.state(single, queued, wrong, blocked, partial, geo7000, geo7001, semi, heater, furnace,
                     idle_heater, idle_furnace, full_semi, core, bottler)
        checks = list(server.checks)
    if digest(installed) != fingerprint or digest(args.jar) != fingerprint:
        raise AssertionError('Production JAR changed during the run')
    (output/'result.json').write_text(json.dumps({'status':'passed', 'jar':str(args.jar.resolve()),
        'sha256':fingerprint, 'checks':checks, 'chineseLabels':'passed'}, ensure_ascii=False, indent=2)+'\n')
    print(f'PASS {len(checks)} checks; {fingerprint}')


if __name__ == '__main__':
    main()
