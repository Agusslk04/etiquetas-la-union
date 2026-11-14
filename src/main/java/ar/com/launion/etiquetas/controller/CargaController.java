package ar.com.launion.etiquetas.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.com.launion.etiquetas.dto.LecturaExcel;
import ar.com.launion.etiquetas.dto.ResultadoCarga;
import ar.com.launion.etiquetas.entity.Carga;
import ar.com.launion.etiquetas.entity.Producto;
import ar.com.launion.etiquetas.service.CargaService;
import ar.com.launion.etiquetas.service.ProductoService;
import ar.com.launion.etiquetas.util.FormatoEtiqueta;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Carga por Excel con dos opciones:
 *  - Solo imprimir: imprime las etiquetas tal como vienen en el archivo, sin guardar nada.
 *  - Guardar productos: guarda los nuevos (los que ya están cargados no se repiten).
 */
@Controller
@RequestMapping("/cargas")
public class CargaController {

	private static final MediaType XLSX = MediaType
			.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final CargaService cargaService;
	private final ProductoService productoService;

	public CargaController(CargaService cargaService, ProductoService productoService) {
		this.cargaService = cargaService;
		this.productoService = productoService;
	}

	// Pantalla para subir el Excel + historial: GET /cargas?pagina=0
	@GetMapping
	public String inicio(@RequestParam(name = "pagina", defaultValue = "0") int pagina, Model model) {
		model.addAttribute("cargas", cargaService.listar(pagina));
		return "cargas/cargaExcel";
	}

	// Recibe el archivo: POST /cargas  (accion = imprimir | guardar)
	@PostMapping
	public String subir(@RequestParam(name = "archivo", required = false) MultipartFile archivo,
			@RequestParam(name = "accion", defaultValue = "guardar") String accion,
			@RequestParam(name = "formato", required = false) String formato,
			@CookieValue(name = FormatoEtiqueta.COOKIE, required = false) String ultimo,
			Model model, HttpServletResponse response, RedirectAttributes redirect) {

		String problema = revisarArchivo(archivo);
		if (problema != null) {
			redirect.addFlashAttribute("error", problema);
			return "redirect:/cargas";
		}

		try (InputStream contenido = archivo.getInputStream()) {
			return "imprimir".equals(accion)
					? soloImprimir(contenido, ImpresionController.elegir(formato, ultimo, response), model, redirect)
					: guardar(archivo.getOriginalFilename(), contenido, redirect);
		} catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("error", e.getMessage());
			return "redirect:/cargas";
		} catch (IOException e) {
			redirect.addFlashAttribute("error", "No se pudo recibir el archivo. Probá de nuevo.");
			return "redirect:/cargas";
		}
	}

	// Opción "Solo imprimir": muestra la vista de impresión con lo que trae el Excel, sin guardar
	private String soloImprimir(InputStream contenido, FormatoEtiqueta formato, Model model, RedirectAttributes redirect) {
		LecturaExcel lectura = cargaService.leerParaImprimir(contenido);
		if (lectura.getEtiquetas().isEmpty()) {
			redirect.addFlashAttribute("error", "Ninguna fila del Excel se puede imprimir. Revisá las filas marcadas.");
			redirect.addFlashAttribute("filasConError", lectura.getFilasConError());
			redirect.addFlashAttribute("conError", lectura.getConError());
			return "redirect:/cargas";
		}
		// Sin selector de tamaño en la vista: el archivo no queda guardado (el tamaño se eligió al subirlo)
		ImpresionController.preparar(model, lectura.getEtiquetas(), formato, "excel", null);
		model.addAttribute("volver", "/cargas");
		model.addAttribute("textoVolver", "Carga Excel");
		model.addAttribute("filasConError", lectura.getFilasConError());
		model.addAttribute("conError", lectura.getConError());
		return "impresion/imprimir";
	}

	// Opción "Guardar productos": guarda los nuevos y muestra el resultado
	private String guardar(String nombreArchivo, InputStream contenido, RedirectAttributes redirect) {
		ResultadoCarga resultado = cargaService.guardar(nombreArchivo, contenido);

		redirect.addFlashAttribute("filasConError", resultado.getFilasConError());
		redirect.addFlashAttribute("productosYaCargados", resultado.getProductosYaCargados());
		redirect.addFlashAttribute("codigosRepetidos", resultado.getCodigosRepetidos());
		redirect.addFlashAttribute("conError", resultado.getConError());
		redirect.addFlashAttribute("yaCargados", resultado.getYaCargados());
		redirect.addFlashAttribute("repetidos", resultado.getRepetidos());

		if (resultado.getCarga() == null) {
			redirect.addFlashAttribute("error", resultado.getYaCargados() > 0
					? "No había productos nuevos: los del Excel ya estaban cargados."
					: "No se guardó ningún producto. Revisá las filas marcadas.");
			return "redirect:/cargas";
		}
		int nuevos = resultado.getNuevos();
		redirect.addFlashAttribute("mensaje", nuevos == 1 ? "Se guardó 1 producto nuevo." : "Se guardaron " + nuevos + " productos nuevos.");
		return "redirect:/cargas/" + resultado.getCarga().getIdCarga();
	}

	// Detalle de una carga, con sus productos de a 15: GET /cargas/5?pagina=0
	@GetMapping("/{id}")
	public String detalle(@PathVariable("id") Long id,
			@RequestParam(name = "pagina", defaultValue = "0") int pagina,
			Model model, RedirectAttributes redirect) {
		Carga carga = cargaService.buscarPorId(id);
		if (carga == null) {
			redirect.addFlashAttribute("error", "Esa carga ya no existe.");
			return "redirect:/cargas";
		}
		Page<Producto> productos = cargaService.productosDeCarga(id, pagina);
		if (pagina > 0 && productos.getTotalPages() > 0 && pagina >= productos.getTotalPages()) {
			productos = cargaService.productosDeCarga(id, productos.getTotalPages() - 1);
		}
		model.addAttribute("carga", carga);
		model.addAttribute("productos", productos);
		return "cargas/detalleCarga";
	}

	// Manda todos los productos de la carga a la hoja A4: POST /cargas/5/hoja
	@PostMapping("/{id}/hoja")
	public String agregarAHoja(@PathVariable("id") Long id, RedirectAttributes redirect) {
		Carga carga = cargaService.buscarConProductos(id);
		if (carga == null) {
			redirect.addFlashAttribute("error", "Esa carga ya no existe.");
			return "redirect:/cargas";
		}
		List<Long> ids = carga.getProductosVigentes().stream().map(Producto::getIdProducto).toList();
		productoService.agregarAHoja(ids);
		redirect.addFlashAttribute("mensaje", "Se agregaron " + ids.size() + " productos a la hoja A4.");
		return "redirect:/hoja";
	}

	// Borra la carga del historial (los productos quedan): POST /cargas/5/eliminar
	@PostMapping("/{id}/eliminar")
	public String eliminar(@PathVariable("id") Long id, RedirectAttributes redirect) {
		cargaService.eliminar(id);
		redirect.addFlashAttribute("mensaje", "La carga se borró del historial. Los productos siguen guardados.");
		return "redirect:/cargas";
	}

	// Plantilla para completar: GET /cargas/plantilla
	@GetMapping("/plantilla")
	public ResponseEntity<byte[]> plantilla() {
		return ResponseEntity.ok()
				.contentType(XLSX)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("plantilla-etiquetas-la-union.xlsx").build().toString())
				.body(cargaService.generarPlantilla());
	}

	private static String revisarArchivo(MultipartFile archivo) {
		if (archivo == null || archivo.isEmpty()) {
			return "Elegí un archivo de Excel.";
		}
		String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename();
		if (!nombre.toLowerCase(Locale.ROOT).matches(".*\\.(xlsx|xlsm|xls)$")) {
			return "El archivo tiene que ser de Excel (.xlsx o .xls).";
		}
		return null;
	}

}
