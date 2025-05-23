let currentPage = 0;
const pageSize = 100;
let totalPages = 0;
let currentUserId = null;
let filters = {};
let userMap;
const customSelects = {};

const transactionTypeMap = {
    "DEPOSIT": "Поповнення",
    "WITHDRAWAL": "Зняття"
};

const transactionTypes = [
    {id: "DEPOSIT", name: "Поповнення"},
    {id: "WITHDRAWAL", name: "Зняття"}
];

const currencyTypes = [
    {id: "UAH", name: "UAH"},
    {id: "USD", name: "USD"},
    {id: "EUR", name: "EUR"}
];


document.addEventListener('DOMContentLoaded', () => {
    initializeCustomSelects();

    const fetchUsers = fetch('/api/v1/user')
        .then(response => response.json())
        .then(data => {
            userMap = new Map(data.map(item => [item.id, item.name]));
            populateSelect('target-user-id-filter', data);
            populateSelect('executor-user-id-filter', data);
        });

    Promise.all([fetchUsers])
        .then(() => {
            loadUserBalances(null);
        })
        .catch(error => {
            console.error('Помилка при завантаженні даних:', error);
        });
});


async function loadUserBalances() {
    try {
        const response = await fetch('/api/v1/user-balance', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        const users = await response.json();
        renderUserBalances(users);
    } catch (error) {
        console.error('Error:', error);
        handleError(error);
    }
}

function renderUserBalances(users) {
    const tbody = document.getElementById('user-balance-body');
    tbody.innerHTML = '';

    users.forEach(user => {
        const row = document.createElement('tr');

        const balancesHtml = Object.entries(user.balances || {})
            .map(([currency, amount]) =>
                `<div class="currency-balance">
                    <span>${currency}: </span>
                    <span class="balance-amount" 
                          onclick="openTransactionsModal(${user.id}, '${currency}')">
                        ${amount.toFixed(2)}
                    </span>
                </div>`
            ).join('');

        row.innerHTML = `
            <td>${user.fullName}</td>
            <td>${user.role}</td>
            <td class="balances-cell">${balancesHtml}</td>
            <td><button class="action-btn" onclick="openBalanceOperationModal(${user.id})">Поповнити/зняти</button></td>
        `;
        tbody.appendChild(row);
    });
}

function openBalanceOperationModal(userId) {
    const modal = document.getElementById('balanceOperationModal');
    modal.classList.add('show');
    document.getElementById('operationUserId').value = userId;
}

function closeBalanceOperationModal() {
    const modal = document.getElementById('balanceOperationModal');
    modal.classList.remove('show');
    document.getElementById('balanceOperationForm').reset();
}

document.querySelector('.close-balance-operation').addEventListener('click', closeBalanceOperationModal);

window.addEventListener('click', (event) => {
    if (event.target === document.getElementById('balanceOperationModal')) {
        closeBalanceOperationModal();
    }
    if (event.target === document.getElementById('transactionsModal')) {
        closeTransactionsModal();
    }
});

document.getElementById('balanceOperationForm').addEventListener('submit', async (event) => {
    event.preventDefault();

    const action = document.getElementById('operationAction').value;
    const amount = document.getElementById('operationAmount').value;
    const currency = document.getElementById('operationCurrency').value;
    const userId = document.getElementById('operationUserId').value;
    const description = document.getElementById('operationDescription').value;

    const endpoint = `/api/v1/transaction/${action}`;
    const requestBody = {
        targetUserId: parseInt(userId),
        amount: parseFloat(amount),
        currency: currency,
        description: description,
    };

    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestBody),
        });

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        closeBalanceOperationModal();
        await loadUserBalances();
        showMessage('Операція успішно виконана', 'info');
    } catch (error) {
        console.error('Ошибка:', error);
        handleError(error);
    }
});

document.querySelector('.close-transactions').addEventListener('click', closeTransactionsModal);

let currentCurrency = null;

function openTransactionsModal(userId, currency = null) {
    currentUserId = userId;
    currentCurrency = currency;

    const modal = document.getElementById('transactionsModal');
    modal.classList.add('show');

    const title = document.getElementById('transactions-title');
    title.textContent = userId ?
        `Транзакції користувача ${userMap.get(parseInt(userId)) || userId}` :
        'Усі транзакції';

    resetFilters(userId, currency);
    loadTransactions(userId, 0);
    document.getElementById('target-user-id-filter').parentElement.style.display = userId ? 'none' : 'block';
}

function closeTransactionsModal() {
    const modal = document.getElementById('transactionsModal');
    modal.classList.remove('show');
    filters = {};
}

async function loadTransactions(userId, page = 0) {
    if (userId && !Object.keys(filters).length) {
        filters = {target_user_id: [userId.toString()]};
    }

    Object.keys(filters).forEach(key => {
        if (Array.isArray(filters[key]) && filters[key].length === 0) {
            delete filters[key];
        }
    });

    const queryParams = `page=${page}&size=${pageSize}&sort=createdAt&direction=DESC&filters=
    ${encodeURIComponent(JSON.stringify(filters))}`;

    try {
        const response = await fetch(`/api/v1/transaction/search?${queryParams}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        const data = await response.json();
        renderTransactions(data.content);
        updatePagination(data.totalPages, page);
    } catch (error) {
        console.error('Error:', error);
        handleError(error);
    }
}

function renderTransactions(transactions) {
    const tbody = document.getElementById('transactions-body');
    tbody.innerHTML = '';

    transactions.forEach(transaction => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${userMap.get(parseInt(transaction.targetUserId)) || transaction.targetUserId}</td>
            <td>${transaction.amount}</td>
            <td>${transaction.currency}</td>
            <td>${transactionTypeMap[transaction.type] || transaction.type}</td>
            <td>${transaction.description || ''}</td>
            <td>${new Date(transaction.createdAt).toLocaleString()}</td>
            <td>${transaction.clientCompany}</td>
            <td>${transaction.executorUserId ?
            (userMap.get(parseInt(transaction.executorUserId)) || transaction.executorUserId) : ''}</td>
        `;
        tbody.appendChild(row);
    });
}

function updatePagination(total, page) {
    totalPages = total;
    currentPage = page;
    document.getElementById('page-info').textContent = `Сторінка ${currentPage + 1} з ${totalPages}`;
    document.getElementById('prev-page').disabled = currentPage === 0;
    document.getElementById('next-page').disabled = currentPage >= totalPages - 1;
}

document.getElementById('prev-page').addEventListener('click', () => {
    if (currentPage > 0) {
        loadTransactions(currentUserId, currentPage - 1);
    }
});

document.getElementById('next-page').addEventListener('click', () => {
    if (currentPage < totalPages - 1) {
        loadTransactions(currentUserId, currentPage + 1);
    }
});


document.getElementById('filter-counter').addEventListener('click', clearFilters);

document.getElementById('show-all-transactions')?.addEventListener('click', () => {
    openTransactionsModal(null);
});


function resetFilters(userId, currency = null) {
    filters = userId ? {target_user_id: [userId.toString()]} : {};

    if (currency) {
        filters.currency = [currency];
    }

    const filterForm = document.getElementById('filterForm');
    if (filterForm) {
        filterForm.reset();
        Object.keys(customSelects).forEach(selectId => {
            customSelects[selectId].reset();
        });

        if (userId) {
            customSelects['target-user-id-filter'].setValue([userId.toString()]);
        }

        if (currency) {
            customSelects['currency-filter'].setValue([currency]);
        }
    }
    updateFilterCounter();
}

document.getElementById('apply-filters').addEventListener('click', (event) => {
    event.preventDefault();
    updateSelectedFilters();
    loadTransactions(currentUserId, 0);
});


document.querySelector('.close-transactions').addEventListener('click', closeTransactionsModal);

window.addEventListener('click', (event) => {
    if (event.target === document.getElementById('transactionsModal')) {
        closeTransactionsModal();
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
    placeholder.textContent =
        currentSelect.querySelector('option[selected]')?.textContent || 'Параметр не задано';

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

function initializeCustomSelects() {
    const selects = document.querySelectorAll('select[id$="-filter"]');
    selects.forEach(select => {
        customSelects[select.id] = createCustomSelect(select);
        if (select.id === 'type-filter') {
            customSelects['type-filter'].populate(transactionTypes);
        }
        if (select.id === 'currency-filter') {
            customSelects['currency-filter'].populate(currencyTypes);
        }
    });
}

function populateSelect(selectId, data) {
    const select = document.getElementById(selectId);
    if (!select) {
        console.error(`Select with id "${selectId}" not found in DOM`);
        return;
    }

    select.innerHTML = '';
    const selectedValues = [];

    data.forEach(item => {
        const option = document.createElement('option');
        option.value = String(item.id);
        option.text = item.name;
        select.appendChild(option);
    });

    const customSelectId = selectId;
    if (customSelects[customSelectId]) {
        customSelects[customSelectId].populate(data);
        if (selectedValues.length > 0) {
            customSelects[customSelectId].setValue(selectedValues);
        }
    } else {
        console.warn(`Custom select "${customSelectId}" not initialized`);
    }
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

    const formData = new FormData(document.getElementById('filterForm'));
    const createdAtFrom = formData.get('created_at_from');
    const createdAtTo = formData.get('created_at_to');
    if (createdAtFrom) filters['created_at_from'] = [createdAtFrom];
    if (createdAtTo) filters['created_at_to'] = [createdAtTo];

    updateFilterCounter();
}

function updateFilterCounter() {
    const counterElement = document.getElementById('filter-counter');
    const countElement = document.getElementById('filter-count');

    if (!counterElement || !countElement) return;

    let totalFilters = 0;

    totalFilters += Object.values(filters)
        .filter(value => Array.isArray(value))
        .reduce((count, values) => count + values.length, 0);

    totalFilters += Object.keys(filters)
        .filter(key => !Array.isArray(filters[key]) && filters[key] !== '')
        .length;

    if (totalFilters > 0) {
        countElement.textContent = totalFilters;
        counterElement.style.display = 'inline-flex';
    } else {
        counterElement.style.display = 'none';
    }
}

function clearFilters() {
    Object.keys(filters).forEach(key => delete filters[key]);
    if (currentUserId) {
        filters.target_user_id = [currentUserId.toString()];
    }

    const filterForm = document.getElementById('filterForm');
    if (filterForm) {
        filterForm.reset();
        Object.keys(customSelects).forEach(selectId => {
            customSelects[selectId].reset();
        });
        if (currentUserId) {
            customSelects['target-user-id-filter'].setValue([currentUserId.toString()]);
        }
    }

    updateFilterCounter();
    loadTransactions(currentUserId, 0);
}

function exportTableToExcel(tableId, filename = 'transactions_data') {
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

document.getElementById('excel-export-transaction').addEventListener('click', () => {
    event.preventDefault();
    exportTableToExcel('transactions-table', 'transactions_export');
});