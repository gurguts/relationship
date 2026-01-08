const RouteRenderer = (function() {
    function createCompanyCell(client, nameFieldLabel) {
        const companyCell = document.createElement('td');
        companyCell.setAttribute('data-label', nameFieldLabel);
        companyCell.className = 'company-cell';
        companyCell.style.cursor = 'pointer';
        if (client.company) {
            companyCell.textContent = client.company;
        } else {
            companyCell.appendChild(ClientUtils.createEmptyCellSpan());
        }
        return companyCell;
    }
    
    function createSourceCell(client, sourceMap) {
        const sourceCell = document.createElement('td');
        sourceCell.setAttribute('data-label', 'Залучення');
        const sourceId = client.sourceId ? (typeof client.sourceId === 'string' ? parseInt(client.sourceId) : client.sourceId) : null;
        const sourceName = sourceId ? ClientUtils.findNameByIdFromMap(sourceMap, sourceId) : '';
        if (sourceName) {
            sourceCell.textContent = sourceName;
        } else {
            sourceCell.appendChild(ClientUtils.createEmptyCellSpan());
        }
        return sourceCell;
    }
    
    function attachCompanyCellClickHandler(companyCell, client, loadClientDetailsFn) {
        if (companyCell && loadClientDetailsFn) {
            companyCell.addEventListener('click', () => {
                loadClientDetailsFn(client);
            });
        }
    }
    
    function attachPurchaseButtonHandler(button, clientId, openCreatePurchaseModalFn) {
        if (button && openCreatePurchaseModalFn) {
            button.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                openCreatePurchaseModalFn(clientId);
            });
        }
    }
    
    function attachContainerButtonHandler(button, clientId, openCreateContainerModalFn) {
        if (button && openCreateContainerModalFn) {
            button.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                openCreateContainerModalFn(clientId);
            });
        }
    }
    
    function formatFieldValue(fieldValue, field) {
        if (!fieldValue) return '';
        
        switch (field.fieldType) {
            case 'TEXT':
            case 'PHONE':
                return fieldValue.valueText || '';
            case 'NUMBER':
                return fieldValue.valueNumber || '';
            case 'DATE':
                return fieldValue.valueDate || '';
            case 'BOOLEAN':
                if (fieldValue.valueBoolean === true) return 'Так';
                if (fieldValue.valueBoolean === false) return 'Ні';
                return '';
            case 'LIST':
                return fieldValue.valueListValue || '';
            default:
                return '';
        }
    }
    
    function formatFieldValueForModal(fieldValue, field) {
        if (!fieldValue) return '';
        
        switch (field.fieldType) {
            case 'TEXT':
                return ClientUtils.escapeHtml(fieldValue.valueText || '');
            case 'PHONE':
                const phone = fieldValue.valueText || '';
                if (phone) {
                    const escapedPhone = ClientUtils.escapeHtml(phone);
                    return `<a href="tel:${escapedPhone}">${escapedPhone}</a>`;
                }
                return '';
            case 'NUMBER':
                return ClientUtils.escapeHtml(String(fieldValue.valueNumber || ''));
            case 'DATE':
                return ClientUtils.escapeHtml(fieldValue.valueDate || '');
            case 'BOOLEAN':
                if (fieldValue.valueBoolean === true) return 'Так';
                if (fieldValue.valueBoolean === false) return 'Ні';
                return '';
            case 'LIST':
                return ClientUtils.escapeHtml(fieldValue.valueListValue || '');
            default:
                return '';
        }
    }
    
    function getDefaultSortDirection(sortField) {
        if (sortField === 'updatedAt' || sortField === 'createdAt') {
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
            th.addEventListener('click', (event) => {
                const sortField = th.getAttribute('data-sort');
                const validSortFields = ['company', 'sourceId', 'createdAt', 'updatedAt'];
                if (!sortField || !validSortFields.includes(sortField)) {
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
            });
        });
        updateSortIndicators(currentSort, currentDirection);
    }
    
    function buildDynamicTable(visibleFields, currentClientType, currentClientTypeId, onColumnResize) {
        const thead = document.querySelector('#client-list table thead tr');
        if (!thead) return;
        
        thead.innerHTML = '';

        const actionsTh = document.createElement('th');
        actionsTh.textContent = 'Дії';
        actionsTh.style.width = '150px';
        actionsTh.style.minWidth = '150px';
        actionsTh.style.maxWidth = '150px';
        actionsTh.style.flexShrink = '0';
        actionsTh.style.flexGrow = '0';
        thead.appendChild(actionsTh);

        const staticFields = (visibleFields || []).filter(f => f.isStatic);
        const dynamicFields = (visibleFields || []).filter(f => !f.isStatic);

        const hasCompanyStatic = staticFields.some(f => f.staticFieldName === 'company');
        const hasSourceStatic = staticFields.some(f => f.staticFieldName === 'source');

        const allFields = [...staticFields, ...dynamicFields];

        if (!hasCompanyStatic) {
            allFields.push({
                id: -1,
                fieldName: 'company',
                fieldLabel: currentClientType ? currentClientType.nameFieldLabel : 'Компанія',
                isStatic: false,
                displayOrder: 0,
                columnWidth: 200,
                isSearchable: true
            });
        }
        
        if (!hasSourceStatic) {
            allFields.push({
                id: -2,
                fieldName: 'source',
                fieldLabel: 'Залучення',
                isStatic: false,
                displayOrder: 999,
                columnWidth: 200,
                isSearchable: false
            });
        }

        allFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));

        allFields.forEach(field => {
            const th = document.createElement('th');
            th.textContent = field.fieldLabel;
            th.setAttribute('data-field-id', field.id);
            
            if (field.isStatic) {
                th.setAttribute('data-static-field', field.staticFieldName);
                if (field.staticFieldName === 'company' || field.staticFieldName === 'source' || field.staticFieldName === 'createdAt' || field.staticFieldName === 'updatedAt') {
                    th.setAttribute('data-sort', field.staticFieldName === 'source' ? 'sourceId' : field.staticFieldName);
                    th.style.cursor = 'pointer';
                }
            } else if (field.fieldName === 'company' || field.fieldName === 'source') {
                th.setAttribute('data-sort', field.fieldName === 'source' ? 'sourceId' : field.fieldName);
                th.style.cursor = 'pointer';
            }
            
            if (field.columnWidth) {
                th.style.width = field.columnWidth + 'px';
                th.style.minWidth = field.columnWidth + 'px';
                th.style.maxWidth = field.columnWidth + 'px';
            }
            th.style.flexShrink = '0';
            th.style.flexGrow = '0';
            thead.appendChild(th);
        });
        
        if (onColumnResize) {
            onColumnResize(currentClientTypeId);
        }
    }
    
    async function renderClientsWithDynamicFields(clients, tableBody, visibleFields, currentClientType, sourceMap, loadClientFieldValuesFn, loadClientDetailsFn, openCreatePurchaseModalFn, openCreateContainerModalFn) {
        const fieldValuesPromises = clients.map(client => loadClientFieldValuesFn(client.id));
        const allFieldValues = await Promise.all(fieldValuesPromises);

        const staticFields = visibleFields.filter(f => f.isStatic);
        const dynamicFields = visibleFields.filter(f => !f.isStatic);

        const hasCompanyStatic = staticFields.some(f => f.staticFieldName === 'company');
        const hasSourceStatic = staticFields.some(f => f.staticFieldName === 'source');

        const allFields = [...staticFields, ...dynamicFields].sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        
        clients.forEach((client, index) => {
            const row = document.createElement('tr');
            row.classList.add('client-row');
            
            const actionsCell = document.createElement('td');
            actionsCell.className = 'button-td';
            actionsCell.setAttribute('data-label', 'Дії');
            
            const purchaseButton = document.createElement('button');
            purchaseButton.className = 'purchase-button';
            purchaseButton.setAttribute('data-client-id', client.id);
            purchaseButton.textContent = 'Закупка';
            attachPurchaseButtonHandler(purchaseButton, client.id, openCreatePurchaseModalFn);
            actionsCell.appendChild(purchaseButton);
            
            const containerButton = document.createElement('button');
            containerButton.className = 'container-button';
            containerButton.setAttribute('data-client-id', client.id);
            containerButton.textContent = 'Тара';
            attachContainerButtonHandler(containerButton, client.id, openCreateContainerModalFn);
            actionsCell.appendChild(containerButton);
            
            row.appendChild(actionsCell);

            if (!hasCompanyStatic) {
                const nameFieldLabel = currentClientType ? currentClientType.nameFieldLabel : 'Компанія';
                const companyCell = createCompanyCell(client, nameFieldLabel);
                row.appendChild(companyCell);
            }
            
            const fieldValues = allFieldValues[index];
            const fieldValuesMap = new Map();
            fieldValues.forEach(fv => {
                if (!fieldValuesMap.has(fv.fieldId)) {
                    fieldValuesMap.set(fv.fieldId, []);
                }
                fieldValuesMap.get(fv.fieldId).push(fv);
            });
            
            client._fieldValues = fieldValues;

            allFields.forEach(field => {
                const cell = document.createElement('td');
                cell.setAttribute('data-label', field.fieldLabel);
                
                if (field.isStatic) {
                    switch (field.staticFieldName) {
                        case 'company':
                            cell.className = 'company-cell';
                            cell.style.cursor = 'pointer';
                            if (client.company) {
                                cell.textContent = client.company;
                            } else {
                                cell.appendChild(ClientUtils.createEmptyCellSpan());
                            }
                            break;
                        case 'source': {
                            const sourceId = client.sourceId ? (typeof client.sourceId === 'string' ? parseInt(client.sourceId) : client.sourceId) : null;
                            const sourceName = sourceId ? ClientUtils.findNameByIdFromMap(sourceMap, sourceId) : '';
                            if (sourceName) {
                                cell.textContent = sourceName;
                            } else {
                                cell.appendChild(ClientUtils.createEmptyCellSpan());
                            }
                            break;
                        }
                        case 'createdAt':
                            cell.setAttribute('data-sort', 'createdAt');
                            cell.style.cursor = 'pointer';
                            if (client.createdAt) {
                                cell.textContent = client.createdAt;
                            } else {
                                cell.appendChild(ClientUtils.createEmptyCellSpan());
                            }
                            break;
                        case 'updatedAt':
                            cell.setAttribute('data-sort', 'updatedAt');
                            cell.style.cursor = 'pointer';
                            if (client.updatedAt) {
                                cell.textContent = client.updatedAt;
                            } else {
                                cell.appendChild(ClientUtils.createEmptyCellSpan());
                            }
                            break;
                    }
                } else {
                    const values = fieldValuesMap.get(field.id) || [];
                    
                    if (values.length > 0) {
                        if (field.allowMultiple) {
                            values.forEach((v, index) => {
                                if (index > 0) {
                                    cell.appendChild(document.createElement('br'));
                                }
                                const value = formatFieldValue(v, field);
                                if (value) {
                                    cell.appendChild(document.createTextNode(value));
                                }
                            });
                        } else {
                            const value = formatFieldValue(values[0], field);
                            if (value) {
                                cell.textContent = value;
                            } else {
                                cell.appendChild(ClientUtils.createEmptyCellSpan());
                            }
                        }
                    } else {
                        cell.appendChild(ClientUtils.createEmptyCellSpan());
                    }
                }
                
                row.appendChild(cell);
            });

            if (!hasSourceStatic) {
                const sourceCell = createSourceCell(client, sourceMap);
                row.appendChild(sourceCell);
            }
            
            tableBody.appendChild(row);
            
            const companyCell = row.querySelector('.company-cell');
            attachCompanyCellClickHandler(companyCell, client, loadClientDetailsFn);
        });
    }
    
    function renderClientsWithDefaultFields(clients, tableBody, sourceMap, loadClientDetailsFn) {
        clients.forEach(client => {
            const row = document.createElement('tr');
            row.classList.add('client-row');
            
            const companyCell = createCompanyCell(client, 'Компанія');
            row.appendChild(companyCell);
            
            const sourceCell = createSourceCell(client, sourceMap);
            row.appendChild(sourceCell);
            
            const createdAtCell = document.createElement('td');
            createdAtCell.setAttribute('data-label', 'Створено');
            createdAtCell.setAttribute('data-sort', 'createdAt');
            createdAtCell.style.cursor = 'pointer';
            if (client.createdAt) {
                createdAtCell.textContent = client.createdAt;
            } else {
                createdAtCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(createdAtCell);
            
            const updatedAtCell = document.createElement('td');
            updatedAtCell.setAttribute('data-label', 'Оновлено');
            updatedAtCell.setAttribute('data-sort', 'updatedAt');
            updatedAtCell.style.cursor = 'pointer';
            if (client.updatedAt) {
                updatedAtCell.textContent = client.updatedAt;
            } else {
                updatedAtCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(updatedAtCell);
            
            tableBody.appendChild(row);
            
            attachCompanyCellClickHandler(companyCell, client, loadClientDetailsFn);
        });
    }
    
    async function renderClients(clients, tableBody, currentClientTypeId, visibleFields, currentClientType, sourceMap, loadClientFieldValuesFn, loadClientDetailsFn, openCreatePurchaseModalFn, openCreateContainerModalFn, onColumnResize) {
        if (!tableBody) return;
        tableBody.innerHTML = '';
        
        if (currentClientTypeId && visibleFields && visibleFields.length > 0) {
            await renderClientsWithDynamicFields(clients, tableBody, visibleFields, currentClientType, sourceMap, loadClientFieldValuesFn, loadClientDetailsFn, openCreatePurchaseModalFn, openCreateContainerModalFn);
        } else {
            renderClientsWithDefaultFields(clients, tableBody, sourceMap, loadClientDetailsFn);
        }
        
        if (onColumnResize && currentClientTypeId) {
            setTimeout(() => {
                onColumnResize(currentClientTypeId);
            }, 0);
        }
    }
    
    function updatePagination(totalClients, clientsOnPage, totalPages, currentPageIndex, prevPageButton, nextPageButton, allClientInfo, paginationInfo) {
        if (allClientInfo) {
            allClientInfo.textContent = `Клієнтів: ${totalClients}`;
        }
        if (paginationInfo) {
            paginationInfo.textContent = `Клієнтів на сторінці: ${clientsOnPage}, Всього сторінок: ${totalPages}, Поточна сторінка: ${currentPageIndex + 1}`;
        }
        if (prevPageButton) {
            prevPageButton.disabled = currentPageIndex <= 0;
        }
        if (nextPageButton) {
            nextPageButton.disabled = currentPageIndex >= totalPages - 1;
        }
    }
    
    return {
        createCompanyCell,
        createSourceCell,
        attachCompanyCellClickHandler,
        attachPurchaseButtonHandler,
        attachContainerButtonHandler,
        formatFieldValue,
        formatFieldValueForModal,
        buildDynamicTable,
        renderClients,
        renderClientsWithDynamicFields,
        renderClientsWithDefaultFields,
        setupSortHandlers,
        updatePagination
    };
})();
