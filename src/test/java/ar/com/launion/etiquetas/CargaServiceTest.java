package ar.com.launion.etiquetas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

import ar.com.launion.etiquetas.dto.EtiquetaForm;
import ar.com.launion.etiquetas.dto.LecturaExcel;
import ar.com.launion.etiquetas.dto.ResultadoCarga;
import ar.com.launion.etiquetas.entity.Carga;
import ar.com.launion.etiquetas.entity.Producto;
import ar.com.launion.etiquetas.repository.CargaRepository;
import ar.com.launion.etiquetas.repository.ProductoRepository;
import ar.com.launion.etiquetas.service.CargaService;
import ar.com.launion.etiquetas.service.ProductoService;

@SpringBootTest
@ActiveProfiles("prueba")
class CargaServiceTest {

	@Autowired
	private CargaService cargaService;

	@Autowired
	private ProductoService productoService;

	@Autowired
	private CargaRepository cargaRepository;

	@Autowired
	private ProductoRepository productoRepository;

	@BeforeEach
	void limpiar() {
		cargaRepository.deleteAll();
		productoRepository.deleteAll();
	}

	/** Arma un .xlsx en memoria. Cada fila: valores en orden (String o Double, null = celda vacía). */
	private static InputStream excel(Object[]... filas) throws IOException {
		try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			Sheet hoja = libro.createSheet("Productos");
			for (int r = 0; r < filas.length; r++) {
				Row row = hoja.createRow(r);
				for (int c = 0; c < filas[r].length; c++) {
					Object valor = filas[r][c];
					if (valor instanceof Double d) {
						row.createCell(c).setCellValue(d);
					} else if (valor != null) {
						row.createCell(c).setCellValue(valor.toString());
					}
				}
			}
			libro.write(salida);
			return new ByteArrayInputStream(salida.toByteArray());
		}
	}

	private Producto guardarUno(String nombre, String precio, String codigo) {
		EtiquetaForm f = new EtiquetaForm();
		f.setNombre(nombre);
		f.setPrecio(precio);
		f.setCodigoBarras(codigo);
		return productoService.guardar(f);
	}

	// ===== Guardar productos =====

	@Test
	void guardaTodosLosProductosNuevosYLaCarga() throws IOException {
		ResultadoCarga r = cargaService.guardar("lista.xlsx", excel(
				new Object[] { "Nombre del producto", "Precio", "Código de barras" },
				new Object[] { "Yerba 1 kg", 4999d, 7790387000017d },   // código guardado como número por Excel
				new Object[] { "Aceite 1,5 L", "3.849,90", "7790060023455" },
				new Object[] { null, null, null },                        // fila vacía: se ignora
				new Object[] { "Arroz", 1890.5d, "INT-001" }));

		assertNotNull(r.getCarga());
		assertEquals(3, r.getNuevos());
		assertTrue(r.getFilasConError().isEmpty());
		assertEquals("7790387000017", productoRepository.findByCodigoBarras("7790387000017").orElseThrow().getCodigoBarras());
		assertEquals("3.849,90", productoRepository.findByCodigoBarras("7790060023455").orElseThrow().getPrecioEditable());

		Carga guardada = cargaService.buscarConProductos(r.getCarga().getIdCarga());
		assertEquals(List.of("Yerba 1 kg", "Aceite 1,5 L", "Arroz"),
				guardada.getProductosVigentes().stream().map(Producto::getNombre).toList());
	}

	@Test
	void losProductosYaCargadosNoSeRepitenNiSeModifican() throws IOException {
		Producto azucar = guardarUno("Azúcar 1 kg", "1000", "444");

		ResultadoCarga r = cargaService.guardar("precios.xlsx", excel(
				new Object[] { "Producto", "Precio", "Código" },
				new Object[] { "Azúcar Ledesma 1 kg", 1200d, "444" },   // ya estaba: no se toca
				new Object[] { "Sal 500 g", 700d, "555" }));

		assertEquals(1, r.getNuevos());
		assertEquals(1, r.getYaCargados());
		assertEquals(2, productoRepository.count());
		assertTrue(r.getProductosYaCargados().get(0).contains("444"));

		Producto igual = productoService.buscarPorId(azucar.getIdProducto());
		assertEquals("Azúcar 1 kg", igual.getNombre());
		assertEquals(0, new BigDecimal("1000").compareTo(igual.getPrecio()));

		// La carga solo tiene el producto nuevo
		Carga carga = cargaService.buscarConProductos(r.getCarga().getIdCarga());
		assertEquals(List.of("Sal 500 g"), carga.getProductosVigentes().stream().map(Producto::getNombre).toList());
		assertEquals(1, carga.getYaEstaban());
	}

	@Test
	void unCodigoRepetidoEnElArchivoSeGuardaUnaSolaVez() throws IOException {
		ResultadoCarga r = cargaService.guardar("repetidos.xlsx", excel(
				new Object[] { "Nombre", "Precio", "Código" },
				new Object[] { "Sal 500 g", 700d, "555" },
				new Object[] { "Sal fina 500 g", 750d, "555" }));

		assertEquals(1, r.getNuevos());
		assertEquals(1, r.getRepetidos());
		assertEquals(1, productoRepository.count());
		assertEquals("Sal 500 g", productoRepository.findByCodigoBarras("555").orElseThrow().getNombre());
		assertTrue(r.getCodigosRepetidos().get(0).startsWith("Fila 3"));
	}

	@Test
	void siTodoYaEstabaCargadoNoSeGuardaLaCarga() throws IOException {
		guardarUno("Café", "3500", "666");

		ResultadoCarga r = cargaService.guardar("otra-vez.xlsx", excel(
				new Object[] { "Nombre", "Precio", "Código" },
				new Object[] { "Café", 3500d, "666" }));

		assertNull(r.getCarga());
		assertEquals(1, r.getYaCargados());
		assertEquals(0, cargaRepository.count());
	}

	@Test
	void reconoceColumnasEnOtroOrdenYReportaFilasConError() throws IOException {
		ResultadoCarga r = cargaService.guardar("otro.xlsx", excel(
				new Object[] { "EAN", "Descripción", "PVP" },
				new Object[] { "111", "Fideos", 900d },
				new Object[] { "222", "", 500d },               // sin nombre
				new Object[] { "333", "Galletitas", "abc" },    // precio inválido
				new Object[] { "", "Leche", 1500d }));           // sin código

		assertEquals(1, r.getNuevos());
		assertEquals(3, r.getConError());
		assertTrue(r.getFilasConError().get(0).startsWith("Fila 3: falta el nombre"));
		assertTrue(r.getFilasConError().get(1).contains("«abc»"));
	}

	@Test
	void sinTitulosUsaColumnasABC() throws IOException {
		ResultadoCarga r = cargaService.guardar("sin-titulos.xlsx", excel(
				new Object[] { "Café 250 g", 3500d, "666" },
				new Object[] { "Té x 25", 1200d, "777" }));

		assertEquals(2, r.getNuevos());
	}

	// ===== Solo imprimir =====

	@Test
	void soloImprimirNoGuardaNadaYRespetaElExcel() throws IOException {
		guardarUno("Azúcar 1 kg", "1000", "444");

		LecturaExcel lectura = cargaService.leerParaImprimir(excel(
				new Object[] { "Nombre", "Precio", "Código" },
				new Object[] { "Azúcar Ledesma 1 kg", 1200d, "444" },  // ya cargado: se imprime con el precio del Excel
				new Object[] { "Sal 500 g", 700d, "555" },
				new Object[] { "Sal 500 g", 700d, "555" },             // repetida: se imprime igual
				new Object[] { "Sin precio", "", "888" }));

		assertEquals(3, lectura.getEtiquetas().size());
		assertEquals(1, lectura.getConError());
		assertEquals("1.200", lectura.getEtiquetas().get(0).getPrecioEntero());
		assertEquals(1, productoRepository.count());   // no se guardó nada nuevo
		assertEquals(0, cargaRepository.count());
		assertEquals("1.000", productoRepository.findByCodigoBarras("444").orElseThrow().getPrecioEditable());
	}

	// ===== Archivos raros, páginas y borrado =====

	@Test
	void archivoQueNoEsExcelDaUnMensajeClaro() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> cargaService.guardar("lista.xlsx", new ByteArrayInputStream("hola".getBytes())));
		assertTrue(e.getMessage().contains("No se pudo leer el archivo"));
	}

	@Test
	void laPlantillaSeLeeSinProductos() throws IOException {
		byte[] plantilla = cargaService.generarPlantilla();
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> cargaService.leerParaImprimir(new ByteArrayInputStream(plantilla)));
		assertTrue(e.getMessage().contains("no tiene productos"));
	}

	@Test
	void losProductosDeUnaCargaSeVenDeA15EnElOrdenDelExcel() throws IOException {
		Object[][] filas = new Object[41][];
		filas[0] = new Object[] { "Nombre", "Precio", "Código" };
		for (int i = 1; i <= 40; i++) {
			filas[i] = new Object[] { "Producto " + i, 100d + i, "C" + i };
		}
		ResultadoCarga r = cargaService.guardar("cuarenta.xlsx", excel(filas));

		Page<Producto> pagina2 = cargaService.productosDeCarga(r.getCarga().getIdCarga(), 1);
		assertEquals(40, pagina2.getTotalElements());
		assertEquals(3, pagina2.getTotalPages());
		assertEquals(15, pagina2.getContent().size());
		assertEquals("Producto 16", pagina2.getContent().get(0).getNombre());
		assertEquals(10, cargaService.productosDeCarga(r.getCarga().getIdCarga(), 2).getContent().size());
	}

	@Test
	void borrarUnProductoLoSacaDeLaCarga() throws IOException {
		ResultadoCarga r = cargaService.guardar("dos.xlsx", excel(
				new Object[] { "Nombre", "Precio", "Código" },
				new Object[] { "Uno", 100d, "901" },
				new Object[] { "Dos", 200d, "902" }));

		Long idUno = productoRepository.findByCodigoBarras("901").orElseThrow().getIdProducto();
		productoService.eliminar(idUno);

		Carga carga = cargaService.buscarConProductos(r.getCarga().getIdCarga());
		assertEquals(1, carga.getCantidad());
		assertEquals("Dos", carga.getProductosVigentes().get(0).getNombre());
	}

}
