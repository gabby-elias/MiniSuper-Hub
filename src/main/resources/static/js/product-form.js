(() => {
    const stockInput = document.getElementById('stock');
    if (!stockInput) return;

    const priceInput = document.getElementById('precioVenta');
    const unitInput = document.getElementById('unidadMedida');
    const nameInput = document.getElementById('nombre');
    const unitOptions = [...document.querySelectorAll('[data-unit-option]')];
    const stockUnitLabel = document.getElementById('stockUnitLabel');
    const priceUnitLabel = document.getElementById('priceUnitLabel');
    const previewName = document.getElementById('productPreviewName');
    const previewStock = document.getElementById('productPreviewStock');
    const previewUnit = document.getElementById('productPreviewUnit');
    const previewPrice = document.getElementById('productPreviewPrice');
    const previewPriceUnit = document.getElementById('productPreviewPriceUnit');

    if (!stockInput.value) stockInput.value = '0';

    const priceInCents = () => {
        const price = Number.parseFloat(priceInput?.value);
        return Number.isFinite(price) ? Math.max(0, Math.round(price * 100)) : 0;
    };

    const selectedUnit = () => {
        const value = unitInput?.value.trim();
        if (value) return value;
        return unitInput && !unitInput.hidden ? 'otra unidad' : 'unidad';
    };

    const updatePreview = () => {
        const unit = selectedUnit();
        const name = nameInput?.value.trim() || 'Producto';
        const stock = Math.max(0, Number.parseInt(stockInput.value, 10) || 0);
        const price = `$${(priceInCents() / 100).toFixed(2)}`;

        if (stockUnitLabel) stockUnitLabel.textContent = unit;
        if (priceUnitLabel) priceUnitLabel.textContent = unit;
        if (previewName) previewName.textContent = name;
        if (previewStock) previewStock.textContent = String(stock);
        if (previewUnit) previewUnit.textContent = unit;
        if (previewPrice) previewPrice.textContent = price;
        if (previewPriceUnit) previewPriceUnit.textContent = unit;
    };

    const markUnitOption = selectedValue => {
        unitOptions.forEach(option => {
            const selected = option.dataset.unitValue === selectedValue;
            option.classList.toggle('is-selected', selected);
            option.setAttribute('aria-pressed', String(selected));
        });
    };

    const standardUnitOptions = unitOptions.filter(option => option.dataset.unitValue !== '__custom__');
    const customUnitOption = unitOptions.find(option => option.dataset.unitValue === '__custom__');
    const matchingUnit = value => standardUnitOptions.find(option =>
        option.dataset.unitValue.toLocaleLowerCase() === value.trim().toLocaleLowerCase());

    unitOptions.forEach(option => option.addEventListener('click', () => {
        if (option.dataset.unitValue === '__custom__') {
            if (matchingUnit(unitInput.value)) unitInput.value = '';
            unitInput.hidden = false;
            markUnitOption('__custom__');
            unitInput.focus();
        } else {
            unitInput.value = option.dataset.unitValue;
            unitInput.hidden = true;
            markUnitOption(option.dataset.unitValue);
        }
        updatePreview();
    }));

    unitInput?.addEventListener('input', () => {
        unitInput.hidden = false;
        markUnitOption('__custom__');
        updatePreview();
    });

    const existingUnit = unitInput?.value.trim();
    if (existingUnit) {
        const matchingOption = matchingUnit(existingUnit);
        if (matchingOption) {
            markUnitOption(matchingOption.dataset.unitValue);
            unitInput.hidden = true;
        } else {
            markUnitOption(customUnitOption?.dataset.unitValue);
            unitInput.hidden = false;
        }
    } else {
        unitInput.hidden = true;
        markUnitOption(null);
    }

    document.getElementById('btnStockMinus')?.addEventListener('click', () => {
        const current = Number.parseInt(stockInput.value, 10) || 0;
        if (current > 0) stockInput.value = String(current - 1);
        updatePreview();
    });

    document.getElementById('btnStockPlus')?.addEventListener('click', () => {
        const current = Number.parseInt(stockInput.value, 10) || 0;
        stockInput.value = String(current + 1);
        updatePreview();
    });

    document.querySelectorAll('.quick-add-pill[data-add]').forEach(button => button.addEventListener('click', () => {
        const addAmount = Number.parseInt(button.dataset.add, 10) || 0;
        const current = Number.parseInt(stockInput.value, 10) || 0;
        stockInput.value = String(current + addAmount);
        updatePreview();
    }));

    document.querySelectorAll('[data-price-step]').forEach(button => button.addEventListener('click', () => {
        const step = Number.parseInt(button.dataset.priceStep, 10) || 0;
        priceInput.value = ((priceInCents() + step) / 100).toFixed(2);
        updatePreview();
    }));

    document.querySelectorAll('[data-price-add]').forEach(button => button.addEventListener('click', () => {
        const amount = Math.round((Number.parseFloat(button.dataset.priceAdd) || 0) * 100);
        priceInput.value = ((priceInCents() + amount) / 100).toFixed(2);
        updatePreview();
    }));

    stockInput.addEventListener('input', updatePreview);
    priceInput?.addEventListener('input', updatePreview);
    nameInput?.addEventListener('input', updatePreview);
    updatePreview();
})();
