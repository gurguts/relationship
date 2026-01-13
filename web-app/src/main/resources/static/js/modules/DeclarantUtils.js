const DeclarantUtils = (function() {
    function escapeHtml(text) {
        if (text == null) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function formatNumber(value, maxDecimals = 6) {
        if (value === null || value === undefined || value === '') return '0';
        const num = parseFloat(value);
        if (isNaN(num)) return '0';
        return parseFloat(num.toFixed(maxDecimals)).toString();
    }

    function formatDate(dateString) {
        if (!dateString) return '-';
        return dateString;
    }

    function formatBoolean(value) {
        return value ? '✓' : '-';
    }

    function findNameByIdFromMap(map, id) {
        const numericId = Number(id);
        return map.get(numericId) || '';
    }

    return {
        escapeHtml,
        formatNumber,
        formatDate,
        formatBoolean,
        findNameByIdFromMap
    };
})();
