package com.industrialage.ia2.logistics;

import com.industrialage.ia2.forge.content.ForgeContent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.registries.ForgeRegistries;

/** Native container for the legacy and universal cell families. */
public final class CellItem extends Item {
    private static final String CONTENTS = "Fluid";
    private final String path;

    CellItem(String path) {
        super(ForgeContent.itemProperties(path));
        this.path = path;
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack carried, CompoundTag ignored) {
        return new FluidHandlerItemStack(carried, CellFamily.CAPACITY) {
            @Override
            public FluidStack getFluid() {
                return contents(container);
            }

            @Override
            public boolean canFillFluidType(FluidStack resource) {
                return CellFamily.of(path(container)).admits(fluidId(resource));
            }

            @Override
            protected void setFluid(FluidStack fluid) {
                container = withContents(container, fluid);
            }

            @Override
            protected void setContainerToEmpty() {
                setFluid(FluidStack.EMPTY);
            }
        };
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return stack.getTagElement(CONTENTS) == null || contents(stack).isEmpty()
                ? super.getMaxStackSize(stack)
                : 1;
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
                CellFamily.CAPACITY);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        var fluid = contents(stack);
        if (!fluid.isEmpty()) {
            tooltip.add(fluid.getDisplayName().copy().append(" " + fluid.getAmount() + " mB"));
        }
    }

    static FluidStack contents(ItemStack stack) {
        var data = stack.getTagElement(CONTENTS);
        var stored = data == null ? FluidStack.EMPTY : FluidStack.loadFluidStackFromNBT(data);
        if (!stored.isEmpty()) return stored;
        String path = path(stack);
        var fixed = CellFamily.of(path).namedLiquids().get(path);
        if (fixed == null) return FluidStack.EMPTY;
        var fluid = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(fixed));
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, CellFamily.CAPACITY);
    }

    static ItemStack withContents(ItemStack stack, FluidStack fluid) {
        String current = path(stack);
        var family = CellFamily.of(current);
        String liquid = fluidId(fluid);
        String target = family.resultingPath(liquid, fluid.getAmount());
        if (!current.equals(target)) {
            stack = retype(stack, target);
        }
        if (!fluid.isEmpty()
                && (fluid.hasTag() || !liquid.equals(family.namedLiquids().get(target)))) {
            stack.addTagElement(CONTENTS, fluid.writeToNBT(new CompoundTag()));
        } else if (stack.getTag() != null) {
            stack.removeTagKey(CONTENTS);
        }
        return stack;
    }

    private static String path(ItemStack stack) {
        return ((CellItem) stack.getItem()).path;
    }

    static ItemStack retype(ItemStack stack, String target) {
        var saved = stack.save(new CompoundTag());
        saved.putString("id", "ic2:" + target);
        saved.putByte("Count", (byte) 1);
        return ItemStack.of(saved);
    }

    static String fluidId(FluidStack fluid) {
        var id = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        return id == null ? "" : id.toString();
    }
}
