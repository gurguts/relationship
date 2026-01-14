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
        sourceMap: sourceMap
    });
}

async function deletePurchase(id, isReceived) {
    if (isReceived === true) {
        showMessage(CONFIRMATION_MESSAGES.CANNOT_DELETE_PURCHASE, 'error');
        return;
    }
    
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_PURCHASE,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            if (loaderBackdrop) {
        loaderBackdrop.style.display = 'flex';
            }
            try {
                await PurchaseDataLoader.deletePurchase(id);
                showMessage("Збір успішно видалено.", 'info');
                loadDataWithSort(0, CLIENT_CONSTANTS.DEFAULT_PAGE_SIZE * 2, currentSort, currentDirection);
            } catch (error) {
                console.error('Error:', error);
                handleError(error);
            } finally {
                if (loaderBackdrop) {
                loaderBackdrop.style.display = 'none';
                }
            }
        },
        () => {}
    );
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
    
    const normalizedFilters = {};
    Object.keys(convertedFilters).forEach(key => {
        const lowerKey = key.toLowerCase();
        if (lowerKey === 'createdatfrom') {
            normalizedFilters['createdAtFrom'] = convertedFilters[key];
        } else if (lowerKey === 'createdatto') {
            normalizedFilters['createdAtTo'] = convertedFilters[key];
        } else if (lowerKey === 'updatedatfrom') {
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


async function loadPurchaseReport() {
    if (!currentClientTypeId) {
        return;
    }
    
    if (loaderBackdrop) {
        loaderBackdrop.style.display = 'flex';
    }
    
    try {
        const searchTerm = searchInput ? searchInput.value : '';
        const filters = { ...selectedFilters };
        if (currentClientTypeId) {
            filters.clientTypeId = [currentClientTypeId.toString()];
        }
        
        const convertedFilters = PurchaseFilters.convertFieldNamesToFieldIds(filters, filterableFields, clientTypeFields);
        
        const normalizedFilters = {};
        Object.keys(convertedFilters).forEach(key => {
            const lowerKey = key.toLowerCase();
            if (lowerKey === 'createdatfrom') {
                normalizedFilters['createdAtFrom'] = convertedFilters[key];
            } else if (lowerKey === 'createdatto') {
                normalizedFilters['createdAtTo'] = convertedFilters[key];
            } else if (lowerKey === 'updatedatfrom') {
                normalizedFilters['updatedAtFrom'] = convertedFilters[key];
            } else if (lowerKey === 'updatedatto') {
                normalizedFilters['updatedAtTo'] = convertedFilters[key];
            } else {
                normalizedFilters[key] = convertedFilters[key];
            }
        });
        
        const report = await PurchaseDataLoader.loadReport(searchTerm, normalizedFilters);
        
        const purchaseReportModalEl = document.getElementById('purchase-report-modal');
        const purchaseReportContentEl = document.getElementById('purchase-report-content');
        
        renderPurchaseReport(report, purchaseReportContentEl);
        
        if (purchaseReportModalEl) {
            purchaseReportModalEl.style.display = 'flex';
            setTimeout(() => {
                purchaseReportModalEl.classList.add('show');
            }, 10);
            document.body.style.overflow = 'hidden';
        }
    } catch (error) {
        console.error('Error loading report:', error);
        handleError(error);
    } finally {
        if (loaderBackdrop) {
            loaderBackdrop.style.display = 'none';
        }
    }
}

function renderPurchaseReport(report, purchaseReportContent) {
    if (!purchaseReportContent) {
        purchaseReportContent = document.getElementById('purchase-report-content');
    }
    
    if (!purchaseReportContent) {
        return;
    }
    
    purchaseReportContent.innerHTML = '';
    
    if (!report || ((!report.drivers || report.drivers.length === 0) && 
        (!report.sources || report.sources.length === 0) && 
        (!report.totals || report.totals.length === 0))) {
        const emptyMessage = document.createElement('p');
        emptyMessage.textContent = 'Немає даних для відображення';
        emptyMessage.style.textAlign = 'center';
        emptyMessage.style.padding = '20px';
        purchaseReportContent.appendChild(emptyMessage);
        return;
    }
    
    if (report.drivers && report.drivers.length > 0) {
        const driversSection = document.createElement('div');
        driversSection.className = 'report-section';
        
        const driversTitle = document.createElement('h3');
        driversTitle.textContent = 'По водіях';
        driversTitle.className = 'report-section-title';
        driversSection.appendChild(driversTitle);
        
        report.drivers.forEach(driver => {
            const driverCard = document.createElement('div');
            driverCard.className = 'report-driver-card';
            
            const driverName = document.createElement('div');
            driverName.className = 'report-driver-name';
            driverName.textContent = driver.userName;
            driverCard.appendChild(driverName);
            
            if (driver.products && driver.products.length > 0) {
                const productsList = document.createElement('div');
                productsList.className = 'report-products-list';
                
                driver.products.forEach(product => {
                    const productItem = document.createElement('div');
                    productItem.className = 'report-product-item';
                    
                    const productName = document.createElement('span');
                    productName.className = 'report-product-name';
                    productName.textContent = product.productName;
                    
                    const productQuantity = document.createElement('span');
                    productQuantity.className = 'report-product-quantity';
                    productQuantity.textContent = product.quantity ? parseFloat(product.quantity).toFixed(2) : '0.00';
                    
                    const productPrice = document.createElement('span');
                    productPrice.className = 'report-product-price';
                    productPrice.textContent = product.totalPriceEur ? parseFloat(product.totalPriceEur).toFixed(2) + ' EUR' : '0.00 EUR';
                    
                    productItem.appendChild(productName);
                    productItem.appendChild(productQuantity);
                    productItem.appendChild(productPrice);
                    productsList.appendChild(productItem);
                });
                
                driverCard.appendChild(productsList);
            }
            
            driversSection.appendChild(driverCard);
        });
        
        purchaseReportContent.appendChild(driversSection);
    }
    
    if (report.sources && report.sources.length > 0) {
        const sourcesSection = document.createElement('div');
        sourcesSection.className = 'report-section';
        
        const sourcesTitle = document.createElement('h3');
        sourcesTitle.textContent = 'По залученнях';
        sourcesTitle.className = 'report-section-title';
        sourcesSection.appendChild(sourcesTitle);
        
        report.sources.forEach(source => {
            const sourceCard = document.createElement('div');
            sourceCard.className = 'report-source-card';
            
            const sourceName = document.createElement('div');
            sourceName.className = 'report-source-name';
            sourceName.textContent = source.sourceName;
            sourceCard.appendChild(sourceName);
            
            if (source.products && source.products.length > 0) {
                const productsList = document.createElement('div');
                productsList.className = 'report-products-list';
                
                source.products.forEach(product => {
                    const productItem = document.createElement('div');
                    productItem.className = 'report-product-item';
                    
                    const productName = document.createElement('span');
                    productName.className = 'report-product-name';
                    productName.textContent = product.productName;
                    
                    const productQuantity = document.createElement('span');
                    productQuantity.className = 'report-product-quantity';
                    productQuantity.textContent = product.quantity ? parseFloat(product.quantity).toFixed(2) : '0.00';
                    
                    const productPrice = document.createElement('span');
                    productPrice.className = 'report-product-price';
                    productPrice.textContent = product.totalPriceEur ? parseFloat(product.totalPriceEur).toFixed(2) + ' EUR' : '0.00 EUR';
                    
                    productItem.appendChild(productName);
                    productItem.appendChild(productQuantity);
                    productItem.appendChild(productPrice);
                    productsList.appendChild(productItem);
                });
                
                sourceCard.appendChild(productsList);
            }
            
            sourcesSection.appendChild(sourceCard);
        });
        
        purchaseReportContent.appendChild(sourcesSection);
    }
    
    if (report.totals && report.totals.length > 0) {
        const totalsSection = document.createElement('div');
        totalsSection.className = 'report-section';
        
        const totalsTitle = document.createElement('h3');
        totalsTitle.textContent = 'Загальна кількість';
        totalsTitle.className = 'report-section-title';
        totalsSection.appendChild(totalsTitle);
        
        const totalsList = document.createElement('div');
        totalsList.className = 'report-products-list report-totals-list';
        
        report.totals.forEach(total => {
            const totalItem = document.createElement('div');
            totalItem.className = 'report-product-item report-total-item';
            
            const productName = document.createElement('span');
            productName.className = 'report-product-name';
            productName.textContent = total.productName;
            
            const productQuantity = document.createElement('span');
            productQuantity.className = 'report-product-quantity';
            productQuantity.textContent = total.quantity ? parseFloat(total.quantity).toFixed(2) : '0.00';
            
            const productPrice = document.createElement('span');
            productPrice.className = 'report-product-price';
            productPrice.textContent = total.totalPriceEur ? parseFloat(total.totalPriceEur).toFixed(2) + ' EUR' : '0.00 EUR';
            
            totalItem.appendChild(productName);
            totalItem.appendChild(productQuantity);
            totalItem.appendChild(productPrice);
            totalsList.appendChild(totalItem);
        });
        
        totalsSection.appendChild(totalsList);
        purchaseReportContent.appendChild(totalsSection);
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
        
        window.sourceMap = sourceMap;
        window.userMap = userMap;
        window.productMap = productMap;

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
    const purchaseReportBtn = document.getElementById('purchase-report-btn');
    const purchaseReportModal = document.getElementById('purchase-report-modal');
    const closePurchaseReportModal = document.getElementById('close-purchase-report-modal');
    const purchaseReportContent = document.getElementById('purchase-report-content');
    
    if (purchaseReportBtn) {
        purchaseReportBtn.addEventListener('click', async (e) => {
            e.preventDefault();
            e.stopPropagation();
            await loadPurchaseReport();
        });
    }

    if (closePurchaseReportModal) {
        closePurchaseReportModal.addEventListener('click', () => {
            if (purchaseReportModal) {
                purchaseReportModal.classList.remove('show');
                setTimeout(() => {
                    purchaseReportModal.style.display = 'none';
                }, 300);
                document.body.style.overflow = '';
            }
        });
    }

    if (purchaseReportModal) {
        const modalContent = purchaseReportModal.querySelector('.modal-content');
        if (modalContent) {
            modalContent.addEventListener('click', (e) => {
                e.stopPropagation();
            });
        }
    }
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
    
    try {
        currentClientType = await PurchaseDataLoader.loadClientType(currentClientTypeId);
        await PurchaseTypeManager.updateNavigationWithCurrentType(newClientTypeId, currentClientType);
        document.title = currentClientType.name;
        const fieldsData = await PurchaseDataLoader.loadClientTypeFields(currentClientTypeId);
        clientTypeFields = fieldsData.all || [];
        window.clientTypeFields = clientTypeFields;
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
    
    const hasCreatedAtFrom = Object.keys(selectedFilters).some(key => 
        key.toLowerCase() === 'createdatfrom'
    );
    
    if (!hasCreatedAtFrom) {
        const oneMonthAgo = new Date();
        oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1);
        const year = oneMonthAgo.getFullYear();
        const month = String(oneMonthAgo.getMonth() + 1).padStart(2, '0');
        const day = String(oneMonthAgo.getDate()).padStart(2, '0');
        selectedFilters.createdatfrom = [`${year}-${month}-${day}`];
    }
    
    if (Object.keys(selectedFilters).length === 0) {
        localStorage.removeItem('selectedFilters');
    } else {
        localStorage.setItem('selectedFilters', JSON.stringify(selectedFilters));
    }
    
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



