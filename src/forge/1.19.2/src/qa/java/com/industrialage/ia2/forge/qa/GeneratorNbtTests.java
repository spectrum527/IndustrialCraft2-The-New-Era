package com.industrialage.ia2.forge.qa;

import com.industrialage.ia2.machine.FuelGenerator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Persistence contracts exercise the actual Forge block entity's public load and save entry points.
 */
@GameTestHolder("ic2_qa")
@PrefixGameTestTemplate(false)
public final class GeneratorNbtTests {
    private static final BlockPos POS = new BlockPos(19, 72, -11);

    private static GeneratorProbe probe() {
        return new GeneratorProbe(BlockEntityType.FURNACE, POS, Blocks.FURNACE.defaultBlockState());
    }

    private static CompoundTag fields(Tag ticks, Tag eu) {
        var body = new CompoundTag();
        if (ticks != null) body.put("fuel_ticks", ticks);
        if (eu != null) body.put("stored_energy", eu);
        return body;
    }

    private static CompoundTag nativeTag(Tag value) {
        var root = new CompoundTag();
        root.put("generator", value);
        return root;
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    /**
     * Missing machine data resets even a previously populated instance and writes both native
     * types.
     */
    @GameTest(template = "smoke")
    public static void generatorNewState(GameTestHelper test) {
        var machine = probe();
        machine.load(nativeTag(fields(IntTag.valueOf(17), DoubleTag.valueOf(42.5))));
        machine.load(new CompoundTag());
        require(machine.savedState().equals(new FuelGenerator.Snapshot(0, 0)), "new snapshot");
        var saved = machine.saveWithFullMetadata().getCompound("generator");
        require(IntTag.valueOf(0).equals(saved.get("fuel_ticks")), "new integer fuel");
        require(DoubleTag.valueOf(0).equals(saved.get("stored_energy")), "new double EU");
        test.succeed();
    }

    /** Serializes fractional EU through a binary NBT file and accepts both numeric limits. */
    @GameTest(template = "smoke")
    public static void generatorFractionalNbt(GameTestHelper test) throws IOException {
        var source = probe();
        source.load(nativeTag(fields(IntTag.valueOf(123), DoubleTag.valueOf(1200.5))));
        var file = new File("generator-fractional.nbt");
        NbtIo.writeCompressed(source.saveWithFullMetadata(), file);
        var data = NbtIo.readCompressed(file);
        var saved = data.getCompound("generator");
        require(IntTag.valueOf(123).equals(saved.get("fuel_ticks")), "integer fuel tag");
        require(DoubleTag.valueOf(1200.5).equals(saved.get("stored_energy")), "fractional EU tag");
        var target = probe();
        target.load(data);
        require(
                target.savedState().equals(new FuelGenerator.Snapshot(123, 1200.5)),
                "binary NBT reload");
        target.load(nativeTag(fields(IntTag.valueOf(Integer.MAX_VALUE), DoubleTag.valueOf(4000))));
        require(
                target.savedState().equals(new FuelGenerator.Snapshot(Integer.MAX_VALUE, 4000)),
                "upper limits");
        target.load(nativeTag(fields(IntTag.valueOf(0), DoubleTag.valueOf(-0.0))));
        require(target.savedState().storedEnergy() == 0, "negative zero");
        test.succeed();
    }

    /**
     * Each bad native payload must diagnose its position, leave the input intact and preserve
     * state.
     */
    @GameTest(template = "smoke")
    public static void generatorCorruptNbt(GameTestHelper test) throws IOException {
        var one = IntTag.valueOf(1);
        var ten = DoubleTag.valueOf(10);
        record Bad(String name, Tag data) {}
        var cases =
                List.of(
                        new Bad("not-compound", StringTag.valueOf("x")),
                        new Bad("empty", fields(null, null)),
                        new Bad("missing-ticks", fields(null, ten)),
                        new Bad("missing-eu", fields(one, null)),
                        new Bad("string-ticks", fields(StringTag.valueOf("1"), ten)),
                        new Bad("string-eu", fields(one, StringTag.valueOf("10"))),
                        new Bad("fractional-ticks", fields(DoubleTag.valueOf(1.5), ten)),
                        new Bad("negative-ticks", fields(IntTag.valueOf(-1), ten)),
                        new Bad("overflow", fields(LongTag.valueOf(2147483648L), ten)),
                        new Bad("nan-ticks", fields(DoubleTag.valueOf(Double.NaN), ten)),
                        new Bad("negative-eu", fields(one, DoubleTag.valueOf(-0.01))),
                        new Bad("over-capacity", fields(one, DoubleTag.valueOf(4000.01))),
                        new Bad("nan-eu", fields(one, DoubleTag.valueOf(Double.NaN))),
                        new Bad(
                                "infinite-eu",
                                fields(one, DoubleTag.valueOf(Double.POSITIVE_INFINITY))));
        var machine = probe();
        machine.load(nativeTag(fields(IntTag.valueOf(17), DoubleTag.valueOf(42.5))));
        var previous = machine.savedState();
        var rejected = new ArrayList<String>();
        for (var sample : cases) {
            var input = nativeTag(sample.data());
            var original = input.copy();
            try {
                machine.load(input);
                throw new AssertionError("Accepted corrupt " + sample.name());
            } catch (IllegalStateException expected) {
                require(
                        expected.getMessage().contains("Invalid generator state at " + POS),
                        sample.name());
                require(
                        input.equals(original) && machine.savedState().equals(previous),
                        sample.name());
                rejected.add(sample.name());
            }
        }
        Files.write(Path.of("generator-rejections.txt"), rejected);
        test.succeed();
    }
}
