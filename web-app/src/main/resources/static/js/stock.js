let productMap = new Map();
let warehouseMap = new Map();
let userMap = new Map();
let withdrawalReasonMap = new Map();
let withdrawalReasons = [];
const customSelects = {};
const entriesCustomSelects = {};
const historyCustomSelects = {};
const transfersCustomSelects = {};
const pageSize = 100;
let currentPage = 0;
let totalPages = 1;
let entriesCurrentPage = 0;
let entriesTotalPages = 1;
let filters = {
    withdrawal_date_from: [],
    withdrawal_date_to: [],
    product_id: [],
    withdrawal_reason_id: []
};

let historyFilters = {
    withdrawal_date_from: [],
    withdrawal_date_to: [],
    product_id: [],
    withdrawal_reason_id: []
};

let entriesFilters = {
    entry_date_from: [],
    entry_date_to: [],
    product_id: [],
    user_id: [],
    warehouse_id: [],
    type: []
};

let currentWithdrawalItem = null;

let transfersCache = [];

/**
 * Formats a number by removing trailing zeros
 * Examples: 25.000000 -> 25, 25.500000 -> 25.5, 25.333333 -> 25.333333
 */
function formatNumber(value, maxDecimals = 6) {
    if (value === null || value === undefined || value === '') return '0';
    const num = parseFloat(value);
    if (isNaN(num)) return '0';
    // toFixed to limit decimals, then parseFloat to remove trailing zeros
    return parseFloat(num.toFixed(maxDecimals)).toString();
}

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
        populateSelect('product-id-filter', products);
        populateSelect('product-id', products);
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
            handleError(new Error(errorData.message || 'Failed to fetch products'));
            return;
        }
        const warehouses = await response.json();
        warehouseMap = new Map(warehouses.map(warehouse => [warehouse.id, warehouse.name]));
        populateSelect('warehouse-id-filter', warehouses);
        populateSelect('warehouse-id', warehouses);
    } catch (error) {
        console.error('Error fetching products:', error);
        handleError(error);
    }
}

async function fetchUsers() {
    try {
        const response = await fetch('/api/v1/user');
        if (!response.ok) {
            handleError(new Error('Failed to load users'));
            return;
        }
        const users = await response.json();
        userMap = new Map(users.map(user => [user.id, user.name]));
        populateSelect('move-executor-id', users);
    } catch (error) {
        console.error('Error fetching users:', error);
        handleError(error);
    }
}

async function fetchWithdrawalReasons() {
    try {
        const response = await fetch('/api/v1/withdrawal-reason');
        if (!response.ok) {
            const errorData = await response.json();
            handleError(new Error(errorData.message || 'Failed to fetch withdrawal reasons'));
            return;
        }
        withdrawalReasons = await response.json();

        withdrawalReasonMap = new Map(withdrawalReasons.map(reason => [reason.id, reason]));

        const removingReasons = getWithdrawalReasonsByPurpose('REMOVING');
        populateSelect('withdrawal-reason-id-filter', removingReasons);
        populateSelect('edit-withdrawal-reason-id', removingReasons);

        return withdrawalReasons;
    } catch (error) {
        console.error('Error fetching withdrawal reasons:', error);
        handleError(error);
    }
}

const findNameByIdFromMap = (map, id) => {
    const numericId = Number(id);
    return map.get(numericId) || '';
};

function getWithdrawalReasonsByPurpose(purpose) {
    if (!withdrawalReasonMap) {
        return [];
    }

    return Array.from(withdrawalReasonMap.values()).filter(reason => reason.purpose === purpose);
}

function populateWithdrawalReasonsForWithdrawal() {
    const reasonsForWithdrawal = getWithdrawalReasonsByPurpose('REMOVING');
    populateSelect('withdrawal-reason-id', reasonsForWithdrawal);
}

function populateTransferReasons(selectId) {
    const select = document.getElementById(selectId);
    if (!select) {
        console.error(`Select with id "${selectId}" not found in DOM`);
        return;
    }
    const reasons = getWithdrawalReasonsByPurpose('BOTH');
    select.innerHTML = '<option value="">Оберіть причину</option>';
    reasons.forEach(reason => {
        const option = document.createElement('option');
        option.value = String(reason.id);
        option.textContent = reason.name;
        select.appendChild(option);
    });
}

function populateMoveTypes() {
    const moveTypes = getWithdrawalReasonsByPurpose('BOTH');
    populateSelect('move-type-id', moveTypes);
}

function populateEntryTypes() {
    const entryTypes = getWithdrawalReasonsByPurpose('ADDING');
    populateSelect('entry-type-id', entryTypes);
}

function setDefaultHistoryDates() {
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('history-withdrawal-date-from-filter').value = today;
    document.getElementById('history-withdrawal-date-to-filter').value = today;
}

function setDefaultEntriesDates() {
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('entries-entry-date-from-filter').value = today;
    document.getElementById('entries-entry-date-to-filter').value = today;
}

function setDefaultTransfersDates() {
    const today = new Date();
    const fromDate = new Date();
    fromDate.setDate(today.getDate() - 30);
    const formattedToday = today.toISOString().split('T')[0];
    const formattedFrom = fromDate.toISOString().split('T')[0];

    const fromInput = document.getElementById('transfer-date-from-filter');
    const toInput = document.getElementById('transfer-date-to-filter');

    if (fromInput && !fromInput.value) {
        fromInput.value = formattedFrom;
    }
    if (toInput && !toInput.value) {
        toInput.value = formattedToday;
    }
}

function updateHistorySelectedFilters() {
    Object.keys(historyFilters).forEach(key => delete historyFilters[key]);

    Object.keys(historyCustomSelects).forEach(selectId => {
        const name = document.getElementById(selectId).name;
        const values = historyCustomSelects[selectId].getValue();
        if (values.length > 0) {
            historyFilters[name] = values;
        }
    });

    const withdrawalDateFrom = document.getElementById('history-withdrawal-date-from-filter').value;
    const withdrawalDateTo = document.getElementById('history-withdrawal-date-to-filter').value;

    if (withdrawalDateFrom) {
        historyFilters['withdrawal_date_from'] = [withdrawalDateFrom];
    }
    if (withdrawalDateTo) {
        historyFilters['withdrawal_date_to'] = [withdrawalDateTo];
    }

    updateHistoryFilterCounter();
}

function updateHistoryFilterCounter() {
    const counterElement = document.getElementById('history-filter-counter');
    const countElement = document.getElementById('history-filter-count');
    const filterButton = document.getElementById('open-history-filter-modal');
    const exportButton = document.getElementById('export-excel-history');

    let totalFilters = 0;
    totalFilters += Object.values(historyFilters)
        .filter(value => Array.isArray(value))
        .reduce((count, values) => count + values.length, 0);

    if (totalFilters > 0) {
        countElement.textContent = totalFilters;
        counterElement.style.display = 'inline-flex';
    } else {
        counterElement.style.display = 'none';
    }

    if (historyContainer.style.display === 'block') {
        filterButton.style.display = 'inline-block';
        exportButton.style.display = 'inline-block';
    }
}

function clearHistoryFilters() {
    Object.keys(historyFilters).forEach(key => delete historyFilters[key]);

    const filterForm = document.getElementById('history-filter-form');
    if (filterForm) {
        filterForm.reset();
        Object.keys(historyCustomSelects).forEach(selectId => {
            historyCustomSelects[selectId].reset();
        });
        setDefaultHistoryDates();
    }

    updateHistoryFilterCounter();
    loadWithdrawalHistory(0);
}

function updateEntriesSelectedFilters() {
    Object.keys(entriesFilters).forEach(key => delete entriesFilters[key]);

    Object.keys(entriesCustomSelects).forEach(selectId => {
        const name = document.getElementById(selectId).name;
        const values = entriesCustomSelects[selectId].getValue();
        if (values.length > 0) {
            entriesFilters[name] = values;
        }
    });

    const entryDateFrom = document.getElementById('entries-entry-date-from-filter').value;
    const entryDateTo = document.getElementById('entries-entry-date-to-filter').value;

    if (entryDateFrom) {
        entriesFilters['entry_date_from'] = [entryDateFrom];
    }
    if (entryDateTo) {
        entriesFilters['entry_date_to'] = [entryDateTo];
    }

    updateEntriesFilterCounter();
}

function updateEntriesFilterCounter() {
    const counterElement = document.getElementById('entries-filter-counter');
    const countElement = document.getElementById('entries-filter-count');
    const filterButton = document.getElementById('open-entries-filter-modal');
    const exportButton = document.getElementById('export-excel-entries');

    let totalFilters = 0;
    totalFilters += Object.values(entriesFilters)
        .filter(value => Array.isArray(value))
        .reduce((count, values) => count + values.length, 0);

    if (totalFilters > 0) {
        countElement.textContent = totalFilters;
        counterElement.style.display = 'inline-flex';
    } else {
        counterElement.style.display = 'none';
    }

    if (entriesContainer.style.display === 'block') {
        filterButton.style.display = 'inline-block';
        exportButton.style.display = 'inline-block';
    }
}

function clearEntriesFilters() {
    Object.keys(entriesFilters).forEach(key => delete entriesFilters[key]);

    const filterForm = document.getElementById('entries-filter-form');
    if (filterForm) {
        filterForm.reset();
        Object.keys(entriesCustomSelects).forEach(selectId => {
            entriesCustomSelects[selectId].reset();
        });
        setDefaultEntriesDates();
    }

    updateEntriesFilterCounter();
    loadWarehouseEntries(0);
}

async function loadBalance() {
    try {
        const response = await fetch('/api/v1/warehouse/balances/active');
        if (!response.ok) {
            handleError(new Error('Failed to load balance'));
            return;
        }
        const balances = await response.json();
        
        // Group balances by warehouse
        const balancesByWarehouse = {};
        balances.forEach(balance => {
            if (!balancesByWarehouse[balance.warehouseId]) {
                balancesByWarehouse[balance.warehouseId] = [];
            }
            balancesByWarehouse[balance.warehouseId].push(balance);
        });
        
        // Sort warehouses by ID to maintain consistent order
        const sortedWarehouseIds = Object.keys(balancesByWarehouse).sort((a, b) => Number(a) - Number(b));
        
        const container = document.getElementById('balance-container');
        let html = '';
        
        // Display balances grouped by warehouse (sorted by warehouse ID)
        for (const warehouseId of sortedWarehouseIds) {
            const warehouseBalances = balancesByWarehouse[warehouseId];
            
            // Sort products by product ID to maintain consistent order
            warehouseBalances.sort((a, b) => Number(a.productId) - Number(b.productId));
            
            const warehouseName = findNameByIdFromMap(warehouseMap, warehouseId);
            html += `<h3>Склад: ${warehouseName}</h3>`;
            html += '<table class="balance-table"><thead><tr>';
            html += '<th>Товар</th>';
            html += '<th>Кількість (кг)</th>';
            html += '<th>Середня ціна (EUR/кг)</th>';
            html += '<th>Загальна вартість (EUR)</th>';
            html += '</tr></thead><tbody>';
            
            let warehouseTotal = 0;
            for (const balance of warehouseBalances) {
                const productName = findNameByIdFromMap(productMap, balance.productId);
                const quantity = formatNumber(balance.quantity, 2);
                const avgPrice = formatNumber(balance.averagePriceEur, 6);
                const totalCost = formatNumber(balance.totalCostEur, 6);
                warehouseTotal += parseFloat(totalCost);
                
                html += `<tr class="balance-row"
                            data-warehouse-id="${warehouseId}"
                            data-product-id="${balance.productId}"
                            data-warehouse-name="${warehouseName}"
                            data-product-name="${productName}"
                            data-quantity="${balance.quantity}"
                            data-total-cost="${balance.totalCostEur}"
                            data-average-price="${balance.averagePriceEur}">
                            <td>${productName}</td>
                            <td>${quantity}</td>
                            <td>${avgPrice}</td>
                            <td>${totalCost}</td>
                        </tr>`;
            }
            
            html += '</tbody><tfoot><tr>';
            html += '<td colspan="3"><strong>Загальна вартість складу:</strong></td>';
            html += `<td><strong>${formatNumber(warehouseTotal, 6)} EUR</strong></td>`;
            html += '</tr></tfoot></table>';
        }
        
        if (html === '') {
            html = '<p>Немає активних балансів на складі</p>';
        }
        
        container.innerHTML = html;
        attachBalanceRowListeners();
    } catch (error) {
        console.error('Error loading balance:', error);
        handleError(error);
    }
}

function populateSelect(selectId, data) {
    const select = document.getElementById(selectId);
    if (!select) {
        console.error(`Select with id "${selectId}" not found in DOM`);
        return;
    }
    select.innerHTML = '';
    data.forEach(item => {
        const option = document.createElement('option');
        option.value = String(item.id || item.value);
        option.text = item.name || item.text || item.value;
        select.appendChild(option);
    });
    if (customSelects[selectId]) {
        customSelects[selectId].populate(data.map(item => ({
            id: item.id || item.value,
            name: item.name || item.text || item.value
        })));
    }
}

function populateProducts(selectId) {
    const select = document.getElementById(selectId);
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
    select.innerHTML = '<option value="">Оберіть склад</option>';
    for (const [id, name] of warehouseMap.entries()) {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = name;
        select.appendChild(option);
    }
}

async function loadWithdrawalHistory(page) {
    const activeFilters = Object.keys(historyFilters).length > 0 ? historyFilters : filters;

    Object.keys(activeFilters).forEach(key => {
        if (Array.isArray(activeFilters[key]) && activeFilters[key].length === 0) {
            delete activeFilters[key];
        }
    });
    const queryParams = `page=${page}&size=${pageSize}&sort=withdrawalDate&direction=DESC&filters=
    ${encodeURIComponent(JSON.stringify(activeFilters))}`;
    try {
        const response = await fetch(`/api/v1/warehouse/withdrawals?${queryParams}`);
        if (!response.ok) {
            const errorData = await response.json();
            handleError(new Error(errorData.message || 'Failed to load withdrawals'));
            return;
        }
        const data = await response.json();
        const container = document.getElementById('history-content');
        let html = '';
        for (const withdrawal of data.content) {
            const productName = findNameByIdFromMap(productMap, withdrawal.productId) || 'Не вказано';
            const warehouseName = findNameByIdFromMap(warehouseMap, withdrawal.warehouseId) || 'Не вказано';
            const reason = withdrawal.withdrawalReason ? withdrawal.withdrawalReason.name : 'Невідома причина';
            const unitPrice = withdrawal.unitPriceEur ? formatNumber(withdrawal.unitPriceEur, 6) + ' EUR' : '-';
            const totalCost = withdrawal.totalCostEur ? formatNumber(withdrawal.totalCostEur, 6) + ' EUR' : '-';
            html += `
                        <tr data-id="${withdrawal.id}">
                            <td>${warehouseName}</td>
                            <td>${productName}</td>
                            <td>${reason}</td>
                            <td>${withdrawal.quantity} кг</td>
                            <td style="text-align: right;">${unitPrice}</td>
                            <td style="text-align: right; font-weight: bold;">${totalCost}</td>
                            <td>${withdrawal.withdrawalDate}</td>
                            <td>${withdrawal.description || ''}</td>
                            <td>${new Date(withdrawal.createdAt).toLocaleString()}</td>
                        </tr>`;
        }
        container.innerHTML = html;

        document.querySelectorAll('.history-table tr').forEach(row => {
            row.addEventListener('click', () => openEditModal(row.dataset.id, data.content));
        });
        updatePagination(data.totalPages, page);
    } catch (error) {
        console.error('Error loading withdrawals:', error);
        handleError(error);
    }
}

async function loadWarehouseEntries(page) {
    const activeFilters = Object.keys(entriesFilters).length > 0 ? entriesFilters : filters;

    Object.keys(activeFilters).forEach(key => {
        if (Array.isArray(activeFilters[key]) && activeFilters[key].length === 0) {
            delete activeFilters[key];
        }
    });

    const queryParams = `page=${page}&size=${pageSize}&sort=entryDate&direction=DESC&filters=${encodeURIComponent(JSON.stringify(activeFilters))}`;

    try {
        const response = await fetch(`/api/v1/warehouse/receipts?${queryParams}`, {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        });

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new Error(errorData.message || 'Failed to load entries'));
            return;
        }

        const data = await response.json();
        const container = document.getElementById('entries-body');
        let html = '';
        for (const entry of data.content) {
            const productName = findNameByIdFromMap(productMap, entry.productId);
            const warehouseName = findNameByIdFromMap(warehouseMap, entry.warehouseId);
            const userName = findNameByIdFromMap(userMap, entry.userId);
            const typeName = entry.type ? entry.type.name : 'Невідомий тип';
            const driverBalance = entry.driverBalanceQuantity || 0;
            const receivedQuantity = entry.quantity || 0;
            const difference = receivedQuantity - driverBalance;
            const totalCost = formatNumber(entry.totalCostEur, 6);
            html += `
                <tr data-id="${entry.id}">
                    <td>${warehouseName}</td>
                    <td>${entry.entryDate}</td>
                    <td>${userName}</td>
                    <td>${productName}</td>
                    <td>${typeName}</td>
                    <td>${receivedQuantity} кг</td>
                    <td>${driverBalance} кг</td>
                    <td>${difference} кг</td>
                    <td>${totalCost} EUR</td>
                </tr>`;
        }
        container.innerHTML = html;

        updateEntriesPagination(data.totalPages, page);
    } catch (error) {
        console.error('Error loading entries:', error);
        handleError(error);
    }
}

function updateEntriesPagination(total, page) {
    entriesTotalPages = total;
    entriesCurrentPage = page;
    document.getElementById('entries-page-info').textContent = `Сторінка ${entriesCurrentPage + 1} з ${entriesTotalPages}`;
    document.getElementById('entries-prev-page').disabled = entriesCurrentPage === 0;
    document.getElementById('entries-next-page').disabled = entriesCurrentPage >= entriesTotalPages - 1;
}

function updatePagination(total, page) {
    totalPages = total;
    currentPage = page;
    document.getElementById('page-info').textContent = `Сторінка ${currentPage + 1} з ${totalPages}`;
    document.getElementById('prev-page').disabled = currentPage === 0;
    document.getElementById('next-page').disabled = currentPage >= totalPages - 1;
}

document.getElementById('withdraw-form').addEventListener('submit',
    async (e) => {
        e.preventDefault();
        
        const withdrawal = {
            warehouseId: Number(document.getElementById('warehouse-id').value),
            productId: Number(document.getElementById('product-id').value),
            withdrawalReasonId: Number(document.getElementById('withdrawal-reason-id').value),
            quantity: Number(document.getElementById('quantity').value),
            description: document.getElementById('description').value,
        };
        try {
            const response = await fetch('/api/v1/warehouse/withdraw', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(withdrawal)
            });
            if (!response.ok) {
                const errorData = await response.json();
                handleError(new Error(errorData.message || 'Failed to create withdrawal'));
                return;
            }
            showMessage('Списання успішно створено', 'info');
            closeModal('withdraw-modal');
            loadBalance();
            if (historyContainer.style.display === 'block') {
                loadWithdrawalHistory(currentPage);
            }
        } catch (error) {
            console.error('Error creating withdrawal:', error);
            handleError(error);
        }
    });

document.getElementById('move-form').addEventListener('submit',
    async (e) => {
        e.preventDefault();
        const formData = new FormData(e.target);

        const transfer = {
            warehouseId: Number(formData.get('warehouse_id')),
            fromProductId: Number(formData.get('from_product_id')),
            toProductId: Number(formData.get('to_product_id')),
            quantity: Number(formData.get('quantity')),
            withdrawalReasonId: Number(formData.get('type_id')),
            description: formData.get('description') || ''
        };

        try {
            const response = await fetch('/api/v1/warehouse/transfer', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(transfer)
            });

            const data = await response.json();

            if (!response.ok || !data.success) {
                handleError(new Error(data.message || 'Помилка переміщення товару'));
                return;
            }

            showMessage(data.message || 'Товар успішно переміщено', 'success');
            closeModal('move-modal');
            loadBalance();
            if (historyContainer.style.display === 'block') {
                loadWithdrawalHistory(currentPage);
            }
        } catch (error) {
            handleError(error);
        }
    });

document.getElementById('entry-form').addEventListener('submit',
    async (e) => {
        e.preventDefault();
        const formData = new FormData(e.target);

        const entry = {
            warehouseId: Number(formData.get('warehouse_id')),
            productId: Number(formData.get('product_id')),
            quantity: Number(formData.get('quantity')),
            typeId: Number(formData.get('type_id')),
            userId: Number(formData.get('user_id'))
        };

        try {
        const response = await fetch('/api/v1/warehouse/receipts', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(entry)
            });

            if (!response.ok) {
                const errorData = await response.json();
                handleError(new Error(errorData.message || 'Failed to create entry'));
                return;
            }

            showMessage('Надходження успішно створено', 'info');
            closeModal('entry-modal');
            loadBalance();
            if (entriesContainer.style.display === 'block') {
                loadWarehouseEntries(0);
            }
        } catch (error) {
            console.error('Error creating entry:', error);
            handleError(error);
        }
    });

function openEditModal(id, withdrawals) {
    const withdrawal = withdrawals.find(w => w.id === Number(id));
    if (!withdrawal) return;

    currentWithdrawalItem = withdrawal;

    document.getElementById('edit-id').value = withdrawal.id;
    const dateInput = document.getElementById('edit-withdrawal-date');
    if (dateInput) {
        dateInput.value = withdrawal.withdrawalDate;
    }
    const dateDisplay = document.getElementById('edit-withdrawal-date-display');
    if (dateDisplay) {
        dateDisplay.textContent = withdrawal.withdrawalDate || '—';
    }
    const productNameDisplay = document.getElementById('edit-withdrawal-product-name');
    if (productNameDisplay) {
        const productName = findNameByIdFromMap(productMap, withdrawal.productId) || 'Не вказано';
        productNameDisplay.textContent = productName;
    }
    document.getElementById('edit-withdrawal-reason-id').value = withdrawal.withdrawalReason?.id || '';

    const quantityField = document.getElementById('edit-quantity');
    if (quantityField) {
        const numericQuantity = parseFloat(withdrawal.quantity);
        quantityField.value = Number.isNaN(numericQuantity) ? '' : numericQuantity.toFixed(2);
    }

    document.getElementById('edit-description').value = withdrawal.description || '';

    const editModal = document.getElementById('edit-modal');
    editModal.style.display = 'flex';
    editModal.classList.add('open');
    document.body.classList.add('modal-open');
}

document.getElementById('edit-form').addEventListener('submit',
    async (e) => {
        e.preventDefault();

        if (!currentWithdrawalItem) {
            showMessage('Не вдалося знайти списання для редагування', 'error');
            return;
        }

        const id = Number(document.getElementById('edit-id').value);
        const reasonElement = document.getElementById('edit-withdrawal-reason-id');
        const quantityField = document.getElementById('edit-quantity');
        const descriptionField = document.getElementById('edit-description');
        const dateField = document.getElementById('edit-withdrawal-date');

        const reasonId = Number(reasonElement.value);
        if (!reasonId) {
            showMessage('Оберіть причину списання', 'error');
            return;
        }

        const rawQuantity = parseFloat(quantityField.value);
        if (Number.isNaN(rawQuantity) || rawQuantity < 0) {
            showMessage('Вкажіть коректну кількість (0 або більше)', 'error');
            return;
        }

        const roundedQuantity = Number(rawQuantity.toFixed(2));
        const descriptionValue = descriptionField.value;
        const withdrawalDateValue = dateField.value;

        const originalQuantity = parseFloat(currentWithdrawalItem.quantity);
        const originalReasonId = currentWithdrawalItem.withdrawalReason ? currentWithdrawalItem.withdrawalReason.id : null;
        const originalDescription = currentWithdrawalItem.description || '';
        const originalDate = currentWithdrawalItem.withdrawalDate;

        const hasQuantityChange = Math.abs(roundedQuantity - originalQuantity) > 0.0001;
        const hasReasonChange = reasonId !== originalReasonId;
        const hasDescriptionChange = (descriptionValue || '') !== originalDescription;
        const hasDateChange = withdrawalDateValue !== originalDate;

        if (!hasQuantityChange && !hasReasonChange && !hasDescriptionChange && !hasDateChange) {
            showMessage('Зміни відсутні', 'info');
            return;
        }

        if (roundedQuantity === 0) {
            const productLabel = findNameByIdFromMap(productMap, currentWithdrawalItem.productId) || 'товар';
            const confirmRemoval = confirm(`Ви впевнені, що хочете повністю видалити списання для ${productLabel}?`);
            if (!confirmRemoval) {
                return;
            }
        }

        const withdrawalUpdate = {
            withdrawalReasonId: reasonId,
            quantity: roundedQuantity,
            description: descriptionValue,
            withdrawalDate: withdrawalDateValue
        };

        try {
            const response = await fetch(`/api/v1/warehouse/withdraw/${id}`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(withdrawalUpdate)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                handleError(new Error(errorData.message || 'Failed to update withdrawal'));
                return;
            }

            const successMessage = response.status === 204
                ? 'Списання успішно видалено'
                : 'Списання успішно оновлено';

            showMessage(successMessage, 'info');
            closeModal('edit-modal');
            await loadBalance();
            await loadWithdrawalHistory(currentPage);
        } catch (error) {
            console.error('Error updating withdrawal:', error);
        handleError(error);
    }
});

function createCustomSelect(selectElement) {
    const selectId = selectElement.id;
    const selectName = selectElement.name;
    const isMultiple = selectElement.multiple;
    const currentSelect = document.getElementById(selectId);
    if (!currentSelect) {
        console.error(`Select with id "${selectId}" not found in DOM`);
        return null;
    }

    const selectContainer = document.createElement('div');
    selectContainer.className = 'custom-select-container';
    selectContainer.dataset.for = selectId;

    const trigger = document.createElement('div');
    trigger.className = 'custom-select-trigger';
    trigger.tabIndex = 0;

    const tagsContainer = document.createElement('div');
    tagsContainer.className = 'custom-select-tags';
    const placeholder = document.createElement('div');
    placeholder.className = 'custom-select-placeholder';
    placeholder.textContent = currentSelect.querySelector('option[selected]')?.textContent ||
        'Параметр не задано';

    trigger.appendChild(tagsContainer);
    trigger.appendChild(placeholder);
    selectContainer.appendChild(trigger);

    const dropdown = document.createElement('div');
    dropdown.className = 'custom-select-dropdown';

    const searchWrapper = document.createElement('div');
    searchWrapper.className = 'custom-select-search';
    const searchInput = document.createElement('input');
    searchInput.type = 'text';
    searchInput.placeholder = 'Пошук...';
    searchWrapper.appendChild(searchInput);
    dropdown.appendChild(searchWrapper);

    const hiddenInput = document.createElement('input');
    hiddenInput.type = 'hidden';
    hiddenInput.name = selectName;
    hiddenInput.id = `hidden-${selectId}`;

    const selectedValues = new Set(
        Array.from(currentSelect.selectedOptions).map(opt => opt.value)
    );
    let selectData = [];

    function debounce(fn, delay) {
        let timeout;
        return (...args) => {
            clearTimeout(timeout);
            timeout = setTimeout(() => fn(...args), delay);
        };
    }

    function updateSelection() {
        requestAnimationFrame(() => {
            tagsContainer.innerHTML = '';
            placeholder.style.display = selectedValues.size === 0 ? 'block' : 'none';
            if (isMultiple) {
                selectedValues.forEach(value => {
                    const option = currentSelect.querySelector(`option[value="${value}"]`);
                    if (!option) return;
                    const tag = document.createElement('div');
                    tag.className = 'custom-select-tag';
                    tag.textContent = option.textContent;
                    tag.dataset.value = value;

                    const removeButton = document.createElement('button');
                    removeButton.type = 'button';
                    removeButton.className = 'custom-select-tag-remove';
                    removeButton.textContent = '×';
                    tag.appendChild(removeButton);
                    tagsContainer.appendChild(tag);
                });
            } else {
                const value = Array.from(selectedValues)[0];
                if (value) {
                    const option = currentSelect.querySelector(`option[value="${value}"]`);
                    if (option) {
                        const tag = document.createElement('div');
                        tag.className = 'custom-select-tag';
                        tag.textContent = option.textContent;
                        tag.dataset.value = value;

                        const removeButton = document.createElement('button');
                        removeButton.type = 'button';
                        removeButton.className = 'custom-select-tag-remove';
                        removeButton.textContent = '×';
                        tag.appendChild(removeButton);
                        tagsContainer.appendChild(tag);
                    }
                }
            }
        });
    }

    function updateHiddenInput() {
        hiddenInput.value = isMultiple ? Array.from(selectedValues).join(',') : Array.from(selectedValues)[0] || '';
    }

    function populateDropdown(data) {
        const fragment = document.createDocumentFragment();
        const availableOptions = data.filter(item => !selectedValues.has(String(item.id)));
        availableOptions.forEach(item => {
            const option = document.createElement('div');
            option.className = 'custom-select-option';
            option.dataset.value = String(item.id);
            option.textContent = item.name;
            fragment.appendChild(option);
        });
        dropdown.innerHTML = '';
        dropdown.appendChild(fragment);
    }

    function sortAndFilterOptions(searchText) {
        const lowerSearch = searchText.toLowerCase();
        const filtered = selectData.filter(item => item.nameLower.includes(lowerSearch));
        filtered.sort((a, b) => {
            const aName = a.nameLower;
            const bName = b.nameLower;
            const aStartsWith = aName.startsWith(lowerSearch);
            const bStartsWith = bName.startsWith(lowerSearch);
            if (aStartsWith && !bStartsWith) return -1;
            if (!aStartsWith && bStartsWith) return 1;
            return aName.localeCompare(bName);
        });
        populateDropdown(filtered);
    }

    dropdown.addEventListener('click', (e) => {
        e.stopPropagation();
        const option = e.target.closest('.custom-select-option');
        if (option) {
            const value = option.dataset.value;
            const nativeOption = currentSelect.querySelector(`option[value="${value}"]`);
            if (!nativeOption) return;

            if (isMultiple) {
                if (selectedValues.has(value)) {
                    selectedValues.delete(value);
                    nativeOption.selected = false;
                } else {
                    selectedValues.add(value);
                    nativeOption.selected = true;
                }
            } else {
                selectedValues.clear();
                selectedValues.add(value);
                nativeOption.selected = true;
                Array.from(currentSelect.options).forEach(opt => opt.selected = opt.value === value);
                currentSelect.value = value;
            }
            updateSelection();
            updateHiddenInput();
            populateDropdown(selectData);
        }
    });

    tagsContainer.addEventListener('click', (e) => {
        const removeButton = e.target.closest('.custom-select-tag-remove');
        if (!removeButton) return;
        e.stopPropagation();
        const tag = removeButton.parentElement;
        const value = tag.dataset.value;
        selectedValues.delete(value);
        const option = currentSelect.querySelector(`option[value="${value}"]`);
        if (option) option.selected = false;
        updateSelection();
        updateHiddenInput();
        populateDropdown(selectData);
    });

    trigger.addEventListener('click', (e) => {
        e.stopPropagation();
        dropdown.classList.toggle('open');
        if (dropdown.classList.contains('open')) searchInput.focus();
    });

    document.addEventListener('click', (e) => {
        if (!selectContainer.contains(e.target)) dropdown.classList.remove('open');
    });

    searchInput.addEventListener('input', debounce(() => {
        sortAndFilterOptions(searchInput.value);
    }, 200));

    selectContainer.appendChild(dropdown);
    selectContainer.appendChild(hiddenInput);
    selectElement.parentNode.insertBefore(selectContainer, selectElement);
    selectElement.style.display = 'none';

    return {
        populate: function (data) {
            selectData = data.map(item => ({
                id: String(item.id),
                name: item.name,
                nameLower: item.name.toLowerCase()
            }));
            if (currentSelect.options.length === 0) {
                currentSelect.innerHTML = '';
                selectData.forEach(item => {
                    const option = document.createElement('option');
                    option.value = item.id;
                    option.text = item.name;
                    currentSelect.appendChild(option);
                });
            }
            populateDropdown(selectData);
            updateSelection();
        },
        setValue: function (values) {
            if (!Array.isArray(values)) values = [values];
            selectedValues.clear();
            values.forEach(value => {
                const option = currentSelect.querySelector(`option[value="${String(value)}"]`);
                if (option) {
                    selectedValues.add(String(value));
                    option.selected = true;
                }
            });
            updateSelection();
            updateHiddenInput();
            populateDropdown(selectData);
        },
        getValue: function () {
            return Array.from(selectedValues);
        },
        reset: function () {
            selectedValues.clear();
            Array.from(currentSelect.options).forEach(opt => opt.selected = false);
            updateSelection();
            updateHiddenInput();
            populateDropdown(selectData);
        }
    };
}

function updateSelectedFilters() {
    Object.keys(filters).forEach(key => delete filters[key]);
    Object.keys(customSelects).forEach(selectId => {
        const name = document.getElementById(selectId).name;
        const values = customSelects[selectId].getValue();
        if (values.length > 0) {
            filters[name] = values;
        }
    });
    const withdrawalDateFrom = document.getElementById('withdrawal-date-from-filter').value;
    const withdrawalDateTo = document.getElementById('withdrawal-date-to-filter').value;
    if (withdrawalDateFrom) {
        filters['withdrawal_date_from'] = [withdrawalDateFrom];
    }
    if (withdrawalDateTo) {
        filters['withdrawal_date_to'] = [withdrawalDateTo];
    }
    updateFilterCounter();
}

function updateFilterCounter() {
    const counterElement = document.getElementById('filter-counter');
    const countElement = document.getElementById('filter-count');
    let totalFilters = 0;
    totalFilters += Object.values(filters)
        .filter(value => Array.isArray(value))
        .reduce((count, values) => count + values.length, 0);
    if (totalFilters > 0) {
        countElement.textContent = totalFilters;
        counterElement.style.display = 'inline-flex';
    } else {
        counterElement.style.display = 'none';
    }
}

function clearFilters() {
    Object.keys(filters).forEach(key => delete filters[key]);
    const filterForm = document.getElementById('history-filters-form');
    if (filterForm) {
        filterForm.reset();
        Object.keys(customSelects).forEach(selectId => {
            customSelects[selectId].reset();
        });
    }
    updateFilterCounter();
    loadWithdrawalHistory(0);
}

const withdrawModal = document.getElementById('withdraw-modal');
const editModal = document.getElementById('edit-modal');
const withdrawBtn = document.getElementById('withdraw-btn');
const moveBtn = document.getElementById('move-btn');
const moveModal = document.getElementById('move-modal');
const historyContainer = document.getElementById('history-container');
const filtersContainer = document.getElementById('filters-container');
const entriesBtn = document.getElementById('entries-btn');
const entriesContainer = document.getElementById('entries-container');
const addEntryBtn = document.getElementById('add-entry-btn');
const entryModal = document.getElementById('entry-modal');
const driverBalancesBtn = document.getElementById('driver-balances-btn');
const driverBalancesModal = document.getElementById('driver-balances-modal');
const discrepanciesBtn = document.getElementById('discrepancies-btn');
const discrepanciesContainer = document.getElementById('discrepancies-container');
const transfersBtn = document.getElementById('transfers-btn');
const transfersContainer = document.getElementById('transfers-container');
const shipmentsBtn = document.getElementById('shipments-btn');
const shipmentsContainer = document.getElementById('shipments-container');
const closeBtns = document.getElementsByClassName('close');
const editTransferModal = document.getElementById('edit-transfer-modal');
const editTransferForm = document.getElementById('edit-transfer-form');
const editTransferReasonSelect = document.getElementById('edit-transfer-reason-id');
const editTransferQuantityInput = document.getElementById('edit-transfer-quantity');
const editTransferDescriptionInput = document.getElementById('edit-transfer-description');
const editTransferDateSpan = document.getElementById('edit-transfer-date');
const editTransferWarehouseSpan = document.getElementById('edit-transfer-warehouse');
const editTransferFromProductSpan = document.getElementById('edit-transfer-from-product');
const editTransferToProductSpan = document.getElementById('edit-transfer-to-product');
const balanceEditModal = document.getElementById('balance-edit-modal');
const balanceEditForm = document.getElementById('balance-edit-form');
const balanceEditModeRadios = document.querySelectorAll('input[name="balance-edit-mode"]');
const balanceEditQuantityInput = document.getElementById('balance-edit-quantity');
const balanceEditTotalCostInput = document.getElementById('balance-edit-total-cost');
const balanceEditDescriptionInput = document.getElementById('balance-edit-description');
const balanceEditWarehouseIdInput = document.getElementById('balance-edit-warehouse-id');
const balanceEditProductIdInput = document.getElementById('balance-edit-product-id');
const balanceHistoryBody = document.getElementById('balance-history-body');
const balanceHistoryEmpty = document.getElementById('balance-history-empty');
const balanceHistoryModal = document.getElementById('balance-history-modal');
const balanceHistoryBtn = document.getElementById('balance-history-btn');

// Discrepancies pagination state
let currentDiscrepanciesPage = 0;
let discrepanciesPageSize = 20;
let discrepanciesFilters = {};

// Transfers pagination state
let currentTransfersPage = 0;
let transfersPageSize = 20;
let transfersFilters = {};

withdrawBtn.addEventListener('click', () => {
    populateProducts('product-id');
    populateWarehouses('warehouse-id');
    populateWithdrawalReasonsForWithdrawal();
    withdrawModal.style.display = 'flex';
    withdrawModal.classList.add('open');
    document.body.classList.add('modal-open');
});

moveBtn.addEventListener('click', () => {
    populateProducts('move-from-product-id');
    populateProducts('move-to-product-id');
    populateWarehouses('move-warehouse-id');
    populateSelect('move-executor-id', Array.from(userMap.entries()).map(([id, name]) => ({id, name})));
    populateMoveTypes();
    moveModal.style.display = 'flex';
    moveModal.classList.add('open');
    document.body.classList.add('modal-open');
});

entriesBtn.addEventListener('click', async () => {
    if (entriesContainer.style.display === 'block') {
        entriesContainer.style.display = 'none';
        document.getElementById('open-entries-filter-modal').style.display = 'none';
        document.getElementById('entries-filter-counter').style.display = 'none';
        document.getElementById('export-excel-entries').style.display = 'none';
    } else {
        // Hide other containers
        document.getElementById('history-container').style.display = 'none';
        transfersContainer.style.display = 'none';
        shipmentsContainer.style.display = 'none';
        if (discrepanciesContainer) discrepanciesContainer.style.display = 'none';
        
        entriesContainer.style.display = 'block';
        document.getElementById('open-entries-filter-modal').style.display = 'inline-block';
        document.getElementById('export-excel-entries').style.display = 'inline-block';
        await initializeEntriesFilters();
        loadWarehouseEntries(0);
    }
});

addEntryBtn.addEventListener('click', () => {
    populateProducts('entry-product-id');
    populateWarehouses('entry-warehouse-id');
    populateSelect('entry-user-id', Array.from(userMap.entries()).map(([id, name]) => ({id, name})));
    populateEntryTypes();
    entryModal.style.display = 'flex';
    entryModal.classList.add('open');
    document.body.classList.add('modal-open');
    
    // Hide driver balance info and warning when opening modal
    const driverBalanceInfo = document.getElementById('driver-balance-info');
    const driverNoBalanceWarning = document.getElementById('driver-no-balance-warning');
    if (driverBalanceInfo) {
        driverBalanceInfo.style.display = 'none';
    }
    if (driverNoBalanceWarning) {
        driverNoBalanceWarning.style.display = 'none';
    }
});

// Use event delegation on entry form for driver and product selection
const entryForm = document.getElementById('entry-form');
if (entryForm) {
    entryForm.addEventListener('change', (e) => {
        if (e.target.id === 'entry-user-id' || e.target.id === 'entry-product-id') {
            loadDriverBalanceForEntry();
        }
    });
}

Array.from(closeBtns).forEach(btn => {
    btn.addEventListener('click', () => {
        closeModal(btn.closest('.modal').id);
    });
});

window.addEventListener('click', (e) => {
    if (e.target === withdrawModal || e.target === editModal || e.target === moveModal || e.target === entryModal ||
        e.target === driverBalancesModal || e.target === createShipmentModal ||
        e.target === shipmentDetailsModal || e.target === addProductToShipmentModal || e.target === editShipmentItemModal ||
        e.target === editTransferModal || e.target === balanceEditModal) {
        closeModal(e.target.id);
    }
});

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    modal.classList.remove('open');
    modal.style.display = 'none';
    document.body.classList.remove('modal-open');
    if (modalId === 'withdraw-modal') {
        document.getElementById('withdraw-form').reset();
    } else if (modalId === 'edit-modal') {
        document.getElementById('edit-form').reset();
        currentWithdrawalItem = null;
    } else if (modalId === 'move-modal') {
        document.getElementById('move-form').reset();
    } else if (modalId === 'entry-modal') {
        document.getElementById('entry-form').reset();
        // Hide driver balance info and warning when closing modal
        const driverBalanceInfo = document.getElementById('driver-balance-info');
        const driverNoBalanceWarning = document.getElementById('driver-no-balance-warning');
        if (driverBalanceInfo) {
            driverBalanceInfo.style.display = 'none';
        }
        if (driverNoBalanceWarning) {
            driverNoBalanceWarning.style.display = 'none';
        }
    } else if (modalId === 'create-shipment-modal') {
        document.getElementById('create-shipment-form').reset();
    } else if (modalId === 'add-product-to-shipment-modal') {
        document.getElementById('add-product-to-shipment-form').reset();
    } else if (modalId === 'shipment-details-modal') {
        resetShipmentFormState();
    } else if (modalId === 'edit-shipment-item-modal') {
        if (editShipmentItemForm) {
            editShipmentItemForm.reset();
        }
        currentShipmentItemId = null;
        updateShipmentItemMode();
    } else if (modalId === 'edit-transfer-modal') {
        if (editTransferForm) {
            editTransferForm.reset();
        }
        currentTransferItem = null;
    } else if (modalId === 'balance-edit-modal') {
        resetBalanceEditModal();
    } else if (modalId === 'balance-history-modal') {
        resetBalanceHistoryModal();
    }
}

const historyBtn = document.getElementById('history-btn');
historyBtn.addEventListener('click', () => {
    if (historyContainer.style.display === 'block') {
        historyContainer.style.display = 'none';
        document.getElementById('open-history-filter-modal').style.display = 'none';
        document.getElementById('history-filter-counter').style.display = 'none';
        document.getElementById('export-excel-history').style.display = 'none';
    } else {
        // Hide other containers
        document.getElementById('entries-container').style.display = 'none';
        transfersContainer.style.display = 'none';
        shipmentsContainer.style.display = 'none';
        if (discrepanciesContainer) discrepanciesContainer.style.display = 'none';
        
        historyContainer.style.display = 'block';
        document.getElementById('open-history-filter-modal').style.display = 'inline-block';
        document.getElementById('export-excel-history').style.display = 'inline-block';
        initializeHistoryFilters();
        loadWithdrawalHistory(0);
    }
});

if (driverBalancesBtn) {
    driverBalancesBtn.addEventListener('click', async () => {
        await loadDriverBalances();
        driverBalancesModal.style.display = 'flex';
        driverBalancesModal.classList.add('open');
        document.body.classList.add('modal-open');
    });
    
    discrepanciesBtn.addEventListener('click', async () => {
        // Toggle container visibility
        if (discrepanciesContainer.style.display === 'none' || discrepanciesContainer.style.display === '') {
            // Show container
            currentDiscrepanciesPage = 0;
            discrepanciesFilters = {};
            await loadDiscrepancies();
            await loadDiscrepanciesStatistics();
            discrepanciesContainer.style.display = 'block';
            
            // Hide other containers
            if (historyContainer) historyContainer.style.display = 'none';
            if (entriesContainer) entriesContainer.style.display = 'none';
            if (transfersContainer) transfersContainer.style.display = 'none';
            if (shipmentsContainer) shipmentsContainer.style.display = 'none';
        } else {
            // Hide container
            discrepanciesContainer.style.display = 'none';
        }
    });
}

// Transfers button click event
if (transfersBtn) {
    transfersBtn.addEventListener('click', async () => {
        const filterButton = document.getElementById('open-transfers-filter-modal');
        const filterCounter = document.getElementById('transfers-filter-counter');
        const exportButton = document.getElementById('export-excel-transfers');
        const transfersFilterModal = document.getElementById('transfers-filter-modal');

        if (transfersContainer.style.display === 'block') {
            transfersContainer.style.display = 'none';
            if (filterButton) filterButton.style.display = 'none';
            if (filterCounter) filterCounter.style.display = 'none';
            if (exportButton) exportButton.style.display = 'none';
            if (transfersFilterModal) transfersFilterModal.classList.remove('open');
            document.body.classList.remove('modal-open');
            if (discrepanciesContainer) discrepanciesContainer.style.display = 'none';
        } else {
            historyContainer.style.display = 'none';
            entriesContainer.style.display = 'none';
            shipmentsContainer.style.display = 'none';
            if (discrepanciesContainer) discrepanciesContainer.style.display = 'none';

            transfersContainer.style.display = 'block';
            if (filterButton) filterButton.style.display = 'inline-block';
            if (exportButton) exportButton.style.display = 'inline-block';

            await initializeTransfersFilters();
            updateTransfersFilterCounter();
            currentTransfersPage = 0;
            await loadTransfers();
        }
    });
}

document.getElementById('apply-filters').addEventListener('click', () => {
    updateSelectedFilters();
    currentPage = 0;
    loadWithdrawalHistory(currentPage);
});

document.getElementById('clear-filters').addEventListener('click', () => {
    clearFilters();
});

document.getElementById('entries-prev-page').addEventListener('click', () => {
    if (entriesCurrentPage > 0) {
        loadWarehouseEntries(entriesCurrentPage - 1);
    }
});

document.getElementById('entries-next-page').addEventListener('click', () => {
    if (entriesCurrentPage < entriesTotalPages - 1) {
        loadWarehouseEntries(entriesCurrentPage + 1);
    }
});

document.getElementById('open-history-filter-modal').addEventListener('click', () => {
    document.getElementById('history-filter-modal').classList.add('open');
    document.body.classList.add('modal-open');
});

document.getElementById('history-filter-modal-close').addEventListener('click', () => {
    document.getElementById('history-filter-modal').classList.remove('open');
    document.body.classList.remove('modal-open');
});

document.getElementById('apply-history-filters').addEventListener('click', () => {
    updateHistorySelectedFilters();
    loadWithdrawalHistory(0);
    document.getElementById('history-filter-modal').classList.remove('open');
    document.body.classList.remove('modal-open');
    updateHistoryFilterCounter();
});

document.getElementById('history-filter-counter').addEventListener('click', clearHistoryFilters);

document.getElementById('open-entries-filter-modal').addEventListener('click', () => {
    document.getElementById('entries-filter-modal').classList.add('open');
    document.body.classList.add('modal-open');
});

document.getElementById('entries-filter-modal-close').addEventListener('click', () => {
    document.getElementById('entries-filter-modal').classList.remove('open');
    document.body.classList.remove('modal-open');
});

const applyEntriesFiltersBtn = document.getElementById('apply-entries-filters');
if (applyEntriesFiltersBtn) {
    applyEntriesFiltersBtn.addEventListener('click', () => {
        updateEntriesSelectedFilters();
        loadWarehouseEntries(0);
        document.getElementById('entries-filter-modal').classList.remove('open');
        document.body.classList.remove('modal-open');
        updateEntriesFilterCounter();
    });
} else {
    console.error('Apply entries filters button not found!');
}

document.getElementById('entries-filter-counter').addEventListener('click', clearEntriesFilters);

// Transfers filter modal toggle
const openTransfersFilterModalBtn = document.getElementById('open-transfers-filter-modal');
if (openTransfersFilterModalBtn) {
    openTransfersFilterModalBtn.addEventListener('click', async () => {
        await initializeTransfersFilters();
        const modal = document.getElementById('transfers-filter-modal');
        if (modal) {
            modal.classList.add('open');
        }
        document.body.classList.add('modal-open');
    });
}

const transfersFilterModalClose = document.getElementById('transfers-filter-modal-close');
if (transfersFilterModalClose) {
    transfersFilterModalClose.addEventListener('click', () => {
        const modal = document.getElementById('transfers-filter-modal');
        if (modal) {
            modal.classList.remove('open');
        }
        document.body.classList.remove('modal-open');
    });
}

const transfersFilterCounterElement = document.getElementById('transfers-filter-counter');
if (transfersFilterCounterElement) {
    transfersFilterCounterElement.addEventListener('click', () => clearTransfersFilters(true));
}

async function initializeHistoryFilters() {

    const historySelects = document.querySelectorAll('#history-product-id-filter, #history-withdrawal-reason-id-filter, #history-warehouse-id-filter');
    historySelects.forEach(select => {
        if (!historyCustomSelects[select.id]) {
            historyCustomSelects[select.id] = createCustomSelect(select);
        }
    });

    const productArray = Array.from(productMap.entries()).map(([id, name]) => ({id, name}));
    const warehouseArray = Array.from(warehouseMap.entries()).map(([id, name]) => ({id, name}));
    const withdrawalReasons = getWithdrawalReasonsByPurpose('REMOVING');

    if (historyCustomSelects['history-product-id-filter']) {
        historyCustomSelects['history-product-id-filter'].populate(productArray);
    }
    if (historyCustomSelects['history-warehouse-id-filter']) {
        historyCustomSelects['history-warehouse-id-filter'].populate(warehouseArray);
    }
    if (historyCustomSelects['history-withdrawal-reason-id-filter']) {
        historyCustomSelects['history-withdrawal-reason-id-filter'].populate(withdrawalReasons);
    }

    setDefaultHistoryDates();

    updateHistorySelectedFilters();
}

async function initializeCustomSelects() {
    const selects = document.querySelectorAll('#product-id-filter, #withdrawal-reason-id-filter, #warehouse-id-filter');
    selects.forEach(select => {
        if (!customSelects[select.id]) {
            customSelects[select.id] = createCustomSelect(select);
        }
    });

    try {
        await fetchProducts();
        await fetchWithdrawalReasons();

        const warehouseArray = Array.from(warehouseMap, ([id, name]) => ({id, name}));
        populateSelect('warehouse-id-filter', warehouseArray);
        updateSelectedFilters();
        loadWithdrawalHistory(currentPage);
    } catch (error) {
        console.error('Error initializing selects:', error);
        handleError(error);
    }
}

async function initializeEntriesFilters() {
    const entriesSelects = document.querySelectorAll('#entries-user-id-filter, #entries-product-id-filter, #entries-warehouse-id-filter, #entries-type-filter');
    entriesSelects.forEach(select => {
        if (!entriesCustomSelects[select.id]) {
            entriesCustomSelects[select.id] = createCustomSelect(select);
        }
    });

    const userArray = Array.from(userMap.entries()).map(([id, name]) => ({id, name}));
    const productArray = Array.from(productMap.entries()).map(([id, name]) => ({id, name}));
    const warehouseArray = Array.from(warehouseMap.entries()).map(([id, name]) => ({id, name}));
    const entryTypes = getWithdrawalReasonsByPurpose('ADDING');

    if (entriesCustomSelects['entries-user-id-filter']) {
        entriesCustomSelects['entries-user-id-filter'].populate(userArray);
    }
    if (entriesCustomSelects['entries-product-id-filter']) {
        entriesCustomSelects['entries-product-id-filter'].populate(productArray);
    }
    if (entriesCustomSelects['entries-warehouse-id-filter']) {
        entriesCustomSelects['entries-warehouse-id-filter'].populate(warehouseArray);
    }
    if (entriesCustomSelects['entries-type-filter']) {
        entriesCustomSelects['entries-type-filter'].populate(entryTypes);
    }

    setDefaultEntriesDates();
    updateEntriesSelectedFilters();
}

/**
 * Load driver balance for entry modal when driver and product are selected
 */
async function loadDriverBalanceForEntry() {
    const driverId = document.getElementById('entry-user-id')?.value;
    const productId = document.getElementById('entry-product-id')?.value;
    const driverBalanceInfo = document.getElementById('driver-balance-info');
    const driverNoBalanceWarning = document.getElementById('driver-no-balance-warning');
    
    // Hide both info and warning if driver or product is not selected
    if (!driverId || !productId) {
        if (driverBalanceInfo) {
            driverBalanceInfo.style.display = 'none';
        }
        if (driverNoBalanceWarning) {
            driverNoBalanceWarning.style.display = 'none';
        }
        return;
    }
    
    try {
        const response = await fetch(`/api/v1/driver/balances/${driverId}/product/${productId}`);
        
        if (response.status === 404) {
            // No balance found for this driver/product combination
            if (driverBalanceInfo) {
                driverBalanceInfo.style.display = 'none';
            }
            if (driverNoBalanceWarning) {
                driverNoBalanceWarning.style.display = 'block';
            }
            return;
        }
        
        if (!response.ok) {
            console.error('Failed to load driver balance');
            if (driverBalanceInfo) {
                driverBalanceInfo.style.display = 'none';
            }
            if (driverNoBalanceWarning) {
                driverNoBalanceWarning.style.display = 'none';
            }
            return;
        }
        
        const balance = await response.json();
        
        // Check if balance quantity is zero or null
        if (!balance.quantity || parseFloat(balance.quantity) === 0) {
            // No balance (quantity is 0)
            if (driverBalanceInfo) {
                driverBalanceInfo.style.display = 'none';
            }
            if (driverNoBalanceWarning) {
                driverNoBalanceWarning.style.display = 'block';
            }
            return;
        }
        
        // Display balance info
        if (driverBalanceInfo) {
            document.getElementById('driver-balance-quantity').textContent = formatNumber(balance.quantity, 2);
            document.getElementById('driver-balance-price').textContent = formatNumber(balance.averagePriceEur, 6);
            document.getElementById('driver-balance-total').textContent = formatNumber(balance.totalCostEur, 6);
            driverBalanceInfo.style.display = 'block';
        }
        if (driverNoBalanceWarning) {
            driverNoBalanceWarning.style.display = 'none';
        }
    } catch (error) {
        console.error('Error loading driver balance:', error);
        if (driverBalanceInfo) {
            driverBalanceInfo.style.display = 'none';
        }
        if (driverNoBalanceWarning) {
            driverNoBalanceWarning.style.display = 'none';
        }
    }
}

async function loadDriverBalances() {
    try {
        const response = await fetch('/api/v1/driver/balances/active');
        if (!response.ok) {
            handleError(new Error('Failed to load driver balances'));
            return;
        }
        const balances = await response.json();
        
        // Group balances by driver
        const balancesByDriver = {};
        balances.forEach(balance => {
            if (!balancesByDriver[balance.driverId]) {
                balancesByDriver[balance.driverId] = [];
            }
            balancesByDriver[balance.driverId].push(balance);
        });
        
        const container = document.getElementById('driver-balances-container');
        let html = '';
        
        // Display balances grouped by driver
        for (const [driverId, driverBalances] of Object.entries(balancesByDriver)) {
            const driverName = findNameByIdFromMap(userMap, driverId);
            html += `<h4>Водій: ${driverName}</h4>`;
            html += '<table class="balance-table"><thead><tr>';
            html += '<th>Товар</th>';
            html += '<th>Кількість (кг)</th>';
            html += '<th>Середня ціна (EUR/кг)</th>';
            html += '<th>Загальна вартість (EUR)</th>';
            html += '</tr></thead><tbody>';
            
            let driverTotal = 0;
            for (const balance of driverBalances) {
                const productName = findNameByIdFromMap(productMap, balance.productId);
                const quantity = formatNumber(balance.quantity, 2);
                const avgPrice = formatNumber(balance.averagePriceEur, 6);
                const totalCost = formatNumber(balance.totalCostEur, 6);
                driverTotal += parseFloat(totalCost);
                
                html += '<tr>';
                html += `<td>${productName}</td>`;
                html += `<td>${quantity}</td>`;
                html += `<td>${avgPrice}</td>`;
                html += `<td>${totalCost}</td>`;
                html += '</tr>';
            }
            
            html += '</tbody><tfoot><tr>';
            html += '<td colspan="3"><strong>Загальна вартість товару водія:</strong></td>';
            html += `<td><strong>${formatNumber(driverTotal, 6)} EUR</strong></td>`;
            html += '</tr></tfoot></table>';
        }
        
        if (html === '') {
            html = '<p>Немає активних балансів водіїв</p>';
        }
        
        container.innerHTML = html;
    } catch (error) {
        console.error('Error loading driver balances:', error);
        handleError(error);
    }
}

async function initialize() {
    await fetchProducts();
    await fetchWarehouses();
    await fetchUsers();
    await fetchWithdrawalReasons();

    await initializeHistoryFilters();
    await initializeEntriesFilters();

        loadBalance();
}

initialize();

// ========================================
// SHIPMENTS FUNCTIONALITY
// ========================================

const createShipmentBtn = document.getElementById('create-shipment-btn');
const createShipmentModal = document.getElementById('create-shipment-modal');
const shipmentDetailsModal = document.getElementById('shipment-details-modal');
const createShipmentForm = document.getElementById('create-shipment-form');
const addProductToShipmentModal = document.getElementById('add-product-to-shipment-modal');
const addProductToShipmentForm = document.getElementById('add-product-to-shipment-form');
const updateShipmentForm = document.getElementById('update-shipment-form');
const detailShipmentDateInput = document.getElementById('detail-shipment-date');
const detailShipmentVehicleInput = document.getElementById('detail-shipment-vehicle-number');
const detailShipmentInvoiceUaInput = document.getElementById('detail-shipment-invoice-ua');
const detailShipmentInvoiceEuInput = document.getElementById('detail-shipment-invoice-eu');
const detailShipmentDescriptionInput = document.getElementById('detail-shipment-description');
const editShipmentBtn = document.getElementById('edit-shipment-btn');
const saveShipmentBtn = document.getElementById('save-shipment-btn');
const editShipmentItemModal = document.getElementById('edit-shipment-item-modal');
const editShipmentItemForm = document.getElementById('edit-shipment-item-form');
const editShipmentItemQuantityInput = document.getElementById('edit-shipment-item-quantity');
const editShipmentItemTotalCostInput = document.getElementById('edit-shipment-item-total-cost');
const editShipmentItemModeRadios = document.querySelectorAll('input[name="edit-shipment-item-mode"]');

let currentShipmentId = null;
let shipmentsCache = [];
let currentShipmentDetails = null;
let currentShipmentItems = new Map();
let currentShipmentItemId = null;
let currentTransferItem = null;
let currentBalanceEditData = null;

function populateShipmentForm(shipment) {
    if (!shipment) {
        if (detailShipmentDateInput) detailShipmentDateInput.value = '';
        if (detailShipmentVehicleInput) detailShipmentVehicleInput.value = '';
        if (detailShipmentInvoiceUaInput) detailShipmentInvoiceUaInput.value = '';
        if (detailShipmentInvoiceEuInput) detailShipmentInvoiceEuInput.value = '';
        if (detailShipmentDescriptionInput) detailShipmentDescriptionInput.value = '';
        return;
    }

    if (detailShipmentDateInput) detailShipmentDateInput.value = shipment.shipmentDate || '';
    if (detailShipmentVehicleInput) detailShipmentVehicleInput.value = shipment.vehicleNumber || '';
    if (detailShipmentInvoiceUaInput) detailShipmentInvoiceUaInput.value = shipment.invoiceUa || '';
    if (detailShipmentInvoiceEuInput) detailShipmentInvoiceEuInput.value = shipment.invoiceEu || '';
    if (detailShipmentDescriptionInput) detailShipmentDescriptionInput.value = shipment.description || '';
}

function setShipmentFormEditable(isEditable) {
    const fields = [
        detailShipmentDateInput,
        detailShipmentVehicleInput,
        detailShipmentInvoiceUaInput,
        detailShipmentInvoiceEuInput,
        detailShipmentDescriptionInput
    ];

    fields.forEach(field => {
        if (field) {
            field.disabled = !isEditable;
        }
    });

    if (saveShipmentBtn) {
        saveShipmentBtn.style.display = isEditable ? 'inline-flex' : 'none';
    }
    if (editShipmentBtn) {
        editShipmentBtn.style.display = isEditable ? 'none' : 'inline-flex';
    }
}

function resetShipmentFormState() {
    populateShipmentForm(currentShipmentDetails);
    setShipmentFormEditable(false);
}

if (updateShipmentForm) {
    setShipmentFormEditable(false);
}

if (editShipmentBtn) {
    editShipmentBtn.addEventListener('click', () => {
        if (!currentShipmentDetails) {
            return;
        }
        populateShipmentForm(currentShipmentDetails);
        setShipmentFormEditable(true);
        detailShipmentDateInput?.focus();
    });
}

// Open shipments container
if (shipmentsBtn) {
    shipmentsBtn.addEventListener('click', async () => {
        if (shipmentsContainer.style.display === 'block') {
            // Close shipments container
            shipmentsContainer.style.display = 'none';
        } else {
            // Hide other containers
            document.getElementById('history-container').style.display = 'none';
            document.getElementById('entries-container').style.display = 'none';
            transfersContainer.style.display = 'none';
            if (discrepanciesContainer) discrepanciesContainer.style.display = 'none';
            
            // Show shipments container
            shipmentsContainer.style.display = 'block';
            await loadShipments();
        }
    });
}

// Open create shipment modal
if (createShipmentBtn) {
    createShipmentBtn.addEventListener('click', () => {
        document.getElementById('shipment-date').valueAsDate = new Date();
        createShipmentModal.style.display = 'flex';
        createShipmentModal.classList.add('open');
        document.body.classList.add('modal-open');
    });
}

// Create shipment form submit
if (createShipmentForm) {
    createShipmentForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const shipmentData = {
            shipmentDate: document.getElementById('shipment-date').value,
            vehicleNumber: document.getElementById('shipment-vehicle-number').value,
            invoiceUa: document.getElementById('shipment-invoice-ua').value,
            invoiceEu: document.getElementById('shipment-invoice-eu').value,
            description: document.getElementById('shipment-description').value
        };
        
        try {
            const response = await fetch('/api/v1/shipments', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(shipmentData)
            });
            
            if (!response.ok) {
                throw new Error('Failed to create shipment');
            }
            
            const result = await response.json();
            showMessage('Машину успішно створено', 'success');
            
            closeModal('create-shipment-modal');
            createShipmentForm.reset();
            
            await loadShipments();
        } catch (error) {
            showMessage('Помилка при створенні машини', 'error');
        }
    });
}

// Load shipments list
async function loadShipments() {
    const dateFrom = document.getElementById('shipments-date-from')?.value;
    const dateTo = document.getElementById('shipments-date-to')?.value;
    
    try {
        let url = '/api/v1/shipments/by-date-range?';
        
        if (dateFrom && dateTo) {
            url += `fromDate=${dateFrom}&toDate=${dateTo}`;
        } else {
            // Default: last 30 days
            const today = new Date();
            const last30Days = new Date();
            last30Days.setDate(today.getDate() - 30);
            url += `fromDate=${last30Days.toISOString().split('T')[0]}&toDate=${today.toISOString().split('T')[0]}`;
        }
        
        const response = await fetch(url);
        
        if (!response.ok) {
            throw new Error('Failed to load shipments');
        }
        
        shipmentsCache = await response.json();
        renderShipments(shipmentsCache);
    } catch (error) {
        showMessage('Помилка завантаження машин', 'error');
        
        // Show empty table even on error
        const tbody = document.getElementById('shipments-tbody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">Помилка завантаження даних</td></tr>';
        }
    }
}

// Render shipments table
function renderShipments(shipments) {
    const tbody = document.getElementById('shipments-tbody');
    
    if (!tbody) {
        return;
    }
    
    if (!shipments || shipments.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">Немає даних</td></tr>';
        return;
    }
    
    tbody.innerHTML = shipments.map(shipment => `
        <tr onclick="viewShipmentDetails(${shipment.id})" style="cursor: pointer;">
            <td>${shipment.shipmentDate}</td>
            <td>${shipment.vehicleNumber || '-'}</td>
            <td>${shipment.invoiceUa || '-'}</td>
            <td>${shipment.invoiceEu || '-'}</td>
            <td style="font-weight: bold; color: #FF6F00;">${formatNumber(shipment.totalCostEur, 2)} EUR</td>
            <td>${shipment.description || '-'}</td>
        </tr>
    `).join('');
}

// View shipment details
async function viewShipmentDetails(shipmentId) {
    currentShipmentId = shipmentId;
    
    try {
        const response = await fetch(`/api/v1/shipments/${shipmentId}`);
        
        if (!response.ok) {
            throw new Error('Failed to load shipment details');
        }
        
        const shipment = await response.json();
        renderShipmentDetails(shipment);
        
        shipmentDetailsModal.style.display = 'flex';
        shipmentDetailsModal.classList.add('open');
        document.body.classList.add('modal-open');
    } catch (error) {
        showMessage('Помилка завантаження деталей машини', 'error');
    }
}

// Render shipment details
function renderShipmentDetails(shipment) {
    currentShipmentDetails = shipment;
    currentShipmentItems = new Map();
    populateShipmentForm(shipment);
    setShipmentFormEditable(false);
    
    const itemsTbody = document.getElementById('shipment-items-tbody');
    
    if (!shipment.items || shipment.items.length === 0) {
        itemsTbody.innerHTML = '<tr><td colspan="5" style="text-align: center;">Товари ще не додані</td></tr>';
    } else {
        itemsTbody.innerHTML = shipment.items.map(item => {
            const productName = findNameByIdFromMap(productMap, item.productId) || 'Невідомий товар';
            const warehouseName = findNameByIdFromMap(warehouseMap, item.warehouseId) || 'Невідомий склад';

            currentShipmentItems.set(Number(item.withdrawalId), {
                ...item,
                productName,
                warehouseName
            });

            return `
                <tr class="shipment-item-row" data-item-id="${item.withdrawalId}" style="cursor: pointer;">
                    <td>${productName}</td>
                    <td>${formatNumber(item.quantity, 2)} кг</td>
                    <td style="text-align: right;">${formatNumber(item.unitPriceEur, 6)} EUR</td>
                    <td style="text-align: right; font-weight: bold;">${formatNumber(item.totalCostEur, 6)} EUR</td>
                    <td>${item.withdrawalDate || shipment.shipmentDate}</td>
                </tr>
            `;
        }).join('');
    }
    
    document.getElementById('shipment-total-cost').textContent = formatNumber(shipment.totalCostEur, 2);
}

// Add product to shipment button
document.getElementById('add-product-to-shipment-btn')?.addEventListener('click', () => {
    // Populate warehouses and products
    populateWarehouses('shipment-warehouse-id');
    populateProducts('shipment-product-id');
    
    addProductToShipmentModal.style.display = 'flex';
    addProductToShipmentModal.classList.add('open');
    document.body.classList.add('modal-open');
});

// Add product to shipment form submit
if (addProductToShipmentForm) {
    addProductToShipmentForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const data = {
            warehouseId: Number(document.getElementById('shipment-warehouse-id').value),
            productId: Number(document.getElementById('shipment-product-id').value),
            quantity: Number(document.getElementById('shipment-quantity').value)
        };
        
        try {
            const response = await fetch(`/api/v1/shipments/${currentShipmentId}/products`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(data)
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to add product to shipment');
            }
            
            const updatedShipment = await response.json();
            
            showMessage('Товар успішно додано до машини', 'success');
            closeModal('add-product-to-shipment-modal');
            addProductToShipmentForm.reset();
            
            // Refresh shipment details
            renderShipmentDetails(updatedShipment);
            
            // Refresh shipments list in table
            await loadShipments();
            
            // Reload balance
    loadBalance();
        } catch (error) {
            showMessage(error.message || 'Помилка при додаванні товару до машини', 'error');
        }
    });
}

// Delete shipment button
document.getElementById('delete-shipment-btn')?.addEventListener('click', async () => {
    if (!confirm('Ви впевнені, що хочете видалити цю машину?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/v1/shipments/${currentShipmentId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            throw new Error('Failed to delete shipment');
        }
        
        showMessage('Машину успішно видалено', 'success');
        closeModal('shipment-details-modal');
        await loadShipments();
        await loadBalance();
    } catch (error) {
        showMessage('Помилка при видаленні машини', 'error');
    }
});

// Apply shipments filters
document.getElementById('apply-shipments-filters')?.addEventListener('click', async () => {
    await loadShipments();
});

function exportTableToExcel(tableId, filename = 'withdrawal_data') {
    const table = document.getElementById(tableId);
    const worksheet = XLSX.utils.table_to_sheet(table);
    const workbook = XLSX.utils.book_new();

    const maxWidths = [];
    const rows = XLSX.utils.sheet_to_json(worksheet, {header: 1});
    rows.forEach(row => {
        row.forEach((cell, i) => {
            const cellLength = cell ? String(cell).length : 10;
            maxWidths[i] = Math.max(maxWidths[i] || 10, cellLength);
        });
    });
    worksheet['!cols'] = maxWidths.map(w => ({wch: w}));

    XLSX.utils.book_append_sheet(workbook, worksheet, 'Sheet1');
    XLSX.writeFile(workbook, `${filename}.xlsx`);
}

document.getElementById('export-excel-withdrawal').addEventListener('click', () => {
    exportTableToExcel('history-table', 'withdrawal_export');
});

document.getElementById('export-excel-history').addEventListener('click', () => {
    exportTableToExcel('history-table', 'withdrawal_export');
});

document.getElementById('export-excel-entries').addEventListener('click', () => {
    exportTableToExcel('entries-table', 'entries_export');
});

// ========================================
// DISCREPANCIES FUNCTIONALITY
// ========================================

async function loadDiscrepanciesStatistics() {
    try {
        // Build query params from current filters
        const params = new URLSearchParams();
        
        if (discrepanciesFilters.type) {
            params.append('type', discrepanciesFilters.type);
        }
        if (discrepanciesFilters.dateFrom) {
            params.append('dateFrom', discrepanciesFilters.dateFrom);
        }
        if (discrepanciesFilters.dateTo) {
            params.append('dateTo', discrepanciesFilters.dateTo);
        }
        
        const url = `/api/v1/warehouse/discrepancies/statistics${params.toString() ? '?' + params.toString() : ''}`;
        const response = await fetch(url);
        
        if (!response.ok) {
            throw new Error('Failed to load statistics');
        }
        
        const stats = await response.json();
        
        document.getElementById('total-losses-value').textContent = `${formatNumber(stats.totalLossesValue, 6)} EUR`;
        document.getElementById('total-losses-count').textContent = `${stats.lossCount} записів`;
        document.getElementById('total-gains-value').textContent = `${formatNumber(stats.totalGainsValue, 6)} EUR`;
        document.getElementById('total-gains-count').textContent = `${stats.gainCount} записів`;
        document.getElementById('net-value').textContent = `${formatNumber(stats.netValue, 6)} EUR`;
        
        // Change color based on positive/negative net value
        const netValueElement = document.getElementById('net-value');
        if (stats.netValue < 0) {
            netValueElement.style.color = '#d32f2f';
        } else if (stats.netValue > 0) {
            netValueElement.style.color = '#388e3c';
        } else {
            netValueElement.style.color = '#1976d2';
        }
    } catch (error) {
        console.error('Error loading discrepancies statistics:', error);
    }
}

async function loadDiscrepancies() {
    try {
        const params = new URLSearchParams({
            page: currentDiscrepanciesPage,
            size: discrepanciesPageSize,
            sort: 'receiptDate',
            direction: 'DESC',
            ...discrepanciesFilters
        });
        
        const response = await fetch(`/api/v1/warehouse/discrepancies?${params}`);
        
        if (!response.ok) {
            throw new Error('Failed to load discrepancies');
        }
        
        const data = await response.json();
        
        // Render table
        const tbody = document.getElementById('discrepancies-table-body');
        tbody.innerHTML = '';
        
        if (data.content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="10" style="text-align: center; padding: 30px; color: #999;">Немає даних</td></tr>';
        } else {
            for (const item of data.content) {
                const row = document.createElement('tr');
                
                const driverName = findNameByIdFromMap(userMap, item.driverId);
                const productName = findNameByIdFromMap(productMap, item.productId);
                const warehouseName = findNameByIdFromMap(warehouseMap, item.warehouseId);
                
                const typeLabel = item.type === 'LOSS' ? 'Втрата' : 'Придбання';
                const typeClass = item.type === 'LOSS' ? 'loss' : 'gain';
                const typeColor = item.type === 'LOSS' ? '#d32f2f' : '#388e3c';
                
                row.innerHTML = `
                    <td>${formatDate(item.receiptDate)}</td>
                    <td>${driverName}</td>
                    <td>${productName}</td>
                    <td>${warehouseName}</td>
                    <td style="text-align: center;">${item.purchasedQuantity} кг</td>
                    <td style="text-align: center;">${item.receivedQuantity} кг</td>
                    <td style="text-align: center; font-weight: bold; color: ${typeColor};">
                        ${item.discrepancyQuantity > 0 ? '+' : ''}${item.discrepancyQuantity} кг
                    </td>
                    <td style="text-align: right;">${formatNumber(item.unitPriceEur, 6)} EUR</td>
                    <td style="text-align: right; font-weight: bold;">
                        ${formatNumber(Math.abs(item.discrepancyValueEur), 6)} EUR
                    </td>
                    <td style="text-align: center;">
                        <span class="discrepancy-type-badge ${typeClass}">
                            ${typeLabel}
                        </span>
                    </td>
                `;
                
                tbody.appendChild(row);
            }
        }
        
        // Update pagination
        updateDiscrepanciesPagination(data);
        
    } catch (error) {
        console.error('Error loading discrepancies:', error);
        const tbody = document.getElementById('discrepancies-table-body');
        tbody.innerHTML = '<tr><td colspan="10" style="text-align: center; padding: 30px; color: #d32f2f;">Помилка завантаження даних</td></tr>';
    }
}

function updateDiscrepanciesPagination(data) {
    const start = data.page * data.size + 1;
    const end = Math.min((data.page + 1) * data.size, data.totalElements);
    document.getElementById('discrepancies-info').textContent = `Показано ${start}-${end} з ${data.totalElements}`;
    
    const prevBtn = document.getElementById('discrepancies-prev');
    const nextBtn = document.getElementById('discrepancies-next');
    
    prevBtn.disabled = data.page === 0;
    nextBtn.disabled = data.page >= data.totalPages - 1;
    
    // Generate page numbers
    const pageNumbersContainer = document.getElementById('discrepancies-page-numbers');
    pageNumbersContainer.innerHTML = '';
    
    const maxPagesToShow = 5;
    let startPage = Math.max(0, data.page - Math.floor(maxPagesToShow / 2));
    let endPage = Math.min(data.totalPages, startPage + maxPagesToShow);
    
    if (endPage - startPage < maxPagesToShow) {
        startPage = Math.max(0, endPage - maxPagesToShow);
    }
    
    for (let i = startPage; i < endPage; i++) {
        const pageBtn = document.createElement('button');
        pageBtn.textContent = i + 1;
        pageBtn.className = 'button';
        
        if (i === data.page) {
            pageBtn.classList.add('active');
        }
        
        pageBtn.addEventListener('click', async () => {
            currentDiscrepanciesPage = i;
            await loadDiscrepancies();
        });
        
        pageNumbersContainer.appendChild(pageBtn);
    }
}

function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}.${month}.${year}`;
}

// Event listeners for discrepancies pagination
document.getElementById('discrepancies-prev').addEventListener('click', async () => {
    if (currentDiscrepanciesPage > 0) {
        currentDiscrepanciesPage--;
        await loadDiscrepancies();
    }
});

document.getElementById('discrepancies-next').addEventListener('click', async () => {
    currentDiscrepanciesPage++;
    await loadDiscrepancies();
});

// Event listeners for discrepancies filters
document.getElementById('apply-discrepancy-filters').addEventListener('click', async () => {
    discrepanciesFilters = {};
    
    const type = document.getElementById('discrepancy-type-filter').value;
    const dateFrom = document.getElementById('discrepancy-date-from').value;
    const dateTo = document.getElementById('discrepancy-date-to').value;
    
    if (type) discrepanciesFilters.type = type;
    if (dateFrom) discrepanciesFilters.dateFrom = dateFrom;
    if (dateTo) discrepanciesFilters.dateTo = dateTo;
    
    currentDiscrepanciesPage = 0;
    await loadDiscrepancies();
    await loadDiscrepanciesStatistics(); // Reload statistics with filters
});

document.getElementById('reset-discrepancy-filters').addEventListener('click', async () => {
    document.getElementById('discrepancy-type-filter').value = '';
    document.getElementById('discrepancy-date-from').value = '';
    document.getElementById('discrepancy-date-to').value = '';
    
    discrepanciesFilters = {};
    currentDiscrepanciesPage = 0;
    await loadDiscrepancies();
    await loadDiscrepanciesStatistics(); // Reload statistics without filters
});

// Export discrepancies to Excel
document.getElementById('export-discrepancies-excel').addEventListener('click', async () => {
    try {
        // Build query params from current filters
        const params = new URLSearchParams();
        
        if (discrepanciesFilters.type) {
            params.append('type', discrepanciesFilters.type);
        }
        if (discrepanciesFilters.dateFrom) {
            params.append('dateFrom', discrepanciesFilters.dateFrom);
        }
        if (discrepanciesFilters.dateTo) {
            params.append('dateTo', discrepanciesFilters.dateTo);
        }
        
        const url = `/api/v1/warehouse/discrepancies/export?${params.toString()}`;
        
        // Fetch the file
        const response = await fetch(url, {
            method: 'GET'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        // Get filename from Content-Disposition header or use default with current date
        const contentDisposition = response.headers.get('Content-Disposition');
        const today = new Date().toISOString().split('T')[0];
        let filename = `vtrati_ta_pridbanna_${today}.xlsx`;
        
        if (contentDisposition) {
            // Try multiple patterns to extract filename
            let extractedFilename = null;
            
            // Pattern 1: filename="something.xlsx"
            let match = contentDisposition.match(/filename="([^"]+)"/);
            if (match && match[1]) {
                extractedFilename = match[1];
            }
            
            // Pattern 2: filename=something.xlsx (without quotes)
            if (!extractedFilename) {
                match = contentDisposition.match(/filename=([^;]+)/);
                if (match && match[1]) {
                    extractedFilename = match[1].trim();
                }
            }
            
            // Pattern 3: filename*=UTF-8''something.xlsx
            if (!extractedFilename) {
                match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/);
                if (match && match[1]) {
                    extractedFilename = decodeURIComponent(match[1]);
                }
            }
            
            if (extractedFilename) {
                // Remove any trailing special characters and ensure .xlsx extension
                filename = extractedFilename.replace(/['";\s]+$/, '').replace(/_+$/, '');
                if (!filename.endsWith('.xlsx')) {
                    filename += '.xlsx';
                }
            }
        }
        
        // Create blob and download
        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(downloadUrl);
        document.body.removeChild(a);
        
        showMessage('Excel файл успішно завантажено!', 'info');
    } catch (error) {
        console.error('Error exporting to Excel:', error);
        showMessage('Помилка при експорті в Excel', 'error');
    }
});

// ============================================
// TRANSFERS CONTAINER FUNCTIONALITY
// ============================================

// Load transfers with pagination
async function loadTransfers() {
    try {
        const params = new URLSearchParams({
            page: currentTransfersPage,
            size: transfersPageSize,
            sort: 'transferDate',
            direction: 'desc'
        });
        
        if (transfersFilters.dateFrom) params.append('dateFrom', transfersFilters.dateFrom);
        if (transfersFilters.dateTo) params.append('dateTo', transfersFilters.dateTo);
        if (transfersFilters.warehouseId) params.append('warehouseId', transfersFilters.warehouseId);
        if (transfersFilters.fromProductId) params.append('fromProductId', transfersFilters.fromProductId);
        if (transfersFilters.toProductId) params.append('toProductId', transfersFilters.toProductId);
        if (transfersFilters.userId) params.append('userId', transfersFilters.userId);
        if (transfersFilters.reasonId) params.append('reasonId', transfersFilters.reasonId);
        
        const response = await fetch(`/api/v1/warehouse/transfers?${params}`);
        
        if (!response.ok) {
            throw new Error('Failed to load transfers');
        }
        
        const data = await response.json();
        
        renderTransfers(data.content);
        updateTransfersPagination(data);
        
    } catch (error) {
        console.error('Error loading transfers:', error);
        showMessage('Помилка завантаження переміщень', 'error');
    }
}

// Render transfers table
function renderTransfers(transfers) {
    const tbody = document.getElementById('transfers-body');
    if (!tbody) return;
    
    if (!Array.isArray(transfers) || transfers.length === 0) {
        transfersCache = [];
        tbody.innerHTML = '<tr><td colspan="10" style="text-align: center;">Немає даних</td></tr>';
        return;
    }
    
    transfersCache = transfers.slice();
    
    tbody.innerHTML = transfers.map(item => {
        const fromProductName = findNameByIdFromMap(productMap, item.fromProductId) || 'Не вказано';
        const toProductName = findNameByIdFromMap(productMap, item.toProductId) || 'Не вказано';
        const warehouseName = findNameByIdFromMap(warehouseMap, item.warehouseId) || 'Не вказано';
        const userName = findNameByIdFromMap(userMap, item.userId) || 'Не вказано';
        const reasonObj = withdrawalReasonMap.get(Number(item.reasonId));
        const reasonName = reasonObj ? reasonObj.name : 'Не вказано';
        
        return `
            <tr data-id="${item.id}">
                <td style="text-align: center;">${item.transferDate || ''}</td>
                <td>${warehouseName}</td>
                <td>${fromProductName}</td>
                <td>${toProductName}</td>
                <td style="text-align: center;">${formatNumber(item.quantity, 2)} кг</td>
                <td style="text-align: right;">${formatNumber(item.unitPriceEur, 6)} EUR</td>
                <td style="text-align: right; font-weight: bold;">${formatNumber(item.totalCostEur, 6)} EUR</td>
                <td>${userName}</td>
                <td>${reasonName}</td>
                <td>${item.description || ''}</td>
            </tr>
        `;
    }).join('');
    
    tbody.querySelectorAll('tr[data-id]').forEach(row => {
        row.addEventListener('click', () => openEditTransferModal(Number(row.dataset.id)));
    });
}

// Update transfers pagination
function updateTransfersPagination(data) {
    const totalPages = data.totalPages || 1;
    const currentPage = data.number || 0;
    
    const infoSpan = document.getElementById('transfers-page-info');
    if (infoSpan) {
        infoSpan.textContent = `Сторінка ${currentPage + 1} з ${totalPages}`;
    }
    
    const prevBtn = document.getElementById('transfers-prev-page');
    const nextBtn = document.getElementById('transfers-next-page');
    
    if (prevBtn) {
        prevBtn.disabled = currentPage === 0;
    }
    
    if (nextBtn) {
        nextBtn.disabled = currentPage >= totalPages - 1;
    }
}

// Apply transfer filters
const applyTransferFiltersBtn = document.getElementById('apply-transfer-filters');
if (applyTransferFiltersBtn) {
    applyTransferFiltersBtn.addEventListener('click', async () => {
        updateTransfersSelectedFilters();
        currentTransfersPage = 0;
        await loadTransfers();
        const modal = document.getElementById('transfers-filter-modal');
        if (modal) {
            modal.classList.remove('open');
        }
        document.body.classList.remove('modal-open');
    });
}

// Reset transfer filters
const clearTransferFiltersBtn = document.getElementById('clear-transfer-filters');
if (clearTransferFiltersBtn) {
    clearTransferFiltersBtn.addEventListener('click', () => {
        clearTransfersFilters();
    });
}

// Transfers pagination - Previous
document.getElementById('transfers-prev-page').addEventListener('click', async () => {
    if (currentTransfersPage > 0) {
        currentTransfersPage--;
        await loadTransfers();
    }
});

// Transfers pagination - Next
document.getElementById('transfers-next-page').addEventListener('click', async () => {
    currentTransfersPage++;
    await loadTransfers();
});

// Export transfers to Excel
async function exportTransfersToExcel() {
    try {
        const params = new URLSearchParams();
        
        if (transfersFilters.dateFrom) params.append('dateFrom', transfersFilters.dateFrom);
        if (transfersFilters.dateTo) params.append('dateTo', transfersFilters.dateTo);
        if (transfersFilters.warehouseId) params.append('warehouseId', transfersFilters.warehouseId);
        if (transfersFilters.fromProductId) params.append('fromProductId', transfersFilters.fromProductId);
        if (transfersFilters.toProductId) params.append('toProductId', transfersFilters.toProductId);
        if (transfersFilters.userId) params.append('userId', transfersFilters.userId);
        if (transfersFilters.reasonId) params.append('reasonId', transfersFilters.reasonId);
        
        const response = await fetch(`/api/v1/warehouse/transfers/export?${params}`);
        
        if (!response.ok) {
            throw new Error('Failed to export transfers');
        }
        
        let filename = 'product_transfers.xlsx';
        const contentDisposition = response.headers.get('Content-Disposition');
        if (contentDisposition) {
            const filenameMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i);
            if (filenameMatch) {
                filename = decodeURIComponent(filenameMatch[1] || filenameMatch[2]);
            }
        }
        
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = filename.endsWith('.xlsx') ? filename : `${filename}.xlsx`;
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
        window.URL.revokeObjectURL(url);
    } catch (error) {
        console.error('Error exporting transfers:', error);
        showMessage('Помилка експорту переміщень', 'error');
    }
}

const exportTransfersBtn = document.getElementById('export-excel-transfers');
if (exportTransfersBtn) {
    exportTransfersBtn.addEventListener('click', exportTransfersToExcel);
}

if (updateShipmentForm) {
    updateShipmentForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!currentShipmentId) {
            showMessage('Не вдалося визначити машину для оновлення', 'error');
            return;
        }
        
        const payload = {
            shipmentDate: detailShipmentDateInput?.value || null,
            vehicleNumber: detailShipmentVehicleInput?.value ?? null,
            invoiceUa: detailShipmentInvoiceUaInput?.value ?? null,
            invoiceEu: detailShipmentInvoiceEuInput?.value ?? null,
            description: detailShipmentDescriptionInput?.value ?? null
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
            const response = await fetch(`/api/v1/shipments/${currentShipmentId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Не вдалося оновити машину');
            }
            
            const updatedShipment = await response.json();
            showMessage('Дані машини оновлено', 'success');
            renderShipmentDetails(updatedShipment);
            await loadShipments();
            await loadBalance();
            closeModal('edit-shipment-item-modal');
        } catch (error) {
            showMessage(error.message || 'Помилка при оновленні машини', 'error');
        }
    });
}

function setDefaultTransfersDates() {
    const today = new Date();
    const fromDate = new Date();
    fromDate.setDate(today.getDate() - 30);
    const formattedToday = today.toISOString().split('T')[0];
    const formattedFrom = fromDate.toISOString().split('T')[0];

    const fromInput = document.getElementById('transfer-date-from-filter');
    const toInput = document.getElementById('transfer-date-to-filter');

    if (fromInput && !fromInput.value) {
        fromInput.value = formattedFrom;
    }
    if (toInput && !toInput.value) {
        toInput.value = formattedToday;
    }
}

function updateTransfersSelectedFilters() {
    const dateFromInput = document.getElementById('transfer-date-from-filter');
    const dateToInput = document.getElementById('transfer-date-to-filter');

    const warehouseValues = transfersCustomSelects['transfer-warehouse-filter']
        ? transfersCustomSelects['transfer-warehouse-filter'].getValue()
        : [];
    const fromProductValues = transfersCustomSelects['transfer-from-product-filter']
        ? transfersCustomSelects['transfer-from-product-filter'].getValue()
        : [];
    const toProductValues = transfersCustomSelects['transfer-to-product-filter']
        ? transfersCustomSelects['transfer-to-product-filter'].getValue()
        : [];
    const userValues = transfersCustomSelects['transfer-user-filter']
        ? transfersCustomSelects['transfer-user-filter'].getValue()
        : [];
    const reasonValues = transfersCustomSelects['transfer-reason-filter']
        ? transfersCustomSelects['transfer-reason-filter'].getValue()
        : [];

    transfersFilters = {
        dateFrom: dateFromInput?.value || null,
        dateTo: dateToInput?.value || null,
        warehouseId: warehouseValues[0] || null,
        fromProductId: fromProductValues[0] || null,
        toProductId: toProductValues[0] || null,
        userId: userValues[0] || null,
        reasonId: reasonValues[0] || null
    };

    updateTransfersFilterCounter();
}

function updateTransfersFilterCounter() {
    const counterElement = document.getElementById('transfers-filter-counter');
    const countElement = document.getElementById('transfers-filter-count');

    if (!counterElement || !countElement) {
        return;
    }

    const activeFilters = Object.values(transfersFilters).filter(Boolean);
    if (activeFilters.length > 0) {
        countElement.textContent = activeFilters.length;
        counterElement.style.display = 'inline-flex';
    } else {
        counterElement.style.display = 'none';
    }
}

function clearTransfersFilters(closeModal = false) {
    const form = document.getElementById('transfers-filter-form');
    if (form) {
        form.reset();
    }

    Object.keys(transfersCustomSelects).forEach(selectId => {
        transfersCustomSelects[selectId].reset();
    });

    transfersFilters = {};
    setDefaultTransfersDates();
    updateTransfersSelectedFilters();
    updateTransfersFilterCounter();
    currentTransfersPage = 0;
    loadTransfers();
 
    if (closeModal) {
        const modal = document.getElementById('transfers-filter-modal');
        if (modal) {
            modal.classList.remove('open');
        }
        document.body.classList.remove('modal-open');
    }
}

async function initializeTransfersFilters() {
    const selectElements = document.querySelectorAll('#transfer-warehouse-filter, #transfer-from-product-filter, #transfer-to-product-filter, #transfer-user-filter, #transfer-reason-filter');
    selectElements.forEach(select => {
        if (!select) return;
        if (!transfersCustomSelects[select.id]) {
            transfersCustomSelects[select.id] = createCustomSelect(select);
        }
    });

    const warehouseArray = Array.from(warehouseMap.entries()).map(([id, name]) => ({id, name}));
    const productArray = Array.from(productMap.entries()).map(([id, name]) => ({id, name}));
    const userArray = Array.from(userMap.entries()).map(([id, name]) => ({id, name}));
    const reasonArray = Array.from(withdrawalReasonMap.entries()).map(([id, reason]) => ({id, name: reason.name}));

    if (transfersCustomSelects['transfer-warehouse-filter']) {
        transfersCustomSelects['transfer-warehouse-filter'].populate(warehouseArray);
        if (transfersFilters.warehouseId) {
            transfersCustomSelects['transfer-warehouse-filter'].setValue(transfersFilters.warehouseId);
        }
    }
    if (transfersCustomSelects['transfer-from-product-filter']) {
        transfersCustomSelects['transfer-from-product-filter'].populate(productArray);
        if (transfersFilters.fromProductId) {
            transfersCustomSelects['transfer-from-product-filter'].setValue(transfersFilters.fromProductId);
        }
    }
    if (transfersCustomSelects['transfer-to-product-filter']) {
        transfersCustomSelects['transfer-to-product-filter'].populate(productArray);
        if (transfersFilters.toProductId) {
            transfersCustomSelects['transfer-to-product-filter'].setValue(transfersFilters.toProductId);
        }
    }
    if (transfersCustomSelects['transfer-user-filter']) {
        transfersCustomSelects['transfer-user-filter'].populate(userArray);
        if (transfersFilters.userId) {
            transfersCustomSelects['transfer-user-filter'].setValue(transfersFilters.userId);
        }
    }
    if (transfersCustomSelects['transfer-reason-filter']) {
        const bothReasons = getWithdrawalReasonsByPurpose('BOTH');
        transfersCustomSelects['transfer-reason-filter'].populate(bothReasons);
        if (transfersFilters.reasonId) {
            transfersCustomSelects['transfer-reason-filter'].setValue(transfersFilters.reasonId);
        }
    }
 
    const fromInput = document.getElementById('transfer-date-from-filter');
    const toInput = document.getElementById('transfer-date-to-filter');
    if (fromInput && transfersFilters.dateFrom) {
        fromInput.value = transfersFilters.dateFrom;
    }
    if (toInput && transfersFilters.dateTo) {
        toInput.value = transfersFilters.dateTo;
    }

    setDefaultTransfersDates();
    updateTransfersSelectedFilters();
}

function updateShipmentItemMode() {
    const mode = document.querySelector('input[name="edit-shipment-item-mode"]:checked')?.value;
    if (!mode) {
        return;
    }

    const quantityEnabled = mode === 'quantity';

    if (editShipmentItemQuantityInput) {
        editShipmentItemQuantityInput.disabled = !quantityEnabled;
        editShipmentItemQuantityInput.classList.toggle('shipment-item-edit-disabled', !quantityEnabled);
    }
    if (editShipmentItemTotalCostInput) {
        editShipmentItemTotalCostInput.disabled = quantityEnabled;
        editShipmentItemTotalCostInput.classList.toggle('shipment-item-edit-disabled', quantityEnabled);
    }

    if (editShipmentItemModeRadios.length > 0) {
        editShipmentItemModeRadios.forEach(radio => {
            const label = radio.closest('label');
            if (label) {
                label.classList.toggle('active', radio.checked);
            }
        });
    }
}

function openEditShipmentItemModal(itemId) {
    if (!currentShipmentDetails) {
        showMessage('Неможливо відредагувати товар: машина не вибрана', 'error');
        return;
    }

    const item = currentShipmentItems.get(Number(itemId));
    if (!item) {
        showMessage('Товар не знайдений або вже оновлений', 'error');
        return;
    }

    currentShipmentItemId = Number(itemId);

    if (editShipmentItemQuantityInput) {
        editShipmentItemQuantityInput.value = parseFloat(item.quantity).toFixed(2);
    }
    if (editShipmentItemTotalCostInput) {
        editShipmentItemTotalCostInput.value = parseFloat(item.totalCostEur).toFixed(6);
    }

    const quantityModeRadio = document.querySelector('input[name="edit-shipment-item-mode"][value="quantity"]');
    if (quantityModeRadio) {
        quantityModeRadio.checked = true;
    }
    updateShipmentItemMode();

    if (editShipmentItemModal) {
        editShipmentItemModal.style.display = 'flex';
        editShipmentItemModal.classList.add('open');
    }
    document.body.classList.add('modal-open');
}

const shipmentItemsTbody = document.getElementById('shipment-items-tbody');
if (shipmentItemsTbody) {
    shipmentItemsTbody.addEventListener('click', (event) => {
        const row = event.target.closest('tr[data-item-id]');
        if (!row) {
            return;
        }
        const itemId = row.dataset.itemId;
        if (itemId) {
            openEditShipmentItemModal(itemId);
        }
    });
}

if (editShipmentItemModeRadios.length > 0) {
    editShipmentItemModeRadios.forEach(radio => {
        radio.addEventListener('change', updateShipmentItemMode);
    });
    updateShipmentItemMode();
}

if (editShipmentItemForm) {
    editShipmentItemForm.addEventListener('submit', async (event) => {
        event.preventDefault();

        if (!currentShipmentId || currentShipmentItemId === null) {
            showMessage('Не вдалося визначити товар для оновлення', 'error');
            return;
        }

        const item = currentShipmentItems.get(Number(currentShipmentItemId));
        if (!item) {
            showMessage('Товар не знайдений або вже оновлений', 'error');
            return;
        }

        const mode = document.querySelector('input[name="edit-shipment-item-mode"]:checked')?.value;
        const payload = {};

        if (mode === 'quantity') {
            const newQuantityValue = parseFloat(editShipmentItemQuantityInput.value);
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
            const newTotalValue = parseFloat(editShipmentItemTotalCostInput.value);
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
            const response = await fetch(`/api/v1/shipments/${currentShipmentId}/products/${currentShipmentItemId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || 'Не вдалося оновити товар у машині');
            }

            const updatedShipment = await response.json();
            showMessage('Дані товару у машині оновлено', 'success');
            renderShipmentDetails(updatedShipment);
            await loadShipments();
            closeModal('edit-shipment-item-modal');
            await loadBalance();
        } catch (error) {
            showMessage(error.message || 'Помилка при оновленні товару у машині', 'error');
        }
    });
}

function openEditTransferModal(id) {
    const transfer = transfersCache.find(item => Number(item.id) === Number(id));
    if (!transfer) {
        showMessage('Не вдалося знайти переміщення для редагування', 'error');
        return;
    }

    currentTransferItem = transfer;
    const hiddenIdInput = document.getElementById('edit-transfer-id');
    if (hiddenIdInput) {
        hiddenIdInput.value = transfer.id;
    }

    populateTransferReasons('edit-transfer-reason-id');
    if (editTransferReasonSelect) {
        editTransferReasonSelect.value = transfer.reasonId ? String(transfer.reasonId) : '';
    }

    if (editTransferQuantityInput) {
        const numericQuantity = parseFloat(transfer.quantity);
        editTransferQuantityInput.value = Number.isNaN(numericQuantity) ? '' : numericQuantity.toFixed(2);
    }

    if (editTransferDescriptionInput) {
        editTransferDescriptionInput.value = transfer.description || '';
    }

    if (editTransferDateSpan) {
        editTransferDateSpan.textContent = transfer.transferDate || '—';
    }

    if (editTransferWarehouseSpan) {
        editTransferWarehouseSpan.textContent = findNameByIdFromMap(warehouseMap, transfer.warehouseId) || '—';
    }

    if (editTransferFromProductSpan) {
        editTransferFromProductSpan.textContent = findNameByIdFromMap(productMap, transfer.fromProductId) || '—';
    }

    if (editTransferToProductSpan) {
        editTransferToProductSpan.textContent = findNameByIdFromMap(productMap, transfer.toProductId) || '—';
    }

    if (editTransferModal) {
        editTransferModal.style.display = 'flex';
        editTransferModal.classList.add('open');
        document.body.classList.add('modal-open');
    }
}

if (editTransferForm) {
    editTransferForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (!currentTransferItem) {
            showMessage('Не вдалося знайти переміщення для редагування', 'error');
            return;
        }

        const idInput = document.getElementById('edit-transfer-id');
        const id = idInput ? Number(idInput.value) : null;
        if (!id) {
            showMessage('Невірний ідентифікатор переміщення', 'error');
            return;
        }

        const reasonId = editTransferReasonSelect ? Number(editTransferReasonSelect.value) : NaN;
        if (!reasonId) {
            showMessage('Оберіть причину переміщення', 'error');
            return;
        }

        const rawQuantity = editTransferQuantityInput ? parseFloat(editTransferQuantityInput.value) : NaN;
        if (Number.isNaN(rawQuantity) || rawQuantity < 0) {
            showMessage('Вкажіть коректну кількість (0 або більше)', 'error');
            return;
        }

        const roundedQuantity = Number(rawQuantity.toFixed(2));
        const descriptionRaw = editTransferDescriptionInput ? editTransferDescriptionInput.value : '';
        const descriptionTrimmed = descriptionRaw ? descriptionRaw.trim() : '';
        const descriptionValue = descriptionTrimmed.length ? descriptionTrimmed : null;

        const originalQuantity = parseFloat(currentTransferItem.quantity);
        const originalReasonId = currentTransferItem.reasonId || null;
        const originalDescription = currentTransferItem.description || '';

        if (roundedQuantity === 0) {
            const fromProductName = findNameByIdFromMap(productMap, currentTransferItem.fromProductId) || 'товару';
            const confirmRemoval = confirm(`Ви впевнені, що хочете повністю скасувати переміщення з ${fromProductName}?`);
            if (!confirmRemoval) {
                return;
            }
        }

        const hasQuantityChange = Math.abs(roundedQuantity - originalQuantity) > 0.0001;
        const hasReasonChange = reasonId !== originalReasonId;
        const hasDescriptionChange = (descriptionValue || '') !== (originalDescription || '');

        if (!hasQuantityChange && !hasReasonChange && !hasDescriptionChange) {
            showMessage('Зміни відсутні', 'info');
            return;
        }

        const payload = {
            quantity: roundedQuantity,
            reasonId,
            description: descriptionValue
        };

        try {
            const response = await fetch(`/api/v1/warehouse/transfers/${id}`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            });

            if (response.status === 204) {
                showMessage('Переміщення успішно видалено', 'info');
                closeModal('edit-transfer-modal');
                await loadBalance();
                await loadTransfers();
                return;
            }

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                handleError(new Error(errorData.message || 'Не вдалося оновити переміщення'));
                return;
            }

            await response.json().catch(() => null);
            showMessage('Переміщення успішно оновлено', 'info');
            closeModal('edit-transfer-modal');
            await loadBalance();
            await loadTransfers();
        } catch (error) {
            console.error('Error updating transfer:', error);
            handleError(error);
        }
    });
}

function attachBalanceRowListeners() {
    const rows = document.querySelectorAll('.balance-row');
    rows.forEach(row => {
        row.addEventListener('click', () => {
            const data = row.dataset;
            openBalanceEditModal({
                warehouseId: Number(data.warehouseId),
                productId: Number(data.productId),
                warehouseName: data.warehouseName,
                productName: data.productName,
                quantity: parseFloat(data.quantity ?? '0') || 0,
                totalCost: parseFloat(data.totalCost ?? '0') || 0,
                averagePrice: parseFloat(data.averagePrice ?? '0') || 0
            });
        });
    });
}

function resetBalanceEditModal() {
    if (balanceEditForm) {
        balanceEditForm.reset();
    }
    currentBalanceEditData = null;
    balanceEditQuantityInput?.removeAttribute('disabled');
    balanceEditTotalCostInput?.setAttribute('disabled', 'disabled');
    balanceEditTotalCostInput && (balanceEditTotalCostInput.value = '');
    balanceEditQuantityInput && (balanceEditQuantityInput.value = '');
    balanceEditDescriptionInput && (balanceEditDescriptionInput.value = '');
    balanceEditModeRadios.forEach(radio => {
        radio.checked = radio.value === 'quantity';
        const label = radio.closest('label');
        if (label) {
            label.classList.toggle('active', radio.checked);
        }
    });
    if (balanceHistoryBody) {
        balanceHistoryBody.innerHTML = '';
    }
    if (balanceHistoryEmpty) {
        balanceHistoryEmpty.style.display = 'none';
    }

    updateBalanceEditMode();
}

function updateBalanceEditMode() {
    const selected = document.querySelector('input[name="balance-edit-mode"]:checked');
    if (!selected) {
        return;
    }
    balanceEditModeRadios.forEach(radio => {
        const label = radio.closest('label');
        if (label) {
            label.classList.toggle('active', radio === selected);
        }
    });

    if (selected.value === 'quantity') {
        balanceEditQuantityInput?.removeAttribute('disabled');
        balanceEditTotalCostInput?.setAttribute('disabled', 'disabled');
        balanceEditQuantityInput?.focus();
    } else {
        balanceEditTotalCostInput?.removeAttribute('disabled');
        balanceEditQuantityInput?.setAttribute('disabled', 'disabled');
        balanceEditTotalCostInput?.focus();
    }
}

balanceEditModeRadios.forEach(radio => {
    radio.addEventListener('change', updateBalanceEditMode);
});

function openBalanceEditModal(data) {
    if (!balanceEditModal) {
        return;
    }

    resetBalanceEditModal();
    currentBalanceEditData = data;

    balanceEditWarehouseIdInput && (balanceEditWarehouseIdInput.value = data.warehouseId || '');
    balanceEditProductIdInput && (balanceEditProductIdInput.value = data.productId || '');

    if (balanceEditQuantityInput && Number.isFinite(data.quantity)) {
        balanceEditQuantityInput.value = Number(data.quantity).toFixed(2);
    }
    if (balanceEditTotalCostInput && Number.isFinite(data.totalCost)) {
        balanceEditTotalCostInput.value = Number(data.totalCost).toFixed(6);
    }

    updateBalanceEditMode();

    balanceEditModal.style.display = 'flex';
    balanceEditModal.classList.add('open');
    document.body.classList.add('modal-open');

    loadBalanceHistory(data.warehouseId, data.productId).catch(error => {
        console.error('Error loading balance history:', error);
    });
}

async function loadBalanceHistory(warehouseId, productId) {
    if (!balanceHistoryBody || !balanceHistoryEmpty) {
        return;
    }

    balanceHistoryBody.innerHTML = '<tr><td colspan="7" style="text-align: center;">Завантаження...</td></tr>';
    balanceHistoryEmpty.style.display = 'none';

    try {
        const response = await fetch(`/api/v1/warehouse/balances/${warehouseId}/product/${productId}/history`);
        if (!response.ok) {
            throw new Error('Не вдалося завантажити історію змін');
        }
        const history = await response.json();

        if (!Array.isArray(history) || history.length === 0) {
            balanceHistoryBody.innerHTML = '';
            balanceHistoryEmpty.style.display = 'block';
            return;
        }

        const typeLabels = {
            QUANTITY: 'Кількість',
            TOTAL_COST: 'Загальна вартість',
            BOTH: 'Кількість та вартість'
        };

        balanceHistoryBody.innerHTML = history.map(item => {
            const userName = findNameByIdFromMap(userMap, item.userId) || '—';
            const typeLabel = typeLabels[item.adjustmentType] || item.adjustmentType || '—';
            const createdAt = item.createdAt ? new Date(item.createdAt).toLocaleString() : '—';
            const quantityChange = `${formatNumber(item.previousQuantity, 2)} → ${formatNumber(item.newQuantity, 2)} кг`;
            const totalChange = `${formatNumber(item.previousTotalCostEur, 6)} → ${formatNumber(item.newTotalCostEur, 6)} EUR`;
            const averageChange = `${formatNumber(item.previousAveragePriceEur, 6)} → ${formatNumber(item.newAveragePriceEur, 6)} EUR/кг`;
            const description = item.description || '—';

            return `
                <tr>
                    <td>${createdAt}</td>
                    <td>${userName}</td>
                    <td>${typeLabel}</td>
                    <td>${quantityChange}</td>
                    <td>${totalChange}</td>
                    <td>${averageChange}</td>
                    <td>${description}</td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        console.error('Error loading balance history:', error);
        balanceHistoryBody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: #e53935;">Помилка завантаження історії</td></tr>';
        balanceHistoryEmpty.style.display = 'none';
    }
}

if (balanceEditForm) {
    balanceEditForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (!currentBalanceEditData) {
            showMessage('Не вдалося визначити баланс для редагування', 'error');
            return;
        }

        const modeRadio = document.querySelector('input[name="balance-edit-mode"]:checked');
        if (!modeRadio) {
            showMessage('Оберіть режим редагування', 'error');
            return;
        }

        const mode = modeRadio.value;
        const descriptionRaw = balanceEditDescriptionInput ? balanceEditDescriptionInput.value : '';
        const descriptionValue = descriptionRaw ? descriptionRaw.trim() : '';
        const payload = {};

        if (mode === 'quantity') {
            const rawValue = balanceEditQuantityInput ? parseFloat(balanceEditQuantityInput.value) : NaN;
            if (Number.isNaN(rawValue) || rawValue < 0) {
                showMessage('Вкажіть коректну кількість (0 або більше)', 'error');
                return;
            }
            const rounded = Number(rawValue.toFixed(2));
            const currentQuantity = currentBalanceEditData.quantity || 0;
            if (Math.abs(rounded - currentQuantity) < 0.0001 && !descriptionValue) {
                showMessage('Зміни відсутні', 'info');
                return;
            }
            payload.newQuantity = rounded;
        } else {
            const rawValue = balanceEditTotalCostInput ? parseFloat(balanceEditTotalCostInput.value) : NaN;
            if (Number.isNaN(rawValue) || rawValue < 0) {
                showMessage('Вкажіть коректну суму (0 або більше)', 'error');
                return;
            }
            const rounded = Number(rawValue.toFixed(6));
            const currentTotal = currentBalanceEditData.totalCost || 0;
            if (Math.abs(rounded - currentTotal) < 0.000001 && !descriptionValue) {
                showMessage('Зміни відсутні', 'info');
                return;
            }
            payload.newTotalCostEur = rounded;
        }

        const hasQuantityChange = Object.prototype.hasOwnProperty.call(payload, 'newQuantity');
        const hasTotalCostChange = Object.prototype.hasOwnProperty.call(payload, 'newTotalCostEur');

        if (!hasQuantityChange && !hasTotalCostChange && !descriptionValue) {
            showMessage('Зміни відсутні', 'info');
            return;
        }

        if (descriptionValue) {
            payload.description = descriptionValue;
        }

        const warehouseId = currentBalanceEditData.warehouseId;
        const productId = currentBalanceEditData.productId;

        try {
            const response = await fetch(`/api/v1/warehouse/balances/${warehouseId}/product/${productId}`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                handleError(new Error(errorData.message || 'Не вдалося оновити баланс'));
                return;
            }

            await response.json().catch(() => null);
            showMessage('Баланс успішно оновлено', 'info');
            closeModal('balance-edit-modal');
            await loadBalance();
        } catch (error) {
            console.error('Error updating balance:', error);
            handleError(error);
        }
    });
}

if (balanceHistoryBtn) {
    balanceHistoryBtn.addEventListener('click', async () => {
        if (historyContainer.style.display === 'block') {
            historyContainer.style.display = 'none';
            document.getElementById('open-history-filter-modal').style.display = 'none';
            document.getElementById('history-filter-counter').style.display = 'none';
            document.getElementById('export-excel-history').style.display = 'none';
        } else {
            // Hide other containers
            document.getElementById('entries-container').style.display = 'none';
            transfersContainer.style.display = 'none';
            shipmentsContainer.style.display = 'none';
            
            historyContainer.style.display = 'block';
            document.getElementById('open-history-filter-modal').style.display = 'inline-block';
            document.getElementById('export-excel-history').style.display = 'inline-block';
            initializeHistoryFilters();
            loadWithdrawalHistory(0);
        }
    });
}

function resetBalanceHistoryModal() {
    if (balanceHistoryBody) {
        balanceHistoryBody.innerHTML = '';
    }
    if (balanceHistoryEmpty) {
        balanceHistoryEmpty.style.display = 'none';
    }
}

document.addEventListener('click', async (event) => {
    const historyButton = event.target.closest('#balance-history-btn');
    if (!historyButton) {
        return;
    }

    if (!currentBalanceEditData) {
        showMessage('Не вдалося визначити баланс для перегляду історії', 'error');
        return;
    }

    resetBalanceHistoryModal();
    if (balanceHistoryModal) {
        balanceHistoryModal.style.display = 'flex';
        balanceHistoryModal.classList.add('open');
        document.body.classList.add('modal-open');
    }

    try {
        await loadBalanceHistory(currentBalanceEditData.warehouseId, currentBalanceEditData.productId);
    } catch (error) {
        console.error('Error loading balance history:', error);
    }
});