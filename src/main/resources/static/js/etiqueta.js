/* =========================================================
   Etiquetas: ajusta el tamaño del texto para que siempre entre,
   escala las vistas previas y formatea precios (es-AR).
   ========================================================= */
(function () {
	'use strict';

	// ---------- Ajuste de texto ----------

	function entra(caja, interno) {
		var estilo = getComputedStyle(caja);
		var ancho = caja.clientWidth - parseFloat(estilo.paddingLeft) - parseFloat(estilo.paddingRight);
		var alto = caja.clientHeight - parseFloat(estilo.paddingTop) - parseFloat(estilo.paddingBottom);
		return interno.scrollWidth <= ancho + 0.5 && interno.offsetHeight <= alto + 0.5;
	}

	// Busca el tamaño de letra más grande (en mm) que entra en la caja
	function ajustar(caja, interno, maxMm, minMm) {
		if (!caja || !interno) return;
		interno.style.fontSize = maxMm + 'mm';
		if (entra(caja, interno)) return;
		var bajo = minMm, alto = maxMm;
		for (var i = 0; i < 12; i++) {
			var medio = (bajo + alto) / 2;
			interno.style.fontSize = medio + 'mm';
			if (entra(caja, interno)) bajo = medio; else alto = medio;
		}
		interno.style.fontSize = bajo + 'mm';
	}

	// Tamaños de letra máximo y mínimo (mm) de cada formato
	var MEDIDAS = {
		grande: { nombre: [7.4, 3.2], precio: [26, 8], codigo: [2.5, 1.4] },
		chica: { nombre: [4.2, 2], precio: [13.6, 5], codigo: [1.75, 1] }
	};

	function ajustarEtiqueta(et) {
		var m = et.classList.contains('etiqueta-chica') ? MEDIDAS.chica : MEDIDAS.grande;
		var nombre = et.querySelector('.et-nombre');
		ajustar(nombre, nombre && nombre.firstElementChild, m.nombre[0], m.nombre[1]);
		var precio = et.querySelector('.et-precio');
		ajustar(precio, precio && precio.firstElementChild, m.precio[0], m.precio[1]);
		var codigo = et.querySelector('.et-codigo');
		ajustar(codigo, codigo && codigo.querySelector('span'), m.codigo[0], m.codigo[1]);
	}

	function ajustarTodas() {
		document.querySelectorAll('.etiqueta').forEach(ajustarEtiqueta);
	}

	// ---------- Escala de la vista previa (sin deformar) ----------

	// data-escala-mm="130": todas las etiquetas del marco usan la misma escala (la de 13 cm),
	// así la de 8 x 4 se ve más chica, como sale impresa. Se muestra la que no está oculta.
	var MM = 96 / 25.4;

	function escalar() {
		document.querySelectorAll('[data-escala]').forEach(function (marco) {
			var hijo = Array.prototype.find.call(marco.querySelectorAll('.etiqueta'), function (e) { return e.offsetWidth > 0; });
			if (!hijo || !hijo.offsetWidth) return;
			var referencia = Number(marco.dataset.escalaMm) * MM || hijo.offsetWidth;
			var s = marco.clientWidth / referencia;
			marco.style.setProperty('--escala', s);
			marco.style.height = (hijo.offsetHeight * s) + 'px';
			hijo.style.left = ((marco.clientWidth - hijo.offsetWidth * s) / 2) + 'px';
		});
	}

	// ---------- Precios ----------

	// Acepta 1299 · 1.299 · 1299,5 · 1.299,50 · $ 1.299 · 1299.50
	function parsearPrecio(texto) {
		var t = String(texto || '').replace(/[$\s ]/g, '');
		if (!t || !/^[0-9.,]+$/.test(t)) return null;
		var coma = t.lastIndexOf(','), punto = t.lastIndexOf('.');
		var normal;
		if (coma >= 0 && punto >= 0) {
			var decimal = coma > punto ? ',' : '.';
			var miles = decimal === ',' ? '.' : ',';
			normal = t.split(miles).join('').replace(decimal, '.');
		} else if (coma >= 0) {
			normal = t.indexOf(',') !== coma ? t.split(',').join('') : t.replace(',', '.');
		} else if (punto >= 0) {
			var sonMiles = t.indexOf('.') !== punto || t.length - punto - 1 === 3;
			normal = sonMiles ? t.split('.').join('') : t;
		} else {
			normal = t;
		}
		var n = Number(normal);
		return isFinite(n) ? Math.round(n * 100) / 100 : null;
	}

	function partesPrecio(n) {
		var centavosTotales = Math.round((n || 0) * 100);
		var entero = Math.floor(centavosTotales / 100);
		var cent = centavosTotales % 100;
		return {
			entero: entero.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.'),
			centavos: cent ? (cent < 10 ? '0' + cent : String(cent)) : ''
		};
	}

	function precioEditable(n) {
		var p = partesPrecio(n);
		return p.centavos ? p.entero + ',' + p.centavos : p.entero;
	}

	// ---------- Arranque ----------

	function listo() {
		ajustarTodas();
		escalar();
	}

	if (document.fonts && document.fonts.ready) {
		document.fonts.ready.then(listo);
	} else {
		window.addEventListener('load', listo);
	}
	window.addEventListener('resize', escalar);

	window.Etiquetas = {
		ajustarEtiqueta: ajustarEtiqueta,
		ajustarTodas: ajustarTodas,
		escalar: escalar,
		parsearPrecio: parsearPrecio,
		partesPrecio: partesPrecio,
		precioEditable: precioEditable
	};
})();
