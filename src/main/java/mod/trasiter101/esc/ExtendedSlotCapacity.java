package mod.trasiter101.esc;

import mod.trasiter101.esc.common.menu.ExtendedSlotCapacitySynchronizer;
import mod.trasiter101.esc.network.ESCPacketHandler;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(ExtendedSlotCapacity.MODID)
public class ExtendedSlotCapacity {

	public static final String MODID = "esc";

	public ExtendedSlotCapacity() {
		ESCPacketHandler.init();

		MinecraftForge.EVENT_BUS.addListener(ExtendedSlotCapacitySynchronizer::setExtendedSlotCapacitySynchronizer);
	}
}