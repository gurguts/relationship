const API_BASE = '/api/v1';
let accountsCache = [];
let branchesCache = [];
let categoriesCache = [];
let usersCache = [];
let clientsCache = [];

const transactionTypeMap = {
    'INTERNAL_TRANSFER': 'Переказ між рахунками',
    'EXTERNAL_INCOME': 'Зовнішній прихід',
    'EXTERNAL_EXPENSE': 'Зовнішня витрата',
    'CLIENT_PAYMENT': 'Оплата клієнту',
    'CURRENCY_CONVERSION': 'Конвертація валют',
    'PURCHASE': 'Закупівля'
};

document.addEventListener('DOMContentLoaded', () => {
    initializeTabs();
    initializeModals();
    loadInitialData();
    setupEventListeners();
});

function initializeTabs() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.getAttribute('data-tab');

            // Remove active class from all
            tabButtons.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));

            // Add active class to clicked
            btn.classList.add('active');
            document.getElementById(`${targetTab}-tab`).classList.add('active');

            // Load data for active tab
            if (targetTab === 'accounts') {
                loadAccountsAndBranches();
            } else if (targetTab === 'transactions') {
                populateTransactionFilters().then(() => {
                    loadTransactions();
                });
            }
        });
    });
}

function initializeModals() {
    // Close modals on X click
    document.querySelectorAll('.close').forEach(closeBtn => {
        closeBtn.addEventListener('click', (e) => {
            const modal = e.target.closest('.modal');
            if (modal) {
                modal.style.display = 'none';
            }
        });
    });

    // Close modals on outside click
    window.addEventListener('click', (e) => {
        if (e.target.classList.contains('modal')) {
            e.target.style.display = 'none';
        }
    });
}

function setupEventListeners() {
    // Create buttons
    document.getElementById('create-transaction-btn').addEventListener('click', () => {
        openCreateTransactionModal();
    });

    // Forms
    document.getElementById('transaction-form').addEventListener('submit', handleCreateTransaction);
    // Account and Branch forms are still used for editing
    document.getElementById('account-form').addEventListener('submit', handleCreateAccount);
    document.getElementById('branch-form').addEventListener('submit', handleCreateBranch);

    // Transaction type change
    document.getElementById('transaction-type').addEventListener('change', handleTransactionTypeChange);

    // Transaction filters
    document.getElementById('apply-transaction-filters')?.addEventListener('click', () => {
        currentTransactionPage = 0;
        loadTransactions();
    });

    // Export transactions
    document.getElementById('export-transactions-btn')?.addEventListener('click', exportTransactionsToExcel);

    // Transaction pagination
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

    // Edit transaction modal
    document.getElementById('close-edit-transaction-modal')?.addEventListener('click', closeEditTransactionModal);
    document.getElementById('cancel-edit-transaction')?.addEventListener('click', closeEditTransactionModal);
    document.getElementById('edit-transaction-form')?.addEventListener('submit', handleUpdateTransaction);
}

async function loadInitialData() {
    try {
        await Promise.all([
            loadBranchesList(),
            loadUsers(),
            loadClients()
        ]);
        // Load accounts and branches together
        await loadAccountsAndBranches();
        // Initialize client autocomplete after clients are loaded
        initializeClientAutocomplete();
    } catch (error) {
        console.error('Error loading initial data:', error);
        showFinanceMessage('Помилка завантаження даних', 'error');
    }
}

// ========== ACCOUNTS AND BRANCHES ==========

async function loadAccountsAndBranches() {
    try {
        // Ensure users and branches are loaded before rendering
        if (usersCache.length === 0) {
            await loadUsers();
        }
        if (branchesCache.length === 0) {
            await loadBranchesList();
        }
        
        // Load accounts
        const accountsResponse = await fetch(`${API_BASE}/accounts`);
        if (!accountsResponse.ok) throw new Error('Failed to load accounts');
        accountsCache = await accountsResponse.json();
        
        // Load balances for all branches
        const branchesWithBalances = await Promise.all(
            branchesCache.map(async (branch) => {
                const totalBalance = await calculateBranchTotalBalance(branch.id);
                return { ...branch, totalBalance };
            })
        );
        
        renderAccountsAndBranches(branchesWithBalances, accountsCache);
    } catch (error) {
        console.error('Error loading accounts and branches:', error);
        showFinanceMessage('Помилка завантаження рахунків та філій', 'error');
    }
}

async function renderAccountsAndBranches(branches, accounts) {
    const container = document.getElementById('accounts-branches-container');
    container.innerHTML = '';
    
    // Group accounts by branch
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
    
    // Render branches with their accounts
    const branchSections = await Promise.all(
        branches.map(async (branch) => {
            const branchAccounts = accountsByBranch[branch.id] || [];
            return await createBranchSection(branch, branchAccounts);
        })
    );
    branchSections.forEach(section => container.appendChild(section));
    
    // Render standalone accounts (without branch) with total balance
    if (standaloneAccounts.length > 0) {
        const standaloneBalance = await calculateAccountsTotalBalance(standaloneAccounts);
        const standaloneSection = await createStandaloneAccountsSection(standaloneAccounts, standaloneBalance);
        container.appendChild(standaloneSection);
    }
}

async function createBranchSection(branch, accounts) {
    const section = document.createElement('div');
    section.className = 'branch-section';
    
    const branchHeader = document.createElement('div');
    branchHeader.className = 'branch-header';
    branchHeader.innerHTML = `
        <div class="branch-header-content">
            <div class="branch-info">
                <h3 class="branch-name">${branch.name}</h3>
                ${branch.description ? `<p class="branch-description">${branch.description}</p>` : ''}
            </div>
            <div class="branch-balance">
                <span class="balance-label">Загальний баланс:</span>
                <span class="balance-value">${formatBranchBalance(branch.totalBalance)}</span>
            </div>
            <div class="branch-actions">
                ${branch.canOperate ? `<button class="action-btn btn-edit" onclick="editBranch(${branch.id})">Редагувати</button>` : ''}
                ${branch.canOperate ? `<button class="action-btn btn-delete" onclick="deleteBranch(${branch.id})">Видалити</button>` : ''}
            </div>
        </div>
    `;
    
    const accountsContainer = document.createElement('div');
    accountsContainer.className = 'branch-accounts';
    
    if (accounts.length === 0) {
        accountsContainer.innerHTML = '<p class="no-accounts">Немає рахунків у цій філії</p>';
    } else {
        // Load account rows asynchronously
        const accountRows = await Promise.all(
            accounts.map(account => createAccountRow(account, true))
        );
        accountRows.forEach(row => accountsContainer.appendChild(row));
    }
    
    section.appendChild(branchHeader);
    section.appendChild(accountsContainer);
    
    return section;
}

async function createStandaloneAccountsSection(accounts, totalBalance) {
    const section = document.createElement('div');
    section.className = 'branch-section standalone-accounts';
    
    const header = document.createElement('div');
    header.className = 'branch-header';
    header.innerHTML = `
        <div class="branch-header-content">
            <div class="branch-info">
                <h3 class="branch-name">Рахунки без філії</h3>
            </div>
            <div class="branch-balance">
                <span class="balance-label">Загальний баланс:</span>
                <span class="balance-value">${formatBranchBalance(totalBalance)}</span>
            </div>
        </div>
    `;
    
    const accountsContainer = document.createElement('div');
    accountsContainer.className = 'branch-accounts';
    
    // Load account rows asynchronously
    const accountRows = await Promise.all(
        accounts.map(account => createAccountRow(account, false))
    );
    accountRows.forEach(row => accountsContainer.appendChild(row));
    
    section.appendChild(header);
    section.appendChild(accountsContainer);
    
    return section;
}

async function createAccountRow(account, isInBranch) {
    const row = document.createElement('div');
    row.className = `account-row ${isInBranch ? 'account-in-branch' : ''}`;
    
    // Check if user can operate on this account
    let canOperateAccount = true;
    if (account.branchId) {
        const branch = branchesCache.find(b => b.id === account.branchId);
        canOperateAccount = branch ? (branch.canOperate === true) : false;
    }
    
    const currenciesHtml = Array.from(account.currencies || [])
        .map(c => `<span class="currency-badge">${c}</span>`)
        .join('');
    
    // Load balances for this account
    let balancesHtml = '<div class="account-balances-loading">Завантаження...</div>';
    try {
        const balancesResponse = await fetch(`${API_BASE}/accounts/${account.id}/balances`);
        if (balancesResponse.ok) {
            const balances = await balancesResponse.json();
            if (balances && balances.length > 0) {
                balancesHtml = balances.map(balance => 
                    `<div class="balance-item-row">
                        <span class="balance-currency">${balance.currency}</span>
                        <span class="balance-amount">${formatNumber(balance.amount)}</span>
                    </div>`
                ).join('');
            } else {
                balancesHtml = '<div class="account-balances-empty">Немає балансів</div>';
            }
        }
    } catch (error) {
        console.warn(`Failed to load balances for account ${account.id}`, error);
        balancesHtml = '<div class="account-balances-error">Помилка завантаження</div>';
    }
    
    row.innerHTML = `
        <div class="account-info">
            <div class="account-name">${account.name}</div>
            <div class="account-details">
                ${account.userId ? `<span class="account-user">👤 ${getUserName(account.userId)}</span>` : ''}
                <span class="account-currencies">${currenciesHtml}</span>
            </div>
            <div class="account-balances">
                ${balancesHtml}
            </div>
        </div>
        <div class="account-actions">
            ${canOperateAccount ? `<button class="action-btn btn-edit" onclick="editAccount(${account.id})">Редагувати</button>` : ''}
            ${canOperateAccount ? `<button class="action-btn btn-delete" onclick="deleteAccount(${account.id})">Видалити</button>` : ''}
            ${!canOperateAccount ? `<span class="no-permission-hint">Немає прав на операції</span>` : ''}
        </div>
    `;
    
    return row;
}

async function viewAccountBalances(accountId, accountName) {
    try {
        const response = await fetch(`${API_BASE}/accounts/${accountId}/balances`);
        if (!response.ok) throw new Error('Failed to load balances');
        const balances = await response.json();

        document.getElementById('balances-account-name').textContent = `Баланси рахунку: ${accountName}`;
        const tbody = document.getElementById('balances-body');
        tbody.innerHTML = '';

        balances.forEach(balance => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${balance.currency}</td>
                <td class="balance-value">${formatNumber(balance.amount)}</td>
            `;
            tbody.appendChild(row);
        });

        document.getElementById('view-balances-modal').style.display = 'block';
    } catch (error) {
        console.error('Error loading balances:', error);
        showFinanceMessage('Помилка завантаження балансів', 'error');
    }
}

// ========== BRANCHES ==========

async function loadBranchesList() {
    try {
        const response = await fetch(`${API_BASE}/branches`);
        if (!response.ok) throw new Error('Failed to load branches');
        branchesCache = await response.json();
        return branchesCache;
    } catch (error) {
        console.error('Error loading branches list:', error);
        return [];
    }
}

async function loadBranches() {
    try {
        // Load branches list first if not loaded
        if (branchesCache.length === 0) {
            await loadBranchesList();
        }
        
        // Load balances for each branch
        const branchesWithBalances = await Promise.all(
            branchesCache.map(async (branch) => {
                const totalBalance = await calculateBranchTotalBalance(branch.id);
                return { ...branch, totalBalance };
            })
        );
        
        renderBranches(branchesWithBalances);
    } catch (error) {
        console.error('Error loading branches:', error);
        showFinanceMessage('Помилка завантаження філій', 'error');
    }
}

async function calculateBranchTotalBalance(branchId) {
    try {
        // Get all accounts for this branch
        const accountsResponse = await fetch(`${API_BASE}/accounts/branch/${branchId}`);
        if (!accountsResponse.ok) {
            console.warn(`Failed to load accounts for branch ${branchId}`);
            return {};
        }
        
        const accounts = await accountsResponse.json();
        if (!accounts || accounts.length === 0) {
            return {};
        }
        
        return await calculateAccountsTotalBalance(accounts);
    } catch (error) {
        console.error(`Error calculating balance for branch ${branchId}:`, error);
        return {};
    }
}

async function calculateAccountsTotalBalance(accounts) {
    try {
        if (!accounts || accounts.length === 0) {
            return {};
        }
        
        // Get balances for all accounts
        const balancePromises = accounts.map(async (account) => {
            try {
                const balancesResponse = await fetch(`${API_BASE}/accounts/${account.id}/balances`);
                if (!balancesResponse.ok) return [];
                return await balancesResponse.json();
            } catch (error) {
                console.warn(`Failed to load balances for account ${account.id}`, error);
                return [];
            }
        });
        
        const allBalances = await Promise.all(balancePromises);
        const flatBalances = allBalances.flat();
        
        // Sum balances by currency
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

function formatBranchBalance(balanceObj) {
    if (!balanceObj || Object.keys(balanceObj).length === 0) {
        return '<span class="text-muted">Немає балансу</span>';
    }
    
    const parts = [];
    const currencies = ['UAH', 'USD', 'EUR'];
    
    currencies.forEach(currency => {
        if (balanceObj[currency] !== undefined && balanceObj[currency] !== 0) {
            const amount = formatNumber(balanceObj[currency]);
            parts.push(`${amount} ${currency}`);
        }
    });
    
    if (parts.length === 0) {
        return '<span class="text-muted">0.00</span>';
    }
    
    return parts.join(', ');
}

function renderBranches(branches) {
    const tbody = document.getElementById('branches-body');
    tbody.innerHTML = '';

    branches.forEach(branch => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${branch.name}</td>
            <td>${branch.description || '-'}</td>
            <td>${formatBranchBalance(branch.totalBalance)}</td>
            <td>
                <div class="action-buttons-table">
                    <button class="action-btn btn-edit" onclick="editBranch(${branch.id})">Редагувати</button>
                    <button class="action-btn btn-delete" onclick="deleteBranch(${branch.id})">Видалити</button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// ========== CATEGORIES (for transactions only) ==========

// ========== TRANSACTIONS ==========

let currentTransactionPage = 0;
const transactionPageSize = 50;
let totalTransactionPages = 1;
let transactionFilters = {};

async function loadTransactions() {
    try {
        const filters = buildTransactionFilters();
        const filtersJson = JSON.stringify(filters);
        const params = new URLSearchParams({
            page: currentTransactionPage.toString(),
            size: transactionPageSize.toString(),
            sort: 'createdAt',
            direction: 'DESC',
            filters: filtersJson
        });

        const response = await fetch(`${API_BASE}/transaction/search?${params}`);
        if (!response.ok) throw new Error('Failed to load transactions');
        
        const data = await response.json();
        totalTransactionPages = data.totalPages || 1;
        renderTransactions(data.content || []);
        updateTransactionPagination();
    } catch (error) {
        console.error('Error loading transactions:', error);
        showFinanceMessage('Помилка завантаження транзакцій', 'error');
    }
}

function buildTransactionFilters() {
    const filters = {};
    
    const type = document.getElementById('transaction-type-filter')?.value;
    if (type) {
        filters.type = [type];
    }

    const accountId = document.getElementById('transaction-account-filter')?.value;
    if (accountId) {
        filters.account_id = [accountId];
    }

    const categoryId = document.getElementById('transaction-category-filter')?.value;
    if (categoryId) {
        filters.category_id = [categoryId];
    }

    const dateFrom = document.getElementById('transaction-date-from')?.value;
    if (dateFrom) {
        filters.created_at_from = [dateFrom];
    }

    const dateTo = document.getElementById('transaction-date-to')?.value;
    if (dateTo) {
        filters.created_at_to = [dateTo];
    }

    return filters;
}

async function exportTransactionsToExcel() {
    try {
        const filters = buildTransactionFilters();
        const filtersJson = JSON.stringify(filters);
        const params = new URLSearchParams({
            filters: filtersJson
        });

        const response = await fetch(`${API_BASE}/transaction/export?${params}`);
        if (!response.ok) throw new Error('Failed to export transactions');
        
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `transactions_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.xlsx`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        
        showFinanceMessage('Транзакції успішно експортовано', 'success');
    } catch (error) {
        console.error('Error exporting transactions:', error);
        showFinanceMessage('Помилка експорту транзакцій', 'error');
    }
}

function renderTransactions(transactions) {
    const tbody = document.getElementById('transactions-body');
    tbody.innerHTML = '';

    if (transactions.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = '<td colspan="9" style="text-align: center;">Транзакції не знайдено</td>';
        tbody.appendChild(row);
        return;
    }

    transactions.forEach(transaction => {
        const row = document.createElement('tr');
        const date = transaction.createdAt ? new Date(transaction.createdAt).toLocaleDateString('uk-UA') : '—';
        const type = transactionTypeMap[transaction.type] || transaction.type;
        const category = transaction.categoryName || '—';
        const fromAccount = transaction.fromAccountName || '—';
        const toAccount = transaction.toAccountName || '—';
        const amount = formatNumber(transaction.amount);
        const currency = transaction.currency || '—';
        const client = transaction.clientCompany || '—';
        const description = transaction.description || '—';

        // For currency conversion, show converted amount
        let amountDisplay = amount;
        if (transaction.type === 'CURRENCY_CONVERSION' && transaction.convertedAmount) {
            amountDisplay = `${amount} → ${formatNumber(transaction.convertedAmount)} ${transaction.convertedCurrency || ''}`;
        }

        row.innerHTML = `
            <td>${date}</td>
            <td>${type}</td>
            <td>${category}</td>
            <td>${fromAccount}</td>
            <td>${toAccount}</td>
            <td>${amountDisplay}</td>
            <td>${currency}</td>
            <td>${client}</td>
            <td>${description}</td>
            <td>
                <div class="action-buttons-table">
                    <button class="action-btn btn-edit" onclick="openEditTransactionModal(${transaction.id})">Редагувати</button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function updateTransactionPagination() {
    const prevBtn = document.getElementById('prev-page-btn');
    const nextBtn = document.getElementById('next-page-btn');
    const pageInfo = document.getElementById('page-info');

    if (prevBtn) {
        prevBtn.disabled = currentTransactionPage === 0;
    }
    if (nextBtn) {
        nextBtn.disabled = currentTransactionPage >= totalTransactionPages - 1;
    }
    if (pageInfo) {
        pageInfo.textContent = `Сторінка ${currentTransactionPage + 1} з ${totalTransactionPages || 1}`;
    }
}

// ========== EDIT TRANSACTION ==========

async function openEditTransactionModal(transactionId) {
    try {
        const response = await fetch(`${API_BASE}/transaction/${transactionId}`);
        if (!response.ok) throw new Error('Failed to load transaction');
        
        const transaction = await response.json();
        
        document.getElementById('edit-transaction-id').value = transaction.id;
        document.getElementById('edit-transaction-amount').value = transaction.amount;
        document.getElementById('edit-transaction-description').value = transaction.description || '';
        
        // Handle transaction type
        let typeStr;
        if (typeof transaction.type === 'string') {
            typeStr = transaction.type;
        } else if (transaction.type && transaction.type.name) {
            typeStr = transaction.type.name;
        } else if (transaction.type) {
            typeStr = transaction.type.toString();
        }
        
        // Show/hide exchange rate field for currency conversion
        const exchangeRateGroup = document.getElementById('edit-exchange-rate-group');
        const exchangeRateInput = document.getElementById('edit-transaction-exchange-rate');
        if (typeStr === 'CURRENCY_CONVERSION') {
            exchangeRateGroup.style.display = 'block';
            exchangeRateInput.value = transaction.exchangeRate || '';
        } else {
            exchangeRateGroup.style.display = 'none';
            exchangeRateInput.value = '';
        }
        
        // Load categories for this transaction type
        const categorySelect = document.getElementById('edit-transaction-category');
        categorySelect.innerHTML = '<option value="">Без категорії</option>';
        
        if (typeStr) {
            const categories = await loadCategoriesForType(typeStr);
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
        
        document.getElementById('edit-transaction-modal').style.display = 'block';
    } catch (error) {
        console.error('Error loading transaction:', error);
        showFinanceMessage('Помилка завантаження транзакції', 'error');
    }
}

function closeEditTransactionModal() {
    document.getElementById('edit-transaction-modal').style.display = 'none';
    document.getElementById('edit-transaction-form').reset();
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
    
    const updateData = {
        categoryId: categoryId ? parseInt(categoryId) : null,
        description: description || null,
        amount: amount
    };
    
    // Add exchange rate for currency conversion
    if (exchangeRate !== null && !isNaN(exchangeRate)) {
        updateData.exchangeRate = exchangeRate;
    }
    
    try {
        const response = await fetch(`${API_BASE}/transaction/${transactionId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(updateData)
        });
        
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to update transaction');
        }
        
        showFinanceMessage('Транзакцію успішно оновлено', 'success');
        closeEditTransactionModal();
        loadTransactions(); // Reload transactions list
    } catch (error) {
        console.error('Error updating transaction:', error);
        showFinanceMessage('Помилка оновлення транзакції: ' + error.message, 'error');
    }
}

async function loadCategoriesForType(type) {
    if (!type) return [];
    try {
        const response = await fetch(`${API_BASE}/transaction-categories/type/${type}`);
        if (!response.ok) return [];
        const categories = await response.json();
        return Array.isArray(categories) ? categories : [];
    } catch (error) {
        console.error('Error loading categories:', error);
        return [];
    }
}

async function populateTransactionFilters() {
    // Populate account filter - only show accounts user can view
    const accountFilter = document.getElementById('transaction-account-filter');
    if (accountFilter) {
        accountFilter.innerHTML = '<option value="">Всі рахунки</option>';
        accountsCache.forEach(account => {
            const option = document.createElement('option');
            option.value = account.id;
            option.textContent = account.name;
            accountFilter.appendChild(option);
        });
    }
    
    // Helper function to check if account can be operated
    const canOperateAccount = (account) => {
        if (!account.branchId) return true; // Standalone accounts are always operable
        const branch = branchesCache.find(b => b.id === account.branchId);
        return branch ? (branch.canOperate === true) : false;
    };

    // Load all categories for filter
    try {
        const types = ['INTERNAL_TRANSFER', 'EXTERNAL_INCOME', 'EXTERNAL_EXPENSE', 'CLIENT_PAYMENT', 'CURRENCY_CONVERSION'];
        const promises = types.map(type => 
            fetch(`${API_BASE}/transaction-categories/type/${type}`)
                .then(r => r.ok ? r.json() : [])
        );
        const results = await Promise.all(promises);
        const allCategories = results.flat();

        const categoryFilter = document.getElementById('transaction-category-filter');
        if (categoryFilter) {
            categoryFilter.innerHTML = '<option value="">Всі категорії</option>';
            allCategories.forEach(category => {
                const option = document.createElement('option');
                option.value = category.id;
                option.textContent = `${transactionTypeMap[category.type] || category.type} - ${category.name}`;
                categoryFilter.appendChild(option);
            });
        }
    } catch (error) {
        console.error('Error loading categories for filter:', error);
    }
}

// ========== MODAL HANDLERS ==========

function openCreateTransactionModal() {
    const modal = document.getElementById('create-transaction-modal');
    populateTransactionForm();
    modal.style.display = 'block';
}

function populateTransactionForm() {
    // Helper function to check if account can be operated
    const canOperateAccount = (account) => {
        if (!account.branchId) return true; // Standalone accounts are always operable
        const branch = branchesCache.find(b => b.id === account.branchId);
        return branch ? (branch.canOperate === true) : false;
    };
    
    // Populate accounts - only show accounts user can operate
    const fromAccountSelect = document.getElementById('from-account');
    const toAccountSelect = document.getElementById('to-account');
    const conversionAccountSelect = document.getElementById('conversion-account');

    [fromAccountSelect, toAccountSelect, conversionAccountSelect].forEach(select => {
        if (!select) return;
        select.innerHTML = '<option value="">Виберіть рахунок</option>';
        accountsCache.forEach(account => {
            // Only show accounts user can operate
            if (canOperateAccount(account)) {
                const option = document.createElement('option');
                option.value = account.id;
                option.textContent = account.name;
                select.appendChild(option);
            }
        });
    });

    // Populate currencies
    const currencySelect = document.getElementById('transaction-currency');
    const conversionCurrencySelect = document.getElementById('conversion-currency');
    const currencies = ['UAH', 'USD', 'EUR'];
    
    [currencySelect, conversionCurrencySelect].forEach(select => {
        if (select) {
            select.innerHTML = '<option value="">Виберіть валюту</option>';
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
            dropdown.innerHTML = '<div class="client-autocomplete-item">Клієнтів не знайдено</div>';
            dropdown.style.display = 'block';
            return;
        }

        dropdown.innerHTML = '';
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

    // Close dropdown when clicking outside
    document.addEventListener('click', (e) => {
        if (!input.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.style.display = 'none';
        }
    });

    // Clear selection when input is cleared
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
    const exchangeRateGroup = document.getElementById('exchange-rate-group');
    const clientGroup = document.getElementById('client-group');
    const currencyGroup = document.getElementById('currency-group');
    const amountGroup = document.getElementById('amount-group');

    // Reset visibility
    fromAccountGroup.style.display = 'none';
    toAccountGroup.style.display = 'none';
    conversionAccountGroup.style.display = 'none';
    conversionCurrencyGroup.style.display = 'none';
    exchangeRateGroup.style.display = 'none';
    clientGroup.style.display = 'none';
    
    // Reset labels
    if (currencyGroup) {
        const label = currencyGroup.querySelector('label');
        if (label) label.textContent = 'Валюта:';
    }

    // Load categories for this type
    loadCategoriesForType(type).then(categories => {
        const select = document.getElementById('transaction-category');
        if (select) {
            select.innerHTML = '<option value="">Виберіть категорію</option>';
            if (Array.isArray(categories)) {
                categories.forEach(cat => {
                    const option = document.createElement('option');
                    option.value = cat.id;
                    option.textContent = cat.name;
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
    } else if (type === 'EXTERNAL_INCOME') {
        toAccountGroup.style.display = 'block';
        currencyGroup.style.display = 'block';
        amountGroup.style.display = 'block';
    } else if (type === 'EXTERNAL_EXPENSE') {
        fromAccountGroup.style.display = 'block';
        currencyGroup.style.display = 'block';
        amountGroup.style.display = 'block';
    } else if (type === 'CLIENT_PAYMENT') {
        fromAccountGroup.style.display = 'block';
        clientGroup.style.display = 'block';
        currencyGroup.style.display = 'block';
        amountGroup.style.display = 'block';
    } else if (type === 'CURRENCY_CONVERSION') {
        conversionAccountGroup.style.display = 'block';
        conversionCurrencyGroup.style.display = 'block';
        exchangeRateGroup.style.display = 'block';
        currencyGroup.style.display = 'block';
        amountGroup.style.display = 'block';
        // Change label for currency conversion
        if (currencyGroup) {
            const label = currencyGroup.querySelector('label');
            if (label) label.textContent = 'З валюту:';
        }
    }
}


async function handleCreateTransaction(e) {
    e.preventDefault();
    const formData = {
        type: document.getElementById('transaction-type').value,
        categoryId: parseInt(document.getElementById('transaction-category').value),
        amount: parseFloat(document.getElementById('transaction-amount').value),
        currency: document.getElementById('transaction-currency').value,
        description: document.getElementById('transaction-description').value
    };

    const type = formData.type;
    if (type === 'INTERNAL_TRANSFER') {
        formData.fromAccountId = parseInt(document.getElementById('from-account').value);
        formData.toAccountId = parseInt(document.getElementById('to-account').value);
    } else if (type === 'EXTERNAL_INCOME') {
        formData.toAccountId = parseInt(document.getElementById('to-account').value);
    } else if (type === 'EXTERNAL_EXPENSE') {
        formData.fromAccountId = parseInt(document.getElementById('from-account').value);
    } else if (type === 'CLIENT_PAYMENT') {
        formData.fromAccountId = parseInt(document.getElementById('from-account').value);
        const clientId = document.getElementById('transaction-client-id').value;
        if (!clientId) {
            showFinanceMessage('Виберіть клієнта', 'error');
            return;
        }
        formData.clientId = parseInt(clientId);
    } else if (type === 'CURRENCY_CONVERSION') {
        const accountId = parseInt(document.getElementById('conversion-account').value);
        formData.fromAccountId = accountId;
        formData.toAccountId = accountId;
        formData.convertedCurrency = document.getElementById('conversion-currency').value;
        formData.exchangeRate = parseFloat(document.getElementById('exchange-rate').value);
    }

    // Check permissions before creating transaction
    const canOperateAccount = (accountId) => {
        const account = accountsCache.find(a => a.id === accountId);
        if (!account) return false;
        if (!account.branchId) return true; // Standalone accounts are always operable
        const branch = branchesCache.find(b => b.id === account.branchId);
        return branch ? (branch.canOperate === true) : false;
    };
    
    // Validate permissions for accounts used in transaction
    if (formData.fromAccountId && !canOperateAccount(formData.fromAccountId)) {
        showFinanceMessage('У вас немає прав на виконання операцій з цим рахунком', 'error');
        return;
    }
    if (formData.toAccountId && !canOperateAccount(formData.toAccountId)) {
        showFinanceMessage('У вас немає прав на виконання операцій з цим рахунком', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/transaction`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || 'Failed to create transaction');
        }
        showFinanceMessage('Транзакцію успішно створено', 'success');
        document.getElementById('create-transaction-modal').style.display = 'none';
        document.getElementById('transaction-form').reset();
        // Reset client autocomplete
        const clientInput = document.getElementById('transaction-client');
        const clientHidden = document.getElementById('transaction-client-id');
        if (clientInput) clientInput.value = '';
        if (clientHidden) clientHidden.value = '';
        // Reload transactions if on transactions tab
        const activeTab = document.querySelector('.tab-btn.active');
        if (activeTab && activeTab.getAttribute('data-tab') === 'transactions') {
            loadTransactions();
        }
    } catch (error) {
        console.error('Error creating transaction:', error);
        showFinanceMessage(error.message || 'Помилка створення транзакції', 'error');
    }
}

function openCreateAccountModal() {
    document.getElementById('account-id').value = '';
    document.getElementById('account-modal-title').textContent = 'Створити рахунок';
    document.getElementById('account-submit-btn').textContent = 'Створити';
    const modal = document.getElementById('create-account-modal');
    populateAccountForm();
    modal.style.display = 'block';
}

function populateAccountForm() {
    const userSelect = document.getElementById('account-user');
    userSelect.innerHTML = '<option value="">Без користувача</option>';
    usersCache.forEach(user => {
        const option = document.createElement('option');
        option.value = user.id;
        option.textContent = user.fullName || user.name;
        userSelect.appendChild(option);
    });

    const branchSelect = document.getElementById('account-branch');
    branchSelect.innerHTML = '<option value="">Без філії</option>';
    branchesCache.forEach(branch => {
        const option = document.createElement('option');
        option.value = branch.id;
        option.textContent = branch.name;
        branchSelect.appendChild(option);
    });
}

async function handleCreateAccount(e) {
    e.preventDefault();
    const accountId = document.getElementById('account-id').value;
    const currencies = Array.from(document.querySelectorAll('input[name="currency"]:checked'))
        .map(cb => cb.value);

    if (currencies.length === 0) {
        showFinanceMessage('Виберіть хоча б одну валюту', 'error');
        return;
    }

    const formData = {
        name: document.getElementById('account-name').value,
        description: document.getElementById('account-description').value,
        userId: document.getElementById('account-user').value || null,
        branchId: document.getElementById('account-branch').value || null,
        currencies: currencies
    };

    try {
        const url = accountId 
            ? `${API_BASE}/accounts/${accountId}`
            : `${API_BASE}/accounts`;
        const method = accountId ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || `Failed to ${accountId ? 'update' : 'create'} account`);
        }
        showFinanceMessage(`Рахунок успішно ${accountId ? 'оновлено' : 'створено'}`, 'success');
        document.getElementById('create-account-modal').style.display = 'none';
        document.getElementById('account-form').reset();
        document.getElementById('account-id').value = '';
        document.getElementById('account-modal-title').textContent = 'Створити рахунок';
        document.getElementById('account-submit-btn').textContent = 'Створити';
        loadAccountsAndBranches();
    } catch (error) {
        console.error(`Error ${accountId ? 'updating' : 'creating'} account:`, error);
        showFinanceMessage(error.message || `Помилка ${accountId ? 'оновлення' : 'створення'} рахунку`, 'error');
    }
}

function openCreateBranchModal() {
    document.getElementById('branch-id').value = '';
    document.getElementById('branch-modal-title').textContent = 'Створити філію';
    document.getElementById('branch-submit-btn').textContent = 'Створити';
    document.getElementById('create-branch-modal').style.display = 'block';
}

async function handleCreateBranch(e) {
    e.preventDefault();
    const branchId = document.getElementById('branch-id').value;
    const formData = {
        name: document.getElementById('branch-name').value,
        description: document.getElementById('branch-description').value
    };

    try {
        const url = branchId 
            ? `${API_BASE}/branches/${branchId}`
            : `${API_BASE}/branches`;
        const method = branchId ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || `Failed to ${branchId ? 'update' : 'create'} branch`);
        }
        showFinanceMessage(`Філію успішно ${branchId ? 'оновлено' : 'створено'}`, 'success');
        document.getElementById('create-branch-modal').style.display = 'none';
        document.getElementById('branch-form').reset();
        document.getElementById('branch-id').value = '';
        document.getElementById('branch-modal-title').textContent = 'Створити філію';
        document.getElementById('branch-submit-btn').textContent = 'Створити';
        loadAccountsAndBranches();
    } catch (error) {
        console.error(`Error ${branchId ? 'updating' : 'creating'} branch:`, error);
        showFinanceMessage(error.message || `Помилка ${branchId ? 'оновлення' : 'створення'} філії`, 'error');
    }
}


// ========== HELPER FUNCTIONS ==========

async function loadUsers() {
    try {
        const response = await fetch(`${API_BASE}/user`);
        if (!response.ok) throw new Error('Failed to load users');
        usersCache = await response.json();
    } catch (error) {
        console.error('Error loading users:', error);
    }
}

async function loadClients() {
    try {
        const response = await fetch('/api/v1/client/search?size=1000');
        if (!response.ok) throw new Error('Failed to load clients');
        const data = await response.json();
        // Extract clients from PageResponse
        clientsCache = data.content || [];
    } catch (error) {
        console.error('Error loading clients:', error);
    }
}

function getUserName(userId) {
    const user = usersCache.find(u => u.id === userId);
    return user ? (user.fullName || user.name) : `User ${userId}`;
}

function getBranchName(branchId) {
    const branch = branchesCache.find(b => b.id === branchId);
    return branch ? branch.name : `Branch ${branchId}`;
}

function formatNumber(value, maxDecimals = 2) {
    if (value === null || value === undefined || value === '') return '0';
    const num = parseFloat(value);
    if (isNaN(num)) return '0';
    return parseFloat(num.toFixed(maxDecimals)).toString();
}

function showFinanceMessage(message, type = 'info') {
    // Try to use common.js showMessage if available
    if (typeof window.showMessage === 'function') {
        window.showMessage(message, type);
    } else {
        alert(message);
    }
}

// Edit/Delete functions
async function editAccount(id) {
    try {
        const response = await fetch(`${API_BASE}/accounts/${id}`);
        if (!response.ok) throw new Error('Failed to load account');
        
        const account = await response.json();
        
        // First populate form options (users and branches)
        populateAccountForm();
        
        // Then set values after options are populated
        document.getElementById('account-id').value = account.id;
        document.getElementById('account-name').value = account.name;
        document.getElementById('account-description').value = account.description || '';
        
        // Set user and branch values (convert to string for select comparison)
        const userIdSelect = document.getElementById('account-user');
        const branchIdSelect = document.getElementById('account-branch');
        
        if (account.userId) {
            userIdSelect.value = String(account.userId);
        } else {
            userIdSelect.value = '';
        }
        
        if (account.branchId) {
            branchIdSelect.value = String(account.branchId);
        } else {
            branchIdSelect.value = '';
        }
        
        // Set currencies
        document.querySelectorAll('input[name="currency"]').forEach(cb => {
            cb.checked = account.currencies && account.currencies.includes(cb.value);
        });
        
        document.getElementById('account-modal-title').textContent = 'Редагувати рахунок';
        document.getElementById('account-submit-btn').textContent = 'Зберегти';
        
        document.getElementById('create-account-modal').style.display = 'block';
    } catch (error) {
        console.error('Error loading account:', error);
        showFinanceMessage('Помилка завантаження рахунку', 'error');
    }
}

async function deleteAccount(id) {
    if (!confirm('Ви впевнені, що хочете видалити цей рахунок? Ця дія незворотна.')) return;
    
    try {
        const response = await fetch(`${API_BASE}/accounts/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to delete account');
        }
        showFinanceMessage('Рахунок успішно видалено', 'success');
        loadAccountsAndBranches();
    } catch (error) {
        console.error('Error deleting account:', error);
        showFinanceMessage(error.message || 'Помилка видалення рахунку', 'error');
    }
}

async function editBranch(id) {
    try {
        const response = await fetch(`${API_BASE}/branches/${id}`);
        if (!response.ok) throw new Error('Failed to load branch');
        
        const branch = await response.json();
        
        document.getElementById('branch-id').value = branch.id;
        document.getElementById('branch-name').value = branch.name;
        document.getElementById('branch-description').value = branch.description || '';
        
        document.getElementById('branch-modal-title').textContent = 'Редагувати філію';
        document.getElementById('branch-submit-btn').textContent = 'Зберегти';
        
        document.getElementById('create-branch-modal').style.display = 'block';
    } catch (error) {
        console.error('Error loading branch:', error);
        showFinanceMessage('Помилка завантаження філії', 'error');
    }
}

async function deleteBranch(id) {
    if (!confirm('Ви впевнені, що хочете видалити цю філію? Ця дія незворотна.')) return;
    
    try {
        const response = await fetch(`${API_BASE}/branches/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to delete branch');
        }
        showFinanceMessage('Філію успішно видалено', 'success');
        loadAccountsAndBranches();
    } catch (error) {
        console.error('Error deleting branch:', error);
        showFinanceMessage(error.message || 'Помилка видалення філії', 'error');
    }
}


