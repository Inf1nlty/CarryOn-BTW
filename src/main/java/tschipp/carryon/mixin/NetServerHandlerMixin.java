package tschipp.carryon.mixin;

import net.minecraft.src.*;

import tschipp.carryon.item.ItemEntity;
import tschipp.carryon.item.ItemTile;
import tschipp.carryon.CarryOnItems;
import tschipp.carryon.CarryOnHelper;
import tschipp.carryon.PickupHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(NetServerHandler.class)
public class NetServerHandlerMixin {

    @Shadow public EntityPlayerMP playerEntity;

    @Unique private static final Map<Integer, Long> pickupCooldown = new HashMap<>();

    @Unique private static final long COOLDOWN_MS = 500L;

    @Inject(method = "handleWindowClick", at = @At("HEAD"), cancellable = true)
    private void carryon$lockSlotOnWindowClick(Packet102WindowClick packet, CallbackInfo ci)
    {
        EntityPlayerMP player = this.playerEntity;
        if (player == null) return;

        if (!CarryOnHelper.isCarryStack(player.inventory.getCurrentItem())) return;

        ci.cancel();
    }

    @Inject(method = "handleBlockItemSwitch", at = @At("HEAD"), cancellable = true)
    private void carryon$lockSlotOnHotbarSwitch(Packet16BlockItemSwitch packet, CallbackInfo ci)
    {
        EntityPlayerMP player = this.playerEntity;

        if (player == null) return;

        if (!CarryOnHelper.isCarryStack(player.inventory.getCurrentItem())) return;
        ci.cancel();
    }

    /**
     * Intercepts Packet15Place (right-click with item / block activation).
     * When carrying: we take over entirely.
     *   - activateBlockOrUseItem is bypassed because in creative mode it restores
     *     stackSize after tryPlaceItemIntoWorld, preventing the slot from being cleared.
     *   - We call onItemUse / onItemRightClick directly, then clear the slot ourselves
     *     and send Packet103SetSlot(null) to the client unconditionally.
     * When empty-handed and sneaking: handle block pickup.
     */
    @Inject(method = "handlePlace", at = @At("HEAD"), cancellable = true)
    private void carryon$handlePlace(Packet15Place packet, CallbackInfo ci)
    {
        EntityPlayerMP player = this.playerEntity;
        if (player == null) return;

        World world = player.worldObj;
        if (world == null) return;

        ItemStack held = player.inventory.getCurrentItem();

        // ── Carrying: bypass activateBlockOrUseItem entirely ──────────────────
        if (CarryOnHelper.isCarryStack(held))
        {
            ci.cancel();   // We handle everything ourselves.

            int dir = packet.getDirection();
            boolean placed;

            if (dir == 255)
            {
                // Air right-click → onItemRightClick path
                held.getItem().onItemRightClick(held, world, player);
                // onItemRightClick returns the same stack object; placement success is
                // signalled by stackSize having been set to 0 inside doPlace.
                placed = (held.stackSize == 0);
            }
            else
            {
                // Block-face right-click → onItemUse path (avoids creative stackSize restore)
                placed = held.getItem().onItemUse(
                        held, player, world,
                        packet.getXPosition(), packet.getYPosition(), packet.getZPosition(),
                        dir,
                        packet.getXOffset(), packet.getYOffset(), packet.getZOffset());
            }

            if (placed)
            {
                // Clear the slot on the server.
                int slot = player.inventory.currentItem;
                player.inventory.mainInventory[slot] = null;

                // Always send Packet103SetSlot(null) so the client clears the slot
                // regardless of creative mode or any other stackSize restoration.
                Slot containerSlot = player.openContainer.getSlotFromInventory(player.inventory, slot);
                player.playerNetServerHandler.sendPacketToPlayer(
                        new Packet103SetSlot(player.openContainer.windowId,
                                             containerSlot.slotNumber,
                                             null));
            }
            else
            {
                // Placement failed — resync both blocks so client doesn't show phantom block.
                player.playerNetServerHandler.sendPacketToPlayer(
                        new Packet53BlockChange(packet.getXPosition(), packet.getYPosition(), packet.getZPosition(), world));
            }

            return;
        }

        // ── Pickup logic (sneaking, empty hand) ───────────────────────────────
        if (!player.isSneaking() || player.inventory.getCurrentItem() != null) return;

        if (packet.getDirection() == 255) return;

        final int carrySlot = player.inventory.currentItem;

        int x = packet.getXPosition();
        int y = packet.getYPosition();
        int z = packet.getZPosition();

        Block block = Block.blocksList[world.getBlockId(x, y, z)];

        if (block == null || !PickupHandler.isFunctionalBlock(block)) return;

        if (block.getBlockHardness(world, x, y, z) < 0) return;

        // Anti-fly checks
        {
            int footY = MathHelper.ceiling_double_int(player.posY);
            if (y < footY - 1) return;

            int underX = MathHelper.floor_double(player.posX);
            int underZ = MathHelper.floor_double(player.posZ);
            // Check a 3x3 area under the player's feet — prevents jumping off the
            // edge of a functional block and picking it up mid-air.
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dz = -1; dz <= 1; dz++)
                {
                    Block underBlock = Block.blocksList[world.getBlockId(underX + dx, footY - 1, underZ + dz)];
                    if (underBlock != null && PickupHandler.isFunctionalBlock(underBlock)) return;
                }
            }
        }

        if (ItemTile.isLocked(x, y, z, world)) return;

        if (!checkCooldown(player.entityId)) {
            ci.cancel(); return;
        }

        ItemStack stack = new ItemStack(CarryOnItems.TILE_ITEM);

        if (ItemTile.storeTileData(world.getBlockTileEntity(x, y, z), world, x, y, z, stack))
        {
            world.removeBlockTileEntity(x, y, z);
            world.setBlockToAir(x, y, z);
            forceCarrySlot(player, carrySlot, stack);
            ci.cancel();
        }
    }

    /**
     * Intercepts Packet7UseEntity (right-click on entity).
     * When empty-handed and sneaking, try to pick up the targeted entity.
     */
    @Inject(method = "handleUseEntity", at = @At("HEAD"), cancellable = true)
    private void carryon$handleUseEntity(Packet7UseEntity packet, CallbackInfo ci)
    {
        EntityPlayerMP player = this.playerEntity;
        if (player == null) return;

        World world = player.worldObj;
        if (world == null) return;

        // While carrying, cancel entity right-click interaction (to avoid feeding / trading while holding something).
        ItemStack held = player.inventory.getCurrentItem();
        if (CarryOnHelper.isCarryStack(held))
        {
            ci.cancel();
            return;
        }

        // Only intercept right-clicks (isLeftClick == 0)
        if (packet.isLeftClick != 0) return;

        // ---- pickup logic (sneaking, empty hand) ----
        if (!player.isSneaking() || player.inventory.getCurrentItem() != null) return;

        final int carrySlot = player.inventory.currentItem;

        Entity entity = world.getEntityByID(packet.targetEntity);

        if (entity != null && !entity.isDead && PickupHandler.canPlayerPickUpEntity(player, entity)
                && checkCooldown(player.entityId))
        {
            ItemStack stack = new ItemStack(CarryOnItems.ENTITY_ITEM);

            if (ItemEntity.storeEntityData(entity, world, stack))
            {
                entity.setDead();
                forceCarrySlot(player, carrySlot, stack);
                ci.cancel();
            }
        }
    }

    @Unique
    private static void forceCarrySlot(EntityPlayerMP player, int slot, ItemStack stack)
    {
        player.inventory.mainInventory[slot] = stack;
        player.inventory.currentItem = slot;
        // Tell the client to select this slot.
        player.playerNetServerHandler.sendPacketToPlayer(new Packet16BlockItemSwitch(slot));
        // Sync the full inventory so the client sees the item in the correct slot.
        player.sendContainerToPlayer(player.inventoryContainer);
    }

    /** Returns true and records timestamp if outside cooldown window; false if still cooling down. */
    @Unique
    private static boolean checkCooldown(int playerId)
    {
        long now = System.currentTimeMillis();
        Long last = pickupCooldown.get(playerId);

        if (last != null && now - last < COOLDOWN_MS)
            return false;

        pickupCooldown.put(playerId, now);
        return true;
    }
}