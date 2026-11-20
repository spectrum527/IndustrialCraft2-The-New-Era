package com.industrialage.ia2.gametest;

import com.industrialage.ia2.equipment.MeterSampling;
import com.industrialage.ia2.gameplay.EnergyNetwork;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Records a real one-half-EU delivery between native storage blocks. */
final class MeterChecks {
    private static final BlockPos SOURCE = new BlockPos(19, 108, 18);
    private static final BlockPos RECEIVER = SOURCE.east();
    private MeterSampling.Watch in;
    private MeterSampling.Watch out;

    void setup(ServerLevel level, BiConsumer<String, Boolean> check) {
        var player = FakePlayerFactory.getMinecraft(level);
        var emitter = new ItemStack(NativeGameplayFixture.item("batbox"));
        emitter.set(EnergyTierChecks.energyComponent(), 32.5);
        NativeGameplayFixture.place(level, player, "batbox", SOURCE, emitter);
        var receiver = new ItemStack(NativeGameplayFixture.item("batbox"));
        receiver.set(EnergyTierChecks.energyComponent(), 39_999.5);
        NativeGameplayFixture.place(level, player, "batbox", RECEIVER, receiver);
        check.accept("meter-rejects-plain-stone", !EnergyNetwork.metered(level, RECEIVER.below()));
        in = MeterSampling.watch(level, RECEIVER);
        out = MeterSampling.watch(level, SOURCE);
    }

    boolean tick(BiConsumer<String, Boolean> check) {
        if (in == null) return true;
        var read = in.read(0);
        if (read.max() == 0) return false;
        check.accept("meter-keeps-half-eu", read.max() == 0.5 && out.read(1).max() == 0.5);
        check.accept("meter-measures-received-voltage-tier", in.read(3).max() == 1);
        check.accept(
                "meter-reports-net-direction",
                in.read(2).max() == 0.5 && out.read(2).min() == -0.5);
        in.close();
        out.close();
        in = null;
        return true;
    }
}
