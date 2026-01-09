const StockFilters = (function() {
    function setDefaultHistoryDates() {
        const today = new Date().toISOString().split('T')[0];
        const fromInput = document.getElementById('history-withdrawal-date-from-filter');
        const toInput = document.getElementById('history-withdrawal-date-to-filter');
        if (fromInput) fromInput.value = today;
        if (toInput) toInput.value = today;
    }
    
    function setDefaultEntriesDates() {
        const today = new Date().toISOString().split('T')[0];
        const fromInput = document.getElementById('entries-entry-date-from-filter');
        const toInput = document.getElementById('entries-entry-date-to-filter');
        if (fromInput) fromInput.value = today;
        if (toInput) toInput.value = today;
    }
    
    function setDefaultTransfersDates() {
        const today = new Date();
        const fromDate = new Date();
        fromDate.setDate(today.getDate() - 30);
        const formattedToday = today.toISOString().split('T')[0];
        const formattedFrom = fromDate.toISOString().split('T')[0];
        
        const fromInput = document.getElementById('transfer-date-from-filter');
        const toInput = document.getElementById('transfer-date-to-filter');
        
        if (fromInput && !fromInput.value) {
            fromInput.value = formattedFrom;
        }
        if (toInput && !toInput.value) {
            toInput.value = formattedToday;
        }
    }
    
    function buildHistoryFilters(historyCustomSelects) {
        const filters = {};
        
        Object.keys(historyCustomSelects).forEach(selectId => {
            const selectElement = document.getElementById(selectId);
            if (!selectElement) return;
            const name = selectElement.name;
            const values = historyCustomSelects[selectId].getValue();
            if (values.length > 0) {
                filters[name] = values;
            }
        });
        
        const withdrawalDateFrom = document.getElementById('history-withdrawal-date-from-filter')?.value;
        const withdrawalDateTo = document.getElementById('history-withdrawal-date-to-filter')?.value;
        
        if (withdrawalDateFrom) {
            filters['withdrawal_date_from'] = [withdrawalDateFrom];
        }
        if (withdrawalDateTo) {
            filters['withdrawal_date_to'] = [withdrawalDateTo];
        }
        
        return filters;
    }
    
    function buildEntriesFilters(entriesCustomSelects) {
        const filters = {};
        
        Object.keys(entriesCustomSelects).forEach(selectId => {
            const selectElement = document.getElementById(selectId);
            if (!selectElement) return;
            const name = selectElement.name;
            const values = entriesCustomSelects[selectId].getValue();
            if (values.length > 0) {
                filters[name] = values;
            }
        });
        
        const entryDateFrom = document.getElementById('entries-entry-date-from-filter')?.value;
        const entryDateTo = document.getElementById('entries-entry-date-to-filter')?.value;
        
        if (entryDateFrom) {
            filters['entry_date_from'] = [entryDateFrom];
        }
        if (entryDateTo) {
            filters['entry_date_to'] = [entryDateTo];
        }
        
        return filters;
    }
    
    function buildTransfersFilters(transfersCustomSelects) {
        const dateFromInput = document.getElementById('transfer-date-from-filter');
        const dateToInput = document.getElementById('transfer-date-to-filter');
        
        const warehouseValues = transfersCustomSelects['transfer-warehouse-filter']
            ? transfersCustomSelects['transfer-warehouse-filter'].getValue()
            : [];
        const fromProductValues = transfersCustomSelects['transfer-from-product-filter']
            ? transfersCustomSelects['transfer-from-product-filter'].getValue()
            : [];
        const toProductValues = transfersCustomSelects['transfer-to-product-filter']
            ? transfersCustomSelects['transfer-to-product-filter'].getValue()
            : [];
        const userValues = transfersCustomSelects['transfer-user-filter']
            ? transfersCustomSelects['transfer-user-filter'].getValue()
            : [];
        const reasonValues = transfersCustomSelects['transfer-reason-filter']
            ? transfersCustomSelects['transfer-reason-filter'].getValue()
            : [];
        
        return {
            dateFrom: dateFromInput?.value || null,
            dateTo: dateToInput?.value || null,
            warehouseId: warehouseValues[0] || null,
            fromProductId: fromProductValues[0] || null,
            toProductId: toProductValues[0] || null,
            userId: userValues[0] || null,
            reasonId: reasonValues[0] || null
        };
    }
    
    function buildDiscrepanciesFilters() {
        const type = document.getElementById('discrepancy-type-filter')?.value;
        const dateFrom = document.getElementById('discrepancy-date-from')?.value;
        const dateTo = document.getElementById('discrepancy-date-to')?.value;
        
        const filters = {};
        if (type) filters.type = type;
        if (dateFrom) filters.dateFrom = dateFrom;
        if (dateTo) filters.dateTo = dateTo;
        
        return filters;
    }
    
    function updateFilterCounter(filters, counterElementId, countElementId) {
        const counterElement = document.getElementById(counterElementId);
        const countElement = document.getElementById(countElementId);
        
        if (!counterElement || !countElement) return;
        
        let totalFilters = 0;
        totalFilters += Object.values(filters)
            .filter(value => Array.isArray(value))
            .reduce((count, values) => count + values.length, 0);
        
        if (totalFilters > 0) {
            countElement.textContent = totalFilters;
            counterElement.style.display = 'inline-flex';
        } else {
            counterElement.style.display = 'none';
        }
    }
    
    function updateTransfersFilterCounter(filters) {
        const counterElement = document.getElementById('transfers-filter-counter');
        const countElement = document.getElementById('transfers-filter-count');
        
        if (!counterElement || !countElement) return;
        
        const activeFilters = Object.values(filters).filter(Boolean);
        if (activeFilters.length > 0) {
            countElement.textContent = activeFilters.length;
            counterElement.style.display = 'inline-flex';
        } else {
            counterElement.style.display = 'none';
        }
    }
    
    function normalizeFilters(filters) {
        const normalized = {};
        Object.keys(filters).forEach(key => {
            if (Array.isArray(filters[key]) && filters[key].length > 0) {
                normalized[key] = filters[key];
            }
        });
        return normalized;
    }
    
    return {
        setDefaultHistoryDates,
        setDefaultEntriesDates,
        setDefaultTransfersDates,
        buildHistoryFilters,
        buildEntriesFilters,
        buildTransfersFilters,
        buildDiscrepanciesFilters,
        updateFilterCounter,
        updateTransfersFilterCounter,
        normalizeFilters
    };
})();
