(() => {
    const searchInput = document.querySelector('#user-list-search');
    const rows = [...document.querySelectorAll('[data-user-row]')];
    const noResults = document.querySelector('[data-user-no-results]');
    const visibleCounter = document.querySelector('[data-user-visible]');

    if (!searchInput || rows.length === 0) return;

    const normalize = value => value
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLocaleLowerCase();

    searchInput.addEventListener('input', () => {
        const query = normalize(searchInput.value.trim());
        let visibleCount = 0;

        rows.forEach(row => {
            const matches = normalize(row.textContent).includes(query);
            row.hidden = !matches;
            if (matches) visibleCount += 1;
        });

        if (visibleCounter) visibleCounter.textContent = String(visibleCount);
        if (noResults) noResults.hidden = visibleCount > 0;
    });
})();
