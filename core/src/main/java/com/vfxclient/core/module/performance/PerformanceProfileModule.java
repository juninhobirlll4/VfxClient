package com.vfxclient.core.module.performance;

import com.google.gson.JsonObject;
import com.vfxclient.core.VfxCore;
import com.vfxclient.core.command.VfxCommands;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.ModuleManager;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ButtonSetting;
import com.vfxclient.core.module.setting.ChoiceSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import com.vfxclient.core.module.setting.Setting;
import com.vfxclient.core.optimization.SystemSpecs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import org.lwjgl.opengl.Display;

/**
 * Presets de desempenho (Baixo, Equilibrado, Alto e Personalizado) e otimização automática.
 *
 * <ul>
 *   <li>Um preset ajusta os módulos de FPS do VfxClient e as opções gráficas do Minecraft
 *       (distância, gráficos, partículas, sombras, suavização de luz, VSync, mipmap...).</li>
 *   <li>Na primeira vez num mundo, aplica o preset indicado pelo launcher
 *       ({@code -Dvfx.preset=low|balanced|high}) ou, em PC fraco, o preset Baixo.</li>
 *   <li>Com a otimização automática ligada, se o FPS médio ficar abaixo da meta por 15 s,
 *       desce um preset (Alto → Equilibrado → Baixo) e avisa no chat.</li>
 * </ul>
 */
public class PerformanceProfileModule extends Module {

	/** Nenhum preset: o jogador ajusta tudo à mão. */
	public static final int CUSTOM = 0;
	/** Mais FPS possível. */
	public static final int LOW = 1;
	/** Bom equilíbrio entre visual e FPS. */
	public static final int BALANCED = 2;
	/** Visual completo. */
	public static final int HIGH = 3;

	private static final String[] NAMES = {"Personalizado", "Baixo", "Equilibrado", "Alto"};
	private static final int SAMPLE_WINDOW = 15;
	private static final int COOLDOWN_TICKS = 20 * 60;

	private final ChoiceSetting preset = add(new ChoiceSetting("preset", "Preset", CUSTOM, NAMES));
	private final BooleanSetting touchRenderDistance = addAdvanced(new BooleanSetting("touchRenderDistance",
			"Preset pode reduzir a distância de renderização", true));
	private final BooleanSetting autoOptimize = add(new BooleanSetting("autoOptimize",
			"Otimização automática (desce o preset se o FPS cair)", true));
	private final NumberSetting targetFps = addAdvanced(new NumberSetting("targetFps", "FPS mínimo desejado", 60, 30, 240, 10, " FPS"));
	private final BooleanSetting weakPcAuto = add(new BooleanSetting("weakPcAuto",
			"Na 1ª vez, ajustar sozinho para o meu PC", true));
	private final ButtonSetting diagnose = add(new ButtonSetting("diagnose", "Diagnóstico do PC", "Ver", new Runnable() {
		@Override
		public void run() {
			int recommended = SystemSpecs.recommendedPreset();
			VfxCommands.say("Seu PC: " + SystemSpecs.summary());
			VfxCommands.say("Preset recomendado: §d" + NAMES[recommended] + "§f. FPS agora: " + MinecraftClient.getCurrentFps());
		}
	}));
	private final ButtonSetting applyRecommended = add(new ButtonSetting("applyRecommended", "Aplicar o preset recomendado", "Aplicar", new Runnable() {
		@Override
		public void run() {
			applyPreset(SystemSpecs.recommendedPreset(), "recomendado para o seu PC");
		}
	}));

	private boolean firstRunDone;
	private int sampleTicks;
	private int samples;
	private long sampleSum;
	private int cooldown;

	public PerformanceProfileModule() {
		super("performanceProfile", "Preset de FPS", "Baixo, Equilibrado ou Alto", Category.PERFORMANCE, true);
	}

	@Override
	public boolean isAlwaysOn() {
		return true;
	}

	@Override
	public void onSettingChanged(Setting<?> setting) {
		if (setting == preset && preset.get() != CUSTOM) {
			apply(preset.get());
		}
	}

	/** Marca o preset como "Personalizado" (o jogador mexeu em alguma opção à mão). */
	public void markCustom() {
		preset.set(CUSTOM);
	}

	@Override
	public void onTick(MinecraftClient client) {
		if (client.world == null) {
			return;
		}
		if (!firstRunDone) {
			firstRunDone = true;
			firstRun();
			VfxCore.get().getConfig().markDirty();
		}
		autoOptimize(client);
	}

	private void firstRun() {
		String hint = System.getProperty("vfx.preset");
		if ("low".equalsIgnoreCase(hint)) {
			applyPreset(LOW, "indicado pelo launcher");
		} else if ("balanced".equalsIgnoreCase(hint)) {
			applyPreset(BALANCED, "indicado pelo launcher");
		} else if ("high".equalsIgnoreCase(hint)) {
			applyPreset(HIGH, "indicado pelo launcher");
		} else if (weakPcAuto.get()) {
			applyPreset(SystemSpecs.recommendedPreset(), "recomendado para o seu PC");
		}
	}

	private void autoOptimize(MinecraftClient client) {
		if (cooldown > 0) {
			cooldown--;
			return;
		}

		if (!autoOptimize.get() || preset.get() == CUSTOM || preset.get() == LOW || client.currentScreen != null) {
			samples = 0;
			sampleSum = 0;
			return;
		}

		if (++sampleTicks < 20) {
			return;
		}

		sampleTicks = 0;
		sampleSum += MinecraftClient.getCurrentFps();
		samples++;
		if (samples < SAMPLE_WINDOW) {
			return;
		}

		double average = sampleSum / (double) samples;
		samples = 0;
		sampleSum = 0;
		if (average < targetFps.get() * 0.8) {
			int lower = preset.get() - 1;
			applyPreset(lower, "o FPS médio ficou em " + Math.round(average));
			cooldown = COOLDOWN_TICKS;
		}
	}

	private void applyPreset(int level, String reason) {
		preset.set(level);
		apply(level);
		VfxCommands.say("Preset de FPS: §d" + NAMES[level] + "§f (" + reason + ").");
	}

	private void apply(int level) {
		ModuleManager modules = VfxCore.get().getModules();
		boolean high = level == HIGH;
		boolean low = level == LOW;

		set(modules.get(EntityCullingModule.class), !high);
		set(modules.get(EntityDistanceModule.class), !high);
		number(modules.get(EntityDistanceModule.class), "distance", low ? 32 : 64);
		set(modules.get(BlockEntityDistanceModule.class), !high);
		number(modules.get(BlockEntityDistanceModule.class), "distance", low ? 20 : 40);

		ParticleLimitModule particles = modules.get(ParticleLimitModule.class);
		set(particles, !high);
		number(particles, "amount", low ? 20 : 60);
		bool(particles, "explosions", !low);
		bool(particles, "smoke", !low);
		bool(particles, "blocks", true);
		bool(particles, "water", !low);
		bool(particles, "magic", !low);
		bool(particles, "footsteps", high);
		bool(particles, "crits", true);
		bool(particles, "potions", true);
		set(modules.get(NoCloudsModule.class), !high);
		set(modules.get(FpsLimiterModule.class), true);

		RenderTweaksModule tweaks = modules.get(RenderTweaksModule.class);
		set(tweaks, true);
		bool(tweaks, "animatedTextures", true);
		bool(tweaks, "animWater", !low);
		bool(tweaks, "animLava", !low);
		bool(tweaks, "animFire", true);
		bool(tweaks, "animPortal", !low);
		bool(tweaks, "fog", !low);
		bool(tweaks, "vignette", high);
		bool(tweaks, "weather", !low);
		bool(tweaks, "sky", true);
		bool(tweaks, "glint", !low);
		bool(tweaks, "stackedItems", !high);
		number(tweaks, "chunkBudget", low ? 2 : 0);

		applyVanilla(level);
		VfxCore.get().getConfig().markDirty();
	}

	/** Ajusta as opções gráficas do Minecraft (as mesmas do menu "Opções > Gráficos"). */
	private void applyVanilla(int level) {
		MinecraftClient client = MinecraftClient.getInstance();
		GameOptions options = client.options;
		boolean high = level == HIGH;
		boolean low = level == LOW;

		options.fancyGraphics = high;
		options.ao = high ? 2 : low ? 0 : 1;
		options.cloudMode = high ? 2 : 0;
		options.particle = high ? 0 : low ? 2 : 1;
		options.entityShadows = high;
		options.vbo = true;
		options.bobView = !low;
		if (!high) {
			options.vsync = false;
			options.maxFramerate = 260;
		}

		if (touchRenderDistance.get()) {
			int max = low ? 6 : level == BALANCED ? 10 : 16;
			options.viewDistance = Math.min(options.viewDistance, max);
		}

		Display.setVSyncEnabled(options.vsync);
		// Mipmap 0 recarrega as texturas (alguns segundos), por isso só muda quando precisa.
		int mipmap = low ? 0 : level == BALANCED ? 2 : 4;
		if (options.mipmapLevels != mipmap) {
			options.setValue(GameOptions.Option.MIPMAP_LEVELS, mipmap);
		}

		options.save();
		if (client.worldRenderer != null && client.world != null) {
			client.worldRenderer.reload();
		}
	}

	@Override
	public JsonObject toJson() {
		JsonObject json = super.toJson();
		json.addProperty("firstRunDone", firstRunDone);
		return json;
	}

	@Override
	public void fromJson(JsonObject json) {
		super.fromJson(json);
		if (json.has("firstRunDone") && json.get("firstRunDone").isJsonPrimitive()) {
			firstRunDone = json.get("firstRunDone").getAsBoolean();
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
