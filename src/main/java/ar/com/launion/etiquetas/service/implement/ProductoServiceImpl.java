package ar.com.launion.etiquetas.service.implement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.launion.etiquetas.dto.EtiquetaForm;
import ar.com.launion.etiquetas.entity.Carga;
import ar.com.launion.etiquetas.entity.Producto;
import ar.com.launion.etiquetas.repository.CargaRepository;
import ar.com.launion.etiquetas.repository.ProductoRepository;
import ar.com.launion.etiquetas.service.ProductoService;
import ar.com.launion.etiquetas.util.Paginas;
import ar.com.launion.etiquetas.util.Precio;

@Service
public class ProductoServiceImpl implements ProductoService {

	public static final int MAX_COPIAS = 20;

	private final ProductoRepository productoRepository;
	private final CargaRepository cargaRepository;

	public ProductoServiceImpl(ProductoRepository productoRepository, CargaRepository cargaRepository) {
		this.productoRepository = productoRepository;
		this.cargaRepository = cargaRepository;
	}

	// ===== Productos guardados =====

	@Override
	@Transactional(readOnly = true)
	public Page<Producto> buscar(String texto, int pagina) {
		String t = texto == null ? "" : texto.trim();
		PageRequest pedido = PageRequest.of(Paginas.normalizar(pagina), Paginas.TAMANIO,
				Sort.by(Sort.Direction.DESC, "fechaModificacion").and(Sort.by(Sort.Direction.DESC, "idProducto")));
		return productoRepository.findByNombreContainingIgnoreCaseOrCodigoBarrasContaining(t, t, pedido);
	}

	@Override
	@Transactional(readOnly = true)
	public Producto buscarPorId(Long id) {
		return id == null ? null : productoRepository.findById(id).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Producto> buscarPorCodigo(String codigoBarras) {
		if (codigoBarras == null || codigoBarras.isBlank()) {
			return Optional.empty();
		}
		return productoRepository.findByCodigoBarras(codigoBarras.trim());
	}

	@Override
	@Transactional
	public Producto guardar(EtiquetaForm form) {
		String codigo = form.getCodigoBarras().trim();
		Optional<Producto> conEseCodigo = productoRepository.findByCodigoBarras(codigo);

		Producto producto;
		if (form.getIdProducto() != null) {
			// Edición de un producto existente
			producto = productoRepository.findById(form.getIdProducto())
					.orElseThrow(() -> new IllegalArgumentException("El producto ya no existe."));
			if (conEseCodigo.isPresent() && !conEseCodigo.get().getIdProducto().equals(producto.getIdProducto())) {
				throw new IllegalArgumentException("Ese código ya está cargado en «" + conEseCodigo.get().getNombre() + "».");
			}
		} else {
			// Carga nueva: si el código ya existe se actualiza ese producto
			producto = conEseCodigo.orElseGet(Producto::new);
		}

		producto.setNombre(form.getNombre().trim());
		producto.setPrecio(Precio.parsear(form.getPrecio()));
		producto.setCodigoBarras(codigo);
		producto.setFechaModificacion(LocalDateTime.now());
		return productoRepository.save(producto);
	}

	@Override
	@Transactional
	public void eliminar(Long id) {
		// Primero se saca de las cargas de Excel que lo incluyen
		for (Carga carga : cargaRepository.buscarQueIncluyen(id)) {
			carga.getProductos().removeIf(p -> p != null && p.getIdProducto().equals(id));
			carga.setCantidad(carga.getProductos().size());
		}
		productoRepository.deleteById(id);
	}

	// ===== Hoja A4 =====

	@Override
	@Transactional(readOnly = true)
	public List<Producto> listarHoja() {
		return productoRepository.findByEnHojaTrueOrderByFechaEnHojaAscIdProductoAsc();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Producto> listarHoja(int pagina) {
		return productoRepository.findByEnHojaTrue(PageRequest.of(Paginas.normalizar(pagina), Paginas.TAMANIO,
				Sort.by(Sort.Direction.ASC, "fechaEnHoja").and(Sort.by(Sort.Direction.ASC, "idProducto"))));
	}

	@Override
	@Transactional(readOnly = true)
	public long contarEtiquetasEnHoja() {
		return productoRepository.contarEtiquetasEnHoja();
	}

	@Override
	@Transactional
	public int ajustarPreciosHoja(BigDecimal porcentaje, int paso) {
		BigDecimal factor = BigDecimal.ONE.add(porcentaje.movePointLeft(2));
		BigDecimal multiplo = BigDecimal.valueOf(Math.max(1, paso));
		LocalDateTime ahora = LocalDateTime.now();
		int cambiados = 0;
		for (Producto p : listarHoja()) {
			BigDecimal nuevo = p.getPrecio().multiply(factor)
					.divide(multiplo, 0, RoundingMode.HALF_UP)
					.multiply(multiplo)
					.max(multiplo)
					.setScale(2, RoundingMode.HALF_UP);
			if (nuevo.compareTo(p.getPrecio()) != 0) {
				p.setPrecio(nuevo);
				p.setFechaModificacion(ahora);
				cambiados++;
			}
		}
		return cambiados;
	}

	@Override
	@Transactional
	public void agregarAHoja(List<Long> ids) {
		if (ids == null) {
			return;
		}
		for (Producto p : productoRepository.findAllById(ids)) {
			if (!p.isEnHoja()) {
				p.setEnHoja(true);
				p.setCopias(1);
				p.setFechaEnHoja(LocalDateTime.now());
			}
		}
	}

	@Override
	@Transactional
	public void quitarDeHoja(Long id) {
		productoRepository.findById(id).ifPresent(p -> {
			p.setEnHoja(false);
			p.setCopias(1);
			p.setFechaEnHoja(null);
		});
	}

	@Override
	@Transactional
	public void vaciarHoja() {
		productoRepository.vaciarHoja();
	}

	@Override
	@Transactional
	public List<String> actualizarHoja(List<Long> ids, List<String> precios, List<String> copias) {
		List<String> errores = new ArrayList<>();
		if (ids == null) {
			return errores;
		}
		for (int i = 0; i < ids.size(); i++) {
			Producto p = productoRepository.findById(ids.get(i)).orElse(null);
			if (p == null) {
				continue;
			}

			BigDecimal precio = Precio.parsear(elemento(precios, i));
			if (!Precio.esValido(precio)) {
				errores.add("Revisá el precio de «" + p.getNombre() + "».");
			} else if (precio.compareTo(p.getPrecio()) != 0) {
				p.setPrecio(precio);
				p.setFechaModificacion(LocalDateTime.now());
			}

			String textoCopias = elemento(copias, i);
			if (textoCopias != null && textoCopias.trim().matches("\\d{1,3}")) {
				p.setCopias(Math.max(1, Math.min(MAX_COPIAS, Integer.parseInt(textoCopias.trim()))));
			}
		}
		return errores;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Producto> etiquetasDeLaHoja() {
		List<Producto> etiquetas = new ArrayList<>();
		for (Producto p : listarHoja()) {
			for (int c = 0; c < Math.max(1, p.getCopias()); c++) {
				etiquetas.add(p);
			}
		}
		return etiquetas;
	}

	private static String elemento(List<String> lista, int i) {
		return lista != null && i < lista.size() ? lista.get(i) : null;
	}

}
