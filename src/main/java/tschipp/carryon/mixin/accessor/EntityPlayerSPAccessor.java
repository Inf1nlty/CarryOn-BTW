package tschipp.carryon.mixin.accessor;

import net.minecraft.src.EntityPlayerSP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPlayerSP.class)
public interface EntityPlayerSPAccessor {

    @Accessor("exhaustionAddedSinceLastGuiUpdate")
    void carryOn$setExhaustionAddedSinceLastGuiUpdate(boolean value);
}