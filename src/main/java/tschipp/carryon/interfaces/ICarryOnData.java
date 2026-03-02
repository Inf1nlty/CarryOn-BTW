package tschipp.carryon.interfaces;

import net.minecraft.src.NBTTagCompound;

public interface ICarryOnData {

    public NBTTagCompound carryOn$getCarryOnData();

    public void carryOn$setCarryOnData(NBTTagCompound tag);

}