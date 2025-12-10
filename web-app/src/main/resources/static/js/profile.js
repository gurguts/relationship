const API_BASE = '/api/v1';

let productsCache = [];
let userId = null;
let usersCache = [];
let currentSelectedUserId = null;

function getUserAuthorities() {
    const authorities = localStorage.getItem('authorities');
    if (!authorities) {
        return [];
    }
    
    let userAuthorities = [];
    try {
        if (authorities.startsWith('[')) {
            userAuthorities = JSON.parse(authorities);
        } else {
            userAuthorities = authorities.split(',').map(auth => auth.trim().replace(/^["']|["']$/g, ''));
        }
    } catch (error) {
        console.error('Failed to parse authorities:', error);
        return [];
    }
    
    return userAuthorities;
}

function checkCanEditProfile() {
    const userAuthorities = getUserAuthorities();
    
    const hasAdmin = userAuthorities.some(auth => {
        const trimmed = String(auth).trim();
        return trimmed === 'system:admin';
    });
    const hasProfileEdit = userAuthorities.some(auth => {
        const trimmed = String(auth).trim();
        return trimmed === 'profile:edit';
    });
    
    return hasAdmin || hasProfileEdit;
}

function checkCanViewMultipleProfiles() {
    const userAuthorities = getUserAuthorities();
    
    const hasAdmin = userAuthorities.some(auth => {
        const trimmed = String(auth).trim();
        return trimmed === 'system:admin';
    });
    const hasMultipleView = userAuthorities.some(auth => {
        const trimmed = String(auth).trim();
        return trimmed === 'profile:multiple_view';
    });
    
    return hasAdmin || hasMultipleView;
}

document.addEventListener('DOMContentLoaded', async function () {
    userId = localStorage.getItem('userId');
    if (!userId) {
        showError('Користувач не авторизований');
        return;
    }

    currentSelectedUserId = userId;

    const canViewMultiple = checkCanViewMultipleProfiles();
    if (canViewMultiple) {
        await loadUsers();
        initializeUserSelector();
    }

    await loadProducts();
    await loadProductBalances();
    await loadAccounts();
    updateProfileTitle();
});

async function loadUsers() {
    try {
        const response = await fetch(`${API_BASE}/user`);
        if (!response.ok) throw new Error('Failed to load users');
        usersCache = await response.json();
    } catch (error) {
        console.error('Error loading users:', error);
    }
}

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
    const targetUserId = currentSelectedUserId || userId;
    
    try {
        const response = await fetch(`${API_BASE}/driver/balances/${targetUserId}`);
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
    const canEditProfile = checkCanEditProfile();
    
    const table = document.createElement('table');
    table.className = 'product-balance-table';
    
    const thead = document.createElement('thead');
    const headerRow = document.createElement('tr');
    headerRow.innerHTML = `
        <th>Товар</th>
        <th>Кількість (кг)</th>
        <th>Середня ціна (EUR/кг)</th>
        <th>Загальна вартість (EUR)</th>
    `;
    if (canEditProfile) {
        const actionsHeader = document.createElement('th');
        actionsHeader.textContent = 'Дії';
        headerRow.appendChild(actionsHeader);
    }
    thead.appendChild(headerRow);
    
    const tbody = document.createElement('tbody');
    
    balances.forEach(balance => {
        const product = productsCache.find(p => p.id === balance.productId);
        const productName = product ? product.name : `Товар #${balance.productId}`;
        
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${productName}</td>
            <td>${formatNumber(balance.quantity, 2)}</td>
            <td>${formatNumber(balance.averagePriceEur, 6)}</td>
            <td>${formatNumber(balance.totalCostEur, 2)}</td>
        `;
        
        if (canEditProfile) {
            const actionsCell = document.createElement('td');
            const editButton = document.createElement('button');
            editButton.className = 'edit-balance-btn';
            editButton.setAttribute('data-balance-id', balance.id);
            editButton.setAttribute('data-driver-id', balance.driverId);
            editButton.setAttribute('data-product-id', balance.productId);
            editButton.setAttribute('data-current-cost', balance.totalCostEur);
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                const balanceId = editButton.getAttribute('data-balance-id');
                const driverId = editButton.getAttribute('data-driver-id');
                const productId = editButton.getAttribute('data-product-id');
                const currentCost = editButton.getAttribute('data-current-cost');
                openEditBalanceModal(driverId, productId, currentCost, balanceId);
            });
            actionsCell.appendChild(editButton);
            row.appendChild(actionsCell);
        }
        
        tbody.appendChild(row);
    });
    
    table.appendChild(thead);
    table.appendChild(tbody);
    container.innerHTML = '';
    container.appendChild(table);
}

async function loadAccounts() {
    const container = document.getElementById('accounts-container');
    const targetUserId = currentSelectedUserId || userId;
    
    try {
        const response = await fetch(`${API_BASE}/accounts/user/${targetUserId}`);
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

function initializeUserSelector() {
    const selectorContainer = document.getElementById('user-selector-container');
    if (!selectorContainer) return;

    const canViewMultiple = checkCanViewMultipleProfiles();
    if (!canViewMultiple) {
        selectorContainer.style.display = 'none';
        return;
    }

    selectorContainer.style.display = 'flex';

    const select = document.createElement('select');
    select.id = 'user-selector';
    select.className = 'user-selector';

    const defaultOption = document.createElement('option');
    defaultOption.value = userId;
    const currentUser = usersCache.find(u => u.id == userId);
    defaultOption.textContent = currentUser ? (currentUser.fullName || currentUser.name || `Користувач #${userId}`) : 'Мій профіль';
    defaultOption.selected = true;
    select.appendChild(defaultOption);

    usersCache.forEach(user => {
        if (user.id != userId) {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = user.fullName || user.name || `Користувач #${user.id}`;
            select.appendChild(option);
        }
    });

    select.addEventListener('change', async (e) => {
        currentSelectedUserId = e.target.value;
        updateProfileTitle();
        await loadProductBalances();
        await loadAccounts();
    });

    selectorContainer.appendChild(select);
}

function updateProfileTitle() {
    const titleElement = document.querySelector('.profile-container h1');
    if (!titleElement) return;

    if (currentSelectedUserId == userId) {
        titleElement.textContent = 'Мій профіль';
    } else {
        const selectedUser = usersCache.find(u => u.id == currentSelectedUserId);
        const userName = selectedUser ? (selectedUser.fullName || selectedUser.name || `Користувач #${currentSelectedUserId}`) : `Профіль користувача #${currentSelectedUserId}`;
        titleElement.textContent = `Профіль: ${userName}`;
    }
}

function showError(message) {
    const container = document.getElementById('product-balance-container');
    if (container) {
        container.innerHTML = `<div class="error">${message}</div>`;
    }
}

function openEditBalanceModal(driverId, productId, currentCost, balanceId) {
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.id = 'edit-balance-modal';
    modal.style.display = 'flex';
    
    const product = productsCache.find(p => p.id == productId);
    const productName = product ? product.name : `Товар #${productId}`;
    
    modal.innerHTML = `
        <div class="modal-content">
            <span class="close">&times;</span>
            <h2>Редагувати загальну вартість</h2>
            <p><strong>Товар:</strong> ${productName}</p>
            <form id="edit-balance-form">
                <label for="total-cost-input">Загальна вартість (EUR):</label>
                <input type="number" id="total-cost-input" step="0.01" min="0" value="${currentCost}" required>
                <div class="modal-buttons">
                    <button type="submit">Зберегти</button>
                    <button type="button" class="cancel-btn">Відмінити</button>
                </div>
            </form>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    const closeModal = () => {
        modal.remove();
    };
    
    modal.querySelector('.close').addEventListener('click', closeModal);
    modal.querySelector('.cancel-btn').addEventListener('click', closeModal);
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            closeModal();
        }
    });
    
    modal.querySelector('#edit-balance-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const newTotalCost = parseFloat(document.getElementById('total-cost-input').value);
        
        if (isNaN(newTotalCost) || newTotalCost < 0) {
            alert('Введіть коректне значення');
            return;
        }
        
        try {
            const response = await fetch(`${API_BASE}/driver/balances/${driverId}/product/${productId}/total-cost`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ totalCostEur: newTotalCost })
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                alert(errorData.message || 'Помилка оновлення балансу');
                return;
            }
            
            closeModal();
            await loadProductBalances();
            
            if (typeof showMessage === 'function') {
                showMessage('Баланс успішно оновлено', 'info');
            } else {
                alert('Баланс успішно оновлено');
            }
        } catch (error) {
            console.error('Error updating balance:', error);
            alert('Помилка оновлення балансу');
        }
    });
}

