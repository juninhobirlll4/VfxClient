package com.vfxclient.core.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.NoCloudsModule;
import com.vfxclient.core.module.performance.RenderTweaksModule;
import com.vfxclient.core.module.visual.BlurModule;
import com.vfxclient.core.module.visual.FovModule;
import com.vfxclient.core.module.visual.FreelookModule;
import com.vfxclient.core.module.visual.MotionBlurModule;
import com.vfxclient.core.module.visual.NoHurtCamModule;
import com.vfxclient.core.module.visual.SaturationModule;
import com.vfxclient.core.module.visual.ZoomModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom, câmera sem tremor ao levar dano, nuvens desligáveis, freelook e efeitos de tela. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	@Inject(method = "getFov(FZ)F", at = @At("RETURN"), cancellable = true)
	private void vfx$zoom(float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		FovModule fov = VfxCore.module(FovModule.class);
		if (fov != null && changingFov && fov.isEnabled()) {
			cir.setReturnValue(fov.adjust(cir.getReturnValue()));
		}
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

	/** Freelook: o movimento do mouse gira a câmera em vez do jogador. */
	@Redirect(method = "render(FJ)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;increaseTransforms(FF)V"))
	private void vfx$freelookMouse(ClientPlayerEntity player, float yaw, float pitch) {
		FreelookModule freelook = VfxCore.module(FreelookModule.class);
		if (freelook == null || !freelook.isEnabled() || !freelook.consumeMouse(yaw, pitch)) {
			player.increaseTransforms(yaw, pitch);
		}
	}

	@Inject(method = "transformCamera(F)V", at = @At("HEAD"))
	private void vfx$freelookBegin(float tickDelta, CallbackInfo ci) {
		FreelookModule freelook = VfxCore.module(FreelookModule.class);
		if (freelook != null && freelook.isEnabled()) {
			freelook.beginCamera(MinecraftClient.getInstance().getCameraEntity());
		}
	}

	@Inject(method = "transformCamera(F)V", at = @At("TAIL"))
	private void vfx$freelookEnd(float tickDelta, CallbackInfo ci) {
		FreelookModule freelook = VfxCore.module(FreelookModule.class);
		if (freelook != null && freelook.isEnabled()) {
			freelook.endCamera(MinecraftClient.getInstance().getCameraEntity());
		}
	}

	/** Efeitos de tela do VfxClient, logo depois do mundo e antes da HUD. */
	@Inject(method = "render(FJ)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawEntityOutlineFramebuffer()V", shift = At.Shift.AFTER))
	private void vfx$postEffects(float tickDelta, long nanoTime, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		SaturationModule saturation = VfxCore.module(SaturationModule.class);
		if (saturation != null) {
			saturation.render(client, tickDelta);
		}
		MotionBlurModule blur = VfxCore.module(MotionBlurModule.class);
		if (blur != null) {
			blur.render(client, tickDelta);
		}
		BlurModule menuBlur = VfxCore.module(BlurModule.class);
		if (menuBlur != null) {
			menuBlur.render(client, tickDelta);
		}
	}

	@Shadow
	private float movementFovMultiplier;

	@Shadow
	private float lastMovementFovMultiplier;

	/** FOV fixo: ignora a mudança de FOV ao correr e com efeitos de velocidade/lentidão. */
	@Inject(method = "updateMovementFovMultiplier()V", at = @At("HEAD"), cancellable = true)
	private void vfx$staticFov(CallbackInfo ci) {
		FovModule fov = VfxCore.module(FovModule.class);
		if (fov != null && fov.staticFov()) {
			lastMovementFovMultiplier = 1F;
			movementFovMultiplier = 1F;
			ci.cancel();
		}
	}
}