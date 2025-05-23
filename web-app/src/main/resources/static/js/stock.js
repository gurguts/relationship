let productMap = new Map();
const customSelects = {};
const pageSize = 10;
let currentPage = 0;
let totalPages = 1;
let filters = {
    withdrawal_date_from: [],
    withdrawal_date_to: [],
    product_id: [],
    reason_type: []
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

const findNameByIdFromMap = (map, id) => {
    const numericId = Number(id);
    return map.get(numericId) || '';
};

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
        for (const [productId, quantity] of Object.entries(data.balanceByProduct)) {
            const productName = findNameByIdFromMap(productMap, productId);
            html += `<div class="balance-item">${productName}: ${quantity} кг</div>`;
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

async function loadWithdrawalHistory(page) {
    Object.keys(filters).forEach(key => {
        if (Array.isArray(filters[key]) && filters[key].length === 0) {
            delete filters[key];
        }
    });
    const queryParams = `page=${page}&size=${pageSize}&sort=withdrawalDate&direction=DESC&filters=
    ${encodeURIComponent(JSON.stringify(filters))}`;
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
            const reason = withdrawal.reasonType === 'SHIPMENT' ? 'Відгрузка машини' : 'Залишок сміття';
            html += `
                        <tr data-id="${withdrawal.id}">
                            <td>${productName}</td>
                            <td>${reason}</td>
                            <td>${withdrawal.quantity} кг</td>
                            <td>${withdrawal.withdrawalDate}</td>
                            <td>${withdrawal.description || 'Немає'}</td>
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

function updatePagination(total, page) {
    totalPages = total;
    currentPage = page;
    document.getElementById('page-info').textContent = `Сторінка ${currentPage + 1} з ${totalPages}`;
    document.getElementById('prev-page').disabled = currentPage === 0;
    document.getElementById('next-page').disabled = currentPage >= totalPages - 1;
}

// Handle withdrawal form submission
document.getElementById('withdraw-form').addEventListener('submit',
    async (e) => {
        e.preventDefault();
        const withdrawal = {
            productId: Number(document.getElementById('product-id').value),
            reasonType: document.getElementById('reason-type').value,
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

function openEditModal(id, withdrawals) {
    const withdrawal = withdrawals.find(w => w.id === Number(id));
    if (!withdrawal) return;
    populateProducts('edit-product-id');
    document.getElementById('edit-id').value = withdrawal.id;
    document.getElementById('edit-withdrawal-date').value = withdrawal.withdrawalDate;
    document.getElementById('edit-reason-type').value = withdrawal.reasonType;
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
            reasonType: document.getElementById('edit-reason-type').value,
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
const historyContainer = document.getElementById('history-container');
const filtersContainer = document.getElementById('filters-container');
const closeBtns = document.getElementsByClassName('close');

withdrawBtn.addEventListener('click', () => {
    populateProducts('product-id');
    withdrawModal.style.display = 'flex';
    withdrawModal.classList.add('open');
});

Array.from(closeBtns).forEach(btn => {
    btn.addEventListener('click', () => {
        closeModal(btn.closest('.modal').id);
    });
});

window.addEventListener('click', (e) => {
    if (e.target === withdrawModal || e.target === editModal) {
        closeModal(e.target.id);
    }
});

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    modal.classList.remove('open');
    modal.style.display = 'none';
    if (modalId === 'withdraw-modal') {
        document.getElementById('withdraw-form').reset();
    } else if (modalId === 'edit-modal') {
        document.getElementById('edit-form').reset();
    }
}

const historyBtn = document.getElementById('history-btn');
historyBtn.addEventListener('click', () => {
    if (historyContainer.style.display === 'block') {
        historyContainer.style.display = 'none';
        filtersContainer.classList.remove('open');
    } else {
        historyContainer.style.display = 'block';
        filtersContainer.classList.add('open');
        initializeCustomSelects();
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

async function initializeCustomSelects() {
    const selects = document.querySelectorAll('#product-id-filter, #reason-type-filter');
    selects.forEach(select => {
        if (!customSelects[select.id]) {
            customSelects[select.id] = createCustomSelect(select);
        }
    });
    try {
        await fetchProducts();
        const reasonTypes = [
            {id: 'SHIPMENT', name: 'Відгрузка машини'},
            {id: 'WASTE', name: 'Залишок сміття'}
        ];
        populateSelect('reason-type-filter', reasonTypes);
        updateSelectedFilters();
        loadWithdrawalHistory(currentPage);
    } catch (error) {
        console.error('Error initializing selects:', error);
        handleError(error);
    }
}

loadBalance();
fetchProducts();

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