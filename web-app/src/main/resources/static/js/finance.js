let accountsCache = [];
let allAccountsCache = [];
let branchesCache = [];
let usersCache = [];
let allBalancesMap = null;
let currentSearchQuery = '';

let currentTransactionPage = 0;
const transactionPageSize = CLIENT_CONSTANTS.DEFAULT_PAGE_SIZE;
let totalTransactionPages = 1;

let isDataLoading = true;
let isDataLoaded = false;

document.addEventListener('DOMContentLoaded', async () => {
    showPageLoader();
    disableCreateTransactionButton();
    try {
        await loadInitialData();
        
        if (accountsCache && accountsCache.length > 0) {
            isDataLoaded = true;
            isDataLoading = false;
            hidePageLoader();
            enableCreateTransactionButton();
        } else {
            isDataLoading = false;
            hidePageLoader();
            showMessage('Не вдалося завантажити дані. Будь ласка, оновіть сторінку.', 'error');
        }
        
        initializeTabs();
        initializeModals();
        setupEventListeners();
    } catch (error) {
        isDataLoading = false;
        isDataLoaded = false;
        hidePageLoader();
        console.error('Error initializing finance page:', error);
        handleError(error);
    }
});

function initializeTabs() {
    FinanceTabs.init((targetTab) => {
            if (targetTab === 'accounts') {
                restoreAccountSearch();
                loadAccountsAndBranches();
            } else if (targetTab === 'transactions') {
            FinanceFilters.populateTransactionFilters(allAccountsCache, branchesCache, transactionFiltersCustomSelects).then((updatedCustomSelects) => {
                    if (updatedCustomSelects) {
                        transactionFiltersCustomSelects = updatedCustomSelects;
                    }
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
    
    FinanceModal.setupCreateTransactionModalHandlers({
        onPopulateForm: populateTransactionForm,
        onTypeChange: handleTransactionTypeChange,
        onResetForm: FinanceTransactionForm.resetFormDisplays,
        customSelects: financeCustomSelects
    });
    
    FinanceModal.setupEditTransactionModalHandlers();
}

async function loadInitialData() {
    try {
        const [branches, users] = await Promise.all([
            FinanceDataLoader.loadBranches(),
            FinanceDataLoader.loadUsers()
        ]);
        
        branchesCache = branches || [];
        usersCache = users || [];
        
        await loadAccountsAndBranches();
        
        FinanceUtils.initializeClientAutocomplete(
            'transaction-client',
            'transaction-client-id',
            'client-autocomplete-dropdown'
        );
    } catch (error) {
        console.error('Error loading initial data:', error);
        handleError(error);
        throw error;
    }
}

async function loadAccountsAndBranches() {
    try {
        restoreAccountSearch();
        
        if (usersCache.length === 0) {
            usersCache = await FinanceDataLoader.loadUsers();
        }
        if (branchesCache.length === 0) {
            branchesCache = await FinanceDataLoader.loadBranches();
        }
        
        allAccountsCache = await FinanceDataLoader.loadAccounts();
        allAccountsCache = allAccountsCache || [];
        
        if (currentSearchQuery) {
            const query = currentSearchQuery.toLowerCase();
            accountsCache = allAccountsCache.filter(account => {
                const accountName = (account.name || '').toLowerCase();
                return accountName.includes(query);
            });
        } else {
            accountsCache = [...allAccountsCache];
        }
        
        const branchAccountIds = [];
        branchesCache.forEach(branch => {
            const branchAccounts = allAccountsCache.filter(acc => acc.branchId === branch.id);
            branchAccounts.forEach(acc => {
                if (acc.id != null && acc.id !== undefined && !isNaN(Number(acc.id))) {
                    branchAccountIds.push(Number(acc.id));
                }
            });
        });
        
        const allAccountIds = [...new Set([
            ...allAccountsCache
                .map(acc => acc.id)
                .filter(id => id != null && id !== undefined && !isNaN(Number(id)))
                .map(id => Number(id)),
            ...branchAccountIds
        ])];
        allBalancesMap = await FinanceDataLoader.loadAccountBalancesBatch(allAccountIds);
        
        await renderFilteredAccounts();
    } catch (error) {
        console.error('Error loading accounts and branches:', error);
        handleError(error);
        throw error;
    }
}

async function renderFilteredAccounts() {
    const standaloneAccounts = accountsCache.filter(acc => !acc.branchId);
    const standaloneBalance = FinanceRenderer.calculateAccountsTotalBalanceFromMap(standaloneAccounts, allBalancesMap);
    const hasStandaloneMatchingAccounts = standaloneAccounts.length > 0;
    
    const branchesWithBalances = await Promise.all(
        branchesCache.map(async (branch) => {
            const branchAccounts = accountsCache.filter(acc => acc.branchId === branch.id);
            const totalBalance = FinanceRenderer.calculateAccountsTotalBalanceFromMap(branchAccounts, allBalancesMap);
            const hasMatchingAccounts = branchAccounts.length > 0;
            return { ...branch, totalBalance, hasMatchingAccounts };
        })
    );
    
    const sortedResult = sortBranchesAndStandalone(branchesWithBalances, hasStandaloneMatchingAccounts);
    
    await FinanceRenderer.renderAccountsAndBranches(
        sortedResult.branches, 
        accountsCache, 
        usersCache, 
        branchesCache,
        allBalancesMap,
        standaloneAccounts,
        standaloneBalance,
        sortedResult.standalonePosition
    );
}

function sortBranchesAndStandalone(branches, hasStandaloneMatchingAccounts) {
    if (!currentSearchQuery) {
        return { branches, standalonePosition: 'end' };
    }
    
    const sortedBranches = branches.sort((a, b) => {
        if (a.hasMatchingAccounts && !b.hasMatchingAccounts) {
            return -1;
        }
        if (!a.hasMatchingAccounts && b.hasMatchingAccounts) {
            return 1;
        }
        return 0;
    });
    
    let standalonePosition = 'end';
    if (hasStandaloneMatchingAccounts) {
        const branchesWithMatches = sortedBranches.filter(b => b.hasMatchingAccounts).length;
        if (branchesWithMatches === 0) {
            standalonePosition = 'start';
        } else {
            standalonePosition = branchesWithMatches;
        }
    }
    
    return { branches: sortedBranches, standalonePosition };
}

function restoreAccountSearch() {
    const accountSearchInput = document.getElementById('account-search-input');
    if (accountSearchInput) {
        const savedSearchValue = sessionStorage.getItem('finance-account-search');
        if (savedSearchValue) {
            accountSearchInput.value = savedSearchValue;
            currentSearchQuery = savedSearchValue;
        } else {
            accountSearchInput.value = '';
            currentSearchQuery = '';
        }
    }
}

function filterAccountsBySearch(searchQuery) {
    currentSearchQuery = searchQuery;
    
    if (!searchQuery) {
        accountsCache = [...allAccountsCache];
        sessionStorage.removeItem('finance-account-search');
    } else {
        const query = searchQuery.toLowerCase();
        accountsCache = allAccountsCache.filter(account => {
            const accountName = (account.name || '').toLowerCase();
            return accountName.includes(query);
        });
        sessionStorage.setItem('finance-account-search', searchQuery);
    }
    
    renderFilteredAccounts();
}

async function loadTransactions() {
    try {
        const filters = FinanceFilters.buildTransactionFilters(transactionFiltersCustomSelects);
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
        const filters = FinanceFilters.buildTransactionFilters(transactionFiltersCustomSelects);
        const blob = await FinanceDataLoader.exportTransactions(filters);
        
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `transactions_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.xlsx`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        
        showMessage(FINANCE_MESSAGES.TRANSACTIONS_EXPORTED, 'success');
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
        showMessage(FINANCE_MESSAGES.EXCHANGE_RATE_UPDATED, 'success');
        await loadExchangeRates();
    } catch (error) {
        console.error('Error updating exchange rate:', error);
        handleError(error);
    }
}

async function openEditTransactionModal(transactionId) {
    await FinanceModal.openEditTransactionModal(transactionId, {
        onLoadTransaction: null,
        onReloadData: null
    });
}

function closeEditTransactionModal() {
    FinanceModal.closeEditTransactionModal();
}

async function handleUpdateTransaction(e) {
    e.preventDefault();
    
    const { transactionId, updateData, shouldDelete } = FinanceTransactionForm.buildUpdateTransactionData();
    
    if (shouldDelete) {
        FinanceModal.showDeleteConfirmationModal(
            async () => {
                try {
                    await FinanceDataLoader.deleteTransaction(transactionId);
                    showMessage(FINANCE_MESSAGES.TRANSACTION_DELETED, 'success');
                    FinanceModal.closeEditTransactionModal();
                    
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
            },
            () => {}
        );
        return;
    }
    
    try {
        await FinanceDataLoader.updateTransaction(transactionId, updateData);
        showMessage(FINANCE_MESSAGES.TRANSACTION_UPDATED, 'success');
        FinanceModal.closeEditTransactionModal();
        
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
    if (isDataLoading) {
        showMessage('Завантаження даних... Будь ласка, зачекайте.', 'info');
        return;
    }
    
    if (!isDataLoaded || !allAccountsCache || allAccountsCache.length === 0) {
        showMessage('Дані ще не завантажені. Будь ласка, зачекайте або оновіть сторінку.', 'error');
        return;
    }
    
    FinanceModal.openCreateTransactionModal({
        onPopulateForm: populateTransactionForm,
        onTypeChange: handleTransactionTypeChange,
        onResetForm: FinanceTransactionForm.resetFormDisplays,
        customSelects: financeCustomSelects
    });
}

function showPageLoader() {
    const loaderBackdrop = document.querySelector('.loader-backdrop');
    if (loaderBackdrop) {
        loaderBackdrop.style.display = 'flex';
    }
}

function hidePageLoader() {
    const loaderBackdrop = document.querySelector('.loader-backdrop');
    if (loaderBackdrop) {
        loaderBackdrop.style.display = 'none';
    }
}

function enableCreateTransactionButton() {
    const createBtn = document.getElementById('create-transaction-btn');
    if (createBtn) {
        createBtn.disabled = false;
        createBtn.removeAttribute('disabled');
    }
}

function disableCreateTransactionButton() {
    const createBtn = document.getElementById('create-transaction-btn');
    if (createBtn) {
        createBtn.disabled = true;
        createBtn.setAttribute('disabled', 'disabled');
    }
}

let financeCustomSelects = {};
let transactionFiltersCustomSelects = {};

function populateTransactionForm() {
    const fromAccountSelect = document.getElementById('from-account');
    const toAccountSelect = document.getElementById('to-account');
    const conversionAccountSelect = document.getElementById('conversion-account');
    
    FinanceTransactionForm.populateAccounts(
        allAccountsCache,
        branchesCache,
        [fromAccountSelect, toAccountSelect, conversionAccountSelect]
    );
    
    [fromAccountSelect, toAccountSelect, conversionAccountSelect].forEach(select => {
        if (select && typeof createCustomSelect === 'function') {
            const selectId = select.id;
            if (!financeCustomSelects[selectId]) {
                financeCustomSelects[selectId] = createCustomSelect(select);
            }
            const accountData = Array.from(select.options)
                .filter(opt => opt.value !== '')
                .map(opt => ({ id: opt.value, name: opt.textContent }));
            if (financeCustomSelects[selectId] && accountData.length > 0) {
                financeCustomSelects[selectId].populate(accountData);
            }
        }
    });

    const currencySelect = document.getElementById('transaction-currency');
    const conversionCurrencySelect = document.getElementById('conversion-currency');
    
    FinanceTransactionForm.populateCurrencies([currencySelect, conversionCurrencySelect]);
    
    setupAccountBalanceListeners();
}

let accountBalanceHandlers = {};

function setupAccountBalanceListeners() {
    function handleAccountChange(accountSelectId, balanceElementId, currencySelectId) {
        const accountSelect = document.getElementById(accountSelectId);
        if (!accountSelect) return;
        
        if (accountBalanceHandlers[accountSelectId]) {
            accountSelect.removeEventListener('change', accountBalanceHandlers[accountSelectId]);
        }
        
        const handler = () => {
            let accountId = null;
            
            if (financeCustomSelects[accountSelectId]) {
                const values = financeCustomSelects[accountSelectId].getValue();
                accountId = values.length > 0 ? values[0] : null;
            } else {
                accountId = accountSelect.value;
            }
            
            if (allBalancesMap) {
                FinanceTransactionForm.displayAccountBalance(accountId, balanceElementId, allBalancesMap);
                
                if (currencySelectId && accountId) {
                    FinanceTransactionForm.updateCurrencyOptions(accountId, allBalancesMap, currencySelectId);
                } else if (currencySelectId && !accountId) {
                    const currencySelect = document.getElementById(currencySelectId);
                    if (currencySelect) {
                        FinanceTransactionForm.populateCurrencies([currencySelect]);
                    }
                }
            }
        };
        
        accountBalanceHandlers[accountSelectId] = handler;
        accountSelect.addEventListener('change', handler);
    }
    
    handleAccountChange('from-account', 'from-account-balance', 'transaction-currency');
    handleAccountChange('to-account', 'to-account-balance', null);
    handleAccountChange('conversion-account', 'conversion-account-balance', 'transaction-currency');
}


function handleTransactionTypeChange() {
    const type = document.getElementById('transaction-type').value;
    FinanceTransactionForm.handleTransactionTypeChange(type, {
        accountsCache: allAccountsCache,
        branchesCache,
        onLoadCounterparties: loadCounterparties,
        customSelects: financeCustomSelects,
        balancesMap: allBalancesMap
    });
    
    const currencySelect = document.getElementById('transaction-currency');
    if (currencySelect) {
        FinanceTransactionForm.populateCurrencies([currencySelect]);
    }
}

function updateCommissionDisplay() {
    FinanceTransactionForm.updateCommissionDisplay();
}

function updateConversionExchangeRateDisplay() {
    FinanceTransactionForm.updateConversionExchangeRateDisplay();
}

async function loadCounterparties(type) {
    try {
        const counterparties = await FinanceDataLoader.loadCounterparties(type);
        const select = document.getElementById('transaction-counterparty');
        if (select) {
            select.textContent = '';
            const defaultOption = document.createElement('option');
            defaultOption.value = '';
            defaultOption.textContent = FINANCE_MESSAGES.WITHOUT_COUNTERPARTY;
            select.appendChild(defaultOption);
            if (Array.isArray(counterparties)) {
                counterparties.forEach(cp => {
                    const option = document.createElement('option');
                    option.value = cp.id;
                    option.textContent = cp.name || '';
                    select.appendChild(option);
                });
            }
            
            if (typeof createCustomSelect === 'function') {
                const selectId = select.id;
                if (!financeCustomSelects[selectId]) {
                    financeCustomSelects[selectId] = createCustomSelect(select);
                }
                const counterpartyData = [
                    { id: '', name: FINANCE_MESSAGES.WITHOUT_COUNTERPARTY },
                    ...(Array.isArray(counterparties) 
                        ? counterparties.map(cp => ({ id: cp.id, name: cp.name || '' }))
                        : [])
                ];
                if (financeCustomSelects[selectId]) {
                    financeCustomSelects[selectId].populate(counterpartyData);
                }
            }
        }
    } catch (error) {
        console.error('Error loading counterparties:', error);
        handleError(error);
    }
}

async function handleCreateTransaction(e) {
    e.preventDefault();
    
    const formData = FinanceTransactionForm.buildTransactionFormData(financeCustomSelects);
    
    if (formData.error) {
        showMessage(formData.error, 'error');
        return;
    }
        
    if (formData.fromAccountId && !FinanceUtils.canOperateAccount(formData.fromAccountId, allAccountsCache, branchesCache)) {
        showMessage(FINANCE_MESSAGES.NO_ACCOUNT_PERMISSION, 'error');
        return;
    }
    if (formData.toAccountId && !FinanceUtils.canOperateAccount(formData.toAccountId, allAccountsCache, branchesCache)) {
        showMessage(FINANCE_MESSAGES.NO_ACCOUNT_PERMISSION, 'error');
        return;
    }
    
    try {
        await FinanceDataLoader.createTransaction(formData);
        showMessage(FINANCE_MESSAGES.TRANSACTION_CREATED, 'success');
        
        FinanceModal.closeCreateTransactionModal({
            onResetForm: FinanceTransactionForm.resetFormDisplays,
            customSelects: financeCustomSelects
        });
        
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
    const accountSearchInput = document.getElementById('account-search-input');
    if (accountSearchInput) {
        const savedSearchValue = sessionStorage.getItem('finance-account-search');
        if (savedSearchValue) {
            accountSearchInput.value = savedSearchValue;
            filterAccountsBySearch(savedSearchValue);
        }
        
        accountSearchInput.addEventListener('input', (e) => {
            const value = e.target.value.trim();
            if (value) {
                sessionStorage.setItem('finance-account-search', value);
            } else {
                sessionStorage.removeItem('finance-account-search');
            }
            filterAccountsBySearch(value);
        });
    }
    
    document.getElementById('create-transaction-btn')?.addEventListener('click', () => {
        FinanceModal.openCreateTransactionModal({
            onPopulateForm: populateTransactionForm,
            onTypeChange: handleTransactionTypeChange,
            customSelects: financeCustomSelects
        });
    });

    document.getElementById('transaction-form')?.addEventListener('submit', handleCreateTransaction);

    document.getElementById('transaction-type')?.addEventListener('change', () => {
        handleTransactionTypeChange();
        setupAccountBalanceListeners();
    });
    
    document.getElementById('transaction-amount')?.addEventListener('input', () => {
        updateCommissionDisplay();
        updateConversionExchangeRateDisplay();
    });
    document.getElementById('transaction-received-amount')?.addEventListener('input', updateCommissionDisplay);
    document.getElementById('conversion-received-amount')?.addEventListener('input', updateConversionExchangeRateDisplay);

    const openFiltersModalBtn = document.getElementById('open-transaction-filters-modal');
    const filtersModal = document.getElementById('transaction-filters-modal');
    const closeFiltersModalBtn = document.getElementById('close-transaction-filters-modal');
    
    if (openFiltersModalBtn && filtersModal) {
        openFiltersModalBtn.addEventListener('click', () => {
            FinanceFilters.openFiltersModal(filtersModal);
        });
    }
    
    if (closeFiltersModalBtn && filtersModal) {
        closeFiltersModalBtn.addEventListener('click', () => {
            FinanceFilters.closeFiltersModal(filtersModal);
        });
    }
    
    
    document.getElementById('apply-transaction-filters')?.addEventListener('click', () => {
        FinanceFilters.closeFiltersModal(filtersModal);
        currentTransactionPage = 0;
        loadTransactions();
    });
    
    document.getElementById('clear-transaction-filters')?.addEventListener('click', () => {
        FinanceFilters.clearFilters(transactionFiltersCustomSelects);
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

    document.getElementById('close-edit-transaction-modal')?.addEventListener('click', () => FinanceModal.closeEditTransactionModal());
    document.getElementById('edit-transaction-form')?.addEventListener('submit', handleUpdateTransaction);
}
