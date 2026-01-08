const prevPageButton = document.getElementById('prev-btn');
const nextPageButton = document.getElementById('next-btn');
const paginationInfo = document.getElementById('pagination-info');
const allClientInfo = document.getElementById('all-client-info');
const loaderBackdrop = document.getElementById('loader-backdrop');
let currentSort = CLIENT_SORT_FIELDS.UPDATED_AT;
let currentDirection = CLIENT_SORT_DIRECTIONS.DESC;
const filterForm = document.getElementById('filterForm');
const customSelects = {};

let availableSources = [];
let sourceMap;

const userId = ClientState.getUserId();
const selectedFilters = {};


let currentPage = 0;
let pageSize = CLIENT_CONSTANTS.DEFAULT_PAGE_SIZE;
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

let filterModalTimeoutId = null;
let columnResizerTimeoutId = null;
let customSelectTimeoutIds = [];
const editingState = { editing: false };
let createModalTimeoutIds = [];

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
        await ClientTypeManager.showClientTypeSelectionModal(clientTypeSelectionModal, clientTypesSelectionList);
        return;
    }
    
    await ClientTypeManager.updateNavigationWithCurrentType(typeId);
    
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
        
        await ClientTypeManager.updateNavigationWithCurrentType(newClientTypeId);
        
        try {
            currentClientType = await ClientDataLoader.loadClientType(currentClientTypeId);
            document.title = currentClientType.name;
            const fieldsData = await ClientDataLoader.loadClientTypeFields(currentClientTypeId);
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
        buildDynamicTable();
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
        try {
            const defaultSources = await ClientDataLoader.loadDefaultClientData();
            if (defaultSources) {
                availableSources = defaultSources;
                sourceMap = new Map(availableSources.map(s => [s.id, s.name]));
            }
        } catch (error) {
            console.error('Error loading default client data:', error);
            handleError(error);
        }
    }
    
    await loadEntitiesAndApplyFilters();
    
    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
});

async function loadEntitiesAndApplyFilters() {
    try {
        const data = await ClientDataLoader.loadEntities();
        if (!data) return;
        
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


function buildDynamicTable() {
    if (!currentClientType) return;
    
    const thead = document.querySelector('#client-list table thead tr');
    if (!thead) return;
    
    ClientRenderer.buildDynamicTable(visibleFields, currentClientType, currentClientTypeId, (typeId) => {
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
        createdAtH2.textContent = CLIENT_MESSAGES.CREATED_AT_LABEL;
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
        createdAtToLabel.textContent = CLIENT_MESSAGES.TO;
        createdAtBlock.appendChild(createdAtToLabel);
        
        const createdAtToInput = document.createElement('input');
        createdAtToInput.type = 'date';
        createdAtToInput.id = 'filter-createdAt-to';
        createdAtToInput.name = 'createdAtTo';
        createdAtBlock.appendChild(createdAtToInput);
        
        filterForm.appendChild(createdAtBlock);
        
        const updatedAtH2 = document.createElement('h2');
        updatedAtH2.textContent = CLIENT_MESSAGES.UPDATED_AT_LABEL;
        filterForm.appendChild(updatedAtH2);
        
        const updatedAtBlock = document.createElement('div');
        updatedAtBlock.className = 'filter-block';
        
        const updatedAtFromLabel = document.createElement('label');
        updatedAtFromLabel.className = 'from-to-style';
        updatedAtFromLabel.setAttribute('for', 'filter-updatedAt-from');
        updatedAtFromLabel.textContent = CLIENT_MESSAGES.FROM;
        updatedAtBlock.appendChild(updatedAtFromLabel);
        
        const updatedAtFromInput = document.createElement('input');
        updatedAtFromInput.type = 'date';
        updatedAtFromInput.id = 'filter-updatedAt-from';
        updatedAtFromInput.name = 'updatedAtFrom';
        updatedAtBlock.appendChild(updatedAtFromInput);
        
        const updatedAtToLabel = document.createElement('label');
        updatedAtToLabel.className = 'from-to-style';
        updatedAtToLabel.setAttribute('for', 'filter-updatedAt-to');
        updatedAtToLabel.textContent = CLIENT_MESSAGES.TO;
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
        sourceLabel.textContent = CLIENT_MESSAGES.SOURCE_LABEL;
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
                    fromLabel.textContent = CLIENT_MESSAGES.FROM;
                    filterBlock.appendChild(fromLabel);
                    
                    const fromInput = document.createElement('input');
                    fromInput.type = 'date';
                    fromInput.id = `filter-${field.fieldName}-from`;
                    fromInput.name = `${field.fieldName}From`;
                    filterBlock.appendChild(fromInput);
                    
                    const toLabel = document.createElement('label');
                    toLabel.className = 'from-to-style';
                    toLabel.setAttribute('for', `filter-${field.fieldName}-to`);
                    toLabel.textContent = CLIENT_MESSAGES.TO;
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
                    fromLabel.textContent = CLIENT_MESSAGES.FROM;
                    filterBlock.appendChild(fromLabel);
                    
                    const fromInput = document.createElement('input');
                    fromInput.type = 'number';
                    fromInput.id = `filter-${field.fieldName}-from`;
                    fromInput.name = `${field.fieldName}From`;
                    fromInput.step = 'any';
                    fromInput.placeholder = CLIENT_MESSAGES.MIN;
                    filterBlock.appendChild(fromInput);
                    
                    const toLabel = document.createElement('label');
                    toLabel.className = 'from-to-style';
                    toLabel.setAttribute('for', `filter-${field.fieldName}-to`);
                    toLabel.textContent = CLIENT_MESSAGES.TO;
                    filterBlock.appendChild(toLabel);
                    
                    const toInput = document.createElement('input');
                    toInput.type = 'number';
                    toInput.id = `filter-${field.fieldName}-to`;
                    toInput.name = `${field.fieldName}To`;
                    toInput.step = 'any';
                    toInput.placeholder = CLIENT_MESSAGES.MAX;
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
                    input.placeholder = CLIENT_MESSAGES.SEARCH_PLACEHOLDER;
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
                    allOption.textContent = CLIENT_MESSAGES.ALL;
                    select.appendChild(allOption);
                    
                    const yesOption = document.createElement('option');
                    yesOption.value = 'true';
                    yesOption.textContent = CLIENT_MESSAGES.YES;
                    select.appendChild(yesOption);
                    
                    const noOption = document.createElement('option');
                    noOption.value = 'false';
                    noOption.textContent = CLIENT_MESSAGES.NO;
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
        span.textContent = CLIENT_MESSAGES.SHOW_INACTIVE_LABEL;
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
    ClientRenderer.updatePagination(totalClients, clientsOnPage, totalPages, currentPageIndex, prevPageButton, nextPageButton, allClientInfo, paginationInfo);
}




async function renderClients(clients) {
    await ClientRenderer.renderClients(clients, tableBody, currentClientTypeId, visibleFields, currentClientType, sourceMap, availableSources, loadClientDetails);
    
    ClientRenderer.setupSortHandlers(currentSort, currentDirection, (newSort, newDirection) => {
        currentSort = newSort;
        currentDirection = newDirection;
        currentPage = 0;
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
    });

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
        const data = await ClientDataLoader.searchClients(params);
        await renderClients(data.content);
        ClientRenderer.setupSortHandlers(currentSort, currentDirection, (newSort, newDirection) => {
            currentSort = newSort;
            currentDirection = newDirection;
            currentPage = 0;
            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        });
        updatePagination(data.totalElements, data.content.length, data.totalPages, currentPage);
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
        clientModal,
        closeModalClientBtn,
        modalClientId,
        modalClientCompany,
        modalClientSource,
        modalClientCreated,
        modalClientUpdated,
        fullDeleteButton,
        deleteButton,
        restoreButton,
        loaderBackdrop,
        currentClientTypeId,
        currentClientType,
        clientTypeFields,
        availableSources,
        sourceMap,
        loadDataWithSort,
        currentPage,
        pageSize,
        currentSort,
        currentDirection,
        enableEditField: enableEditFieldWrapper,
        editingState
    });
}

function closeModal() {
    ClientModal.closeModal(clientModal, closeModalClientBtn, editingState.editing);
    editingState.editing = false;
}


/*-------create client-------*/

function cleanupCreateModalTimeouts() {
    createModalTimeoutIds.forEach(id => clearTimeout(id));
    createModalTimeoutIds = [];
}

if (openModalBtn) {
    openModalBtn.onclick = function () {
        if (!currentClientTypeId) {
            showMessage(CLIENT_MESSAGES.SELECT_CLIENT_TYPE, 'error');
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


function buildDynamicCreateForm() {
    ClientForm.buildDynamicCreateForm(
        currentClientTypeId,
        currentClientType,
        visibleInCreateFields,
        availableSources,
        customSelects,
        customSelectTimeoutIds,
        defaultValues
    );
}


function resetForm() {
    ClientForm.resetForm(currentClientTypeId, buildDynamicCreateForm, customSelects, defaultValues);
}

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
            return sourceUserId === String(currentUserId);
        });
        return userSource ? String(userSource.id) : '';
    }
};



document.getElementById('client-form').addEventListener('submit',
    async function (event) {
        event.preventDefault();

        if (!currentClientTypeId) {
            showMessage(CLIENT_MESSAGES.SELECT_CLIENT_TYPE, 'error');
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
            showMessage(CLIENT_MESSAGES.FIX_PHONE_ERRORS, 'error');
            return;
        }

        if (loaderBackdrop) {
            loaderBackdrop.style.display = 'flex';
        }

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
            } else if (fieldValue && !(field.fieldType === 'LIST' && field.allowMultiple)) {
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
            const data = await ClientDataLoader.createClient(clientData);

            createClientModal.style.display = "none";
            resetForm();
            loadDataWithSort(0, pageSize, currentSort, currentDirection);

            showMessage(CLIENT_MESSAGES.CLIENT_CREATED.replace('{id}', data.id), 'info');
        } catch (error) {
            console.error('Error creating client:', error);
            handleError(error instanceof ErrorResponse ? error : new ErrorResponse('CREATE_ERROR', error.message || 'Failed to create client'));
        } finally {
            if (loaderBackdrop) {
                loaderBackdrop.style.display = 'none';
            }
        }
    });


/*--search--*/

let searchDebounceTimer = null;

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
            clearTimeout(searchDebounceTimer);
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
const modalContent = filterModal ? filterModal.querySelector('.modal-content-filter') : null;

if (filterButton && filterModal) {
    filterButton.addEventListener('click', () => {
    if (filterModalTimeoutId !== null) {
        clearTimeout(filterModalTimeoutId);
    }
        filterModal.style.display = 'block';
        filterModalTimeoutId = setTimeout(() => {
            filterModal.classList.add('show');
            filterModalTimeoutId = null;
        }, CLIENT_CONSTANTS.MODAL_ANIMATION_DELAY);
    });
}

if (closeFilter) {
    closeFilter.addEventListener('click', () => {
        closeModalFilter();
    });
}

// Removed: filter modal click handler to prevent closing on outside click
// filterModal.addEventListener('click', (event) => {
//     if (!modalContent.contains(event.target)) {
//         closeModalFilter();
//     }
// });

function closeModalFilter() {
    const filterModalTimeoutIdRef = { current: filterModalTimeoutId };
    ClientFilters.closeModalFilter(filterModal, modalContent, filterModalTimeoutIdRef);
    filterModalTimeoutId = filterModalTimeoutIdRef.current;
}

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





