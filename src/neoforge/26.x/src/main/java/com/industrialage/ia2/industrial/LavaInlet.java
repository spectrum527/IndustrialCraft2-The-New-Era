package com.industrialage.ia2.industrial;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Multiple shell ports share the controller's transaction journal and tank contents. */
final class LavaInlet extends SnapshotJournal<int[]> implements ResourceHandler<FluidResource> {
    private final PowerNode node;

    LavaInlet(PowerNode node) {
        this.node = node;
    }

    @Override
    protected int[] createSnapshot() {
        return new int[] {node.liquid, node.coolant, node.hotCoolant};
    }

    @Override
    protected void revertToSnapshot(int[] before) {
        node.liquid = before[0];
        node.coolant = before[1];
        node.hotCoolant = before[2];
    }

    @Override
    protected void onRootCommit(int[] before) {
        node.setChanged();
    }

    private boolean geo() {
        return node.kind == PowerKind.GEO_GENERATOR;
    }

    private PowerNode tank() {
        return geo() ? node : node.coolantTank();
    }

    private FluidResource resource(int slot) {
        return FluidResource.of(
                WorkshopPlatform.fluid(
                        geo() ? "minecraft:lava" : slot == 0 ? "ic2:coolant" : "ic2:hot_coolant",
                        1));
    }

    @Override
    public int size() {
        return geo() ? 1 : 2;
    }

    @Override
    public FluidResource getResource(int slot) {
        return getAmountAsLong(slot) > 0 ? resource(slot) : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int slot) {
        var t = tank();
        return t == null || slot < 0 || slot >= size()
                ? 0
                : geo() ? t.liquid : t.coolantAmount(slot == 1);
    }

    @Override
    public long getCapacityAsLong(int slot, FluidResource resource) {
        return slot < 0 || slot >= size() ? 0 : geo() ? 8000 : ReactorVessel.TANK_CAPACITY;
    }

    @Override
    public boolean isValid(int slot, FluidResource value) {
        if (slot < 0 || slot >= size() || value.isEmpty()) return false;
        if (resource(slot).equals(value)) return true;
        if (slot == 0) {
            String id = WorkshopPlatform.worldFluidId(value.getFluid());
            return geo() ? WorkshopThermal.isLava(id) : WorkshopThermal.isCoolant(id);
        }
        return false;
    }

    @Override
    public int insert(int slot, FluidResource value, int amount, TransactionContext tx) {
        if (slot != 0 || amount <= 0 || !isValid(slot, value)) return 0;
        var t = tank();
        if (t == null) return 0;
        int accepted =
                Math.min(amount, geo() ? 8000 - t.liquid : ReactorVessel.TANK_CAPACITY - t.coolant);
        if (accepted > 0) {
            t.inlet.updateSnapshots(tx);
            if (geo()) t.liquid += accepted;
            else t.coolant += accepted;
        }
        return accepted;
    }

    @Override
    public int extract(int slot, FluidResource value, int amount, TransactionContext tx) {
        if (geo() || slot != 1 || amount <= 0 || !isValid(slot, value)) return 0;
        var t = tank();
        if (t == null) return 0;
        int drained = Math.min(amount, t.hotCoolant);
        if (drained > 0) {
            t.inlet.updateSnapshots(tx);
            t.hotCoolant -= drained;
        }
        return drained;
    }
}
