package tschipp.carryon.client.render;

import btw.block.blocks.*;

import net.minecraft.src.*;

import tschipp.carryon.CarryOnItems;
import tschipp.carryon.item.ItemTile;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class BlockRendererLayer {

    /** Render-type value returned by BlockBrewingStand.getRenderType() */
    private static final int RENDER_TYPE_BREWING_STAND = 25;

    public static void renderThirdPerson(AbstractClientPlayer player, float partialTicks)
    {
        ItemStack stack = player.getCurrentEquippedItem();

        if (stack == null || stack.getItem() != CarryOnItems.TILE_ITEM) return;

        if (!ItemTile.hasTileData(stack)) return;

        Block block = ItemTile.getBlock(stack);

        if (block == null || block.blockID == 0) return;

        int meta = ItemTile.getMeta(stack);

        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null) return;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        RenderHelper.enableStandardItemLighting();
        setLightCoords(player);
        GL11.glPushMatrix();
        GL11.glRotated(180, 1, 0, 0);
        GL11.glRotated(180, 0, 1, 0);
        GL11.glScaled(0.6, 0.6, 0.6);
        GL11.glTranslated(0, -0.75, -0.65);

        if (player.isSneaking()) GL11.glTranslated(0, -0.15, -0.15);

        if (isChest(block))
        {
            GL11.glRotated(180, 0, 1, 0);
            renderChestWithMeta(block, 5, 1.0f);
        }
        else if (block.getRenderType() == RENDER_TYPE_BREWING_STAND)
        {
            renderBrewingStand(meta);
        }
        else if (block instanceof AnvilBlock || block instanceof SoulforgeBlock || block instanceof DormantSoulforgeBlock)
        {
            GL11.glRotatef(90f, 0f, 1f, 0f);
            block.renderBlockAsItem(new RenderBlocks(), meta, 1.0f);
        }
        else
        {
            block.renderBlockAsItem(new RenderBlocks(), meta, 1.0f);
        }

        GL11.glPopMatrix();
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopAttrib();
    }

    public static void renderFirstPerson(EntityLivingBase entity, ItemStack stack, float partialTicks)
    {
        if (stack == null || stack.getItem() != CarryOnItems.TILE_ITEM) return;

        if (!ItemTile.hasTileData(stack)) return;

        Block block = ItemTile.getBlock(stack);

        if (block == null || block.blockID == 0) return;

        int meta = ItemTile.getMeta(stack);

        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null) return;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        RenderHelper.enableStandardItemLighting();
        setLightCoords(entity);
        GL11.glPushMatrix();
        GL11.glScaled(1.6, 1.6, 1.6);
        GL11.glTranslated(0, -0.55, -1.4);

        if (isChest(block))
        {
            GL11.glRotated(180, 0, 1, 0);
            // Fixed meta=2: latch always faces outward (same as third-person).
            renderChestWithMeta(block, 5, 1.0f);
        }
        else if (block.getRenderType() == RENDER_TYPE_BREWING_STAND)
        {
            renderBrewingStand(meta);
        }
        else if (block instanceof AnvilBlock || block instanceof SoulforgeBlock || block instanceof DormantSoulforgeBlock)
        {
            GL11.glRotatef(90f, 0f, 1f, 0f);
            block.renderBlockAsItem(new RenderBlocks(), meta, 1.0f);
        }
        else
        {
            block.renderBlockAsItem(new RenderBlocks(), meta, 1.0f);
        }

        GL11.glPopMatrix();
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopAttrib();
    }

    public static boolean isChest(Block block)
    {
        if (block instanceof BlockChest)    return true;
        if (block instanceof BlockEnderChest) return true;
        return false;
    }

    /**
     * Renders the brewing stand by delegating to the original
     * {@code RenderBlocks.renderBlockByRenderType} path via a minimal
     * {@link IBlockAccess} stub.
     */
    private static void renderBrewingStand(int meta)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;

        BlockBrewingStand block = (BlockBrewingStand) Block.brewingStand;

        World world = mc.theWorld;
        int bx = ((int) mc.thePlayer.posX & ~15);
        int by = 255;
        int bz = ((int) mc.thePlayer.posZ & ~15);

        int savedId   = world.getBlockId(bx, by, bz);
        int savedMeta = world.getBlockMetadata(bx, by, bz);

        world.setBlock(bx, by, bz, block.blockID, meta, 0);

        IBlockAccess brightWorld = new BrightBlockAccess(world);
        RenderBlocks rb = new RenderBlocks(brightWorld);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setBrightness(0xF000F0);
        tessellator.setTranslation(-bx - 0.5, -by, -bz - 0.5);

        rb.renderBlockByRenderType(block, bx, by, bz);

        tessellator.draw();
        tessellator.setTranslation(0, 0, 0);

        world.setBlock(bx, by, bz, savedId, savedMeta, 0);
    }

    /**
     * Thin {@link IBlockAccess} wrapper that returns full-bright values for all
     * brightness queries, forwarding all other calls to the delegate.
     */
    private record BrightBlockAccess(IBlockAccess delegate) implements IBlockAccess {

        @Override
        public int getLightBrightnessForSkyBlocks(int x, int y, int z, int min) {
            return 0xF000F0;
        }

        @Override
        public float getLightBrightness(int x, int y, int z) {
            return 1.0f;
        }

        @Override
        public float getBrightness(int x, int y, int z, int face) {
            return 1.0f;
        }

        @Override
        public int getBlockId(int x, int y, int z) {
            return delegate.getBlockId(x, y, z);
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            return delegate.getBlockMetadata(x, y, z);
        }

        @Override
        public TileEntity getBlockTileEntity(int x, int y, int z) {
            return delegate.getBlockTileEntity(x, y, z);
        }

        @Override
        public Material getBlockMaterial(int x, int y, int z) {
            return delegate.getBlockMaterial(x, y, z);
        }

        @Override
        public boolean isAirBlock(int x, int y, int z) {
            return delegate.isAirBlock(x, y, z);
        }

        @Override
        public boolean isBlockOpaqueCube(int x, int y, int z) {
            return delegate.isBlockOpaqueCube(x, y, z);
        }

        @Override
        public boolean isBlockNormalCube(int x, int y, int z) {
            return delegate.isBlockNormalCube(x, y, z);
        }

        @Override
        public boolean doesBlockHaveSolidTopSurface(int x, int y, int z) {
            return delegate.doesBlockHaveSolidTopSurface(x, y, z);
        }

        @Override
        public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
            return delegate.isBlockProvidingPowerTo(x, y, z, side);
        }

        @Override
        public BiomeGenBase getBiomeGenForCoords(int x, int z) {
            return delegate.getBiomeGenForCoords(x, z);
        }

        @Override
        public int getHeight() {
            return delegate.getHeight();
        }

        @Override
        public boolean extendedLevelsInChunkCache() {
            return delegate.extendedLevelsInChunkCache();
        }

        @Override
        public Vec3Pool getWorldVec3Pool() {
            return delegate.getWorldVec3Pool();
        }
    }

    private static void renderChestWithMeta(Block block, int meta, float brightness)
    {
        float extra;
        if      (meta == 2) extra = 180f;
        else if (meta == 3) extra =   0f;
        else if (meta == 4) extra =  90f;
        else if (meta == 5) extra = -90f;
        else                extra =   0f;

        if (extra != 0f) GL11.glRotatef(extra, 0f, 1f, 0f);
        block.renderBlockAsItem(new RenderBlocks(), meta, brightness);
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