package mod.trasiter101.esc.network;

import mod.trasiter101.esc.common.menu.ExtendedSlotCapacityMenu;
import mod.trasiter101.esc.network.utils.ByteBufUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ClientboundExtendedSlotSyncPacket {

	final int windowId;
	final int slotIndex;
	final ItemStack itemStack;

	public ClientboundExtendedSlotSyncPacket(final int windowId, final int slotIndex, final ItemStack itemStack) {
		this.windowId = windowId;
		this.slotIndex = slotIndex;
		this.itemStack = itemStack;
	}

	ClientboundExtendedSlotSyncPacket(final FriendlyByteBuf friendlyByteBuf) {
		windowId = friendlyByteBuf.readInt();
		slotIndex = friendlyByteBuf.readInt();
		itemStack = ByteBufUtils.readExtendedItemStack(friendlyByteBuf);
	}

	void encode(final FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeInt(windowId);
		friendlyByteBuf.writeInt(slotIndex);
		ByteBufUtils.writeExtendedItemStack(friendlyByteBuf, itemStack);
	}

	void handle() {
		final Player player = Minecraft.getInstance().player;
		if (player != null && player.containerMenu instanceof ExtendedSlotCapacityMenu && windowId == player.containerMenu.containerId) {
			player.containerMenu.slots.get(slotIndex).set(itemStack);
		}
	}
}