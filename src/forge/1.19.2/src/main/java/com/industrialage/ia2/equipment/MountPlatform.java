package com.industrialage.ia2.equipment;

import com.industrialage.ia2.forge.content.ForgeContent;
import com.industrialage.ia2.forge.equipment.ForgeGear;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Native crafting, stored mount charge and equipped armor break checks on Forge. */
public final class MountPlatform {
    private static final String MOUNTED = "ic2_mounted_jetpack";
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "ic2");
    private static final java.util.function.Supplier<RecipeSerializer<?>> ATTACHMENT =
            RECIPES.register(
                    "jetpack_attachment", () -> new SimpleRecipeSerializer<>(AttachRecipe::new));

    private MountPlatform() {}

    /** Registers the mounting plate, crafting recipe and real damage events. */
    public static void install(DeferredRegister<Item> items, IEventBus modBus, IEventBus gameBus) {
        items.register(
                "jetpack_attachment_plate",
                () -> new Item(ForgeContent.itemProperties("jetpack_attachment_plate")));
        RECIPES.register(modBus);
        gameBus.addListener(
                (LivingHurtEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player)
                        JetpackMount.beforeDamage(player);
                });
        gameBus.addListener(
                (LivingDamageEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player)
                        JetpackMount.afterDamage(player);
                });
        gameBus.addListener(
                (ItemTooltipEvent event) -> {
                    if (JetpackMount.installed(event.getItemStack())) {
                        event.getToolTip()
                                .add(
                                        Component.translatable(
                                                "item.ic2.jetpack.mounted",
                                                Math.round(
                                                        JetpackMount.energy(
                                                                event.getItemStack()))));
                    }
                });
    }

    static String itemId(ItemStack stack) {
        return ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
    }

    static boolean chestArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armor && armor.getSlot() == EquipmentSlot.CHEST;
    }

    static double packEnergy(ItemStack stack) {
        return ForgeGear.stored(stack);
    }

    static double read(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(MOUNTED)
                ? stack.getTag().getDouble(MOUNTED)
                : -1;
    }

    static void write(ItemStack stack, double eu) {
        stack.getOrCreateTag().putDouble(MOUNTED, eu);
    }

    static ItemStack recover(double eu) {
        var pack =
                new ItemStack(
                        ForgeRegistries.ITEMS.getValue(
                                new ResourceLocation("ic2", "electric_jetpack")));
        ForgeGear.restoreEnergy(pack, eu);
        return pack;
    }

    private static final class AttachRecipe extends CustomRecipe {
        AttachRecipe(ResourceLocation id) {
            super(id);
        }

        @Override
        public boolean matches(CraftingContainer input, Level level) {
            return !assemble(input).isEmpty();
        }

        @Override
        public ItemStack assemble(CraftingContainer input) {
            List<ItemStack> stacks = new ArrayList<>(input.getContainerSize());
            for (int slot = 0; slot < input.getContainerSize(); slot++)
                stacks.add(input.getItem(slot));
            return JetpackMount.craft(stacks);
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return width * height >= 3;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return ATTACHMENT.get();
        }
    }
}
