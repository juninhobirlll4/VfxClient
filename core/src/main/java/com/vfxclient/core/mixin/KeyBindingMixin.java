package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.util.ToggleSprintModule;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Recebe cada tecla pressionada durante o jogo e permite "segurar" teclas virtualmente.
 *
 * <p>O Minecraft chama {@code KeyBinding.onKeyPressed} para toda tecla apertada fora
 * de telas; usamos isso para abrir a Click GUI sem perder toques rápidos. O
 * {@code isPressed} é usado pelo sprint automático e pelo agachar alternado.</p>
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

	@Inject(method = "isPressed()Z", at = @At("RETURN"), cancellable = true)
	private void vfx$virtualPress(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue()) {
			return;
		}
		ToggleSprintModule module = VfxCore.module(ToggleSprintModule.class);
		if (module != null && module.forcePressed((KeyBinding) (Object) this)) {
			cir.setReturnValue(true);
		}
	}
}
