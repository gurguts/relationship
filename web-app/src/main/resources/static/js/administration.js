const loaderBackdrop = document.getElementById('loader-backdrop');
const roles = {
    'manager': 'Менеджер',
    'driver': 'Водій',
    'storekeeper': 'Комірник',
    'leader': 'Керівник',
    'accountant': 'Бухгалтер',
    'declarant': 'Декларант'
};

const allPermissions = [
    'system:admin',
    'user:create',
    'user:edit',
    'user:delete',
    'profile:edit',
    'profile:multiple_view',
    'client:view',
    'client:create',
    'client:edit',
    'client_stranger:edit',
    'client:delete',
    'client:full_delete',
    'client:excel',
    'sale:view',
    'sale:create',
    'sale:edit',
    'sale:edit_strangers',
    'sale:delete',
    'sale:edit_source',
    'sale:excel',
    'purchase:view',
    'purchase:create',
    'purchase:edit',
    'purchase:edit_strangers',
    'purchase:delete',
    'purchase:edit_source',
    'purchase:excel',
    'container:view',
    'container:transfer',
    'container:balance',
    'container:excel',
    'finance:view',
    'finance:balance_edit',
    'finance:transfer_view',
    'finance:transfer_excel',
    'transaction:delete',
    'warehouse:view',
    'warehouse:create',
    'warehouse:edit',
    'warehouse:withdraw',
    'warehouse:delete',
    'warehouse:excel',
    'inventory:view',
    'inventory:manage',
    'declarant:view',
    'declarant:create',
    'declarant:edit',
    'declarant:delete',
    'declarant:excel',
    'analytics:view',
    'settings:view',
    'settings_client:create',
    'settings_client:edit',
    'settings_client:delete',
    'settings_finance:create',
    'settings_finance:edit',
    'settings_exchange:edit',
    'settings_finance:delete',
    'settings_product:create',
    'settings_product:delete',
    'settings_declarant:create',
    'settings_declarant:delete',
    'administration:view',
    'administration:edit'
];

let usersCacheForSources = [];
let usersCacheForPermissions = [];
let branchesCacheForPermissions = [];
let branchPermissionsCache = [];
let currentClientTypeId = null;
let currentUserForClientTypePermissions = null;
let allClientTypes = [];

let loadingStates = {
    users: false,
    containers: false,
    storages: false,
    withdrawalReasons: false,
    products: false,
    sources: false,
    clientTypes: false,
    branchPermissions: false
};

document.addEventListener('DOMContentLoaded', () => {
    AdministrationModal.setupModalClickHandlers();
    initializeAdminTabs();
    setupEventListeners();
    setupPermissionModals();
});

function initializeAdminTabs() {
    const adminContainers = [
        'userListContainer',
        'addUserContainer',
        'client-types-list-container',
        'containerListContainer',
        'addContainerContainer',
        'storageListContainer',
        'addStorageContainer',
        'withdrawalReasonListContainer',
        'addWithdrawalReasonContainer',
        'product-list',
        'create-product',
        'sources-list-container',
        'branchPermissionsContainer'
    ];
    
    let activeAdminButton = null;

    function hideAllAdminSections() {
        adminContainers.forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                el.style.display = 'none';
            }
        });
        document.querySelectorAll('.admin-tabs .tab-btn').forEach(btn => btn.classList.remove('active'));
    }

    hideAllAdminSections();

    function setupAdminTab(buttonId, onShow) {
        const button = document.getElementById(buttonId);
        if (!button) {
            return;
        }
        button.addEventListener('click', async () => {
            const isActive = activeAdminButton === buttonId;
            hideAllAdminSections();
            if (isActive) {
                activeAdminButton = null;
                return;
            }
            activeAdminButton = buttonId;
            button.classList.add('active');
            if (onShow) {
                try {
                    await onShow();
                } catch (error) {
                    console.error(`Error opening section ${buttonId}:`, error);
                    handleError(error);
                }
            }
        });
    }

    setupAdminTab('showUsersBtn', async () => {
        const userListContainer = document.getElementById('userListContainer');
        if (userListContainer) {
            userListContainer.style.display = 'block';
        }
        const addUserContainer = document.getElementById('addUserContainer');
        if (addUserContainer) {
            addUserContainer.style.display = 'none';
        }
        await loadUsers();
    });

    setupAdminTab('showContainersBtn', async () => {
        const list = document.getElementById('containerListContainer');
        const form = document.getElementById('addContainerContainer');
        if (list) list.style.display = 'block';
        if (form) form.style.display = 'block';
        await loadContainers();
    });

    setupAdminTab('showStoragesBtn', async () => {
        const list = document.getElementById('storageListContainer');
        const form = document.getElementById('addStorageContainer');
        if (list) list.style.display = 'block';
        if (form) form.style.display = 'block';
        await loadStorages();
    });

    setupAdminTab('showWithdrawalReasonsBtn', async () => {
        const list = document.getElementById('withdrawalReasonListContainer');
        const form = document.getElementById('addWithdrawalReasonContainer');
        if (list) list.style.display = 'block';
        if (form) form.style.display = 'block';
        await loadWithdrawalReasons();
    });

    setupAdminTab('show-products', async () => {
        const productList = document.getElementById('product-list');
        const createProduct = document.getElementById('create-product');
        if (productList) productList.style.display = 'block';
        if (createProduct) createProduct.style.display = 'block';
        await loadProducts();
    });

    setupAdminTab('showClientTypesBtn', async () => {
        document.getElementById('client-types-list-container').style.display = 'block';
        await loadClientTypes();
    });

    setupAdminTab('showSourcesBtn', async () => {
        const container = document.getElementById('sources-list-container');
        if (container) container.style.display = 'block';
        await loadSources();
    });

    setupAdminTab('showBranchPermissionsBtn', async () => {
        const container = document.getElementById('branchPermissionsContainer');
        if (container) container.style.display = 'block';
        await initializeBranchPermissionsSection();
    });
}

function setupEventListeners() {
    document.getElementById('addUserBtn')?.addEventListener('click', () => {
        const addUserContainer = document.getElementById('addUserContainer');
        if (addUserContainer) {
            addUserContainer.style.display = addUserContainer.style.display === 'none' ? 'block' : 'none';
        }
    });

    document.getElementById('createUserBtn')?.addEventListener('click', handleCreateUser);
    
    const createUserForm = document.getElementById('create-user-form');
    if (createUserForm) {
        createUserForm.addEventListener('submit', (e) => {
            e.preventDefault();
            handleCreateUser();
        });
    }

    document.getElementById('createBarrelTypeBtn')?.addEventListener('click', handleCreateContainer);

    document.getElementById('createStorageBtn')?.addEventListener('click', handleCreateStorage);

    document.getElementById('createWithdrawalReasonBtn')?.addEventListener('click', handleCreateWithdrawalReason);

    document.getElementById('create-product-form')?.addEventListener('submit', handleCreateProduct);
    document.getElementById('edit-product-form')?.addEventListener('submit', handleUpdateProduct);

    document.getElementById('create-source-btn')?.addEventListener('click', async () => {
        await loadUsersForSources();
        AdministrationModal.openCreateSourceModal();
        AdministrationRenderer.populateUserSelect(document.getElementById('source-user'), usersCacheForSources);
    });

    document.getElementById('source-form')?.addEventListener('submit', handleSourceSubmit);

    document.getElementById('addClientTypeBtn')?.addEventListener('click', () => {
        AdministrationModal.openCreateClientTypeModal();
    });

    document.getElementById('create-client-type-form')?.addEventListener('submit', handleCreateClientType);
    document.getElementById('edit-client-type-form')?.addEventListener('submit', handleUpdateClientType);

    document.getElementById('addFieldBtn')?.addEventListener('click', () => {
        document.getElementById('field-client-type-id').value = currentClientTypeId || '';
        AdministrationModal.openAddFieldModal();
    });

    document.getElementById('field-type')?.addEventListener('change', (e) => {
        const type = e.target.value;
        const validationGroup = document.getElementById('field-validation-pattern-group');
        const listValuesGroup = document.getElementById('field-list-values-group');
        if (validationGroup) validationGroup.style.display = type === 'PHONE' ? 'block' : 'none';
        if (listValuesGroup) listValuesGroup.style.display = type === 'LIST' ? 'block' : 'none';
    });

    document.getElementById('create-field-form')?.addEventListener('submit', handleCreateField);
    document.getElementById('edit-field-form')?.addEventListener('submit', handleUpdateField);

    document.getElementById('static-fields-config-form')?.addEventListener('submit', handleStaticFieldsConfigSubmit);

    document.getElementById('client-import-form')?.addEventListener('submit', handleClientImport);

    document.getElementById('edit-container-form')?.addEventListener('submit', handleUpdateContainer);
    document.getElementById('edit-storage-form')?.addEventListener('submit', handleUpdateStorage);
    document.getElementById('edit-withdrawal-reason-form')?.addEventListener('submit', handleUpdateWithdrawalReason);

    document.getElementById('add-branch-permission-btn')?.addEventListener('click', () => {
        AdministrationModal.openBranchPermissionModal();
    });

    document.getElementById('branch-permission-form')?.addEventListener('submit', handleBranchPermissionSubmit);

    document.getElementById('branch-permissions-user-filter')?.addEventListener('change', loadBranchPermissions);
    document.getElementById('branch-permissions-branch-filter')?.addEventListener('change', loadBranchPermissions);
}

function setupPermissionModals() {
    document.querySelector('.close-permissions')?.addEventListener('click', () => {
        AdministrationModal.closeUserPermissionsModal();
    });

    document.getElementById('savePermissionsButton')?.addEventListener('click', handleSaveUserPermissions);

    document.getElementById('saveClientTypePermissionsButton')?.addEventListener('click', handleSaveClientTypePermissions);

    document.querySelectorAll('#branch-permission-modal .close').forEach(closeBtn => {
        closeBtn.addEventListener('click', () => {
            AdministrationModal.closeModal('branch-permission-modal');
        });
    });
}

function loadUserPermissions(user) {
    try {
        const userPermissions = user.authorities || [];
        const permissionsList = document.getElementById('permissionsList');
        if (!permissionsList) return;
        
        permissionsList.textContent = '';

        allPermissions.forEach(permission => {
            const label = document.createElement('label');
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.value = permission;
            checkbox.checked = userPermissions.includes(permission);
            const displayName = AdministrationUtils.formatPermissionName(permission);
            label.appendChild(checkbox);
            label.appendChild(document.createTextNode(` ${displayName}`));
            permissionsList.appendChild(label);
        });

        permissionsList.dataset.userId = user.id;
    } catch (error) {
        console.error('Error loading permissions:', error);
        handleError(error);
    }
}

window.showUserPermissionsModal = function(user) {
    AdministrationModal.openUserPermissionsModal();
    loadUserPermissions(user);
};

window.showUserClientTypePermissionsModal = async function(user) {
    if (!user || !user.id) {
        return;
    }
    currentUserForClientTypePermissions = user;
    window.currentUserForClientTypePermissions = user;
    AdministrationModal.openUserClientTypePermissionsModal(user);
    await loadClientTypePermissions(user.id);
};

async function handleSaveUserPermissions() {
    const permissionsList = document.getElementById('permissionsList');
    if (!permissionsList) return;
    
    const userId = permissionsList.dataset.userId;
    if (!userId) {
        showMessage('Користувач не вибрано', 'error');
        return;
    }
    
    const selectedPermissions = Array.from(permissionsList.querySelectorAll('input:checked'))
        .map(input => input.value);

    try {
        await AdministrationDataLoader.updateUserPermissions(userId, selectedPermissions);
        showMessage('Дозволи оновлено', 'success');
        AdministrationModal.closeUserPermissionsModal();
        await loadUsers();
    } catch (error) {
        handleError(error);
    }
}

async function loadUsers() {
    if (loadingStates.users) return;
    loadingStates.users = true;
    try {
        const users = await AdministrationDataLoader.loadUsers();
        const userList = document.getElementById('userList');
        AdministrationRenderer.renderUsers(users, userList);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.users = false;
    }
}

async function handleCreateUser() {
    const name = document.getElementById('newFullName')?.value.trim();
    const login = document.getElementById('newLogin')?.value.trim();
    const password = document.getElementById('newPassword')?.value;
    const role = document.getElementById('newRole')?.value;

    if (!name || !login || !password || !role) {
        showMessage('Заповніть всі обов\'язкові поля', 'error');
        return;
    }

    try {
        const userDTO = {
            fullName: name,
            login: login,
            password: password,
            role: role
        };

        await AdministrationDataLoader.createUser(userDTO);
        showMessage('Користувач успішно створений', 'success');

        document.getElementById('newFullName').value = '';
        document.getElementById('newLogin').value = '';
        document.getElementById('newPassword').value = '';
        document.getElementById('newRole').value = 'MANAGER';

        await loadUsers();
    } catch (error) {
        handleError(error);
    }
}

window.editUser = function(user, listItem) {
    listItem.textContent = '';

    const loginInput = document.createElement('input');
    loginInput.type = 'text';
    loginInput.value = user.login || '';

    const fullNameInput = document.createElement('input');
    fullNameInput.type = 'text';
    fullNameInput.value = user.fullName || '';

    const roleSelect = document.createElement('select');
    for (const [roleValue, roleLabel] of Object.entries(roles)) {
        const option = document.createElement('option');
        option.value = roleValue.toUpperCase();
        option.textContent = roleLabel;
        if (user.role?.toLowerCase() === roleValue) option.selected = true;
        roleSelect.appendChild(option);
    }

    const saveBtn = document.createElement('button');
    saveBtn.textContent = 'Зберегти';
    saveBtn.addEventListener('click', async () => {
        const updatedUser = {
            login: loginInput.value.trim(),
            fullName: fullNameInput.value.trim(),
            role: roleSelect.value,
            status: user.status
        };

        try {
            await AdministrationDataLoader.updateUser(user.id, updatedUser);
            showMessage('Користувача оновлено', 'success');
            await loadUsers();
        } catch (error) {
            handleError(error);
        }
    });

    const cancelBtn = document.createElement('button');
    cancelBtn.textContent = 'Скасувати';
    cancelBtn.addEventListener('click', () => {
        loadUsers();
    });

    listItem.appendChild(loginInput);
    listItem.appendChild(fullNameInput);
    listItem.appendChild(roleSelect);
    listItem.appendChild(saveBtn);
    listItem.appendChild(cancelBtn);
};

window.toggleUserStatus = async function(userId, newStatus) {
    try {
        const users = await AdministrationDataLoader.loadUsers();
        const user = users.find(u => u.id === userId);
        if (!user) throw new Error('Користувача не знайдено');
        
        const updatedUser = {
            login: user.login,
            fullName: user.fullName,
            role: user.role,
            status: newStatus
        };
        
        await AdministrationDataLoader.updateUser(userId, updatedUser);
        showMessage(`Користувач ${newStatus === 'ACTIVE' ? 'активовано' : 'деактивовано'}`, 'success');
        await loadUsers();
    } catch (error) {
        handleError(error);
    }
};

window.deleteUser = function(userId) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_USER,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await AdministrationDataLoader.deleteUser(userId);
                showMessage('Користувача успішно видалено', 'success');
                await loadUsers();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadContainers() {
    if (loadingStates.containers) return;
    loadingStates.containers = true;
    try {
        const containers = await AdministrationDataLoader.loadContainers();
        const barrelTypeList = document.getElementById('barrelTypeList');
        AdministrationRenderer.renderContainers(containers, barrelTypeList);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.containers = false;
    }
}

async function handleCreateContainer() {
    const name = document.getElementById('newBarrelTypeName')?.value.trim();

    if (!name) {
        showMessage('Введіть назву типу тари', 'error');
        return;
    }

    try {
        await AdministrationDataLoader.createContainer({ name });
        showMessage('Тип тари створено', 'success');
        document.getElementById('newBarrelTypeName').value = '';
        await loadContainers();
    } catch (error) {
        handleError(error);
    }
}

window.editContainer = async function(container) {
    AdministrationModal.openEditContainerModal(container);
};

async function handleUpdateContainer(e) {
    e.preventDefault();
    const id = document.getElementById('edit-container-id')?.value;
    const name = document.getElementById('edit-container-name')?.value.trim();

    if (!name) {
        showMessage('Введіть назву типу тари', 'error');
        return;
    }

    try {
        await AdministrationDataLoader.updateContainer(id, { id: Number(id), name });
        showMessage('Тип тари оновлено', 'success');
        AdministrationModal.closeModal('edit-container-modal');
        await loadContainers();
    } catch (error) {
        handleError(error);
    }
}

window.deleteContainer = function(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_BARREL_TYPE,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await AdministrationDataLoader.deleteContainer(id);
                showMessage('Тип тари видалено', 'success');
                await loadContainers();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadStorages() {
    if (loadingStates.storages) return;
    loadingStates.storages = true;
    try {
        const storages = await AdministrationDataLoader.loadStorages();
        const storageList = document.getElementById('storageList');
        AdministrationRenderer.renderStorages(storages, storageList);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.storages = false;
    }
}

async function handleCreateStorage() {
    const name = document.getElementById('newStorageName')?.value.trim();
    const description = document.getElementById('newStorageDescription')?.value.trim();

    if (!name) {
        showMessage('Введіть назву складу', 'error');
        return;
    }

    try {
        await AdministrationDataLoader.createStorage({ name, description });
        showMessage('Склад створено', 'success');
        document.getElementById('newStorageName').value = '';
        document.getElementById('newStorageDescription').value = '';
        await loadStorages();
    } catch (error) {
        handleError(error);
    }
}

window.editStorage = async function(storage) {
    AdministrationModal.openEditStorageModal(storage);
};

async function handleUpdateStorage(e) {
    e.preventDefault();
    const id = document.getElementById('edit-storage-id')?.value;
    const name = document.getElementById('edit-storage-name')?.value.trim();
    const description = document.getElementById('edit-storage-description')?.value.trim();

    if (!name) {
        showMessage('Введіть назву складу', 'error');
        return;
    }

    try {
        await AdministrationDataLoader.updateStorage(id, { id: Number(id), name, description });
        showMessage('Склад оновлено', 'success');
        AdministrationModal.closeModal('edit-storage-modal');
        await loadStorages();
    } catch (error) {
        handleError(error);
    }
}

window.deleteStorage = function(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_STORAGE,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await AdministrationDataLoader.deleteStorage(id);
                showMessage('Склад видалено', 'success');
                await loadStorages();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadWithdrawalReasons() {
    if (loadingStates.withdrawalReasons) return;
    loadingStates.withdrawalReasons = true;
    try {
        const reasons = await AdministrationDataLoader.loadWithdrawalReasons();
        const withdrawalReasonList = document.getElementById('withdrawalReasonList');
        AdministrationRenderer.renderWithdrawalReasons(reasons, withdrawalReasonList);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.withdrawalReasons = false;
    }
}

async function handleCreateWithdrawalReason() {
    const name = document.getElementById('newWithdrawalReasonName')?.value.trim();
    const purpose = document.getElementById('newWithdrawalReasonPurpose')?.value;

    if (!name || !purpose) {
        showMessage('Заповніть всі обов\'язкові поля', 'error');
        return;
    }

    try {
        await AdministrationDataLoader.createWithdrawalReason({ name, purpose });
        showMessage('Причину списання створено', 'success');
        document.getElementById('newWithdrawalReasonName').value = '';
        document.getElementById('newWithdrawalReasonPurpose').value = 'REMOVING';
        await loadWithdrawalReasons();
    } catch (error) {
        handleError(error);
    }
}

window.editWithdrawalReason = async function(reason) {
    AdministrationModal.openEditWithdrawalReasonModal(reason);
};

async function handleUpdateWithdrawalReason(e) {
    e.preventDefault();
    const id = document.getElementById('edit-withdrawal-reason-id')?.value;
    const name = document.getElementById('edit-withdrawal-reason-name')?.value.trim();
    const purpose = document.getElementById('edit-withdrawal-reason-purpose')?.value;

    if (!name || !purpose) {
        showMessage('Заповніть всі обов\'язкові поля', 'error');
        return;
    }

    try {
        await AdministrationDataLoader.updateWithdrawalReason(id, { name, purpose });
        showMessage('Причину списання оновлено', 'success');
        AdministrationModal.closeModal('edit-withdrawal-reason-modal');
        await loadWithdrawalReasons();
    } catch (error) {
        handleError(error);
    }
}

window.deleteWithdrawalReason = function(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_WITHDRAWAL_REASON,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await AdministrationDataLoader.deleteWithdrawalReason(id);
                showMessage('Причину списання видалено', 'success');
                await loadWithdrawalReasons();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadProducts() {
    if (loadingStates.products) return;
    loadingStates.products = true;
    try {
        const products = await AdministrationDataLoader.loadProducts();
        const productsTableBody = document.getElementById('products-table-body');
        AdministrationRenderer.renderProducts(products, productsTableBody);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.products = false;
    }
}

async function handleCreateProduct(e) {
    e.preventDefault();
    const productData = {
        name: document.getElementById('create-name')?.value.trim(),
        usage: document.getElementById('create-usage')?.value
    };

    if (!productData.name || !productData.usage) {
        showMessage('Заповніть всі обов\'язкові поля', 'error');
        return;
    }

    try {
        await AdministrationDataLoader.createProduct(productData);
        showMessage('Продукт створено', 'success');
        document.getElementById('create-product-form')?.reset();
        await loadProducts();
    } catch (error) {
        handleError(error);
    }
}

window.editProduct = async function(id) {
    try {
        const product = await AdministrationDataLoader.loadProduct(id);
        AdministrationModal.openEditProductModal(id);
        AdministrationModal.populateProductForm(product);
    } catch (error) {
        handleError(error);
    }
};

async function handleUpdateProduct(e) {
    e.preventDefault();
    const id = document.getElementById('edit-id')?.value;
    const productData = {
        name: document.getElementById('edit-name')?.value.trim(),
        usage: document.getElementById('edit-usage')?.value
    };

    if (!productData.name || !productData.usage) {
        showMessage('Заповніть всі обов\'язкові поля', 'error');
        return;
    }

    try {
        await AdministrationDataLoader.updateProduct(id, productData);
        showMessage('Продукт оновлено', 'success');
        AdministrationModal.closeModal('edit-product-modal');
        await loadProducts();
    } catch (error) {
        handleError(error);
    }
}

window.deleteProduct = function(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_PRODUCT,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await AdministrationDataLoader.deleteProduct(id);
                showMessage('Продукт видалено', 'success');
                await loadProducts();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadUsersForSources() {
    try {
        if (usersCacheForSources.length === 0) {
            usersCacheForSources = await AdministrationDataLoader.loadAllUsers();
        }
    } catch (error) {
        console.error('Error loading users for sources:', error);
        usersCacheForSources = [];
    }
}

async function loadSources() {
    if (loadingStates.sources) return;
    loadingStates.sources = true;
    try {
        await loadUsersForSources();
        const sources = await AdministrationDataLoader.loadSources();
        const sourceBody = document.getElementById('source-body');
        AdministrationRenderer.renderSources(sources, sourceBody, usersCacheForSources);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.sources = false;
    }
}

async function handleSourceSubmit(e) {
    e.preventDefault();
    const sourceId = document.getElementById('source-id')?.value;
    const name = document.getElementById('source-name')?.value.trim();
    const userIdValue = document.getElementById('source-user')?.value;

    if (!name) {
        showMessage('Введіть назву джерела', 'error');
        return;
    }

    try {
        const formData = {
            name: name,
            userId: userIdValue && userIdValue !== '' ? Number(userIdValue) : null
        };

        if (sourceId) {
            await AdministrationDataLoader.updateSource(sourceId, formData);
            showMessage('Джерело оновлено', 'success');
        } else {
            await AdministrationDataLoader.createSource(formData);
            showMessage('Джерело створено', 'success');
        }
        
        AdministrationModal.closeModal('create-source-modal');
        document.getElementById('source-form')?.reset();
        document.getElementById('source-id').value = '';
        await loadSources();
    } catch (error) {
        handleError(error);
    }
}

window.editSource = async function(id) {
    try {
        await loadUsersForSources();
        const source = await AdministrationDataLoader.loadSource(id);
        AdministrationModal.openEditSourceModal(source);
        AdministrationRenderer.populateUserSelect(document.getElementById('source-user'), usersCacheForSources, source.userId);
    } catch (error) {
        handleError(error);
    }
};

window.deleteSource = function(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_SOURCE,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await AdministrationDataLoader.deleteSource(id);
                showMessage('Джерело видалено', 'success');
                await loadSources();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadClientTypes() {
    if (loadingStates.clientTypes) return;
    loadingStates.clientTypes = true;
    try {
        const clientTypes = await AdministrationDataLoader.loadClientTypes();
        const tbody = document.getElementById('client-types-table-body');
        AdministrationRenderer.renderClientTypes(clientTypes, tbody);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.clientTypes = false;
    }
}

window.downloadClientImportTemplate = function(clientTypeId) {
    window.location.href = `/api/v1/client/import/template/${clientTypeId}`;
};

window.openClientImportModal = function(clientTypeId) {
    AdministrationModal.openClientImportModal(clientTypeId);
};

async function handleClientImport(e) {
    e.preventDefault();
    const clientTypeId = document.getElementById('import-client-type-id')?.value;
    const fileInput = document.getElementById('import-file');
    const file = fileInput?.files[0];
    
    if (!file) {
        showMessage('Будь ласка, виберіть файл', 'error');
        return;
    }
    
    try {
        if (loaderBackdrop) loaderBackdrop.style.display = 'flex';
        const result = await AdministrationDataLoader.importClients(clientTypeId, file);
        showMessage(result || 'Клієнтів успішно імпортовано', 'success');
        AdministrationModal.closeModal('client-import-modal');
        document.getElementById('client-import-form')?.reset();
    } catch (error) {
        handleError(error);
    } finally {
        if (loaderBackdrop) loaderBackdrop.style.display = 'none';
    }
}

async function handleCreateClientType(e) {
    e.preventDefault();
    const formData = {
        name: document.getElementById('client-type-name')?.value.trim(),
        nameFieldLabel: document.getElementById('client-type-name-field-label')?.value.trim() || 'Компанія'
    };

    if (!formData.name) {
        showMessage('Введіть назву типу клієнта', 'error');
        return;
    }

    try {
        await AdministrationDataLoader.createClientType(formData);
        showMessage('Тип клієнта успішно створено', 'success');
        AdministrationModal.closeModal('add-client-type-modal');
        document.getElementById('create-client-type-form')?.reset();
        await loadClientTypes();
    } catch (error) {
        handleError(error);
    }
}

window.openEditClientTypeModal = async function(id) {
    try {
        const type = await AdministrationDataLoader.loadClientType(id);
        AdministrationModal.openEditClientTypeModal(type);
    } catch (error) {
        handleError(error);
    }
};

async function handleUpdateClientType(e) {
    e.preventDefault();
    const id = document.getElementById('edit-client-type-id')?.value;
    const formData = {
        name: document.getElementById('edit-client-type-name')?.value.trim(),
        nameFieldLabel: document.getElementById('edit-client-type-name-field-label')?.value.trim(),
        isActive: document.getElementById('edit-client-type-active')?.checked
    };

    if (!formData.name) {
        showMessage('Введіть назву типу клієнта', 'error');
        return;
    }

    try {
        await AdministrationDataLoader.updateClientType(id, formData);
        showMessage('Тип клієнта успішно оновлено', 'success');
        AdministrationModal.closeModal('edit-client-type-modal');
        await loadClientTypes();
    } catch (error) {
        handleError(error);
    }
}

window.deleteClientType = function(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_CLIENT_TYPE,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await AdministrationDataLoader.deleteClientType(id);
                showMessage('Тип клієнта успішно видалено', 'success');
                await loadClientTypes();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

window.manageClientTypeFields = async function(clientTypeId) {
    currentClientTypeId = clientTypeId;
    AdministrationModal.openManageFieldsModal();
    await loadClientTypeFields(clientTypeId);
    
    const configureBtn = document.getElementById('configureStaticFieldsBtn');
    if (configureBtn) {
        const newBtn = configureBtn.cloneNode(true);
        configureBtn.parentNode.replaceChild(newBtn, configureBtn);
        
        newBtn.addEventListener('click', async (e) => {
            e.preventDefault();
            e.stopPropagation();
            if (!currentClientTypeId) {
                showMessage('Спочатку виберіть тип клієнта', 'error');
                return;
            }
            await openStaticFieldsConfigModal(currentClientTypeId);
        });
    }
};

async function loadClientTypeFields(clientTypeId) {
    try {
        const fields = await AdministrationDataLoader.loadClientTypeFields(clientTypeId);
        const tbody = document.getElementById('fields-table-body');
        AdministrationRenderer.renderFields(fields, tbody);
    } catch (error) {
        handleError(error);
    }
}

async function handleCreateField(e) {
    e.preventDefault();
    const listValuesText = document.getElementById('field-list-values')?.value || '';
    const formData = {
        fieldName: document.getElementById('field-name')?.value.trim(),
        fieldLabel: document.getElementById('field-label')?.value.trim(),
        fieldType: document.getElementById('field-type')?.value,
        isRequired: document.getElementById('field-required')?.checked || false,
        isSearchable: document.getElementById('field-searchable')?.checked || false,
        isFilterable: document.getElementById('field-filterable')?.checked || false,
        isVisibleInTable: document.getElementById('field-visible')?.checked || false,
        isVisibleInCreate: document.getElementById('field-visible-in-create')?.checked || false,
        columnWidth: document.getElementById('field-column-width')?.value ? parseInt(document.getElementById('field-column-width').value) : null,
        validationPattern: document.getElementById('field-validation-pattern')?.value?.trim() || null,
        allowMultiple: document.getElementById('field-allow-multiple')?.checked || false,
        listValues: listValuesText ? listValuesText.split('\n').filter(v => v.trim()).map(v => v.trim()) : []
    };

    if (!formData.fieldName || !formData.fieldLabel || !formData.fieldType) {
        showMessage('Заповніть всі обов\'язкові поля', 'error');
        return;
    }

    try {
        await AdministrationDataLoader.createClientTypeField(currentClientTypeId, formData);
        showMessage('Поле успішно створено', 'success');
        AdministrationModal.closeModal('add-field-modal');
        document.getElementById('create-field-form')?.reset();
        await loadClientTypeFields(currentClientTypeId);
    } catch (error) {
        handleError(error);
    }
}

window.openEditFieldModal = async function(fieldId) {
    try {
        const field = await AdministrationDataLoader.loadClientTypeField(fieldId);
        AdministrationModal.openEditFieldModal(field);
    } catch (error) {
        handleError(error);
    }
};

async function handleUpdateField(e) {
    e.preventDefault();
    const fieldId = document.getElementById('edit-field-id')?.value;
    const listValuesText = document.getElementById('edit-field-list-values')?.value || '';
    const formData = {
        fieldLabel: document.getElementById('edit-field-label')?.value.trim(),
        isRequired: document.getElementById('edit-field-required')?.checked || false,
        isSearchable: document.getElementById('edit-field-searchable')?.checked || false,
        isFilterable: document.getElementById('edit-field-filterable')?.checked || false,
        isVisibleInTable: document.getElementById('edit-field-visible')?.checked || false,
        isVisibleInCreate: document.getElementById('edit-field-visible-in-create')?.checked || false,
        displayOrder: document.getElementById('edit-field-display-order')?.value ? parseInt(document.getElementById('edit-field-display-order').value) || 0 : 0,
        columnWidth: document.getElementById('edit-field-column-width')?.value ? parseInt(document.getElementById('edit-field-column-width').value) : null,
        validationPattern: document.getElementById('edit-field-validation-pattern')?.value?.trim() || null,
        allowMultiple: document.getElementById('edit-field-allow-multiple')?.checked || false,
        listValues: listValuesText ? listValuesText.split('\n').filter(v => v.trim()).map(v => v.trim()) : []
    };

    if (!formData.fieldLabel) {
        showMessage('Введіть мітку поля', 'error');
        return;
    }

    try {
        await AdministrationDataLoader.updateClientTypeField(fieldId, formData);
        showMessage('Поле успішно оновлено', 'success');
        AdministrationModal.closeModal('edit-field-modal');
        await loadClientTypeFields(currentClientTypeId);
    } catch (error) {
        handleError(error);
    }
}

window.deleteField = function(fieldId) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_FIELD,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await AdministrationDataLoader.deleteClientTypeField(fieldId);
                showMessage('Поле успішно видалено', 'success');
                await loadClientTypeFields(currentClientTypeId);
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function openStaticFieldsConfigModal(clientTypeId) {
    try {
        const config = await AdministrationDataLoader.loadClientTypeStaticFieldsConfig(clientTypeId);
        
        document.getElementById('static-company-visible').checked = config.company?.visible || false;
        document.getElementById('static-company-label').value = config.company?.label || 'Компанія';
        document.getElementById('static-company-order').value = config.company?.order || 0;
        document.getElementById('static-company-width').value = config.company?.width || 200;

        document.getElementById('static-source-visible').checked = config.source?.visible !== undefined ? config.source.visible : true;
        document.getElementById('static-source-label').value = config.source?.label || 'Залучення';
        document.getElementById('static-source-order').value = config.source?.order || 999;
        document.getElementById('static-source-width').value = config.source?.width || 200;

        document.getElementById('static-createdAt-visible').checked = config.createdAt?.visible || false;
        document.getElementById('static-createdAt-label').value = config.createdAt?.label || 'Створено';
        document.getElementById('static-createdAt-order').value = config.createdAt?.order || 1000;
        document.getElementById('static-createdAt-width').value = config.createdAt?.width || 150;

        document.getElementById('static-updatedAt-visible').checked = config.updatedAt?.visible || false;
        document.getElementById('static-updatedAt-label').value = config.updatedAt?.label || 'Оновлено';
        document.getElementById('static-updatedAt-order').value = config.updatedAt?.order || 1001;
        document.getElementById('static-updatedAt-width').value = config.updatedAt?.width || 150;

        AdministrationModal.openStaticFieldsConfigModal(clientTypeId);
    } catch (error) {
        handleError(error);
    }
}

async function handleStaticFieldsConfigSubmit(e) {
    e.preventDefault();
    const clientTypeId = document.getElementById('static-fields-client-type-id')?.value;
    if (!clientTypeId) {
        showMessage('Тип клієнта не вибрано', 'error');
        return;
    }

    try {
        const configData = {
            company: {
                visible: document.getElementById('static-company-visible')?.checked || false,
                label: document.getElementById('static-company-label')?.value.trim() || 'Компанія',
                order: parseInt(document.getElementById('static-company-order')?.value) || 0,
                width: parseInt(document.getElementById('static-company-width')?.value) || 200
            },
            source: {
                visible: document.getElementById('static-source-visible')?.checked !== undefined ? document.getElementById('static-source-visible').checked : true,
                label: document.getElementById('static-source-label')?.value.trim() || 'Залучення',
                order: parseInt(document.getElementById('static-source-order')?.value) || 999,
                width: parseInt(document.getElementById('static-source-width')?.value) || 200
            },
            createdAt: {
                visible: document.getElementById('static-createdAt-visible')?.checked || false,
                label: document.getElementById('static-createdAt-label')?.value.trim() || 'Створено',
                order: parseInt(document.getElementById('static-createdAt-order')?.value) || 1000,
                width: parseInt(document.getElementById('static-createdAt-width')?.value) || 150
            },
            updatedAt: {
                visible: document.getElementById('static-updatedAt-visible')?.checked || false,
                label: document.getElementById('static-updatedAt-label')?.value.trim() || 'Оновлено',
                order: parseInt(document.getElementById('static-updatedAt-order')?.value) || 1001,
                width: parseInt(document.getElementById('static-updatedAt-width')?.value) || 150
            }
        };

        await AdministrationDataLoader.updateClientTypeStaticFieldsConfig(clientTypeId, configData);
        showMessage('Налаштування статичних полів збережено', 'success');
        AdministrationModal.closeModal('static-fields-config-modal');
        await loadClientTypeFields(currentClientTypeId);
    } catch (error) {
        handleError(error);
    }
}

async function loadClientTypePermissions(userId) {
    if (!userId || userId === 'undefined' || userId === undefined) {
        return;
    }
    try {
        const { allClientTypes: loadedClientTypes, permissionsMap } = await AdministrationDataLoader.loadClientTypePermissions(userId);
        allClientTypes = loadedClientTypes;
        const container = document.getElementById('clientTypePermissionsList');
        AdministrationRenderer.renderClientTypePermissions(allClientTypes, permissionsMap, container);
    } catch (error) {
        handleError(error);
    }
}

async function handleSaveClientTypePermissions() {
    if (!currentUserForClientTypePermissions) {
        showMessage('Користувач не вибрано', 'error');
        return;
    }
    
    const checkboxes = document.querySelectorAll('#clientTypePermissionsList .permission-checkbox:not(:disabled)');
    const permissionsByClientType = new Map();
    
    checkboxes.forEach(checkbox => {
        const clientTypeId = parseInt(checkbox.dataset.clientTypeId);
        const permissionType = checkbox.dataset.permissionType;
        
        if (!permissionsByClientType.has(clientTypeId)) {
            permissionsByClientType.set(clientTypeId, {
                canView: false,
                canCreate: false,
                canEdit: false,
                canDelete: false
            });
        }
        
        const permission = permissionsByClientType.get(clientTypeId);
        permission[permissionType] = checkbox.checked;
    });
    
    if (!currentUserForClientTypePermissions || !currentUserForClientTypePermissions.id) {
        return;
    }
    
    try {
        const updatePromises = [];
        for (const [clientTypeId, permission] of permissionsByClientType.entries()) {
            updatePromises.push(
                AdministrationDataLoader.updateClientTypePermission(clientTypeId, currentUserForClientTypePermissions.id, permission)
            );
        }
        await Promise.all(updatePromises);
        showMessage('Права доступу до типів клієнтів збережено', 'success');
        await loadClientTypePermissions(currentUserForClientTypePermissions.id);
        AdministrationModal.closeModal('userClientTypePermissionsModal');
        currentUserForClientTypePermissions = null;
        window.currentUserForClientTypePermissions = null;
    } catch (error) {
        handleError(error);
    }
}

window.createClientTypePermission = async function(clientTypeId, userId) {
    if (!userId || userId === 'undefined' || userId === undefined) {
        return;
    }
    try {
        await AdministrationDataLoader.createClientTypePermission(clientTypeId, userId);
        showMessage('Доступ до типу клієнта створено', 'success');
        await loadClientTypePermissions(userId);
    } catch (error) {
        handleError(error);
    }
};

window.deleteClientTypePermission = function(clientTypeId, userId) {
    if (!userId || userId === 'undefined' || userId === undefined) {
        return;
    }
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_CLIENT_TYPE_ACCESS,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await AdministrationDataLoader.deleteClientTypePermission(clientTypeId, userId);
                showMessage('Доступ до типу клієнта видалено', 'success');
                await loadClientTypePermissions(userId);
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function initializeBranchPermissionsSection() {
    await loadUsersForPermissions();
    await loadBranchesForPermissions();
    await loadBranchPermissions();
}

async function loadUsersForPermissions() {
    try {
        usersCacheForPermissions = await AdministrationDataLoader.loadAllUsers();
        const userFilter = document.getElementById('branch-permissions-user-filter');
        const userSelect = document.getElementById('branch-permission-user');
        AdministrationRenderer.populateUserFilterSelect(userFilter, usersCacheForPermissions);
        AdministrationRenderer.populateUserSelect(userSelect, usersCacheForPermissions);
    } catch (error) {
        handleError(error);
    }
}

async function loadBranchesForPermissions() {
    try {
        branchesCacheForPermissions = await AdministrationDataLoader.loadBranches();
        const branchFilter = document.getElementById('branch-permissions-branch-filter');
        const branchSelect = document.getElementById('branch-permission-branch');
        AdministrationRenderer.populateBranchFilterSelect(branchFilter, branchesCacheForPermissions);
        AdministrationRenderer.populateBranchSelect(branchSelect, branchesCacheForPermissions);
    } catch (error) {
        handleError(error);
    }
}

async function loadBranchPermissions() {
    if (loadingStates.branchPermissions) return;
    loadingStates.branchPermissions = true;
    try {
        const userId = document.getElementById('branch-permissions-user-filter')?.value;
        const branchId = document.getElementById('branch-permissions-branch-filter')?.value;
        const permissions = await AdministrationDataLoader.loadBranchPermissions(userId || null, branchId || null);
        branchPermissionsCache = permissions;
        const tbody = document.getElementById('branch-permissions-body');
        AdministrationRenderer.renderBranchPermissions(permissions, tbody, usersCacheForPermissions, branchesCacheForPermissions);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.branchPermissions = false;
    }
}

window.editBranchPermission = async function(permissionId) {
    try {
        const permission = branchPermissionsCache.find(p => p.id === permissionId);
        if (!permission) {
            showMessage('Права доступу не знайдено', 'error');
            return;
        }
        AdministrationModal.openEditBranchPermissionModal(permission);
    } catch (error) {
        handleError(error);
    }
};

async function handleBranchPermissionSubmit(e) {
    e.preventDefault();
    const permissionId = document.getElementById('branch-permission-id')?.value;
    const formData = {
        userId: parseInt(document.getElementById('branch-permission-user')?.value),
        branchId: parseInt(document.getElementById('branch-permission-branch')?.value),
        canView: document.getElementById('branch-permission-can-view')?.checked || false,
        canOperate: document.getElementById('branch-permission-can-operate')?.checked || false
    };

    if (!formData.userId || !formData.branchId) {
        showMessage('Виберіть користувача та філію', 'error');
        return;
    }

    try {
        await AdministrationDataLoader.createBranchPermission(formData);
        showMessage(`Права доступу успішно ${permissionId ? 'оновлено' : 'створено'}`, 'success');
        AdministrationModal.closeModal('branch-permission-modal');
        document.getElementById('branch-permission-form')?.reset();
        document.getElementById('branch-permission-id').value = '';
        await loadBranchPermissions();
    } catch (error) {
        handleError(error);
    }
}

window.deleteBranchPermission = function(userId, branchId) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_PERMISSIONS,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await AdministrationDataLoader.deleteBranchPermission(userId, branchId);
                showMessage('Права доступу успішно видалено', 'success');
                await loadBranchPermissions();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

