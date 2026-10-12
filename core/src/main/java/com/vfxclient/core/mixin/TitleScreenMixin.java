package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gancho na tela de título.
 *
 * <p>No M0 serve só para validar que o pipeline de Mixin funciona. Na v0.1 é aqui
 * que a logo do VfxClient entra no lugar da logo padrão.</p>
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

	@Unique
	private static boolean vfx$announced;

	@Inject(method = "init()V", at = @At("HEAD"))
	private void vfx$onInit(CallbackInfo ci) {
		if (!vfx$announced) {
			vfx$announced = true;
			VfxCore.LOGGER.info("Tela de título aberta — Mixins do VfxClient ativos.");
		}
	}
}
