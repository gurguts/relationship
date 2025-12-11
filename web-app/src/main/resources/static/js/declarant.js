let productMap = new Map();
let warehouseMap = new Map();
let userMap = new Map();

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

const createVehicleBtn = document.getElementById('create-vehicle-btn');
const createVehicleModal = document.getElementById('create-vehicle-modal');
const vehicleDetailsModal = document.getElementById('vehicle-details-modal');
const createVehicleForm = document.getElementById('create-vehicle-form');
const addProductToVehicleModal = document.getElementById('add-product-to-vehicle-modal');
const addProductToVehicleForm = document.getElementById('add-product-to-vehicle-form');
const updateVehicleForm = document.getElementById('update-vehicle-form');
const detailVehicleDateInput = document.getElementById('detail-vehicle-date');
const detailVehicleVehicleInput = document.getElementById('detail-vehicle-vehicle-number');
const detailVehicleInvoiceUaInput = document.getElementById('detail-vehicle-invoice-ua');
const detailVehicleInvoiceEuInput = document.getElementById('detail-vehicle-invoice-eu');
const detailVehicleDescriptionInput = document.getElementById('detail-vehicle-description');
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
        return;
    }

    if (detailVehicleDateInput) detailVehicleDateInput.value = vehicle.shipmentDate || '';
    if (detailVehicleVehicleInput) detailVehicleVehicleInput.value = vehicle.vehicleNumber || '';
    if (detailVehicleInvoiceUaInput) detailVehicleInvoiceUaInput.value = vehicle.invoiceUa || '';
    if (detailVehicleInvoiceEuInput) detailVehicleInvoiceEuInput.value = vehicle.invoiceEu || '';
    if (detailVehicleDescriptionInput) detailVehicleDescriptionInput.value = vehicle.description || '';
}

function setVehicleFormEditable(isEditable) {
    const fields = [
        detailVehicleDateInput,
        detailVehicleVehicleInput,
        detailVehicleInvoiceUaInput,
        detailVehicleInvoiceEuInput,
        detailVehicleDescriptionInput
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
        document.getElementById('vehicle-date').valueAsDate = new Date();
        createVehicleModal.style.display = 'flex';
        createVehicleModal.classList.add('open');
        document.body.classList.add('modal-open');
    });
}

if (createVehicleForm) {
    createVehicleForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const vehicleData = {
            shipmentDate: document.getElementById('vehicle-date').value,
            vehicleNumber: document.getElementById('vehicle-vehicle-number').value,
            invoiceUa: document.getElementById('vehicle-invoice-ua').value,
            invoiceEu: document.getElementById('vehicle-invoice-eu').value,
            description: document.getElementById('vehicle-description').value,
            isOurVehicle: false
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
            createVehicleForm.reset();
            
            await loadVehicles();
        } catch (error) {
            showMessage(error.message || 'Помилка при створенні машини', 'error');
        }
    });
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
        renderVehicles(vehiclesCache);
    } catch (error) {
        showMessage('Помилка завантаження машин', 'error');
        
        const tbody = document.getElementById('vehicles-tbody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">Помилка завантаження даних</td></tr>';
        }
    }
}

function renderVehicles(vehicles) {
    const tbody = document.getElementById('vehicles-tbody');
    
    if (!tbody) {
        return;
    }
    
    if (!vehicles || vehicles.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">Немає даних</td></tr>';
        return;
    }
    
    tbody.innerHTML = vehicles.map(vehicle => `
        <tr onclick="viewVehicleDetails(${vehicle.id})" style="cursor: pointer;">
            <td>${vehicle.shipmentDate}</td>
            <td>${vehicle.vehicleNumber || '-'}</td>
            <td>${vehicle.invoiceUa || '-'}</td>
            <td>${vehicle.invoiceEu || '-'}</td>
            <td style="font-weight: bold; color: #FF6F00;">${formatNumber(vehicle.totalCostEur, 2)} EUR</td>
            <td>${vehicle.description || '-'}</td>
        </tr>
    `).join('');
}

async function viewVehicleDetails(vehicleId) {
    currentVehicleId = vehicleId;
    
    try {
        const response = await fetch(`/api/v1/vehicles/${vehicleId}`);
        
        if (!response.ok) {
            throw new Error('Failed to load vehicle details');
        }
        
        const vehicle = await response.json();
        renderVehicleDetails(vehicle);
        
        vehicleDetailsModal.style.display = 'flex';
        vehicleDetailsModal.classList.add('open');
        document.body.classList.add('modal-open');
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
        itemsTbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">Товари ще не додані</td></tr>';
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
                <tr class="vehicle-item-row" data-item-id="${item.withdrawalId}" style="cursor: pointer;">
                    <td>${productName}</td>
                    <td>${warehouseName}</td>
                    <td>${formatNumber(item.quantity, 2)} кг</td>
                    <td style="text-align: right;">${formatNumber(item.unitPriceEur, 6)} EUR</td>
                    <td style="text-align: right; font-weight: bold;">${formatNumber(item.totalCostEur, 6)} EUR</td>
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
    
    addProductToVehicleModal.style.display = 'flex';
    addProductToVehicleModal.classList.add('open');
    document.body.classList.add('modal-open');
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
            addProductToVehicleForm.reset();
            
            renderVehicleDetails(updatedVehicle);
            await loadVehicles();
        } catch (error) {
            showMessage(error.message || 'Помилка при додаванні товару до машини', 'error');
        }
    });
}

document.getElementById('delete-vehicle-btn')?.addEventListener('click', async () => {
    if (!confirm('Ви впевнені, що хочете видалити цю машину?')) {
        return;
    }
    
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
});

document.getElementById('apply-vehicles-filters')?.addEventListener('click', async () => {
    await loadVehicles();
    const modal = document.getElementById('vehicles-filter-modal');
    if (modal) {
        modal.classList.remove('open');
    }
    document.body.classList.remove('modal-open');
});

document.getElementById('clear-vehicles-filters')?.addEventListener('click', () => {
    document.getElementById('vehicles-date-from-filter').value = '';
    document.getElementById('vehicles-date-to-filter').value = '';
    setDefaultVehicleDates();
    loadVehicles();
});

document.getElementById('open-vehicles-filter-modal')?.addEventListener('click', () => {
    const modal = document.getElementById('vehicles-filter-modal');
    if (modal) {
        modal.classList.add('open');
    }
    document.body.classList.add('modal-open');
});

document.getElementById('vehicles-filter-modal-close')?.addEventListener('click', () => {
    const modal = document.getElementById('vehicles-filter-modal');
    if (modal) {
        modal.classList.remove('open');
    }
    document.body.classList.remove('modal-open');
});

if (updateVehicleForm) {
    updateVehicleForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!currentVehicleId) {
            showMessage('Не вдалося визначити машину для оновлення', 'error');
            return;
        }
        
        const payload = {
            shipmentDate: detailVehicleDateInput?.value || null,
            vehicleNumber: detailVehicleVehicleInput?.value ?? null,
            invoiceUa: detailVehicleInvoiceUaInput?.value ?? null,
            invoiceEu: detailVehicleInvoiceEuInput?.value ?? null,
            description: detailVehicleDescriptionInput?.value ?? null
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

    if (editVehicleItemModal) {
        editVehicleItemModal.style.display = 'flex';
        editVehicleItemModal.classList.add('open');
    }
    document.body.classList.add('modal-open');
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

const closeBtns = document.getElementsByClassName('close');
Array.from(closeBtns).forEach(btn => {
    btn.addEventListener('click', () => {
        closeModal(btn.closest('.modal').id);
    });
});

window.addEventListener('click', (e) => {
    const vehiclesFilterModal = document.getElementById('vehicles-filter-modal');
    
    if (e.target === createVehicleModal ||
        e.target === vehicleDetailsModal || 
        e.target === addProductToVehicleModal || 
        e.target === editVehicleItemModal ||
        e.target === vehiclesFilterModal) {
        const modalId = e.target.id;
        if (modalId) {
            closeModal(modalId);
        }
    }
});

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (!modal) return;
    
    modal.classList.remove('open');
    modal.style.display = 'none';
    document.body.classList.remove('modal-open');
    
    if (modalId === 'create-vehicle-modal') {
        document.getElementById('create-vehicle-form').reset();
    } else if (modalId === 'add-product-to-vehicle-modal') {
        document.getElementById('add-product-to-vehicle-form').reset();
    } else if (modalId === 'vehicle-details-modal') {
        resetVehicleFormState();
    } else if (modalId === 'edit-vehicle-item-modal') {
        if (editVehicleItemForm) {
            editVehicleItemForm.reset();
        }
        currentVehicleItemId = null;
        updateVehicleItemMode();
    }
}

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

async function initialize() {
    await fetchProducts();
    await fetchWarehouses();
    setDefaultVehicleDates();
    await loadVehicles();
}

initialize();

