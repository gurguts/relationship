const DeclarantRenderer = (function() {
    function populateCarriers(selectId, carrierMap) {
        const select = document.getElementById(selectId);
        if (!select) return;
        select.textContent = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = 'Оберіть перевізника';
        select.appendChild(defaultOption);
        for (const [id, carrier] of carrierMap.entries()) {
            const option = document.createElement('option');
            option.value = id;
            option.textContent = carrier.companyName;
            select.appendChild(option);
        }
    }
    
    function populateVehicleSenders(selectId, vehicleSenderMap) {
        const select = document.getElementById(selectId);
        if (!select) return;
        select.textContent = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = 'Оберіть відправника';
        select.appendChild(defaultOption);
        for (const [id, sender] of vehicleSenderMap.entries()) {
            const option = document.createElement('option');
            option.value = id;
            option.textContent = sender.name;
            select.appendChild(option);
        }
    }
    
    function populateVehicleReceivers(selectId, vehicleReceiverMap) {
        const select = document.getElementById(selectId);
        if (!select) return;
        select.textContent = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = 'Оберіть отримувача';
        select.appendChild(defaultOption);
        for (const [id, receiver] of vehicleReceiverMap.entries()) {
            const option = document.createElement('option');
            option.value = id;
            option.textContent = receiver.name;
            select.appendChild(option);
        }
    }
    
    function populateVehicleTerminals(selectId, vehicleTerminalMap) {
        const select = document.getElementById(selectId);
        if (!select) return;
        select.textContent = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = 'Оберіть термінал';
        select.appendChild(defaultOption);
        for (const [id, terminal] of vehicleTerminalMap.entries()) {
            const option = document.createElement('option');
            option.value = id;
            option.textContent = terminal.name;
            select.appendChild(option);
        }
    }
    
    function populateVehicleDestinationCountries(selectId, vehicleDestinationCountryMap) {
        const select = document.getElementById(selectId);
        if (!select) return;
        select.textContent = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = 'Оберіть країну';
        select.appendChild(defaultOption);
        for (const [id, country] of vehicleDestinationCountryMap.entries()) {
            const option = document.createElement('option');
            option.value = id;
            option.textContent = country.name;
            select.appendChild(option);
        }
    }
    
    function populateVehicleDestinationPlaces(selectId, vehicleDestinationPlaceMap) {
        const select = document.getElementById(selectId);
        if (!select) return;
        select.textContent = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = 'Оберіть місце';
        select.appendChild(defaultOption);
        for (const [id, place] of vehicleDestinationPlaceMap.entries()) {
            const option = document.createElement('option');
            option.value = id;
            option.textContent = place.name;
            select.appendChild(option);
        }
    }
    
    function populateAccounts(selectId, accounts, customSelects) {
        const select = document.getElementById(selectId);
        if (!select) return;
        
        select.textContent = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = 'Оберіть рахунок';
        select.appendChild(defaultOption);
        accounts.forEach(account => {
            const option = document.createElement('option');
            option.value = account.id;
            option.textContent = account.name || `Рахунок #${account.id}`;
            select.appendChild(option);
        });
        
        if (customSelects && typeof createCustomSelect === 'function') {
            if (!customSelects[selectId]) {
                customSelects[selectId] = createCustomSelect(select);
            }
            const accountData = [
                { id: '', name: 'Оберіть рахунок' },
                ...accounts.map(account => ({ id: account.id, name: account.name || `Рахунок #${account.id}` }))
            ];
            if (customSelects[selectId]) {
                customSelects[selectId].populate(accountData);
            }
        }
    }
    
    function populateCategories(selectId, categories, customSelects) {
        const select = document.getElementById(selectId);
        if (!select) return;
        
        select.textContent = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = 'Оберіть категорію';
        select.appendChild(defaultOption);
        categories.forEach(category => {
            const option = document.createElement('option');
            option.value = category.id;
            option.textContent = category.name;
            select.appendChild(option);
        });
        
        if (customSelects && typeof createCustomSelect === 'function') {
            if (!customSelects[selectId]) {
                customSelects[selectId] = createCustomSelect(select);
            }
            const categoryData = [
                { id: '', name: 'Оберіть категорію' },
                ...categories.map(category => ({ id: category.id, name: category.name }))
            ];
            if (customSelects[selectId]) {
                customSelects[selectId].populate(categoryData);
            }
        }
    }
    
    function populateCurrencies(selectId, accountId, accounts) {
        const select = document.getElementById(selectId);
        if (!select) return;
        select.textContent = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = 'Оберіть валюту';
        select.appendChild(defaultOption);
        
        if (!accountId) return;
        
        const account = accounts.find(a => a.id === parseInt(accountId));
        if (account && account.currencies) {
            account.currencies.forEach(currency => {
                const option = document.createElement('option');
                option.value = currency;
                option.textContent = currency;
                select.appendChild(option);
            });
        }
    }
    
    function formatCarrier(carrier) {
        return carrier?.companyName || '-';
    }
    
    async function renderVehicles(vehicles, currentPage, pageSize, totalElements, totalPages, productMap, warehouseMap, carrierMap, userMap, onVehicleClick) {
        const vehiclesTbody = document.getElementById('vehicles-tbody');
        const vehiclesCount = document.getElementById('vehicles-count');
        
        if (!vehiclesTbody) {
            return;
        }
        
        if (vehiclesCount) {
            const start = currentPage * pageSize + 1;
            const end = Math.min((currentPage + 1) * pageSize, totalElements);
            vehiclesCount.textContent = totalElements > 0 
                ? `Показано ${start}-${end} з ${totalElements} ${totalElements === 1 ? 'машини' : 'машин'}`
                : '0 машин';
        }
        
        if (!vehicles || vehicles.length === 0) {
            vehiclesTbody.textContent = '';
            const row = document.createElement('tr');
            row.className = 'loading-row';
            const cell = document.createElement('td');
            cell.colSpan = 26;
            cell.style.textAlign = 'center';
            cell.style.color = 'var(--text-muted)';
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            row.appendChild(cell);
            vehiclesTbody.appendChild(row);
            return;
        }
        
        vehiclesTbody.textContent = '';
        
        vehicles.forEach((vehicle) => {
            const row = document.createElement('tr');
            row.dataset.vehicleId = vehicle.id.toString();
            const rowClickHandler = () => onVehicleClick(vehicle.id);
            row.addEventListener('click', rowClickHandler);
            row._clickHandler = rowClickHandler;
            
            const createCell = (text, label, style) => {
                const cell = document.createElement('td');
                cell.setAttribute('data-label', label);
                if (style) {
                    Object.assign(cell.style, style);
                }
                cell.textContent = text;
                return cell;
            };
            
            const productsTotalCost = (vehicle.items && Array.isArray(vehicle.items) && vehicle.items.length > 0)
                ? DeclarantCalculations.calculateProductsTotalCost(vehicle)
                : 0;
            const totalExpensesValue = vehicle.totalExpenses != null ? parseFloat(vehicle.totalExpenses) : 0;
            const expensesTotal = Math.max(0, totalExpensesValue - productsTotalCost);
            
            row.appendChild(createCell(vehicle.vehicleNumber || '-', 'Номер машини'));
            row.appendChild(createCell(`${DeclarantUtils.formatNumber(productsTotalCost, 2)} EUR`, 'Витрати на товар', { fontWeight: '600', color: 'var(--primary)' }));
            row.appendChild(createCell(`${DeclarantUtils.formatNumber(expensesTotal, 2)} EUR`, 'Витрати на машину', { fontWeight: '600', color: 'var(--primary)' }));
            row.appendChild(createCell(`${DeclarantUtils.formatNumber(vehicle.totalExpenses, 2)} EUR`, 'Загальні витрати', { fontWeight: '600', color: 'var(--primary)' }));
            row.appendChild(createCell(`${DeclarantUtils.formatNumber(vehicle.totalIncome, 2)} EUR`, 'Загальний дохід', { fontWeight: '600', color: 'var(--success)' }));
            const marginValue = vehicle.margin != null ? parseFloat(vehicle.margin) : 0;
            row.appendChild(createCell(`${DeclarantUtils.formatNumber(vehicle.margin, 2)} EUR`, 'Маржа', { fontWeight: '600', color: marginValue >= 0 ? 'var(--success)' : 'var(--danger)' }));
            row.appendChild(createCell(DeclarantUtils.formatDate(vehicle.shipmentDate), 'Дата відвантаження'));
            row.appendChild(createCell(DeclarantUtils.formatDate(vehicle.customsDate), 'Дата замитнення'));
            row.appendChild(createCell(vehicle.invoiceUa || '-', 'Інвойс УА'));
            row.appendChild(createCell(vehicle.invoiceEu || '-', 'Інвойс ЄС'));
            row.appendChild(createCell(vehicle.senderName || '-', 'Відправник'));
            row.appendChild(createCell(DeclarantUtils.formatDate(vehicle.invoiceUaDate), 'Дата інвойсу УА'));
            row.appendChild(createCell(DeclarantUtils.formatDate(vehicle.invoiceEuDate), 'Дата інвойсу ЄС'));
            row.appendChild(createCell(vehicle.receiverName || '-', 'Отримувач'));
            row.appendChild(createCell(vehicle.destinationCountryName || '-', 'Країна призначення'));
            row.appendChild(createCell(vehicle.destinationPlaceName || '-', 'Місце призначення'));
            row.appendChild(createCell(vehicle.product || '-', 'Товар'));
            row.appendChild(createCell(vehicle.productQuantity || '-', 'Кількість товару'));
            row.appendChild(createCell(vehicle.declarationNumber || '-', 'Номер декларації'));
            row.appendChild(createCell(vehicle.terminalName || '-', 'Термінал'));
            row.appendChild(createCell(vehicle.driverFullName || '-', 'Водій (ПІБ)'));
            row.appendChild(createCell(DeclarantUtils.formatBoolean(vehicle.eur1), 'EUR1'));
            row.appendChild(createCell(DeclarantUtils.formatBoolean(vehicle.fito), 'FITO'));
            row.appendChild(createCell(DeclarantUtils.formatDate(vehicle.customsClearanceDate), 'Дата розмитнення'));
            row.appendChild(createCell(DeclarantUtils.formatDate(vehicle.unloadingDate), 'Дата вивантаження'));
            row.appendChild(createCell(formatCarrier(vehicle.carrier), 'Перевізник'));
            row.appendChild(createCell(vehicle.description || '-', 'Коментар'));
            row.appendChild(createCell(vehicle.additionalDescription || '-', 'Додатковий опис'));
            const managerName = userMap && vehicle.managerId != null ? (DeclarantUtils.findNameByIdFromMap(userMap, vehicle.managerId) || '-') : '-';
            row.appendChild(createCell(managerName, 'Менеджер'));
            
            vehiclesTbody.appendChild(row);
        });
    }
    
    function renderPagination(currentPage, totalPages, onPageChange) {
        const vehiclesPagination = document.getElementById('vehicles-pagination');
        if (!vehiclesPagination) return;
        
        if (totalPages <= 1) {
            vehiclesPagination.textContent = '';
            return;
        }
        
        vehiclesPagination.textContent = '';
        const paginationDiv = document.createElement('div');
        paginationDiv.className = 'pagination';
        
        const firstBtn = document.createElement('button');
        firstBtn.className = 'pagination-btn';
        firstBtn.disabled = currentPage === 0;
        const firstSpan = document.createElement('span');
        firstSpan.textContent = '«';
        firstBtn.appendChild(firstSpan);
        const firstBtnHandler = () => onPageChange(0);
        firstBtn.addEventListener('click', firstBtnHandler);
        firstBtn._clickHandler = firstBtnHandler;
        paginationDiv.appendChild(firstBtn);
        
        const prevBtn = document.createElement('button');
        prevBtn.className = 'pagination-btn';
        prevBtn.disabled = currentPage === 0;
        const prevSpan = document.createElement('span');
        prevSpan.textContent = '‹';
        prevBtn.appendChild(prevSpan);
        const prevBtnHandler = () => onPageChange(currentPage - 1);
        prevBtn.addEventListener('click', prevBtnHandler);
        prevBtn._clickHandler = prevBtnHandler;
        paginationDiv.appendChild(prevBtn);
        
        const startPage = Math.max(0, currentPage - 2);
        const endPage = Math.min(totalPages - 1, currentPage + 2);
        
        if (startPage > 0) {
            const firstPageBtn = document.createElement('button');
            firstPageBtn.className = 'pagination-btn';
            firstPageBtn.textContent = '1';
            const firstPageBtnHandler = () => onPageChange(0);
            firstPageBtn.addEventListener('click', firstPageBtnHandler);
            firstPageBtn._clickHandler = firstPageBtnHandler;
            paginationDiv.appendChild(firstPageBtn);
            if (startPage > 1) {
                const ellipsis1 = document.createElement('span');
                ellipsis1.className = 'pagination-ellipsis';
                ellipsis1.textContent = '...';
                paginationDiv.appendChild(ellipsis1);
            }
        }
        
        for (let i = startPage; i <= endPage; i++) {
            const pageBtn = document.createElement('button');
            pageBtn.className = 'pagination-btn';
            if (i === currentPage) {
                pageBtn.classList.add('active');
            }
            pageBtn.textContent = (i + 1).toString();
            const pageBtnHandler = () => onPageChange(i);
            pageBtn.addEventListener('click', pageBtnHandler);
            pageBtn._clickHandler = pageBtnHandler;
            paginationDiv.appendChild(pageBtn);
        }
        
        if (endPage < totalPages - 1) {
            if (endPage < totalPages - 2) {
                const ellipsis2 = document.createElement('span');
                ellipsis2.className = 'pagination-ellipsis';
                ellipsis2.textContent = '...';
                paginationDiv.appendChild(ellipsis2);
            }
            const lastPageBtn = document.createElement('button');
            lastPageBtn.className = 'pagination-btn';
            lastPageBtn.textContent = totalPages.toString();
            const lastPageBtnHandler = () => onPageChange(totalPages - 1);
            lastPageBtn.addEventListener('click', lastPageBtnHandler);
            lastPageBtn._clickHandler = lastPageBtnHandler;
            paginationDiv.appendChild(lastPageBtn);
        }
        
        const nextBtn = document.createElement('button');
        nextBtn.className = 'pagination-btn';
        nextBtn.disabled = currentPage >= totalPages - 1;
        const nextSpan = document.createElement('span');
        nextSpan.textContent = '›';
        nextBtn.appendChild(nextSpan);
        const nextBtnHandler = () => onPageChange(currentPage + 1);
        nextBtn.addEventListener('click', nextBtnHandler);
        nextBtn._clickHandler = nextBtnHandler;
        paginationDiv.appendChild(nextBtn);
        
        const lastBtn = document.createElement('button');
        lastBtn.className = 'pagination-btn';
        lastBtn.disabled = currentPage >= totalPages - 1;
        const lastSpan = document.createElement('span');
        lastSpan.textContent = '»';
        lastBtn.appendChild(lastSpan);
        const lastBtnHandler = () => onPageChange(totalPages - 1);
        lastBtn.addEventListener('click', lastBtnHandler);
        lastBtn._clickHandler = lastBtnHandler;
        paginationDiv.appendChild(lastBtn);
        
        vehiclesPagination.appendChild(paginationDiv);
    }
    
    function renderVehicleDetails(vehicle, productMap, warehouseMap, currentVehicleItems) {
        const vehicleItemsTbody = document.getElementById('vehicle-items-tbody');
        if (!vehicleItemsTbody) return;
        
        vehicleItemsTbody.textContent = '';
        
        if (!vehicle.items || vehicle.items.length === 0) {
            const row = document.createElement('tr');
            row.className = 'loading-row';
            const cell = document.createElement('td');
            cell.colSpan = 6;
            cell.style.textAlign = 'center';
            cell.style.color = 'var(--text-muted)';
            cell.textContent = 'Товари ще не додані';
            row.appendChild(cell);
            vehicleItemsTbody.appendChild(row);
        } else {
            let totalQuantity = 0;
            let totalCost = 0;
            
            vehicle.items.forEach(item => {
                const productName = DeclarantUtils.findNameByIdFromMap(productMap, item.productId) || 'Невідомий товар';
                const warehouseName = DeclarantUtils.findNameByIdFromMap(warehouseMap, item.warehouseId) || 'Невідомий склад';

                if (currentVehicleItems) {
                    currentVehicleItems.set(Number(item.withdrawalId), {
                        ...item,
                        productName,
                        warehouseName
                    });
                }

                const row = document.createElement('tr');
                row.className = 'vehicle-item-row';
                row.setAttribute('data-item-id', item.withdrawalId.toString());
                
                const createCell = (text, label, style) => {
                    const cell = document.createElement('td');
                    cell.setAttribute('data-label', label);
                    if (style) {
                        Object.assign(cell.style, style);
                    }
                    cell.textContent = text;
                    return cell;
                };
                
                row.appendChild(createCell(productName, 'Товар'));
                row.appendChild(createCell(warehouseName, 'Склад'));
                row.appendChild(createCell(`${DeclarantUtils.formatNumber(item.quantity, 2)} кг`, 'Кількість'));
                row.appendChild(createCell(`${DeclarantUtils.formatNumber(item.unitPriceEur, 6)} EUR`, 'Ціна за кг', { textAlign: 'right' }));
                row.appendChild(createCell(`${DeclarantUtils.formatNumber(item.totalCostEur, 6)} EUR`, 'Загальна вартість', { textAlign: 'right', fontWeight: '600', color: 'var(--primary)' }));
                row.appendChild(createCell(item.withdrawalDate || vehicle.shipmentDate || '-', 'Дата списання'));
                
                const rowClickHandler = () => {
                    if (typeof window.openEditVehicleItemModal === 'function') {
                        window.openEditVehicleItemModal(item.withdrawalId);
                    }
                };
                row.addEventListener('click', rowClickHandler);
                row._clickHandler = rowClickHandler;
                
                vehicleItemsTbody.appendChild(row);
                
                totalQuantity += parseFloat(item.quantity) || 0;
                totalCost += parseFloat(item.totalCostEur) || 0;
            });
            
            const averagePrice = totalQuantity > 0 ? totalCost / totalQuantity : 0;
            
            const totalRow = document.createElement('tr');
            totalRow.className = 'vehicle-items-total-row';
            totalRow.style.fontWeight = '600';
            totalRow.style.backgroundColor = '#f5f5f5';
            
            const createTotalCell = (text, style) => {
                const cell = document.createElement('td');
                if (style) {
                    Object.assign(cell.style, style);
                }
                cell.textContent = text;
                return cell;
            };
            
            totalRow.appendChild(createTotalCell('Загалом', { fontWeight: '700' }));
            totalRow.appendChild(createTotalCell(''));
            totalRow.appendChild(createTotalCell(`${DeclarantUtils.formatNumber(totalQuantity, 2)} кг`, { fontWeight: '600' }));
            totalRow.appendChild(createTotalCell(`${DeclarantUtils.formatNumber(averagePrice, 6)} EUR`, { textAlign: 'right', fontWeight: '600' }));
            totalRow.appendChild(createTotalCell(`${DeclarantUtils.formatNumber(totalCost, 6)} EUR`, { textAlign: 'right', fontWeight: '700', color: 'var(--primary)' }));
            totalRow.appendChild(createTotalCell(''));
            
            vehicleItemsTbody.appendChild(totalRow);
        }
    }
    
    function renderVehicleExpenses(expenses, accounts, categoryNameMap) {
        const vehicleExpensesTbody = document.getElementById('vehicle-expenses-tbody');
        if (!vehicleExpensesTbody) return;
        
        vehicleExpensesTbody.textContent = '';
        
        if (!expenses || expenses.length === 0) {
            const row = document.createElement('tr');
            row.className = 'loading-row';
            const cell = document.createElement('td');
            cell.colSpan = 9;
            cell.style.textAlign = 'center';
            cell.style.color = 'var(--text-muted)';
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            row.appendChild(cell);
            vehicleExpensesTbody.appendChild(row);
            return;
        }
        
        const accountMap = new Map(accounts.map(a => [a.id, a]));
        
        expenses.forEach(expense => {
            const account = accountMap.get(expense.fromAccountId);
            const accountName = account ? (account.name || `Рахунок #${account.id}`) : '-';
            const date = expense.createdAt ? new Date(expense.createdAt).toLocaleDateString('uk-UA') : '-';
            const categoryName = expense.categoryId ? (categoryNameMap.get(expense.categoryId) || 'Категорія') : '-';
            const exchangeRate = expense.exchangeRate ? DeclarantUtils.formatNumber(expense.exchangeRate, 6) : '-';
            const convertedAmount = expense.convertedAmount ? DeclarantUtils.formatNumber(expense.convertedAmount, 2) : '-';
            
            const row = document.createElement('tr');
            
            const createCell = (text, label) => {
                const cell = document.createElement('td');
                cell.setAttribute('data-label', label);
                cell.textContent = text || '-';
                return cell;
            };
            
            row.appendChild(createCell(date, 'Дата'));
            row.appendChild(createCell(DeclarantUtils.formatNumber(expense.amount, 2), 'Сума'));
            row.appendChild(createCell(expense.currency, 'Валюта'));
            row.appendChild(createCell(exchangeRate, 'Курс'));
            row.appendChild(createCell(convertedAmount, 'Сума в EUR'));
            row.appendChild(createCell(categoryName, 'Категорія'));
            row.appendChild(createCell(accountName, 'Рахунок'));
            row.appendChild(createCell(expense.description, 'Опис'));
            
            const actionsCell = document.createElement('td');
            actionsCell.setAttribute('data-label', 'Дії');
            const editBtn = document.createElement('button');
            editBtn.className = 'btn btn-sm btn-primary';
            editBtn.textContent = 'Редагувати';
            editBtn.addEventListener('click', () => {
                if (typeof window.openEditVehicleExpenseModal === 'function') {
                    window.openEditVehicleExpenseModal(expense);
                }
            });
            actionsCell.appendChild(editBtn);
            row.appendChild(actionsCell);
            
            vehicleExpensesTbody.appendChild(row);
        });
    }
    
    function renderCarriers(carriers) {
        const carriersTbody = document.getElementById('carriers-tbody');
        if (!carriersTbody) return;
        
        carriersTbody.textContent = '';
        
        if (!carriers || carriers.length === 0) {
            const row = document.createElement('tr');
            row.className = 'loading-row';
            const cell = document.createElement('td');
            cell.colSpan = 6;
            cell.style.textAlign = 'center';
            cell.style.color = 'var(--text-muted)';
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            row.appendChild(cell);
            carriersTbody.appendChild(row);
            return;
        }
        
        carriers.forEach(carrier => {
            const row = document.createElement('tr');
            
            const createCell = (text, label) => {
                const cell = document.createElement('td');
                if (label) {
                    cell.setAttribute('data-label', label);
                }
                cell.textContent = text || '-';
                return cell;
            };
            
            row.appendChild(createCell(carrier.companyName, 'Назва компанії'));
            row.appendChild(createCell(carrier.registrationAddress, 'Адреса реєстрації'));
            row.appendChild(createCell(carrier.phoneNumber, 'Телефон'));
            row.appendChild(createCell(carrier.code, 'Код'));
            row.appendChild(createCell(carrier.account, 'Рахунок'));
            
            const actionsCell = document.createElement('td');
            actionsCell.setAttribute('data-label', 'Дії');
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'action-buttons';
            
            const editBtn = document.createElement('button');
            editBtn.className = 'btn btn-secondary btn-sm';
            editBtn.textContent = '✏️ Редагувати';
            const editBtnHandler = () => {
                if (typeof window.editCarrier === 'function') {
                    window.editCarrier(carrier.id);
                }
            };
            editBtn.addEventListener('click', editBtnHandler);
            editBtn._clickHandler = editBtnHandler;
            actionsDiv.appendChild(editBtn);
            
            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'btn btn-danger btn-sm';
            deleteBtn.textContent = '🗑️ Видалити';
            const deleteBtnHandler = () => {
                if (typeof window.deleteCarrier === 'function') {
                    window.deleteCarrier(carrier.id);
                }
            };
            deleteBtn.addEventListener('click', deleteBtnHandler);
            deleteBtn._clickHandler = deleteBtnHandler;
            actionsDiv.appendChild(deleteBtn);
            
            actionsCell.appendChild(actionsDiv);
            row.appendChild(actionsCell);
            
            carriersTbody.appendChild(row);
        });
    }
    
    return {
        populateCarriers,
        populateVehicleSenders,
        populateVehicleReceivers,
        populateVehicleTerminals,
        populateVehicleDestinationCountries,
        populateVehicleDestinationPlaces,
        populateAccounts,
        populateCategories,
        populateCurrencies,
        formatCarrier,
        renderVehicles,
        renderPagination,
        renderVehicleDetails,
        renderVehicleExpenses,
        renderCarriers
    };
})();
