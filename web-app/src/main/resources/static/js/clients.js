const prevPageButton = document.getElementById('prev-btn');
const nextPageButton = document.getElementById('next-btn');
const paginationInfo = document.getElementById('pagination-info');
const allClientInfo = document.getElementById('all-client-info');
const loaderBackdrop = document.getElementById('loader-backdrop');
let currentSort = 'updatedAt';
let currentDirection = 'DESC';
const filterForm = document.getElementById('filterForm');
const customSelects = {};

let availableStatuses = [];
let availableRegions = [];
let availableSources = [];
let availableRoutes = [];
let availableBusiness = [];

let statusMap;
let regionMap;
let routeMap;
let businessMap;
let sourceMap;
let userMap;
let productMap;

const userId = localStorage.getItem('userId');
const selectedFilters = {};

const API_URL = '/api/v1/client';

let currentPage = 0;
let pageSize = 50;

const tableBody = document.getElementById('client-table-body');

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


function updatePagination(totalClients, clientsOnPage, totalPages, currentPageIndex) {
    allClientInfo.textContent = `
    Клієнтів: ${totalClients}
    `
    paginationInfo.textContent = `
        Клієнтів на сторінці: ${clientsOnPage},
        Всього сторінок: ${totalPages},
        Поточна сторінка: ${currentPageIndex + 1}
    `;

    prevPageButton.disabled = currentPageIndex <= 0;
    nextPageButton.disabled = currentPageIndex >= totalPages - 1;
}

const findNameByIdFromMap = (map, id) => {
    const numericId = Number(id);

    const name = map.get(numericId);

    return name || '';
};

function renderClients(clients) {
    tableBody.innerHTML = '';
    clients.forEach(client => {
        const row = document.createElement('tr');
        row.classList.add('client-row');

        row.innerHTML = `
            <td><input type="checkbox" class="client-checkbox" data-client-id="${client.id}">
            <td data-label="Компанія" data-sort="company" class="company-cell">${client.company}</td>
            <td data-label="Область">${findNameByIdFromMap(regionMap, client.regionId)}</td>
            <td data-label="Статус">${findNameByIdFromMap(statusMap, client.statusId)}</td>
            <td data-label="Телефони">${client.phoneNumbers ? client.phoneNumbers.map(number =>
            `<a href="tel:${number}">${number}</a>`).join('<br>') : ''}</td>
            <td data-label="Залучення">${findNameByIdFromMap(sourceMap, client.sourceId)}</td>
            <td data-label="Маршруты">${findNameByIdFromMap(routeMap, client.routeId)}</td>
            <td data-label="Коментар">${client.comment ? client.comment : ''}</td>
            <td data-label="Адреса" data-sort="location">${client.location ? client.location : ''}</td>

        `;
        tableBody.appendChild(row);
        row.querySelector('.company-cell').addEventListener('click', () => {
            loadClientDetails(client);
        });
    });
}

document.querySelectorAll('th[data-sort]').forEach(th => {
    th.addEventListener('click', () => {
        const sortField = th.getAttribute('data-sort');

        if (currentSort === sortField) {
            currentDirection = currentDirection === 'ASC' ? 'DESC' : 'ASC';
        } else {
            currentSort = sortField;
            currentDirection = 'ASC';
        }

        loadDataWithSort(0, 100, currentSort, currentDirection);
    });
});

async function loadDataWithSort(page, size, sort, direction) {
    loaderBackdrop.style.display = 'flex';
    const searchInput = document.getElementById('inputSearch');
    const searchTerm = searchInput ? searchInput.value : '';
    let queryParams = `page=${page}&size=${size}&sort=${sort}&direction=${direction}`;

    if (searchTerm) {
        queryParams += `&q=${encodeURIComponent(searchTerm)}`;
    }

    if (Object.keys(selectedFilters).length > 0) {
        queryParams += `&filters=${encodeURIComponent(JSON.stringify(selectedFilters))}`;
    }

    /*    const excludeStatuses = [];
        queryParams += `&excludeStatuses=${encodeURIComponent(excludeStatuses.join(','))}`;*/

    try {
        const response = await fetch(`${API_URL}/search?${queryParams}`);

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        const data = await response.json();
        renderClients(data.content);

        updatePagination(data.totalElements, data.content.length, data.totalPages, currentPage);
    } catch (error) {
        console.error('Error creating client:', error);
        handleError(error);
    } finally {
        loaderBackdrop.style.display = 'none';
    }
}


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
    document.getElementById('modal-client-business').innerText =
        findNameByIdFromMap(businessMap, client.businessId);
    document.getElementById('modal-client-route').innerText =
        findNameByIdFromMap(routeMap, client.routeId);
    document.getElementById('modal-client-region').innerText =
        findNameByIdFromMap(regionMap, client.regionId);
    document.getElementById('modal-client-status').innerText =
        findNameByIdFromMap(statusMap, client.statusId);
    document.getElementById('modal-client-source').innerText =
        findNameByIdFromMap(sourceMap, client.sourceId);
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
            const response = await fetch(`${API_URL}/${client.id}`, {method: 'DELETE'});
            if (!response.ok) {
                const errorData = await response.json();
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                return;
            }
            showMessage('Клієнт повністю видалений', 'info');
            modal.style.display = 'none';

            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        } catch (error) {
            console.error('Помилка видалення клієнта:', error);
            handleError(error);
        } finally {
            loaderBackdrop.style.display = 'none';
        }
    };


    const deleteButton = document.getElementById('delete-client');
    deleteButton.onclick = async () => {
        loaderBackdrop.style.display = 'flex';
        try {
            const response = await fetch(`${API_URL}/active/${client.id}`, {method: 'DELETE'});
            if (!response.ok) {
                const errorData = await response.json();
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                return;
            }
            showMessage('Клієнт видалений', 'info');
            modal.style.display = 'none';

            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        } catch (error) {
            console.error('Помилка вимкнення клієнта:', error);
            handleError(error);
        } finally {
            loaderBackdrop.style.display = 'none';
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

/*-------create client-------*/

var modal = document.getElementById("createClientModal");
var btn = document.getElementById("open-modal");
var span = document.getElementsByClassName("create-client-close")[0];
let editing = false;

btn.onclick = function () {
    modal.classList.remove('hide');
    modal.style.display = "flex";
    setTimeout(() => {
        modal.classList.add('show');
    }, 10);
};

span.onclick = function () {
    modal.classList.remove('show');
    modal.classList.add('hide');
    setTimeout(() => {
        modal.style.display = "none";
        resetForm();
    }, 300);
};

window.onclick = function (event) {
    if (event.target === modal) {
        modal.classList.remove('show');
        modal.classList.add('hide');
        setTimeout(() => {
            modal.style.display = "none";
            resetForm();
        }, 300);
    }
};


window.onclick = function (event) {
    if (event.target === modal) {
        modal.style.display = "none";
        resetForm();
    }
}

function resetForm() {
    const form = document.getElementById('client-form');
    form.reset();

    ['region', 'status', 'source', 'route', 'business'].forEach(selectId => {
        const select = document.getElementById(selectId);
        select.selectedIndex = 0;
        const customSelectId = `${selectId}-custom`;
        if (customSelects[customSelectId]) {
            customSelects[customSelectId].reset();
            let defaultValue = defaultValues[selectId];
            if (typeof defaultValue === 'function') {
                defaultValue = defaultValue();
            }
            if (defaultValue && select.querySelector(`option[value="${defaultValue}"]`)) {
                customSelects[customSelectId].setValue(defaultValue);
            }
        }
    });
}

const defaultValues = {
    region: '136',
    status: '24',
    route: '66',
    business: '1',
    source: () => {
        const userId = localStorage.getItem('userId');
        return userSourceMapping[userId] ? String(userSourceMapping[userId]) : '';
    }
};

const userSourceMapping = {
    '1': 8, // admin
    '2': 15, // Музика Катя
    '3': 28, // Водій Дмитро
    '4': 7, // Водій Сергій
    '5': 8, // Шмигельська Олена
    '6': 10, // Денис Казаков
    '7': 26, // Водій Андрій
    '9': 14, // Богдан Осипишин
    '10': 8, // test driver
    '11': 8, // КЗП
    '12': 32, // Водій Саша
    '13': 8, // Сергій Дзвунко
    '14': 30, // Юрій Ємець
    '15': 31 // Артем Фаєр
};


const phonePattern = /^\+380\d{9}$/;
document.getElementById('phoneNumbers').addEventListener('input', updateOutput);

function formatPhoneNumber(num) {
    const cleanedNum = num.replace(/[^\d+]/g, '');
    if (phonePattern.test(cleanedNum)) {
        return cleanedNum;
    } else if (cleanedNum.length === 10 && cleanedNum.startsWith('0')) {
        return "+380" + cleanedNum.substring(1);
    } else if (cleanedNum.length === 12 && cleanedNum.startsWith('380')) {
        return "+380" + cleanedNum.substring(3);
    } else if (cleanedNum.length >= 12 && cleanedNum.startsWith('+380')) {
        return "+380" + cleanedNum.substring(4);
    } else {
        return null;
    }
}

function updateOutput() {
    const input = document.getElementById('phoneNumbers').value;
    const outputDiv = document.getElementById('output');
    outputDiv.innerHTML = '';

    let formattedNumbers = input.split(',')
        .map(num => num.trim())
        .filter(num => num.length > 0)
        .map(formatPhoneNumber)
        .filter(num => num !== null);

    if (formattedNumbers.length > 0) {
        const formattedNumbersList = document.createElement('ul');
        formattedNumbersList.className = 'phone-numbers-list';
        formattedNumbers.forEach(num => {
            const listItem = document.createElement('li');
            listItem.className = 'phone-number-item';
            listItem.textContent = num;
            formattedNumbersList.appendChild(listItem);
        });
        outputDiv.appendChild(formattedNumbersList);
    }
}


document.getElementById('client-form').addEventListener('submit',
    async function (event) {
        event.preventDefault();

        loaderBackdrop.style.display = 'flex';

        const formData = new FormData(this);
        const clientData = {};

        formData.forEach((value, key) => clientData[key] = value);

        clientData.phoneNumbers = clientData.phoneNumbers
            .split(',')
            .map(num => num.trim())
            .filter(num => num.length > 0)
            .map(num => formatPhoneNumber(num))
            .filter(num => num !== null);

        try {
            const response = await fetch('/api/v1/client', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(clientData),
            });

            if (!response.ok) {
                const errorData = await response.json();
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                return;
            }

            const data = await response.json();

            modal.style.display = "none";
            resetForm();
            loadDataWithSort(0, pageSize, currentSort, currentDirection);

            showMessage(`Клієнт з ID: ${data.id} успішно створений`, 'info');
        } catch (error) {
            console.error('Error creating client:', error);
            handleError(error);
        } finally {
            loaderBackdrop.style.display = 'none';
        }
    });


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

    const createdAtFrom = formData.get('createdAtFrom');
    const createdAtTo = formData.get('createdAtTo');
    const updatedAtFrom = formData.get('updatedAtFrom');
    const updatedAtTo = formData.get('updatedAtTo');

    if (createdAtFrom) selectedFilters['createdAtFrom'] = [createdAtFrom];
    if (createdAtTo) selectedFilters['createdAtTo'] = [createdAtTo];
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

function populateSelect(selectId, data) {
    const select = document.getElementById(selectId);
    if (!select) {
        console.error(`Select with id "${selectId}" not found in DOM`);
        return;
    }

    select.innerHTML = '';

    if (!selectId.endsWith('-filter')) {
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.text = select.dataset.placeholder || 'Виберіть параметр';
        defaultOption.disabled = true;
        defaultOption.selected = true;
        select.appendChild(defaultOption);
    }

    data.forEach(item => {
        const option = document.createElement('option');
        option.value = String(item.id);
        option.text = item.name;
        select.appendChild(option);
    });

    const customSelectId = selectId.endsWith('-filter') ? `${selectId}` : `${selectId}-custom`;
    if (!customSelects[customSelectId]) {
        customSelects[customSelectId] = createCustomSelect(select);
    }
    customSelects[customSelectId].populate(data);

    if (!selectId.endsWith('-filter')) {
        let defaultValue = defaultValues[selectId];
        if (typeof defaultValue === 'function') {
            defaultValue = defaultValue();
        }
        if (defaultValue && data.some(item => String(item.id) === defaultValue)) {
            customSelects[customSelectId].setValue(defaultValue);
        }
    }
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

    const companyInput = document.getElementById('company');
    const saveButton = document.getElementById('save-button');

    const validateForm = () => {
        const isCompanyFilled = companyInput.value.trim() !== '';
        saveButton.disabled = !isCompanyFilled;
    };

    companyInput.addEventListener('input', validateForm);
    validateForm();

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

            populateSelect('status', availableStatuses);
            populateSelect('status-filter', availableStatuses);
            populateSelect('region', availableRegions);
            populateSelect('region-filter', availableRegions);
            populateSelect('source', availableSources);
            populateSelect('source-filter', availableSources);
            populateSelect('route', availableRoutes);
            populateSelect('route-filter', availableRoutes);
            populateSelect('business', availableBusiness);
            populateSelect('business-filter', availableBusiness);

            ['region', 'status', 'source', 'route', 'business'].forEach(selectId => {
                const select = document.getElementById(selectId);
                if (select && !customSelects[`${selectId}-custom`]) {
                    customSelects[`${selectId}-custom`] = createCustomSelect(select);
                }
            });

            ['region-filter', 'status-filter', 'source-filter', 'route-filter', 'business-filter']
                .forEach(selectId => {
                    const select = document.getElementById(selectId);
                    if (select && !customSelects[selectId]) {
                        customSelects[selectId] = createCustomSelect(select);
                    }
                });

            ['region-filter', 'status-filter', 'source-filter', 'route-filter', 'business-filter']
                .forEach(selectId => {
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
                if (window.selectedFilters['createdAtFrom']) {
                    filterForm.querySelector('#createdAtFrom').value = window.selectedFilters['createdAtFrom'];
                }
                if (window.selectedFilters['createdAtTo']) {
                    filterForm.querySelector('#createdAtTo').value = window.selectedFilters['createdAtTo'];
                }
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



