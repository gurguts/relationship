const API_URL_SALE = '/api/v1/sale';

const prevPageButton = document.getElementById('prev-btn');
const nextPageButton = document.getElementById('next-btn');
const paginationInfo = document.getElementById('pagination-info');
const allSaleInfo = document.getElementById('all-sale-info');
const loaderBackdrop = document.getElementById('loader-backdrop');
const filterForm = document.getElementById('filterForm');
let currentSort = 'createdAt';
let currentDirection = 'DESC';
let currentPage = 0;
let pageSize = 50;
let editing = false;
const selectedFilters = {};
const customSelects = {};

let availableStatuses = [];
let availableRegions = [];
let availableSources = [];
let availableRoutes = [];
let availableBusiness = [];
let availableUsers = [];
let availableProducts = [];
let availableClientProducts = [];

let statusMap;
let regionMap;
let routeMap;
let businessMap;
let clientProductMap;
let sourceMap;
let userMap;
let productMap;

const paymentMethodMap = [
    {id: "1", name: "1"},
    {id: "2", name: "2"}
];
const currencyTypes = [
    {id: "UAH", name: "UAH"},
    {id: "USD", name: "USD"},
    {id: "EUR", name: "EUR"}
];
let availablePaymentMethods = paymentMethodMap;
let availableCurrencies = currencyTypes;
new Map(paymentMethodMap.map(item => [item.id, item.name]));
new Map(currencyTypes.map(item => [item.id, item.name]));
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

function updatePagination(totalOil, oilOnPage, totalPages, currentPageIndex) {
    allSaleInfo.textContent = `
    Продажів: ${totalOil}
    `
    paginationInfo.textContent = `
        Закупок на сторінці: ${oilOnPage},
        Всього сторінок: ${totalPages},
        Поточна сторінка: ${currentPageIndex + 1}
    `;

    prevPageButton.disabled = currentPageIndex <= 0;
    nextPageButton.disabled = currentPageIndex >= totalPages - 1;
}

const userRole = localStorage.getItem('userRole');

function renderSale(sales) {
    const tbodySale = document.getElementById('sale-table-body');
    tbodySale.innerHTML = '';

    sales.forEach(sale => {
        const row = document.createElement('tr');
        row.classList.add('sale-row');
        row.dataset.id = sale.id;

        row.innerHTML = getRowHtml(sale);
        tbodySale.appendChild(row);

        const editButton = row.querySelector('.edit-button');
        const deleteButton = row.querySelector('.delete-button');
        const companyCell = row.querySelector('.company-cell');

        editButton.addEventListener('click', () => showEditModal(sale));
        deleteButton.addEventListener('click', () => deleteSale(sale.id));
        companyCell.addEventListener('click', () => loadClientDetails(sale.client));
    });
}

document.querySelectorAll('th[data-sort]').forEach(th => {
    th.addEventListener('click', () => {
        const sortField = th.getAttribute('data-sort');

        if (currentSort === sortField) {
            currentDirection = currentDirection === 'ASC' ? 'DESC' : 'ASC';
        } else {
            currentSort = sortField;
            currentDirection = 'ASC';
        }

        loadDataWithSort(0, 100, currentSort, currentDirection);
    });
});

function getRowHtml(sale) {
    return `
        <td data-label="Компанія" class="company-cell">${sale.client.company}</td>
        <td data-label="Товар">${findNameByIdFromMap(productMap, sale.productId)}</td>
        <td data-label="Водій">${findNameByIdFromMap(userMap, sale.userId)}</td>
        <td data-label="Залучення">${findNameByIdFromMap(sourceMap, sale.sourceId)}</td>
        <td data-label="Всього кг">${sale.quantity ? sale.quantity : ''}</td>
        <td data-label="Всього заплачено">${sale.totalPrice ? sale.totalPrice : ''}</td>
        <td data-label="Ціна за кг">${sale.unitPrice ? sale.unitPrice : ''}</td>
        <td data-label="Форма">${({CASH: '2', BANKTRANSFER: '1'}[sale.paymentMethod] || '')}</td>
        <td data-label="Валюта">${sale.currency ? sale.currency : ''}</td>
        <td data-label="Курс">${sale.exchangeRate ? sale.exchangeRate : ''}</td>
        <td data-label="Забрано">${sale.createdAt ? new Date(sale.createdAt)
        .toLocaleDateString('ua-UA') : ''}</td>
        <td data-label="Дії">
            <button class="edit-button" data-id="${sale.id}" title="Редагувати"><i class="fas fa-edit"></i></button>
            <button class="delete-button" data-id="${sale.id}" title="Видалити"><i class="fas fa-trash"></i></button>
        </td>
    `;
}

function generateProductOptions(selectedId) {
    if (!productMap.size) {
        return '<option value="">Продукти не завантажені</option>';
    }
    return Array.from(productMap.entries()).map(([id, name]) =>
        `<option value="${id}" ${id === selectedId ? 'selected' : ''}>${name}</option>`
    ).join('');
}

function generateSourceOptions(selectedId) {
    if (!sourceMap.size) {
        return '<option value="">Джерела не завантажені</option>';
    }
    return Array.from(sourceMap.entries()).map(([id, name]) =>
        `<option value="${id}" ${id === selectedId ? 'selected' : ''}>${name}</option>`
    ).join('');
}

function showEditModal(sale) {
    const modal = document.getElementById('edit-modal');
    const form = document.getElementById('edit-form');
    const header = modal.querySelector('h3');
    const productSelect = form.querySelector('select[name="productId"]');
    const quantityInput = form.querySelector('input[name="quantity"]');
    const totalPriceInput = form.querySelector('input[name="totalPrice"]');
    const createdAtInput = form.querySelector('input[name="createdAt"]');
    const exchangeRate = form.querySelector('input[name="exchangeRate"]');
    const sourceSelect = form.querySelector('select[name="sourceId"]');

    header.textContent = `ID: ${sale.id}`;
    productSelect.innerHTML = generateProductOptions(sale.productId);
    quantityInput.value = sale.quantity || 0;
    totalPriceInput.value = sale.totalPrice || 0;
    exchangeRate.value = sale.exchangeRate || '';
    createdAtInput.value = sale.createdAt
        ? new Date(sale.createdAt.replace(' ', 'T') + 'Z').toISOString().split('T')[0]
        : '';
    sourceSelect.innerHTML = generateSourceOptions(sale.sourceId);

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
            exchangeRate: exchangeRate.value
        };
        await saveSale(sale, updatedData);
        modal.style.display = 'none';
        setTimeout(() => modal.style.display = 'none', 300);
    };

    document.getElementById('cancel-edit').onclick = () => {
        modal.classList.remove('active');
        setTimeout(() => modal.style.display = 'none', 300);
    };
}

async function saveSale(sale, updatedData) {
    try {
        const response = await fetch(`/api/v1/sale/${sale.id}`, {
            method: 'PATCH',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(updatedData)
        });
        if (response.ok) {
            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection)
        } else {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
        }
    } catch (error) {
        console.error('Помилка:', error);
        handleError(error);
    }
}

function deleteSale(id) {
    if (confirm("Ви впевнені, що хочете видалити цей запис?")) {
        loaderBackdrop.style.display = 'flex';
        fetch(`/api/v1/sale/${id}`, {
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


async function loadDataWithSort(page, size, sort, direction) {
    loaderBackdrop.style.display = 'flex';
    const searchInput = document.getElementById('inputSearch');
    const searchTerm = searchInput ? searchInput.value : '';
    let queryParams = `page=${page}&size=${size}&sort=${sort}&direction=${direction}`;

    if (searchTerm) {
        queryParams += `&q=${encodeURIComponent(searchTerm)}`;
    }

    if (Object.keys(selectedFilters).length > 0) {
        queryParams += `&filters=${encodeURIComponent(JSON.stringify(selectedFilters))}`;
    }

    try {
        const response = await fetch(`${API_URL_SALE}/search?${queryParams}`);

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        const data = await response.json();
        renderSale(data.content);

        updatePagination(data.totalElements, data.content.length, data.totalPages, currentPage);
    } catch (error) {
        console.error('Ошибка:', error);
        handleError(error);
    } finally {
        loaderBackdrop.style.display = 'none';
    }
}


function loadClientDetails(client) {
    showClientModal(client);
}

function showClientModal(client) {
    document.getElementById('client-modal').setAttribute('data-client-id', client.id);

    document.getElementById('modal-client-id').innerText = client.id;
    document.getElementById('modal-client-company').innerText = client.company;
    document.getElementById('modal-client-person').innerText = client.person || '';
    document.getElementById('modal-client-phone').innerText = client.phoneNumbers || '';
    document.getElementById('modal-client-location').innerText = client.location || '';
    document.getElementById('modal-client-price-purchase').innerText = client.pricePurchase || '';
    document.getElementById('modal-client-price-sale').innerText = client.priceSale || '';
    if (client.vat === true) {
        document.getElementById('modal-client-vat').innerHTML =
            `<input type="checkbox" id="edit-vat" checked disabled />`;
    } else if (client.vat === false || client.vat === null || client.vat === undefined) {
        document.getElementById('modal-client-vat').innerHTML =
            `<input type="checkbox" id="edit-vat" disabled />`;
    } else {
        document.getElementById('modal-client-vat').innerHTML = '';
    }
    document.getElementById('modal-client-volumeMonth').innerText = client.volumeMonth || '';
    document.getElementById('modal-client-edrpou').innerText = client.edrpou || '';
    document.getElementById('modal-client-enterpriseName').innerText = client.enterpriseName || '';
    document.getElementById('modal-client-business').innerText = client.business?.id ?
        findNameByIdFromMap(businessMap, client.business.id) : '';
    document.getElementById('modal-client-clientProduct').innerText =
        client.clientProduct?.id ? findNameByIdFromMap(clientProductMap, client.clientProduct.id) : '';
    document.getElementById('modal-client-route').innerText = client.route?.id ?
        findNameByIdFromMap(routeMap, client.route.id) : '';
    document.getElementById('modal-client-region').innerText = client.region?.id ?
        findNameByIdFromMap(regionMap, client.region.id) : '';
    document.getElementById('modal-client-status').innerText = client.status?.id ?
        findNameByIdFromMap(statusMap, client.status.id) : '';
    document.getElementById('modal-client-source').innerText = client.source?.id ?
        findNameByIdFromMap(sourceMap, client.source.id) : '';
    document.getElementById('modal-client-comment').innerText = client.comment || '';
    document.getElementById('modal-client-created').innerText = client.createdAt || '';
    document.getElementById('modal-client-updated').innerText = client.updatedAt || '';

    const urgentlyCheckbox = document.getElementById('modal-client-urgently');
    urgentlyCheckbox.checked = client.urgently;
    urgentlyCheckbox.addEventListener('change', function () {
        toggleUrgently(client.id);
    });

    const modal = document.getElementById('client-modal');
    modal.style.display = 'flex';
    setTimeout(() => {
        modal.classList.add('open');
    }, 10);

    document.getElementById('close-modal-client').addEventListener('click', () => {
        modal.classList.remove('open');
        setTimeout(() => {
            closeModal();
        });
    });

    window.onclick = function (event) {
        if (event.target === modal) {
            closeModal();
        }
    }

    const fullDeleteButton = document.getElementById('full-delete-client');
    fullDeleteButton.onclick = async () => {
        loaderBackdrop.style.display = 'flex';
        try {
            const response = await fetch(`${API_URL}/${client.id}`, {method: 'DELETE'});
            if (!response.ok) {
                const errorData = await response.json();
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                return;
            }
            showMessage('Клієнт повністю видалений', 'info');
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
    deleteButton.onclick = async () => {
        loaderBackdrop.style.display = 'flex';
        try {
            const response = await fetch(`${API_URL}/active/${client.id}`, {method: 'DELETE'});
            if (!response.ok) {
                const errorData = await response.json();
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                return;
            }
            showMessage('Клієнт видалений', 'info');
            modal.style.display = 'none';

            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        } catch (error) {
            console.error('Помилка вимкнення клієнта:', error);
            handleError(error);
        } finally {
            loaderBackdrop.style.display = 'none';
        }
    };

}

async function toggleUrgently(clientId) {
    loaderBackdrop.style.display = 'flex';

    try {
        const response = await fetch(`${API_URL}/urgently/${clientId}`, {method: 'PATCH'});

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        showMessage('Статус терміновості успішно оновлено', 'info');

        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
    } catch (error) {
        console.error('Ошибка обновления статуса терміновості:', error);
        handleError(error);
    } finally {
        loaderBackdrop.style.display = 'none';
    }
}


/*--report--*/

document.getElementById('fetch-report-btn').addEventListener('click', async () => {
    const reportContainer = document.getElementById('report-container');
    const reportContent = document.getElementById('report-content');

    const searchInput = document.getElementById('inputSearch');
    const searchTerm = searchInput ? searchInput.value : '';
    let queryParams = '';

    if (searchTerm) {
        queryParams += `&q=${encodeURIComponent(searchTerm)}`;
    }

    if (Object.keys(selectedFilters).length > 0) {
        queryParams += `&filters=${encodeURIComponent(JSON.stringify(selectedFilters))}`;
    }

    try {
        const url = `api/v1/sale/report?${queryParams}`;

        const response = await fetch(url);
        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        const data = await response.json();
        reportContent.innerHTML = generateReportHTML(data);
        reportContainer.style.display = 'block';

        showMessage('Звіт успішно завантажено', 'info');
    } catch (error) {
        handleError(error);
    }
});


function generateReportHTML(data) {
    let html = `<div class="report-item">Всього продано: ${data.totalCollected} кг</div>`;

    if (data.byAttractors && Object.keys(data.byAttractors).length > 0) {
        html += `<h4>Продажі по залученням:</h4>`;
        for (const [attractor, amount] of Object.entries(data.byAttractors)) {
            html += `<div class="report-item">${attractor}: ${amount} кг</div>`;
        }
    }

    if (data.byDrivers && Object.keys(data.byDrivers).length > 0) {
        html += `<h4>Продано водіями:</h4>`;
        for (const [driver, amount] of Object.entries(data.byDrivers)) {
            html += `<div class="report-item">${driver}: ${amount} кг</div>`;
        }
    }

    if (data.byRegions && Object.keys(data.byRegions).length > 0) {
        if (confirm("Включити області у звіт?")) {
            html += `<h4>Продано по областям:</h4>`;
            for (const [region, amount] of Object.entries(data.byRegions)) {
                html += `<div class="report-item">${region}: ${amount} кг</div>`;
            }
        }
    }

    html += `
                <h4>Додатково:</h4>
                <div class="report-item">Всього зароблено: ${data.totalSpent} грн</div>
                <div class="report-item">Середня ціна за од: ${data.averagePrice} грн</div>
                <div class="report-item">В середньому продаємо по: ${data.averageCollectedPerTime} кг</div>
            `;
    return html;
}

function closeReport() {
    const reportContainer = document.getElementById('report-container');
    reportContainer.style.display = 'none';
}

function printReport() {
    const reportContent = document.getElementById('report-content').innerHTML;
    const printWindow = window.open('', '', 'height=600,width=800');

    printWindow.document.write('<html lang="ua"><head><title>Звіт</title>');

    printWindow.document.write(`
        <style>
            body {
                font-size: 14px;
                margin: 20px;
            }
            h1, h2, h3 {
                font-size: 18px;
            }
        </style>
    `);

    printWindow.document.write('</head><body>');
    printWindow.document.write(reportContent);
    printWindow.document.write('</body></html>');
    printWindow.document.close();

    printWindow.print();
}

/*--search--*/

const searchInput = document.getElementById('inputSearch');
const searchButton = document.getElementById('searchButton');

searchInput.addEventListener('keypress', async (event) => {
    if (event.key === 'Enter') {
        searchButton.click();
    }
});

searchButton.addEventListener('click', async () => {
    const searchTerm = searchInput.value;
    localStorage.setItem('searchTerm', searchTerm);
    loadDataWithSort(0, 100, currentSort, currentDirection);
});

/*--filter--*/

const filterButton = document.querySelector('.filter-button-block');
const filterModal = document.getElementById('filterModal');
const closeFilter = document.querySelector('.close-filter');
const modalContent = filterModal.querySelector('.modal-content-filter');

filterButton.addEventListener('click', () => {
    filterModal.style.display = 'block';
    setTimeout(() => {
        filterModal.classList.add('show');
    }, 10);
});

closeFilter.addEventListener('click', () => {
    closeModalFilter();
});

filterModal.addEventListener('click', (event) => {
    if (!modalContent.contains(event.target)) {
        closeModalFilter();
    }
});

function closeModalFilter() {
    filterModal.classList.add('closing');
    modalContent.classList.add('closing-content');

    setTimeout(() => {
        filterModal.style.display = 'none';
        filterModal.classList.remove('closing');
        modalContent.classList.remove('closing-content');
    }, 200);
}


document.getElementById("modal-filter-button-submit").addEventListener('click',
    (event) => {
        event.preventDefault();
        updateSelectedFilters();
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);

        closeModalFilter();
    });


function updateSelectedFilters() {
    if (typeof selectedFilters === 'undefined') {
        window.selectedFilters = {};
    }

    Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);

    Object.keys(customSelects).forEach(selectId => {
        if (selectId.endsWith('-filter')) {
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
    const formData = new FormData(filterForm);

    const createdAtFrom = formData.get('createdAtFrom');
    const createdAtTo = formData.get('createdAtTo');
    if (createdAtFrom) selectedFilters['createdAtFrom'] = [createdAtFrom];
    if (createdAtTo) selectedFilters['createdAtTo'] = [createdAtTo];

    const numberFields = [
        'quantityFrom', 'quantityTo',
        'totalPriceFrom', 'totalPriceTo',
        'unitPriceFrom', 'unitPriceTo'
    ];

    numberFields.forEach(field => {
        const value = formData.get(field);
        if (value && !isNaN(value) && value.trim() !== '') {
            selectedFilters[field] = [value];
        }
    });

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


document.getElementById('filter-counter').addEventListener('click', () => {
    clearFilters();
});

function clearFilters() {
    Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);

    const filterForm = document.getElementById('filterForm');
    if (filterForm) {
        filterForm.reset();
        Object.keys(customSelects).forEach(selectId => {
            if (selectId.endsWith('-filter')) {
                customSelects[selectId].reset();
            }
        });
    }

    const searchInput = document.getElementById('inputSearch');
    searchInput.value = '';

    localStorage.removeItem('selectedFilters');
    localStorage.removeItem('searchTerm');

    updateFilterCounter();
    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
}


document.addEventListener('DOMContentLoaded', () => {

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
    window.selectedFilters = parsedFilters;

    const savedSearchTerm = localStorage.getItem('searchTerm');
    if (savedSearchTerm) {
        const searchInput = document.getElementById('inputSearch');
        searchInput.value = savedSearchTerm;
    }

    fetch('/api/v1/entities')
        .then(response => response.json())
        .then(data => {
            availableStatuses = data.statuses || [];
            availableRegions = data.regions || [];
            availableSources = data.sources || [];
            availableRoutes = data.routes || [];
            availableBusiness = data.businesses || [];
            availableUsers = data.users || [];
            availableProducts = data.products || [];
            availableClientProducts = data.clientProducts || [];

            statusMap = new Map(availableStatuses.map(item => [item.id, item.name]));
            regionMap = new Map(availableRegions.map(item => [item.id, item.name]));
            sourceMap = new Map(availableSources.map(item => [item.id, item.name]));
            routeMap = new Map(availableRoutes.map(item => [item.id, item.name]));
            businessMap = new Map(availableBusiness.map(item => [item.id, item.name]));
            clientProductMap = new Map(availableClientProducts.map(item => [item.id, item.name]));
            userMap = new Map(availableUsers.map(item => [item.id, item.name]));
            productMap = new Map(availableProducts.map(item => [item.id, item.name]));

            populateSelect('status-filter', availableStatuses || []);
            populateSelect('region-filter', availableRegions || []);
            populateSelect('source-filter', availableSources || []);
            populateSelect('route-filter', availableRoutes || []);
            populateSelect('business-filter', availableBusiness || []);
            populateSelect('user-filter', availableUsers || []);
            populateSelect('product-filter', availableProducts || []);
            populateSelect('paymentMethod-filter', availablePaymentMethods || []);
            populateSelect('currencyType-filter', availableCurrencies || []);
            populateSelect('client-product-filter', availableClientProducts);

            ['region', 'status', 'source', 'route', 'business', 'product', 'user', 'paymentMethod', 'currencyType']
                .forEach(selectId => {
                    const select = document.getElementById(selectId);
                    if (select && !customSelects[`${selectId}-custom`]) {
                        customSelects[`${selectId}-custom`] = createCustomSelect(select);
                    }
                });

            ['region-filter', 'status-filter', 'source-filter', 'route-filter', 'business-filter', 'product-filter',
                'user-filter', 'paymentMethod-filter', 'currencyType-filter', 'client-product-filter'].forEach(selectId => {
                const select = document.getElementById(selectId);
                if (select && !customSelects[selectId]) {
                    customSelects[selectId] = createCustomSelect(select);
                }
            });

            ['region-filter', 'status-filter', 'source-filter', 'route-filter', 'business-filter', 'product-filter',
                'user-filter', 'paymentMethod-filter', 'currencyType-filter', 'client-product-filter'].forEach(selectId => {
                const select = document.getElementById(selectId);
                if (select && customSelects[selectId]) {
                    const name = select.name;
                    if (window.selectedFilters[name] && Array.isArray(window.selectedFilters[name])) {
                        customSelects[selectId].setValue(window.selectedFilters[name]);
                    }
                }
            });

            const filterForm = document.getElementById('filterForm');
            if (filterForm) {
                if (window.selectedFilters['createdAtFrom']) {
                    filterForm.querySelector('#createdAtFrom').value = window.selectedFilters['createdAtFrom'];
                }
                if (window.selectedFilters['createdAtTo']) {
                    filterForm.querySelector('#createdAtTo').value = window.selectedFilters['createdAtTo'];
                }
                if (window.selectedFilters['quantityFrom']) {
                    filterForm.querySelector('#quantityFrom').value = window.selectedFilters['quantityFrom'];
                }
                if (window.selectedFilters['quantityTo']) {
                    filterForm.querySelector('#quantityTo').value = window.selectedFilters['quantityTo'];
                }
                if (window.selectedFilters['totalPriceFrom']) {
                    filterForm.querySelector('#totalPriceFrom').value = window.selectedFilters['totalPriceFrom'];
                }
                if (window.selectedFilters['totalPriceTo']) {
                    filterForm.querySelector('#totalPriceTo').value = window.selectedFilters['totalPriceTo'];
                }
                if (window.selectedFilters['unitPriceFrom']) {
                    filterForm.querySelector('#unitPriceFrom').value = window.selectedFilters['unitPriceFrom'];
                }
                if (window.selectedFilters['unitPriceTo']) {
                    filterForm.querySelector('#unitPriceTo').value = window.selectedFilters['unitPriceTo'];
                }
            }

            updateSelectedFilters();

            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        })
        .catch(error => {
            console.error(error);
            handleError(error);
        });
});


