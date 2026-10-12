package com.vfxclient.core.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import com.vfxclient.core.module.performance.NoCloudsModule;
import com.vfxclient.core.module.performance.RenderTweaksModule;
import com.vfxclient.core.module.visual.NoHurtCamModule;
import com.vfxclient.core.module.visual.ZoomModule;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom, câmera sem tremor ao levar dano e nuvens desligáveis. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	@Inject(method = "getFov(FZ)F", at = @At("RETURN"), cancellable = true)
	private void vfx$zoom(float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		ZoomModule zoom = VfxCore.module(ZoomModule.class);
		if (zoom != null && changingFov) {
			float multiplier = zoom.fovMultiplier();
			if (multiplier != 1F) {
				cir.setReturnValue(cir.getReturnValue() * multiplier);
			}
		}
	}

	@Inject(method = "bobViewWhenHurt(F)V", at = @At("HEAD"), cancellable = true)
	private void vfx$noHurtCam(float tickDelta, CallbackInfo ci) {
		NoHurtCamModule module = VfxCore.module(NoHurtCamModule.class);
		if (module != null && module.isEnabled()) {
			ci.cancel();
		}
	}

	/**
	 * Sem névoa: desliga a névoa logo depois que o jogo a configura. Cegueira continua
	 * funcionando (é efeito de jogo, não enfeite).
	 */
	@Inject(method = "renderFog(IF)V", at = @At("TAIL"))
	private void vfx$noFog(int mode, float tickDelta, CallbackInfo ci) {
		RenderTweaksModule tweaks = VfxCore.module(RenderTweaksModule.class);
		if (tweaks == null || tweaks.fog()) {
			return;
		}
		Entity camera = MinecraftClient.getInstance().getCameraEntity();
		if (camera instanceof LivingEntity && ((LivingEntity) camera).hasStatusEffect(StatusEffect.BLINDNESS)) {
			return;
		}
		GlStateManager.disableFog();
	}

	@Inject(method = "renderWeather(F)V", at = @At("HEAD"), cancellable = true)
	private void vfx$noWeather(float tickDelta, CallbackInfo ci) {
		RenderTweaksModule tweaks = VfxCore.module(RenderTweaksModule.class);
		if (tweaks != null && !tweaks.weather()) {
			ci.cancel();
		}
	}

	@Inject(method = "tickRainSplashing()V", at = @At("HEAD"), cancellable = true)
	private void vfx$noRainSplash(CallbackInfo ci) {
		RenderTweaksModule tweaks = VfxCore.module(RenderTweaksModule.class);
		if (tweaks != null && !tweaks.weather()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void vfx$noClouds(CallbackInfo ci) {
		NoCloudsModule module = VfxCore.module(NoCloudsModule.class);
		if (module != null && module.isEnabled()) {
			ci.cancel();
		}
	}
}
