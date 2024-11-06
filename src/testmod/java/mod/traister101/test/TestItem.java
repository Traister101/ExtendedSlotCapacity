package mod.traister101.test;

import mod.traister101.esc.common.capability.ExtendedSlotCapacityHandler;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkHooks;

import org.jetbrains.annotations.*;

public class TestItem extends Item {

	public static final int SLOT_COUNT = 27;
	public static final int SLOT_STACK_LIMIT = 128;

	public TestItem(final Properties pProperties) {
		super(pProperties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
		final ItemStack heldStack = player.getItemInHand(hand);

		if (level.isClientSide) return InteractionResultHolder.consume(heldStack);

		NetworkHooks.openScreen(((ServerPlayer) player), new SimpleMenuProvider(
						(pContainerId, pPlayerInventory, pPlayer) -> new TestMenu(pContainerId, pPlayerInventory,
								heldStack.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElseThrow()), heldStack.getHoverName()),
				friendlyByteBuf -> {
					friendlyByteBuf.writeVarInt(SLOT_COUNT);
					friendlyByteBuf.writeVarInt(SLOT_STACK_LIMIT);
				});


		return InteractionResultHolder.success(heldStack);
	}

	@Override
	public @Nullable ICapabilityProvider initCapabilities(final ItemStack stack, @Nullable final CompoundTag nbt) {
		return new ICapabilitySerializable<CompoundTag>() {

			private final LazyOptional<ExtendedSlotCapacityHandler> handler = LazyOptional.of(
					() -> new ExtendedSlotCapacityHandler(SLOT_COUNT, SLOT_STACK_LIMIT));

			@Override
			public CompoundTag serializeNBT() {
				return handler.map(ExtendedSlotCapacityHandler::serializeNBT).orElseGet(CompoundTag::new);
			}

			@Override
			public void deserializeNBT(final CompoundTag nbt) {
				handler.ifPresent(extendedSlotCapacityHandler -> extendedSlotCapacityHandler.deserializeNBT(nbt));
			}

			@Override
			public @NotNull <T> LazyOptional<T> getCapability(@NotNull final Capability<T> cap, @Nullable final Direction side) {
				return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, handler.cast());
			}
		};
	}
}