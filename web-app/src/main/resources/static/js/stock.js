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

function escapeHtml(text) {
    if (text == null) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatNumber(value, maxDecimals = 6) {
    if (value === null || value === undefined || value === '') return '0';
    const num = parseFloat(value);
    if (isNaN(num)) return '0';
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
    select.textContent = '';
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = 'Оберіть причину';
    select.appendChild(defaultOption);
    reasons.forEach(reason => {
        const option = document.createElement('option');
        option.value = String(reason.id);
        option.textContent = reason.name || '';
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
        
        const balancesByWarehouse = {};
        balances.forEach(balance => {
            if (!balancesByWarehouse[balance.warehouseId]) {
                balancesByWarehouse[balance.warehouseId] = [];
            }
            balancesByWarehouse[balance.warehouseId].push(balance);
        });
        
        const sortedWarehouseIds = Object.keys(balancesByWarehouse).sort((a, b) => Number(a) - Number(b));
        
        const container = document.getElementById('balance-container');
        if (!container) return;
        container.textContent = '';
        
        if (sortedWarehouseIds.length === 0) {
            const emptyMessage = document.createElement('p');
            emptyMessage.textContent = 'Немає активних балансів на складі';
            container.appendChild(emptyMessage);
            return;
        }
        
        for (const warehouseId of sortedWarehouseIds) {
            const warehouseBalances = balancesByWarehouse[warehouseId];
            warehouseBalances.sort((a, b) => Number(a.productId) - Number(b.productId));
            
            const warehouseName = findNameByIdFromMap(warehouseMap, warehouseId) || '';
            
            const warehouseHeading = document.createElement('h3');
            warehouseHeading.textContent = `Склад: ${warehouseName}`;
            container.appendChild(warehouseHeading);
            
            const table = document.createElement('table');
            table.className = 'balance-table';
            
            const thead = document.createElement('thead');
            const headerRow = document.createElement('tr');
            
            const th1 = document.createElement('th');
            th1.textContent = 'Товар';
            headerRow.appendChild(th1);
            
            const th2 = document.createElement('th');
            th2.textContent = 'Кількість (кг)';
            headerRow.appendChild(th2);
            
            const th3 = document.createElement('th');
            th3.textContent = 'Середня ціна (EUR/кг)';
            headerRow.appendChild(th3);
            
            const th4 = document.createElement('th');
            th4.textContent = 'Загальна вартість (EUR)';
            headerRow.appendChild(th4);
            
            thead.appendChild(headerRow);
            table.appendChild(thead);
            
            const tbody = document.createElement('tbody');
            let warehouseTotal = 0;
            let warehouseTotalQuantity = 0;
            
            for (const balance of warehouseBalances) {
                const productName = findNameByIdFromMap(productMap, balance.productId) || '';
                const quantity = formatNumber(balance.quantity, 2);
                const avgPrice = formatNumber(balance.averagePriceEur, 6);
                const totalCost = formatNumber(balance.totalCostEur, 6);
                warehouseTotal += parseFloat(balance.totalCostEur);
                warehouseTotalQuantity += parseFloat(balance.quantity);
                
                const row = document.createElement('tr');
                row.className = 'balance-row';
                row.setAttribute('data-warehouse-id', warehouseId);
                row.setAttribute('data-product-id', balance.productId);
                row.setAttribute('data-warehouse-name', warehouseName);
                row.setAttribute('data-product-name', productName);
                row.setAttribute('data-quantity', balance.quantity);
                row.setAttribute('data-total-cost', balance.totalCostEur);
                row.setAttribute('data-average-price', balance.averagePriceEur);
                
                const productCell = document.createElement('td');
                productCell.setAttribute('data-label', 'Товар');
                productCell.textContent = productName;
                row.appendChild(productCell);
                
                const quantityCell = document.createElement('td');
                quantityCell.setAttribute('data-label', 'Кількість (кг)');
                quantityCell.textContent = quantity;
                row.appendChild(quantityCell);
                
                const avgPriceCell = document.createElement('td');
                avgPriceCell.setAttribute('data-label', 'Середня ціна (EUR/кг)');
                avgPriceCell.textContent = avgPrice;
                row.appendChild(avgPriceCell);
                
                const totalCostCell = document.createElement('td');
                totalCostCell.setAttribute('data-label', 'Загальна вартість (EUR)');
                totalCostCell.textContent = totalCost;
                row.appendChild(totalCostCell);
                
                tbody.appendChild(row);
            }
            
            table.appendChild(tbody);
            
            const tfoot = document.createElement('tfoot');
            const footerRow = document.createElement('tr');
            footerRow.className = 'balance-tfoot-row';
            
            const footerCell1 = document.createElement('td');
            footerCell1.setAttribute('data-label', 'Загалом');
            const strong1 = document.createElement('strong');
            strong1.textContent = 'Загалом:';
            footerCell1.appendChild(strong1);
            footerRow.appendChild(footerCell1);
            
            const footerCell2 = document.createElement('td');
            footerCell2.setAttribute('data-label', 'Кількість (кг)');
            const strong2 = document.createElement('strong');
            strong2.textContent = formatNumber(warehouseTotalQuantity, 2);
            footerCell2.appendChild(strong2);
            footerRow.appendChild(footerCell2);
            
            const footerCell3 = document.createElement('td');
            footerCell3.setAttribute('data-label', 'Середня ціна (EUR/кг)');
            const strong3 = document.createElement('strong');
            const averagePrice = warehouseTotalQuantity > 0 ? warehouseTotal / warehouseTotalQuantity : 0;
            strong3.textContent = formatNumber(averagePrice, 6);
            footerCell3.appendChild(strong3);
            footerRow.appendChild(footerCell3);
            
            const footerCell4 = document.createElement('td');
            footerCell4.setAttribute('data-label', 'Загальна вартість (EUR)');
            const strong4 = document.createElement('strong');
            strong4.textContent = `${formatNumber(warehouseTotal, 6)} EUR`;
            footerCell4.appendChild(strong4);
            footerRow.appendChild(footerCell4);
            
            tfoot.appendChild(footerRow);
            table.appendChild(tfoot);
            
            container.appendChild(table);
        }
        
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
    select.textContent = '';
    data.forEach(item => {
        const option = document.createElement('option');
        option.value = String(item.id || item.value);
        option.text = item.name || item.text || item.value || '';
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
    if (!select) return;
    select.textContent = '';
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = 'Оберіть продукт';
    select.appendChild(defaultOption);
    for (const [id, name] of productMap.entries()) {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = name || '';
        select.appendChild(option);
    }
}

function populateWarehouses(selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.textContent = '';
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = 'Оберіть склад';
    select.appendChild(defaultOption);
    for (const [id, name] of warehouseMap.entries()) {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = name || '';
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
        if (!container) return;
        container.textContent = '';
        
        for (const withdrawal of data.content) {
            const productName = findNameByIdFromMap(productMap, withdrawal.productId) || 'Не вказано';
            const warehouseName = findNameByIdFromMap(warehouseMap, withdrawal.warehouseId) || 'Не вказано';
            const reason = withdrawal.withdrawalReason ? withdrawal.withdrawalReason.name : 'Невідома причина';
            const unitPrice = withdrawal.unitPriceEur ? formatNumber(withdrawal.unitPriceEur, 6) + ' EUR' : '-';
            const totalCost = withdrawal.totalCostEur ? formatNumber(withdrawal.totalCostEur, 6) + ' EUR' : '-';
            
            const row = document.createElement('tr');
            row.setAttribute('data-id', withdrawal.id);
            
            const warehouseCell = document.createElement('td');
            warehouseCell.setAttribute('data-label', 'Склад');
            warehouseCell.textContent = warehouseName;
            row.appendChild(warehouseCell);
            
            const productCell = document.createElement('td');
            productCell.setAttribute('data-label', 'Товар');
            productCell.textContent = productName;
            row.appendChild(productCell);
            
            const reasonCell = document.createElement('td');
            reasonCell.setAttribute('data-label', 'Причина');
            reasonCell.textContent = reason;
            row.appendChild(reasonCell);
            
            const quantityCell = document.createElement('td');
            quantityCell.setAttribute('data-label', 'Кількість');
            quantityCell.textContent = `${withdrawal.quantity} кг`;
            row.appendChild(quantityCell);
            
            const unitPriceCell = document.createElement('td');
            unitPriceCell.setAttribute('data-label', 'Ціна за кг');
            unitPriceCell.style.textAlign = 'right';
            unitPriceCell.textContent = unitPrice;
            row.appendChild(unitPriceCell);
            
            const totalCostCell = document.createElement('td');
            totalCostCell.setAttribute('data-label', 'Загальна вартість');
            totalCostCell.style.textAlign = 'right';
            totalCostCell.style.fontWeight = 'bold';
            totalCostCell.textContent = totalCost;
            row.appendChild(totalCostCell);
            
            const withdrawalDateCell = document.createElement('td');
            withdrawalDateCell.setAttribute('data-label', 'Дата списання');
            withdrawalDateCell.textContent = withdrawal.withdrawalDate || '';
            row.appendChild(withdrawalDateCell);
            
            const descriptionCell = document.createElement('td');
            descriptionCell.setAttribute('data-label', 'Опис');
            descriptionCell.textContent = withdrawal.description || '';
            row.appendChild(descriptionCell);
            
            const createdAtCell = document.createElement('td');
            createdAtCell.setAttribute('data-label', 'Створено');
            createdAtCell.textContent = withdrawal.createdAt ? new Date(withdrawal.createdAt).toLocaleString() : '';
            row.appendChild(createdAtCell);
            
            if (row._clickHandler) {
                row.removeEventListener('click', row._clickHandler);
            }
            row._clickHandler = () => openEditModal(row.dataset.id, data.content);
            row.addEventListener('click', row._clickHandler);
            container.appendChild(row);
        }
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
        if (!container) return;
        container.textContent = '';
        
        for (const entry of data.content) {
            const productName = findNameByIdFromMap(productMap, entry.productId) || '';
            const warehouseName = findNameByIdFromMap(warehouseMap, entry.warehouseId) || '';
            const userName = findNameByIdFromMap(userMap, entry.userId) || '';
            const typeName = entry.type ? entry.type.name : 'Невідомий тип';
            const driverBalance = entry.driverBalanceQuantity || 0;
            const receivedQuantity = entry.quantity || 0;
            const difference = receivedQuantity - driverBalance;
            const totalCost = formatNumber(entry.totalCostEur, 6);
            
            const row = document.createElement('tr');
            row.setAttribute('data-id', entry.id);
            
            const warehouseCell = document.createElement('td');
            warehouseCell.setAttribute('data-label', 'Склад');
            warehouseCell.textContent = warehouseName;
            row.appendChild(warehouseCell);
            
            const entryDateCell = document.createElement('td');
            entryDateCell.setAttribute('data-label', 'Дата');
            entryDateCell.textContent = entry.entryDate || '';
            row.appendChild(entryDateCell);
            
            const userCell = document.createElement('td');
            userCell.setAttribute('data-label', 'Водій');
            userCell.textContent = userName;
            row.appendChild(userCell);
            
            const productCell = document.createElement('td');
            productCell.setAttribute('data-label', 'Товар');
            productCell.textContent = productName;
            row.appendChild(productCell);
            
            const typeCell = document.createElement('td');
            typeCell.setAttribute('data-label', 'Тип');
            typeCell.textContent = typeName;
            row.appendChild(typeCell);
            
            const receivedCell = document.createElement('td');
            receivedCell.setAttribute('data-label', 'Привезено');
            receivedCell.textContent = `${receivedQuantity} кг`;
            row.appendChild(receivedCell);
            
            const purchasedCell = document.createElement('td');
            purchasedCell.setAttribute('data-label', 'Закуплено');
            purchasedCell.textContent = `${driverBalance} кг`;
            row.appendChild(purchasedCell);
            
            const differenceCell = document.createElement('td');
            differenceCell.setAttribute('data-label', 'Різниця');
            differenceCell.textContent = `${difference} кг`;
            row.appendChild(differenceCell);
            
            const totalCostCell = document.createElement('td');
            totalCostCell.setAttribute('data-label', 'Вартість');
            totalCostCell.textContent = `${totalCost} EUR`;
            row.appendChild(totalCostCell);
            
            container.appendChild(row);
        }

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
            tagsContainer.textContent = '';
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
            option.textContent = item.name || '';
            fragment.appendChild(option);
        });
        dropdown.textContent = '';
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

    const dropdownClickHandler = (e) => {
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
    };
    dropdown.addEventListener('click', dropdownClickHandler);

    const tagsContainerClickHandler = (e) => {
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
    };
    tagsContainer.addEventListener('click', tagsContainerClickHandler);

    const triggerClickHandler = (e) => {
        e.stopPropagation();
        dropdown.classList.toggle('open');
        if (dropdown.classList.contains('open')) searchInput.focus();
    };
    trigger.addEventListener('click', triggerClickHandler);

    const documentClickHandler = (e) => {
        if (!selectContainer.contains(e.target)) dropdown.classList.remove('open');
    };
    document.addEventListener('click', documentClickHandler);
    selectContainer._documentClickHandler = documentClickHandler;

    const debouncedSearch = debounce(() => {
        sortAndFilterOptions(searchInput.value);
    }, 200);
    searchInput.addEventListener('input', debouncedSearch);
    searchInput._inputHandler = debouncedSearch;

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
                currentSelect.textContent = '';
                selectData.forEach(item => {
                    const option = document.createElement('option');
                    option.value = item.id;
                    option.text = item.name || '';
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
        },
        destroy: function () {
            if (selectContainer._documentClickHandler) {
                document.removeEventListener('click', selectContainer._documentClickHandler);
            }
            if (searchInput._inputHandler) {
                searchInput.removeEventListener('input', searchInput._inputHandler);
            }
            dropdown.removeEventListener('click', dropdownClickHandler);
            tagsContainer.removeEventListener('click', tagsContainerClickHandler);
            trigger.removeEventListener('click', triggerClickHandler);
            if (selectContainer.parentNode) {
                selectContainer.parentNode.removeChild(selectContainer);
            }
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
const vehiclesBtn = document.getElementById('vehicles-btn');
const vehiclesContainer = document.getElementById('vehicles-container');
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
        vehiclesContainer.style.display = 'none';
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

function initializeModalClickHandlers() {
    const modals = [
        withdrawModal,
        editModal,
        moveModal,
        entryModal,
        driverBalancesModal,
        createVehicleModal,
        vehicleDetailsModal,
        addProductToVehicleModal,
        editVehicleItemModal,
        editTransferModal,
        balanceEditModal,
        balanceHistoryModal,
        document.getElementById('history-filter-modal'),
        document.getElementById('entries-filter-modal'),
        document.getElementById('transfers-filter-modal')
    ];

    modals.forEach(modal => {
        if (!modal) return;

        if (modal._modalClickHandler) {
            modal.removeEventListener('click', modal._modalClickHandler);
            modal._modalClickHandler = null;
        }
    });
}

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
    } else if (modalId === 'create-vehicle-modal') {
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
        vehiclesContainer.style.display = 'none';
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
            if (vehiclesContainer) vehiclesContainer.style.display = 'none';
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
            vehiclesContainer.style.display = 'none';
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
        if (!container) return;
        container.textContent = '';
        
        const driverIds = Object.keys(balancesByDriver);
        if (driverIds.length === 0) {
            const emptyMessage = document.createElement('p');
            emptyMessage.textContent = 'Немає активних балансів водіїв';
            container.appendChild(emptyMessage);
            return;
        }
        
        for (const [driverId, driverBalances] of Object.entries(balancesByDriver)) {
            const driverName = findNameByIdFromMap(userMap, driverId) || '';
            
            const driverHeading = document.createElement('h4');
            driverHeading.textContent = `Водій: ${driverName}`;
            container.appendChild(driverHeading);
            
            const table = document.createElement('table');
            table.className = 'balance-table';
            
            const thead = document.createElement('thead');
            const headerRow = document.createElement('tr');
            
            const th1 = document.createElement('th');
            th1.textContent = 'Товар';
            headerRow.appendChild(th1);
            
            const th2 = document.createElement('th');
            th2.textContent = 'Кількість (кг)';
            headerRow.appendChild(th2);
            
            const th3 = document.createElement('th');
            th3.textContent = 'Середня ціна (EUR/кг)';
            headerRow.appendChild(th3);
            
            const th4 = document.createElement('th');
            th4.textContent = 'Загальна вартість (EUR)';
            headerRow.appendChild(th4);
            
            thead.appendChild(headerRow);
            table.appendChild(thead);
            
            const tbody = document.createElement('tbody');
            let driverTotal = 0;
            
            for (const balance of driverBalances) {
                const productName = findNameByIdFromMap(productMap, balance.productId) || '';
                const quantity = formatNumber(balance.quantity, 2);
                const avgPrice = formatNumber(balance.averagePriceEur, 6);
                const totalCost = formatNumber(balance.totalCostEur, 6);
                driverTotal += parseFloat(totalCost);
                
                const row = document.createElement('tr');
                
                const productCell = document.createElement('td');
                productCell.setAttribute('data-label', 'Товар');
                productCell.textContent = productName;
                row.appendChild(productCell);
                
                const quantityCell = document.createElement('td');
                quantityCell.setAttribute('data-label', 'Кількість (кг)');
                quantityCell.textContent = quantity;
                row.appendChild(quantityCell);
                
                const avgPriceCell = document.createElement('td');
                avgPriceCell.setAttribute('data-label', 'Середня ціна (EUR/кг)');
                avgPriceCell.textContent = avgPrice;
                row.appendChild(avgPriceCell);
                
                const totalCostCell = document.createElement('td');
                totalCostCell.setAttribute('data-label', 'Загальна вартість (EUR)');
                totalCostCell.textContent = totalCost;
                row.appendChild(totalCostCell);
                
                tbody.appendChild(row);
            }
            
            table.appendChild(tbody);
            
            const tfoot = document.createElement('tfoot');
            const footerRow = document.createElement('tr');
            footerRow.className = 'balance-tfoot-row';
            
            const footerCell1 = document.createElement('td');
            footerCell1.setAttribute('data-label', 'Загальна вартість товару водія');
            const strong1 = document.createElement('strong');
            strong1.textContent = 'Загальна вартість товару водія:';
            footerCell1.appendChild(strong1);
            footerRow.appendChild(footerCell1);
            
            const footerCell2 = document.createElement('td');
            footerCell2.setAttribute('data-label', '');
            footerRow.appendChild(footerCell2);
            
            const footerCell3 = document.createElement('td');
            footerCell3.setAttribute('data-label', '');
            footerRow.appendChild(footerCell3);
            
            const footerCell4 = document.createElement('td');
            footerCell4.setAttribute('data-label', 'Сума');
            const strong2 = document.createElement('strong');
            strong2.textContent = `${formatNumber(driverTotal, 6)} EUR`;
            footerCell4.appendChild(strong2);
            footerRow.appendChild(footerCell4);
            
            tfoot.appendChild(footerRow);
            table.appendChild(tfoot);
            
            container.appendChild(table);
        }
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
// VEHICLES FUNCTIONALITY
// ========================================

const createVehicleBtn = document.getElementById('create-vehicle-btn');
const createVehicleModal = document.getElementById('create-vehicle-modal');
const vehicleDetailsModal = document.getElementById('vehicle-details-modal');
const createVehicleForm = document.getElementById('create-vehicle-form');
const addProductToVehicleModal = document.getElementById('add-product-to-vehicle-modal');
const addProductToVehicleForm = document.getElementById('add-product-to-vehicle-form');
const updateVehicleForm = document.getElementById('update-vehicle-form');
const detailVehicleDateInput = document.getElementById('detail-vehicle-date');
const detailVehicleVehicleInput = document.getElementById('detail-vehicle-vehicle-number');
const detailVehicleDescriptionInput = document.getElementById('detail-vehicle-description');
const editVehicleBtn = document.getElementById('edit-vehicle-btn');
const saveVehicleBtn = document.getElementById('save-vehicle-btn');
const editVehicleItemModal = document.getElementById('edit-vehicle-item-modal');
const editVehicleItemForm = document.getElementById('edit-vehicle-item-form');
const editVehicleItemQuantityInput = document.getElementById('edit-vehicle-item-quantity');
const editVehicleItemTotalCostInput = document.getElementById('edit-vehicle-item-total-cost');
const editVehicleItemModeRadios = document.querySelectorAll('input[name="edit-vehicle-item-mode"]');

initializeModalClickHandlers();

let currentVehicleId = null;
let vehiclesCache = [];
let currentVehicleDetails = null;
let currentVehicleItems = new Map();
let currentVehicleItemId = null;
let currentTransferItem = null;
let currentBalanceEditData = null;

function populateVehicleForm(vehicle) {
    if (!vehicle) {
        if (detailVehicleDateInput) detailVehicleDateInput.value = '';
        if (detailVehicleVehicleInput) detailVehicleVehicleInput.value = '';
        if (detailVehicleDescriptionInput) detailVehicleDescriptionInput.value = '';
        return;
    }

    if (detailVehicleDateInput) detailVehicleDateInput.value = vehicle.shipmentDate || '';
    if (detailVehicleVehicleInput) detailVehicleVehicleInput.value = vehicle.vehicleNumber || '';
    if (detailVehicleDescriptionInput) detailVehicleDescriptionInput.value = vehicle.description || '';
}

function setVehicleFormEditable(isEditable) {
    const fields = [
        detailVehicleDateInput,
        detailVehicleVehicleInput,
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
        editVehicleBtn.style.display = isEditable ? 'none' : 'block';
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

// Open vehicles container
if (vehiclesBtn) {
    vehiclesBtn.addEventListener('click', async () => {
        if (vehiclesContainer.style.display === 'block') {
            vehiclesContainer.style.display = 'none';
        } else {
            document.getElementById('history-container').style.display = 'none';
            document.getElementById('entries-container').style.display = 'none';
            transfersContainer.style.display = 'none';
            if (discrepanciesContainer) discrepanciesContainer.style.display = 'none';
            
            vehiclesContainer.style.display = 'block';
            await loadVehicles();
        }
    });
}

// Open create vehicle modal
if (createVehicleBtn) {
    createVehicleBtn.addEventListener('click', () => {
        document.getElementById('vehicle-date').valueAsDate = new Date();
        createVehicleModal.style.display = 'flex';
        createVehicleModal.classList.add('open');
        document.body.classList.add('modal-open');
    });
}

// Create vehicle form submit
if (createVehicleForm) {
    createVehicleForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const vehicleData = {
            shipmentDate: document.getElementById('vehicle-date').value,
            vehicleNumber: document.getElementById('vehicle-vehicle-number').value,
            description: document.getElementById('vehicle-description').value,
            isOurVehicle: true
        };
        
        try {
            const response = await fetch('/api/v1/vehicles', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(vehicleData)
            });
            
            if (!response.ok) {
                throw new Error('Failed to create vehicle');
            }
            
            const result = await response.json();
            showMessage('Машину успішно створено', 'success');
            
            closeModal('create-vehicle-modal');
            createVehicleForm.reset();
            
            await loadVehicles();
        } catch (error) {
            showMessage('Помилка при створенні машини', 'error');
        }
    });
}

// Load vehicles list
async function loadVehicles() {
    const dateFrom = document.getElementById('vehicles-date-from')?.value;
    const dateTo = document.getElementById('vehicles-date-to')?.value;
    
    try {
        let url = '/api/v1/vehicles/by-date-range?';
        
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
            throw new Error('Failed to load vehicles');
        }
        
        vehiclesCache = await response.json();
        renderVehicles(vehiclesCache);
    } catch (error) {
        showMessage('Помилка завантаження машин', 'error');
        
        const tbody = document.getElementById('vehicles-tbody');
        if (tbody) {
            tbody.textContent = '';
            const errorRow = document.createElement('tr');
            const errorCell = document.createElement('td');
            errorCell.setAttribute('colspan', '4');
            errorCell.style.textAlign = 'center';
            errorCell.textContent = 'Помилка завантаження даних';
            errorRow.appendChild(errorCell);
            tbody.appendChild(errorRow);
        }
    }
}

function renderVehicles(vehicles) {
    const tbody = document.getElementById('vehicles-tbody');
    
    if (!tbody) {
        return;
    }
    
    tbody.textContent = '';
    
    if (!vehicles || vehicles.length === 0) {
        const emptyRow = document.createElement('tr');
        const emptyCell = document.createElement('td');
        emptyCell.setAttribute('colspan', '4');
        emptyCell.style.textAlign = 'center';
        emptyCell.textContent = 'Немає даних';
        emptyRow.appendChild(emptyCell);
        tbody.appendChild(emptyRow);
        return;
    }
    
    vehicles.forEach(vehicle => {
        const row = document.createElement('tr');
        row.style.cursor = 'pointer';
        if (row._clickHandler) {
            row.removeEventListener('click', row._clickHandler);
        }
        row._clickHandler = () => viewVehicleDetails(vehicle.id);
        row.addEventListener('click', row._clickHandler);
        
        const shipmentDateCell = document.createElement('td');
        shipmentDateCell.setAttribute('data-label', 'Дата відвантаження');
        shipmentDateCell.textContent = vehicle.shipmentDate || '';
        row.appendChild(shipmentDateCell);
        
        const vehicleNumberCell = document.createElement('td');
        vehicleNumberCell.setAttribute('data-label', 'Номер машини');
        vehicleNumberCell.textContent = vehicle.vehicleNumber || '-';
        row.appendChild(vehicleNumberCell);
        
        const totalCostCell = document.createElement('td');
        totalCostCell.setAttribute('data-label', 'Загальна вартість');
        totalCostCell.style.fontWeight = 'bold';
        totalCostCell.style.color = '#FF6F00';
        totalCostCell.textContent = `${formatNumber(vehicle.totalCostEur, 2)} EUR`;
        row.appendChild(totalCostCell);
        
        const descriptionCell = document.createElement('td');
        descriptionCell.setAttribute('data-label', 'Коментар');
        descriptionCell.textContent = vehicle.description || '-';
        row.appendChild(descriptionCell);
        
        tbody.appendChild(row);
    });
}

// View vehicle details
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

// Render vehicle details
function renderVehicleDetails(vehicle) {
    currentVehicleDetails = vehicle;
    currentVehicleItems = new Map();
    populateVehicleForm(vehicle);
    setVehicleFormEditable(false);
    
    const itemsTbody = document.getElementById('vehicle-items-tbody');
    if (!itemsTbody) return;
    
    itemsTbody.textContent = '';
    
    if (!vehicle.items || vehicle.items.length === 0) {
        const emptyRow = document.createElement('tr');
        const emptyCell = document.createElement('td');
        emptyCell.setAttribute('colspan', '6');
        emptyCell.style.textAlign = 'center';
        emptyCell.textContent = 'Товари ще не додані';
        emptyRow.appendChild(emptyCell);
        itemsTbody.appendChild(emptyRow);
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
            row.setAttribute('data-item-id', item.withdrawalId);
            row.style.cursor = 'pointer';
            
            const productCell = document.createElement('td');
            productCell.setAttribute('data-label', 'Товар');
            productCell.textContent = productName;
            row.appendChild(productCell);
            
            const warehouseCell = document.createElement('td');
            warehouseCell.setAttribute('data-label', 'Склад');
            warehouseCell.textContent = warehouseName;
            row.appendChild(warehouseCell);
            
            const quantityCell = document.createElement('td');
            quantityCell.setAttribute('data-label', 'Кількість');
            quantityCell.textContent = `${formatNumber(item.quantity, 2)} кг`;
            row.appendChild(quantityCell);
            
            const unitPriceCell = document.createElement('td');
            unitPriceCell.setAttribute('data-label', 'Ціна за кг');
            unitPriceCell.style.textAlign = 'right';
            unitPriceCell.textContent = `${formatNumber(item.unitPriceEur, 6)} EUR`;
            row.appendChild(unitPriceCell);
            
            const totalCostCell = document.createElement('td');
            totalCostCell.setAttribute('data-label', 'Загальна вартість');
            totalCostCell.style.textAlign = 'right';
            totalCostCell.style.fontWeight = 'bold';
            totalCostCell.textContent = `${formatNumber(item.totalCostEur, 6)} EUR`;
            row.appendChild(totalCostCell);
            
            const withdrawalDateCell = document.createElement('td');
            withdrawalDateCell.setAttribute('data-label', 'Дата списання');
            withdrawalDateCell.textContent = item.withdrawalDate || vehicle.shipmentDate || '';
            row.appendChild(withdrawalDateCell);
            
            itemsTbody.appendChild(row);
        });
    }
    
    document.getElementById('vehicle-total-cost').textContent = formatNumber(vehicle.totalCostEur, 2);
}

// Add product to vehicle button
document.getElementById('add-product-to-vehicle-btn')?.addEventListener('click', () => {
    // Populate warehouses and products
    populateWarehouses('vehicle-warehouse-id');
    populateProducts('vehicle-product-id');
    
    addProductToVehicleModal.style.display = 'flex';
    addProductToVehicleModal.classList.add('open');
    document.body.classList.add('modal-open');
});

// Add product to vehicle form submit
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
            
            // Refresh vehicle details
            renderVehicleDetails(updatedVehicle);
            
            // Refresh vehicles list in table
            await loadVehicles();
            
            // Reload balance
    loadBalance();
        } catch (error) {
            showMessage(error.message || 'Помилка при додаванні товару до машини', 'error');
        }
    });
}

// Delete vehicle button
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
        await loadBalance();
    } catch (error) {
        showMessage('Помилка при видаленні машини', 'error');
    }
});

// Apply vehicles filters
document.getElementById('apply-vehicles-filters')?.addEventListener('click', async () => {
    await loadVehicles();
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
        
        const tbody = document.getElementById('discrepancies-table-body');
        if (!tbody) return;
        tbody.textContent = '';
        
        if (data.content.length === 0) {
            const emptyRow = document.createElement('tr');
            const emptyCell = document.createElement('td');
            emptyCell.setAttribute('colspan', '10');
            emptyCell.style.textAlign = 'center';
            emptyCell.style.padding = '30px';
            emptyCell.style.color = '#999';
            emptyCell.textContent = 'Немає даних';
            emptyRow.appendChild(emptyCell);
            tbody.appendChild(emptyRow);
        } else {
            for (const item of data.content) {
                const row = document.createElement('tr');
                
                const driverName = findNameByIdFromMap(userMap, item.driverId) || '';
                const productName = findNameByIdFromMap(productMap, item.productId) || '';
                const warehouseName = findNameByIdFromMap(warehouseMap, item.warehouseId) || '';
                
                const typeLabel = item.type === 'LOSS' ? 'Втрата' : 'Придбання';
                const typeClass = item.type === 'LOSS' ? 'loss' : 'gain';
                const typeColor = item.type === 'LOSS' ? '#d32f2f' : '#388e3c';
                
                const receiptDateCell = document.createElement('td');
                receiptDateCell.setAttribute('data-label', 'Дата');
                receiptDateCell.textContent = formatDate(item.receiptDate);
                row.appendChild(receiptDateCell);
                
                const driverCell = document.createElement('td');
                driverCell.setAttribute('data-label', 'Водій');
                driverCell.textContent = driverName;
                row.appendChild(driverCell);
                
                const productCell = document.createElement('td');
                productCell.setAttribute('data-label', 'Товар');
                productCell.textContent = productName;
                row.appendChild(productCell);
                
                const warehouseCell = document.createElement('td');
                warehouseCell.setAttribute('data-label', 'Склад');
                warehouseCell.textContent = warehouseName;
                row.appendChild(warehouseCell);
                
                const purchasedCell = document.createElement('td');
                purchasedCell.setAttribute('data-label', 'Закуплено');
                purchasedCell.style.textAlign = 'center';
                purchasedCell.textContent = `${item.purchasedQuantity} кг`;
                row.appendChild(purchasedCell);
                
                const receivedCell = document.createElement('td');
                receivedCell.setAttribute('data-label', 'Прийнято');
                receivedCell.style.textAlign = 'center';
                receivedCell.textContent = `${item.receivedQuantity} кг`;
                row.appendChild(receivedCell);
                
                const discrepancyCell = document.createElement('td');
                discrepancyCell.setAttribute('data-label', 'Різниця');
                discrepancyCell.style.textAlign = 'center';
                discrepancyCell.style.fontWeight = 'bold';
                discrepancyCell.style.color = typeColor;
                discrepancyCell.textContent = `${item.discrepancyQuantity > 0 ? '+' : ''}${item.discrepancyQuantity} кг`;
                row.appendChild(discrepancyCell);
                
                const unitPriceCell = document.createElement('td');
                unitPriceCell.setAttribute('data-label', 'Ціна/кг');
                unitPriceCell.style.textAlign = 'right';
                unitPriceCell.textContent = `${formatNumber(item.unitPriceEur, 6)} EUR`;
                row.appendChild(unitPriceCell);
                
                const valueCell = document.createElement('td');
                valueCell.setAttribute('data-label', 'Вартість');
                valueCell.style.textAlign = 'right';
                valueCell.style.fontWeight = 'bold';
                valueCell.textContent = `${formatNumber(Math.abs(item.discrepancyValueEur), 6)} EUR`;
                row.appendChild(valueCell);
                
                const typeCell = document.createElement('td');
                typeCell.setAttribute('data-label', 'Тип');
                typeCell.style.textAlign = 'center';
                const typeBadge = document.createElement('span');
                typeBadge.className = `discrepancy-type-badge ${typeClass}`;
                typeBadge.textContent = typeLabel;
                typeCell.appendChild(typeBadge);
                row.appendChild(typeCell);
                
                tbody.appendChild(row);
            }
        }
        
        // Update pagination
        updateDiscrepanciesPagination(data);
        
    } catch (error) {
        console.error('Error loading discrepancies:', error);
        const tbody = document.getElementById('discrepancies-table-body');
        if (tbody) {
            tbody.textContent = '';
            const errorRow = document.createElement('tr');
            const errorCell = document.createElement('td');
            errorCell.setAttribute('colspan', '10');
            errorCell.style.textAlign = 'center';
            errorCell.style.padding = '30px';
            errorCell.style.color = '#d32f2f';
            errorCell.textContent = 'Помилка завантаження даних';
            errorRow.appendChild(errorCell);
            tbody.appendChild(errorRow);
        }
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
    
    const pageNumbersContainer = document.getElementById('discrepancies-page-numbers');
    if (!pageNumbersContainer) return;
    pageNumbersContainer.textContent = '';
    
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
        
        if (pageBtn._clickHandler) {
            pageBtn.removeEventListener('click', pageBtn._clickHandler);
        }
        pageBtn._clickHandler = async () => {
            currentDiscrepanciesPage = i;
            await loadDiscrepancies();
        };
        pageBtn.addEventListener('click', pageBtn._clickHandler);
        
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

function renderTransfers(transfers) {
    const tbody = document.getElementById('transfers-body');
    if (!tbody) return;
    
    tbody.textContent = '';
    
    if (!Array.isArray(transfers) || transfers.length === 0) {
        transfersCache = [];
        const emptyRow = document.createElement('tr');
        const emptyCell = document.createElement('td');
        emptyCell.setAttribute('colspan', '10');
        emptyCell.style.textAlign = 'center';
        emptyCell.textContent = 'Немає даних';
        emptyRow.appendChild(emptyCell);
        tbody.appendChild(emptyRow);
        return;
    }
    
    transfersCache = transfers.slice();
    
    transfers.forEach(item => {
        const fromProductName = findNameByIdFromMap(productMap, item.fromProductId) || 'Не вказано';
        const toProductName = findNameByIdFromMap(productMap, item.toProductId) || 'Не вказано';
        const warehouseName = findNameByIdFromMap(warehouseMap, item.warehouseId) || 'Не вказано';
        const userName = findNameByIdFromMap(userMap, item.userId) || 'Не вказано';
        const reasonObj = withdrawalReasonMap.get(Number(item.reasonId));
        const reasonName = reasonObj ? reasonObj.name : 'Не вказано';
        
        const row = document.createElement('tr');
        row.setAttribute('data-id', item.id);
        if (row._clickHandler) {
            row.removeEventListener('click', row._clickHandler);
        }
        row._clickHandler = () => openEditTransferModal(Number(item.id));
        row.addEventListener('click', row._clickHandler);
        
        const transferDateCell = document.createElement('td');
        transferDateCell.setAttribute('data-label', 'Дата');
        transferDateCell.style.textAlign = 'center';
        transferDateCell.textContent = item.transferDate || '';
        row.appendChild(transferDateCell);
        
        const warehouseCell = document.createElement('td');
        warehouseCell.setAttribute('data-label', 'Склад');
        warehouseCell.textContent = warehouseName;
        row.appendChild(warehouseCell);
        
        const fromProductCell = document.createElement('td');
        fromProductCell.setAttribute('data-label', 'З товару');
        fromProductCell.textContent = fromProductName;
        row.appendChild(fromProductCell);
        
        const toProductCell = document.createElement('td');
        toProductCell.setAttribute('data-label', 'До товару');
        toProductCell.textContent = toProductName;
        row.appendChild(toProductCell);
        
        const quantityCell = document.createElement('td');
        quantityCell.setAttribute('data-label', 'Кількість');
        quantityCell.style.textAlign = 'center';
        quantityCell.textContent = `${formatNumber(item.quantity, 2)} кг`;
        row.appendChild(quantityCell);
        
        const unitPriceCell = document.createElement('td');
        unitPriceCell.setAttribute('data-label', 'Ціна за кг');
        unitPriceCell.style.textAlign = 'right';
        unitPriceCell.textContent = `${formatNumber(item.unitPriceEur, 6)} EUR`;
        row.appendChild(unitPriceCell);
        
        const totalCostCell = document.createElement('td');
        totalCostCell.setAttribute('data-label', 'Загальна вартість');
        totalCostCell.style.textAlign = 'right';
        totalCostCell.style.fontWeight = 'bold';
        totalCostCell.textContent = `${formatNumber(item.totalCostEur, 6)} EUR`;
        row.appendChild(totalCostCell);
        
        const userCell = document.createElement('td');
        userCell.setAttribute('data-label', 'Виконавець');
        userCell.textContent = userName;
        row.appendChild(userCell);
        
        const reasonCell = document.createElement('td');
        reasonCell.setAttribute('data-label', 'Причина');
        reasonCell.textContent = reasonName;
        row.appendChild(reasonCell);
        
        const descriptionCell = document.createElement('td');
        descriptionCell.setAttribute('data-label', 'Опис');
        descriptionCell.textContent = item.description || '';
        row.appendChild(descriptionCell);
        
        tbody.appendChild(row);
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
            await loadBalance();
            closeModal('edit-vehicle-item-modal');
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
        if (row._clickHandler) {
            row.removeEventListener('click', row._clickHandler);
        }
        row._clickHandler = () => {
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
        };
        row.addEventListener('click', row._clickHandler);
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
        balanceHistoryBody.textContent = '';
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

    balanceHistoryBody.textContent = '';
    const loadingRow = document.createElement('tr');
    const loadingCell = document.createElement('td');
    loadingCell.setAttribute('colspan', '7');
    loadingCell.style.textAlign = 'center';
    loadingCell.textContent = 'Завантаження...';
    loadingRow.appendChild(loadingCell);
    balanceHistoryBody.appendChild(loadingRow);
    balanceHistoryEmpty.style.display = 'none';

    try {
        const response = await fetch(`/api/v1/warehouse/balances/${warehouseId}/product/${productId}/history`);
        if (!response.ok) {
            throw new Error('Не вдалося завантажити історію змін');
        }
        const history = await response.json();

        balanceHistoryBody.textContent = '';

        if (!Array.isArray(history) || history.length === 0) {
            balanceHistoryEmpty.style.display = 'block';
            return;
        }

        const typeLabels = {
            QUANTITY: 'Кількість',
            TOTAL_COST: 'Загальна вартість',
            BOTH: 'Кількість та вартість'
        };

        history.forEach(item => {
            const userName = findNameByIdFromMap(userMap, item.userId) || '—';
            const typeLabel = typeLabels[item.adjustmentType] || item.adjustmentType || '—';
            const createdAt = item.createdAt ? new Date(item.createdAt).toLocaleString() : '—';
            const quantityChange = `${formatNumber(item.previousQuantity, 2)} → ${formatNumber(item.newQuantity, 2)} кг`;
            const totalChange = `${formatNumber(item.previousTotalCostEur, 6)} → ${formatNumber(item.newTotalCostEur, 6)} EUR`;
            const averageChange = `${formatNumber(item.previousAveragePriceEur, 6)} → ${formatNumber(item.newAveragePriceEur, 6)} EUR/кг`;
            const description = item.description || '—';

            const row = document.createElement('tr');
            
            const createdAtCell = document.createElement('td');
            createdAtCell.setAttribute('data-label', 'Дата');
            createdAtCell.textContent = createdAt;
            row.appendChild(createdAtCell);
            
            const userCell = document.createElement('td');
            userCell.setAttribute('data-label', 'Користувач');
            userCell.textContent = userName;
            row.appendChild(userCell);
            
            const typeCell = document.createElement('td');
            typeCell.setAttribute('data-label', 'Тип');
            typeCell.textContent = typeLabel;
            row.appendChild(typeCell);
            
            const quantityCell = document.createElement('td');
            quantityCell.setAttribute('data-label', 'Кількість');
            quantityCell.textContent = quantityChange;
            row.appendChild(quantityCell);
            
            const totalCell = document.createElement('td');
            totalCell.setAttribute('data-label', 'Загальна вартість');
            totalCell.textContent = totalChange;
            row.appendChild(totalCell);
            
            const averageCell = document.createElement('td');
            averageCell.setAttribute('data-label', 'Середня ціна');
            averageCell.textContent = averageChange;
            row.appendChild(averageCell);
            
            const descriptionCell = document.createElement('td');
            descriptionCell.setAttribute('data-label', 'Коментар');
            descriptionCell.textContent = description;
            row.appendChild(descriptionCell);
            
            balanceHistoryBody.appendChild(row);
        });
    } catch (error) {
        console.error('Error loading balance history:', error);
        balanceHistoryBody.textContent = '';
        const errorRow = document.createElement('tr');
        const errorCell = document.createElement('td');
        errorCell.setAttribute('colspan', '7');
        errorCell.style.textAlign = 'center';
        errorCell.style.color = '#e53935';
        errorCell.textContent = 'Помилка завантаження історії';
        errorRow.appendChild(errorCell);
        balanceHistoryBody.appendChild(errorRow);
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
            vehiclesContainer.style.display = 'none';
            
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
        balanceHistoryBody.textContent = '';
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