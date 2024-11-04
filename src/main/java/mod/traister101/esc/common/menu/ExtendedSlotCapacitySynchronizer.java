package mod.traister101.esc.common.menu;

import mod.traister101.esc.network.*;

import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * A {@link ContainerSynchronizer} for containers with extended slot capacity.
 */
public class ExtendedSlotCapacitySynchronizer implements ContainerSynchronizer {

	private final ServerPlayer player;

	public ExtendedSlotCapacitySynchronizer(final ServerPlayer player) {
		this.player = player;
	}

	/**
	 * How {@link ExtendedSlotCapacityMenu}s have their {@link ContainerSynchronizer} set to {@link ExtendedSlotCapacitySynchronizer}.
	 */
	public static void setExtendedSlotCapacitySynchronizer(final PlayerContainerEvent.Open event) {
		final var containerMenu = event.getContainer();
		if (!(containerMenu instanceof ExtendedSlotCapacityMenu)) return;

		containerMenu.setSynchronizer(new ExtendedSlotCapacitySynchronizer((ServerPlayer) event.getEntity()));
	}

	@Override
	public void sendInitialData(final AbstractContainerMenu containerMenu, final NonNullList<ItemStack> itemStacks, final ItemStack carriedStack,
			final int[] dataSlotsValues) {
		ESCPacketHandler.send(PacketDistributor.PLAYER.with(() -> player),
				new ClientboundExtendedSlotInitialDataPacket(containerMenu.incrementStateId(), containerMenu.containerId, itemStacks, carriedStack));

		for (int index = 0; index < dataSlotsValues.length; ++index) {
			broadcastDataValue(containerMenu, index, dataSlotsValues[index]);
		}
	}

	@Override
	public void sendSlotChange(final AbstractContainerMenu containerMenu, final int slotIndex, final ItemStack itemStack) {
		ESCPacketHandler.send(PacketDistributor.PLAYER.with(() -> player),
				new ClientboundExtendedSlotSyncPacket(containerMenu.containerId, slotIndex, itemStack));
	}

	@Override
	public void sendCarriedChange(final AbstractContainerMenu containerMenu, final ItemStack itemStack) {
		player.connection.send(new ClientboundContainerSetSlotPacket(-1, containerMenu.incrementStateId(), -1, itemStack));
	}

	@Override
	public void sendDataChange(final AbstractContainerMenu containerMenu, final int index, final int value) {
		broadcastDataValue(containerMenu, index, value);
	}

	private void broadcastDataValue(final AbstractContainerMenu containerMenu, final int index, final int value) {
		player.connection.send(new ClientboundContainerSetDataPacket(containerMenu.containerId, index, value));
	}
}