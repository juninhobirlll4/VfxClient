package com.vfxclient.core.mixin;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.performance.EntityCullingModule;
import com.vfxclient.core.module.performance.EntityDistanceModule;
import com.vfxclient.core.module.visual.HitboxModule;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.CameraView;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pula o desenho de entidades além da distância configurada; hitbox com cor e espessura. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void vfx$entityDistance(Entity entity, CameraView cameraView, double x, double y, double z,
			CallbackInfoReturnable<Boolean> cir) {
		EntityDistanceModule module = VfxCore.module(EntityDistanceModule.class);
		if (module != null && module.shouldSkip(entity)) {
			cir.setReturnValue(false);
			return;
		}
		EntityCullingModule culling = VfxCore.module(EntityCullingModule.class);
		if (culling != null && culling.shouldCull(entity)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
	private void vfx$hitboxStart(Entity entity, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
		HitboxModule module = VfxCore.module(HitboxModule.class);
		if (module == null || !module.isEnabled()) {
			return;
		}
		if (!module.shows(entity)) {
			ci.cancel();
			return;
		}
		GL11.glLineWidth(module.lineWidth());
	}

	@Inject(method = "renderHitbox", at = @At("TAIL"))
	private void vfx$hitboxEnd(Entity entity, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
		GL11.glLineWidth(1F);
	}

	/** Primeira caixa do renderHitbox = a caixa da entidade (branca no jogo). */
	@Redirect(method = "renderHitbox",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawBox(Lnet/minecraft/util/math/Box;IIII)V", ordinal = 0))
	private void vfx$hitboxColor(Box box, int r, int g, int b, int a) {
		HitboxModule module = VfxCore.module(HitboxModule.class);
		if (module != null && module.isEnabled()) {
			int c = module.color();
			WorldRenderer.drawBox(box, (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, (c >>> 24) & 0xFF);
			return;
		}
		WorldRenderer.drawBox(box, r, g, b, a);
	}

	/** Segunda caixa = a linha dos olhos (vermelha). */
	@Redirect(method = "renderHitbox",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawBox(Lnet/minecraft/util/math/Box;IIII)V", ordinal = 1))
	private void vfx$eyeLine(Box box, int r, int g, int b, int a) {
		HitboxModule module = VfxCore.module(HitboxModule.class);
		if (module == null || !module.isEnabled() || module.eyeLine()) {
			WorldRenderer.drawBox(box, r, g, b, a);
		}
	}
}