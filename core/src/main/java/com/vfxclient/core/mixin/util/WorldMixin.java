package com.vfxclient.core.mixin.util;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.util.TimeChangerModule;
import com.vfxclient.core.module.util.WeatherChangerModule;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hora e clima locais: só o mundo do cliente é afetado, nunca o servidor (nem o integrado). */
@Mixin(World.class)
public abstract class WorldMixin {

	@Inject(method = "getTimeOfDay()J", at = @At("HEAD"), cancellable = true)
	private void vfx$localTime(CallbackInfoReturnable<Long> cir) {
		if (!((Object) this instanceof ClientWorld)) {
			return;
		}
		TimeChangerModule module = VfxCore.module(TimeChangerModule.class);
		if (module != null && module.isEnabled()) {
			cir.setReturnValue(module.timeOfDay());
		}
	}

	@Inject(method = "getRainGradient(F)F", at = @At("HEAD"), cancellable = true)
	private void vfx$localRain(float delta, CallbackInfoReturnable<Float> cir) {
		WeatherChangerModule module = vfx$weather();
		if (module != null) {
			cir.setReturnValue(module.rain());
		}
	}

	@Inject(method = "getThunderGradient(F)F", at = @At("HEAD"), cancellable = true)
	private void vfx$localThunder(float delta, CallbackInfoReturnable<Float> cir) {
		WeatherChangerModule module = vfx$weather();
		if (module != null) {
			cir.setReturnValue(module.thunder());
		}
	}

	@Unique
	private WeatherChangerModule vfx$weather() {
		if (!((Object) this instanceof ClientWorld)) {
			return null;
		}
		WeatherChangerModule module = VfxCore.module(WeatherChangerModule.class);
		return module != null && module.isEnabled() ? module : null;
	}
}
