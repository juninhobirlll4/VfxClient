package com.vfxclient.core.mixin;

import com.vfxclient.core.gui.VanillaStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Botões das telas do jogo no visual do VfxClient (ver {@link VanillaStyle}). */
@Mixin(ButtonWidget.class)
public abstract class ButtonWidgetMixin {

	@Shadow
	public int x;
	@Shadow
	public int y;
	@Shadow
	protected int width;
	@Shadow
	protected int height;
	@Shadow
	public boolean visible;
	@Shadow
	protected boolean hovered;

	@Shadow
	protected abstract void mouseDragged(MinecraftClient client, int mouseX, int mouseY);

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void vfx$render(MinecraftClient client, int mouseX, int mouseY, CallbackInfo ci) {
		if (!VanillaStyle.enabled()) {
			return;
		}
		ci.cancel();
		if (!visible) {
			return;
		}

		ButtonWidget self = (ButtonWidget) (Object) this;
		hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

		VanillaStyle.drawButtonFrame(self, height, hovered);
		// Sliders atualizam o valor aqui; o pegador original é pulado pelo SliderWidgetsMixin.
		mouseDragged(client, mouseX, mouseY);
		VanillaStyle.drawButtonLabel(self, height);
	}
}
