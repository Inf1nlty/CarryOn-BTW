package tschipp.carryon.item;

import net.minecraft.src.*;

import tschipp.carryon.CarryOnData;

public class ItemEntity extends Item {

    public static final String ENTITY_DATA_KEY = "entityData";

    public ItemEntity(int id) {
        super(id);
        this.setMaxStackSize(1);
        this.setUnlocalizedName("carryon.entity_item");
        this.setTextureName("carryon:carryon_entity");
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
     */
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
    {
        if (!hasEntityData(stack)) return stack;

        MovingObjectPosition hit = player.rayTrace(5.0, 1.0f);

        if (hit == null || hit.typeOfHit != EnumMovingObjectType.TILE) return stack;

        doPlace(stack, world, player, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit);

        return stack;
    }

    /** Core placement logic. Returns true on success. */
    private boolean doPlace(ItemStack stack, World world, EntityPlayer player, int x, int y, int z, int side)
    {
        if (!hasEntityData(stack)) return false;

        int placeX = x, placeY = y, placeZ = z;
        if      (side == 0) placeY--;
        else if (side == 1) placeY++;
        else if (side == 2) placeZ--;
        else if (side == 3) placeZ++;
        else if (side == 4) placeX--;
        else if (side == 5) placeX++;

        Block clicked = Block.blocksList[world.getBlockId(x, y, z)];
        if (clicked != null && clicked.blockMaterial.isReplaceable())
        {
            placeX = x; placeY = y; placeZ = z;
        }

        // Target space must be clear of solid blocks on both sides
        if (world.getBlockId(placeX, placeY, placeZ) != 0)
        {
            Block there = Block.blocksList[world.getBlockId(placeX, placeY, placeZ)];
            if (there == null || !there.blockMaterial.isReplaceable()) return false;
        }

        if (!world.isRemote)
        {
            Entity entity = getEntity(stack, world);
            if (entity == null) return false;

            entity.setPosition(placeX + 0.5, placeY, placeZ + 0.5);
            entity.rotationYaw   = 180 + player.rotationYaw;
            entity.rotationPitch = 0.0f;
            world.spawnEntityInWorld(entity);
        }

        clearEntityData(stack);
        stack.stackSize = 0;
        // Client-side slot clearing is handled by PlayerControllerMixin.carryon$clearSlotAfterPlace.

        return true;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, EntityPlayer entity, int itemSlot, boolean isSelected)
    {
        if (hasEntityData(stack))
        {
            if (entity.capabilities.isCreativeMode) return;

            entity.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 1, potionLevel(stack), false));
        }
        else if (isSelected && !world.isRemote)
        {
            stack.stackSize = 0;
        }
    }

    public static boolean hasEntityData(ItemStack stack)
    {
        return stack != null && stack.stackTagCompound != null && stack.stackTagCompound.hasKey(ENTITY_DATA_KEY) && stack.stackTagCompound.hasKey("entity");
    }

    public static boolean storeEntityData(Entity entity, World world, ItemStack stack)
    {
        if (entity == null || stack == null || stack.stackSize == 0) return false;

        String name = EntityList.getEntityString(entity);

        if (name == null || name.isEmpty()) return false;

        NBTTagCompound entityData = new NBTTagCompound();

        entity.writeToNBT(entityData);

        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();

        NBTTagCompound tag = stack.stackTagCompound;

        if (tag.hasKey(ENTITY_DATA_KEY)) return false;

        tag.setCompoundTag(ENTITY_DATA_KEY, entityData);
        tag.setString("entity", name);
        tag.setByte(CarryOnData.NO_DROP_KEY, (byte) 1);

        return true;
    }

    public static void clearEntityData(ItemStack stack)
    {
        if (stack != null && stack.stackTagCompound != null)
        {
            stack.stackTagCompound.removeTag(ENTITY_DATA_KEY);
            stack.stackTagCompound.removeTag("entity");
            stack.stackTagCompound.removeTag(CarryOnData.NO_DROP_KEY);
        }
    }

    public static NBTTagCompound getEntityData(ItemStack stack)
    {
        if (stack != null && stack.stackTagCompound != null && stack.stackTagCompound.hasKey(ENTITY_DATA_KEY))
            return stack.stackTagCompound.getCompoundTag(ENTITY_DATA_KEY);

        return null;
    }

    public static Entity getEntity(ItemStack stack, World world)
    {
        if (world == null || !hasEntityData(stack)) return null;

        String name = getEntityName(stack);

        if (name == null || name.isEmpty()) return null;

        Entity entity = EntityList.createEntityByName(name, world);

        if (entity != null) entity.readFromNBT(getEntityData(stack));

        return entity;
    }

    public static String getEntityName(ItemStack stack)
    {
        if (stack != null && stack.stackTagCompound != null && stack.stackTagCompound.hasKey("entity"))
            return stack.stackTagCompound.getString("entity");
        return null;
    }

    private int potionLevel(ItemStack stack)
    {
        NBTTagCompound data = getEntityData(stack);

        if (data == null) return 1;

        return Math.max(1, Math.min(4, data.toString().length() / 500 + 1));
    }
}