const InventoryDataLoader = (function() {
    const API_URL_BALANCE = '/api/v1/containers/balance/users';
    const API_URL_CONTAINER = '/api/v1/container';
    
    async function loadUserContainerBalances() {
        try {
            const response = await fetch(API_URL_BALANCE, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                },
            });

            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }

            return await response.json();
        } catch (error) {
            console.error('Error loading user container balances:', error);
            throw error;
        }
    }
    
    async function loadContainerTypes() {
        try {
            const response = await fetch(API_URL_CONTAINER, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                },
            });

            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }

            return await response.json();
        } catch (error) {
            console.error('Error loading container types:', error);
            throw error;
        }
    }
    
    async function executeBalanceOperation(action, userId, containerId, quantity) {
        try {
            const endpoint = `/api/v1/containers/balance/${action}`;
            const requestBody = {
                userId: parseInt(userId),
                containerId: parseInt(containerId),
                quantity: quantity,
            };

            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestBody),
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
            console.error('Error executing balance operation:', error);
            throw error;
        }
    }
    
    return {
        loadUserContainerBalances,
        loadContainerTypes,
        executeBalanceOperation
    };
})();
