package com.industrialage.ia2.logistics;

import net.minecraft.world.item.ItemStack;

/** Keeps a container's item data when a processor changes only its cell state. */
public final class CellProcessing {
    private CellProcessing() {}

    /** Applies mutations specific to the compressor's fill and the extractor's shell recovery. */
    public static ItemStack result(String machine, ItemStack input, ItemStack result) {
        if ("compressor".equals(machine)
                && "ic2:air_cell".equals(TransportPlatform.itemId(result))) {
            return TransportPlatform.bindCell(input, result);
        }
        if ("extractor".equals(machine)) {
            String path = CellFamily.itemPath(TransportPlatform.itemId(result));
            if (path.equals(CellFamily.BASIC.emptyPath())
                    || path.equals(CellFamily.UNIVERSAL.emptyPath())) {
                var shell = CellCrafting.recover(input);
                if (shell.is(result.getItem())) return shell;
            }
        }
        return result;
    }
}
