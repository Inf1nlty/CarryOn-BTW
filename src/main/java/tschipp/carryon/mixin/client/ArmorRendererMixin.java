package tschipp.carryon.mixin.client;

import net.minecraft.src.*;

import tschipp.carryon.CarryOnItems;
import tschipp.carryon.client.render.ICarrying;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderPlayer.class)
public abstract class ArmorRendererMixin {

    @Shadow private ModelBiped modelArmorChestplate;
    @Shadow private ModelBiped modelArmor;

    @Inject(method = "setArmorModel", at = @At("RETURN"))
    private void onSetArmorModel(AbstractClientPlayer player, int pass, float partialTick, CallbackInfoReturnable<Integer> info)
    {
        ItemStack stack = player.getCurrentEquippedItem();

        boolean carryBlock = stack != null && stack.getItem() == CarryOnItems.TILE_ITEM;

        boolean carryEntity = stack != null && stack.getItem() == CarryOnItems.ENTITY_ITEM;

        if (modelArmorChestplate instanceof ICarrying carrying)
        {
            carrying.carryOn$setCarryingBlock(carryBlock); carrying.carryOn$setCarryingEntity(carryEntity);
        }

        if (modelArmor instanceof ICarrying carrying)
        {
            carrying.carryOn$setCarryingBlock(carryBlock); carrying.carryOn$setCarryingEntity(carryEntity);
        }
    }
}