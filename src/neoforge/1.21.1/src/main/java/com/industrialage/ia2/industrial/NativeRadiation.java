package com.industrialage.ia2.industrial;

import com.industrialage.ia2.content.RadiationRules;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

final class NativeRadiation extends MobEffect {
    NativeRadiation() {
        super(MobEffectCategory.HARMFUL, 0x719630);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int ticks, int amp) {
        return RadiationRules.damageTick(ticks, amp);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amp) {
        if (com.industrialage.ia2.equipment.ArmorEffects.fullHazmat(entity)) return true;
        entity.hurt(entity.damageSources().magic(), RadiationRules.damage(amp));
        return true;
    }
}
