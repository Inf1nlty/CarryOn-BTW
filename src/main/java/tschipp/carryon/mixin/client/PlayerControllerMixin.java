package tschipp.carryon.mixin.client;

import net.minecraft.src.*;


import tschipp.carryon.CarryOnHelper;
import tschipp.carryon.CarryOnItems;
import tschipp.carryon.item.ItemTile;
import tschipp.carryon.mixin.accessor.EntityPlayerSPAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents digging and entity-attacking while the player is carrying a block or entity.
 */
@Mixin(PlayerControllerMP.class)
public class PlayerControllerMixin {

    /** Suppress initial left-click on block (starts digging). */
    @Inject(method = "clickBlock(IIII)V", at = @At("HEAD"), cancellable = true)
    private void carryon$blockClickBlock(int x, int y, int z, int face, CallbackInfo ci)
    {
        if (CarryOnHelper.isCarrying(Minecraft.getMinecraft().thePlayer)) ci.cancel();
    }

    /**
     * While carrying a tile and moving on the ground, set exhaustionAddedSinceLastGuiUpdate=true
     * so the HUD food bar shakes — the same visual feedback as sprinting or swimming.
     */
    @Inject(method = "updateController", at = @At("RETURN"))
    private void carryon$updateControllerExhaustion(CallbackInfo ci)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        EntityPlayer player = mc.thePlayer;

        ItemStack held = player.inventory.getCurrentItem();
        if (!CarryOnHelper.isCarryStack(held)) return;
        if (!ItemTile.hasTileData(held)) return;

        // Only trigger visual feedback when actually moving on the ground
        double dx = player.posX - player.prevPosX;
        double dz = player.posZ - player.prevPosZ;
        if (player.onGround && (dx * dx + dz * dz) > 0.0)
        {
            ((EntityPlayerSPAccessor) player).carryOn$setExhaustionAddedSinceLastGuiUpdate(true);
        }
    }

    /** Suppress continued digging progress, crack animation and particles. */
    @Inject(method = "onPlayerDamageBlock(IIII)V", at = @At("HEAD"), cancellable = true)
    private void carryon$blockDamageBlock(int x, int y, int z, int face, CallbackInfo ci)
    {
        if (CarryOnHelper.isCarrying(Minecraft.getMinecraft().thePlayer)) ci.cancel();
    }

    /** Suppress left-click entity attack (arm swing animation + attack). */
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void carryon$blockLeftClickEntity(EntityPlayer player, Entity target, CallbackInfo ci)
    {
        if (CarryOnHelper.isCarrying(Minecraft.getMinecraft().thePlayer)) ci.cancel();
    }

    /**
     * After a successful right-click with a carry item, forcibly clear the client slot
     * and reset the equipped-item animation.
     *
     * This is necessary because PlayerControllerMP.onPlayerRightClick does NOT clear the
     * inventory slot after consuming an item — it leaves that to Minecraft.clickMouse which
     * checks var3.stackSize == 0.  However, in creative mode clickMouse restores stackSize
     * AFTER tryPlaceItemIntoWorld returns, so the slot is never cleared.  In survival the
     * slot check works in theory but the animation never resets (Minecraft.clickMouse only
     * calls resetEquippedProgress in the else-if branch, not when stackSize becomes 0).
     *
     * Intercepting here, at the return of onPlayerRightClick, covers every path cleanly.
     */
    @Inject(method = "onPlayerRightClick", at = @At("RETURN"))
    private void carryon$clearSlotAfterPlace(
            EntityPlayer player, World world, ItemStack stack,
            int x, int y, int z, int side, Vec3 hitVec,
            CallbackInfoReturnable<Boolean> cir)
    {
        if (!cir.getReturnValue()) return;   // placement did not succeed

        if (stack == null) return;
        if (stack.getItem() != CarryOnItems.TILE_ITEM
         && stack.getItem() != CarryOnItems.ENTITY_ITEM) return;

        // The item was just placed — force the client to drop the slot immediately.
        int slot = player.inventory.currentItem;
        player.inventory.mainInventory[slot] = null;
        Minecraft.getMinecraft().entityRenderer.itemRenderer.resetEquippedProgress();
    }
}