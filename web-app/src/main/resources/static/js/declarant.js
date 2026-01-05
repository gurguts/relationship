function escapeHtml(text) {
    if (text == null) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

let productMap = new Map();
let warehouseMap = new Map();
let carrierMap = new Map();
let vehicleSenderMap = new Map();
let vehicleReceiverMap = new Map();

function formatNumber(value, maxDecimals = 6) {
    if (value === null || value === undefined || value === '') return '0';
    const num = parseFloat(value);
    if (isNaN(num)) return '0';
    return parseFloat(num.toFixed(maxDecimals)).toString();
}

const findNameByIdFromMap = (map, id) => {
    const numericId = Number(id);
    return map.get(numericId) || '';
};

async function fetchProducts() {
    try {
        const response = await fetch('/api/v1/product');
        if (!response.ok) {
            const errorData = await response.json();
            handleError(new Error(errorData.message || 'Failed to fetch products'));
            return;
        }
        const products = await response.json();
        productMap = new Map(products.map(product => [product.id, product.name]));
    } catch (error) {
        console.error('Error fetching products:', error);
        handleError(error);
    }
}

async function fetchWarehouses() {
    try {
        const response = await fetch('/api/v1/warehouse');
        if (!response.ok) {
            const errorData = await response.json();
            handleError(new Error(errorData.message || 'Failed to fetch warehouses'));
            return;
        }
        const warehouses = await response.json();
        warehouseMap = new Map(warehouses.map(warehouse => [warehouse.id, warehouse.name]));
    } catch (error) {
        console.error('Error fetching warehouses:', error);
        handleError(error);
    }
}


async function fetchCarriers() {
    try {
        const response = await fetch('/api/v1/carriers');
        if (!response.ok) {
            const errorData = await response.json();
            handleError(new Error(errorData.message || 'Failed to fetch carriers'));
            return;
        }
        const carriers = await response.json();
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
        const response = await fetch('/api/v1/vehicle-senders');
        if (!response.ok) {
            const errorData = await response.json();
            handleError(new Error(errorData.message || 'Failed to fetch vehicle senders'));
            return;
        }
        const senders = await response.json();
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
        const response = await fetch('/api/v1/vehicle-receivers');
        if (!response.ok) {
            const errorData = await response.json();
            handleError(new Error(errorData.message || 'Failed to fetch vehicle receivers'));
            return;
        }
        const receivers = await response.json();
        vehicleReceiverMap = new Map(receivers.map(receiver => [receiver.id, receiver]));
        return receivers;
    } catch (error) {
        console.error('Error fetching vehicle receivers:', error);
        handleError(error);
        return [];
    }
}

function populateCarriers(selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.textContent = '';
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = 'Оберіть перевізника';
    select.appendChild(defaultOption);
    for (const [id, carrier] of carrierMap.entries()) {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = carrier.companyName;
        select.appendChild(option);
    }
}

function populateVehicleSenders(selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.textContent = '';
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = 'Оберіть відправника';
    select.appendChild(defaultOption);
    for (const [id, sender] of vehicleSenderMap.entries()) {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = sender.name;
        select.appendChild(option);
    }
}

function populateVehicleReceivers(selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.textContent = '';
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = 'Оберіть отримувача';
    select.appendChild(defaultOption);
    for (const [id, receiver] of vehicleReceiverMap.entries()) {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = receiver.name;
        select.appendChild(option);
    }
}

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
const vehicleDestinationCountry = document.getElementById('vehicle-destination-country');
const vehicleDestinationPlace = document.getElementById('vehicle-destination-place');
const vehicleProduct = document.getElementById('vehicle-product');
const vehicleProductQuantity = document.getElementById('vehicle-product-quantity');
const vehicleDeclarationNumber = document.getElementById('vehicle-declaration-number');
const vehicleTerminal = document.getElementById('vehicle-terminal');
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

const carrierForm = document.getElementById('carrier-form');
const carrierId = document.getElementById('carrier-id');
const carrierCompanyName = document.getElementById('carrier-company-name');
const carrierRegistrationAddress = document.getElementById('carrier-registration-address');
const carrierPhoneNumber = document.getElementById('carrier-phone-number');
const carrierCode = document.getElementById('carrier-code');
const carrierAccount = document.getElementById('carrier-account');
const carrierFormTitle = document.getElementById('carrier-form-title');

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
let pageSize = 20;
let totalPages = 0;
let totalElements = 0;
let currentVehicleItems = new Map();
let currentVehicleItemId = null;

function populateVehicleForm(vehicle) {
    if (!vehicle) {
        if (detailVehicleDateInput) detailVehicleDateInput.value = '';
        if (detailVehicleVehicleInput) detailVehicleVehicleInput.value = '';
        if (detailVehicleInvoiceUaInput) detailVehicleInvoiceUaInput.value = '';
        if (detailVehicleInvoiceEuInput) detailVehicleInvoiceEuInput.value = '';
        if (detailVehicleDescriptionInput) detailVehicleDescriptionInput.value = '';
        if (detailVehicleSenderSelect) detailVehicleSenderSelect.value = '';
        if (detailVehicleReceiverSelect) detailVehicleReceiverSelect.value = '';
        if (detailVehicleDestinationCountryInput) detailVehicleDestinationCountryInput.value = '';
        if (detailVehicleDestinationPlaceInput) detailVehicleDestinationPlaceInput.value = '';
        if (detailVehicleProductInput) detailVehicleProductInput.value = '';
        if (detailVehicleProductQuantityInput) detailVehicleProductQuantityInput.value = '';
        if (detailVehicleDeclarationNumberInput) detailVehicleDeclarationNumberInput.value = '';
        if (detailVehicleTerminalInput) detailVehicleTerminalInput.value = '';
        if (detailVehicleDriverFullNameInput) detailVehicleDriverFullNameInput.value = '';
        if (detailVehicleIsOurVehicleInput) detailVehicleIsOurVehicleInput.checked = false;
        if (detailVehicleEur1Input) detailVehicleEur1Input.checked = false;
        if (detailVehicleFitoInput) detailVehicleFitoInput.checked = false;
        if (detailVehicleCustomsDateInput) detailVehicleCustomsDateInput.value = '';
        if (detailVehicleCustomsClearanceDateInput) detailVehicleCustomsClearanceDateInput.value = '';
        if (detailVehicleUnloadingDateInput) detailVehicleUnloadingDateInput.value = '';
        if (detailVehicleCarrierSelect) detailVehicleCarrierSelect.value = '';
        if (detailVehicleInvoiceUaDateInput) detailVehicleInvoiceUaDateInput.value = '';
        if (detailVehicleInvoiceUaPricePerTonInput) detailVehicleInvoiceUaPricePerTonInput.value = '';
        if (detailVehicleInvoiceUaTotalPriceInput) detailVehicleInvoiceUaTotalPriceInput.value = '';
        if (detailVehicleInvoiceEuDateInput) detailVehicleInvoiceEuDateInput.value = '';
        if (detailVehicleInvoiceEuPricePerTonInput) detailVehicleInvoiceEuPricePerTonInput.value = '';
        if (detailVehicleInvoiceEuTotalPriceInput) detailVehicleInvoiceEuTotalPriceInput.value = '';
        if (detailVehicleReclamationInput) detailVehicleReclamationInput.value = '';
        if (detailVehicleFullReclamationInput) detailVehicleFullReclamationInput.value = '';
        return;
    }

    if (detailVehicleDateInput) detailVehicleDateInput.value = vehicle.shipmentDate || '';
    if (detailVehicleVehicleInput) detailVehicleVehicleInput.value = vehicle.vehicleNumber || '';
    if (detailVehicleInvoiceUaInput) detailVehicleInvoiceUaInput.value = vehicle.invoiceUa || '';
    if (detailVehicleInvoiceEuInput) detailVehicleInvoiceEuInput.value = vehicle.invoiceEu || '';
    if (detailVehicleDescriptionInput) detailVehicleDescriptionInput.value = vehicle.description || '';
    if (detailVehicleSenderSelect) detailVehicleSenderSelect.value = vehicle.senderId || '';
    if (detailVehicleReceiverSelect) detailVehicleReceiverSelect.value = vehicle.receiverId || '';
    if (detailVehicleDestinationCountryInput) detailVehicleDestinationCountryInput.value = vehicle.destinationCountry || '';
    if (detailVehicleDestinationPlaceInput) detailVehicleDestinationPlaceInput.value = vehicle.destinationPlace || '';
    if (detailVehicleProductInput) detailVehicleProductInput.value = vehicle.product || '';
    if (detailVehicleProductQuantityInput) detailVehicleProductQuantityInput.value = vehicle.productQuantity || '';
    if (detailVehicleDeclarationNumberInput) detailVehicleDeclarationNumberInput.value = vehicle.declarationNumber || '';
    if (detailVehicleTerminalInput) detailVehicleTerminalInput.value = vehicle.terminal || '';
    if (detailVehicleDriverFullNameInput) detailVehicleDriverFullNameInput.value = vehicle.driverFullName || '';
    if (detailVehicleIsOurVehicleInput) detailVehicleIsOurVehicleInput.checked = vehicle.isOurVehicle || false;
    if (detailVehicleEur1Input) detailVehicleEur1Input.checked = vehicle.eur1 || false;
    if (detailVehicleFitoInput) detailVehicleFitoInput.checked = vehicle.fito || false;
    if (detailVehicleCustomsDateInput) detailVehicleCustomsDateInput.value = vehicle.customsDate || '';
    if (detailVehicleCustomsClearanceDateInput) detailVehicleCustomsClearanceDateInput.value = vehicle.customsClearanceDate || '';
    if (detailVehicleUnloadingDateInput) detailVehicleUnloadingDateInput.value = vehicle.unloadingDate || '';
    if (detailVehicleCarrierSelect) detailVehicleCarrierSelect.value = vehicle.carrier?.id || '';
    if (detailVehicleInvoiceUaDateInput) detailVehicleInvoiceUaDateInput.value = vehicle.invoiceUaDate || '';
    if (detailVehicleInvoiceUaPricePerTonInput) detailVehicleInvoiceUaPricePerTonInput.value = vehicle.invoiceUaPricePerTon || '';
    if (detailVehicleInvoiceUaTotalPriceInput) detailVehicleInvoiceUaTotalPriceInput.value = vehicle.invoiceUaTotalPrice || '';
    if (detailVehicleInvoiceEuDateInput) detailVehicleInvoiceEuDateInput.value = vehicle.invoiceEuDate || '';
    if (detailVehicleInvoiceEuPricePerTonInput) detailVehicleInvoiceEuPricePerTonInput.value = vehicle.invoiceEuPricePerTon || '';
    if (detailVehicleInvoiceEuTotalPriceInput) detailVehicleInvoiceEuTotalPriceInput.value = vehicle.invoiceEuTotalPrice || '';
    if (detailVehicleReclamationInput) detailVehicleReclamationInput.value = vehicle.reclamation || '';
    
    // Calculate and display full reclamation
    const fullReclamation = calculateFullReclamation(vehicle);
    if (detailVehicleFullReclamationInput) {
        detailVehicleFullReclamationInput.value = fullReclamation > 0 ? fullReclamation.toFixed(6) : '';
    }
}

function setVehicleFormEditable(isEditable) {
    const fields = [
        detailVehicleDateInput,
        detailVehicleVehicleInput,
        detailVehicleInvoiceUaInput,
        detailVehicleInvoiceEuInput,
        detailVehicleInvoiceUaDateInput,
        detailVehicleInvoiceUaPricePerTonInput,
        detailVehicleInvoiceEuDateInput,
        detailVehicleInvoiceEuPricePerTonInput,
        detailVehicleReclamationInput,
        detailVehicleDescriptionInput,
        detailVehicleSenderSelect,
        detailVehicleReceiverSelect,
        detailVehicleDestinationCountryInput,
        detailVehicleDestinationPlaceInput,
        detailVehicleProductInput,
        detailVehicleProductQuantityInput,
        detailVehicleDeclarationNumberInput,
        detailVehicleTerminalInput,
        detailVehicleDriverFullNameInput,
        detailVehicleIsOurVehicleInput,
        detailVehicleEur1Input,
        detailVehicleFitoInput,
        detailVehicleCustomsDateInput,
        detailVehicleCustomsClearanceDateInput,
        detailVehicleUnloadingDateInput,
        detailVehicleCarrierSelect
    ];

    fields.forEach(field => {
        if (field) {
            field.disabled = !isEditable;
        }
    });

    if (saveVehicleBtn) {
        saveVehicleBtn.style.display = isEditable ? 'inline-flex' : 'none';
    }
    if (editVehicleBtn) {
        editVehicleBtn.style.display = isEditable ? 'none' : 'inline-flex';
    }
}

function resetVehicleFormState() {
    populateVehicleForm(currentVehicleDetails);
    setVehicleFormEditable(false);
}

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
            destinationCountry: vehicleDestinationCountry?.value || '',
            destinationPlace: vehicleDestinationPlace?.value || '',
            product: vehicleProduct?.value || '',
            productQuantity: vehicleProductQuantity?.value || '',
            declarationNumber: vehicleDeclarationNumber?.value || '',
            terminal: vehicleTerminal?.value || '',
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
            const response = await fetch('/api/v1/vehicles', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(vehicleData)
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to create vehicle');
            }
            
            await response.json();
            showMessage('Машину успішно створено', 'success');
            
            closeModal('create-vehicle-modal');
            createVehicleForm?.reset();
            
            await loadVehicles(0);
        } catch (error) {
            showMessage(error.message || 'Помилка при створенні машини', 'error');
        }
    });
}


function buildFilters() {
    const filters = {};
    
    const dateFrom = vehiclesDateFromFilter?.value;
    const dateTo = vehiclesDateToFilter?.value;
    if (dateFrom) {
        filters.shipmentDateFrom = [dateFrom];
    }
    if (dateTo) {
        filters.shipmentDateTo = [dateTo];
    }
    
    const isOurVehicleFilter = vehiclesIsOurVehicleFilter?.checked;
    if (isOurVehicleFilter !== undefined && isOurVehicleFilter) {
        filters.isOurVehicle = ['true'];
    }
    
    const customsDateFrom = vehiclesCustomsDateFromFilter?.value;
    const customsDateTo = vehiclesCustomsDateToFilter?.value;
    if (customsDateFrom) {
        filters.customsDateFrom = [customsDateFrom];
    }
    if (customsDateTo) {
        filters.customsDateTo = [customsDateTo];
    }
    
    const customsClearanceDateFrom = vehiclesCustomsClearanceDateFromFilter?.value;
    const customsClearanceDateTo = vehiclesCustomsClearanceDateToFilter?.value;
    if (customsClearanceDateFrom) {
        filters.customsClearanceDateFrom = [customsClearanceDateFrom];
    }
    if (customsClearanceDateTo) {
        filters.customsClearanceDateTo = [customsClearanceDateTo];
    }
    
    const unloadingDateFrom = vehiclesUnloadingDateFromFilter?.value;
    const unloadingDateTo = vehiclesUnloadingDateToFilter?.value;
    if (unloadingDateFrom) {
        filters.unloadingDateFrom = [unloadingDateFrom];
    }
    if (unloadingDateTo) {
        filters.unloadingDateTo = [unloadingDateTo];
    }
    
    return filters;
}

async function loadVehicles(page = 0) {
    currentPage = page;
    
    const searchTerm = vehiclesSearchInput?.value || '';
    const filters = buildFilters();
    const filtersJson = Object.keys(filters).length > 0 ? JSON.stringify(filters) : '';
    
    try {
        let url = `/api/v1/vehicles/search?page=${page}&size=${pageSize}&sort=id&direction=DESC`;
        
        if (searchTerm) {
            url += `&q=${encodeURIComponent(searchTerm)}`;
        }
        
        if (filtersJson) {
            url += `&filters=${encodeURIComponent(filtersJson)}`;
        }
        
        const response = await fetch(url);
        
        if (!response.ok) {
            throw new Error('Failed to load vehicles');
        }
        
        const data = await response.json();
        vehiclesCache = data.content || [];
        totalPages = data.totalPages || 0;
        totalElements = data.totalElements || 0;
        
        renderVehicles(vehiclesCache);
        renderPagination();
    } catch (error) {
        console.error('Error loading vehicles:', error);
        showMessage('Помилка завантаження машин', 'error');
        
        if (vehiclesTbody) {
            vehiclesTbody.textContent = '';
            const row = document.createElement('tr');
            row.className = 'loading-row';
        const cell = document.createElement('td');
        cell.colSpan = 26;
        cell.style.textAlign = 'center';
        cell.style.color = 'var(--text-muted)';
        cell.textContent = 'Помилка завантаження даних';
            row.appendChild(cell);
            vehiclesTbody.appendChild(row);
        }
    }
}

function renderPagination() {
    if (!vehiclesPagination) return;
    
    if (totalPages <= 1) {
        vehiclesPagination.textContent = '';
        return;
    }
    
    vehiclesPagination.textContent = '';
    const paginationDiv = document.createElement('div');
    paginationDiv.className = 'pagination';
    
    const firstBtn = document.createElement('button');
    firstBtn.className = 'pagination-btn';
    firstBtn.disabled = currentPage === 0;
    const firstSpan = document.createElement('span');
    firstSpan.textContent = '«';
    firstBtn.appendChild(firstSpan);
    const firstBtnHandler = () => loadVehicles(0);
    firstBtn.addEventListener('click', firstBtnHandler);
    firstBtn._clickHandler = firstBtnHandler;
    paginationDiv.appendChild(firstBtn);
    
    const prevBtn = document.createElement('button');
    prevBtn.className = 'pagination-btn';
    prevBtn.disabled = currentPage === 0;
    const prevSpan = document.createElement('span');
    prevSpan.textContent = '‹';
    prevBtn.appendChild(prevSpan);
    const prevBtnHandler = () => loadVehicles(currentPage - 1);
    prevBtn.addEventListener('click', prevBtnHandler);
    prevBtn._clickHandler = prevBtnHandler;
    paginationDiv.appendChild(prevBtn);
    
    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(totalPages - 1, currentPage + 2);
    
    if (startPage > 0) {
        const firstPageBtn = document.createElement('button');
        firstPageBtn.className = 'pagination-btn';
        firstPageBtn.textContent = '1';
        const firstPageBtnHandler = () => loadVehicles(0);
        firstPageBtn.addEventListener('click', firstPageBtnHandler);
        firstPageBtn._clickHandler = firstPageBtnHandler;
        paginationDiv.appendChild(firstPageBtn);
        if (startPage > 1) {
            const ellipsis1 = document.createElement('span');
            ellipsis1.className = 'pagination-ellipsis';
            ellipsis1.textContent = '...';
            paginationDiv.appendChild(ellipsis1);
        }
    }
    
    for (let i = startPage; i <= endPage; i++) {
        const pageBtn = document.createElement('button');
        pageBtn.className = 'pagination-btn';
        if (i === currentPage) {
            pageBtn.classList.add('active');
        }
        pageBtn.textContent = (i + 1).toString();
        const pageBtnHandler = () => loadVehicles(i);
        pageBtn.addEventListener('click', pageBtnHandler);
        pageBtn._clickHandler = pageBtnHandler;
        paginationDiv.appendChild(pageBtn);
    }
    
    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            const ellipsis2 = document.createElement('span');
            ellipsis2.className = 'pagination-ellipsis';
            ellipsis2.textContent = '...';
            paginationDiv.appendChild(ellipsis2);
        }
        const lastPageBtn = document.createElement('button');
        lastPageBtn.className = 'pagination-btn';
        lastPageBtn.textContent = totalPages.toString();
        const lastPageBtnHandler = () => loadVehicles(totalPages - 1);
        lastPageBtn.addEventListener('click', lastPageBtnHandler);
        lastPageBtn._clickHandler = lastPageBtnHandler;
        paginationDiv.appendChild(lastPageBtn);
    }
    
    const nextBtn = document.createElement('button');
    nextBtn.className = 'pagination-btn';
    nextBtn.disabled = currentPage >= totalPages - 1;
    const nextSpan = document.createElement('span');
    nextSpan.textContent = '›';
    nextBtn.appendChild(nextSpan);
    const nextBtnHandler = () => loadVehicles(currentPage + 1);
    nextBtn.addEventListener('click', nextBtnHandler);
    nextBtn._clickHandler = nextBtnHandler;
    paginationDiv.appendChild(nextBtn);
    
    const lastBtn = document.createElement('button');
    lastBtn.className = 'pagination-btn';
    lastBtn.disabled = currentPage >= totalPages - 1;
    const lastSpan = document.createElement('span');
    lastSpan.textContent = '»';
    lastBtn.appendChild(lastSpan);
    const lastBtnHandler = () => loadVehicles(totalPages - 1);
    lastBtn.addEventListener('click', lastBtnHandler);
    lastBtn._clickHandler = lastBtnHandler;
    paginationDiv.appendChild(lastBtn);
    
    vehiclesPagination.appendChild(paginationDiv);
}

function formatDate(dateString) {
    if (!dateString) return '-';
    return dateString;
}

function formatBoolean(value) {
    return value ? '✓' : '-';
}

function formatCarrier(carrier) {
    return carrier?.companyName || '-';
}

async function renderVehicles(vehicles) {
    if (!vehiclesTbody) {
        return;
    }
    
    if (vehiclesCount) {
        const start = currentPage * pageSize + 1;
        const end = Math.min((currentPage + 1) * pageSize, totalElements);
        vehiclesCount.textContent = totalElements > 0 
            ? `Показано ${start}-${end} з ${totalElements} ${totalElements === 1 ? 'машини' : 'машин'}`
            : '0 машин';
    }
    
    if (!vehicles || vehicles.length === 0) {
        vehiclesTbody.textContent = '';
        const row = document.createElement('tr');
        row.className = 'loading-row';
        const cell = document.createElement('td');
        cell.colSpan = 26;
        cell.style.textAlign = 'center';
        cell.style.color = 'var(--text-muted)';
        cell.textContent = 'Немає даних';
        row.appendChild(cell);
        vehiclesTbody.appendChild(row);
        return;
    }
    
    vehiclesTbody.textContent = '';
    
    const expensesPromises = vehicles.map(async (vehicle) => {
        try {
            const response = await fetch(`/api/v1/vehicles/${vehicle.id}/expenses`);
            if (!response.ok) return 0;
            const expenses = await response.json();
            return expenses.reduce((sum, e) => sum + (parseFloat(e.convertedAmount) || 0), 0);
        } catch (error) {
            return 0;
        }
    });
    
    const itemsPromises = vehicles.map(async (vehicle) => {
        if (vehicle.items && vehicle.items.length > 0) {
            return calculateProductsTotalCost(vehicle);
        }
        try {
            const response = await fetch(`/api/v1/vehicles/${vehicle.id}`);
            if (!response.ok) return 0;
            const vehicleDetails = await response.json();
            return calculateProductsTotalCost(vehicleDetails);
        } catch (error) {
            return 0;
        }
    });
    
    const [expensesTotals, productsTotals] = await Promise.all([
        Promise.all(expensesPromises),
        Promise.all(itemsPromises)
    ]);
    
    vehicles.forEach((vehicle, index) => {
        const row = document.createElement('tr');
        const rowClickHandler = () => viewVehicleDetails(vehicle.id);
        row.addEventListener('click', rowClickHandler);
        row._clickHandler = rowClickHandler;
        
        const createCell = (text, label, style) => {
            const cell = document.createElement('td');
            cell.setAttribute('data-label', label);
            if (style) {
                Object.assign(cell.style, style);
            }
            cell.textContent = text;
            return cell;
        };
        
        const productsTotalCost = productsTotals[index] || 0;
        const expensesTotal = expensesTotals[index] || 0;
        
        row.appendChild(createCell(vehicle.vehicleNumber || '-', 'Номер машини'));
        row.appendChild(createCell(`${formatNumber(productsTotalCost, 2)} EUR`, 'Витрати на товар', { fontWeight: '600', color: 'var(--primary)' }));
        row.appendChild(createCell(`${formatNumber(expensesTotal, 2)} EUR`, 'Витрати на машину', { fontWeight: '600', color: 'var(--primary)' }));
        row.appendChild(createCell(`${formatNumber(vehicle.totalExpenses, 2)} EUR`, 'Загальні витрати', { fontWeight: '600', color: 'var(--primary)' }));
        row.appendChild(createCell(`${formatNumber(vehicle.totalIncome, 2)} EUR`, 'Загальний дохід', { fontWeight: '600', color: 'var(--success)' }));
        const marginValue = vehicle.margin != null ? parseFloat(vehicle.margin) : 0;
        row.appendChild(createCell(`${formatNumber(vehicle.margin, 2)} EUR`, 'Маржа', { fontWeight: '600', color: marginValue >= 0 ? 'var(--success)' : 'var(--danger)' }));
        row.appendChild(createCell(formatDate(vehicle.shipmentDate), 'Дата відвантаження'));
        row.appendChild(createCell(vehicle.invoiceUa || '-', 'Інвойс УА'));
        row.appendChild(createCell(vehicle.invoiceEu || '-', 'Інвойс ЄС'));
        row.appendChild(createCell(formatBoolean(vehicle.isOurVehicle), 'Наше завантаження'));
        row.appendChild(createCell(vehicle.senderName || '-', 'Відправник'));
        row.appendChild(createCell(vehicle.receiverName || '-', 'Отримувач'));
        row.appendChild(createCell(vehicle.destinationCountry || '-', 'Країна призначення'));
        row.appendChild(createCell(vehicle.destinationPlace || '-', 'Місце призначення'));
        row.appendChild(createCell(vehicle.product || '-', 'Товар'));
        row.appendChild(createCell(vehicle.productQuantity || '-', 'Кількість товару'));
        row.appendChild(createCell(vehicle.declarationNumber || '-', 'Номер декларації'));
        row.appendChild(createCell(vehicle.terminal || '-', 'Термінал'));
        row.appendChild(createCell(vehicle.driverFullName || '-', 'Водій (ПІБ)'));
        row.appendChild(createCell(formatBoolean(vehicle.eur1), 'EUR1'));
        row.appendChild(createCell(formatBoolean(vehicle.fito), 'FITO'));
        row.appendChild(createCell(formatDate(vehicle.customsDate), 'Дата замитнення'));
        row.appendChild(createCell(formatDate(vehicle.customsClearanceDate), 'Дата розмитнення'));
        row.appendChild(createCell(formatDate(vehicle.unloadingDate), 'Дата вивантаження'));
        row.appendChild(createCell(formatCarrier(vehicle.carrier), 'Перевізник'));
        row.appendChild(createCell(vehicle.description || '-', 'Коментар'));
        
        vehiclesTbody.appendChild(row);
    });
    
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
        const response = await fetch(`/api/v1/vehicles/${vehicleId}`);
        
        if (!response.ok) {
            throw new Error('Failed to load vehicle details');
        }
        
        const vehicle = await response.json();
        populateCarriers('detail-vehicle-carrier-id');
        populateVehicleSenders('detail-vehicle-sender');
        populateVehicleReceivers('detail-vehicle-receiver');
        renderVehicleDetails(vehicle);
        
        document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
        document.querySelector('.tab-btn[data-tab="info"]')?.classList.add('active');
        const tabInfo = document.getElementById('tab-info');
        if (tabInfo) {
            tabInfo.classList.add('active');
        }
        
        await loadVehicleExpenses(vehicleId);
        
        const expensesTotal = parseFloat(await getVehicleExpensesTotal()) || 0;
        const productsTotalCost = calculateProductsTotalCost(vehicle);
        const totalExpenses = productsTotalCost + expensesTotal;
        
        const invoiceEuTotalPrice = vehicle.invoiceEuTotalPrice || 0;
        const fullReclamation = calculateFullReclamation(vehicle);
        const totalIncome = invoiceEuTotalPrice - fullReclamation;
        const margin = totalIncome - totalExpenses;
        
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
    
    if (!vehicleItemsTbody) return;
    
    vehicleItemsTbody.textContent = '';
    
    if (!vehicle.items || vehicle.items.length === 0) {
        const row = document.createElement('tr');
        row.className = 'loading-row';
        const cell = document.createElement('td');
        cell.colSpan = 6;
        cell.style.textAlign = 'center';
        cell.style.color = 'var(--text-muted)';
        cell.textContent = 'Товари ще не додані';
        row.appendChild(cell);
        vehicleItemsTbody.appendChild(row);
    } else {
        vehicle.items.forEach(item => {
            const productName = findNameByIdFromMap(productMap, item.productId) || 'Невідомий товар';
            const warehouseName = findNameByIdFromMap(warehouseMap, item.warehouseId) || 'Невідомий склад';

            currentVehicleItems.set(Number(item.withdrawalId), {
                ...item,
                productName,
                warehouseName
            });

            const row = document.createElement('tr');
            row.className = 'vehicle-item-row';
            row.setAttribute('data-item-id', item.withdrawalId.toString());
            
            const createCell = (text, label, style) => {
                const cell = document.createElement('td');
                cell.setAttribute('data-label', label);
                if (style) {
                    Object.assign(cell.style, style);
                }
                cell.textContent = text;
                return cell;
            };
            
            row.appendChild(createCell(productName, 'Товар'));
            row.appendChild(createCell(warehouseName, 'Склад'));
            row.appendChild(createCell(`${formatNumber(item.quantity, 2)} кг`, 'Кількість'));
            row.appendChild(createCell(`${formatNumber(item.unitPriceEur, 6)} EUR`, 'Ціна за кг', { textAlign: 'right' }));
            row.appendChild(createCell(`${formatNumber(item.totalCostEur, 6)} EUR`, 'Загальна вартість', { textAlign: 'right', fontWeight: '600', color: 'var(--primary)' }));
            row.appendChild(createCell(item.withdrawalDate || vehicle.shipmentDate || '-', 'Дата списання'));
            
            vehicleItemsTbody.appendChild(row);
        });
    }
    
}

async function getVehicleExpensesTotal() {
    if (!currentVehicleId) return 0;
    try {
        const response = await fetch(`/api/v1/vehicles/${currentVehicleId}/expenses`);
        if (!response.ok) return 0;
        const expenses = await response.json();
        return expenses.reduce((sum, e) => sum + (parseFloat(e.convertedAmount) || 0), 0);
    } catch (error) {
        return 0;
    }
}

function calculateProductsTotalCost(vehicle) {
    if (!vehicle || !vehicle.items || vehicle.items.length === 0) {
        return 0;
    }
    return vehicle.items.reduce((sum, item) => {
        const itemTotalCost = parseFloat(item.totalCostEur) || 0;
        return sum + itemTotalCost;
    }, 0);
}

function calculateFullReclamation(vehicle) {
    const reclamationPerTon = parseFloat(vehicle.reclamation) || 0;
    if (reclamationPerTon === 0) {
        return 0;
    }
    
    const productQuantityStr = vehicle.productQuantity;
    if (!productQuantityStr || productQuantityStr.trim() === '') {
        return 0;
    }
    
    try {
        const quantityInTons = parseFloat(productQuantityStr.replace(',', '.')) || 0;
        return reclamationPerTon * quantityInTons;
        } catch (error) {
        console.warn('Failed to parse productQuantity for reclamation calculation:', productQuantityStr, error);
        return 0;
    }
}

function calculateInvoiceUaTotalPrice() {
    const quantity = parseFloat(vehicleProductQuantity?.value?.replace(',', '.') || '0');
    const pricePerTon = parseFloat(vehicleInvoiceUaPricePerTon?.value || '0');
    if (quantity > 0 && pricePerTon > 0) {
        const total = quantity * pricePerTon;
        if (vehicleInvoiceUaTotalPrice) {
            vehicleInvoiceUaTotalPrice.value = total.toFixed(6);
        }
    } else if (vehicleInvoiceUaTotalPrice) {
        vehicleInvoiceUaTotalPrice.value = '';
    }
}

function calculateInvoiceEuTotalPrice() {
    const quantity = parseFloat(vehicleProductQuantity?.value?.replace(',', '.') || '0');
    const pricePerTon = parseFloat(vehicleInvoiceEuPricePerTon?.value || '0');
    if (quantity > 0 && pricePerTon > 0) {
        const total = quantity * pricePerTon;
        if (vehicleInvoiceEuTotalPrice) {
            vehicleInvoiceEuTotalPrice.value = total.toFixed(6);
        }
    } else if (vehicleInvoiceEuTotalPrice) {
        vehicleInvoiceEuTotalPrice.value = '';
    }
}

function calculateDetailInvoiceUaTotalPrice() {
    const quantity = parseFloat(detailVehicleProductQuantityInput?.value?.replace(',', '.') || '0');
    const pricePerTon = parseFloat(detailVehicleInvoiceUaPricePerTonInput?.value || '0');
    if (quantity > 0 && pricePerTon > 0) {
        const total = quantity * pricePerTon;
        if (detailVehicleInvoiceUaTotalPriceInput) {
            detailVehicleInvoiceUaTotalPriceInput.value = total.toFixed(6);
        }
    } else if (detailVehicleInvoiceUaTotalPriceInput) {
        detailVehicleInvoiceUaTotalPriceInput.value = '';
    }
}

function calculateDetailInvoiceEuTotalPrice() {
    const quantity = parseFloat(detailVehicleProductQuantityInput?.value?.replace(',', '.') || '0');
    const pricePerTon = parseFloat(detailVehicleInvoiceEuPricePerTonInput?.value || '0');
    if (quantity > 0 && pricePerTon > 0) {
        const total = quantity * pricePerTon;
        if (detailVehicleInvoiceEuTotalPriceInput) {
            detailVehicleInvoiceEuTotalPriceInput.value = total.toFixed(6);
        }
    } else if (detailVehicleInvoiceEuTotalPriceInput) {
        detailVehicleInvoiceEuTotalPriceInput.value = '';
    }
}

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
    
    if (!confirm('Ви впевнені, що хочете видалити цю машину?')) {
        return;
    }
    
    (async () => {
    try {
        const response = await fetch(`/api/v1/vehicles/${currentVehicleId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            throw new Error('Failed to delete vehicle');
        }
        
        showMessage('Машину успішно видалено', 'success');
        closeModal('vehicle-details-modal');
            await loadVehicles(0);
    } catch (error) {
        showMessage('Помилка при видаленні машини', 'error');
    }
    })();
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
        }, 500);
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
            destinationCountry: detailVehicleDestinationCountryInput?.value ?? null,
            destinationPlace: detailVehicleDestinationPlaceInput?.value ?? null,
            product: detailVehicleProductInput?.value ?? null,
            productQuantity: detailVehicleProductQuantityInput?.value ?? null,
            declarationNumber: detailVehicleDeclarationNumberInput?.value ?? null,
            terminal: detailVehicleTerminalInput?.value ?? null,
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
            const response = await fetch(`/api/v1/vehicles/${currentVehicleId}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Не вдалося оновити машину');
            }
            
            const updatedVehicle = await response.json();
            currentVehicleDetails = updatedVehicle;
            renderVehicleDetails(updatedVehicle);
            
            const expensesTotal = parseFloat(await getVehicleExpensesTotal()) || 0;
            const productsTotalCost = calculateProductsTotalCost(updatedVehicle);
            const totalExpenses = productsTotalCost + expensesTotal;
            
            const invoiceEuTotalPrice = updatedVehicle.invoiceEuTotalPrice || 0;
            const fullReclamation = calculateFullReclamation(updatedVehicle);
            const totalIncome = invoiceEuTotalPrice - fullReclamation;
            const margin = totalIncome - totalExpenses;
            
            if (vehicleTotalExpenses) {
                vehicleTotalExpenses.textContent = formatNumber(totalExpenses, 2);
            }
            if (vehicleTotalIncome) {
                vehicleTotalIncome.textContent = formatNumber(totalIncome, 2);
            }
            if (vehicleMargin) {
                vehicleMargin.textContent = formatNumber(margin, 2);
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

function openEditVehicleItemModal(itemId) {
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
}


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

            if (roundedQuantity === 0) {
                const productLabel = item.productName || 'товар';
                const confirmRemoval = confirm(`Ви впевнені, що хочете повністю видалити ${productLabel} з машини?`);
                if (!confirmRemoval) {
                    return;
                }
            }

            payload.quantity = roundedQuantity;
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
        } else {
            showMessage('Оберіть параметр для редагування', 'error');
            return;
        }

        try {
            const response = await fetch(`/api/v1/vehicles/${currentVehicleId}/products/${currentVehicleItemId}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || 'Не вдалося оновити товар у машині');
            }

            const updatedVehicle = await response.json();
            currentVehicleDetails = updatedVehicle;
            renderVehicleDetails(updatedVehicle);
            
            const expensesTotal = parseFloat(await getVehicleExpensesTotal()) || 0;
            const productsTotalCost = calculateProductsTotalCost(updatedVehicle);
            const totalExpenses = productsTotalCost + expensesTotal;
            
            const invoiceEuTotalPrice = updatedVehicle.invoiceEuTotalPrice || 0;
            const fullReclamation = calculateFullReclamation(updatedVehicle);
            const totalIncome = invoiceEuTotalPrice - fullReclamation;
            const margin = totalIncome - totalExpenses;
            
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
            showMessage(error.message || 'Помилка при оновленні товару у машині', 'error');
        }
    });
}

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('open');
        document.body.classList.add('modal-open');
    }
}

window.openModal = openModal;

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (!modal) return;
    
    modal.classList.remove('open');
    document.body.classList.remove('modal-open');
    
    if (modalId === 'create-vehicle-modal') {
        createVehicleForm?.reset();
    } else if (modalId === 'vehicle-details-modal') {
        resetVehicleFormState();
    } else if (modalId === 'edit-vehicle-item-modal') {
        if (editVehicleItemForm) {
            editVehicleItemForm.reset();
        }
        currentVehicleItemId = null;
        updateVehicleItemMode();
    } else if (modalId === 'edit-vehicle-expense-modal') {
        if (editVehicleExpenseForm) {
            editVehicleExpenseForm.reset();
        }
        currentExpenseId = null;
    } else if (modalId === 'carrier-form-modal') {
        carrierForm?.reset();
    }
}

window.closeModal = closeModal;

function initializeModalClickHandlers() {
    const modals = [
        document.getElementById('vehicles-filter-modal'),
        document.getElementById('create-vehicle-modal'),
        document.getElementById('vehicle-details-modal'),
        document.getElementById('create-vehicle-expense-modal'),
        document.getElementById('edit-vehicle-expense-modal'),
        document.getElementById('edit-vehicle-item-modal'),
        document.getElementById('manage-carriers-modal'),
        document.getElementById('carrier-form-modal')
    ];

    modals.forEach(modal => {
        if (!modal) return;

        if (modal._modalClickHandler) {
            modal.removeEventListener('click', modal._modalClickHandler);
            modal._modalClickHandler = null;
        }
    });

    document.querySelectorAll('.modal-close').forEach(btn => {
        if (btn._closeModalHandler) {
            btn.removeEventListener('click', btn._closeModalHandler);
        }

        btn._closeModalHandler = () => {
            const modal = btn.closest('.modal-overlay');
            if (modal) {
                closeModal(modal.id);
            }
        };

        btn.addEventListener('click', btn._closeModalHandler);
    });
}

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

function setDefaultVehicleDates() {
    const today = new Date();
    const last30Days = new Date();
    last30Days.setDate(today.getDate() - 30);
    const formattedToday = today.toISOString().split('T')[0];
    const formattedFrom = last30Days.toISOString().split('T')[0];

    if (vehiclesDateFromFilter && !vehiclesDateFromFilter.value) {
        vehiclesDateFromFilter.value = formattedFrom;
    }
    if (vehiclesDateToFilter && !vehiclesDateToFilter.value) {
        vehiclesDateToFilter.value = formattedToday;
    }
}

async function loadCarriers() {
    const carriers = await fetchCarriers();
    const tbody = document.getElementById('carriers-tbody');
    if (!tbody) return;
    
    tbody.textContent = '';
    
    if (!carriers || carriers.length === 0) {
        const row = document.createElement('tr');
        row.className = 'loading-row';
        const cell = document.createElement('td');
        cell.colSpan = 6;
        cell.style.textAlign = 'center';
        cell.style.color = 'var(--text-muted)';
        cell.textContent = 'Немає перевізників';
        row.appendChild(cell);
        tbody.appendChild(row);
        return;
    }
    
    carriers.forEach(carrier => {
        const row = document.createElement('tr');
        
        const createCell = (text, label) => {
            const cell = document.createElement('td');
            cell.setAttribute('data-label', label);
            cell.textContent = text || '-';
            return cell;
        };
        
        row.appendChild(createCell(carrier.companyName, 'Назва компанії'));
        row.appendChild(createCell(carrier.registrationAddress, 'Адреса реєстрації'));
        row.appendChild(createCell(carrier.phoneNumber, 'Телефон'));
        row.appendChild(createCell(carrier.code, 'Код'));
        row.appendChild(createCell(carrier.account, 'Рахунок'));
        
        const actionsCell = document.createElement('td');
        actionsCell.setAttribute('data-label', 'Дії');
        const actionsDiv = document.createElement('div');
        actionsDiv.className = 'action-buttons';
        
        const editBtn = document.createElement('button');
        editBtn.className = 'btn btn-secondary btn-sm';
        editBtn.textContent = '✏️ Редагувати';
        const editBtnHandler = () => editCarrier(carrier.id);
        editBtn.addEventListener('click', editBtnHandler);
        editBtn._clickHandler = editBtnHandler;
        actionsDiv.appendChild(editBtn);
        
        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'btn btn-danger btn-sm';
        deleteBtn.textContent = '🗑️ Видалити';
        const deleteBtnHandler = () => deleteCarrier(carrier.id);
        deleteBtn.addEventListener('click', deleteBtnHandler);
        deleteBtn._clickHandler = deleteBtnHandler;
        actionsDiv.appendChild(deleteBtn);
        
        actionsCell.appendChild(actionsDiv);
        row.appendChild(actionsCell);
        
        tbody.appendChild(row);
    });
}

document.getElementById('manage-carriers-btn')?.addEventListener('click', async () => {
    await loadCarriers();
    openModal('manage-carriers-modal');
});

document.getElementById('create-carrier-btn')?.addEventListener('click', () => {
    if (carrierFormTitle) carrierFormTitle.textContent = '➕ Створити перевізника';
    if (carrierForm) carrierForm.reset();
    if (carrierId) carrierId.value = '';
    openModal('carrier-form-modal');
});

async function editCarrier(id) {
    const carrier = carrierMap.get(id);
    if (!carrier) {
        showMessage('Перевізник не знайдений', 'error');
        return;
    }
    
    if (carrierFormTitle) carrierFormTitle.textContent = '✏️ Редагувати перевізника';
    if (carrierId) carrierId.value = carrier.id;
    if (carrierCompanyName) carrierCompanyName.value = carrier.companyName || '';
    if (carrierRegistrationAddress) carrierRegistrationAddress.value = carrier.registrationAddress || '';
    if (carrierPhoneNumber) carrierPhoneNumber.value = carrier.phoneNumber || '';
    if (carrierCode) carrierCode.value = carrier.code || '';
    if (carrierAccount) carrierAccount.value = carrier.account || '';
    openModal('carrier-form-modal');
}

async function deleteCarrier(carrierId) {
    if (!confirm('Ви впевнені, що хочете видалити цього перевізника?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/v1/carriers/${carrierId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            throw new Error('Failed to delete carrier');
        }
        
        showMessage('Перевізника успішно видалено', 'success');
        await loadCarriers();
        await fetchCarriers();
        await fetchVehicleSenders();
        await fetchVehicleReceivers();
        populateCarriers('vehicle-carrier-id');
        populateCarriers('detail-vehicle-carrier-id');
        populateVehicleSenders('vehicle-sender');
        populateVehicleSenders('detail-vehicle-sender');
        populateVehicleReceivers('vehicle-receiver');
        populateVehicleReceivers('detail-vehicle-receiver');
    } catch (error) {
        showMessage('Помилка при видаленні перевізника', 'error');
    }
}

if (carrierForm) {
    carrierForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const id = carrierId?.value;
        const carrierData = {
            companyName: carrierCompanyName?.value || '',
            registrationAddress: carrierRegistrationAddress?.value || '',
            phoneNumber: carrierPhoneNumber?.value || '',
            code: carrierCode?.value || '',
            account: carrierAccount?.value || ''
        };
    
        try {
            let response;
            if (id) {
                response = await fetch(`/api/v1/carriers/${id}`, {
                    method: 'PUT',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(carrierData)
                });
            } else {
                response = await fetch('/api/v1/carriers', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(carrierData)
                });
            }
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to save carrier');
            }
            
            showMessage(id ? 'Перевізника успішно оновлено' : 'Перевізника успішно створено', 'success');
            closeModal('carrier-form-modal');
            await loadCarriers();
            await fetchCarriers();
            populateCarriers('vehicle-carrier-id');
            populateCarriers('detail-vehicle-carrier-id');
        } catch (error) {
            showMessage(error.message || 'Помилка при збереженні перевізника', 'error');
        }
    });
}

document.getElementById('cancel-carrier-btn')?.addEventListener('click', () => {
    closeModal('carrier-form-modal');
});

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
            const newWidth = Math.max(50, startWidth + diff);
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
                const width = Math.max(50, currentResizeHeader.offsetWidth);
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
    populateCarriers('vehicle-carrier-id');
    populateCarriers('detail-vehicle-carrier-id');
    populateVehicleSenders('vehicle-sender');
    populateVehicleSenders('detail-vehicle-sender');
    populateVehicleReceivers('vehicle-receiver');
    populateVehicleReceivers('detail-vehicle-receiver');
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
        const response = await fetch('/api/v1/accounts');
        if (!response.ok) {
            throw new Error('Failed to load accounts');
        }
        accountsCache = await response.json();
    } catch (error) {
        console.error('Error loading accounts:', error);
        handleError(error);
    }
}

function populateAccounts(selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.textContent = '';
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = 'Оберіть рахунок';
    select.appendChild(defaultOption);
    accountsCache.forEach(account => {
        const option = document.createElement('option');
        option.value = account.id;
        option.textContent = account.name || `Рахунок #${account.id}`;
        select.appendChild(option);
    });
}

async function loadCategoriesForVehicleExpense() {
    try {
        const response = await fetch('/api/v1/transaction-categories/type/VEHICLE_EXPENSE');
        if (!response.ok) {
            throw new Error('Failed to load categories');
        }
        const categories = await response.json();
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

function populateCategories(selectId, categories) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.textContent = '';
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = 'Оберіть категорію';
    select.appendChild(defaultOption);
    if (categories && categories.length > 0) {
        categories.forEach(category => {
            const option = document.createElement('option');
            option.value = category.id;
            option.textContent = category.name;
            select.appendChild(option);
        });
    }
}

function populateCurrencies(selectId, accountId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.textContent = '';
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = 'Оберіть валюту';
    select.appendChild(defaultOption);
    
    if (!accountId) return;
    
    const account = accountsCache.find(a => a.id === parseInt(accountId));
    if (account && account.currencies) {
        account.currencies.forEach(currency => {
            const option = document.createElement('option');
            option.value = currency;
            option.textContent = currency;
            select.appendChild(option);
        });
    }
}

async function loadVehicleExpenses(vehicleId) {
    if (!vehicleExpensesTbody) return;
    
    try {
        if (!categoriesCache.has('VEHICLE_EXPENSE') || categoriesCache.get('VEHICLE_EXPENSE')?.length === 0) {
            await loadCategoriesForVehicleExpense();
        }
        
        const response = await fetch(`/api/v1/vehicles/${vehicleId}/expenses`);
        if (!response.ok) {
            throw new Error('Failed to load vehicle expenses');
        }
        
        const expenses = await response.json();
        
        vehicleExpensesTbody.textContent = '';
        
        if (!expenses || expenses.length === 0) {
            const row = document.createElement('tr');
            row.className = 'loading-row';
            const cell = document.createElement('td');
            cell.colSpan = 9;
            cell.style.textAlign = 'center';
            cell.style.color = 'var(--text-muted)';
            cell.textContent = 'Немає витрат';
            row.appendChild(cell);
            vehicleExpensesTbody.appendChild(row);
            return;
        }
        
        const accountMap = new Map(accountsCache.map(a => [a.id, a]));
        
        expenses.forEach(expense => {
            const account = accountMap.get(expense.fromAccountId);
            const accountName = account ? (account.name || `Рахунок #${account.id}`) : '-';
            const date = expense.createdAt ? new Date(expense.createdAt).toLocaleDateString('uk-UA') : '-';
            const categoryName = expense.categoryId ? (categoryNameMap.get(expense.categoryId) || 'Категорія') : '-';
            const exchangeRate = expense.exchangeRate ? formatNumber(expense.exchangeRate, 6) : '-';
            const convertedAmount = expense.convertedAmount ? formatNumber(expense.convertedAmount, 2) : '-';
            
            const row = document.createElement('tr');
            
            const createCell = (text, label) => {
                const cell = document.createElement('td');
                cell.setAttribute('data-label', label);
                cell.textContent = text || '-';
                return cell;
            };
            
            row.appendChild(createCell(date, 'Дата'));
            row.appendChild(createCell(formatNumber(expense.amount, 2), 'Сума'));
            row.appendChild(createCell(expense.currency, 'Валюта'));
            row.appendChild(createCell(exchangeRate, 'Курс'));
            row.appendChild(createCell(convertedAmount, 'Сума в EUR'));
            row.appendChild(createCell(categoryName, 'Категорія'));
            row.appendChild(createCell(accountName, 'Рахунок'));
            row.appendChild(createCell(expense.description, 'Опис'));
            
            const actionsCell = document.createElement('td');
            actionsCell.setAttribute('data-label', 'Дії');
            const editBtn = document.createElement('button');
            editBtn.className = 'btn btn-sm btn-primary';
            editBtn.textContent = 'Редагувати';
            editBtn.onclick = () => openEditVehicleExpenseModal(expense);
            actionsCell.appendChild(editBtn);
            row.appendChild(actionsCell);
            
            vehicleExpensesTbody.appendChild(row);
        });
    } catch (error) {
        console.error('Error loading vehicle expenses:', error);
        vehicleExpensesTbody.textContent = '';
        const row = document.createElement('tr');
        row.className = 'loading-row';
        const cell = document.createElement('td');
        cell.colSpan = 9;
        cell.style.textAlign = 'center';
        cell.style.color = 'var(--danger)';
        cell.textContent = 'Помилка завантаження витрат';
        row.appendChild(cell);
        vehicleExpensesTbody.appendChild(row);
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
            const response = await fetch(`/api/v1/vehicles/${currentVehicleId}/expenses`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(formData)
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to create vehicle expense');
            }
            
            showMessage('Витрату успішно створено', 'success');
            closeModal('create-vehicle-expense-modal');
            createVehicleExpenseForm.reset();
            
            await loadVehicleExpenses(currentVehicleId);
            
            if (currentVehicleDetails) {
                const expensesTotal = parseFloat(await getVehicleExpensesTotal()) || 0;
                const productsTotalCost = calculateProductsTotalCost(currentVehicleDetails);
                const totalExpenses = productsTotalCost + expensesTotal;
                
                const invoiceEuTotalPrice = currentVehicleDetails.invoiceEuTotalPrice || 0;
                const fullReclamation = calculateFullReclamation(currentVehicleDetails);
                const totalIncome = invoiceEuTotalPrice - fullReclamation;
                const margin = totalIncome - totalExpenses;
                
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

async function openEditVehicleExpenseModal(expense) {
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
                const response = await fetch(`/api/v1/exchange-rates/${currency}/EUR`);
                if (response.ok) {
                    const data = await response.json();
                    if (data.rate) {
                        const convertedAmount = amount / data.rate;
                    }
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
            const response = await fetch(`/api/v1/vehicles/expenses/${currentExpenseId}`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(formData)
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to update vehicle expense');
            }
            
            showMessage('Витрату успішно оновлено', 'success');
            closeModal('edit-vehicle-expense-modal');
            editVehicleExpenseForm.reset();
            currentExpenseId = null;
            
            await loadVehicleExpenses(currentVehicleId);
            
            if (currentVehicleDetails) {
                const expensesTotal = parseFloat(await getVehicleExpensesTotal()) || 0;
                const productsTotalCost = calculateProductsTotalCost(currentVehicleDetails);
                const totalExpenses = productsTotalCost + expensesTotal;
                
                const invoiceEuTotalPrice = currentVehicleDetails.invoiceEuTotalPrice || 0;
                const fullReclamation = calculateFullReclamation(currentVehicleDetails);
                const totalIncome = invoiceEuTotalPrice - fullReclamation;
                const margin = totalIncome - totalExpenses;
                
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
        
        let url = `/api/v1/vehicles/export`;
        const params = new URLSearchParams();
        
        if (searchTerm) {
            params.append('q', searchTerm);
        }
        
        if (filtersJson) {
            params.append('filters', filtersJson);
        }
        
        if (params.toString()) {
            url += '?' + params.toString();
        }
        
        const response = await fetch(url);
        
        if (!response.ok) {
            throw new Error('Failed to export vehicles');
        }
        
        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = `vehicles_${new Date().toISOString().split('T')[0]}.xlsx`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);
        
        showMessage('Експорт успішно виконано', 'success');
    } catch (error) {
        console.error('Error exporting vehicles:', error);
        showMessage('Помилка експорту: ' + error.message, 'error');
    }
});

initialize();

