package tschipp.carryon;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;

public final class CarryOnHelper {

    private CarryOnHelper() {}

    /**
     * Returns true if the player is currently holding a CarryOn carry item
     * (either a carried block or a carried entity).
     */
    public static boolean isCarrying(EntityPlayer player) {

        if (player == null) return false;

        ItemStack held = player.getCurrentEquippedItem();
        return held != null && (held.getItem() == CarryOnItems.TILE_ITEM || held.getItem() == CarryOnItems.ENTITY_ITEM);
    }

    /**
     * Returns true if the given ItemStack is a CarryOn carry item.
     */
    public static boolean isCarryStack(ItemStack stack) {

        if (stack == null) return false;

        return stack.getItem() == CarryOnItems.TILE_ITEM || stack.getItem() == CarryOnItems.ENTITY_ITEM;
    }
}