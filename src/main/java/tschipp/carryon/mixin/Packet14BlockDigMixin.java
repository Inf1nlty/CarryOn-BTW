package tschipp.carryon.mixin;

import net.minecraft.src.*;

import tschipp.carryon.CarryOnHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts Packet14BlockDig.processPacket before it reaches NetServerHandler.
 * NetServerHandler does not override handleBlockDig, so we hook the packet itself.
 */
@Mixin(Packet14BlockDig.class)
public class Packet14BlockDigMixin {

    @Inject(method = "processPacket", at = @At("HEAD"), cancellable = true)
    private void carryon$cancelDigWhileCarrying(NetHandler handler, CallbackInfo ci)
    {
        if (!handler.isServerHandler()) return;

        if (!(handler instanceof NetServerHandler)) return;

        EntityPlayerMP player = ((NetServerHandler) handler).playerEntity;
        if (player == null) return;

        ItemStack held = player.inventory.getCurrentItem();
        if (!CarryOnHelper.isCarryStack(held)) return;

        ci.cancel();
    }
}