package com.vfxclient.core.mixin;

import com.vfxclient.core.gui.Render2D;
import com.vfxclient.core.gui.VanillaStyle;
import net.minecraft.client.gui.widget.ListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Listas das telas do jogo (Controles, mundos, pacotes) no visual do VfxClient: sem a terra nas
 * faixas de cima e de baixo. As linhas que saem da área visível são cortadas com recorte
 * (scissor) em vez de ficarem escondidas atrás da terra. A terra atrás da lista vira um véu
 * escuro no {@code TextureManagerMixin}.
 */
@Mixin(ListWidget.class)
public abstract class ListWidgetMixin {

	@Shadow
	protected int xStart;
	@Shadow
	protected int xEnd;
	@Shadow
	protected int yStart;
	@Shadow
	protected int yEnd;

	@Inject(method = "renderList", at = @At("HEAD"))
	private void vfx$clipList(int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
		if (VanillaStyle.enabled()) {
			Render2D.scissor(xStart, yStart, xEnd - xStart, yEnd - yStart);
		}
	}

	@Inject(method = "renderList", at = @At("RETURN"))
	private void vfx$endClip(int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
		if (VanillaStyle.enabled()) {
			Render2D.endScissor();
		}
	}

	@Inject(method = "renderHoleBackground", at = @At("HEAD"), cancellable = true)
	private void vfx$noHoles(int top, int bottom, int topAlpha, int bottomAlpha, CallbackInfo ci) {
		if (VanillaStyle.enabled()) {
			ci.cancel();
		}
	}
}
