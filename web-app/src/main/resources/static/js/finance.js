let accountsCache = [];
let branchesCache = [];
let usersCache = [];
let clientsCache = [];

let currentTransactionPage = 0;
const transactionPageSize = CLIENT_CONSTANTS.DEFAULT_PAGE_SIZE;
let totalTransactionPages = 1;

document.addEventListener('DOMContentLoaded', async () => {
    try {
        await loadInitialData();
        initializeTabs();
        initializeModals();
        setupEventListeners();
    } catch (error) {
        console.error('Error initializing finance page:', error);
        handleError(error);
    }
});

function initializeTabs() {
    FinanceTabs.init((targetTab) => {
        if (targetTab === 'accounts') {
            loadAccountsAndBranches();
        } else if (targetTab === 'transactions') {
            FinanceFilters.populateTransactionFilters(accountsCache, branchesCache).then(() => {
                loadTransactions();
            });
        } else if (targetTab === 'exchange-rates') {
            loadExchangeRates();
        }
    });
}

function initializeModals() {
    FinanceExchangeRateModal.init({
        modalId: 'edit-exchange-rate-modal',
        formId: 'exchange-rate-form',
        closeBtnSelector: '.close',
        onSubmit: handleUpdateExchangeRate
    });
    
    const createTransactionModal = document.getElementById('create-transaction-modal');
    if (createTransactionModal) {
        const closeBtn = createTransactionModal.querySelector('.close');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                createTransactionModal.classList.remove('show');
                setTimeout(() => {
                    createTransactionModal.style.display = 'none';
                    document.getElementById('transaction-form')?.reset();
                    const clientInput = document.getElementById('transaction-client');
                    const clientHidden = document.getElementById('transaction-client-id');
                    if (clientInput) clientInput.value = '';
                    if (clientHidden) clientHidden.value = '';
                }, CLIENT_CONSTANTS.MODAL_CLOSE_DELAY);
            });
        }
        
        createTransactionModal.addEventListener('click', (e) => {
            const modalContent = createTransactionModal.querySelector('.modal-content');
            if (modalContent && !modalContent.contains(e.target)) {
                createTransactionModal.classList.remove('show');
                setTimeout(() => {
                    createTransactionModal.style.display = 'none';
                    document.getElementById('transaction-form')?.reset();
                    const clientInput = document.getElementById('transaction-client');
                    const clientHidden = document.getElementById('transaction-client-id');
                    if (clientInput) clientInput.value = '';
                    if (clientHidden) clientHidden.value = '';
                }, CLIENT_CONSTANTS.MODAL_CLOSE_DELAY);
            }
        });
    }
    
    const editTransactionModal = document.getElementById('edit-transaction-modal');
    if (editTransactionModal) {
        editTransactionModal.addEventListener('click', (e) => {
            const modalContent = editTransactionModal.querySelector('.modal-content');
            if (modalContent && !modalContent.contains(e.target)) {
                closeEditTransactionModal();
            }
        });
    }
}

async function loadInitialData() {
    try {
        const [branches, users, clients] = await Promise.all([
            FinanceDataLoader.loadBranches(),
            FinanceDataLoader.loadUsers(),
            FinanceDataLoader.loadClients()
        ]);
        
        branchesCache = branches;
        usersCache = users;
        clientsCache = clients;
        
        await loadAccountsAndBranches();
        initializeClientAutocomplete();
    } catch (error) {
        console.error('Error loading initial data:', error);
        handleError(error);
    }
}

async function loadAccountsAndBranches() {
    try {
        if (usersCache.length === 0) {
            usersCache = await FinanceDataLoader.loadUsers();
        }
        if (branchesCache.length === 0) {
            branchesCache = await FinanceDataLoader.loadBranches();
        }
        
        accountsCache = await FinanceDataLoader.loadAccounts();
        
        const branchesWithBalances = await Promise.all(
            branchesCache.map(async (branch) => {
                const totalBalance = await FinanceRenderer.calculateBranchTotalBalance(branch.id);
                return { ...branch, totalBalance };
            })
        );
        
        await FinanceRenderer.renderAccountsAndBranches(
            branchesWithBalances, 
            accountsCache, 
            usersCache, 
            branchesCache
        );
    } catch (error) {
        console.error('Error loading accounts and branches:', error);
        handleError(error);
    }
}

async function loadTransactions() {
    try {
        const filters = FinanceFilters.buildTransactionFilters();
        const data = await FinanceDataLoader.loadTransactions(
            currentTransactionPage,
            transactionPageSize,
            'createdAt',
            'DESC',
            filters
        );
        
        totalTransactionPages = data.totalPages || 1;
        FinanceRenderer.renderTransactions(data.content || [], openEditTransactionModal);
        FinanceRenderer.updateTransactionPagination(currentTransactionPage, totalTransactionPages);
    } catch (error) {
        console.error('Error loading transactions:', error);
        handleError(error);
    }
}

async function exportTransactionsToExcel() {
    try {
        const filters = FinanceFilters.buildTransactionFilters();
        const blob = await FinanceDataLoader.exportTransactions(filters);
        
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `transactions_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.xlsx`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        
        if (typeof showMessage === 'function') {
            showMessage('Транзакції успішно експортовано', 'success');
        }
    } catch (error) {
        console.error('Error exporting transactions:', error);
        handleError(error);
    }
}

async function loadExchangeRates() {
    try {
        const rates = await FinanceDataLoader.loadExchangeRates();
        FinanceRenderer.renderExchangeRates(rates, (currency, rate) => {
            FinanceExchangeRateModal.openModal(currency, rate);
        });
    } catch (error) {
        console.error('Error loading exchange rates:', error);
        handleError(error);
    }
}

async function handleUpdateExchangeRate(currency, rate) {
    try {
        await FinanceDataLoader.updateExchangeRate(currency, rate);
        FinanceExchangeRateModal.closeModal();
        if (typeof showMessage === 'function') {
            showMessage('Курс валют успішно оновлено', 'success');
        }
        await loadExchangeRates();
    } catch (error) {
        console.error('Error updating exchange rate:', error);
        handleError(error);
    }
}

async function openEditTransactionModal(transactionId) {
    try {
        const transaction = await FinanceDataLoader.loadTransaction(transactionId);
        
        document.getElementById('edit-transaction-id').value = transaction.id;
        document.getElementById('edit-transaction-amount').value = transaction.amount;
        document.getElementById('edit-transaction-description').value = transaction.description || '';
        
        let typeStr;
        if (typeof transaction.type === 'string') {
            typeStr = transaction.type;
        } else if (transaction.type && transaction.type.name) {
            typeStr = transaction.type.name;
        } else if (transaction.type) {
            typeStr = transaction.type.toString();
        }
        
        const exchangeRateGroup = document.getElementById('edit-exchange-rate-group');
        const exchangeRateInput = document.getElementById('edit-transaction-exchange-rate');
        if (typeStr === 'CURRENCY_CONVERSION') {
            exchangeRateGroup.style.display = 'block';
            exchangeRateInput.value = transaction.exchangeRate || '';
        } else {
            exchangeRateGroup.style.display = 'none';
            exchangeRateInput.value = '';
        }
        
        const commissionGroup = document.getElementById('edit-commission-group');
        const commissionInput = document.getElementById('edit-transaction-commission');
        if (typeStr === 'INTERNAL_TRANSFER') {
            commissionGroup.style.display = 'block';
            commissionInput.value = transaction.commission || '';
        } else {
            commissionGroup.style.display = 'none';
            commissionInput.value = '';
        }
        
        const categorySelect = document.getElementById('edit-transaction-category');
        if (categorySelect) {
            categorySelect.textContent = '';
            const defaultOption = document.createElement('option');
            defaultOption.value = '';
            defaultOption.textContent = 'Без категорії';
            categorySelect.appendChild(defaultOption);
        }
        
        if (typeStr) {
            const categories = await FinanceDataLoader.loadCategoriesForType(typeStr);
            if (categories && Array.isArray(categories)) {
                categories.forEach(category => {
                    const option = document.createElement('option');
                    option.value = category.id;
                    option.textContent = category.name;
                    if (transaction.categoryId === category.id) {
                        option.selected = true;
                    }
                    categorySelect.appendChild(option);
                });
            }
        }
        
        const editModal = document.getElementById('edit-transaction-modal');
        if (editModal) {
            editModal.style.display = 'flex';
            setTimeout(() => {
                editModal.classList.add('show');
            }, CLIENT_CONSTANTS.MODAL_ANIMATION_DELAY);
        }
    } catch (error) {
        console.error('Error loading transaction:', error);
        handleError(error);
    }
}

function closeEditTransactionModal() {
    const modal = document.getElementById('edit-transaction-modal');
    if (!modal) return;
    modal.classList.remove('show');
    setTimeout(() => {
        modal.style.display = 'none';
        document.getElementById('edit-transaction-form')?.reset();
    }, CLIENT_CONSTANTS.MODAL_CLOSE_DELAY);
}

async function handleUpdateTransaction(e) {
    e.preventDefault();
    
    const transactionId = document.getElementById('edit-transaction-id').value;
    const categoryId = document.getElementById('edit-transaction-category').value;
    const amount = parseFloat(document.getElementById('edit-transaction-amount').value);
    const description = document.getElementById('edit-transaction-description').value;
    const exchangeRateInput = document.getElementById('edit-transaction-exchange-rate');
    const exchangeRate = exchangeRateInput && exchangeRateInput.style.display !== 'none' 
        ? parseFloat(exchangeRateInput.value) 
        : null;
    
    if (amount === 0 || isNaN(amount)) {
        if (!confirm('Ви впевнені, що хочете видалити цю транзакцію? Гроші будуть повернуті.')) {
            return;
        }
        
        try {
            await FinanceDataLoader.deleteTransaction(transactionId);
            if (typeof showMessage === 'function') {
                showMessage('Транзакцію успішно видалено', 'success');
            }
            closeEditTransactionModal();
            
            const activeTab = document.querySelector('.tab-btn.active');
            if (activeTab) {
                const activeTabName = activeTab.getAttribute('data-tab');
                if (activeTabName === 'transactions') {
                    await loadTransactions();
                } else if (activeTabName === 'accounts') {
                    await loadAccountsAndBranches();
                }
            } else {
                await loadTransactions();
            }
        } catch (error) {
            console.error('Error deleting transaction:', error);
            handleError(error);
        }
        return;
    }
    
    const updateData = {
        categoryId: categoryId ? parseInt(categoryId) : null,
        description: description || null,
        amount: amount
    };
    
    if (exchangeRate !== null && !isNaN(exchangeRate)) {
        updateData.exchangeRate = exchangeRate;
    }
    
    const commissionInput = document.getElementById('edit-transaction-commission');
    if (commissionInput && commissionInput.style.display !== 'none') {
        const commissionValue = commissionInput.value;
        if (commissionValue && commissionValue.trim() !== '') {
            const commission = parseFloat(commissionValue);
            if (!isNaN(commission) && commission >= 0) {
                updateData.commission = commission;
            } else {
                updateData.commission = null;
            }
        } else {
            updateData.commission = null;
        }
    }
    
    try {
        await FinanceDataLoader.updateTransaction(transactionId, updateData);
        if (typeof showMessage === 'function') {
            showMessage('Транзакцію успішно оновлено', 'success');
        }
        closeEditTransactionModal();
        
        const activeTab = document.querySelector('.tab-btn.active');
        if (activeTab) {
            const activeTabName = activeTab.getAttribute('data-tab');
            if (activeTabName === 'transactions') {
                await loadTransactions();
            } else if (activeTabName === 'accounts') {
                await loadAccountsAndBranches();
            }
        } else {
            await loadTransactions();
        }
    } catch (error) {
        console.error('Error updating transaction:', error);
        handleError(error);
    }
}

function openCreateTransactionModal() {
    const modal = document.getElementById('create-transaction-modal');
    if (!modal) return;
    populateTransactionForm();
    modal.style.display = 'flex';
    setTimeout(() => {
        modal.classList.add('show');
    }, CLIENT_CONSTANTS.MODAL_ANIMATION_DELAY);
}

function populateTransactionForm() {
    const canOperateAccount = (account) => {
        if (!account.branchId) return true;
        const branch = branchesCache.find(b => b.id === account.branchId);
        return branch ? (branch.canOperate === true) : false;
    };
    
    const fromAccountSelect = document.getElementById('from-account');
    const toAccountSelect = document.getElementById('to-account');
    const conversionAccountSelect = document.getElementById('conversion-account');

    [fromAccountSelect, toAccountSelect, conversionAccountSelect].forEach(select => {
        if (!select) return;
        select.textContent = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = 'Виберіть рахунок';
        select.appendChild(defaultOption);
        accountsCache.forEach(account => {
            if (canOperateAccount(account)) {
                const option = document.createElement('option');
                option.value = account.id;
                option.textContent = account.name || '';
                select.appendChild(option);
            }
        });
    });

    const currencySelect = document.getElementById('transaction-currency');
    const conversionCurrencySelect = document.getElementById('conversion-currency');
    const currencies = ['UAH', 'USD', 'EUR'];
    
    [currencySelect, conversionCurrencySelect].forEach(select => {
        if (select) {
            select.textContent = '';
            const defaultOption = document.createElement('option');
            defaultOption.value = '';
            defaultOption.textContent = 'Виберіть валюту';
            select.appendChild(defaultOption);
            currencies.forEach(currency => {
                const option = document.createElement('option');
                option.value = currency;
                option.textContent = currency;
                select.appendChild(option);
            });
        }
    });
}

function initializeClientAutocomplete() {
    const input = document.getElementById('transaction-client');
    const hiddenInput = document.getElementById('transaction-client-id');
    const dropdown = document.getElementById('client-autocomplete-dropdown');
    
    if (!input || !hiddenInput || !dropdown) return;

    let selectedClientId = null;

    input.addEventListener('input', (e) => {
        const query = e.target.value.trim().toLowerCase();
        
        if (query.length === 0) {
            dropdown.style.display = 'none';
            hiddenInput.value = '';
            selectedClientId = null;
            return;
        }

        const filtered = clientsCache.filter(client => {
            const company = (client.company || '').toLowerCase();
            return company.includes(query);
        });

        if (filtered.length === 0) {
            dropdown.textContent = '';
            const noResultsItem = document.createElement('div');
            noResultsItem.className = 'client-autocomplete-item';
            noResultsItem.textContent = CLIENT_MESSAGES.NO_DATA;
            dropdown.appendChild(noResultsItem);
            dropdown.style.display = 'block';
            return;
        }

        dropdown.textContent = '';
        filtered.forEach(client => {
            const item = document.createElement('div');
            item.className = 'client-autocomplete-item';
            item.textContent = client.company || `Клієнт #${client.id}`;
            item.addEventListener('click', () => {
                input.value = client.company || `Клієнт #${client.id}`;
                hiddenInput.value = client.id;
                selectedClientId = client.id;
                dropdown.style.display = 'none';
            });
            dropdown.appendChild(item);
        });

        dropdown.style.display = 'block';
    });

    document.addEventListener('click', (e) => {
        if (!input.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.style.display = 'none';
        }
    });

    input.addEventListener('focus', () => {
        if (input.value === '') {
            hiddenInput.value = '';
            selectedClientId = null;
        }
    });
}

function handleTransactionTypeChange() {
    const type = document.getElementById('transaction-type').value;
    const fromAccountGroup = document.getElementById('from-account-group');
    const toAccountGroup = document.getElementById('to-account-group');
    const conversionAccountGroup = document.getElementById('conversion-account-group');
    const conversionCurrencyGroup = document.getElementById('conversion-currency-group');
    const conversionReceivedAmountGroup = document.getElementById('conversion-received-amount-group');
    const conversionExchangeRateDisplayGroup = document.getElementById('conversion-exchange-rate-display-group');
    const clientGroup = document.getElementById('client-group');
    const counterpartyGroup = document.getElementById('counterparty-group');
    const currencyGroup = document.getElementById('currency-group');
    const amountGroup = document.getElementById('amount-group');
    const receivedAmountGroup = document.getElementById('received-amount-group');
    const commissionDisplayGroup = document.getElementById('commission-display-group');
    const amountLabel = document.getElementById('transaction-amount-label');

    fromAccountGroup.style.display = 'none';
    toAccountGroup.style.display = 'none';
    conversionAccountGroup.style.display = 'none';
    conversionCurrencyGroup.style.display = 'none';
    conversionReceivedAmountGroup.style.display = 'none';
    conversionExchangeRateDisplayGroup.style.display = 'none';
    clientGroup.style.display = 'none';
    counterpartyGroup.style.display = 'none';
    receivedAmountGroup.style.display = 'none';
    commissionDisplayGroup.style.display = 'none';
    
    if (currencyGroup) {
        const label = currencyGroup.querySelector('label');
        if (label) label.textContent = 'Валюта:';
    }
    if (amountLabel) {
        amountLabel.textContent = 'Сума:';
    }

    FinanceDataLoader.loadCategoriesForType(type).then(categories => {
        const select = document.getElementById('transaction-category');
        if (select) {
            select.textContent = '';
            const defaultOption = document.createElement('option');
            defaultOption.value = '';
            defaultOption.textContent = 'Без категорії';
            select.appendChild(defaultOption);
            if (Array.isArray(categories)) {
                categories.forEach(cat => {
                    const option = document.createElement('option');
                    option.value = cat.id;
                    option.textContent = cat.name || '';
                    select.appendChild(option);
                });
            }
        }
    });

    if (type === 'INTERNAL_TRANSFER') {
        fromAccountGroup.style.display = 'block';
        toAccountGroup.style.display = 'block';
        currencyGroup.style.display = 'block';
        amountGroup.style.display = 'block';
        receivedAmountGroup.style.display = 'block';
        commissionDisplayGroup.style.display = 'block';
        if (amountLabel) {
            amountLabel.textContent = 'Сума списання:';
        }
        const receivedAmountInput = document.getElementById('transaction-received-amount');
        if (receivedAmountInput) {
            receivedAmountInput.required = true;
        }
        updateCommissionDisplay();
    } else if (type === 'EXTERNAL_INCOME') {
        toAccountGroup.style.display = 'block';
        counterpartyGroup.style.display = 'block';
        currencyGroup.style.display = 'block';
        amountGroup.style.display = 'block';
        loadCounterparties('INCOME');
    } else if (type === 'EXTERNAL_EXPENSE') {
        fromAccountGroup.style.display = 'block';
        counterpartyGroup.style.display = 'block';
        currencyGroup.style.display = 'block';
        amountGroup.style.display = 'block';
        loadCounterparties('EXPENSE');
    } else if (type === 'CLIENT_PAYMENT') {
        fromAccountGroup.style.display = 'block';
        clientGroup.style.display = 'block';
        currencyGroup.style.display = 'block';
        amountGroup.style.display = 'block';
    } else if (type === 'CURRENCY_CONVERSION') {
        conversionAccountGroup.style.display = 'block';
        conversionCurrencyGroup.style.display = 'block';
        conversionReceivedAmountGroup.style.display = 'block';
        conversionExchangeRateDisplayGroup.style.display = 'block';
        currencyGroup.style.display = 'block';
        amountGroup.style.display = 'block';
        if (currencyGroup) {
            const label = currencyGroup.querySelector('label');
            if (label) label.textContent = 'З валюту:';
        }
        if (amountLabel) {
            amountLabel.textContent = 'Сума списання:';
        }
        const conversionReceivedAmountInput = document.getElementById('conversion-received-amount');
        if (conversionReceivedAmountInput) {
            conversionReceivedAmountInput.required = true;
        }
        updateConversionExchangeRateDisplay();
    }
    
    const receivedAmountInput = document.getElementById('transaction-received-amount');
    if (receivedAmountInput && type !== 'INTERNAL_TRANSFER') {
        receivedAmountInput.required = false;
    }
    
    const conversionReceivedAmountInput = document.getElementById('conversion-received-amount');
    if (conversionReceivedAmountInput && type !== 'CURRENCY_CONVERSION') {
        conversionReceivedAmountInput.required = false;
    }
}

function updateCommissionDisplay() {
    const type = document.getElementById('transaction-type').value;
    if (type !== 'INTERNAL_TRANSFER') {
        return;
    }
    
    const amountInput = document.getElementById('transaction-amount');
    const receivedAmountInput = document.getElementById('transaction-received-amount');
    const commissionDisplay = document.getElementById('transaction-commission-display');
    
    if (!amountInput || !receivedAmountInput || !commissionDisplay) {
        return;
    }
    
    const amount = parseFloat(amountInput.value) || 0;
    const receivedAmount = parseFloat(receivedAmountInput.value) || 0;
    
    if (amount > 0 && receivedAmount > 0) {
        const commission = amount - receivedAmount;
        commissionDisplay.textContent = commission.toFixed(2);
        if (commission < 0) {
            commissionDisplay.style.color = '#d32f2f';
        } else if (commission > 0) {
            commissionDisplay.style.color = '#1976d2';
        } else {
            commissionDisplay.style.color = '#666';
        }
    } else {
        commissionDisplay.textContent = '0.00';
        commissionDisplay.style.color = '#666';
    }
}

function updateConversionExchangeRateDisplay() {
    const type = document.getElementById('transaction-type').value;
    if (type !== 'CURRENCY_CONVERSION') {
        return;
    }
    
    const amountInput = document.getElementById('transaction-amount');
    const receivedAmountInput = document.getElementById('conversion-received-amount');
    const exchangeRateDisplay = document.getElementById('conversion-exchange-rate-display');
    
    if (!amountInput || !receivedAmountInput || !exchangeRateDisplay) {
        return;
    }
    
    const amount = parseFloat(amountInput.value) || 0;
    const receivedAmount = parseFloat(receivedAmountInput.value) || 0;
    
    if (amount > 0 && receivedAmount > 0) {
        const exchangeRate = amount / receivedAmount;
        exchangeRateDisplay.textContent = exchangeRate.toFixed(6);
        exchangeRateDisplay.style.color = '#1976d2';
    } else {
        exchangeRateDisplay.textContent = '0.000000';
        exchangeRateDisplay.style.color = '#666';
    }
}

async function loadCounterparties(type) {
    try {
        const counterparties = await FinanceDataLoader.loadCounterparties(type);
        const select = document.getElementById('transaction-counterparty');
        if (select) {
            select.textContent = '';
            const defaultOption = document.createElement('option');
            defaultOption.value = '';
            defaultOption.textContent = 'Без контрагента';
            select.appendChild(defaultOption);
            if (Array.isArray(counterparties)) {
                counterparties.forEach(cp => {
                    const option = document.createElement('option');
                    option.value = cp.id;
                    option.textContent = cp.name || '';
                    select.appendChild(option);
                });
            }
        }
    } catch (error) {
        console.error('Error loading counterparties:', error);
    }
}

async function handleCreateTransaction(e) {
    e.preventDefault();
    const categoryValue = document.getElementById('transaction-category').value;
    const formData = {
        type: document.getElementById('transaction-type').value,
        amount: parseFloat(document.getElementById('transaction-amount').value),
        currency: document.getElementById('transaction-currency').value,
        description: document.getElementById('transaction-description').value
    };
    
    if (categoryValue && categoryValue.trim() !== '') {
        formData.categoryId = parseInt(categoryValue);
    }

    const type = formData.type;
    if (type === 'INTERNAL_TRANSFER') {
        formData.fromAccountId = parseInt(document.getElementById('from-account').value);
        formData.toAccountId = parseInt(document.getElementById('to-account').value);
        
        const receivedAmountValue = document.getElementById('transaction-received-amount').value;
        if (!receivedAmountValue || receivedAmountValue.trim() === '') {
            if (typeof showMessage === 'function') {
                showMessage('Вкажіть суму зачислення', 'error');
            }
            return;
        }
        
        const receivedAmount = parseFloat(receivedAmountValue);
        const amount = formData.amount;
        
        if (receivedAmount > amount) {
            if (typeof showMessage === 'function') {
                showMessage('Сума зачислення не може бути більшою за суму списання', 'error');
            }
            return;
        }
        
        if (receivedAmount <= 0) {
            if (typeof showMessage === 'function') {
                showMessage('Сума зачислення повинна бути більше нуля', 'error');
            }
            return;
        }
        
        const commission = amount - receivedAmount;
        if (commission > 0) {
            formData.commission = commission;
        }
    } else if (type === 'EXTERNAL_INCOME') {
        formData.toAccountId = parseInt(document.getElementById('to-account').value);
        const counterpartyId = document.getElementById('transaction-counterparty').value;
        if (counterpartyId && counterpartyId.trim() !== '') {
            formData.counterpartyId = parseInt(counterpartyId);
        }
    } else if (type === 'EXTERNAL_EXPENSE') {
        formData.fromAccountId = parseInt(document.getElementById('from-account').value);
        const counterpartyId = document.getElementById('transaction-counterparty').value;
        if (counterpartyId && counterpartyId.trim() !== '') {
            formData.counterpartyId = parseInt(counterpartyId);
        }
    } else if (type === 'CLIENT_PAYMENT') {
        formData.fromAccountId = parseInt(document.getElementById('from-account').value);
        const clientId = document.getElementById('transaction-client-id').value;
        if (!clientId) {
            if (typeof showMessage === 'function') {
                showMessage('Виберіть клієнта', 'error');
            }
            return;
        }
        formData.clientId = parseInt(clientId);
    } else if (type === 'CURRENCY_CONVERSION') {
        const accountId = parseInt(document.getElementById('conversion-account').value);
        formData.fromAccountId = accountId;
        formData.toAccountId = accountId;
        formData.convertedCurrency = document.getElementById('conversion-currency').value;
        
        const receivedAmountValue = document.getElementById('conversion-received-amount').value;
        if (!receivedAmountValue || receivedAmountValue.trim() === '') {
            if (typeof showMessage === 'function') {
                showMessage('Вкажіть суму зачислення', 'error');
            }
            return;
        }
        
        const receivedAmount = parseFloat(receivedAmountValue);
        const amount = formData.amount;
        
        if (receivedAmount <= 0) {
            if (typeof showMessage === 'function') {
                showMessage('Сума зачислення повинна бути більше нуля', 'error');
            }
            return;
        }
        
        if (amount <= 0) {
            if (typeof showMessage === 'function') {
                showMessage('Сума списання повинна бути більше нуля', 'error');
            }
            return;
        }
        
        const exchangeRate = amount / receivedAmount;
        formData.exchangeRate = exchangeRate;
        formData.convertedAmount = receivedAmount;
    }

    const canOperateAccount = (accountId) => {
        const account = accountsCache.find(a => a.id === accountId);
        if (!account) return false;
        if (!account.branchId) return true;
        const branch = branchesCache.find(b => b.id === account.branchId);
        return branch ? (branch.canOperate === true) : false;
    };
    
    if (formData.fromAccountId && !canOperateAccount(formData.fromAccountId)) {
        if (typeof showMessage === 'function') {
            showMessage('У вас немає прав на виконання операцій з цим рахунком', 'error');
        }
        return;
    }
    if (formData.toAccountId && !canOperateAccount(formData.toAccountId)) {
        if (typeof showMessage === 'function') {
            showMessage('У вас немає прав на виконання операцій з цим рахунком', 'error');
        }
        return;
    }

    try {
        await FinanceDataLoader.createTransaction(formData);
        if (typeof showMessage === 'function') {
            showMessage('Транзакцію успішно створено', 'success');
        }
        const createModal = document.getElementById('create-transaction-modal');
        if (createModal) {
            createModal.classList.remove('show');
            setTimeout(() => {
                createModal.style.display = 'none';
            }, CLIENT_CONSTANTS.MODAL_CLOSE_DELAY);
        }
        document.getElementById('transaction-form').reset();
        
        const clientInput = document.getElementById('transaction-client');
        const clientHidden = document.getElementById('transaction-client-id');
        if (clientInput) clientInput.value = '';
        if (clientHidden) clientHidden.value = '';
        
        const commissionDisplay = document.getElementById('transaction-commission-display');
        if (commissionDisplay) {
            commissionDisplay.textContent = '0.00';
            commissionDisplay.style.color = '#666';
        }
        
        const conversionExchangeRateDisplay = document.getElementById('conversion-exchange-rate-display');
        if (conversionExchangeRateDisplay) {
            conversionExchangeRateDisplay.textContent = '0.000000';
            conversionExchangeRateDisplay.style.color = '#666';
        }
        
        const activeTab = document.querySelector('.tab-btn.active');
        if (activeTab) {
            const activeTabName = activeTab.getAttribute('data-tab');
            if (activeTabName === 'transactions') {
                await loadTransactions();
            } else if (activeTabName === 'accounts') {
                await loadAccountsAndBranches();
            }
        }
    } catch (error) {
        console.error('Error creating transaction:', error);
        handleError(error);
    }
}

function setupEventListeners() {
    document.getElementById('create-transaction-btn')?.addEventListener('click', () => {
        openCreateTransactionModal();
    });

    document.getElementById('transaction-form')?.addEventListener('submit', handleCreateTransaction);

    document.getElementById('transaction-type')?.addEventListener('change', handleTransactionTypeChange);
    
    document.getElementById('transaction-amount')?.addEventListener('input', () => {
        updateCommissionDisplay();
        updateConversionExchangeRateDisplay();
    });
    document.getElementById('transaction-received-amount')?.addEventListener('input', updateCommissionDisplay);
    document.getElementById('conversion-received-amount')?.addEventListener('input', updateConversionExchangeRateDisplay);

    document.getElementById('apply-transaction-filters')?.addEventListener('click', () => {
        currentTransactionPage = 0;
        loadTransactions();
    });

    document.getElementById('export-transactions-btn')?.addEventListener('click', exportTransactionsToExcel);

    document.getElementById('prev-page-btn')?.addEventListener('click', () => {
        if (currentTransactionPage > 0) {
            currentTransactionPage--;
            loadTransactions();
        }
    });

    document.getElementById('next-page-btn')?.addEventListener('click', () => {
        if (currentTransactionPage < totalTransactionPages - 1) {
            currentTransactionPage++;
            loadTransactions();
        }
    });

    document.getElementById('close-edit-transaction-modal')?.addEventListener('click', closeEditTransactionModal);
    document.getElementById('cancel-edit-transaction')?.addEventListener('click', closeEditTransactionModal);
    document.getElementById('edit-transaction-form')?.addEventListener('submit', handleUpdateTransaction);
}
