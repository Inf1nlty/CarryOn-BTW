package tschipp.carryon.mixin;

import net.minecraft.src.*;

import tschipp.carryon.CarryOnHelper;
import tschipp.carryon.PickupHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true)
    public void onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int face, float offsetX, float offsetY, float offsetZ, CallbackInfoReturnable<Boolean> info)
    {
        ItemStack held = player.getCurrentEquippedItem();

        // Block activation is suppressed on BOTH sides when carrying.
        if (CarryOnHelper.isCarryStack(held))
        {
            info.setReturnValue(false);
            info.cancel();
            return;
        }

        // Client-side pickup hint only — actual pickup is handled server-side in NetServerHandlerMixin.
        if (!world.isRemote) return;

        if (player.isSneaking() && held == null && PickupHandler.isFunctionalBlock((Block)(Object) this))
        {
            int footY = MathHelper.ceiling_double_int(player.posY);
            if (y < footY - 1) return;

            int underX = MathHelper.floor_double(player.posX);
            int underZ = MathHelper.floor_double(player.posZ);
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                {
                    Block underBlock = Block.blocksList[world.getBlockId(underX + dx, footY - 1, underZ + dz)];
                    if (underBlock != null && PickupHandler.isFunctionalBlock(underBlock)) return;
                }

            info.setReturnValue(false);
            info.cancel();
        }
    }
}