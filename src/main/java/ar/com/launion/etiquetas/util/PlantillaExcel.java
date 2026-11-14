package ar.com.launion.etiquetas.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Genera la plantilla para descargar: hoja "Productos" vacía (para completar)
 * y hoja "Ejemplo" con tres filas de muestra.
 */
public final class PlantillaExcel {

	private static final String[] TITULOS = { "Nombre del producto", "Precio", "Código de barras" };

	private static final Object[][] EJEMPLOS = {
			{ "Yerba Mate Playadito 1 kg", 4999d, "7790387000017" },
			{ "Aceite de Girasol Natura 1,5 L", 3849.90d, "7790060023455" },
			{ "Pan francés x kg", 2500d, "2000123" } };

	private PlantillaExcel() {
	}

	public static byte[] generar() throws IOException {
		try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

			XSSFFont fuenteTitulo = libro.createFont();
			fuenteTitulo.setBold(true);
			fuenteTitulo.setFontHeightInPoints((short) 12);
			fuenteTitulo.setColor(IndexedColors.WHITE.getIndex());

			XSSFCellStyle estiloTitulo = libro.createCellStyle();
			estiloTitulo.setFont(fuenteTitulo);
			estiloTitulo.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 0xED, (byte) 0x67, (byte) 0x2B }, null));
			estiloTitulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			estiloTitulo.setAlignment(HorizontalAlignment.CENTER);
			estiloTitulo.setVerticalAlignment(VerticalAlignment.CENTER);
			estiloTitulo.setBorderBottom(BorderStyle.THIN);

			// El código va como TEXTO para que Excel no lo convierta en 7,79E+12
			XSSFCellStyle estiloCodigo = libro.createCellStyle();
			estiloCodigo.setDataFormat(libro.createDataFormat().getFormat("@"));

			XSSFCellStyle estiloPrecio = libro.createCellStyle();
			estiloPrecio.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

			crearHoja(libro, "Productos", estiloTitulo, estiloPrecio, estiloCodigo, false);
			crearHoja(libro, "Ejemplo", estiloTitulo, estiloPrecio, estiloCodigo, true);
			libro.setActiveSheet(0);

			libro.write(salida);
			return salida.toByteArray();
		}
	}

	private static void crearHoja(XSSFWorkbook libro, String nombre, XSSFCellStyle titulo, XSSFCellStyle precio,
			XSSFCellStyle codigo, boolean conEjemplos) {
		XSSFSheet hoja = libro.createSheet(nombre);
		hoja.setColumnWidth(0, 44 * 256);
		hoja.setColumnWidth(1, 16 * 256);
		hoja.setColumnWidth(2, 24 * 256);
		hoja.setDefaultColumnStyle(1, precio);
		hoja.setDefaultColumnStyle(2, codigo);
		hoja.createFreezePane(0, 1);

		Row encabezado = hoja.createRow(0);
		encabezado.setHeightInPoints(26);
		for (int i = 0; i < TITULOS.length; i++) {
			encabezado.createCell(i).setCellValue(TITULOS[i]);
			encabezado.getCell(i).setCellStyle(titulo);
		}

		if (!conEjemplos) {
			return;
		}
		for (int r = 0; r < EJEMPLOS.length; r++) {
			Row fila = hoja.createRow(r + 1);
			fila.createCell(0).setCellValue((String) EJEMPLOS[r][0]);
			fila.createCell(1).setCellValue((Double) EJEMPLOS[r][1]);
			fila.getCell(1).setCellStyle(precio);
			fila.createCell(2).setCellValue((String) EJEMPLOS[r][2]);
			fila.getCell(2).setCellStyle(codigo);
		}
	}

}
