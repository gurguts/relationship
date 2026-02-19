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

const escapeHtml = StockUtils.escapeHtml;
const formatNumber = StockUtils.formatNumber;
const findNameByIdFromMap = StockUtils.findNameByIdFromMap;

async function fetchProducts() {
    try {
        const products = await StockDataLoader.fetchProducts();
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
        const warehouses = await StockDataLoader.fetchWarehouses();
        warehouseMap = new Map(warehouses.map(warehouse => [warehouse.id, warehouse.name]));
        populateSelect('warehouse-id-filter', warehouses);
        populateSelect('warehouse-id', warehouses);
    } catch (error) {
        console.error('Error fetching warehouses:', error);
        handleError(error);
    }
}

async function fetchUsers() {
    try {
        const users = await StockDataLoader.fetchUsers();
        userMap = new Map(users.map(user => [user.id, user.name]));
        populateSelect('move-executor-id', users);
    } catch (error) {
        console.error('Error fetching users:', error);
        handleError(error);
    }
}

async function fetchWithdrawalReasons() {
    try {
        withdrawalReasons = await StockDataLoader.fetchWithdrawalReasons();
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

const setDefaultHistoryDates = StockFilters.setDefaultHistoryDates;
const setDefaultEntriesDates = StockFilters.setDefaultEntriesDates;
const setDefaultTransfersDates = StockFilters.setDefaultTransfersDates;

function updateHistorySelectedFilters() {
    Object.keys(historyFilters).forEach(key => delete historyFilters[key]);
    Object.assign(historyFilters, StockFilters.buildHistoryFilters(historyCustomSelects));
    updateHistoryFilterCounter();
}

function updateHistoryFilterCounter() {
    StockFilters.updateFilterCounter(historyFilters, 'history-filter-counter', 'history-filter-count');
    const filterButton = document.getElementById('open-history-filter-modal');
    const exportButton = document.getElementById('export-excel-history');
    if (historyContainer && historyContainer.style.display === 'block') {
        if (filterButton) filterButton.style.display = 'inline-block';
        if (exportButton) exportButton.style.display = 'inline-block';
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
    Object.assign(entriesFilters, StockFilters.buildEntriesFilters(entriesCustomSelects));
    updateEntriesFilterCounter();
}

function updateEntriesFilterCounter() {
    StockFilters.updateFilterCounter(entriesFilters, 'entries-filter-counter', 'entries-filter-count');
    const filterButton = document.getElementById('open-entries-filter-modal');
    const exportButton = document.getElementById('export-excel-entries');
    if (entriesContainer && entriesContainer.style.display === 'block') {
        if (filterButton) filterButton.style.display = 'inline-block';
        if (exportButton) exportButton.style.display = 'inline-block';
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
        const balances = await StockDataLoader.loadBalance();
        StockRenderer.renderBalance(balances, productMap, warehouseMap, (data) => {
            openBalanceEditModal(data);
        });
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
    try {
        const activeFilters = Object.keys(historyFilters).length > 0 ? historyFilters : filters;
        const normalizedFilters = StockFilters.normalizeFilters(activeFilters);
        
        const data = await StockDataLoader.loadWithdrawalHistory(page, pageSize, normalizedFilters);
        StockRenderer.renderWithdrawalHistory(data.content, productMap, warehouseMap, (id, withdrawals) => {
            openEditModal(id, withdrawals);
        });
        StockRenderer.updatePagination(data.totalPages, page, 'page-info', 'prev-page', 'next-page');
    } catch (error) {
        console.error('Error loading withdrawals:', error);
        handleError(error);
    }
}

async function loadWarehouseEntries(page) {
    try {
        const activeFilters = Object.keys(entriesFilters).length > 0 ? entriesFilters : filters;
        const normalizedFilters = StockFilters.normalizeFilters(activeFilters);
        
        const data = await StockDataLoader.loadWarehouseEntries(page, pageSize, normalizedFilters);
        StockRenderer.renderWarehouseEntries(data.content, productMap, warehouseMap, userMap);
        StockRenderer.updatePagination(data.totalPages, page, 'entries-page-info', 'entries-prev-page', 'entries-next-page');
    } catch (error) {
        console.error('Error loading entries:', error);
        handleError(error);
    }
}

function updateEntriesPagination(total, page) {
    entriesTotalPages = total;
    entriesCurrentPage = page;
    StockRenderer.updatePagination(total, page, 'entries-page-info', 'entries-prev-page', 'entries-next-page');
}

function updatePagination(total, page) {
    totalPages = total;
    currentPage = page;
    StockRenderer.updatePagination(total, page, 'page-info', 'prev-page', 'next-page');
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
            await StockDataLoader.createWithdrawal(withdrawal);
            if (typeof showMessage === 'function') {
                showMessage('Списання успішно створено', 'info');
            }
            closeModal('withdraw-modal');
            await loadBalance();
            if (historyContainer && historyContainer.style.display === 'block') {
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
            await StockDataLoader.createTransfer(transfer);
            if (typeof showMessage === 'function') {
                showMessage('Товар успішно переміщено', 'success');
            }
            closeModal('move-modal');
            await loadBalance();
            if (historyContainer && historyContainer.style.display === 'block') {
                loadWithdrawalHistory(currentPage);
            }
        } catch (error) {
            console.error('Error creating transfer:', error);
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
            await StockDataLoader.createEntry(entry);
            if (typeof showMessage === 'function') {
                showMessage('Надходження успішно створено', 'info');
            }
            closeModal('entry-modal');
            await loadBalance();
            if (entriesContainer && entriesContainer.style.display === 'block') {
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

    StockModal.openModal('edit-modal');
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

        const withdrawalUpdate = {
            withdrawalReasonId: reasonId,
            quantity: roundedQuantity,
            description: descriptionValue,
            withdrawalDate: withdrawalDateValue
        };

        const performUpdate = async () => {
            try {
                await StockDataLoader.updateWithdrawal(id, withdrawalUpdate);
                showMessage('Списання успішно оновлено', 'info');
                closeModal('edit-modal');
                await loadBalance();
                await loadWithdrawalHistory(currentPage);
            } catch (error) {
                console.error('Error updating withdrawal:', error);
                handleError(error);
            }
        };

        if (roundedQuantity === 0) {
            const productLabel = findNameByIdFromMap(productMap, currentWithdrawalItem.productId) || 'товар';
            const message = `Ви впевнені, що хочете повністю видалити списання для ${productLabel}?`;
            ConfirmationModal.show(
                message,
                CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
                performUpdate,
                () => {}
            );
            return;
        }

        await performUpdate();
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
    StockFilters.updateFilterCounter(filters, 'filter-counter', 'filter-count');
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

let currentDiscrepanciesPage = 0;
let discrepanciesPageSize = 20;
let discrepanciesFilters = {};

let currentTransfersPage = 0;
let transfersPageSize = 20;
let transfersFilters = {};

withdrawBtn.addEventListener('click', () => {
    populateProducts('product-id');
    populateWarehouses('warehouse-id');
    populateWithdrawalReasonsForWithdrawal();
    StockModal.openModal('withdraw-modal');
});

moveBtn.addEventListener('click', () => {
    populateProducts('move-from-product-id');
    populateProducts('move-to-product-id');
    populateWarehouses('move-warehouse-id');
    populateSelect('move-executor-id', Array.from(userMap.entries()).map(([id, name]) => ({id, name})));
    populateMoveTypes();
    StockModal.openModal('move-modal');
});

entriesBtn.addEventListener('click', async () => {
    if (entriesContainer.style.display === 'block') {
        entriesContainer.style.display = 'none';
        document.getElementById('open-entries-filter-modal').style.display = 'none';
        document.getElementById('entries-filter-counter').style.display = 'none';
        document.getElementById('export-excel-entries').style.display = 'none';
    } else {
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
    StockModal.hideDriverBalanceInfo();
    StockModal.openModal('entry-modal');
});

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
    const onClose = () => {
        if (modalId === 'withdraw-modal') {
            document.getElementById('withdraw-form')?.reset();
        } else if (modalId === 'edit-modal') {
            document.getElementById('edit-form')?.reset();
            currentWithdrawalItem = null;
        } else if (modalId === 'move-modal') {
            document.getElementById('move-form')?.reset();
        } else if (modalId === 'entry-modal') {
            document.getElementById('entry-form')?.reset();
            StockModal.hideDriverBalanceInfo();
        } else if (modalId === 'create-vehicle-modal') {
            document.getElementById('create-vehicle-form')?.reset();
        } else if (modalId === 'add-product-to-vehicle-modal') {
            document.getElementById('add-product-to-vehicle-form')?.reset();
            StockModal.hideWarehouseBalanceInfo();
        } else if (modalId === 'vehicle-details-modal') {
            StockModal.resetVehicleFormState(currentVehicleDetails);
        } else if (modalId === 'edit-vehicle-item-modal') {
            const editVehicleItemForm = document.getElementById('edit-vehicle-item-form');
            if (editVehicleItemForm) {
                editVehicleItemForm.reset();
            }
            currentVehicleItemId = null;
        } else if (modalId === 'edit-transfer-modal') {
            const editTransferForm = document.getElementById('edit-transfer-form');
            if (editTransferForm) {
                editTransferForm.reset();
            }
            currentTransferItem = null;
        } else if (modalId === 'balance-edit-modal') {
            StockModal.resetBalanceEditModal();
            currentBalanceEditData = null;
        } else if (modalId === 'balance-history-modal') {
            StockModal.resetBalanceHistoryModal();
        }
    };
    
    StockModal.closeModal(modalId, onClose);
}

const historyBtn = document.getElementById('history-btn');
historyBtn.addEventListener('click', () => {
    if (historyContainer.style.display === 'block') {
        historyContainer.style.display = 'none';
        document.getElementById('open-history-filter-modal').style.display = 'none';
        document.getElementById('history-filter-counter').style.display = 'none';
        document.getElementById('export-excel-history').style.display = 'none';
    } else {
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
    StockModal.openModal('driver-balances-modal');
    });
    
    discrepanciesBtn.addEventListener('click', async () => {
        if (discrepanciesContainer.style.display === 'none' || discrepanciesContainer.style.display === '') {
            
            currentDiscrepanciesPage = 0;
            discrepanciesFilters = {};
            await loadDiscrepancies();
            await loadDiscrepanciesStatistics();
            discrepanciesContainer.style.display = 'block';
            
            if (historyContainer) historyContainer.style.display = 'none';
            if (entriesContainer) entriesContainer.style.display = 'none';
            if (transfersContainer) transfersContainer.style.display = 'none';
            if (vehiclesContainer) vehiclesContainer.style.display = 'none';
        } else {
            discrepanciesContainer.style.display = 'none';
        }
    });
}

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
            StockFilters.updateTransfersFilterCounter(transfersFilters);
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

async function loadDriverBalanceForEntry() {
    const driverId = document.getElementById('entry-user-id')?.value;
    const productId = document.getElementById('entry-product-id')?.value;
    
    if (!driverId || !productId) {
        StockModal.hideDriverBalanceInfo();
        return;
    }
    
    try {
        const balance = await StockDataLoader.loadDriverBalance(driverId, productId);
        StockModal.showDriverBalanceInfo(balance);
    } catch (error) {
        console.error('Error loading driver balance:', error);
        StockModal.hideDriverBalanceInfo();
    }
}

async function loadWarehouseBalanceForVehicle() {
    const warehouseId = document.getElementById('vehicle-warehouse-id')?.value;
    const productId = document.getElementById('vehicle-product-id')?.value;
    
    if (!warehouseId || !productId) {
        StockModal.hideWarehouseBalanceInfo();
        return;
    }
    
    try {
        const balance = await StockDataLoader.loadWarehouseBalance(warehouseId, productId);
        StockModal.showWarehouseBalanceInfo(balance);
    } catch (error) {
        console.error('Error loading warehouse balance:', error);
        StockModal.hideWarehouseBalanceInfo();
    }
}

async function loadDriverBalances() {
    try {
        const balances = await StockDataLoader.loadDriverBalances();
        StockRenderer.renderDriverBalances(balances, productMap, userMap);
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
let currentVehiclesPage = 0;
const vehiclesPageSize = 50;
let currentVehicleDetails = null;
let currentVehicleItems = new Map();
let currentVehicleItemId = null;
let currentTransferItem = null;
let currentBalanceEditData = null;

const resetVehicleFormState = () => StockModal.resetVehicleFormState(currentVehicleDetails);

if (updateVehicleForm) {
    StockModal.setVehicleFormEditable(false);
}

if (editVehicleBtn) {
    editVehicleBtn.addEventListener('click', () => {
        if (!currentVehicleDetails) {
            return;
        }
        StockModal.populateVehicleForm(currentVehicleDetails);
        StockModal.setVehicleFormEditable(true);
        const detailVehicleDateInput = document.getElementById('detail-vehicle-date');
        if (detailVehicleDateInput) {
            detailVehicleDateInput.focus();
        }
    });
}

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
            await loadVehicles(0);
        }
    });
}

if (createVehicleBtn) {
    createVehicleBtn.addEventListener('click', () => {
        document.getElementById('vehicle-date').valueAsDate = new Date();
        const managerSelect = document.getElementById('vehicle-manager-id');
        if (managerSelect) {
            managerSelect.textContent = '';
            const emptyOpt = document.createElement('option');
            emptyOpt.value = '';
            emptyOpt.textContent = 'Не обрано';
            managerSelect.appendChild(emptyOpt);
            for (const [id, name] of userMap.entries()) {
                const opt = document.createElement('option');
                opt.value = id;
                opt.textContent = name || '';
                managerSelect.appendChild(opt);
            }
        }
        StockModal.openModal('create-vehicle-modal');
    });
}

if (createVehicleForm) {
    createVehicleForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const managerIdEl = document.getElementById('vehicle-manager-id');
        const vehicleData = {
            shipmentDate: document.getElementById('vehicle-date').value,
            vehicleNumber: document.getElementById('vehicle-vehicle-number').value,
            description: document.getElementById('vehicle-description').value,
            managerId: managerIdEl && managerIdEl.value ? Number(managerIdEl.value) : null
        };
        
        try {
            await StockDataLoader.createVehicle(vehicleData);
            if (typeof showMessage === 'function') {
                showMessage('Машину успішно створено', 'success');
            }
            closeModal('create-vehicle-modal');
            createVehicleForm.reset();
            await loadVehicles(0);
        } catch (error) {
            console.error('Error creating vehicle:', error);
            if (typeof showMessage === 'function') {
                showMessage('Помилка при створенні машини', 'error');
            }
            handleError(error);
        }
    });
}

let vehiclesManagerCustomSelect = null;

function getSelectedManagerIds() {
    if (vehiclesManagerCustomSelect) {
        return vehiclesManagerCustomSelect.getValue().map(v => Number(v)).filter(n => Number.isFinite(n));
    }
    const select = document.getElementById('vehicles-manager-filter');
    if (!select) return [];
    return Array.from(select.selectedOptions).map(opt => Number(opt.value)).filter(n => Number.isFinite(n));
}

function formatVehiclesStatsQuantity(value) {
    if (value == null || value === '') return '0';
    const n = Number(value);
    if (!Number.isFinite(n)) return '0';
    return n % 1 === 0 ? String(n) : n.toFixed(2);
}

function formatVehiclesStatsCost(value) {
    if (value == null || value === '') return '0';
    const n = Number(value);
    if (!Number.isFinite(n)) return '0';
    return n.toFixed(2);
}

function updateVehiclesStatsDisplay(stats) {
    const countEl = document.getElementById('vehicles-stats-count');
    const quantityEl = document.getElementById('vehicles-stats-quantity');
    const costEl = document.getElementById('vehicles-stats-cost');
    if (countEl) countEl.textContent = stats && typeof stats.vehicleCount === 'number' ? String(stats.vehicleCount) : '0';
    if (quantityEl) quantityEl.textContent = stats && stats.totalQuantityKg != null ? formatVehiclesStatsQuantity(stats.totalQuantityKg) : '0';
    if (costEl) costEl.textContent = stats && stats.totalCostEur != null ? formatVehiclesStatsCost(stats.totalCostEur) : '0';
}

async function loadVehicles(page) {
    try {
        const dateFrom = document.getElementById('vehicles-date-from')?.value || '';
        const dateTo = document.getElementById('vehicles-date-to')?.value || '';
        const searchQuery = document.getElementById('vehicles-search-input')?.value || '';
        const managerIds = getSelectedManagerIds();
        const data = await StockDataLoader.loadVehicles(page, vehiclesPageSize, dateFrom, dateTo, searchQuery, managerIds);
        vehiclesCache = data.content || [];
        currentVehiclesPage = data.page ?? 0;
        StockRenderer.renderVehicles(vehiclesCache, userMap, (vehicleId) => {
            viewVehicleDetails(vehicleId);
        });
        StockRenderer.updateVehiclesPagination(data);
        try {
            const stats = await StockDataLoader.loadVehiclesStats(dateFrom, dateTo, searchQuery, managerIds);
            updateVehiclesStatsDisplay(stats);
        } catch (statsErr) {
            console.error('Error loading vehicles stats:', statsErr);
            updateVehiclesStatsDisplay(null);
        }
    } catch (error) {
        console.error('Error loading vehicles:', error);
        if (typeof showMessage === 'function') {
            showMessage('Помилка завантаження машин', 'error');
        }
        handleError(error);
    }
}

async function viewVehicleDetails(vehicleId) {
    currentVehicleId = vehicleId;
    
    try {
        const vehicle = await StockDataLoader.loadVehicleDetails(vehicleId);
        currentVehicleDetails = vehicle;
        currentVehicleItems = new Map();
        
        StockModal.populateVehicleForm(vehicle);
        const detailManagerSelect = document.getElementById('detail-vehicle-manager-id');
        if (detailManagerSelect) {
            detailManagerSelect.textContent = '';
            const emptyOpt = document.createElement('option');
            emptyOpt.value = '';
            emptyOpt.textContent = 'Не обрано';
            detailManagerSelect.appendChild(emptyOpt);
            for (const [id, name] of userMap.entries()) {
                const opt = document.createElement('option');
                opt.value = id;
                opt.textContent = name || '';
                detailManagerSelect.appendChild(opt);
            }
            detailManagerSelect.value = vehicle.managerId != null ? String(vehicle.managerId) : '';
        }
        StockModal.setVehicleFormEditable(false);
        
        StockRenderer.renderVehicleDetails(vehicle, productMap, warehouseMap);
        
        const vehicleDetailsModal = document.getElementById('vehicle-details-modal');
        if (vehicleDetailsModal) {
            StockModal.openModal('vehicle-details-modal');
        }
    } catch (error) {
        console.error('Error loading vehicle details:', error);
        if (typeof showMessage === 'function') {
            showMessage('Помилка завантаження деталей машини', 'error');
        }
        handleError(error);
    }
}

document.getElementById('add-product-to-vehicle-btn')?.addEventListener('click', () => {
    
    populateWarehouses('vehicle-warehouse-id');
    populateProducts('vehicle-product-id');
    StockModal.hideWarehouseBalanceInfo();
    StockModal.openModal('add-product-to-vehicle-modal');
});

if (addProductToVehicleForm) {
    addProductToVehicleForm.addEventListener('change', (e) => {
        if (e.target.id === 'vehicle-warehouse-id' || e.target.id === 'vehicle-product-id') {
            loadWarehouseBalanceForVehicle();
        }
    });
}

if (addProductToVehicleForm) {
    addProductToVehicleForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const data = {
            warehouseId: Number(document.getElementById('vehicle-warehouse-id').value),
            productId: Number(document.getElementById('vehicle-product-id').value),
            quantity: Number(document.getElementById('vehicle-quantity').value)
        };
        
        try {
            const updatedVehicle = await StockDataLoader.addProductToVehicle(currentVehicleId, data);
            if (typeof showMessage === 'function') {
                showMessage('Товар успішно додано до машини', 'success');
            }
            closeModal('add-product-to-vehicle-modal');
            addProductToVehicleForm.reset();
            currentVehicleDetails = updatedVehicle;
            currentVehicleItems = new Map();
            StockModal.populateVehicleForm(updatedVehicle);
            StockModal.setVehicleFormEditable(false);
            StockRenderer.renderVehicleDetails(updatedVehicle, productMap, warehouseMap);
            await loadVehicles(currentVehiclesPage);
            await loadBalance();
        } catch (error) {
            console.error('Error adding product to vehicle:', error);
            if (typeof showMessage === 'function') {
                showMessage(error.message || 'Помилка при додаванні товару до машини', 'error');
            }
            handleError(error);
        }
    });
}

document.getElementById('delete-vehicle-btn')?.addEventListener('click', () => {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_VEHICLE,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await StockDataLoader.deleteVehicle(currentVehicleId);
                showMessage('Машину успішно видалено', 'success');
                closeModal('vehicle-details-modal');
                await loadVehicles(currentVehiclesPage);
                await loadBalance();
            } catch (error) {
                console.error('Error deleting vehicle:', error);
                showMessage('Помилка при видаленні машини', 'error');
                handleError(error);
            }
        },
        () => {}
    );
});

function populateVehiclesManagerFilter() {
    const select = document.getElementById('vehicles-manager-filter');
    if (!select || !userMap) return;
    const previousIds = getSelectedManagerIds();
    const managerData = Array.from(userMap.entries()).map(([id, name]) => ({ id, name: name || '' }));
    if (managerData.length === 0) return;
    select.textContent = '';
    managerData.forEach(item => {
        const opt = document.createElement('option');
        opt.value = String(item.id);
        opt.textContent = item.name;
        select.appendChild(opt);
    });
    if (!vehiclesManagerCustomSelect) {
        vehiclesManagerCustomSelect = createCustomSelect(select);
        setupVehiclesFilterModalExpandOnDropdown();
    }
    if (vehiclesManagerCustomSelect) {
        vehiclesManagerCustomSelect.populate(managerData);
        if (previousIds.length > 0) {
            vehiclesManagerCustomSelect.setValue(previousIds.map(String));
        }
    }
}

function setupVehiclesFilterModalExpandOnDropdown() {
    const modal = document.getElementById('vehicles-filter-modal');
    const dropdown = modal?.querySelector('.custom-select-dropdown');
    const modalContent = modal?.querySelector('.modal-content');
    if (!dropdown || !modalContent) return;
    const observer = new MutationObserver(() => {
        if (dropdown.classList.contains('open')) {
            modalContent.classList.add('vehicles-filter-modal-expanded');
        } else {
            modalContent.classList.remove('vehicles-filter-modal-expanded');
        }
    });
    observer.observe(dropdown, { attributes: true, attributeFilter: ['class'] });
}

document.getElementById('vehicles-open-filters-btn')?.addEventListener('click', () => {
    populateVehiclesManagerFilter();
    StockModal.openModal('vehicles-filter-modal');
});
document.querySelector('.vehicles-filter-modal-close')?.addEventListener('click', () => {
    StockModal.closeModal('vehicles-filter-modal');
});
document.getElementById('vehicles-apply-filters-btn')?.addEventListener('click', async () => {
    StockModal.closeModal('vehicles-filter-modal');
    await loadVehicles(0);
});
document.getElementById('vehicles-clear-filters-btn')?.addEventListener('click', () => {
    const fromInput = document.getElementById('vehicles-date-from');
    const toInput = document.getElementById('vehicles-date-to');
    if (fromInput) fromInput.value = '';
    if (toInput) toInput.value = '';
    if (vehiclesManagerCustomSelect) vehiclesManagerCustomSelect.reset();
});
StockModal.setupModalClickHandlers('vehicles-filter-modal');

const vehiclesSearchInput = document.getElementById('vehicles-search-input');
if (vehiclesSearchInput) {
    let vehiclesSearchTimeout = null;
    vehiclesSearchInput.addEventListener('input', () => {
        if (vehiclesSearchTimeout) clearTimeout(vehiclesSearchTimeout);
        vehiclesSearchTimeout = setTimeout(() => loadVehicles(0), 300);
    });
}

document.getElementById('export-excel-vehicles-products')?.addEventListener('click', async () => {
    try {
        const dateFrom = document.getElementById('vehicles-date-from')?.value || '';
        const dateTo = document.getElementById('vehicles-date-to')?.value || '';
        const searchQuery = document.getElementById('vehicles-search-input')?.value || '';
        const managerIds = getSelectedManagerIds();
        const blob = await StockDataLoader.exportVehicleProductsToExcel(dateFrom, dateTo, searchQuery, managerIds);
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = 'vehicle_products.xlsx';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(downloadUrl);
        document.body.removeChild(a);
        if (typeof showMessage === 'function') {
            showMessage('Excel файл успішно завантажено', 'success');
        }
    } catch (error) {
        console.error('Error exporting vehicle products to Excel:', error);
        if (typeof showMessage === 'function') {
            showMessage('Помилка при експорті в Excel', 'error');
        }
        handleError(error);
    }
});

document.getElementById('vehicles-prev-page')?.addEventListener('click', async () => {
    if (currentVehiclesPage > 0) {
        await loadVehicles(currentVehiclesPage - 1);
    }
});

document.getElementById('vehicles-next-page')?.addEventListener('click', async () => {
    await loadVehicles(currentVehiclesPage + 1);
});

const exportTableToExcel = StockRenderer.exportTableToExcel;

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
        const stats = await StockDataLoader.loadDiscrepanciesStatistics(discrepanciesFilters);
        StockRenderer.renderDiscrepanciesStatistics(stats);
    } catch (error) {
        console.error('Error loading discrepancies statistics:', error);
    }
}

async function loadDiscrepancies() {
    try {
        const data = await StockDataLoader.loadDiscrepancies(currentDiscrepanciesPage, discrepanciesPageSize, discrepanciesFilters);
        StockRenderer.renderDiscrepancies(data.content, productMap, warehouseMap, userMap);
        StockRenderer.updateDiscrepanciesPagination(data);
        
        const pageNumbersContainer = document.getElementById('discrepancies-page-numbers');
        if (pageNumbersContainer) {
            const existingButtons = pageNumbersContainer.querySelectorAll('button');
            existingButtons.forEach(btn => {
                if (btn._clickHandler) {
                    btn.removeEventListener('click', btn._clickHandler);
                }
            });
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
                
                pageBtn._clickHandler = async () => {
                    currentDiscrepanciesPage = i;
                    await loadDiscrepancies();
                };
                pageBtn.addEventListener('click', pageBtn._clickHandler);
                
                pageNumbersContainer.appendChild(pageBtn);
            }
        }
    } catch (error) {
        console.error('Error loading discrepancies:', error);
        handleError(error);
    }
}


const formatDate = StockUtils.formatDate;

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

document.getElementById('apply-discrepancy-filters').addEventListener('click', async () => {
    discrepanciesFilters = StockFilters.buildDiscrepanciesFilters();
    currentDiscrepanciesPage = 0;
    await loadDiscrepancies();
    await loadDiscrepanciesStatistics();
});

document.getElementById('reset-discrepancy-filters').addEventListener('click', async () => {
    const typeFilter = document.getElementById('discrepancy-type-filter');
    const dateFromFilter = document.getElementById('discrepancy-date-from');
    const dateToFilter = document.getElementById('discrepancy-date-to');
    if (typeFilter) typeFilter.value = '';
    if (dateFromFilter) dateFromFilter.value = '';
    if (dateToFilter) dateToFilter.value = '';
    
    discrepanciesFilters = {};
    currentDiscrepanciesPage = 0;
    await loadDiscrepancies();
    await loadDiscrepanciesStatistics();
});

document.getElementById('export-discrepancies-excel').addEventListener('click', async () => {
    try {
        const blob = await StockDataLoader.exportDiscrepancies(discrepanciesFilters);
        const today = new Date().toISOString().split('T')[0];
        let filename = `vtrati_ta_pridbanna_${today}.xlsx`;
        
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(downloadUrl);
        document.body.removeChild(a);
        
        if (typeof showMessage === 'function') {
            showMessage('Excel файл успішно завантажено!', 'info');
        }
    } catch (error) {
        console.error('Error exporting to Excel:', error);
        if (typeof showMessage === 'function') {
            showMessage('Помилка при експорті в Excel', 'error');
        }
        handleError(error);
    }
});

// ============================================
// TRANSFERS CONTAINER FUNCTIONALITY
// ============================================

async function loadTransfers() {
    try {
        const data = await StockDataLoader.loadTransfers(currentTransfersPage, transfersPageSize, transfersFilters);
        transfersCache = data.content.slice();
        StockRenderer.renderTransfers(data.content, productMap, warehouseMap, userMap, withdrawalReasonMap, (id) => {
            openEditTransferModal(Number(id));
        });
        StockRenderer.updateTransfersPagination(data);
    } catch (error) {
        console.error('Error loading transfers:', error);
        if (typeof showMessage === 'function') {
            showMessage('Помилка завантаження переміщень', 'error');
        }
        handleError(error);
    }
}

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

const clearTransferFiltersBtn = document.getElementById('clear-transfer-filters');
if (clearTransferFiltersBtn) {
    clearTransferFiltersBtn.addEventListener('click', () => {
        clearTransfersFilters();
    });
}

document.getElementById('transfers-prev-page').addEventListener('click', async () => {
    if (currentTransfersPage > 0) {
        currentTransfersPage--;
        await loadTransfers();
    }
});

document.getElementById('transfers-next-page').addEventListener('click', async () => {
    currentTransfersPage++;
    await loadTransfers();
});

async function exportTransfersToExcel() {
    try {
        await StockDataLoader.exportTransfers(transfersFilters);
        if (typeof showMessage === 'function') {
            showMessage('Експорт переміщень успішно виконано', 'success');
        }
    } catch (error) {
        console.error('Error exporting transfers:', error);
        if (typeof showMessage === 'function') {
            showMessage('Помилка експорту переміщень', 'error');
        }
        handleError(error);
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
        
        const detailManagerSelect = document.getElementById('detail-vehicle-manager-id');
        const managerIdVal = detailManagerSelect?.value;
        const payload = {
            shipmentDate: detailVehicleDateInput?.value || null,
            vehicleNumber: detailVehicleVehicleInput?.value ?? null,
            description: detailVehicleDescriptionInput?.value ?? null,
            managerId: managerIdVal ? Number(managerIdVal) : null
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
            const updatedVehicle = await StockDataLoader.updateVehicle(currentVehicleId, payload);
            if (typeof showMessage === 'function') {
                showMessage('Дані машини оновлено', 'success');
            }
            currentVehicleDetails = updatedVehicle;
            currentVehicleItems = new Map();
            StockModal.populateVehicleForm(updatedVehicle);
            StockModal.setVehicleFormEditable(false);
            StockRenderer.renderVehicleDetails(updatedVehicle, productMap, warehouseMap);
            await loadVehicles(currentVehiclesPage);
            await loadBalance();
            closeModal('edit-vehicle-item-modal');
        } catch (error) {
            console.error('Error updating vehicle:', error);
            if (typeof showMessage === 'function') {
                showMessage(error.message || 'Помилка при оновленні машини', 'error');
            }
            handleError(error);
        }
    });
}

function updateTransfersSelectedFilters() {
    transfersFilters = StockFilters.buildTransfersFilters(transfersCustomSelects);
    StockFilters.updateTransfersFilterCounter(transfersFilters);
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
    currentTransfersPage = 0;
    loadTransfers();
 
    if (closeModal) {
        StockModal.closeModal('transfers-filter-modal');
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

    StockModal.openModal('edit-vehicle-item-modal');
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

            const performVehicleItemUpdate = async () => {
                try {
                    payload.quantity = roundedQuantity;
                    const updatedVehicle = await StockDataLoader.updateVehicleProduct(currentVehicleId, currentVehicleItemId, payload);
                    showMessage('Дані товару у машині оновлено', 'success');
                    currentVehicleDetails = updatedVehicle;
                    currentVehicleItems = new Map();
                    StockModal.populateVehicleForm(updatedVehicle);
                    StockModal.setVehicleFormEditable(false);
                    StockRenderer.renderVehicleDetails(updatedVehicle, productMap, warehouseMap);
                    await loadVehicles(currentVehiclesPage);
                    closeModal('edit-vehicle-item-modal');
                    await loadBalance();
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
                const updatedVehicle = await StockDataLoader.updateVehicleProduct(currentVehicleId, currentVehicleItemId, payload);
                showMessage('Дані товару у машині оновлено', 'success');
                currentVehicleDetails = updatedVehicle;
                currentVehicleItems = new Map();
                StockModal.populateVehicleForm(updatedVehicle);
                StockModal.setVehicleFormEditable(false);
                StockRenderer.renderVehicleDetails(updatedVehicle, productMap, warehouseMap);
                await loadVehicles(currentVehiclesPage);
                closeModal('edit-vehicle-item-modal');
                await loadBalance();
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

    StockModal.openModal('edit-transfer-modal');
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

        const performTransferUpdate = async () => {
            try {
                await StockDataLoader.updateTransfer(id, payload);
                showMessage('Переміщення успішно оновлено', 'info');
                closeModal('edit-transfer-modal');
                await loadBalance();
                await loadTransfers();
            } catch (error) {
                console.error('Error updating transfer:', error);
                handleError(error);
            }
        };

        if (roundedQuantity === 0) {
            const fromProductName = findNameByIdFromMap(productMap, currentTransferItem.fromProductId) || 'товару';
            const message = `Ви впевнені, що хочете повністю скасувати переміщення з ${fromProductName}?`;
            ConfirmationModal.show(
                message,
                CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
                performTransferUpdate,
                () => {}
            );
            return;
        }

        await performTransferUpdate();
    });
}

balanceEditModeRadios.forEach(radio => {
    radio.addEventListener('change', StockModal.updateBalanceEditMode);
});

function openBalanceEditModal(data) {
    currentBalanceEditData = data;
    StockModal.openBalanceEditModal(data, (warehouseId, productId) => {
        StockDataLoader.loadBalanceHistory(warehouseId, productId)
            .then(history => {
                StockRenderer.renderBalanceHistory(history, userMap);
            })
            .catch(error => {
                console.error('Error loading balance history:', error);
                handleError(error);
            });
    });
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
            await StockDataLoader.updateBalance(warehouseId, productId, payload);
            if (typeof showMessage === 'function') {
                showMessage('Баланс успішно оновлено', 'info');
            }
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


document.addEventListener('click', async (event) => {
    const historyButton = event.target.closest('#balance-history-btn');
    if (!historyButton) {
        return;
    }

    if (!currentBalanceEditData) {
        showMessage('Не вдалося визначити баланс для перегляду історії', 'error');
        return;
    }

    StockModal.openBalanceHistoryModal();
    
    try {
        const history = await StockDataLoader.loadBalanceHistory(currentBalanceEditData.warehouseId, currentBalanceEditData.productId);
        StockRenderer.renderBalanceHistory(history, userMap);
    } catch (error) {
        console.error('Error loading balance history:', error);
        handleError(error);
    }
});