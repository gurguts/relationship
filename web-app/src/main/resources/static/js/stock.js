let productMap = new Map();
let warehouseMap = new Map();
let userMap = new Map();
let withdrawalReasonMap = new Map();
let withdrawalReasons = [];
const customSelects = {};
const entriesCustomSelects = {};
const historyCustomSelects = {};
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
        populateSelect('edit-product-id', products);
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
        
        const filterReasons = withdrawalReasons.filter(reason => 
            reason.purpose === 'REMOVING' || reason.purpose === 'BOTH'
        );

        populateSelect('withdrawal-reason-id-filter', filterReasons);
        
        populateSelect('edit-withdrawal-reason-id', filterReasons);
        
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
    return Array.from(withdrawalReasonMap.values()).filter(reason => reason.purpose === purpose);
}

function populateWithdrawalReasonsForWithdrawal() {
    const reasonsForWithdrawal = getWithdrawalReasonsByPurpose('REMOVING');
    populateSelect('withdrawal-reason-id', reasonsForWithdrawal);
}

function populateMoveTypes() {
    const moveTypes = getWithdrawalReasonsByPurpose('BOTH');
    populateSelect('move-type-id', moveTypes);
}

function setDefaultMoveDate() {
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('move-date').value = today;
}

function populateEntryTypes() {
    const entryTypes = getWithdrawalReasonsByPurpose('ADDING');
    populateSelect('entry-type-id', entryTypes);
}

function setDefaultEntryDate() {
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('entry-date').value = today;
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
        const response = await fetch('/api/v1/warehouse/balance');
        if (!response.ok) {
            handleError(new Error('Failed to load balance'));
            return;
        }
        const data = await response.json();
        const container = document.getElementById('balance-container');
        let html = '';
        for (const [warehouseId, products] of Object.entries(data.balanceByWarehouseAndProduct)) {
            const warehouseName = findNameByIdFromMap(warehouseMap, warehouseId);
            html += `<h3>Склад: ${warehouseName}</h3>`;
            for (const [productId, quantity] of Object.entries(products)) {
                const productName = findNameByIdFromMap(productMap, productId);
                html += `<div class="balance-item">${productName}: ${quantity} кг</div>`;
            }
        }
        container.innerHTML = html;
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
            const productName = findNameByIdFromMap(productMap, withdrawal.productId);
            const warehouseName = findNameByIdFromMap(warehouseMap, withdrawal.warehouseId);
            const reason = withdrawal.withdrawalReason ? withdrawal.withdrawalReason.name : 'Невідома причина';
            html += `
                        <tr data-id="${withdrawal.id}">
                            <td>${warehouseName}</td>
                            <td>${productName}</td>
                            <td>${reason}</td>
                            <td>${withdrawal.quantity} кг</td>
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
        const response = await fetch(`/api/v1/warehouse/entries?${queryParams}`, {
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
            const difference = (entry.quantity || 0) - (entry.purchasedQuantity || 0);
            html += `
                <tr data-id="${entry.id}">
                    <td>${warehouseName}</td>
                    <td>${entry.entryDate}</td>
                    <td>${userName}</td>
                    <td>${productName}</td>
                    <td>${typeName}</td>
                    <td>${entry.quantity} кг</td>
                    <td>${entry.purchasedQuantity} кг</td>
                    <td>${difference} кг</td>
                </tr>`;
        }
        container.innerHTML = html;
        
        // Добавляем обработчики клика на строки таблицы надходжень
        const rows = container.querySelectorAll('tr[data-id]');
        rows.forEach(row => {
            row.addEventListener('click', () => {
                const entryId = row.dataset.id;
                const entry = data.content.find(e => e.id == entryId);
                if (entry) {
                    openEditEntryModal(entry);
                }
            });
        });
        
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
            withdrawalDate: document.getElementById('withdrawal-date').value
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
        
        const withdrawal = {
            warehouseId: Number(formData.get('warehouse_id')),
            productId: Number(formData.get('from_product_id')),
            withdrawalReasonId: Number(formData.get('type_id')),
            quantity: Number(formData.get('from_quantity')),
            description: `${formData.get('description') || 'Без опису'}`,
            withdrawalDate: formData.get('move_date')
        };
        
        const entry = {
            warehouseId: Number(formData.get('warehouse_id')),
            productId: Number(formData.get('to_product_id')),
            quantity: Number(formData.get('to_quantity')),
            entryDate: formData.get('move_date'),
            typeId: Number(formData.get('type_id')),
            userId: Number(formData.get('executor_id'))
        };
        
        try {
            const withdrawalResponse = await fetch('/api/v1/warehouse/withdraw', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(withdrawal)
            });
            
            if (!withdrawalResponse.ok) {
                const errorData = await withdrawalResponse.json();
                handleError(new Error(errorData.message || 'Failed to create withdrawal'));
                return;
            }
            
            const entryResponse = await fetch('/api/v1/warehouse/entries', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(entry)
            });
            
            if (!entryResponse.ok) {
                const errorData = await entryResponse.json();
                handleError(new Error(errorData.message || 'Failed to create entry'));
                return;
            }
            
            showMessage('Переміщення успішно виконано', 'info');
            closeModal('move-modal');
            loadBalance();
            if (historyContainer.style.display === 'block') {
                loadWithdrawalHistory(currentPage);
            }
        } catch (error) {
            console.error('Error creating move:', error);
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
            entryDate: formData.get('entry_date'),
            typeId: Number(formData.get('type_id')),
            userId: Number(formData.get('user_id'))
        };
        
        try {
            const response = await fetch('/api/v1/warehouse/entries', {
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
    populateProducts('edit-product-id');
    document.getElementById('edit-id').value = withdrawal.id;
    document.getElementById('edit-withdrawal-date').value = withdrawal.withdrawalDate;
    document.getElementById('edit-withdrawal-reason-id').value = withdrawal.withdrawalReason?.id || '';
    document.getElementById('edit-product-id').value = withdrawal.productId;
    document.getElementById('edit-quantity').value = withdrawal.quantity;
    document.getElementById('edit-description').value = withdrawal.description || '';
    const editModal = document.getElementById('edit-modal');
    editModal.style.display = 'flex';
    editModal.classList.add('open');
}

document.getElementById('edit-form').addEventListener('submit',
    async (e) => {
        e.preventDefault();
        const id = Number(document.getElementById('edit-id').value);
        const withdrawal = {
            productId: Number(document.getElementById('edit-product-id').value),
            withdrawalReasonId: Number(document.getElementById('edit-withdrawal-reason-id').value),
            quantity: Number(document.getElementById('edit-quantity').value),
            description: document.getElementById('edit-description').value,
            withdrawalDate: document.getElementById('edit-withdrawal-date').value
        };
        try {
            const response = await fetch(`/api/v1/warehouse/withdraw/${id}`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(withdrawal)
            });
            if (!response.ok) {
                const errorData = await response.json();
                handleError(new Error(errorData.message || 'Failed to update withdrawal'));
                return;
            }
            showMessage('Списання успішно оновлено', 'info');
            closeModal('edit-modal');
            loadBalance();
            loadWithdrawalHistory(currentPage);
        } catch (error) {
            console.error('Error updating withdrawal:', error);
            handleError(error);
        }
    });

document.getElementById('delete-btn').addEventListener('click', async () => {
    if (!confirm('Ви впевнені, що хочете видалити це списання?')) return;
    const id = Number(document.getElementById('edit-id').value);
    try {
        const response = await fetch(`/api/v1/warehouse/withdraw/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            const errorData = await response.json();
            handleError(new Error(errorData.message || 'Failed to delete withdrawal'));
            return;
        }
        showMessage('Списання успішно видалено', 'info');
        closeModal('edit-modal');
        loadBalance();
        loadWithdrawalHistory(currentPage);
    } catch (error) {
        console.error('Error deleting withdrawal:', error);
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
const editEntryModal = document.getElementById('edit-entry-modal');
const closeBtns = document.getElementsByClassName('close');

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
    setDefaultMoveDate();
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
    setDefaultEntryDate();
    entryModal.style.display = 'flex';
    entryModal.classList.add('open');
    document.body.classList.add('modal-open');
});

Array.from(closeBtns).forEach(btn => {
    btn.addEventListener('click', () => {
        closeModal(btn.closest('.modal').id);
    });
});

window.addEventListener('click', (e) => {
    if (e.target === withdrawModal || e.target === editModal || e.target === moveModal || e.target === entryModal || e.target === editEntryModal) {
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
    } else if (modalId === 'move-modal') {
        document.getElementById('move-form').reset();
    } else if (modalId === 'entry-modal') {
        document.getElementById('entry-form').reset();
    } else if (modalId === 'edit-entry-modal') {
        document.getElementById('edit-entry-form').reset();
    }
}

// Функции для редактирования надходжень
function openEditEntryModal(entry) {
    if (entry.id == null) {
        handleError(new Error('Дані надходження не були вказані'));
        return;
    }
    document.getElementById('edit-entry-id').value = entry.id;
    document.getElementById('edit-entry-quantity').value = entry.quantity;
    editEntryModal.style.display = 'flex';
    editEntryModal.classList.add('open');
    document.body.classList.add('modal-open');
}

// Обработчик формы редактирования надходжень
document.getElementById('edit-entry-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('edit-entry-id').value;
    const newQuantity = document.getElementById('edit-entry-quantity').value;

    try {
        // Находим WithdrawalReason с purpose = ADDING
        const addingReason = withdrawalReasons.find(reason => reason.purpose === 'ADDING');
        if (!addingReason) {
            handleError(new Error('Не знайдена причина з типом ADDING'));
            return;
        }

        const response = await fetch(`/api/v1/warehouse/entries/${id}`, {
            method: 'PATCH',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({quantity: Number(newQuantity), typeId: addingReason.id})
        });

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new Error(errorData.message || 'Failed to update entry'));
            return;
        }

        showMessage('Надходження успішно оновлено', 'info');
        closeModal('edit-entry-modal');
        loadBalance();
        if (entriesContainer.style.display === 'block') {
            loadWarehouseEntries(0);
        }
    } catch (error) {
        console.error('Error updating entry:', error);
        handleError(error);
    }
});

const historyBtn = document.getElementById('history-btn');
historyBtn.addEventListener('click', () => {
    if (historyContainer.style.display === 'block') {
        historyContainer.style.display = 'none';
        document.getElementById('open-history-filter-modal').style.display = 'none';
        document.getElementById('history-filter-counter').style.display = 'none';
        document.getElementById('export-excel-history').style.display = 'none';
    } else {
        historyContainer.style.display = 'block';
        document.getElementById('open-history-filter-modal').style.display = 'inline-block';
        document.getElementById('export-excel-history').style.display = 'inline-block';
        initializeHistoryFilters();
        loadWithdrawalHistory(0);
    }
});

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
        console.log('Apply entries filters button clicked');
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