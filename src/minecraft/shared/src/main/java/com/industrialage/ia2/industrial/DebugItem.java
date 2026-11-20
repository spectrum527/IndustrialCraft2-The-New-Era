package com.industrialage.ia2.industrial;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Creative inspection tool for the identifying state of the selected world block. */
final class DebugItem extends Item {
    DebugItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel() instanceof ServerLevel level && context.getPlayer() != null) {
            var pos = context.getClickedPos();
            var state = level.getBlockState(pos);
            var machine = level.getBlockEntity(pos);
            var player = context.getPlayer();
            player.sendSystemMessage(
                    Component.literal(
                            PowerPlatform.blockId(state)
                                    + " ("
                                    + pos.getX()
                                    + ", "
                                    + pos.getY()
                                    + ", "
                                    + pos.getZ()
                                    + ") "
                                    + state.getValues()));
            if (machine instanceof PowerNode power)
                player.sendSystemMessage(power.getDisplayName());
            if (machine instanceof WorkshopNode workshop)
                player.sendSystemMessage(workshop.getDisplayName());
        }
        return InteractionResult.SUCCESS;
    }
}
