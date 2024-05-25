package mod.trasiter101.esc.network;

import mod.trasiter101.esc.common.menu.ExtendedSlotCapacitySynchronizer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PacketHelper {

	/**
	 * Register packets that {@link ExtendedSlotCapacitySynchronizer} uses
	 *
	 * @param channel The channel to register the packets to
	 * @param idStart The starting packet id. This is used as provided.
	 *
	 * @return The next, unused id
	 */
	public static int registerPackets(final SimpleChannel channel, int idStart) {
		register(idStart++, channel, ClientboundExtendedSlotInitialDataPacket.class, ClientboundExtendedSlotInitialDataPacket::encode,
				ClientboundExtendedSlotInitialDataPacket::new, ClientboundExtendedSlotInitialDataPacket::handle);
		register(idStart++, channel, ClientboundExtendedSlotSyncPacket.class, ClientboundExtendedSlotSyncPacket::encode,
				ClientboundExtendedSlotSyncPacket::new, ClientboundExtendedSlotSyncPacket::handle);
		return idStart;
	}

	private static <T> void register(final int id, final SimpleChannel channel, final Class<T> clazz, final BiConsumer<T, FriendlyByteBuf> encoder,
			final Function<FriendlyByteBuf, T> decoder, final Consumer<T> handler) {
		register(id, channel, clazz, encoder, decoder, (packet, player) -> handler.accept(packet));
	}

	private static <T> void register(final int id, final SimpleChannel channel, final Class<T> clazz, final BiConsumer<T, FriendlyByteBuf> encoder,
			final Function<FriendlyByteBuf, T> decoder, final BiConsumer<T, ServerPlayer> handler) {
		channel.registerMessage(id, clazz, encoder, decoder, (packet, context) -> {
			context.get().setPacketHandled(true);
			context.get().enqueueWork(() -> handler.accept(packet, context.get().getSender()));
		});
	}
}