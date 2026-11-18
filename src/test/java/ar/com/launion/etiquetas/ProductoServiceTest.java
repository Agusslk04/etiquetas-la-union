package ar.com.launion.etiquetas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import ar.com.launion.etiquetas.dto.EtiquetaForm;
import ar.com.launion.etiquetas.entity.Producto;
import ar.com.launion.etiquetas.repository.CargaRepository;
import ar.com.launion.etiquetas.repository.ProductoRepository;
import ar.com.launion.etiquetas.service.CodigoBarrasService;
import ar.com.launion.etiquetas.service.ProductoService;

@SpringBootTest
@ActiveProfiles("prueba")
class ProductoServiceTest {

	@Autowired
	private ProductoService productoService;

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private CodigoBarrasService codigoBarrasService;

	@Autowired
	private CargaRepository cargaRepository;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void limpiar() {
		cargaRepository.deleteAll();
		productoRepository.deleteAll();
	}

	private static EtiquetaForm form(String nombre, String precio, String codigo) {
		EtiquetaForm f = new EtiquetaForm();
		f.setNombre(nombre);
		f.setPrecio(precio);
		f.setCodigoBarras(codigo);
		return f;
	}

	@Test
	void mismoCodigoActualizaEnVezDeDuplicar() {
		productoService.guardar(form("Yerba 1 kg", "4.500", "7790387000017"));
		Producto actualizado = productoService.guardar(form("Yerba Playadito 1 kg", "4.990", "7790387000017"));

		assertEquals(1, productoRepository.count());
		assertEquals("Yerba Playadito 1 kg", actualizado.getNombre());
		assertEquals(0, new BigDecimal("4990").compareTo(actualizado.getPrecio()));
	}

	@Test
	void alEditarNoPermiteUsarElCodigoDeOtroProducto() {
		productoService.guardar(form("Yerba", "4500", "111"));
		Producto azucar = productoService.guardar(form("Azúcar", "1200", "222"));

		EtiquetaForm edicion = EtiquetaForm.desde(azucar);
		edicion.setCodigoBarras("111");
		assertThrows(IllegalArgumentException.class, () -> productoService.guardar(edicion));
	}

	@Test
	void conservaLasUnidadesDeSuperofertaSinMultiplicarElPrecio() {
		EtiquetaForm oferta = form("Alfajor Genio", "1.100", "OFERTA-3");
		oferta.setUnidadesOferta(3);
		Long id = productoService.guardar(oferta).getIdProducto();
		Producto guardado = productoService.buscarPorId(id);
		assertEquals(3, guardado.getUnidadesOferta());
		assertEquals("1.100", guardado.getPrecioEditable());

		productoService.agregarAHoja(List.of(id));
		productoService.actualizarHoja(List.of(id), List.of("1.200,50"), List.of("2"));
		assertEquals(2, productoService.etiquetasDeLaHoja().size());
		assertTrue(productoService.etiquetasDeLaHoja().stream().allMatch(p -> p.getUnidadesOferta() == 3));

		EtiquetaForm edicion = EtiquetaForm.desde(productoService.buscarPorId(id));
		assertEquals(3, edicion.getUnidadesOferta());
		edicion.setUnidadesOferta(2);
		productoService.guardar(edicion);
		assertEquals(2, productoService.buscarPorId(id).getUnidadesOferta());
		edicion.setUnidadesOferta(1);
		productoService.guardar(edicion);
		assertEquals(1, productoService.buscarPorId(id).getUnidadesOferta());
		assertEquals("1.200,50", productoService.buscarPorId(id).getPrecioEditable());
	}

	@Test
	void productosAnterioresEImportadosUsanPrecioSimple() {
		assertEquals(1, new Producto().getUnidadesOferta());
		Long id = productoService.guardar(form("Producto anterior", "900", "ANTERIOR")).getIdProducto();
		// La columna nueva queda en null en los registros creados antes de la actualización.
		jdbc.update("update producto set unidades_oferta = null where id_producto = ?", id);
		Producto anterior = productoService.buscarPorId(id);
		assertEquals(1, anterior.getUnidadesOferta());
		assertEquals(1, EtiquetaForm.desde(anterior).getUnidadesOferta());
		assertEquals("900", anterior.getPrecioEditable());
	}

	@Test
	void rechazaUnidadesDeOfertaInvalidas() {
		EtiquetaForm oferta = form("Oferta", "1000", "INVALIDA");
		for (Integer unidades : new Integer[] { null, -1, 0, 100 }) {
			oferta.setUnidadesOferta(unidades);
			assertThrows(IllegalArgumentException.class, () -> productoService.guardar(oferta));
		}
		assertEquals(0, productoRepository.count());
	}

	@Test
	void laHojaRepiteSegunCopiasYGuardaPreciosEditados() {
		Producto a = productoService.guardar(form("Leche", "1500", "333"));
		Producto b = productoService.guardar(form("Pan", "2000", "444"));
		productoService.agregarAHoja(List.of(a.getIdProducto(), b.getIdProducto()));

		List<String> errores = productoService.actualizarHoja(
				List.of(a.getIdProducto(), b.getIdProducto()),
				List.of("1.650", "2.200,50"),
				List.of("3", "1"));

		assertTrue(errores.isEmpty());
		assertEquals(4, productoService.contarEtiquetasEnHoja());
		assertEquals(4, productoService.etiquetasDeLaHoja().size());
		assertEquals("1.650", productoService.buscarPorId(a.getIdProducto()).getPrecioEditable());
		assertEquals("2.200,50", productoService.buscarPorId(b.getIdProducto()).getPrecioEditable());

		productoService.vaciarHoja();
		assertEquals(0, productoService.contarEtiquetasEnHoja());
	}

	@Test
	void laHojaSeVeDeA15YElPorcentajeCambiaTodasLasPaginas() {
		List<Long> ids = new java.util.ArrayList<>();
		for (int i = 1; i <= 20; i++) {
			ids.add(productoService.guardar(form("Producto " + i, "1000", "H" + i)).getIdProducto());
		}
		productoService.agregarAHoja(ids);

		assertEquals(15, productoService.listarHoja(0).getContent().size());
		assertEquals(5, productoService.listarHoja(1).getContent().size());
		assertEquals(2, productoService.listarHoja(0).getTotalPages());

		// +10 % redondeado a $50: 1000 -> 1100 en los 20 (también los de la página 2)
		assertEquals(20, productoService.ajustarPreciosHoja(new BigDecimal("10"), 50));
		assertTrue(productoService.listarHoja().stream().allMatch(p -> p.getPrecio().compareTo(new BigDecimal("1100")) == 0));

		// -7 % redondeado a $10: 1100 -> 1023 -> 1020
		productoService.ajustarPreciosHoja(new BigDecimal("-7"), 10);
		assertEquals("1.020", productoService.listarHoja(1).getContent().get(0).getPrecioEditable());
	}

	@Test
	void precioInvalidoEnLaHojaDevuelveError() {
		Producto a = productoService.guardar(form("Fideos", "900", "555"));
		productoService.agregarAHoja(List.of(a.getIdProducto()));

		List<String> errores = productoService.actualizarHoja(List.of(a.getIdProducto()), List.of("abc"), List.of("1"));

		assertEquals(1, errores.size());
		assertEquals("900", productoService.buscarPorId(a.getIdProducto()).getPrecioEditable());
	}

	@Test
	void generaCodigoDeBarrasEnSvg() {
		String ean = codigoBarrasService.generarSvg("7790070012340");
		assertTrue(ean.startsWith("<svg") && ean.contains("viewBox=\"0 0 95 1\""), "EAN-13 tiene 95 módulos");
		assertTrue(codigoBarrasService.generarSvg("INTERNO-123").contains("<path"));
		assertTrue(!codigoBarrasService.esValido("con espacio"));
	}

}
