document.cookie = "authToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
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
                return response.text().then(text => {
                    if (!text) {
                        throw new Error(`Empty response with status ${response.status}`);
                    }
                    let errors;
                    try {
                        errors = JSON.parse(text);
                    } catch (e) {
                        throw new Error(`Invalid JSON response: ${text}`);
                    }

                    const errorMessages = Object.entries(errors)
                        .map(([field, message]) => `${field}: ${message}`)
                        .join(', ');

                    throw new Error(errorMessages);
                });
            }
            return response.json();
        })
        .then(data => {
            if (data.token) {

                document.cookie = `authToken=${data.token};max-age=${data.expiration}path=/;`;
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
                        window.location.href = '/balance';
                        break;
                    case 'Комірник':
                        window.location.href = '/warehouse';
                        break;
                    case 'Адміністратор':
                        window.location.href = '/settings';
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