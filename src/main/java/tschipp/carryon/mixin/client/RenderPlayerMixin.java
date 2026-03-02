package tschipp.carryon.mixin.client;

import net.minecraft.src.*;

import tschipp.carryon.CarryOnItems;
import tschipp.carryon.client.render.BlockRendererLayer;
import tschipp.carryon.client.render.EntityRendererLayer;
import tschipp.carryon.client.render.ICarrying;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public abstract class RenderPlayerMixin {

    @Shadow private ModelBiped modelBipedMain;

    @Inject(method = "renderSpecials", at = @At("RETURN"))
    private void onRenderSpecials(AbstractClientPlayer player, float partialTick, CallbackInfo info)
    {
        ItemStack stack  = player.getCurrentEquippedItem();
        ICarrying model  = (ICarrying) this.modelBipedMain;

        if (stack != null && stack.getItem() == CarryOnItems.TILE_ITEM)
        {
            model.carryOn$setCarryingBlock(true);
            model.carryOn$setCarryingEntity(false);
            BlockRendererLayer.renderThirdPerson(player, partialTick);

        }
        else if (stack != null && stack.getItem() == CarryOnItems.ENTITY_ITEM)
        {
            model.carryOn$setCarryingBlock(false);
            model.carryOn$setCarryingEntity(true);
            EntityRendererLayer.renderThirdPerson(player, partialTick);
        }
        else
        {
            model.carryOn$setCarryingBlock(false);
            model.carryOn$setCarryingEntity(false);
        }
    }
}