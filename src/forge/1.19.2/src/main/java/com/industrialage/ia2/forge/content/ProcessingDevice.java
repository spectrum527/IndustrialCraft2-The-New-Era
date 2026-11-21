package com.industrialage.ia2.forge.content;

import com.industrialage.ia2.content.ProcessingMachine;
import com.industrialage.ia2.content.ProcessingMachine.Recipe;
import com.industrialage.ia2.forge.content.MachineKind.Slot;
import com.industrialage.ia2.forge.equipment.ForgeGear;
import com.industrialage.ia2.industrial.WorkshopNode;
import com.industrialage.ia2.world.MachineAutomation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;

final class ProcessingDevice extends InventoryDevice {
    private static final int IRON_FURNACE_DURATION = 160;
    private final MachineKind kind;
    private final ProcessingMachine machine;
    private int heat;
    private int fuelCapacity;
    private int cooking;
    private boolean burning;
    private String smeltingItem = "";

    ProcessingDevice(BlockPos pos, BlockState state) {
        super(ForgeContent.PROCESSOR.get(), pos, state, ((MachineBlock) state.getBlock()).kind);
        kind = inventory().kind;
        machine = kind == MachineKind.IRON_FURNACE ? null : new ProcessingMachine(kind.path);
    }

    boolean acceptsInput(ItemStack stack) {
        return work(stack) != null;
    }

    void tick() {
        var inventory = inventory();
        var input = inventory.stack(Slot.INPUT);
        var output = inventory.stack(Slot.OUTPUT);
        var pump =
                input.isEmpty()
                                && kind == MachineKind.COMPRESSOR
                                && level instanceof ServerLevel server
                        ? WorkshopNode.connectedWaterPump(server, getBlockPos())
                        : null;
        var work =
                pump == null
                        ? work(input)
                        : new Work(
                                new Recipe("ic2:pumped_water", 1, "minecraft:snowball", 1),
                                new ItemStack(Items.SNOWBALL));
        int room = work == null ? 0 : room(output, work.result);

        if (machine == null) {
            tickFurnace(input, output, work, room);
            return;
        }

        boolean upgradeChanged = refreshUpgrades();
        var battery = inventory.stack(Slot.DISCHARGE);
        receive(ForgeGear.discharge(battery, capacity() - energy(), capacity(), itemTier()));
        if (battery.is(Items.REDSTONE) && capacity() - energy() >= 800) {
            receive(800);
            battery.shrink(1);
            inventory.setChanged();
        }
        double before = machine.energy();
        int progress = machine.progress();
        String key = machine.recipeKey();
        if (machine.tick(
                work == null ? null : work.recipe, pump == null ? input.getCount() : 1, room)) {
            if (pump != null) {
                pump.consumeCompressionWater();
            }
            complete(input, output, work, machine.completedOperations());
        }
        if (level instanceof ServerLevel server) {
            upgradeChanged |=
                    MachineAutomation.transfer(
                            server,
                            getBlockPos(),
                            inventory,
                            kind.slots.indexOf(Slot.INPUT),
                            kind.slots.indexOf(Slot.OUTPUT),
                            upgrades(),
                            this::acceptsInput);
        }
        if (upgradeChanged
                || before != machine.energy()
                || progress != machine.progress()
                || !key.equals(machine.recipeKey())) {
            setChanged();
        }
    }

    private void tickFurnace(ItemStack input, ItemStack output, Work work, int room) {
        int oldHeat = heat;
        int oldCooking = cooking;
        var oldSmeltingItem = smeltingItem;
        var inventory = inventory();
        boolean canCook = work != null && room >= work.result.getCount();
        if (!canCook || !work.recipe.input().equals(smeltingItem)) {
            cooking = 0;
            smeltingItem = canCook ? work.recipe.input() : "";
        }

        if (heat == 0 && canCook) {
            int burnTime = MachineInventory.burnTime(inventory.stack(Slot.FUEL));
            if (burnTime > 0) {
                inventory.consumeFuel();
                heat = burnTime;
                fuelCapacity = burnTime;
            }
        }

        burning = heat > 0;
        if (burning) {
            heat--;
            if (canCook && ++cooking >= IRON_FURNACE_DURATION) {
                complete(input, output, work);
                cooking = 0;
            }
        }
        if (oldHeat != heat || oldCooking != cooking || !oldSmeltingItem.equals(smeltingItem)) {
            setChanged();
        }
    }

    private Work work(ItemStack input) {
        if (input.isEmpty() || level == null) {
            return null;
        }
        String id = ForgeRegistries.ITEMS.getKey(input.getItem()).toString();
        if (kind == MachineKind.COMPRESSOR
                && id.equals("ic2:empty_cell")
                && FluidUtil.getFluidContained(input)
                        .filter(fluid -> !fluid.isEmpty())
                        .isPresent()) {
            return null;
        }
        if (kind == MachineKind.IRON_FURNACE || kind == MachineKind.ELECTRIC_FURNACE) {
            var single = new SimpleContainer(input);
            return level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, single, level)
                    .map(recipe -> new Work(id, recipe.assemble(single)))
                    .orElse(null);
        }
        var recipe = ProcessingMachine.recipe(kind.path, id);
        if (recipe == null) {
            return null;
        }
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(recipe.output()));
        return item == null ? null : new Work(recipe, new ItemStack(item, recipe.outputCount()));
    }

    private static int room(ItemStack current, ItemStack result) {
        if (current.isEmpty()) {
            return result.getMaxStackSize();
        }
        return ItemStack.isSameItemSameTags(current, result)
                ? current.getMaxStackSize() - current.getCount()
                : 0;
    }

    private void complete(ItemStack input, ItemStack output, Work work) {
        complete(input, output, work, 1);
    }

    private void complete(ItemStack input, ItemStack output, Work work, int operations) {
        var inventory = inventory();
        if (!input.isEmpty()) {
            input.shrink(work.recipe.inputCount() * operations);
            inventory.set(Slot.INPUT, input.isEmpty() ? ItemStack.EMPTY : input);
        }
        if (output.isEmpty()) {
            var produced = work.result.copy();
            produced.setCount(work.result.getCount() * operations);
            inventory.set(Slot.OUTPUT, produced);
        } else {
            output.grow(work.result.getCount() * operations);
            inventory.set(Slot.OUTPUT, output);
        }
    }

    boolean electric() {
        return machine != null;
    }

    int itemTier() {
        return machine == null ? 1 : machine.stats().tier();
    }

    private java.util.List<ItemStack> upgrades() {
        return java.util.stream.IntStream.range(3, inventory().getContainerSize())
                .mapToObj(inventory()::getItem)
                .toList();
    }

    boolean refreshUpgrades() {
        return machine != null && machine.setUpgrades(MachineAutomation.installed(upgrades()));
    }

    @Override
    public double energy() {
        return machine == null ? 0 : machine.energy();
    }

    @Override
    public double capacity() {
        return machine == null ? 0 : machine.capacity();
    }

    @Override
    public int progress() {
        return machine == null ? cooking : machine.progress();
    }

    @Override
    public int duration() {
        return machine == null ? IRON_FURNACE_DURATION : machine.duration();
    }

    @Override
    public boolean active() {
        return machine == null ? burning : machine.active();
    }

    @Override
    public boolean accepts(Direction face) {
        return machine != null;
    }

    @Override
    public double inputRating() {
        return com.industrialage.ia2.energy.graph.PacketBudget.packetSize(itemTier());
    }

    @Override
    public double receive(double eu) {
        if (machine == null || !(energy() < capacity())) {
            return 0;
        }
        double accepted = machine.receive(eu);
        if (accepted > 0) {
            setChanged();
        }
        return accepted;
    }

    @Override
    public double fuelRatio() {
        return heat == 0 || fuelCapacity == 0 ? 0 : (double) heat / fuelCapacity;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (machine == null) {
            tag.putInt("ic2_heat", heat);
            tag.putInt("ic2_fuel_capacity", heat == 0 ? 0 : fuelCapacity);
            tag.putString("ic2_smelting", smeltingItem);
        } else {
            tag.putDouble("ic2_energy", machine.energy());
            tag.putString("ic2_recipe", machine.recipeKey());
            tag.putInt("ic2_duration", machine.duration());
        }
        tag.putInt("ic2_progress", progress());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (machine == null) {
            heat = Math.max(0, tag.getInt("ic2_heat"));
            fuelCapacity = Math.max(heat, tag.getInt("ic2_fuel_capacity"));
            smeltingItem = tag.getString("ic2_smelting");
            cooking = Math.max(0, Math.min(IRON_FURNACE_DURATION - 1, tag.getInt("ic2_progress")));
        } else {
            refreshUpgrades();
            machine.restore(
                    tag.getDouble("ic2_energy"),
                    tag.getInt("ic2_progress"),
                    tag.getString("ic2_recipe"),
                    tag.getInt("ic2_duration"));
        }
    }

    private record Work(Recipe recipe, ItemStack result) {
        Work(String input, ItemStack result) {
            this(
                    new Recipe(
                            input,
                            1,
                            ForgeRegistries.ITEMS.getKey(result.getItem()).toString(),
                            result.getCount()),
                    result);
        }
    }
}
