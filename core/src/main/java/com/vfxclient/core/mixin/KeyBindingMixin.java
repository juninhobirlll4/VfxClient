package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Recebe cada tecla pressionada durante o jogo.
 *
 * <p>O Minecraft chama {@code KeyBinding.onKeyPressed} para toda tecla apertada fora
 * de telas; usamos isso para abrir a Click GUI sem perder toques rápidos.</p>
 */
@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {

	@Inject(method = "onKeyPressed(I)V", at = @At("HEAD"))
	private static void vfx$onKeyPressed(int keyCode, CallbackInfo ci) {
		VfxCore core = VfxCore.get();
		if (core != null) {
			core.onKeyPressed(keyCode);
		}
	}
}
