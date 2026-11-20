package com.industrialage.ia2.equipment;

import com.industrialage.ia2.content.PortableEquipment;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Electric jetpack mounting, charging and recovery from the original chest item. */
public final class JetpackMount {
    private static final PortableEquipment PACK = PortableEquipment.ELECTRIC_JETPACK;
    private static final Map<ServerPlayer, BeforeDamage> DAMAGED = new WeakHashMap<>();

    private JetpackMount() {}

    /** Whether the stack carries an installed jetpack, including an empty one. */
    public static boolean installed(ItemStack stack) {
        return !stack.isEmpty() && MountPlatform.read(stack) >= 0;
    }

    /** Reads available flight energy within the attached pack. */
    public static double energy(ItemStack stack) {
        return installed(stack) ? PACK.stored(MountPlatform.read(stack)) : 0;
    }

    /** Transfers EU accepted into the installed flight unit. */
    public static double charge(ItemStack stack, double offered, int tier) {
        if (!installed(stack) || stack.getCount() != 1) return 0;
        double prior = PACK.stored(MountPlatform.read(stack));
        double accepted = PACK.chargeAmount(offered, prior, tier);
        if (accepted > 0) MountPlatform.write(stack, prior + accepted);
        return accepted;
    }

    /** Pays for one thrust or hover tick from the separately tracked attached pack. */
    public static boolean thrust(ItemStack stack, boolean hover, boolean creative) {
        if (!installed(stack)) return false;
        int cost = hover ? 7 : 8;
        double energy = PACK.stored(MountPlatform.read(stack));
        if (energy < cost) return false;
        if (!creative) MountPlatform.write(stack, energy - cost);
        return true;
    }

    /** Combines one chestplate, attachment plate and electric jetpack in the native recipe. */
    public static ItemStack craft(List<ItemStack> inputs) {
        ItemStack chest = ItemStack.EMPTY;
        ItemStack jetpack = ItemStack.EMPTY;
        boolean plate = false;
        for (var stack : inputs) {
            if (stack.isEmpty()) continue;
            String id = MountPlatform.itemId(stack);
            if (id.equals("ic2:electric_jetpack") && jetpack.isEmpty()) jetpack = stack;
            else if (id.equals("ic2:jetpack_attachment_plate") && !plate) plate = true;
            else if (chest.isEmpty() && MountPlatform.chestArmor(stack) && !installed(stack))
                chest = stack;
            else return ItemStack.EMPTY;
        }
        if (chest.isEmpty() || jetpack.isEmpty() || !plate) return ItemStack.EMPTY;
        var mounted = chest.copy();
        mounted.setCount(1);
        MountPlatform.write(mounted, PACK.stored(MountPlatform.packEnergy(jetpack)));
        return mounted;
    }

    /** Captures the actual worn stack before a native damage operation writes durability. */
    public static void beforeDamage(ServerPlayer player) {
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (installed(chest))
            DAMAGED.put(player, new BeforeDamage(chest, MountPlatform.read(chest)));
        else DAMAGED.remove(player);
    }

    /** Returns one standalone pack only if that same stack broke during this damage operation. */
    public static void afterDamage(ServerPlayer player) {
        var snapshot = DAMAGED.remove(player);
        if (snapshot == null || !snapshot.chest().isEmpty()) return;
        var recovered = MountPlatform.recover(PACK.stored(snapshot.eu()));
        if (!player.getInventory().add(recovered)) player.drop(recovered, false);
    }

    private record BeforeDamage(ItemStack chest, double eu) {}
}
