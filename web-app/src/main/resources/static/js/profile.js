const API_BASE = '/api/v1';

let productsCache = [];
let userId = null;

document.addEventListener('DOMContentLoaded', async function () {
    userId = localStorage.getItem('userId');
    if (!userId) {
        showError('Користувач не авторизований');
        return;
    }

    await loadProducts();
    await loadProductBalances();
    await loadAccounts();
});

async function loadProducts() {
    try {
        const response = await fetch(`${API_BASE}/product`);
        if (!response.ok) throw new Error('Failed to load products');
        productsCache = await response.json();
    } catch (error) {
        console.error('Error loading products:', error);
    }
}

async function loadProductBalances() {
    const container = document.getElementById('product-balance-container');
    
    try {
        const response = await fetch(`${API_BASE}/driver/balances/${userId}`);
        if (!response.ok) {
            if (response.status === 404) {
                container.innerHTML = '<div class="empty">Немає балансу товару</div>';
                return;
            }
            if (response.status === 403) {
                container.innerHTML = '<div class="empty">Немає доступу до балансу товару</div>';
                return;
            }
            throw new Error('Failed to load product balances');
        }

        const balances = await response.json();
        
        // Фильтруем только балансы с количеством > 0
        const activeBalances = balances.filter(b => b.quantity && parseFloat(b.quantity) > 0);
        
        if (!activeBalances || activeBalances.length === 0) {
            container.innerHTML = '<div class="empty">Немає балансу товару</div>';
            return;
        }

        renderProductBalances(activeBalances);
    } catch (error) {
        console.error('Error loading product balances:', error);
        container.innerHTML = '<div class="error">Помилка завантаження балансу товару</div>';
    }
}

function renderProductBalances(balances) {
    const container = document.getElementById('product-balance-container');
    
    const table = document.createElement('table');
    table.className = 'product-balance-table';
    
    const thead = document.createElement('thead');
    thead.innerHTML = `
        <tr>
            <th>Товар</th>
            <th>Кількість (кг)</th>
            <th>Середня ціна (UAH/кг)</th>
            <th>Загальна вартість (UAH)</th>
        </tr>
    `;
    
    const tbody = document.createElement('tbody');
    
    balances.forEach(balance => {
        const product = productsCache.find(p => p.id === balance.productId);
        const productName = product ? product.name : `Товар #${balance.productId}`;
        
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${productName}</td>
            <td>${formatNumber(balance.quantity, 2)}</td>
            <td>${formatNumber(balance.averagePriceUah, 6)}</td>
            <td>${formatNumber(balance.totalCostUah, 2)}</td>
        `;
        tbody.appendChild(row);
    });
    
    table.appendChild(thead);
    table.appendChild(tbody);
    container.innerHTML = '';
    container.appendChild(table);
}

async function loadAccounts() {
    const container = document.getElementById('accounts-container');
    
    try {
        const response = await fetch(`${API_BASE}/accounts/user/${userId}`);
        if (!response.ok) {
            if (response.status === 404) {
                container.innerHTML = '<div class="empty">Немає фінансових рахунків</div>';
                return;
            }
            if (response.status === 403) {
                container.innerHTML = '<div class="empty">Немає доступу до фінансових рахунків</div>';
                return;
            }
            throw new Error('Failed to load accounts');
        }

        const accounts = await response.json();
        
        if (!accounts || accounts.length === 0) {
            container.innerHTML = '<div class="empty">Немає фінансових рахунків</div>';
            return;
        }

        await renderAccounts(accounts);
    } catch (error) {
        console.error('Error loading accounts:', error);
        container.innerHTML = '<div class="error">Помилка завантаження рахунків</div>';
    }
}

async function renderAccounts(accounts) {
    const container = document.getElementById('accounts-container');
    container.innerHTML = '';
    
    for (const account of accounts) {
        const accountCard = await createAccountCard(account);
        container.appendChild(accountCard);
    }
}

async function createAccountCard(account) {
    const card = document.createElement('div');
    card.className = 'account-card';
    
    // Load balances for this account
    let balancesHtml = '<div class="account-balances-empty">Немає балансів</div>';
    
    try {
        const balancesResponse = await fetch(`${API_BASE}/accounts/${account.id}/balances`);
        if (balancesResponse.ok) {
            const balances = await balancesResponse.json();
            if (balances && balances.length > 0) {
                balancesHtml = balances.map(balance => 
                    `<div class="balance-item">
                        <span class="balance-currency">${balance.currency}</span>
                        <span class="balance-amount">${formatNumber(balance.amount, 2)}</span>
                    </div>`
                ).join('');
            }
        }
    } catch (error) {
        console.warn(`Failed to load balances for account ${account.id}`, error);
    }
    
    card.innerHTML = `
        <div class="account-card-header">
            <span class="account-name">${account.name || 'Без назви'}</span>
        </div>
        ${account.description ? `<div class="account-description">${account.description}</div>` : ''}
        <div class="account-balances">
            ${balancesHtml}
        </div>
    `;
    
    return card;
}

function formatNumber(number, decimals = 2) {
    if (number == null || number === undefined) return '0';
    return parseFloat(number).toFixed(decimals);
}

function showError(message) {
    const container = document.getElementById('product-balance-container');
    if (container) {
        container.innerHTML = `<div class="error">${message}</div>`;
    }
}

