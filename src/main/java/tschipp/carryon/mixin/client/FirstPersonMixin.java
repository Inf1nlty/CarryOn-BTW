package tschipp.carryon.mixin.client;

import net.minecraft.src.*;

import tschipp.carryon.CarryOnItems;
import tschipp.carryon.client.render.BlockRendererLayer;
import tschipp.carryon.client.render.EntityRendererLayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class FirstPersonMixin {

    @Shadow private ItemStack itemToRender;

    @Inject(method = "renderItemInFirstPerson", at = @At("HEAD"), cancellable = true)
    public void onRenderItem(float partialTicks, CallbackInfo info)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null) return;

        ItemStack stack = mc.thePlayer.getCurrentEquippedItem();

        if (stack == null && itemToRender != null
                && (itemToRender.getItem() == CarryOnItems.TILE_ITEM
                 || itemToRender.getItem() == CarryOnItems.ENTITY_ITEM))
        {
            itemToRender = null;
            return;
        }

        if (stack == null) return;

        if (stack.getItem() == CarryOnItems.TILE_ITEM)
        {
            info.cancel();
            BlockRendererLayer.renderFirstPerson(mc.thePlayer, stack, partialTicks);
        }
        else if (stack.getItem() == CarryOnItems.ENTITY_ITEM)
        {
            info.cancel();
            EntityRendererLayer.renderFirstPerson(mc.thePlayer, stack, partialTicks);
        }
    }
}