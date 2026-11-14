package ar.com.launion.etiquetas.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import ar.com.launion.etiquetas.dto.EtiquetaForm;
import ar.com.launion.etiquetas.entity.Producto;

public interface ProductoService {

	// ===== Productos guardados =====

	Page<Producto> buscar(String texto, int pagina);

	Producto buscarPorId(Long id);

	Optional<Producto> buscarPorCodigo(String codigoBarras);

	/**
	 * Guarda lo que se cargó en el panel. Si el código de barras ya existe,
	 * actualiza ese producto en lugar de duplicarlo.
	 * Lanza IllegalArgumentException si al editar se usa un código de otro producto.
	 */
	Producto guardar(EtiquetaForm form);

	void eliminar(Long id);

	// ===== Hoja A4 =====

	List<Producto> listarHoja();

	/** Una página de la hoja A4 (de a 15), en el orden en que se agregaron. */
	Page<Producto> listarHoja(int pagina);

	long contarEtiquetasEnHoja();

	/**
	 * Sube (o baja, si es negativo) un porcentaje el precio de TODOS los productos de la hoja,
	 * redondeando al múltiplo de "paso" (1, 10, 50 o 100). Devuelve cuántos precios cambiaron.
	 */
	int ajustarPreciosHoja(BigDecimal porcentaje, int paso);

	void agregarAHoja(List<Long> ids);

	void quitarDeHoja(Long id);

	void vaciarHoja();

	/**
	 * Guarda los precios y copias editados en la hoja.
	 * Devuelve los mensajes de error de los precios que no se pudieron leer.
	 */
	List<String> actualizarHoja(List<Long> ids, List<String> precios, List<String> copias);

	/** Lista de etiquetas a imprimir, repitiendo cada producto según sus copias. */
	List<Producto> etiquetasDeLaHoja();

}
