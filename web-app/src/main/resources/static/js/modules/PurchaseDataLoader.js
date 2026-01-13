const PurchaseDataLoader = (function() {
    const API_URL_PURCHASE = '/api/v1/purchase';
    const API_URL_CLIENT = '/api/v1/client';
    
    async function loadClientType(typeId) {
        try {
            const response = await fetch(`/api/v1/client-type/${typeId}`);
            if (!response.ok) throw new Error('Failed to load client type');
            return await response.json();
        } catch (error) {
            console.error('Error loading client type:', error);
            throw error;
        }
    }
    
    async function loadClientTypeFields(typeId) {
        try {
            const [fieldsRes, visibleRes, filterableRes, visibleInCreateRes] = await Promise.all([
                fetch(`/api/v1/client-type/${typeId}/field`),
                fetch(`/api/v1/client-type/${typeId}/field/visible`),
                fetch(`/api/v1/client-type/${typeId}/field/filterable`),
                fetch(`/api/v1/client-type/${typeId}/field/visible-in-create`)
            ]);
            
            return {
                all: await fieldsRes.json(),
                visible: await visibleRes.json(),
                filterable: await filterableRes.json(),
                visibleInCreate: await visibleInCreateRes.json()
            };
        } catch (error) {
            console.error('Error loading fields:', error);
            throw error;
        }
    }
    
    async function loadEntities() {
        try {
            const response = await fetch('/api/v1/entities');
            if (!response.ok) return null;
            return await response.json();
        } catch (error) {
            console.error('Error loading entities:', error);
            return null;
        }
    }
    
    async function loadPurchases(params) {
        try {
            let queryParams = [];
            Object.keys(params).forEach(key => {
                const value = params[key];
                if (value !== null && value !== undefined && value !== '') {
                    queryParams.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`);
                }
            });
            
            const queryString = queryParams.join('&');
            const response = await fetch(`${API_URL_PURCHASE}/search?${queryString}`);
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new ErrorResponse(errorData.error, errorData.message, errorData.details);
            }
            
            return await response.json();
        } catch (error) {
            console.error('Error loading purchases:', error);
            throw error;
        }
    }
    
    async function updatePurchase(purchaseId, purchaseData) {
        try {
            const response = await fetch(`${API_URL_PURCHASE}/${purchaseId}`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(purchaseData)
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new ErrorResponse(errorData.error, errorData.message, errorData.details);
            }
            
            return await response.json();
        } catch (error) {
            console.error('Error updating purchase:', error);
            throw error;
        }
    }
    
    async function deletePurchase(purchaseId) {
        try {
            const response = await fetch(`${API_URL_PURCHASE}/${purchaseId}`, {
                method: 'DELETE'
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new ErrorResponse(errorData.error, errorData.message, errorData.details);
            }
            
            return true;
        } catch (error) {
            console.error('Error deleting purchase:', error);
            throw error;
        }
    }
    
    async function deleteClient(clientId) {
        try {
            const response = await fetch(`${API_URL_CLIENT}/${clientId}`, {
                method: 'DELETE'
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new ErrorResponse(errorData.error, errorData.message, errorData.details);
            }
            
            return true;
        } catch (error) {
            console.error('Error deleting client:', error);
            throw error;
        }
    }
    
    async function deleteClientActive(clientId) {
        try {
            const response = await fetch(`${API_URL_CLIENT}/active/${clientId}`, {
                method: 'DELETE'
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new ErrorResponse(errorData.error, errorData.message, errorData.details);
            }
            
            return true;
        } catch (error) {
            console.error('Error deleting client active:', error);
            throw error;
        }
    }
    
    async function restoreClient(clientId) {
        try {
            const response = await fetch(`${API_URL_CLIENT}/active/${clientId}`, {
                method: 'PATCH'
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new ErrorResponse(errorData.error, errorData.message, errorData.details);
            }
            
            return await response.json();
        } catch (error) {
            console.error('Error restoring client:', error);
            throw error;
        }
    }
    
    async function loadReport(query, filters) {
        try {
            let queryParams = [];
            
            if (query) {
                queryParams.push(`q=${encodeURIComponent(query)}`);
            }
            
            if (filters && Object.keys(filters).length > 0) {
                queryParams.push(`filters=${encodeURIComponent(JSON.stringify(filters))}`);
            }
            
            const url = `${API_URL_PURCHASE}/report${queryParams.length > 0 ? '?' + queryParams.join('&') : ''}`;
            const response = await fetch(url);
            
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Failed to load report' }));
                throw new Error(errorData.message || 'Failed to load report');
            }
            
            return await response.json();
        } catch (error) {
            console.error('Error loading report:', error);
            throw error;
        }
    }
    
    return {
        loadClientType,
        loadClientTypeFields,
        loadEntities,
        loadPurchases,
        updatePurchase,
        deletePurchase,
        deleteClient,
        deleteClientActive,
        restoreClient,
        loadReport
    };
})();
