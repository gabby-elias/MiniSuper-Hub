(() => {
    const table = document.querySelector('[data-catalog-list]');
    if (!table) return;

    const body = table.querySelector('tbody');
    const rows = [...body.querySelectorAll('[data-catalog-row]')];
    const noResults = body.querySelector('[data-catalog-no-results]');
    const visibleCounter = document.querySelector('[data-catalog-visible]');
    const totalCounter = document.querySelector('[data-catalog-total]');
    const pagination = document.querySelector('[data-catalog-pagination]');
    const searches = [...document.querySelectorAll('[data-table-search]')];
    const pageSize = 5;
    let currentPage = 1;
    let filteredRows = [...rows];

    function renderPagination() {
        if (!pagination) return;
        pagination.replaceChildren();
        const totalPages = Math.ceil(filteredRows.length / pageSize) || 1;
        currentPage = Math.min(Math.max(currentPage, 1), totalPages);

        const addButton = (label, page, disabled, active = false) => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = `page-btn${active ? ' active' : ''}`;
            button.textContent = label;
            button.disabled = disabled;
            button.addEventListener('click', () => {
                currentPage = page;
                showPage();
            });
            pagination.appendChild(button);
        };

        addButton('‹', currentPage - 1, currentPage === 1);
        for (let page = 1; page <= totalPages; page++) {
            addButton(String(page), page, false, page === currentPage);
        }
        addButton('›', currentPage + 1, currentPage === totalPages);
    }

    function showPage() {
        const start = (currentPage - 1) * pageSize;
        const pageRows = filteredRows.slice(start, start + pageSize);
        rows.forEach(row => row.hidden = true);
        pageRows.forEach(row => row.hidden = false);
        if (visibleCounter) visibleCounter.textContent = String(pageRows.length);
        if (totalCounter) totalCounter.textContent = String(filteredRows.length);
        renderPagination();
    }

    function filterRows(query = '') {
        const normalizedQuery = query.trim().toLocaleLowerCase();
        filteredRows = rows.filter(row => !normalizedQuery
                || row.textContent.toLocaleLowerCase().includes(normalizedQuery));
        if (noResults) noResults.hidden = filteredRows.length > 0 || rows.length === 0;
        currentPage = 1;
        showPage();
    }

    searches.forEach(input => input.addEventListener('input', () => filterRows(input.value)));
    filterRows();

    const clientDocument = document.getElementById('nuevoClienteDocumento');
    if (clientDocument) {
        clientDocument.addEventListener('input', () => {
            const digits = clientDocument.value.replace(/\D/g, '').slice(0, 9);
            clientDocument.value = digits.length >= 8
                ? `${digits.slice(0, 8)}-${digits.slice(8)}`
                : digits;
        });
        clientDocument.addEventListener('keydown', (event) => {
            if (event.key === 'Backspace' && clientDocument.value.endsWith('-')
                    && clientDocument.selectionStart === clientDocument.value.length) {
                event.preventDefault();
                clientDocument.value = clientDocument.value.slice(0, -2);
                clientDocument.dispatchEvent(new Event('input', { bubbles: true }));
            }
        });
    }
})();
