/* Confirmación antes de acciones que borran (botones con data-confirmar) */
document.addEventListener('click', function (e) {
	var boton = e.target.closest('[data-confirmar]');
	if (boton && !window.confirm(boton.getAttribute('data-confirmar'))) {
		e.preventDefault();
	}
});
