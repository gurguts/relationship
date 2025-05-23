const API_URL_CLIENT = '/api/v1/client';
const tbodyContainer = document.getElementById('barrel-table-body');

const prevPageButton = document.getElementById('prev-btn');
const nextPageButton = document.getElementById('next-btn');
const paginationInfo = document.getElementById('pagination-info');
const allOilInfo = document.getElementById('all-barrel-info');
const loaderBackdrop = document.getElementById('loader-backdrop');
const filterForm = document.getElementById('filterForm');
let currentSort = 'updatedAt';
let currentDirection = 'DESC';
let currentPage = 0;
let pageSize = 100;
let editing = false;
const selectedFilters = {};
const customSelects = {};

let availableStatuses = [];
let availableRegions = [];
let availableSources = [];
let availableRoutes = [];
let availableBusiness = [];
let availableUsers = [];
let availableContainers = [];

let statusMap;
let regionMap;
let routeMap;
let businessMap;
let sourceMap;
let userMap;
let containerMap;

const findNameByIdFromMap = (map, id) => {
    const numericId = Number(id); // Convert to number

    const name = map.get(numericId);

    return name || '';
};


/*--showClientModal--*/

function loadClientDetails(client) {
    showClientModal(client);
}

function showClientModal(client) {
    document.getElementById('client-modal').setAttribute('data-client-id', client.id);

    document.getElementById('modal-client-id').innerText = client.id;
    document.getElementById('modal-client-company').innerText = client.company;
    document.getElementById('modal-client-person').innerText = client.person || '';
    document.getElementById('modal-client-phone').innerText = client.phoneNumbers || '';
    document.getElementById('modal-client-location').innerText = client.location || '';
    document.getElementById('modal-client-price-purchase').innerText = client.pricePurchase || '';
    document.getElementById('modal-client-price-sale').innerText = client.priceSale || '';
    if (client.vat === true) {
        document.getElementById('modal-client-vat').innerHTML =
            `<input type="checkbox" id="edit-vat" checked disabled />`;
    } else if (client.vat === false || client.vat === null || client.vat === undefined) {
        document.getElementById('modal-client-vat').innerHTML =
            `<input type="checkbox" id="edit-vat" disabled />`;
    } else {
        document.getElementById('modal-client-vat').innerHTML = '';
    }
    document.getElementById('modal-client-volumeMonth').innerText = client.volumeMonth || '';
    document.getElementById('modal-client-edrpou').innerText = client.edrpou || '';
    document.getElementById('modal-client-enterpriseName').innerText = client.enterpriseName || '';
    document.getElementById('modal-client-business').innerText = client.business?.id ?
        findNameByIdFromMap(businessMap, client.business.id) : '';
    document.getElementById('modal-client-route').innerText = client.route?.id ?
        findNameByIdFromMap(routeMap, client.route.id) : '';
    document.getElementById('modal-client-region').innerText = client.region?.id ?
        findNameByIdFromMap(regionMap, client.region.id) : '';
    document.getElementById('modal-client-status').innerText = client.status?.id ?
        findNameByIdFromMap(statusMap, client.status.id) : '';
    document.getElementById('modal-client-source').innerText = client.source?.id ?
        findNameByIdFromMap(sourceMap, client.source.id) : '';
    document.getElementById('modal-client-comment').innerText = client.comment || '';
    document.getElementById('modal-client-created').innerText = client.createdAt || '';
    document.getElementById('modal-client-updated').innerText = client.updatedAt || '';

    const urgentlyCheckbox = document.getElementById('modal-client-urgently');
    urgentlyCheckbox.checked = client.urgently;
    urgentlyCheckbox.addEventListener('change', function () {
        toggleUrgently(client.id);
    });

    const modal = document.getElementById('client-modal');
    modal.style.display = 'flex';
    setTimeout(() => {
        modal.classList.add('open');
    }, 10);

    document.getElementById('close-modal-client').addEventListener('click', () => {
        modal.classList.remove('open');
        setTimeout(() => {
            closeModal();
        });
    });

    window.onclick = function (event) {
        if (event.target === modal) {
            closeModal();
        }
    }

    const fullDeleteButton = document.getElementById('full-delete-client');
    fullDeleteButton.onclick = async () => {
        loaderBackdrop.style.display = 'flex';
        try {
            const response = await fetch(`${API_URL_CLIENT}/${client.id}`, {method: 'DELETE'});
            if (!response.ok) {
                const errorData = await response.json();
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                return;
            }
            modal.style.display = 'none';
            loaderBackdrop.style.display = 'none';

            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
            showMessage('Клієнт успішно видалено', 'info');
        } catch (error) {
            loaderBackdrop.style.display = 'none';
            console.error('Помилка видалення клієнта:', error);
            handleError(error);
        }
    };

    const deleteButton = document.getElementById('delete-client');
    deleteButton.onclick = async () => {
        loaderBackdrop.style.display = 'flex';
        try {
            const response = await fetch(`${API_URL_CLIENT}/active/${client.id}`, {method: 'DELETE'});
            if (!response.ok) {
                const errorData = await response.json();
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                return;
            }
            showMessage('Клієнта видалено', 'info');
            modal.style.display = 'none';

            loaderBackdrop.style.display = 'none';
            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        } catch (error) {
            loaderBackdrop.style.display = 'none';
            console.error('Помилка вимкненого клієнта:', error);
            handleError(error);
        }
    };
}

async function toggleUrgently(clientId) {
    loaderBackdrop.style.display = 'flex';

    try {
        const response = await fetch(`${API_URL}/urgently/${clientId}`, {method: 'PATCH'});

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        showMessage('Статус терміновості успішно оновлено', 'info');

        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
    } catch (error) {
        console.error('Ошибка обновления статуса терміновості:', error);
        handleError(error);
    } finally {
        loaderBackdrop.style.display = 'none';
    }
}

/*--loadContainersWithSort--*/

async function loadDataWithSort(page, size, sort, direction) {
    loaderBackdrop.style.display = 'flex';
    const searchInput = document.getElementById('inputSearch');
    const searchTerm = searchInput ? searchInput.value : '';
    let queryParams = `page=${page}&size=${size}&sort=${sort}&direction=${direction}`;

    if (searchTerm) {
        queryParams += `&query=${encodeURIComponent(searchTerm)}`;
    }

    if (Object.keys(selectedFilters).length > 0) {
        queryParams += `&filters=${encodeURIComponent(JSON.stringify(selectedFilters))}`;
    }

    try {
        const response = await fetch(`/api/v1/containers/client/search-containers?${queryParams}`);

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        const data = await response.json();
        renderContainers(data.content);

        updatePagination(data.totalElements, data.content.length, data.totalPages, currentPage);
    } catch (error) {
        console.error('Error:', error);
        handleError(error);
    } finally {
        loaderBackdrop.style.display = 'none';
    }
}

function renderContainers(containers) {
    tbodyContainer.innerHTML = '';
    containers.forEach(container => {
        const row = document.createElement('tr');
        row.classList.add('barrel-row');
        row.innerHTML = `
            <td data-label="Клієнт" class="company-cell">${container.client.company}</td>
            <td data-label="Тип тари">${container.containerName ? container.containerName : ''}</td>
            <td data-label="Кількість">${container.quantity ? container.quantity : ''}</td>
            <td data-label="Власник">${findNameByIdFromMap(userMap, container.userId)}</td>
            <td data-label="Оновлено">${container.updatedAt ? container.updatedAt : ''}</td>
        `;
        tbodyContainer.appendChild(row);

        row.querySelector('.company-cell').addEventListener('click', () => {
            loadClientDetails(container.client);
        });
    });
}

/*--pagination--*/

prevPageButton.addEventListener('click', () => {
    if (currentPage > 0) {
        currentPage--;
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
    }
});

nextPageButton.addEventListener('click', () => {
    currentPage++;
    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
});

function updatePagination(totalPoint, pointOnPage, totalPages, currentPageIndex) {
    allOilInfo.textContent = `
    Точок: ${totalPoint}
    `
    paginationInfo.textContent = `
        Точок на сторінці: ${pointOnPage},
        Всього сторінок: ${totalPages},
        Поточна сторінка: ${currentPageIndex + 1}
    `;

    prevPageButton.disabled = currentPageIndex <= 0;
    nextPageButton.disabled = currentPageIndex >= totalPages - 1;
}

/*--search--*/

const searchInput = document.getElementById('inputSearch');
const searchButton = document.getElementById('searchButton');

searchInput.addEventListener('keypress', async (event) => {
    if (event.key === 'Enter') {
        searchButton.click();
    }
});

searchButton.addEventListener('click', async () => {
    const searchTerm = searchInput.value;
    localStorage.setItem('searchTerm', searchTerm);
    loadDataWithSort(0, 100, currentSort, currentDirection);
});

/*--filter--*/

const filterButton = document.querySelector('.filter-button-block');
const filterModal = document.getElementById('filterModal');
const closeFilter = document.querySelector('.close-filter');
const modalContent = filterModal.querySelector('.modal-content-filter');

filterButton.addEventListener('click', () => {
    filterModal.style.display = 'block';
    setTimeout(() => {
        filterModal.classList.add('show');
    }, 10);
});

closeFilter.addEventListener('click', () => {
    closeModalFilter();
});

filterModal.addEventListener('click', (event) => {
    if (!modalContent.contains(event.target)) {
        closeModalFilter();
    }
});

function closeModalFilter() {
    filterModal.classList.add('closing');
    modalContent.classList.add('closing-content');

    setTimeout(() => {
        filterModal.style.display = 'none';
        filterModal.classList.remove('closing');
        modalContent.classList.remove('closing-content');
    }, 200);
}


document.getElementById("modal-filter-button-submit").addEventListener('click',
    (event) => {
        event.preventDefault();
        updateSelectedFilters();
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);

        closeModalFilter();
    });


function updateSelectedFilters() {
    if (typeof selectedFilters === 'undefined') {
        window.selectedFilters = {};
    }

    Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);

    // Обработка select-фильтров
    Object.keys(customSelects).forEach(selectId => {
        if (selectId.endsWith('-filter')) {
            const select = document.getElementById(selectId);
            if (select) {
                const name = select.name;
                const values = customSelects[selectId].getValue();
                if (values.length > 0) {
                    selectedFilters[name] = values;
                }
            }
        }
    });

    const filterForm = document.getElementById('filterForm');
    const formData = new FormData(filterForm);

    const updatedAtFrom = formData.get('updatedAtFrom');
    const updatedAtTo = formData.get('updatedAtTo');
    if (updatedAtFrom) selectedFilters['updatedAtFrom'] = [updatedAtFrom];
    if (updatedAtTo) selectedFilters['updatedAtTo'] = [updatedAtTo];

    localStorage.setItem('selectedFilters', JSON.stringify(selectedFilters));
    updateFilterCounter();
}

function updateFilterCounter() {
    const counterElement = document.getElementById('filter-counter');
    const countElement = document.getElementById('filter-count');

    if (!counterElement || !countElement) return;

    let totalFilters = 0;

    totalFilters += Object.values(selectedFilters)
        .filter(value => Array.isArray(value))
        .reduce((count, values) => count + values.length, 0);

    totalFilters += Object.keys(selectedFilters)
        .filter(key => !Array.isArray(selectedFilters[key]) && selectedFilters[key] !== '')
        .length;

    if (totalFilters > 0) {
        countElement.textContent = totalFilters;
        counterElement.style.display = 'inline-flex';
    } else {
        counterElement.style.display = 'none';
    }
}


document.getElementById('filter-counter').addEventListener('click', () => {
    clearFilters();
});

function clearFilters() {
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
    searchInput.value = '';

    localStorage.removeItem('selectedFilters');
    localStorage.removeItem('searchTerm');

    updateFilterCounter();
    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
}


document.addEventListener('DOMContentLoaded', () => {

    const savedFilters = localStorage.getItem('selectedFilters');
    let parsedFilters;
    if (savedFilters) {
        try {
            parsedFilters = JSON.parse(savedFilters);
        } catch (e) {
            console.error('Invalid selectedFilters in localStorage:', e);
            parsedFilters = {};
        }
    } else {
        parsedFilters = {};
    }
    window.selectedFilters = parsedFilters;

    const savedSearchTerm = localStorage.getItem('searchTerm');
    if (savedSearchTerm) {
        const searchInput = document.getElementById('inputSearch');
        searchInput.value = savedSearchTerm;
    }

    fetch('/api/v1/container')
        .then(response => response.json())
        .then(data => {
            availableContainers = data || [];
        })
        .catch(error => {
            console.error(error);
            handleError(error);
        });

    fetch('/api/v1/entities')
        .then(response => response.json())
        .then(data => {
            availableStatuses = data.statuses || [];
            availableRegions = data.regions || [];
            availableSources = data.sources || [];
            availableRoutes = data.routes || [];
            availableBusiness = data.businesses || [];
            availableUsers = data.users || [];
            availableProducts = data.products || [];

            statusMap = new Map(availableStatuses.map(item => [item.id, item.name]));
            regionMap = new Map(availableRegions.map(item => [item.id, item.name]));
            sourceMap = new Map(availableSources.map(item => [item.id, item.name]));
            routeMap = new Map(availableRoutes.map(item => [item.id, item.name]));
            businessMap = new Map(availableBusiness.map(item => [item.id, item.name]));
            userMap = new Map(availableUsers.map(item => [item.id, item.name]));
            productMap = new Map(availableProducts.map(item => [item.id, item.name]));
            containerMap = new Map(availableContainers.map(item => [item.id, item.name]));

            populateSelect('status-filter', availableStatuses || []);
            populateSelect('region-filter', availableRegions || []);
            populateSelect('source-filter', availableSources || []);
            populateSelect('route-filter', availableRoutes || []);
            populateSelect('business-filter', availableBusiness || []);
            populateSelect('container-filter', availableContainers || []);
            populateSelect('user-filter', availableUsers || []);

            ['region', 'status', 'source', 'route', 'business', 'user', 'container'].forEach(selectId => {
                const select = document.getElementById(selectId);
                if (select && !customSelects[`${selectId}-custom`]) {
                    customSelects[`${selectId}-custom`] = createCustomSelect(select);
                }
            });

            ['region-filter', 'status-filter', 'source-filter', 'route-filter', 'business-filter',
                'user-filter', 'container-filter'].forEach(selectId => {
                const select = document.getElementById(selectId);
                if (select && !customSelects[selectId]) {
                    customSelects[selectId] = createCustomSelect(select);
                }
            });

            ['region-filter', 'status-filter', 'source-filter', 'route-filter', 'business-filter',
                'user-filter', 'container-filter'].forEach(selectId => {
                const select = document.getElementById(selectId);
                if (select && customSelects[selectId]) {
                    const name = select.name;
                    if (window.selectedFilters[name] && Array.isArray(window.selectedFilters[name])) {
                        customSelects[selectId].setValue(window.selectedFilters[name]);
                    }
                }
            });

            const filterForm = document.getElementById('filterForm');
            if (filterForm) {
                if (window.selectedFilters['updatedAtFrom']) {
                    filterForm.querySelector('#updatedAtFrom').value = window.selectedFilters['updatedAtFrom'];
                }
                if (window.selectedFilters['updatedAtTo']) {
                    filterForm.querySelector('#updatedAtTo').value = window.selectedFilters['updatedAtTo'];
                }
            }

            updateSelectedFilters();

            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        })
        .catch(error => {
            console.error(error);
            handleError(error);
        });
});
