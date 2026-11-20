package com.industrialage.ia2.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/** Native crafting lookup with its per-square container and tool remainders. */
public final class WorldCrafting {
    private WorldCrafting() {}

    /** Resolves a nine-square grid against this world's active recipe data. */
    public static Result result(ServerLevel level, List<ItemStack> stacks) {
        var positioned = CraftingInput.ofPositioned(3, 3, stacks);
        var input = positioned.input();
        var manager = level.getServer().getRecipeManager();
        var match = manager.getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
        if (match == null) return null;
        var recipe = match.value();
        var remaining = recipe.getRemainingItems(input);
        var grid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        for (int z = 0; z < input.height(); z++) {
            for (int x = 0; x < input.width(); x++) {
                grid.set(
                        (positioned.top() + z) * 3 + positioned.left() + x,
                        remaining.get(z * input.width() + x));
            }
        }
        return new Result(recipe.assemble(input), grid);
    }

    /** Output and remaining stacks, indexed in the originally supplied grid. */
    public record Result(ItemStack output, List<ItemStack> remainders) {}
}
