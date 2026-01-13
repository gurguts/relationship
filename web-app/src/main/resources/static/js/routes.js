const prevPageButton = document.getElementById('prev-btn');
const nextPageButton = document.getElementById('next-btn');
const paginationInfo = document.getElementById('pagination-info');
const allClientInfo = document.getElementById('all-client-info');
const loaderBackdrop = document.getElementById('loader-backdrop');
let currentSort = CLIENT_SORT_FIELDS.UPDATED_AT;
let currentDirection = CLIENT_SORT_DIRECTIONS.DESC;
const filterForm = document.getElementById('filterForm');
const customSelects = {};
const searchInput = document.getElementById('inputSearch');

let availableSources = [];
let sourceMap;

const userId = ClientState.getUserId();
const selectedFilters = {};

let filterModalTimeoutId = null;
let columnResizerTimeoutId = null;
const editingState = { editing: false };

let currentPage = 0;
let pageSize = CLIENT_CONSTANTS.DEFAULT_PAGE_SIZE;
const tableBody = document.getElementById('client-table-body');

let currentClientTypeId = null;
let currentClientType = null;
let clientTypeFields = [];
let visibleFields = [];
let filterableFields = [];
let visibleInCreateFields = [];

document.addEventListener('DOMContentLoaded', async () => {
    const savedFilters = localStorage.getItem('selectedFilters');
    let parsedFilters;
    if (savedFilters) {
        try {
            parsedFilters = JSON.parse(savedFilters);
            parsedFilters = ClientUtils.normalizeFilterKeys(parsedFilters);
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
        const clientTypeSelectionModal = document.getElementById('clientTypeSelectionModal');
        const clientTypesSelectionList = document.getElementById('client-types-selection-list');
        await RouteTypeManager.showClientTypeSelectionModal(clientTypeSelectionModal, clientTypesSelectionList);
        return;
    }
    
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
        
        try {
            currentClientType = await RouteDataLoader.loadClientType(currentClientTypeId);
            await RouteTypeManager.updateNavigationWithCurrentType(newClientTypeId, currentClientType);
            document.title = currentClientType.name;
            const fieldsData = await RouteDataLoader.loadClientTypeFields(currentClientTypeId);
            clientTypeFields = fieldsData.all || [];
            window.clientTypeFields = clientTypeFields;
            visibleFields = fieldsData.visible || [];
            window.visibleFields = visibleFields;
            filterableFields = fieldsData.filterable || [];
            visibleInCreateFields = fieldsData.visibleInCreate || [];
        } catch (error) {
            console.error('Error loading client type data:', error);
            handleError(error);
            return;
        }
        RouteRenderer.buildDynamicTable(visibleFields, currentClientType, currentClientTypeId, (typeId) => {
            if (typeof initColumnResizer === 'function' && typeId) {
                if (columnResizerTimeoutId !== null) {
                    clearTimeout(columnResizerTimeoutId);
                }
                columnResizerTimeoutId = setTimeout(() => {
                    initColumnResizer(typeId);
                    if (typeof applyColumnWidths === 'function') {
                        applyColumnWidths(typeId);
                    }
                    columnResizerTimeoutId = null;
                }, 0);
            }
        });
        buildDynamicFilters();
        
        const validFieldNames = new Set(filterableFields.map(f => f.fieldName));
        
        const cleanedFilters = ClientUtils.normalizeFilterKeys(selectedFilters, staticFilterKeys, validFieldNames);
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
        const defaultSources = await RouteDataLoader.loadDefaultClientData();
        if (defaultSources) {
            availableSources = defaultSources;
            sourceMap = new Map(availableSources.map(s => [s.id, s.name]));
        }
    }
    
    await loadEntitiesAndApplyFilters();
    
    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
});

async function loadEntitiesAndApplyFilters() {
    try {
        const data = await RouteDataLoader.loadEntities();
        if (!data) return;
        
        availableSources = data.sources || [];
        sourceMap = new Map(availableSources.map(item => [item.id, item.name]));
        
        if (!window.sourceMap || window.sourceMap.size === 0) {
            window.sourceMap = sourceMap;
        }

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





async function renderClients(clients) {
    const openCreatePurchaseModalWrapper = (clientId) => {
        RoutePurchaseModal.openCreatePurchaseModal(clientId, {
            modal: document.getElementById('createPurchaseModal'),
            form: document.getElementById('createPurchaseForm'),
            clientIdInput: document.getElementById('purchaseClientId'),
            sourceIdInput: document.getElementById('purchaseSourceId'),
            userIdSelect: document.getElementById('purchaseUserId'),
            productIdSelect: document.getElementById('purchaseProductId'),
            currencySelect: document.getElementById('purchaseCurrency'),
            exchangeRateLabel: document.getElementById('exchangeRateLabel'),
            exchangeRateInput: document.getElementById('purchaseExchangeRate'),
            exchangeRateWarning: document.getElementById('exchange-rate-warning'),
            userId: userId,
            loaderBackdrop: loaderBackdrop
        });
    };
    
    const openCreateContainerModalWrapper = (clientId) => {
        RouteContainerModal.openCreateContainerModal(clientId, {
            modal: document.getElementById('createContainerModal'),
            form: document.getElementById('createContainerForm'),
            clientIdInput: document.getElementById('containerClientId'),
            userIdSelect: document.getElementById('containerUserId'),
            containerIdSelect: document.getElementById('containerContainerId'),
            userId: userId,
            loaderBackdrop: loaderBackdrop
        });
    };
    
    await RouteRenderer.renderClients(
        clients,
        tableBody,
        currentClientTypeId,
        visibleFields,
        currentClientType,
        sourceMap,
        loadClientDetails,
        openCreatePurchaseModalWrapper,
        openCreateContainerModalWrapper,
        (typeId) => {
            if (typeof applyColumnWidths === 'function' && typeId) {
        setTimeout(() => {
                    applyColumnWidths(typeId);
        }, 0);
    }
}
    );
    
    RouteRenderer.setupSortHandlers(currentSort, currentDirection, (newSort, newDirection) => {
        currentSort = newSort;
        currentDirection = newDirection;
        currentPage = 0;
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
    });
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
    if (loaderBackdrop) {
    loaderBackdrop.style.display = 'flex';
    }
    const searchTerm = searchInput && searchInput.value ? searchInput.value : '';
    const params = {
        page: page.toString(),
        size: size.toString(),
        sort: sort,
        direction: direction
    };

    if (currentClientTypeId) {
        params.clientTypeId = currentClientTypeId.toString();
    }

    if (searchTerm) {
        params.q = searchTerm;
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
        params.filters = JSON.stringify(cleanedFilters);
    }

    try {
        const data = await RouteDataLoader.searchClients(params);
        await renderClients(data.content);
        RouteRenderer.setupSortHandlers(currentSort, currentDirection, (newSort, newDirection) => {
            currentSort = newSort;
            currentDirection = newDirection;
            currentPage = 0;
            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        });
        RouteRenderer.updatePagination(data.totalElements, data.content.length, data.totalPages, currentPage, prevPageButton, nextPageButton, allClientInfo, paginationInfo);
    } catch (error) {
        handleError(error instanceof ErrorResponse ? error : new Error(error.message || 'Failed to load clients'));
    } finally {
        if (loaderBackdrop) {
        loaderBackdrop.style.display = 'none';
    }
}
}


function loadClientDetails(client) {
    const enableEditFieldWrapper = (fieldId, fieldType, allowMultiple) => {
        ClientEditor.enableEditField(fieldId, fieldType, allowMultiple, {
            clientTypeFields,
            customSelects,
            editingState
        });
    };
    ClientModal.showClientModal(client, {
        clientModal: document.getElementById('client-modal'),
        closeModalClientBtn: document.getElementById('close-modal-client'),
        modalClientId: document.getElementById('modal-client-id'),
        modalClientCompany: document.getElementById('modal-client-company'),
        modalClientSource: document.getElementById('modal-client-source'),
        modalClientCreated: document.getElementById('modal-client-created'),
        modalClientUpdated: document.getElementById('modal-client-updated'),
        fullDeleteButton: document.getElementById('full-delete-client'),
        deleteButton: document.getElementById('delete-client'),
        restoreButton: document.getElementById('restore-client'),
        loaderBackdrop: loaderBackdrop,
        currentClientTypeId: currentClientTypeId,
        currentClientType: currentClientType,
        clientTypeFields: clientTypeFields,
        availableSources: availableSources,
        sourceMap: sourceMap,
        loadDataWithSort: loadDataWithSort,
        currentPage: currentPage,
        pageSize: pageSize,
        currentSort: currentSort,
        currentDirection: currentDirection,
        enableEditField: enableEditFieldWrapper,
        editingState: editingState
    });
}

/*-------create client-------*/

var modal = document.getElementById("createClientModal");
var btn = document.getElementById("open-modal");
var span = document.getElementsByClassName("create-client-close")[0];

if (btn) {
    btn.addEventListener('click', function () {
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
    });
}

if (span) {
    span.addEventListener('click', function () {
        modal.classList.remove('show');
        modal.classList.add('hide');
        setTimeout(() => {
            modal.style.display = "none";
            resetForm();
        }, 300);
    });
}

// Removed: create modal click handler to prevent closing on outside click
// const handleCreateModalClick = function (event) {
//     if (event.target === modal) {
//         modal.classList.remove('show');
//         modal.classList.add('hide');
//         setTimeout(() => {
//             modal.style.display = "none";
//             resetForm();
//         }, 300);
//     }
// };
// modal.removeEventListener('click', handleCreateModalClick);
// modal.addEventListener('click', handleCreateModalClick);

const defaultValues = {
    source: () => {
        const currentUserId = ClientState.getUserId();
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

function buildDynamicCreateForm() {
    ClientForm.buildDynamicCreateForm(
        currentClientTypeId,
        currentClientType,
        visibleInCreateFields,
        availableSources,
        customSelects,
        [],
        defaultValues
    );
}

function resetForm() {
    ClientForm.resetForm(currentClientTypeId, buildDynamicCreateForm, customSelects, defaultValues);
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

const performSearch = async () => {
    if (!searchInput) return;
    const searchTerm = searchInput.value || '';
    localStorage.setItem('searchTerm', searchTerm);
    loadDataWithSort(0, CLIENT_CONSTANTS.DEFAULT_PAGE_SIZE * 2, currentSort, currentDirection);
};

const debouncedSearch = ClientUtils.debounce(performSearch, CLIENT_CONSTANTS.SEARCH_DEBOUNCE_DELAY);

if (searchInput) {
    searchInput.addEventListener('keypress', async (event) => {
        if (event.key === 'Enter') {
            performSearch();
        } else {
            debouncedSearch();
        }
    });

    searchInput.addEventListener('input', () => {
        debouncedSearch();
    });
}

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

function closeModalFilter() {
    const filterModalTimeoutIdRef = { current: filterModalTimeoutId };
    ClientFilters.closeModalFilter(filterModal, modalContent, filterModalTimeoutIdRef);
    filterModalTimeoutId = filterModalTimeoutIdRef.current;
}


const modalFilterButtonSubmit = document.getElementById("modal-filter-button-submit");
if (modalFilterButtonSubmit) {
    modalFilterButtonSubmit.addEventListener('click', (event) => {
        event.preventDefault();
        updateSelectedFilters();
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        closeModalFilter();
    });
}


function updateSelectedFilters() {
    ClientFilters.updateSelectedFilters(selectedFilters, filterForm, customSelects, filterableFields, () => {
        updateFilterCounter();
    });
}

const filterCounter = document.getElementById('filter-counter');
const filterCount = document.getElementById('filter-count');

function updateFilterCounter() {
    ClientFilters.updateFilterCounter(selectedFilters, filterCounter, filterCount);
}

if (filterCounter) {
    filterCounter.addEventListener('click', () => {
    clearFilters();
    });
}

function clearFilters() {
    ClientFilters.clearFilters(selectedFilters, filterForm, customSelects, searchInput, () => {
    updateFilterCounter();
    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
    });
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


document.addEventListener('DOMContentLoaded', () => {
    RoutePurchaseModal.init({
        currencySelect: document.getElementById('purchaseCurrency'),
        exchangeRateLabel: document.getElementById('exchangeRateLabel'),
        exchangeRateInput: document.getElementById('purchaseExchangeRate'),
        closeButton: document.getElementById('closeCreatePurchaseModal'),
        form: document.getElementById('createPurchaseForm'),
        modal: document.getElementById('createPurchaseModal'),
        loaderBackdrop: loaderBackdrop
    });
    
    RouteContainerModal.init({
        closeButton: document.getElementById('closeCreateContainerModal'),
        form: document.getElementById('createContainerForm'),
        modal: document.getElementById('createContainerModal'),
        loaderBackdrop: loaderBackdrop
    });
    
    const saveClientBtn = document.getElementById('save-client');
    if (saveClientBtn && !saveClientBtn.hasAttribute('data-listener-attached')) {
        saveClientBtn.setAttribute('data-listener-attached', 'true');
        saveClientBtn.addEventListener('click', () => {
            if (typeof saveClientChanges === 'function') {
                saveClientChanges();
            }
        });
    }
    
    const cancelClientBtn = document.getElementById('cancel-client');
    if (cancelClientBtn && !cancelClientBtn.hasAttribute('data-listener-attached')) {
        cancelClientBtn.setAttribute('data-listener-attached', 'true');
        cancelClientBtn.addEventListener('click', () => {
            if (typeof cancelClientChanges === 'function') {
                cancelClientChanges();
            }
        });
    }
    
    const editCompanyBtn = document.getElementById('edit-company');
    if (editCompanyBtn && !editCompanyBtn.hasAttribute('data-listener-attached')) {
        editCompanyBtn.setAttribute('data-listener-attached', 'true');
        editCompanyBtn.addEventListener('click', () => {
            if (typeof enableEdit === 'function') {
                enableEdit('company');
            }
        });
    }
    
    const editSourceBtn = document.getElementById('edit-source');
    if (editSourceBtn && !editSourceBtn.hasAttribute('data-listener-attached')) {
        editSourceBtn.setAttribute('data-listener-attached', 'true');
        editSourceBtn.addEventListener('click', () => {
            if (typeof enableSelect === 'function' && typeof availableSources !== 'undefined') {
                enableSelect('source', availableSources);
            }
        });
    }
});
