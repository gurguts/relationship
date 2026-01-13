const ProfileRenderer = (function() {
    function renderProductBalances(balances, productsCache, canEditProfile, onEditClick) {
        const container = document.getElementById('product-balance-container');
        if (!container) return;
        
        container.textContent = '';
        
        if (!balances || balances.length === 0) {
            const emptyDiv = document.createElement('div');
            emptyDiv.className = 'empty';
            emptyDiv.textContent = PROFILE_MESSAGES.NO_PRODUCT_BALANCE;
            container.appendChild(emptyDiv);
            return;
        }

        const activeBalances = balances.filter(b => b.quantity && parseFloat(b.quantity) > 0);
        
        if (activeBalances.length === 0) {
            const emptyDiv = document.createElement('div');
            emptyDiv.className = 'empty';
            emptyDiv.textContent = PROFILE_MESSAGES.NO_PRODUCT_BALANCE;
            container.appendChild(emptyDiv);
            return;
        }

        const table = document.createElement('table');
        table.className = 'product-balance-table';
        
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        
        const productHeader = document.createElement('th');
        productHeader.textContent = 'Товар';
        headerRow.appendChild(productHeader);
        
        const quantityHeader = document.createElement('th');
        quantityHeader.textContent = 'Кількість (кг)';
        headerRow.appendChild(quantityHeader);
        
        const priceHeader = document.createElement('th');
        priceHeader.textContent = 'Середня ціна (EUR/кг)';
        headerRow.appendChild(priceHeader);
        
        const totalHeader = document.createElement('th');
        totalHeader.textContent = 'Загальна вартість (EUR)';
        headerRow.appendChild(totalHeader);
        
        if (canEditProfile) {
            const actionsHeader = document.createElement('th');
            actionsHeader.textContent = 'Дії';
            headerRow.appendChild(actionsHeader);
        }
        
        thead.appendChild(headerRow);
        table.appendChild(thead);
        
        const tbody = document.createElement('tbody');
        
        activeBalances.forEach(balance => {
            const product = productsCache.find(p => Number(p.id) === Number(balance.productId));
            const productName = product ? product.name : `Товар #${balance.productId}`;
            
            const row = document.createElement('tr');
            
            const productCell = document.createElement('td');
            productCell.setAttribute('data-label', 'Товар');
            productCell.textContent = productName;
            row.appendChild(productCell);
            
            const quantityCell = document.createElement('td');
            quantityCell.setAttribute('data-label', 'Кількість (кг)');
            quantityCell.textContent = ProfileUtils.formatNumber(balance.quantity, 2);
            row.appendChild(quantityCell);
            
            const priceCell = document.createElement('td');
            priceCell.setAttribute('data-label', 'Середня ціна (EUR/кг)');
            priceCell.textContent = ProfileUtils.formatNumber(balance.averagePriceEur, 6);
            row.appendChild(priceCell);
            
            const totalCell = document.createElement('td');
            totalCell.setAttribute('data-label', 'Загальна вартість (EUR)');
            totalCell.textContent = ProfileUtils.formatNumber(balance.totalCostEur, 2);
            row.appendChild(totalCell);
            
            if (canEditProfile) {
                const actionsCell = document.createElement('td');
                actionsCell.setAttribute('data-label', 'Дії');
                
                const editButton = document.createElement('button');
                editButton.className = 'edit-balance-btn';
                editButton.textContent = 'Редагувати';
                editButton.addEventListener('click', () => {
                    if (onEditClick) {
                        onEditClick(balance);
                    }
                });
                
                actionsCell.appendChild(editButton);
                row.appendChild(actionsCell);
            }
            
            tbody.appendChild(row);
        });
        
        table.appendChild(tbody);
        container.appendChild(table);
    }

    async function renderAccounts(accounts, loadAccountBalancesFn) {
        const container = document.getElementById('accounts-container');
        if (!container) return;
        
        container.textContent = '';
        
        if (!accounts || accounts.length === 0) {
            const emptyDiv = document.createElement('div');
            emptyDiv.className = 'empty';
            emptyDiv.textContent = PROFILE_MESSAGES.NO_ACCOUNTS;
            container.appendChild(emptyDiv);
            return;
        }

        let balancesMap = new Map();
        if (loadAccountBalancesFn && loadAccountBalancesFn.loadAccountBalancesBatch) {
            const accountIds = accounts.map(acc => acc.id).filter(id => id != null);
            balancesMap = await loadAccountBalancesFn.loadAccountBalancesBatch(accountIds);
        }

        for (const account of accounts) {
            const balances = balancesMap.has(account.id) ? balancesMap.get(account.id) : null;
            const accountCard = await createAccountCard(account, loadAccountBalancesFn, balances);
            container.appendChild(accountCard);
        }
    }

    async function createAccountCard(account, loadAccountBalancesFn, balances = null) {
        const card = document.createElement('div');
        card.className = 'account-card';
        
        const header = document.createElement('div');
        header.className = 'account-card-header';
        
        const nameSpan = document.createElement('span');
        nameSpan.className = 'account-name';
        nameSpan.textContent = account.name || PROFILE_MESSAGES.NO_ACCOUNT_NAME;
        header.appendChild(nameSpan);
        card.appendChild(header);
        
        if (account.description) {
            const descriptionDiv = document.createElement('div');
            descriptionDiv.className = 'account-description';
            descriptionDiv.textContent = account.description || '';
            card.appendChild(descriptionDiv);
        }
        
        const balancesDiv = document.createElement('div');
        balancesDiv.className = 'account-balances';
        
        if (!balances && loadAccountBalancesFn) {
            try {
                balances = await loadAccountBalancesFn(account.id);
            } catch (error) {
                console.warn(`Failed to load balances for account ${account.id}:`, error);
                balances = [];
            }
        }
        
        if (!balances) {
            balances = [];
        }
        
        if (!balances || balances.length === 0) {
            const emptyDiv = document.createElement('div');
            emptyDiv.className = 'account-balances-empty';
            emptyDiv.textContent = PROFILE_MESSAGES.NO_ACCOUNT_BALANCES;
            balancesDiv.appendChild(emptyDiv);
        } else {
            balances.forEach(balance => {
                const balanceItem = document.createElement('div');
                balanceItem.className = 'balance-item';
                
                const currencySpan = document.createElement('span');
                currencySpan.className = 'balance-currency';
                currencySpan.textContent = balance.currency || '';
                balanceItem.appendChild(currencySpan);
                
                const amountSpan = document.createElement('span');
                amountSpan.className = 'balance-amount';
                amountSpan.textContent = ProfileUtils.formatNumber(balance.amount, 2);
                balanceItem.appendChild(amountSpan);
                
                balancesDiv.appendChild(balanceItem);
            });
        }
        
        card.appendChild(balancesDiv);
        return card;
    }

    function renderUserSelector(usersCache, currentUserId, onUserChange) {
        const selectorContainer = document.getElementById('user-selector-container');
        if (!selectorContainer) return;
        
        selectorContainer.textContent = '';
        selectorContainer.style.display = 'flex';
        
        const select = document.createElement('select');
        select.id = 'user-selector';
        select.className = 'user-selector';
        
        const defaultOption = document.createElement('option');
        defaultOption.value = currentUserId;
        const currentUser = usersCache.find(u => Number(u.id) === Number(currentUserId));
        defaultOption.textContent = currentUser ? (currentUser.fullName || currentUser.name || `${PROFILE_MESSAGES.USER_LABEL}${currentUserId}`) : PROFILE_MESSAGES.MY_PROFILE;
        defaultOption.selected = true;
        select.appendChild(defaultOption);
        
        usersCache.forEach(user => {
            if (Number(user.id) !== Number(currentUserId)) {
                const option = document.createElement('option');
                option.value = user.id;
                option.textContent = user.fullName || user.name || `Користувач #${user.id}`;
                select.appendChild(option);
            }
        });
        
        select.addEventListener('change', (e) => {
            if (onUserChange) {
                onUserChange(e.target.value);
            }
        });
        
        selectorContainer.appendChild(select);
    }

    function updateProfileTitle(selectedUserId, currentUserId, usersCache) {
        const titleElement = document.querySelector('.profile-container h1');
        if (!titleElement) return;
        
        if (Number(selectedUserId) === Number(currentUserId)) {
            titleElement.textContent = PROFILE_MESSAGES.MY_PROFILE;
        } else {
            const selectedUser = usersCache.find(u => Number(u.id) === Number(selectedUserId));
            const userName = selectedUser ? (selectedUser.fullName || selectedUser.name || `${PROFILE_MESSAGES.USER_LABEL}${selectedUserId}`) : `${PROFILE_MESSAGES.USER_LABEL}${selectedUserId}`;
            titleElement.textContent = `${PROFILE_MESSAGES.USER_PROFILE}${userName}`;
        }
    }

    function showEmptyState(containerId, message) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        container.textContent = '';
        const emptyDiv = document.createElement('div');
        emptyDiv.className = 'empty';
        emptyDiv.textContent = message;
        container.appendChild(emptyDiv);
    }

    function showErrorState(containerId, message) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        container.textContent = '';
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error';
        errorDiv.textContent = message;
        container.appendChild(errorDiv);
    }

    function showLoadingState(containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        container.textContent = '';
        const loadingDiv = document.createElement('div');
        loadingDiv.className = 'loading';
        loadingDiv.textContent = CLIENT_MESSAGES.LOADING;
        container.appendChild(loadingDiv);
    }

    return {
        renderProductBalances,
        renderAccounts,
        renderUserSelector,
        updateProfileTitle,
        showEmptyState,
        showErrorState,
        showLoadingState
    };
})();
