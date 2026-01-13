const SettingsRenderer = (function() {
    const transactionTypeMap = {
        'INTERNAL_TRANSFER': 'Переказ між рахунками',
        'EXTERNAL_INCOME': 'Зовнішній прихід',
        'EXTERNAL_EXPENSE': 'Зовнішня витрата',
        'CLIENT_PAYMENT': 'Оплата клієнту',
        'CURRENCY_CONVERSION': 'Конвертація валют',
        'PURCHASE': 'Закупівля',
        'VEHICLE_EXPENSE': 'Витрати на машину'
    };
    
    function renderCategories(categories) {
        const tbody = document.getElementById('categories-body');
        if (!tbody) return;
        
        tbody.textContent = '';
        
        if (categories.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 5;
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            cell.style.textAlign = 'center';
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }
        
        categories.forEach(category => {
            const row = document.createElement('tr');
            
            const typeCell = document.createElement('td');
            typeCell.textContent = transactionTypeMap[category.type] || category.type;
            row.appendChild(typeCell);
            
            const nameCell = document.createElement('td');
            nameCell.textContent = category.name || '';
            row.appendChild(nameCell);
            
            const descriptionCell = document.createElement('td');
            descriptionCell.textContent = category.description || '-';
            row.appendChild(descriptionCell);
            
            const statusCell = document.createElement('td');
            const statusBadge = document.createElement('span');
            statusBadge.className = `status-badge ${category.isActive ? 'status-active' : 'status-inactive'}`;
            statusBadge.textContent = category.isActive ? 'Активна' : 'Неактивна';
            statusCell.appendChild(statusBadge);
            row.appendChild(statusCell);
            
            const actionsCell = document.createElement('td');
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'action-buttons-table';
            
            const editButton = document.createElement('button');
            editButton.className = 'action-btn btn-edit';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.editCategory === 'function') {
                    window.editCategory(category.id);
                }
            });
            actionsDiv.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'action-btn btn-delete';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteCategory === 'function') {
                    window.deleteCategory(category.id);
                }
            });
            actionsDiv.appendChild(deleteButton);
            
            actionsCell.appendChild(actionsDiv);
            row.appendChild(actionsCell);
            
            tbody.appendChild(row);
        });
    }
    
    function renderCounterparties(counterparties) {
        const tbody = document.getElementById('counterparties-body');
        if (!tbody) return;
        
        tbody.textContent = '';
        
        if (counterparties.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 4;
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            cell.style.textAlign = 'center';
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }
        
        counterparties.forEach(cp => {
            const row = document.createElement('tr');
            
            const typeCell = document.createElement('td');
            typeCell.textContent = cp.type === 'INCOME' ? 'Для доходів' : 'Для витрат';
            row.appendChild(typeCell);
            
            const nameCell = document.createElement('td');
            nameCell.textContent = cp.name || '';
            row.appendChild(nameCell);
            
            const descriptionCell = document.createElement('td');
            descriptionCell.textContent = cp.description || '';
            row.appendChild(descriptionCell);
            
            const actionsCell = document.createElement('td');
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'action-buttons-table';
            
            const editButton = document.createElement('button');
            editButton.className = 'action-btn btn-edit';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.editCounterparty === 'function') {
                    window.editCounterparty(cp.id);
                }
            });
            actionsDiv.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'action-btn btn-delete';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteCounterparty === 'function') {
                    window.deleteCounterparty(cp.id);
                }
            });
            actionsDiv.appendChild(deleteButton);
            
            actionsCell.appendChild(actionsDiv);
            row.appendChild(actionsCell);
            tbody.appendChild(row);
        });
    }
    
    function renderBranches(branches) {
        const tbody = document.getElementById('branches-body');
        if (!tbody) return;
        
        tbody.textContent = '';
        
        if (branches.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 3;
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            cell.style.textAlign = 'center';
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }
        
        branches.forEach(branch => {
            const row = document.createElement('tr');
            
            const nameCell = document.createElement('td');
            nameCell.textContent = branch.name || '';
            row.appendChild(nameCell);
            
            const descriptionCell = document.createElement('td');
            descriptionCell.textContent = branch.description || '-';
            row.appendChild(descriptionCell);
            
            const actionsCell = document.createElement('td');
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'action-buttons-table';
            
            const editButton = document.createElement('button');
            editButton.className = 'action-btn btn-edit';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.editBranch === 'function') {
                    window.editBranch(branch.id);
                }
            });
            actionsDiv.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'action-btn btn-delete';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteBranch === 'function') {
                    window.deleteBranch(branch.id);
                }
            });
            actionsDiv.appendChild(deleteButton);
            
            actionsCell.appendChild(actionsDiv);
            row.appendChild(actionsCell);
            
            tbody.appendChild(row);
        });
    }
    
    function renderAccounts(accounts, usersCache, branchesCache) {
        const tbody = document.getElementById('accounts-body');
        if (!tbody) return;
        
        tbody.textContent = '';
        
        if (accounts.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 6;
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            cell.style.textAlign = 'center';
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }
        
        const userMap = new Map(usersCache.map(u => [Number(u.id), u.fullName || u.name || '']));
        const branchMap = new Map(branchesCache.map(b => [Number(b.id), b.name || '']));
        
        accounts.forEach(account => {
            const userId = account.userId ? Number(account.userId) : null;
            const branchId = account.branchId ? Number(account.branchId) : null;
            const user = userId ? userMap.get(userId) : null;
            const userName = user || (userId ? `User ${userId}` : '-');
            const branchName = branchId 
                ? (branchMap.get(branchId) || `Branch ${branchId}`)
                : '-';
            const currencies = account.currencies && account.currencies.length > 0 
                ? account.currencies.join(', ')
                : '-';
            
            const row = document.createElement('tr');
            
            const nameCell = document.createElement('td');
            nameCell.textContent = account.name || '';
            row.appendChild(nameCell);
            
            const descriptionCell = document.createElement('td');
            descriptionCell.textContent = account.description || '-';
            row.appendChild(descriptionCell);
            
            const userCell = document.createElement('td');
            userCell.textContent = userName;
            row.appendChild(userCell);
            
            const branchCell = document.createElement('td');
            branchCell.textContent = branchName;
            row.appendChild(branchCell);
            
            const currenciesCell = document.createElement('td');
            currenciesCell.textContent = currencies;
            row.appendChild(currenciesCell);
            
            const actionsCell = document.createElement('td');
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'action-buttons-table';
            
            const editButton = document.createElement('button');
            editButton.className = 'action-btn btn-edit';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.editAccount === 'function') {
                    window.editAccount(account.id);
                }
            });
            actionsDiv.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'action-btn btn-delete';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteAccount === 'function') {
                    window.deleteAccount(account.id);
                }
            });
            actionsDiv.appendChild(deleteButton);
            
            actionsCell.appendChild(actionsDiv);
            row.appendChild(actionsCell);
            
            tbody.appendChild(row);
        });
    }
    
    function renderVehicleSenders(senders) {
        const tbody = document.getElementById('vehicle-senders-body');
        if (!tbody) return;
        
        tbody.textContent = '';
        
        if (senders.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 2;
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            cell.style.textAlign = 'center';
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }
        
        senders.forEach(sender => {
            const row = document.createElement('tr');
            
            const nameCell = document.createElement('td');
            nameCell.setAttribute('data-label', 'Назва');
            nameCell.textContent = sender.name || '';
            row.appendChild(nameCell);
            
            const actionsCell = document.createElement('td');
            actionsCell.setAttribute('data-label', 'Дії');
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'action-buttons-table';
            
            const editButton = document.createElement('button');
            editButton.className = 'action-btn btn-edit';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.editVehicleSender === 'function') {
                    window.editVehicleSender(sender.id);
                }
            });
            actionsDiv.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'action-btn btn-delete';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteVehicleSender === 'function') {
                    window.deleteVehicleSender(sender.id);
                }
            });
            actionsDiv.appendChild(deleteButton);
            
            actionsCell.appendChild(actionsDiv);
            row.appendChild(actionsCell);
            
            tbody.appendChild(row);
        });
    }
    
    function renderVehicleReceivers(receivers) {
        const tbody = document.getElementById('vehicle-receivers-body');
        if (!tbody) return;
        
        tbody.textContent = '';
        
        if (receivers.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 2;
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            cell.style.textAlign = 'center';
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }
        
        receivers.forEach(receiver => {
            const row = document.createElement('tr');
            
            const nameCell = document.createElement('td');
            nameCell.setAttribute('data-label', 'Назва');
            nameCell.textContent = receiver.name || '';
            row.appendChild(nameCell);
            
            const actionsCell = document.createElement('td');
            actionsCell.setAttribute('data-label', 'Дії');
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'action-buttons-table';
            
            const editButton = document.createElement('button');
            editButton.className = 'action-btn btn-edit';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.editVehicleReceiver === 'function') {
                    window.editVehicleReceiver(receiver.id);
                }
            });
            actionsDiv.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'action-btn btn-delete';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteVehicleReceiver === 'function') {
                    window.deleteVehicleReceiver(receiver.id);
                }
            });
            actionsDiv.appendChild(deleteButton);
            
            actionsCell.appendChild(actionsDiv);
            row.appendChild(actionsCell);
            
            tbody.appendChild(row);
        });
    }
    
    function renderVehicleTerminals(terminals) {
        const tbody = document.getElementById('vehicle-terminals-body');
        if (!tbody) return;
        
        tbody.textContent = '';
        
        if (terminals.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 2;
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            cell.style.textAlign = 'center';
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }
        
        terminals.forEach(terminal => {
            const row = document.createElement('tr');
            
            const nameCell = document.createElement('td');
            nameCell.setAttribute('data-label', 'Назва');
            nameCell.textContent = terminal.name || '';
            row.appendChild(nameCell);
            
            const actionsCell = document.createElement('td');
            actionsCell.setAttribute('data-label', 'Дії');
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'action-buttons-table';
            
            const editButton = document.createElement('button');
            editButton.className = 'action-btn btn-edit';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.editVehicleTerminal === 'function') {
                    window.editVehicleTerminal(terminal.id);
                }
            });
            actionsDiv.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'action-btn btn-delete';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteVehicleTerminal === 'function') {
                    window.deleteVehicleTerminal(terminal.id);
                }
            });
            actionsDiv.appendChild(deleteButton);
            
            actionsCell.appendChild(actionsDiv);
            row.appendChild(actionsCell);
            
            tbody.appendChild(row);
        });
    }
    
    function renderVehicleDestinationCountries(countries) {
        const tbody = document.getElementById('vehicle-destination-countries-body');
        if (!tbody) return;
        
        tbody.textContent = '';
        
        if (countries.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 2;
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            cell.style.textAlign = 'center';
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }
        
        countries.forEach(country => {
            const row = document.createElement('tr');
            
            const nameCell = document.createElement('td');
            nameCell.setAttribute('data-label', 'Назва');
            nameCell.textContent = country.name || '';
            row.appendChild(nameCell);
            
            const actionsCell = document.createElement('td');
            actionsCell.setAttribute('data-label', 'Дії');
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'action-buttons-table';
            
            const editButton = document.createElement('button');
            editButton.className = 'action-btn btn-edit';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.editVehicleDestinationCountry === 'function') {
                    window.editVehicleDestinationCountry(country.id);
                }
            });
            actionsDiv.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'action-btn btn-delete';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteVehicleDestinationCountry === 'function') {
                    window.deleteVehicleDestinationCountry(country.id);
                }
            });
            actionsDiv.appendChild(deleteButton);
            
            actionsCell.appendChild(actionsDiv);
            row.appendChild(actionsCell);
            
            tbody.appendChild(row);
        });
    }
    
    function renderVehicleDestinationPlaces(places) {
        const tbody = document.getElementById('vehicle-destination-places-body');
        if (!tbody) return;
        
        tbody.textContent = '';
        
        if (places.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 2;
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            cell.style.textAlign = 'center';
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }
        
        places.forEach(place => {
            const row = document.createElement('tr');
            
            const nameCell = document.createElement('td');
            nameCell.setAttribute('data-label', 'Назва');
            nameCell.textContent = place.name || '';
            row.appendChild(nameCell);
            
            const actionsCell = document.createElement('td');
            actionsCell.setAttribute('data-label', 'Дії');
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'action-buttons-table';
            
            const editButton = document.createElement('button');
            editButton.className = 'action-btn btn-edit';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.editVehicleDestinationPlace === 'function') {
                    window.editVehicleDestinationPlace(place.id);
                }
            });
            actionsDiv.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'action-btn btn-delete';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteVehicleDestinationPlace === 'function') {
                    window.deleteVehicleDestinationPlace(place.id);
                }
            });
            actionsDiv.appendChild(deleteButton);
            
            actionsCell.appendChild(actionsDiv);
            row.appendChild(actionsCell);
            
            tbody.appendChild(row);
        });
    }
    
    function renderCarriers(carriers) {
        const tbody = document.getElementById('carriers-body');
        if (!tbody) return;
        
        tbody.textContent = '';
        
        if (carriers.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 6;
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            cell.style.textAlign = 'center';
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }
        
        carriers.forEach(carrier => {
            const row = document.createElement('tr');
            
            const companyNameCell = document.createElement('td');
            companyNameCell.textContent = carrier.companyName || '-';
            row.appendChild(companyNameCell);
            
            const registrationAddressCell = document.createElement('td');
            registrationAddressCell.textContent = carrier.registrationAddress || '-';
            row.appendChild(registrationAddressCell);
            
            const phoneNumberCell = document.createElement('td');
            phoneNumberCell.textContent = carrier.phoneNumber || '-';
            row.appendChild(phoneNumberCell);
            
            const codeCell = document.createElement('td');
            codeCell.textContent = carrier.code || '-';
            row.appendChild(codeCell);
            
            const accountCell = document.createElement('td');
            accountCell.textContent = carrier.account || '-';
            row.appendChild(accountCell);
            
            const actionsCell = document.createElement('td');
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'action-buttons-table';
            
            const editButton = document.createElement('button');
            editButton.className = 'action-btn btn-edit';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.editCarrier === 'function') {
                    window.editCarrier(carrier.id);
                }
            });
            actionsDiv.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'action-btn btn-delete';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteCarrier === 'function') {
                    window.deleteCarrier(carrier.id);
                }
            });
            actionsDiv.appendChild(deleteButton);
            
            actionsCell.appendChild(actionsDiv);
            row.appendChild(actionsCell);
            
            tbody.appendChild(row);
        });
    }
    
    function populateAccountFormDropdowns(usersCache, branchesCache) {
        const userSelect = document.getElementById('account-user');
        if (userSelect) {
            userSelect.textContent = '';
            const defaultUserOption = document.createElement('option');
            defaultUserOption.value = '';
            defaultUserOption.textContent = 'Без користувача';
            userSelect.appendChild(defaultUserOption);
            usersCache.forEach(user => {
                const option = document.createElement('option');
                option.value = user.id;
                option.textContent = user.fullName || user.name;
                userSelect.appendChild(option);
            });
        }
        
        const branchSelect = document.getElementById('account-branch');
        if (branchSelect) {
            branchSelect.textContent = '';
            const defaultBranchOption = document.createElement('option');
            defaultBranchOption.value = '';
            defaultBranchOption.textContent = 'Без філії';
            branchSelect.appendChild(defaultBranchOption);
            branchesCache.forEach(branch => {
                const option = document.createElement('option');
                option.value = branch.id;
                option.textContent = branch.name;
                branchSelect.appendChild(option);
            });
        }
    }
    
    return {
        renderCategories,
        renderCounterparties,
        renderBranches,
        renderAccounts,
        renderVehicleSenders,
        renderVehicleReceivers,
        renderVehicleTerminals,
        renderVehicleDestinationCountries,
        renderVehicleDestinationPlaces,
        renderCarriers,
        populateAccountFormDropdowns
    };
})();
