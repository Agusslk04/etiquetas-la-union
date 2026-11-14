package ar.com.launion.etiquetas.util;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Lee los productos de un Excel (.xlsx o .xls).
 *
 * Busca en las primeras filas los títulos "Nombre / Producto", "Precio" y "Código";
 * si no los encuentra, usa las columnas A (nombre), B (precio) y C (código).
 */
public final class LectorExcel {

	public static final int MAX_PRODUCTOS = 3000;

	/** Una fila del Excel. numero = número de fila como lo ve el usuario en Excel. */
	public record Fila(int numero, String nombre, String precioTexto, BigDecimal precioNumero, String codigo) {

		boolean vacia() {
			return nombre.isEmpty() && precioTexto.isEmpty() && precioNumero == null && codigo.isEmpty();
		}
	}

	/** El archivo supera el máximo de productos permitido. */
	public static class DemasiadosProductos extends RuntimeException {

		private static final long serialVersionUID = 1L;

		DemasiadosProductos() {
			super("El archivo tiene más de " + MAX_PRODUCTOS + " productos. Dividilo en varios archivos.");
		}
	}

	private LectorExcel() {
	}

	public static List<Fila> leer(InputStream entrada) throws IOException {
		try (Workbook libro = WorkbookFactory.create(entrada)) {
			Sheet hoja = primeraHojaConDatos(libro);
			if (hoja == null) {
				return List.of();
			}
			DataFormatter formato = new DataFormatter(Locale.ROOT);
			FormulaEvaluator evaluador = libro.getCreationHelper().createFormulaEvaluator();

			// 1) ¿Dónde están los títulos?
			int colNombre = 0, colPrecio = 1, colCodigo = 2;
			int primeraFila = hoja.getFirstRowNum();
			boolean conTitulos = false;
			int hasta = Math.min(hoja.getLastRowNum(), hoja.getFirstRowNum() + 10);
			for (int r = hoja.getFirstRowNum(); r <= hasta; r++) {
				int[] columnas = detectarTitulos(hoja.getRow(r), formato);
				if (columnas != null) {
					colNombre = columnas[0];
					colPrecio = columnas[1];
					colCodigo = columnas[2];
					primeraFila = r + 1;
					conTitulos = true;
					break;
				}
			}

			// 2) Leer los productos
			List<Fila> filas = new ArrayList<>();
			for (int r = primeraFila; r <= hoja.getLastRowNum(); r++) {
				Row row = hoja.getRow(r);
				if (row == null) {
					continue;
				}
				Cell celdaPrecio = row.getCell(colPrecio);
				BigDecimal precioNumero = numero(celdaPrecio, evaluador);
				Fila fila = new Fila(r + 1,
						texto(row.getCell(colNombre), formato, evaluador),
						precioNumero == null ? texto(celdaPrecio, formato, evaluador) : "",
						precioNumero,
						codigo(row.getCell(colCodigo), formato, evaluador));
				if (fila.vacia()) {
					continue;
				}
				// Sin títulos reconocibles: si la primera fila no tiene un precio, son títulos con otro nombre
				if (!conTitulos && filas.isEmpty() && r == primeraFila && precioNumero == null
						&& Precio.parsear(fila.precioTexto()) == null) {
					continue;
				}
				if (filas.size() >= MAX_PRODUCTOS) {
					throw new DemasiadosProductos();
				}
				filas.add(fila);
			}
			return filas;
		}
	}

	// ===== Títulos =====

	private static int[] detectarTitulos(Row row, DataFormatter formato) {
		if (row == null) {
			return null;
		}
		int nombre = -1, precio = -1, codigo = -1;
		for (Cell celda : row) {
			String t = normalizar(formato.formatCellValue(celda));
			if (t.isEmpty()) {
				continue;
			}
			int col = celda.getColumnIndex();
			if (codigo < 0 && (t.contains("cod") || t.contains("ean") || t.contains("barra"))) {
				codigo = col;
			} else if (precio < 0 && (t.contains("precio") || t.contains("importe") || t.contains("pvp") || t.equals("valor"))) {
				precio = col;
			} else if (nombre < 0 && (t.contains("nombre") || t.contains("producto") || t.contains("descrip")
					|| t.contains("articulo") || t.contains("detalle"))) {
				nombre = col;
			}
		}
		return nombre >= 0 && precio >= 0 && codigo >= 0 ? new int[] { nombre, precio, codigo } : null;
	}

	private static String normalizar(String texto) {
		String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return sinAcentos.toLowerCase(Locale.ROOT).trim();
	}

	// ===== Celdas =====

	private static CellType tipo(Cell celda) {
		return celda.getCellType() == CellType.FORMULA ? celda.getCachedFormulaResultType() : celda.getCellType();
	}

	private static String texto(Cell celda, DataFormatter formato, FormulaEvaluator evaluador) {
		if (celda == null) {
			return "";
		}
		return formato.formatCellValue(celda, evaluador).replace(' ', ' ').trim();
	}

	/** El precio, si la celda es un número de Excel; si es texto, null. */
	private static BigDecimal numero(Cell celda, FormulaEvaluator evaluador) {
		if (celda == null || tipo(celda) != CellType.NUMERIC || DateUtil.isCellDateFormatted(celda)) {
			return null;
		}
		return BigDecimal.valueOf(celda.getNumericCellValue());
	}

	/**
	 * Excel guarda los códigos largos como número (y los muestra como 7,79E+12).
	 * Acá se recuperan todos los dígitos y se sacan los espacios.
	 */
	private static String codigo(Cell celda, DataFormatter formato, FormulaEvaluator evaluador) {
		if (celda == null) {
			return "";
		}
		if (tipo(celda) == CellType.NUMERIC) {
			String comoSeVe = formato.formatCellValue(celda, evaluador).trim();
			if (comoSeVe.matches("\\d+")) {
				return comoSeVe; // respeta ceros a la izquierda si la celda tiene formato 0000...
			}
			BigDecimal valor = BigDecimal.valueOf(celda.getNumericCellValue()).stripTrailingZeros();
			return valor.scale() <= 0 ? valor.toBigInteger().toString() : valor.toPlainString();
		}
		return texto(celda, formato, evaluador).replaceAll("\\s+", "");
	}

	private static Sheet primeraHojaConDatos(Workbook libro) {
		for (Sheet hoja : libro) {
			if (hoja.getPhysicalNumberOfRows() > 0) {
				return hoja;
			}
		}
		return null;
	}

}
