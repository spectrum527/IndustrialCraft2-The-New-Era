package com.industrialage.ia2.forge.qa;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Submit the observed Classic and survival-access patterns through a vanilla server crafting grid.
 */
@net.minecraftforge.gametest.GameTestHolder("ic2_qa")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class ClassicCraftChecks {
    @net.minecraft.gametest.framework.GameTest(template = "smoke")
    public static void craftClassicRecipes(net.minecraft.gametest.framework.GameTestHelper helper) {
        run(helper.getLevel(), (id, ok) -> PlayableGameTests.check(helper, ok, id));
        helper.succeed();
    }

    private ClassicCraftChecks() {}

    static void run(ServerLevel level, BiConsumer<String, Boolean> check) {
        PipeGeometryChecks.run(
                level,
                path ->
                        net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                                new net.minecraft.resources.ResourceLocation("ic2", path)));
        check.accept("all-1024-physical-pipe-shapes", true);
        var player = FakePlayerFactory.getMinecraft(level);
        var source =
                Objects.requireNonNull(
                        ClassicCraftChecks.class.getResourceAsStream("/classic-recipes.json"));
        try (var reader = new InputStreamReader(source, StandardCharsets.UTF_8)) {
            for (JsonElement element :
                    JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("recipes")) {
                var recipe = element.getAsJsonObject();
                String result = recipe.get("output").getAsString();
                var grid =
                        new CraftingMenu(
                                55,
                                player.getInventory(),
                                ContainerLevelAccess.create(level, new BlockPos(5, 150, 5)));
                if (recipe.has("pattern")) {
                    int row = 0;
                    for (var line : recipe.getAsJsonArray("pattern")) {
                        char[] chars = line.getAsString().toCharArray();
                        for (int x = 0; x < chars.length; x++)
                            if (chars[x] != ' ')
                                grid.getSlot(1 + row * 3 + x)
                                        .set(
                                                sampleIngredient(
                                                        result,
                                                        recipe.getAsJsonObject("key")
                                                                .get(Character.toString(chars[x]))
                                                                .getAsString()));
                        row++;
                    }
                } else {
                    int i = 1;
                    for (var ingredient : recipe.getAsJsonArray("ingredients"))
                        grid.getSlot(i++).set(sampleIngredient(result, ingredient.getAsString()));
                }
                grid.slotsChanged(grid.getSlot(1).container);
                var actual = grid.getSlot(0).getItem();
                check.accept(
                        "craft-" + result,
                        actual.is(sample("ic2:" + result).getItem())
                                && actual.getCount() == recipe.get("count").getAsInt());
            }
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static ItemStack sampleIngredient(String output, String id) {
        // The observed layouts are retained, with the native shell required by this release.
        if (id.equals("ic2:empty_cell")) {
            id =
                    switch (output) {
                        case "near_depleted_uranium",
                                "bio_cell",
                                "hydrated_coal_cell",
                                "weed_ex_cell" ->
                                "ic2:fluid_cell";
                        default -> id;
                    };
        }
        return sample(id);
    }

    private static ItemStack sample(String id) {
        var registry = ForgeRegistries.ITEMS;
        return new ItemStack(
                java.util.stream.StreamSupport.stream(registry.spliterator(), false)
                        .filter(item -> registry.getKey(item).toString().equals(id))
                        .findFirst()
                        .orElseThrow());
    }
}
