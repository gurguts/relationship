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
