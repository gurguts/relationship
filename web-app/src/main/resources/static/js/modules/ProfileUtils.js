const ProfileUtils = (function() {
    function formatNumber(number, decimals = 2) {
        if (number == null || number === undefined) return '0';
        return parseFloat(number).toFixed(decimals);
    }

    function getUserAuthorities() {
        const authorities = localStorage.getItem('authorities');
        if (!authorities) {
            return [];
        }
        
        let userAuthorities = [];
        try {
            if (authorities.startsWith('[')) {
                userAuthorities = JSON.parse(authorities);
            } else {
                userAuthorities = authorities.split(',').map(auth => auth.trim().replace(/^["']|["']$/g, ''));
            }
        } catch (error) {
            console.error('Failed to parse authorities:', error);
            return [];
        }
        
        return userAuthorities;
    }

    function checkCanEditProfile() {
        const userAuthorities = getUserAuthorities();
        
        const hasAdmin = userAuthorities.some(auth => {
            const trimmed = String(auth).trim();
            return trimmed === 'system:admin';
        });
        const hasProfileEdit = userAuthorities.some(auth => {
            const trimmed = String(auth).trim();
            return trimmed === 'profile:edit';
        });
        
        return hasAdmin || hasProfileEdit;
    }

    function checkCanViewMultipleProfiles() {
        const userAuthorities = getUserAuthorities();
        
        const hasAdmin = userAuthorities.some(auth => {
            const trimmed = String(auth).trim();
            return trimmed === 'system:admin';
        });
        const hasMultipleView = userAuthorities.some(auth => {
            const trimmed = String(auth).trim();
            return trimmed === 'profile:multiple_view';
        });
        
        return hasAdmin || hasMultipleView;
    }

    function escapeHtml(text) {
        if (text == null || text === undefined) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    return {
        formatNumber,
        getUserAuthorities,
        checkCanEditProfile,
        checkCanViewMultipleProfiles,
        escapeHtml
    };
})();
