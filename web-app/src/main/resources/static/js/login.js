document.getElementById('loginForm').addEventListener('submit',
    async function (event) {
    event.preventDefault();

    const login = document.getElementById('login').value;
    const password = document.getElementById('password').value;

    fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            login: login,
            password: password
        }),
        credentials: 'include'
    })
        .then(response => {
            if (!response.ok) {
                return response.json().then(errorResponse => {
                    const message = errorResponse.message || 'Помилка авторизації';
                    throw new Error(message);
                }).catch(() => {
                    throw new Error('Помилка авторизації');
                });
            }
            return response.json();
        })
        .then(data => {
            if (data.token) {

                document.cookie = `authToken=${data.token}; max-age=${data.expiration}; path=/; SameSite=Lax`;
                localStorage.setItem('userId', data.userId);
                localStorage.setItem('userRole', data.role);
                localStorage.setItem('fullName', data.fullName);
                localStorage.setItem('authorities', data.authorities);


                switch (data.role) {
                    case 'Менеджер':
                        window.location.href = '/clients';
                        break;
                    case 'Водій':
                        window.location.href = '/routes';
                        break;
                    case 'Бухгалтер':
                        window.location.href = '/finance';
                        break;
                    case 'Комірник':
                        window.location.href = '/stock';
                        break;
                    case 'Адміністратор':
                        window.location.href = '/administration';
                        break;
                    case 'Декларант':
                        window.location.href = '/declarant';
                        break;
                    default:
                        console.error("Unknown user role:", data.role);
                        window.location.href = '/clients';
                        break;
                }
            } else {
                throw new Error('Token was not received');
            }
        })
        .catch((error) => {
            console.error('Error:', error.message);
            document.getElementById('error-message').textContent = error.message;
        });
});