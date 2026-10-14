package com.vfxclient.core.mixin;

import com.vfxclient.core.gui.VanillaStyle;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Troca a terra atrás das listas do jogo pelo véu do VfxClient. É feito na textura (e não no
 * desenho da lista) porque o OptiFine muda o método que desenha esse fundo.
 */
@Mixin(TextureManager.class)
public abstract class TextureManagerMixin {

	@Inject(method = "bindTexture", at = @At("HEAD"), cancellable = true)
	private void vfx$listVeil(Identifier id, CallbackInfo ci) {
		if (VanillaStyle.bindInsteadOf(id)) {
			ci.cancel();
		}
	}
}
