const ClientRenderer = (function() {
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
        sourceCell.setAttribute('data-label', CLIENT_MESSAGES.SOURCE_LABEL);
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
    
    function getDefaultSortDirection(sortField) {
        if (sortField === CLIENT_SORT_FIELDS.UPDATED_AT || sortField === CLIENT_SORT_FIELDS.CREATED_AT) {
            return CLIENT_SORT_DIRECTIONS.DESC;
        }
        return CLIENT_SORT_DIRECTIONS.ASC;
    }
    
    function updateSortIndicators(currentSort, currentDirection) {
        document.querySelectorAll('th[data-sort]').forEach(th => {
            const sortField = th.getAttribute('data-sort');
            th.classList.remove('sort-asc', 'sort-desc');
            
            if (currentSort === sortField) {
                if (currentDirection === CLIENT_SORT_DIRECTIONS.ASC) {
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
            th.addEventListener('click', (event) => handleSortClick(event, currentSort, currentDirection, onSortChange));
        });
        updateSortIndicators(currentSort, currentDirection);
    }
    
    function handleSortClick(event, currentSort, currentDirection, onSortChange) {
        const th = event.currentTarget;
        const sortField = th.getAttribute('data-sort');
        
        const validSortFields = [CLIENT_SORT_FIELDS.COMPANY, CLIENT_SORT_FIELDS.SOURCE, CLIENT_SORT_FIELDS.CREATED_AT, CLIENT_SORT_FIELDS.UPDATED_AT];
        if (!sortField || !validSortFields.includes(sortField)) {
            return;
        }
        
        let newSort = currentSort;
        let newDirection = currentDirection;
        
        if (currentSort === sortField) {
            newDirection = currentDirection === CLIENT_SORT_DIRECTIONS.ASC ? CLIENT_SORT_DIRECTIONS.DESC : CLIENT_SORT_DIRECTIONS.ASC;
        } else {
            newSort = sortField;
            newDirection = getDefaultSortDirection(sortField);
        }
        
        if (onSortChange) {
            onSortChange(newSort, newDirection);
        }
    }
    
    function buildDynamicTable(visibleFields, currentClientType, currentClientTypeId, onColumnResize) {
        const thead = document.querySelector('#client-list table thead tr');
        if (!thead) return;
        
        thead.textContent = '';

        const staticFields = (visibleFields || []).filter(f => f.isStatic);
        const dynamicFields = (visibleFields || []).filter(f => !f.isStatic);

        const hasCompanyStatic = staticFields.some(f => f.staticFieldName === CLIENT_STATIC_FIELDS.COMPANY);
        const hasSourceStatic = staticFields.some(f => f.staticFieldName === CLIENT_STATIC_FIELDS.SOURCE);

        const allFields = [...staticFields, ...dynamicFields];

        if (!hasCompanyStatic) {
            allFields.push({
                id: -1,
                fieldName: CLIENT_STATIC_FIELDS.COMPANY,
                fieldLabel: currentClientType ? currentClientType.nameFieldLabel : 'Компанія',
                isStatic: false,
                displayOrder: 0,
                columnWidth: CLIENT_CONSTANTS.DEFAULT_COLUMN_WIDTH,
                isSearchable: true
            });
        }
        
        if (!hasSourceStatic) {
            allFields.push({
                id: -2,
                fieldName: CLIENT_STATIC_FIELDS.SOURCE,
                fieldLabel: CLIENT_MESSAGES.SOURCE_LABEL,
                isStatic: false,
                displayOrder: 999,
                columnWidth: CLIENT_CONSTANTS.DEFAULT_COLUMN_WIDTH,
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
                if (field.staticFieldName === CLIENT_STATIC_FIELDS.COMPANY || field.staticFieldName === CLIENT_STATIC_FIELDS.SOURCE || 
                    field.staticFieldName === CLIENT_STATIC_FIELDS.CREATED_AT || field.staticFieldName === CLIENT_STATIC_FIELDS.UPDATED_AT) {
                    th.setAttribute('data-sort', field.staticFieldName);
                    th.style.cursor = 'pointer';
                }
            } else if (field.fieldName === CLIENT_STATIC_FIELDS.COMPANY || field.fieldName === CLIENT_STATIC_FIELDS.SOURCE) {
                th.setAttribute('data-sort', field.fieldName);
                th.style.cursor = 'pointer';
            }
            
            if (field.columnWidth && window.innerWidth > 1024) {
                th.style.width = field.columnWidth + 'px';
                th.style.minWidth = field.columnWidth + 'px';
                th.style.maxWidth = field.columnWidth + 'px';
            }
            th.style.flexShrink = '0';
            th.style.flexGrow = '0';
            thead.appendChild(th);
        });

        if (typeof onColumnResize === 'function' && currentClientTypeId) {
            setTimeout(() => {
                onColumnResize(currentClientTypeId);
            }, 0);
        }
    }
    
    function renderClientsWithDefaultFields(clients, tableBody, sourceMap, loadClientDetailsFn) {
        if (!tableBody) return;
        
        clients.forEach(client => {
            const row = document.createElement('tr');
            row.classList.add('client-row');

            const companyCell = createCompanyCell(client, 'Компанія');
            companyCell.setAttribute('data-sort', CLIENT_SORT_FIELDS.COMPANY);
            row.appendChild(companyCell);

            const sourceCell = createSourceCell(client, sourceMap);
            row.appendChild(sourceCell);

            tableBody.appendChild(row);
            
            attachCompanyCellClickHandler(companyCell, client, loadClientDetailsFn);
        });
    }
    
    async function renderClientsWithDynamicFields(clients, tableBody, visibleFields, currentClientType, sourceMap, availableSources, loadClientDetailsFn) {
        if (!tableBody) return;
        
        const staticFields = visibleFields.filter(f => f.isStatic);
        const dynamicFields = visibleFields.filter(f => !f.isStatic);

        const hasCompanyStatic = staticFields.some(f => f.staticFieldName === CLIENT_STATIC_FIELDS.COMPANY);
        const hasSourceStatic = staticFields.some(f => f.staticFieldName === CLIENT_STATIC_FIELDS.SOURCE);

        const allFields = [...staticFields, ...dynamicFields].sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        
        clients.forEach((client) => {
            const row = document.createElement('tr');
            row.classList.add('client-row');

            if (!hasCompanyStatic) {
                const nameFieldLabel = currentClientType ? currentClientType.nameFieldLabel : 'Компанія';
                const companyCell = createCompanyCell(client, nameFieldLabel);
                row.appendChild(companyCell);
            }
            
            const fieldValues = client.fieldValues || [];
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
                        case CLIENT_STATIC_FIELDS.COMPANY:
                            cell.className = 'company-cell';
                            cell.style.cursor = 'pointer';
                            if (client.company) {
                                cell.textContent = client.company;
                            } else {
                                cell.appendChild(ClientUtils.createEmptyCellSpan());
                            }
                            break;
                        case CLIENT_STATIC_FIELDS.SOURCE: {
                            const sourceId = client.sourceId ? (typeof client.sourceId === 'string' ? parseInt(client.sourceId) : client.sourceId) : null;
                            const sourceName = sourceId ? ClientUtils.findNameByIdFromMap(sourceMap, sourceId) : '';
                            if (sourceName) {
                                cell.textContent = sourceName;
                            } else {
                                cell.appendChild(ClientUtils.createEmptyCellSpan());
                            }
                            break;
                        }
                        case CLIENT_STATIC_FIELDS.CREATED_AT:
                            cell.setAttribute('data-sort', CLIENT_STATIC_FIELDS.CREATED_AT);
                            cell.style.cursor = 'pointer';
                            if (client.createdAt) {
                                cell.textContent = client.createdAt;
                            } else {
                                cell.appendChild(ClientUtils.createEmptyCellSpan());
                            }
                            break;
                        case CLIENT_STATIC_FIELDS.UPDATED_AT:
                            cell.setAttribute('data-sort', CLIENT_STATIC_FIELDS.UPDATED_AT);
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
                                const value = ClientFieldFormatter.formatFieldValue(v, field);
                                if (value) {
                                    cell.appendChild(document.createTextNode(value));
                                }
                            });
                        } else {
                            const value = ClientFieldFormatter.formatFieldValue(values[0], field);
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
    
    async function renderClients(clients, tableBody, currentClientTypeId, visibleFields, currentClientType, sourceMap, availableSources, loadClientDetailsFn, onColumnResize) {
        if (!tableBody) return;
        tableBody.textContent = '';
        
        if (currentClientTypeId && visibleFields && visibleFields.length > 0) {
            await renderClientsWithDynamicFields(clients, tableBody, visibleFields, currentClientType, sourceMap, availableSources, loadClientDetailsFn);
        } else {
            renderClientsWithDefaultFields(clients, tableBody, sourceMap, loadClientDetailsFn);
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
        buildDynamicTable,
        renderClients,
        renderClientsWithDynamicFields,
        renderClientsWithDefaultFields,
        updateSortIndicators,
        setupSortHandlers,
        getDefaultSortDirection,
        updatePagination
    };
})();
