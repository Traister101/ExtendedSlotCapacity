package mod.traister101.test;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TestMenuScreen extends AbstractContainerScreen<TestMenu> {

	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");

	public TestMenuScreen(final TestMenu pMenu, final Inventory pPlayerInventory, final Component pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
	}

	@Override
	public void render(final GuiGraphics pGuiGraphics, final int pMouseX, final int pMouseY, final float pPartialTick) {
		renderBackground(pGuiGraphics);
		super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
		renderTooltip(pGuiGraphics, pMouseX, pMouseY);
	}

	@Override
	protected void renderBg(final GuiGraphics pGuiGraphics, final float pPartialTick, final int pMouseX, final int pMouseY) {
		final var x = (width - imageWidth) / 2;
		final var y = (height - imageHeight) / 2;
		pGuiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, 71);
		pGuiGraphics.blit(TEXTURE, x, y + 71, 0, 126, imageWidth, 96);
	}
}