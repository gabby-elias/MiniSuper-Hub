(() => {
    const pageSize = 5;

    document.querySelectorAll('[data-pagination-table]').forEach((table) => {
        const key = table.dataset.paginationTable;
        const label = table.dataset.paginationLabel || 'registros';
        const rows = [...table.querySelectorAll('tbody [data-pagination-row]')];
        const noResults = table.querySelector('[data-invoice-no-results], [data-pagination-empty]');
        const summary = document.querySelector(`[data-pagination-summary="${key}"]`);
        const controls = document.querySelector(`[data-pagination-controls="${key}"]`);
        const previousButton = controls?.querySelector('[data-page-prev]');
        const nextButton = controls?.querySelector('[data-page-next]');
        const pageNumbers = controls?.querySelector('[data-page-numbers]');
        const searchInputs = [...document.querySelectorAll('[data-table-search]')];
        const statusFilter = document.querySelector('[data-invoice-state]');
        let currentPage = 1;
        let filteredRows = [...rows];

        function filterRows() {
            const query = searchInputs.map(input => input.value.trim().toLocaleLowerCase())
                .find(value => value) || '';
            const status = statusFilter?.value || '';
            filteredRows = rows.filter(row => {
                const matchesQuery = !query || row.textContent.toLocaleLowerCase().includes(query);
                const matchesStatus = !status || row.dataset.invoiceStatus === status;
                return matchesQuery && matchesStatus;
            });
            currentPage = 1;
            showPage();
        }

        function renderPagination(totalPages) {
            if (!controls || !pageNumbers) return;
            pageNumbers.replaceChildren();
            for (let page = 1; page <= totalPages; page += 1) {
                const button = document.createElement('button');
                button.type = 'button';
                button.textContent = String(page);
                button.setAttribute('aria-label', `Página ${page}`);
                if (page === currentPage) {
                    button.classList.add('is-active');
                    button.setAttribute('aria-current', 'page');
                }
                button.addEventListener('click', () => {
                    currentPage = page;
                    showPage();
                });
                pageNumbers.appendChild(button);
            }
            previousButton.disabled = currentPage === 1;
            nextButton.disabled = currentPage === totalPages;
        }

        function showPage() {
            const totalPages = Math.max(1, Math.ceil(filteredRows.length / pageSize));
            currentPage = Math.min(Math.max(currentPage, 1), totalPages);
            const start = (currentPage - 1) * pageSize;
            const pageRows = filteredRows.slice(start, start + pageSize);
            rows.forEach(row => row.hidden = true);
            pageRows.forEach(row => row.hidden = false);

            if (noResults) noResults.hidden = filteredRows.length > 0 || rows.length === 0;
            if (summary) summary.textContent = `Mostrando ${pageRows.length} de ${filteredRows.length} ${label}`;
            renderPagination(totalPages);
        }

        searchInputs.forEach(input => input.addEventListener('input', filterRows));
        statusFilter?.addEventListener('change', filterRows);
        previousButton?.addEventListener('click', () => {
            currentPage -= 1;
            showPage();
        });
        nextButton?.addEventListener('click', () => {
            currentPage += 1;
            showPage();
        });

        filterRows();
    });
})();
