package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.shocktest.CableShockChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runs the live-wire matrix in the Forge dedicated GameTest server. */
@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class CableShockGameTests {
    private CableShockGameTests() {}

    /** Proves the delivered packet, damage source and collision range together. */
    @GameTest(template = "smoke")
    public static void bareCableDamageUsesDeliveredEnergy(GameTestHelper helper) {
        var nearby = helper.absolutePos(BlockPos.ZERO);
        var origin = new BlockPos((nearby.getX() & ~15) + 7, 96, (nearby.getZ() & ~15) + 7);
        var checks =
                new CableShockChecks(
                        helper.getLevel(),
                        origin,
                        new CableShockChecks.Host(
                                PlayableGameTests::block,
                                id -> PlayableGameTests.stack(id).getItem(),
                                (pos, eu) -> {
                                    var block = PlayableGameTests.machine(helper, pos);
                                    var saved = block.saveWithoutMetadata();
                                    saved.putDouble("ic2_energy", eu);
                                    block.load(saved);
                                    block.setChanged();
                                },
                                pos ->
                                        PlayableGameTests.machine(helper, pos)
                                                .saveWithoutMetadata()
                                                .getDouble("ic2_energy"),
                                EntityType.IRON_GOLEM::create,
                                damage -> damage == DamageSource.MAGIC));
        checks.arrange();
        helper.runAfterDelay(
                3, () -> checks.connect((name, ok) -> PlayableGameTests.check(helper, ok, name)));
        helper.runAfterDelay(
                8,
                () -> {
                    checks.verify((name, ok) -> PlayableGameTests.check(helper, ok, name));
                    helper.succeed();
                });
    }
}
