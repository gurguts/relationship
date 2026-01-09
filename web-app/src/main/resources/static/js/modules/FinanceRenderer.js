const FinanceRenderer = (function() {
    async function renderAccountsAndBranches(branches, accounts, usersCache, branchesCache) {
        const container = document.getElementById('accounts-branches-container');
        if (!container) return;
        container.textContent = '';
        
        const accountsByBranch = {};
        const standaloneAccounts = [];
        
        accounts.forEach(account => {
            if (account.branchId) {
                if (!accountsByBranch[account.branchId]) {
                    accountsByBranch[account.branchId] = [];
                }
                accountsByBranch[account.branchId].push(account);
            } else {
                standaloneAccounts.push(account);
            }
        });
        
        const branchSections = await Promise.all(
            branches.map(async (branch) => {
                const branchAccounts = accountsByBranch[branch.id] || [];
                return await createBranchSection(branch, branchAccounts, usersCache, branchesCache);
            })
        );
        branchSections.forEach(section => container.appendChild(section));
        
        if (standaloneAccounts.length > 0) {
            const standaloneBalance = await calculateAccountsTotalBalance(standaloneAccounts);
            const standaloneSection = await createStandaloneAccountsSection(standaloneAccounts, standaloneBalance, usersCache, branchesCache);
            container.appendChild(standaloneSection);
        }
    }
    
    async function createBranchSection(branch, accounts, usersCache, branchesCache) {
        const section = document.createElement('div');
        section.className = 'branch-section';
        
        const branchHeader = document.createElement('div');
        branchHeader.className = 'branch-header';
        
        const headerContent = document.createElement('div');
        headerContent.className = 'branch-header-content';
        
        const branchInfo = document.createElement('div');
        branchInfo.className = 'branch-info';
        
        const branchName = document.createElement('h3');
        branchName.className = 'branch-name';
        branchName.textContent = branch.name || '';
        branchInfo.appendChild(branchName);
        
        if (branch.description) {
            const branchDescription = document.createElement('p');
            branchDescription.className = 'branch-description';
            branchDescription.textContent = branch.description;
            branchInfo.appendChild(branchDescription);
        }
        
        const branchBalance = document.createElement('div');
        branchBalance.className = 'branch-balance';
        
        const balanceLabel = document.createElement('span');
        balanceLabel.className = 'balance-label';
        balanceLabel.textContent = 'Загальний баланс:';
        branchBalance.appendChild(balanceLabel);
        
        const balanceValue = document.createElement('span');
        balanceValue.className = 'balance-value';
        const balanceHtml = FinanceUtils.formatBranchBalance(branch.totalBalance);
        balanceValue.innerHTML = balanceHtml;
        branchBalance.appendChild(balanceValue);
        
        const branchActions = document.createElement('div');
        branchActions.className = 'branch-actions';
        
        headerContent.appendChild(branchInfo);
        headerContent.appendChild(branchBalance);
        headerContent.appendChild(branchActions);
        branchHeader.appendChild(headerContent);
        
        const accountsContainer = document.createElement('div');
        accountsContainer.className = 'branch-accounts';
        
        if (accounts.length === 0) {
            const noAccounts = document.createElement('p');
            noAccounts.className = 'no-accounts';
            noAccounts.textContent = CLIENT_MESSAGES.NO_DATA;
            accountsContainer.appendChild(noAccounts);
        } else {
            const accountRows = await Promise.all(
                accounts.map(account => createAccountRow(account, true, usersCache, branchesCache))
            );
            accountRows.forEach(row => accountsContainer.appendChild(row));
        }
        
        section.appendChild(branchHeader);
        section.appendChild(accountsContainer);
        
        return section;
    }
    
    async function createStandaloneAccountsSection(accounts, totalBalance, usersCache, branchesCache) {
        const section = document.createElement('div');
        section.className = 'branch-section standalone-accounts';
        
        const header = document.createElement('div');
        header.className = 'branch-header';
        
        const headerContent = document.createElement('div');
        headerContent.className = 'branch-header-content';
        
        const branchInfo = document.createElement('div');
        branchInfo.className = 'branch-info';
        
        const branchName = document.createElement('h3');
        branchName.className = 'branch-name';
        branchName.textContent = 'Рахунки без філії';
        branchInfo.appendChild(branchName);
        
        const branchBalance = document.createElement('div');
        branchBalance.className = 'branch-balance';
        
        const balanceLabel = document.createElement('span');
        balanceLabel.className = 'balance-label';
        balanceLabel.textContent = 'Загальний баланс:';
        branchBalance.appendChild(balanceLabel);
        
        const balanceValue = document.createElement('span');
        balanceValue.className = 'balance-value';
        const balanceHtml = FinanceUtils.formatBranchBalance(totalBalance);
        balanceValue.innerHTML = balanceHtml;
        branchBalance.appendChild(balanceValue);
        
        headerContent.appendChild(branchInfo);
        headerContent.appendChild(branchBalance);
        header.appendChild(headerContent);
        
        const accountsContainer = document.createElement('div');
        accountsContainer.className = 'branch-accounts';
        
        const accountRows = await Promise.all(
            accounts.map(account => createAccountRow(account, false, usersCache, branchesCache))
        );
        accountRows.forEach(row => accountsContainer.appendChild(row));
        
        section.appendChild(header);
        section.appendChild(accountsContainer);
        
        return section;
    }
    
    async function createAccountRow(account, isInBranch, usersCache, branchesCache) {
        const row = document.createElement('div');
        row.className = `account-row ${isInBranch ? 'account-in-branch' : ''}`;
        
        let canOperateAccount = true;
        if (account.branchId) {
            const branch = branchesCache.find(b => b.id === account.branchId);
            canOperateAccount = branch ? (branch.canOperate === true) : false;
        }
        
        const accountInfo = document.createElement('div');
        accountInfo.className = 'account-info';
        
        const accountName = document.createElement('div');
        accountName.className = 'account-name';
        accountName.textContent = account.name || '';
        accountInfo.appendChild(accountName);
        
        const accountDetails = document.createElement('div');
        accountDetails.className = 'account-details';
        
        if (account.userId) {
            const accountUser = document.createElement('span');
            accountUser.className = 'account-user';
            accountUser.textContent = `👤 ${FinanceUtils.getUserName(account.userId, usersCache)}`;
            accountDetails.appendChild(accountUser);
        }
        
        const accountCurrencies = document.createElement('span');
        accountCurrencies.className = 'account-currencies';
        if (account.currencies && Array.isArray(account.currencies)) {
            account.currencies.forEach(currency => {
                const currencyBadge = document.createElement('span');
                currencyBadge.className = 'currency-badge';
                currencyBadge.textContent = currency || '';
                accountCurrencies.appendChild(currencyBadge);
            });
        }
        accountDetails.appendChild(accountCurrencies);
        
        const accountBalances = document.createElement('div');
        accountBalances.className = 'account-balances';
        
        const loadingDiv = document.createElement('div');
        loadingDiv.className = 'account-balances-loading';
        loadingDiv.textContent = CLIENT_MESSAGES.LOADING;
        accountBalances.appendChild(loadingDiv);
        
        try {
            const balances = await FinanceDataLoader.loadAccountBalances(account.id);
            accountBalances.textContent = '';
            if (balances && balances.length > 0) {
                balances.forEach(balance => {
                    const balanceItemRow = document.createElement('div');
                    balanceItemRow.className = 'balance-item-row';
                    
                    const balanceCurrency = document.createElement('span');
                    balanceCurrency.className = 'balance-currency';
                    balanceCurrency.textContent = balance.currency || '';
                    balanceItemRow.appendChild(balanceCurrency);
                    
                    const balanceAmount = document.createElement('span');
                    balanceAmount.className = 'balance-amount';
                    balanceAmount.textContent = FinanceUtils.formatNumber(balance.amount);
                    balanceItemRow.appendChild(balanceAmount);
                    
                    accountBalances.appendChild(balanceItemRow);
                });
            } else {
                const emptyDiv = document.createElement('div');
                emptyDiv.className = 'account-balances-empty';
                emptyDiv.textContent = CLIENT_MESSAGES.NO_DATA;
                accountBalances.appendChild(emptyDiv);
            }
        } catch (error) {
            console.warn(`Failed to load balances for account ${account.id}`, error);
            accountBalances.textContent = '';
            const errorDiv = document.createElement('div');
            errorDiv.className = 'account-balances-error';
            errorDiv.textContent = CLIENT_MESSAGES.LOAD_ERROR;
            accountBalances.appendChild(errorDiv);
        }
        
        accountInfo.appendChild(accountDetails);
        accountInfo.appendChild(accountBalances);
        
        const accountActions = document.createElement('div');
        accountActions.className = 'account-actions';
        
        if (!canOperateAccount) {
            const noPermissionHint = document.createElement('span');
            noPermissionHint.className = 'no-permission-hint';
            noPermissionHint.textContent = 'Немає прав на операції';
            accountActions.appendChild(noPermissionHint);
        }
        
        row.appendChild(accountInfo);
        row.appendChild(accountActions);
        
        return row;
    }
    
    async function calculateAccountsTotalBalance(accounts) {
        try {
            if (!accounts || accounts.length === 0) {
                return {};
            }
            
            const balancePromises = accounts.map(async (account) => {
                try {
                    return await FinanceDataLoader.loadAccountBalances(account.id);
                } catch (error) {
                    console.warn(`Failed to load balances for account ${account.id}`, error);
                    return [];
                }
            });
            
            const allBalances = await Promise.all(balancePromises);
            const flatBalances = allBalances.flat();
            
            const totalBalance = {};
            flatBalances.forEach(balance => {
                const currency = balance.currency;
                if (!totalBalance[currency]) {
                    totalBalance[currency] = 0;
                }
                totalBalance[currency] += parseFloat(balance.amount || 0);
            });
            
            return totalBalance;
        } catch (error) {
            console.error('Error calculating accounts total balance:', error);
            return {};
        }
    }
    
    async function calculateBranchTotalBalance(branchId) {
        try {
            const accounts = await FinanceDataLoader.loadBranchAccounts(branchId);
            if (!accounts || accounts.length === 0) {
                return {};
            }
            return await calculateAccountsTotalBalance(accounts);
        } catch (error) {
            console.error(`Error calculating balance for branch ${branchId}:`, error);
            return {};
        }
    }
    
    function renderTransactions(transactions, onEditTransaction) {
        const tbody = document.getElementById('transactions-body');
        if (!tbody) return;
        tbody.textContent = '';

        if (transactions.length === 0) {
            const row = document.createElement('tr');
            const emptyCell = document.createElement('td');
            emptyCell.colSpan = 12;
            emptyCell.style.textAlign = 'center';
            emptyCell.textContent = CLIENT_MESSAGES.NO_DATA;
            row.appendChild(emptyCell);
            tbody.appendChild(row);
            return;
        }

        transactions.forEach(transaction => {
            const row = document.createElement('tr');
            const date = transaction.createdAt ? new Date(transaction.createdAt).toLocaleDateString('uk-UA') : CLIENT_MESSAGES.EMPTY_VALUE;
            const type = FinanceUtils.getTransactionTypeName(transaction.type);
            const category = transaction.categoryName || CLIENT_MESSAGES.EMPTY_VALUE;
            const fromAccount = transaction.fromAccountName || CLIENT_MESSAGES.EMPTY_VALUE;
            const toAccount = transaction.toAccountName || CLIENT_MESSAGES.EMPTY_VALUE;
            const amount = FinanceUtils.formatNumber(transaction.amount);
            const currency = transaction.currency || CLIENT_MESSAGES.EMPTY_VALUE;
            const client = transaction.clientCompany || CLIENT_MESSAGES.EMPTY_VALUE;
            const description = transaction.description || CLIENT_MESSAGES.EMPTY_VALUE;
            const vehicle = transaction.vehicleNumber || CLIENT_MESSAGES.EMPTY_VALUE;

            let amountDisplay = amount;
            if (transaction.type === 'CURRENCY_CONVERSION' && transaction.convertedAmount) {
                amountDisplay = `${amount} → ${FinanceUtils.formatNumber(transaction.convertedAmount)} ${transaction.convertedCurrency || ''}`;
            } else if (transaction.type === 'INTERNAL_TRANSFER' && transaction.commission) {
                const transferAmount = parseFloat(transaction.amount) - parseFloat(transaction.commission);
                amountDisplay = `${amount} (комісія: ${FinanceUtils.formatNumber(transaction.commission)}, переказ: ${FinanceUtils.formatNumber(transferAmount)})`;
            }

            const dateCell = document.createElement('td');
            dateCell.setAttribute('data-label', 'Дата');
            dateCell.textContent = date;
            row.appendChild(dateCell);

            const typeCell = document.createElement('td');
            typeCell.setAttribute('data-label', 'Тип');
            typeCell.textContent = type;
            row.appendChild(typeCell);

            const vehicleCell = document.createElement('td');
            vehicleCell.setAttribute('data-label', 'Машина');
            vehicleCell.textContent = vehicle;
            row.appendChild(vehicleCell);

            const categoryCell = document.createElement('td');
            categoryCell.setAttribute('data-label', 'Категорія');
            categoryCell.textContent = category;
            row.appendChild(categoryCell);

            const fromAccountCell = document.createElement('td');
            fromAccountCell.setAttribute('data-label', 'З рахунку');
            fromAccountCell.textContent = fromAccount;
            row.appendChild(fromAccountCell);

            const toAccountCell = document.createElement('td');
            toAccountCell.setAttribute('data-label', 'На рахунок');
            toAccountCell.textContent = toAccount;
            row.appendChild(toAccountCell);

            const amountCell = document.createElement('td');
            amountCell.setAttribute('data-label', 'Сума');
            amountCell.textContent = amountDisplay;
            row.appendChild(amountCell);

            const currencyCell = document.createElement('td');
            currencyCell.setAttribute('data-label', 'Валюта');
            currencyCell.textContent = currency;
            row.appendChild(currencyCell);

            const clientCell = document.createElement('td');
            clientCell.setAttribute('data-label', 'Клієнт');
            clientCell.textContent = client;
            row.appendChild(clientCell);

            const counterpartyCell = document.createElement('td');
            counterpartyCell.setAttribute('data-label', 'Контрагент');
            counterpartyCell.textContent = transaction.counterpartyName || CLIENT_MESSAGES.EMPTY_VALUE;
            row.appendChild(counterpartyCell);

            const descriptionCell = document.createElement('td');
            descriptionCell.setAttribute('data-label', 'Опис');
            descriptionCell.textContent = description;
            row.appendChild(descriptionCell);

            const actionsCell = document.createElement('td');
            actionsCell.setAttribute('data-label', 'Дії');
            const actionButtonsTable = document.createElement('div');
            actionButtonsTable.className = 'action-buttons-table';
            const editButton = document.createElement('button');
            editButton.className = 'action-btn btn-edit';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (onEditTransaction) {
                    onEditTransaction(transaction.id);
                }
            });
            actionButtonsTable.appendChild(editButton);
            actionsCell.appendChild(actionButtonsTable);
            row.appendChild(actionsCell);

            tbody.appendChild(row);
        });
    }
    
    function renderExchangeRates(rates, onEditRate) {
        const tbody = document.getElementById('exchange-rates-body');
        if (!tbody) return;
        
        tbody.textContent = '';
        
        const expectedCurrencies = ['UAH', 'USD'];
        
        expectedCurrencies.forEach(currency => {
            const rate = rates.find(r => r.fromCurrency === currency);
            const row = document.createElement('tr');
            
            const updatedAt = rate && rate.updatedAt 
                ? new Date(rate.updatedAt).toLocaleString('uk-UA')
                : 'Не встановлено';
            
            const currencyCell = document.createElement('td');
            currencyCell.setAttribute('data-label', 'Валюта');
            currencyCell.textContent = currency;
            row.appendChild(currencyCell);
            
            const rateCell = document.createElement('td');
            rateCell.setAttribute('data-label', 'Курс до EUR');
            rateCell.textContent = rate ? rate.rate.toFixed(6) : 'Не встановлено';
            row.appendChild(rateCell);
            
            const updatedAtCell = document.createElement('td');
            updatedAtCell.setAttribute('data-label', 'Оновлено');
            updatedAtCell.textContent = updatedAt;
            row.appendChild(updatedAtCell);
            
            const actionsCell = document.createElement('td');
            actionsCell.setAttribute('data-label', 'Дії');
            const editButton = document.createElement('button');
            editButton.className = 'action-btn btn-edit';
            editButton.textContent = rate ? 'Оновити' : 'Встановити';
            editButton.addEventListener('click', () => {
                if (onEditRate) {
                    onEditRate(currency, rate ? rate.rate : null);
                }
            });
            actionsCell.appendChild(editButton);
            row.appendChild(actionsCell);
            
            tbody.appendChild(row);
        });
    }
    
    function updateTransactionPagination(currentPage, totalPages) {
        const prevBtn = document.getElementById('prev-page-btn');
        const nextBtn = document.getElementById('next-page-btn');
        const pageInfo = document.getElementById('page-info');

        if (prevBtn) {
            prevBtn.disabled = currentPage === 0;
        }
        if (nextBtn) {
            nextBtn.disabled = currentPage >= totalPages - 1;
        }
        if (pageInfo) {
            pageInfo.textContent = `Сторінка ${currentPage + 1} з ${totalPages || 1}`;
        }
    }
    
    return {
        renderAccountsAndBranches,
        calculateAccountsTotalBalance,
        calculateBranchTotalBalance,
        renderTransactions,
        renderExchangeRates,
        updateTransactionPagination
    };
})();
