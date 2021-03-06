/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2017
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.common.blocks.tracks.behaivor;

import mods.railcraft.common.blocks.charge.ChargeManager;
import mods.railcraft.common.blocks.charge.ChargeNetwork;
import mods.railcraft.common.items.ModItems;
import mods.railcraft.common.items.RailcraftItems;
import mods.railcraft.common.plugins.misc.Mod;
import mods.railcraft.common.util.effects.EffectManager;
import mods.railcraft.common.util.inventory.InvTools;
import mods.railcraft.common.util.misc.Game;
import mods.railcraft.common.util.misc.MiscTools;
import mods.railcraft.common.util.misc.RailcraftDamageSource;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Created by CovertJaguar on 8/7/2016 for Railcraft.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public enum CollisionHandler {
    NULL,
    ELECTRIC {
        @Override
        public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
            if (Game.isClient(world))
                return;

            if (!MiscTools.isKillableEntity(entity))
                return;

            ChargeNetwork.ChargeNode node = ChargeManager.getNetwork(world).getNode(pos);
            if (node.getChargeGraph().getCharge() > 2000) {
                boolean shock = true;
                ItemStack overalls = getOveralls(entity);
                ItemStack boots = getRubberBoots(entity);
                if (!InvTools.isEmpty(overalls) && !InvTools.isEmpty(boots)) {
                    shock = false;
                    if (MiscTools.RANDOM.nextInt(300) == 0)
                        entity.setItemStackToSlot(EntityEquipmentSlot.LEGS, InvTools.damageItem(overalls, 1));
                    else if (MiscTools.RANDOM.nextInt(300) == 150)
                        entity.setItemStackToSlot(EntityEquipmentSlot.FEET, InvTools.damageItem(boots, 1));
                }
                if (!InvTools.isEmpty(overalls)) {
                    shock = false;
                    if (MiscTools.RANDOM.nextInt(150) == 0)
                        entity.setItemStackToSlot(EntityEquipmentSlot.LEGS, InvTools.damageItem(overalls, 1));
                }
                if (!InvTools.isEmpty(boots)) {
                    shock = false;
                    if (MiscTools.RANDOM.nextInt(150) == 0)
                        entity.setItemStackToSlot(EntityEquipmentSlot.FEET, InvTools.damageItem(boots, 1));
                }
                if (shock && entity.attackEntityFrom(RailcraftDamageSource.TRACK_ELECTRIC, 2)) {
                    node.removeCharge(2000);
                    EffectManager.instance.zapEffectDeath(world, entity);
                }
            }
        }

        @Nullable
        private ItemStack getOveralls(Entity entity) {
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = ((EntityPlayer) entity);
                ItemStack pants = player.getItemStackFromSlot(EntityEquipmentSlot.LEGS);
                if (!InvTools.isEmpty(pants) && RailcraftItems.OVERALLS.isInstance(pants) && !((EntityPlayer) entity).capabilities.isCreativeMode)
                    return pants;
            }
            return InvTools.emptyStack();
        }
        @Nullable
        private ItemStack getRubberBoots(Entity entity) {
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = ((EntityPlayer) entity);
                ItemStack feet = player.getItemStackFromSlot(EntityEquipmentSlot.FEET);
                if (!InvTools.isEmpty(feet) && (ModItems.RUBBER_BOOTS.isEqual(feet, false, false) || ModItems.STATIC_BOOTS.isEqual(feet, false, false)) && !((EntityPlayer) entity).capabilities.isCreativeMode)
                    return feet;
            }
            return InvTools.emptyStack();
        }
    };

    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
    }
}
