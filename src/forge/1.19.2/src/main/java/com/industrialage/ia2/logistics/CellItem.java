package com.industrialage.ia2.logistics;

import com.industrialage.ia2.forge.content.ForgeContent;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStackSimple;

/** Native container for the legacy and universal cell families. */
public final class CellItem extends Item {
    private static final int CAPACITY = 1000;
    private final Supplier<FluidStack> fixedContents;
    private final Supplier<ItemStack> emptyRemainder;

    CellItem(boolean universal) {
        this(
                universal
                        ? ForgeContent.itemProperties().stacksTo(1)
                        : ForgeContent.itemProperties(),
                null,
                null);
    }

    private CellItem(
            Properties properties,
            Supplier<FluidStack> fixedContents,
            Supplier<ItemStack> emptyRemainder) {
        super(properties);
        this.fixedContents = fixedContents;
        this.emptyRemainder = emptyRemainder;
    }

    /** Creates a legacy prefilled cell with its own registry identity and reusable shell. */
    public static Item named(Supplier<FluidStack> fluid, Supplier<ItemStack> emptyRemainder) {
        return new CellItem(ForgeContent.itemProperties(), fluid, emptyRemainder);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack carried, CompoundTag ignored) {
        if (fixedContents == null) {
            return new FluidHandlerItemStack(carried, CAPACITY);
        }
        return new FluidHandlerItemStackSimple.SwapEmpty(carried, emptyRemainder.get(), CAPACITY) {
            @Override
            public FluidStack getFluid() {
                return fixedContents.get();
            }

            @Override
            public boolean canFillFluidType(FluidStack resource) {
                return false;
            }
        };
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return fixedContents != null || content(stack).isEmpty() ? super.getMaxStackSize(stack) : 1;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var face = context.getClickedFace();
        if (player == null) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            return interactWithBlock(server, pos, face, player, context.getHand())
                    ? InteractionResult.CONSUME
                    : InteractionResult.PASS;
        }
        return FluidUtil.getFluidHandler(level, pos, face).isPresent()
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    /** Exchanges fluid with the clicked face before a machine opens its menu. */
    public static boolean interactWithBlock(
            ServerLevel level, BlockPos pos, Direction face, Player player, InteractionHand hand) {
        return level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, face, player.getItemInHand(hand))
                && FluidUtil.interactWithFluidHandler(player, hand, level, pos, face);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level world, Player player, InteractionHand hand) {
        return CellPickup.collect(
                world,
                player,
                hand,
                getPlayerPOVHitResult(world, player, ClipContext.Fluid.SOURCE_ONLY),
                CAPACITY);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        var fluid = content(stack);
        if (!fluid.isEmpty()) {
            tooltip.add(fluid.getDisplayName().copy().append(" " + fluid.getAmount() + " mB"));
        }
    }

    private static FluidStack content(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
    }
}
