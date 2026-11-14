package ar.com.launion.etiquetas.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.com.launion.etiquetas.entity.Carga;
import ar.com.launion.etiquetas.entity.Producto;
import ar.com.launion.etiquetas.service.CargaService;
import ar.com.launion.etiquetas.service.ProductoService;
import ar.com.launion.etiquetas.util.Paginas;

/**
 * Vista de impresión: hojas A4 horizontales con 4 etiquetas de 13 x 8 cm.
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

	// Una sola etiqueta: GET /imprimir/producto/5
	@GetMapping("/producto/{id}")
	public String producto(@PathVariable("id") Long id,
			@RequestParam(name = "desde", defaultValue = "") String desde,
			Model model, RedirectAttributes redirect) {
		Producto producto = productoService.buscarPorId(id);
		if (producto == null) {
			redirect.addFlashAttribute("error", "Ese producto ya no existe.");
			return "redirect:/productos";
		}
		boolean desdeProductos = "productos".equals(desde);
		model.addAttribute("paginas", List.of(List.of(producto)));
		model.addAttribute("totalEtiquetas", 1);
		model.addAttribute("origen", "producto");
		model.addAttribute("volver", desdeProductos ? "/productos" : "/");
		model.addAttribute("textoVolver", desdeProductos ? "Productos" : "Nueva etiqueta");
		return "impresion/imprimir";
	}

	// Toda la hoja: GET /imprimir/hoja
	@GetMapping("/hoja")
	public String hoja(Model model, RedirectAttributes redirect) {
		List<Producto> etiquetas = productoService.etiquetasDeLaHoja();
		if (etiquetas.isEmpty()) {
			redirect.addFlashAttribute("error", "La hoja está vacía.");
			return "redirect:/hoja";
		}
		model.addAttribute("paginas", Paginas.dividir(etiquetas, Paginas.ETIQUETAS_POR_HOJA));
		model.addAttribute("totalEtiquetas", etiquetas.size());
		model.addAttribute("origen", "hoja");
		model.addAttribute("volver", "/hoja");
		model.addAttribute("textoVolver", "Hoja A4");
		return "impresion/imprimir";
	}

	// Todos los productos de un Excel subido: GET /imprimir/carga/5
	@GetMapping("/carga/{id}")
	public String carga(@PathVariable("id") Long id, Model model, RedirectAttributes redirect) {
		Carga carga = cargaService.buscarConProductos(id);
		List<Producto> etiquetas = carga == null ? List.of() : carga.getProductosVigentes();
		if (etiquetas.isEmpty()) {
			redirect.addFlashAttribute("error", "Esa carga no tiene productos para imprimir.");
			return "redirect:/cargas";
		}
		model.addAttribute("paginas", Paginas.dividir(etiquetas, Paginas.ETIQUETAS_POR_HOJA));
		model.addAttribute("totalEtiquetas", etiquetas.size());
		model.addAttribute("origen", "carga");
		model.addAttribute("volver", "/cargas/" + id);
		model.addAttribute("textoVolver", "Carga de Excel");
		return "impresion/imprimir";
	}

}
