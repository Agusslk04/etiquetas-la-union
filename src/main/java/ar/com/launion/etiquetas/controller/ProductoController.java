package ar.com.launion.etiquetas.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ar.com.launion.etiquetas.entity.Producto;
import ar.com.launion.etiquetas.service.ProductoService;

@Controller
@RequestMapping("/productos")
public class ProductoController {

	private final ProductoService productoService;

	public ProductoController(ProductoService productoService) {
		this.productoService = productoService;
	}

	// Listado con buscador: GET /productos?q=yerba&pagina=0
	@GetMapping
	public String listar(@RequestParam(name = "q", defaultValue = "") String q,
			@RequestParam(name = "pagina", defaultValue = "0") int pagina, Model model) {
		model.addAttribute("q", q);
		model.addAttribute("productos", productoService.buscar(q, pagina));
		return "productos/listProductos";
	}

	// Borra: POST /productos/eliminar/5
	@PostMapping("/eliminar/{id}")
	public String eliminar(@PathVariable("id") Long id,
			@RequestParam(name = "q", defaultValue = "") String q,
			@RequestParam(name = "pagina", defaultValue = "0") int pagina,
			RedirectAttributes redirect) {
		Producto producto = productoService.buscarPorId(id);
		if (producto != null) {
			productoService.eliminar(id);
			redirect.addFlashAttribute("mensaje", "«" + producto.getNombre() + "» se borró.");
		}
		redirect.addAttribute("q", q);
		redirect.addAttribute("pagina", pagina);
		return "redirect:/productos";
	}

	// Usado por el panel al escanear: GET /productos/buscar?codigo=7790...  (JSON)
	@GetMapping("/buscar")
	@ResponseBody
	public Map<String, Object> buscarPorCodigo(@RequestParam(name = "codigo", defaultValue = "") String codigo) {
		return productoService.buscarPorCodigo(codigo)
				.<Map<String, Object>>map(p -> Map.of(
						"encontrado", true,
						"id", p.getIdProducto(),
						"nombre", p.getNombre(),
						"precio", p.getPrecioEditable()))
				.orElse(Map.of("encontrado", false));
	}

}
