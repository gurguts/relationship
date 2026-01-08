const ClientUtils = (function() {
    function findNameByIdFromMap(map, id) {
        const numericId = Number(id);
        const name = map.get(numericId);
        return name || '';
    }
    
    function escapeHtml(text) {
        if (text == null) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    function createEmptyCellSpan() {
        const emptySpan = document.createElement('span');
        emptySpan.style.color = '#999';
        emptySpan.style.fontStyle = 'italic';
        emptySpan.textContent = CLIENT_MESSAGES.EMPTY_VALUE;
        return emptySpan;
    }
    
    function debounce(func, delay) {
        let timeout;
        return function(...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), delay);
        };
    }
    
    function normalizeFilterKeys(filters, staticFilterKeys = [], validFieldNames = new Set()) {
        const normalizedFilters = {};
        Object.keys(filters).forEach(key => {
            const normalizedKey = key.toLowerCase();
            const normalizedStaticKeys = staticFilterKeys.map(k => k.toLowerCase());
            
            if (normalizedStaticKeys.includes(normalizedKey) || 
                normalizedKey === 'source' ||
                normalizedKey.endsWith('from') || 
                normalizedKey.endsWith('to') ||
                normalizedKey === 'createdatfrom' || 
                normalizedKey === 'createdatto' ||
                normalizedKey === 'updatedatfrom' || 
                normalizedKey === 'updatedatto') {
                const value = filters[key];
                if (value !== null && value !== undefined && value !== '' && 
                    !(Array.isArray(value) && value.length === 0)) {
                    normalizedFilters[normalizedKey] = value;
                }
            } else if (validFieldNames.size === 0 || validFieldNames.has(key)) {
                const value = filters[key];
                if (value !== null && value !== undefined && value !== '' && 
                    !(Array.isArray(value) && (value.length === 0 || (value.length === 1 && (value[0] === '' || value[0] === 'null'))))) {
                    normalizedFilters[key] = value;
                }
            }
        });
        return normalizedFilters;
    }
    
    return {
        findNameByIdFromMap,
        escapeHtml,
        createEmptyCellSpan,
        debounce,
        normalizeFilterKeys
    };
})();
