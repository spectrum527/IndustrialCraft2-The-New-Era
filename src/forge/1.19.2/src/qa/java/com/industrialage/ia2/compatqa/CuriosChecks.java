package com.industrialage.ia2.compatqa;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

final class CuriosChecks {
    private CuriosChecks() {}

    static void equip(ServerPlayer player, ItemStack pack) {
        var inventory =
                CuriosApi.getCuriosHelper().getCuriosHandler(player).resolve().orElseThrow();
        var handler = inventory.getStacksHandler("back").orElseThrow();
        var context = new SlotContext("back", player, 0, false, true);
        for (var path :
                new String[] {
                    "batpack", "advanced_batpack", "energy_pack", "electric_jetpack", "jetpack"
                }) {
            OptionalCompatFixture.require(
                    CuriosApi.getCuriosHelper()
                            .isStackValid(context, new ItemStack(CompatHost.item(path))),
                    "Curios validates " + path + " in back slot");
        }
        OptionalCompatFixture.require(
                !CuriosApi.getCuriosHelper().isStackValid(context, new ItemStack(Items.DIAMOND)),
                "Curios rejects an untagged item");
        handler.getStacks().setStackInSlot(0, pack);
    }

    static ItemStack unequip(ServerPlayer player) {
        var inventory =
                CuriosApi.getCuriosHelper().getCuriosHandler(player).resolve().orElseThrow();
        var stacks = inventory.getStacksHandler("back").orElseThrow().getStacks();
        var stack = stacks.getStackInSlot(0);
        stacks.setStackInSlot(0, ItemStack.EMPTY);
        return stack;
    }
}
