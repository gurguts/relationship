const escapeHtml = DeclarantUtils.escapeHtml;
const formatNumber = DeclarantUtils.formatNumber;
const formatDate = DeclarantUtils.formatDate;
const formatBoolean = DeclarantUtils.formatBoolean;
const findNameByIdFromMap = DeclarantUtils.findNameByIdFromMap;

let productMap = new Map();
let warehouseMap = new Map();
let carrierMap = new Map();
let vehicleSenderMap = new Map();
let vehicleReceiverMap = new Map();
let vehicleTerminalMap = new Map();
let vehicleDestinationCountryMap = new Map();
let vehicleDestinationPlaceMap = new Map();

async function fetchProducts() {
    try {
        const products = await DeclarantDataLoader.fetchProducts();
        productMap = new Map(products.map(product => [product.id, product.name]));
    } catch (error) {
        console.error('Error fetching products:', error);
        handleError(error);
    }
}

async function fetchWarehouses() {
    try {
        const warehouses = await DeclarantDataLoader.fetchWarehouses();
        warehouseMap = new Map(warehouses.map(warehouse => [warehouse.id, warehouse.name]));
    } catch (error) {
        console.error('Error fetching warehouses:', error);
        handleError(error);
    }
}

async function fetchCarriers() {
    try {
        const carriers = await DeclarantDataLoader.fetchCarriers();
        carrierMap = new Map(carriers.map(carrier => [carrier.id, carrier]));
        return carriers;
    } catch (error) {
        console.error('Error fetching carriers:', error);
        handleError(error);
        return [];
    }
}

async function fetchVehicleSenders() {
    try {
        const senders = await DeclarantDataLoader.fetchVehicleSenders();
        vehicleSenderMap = new Map(senders.map(sender => [sender.id, sender]));
        return senders;
    } catch (error) {
        console.error('Error fetching vehicle senders:', error);
        handleError(error);
        return [];
    }
}

async function fetchVehicleReceivers() {
    try {
        const receivers = await DeclarantDataLoader.fetchVehicleReceivers();
        vehicleReceiverMap = new Map(receivers.map(receiver => [receiver.id, receiver]));
        return receivers;
    } catch (error) {
        console.error('Error fetching vehicle receivers:', error);
        handleError(error);
        return [];
    }
}

async function fetchVehicleTerminals() {
    try {
        const terminals = await DeclarantDataLoader.fetchVehicleTerminals();
        vehicleTerminalMap = new Map(terminals.map(terminal => [terminal.id, terminal]));
        return terminals;
    } catch (error) {
        console.error('Error fetching vehicle terminals:', error);
        handleError(error);
        return [];
    }
}

async function fetchVehicleDestinationCountries() {
    try {
        const countries = await DeclarantDataLoader.fetchVehicleDestinationCountries();
        vehicleDestinationCountryMap = new Map(countries.map(country => [country.id, country]));
        return countries;
    } catch (error) {
        console.error('Error fetching vehicle destination countries:', error);
        handleError(error);
        return [];
    }
}

async function fetchVehicleDestinationPlaces() {
    try {
        const places = await DeclarantDataLoader.fetchVehicleDestinationPlaces();
        vehicleDestinationPlaceMap = new Map(places.map(place => [place.id, place]));
        return places;
    } catch (error) {
        console.error('Error fetching vehicle destination places:', error);
        handleError(error);
        return [];
    }
}

const populateCarriers = (selectId) => DeclarantRenderer.populateCarriers(selectId, carrierMap);
const populateVehicleSenders = (selectId) => DeclarantRenderer.populateVehicleSenders(selectId, vehicleSenderMap);
const populateVehicleReceivers = (selectId) => DeclarantRenderer.populateVehicleReceivers(selectId, vehicleReceiverMap);
const populateVehicleTerminals = (selectId) => DeclarantRenderer.populateVehicleTerminals(selectId, vehicleTerminalMap);
const populateVehicleDestinationCountries = (selectId) => DeclarantRenderer.populateVehicleDestinationCountries(selectId, vehicleDestinationCountryMap);
const populateVehicleDestinationPlaces = (selectId) => DeclarantRenderer.populateVehicleDestinationPlaces(selectId, vehicleDestinationPlaceMap);

const createVehicleBtn = document.getElementById('create-vehicle-btn');
const createVehicleForm = document.getElementById('create-vehicle-form');
const updateVehicleForm = document.getElementById('update-vehicle-form');
const detailVehicleDateInput = document.getElementById('detail-vehicle-date');
const detailVehicleVehicleInput = document.getElementById('detail-vehicle-vehicle-number');
const detailVehicleInvoiceUaInput = document.getElementById('detail-vehicle-invoice-ua');
const detailVehicleInvoiceEuInput = document.getElementById('detail-vehicle-invoice-eu');
const detailVehicleInvoiceUaDateInput = document.getElementById('detail-vehicle-invoice-ua-date');
const detailVehicleInvoiceUaPricePerTonInput = document.getElementById('detail-vehicle-invoice-ua-price-per-ton');
const detailVehicleInvoiceUaTotalPriceInput = document.getElementById('detail-vehicle-invoice-ua-total-price');
const detailVehicleInvoiceEuDateInput = document.getElementById('detail-vehicle-invoice-eu-date');
const detailVehicleInvoiceEuPricePerTonInput = document.getElementById('detail-vehicle-invoice-eu-price-per-ton');
const detailVehicleInvoiceEuTotalPriceInput = document.getElementById('detail-vehicle-invoice-eu-total-price');
const detailVehicleReclamationInput = document.getElementById('detail-vehicle-reclamation');
const detailVehicleFullReclamationInput = document.getElementById('detail-vehicle-full-reclamation');
const detailVehicleDescriptionInput = document.getElementById('detail-vehicle-description');
const detailVehicleDestinationCountryInput = document.getElementById('detail-vehicle-destination-country');
const detailVehicleDestinationPlaceInput = document.getElementById('detail-vehicle-destination-place');
const detailVehicleProductInput = document.getElementById('detail-vehicle-product');
const detailVehicleProductQuantityInput = document.getElementById('detail-vehicle-product-quantity');
const detailVehicleDeclarationNumberInput = document.getElementById('detail-vehicle-declaration-number');
const detailVehicleTerminalInput = document.getElementById('detail-vehicle-terminal');
const detailVehicleDriverFullNameInput = document.getElementById('detail-vehicle-driver-full-name');
const detailVehicleIsOurVehicleInput = document.getElementById('detail-vehicle-is-our-vehicle');
const detailVehicleEur1Input = document.getElementById('detail-vehicle-eur1');
const detailVehicleFitoInput = document.getElementById('detail-vehicle-fito');
const detailVehicleCustomsDateInput = document.getElementById('detail-vehicle-customs-date');
const detailVehicleCustomsClearanceDateInput = document.getElementById('detail-vehicle-customs-clearance-date');
const detailVehicleUnloadingDateInput = document.getElementById('detail-vehicle-unloading-date');
const detailVehicleCarrierSelect = document.getElementById('detail-vehicle-carrier-id');
const detailVehicleSenderSelect = document.getElementById('detail-vehicle-sender');
const detailVehicleReceiverSelect = document.getElementById('detail-vehicle-receiver');
const editVehicleBtn = document.getElementById('edit-vehicle-btn');
const saveVehicleBtn = document.getElementById('save-vehicle-btn');
const editVehicleItemModal = document.getElementById('edit-vehicle-item-modal');
const editVehicleItemForm = document.getElementById('edit-vehicle-item-form');
const editVehicleItemQuantityInput = document.getElementById('edit-vehicle-item-quantity');
const editVehicleItemTotalCostInput = document.getElementById('edit-vehicle-item-total-cost');
const editVehicleItemModeRadios = document.querySelectorAll('input[name="edit-vehicle-item-mode"]');

const vehiclesTbody = document.getElementById('vehicles-tbody');
const vehiclesCount = document.getElementById('vehicles-count');
const vehiclesTable = document.getElementById('vehicles-table');
const vehiclesPagination = document.getElementById('vehicles-pagination');
const vehiclesSearchInput = document.getElementById('vehicles-search-input');

const vehicleVehicleNumber = document.getElementById('vehicle-vehicle-number');
const vehicleInvoiceUa = document.getElementById('vehicle-invoice-ua');
const vehicleInvoiceEu = document.getElementById('vehicle-invoice-eu');
const vehicleInvoiceUaDate = document.getElementById('vehicle-invoice-ua-date');
const vehicleInvoiceUaPricePerTon = document.getElementById('vehicle-invoice-ua-price-per-ton');
const vehicleInvoiceUaTotalPrice = document.getElementById('vehicle-invoice-ua-total-price');
const vehicleInvoiceEuDate = document.getElementById('vehicle-invoice-eu-date');
const vehicleInvoiceEuPricePerTon = document.getElementById('vehicle-invoice-eu-price-per-ton');
const vehicleInvoiceEuTotalPrice = document.getElementById('vehicle-invoice-eu-total-price');
const vehicleReclamation = document.getElementById('vehicle-reclamation');
const vehicleDescription = document.getElementById('vehicle-description');
const vehicleSenderSelect = document.getElementById('vehicle-sender');
const vehicleReceiverSelect = document.getElementById('vehicle-receiver');
const vehicleDestinationCountrySelect = document.getElementById('vehicle-destination-country');
const vehicleDestinationPlaceSelect = document.getElementById('vehicle-destination-place');
const vehicleProduct = document.getElementById('vehicle-product');
const vehicleProductQuantity = document.getElementById('vehicle-product-quantity');
const vehicleDeclarationNumber = document.getElementById('vehicle-declaration-number');
const vehicleTerminalSelect = document.getElementById('vehicle-terminal');
const vehicleDriverFullName = document.getElementById('vehicle-driver-full-name');
const vehicleEur1 = document.getElementById('vehicle-eur1');
const vehicleFito = document.getElementById('vehicle-fito');
const vehicleCustomsDate = document.getElementById('vehicle-customs-date');
const vehicleCustomsClearanceDate = document.getElementById('vehicle-customs-clearance-date');
const vehicleUnloadingDate = document.getElementById('vehicle-unloading-date');
const vehicleIsOurVehicle = document.getElementById('vehicle-is-our-vehicle');
const vehicleCarrierId = document.getElementById('vehicle-carrier-id');

const vehiclesDateFromFilter = document.getElementById('vehicles-date-from-filter');
const vehiclesDateToFilter = document.getElementById('vehicles-date-to-filter');
const vehiclesCustomsDateFromFilter = document.getElementById('vehicles-customs-date-from-filter');
const vehiclesCustomsDateToFilter = document.getElementById('vehicles-customs-date-to-filter');
const vehiclesCustomsClearanceDateFromFilter = document.getElementById('vehicles-customs-clearance-date-from-filter');
const vehiclesCustomsClearanceDateToFilter = document.getElementById('vehicles-customs-clearance-date-to-filter');
const vehiclesUnloadingDateFromFilter = document.getElementById('vehicles-unloading-date-from-filter');
const vehiclesUnloadingDateToFilter = document.getElementById('vehicles-unloading-date-to-filter');
const vehiclesIsOurVehicleFilter = document.getElementById('vehicles-is-our-vehicle-filter');

const vehicleItemsTbody = document.getElementById('vehicle-items-tbody');
const vehicleTotalCost = document.getElementById('vehicle-total-cost');
const vehicleTotalExpenses = document.getElementById('vehicle-total-expenses');
const vehicleTotalIncome = document.getElementById('vehicle-total-income');
const vehicleMargin = document.getElementById('vehicle-margin');


const expenseFromAccount = document.getElementById('expense-from-account');
const expenseCategory = document.getElementById('expense-category');
const expenseAmount = document.getElementById('expense-amount');
const expenseCurrency = document.getElementById('expense-currency');
const expenseDescription = document.getElementById('expense-description');

const editExpenseFromAccount = document.getElementById('edit-expense-from-account');
const editExpenseCategory = document.getElementById('edit-expense-category');
const editExpenseAmount = document.getElementById('edit-expense-amount');
const editExpenseCurrency = document.getElementById('edit-expense-currency');
const editExpenseDescription = document.getElementById('edit-expense-description');
const editVehicleExpenseForm = document.getElementById('edit-vehicle-expense-form');

let currentExpenseId = null;
const createVehicleExpenseForm = document.getElementById('create-vehicle-expense-form');
const vehicleExpensesTbody = document.getElementById('vehicle-expenses-tbody');

let currentVehicleId = null;
let vehiclesCache = [];
let currentVehicleDetails = null;
let currentPage = 0;
const pageSize = CLIENT_CONSTANTS.DEFAULT_PAGE_SIZE;
let totalPages = 0;
let totalElements = 0;
let currentVehicleItems = new Map();
let currentVehicleItemId = null;

const populateVehicleForm = (vehicle) => DeclarantModal.populateVehicleForm(vehicle);
const setVehicleFormEditable = DeclarantModal.setVehicleFormEditable;
const resetVehicleFormState = () => DeclarantModal.resetVehicleFormState(currentVehicleDetails);

if (updateVehicleForm) {
    setVehicleFormEditable(false);
}

if (editVehicleBtn) {
    editVehicleBtn.addEventListener('click', () => {
        if (!currentVehicleDetails) {
            return;
        }
        populateVehicleForm(currentVehicleDetails);
        setVehicleFormEditable(true);
        detailVehicleDateInput?.focus();
    });
}

if (createVehicleBtn) {
    createVehicleBtn.addEventListener('click', () => {
        populateCarriers('vehicle-carrier-id');
        populateVehicleSenders('vehicle-sender');
        populateVehicleReceivers('vehicle-receiver');
        populateVehicleTerminals('vehicle-terminal');
        populateVehicleDestinationCountries('vehicle-destination-country');
        populateVehicleDestinationPlaces('vehicle-destination-place');
        openModal('create-vehicle-modal');
    });
}

if (createVehicleForm) {
    createVehicleForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const carrierIdValue = vehicleCarrierId?.value;
        const vehicleData = {
            vehicleNumber: vehicleVehicleNumber?.value || '',
            invoiceUa: vehicleInvoiceUa?.value || '',
            invoiceEu: vehicleInvoiceEu?.value || '',
            invoiceUaDate: vehicleInvoiceUaDate?.value || null,
            invoiceUaPricePerTon: vehicleInvoiceUaPricePerTon?.value ? parseFloat(vehicleInvoiceUaPricePerTon.value) : null,
            invoiceEuDate: vehicleInvoiceEuDate?.value || null,
            invoiceEuPricePerTon: vehicleInvoiceEuPricePerTon?.value ? parseFloat(vehicleInvoiceEuPricePerTon.value) : null,
            reclamation: vehicleReclamation?.value ? parseFloat(vehicleReclamation.value) : null,
            description: vehicleDescription?.value || '',
            senderId: vehicleSenderSelect?.value ? parseInt(vehicleSenderSelect.value) : null,
            receiverId: vehicleReceiverSelect?.value ? parseInt(vehicleReceiverSelect.value) : null,
            destinationCountryId: vehicleDestinationCountrySelect?.value ? parseInt(vehicleDestinationCountrySelect.value) : null,
            destinationPlaceId: vehicleDestinationPlaceSelect?.value ? parseInt(vehicleDestinationPlaceSelect.value) : null,
            product: vehicleProduct?.value || '',
            productQuantity: vehicleProductQuantity?.value || '',
            declarationNumber: vehicleDeclarationNumber?.value || '',
            terminalId: vehicleTerminalSelect?.value ? parseInt(vehicleTerminalSelect.value) : null,
            driverFullName: vehicleDriverFullName?.value || '',
            eur1: vehicleEur1?.checked || false,
            fito: vehicleFito?.checked || false,
            customsDate: vehicleCustomsDate?.value || null,
            customsClearanceDate: vehicleCustomsClearanceDate?.value || null,
            unloadingDate: vehicleUnloadingDate?.value || null,
            carrierId: carrierIdValue ? Number(carrierIdValue) : null,
            isOurVehicle: vehicleIsOurVehicle?.checked || false
        };
        
        try {
            await DeclarantDataLoader.createVehicle(vehicleData);
            showMessage('Машину успішно створено', 'success');
            
            closeModal('create-vehicle-modal');
            createVehicleForm?.reset();
            
            await loadVehicles(0);
        } catch (error) {
            showMessage(error.message || 'Помилка при створенні машини', 'error');
        }
    });
}


const buildFilters = DeclarantFilters.buildFilters;

async function loadVehicles(page = 0) {
    currentPage = page;
    
    const vehiclesSearchInput = document.getElementById('vehicles-search-input');
    const searchTerm = vehiclesSearchInput?.value || '';
    const filters = buildFilters();
    const filtersJson = Object.keys(filters).length > 0 ? JSON.stringify(filters) : '';
    
    try {
        const data = await DeclarantDataLoader.loadVehicles(page, pageSize, 'id', 'DESC', searchTerm, filtersJson);
        vehiclesCache = data.content || [];
        totalPages = data.totalPages || 0;
        totalElements = data.totalElements || 0;
        
        await renderVehicles(vehiclesCache, currentPage, pageSize, totalElements, totalPages, productMap, warehouseMap, carrierMap, viewVehicleDetails);
        renderPagination();
    } catch (error) {
        console.error('Error loading vehicles:', error);
        showMessage('Помилка завантаження машин', 'error');
        
        const vehiclesTbody = document.getElementById('vehicles-tbody');
        if (vehiclesTbody) {
            vehiclesTbody.textContent = '';
            const row = document.createElement('tr');
            row.className = 'loading-row';
            const cell = document.createElement('td');
            cell.colSpan = 26;
            cell.style.textAlign = 'center';
            cell.style.color = 'var(--text-muted)';
            cell.textContent = CLIENT_MESSAGES.LOAD_ERROR;
            row.appendChild(cell);
            vehiclesTbody.appendChild(row);
        }
    }
}

function renderPagination() {
    DeclarantRenderer.renderPagination(currentPage, totalPages, loadVehicles);
}

const formatCarrier = DeclarantRenderer.formatCarrier;

async function renderVehicles(vehicles, currentPage, pageSize, totalElements, totalPages, productMap, warehouseMap, carrierMap, onVehicleClick) {
    await DeclarantRenderer.renderVehicles(vehicles, currentPage, pageSize, totalElements, totalPages, productMap, warehouseMap, carrierMap, onVehicleClick);
    applySavedColumnWidths();
    initializeColumnResize();
}

function applySavedColumnWidths() {
    if (!vehiclesTable) return;
    
    // Don't apply column widths on mobile devices
    if (window.innerWidth <= 1024) {
        return;
    }
    
    const headers = vehiclesTable.querySelectorAll('.resizable-header');
    headers.forEach(header => {
        const column = header.dataset.column;
        const savedWidth = localStorage.getItem(`vehicle-column-width-${column}`);
        if (savedWidth) {
            const width = parseInt(savedWidth);
            header.style.minWidth = width + 'px';
            header.style.width = width + 'px';
            const index = Array.from(header.parentElement.children).indexOf(header);
            const cells = vehiclesTable.querySelectorAll(`tbody tr td:nth-child(${index + 1})`);
            cells.forEach(cell => {
                cell.style.minWidth = width + 'px';
                cell.style.width = width + 'px';
            });
        } else {
            header.style.width = 'auto';
            header.style.minWidth = 'fit-content';
        }
    });
}

async function viewVehicleDetails(vehicleId) {
    currentVehicleId = vehicleId;
    
    try {
        const vehicle = await DeclarantDataLoader.loadVehicleDetails(vehicleId);
        populateCarriers('detail-vehicle-carrier-id');
        populateVehicleSenders('detail-vehicle-sender');
        populateVehicleReceivers('detail-vehicle-receiver');
        populateVehicleTerminals('detail-vehicle-terminal');
        populateVehicleDestinationCountries('detail-vehicle-destination-country');
        populateVehicleDestinationPlaces('detail-vehicle-destination-place');
        renderVehicleDetails(vehicle);
        
        document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
        document.querySelector('.tab-btn[data-tab="info"]')?.classList.add('active');
        const tabInfo = document.getElementById('tab-info');
        if (tabInfo) {
            tabInfo.classList.add('active');
        }
        
        await loadVehicleExpenses(vehicleId);
        
        const expensesTotal = parseFloat(await getVehicleExpensesTotal(vehicleId)) || 0;
        const productsTotalCost = calculateProductsTotalCost(vehicle);
        const totalExpenses = productsTotalCost + expensesTotal;
        
        const invoiceEuTotalPrice = vehicle.invoiceEuTotalPrice || 0;
        const fullReclamation = calculateFullReclamation(vehicle);
        const totalIncome = invoiceEuTotalPrice - fullReclamation;
        const margin = totalIncome - totalExpenses;
        
        const vehicleTotalExpenses = document.getElementById('vehicle-total-expenses');
        const vehicleTotalIncome = document.getElementById('vehicle-total-income');
        const vehicleMargin = document.getElementById('vehicle-margin');
        
        if (vehicleTotalExpenses) {
            vehicleTotalExpenses.textContent = formatNumber(totalExpenses, 2);
        }
        if (vehicleTotalIncome) {
            vehicleTotalIncome.textContent = formatNumber(totalIncome, 2);
        }
        if (vehicleMargin) {
            vehicleMargin.textContent = formatNumber(margin, 2);
        }
        
        openModal('vehicle-details-modal');
    } catch (error) {
        showMessage('Помилка завантаження деталей машини', 'error');
    }
}

function renderVehicleDetails(vehicle) {
    currentVehicleDetails = vehicle;
    currentVehicleItems = new Map();
    populateVehicleForm(vehicle);
    setVehicleFormEditable(false);
    
    DeclarantRenderer.renderVehicleDetails(vehicle, productMap, warehouseMap, currentVehicleItems);
}

const getVehicleExpensesTotal = (vehicleId) => DeclarantCalculations.getVehicleExpensesTotal(vehicleId);
const calculateProductsTotalCost = DeclarantCalculations.calculateProductsTotalCost;
const calculateFullReclamation = DeclarantCalculations.calculateFullReclamation;
const calculateInvoiceUaTotalPrice = DeclarantCalculations.calculateInvoiceUaTotalPrice;
const calculateInvoiceEuTotalPrice = DeclarantCalculations.calculateInvoiceEuTotalPrice;
const calculateDetailInvoiceUaTotalPrice = DeclarantCalculations.calculateDetailInvoiceUaTotalPrice;
const calculateDetailInvoiceEuTotalPrice = DeclarantCalculations.calculateDetailInvoiceEuTotalPrice;

if (vehicleProductQuantity) {
    vehicleProductQuantity.addEventListener('input', () => {
        calculateInvoiceUaTotalPrice();
        calculateInvoiceEuTotalPrice();
    });
}

if (vehicleInvoiceUaPricePerTon) {
    vehicleInvoiceUaPricePerTon.addEventListener('input', calculateInvoiceUaTotalPrice);
}

if (vehicleInvoiceEuPricePerTon) {
    vehicleInvoiceEuPricePerTon.addEventListener('input', calculateInvoiceEuTotalPrice);
}

if (detailVehicleProductQuantityInput) {
    detailVehicleProductQuantityInput.addEventListener('input', () => {
        calculateDetailInvoiceUaTotalPrice();
        calculateDetailInvoiceEuTotalPrice();
    });
}

if (detailVehicleInvoiceUaPricePerTonInput) {
    detailVehicleInvoiceUaPricePerTonInput.addEventListener('input', calculateDetailInvoiceUaTotalPrice);
}

if (detailVehicleInvoiceEuPricePerTonInput) {
    detailVehicleInvoiceEuPricePerTonInput.addEventListener('input', calculateDetailInvoiceEuTotalPrice);
}


function deleteVehicle() {
    if (!currentVehicleId) {
        showMessage('Не вдалося визначити машину для видалення', 'error');
        return;
    }
    
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_VEHICLE,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await DeclarantDataLoader.deleteVehicle(currentVehicleId);
                showMessage('Машину успішно видалено', 'success');
                closeModal('vehicle-details-modal');
                await loadVehicles(0);
            } catch (error) {
                showMessage('Помилка при видаленні машини', 'error');
                handleError(error);
            }
        },
        () => {}
    );
}

document.getElementById('delete-vehicle-btn')?.addEventListener('click', deleteVehicle);
document.getElementById('delete-vehicle-from-details-btn')?.addEventListener('click', deleteVehicle);

document.getElementById('apply-vehicles-filters')?.addEventListener('click', async () => {
    await loadVehicles(0);
    closeModal('vehicles-filter-modal');
});

document.getElementById('clear-vehicles-filters')?.addEventListener('click', () => {
    if (vehiclesDateFromFilter) vehiclesDateFromFilter.value = '';
    if (vehiclesDateToFilter) vehiclesDateToFilter.value = '';
    if (vehiclesCustomsDateFromFilter) vehiclesCustomsDateFromFilter.value = '';
    if (vehiclesCustomsDateToFilter) vehiclesCustomsDateToFilter.value = '';
    if (vehiclesCustomsClearanceDateFromFilter) vehiclesCustomsClearanceDateFromFilter.value = '';
    if (vehiclesCustomsClearanceDateToFilter) vehiclesCustomsClearanceDateToFilter.value = '';
    if (vehiclesUnloadingDateFromFilter) vehiclesUnloadingDateFromFilter.value = '';
    if (vehiclesUnloadingDateToFilter) vehiclesUnloadingDateToFilter.value = '';
    if (vehiclesIsOurVehicleFilter) vehiclesIsOurVehicleFilter.checked = false;
    if (vehiclesSearchInput) {
        vehiclesSearchInput.value = '';
    }
    setDefaultVehicleDates();
    loadVehicles(0);
});

let searchTimeout;
if (vehiclesSearchInput) {
    vehiclesSearchInput.addEventListener('input', () => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            loadVehicles(0);
        }, CLIENT_CONSTANTS.SEARCH_DEBOUNCE_DELAY);
    });
}

document.getElementById('open-vehicles-filter-modal')?.addEventListener('click', () => {
    openModal('vehicles-filter-modal');
});

document.getElementById('vehicles-filter-modal-close')?.addEventListener('click', () => {
    closeModal('vehicles-filter-modal');
});

if (updateVehicleForm) {
    updateVehicleForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!currentVehicleId) {
            showMessage('Не вдалося визначити машину для оновлення', 'error');
            return;
        }
        
        const carrierIdValue = detailVehicleCarrierSelect?.value;
        const payload = {
            shipmentDate: detailVehicleDateInput?.value || null,
            vehicleNumber: detailVehicleVehicleInput?.value ?? null,
            invoiceUa: detailVehicleInvoiceUaInput?.value ?? null,
            invoiceEu: detailVehicleInvoiceEuInput?.value ?? null,
            invoiceUaDate: detailVehicleInvoiceUaDateInput?.value || null,
            invoiceUaPricePerTon: detailVehicleInvoiceUaPricePerTonInput?.value ? parseFloat(detailVehicleInvoiceUaPricePerTonInput.value) : null,
            invoiceEuDate: detailVehicleInvoiceEuDateInput?.value || null,
            invoiceEuPricePerTon: detailVehicleInvoiceEuPricePerTonInput?.value ? parseFloat(detailVehicleInvoiceEuPricePerTonInput.value) : null,
            reclamation: detailVehicleReclamationInput?.value ? parseFloat(detailVehicleReclamationInput.value) : null,
            description: detailVehicleDescriptionInput?.value ?? null,
            senderId: detailVehicleSenderSelect?.value ? parseInt(detailVehicleSenderSelect.value) : null,
            receiverId: detailVehicleReceiverSelect?.value ? parseInt(detailVehicleReceiverSelect.value) : null,
            destinationCountryId: detailVehicleDestinationCountryInput?.value ? parseInt(detailVehicleDestinationCountryInput.value) : null,
            destinationPlaceId: detailVehicleDestinationPlaceInput?.value ? parseInt(detailVehicleDestinationPlaceInput.value) : null,
            product: detailVehicleProductInput?.value ?? null,
            productQuantity: detailVehicleProductQuantityInput?.value ?? null,
            declarationNumber: detailVehicleDeclarationNumberInput?.value ?? null,
            terminalId: detailVehicleTerminalInput?.value ? parseInt(detailVehicleTerminalInput.value) : null,
            driverFullName: detailVehicleDriverFullNameInput?.value ?? null,
            isOurVehicle: detailVehicleIsOurVehicleInput?.checked || false,
            eur1: detailVehicleEur1Input?.checked || false,
            fito: detailVehicleFitoInput?.checked || false,
            customsDate: detailVehicleCustomsDateInput?.value || null,
            customsClearanceDate: detailVehicleCustomsClearanceDateInput?.value || null,
            unloadingDate: detailVehicleUnloadingDateInput?.value || null,
            carrierId: carrierIdValue ? Number(carrierIdValue) : null
        };
        
        Object.keys(payload).forEach(key => {
            if (typeof payload[key] === 'string') {
                const trimmed = payload[key].trim();
                payload[key] = trimmed.length ? trimmed : null;
            }
        });
        
        if (!payload.shipmentDate) {
            showMessage('Вкажіть дату відвантаження', 'error');
            return;
        }
        
        try {
            const updatedVehicle = await DeclarantDataLoader.updateVehicle(currentVehicleId, payload);
            if (updatedVehicle && typeof updatedVehicle === 'object') {
                currentVehicleDetails = updatedVehicle;
                renderVehicleDetails(updatedVehicle);
                
                const expensesTotal = parseFloat(await getVehicleExpensesTotal(currentVehicleId)) || 0;
                const productsTotalCost = calculateProductsTotalCost(updatedVehicle);
                const totalExpenses = productsTotalCost + expensesTotal;
                
                const invoiceEuTotalPrice = updatedVehicle.invoiceEuTotalPrice || 0;
                const fullReclamation = calculateFullReclamation(updatedVehicle);
                const totalIncome = invoiceEuTotalPrice - fullReclamation;
                const margin = totalIncome - totalExpenses;
                
                const vehicleTotalExpenses = document.getElementById('vehicle-total-expenses');
                const vehicleTotalIncome = document.getElementById('vehicle-total-income');
                const vehicleMargin = document.getElementById('vehicle-margin');
                
                if (vehicleTotalExpenses) {
                    vehicleTotalExpenses.textContent = formatNumber(totalExpenses, 2);
                }
                if (vehicleTotalIncome) {
                    vehicleTotalIncome.textContent = formatNumber(totalIncome, 2);
                }
                if (vehicleMargin) {
                    vehicleMargin.textContent = formatNumber(margin, 2);
                }
            }
            
            showMessage('Дані машини оновлено', 'success');
            await loadVehicles(0);
        } catch (error) {
            showMessage(error.message || 'Помилка при оновленні машини', 'error');
        }
    });
}

function updateVehicleItemMode() {
    const mode = document.querySelector('input[name="edit-vehicle-item-mode"]:checked')?.value;
    if (!mode) {
        return;
    }

    const quantityEnabled = mode === 'quantity';

    if (editVehicleItemQuantityInput) {
        editVehicleItemQuantityInput.disabled = !quantityEnabled;
        editVehicleItemQuantityInput.classList.toggle('vehicle-item-edit-disabled', !quantityEnabled);
    }
    if (editVehicleItemTotalCostInput) {
        editVehicleItemTotalCostInput.disabled = quantityEnabled;
        editVehicleItemTotalCostInput.classList.toggle('vehicle-item-edit-disabled', quantityEnabled);
    }

    if (editVehicleItemModeRadios.length > 0) {
        editVehicleItemModeRadios.forEach(radio => {
            const label = radio.closest('label');
            if (label) {
                label.classList.toggle('active', radio.checked);
            }
        });
    }
}

window.openEditVehicleItemModal = function openEditVehicleItemModal(itemId) {
    if (!currentVehicleDetails) {
        showMessage('Неможливо відредагувати товар: машина не вибрана', 'error');
        return;
    }

    const item = currentVehicleItems.get(Number(itemId));
    if (!item) {
        showMessage('Товар не знайдений або вже оновлений', 'error');
        return;
    }

    currentVehicleItemId = Number(itemId);

    if (editVehicleItemQuantityInput) {
        editVehicleItemQuantityInput.value = parseFloat(item.quantity).toFixed(2);
    }
    if (editVehicleItemTotalCostInput) {
        editVehicleItemTotalCostInput.value = parseFloat(item.totalCostEur).toFixed(6);
    }

    const quantityModeRadio = document.querySelector('input[name="edit-vehicle-item-mode"][value="quantity"]');
    if (quantityModeRadio) {
        quantityModeRadio.checked = true;
    }
    updateVehicleItemMode();

    openModal('edit-vehicle-item-modal');
};


if (editVehicleItemModeRadios.length > 0) {
    editVehicleItemModeRadios.forEach(radio => {
        radio.addEventListener('change', updateVehicleItemMode);
    });
    updateVehicleItemMode();
}

if (editVehicleItemForm) {
    editVehicleItemForm.addEventListener('submit', async (event) => {
        event.preventDefault();

        if (!currentVehicleId || currentVehicleItemId === null) {
            showMessage('Не вдалося визначити товар для оновлення', 'error');
            return;
        }

        const item = currentVehicleItems.get(Number(currentVehicleItemId));
        if (!item) {
            showMessage('Товар не знайдений або вже оновлений', 'error');
            return;
        }

        const mode = document.querySelector('input[name="edit-vehicle-item-mode"]:checked')?.value;
        const payload = {};

        if (mode === 'quantity') {
            const newQuantityValue = parseFloat(editVehicleItemQuantityInput.value);
            if (Number.isNaN(newQuantityValue) || newQuantityValue < 0) {
                showMessage('Вкажіть коректну кількість', 'error');
                return;
            }

            const roundedQuantity = parseFloat(newQuantityValue.toFixed(2));
            if (Math.abs(roundedQuantity - parseFloat(item.quantity)) < 0.001) {
                showMessage('Кількість не змінилася', 'info');
                return;
            }

            const performVehicleItemUpdate = async () => {
                try {
                    payload.quantity = roundedQuantity;
                    const updatedVehicle = await DeclarantDataLoader.updateVehicleProduct(currentVehicleId, currentVehicleItemId, payload);
                    currentVehicleDetails = updatedVehicle;
                    currentVehicleItems = new Map();
                    DeclarantModal.populateVehicleForm(updatedVehicle);
                    DeclarantModal.setVehicleFormEditable(false);
                    renderVehicleDetails(updatedVehicle);
                    
                    const expensesTotal = parseFloat(await getVehicleExpensesTotal(currentVehicleId)) || 0;
                    const productsTotalCost = calculateProductsTotalCost(updatedVehicle);
                    const totalExpenses = productsTotalCost + expensesTotal;
                    
                    const invoiceEuTotalPrice = updatedVehicle.invoiceEuTotalPrice || 0;
                    const fullReclamation = calculateFullReclamation(updatedVehicle);
                    const totalIncome = invoiceEuTotalPrice - fullReclamation;
                    const margin = totalIncome - totalExpenses;
                    
                    const vehicleTotalExpenses = document.getElementById('vehicle-total-expenses');
                    const vehicleTotalIncome = document.getElementById('vehicle-total-income');
                    const vehicleMargin = document.getElementById('vehicle-margin');
                    
                    if (vehicleTotalExpenses) {
                        vehicleTotalExpenses.textContent = formatNumber(totalExpenses, 2);
                    }
                    if (vehicleTotalIncome) {
                        vehicleTotalIncome.textContent = formatNumber(totalIncome, 2);
                    }
                    if (vehicleMargin) {
                        vehicleMargin.textContent = formatNumber(margin, 2);
                    }
                    
                    showMessage('Дані товару у машині оновлено', 'success');
                    await loadVehicles(0);
                    closeModal('edit-vehicle-item-modal');
                } catch (error) {
                    console.error('Error updating vehicle product:', error);
                    showMessage(error.message || 'Помилка при оновленні товару у машині', 'error');
                    handleError(error);
                }
            };

            if (roundedQuantity === 0) {
                const productLabel = item.productName || 'товар';
                const message = `Ви впевнені, що хочете повністю видалити ${productLabel} з машини?`;
                ConfirmationModal.show(
                    message,
                    CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
                    performVehicleItemUpdate,
                    () => {}
                );
                return;
            }

            await performVehicleItemUpdate();
        } else if (mode === 'totalCost') {
            const newTotalValue = parseFloat(editVehicleItemTotalCostInput.value);
            if (newTotalValue === undefined || newTotalValue === null || isNaN(newTotalValue) || newTotalValue <= 0) {
                showMessage('Вкажіть коректну загальну вартість', 'error');
                return;
            }

            const roundedTotal = parseFloat(newTotalValue.toFixed(6));
            if (Math.abs(roundedTotal - parseFloat(item.totalCostEur)) < 0.000001) {
                showMessage('Загальна вартість не змінилася', 'info');
                return;
            }

            payload.totalCostEur = roundedTotal;

            try {
                const updatedVehicle = await DeclarantDataLoader.updateVehicleProduct(currentVehicleId, currentVehicleItemId, payload);
                currentVehicleDetails = updatedVehicle;
                currentVehicleItems = new Map();
                DeclarantModal.populateVehicleForm(updatedVehicle);
                DeclarantModal.setVehicleFormEditable(false);
                renderVehicleDetails(updatedVehicle);
                
                const expensesTotal = parseFloat(await getVehicleExpensesTotal(currentVehicleId)) || 0;
                const productsTotalCost = calculateProductsTotalCost(updatedVehicle);
                const totalExpenses = productsTotalCost + expensesTotal;
                
                const invoiceEuTotalPrice = updatedVehicle.invoiceEuTotalPrice || 0;
                const fullReclamation = calculateFullReclamation(updatedVehicle);
                const totalIncome = invoiceEuTotalPrice - fullReclamation;
                const margin = totalIncome - totalExpenses;
                
                const vehicleTotalExpenses = document.getElementById('vehicle-total-expenses');
                const vehicleTotalIncome = document.getElementById('vehicle-total-income');
                const vehicleMargin = document.getElementById('vehicle-margin');
                
                if (vehicleTotalExpenses) {
                    vehicleTotalExpenses.textContent = formatNumber(totalExpenses, 2);
                }
                if (vehicleTotalIncome) {
                    vehicleTotalIncome.textContent = formatNumber(totalIncome, 2);
                }
                if (vehicleMargin) {
                    vehicleMargin.textContent = formatNumber(margin, 2);
                }
                
                showMessage('Дані товару у машині оновлено', 'success');
                await loadVehicles(0);
                closeModal('edit-vehicle-item-modal');
            } catch (error) {
                console.error('Error updating vehicle product:', error);
                showMessage(error.message || 'Помилка при оновленні товару у машині', 'error');
                handleError(error);
            }
        } else {
            showMessage('Оберіть параметр для редагування', 'error');
            return;
        }
    });
}

function openModal(modalId) {
    DeclarantModal.openModal(modalId);
}

window.openModal = openModal;

function closeModal(modalId) {
    const onClose = () => {
        if (modalId === 'create-vehicle-modal') {
            const createVehicleForm = document.getElementById('create-vehicle-form');
            if (createVehicleForm) createVehicleForm.reset();
        } else if (modalId === 'vehicle-details-modal') {
            resetVehicleFormState();
        } else if (modalId === 'edit-vehicle-item-modal') {
            const editVehicleItemForm = document.getElementById('edit-vehicle-item-form');
            if (editVehicleItemForm) {
                editVehicleItemForm.reset();
            }
            currentVehicleItemId = null;
            updateVehicleItemMode();
        } else if (modalId === 'edit-vehicle-expense-modal') {
            const editVehicleExpenseForm = document.getElementById('edit-vehicle-expense-form');
            if (editVehicleExpenseForm) {
                editVehicleExpenseForm.reset();
            }
            currentExpenseId = null;
        }
    };
    
    DeclarantModal.closeModal(modalId, onClose);
}

window.closeModal = closeModal;

const initializeModalClickHandlers = DeclarantModal.initializeModalClickHandlers;

document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const tabName = btn.dataset.tab;
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
        btn.classList.add('active');
        const content = document.getElementById(`tab-${tabName}`);
        if (content) {
            content.classList.add('active');
        }
        
        if (tabName === 'expenses' && currentVehicleId) {
            loadVehicleExpenses(currentVehicleId);
        }
    });
});

const setDefaultVehicleDates = DeclarantFilters.setDefaultVehicleDates;

document.getElementById('cancel-create-vehicle-btn')?.addEventListener('click', () => {
    closeModal('create-vehicle-modal');
});

document.getElementById('cancel-create-vehicle-expense-btn')?.addEventListener('click', () => {
    closeModal('create-vehicle-expense-modal');
});

document.getElementById('cancel-edit-vehicle-item-btn')?.addEventListener('click', () => {
    closeModal('edit-vehicle-item-modal');
});

let currentResizeHeader = null;
let startX = 0;
let startWidth = 0;

function initializeColumnResize() {
    if (!vehiclesTable) return;
    
    // Don't initialize column resize on mobile devices
    if (window.innerWidth <= 1024) {
        return;
    }
    
    const headers = vehiclesTable.querySelectorAll('.resizable-header');
    
    headers.forEach(header => {
        let resizeHandle = header.querySelector('.resize-handle');
        if (resizeHandle) {
            if (resizeHandle._mousedownHandler) {
                resizeHandle.removeEventListener('mousedown', resizeHandle._mousedownHandler);
            }
        } else {
            resizeHandle = document.createElement('div');
            resizeHandle.className = 'resize-handle';
            header.style.position = 'relative';
            header.appendChild(resizeHandle);
        }
        
        const mousedownHandler = (e) => {
            e.preventDefault();
            e.stopPropagation();
            currentResizeHeader = header;
            startX = e.pageX;
            startWidth = header.offsetWidth;
            header.classList.add('resizing');
            document.body.style.cursor = 'col-resize';
            document.body.style.userSelect = 'none';
        };
        resizeHandle.addEventListener('mousedown', mousedownHandler);
        resizeHandle._mousedownHandler = mousedownHandler;
    });
    
    if (!window.columnResizeInitialized) {
        const mousemoveHandler = (e) => {
            if (!currentResizeHeader) return;
            
            const diff = e.pageX - startX;
            const newWidth = Math.max(CLIENT_CONSTANTS.MIN_COLUMN_WIDTH, startWidth + diff);
            const column = currentResizeHeader.dataset.column;
            
            currentResizeHeader.style.minWidth = newWidth + 'px';
            currentResizeHeader.style.width = newWidth + 'px';
            const index = Array.from(currentResizeHeader.parentElement.children).indexOf(currentResizeHeader);
            const cells = vehiclesTable.querySelectorAll(`tbody tr td:nth-child(${index + 1})`);
            cells.forEach(cell => {
                cell.style.minWidth = newWidth + 'px';
                cell.style.width = newWidth + 'px';
            });
        };
        
        const mouseupHandler = () => {
            if (currentResizeHeader) {
                const column = currentResizeHeader.dataset.column;
                const width = Math.max(CLIENT_CONSTANTS.MIN_COLUMN_WIDTH, currentResizeHeader.offsetWidth);
                localStorage.setItem(`vehicle-column-width-${column}`, width.toString());
                currentResizeHeader.classList.remove('resizing');
                currentResizeHeader = null;
                document.body.style.cursor = '';
                document.body.style.userSelect = '';
            }
        };
        
        document.addEventListener('mousemove', mousemoveHandler);
        document.addEventListener('mouseup', mouseupHandler);
        
        window.columnResizeMousemoveHandler = mousemoveHandler;
        window.columnResizeMouseupHandler = mouseupHandler;
        window.columnResizeInitialized = true;
    }
    
    applySavedColumnWidths();
}

async function initialize() {
    await fetchProducts();
    await fetchWarehouses();
    await fetchCarriers();
    await fetchVehicleSenders();
    await fetchVehicleReceivers();
    await fetchVehicleTerminals();
    await fetchVehicleDestinationCountries();
    await fetchVehicleDestinationPlaces();
    populateCarriers('vehicle-carrier-id');
    populateCarriers('detail-vehicle-carrier-id');
    populateVehicleSenders('vehicle-sender');
    populateVehicleSenders('detail-vehicle-sender');
    populateVehicleReceivers('vehicle-receiver');
    populateVehicleReceivers('detail-vehicle-receiver');
    populateVehicleTerminals('vehicle-terminal');
    populateVehicleTerminals('detail-vehicle-terminal');
    populateVehicleDestinationCountries('vehicle-destination-country');
    populateVehicleDestinationCountries('detail-vehicle-destination-country');
    populateVehicleDestinationPlaces('vehicle-destination-place');
    populateVehicleDestinationPlaces('detail-vehicle-destination-place');
    await loadAccounts();
    setDefaultVehicleDates();
    await loadVehicles(0);
    initializeColumnResize();
    initializeModalClickHandlers();
}

let accountsCache = [];
let categoriesCache = new Map();
let categoryNameMap = new Map();

async function loadAccounts() {
    try {
        accountsCache = await DeclarantDataLoader.loadAccounts();
    } catch (error) {
        console.error('Error loading accounts:', error);
        handleError(error);
    }
}

const populateAccounts = (selectId) => DeclarantRenderer.populateAccounts(selectId, accountsCache);

async function loadCategoriesForVehicleExpense() {
    try {
        const categories = await DeclarantDataLoader.loadCategoriesForVehicleExpense();
        categoriesCache.set('VEHICLE_EXPENSE', categories);
        categoryNameMap.clear();
        categories.forEach(cat => {
            categoryNameMap.set(cat.id, cat.name);
        });
        return categories;
    } catch (error) {
        console.error('Error loading categories:', error);
        return [];
    }
}

const populateCategories = DeclarantRenderer.populateCategories;
const populateCurrencies = (selectId, accountId) => DeclarantRenderer.populateCurrencies(selectId, accountId, accountsCache);

async function loadVehicleExpenses(vehicleId) {
    const vehicleExpensesTbody = document.getElementById('vehicle-expenses-tbody');
    if (!vehicleExpensesTbody) return;
    
    try {
        if (!categoriesCache.has('VEHICLE_EXPENSE') || categoriesCache.get('VEHICLE_EXPENSE')?.length === 0) {
            await loadCategoriesForVehicleExpense();
        }
        
        const expenses = await DeclarantDataLoader.loadVehicleExpenses(vehicleId);
        DeclarantRenderer.renderVehicleExpenses(expenses, accountsCache, categoryNameMap);
    } catch (error) {
        console.error('Error loading vehicle expenses:', error);
        vehicleExpensesTbody.textContent = '';
        const row = document.createElement('tr');
        row.className = 'loading-row';
        const cell = document.createElement('td');
        cell.colSpan = 9;
        cell.style.textAlign = 'center';
        cell.style.color = 'var(--danger)';
        cell.textContent = CLIENT_MESSAGES.LOAD_ERROR;
        row.appendChild(cell);
        vehicleExpensesTbody.appendChild(row);
    }
}

const checkExchangeRatesFreshness = DeclarantDataLoader.checkExchangeRatesFreshness;

document.getElementById('create-vehicle-expense-btn')?.addEventListener('click', async () => {
    if (!currentVehicleId) {
        showMessage('Не вдалося визначити машину', 'error');
        return;
    }
    
    if (accountsCache.length === 0) {
        await loadAccounts();
    }
    
    populateAccounts('expense-from-account');
    
    const categories = await loadCategoriesForVehicleExpense();
    populateCategories('expense-category', categories);
    
    if (expenseFromAccount) expenseFromAccount.value = '';
    if (expenseCategory) expenseCategory.value = '';
    if (expenseAmount) expenseAmount.value = '';
    if (expenseCurrency) expenseCurrency.value = '';
    if (expenseDescription) expenseDescription.value = '';
    
    const exchangeRateWarning = document.getElementById('vehicle-expense-exchange-rate-warning');
    const ratesAreFresh = await checkExchangeRatesFreshness();
    if (exchangeRateWarning) {
        exchangeRateWarning.style.display = ratesAreFresh ? 'none' : 'block';
    }
    
    openModal('create-vehicle-expense-modal');
});

if (expenseFromAccount) {
    expenseFromAccount.addEventListener('change', (e) => {
        const accountId = e.target.value;
        populateCurrencies('expense-currency', accountId);
    });
}

if (createVehicleExpenseForm) {
    createVehicleExpenseForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        if (!currentVehicleId) {
            showMessage('Не вдалося визначити машину', 'error');
            return;
        }
        
        const fromAccountIdValue = expenseFromAccount?.value;
        const categoryIdValue = expenseCategory?.value;
        
        const formData = {
            fromAccountId: fromAccountIdValue && fromAccountIdValue !== '0' ? parseInt(fromAccountIdValue) : null,
            categoryId: categoryIdValue && categoryIdValue !== '0' ? parseInt(categoryIdValue) : null,
            amount: parseFloat(expenseAmount?.value || '0'),
            currency: expenseCurrency?.value || '',
            description: expenseDescription?.value || ''
        };
        
        try {
            await DeclarantDataLoader.createVehicleExpense(currentVehicleId, formData);
            showMessage('Витрату успішно створено', 'success');
            closeModal('create-vehicle-expense-modal');
            createVehicleExpenseForm.reset();
            
            await loadVehicleExpenses(currentVehicleId);
            
            if (currentVehicleDetails) {
                const expensesTotal = parseFloat(await getVehicleExpensesTotal(currentVehicleId)) || 0;
                const productsTotalCost = calculateProductsTotalCost(currentVehicleDetails);
                const totalExpenses = productsTotalCost + expensesTotal;
                
                const invoiceEuTotalPrice = currentVehicleDetails.invoiceEuTotalPrice || 0;
                const fullReclamation = calculateFullReclamation(currentVehicleDetails);
                const totalIncome = invoiceEuTotalPrice - fullReclamation;
                const margin = totalIncome - totalExpenses;
                
                const vehicleTotalExpenses = document.getElementById('vehicle-total-expenses');
                const vehicleTotalIncome = document.getElementById('vehicle-total-income');
                const vehicleMargin = document.getElementById('vehicle-margin');
                
                if (vehicleTotalExpenses) {
                    vehicleTotalExpenses.textContent = formatNumber(totalExpenses, 2);
                }
                if (vehicleTotalIncome) {
                    vehicleTotalIncome.textContent = formatNumber(totalIncome, 2);
                }
                if (vehicleMargin) {
                    vehicleMargin.textContent = formatNumber(margin, 2);
                }
            }
        } catch (error) {
            showMessage(error.message || 'Помилка при створенні витрати', 'error');
        }
    });
}

window.openEditVehicleExpenseModal = async function openEditVehicleExpenseModal(expense) {
    if (!currentVehicleId) {
        showMessage('Не вдалося визначити машину', 'error');
        return;
    }
    
    currentExpenseId = expense.id;
    
    if (accountsCache.length === 0) {
        await loadAccounts();
    }
    
    populateAccounts('edit-expense-from-account');
    
    const categories = await loadCategoriesForVehicleExpense();
    populateCategories('edit-expense-category', categories);
    
    if (editExpenseFromAccount) editExpenseFromAccount.value = expense.fromAccountId || '';
    if (editExpenseCategory) editExpenseCategory.value = expense.categoryId || '';
    if (editExpenseAmount) editExpenseAmount.value = expense.amount || '';
    if (editExpenseCurrency) {
        editExpenseCurrency.value = expense.currency || '';
        if (expense.fromAccountId) {
            populateCurrencies('edit-expense-currency', expense.fromAccountId);
        }
    }
    if (editExpenseDescription) editExpenseDescription.value = expense.description || '';
    
    const exchangeRateWarning = document.getElementById('edit-vehicle-expense-exchange-rate-warning');
    const ratesAreFresh = await checkExchangeRatesFreshness();
    if (exchangeRateWarning) {
        exchangeRateWarning.style.display = ratesAreFresh ? 'none' : 'block';
    }
    
    openModal('edit-vehicle-expense-modal');
}

if (editExpenseFromAccount) {
    editExpenseFromAccount.addEventListener('change', (e) => {
        const accountId = e.target.value;
        populateCurrencies('edit-expense-currency', accountId);
    });
}

if (editExpenseAmount && editExpenseCurrency) {
    const recalculateConvertedAmount = async () => {
        const amount = parseFloat(editExpenseAmount.value);
        const currency = editExpenseCurrency.value;
        
        if (amount && currency && currency !== 'EUR') {
            try {
                const data = await DeclarantDataLoader.getExchangeRate(currency, 'EUR');
                if (data && data.rate) {
                    const convertedAmount = amount / data.rate;
                }
            } catch (error) {
                console.error('Error fetching exchange rate:', error);
            }
        }
    };
    
    editExpenseAmount.addEventListener('input', recalculateConvertedAmount);
    editExpenseCurrency.addEventListener('change', recalculateConvertedAmount);
}

if (editVehicleExpenseForm) {
    editVehicleExpenseForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        if (!currentVehicleId || !currentExpenseId) {
            showMessage('Не вдалося визначити машину або витрату', 'error');
            return;
        }
        
        const fromAccountIdValue = editExpenseFromAccount?.value;
        const categoryIdValue = editExpenseCategory?.value;
        
        const formData = {
            fromAccountId: fromAccountIdValue && fromAccountIdValue !== '0' ? parseInt(fromAccountIdValue) : null,
            categoryId: categoryIdValue && categoryIdValue !== '0' ? parseInt(categoryIdValue) : null,
            amount: parseFloat(editExpenseAmount?.value || '0'),
            currency: editExpenseCurrency?.value || '',
            description: editExpenseDescription?.value || ''
        };
        
        try {
            await DeclarantDataLoader.updateVehicleExpense(currentExpenseId, formData);
            showMessage('Витрату успішно оновлено', 'success');
            closeModal('edit-vehicle-expense-modal');
            editVehicleExpenseForm.reset();
            currentExpenseId = null;
            
            await loadVehicleExpenses(currentVehicleId);
            
            if (currentVehicleDetails) {
                const expensesTotal = parseFloat(await getVehicleExpensesTotal(currentVehicleId)) || 0;
                const productsTotalCost = calculateProductsTotalCost(currentVehicleDetails);
                const totalExpenses = productsTotalCost + expensesTotal;
                
                const invoiceEuTotalPrice = currentVehicleDetails.invoiceEuTotalPrice || 0;
                const fullReclamation = calculateFullReclamation(currentVehicleDetails);
                const totalIncome = invoiceEuTotalPrice - fullReclamation;
                const margin = totalIncome - totalExpenses;
                
                const vehicleTotalExpenses = document.getElementById('vehicle-total-expenses');
                const vehicleTotalIncome = document.getElementById('vehicle-total-income');
                const vehicleMargin = document.getElementById('vehicle-margin');
                
                if (vehicleTotalExpenses) {
                    vehicleTotalExpenses.textContent = formatNumber(totalExpenses, 2);
                }
                if (vehicleTotalIncome) {
                    vehicleTotalIncome.textContent = formatNumber(totalIncome, 2);
                }
                if (vehicleMargin) {
                    vehicleMargin.textContent = formatNumber(margin, 2);
                }
            }
        } catch (error) {
            showMessage(error.message || 'Помилка при оновленні витрати', 'error');
        }
    });
}

document.getElementById('cancel-edit-vehicle-expense-btn')?.addEventListener('click', () => {
    closeModal('edit-vehicle-expense-modal');
    currentExpenseId = null;
});

document.getElementById('export-vehicles-btn')?.addEventListener('click', async () => {
    try {
        const searchTerm = document.getElementById('vehicles-search-input')?.value || '';
        const filters = buildFilters();
        const filtersJson = Object.keys(filters).length > 0 ? JSON.stringify(filters) : '';
        
        await DeclarantDataLoader.exportVehicles(filtersJson, searchTerm);
        showMessage('Експорт успішно виконано', 'success');
    } catch (error) {
        console.error('Error exporting vehicles:', error);
        showMessage('Помилка експорту: ' + error.message, 'error');
    }
});

initialize();

