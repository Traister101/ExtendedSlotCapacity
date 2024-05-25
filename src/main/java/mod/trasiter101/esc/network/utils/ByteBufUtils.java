package mod.trasiter101.esc.network.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Vanilla {@link ItemStack} networking writes the stack count to a byte. As we can't extend {@link FriendlyByteBuf}
 * to change this and a mixin to {@link FriendlyByteBuf#writeItemStack(ItemStack, boolean)} and {@link FriendlyByteBuf#readItem()} is
 * a bad idea we provide {@link #writeExtendedItemStack(FriendlyByteBuf, ItemStack)} and {@link #readExtendedItemStack(FriendlyByteBuf)}
 */
public final class ByteBufUtils {

	private ByteBufUtils() {
		throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	/**
	 * Writes the full stack count to the buffer.
	 *
	 * @param friendlyByteBuf The buffer to write to
	 * @param itemStack The {@link ItemStack} to write to the buffer
	 *
	 * @apiNote For stack sizes within vanilla networking bounds [0, {@value Byte#MAX_VALUE}] this has no added network overhead
	 * @see #readExtendedItemStack(FriendlyByteBuf)
	 */
	public static void writeExtendedItemStack(final FriendlyByteBuf friendlyByteBuf, final ItemStack itemStack) {
		if (itemStack.isEmpty()) {
			friendlyByteBuf.writeByte(0);
			return;
		}

		friendlyByteBuf.writeVarInt(itemStack.getCount());
		friendlyByteBuf.writeInt(Item.getId(itemStack.getItem()));

		final CompoundTag itemStackTag;
		if (itemStack.getItem().isDamageable(itemStack) || itemStack.getItem().shouldOverrideMultiplayerNbt()) {
			itemStackTag = itemStack.getShareTag();
		} else itemStackTag = null;

		friendlyByteBuf.writeNbt(itemStackTag);
	}

	/**
	 * Reads a full stack count from the buffer
	 *
	 * @param friendlyByteBuf The buffer to read from
	 *
	 * @return The {@link ItemStack} which was written to the buffer
	 *
	 * @see #writeExtendedItemStack(FriendlyByteBuf, ItemStack)
	 */
	public static ItemStack readExtendedItemStack(final FriendlyByteBuf friendlyByteBuf) {
		final int count = friendlyByteBuf.readVarInt();

		if (count == 0) {
			return ItemStack.EMPTY;
		}

		final ItemStack itemStack = new ItemStack(Item.byId(friendlyByteBuf.readInt()), count);

		itemStack.readShareTag(friendlyByteBuf.readNbt());
		return itemStack;
	}
}