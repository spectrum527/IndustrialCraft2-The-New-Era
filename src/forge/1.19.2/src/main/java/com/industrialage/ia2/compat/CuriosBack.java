package com.industrialage.ia2.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/** Access to the functional Curios back slots when that optional mod is installed. */
public final class CuriosBack {
    private CuriosBack() {}

    /** References to the currently equipped stacks, in slot order. */
    public static List<ItemStack> slots(LivingEntity wearer) {
        return ModList.get().isLoaded("curios") ? Loaded.slots(wearer) : List.of();
    }

    /** Live stacks a pad can charge, including worn gear, the player's inventory and Curios. */
    public static List<ItemStack> chargeableEquipment(LivingEntity wearer) {
        var stacks = new ArrayList<ItemStack>();
        stacks.add(wearer.getItemBySlot(EquipmentSlot.CHEST));
        stacks.add(wearer.getItemBySlot(EquipmentSlot.HEAD));
        stacks.add(wearer.getItemBySlot(EquipmentSlot.LEGS));
        stacks.add(wearer.getItemBySlot(EquipmentSlot.FEET));
        stacks.add(wearer.getMainHandItem());
        stacks.add(wearer.getOffhandItem());
        if (wearer instanceof Player player) {
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                stacks.add(inventory.getItem(i));
            }
        }
        stacks.addAll(slots(wearer));
        // The selected hotbar slot and main hand reference the same stack.
        var seen = Collections.newSetFromMap(new IdentityHashMap<ItemStack, Boolean>());
        stacks.removeIf(stack -> stack.isEmpty() || !seen.add(stack));
        return stacks;
    }

    /** Picks one matching worn item, giving the regular chest slot priority. */
    public static ItemStack preferChest(LivingEntity wearer, Predicate<ItemStack> accepts) {
        var chest = wearer.getItemBySlot(EquipmentSlot.CHEST);
        if (accepts.test(chest)) return chest;
        for (var stack : slots(wearer)) {
            if (accepts.test(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    static void enqueueSlot() {
        if (ModList.get().isLoaded("curios")) Loaded.registerSlot();
    }

    private static final class Loaded {
        static void registerSlot() {
            InterModComms.sendTo(
                    "curios",
                    SlotTypeMessage.REGISTER_TYPE,
                    () -> SlotTypePreset.BACK.getMessageBuilder().size(1).build());
        }

        static List<ItemStack> slots(LivingEntity wearer) {
            return CuriosApi.getCuriosHelper()
                    .getCuriosHandler(wearer)
                    .resolve()
                    .flatMap(handler -> handler.getStacksHandler("back"))
                    .map(ICurioStacksHandler::getStacks)
                    .map(
                            stacks -> {
                                var equipped = new ArrayList<ItemStack>(stacks.getSlots());
                                for (int slot = 0; slot < stacks.getSlots(); slot++) {
                                    var stack = stacks.getStackInSlot(slot);
                                    if (!stack.isEmpty()) equipped.add(stack);
                                }
                                return equipped;
                            })
                    .orElseGet(ArrayList::new);
        }
    }
}
