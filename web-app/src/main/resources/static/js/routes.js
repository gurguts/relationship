const prevPageButton = document.getElementById('prev-btn');
const nextPageButton = document.getElementById('next-btn');
const paginationInfo = document.getElementById('pagination-info');
const allClientInfo = document.getElementById('all-client-info');
const loaderBackdrop = document.getElementById('loader-backdrop');
let currentSort = 'updatedAt';
let currentDirection = 'DESC';
const filterForm = document.getElementById('filterForm');
const customSelects = {};

const userId = localStorage.getItem('userId');
const selectedFilters = {};

const API_URL = '/api/v1/client';

let currentPage = 0;
let pageSize = 50;

const tableBody = document.getElementById('client-table-body');

let availableStatuses = [];
let availableRegions = [];
let availableSources = [];
let availableRoutes = [];
let availableBusiness = [];
let availableClientProducts = [];

let statusMap;
let regionMap;
let routeMap;
let businessMap;
let clientProductMap;
let sourceMap;
let userMap;

const findNameByIdFromMap = (map, id) => {
    const numericId = Number(id);

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


function updatePagination(totalClients, clientsOnPage, totalPages, currentPageIndex) {
    allClientInfo.textContent = `
    Клієнтів: ${totalClients}
    `
    paginationInfo.textContent = `
        Клієнтів на сторінці: ${clientsOnPage},
        Всього сторінок: ${totalPages},
        Поточна сторінка: ${currentPageIndex + 1}
    `;

    prevPageButton.disabled = currentPageIndex <= 0;
    nextPageButton.disabled = currentPageIndex >= totalPages - 1;
}


function renderClients(clients) {
    tableBody.innerHTML = '';
    clients.forEach(client => {
        const row = document.createElement('tr');
        row.classList.add('client-row');
        row.innerHTML = `
            <td><input type="checkbox" class="client-checkbox" data-client-id="${client.id}">
            <td class="button-td" data-label="">  
            <button class="sale-button" data-client-id="${client.id}">Продаж</button>
            <button class="purchase-button" data-client-id="${client.id}">Закупка</button>
            <button class="container-button" data-client-id="${client.id}">Тара</button>
            </td>
            <td data-label="Компанія" data-sort="company" class="company-cell">${client.company}</td>
            <td data-label="Область">${findNameByIdFromMap(regionMap, client.regionId)}</td>
            <td data-label="Адрес" data-sort="location">${client.location ? client.location : ''}</td>
            <td data-label="Телефони">${client.phoneNumbers ? client.phoneNumbers.map(number =>
            `<a href="tel:${number}">${number}</a>`).join('<br>') : ''}</td>
            <td data-label="Маршруты">${findNameByIdFromMap(routeMap, client.routeId)}</td>
            <td data-label="Ціна">${client.price ? client.price : ''}</td>
            <td data-label="Статус">${findNameByIdFromMap(statusMap, client.statusId)}</td>
            ${client.urgently ? '<td data-label="" class="urgent-cell urgent-text">Терміновий Збір</td>' : ''}
         `;
        tableBody.appendChild(row);
        row.querySelector('.company-cell').addEventListener('click', () => {
            loadClientDetails(client);
        });

        row.querySelector('.purchase-button').addEventListener('click', () => {
            openPurchaseModal(client.id, client.sourceId);
        });

        row.querySelector('.sale-button').addEventListener('click', () => {
            openSaleModal(client.id, client.sourceId);
        });

        row.querySelector('.container-button').addEventListener('click', () => {
            openBarrelModal(client.id);
        });
    });
    addCheckboxListeners();
}


/*--saleModal--*/

const saleModal = document.getElementById('saleModal');
const closeButtonSale = saleModal.querySelector('.close-sale-modal');
const saleForm = document.getElementById('saleForm');
const productSelectSale = document.getElementById('productSale');
const currencySelectSale = document.getElementById('currencySale');
const exchangeRateInputSale = document.getElementById('exchangeRateSale');
const exchangeRateLabelSale = document.getElementById('exchangeRateSaleLabel');
let availableProductsSale = [];

document.addEventListener('DOMContentLoaded', () => {
    fetch('/api/v1/product?usage=SALE_ONLY')
        .then(response => response.json())
        .then(data => {
            availableProductsSale = data;
            data.forEach(type => {
                const option = document.createElement("option");
                option.value = type.id;
                option.textContent = type.name;
                productSelectSale.appendChild(option);
            });
        });
    toggleExchangeRateFieldSale();
    currencySelectSale.addEventListener('change', toggleExchangeRateFieldSale);
});

function toggleExchangeRateFieldSale() {
    if (currencySelectSale.value === 'UAH') {
        exchangeRateInputSale.style.display = 'none';
        exchangeRateLabelSale.style.display = 'none';
        exchangeRateInputSale.value = ''; // Очистить значение
        exchangeRateInputSale.removeAttribute('required');
    } else {
        exchangeRateInputSale.style.display = 'block';
        exchangeRateLabelSale.style.display = 'block';
        exchangeRateInputSale.setAttribute('required', 'required');
    }
}

function openSaleModal(clientId, sourceId) {
    saleModal.classList.remove('hide');
    saleModal.style.display = "block";
    setTimeout(() => {
        saleModal.classList.add('show');
    }, 10);
    document.getElementById('clientIdSale').value = clientId;
    document.getElementById('sourceIdSale').value = sourceId;
}

function closeModalSale() {
    saleModal.classList.remove('show');
    saleModal.classList.add('hide');
    setTimeout(() => {
        saleModal.style.display = "none";
        saleForm.reset();
    }, 300);
}


closeButtonSale.addEventListener('click', closeModalSale);
window.addEventListener('click', (event) => {
    if (event.target === saleModal) {
        closeModalSale();
    }
});

saleForm.addEventListener('submit', async (event) => {
    loaderBackdrop.style.display = 'flex';
    event.preventDefault();

    const clientId = document.getElementById('clientIdSale').value;
    const sourceId = document.getElementById('sourceIdSale').value;
    const productId = document.getElementById('productSale').value;
    const quantity = document.getElementById('quantityProductSale').value;
    const totalPrice = document.getElementById('totalPriceSale').value;
    const paymentMethod = document.getElementById('paymentMethodSale').value;
    const currency = document.getElementById('currencySale').value;
    const exchangeRate = currency !== 'UAH' && exchangeRateInputSale.value ? exchangeRateInputSale.value : null;

    try {
        const response = await fetch(`/api/v1/sale`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({clientId, sourceId, productId, quantity, totalPrice, paymentMethod, currency, exchangeRate})
        });

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        closeModalSale();
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        showMessage('Дані успішно надіслані', 'info');
    } catch (error) {
        console.error('Помилка:', error);
        handleError(error);
    } finally {
        loaderBackdrop.style.display = 'none';
    }
});


/*--purchaseModal--*/

const purchaseModal = document.getElementById('purchaseModal');
const closeButton = purchaseModal.querySelector('.close-purchase-modal');
const purchaseForm = document.getElementById('purchaseForm');
const productSelect = document.getElementById('product');
const currencySelect = document.getElementById('currencyPurchase');
const exchangeRateInput = document.getElementById('exchangeRatePurchase');
const exchangeRateLabel = document.getElementById('exchangeRatePurchaseLabel');
let availableProducts = [];

document.addEventListener('DOMContentLoaded', () => {
    fetch('/api/v1/product?usage=PURCHASE_ONLY')
        .then(response => response.json())
        .then(data => {
            availableProducts = data;
            data.forEach(type => {
                const option = document.createElement("option");
                option.value = type.id;
                option.textContent = type.name;
                productSelect.appendChild(option);
            });
        });
    toggleExchangeRateField();
    currencySelect.addEventListener('change', toggleExchangeRateField);
});

function toggleExchangeRateField() {
    if (currencySelect.value === 'UAH') {
        exchangeRateInput.style.display = 'none';
        exchangeRateLabel.style.display = 'none';
        exchangeRateInput.value = '';
    } else {
        exchangeRateInput.style.display = 'block';
        exchangeRateLabel.style.display = 'block';
    }
}

function openPurchaseModal(clientId, sourceId) {
    purchaseModal.classList.remove('hide');
    purchaseModal.style.display = "block";
    setTimeout(() => {
        purchaseModal.classList.add('show');
    }, 10);
    document.getElementById('clientIdPurchase').value = clientId;
    document.getElementById('sourceIdPurchase').value = sourceId;
    toggleExchangeRateField();
}

function closeModalPurchase() {
    purchaseModal.classList.remove('show');
    purchaseModal.classList.add('hide');
    setTimeout(() => {
        purchaseModal.style.display = "none";
        purchaseForm.reset();
        toggleExchangeRateField();
    }, 300);
}


closeButton.addEventListener('click', closeModalPurchase);
window.addEventListener('click', (event) => {
    if (event.target === purchaseModal) {
        closeModalPurchase();
    }
});

purchaseForm.addEventListener('submit', async (event) => {
    loaderBackdrop.style.display = 'flex';
    event.preventDefault();

    const clientId = document.getElementById('clientIdPurchase').value;
    const sourceId = document.getElementById('sourceIdPurchase').value;
    const productId = document.getElementById('product').value;
    const quantity = document.getElementById('quantityProduct').value;
    const totalPrice = document.getElementById('totalPrice').value;
    const paymentMethod = document.getElementById('paymentMethod').value;
    const currency = document.getElementById('currencyPurchase').value;
    const exchangeRate = currency !== 'UAH' && exchangeRateInput.value ? exchangeRateInput.value : null;

    try {
        const response = await fetch(`/api/v1/purchase`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({clientId, sourceId, productId, quantity, totalPrice, paymentMethod, currency, exchangeRate})
        });

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        closeModalPurchase();
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        showMessage('Дані успішно надіслані', 'info');
    } catch (error) {
        console.error('Помилка:', error);
        handleError(error);
    } finally {
        loaderBackdrop.style.display = 'none';
    }
});


const modalElementBarrel = document.getElementById('barrelModal');
const closeBarrelButton = modalElementBarrel.querySelector('.close-barrel');
const barrelForm = document.getElementById('barrelForm');
const barrelTypeSelect = document.getElementById('barrelType');
let availableBarrelTypes = [];


document.addEventListener('DOMContentLoaded', async () => {
    try {
        const response = await fetch('/api/v1/container', {
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

        availableBarrelTypes = await response.json();

        availableBarrelTypes.forEach(type => {
            const option = document.createElement('option');
            option.value = type.id;
            option.textContent = type.name;
            barrelTypeSelect.appendChild(option);
        });
    } catch (error) {
        console.error('Error loading container types:', error);
        handleError(error);
    }
});

function openBarrelModal(clientId) {
    modalElementBarrel.classList.remove('hide');
    modalElementBarrel.style.display = 'block';
    setTimeout(() => {
        modalElementBarrel.classList.add('show');
    }, 10);
    document.getElementById('clientIdBarrel').value = clientId;
}

function closeModalBarrel() {
    modalElementBarrel.classList.remove('show');
    modalElementBarrel.classList.add('hide');
    setTimeout(() => {
        modalElementBarrel.style.display = 'none';
        barrelForm.reset();
    }, 300);
}

closeBarrelButton.addEventListener('click', closeModalBarrel);
window.addEventListener('click', (event) => {
    if (event.target === modalElementBarrel) {
        closeModalBarrel();
    }
});

barrelForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    loaderBackdrop.style.display = 'flex';

    const clientId = document.getElementById('clientIdBarrel').value;
    const action = document.getElementById('action').value;
    const containerId = document.getElementById('barrelType').value;
    const quantity = document.getElementById('quantity').value;

    try {
        let endpoint, requestBody;
        if (action === 'TRANSFER_TO_CLIENT') {
            endpoint = '/api/v1/containers/client/transfer';
            requestBody = {
                clientId: parseInt(clientId),
                containerId: parseInt(containerId),
                quantity: Number(quantity).toString(),
            };
        } else if (action === 'COLLECT_FROM_CLIENT') {
            endpoint = '/api/v1/containers/client/collect';
            requestBody = {
                clientId: parseInt(clientId),
                containerId: parseInt(containerId),
                quantity: Number(quantity).toString(),
            };
        }

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
        }

        closeModalBarrel();
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        showMessage('Операція успішно виконана', 'info');

    } catch (error) {
        console.error('Помилка:', error);
        handleError(error);
    } finally {
        loaderBackdrop.style.display = 'none';
    }
});


/*--checkbox--*/

function addCheckboxListeners() {
    const checkboxes = document.querySelectorAll('.client-checkbox');
    const selectAll = document.getElementById('select-all');
    const mapButton = document.getElementById('map-button');

    function updateMapButtonVisibility() {
        const selectedCount = document.querySelectorAll('.client-checkbox:checked').length;
        mapButton.style.display = selectedCount > 0 ? 'block' : 'none';
    }

    selectAll.addEventListener('change', () => {
        const isChecked = selectAll.checked;
        checkboxes.forEach(checkbox => {
            checkbox.checked = isChecked;
        });
        updateMapButtonVisibility();
    });

    checkboxes.forEach(checkbox => {
        checkbox.addEventListener('change', () => {
            if (!checkbox.checked) {
                selectAll.checked = false;
            }
            updateMapButtonVisibility();
        });
    });
}

/*--map--*/

function setupMapModal() {
    const modal = document.getElementById('map-modal');
    const closeModal = modal.querySelector('.map-close');
    const mapButton = document.getElementById('map-button');
    const routeForm = document.getElementById('route-form');

    mapButton.addEventListener('click', () => {
        const selectedClients = Array.from(document.querySelectorAll('.client-checkbox:checked'))
            .map(checkbox => {
                const row = checkbox.closest('tr');
                const locationCell = row.querySelector('[data-sort="location"]');
                const companyCell = row.querySelector('[data-sort="company"]');
                return {
                    location: locationCell ? locationCell.textContent.trim() : null,
                    company: companyCell ? companyCell.textContent.trim() : null
                };
            })
            .filter(client => client.location);

        if (selectedClients.length === 0) {
            alert('Виберіть хоч одного клієнта з адресою');
            return;
        }

        modal.style.display = 'flex';

        routeForm.dataset.selectedClients = JSON.stringify(selectedClients);
    });

    routeForm.addEventListener('submit', async (event) => {
        event.preventDefault();

        const startLocation = document.getElementById('start-location').value;
        const endLocation = document.getElementById('end-location').value;
        const clientData = JSON.parse(routeForm.dataset.selectedClients);

        const locations = [startLocation, ...clientData.map(client => client.location), endLocation];
        const companies = clientData.map(client => client.company);

        if (locations.length < 2) {
            alert('Потрібно мінімум дві локації для побудови маршруту.');
            return;
        }

        const response = await fetch('/api/v1/client/map', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({locations, companies})
        });

        if (response.ok) {
            const data = await response.json();
            window.location.href = `/map/${data.mapId}`;
        } else {
            alert('Помилка при побудові маршруту.');
        }
    });


    closeModal.addEventListener('click', () => {
        modal.style.display = 'none';
    });

    window.addEventListener('click', (event) => {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
}

document.addEventListener('DOMContentLoaded', () => {
    setupMapModal();
});


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
        const response = await fetch(`${API_URL}/search?${queryParams}`);

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        const data = await response.json();
        renderClients(data.content);
        updatePagination(data.totalElements, data.content.length, data.totalPages, currentPage);
    } catch (error) {
        console.error('Помилка:', error);
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
    document.getElementById('modal-client-business').innerText =
        findNameByIdFromMap(businessMap, client.businessId);
    document.getElementById('modal-client-clientProduct').innerText =
        findNameByIdFromMap(clientProductMap, client.clientProductId);
    document.getElementById('modal-client-route').innerText =
        findNameByIdFromMap(routeMap, client.routeId);
    document.getElementById('modal-client-region').innerText =
        findNameByIdFromMap(regionMap, client.regionId);
    document.getElementById('modal-client-status').innerText =
        findNameByIdFromMap(statusMap, client.statusId);
    document.getElementById('modal-client-source').innerText =
        findNameByIdFromMap(sourceMap, client.sourceId);
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

/*-------create client-------*/

var modal = document.getElementById("createClientModal");
var btn = document.getElementById("open-modal");
var span = document.getElementsByClassName("create-client-close")[0];
let editing = false;

btn.onclick = function () {
    modal.classList.remove('hide');
    modal.style.display = "flex";
    setTimeout(() => {
        modal.classList.add('show');
    }, 10);
};

span.onclick = function () {
    modal.classList.remove('show');
    modal.classList.add('hide');
    setTimeout(() => {
        modal.style.display = "none";
        resetForm();
    }, 300);
};

window.onclick = function (event) {
    if (event.target === modal) {
        modal.classList.remove('show');
        modal.classList.add('hide');
        setTimeout(() => {
            modal.style.display = "none";
            resetForm();
        }, 300);
    }
};


window.onclick = function (event) {
    if (event.target === modal) {
        modal.style.display = "none";
        resetForm();
    }
}

function resetForm() {
    const form = document.getElementById('client-form');
    form.reset();

    ['region', 'status', 'source', 'route', 'business'].forEach(selectId => {
        const select = document.getElementById(selectId);
        select.selectedIndex = 0;
        const customSelectId = `${selectId}-custom`;
        if (customSelects[customSelectId]) {
            customSelects[customSelectId].reset();
            let defaultValue = defaultValues[selectId];
            if (typeof defaultValue === 'function') {
                defaultValue = defaultValue();
            }
            if (defaultValue && select.querySelector(`option[value="${defaultValue}"]`)) {
                customSelects[customSelectId].setValue(defaultValue);
            }
        }
    });
}

const defaultValues = {
    region: '136',
    status: '24',
    route: '66',
    business: '1',
    clientProduct: '1',
    source: () => {
        const userId = localStorage.getItem('userId');
        return userSourceMapping[userId] ? String(userSourceMapping[userId]) : '';
    }
};

const userSourceMapping = {
    '1': 8, // admin
    '2': 15, // Музика Катя
    '3': 28, // Водій Дмитро
    '4': 7, // Водій Сергій
    '5': 8, // Шмигельська Олена
    '6': 10, // Денис Казаков
    '7': 26, // Водій Андрій
    '9': 14, // Богдан Осипишин
    '10': 8, // test driver
    '11': 8, // КЗП
    '12': 32, // Водій Саша
    '13': 8, // Сергій Дзвунко
    '14': 30, // Юрій Ємець
    '15': 31 // Артем Фаєр
};


const phonePattern = /^\+380\d{9}$/;
document.getElementById('phoneNumbers').addEventListener('input', updateOutput);

function formatPhoneNumber(num) {
    const cleanedNum = num.replace(/[^\d+]/g, '');
    if (phonePattern.test(cleanedNum)) {
        return cleanedNum;
    } else if (cleanedNum.length === 10 && cleanedNum.startsWith('0')) {
        return "+380" + cleanedNum.substring(1);
    } else if (cleanedNum.length === 12 && cleanedNum.startsWith('380')) {
        return "+380" + cleanedNum.substring(3);
    } else if (cleanedNum.length >= 12 && cleanedNum.startsWith('+380')) {
        return "+380" + cleanedNum.substring(4);
    } else {
        return null;
    }
}

function updateOutput() {
    const input = document.getElementById('phoneNumbers').value;
    const outputDiv = document.getElementById('output');
    outputDiv.innerHTML = '';

    let formattedNumbers = input.split(',')
        .map(num => num.trim())
        .filter(num => num.length > 0)
        .map(formatPhoneNumber)
        .filter(num => num !== null);

    if (formattedNumbers.length > 0) {
        const formattedNumbersList = document.createElement('ul');
        formattedNumbersList.className = 'phone-numbers-list';
        formattedNumbers.forEach(num => {
            const listItem = document.createElement('li');
            listItem.className = 'phone-number-item';
            listItem.textContent = num;
            formattedNumbersList.appendChild(listItem);
        });
        outputDiv.appendChild(formattedNumbersList);
    }
}


document.getElementById('client-form').addEventListener('submit',
    async function (event) {
        event.preventDefault();

        loaderBackdrop.style.display = 'flex';

        const formData = new FormData(this);
        const clientData = {};

        formData.forEach((value, key) => clientData[key] = value);

        clientData.phoneNumbers = clientData.phoneNumbers
            .split(',')
            .map(num => num.trim())
            .filter(num => num.length > 0)
            .map(num => formatPhoneNumber(num))
            .filter(num => num !== null);

        try {
            const response = await fetch('/api/v1/client', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(clientData),
            });

            if (!response.ok) {
                const errorData = await response.json();
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                return;
            }

            const data = await response.json();

            modal.style.display = "none";
            resetForm();
            loadDataWithSort(0, pageSize, currentSort, currentDirection);

            showMessage(`Клієнт з ID: ${data.id} успішно створений`, 'info');
        } catch (error) {
            console.error('Ошибка создания клиента:', error);
            handleError(error);
        } finally {
            loaderBackdrop.style.display = 'none';
        }
    });


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
    const updatedAtFrom = formData.get('updatedAtFrom');
    const updatedAtTo = formData.get('updatedAtTo');

    if (createdAtFrom) selectedFilters['createdAtFrom'] = [createdAtFrom];
    if (createdAtTo) selectedFilters['createdAtTo'] = [createdAtTo];
    if (updatedAtFrom) selectedFilters['updatedAtFrom'] = [updatedAtFrom];
    if (updatedAtTo) selectedFilters['updatedAtTo'] = [updatedAtTo];

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

    const companyInput = document.getElementById('company');
    const saveButton = document.getElementById('save-button');

    const validateForm = () => {
        const isCompanyFilled = companyInput.value.trim() !== '';
        saveButton.disabled = !isCompanyFilled;
    };

    companyInput.addEventListener('input', validateForm);
    validateForm();

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

            populateSelect('status', availableStatuses || []);
            populateSelect('status-filter', availableStatuses || []);
            populateSelect('region', availableRegions || []);
            populateSelect('region-filter', availableRegions || []);
            populateSelect('source', availableSources || []);
            populateSelect('source-filter', availableSources || []);
            populateSelect('route', availableRoutes || []);
            populateSelect('route-filter', availableRoutes || []);
            populateSelect('business', availableBusiness || []);
            populateSelect('business-filter', availableBusiness || []);
            populateSelect('clientProduct', availableClientProducts);
            populateSelect('client-product-filter', availableClientProducts);

            ['region', 'status', 'source', 'route', 'business', 'clientProduct'].forEach(selectId => {
                const select = document.getElementById(selectId);
                if (select && !customSelects[`${selectId}-custom`]) {
                    customSelects[`${selectId}-custom`] = createCustomSelect(select);
                }
            });

            ['region-filter', 'status-filter', 'source-filter', 'route-filter', 'business-filter', 'client-product-filter']
                .forEach(selectId => {
                    const select = document.getElementById(selectId);
                    if (select && !customSelects[selectId]) {
                        customSelects[selectId] = createCustomSelect(select);
                    }
                });

            ['region-filter', 'status-filter', 'source-filter', 'route-filter', 'business-filter', 'client-product-filter']
                .forEach(selectId => {
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
                if (window.selectedFilters['updatedAtFrom']) {
                    filterForm.querySelector('#updatedAtFrom').value = window.selectedFilters['updatedAtFrom'];
                }
                if (window.selectedFilters['updatedAtTo']) {
                    filterForm.querySelector('#updatedAtTo').value = window.selectedFilters['updatedAtTo'];
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



