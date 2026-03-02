package tschipp.carryon.item;

import btw.block.blocks.*;

import net.minecraft.src.*;

import tschipp.carryon.CarryOnData;

public class ItemTile extends Item {

    public static final String TILE_DATA_KEY = "tileData";

    public ItemTile(int id) {
        super(id);
        this.setMaxStackSize(1);
        this.setUnlocalizedName("carryon.tile_item");
        this.setTextureName("carryon:carryon_tile");
    }

    /**
     * Called by activateBlockOrUseItem when the player right-clicks a block face
     * (Packet15Place direction = 0..5).  This is the primary placement path in BTW.
     */
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ)
    {
        return doPlace(stack, world, player, x, y, z, side);
    }

    /**
     * Called by tryUseItem when the player right-clicks in the air (direction = 255).
     * Delegates to the same placement logic via a ray-trace.
     */
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
    {
        if (!hasTileData(stack)) return stack;

        MovingObjectPosition hit = player.rayTrace(5.0, 1.0f);

        if (hit == null || hit.typeOfHit != EnumMovingObjectType.TILE) return stack;

        doPlace(stack, world, player, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit);

        return stack;
    }

    /** Core placement logic shared by onItemUse and onItemRightClick. Returns true on success. */
    private boolean doPlace(ItemStack stack, World world, EntityPlayer player, int x, int y, int z, int side)
    {
        if (!hasTileData(stack)) return false;

        // Compute target position from the clicked face
        int placeX = x, placeY = y, placeZ = z;
        if      (side == 0) placeY--;
        else if (side == 1) placeY++;
        else if (side == 2) placeZ--;
        else if (side == 3) placeZ++;
        else if (side == 4) placeX--;
        else if (side == 5) placeX++;

        // If the clicked block is replaceable, place directly on it
        Block clicked = Block.blocksList[world.getBlockId(x, y, z)];
        if (clicked != null && clicked.blockMaterial.isReplaceable())
        {
            placeX = x; placeY = y; placeZ = z;
        }

        Block containedBlock = getBlock(stack);
        int containedMeta = getMeta(stack);
        if (containedBlock == null || containedBlock.blockID == 0) return false;

        // Target space must be empty or replaceable
        int existingId = world.getBlockId(placeX, placeY, placeZ);
        Block existing = Block.blocksList[existingId];
        if (existingId != 0 && (existing == null || !existing.blockMaterial.isReplaceable())) return false;

        // Must not overlap with the player's bounding box
        AxisAlignedBB playerBB = player.boundingBox;
        AxisAlignedBB placeBB  = AxisAlignedBB.getAABBPool().getAABB(placeX, placeY, placeZ, placeX + 1, placeY + 1, placeZ + 1);
        if (playerBB.intersectsWith(placeBB)) return false;

        int finalMeta;

        if (containedBlock instanceof AnvilBlock || containedBlock instanceof SoulforgeBlock || containedBlock instanceof DormantSoulforgeBlock)
        {
            finalMeta = containedMeta;
        }
        else
        {
            finalMeta = containedBlock.preBlockPlacedBy(world, placeX, placeY, placeZ, containedMeta, player);
        }

        world.setBlock(placeX, placeY, placeZ, containedBlock.blockID, finalMeta, 3);

        if (world.getBlockId(placeX, placeY, placeZ) != containedBlock.blockID)
        {
            if (!world.isRemote)
            {
                ((EntityPlayerMP) player).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(placeX, placeY, placeZ, world));
                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
            else
            {
                world.setBlockToAir(placeX, placeY, placeZ);
            }
            return false;
        }

        // For blocks whose onBlockPlacedBy re-computes facing from the player's current
        // orientation (SoulforgeBlock, AnvilBlock), skip it and restore the stored meta
        // so the block faces the same direction it had when picked up.
        boolean skipFacingRecalculate = (containedBlock instanceof AnvilBlock) || (containedBlock instanceof SoulforgeBlock || (containedBlock instanceof DormantSoulforgeBlock));

        containedBlock.onBlockPlacedBy(world, placeX, placeY, placeZ, player, stack);

        if (skipFacingRecalculate)
        {
            // onBlockPlacedBy just overwrote the facing — restore the original stored meta.
            world.setBlockMetadataWithNotify(placeX, placeY, placeZ, finalMeta, 3);
        }

        world.playSoundEffect(placeX + 0.5, placeY + 0.5, placeZ + 0.5,
                containedBlock.stepSound.getPlaceSound(),
                (containedBlock.stepSound.getVolume() + 1.0F) / 2.0F,
                containedBlock.stepSound.getPitch() * 0.8F);

        // Restore tile entity NBT on the server only
        if (!world.isRemote)
        {
            NBTTagCompound tileData = getTileData(stack);
            if (tileData != null && !tileData.hasNoTags())
            {
                TileEntity tileEntity = world.getBlockTileEntity(placeX, placeY, placeZ);
                if (tileEntity != null)
                {
                    tileData.setInteger("x", placeX);
                    tileData.setInteger("y", placeY);
                    tileData.setInteger("z", placeZ);
                    tileEntity.readFromNBT(tileData);
                }
            }
        }

        clearTileData(stack);
        stack.stackSize = 0;
        // Client-side slot clearing is handled by PlayerControllerMixin.carryon$clearSlotAfterPlace
        // which fires at the RETURN of onPlayerRightClick — covering all game modes reliably.

        return true;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, EntityPlayer entity, int itemSlot, boolean isSelected)
    {
        if (hasTileData(stack))
        {
            if (entity.capabilities.isCreativeMode) return;

            entity.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 1, potionLevel(stack), false));
            // Food exhaustion while carrying is handled in EntityPlayerMixin.carryon$onUpdate
            // because Item.onUpdate is never called in BTW (ItemStack.updateAnimation has no caller).
        }
        else if (isSelected && !world.isRemote)
        {
            stack.stackSize = 0;
        }
    }

    public static boolean hasTileData(ItemStack stack)
    {
        return stack != null && stack.stackTagCompound != null && stack.stackTagCompound.hasKey(TILE_DATA_KEY) && stack.stackTagCompound.hasKey("blockId");
    }

    public static boolean storeTileData(TileEntity tile, World world, int x, int y, int z, ItemStack stack)
    {
        if (stack == null || stack.stackSize == 0) return false;

        NBTTagCompound tileNbt = new NBTTagCompound();

        if (tile != null) tile.writeToNBT(tileNbt);

        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();

        NBTTagCompound tag = stack.stackTagCompound;

        if (tag.hasKey(TILE_DATA_KEY)) return false;

        tag.setCompoundTag(TILE_DATA_KEY, tileNbt);
        tag.setInteger("blockId", world.getBlockId(x, y, z));
        tag.setInteger("blockMeta", world.getBlockMetadata(x, y, z));
        tag.setByte(CarryOnData.NO_DROP_KEY, (byte) 1);

        return true;
    }

    public static void clearTileData(ItemStack stack)
    {
        if (stack != null && stack.stackTagCompound != null)
        {
            stack.stackTagCompound.removeTag(TILE_DATA_KEY);
            stack.stackTagCompound.removeTag("blockId");
            stack.stackTagCompound.removeTag("blockMeta");
            stack.stackTagCompound.removeTag(CarryOnData.NO_DROP_KEY);
        }
    }

    public static NBTTagCompound getTileData(ItemStack stack)
    {
        if (stack != null && stack.stackTagCompound != null && stack.stackTagCompound.hasKey(TILE_DATA_KEY))
            return stack.stackTagCompound.getCompoundTag(TILE_DATA_KEY);

        return null;
    }

    public static Block getBlock(ItemStack stack)
    {
        if (stack != null && stack.stackTagCompound != null && stack.stackTagCompound.hasKey("blockId"))
            return Block.blocksList[stack.stackTagCompound.getInteger("blockId")];

        return null;
    }

    public static int getMeta(ItemStack stack)
    {
        if (stack != null && stack.stackTagCompound != null && stack.stackTagCompound.hasKey("blockMeta"))
            return stack.stackTagCompound.getInteger("blockMeta");

        return 0;
    }

    public static boolean isLocked(int x, int y, int z, World world)
    {
        TileEntity te = world.getBlockTileEntity(x, y, z);

        if (te == null) return false;

        NBTTagCompound tag = new NBTTagCompound();

        te.writeToNBT(tag);

        return tag.hasKey("Lock") && !tag.getString("Lock").isEmpty();
    }

    private int potionLevel(ItemStack stack)
    {
        NBTTagCompound tileData = getTileData(stack);

        if (tileData == null) return 1;

        return Math.max(1, Math.min(4, tileData.toString().length() / 500));
    }
}