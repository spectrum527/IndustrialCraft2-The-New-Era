package com.industrialage.ia2.equipment;

import com.industrialage.ia2.equipment.ArmorEffects.Piece;
import com.industrialage.ia2.gameplay.PortableGear;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Component-backed armor items and modifiers on NeoForge 26.x. */
final class NativeArmor {
    private static final AttributeModifier SPRINT =
            new AttributeModifier(
                    id("quantum_sprint"), 0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private NativeArmor() {}

    static void register(DeferredRegister.Items items, IEventBus bus) {
        for (var piece : ArmorEffects.PIECES) {
            items.registerItem(
                    piece.path(), props -> new SuitItem(piece, properties(piece, props)));
        }
    }

    private static Item.Properties properties(Piece piece, Item.Properties props) {
        var asset = ResourceKey.create(EquipmentAssets.ROOT_ID, id(piece.material()));
        if (piece.material().equals("alloy")) {
            var repair = TagKey.create(Registries.ITEM, id("alloys"));
            var material =
                    new ArmorMaterial(
                            50,
                            Map.of(ArmorType.CHESTPLATE, 6),
                            12,
                            SoundEvents.ARMOR_EQUIP_IRON,
                            2,
                            0,
                            repair,
                            asset);
            return props.humanoidArmor(material, ArmorType.CHESTPLATE)
                    .durability(piece.durability());
        }
        props.stacksTo(1)
                .component(
                        DataComponents.EQUIPPABLE,
                        Equippable.builder(piece.slot()).setAsset(asset).build());
        if (piece.durability() > 0) props.durability(piece.durability());
        return props;
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
        return BuiltInRegistries.ITEM.getValue(id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("ic2", path);
    }

    private static final class SuitItem extends Item implements ArmorEffects.Wearable {
        private final Piece piece;

        SuitItem(Piece piece, Properties props) {
            super(props);
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
