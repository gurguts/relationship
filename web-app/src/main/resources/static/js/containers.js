const prevPageButton = document.getElementById('prev-btn');
const nextPageButton = document.getElementById('next-btn');
const paginationInfo = document.getElementById('pagination-info');
const allClientInfo = document.getElementById('all-client-info');
const loaderBackdrop = document.getElementById('loader-backdrop');
const filterForm = document.getElementById('filterForm');
const searchInput = document.getElementById('inputSearch');
const tableBody = document.getElementById('client-table-body');
const filterCounter = document.getElementById('filter-counter');
const filterCount = document.getElementById('filter-count');
const modalFilterButtonSubmit = document.getElementById('modal-filter-button-submit');
const filterButton = document.querySelector('.filter-button-block');
const filterModal = document.getElementById('filterModal');
const closeFilter = document.querySelector('.close-filter');
const modalContent = filterModal ? filterModal.querySelector('.modal-content-filter') : null;

let currentSort = 'updatedAt';
let currentDirection = 'DESC';
let currentPage = 0;
let pageSize = CLIENT_CONSTANTS.DEFAULT_PAGE_SIZE;
const selectedFilters = {};
const customSelects = {};

let currentClientTypeId = null;
let currentClientType = null;
let clientTypeFields = [];
let filterableFields = [];

let availableSources = [];
let availableUsers = [];
let availableContainers = [];

let sourceMap;
let userMap;
let containerMap;

let filterModalTimeoutId = null;
const editingState = { editing: false };

if (prevPageButton) {
    prevPageButton.addEventListener('click', () => {
        if (currentPage > 0) {
            currentPage--;
            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        }
    });
}

if (nextPageButton) {
    nextPageButton.addEventListener('click', () => {
        currentPage++;
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
    });
}

async function loadDataWithSort(page, size, sort, direction) {
    if (!currentClientTypeId) {
        return;
    }
    
    if (loaderBackdrop) {
        loaderBackdrop.style.display = 'flex';
    }

    const searchTerm = searchInput ? searchInput.value : '';
    const filters = { ...selectedFilters };
    if (currentClientTypeId) {
        filters.clientTypeId = [currentClientTypeId.toString()];
    }

    const convertedFilters = ContainerFilters.convertFieldNamesToFieldIds(filters, filterableFields, clientTypeFields);
    
    const normalizedFilters = {};
    Object.keys(convertedFilters).forEach(key => {
        const lowerKey = key.toLowerCase();
        if (lowerKey === 'updatedatfrom') {
            normalizedFilters['updatedAtFrom'] = convertedFilters[key];
        } else if (lowerKey === 'updatedatto') {
            normalizedFilters['updatedAtTo'] = convertedFilters[key];
        } else {
            normalizedFilters[key] = convertedFilters[key];
        }
    });
    
    const params = {
        page: page,
        size: size,
        sort: sort,
        direction: direction
    };
    
    if (searchTerm) {
        params.q = searchTerm;
    }
    
    if (Object.keys(normalizedFilters).length > 0) {
        params.filters = JSON.stringify(normalizedFilters);
    }

    try {
        const data = await ContainerDataLoader.loadContainerData(params);
        
        ContainerRenderer.renderContainers(data.content, {
            tableBody: tableBody,
            userMap: userMap,
            loadClientDetailsFn: loadClientDetails,
            currentClientTypeId: currentClientTypeId,
            applyColumnWidthsFn: typeof applyColumnWidthsForContainers === 'function' ? applyColumnWidthsForContainers : null
        });
        
        ContainerRenderer.setupSortHandlers(currentSort, currentDirection, (newSort, newDirection) => {
            currentSort = newSort;
            currentDirection = newDirection;
            currentPage = 0;
            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        });

        ContainerRenderer.updatePagination(data.totalElements, data.content.length, data.totalPages, currentPage, prevPageButton, nextPageButton, allClientInfo, paginationInfo);
    } catch (error) {
        console.error('Ошибка:', error);
        handleError(error);
    } finally {
        if (loaderBackdrop) {
            loaderBackdrop.style.display = 'none';
        }
    }
}

function buildDynamicFilters() {
    ContainerFilters.buildDynamicFilters({
        filterForm: filterForm,
        customSelects: customSelects,
        selectedFilters: selectedFilters,
        filterableFields: filterableFields,
        availableSources: availableSources,
        availableUsers: availableUsers,
        availableContainers: availableContainers
    });
}

async function loadEntitiesAndApplyFilters() {
    try {
        if (!availableSources || availableSources.length === 0 || !availableUsers || availableUsers.length === 0) {
            const data = await ContainerDataLoader.loadEntities();
            if (!data) return;
            
            availableSources = data.sources || [];
            availableUsers = data.users || [];
            
            sourceMap = new Map(availableSources.map(item => [item.id, item.name]));
            userMap = new Map(availableUsers.map(item => [item.id, item.name]));
        }

        ContainerFilters.restoreFilterValues({
            filterForm: filterForm,
            selectedFilters: selectedFilters,
            customSelects: customSelects,
            filterableFields: filterableFields,
            availableSources: availableSources,
            availableUsers: availableUsers,
            availableContainers: availableContainers
        });
    } catch (error) {
        console.error('Error loading entities:', error);
    }
}

function loadClientDetails(client) {
    if (!client || !client.id) {
        return;
    }
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
            event.preventDefault();
            performSearch();
        } else {
            debouncedSearch();
        }
    });

    searchInput.addEventListener('input', () => {
        debouncedSearch();
    });
}

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
    ContainerFilters.updateSelectedFilters({
        selectedFilters: selectedFilters,
        filterForm: filterForm,
        customSelects: customSelects,
        filterableFields: filterableFields
    });
    updateFilterCounter();
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
        const modal = document.getElementById('clientTypeSelectionModal');
        const list = document.getElementById('client-types-selection-list');
        await ContainerTypeManager.showClientTypeSelectionModal(modal, list);
        return;
    }
    
    const newClientTypeId = parseInt(typeId);
    const savedClientTypeId = localStorage.getItem('currentClientTypeId');
    const staticFilterKeys = ['updatedAtFrom', 'updatedAtTo', 'source', 'user', 'container', 'quantityFrom', 'quantityTo', 'clientCreatedAtFrom', 'clientCreatedAtTo', 'clientUpdatedAtFrom', 'clientUpdatedAtTo', 'clientSource'];
    
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
    
    await ContainerTypeManager.updateNavigationWithCurrentType(newClientTypeId);
    
    try {
        currentClientType = await ContainerDataLoader.loadClientType(currentClientTypeId);
        document.title = currentClientType.name;
        const fieldsData = await ContainerDataLoader.loadClientTypeFields(currentClientTypeId);
        clientTypeFields = fieldsData.all || [];
        window.clientTypeFields = clientTypeFields;
        filterableFields = fieldsData.filterable || [];
        window.filterableFields = filterableFields;
    } catch (error) {
        console.error('Error loading client type data:', error);
        handleError(error);
        return;
    }
    
    ContainerRenderer.buildContainersTable();
    buildDynamicFilters();
    
    if (typeof initColumnResizerForContainers === 'function' && currentClientTypeId) {
        setTimeout(() => {
            const storageKey = `containers_${currentClientTypeId}`;
            initColumnResizerForContainers('client-list', storageKey);
            if (typeof applyColumnWidthsForContainers === 'function') {
                applyColumnWidthsForContainers('client-list', storageKey);
            }
        }, 0);
    }

    const validFieldNames = new Set(filterableFields.map(f => f.fieldName));
    const cleanedFilters = ClientUtils.normalizeFilterKeys(selectedFilters, staticFilterKeys, validFieldNames);
    Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);
    Object.assign(selectedFilters, cleanedFilters);
    
    if (Object.keys(selectedFilters).length === 0) {
        localStorage.removeItem('selectedFilters');
    } else {
        localStorage.setItem('selectedFilters', JSON.stringify(selectedFilters));
    }
    
    ContainerFilters.restoreFilterValues({
        filterForm: filterForm,
        selectedFilters: selectedFilters,
        customSelects: customSelects,
        filterableFields: filterableFields,
        availableSources: availableSources,
        availableUsers: availableUsers,
        availableContainers: availableContainers
    });
    
    ContainerFilters.restoreDynamicClientFields({
        filterableFields: filterableFields,
        selectedFilters: selectedFilters,
        customSelects: customSelects
    });
    updateFilterCounter();

    try {
        const [containersRes, entitiesRes] = await Promise.all([
            ContainerDataLoader.loadContainers(),
            ContainerDataLoader.loadEntities()
        ]);

        availableContainers = containersRes || [];
        containerMap = new Map(availableContainers.map(item => [item.id, item.name]));

        if (entitiesRes) {
            availableSources = entitiesRes.sources || [];
            availableUsers = entitiesRes.users || [];

            sourceMap = new Map(availableSources.map(item => [item.id, item.name]));
            userMap = new Map(availableUsers.map(item => [item.id, item.name]));
        }

        await loadEntitiesAndApplyFilters();
        
        ClientDetailsModals.init();
        
        await loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        
        if (typeof initExcelExportContainer === 'function') {
            initExcelExportContainer({
                triggerId: 'exportToExcelData',
                modalId: 'exportModalData',
                cancelId: 'exportCancel',
                confirmId: 'exportConfirm',
                formId: 'exportFieldsForm',
                searchInputId: 'inputSearch',
                apiPath: '/api/v1/containers/client'
            });
        }
    } catch (error) {
        console.error('Error loading initial data:', error);
        handleError(error);
    }
});
