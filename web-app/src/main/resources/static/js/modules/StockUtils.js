const StockUtils = (function() {
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

    function findNameByIdFromMap(map, id) {
        const numericId = Number(id);
        return map.get(numericId) || '';
    }

    function formatDate(dateString) {
        if (!dateString) return '';
        const date = new Date(dateString);
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        return `${day}.${month}.${year}`;
    }

    return {
        escapeHtml,
        formatNumber,
        findNameByIdFromMap,
        formatDate
    };
})();
