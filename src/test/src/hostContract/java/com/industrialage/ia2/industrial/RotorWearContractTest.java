package com.industrialage.ia2.industrial;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Host-adapter contracts for native wind/water rotor wear, steam turbine non-wear, and tag lookup.
 */
class RotorWearContractTest {
    @Test
    void allFiveRotorsDeclareExactDurabilityMaxSafeWindAndWaterEligibility() {
        assertEquals(10_800, WindRotorMount.durability("wooden"));
        assertEquals(86_400, WindRotorMount.durability("bronze"));
        assertEquals(86_400, WindRotorMount.durability("iron"));
        assertEquals(172_800, WindRotorMount.durability("steel"));
        assertEquals(604_800, WindRotorMount.durability("carbon"));
        assertEquals(0, WindRotorMount.durability("steam_turbine"));

        assertEquals(60, WindRotorMount.Material.WOOD.maxSafeWind);
        assertEquals(75, WindRotorMount.Material.BRONZE.maxSafeWind);
        assertEquals(75, WindRotorMount.Material.IRON.maxSafeWind);
        assertEquals(90, WindRotorMount.Material.STEEL.maxSafeWind);
        assertEquals(110, WindRotorMount.Material.CARBON.maxSafeWind);

        assertFalse(WindRotorMount.Material.WOOD.waterAllowed);
        assertTrue(WindRotorMount.Material.BRONZE.waterAllowed);
        assertTrue(WindRotorMount.Material.IRON.waterAllowed);
        assertTrue(WindRotorMount.Material.STEEL.waterAllowed);
        assertTrue(WindRotorMount.Material.CARBON.waterAllowed);
    }

    @Test
    void rotorWearsOnePerTwentyActiveTicksAtSafeWindAndFourAboveMaxSafeWind() {
        int maxDamage = WindRotorMount.durability("wooden");
        int wearTicks = 0;
        int damage = 0;

        // First 19 active ticks accumulate counter without damaging the rotor.
        for (int tick = 1; tick <= 19; tick++) {
            var outcome = WorkshopKinetic.advanceRotorWear(wearTicks, damage, maxDamage, false);
            assertEquals(tick, outcome.nextWearTicks());
            assertEquals(0, outcome.nextDamage());
            assertFalse(outcome.broke());
            wearTicks = outcome.nextWearTicks();
            damage = outcome.nextDamage();
        }

        // 20th active tick at safe wind applies 1x wear.
        var normalTick = WorkshopKinetic.advanceRotorWear(wearTicks, damage, maxDamage, false);
        assertEquals(0, normalTick.nextWearTicks());
        assertEquals(1, normalTick.nextDamage());
        assertFalse(normalTick.broke());

        // 20 active ticks above maxSafeWind apply 4x wear.
        wearTicks = normalTick.nextWearTicks();
        damage = normalTick.nextDamage();
        for (int tick = 1; tick <= 20; tick++) {
            var outcome = WorkshopKinetic.advanceRotorWear(wearTicks, damage, maxDamage, true);
            wearTicks = outcome.nextWearTicks();
            damage = outcome.nextDamage();
            assertFalse(outcome.broke());
        }
        assertEquals(0, wearTicks);
        assertEquals(5, damage);
    }

    @Test
    void rotorBreaksAtZeroRemainingDurabilityAndSteamTurbineNeverWears() {
        int maxDamage = WindRotorMount.durability("wooden");

        // Normal 1x break at exact last point of durability.
        var normalBreak = WorkshopKinetic.advanceRotorWear(19, maxDamage - 1, maxDamage, false);
        assertTrue(normalBreak.broke());
        assertEquals(0, normalBreak.nextWearTicks());
        assertEquals(maxDamage, normalBreak.nextDamage());

        // 4x over-wind break when fewer than 4 durability remain.
        var stormBreak = WorkshopKinetic.advanceRotorWear(19, maxDamage - 2, maxDamage, true);
        assertTrue(stormBreak.broke());
        assertEquals(0, stormBreak.nextWearTicks());
        assertEquals(maxDamage, stormBreak.nextDamage());

        // Steam turbine has maxDamage == 0 and must never accumulate wear or break.
        int turbineMax = WindRotorMount.durability("steam_turbine");
        assertEquals(0, turbineMax);
        for (int i = 0; i < 100; i++) {
            var outcome = WorkshopKinetic.advanceRotorWear(0, 0, turbineMax, true);
            assertEquals(0, outcome.nextWearTicks());
            assertEquals(0, outcome.nextDamage());
            assertFalse(outcome.broke());
        }
    }

    @Test
    void workshopKnownTagMembersSupportCommonAndForgeAliases() {
        assertTrue(WorkshopRecipes.knownMember("forge:ingots/tin", "ic2:tin_ingot"));
        assertTrue(WorkshopRecipes.knownMember("c:ingots/tin", "ic2:tin_ingot"));
        assertTrue(WorkshopRecipes.knownMember("forge:raw_ores/tin", "ic2:raw_tin"));
        assertTrue(WorkshopRecipes.knownMember("c:raw_materials/tin", "ic2:raw_tin"));
        assertTrue(WorkshopRecipes.knownMember("forge:blocks/bronze", "ic2:bronze_block"));
        assertTrue(WorkshopRecipes.knownMember("c:storage_blocks/bronze", "ic2:bronze_block"));
        assertFalse(WorkshopRecipes.knownMember("c:ingots/tin", "minecraft:copper_ingot"));
    }
}
