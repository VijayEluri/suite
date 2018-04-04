package suite.editor;

import java.awt.Font;
import java.awt.Toolkit;

import suite.util.RunUtil;

public class FontUtil {

	public Font monoFont;
	public Font sansFont;

	public FontUtil() {
		String monoFontName;
		String sansFontName;

		if (!RunUtil.isUnix()) {
			System.setProperty("awt.useSystemAAFontSettings", "off");
			System.setProperty("swing.aatext", "false");
			monoFontName = "Courier New";
			sansFontName = "Arial";
		} else {
			monoFontName = "Akkurat-Mono";
			sansFontName = "Sans";
		}

		var size = Toolkit.getDefaultToolkit().getScreenSize().getWidth() > 1920 ? 24 : 12;

		monoFont = new Font(monoFontName, Font.PLAIN, size);
		sansFont = new Font(sansFontName, Font.PLAIN, size);
	}

}
