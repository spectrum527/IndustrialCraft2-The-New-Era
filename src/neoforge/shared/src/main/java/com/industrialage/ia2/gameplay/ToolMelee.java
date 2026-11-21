package com.industrialage.ia2.gameplay;

import com.industrialage.ia2.content.PortableEquipment;
import com.industrialage.ia2.progress.IndustrialProgress;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/** Main-hand tool attributes and the charge paid for a damaging melee hit. */
final class ToolMelee {
    private static final AttributeModifier SPEED =
            new AttributeModifier(
                    Item.BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE);

    private ToolMelee() {}

    static void install(IEventBus bus) {
        bus.addListener(ItemAttributeModifierEvent.class, ToolMelee::attributes);
        bus.addListener(AttackEntityEvent.class, event -> refreshHeldTool(event.getEntity()));
        bus.addListener(LivingDamageEvent.Post.class, ToolMelee::paidHit);
    }

    private static PortableEquipment poweredTool(ItemStack stack) {
        var tool = PortableGear.type(stack);
        return tool != null
                        && tool.poweredMeleeDamage() > 0
                        && PortableGear.stored(stack) >= tool.cost
                ? tool
                : null;
    }

    private static void attributes(ItemAttributeModifierEvent event) {
        var tool = poweredTool(event.getItemStack());
        if (tool == null) return;
        event.addModifier(Attributes.ATTACK_DAMAGE, damage(tool), EquipmentSlotGroup.MAINHAND);
        event.addModifier(Attributes.ATTACK_SPEED, SPEED, EquipmentSlotGroup.MAINHAND);
    }

    private static AttributeModifier damage(PortableEquipment tool) {
        return new AttributeModifier(
                Item.BASE_ATTACK_DAMAGE_ID,
                tool.poweredMeleeDamage() - 1,
                AttributeModifier.Operation.ADD_VALUE);
    }

    // Vanilla refreshes cached equipment once per tick; two attacks can use different charge states
    // within that tick.
    private static void refreshHeldTool(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel)) return;
        var stack = attacker.getMainHandItem();
        var tool = PortableGear.type(stack);
        if (tool == null || tool.poweredMeleeDamage() == 0) return;
        boolean charged = PortableGear.stored(stack) >= tool.cost;
        refresh(attacker.getAttribute(Attributes.ATTACK_DAMAGE), damage(tool), charged);
        refresh(attacker.getAttribute(Attributes.ATTACK_SPEED), SPEED, charged);
    }

    private static void refresh(
            AttributeInstance attribute, AttributeModifier desired, boolean charged) {
        if (attribute == null) return;
        var current = attribute.getModifier(desired.id());
        if (charged && !desired.equals(current)) attribute.addOrUpdateTransientModifier(desired);
        if (!charged && current != null) attribute.removeModifier(desired.id());
    }

    private static void paidHit(LivingDamageEvent.Post event) {
        var source = event.getSource();
        if (!(event.getEntity().level() instanceof ServerLevel)
                || NativeGameplay.healthDamage(event) <= 0
                || !(source.getEntity() instanceof LivingEntity attacker)
                || source.getDirectEntity() != attacker
                || !(source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK))
                || attacker instanceof ServerPlayer player && player.isCreative()) return;
        var stack = attacker.getMainHandItem();
        var tool = poweredTool(stack);
        if (tool == null || !PortableGear.spend(stack, tool.cost)) return;
        if (PortableGear.stored(stack) < tool.cost) refreshHeldTool(attacker);
        if (attacker instanceof ServerPlayer player)
            IndustrialProgress.poweredToolHit(player, stack);
    }
}
