const API_BASE = '/api/v1';
let categoriesCache = [];
let branchesCache = [];
let accountsCache = [];
let usersCache = [];

const transactionTypeMap = {
    'INTERNAL_TRANSFER': 'Переказ між рахунками',
    'EXTERNAL_INCOME': 'Зовнішній прихід',
    'EXTERNAL_EXPENSE': 'Зовнішня витрата',
    'CLIENT_PAYMENT': 'Оплата клієнту',
    'CURRENCY_CONVERSION': 'Конвертація валют',
    'PURCHASE': 'Закупівля',
    'VEHICLE_EXPENSE': 'Витрати на машину'
};

document.addEventListener('DOMContentLoaded', () => {
    initializeTabs();
    initializeModals();
    setupEventListeners();
    loadCategories();
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
            if (targetTab === 'finance-categories') {
                loadCategories();
            } else if (targetTab === 'counterparties') {
                loadCounterparties();
            } else if (targetTab === 'branches') {
                loadBranches();
            } else if (targetTab === 'accounts') {
                loadAccounts();
            } else if (targetTab === 'vehicle-senders') {
                loadVehicleSenders();
            } else if (targetTab === 'vehicle-receivers') {
                loadVehicleReceivers();
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
    document.getElementById('create-category-btn').addEventListener('click', () => {
        openCreateCategoryModal();
    });

    document.getElementById('category-form').addEventListener('submit', handleCreateCategory);

    document.getElementById('category-type-filter').addEventListener('change', loadCategories);

    document.getElementById('create-counterparty-btn').addEventListener('click', () => {
        openCreateCounterpartyModal();
    });

    document.getElementById('counterparty-form').addEventListener('submit', handleCreateCounterparty);

    document.getElementById('counterparty-type-filter').addEventListener('change', loadCounterparties);

    document.getElementById('create-branch-btn').addEventListener('click', () => {
        openCreateBranchModal();
    });
    document.getElementById('create-account-btn').addEventListener('click', () => {
        openCreateAccountModal();
    });

    document.getElementById('field-form').addEventListener('submit', handleCreateField);
    
    document.getElementById('branch-form').addEventListener('submit', handleCreateBranch);
    document.getElementById('account-form').addEventListener('submit', handleCreateAccount);

    document.getElementById('create-vehicle-sender-btn').addEventListener('click', () => {
        openCreateVehicleSenderModal();
    });
    document.getElementById('vehicle-sender-form').addEventListener('submit', handleCreateVehicleSender);

    document.getElementById('create-vehicle-receiver-btn').addEventListener('click', () => {
        openCreateVehicleReceiverModal();
    });
    document.getElementById('vehicle-receiver-form').addEventListener('submit', handleCreateVehicleReceiver);
}

// ========== COUNTERPARTIES ==========

async function loadCounterparties() {
    try {
        const typeFilter = document.getElementById('counterparty-type-filter').value;
        const tbody = document.getElementById('counterparties-body');
        if (!tbody) return;
        
        tbody.textContent = '';
        
        const types = typeFilter ? [typeFilter] : ['INCOME', 'EXPENSE'];
        const allCounterparties = [];
        
        for (const type of types) {
            const response = await fetch(`${API_BASE}/counterparties/type/${type}`);
            if (response.ok) {
                const counterparties = await response.json();
                allCounterparties.push(...counterparties);
            }
        }
        
        if (allCounterparties.length === 0) {
            const row = document.createElement('tr');
            row.innerHTML = '<td colspan="4" style="text-align: center;">Контрагентів не знайдено</td>';
            tbody.appendChild(row);
            return;
        }
        
        allCounterparties.forEach(cp => {
            const row = document.createElement('tr');
            const typeText = cp.type === 'INCOME' ? 'Для доходів' : 'Для витрат';
            row.innerHTML = `
                <td>${escapeHtml(typeText)}</td>
                <td>${escapeHtml(cp.name || '')}</td>
                <td>${escapeHtml(cp.description || '')}</td>
                <td>
                    <button onclick="editCounterparty(${cp.id})" class="btn-edit">Редагувати</button>
                    <button onclick="deleteCounterparty(${cp.id})" class="btn-delete">Видалити</button>
                </td>
            `;
            tbody.appendChild(row);
        });
    } catch (error) {
        console.error('Error loading counterparties:', error);
        showSettingsMessage('Помилка завантаження контрагентів', 'error');
    }
}

function openCreateCounterpartyModal() {
    document.getElementById('counterparty-id').value = '';
    document.getElementById('counterparty-type').value = '';
    document.getElementById('counterparty-name').value = '';
    document.getElementById('counterparty-description').value = '';
    document.getElementById('counterparty-modal-title').textContent = 'Створити контрагента';
    document.getElementById('counterparty-submit-btn').textContent = 'Створити';
    document.getElementById('create-counterparty-modal').style.display = 'block';
}

async function handleCreateCounterparty(e) {
    e.preventDefault();
    const id = document.getElementById('counterparty-id').value;
    const type = document.getElementById('counterparty-type').value;
    const name = document.getElementById('counterparty-name').value.trim();
    const description = document.getElementById('counterparty-description').value.trim();
    
    if (!type || !name) {
        showSettingsMessage('Заповніть всі обов\'язкові поля', 'error');
        return;
    }
    
    try {
        const data = { type, name, description };
        const url = `${API_BASE}/counterparties${id ? `/${id}` : ''}`;
        const method = id ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to save counterparty');
        }
        
        showSettingsMessage(`Контрагента ${id ? 'оновлено' : 'створено'} успішно`, 'success');
        document.getElementById('create-counterparty-modal').style.display = 'none';
        loadCounterparties();
    } catch (error) {
        console.error('Error saving counterparty:', error);
        showSettingsMessage(error.message || 'Помилка збереження контрагента', 'error');
    }
}

async function editCounterparty(id) {
    try {
        const response = await fetch(`${API_BASE}/counterparties/${id}`);
        if (!response.ok) throw new Error('Failed to load counterparty');
        
        const cp = await response.json();
        
        document.getElementById('counterparty-id').value = cp.id;
        document.getElementById('counterparty-type').value = cp.type;
        document.getElementById('counterparty-name').value = cp.name || '';
        document.getElementById('counterparty-description').value = cp.description || '';
        document.getElementById('counterparty-modal-title').textContent = 'Редагувати контрагента';
        document.getElementById('counterparty-submit-btn').textContent = 'Зберегти';
        
        document.getElementById('create-counterparty-modal').style.display = 'block';
    } catch (error) {
        console.error('Error loading counterparty:', error);
        showSettingsMessage('Помилка завантаження контрагента', 'error');
    }
}

async function deleteCounterparty(id) {
    if (!confirm('Ви впевнені, що хочете видалити цього контрагента? Ця дія незворотна.')) return;
    
    try {
        const response = await fetch(`${API_BASE}/counterparties/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to delete counterparty');
        }
        showSettingsMessage('Контрагента успішно видалено', 'success');
        loadCounterparties();
    } catch (error) {
        console.error('Error deleting counterparty:', error);
        showSettingsMessage(error.message || 'Помилка видалення контрагента', 'error');
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ========== CATEGORIES ==========

async function loadCategories() {
    try {
        const typeFilter = document.getElementById('category-type-filter').value;
        let categories = [];

        if (typeFilter) {
            const response = await fetch(`${API_BASE}/transaction-categories/type/${typeFilter}`);
            if (!response.ok) throw new Error('Failed to load categories');
            categories = await response.json();
        } else {
            // Load all categories
            const types = ['INTERNAL_TRANSFER', 'EXTERNAL_INCOME', 'EXTERNAL_EXPENSE', 'CLIENT_PAYMENT', 'CURRENCY_CONVERSION', 'VEHICLE_EXPENSE'];
            const promises = types.map(type => 
                fetch(`${API_BASE}/transaction-categories/type/${type}`)
                    .then(r => r.ok ? r.json() : [])
            );
            const results = await Promise.all(promises);
            categories = results.flat();
        }

        categoriesCache = categories;
        renderCategories(categories);
    } catch (error) {
        console.error('Error loading categories:', error);
        showSettingsMessage('Помилка завантаження категорій', 'error');
    }
}

function renderCategories(categories) {
    const tbody = document.getElementById('categories-body');
    tbody.innerHTML = '';

    categories.forEach(category => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${transactionTypeMap[category.type] || category.type}</td>
            <td>${category.name}</td>
            <td>${category.description || '-'}</td>
            <td><span class="status-badge ${category.isActive ? 'status-active' : 'status-inactive'}">${category.isActive ? 'Активна' : 'Неактивна'}</span></td>
            <td>
                <div class="action-buttons-table">
                    <button class="action-btn btn-edit" onclick="editCategory(${category.id})">Редагувати</button>
                    <button class="action-btn btn-delete" onclick="deleteCategory(${category.id})">Видалити</button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function openCreateCategoryModal() {
    document.getElementById('category-id').value = '';
    document.getElementById('category-modal-title').textContent = 'Створити категорію';
    document.getElementById('category-submit-btn').textContent = 'Створити';
    document.getElementById('category-active-group').style.display = 'none';
    document.getElementById('category-type').disabled = false;
    document.getElementById('create-category-modal').style.display = 'block';
}

async function handleCreateCategory(e) {
    e.preventDefault();
    const categoryId = document.getElementById('category-id').value;
    const formData = {
        name: document.getElementById('category-name').value,
        description: document.getElementById('category-description').value
    };

    // Only include type for new categories
    if (!categoryId) {
        formData.type = document.getElementById('category-type').value;
    } else {
        // For editing, include isActive
        formData.isActive = document.getElementById('category-active').checked;
    }

    try {
        const url = categoryId 
            ? `${API_BASE}/transaction-categories/${categoryId}`
            : `${API_BASE}/transaction-categories`;
        const method = categoryId ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || `Failed to ${categoryId ? 'update' : 'create'} category`);
        }
        showSettingsMessage(`Категорію успішно ${categoryId ? 'оновлено' : 'створено'}`, 'success');
        document.getElementById('create-category-modal').style.display = 'none';
        document.getElementById('category-form').reset();
        document.getElementById('category-id').value = '';
        document.getElementById('category-modal-title').textContent = 'Створити категорію';
        document.getElementById('category-submit-btn').textContent = 'Створити';
        document.getElementById('category-active-group').style.display = 'none';
        document.getElementById('category-type').disabled = false;
        loadCategories();
    } catch (error) {
        console.error(`Error ${categoryId ? 'updating' : 'creating'} category:`, error);
        showSettingsMessage(error.message || `Помилка ${categoryId ? 'оновлення' : 'створення'} категорії`, 'error');
    }
}

async function editCategory(id) {
    try {
        const response = await fetch(`${API_BASE}/transaction-categories/${id}`);
        if (!response.ok) throw new Error('Failed to load category');
        
        const category = await response.json();
        
        const categoryIdEl = document.getElementById('category-id');
        const categoryTypeEl = document.getElementById('category-type');
        const categoryNameEl = document.getElementById('category-name');
        const categoryDescEl = document.getElementById('category-description');
        const categoryActiveEl = document.getElementById('category-active');
        const categoryModalTitleEl = document.getElementById('category-modal-title');
        const categorySubmitBtnEl = document.getElementById('category-submit-btn');
        const categoryActiveGroupEl = document.getElementById('category-active-group');
        const categoryModalEl = document.getElementById('create-category-modal');
        
        if (!categoryIdEl || !categoryTypeEl || !categoryNameEl || !categoryDescEl || 
            !categoryActiveEl || !categoryModalTitleEl || !categorySubmitBtnEl || 
            !categoryActiveGroupEl || !categoryModalEl) {
            throw new Error('Required form elements not found');
        }
        
        categoryIdEl.value = category.id;
        categoryTypeEl.value = category.type;
        categoryTypeEl.disabled = true; // Don't allow changing type
        categoryNameEl.value = category.name;
        categoryDescEl.value = category.description || '';
        categoryActiveEl.checked = category.isActive;
        
        categoryModalTitleEl.textContent = 'Редагувати категорію';
        categorySubmitBtnEl.textContent = 'Зберегти';
        categoryActiveGroupEl.style.display = 'block';
        
        categoryModalEl.style.display = 'block';
    } catch (error) {
        console.error('Error loading category:', error);
        showSettingsMessage('Помилка завантаження категорії', 'error');
    }
}

async function deleteCategory(id) {
    if (!confirm('Ви впевнені, що хочете видалити цю категорію? Ця дія незворотна.')) return;
    
    try {
        const response = await fetch(`${API_BASE}/transaction-categories/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to delete category');
        }
        showSettingsMessage('Категорію успішно видалено', 'success');
        loadCategories();
    } catch (error) {
        console.error('Error deleting category:', error);
        showSettingsMessage(error.message || 'Помилка видалення категорії', 'error');
    }
}

// ========== FIELDS (Business, Region, Route, Status) ==========

const fieldConfig = {};

async function loadField(fieldType) {
    try {
        const config = fieldConfig[fieldType];
        const response = await fetch(`${API_BASE}/${config.endpoint}`);
        if (!response.ok) throw new Error(`Failed to load ${config.namePlural}`);
        
        const data = await response.json();
        config.cache = data;
        renderField(fieldType, data);
    } catch (error) {
        console.error(`Error loading ${fieldConfig[fieldType].namePlural}:`, error);
        showSettingsMessage(`Помилка завантаження ${fieldConfig[fieldType].namePlural}`, 'error');
    }
}

function renderField(fieldType, items) {
    const config = fieldConfig[fieldType];
    const tbody = document.getElementById(`${fieldType}-body`);
    if (!tbody) return;
    
    tbody.innerHTML = '';

    items.forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${item.name}</td>
            <td>
                <div class="action-buttons-table">
                    <button class="action-btn btn-edit" onclick="editField('${fieldType}', ${item.id})">Редагувати</button>
                    <button class="action-btn btn-delete" onclick="deleteField('${fieldType}', ${item.id})">Видалити</button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

async function openCreateFieldModal(fieldType, fieldName) {
    const config = fieldConfig[fieldType];
    document.getElementById('field-id').value = '';
    document.getElementById('field-type').value = fieldType;
    document.getElementById('field-modal-title').textContent = `Створити ${fieldName}`;
    document.getElementById('field-submit-btn').textContent = 'Створити';
    document.getElementById('field-name').value = '';
    
    document.getElementById('create-field-modal').style.display = 'block';
}

async function handleCreateField(e) {
    e.preventDefault();
    const fieldType = document.getElementById('field-type').value;
    const fieldId = document.getElementById('field-id').value;
    const config = fieldConfig[fieldType];
    
    const formData = {
        name: document.getElementById('field-name').value
    };

    try {
        const url = fieldId 
            ? `${API_BASE}/${config.endpoint}/${fieldId}`
            : `${API_BASE}/${config.endpoint}`;
        const method = fieldId ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || `Failed to ${fieldId ? 'update' : 'create'} ${config.name}`);
        }
        showSettingsMessage(`${config.name.charAt(0).toUpperCase() + config.name.slice(1)} успішно ${fieldId ? 'оновлено' : 'створено'}`, 'success');
        document.getElementById('create-field-modal').style.display = 'none';
        document.getElementById('field-form').reset();
        document.getElementById('field-id').value = '';
        document.getElementById('field-type').value = '';
        
        // Reload the field list
        await loadField(fieldType);
    } catch (error) {
        console.error(`Error ${fieldId ? 'updating' : 'creating'} ${config.name}:`, error);
        showSettingsMessage(error.message || `Помилка ${fieldId ? 'оновлення' : 'створення'} ${config.name}`, 'error');
    }
}

async function editField(fieldType, id) {
    try {
        const config = fieldConfig[fieldType];
        const response = await fetch(`${API_BASE}/${config.endpoint}/${id}`);
        if (!response.ok) throw new Error(`Failed to load ${config.name}`);
        
        const item = await response.json();
        
        document.getElementById('field-id').value = item.id;
        document.getElementById('field-type').value = fieldType;
        document.getElementById('field-name').value = item.name;
        document.getElementById('field-modal-title').textContent = `Редагувати ${config.name}`;
        document.getElementById('field-submit-btn').textContent = 'Зберегти';
        
        document.getElementById('create-field-modal').style.display = 'block';
    } catch (error) {
        console.error(`Error loading ${fieldConfig[fieldType].name}:`, error);
        showSettingsMessage(`Помилка завантаження ${fieldConfig[fieldType].name}`, 'error');
    }
}

async function deleteField(fieldType, id) {
    const config = fieldConfig[fieldType];
    if (!confirm(`Ви впевнені, що хочете видалити цей ${config.name}? Ця дія незворотна.`)) return;
    
    try {
        const response = await fetch(`${API_BASE}/${config.endpoint}/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || `Failed to delete ${config.name}`);
        }
        showSettingsMessage(`${config.name.charAt(0).toUpperCase() + config.name.slice(1)} успішно видалено`, 'success');
        await loadField(fieldType);
    } catch (error) {
        console.error(`Error deleting ${config.name}:`, error);
        showSettingsMessage(error.message || `Помилка видалення ${config.name}`, 'error');
    }
}

// ========== HELPER FUNCTIONS ==========

// ========== BRANCHES ==========

async function loadBranches() {
    try {
        await loadUsers();
        await loadBranchesList();
        renderBranches(branchesCache);
    } catch (error) {
        console.error('Error loading branches:', error);
        showSettingsMessage('Помилка завантаження філій', 'error');
    }
}

async function loadBranchesList() {
    try {
        const response = await fetch(`${API_BASE}/branches`);
        if (!response.ok) throw new Error('Failed to load branches');
        branchesCache = await response.json();
    } catch (error) {
        console.error('Error loading branches list:', error);
        throw error;
    }
}

function renderBranches(branches) {
    const tbody = document.getElementById('branches-body');
    if (!tbody) return;
    
    tbody.innerHTML = '';

    branches.forEach(branch => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${branch.name}</td>
            <td>${branch.description || '-'}</td>
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

function openCreateBranchModal() {
    document.getElementById('branch-id').value = '';
    document.getElementById('branch-modal-title').textContent = 'Створити філію';
    document.getElementById('branch-submit-btn').textContent = 'Створити';
    document.getElementById('branch-form').reset();
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
        showSettingsMessage(`Філію успішно ${branchId ? 'оновлено' : 'створено'}`, 'success');
        document.getElementById('create-branch-modal').style.display = 'none';
        document.getElementById('branch-form').reset();
        document.getElementById('branch-id').value = '';
        document.getElementById('branch-modal-title').textContent = 'Створити філію';
        document.getElementById('branch-submit-btn').textContent = 'Створити';
        loadBranches();
    } catch (error) {
        console.error(`Error ${branchId ? 'updating' : 'creating'} branch:`, error);
        showSettingsMessage(error.message || `Помилка ${branchId ? 'оновлення' : 'створення'} філії`, 'error');
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
        showSettingsMessage('Помилка завантаження філії', 'error');
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
        showSettingsMessage('Філію успішно видалено', 'success');
        loadBranches();
    } catch (error) {
        console.error('Error deleting branch:', error);
        showSettingsMessage(error.message || 'Помилка видалення філії', 'error');
    }
}

// ========== ACCOUNTS ==========

async function loadAccounts() {
    try {
        await Promise.all([loadUsers(), loadBranchesList()]);
        await loadAccountsList();
        renderAccounts(accountsCache);
    } catch (error) {
        console.error('Error loading accounts:', error);
        showSettingsMessage('Помилка завантаження рахунків', 'error');
    }
}

async function loadAccountsList() {
    try {
        const response = await fetch(`${API_BASE}/accounts`);
        if (!response.ok) throw new Error('Failed to load accounts');
        accountsCache = await response.json();
    } catch (error) {
        console.error('Error loading accounts list:', error);
        throw error;
    }
}

function renderAccounts(accounts) {
    const tbody = document.getElementById('accounts-body');
    if (!tbody) return;
    
    tbody.innerHTML = '';

    accounts.forEach(account => {
        const userName = account.userId 
            ? (usersCache.find(u => u.id === account.userId)?.fullName || usersCache.find(u => u.id === account.userId)?.name || `User ${account.userId}`)
            : '-';
        const branchName = account.branchId 
            ? (branchesCache.find(b => b.id === account.branchId)?.name || `Branch ${account.branchId}`)
            : '-';
        const currencies = account.currencies && account.currencies.length > 0 
            ? account.currencies.join(', ')
            : '-';
        
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${account.name}</td>
            <td>${account.description || '-'}</td>
            <td>${userName}</td>
            <td>${branchName}</td>
            <td>${currencies}</td>
            <td>
                <div class="action-buttons-table">
                    <button class="action-btn btn-edit" onclick="editAccount(${account.id})">Редагувати</button>
                    <button class="action-btn btn-delete" onclick="deleteAccount(${account.id})">Видалити</button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function openCreateAccountModal() {
    document.getElementById('account-id').value = '';
    document.getElementById('account-modal-title').textContent = 'Створити рахунок';
    document.getElementById('account-submit-btn').textContent = 'Створити';
    populateAccountForm();
    document.getElementById('account-form').reset();
    // Reset checkboxes to default (UAH checked)
    document.querySelectorAll('input[name="currency"]').forEach(cb => {
        cb.checked = cb.value === 'UAH';
    });
    document.getElementById('create-account-modal').style.display = 'block';
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
        showSettingsMessage('Виберіть хоча б одну валюту', 'error');
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
        showSettingsMessage(`Рахунок успішно ${accountId ? 'оновлено' : 'створено'}`, 'success');
        document.getElementById('create-account-modal').style.display = 'none';
        document.getElementById('account-form').reset();
        document.getElementById('account-id').value = '';
        document.getElementById('account-modal-title').textContent = 'Створити рахунок';
        document.getElementById('account-submit-btn').textContent = 'Створити';
        loadAccounts();
    } catch (error) {
        console.error(`Error ${accountId ? 'updating' : 'creating'} account:`, error);
        showSettingsMessage(error.message || `Помилка ${accountId ? 'оновлення' : 'створення'} рахунку`, 'error');
    }
}

async function editAccount(id) {
    try {
        const response = await fetch(`${API_BASE}/accounts/${id}`);
        if (!response.ok) throw new Error('Failed to load account');
        
        const account = await response.json();
        
        await Promise.all([loadUsers(), loadBranchesList()]);
        populateAccountForm();
        
        document.getElementById('account-id').value = account.id;
        document.getElementById('account-name').value = account.name;
        document.getElementById('account-description').value = account.description || '';
        document.getElementById('account-user').value = account.userId || '';
        document.getElementById('account-branch').value = account.branchId || '';
        
        // Set currency checkboxes
        document.querySelectorAll('input[name="currency"]').forEach(cb => {
            cb.checked = account.currencies && account.currencies.includes(cb.value);
        });
        
        document.getElementById('account-modal-title').textContent = 'Редагувати рахунок';
        document.getElementById('account-submit-btn').textContent = 'Зберегти';
        
        document.getElementById('create-account-modal').style.display = 'block';
    } catch (error) {
        console.error('Error loading account:', error);
        showSettingsMessage('Помилка завантаження рахунку', 'error');
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
        showSettingsMessage('Рахунок успішно видалено', 'success');
        loadAccounts();
    } catch (error) {
        console.error('Error deleting account:', error);
        showSettingsMessage(error.message || 'Помилка видалення рахунку', 'error');
    }
}

async function loadUsers() {
    try {
        const response = await fetch(`${API_BASE}/user`);
        if (!response.ok) throw new Error('Failed to load users');
        usersCache = await response.json();
    } catch (error) {
        console.error('Error loading users:', error);
    }
}

// ========== VEHICLE SENDERS ==========

async function loadVehicleSenders() {
    try {
        const response = await fetch(`${API_BASE}/vehicle-senders`);
        if (!response.ok) throw new Error('Failed to load vehicle senders');
        const senders = await response.json();

        const tbody = document.getElementById('vehicle-senders-body');
        if (!tbody) return;
        tbody.textContent = '';

        if (senders.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 2;
            cell.textContent = 'Відправників не знайдено.';
            cell.style.textAlign = 'center';
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }

        senders.forEach(sender => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td data-label="Назва">${escapeHtml(sender.name)}</td>
                <td data-label="Дії">
                    <div class="action-buttons-table">
                        <button onclick="editVehicleSender(${sender.id})" class="btn-edit">Редагувати</button>
                        <button onclick="deleteVehicleSender(${sender.id})" class="btn-delete">Видалити</button>
                    </div>
                </td>
            `;
            tbody.appendChild(row);
        });
    } catch (error) {
        console.error('Error loading vehicle senders:', error);
        showSettingsMessage(error.message || 'Помилка завантаження відправників', 'error');
    }
}

function openCreateVehicleSenderModal() {
    document.getElementById('vehicle-sender-id').value = '';
    document.getElementById('vehicle-sender-name').value = '';
    document.getElementById('vehicle-sender-modal-title').textContent = 'Створити відправника';
    document.getElementById('vehicle-sender-submit-btn').textContent = 'Створити';
    document.getElementById('create-vehicle-sender-modal').style.display = 'block';
}

async function handleCreateVehicleSender(e) {
    e.preventDefault();
    const id = document.getElementById('vehicle-sender-id').value;
    const name = document.getElementById('vehicle-sender-name').value.trim();

    if (!name) {
        showSettingsMessage('Назва відправника не може бути порожньою', 'error');
        return;
    }

    const method = id ? 'PUT' : 'POST';
    const url = id ? `${API_BASE}/vehicle-senders/${id}` : `${API_BASE}/vehicle-senders`;

    try {
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to save vehicle sender');
        }
        showSettingsMessage(`Відправника успішно ${id ? 'оновлено' : 'створено'}`, 'success');
        document.getElementById('create-vehicle-sender-modal').style.display = 'none';
        loadVehicleSenders();
    } catch (error) {
        console.error('Error saving vehicle sender:', error);
        showSettingsMessage(error.message || `Помилка ${id ? 'оновлення' : 'створення'} відправника`, 'error');
    }
}

async function editVehicleSender(id) {
    try {
        const response = await fetch(`${API_BASE}/vehicle-senders/${id}`);
        if (!response.ok) throw new Error('Failed to load vehicle sender');
        const sender = await response.json();

        document.getElementById('vehicle-sender-id').value = sender.id;
        document.getElementById('vehicle-sender-name').value = sender.name || '';
        document.getElementById('vehicle-sender-modal-title').textContent = 'Редагувати відправника';
        document.getElementById('vehicle-sender-submit-btn').textContent = 'Зберегти';
        document.getElementById('create-vehicle-sender-modal').style.display = 'block';
    } catch (error) {
        console.error('Error loading vehicle sender:', error);
        showSettingsMessage('Помилка завантаження відправника', 'error');
    }
}

async function deleteVehicleSender(id) {
    if (!confirm('Ви впевнені, що хочете видалити цього відправника? Ця дія незворотна.')) return;
    try {
        const response = await fetch(`${API_BASE}/vehicle-senders/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to delete vehicle sender');
        }
        showSettingsMessage('Відправника успішно видалено', 'success');
        loadVehicleSenders();
    } catch (error) {
        console.error('Error deleting vehicle sender:', error);
        showSettingsMessage(error.message || 'Помилка видалення відправника', 'error');
    }
}

// ========== VEHICLE RECEIVERS ==========

async function loadVehicleReceivers() {
    try {
        const response = await fetch(`${API_BASE}/vehicle-receivers`);
        if (!response.ok) throw new Error('Failed to load vehicle receivers');
        const receivers = await response.json();

        const tbody = document.getElementById('vehicle-receivers-body');
        if (!tbody) return;
        tbody.textContent = '';

        if (receivers.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 2;
            cell.textContent = 'Отримувачів не знайдено.';
            cell.style.textAlign = 'center';
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }

        receivers.forEach(receiver => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td data-label="Назва">${escapeHtml(receiver.name)}</td>
                <td data-label="Дії">
                    <div class="action-buttons-table">
                        <button onclick="editVehicleReceiver(${receiver.id})" class="btn-edit">Редагувати</button>
                        <button onclick="deleteVehicleReceiver(${receiver.id})" class="btn-delete">Видалити</button>
                    </div>
                </td>
            `;
            tbody.appendChild(row);
        });
    } catch (error) {
        console.error('Error loading vehicle receivers:', error);
        showSettingsMessage(error.message || 'Помилка завантаження отримувачів', 'error');
    }
}

function openCreateVehicleReceiverModal() {
    document.getElementById('vehicle-receiver-id').value = '';
    document.getElementById('vehicle-receiver-name').value = '';
    document.getElementById('vehicle-receiver-modal-title').textContent = 'Створити отримувача';
    document.getElementById('vehicle-receiver-submit-btn').textContent = 'Створити';
    document.getElementById('create-vehicle-receiver-modal').style.display = 'block';
}

async function handleCreateVehicleReceiver(e) {
    e.preventDefault();
    const id = document.getElementById('vehicle-receiver-id').value;
    const name = document.getElementById('vehicle-receiver-name').value.trim();

    if (!name) {
        showSettingsMessage('Назва отримувача не може бути порожньою', 'error');
        return;
    }

    const method = id ? 'PUT' : 'POST';
    const url = id ? `${API_BASE}/vehicle-receivers/${id}` : `${API_BASE}/vehicle-receivers`;

    try {
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to save vehicle receiver');
        }
        showSettingsMessage(`Отримувача успішно ${id ? 'оновлено' : 'створено'}`, 'success');
        document.getElementById('create-vehicle-receiver-modal').style.display = 'none';
        loadVehicleReceivers();
    } catch (error) {
        console.error('Error saving vehicle receiver:', error);
        showSettingsMessage(error.message || `Помилка ${id ? 'оновлення' : 'створення'} отримувача`, 'error');
    }
}

async function editVehicleReceiver(id) {
    try {
        const response = await fetch(`${API_BASE}/vehicle-receivers/${id}`);
        if (!response.ok) throw new Error('Failed to load vehicle receiver');
        const receiver = await response.json();

        document.getElementById('vehicle-receiver-id').value = receiver.id;
        document.getElementById('vehicle-receiver-name').value = receiver.name || '';
        document.getElementById('vehicle-receiver-modal-title').textContent = 'Редагувати отримувача';
        document.getElementById('vehicle-receiver-submit-btn').textContent = 'Зберегти';
        document.getElementById('create-vehicle-receiver-modal').style.display = 'block';
    } catch (error) {
        console.error('Error loading vehicle receiver:', error);
        showSettingsMessage('Помилка завантаження отримувача', 'error');
    }
}

async function deleteVehicleReceiver(id) {
    if (!confirm('Ви впевнені, що хочете видалити цього отримувача? Ця дія незворотна.')) return;
    try {
        const response = await fetch(`${API_BASE}/vehicle-receivers/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to delete vehicle receiver');
        }
        showSettingsMessage('Отримувача успішно видалено', 'success');
        loadVehicleReceivers();
    } catch (error) {
        console.error('Error deleting vehicle receiver:', error);
        showSettingsMessage(error.message || 'Помилка видалення отримувача', 'error');
    }
}

// ========== HELPER FUNCTIONS ==========

function showSettingsMessage(message, type = 'info') {
    // Try to use common.js showMessage if available
    if (typeof window.showMessage === 'function') {
        window.showMessage(message, type);
    } else {
        alert(message);
    }
}

