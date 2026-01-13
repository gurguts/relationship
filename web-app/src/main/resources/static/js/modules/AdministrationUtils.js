const AdministrationUtils = (function() {
    function escapeHtml(text) {
        if (text === null || text === undefined) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function formatPermissionName(permission) {
        const permissionMap = {
            'system:admin': 'system:admin',
            'user:create': 'Створення користувачів',
            'user:edit': 'Зміна користувачів',
            'user:delete': 'Видалення користувачів',
            'profile:edit': 'Зміна в профілі',
            'profile:multiple_view': 'Перегляд кількох профілів',
            'client:view': 'Відображення клієнтів',
            'client:create': 'Створення клієнтів',
            'client:edit': 'Редагування клієнтів',
            'client:delete': 'Видалення клієнтів',
            'client_stranger:edit': 'Редагування чужих клієнтів та зміна залучення',
            'client:full_delete': 'Повне видалення клієнта',
            'client:excel': 'Вивантаження клієнтів в excel',
            'sale:view': 'Відображення продаж',
            'sale:create': 'Створення продаж',
            'sale:edit': 'Зміна продаж',
            'sale:edit_strangers': 'Редагування чужих продаж',
            'sale:delete': 'Видалення продаж',
            'sale:edit_source': 'Зміна залучення продаж',
            'sale:excel': 'Вивантаження продаж в excel',
            'purchase:view': 'Відображення закупівель',
            'purchase:create': 'Створення закупівель',
            'purchase:edit': 'Зміна закупівель',
            'purchase:edit_strangers': 'Редагування чужих закупівель',
            'purchase:delete': 'Видалення закупівель',
            'purchase:edit_source': 'Зміна залучення закупівель',
            'purchase:excel': 'Вивантаження закупівель в excel',
            'container:view': 'Відображення тари',
            'container:transfer': 'Передача тари',
            'container:balance': 'Зміни балансу тари',
            'container:excel': 'Вивантаження тари в excel',
            'finance:view': 'Відображення фінансів',
            'finance:balance_edit': 'Зміна балансу фінансів',
            'finance:transfer_view': 'Перегляд транзакцій',
            'finance:transfer_excel': 'Вивантаження в excel транзакцій',
            'transaction:delete': 'Видалення закупівель і продаж',
            'warehouse:view': 'Відображення складу',
            'warehouse:create': 'Створення приходу на склад',
            'warehouse:edit': 'Зміна приходу на склад',
            'warehouse:withdraw': 'Зняття балансу складу',
            'warehouse:delete': 'Видалення приходу на склад',
            'warehouse:excel': 'Вивантаження в excel даних зі складу',
            'inventory:view': 'Відображення балансу тари',
            'inventory:manage': 'Управління балансом тари',
            'declarant:view': 'Відображення сторінки декларантів',
            'declarant:create': 'Створення на сторінці декларантів',
            'declarant:edit': 'Редагування на сторінці декларантів',
            'declarant:delete': 'Видалення на сторінці декларантів',
            'declarant:excel': 'Експорт в excel на сторінці декларантів',
            'analytics:view': 'Відображення аналітики',
            'settings:view': 'Відображення налаштувань',
            'settings_client:create': 'Дозвіл створювати поля для клієнта',
            'settings_client:edit': 'Дозвіл редагувати поля для клієнта',
            'settings_client:delete': 'Дозвіл видаляти поля для клієнта',
            'settings_finance:create': 'Дозвіл створювати поля для фінансів',
            'settings_finance:edit': 'Дозвіл змінювати поля для фінансів',
            'settings_exchange:edit': 'Дозвіл змінювати курс валют',
            'settings_finance:delete': 'Дозвіл видаляти поля для фінансів',
            'settings_product:create': 'Створення і редагування товару закупівлі і складу',
            'settings_product:delete': 'Видалення закупівлі і складу',
            'settings_declarant:create': 'Створення і редагування полів машини декларанта',
            'settings_declarant:delete': 'Видалення полів машини декларанта',
            'administration:view': 'Відображення адміністраторської',
            'administration:edit': 'Редагування в адміністраторській'
        };
        return permissionMap[permission] || permission;
    }

    function getFieldTypeLabel(type) {
        const labels = {
            'TEXT': 'Текст',
            'NUMBER': 'Число',
            'DATE': 'Дата',
            'PHONE': 'Телефон',
            'LIST': 'Список',
            'BOOLEAN': 'Так/Ні'
        };
        return labels[type] || type;
    }

    function getPurposeLabel(purpose) {
        const labels = {
            'REMOVING': 'Сняття',
            'ADDING': 'Поповнення',
            'BOTH': 'Загальний'
        };
        return labels[purpose] || purpose;
    }

    function getUsageLabel(usage) {
        const labels = {
            'SALE_ONLY': 'Тільки продаж',
            'PURCHASE_ONLY': 'Тільки закупівля',
            'BOTH': 'Обидва'
        };
        return labels[usage] || usage;
    }

    return {
        escapeHtml,
        formatPermissionName,
        getFieldTypeLabel,
        getPurposeLabel,
        getUsageLabel
    };
})();
