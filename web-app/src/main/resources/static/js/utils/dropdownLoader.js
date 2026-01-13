let clientTypesCache = null;
let clientTypesCachePromise = null;

let clientTypePermissionsCache = null;
let clientTypePermissionsCachePromise = null;

async function loadClientTypesForDropdown() {
    if (clientTypesCache) {
        return clientTypesCache;
    }
    
    if (clientTypesCachePromise) {
        return await clientTypesCachePromise;
    }
    
    clientTypesCachePromise = (async () => {
        try {
            const response = await fetch('/api/v1/client-type/active');
            if (!response.ok) {
                if (response.status === 403 || response.status === 401) {
                    return null;
                }
                return null;
            }
            const allClientTypes = await response.json();
            clientTypesCache = allClientTypes;
            return allClientTypes;
        } catch (error) {
            console.error('Failed to load client types:', error);
            return null;
        } finally {
            clientTypesCachePromise = null;
        }
    })();
    
    return await clientTypesCachePromise;
}

async function loadClientTypePermissionsForDropdown() {
    if (clientTypePermissionsCache !== null) {
        return clientTypePermissionsCache;
    }
    
    if (clientTypePermissionsCachePromise) {
        return await clientTypePermissionsCachePromise;
    }
    
    const userId = localStorage.getItem('userId');
    if (!userId) {
        return null;
    }
    
    clientTypePermissionsCachePromise = (async () => {
        try {
            const permissionsResponse = await fetch(`/api/v1/client-type/permission/me`);
            if (permissionsResponse.ok) {
                const permissions = await permissionsResponse.json();
                clientTypePermissionsCache = permissions;
                return permissions;
            }
            return null;
        } catch (error) {
            console.warn('Failed to load user client type permissions:', error);
            return null;
        } finally {
            clientTypePermissionsCachePromise = null;
        }
    })();
    
    return await clientTypePermissionsCachePromise;
}

async function loadClientTypeDropdown(dropdownId, navElementId, basePath) {
    const dropdown = document.getElementById(dropdownId);
    const navElement = document.getElementById(navElementId);
    if (!dropdown || !navElement) {
        return;
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
    const hasClientAccess = isAdmin || userAuthorities.includes('client:view');
    const hasRouteAccess = isAdmin || userAuthorities.includes('client:view');
    const hasPurchaseAccess = isAdmin || userAuthorities.includes('purchase:view');
    const hasContainerAccess = isAdmin || userAuthorities.includes('container:view');

    let hasAccess = false;
    if (dropdownId === 'client-types-dropdown' && navElementId === 'nav-clients') {
        hasAccess = hasClientAccess;
    } else if (dropdownId === 'route-types-dropdown' && navElementId === 'nav-routes') {
        hasAccess = hasRouteAccess;
    } else if (dropdownId === 'purchase-types-dropdown' && navElementId === 'nav-purchase') {
        hasAccess = hasPurchaseAccess;
    } else if (dropdownId === 'container-types-dropdown' && navElementId === 'nav-containers') {
        hasAccess = hasContainerAccess;
    }

    if (!hasAccess) {
        const navLi = navElement.closest('li');
        if (navLi) {
            navLi.style.display = 'none';
        }
        return;
    }

    try {
        const allClientTypes = await loadClientTypesForDropdown();
        
        if (!allClientTypes || (Array.isArray(allClientTypes) && allClientTypes.length === 0)) {
            if (!isAdmin) {
                const navLi = navElement.closest('li');
                if (navLi) {
                    navLi.style.display = 'none';
                }
            } else {
                dropdown.textContent = '';
                const li = document.createElement('li');
                const span = document.createElement('span');
                span.style.padding = '0.7em 0.5em';
                span.style.color = 'rgba(255, 255, 255, 0.6)';
                span.style.display = 'block';
                span.textContent = 'Немає типів клієнтів';
                li.appendChild(span);
                dropdown.appendChild(li);
                
                const navLink = navElement.querySelector('a');
                if (navLink) {
                    const existingHandler = navLink._dropdownClickHandler;
                    if (existingHandler) {
                        navLink.removeEventListener('click', existingHandler, true);
                    }
                    
                    const clickHandler = function(e) {
                        e.preventDefault();
                        e.stopPropagation();
                        e.stopImmediatePropagation();
                        const currentDisplay = dropdown.style.display || window.getComputedStyle(dropdown).display;
                        const computedDisplay = window.getComputedStyle(dropdown).display;
                        const isVisible = currentDisplay === 'block' || computedDisplay === 'block';
                        if (isVisible) {
                            dropdown.style.display = 'none';
                        } else {
                            dropdown.style.display = 'block';
                        }
                    };
                    
                    navLink._dropdownClickHandler = clickHandler;
                    navLink.addEventListener('click', clickHandler, true);
                }
            }
            return;
        }
        
        const userId = localStorage.getItem('userId');
        let accessibleClientTypeIds = new Set();
        
        if (userId) {
            const permissions = await loadClientTypePermissionsForDropdown();
            if (permissions) {
                permissions.forEach(perm => {
                    if (perm.canView) {
                        accessibleClientTypeIds.add(perm.clientTypeId);
                    }
                });
            } else {
                allClientTypes.forEach(type => accessibleClientTypeIds.add(type.id));
            }
        }

        if (isAdmin || accessibleClientTypeIds.size === 0) {
            allClientTypes.forEach(type => accessibleClientTypeIds.add(type.id));
        }

        const accessibleClientTypes = allClientTypes.filter(type => accessibleClientTypeIds.has(type.id));
        
        dropdown.textContent = '';
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
        if (!navLink) {
            return;
        }
        
        const existingHandler = navLink._dropdownClickHandler;
        if (existingHandler) {
            navLink.removeEventListener('click', existingHandler, true);
        }
        
        const clickHandler = function(e) {
            e.preventDefault();
            e.stopPropagation();
            e.stopImmediatePropagation();
            const currentDisplay = dropdown.style.display || window.getComputedStyle(dropdown).display;
            const computedDisplay = window.getComputedStyle(dropdown).display;
            const isVisible = currentDisplay === 'block' || computedDisplay === 'block';
            if (isVisible) {
                dropdown.style.display = 'none';
            } else {
                dropdown.style.display = 'block';
            }
            return false;
        };
        
        navLink._dropdownClickHandler = clickHandler;
        navLink.addEventListener('click', clickHandler, true);
        
        navLink.setAttribute('data-dropdown-initialized', 'true');

        const existingOutsideHandler = document._dropdownOutsideHandlers;
        if (!existingOutsideHandler) {
            document._dropdownOutsideHandlers = new Map();
        }
        
        const outsideClickHandler = function(e) {
            if (!navElement.contains(e.target)) {
                dropdown.style.display = 'none';
            }
        };
        
        document._dropdownOutsideHandlers.set(dropdownId, outsideClickHandler);
        document.addEventListener('click', outsideClickHandler);
    } catch (error) {
        const navLi = navElement.closest('li');
        if (navLi) {
            navLi.style.display = 'none';
        }
    }
}
