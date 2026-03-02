package tschipp.carryon.mixin.client;

import net.minecraft.src.*;

import tschipp.carryon.CarryOnHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, priority = 10001)
public abstract class KeyboardMixin {

    @Shadow public EntityClientPlayerMP thePlayer;
    @Shadow private int rightClickDelayTimer;

    /**
     * Redirects inventory.changeCurrentItem(dwheel) — the scroll wheel path.
     * When carrying, we swallow the call entirely.
     */
    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "net/minecraft/src/InventoryPlayer.changeCurrentItem(I)V"))
    private void redirectScrollHotbar(InventoryPlayer inventory, int direction)
    {
        if (!CarryOnHelper.isCarrying(thePlayer))
            inventory.changeCurrentItem(direction);
    }

    /** Saved hotbar slot captured at the start of each tick. Only valid when {@code carryon_wasCarrying} is true. */
    @Unique private int carryon_savedSlot = 0;
    /** Whether the player was already carrying at the start of this tick. */
    @Unique private boolean carryon_wasCarrying = false;

    /** Capture the current hotbar slot at the very start of runTick. */
    @Inject(method = "runTick", at = @At("HEAD"))
    private void carryon_captureSlot(CallbackInfo ci)
    {
        carryon_wasCarrying = CarryOnHelper.isCarrying(thePlayer);

        if (carryon_wasCarrying && thePlayer != null)
            carryon_savedSlot = thePlayer.inventory.currentItem;
    }

    /**
     * Restore the hotbar slot at the end of runTick — only if the player was
     * already carrying at the start of this tick. This prevents slot changes
     * made by number keys or scroll wheel from taking effect while carrying,
     * without interfering with the pickup tick itself.
     */
    @Inject(method = "runTick", at = @At("TAIL"))
    private void carryon_restoreSlot(CallbackInfo ci)
    {
        if (thePlayer == null) return;

        boolean nowCarrying = CarryOnHelper.isCarrying(thePlayer);

        if (carryon_wasCarrying && nowCarrying)
            thePlayer.inventory.currentItem = carryon_savedSlot;

        // Just picked something up — block right-click for ~10 ticks so the player
        // must release and re-press before being able to place it back down.
        if (!carryon_wasCarrying && nowCarrying)
            rightClickDelayTimer = 10;
    }

    /** Zero out drop, attack, and inventory key press-time while carrying. */
    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "net/minecraft/src/KeyBinding.onTick(I)V"))
    private void redirectOnTick(int keyCode)
    {
        if (CarryOnHelper.isCarrying(thePlayer))
        {
            GameSettings settings = ((Minecraft) (Object) this).gameSettings;

            if (settings != null && keyCode == settings.keyBindDrop.keyCode) return;

            if (settings != null && keyCode == settings.keyBindAttack.keyCode) return;

            if (settings != null && keyCode == settings.keyBindInventory.keyCode) return;
        }

        KeyBinding.onTick(keyCode);
    }
}