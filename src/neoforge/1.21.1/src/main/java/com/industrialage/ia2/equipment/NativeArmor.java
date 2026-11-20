package com.industrialage.ia2.equipment;

import com.industrialage.ia2.equipment.ArmorEffects.Piece;
import com.industrialage.ia2.gameplay.PortableGear;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Armor materials and named attribute modifiers on NeoForge 1.21.1. */
final class NativeArmor {
    private static final AttributeModifier SPRINT =
            new AttributeModifier(
                    id("quantum_sprint"), 0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private NativeArmor() {}

    static void register(DeferredRegister.Items items, IEventBus bus) {
        var materials = DeferredRegister.create(Registries.ARMOR_MATERIAL, "ic2");
        Map<String, Supplier<? extends Holder<ArmorMaterial>>> registered = new HashMap<>();
        for (var piece : ArmorEffects.PIECES) {
            var material =
                    registered.computeIfAbsent(
                            piece.material(),
                            name -> {
                                var holder = materials.register(name, () -> material(name));
                                return () -> holder;
                            });
            var props = new Item.Properties().stacksTo(1);
            if (piece.durability() > 0) props.durability(piece.durability());
            items.registerItem(piece.path(), p -> new SuitItem(piece, material.get(), p), props);
        }
        materials.register(bus);
    }

    private static ArmorMaterial material(String name) {
        boolean alloy = name.equals("alloy");
        return new ArmorMaterial(
                alloy ? Map.of(ArmorItem.Type.CHESTPLATE, 6) : Map.of(),
                alloy ? 12 : 0,
                SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> alloy ? Ingredient.of(item("alloy")) : Ingredient.EMPTY,
                name.equals("quantum")
                        ? List.of(
                                new ArmorMaterial.Layer(id(name)),
                                new ArmorMaterial.Layer(id(name), "_overlay", false))
                        : List.of(new ArmorMaterial.Layer(id(name))),
                alloy ? 2 : 0,
                0);
    }

    static void sprint(ServerPlayer player, boolean boosted) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null || boosted == speed.hasModifier(SPRINT.id())) return;
        if (boosted) speed.addTransientModifier(SPRINT);
        else speed.removeModifier(SPRINT.id());
    }

    static AttributeModifier protectionModifier(String path, double amount) {
        return new AttributeModifier(id(path), amount, AttributeModifier.Operation.ADD_VALUE);
    }

    static boolean isFilledCan(ItemStack stack) {
        return stack.is(item("filled_tin_can"));
    }

    static ItemStack emptyCan() {
        return new ItemStack(item("tin_can"));
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(id(path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("ic2", path);
    }

    private static ArmorItem.Type type(Piece piece) {
        return switch (piece.slot()) {
            case HEAD -> ArmorItem.Type.HELMET;
            case CHEST -> ArmorItem.Type.CHESTPLATE;
            case LEGS -> ArmorItem.Type.LEGGINGS;
            case FEET -> ArmorItem.Type.BOOTS;
            default -> throw new IllegalArgumentException(piece.path());
        };
    }

    private static final class SuitItem extends ArmorItem implements ArmorEffects.Wearable {
        private final Piece piece;

        SuitItem(Piece piece, Holder<ArmorMaterial> material, Properties props) {
            super(material, type(piece), props);
            this.piece = piece;
        }

        @Override
        public Piece piece() {
            return piece;
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
            return piece.electric() == null
                    ? super.isBarVisible(stack)
                    : PortableGear.stored(stack) > 0;
        }

        @Override
        public int getBarWidth(ItemStack stack) {
            return piece.electric() == null
                    ? super.getBarWidth(stack)
                    : (int) Math.round(13 * PortableGear.stored(stack) / piece.electric().capacity);
        }

        @Override
        public int getBarColor(ItemStack stack) {
            return piece.electric() == null ? super.getBarColor(stack) : 0x58C7D5;
        }
    }
}
