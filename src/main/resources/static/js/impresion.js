/* =========================================================
   Vista de impresión: escala las hojas para verlas en pantalla
   y abre el diálogo de impresión cuando todo terminó de cargar.
   ========================================================= */
(function () {
	'use strict';

	var hojas = document.querySelector('.hojas');
	var MM = 96 / 25.4;

	function escalar() {
		var s = Math.min(1, (document.documentElement.clientWidth - 40) / (297 * MM));
		hojas.style.setProperty('--escala', s);
	}

	function imagenesCargadas() {
		return Promise.all(Array.prototype.map.call(document.images, function (img) {
			if (img.complete) return Promise.resolve();
			return new Promise(function (ok) { img.onload = img.onerror = ok; });
		}));
	}

	escalar();
	window.addEventListener('resize', escalar);

	Promise.all([document.fonts ? document.fonts.ready : Promise.resolve(), imagenesCargadas()]).then(function () {
		Etiquetas.ajustarTodas();
		// ?auto=0 para ver sin abrir el diálogo
		if (new URLSearchParams(location.search).get('auto') !== '0') {
			setTimeout(function () { window.print(); }, 400);
		}
	});
})();
