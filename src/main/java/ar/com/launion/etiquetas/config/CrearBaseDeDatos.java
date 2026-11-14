package ar.com.launion.etiquetas.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * PostgreSQL no tiene "createDatabaseIfNotExist" como MySQL.
 * Este listener se conecta a la base "postgres" del servidor y crea
 * la base del sistema (etiquetas_launion) si todavía no existe.
 */
public class CrearBaseDeDatos implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	private static final Logger log = LoggerFactory.getLogger(CrearBaseDeDatos.class);

	// jdbc:postgresql://servidor:puerto/base?parametros
	private static final Pattern URL_POSTGRES = Pattern.compile("^jdbc:postgresql://([^/]+)/([A-Za-z0-9_]+)(\\?.*)?$");

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		Environment env = event.getEnvironment();
		Matcher url = URL_POSTGRES.matcher(env.getProperty("spring.datasource.url", ""));
		if (!url.matches()) {
			return; // no es PostgreSQL (por ejemplo, la base en memoria de los tests)
		}

		String servidor = url.group(1);
		String base = url.group(2);
		String parametros = url.group(3) == null ? "" : url.group(3);
		String usuario = env.getProperty("spring.datasource.username", "postgres");
		String clave = env.getProperty("spring.datasource.password", "");

		try (Connection conexion = DriverManager.getConnection("jdbc:postgresql://" + servidor + "/postgres" + parametros, usuario, clave);
				PreparedStatement consulta = conexion.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {

			consulta.setString(1, base);
			try (ResultSet rs = consulta.executeQuery()) {
				if (rs.next()) {
					return; // ya existe
				}
			}
			try (Statement st = conexion.createStatement()) {
				st.executeUpdate("CREATE DATABASE \"" + base + "\"");
			}
			log.info("Base de datos '{}' creada en PostgreSQL.", base);

		} catch (SQLException e) {
			log.error("No se pudo conectar a PostgreSQL en {}. Revisá que el servicio esté iniciado y que el usuario y la clave "
					+ "de application.properties sean correctos. Detalle: {}", servidor, e.getMessage());
		}
	}

}
