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
        'nav-balance': ['finance:view'],
        'nav-warehouse': ['warehouse:view'],
        'actions-container-transfer': ['container:transfer'],
        'nav-analytics': ['analytics:view'],
        'nav-settings': ['settings:view'],

        'full-delete-client': ['client:full_delete'],
        'export-excel-warehouse': ['warehouse:excel'],
        'excel-export-transaction': ['finance:transfer_excel'],
        'edit-source': ['client:edit_source', 'purchase:edit_source', 'sale:edit_source']
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
const balance = localStorage.getItem('balance') || '0';

document.getElementById('userName').textContent = fullName + ': ';
document.getElementById('userBalance').textContent = balance;


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
});

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