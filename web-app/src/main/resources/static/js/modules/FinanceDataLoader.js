const FinanceDataLoader = (function() {
    const API_BASE = '/api/v1';
    
    async function loadAccounts() {
        try {
            const response = await fetch(`${API_BASE}/accounts`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading accounts:', error);
            throw error;
        }
    }
    
    async function loadBranches() {
        try {
            const response = await fetch(`${API_BASE}/branches`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading branches:', error);
            throw error;
        }
    }
    
    async function loadAccountBalances(accountId) {
        try {
            const response = await fetch(`${API_BASE}/accounts/${accountId}/balances`);
            if (!response.ok) return [];
            return await response.json();
        } catch (error) {
            console.warn(`Failed to load balances for account ${accountId}`, error);
            return [];
        }
    }
    
    async function loadBranchAccounts(branchId) {
        try {
            const response = await fetch(`${API_BASE}/accounts/branch/${branchId}`);
            if (!response.ok) return [];
            return await response.json();
        } catch (error) {
            console.warn(`Failed to load accounts for branch ${branchId}`, error);
            return [];
        }
    }
    
    async function loadTransactions(page, size, sort, direction, filters) {
        try {
            const filtersJson = JSON.stringify(filters);
            const params = new URLSearchParams({
                page: page.toString(),
                size: size.toString(),
                sort: sort,
                direction: direction,
                filters: filtersJson
            });

            const response = await fetch(`${API_BASE}/transaction/search?${params}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            return await response.json();
        } catch (error) {
            console.error('Error loading transactions:', error);
            throw error;
        }
    }
    
    async function loadTransaction(transactionId) {
        try {
            const response = await fetch(`${API_BASE}/transaction/${transactionId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading transaction:', error);
            throw error;
        }
    }
    
    async function createTransaction(transactionData) {
        try {
            const response = await fetch(`${API_BASE}/transaction`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(transactionData)
            });
            
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const text = await response.text();
                if (text.trim()) {
                    return JSON.parse(text);
                }
            }
            return true;
        } catch (error) {
            console.error('Error creating transaction:', error);
            throw error;
        }
    }
    
    async function updateTransaction(transactionId, updateData) {
        try {
            const response = await fetch(`${API_BASE}/transaction/${transactionId}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(updateData)
            });
            
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const text = await response.text();
                if (text.trim()) {
                    return JSON.parse(text);
                }
            }
            return true;
        } catch (error) {
            console.error('Error updating transaction:', error);
            throw error;
        }
    }
    
    async function deleteTransaction(transactionId) {
        try {
            const response = await fetch(`${API_BASE}/transaction/${transactionId}`, {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' }
            });
            
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            return true;
        } catch (error) {
            console.error('Error deleting transaction:', error);
            throw error;
        }
    }
    
    async function exportTransactions(filters) {
        try {
            const filtersJson = JSON.stringify(filters);
            const params = new URLSearchParams({
                filters: filtersJson
            });

            const response = await fetch(`${API_BASE}/transaction/export?${params}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            return await response.blob();
        } catch (error) {
            console.error('Error exporting transactions:', error);
            throw error;
        }
    }
    
    async function loadCategoriesForType(type) {
        if (!type) return [];
        try {
            const response = await fetch(`${API_BASE}/transaction-categories/type/${type}`);
            if (!response.ok) return [];
            const categories = await response.json();
            return Array.isArray(categories) ? categories : [];
        } catch (error) {
            console.error('Error loading categories:', error);
            return [];
        }
    }
    
    async function loadAllCategories() {
        try {
            const types = ['INTERNAL_TRANSFER', 'EXTERNAL_INCOME', 'EXTERNAL_EXPENSE', 'CLIENT_PAYMENT', 'CURRENCY_CONVERSION'];
            const promises = types.map(type => 
                fetch(`${API_BASE}/transaction-categories/type/${type}`)
                    .then(r => r.ok ? r.json() : [])
                    .catch(() => [])
            );
            const results = await Promise.all(promises);
            return results.flat();
        } catch (error) {
            console.error('Error loading all categories:', error);
            return [];
        }
    }
    
    async function loadCounterparties(type) {
        try {
            const response = await fetch(`${API_BASE}/counterparties/type/${type}`);
            if (!response.ok) return [];
            const counterparties = await response.json();
            return Array.isArray(counterparties) ? counterparties : [];
        } catch (error) {
            console.error('Error loading counterparties:', error);
            return [];
        }
    }
    
    async function loadExchangeRates() {
        try {
            const response = await fetch(`${API_BASE}/exchange-rates`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading exchange rates:', error);
            throw error;
        }
    }
    
    async function updateExchangeRate(currency, rate) {
        try {
            const response = await fetch(`${API_BASE}/exchange-rates/${currency}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    fromCurrency: currency,
                    rate: rate
                })
            });
            
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const text = await response.text();
                if (text.trim()) {
                    return JSON.parse(text);
                }
            }
            return true;
        } catch (error) {
            console.error('Error updating exchange rate:', error);
            throw error;
        }
    }
    
    async function loadUsers() {
        try {
            const response = await fetch(`${API_BASE}/user`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading users:', error);
            throw error;
        }
    }
    
    async function loadClients() {
        try {
            const response = await fetch('/api/v1/client/search?size=1000');
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            const data = await response.json();
            return data.content || [];
        } catch (error) {
            console.error('Error loading clients:', error);
            throw error;
        }
    }
    
    return {
        loadAccounts,
        loadBranches,
        loadAccountBalances,
        loadBranchAccounts,
        loadTransactions,
        loadTransaction,
        createTransaction,
        updateTransaction,
        deleteTransaction,
        exportTransactions,
        loadCategoriesForType,
        loadAllCategories,
        loadCounterparties,
        loadExchangeRates,
        updateExchangeRate,
        loadUsers,
        loadClients
    };
})();
