let productMap = new Map();
let warehouseMap = new Map();
let userMap = new Map();
let carrierMap = new Map();

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

function populateProducts(selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.innerHTML = '<option value="">Оберіть продукт</option>';
    for (const [id, name] of productMap.entries()) {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = name;
        select.appendChild(option);
    }
}

function populateWarehouses(selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.innerHTML = '<option value="">Оберіть склад</option>';
    for (const [id, name] of warehouseMap.entries()) {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = name;
        select.appendChild(option);
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

function populateCarriers(selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.innerHTML = '<option value="">Оберіть перевізника</option>';
    for (const [id, carrier] of carrierMap.entries()) {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = carrier.companyName;
        select.appendChild(option);
    }
}

const createVehicleBtn = document.getElementById('create-vehicle-btn');
const createVehicleForm = document.getElementById('create-vehicle-form');
const addProductToVehicleForm = document.getElementById('add-product-to-vehicle-form');
const updateVehicleForm = document.getElementById('update-vehicle-form');
const detailVehicleDateInput = document.getElementById('detail-vehicle-date');
const detailVehicleVehicleInput = document.getElementById('detail-vehicle-vehicle-number');
const detailVehicleInvoiceUaInput = document.getElementById('detail-vehicle-invoice-ua');
const detailVehicleInvoiceEuInput = document.getElementById('detail-vehicle-invoice-eu');
const detailVehicleDescriptionInput = document.getElementById('detail-vehicle-description');
const detailVehicleSenderInput = document.getElementById('detail-vehicle-sender');
const detailVehicleReceiverInput = document.getElementById('detail-vehicle-receiver');
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
const editVehicleBtn = document.getElementById('edit-vehicle-btn');
const saveVehicleBtn = document.getElementById('save-vehicle-btn');
const editVehicleItemModal = document.getElementById('edit-vehicle-item-modal');
const editVehicleItemForm = document.getElementById('edit-vehicle-item-form');
const editVehicleItemQuantityInput = document.getElementById('edit-vehicle-item-quantity');
const editVehicleItemTotalCostInput = document.getElementById('edit-vehicle-item-total-cost');
const editVehicleItemModeRadios = document.querySelectorAll('input[name="edit-vehicle-item-mode"]');

let currentVehicleId = null;
let vehiclesCache = [];
let currentVehicleDetails = null;
let currentVehicleItems = new Map();
let currentVehicleItemId = null;

function populateVehicleForm(vehicle) {
    if (!vehicle) {
        if (detailVehicleDateInput) detailVehicleDateInput.value = '';
        if (detailVehicleVehicleInput) detailVehicleVehicleInput.value = '';
        if (detailVehicleInvoiceUaInput) detailVehicleInvoiceUaInput.value = '';
        if (detailVehicleInvoiceEuInput) detailVehicleInvoiceEuInput.value = '';
        if (detailVehicleDescriptionInput) detailVehicleDescriptionInput.value = '';
        if (detailVehicleSenderInput) detailVehicleSenderInput.value = '';
        if (detailVehicleReceiverInput) detailVehicleReceiverInput.value = '';
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
        return;
    }

    if (detailVehicleDateInput) detailVehicleDateInput.value = vehicle.shipmentDate || '';
    if (detailVehicleVehicleInput) detailVehicleVehicleInput.value = vehicle.vehicleNumber || '';
    if (detailVehicleInvoiceUaInput) detailVehicleInvoiceUaInput.value = vehicle.invoiceUa || '';
    if (detailVehicleInvoiceEuInput) detailVehicleInvoiceEuInput.value = vehicle.invoiceEu || '';
    if (detailVehicleDescriptionInput) detailVehicleDescriptionInput.value = vehicle.description || '';
    if (detailVehicleSenderInput) detailVehicleSenderInput.value = vehicle.sender || '';
    if (detailVehicleReceiverInput) detailVehicleReceiverInput.value = vehicle.receiver || '';
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
}

function setVehicleFormEditable(isEditable) {
    const fields = [
        detailVehicleDateInput,
        detailVehicleVehicleInput,
        detailVehicleInvoiceUaInput,
        detailVehicleInvoiceEuInput,
        detailVehicleDescriptionInput,
        detailVehicleSenderInput,
        detailVehicleReceiverInput,
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
        openModal('create-vehicle-modal');
    });
}

if (createVehicleForm) {
    createVehicleForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const carrierIdValue = document.getElementById('vehicle-carrier-id').value;
        const vehicleData = {
            vehicleNumber: document.getElementById('vehicle-vehicle-number').value,
            invoiceUa: document.getElementById('vehicle-invoice-ua').value,
            invoiceEu: document.getElementById('vehicle-invoice-eu').value,
            description: document.getElementById('vehicle-description').value,
            sender: document.getElementById('vehicle-sender').value,
            receiver: document.getElementById('vehicle-receiver').value,
            destinationCountry: document.getElementById('vehicle-destination-country').value,
            destinationPlace: document.getElementById('vehicle-destination-place').value,
            product: document.getElementById('vehicle-product').value,
            productQuantity: document.getElementById('vehicle-product-quantity').value,
            declarationNumber: document.getElementById('vehicle-declaration-number').value,
            terminal: document.getElementById('vehicle-terminal').value,
            driverFullName: document.getElementById('vehicle-driver-full-name').value,
            eur1: document.getElementById('vehicle-eur1').checked,
            fito: document.getElementById('vehicle-fito').checked,
            customsDate: document.getElementById('vehicle-customs-date').value || null,
            customsClearanceDate: document.getElementById('vehicle-customs-clearance-date').value || null,
            unloadingDate: document.getElementById('vehicle-unloading-date').value || null,
            carrierId: carrierIdValue ? Number(carrierIdValue) : null,
            isOurVehicle: document.getElementById('vehicle-is-our-vehicle').checked
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
            
            const result = await response.json();
            showMessage('Машину успішно створено', 'success');
            
            closeModal('create-vehicle-modal');
            document.getElementById('create-vehicle-form')?.reset();
            
            await loadVehicles();
        } catch (error) {
            showMessage(error.message || 'Помилка при створенні машини', 'error');
        }
    });
}

function searchInVehicle(vehicle, searchTerm) {
    if (!searchTerm || searchTerm.trim() === '') return true;
    
    const term = searchTerm.toLowerCase().trim();
    const searchableFields = [
        vehicle.shipmentDate?.toString() || '',
        vehicle.vehicleNumber || '',
        vehicle.invoiceUa || '',
        vehicle.invoiceEu || '',
        vehicle.sender || '',
        vehicle.receiver || '',
        vehicle.destinationCountry || '',
        vehicle.destinationPlace || '',
        vehicle.product || '',
        vehicle.productQuantity || '',
        vehicle.declarationNumber || '',
        vehicle.terminal || '',
        vehicle.driverFullName || '',
        vehicle.description || '',
        formatCarrier(vehicle.carrier),
        formatNumber(vehicle.totalCostEur, 2)
    ];
    
    return searchableFields.some(field => field.toLowerCase().includes(term));
}

function filterVehicles(vehicles) {
    if (!vehicles || vehicles.length === 0) return vehicles;
    
    let filtered = [...vehicles];
    
    const searchTerm = document.getElementById('vehicles-search-input')?.value || '';
    if (searchTerm) {
        filtered = filtered.filter(vehicle => searchInVehicle(vehicle, searchTerm));
    }
    
    const isOurVehicleFilter = document.getElementById('vehicles-is-our-vehicle-filter')?.checked;
    if (isOurVehicleFilter !== undefined && isOurVehicleFilter) {
        filtered = filtered.filter(vehicle => vehicle.isOurVehicle === true);
    }
    
    const customsDateFrom = document.getElementById('vehicles-customs-date-from-filter')?.value;
    const customsDateTo = document.getElementById('vehicles-customs-date-to-filter')?.value;
    if (customsDateFrom || customsDateTo) {
        filtered = filtered.filter(vehicle => {
            if (!vehicle.customsDate) return false;
            const date = new Date(vehicle.customsDate);
            if (customsDateFrom && date < new Date(customsDateFrom)) return false;
            if (customsDateTo && date > new Date(customsDateTo)) return false;
            return true;
        });
    }
    
    const customsClearanceDateFrom = document.getElementById('vehicles-customs-clearance-date-from-filter')?.value;
    const customsClearanceDateTo = document.getElementById('vehicles-customs-clearance-date-to-filter')?.value;
    if (customsClearanceDateFrom || customsClearanceDateTo) {
        filtered = filtered.filter(vehicle => {
            if (!vehicle.customsClearanceDate) return false;
            const date = new Date(vehicle.customsClearanceDate);
            if (customsClearanceDateFrom && date < new Date(customsClearanceDateFrom)) return false;
            if (customsClearanceDateTo && date > new Date(customsClearanceDateTo)) return false;
            return true;
        });
    }
    
    const unloadingDateFrom = document.getElementById('vehicles-unloading-date-from-filter')?.value;
    const unloadingDateTo = document.getElementById('vehicles-unloading-date-to-filter')?.value;
    if (unloadingDateFrom || unloadingDateTo) {
        filtered = filtered.filter(vehicle => {
            if (!vehicle.unloadingDate) return false;
            const date = new Date(vehicle.unloadingDate);
            if (unloadingDateFrom && date < new Date(unloadingDateFrom)) return false;
            if (unloadingDateTo && date > new Date(unloadingDateTo)) return false;
            return true;
        });
    }
    
    return filtered;
}

async function loadVehicles() {
    const dateFrom = document.getElementById('vehicles-date-from-filter')?.value;
    const dateTo = document.getElementById('vehicles-date-to-filter')?.value;
    
    try {
        let url = '/api/v1/vehicles/all/by-date-range?';
        
        if (dateFrom && dateTo) {
            url += `fromDate=${dateFrom}&toDate=${dateTo}`;
        } else {
            const today = new Date();
            const last30Days = new Date();
            last30Days.setDate(today.getDate() - 30);
            url += `fromDate=${last30Days.toISOString().split('T')[0]}&toDate=${today.toISOString().split('T')[0]}`;
        }
        
        const response = await fetch(url);
        
        if (!response.ok) {
            throw new Error('Failed to load vehicles');
        }
        
        vehiclesCache = await response.json();
        const filtered = filterVehicles(vehiclesCache);
        renderVehicles(filtered);
    } catch (error) {
        showMessage('Помилка завантаження машин', 'error');
        
        const tbody = document.getElementById('vehicles-tbody');
        if (tbody) {
            tbody.innerHTML = '<tr class="loading-row"><td colspan="21" style="text-align: center; color: var(--text-muted);">Помилка завантаження даних</td></tr>';
        }
    }
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

function renderVehicles(vehicles) {
    const tbody = document.getElementById('vehicles-tbody');
    const countElement = document.getElementById('vehicles-count');
    
    if (!tbody) {
        return;
    }
    
    if (countElement) {
        countElement.textContent = vehicles && vehicles.length > 0 ? `${vehicles.length} ${vehicles.length === 1 ? 'машина' : 'машин'}` : '0 машин';
    }
    
    if (!vehicles || vehicles.length === 0) {
        tbody.innerHTML = '<tr class="loading-row"><td colspan="21" style="text-align: center; color: var(--text-muted);">Немає даних</td></tr>';
        return;
    }
    
    tbody.innerHTML = vehicles.map(vehicle => `
        <tr onclick="viewVehicleDetails(${vehicle.id})">
            <td style="font-weight: 600; color: var(--primary);">${formatNumber(vehicle.totalCostEur, 2)} EUR</td>
            <td>${formatDate(vehicle.shipmentDate)}</td>
            <td>${vehicle.vehicleNumber || '-'}</td>
            <td>${vehicle.invoiceUa || '-'}</td>
            <td>${vehicle.invoiceEu || '-'}</td>
            <td>${formatBoolean(vehicle.isOurVehicle)}</td>
            <td>${vehicle.sender || '-'}</td>
            <td>${vehicle.receiver || '-'}</td>
            <td>${vehicle.destinationCountry || '-'}</td>
            <td>${vehicle.destinationPlace || '-'}</td>
            <td>${vehicle.product || '-'}</td>
            <td>${vehicle.productQuantity || '-'}</td>
            <td>${vehicle.declarationNumber || '-'}</td>
            <td>${vehicle.terminal || '-'}</td>
            <td>${vehicle.driverFullName || '-'}</td>
            <td>${formatBoolean(vehicle.eur1)}</td>
            <td>${formatBoolean(vehicle.fito)}</td>
            <td>${formatDate(vehicle.customsDate)}</td>
            <td>${formatDate(vehicle.customsClearanceDate)}</td>
            <td>${formatDate(vehicle.unloadingDate)}</td>
            <td>${formatCarrier(vehicle.carrier)}</td>
            <td>${vehicle.description || '-'}</td>
        </tr>
    `).join('');
    
    applySavedColumnWidths();
}

function applySavedColumnWidths() {
    const table = document.getElementById('vehicles-table');
    if (!table) return;
    
    const headers = table.querySelectorAll('.resizable-header');
    headers.forEach(header => {
        const column = header.dataset.column;
        const savedWidth = localStorage.getItem(`vehicle-column-width-${column}`);
        if (savedWidth) {
            const width = parseInt(savedWidth);
            header.style.minWidth = width + 'px';
            header.style.width = width + 'px';
            const index = Array.from(header.parentElement.children).indexOf(header);
            const cells = table.querySelectorAll(`tbody tr td:nth-child(${index + 1})`);
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
        renderVehicleDetails(vehicle);
        
        document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
        document.querySelector('.tab-btn[data-tab="info"]')?.classList.add('active');
        document.getElementById('tab-info')?.classList.add('active');
        
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
    
    const itemsTbody = document.getElementById('vehicle-items-tbody');
    
    if (!vehicle.items || vehicle.items.length === 0) {
        itemsTbody.innerHTML = '<tr class="loading-row"><td colspan="6" style="text-align: center; color: var(--text-muted);">Товари ще не додані</td></tr>';
    } else {
        itemsTbody.innerHTML = vehicle.items.map(item => {
            const productName = findNameByIdFromMap(productMap, item.productId) || 'Невідомий товар';
            const warehouseName = findNameByIdFromMap(warehouseMap, item.warehouseId) || 'Невідомий склад';

            currentVehicleItems.set(Number(item.withdrawalId), {
                ...item,
                productName,
                warehouseName
            });

            return `
                <tr class="vehicle-item-row" data-item-id="${item.withdrawalId}">
                    <td>${productName}</td>
                    <td>${warehouseName}</td>
                    <td>${formatNumber(item.quantity, 2)} кг</td>
                    <td style="text-align: right;">${formatNumber(item.unitPriceEur, 6)} EUR</td>
                    <td style="text-align: right; font-weight: 600; color: var(--primary);">${formatNumber(item.totalCostEur, 6)} EUR</td>
                    <td>${item.withdrawalDate || vehicle.shipmentDate}</td>
                </tr>
            `;
        }).join('');
    }
    
    document.getElementById('vehicle-total-cost').textContent = formatNumber(vehicle.totalCostEur, 2);
}

document.getElementById('add-product-to-vehicle-btn')?.addEventListener('click', () => {
    populateWarehouses('vehicle-warehouse-id');
    populateProducts('vehicle-product-id');
    openModal('add-product-to-vehicle-modal');
});

if (addProductToVehicleForm) {
    addProductToVehicleForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const data = {
            warehouseId: Number(document.getElementById('vehicle-warehouse-id').value),
            productId: Number(document.getElementById('vehicle-product-id').value),
            quantity: Number(document.getElementById('vehicle-quantity').value)
        };
        
        try {
            const response = await fetch(`/api/v1/vehicles/${currentVehicleId}/products`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(data)
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to add product to vehicle');
            }
            
            const updatedVehicle = await response.json();
            
            showMessage('Товар успішно додано до машини', 'success');
            closeModal('add-product-to-vehicle-modal');
            document.getElementById('add-product-to-vehicle-form')?.reset();
            
            renderVehicleDetails(updatedVehicle);
            await loadVehicles();
        } catch (error) {
            showMessage(error.message || 'Помилка при додаванні товару до машини', 'error');
        }
    });
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
            await loadVehicles();
        } catch (error) {
            showMessage('Помилка при видаленні машини', 'error');
        }
    })();
}

document.getElementById('delete-vehicle-btn')?.addEventListener('click', deleteVehicle);
document.getElementById('delete-vehicle-from-details-btn')?.addEventListener('click', deleteVehicle);

document.getElementById('apply-vehicles-filters')?.addEventListener('click', async () => {
    await loadVehicles();
    closeModal('vehicles-filter-modal');
});

document.getElementById('clear-vehicles-filters')?.addEventListener('click', () => {
    document.getElementById('vehicles-date-from-filter').value = '';
    document.getElementById('vehicles-date-to-filter').value = '';
    document.getElementById('vehicles-customs-date-from-filter').value = '';
    document.getElementById('vehicles-customs-date-to-filter').value = '';
    document.getElementById('vehicles-customs-clearance-date-from-filter').value = '';
    document.getElementById('vehicles-customs-clearance-date-to-filter').value = '';
    document.getElementById('vehicles-unloading-date-from-filter').value = '';
    document.getElementById('vehicles-unloading-date-to-filter').value = '';
    document.getElementById('vehicles-is-our-vehicle-filter').checked = false;
    const searchInput = document.getElementById('vehicles-search-input');
    if (searchInput) {
        searchInput.value = '';
    }
    setDefaultVehicleDates();
    loadVehicles();
});

document.getElementById('vehicles-search-input')?.addEventListener('input', () => {
    if (vehiclesCache && vehiclesCache.length > 0) {
        const filtered = filterVehicles(vehiclesCache);
        renderVehicles(filtered);
    }
});

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
            description: detailVehicleDescriptionInput?.value ?? null,
            sender: detailVehicleSenderInput?.value ?? null,
            receiver: detailVehicleReceiverInput?.value ?? null,
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
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Не вдалося оновити машину');
            }
            
            const updatedVehicle = await response.json();
            showMessage('Дані машини оновлено', 'success');
            renderVehicleDetails(updatedVehicle);
            await loadVehicles();
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

const vehicleItemsTbody = document.getElementById('vehicle-items-tbody');
if (vehicleItemsTbody) {
    vehicleItemsTbody.addEventListener('click', (event) => {
        const row = event.target.closest('tr[data-item-id]');
        if (!row) {
            return;
        }
        const itemId = row.dataset.itemId;
        if (itemId) {
            openEditVehicleItemModal(itemId);
        }
    });
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
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || 'Не вдалося оновити товар у машині');
            }

            const updatedVehicle = await response.json();
            showMessage('Дані товару у машині оновлено', 'success');
            renderVehicleDetails(updatedVehicle);
            await loadVehicles();
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
        document.getElementById('create-vehicle-form')?.reset();
    } else if (modalId === 'add-product-to-vehicle-modal') {
        document.getElementById('add-product-to-vehicle-form')?.reset();
    } else if (modalId === 'vehicle-details-modal') {
        resetVehicleFormState();
    } else if (modalId === 'edit-vehicle-item-modal') {
        if (editVehicleItemForm) {
            editVehicleItemForm.reset();
        }
        currentVehicleItemId = null;
        updateVehicleItemMode();
    } else if (modalId === 'carrier-form-modal') {
        document.getElementById('carrier-form')?.reset();
    }
}

window.closeModal = closeModal;

document.querySelectorAll('.modal-close').forEach(btn => {
    btn.addEventListener('click', () => {
        const modal = btn.closest('.modal-overlay');
        if (modal) {
            closeModal(modal.id);
        }
    });
});

window.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
        closeModal(e.target.id);
    }
});

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
    });
});

function setDefaultVehicleDates() {
    const today = new Date();
    const last30Days = new Date();
    last30Days.setDate(today.getDate() - 30);
    const formattedToday = today.toISOString().split('T')[0];
    const formattedFrom = last30Days.toISOString().split('T')[0];

    const fromInput = document.getElementById('vehicles-date-from-filter');
    const toInput = document.getElementById('vehicles-date-to-filter');

    if (fromInput && !fromInput.value) {
        fromInput.value = formattedFrom;
    }
    if (toInput && !toInput.value) {
        toInput.value = formattedToday;
    }
}

async function loadCarriers() {
    const carriers = await fetchCarriers();
    const tbody = document.getElementById('carriers-tbody');
    if (!tbody) return;
    
    if (!carriers || carriers.length === 0) {
        tbody.innerHTML = '<tr class="loading-row"><td colspan="6" style="text-align: center; color: var(--text-muted);">Немає перевізників</td></tr>';
        return;
    }
    
    tbody.innerHTML = carriers.map(carrier => `
        <tr>
            <td>${carrier.companyName || '-'}</td>
            <td>${carrier.registrationAddress || '-'}</td>
            <td>${carrier.phoneNumber || '-'}</td>
            <td>${carrier.code || '-'}</td>
            <td>${carrier.account || '-'}</td>
            <td>
                <div class="action-buttons">
                    <button class="btn btn-secondary btn-sm" onclick="editCarrier(${carrier.id})">✏️ Редагувати</button>
                    <button class="btn btn-danger btn-sm" onclick="deleteCarrier(${carrier.id})">🗑️ Видалити</button>
                </div>
            </td>
        </tr>
    `).join('');
}

document.getElementById('manage-carriers-btn')?.addEventListener('click', async () => {
    await loadCarriers();
    openModal('manage-carriers-modal');
});

document.getElementById('create-carrier-btn')?.addEventListener('click', () => {
    document.getElementById('carrier-form-title').textContent = '➕ Створити перевізника';
    document.getElementById('carrier-form').reset();
    document.getElementById('carrier-id').value = '';
    openModal('carrier-form-modal');
});

async function editCarrier(carrierId) {
    const carrier = carrierMap.get(carrierId);
    if (!carrier) {
        showMessage('Перевізник не знайдений', 'error');
        return;
    }
    
    document.getElementById('carrier-form-title').textContent = '✏️ Редагувати перевізника';
    document.getElementById('carrier-id').value = carrier.id;
    document.getElementById('carrier-company-name').value = carrier.companyName || '';
    document.getElementById('carrier-registration-address').value = carrier.registrationAddress || '';
    document.getElementById('carrier-phone-number').value = carrier.phoneNumber || '';
    document.getElementById('carrier-code').value = carrier.code || '';
    document.getElementById('carrier-account').value = carrier.account || '';
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
        populateCarriers('vehicle-carrier-id');
        populateCarriers('detail-vehicle-carrier-id');
    } catch (error) {
        showMessage('Помилка при видаленні перевізника', 'error');
    }
}

document.getElementById('carrier-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const carrierId = document.getElementById('carrier-id').value;
    const carrierData = {
        companyName: document.getElementById('carrier-company-name').value,
        registrationAddress: document.getElementById('carrier-registration-address').value,
        phoneNumber: document.getElementById('carrier-phone-number').value,
        code: document.getElementById('carrier-code').value,
        account: document.getElementById('carrier-account').value
    };
    
    try {
        let response;
        if (carrierId) {
            response = await fetch(`/api/v1/carriers/${carrierId}`, {
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
        
        showMessage(carrierId ? 'Перевізника успішно оновлено' : 'Перевізника успішно створено', 'success');
        closeModal('carrier-form-modal');
        await loadCarriers();
        await fetchCarriers();
        populateCarriers('vehicle-carrier-id');
        populateCarriers('detail-vehicle-carrier-id');
    } catch (error) {
        showMessage(error.message || 'Помилка при збереженні перевізника', 'error');
    }
});

document.getElementById('cancel-carrier-btn')?.addEventListener('click', () => {
    closeModal('carrier-form-modal');
});

let currentResizeHeader = null;
let startX = 0;
let startWidth = 0;

function initializeColumnResize() {
    const table = document.getElementById('vehicles-table');
    if (!table) return;
    
    const headers = table.querySelectorAll('.resizable-header');
    
    headers.forEach(header => {
        const resizeHandle = document.createElement('div');
        resizeHandle.className = 'resize-handle';
        header.style.position = 'relative';
        header.appendChild(resizeHandle);
        
        resizeHandle.addEventListener('mousedown', (e) => {
            e.preventDefault();
            e.stopPropagation();
            currentResizeHeader = header;
            startX = e.pageX;
            startWidth = header.offsetWidth;
            header.classList.add('resizing');
            document.body.style.cursor = 'col-resize';
            document.body.style.userSelect = 'none';
        });
    });
    
    if (!window.columnResizeInitialized) {
    document.addEventListener('mousemove', (e) => {
        if (!currentResizeHeader) return;
        
        const diff = e.pageX - startX;
        const newWidth = Math.max(50, startWidth + diff);
        const column = currentResizeHeader.dataset.column;
        
        currentResizeHeader.style.minWidth = newWidth + 'px';
        currentResizeHeader.style.width = newWidth + 'px';
        const index = Array.from(currentResizeHeader.parentElement.children).indexOf(currentResizeHeader);
        const cells = table.querySelectorAll(`tbody tr td:nth-child(${index + 1})`);
        cells.forEach(cell => {
            cell.style.minWidth = newWidth + 'px';
            cell.style.width = newWidth + 'px';
        });
    });
        
        document.addEventListener('mouseup', () => {
            if (currentResizeHeader) {
                const column = currentResizeHeader.dataset.column;
                const width = Math.max(50, currentResizeHeader.offsetWidth);
                localStorage.setItem(`vehicle-column-width-${column}`, width.toString());
                currentResizeHeader.classList.remove('resizing');
                currentResizeHeader = null;
                document.body.style.cursor = '';
                document.body.style.userSelect = '';
            }
        });
        
        window.columnResizeInitialized = true;
    }
    
    applySavedColumnWidths();
}

async function initialize() {
    await fetchProducts();
    await fetchWarehouses();
    await fetchCarriers();
    populateCarriers('vehicle-carrier-id');
    populateCarriers('detail-vehicle-carrier-id');
    setDefaultVehicleDates();
    await loadVehicles();
    initializeColumnResize();
}

initialize();

