package mod.traister101.test;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.network.NetworkHooks;

import org.jetbrains.annotations.Nullable;

public class TestBlock extends Block implements EntityBlock {

	public TestBlock(final Properties pProperties) {
		super(pProperties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(final BlockPos pPos, final BlockState pState) {
		return new TestBE(pPos, pState);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(final BlockState blockState, final Level level, final BlockPos blockPos, final Player player,
			final InteractionHand hand, final BlockHitResult hitResult) {

		if (level.isClientSide) return InteractionResult.SUCCESS;

		level.getBlockEntity(blockPos, TestMod.TEST_BE.get())
				.map(testBE -> new SimpleMenuProvider(
						(pContainerId, pPlayerInventory, pPlayer) -> new TestMenu(pContainerId, pPlayerInventory, testBE), Component.literal("UwU")))
				.ifPresent(simpleMenuProvider -> {
					NetworkHooks.openScreen(((ServerPlayer) player), simpleMenuProvider, friendlyByteBuf -> {
						friendlyByteBuf.writeVarInt(TestBE.CONTAINER_SIZE);
						friendlyByteBuf.writeVarInt(TestBE.SLOT_CAPACITY);
					});
				});

		return InteractionResult.CONSUME;
	}
}