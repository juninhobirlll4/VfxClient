package com.vfxclient.core.gui;

import java.util.ArrayList;
import java.util.List;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.gui.font.Fonts;
import com.vfxclient.core.module.hud.PingModule;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ResourcePackScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;

/**
 * Menu de pausa (ESC) no visual do VfxClient: jogo desfocado ao fundo, cartão com a logo e os
 * botões, e informações do servidor embaixo.
 *
 * <p>Estende o menu de pausa do jogo e reaproveita as ações dele (voltar, opções, conquistas,
 * estatísticas, desconectar); só o desenho é nosso.</p>
 */
public class VfxPauseScreen extends GameMenuScreen implements BlurredScreen {

	// Ids dos botões do menu de pausa do jogo.
	private static final int OPTIONS = 0;
	private static final int DISCONNECT = 1;
	private static final int RETURN = 4;
	private static final int ACHIEVEMENTS = 5;
	private static final int STATS = 6;

	// Ações que só existem no nosso menu.
	private static final int VFX_MENU = 100;
	private static final int HUD_EDITOR = 101;
	private static final int PACKS = 102;

	private static final float CARD_W = 560;
	private static final float BUTTON_W = 464;
	private static final float BUTTON_H = 58;
	private static final float GAP = 14;

	private final List<ClickArea> areas = new ArrayList<ClickArea>();
	private final long openedAt = System.currentTimeMillis();
	private Canvas canvas;

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		areas.clear();
		drawBackdrop();

		canvas = Canvas.begin(width, height);
		float mx = canvas.toCanvas(mouseX);
		float my = canvas.toCanvas(mouseY);

		drawTopBar(mx, my);
		drawCard(mx, my);
		drawServerInfo();
		drawFooter();

		canvas.end();
		Ui.restore();
	}

	/** Escurece o jogo (o desfoque é aplicado no mundo pelo {@link BlurredScreen}). */
	private void drawBackdrop() {
		fillGradient(0, 0, width, height, 0x99050309, 0xD9050309);
	}

	private void drawTopBar(float mx, float my) {
		Ui.brand(30, 24);

		float x = canvas.width() - 28 - 58;
		int[] actions = {RETURN, PACKS, VFX_MENU, OPTIONS};
		Ui.Icon[] icons = {Ui.Icon.CLOSE, Ui.Icon.PACKS, Ui.Icon.MENU, Ui.Icon.SETTINGS};
		for (int i = 0; i < actions.length; i++) {
			Ui.iconButton(icons[i], x, 24, Ui.inside(mx, my, x, 24, 58, 58));
			areas.add(new ClickArea(actions[i], x, 24, 58, 58));
			x -= 70;
		}
	}

	private void drawCard(float mx, float my) {
		float appear = Anim.easeOutCubic((System.currentTimeMillis() - openedAt) / 260F);
		float cardH = 648;
		float cardX = canvas.centerX() - CARD_W / 2F;
		float cardY = canvas.centerY() - cardH / 2F - 8 + (1F - appear) * 18F;

		Render2D.outlinedRect(cardX, cardY, CARD_W, cardH, 22, 0x2EB48CFF, 0x9E0A0810);

		// Logo, título e subtítulo.
		Logo.draw((int) (canvas.centerX() - 48), (int) (cardY + 42), 96);
		Ui.restore();
		Fonts.drawSpacedCentered(Fonts.TITLE, "PAUSADO", canvas.centerX(), cardY + 156, 40, 0.12F, 0xFFFFFFFF);
		String subtitle = client.isInSingleplayer() ? "O mundo fica parado enquanto você está aqui" : "O jogo continua rodando no servidor";
		Fonts.drawCentered(Fonts.TEXT, subtitle, canvas.centerX(), cardY + 212, 17, Ui.MUTED);

		// Botões.
		float x = canvas.centerX() - BUTTON_W / 2F;
		float half = (BUTTON_W - GAP) / 2F;
		float y = cardY + 260;

		button("VOLTAR AO JOGO", RETURN, x, y, BUTTON_W, 64, 21, Ui.Style.PRIMARY, mx, my);
		y += 64 + GAP;
		button("MENU DO VFXCLIENT", VFX_MENU, x, y, BUTTON_W, BUTTON_H, 19, Ui.Style.NORMAL, mx, my);
		y += BUTTON_H + GAP;
		button("OPÇÕES", OPTIONS, x, y, half, BUTTON_H, 19, Ui.Style.NORMAL, mx, my);
		button("EDITAR HUD", HUD_EDITOR, x + half + GAP, y, half, BUTTON_H, 19, Ui.Style.NORMAL, mx, my);
		y += BUTTON_H + GAP;
		button("CONQUISTAS", ACHIEVEMENTS, x, y, half, BUTTON_H, 19, Ui.Style.NORMAL, mx, my);
		button("ESTATÍSTICAS", STATS, x + half + GAP, y, half, BUTTON_H, 19, Ui.Style.NORMAL, mx, my);
		y += BUTTON_H + GAP;
		String leave = client.isInSingleplayer() ? "SALVAR E SAIR" : "DESCONECTAR";
		button(leave, DISCONNECT, x, y, BUTTON_W, BUTTON_H, 19, Ui.Style.DANGER, mx, my);
	}

	private void button(String label, int action, float x, float y, float w, float h, float size, Ui.Style style,
			float mx, float my) {
		Ui.button(label, x, y, w, h, size, style, Ui.inside(mx, my, x, y, w, h));
		areas.add(new ClickArea(action, x, y, w, h));
	}

	/** Etiquetas embaixo: servidor, ping, jogadores e tempo de sessão. */
	private void drawServerInfo() {
		List<String[]> pills = new ArrayList<String[]>();
		List<Integer> colors = new ArrayList<Integer>();

		pills.add(new String[] {"Servidor", serverName()});
		colors.add(0xFFFFFFFF);

		int ping = currentPing();
		if (ping >= 0) {
			pills.add(new String[] {"Ping", ping + " ms"});
			colors.add(pingColor(ping));
		}
		if (client.getNetworkHandler() != null && !client.isInSingleplayer()) {
			pills.add(new String[] {"Jogadores", String.valueOf(client.getNetworkHandler().getPlayerList().size())});
			colors.add(0xFFFFFFFF);
		}
		pills.add(new String[] {"Sessão", sessionText()});
		colors.add(0xFFFFFFFF);

		float total = 0;
		for (String[] pill : pills) {
			total += Ui.pillWidth(pill[0], pill[1]) + 14;
		}
		float x = canvas.centerX() - (total - 14) / 2F;
		float y = canvas.height() - 110 - 46;
		for (int i = 0; i < pills.size(); i++) {
			String[] pill = pills.get(i);
			Ui.pill(pill[0], pill[1], colors.get(i), x, y, 46);
			x += Ui.pillWidth(pill[0], pill[1]) + 14;
		}
	}

	/** Endereço do servidor (ou "Mundo local"); se entrou direto pelo launcher, usa a conexão. */
	private String serverName() {
		if (client.isInSingleplayer()) {
			return "Mundo local";
		}
		ServerInfo server = client.getCurrentServerEntry();
		if (server != null) {
			return server.address;
		}
		if (client.getNetworkHandler() != null) {
			String address = String.valueOf(client.getNetworkHandler().getClientConnection().getAddress());
			int slash = address.indexOf('/');
			return slash > 0 ? address.substring(0, slash) : address.replace("/", "");
		}
		return "-";
	}

	private int currentPing() {
		if (client.player == null || client.getNetworkHandler() == null || client.isInSingleplayer()) {
			return -1;
		}
		PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
		return entry == null ? -1 : entry.getLatency();
	}

	private static int pingColor(int ping) {
		String code = PingModule.qualityCode(ping);
		if ("§a".equals(code)) {
			return Ui.GREEN;
		}
		return "§e".equals(code) || "§6".equals(code) ? Ui.YELLOW : Ui.RED;
	}

	private static String sessionText() {
		long seconds = VfxCore.get().sessionMillis() / 1000L;
		long hours = seconds / 3600;
		long minutes = seconds / 60 % 60;
		return hours > 0 ? hours + "h " + minutes + "min" : minutes + "min";
	}

	private void drawFooter() {
		float y = canvas.height() - 40;
		String version = "VFXCLIENT 1.8.9 (v" + VfxCore.get().getVersion() + ")";
		Fonts.drawWithShadow(Fonts.TEXT_BOLD, version, 28, y, 18, 0xFFB8B8C0);
		String hint = "ESC volta ao jogo";
		Fonts.drawWithShadow(Fonts.TEXT_BOLD, hint, canvas.width() - 28 - Fonts.width(Fonts.TEXT_BOLD, hint, 18), y, 18, 0xFFB8B8C0);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (button != 0 || canvas == null) {
			return;
		}
		float mx = canvas.toCanvas(mouseX);
		float my = canvas.toCanvas(mouseY);
		for (ClickArea area : areas) {
			if (area.contains(mx, my)) {
				playClickSound();
				run(area.action());
				return;
			}
		}
	}

	private void run(int action) {
		switch (action) {
			case VFX_MENU:
				client.setScreen(new ClickGuiScreen());
				break;
			case HUD_EDITOR:
				client.setScreen(new HudEditorScreen(this));
				break;
			case PACKS:
				client.setScreen(new ResourcePackScreen(this));
				break;
			default:
				ButtonWidget vanilla = vanillaButton(action);
				if (vanilla != null) {
					buttonClicked(vanilla);
				}
				break;
		}
	}

	/** Botão original do jogo com esse id (as ações do jogo leem dele). */
	private ButtonWidget vanillaButton(int id) {
		for (ButtonWidget widget : buttons) {
			if (widget.id == id) {
				return widget;
			}
		}
		return null;
	}

	private void playClickSound() {
		client.getSoundManager().play(PositionedSoundInstance.master(new Identifier("gui.button.press"), 1.0F));
	}

	/** Usado para desfocar o mundo atrás desta tela. */
	@Override
	public float blurProgress() {
		return Anim.easeOutCubic((System.currentTimeMillis() - openedAt) / 220F);
	}
}
