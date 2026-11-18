package ar.com.launion.etiquetas.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.com.launion.etiquetas.entity.Carga;
import ar.com.launion.etiquetas.entity.Producto;
import ar.com.launion.etiquetas.service.CargaService;
import ar.com.launion.etiquetas.service.ProductoService;
import ar.com.launion.etiquetas.util.FormatoEtiqueta;
import ar.com.launion.etiquetas.util.Paginas;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Vista de impresión en hojas A4 (los tamaños y cuántos entran por hoja están en FormatoEtiqueta).
 * El tamaño llega como ?formato=<clave de FormatoEtiqueta>;
 * si no viene, se usa el último que eligió esta compu.
 */
@Controller
@RequestMapping("/imprimir")
public class ImpresionController {

	private final ProductoService productoService;
	private final CargaService cargaService;

	public ImpresionController(ProductoService productoService, CargaService cargaService) {
		this.productoService = productoService;
		this.cargaService = cargaService;
	}

	// Una sola etiqueta: GET /imprimir/producto/5?formato=chica
	@GetMapping("/producto/{id}")
	public String producto(@PathVariable("id") Long id,
			@RequestParam(name = "desde", defaultValue = "") String desde,
			@RequestParam(name = "formato", required = false) String formato,
			@CookieValue(name = FormatoEtiqueta.COOKIE, required = false) String ultimo,
			Model model, HttpServletResponse response, RedirectAttributes redirect) {
		Producto producto = productoService.buscarPorId(id);
		if (producto == null) {
			redirect.addFlashAttribute("error", "Ese producto ya no existe.");
			return "redirect:/productos";
		}
		boolean desdeProductos = "productos".equals(desde);
		preparar(model, List.of(producto), elegir(formato, ultimo, response), "producto",
				"/imprimir/producto/" + id + (desdeProductos ? "?desde=productos" : ""));
		model.addAttribute("volver", desdeProductos ? "/productos" : "/");
		model.addAttribute("textoVolver", desdeProductos ? "Productos" : "Nueva etiqueta");
		return "impresion/imprimir";
	}

	// Toda la hoja: GET /imprimir/hoja?formato=chica
	@GetMapping("/hoja")
	public String hoja(@RequestParam(name = "formato", required = false) String formato,
			@CookieValue(name = FormatoEtiqueta.COOKIE, required = false) String ultimo,
			Model model, HttpServletResponse response, RedirectAttributes redirect) {
		List<Producto> etiquetas = productoService.etiquetasDeLaHoja();
		if (etiquetas.isEmpty()) {
			redirect.addFlashAttribute("error", "La hoja está vacía.");
			return "redirect:/hoja";
		}
		preparar(model, etiquetas, elegir(formato, ultimo, response), "hoja", "/imprimir/hoja");
		model.addAttribute("volver", "/hoja");
		model.addAttribute("textoVolver", "Hoja A4");
		return "impresion/imprimir";
	}

	// Todos los productos de un Excel guardado: GET /imprimir/carga/5?formato=chica
	@GetMapping("/carga/{id}")
	public String carga(@PathVariable("id") Long id,
			@RequestParam(name = "formato", required = false) String formato,
			@CookieValue(name = FormatoEtiqueta.COOKIE, required = false) String ultimo,
			Model model, HttpServletResponse response, RedirectAttributes redirect) {
		Carga carga = cargaService.buscarConProductos(id);
		List<Producto> etiquetas = carga == null ? List.of() : carga.getProductosVigentes();
		if (etiquetas.isEmpty()) {
			redirect.addFlashAttribute("error", "Esa carga no tiene productos para imprimir.");
			return "redirect:/cargas";
		}
		preparar(model, etiquetas, elegir(formato, ultimo, response), "carga", "/imprimir/carga/" + id);
		model.addAttribute("volver", "/cargas/" + id);
		model.addAttribute("textoVolver", "Carga de Excel");
		return "impresion/imprimir";
	}

	/**
	 * Arma las hojas para la vista de impresión.
	 * urlFormato: dirección de esta misma vista, para el selector de tamaño (null = sin selector).
	 */
	static void preparar(Model model, List<Producto> etiquetas, FormatoEtiqueta formato, String origen, String urlFormato) {
		model.addAttribute("formato", formato);
		model.addAttribute("paginas", Paginas.dividir(etiquetas, formato.getPorHoja()));
		model.addAttribute("totalEtiquetas", etiquetas.size());
		model.addAttribute("origen", origen);
		model.addAttribute("urlFormato", urlFormato);
	}

	/** El tamaño pedido (y se recuerda), o el último usado en esta compu. */
	static FormatoEtiqueta elegir(String pedido, String ultimo, HttpServletResponse response) {
		if (pedido == null || pedido.isBlank()) {
			return FormatoEtiqueta.de(ultimo);
		}
		FormatoEtiqueta formato = FormatoEtiqueta.de(pedido);
		DatosGlobales.recordarFormato(response, formato);
		return formato;
	}

}
