const CLIENT_CONSTANTS = {
    DEFAULT_PAGE_SIZE: 50,
    SEARCH_DEBOUNCE_DELAY: 400,
    MIN_COLUMN_WIDTH: 50,
    DEFAULT_COLUMN_WIDTH: 200,
    DEFAULT_DYNAMIC_COLUMN_WIDTH: 100,
    PHONE_MAX_LENGTH: 15,
    MODAL_ANIMATION_DELAY: 10,
    MODAL_CLOSE_DELAY: 200,
    FILTER_DEBOUNCE_DELAY: 200,
    CUSTOM_SELECT_DEBOUNCE_DELAY: 200
};

const CLIENT_MESSAGES = {
    PARAMETER_NOT_SET: 'Параметр не задано',
    SEARCH_PLACEHOLDER: 'Пошук...',
    SELECT_PLACEHOLDER: 'Виберіть параметр',
    SELECT_OPTION: 'Виберіть...',
    YES: 'Так',
    NO: 'Ні',
    ALL: 'Всі',
    EMPTY_VALUE: '—',
    LOADING: 'Завантаження...',
    NO_DATA: 'Немає даних',
    SAVE_OR_CANCEL: 'Збережіть або відмініть зміни',
    CLIENT_EDITED: 'Клієнт успішно відредагований',
    CLIENT_CREATED: 'Клієнт з ID: {id} успішно створений',
    SELECT_CLIENT_TYPE: 'Будь ласка, виберіть тип клієнта з навігації',
    FIX_PHONE_ERRORS: 'Будь ласка, виправте помилки в полях телефонів',
    EXPORT_SUCCESS: 'Дані успішно експортовані в Excel',
    NO_PURCHASES: 'Немає закупівель',
    NO_CONTAINERS: 'Немає тар',
    LOAD_ERROR: 'Помилка завантаження',
    PHONE_INPUT_PLACEHOLDER: 'Введіть номери телефонів через кому',
    FROM: 'Від:',
    TO: 'До:',
    MIN: 'Мінімум',
    MAX: 'Максимум',
    CREATED_AT_LABEL: 'Дата створення:',
    UPDATED_AT_LABEL: 'Дата оновлення:',
    SOURCE_LABEL: 'Залучення:',
    SHOW_INACTIVE_LABEL: 'Показати неактивних клієнтів'
};

const CLIENT_FIELD_TYPES = {
    TEXT: 'TEXT',
    PHONE: 'PHONE',
    NUMBER: 'NUMBER',
    DATE: 'DATE',
    LIST: 'LIST',
    BOOLEAN: 'BOOLEAN'
};

const CLIENT_SORT_FIELDS = {
    COMPANY: 'company',
    SOURCE: 'sourceId',
    CREATED_AT: 'createdAt',
    UPDATED_AT: 'updatedAt'
};

const CLIENT_SORT_DIRECTIONS = {
    ASC: 'ASC',
    DESC: 'DESC'
};

const CLIENT_STATIC_FIELDS = {
    COMPANY: 'company',
    SOURCE: 'source',
    CREATED_AT: 'createdAt',
    UPDATED_AT: 'updatedAt'
};

const CLIENT_FILTER_KEYS = {
    CREATED_AT_FROM: 'createdAtFrom',
    CREATED_AT_TO: 'createdAtTo',
    UPDATED_AT_FROM: 'updatedAtFrom',
    UPDATED_AT_TO: 'updatedAtTo',
    SOURCE: 'source',
    SHOW_INACTIVE: 'showInactive'
};

const PROFILE_MESSAGES = {
    USER_NOT_AUTHORIZED: 'Користувач не авторизований',
    NO_PRODUCT_BALANCE: 'Немає балансу товару',
    NO_ACCESS_TO_BALANCE: 'Немає доступу до балансу товару',
    NO_ACCOUNTS: 'Немає фінансових рахунків',
    NO_ACCESS_TO_ACCOUNTS: 'Немає доступу до фінансових рахунків',
    NO_ACCOUNT_BALANCES: 'Немає балансів',
    LOAD_BALANCE_ERROR: 'Помилка завантаження балансу товару',
    LOAD_ACCOUNTS_ERROR: 'Помилка завантаження рахунків',
    MY_PROFILE: 'Мій профіль',
    USER_PROFILE: 'Профіль: ',
    USER_LABEL: 'Користувач #',
    INVALID_VALUE: 'Введіть коректне значення',
    BALANCE_UPDATED: 'Баланс успішно оновлено',
    UPDATE_BALANCE_ERROR: 'Помилка оновлення балансу',
    NO_ACCOUNT_NAME: 'Без назви'
};

const FINANCE_MESSAGES = {
    TRANSACTION_CREATED: 'Транзакцію успішно створено',
    TRANSACTION_UPDATED: 'Транзакцію успішно оновлено',
    TRANSACTION_DELETED: 'Транзакцію успішно видалено',
    TRANSACTIONS_EXPORTED: 'Транзакції успішно експортовано',
    EXCHANGE_RATE_UPDATED: 'Курс валют успішно оновлено',
    DELETE_CONFIRMATION: 'Ви впевнені, що хочете видалити цю транзакцію? Гроші будуть повернуті.',
    SPECIFY_RECEIVED_AMOUNT: 'Вкажіть суму зачислення',
    SELECT_CLIENT: 'Виберіть клієнта',
    RECEIVED_AMOUNT_TOO_LARGE: 'Сума зачислення не може бути більшою за суму списання',
    RECEIVED_AMOUNT_MUST_BE_POSITIVE: 'Сума зачислення повинна бути більше нуля',
    WRITE_OFF_AMOUNT_MUST_BE_POSITIVE: 'Сума списання повинна бути більше нуля',
    NO_ACCOUNT_PERMISSION: 'У вас немає прав на виконання операцій з цим рахунком',
    WITHOUT_CATEGORY: 'Без категорії',
    WITHOUT_COUNTERPARTY: 'Без контрагента',
    SELECT_ACCOUNT: 'Виберіть рахунок',
    SELECT_CURRENCY: 'Виберіть валюту',
    DELETE_CONFIRMATION_TITLE: 'Підтвердження видалення',
    DELETE: 'Видалити',
    CANCEL: 'Відмінити'
};

const CONFIRMATION_MESSAGES = {
    DELETE_COUNTERPARTY: 'Ви впевнені, що хочете видалити цього контрагента? Ця дія незворотна.',
    DELETE_CATEGORY: 'Ви впевнені, що хочете видалити цю категорію? Ця дія незворотна.',
    DELETE_BRANCH: 'Ви впевнені, що хочете видалити цю філію? Ця дія незворотна.',
    DELETE_ACCOUNT: 'Ви впевнені, що хочете видалити цей рахунок? Ця дія незворотна.',
    DELETE_VEHICLE_SENDER: 'Ви впевнені, що хочете видалити цього відправника? Ця дія незворотна.',
    DELETE_VEHICLE_RECEIVER: 'Ви впевнені, що хочете видалити цього отримувача? Ця дія незворотна.',
    DELETE_VEHICLE_TERMINAL: 'Ви впевнені, що хочете видалити цей термінал? Ця дія незворотна.',
    DELETE_VEHICLE_DESTINATION_COUNTRY: 'Ви впевнені, що хочете видалити цю країну? Ця дія незворотна.',
    DELETE_VEHICLE_DESTINATION_PLACE: 'Ви впевнені, що хочете видалити це місце? Ця дія незворотна.',
    DELETE_USER: 'Ви впевнені, що хочете видалити цього користувача? Ця дія незворотна.',
    DELETE_BARREL_TYPE: 'Ви впевнені, що хочете видалити цей тип тари? Ця дія незворотна.',
    DELETE_STORAGE: 'Ви впевнені, що хочете видалити цей склад? Ця дія незворотна.',
    DELETE_WITHDRAWAL_REASON: 'Ви впевнені, що хочете видалити цю причину списання? Ця дія незворотна.',
    DELETE_PRODUCT: 'Ви впевнені, що хочете видалити цей продукт? Ця дія незворотна.',
    DELETE_SOURCE: 'Ви впевнені, що хочете видалити це джерело? Ця дія незворотна.',
    DELETE_CLIENT_TYPE: 'Ви впевнені, що хочете видалити цей тип клієнта? Ця дія незворотна.',
    DELETE_FIELD: 'Ви впевнені, що хочете видалити це поле? Ця дія незворотна.',
    DELETE_CLIENT_TYPE_ACCESS: 'Ви впевнені, що хочете видалити доступ до цього типу клієнта?',
    DELETE_PERMISSIONS: 'Ви впевнені, що хочете видалити ці права доступу? Ця дія незворотна.',
    FULL_DELETE_CLIENT: 'Ви впевнені, що хочете повністю видалити цього клієнта з бази даних? Ця дія незворотна!',
    DEACTIVATE_CLIENT: 'Ви впевнені, що хочете деактивувати цього клієнта? Клієнт буде прихований, але залишиться в базі даних.',
    RESTORE_CLIENT: 'Ви впевнені, що хочете відновити цього клієнта? Клієнт знову стане активним.',
    DELETE_PURCHASE: 'Ви впевнені, що хочете видалити цей запис?',
    DELETE_VEHICLE: 'Ви впевнені, що хочете видалити цю машину?',
    DELETE_CARRIER: 'Ви впевнені, що хочете видалити цього перевізника?',
    DELETE_BALANCE: 'Ви впевнені, що хочете повністю видалити списання?',
    DELETE_PRODUCT_FROM_VEHICLE: 'Ви впевнені, що хочете повністю видалити товар з машини?',
    CANCEL_TRANSFER: 'Ви впевнені, що хочете повністю скасувати переміщення?',
    INCLUDE_REGIONS_IN_REPORT: 'Включити області у звіт?',
    CANNOT_DELETE_PURCHASE: 'Неможливо видалити закупку, оскільки товар вже прийнято кладовщиком.',
    CANNOT_EDIT_PURCHASE: 'Неможливо редагувати закупку, оскільки товар вже прийнято кладовщиком.',
    CONFIRMATION_TITLE: 'Підтвердження',
    CONFIRM: 'Підтвердити',
    CANCEL: 'Відмінити'
};
