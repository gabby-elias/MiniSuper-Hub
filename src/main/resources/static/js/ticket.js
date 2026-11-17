window.prepararImpresion = () => {
    const ticket = document.querySelector('.ticket');
    const alturaMm = Math.ceil(ticket.scrollHeight * 25.4 / 96);
    let estiloPagina = document.getElementById('ticket-page-size');
    if (!estiloPagina) {
        estiloPagina = document.createElement('style');
        estiloPagina.id = 'ticket-page-size';
        document.head.appendChild(estiloPagina);
    }
    estiloPagina.textContent = `@page { size: 80mm ${alturaMm}mm; margin: 0; }`;
    window.print();
};

document.querySelector('[data-ticket-print]')?.addEventListener('click', window.prepararImpresion);
window.addEventListener('load', () => window.setTimeout(window.prepararImpresion, 300));
