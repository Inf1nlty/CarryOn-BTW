package tschipp.carryon;

import btw.block.blocks.*;

import net.minecraft.src.*;

/**
 * Determines which blocks and entities the player is allowed to carry.
 */
public class PickupHandler {

    public static boolean canPlayerPickUpBlock(EntityPlayer player, TileEntity te, World world, int x, int y, int z) {
        int blockId = world.getBlockId(x, y, z);
        Block block = Block.blocksList[blockId];
        if (block == null) return false;
        return isFunctionalBlock(block);
    }

    public static boolean canPlayerPickUpEntity(EntityPlayer player, Entity entity) {

        if (entity instanceof EntityAgeable && ((EntityAgeable) entity).isChild()) return true;
        return false;
    }

    public static boolean isFunctionalBlock(Block block) {

        if (block instanceof BlockChest) return true;
        if (block instanceof BlockEnderChest) return true;
        String className = block.getClass().getSimpleName().toLowerCase();
        if (className.contains("chest") || className.contains("locker")) return true;

        if (block instanceof BasketBlock) return true;

        if (block instanceof OvenBlock) return true;

        if (block instanceof MillstoneBlock) return true;
        if (block instanceof SawBlock) return true;

        if (block instanceof UnfiredBrickBlock) return true;
        if (block instanceof UnfiredClayBlock) return true;
        if (block instanceof UnfiredPotteryBlock) return true;

        if (block instanceof VaseBlock) return true;

        if (block instanceof PlanterBlockBase) return true;

        if (block instanceof HopperBlock) return true;

        if (block instanceof GearBoxBlock) return true;

        if (block instanceof TurntableBlock) return true;

        if (block instanceof BellowsBlock) return true;

        if (block instanceof HibachiBlock) return true;

        if (block instanceof VesselBlock) return true;

        if (block instanceof AnvilBlock) return true;
        if (block instanceof SoulforgeBlock) return true;
        if (block instanceof DormantSoulforgeBlock) return true;

        if (block instanceof DetectorBlock) return true;

        if (block instanceof PulleyBlock) return true;

        if (block instanceof BlockPistonBase) return true;

        if (block instanceof LightBlock) return true;

        if (block instanceof BuddyBlock) return true;

        if (block instanceof LensBlock) return true;

        if (block instanceof ScrewPumpBlock) return true;

        if (block instanceof PistonShovelBlock) return true;

        if (block instanceof BlockDispenser) return true;
        if (block instanceof BlockDispenserBlock) return true;

//        if (block instanceof BlockEnchantmentTable) return true;
//        if (block instanceof InfernalEnchanterBlock) return true;

        if (block instanceof BlockBrewingStand) return true;

        if (block instanceof BlockNote) return true;
        if (block instanceof BlockJukeBox) return true;

        if (block instanceof BlockBeacon) return true;

        return false;
    }
}