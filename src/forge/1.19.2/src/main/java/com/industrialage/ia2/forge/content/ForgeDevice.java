package com.industrialage.ia2.forge.content;

import com.industrialage.ia2.power.EuBuffer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

interface ForgeDevice extends MenuProvider, EuBuffer {
    @Override
    default EuBuffer.Reading bufferedEu() {
        return new EuBuffer.Reading(energy(), capacity());
    }

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

    default double fuelRatio() {
        return 0;
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
