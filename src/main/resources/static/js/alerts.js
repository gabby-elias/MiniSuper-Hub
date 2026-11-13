document.addEventListener('DOMContentLoaded', () => {
    const flashAlert = document.getElementById('flash-alert');
    if (flashAlert && window.Swal) {
        Swal.fire({
            toast: true,
            position: 'top-end',
            icon: flashAlert.dataset.type || 'success',
            title: flashAlert.dataset.message || '',
            showConfirmButton: false,
            timer: 2600,
            timerProgressBar: true
        });
    }

    document.querySelectorAll('form[data-swal-confirm]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            if (event.defaultPrevented || form.dataset.confirmed === 'true') return;
            event.preventDefault();
            const isDelete = form.dataset.swalAction === 'delete';
            if (!window.Swal) {
                const title = form.dataset.swalTitle || '¿Estás seguro?';
                const text = form.dataset.swalText || 'Confirma para continuar.';
                if (window.confirm(`${title}\n\n${text}`)) {
                    form.dataset.confirmed = 'true';
                    form.requestSubmit();
                }
                return;
            }
            window.Swal.fire({
                title: form.dataset.swalTitle || '¿Estás seguro?',
                text: form.dataset.swalText || 'Confirma para continuar.',
                icon: isDelete ? 'warning' : 'question',
                showCancelButton: true,
                confirmButtonText: form.dataset.swalConfirmText || (isDelete ? 'Sí, eliminar' : 'Sí, guardar'),
                cancelButtonText: 'Cancelar',
                reverseButtons: true,
                buttonsStyling: false,
                customClass: {
                    confirmButton: isDelete ? 'btn btn-danger ms-2' : 'btn btn-primary ms-2',
                    cancelButton: 'btn btn-secondary'
                }
            }).then((result) => {
                if (result.isConfirmed) {
                    form.dataset.confirmed = 'true';
                    form.requestSubmit();
                }
            });
        });
    });
});
