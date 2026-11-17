(() => {
    const toggle = document.querySelector('[data-sidebar-toggle]');
    const isMobileLayout = () => window.matchMedia('(max-width: 720px)').matches;
    const isSidebarOpen = () => isMobileLayout()
            ? document.body.classList.contains('sidebar-open')
            : !document.body.classList.contains('sidebar-collapsed');
    const setSidebarOpen = (opened) => {
        const mobile = isMobileLayout();
        document.body.classList.toggle('sidebar-open', mobile && opened);
        document.body.classList.toggle('sidebar-collapsed', !mobile && !opened);
        toggle?.setAttribute('aria-expanded', String(opened));
        toggle?.setAttribute('aria-label', opened ? 'Cerrar menú' : 'Abrir menú');
    };
    const syncSidebarMode = () => {
        if (isMobileLayout()) document.body.classList.remove('sidebar-collapsed');
        else document.body.classList.remove('sidebar-open');
        const opened = isSidebarOpen();
        toggle?.setAttribute('aria-expanded', String(opened));
        toggle?.setAttribute('aria-label', opened ? 'Cerrar menú' : 'Abrir menú');
    };

    toggle?.addEventListener('click', (event) => {
        event.stopPropagation();
        setSidebarOpen(!isSidebarOpen());
    });
    document.addEventListener('click', (event) => {
        if (isMobileLayout() && isSidebarOpen()
                && !event.target.closest('.app-sidebar, [data-sidebar-toggle]')) {
            setSidebarOpen(false);
        }
    });
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && isSidebarOpen()) setSidebarOpen(false);
    });
    window.addEventListener('resize', syncSidebarMode);
    syncSidebarMode();

    const searches = [...document.querySelectorAll('[data-table-search]')];
    const invoiceRows = [...document.querySelectorAll('[data-invoice-row]')];
    const invoiceState = document.querySelector('[data-invoice-state]');
    const noResults = document.querySelector('[data-invoice-no-results]');
    const resultCount = document.getElementById('invoiceResultCount');

    const filterInvoices = () => {
        if (!invoiceRows.length) return;
        const query = searches.map(input => input.value.trim().toLocaleLowerCase()).find(Boolean) || '';
        const state = invoiceState?.value || '';
        let visibleCount = 0;
        invoiceRows.forEach(row => {
            const matches = row.textContent.toLocaleLowerCase().includes(query)
                    && (!state || row.dataset.invoiceStatus === state);
            row.hidden = !matches;
            if (matches) visibleCount++;
        });
        if (noResults) noResults.hidden = visibleCount > 0;
        if (resultCount) resultCount.textContent = `Mostrando ${visibleCount} de ${invoiceRows.length} facturas`;
    };

    document.addEventListener('keydown', event => {
        if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
            event.preventDefault();
            searches[0]?.focus();
        }
    });
    searches.forEach(input => input.addEventListener('input', () => {
        searches.forEach(other => {
            if (other !== input) other.value = input.value;
        });
        filterInvoices();
    }));
    invoiceState?.addEventListener('change', filterInvoices);
    filterInvoices();
})();
