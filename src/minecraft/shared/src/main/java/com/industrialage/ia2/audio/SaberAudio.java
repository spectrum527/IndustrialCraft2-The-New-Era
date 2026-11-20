package com.industrialage.ia2.audio;

import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** Owns the single idle hum of the active saber in the local player's hands. */
public final class SaberAudio {
    private Loop idle;

    /** Starts or stops the held sound as the active item or world changes. */
    public void reconcile(Predicate<ItemStack> active) {
        var client = Minecraft.getInstance();
        var player = client.player;
        boolean held =
                player != null
                        && (active.test(player.getMainHandItem())
                                || active.test(player.getOffhandItem()));
        if (held) {
            if (idle == null) {
                idle = new Loop();
                client.getSoundManager().play(idle);
            }
        } else if (idle != null) {
            idle.finish();
            idle = null;
        }
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        Loop() {
            super(
                    SoundPlatform.event("item.nanosaber.idle"),
                    SoundSource.PLAYERS,
                    SoundInstance.createUnseededRandom());
            looping = true;
            attenuation = SoundInstance.Attenuation.NONE;
            relative = true;
            volume = 0.35F;
        }

        void finish() {
            stop();
        }

        @Override
        public void tick() {}
    }
}
