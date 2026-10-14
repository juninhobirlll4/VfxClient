package com.vfxclient.core.mixin;

import com.vfxclient.core.gui.VanillaStyle;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Caixas de texto das telas do jogo (nome do mundo, endereço do servidor) no visual do
 * VfxClient. O chat não muda: a caixa dele não tem borda.
 */
@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {

	@Shadow
	public int x;
	@Shadow
	public int y;
	@Shadow
	@Final
	private int width;
	@Shadow
	@Final
	private int height;
	@Shadow
	private boolean focused;

	/** A primeira chamada é a borda cinza: vira a nossa caixa inteira. */
	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;fill(IIIII)V", ordinal = 0))
	private void vfx$frame(int left, int top, int right, int bottom, int color) {
		if (VanillaStyle.enabled()) {
			VanillaStyle.drawTextField(x, y, width, height, focused);
		} else {
			DrawableHelper.fill(left, top, right, bottom, color);
		}
	}

	/** A segunda é o fundo preto, que a nossa caixa já desenhou. */
	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;fill(IIIII)V", ordinal = 1))
	private void vfx$background(int left, int top, int right, int bottom, int color) {
		if (!VanillaStyle.enabled()) {
			DrawableHelper.fill(left, top, right, bottom, color);
		}
	}
}
