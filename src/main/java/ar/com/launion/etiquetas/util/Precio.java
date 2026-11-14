package ar.com.launion.etiquetas.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Lectura y formato de precios al estilo argentino: $ 1.299,50
 */
public final class Precio {

	public static final BigDecimal MAXIMO = new BigDecimal("99999999.99");

	private Precio() {
	}

	/**
	 * Acepta lo que la gente escribe: 1299 · 1.299 · 1299,5 · 1.299,50 · $ 1.299 · 1299.50
	 * Devuelve null si no es un número.
	 */
	public static BigDecimal parsear(String texto) {
		if (texto == null) {
			return null;
		}
		String t = texto.replace("$", "").replace(" ", "").replace(" ", "").trim();
		if (t.isEmpty() || !t.matches("[0-9.,]+")) {
			return null;
		}

		int coma = t.lastIndexOf(',');
		int punto = t.lastIndexOf('.');
		String normal;
		if (coma >= 0 && punto >= 0) {
			// Tiene los dos: el último que aparece es el separador decimal
			char decimal = coma > punto ? ',' : '.';
			char miles = decimal == ',' ? '.' : ',';
			normal = t.replace(String.valueOf(miles), "").replace(decimal, '.');
		} else if (coma >= 0) {
			// Solo comas: una sola es decimal (1299,50); varias son de miles
			normal = t.indexOf(',') != coma ? t.replace(",", "") : t.replace(',', '.');
		} else if (punto >= 0) {
			// Solo puntos: "1.299" o "1.299.999" son miles; "12.5" es decimal
			boolean sonMiles = t.indexOf('.') != punto || t.length() - punto - 1 == 3;
			normal = sonMiles ? t.replace(".", "") : t;
		} else {
			normal = t;
		}

		try {
			return new BigDecimal(normal).setScale(2, RoundingMode.HALF_UP);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static boolean esValido(BigDecimal precio) {
		return precio != null && precio.signum() > 0 && precio.compareTo(MAXIMO) <= 0;
	}

	/** Parte entera con puntos de miles: 1.299 */
	public static String entero(BigDecimal precio) {
		if (precio == null) {
			return "0";
		}
		DecimalFormat formato = new DecimalFormat("#,##0", simbolos());
		formato.setRoundingMode(RoundingMode.DOWN);
		return formato.format(precio);
	}

	/** Centavos en dos cifras, o vacío si son cero: "50" / "" */
	public static String centavos(BigDecimal precio) {
		if (precio == null) {
			return "";
		}
		BigDecimal resto = precio.abs().subtract(precio.abs().setScale(0, RoundingMode.DOWN));
		int cent = resto.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
		return cent == 0 ? "" : String.format("%02d", cent);
	}

	/** Para mostrar en un campo editable: 1.299 o 1.299,50 */
	public static String editable(BigDecimal precio) {
		if (precio == null) {
			return "";
		}
		String cent = centavos(precio);
		return cent.isEmpty() ? entero(precio) : entero(precio) + "," + cent;
	}

	/** Para listados: $ 1.299,50 */
	public static String texto(BigDecimal precio) {
		return "$ " + editable(precio);
	}

	private static DecimalFormatSymbols simbolos() {
		DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
		s.setGroupingSeparator('.');
		s.setDecimalSeparator(',');
		return s;
	}

}
