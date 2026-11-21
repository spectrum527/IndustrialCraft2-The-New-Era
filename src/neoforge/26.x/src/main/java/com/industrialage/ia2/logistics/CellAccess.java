package com.industrialage.ia2.logistics;

import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Uses a transactional item exchange for each change of cell contents or registry identity. */
final class CellAccess extends ItemAccessResourceHandler<FluidResource> {
    CellAccess(ItemAccess access) {
        super(access, 1);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource item, int slot) {
        return FluidResource.of(TransportPlatform.readCellFluid(item.toStack()));
    }

    @Override
    protected int getAmountFrom(ItemResource item, int slot) {
        return TransportPlatform.readCellFluid(item.toStack()).getAmount();
    }

    @Override
    public boolean isValid(int slot, FluidResource fluid) {
        return slot == 0
                && TransportPlatform.cellAdmits(
                        itemAccess.getResource().toStack(), fluid.toStack(1));
    }

    @Override
    protected int getCapacity(int slot, FluidResource fluid) {
        return CellFamily.CAPACITY;
    }

    @Override
    protected ItemResource update(ItemResource item, int slot, FluidResource fluid, int amount) {
        return ItemResource.of(
                TransportPlatform.withCellContents(item.toStack(), fluid.toStack(amount)));
    }
}
