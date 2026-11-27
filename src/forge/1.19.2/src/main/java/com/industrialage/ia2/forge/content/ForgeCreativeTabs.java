package com.industrialage.ia2.forge.content;

import com.industrialage.ia2.content.CreativeCategory;
import java.util.EnumMap;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Binds the item families to Forge 1.19's native tab pages. */
final class ForgeCreativeTabs {
    private static final EnumMap<CreativeCategory, CreativeModeTab> TABS =
            new EnumMap<>(CreativeCategory.class);

    static {
        for (var category : CreativeCategory.values()) {
            TABS.put(category, new Tab(category));
        }
    }

    private ForgeCreativeTabs() {}

    static CreativeModeTab forItem(String path) {
        return TABS.get(CreativeCategory.forItem(path));
    }

    private static Item item(String path) {
        return ForgeRegistries.ITEMS.getValue(ForgeContent.id(path));
    }

    private static final class Tab extends CreativeModeTab {
        private final CreativeCategory category;

        Tab(CreativeCategory category) {
            super(category.label());
            this.category = category;
        }

        @Override
        public ItemStack makeIcon() {
            return new ItemStack(item(category.icon()));
        }

        @Override
        public void fillItemList(NonNullList<ItemStack> items) {
            for (var path : category.entries()) {
                var item = item(path);
                if (item != null) item.fillItemCategory(this, items);
            }
        }
    }
}
