package com.vfxclient.core.module.hud;

import java.util.Locale;

import com.vfxclient.core.hud.ClickTracker;
import com.vfxclient.core.module.TextHudModule;
import com.vfxclient.core.module.setting.ChoiceSetting;
import net.minecraft.client.MinecraftClient;

/** Mostra os cliques por segundo: esquerdo, direito, os dois ou a soma; último segundo ou média de 5 s. */
public class CpsModule extends TextHudModule {

	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	private static final int BOTH = 2;
	private static final int COMBINED = 3;

	private final ChoiceSetting buttons = add(new ChoiceSetting("buttons", "Botões", BOTH, "Esquerdo", "Direito", "Esquerdo | Direito", "Soma dos dois"));
	private final ChoiceSetting average = add(new ChoiceSetting("average", "Cálculo", 0, "Último segundo", "Média de 5 s"));

	public CpsModule() {
		super("cps", "CPS", "Cliques por segundo", false, 4, 76);
	}

	private String value(double left, double right) {
		switch (buttons.get()) {
			case LEFT:
				return VALUE + number(left);
			case RIGHT:
				return VALUE + number(right);
			case COMBINED:
				return VALUE + number(left + right);
			default:
				return VALUE + number(left) + TEXT + " §7|§r " + VALUE + number(right);
		}
	}

	private String number(double value) {
		return average.get() == 0 ? String.valueOf((int) value) : String.format(Locale.ROOT, "%.1f", value);
	}

	private String format(double left, double right) {
		String value = value(left, right);
		return advanced() ? "CPS " + value : value + TEXT + " CPS";
	}

	@Override
	public String getText(MinecraftClient client) {
		if (average.get() == 0) {
			return format(ClickTracker.leftCps(), ClickTracker.rightCps());
		}
		return format(ClickTracker.average5s(true), ClickTracker.average5s(false));
	}

	@Override
	public String getSampleText() {
		return average.get() == 0 ? format(12, 4) : format(11.6, 3.8);
	}
}
