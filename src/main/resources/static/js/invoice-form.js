(() => {
    const productSearch = document.getElementById('productoSearch');
    const products = document.getElementById('productosDisponibles');
    const suggestions = document.getElementById('productSuggestions');
    const quantityInput = document.getElementById('cantidadInput');
    const lines = document.getElementById('invoiceLines');
    const subtotalLabel = document.getElementById('invoiceSubtotal');
    const totalLabel = document.getElementById('invoiceTotal');
    const itemCount = document.getElementById('invoiceItemCount');
    const saveButton = document.getElementById('saveInvoice');
    const saveHint = document.getElementById('invoiceSaveHint');
    const entryFeedback = document.getElementById('invoiceEntryFeedback');
    const quantityFeedback = document.getElementById('quantityFeedback');
    const form = document.getElementById('invoiceForm');
    const cancelLink = document.getElementById('cancelInvoice');
    const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
    let selectedProduct = null;

    function closeSuggestions() {
        suggestions.hidden = true;
        productSearch.setAttribute('aria-expanded', 'false');
    }

    function renderSuggestions() {
        const query = productSearch.value.trim().toLocaleLowerCase();
        selectedProduct = null;
        suggestions.replaceChildren();
        if (!query) {
            closeSuggestions();
            return;
        }

        const matches = [...products.options]
            .filter(product => product.dataset.name.toLocaleLowerCase().includes(query))
            .slice(0, 8);
        if (!matches.length) {
            const emptyMessage = document.createElement('div');
            emptyMessage.className = 'invoice-product-no-results';
            emptyMessage.textContent = 'No se encontraron productos.';
            suggestions.appendChild(emptyMessage);
        } else {
            matches.forEach((product, index) => {
                const option = document.createElement('button');
                option.type = 'button';
                option.id = `product-suggestion-${index}`;
                option.className = 'invoice-product-suggestion';
                option.setAttribute('role', 'option');
                option.setAttribute('aria-selected', 'false');
                const name = document.createElement('span');
                name.textContent = product.dataset.name;
                const price = document.createElement('strong');
                price.textContent = currency.format(Number(product.dataset.price));
                option.append(name, price);
                option.addEventListener('click', () => {
                    selectedProduct = product;
                    productSearch.value = product.dataset.name;
                    closeSuggestions();
                    productSearch.classList.remove('is-invalid');
                    if (!entryFeedback.hidden) clearEntryFeedback();
                    quantityInput.focus();
                });
                suggestions.appendChild(option);
            });
        }
        suggestions.hidden = false;
        productSearch.setAttribute('aria-expanded', 'true');
    }

    function showEntryFeedback(message) {
        entryFeedback.textContent = message;
        entryFeedback.hidden = false;
    }

    function clearEntryFeedback() {
        entryFeedback.textContent = '';
        entryFeedback.hidden = true;
        productSearch.classList.remove('is-invalid');
        quantityInput.classList.remove('is-invalid');
        quantityFeedback.classList.remove('d-block');
    }

    function renderEmptyState() {
        if (lines.querySelector('.invoice-line')) return;
        const row = document.createElement('tr');
        row.className = 'empty-lines';
        const cell = document.createElement('td');
        cell.colSpan = 5;
        cell.className = 'empty-lines-cell';
        cell.innerHTML = '<div class="invoice-empty-state"><span class="invoice-empty-icon" aria-hidden="true">🛒</span><strong>Todavía no hay productos</strong><span>Busca y agrega productos para empezar a facturar.</span></div>';
        row.appendChild(cell);
        lines.appendChild(row);
    }

    function updateTotal() {
        const rows = [...lines.querySelectorAll('.invoice-line')];
        let total = 0;
        let articulos = 0;
        rows.forEach((row) => {
            const cantidad = Number(row.dataset.quantity);
            const subtotal = Number(row.dataset.price) * cantidad;
            total += subtotal;
            articulos += cantidad;
            row.querySelector('.line-subtotal').textContent = currency.format(subtotal);
        });
        subtotalLabel.textContent = currency.format(total);
        totalLabel.textContent = currency.format(total);
        itemCount.textContent = `${articulos} ${articulos === 1 ? 'artículo' : 'artículos'}`;
        saveButton.disabled = rows.length === 0;
        saveHint.textContent = rows.length ? 'Revisa el total y guarda la factura.' : 'Agrega al menos un producto para poder guardar.';
        saveHint.classList.toggle('invoice-save-hint--ready', rows.length > 0);
        if (rows.length) lines.querySelector('.empty-lines')?.remove();
        else renderEmptyState();
    }

    function addLine(productId, name, price, quantity) {
        const row = document.createElement('tr');
        row.className = 'invoice-line';
        row.dataset.productId = productId;
        row.dataset.name = name;
        row.dataset.price = price;
        row.dataset.quantity = quantity;

        const productCell = document.createElement('td');
        const productName = document.createElement('span');
        productName.className = 'line-product-name';
        productName.textContent = name;
        productCell.appendChild(productName);
        const productIdInput = document.createElement('input');
        productIdInput.type = 'hidden';
        productIdInput.name = 'productoIds';
        productIdInput.value = productId;
        productCell.appendChild(productIdInput);
        const quantityInputHidden = document.createElement('input');
        quantityInputHidden.type = 'hidden';
        quantityInputHidden.name = 'cantidades';
        quantityInputHidden.value = quantity;
        productCell.appendChild(quantityInputHidden);

        const quantityCell = document.createElement('td');
        quantityCell.className = 'line-quantity';
        quantityCell.textContent = quantity;
        const priceCell = document.createElement('td');
        priceCell.className = 'line-price';
        priceCell.textContent = currency.format(price);
        const subtotalCell = document.createElement('td');
        subtotalCell.className = 'line-subtotal';
        subtotalCell.textContent = currency.format(price * quantity);
        const actionCell = document.createElement('td');
        const removeButton = document.createElement('button');
        removeButton.type = 'button';
        removeButton.className = 'btn btn-sm btn-outline-secondary remove-line';
        removeButton.setAttribute('aria-label', 'Quitar producto');
        removeButton.title = 'Quitar producto';
        removeButton.textContent = '×';
        actionCell.appendChild(removeButton);

        row.append(productCell, quantityCell, priceCell, subtotalCell, actionCell);
        lines.querySelector('.empty-lines')?.remove();
        lines.appendChild(row);
        updateTotal();
    }

    document.getElementById('agregarProducto').addEventListener('click', () => {
        const option = selectedProduct;
        const quantity = quantityInput.valueAsNumber;
        clearEntryFeedback();
        if (!option) {
            productSearch.classList.add('is-invalid');
            showEntryFeedback('Selecciona un producto de las sugerencias para agregarlo.');
            productSearch.focus();
            return;
        }
        if (!Number.isInteger(quantity) || quantity < 1) {
            quantityInput.classList.add('is-invalid');
            quantityFeedback.classList.add('d-block');
            showEntryFeedback('La cantidad debe ser un número entero igual o mayor que 1.');
            quantityInput.focus();
            return;
        }
        addLine(option.value, option.dataset.name, Number(option.dataset.price), quantity);
        selectedProduct = null;
        productSearch.value = '';
        quantityInput.value = '1';
        productSearch.focus();
    });

    productSearch.addEventListener('input', () => {
        productSearch.classList.remove('is-invalid');
        if (!entryFeedback.hidden) clearEntryFeedback();
        renderSuggestions();
    });
    productSearch.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') closeSuggestions();
        if (event.key === 'Enter' && !suggestions.hidden) {
            const firstOption = suggestions.querySelector('.invoice-product-suggestion');
            if (firstOption) {
                event.preventDefault();
                firstOption.click();
            }
        }
    });
    document.addEventListener('click', (event) => {
        if (!event.target.closest('.invoice-product-field')) closeSuggestions();
    });
    quantityInput.addEventListener('input', () => {
        quantityInput.classList.remove('is-invalid');
        quantityFeedback.classList.remove('d-block');
        if (!entryFeedback.hidden) clearEntryFeedback();
    });

    document.querySelectorAll('[data-quantity-step]').forEach((button) => {
        button.addEventListener('click', () => {
            let quantity = quantityInput.valueAsNumber;
            if (!Number.isInteger(quantity) || quantity < 1) quantity = 1;
            quantityInput.value = String(Math.max(1, quantity + Number(button.dataset.quantityStep)));
            quantityInput.dispatchEvent(new Event('input', { bubbles: true }));
        });
    });

    lines.addEventListener('click', (event) => {
        const removeButton = event.target.closest('.remove-line');
        if (removeButton) {
            removeButton.closest('.invoice-line').remove();
            updateTotal();
        }
    });

    form.addEventListener('submit', (event) => {
        if (!lines.querySelector('.invoice-line')) {
            event.preventDefault();
            saveHint.textContent = 'Agrega al menos un producto antes de guardar.';
            saveHint.classList.add('invoice-save-hint--error');
        }
    });

    cancelLink.addEventListener('click', (event) => {
        if (!lines.querySelector('.invoice-line')) return;
        event.preventDefault();
        const confirmarCancelacion = () => {
            if (window.Swal) {
                window.Swal.fire({
                    title: '¿Cancelar factura?',
                    text: 'Se perderán los productos que agregaste a esta factura.',
                    icon: 'warning',
                    showCancelButton: true,
                    confirmButtonText: 'Sí, cancelar factura',
                    cancelButtonText: 'Seguir editando',
                    reverseButtons: true
                }).then((result) => {
                    if (result.isConfirmed) window.location.assign(cancelLink.href);
                });
            } else if (window.confirm('Se perderán los productos agregados. ¿Deseas cancelar?')) {
                window.location.assign(cancelLink.href);
            }
        };
        confirmarCancelacion();
    });

    updateTotal();
})();
