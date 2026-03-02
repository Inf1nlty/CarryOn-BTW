package tschipp.carryon.mixin.client;

import net.minecraft.src.*;

import tschipp.carryon.client.render.ICarrying;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBiped.class)
public class ModelBipedMixin implements ICarrying {

    @Unique private boolean isCarryingBlock;
    @Unique private boolean isCarryingEntity;

    @Shadow public ModelRenderer bipedRightArm;
    @Shadow public ModelRenderer bipedLeftArm;
    @Shadow public boolean isSneak;

    @Override
    public boolean carryOn$isCarryingBlock() {
        return isCarryingBlock;
    }

    @Override
    public boolean carryOn$isCarryingEntity() {
        return isCarryingEntity;
    }

    @Override
    public void carryOn$setCarryingBlock(boolean block) {
        isCarryingBlock = block;
    }

    @Override
    public void carryOn$setCarryingEntity(boolean entity) {
        isCarryingEntity = entity;
    }

    @Inject(method = "setRotationAngles", at = @At("RETURN"))
    public void onSetAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entity, CallbackInfo info)
    {
        if (isCarryingBlock)
        {
            float x = -1F + (isSneak ? 0f : 0.2f);
            bipedRightArm.rotateAngleX = x; bipedRightArm.rotateAngleY = 0f; bipedRightArm.rotateAngleZ =  0f;
            bipedLeftArm.rotateAngleX  = x; bipedLeftArm.rotateAngleY  = 0f; bipedLeftArm.rotateAngleZ  =  0f;
        }

        else if (isCarryingEntity)
        {
            float x = -1.2F + (isSneak ? 0f : 0.2f);
            bipedRightArm.rotateAngleX = x; bipedRightArm.rotateAngleY = 0f; bipedRightArm.rotateAngleZ = -0.15f;
            bipedLeftArm.rotateAngleX  = x; bipedLeftArm.rotateAngleY  = 0f; bipedLeftArm.rotateAngleZ  =  0.15f;
        }
    }
}