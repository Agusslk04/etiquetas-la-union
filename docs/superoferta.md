# Fondo Superoferta

El fondo está en `src/main/resources/static/img/cartel-superoferta.png` y se comparte entre A4 completa y media A4. El nombre del producto, el precio y la fecha se dibujan con HTML/CSS para poder cambiarlos e imprimirlos nítidos en ambos tamaños.

La cantidad de unidades se guarda por producto. Con 1 se muestra solo el precio, y con 2 a 99 se antepone «2 ×», «3 ×», etc. únicamente en los dos formatos Superoferta. El importe es el total ingresado, sin multiplicarlo. La columna `unidades_oferta` acepta null para que los productos anteriores sigan mostrando precio simple al actualizar; el getter los interpreta como 1.

Se preparó a partir de la imagen `Nuevo Cartel Ofertas.jpg` proporcionada por el usuario, usando la herramienta integrada de edición de imágenes (ImageGen). Se quitaron el producto y el precio de ejemplo, manteniendo el logo, los títulos y las zonas de escritura. El original del usuario no se modificó.

## Prompt utilizado

> Use case: precise-object-edit. Asset type: reusable print background for A4 landscape supermarket signs. Input image 1 is the edit target. Change ONLY two areas: remove the black product text 'ALFAJOR GENIO' from inside the narrow cream/white rounded rectangle with red outline, and remove ALL of '3X $1.100' including black letters, digits, currency symbol, orange edges and shadows from inside the large yellow rounded price panel. Reconstruct seamless blank cream/white and yellow panel interiors respectively. These two panels must be completely empty, so software can overlay any product and price later. Preserve EVERYTHING ELSE exactly: the full LA UNIÓN SUPERMERCADO logo and its text, the large red/white/yellow SUPEROFERTA title, the APROVECHÁ banner and megaphone, all border decorations, colors, gradients, panel outlines, positions, dimensions and relative spacing. Keep the complete original landscape A4 composition and aspect ratio, edge to edge, no cropping, no new text, no new objects, no redesign. High-resolution clean print-quality output.
