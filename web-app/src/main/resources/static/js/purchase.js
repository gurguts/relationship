const API_URL_PURCHASE = '/api/v1/purchase';
const API_URL = '/api/v1/client';

function escapeHtml(text) {
    if (text == null) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

const prevPageButton = document.getElementById('prev-btn');
const nextPageButton = document.getElementById('next-btn');
const paginationInfo = document.getElementById('pagination-info');
const allClientInfo = document.getElementById('all-client-info');
const loaderBackdrop = document.getElementById('loader-backdrop');
const filterForm = document.getElementById('filterForm');
const searchInput = document.getElementById('inputSearch');
let currentSort = 'createdAt';
let currentDirection = 'DESC';
let currentPage = 0;
let pageSize = 50;
const selectedFilters = {};
const customSelects = {};

let currentClientTypeId = null;
let currentClientType = null;
let clientTypeFields = [];
let visibleFields = [];
window.visibleFields = visibleFields;
let filterableFields = [];

let availableSources = [];
let availableUsers = [];
let availableProducts = [];

let sourceMap;
let userMap;
let productMap;
const currencyTypes = [
    {id: "UAH", name: "UAH"},
    {id: "USD", name: "USD"},
    {id: "EUR", name: "EUR"}
];

let availableCurrencies = currencyTypes;
const findNameByIdFromMap = (map, id) => {
    const numericId = Number(id); // Convert to number

    const name = map.get(numericId);

    return name || '';
};

prevPageButton.addEventListener('click', () => {
    if (currentPage > 0) {
        currentPage--;
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
    }
});

nextPageButton.addEventListener('click', () => {
    currentPage++;
    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
});

function updatePagination(totalData, dataOnPage, totalPages, currentPageIndex) {
    if (allClientInfo) {
        allClientInfo.textContent = `Закупок: ${totalData}`;
    }
    paginationInfo.textContent = `
        Закупок на сторінці: ${dataOnPage},
        Всього сторінок: ${totalPages},
        Поточна сторінка: ${currentPageIndex + 1}
    `;

    prevPageButton.disabled = currentPageIndex <= 0;
    nextPageButton.disabled = currentPageIndex >= totalPages - 1;
}


function renderPurchase(purchases) {
    const tbodyData = document.getElementById('client-table-body');
    if (!tbodyData) return;
    
    tbodyData.innerHTML = '';

    purchases.forEach(purchase => {
        const row = document.createElement('tr');
        row.classList.add('purchase-row');
        row.dataset.id = purchase.id;

        const clientName = purchase.client ? (purchase.client.company || purchase.client.person || '') : '';
        const userName = findNameByIdFromMap(userMap, purchase.userId) || '';
        const productName = findNameByIdFromMap(productMap, purchase.productId) || '';
        const sourceName = findNameByIdFromMap(sourceMap, purchase.sourceId) || '';
        const quantity = purchase.quantity ? purchase.quantity.toString() : '';
        const unitPrice = purchase.unitPrice ? purchase.unitPrice.toString() : '';
        const totalPrice = purchase.totalPrice ? purchase.totalPrice.toString() : '';
        const totalPriceEur = purchase.totalPriceEur ? purchase.totalPriceEur.toString() : '';
        const currency = purchase.currency || '';
        const exchangeRate = purchase.exchangeRate ? purchase.exchangeRate.toString() : '';
        const paymentMethod = purchase.paymentMethod === 'CASH' ? '2' : purchase.paymentMethod === 'BANKTRANSFER' ? '1' : '';
        const createdAt = purchase.createdAt ? new Date(purchase.createdAt).toLocaleDateString('ua-UA') : '';
        const isReceived = purchase.isReceived === true;

        const companyCell = document.createElement('td');
        companyCell.className = 'company-cell';
        companyCell.setAttribute('data-label', 'Назва клієнта');
        companyCell.textContent = clientName;
        row.appendChild(companyCell);

        const userCell = document.createElement('td');
        userCell.setAttribute('data-label', 'Водій');
        userCell.textContent = userName;
        row.appendChild(userCell);

        const productCell = document.createElement('td');
        productCell.setAttribute('data-label', 'Товар');
        productCell.textContent = productName;
        row.appendChild(productCell);

        const sourceCell = document.createElement('td');
        sourceCell.setAttribute('data-label', 'Залучення');
        sourceCell.textContent = sourceName;
        row.appendChild(sourceCell);

        const quantityCell = document.createElement('td');
        quantityCell.setAttribute('data-label', 'Кількість');
        quantityCell.textContent = quantity;
        row.appendChild(quantityCell);

        const unitPriceCell = document.createElement('td');
        unitPriceCell.setAttribute('data-label', 'Ціна за одиницю');
        unitPriceCell.textContent = unitPrice;
        row.appendChild(unitPriceCell);

        const totalPriceCell = document.createElement('td');
        totalPriceCell.setAttribute('data-label', 'Всього сплачено');
        totalPriceCell.textContent = totalPrice;
        row.appendChild(totalPriceCell);

        const currencyCell = document.createElement('td');
        currencyCell.setAttribute('data-label', 'Валюта');
        currencyCell.textContent = currency;
        row.appendChild(currencyCell);

        const totalPriceEurCell = document.createElement('td');
        totalPriceEurCell.setAttribute('data-label', 'Всього сплачено (EUR)');
        totalPriceEurCell.textContent = totalPriceEur;
        row.appendChild(totalPriceEurCell);

        const exchangeRateCell = document.createElement('td');
        exchangeRateCell.setAttribute('data-label', 'Курс');
        exchangeRateCell.textContent = exchangeRate;
        row.appendChild(exchangeRateCell);

        const paymentMethodCell = document.createElement('td');
        paymentMethodCell.setAttribute('data-label', 'Метод оплати');
        paymentMethodCell.textContent = paymentMethod;
        row.appendChild(paymentMethodCell);

        const createdAtCell = document.createElement('td');
        createdAtCell.setAttribute('data-label', 'Дата створення');
        createdAtCell.textContent = createdAt;
        row.appendChild(createdAtCell);

        const actionsCell = document.createElement('td');
        actionsCell.setAttribute('data-label', 'Дії');
        
        const editButton = document.createElement('button');
        editButton.className = 'edit-button';
        editButton.setAttribute('data-purchase-id', purchase.id);
        editButton.textContent = 'Редагувати';
        if (isReceived) {
            editButton.disabled = true;
            editButton.style.opacity = '0.5';
            editButton.style.cursor = 'not-allowed';
        }
        actionsCell.appendChild(editButton);

        const deleteButton = document.createElement('button');
        deleteButton.className = 'delete-button';
        deleteButton.setAttribute('data-purchase-id', purchase.id);
        deleteButton.textContent = 'Видалити';
        if (isReceived) {
            deleteButton.disabled = true;
            deleteButton.style.opacity = '0.5';
            deleteButton.style.cursor = 'not-allowed';
        }
        actionsCell.appendChild(deleteButton);
        
        row.appendChild(actionsCell);
        tbodyData.appendChild(row);

        if (companyCell && purchase.client) {
            companyCell.addEventListener('click', () => {
                if (typeof loadClientDetails === 'function') {
                    loadClientDetails(purchase.client);
                }
            });
        }

        if (!isReceived) {
            editButton.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                showEditModal(purchase);
            });

            deleteButton.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                deletePurchase(purchase.id, purchase.isReceived);
            });
        }
    });

    if (typeof applyColumnWidthsForPurchase === 'function' && currentClientTypeId) {
        setTimeout(() => {
            const storageKey = `purchase_${currentClientTypeId}`;
            applyColumnWidthsForPurchase('client-list', storageKey);
        }, 0);
    }
}


function generateProductOptions(selectedId) {
    if (!productMap.size) {
        const option = document.createElement('option');
        option.value = '';
        option.textContent = 'Продукти не завантажені';
        return option;
    }
    const fragment = document.createDocumentFragment();
    Array.from(productMap.entries()).forEach(([id, name]) => {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = name;
        if (id === selectedId) {
            option.selected = true;
        }
        fragment.appendChild(option);
    });
    return fragment;
}

function generateSourceOptions(selectedId) {
    if (!sourceMap.size) {
        const option = document.createElement('option');
        option.value = '';
        option.textContent = 'Джерела не завантажені';
        return option;
    }
    const fragment = document.createDocumentFragment();
    Array.from(sourceMap.entries()).forEach(([id, name]) => {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = name;
        if (id === selectedId) {
            option.selected = true;
        }
        fragment.appendChild(option);
    });
    return fragment;
}

function showEditModal(purchase) {
    if (purchase.isReceived === true) {
        if (typeof showMessage === 'function') {
            showMessage('Неможливо редагувати закупку, оскільки товар вже прийнято кладовщиком.', 'error');
        } else {
            alert('Неможливо редагувати закупку, оскільки товар вже прийнято кладовщиком.');
        }
        return;
    }
    
    const modal = document.getElementById('edit-modal');
    const form = document.getElementById('edit-form');
    const header = modal.querySelector('h3');
    const productSelect = form.querySelector('select[name="productId"]');
    const quantityInput = form.querySelector('input[name="quantity"]');
    const totalPriceInput = form.querySelector('input[name="totalPrice"]');
    const createdAtInput = form.querySelector('input[name="createdAt"]');
    const exchangeRate = form.querySelector('input[name="exchangeRate"]');
    const sourceSelect = form.querySelector('select[name="sourceId"]');
    const commentTextarea = form.querySelector('textarea[name="comment"]');

    header.textContent = `ID: ${purchase.id}`;
    productSelect.innerHTML = '';
    const productOptions = generateProductOptions(purchase.productId);
    productSelect.appendChild(productOptions);
    quantityInput.value = purchase.quantity || 0;
    totalPriceInput.value = purchase.totalPrice || 0;
    exchangeRate.value = purchase.exchangeRate || '';
    commentTextarea.value = purchase.comment || '';
    createdAtInput.value = purchase.createdAt
        ? new Date(purchase.createdAt.replace(' ', 'T') + 'Z').toISOString().split('T')[0]
        : '';
    sourceSelect.innerHTML = '';
    const sourceOptions = generateSourceOptions(purchase.sourceId);
    sourceSelect.appendChild(sourceOptions);

    modal.style.display = 'flex';
    setTimeout(() => modal.classList.add('active'), 10);

    form.onsubmit = async (e) => {
        e.preventDefault();
        const updatedData = {
            productId: productSelect.value,
            quantity: parseFloat(quantityInput.value),
            totalPrice: parseFloat(totalPriceInput.value),
            createdAt: createdAtInput.value,
            sourceId: sourceSelect.value,
            exchangeRate: exchangeRate.value,
            comment: commentTextarea.value
        };
        await savePurchase(purchase, updatedData);
        modal.classList.remove('active');
        setTimeout(() => {
            modal.style.display = 'none';
        }, 300);
    };

    const closeButton = document.getElementById('close-edit-modal');
    if (closeButton) {
        closeButton.onclick = () => {
            modal.classList.remove('active');
            setTimeout(() => {
                modal.style.display = 'none';
            }, 300);
        };
    }
}

async function savePurchase(purchase, updatedData) {
    try {
        const response = await fetch(`/api/v1/purchase/${purchase.id}`, {
            method: 'PATCH',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(updatedData)
        });
        if (response.ok) {
            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection)
        } else {
            const errorData = await response.json();
            // Показываем понятное сообщение для принятых закупок
            if (errorData.message && errorData.message.includes('прийнято кладовщиком')) {
                if (typeof showMessage === 'function') {
                    showMessage(errorData.message, 'error');
                } else {
                    alert(errorData.message);
                }
            } else {
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            }
        }
    } catch (error) {
        console.error('Помилка:', error);
        handleError(error);
    }
}

function deletePurchase(id, isReceived) {
    if (isReceived === true) {
        if (typeof showMessage === 'function') {
            showMessage('Неможливо видалити закупку, оскільки товар вже прийнято кладовщиком.', 'error');
        } else {
            alert('Неможливо видалити закупку, оскільки товар вже прийнято кладовщиком.');
        }
        return;
    }
    
    if (confirm("Ви впевнені, що хочете видалити цей запис?")) {
        loaderBackdrop.style.display = 'flex';
        fetch(`/api/v1/purchase/${id}`, {
            method: 'DELETE'
        })
            .then(response => {
                if (response.ok) {
                    showMessage("Збір успішно видалено.", 'info');
                    loadDataWithSort(0, 100, currentSort, currentDirection);
                } else {
                    const errorData = response.json();
                    throw new ErrorResponse(errorData.error, errorData.message, errorData.details);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                handleError(error);
            })
            .finally(() => {
                loaderBackdrop.style.display = 'none';
            });
    }
}


function convertFieldNamesToFieldIds(filters) {
    const converted = { ...filters };
    const allFields = filterableFields && filterableFields.length > 0 ? filterableFields : 
                     (clientTypeFields && clientTypeFields.length > 0 ? clientTypeFields : []);
    
    if (allFields.length === 0) {
        return converted;
    }
    
    const fieldNameToIdMap = {};
    allFields.forEach(field => {
        if (field.fieldName && field.id) {
            fieldNameToIdMap[field.fieldName] = field.id;
        }
    });
    
    Object.keys(converted).forEach(key => {
        if (fieldNameToIdMap[key]) {
            const fieldId = fieldNameToIdMap[key];
            const newKey = `field_${fieldId}`;
            converted[newKey] = converted[key];
            delete converted[key];
        } else if (key.endsWith('From') || key.endsWith('To')) {
            const baseName = key.endsWith('From') ? key.slice(0, -4) : key.slice(0, -2);
            if (fieldNameToIdMap[baseName]) {
                const fieldId = fieldNameToIdMap[baseName];
                const suffix = key.endsWith('From') ? 'From' : 'To';
                const newKey = `field_${fieldId}${suffix}`;
                converted[newKey] = converted[key];
                delete converted[key];
            }
        }
    });
    
    return converted;
}

async function loadDataWithSort(page, size, sort, direction) {
    if (!currentClientTypeId) {
        return;
    }
    
    loaderBackdrop.style.display = 'flex';
    const searchTerm = searchInput ? searchInput.value : '';
    let queryParams = `page=${page}&size=${size}&sort=${sort}&direction=${direction}`;

    if (searchTerm) {
        queryParams += `&q=${encodeURIComponent(searchTerm)}`;
    }

    const filters = { ...selectedFilters };
    if (currentClientTypeId) {
        filters.clientTypeId = [currentClientTypeId.toString()];
    }

    const convertedFilters = convertFieldNamesToFieldIds(filters);
    if (Object.keys(convertedFilters).length > 0) {
        queryParams += `&filters=${encodeURIComponent(JSON.stringify(convertedFilters))}`;
    }

    try {
        const response = await fetch(`${API_URL_PURCHASE}/search?${queryParams}`);

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        const data = await response.json();
        renderPurchase(data.content);
        
        setupSortHandlers();

        updatePagination(data.totalElements, data.content.length, data.totalPages, currentPage);
    } catch (error) {
        console.error('Ошибка:', error);
        handleError(error);
    } finally {
        loaderBackdrop.style.display = 'none';
    }
}

function updateSortIndicators() {
    document.querySelectorAll('th[data-sort]').forEach(th => {
        const sortField = th.getAttribute('data-sort');
        th.classList.remove('sort-asc', 'sort-desc');
        
        if (currentSort === sortField) {
            if (currentDirection === 'ASC') {
                th.classList.add('sort-asc');
            } else {
                th.classList.add('sort-desc');
            }
        }
    });
}

function getDefaultSortDirection(sortField) {
    if (sortField === 'updatedAt' || sortField === 'createdAt') {
        return 'DESC';
    }
    return 'ASC';
}

function setupSortHandlers() {
    document.querySelectorAll('th[data-sort]').forEach(th => {
        th.removeEventListener('click', handleSortClick);
        th.addEventListener('click', handleSortClick);
    });
    updateSortIndicators();
}

function handleSortClick(event) {
    const th = event.currentTarget;
    const sortField = th.getAttribute('data-sort');
    
    const staticFields = ['quantity', 'unitPrice', 'totalPrice', 'currency', 'totalPriceEur', 'exchangeRate', 'paymentMethod', 'createdAt', 'updatedAt'];
    
    if (!sortField || !staticFields.includes(sortField)) {
        return;
    }
    
    if (currentSort === sortField) {
        currentDirection = currentDirection === 'ASC' ? 'DESC' : 'ASC';
    } else {
        currentSort = sortField;
        currentDirection = getDefaultSortDirection(sortField);
    }
    
    currentPage = 0;
    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
}

function buildPurchaseTable() {
    const thead = document.querySelector('#client-list table thead tr');
    if (!thead) return;
    
    thead.innerHTML = '';
    
    const headers = [
        { text: 'Назва клієнта', sort: null },
        { text: 'Водій', sort: null },
        { text: 'Товар', sort: null },
        { text: 'Залучення', sort: null },
        { text: 'Кількість', sort: 'quantity' },
        { text: 'Ціна за одиницю', sort: 'unitPrice' },
        { text: 'Всього сплачено', sort: 'totalPrice' },
        { text: 'Валюта', sort: 'currency' },
        { text: 'Всього сплачено (EUR)', sort: 'totalPriceEur' },
        { text: 'Курс', sort: 'exchangeRate' },
        { text: 'Метод оплати', sort: 'paymentMethod' },
        { text: 'Дата створення', sort: 'createdAt' },
        { text: 'Дії', sort: null }
    ];
    
    headers.forEach(header => {
        const th = document.createElement('th');
        th.textContent = header.text;
        if (header.sort) {
            th.setAttribute('data-sort', header.sort);
            th.style.cursor = 'pointer';
        }
        thead.appendChild(th);
    });
    
    setupSortHandlers();
}

function buildDynamicFilters() {
    if (!filterForm) return;

    if (buildDynamicFilters._isBuilding) {
        return;
    }
    buildDynamicFilters._isBuilding = true;
    
    try {
        Object.keys(customSelects).forEach(selectId => {
            if (selectId.startsWith('filter-')) {
                const customSelect = customSelects[selectId];
                if (customSelect && typeof customSelect.reset === 'function') {
                    try {
                        customSelect.reset();
                    } catch (e) {
                        console.warn('Error resetting custom select:', e);
                    }
                }
                delete customSelects[selectId];
            }
        });

        const existingFilters = filterForm.querySelectorAll('h2, .filter-block, .select-section-item');
        existingFilters.forEach(el => {
            const selects = el.querySelectorAll('select');
            selects.forEach(sel => {
                sel.innerHTML = '';
            });
            el.remove();
        });

        const purchaseH2 = document.createElement('h2');
        purchaseH2.textContent = 'Фільтри закупівлі:';
        filterForm.appendChild(purchaseH2);

        const userSelectItem = document.createElement('div');
        userSelectItem.className = 'select-section-item';
        userSelectItem.innerHTML = `
            <br>
            <label class="select-label-style" for="filter-user">Водій:</label>
            <select id="filter-user" name="user" multiple>
            </select>
        `;
        filterForm.appendChild(userSelectItem);

        const sourceSelectItem = document.createElement('div');
        sourceSelectItem.className = 'select-section-item';
        sourceSelectItem.innerHTML = `
            <br>
            <label class="select-label-style" for="filter-source">Залучення:</label>
            <select id="filter-source" name="source" multiple>
            </select>
        `;
        filterForm.appendChild(sourceSelectItem);

        const productSelectItem = document.createElement('div');
        productSelectItem.className = 'select-section-item';
        productSelectItem.innerHTML = `
            <br>
            <label class="select-label-style" for="filter-product">Товар:</label>
            <select id="filter-product" name="product" multiple>
            </select>
        `;
        filterForm.appendChild(productSelectItem);

        const quantityH2 = document.createElement('h2');
        quantityH2.textContent = 'Кількість:';
        filterForm.appendChild(quantityH2);
        
        const quantityBlock = document.createElement('div');
        quantityBlock.className = 'filter-block';
        quantityBlock.innerHTML = `
            <label class="from-to-style" for="filter-quantity-from">Від:</label>
            <input type="number" id="filter-quantity-from" name="quantityFrom" step="0.01" placeholder="Мінімум">
            <label class="from-to-style" for="filter-quantity-to">До:</label>
            <input type="number" id="filter-quantity-to" name="quantityTo" step="0.01" placeholder="Максимум">
        `;
        filterForm.appendChild(quantityBlock);

        const unitPriceH2 = document.createElement('h2');
        unitPriceH2.textContent = 'Ціна за одиницю:';
        filterForm.appendChild(unitPriceH2);
        
        const unitPriceBlock = document.createElement('div');
        unitPriceBlock.className = 'filter-block';
        unitPriceBlock.innerHTML = `
            <label class="from-to-style" for="filter-unitPrice-from">Від:</label>
            <input type="number" id="filter-unitPrice-from" name="unitPriceFrom" step="0.01" placeholder="Мінімум">
            <label class="from-to-style" for="filter-unitPrice-to">До:</label>
            <input type="number" id="filter-unitPrice-to" name="unitPriceTo" step="0.01" placeholder="Максимум">
        `;
        filterForm.appendChild(unitPriceBlock);

        const totalPriceH2 = document.createElement('h2');
        totalPriceH2.textContent = 'Всього сплачено:';
        filterForm.appendChild(totalPriceH2);
        
        const totalPriceBlock = document.createElement('div');
        totalPriceBlock.className = 'filter-block';
        totalPriceBlock.innerHTML = `
            <label class="from-to-style" for="filter-totalPrice-from">Від:</label>
            <input type="number" id="filter-totalPrice-from" name="totalPriceFrom" step="0.01" placeholder="Мінімум">
            <label class="from-to-style" for="filter-totalPrice-to">До:</label>
            <input type="number" id="filter-totalPrice-to" name="totalPriceTo" step="0.01" placeholder="Максимум">
        `;
        filterForm.appendChild(totalPriceBlock);

        const paymentMethodSelectItem = document.createElement('div');
        paymentMethodSelectItem.className = 'select-section-item';
        paymentMethodSelectItem.innerHTML = `
            <br>
            <label class="select-label-style" for="filter-paymentMethod">Метод оплати:</label>
            <select id="filter-paymentMethod" name="paymentMethod">
                <option value="">Всі</option>
                <option value="2">2</option>
                <option value="1">1</option>
            </select>
        `;
        filterForm.appendChild(paymentMethodSelectItem);

        const createdAtH2 = document.createElement('h2');
        createdAtH2.textContent = 'Дата створення:';
        filterForm.appendChild(createdAtH2);
        
        const createdAtBlock = document.createElement('div');
        createdAtBlock.className = 'filter-block';
        createdAtBlock.innerHTML = `
            <label class="from-to-style" for="filter-createdAt-from">Від:</label>
            <input type="date" id="filter-createdAt-from" name="createdAtFrom">
            <label class="from-to-style" for="filter-createdAt-to">До:</label>
            <input type="date" id="filter-createdAt-to" name="createdAtTo">
        `;
        filterForm.appendChild(createdAtBlock);

        const currencySelectItem = document.createElement('div');
        currencySelectItem.className = 'select-section-item';
        currencySelectItem.innerHTML = `
            <br>
            <label class="select-label-style" for="filter-currency">Валюта:</label>
            <select id="filter-currency" name="currency" multiple>
            </select>
        `;
        filterForm.appendChild(currencySelectItem);

        const clientH2 = document.createElement('h2');
        clientH2.textContent = 'Фільтри клієнта:';
        filterForm.appendChild(clientH2);

        const clientCreatedAtH2 = document.createElement('h2');
        clientCreatedAtH2.textContent = 'Дата створення клієнта:';
        filterForm.appendChild(clientCreatedAtH2);
        
        const clientCreatedAtBlock = document.createElement('div');
        clientCreatedAtBlock.className = 'filter-block';
        clientCreatedAtBlock.innerHTML = `
            <label class="from-to-style" for="filter-clientCreatedAt-from">Від:</label>
            <input type="date" id="filter-clientCreatedAt-from" name="clientCreatedAtFrom">
            <label class="from-to-style" for="filter-clientCreatedAt-to">До:</label>
            <input type="date" id="filter-clientCreatedAt-to" name="clientCreatedAtTo">
        `;
        filterForm.appendChild(clientCreatedAtBlock);

        const clientUpdatedAtH2 = document.createElement('h2');
        clientUpdatedAtH2.textContent = 'Дата оновлення клієнта:';
        filterForm.appendChild(clientUpdatedAtH2);
        
        const clientUpdatedAtBlock = document.createElement('div');
        clientUpdatedAtBlock.className = 'filter-block';
        clientUpdatedAtBlock.innerHTML = `
            <label class="from-to-style" for="filter-clientUpdatedAt-from">Від:</label>
            <input type="date" id="filter-clientUpdatedAt-from" name="clientUpdatedAtFrom">
            <label class="from-to-style" for="filter-clientUpdatedAt-to">До:</label>
            <input type="date" id="filter-clientUpdatedAt-to" name="clientUpdatedAtTo">
        `;
        filterForm.appendChild(clientUpdatedAtBlock);

        const clientSourceSelectItem = document.createElement('div');
        clientSourceSelectItem.className = 'select-section-item';
        clientSourceSelectItem.innerHTML = `
            <br>
            <label class="select-label-style" for="filter-clientSource">Залучення клієнта:</label>
            <select id="filter-clientSource" name="clientSource" multiple>
            </select>
        `;
        filterForm.appendChild(clientSourceSelectItem);

        setTimeout(() => {
            const clientSourceSelect = filterForm.querySelector('#filter-clientSource');
            if (clientSourceSelect && !customSelects['filter-clientSource'] && availableSources && availableSources.length > 0) {
                const sourceData = availableSources.map(s => ({
                    id: s.id,
                    name: s.name
                }));
                if (typeof createCustomSelect === 'function') {
                    const customSelect = createCustomSelect(clientSourceSelect, true);
                    if (customSelect) {
                        customSelects['filter-clientSource'] = customSelect;
                        customSelect.populate(sourceData);
                    }
                }
            }
        }, 0);

        if (filterableFields && filterableFields.length > 0) {

            filterableFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
            filterableFields.forEach(field => {
                if (field.fieldType === 'DATE') {
                    const h2 = document.createElement('h2');
                    h2.textContent = field.fieldLabel + ':';
                    filterForm.appendChild(h2);
                    
                    const filterBlock = document.createElement('div');
                    filterBlock.className = 'filter-block';
                    filterBlock.innerHTML = `
                        <label class="from-to-style" for="filter-${field.fieldName}-from">Від:</label>
                        <input type="date" id="filter-${field.fieldName}-from" name="${field.fieldName}From">
                        <label class="from-to-style" for="filter-${field.fieldName}-to">До:</label>
                        <input type="date" id="filter-${field.fieldName}-to" name="${field.fieldName}To">
                    `;
                    filterForm.appendChild(filterBlock);
                } else if (field.fieldType === 'NUMBER') {
                    const h2 = document.createElement('h2');
                    h2.textContent = field.fieldLabel + ':';
                    filterForm.appendChild(h2);
                    
                    const filterBlock = document.createElement('div');
                    filterBlock.className = 'filter-block';
                    filterBlock.innerHTML = `
                        <label class="from-to-style" for="filter-${field.fieldName}-from">Від:</label>
                        <input type="number" id="filter-${field.fieldName}-from" name="${field.fieldName}From" step="any" placeholder="Мінімум">
                        <label class="from-to-style" for="filter-${field.fieldName}-to">До:</label>
                        <input type="number" id="filter-${field.fieldName}-to" name="${field.fieldName}To" step="any" placeholder="Максимум">
                    `;
                    filterForm.appendChild(filterBlock);
                } else if (field.fieldType === 'LIST') {
                    const selectId = `filter-${field.fieldName}`;

                    if (customSelects[selectId]) {
                        try {
                            const oldSelect = customSelects[selectId];
                            if (oldSelect && typeof oldSelect.reset === 'function') {
                                oldSelect.reset();
                            }
                        } catch (e) {
                            console.warn('Error cleaning up old custom select:', e);
                        }
                        delete customSelects[selectId];
                    }

                    const existingContainer = document.querySelector(`.custom-select-container[data-for="${selectId}"]`);
                    if (existingContainer) {
                        existingContainer.remove();
                    }
                    
                    const selectItem = document.createElement('div');
                    selectItem.className = 'select-section-item';
                    selectItem.appendChild(document.createElement('br'));
                    
                    const label = document.createElement('label');
                    label.className = 'select-label-style';
                    label.setAttribute('for', `filter-${field.fieldName}`);
                    label.textContent = field.fieldLabel + ':';
                    selectItem.appendChild(label);
                    
                    const select = document.createElement('select');
                    select.id = `filter-${field.fieldName}`;
                    select.name = field.fieldName;
                    select.multiple = true;
                    selectItem.appendChild(select);
                    
                    filterForm.appendChild(selectItem);

                    if (field.listValues && field.listValues.length > 0) {
                        field.listValues.forEach(listValue => {
                            const option = document.createElement('option');
                            option.value = listValue.id;
                            option.textContent = listValue.value;
                            select.appendChild(option);
                        });
                    }

                    setTimeout(() => {
                        if (typeof createCustomSelect === 'function') {
                            const existingContainer = document.querySelector(`.custom-select-container[data-for="${selectId}"]`);
                            if (existingContainer) {
                                console.warn('Custom select container already exists for:', selectId);
                                return;
                            }
                            
                            const customSelect = createCustomSelect(select, true);
                            if (customSelect) {
                                customSelects[selectId] = customSelect;
                                
                                if (field.listValues && field.listValues.length > 0) {
                                    const listData = field.listValues.map(lv => ({
                                        id: lv.id,
                                        name: lv.value
                                    }));
                                    customSelect.populate(listData);
                                    
                                    if (selectedFilters[field.fieldName]) {
                                        const savedValues = selectedFilters[field.fieldName];
                                        if (Array.isArray(savedValues) && savedValues.length > 0) {
                                            const validValues = savedValues.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                                            if (validValues.length > 0) {
                                                setTimeout(() => {
                                                    customSelect.setValue(validValues);
                                                }, 50);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }, 0);
                } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                    const selectItem = document.createElement('div');
                    selectItem.className = 'select-section-item';
                    selectItem.innerHTML = `
                        <br>
                        <label class="select-label-style" for="filter-${field.fieldName}">${field.fieldLabel}:</label>
                        <input type="text" 
                               id="filter-${field.fieldName}" 
                               name="${field.fieldName}" 
                               placeholder="Пошук...">
                    `;
                    filterForm.appendChild(selectItem);
                } else if (field.fieldType === 'BOOLEAN') {
                    const selectItem = document.createElement('div');
                    selectItem.className = 'select-section-item';
                    selectItem.innerHTML = `
                        <br>
                        <label class="select-label-style" for="filter-${field.fieldName}">${field.fieldLabel}:</label>
                        <select id="filter-${field.fieldName}" name="${field.fieldName}">
                            <option value="">Всі</option>
                            <option value="true">Так</option>
                            <option value="false">Ні</option>
                        </select>
                    `;
                    filterForm.appendChild(selectItem);
                }
            });
        }

        setTimeout(() => {
            const userSelect = filterForm.querySelector('#filter-user');
            if (userSelect && availableUsers && availableUsers.length > 0) {
                const userData = availableUsers.map(u => ({
                    id: u.id,
                    name: u.name
                }));
                if (typeof createCustomSelect === 'function') {
                    const customSelect = createCustomSelect(userSelect, true);
                    if (customSelect) {
                        customSelects['filter-user'] = customSelect;
                        customSelect.populate(userData);
                    }
                }
            }

            const sourceSelect = filterForm.querySelector('#filter-source');
            if (sourceSelect && availableSources && availableSources.length > 0) {
                const sourceData = availableSources.map(s => ({
                    id: s.id,
                    name: s.name
                }));
                if (typeof createCustomSelect === 'function') {
                    const customSelect = createCustomSelect(sourceSelect, true);
                    if (customSelect) {
                        customSelects['filter-source'] = customSelect;
                        customSelect.populate(sourceData);
                    }
                }
            }

            const productSelect = filterForm.querySelector('#filter-product');
            if (productSelect && availableProducts && availableProducts.length > 0) {
                const productData = availableProducts.map(p => ({
                    id: p.id,
                    name: p.name
                }));
                if (typeof createCustomSelect === 'function') {
                    const customSelect = createCustomSelect(productSelect, true);
                    if (customSelect) {
                        customSelects['filter-product'] = customSelect;
                        customSelect.populate(productData);
                    }
                }
            }

            const currencySelect = filterForm.querySelector('#filter-currency');
            if (currencySelect && availableCurrencies && availableCurrencies.length > 0) {
                const currencyData = availableCurrencies.map(c => ({
                    id: c.id,
                    name: c.name
                }));
                if (typeof createCustomSelect === 'function') {
                    const customSelect = createCustomSelect(currencySelect, true);
                    if (customSelect) {
                        customSelects['filter-currency'] = customSelect;
                        customSelect.populate(currencyData);
                    }
                }
            }
        }, 0);
    } finally {
        buildDynamicFilters._isBuilding = false;
    }
}

async function showClientTypeSelectionModal() {
    const modal = document.getElementById('clientTypeSelectionModal');
    if (!modal) return;
    
    try {
        const response = await fetch('/api/v1/client-type/active');
        if (!response.ok) return;
        const allClientTypes = await response.json();
        
        const userId = localStorage.getItem('userId');
        let accessibleClientTypeIds = new Set();
        
        if (userId) {
            try {
                const permissionsResponse = await fetch(`/api/v1/client-type/permission/me`);
                if (permissionsResponse.ok) {
                    const permissions = await permissionsResponse.json();
                    permissions.forEach(perm => {
                        if (perm.canView) {
                            accessibleClientTypeIds.add(perm.clientTypeId);
                        }
                    });
                }
            } catch (error) {
                console.warn('Failed to load user client type permissions:', error);
                allClientTypes.forEach(type => accessibleClientTypeIds.add(type.id));
            }
        }
        
        const authorities = localStorage.getItem('authorities');
        let userAuthorities = [];
        try {
            if (authorities) {
                userAuthorities = authorities.startsWith('[')
                    ? JSON.parse(authorities)
                    : authorities.split(',').map(auth => auth.trim());
            }
        } catch (error) {
            console.error('Failed to parse authorities:', error);
        }
        
        const isAdmin = userAuthorities.includes('system:admin') || userAuthorities.includes('administration:view');

        if (isAdmin || accessibleClientTypeIds.size === 0) {
            allClientTypes.forEach(type => accessibleClientTypeIds.add(type.id));
        }
        
        const accessibleClientTypes = allClientTypes.filter(type => accessibleClientTypeIds.has(type.id));
        
        const listContainer = document.getElementById('client-types-selection-list');
        if (!listContainer) return;
        
        if (accessibleClientTypes.length === 0) {
            const emptyMessage = document.createElement('p');
            emptyMessage.style.textAlign = 'center';
            emptyMessage.style.color = 'var(--main-grey)';
            emptyMessage.style.padding = '2em';
            emptyMessage.textContent = 'Немає доступних типів клієнтів';
            listContainer.textContent = '';
            listContainer.appendChild(emptyMessage);
            modal.style.display = 'flex';
        } else if (accessibleClientTypes.length === 1) {
            window.location.href = `/purchase?type=${accessibleClientTypes[0].id}`;
            return;
        } else {
            listContainer.textContent = '';
            accessibleClientTypes.forEach(type => {
                const card = document.createElement('div');
                card.className = 'client-type-card';
                
                const iconDiv = document.createElement('div');
                iconDiv.className = 'client-type-card-icon';
                iconDiv.textContent = '👥';
                card.appendChild(iconDiv);
                
                const nameDiv = document.createElement('div');
                nameDiv.className = 'client-type-card-name';
                nameDiv.textContent = type.name;
                card.appendChild(nameDiv);
                
                card.addEventListener('click', () => {
                    window.location.href = `/purchase?type=${type.id}`;
                });
                listContainer.appendChild(card);
            });
            modal.style.display = 'flex';
        }

        const closeBtn = document.querySelector('.close-client-type-modal');
        if (closeBtn) {
            if (closeBtn._closeTypeModalHandler) {
                closeBtn.removeEventListener('click', closeBtn._closeTypeModalHandler);
            }
            const closeHandler = () => {
                modal.style.display = 'none';
            };
            closeBtn._closeTypeModalHandler = closeHandler;
            closeBtn.addEventListener('click', closeHandler);
        }
        
        if (modal._typeModalClickHandler) {
            modal.removeEventListener('click', modal._typeModalClickHandler);
        }
        const modalClickHandler = (e) => {
            if (e.target === modal) {
                modal.style.display = 'none';
            }
        };
        modal._typeModalClickHandler = modalClickHandler;
        modal.addEventListener('click', modalClickHandler);
    } catch (error) {
        console.error('Error loading client types:', error);
    }
}

async function updateNavigationWithCurrentType(typeId) {
    try {
        const response = await fetch(`/api/v1/client-type/${typeId}`);
        if (!response.ok) return;
        
        const clientType = await response.json();
        const navLink = document.querySelector('#nav-purchase a');
        
        if (navLink && clientType.name) {
            navLink.textContent = '';
            
            const labelSpan = document.createElement('span');
            labelSpan.className = 'nav-client-type-label';
            labelSpan.textContent = 'Закупівлі:';
            navLink.appendChild(labelSpan);
            
            const nameSpan = document.createElement('span');
            nameSpan.className = 'nav-client-type-name';
            nameSpan.textContent = clientType.name;
            navLink.appendChild(nameSpan);
            
            const arrowSpan = document.createElement('span');
            arrowSpan.className = 'dropdown-arrow';
            arrowSpan.textContent = '▼';
            navLink.appendChild(arrowSpan);
        }

        const dropdown = document.getElementById('purchase-types-dropdown');
        if (dropdown) {
            const links = dropdown.querySelectorAll('a');
            links.forEach(link => {
                link.classList.remove('active');
                if (link.href.includes(`type=${typeId}`)) {
                    link.classList.add('active');
                }
            });
        }
    } catch (error) {
        console.error('Error updating navigation:', error);
    }
}

async function loadClientType(typeId) {
    try {
        const response = await fetch(`/api/v1/client-type/${typeId}`);
        if (!response.ok) throw new Error('Failed to load client type');
        currentClientType = await response.json();
        document.title = currentClientType.name;
    } catch (error) {
        console.error('Error loading client type:', error);
    }
}

async function loadClientTypeFields(typeId) {
    try {
        const [fieldsRes, visibleRes, searchableRes, filterableRes] = await Promise.all([
            fetch(`/api/v1/client-type/${typeId}/field`),
            fetch(`/api/v1/client-type/${typeId}/field/visible`),
            fetch(`/api/v1/client-type/${typeId}/field/searchable`),
            fetch(`/api/v1/client-type/${typeId}/field/filterable`)
        ]);
        
        clientTypeFields = await fieldsRes.json();
        window.clientTypeFields = clientTypeFields;
        visibleFields = await visibleRes.json();
        window.visibleFields = visibleFields;
        filterableFields = await filterableRes.json();
    } catch (error) {
        console.error('Error loading fields:', error);
    }
}

async function loadEntitiesAndApplyFilters() {
    try {
        const response = await fetch('/api/v1/entities');
        if (!response.ok) return;
        
        const data = await response.json();
        availableSources = data.sources || [];
        availableUsers = data.users || [];
        availableProducts = data.products || [];
        
        sourceMap = new Map(availableSources.map(item => [item.id, item.name]));
        userMap = new Map(availableUsers.map(item => [item.id, item.name]));
        productMap = new Map(availableProducts.map(item => [item.id, item.name]));

        if (filterForm) {
            if (selectedFilters['createdAtFrom']) {
                const fromInput = filterForm.querySelector('#filter-createdAt-from');
                if (fromInput) {
                    const value = Array.isArray(selectedFilters['createdAtFrom']) 
                        ? selectedFilters['createdAtFrom'][0] 
                        : selectedFilters['createdAtFrom'];
                    if (value) fromInput.value = value;
                }
            }
            if (selectedFilters['createdAtTo']) {
                const toInput = filterForm.querySelector('#filter-createdAt-to');
                if (toInput) {
                    const value = Array.isArray(selectedFilters['createdAtTo']) 
                        ? selectedFilters['createdAtTo'][0] 
                        : selectedFilters['createdAtTo'];
                    if (value) toInput.value = value;
                }
            }

            const purchaseNumberFields = [
                { key: 'quantityFrom', inputId: '#filter-quantity-from' },
                { key: 'quantityTo', inputId: '#filter-quantity-to' },
                { key: 'unitPriceFrom', inputId: '#filter-unitPrice-from' },
                { key: 'unitPriceTo', inputId: '#filter-unitPrice-to' },
                { key: 'totalPriceFrom', inputId: '#filter-totalPrice-from' },
                { key: 'totalPriceTo', inputId: '#filter-totalPrice-to' }
            ];

            purchaseNumberFields.forEach(field => {
                if (selectedFilters[field.key]) {
                    const input = filterForm.querySelector(field.inputId);
                    if (input) {
                        const value = Array.isArray(selectedFilters[field.key]) 
                            ? selectedFilters[field.key][0] 
                            : selectedFilters[field.key];
                        if (value) input.value = value;
                    }
                }
            });

            if (selectedFilters['paymentMethod']) {
                const paymentMethodSelect = filterForm.querySelector('#filter-paymentMethod');
                if (paymentMethodSelect) {
                    const value = Array.isArray(selectedFilters['paymentMethod']) 
                        ? selectedFilters['paymentMethod'][0] 
                        : selectedFilters['paymentMethod'];
                    if (value) paymentMethodSelect.value = value;
                }
            }

            setTimeout(() => {
                const userSelect = filterForm.querySelector('#filter-user');
                if (userSelect && !customSelects['filter-user'] && availableUsers && availableUsers.length > 0) {
                    const userData = availableUsers.map(u => ({
                        id: u.id,
                        name: u.name
                    }));
                    if (typeof createCustomSelect === 'function') {
                        const customSelect = createCustomSelect(userSelect, true);
                        if (customSelect) {
                            customSelects['filter-user'] = customSelect;
                            customSelect.populate(userData);
                            
                            if (selectedFilters['user']) {
                                const savedUsers = selectedFilters['user'];
                                if (Array.isArray(savedUsers) && savedUsers.length > 0) {
                                    const validUsers = savedUsers.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                                    if (validUsers.length > 0) {
                                        customSelect.setValue(validUsers);
                                    }
                                }
                            }
                        }
                    }
                }

                const sourceSelect = filterForm.querySelector('#filter-source');
                if (sourceSelect && !customSelects['filter-source'] && availableSources && availableSources.length > 0) {
                    const sourceData = availableSources.map(s => ({
                        id: s.id,
                        name: s.name
                    }));
                    if (typeof createCustomSelect === 'function') {
                        const customSelect = createCustomSelect(sourceSelect, true);
                        if (customSelect) {
                            customSelects['filter-source'] = customSelect;
                            customSelect.populate(sourceData);
                            
                            if (selectedFilters['source']) {
                                const savedSources = selectedFilters['source'];
                                if (Array.isArray(savedSources) && savedSources.length > 0) {
                                    const validSources = savedSources.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                                    if (validSources.length > 0) {
                                        customSelect.setValue(validSources);
                                    }
                                }
                            }
                        }
                    }
                }

                const productSelect = filterForm.querySelector('#filter-product');
                if (productSelect && !customSelects['filter-product'] && availableProducts && availableProducts.length > 0) {
                    const productData = availableProducts.map(p => ({
                        id: p.id,
                        name: p.name
                    }));
                    if (typeof createCustomSelect === 'function') {
                        const customSelect = createCustomSelect(productSelect, true);
                        if (customSelect) {
                            customSelects['filter-product'] = customSelect;
                            customSelect.populate(productData);
                            
                            if (selectedFilters['product']) {
                                const savedProducts = selectedFilters['product'];
                                if (Array.isArray(savedProducts) && savedProducts.length > 0) {
                                    const validProducts = savedProducts.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                                    if (validProducts.length > 0) {
                                        customSelect.setValue(validProducts);
                                    }
                                }
                            }
                        }
                    }
                }

                const currencySelect = filterForm.querySelector('#filter-currency');
                if (currencySelect && !customSelects['filter-currency'] && availableCurrencies && availableCurrencies.length > 0) {
                    const currencyData = availableCurrencies.map(c => ({
                        id: c.id,
                        name: c.name
                    }));
                    if (typeof createCustomSelect === 'function') {
                        const customSelect = createCustomSelect(currencySelect, true);
                        if (customSelect) {
                            customSelects['filter-currency'] = customSelect;
                            customSelect.populate(currencyData);
                            
                            if (selectedFilters['currency']) {
                                const savedCurrencies = selectedFilters['currency'];
                                if (Array.isArray(savedCurrencies) && savedCurrencies.length > 0) {
                                    const validCurrencies = savedCurrencies.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                                    if (validCurrencies.length > 0) {
                                        customSelect.setValue(validCurrencies);
                                    }
                                }
                            }
                        }
                    }
                }

                const clientSourceSelect = filterForm.querySelector('#filter-clientSource');
                if (clientSourceSelect && !customSelects['filter-clientSource'] && availableSources && availableSources.length > 0) {
                    const sourceData = availableSources.map(s => ({
                        id: s.id,
                        name: s.name
                    }));
                    if (typeof createCustomSelect === 'function') {
                        const customSelect = createCustomSelect(clientSourceSelect, true);
                        if (customSelect) {
                            customSelects['filter-clientSource'] = customSelect;
                            customSelect.populate(sourceData);
                            
                            if (selectedFilters['clientSource']) {
                                const savedSources = selectedFilters['clientSource'];
                                if (Array.isArray(savedSources) && savedSources.length > 0) {
                                    const validSources = savedSources.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                                    if (validSources.length > 0) {
                                        customSelect.setValue(validSources);
                                    }
                                }
                            }
                        }
                    }
                }

                if (filterableFields && filterableFields.length > 0) {
                    filterableFields.forEach(field => {
                        const filterId = `filter-${field.fieldName}`;
                        if (field.fieldType === 'DATE') {
                            const fromInput = filterForm.querySelector(`#${filterId}-from`);
                            const toInput = filterForm.querySelector(`#${filterId}-to`);
                            if (fromInput && selectedFilters[`${field.fieldName}From`]) {
                                const value = Array.isArray(selectedFilters[`${field.fieldName}From`]) 
                                    ? selectedFilters[`${field.fieldName}From`][0] 
                                    : selectedFilters[`${field.fieldName}From`];
                                if (value) fromInput.value = value;
                            }
                            if (toInput && selectedFilters[`${field.fieldName}To`]) {
                                const value = Array.isArray(selectedFilters[`${field.fieldName}To`]) 
                                    ? selectedFilters[`${field.fieldName}To`][0] 
                                    : selectedFilters[`${field.fieldName}To`];
                                if (value) toInput.value = value;
                            }
                        } else if (field.fieldType === 'NUMBER') {
                            const fromInput = filterForm.querySelector(`#${filterId}-from`);
                            const toInput = filterForm.querySelector(`#${filterId}-to`);
                            if (fromInput && selectedFilters[`${field.fieldName}From`]) {
                                const value = Array.isArray(selectedFilters[`${field.fieldName}From`]) 
                                    ? selectedFilters[`${field.fieldName}From`][0] 
                                    : selectedFilters[`${field.fieldName}From`];
                                if (value) fromInput.value = value;
                            }
                            if (toInput && selectedFilters[`${field.fieldName}To`]) {
                                const value = Array.isArray(selectedFilters[`${field.fieldName}To`]) 
                                    ? selectedFilters[`${field.fieldName}To`][0] 
                                    : selectedFilters[`${field.fieldName}To`];
                                if (value) toInput.value = value;
                            }
                        } else if (field.fieldType === 'LIST') {
                            if (customSelects[filterId] && selectedFilters[field.fieldName]) {
                                const savedValues = selectedFilters[field.fieldName];
                                if (Array.isArray(savedValues) && savedValues.length > 0) {
                                    const validValues = savedValues.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                                    if (validValues.length > 0) {
                                        customSelects[filterId].setValue(validValues);
                                    }
                                }
                            }
                        } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                            const input = filterForm.querySelector(`#${filterId}`);
                            if (input && selectedFilters[field.fieldName]) {
                                const value = Array.isArray(selectedFilters[field.fieldName]) 
                                    ? selectedFilters[field.fieldName][0] 
                                    : selectedFilters[field.fieldName];
                                if (value) input.value = value;
                            }
                        } else if (field.fieldType === 'BOOLEAN') {
                            const select = filterForm.querySelector(`#${filterId}`);
                            if (select && selectedFilters[field.fieldName]) {
                                const value = Array.isArray(selectedFilters[field.fieldName]) 
                                    ? selectedFilters[field.fieldName][0] 
                                    : selectedFilters[field.fieldName];
                                if (value) select.value = value;
                            }
                        }
                    });
                }
            }, 100);
        }
    } catch (error) {
        console.error('Error loading entities:', error);
    }
}


function getUserAuthorities() {
    const authoritiesStr = localStorage.getItem('userAuthorities');
    if (!authoritiesStr) return [];
    try {
        return JSON.parse(authoritiesStr);
    } catch (e) {
        return [];
    }
}

function canEditStrangers() {
    return getUserAuthorities().includes('client_stranger:edit');
}

function isOwnClient(client) {
    const currentUserId = localStorage.getItem('userId');
    if (!currentUserId || !client.sourceId) return false;
    const userSource = availableSources.find(source => {
        const sourceUserId = source.userId !== null && source.userId !== undefined 
            ? String(source.userId) 
            : null;
        return sourceUserId === currentUserId && source.id === client.sourceId;
    });
    return !!userSource;
}

function canEditClient(client) {
    if (canEditStrangers()) {
        return true;
    }
    
    return isOwnClient(client);
}

function canEditCompany(client) {
    if (canEditStrangers()) {
        return true;
    }
    return isOwnClient(client);
}

function canEditSource() {
    return canEditStrangers();
}

function canDeleteClient(client) {
    if (canEditStrangers()) {
        return true;
    }
    
    return isOwnClient(client);
}

function formatFieldValueForModal(fieldValue, field) {
    if (!fieldValue) return '';
    
    switch (field.fieldType) {
        case 'TEXT':
            return escapeHtml(fieldValue.valueText || '');
        case 'PHONE':
            const phone = fieldValue.valueText || '';
            if (phone) {
                const escapedPhone = escapeHtml(phone);
                return `<a href="tel:${escapedPhone}">${escapedPhone}</a>`;
            }
            return '';
        case 'NUMBER':
            return escapeHtml(String(fieldValue.valueNumber || ''));
        case 'DATE':
            return escapeHtml(fieldValue.valueDate || '');
        case 'BOOLEAN':
            if (fieldValue.valueBoolean === true) return 'Так';
            if (fieldValue.valueBoolean === false) return 'Ні';
            return '';
        case 'LIST':
            return escapeHtml(fieldValue.valueListValue || '');
        default:
            return '';
    }
}

async function loadClientFieldValues(clientId) {
    try {
        const response = await fetch(`/api/v1/client/${clientId}/field-values`);
        if (!response.ok) return [];
        return await response.json();
    } catch (error) {
        console.error('Error loading field values:', error);
        return [];
    }
}

let editing = false;

function loadClientDetails(client) {
    showClientModal(client);
}

async function showClientModal(client) {
    document.getElementById('client-modal').setAttribute('data-client-id', client.id);

    document.getElementById('modal-client-id').innerText = client.id;
    
    const modalContent = document.querySelector('.modal-content-client');
    const existingFields = modalContent.querySelectorAll('p[data-field-id]');
    existingFields.forEach(el => el.remove());
    
    const nameFieldLabel = currentClientType ? currentClientType.nameFieldLabel : 'Компанія';
    document.getElementById('modal-client-company').parentElement.querySelector('strong').textContent = nameFieldLabel + ':';
    document.getElementById('modal-client-company').innerText = client.company;
    
    if (currentClientTypeId && clientTypeFields.length > 0) {
        let fieldValues = client._fieldValues;
        if (!fieldValues) {
            fieldValues = await loadClientFieldValues(client.id);
        }
        const fieldValuesMap = new Map();
        fieldValues.forEach(fv => {
            if (!fieldValuesMap.has(fv.fieldId)) {
                fieldValuesMap.set(fv.fieldId, []);
            }
            fieldValuesMap.get(fv.fieldId).push(fv);
        });
        
        const companyP = document.getElementById('modal-client-company').parentElement;
        
        clientTypeFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        
        let lastInsertedElement = companyP;
        clientTypeFields.forEach(field => {
            const values = fieldValuesMap.get(field.id) || [];
            const fieldP = document.createElement('p');
            fieldP.setAttribute('data-field-id', field.id);
            
            let fieldValue = '';
            if (values.length > 0) {
                if (field.allowMultiple) {
                    fieldValue = values.map(v => formatFieldValueForModal(v, field)).join('<br>');
    } else {
                    fieldValue = formatFieldValueForModal(values[0], field);
                }
            }
            
            const canEdit = canEditClient(client);
            
            const strong = document.createElement('strong');
            strong.textContent = field.fieldLabel + ':';
            fieldP.appendChild(strong);
            
            const valueSpan = document.createElement('span');
            valueSpan.id = `modal-field-${field.id}`;
            if (!fieldValue) {
                valueSpan.classList.add('empty-value');
            }
            if (fieldValue) {
                valueSpan.innerHTML = fieldValue;
            } else {
                valueSpan.textContent = '—';
            }
            fieldP.appendChild(valueSpan);
            
            if (canEdit) {
                const editBtn = document.createElement('button');
                editBtn.className = 'edit-icon';
                editBtn.setAttribute('data-field-id', field.id);
                editBtn.setAttribute('title', 'Редагувати');
                editBtn.setAttribute('onclick', `enableEditField(${field.id}, '${field.fieldType}', ${field.allowMultiple || false})`);
                editBtn.innerHTML = `
                    <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
                    </svg>
                `;
                fieldP.appendChild(editBtn);
            }
            fieldP.setAttribute('data-field-type', field.fieldType);

            lastInsertedElement.insertAdjacentElement('afterend', fieldP);
            lastInsertedElement = fieldP;
        });

        const sourceElement = document.getElementById('modal-client-source')?.parentElement;
        if (sourceElement) {
            sourceElement.style.display = '';
    document.getElementById('modal-client-source').innerText =
        findNameByIdFromMap(sourceMap, client.sourceId);
        }

        canEditClient(client);
        const canEditSourceField = canEditSource();
        const canEditCompanyField = canEditCompany(client);

        const companyEditButton = document.querySelector(`button.edit-icon[onclick*="enableEdit('company')"]`);
        if (companyEditButton) {
            if (!canEditCompanyField) {
                companyEditButton.style.display = 'none';
            } else {
                companyEditButton.style.display = '';
            }
        }

        const sourceEditButton = document.getElementById('edit-source');
        if (sourceEditButton) {
            if (!canEditSourceField) {
                sourceEditButton.style.display = 'none';
            } else {
                sourceEditButton.style.display = '';
            }
        }
    } else {
        const sourceElement = document.getElementById('modal-client-source')?.parentElement;
        if (sourceElement) {
            sourceElement.style.display = '';
            document.getElementById('modal-client-source').innerText =
                findNameByIdFromMap(sourceMap, client.sourceId);
        }

        canEditClient(client);
        const canEditSourceField = canEditSource();
        const canEditCompanyField = canEditCompany(client);

        const companyEditButton = document.querySelector(`button.edit-icon[onclick*="enableEdit('company')"]`);
        if (companyEditButton) {
            if (!canEditCompanyField) {
                companyEditButton.style.display = 'none';
            } else {
                companyEditButton.style.display = '';
            }
        }

        const sourceEditButton = document.getElementById('edit-source');
        if (sourceEditButton) {
            if (!canEditSourceField) {
                sourceEditButton.style.display = 'none';
            } else {
                sourceEditButton.style.display = '';
            }
        }
    }
    
    document.getElementById('modal-client-created').innerText = client.createdAt || '';
    document.getElementById('modal-client-updated').innerText = client.updatedAt || '';

    const modal = document.getElementById('client-modal');
    modal.style.display = 'flex';
    setTimeout(() => {
        modal.classList.add('open');
    }, 10);

    const closeModalBtn = document.getElementById('close-modal-client');
    if (closeModalBtn) {
        if (closeModalBtn._closeHandler) {
            closeModalBtn.removeEventListener('click', closeModalBtn._closeHandler);
        }
        const handleClose = () => {
            if (!editing) {
                modal.classList.remove('open');
                setTimeout(() => {
                    closeModal();
                });
            } else {
                showMessage('Збережіть або відмініть зміни', 'error');
            }
        };
        closeModalBtn._closeHandler = handleClose;
        closeModalBtn.addEventListener('click', handleClose);
    }

    // Removed: modal click handler to prevent closing on outside click
    // if (modal._modalClickHandler) {
    //     modal.removeEventListener('click', modal._modalClickHandler);
    // }
    // const handleModalClick = function (event) {
    //     if (event.target === modal) {
    //         if (!editing) {
    //             closeModal();
    //         } else {
    //             showMessage('Збережіть або відмініть зміни', 'error');
    //         }
    //     }
    // };
    // modal._modalClickHandler = handleModalClick;
    // modal.addEventListener('click', handleModalClick);

    const fullDeleteButton = document.getElementById('full-delete-client');
    if (fullDeleteButton) {
        const canDelete = canDeleteClient(client);

        if (fullDeleteButton.style.display !== 'none' && !canDelete) {
            fullDeleteButton.style.display = 'none';
        }
    }
    fullDeleteButton.onclick = async () => {
        if (!confirm('Ви впевнені, що хочете повністю видалити цього клієнта з бази даних? Ця дія незворотна!')) {
            return;
        }
        
        loaderBackdrop.style.display = 'flex';
        try {
            const response = await fetch(`${API_URL_PURCHASE}/../client/${client.id}`, {method: 'DELETE'});
            if (!response.ok) {
                const errorData = await response.json();
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                return;
            }
            showMessage('Клієнт повністю видалений з бази даних', 'info');
            modal.style.display = 'none';

            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        } catch (error) {
            console.error('Помилка видалення клієнта:', error);
            handleError(error);
        } finally {
            loaderBackdrop.style.display = 'none';
        }
    };


    const deleteButton = document.getElementById('delete-client');
    const restoreButton = document.getElementById('restore-client');

    const canDelete = canDeleteClient(client);

    if (client.isActive === false) {
        if (deleteButton) deleteButton.style.display = 'none';
        if (restoreButton) restoreButton.style.display = 'block';
    } else {
        if (deleteButton) {
            deleteButton.style.display = canDelete ? 'block' : 'none';
        }
        if (restoreButton) restoreButton.style.display = 'none';
    }
    
    deleteButton.onclick = async () => {
        if (!confirm('Ви впевнені, що хочете деактивувати цього клієнта? Клієнт буде прихований, але залишиться в базі даних.')) {
            return;
        }
        
        loaderBackdrop.style.display = 'flex';
        try {
            const response = await fetch(`${API_URL_PURCHASE}/../client/active/${client.id}`, {method: 'DELETE'});
            if (!response.ok) {
                const errorData = await response.json();
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                return;
            }
            showMessage('Клієнт деактивовано (isActive = false)', 'info');
            modal.style.display = 'none';

            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        } catch (error) {
            console.error('Помилка деактивації клієнта:', error);
            handleError(error);
        } finally {
            loaderBackdrop.style.display = 'none';
        }
    };

    if (restoreButton) {
        restoreButton.onclick = async () => {
            if (!confirm('Ви впевнені, що хочете відновити цього клієнта? Клієнт знову стане активним.')) {
                return;
            }
            
            loaderBackdrop.style.display = 'flex';
            try {
                const response = await fetch(`${API_URL_PURCHASE}/../client/active/${client.id}`, {method: 'PATCH'});
                if (!response.ok) {
                    const errorData = await response.json();
                    handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                    return;
                }
                showMessage('Клієнт відновлено (isActive = true)', 'info');
                modal.style.display = 'none';

                loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
            } catch (error) {
                console.error('Помилка відновлення клієнта:', error);
                handleError(error);
            } finally {
                loaderBackdrop.style.display = 'none';
            }
        };
    }

    attachPurchasesButtonHandler();
    attachContainersButtonHandler();
}

function attachPurchasesButtonHandler() {
    const showPurchasesButton = document.getElementById('show-purchases-client');
    if (!showPurchasesButton) {
        console.warn('show-purchases-client button not found');
        return;
    }
    
    if (showPurchasesButton._purchasesHandler) {
        showPurchasesButton.removeEventListener('click', showPurchasesButton._purchasesHandler);
    }
    const purchasesHandler = async (e) => {
        e.preventDefault();
        e.stopPropagation();
        
        const clientModal = document.getElementById('client-modal');
        const clientId = clientModal ? clientModal.getAttribute('data-client-id') : null;
        
        if (!clientId) {
            console.error('Client ID not found');
            return;
        }

        const modal = document.getElementById('purchaseClientModal');
        const tableBody = document.querySelector('#purchaseTable tbody');
        const clientIdElement = document.getElementById('client-id-purchase');
        
        if (!modal || !tableBody || !clientIdElement) {
            console.error('Required elements not found');
            return;
        }

        modal.style.display = 'flex';
        tableBody.textContent = '';
        const loadingRow = document.createElement('tr');
        const loadingCell = document.createElement('td');
        loadingCell.colSpan = 9;
        loadingCell.style.textAlign = 'center';
        loadingCell.style.padding = '2em';
        loadingCell.textContent = 'Завантаження...';
        loadingRow.appendChild(loadingCell);
        tableBody.appendChild(loadingRow);
        clientIdElement.textContent = `Клієнт: ${clientId}`;

        setTimeout(() => {
            modal.classList.add('show');
        }, 10);

        attachPurchaseModalHandlers();

        const currentUserMap = userMap || (typeof window.userMap !== 'undefined' ? window.userMap : new Map());
        const currentSourceMap = sourceMap || (typeof window.sourceMap !== 'undefined' ? window.sourceMap : new Map());
        const currentProductMap = productMap || (typeof window.productMap !== 'undefined' ? window.productMap : new Map());
        
        if (currentUserMap.size === 0 || currentSourceMap.size === 0 || currentProductMap.size === 0) {
            try {
                const entitiesResponse = await fetch('/api/v1/entities');
                if (entitiesResponse.ok) {
                    const entitiesData = await entitiesResponse.json();
                    if (entitiesData.users && entitiesData.users.length > 0 && currentUserMap.size === 0) {
                        entitiesData.users.forEach(user => {
                            currentUserMap.set(user.id, user.name || '');
                        });
                    }
                    if (entitiesData.sources && entitiesData.sources.length > 0 && currentSourceMap.size === 0) {
                        entitiesData.sources.forEach(source => {
                            currentSourceMap.set(source.id, source.name || '');
                        });
                    }
                    if (entitiesData.products && entitiesData.products.length > 0 && currentProductMap.size === 0) {
                        entitiesData.products.forEach(product => {
                            currentProductMap.set(product.id, product.name || '');
                        });
                    }
                }
            } catch (e) {
                console.warn('Failed to load entities:', e);
            }
        }

        try {
            const response = await fetch(`/api/v1/purchase/client/${clientId}`);
            
            if (!response.ok) {
                throw new Error(`Failed to load purchases: ${response.status} ${response.statusText}`);
            }
            
            const data = await response.json();
            tableBody.textContent = '';

            if (!data || data.length === 0) {
                const emptyRow = document.createElement('tr');
                const emptyCell = document.createElement('td');
                emptyCell.colSpan = 9;
                emptyCell.style.textAlign = 'center';
                emptyCell.style.padding = '2em';
                emptyCell.style.color = '#999';
                emptyCell.textContent = 'Немає закупівель';
                emptyRow.appendChild(emptyCell);
                tableBody.appendChild(emptyRow);
                return;
            }

            const userMapToUse = currentUserMap;
            const sourceMapToUse = currentSourceMap;
            const productMapToUse = currentProductMap;

            data.forEach(purchase => {
                const row = document.createElement('tr');
                
                const userCell = document.createElement('td');
                userCell.setAttribute('data-label', 'Водій');
                userCell.textContent = findNameByIdFromMap(userMapToUse, purchase.userId) || '-';
                row.appendChild(userCell);
                
                const sourceCell = document.createElement('td');
                sourceCell.setAttribute('data-label', 'Залучення');
                sourceCell.textContent = findNameByIdFromMap(sourceMapToUse, purchase.sourceId) || '-';
                row.appendChild(sourceCell);
                
                const productCell = document.createElement('td');
                productCell.setAttribute('data-label', 'Товар');
                productCell.textContent = findNameByIdFromMap(productMapToUse, purchase.productId) || '-';
                row.appendChild(productCell);
                
                const quantityCell = document.createElement('td');
                quantityCell.setAttribute('data-label', 'Кількість');
                quantityCell.textContent = purchase.quantity || '-';
                row.appendChild(quantityCell);
                
                const unitPriceCell = document.createElement('td');
                unitPriceCell.setAttribute('data-label', 'Ціна');
                unitPriceCell.textContent = purchase.unitPrice || '-';
                row.appendChild(unitPriceCell);
                
                const totalPriceCell = document.createElement('td');
                totalPriceCell.setAttribute('data-label', 'Сплачено');
                totalPriceCell.textContent = purchase.totalPrice || '-';
                row.appendChild(totalPriceCell);
                
                const paymentMethodCell = document.createElement('td');
                paymentMethodCell.setAttribute('data-label', 'Форма');
                paymentMethodCell.textContent = purchase.paymentMethod || '-';
                row.appendChild(paymentMethodCell);
                
                const currencyCell = document.createElement('td');
                currencyCell.setAttribute('data-label', 'Валюта');
                currencyCell.textContent = purchase.currency || '-';
                row.appendChild(currencyCell);
                
                const createdAtCell = document.createElement('td');
                createdAtCell.setAttribute('data-label', 'Дата');
                createdAtCell.textContent = purchase.createdAt || '-';
                row.appendChild(createdAtCell);
                
                tableBody.appendChild(row);
            });
        } catch (error) {
            console.error('Error loading purchases:', error);
            tableBody.textContent = '';
            const errorRow = document.createElement('tr');
            const errorCell = document.createElement('td');
            errorCell.colSpan = 9;
            errorCell.style.textAlign = 'center';
            errorCell.style.padding = '2em';
            errorCell.style.color = '#d32f2f';
            errorCell.textContent = `Помилка завантаження закупівель: ${error.message}`;
            errorRow.appendChild(errorCell);
            tableBody.appendChild(errorRow);
        }
    };
    showPurchasesButton._purchasesHandler = purchasesHandler;
    showPurchasesButton.addEventListener('click', purchasesHandler);
}

function attachPurchaseModalHandlers() {
    const closePurchaseModalBtn = document.getElementById('closePurchaseModal');
    if (closePurchaseModalBtn) {
        if (closePurchaseModalBtn._closePurchaseHandler) {
            closePurchaseModalBtn.removeEventListener('click', closePurchaseModalBtn._closePurchaseHandler);
        }
        const closePurchaseHandler = () => {
            const modal = document.getElementById('purchaseClientModal');
            if (modal) {
                modal.classList.remove('show');
                setTimeout(() => {
                    modal.style.display = 'none';
                }, 300);
            }
        };
        closePurchaseModalBtn._closePurchaseHandler = closePurchaseHandler;
        closePurchaseModalBtn.addEventListener('click', closePurchaseHandler);
    }

    const purchaseClientModal = document.getElementById('purchaseClientModal');
    if (purchaseClientModal) {
        if (purchaseClientModal._purchaseModalClickHandler) {
            purchaseClientModal.removeEventListener('click', purchaseClientModal._purchaseModalClickHandler);
        }
        const purchaseModalClickHandler = (event) => {
            const modalContent = document.querySelector('.modal-purchase-client-content');
            if (modalContent && !modalContent.contains(event.target)) {
                purchaseClientModal.classList.remove('show');
                setTimeout(() => {
                    purchaseClientModal.style.display = 'none';
                }, 300);
            }
        };
        purchaseClientModal._purchaseModalClickHandler = purchaseModalClickHandler;
        purchaseClientModal.addEventListener('click', purchaseModalClickHandler);
    }
}

function attachContainersButtonHandler() {
    const showContainersButton = document.getElementById('show-containers-client');
    if (!showContainersButton) {
        console.warn('show-containers-client button not found');
        return;
    }
    
    if (showContainersButton._containersHandler) {
        showContainersButton.removeEventListener('click', showContainersButton._containersHandler);
    }
    const containersHandler = async (e) => {
        e.preventDefault();
        e.stopPropagation();
        
        const clientModal = document.getElementById('client-modal');
        const clientId = clientModal ? clientModal.getAttribute('data-client-id') : null;
        
        if (!clientId) {
            console.error('Client ID not found');
            return;
        }

        const modal = document.getElementById('containerClientModal');
        const tableBody = document.querySelector('#containerTable tbody');
        const clientIdElement = document.getElementById('client-id-container');
        
        if (!modal || !tableBody || !clientIdElement) {
            console.error('Required elements not found');
            return;
        }

        modal.style.display = 'flex';
        tableBody.textContent = '';
        const loadingRow = document.createElement('tr');
        const loadingCell = document.createElement('td');
        loadingCell.colSpan = 4;
        loadingCell.style.textAlign = 'center';
        loadingCell.style.padding = '2em';
        loadingCell.textContent = 'Завантаження...';
        loadingRow.appendChild(loadingCell);
        tableBody.appendChild(loadingRow);
        clientIdElement.textContent = `Клієнт: ${clientId}`;

        setTimeout(() => {
            modal.classList.add('show');
        }, 10);

        attachContainerModalHandlers();

        const currentUserMap = userMap || (typeof window.userMap !== 'undefined' ? window.userMap : new Map());
        
        if (currentUserMap.size === 0) {
            try {
                const entitiesResponse = await fetch('/api/v1/entities');
                if (entitiesResponse.ok) {
                    const entitiesData = await entitiesResponse.json();
                    if (entitiesData.users && entitiesData.users.length > 0) {
                        entitiesData.users.forEach(user => {
                            currentUserMap.set(user.id, user.name || '');
                        });
                    }
                }
            } catch (e) {
                console.warn('Failed to load entities:', e);
            }
        }

        try {
            const response = await fetch(`/api/v1/containers/client/${clientId}`);
            
            if (!response.ok) {
                throw new Error(`Failed to load containers: ${response.status} ${response.statusText}`);
            }
            
            const data = await response.json();
            tableBody.textContent = '';

            if (!data || data.length === 0) {
                const emptyRow = document.createElement('tr');
                const emptyCell = document.createElement('td');
                emptyCell.colSpan = 4;
                emptyCell.style.textAlign = 'center';
                emptyCell.style.padding = '2em';
                emptyCell.style.color = '#999';
                emptyCell.textContent = 'Немає тар';
                emptyRow.appendChild(emptyCell);
                tableBody.appendChild(emptyRow);
                return;
            }

            const userMapToUse = currentUserMap;

            data.forEach(container => {
                const row = document.createElement('tr');
                
                const quantityCell = document.createElement('td');
                quantityCell.setAttribute('data-label', 'Кількість');
                quantityCell.textContent = container.quantity || '-';
                row.appendChild(quantityCell);
                
                const nameCell = document.createElement('td');
                nameCell.setAttribute('data-label', 'Тип тари');
                nameCell.textContent = container.containerName || '-';
                row.appendChild(nameCell);
                
                const userCell = document.createElement('td');
                userCell.setAttribute('data-label', 'Власник');
                userCell.textContent = findNameByIdFromMap(userMapToUse, container.userId) || '-';
                row.appendChild(userCell);
                
                const dateCell = document.createElement('td');
                dateCell.setAttribute('data-label', 'Оновлено');
                dateCell.textContent = container.updatedAt || container.createdAt || '-';
                row.appendChild(dateCell);
                
                tableBody.appendChild(row);
            });
        } catch (error) {
            console.error('Error loading containers:', error);
            tableBody.textContent = '';
            const errorRow = document.createElement('tr');
            const errorCell = document.createElement('td');
            errorCell.colSpan = 4;
            errorCell.style.textAlign = 'center';
            errorCell.style.padding = '2em';
            errorCell.style.color = '#d32f2f';
            errorCell.textContent = `Помилка завантаження тар: ${error.message}`;
            errorRow.appendChild(errorCell);
            tableBody.appendChild(errorRow);
        }
    };
    showContainersButton._containersHandler = containersHandler;
    showContainersButton.addEventListener('click', containersHandler);
}

function attachContainerModalHandlers() {
    const closeContainerModalBtn = document.getElementById('closeContainerModal');
    if (closeContainerModalBtn) {
        if (closeContainerModalBtn._closeContainerHandler) {
            closeContainerModalBtn.removeEventListener('click', closeContainerModalBtn._closeContainerHandler);
        }
        const closeContainerHandler = () => {
            const modal = document.getElementById('containerClientModal');
            if (modal) {
                modal.classList.remove('show');
                setTimeout(() => {
                    modal.style.display = 'none';
                }, 300);
            }
        };
        closeContainerModalBtn._closeContainerHandler = closeContainerHandler;
        closeContainerModalBtn.addEventListener('click', closeContainerHandler);
    }

    const containerClientModal = document.getElementById('containerClientModal');
    if (containerClientModal) {
        if (containerClientModal._containerModalClickHandler) {
            containerClientModal.removeEventListener('click', containerClientModal._containerModalClickHandler);
        }
        const containerModalClickHandler = (event) => {
            const modalContent = document.querySelector('.modal-container-client-content');
            if (modalContent && !modalContent.contains(event.target)) {
                containerClientModal.classList.remove('show');
                setTimeout(() => {
                    containerClientModal.style.display = 'none';
                }, 300);
            }
        };
        containerClientModal._containerModalClickHandler = containerModalClickHandler;
        containerClientModal.addEventListener('click', containerModalClickHandler);
    }
}





/*--search--*/

function debounce(func, delay) {
    let timeoutId;
    return function(...args) {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => func.apply(this, args), delay);
    };
}

const performSearch = async () => {
    const searchTerm = searchInput.value;
    localStorage.setItem('searchTerm', searchTerm);
    loadDataWithSort(0, pageSize, currentSort, currentDirection);
};

const debouncedSearch = debounce(performSearch, 400);

if (searchInput) {
    searchInput.addEventListener('keypress', async (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            performSearch();
        } else {
            debouncedSearch();
        }
    });

    searchInput.addEventListener('input', () => {
        debouncedSearch();
    });
}

/*--filter--*/

const filterButton = document.querySelector('.filter-button-block');
const filterModal = document.getElementById('filterModal');
const closeFilter = document.querySelector('.close-filter');
let modalContent = null;

if (filterModal) {
    modalContent = filterModal.querySelector('.modal-content-filter');
}

if (filterButton && filterModal) {
    filterButton.addEventListener('click', () => {
        filterModal.style.display = 'block';
        setTimeout(() => {
            filterModal.classList.add('show');
        }, 10);
    });

    if (closeFilter) {
        closeFilter.addEventListener('click', () => {
            closeModalFilter();
        });
    }

    if (modalContent) {
        // Removed: filter modal click handler to prevent closing on outside click
        // filterModal.addEventListener('click', (event) => {
        //     if (!modalContent.contains(event.target)) {
        //         closeModalFilter();
        //     }
        // });
    }
}

function closeModalFilter() {
    if (!filterModal) return;
    
    filterModal.classList.add('closing');
    if (modalContent) {
        modalContent.classList.add('closing-content');
    }

    setTimeout(() => {
        filterModal.style.display = 'none';
        filterModal.classList.remove('closing');
        if (modalContent) {
            modalContent.classList.remove('closing-content');
        }
    }, 200);
}


const modalFilterButtonSubmit = document.getElementById("modal-filter-button-submit");
if (modalFilterButtonSubmit) {
    modalFilterButtonSubmit.addEventListener('click', (event) => {
        event.preventDefault();
        updateSelectedFilters();
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);

        closeModalFilter();
    });
}


function updateSelectedFilters() {
    Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);

    Object.keys(customSelects).forEach(selectId => {
        if (selectId.startsWith('filter-')) {
            const select = document.getElementById(selectId);
            if (select) {
                const name = select.name;
                const values = customSelects[selectId].getValue();
                if (values.length > 0) {
                    selectedFilters[name] = values;
                }
            }
        }
    });

    const filterForm = document.getElementById('filterForm');
    if (!filterForm) return;
    
    const formData = new FormData(filterForm);

    const purchaseFilters = [
        'createdAtFrom', 'createdAtTo',
        'quantityFrom', 'quantityTo',
        'totalPriceFrom', 'totalPriceTo',
        'unitPriceFrom', 'unitPriceTo',
        'paymentMethod'
    ];

    purchaseFilters.forEach(field => {
        const value = formData.get(field);
        if (value && value.trim() !== '') {
            selectedFilters[field] = [value];
        }
    });

    const clientCreatedAtFrom = formData.get('clientCreatedAtFrom');
    const clientCreatedAtTo = formData.get('clientCreatedAtTo');
    if (clientCreatedAtFrom && clientCreatedAtFrom.trim() !== '') {
        selectedFilters['clientCreatedAtFrom'] = [clientCreatedAtFrom];
    }
    if (clientCreatedAtTo && clientCreatedAtTo.trim() !== '') {
        selectedFilters['clientCreatedAtTo'] = [clientCreatedAtTo];
    }

    const clientUpdatedAtFrom = formData.get('clientUpdatedAtFrom');
    const clientUpdatedAtTo = formData.get('clientUpdatedAtTo');
    if (clientUpdatedAtFrom && clientUpdatedAtFrom.trim() !== '') {
        selectedFilters['clientUpdatedAtFrom'] = [clientUpdatedAtFrom];
    }
    if (clientUpdatedAtTo && clientUpdatedAtTo.trim() !== '') {
        selectedFilters['clientUpdatedAtTo'] = [clientUpdatedAtTo];
    }

    const clientSourceSelect = filterForm.querySelector('#filter-clientSource');
    if (clientSourceSelect && customSelects['filter-clientSource']) {
        const selectedSources = customSelects['filter-clientSource'].getValue();
        const filteredSources = selectedSources.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
        if (filteredSources.length > 0) {
            selectedFilters['clientSource'] = filteredSources;
        }
    }

    if (filterableFields && filterableFields.length > 0) {
        filterableFields.forEach(field => {
            if (field.fieldType === 'DATE') {
                const fromValue = formData.get(`${field.fieldName}From`);
                const toValue = formData.get(`${field.fieldName}To`);
                if (fromValue && fromValue.trim() !== '') {
                    selectedFilters[`${field.fieldName}From`] = [fromValue];
                }
                if (toValue && toValue.trim() !== '') {
                    selectedFilters[`${field.fieldName}To`] = [toValue];
                }
            } else if (field.fieldType === 'NUMBER') {
                const fromValue = formData.get(`${field.fieldName}From`);
                const toValue = formData.get(`${field.fieldName}To`);
                if (fromValue && fromValue.trim() !== '' && !isNaN(fromValue)) {
                    selectedFilters[`${field.fieldName}From`] = [fromValue];
                }
                if (toValue && toValue.trim() !== '' && !isNaN(toValue)) {
                    selectedFilters[`${field.fieldName}To`] = [toValue];
                }
            } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                const value = formData.get(field.fieldName);
                if (value && value.trim() !== '') {
                    selectedFilters[field.fieldName] = [value];
                }
            } else if (field.fieldType === 'BOOLEAN') {
                const value = formData.get(field.fieldName);
                if (value && value.trim() !== '') {
                    selectedFilters[field.fieldName] = [value];
                }
            }
        });
    }

    localStorage.setItem('selectedFilters', JSON.stringify(selectedFilters));
    updateFilterCounter();
}

function updateFilterCounter() {
    const counterElement = document.getElementById('filter-counter');
    const countElement = document.getElementById('filter-count');

    if (!counterElement || !countElement) return;

    let totalFilters = 0;

    totalFilters += Object.values(selectedFilters)
        .filter(value => Array.isArray(value))
        .reduce((count, values) => count + values.length, 0);

    totalFilters += Object.keys(selectedFilters)
        .filter(key => !Array.isArray(selectedFilters[key]) && selectedFilters[key] !== '')
        .length;

    if (totalFilters > 0) {
        countElement.textContent = totalFilters;
        counterElement.style.display = 'inline-flex';
    } else {
        counterElement.style.display = 'none';
    }
}


const filterCounter = document.getElementById('filter-counter');
if (filterCounter) {
    filterCounter.addEventListener('click', () => {
        clearFilters();
    });
}

function clearFilters() {
    Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);

    const filterForm = document.getElementById('filterForm');
    if (filterForm) {
        filterForm.reset();
        Object.keys(customSelects).forEach(selectId => {
            if (selectId.startsWith('filter-')) {
                if (customSelects[selectId] && typeof customSelects[selectId].reset === 'function') {
                    customSelects[selectId].reset();
                }
            }
        });
    }

    if (searchInput) {
        searchInput.value = '';
    }

    localStorage.removeItem('selectedFilters');
    localStorage.removeItem('searchTerm');

    updateFilterCounter();
    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
}

document.addEventListener('DOMContentLoaded', async () => {
    const urlParams = new URLSearchParams(window.location.search);
    const typeId = urlParams.get('type');
    
    if (!typeId) {
        await showClientTypeSelectionModal();
        return;
    }
    
    currentClientTypeId = parseInt(typeId);
    await updateNavigationWithCurrentType(currentClientTypeId);
    await loadClientType(currentClientTypeId);
    await loadClientTypeFields(currentClientTypeId);
    buildPurchaseTable();
    buildDynamicFilters();
    
    if (typeof initColumnResizerForPurchase === 'function' && currentClientTypeId) {
        setTimeout(() => {
            const storageKey = `purchase_${currentClientTypeId}`;
            initColumnResizerForPurchase('client-list', storageKey);
            if (typeof applyColumnWidthsForPurchase === 'function') {
                applyColumnWidthsForPurchase('client-list', storageKey);
            }
        }, 0);
    }

    const savedFilters = localStorage.getItem('selectedFilters');
    let parsedFilters;
    if (savedFilters) {
        try {
            parsedFilters = JSON.parse(savedFilters);
        } catch (e) {
            console.error('Invalid selectedFilters in localStorage:', e);
            parsedFilters = {};
        }
    } else {
        parsedFilters = {};
    }
    Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);
    Object.assign(selectedFilters, parsedFilters);

    const savedSearchTerm = localStorage.getItem('searchTerm');
    if (savedSearchTerm && searchInput) {
        searchInput.value = savedSearchTerm;
    }

    await loadEntitiesAndApplyFilters();
    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
    
    // Инициализация экспорта в Excel
    if (typeof initExcelExportPurchase === 'function') {
        initExcelExportPurchase({
            triggerId: 'exportToExcelData',
            modalId: 'exportModalData',
            cancelId: 'exportCancel',
            confirmId: 'exportConfirm',
            formId: 'exportFieldsForm',
            searchInputId: 'inputSearch',
            apiPath: API_URL_PURCHASE
        });
    }
});



