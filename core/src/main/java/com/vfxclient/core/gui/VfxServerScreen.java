package com.vfxclient.core.gui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vfxclient.core.gui.font.Fonts;
import com.vfxclient.core.mixin.MultiplayerScreenAccessor;
import com.vfxclient.core.mixin.ServerListWidgetAccessor;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.LanServerEntry;
import net.minecraft.client.network.LanServerQueryManager;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Tela de servidores (Multijogador) no visual do VfxClient: servidores em cartões com ping em
 * número e colorido, favoritos (★) no topo, busca e abas.
 *
 * <p>Estende a tela do jogo e usa as mesmas ações dela (entrar, conexão direta, adicionar,
 * editar, excluir, atualizar); o desenho e a lista são nossos.</p>
 */
public class VfxServerScreen extends MultiplayerScreen {

	// Ids dos botões da tela original (as ações do jogo leem deles).
	private static final int BACK = 0;
	private static final int DELETE = 2;
	private static final int ADD = 3;
	private static final int DIRECT = 4;
	private static final int EDIT = 7;
	private static final int REFRESH = 8;

	// Ações que só existem aqui.
	private static final int JOIN = 100;
	private static final int OPTIONS = 101;
	private static final int VFX_MENU = 102;
	private static final int TAB_ALL = 110;
	private static final int TAB_FAVORITES = 111;
	private static final int TAB_LAN = 112;
	private static final int ROW = 1000;
	private static final int STAR = 2000;

	private static final float PANEL_W = 1180;
	private static final float ROW_H = 106;
	private static final float ROW_GAP = 10;
	private static final long DOUBLE_CLICK_MS = 350L;

	/** Mesma ideia da tela original: o ping de cada servidor roda em segundo plano. */
	private static final ExecutorService PINGER = Executors.newFixedThreadPool(5, runnable -> {
		Thread thread = new Thread(runnable, "VfxClient Server Ping");
		thread.setDaemon(true);
		return thread;
	});

	private final List<ClickArea> areas = new ArrayList<ClickArea>();
	private final Map<String, Identifier> icons = new HashMap<String, Identifier>();
	private final long openedAt = System.currentTimeMillis();
	private Canvas canvas;
	private int tab = TAB_ALL;
	private String search = "";
	private float scroll;
	private float scrollTarget;
	private int selected = -1;
	private int lastClicked = -1;
	private long lastClickAt;

	/** Uma linha da lista (servidor salvo ou servidor da rede local). */
	private static final class Entry {
		final int index;
		final ServerInfo server;
		final LanServerQueryManager.LanServerInfo lan;

		Entry(int index, ServerInfo server, LanServerQueryManager.LanServerInfo lan) {
			this.index = index;
			this.server = server;
			this.lan = lan;
		}
	}

	/** @param parent tela para onde "Voltar" leva */
	public VfxServerScreen(Screen parent) {
		super(parent);
	}

	// ------------------------------------------------------------------
	// Lista
	// ------------------------------------------------------------------

	/** @return linhas da aba atual, com a busca aplicada e os favoritos primeiro */
	private List<Entry> entries() {
		List<Entry> result = new ArrayList<Entry>();
		String query = search.toLowerCase(Locale.ROOT);

		if (tab == TAB_LAN) {
			List<LanServerEntry> lan = ((ServerListWidgetAccessor) widget()).vfx$getLanServers();
			int offset = getServerList().size() + 1;
			for (int i = 0; i < lan.size(); i++) {
				LanServerQueryManager.LanServerInfo info = lan.get(i).getServer();
				if (matches(query, info.getMotd(), info.getAddressPort())) {
					result.add(new Entry(offset + i, null, info));
				}
			}
			return result;
		}

		ServerList list = getServerList();
		List<Entry> others = new ArrayList<Entry>();
		for (int i = 0; i < list.size(); i++) {
			ServerInfo server = list.get(i);
			boolean favorite = ServerFavorites.contains(server.address);
			if (tab == TAB_FAVORITES && !favorite) {
				continue;
			}
			if (!matches(query, server.name, server.address)) {
				continue;
			}
			(favorite ? result : others).add(new Entry(i, server, null));
		}
		result.addAll(others);
		return result;
	}

	private static boolean matches(String query, String name, String address) {
		if (query.isEmpty()) {
			return true;
		}
		return Fonts.stripFormatting(name).toLowerCase(Locale.ROOT).contains(query)
				|| String.valueOf(address).toLowerCase(Locale.ROOT).contains(query);
	}

	private MultiplayerServerListWidget widget() {
		return ((MultiplayerScreenAccessor) this).vfx$getServerListWidget();
	}

	// ------------------------------------------------------------------
	// Desenho
	// ------------------------------------------------------------------

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		areas.clear();
		drawBackground();

		canvas = Canvas.begin(width, height);
		float mx = canvas.toCanvas(mouseX);
		float my = canvas.toCanvas(mouseY);

		drawTopBar(mx, my);
		float panelX = canvas.centerX() - PANEL_W / 2F;
		drawHeader(panelX, mx, my);
		drawList(panelX, mx, my);
		drawActions(mx, my);

		canvas.end();
		Ui.restore();
	}

	private void drawBackground() {
		if (client.world != null) {
			fillGradient(0, 0, width, height, 0xB0050309, 0xE0050309);
		} else {
			// Fora do jogo: fundo escuro com um brilho roxo, no lugar da terra do Minecraft.
			fillGradient(0, 0, width, height, 0xFF0F0A1A, 0xFF07050C);
		}
	}

	private void drawTopBar(float mx, float my) {
		Ui.brand(30, 24);

		float x = canvas.width() - 28 - 58;
		int[] actions = {BACK, VFX_MENU, OPTIONS};
		Ui.Icon[] icons = {Ui.Icon.CLOSE, Ui.Icon.MENU, Ui.Icon.SETTINGS};
		for (int i = 0; i < actions.length; i++) {
			Ui.iconButton(icons[i], x, 24, Ui.inside(mx, my, x, 24, 58, 58));
			areas.add(new ClickArea(actions[i], x, 24, 58, 58));
			x -= 70;
		}
	}

	private void drawHeader(float panelX, float mx, float my) {
		float top = 118;
		Fonts.drawSpaced(Fonts.TITLE, "MULTIJOGADOR", panelX, top, 38, 0.1F, 0xFFFFFFFF);
		Fonts.draw(Fonts.TEXT, "Clique duas vezes para entrar  ·  clique na estrela para favoritar", panelX, top + 52, 17, Ui.MUTED);

		// Busca (é só digitar).
		float searchX = panelX + PANEL_W - 340;
		boolean typing = !search.isEmpty();
		Render2D.outlinedRect(searchX, top + 10, 340, 50, 8, typing ? 0xFFB48CFF : 0x59FFFFFF, 0xD90C0C0F);
		Render2D.ring(searchX + 26, top + 33, 8F, 2.5F, 0xFF9A9AA6);
		Render2D.rawRect(searchX + 31, top + 39, 6, 3, 0xFF9A9AA6);
		boolean caret = typing && (System.currentTimeMillis() / 500) % 2 == 0;
		String shown = typing ? search + (caret ? "_" : "") : "Buscar servidor...";
		Fonts.draw(Fonts.TEXT, shown, searchX + 46, top + 23, 18, typing ? 0xFFFFFFFF : 0xFF9A9AA6);

		// Abas.
		float tabX = panelX;
		float tabY = top + 90;
		String[] labels = {"Meus servidores (" + getServerList().size() + ")", "Favoritos", "Na rede local"};
		int[] ids = {TAB_ALL, TAB_FAVORITES, TAB_LAN};
		for (int i = 0; i < labels.length; i++) {
			float w = Fonts.width(Fonts.TEXT_BOLD, labels[i], 16) + 36;
			boolean on = tab == ids[i];
			boolean hover = Ui.inside(mx, my, tabX, tabY, w, 41);
			int border = on ? Ui.PURPLE_LIGHT : (hover ? 0x80FFFFFF : 0x38FFFFFF);
			Render2D.outlinedRect(tabX, tabY, w, 41, 8, border, on ? 0x407B2FF7 : 0xB30F0D14);
			Fonts.draw(Fonts.TEXT_BOLD, labels[i], tabX + 18, tabY + 12, 16, on ? 0xFFFFFFFF : 0xFFC4C4CC);
			areas.add(new ClickArea(ids[i], tabX, tabY, w, 41));
			tabX += w + 8;
		}
	}

	private void drawList(float panelX, float mx, float my) {
		float top = 263;
		float bottom = canvas.height() - 40 - 54 - 24;
		float visible = bottom - top;
		List<Entry> entries = entries();

		float content = entries.size() * (ROW_H + ROW_GAP) - ROW_GAP;
		scrollTarget = Math.max(0, Math.min(scrollTarget, Math.max(0, content - visible)));
		scroll += (scrollTarget - scroll) * 0.3F;

		if (entries.isEmpty()) {
			String text = tab == TAB_LAN ? "Procurando mundos abertos na rede local..." : "Nenhum servidor aqui ainda. Use + ADICIONAR.";
			Fonts.drawCentered(Fonts.TEXT, text, canvas.centerX(), top + 40, 19, Ui.MUTED);
			return;
		}

		scissor(panelX - 6, top - 6, PANEL_W + 12, visible + 12);
		float appear = Anim.easeOutCubic((System.currentTimeMillis() - openedAt) / 300F);
		for (int i = 0; i < entries.size(); i++) {
			float y = top + i * (ROW_H + ROW_GAP) - scroll + (1F - appear) * (12 + i * 6);
			if (y + ROW_H < top - 6 || y > bottom + 6) {
				continue;
			}
			drawRow(entries.get(i), panelX, y, mx, my, Ui.inside(mx, my, panelX, Math.max(y, top), PANEL_W, ROW_H));
		}
		endScissor();
	}

	private void drawRow(Entry entry, float x, float y, float mx, float my, boolean hover) {
		boolean isSelected = entry.index == selected;
		boolean offline = entry.server != null && entry.server.ping == -1L;

		int border = isSelected ? Ui.PURPLE_LIGHT : (hover ? 0x40FFFFFF : 0x1FFFFFFF);
		int fill = isSelected ? 0x8C281650 : 0xC70E0C14;
		if (isSelected) {
			Render2D.roundedRect(x - 3, y - 3, PANEL_W + 6, ROW_H + 6, 14, 0x2E7B2FF7);
		}
		Render2D.outlinedRect(x, y, PANEL_W, ROW_H, 12, border, fill);
		areas.add(new ClickArea(ROW + entry.index, x, y, PANEL_W, ROW_H));

		if (entry.server != null) {
			startPing(entry.server);
			drawIcon(entry.server, x + 18, y + 15, offline);
			drawServerText(entry, x, y, offline, mx, my);
			drawServerStatus(entry.server, x + PANEL_W - 18, y);
		} else {
			drawInitials("LAN", 0xFF2B4B6B, x + 18, y + 15);
			Fonts.draw(Fonts.TEXT_BOLD, Fonts.stripFormatting(entry.lan.getMotd()), x + 113, y + 24, 21, 0xFFFFFFFF);
			Fonts.draw(Fonts.TEXT_BOLD, entry.lan.getAddressPort(), x + 113, y + 58, 17, 0xFF9AD3FF);
		}
	}

	private void drawServerText(Entry entry, float x, float y, boolean offline, float mx, float my) {
		ServerInfo server = entry.server;
		float nameX = x + 113;

		// Estrela de favorito (clicável).
		boolean favorite = ServerFavorites.contains(server.address);
		boolean starHover = Ui.inside(mx, my, nameX - 4, y + 18, 30, 30);
		if (favorite || starHover) {
			drawStar(nameX + 11, y + 34, 10F, favorite ? Ui.YELLOW : 0x99F5C542);
			nameX += 30;
		}
		areas.add(0, new ClickArea(STAR + entry.index, x + 109, y + 18, 30, 30));

		int nameColor = offline ? 0xFFB0B0B8 : 0xFFFFFFFF;
		Fonts.draw(Fonts.TEXT_BOLD, Fonts.stripFormatting(server.name), nameX, y + 22, 21, nameColor);

		String motd;
		if (server.ping == -2L) {
			motd = "§7Conectando...";
		} else if (offline) {
			motd = "§7Não foi possível conectar";
		} else {
			motd = server.label == null ? "" : server.label.replace("\n", "  ");
		}
		Fonts.drawFormatted(Fonts.TEXT_BOLD, motd, x + 113, y + 58, 17, 0xFFB48CFF);
	}

	/** Jogadores e ping no lado direito do cartão. */
	private void drawServerStatus(ServerInfo server, float right, float y) {
		if (server.ping == -1L) {
			Fonts.draw(Fonts.TEXT, "offline", right - Fonts.width(Fonts.TEXT, "offline", 17), y + 26, 17, 0xFFFF8A8D);
			Ui.drawCross(right - 8, y + 66, 7F, 2F, 0xFFFF6B6B);
			return;
		}
		if (server.ping == -2L) {
			Fonts.draw(Fonts.TEXT, "...", right - Fonts.width(Fonts.TEXT, "...", 18), y + 26, 18, Ui.MUTED);
			return;
		}

		String[] players = Fonts.stripFormatting(server.playerCountLabel).split("/");
		if (players.length == 2) {
			String max = " / " + players[1].trim();
			float maxW = Fonts.width(Fonts.TEXT, max, 17);
			Fonts.draw(Fonts.TEXT, max, right - maxW, y + 26, 17, 0xFFD8D8DE);
			String online = players[0].trim();
			Fonts.draw(Fonts.TEXT_BOLD, online, right - maxW - Fonts.width(Fonts.TEXT_BOLD, online, 17), y + 26, 17, 0xFFFFFFFF);
		}

		int ping = (int) server.ping;
		int color = ping <= 80 ? Ui.GREEN : (ping <= 150 ? Ui.YELLOW : Ui.RED);
		int level = ping <= 80 ? 5 : (ping <= 150 ? 3 : (ping <= 300 ? 2 : 1));
		float barsX = right - 28;
		for (int i = 0; i < 5; i++) {
			float h = 6 + i * 3;
			int barColor = i < level ? color : Theme.withAlpha(color, 0.25F);
			Render2D.rawRect(barsX + i * 6, y + 74 - h, 4, h, barColor);
		}
		String text = ping + " ms";
		Fonts.draw(Fonts.TEXT_BOLD, text, barsX - 10 - Fonts.width(Fonts.TEXT_BOLD, text, 18), y + 56, 18, color);
	}

	private void drawIcon(ServerInfo server, float x, float y, boolean offline) {
		Identifier texture = icon(server);
		if (texture == null) {
			String initials = initials(server.name);
			int color = offline ? 0xFF333333 : 0xFF000000 | (Math.abs(server.address.hashCode()) & 0x7F7F7F) | 0x303030;
			drawInitials(offline ? "?" : initials, color, x, y);
			return;
		}
		Render2D.roundedRect(x, y, 76, 76, 10, 0xFF000000);
		client.getTextureManager().bindTexture(texture);
		GlStateManager.color(1F, 1F, 1F, offline ? 0.5F : 1F);
		GlStateManager.enableBlend();
		DrawableHelper.drawTexture((int) x, (int) y, 0, 0, 76, 76, 76, 76);
		Ui.restore();
	}

	private static void drawInitials(String text, int color, float x, float y) {
		Render2D.roundedRect(x, y, 76, 76, 10, color);
		Fonts.drawCentered(Fonts.TITLE, text, x + 38, y + 38 - Fonts.lineHeight(Fonts.TITLE, 26) / 2F, 26, 0xFFFFFFFF);
	}

	private static String initials(String name) {
		String clean = Fonts.stripFormatting(name).replaceAll("[^\\p{L}\\p{N} ]", " ").trim();
		if (clean.isEmpty()) {
			return "?";
		}
		String[] words = clean.split("\\s+");
		if (words.length >= 2) {
			return (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase(Locale.ROOT);
		}
		return clean.substring(0, Math.min(2, clean.length())).toUpperCase(Locale.ROOT);
	}

	/** Estrela de 5 pontas desenhada com triângulos. */
	private static void drawStar(float cx, float cy, float r, int color) {
		float inner = r * 0.45F;
		for (int i = 0; i < 5; i++) {
			double a = -Math.PI / 2 + i * 2 * Math.PI / 5;
			double left = a - Math.PI / 5;
			double right = a + Math.PI / 5;
			Render2D.triangle(cx, cy,
					cx + (float) Math.cos(left) * inner, cy + (float) Math.sin(left) * inner,
					cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, color);
			Render2D.triangle(cx, cy,
					cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r,
					cx + (float) Math.cos(right) * inner, cy + (float) Math.sin(right) * inner, color);
		}
	}

	private void drawActions(float mx, float my) {
		boolean hasSelection = selected >= 0;
		boolean savedSelected = hasSelection && selected < getServerList().size();
		String[] labels = {"ENTRAR", "CONEXÃO DIRETA", "+ ADICIONAR", "EDITAR", "EXCLUIR", "ATUALIZAR", "VOLTAR"};
		int[] actions = {JOIN, DIRECT, ADD, EDIT, DELETE, REFRESH, BACK};

		float[] widths = new float[labels.length];
		float total = 0;
		for (int i = 0; i < labels.length; i++) {
			widths[i] = Fonts.width(Fonts.BUTTON, labels[i], 17, 0.05F) + (i == 0 ? 80 : 52);
			total += widths[i] + 12;
		}

		float x = canvas.centerX() - (total - 12) / 2F;
		float y = canvas.height() - 40 - 54;
		for (int i = 0; i < labels.length; i++) {
			Ui.Style style = i == 0 ? Ui.Style.PRIMARY : Ui.Style.NORMAL;
			boolean needsSelection = actions[i] == JOIN ? !hasSelection : (actions[i] == EDIT || actions[i] == DELETE) && !savedSelected;
			if (needsSelection) {
				style = Ui.Style.DISABLED;
			}
			Ui.button(labels[i], x, y, widths[i], 54, 17, style, !needsSelection && Ui.inside(mx, my, x, y, widths[i], 54));
			if (!needsSelection) {
				areas.add(new ClickArea(actions[i], x, y, widths[i], 54));
			}
			x += widths[i] + 12;
		}
	}

	// ------------------------------------------------------------------
	// Ping e ícones
	// ------------------------------------------------------------------

	/** Mesmo início de ping da tela original (que só pinga as linhas que ela desenha). */
	private void startPing(final ServerInfo server) {
		if (server.online) {
			return;
		}
		server.online = true;
		server.ping = -2L;
		server.label = "";
		server.playerCountLabel = "";
		PINGER.submit(() -> {
			try {
				getServerListPinger().add(server);
			} catch (Exception e) {
				server.ping = -1L;
				server.label = "§4Não foi possível conectar";
			}
		});
	}

	/** Ícone que o servidor manda (PNG 64x64 em base64), guardado como textura. */
	private Identifier icon(ServerInfo server) {
		String data = server.getIcon();
		if (data == null) {
			return null;
		}

		String key = server.address + "#" + data.hashCode();
		if (icons.containsKey(key)) {
			return icons.get(key);
		}

		Identifier id = null;
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getMimeDecoder().decode(data)));
			if (image != null) {
				id = client.getTextureManager().registerDynamicTexture("vfx_server_icon", new NativeImageBackedTexture(image));
			}
		} catch (Exception ignored) {
			// Ícone inválido: fica com as iniciais.
		}

		icons.put(key, id);
		return id;
	}

	// ------------------------------------------------------------------
	// Entrada
	// ------------------------------------------------------------------

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (button != 0 || canvas == null) {
			return;
		}
		float mx = canvas.toCanvas(mouseX);
		float my = canvas.toCanvas(mouseY);
		for (ClickArea area : areas) {
			if (area.contains(mx, my)) {
				click(area.action());
				return;
			}
		}
	}

	private void click(int action) {
		if (action >= STAR) {
			ServerInfo server = getServerList().get(action - STAR);
			ServerFavorites.toggle(server.address);
			return;
		}

		if (action >= ROW) {
			int index = action - ROW;
			select(index);
			long now = System.currentTimeMillis();
			if (index == lastClicked && now - lastClickAt < DOUBLE_CLICK_MS) {
				join();
			}
			lastClicked = index;
			lastClickAt = now;
			return;
		}

		playClickSound();
		switch (action) {
			case TAB_ALL:
			case TAB_FAVORITES:
			case TAB_LAN:
				tab = action;
				scrollTarget = 0;
				select(-1);
				break;
			case JOIN:
				join();
				break;
			case OPTIONS:
				client.setScreen(new SettingsScreen(this, client.options));
				break;
			case VFX_MENU:
				client.setScreen(new ClickGuiScreen());
				break;
			default:
				pressVanillaButton(action);
				break;
		}
	}

	private void select(int index) {
		selected = index;
		selectEntry(index);
	}

	private void join() {
		if (selected >= 0) {
			selectEntry(selected);
			connect();
		}
	}

	/** Usa a ação original do jogo (adicionar, editar, excluir, conexão direta, atualizar, voltar). */
	private void pressVanillaButton(int id) {
		if (selected >= 0) {
			selectEntry(selected);
		}
		for (ButtonWidget widget : buttons) {
			if (widget.id == id) {
				buttonClicked(widget);
				return;
			}
		}
	}

	private void playClickSound() {
		client.getSoundManager().play(PositionedSoundInstance.master(new Identifier("gui.button.press"), 1.0F));
	}

	@Override
	public void handleMouse() {
		// Não chama a lista original: a rolagem é nossa.
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0) {
			scrollTarget -= Math.signum(wheel) * (ROW_H + ROW_GAP);
		}
		superHandleMouse();
	}

	/** Cliques e arrastos de tela (sem a rolagem da lista original). */
	private void superHandleMouse() {
		int x = Mouse.getEventX() * width / client.width;
		int y = height - Mouse.getEventY() * height / client.height - 1;
		int button = Mouse.getEventButton();
		if (Mouse.getEventButtonState()) {
			mouseClicked(x, y, button);
		}
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			if (!search.isEmpty()) {
				search = "";
			} else {
				pressVanillaButton(BACK);
			}
		} else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
			join();
		} else if (keyCode == Keyboard.KEY_F5) {
			pressVanillaButton(REFRESH);
		} else if (keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN) {
			moveSelection(keyCode == Keyboard.KEY_UP ? -1 : 1);
		} else if (keyCode == Keyboard.KEY_BACK) {
			if (!search.isEmpty()) {
				search = search.substring(0, search.length() - 1);
			}
		} else if (character >= 32 && character != 127 && character != '§' && search.length() < 32) {
			search += character;
			scrollTarget = 0;
		}
	}

	private void moveSelection(int direction) {
		List<Entry> entries = entries();
		if (entries.isEmpty()) {
			return;
		}
		int position = 0;
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).index == selected) {
				position = i + direction;
			}
		}
		position = Math.max(0, Math.min(entries.size() - 1, position));
		select(entries.get(position).index);
	}

	// ------------------------------------------------------------------
	// Recorte da lista
	// ------------------------------------------------------------------

	private void scissor(float x, float y, float w, float h) {
		float unit = width / canvas.width();
		Render2D.scissor((int) (x * unit), (int) (y * unit), (int) Math.ceil(w * unit), (int) Math.ceil(h * unit));
	}

	private static void endScissor() {
		Render2D.endScissor();
	}
}
