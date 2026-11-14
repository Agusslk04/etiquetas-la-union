package ar.com.launion.etiquetas.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ar.com.launion.etiquetas.entity.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

	Optional<Producto> findByCodigoBarras(String codigoBarras);

	// Cuáles de estos códigos ya están cargados (una sola consulta para todo el Excel)
	@Query("select p.codigoBarras from Producto p where p.codigoBarras in :codigos")
	List<String> codigosExistentes(@Param("codigos") Collection<String> codigos);

	Page<Producto> findByNombreContainingIgnoreCaseOrCodigoBarrasContaining(String nombre, String codigoBarras, Pageable pageable);

	// Productos cargados en la hoja A4, en el orden en que se agregaron
	List<Producto> findByEnHojaTrueOrderByFechaEnHojaAscIdProductoAsc();

	// Lo mismo, de a una página
	Page<Producto> findByEnHojaTrue(Pageable pageable);

	// Cantidad de etiquetas en la hoja (sumando las copias)
	@Query("select coalesce(sum(p.copias), 0) from Producto p where p.enHoja = true")
	long contarEtiquetasEnHoja();

	@Modifying
	@Query("update Producto p set p.enHoja = false, p.copias = 1, p.fechaEnHoja = null where p.enHoja = true")
	int vaciarHoja();

}
