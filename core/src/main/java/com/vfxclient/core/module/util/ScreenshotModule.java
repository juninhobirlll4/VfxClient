package com.vfxclient.core.module.util;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import javax.imageio.ImageIO;

import com.vfxclient.core.VfxCore;
import com.vfxclient.core.module.Category;
import com.vfxclient.core.module.Module;
import com.vfxclient.core.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;

/**
 * Prints mais práticos: ao tirar um print (F2), a imagem também vai para a área de
 * transferência, pronta para colar no Discord ou no WhatsApp. O arquivo continua salvo
 * normalmente na pasta {@code screenshots}. Nada é enviado para a internet.
 */
public class ScreenshotModule extends Module {

	private final BooleanSetting clipboard = add(new BooleanSetting("clipboard", "Copiar o print para a área de transferência", true));
	private final BooleanSetting notify = add(new BooleanSetting("notify", "Avisar no chat", true));

	private long lastSeen = -1;
	private int tickCounter;

	public ScreenshotModule() {
		super("screenshot", "Prints", "Copia o print (F2) para colar", Category.UTILITY, true);
	}

	@Override
	public void onTick(MinecraftClient client) {
		if (!clipboard.get() || ++tickCounter % 10 != 0) {
			return;
		}
		File newest = newest(new File(client.runDirectory, "screenshots"));
		long modified = newest == null ? 0 : newest.lastModified();
		if (lastSeen < 0) {
			lastSeen = modified; // não copia prints antigos ao abrir o jogo
			return;
		}
		if (newest != null && modified > lastSeen) {
			lastSeen = modified;
			copyAsync(client, newest);
		}
	}

	private static File newest(File dir) {
		File[] files = dir.listFiles();
		File best = null;
		if (files != null) {
			for (File file : files) {
				if (file.getName().endsWith(".png") && (best == null || file.lastModified() > best.lastModified())) {
					best = file;
				}
			}
		}
		return best;
	}

	private void copyAsync(final MinecraftClient client, final File file) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(300); // espera o jogo terminar de gravar o PNG
					final Image image = ImageIO.read(file);
					if (image == null) {
						return;
					}
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(image), null);
					VfxCore.LOGGER.info("Print copiado para a área de transferência: {}", file.getName());
					client.submit(new Runnable() {
						@Override
						public void run() {
							if (notify.get() && client.player != null) {
								client.player.sendMessage(new LiteralText("§d[VfxClient] §7Print copiado para a área de transferência."));
							}
						}
					});
				} catch (Exception e) {
					VfxCore.LOGGER.warn("Não deu para copiar o print: {}", e.toString());
				}
			}
		}, "VfxClient-Print");
		thread.setDaemon(true);
		thread.start();
	}

	/** Imagem na área de transferência. */
	private static final class ImageSelection implements Transferable {
		private final Image image;

		ImageSelection(Image image) {
			this.image = image;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] {DataFlavor.imageFlavor};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.imageFlavor.equals(flavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (!isDataFlavorSupported(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
			return image;
		}
	}
}
