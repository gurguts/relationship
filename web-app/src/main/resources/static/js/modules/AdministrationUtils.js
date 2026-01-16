const AdministrationUtils = (function() {
    function escapeHtml(text) {
        if (text === null || text === undefined) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function formatPermissionName(permission) {
        return permission;
    }

    function getFieldTypeLabel(type) {
        const labels = {
            'TEXT': 'Текст',
            'NUMBER': 'Число',
            'DATE': 'Дата',
            'PHONE': 'Телефон',
            'LIST': 'Список',
            'BOOLEAN': 'Да/Нет'
        };
        return labels[type] || type;
    }

    function getPurposeLabel(purpose) {
        const labels = {
            'REMOVING': 'Списание',
            'ADDING': 'Пополнение',
            'BOTH': 'Оба'
        };
        return labels[purpose] || purpose;
    }

    function getUsageLabel(usage) {
        const labels = {
            'SALE_ONLY': 'Только продажа',
            'PURCHASE_ONLY': 'Только закупка',
            'BOTH': 'Оба'
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
