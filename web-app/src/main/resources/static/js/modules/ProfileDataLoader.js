const ProfileDataLoader = (function() {
    const API_BASE = '/api/v1';

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

    async function loadProducts() {
        try {
            const response = await fetch(`${API_BASE}/product`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading products:', error);
            throw error;
        }
    }

    async function loadProductBalances(userId) {
        try {
            const response = await fetch(`${API_BASE}/driver/balances/${userId}`);
            if (!response.ok) {
                if (response.status === 404 || response.status === 403) {
                    return null;
                }
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading product balances:', error);
            throw error;
        }
    }

    async function loadAccounts(userId) {
        try {
            const response = await fetch(`${API_BASE}/accounts/user/${userId}`);
            if (!response.ok) {
                if (response.status === 404 || response.status === 403) {
                    return [];
                }
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading accounts:', error);
            throw error;
        }
    }

    async function loadAccountBalances(accountId) {
        try {
            const response = await fetch(`${API_BASE}/accounts/${accountId}/balances`);
            if (!response.ok) {
                if (response.status === 404) {
                    return [];
                }
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.warn(`Error loading balances for account ${accountId}:`, error);
            return [];
        }
    }

    async function loadAccountBalancesBatch(accountIds) {
        try {
            if (!accountIds || accountIds.length === 0) {
                return new Map();
            }
            const response = await fetch(`${API_BASE}/accounts/balances/batch`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(accountIds)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            const balancesMap = await response.json();
            return new Map(Object.entries(balancesMap).map(([id, balances]) => [Number(id), balances]));
        } catch (error) {
            console.warn('Error loading balances batch:', error);
            return new Map();
        }
    }

    async function updateProductBalance(driverId, productId, totalCostEur) {
        try {
            const response = await fetch(`${API_BASE}/driver/balances/${driverId}/product/${productId}/total-cost`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ totalCostEur })
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            }
            return true;
        } catch (error) {
            console.error('Error updating product balance:', error);
            throw error;
        }
    }

    return {
        loadUsers,
        loadProducts,
        loadProductBalances,
        loadAccounts,
        loadAccountBalances,
        loadAccountBalancesBatch,
        updateProductBalance
    };
})();
