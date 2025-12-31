const API_URL_CONTAINER = '/api/v1/containers/client';
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
    
    tbodyData.textContent = '';

    containers.forEach(container => {
        const row = document.createElement('tr');
        row.classList.add('container-row');
        row.dataset.id = container.containerId;

        const clientName = container.client ? (container.client.company || container.client.person || '') : '';
        const containerName = container.containerName || '';
        const quantity = container.quantity ? container.quantity.toString() : '';
        const userName = findNameByIdFromMap(userMap, container.userId) || '';
        const updatedAt = container.updatedAt ? new Date(container.updatedAt).toLocaleDateString('ua-UA') : '';

        const companyCell = document.createElement('td');
        companyCell.className = 'company-cell';
        companyCell.setAttribute('data-label', 'Назва клієнта');
        companyCell.textContent = clientName;
        row.appendChild(companyCell);

        const containerCell = document.createElement('td');
        containerCell.setAttribute('data-label', 'Тип тари');
        containerCell.textContent = containerName;
        row.appendChild(containerCell);

        const quantityCell = document.createElement('td');
        quantityCell.setAttribute('data-label', 'Кількість');
        quantityCell.textContent = quantity;
        row.appendChild(quantityCell);

        const userCell = document.createElement('td');
        userCell.setAttribute('data-label', 'Власник');
        userCell.textContent = userName;
        row.appendChild(userCell);

        const updatedAtCell = document.createElement('td');
        updatedAtCell.setAttribute('data-label', 'Оновлено');
        updatedAtCell.textContent = updatedAt;
        row.appendChild(updatedAtCell);

        tbodyData.appendChild(row);

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
    
    thead.textContent = '';
    
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
            listContainer.textContent = '';
            const emptyMessage = document.createElement('p');
            emptyMessage.style.textAlign = 'center';
            emptyMessage.style.color = 'var(--main-grey)';
            emptyMessage.style.padding = '2em';
            emptyMessage.textContent = 'Немає доступних типів клієнтів';
            listContainer.appendChild(emptyMessage);
            modal.style.display = 'flex';
        } else if (accessibleClientTypes.length === 1) {
            window.location.href = `/containers?type=${accessibleClientTypes[0].id}`;
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
                    window.location.href = `/containers?type=${type.id}`;
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
            const closeTypeModalHandler = () => {
                modal.style.display = 'none';
            };
            closeBtn._closeTypeModalHandler = closeTypeModalHandler;
            closeBtn.addEventListener('click', closeTypeModalHandler);
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
        const navLink = document.querySelector('#nav-containers a');
        
        if (navLink && clientType.name) {
            navLink.textContent = '';
            
            const labelSpan = document.createElement('span');
            labelSpan.className = 'nav-client-type-label';
            labelSpan.textContent = 'Тари:';
            navLink.appendChild(labelSpan);
            
            const nameSpan = document.createElement('span');
            nameSpan.className = 'nav-client-type-name';
            nameSpan.textContent = clientType.name;
            navLink.appendChild(nameSpan);
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
            fieldP.setAttribute('data-field-type', field.fieldType);
            
            const strong = document.createElement('strong');
            strong.textContent = field.fieldLabel + ':';
            fieldP.appendChild(strong);
            
            const valueSpan = document.createElement('span');
            valueSpan.id = `modal-field-${field.id}`;
            if (values.length === 0) {
                valueSpan.classList.add('empty-value');
                valueSpan.textContent = '—';
    } else {
                if (field.allowMultiple) {
                    const fragment = document.createDocumentFragment();
                    values.forEach((v, index) => {
                        if (index > 0) {
                            const br = document.createElement('br');
                            fragment.appendChild(br);
                        }
                        const text = document.createTextNode(formatFieldValueForModal(v, field));
                        fragment.appendChild(text);
                    });
                    valueSpan.appendChild(fragment);
                } else {
                    valueSpan.textContent = formatFieldValueForModal(values[0], field);
                }
            }
            fieldP.appendChild(valueSpan);
            
            const canEdit = canEditClient(client);
            if (canEdit) {
                const editBtn = document.createElement('button');
                editBtn.className = 'edit-icon';
                editBtn.setAttribute('data-field-id', field.id);
                editBtn.setAttribute('title', 'Редагувати');
                editBtn.setAttribute('onclick', `enableEditField(${field.id}, '${escapeHtml(field.fieldType)}', ${field.allowMultiple || false})`);
                
                const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                svg.setAttribute('viewBox', '0 0 24 24');
                const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                path.setAttribute('d', 'M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z');
                svg.appendChild(path);
                editBtn.appendChild(svg);
                
                fieldP.appendChild(editBtn);
            }

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

    const closeModalBtn = document.getElementById('close-modal-client');
    if (closeModalBtn) {
        if (closeModalBtn._closeModalHandler) {
            closeModalBtn.removeEventListener('click', closeModalBtn._closeModalHandler);
        }
        const closeModalHandler = () => {
            if (!editing) {
        modal.classList.remove('open');
        setTimeout(() => {
            closeModal();
        });
            } else {
                showMessage('Збережіть або відмініть зміни', 'error');
            }
        };
        closeModalBtn._closeModalHandler = closeModalHandler;
        closeModalBtn.addEventListener('click', closeModalHandler);
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

        if (fullDeleteButton._fullDeleteHandler) {
            fullDeleteButton.removeEventListener('click', fullDeleteButton._fullDeleteHandler);
        }
        const fullDeleteHandler = async () => {
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
        fullDeleteButton._fullDeleteHandler = fullDeleteHandler;
        fullDeleteButton.addEventListener('click', fullDeleteHandler);
    }

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
    
    if (deleteButton) {
        if (deleteButton._deleteHandler) {
            deleteButton.removeEventListener('click', deleteButton._deleteHandler);
        }
        const deleteHandler = async () => {
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
        deleteButton._deleteHandler = deleteHandler;
        deleteButton.addEventListener('click', deleteHandler);
    }

    if (restoreButton) {
        if (restoreButton._restoreHandler) {
            restoreButton.removeEventListener('click', restoreButton._restoreHandler);
        }
        const restoreHandler = async () => {
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
        restoreButton._restoreHandler = restoreHandler;
        restoreButton.addEventListener('click', restoreHandler);
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
            const phoneValue = fieldValue.valueText || '';
            return escapeHtml(phoneValue);
        case 'NUMBER':
            return fieldValue.valueNumber ? fieldValue.valueNumber.toString() : '';
        case 'DATE':
            return fieldValue.valueDate ? new Date(fieldValue.valueDate).toLocaleDateString('ua-UA') : '';
        case 'BOOLEAN':
            return fieldValue.valueBoolean ? 'Так' : 'Ні';
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

let searchDebounceTimer = null;

function debounce(func, delay) {
    return function(...args) {
        clearTimeout(searchDebounceTimer);
        searchDebounceTimer = setTimeout(() => func.apply(this, args), delay);
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
            clearTimeout(searchDebounceTimer);
            performSearch();
        } else {
            debouncedSearch();
        }
    });

    searchInput.addEventListener('input', () => {
        debouncedSearch();
    });
}

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

// Removed: filter modal click handler to prevent closing on outside click
// filterModal.addEventListener('click', (event) => {
//     if (!modalContent.contains(event.target)) {
//         closeModalFilter();
//     }
// });

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
                sel.textContent = '';
            });
            el.remove();
        });

        const containerH2 = document.createElement('h2');
        containerH2.textContent = 'Фільтри тари:';
        filterForm.appendChild(containerH2);

        const containerSelectItem = document.createElement('div');
        containerSelectItem.className = 'select-section-item';
        const containerBr = document.createElement('br');
        containerSelectItem.appendChild(containerBr);
        const containerLabel = document.createElement('label');
        containerLabel.className = 'select-label-style';
        containerLabel.setAttribute('for', 'filter-container');
        containerLabel.textContent = 'Тип тари:';
        containerSelectItem.appendChild(containerLabel);
        const containerSelect = document.createElement('select');
        containerSelect.id = 'filter-container';
        containerSelect.name = 'container';
        containerSelect.multiple = true;
        containerSelectItem.appendChild(containerSelect);
        filterForm.appendChild(containerSelectItem);

        const userSelectItem = document.createElement('div');
        userSelectItem.className = 'select-section-item';
        const userBr = document.createElement('br');
        userSelectItem.appendChild(userBr);
        const userLabel = document.createElement('label');
        userLabel.className = 'select-label-style';
        userLabel.setAttribute('for', 'filter-user');
        userLabel.textContent = 'Власник:';
        userSelectItem.appendChild(userLabel);
        const userSelect = document.createElement('select');
        userSelect.id = 'filter-user';
        userSelect.name = 'user';
        userSelect.multiple = true;
        userSelectItem.appendChild(userSelect);
        filterForm.appendChild(userSelectItem);

        const quantityH2 = document.createElement('h2');
        quantityH2.textContent = 'Кількість:';
        filterForm.appendChild(quantityH2);
        
        const quantityBlock = document.createElement('div');
        quantityBlock.className = 'filter-block';
        const quantityFromLabel = document.createElement('label');
        quantityFromLabel.className = 'from-to-style';
        quantityFromLabel.setAttribute('for', 'filter-quantity-from');
        quantityFromLabel.textContent = 'Від:';
        quantityBlock.appendChild(quantityFromLabel);
        const quantityFromInput = document.createElement('input');
        quantityFromInput.type = 'number';
        quantityFromInput.id = 'filter-quantity-from';
        quantityFromInput.name = 'quantityFrom';
        quantityFromInput.step = '0.01';
        quantityFromInput.placeholder = 'Мінімум';
        quantityBlock.appendChild(quantityFromInput);
        const quantityToLabel = document.createElement('label');
        quantityToLabel.className = 'from-to-style';
        quantityToLabel.setAttribute('for', 'filter-quantity-to');
        quantityToLabel.textContent = 'До:';
        quantityBlock.appendChild(quantityToLabel);
        const quantityToInput = document.createElement('input');
        quantityToInput.type = 'number';
        quantityToInput.id = 'filter-quantity-to';
        quantityToInput.name = 'quantityTo';
        quantityToInput.step = '0.01';
        quantityToInput.placeholder = 'Максимум';
        quantityBlock.appendChild(quantityToInput);
        filterForm.appendChild(quantityBlock);

        const updatedAtH2 = document.createElement('h2');
        updatedAtH2.textContent = 'Дата оновлення:';
        filterForm.appendChild(updatedAtH2);
        
        const updatedAtBlock = document.createElement('div');
        updatedAtBlock.className = 'filter-block';
        const updatedAtFromLabel = document.createElement('label');
        updatedAtFromLabel.className = 'from-to-style';
        updatedAtFromLabel.setAttribute('for', 'filter-updatedAt-from');
        updatedAtFromLabel.textContent = 'Від:';
        updatedAtBlock.appendChild(updatedAtFromLabel);
        const updatedAtFromInput = document.createElement('input');
        updatedAtFromInput.type = 'date';
        updatedAtFromInput.id = 'filter-updatedAt-from';
        updatedAtFromInput.name = 'updatedAtFrom';
        updatedAtBlock.appendChild(updatedAtFromInput);
        const updatedAtToLabel = document.createElement('label');
        updatedAtToLabel.className = 'from-to-style';
        updatedAtToLabel.setAttribute('for', 'filter-updatedAt-to');
        updatedAtToLabel.textContent = 'До:';
        updatedAtBlock.appendChild(updatedAtToLabel);
        const updatedAtToInput = document.createElement('input');
        updatedAtToInput.type = 'date';
        updatedAtToInput.id = 'filter-updatedAt-to';
        updatedAtToInput.name = 'updatedAtTo';
        updatedAtBlock.appendChild(updatedAtToInput);
        filterForm.appendChild(updatedAtBlock);

        const clientH2 = document.createElement('h2');
        clientH2.textContent = 'Фільтри клієнта:';
        filterForm.appendChild(clientH2);

        const clientCreatedAtH2 = document.createElement('h2');
        clientCreatedAtH2.textContent = 'Дата створення клієнта:';
        filterForm.appendChild(clientCreatedAtH2);
        
        const clientCreatedAtBlock = document.createElement('div');
        clientCreatedAtBlock.className = 'filter-block';
        const clientCreatedAtFromLabel = document.createElement('label');
        clientCreatedAtFromLabel.className = 'from-to-style';
        clientCreatedAtFromLabel.setAttribute('for', 'filter-clientCreatedAt-from');
        clientCreatedAtFromLabel.textContent = 'Від:';
        clientCreatedAtBlock.appendChild(clientCreatedAtFromLabel);
        const clientCreatedAtFromInput = document.createElement('input');
        clientCreatedAtFromInput.type = 'date';
        clientCreatedAtFromInput.id = 'filter-clientCreatedAt-from';
        clientCreatedAtFromInput.name = 'clientCreatedAtFrom';
        clientCreatedAtBlock.appendChild(clientCreatedAtFromInput);
        const clientCreatedAtToLabel = document.createElement('label');
        clientCreatedAtToLabel.className = 'from-to-style';
        clientCreatedAtToLabel.setAttribute('for', 'filter-clientCreatedAt-to');
        clientCreatedAtToLabel.textContent = 'До:';
        clientCreatedAtBlock.appendChild(clientCreatedAtToLabel);
        const clientCreatedAtToInput = document.createElement('input');
        clientCreatedAtToInput.type = 'date';
        clientCreatedAtToInput.id = 'filter-clientCreatedAt-to';
        clientCreatedAtToInput.name = 'clientCreatedAtTo';
        clientCreatedAtBlock.appendChild(clientCreatedAtToInput);
        filterForm.appendChild(clientCreatedAtBlock);

        const clientUpdatedAtH2 = document.createElement('h2');
        clientUpdatedAtH2.textContent = 'Дата оновлення клієнта:';
        filterForm.appendChild(clientUpdatedAtH2);
        
        const clientUpdatedAtBlock = document.createElement('div');
        clientUpdatedAtBlock.className = 'filter-block';
        const clientUpdatedAtFromLabel = document.createElement('label');
        clientUpdatedAtFromLabel.className = 'from-to-style';
        clientUpdatedAtFromLabel.setAttribute('for', 'filter-clientUpdatedAt-from');
        clientUpdatedAtFromLabel.textContent = 'Від:';
        clientUpdatedAtBlock.appendChild(clientUpdatedAtFromLabel);
        const clientUpdatedAtFromInput = document.createElement('input');
        clientUpdatedAtFromInput.type = 'date';
        clientUpdatedAtFromInput.id = 'filter-clientUpdatedAt-from';
        clientUpdatedAtFromInput.name = 'clientUpdatedAtFrom';
        clientUpdatedAtBlock.appendChild(clientUpdatedAtFromInput);
        const clientUpdatedAtToLabel = document.createElement('label');
        clientUpdatedAtToLabel.className = 'from-to-style';
        clientUpdatedAtToLabel.setAttribute('for', 'filter-clientUpdatedAt-to');
        clientUpdatedAtToLabel.textContent = 'До:';
        clientUpdatedAtBlock.appendChild(clientUpdatedAtToLabel);
        const clientUpdatedAtToInput = document.createElement('input');
        clientUpdatedAtToInput.type = 'date';
        clientUpdatedAtToInput.id = 'filter-clientUpdatedAt-to';
        clientUpdatedAtToInput.name = 'clientUpdatedAtTo';
        clientUpdatedAtBlock.appendChild(clientUpdatedAtToInput);
        filterForm.appendChild(clientUpdatedAtBlock);

        const clientSourceSelectItem = document.createElement('div');
        clientSourceSelectItem.className = 'select-section-item';
        const clientSourceBr = document.createElement('br');
        clientSourceSelectItem.appendChild(clientSourceBr);
        const clientSourceLabel = document.createElement('label');
        clientSourceLabel.className = 'select-label-style';
        clientSourceLabel.setAttribute('for', 'filter-clientSource');
        clientSourceLabel.textContent = 'Залучення клієнта:';
        clientSourceSelectItem.appendChild(clientSourceLabel);
        const clientSourceSelect = document.createElement('select');
        clientSourceSelect.id = 'filter-clientSource';
        clientSourceSelect.name = 'clientSource';
        clientSourceSelect.multiple = true;
        clientSourceSelectItem.appendChild(clientSourceSelect);
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
                    const dateFromLabel = document.createElement('label');
                    dateFromLabel.className = 'from-to-style';
                    dateFromLabel.setAttribute('for', `filter-${escapeHtml(field.fieldName)}-from`);
                    dateFromLabel.textContent = 'Від:';
                    filterBlock.appendChild(dateFromLabel);
                    const dateFromInput = document.createElement('input');
                    dateFromInput.type = 'date';
                    dateFromInput.id = `filter-${escapeHtml(field.fieldName)}-from`;
                    dateFromInput.name = `${escapeHtml(field.fieldName)}From`;
                    filterBlock.appendChild(dateFromInput);
                    const dateToLabel = document.createElement('label');
                    dateToLabel.className = 'from-to-style';
                    dateToLabel.setAttribute('for', `filter-${escapeHtml(field.fieldName)}-to`);
                    dateToLabel.textContent = 'До:';
                    filterBlock.appendChild(dateToLabel);
                    const dateToInput = document.createElement('input');
                    dateToInput.type = 'date';
                    dateToInput.id = `filter-${escapeHtml(field.fieldName)}-to`;
                    dateToInput.name = `${escapeHtml(field.fieldName)}To`;
                    filterBlock.appendChild(dateToInput);
                    filterForm.appendChild(filterBlock);
                } else if (field.fieldType === 'NUMBER') {
                    const h2 = document.createElement('h2');
                    h2.textContent = field.fieldLabel + ':';
                    filterForm.appendChild(h2);
                    
                    const filterBlock = document.createElement('div');
                    filterBlock.className = 'filter-block';
                    const numberFromLabel = document.createElement('label');
                    numberFromLabel.className = 'from-to-style';
                    numberFromLabel.setAttribute('for', `filter-${escapeHtml(field.fieldName)}-from`);
                    numberFromLabel.textContent = 'Від:';
                    filterBlock.appendChild(numberFromLabel);
                    const numberFromInput = document.createElement('input');
                    numberFromInput.type = 'number';
                    numberFromInput.id = `filter-${escapeHtml(field.fieldName)}-from`;
                    numberFromInput.name = `${escapeHtml(field.fieldName)}From`;
                    numberFromInput.step = 'any';
                    numberFromInput.placeholder = 'Мінімум';
                    filterBlock.appendChild(numberFromInput);
                    const numberToLabel = document.createElement('label');
                    numberToLabel.className = 'from-to-style';
                    numberToLabel.setAttribute('for', `filter-${escapeHtml(field.fieldName)}-to`);
                    numberToLabel.textContent = 'До:';
                    filterBlock.appendChild(numberToLabel);
                    const numberToInput = document.createElement('input');
                    numberToInput.type = 'number';
                    numberToInput.id = `filter-${escapeHtml(field.fieldName)}-to`;
                    numberToInput.name = `${escapeHtml(field.fieldName)}To`;
                    numberToInput.step = 'any';
                    numberToInput.placeholder = 'Максимум';
                    filterBlock.appendChild(numberToInput);
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
                    const listBr = document.createElement('br');
                    selectItem.appendChild(listBr);
                    const listLabel = document.createElement('label');
                    listLabel.className = 'select-label-style';
                    listLabel.setAttribute('for', `filter-${escapeHtml(field.fieldName)}`);
                    listLabel.textContent = field.fieldLabel + ':';
                    selectItem.appendChild(listLabel);
                    const listSelect = document.createElement('select');
                    listSelect.id = `filter-${escapeHtml(field.fieldName)}`;
                    listSelect.name = escapeHtml(field.fieldName);
                    listSelect.multiple = true;
                    selectItem.appendChild(listSelect);
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
                    const textBr = document.createElement('br');
                    selectItem.appendChild(textBr);
                    const textLabel = document.createElement('label');
                    textLabel.className = 'select-label-style';
                    textLabel.setAttribute('for', `filter-${escapeHtml(field.fieldName)}`);
                    textLabel.textContent = field.fieldLabel + ':';
                    selectItem.appendChild(textLabel);
                    const textInput = document.createElement('input');
                    textInput.type = 'text';
                    textInput.id = `filter-${escapeHtml(field.fieldName)}`;
                    textInput.name = escapeHtml(field.fieldName);
                    textInput.placeholder = 'Пошук...';
                    selectItem.appendChild(textInput);
                    filterForm.appendChild(selectItem);
                } else if (field.fieldType === 'BOOLEAN') {
                    const selectItem = document.createElement('div');
                    selectItem.className = 'select-section-item';
                    const booleanBr = document.createElement('br');
                    selectItem.appendChild(booleanBr);
                    const booleanLabel = document.createElement('label');
                    booleanLabel.className = 'select-label-style';
                    booleanLabel.setAttribute('for', `filter-${escapeHtml(field.fieldName)}`);
                    booleanLabel.textContent = field.fieldLabel + ':';
                    selectItem.appendChild(booleanLabel);
                    const booleanSelect = document.createElement('select');
                    booleanSelect.id = `filter-${escapeHtml(field.fieldName)}`;
                    booleanSelect.name = escapeHtml(field.fieldName);
                    const optionAll = document.createElement('option');
                    optionAll.value = '';
                    optionAll.textContent = 'Всі';
                    booleanSelect.appendChild(optionAll);
                    const optionTrue = document.createElement('option');
                    optionTrue.value = 'true';
                    optionTrue.textContent = 'Так';
                    booleanSelect.appendChild(optionTrue);
                    const optionFalse = document.createElement('option');
                    optionFalse.value = 'false';
                    optionFalse.textContent = 'Ні';
                    booleanSelect.appendChild(optionFalse);
                    selectItem.appendChild(booleanSelect);
                    filterForm.appendChild(selectItem);
                }
            });
        }
    } finally {
        buildDynamicFilters._isBuilding = false;
    }
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
    Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);
    Object.assign(selectedFilters, parsedFilters);

    const savedSearchTerm = localStorage.getItem('searchTerm');
    if (savedSearchTerm && searchInput) {
        searchInput.value = savedSearchTerm;
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
