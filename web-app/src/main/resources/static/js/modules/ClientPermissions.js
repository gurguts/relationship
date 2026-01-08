const ClientPermissions = (function() {
    function getUserAuthorities() {
        return ClientState.getAuthorities();
    }
    
    function canEditStrangers() {
        const userAuthorities = getUserAuthorities();
        return userAuthorities.includes('client_stranger:edit') || 
               userAuthorities.includes('system:admin');
    }
    
    function isOwnClient(client, availableSources) {
        if (!client.sourceId) {
            return true;
        }
        
        if (!availableSources || availableSources.length === 0) {
            return true;
        }
        
        const sourceId = Number(client.sourceId);
        const source = availableSources.find(s => Number(s.id) === sourceId);
        if (!source) {
            return false;
        }
        
        const currentUserId = ClientState.getUserId() ? Number(ClientState.getUserId()) : null;
        const sourceUserId = (source.userId !== null && source.userId !== undefined) ? Number(source.userId) : null;
        
        return currentUserId != null && sourceUserId != null && Number(sourceUserId) === Number(currentUserId);
    }
    
    function canEditClient(client, availableSources) {
        if (canEditStrangers()) {
            return true;
        }
        return isOwnClient(client, availableSources);
    }
    
    function canEditCompany(client, availableSources) {
        if (canEditStrangers()) {
            return true;
        }
        return isOwnClient(client, availableSources);
    }
    
    function canEditSource() {
        return canEditStrangers();
    }
    
    function canDeleteClient(client, availableSources) {
        if (canEditStrangers()) {
            return true;
        }
        return isOwnClient(client, availableSources);
    }
    
    return {
        canEditClient,
        canEditCompany,
        canEditSource,
        canDeleteClient,
        canEditStrangers,
        isOwnClient
    };
})();
