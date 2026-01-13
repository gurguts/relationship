const FinanceFilters = (function() {
    function buildTransactionFilters(customSelects) {
        const filters = {};
        
        const type = document.getElementById('transaction-type-filter')?.value;
        if (type) {
            filters.type = [type];
        }

        let accountId = '';
        if (customSelects && customSelects['transaction-account-filter']) {
            const values = customSelects['transaction-account-filter'].getValue();
            accountId = values.length > 0 ? values[0] : '';
        } else {
            const accountFilter = document.getElementById('transaction-account-filter');
            accountId = accountFilter ? accountFilter.value : '';
        }
        if (accountId) {
            filters.account_id = [accountId];
        }

        let categoryId = '';
        if (customSelects && customSelects['transaction-category-filter']) {
            const values = customSelects['transaction-category-filter'].getValue();
            categoryId = values.length > 0 ? values[0] : '';
        } else {
            const categoryFilter = document.getElementById('transaction-category-filter');
            categoryId = categoryFilter ? categoryFilter.value : '';
        }
        if (categoryId) {
            filters.category_id = [categoryId];
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
            accountFilter.textContent = '';
            const defaultOption = document.createElement('option');
            defaultOption.value = '';
            defaultOption.textContent = 'Всі рахунки';
            accountFilter.appendChild(defaultOption);
            accountsCache.forEach(account => {
                const option = document.createElement('option');
                option.value = account.id;
                option.textContent = account.name || '';
                accountFilter.appendChild(option);
            });
            
            if (typeof createCustomSelect === 'function') {
                const selectId = accountFilter.id;
                if (!customSelects[selectId]) {
                    customSelects[selectId] = createCustomSelect(accountFilter, true);
                }
                const accountData = accountsCache.map(account => ({ id: account.id, name: account.name || '' }));
                if (customSelects[selectId]) {
                    customSelects[selectId].populate(accountData);
                }
            }
        }
        
        try {
            const allCategories = await FinanceDataLoader.loadAllCategories();

            const categoryFilter = document.getElementById('transaction-category-filter');
            if (categoryFilter) {
                categoryFilter.textContent = '';
                const defaultOption = document.createElement('option');
                defaultOption.value = '';
                defaultOption.textContent = 'Всі категорії';
                categoryFilter.appendChild(defaultOption);
                allCategories.forEach(category => {
                    const option = document.createElement('option');
                    option.value = category.id;
                    const typeName = FinanceUtils.getTransactionTypeName(category.type);
                    const categoryName = category.name || '';
                    option.textContent = `${typeName} - ${categoryName}`;
                    categoryFilter.appendChild(option);
                });
                
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
                    if (customSelects[selectId]) {
                        customSelects[selectId].populate(categoryData);
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
