class ErrorResponse extends Error {
    constructor(error, message, details) {
        super(message);
        this.error = error;
        this.details = details;
    }
}

function handleError(error) {
    if (error instanceof ErrorResponse) {
        if (error.error === 'VALIDATION_ERROR' && error.details) {
            const detailsMessage = Object.entries(error.details)
                .map(([, message]) => `${message}`)
                .join('\n');
            const fullMessage = detailsMessage 
                ? `${error.message}\n${detailsMessage}`
                : error.message;
            showMessage(fullMessage, 'error');
        } else {
            showMessage(error.message || 'Виникла помилка', 'error');
        }
    } else if (error instanceof TypeError && (error.message.includes('fetch') || error.message.includes('Failed to fetch'))) {
        showMessage('Сервер недоступен. Перевірте підключення до інтернету.', 'error');
        console.error('Network error:', error);
    } else if (error instanceof Error) {
        showMessage(error.message || 'Неможливо виконати запит', 'error');
        console.error('Error:', error);
    } else {
        showMessage('Виникла невідома помилка', 'error');
        console.error('Unknown error:', error);
    }
}

async function parseErrorResponse(response) {
    try {
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const errorData = await response.json();
            if (errorData.error && errorData.message) {
                return new ErrorResponse(errorData.error, errorData.message, errorData.details || null);
            } else if (errorData.message) {
                return new Error(errorData.message);
            }
        }
        const text = await response.text();
        return new Error(text || `HTTP ${response.status}: ${response.statusText}`);
    } catch (e) {
        return new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
}