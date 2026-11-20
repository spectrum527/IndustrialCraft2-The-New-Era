package com.industrialage.ia2.world;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/** Native crafting lookup with its per-square container and tool remainders. */
public final class WorldCrafting {
    private WorldCrafting() {}

    /** Resolves a nine-square grid against this world's active recipe data. */
    public static Result result(ServerLevel level, List<ItemStack> stacks) {
        var input =
                new CraftingContainer(
                        new AbstractContainerMenu(null, -1) {
                            @Override
                            public boolean stillValid(Player player) {
                                return false;
                            }

                            @Override
                            public ItemStack quickMoveStack(Player player, int slot) {
                                return ItemStack.EMPTY;
                            }
                        },
                        3,
                        3);
        for (int i = 0; i < stacks.size(); i++) input.setItem(i, stacks.get(i));
        var match =
                level.getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, level)
                        .orElse(null);
        return match == null
                ? null
                : new Result(match.assemble(input), match.getRemainingItems(input));
    }

    /** Output and remaining stacks, indexed in the originally supplied grid. */
    public record Result(ItemStack output, List<ItemStack> remainders) {}
}
