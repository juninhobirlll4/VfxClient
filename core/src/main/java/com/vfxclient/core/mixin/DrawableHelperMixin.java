package com.vfxclient.core.mixin;

import com.vfxclient.core.gui.VanillaStyle;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Títulos das telas do jogo em Montserrat. O jogo não marca o que é título; por isso vale o
 * texto branco centralizado bem no alto da tela, que é onde todas as telas põem o título.
 */
@Mixin(DrawableHelper.class)
public abstract class DrawableHelperMixin {

	@Unique
	private static final int VFX$TITLE_MAX_Y = 20;
	@Unique
	private static final int VFX$WHITE = 0xFFFFFF;

	@Inject(method = "drawCenteredString", at = @At("HEAD"), cancellable = true)
	private void vfx$title(TextRenderer textRenderer, String text, int centerX, int y, int color, CallbackInfo ci) {
		if (y > VFX$TITLE_MAX_Y || (color & 0xFFFFFF) != VFX$WHITE || !vfx$isGameScreen() || !VanillaStyle.enabled()) {
			return;
		}
		VanillaStyle.drawTitle(text, centerX, y);
		ci.cancel();
	}

	/** Só telas do jogo; as do VfxClient já desenham o próprio título. */
	@Unique
	private boolean vfx$isGameScreen() {
		Object self = this;
		return self instanceof Screen && !self.getClass().getName().startsWith("com.vfxclient.");
	}
}
