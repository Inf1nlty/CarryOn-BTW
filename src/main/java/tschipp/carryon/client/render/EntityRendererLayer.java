package tschipp.carryon.client.render;

import net.minecraft.src.*;

import tschipp.carryon.CarryOnItems;
import tschipp.carryon.item.ItemEntity;

import org.lwjgl.opengl.GL11;

public class EntityRendererLayer {

    public static void renderThirdPerson(AbstractClientPlayer player, float partialTicks)
    {
        ItemStack stack = player.getCurrentEquippedItem();

        if (stack == null || stack.getItem() != CarryOnItems.ENTITY_ITEM) return;

        if (!ItemEntity.hasEntityData(stack)) return;

        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null) return;

        Entity renderEntity = ItemEntity.getEntity(stack, player.worldObj);

        if (renderEntity == null) return;

        renderEntity.setPosition(
                player.prevPosX + (player.posX - player.prevPosX) * partialTicks,
                player.prevPosY + (player.posY - player.prevPosY) * partialTicks,
                player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks);

        renderEntity.rotationYaw      = 0f; renderEntity.prevRotationYaw   = 0f;
        renderEntity.rotationPitch    = 0f; renderEntity.prevRotationPitch = 0f;

        if (renderEntity instanceof EntityLivingBase living)
        {
            living.rotationYawHead = 0f; living.prevRotationYawHead = 0f;
        }

        float height     = renderEntity.height;
        float width      = renderEntity.width;
        float multiplier = height * width;

        GL11.glPushMatrix();
        GL11.glScaled(1, -1, 1);
        GL11.glScaled((10 - multiplier) * 0.08, (10 - multiplier) * 0.08, (10 - multiplier) * 0.08);
        GL11.glRotated(180, 0, 1, 0);
        GL11.glTranslated(0.0, -1.2, (Math.max(width - 0.1, 0.7)) + 0.1);
        GL11.glColor3f(1f, 1f, 1f);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        if (player.isSneaking()) GL11.glTranslated(0, -0.1, 0);

        RenderManager.instance.renderEntityWithPosYaw(renderEntity, 0, 0, 0, 0, partialTicks);

        GL11.glScaled(1, 1, 1);
        GL11.glPopMatrix();
    }

    public static void renderFirstPerson(EntityLivingBase player, ItemStack stack, float partialTicks)
    {
        if (stack == null || stack.getItem() != CarryOnItems.ENTITY_ITEM) return;

        if (!ItemEntity.hasEntityData(stack)) return;

        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null) return;

        Entity renderEntity = ItemEntity.getEntity(stack, player.worldObj);

        if (renderEntity == null) return;

        renderEntity.setPosition(
                player.prevPosX + (player.posX - player.prevPosX) * partialTicks,
                player.prevPosY + (player.posY - player.prevPosY) * partialTicks,
                player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks);

        renderEntity.rotationYaw      = 0f; renderEntity.prevRotationYaw   = 0f;
        renderEntity.rotationPitch    = 0f; renderEntity.prevRotationPitch = 0f;

        if (renderEntity instanceof EntityLivingBase living)
        {
            living.rotationYawHead = 0f; living.prevRotationYawHead = 0f;
        }

        float height = renderEntity.height;
        float width  = renderEntity.width;

        RenderHelper.enableStandardItemLighting();
        GL11.glPushMatrix();
        GL11.glScaled(.55, .55, .55);
        GL11.glRotated(180, 0, 1, 0);
        GL11.glTranslated(0.0, -height - .1, width + 0.6);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        setLightCoords(player);
        RenderManager.instance.renderEntityWithPosYaw(renderEntity, 0, 0, 0, 0, partialTicks);

        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glScaled(1, 1, 1);
        GL11.glPopMatrix();
        RenderHelper.disableStandardItemLighting();
    }

    private static void setLightCoords(EntityLivingBase player)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.theWorld == null) return;

        int x = (int) player.posX;
        int y = (int) (player.posY + player.getEyeHeight());
        int z = (int) player.posZ;
        int lightValue = mc.theWorld.getLightBrightnessForSkyBlocks(x, y, z, 0);

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                (float)(lightValue & 0xFFFF), (float)(lightValue >> 16));
    }
}