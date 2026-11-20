package com.industrialage.ia2.forge.content;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

interface ForgeDevice extends MenuProvider {
    MachineInventory inventory();

    default double energy() {
        return 0;
    }

    default double capacity() {
        return 0;
    }

    default int progress() {
        return 0;
    }

    default int duration() {
        return 0;
    }

    default boolean active() {
        return false;
    }

    @Override
    default Component getDisplayName() {
        return Component.translatable("block.ic2." + inventory().kind.path);
    }

    @Override
    default AbstractContainerMenu createMenu(int id, Inventory player, Player opener) {
        return new MachineMenu(id, player, this);
    }
}
