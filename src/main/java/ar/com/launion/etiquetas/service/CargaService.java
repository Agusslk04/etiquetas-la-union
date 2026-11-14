package ar.com.launion.etiquetas.service;

import java.io.InputStream;

import org.springframework.data.domain.Page;

import ar.com.launion.etiquetas.dto.LecturaExcel;
import ar.com.launion.etiquetas.dto.ResultadoCarga;
import ar.com.launion.etiquetas.entity.Carga;
import ar.com.launion.etiquetas.entity.Producto;

public interface CargaService {

	/**
	 * Opción "Guardar productos": guarda los productos nuevos del Excel.
	 * Los que ya están cargados (mismo código de barras) no se tocan y
	 * un código repetido dentro del mismo archivo se guarda una sola vez.
	 * Lanza IllegalArgumentException con un mensaje claro si el archivo no se puede usar.
	 */
	ResultadoCarga guardar(String nombreArchivo, InputStream contenido);

	/**
	 * Opción "Solo imprimir": arma las etiquetas con los datos del Excel tal cual,
	 * sin guardar nada.
	 */
	LecturaExcel leerParaImprimir(InputStream contenido);

	Page<Carga> listar(int pagina);

	/** La carga (sin sus productos), o null si no existe. */
	Carga buscarPorId(Long id);

	/** Una página de los productos de la carga, en el orden del Excel. */
	Page<Producto> productosDeCarga(Long id, int pagina);

	/** La carga con todos sus productos (para imprimirla), o null si no existe. */
	Carga buscarConProductos(Long id);

	/** Borra la carga del historial (los productos quedan guardados). */
	void eliminar(Long id);

	byte[] generarPlantilla();

}
