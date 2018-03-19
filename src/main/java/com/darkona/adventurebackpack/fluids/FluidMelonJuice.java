package com.darkona.adventurebackpack.fluids;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.darkona.adventurebackpack.reference.ModInfo;

public class FluidMelonJuice extends Fluid
{
    public FluidMelonJuice()
    {
        super("melonJuice", new ResourceLocation(ModInfo.MODID,"fluid.mushroomStewStill.png"), new ResourceLocation(ModInfo.MODID, "fluid.mushroomStewFlowing.png"));
    }

    //TODO fluid rendering
//    @Override
//    public IIcon getStillIcon()
//    {
//        return Icons.melonJuiceStill;
//    }
//
//    @Override
//    public IIcon getIcon()
//    {
//        return Icons.melonJuiceStill;
//    }
//
//    @Override
//    public IIcon getFlowingIcon()
//    {
//        return Icons.melonJuiceFlowing;
//    }

    @Override
    public int getColor(FluidStack stack)
    {
        return 0xc31d08;
    }
}
