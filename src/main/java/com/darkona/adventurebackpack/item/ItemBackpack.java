package com.darkona.adventurebackpack.item;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.darkona.adventurebackpack.block.BlockBackpack;
import com.darkona.adventurebackpack.block.TileBackpack;
import com.darkona.adventurebackpack.common.BackpackAbilities;
import com.darkona.adventurebackpack.common.Constants;
import com.darkona.adventurebackpack.config.ConfigHandler;
import com.darkona.adventurebackpack.events.WearableEvent;
import com.darkona.adventurebackpack.init.ModNetwork;
import com.darkona.adventurebackpack.network.GuiPacket;
import com.darkona.adventurebackpack.playerProperties.BackpackProperty;
import com.darkona.adventurebackpack.proxy.ClientProxy;
import com.darkona.adventurebackpack.reference.BackpackTypes;
import com.darkona.adventurebackpack.reference.ModInfo;
import com.darkona.adventurebackpack.util.BackpackUtils;
import com.darkona.adventurebackpack.util.CoordsUtils;
import com.darkona.adventurebackpack.util.EnchUtils;
import com.darkona.adventurebackpack.util.Resources;
import com.darkona.adventurebackpack.util.TipUtils;
import com.darkona.adventurebackpack.util.Utils;

import static com.darkona.adventurebackpack.common.Constants.BASIC_TANK_CAPACITY;
import static com.darkona.adventurebackpack.common.Constants.TAG_DISABLE_CYCLING;
import static com.darkona.adventurebackpack.common.Constants.TAG_DISABLE_NVISION;
import static com.darkona.adventurebackpack.common.Constants.TAG_INVENTORY;
import static com.darkona.adventurebackpack.common.Constants.TAG_LEFT_TANK;
import static com.darkona.adventurebackpack.common.Constants.TAG_RIGHT_TANK;
import static com.darkona.adventurebackpack.common.Constants.TAG_TYPE;
import static com.darkona.adventurebackpack.util.TipUtils.l10n;

/**
 * Created on 12/10/2014
 *
 * @author Darkona
 */
public class ItemBackpack extends ItemAdventure
{
    public ItemBackpack()
    {
        super();
        setUnlocalizedName("adventureBackpack");
        this.setRegistryName(ModInfo.MODID, "adventure_backpack");
    }

    @Override
    @SuppressWarnings({"unchecked"})
    @SideOnly(Side.CLIENT)
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        for (BackpackTypes type : BackpackTypes.values())
        {
            if (type == BackpackTypes.UNKNOWN)
                continue;

            items.add(BackpackUtils.createBackpackStack(type));
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        NBTTagCompound backpackTag = BackpackUtils.getWearableCompound(stack);

        BackpackTypes type = BackpackTypes.getType(backpackTag.getByte(TAG_TYPE));
        tooltip.add(Utils.getColoredSkinName(type));

        FluidTank tank = new FluidTank(BASIC_TANK_CAPACITY);

        if (GuiScreen.isShiftKeyDown())
        {
            NBTTagList itemList = backpackTag.getTagList(TAG_INVENTORY, NBT.TAG_COMPOUND);
            tooltip.add(l10n("backpack.slots.used") + ": " + TipUtils.inventoryTooltip(itemList));

            tank.readFromNBT(backpackTag.getCompoundTag(TAG_LEFT_TANK));
            tooltip.add(l10n("backpack.tank.left") + ": " + TipUtils.tankTooltip(tank));

            tank.readFromNBT(backpackTag.getCompoundTag(TAG_RIGHT_TANK));
            tooltip.add(l10n("backpack.tank.right") + ": " + TipUtils.tankTooltip(tank));

            TipUtils.shiftFooter(tooltip);
        }
        else if (!GuiScreen.isCtrlKeyDown())
        {
            tooltip.add(TipUtils.holdShift());
        }

        if (GuiScreen.isCtrlKeyDown())
        {
            {
                boolean cycling = !backpackTag.getBoolean(TAG_DISABLE_CYCLING);
                tooltip.add(l10n("backpack.cycling") + ": " + TipUtils.switchTooltip(cycling, true));
                tooltip.add(TipUtils.pressKeyFormat(TipUtils.actionKeyFormat()) + l10n("backpack.cycling.key1"));
                tooltip.add(l10n("backpack.cycling.key2") + " " + TipUtils.switchTooltip(!cycling, false));
            }

            if (BackpackTypes.isNightVision(type))
            {
                boolean vision = !backpackTag.getBoolean(TAG_DISABLE_NVISION);
                tooltip.add(l10n("backpack.vision") + ": " + TipUtils.switchTooltip(vision, true));
                tooltip.add(TipUtils.pressShiftKeyFormat(TipUtils.actionKeyFormat()) + l10n("backpack.vision.key1"));
                tooltip.add(l10n("backpack.vision.key2") + " " + TipUtils.switchTooltip(!vision, false));
            }
        }
    }

    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer player)
    {
        super.onCreated(stack, world, player);
        BackpackUtils.setBackpackType(stack, BackpackTypes.getType(stack.getItemDamage()));
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        return player.canPlayerEdit(pos, side, player.getHeldItem(hand))
                && placeBackpack(player.getHeldItem(hand), player, world, pos, side, true)
                ? EnumActionResult.SUCCESS : EnumActionResult.FAIL;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
    {
        RayTraceResult trace = rayTrace(world, player, true);
        if (trace == null || trace.typeOfHit == RayTraceResult.Type.ENTITY)
        {
            if (world.isRemote)
            {
                ModNetwork.INSTANCE.sendToServer(new GuiPacket.GuiMessage(GuiPacket.GUI_BACKPACK, GuiPacket.FROM_HOLDING));
            }
        }
        return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
    }

    @Override
    public void onPlayerDeath(World world, EntityPlayer player, ItemStack stack)
    {
        if (world.isRemote || !ConfigHandler.backpackDeathPlace || EnchUtils.isSoulBounded(stack)
                || player.getEntityWorld().getGameRules().getBoolean("keepInventory"))
        {
            return;
        }

        if (!tryPlace(world, player, stack))
        {
            player.dropItem(stack, false);
        }

        BackpackProperty.get(player).setWearable(null);
    }

    private boolean tryPlace(World world, EntityPlayer player, ItemStack backpack) //TODO extract behavior to CoordsUtils
    {
        int x = MathHelper.floor(player.posX);
        int z = MathHelper.floor(player.posZ);
        int Y = MathHelper.floor(player.posY);
        if (Y < 1) Y = 1;

        int positions[] = {0, -1, 1, -2, 2, -3, 3, -4, 4, -5, 5, -6, 6};

        for (int shiftY : positions)
        {
            if (Y + shiftY >= 1)
            {
                BlockPos spawn = CoordsUtils.getNearestEmptyChunkCoordinatesSpiral(world, x, z, x, Y + shiftY, z, 6, true, 1, (byte) 0, false);
                if (spawn != null)
                {
                    return placeBackpack(backpack, player, world, spawn, EnumFacing.UP, false);
                }
            }
        }
        return false;
    }

    private boolean placeBackpack(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, boolean from)
    {
        if (stack.isEmpty()
                || !player.canPlayerEdit(pos, side, stack)
                || (pos.getY() <= 0 || pos.getY() >= world.getHeight()))
            return false;

        //BlockBackpack backpack = ModBlocks.BLOCK_BACKPACK; //TODO correctly register blocks
        BlockBackpack backpack = new BlockBackpack();

        //pos = pos.up(); //from now on, we are working with block ABOVE one player click

        if (backpack.canPlaceBlockOnSide(world, pos, side) && world.getBlockState(pos).getMaterial().isSolid())
        {
            pos = pos.offset(side);

            if (pos.getY() <= 0 || pos.getY() >= world.getHeight())
                return false;

            if (backpack.canPlaceBlockAt(world, pos))
            {
                if (world.setBlockState(pos, backpack.getDefaultState()))
                //if (world.setBlockState(pos, ModBlocks.BLOCK_BACKPACK.getDefaultState()))
                {
                    backpack.onBlockPlacedBy(world, pos, backpack.getDefaultState(), player, stack);
                    //backpack.onBlockPlacedBy(world, pos, ModBlocks.BLOCK_BACKPACK.getDefaultState(), player, stack);
                    player.playSound(SoundEvents.BLOCK_CLOTH_PLACE, 0.5f, 1.0f);
                    ((TileBackpack) world.getTileEntity(pos)).loadFromNBT(stack.getTagCompound());
                    if (from)
                    {
                        stack.setCount(0);
                    }
                    else
                    {
                        BackpackProperty.get(player).setWearable(null);
                    }
                    WearableEvent event = new WearableEvent(player, stack);
                    MinecraftForge.EVENT_BUS.post(event);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onEquipped(World world, EntityPlayer player, ItemStack stack)
    {

    }

    @Override
    public void onEquippedUpdate(World world, EntityPlayer player, ItemStack stack)
    {
        if (!ConfigHandler.backpackAbilities || world == null || player == null || stack == null)
            return;

        if (BackpackTypes.isSpecial(BackpackTypes.getType(stack)))
        {
            BackpackAbilities.backpackAbilities.executeAbility(player, world, stack);
        }
    }

    @Override
    public void onUnequipped(World world, EntityPlayer player, ItemStack stack)
    {
        if (BackpackTypes.hasProperty(BackpackTypes.getType(stack), BackpackTypes.Props.REMOVAL))
        {
            BackpackAbilities.backpackAbilities.executeRemoval(player, world, stack);
        }
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack)
    {
        return (double) getItemCount(stack) / Constants.INVENTORY_MAIN_SIZE;
    }

    private int getItemCount(ItemStack backpack)
    {
        NBTTagList itemList = BackpackUtils.getWearableInventory(backpack);
        int itemCount = itemList.tagCount();
        for (int i = itemCount - 1; i >= 0; i--)
        {
            int slotAtI = itemList.getCompoundTagAt(i).getInteger(Constants.TAG_SLOT);
            if (slotAtI < Constants.INVENTORY_MAIN_SIZE)
                break;
            itemCount--;
        }
        return itemCount;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack)
    {
        return ConfigHandler.enableFullnessBar && getItemCount(stack) > 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModelBiped getWearableModel(ItemStack wearable)
    {
        return ClientProxy.modelAdventureBackpack.setWearable(wearable);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ResourceLocation getWearableTexture(ItemStack wearable)
    {
        return Resources.getBackpackTexture(BackpackTypes.getType(wearable));
    }
}