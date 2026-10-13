package com.vfxclient.core.render;

import java.util.List;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.mixin.ShaderEffectAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.PostProcessShader;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.util.Identifier;

/**
 * Efeito de pós-processamento que usa os shaders que já vêm no próprio Minecraft
 * ({@code assets/minecraft/shaders/post}). Fica separado do shader do jogo (visão de
 * creeper etc.) e só existe enquanto está ligado.
 */
public final class PostEffect {

	private final Identifier id;
	private ShaderEffect effect;
	private int width;
	private int height;
	private boolean failed;

	/** @param path caminho do JSON do efeito, ex.: {@code shaders/post/phosphor.json} */
	public PostEffect(String path) {
		this.id = new Identifier(path);
	}

	/** @return se a placa de vídeo e as opções permitem efeitos de tela */
	public static boolean supported(MinecraftClient client) {
		return GLX.shadersSupported && client.options.fbo;
	}

	/**
	 * Desenha o efeito sobre a imagem do mundo.
	 *
	 * @param client    instância do Minecraft
	 * @param tickDelta fração do tick
	 * @param uniform   nome do parâmetro do shader a ajustar
	 * @param values    valores do parâmetro
	 */
	public void render(MinecraftClient client, float tickDelta, String uniform, float... values) {
		if (failed || !supported(client)) {
			return;
		}
		if (effect == null) {
			try {
				effect = new ShaderEffect(client.getTextureManager(), client.getResourceManager(), client.getFramebuffer(), id);
			} catch (Exception e) {
				failed = true;
				VfxCore.LOGGER.warn("Efeito de tela indisponível ({}): {}", id, e.toString());
				return;
			}
			width = -1;
		}
		if (width != client.width || height != client.height) {
			width = client.width;
			height = client.height;
			effect.setupDimensions(width, height);
		}
		List<PostProcessShader> passes = ((ShaderEffectAccessor) effect).vfx$getPasses();
		for (PostProcessShader pass : passes) {
			GlUniform u = pass.getProgram().getUniformByName(uniform);
			if (u != null) {
				u.set(values);
			}
		}
		GlStateManager.matrixMode(5890);
		GlStateManager.pushMatrix();
		GlStateManager.loadIdentity();
		effect.render(tickDelta);
		GlStateManager.popMatrix();
	}

	/** Libera o efeito (chamado ao desligar). */
	public void close() {
		if (effect != null) {
			effect.disable();
			effect = null;
		}
		failed = false;
	}
}
