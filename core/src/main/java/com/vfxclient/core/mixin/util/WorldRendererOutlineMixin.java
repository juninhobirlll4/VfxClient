package com.vfxclient.core.mixin.util;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.util.BlockOutlineModule;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Troca o contorno preto do bloco mirado pelo contorno personalizado. */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererOutlineMixin {

	@Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
	private void vfx$customOutline(PlayerEntity player, BlockHitResult hitResult, int pass, float tickDelta, CallbackInfo ci) {
		BlockOutlineModule module = VfxCore.module(BlockOutlineModule.class);
		if (module == null || !module.isEnabled()) {
			return;
		}
		ci.cancel();
		ClientWorld world = MinecraftClient.getInstance().world;
		if (pass != 0 || world == null || hitResult.type != BlockHitResult.Type.BLOCK) {
			return;
		}
		BlockPos pos = hitResult.getBlockPos();
		Block block = world.getBlockState(pos).getBlock();
		if (block.getMaterial() == Material.AIR || !world.getWorldBorder().contains(pos)) {
			return;
		}
		block.setBoundingBox(world, pos);
		double x = player.prevTickX + (player.x - player.prevTickX) * tickDelta;
		double y = player.prevTickY + (player.y - player.prevTickY) * tickDelta;
		double z = player.prevTickZ + (player.z - player.prevTickZ) * tickDelta;
		Box box = block.getSelectionBox(world, pos).expand(0.002, 0.002, 0.002).offset(-x, -y, -z);
		module.draw(box);
	}
}
