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

const userId = getUserId();
const selectedFilters = {};

function getUserAuthorities() {
    return getAuthorities();
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
    
    const currentUserId = getUserId() ? Number(getUserId()) : null;
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
const searchInput = document.getElementById('inputSearch');
const clientModal = document.getElementById('client-modal');
const closeModalClientBtn = document.getElementById('close-modal-client');
const modalClientId = document.getElementById('modal-client-id');
const modalClientCompany = document.getElementById('modal-client-company');
const modalClientSource = document.getElementById('modal-client-source');
const modalClientCreated = document.getElementById('modal-client-created');
const modalClientUpdated = document.getElementById('modal-client-updated');
const fullDeleteButton = document.getElementById('full-delete-client');
const deleteButton = document.getElementById('delete-client');
const restoreButton = document.getElementById('restore-client');
const filterCounter = document.getElementById('filter-counter');
const filterCount = document.getElementById('filter-count');
const modalFilterButtonSubmit = document.getElementById('modal-filter-button-submit');
const createClientModal = document.getElementById('createClientModal');
const openModalBtn = document.getElementById('open-modal');
const createClientCloseBtn = document.getElementsByClassName('create-client-close')[0];
const clientTypeSelectionModal = document.getElementById('clientTypeSelectionModal');
const clientTypesSelectionList = document.getElementById('client-types-selection-list');

let modalCloseHandler = null;
let modalClickHandler = null;
let modalTimeoutId = null;
let filterModalTimeoutId = null;
let columnResizerTimeoutId = null;
let customSelectTimeoutIds = [];
let editing = false;
let createModalTimeoutIds = [];
let createModalClickHandler = null;

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
            parsedFilters = normalizeFilterKeys(parsedFilters);
        } catch (e) {
            console.error('Invalid selectedFilters in localStorage:', e);
            parsedFilters = {};
        }
    } else {
        parsedFilters = {};
    }
    Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);
    Object.assign(selectedFilters, parsedFilters);

    const savedSearchTerm = localStorage.getItem('searchTerm');
    if (savedSearchTerm && searchInput) {
        searchInput.value = savedSearchTerm;
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
            Object.keys(selectedFilters).forEach(key => {
                if (staticFilterKeys.includes(key) || key.endsWith('From') || key.endsWith('To')) {
                    cleanedFilters[key] = selectedFilters[key];
                }
            });
            Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);
            Object.assign(selectedFilters, cleanedFilters);
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
        
        const cleanedFilters = normalizeFilterKeys(selectedFilters, staticFilterKeys, validFieldNames);
        Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);
        Object.assign(selectedFilters, cleanedFilters);
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
                    if (fromInput && selectedFilters[`${field.fieldName}From`]) {
                        fromInput.value = selectedFilters[`${field.fieldName}From`][0] || '';
                    }
                    if (toInput && selectedFilters[`${field.fieldName}To`]) {
                        toInput.value = selectedFilters[`${field.fieldName}To`][0] || '';
                    }
                } else if (field.fieldType === 'NUMBER') {
                    const fromInput = document.getElementById(`${filterId}-from`);
                    const toInput = document.getElementById(`${filterId}-to`);
                    if (fromInput && selectedFilters[`${field.fieldName}From`]) {
                        fromInput.value = selectedFilters[`${field.fieldName}From`][0] || '';
                    }
                    if (toInput && selectedFilters[`${field.fieldName}To`]) {
                        toInput.value = selectedFilters[`${field.fieldName}To`][0] || '';
                    }
                } else if (field.fieldType === 'LIST') {
                    if (customSelects[filterId] && selectedFilters[field.fieldName]) {
                        const savedValues = selectedFilters[field.fieldName];
                        if (Array.isArray(savedValues) && savedValues.length > 0) {
                            const validValues = savedValues.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                            if (validValues.length > 0) {
                                customSelects[filterId].setValue(validValues);
                            }
                        }
                    }
                } else if (field.fieldType === 'BOOLEAN') {
                    const select = document.getElementById(filterId);
                    if (select && selectedFilters[field.fieldName] && selectedFilters[field.fieldName].length > 0) {
                        const savedValue = selectedFilters[field.fieldName][0];
                        if (savedValue && savedValue !== '' && savedValue !== 'null') {
                            select.value = savedValue;
                        }
                    }
                } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                    const input = document.getElementById(filterId);
                    if (input && selectedFilters[field.fieldName]) {
                        input.value = Array.isArray(selectedFilters[field.fieldName]) 
                            ? selectedFilters[field.fieldName][0] 
                            : selectedFilters[field.fieldName];
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
            if (selectedFilters['createdAtFrom']) {
                const fromInput = filterForm.querySelector('#createdAtFrom');
                if (fromInput) fromInput.value = selectedFilters['createdAtFrom'][0];
            }
            if (selectedFilters['createdAtTo']) {
                const toInput = filterForm.querySelector('#createdAtTo');
                if (toInput) toInput.value = selectedFilters['createdAtTo'][0];
            }
            if (selectedFilters['updatedAtFrom']) {
                const fromInput = filterForm.querySelector('#updatedAtFrom');
                if (fromInput) fromInput.value = selectedFilters['updatedAtFrom'][0];
            }
            if (selectedFilters['updatedAtTo']) {
                const toInput = filterForm.querySelector('#updatedAtTo');
                if (toInput) toInput.value = selectedFilters['updatedAtTo'][0];
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

            if (selectedFilters['source'] && customSelects['filter-source']) {
                const savedSources = selectedFilters['source'];
                if (Array.isArray(savedSources) && savedSources.length > 0) {
                    const validSources = savedSources.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                    if (validSources.length > 0) {
                        customSelects['filter-source'].setValue(validSources);
                    }
                }
            }

            const showInactiveCheckbox = filterForm.querySelector('#filter-show-inactive');
            if (showInactiveCheckbox && selectedFilters['showInactive'] && selectedFilters['showInactive'][0] === 'true') {
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
        const response = await fetch(`/api/v1/client-type/${typeId}/fields/all`);
        if (!response.ok) throw new Error('Failed to load fields');
        
        const data = await response.json();
        
        clientTypeFields = data.all || [];
        window.clientTypeFields = clientTypeFields;
        visibleFields = data.visible || [];
        window.visibleFields = visibleFields;
        filterableFields = data.filterable || [];
        visibleInCreateFields = data.visibleInCreate || [];
    } catch (error) {
        console.error('Error loading fields:', error);
    }
}

async function loadDefaultClientData() {
    try {
        const sourcesRes = await fetch('/api/v1/source');
        if (!sourcesRes.ok) {
            console.error('Failed to load sources:', sourcesRes.status, sourcesRes.statusText);
            return;
        }
        availableSources = await sourcesRes.json();
        sourceMap = new Map(availableSources.map(s => [s.id, s.name]));
    } catch (error) {
        console.error('Error loading default data:', error);
        handleError(error);
    }
}

function buildDynamicTable() {
    if (!currentClientType) return;
    
    const thead = document.querySelector('#client-list table thead tr');
    if (!thead) return;
    
    thead.textContent = '';

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
            if (field.staticFieldName === 'company' || field.staticFieldName === 'source' || field.staticFieldName === 'createdAt' || field.staticFieldName === 'updatedAt') {
                th.setAttribute('data-sort', field.staticFieldName);
                th.style.cursor = 'pointer';
            }
        } else if (field.fieldName === 'company' || field.fieldName === 'source') {
            th.setAttribute('data-sort', field.fieldName);
            th.style.cursor = 'pointer';
        }
        
        if (field.columnWidth && window.innerWidth > 1024) {
            th.style.width = field.columnWidth + 'px';
            th.style.minWidth = field.columnWidth + 'px';
            th.style.maxWidth = field.columnWidth + 'px';
        }
        th.style.flexShrink = '0';
        th.style.flexGrow = '0';
        thead.appendChild(th);
    });

    if (typeof initColumnResizer === 'function' && currentClientTypeId) {
        if (columnResizerTimeoutId !== null) {
            clearTimeout(columnResizerTimeoutId);
        }
        columnResizerTimeoutId = setTimeout(() => {
            initColumnResizer(currentClientTypeId);
            if (typeof applyColumnWidths === 'function') {
                applyColumnWidths(currentClientTypeId);
            }
            columnResizerTimeoutId = null;
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
        customSelectTimeoutIds.forEach(id => clearTimeout(id));
        customSelectTimeoutIds = [];
        
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
                sel.textContent = '';
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
        
        const createdAtFromLabel = document.createElement('label');
        createdAtFromLabel.className = 'from-to-style';
        createdAtFromLabel.setAttribute('for', 'filter-createdAt-from');
        createdAtFromLabel.textContent = 'Від:';
        createdAtBlock.appendChild(createdAtFromLabel);
        
        const createdAtFromInput = document.createElement('input');
        createdAtFromInput.type = 'date';
        createdAtFromInput.id = 'filter-createdAt-from';
        createdAtFromInput.name = 'createdAtFrom';
        createdAtBlock.appendChild(createdAtFromInput);
        
        const createdAtToLabel = document.createElement('label');
        createdAtToLabel.className = 'from-to-style';
        createdAtToLabel.setAttribute('for', 'filter-createdAt-to');
        createdAtToLabel.textContent = 'До:';
        createdAtBlock.appendChild(createdAtToLabel);
        
        const createdAtToInput = document.createElement('input');
        createdAtToInput.type = 'date';
        createdAtToInput.id = 'filter-createdAt-to';
        createdAtToInput.name = 'createdAtTo';
        createdAtBlock.appendChild(createdAtToInput);
        
        filterForm.appendChild(createdAtBlock);
        
        const updatedAtH2 = document.createElement('h2');
        updatedAtH2.textContent = 'Дата оновлення:';
        filterForm.appendChild(updatedAtH2);
        
        const updatedAtBlock = document.createElement('div');
        updatedAtBlock.className = 'filter-block';
        
        const updatedAtFromLabel = document.createElement('label');
        updatedAtFromLabel.className = 'from-to-style';
        updatedAtFromLabel.setAttribute('for', 'filter-updatedAt-from');
        updatedAtFromLabel.textContent = 'Від:';
        updatedAtBlock.appendChild(updatedAtFromLabel);
        
        const updatedAtFromInput = document.createElement('input');
        updatedAtFromInput.type = 'date';
        updatedAtFromInput.id = 'filter-updatedAt-from';
        updatedAtFromInput.name = 'updatedAtFrom';
        updatedAtBlock.appendChild(updatedAtFromInput);
        
        const updatedAtToLabel = document.createElement('label');
        updatedAtToLabel.className = 'from-to-style';
        updatedAtToLabel.setAttribute('for', 'filter-updatedAt-to');
        updatedAtToLabel.textContent = 'До:';
        updatedAtBlock.appendChild(updatedAtToLabel);
        
        const updatedAtToInput = document.createElement('input');
        updatedAtToInput.type = 'date';
        updatedAtToInput.id = 'filter-updatedAt-to';
        updatedAtToInput.name = 'updatedAtTo';
        updatedAtBlock.appendChild(updatedAtToInput);
        
        filterForm.appendChild(updatedAtBlock);

        const sourceSelectItem = document.createElement('div');
        sourceSelectItem.className = 'select-section-item';
        
        sourceSelectItem.appendChild(document.createElement('br'));
        
        const sourceLabel = document.createElement('label');
        sourceLabel.className = 'select-label-style';
        sourceLabel.setAttribute('for', 'filter-source');
        sourceLabel.textContent = 'Залучення:';
        sourceSelectItem.appendChild(sourceLabel);
        
        const sourceSelect = document.createElement('select');
        sourceSelect.id = 'filter-source';
        sourceSelect.name = 'source';
        sourceSelect.multiple = true;
        sourceSelectItem.appendChild(sourceSelect);
        
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
                    
                    const fromLabel = document.createElement('label');
                    fromLabel.className = 'from-to-style';
                    fromLabel.setAttribute('for', `filter-${field.fieldName}-from`);
                    fromLabel.textContent = 'Від:';
                    filterBlock.appendChild(fromLabel);
                    
                    const fromInput = document.createElement('input');
                    fromInput.type = 'date';
                    fromInput.id = `filter-${field.fieldName}-from`;
                    fromInput.name = `${field.fieldName}From`;
                    filterBlock.appendChild(fromInput);
                    
                    const toLabel = document.createElement('label');
                    toLabel.className = 'from-to-style';
                    toLabel.setAttribute('for', `filter-${field.fieldName}-to`);
                    toLabel.textContent = 'До:';
                    filterBlock.appendChild(toLabel);
                    
                    const toInput = document.createElement('input');
                    toInput.type = 'date';
                    toInput.id = `filter-${field.fieldName}-to`;
                    toInput.name = `${field.fieldName}To`;
                    filterBlock.appendChild(toInput);
                    
                    filterForm.appendChild(filterBlock);
                } else if (field.fieldType === 'NUMBER') {
                    const h2 = document.createElement('h2');
                    h2.textContent = field.fieldLabel + ':';
                    filterForm.appendChild(h2);
                    
                    const filterBlock = document.createElement('div');
                    filterBlock.className = 'filter-block';
                    
                    const fromLabel = document.createElement('label');
                    fromLabel.className = 'from-to-style';
                    fromLabel.setAttribute('for', `filter-${field.fieldName}-from`);
                    fromLabel.textContent = 'Від:';
                    filterBlock.appendChild(fromLabel);
                    
                    const fromInput = document.createElement('input');
                    fromInput.type = 'number';
                    fromInput.id = `filter-${field.fieldName}-from`;
                    fromInput.name = `${field.fieldName}From`;
                    fromInput.step = 'any';
                    fromInput.placeholder = 'Мінімум';
                    filterBlock.appendChild(fromInput);
                    
                    const toLabel = document.createElement('label');
                    toLabel.className = 'from-to-style';
                    toLabel.setAttribute('for', `filter-${field.fieldName}-to`);
                    toLabel.textContent = 'До:';
                    filterBlock.appendChild(toLabel);
                    
                    const toInput = document.createElement('input');
                    toInput.type = 'number';
                    toInput.id = `filter-${field.fieldName}-to`;
                    toInput.name = `${field.fieldName}To`;
                    toInput.step = 'any';
                    toInput.placeholder = 'Максимум';
                    filterBlock.appendChild(toInput);
                    
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
                    
                    selectItem.appendChild(document.createElement('br'));
                    
                    const label = document.createElement('label');
                    label.className = 'select-label-style';
                    label.setAttribute('for', `filter-${field.fieldName}`);
                    label.textContent = field.fieldLabel + ':';
                    selectItem.appendChild(label);
                    
                    const select = document.createElement('select');
                    select.id = `filter-${field.fieldName}`;
                    select.name = field.fieldName;
                    select.multiple = true;
                    selectItem.appendChild(select);
                    
                    filterForm.appendChild(selectItem);

                    if (field.listValues && field.listValues.length > 0) {
                        field.listValues.forEach(listValue => {
                            const option = document.createElement('option');
                            option.value = listValue.id;
                            option.textContent = listValue.value;
                            select.appendChild(option);
                        });
                    }

                    const timeoutId = setTimeout(() => {
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
                    customSelectTimeoutIds.push(timeoutId);
                } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                    const selectItem = document.createElement('div');
                    selectItem.className = 'select-section-item';
                    
                    selectItem.appendChild(document.createElement('br'));
                    
                    const label = document.createElement('label');
                    label.className = 'select-label-style';
                    label.setAttribute('for', `filter-${field.fieldName}`);
                    label.textContent = field.fieldLabel + ':';
                    selectItem.appendChild(label);
                    
                    const input = document.createElement('input');
                    input.type = 'text';
                    input.id = `filter-${field.fieldName}`;
                    input.name = field.fieldName;
                    input.placeholder = 'Пошук...';
                    selectItem.appendChild(input);
                    
                    filterForm.appendChild(selectItem);
                } else if (field.fieldType === 'BOOLEAN') {
                    const selectItem = document.createElement('div');
                    selectItem.className = 'select-section-item';
                    
                    selectItem.appendChild(document.createElement('br'));
                    
                    const label = document.createElement('label');
                    label.className = 'select-label-style';
                    label.setAttribute('for', `filter-${field.fieldName}`);
                    label.textContent = field.fieldLabel + ':';
                    selectItem.appendChild(label);
                    
                    const select = document.createElement('select');
                    select.id = `filter-${field.fieldName}`;
                    select.name = field.fieldName;
                    
                    const allOption = document.createElement('option');
                    allOption.value = '';
                    allOption.textContent = 'Всі';
                    select.appendChild(allOption);
                    
                    const yesOption = document.createElement('option');
                    yesOption.value = 'true';
                    yesOption.textContent = 'Так';
                    select.appendChild(yesOption);
                    
                    const noOption = document.createElement('option');
                    noOption.value = 'false';
                    noOption.textContent = 'Ні';
                    select.appendChild(noOption);
                    
                    selectItem.appendChild(select);
                    
                    filterForm.appendChild(selectItem);
                }
            });
        }

        const isActiveBlock = document.createElement('div');
        isActiveBlock.className = 'filter-block';
        
        const label = document.createElement('label');
        label.style.display = 'flex';
        label.style.alignItems = 'center';
        label.style.gap = '0.5em';
        label.style.margin = '0.5em 0';
        
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.id = 'filter-show-inactive';
        checkbox.name = 'showInactive';
        checkbox.value = 'true';
        label.appendChild(checkbox);
        
        const span = document.createElement('span');
        span.textContent = 'Показати неактивних клієнтів';
        label.appendChild(span);
        
        isActiveBlock.appendChild(label);
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

function escapeHtml(text) {
    if (text == null) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function createEmptyCellSpan() {
    const emptySpan = document.createElement('span');
    emptySpan.style.color = '#999';
    emptySpan.style.fontStyle = 'italic';
    emptySpan.textContent = '—';
    return emptySpan;
}

function createCompanyCell(client, nameFieldLabel) {
    const companyCell = document.createElement('td');
    companyCell.setAttribute('data-label', nameFieldLabel);
    companyCell.className = 'company-cell';
    companyCell.style.cursor = 'pointer';
    if (client.company) {
        companyCell.textContent = client.company;
    } else {
        companyCell.appendChild(createEmptyCellSpan());
    }
    return companyCell;
}

function createSourceCell(client) {
    const sourceCell = document.createElement('td');
    sourceCell.setAttribute('data-label', 'Залучення');
    const sourceId = client.sourceId ? (typeof client.sourceId === 'string' ? parseInt(client.sourceId) : client.sourceId) : null;
    const sourceName = sourceId ? findNameByIdFromMap(sourceMap, sourceId) : '';
    if (sourceName) {
        sourceCell.textContent = sourceName;
    } else {
        sourceCell.appendChild(createEmptyCellSpan());
    }
    return sourceCell;
}

function attachCompanyCellClickHandler(companyCell, client) {
    if (companyCell) {
        companyCell.addEventListener('click', () => {
            loadClientDetails(client);
        });
    }
}

function normalizeFilterKeys(filters, staticFilterKeys = [], validFieldNames = new Set()) {
    const normalizedFilters = {};
    Object.keys(filters).forEach(key => {
        const normalizedKey = key.toLowerCase();
        const normalizedStaticKeys = staticFilterKeys.map(k => k.toLowerCase());
        
        if (normalizedStaticKeys.includes(normalizedKey) || 
            normalizedKey === 'source' ||
            normalizedKey.endsWith('from') || 
            normalizedKey.endsWith('to') ||
            normalizedKey === 'createdatfrom' || 
            normalizedKey === 'createdatto' ||
            normalizedKey === 'updatedatfrom' || 
            normalizedKey === 'updatedatto') {
            const value = filters[key];
            if (value !== null && value !== undefined && value !== '' && 
                !(Array.isArray(value) && value.length === 0)) {
                normalizedFilters[normalizedKey] = value;
            }
        } else if (validFieldNames.size === 0 || validFieldNames.has(key)) {
            const value = filters[key];
            if (value !== null && value !== undefined && value !== '' && 
                !(Array.isArray(value) && (value.length === 0 || (value.length === 1 && (value[0] === '' || value[0] === 'null'))))) {
                normalizedFilters[key] = value;
            }
        }
    });
    return normalizedFilters;
}

async function renderClients(clients) {
    tableBody.textContent = '';
    
    if (currentClientTypeId && visibleFields && visibleFields.length > 0) {
        await renderClientsWithDynamicFields(clients);
    } else {
        renderClientsWithDefaultFields(clients);
    }
    
    setupSortHandlers();

    if (typeof applyColumnWidths === 'function' && currentClientTypeId) {
        if (columnResizerTimeoutId !== null) {
            clearTimeout(columnResizerTimeoutId);
        }
        columnResizerTimeoutId = setTimeout(() => {
            applyColumnWidths(currentClientTypeId);
            columnResizerTimeoutId = null;
        }, 0);
    }
}

async function renderClientsWithDynamicFields(clients) {
    const staticFields = visibleFields.filter(f => f.isStatic);
    const dynamicFields = visibleFields.filter(f => !f.isStatic);

    const hasCompanyStatic = staticFields.some(f => f.staticFieldName === 'company');
    const hasSourceStatic = staticFields.some(f => f.staticFieldName === 'source');

    const allFields = [...staticFields, ...dynamicFields].sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
    
    clients.forEach((client) => {
        const row = document.createElement('tr');
        row.classList.add('client-row');

        if (!hasCompanyStatic) {
            const nameFieldLabel = currentClientType ? currentClientType.nameFieldLabel : 'Компанія';
            const companyCell = createCompanyCell(client, nameFieldLabel);
            row.appendChild(companyCell);
        }
        
        const fieldValues = client.fieldValues || [];
        const fieldValuesMap = new Map();
        fieldValues.forEach(fv => {
            if (!fieldValuesMap.has(fv.fieldId)) {
                fieldValuesMap.set(fv.fieldId, []);
            }
            fieldValuesMap.get(fv.fieldId).push(fv);
        });
        
        client._fieldValues = fieldValues;

        allFields.forEach(field => {
            const cell = document.createElement('td');
            cell.setAttribute('data-label', field.fieldLabel);
            
            if (field.isStatic) {
                switch (field.staticFieldName) {
                    case 'company':
                        cell.className = 'company-cell';
                        cell.style.cursor = 'pointer';
                        if (client.company) {
                            cell.textContent = client.company;
                        } else {
                            cell.appendChild(createEmptyCellSpan());
                        }
                        break;
                    case 'source': {
                        const sourceId = client.sourceId ? (typeof client.sourceId === 'string' ? parseInt(client.sourceId) : client.sourceId) : null;
                        const sourceName = sourceId ? findNameByIdFromMap(sourceMap, sourceId) : '';
                        if (sourceName) {
                            cell.textContent = sourceName;
                        } else {
                            cell.appendChild(createEmptyCellSpan());
                        }
                        break;
                    }
                    case 'createdAt':
                        cell.setAttribute('data-sort', 'createdAt');
                        cell.style.cursor = 'pointer';
                        if (client.createdAt) {
                            cell.textContent = client.createdAt;
                        } else {
                            cell.appendChild(createEmptyCellSpan());
                        }
                        break;
                    case 'updatedAt':
                        cell.setAttribute('data-sort', 'updatedAt');
                        cell.style.cursor = 'pointer';
                        if (client.updatedAt) {
                            cell.textContent = client.updatedAt;
                        } else {
                            cell.appendChild(createEmptyCellSpan());
                        }
                        break;
                }
            } else {
                const values = fieldValuesMap.get(field.id) || [];
                
                if (values.length > 0) {
                    if (field.allowMultiple) {
                        values.forEach((v, index) => {
                            if (index > 0) {
                                cell.appendChild(document.createElement('br'));
                            }
                            const value = formatFieldValue(v, field);
                            if (value) {
                                cell.appendChild(document.createTextNode(value));
                            }
                        });
                    } else {
                        const value = formatFieldValue(values[0], field);
                        if (value) {
                            cell.textContent = value;
                        } else {
                            cell.appendChild(createEmptyCellSpan());
                        }
                    }
                } else {
                    cell.appendChild(createEmptyCellSpan());
                }
            }
            
            row.appendChild(cell);
        });

        if (!hasSourceStatic) {
            const sourceCell = createSourceCell(client);
            row.appendChild(sourceCell);
        }
        
        tableBody.appendChild(row);
        
        const companyCell = row.querySelector('.company-cell');
        attachCompanyCellClickHandler(companyCell, client);
    });
}

function renderClientsWithDefaultFields(clients) {
    clients.forEach(client => {
        const row = document.createElement('tr');
        row.classList.add('client-row');

        const companyCell = createCompanyCell(client, 'Компанія');
        companyCell.setAttribute('data-sort', 'company');
        row.appendChild(companyCell);

        const sourceCell = createSourceCell(client);
        row.appendChild(sourceCell);

        tableBody.appendChild(row);
        
        attachCompanyCellClickHandler(companyCell, client);
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

function updateSortIndicators() {
    document.querySelectorAll('th[data-sort]').forEach(th => {
        const sortField = th.getAttribute('data-sort');
        th.classList.remove('sort-asc', 'sort-desc');
        
        if (currentSort === sortField) {
            if (currentDirection === 'ASC') {
                th.classList.add('sort-asc');
            } else {
                th.classList.add('sort-desc');
            }
        }
    });
}

function getDefaultSortDirection(sortField) {
    if (sortField === 'updatedAt' || sortField === 'createdAt') {
        return 'DESC';
    }
    return 'ASC';
}

function setupSortHandlers() {
    document.querySelectorAll('th[data-sort]').forEach(th => {
        th.removeEventListener('click', handleSortClick);
        th.addEventListener('click', handleSortClick);
    });
    updateSortIndicators();
}

function handleSortClick(event) {
    const th = event.currentTarget;
    const sortField = th.getAttribute('data-sort');
    
    if (!sortField || (sortField !== 'company' && sortField !== 'source' && sortField !== 'createdAt' && sortField !== 'updatedAt')) {
        return;
    }
    
    if (currentSort === sortField) {
        currentDirection = currentDirection === 'ASC' ? 'DESC' : 'ASC';
    } else {
        currentSort = sortField;
        currentDirection = getDefaultSortDirection(sortField);
    }
    
    currentPage = 0;
    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
}

async function loadDataWithSort(page, size, sort, direction) {
    loaderBackdrop.style.display = 'flex';
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
        setupSortHandlers();

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

function closeModal() {
    if (modalTimeoutId !== null) {
        clearTimeout(modalTimeoutId);
        modalTimeoutId = null;
    }
    
    if (modalCloseHandler) {
        closeModalClientBtn.removeEventListener('click', modalCloseHandler);
        modalCloseHandler = null;
    }
    
    if (modalClickHandler) {
        window.removeEventListener('click', modalClickHandler);
        modalClickHandler = null;
    }
    
    clientModal.classList.remove('open');
    clientModal.style.display = 'none';
    editing = false;
}

async function showClientModal(client) {
    if (modalCloseHandler) {
        closeModalClientBtn.removeEventListener('click', modalCloseHandler);
        modalCloseHandler = null;
    }
    
    if (modalClickHandler) {
        window.removeEventListener('click', modalClickHandler);
        modalClickHandler = null;
    }
    
    if (modalTimeoutId !== null) {
        clearTimeout(modalTimeoutId);
        modalTimeoutId = null;
    }

    clientModal.setAttribute('data-client-id', client.id);

    modalClientId.textContent = client.id;
    
    const modalContent = document.querySelector('.modal-content-client');
    const existingFields = modalContent.querySelectorAll('p[data-field-id]');
    existingFields.forEach(el => el.remove());
    
    const nameFieldLabel = currentClientType ? currentClientType.nameFieldLabel : 'Компанія';
    modalClientCompany.parentElement.querySelector('strong').textContent = nameFieldLabel + ':';
    modalClientCompany.textContent = client.company;
    
    if (currentClientTypeId && clientTypeFields.length > 0) {
        let fieldValues = client._fieldValues || client.fieldValues;
        if (!fieldValues || fieldValues.length === 0) {
            fieldValues = await loadClientFieldValues(client.id);
        }
        const fieldValuesMap = new Map();
        fieldValues.forEach(fv => {
            if (!fieldValuesMap.has(fv.fieldId)) {
                fieldValuesMap.set(fv.fieldId, []);
            }
            fieldValuesMap.get(fv.fieldId).push(fv);
        });
        
        const companyP = modalClientCompany.parentElement;
        
        clientTypeFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        
        let lastInsertedElement = companyP;
        clientTypeFields.forEach(field => {
            const values = fieldValuesMap.get(field.id) || [];
            const fieldP = document.createElement('p');
            fieldP.setAttribute('data-field-id', field.id);
            
            const strong = document.createElement('strong');
            strong.textContent = field.fieldLabel + ':';
            fieldP.appendChild(strong);

            const valueSpan = document.createElement('span');
            valueSpan.id = `modal-field-${field.id}`;
            
            if (values.length > 0) {
                if (field.allowMultiple) {
                    values.forEach((v, index) => {
                        if (index > 0) {
                            valueSpan.appendChild(document.createElement('br'));
                        }
                        if (field.fieldType === 'PHONE') {
                            const phone = v.valueText || '';
                            if (phone) {
                                const phoneLink = document.createElement('a');
                                phoneLink.href = `tel:${phone}`;
                                phoneLink.textContent = phone;
                                valueSpan.appendChild(phoneLink);
                            }
                        } else {
                            const value = formatFieldValueForModal(v, field);
                            if (value) {
                                valueSpan.appendChild(document.createTextNode(value));
                            }
                        }
                    });
                } else {
                    if (field.fieldType === 'PHONE') {
                        const phone = values[0].valueText || '';
                        if (phone) {
                            const phoneLink = document.createElement('a');
                            phoneLink.href = `tel:${phone}`;
                            phoneLink.textContent = phone;
                            valueSpan.appendChild(phoneLink);
                        } else {
                            valueSpan.className = 'empty-value';
                            valueSpan.textContent = '—';
                        }
                    } else {
                        const value = formatFieldValueForModal(values[0], field);
                        if (value) {
                            valueSpan.appendChild(document.createTextNode(value));
                        } else {
                            valueSpan.className = 'empty-value';
                            valueSpan.textContent = '—';
                        }
                    }
                }
            } else {
                valueSpan.className = 'empty-value';
                valueSpan.textContent = '—';
            }
            fieldP.appendChild(valueSpan);

            const canEdit = canEditClient(client);
            if (canEdit) {
                const editButton = document.createElement('button');
                editButton.className = 'edit-icon';
                editButton.setAttribute('data-field-id', field.id);
                editButton.setAttribute('title', 'Редагувати');
                editButton.onclick = () => enableEditField(field.id, field.fieldType, field.allowMultiple || false);
                const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                svg.setAttribute('viewBox', '0 0 24 24');
                const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                path.setAttribute('d', 'M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z');
                svg.appendChild(path);
                editButton.appendChild(svg);
                fieldP.appendChild(editButton);
            }
            
            fieldP.setAttribute('data-field-type', field.fieldType);

            lastInsertedElement.insertAdjacentElement('afterend', fieldP);
            lastInsertedElement = fieldP;
        });

        const sourceElement = modalClientSource?.parentElement;
        if (sourceElement) {
            sourceElement.style.display = '';
            modalClientSource.textContent = findNameByIdFromMap(sourceMap, client.sourceId);
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
        const sourceElement = modalClientSource?.parentElement;
        if (sourceElement) {
            sourceElement.style.display = '';
            modalClientSource.textContent = findNameByIdFromMap(sourceMap, client.sourceId);
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
    
    modalClientCreated.textContent = client.createdAt || '';
    modalClientUpdated.textContent = client.updatedAt || '';

    clientModal.style.display = 'flex';
    modalTimeoutId = setTimeout(() => {
        clientModal.classList.add('open');
        modalTimeoutId = null;
    }, 10);

    modalCloseHandler = () => {
        if (!editing) {
            clientModal.classList.remove('open');
            modalTimeoutId = setTimeout(() => {
                closeModal();
                modalTimeoutId = null;
            }, 200);
        } else {
            showMessage('Збережіть або відмініть зміни', 'error');
        }
    };
    closeModalClientBtn.addEventListener('click', modalCloseHandler);

    // Removed: modal click handler to prevent closing on outside click
    // modalClickHandler = function (event) {
    //     if (event.target === clientModal) {
    //         if (!editing) {
    //             closeModal();
    //         } else {
    //             showMessage('Збережіть або відмініть зміни', 'error');
    //         }
    //     }
    // };
    // window.addEventListener('click', modalClickHandler);
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
            clientModal.style.display = 'none';

            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        } catch (error) {
            console.error('Помилка видалення клієнта:', error);
            handleError(error);
        } finally {
            loaderBackdrop.style.display = 'none';
        }
    };



    const canDelete = canDeleteClient(client);

    if (client.isActive === false) {
        if (deleteButton) {
            deleteButton.style.display = 'none';
            deleteButton.dataset.originalDisplay = 'none';
        }
        if (restoreButton) {
            restoreButton.style.display = 'block';
            restoreButton.dataset.originalDisplay = 'block';
        }
    } else {
        if (deleteButton) {
            const displayValue = canDelete ? 'block' : 'none';
            deleteButton.style.display = displayValue;
            deleteButton.dataset.originalDisplay = displayValue;
        }
        if (restoreButton) {
            restoreButton.style.display = 'none';
            restoreButton.dataset.originalDisplay = 'none';
        }
    }
    
    // Сохраняем оригинальное состояние кнопки full-delete
    if (fullDeleteButton) {
        fullDeleteButton.dataset.originalDisplay = fullDeleteButton.style.display || 'block';
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
            clientModal.style.display = 'none';

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
                clientModal.style.display = 'none';

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

function cleanupCreateModalTimeouts() {
    createModalTimeoutIds.forEach(id => clearTimeout(id));
    createModalTimeoutIds = [];
}

if (openModalBtn) {
    openModalBtn.onclick = function () {
        if (!currentClientTypeId) {
            showMessage('Будь ласка, виберіть тип клієнта з навігації', 'error');
            return;
        }
        if (!createClientModal) return;
        buildDynamicCreateForm();
        createClientModal.classList.remove('hide');
        createClientModal.style.display = "flex";
        const timeoutId = setTimeout(() => {
            if (createClientModal) {
                createClientModal.classList.add('show');
            }
        }, 10);
        createModalTimeoutIds.push(timeoutId);
    };
}

if (createClientCloseBtn && createClientModal) {
    createClientCloseBtn.onclick = function () {
        cleanupCreateModalTimeouts();
        createClientModal.classList.remove('show');
        createClientModal.classList.add('hide');
        const timeoutId = setTimeout(() => {
            if (createClientModal) {
                createClientModal.style.display = "none";
            }
            resetForm();
        }, 300);
        createModalTimeoutIds.push(timeoutId);
    };
}

if (createModalClickHandler) {
    window.removeEventListener('click', createModalClickHandler);
}
// Removed: create modal click handler to prevent closing on outside click
// createModalClickHandler = function (event) {
//     if (event.target === createClientModal) {
//         cleanupCreateModalTimeouts();
//         createClientModal.classList.remove('show');
//         createClientModal.classList.add('hide');
//         const timeoutId = setTimeout(() => {
//             createClientModal.style.display = "none";
//             resetForm();
//         }, 300);
//         createModalTimeoutIds.push(timeoutId);
//     }
// };
// window.addEventListener('click', createModalClickHandler);

function buildDynamicCreateForm() {
    if (!currentClientTypeId) {
        return;
    }

    customSelectTimeoutIds.forEach(id => clearTimeout(id));
    customSelectTimeoutIds = [];

    const form = document.getElementById('client-form');
    form.textContent = '';

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
            const timeoutId = setTimeout(() => {
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
            customSelectTimeoutIds.push(timeoutId);
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

    const timeoutId = setTimeout(() => {
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
    customSelectTimeoutIds.push(timeoutId);

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
    outputDiv.textContent = '';

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
        const currentUserId = getUserId();
        if (!currentUserId || !availableSources || availableSources.length === 0) {
            return '';
        }
        const userSource = availableSources.find(source => {
            const sourceUserId = source.userId !== null && source.userId !== undefined 
                ? String(source.userId) 
                : null;
            return sourceUserId === String(currentUserId);
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

            createClientModal.style.display = "none";
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

let searchDebounceTimer = null;

function debounce(func, delay) {
    return function(...args) {
        clearTimeout(searchDebounceTimer);
        searchDebounceTimer = setTimeout(() => func.apply(this, args), delay);
    };
}

const performSearch = async () => {
    const searchTerm = searchInput.value;
    localStorage.setItem('searchTerm', searchTerm);
    loadDataWithSort(0, 100, currentSort, currentDirection);
};

const debouncedSearch = debounce(performSearch, 400);

searchInput.addEventListener('keypress', async (event) => {
    if (event.key === 'Enter') {
        clearTimeout(searchDebounceTimer);
        performSearch();
    } else {
        debouncedSearch();
    }
});

searchInput.addEventListener('input', () => {
    debouncedSearch();
});

/*--filter--*/

const filterButton = document.querySelector('.filter-button-block');
const filterModal = document.getElementById('filterModal');
const closeFilter = document.querySelector('.close-filter');
const modalContent = filterModal.querySelector('.modal-content-filter');

filterButton.addEventListener('click', () => {
    if (filterModalTimeoutId !== null) {
        clearTimeout(filterModalTimeoutId);
    }
    filterModal.style.display = 'block';
    filterModalTimeoutId = setTimeout(() => {
        filterModal.classList.add('show');
        filterModalTimeoutId = null;
    }, 10);
});

closeFilter.addEventListener('click', () => {
    closeModalFilter();
});

// Removed: filter modal click handler to prevent closing on outside click
// filterModal.addEventListener('click', (event) => {
//     if (!modalContent.contains(event.target)) {
//         closeModalFilter();
//     }
// });

function closeModalFilter() {
    if (filterModalTimeoutId !== null) {
        clearTimeout(filterModalTimeoutId);
        filterModalTimeoutId = null;
    }
    filterModal.classList.add('closing');
    modalContent.classList.add('closing-content');

    filterModalTimeoutId = setTimeout(() => {
        filterModal.style.display = 'none';
        filterModal.classList.remove('closing');
        modalContent.classList.remove('closing-content');
        filterModalTimeoutId = null;
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
        selectedFilters = {};
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
    if (!filterCounter || !filterCount) return;

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
        filterCount.textContent = totalFilters;
        filterCounter.style.display = 'inline-flex';
    } else {
        filterCount.textContent = '0';
        filterCounter.style.display = 'none';
    }
}


if (filterCounter) {
    filterCounter.addEventListener('click', () => {
        clearFilters();
    });
}

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

    if (searchInput) {
        searchInput.value = '';
    }

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

    select.textContent = '';

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
    if (!clientTypeSelectionModal || !clientTypesSelectionList) return;
    
    try {
        const response = await fetch('/api/v1/client-type/active');
        if (!response.ok) {
            console.error('Failed to load client types:', response.status, response.statusText);
            handleError(new ErrorResponse('CLIENT_ERROR_DEFAULT', 'Не вдалося завантажити типи клієнтів', null));
            return;
        }
        const allClientTypes = await response.json();

        const currentUserId = getUserId();
        let accessibleClientTypeIds = new Set();
        
        if (currentUserId) {
            try {
                const permissionsResponse = await fetch(`/api/v1/client-type/permission/me`);
                if (permissionsResponse.ok) {
                    const permissions = await permissionsResponse.json();
                    permissions.forEach(perm => {
                        if (perm.canView) {
                            accessibleClientTypeIds.add(perm.clientTypeId);
                        }
                    });
                } else {
                    console.warn('Failed to load user client type permissions:', permissionsResponse.status, permissionsResponse.statusText);
                }
            } catch (error) {
                console.warn('Failed to load user client type permissions:', error);
            }
        }

        const userAuthorities = getAuthorities();
        const isAdmin = userAuthorities.includes('system:admin') || userAuthorities.includes('administration:view');

        if (isAdmin || accessibleClientTypeIds.size === 0) {
            allClientTypes.forEach(type => accessibleClientTypeIds.add(type.id));
        }

        const accessibleClientTypes = allClientTypes.filter(type => accessibleClientTypeIds.has(type.id));
        
        if (accessibleClientTypes.length === 0) {
            const emptyMessage = document.createElement('p');
            emptyMessage.style.textAlign = 'center';
            emptyMessage.style.color = 'var(--main-grey)';
            emptyMessage.style.padding = '2em';
            emptyMessage.textContent = 'Немає доступних типів клієнтів';
            clientTypesSelectionList.appendChild(emptyMessage);
            clientTypeSelectionModal.style.display = 'flex';
        } else if (accessibleClientTypes.length === 1) {
            window.location.href = `/clients?type=${accessibleClientTypes[0].id}`;
            return;
        } else {
            clientTypesSelectionList.textContent = '';
            accessibleClientTypes.forEach(type => {
                const card = document.createElement('div');
                card.className = 'client-type-card';
                
                const iconDiv = document.createElement('div');
                iconDiv.className = 'client-type-card-icon';
                iconDiv.textContent = '👥';
                card.appendChild(iconDiv);
                
                const nameDiv = document.createElement('div');
                nameDiv.className = 'client-type-card-name';
                nameDiv.textContent = type.name;
                card.appendChild(nameDiv);
                
                card.addEventListener('click', () => {
                    window.location.href = `/clients?type=${type.id}`;
                });
                clientTypesSelectionList.appendChild(card);
            });
            clientTypeSelectionModal.style.display = 'flex';
        }

        const closeBtn = document.querySelector('.close-client-type-modal');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                clientTypeSelectionModal.style.display = 'none';
            });
        }
        
        clientTypeSelectionModal.addEventListener('click', (e) => {
            if (e.target === clientTypeSelectionModal) {
                clientTypeSelectionModal.style.display = 'none';
            }
        });
    } catch (error) {
        console.error('Error loading client types:', error);
    }
}

async function updateNavigationWithCurrentType(typeId) {
    try {
        const response = await fetch(`/api/v1/client-type/${typeId}`);
        if (!response.ok) {
            console.error('Failed to load client type:', response.status, response.statusText);
            return;
        }
        
        const clientType = await response.json();
        const navLink = document.querySelector('#nav-clients a');
        
        if (navLink && clientType.name) {
            navLink.textContent = '';
            
            const labelSpan = document.createElement('span');
            labelSpan.className = 'nav-client-type-label';
            labelSpan.textContent = 'Клієнти:';
            navLink.appendChild(labelSpan);
            
            const nameSpan = document.createElement('span');
            nameSpan.className = 'nav-client-type-name';
            nameSpan.textContent = clientType.name;
            navLink.appendChild(nameSpan);
            
            const arrowSpan = document.createElement('span');
            arrowSpan.className = 'dropdown-arrow';
            arrowSpan.textContent = '▼';
            navLink.appendChild(arrowSpan);
        }

        const dropdown = document.getElementById('client-types-dropdown');
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




