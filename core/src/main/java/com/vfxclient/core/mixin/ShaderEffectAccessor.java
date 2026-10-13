package com.vfxclient.core.mixin;

import java.util.List;

import net.minecraft.client.gl.PostProcessShader;
import net.minecraft.client.gl.ShaderEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Acesso às etapas de um efeito de tela, para ajustar a intensidade. */
@Mixin(ShaderEffect.class)
public interface ShaderEffectAccessor {

	@Accessor("passes")
	List<PostProcessShader> vfx$getPasses();
}