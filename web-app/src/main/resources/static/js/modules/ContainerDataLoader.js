const ContainerDataLoader = (function() {
    const API_URL_CONTAINER = '/api/v1/containers/client';
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
    
    async function loadContainers() {
        try {
            const response = await fetch('/api/v1/container');
            if (!response.ok) return [];
            return await response.json();
        } catch (error) {
            console.error('Error loading containers:', error);
            return [];
        }
    }
    
    async function loadContainerData(params) {
        try {
            let queryParams = [];
            Object.keys(params).forEach(key => {
                const value = params[key];
                if (value !== null && value !== undefined && value !== '') {
                    queryParams.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`);
                }
            });
            
            const queryString = queryParams.join('&');
            const response = await fetch(`${API_URL_CONTAINER}/search-containers?${queryString}`);
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new ErrorResponse(errorData.error, errorData.message, errorData.details);
            }
            
            return await response.json();
        } catch (error) {
            console.error('Error loading container data:', error);
            throw error;
        }
    }
    
    return {
        loadClientType,
        loadClientTypeFields,
        loadEntities,
        loadContainers,
        loadContainerData
    };
})();
