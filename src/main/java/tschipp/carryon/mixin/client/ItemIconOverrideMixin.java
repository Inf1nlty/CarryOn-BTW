package tschipp.carryon.mixin.client;

import net.minecraft.src.*;

import tschipp.carryon.CarryOnItems;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects into ItemRenderer.renderItem to completely suppress the default
 * sprite render when holding a carry item.
 * Third-person block rendering is handled by BlockRendererLayer (called from
 * PlayerRendererMixin.renderSpecials). If we let renderItem also run, it draws
 * a sprite quad on top of / around the block, causing the "stray texture" bleed.
 * First-person rendering is already cancelled in FirstPersonMixin, so this mixin
 * only needs to guard the third-person path (renderItem called from RenderPlayer).
 */
@Mixin(ItemRenderer.class)
public class ItemIconOverrideMixin {

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    public void onRenderItemHead(EntityLivingBase entity, ItemStack stack, int pass, CallbackInfo ci) {
        if (stack == null) return;
        // Completely suppress sprite rendering for carry items in ALL contexts.
        // Block: rendered as a full 3D block by BlockRendererLayer.renderThirdPerson
        // Entity: rendered as a 3D entity by EntityRendererLayer.renderThirdPerson
        // In both cases, drawing a sprite here would cause texture pollution.
        if (stack.getItem() == CarryOnItems.TILE_ITEM
                || stack.getItem() == CarryOnItems.ENTITY_ITEM) {
            ci.cancel();
        }
    }
}