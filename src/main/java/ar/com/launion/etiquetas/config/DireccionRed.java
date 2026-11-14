package ar.com.launion.etiquetas.config;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Calcula la dirección para entrar desde otras compus de la red
 * (por ejemplo http://192.168.0.15:8081). Se muestra en el pie del panel.
 */
@Component
public class DireccionRed {

	private static final Logger log = LoggerFactory.getLogger(DireccionRed.class);
	private static final long REFRESCO_MS = 60_000;

	private final int puerto;
	private String direccion;
	private long calculadaEn;

	public DireccionRed(@Value("${server.port:8080}") int puerto) {
		this.puerto = puerto;
	}

	/** http://IP:puerto de esta compu en la red local, o null si no hay red. */
	public synchronized String getDireccion() {
		long ahora = System.currentTimeMillis();
		if (ahora - calculadaEn > REFRESCO_MS) {
			String ip = ipLocal();
			direccion = ip == null ? null : "http://" + ip + ":" + puerto;
			calculadaEn = ahora;
		}
		return direccion;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void avisar() {
		String red = getDireccion();
		log.info("Etiquetas La Unión listo en http://localhost:{}{}", puerto,
				red == null ? "" : "  ·  desde otras compus de la red: " + red);
	}

	private static String ipLocal() {
		try {
			List<String> privadas = new ArrayList<>();
			List<String> otras = new ArrayList<>();
			for (NetworkInterface placa : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				if (!placa.isUp() || placa.isLoopback() || placa.isVirtual() || esVirtual(placa)) {
					continue;
				}
				for (InetAddress ip : Collections.list(placa.getInetAddresses())) {
					if (!(ip instanceof Inet4Address) || ip.isLoopbackAddress() || ip.isLinkLocalAddress()) {
						continue;
					}
					(ip.isSiteLocalAddress() ? privadas : otras).add(ip.getHostAddress());
				}
			}
			if (!privadas.isEmpty()) {
				return privadas.get(0);
			}
			return otras.isEmpty() ? null : otras.get(0);
		} catch (SocketException e) {
			return null;
		}
	}

	// Placas de máquinas virtuales, WSL, Docker, VPN, etc. no sirven para la red del local
	private static boolean esVirtual(NetworkInterface placa) {
		String nombre = (placa.getDisplayName() + " " + placa.getName()).toLowerCase(Locale.ROOT);
		for (String palabra : new String[] { "virtual", "vmware", "hyper-v", "vethernet", "wsl", "docker",
				"bluetooth", "loopback", "tap-", "tunnel", "vpn" }) {
			if (nombre.contains(palabra)) {
				return true;
			}
		}
		return false;
	}

}
