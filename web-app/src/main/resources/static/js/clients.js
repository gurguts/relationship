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
let availableClientProducts = [];

let statusMap;
let regionMap;
let routeMap;
let businessMap;
let clientProductMap;
let sourceMap;
let userMap;
let productMap;

const userId = localStorage.getItem('userId');
const selectedFilters = {};

const API_URL = '/api/v1/client';

let currentPage = 0;
let pageSize = 50;

const tableBody = document.getElementById('client-table-body');

let currentClientTypeId = null;
let currentClientType = null;
let clientTypeFields = [];
let visibleFields = [];
let searchableFields = [];
let filterableFields = [];
let visibleInCreateFields = [];

document.addEventListener('DOMContentLoaded', async () => {
    const savedFilters = localStorage.getItem('selectedFilters');
    let parsedFilters;
    if (savedFilters) {
        try {
            parsedFilters = JSON.parse(savedFilters);
            const normalizedFilters = {};
            Object.keys(parsedFilters).forEach(key => {
                const normalizedKey = key.toLowerCase();
                if (normalizedKey === 'status' || normalizedKey === 'region' || 
                    normalizedKey === 'route' || normalizedKey === 'business' || 
                    normalizedKey === 'source' || normalizedKey === 'clientproduct' ||
                    normalizedKey.endsWith('from') || normalizedKey.endsWith('to') ||
                    normalizedKey === 'createdatfrom' || normalizedKey === 'createdatto' ||
                    normalizedKey === 'updatedatfrom' || normalizedKey === 'updatedatto') {
                    normalizedFilters[normalizedKey] = parsedFilters[key];
                }
            });
            parsedFilters = normalizedFilters;
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
        if (searchInput) {
            searchInput.value = savedSearchTerm;
        }
    }

    const urlParams = new URLSearchParams(window.location.search);
    const typeId = urlParams.get('type');
    
    const savedClientTypeId = localStorage.getItem('currentClientTypeId');
    const staticFilterKeys = ['createdAtFrom', 'createdAtTo', 'updatedAtFrom', 'updatedAtTo', 
                              'business', 'route', 'region', 'status', 'source', 'clientProduct'];
    
    if (typeId) {
        const newClientTypeId = parseInt(typeId);
        
        if (savedClientTypeId && parseInt(savedClientTypeId) !== newClientTypeId) {
            const cleanedFilters = {};
            Object.keys(window.selectedFilters).forEach(key => {
                if (staticFilterKeys.includes(key) || key.endsWith('From') || key.endsWith('To')) {
                    cleanedFilters[key] = window.selectedFilters[key];
                }
            });
            window.selectedFilters = cleanedFilters;
            localStorage.setItem('selectedFilters', JSON.stringify(cleanedFilters));
        }
        
        localStorage.setItem('currentClientTypeId', newClientTypeId.toString());
        currentClientTypeId = newClientTypeId;
        await loadClientType(currentClientTypeId);
        await loadClientTypeFields(currentClientTypeId);
        buildDynamicTable();
        buildDynamicFilters();
        
        const validFieldNames = new Set(filterableFields.map(f => f.fieldName));
        const staticFilterKeys = ['createdAtFrom', 'createdAtTo', 'updatedAtFrom', 'updatedAtTo', 
                                  'business', 'route', 'region', 'status', 'source', 'clientProduct'];
        
        const cleanedFilters = {};
        Object.keys(window.selectedFilters).forEach(key => {
            const normalizedKey = key.toLowerCase();
            if (staticFilterKeys.includes(normalizedKey) || normalizedKey.endsWith('from') || normalizedKey.endsWith('to')) {
                const value = window.selectedFilters[key];
                if (value !== null && value !== undefined && value !== '' && 
                    !(Array.isArray(value) && value.length === 0)) {
                    cleanedFilters[normalizedKey] = value;
                }
            } else if (validFieldNames.has(key)) {
                const value = window.selectedFilters[key];
                if (value !== null && value !== undefined && value !== '' && 
                    !(Array.isArray(value) && (value.length === 0 || (value.length === 1 && (value[0] === '' || value[0] === 'null'))))) {
                    cleanedFilters[key] = value;
                }
            }
        });
        window.selectedFilters = cleanedFilters;
        if (Object.keys(cleanedFilters).length === 0) {
            localStorage.removeItem('selectedFilters');
        } else {
            localStorage.setItem('selectedFilters', JSON.stringify(cleanedFilters));
        }
        
        filterableFields.forEach(field => {
            const filterId = `filter-${field.fieldName}`;
            if (window.selectedFilters[field.fieldName]) {
                if (field.fieldType === 'DATE') {
                    const fromInput = document.getElementById(`${filterId}-from`);
                    const toInput = document.getElementById(`${filterId}-to`);
                    if (fromInput && window.selectedFilters[`${field.fieldName}From`]) {
                        fromInput.value = window.selectedFilters[`${field.fieldName}From`][0] || '';
                    }
                    if (toInput && window.selectedFilters[`${field.fieldName}To`]) {
                        toInput.value = window.selectedFilters[`${field.fieldName}To`][0] || '';
                    }
                } else if (field.fieldType === 'LIST') {
                    if (customSelects[filterId] && window.selectedFilters[field.fieldName]) {
                        const savedValues = window.selectedFilters[field.fieldName];
                        if (Array.isArray(savedValues) && savedValues.length > 0) {
                            const validValues = savedValues.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                            if (validValues.length > 0) {
                                customSelects[filterId].setValue(validValues);
                            }
                        }
                    }
                } else if (field.fieldType === 'BOOLEAN') {
                    const select = document.getElementById(filterId);
                    if (select && window.selectedFilters[field.fieldName] && window.selectedFilters[field.fieldName].length > 0) {
                        const savedValue = window.selectedFilters[field.fieldName][0];
                        if (savedValue && savedValue !== '' && savedValue !== 'null') {
                            select.value = savedValue;
                            if (customSelects[filterId]) {
                                customSelects[filterId].setValue(savedValue);
                            }
                        }
                    }
                } else {
                    const input = document.getElementById(filterId);
                    if (input && window.selectedFilters[field.fieldName]) {
                        input.value = Array.isArray(window.selectedFilters[field.fieldName]) 
                            ? window.selectedFilters[field.fieldName][0] 
                            : window.selectedFilters[field.fieldName];
                    }
                }
            }
        });
        updateFilterCounter();
    } else {
        await loadDefaultClientData();
    }
    
    await loadEntitiesAndApplyFilters();
    
    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
});

async function loadEntitiesAndApplyFilters() {
    try {
        const response = await fetch('/api/v1/entities');
        if (!response.ok) return;
        
        const data = await response.json();
        availableStatuses = data.statuses || [];
        availableRegions = data.regions || [];
        availableSources = data.sources || [];
        availableRoutes = data.routes || [];
        availableBusiness = data.businesses || [];
        availableUsers = data.users || [];
        availableProducts = data.products || [];
        availableClientProducts = data.clientProducts || [];

        statusMap = new Map(availableStatuses.map(item => [item.id, item.name]));
        regionMap = new Map(availableRegions.map(item => [item.id, item.name]));
        sourceMap = new Map(availableSources.map(item => [item.id, item.name]));
        routeMap = new Map(availableRoutes.map(item => [item.id, item.name]));
        businessMap = new Map(availableBusiness.map(item => [item.id, item.name]));
        clientProductMap = new Map(availableClientProducts.map(item => [item.id, item.name]));
        userMap = new Map(availableUsers.map(item => [item.id, item.name]));
        productMap = new Map(availableProducts.map(item => [item.id, item.name]));

        const filterForm = document.getElementById('filterForm');
        if (filterForm) {
            if (window.selectedFilters['createdAtFrom']) {
                const fromInput = filterForm.querySelector('#createdAtFrom');
                if (fromInput) fromInput.value = window.selectedFilters['createdAtFrom'][0];
            }
            if (window.selectedFilters['createdAtTo']) {
                const toInput = filterForm.querySelector('#createdAtTo');
                if (toInput) toInput.value = window.selectedFilters['createdAtTo'][0];
            }
            if (window.selectedFilters['updatedAtFrom']) {
                const fromInput = filterForm.querySelector('#updatedAtFrom');
                if (fromInput) fromInput.value = window.selectedFilters['updatedAtFrom'][0];
            }
            if (window.selectedFilters['updatedAtTo']) {
                const toInput = filterForm.querySelector('#updatedAtTo');
                if (toInput) toInput.value = window.selectedFilters['updatedAtTo'][0];
            }
        }
    } catch (error) {
        console.error('Error loading entities:', error);
    }
}

async function loadClientType(typeId) {
    try {
        const response = await fetch(`/api/v1/client-type/${typeId}`);
        if (!response.ok) throw new Error('Failed to load client type');
        currentClientType = await response.json();
        document.title = currentClientType.name;
    } catch (error) {
        console.error('Error loading client type:', error);
    }
}

async function loadClientTypeFields(typeId) {
    try {
        const [fieldsRes, visibleRes, searchableRes, filterableRes, visibleInCreateRes] = await Promise.all([
            fetch(`/api/v1/client-type/${typeId}/field`),
            fetch(`/api/v1/client-type/${typeId}/field/visible`),
            fetch(`/api/v1/client-type/${typeId}/field/searchable`),
            fetch(`/api/v1/client-type/${typeId}/field/filterable`),
            fetch(`/api/v1/client-type/${typeId}/field/visible-in-create`)
        ]);
        
        clientTypeFields = await fieldsRes.json();
        visibleFields = await visibleRes.json();
        searchableFields = await searchableRes.json();
        filterableFields = await filterableRes.json();
        visibleInCreateFields = await visibleInCreateRes.json();
    } catch (error) {
        console.error('Error loading fields:', error);
    }
}

async function loadDefaultClientData() {
    try {
        const [statusesRes, regionsRes, sourcesRes, routesRes, businessRes, clientProductsRes] = await Promise.all([
            fetch('/api/v1/status'),
            fetch('/api/v1/region'),
            fetch('/api/v1/source'),
            fetch('/api/v1/route'),
            fetch('/api/v1/business'),
            fetch('/api/v1/clientProduct')
        ]);
        
        availableStatuses = await statusesRes.json();
        availableRegions = await regionsRes.json();
        availableSources = await sourcesRes.json();
        availableRoutes = await routesRes.json();
        availableBusiness = await businessRes.json();
        availableClientProducts = await clientProductsRes.json();
        
        statusMap = new Map(availableStatuses.map(s => [s.id, s.name]));
        regionMap = new Map(availableRegions.map(r => [r.id, r.name]));
        sourceMap = new Map(availableSources.map(s => [s.id, s.name]));
        routeMap = new Map(availableRoutes.map(r => [r.id, r.name]));
        businessMap = new Map(availableBusiness.map(b => [b.id, b.name]));
        clientProductMap = new Map(availableClientProducts.map(cp => [cp.id, cp.name]));
    } catch (error) {
        console.error('Error loading default data:', error);
    }
}

function buildDynamicTable() {
    if (!currentClientType || !visibleFields.length) return;
    
    const thead = document.querySelector('#client-list table thead tr');
    if (!thead) return;
    
    thead.innerHTML = '';
    
    const nameTh = document.createElement('th');
    nameTh.textContent = currentClientType.nameFieldLabel || 'Компанія';
    nameTh.setAttribute('data-sort', 'company');
    thead.appendChild(nameTh);
    
    visibleFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
    visibleFields.forEach(field => {
        const th = document.createElement('th');
        th.textContent = field.fieldLabel;
        if (field.isSearchable) {
            th.setAttribute('data-sort', field.fieldName);
        }
        thead.appendChild(th);
    });
}

function buildDynamicFilters() {
    if (!filterForm || !filterableFields.length) return;
    
    const existingFilters = filterForm.querySelectorAll('h2, .filter-block, .select-section-item');
    existingFilters.forEach(el => el.remove());
    
    filterableFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
    filterableFields.forEach(field => {
        if (field.fieldType === 'DATE') {
            const h2 = document.createElement('h2');
            h2.textContent = field.fieldLabel + ':';
            filterForm.appendChild(h2);
            
            const filterBlock = document.createElement('div');
            filterBlock.className = 'filter-block';
            filterBlock.innerHTML = `
                <label class="from-to-style" for="filter-${field.fieldName}-from">Від:</label>
                <input type="date" id="filter-${field.fieldName}-from" name="${field.fieldName}From"><br><br>
                <label class="from-to-style" for="filter-${field.fieldName}-to">До:</label>
                <input type="date" id="filter-${field.fieldName}-to" name="${field.fieldName}To"><br><br>
            `;
            filterForm.appendChild(filterBlock);
        } else if (field.fieldType === 'LIST') {
            const selectItem = document.createElement('div');
            selectItem.className = 'select-section-item';
            selectItem.innerHTML = `
                <br>
                <label class="select-label-style" for="filter-${field.fieldName}">${field.fieldLabel}:</label>
                <select id="filter-${field.fieldName}" name="${field.fieldName}" ${field.allowMultiple ? 'multiple' : ''}>
                </select>
            `;
            filterForm.appendChild(selectItem);
            
            const select = selectItem.querySelector('select');
            if (field.listValues && field.listValues.length > 0) {
                field.listValues.forEach(listValue => {
                    const option = document.createElement('option');
                    option.value = listValue.id;
                    option.textContent = listValue.value;
                    select.appendChild(option);
                });
            }
            
            if (select && !customSelects[`filter-${field.fieldName}`]) {
                customSelects[`filter-${field.fieldName}`] = createCustomSelect(select);
            }
        } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE' || field.fieldType === 'NUMBER') {
            const selectItem = document.createElement('div');
            selectItem.className = 'select-section-item';
            selectItem.innerHTML = `
                <br>
                <label class="select-label-style" for="filter-${field.fieldName}">${field.fieldLabel}:</label>
                <input type="${field.fieldType === 'NUMBER' ? 'number' : 'text'}" 
                       id="filter-${field.fieldName}" 
                       name="${field.fieldName}" 
                       placeholder="Пошук...">
            `;
            filterForm.appendChild(selectItem);
        } else if (field.fieldType === 'BOOLEAN') {
            const selectItem = document.createElement('div');
            selectItem.className = 'select-section-item';
            selectItem.innerHTML = `
                <br>
                <label class="select-label-style" for="filter-${field.fieldName}">${field.fieldLabel}:</label>
                <select id="filter-${field.fieldName}" name="${field.fieldName}">
                    <option value="">Всі</option>
                    <option value="true">Так</option>
                    <option value="false">Ні</option>
                </select>
            `;
            filterForm.appendChild(selectItem);
            
            const select = selectItem.querySelector('select');
            if (select && !customSelects[`filter-${field.fieldName}`]) {
                customSelects[`filter-${field.fieldName}`] = createCustomSelect(select);
            }
        }
    });
}

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

async function renderClients(clients) {
    tableBody.innerHTML = '';
    
    if (currentClientTypeId && visibleFields.length > 0) {
        await renderClientsWithDynamicFields(clients);
    } else {
        renderClientsWithDefaultFields(clients);
    }
}

async function renderClientsWithDynamicFields(clients) {
    const fieldValuesPromises = clients.map(client => loadClientFieldValues(client.id));
    const allFieldValues = await Promise.all(fieldValuesPromises);
    
    clients.forEach((client, index) => {
        const row = document.createElement('tr');
        row.classList.add('client-row');
        
        const nameFieldLabel = currentClientType ? currentClientType.nameFieldLabel : 'Компанія';
        let html = `<td data-label="${nameFieldLabel}" class="company-cell" style="cursor: pointer;">${client.company || ''}</td>`;
        
        const fieldValues = allFieldValues[index];
        const fieldValuesMap = new Map();
        fieldValues.forEach(fv => {
            if (!fieldValuesMap.has(fv.fieldId)) {
                fieldValuesMap.set(fv.fieldId, []);
            }
            fieldValuesMap.get(fv.fieldId).push(fv);
        });
        
        client._fieldValues = fieldValues;
        
        visibleFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        visibleFields.forEach(field => {
            const values = fieldValuesMap.get(field.id) || [];
            let cellValue = '';
            
            if (values.length > 0) {
                if (field.allowMultiple) {
                    cellValue = values.map(v => formatFieldValue(v, field)).join('<br>');
                } else {
                    cellValue = formatFieldValue(values[0], field);
                }
            }
            
            html += `<td data-label="${field.fieldLabel}">${cellValue}</td>`;
        });
        
        row.innerHTML = html;
        tableBody.appendChild(row);
        
        const companyCell = row.querySelector('.company-cell');
        if (companyCell) {
            companyCell.addEventListener('click', () => {
                loadClientDetails(client);
            });
        }
    });
}

function renderClientsWithDefaultFields(clients) {
    clients.forEach(client => {
        const row = document.createElement('tr');
        row.classList.add('client-row');

        row.innerHTML = `
            <td data-label="Компанія" data-sort="company" class="company-cell" style="cursor: pointer;">${client.company || ''}</td>
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
        const companyCell = row.querySelector('.company-cell');
        if (companyCell) {
            companyCell.addEventListener('click', () => {
                loadClientDetails(client);
            });
        }
    });
}

function formatFieldValue(fieldValue, field) {
    if (!fieldValue) return '';
    
    switch (field.fieldType) {
        case 'TEXT':
        case 'PHONE':
            return fieldValue.valueText || '';
        case 'NUMBER':
            return fieldValue.valueNumber || '';
        case 'DATE':
            return fieldValue.valueDate || '';
        case 'BOOLEAN':
            if (fieldValue.valueBoolean === true) return 'Так';
            if (fieldValue.valueBoolean === false) return 'Ні';
            return '';
        case 'LIST':
            return fieldValue.valueListValue || '';
        default:
            return '';
    }
}

function formatFieldValueForModal(fieldValue, field) {
    if (!fieldValue) return '';
    
    switch (field.fieldType) {
        case 'TEXT':
            return fieldValue.valueText || '';
        case 'PHONE':
            const phone = fieldValue.valueText || '';
            return phone ? `<a href="tel:${phone}">${phone}</a>` : '';
        case 'NUMBER':
            return fieldValue.valueNumber || '';
        case 'DATE':
            return fieldValue.valueDate || '';
        case 'BOOLEAN':
            if (fieldValue.valueBoolean === true) return 'Так';
            if (fieldValue.valueBoolean === false) return 'Ні';
            return '';
        case 'LIST':
            return fieldValue.valueListValue || '';
        default:
            return '';
    }
}

async function loadClientFieldValues(clientId) {
    try {
        const response = await fetch(`/api/v1/client/${clientId}/field-values`);
        if (!response.ok) return [];
        return await response.json();
    } catch (error) {
        console.error('Error loading field values:', error);
        return [];
    }
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

    if (currentClientTypeId) {
        queryParams += `&clientTypeId=${currentClientTypeId}`;
    }

    if (searchTerm) {
        queryParams += `&q=${encodeURIComponent(searchTerm)}`;
    }

    const cleanedFilters = {};
    Object.keys(selectedFilters).forEach(key => {
        const value = selectedFilters[key];
        if (value !== null && value !== undefined) {
            if (Array.isArray(value)) {
                const filteredArray = value.filter(v => v !== null && v !== undefined && v !== '');
                if (filteredArray.length > 0) {
                    cleanedFilters[key] = filteredArray;
                }
            } else if (typeof value === 'string' && value.trim() !== '') {
                cleanedFilters[key] = value;
            } else if (typeof value !== 'string') {
                cleanedFilters[key] = value;
            }
        }
    });
    
    if (Object.keys(cleanedFilters).length > 0) {
        queryParams += `&filters=${encodeURIComponent(JSON.stringify(cleanedFilters))}`;
    }

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
        console.error('Error loading clients:', error);
        handleError(error);
    } finally {
        loaderBackdrop.style.display = 'none';
    }
}


function loadClientDetails(client) {
    showClientModal(client);
}

async function showClientModal(client) {
    document.getElementById('client-modal').setAttribute('data-client-id', client.id);

    document.getElementById('modal-client-id').innerText = client.id;
    
    const modalContent = document.querySelector('.modal-content-client');
    const existingFields = modalContent.querySelectorAll('p[data-field-id]');
    existingFields.forEach(el => el.remove());
    
    const nameFieldLabel = currentClientType ? currentClientType.nameFieldLabel : 'Компанія';
    document.getElementById('modal-client-company').parentElement.querySelector('strong').textContent = nameFieldLabel + ':';
    document.getElementById('modal-client-company').innerText = client.company;
    
    if (currentClientTypeId && clientTypeFields.length > 0) {
        const oldFields = ['person', 'phone', 'location', 'price-purchase', 'price-sale', 'vat', 'volumeMonth', 
                          'edrpou', 'enterpriseName', 'business', 'route', 'region', 'status', 'source', 
                          'clientProduct', 'comment', 'urgently'];
        oldFields.forEach(fieldId => {
            const fieldElement = document.getElementById(`modal-client-${fieldId}`)?.parentElement;
            if (fieldElement) {
                fieldElement.style.display = 'none';
            }
        });
        
        let fieldValues = client._fieldValues;
        if (!fieldValues) {
            fieldValues = await loadClientFieldValues(client.id);
        }
        const fieldValuesMap = new Map();
        fieldValues.forEach(fv => {
            if (!fieldValuesMap.has(fv.fieldId)) {
                fieldValuesMap.set(fv.fieldId, []);
            }
            fieldValuesMap.get(fv.fieldId).push(fv);
        });
        
        const companyP = document.getElementById('modal-client-company').parentElement;
        
        clientTypeFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        clientTypeFields.forEach(field => {
            const values = fieldValuesMap.get(field.id) || [];
            const fieldP = document.createElement('p');
            fieldP.setAttribute('data-field-id', field.id);
            
            let fieldValue = '';
            if (values.length > 0) {
                if (field.allowMultiple) {
                    fieldValue = values.map(v => formatFieldValueForModal(v, field)).join('<br>');
                } else {
                    fieldValue = formatFieldValueForModal(values[0], field);
                }
            }
            
            fieldP.innerHTML = `
                <strong>${field.fieldLabel}:</strong>
                <span id="modal-field-${field.id}" class="${!fieldValue ? 'empty-value' : ''}">${fieldValue || '—'}</span>
                <button class="edit-icon" onclick="enableEditField(${field.id}, '${field.fieldType}', ${field.allowMultiple || false})" data-field-id="${field.id}" title="Редагувати">
                    <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
                    </svg>
                </button>
            `;
            fieldP.setAttribute('data-field-type', field.fieldType);
            
            companyP.insertAdjacentElement('afterend', fieldP);
        });
    } else {
        const oldFields = ['person', 'phone', 'location', 'price-purchase', 'price-sale', 'vat', 'volumeMonth', 
                          'edrpou', 'enterpriseName', 'business', 'route', 'region', 'status', 'source', 
                          'clientProduct', 'comment', 'urgently'];
        oldFields.forEach(fieldId => {
            const fieldElement = document.getElementById(`modal-client-${fieldId}`)?.parentElement;
            if (fieldElement) {
                fieldElement.style.display = '';
            }
        });
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
        document.getElementById('modal-client-clientProduct').innerText =
            findNameByIdFromMap(clientProductMap, client.clientProductId);
        document.getElementById('modal-client-route').innerText =
            findNameByIdFromMap(routeMap, client.routeId);
        document.getElementById('modal-client-region').innerText =
            findNameByIdFromMap(regionMap, client.regionId);
        document.getElementById('modal-client-status').innerText =
            findNameByIdFromMap(statusMap, client.statusId);
        document.getElementById('modal-client-source').innerText =
            findNameByIdFromMap(sourceMap, client.sourceId);
        document.getElementById('modal-client-comment').innerText = client.comment || '';
    }
    
    document.getElementById('modal-client-created').innerText = client.createdAt || '';
    document.getElementById('modal-client-updated').innerText = client.updatedAt || '';

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


/*-------create client-------*/

var modal = document.getElementById("createClientModal");
var btn = document.getElementById("open-modal");
var span = document.getElementsByClassName("create-client-close")[0];
let editing = false;

btn.onclick = function () {
    if (!currentClientTypeId) {
        showMessage('Будь ласка, виберіть тип клієнта з навігації', 'error');
        return;
    }
    buildDynamicCreateForm();
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

function buildDynamicCreateForm() {
    if (!currentClientTypeId || !visibleInCreateFields || visibleInCreateFields.length === 0) {
        return;
    }

    const form = document.getElementById('client-form');
    form.innerHTML = '';

    const nameFieldLabel = currentClientType ? currentClientType.nameFieldLabel : 'Компанія';
    
    const nameFieldDiv = document.createElement('div');
    nameFieldDiv.className = 'form-group';
    const nameLabel = document.createElement('label');
    nameLabel.setAttribute('for', 'company');
    nameLabel.textContent = nameFieldLabel + ' *';
    const nameInput = document.createElement('input');
    nameInput.type = 'text';
    nameInput.id = 'company';
    nameInput.name = 'company';
    nameInput.required = true;
    nameInput.placeholder = nameFieldLabel;
    nameFieldDiv.appendChild(nameLabel);
    nameFieldDiv.appendChild(nameInput);
    form.appendChild(nameFieldDiv);

    visibleInCreateFields.forEach((field, index) => {
        const fieldDiv = document.createElement('div');
        fieldDiv.className = 'form-group';
        fieldDiv.setAttribute('data-field-id', field.id);
        fieldDiv.setAttribute('data-field-type', field.fieldType);

        const label = document.createElement('label');
        label.setAttribute('for', `field-${field.id}`);
        label.textContent = field.fieldLabel + (field.isRequired ? ' *' : '');
        fieldDiv.appendChild(label);

        let input;
        if (field.fieldType === 'TEXT') {
            input = document.createElement('input');
            input.type = 'text';
            input.id = `field-${field.id}`;
            input.name = `field-${field.id}`;
            input.required = field.isRequired || false;
            input.placeholder = field.fieldLabel;
        } else if (field.fieldType === 'NUMBER') {
            input = document.createElement('input');
            input.type = 'number';
            input.id = `field-${field.id}`;
            input.name = `field-${field.id}`;
            input.required = field.isRequired || false;
            input.placeholder = field.fieldLabel;
        } else if (field.fieldType === 'DATE') {
            input = document.createElement('input');
            input.type = 'date';
            input.id = `field-${field.id}`;
            input.name = `field-${field.id}`;
            input.required = field.isRequired || false;
        } else if (field.fieldType === 'PHONE') {
            input = document.createElement('input');
            input.type = 'text';
            input.id = `field-${field.id}`;
            input.name = `field-${field.id}`;
            input.required = field.isRequired || false;
            input.placeholder = field.allowMultiple ? 'Телефони (розділяємо комою)' : 'Телефон';
            if (field.validationPattern) {
                input.pattern = field.validationPattern;
            }
            if (field.allowMultiple) {
                const outputDiv = document.createElement('div');
                outputDiv.id = `output-${field.id}`;
                outputDiv.className = 'phone-output';
                fieldDiv.appendChild(outputDiv);
                input.addEventListener('input', () => updatePhoneOutput(field.id, input.value));
            }
        } else if (field.fieldType === 'LIST') {
            input = document.createElement('select');
            input.id = `field-${field.id}`;
            input.name = `field-${field.id}`;
            input.required = field.isRequired || false;
            if (field.allowMultiple) {
                input.multiple = true;
            }
            if (field.listValues && field.listValues.length > 0) {
                field.listValues.forEach(listValue => {
                    const option = document.createElement('option');
                    option.value = listValue.id;
                    option.textContent = listValue.value;
                    if (!field.allowMultiple) {
                        option.selected = false;
                    }
                    input.appendChild(option);
                });
            }
            if (!field.allowMultiple) {
                input.selectedIndex = -1;
            }
            fieldDiv.appendChild(input);
            form.appendChild(fieldDiv);
            setTimeout(() => {
                if (typeof createCustomSelect === 'function') {
                    const customSelect = createCustomSelect(input);
                    customSelects[`field-${field.id}`] = customSelect;
                    if (field.listValues && field.listValues.length > 0) {
                        const listData = field.listValues.map(lv => ({
                            id: lv.id,
                            name: lv.value
                        }));
                        customSelect.populate(listData);
                    }
                    if (!field.allowMultiple) {
                        customSelect.reset();
                    }
                }
            }, 0);
            return;
        } else if (field.fieldType === 'BOOLEAN') {
            input = document.createElement('select');
            input.id = `field-${field.id}`;
            input.name = `field-${field.id}`;
            input.required = field.isRequired || false;
            const defaultOption = document.createElement('option');
            defaultOption.value = '';
            defaultOption.textContent = 'Виберіть...';
            defaultOption.disabled = true;
            defaultOption.selected = true;
            input.appendChild(defaultOption);
            const yesOption = document.createElement('option');
            yesOption.value = 'true';
            yesOption.textContent = 'Так';
            input.appendChild(yesOption);
            const noOption = document.createElement('option');
            noOption.value = 'false';
            noOption.textContent = 'Ні';
            input.appendChild(noOption);
        }
        fieldDiv.appendChild(input);
        form.appendChild(fieldDiv);
    });

    const submitButton = document.createElement('button');
    submitButton.type = 'submit';
    submitButton.id = 'save-button';
    submitButton.textContent = 'Зберегти';
    form.appendChild(submitButton);

    const companyInput = document.getElementById('company');
    if (companyInput) {
        const validateForm = () => {
            const isCompanyFilled = companyInput.value.trim() !== '';
            submitButton.disabled = !isCompanyFilled;
        };
        companyInput.addEventListener('input', validateForm);
        validateForm();
    }
}

function updatePhoneOutput(fieldId, value) {
    const outputDiv = document.getElementById(`output-${fieldId}`);
    if (!outputDiv) return;
    outputDiv.innerHTML = '';

    let formattedNumbers = value.split(',')
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

function resetForm() {
    const form = document.getElementById('client-form');
    if (currentClientTypeId) {
        buildDynamicCreateForm();
    } else {
        form.reset();
    }
}

const defaultValues = {
    region: '136',
    status: '24',
    route: '66',
    business: '1',
    clientProduct: '1',
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

        if (!currentClientTypeId) {
            showMessage('Будь ласка, виберіть тип клієнта з навігації', 'error');
            return;
        }

        loaderBackdrop.style.display = 'flex';

        const formData = new FormData(this);
        const clientData = {
            clientTypeId: currentClientTypeId,
            company: formData.get('company'),
            fieldValues: []
        };

        visibleInCreateFields.forEach(field => {
            const fieldValue = formData.get(`field-${field.id}`);
            const fieldValueMultiple = formData.getAll(`field-${field.id}`);
            
            if (field.fieldType === 'PHONE' && field.allowMultiple) {
                const phoneValue = formData.get(`field-${field.id}`);
                if (phoneValue) {
                    const phones = phoneValue.split(',')
                        .map(num => num.trim())
                        .filter(num => num.length > 0)
                        .map(formatPhoneNumber)
                        .filter(num => num !== null);
                    
                    phones.forEach((phone, index) => {
                        clientData.fieldValues.push({
                            fieldId: field.id,
                            valueText: phone,
                            displayOrder: index
                        });
                    });
                }
            } else if (field.fieldType === 'LIST' && field.allowMultiple) {
                if (fieldValueMultiple && fieldValueMultiple.length > 0) {
                    fieldValueMultiple.forEach((value, index) => {
                        if (value) {
                            clientData.fieldValues.push({
                                fieldId: field.id,
                                valueListId: parseInt(value),
                                displayOrder: index
                            });
                        }
                    });
                }
            } else if (fieldValue) {
                const fieldValueData = {
                    fieldId: field.id,
                    displayOrder: 0
                };
                
                if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                    fieldValueData.valueText = fieldValue;
                } else if (field.fieldType === 'NUMBER') {
                    fieldValueData.valueNumber = parseFloat(fieldValue);
                } else if (field.fieldType === 'DATE') {
                    fieldValueData.valueDate = fieldValue;
                } else if (field.fieldType === 'BOOLEAN') {
                    fieldValueData.valueBoolean = fieldValue === 'true';
                } else if (field.fieldType === 'LIST') {
                    fieldValueData.valueListId = parseInt(fieldValue);
                }
                
                clientData.fieldValues.push(fieldValueData);
            }
        });

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
        if (selectId.startsWith('filter-')) {
            const select = document.getElementById(selectId);
            if (select) {
                const name = select.name;
                const values = customSelects[selectId].getValue();
                const filteredValues = values.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                if (filteredValues.length > 0) {
                    selectedFilters[name] = filteredValues;
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

    filterableFields.forEach(field => {
        if (field.fieldType === 'DATE') {
            const fromValue = formData.get(`${field.fieldName}From`);
            const toValue = formData.get(`${field.fieldName}To`);
            if (fromValue) selectedFilters[`${field.fieldName}From`] = [fromValue];
            if (toValue) selectedFilters[`${field.fieldName}To`] = [toValue];
        } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE' || field.fieldType === 'NUMBER') {
            const value = formData.get(field.fieldName);
            if (value && value.trim() !== '') {
                selectedFilters[field.fieldName] = value.trim();
            }
        } else if (field.fieldType === 'BOOLEAN') {
            const value = formData.get(field.fieldName);
            if (value && value !== '' && value !== 'null') {
                selectedFilters[field.fieldName] = [value];
            }
        } else if (field.fieldType === 'LIST') {
            const select = document.getElementById(`filter-${field.fieldName}`);
            if (select && customSelects[`filter-${field.fieldName}`]) {
                const selectedValues = customSelects[`filter-${field.fieldName}`].getValue();
                if (selectedValues && selectedValues.length > 0) {
                    selectedFilters[field.fieldName] = selectedValues;
                }
            }
        }
    });

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
            if (selectId.startsWith('filter-')) {
                if (customSelects[selectId] && typeof customSelects[selectId].reset === 'function') {
                    customSelects[selectId].reset();
                } else if (customSelects[selectId] && typeof customSelects[selectId].setValue === 'function') {
                    customSelects[selectId].setValue([]);
                }
            }
        });
    }

    const searchInput = document.getElementById('inputSearch');
    if (searchInput) {
        searchInput.value = '';
    }

    localStorage.removeItem('selectedFilters');
    localStorage.removeItem('searchTerm');
    window.selectedFilters = {};

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




