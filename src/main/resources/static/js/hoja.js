/* =========================================================
   Hoja A4 (de a 15 por página): editar precios y copias.
   El "cambiar todos los precios" lo aplica el servidor a TODA la hoja.
   ========================================================= */
(function () {
	'use strict';

	var form = document.getElementById('formHoja');
	if (!form) return;

	var precios = Array.prototype.slice.call(form.querySelectorAll('.campo-precio'));
	var copias = Array.prototype.slice.call(form.querySelectorAll('.campo-copias'));
	var porcentaje = document.getElementById('porcentaje');
	var aplicar = document.getElementById('aplicarPorcentaje');
	var resumen = document.getElementById('resumenHoja');

	// Marca en naranja los precios que cambiaron
	function marcar(campo) {
		var antes = Etiquetas.parsearPrecio(campo.dataset.original);
		var ahora = Etiquetas.parsearPrecio(campo.value);
		campo.classList.toggle('modificado', ahora !== antes);
	}

	function formatear(n) {
		return String(n).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
	}

	// "N etiquetas" y cuántas hojas salen con cada tamaño:
	// total del servidor + lo que cambió en las copias de esta página
	function actualizarResumen() {
		if (!resumen) return;
		var total = Number(resumen.dataset.total) || 0;
		copias.forEach(function (c) {
			var antes = Number(c.dataset.original) || 1;
			var ahora = Math.min(20, Math.max(1, parseInt(c.value, 10) || 1));
			total += ahora - antes;
		});
		resumen.querySelector('strong').textContent = formatear(total);
		resumen.querySelector('span').textContent = total === 1 ? 'etiqueta para imprimir' : 'etiquetas para imprimir';
		form.querySelectorAll('input[name="formato"]').forEach(function (radio) {
			var hojas = Math.ceil(total / (Number(radio.dataset.porHoja) || 4));
			var texto = radio.parentNode.querySelector('.hojas-formato');
			if (texto) texto.textContent = '· ' + formatear(hojas) + (hojas === 1 ? ' hoja' : ' hojas');
		});
	}

	precios.forEach(function (campo, i) {
		campo.addEventListener('input', function () { marcar(campo); });
		// Enter baja al precio siguiente, como en una planilla
		campo.addEventListener('keydown', function (e) {
			if (e.key !== 'Enter') return;
			e.preventDefault();
			var siguiente = precios[i + 1];
			if (siguiente) {
				siguiente.focus();
				siguiente.select();
			} else {
				campo.blur();
			}
		});
	});

	copias.forEach(function (campo) {
		campo.addEventListener('input', actualizarResumen);
		campo.addEventListener('keydown', function (e) {
			if (e.key === 'Enter') e.preventDefault();
		});
	});

	// Cambiar todos los precios: confirmar antes, porque toca toda la hoja (todas las páginas)
	aplicar.addEventListener('click', function (e) {
		var texto = porcentaje.value.trim().replace('%', '');
		if (!texto) {
			e.preventDefault();
			porcentaje.focus();
			return;
		}
		var signo = texto.charAt(0) === '-' ? '' : '+';
		var cantidad = formatear(aplicar.dataset.cantidad || '');
		if (!window.confirm('¿Cambiar ' + signo + texto + ' % el precio de los ' + cantidad + ' productos de la hoja?')) {
			e.preventDefault();
		}
	});
	porcentaje.addEventListener('keydown', function (e) {
		if (e.key === 'Enter') {
			e.preventDefault();
			aplicar.click();
		}
	});
})();
