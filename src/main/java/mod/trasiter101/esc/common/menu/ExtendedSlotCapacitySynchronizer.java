package mod.trasiter101.esc.common.menu;

import mod.trasiter101.esc.network.ClientboundExtendedSlotInitialDataPacket;
import mod.trasiter101.esc.network.ClientboundExtendedSlotSyncPacket;
import mod.trasiter101.esc.network.PacketHelper;

import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;

/**
 * A {@link ContainerSynchronizer} for containers with extended slot capacity.
 */
public class ExtendedSlotCapacitySynchronizer implements ContainerSynchronizer {

	private final ServerPlayer player;
	private final BiConsumer<PacketTarget, Object> packetConsumer;

	/**
	 * @param player The player this synchronizer is for
	 * @param channel A {@link SimpleChannel} that accepts {@link ClientboundExtendedSlotInitialDataPacket} and
	 * {@link ClientboundExtendedSlotSyncPacket}
	 *
	 * @see PacketHelper#registerPackets(SimpleChannel, int)
	 */
	@SuppressWarnings("unused")
	public ExtendedSlotCapacitySynchronizer(final ServerPlayer player, final SimpleChannel channel) {
		this(player, channel::send);
	}

	/**
	 * @param player The player this synchronizer is for
	 * @param packetConsumer A consumer that accepts {@link ClientboundExtendedSlotInitialDataPacket} and {@link ClientboundExtendedSlotSyncPacket}.
	 *
	 * @see PacketHelper#registerPackets(SimpleChannel, int)
	 */
	public ExtendedSlotCapacitySynchronizer(final ServerPlayer player, final BiConsumer<PacketTarget, Object> packetConsumer) {
		this.player = player;
		this.packetConsumer = packetConsumer;
	}

	@Override
	public void sendInitialData(final AbstractContainerMenu containerMenu, final NonNullList<ItemStack> itemStacks, final ItemStack carriedStack,
			final int[] dataSlotsValues) {
		packetConsumer.accept(PacketDistributor.PLAYER.with(() -> player),
				new ClientboundExtendedSlotInitialDataPacket(containerMenu.incrementStateId(), containerMenu.containerId, itemStacks, carriedStack));

		for (int index = 0; index < dataSlotsValues.length; ++index) {
			broadcastDataValue(containerMenu, index, dataSlotsValues[index]);
		}
	}

	@Override
	public void sendSlotChange(final AbstractContainerMenu containerMenu, final int slotIndex, final ItemStack itemStack) {
		packetConsumer.accept(PacketDistributor.PLAYER.with(() -> player),
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