const FinanceFilters = (function() {
    function buildTransactionFilters() {
        const filters = {};
        
        const type = document.getElementById('transaction-type-filter')?.value;
        if (type) {
            filters.type = [type];
        }

        const accountId = document.getElementById('transaction-account-filter')?.value;
        if (accountId) {
            filters.account_id = [accountId];
        }

        const categoryId = document.getElementById('transaction-category-filter')?.value;
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
    
    async function populateTransactionFilters(accountsCache, branchesCache) {
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
            }
        } catch (error) {
            console.error('Error loading categories for filter:', error);
        }
    }
    
    return {
        buildTransactionFilters,
        populateTransactionFilters
    };
})();
