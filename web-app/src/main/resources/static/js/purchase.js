const prevPageButton = document.getElementById('prev-btn');
const nextPageButton = document.getElementById('next-btn');
const paginationInfo = document.getElementById('pagination-info');
const allClientInfo = document.getElementById('all-client-info');
const loaderBackdrop = document.getElementById('loader-backdrop');
const filterForm = document.getElementById('filterForm');
const searchInput = document.getElementById('inputSearch');
const tableBody = document.getElementById('client-table-body');
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
const filterButton = document.querySelector('.filter-button-block');
const filterModal = document.getElementById('filterModal');
const closeFilter = document.querySelector('.close-filter');
const modalContent = filterModal ? filterModal.querySelector('.modal-content-filter') : null;
let currentSort = CLIENT_SORT_FIELDS.CREATED_AT;
let currentDirection = CLIENT_SORT_DIRECTIONS.DESC;
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
let availableProducts = [];

let sourceMap;
let userMap;
let productMap;
const currencyTypes = [
    {id: "UAH", name: "UAH"},
    {id: "USD", name: "USD"},
    {id: "EUR", name: "EUR"}
];

let availableCurrencies = currencyTypes;
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




function showEditModal(purchase) {
    PurchaseEditModal.showEditModal(purchase, {
        productMap: productMap,
        sourceMap: sourceMap
    });
}

async function deletePurchase(id, isReceived) {
    if (isReceived === true) {
        if (typeof showMessage === 'function') {
            showMessage('Неможливо видалити закупку, оскільки товар вже прийнято кладовщиком.', 'error');
        } else {
            alert('Неможливо видалити закупку, оскільки товар вже прийнято кладовщиком.');
        }
        return;
    }
    
    if (confirm("Ви впевнені, що хочете видалити цей запис?")) {
        if (loaderBackdrop) {
            loaderBackdrop.style.display = 'flex';
        }
        try {
            await PurchaseDataLoader.deletePurchase(id);
            if (typeof showMessage === 'function') {
                showMessage("Збір успішно видалено.", 'info');
            }
            loadDataWithSort(0, CLIENT_CONSTANTS.DEFAULT_PAGE_SIZE * 2, currentSort, currentDirection);
        } catch (error) {
            console.error('Error:', error);
            handleError(error);
        } finally {
            if (loaderBackdrop) {
                loaderBackdrop.style.display = 'none';
            }
        }
    }
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

    const convertedFilters = PurchaseFilters.convertFieldNamesToFieldIds(filters, filterableFields, clientTypeFields);
    
    const params = {
        page: page,
        size: size,
        sort: sort,
        direction: direction
    };
    
    if (searchTerm) {
        params.q = searchTerm;
    }
    
    if (Object.keys(convertedFilters).length > 0) {
        params.filters = JSON.stringify(convertedFilters);
    }

    try {
        const data = await PurchaseDataLoader.loadPurchases(params);
        
        PurchaseRenderer.renderPurchases(data.content, {
            tableBody: tableBody,
            userMap: userMap,
            productMap: productMap,
            sourceMap: sourceMap,
            loadClientDetailsFn: loadClientDetails,
            showEditModalFn: showEditModal,
            deletePurchaseFn: deletePurchase,
            currentClientTypeId: currentClientTypeId,
            applyColumnWidthsFn: typeof applyColumnWidthsForPurchase === 'function' ? applyColumnWidthsForPurchase : null
        });
        
        PurchaseRenderer.setupSortHandlers(currentSort, currentDirection, (newSort, newDirection) => {
            currentSort = newSort;
            currentDirection = newDirection;
            currentPage = 0;
            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        });

        PurchaseRenderer.updatePagination(data.totalElements, data.content.length, data.totalPages, currentPage, prevPageButton, nextPageButton, allClientInfo, paginationInfo);
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
    PurchaseFilters.buildDynamicFilters({
        filterForm: filterForm,
        customSelects: customSelects,
        selectedFilters: selectedFilters,
        filterableFields: filterableFields,
        availableSources: availableSources,
        availableUsers: availableUsers,
        availableProducts: availableProducts,
        availableCurrencies: availableCurrencies
    });
}


async function loadEntitiesAndApplyFilters() {
    try {
        const data = await PurchaseDataLoader.loadEntities();
        if (!data) return;
        
        availableSources = data.sources || [];
        availableUsers = data.users || [];
        availableProducts = data.products || [];
        
        sourceMap = new Map(availableSources.map(item => [item.id, item.name]));
        userMap = new Map(availableUsers.map(item => [item.id, item.name]));
        productMap = new Map(availableProducts.map(item => [item.id, item.name]));

        PurchaseFilters.restoreFilterValues({
            filterForm: filterForm,
            selectedFilters: selectedFilters,
            customSelects: customSelects,
            filterableFields: filterableFields,
            availableSources: availableSources,
            availableUsers: availableUsers,
            availableProducts: availableProducts,
            availableCurrencies: availableCurrencies
        });
    } catch (error) {
        console.error('Error loading entities:', error);
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
        clientModal: clientModal,
        closeModalClientBtn: closeModalClientBtn,
        modalClientId: modalClientId,
        modalClientCompany: modalClientCompany,
        modalClientSource: modalClientSource,
        modalClientCreated: modalClientCreated,
        modalClientUpdated: modalClientUpdated,
        fullDeleteButton: fullDeleteButton,
        deleteButton: deleteButton,
        restoreButton: restoreButton,
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
        editingState: editingState,
        deleteClientFn: PurchaseDataLoader.deleteClient,
        deleteClientActiveFn: PurchaseDataLoader.deleteClientActive,
        restoreClientFn: PurchaseDataLoader.restoreClient
    });
}







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

/*--filter--*/

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
    PurchaseFilters.updateSelectedFilters({
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
        const clientTypeSelectionModal = document.getElementById('clientTypeSelectionModal');
        const clientTypesSelectionList = document.getElementById('client-types-selection-list');
        await PurchaseTypeManager.showClientTypeSelectionModal(clientTypeSelectionModal, clientTypesSelectionList);
        return;
    }
    
    const newClientTypeId = parseInt(typeId);
    const savedClientTypeId = localStorage.getItem('currentClientTypeId');
    const staticFilterKeys = ['createdAtFrom', 'createdAtTo', 'updatedAtFrom', 'updatedAtTo', 'source', 'showInactive', 'user', 'product', 'quantityFrom', 'quantityTo', 'unitPriceFrom', 'unitPriceTo', 'totalPriceFrom', 'totalPriceTo', 'paymentMethod', 'currency', 'clientCreatedAtFrom', 'clientCreatedAtTo', 'clientUpdatedAtFrom', 'clientUpdatedAtTo', 'clientSource'];
    
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
    
    await PurchaseTypeManager.updateNavigationWithCurrentType(newClientTypeId);
    
    try {
        currentClientType = await PurchaseDataLoader.loadClientType(currentClientTypeId);
        document.title = currentClientType.name;
        const fieldsData = await PurchaseDataLoader.loadClientTypeFields(currentClientTypeId);
        clientTypeFields = fieldsData.all || [];
        filterableFields = fieldsData.filterable || [];
    } catch (error) {
        console.error('Error loading client type data:', error);
        handleError(error);
        return;
    }
    
    PurchaseRenderer.buildPurchaseTable();
    buildDynamicFilters();
    
    if (typeof initColumnResizerForPurchase === 'function' && currentClientTypeId) {
        setTimeout(() => {
            const storageKey = `purchase_${currentClientTypeId}`;
            initColumnResizerForPurchase('client-list', storageKey);
            if (typeof applyColumnWidthsForPurchase === 'function') {
                applyColumnWidthsForPurchase('client-list', storageKey);
            }
        }, 0);
    }
    
    const validFieldNames = new Set(filterableFields.map(f => f.fieldName));
    const cleanedFilters = ClientUtils.normalizeFilterKeys(selectedFilters, staticFilterKeys, validFieldNames);
    Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);
    Object.assign(selectedFilters, cleanedFilters);
    if (Object.keys(cleanedFilters).length === 0) {
        localStorage.removeItem('selectedFilters');
    } else {
        localStorage.setItem('selectedFilters', JSON.stringify(cleanedFilters));
    }
    
    PurchaseFilters.restoreDynamicClientFields({
        filterableFields: filterableFields,
        selectedFilters: selectedFilters,
        customSelects: customSelects
    });
    updateFilterCounter();
    
    await loadEntitiesAndApplyFilters();
    
    PurchaseEditModal.init({
        modalId: 'edit-modal',
        formId: 'edit-form',
        productMap: productMap,
        sourceMap: sourceMap,
        onSaveSuccess: () => {
            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        }
    });
    
    ClientDetailsModals.init();
    
    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
    
    if (typeof initExcelExportPurchase === 'function') {
        initExcelExportPurchase({
            triggerId: 'exportToExcelData',
            modalId: 'exportModalData',
            cancelId: 'exportCancel',
            confirmId: 'exportConfirm',
            formId: 'exportFieldsForm',
            searchInputId: 'inputSearch',
            apiPath: '/api/v1/purchase'
        });
    }
});



