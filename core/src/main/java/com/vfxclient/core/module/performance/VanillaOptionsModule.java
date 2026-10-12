package com.vfxclient.core.module.performance;

import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ChoiceSetting;
import com.vfxclient.core.module.setting.NumberSetting;
import com.vfxclient.core.module.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import org.lwjgl.opengl.Display;

/**
 * As opções gráficas do Minecraft dentro do menu do VfxClient, todas em um lugar.
 *
 * <p>Os valores são lidos do jogo sempre que a tela de opções abre e aplicados na hora
 * quando mudam (com o mesmo efeito do menu "Opções > Gráficos" do jogo).</p>
 */
public class VanillaOptionsModule extends Module {

	private final NumberSetting renderDistance = add(new NumberSetting("renderDistance", "Distância de renderização", 8, 2, 16, 1, " chunks"));
	private final NumberSetting maxFps = add(new NumberSetting("maxFps", "Limite de FPS (260 = sem limite)", 260, 10, 260, 10, ""));
	private final BooleanSetting vsync = add(new BooleanSetting("vsync", "VSync", false));
	private final ChoiceSetting graphics = add(new ChoiceSetting("graphics", "Gráficos", 0, "Rápidos", "Bonitos"));
	private final ChoiceSetting smoothLighting = add(new ChoiceSetting("smoothLighting", "Suavização de luz", 0, "Desligada", "Mínima", "Máxima"));
	private final ChoiceSetting clouds = add(new ChoiceSetting("clouds", "Nuvens do jogo", 0, "Desligadas", "Rápidas", "Bonitas"));
	private final ChoiceSetting particles = add(new ChoiceSetting("particles", "Partículas do jogo", 0, "Todas", "Reduzidas", "Mínimas"));
	private final BooleanSetting entityShadows = add(new BooleanSetting("entityShadows", "Sombras das entidades", true));
	private final BooleanSetting vbo = add(new BooleanSetting("vbo", "VBO (mais FPS na maioria das placas)", true));
	private final NumberSetting mipmap = add(new NumberSetting("mipmap", "Mipmap (0 = mais FPS, recarrega texturas)", 4, 0, 4, 1, ""));
	private final BooleanSetting viewBobbing = add(new BooleanSetting("viewBobbing", "Balançar a câmera ao andar", true));

	public VanillaOptionsModule() {
		super("vanillaOptions", "Opções gráficas", "Gráficos do Minecraft num lugar só", Category.PERFORMANCE, true);
	}

	@Override
	public boolean isAlwaysOn() {
		return true;
	}

	@Override
	public void onSettingsOpened() {
		GameOptions o = MinecraftClient.getInstance().options;
		renderDistance.set((double) o.viewDistance);
		maxFps.set((double) o.maxFramerate);
		vsync.set(o.vsync);
		graphics.set(o.fancyGraphics ? 1 : 0);
		smoothLighting.set(o.ao);
		clouds.set(o.cloudMode);
		particles.set(o.particle);
		entityShadows.set(o.entityShadows);
		vbo.set(o.vbo);
		mipmap.set((double) o.mipmapLevels);
		viewBobbing.set(o.bobView);
	}

	@Override
	public void onSettingChanged(Setting<?> setting) {
		MinecraftClient client = MinecraftClient.getInstance();
		GameOptions o = client.options;
		boolean reloadWorld = false;

		if (setting == renderDistance) {
			o.setValue(GameOptions.Option.RENDER_DISTANCE, renderDistance.getInt());
		} else if (setting == maxFps) {
			o.setValue(GameOptions.Option.MAX_FPS, maxFps.getInt());
		} else if (setting == mipmap) {
			o.setValue(GameOptions.Option.MIPMAP_LEVELS, mipmap.getInt());
		} else if (setting == vsync) {
			o.vsync = vsync.get();
			Display.setVSyncEnabled(o.vsync);
		} else if (setting == graphics) {
			o.fancyGraphics = graphics.get() == 1;
			reloadWorld = true;
		} else if (setting == smoothLighting) {
			o.ao = smoothLighting.get();
			reloadWorld = true;
		} else if (setting == clouds) {
			o.cloudMode = clouds.get();
		} else if (setting == particles) {
			o.particle = particles.get();
		} else if (setting == entityShadows) {
			o.entityShadows = entityShadows.get();
		} else if (setting == vbo) {
			o.vbo = vbo.get();
			reloadWorld = true;
		} else if (setting == viewBobbing) {
			o.bobView = viewBobbing.get();
		}
		o.save();
		if (reloadWorld && client.world != null && client.worldRenderer != null) {
			client.worldRenderer.reload();
		}
	}
}
