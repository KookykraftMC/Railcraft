/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.carts;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import mods.railcraft.api.carts.CartTools;
import mods.railcraft.api.core.items.ITrackItem;
import mods.railcraft.api.tracks.ITrackTile;
import mods.railcraft.api.tracks.RailTools;
import mods.railcraft.common.blocks.tracks.TrackSuspended;
import mods.railcraft.common.blocks.tracks.TrackTools;
import mods.railcraft.common.gui.EnumGui;
import mods.railcraft.common.gui.GuiHandler;
import mods.railcraft.common.plugins.forge.LocalizationPlugin;
import mods.railcraft.common.plugins.forge.WorldPlugin;
import mods.railcraft.common.util.inventory.InvTools;
import mods.railcraft.common.util.misc.Game;
import net.minecraftforge.common.util.ForgeDirection;

public class EntityCartTrackRelayer extends CartMaintanceBase {

    private static final int SLOT_STOCK = 0;
    private static final int SLOT_EXIST = 0;
    private static final int SLOT_REPLACE = 1;
    private static final int[] SLOTS = InvTools.buildSlotArray(0, 1);

    public EntityCartTrackRelayer(World world) {
        super(world);
    }

    public EntityCartTrackRelayer(World world, double d, double d1, double d2) {
        this(world);
        setPosition(d, d1 + (double) yOffset, d2);
        motionX = 0.0D;
        motionY = 0.0D;
        motionZ = 0.0D;
        prevPosX = d;
        prevPosY = d1;
        prevPosZ = d2;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (Game.isNotHost(worldObj))
            return;

        stockItems(SLOT_REPLACE, SLOT_STOCK);
        replace();
    }

    private void replace() {
        int i = MathHelper.floor_double(this.posX);
        int j = MathHelper.floor_double(this.posY);
        int k = MathHelper.floor_double(this.posZ);

        if (TrackTools.isRailBlockAt(this.worldObj, i, j - 1, k))
            --j;

        Block block = this.worldObj.getBlock(i, j, k);

        if (TrackTools.isRailBlock(block)) {
            ItemStack trackExist = patternInv.getStackInSlot(SLOT_EXIST);
            ItemStack trackStock = getStackInSlot(SLOT_STOCK);

            boolean nextToSuspended = false;
            for (ForgeDirection side : EnumSet.of(ForgeDirection.EAST, ForgeDirection.WEST, ForgeDirection.NORTH, ForgeDirection.SOUTH)) {
                TileEntity tile = WorldPlugin.getTileEntityOnSide(worldObj, i, j, k, side);
                if (tile instanceof ITrackTile) {
                    ITrackTile track = (ITrackTile) tile;
                    if (track.getTrackInstance() instanceof TrackSuspended) {
                        nextToSuspended = true;
                        break;
                    }
                }
            }
            
            if(nextToSuspended)
                return;

            if (trackExist != null && trackStock != null)
                if (trackExist.getItem() instanceof ITrackItem) {
                    ITrackItem trackItem = (ITrackItem) trackExist.getItem();
                    if (trackItem.getPlacedBlock() == block) {
                        TileEntity tile = worldObj.getTileEntity(i, j, k);
                        if (trackItem.isPlacedTileEntity(trackExist, tile)) {
                            int meta = removeOldTrack(i, j, k, block);
                            if (meta != -1)
                                placeNewTrack(i, j, k, SLOT_STOCK, meta);
                        }
                    }
                } else if (InvTools.isStackEqualToBlock(trackExist, block)) {
                    int meta = removeOldTrack(i, j, k, block);
                    if (meta != -1)
                        placeNewTrack(i, j, k, SLOT_STOCK, meta);
                }
        }
    }

    private int removeOldTrack(int x, int y, int z, Block block) {
        List<ItemStack> drops = block.getDrops(worldObj, x, y, z, 0, 0);

        for (ItemStack stack : drops) {
            CartTools.offerOrDropItem(this, stack);
        }
        int meta = worldObj.getBlockMetadata(x, y, z);
        if (((BlockRailBase) block).isPowered())
            meta = meta & 7;
        worldObj.setBlockToAir(x, y, z);
        return meta;
    }

    private void placeNewTrack(int x, int y, int z, int slotStock, int meta) {
        ItemStack trackStock = getStackInSlot(slotStock);
        if (trackStock != null)
            if (RailTools.placeRailAt(trackStock, worldObj, x, y, z)) {
                worldObj.setBlockMetadataWithNotify(x, y, z, meta, 0x02);
                Block block = worldObj.getBlock(x, y, z);
                block.onNeighborBlockChange(worldObj, x, y, z, block);
                worldObj.markBlockForUpdate(x, y, z);
                decrStackSize(slotStock, 1);
                blink();
            }
    }

    @Override
    public boolean doInteract(EntityPlayer player) {
        if (Game.isHost(worldObj))
            GuiHandler.openGui(EnumGui.CART_TRACK_RELAYER, player, worldObj, this);
        return true;
    }

    @Override
    public String getInventoryName() {
        return LocalizationPlugin.translate(EnumCart.TRACK_RELAYER.getTag());
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return SLOTS;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        ItemStack trackReplace = patternInv.getStackInSlot(SLOT_REPLACE);
        return InvTools.isItemEqual(stack, trackReplace);
    }

}
