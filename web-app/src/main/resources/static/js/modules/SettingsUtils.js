const SettingsUtils = (function() {
    function escapeHtml(text) {
        if (text === null || text === undefined) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    function formatDate(date) {
        if (!date) return '-';
        return new Date(date).toLocaleDateString('uk-UA');
    }
    
    function formatBoolean(value) {
        return value ? '✓' : '-';
    }
    
    return {
        escapeHtml,
        formatDate,
        formatBoolean
    };
})();
