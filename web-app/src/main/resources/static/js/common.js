document.getElementById('logout').addEventListener('click', function () {
    fetch('/api/v1/auth/logout', {
        method: 'GET',
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            window.location.href = "/login";
        })
        .catch((error) => {
            console.error('Error:', error);
        });
});

document.getElementById('hamburger').addEventListener('click', function () {
    var header = document.querySelector('header');
    var hamburger = document.querySelector('.hamburger');
    header.classList.toggle('open');
    hamburger.classList.toggle('open');
});


/*authorities*/

document.addEventListener('DOMContentLoaded', () => {
    const authorities = localStorage.getItem('authorities');
    showHideElements(authorities);
});

function showHideElements(authorities) {
    let userAuthorities = [];
    try {
        if (authorities) {
            userAuthorities = authorities.startsWith('[')
                ? JSON.parse(authorities)
                : authorities.split(',').map(auth => auth.trim());
        }
    } catch (error) {
        console.error('Failed to parse authorities:', error);
        return;
    }

    const accessControl = {
        'nav-clients': ['client:view'],
        'nav-routes': ['client:view'],
        'nav-purchase': ['purchase:view'],
        'nav-sale': ['sale:view'],
        'nav-containers': ['container:view'],
        'nav-inventory': ['inventory:view'],
        'nav-finance': ['finance:view'],
        'nav-warehouse': ['warehouse:view'],
        'actions-container-transfer': ['container:transfer'],
        'nav-analytics': ['analytics:view'],
        'nav-settings': ['settings:view'],

        'full-delete-client': ['client:full_delete'],
        'export-excel-warehouse': ['warehouse:excel'],
        'excel-export-transaction': ['finance:transfer_excel'],
        'edit-source': ['client_stranger:edit', 'purchase:edit_source', 'sale:edit_source']
    };

    const hasAccess = (requiredAuthorities) => {
        return userAuthorities.includes('system:admin') ||
            requiredAuthorities.some(auth => userAuthorities.includes(auth));
    };

    for (const blockId in accessControl) {
        const element = document.getElementById(blockId);
        if (element) {
            element.style.display = hasAccess(accessControl[blockId]) ? 'flex' : 'none';
        }
    }

    for (const blockId in accessControl) {
        const elements = document.getElementsByClassName(blockId);
        for (const element of elements) {
            element.style.display = hasAccess(accessControl[blockId]) ? 'block' : 'none';
        }
    }
}

const fullName = localStorage.getItem('fullName') || 'Не вказано';

document.getElementById('userName').textContent = fullName;
const userBalanceElement = document.getElementById('userBalance');
if (userBalanceElement) {
    userBalanceElement.style.display = 'none';
}


document.addEventListener('DOMContentLoaded', function () {
    const navLinks = document.querySelectorAll('nav a:not(#logout)');

    navLinks.forEach(link => {
        link.addEventListener('click', function () {

            if (!this.classList.contains('selected-nav')) {
                resetFiltersOnPageChange();
            }
        });
    });

    document.getElementById('logout').addEventListener('click', resetFiltersOnPageChange);
    
    loadClientTypesDropdown();
    loadRouteTypesDropdown();
    loadPurchaseTypesDropdown();
    loadContainerTypesDropdown();
});

async function loadClientTypesDropdown() {
    const dropdown = document.getElementById('client-types-dropdown');
    const navClients = document.getElementById('nav-clients');
    if (!dropdown || !navClients) return;

    try {
        // Загружаем все активные типы клиентов
        const response = await fetch('/api/v1/client-type/active');
        if (!response.ok) return;
        const allClientTypes = await response.json();
        
        // Получаем права доступа текущего пользователя к типам клиентов
        const userId = localStorage.getItem('userId');
        let accessibleClientTypeIds = new Set();
        
        if (userId) {
            try {
                // Используем endpoint /me для получения своих собственных прав без требования administration:view
                const permissionsResponse = await fetch(`/api/v1/client-type/permission/me`);
                if (permissionsResponse.ok) {
                    const permissions = await permissionsResponse.json();
                    // Добавляем типы клиентов, к которым у пользователя есть доступ на просмотр
                    permissions.forEach(perm => {
                        if (perm.canView) {
                            accessibleClientTypeIds.add(perm.clientTypeId);
                        }
                    });
                }
            } catch (error) {
                console.warn('Failed to load user client type permissions:', error);
                // Если не удалось загрузить права доступа, показываем все типы (для обратной совместимости)
                allClientTypes.forEach(type => accessibleClientTypeIds.add(type.id));
            }
        }
        
        // Проверяем, есть ли у пользователя права администратора
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
        
        // Если пользователь администратор или нет прав доступа, показываем все типы
        if (isAdmin || accessibleClientTypeIds.size === 0) {
            allClientTypes.forEach(type => accessibleClientTypeIds.add(type.id));
        }
        
        // Фильтруем типы клиентов по правам доступа
        const accessibleClientTypes = allClientTypes.filter(type => accessibleClientTypeIds.has(type.id));
        
        dropdown.innerHTML = '';
        accessibleClientTypes.forEach(type => {
            const li = document.createElement('li');
            const a = document.createElement('a');
            a.href = `/clients?type=${type.id}`;
            a.textContent = type.name;
            
            // Проверяем, является ли этот тип текущим
            const urlParams = new URLSearchParams(window.location.search);
            const currentTypeId = urlParams.get('type');
            if (currentTypeId && parseInt(currentTypeId) === type.id) {
                a.classList.add('active');
            }
            
            li.appendChild(a);
            dropdown.appendChild(li);
        });

        const navLink = navClients.querySelector('a');
        navLink.addEventListener('click', function(e) {
            e.preventDefault();
            const isVisible = dropdown.style.display === 'block';
            dropdown.style.display = isVisible ? 'none' : 'block';
        });

        document.addEventListener('click', function(e) {
            if (!navClients.contains(e.target)) {
                dropdown.style.display = 'none';
            }
        });
    } catch (error) {
        console.error('Error loading client types:', error);
    }
}

async function loadRouteTypesDropdown() {
    const dropdown = document.getElementById('route-types-dropdown');
    const navRoutes = document.getElementById('nav-routes');
    if (!dropdown || !navRoutes) return;

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
            a.href = `/routes?type=${type.id}`;
            a.textContent = type.name;
            
            const urlParams = new URLSearchParams(window.location.search);
            const currentTypeId = urlParams.get('type');
            if (currentTypeId && parseInt(currentTypeId) === type.id) {
                a.classList.add('active');
            }
            
            li.appendChild(a);
            dropdown.appendChild(li);
        });

        const navLink = navRoutes.querySelector('a');
        navLink.addEventListener('click', function(e) {
            e.preventDefault();
            const isVisible = dropdown.style.display === 'block';
            dropdown.style.display = isVisible ? 'none' : 'block';
        });

        document.addEventListener('click', function(e) {
            if (!navRoutes.contains(e.target)) {
                dropdown.style.display = 'none';
            }
        });
    } catch (error) {
        console.error('Error loading route types:', error);
    }
}

async function loadPurchaseTypesDropdown() {
    const dropdown = document.getElementById('purchase-types-dropdown');
    const navPurchase = document.getElementById('nav-purchase');
    if (!dropdown || !navPurchase) return;

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
            a.href = `/purchase?type=${type.id}`;
            a.textContent = type.name;
            
            const urlParams = new URLSearchParams(window.location.search);
            const currentTypeId = urlParams.get('type');
            if (currentTypeId && parseInt(currentTypeId) === type.id) {
                a.classList.add('active');
            }
            
            li.appendChild(a);
            dropdown.appendChild(li);
        });

        const navLink = navPurchase.querySelector('a');
        navLink.addEventListener('click', function(e) {
            e.preventDefault();
            const isVisible = dropdown.style.display === 'block';
            dropdown.style.display = isVisible ? 'none' : 'block';
        });

        document.addEventListener('click', function(e) {
            if (!navPurchase.contains(e.target)) {
                dropdown.style.display = 'none';
            }
        });
    } catch (error) {
        console.error('Error loading purchase types:', error);
    }
}

async function loadContainerTypesDropdown() {
    const dropdown = document.getElementById('container-types-dropdown');
    const navContainers = document.getElementById('nav-containers');
    if (!dropdown || !navContainers) return;

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
            a.href = `/containers?type=${type.id}`;
            a.textContent = type.name;
            
            const urlParams = new URLSearchParams(window.location.search);
            const currentTypeId = urlParams.get('type');
            if (currentTypeId && parseInt(currentTypeId) === type.id) {
                a.classList.add('active');
            }
            
            li.appendChild(a);
            dropdown.appendChild(li);
        });

        const navLink = navContainers.querySelector('a');
        navLink.addEventListener('click', function(e) {
            e.preventDefault();
            const isVisible = dropdown.style.display === 'block';
            dropdown.style.display = isVisible ? 'none' : 'block';
        });

        document.addEventListener('click', function(e) {
            if (!navContainers.contains(e.target)) {
                dropdown.style.display = 'none';
            }
        });
    } catch (error) {
        console.error('Error loading container types:', error);
    }
}

function resetFiltersOnPageChange() {
    Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);

    const filterForm = document.getElementById('filterForm');
    if (filterForm) {
        filterForm.reset();
        Object.keys(customSelects).forEach(selectId => {
            if (selectId.endsWith('-filter')) {
                customSelects[selectId].reset();
            }
        });
    }

    const searchInput = document.getElementById('inputSearch');
    if (searchInput) {
        searchInput.value = '';
    }

    localStorage.removeItem('selectedFilters');
    localStorage.removeItem('searchTerm');

    if (typeof updateFilterCounter === 'function') {
        updateFilterCounter();
    }
}


function showMessage(message, type = 'info') {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message-box ${type}`;

    messageDiv.style.whiteSpace = 'pre-line';
    messageDiv.textContent = message;
    document.body.appendChild(messageDiv);

    if (type === 'error') {
        const closeButton = document.createElement('span');
        closeButton.className = 'message-close';
        closeButton.innerHTML = '×';
        closeButton.addEventListener('click', () => {
            messageDiv.classList.add('fade-out');
            setTimeout(() => {
                document.body.removeChild(messageDiv);
            }, 200);
        });
        messageDiv.appendChild(closeButton);
    } else {
        setTimeout(() => {
            messageDiv.classList.add('fade-out');
            setTimeout(() => {
                document.body.removeChild(messageDiv);
            }, 500);
        }, 1000);
    }
}