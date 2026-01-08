const ClientState = (function() {
    let cachedUserId = null;
    let cachedAuthorities = null;
    
    function getUserId() {
        if (cachedUserId === null) {
            cachedUserId = localStorage.getItem('userId');
        }
        return cachedUserId;
    }
    
    function getAuthorities() {
        if (cachedAuthorities === null) {
            const authorities = localStorage.getItem('authorities');
            try {
                if (authorities) {
                    cachedAuthorities = authorities.startsWith('[')
                        ? JSON.parse(authorities)
                        : authorities.split(',').map(auth => auth.trim());
                } else {
                    cachedAuthorities = [];
                }
            } catch (error) {
                console.error('Failed to parse authorities:', error);
                cachedAuthorities = [];
            }
        }
        return cachedAuthorities;
    }
    
    function clearCache() {
        cachedUserId = null;
        cachedAuthorities = null;
    }
    
    window.addEventListener('storage', (e) => {
        if (e.key === 'userId' || e.key === 'authorities') {
            clearCache();
        }
    });
    
    return {
        getUserId,
        getAuthorities,
        clearCache
    };
})();
