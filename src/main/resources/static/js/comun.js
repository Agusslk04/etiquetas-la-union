/* Confirmación antes de acciones que borran (botones con data-confirmar) */
document.addEventListener('click', function (e) {
	var boton = e.target.closest('[data-confirmar]');
	if (boton && !window.confirm(boton.getAttribute('data-confirmar'))) {
		e.preventDefault();
	}
});

/* Tamaño de etiqueta o cartel: esta compu recuerda el último elegido */
document.addEventListener('change', function (e) {
	var radio = e.target;
	if (radio.name === 'formato' && radio.type === 'radio' && radio.checked) {
		document.cookie = 'formato=' + encodeURIComponent(radio.value) + '; path=/; max-age=31536000; SameSite=Lax';
	}
});
