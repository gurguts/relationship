async function loadClientTypeDropdown(dropdownId, navElementId, basePath) {
    const dropdown = document.getElementById(dropdownId);
    const navElement = document.getElementById(navElementId);
    if (!dropdown || !navElement) return;

    try {
        const response = await fetch('/api/v1/client-type/active');
        if (!response.ok) return;
        const allClientTypes = await response.json();
        
        const userId = localStorage.getItem('userId');
        let accessibleClientTypeIds = new Set();
        
        if (userId) {
            try {
                const permissionsResponse = await fetch(`/api/v1/client-type/permission/me`);
                if (permissionsResponse.ok) {
                    const permissions = await permissionsResponse.json();
                    permissions.forEach(perm => {
                        if (perm.canView) {
                            accessibleClientTypeIds.add(perm.clientTypeId);
                        }
                    });
                }
            } catch (error) {
                console.warn('Failed to load user client type permissions:', error);
                allClientTypes.forEach(type => accessibleClientTypeIds.add(type.id));
            }
        }
        
        const authorities = localStorage.getItem('authorities');
        let userAuthorities = [];
        try {
            if (authorities) {
                userAuthorities = authorities.startsWith('[')
                    ? JSON.parse(authorities)
                    : authorities.split(',').map(auth => auth.trim());
            }
        } catch (error) {
            console.error('Failed to parse authorities:', error);
        }
        
        const isAdmin = userAuthorities.includes('system:admin') || userAuthorities.includes('administration:view');

        if (isAdmin || accessibleClientTypeIds.size === 0) {
            allClientTypes.forEach(type => accessibleClientTypeIds.add(type.id));
        }

        const accessibleClientTypes = allClientTypes.filter(type => accessibleClientTypeIds.has(type.id));
        
        dropdown.innerHTML = '';
        accessibleClientTypes.forEach(type => {
            const li = document.createElement('li');
            const a = document.createElement('a');
            a.href = `${basePath}?type=${type.id}`;
            a.textContent = type.name;
            
            const urlParams = new URLSearchParams(window.location.search);
            const currentTypeId = urlParams.get('type');
            if (currentTypeId && parseInt(currentTypeId) === type.id) {
                a.classList.add('active');
            }
            
            li.appendChild(a);
            dropdown.appendChild(li);
        });

        const navLink = navElement.querySelector('a');
        navLink.addEventListener('click', function(e) {
            e.preventDefault();
            const isVisible = dropdown.style.display === 'block';
            dropdown.style.display = isVisible ? 'none' : 'block';
        });

        document.addEventListener('click', function(e) {
            if (!navElement.contains(e.target)) {
                dropdown.style.display = 'none';
            }
        });
    } catch (error) {
        console.error(`Error loading ${dropdownId}:`, error);
    }
}
