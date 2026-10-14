package com.vfxclient.core.mixin;

import com.vfxclient.core.gui.VanillaStyle;
import net.minecraft.client.gui.widget.SliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Pula o pegador original dos outros sliders do jogo (o nosso é desenhado pelo {@link VanillaStyle}). */
@Mixin(SliderWidget.class)
public abstract class SliderWidgetMixin {

	@Redirect(method = "mouseDragged", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/widget/SliderWidget;drawTexture(IIIIII)V"))
	private void vfx$skipKnob(SliderWidget slider, int x, int y, int u, int v, int width, int height) {
		if (!VanillaStyle.enabled()) {
			slider.drawTexture(x, y, u, v, width, height);
		}
	}
}
