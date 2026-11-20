package com.industrialage.ia2.fluids;

import net.minecraft.world.level.block.state.BlockBehaviour;

/** Host spelling of the vanilla fluid block collision setting. */
final class FluidBlockProperties {
    private FluidBlockProperties() {}

    static BlockBehaviour.Properties nonSolid(BlockBehaviour.Properties properties) {
        return properties.noCollision();
    }
}
