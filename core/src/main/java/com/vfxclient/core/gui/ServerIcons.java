package com.vfxclient.core.gui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Ícone dos servidores (o PNG 64x64 em base64 que o servidor manda ao ser consultado), guardado
 * como textura. Usado pela lista de servidores e pelo HUD do servidor.
 */
public final class ServerIcons {

	private static final Map<String, Identifier> CACHE = new HashMap<String, Identifier>();

	private static final Map<String, String> SAVED = new HashMap<String, String>();
	private static final Map<String, Long> MISSES = new HashMap<String, Long>();

	private ServerIcons() {
	}

	/**
	 * @param server servidor
	 * @return a textura do ícone, ou {@code null} se o servidor não tem ícone (ainda não foi consultado)
	 */
	public static Identifier textureFor(ServerInfo server) {
		String data = server.getIcon();
		if (data == null) {
			data = savedIcon(server.address);
		}
		if (data == null) {
			return null;
		}

		String key = server.address + "#" + data.hashCode();
		if (CACHE.containsKey(key)) {
			return CACHE.get(key);
		}

		Identifier id = null;
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getMimeDecoder().decode(data)));
			if (image != null) {
				id = MinecraftClient.getInstance().getTextureManager()
						.registerDynamicTexture("vfx_server_icon", new NativeImageBackedTexture(image));
			}
		} catch (Exception ignored) {
			// Ícone inválido: quem chama usa as iniciais.
		}

		CACHE.put(key, id);
		return id;
	}

	/** Ícone do servidor com esse endereço na lista salva (para quem entra por conexão direta). */
	private static String savedIcon(String address) {
		if (SAVED.containsKey(address)) {
			return SAVED.get(address);
		}
		// Sem ícone na lista: tenta de novo só a cada 30 s (ler o arquivo todo quadro pesaria).
		long now = System.currentTimeMillis();
		Long last = MISSES.get(address);
		if (last != null && now - last < 30000L) {
			return null;
		}
		MISSES.put(address, now);

		String icon = null;
		ServerList list = new ServerList(MinecraftClient.getInstance());
		list.loadFile();
		for (int i = 0; i < list.size(); i++) {
			ServerInfo saved = list.get(i);
			if (address.equalsIgnoreCase(saved.address) && saved.getIcon() != null) {
				icon = saved.getIcon();
			}
		}
		// Só guarda se achou: o ícone pode chegar depois, quando a lista for consultada.
		if (icon != null) {
			SAVED.put(address, icon);
		}
		return icon;
	}
}
