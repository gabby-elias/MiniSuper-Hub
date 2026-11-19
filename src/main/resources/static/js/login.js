const passwordToggle = document.querySelector('[data-password-toggle]');

if (passwordToggle) {
    passwordToggle.addEventListener('click', function () {
        const password = document.getElementById('password');
        const isVisible = password.type === 'text';

        password.type = isVisible ? 'password' : 'text';
        this.setAttribute('aria-pressed', String(!isVisible));
        this.setAttribute('aria-label', isVisible ? 'Mostrar contraseña' : 'Ocultar contraseña');
    });
}
