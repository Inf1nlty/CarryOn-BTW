package tschipp.carryon;

import net.minecraft.src.CreativeTabs;
import net.minecraft.src.Item;

import tschipp.carryon.item.ItemEntity;
import tschipp.carryon.item.ItemTile;

public class CarryOnItems {

    public static Item TILE_ITEM;
    public static Item ENTITY_ITEM;

    public static void registerItems() {

        TILE_ITEM = new ItemTile(23601)
                .setCreativeTab(CreativeTabs.tabMisc);
        Item.itemsList[TILE_ITEM.itemID] = TILE_ITEM;

        ENTITY_ITEM = new ItemEntity(23602)
                .setCreativeTab(CreativeTabs.tabMisc);
        Item.itemsList[ENTITY_ITEM.itemID] = ENTITY_ITEM;
    }
}