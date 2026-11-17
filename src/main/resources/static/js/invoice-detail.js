(() => {
    const product = document.getElementById('detalleProducto');
    const quantity = document.getElementById('detalleCantidad');
    const price = document.getElementById('detallePrecio');
    const subtotal = document.getElementById('detalleSubtotal');
    if (!product || !quantity || !price || !subtotal) return;

    const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
    const update = () => {
        const option = product.selectedOptions[0];
        const unitPrice = Number(option?.dataset.price || 0);
        const units = Math.max(1, Number.parseInt(quantity.value, 10) || 1);
        price.value = currency.format(unitPrice);
        subtotal.value = currency.format(unitPrice * units);
    };

    product.addEventListener('change', update);
    quantity.addEventListener('input', update);
    update();
})();
