const API_URL_CONTAINER = '/api/v1/containers/client';
const API_URL = '/api/v1/client';

const prevPageButton = document.getElementById('prev-btn');
const nextPageButton = document.getElementById('next-btn');
const paginationInfo = document.getElementById('pagination-info');
const allClientInfo = document.getElementById('all-client-info');
const loaderBackdrop = document.getElementById('loader-backdrop');
const filterForm = document.getElementById('filterForm');
let currentSort = 'updatedAt';
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
let availableContainers = [];

let sourceMap;
let userMap;
let containerMap;

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

function updatePagination(totalData, dataOnPage, totalPages, currentPageIndex) {
    if (allClientInfo) {
        allClientInfo.textContent = `Тари: ${totalData}`;
    }
    paginationInfo.textContent = `
        Тари на сторінці: ${dataOnPage},
        Всього сторінок: ${totalPages},
        Поточна сторінка: ${currentPageIndex + 1}
    `;

    prevPageButton.disabled = currentPageIndex <= 0;
    nextPageButton.disabled = currentPageIndex >= totalPages - 1;
}

function renderContainers(containers) {
    const tbodyData = document.getElementById('client-table-body');
    if (!tbodyData) return;
    
    tbodyData.innerHTML = '';

    containers.forEach(container => {
        const row = document.createElement('tr');
        row.classList.add('container-row');
        row.dataset.id = container.containerId;

        row.innerHTML = getRowHtml(container);
        tbodyData.appendChild(row);

        const companyCell = row.querySelector('.company-cell');
        if (companyCell && container.client) {
            companyCell.addEventListener('click', () => {
                if (typeof loadClientDetails === 'function') {
                    loadClientDetails(container.client);
                }
            });
        }
    });

    if (typeof applyColumnWidthsForContainers === 'function' && currentClientTypeId) {
        setTimeout(() => {
            const storageKey = `containers_${currentClientTypeId}`;
            applyColumnWidthsForContainers('client-list', storageKey);
        }, 0);
    }
}

function getRowHtml(container) {
    const clientName = container.client ? (container.client.company || container.client.person || '') : '';
    const containerName = container.containerName || '';
    const quantity = container.quantity ? container.quantity.toString() : '';
    const userName = findNameByIdFromMap(userMap, container.userId) || '';
    const updatedAt = container.updatedAt ? new Date(container.updatedAt).toLocaleDateString('ua-UA') : '';
    
    return `
        <td class="company-cell" data-label="Назва клієнта">${clientName}</td>
        <td data-label="Тип тари">${containerName}</td>
        <td data-label="Кількість">${quantity}</td>
        <td data-label="Власник">${userName}</td>
        <td data-label="Оновлено">${updatedAt}</td>
    `;
}


async function loadDataWithSort(page, size, sort, direction) {
    if (!currentClientTypeId) {
        return;
    }
    
    loaderBackdrop.style.display = 'flex';
    const searchInput = document.getElementById('inputSearch');
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
        const response = await fetch(`${API_URL_CONTAINER}/search-containers?${queryParams}`);

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        const data = await response.json();
        renderContainers(data.content);
        
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
    if (sortField === 'updatedAt') {
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
    
    const staticFields = ['quantity', 'updatedAt'];
    
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

function buildContainersTable() {
    const thead = document.querySelector('#client-list table thead tr');
    if (!thead) return;
    
    thead.innerHTML = '';
    
    const headers = [
        { text: 'Назва клієнта', sort: null },
        { text: 'Тип тари', sort: null },
        { text: 'Кількість', sort: 'quantity' },
        { text: 'Власник', sort: null },
        { text: 'Оновлено', sort: 'updatedAt' }
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
            listContainer.innerHTML = '<p style="text-align: center; color: var(--main-grey); padding: 2em;">Немає доступних типів клієнтів</p>';
            modal.style.display = 'flex';
        } else if (accessibleClientTypes.length === 1) {
            window.location.href = `/containers?type=${accessibleClientTypes[0].id}`;
            return;
        } else {
            listContainer.innerHTML = '';
            accessibleClientTypes.forEach(type => {
                const card = document.createElement('div');
                card.className = 'client-type-card';
                card.innerHTML = `
                    <div class="client-type-card-icon">👥</div>
                    <div class="client-type-card-name">${type.name}</div>
                `;
                card.addEventListener('click', () => {
                    window.location.href = `/containers?type=${type.id}`;
                });
                listContainer.appendChild(card);
            });
            modal.style.display = 'flex';
        }

        const closeBtn = document.querySelector('.close-client-type-modal');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                modal.style.display = 'none';
            });
        }
        
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.style.display = 'none';
            }
        });
    } catch (error) {
        console.error('Error loading client types:', error);
    }
}

async function updateNavigationWithCurrentType(typeId) {
    try {
        const response = await fetch(`/api/v1/client-type/${typeId}`);
        if (!response.ok) return;
        
        const clientType = await response.json();
        const navLink = document.querySelector('#nav-containers a');
        
        if (navLink && clientType.name) {
            navLink.innerHTML = `
                <span class="nav-client-type-label">Тари:</span>
                <span class="nav-client-type-name">${clientType.name}</span>
            `;
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
        const [fieldsRes, visibleRes, filterableRes] = await Promise.all([
            fetch(`/api/v1/client-type/${typeId}/field`),
            fetch(`/api/v1/client-type/${typeId}/field/visible`),
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
            const editButtonHtml = canEdit ? `
                <button class="edit-icon" onclick="enableEditField(${field.id}, '${field.fieldType}', ${field.allowMultiple || false})" data-field-id="${field.id}" title="Редагувати">
                    <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
                    </svg>
                </button>
            ` : '';
            
            fieldP.innerHTML = `
                <strong>${field.fieldLabel}:</strong>
                <span id="modal-field-${field.id}" class="${!fieldValue ? 'empty-value' : ''}">${fieldValue || '—'}</span>
                ${editButtonHtml}
            `;
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

    const originalDeleteButtonDisplay = document.getElementById('delete-client')?.style.display || '';
    const originalRestoreButtonDisplay = document.getElementById('restore-client')?.style.display || '';

    document.getElementById('close-modal-client').addEventListener('click', () => {
        if (!editing) {
        modal.classList.remove('open');
        setTimeout(() => {
            closeModal();
        });
        } else {
            showMessage('Збережіть або відмініть зміни', 'error');
        }
    });

    window.onclick = function (event) {
        if (event.target === modal) {
            if (!editing) {
            closeModal();
            } else {
                showMessage('Збережіть або відмініть зміни', 'error');
            }
        }
    }

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
            const response = await fetch(`${API_URL}/${client.id}`, {method: 'DELETE'});
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

    if (editing) {
        if (deleteButton) deleteButton.style.display = 'none';
        if (restoreButton) restoreButton.style.display = 'none';
    } else {
        if (client.isActive === false) {
            if (deleteButton) deleteButton.style.display = 'none';
            if (restoreButton) restoreButton.style.display = 'block';
        } else {
            if (deleteButton) {
                deleteButton.style.display = canDelete ? 'block' : 'none';
            }
            if (restoreButton) restoreButton.style.display = 'none';
        }
    }
    
    deleteButton.onclick = async () => {
        if (!confirm('Ви впевнені, що хочете деактивувати цього клієнта? Клієнт буде прихований, але залишиться в базі даних.')) {
            return;
        }
        
        loaderBackdrop.style.display = 'flex';
        try {
            const response = await fetch(`${API_URL}/active/${client.id}`, {method: 'DELETE'});
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
                const response = await fetch(`${API_URL}/active/${client.id}`, {method: 'PATCH'});
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
        case 'PHONE':
            return fieldValue.valueText || '';
        case 'NUMBER':
            return fieldValue.valueNumber ? fieldValue.valueNumber.toString() : '';
        case 'DATE':
            return fieldValue.valueDate ? new Date(fieldValue.valueDate).toLocaleDateString('ua-UA') : '';
        case 'BOOLEAN':
            return fieldValue.valueBoolean ? 'Так' : 'Ні';
        case 'LIST':
            return fieldValue.valueListValue || '';
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
    loadDataWithSort(0, pageSize, currentSort, currentDirection);
});

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

const modalFilterButtonSubmit = document.getElementById("modal-filter-button-submit");
if (modalFilterButtonSubmit) {
    modalFilterButtonSubmit.addEventListener('click', (event) => {
        event.preventDefault();
        updateSelectedFilters();
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);

        closeModalFilter();
    });
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

        const containerH2 = document.createElement('h2');
        containerH2.textContent = 'Фільтри тари:';
        filterForm.appendChild(containerH2);

        const containerSelectItem = document.createElement('div');
        containerSelectItem.className = 'select-section-item';
        containerSelectItem.innerHTML = `
            <br>
            <label class="select-label-style" for="filter-container">Тип тари:</label>
            <select id="filter-container" name="container" multiple>
            </select>
        `;
        filterForm.appendChild(containerSelectItem);

        const userSelectItem = document.createElement('div');
        userSelectItem.className = 'select-section-item';
        userSelectItem.innerHTML = `
            <br>
            <label class="select-label-style" for="filter-user">Власник:</label>
            <select id="filter-user" name="user" multiple>
            </select>
        `;
        filterForm.appendChild(userSelectItem);

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

        const updatedAtH2 = document.createElement('h2');
        updatedAtH2.textContent = 'Дата оновлення:';
        filterForm.appendChild(updatedAtH2);
        
        const updatedAtBlock = document.createElement('div');
        updatedAtBlock.className = 'filter-block';
        updatedAtBlock.innerHTML = `
            <label class="from-to-style" for="filter-updatedAt-from">Від:</label>
            <input type="date" id="filter-updatedAt-from" name="updatedAtFrom">
            <label class="from-to-style" for="filter-updatedAt-to">До:</label>
            <input type="date" id="filter-updatedAt-to" name="updatedAtTo">
        `;
        filterForm.appendChild(updatedAtBlock);

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
            const containerSelect = filterForm.querySelector('#filter-container');
            if (containerSelect && availableContainers && availableContainers.length > 0) {
                const containerData = availableContainers.map(c => ({
                    id: c.id,
                    name: c.name
                }));
                if (typeof createCustomSelect === 'function') {
                    const customSelect = createCustomSelect(containerSelect, true);
                    if (customSelect) {
                        customSelects['filter-container'] = customSelect;
                        customSelect.populate(containerData);
                    }
                }
            } else if (containerSelect && (!availableContainers || availableContainers.length === 0)) {
                console.warn('No containers available for filter');
            }

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
            } else if (userSelect && (!availableUsers || availableUsers.length === 0)) {
                console.warn('No users available for filter');
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
                    }
                }
            } else if (clientSourceSelect && (!availableSources || availableSources.length === 0)) {
                console.warn('No sources available for filter');
            }
        }, 100);

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
                    selectItem.innerHTML = `
                        <br>
                        <label class="select-label-style" for="filter-${field.fieldName}">${field.fieldLabel}:</label>
                        <select id="filter-${field.fieldName}" name="${field.fieldName}" multiple>
                        </select>
                    `;
                    filterForm.appendChild(selectItem);
                    
                    const select = selectItem.querySelector('select');
                    if (!select) {
                        console.error('Select not found for field:', field.fieldName);
                        return;
                    }

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
                                    
                                    if (window.selectedFilters[field.fieldName]) {
                                        const savedValues = window.selectedFilters[field.fieldName];
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
    } finally {
        buildDynamicFilters._isBuilding = false;
    }
}

function updateSelectedFilters() {
    if (typeof selectedFilters === 'undefined') {
        window.selectedFilters = {};
    }

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

    const quantityFrom = formData.get('quantityFrom');
    const quantityTo = formData.get('quantityTo');
    if (quantityFrom && quantityFrom.trim() !== '' && !isNaN(quantityFrom)) {
        selectedFilters['quantityFrom'] = [quantityFrom];
    }
    if (quantityTo && quantityTo.trim() !== '' && !isNaN(quantityTo)) {
        selectedFilters['quantityTo'] = [quantityTo];
    }

    const updatedAtFrom = formData.get('updatedAtFrom');
    const updatedAtTo = formData.get('updatedAtTo');
    if (updatedAtFrom && updatedAtFrom.trim() !== '') {
        selectedFilters['updatedAtFrom'] = [updatedAtFrom];
    }
    if (updatedAtTo && updatedAtTo.trim() !== '') {
        selectedFilters['updatedAtTo'] = [updatedAtTo];
    }

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

    const searchInput = document.getElementById('inputSearch');
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
    buildContainersTable();
    
    if (typeof initColumnResizerForContainers === 'function' && currentClientTypeId) {
        setTimeout(() => {
            const storageKey = `containers_${currentClientTypeId}`;
            initColumnResizerForContainers('client-list', storageKey);
            if (typeof applyColumnWidthsForContainers === 'function') {
                applyColumnWidthsForContainers('client-list', storageKey);
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
    window.selectedFilters = parsedFilters;

    const savedSearchTerm = localStorage.getItem('searchTerm');
    if (savedSearchTerm) {
        const searchInput = document.getElementById('inputSearch');
        if (searchInput) {
        searchInput.value = savedSearchTerm;
        }
    }

    try {
        const [containersRes, entitiesRes] = await Promise.all([
            fetch('/api/v1/container'),
    fetch('/api/v1/entities')
        ]);

        if (containersRes.ok) {
            availableContainers = await containersRes.json();
            containerMap = new Map(availableContainers.map(item => [item.id, item.name]));
        } else {
            console.error('Failed to load containers:', containersRes.status, containersRes.statusText);
        }

        if (entitiesRes.ok) {
            const data = await entitiesRes.json();
            availableSources = data.sources || [];
            availableUsers = data.users || [];

            sourceMap = new Map(availableSources.map(item => [item.id, item.name]));
            userMap = new Map(availableUsers.map(item => [item.id, item.name]));
        } else {
            console.error('Failed to load entities:', entitiesRes.status, entitiesRes.statusText);
        }

        buildDynamicFilters();


        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        
        if (typeof initExcelExportContainer === 'function') {
            initExcelExportContainer({
                triggerId: 'exportToExcelData',
                modalId: 'exportModalData',
                cancelId: 'exportCancel',
                confirmId: 'exportConfirm',
                formId: 'exportFieldsForm',
                searchInputId: 'inputSearch',
                apiPath: API_URL_CONTAINER
            });
        }
    } catch (error) {
        console.error('Error loading initial data:', error);
            handleError(error);
    }
});
