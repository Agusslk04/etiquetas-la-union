package ar.com.launion.etiquetas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ar.com.launion.etiquetas.entity.Carga;
import ar.com.launion.etiquetas.entity.Producto;

@Repository
public interface CargaRepository extends JpaRepository<Carga, Long> {

	// Una carga con todos sus productos (para imprimirla entera)
	@EntityGraph(attributePaths = "productos")
	Optional<Carga> findConProductosByIdCarga(Long idCarga);

	// Los productos de una carga de a una página, en el orden del Excel
	@Query(value = "select p from Carga c join c.productos p where c.idCarga = :idCarga order by index(p)",
			countQuery = "select count(p) from Carga c join c.productos p where c.idCarga = :idCarga")
	Page<Producto> productosDeCarga(@Param("idCarga") Long idCarga, Pageable pageable);

	// Cargas que incluyen un producto (para sacarlo antes de borrarlo)
	@Query("select distinct c from Carga c join c.productos p where p.idProducto = :idProducto")
	List<Carga> buscarQueIncluyen(@Param("idProducto") Long idProducto);

}
