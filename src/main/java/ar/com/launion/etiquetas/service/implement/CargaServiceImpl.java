package ar.com.launion.etiquetas.service.implement;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.launion.etiquetas.dto.LecturaExcel;
import ar.com.launion.etiquetas.dto.ResultadoCarga;
import ar.com.launion.etiquetas.entity.Carga;
import ar.com.launion.etiquetas.entity.Producto;
import ar.com.launion.etiquetas.repository.CargaRepository;
import ar.com.launion.etiquetas.repository.ProductoRepository;
import ar.com.launion.etiquetas.service.CargaService;
import ar.com.launion.etiquetas.service.CodigoBarrasService;
import ar.com.launion.etiquetas.util.LectorExcel;
import ar.com.launion.etiquetas.util.LectorExcel.Fila;
import ar.com.launion.etiquetas.util.Paginas;
import ar.com.launion.etiquetas.util.PlantillaExcel;
import ar.com.launion.etiquetas.util.Precio;

@Service
public class CargaServiceImpl implements CargaService {

	private static final int MAX_MENSAJES = 100;

	/** Una fila del Excel que pasó la validación. */
	private record FilaValida(int numero, String nombre, BigDecimal precio, String codigo) {

		Producto comoProducto() {
			Producto p = new Producto();
			p.setNombre(nombre);
			p.setPrecio(precio);
			p.setCodigoBarras(codigo);
			return p;
		}
	}

	private final CargaRepository cargaRepository;
	private final ProductoRepository productoRepository;
	private final CodigoBarrasService codigoBarrasService;

	public CargaServiceImpl(CargaRepository cargaRepository, ProductoRepository productoRepository,
			CodigoBarrasService codigoBarrasService) {
		this.cargaRepository = cargaRepository;
		this.productoRepository = productoRepository;
		this.codigoBarrasService = codigoBarrasService;
	}

	// ===== Opción "Guardar productos" =====

	@Override
	@Transactional
	public ResultadoCarga guardar(String nombreArchivo, InputStream contenido) {
		List<Fila> filas = leerFilas(contenido);
		ResultadoCarga resultado = new ResultadoCarga();

		// 1) Validar cada fila y dejar una sola por código (la primera que aparece)
		Map<String, FilaValida> porCodigo = new LinkedHashMap<>();
		int conError = 0, repetidos = 0;
		for (Fila fila : filas) {
			FilaValida valida = validar(fila, resultado.getFilasConError());
			if (valida == null) {
				conError++;
				continue;
			}
			FilaValida primera = porCodigo.putIfAbsent(valida.codigo(), valida);
			if (primera != null) {
				repetidos++;
				agregar(resultado.getCodigosRepetidos(), "Fila " + valida.numero() + ": el código " + valida.codigo()
						+ " ya estaba en la fila " + primera.numero() + " del archivo. Se tomó solo la primera.");
			}
		}

		// 2) Qué códigos ya están en el sistema: una sola consulta para todo el archivo
		Set<String> existentes = porCodigo.isEmpty() ? Set.of()
				: new HashSet<>(productoRepository.codigosExistentes(porCodigo.keySet()));

		// 3) Guardar solo los nuevos; los que ya estaban no se tocan
		LocalDateTime ahora = LocalDateTime.now();
		List<Producto> nuevos = new ArrayList<>();
		int yaCargados = 0;
		for (FilaValida fila : porCodigo.values()) {
			if (existentes.contains(fila.codigo())) {
				yaCargados++;
				agregar(resultado.getProductosYaCargados(),
						"Fila " + fila.numero() + ": «" + fila.nombre() + "» (código " + fila.codigo() + ")");
				continue;
			}
			Producto producto = fila.comoProducto();
			producto.setFechaModificacion(ahora);
			nuevos.add(producto);
		}
		productoRepository.saveAll(nuevos);

		resultado.setNuevos(nuevos.size());
		resultado.setYaCargados(yaCargados);
		resultado.setRepetidos(repetidos);
		resultado.setConError(conError);
		if (nuevos.isEmpty()) {
			return resultado; // no había nada nuevo: no se guarda la carga
		}

		Carga carga = new Carga();
		carga.setNombreArchivo(limpiarNombre(nombreArchivo));
		carga.setFecha(ahora);
		carga.setProductos(nuevos);
		carga.setCantidad(nuevos.size());
		carga.setNuevos(nuevos.size());
		carga.setYaCargados(yaCargados);
		carga.setRepetidos(repetidos);
		carga.setConError(conError);
		resultado.setCarga(cargaRepository.save(carga));
		return resultado;
	}

	// ===== Opción "Solo imprimir" =====

	@Override
	public LecturaExcel leerParaImprimir(InputStream contenido) {
		LecturaExcel lectura = new LecturaExcel();
		int conError = 0;
		for (Fila fila : leerFilas(contenido)) {
			FilaValida valida = validar(fila, lectura.getFilasConError());
			if (valida == null) {
				conError++;
			} else {
				lectura.getEtiquetas().add(valida.comoProducto()); // en memoria, no se guarda
			}
		}
		lectura.setConError(conError);
		return lectura;
	}

	// ===== Historial =====

	@Override
	@Transactional(readOnly = true)
	public Page<Carga> listar(int pagina) {
		return cargaRepository.findAll(PageRequest.of(Paginas.normalizar(pagina), Paginas.TAMANIO,
				Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by(Sort.Direction.DESC, "idCarga"))));
	}

	@Override
	@Transactional(readOnly = true)
	public Carga buscarPorId(Long id) {
		return id == null ? null : cargaRepository.findById(id).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Producto> productosDeCarga(Long id, int pagina) {
		return cargaRepository.productosDeCarga(id, PageRequest.of(Paginas.normalizar(pagina), Paginas.TAMANIO));
	}

	@Override
	@Transactional(readOnly = true)
	public Carga buscarConProductos(Long id) {
		return id == null ? null : cargaRepository.findConProductosByIdCarga(id).orElse(null);
	}

	@Override
	@Transactional
	public void eliminar(Long id) {
		cargaRepository.deleteById(id);
	}

	@Override
	public byte[] generarPlantilla() {
		try {
			return PlantillaExcel.generar();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// ===== Ayudas =====

	private static List<Fila> leerFilas(InputStream contenido) {
		List<Fila> filas;
		try {
			filas = LectorExcel.leer(contenido);
		} catch (LectorExcel.DemasiadosProductos e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch (Exception e) {
			throw new IllegalArgumentException("No se pudo leer el archivo. Tiene que ser un Excel (.xlsx o .xls) sin contraseña.");
		}
		if (filas.isEmpty()) {
			throw new IllegalArgumentException("El Excel no tiene productos. Completá la hoja con nombre, precio y código.");
		}
		return filas;
	}

	/** Devuelve la fila lista para usar, o null (y anota el problema) si tiene errores. */
	private FilaValida validar(Fila fila, List<String> errores) {
		String nombre = fila.nombre().trim();
		String codigo = fila.codigo();
		BigDecimal precio = fila.precioNumero() != null
				? fila.precioNumero().setScale(2, RoundingMode.HALF_UP)
				: Precio.parsear(fila.precioTexto());

		List<String> problemas = new ArrayList<>();
		if (nombre.isEmpty()) {
			problemas.add("falta el nombre");
		} else if (nombre.length() > 60) {
			problemas.add("el nombre tiene más de 60 caracteres");
		}
		if (precio == null) {
			problemas.add(fila.precioTexto().isEmpty() ? "falta el precio" : "el precio «" + fila.precioTexto() + "» no es válido");
		} else if (!Precio.esValido(precio)) {
			problemas.add("el precio tiene que ser mayor a cero");
		}
		if (codigo.isEmpty()) {
			problemas.add("falta el código de barras");
		} else if (!codigoBarrasService.esValido(codigo)) {
			problemas.add("el código «" + codigo + "» tiene caracteres no permitidos");
		}

		if (!problemas.isEmpty()) {
			agregar(errores, "Fila " + fila.numero() + ": " + String.join(", ", problemas) + ".");
			return null;
		}
		return new FilaValida(fila.numero(), nombre, precio, codigo);
	}

	private static void agregar(List<String> lista, String mensaje) {
		if (lista.size() < MAX_MENSAJES) {
			lista.add(mensaje);
		}
	}

	private static String limpiarNombre(String nombreArchivo) {
		String nombre = nombreArchivo == null ? "" : nombreArchivo.replace('\\', '/');
		nombre = nombre.substring(nombre.lastIndexOf('/') + 1).trim();
		if (nombre.isEmpty()) {
			nombre = "productos.xlsx";
		}
		return nombre.length() > 200 ? nombre.substring(0, 200) : nombre;
	}

}
