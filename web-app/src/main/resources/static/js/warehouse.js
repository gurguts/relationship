let filters = {};
let userMap = new Map();
let productMap = new Map();
let warehouseMap = new Map();
let currentPage = 0;
let totalPages = 0;
let pageSize = 200;
const customSelects = {};

function setDefaultDates() {
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('entry-date-from-filter').value = today;
    document.getElementById('entry-date-to-filter').value = today;
    document.getElementById('entry-date').value = today;
}

document.getElementById('apply-filters').addEventListener('click', () => {
    updateSelectedFilters();
    loadWarehouseEntries(0);
    document.getElementById('filterModal').classList.remove('open');
});

document.getElementById('filter-counter').addEventListener('click', clearFilters);

document.getElementById('prev-page').addEventListener('click', () => {
    if (currentPage > 0) {
        loadWarehouseEntries(currentPage - 1);
    }
});

document.getElementById('next-page').addEventListener('click', () => {
    if (currentPage < totalPages - 1) {
        loadWarehouseEntries(currentPage + 1);
    }
});

/*--add--*/

document.getElementById('open-modal').addEventListener('click', () => {
    const modal = document.getElementById('entryModal');
    modal.classList.add('open');
});

document.getElementById('modal-close').addEventListener('click', () => {
    const modal = document.getElementById('entryModal');
    modal.classList.remove('open');
});

document.getElementById('entryModal').addEventListener('click', (e) => {
    if (e.target === e.currentTarget) {
        e.currentTarget.classList.remove('open');
    }
});

document.getElementById('open-filter-modal').addEventListener('click', () => {
    const modal = document.getElementById('filterModal');
    modal.classList.add('open');
});

document.getElementById('filter-modal-close').addEventListener('click', () => {
    const modal = document.getElementById('filterModal');
    modal.classList.remove('open');
});

document.getElementById('filterModal').addEventListener('click', (e) => {
    if (e.target === e.currentTarget) {
        e.currentTarget.classList.remove('open');
    }
});

document.getElementById('entryForm').addEventListener('submit',
    async (event) => {
    event.preventDefault();
    const formData = new FormData(event.target);
    const entry = {
        entryDate: formData.get('entry_date'),
        userId: formData.get('user_id'),
        warehouseId: formData.get('warehouse_id'),
        productId: formData.get('product_id'),
        quantity: parseFloat(formData.get('quantity'))
    };

    try {
        const response = await fetch('/api/v1/warehouse/entries', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(entry)
        });

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        event.target.reset();
        document.getElementById('entry-date').value = new Date().toISOString().split('T')[0];
        customSelects['user-id'].reset();
        customSelects['warehouse-id'].reset();
        customSelects['product-id'].reset();
        document.getElementById('entryModal').classList.remove('open');
        loadWarehouseEntries(currentPage);
    } catch (error) {
        console.error('Ошибка:', error);
        handleError(error);
    }
});

async function loadWarehouseEntries(page) {
    Object.keys(filters).forEach(key => {
        if (Array.isArray(filters[key]) && filters[key].length === 0) {
            delete filters[key];
        }
    });

    const queryParams = `page=${page}&size=${pageSize}&sort=entryDate&direction=DESC&filters=
    ${encodeURIComponent(JSON.stringify(filters))}`;

    try {
        const response = await fetch(`/api/v1/warehouse/entries?${queryParams}`, {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        });

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        const data = await response.json();
        renderWarehouseEntries(data.content);
        updatePagination(data.totalPages, page);
    } catch (error) {
        console.error('Ошибка:', error);
        handleError(error);
    }
}

function renderWarehouseEntries(entries) {
    const tbody = document.getElementById('warehouse-body');
    tbody.innerHTML = '';

    entries.forEach(entry => {
        const row = document.createElement('tr');
        row.dataset.id = entry.id;
        row.innerHTML = `
            <td>${warehouseMap.get(Number(entry.warehouseId)) || ''}</td>
            <td>${entry.entryDate}</td>
            <td>${userMap.get(Number(entry.userId)) || entry.userId}</td>
            <td>${productMap.get(Number(entry.productId)) || entry.productId}</td>
            <td>${entry.quantity}</td>
            <td>${entry.purchasedQuantity}</td>
            <td class="difference"></td>
        `;
        row.addEventListener('click', () => {
            if (!row.classList.contains('summary-row')) {
                openEditModal(entry);
            }
        });
        tbody.appendChild(row);
    });

    setTimeout(() => {
        let totalQuantity = 0;
        let totalPurchased = 0;
        let totalDifference = 0;

        const rows = tbody.querySelectorAll('tr');
        rows.forEach((row,) => {
            const quantityCell = row.cells[4];
            const purchasedCell = row.cells[5];
            const differenceCell = row.cells[6];

            const quantity = parseFloat(quantityCell.textContent) || 0;
            const purchased = parseFloat(purchasedCell.textContent) || 0;
            const difference = quantity - purchased;

            differenceCell.textContent = difference.toFixed(2);

            totalQuantity += quantity;
            totalPurchased += purchased;
            totalDifference += difference;
        });

        const summaryRow = document.createElement('tr');
        summaryRow.classList.add('summary-row');
        summaryRow.innerHTML = `
            <td colspan="4"><strong>Сума</strong></td>
            <td><strong>${totalQuantity.toFixed(2)}</strong></td>
            <td><strong>${totalPurchased.toFixed(2)}</strong></td>
            <td><strong>${totalDifference.toFixed(2)}</strong></td>
        `;
        tbody.appendChild(summaryRow);
    }, 0);
}

function updatePagination(total, page) {
    totalPages = total;
    currentPage = page;
    document.getElementById('page-info').textContent = `Сторінка ${currentPage + 1} з ${totalPages}`;
    document.getElementById('prev-page').disabled = currentPage === 0;
    document.getElementById('next-page').disabled = currentPage >= totalPages - 1;
}

const modal = document.getElementById('editModal');
const closeBtn = document.querySelector('.close');
const editForm = document.getElementById('editForm');

function openEditModal(entry) {
    if (entry.id == null) {
        handleError(new ErrorResponse(
            'DEFAULT',
            'Данні привозу не були вказані',
            null
        ));
        return;
    }
    document.getElementById('entryId').value = entry.id;
    document.getElementById('quantityInput').value = entry.quantity;
    modal.style.display = 'block';
}

function closeModal() {
    modal.style.display = 'none';
    editForm.reset();
}

closeBtn.addEventListener('click', closeModal);

editForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('entryId').value;
    const newQuantity = document.getElementById('quantityInput').value;

    try {
        const response = await fetch(`/api/v1/warehouse/entries/${id}`, {
            method: 'PATCH',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({quantity: Number(newQuantity)})
        });

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        closeModal();
        await loadWarehouseEntries(currentPage);
    } catch (error) {
        console.error('Ошибка при обновлении:', error);
        handleError(error);
    }
});

window.addEventListener('click', (e) => {
    if (e.target === modal) {
        closeModal();
    }
});

function updateSelectedFilters() {
    Object.keys(filters).forEach(key => delete filters[key]);

    Object.keys(customSelects).forEach(selectId => {
        const name = document.getElementById(selectId).name;
        const values = customSelects[selectId].getValue();
        if (values.length > 0) {
            filters[name] = values;
        }
    });

    const entryDateFrom = document.getElementById('entry-date-from-filter').value;
    const entryDateTo = document.getElementById('entry-date-to-filter').value;

    if (entryDateFrom) {
        filters['entry_date_from'] = [entryDateFrom];
    }
    if (entryDateTo) {
        filters['entry_date_to'] = [entryDateTo];
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

    const filterForm = document.getElementById('warehouseForm');
    if (filterForm) {
        filterForm.reset();
        Object.keys(customSelects).forEach(selectId => {
            customSelects[selectId].reset();
        });
        setDefaultDates();
    }

    updateFilterCounter();
    loadWarehouseEntries(0);
}

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

async function initializeCustomSelects() {
    const selects = document.querySelectorAll('select');
    selects.forEach(select => {
        customSelects[select.id] = createCustomSelect(select);
    });

    try {
        await fetchUsers();
        await fetchProducts();
        await fetchWarehouses();
        setDefaultDates();
        updateSelectedFilters();
        await loadWarehouseEntries(0);
    } catch (error) {
        console.error('Ошибка при инициализации:', error);
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
        option.value = String(item.id);
        option.text = item.name;
        select.appendChild(option);
    });

    if (customSelects[selectId]) {
        customSelects[selectId].populate(data);
    }
}

async function fetchUsers() {
    try {
        const response = await fetch('/api/v1/user');
        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }
        const users = await response.json();
        userMap = new Map(users.map(user => [user.id, user.name]));
        populateSelect('user-id-filter', users);
        populateSelect('user-id', users);
        /*populateSelect('user-remove-id', users);*/
    } catch (error) {
        console.error('Ошибка:', error);
        handleError(error);
    }
}

async function fetchProducts() {
    try {
        const response = await fetch('/api/v1/product');
        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }
        const products = await response.json();
        productMap = new Map(products.map(product => [product.id, product.name]));
        populateSelect('product-id-filter', products);
        populateSelect('product-id', products);
    } catch (error) {
        console.error('Ошибка:', error);
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

function exportTableToExcel(tableId, filename = 'warehouse_data') {
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

document.getElementById('export-excel-warehouse').addEventListener('click', () => {
    exportTableToExcel('warehouse-table', 'warehouse_export');
});

initializeCustomSelects();