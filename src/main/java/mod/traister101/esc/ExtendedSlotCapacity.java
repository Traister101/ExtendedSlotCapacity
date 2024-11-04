package mod.traister101.esc;

import mod.traister101.esc.common.menu.ExtendedSlotCapacitySynchronizer;
import mod.traister101.esc.network.ESCPacketHandler;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod(ExtendedSlotCapacity.MODID)
public class ExtendedSlotCapacity {

	public static final String MODID = "esc";

	public ExtendedSlotCapacity() {
		ESCPacketHandler.init();

		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, ExtendedSlotCapacitySynchronizer::setExtendedSlotCapacitySynchronizer);
	}
}