const FinanceFilters = (function() {
    function buildTransactionFilters(customSelects) {
        const filters = {};
        
        const type = document.getElementById('transaction-type-filter')?.value;
        if (type) {
            filters.type = [type];
        }

        if (customSelects && customSelects['transaction-account-filter']) {
            const values = customSelects['transaction-account-filter'].getValue();
            if (values.length > 0) {
                filters.account_id = values;
            }
        } else {
            const accountFilter = document.getElementById('transaction-account-filter');
            if (accountFilter && accountFilter.selectedOptions.length > 0) {
                filters.account_id = Array.from(accountFilter.selectedOptions).map(opt => opt.value);
            }
        }

        if (customSelects && customSelects['transaction-category-filter']) {
            const values = customSelects['transaction-category-filter'].getValue();
            if (values.length > 0) {
                filters.category_id = values;
            }
        } else {
            const categoryFilter = document.getElementById('transaction-category-filter');
            if (categoryFilter && categoryFilter.selectedOptions.length > 0) {
                filters.category_id = Array.from(categoryFilter.selectedOptions).map(opt => opt.value);
            }
        }

        const dateFrom = document.getElementById('transaction-date-from')?.value;
        if (dateFrom) {
            filters.created_at_from = [dateFrom];
        }

        const dateTo = document.getElementById('transaction-date-to')?.value;
        if (dateTo) {
            filters.created_at_to = [dateTo];
        }

        return filters;
    }
    
    async function populateTransactionFilters(accountsCache, branchesCache, customSelects) {
        if (!customSelects) {
            customSelects = {};
        }
        
        const accountFilter = document.getElementById('transaction-account-filter');
        if (accountFilter) {
            const previousIds = customSelects[accountFilter.id] ? customSelects[accountFilter.id].getValue() : [];
            accountFilter.textContent = '';
            if (typeof createCustomSelect === 'function') {
                const selectId = accountFilter.id;
                if (!customSelects[selectId]) {
                    customSelects[selectId] = createCustomSelect(accountFilter, true);
                }
                const accountData = accountsCache.map(account => ({ id: account.id, name: account.name || '' }));
                if (accountData.length > 0 && customSelects[selectId]) {
                    customSelects[selectId].populate(accountData);
                    if (previousIds.length > 0) {
                        customSelects[selectId].setValue(previousIds.map(String));
                    }
                }
            }
        }
        
        try {
            const allCategories = await FinanceDataLoader.loadAllCategories();

            const categoryFilter = document.getElementById('transaction-category-filter');
            if (categoryFilter) {
                const previousIds = customSelects[categoryFilter.id] ? customSelects[categoryFilter.id].getValue() : [];
                categoryFilter.textContent = '';
                if (typeof createCustomSelect === 'function') {
                    const selectId = categoryFilter.id;
                    if (!customSelects[selectId]) {
                        customSelects[selectId] = createCustomSelect(categoryFilter, true);
                    }
                    const categoryData = allCategories.map(category => {
                        const typeName = FinanceUtils.getTransactionTypeName(category.type);
                        const categoryName = category.name || '';
                        return { id: category.id, name: `${typeName} - ${categoryName}` };
                    });
                    if (categoryData.length > 0 && customSelects[selectId]) {
                        customSelects[selectId].populate(categoryData);
                        if (previousIds.length > 0) {
                            customSelects[selectId].setValue(previousIds.map(String));
                        }
                    }
                }
            }
        } catch (error) {
            console.error('Error loading categories for filter:', error);
        }
        
        return customSelects;
    }
    
    function openFiltersModal(modal) {
        if (!modal) return;
        
        const allDropdowns = modal.querySelectorAll('.custom-select-dropdown.open');
        allDropdowns.forEach(dropdown => {
            dropdown.classList.remove('open');
        });
        
        document.body.style.overflow = 'hidden';
        modal.style.display = 'flex';
        setTimeout(() => {
            modal.classList.add('show');
        }, CLIENT_CONSTANTS.MODAL_ANIMATION_DELAY);
    }
    
    function closeFiltersModal(modal) {
        if (!modal) return;
        modal.classList.remove('show');
        setTimeout(() => {
            modal.style.display = 'none';
            document.body.style.overflow = '';
        }, CLIENT_CONSTANTS.MODAL_CLOSE_DELAY);
    }
    
    function clearFilters(customSelects) {
        const typeFilter = document.getElementById('transaction-type-filter');
        if (typeFilter) typeFilter.value = '';
        
        const dateFrom = document.getElementById('transaction-date-from');
        if (dateFrom) dateFrom.value = '';
        
        const dateTo = document.getElementById('transaction-date-to');
        if (dateTo) dateTo.value = '';
        
        if (customSelects) {
            Object.values(customSelects).forEach(customSelect => {
                if (customSelect && typeof customSelect.reset === 'function') {
                    customSelect.reset();
                }
            });
        }
    }
    
    return {
        buildTransactionFilters,
        populateTransactionFilters,
        openFiltersModal,
        closeFiltersModal,
        clearFilters
    };
})();
