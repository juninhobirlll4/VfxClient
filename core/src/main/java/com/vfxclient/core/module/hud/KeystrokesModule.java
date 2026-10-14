package com.vfxclient.core.module.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.gui.Anim;
import com.vfxclient.core.gui.Render2D;
import com.vfxclient.core.gui.Theme;
import com.vfxclient.core.hud.ClickTracker;
import com.vfxclient.core.module.HudModule;
import com.vfxclient.core.module.setting.BooleanSetting;
import com.vfxclient.core.module.setting.ChoiceSetting;
import com.vfxclient.core.module.setting.ColorSetting;
import com.vfxclient.core.module.setting.KeySetting;
import com.vfxclient.core.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;

/**
 * Teclas na tela (WASD, cliques, espaço e agachar) que acendem quando pressionadas.
 *
 * <p>Tudo é configurável: estilo, cores (fundo, texto, borda e as cores de "pressionado"),
 * tamanho e espaço das teclas, quais teclas aparecem e o alinhamento das linhas.</p>
 */
public class KeystrokesModule extends HudModule {

	private static final int MINIMAL = 0;
	private static final int BORDERED = 1;
	private static final int ROUNDED = 2;
	private static final int TRANSPARENT = 3;

	private static final int ALIGN_LEFT = 0;
	private static final int ALIGN_CENTER = 1;
	private static final int ALIGN_RIGHT = 2;

	private final ChoiceSetting style = add(new ChoiceSetting("style", "Estilo", MINIMAL, "Minimalista", "Com borda", "Arredondado", "Transparente"));
	private final ChoiceSetting align = addAdvanced(new ChoiceSetting("align", "Alinhamento das linhas", ALIGN_CENTER, "Esquerda", "Centro", "Direita"));
	private final NumberSetting keySize = add(new NumberSetting("keySize", "Tamanho da tecla", 22, 14, 32, 1, " px"));
	private final NumberSetting gap = addAdvanced(new NumberSetting("gap", "Espaço entre teclas", 2, 0, 6, 1, " px"));

	private final ColorSetting backgroundColor = add(new ColorSetting("bgColor", "Cor de fundo", 0x59000000));
	private final ColorSetting textColorSetting = add(new ColorSetting("textColor", "Cor do texto", 0xFFFFFFFF));
	private final ColorSetting borderColor = addAdvanced(new ColorSetting("borderColor", "Cor da borda", 0x99FFFFFF));
	private final ColorSetting pressedColor = add(new ColorSetting("pressedColor", "Fundo ao pressionar", 0xB3FFFFFF));
	private final ColorSetting pressedTextColor = addAdvanced(new ColorSetting("pressedTextColor", "Texto ao pressionar", 0xFF101010));

	private final BooleanSetting showW = add(new BooleanSetting("showW", "Tecla W", true));
	private final BooleanSetting showA = add(new BooleanSetting("showA", "Tecla A", true));
	private final BooleanSetting showS = add(new BooleanSetting("showS", "Tecla S", true));
	private final BooleanSetting showD = add(new BooleanSetting("showD", "Tecla D", true));
	private final BooleanSetting showLeft = add(new BooleanSetting("showLeft", "Clique esquerdo", true));
	private final BooleanSetting showRight = add(new BooleanSetting("showRight", "Clique direito", true));
	private final BooleanSetting showSpace = add(new BooleanSetting("showSpace", "Espaço", true));
	private final BooleanSetting showSneak = add(new BooleanSetting("showSneak", "Agachar (Shift)", false));
	private final BooleanSetting showCps = add(new BooleanSetting("showCps", "CPS nos cliques", true));
	private final ChoiceSetting mouseLabels = add(new ChoiceSetting("mouseLabels", "Nome dos cliques", 0, "ESQ / DIR", "LMB / RMB"));

	private final Map<String, Anim> anims = new HashMap<String, Anim>();

	public KeystrokesModule() {
		super("keystrokes", "Teclas", "WASD e cliques na tela", false, 4, 160);
	}

	@Override
	public boolean isSettingVisible(com.vfxclient.core.module.setting.Setting<?> setting) {
		// O fundo e a cor do texto daqui têm opções próprias (estilo e cores das teclas).
		return setting != background && setting != brandColor && super.isSettingVisible(setting);
	}

	@Override
	protected boolean usesSharedBackground() {
		return false;
	}

	@Override
	public boolean isVisible(MinecraftClient client, boolean preview) {
		return !rows(client.options).isEmpty();
	}

	@Override
	protected int contentWidth(MinecraftClient client, boolean preview) {
		return key() * 3 + gap() * 2;
	}

	@Override
	protected int contentHeight(MinecraftClient client, boolean preview) {
		List<List<Key>> rows = rows(client.options);
		int h = 0;
		for (List<Key> row : rows) {
			h += row.get(0).height + gap();
		}
		return Math.max(0, h - gap());
	}

	@Override
	protected void renderContent(MinecraftClient client, boolean preview) {
		int width = contentWidth(client, preview);
		int y = 0;
		for (List<Key> row : rows(client.options)) {
			int rowWidth = 0;
			for (Key key : row) {
				rowWidth += key.width;
			}
			rowWidth += gap() * (row.size() - 1);
			int x = align.get() == ALIGN_LEFT ? 0 : align.get() == ALIGN_RIGHT ? width - rowWidth : (width - rowWidth) / 2;
			for (Key key : row) {
				drawKey(client, key, x, y);
				x += key.width + gap();
			}
			y += row.get(0).height + gap();
		}
	}

	/** Monta as linhas visíveis conforme as teclas ligadas. */
	private List<List<Key>> rows(GameOptions o) {
		int k = key();
		int g = gap();
		int full = k * 3 + g * 2;
		List<List<Key>> rows = new ArrayList<List<Key>>();
		addRow(rows, key(showW, o.forwardKey, k, k, false));
		addRow(rows, key(showA, o.leftKey, k, k, false), key(showS, o.backKey, k, k, false), key(showD, o.rightKey, k, k, false));
		boolean both = showLeft.get() && showRight.get();
		int mouseW = both ? (full - g) / 2 : full;
		addRow(rows, key(showLeft, o.attackKey, mouseW, k, true), key(showRight, o.useKey, both ? full - mouseW - g : mouseW, k, true));
		addRow(rows, key(showSpace, o.jumpKey, full, Math.max(8, k / 2), false));
		addRow(rows, key(showSneak, o.sneakKey, full, Math.max(10, k * 2 / 3), false));
		return rows;
	}

	private static void addRow(List<List<Key>> rows, Key... keys) {
		List<Key> row = new ArrayList<Key>();
		for (Key key : keys) {
			if (key != null) {
				row.add(key);
			}
		}
		if (!row.isEmpty()) {
			rows.add(row);
		}
	}

	private static Key key(BooleanSetting shown, KeyBinding binding, int w, int h, boolean mouse) {
		return shown.get() ? new Key(binding, w, h, mouse) : null;
	}

	private void drawKey(MinecraftClient client, Key key, int x, int y) {
		Anim anim = anims.get(key.binding.getTranslationKey());
		if (anim == null) {
			anim = new Anim(0F);
			anims.put(key.binding.getTranslationKey(), anim);
		}
		float pressed = anim.update(key.binding.isPressed() ? 1F : 0F, 20F);
		int s = style.get();
		int idle = s == TRANSPARENT ? 0x00000000 : backgroundColor.argb();
		int fill = Theme.lerp(idle, pressedColor.argb(), pressed);
		float radius = s == ROUNDED ? Math.min(4F, key.height / 3F) : 0F;

		if (s == BORDERED || s == TRANSPARENT) {
			int border = Theme.lerp(borderColor.argb(), pressedColor.argb(), pressed);
			Render2D.roundedRect(x, y, key.width, key.height, radius, border);
			Render2D.roundedRect(x + 1, y + 1, key.width - 2, key.height - 2, Math.max(0, radius - 1), fill);
		} else {
			Render2D.roundedRect(x, y, key.width, key.height, radius, fill);
		}

		int color = Theme.lerp(textColorSetting.argb(), pressedTextColor.argb(), pressed);
		boolean textShadow = (shadow.get() || (style() != null && style().shadow())) && pressed < 0.5F;
		String label = labelFor(key);
		if (key.binding.getCode() == client.options.jumpKey.getCode() && key.height < 12) {
			// Barra de espaço baixa: só um traço no meio.
			DrawableHelper.fill(x + key.width / 2 - 10, y + key.height / 2, x + key.width / 2 + 10, y + key.height / 2 + 1, color);
			return;
		}
		int textW = client.textRenderer.getStringWidth(label);
		boolean cps = key.mouse && showCps.get() && key.height >= 18;
		float textY = cps ? y + key.height / 2F - 6 : y + (key.height - 8) / 2F;
		client.textRenderer.draw(label, x + (key.width - textW) / 2F, textY, color, textShadow);
		if (cps) {
			String sub = (key.binding.getCode() == ClickTracker.LEFT ? ClickTracker.leftCps() : ClickTracker.rightCps()) + " CPS";
			GlStateManager.pushMatrix();
			GlStateManager.translate(x + key.width / 2F, y + key.height / 2F + 3, 0F);
			GlStateManager.scale(0.5F, 0.5F, 1F);
			client.textRenderer.draw(sub, -client.textRenderer.getStringWidth(sub) / 2F, 0F, color, false);
			GlStateManager.popMatrix();
		}
	}

	private String labelFor(Key key) {
		int code = key.binding.getCode();
		boolean english = mouseLabels.get() == 1;
		if (code == ClickTracker.LEFT) {
			return english ? "LMB" : "ESQ";
		}
		if (code == ClickTracker.RIGHT) {
			return english ? "RMB" : "DIR";
		}
		String name = KeySetting.describe(code);
		if (name.equals("SPACE")) {
			return "Espaço";
		}
		if (name.equals("LSHIFT") || name.equals("RSHIFT")) {
			return "Shift";
		}
		return name.length() > 3 ? name.substring(0, 3) : name;
	}

	private int key() {
		return keySize.getInt();
	}

	private int gap() {
		return gap.getInt();
	}

	/** Uma tecla na grade. */
	private static final class Key {
		private final KeyBinding binding;
		private final int width;
		private final int height;
		private final boolean mouse;

		private Key(KeyBinding binding, int width, int height, boolean mouse) {
			this.binding = binding;
			this.width = width;
			this.height = height;
			this.mouse = mouse;
		}
	}
}
