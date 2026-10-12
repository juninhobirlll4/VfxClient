package com.vfxclient.core.module.performance;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.ModuleManager;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ChoiceSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import com.vfxclient.core.module.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

/**
 * Perfis de desempenho prontos: um clique ajusta os módulos de FPS do VfxClient e as
 * opções gráficas do próprio Minecraft.
 *
 * <p>Depois de aplicar, tudo continua editável; mexer em qualquer opção não desfaz o
 * perfil, só o deixa "personalizado".</p>
 */
public class PerformanceProfileModule extends Module {

	private static final int CUSTOM = 0;
	private static final int QUALITY = 1;
	private static final int BALANCED = 2;
	private static final int MAX_FPS = 3;
	private static final int POTATO = 4;

	private final ChoiceSetting profile = add(new ChoiceSetting("profile", "Perfil", CUSTOM,
			"Personalizado", "Qualidade", "Equilibrado", "Máximo FPS", "PC fraco"));
	private final BooleanSetting touchRenderDistance = add(new BooleanSetting("touchRenderDistance",
			"Perfil pode reduzir a distância de renderização", true));

	public PerformanceProfileModule() {
		super("performanceProfile", "Perfil de FPS", "Um clique para mais FPS", Category.PERFORMANCE, true);
	}

	@Override
	public boolean isAlwaysOn() {
		return true;
	}

	@Override
	public void onSettingChanged(Setting<?> setting) {
		if (setting == profile && profile.get() != CUSTOM) {
			apply(profile.get());
		}
	}

	/** Marca o perfil como "Personalizado" (o jogador mexeu em alguma opção à mão). */
	public void markCustom() {
		profile.set(CUSTOM);
	}

	private void apply(int level) {
		ModuleManager modules = VfxCore.get().getModules();
		boolean quality = level == QUALITY;

		set(modules.get(EntityCullingModule.class), !quality);
		set(modules.get(EntityDistanceModule.class), level >= BALANCED);
		number(modules.get(EntityDistanceModule.class), "distance", level == BALANCED ? 64 : level == MAX_FPS ? 40 : 24);
		set(modules.get(BlockEntityDistanceModule.class), level >= BALANCED);
		number(modules.get(BlockEntityDistanceModule.class), "distance", level == BALANCED ? 40 : level == MAX_FPS ? 24 : 16);
		set(modules.get(ParticleLimitModule.class), !quality);
		number(modules.get(ParticleLimitModule.class), "amount", level == BALANCED ? 60 : level == MAX_FPS ? 25 : 10);
		set(modules.get(NoCloudsModule.class), !quality);
		set(modules.get(FpsLimiterModule.class), true);

		RenderTweaksModule tweaks = modules.get(RenderTweaksModule.class);
		set(tweaks, true);
		bool(tweaks, "animatedTextures", level <= BALANCED);
		bool(tweaks, "weather", level <= BALANCED);
		bool(tweaks, "sky", level <= MAX_FPS);
		bool(tweaks, "glint", level <= BALANCED);
		bool(tweaks, "stackedItems", !quality);
		number(tweaks, "chunkBudget", level >= MAX_FPS ? 2 : 0);

		applyVanilla(level);
		VfxCore.get().getConfig().markDirty();
	}

	/** Ajusta as opções gráficas do Minecraft (as mesmas do menu "Opções > Gráficos"). */
	private void applyVanilla(int level) {
		MinecraftClient client = MinecraftClient.getInstance();
		GameOptions options = client.options;
		boolean quality = level == QUALITY;

		options.fancyGraphics = quality;
		options.ao = quality ? 2 : level == BALANCED ? 1 : 0;
		options.cloudMode = quality ? 2 : 0;
		options.particle = quality ? 0 : level == BALANCED ? 1 : 2;
		options.entityShadows = quality;
		options.vbo = true;
		if (!quality) {
			options.vsync = false;
			options.maxFramerate = Math.max(options.maxFramerate, 260);
		}
		if (touchRenderDistance.get()) {
			int max = level == BALANCED ? 12 : level == MAX_FPS ? 8 : level == POTATO ? 6 : Integer.MAX_VALUE;
			options.viewDistance = Math.min(options.viewDistance, max);
		}
		org.lwjgl.opengl.Display.setVSyncEnabled(options.vsync);
		options.save();
		if (client.worldRenderer != null && client.world != null) {
			client.worldRenderer.reload();
		}
	}

	private static void set(Module module, boolean enabled) {
		module.setEnabled(enabled);
	}

	private static void bool(Module module, String id, boolean value) {
		Setting<?> setting = module.getSetting(id);
		if (setting instanceof BooleanSetting) {
			((BooleanSetting) setting).set(value);
		}
	}

	private static void number(Module module, String id, double value) {
		Setting<?> setting = module.getSetting(id);
		if (setting instanceof NumberSetting) {
			((NumberSetting) setting).set(value);
		}
	}
}
