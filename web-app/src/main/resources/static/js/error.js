class ErrorResponse extends Error {
    constructor(error, message, details) {
        super(message);
        this.error = error;
        this.details = details;
    }
}

function handleError(error) {
    if (error instanceof ErrorResponse) {
        switch (error.error) {
            case 'VALIDATION_ERROR':
                const detailsMessage = error.details
                    ? Object.entries(error.details)
                        .map(([, message]) => `${message}`)
                        .join('\n')
                    : '';
                showMessage(
                    `Помилка валідації:\n${detailsMessage || error.message}`,
                    'error'
                );
                break;
            case 'ACCESS_DENIED':
                showMessage(error.message, 'error');
                break;
            case 'CLIENT_ERROR_DEFAULT':
                showMessage(`Помилка клієнта: ${error.message}`, 'error');
                break;
            case 'CLIENT_NOT_FOUND':
                showMessage(`Клієнта не знайдено: ${error.message}`, 'error');
                break;
            case 'BALANCE_ERROR_DEFAULT':
                showMessage(`Помилка балансу: ${error.message}`, 'error');
                break;
            case 'BALANCE_NOT_FOUND':
                showMessage(`Баланс не знайдено: ${error.message}`, 'error');
                break;
            case 'STATUSCLIENT_ERROR_DEFAULT':
                showMessage(`Помилка балансу: ${error.message}`, 'error');
                break;
            case 'STATUSCLIENT_NOT_FOUND':
                showMessage(`Баланс не знайдено: ${error.message}`, 'error');
                break;
            case 'BUSINESS_ERROR_DEFAULT':
                showMessage(`Помилка бізнесу: ${error.message}`, 'error');
                break;
            case 'BUSINESS_NOT_FOUND':
                showMessage(`Бізнес не знайдено: ${error.message}`, 'error');
                break;
            case 'SOURCE_ERROR_DEFAULT':
                showMessage(`Помилка залучення: ${error.message}`, 'error');
                break;
            case 'SOURCE_NOT_FOUND':
                showMessage(`Залучення не знайдено: ${error.message}`, 'error');
                break;
            case 'ROUTE_ERROR_DEFAULT':
                showMessage(`Помилка маршруту: ${error.message}`, 'error');
                break;
            case 'ROUTE_NOT_FOUND':
                showMessage(`Маршрут не знайдено: ${error.message}`, 'error');
                break;
            case 'REGION_ERROR_DEFAULT':
                showMessage(`Помилка області: ${error.message}`, 'error');
                break;
            case 'REGION_NOT_FOUND':
                showMessage(`Область не знайдено: ${error.message}`, 'error');
                break;
            case 'PRODUCT_ERROR_DEFAULT':
                showMessage(`Помилка продукту: ${error.message}`, 'error');
                break;
            case 'PRODUCT_NOT_FOUND':
                showMessage(`Продукт не знайдено: ${error.message}`, 'error');
                break;
            case 'PURCHASE_ERROR_DEFAULT':
                showMessage(`Помилка закупівлі: ${error.message}`, 'error');
                break;
            case 'PURCHASE_NOT_FOUND':
                showMessage(`Закупівлю не знайдено: ${error.message}`, 'error');
                break;
            case 'SALE_ERROR_DEFAULT':
                showMessage(`Помилка продажі: ${error.message}`, 'error');
                break;
            case 'SALE_NOT_FOUND':
                showMessage(`Продаж не знайдено: ${error.message}`, 'error');
                break;
            case 'TRANSACTION_ERROR_DEFAULT':
                showMessage(`Помилка транзакції: ${error.message}`, 'error');
                break;
            case 'TRANSACTION_NOT_FOUND':
                showMessage(`Продаж не транзакції: ${error.message}`, 'error');
                break;
            case 'USER_ERROR_DEFAULT':
                showMessage(`Помилка користувача: ${error.message}`, 'error');
                break;
            case 'USER_NOT_FOUND':
                showMessage(`Користувача не знайдено: ${error.message}`, 'error');
                break;
            case 'INVALID_JSON':
                showMessage(`Некорректный формат JSON: ${error.message}`, 'error');
                break;
            case 'SERVER_ERROR':
                showMessage('Виникла помилка на сервері.', 'error');
                break;
            default:
                showMessage(`Помилка: ${error.message}`, 'error');
        }
    } else {
        showMessage(`Неможливо виконати запит: ${error.message}`, 'error');
    }
}