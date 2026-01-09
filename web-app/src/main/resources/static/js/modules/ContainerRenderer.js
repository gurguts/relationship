const ContainerRenderer = (function() {
    function attachCompanyCellClickHandler(companyCell, client, loadClientDetailsFn) {
        if (companyCell && loadClientDetailsFn && client) {
            companyCell.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                loadClientDetailsFn(client);
            });
        }
    }
    
    function getDefaultSortDirection(sortField) {
        if (sortField === 'updatedAt') {
            return 'DESC';
        }
        return 'ASC';
    }
    
    function updateSortIndicators(currentSort, currentDirection) {
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
    
    function setupSortHandlers(currentSort, currentDirection, onSortChange) {
        document.querySelectorAll('th[data-sort]').forEach(th => {
            th.removeEventListener('click', handleSortClick);
            th.addEventListener('click', handleSortClick);
        });
        updateSortIndicators(currentSort, currentDirection);
        
        function handleSortClick(event) {
            const th = event.currentTarget;
            const sortField = th.getAttribute('data-sort');
            
            const staticFields = ['quantity', 'updatedAt'];
            
            if (!sortField || !staticFields.includes(sortField)) {
                return;
            }
            
            let newSort = currentSort;
            let newDirection = currentDirection;
            
            if (currentSort === sortField) {
                newDirection = currentDirection === 'ASC' ? 'DESC' : 'ASC';
            } else {
                newSort = sortField;
                newDirection = getDefaultSortDirection(sortField);
            }
            
            if (onSortChange) {
                onSortChange(newSort, newDirection);
            }
        }
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
    }
    
    function updatePagination(totalData, dataOnPage, totalPages, currentPageIndex, prevPageButton, nextPageButton, allClientInfo, paginationInfo) {
        if (allClientInfo) {
            allClientInfo.textContent = `Тари: ${totalData}`;
        }
        if (paginationInfo) {
            paginationInfo.textContent = `
                Тари на сторінці: ${dataOnPage},
                Всього сторінок: ${totalPages},
                Поточна сторінка: ${currentPageIndex + 1}
            `;
        }
        
        if (prevPageButton) {
            prevPageButton.disabled = currentPageIndex <= 0;
        }
        if (nextPageButton) {
            nextPageButton.disabled = currentPageIndex >= totalPages - 1;
        }
    }
    
    function renderContainers(containers, config) {
        const {
            tableBody,
            userMap,
            loadClientDetailsFn,
            currentClientTypeId,
            applyColumnWidthsFn
        } = config;
        
        if (!tableBody) return;
        
        tableBody.innerHTML = '';
        
        containers.forEach(container => {
            const row = document.createElement('tr');
            row.classList.add('container-row');
            row.dataset.id = container.containerId;
            
            const clientName = container.client ? (container.client.company || container.client.person || '') : '';
            const containerName = container.containerName || '';
            const quantity = container.quantity ? container.quantity.toString() : '';
            const userName = ClientUtils.findNameByIdFromMap(userMap, container.userId) || '';
            const updatedAt = container.updatedAt ? new Date(container.updatedAt).toLocaleDateString('ua-UA') : '';
            
            const companyCell = document.createElement('td');
            companyCell.className = 'company-cell';
            companyCell.setAttribute('data-label', 'Назва клієнта');
            companyCell.style.cursor = 'pointer';
            if (clientName) {
                companyCell.textContent = clientName;
            } else {
                companyCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(companyCell);
            
            const containerCell = document.createElement('td');
            containerCell.setAttribute('data-label', 'Тип тари');
            if (containerName) {
                containerCell.textContent = containerName;
            } else {
                containerCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(containerCell);
            
            const quantityCell = document.createElement('td');
            quantityCell.setAttribute('data-label', 'Кількість');
            if (quantity) {
                quantityCell.textContent = quantity;
            } else {
                quantityCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(quantityCell);
            
            const userCell = document.createElement('td');
            userCell.setAttribute('data-label', 'Власник');
            if (userName) {
                userCell.textContent = userName;
            } else {
                userCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(userCell);
            
            const updatedAtCell = document.createElement('td');
            updatedAtCell.setAttribute('data-label', 'Оновлено');
            if (updatedAt) {
                updatedAtCell.textContent = updatedAt;
            } else {
                updatedAtCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(updatedAtCell);
            
            tableBody.appendChild(row);
            
            if (container.client && loadClientDetailsFn) {
                const companyCellInRow = row.querySelector('.company-cell');
                attachCompanyCellClickHandler(companyCellInRow, container.client, loadClientDetailsFn);
            }
        });
        
        if (applyColumnWidthsFn && currentClientTypeId) {
            setTimeout(() => {
                const storageKey = `containers_${currentClientTypeId}`;
                applyColumnWidthsFn('client-list', storageKey);
            }, 0);
        }
    }
    
    return {
        buildContainersTable,
        renderContainers,
        updatePagination,
        setupSortHandlers,
        updateSortIndicators,
        getDefaultSortDirection
    };
})();
