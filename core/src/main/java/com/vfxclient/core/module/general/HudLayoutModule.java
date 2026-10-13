package com.vfxclient.core.module.general;

import java.util.Arrays;
import java.util.List;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.HudModule;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.hud.ArmorModule;
import com.vfxclient.core.module.hud.ClockModule;
import com.vfxclient.core.module.hud.CoordinatesModule;
import com.vfxclient.core.module.hud.CpsModule;
import com.vfxclient.core.module.hud.FpsModule;
import com.vfxclient.core.module.hud.KeystrokesModule;
import com.vfxclient.core.module.hud.PingModule;
import com.vfxclient.core.module.hud.PlaytimeModule;
import com.vfxclient.core.module.hud.PotionEffectsModule;
import com.vfxclient.core.module.hud.ServerIpModule;
import com.vfxclient.core.module.setting.ButtonSetting;
import com.vfxclient.core.module.setting.ChoiceSetting;
import net.minecraft.client.MinecraftClient;

/**
 * Layouts prontos da HUD. Cada um liga só os elementos que fazem sentido e os organiza
 * nos cantos, com espaço entre eles:
 * <ul>
 *   <li>superior esquerdo: FPS, ping, coordenadas, relógio;</li>
 *   <li>superior direito: servidor e tempo de jogo;</li>
 *   <li>inferior central: teclas (com CPS), acima da barra de itens;</li>
 *   <li>inferior direito: armadura e efeitos.</li>
 * </ul>
 * Depois de aplicar, dá para ajustar tudo no editor de HUD.
 */
public class HudLayoutModule extends Module {

	/** Espaço entre elementos empilhados e até a borda da tela. */
	private static final int GAP = 4;
	/** Altura reservada acima da borda inferior para a barra de itens, vida e fome. */
	private static final int HOTBAR = 52;

	private static final String[] NAMES = {"Minimal", "PvP", "Survival", "Content Creator", "Competitivo"};

	private final ChoiceSetting layout = add(new ChoiceSetting("layout", "Layout", 1, NAMES));
	private final ButtonSetting apply = add(new ButtonSetting("apply", "Aplicar o layout escolhido", "Aplicar", new Runnable() {
		@Override
		public void run() {
			apply(layout.get());
		}
	}));

	public HudLayoutModule() {
		super("hudLayout", "Layouts da HUD", "Minimal, PvP, Survival, Content Creator, Competitivo", Category.GENERAL, true);
	}

	@Override
	public boolean isAlwaysOn() {
		return true;
	}

	/**
	 * Aplica um layout pronto.
	 *
	 * @param index posição em {@link #NAMES}
	 */
	public void apply(int index) {
		List<Class<? extends HudModule>> topLeft;
		List<Class<? extends HudModule>> topRight = Arrays.<Class<? extends HudModule>>asList();
		List<Class<? extends HudModule>> bottomCenter = Arrays.<Class<? extends HudModule>>asList();
		List<Class<? extends HudModule>> bottomRight = Arrays.<Class<? extends HudModule>>asList();
		switch (index) {
			case 0: // Minimal
				topLeft = Arrays.<Class<? extends HudModule>>asList(FpsModule.class, CoordinatesModule.class);
				break;
			case 2: // Survival
				topLeft = Arrays.<Class<? extends HudModule>>asList(FpsModule.class, CoordinatesModule.class, ClockModule.class);
				topRight = Arrays.<Class<? extends HudModule>>asList(PlaytimeModule.class);
				bottomRight = Arrays.<Class<? extends HudModule>>asList(ArmorModule.class, PotionEffectsModule.class);
				break;
			case 3: // Content Creator
				topLeft = Arrays.<Class<? extends HudModule>>asList(FpsModule.class, PingModule.class, ClockModule.class);
				topRight = Arrays.<Class<? extends HudModule>>asList(ServerIpModule.class, PlaytimeModule.class);
				bottomCenter = Arrays.<Class<? extends HudModule>>asList(KeystrokesModule.class);
				bottomRight = Arrays.<Class<? extends HudModule>>asList(ArmorModule.class, PotionEffectsModule.class);
				break;
			case 4: // Competitivo
				topLeft = Arrays.<Class<? extends HudModule>>asList(FpsModule.class, PingModule.class, CpsModule.class);
				bottomCenter = Arrays.<Class<? extends HudModule>>asList(KeystrokesModule.class);
				bottomRight = Arrays.<Class<? extends HudModule>>asList(ArmorModule.class, PotionEffectsModule.class);
				break;
			default: // PvP
				topLeft = Arrays.<Class<? extends HudModule>>asList(FpsModule.class, PingModule.class, CoordinatesModule.class, ClockModule.class);
				bottomCenter = Arrays.<Class<? extends HudModule>>asList(KeystrokesModule.class);
				bottomRight = Arrays.<Class<? extends HudModule>>asList(ArmorModule.class, PotionEffectsModule.class);
				break;
		}

		// Só mexe nos elementos da categoria HUD; o resto (mira, sprint...) fica como está.
		for (Module module : VfxCore.get().getModules().byCategory(Category.HUD)) {
			module.setEnabled(false);
		}
		stack(topLeft, HudModule.TOP_LEFT, GAP);
		stack(topRight, HudModule.TOP_RIGHT, GAP);
		stack(bottomCenter, HudModule.BOTTOM_CENTER, HOTBAR);
		stack(bottomRight, HudModule.BOTTOM_RIGHT, GAP);
		VfxCore.get().getConfig().markDirty();
	}

	/** Liga e empilha os elementos a partir da borda da âncora. */
	private static void stack(List<Class<? extends HudModule>> types, int anchor, int start) {
		MinecraftClient client = MinecraftClient.getInstance();
		int offset = start;
		for (Class<? extends HudModule> type : types) {
			HudModule module = VfxCore.get().getModules().get(type);
			module.setEnabled(true);
			module.place(anchor, anchor == HudModule.BOTTOM_CENTER ? 0 : GAP, offset);
			offset += module.getHeight(client, true) + GAP;
		}
	}
}
