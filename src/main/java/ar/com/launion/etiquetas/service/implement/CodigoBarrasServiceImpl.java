package ar.com.launion.etiquetas.service.implement;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import ar.com.launion.etiquetas.service.CodigoBarrasService;

@Service
public class CodigoBarrasServiceImpl implements CodigoBarrasService {

	private static final Pattern VALIDO = Pattern.compile("^[0-9A-Za-z.\\-]{1,40}$");
	private static final String SVG_VACIO = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1 1\"/>";
	private static final Map<EncodeHintType, Object> SIN_MARGEN = Map.of(EncodeHintType.MARGIN, 0);

	@Override
	public boolean esValido(String codigo) {
		return codigo != null && VALIDO.matcher(codigo).matches();
	}

	@Override
	public String generarSvg(String codigo) {
		if (!esValido(codigo)) {
			return SVG_VACIO;
		}
		BitMatrix barras = codificar(codigo);
		int ancho = barras.getWidth();

		// Cada barra negra es un rectángulo de 1 de alto; el SVG se estira al tamaño de la etiqueta
		StringBuilder d = new StringBuilder();
		int x = 0;
		while (x < ancho) {
			if (barras.get(x, 0)) {
				int inicio = x;
				while (x < ancho && barras.get(x, 0)) {
					x++;
				}
				int grosor = x - inicio;
				d.append('M').append(inicio).append(" 0h").append(grosor).append("v1h-").append(grosor).append('z');
			} else {
				x++;
			}
		}
		return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + ancho + " 1\" preserveAspectRatio=\"none\" "
				+ "shape-rendering=\"crispEdges\"><path fill=\"#000\" d=\"" + d + "\"/></svg>";
	}

	private BitMatrix codificar(String codigo) {
		MultiFormatWriter escritor = new MultiFormatWriter();
		try {
			return escritor.encode(codigo, elegirFormato(codigo), 0, 1, SIN_MARGEN);
		} catch (WriterException | IllegalArgumentException e) {
			try {
				return escritor.encode(codigo, BarcodeFormat.CODE_128, 0, 1, SIN_MARGEN);
			} catch (WriterException ex) {
				throw new IllegalStateException("No se pudo generar el código de barras", ex);
			}
		}
	}

	private static BarcodeFormat elegirFormato(String codigo) {
		if (codigo.matches("\\d{13}") && digitoVerificadorOk(codigo)) {
			return BarcodeFormat.EAN_13; // los productos argentinos (779...)
		}
		if (codigo.matches("\\d{12}") && digitoVerificadorOk(codigo)) {
			return BarcodeFormat.UPC_A;
		}
		if (codigo.matches("\\d{8}") && digitoVerificadorOk(codigo)) {
			return BarcodeFormat.EAN_8;
		}
		return BarcodeFormat.CODE_128; // códigos internos o con letras
	}

	/** Dígito verificador GS1 (EAN / UPC): pesos 3 y 1 desde la derecha. */
	static boolean digitoVerificadorOk(String codigo) {
		int suma = 0;
		int peso = 3;
		for (int i = codigo.length() - 2; i >= 0; i--) {
			suma += (codigo.charAt(i) - '0') * peso;
			peso = 4 - peso;
		}
		return (10 - suma % 10) % 10 == codigo.charAt(codigo.length() - 1) - '0';
	}

}
