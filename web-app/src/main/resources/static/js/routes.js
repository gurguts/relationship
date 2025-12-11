const prevPageButton = document.getElementById('prev-btn');
const nextPageButton = document.getElementById('next-btn');
const paginationInfo = document.getElementById('pagination-info');
const allClientInfo = document.getElementById('all-client-info');
const loaderBackdrop = document.getElementById('loader-backdrop');
let currentSort = 'updatedAt';
let currentDirection = 'DESC';
const filterForm = document.getElementById('filterForm');
const customSelects = {};

let availableSources = [];
let sourceMap;
let availableUsers = [];
let userMap;
let availableProducts = [];
let productMap;
let availableContainers = [];
let containerMap;

const userId = localStorage.getItem('userId');
const selectedFilters = {};

function getUserAuthorities() {
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
        return [];
    }
    return userAuthorities;
}

function canEditStrangers() {
    const userAuthorities = getUserAuthorities();
    return userAuthorities.includes('client_stranger:edit') || 
           userAuthorities.includes('system:admin');
}

function isOwnClient(client) {
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
    
    const currentUserId = userId ? Number(userId) : null;
    const sourceUserId = (source.userId !== null && source.userId !== undefined) ? Number(source.userId) : null;
    
    return currentUserId != null && sourceUserId != null && Number(sourceUserId) === Number(currentUserId);
}

function canEditClient(client) {
    if (canEditStrangers()) {
        return true;
    }
    
    return isOwnClient(client);
}

function canEditCompany(client) {
    if (canEditStrangers()) {
        return true;
    }
    return isOwnClient(client);
}

function canEditSource() {
    return canEditStrangers();
}

function canDeleteClient(client) {
    if (canEditStrangers()) {
        return true;
    }
    return isOwnClient(client);
}

const API_URL = '/api/v1/client';

let currentPage = 0;
let pageSize = 50;
const tableBody = document.getElementById('client-table-body');

let currentClientTypeId = null;
let currentClientType = null;
let clientTypeFields = [];
let visibleFields = [];
window.visibleFields = visibleFields;
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
                if (normalizedKey === 'source' ||
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
    
    if (!typeId) {
        await showClientTypeSelectionModal();
        return;
    }
    
    await updateNavigationWithCurrentType(typeId);
    
    const savedClientTypeId = localStorage.getItem('currentClientTypeId');
    const staticFilterKeys = ['createdAtFrom', 'createdAtTo', 'updatedAtFrom', 'updatedAtTo', 'source', 'showInactive'];
    
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
        
        await updateNavigationWithCurrentType(newClientTypeId);
        
        await loadClientType(currentClientTypeId);
        await loadClientTypeFields(currentClientTypeId);
        buildDynamicTable();
        buildDynamicFilters();
        
        const validFieldNames = new Set(filterableFields.map(f => f.fieldName));
        
        const cleanedFilters = {};
        Object.keys(window.selectedFilters).forEach(key => {
            const normalizedKey = key.toLowerCase();
            const normalizedStaticKeys = staticFilterKeys.map(k => k.toLowerCase());
            if (normalizedStaticKeys.includes(normalizedKey) || normalizedKey.endsWith('from') || normalizedKey.endsWith('to')) {
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
        
        if (filterableFields && filterableFields.length > 0) {
            filterableFields.forEach(field => {
                const filterId = `filter-${field.fieldName}`;
                if (field.fieldType === 'DATE') {
                    const fromInput = document.getElementById(`${filterId}-from`);
                    const toInput = document.getElementById(`${filterId}-to`);
                    if (fromInput && window.selectedFilters[`${field.fieldName}From`]) {
                        fromInput.value = window.selectedFilters[`${field.fieldName}From`][0] || '';
                    }
                    if (toInput && window.selectedFilters[`${field.fieldName}To`]) {
                        toInput.value = window.selectedFilters[`${field.fieldName}To`][0] || '';
                    }
                } else if (field.fieldType === 'NUMBER') {
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
                        }
                    }
                } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                    const input = document.getElementById(filterId);
                    if (input && window.selectedFilters[field.fieldName]) {
                        input.value = Array.isArray(window.selectedFilters[field.fieldName]) 
                            ? window.selectedFilters[field.fieldName][0] 
                            : window.selectedFilters[field.fieldName];
                    }
                }
            });
        }
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
        availableSources = data.sources || [];
        sourceMap = new Map(availableSources.map(item => [item.id, item.name]));

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

            const sourceSelect = filterForm.querySelector('#filter-source');
            if (sourceSelect && !customSelects['filter-source'] && availableSources && availableSources.length > 0) {
                const sourceData = availableSources.map(s => ({
                    id: s.id,
                    name: s.name
                }));
                if (typeof createCustomSelect === 'function') {
                    const customSelect = createCustomSelect(sourceSelect, true);
                    if (customSelect) {
                        customSelects['filter-source'] = customSelect;
                        customSelect.populate(sourceData);
                    }
                }
            }

            if (window.selectedFilters['source'] && customSelects['filter-source']) {
                const savedSources = window.selectedFilters['source'];
                if (Array.isArray(savedSources) && savedSources.length > 0) {
                    const validSources = savedSources.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                    if (validSources.length > 0) {
                        customSelects['filter-source'].setValue(validSources);
                    }
                }
            }

            const showInactiveCheckbox = filterForm.querySelector('#filter-show-inactive');
            if (showInactiveCheckbox && window.selectedFilters['showInactive'] && window.selectedFilters['showInactive'][0] === 'true') {
                showInactiveCheckbox.checked = true;
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
        window.clientTypeFields = clientTypeFields;
        visibleFields = await visibleRes.json();
        window.visibleFields = visibleFields;
        filterableFields = await filterableRes.json();
        visibleInCreateFields = await visibleInCreateRes.json();
    } catch (error) {
        console.error('Error loading fields:', error);
    }
}

async function loadDefaultClientData() {
    try {
        const sourcesRes = await fetch('/api/v1/source');
        availableSources = await sourcesRes.json();
        sourceMap = new Map(availableSources.map(s => [s.id, s.name]));
    } catch (error) {
        console.error('Error loading default data:', error);
    }
}

function buildDynamicTable() {
    if (!currentClientType) return;
    
    const thead = document.querySelector('#client-list table thead tr');
    if (!thead) return;
    
    thead.innerHTML = '';

    const actionsTh = document.createElement('th');
    actionsTh.textContent = 'Дії';
    actionsTh.style.width = '150px';
    actionsTh.style.minWidth = '150px';
    actionsTh.style.maxWidth = '150px';
    actionsTh.style.flexShrink = '0';
    actionsTh.style.flexGrow = '0';
    thead.appendChild(actionsTh);

    const staticFields = (visibleFields || []).filter(f => f.isStatic);
    const dynamicFields = (visibleFields || []).filter(f => !f.isStatic);

    const hasCompanyStatic = staticFields.some(f => f.staticFieldName === 'company');
    const hasSourceStatic = staticFields.some(f => f.staticFieldName === 'source');

    const allFields = [...staticFields, ...dynamicFields];

    if (!hasCompanyStatic) {
        allFields.push({
            id: -1,
            fieldName: 'company',
            fieldLabel: currentClientType.nameFieldLabel || 'Компанія',
            isStatic: false,
            displayOrder: 0,
            columnWidth: 200,
            isSearchable: true
        });
    }
    
    if (!hasSourceStatic) {
        allFields.push({
            id: -2,
            fieldName: 'source',
            fieldLabel: 'Залучення',
            isStatic: false,
            displayOrder: 999,
            columnWidth: 200,
            isSearchable: false
        });
    }

    allFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));

    allFields.forEach(field => {
        const th = document.createElement('th');
        th.textContent = field.fieldLabel;
        th.setAttribute('data-field-id', field.id);
        if (field.isStatic) {
            th.setAttribute('data-static-field', field.staticFieldName);
            if (field.staticFieldName === 'company') {
                th.setAttribute('data-sort', 'company');
            }
        } else if (field.fieldName === 'company') {
            th.setAttribute('data-sort', 'company');
        } else if (field.isSearchable) {
            th.setAttribute('data-sort', field.fieldName);
        }
        if (field.columnWidth) {
            th.style.width = field.columnWidth + 'px';
            th.style.minWidth = field.columnWidth + 'px';
            th.style.maxWidth = field.columnWidth + 'px';
        }
        th.style.flexShrink = '0';
        th.style.flexGrow = '0';
        thead.appendChild(th);
    });

    if (typeof initColumnResizer === 'function' && currentClientTypeId) {
        setTimeout(() => {
            initColumnResizer(currentClientTypeId);
            if (typeof applyColumnWidths === 'function') {
                applyColumnWidths(currentClientTypeId);
            }
        }, 0);
    }
}

function buildDynamicFilters() {
    if (!filterForm) return;

    if (buildDynamicFilters._isBuilding) {
        return;
    }
    buildDynamicFilters._isBuilding = true;
    
    try {
        Object.keys(customSelects).forEach(selectId => {
            if (selectId.startsWith('filter-') && selectId !== 'filter-source') {
                const customSelect = customSelects[selectId];
                if (customSelect && typeof customSelect.reset === 'function') {
                    try {
                        customSelect.reset();
                    } catch (e) {
                        console.warn('Error resetting custom select:', e);
                    }
                }
                delete customSelects[selectId];
            }
        });

        const existingFilters = filterForm.querySelectorAll('h2, .filter-block, .select-section-item');
        existingFilters.forEach(el => {
            const selects = el.querySelectorAll('select');
            selects.forEach(sel => {
                sel.innerHTML = '';
            });
            el.remove();
        });

        if (customSelects['filter-source']) {
            try {
                if (customSelects['filter-source'] && typeof customSelects['filter-source'].reset === 'function') {
                    customSelects['filter-source'].reset();
                }
            } catch (e) {
                console.warn('Error resetting source custom select:', e);
            }
            delete customSelects['filter-source'];
        }

        const createdAtH2 = document.createElement('h2');
        createdAtH2.textContent = 'Дата створення:';
        filterForm.appendChild(createdAtH2);
        
        const createdAtBlock = document.createElement('div');
        createdAtBlock.className = 'filter-block';
        createdAtBlock.innerHTML = `
            <label class="from-to-style" for="filter-createdAt-from">Від:</label>
            <input type="date" id="filter-createdAt-from" name="createdAtFrom"><br><br>
            <label class="from-to-style" for="filter-createdAt-to">До:</label>
            <input type="date" id="filter-createdAt-to" name="createdAtTo"><br><br>
        `;
        filterForm.appendChild(createdAtBlock);
        
        const updatedAtH2 = document.createElement('h2');
        updatedAtH2.textContent = 'Дата оновлення:';
        filterForm.appendChild(updatedAtH2);
        
        const updatedAtBlock = document.createElement('div');
        updatedAtBlock.className = 'filter-block';
        updatedAtBlock.innerHTML = `
            <label class="from-to-style" for="filter-updatedAt-from">Від:</label>
            <input type="date" id="filter-updatedAt-from" name="updatedAtFrom"><br><br>
            <label class="from-to-style" for="filter-updatedAt-to">До:</label>
            <input type="date" id="filter-updatedAt-to" name="updatedAtTo"><br><br>
        `;
        filterForm.appendChild(updatedAtBlock);

        const sourceSelectItem = document.createElement('div');
        sourceSelectItem.className = 'select-section-item';
        sourceSelectItem.innerHTML = `
            <br>
            <label class="select-label-style" for="filter-source">Залучення:</label>
            <select id="filter-source" name="source" multiple>
            </select>
        `;
        filterForm.appendChild(sourceSelectItem);


        if (filterableFields && filterableFields.length > 0) {
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
                } else if (field.fieldType === 'NUMBER') {
                    const h2 = document.createElement('h2');
                    h2.textContent = field.fieldLabel + ':';
                    filterForm.appendChild(h2);
                    
                    const filterBlock = document.createElement('div');
                    filterBlock.className = 'filter-block';
                    filterBlock.innerHTML = `
                        <label class="from-to-style" for="filter-${field.fieldName}-from">Від:</label>
                        <input type="number" id="filter-${field.fieldName}-from" name="${field.fieldName}From" step="any" placeholder="Мінімум"><br><br>
                        <label class="from-to-style" for="filter-${field.fieldName}-to">До:</label>
                        <input type="number" id="filter-${field.fieldName}-to" name="${field.fieldName}To" step="any" placeholder="Максимум"><br><br>
                    `;
                    filterForm.appendChild(filterBlock);
                } else if (field.fieldType === 'LIST') {
                    const selectId = `filter-${field.fieldName}`;

                    if (customSelects[selectId]) {
                        try {
                            const oldSelect = customSelects[selectId];
                            if (oldSelect && typeof oldSelect.reset === 'function') {
                                oldSelect.reset();
                            }
                        } catch (e) {
                            console.warn('Error cleaning up old custom select:', e);
                        }
                        delete customSelects[selectId];
                    }

                    const existingContainer = document.querySelector(`.custom-select-container[data-for="${selectId}"]`);
                    if (existingContainer) {
                        existingContainer.remove();
                    }
                    
                    const selectItem = document.createElement('div');
                    selectItem.className = 'select-section-item';
                    selectItem.innerHTML = `
                        <br>
                        <label class="select-label-style" for="filter-${field.fieldName}">${field.fieldLabel}:</label>
                        <select id="filter-${field.fieldName}" name="${field.fieldName}" multiple>
                        </select>
                    `;
                    filterForm.appendChild(selectItem);
                    
                    const select = selectItem.querySelector('select');
                    if (!select) {
                        console.error('Select not found for field:', field.fieldName);
                        return;
                    }

                    if (field.listValues && field.listValues.length > 0) {
                        field.listValues.forEach(listValue => {
                            const option = document.createElement('option');
                            option.value = listValue.id;
                            option.textContent = listValue.value;
                            select.appendChild(option);
                        });
                    }

                    setTimeout(() => {
                        if (typeof createCustomSelect === 'function') {
                            const existingContainer = document.querySelector(`.custom-select-container[data-for="${selectId}"]`);
                            if (existingContainer) {
                                console.warn('Custom select container already exists for:', selectId);
                                return;
                            }
                            
                            const customSelect = createCustomSelect(select, true);
                            if (customSelect) {
                                customSelects[selectId] = customSelect;
                                
                                if (field.listValues && field.listValues.length > 0) {
                                    const listData = field.listValues.map(lv => ({
                                        id: lv.id,
                                        name: lv.value
                                    }));
                                    customSelect.populate(listData);
                                }
                            }
                        }
                    }, 0);
                } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                    const selectItem = document.createElement('div');
                    selectItem.className = 'select-section-item';
                    selectItem.innerHTML = `
                        <br>
                        <label class="select-label-style" for="filter-${field.fieldName}">${field.fieldLabel}:</label>
                        <input type="text" 
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
                }
            });
        }

        const isActiveBlock = document.createElement('div');
        isActiveBlock.className = 'filter-block';
        isActiveBlock.innerHTML = `
            <label style="display: flex; align-items: center; gap: 0.5em; margin: 0.5em 0;">
                <input type="checkbox" id="filter-show-inactive" name="showInactive" value="true">
                <span>Показати неактивних клієнтів</span>
            </label>
        `;
        filterForm.appendChild(isActiveBlock);
    } finally {
        buildDynamicFilters._isBuilding = false;
    }
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
    
    if (currentClientTypeId && visibleFields && visibleFields.length > 0) {
        await renderClientsWithDynamicFields(clients);
    } else {
        renderClientsWithDefaultFields(clients);
    }

    if (typeof applyColumnWidths === 'function' && currentClientTypeId) {
        setTimeout(() => {
            applyColumnWidths(currentClientTypeId);
        }, 0);
    }
}

async function renderClientsWithDynamicFields(clients) {
    const fieldValuesPromises = clients.map(client => loadClientFieldValues(client.id));
    const allFieldValues = await Promise.all(fieldValuesPromises);

    const staticFields = visibleFields.filter(f => f.isStatic);
    const dynamicFields = visibleFields.filter(f => !f.isStatic);

    const hasCompanyStatic = staticFields.some(f => f.staticFieldName === 'company');
    const hasSourceStatic = staticFields.some(f => f.staticFieldName === 'source');

    const allFields = [...staticFields, ...dynamicFields].sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
    
    clients.forEach((client, index) => {
        const row = document.createElement('tr');
        row.classList.add('client-row');
        
        let html = '<td class="button-td" data-label="">';
        html += `<button class="purchase-button" data-client-id="${client.id}">Закупка</button>`;
        html += `<button class="container-button" data-client-id="${client.id}">Тара</button>`;
        html += '</td>';

        if (!hasCompanyStatic) {
            const nameFieldLabel = currentClientType ? currentClientType.nameFieldLabel : 'Компанія';
            html += `<td data-label="${nameFieldLabel}" class="company-cell" style="cursor: pointer;">${client.company || ''}</td>`;
        }
        
        const fieldValues = allFieldValues[index];
        const fieldValuesMap = new Map();
        fieldValues.forEach(fv => {
            if (!fieldValuesMap.has(fv.fieldId)) {
                fieldValuesMap.set(fv.fieldId, []);
            }
            fieldValuesMap.get(fv.fieldId).push(fv);
        });
        
        client._fieldValues = fieldValues;

        allFields.forEach(field => {
            let cellValue = '';
            
            if (field.isStatic) {
                switch (field.staticFieldName) {
                    case 'company':
                        cellValue = client.company || '';
                        html += `<td data-label="${field.fieldLabel}" class="company-cell" style="cursor: pointer;">${cellValue}</td>`;
                        break;
                    case 'source':
                        const sourceId = client.sourceId ? (typeof client.sourceId === 'string' ? parseInt(client.sourceId) : client.sourceId) : null;
                        cellValue = sourceId ? findNameByIdFromMap(sourceMap, sourceId) : '';
                        html += `<td data-label="${field.fieldLabel}">${cellValue}</td>`;
                        break;
                    case 'createdAt':
                        cellValue = client.createdAt || '';
                        html += `<td data-label="${field.fieldLabel}">${cellValue}</td>`;
                        break;
                    case 'updatedAt':
                        cellValue = client.updatedAt || '';
                        html += `<td data-label="${field.fieldLabel}">${cellValue}</td>`;
                        break;
                }
            } else {
                const values = fieldValuesMap.get(field.id) || [];
                
                if (values.length > 0) {
                    if (field.allowMultiple) {
                        cellValue = values.map(v => formatFieldValue(v, field)).join('<br>');
                    } else {
                        cellValue = formatFieldValue(values[0], field);
                    }
                }
                
                html += `<td data-label="${field.fieldLabel}">${cellValue}</td>`;
            }
        });

        if (!hasSourceStatic) {
            const sourceId = client.sourceId ? (typeof client.sourceId === 'string' ? parseInt(client.sourceId) : client.sourceId) : null;
            html += `<td data-label="Залучення">${sourceId ? findNameByIdFromMap(sourceMap, sourceId) : ''}</td>`;
        }
        
        row.innerHTML = html;
        tableBody.appendChild(row);
        
        const purchaseButton = row.querySelector('.purchase-button');
        if (purchaseButton) {
            purchaseButton.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                openCreatePurchaseModal(client.id);
            });
        }
        
        const containerButton = row.querySelector('.container-button');
        if (containerButton) {
            containerButton.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                openCreateContainerModal(client.id);
            });
        }
        
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
            <td class="button-td" data-label="">
                <button class="purchase-button" data-client-id="${client.id}">Закупка</button>
                <button class="container-button" data-client-id="${client.id}">Тара</button>
            </td>
            <td data-label="Компанія" data-sort="company" class="company-cell" style="cursor: pointer;">${client.company || ''}</td>
            <td data-label="Залучення">${findNameByIdFromMap(sourceMap, client.sourceId)}</td>
        `;
        tableBody.appendChild(row);
        
        const purchaseButton = row.querySelector('.purchase-button');
        if (purchaseButton) {
            purchaseButton.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                openCreatePurchaseModal(client.id);
            });
        }
        
        const containerButton = row.querySelector('.container-button');
        if (containerButton) {
            containerButton.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                openCreateContainerModal(client.id);
            });
        }
        
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
                const filteredArray = value.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                if (filteredArray.length > 0) {
                    cleanedFilters[key] = filteredArray;
                }
            } else if (typeof value === 'string' && value.trim() !== '') {
                cleanedFilters[key] = [value.trim()];
            } else if (typeof value !== 'string') {
                cleanedFilters[key] = [value];
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
        
        let lastInsertedElement = companyP;
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
            
            const canEdit = canEditClient(client);
            const editButtonHtml = canEdit ? `
                <button class="edit-icon" onclick="enableEditField(${field.id}, '${field.fieldType}', ${field.allowMultiple || false})" data-field-id="${field.id}" title="Редагувати">
                    <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
                    </svg>
                </button>
            ` : '';
            
            fieldP.innerHTML = `
                <strong>${field.fieldLabel}:</strong>
                <span id="modal-field-${field.id}" class="${!fieldValue ? 'empty-value' : ''}">${fieldValue || '—'}</span>
                ${editButtonHtml}
            `;
            fieldP.setAttribute('data-field-type', field.fieldType);

            lastInsertedElement.insertAdjacentElement('afterend', fieldP);
            lastInsertedElement = fieldP;
        });

        const sourceElement = document.getElementById('modal-client-source')?.parentElement;
        if (sourceElement) {
            sourceElement.style.display = '';
    document.getElementById('modal-client-source').innerText =
        findNameByIdFromMap(sourceMap, client.sourceId);
        }

        canEditClient(client);
        const canEditSourceField = canEditSource();
        const canEditCompanyField = canEditCompany(client);

        const companyEditButton = document.querySelector(`button.edit-icon[onclick*="enableEdit('company')"]`);
        if (companyEditButton) {
            if (!canEditCompanyField) {
                companyEditButton.style.display = 'none';
            } else {
                companyEditButton.style.display = '';
            }
        }

        const sourceEditButton = document.getElementById('edit-source');
        if (sourceEditButton) {
            if (!canEditSourceField) {
                sourceEditButton.style.display = 'none';
            } else {
                sourceEditButton.style.display = '';
            }
        }
    } else {
        const sourceElement = document.getElementById('modal-client-source')?.parentElement;
        if (sourceElement) {
            sourceElement.style.display = '';
            document.getElementById('modal-client-source').innerText =
                findNameByIdFromMap(sourceMap, client.sourceId);
        }

        canEditClient(client);
        const canEditSourceField = canEditSource();
        const canEditCompanyField = canEditCompany(client);

        const companyEditButton = document.querySelector(`button.edit-icon[onclick*="enableEdit('company')"]`);
        if (companyEditButton) {
            if (!canEditCompanyField) {
                companyEditButton.style.display = 'none';
            } else {
                companyEditButton.style.display = '';
            }
        }

        const sourceEditButton = document.getElementById('edit-source');
        if (sourceEditButton) {
            if (!canEditSourceField) {
                sourceEditButton.style.display = 'none';
            } else {
                sourceEditButton.style.display = '';
            }
        }
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
    if (fullDeleteButton) {
        const canDelete = canDeleteClient(client);

        if (fullDeleteButton.style.display !== 'none' && !canDelete) {
            fullDeleteButton.style.display = 'none';
        }
    }
    fullDeleteButton.onclick = async () => {
        if (!confirm('Ви впевнені, що хочете повністю видалити цього клієнта з бази даних? Ця дія незворотна!')) {
            return;
        }
        
        loaderBackdrop.style.display = 'flex';
        try {
            const response = await fetch(`${API_URL}/${client.id}`, {method: 'DELETE'});
            if (!response.ok) {
                const errorData = await response.json();
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                return;
            }
            showMessage('Клієнт повністю видалений з бази даних', 'info');
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
    const restoreButton = document.getElementById('restore-client');

    const canDelete = canDeleteClient(client);

    if (client.isActive === false) {
        if (deleteButton) deleteButton.style.display = 'none';
        if (restoreButton) restoreButton.style.display = 'block';
    } else {
        if (deleteButton) {
            deleteButton.style.display = canDelete ? 'block' : 'none';
        }
        if (restoreButton) restoreButton.style.display = 'none';
    }
    
    deleteButton.onclick = async () => {
        if (!confirm('Ви впевнені, що хочете деактивувати цього клієнта? Клієнт буде прихований, але залишиться в базі даних.')) {
            return;
        }
        
        loaderBackdrop.style.display = 'flex';
        try {
            const response = await fetch(`${API_URL}/active/${client.id}`, {method: 'DELETE'});
            if (!response.ok) {
                const errorData = await response.json();
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                return;
            }
            showMessage('Клієнт деактивовано (isActive = false)', 'info');
            modal.style.display = 'none';

            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        } catch (error) {
            console.error('Помилка деактивації клієнта:', error);
            handleError(error);
        } finally {
            loaderBackdrop.style.display = 'none';
        }
    };

    if (restoreButton) {
        restoreButton.onclick = async () => {
            if (!confirm('Ви впевнені, що хочете відновити цього клієнта? Клієнт знову стане активним.')) {
                return;
}

    loaderBackdrop.style.display = 'flex';
    try {
                const response = await fetch(`${API_URL}/active/${client.id}`, {method: 'PATCH'});
        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }
                showMessage('Клієнт відновлено (isActive = true)', 'info');
                modal.style.display = 'none';

        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
    } catch (error) {
                console.error('Помилка відновлення клієнта:', error);
        handleError(error);
    } finally {
        loaderBackdrop.style.display = 'none';
    }
        };
}

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
    if (!currentClientTypeId) {
        return;
    }

    const form = document.getElementById('client-form');
    form.innerHTML = '';

    const nameFieldLabel = currentClientType ? currentClientType.nameFieldLabel : 'Компанія';
    
    const nameFieldDiv = document.createElement('div');
    nameFieldDiv.className = 'form-group';
    const nameLabel = document.createElement('label');
    nameLabel.setAttribute('for', 'company');
    nameLabel.textContent = nameFieldLabel;
    const nameInput = document.createElement('input');
    nameInput.type = 'text';
    nameInput.id = 'company';
    nameInput.name = 'company';
    nameInput.required = true;
    nameInput.placeholder = nameFieldLabel;
    nameFieldDiv.appendChild(nameLabel);
    nameFieldDiv.appendChild(nameInput);
    form.appendChild(nameFieldDiv);

    if (visibleInCreateFields && visibleInCreateFields.length > 0) {
        visibleInCreateFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        
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
            input.type = 'tel';
            input.id = `field-${field.id}`;
            input.name = `field-${field.id}`;
            input.required = field.isRequired || false;
            input.placeholder = field.allowMultiple ? 'Телефони (розділяємо комою, формат: +1234567890)' : 'Телефон (формат: +1234567890)';
            input.title = field.allowMultiple 
                ? 'Номери повинні починатися з + та містити від 1 до 15 цифр (формат E.164), розділяйте комою'
                : 'Номер повинен починатися з + та містити від 1 до 15 цифр (формат E.164)';

            const validatePhoneField = function() {
                const value = this.value.trim();
                if (!value) {
                    if (this.required) {
                        this.setCustomValidity('Це поле обов\'язкове для заповнення');
                    } else {
                        this.setCustomValidity('');
                    }
                    return;
                }
                
                if (field.allowMultiple) {
                    const phones = value.split(',').map(p => p.trim()).filter(p => p);
                    if (phones.length === 0) {
                        this.setCustomValidity('Введіть хоча б один номер телефону');
                        return;
                    }
                    const normalizedPhones = phones.map(p => normalizePhoneNumber(p));
                    const invalidPhones = normalizedPhones.filter(p => !validatePhoneNumber(p));
                    if (invalidPhones.length > 0) {
                        this.setCustomValidity('Деякі номери мають некоректний формат. Використовуйте формат E.164: +1234567890');
                    } else {
                        this.setCustomValidity('');
                    }
                } else {
                    const normalized = normalizePhoneNumber(value);
                    if (!validatePhoneNumber(normalized)) {
                        this.setCustomValidity('Номер має некоректний формат. Використовуйте формат E.164: +1234567890');
                    } else {
                        this.setCustomValidity('');
                    }
                }
            };
            
            input.addEventListener('blur', validatePhoneField);
            input.addEventListener('input', function() {
                if (this.value.trim() === '') {
                    this.setCustomValidity('');
                }
            });
            
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
    }

    const sourceFieldDiv = document.createElement('div');
    sourceFieldDiv.className = 'form-group';
    
    const sourceLabel = document.createElement('label');
    sourceLabel.setAttribute('for', 'source');
    sourceLabel.textContent = 'Залучення *';
    sourceFieldDiv.appendChild(sourceLabel);
    
    const sourceSelect = document.createElement('select');
    sourceSelect.id = 'source';
    sourceSelect.name = 'sourceId';
    const defaultSourceOption = document.createElement('option');
    defaultSourceOption.value = '';
    defaultSourceOption.textContent = 'Виберіть...';
    defaultSourceOption.selected = true;
    sourceSelect.appendChild(defaultSourceOption);
    
    availableSources.forEach(source => {
        const option = document.createElement('option');
        option.value = source.id;
        option.textContent = source.name;
        sourceSelect.appendChild(option);
    });
    
    const defaultSourceId = defaultValues.source ? defaultValues.source() : '';
    if (defaultSourceId && sourceSelect.querySelector(`option[value="${defaultSourceId}"]`)) {
        sourceSelect.value = defaultSourceId;
    }
    
    sourceFieldDiv.appendChild(sourceSelect);
    form.appendChild(sourceFieldDiv);

    setTimeout(() => {
        if (typeof createCustomSelect === 'function') {
            const customSelect = createCustomSelect(sourceSelect);
            customSelects['source-custom'] = customSelect;
            const sourceData = availableSources.map(s => ({
                id: s.id,
                name: s.name
            }));
            customSelect.populate(sourceData);
            if (defaultSourceId) {
                customSelect.setValue(defaultSourceId);
            }
        }
    }, 0);

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
        .map(normalizePhoneNumber)
        .filter(phone => validatePhoneNumber(phone));

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
        const sourceSelect = document.getElementById('source');
        if (sourceSelect) {
            sourceSelect.selectedIndex = 0;
            const customSelectId = 'source-custom';
        if (customSelects[customSelectId]) {
            customSelects[customSelectId].reset();
                const defaultSourceId = defaultValues.source ? defaultValues.source() : '';
                if (defaultSourceId && sourceSelect.querySelector(`option[value="${defaultSourceId}"]`)) {
                    customSelects[customSelectId].setValue(defaultSourceId);
                }
            }
        }
    }
}

const defaultValues = {
    source: () => {
        const currentUserId = localStorage.getItem('userId');
        if (!currentUserId || !availableSources || availableSources.length === 0) {
            return '';
        }
        const userSource = availableSources.find(source => {
            const sourceUserId = source.userId !== null && source.userId !== undefined 
                ? String(source.userId) 
                : null;
            return sourceUserId === currentUserId;
        });
        return userSource ? String(userSource.id) : '';
    }
};


function validatePhoneNumber(phone) {
    if (!phone || typeof phone !== 'string') {
        return false;
    }
    const cleaned = phone.replace(/[^\d+]/g, '');

    const e164Pattern = /^\+[1-9]\d{1,14}$/;
    return e164Pattern.test(cleaned);
}

function normalizePhoneNumber(phone) {
    if (!phone || typeof phone !== 'string') {
        return phone;
    }
    let cleaned = phone.replace(/[^\d+]/g, '');
    
    if (cleaned.length === 0) {
        return phone;
    }

    let hasPlus = cleaned.startsWith('+');
    if (hasPlus) {
        cleaned = cleaned.substring(1);
    }

    cleaned = cleaned.replace(/^0+/, '');

    if (cleaned.length === 0) {
        return phone;
    }

    if (cleaned.startsWith('0')) {
        cleaned = cleaned.replace(/^0+/, '');
        if (cleaned.length === 0) {
            return phone;
        }
    }

    if (cleaned.length > 15) {
        cleaned = cleaned.substring(0, 15);
    }

    return '+' + cleaned;
}

document.getElementById('client-form').addEventListener('submit',
    async function (event) {
        event.preventDefault();

        if (!currentClientTypeId) {
            showMessage('Будь ласка, виберіть тип клієнта з навігації', 'error');
            return;
        }

        let hasValidationErrors = false;
        visibleInCreateFields.forEach(field => {
            if (field.fieldType === 'PHONE') {
                const phoneInput = document.getElementById(`field-${field.id}`);
                if (phoneInput) {

                    phoneInput.dispatchEvent(new Event('blur'));
                    if (!phoneInput.validity.valid) {
                        hasValidationErrors = true;
                        phoneInput.reportValidity();
                    }
                }
            }
        });

        if (hasValidationErrors) {
            showMessage('Будь ласка, виправте помилки в полях телефонів', 'error');
            return;
        }

        loaderBackdrop.style.display = 'flex';

        const formData = new FormData(this);
        const clientData = {
            clientTypeId: currentClientTypeId,
            company: formData.get('company'),
            fieldValues: []
        };

        const sourceId = formData.get('sourceId');
        if (sourceId) {
            clientData.sourceId = parseInt(sourceId);
        }

        visibleInCreateFields.forEach(field => {
            const fieldValue = formData.get(`field-${field.id}`);
            const fieldValueMultiple = formData.getAll(`field-${field.id}`);
            
            if (field.fieldType === 'PHONE' && field.allowMultiple) {
                const phoneValue = formData.get(`field-${field.id}`);
                if (phoneValue) {
                    const phones = phoneValue.split(',')
            .map(num => num.trim())
            .filter(num => num.length > 0)
                        .map(normalizePhoneNumber)
                        .filter(phone => validatePhoneNumber(phone));
                    
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
                    if (field.fieldType === 'PHONE') {
                        const normalizedValue = normalizePhoneNumber(fieldValue);
                        if (validatePhoneNumber(normalizedValue)) {
                            fieldValueData.valueText = normalizedValue;
                        } else {
                            return;
                        }
                    } else {
                        fieldValueData.valueText = fieldValue;
                    }
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

    if (!filterForm) return;
    const formData = new FormData(filterForm);

    const createdAtFrom = formData.get('createdAtFrom');
    const createdAtTo = formData.get('createdAtTo');
    const updatedAtFrom = formData.get('updatedAtFrom');
    const updatedAtTo = formData.get('updatedAtTo');

    if (createdAtFrom) selectedFilters['createdAtFrom'] = [createdAtFrom];
    if (createdAtTo) selectedFilters['createdAtTo'] = [createdAtTo];
    if (updatedAtFrom) selectedFilters['updatedAtFrom'] = [updatedAtFrom];
    if (updatedAtTo) selectedFilters['updatedAtTo'] = [updatedAtTo];

    const sourceSelectId = 'filter-source';
    if (customSelects[sourceSelectId]) {
        const selectedSources = customSelects[sourceSelectId].getValue();
        const filteredSources = selectedSources.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
        if (filteredSources.length > 0) {
            selectedFilters['source'] = filteredSources;
        }
    }

    const showInactive = formData.get('showInactive');
    if (showInactive === 'true') {
        selectedFilters['showInactive'] = ['true'];
    }

    if (filterableFields && filterableFields.length > 0) {
        filterableFields.forEach(field => {
            if (field.fieldType === 'DATE') {
                const fromValue = formData.get(`${field.fieldName}From`);
                const toValue = formData.get(`${field.fieldName}To`);
                if (fromValue) selectedFilters[`${field.fieldName}From`] = [fromValue];
                if (toValue) selectedFilters[`${field.fieldName}To`] = [toValue];
            } else if (field.fieldType === 'NUMBER') {
                const fromValue = formData.get(`${field.fieldName}From`);
                const toValue = formData.get(`${field.fieldName}To`);
                if (fromValue && fromValue.trim() !== '') {
                    selectedFilters[`${field.fieldName}From`] = [fromValue.trim()];
                }
                if (toValue && toValue.trim() !== '') {
                    selectedFilters[`${field.fieldName}To`] = [toValue.trim()];
                }
            } else if (field.fieldType === 'LIST') {
                const selectId = `filter-${field.fieldName}`;
                if (customSelects[selectId]) {
                    const selectedValues = customSelects[selectId].getValue();
                    const filteredValues = selectedValues.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                    if (filteredValues.length > 0) {
                        selectedFilters[field.fieldName] = filteredValues;
                    }
                }
            } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                const value = formData.get(field.fieldName);
                if (value && value.trim() !== '') {
                    selectedFilters[field.fieldName] = [value.trim()];
                }
            } else if (field.fieldType === 'BOOLEAN') {
                const value = formData.get(field.fieldName);
                if (value && value !== '' && value !== 'null') {
                    selectedFilters[field.fieldName] = [value];
                }
            }
        });
    }

    localStorage.setItem('selectedFilters', JSON.stringify(selectedFilters));
    updateFilterCounter();
}

function updateFilterCounter() {
    const counterElement = document.getElementById('filter-counter');
    const countElement = document.getElementById('filter-count');

    if (!counterElement || !countElement) return;

    let totalFilters = 0;

    Object.keys(selectedFilters).forEach(key => {
        const value = selectedFilters[key];
        if (Array.isArray(value)) {
            const validValues = value.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
            totalFilters += validValues.length;
        } else if (value !== null && value !== undefined && value !== '') {
            totalFilters += 1;
        }
    });

    if (totalFilters > 0) {
        countElement.textContent = totalFilters;
        counterElement.style.display = 'inline-flex';
    } else {
        countElement.textContent = '0';
        counterElement.style.display = 'none';
    }
}


document.getElementById('filter-counter').addEventListener('click', () => {
    clearFilters();
});

function clearFilters() {
    Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);

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

async function showClientTypeSelectionModal() {
    const modal = document.getElementById('clientTypeSelectionModal');
    const listContainer = document.getElementById('client-types-selection-list');
    
    if (!modal || !listContainer) return;
    
    try {
        const response = await fetch('/api/v1/client-type/active');
        if (!response.ok) {
            console.error('Failed to load client types');
            return;
        }
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
        
        if (accessibleClientTypes.length === 0) {
            listContainer.innerHTML = '<p style="text-align: center; color: var(--main-grey); padding: 2em;">Немає доступних типів клієнтів</p>';
            modal.style.display = 'flex';
        } else if (accessibleClientTypes.length === 1) {
            window.location.href = `/routes?type=${accessibleClientTypes[0].id}`;
            return;
        } else {
            listContainer.innerHTML = '';
            accessibleClientTypes.forEach(type => {
                const card = document.createElement('div');
                card.className = 'client-type-card';
                card.innerHTML = `
                    <div class="client-type-card-icon">👥</div>
                    <div class="client-type-card-name">${type.name}</div>
                `;
                card.addEventListener('click', () => {
                    window.location.href = `/routes?type=${type.id}`;
                });
                listContainer.appendChild(card);
            });
            modal.style.display = 'flex';
        }

        const closeBtn = document.querySelector('.close-client-type-modal');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                modal.style.display = 'none';
            });
        }
        
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.style.display = 'none';
            }
        });
    } catch (error) {
        console.error('Error loading client types:', error);
    }
}

async function updateNavigationWithCurrentType(typeId) {
    try {
        const response = await fetch(`/api/v1/client-type/${typeId}`);
        if (!response.ok) return;
        
        const clientType = await response.json();
        const navLink = document.querySelector('#nav-routes a');
        
        if (navLink && clientType.name) {
            navLink.innerHTML = `
                <span class="nav-client-type-label">Маршрути:</span>
                <span class="nav-client-type-name">${clientType.name}</span>
                <span class="dropdown-arrow">▼</span>
            `;
        }

        const dropdown = document.getElementById('route-types-dropdown');
        if (dropdown) {
            const links = dropdown.querySelectorAll('a');
            links.forEach(link => {
                link.classList.remove('active');
                if (link.href.includes(`type=${typeId}`)) {
                    link.classList.add('active');
                }
            });
        }
    } catch (error) {
        console.error('Error updating navigation:', error);
    }
}

async function loadUsers() {
    try {
        const response = await fetch('/api/v1/user');
        if (!response.ok) throw new Error('Failed to load users');
        availableUsers = await response.json();
        userMap = new Map(availableUsers.map(u => [u.id, u.name]));
    } catch (error) {
        console.error('Error loading users:', error);
        availableUsers = [];
        userMap = new Map();
    }
}

async function loadProducts() {
    try {
        const response = await fetch('/api/v1/product?usage=PURCHASE_ONLY');
        if (!response.ok) throw new Error('Failed to load products');
        availableProducts = await response.json();
        productMap = new Map(availableProducts.map(p => [p.id, p.name]));
    } catch (error) {
        console.error('Error loading products:', error);
        availableProducts = [];
        productMap = new Map();
    }
}

async function checkExchangeRatesFreshness() {
    try {
        const response = await fetch('/api/v1/exchange-rates');
        if (!response.ok) {
            return false;
        }
        const rates = await response.json();
        
        if (!rates || rates.length === 0) {
            return false;
        }
        
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        for (const rate of rates) {
            let rateDate = null;
            
            if (rate.updatedAt) {
                rateDate = new Date(rate.updatedAt);
            } else if (rate.createdAt) {
                rateDate = new Date(rate.createdAt);
            } else {
                return false;
            }
            
            rateDate.setHours(0, 0, 0, 0);
            
            if (rateDate.getTime() < today.getTime()) {
                return false;
            }
        }
        
        return true;
    } catch (error) {
        console.error('Error checking exchange rates freshness:', error);
        return false;
    }
}

async function openCreatePurchaseModal(clientId) {
    const modal = document.getElementById('createPurchaseModal');
    if (!modal) {
        return;
    }
    const form = document.getElementById('createPurchaseForm');
    const clientIdInput = document.getElementById('purchaseClientId');
    const sourceIdInput = document.getElementById('purchaseSourceId');
    const userIdSelect = document.getElementById('purchaseUserId');
    const productIdSelect = document.getElementById('purchaseProductId');
    const currencySelect = document.getElementById('purchaseCurrency');
    const exchangeRateLabel = document.getElementById('exchangeRateLabel');
    const exchangeRateInput = document.getElementById('purchaseExchangeRate');
    const exchangeRateWarning = document.getElementById('exchange-rate-warning');
    
    if (!form || !clientIdInput || !sourceIdInput || !userIdSelect || !productIdSelect || !currencySelect) {
        return;
    }
    
    form.reset();
    clientIdInput.value = clientId;
    
    const ratesAreFresh = await checkExchangeRatesFreshness();
    if (exchangeRateWarning) {
        exchangeRateWarning.style.display = ratesAreFresh ? 'none' : 'block';
    }
    
    try {
        const clientResponse = await fetch(`/api/v1/client/${clientId}`);
        if (!clientResponse.ok) throw new Error('Failed to load client');
        const clientData = await clientResponse.json();
        sourceIdInput.value = clientData.sourceId || '';
    } catch (error) {
        console.error('Error loading client:', error);
        sourceIdInput.value = '';
    }
    
    await Promise.all([loadUsers(), loadProducts()]);
    
    userIdSelect.innerHTML = '';
    const currentUserId = userId ? Number(userId) : null;
    availableUsers.forEach(user => {
        const option = document.createElement('option');
        option.value = user.id;
        option.textContent = user.name;
        if (currentUserId && Number(user.id) === currentUserId) {
            option.selected = true;
        }
        userIdSelect.appendChild(option);
    });
    
    productIdSelect.innerHTML = '<option value="">Виберіть товар</option>';
    availableProducts.forEach(product => {
        const option = document.createElement('option');
        option.value = product.id;
        option.textContent = product.name;
        productIdSelect.appendChild(option);
    });
    
    currencySelect.value = 'UAH';
    exchangeRateLabel.style.display = 'none';
    exchangeRateInput.value = '';
    
    modal.style.display = 'flex';
}

document.addEventListener('DOMContentLoaded', () => {
    const createPurchaseModal = document.getElementById('createPurchaseModal');
    const closeCreatePurchaseModal = document.getElementById('closeCreatePurchaseModal');
    const cancelCreatePurchase = document.getElementById('cancelCreatePurchase');
    const createPurchaseForm = document.getElementById('createPurchaseForm');
    const currencySelect = document.getElementById('purchaseCurrency');
    const exchangeRateLabel = document.getElementById('exchangeRateLabel');
    const exchangeRateInput = document.getElementById('purchaseExchangeRate');
    
    if (currencySelect && exchangeRateLabel && exchangeRateInput) {
        currencySelect.addEventListener('change', function() {
            if (this.value === 'USD' || this.value === 'EUR') {
                exchangeRateLabel.style.display = 'flex';
            } else {
                exchangeRateLabel.style.display = 'none';
                exchangeRateInput.value = '';
            }
        });
    }
    
    if (closeCreatePurchaseModal) {
        closeCreatePurchaseModal.addEventListener('click', () => {
            createPurchaseModal.style.display = 'none';
        });
    }
    
    if (cancelCreatePurchase) {
        cancelCreatePurchase.addEventListener('click', () => {
            createPurchaseModal.style.display = 'none';
        });
    }
    
    if (createPurchaseForm) {
        createPurchaseForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const sourceIdValue = document.getElementById('purchaseSourceId').value;
            const formData = {
                userId: Number(document.getElementById('purchaseUserId').value),
                clientId: Number(document.getElementById('purchaseClientId').value),
                sourceId: sourceIdValue && sourceIdValue !== '' ? Number(sourceIdValue) : null,
                productId: Number(document.getElementById('purchaseProductId').value),
                quantity: parseFloat(document.getElementById('purchaseQuantity').value),
                totalPrice: parseFloat(document.getElementById('purchaseTotalPrice').value),
                paymentMethod: document.getElementById('purchasePaymentMethod').value,
                currency: document.getElementById('purchaseCurrency').value,
                exchangeRate: document.getElementById('purchaseExchangeRate').value ? parseFloat(document.getElementById('purchaseExchangeRate').value) : null,
                comment: document.getElementById('purchaseComment').value || null
            };
            
            try {
                loaderBackdrop.style.display = 'flex';
                const response = await fetch('/api/v1/purchase', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(formData)
                });
                
                if (!response.ok) {
                    const error = await response.json();
                    throw new Error(error.message || 'Помилка створення закупівлі');
                }
                
                createPurchaseModal.style.display = 'none';
                createPurchaseForm.reset();
                showMessage('Закупівлю успішно створено', 'info');
            } catch (error) {
                console.error('Error creating purchase:', error);
                showMessage('Помилка створення закупівлі: ' + error.message, 'error');
            } finally {
                loaderBackdrop.style.display = 'none';
            }
        });
    }
    
    const createContainerModal = document.getElementById('createContainerModal');
    const closeCreateContainerModal = document.getElementById('closeCreateContainerModal');
    const cancelCreateContainer = document.getElementById('cancelCreateContainer');
    const createContainerForm = document.getElementById('createContainerForm');
    
    if (closeCreateContainerModal) {
        closeCreateContainerModal.addEventListener('click', () => {
            createContainerModal.style.display = 'none';
        });
    }
    
    if (cancelCreateContainer) {
        cancelCreateContainer.addEventListener('click', () => {
            createContainerModal.style.display = 'none';
        });
    }
    
    if (createContainerForm) {
        createContainerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const operationType = document.getElementById('containerOperationType').value;
            const clientId = Number(document.getElementById('containerClientId').value);
            const containerId = Number(document.getElementById('containerContainerId').value);
            const quantity = parseFloat(document.getElementById('containerQuantity').value);
            
            if (!operationType || !clientId || !containerId || !quantity) {
                showMessage('Будь ласка, заповніть всі поля', 'error');
                return;
            }
            
            const formData = {
                clientId: clientId,
                containerId: containerId,
                quantity: quantity
            };
            
            const endpoint = operationType === 'transfer' 
                ? '/api/v1/containers/client/transfer'
                : '/api/v1/containers/client/collect';
            
            try {
                loaderBackdrop.style.display = 'flex';
                const response = await fetch(endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(formData)
                });
                
                if (!response.ok) {
                    const error = await response.json();
                    throw new Error(error.message || 'Помилка виконання транзакції тари');
                }
                
                createContainerModal.style.display = 'none';
                createContainerForm.reset();
                const operationText = operationType === 'transfer' 
                    ? 'Тара успішно залишена у клієнта'
                    : 'Тара успішно забрана у клієнта';
                showMessage(operationText, 'info');
            } catch (error) {
                console.error('Error executing container transaction:', error);
                showMessage('Помилка виконання транзакції тари: ' + error.message, 'error');
            } finally {
                loaderBackdrop.style.display = 'none';
            }
        });
    }
    
    window.addEventListener('click', (e) => {
        const purchaseModal = document.getElementById('createPurchaseModal');
        const containerModal = document.getElementById('createContainerModal');
        if (e.target === purchaseModal) {
            purchaseModal.style.display = 'none';
        }
        if (e.target === containerModal) {
            containerModal.style.display = 'none';
        }
    });
});

async function loadContainers() {
    try {
        const response = await fetch('/api/v1/container');
        if (!response.ok) throw new Error('Failed to load containers');
        availableContainers = await response.json();
        containerMap = new Map(availableContainers.map(c => [c.id, c.name]));
    } catch (error) {
        console.error('Error loading containers:', error);
        availableContainers = [];
        containerMap = new Map();
    }
}

async function openCreateContainerModal(clientId) {
    const modal = document.getElementById('createContainerModal');
    if (!modal) {
        return;
    }
    const form = document.getElementById('createContainerForm');
    const clientIdInput = document.getElementById('containerClientId');
    const operationTypeSelect = document.getElementById('containerOperationType');
    const containerIdSelect = document.getElementById('containerContainerId');
    
    if (!form || !clientIdInput || !operationTypeSelect || !containerIdSelect) {
        return;
    }
    
    form.reset();
    clientIdInput.value = clientId;
    
    await loadContainers();
    
    operationTypeSelect.value = '';
    
    containerIdSelect.innerHTML = '<option value="">Виберіть тип тари</option>';
    availableContainers.forEach(container => {
        const option = document.createElement('option');
        option.value = container.id;
        option.textContent = container.name;
        containerIdSelect.appendChild(option);
    });
    
    modal.style.display = 'flex';
}




