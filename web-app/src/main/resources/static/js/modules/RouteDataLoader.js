const RouteDataLoader = (function() {
    const API_URL = '/api/v1/client';
    
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
    
    async function loadClientFieldValues(clientId) {
        try {
            const response = await fetch(`/api/v1/client/${clientId}/field-values`);
            if (!response.ok) return [];
            return await response.json();
        } catch (error) {
            console.error('Error loading field values:', error);
            return [];
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
    
    async function loadDefaultClientData() {
        try {
            const sourcesRes = await fetch('/api/v1/source');
            if (!sourcesRes.ok) {
                console.error('Failed to load sources:', sourcesRes.status, sourcesRes.statusText);
                return null;
            }
            return await sourcesRes.json();
        } catch (error) {
            console.error('Error loading default data:', error);
            return null;
        }
    }
    
    async function searchClients(params) {
        try {
            let queryParams = [];
            Object.keys(params).forEach(key => {
                const value = params[key];
                if (value !== null && value !== undefined && value !== '') {
                    queryParams.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`);
                }
            });
            const queryString = queryParams.join('&');
            const response = await fetch(`${API_URL}/search?${queryString}`);
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new ErrorResponse(errorData.error, errorData.message, errorData.details);
            }
            
            return await response.json();
        } catch (error) {
            console.error('Error loading clients:', error);
            throw error;
        }
    }
    
    async function updateClient(clientId, clientData) {
        try {
            const response = await fetch(`${API_URL}/${clientId}`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(clientData),
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new ErrorResponse(errorData.error, errorData.message, errorData.details);
            }
            
            return await response.json();
        } catch (error) {
            console.error('Error updating client:', error);
            throw error;
        }
    }
    
    async function createClient(clientData) {
        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(clientData),
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new ErrorResponse(errorData.error, errorData.message, errorData.details);
            }
            
            return await response.json();
        } catch (error) {
            console.error('Error creating client:', error);
            throw error;
        }
    }
    
    async function deleteClient(clientId, fullDelete = false) {
        try {
            const url = fullDelete ? `${API_URL}/${clientId}` : `${API_URL}/active/${clientId}`;
            const response = await fetch(url, {
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
    
    async function restoreClient(clientId) {
        try {
            const response = await fetch(`${API_URL}/active/${clientId}`, {
                method: 'PATCH'
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new ErrorResponse(errorData.error, errorData.message, errorData.details);
            }
            
            return true;
        } catch (error) {
            console.error('Error restoring client:', error);
            throw error;
        }
    }
    
    return {
        loadClientType,
        loadClientTypeFields,
        loadClientFieldValues,
        loadEntities,
        loadDefaultClientData,
        searchClients,
        updateClient,
        createClient,
        deleteClient,
        restoreClient
    };
})();
