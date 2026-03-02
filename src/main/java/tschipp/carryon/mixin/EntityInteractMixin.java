package tschipp.carryon.mixin;

import net.minecraft.src.*;

import tschipp.carryon.PickupHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityAnimal.class, EntityVillager.class})
public abstract class EntityInteractMixin {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    public void onInteract(EntityPlayer player, CallbackInfoReturnable<Boolean> info)
    {
        if (!player.worldObj.isRemote) return;

        if (!player.isSneaking() || player.getCurrentEquippedItem() != null) return;

        Entity entity = (Entity)(Object) this;

        if (entity.isDead) return;

        if (!PickupHandler.canPlayerPickUpEntity(player, entity)) return;

        info.setReturnValue(true);
        info.cancel();
    }

}