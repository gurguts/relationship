const PurchaseRenderer = (function() {
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
            th.removeEventListener('click', handleSortClick);
            th.addEventListener('click', handleSortClick);
        });
        updateSortIndicators(currentSort, currentDirection);
        
        function handleSortClick(event) {
            const th = event.currentTarget;
            const sortField = th.getAttribute('data-sort');
            
            const staticFields = ['quantity', 'unitPrice', 'totalPrice', 'currency', 'totalPriceEur', 'exchangeRate', 'paymentMethod', 'createdAt', 'updatedAt'];
            
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
    }
    
    function updatePagination(totalData, dataOnPage, totalPages, currentPageIndex, prevPageButton, nextPageButton, allClientInfo, paginationInfo) {
        if (allClientInfo) {
            allClientInfo.textContent = `Закупок: ${totalData}`;
        }
        if (paginationInfo) {
            paginationInfo.textContent = `
                Закупок на сторінці: ${dataOnPage},
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
    
    function renderPurchases(purchases, config) {
        const {
            tableBody,
            userMap,
            productMap,
            sourceMap,
            loadClientDetailsFn,
            showEditModalFn,
            deletePurchaseFn,
            currentClientTypeId,
            applyColumnWidthsFn
        } = config;
        
        if (!tableBody) return;
        
        tableBody.innerHTML = '';
        
        purchases.forEach(purchase => {
            const row = document.createElement('tr');
            row.classList.add('purchase-row');
            row.dataset.id = purchase.id;
            
            const clientName = purchase.client ? (purchase.client.company || purchase.client.person || '') : '';
            const userName = ClientUtils.findNameByIdFromMap(userMap, purchase.userId) || '';
            const productName = ClientUtils.findNameByIdFromMap(productMap, purchase.productId) || '';
            const sourceName = ClientUtils.findNameByIdFromMap(sourceMap, purchase.sourceId) || '';
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
            if (clientName) {
                companyCell.textContent = clientName;
                companyCell.style.cursor = 'pointer';
            } else {
                companyCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(companyCell);
            
            const userCell = document.createElement('td');
            userCell.setAttribute('data-label', 'Водій');
            if (userName) {
                userCell.textContent = userName;
            } else {
                userCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(userCell);
            
            const productCell = document.createElement('td');
            productCell.setAttribute('data-label', 'Товар');
            if (productName) {
                productCell.textContent = productName;
            } else {
                productCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(productCell);
            
            const sourceCell = document.createElement('td');
            sourceCell.setAttribute('data-label', 'Залучення');
            if (sourceName) {
                sourceCell.textContent = sourceName;
            } else {
                sourceCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(sourceCell);
            
            const quantityCell = document.createElement('td');
            quantityCell.setAttribute('data-label', 'Кількість');
            if (quantity) {
                quantityCell.textContent = quantity;
            } else {
                quantityCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(quantityCell);
            
            const unitPriceCell = document.createElement('td');
            unitPriceCell.setAttribute('data-label', 'Ціна за одиницю');
            if (unitPrice) {
                unitPriceCell.textContent = unitPrice;
            } else {
                unitPriceCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(unitPriceCell);
            
            const totalPriceCell = document.createElement('td');
            totalPriceCell.setAttribute('data-label', 'Всього сплачено');
            if (totalPrice) {
                totalPriceCell.textContent = totalPrice;
            } else {
                totalPriceCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(totalPriceCell);
            
            const currencyCell = document.createElement('td');
            currencyCell.setAttribute('data-label', 'Валюта');
            if (currency) {
                currencyCell.textContent = currency;
            } else {
                currencyCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(currencyCell);
            
            const totalPriceEurCell = document.createElement('td');
            totalPriceEurCell.setAttribute('data-label', 'Всього сплачено (EUR)');
            if (totalPriceEur) {
                totalPriceEurCell.textContent = totalPriceEur;
            } else {
                totalPriceEurCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(totalPriceEurCell);
            
            const exchangeRateCell = document.createElement('td');
            exchangeRateCell.setAttribute('data-label', 'Курс');
            if (exchangeRate) {
                exchangeRateCell.textContent = exchangeRate;
            } else {
                exchangeRateCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(exchangeRateCell);
            
            const paymentMethodCell = document.createElement('td');
            paymentMethodCell.setAttribute('data-label', 'Метод оплати');
            if (paymentMethod) {
                paymentMethodCell.textContent = paymentMethod;
            } else {
                paymentMethodCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
            row.appendChild(paymentMethodCell);
            
            const createdAtCell = document.createElement('td');
            createdAtCell.setAttribute('data-label', 'Дата створення');
            if (createdAt) {
                createdAtCell.textContent = createdAt;
            } else {
                createdAtCell.appendChild(ClientUtils.createEmptyCellSpan());
            }
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
            tableBody.appendChild(row);
            
            if (companyCell && purchase.client && loadClientDetailsFn) {
                companyCell.addEventListener('click', () => {
                    loadClientDetailsFn(purchase.client);
                });
            }
            
            if (!isReceived) {
                if (showEditModalFn) {
                    editButton.addEventListener('click', (e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        showEditModalFn(purchase);
                    });
                }
                
                if (deletePurchaseFn) {
                    deleteButton.addEventListener('click', (e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        deletePurchaseFn(purchase.id, purchase.isReceived);
                    });
                }
            }
        });
        
        if (applyColumnWidthsFn && currentClientTypeId) {
            setTimeout(() => {
                const storageKey = `purchase_${currentClientTypeId}`;
                applyColumnWidthsFn('client-list', storageKey);
            }, 0);
        }
    }
    
    return {
        buildPurchaseTable,
        renderPurchases,
        updatePagination,
        setupSortHandlers,
        updateSortIndicators,
        getDefaultSortDirection
    };
})();
