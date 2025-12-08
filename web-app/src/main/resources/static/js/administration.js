const loaderBackdrop = document.getElementById('loader-backdrop');
const allPermissions = [
    'system:admin',

    'user:create',
    'user:edit',
    'user:delete',

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

    'analytics:view',

    'settings:view',
    'settings_client:create',
    'settings_client:edit',
    'settings_client:delete',
    'settings_finance:create',
    'settings_finance:edit',
    'settings_exchange:edit',
    'settings_finance:delete',

    'administration:view',
    'administration:edit'
];

function showUserPermissionsModal(user) {
    const modal = document.getElementById('userPermissionsModal');
    modal.style.display = 'flex';
    loadUserPermissions(user);
}

function closeUserPermissionsModal() {
    const modal = document.getElementById('userPermissionsModal');
    modal.style.display = 'none';
}

document.querySelector('.close-permissions').addEventListener('click', closeUserPermissionsModal);

function loadUserPermissions(user) {
    try {
        const userPermissions = user.authorities || [];
        const permissionsList = document.getElementById('permissionsList');
        permissionsList.innerHTML = '';

        allPermissions.forEach(permission => {
            const label = document.createElement('label');
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.value = permission;
            checkbox.checked = userPermissions.includes(permission);
            // Форматируем метку для читаемости, например, "System Admin" вместо "system:admin"
            const displayName = formatPermissionName(permission);
            label.appendChild(checkbox);
            label.appendChild(document.createTextNode(` ${displayName}`));
            permissionsList.appendChild(label);
        });

        permissionsList.dataset.userId = user.id;
    } catch (error) {
        console.error('Error loading permissions:', error);
        showMessage('Помилка завантаження дозволів', 'error');
    }
}

function formatPermissionName(permission) {
    const permissionMap = {
        'system:admin': 'system:admin',

        'user:create': 'Создание пользователей',
        'user:edit': 'Изменение пользователей',
        'user:delete': 'Удаление пользователей',

        'client:view': 'Отображение клиентов',
        'client:create': 'Создание клиентов',
        'client:edit': 'Редактирование клиентов',
        'client:delete': 'Удаление клиентов',
        'client_stranger:edit': 'Редактирование чужих клиентов та зміна привлечення',
        'client:full_delete': 'Полное удаление клиента',
        'client:excel': 'Выгрузка клиентов в excel',

        'sale:view': 'Отображение продаж',
        'sale:create': 'Создание продаж',
        'sale:edit': 'Изменение продаж',
        'sale:edit_strangers': 'Редактирование чужих продаж',
        'sale:delete': 'Удаление продаж',
        'sale:edit_source': 'Изменение привлечения продаж',
        'sale:excel': 'Выгрузка продаж в excel',

        'purchase:view': 'Отображение закупок',
        'purchase:create': 'Создание закупок',
        'purchase:edit': 'Изменение закупок',
        'purchase:edit_strangers': 'Редактирование чужих закупок',
        'purchase:delete': 'Удаление закупок',
        'purchase:edit_source': 'Изменение привлечения закупок',
        'purchase:excel': 'Выгрузка закупок в excel',

        'container:view': 'Отображение тары',
        'container:transfer': 'Передача тары',
        'container:balance': 'Изменения баланса тары',
        'container:excel': 'Выгрузка тары в excel',

        'finance:view': 'Отображение финансов',
        'finance:balance_edit': 'Изменение баланса финансов',
        'finance:transfer_view': 'Просмотр транзакций',
        'finance:transfer_excel': 'Выгрузка в excel транзакций',
        'transaction:delete': 'Удаление закупок и продаж',

        'warehouse:view': 'Отображение склада',
        'warehouse:create': 'Создание прихода на склад',
        'warehouse:edit': 'Изменение прихода на склад',
        'warehouse:withdraw': 'Снятие баланса склада',
        'warehouse:delete': 'Удаление прихода на склад',
        'warehouse:excel': 'Выгрузка в excel данных со склада',

        'inventory:view': 'Отображение баланса тары',
        'inventory:manage': 'Управление балансом тары',

        'analytics:view': 'Отображение аналитики',

        'settings:view': 'Отображение настроек',
        'settings_client:create': 'Разрешение создавать поля для клиента',
        'settings_client:edit': 'Разрешение редактировать поля для клиента',
        'settings_client:delete': 'Разрешение удалять поля для клиента',
        'settings_finance:create': 'Разрешение создавать поля для финансов',
        'settings_finance:edit': 'Разрешение изменять поля для финансов',
        'settings_exchange:edit': 'Разрешение изменять курс валют',
        'settings_finance:delete': 'Разрешение удалять поля для финансов',

        'administration:view': 'Отображение администраторской',
        'administration:edit': 'Редактирование в администраторской'
    };
    return permissionMap[permission] || permission;
}

document.getElementById('savePermissionsButton').addEventListener('click',
    async () => {
    const permissionsList = document.getElementById('permissionsList');
    const userId = permissionsList.dataset.userId;
    const selectedPermissions = Array.from(permissionsList.querySelectorAll('input:checked'))
        .map(input => input.value);

    try {
        const response = await fetch(`/api/v1/user/${userId}/permissions`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(selectedPermissions)
        });
        if (!response.ok) new Error('Failed to update permissions');
        showMessage('Дозволи оновлено', 'info');
        closeUserPermissionsModal();
    } catch (error) {
        console.error('Error updating permissions:', error);
        showMessage('Помилка оновлення дозволів', 'error');
    }
});

document.addEventListener('DOMContentLoaded', function () {
    const adminContainers = [
        'userListContainer',
        'addUserContainer',
        'client-types-list-container',
        'oilTypeListContainer',
        'addOilTypeContainer',
        'containerListContainer',
        'addContainerContainer',
        'storageListContainer',
        'addStorageContainer',
        'withdrawalReasonListContainer',
        'addWithdrawalReasonContainer',
        'product-list',
        'create-product',
        'client-product-list',
        'client-create-product',
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
                }
            }
        });
    }

    const addUserBtn = document.getElementById('addUserBtn');
    const addUserContainer = document.getElementById('addUserContainer');

    setupAdminTab('showUsersBtn', async () => {
        const userListContainer = document.getElementById('userListContainer');
        if (userListContainer) {
            userListContainer.style.display = 'block';
        }
        if (addUserContainer) {
            addUserContainer.style.display = 'none';
        }
        await renderUserList();
    });

    function showEditForm(user, listItem) {
        listItem.innerHTML = '';

        // Create input for login change
        const loginInput = document.createElement('input');
        loginInput.type = 'text';
        loginInput.value = user.login;

        // Create input for fullName change
        const fullNameInput = document.createElement('input');
        fullNameInput.type = 'text';
        fullNameInput.value = user.fullName;

        // Create select for role change
        const roleSelect = document.createElement('select');
        const roles = {
            'manager': 'Менеджер',
            'driver': 'Водій',
            'storekeeper': 'Комірник',
            'leader': 'Керівник'
        };

        for (const [roleValue, roleLabel] of Object.entries(roles)) {
            const option = document.createElement('option');
            option.value = roleValue;
            option.textContent = roleLabel;
            if (user.role.toLowerCase() === roleValue) option.selected = true;
            roleSelect.appendChild(option);
        }

        // Save button
        const saveBtn = document.createElement('button');
        saveBtn.textContent = 'Зберегти';
        saveBtn.addEventListener('click', async () => {
            const selectedRole = roleSelect.value;
            const updatedUser = {
                ...user,
                login: loginInput.value,
                fullName: fullNameInput.value,
                role: selectedRole
            };

            try {
                const response = await fetch(`/api/v1/user/${user.id}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(updatedUser)
                });

                if (response.ok) {
                    const updatedUserData = await response.json();
                    listItem.textContent = `${updatedUserData.id} - ${updatedUserData.login} - 
                    ${roles[updatedUserData.role.toLowerCase()]}`;
                    listItem.appendChild(createEditButton(updatedUserData, listItem));
                    listItem.appendChild(createDeleteButton(updatedUserData.id));
                } else {
                    console.error('Error updating user');
                }
            } catch (error) {
                console.error('Error saving user:', error);
            }
        });

        const cancelBtn = document.createElement('button');
        cancelBtn.textContent = 'Скасувати';
        cancelBtn.addEventListener('click', () => {
            listItem.textContent = `${user.fullName} - ${user.id} - ${user.login} - ${roles[user.role.toLowerCase()]}`;
            listItem.appendChild(createEditButton(user, listItem));
            listItem.appendChild(createDeleteButton(user.id));
        });

        listItem.appendChild(loginInput);
        listItem.appendChild(fullNameInput);
        listItem.appendChild(roleSelect);
        listItem.appendChild(saveBtn);
        listItem.appendChild(cancelBtn);
    }

    async function toggleUserStatus(userId, newStatus) {
        try {
            // First get current user data
            const getUserResponse = await fetch(`/api/v1/user/page`);
            if (!getUserResponse.ok) throw new Error('Failed to get user');
            const users = await getUserResponse.json();
            const user = users.find(u => u.id === userId);
            if (!user) throw new Error('User not found');
            
            // Update user with new status
            const updatedUser = {
                login: user.login,
                fullName: user.fullName,
                role: user.role,
                status: newStatus
            };
            
            const response = await fetch(`/api/v1/user/${userId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(updatedUser)
            });
            
            if (response.ok) {
                showMessage(`Користувач ${newStatus === 'ACTIVE' ? 'активовано' : 'деактивовано'}`, 'info');
                if (activeAdminButton === 'showUsersBtn') {
                    await renderUserList();
                }
            } else {
                const error = await response.json().catch(() => ({}));
                throw new Error(error.message || 'Failed to update user status');
            }
        } catch (error) {
            console.error('Error toggling user status:', error);
            showMessage('Помилка зміни статусу користувача: ' + error.message, 'error');
        }
    }

    async function deleteUser(userId) {
        try {
            const response = await fetch(`/api/v1/user/${userId}`, {
                method: 'DELETE'
            });
            if (response.ok) {
                if (activeAdminButton === 'showUsersBtn') {
                    await renderUserList();
                }
            } else {
                console.error('Error deleting user');
            }
        } catch (error) {
            console.error('Error:', error);
        }
    }


// Helper functions to create buttons
    function createEditButton(user, listItem) {
        const editBtn = document.createElement('button');
        editBtn.textContent = 'Змінити';
        editBtn.addEventListener('click', () => showEditForm(user, listItem));
        return editBtn;
    }

    function createDeleteButton(userId) {
        const deleteBtn = document.createElement('button');
        deleteBtn.textContent = 'Видалити';
        deleteBtn.addEventListener('click', () => deleteUser(userId));
        return deleteBtn;
    }


    addUserBtn.addEventListener('click', function () {
        addUserContainer.style.display = addUserContainer.style.display === 'none' ? 'block' : 'none';
    });

    async function renderUserList() {
        try {
            const response = await fetch('/api/v1/user/page');
            const users = await response.json();
            const userList = document.getElementById('userList');
            userList.innerHTML = '';

            users.forEach(user => {
                const listItem = document.createElement('li');
                const statusText = user.status === 'BANNED' ? '❌ Заблокований' : '✅ Активний';
                listItem.textContent = `${user.fullName} - ${user.id} - ${user.login} - ${user.role} - ${statusText} `;

                const editBtn = document.createElement('button');
                editBtn.textContent = 'Змінити';
                editBtn.addEventListener('click', () => showEditForm(user, listItem));

                const permissionsBtn = document.createElement('button');
                permissionsBtn.textContent = 'Редагувати дозволи';
                permissionsBtn.className = 'edit-permissions';
                permissionsBtn.addEventListener('click', () => showUserPermissionsModal(user));

                const clientTypePermissionsBtn = document.createElement('button');
                clientTypePermissionsBtn.textContent = 'Доступ до типів клієнтів';
                clientTypePermissionsBtn.className = 'btn-secondary';
                clientTypePermissionsBtn.style.marginLeft = '5px';
                clientTypePermissionsBtn.addEventListener('click', () => showUserClientTypePermissionsModal(user));

                const statusBtn = document.createElement('button');
                const nextStatus = user.status === 'BANNED' ? 'ACTIVE' : 'BANNED';
                statusBtn.textContent = user.status === 'BANNED' ? 'Активувати' : 'Деактивувати';
                statusBtn.className = user.status === 'BANNED' ? 'btn-activate' : 'btn-deactivate';
                statusBtn.addEventListener('click', () => toggleUserStatus(user.id, nextStatus));

                const deleteBtn = document.createElement('button');
                deleteBtn.textContent = 'Видалити';
                deleteBtn.addEventListener('click', () => deleteUser(user.id));

                listItem.appendChild(editBtn);
                listItem.appendChild(statusBtn);
                listItem.appendChild(deleteBtn);
                listItem.appendChild(permissionsBtn);
                listItem.appendChild(clientTypePermissionsBtn);
                userList.appendChild(listItem);
            });
        } catch (error) {
            console.error('Error fetching users:', error);
        }
    }

    document.getElementById('createUserBtn').addEventListener('click',
        async function () {
        const name = document.getElementById('newFullName').value;
        const login = document.getElementById('newLogin').value;
        const password = document.getElementById('newPassword').value;
        const role = document.getElementById('newRole').value;

        const userDTO = {
            fullName: name,
            login: login,
            password: password,
            role: role
        };

        try {
            const response = await fetch('/api/v1/user', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(userDTO)
            });

            if (!response.ok) {
                new Error('Помилка створення користувача');
            }

            alert('Користувач успішно створений!');

            if (activeAdminButton === 'showUsersBtn') {
                await renderUserList();
            }

            document.getElementById('newLogin').value = '';
            document.getElementById('newPassword').value = '';
            document.getElementById('newRole').value = 'MANAGER';

        } catch (error) {
            console.error('Error creating user:', error);
        }
    });

    /*----------container----------*/

    setupAdminTab('showContainersBtn', async () => {
        const list = document.getElementById('containerListContainer');
        const form = document.getElementById('addContainerContainer');
        if (list) list.style.display = 'block';
        if (form) form.style.display = 'block';
        await loadContainers();
    });

    async function loadContainers() {
        try {
            const response = await fetch('/api/v1/container');
            if (!response.ok) new Error('Error fetching barrelTypes');
            const barrelTypes = await response.json();
            const barrelTypeList = document.getElementById('barrelTypeList');
            barrelTypeList.innerHTML = '';

            barrelTypes.forEach(barrelType => {
                const listItem = document.createElement('li');
                listItem.classList.add('barrelType-item');
                listItem.setAttribute('data-barrelType-id', barrelType.id);

                const barrelTypeText = document.createElement('span');
                barrelTypeText.textContent = `${barrelType.id} - ${barrelType.name}`;
                listItem.appendChild(barrelTypeText);

                const editButton = document.createElement('button');
                editButton.textContent = 'Змінити';
                editButton.onclick = () => editBarrelType(barrelType);
                listItem.appendChild(editButton);

                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'Видалити';
                deleteButton.onclick = () => deleteBarrelType(barrelType.id);
                listItem.appendChild(deleteButton);

                barrelTypeList.appendChild(listItem);
            });
        } catch (error) {
            console.error('Error loading barrelTypes:', error);
        }
    }

    function editBarrelType(barrelType) {
        const newName = prompt('Введіть нове ім’я типу тари:', barrelType.name);
        if (newName !== null) {
            updateBarrelType(barrelType.id, newName);
        }
    }

    async function updateBarrelType(id, name) {
        try {
            const response = await fetch(`/api/v1/container/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({id, name})
            });

            if (!response.ok) new Error('Error updating barrelType');
            alert('Тип тари оновлено!');
            loadContainers();
        } catch (error) {
            console.error('Error updating barrelType:', error);
        }
    }

    async function deleteBarrelType(id) {
        try {
            const response = await fetch(`/api/v1/container/${id}`, {
                method: 'DELETE'
            });

            if (!response.ok) new Error('Error deleting barrelType');
            alert('Тип тари видалено!');
            loadContainers();
        } catch (error) {
            console.error('Error deleting barrelType:', error);
        }
    }

    document.getElementById('createBarrelTypeBtn').addEventListener('click',
        async function () {
        const name = document.getElementById('newBarrelTypeName').value;
        /*const id = document.getElementById('newRouteId').value;*/

        if (!name /*|| !id*/) {
            alert('Будь ласка, введіть всі дані для створення типу тари.');
            return;
        }

        try {
            const response = await fetch('/api/v1/container', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({name})
            });

            if (!response.ok) new Error('Error creating barrelType');
            alert('Тип тари створено!');
            document.getElementById('newBarrelTypeName').value = '';
            /*document.getElementById('newRouteId').value = '';*/
            loadContainers();
        } catch (error) {
            console.error('Error creating barrelType:', error);
        }
    });

    /*----------Storage----------*/

    setupAdminTab('showStoragesBtn', async () => {
        const list = document.getElementById('storageListContainer');
        const form = document.getElementById('addStorageContainer');
        if (list) list.style.display = 'block';
        if (form) form.style.display = 'block';
        await loadStorages();
    });

    /*----------WithdrawalReason----------*/

    setupAdminTab('showWithdrawalReasonsBtn', async () => {
        const list = document.getElementById('withdrawalReasonListContainer');
        const form = document.getElementById('addWithdrawalReasonContainer');
        if (list) list.style.display = 'block';
        if (form) form.style.display = 'block';
        await loadWithdrawalReasons();
    });

    async function loadStorages() {
        try {
            const response = await fetch('/api/v1/warehouse');
            if (!response.ok) new Error('Error fetching storages');
            const storages = await response.json();
            const storageList = document.getElementById('storageList');
            storageList.innerHTML = '';

            storages.forEach(storage => {
                const listItem = document.createElement('li');
                listItem.classList.add('storage-item');
                listItem.setAttribute('data-storage-id', storage.id);

                const storageText = document.createElement('span');
                storageText.textContent = `${storage.id} - ${storage.name} - ${storage.description}`;
                listItem.appendChild(storageText);

                const editButton = document.createElement('button');
                editButton.textContent = 'Змінити';
                editButton.onclick = () => editStorage(storage);
                listItem.appendChild(editButton);

                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'Видалити';
                deleteButton.onclick = () => deleteStorage(storage.id);
                listItem.appendChild(deleteButton);

                storageList.appendChild(listItem);
            });
        } catch (error) {
            console.error('Error loading storages:', error);
        }
    }

    function editStorage(storage) {
        const newName = prompt('Введіть нове ім’я складу:', storage.name);
        const newDescription = prompt('Введіть новий опис складу:', storage.description);
        if (newName !== null && newDescription !== null) {
            updateStorage(storage.id, newName, newDescription);
        }
    }

    async function updateStorage(id, name, description) {
        try {
            const response = await fetch(`/api/v1/warehouse/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({id, name, description})
            });

            if (!response.ok) new Error('Error updating storage');
            alert('Склад оновлено!');
            loadStorages();
        } catch (error) {
            console.error('Error updating storage:', error);
        }
    }

    async function deleteStorage(id) {
        try {
            const response = await fetch(`/api/v1/warehouse/${id}`, {
                method: 'DELETE'
            });

            if (!response.ok) new Error('Error deleting storage');
            alert('Склад видалено!');
            loadStorages();
        } catch (error) {
            console.error('Error deleting storage:', error);
        }
    }

    document.getElementById('createStorageBtn').addEventListener('click',
        async function () {
        const name = document.getElementById('newStorageName').value;
        const description = document.getElementById('newStorageDescription').value;

        if (!name) {
            alert('Будь ласка, введіть ім\'я для створення складу.');
            return;
        }

        try {
            const response = await fetch('/api/v1/warehouse', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({name, description})
            });

            if (!response.ok) new Error('Error creating storage');
            alert('Склад створено!');
            document.getElementById('newStorageName').value = '';
            document.getElementById('newStorageDescription').value = '';
            /*document.getElementById('newRouteId').value = '';*/
            loadStorages();
        } catch (error) {
            console.error('Error creating storage:', error);
        }
    });

    async function loadWithdrawalReasons() {
        try {
            const response = await fetch('/api/v1/withdrawal-reason');
            if (!response.ok) new Error('Error fetching withdrawal reasons');
            const withdrawalReasons = await response.json();
            const withdrawalReasonList = document.getElementById('withdrawalReasonList');
            withdrawalReasonList.innerHTML = '';

            withdrawalReasons.forEach(withdrawalReason => {
                const listItem = document.createElement('li');
                listItem.classList.add('withdrawalReason-item');
                listItem.setAttribute('data-withdrawalReason-id', withdrawalReason.id);

                const withdrawalReasonText = document.createElement('span');
                const purposeText = withdrawalReason.purpose === 'REMOVING' ? 'Сняття' : 
                                   withdrawalReason.purpose === 'ADDING' ? 'Пополнення' : 'Загальний';
                withdrawalReasonText.textContent = `${withdrawalReason.id} - ${withdrawalReason.name} (${purposeText})`;
                listItem.appendChild(withdrawalReasonText);

                const editButton = document.createElement('button');
                editButton.textContent = 'Змінити';
                editButton.onclick = () => editWithdrawalReason(withdrawalReason);
                listItem.appendChild(editButton);

                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'Видалити';
                deleteButton.onclick = () => deleteWithdrawalReason(withdrawalReason.id);
                listItem.appendChild(deleteButton);

                withdrawalReasonList.appendChild(listItem);
            });
        } catch (error) {
            console.error('Error loading withdrawal reasons:', error);
        }
    }

    function editWithdrawalReason(withdrawalReason) {
        const newName = prompt('Введіть нову назву причини списання:', withdrawalReason.name);
        const newPurpose = prompt('Введіть нове призначення (ADDING, REMOVING або BOTH):', withdrawalReason.purpose);
        if (newName !== null && newPurpose !== null) {
            updateWithdrawalReason(withdrawalReason.id, newName, newPurpose);
        }
    }

    async function updateWithdrawalReason(id, name, purpose) {
        try {
            const response = await fetch(`/api/v1/withdrawal-reason/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({name, purpose})
            });

            if (!response.ok) new Error('Error updating withdrawal reason');
            alert('Причину списання оновлено!');
            loadWithdrawalReasons();
        } catch (error) {
            console.error('Error updating withdrawal reason:', error);
        }
    }

    async function deleteWithdrawalReason(id) {
        try {
            const response = await fetch(`/api/v1/withdrawal-reason/${id}`, {
                method: 'DELETE'
            });

            if (!response.ok) new Error('Error deleting withdrawal reason');
            alert('Причину списання видалено!');
            loadWithdrawalReasons();
        } catch (error) {
            console.error('Error deleting withdrawal reason:', error);
        }
    }

    document.getElementById('createWithdrawalReasonBtn').addEventListener('click',
        async function () {
        const name = document.getElementById('newWithdrawalReasonName').value;
        const purpose = document.getElementById('newWithdrawalReasonPurpose').value;

        if (!name) {
            alert('Будь ласка, введіть назву для створення причини списання.');
            return;
        }

        try {
            const response = await fetch('/api/v1/withdrawal-reason', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({name, purpose})
            });

            if (!response.ok) new Error('Error creating withdrawal reason');
            alert('Причину списання створено!');
            document.getElementById('newWithdrawalReasonName').value = '';
            document.getElementById('newWithdrawalReasonPurpose').value = 'REMOVING';
            loadWithdrawalReasons();
        } catch (error) {
            console.error('Error creating withdrawal reason:', error);
        }
    });

    /*--product--*/

    const productList = document.getElementById('product-list');
    const productsTableBody = document.getElementById('products-table-body');
    const createProductForm = document.getElementById('create-product-form');
    const createProduct = document.getElementById('create-product');
    const editProductModal = document.getElementById('edit-product-modal');
    const editProductForm = document.getElementById('edit-product-form');

    const apiBaseUrl = '/api/v1/product';

    setupAdminTab('show-products', () => {
        if (productList) productList.style.display = 'block';
        if (createProduct) createProduct.style.display = 'block';
        fetchProducts();
    });

    function fetchProducts() {
        fetch(`${apiBaseUrl}`)
            .then(response => response.json())
            .then(products => {
                productsTableBody.innerHTML = '';
                products.forEach(product => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${product.id}</td>
                        <td>${product.name}</td>
                        <td>${product.usage}</td>
                        <td>
                            <button onclick="editProduct(${product.id})">Изменить</button>
                            <button onclick="deleteProduct(${product.id})">Удалить</button>
                        </td>
                    `;
                    productsTableBody.appendChild(row);
                });
            })
            .catch(error => console.error('Ошибка при получении продуктов:', error));
    }

    createProductForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const productData = {
            name: document.getElementById('create-name').value,
            usage: document.getElementById('create-usage').value
        };

        fetch(`${apiBaseUrl}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(productData)
        })
            .then(response => {
                if (!response.ok) throw new Error('Ошибка при создании продукта');
                return response.json();
            })
            .then(() => {
                createProductForm.reset();
                fetchProducts();
            })
            .catch(error => console.error('Ошибка:', error));
    });

    window.deleteProduct = function (id) {
        if (confirm('Вы уверены, что хотите удалить этот продукт?')) {
            fetch(`${apiBaseUrl}/${id}`, {
                method: 'DELETE'
            })
                .then(response => {
                    if (!response.ok) throw new Error('Ошибка при удалении продукта');
                    fetchProducts(); // Обновляем список после удаления
                })
                .catch(error => console.error('Ошибка:', error));
        }
    };

    window.editProduct = function (id) {
        fetch(`${apiBaseUrl}/${id}`)
            .then(response => response.json())
            .then(product => {
                document.getElementById('edit-id').value = product.id;
                document.getElementById('edit-name').value = product.name;
                document.getElementById('edit-usage').value = product.usage;
                editProductModal.style.display = 'flex';
            })
            .catch(error => console.error('Ошибка при получении продукта:', error));
    };

    editProductForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const id = document.getElementById('edit-id').value;
        const productData = {
            name: document.getElementById('edit-name').value,
            usage: document.getElementById('edit-usage').value
        };

        fetch(`${apiBaseUrl}/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(productData)
        })
            .then(response => {
                if (!response.ok) throw new Error('Ошибка при обновлении продукта');
                return response.json();
            })
            .then(() => {
                editProductModal.style.display = 'none';
                fetchProducts();
            })
            .catch(error => console.error('Ошибка:', error));
    });


    /*--client-product--*/

    const clientProductList = document.getElementById('client-product-list');
    const clientProductsTableBody = document.getElementById('client-products-table-body');
    const clientCreateProductForm = document.getElementById('client-create-product-form');
    const clientCreateProduct = document.getElementById('client-create-product');
    const clientEditProductModal = document.getElementById('client-edit-product-modal');
    const clientEditProductForm = document.getElementById('client-edit-product-form');

    const apiBaseClientProductUrl = '/api/v1/clientProduct';

    setupAdminTab('show-client-products', () => {
        if (clientProductList) clientProductList.style.display = 'block';
        if (clientCreateProduct) clientCreateProduct.style.display = 'block';
        fetchClientProducts();
    });

    function fetchClientProducts() {
        fetch(`${apiBaseClientProductUrl}`)
            .then(response => response.json())
            .then(clientProducts => {
                clientProductsTableBody.innerHTML = '';
                clientProducts.forEach(clientProduct => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${clientProduct.id}</td>
                        <td>${clientProduct.name}</td>
                        <td>
                            <button onclick="editClientProduct(${clientProduct.id})">Изменить</button>
                            <button onclick="deleteClientProduct(${clientProduct.id})">Удалить</button>
                        </td>
                    `;
                    clientProductsTableBody.appendChild(row);
                });
            })
            .catch(error => console.error('Ошибка при получении продуктов клиентов:', error));
    }

    clientCreateProductForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const clientProductData = {
            name: document.getElementById('client-create-name').value,
        };

        fetch(`${apiBaseClientProductUrl}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(clientProductData)
        })
            .then(response => {
                if (!response.ok) throw new Error('Ошибка при создании продукта клиента');
                return response.json();
            })
            .then(() => {
                clientCreateProductForm.reset();
                fetchClientProducts();
            })
            .catch(error => console.error('Ошибка:', error));
    });

    window.deleteClientProduct = function (id) {
        if (confirm('Вы уверены, что хотите удалить этот продукт клиента?')) {
            fetch(`${apiBaseClientProductUrl}/${id}`, {
                method: 'DELETE'
            })
                .then(response => {
                    if (!response.ok) throw new Error('Ошибка при удалении продукта клиента');
                    fetchClientProducts();
                })
                .catch(error => console.error('Ошибка:', error));
        }
    };

    window.editClientProduct = function (id) {
        fetch(`${apiBaseClientProductUrl}/${id}`)
            .then(response => response.json())
            .then(product => {
                document.getElementById('client-edit-id').value = product.id;
                document.getElementById('client-edit-name').value = product.name;
                clientEditProductModal.style.display = 'flex';
            })
            .catch(error => console.error('Ошибка при получении продукта:', error));
    };

    clientEditProductForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const id = document.getElementById('client-edit-id').value;
        const productData = {
            name: document.getElementById('client-edit-name').value
        };

        fetch(`${apiBaseClientProductUrl}/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(productData)
        })
            .then(response => {
                if (!response.ok) throw new Error('Ошибка при обновлении продукта клиента');
                return response.json();
            })
            .then(() => {
                clientEditProductForm.style.display = 'none';
                fetchClientProducts();
            })
            .catch(error => console.error('Ошибка:', error));
    });



    // Branch Permissions
    setupAdminTab('showClientTypesBtn', async () => {
        document.getElementById('client-types-list-container').style.display = 'block';
        await loadClientTypes();
    });

    setupAdminTab('showSourcesBtn', async () => {
        const container = document.getElementById('sources-list-container');
        if (container) container.style.display = 'block';
        await loadSources();
    });

    const createSourceBtn = document.getElementById('create-source-btn');
    const sourceForm = document.getElementById('source-form');
    const sourceModal = document.getElementById('create-source-modal');
    const sourceModalTitle = document.getElementById('source-modal-title');
    const sourceSubmitBtn = document.getElementById('source-submit-btn');
    const sourceNameInput = document.getElementById('source-name');
    const sourceUserSelect = document.getElementById('source-user');
    const sourceIdInput = document.getElementById('source-id');
    const sourceBody = document.getElementById('source-body');

    let sourcesUsersCache = [];

    async function loadUsersForSources() {
        try {
            const response = await fetch('/api/v1/user');
            if (!response.ok) throw new Error('Failed to load users');
            sourcesUsersCache = await response.json();
        } catch (error) {
            console.error('Error loading users for sources:', error);
        }
    }

    async function loadSources() {
        try {
            await loadUsersForSources();
            const response = await fetch('/api/v1/source');
            if (!response.ok) throw new Error('Failed to load sources');
            const sources = await response.json();
            renderSources(sources);
        } catch (error) {
            console.error('Error loading sources:', error);
        }
    }

    function renderSources(sources) {
        if (!sourceBody) return;
        sourceBody.innerHTML = '';
        sources.forEach(source => {
            const row = document.createElement('tr');
            const userName = source.userId 
                ? (sourcesUsersCache.find(u => u.id === source.userId)?.fullName || sourcesUsersCache.find(u => u.id === source.userId)?.name || `User ${source.userId}`)
                : 'Не закріплено';
            row.innerHTML = `
                <td>${source.name}</td>
                <td>${userName}</td>
                <td>
                    <button onclick="editSource(${source.id})" class="btn-edit">Редагувати</button>
                    <button onclick="deleteSource(${source.id})" class="btn-delete">Видалити</button>
                </td>
            `;
            sourceBody.appendChild(row);
        });
    }

    if (createSourceBtn) {
        createSourceBtn.addEventListener('click', async () => {
            sourceIdInput.value = '';
            sourceModalTitle.textContent = 'Створити джерело';
            sourceSubmitBtn.textContent = 'Створити';
            sourceNameInput.value = '';
            await loadUsersForSources();
            sourceUserSelect.innerHTML = '<option value="">Не закріплено</option>';
            sourcesUsersCache.forEach(user => {
                const option = document.createElement('option');
                option.value = user.id;
                option.textContent = user.fullName || user.name;
                sourceUserSelect.appendChild(option);
            });
            sourceUserSelect.value = '';
            sourceModal.style.display = 'flex';
        });
    }

    if (sourceForm) {
        sourceForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const sourceId = sourceIdInput.value;
            const formData = {
                name: sourceNameInput.value,
                userId: sourceUserSelect.value ? parseInt(sourceUserSelect.value) : null
            };

            try {
                const url = sourceId 
                    ? `/api/v1/source/${sourceId}`
                    : '/api/v1/source';
                const method = sourceId ? 'PUT' : 'POST';
                
                const response = await fetch(url, {
                    method: method,
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(formData)
                });
                if (!response.ok) {
                    const error = await response.json();
                    throw new Error(error.message || `Failed to ${sourceId ? 'update' : 'create'} source`);
                }
                sourceModal.style.display = 'none';
                sourceForm.reset();
                sourceIdInput.value = '';
                await loadSources();
            } catch (error) {
                console.error(`Error ${sourceId ? 'updating' : 'creating'} source:`, error);
                alert(error.message || `Помилка ${sourceId ? 'оновлення' : 'створення'} джерела`);
            }
        });
    }

    window.editSource = async function(id) {
        try {
            const response = await fetch(`/api/v1/source/${id}`);
            if (!response.ok) throw new Error('Failed to load source');
            const source = await response.json();
            
            sourceIdInput.value = source.id;
            sourceNameInput.value = source.name;
            sourceModalTitle.textContent = 'Редагувати джерело';
            sourceSubmitBtn.textContent = 'Зберегти';
            
            await loadUsersForSources();
            sourceUserSelect.innerHTML = '<option value="">Не закріплено</option>';
            sourcesUsersCache.forEach(user => {
                const option = document.createElement('option');
                option.value = user.id;
                option.textContent = user.fullName || user.name;
                sourceUserSelect.appendChild(option);
            });
            sourceUserSelect.value = source.userId || '';
            
            sourceModal.style.display = 'flex';
        } catch (error) {
            console.error('Error loading source:', error);
            alert('Помилка завантаження джерела');
        }
    };

    window.deleteSource = async function(id) {
        if (!confirm('Ви впевнені, що хочете видалити це джерело? Ця дія незворотна.')) return;
        
        try {
            const response = await fetch(`/api/v1/source/${id}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to delete source');
            }
            await loadSources();
        } catch (error) {
            console.error('Error deleting source:', error);
            alert(error.message || 'Помилка видалення джерела');
        }
    };

    const closeSourceModal = sourceModal?.querySelector('.close-modal');
    if (closeSourceModal) {
        closeSourceModal.addEventListener('click', () => {
            sourceModal.style.display = 'none';
        });
    }

    if (sourceModal) {
        sourceModal.addEventListener('click', (e) => {
            if (e.target === sourceModal) {
                sourceModal.style.display = 'none';
            }
        });
    }

    setupAdminTab('showBranchPermissionsBtn', async () => {
        const container = document.getElementById('branchPermissionsContainer');
        if (container) container.style.display = 'block';
        await initializeBranchPermissionsSection();
    });

    document.getElementById('add-branch-permission-btn').addEventListener('click', function () {
        openBranchPermissionModal();
    });

    document.getElementById('branch-permission-form').addEventListener('submit', handleBranchPermissionSubmit);

    document.getElementById('branch-permissions-user-filter').addEventListener('change', loadBranchPermissions);
    document.getElementById('branch-permissions-branch-filter').addEventListener('change', loadBranchPermissions);

    // Close modal on X click
    document.querySelectorAll('#branch-permission-modal .close').forEach(closeBtn => {
        closeBtn.addEventListener('click', () => {
            document.getElementById('branch-permission-modal').style.display = 'none';
        });
    });

    // Close modal on outside click
    window.addEventListener('click', (e) => {
        if (e.target.id === 'branch-permission-modal') {
            e.target.style.display = 'none';
        }
    });
});

// ========== BRANCH PERMISSIONS ==========

let usersCacheForPermissions = [];
let branchesCacheForPermissions = [];
let branchPermissionsCache = [];

async function initializeBranchPermissionsSection() {
    await loadUsersForPermissions();
    await loadBranchesForPermissions();
    await loadBranchPermissions();
}

async function loadUsersForPermissions() {
    try {
        const response = await fetch('/api/v1/user');
        if (!response.ok) throw new Error('Failed to load users');
        usersCacheForPermissions = await response.json();
        
        const userFilter = document.getElementById('branch-permissions-user-filter');
        const userSelect = document.getElementById('branch-permission-user');
        
        // Clear existing options except first
        userFilter.innerHTML = '<option value="">Всі користувачі</option>';
        userSelect.innerHTML = '<option value="">Виберіть користувача</option>';
        
        usersCacheForPermissions.forEach(user => {
            const option1 = document.createElement('option');
            option1.value = user.id;
            option1.textContent = user.name || user.fullName || user.login || `User ${user.id}`;
            userFilter.appendChild(option1);
            
            const option2 = document.createElement('option');
            option2.value = user.id;
            option2.textContent = user.name || user.fullName || user.login || `User ${user.id}`;
            userSelect.appendChild(option2);
        });
    } catch (error) {
        console.error('Error loading users:', error);
        showMessage('Помилка завантаження користувачів', 'error');
    }
}

async function loadBranchesForPermissions() {
    try {
        const response = await fetch('/api/v1/branches/all');
        if (!response.ok) throw new Error('Failed to load branches');
        branchesCacheForPermissions = await response.json();
        
        const branchFilter = document.getElementById('branch-permissions-branch-filter');
        const branchSelect = document.getElementById('branch-permission-branch');
        
        // Clear existing options except first
        branchFilter.innerHTML = '<option value="">Всі філії</option>';
        branchSelect.innerHTML = '<option value="">Виберіть філію</option>';
        
        branchesCacheForPermissions.forEach(branch => {
            const option1 = document.createElement('option');
            option1.value = branch.id;
            option1.textContent = branch.name;
            branchFilter.appendChild(option1);
            
            const option2 = document.createElement('option');
            option2.value = branch.id;
            option2.textContent = branch.name;
            branchSelect.appendChild(option2);
        });
    } catch (error) {
        console.error('Error loading branches:', error);
        showMessage('Помилка завантаження філій', 'error');
    }
}

async function loadBranchPermissions() {
    try {
        const userId = document.getElementById('branch-permissions-user-filter').value;
        const branchId = document.getElementById('branch-permissions-branch-filter').value;
        
        let permissions = [];
        
        if (userId) {
            const response = await fetch(`/api/v1/branch-permissions/user/${userId}`);
            if (!response.ok) throw new Error('Failed to load permissions');
            permissions = await response.json();
        } else if (branchId) {
            const response = await fetch(`/api/v1/branch-permissions/branch/${branchId}`);
            if (!response.ok) throw new Error('Failed to load permissions');
            permissions = await response.json();
        } else {
            // Load all permissions by loading for each user
            const allPermissions = [];
            for (const user of usersCacheForPermissions) {
                try {
                    const response = await fetch(`/api/v1/branch-permissions/user/${user.id}`);
                    if (response.ok) {
                        const userPermissions = await response.json();
                        allPermissions.push(...userPermissions);
                    }
                } catch (error) {
                    console.error(`Error loading permissions for user ${user.id}:`, error);
                }
            }
            permissions = allPermissions;
        }
        
        branchPermissionsCache = permissions;
        renderBranchPermissions(permissions);
    } catch (error) {
        console.error('Error loading branch permissions:', error);
        showMessage('Помилка завантаження прав доступу', 'error');
    }
}

function renderBranchPermissions(permissions) {
    const tbody = document.getElementById('branch-permissions-body');
    tbody.innerHTML = '';
    
    permissions.forEach(permission => {
        const user = usersCacheForPermissions.find(u => u.id === permission.userId);
        const branch = branchesCacheForPermissions.find(b => b.id === permission.branchId);
        
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${user ? (user.name || user.fullName || user.login || `User ${permission.userId}`) : `User ${permission.userId}`}</td>
            <td>${branch ? branch.name : `Branch ${permission.branchId}`}</td>
            <td><span class="status-badge ${permission.canView ? 'status-active' : 'status-inactive'}">${permission.canView ? 'Так' : 'Ні'}</span></td>
            <td><span class="status-badge ${permission.canOperate ? 'status-active' : 'status-inactive'}">${permission.canOperate ? 'Так' : 'Ні'}</span></td>
            <td>
                <div class="action-buttons-table">
                    <button class="action-btn btn-edit" onclick="editBranchPermission(${permission.id})">Редагувати</button>
                    <button class="action-btn btn-delete" onclick="deleteBranchPermission(${permission.userId}, ${permission.branchId})">Видалити</button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function openBranchPermissionModal() {
    document.getElementById('branch-permission-id').value = '';
    document.getElementById('branch-permission-modal-title').textContent = 'Додати права доступу';
    document.getElementById('branch-permission-submit-btn').textContent = 'Додати';
    document.getElementById('branch-permission-user').value = '';
    document.getElementById('branch-permission-branch').value = '';
    document.getElementById('branch-permission-can-view').checked = true;
    document.getElementById('branch-permission-can-operate').checked = false;
    document.getElementById('branch-permission-modal').style.display = 'block';
}

async function editBranchPermission(permissionId) {
    try {
        // Find permission in cache
        const permission = branchPermissionsCache.find(p => p.id === permissionId);
        if (!permission) {
            showMessage('Права доступу не знайдено', 'error');
            return;
        }
        
        document.getElementById('branch-permission-id').value = permission.id;
        document.getElementById('branch-permission-user').value = permission.userId;
        document.getElementById('branch-permission-branch').value = permission.branchId;
        document.getElementById('branch-permission-can-view').checked = permission.canView;
        document.getElementById('branch-permission-can-operate').checked = permission.canOperate;
        document.getElementById('branch-permission-modal-title').textContent = 'Редагувати права доступу';
        document.getElementById('branch-permission-submit-btn').textContent = 'Зберегти';
        
        document.getElementById('branch-permission-modal').style.display = 'block';
    } catch (error) {
        console.error('Error loading permission:', error);
        showMessage('Помилка завантаження прав доступу', 'error');
    }
}

async function deleteBranchPermission(userId, branchId) {
    if (!confirm('Ви впевнені, що хочете видалити ці права доступу? Ця дія незворотна.')) return;
    
    try {
        const response = await fetch(`/api/v1/branch-permissions/user/${userId}/branch/${branchId}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to delete permission');
        }
        showMessage('Права доступу успішно видалено', 'success');
        loadBranchPermissions();
    } catch (error) {
        console.error('Error deleting permission:', error);
        showMessage(error.message || 'Помилка видалення прав доступу', 'error');
    }
}

async function handleBranchPermissionSubmit(e) {
    e.preventDefault();
    const permissionId = document.getElementById('branch-permission-id').value;
    const formData = {
        userId: parseInt(document.getElementById('branch-permission-user').value),
        branchId: parseInt(document.getElementById('branch-permission-branch').value),
        canView: document.getElementById('branch-permission-can-view').checked,
        canOperate: document.getElementById('branch-permission-can-operate').checked
    };

    try {
        const response = await fetch('/api/v1/branch-permissions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || `Failed to ${permissionId ? 'update' : 'create'} permission`);
        }
        showMessage(`Права доступу успішно ${permissionId ? 'оновлено' : 'створено'}`, 'success');
        document.getElementById('branch-permission-modal').style.display = 'none';
        document.getElementById('branch-permission-form').reset();
        document.getElementById('branch-permission-id').value = '';
        loadBranchPermissions();
    } catch (error) {
        console.error(`Error ${permissionId ? 'updating' : 'creating'} permission:`, error);
        showMessage(error.message || `Помилка ${permissionId ? 'оновлення' : 'створення'} прав доступу`, 'error');
    }
}

function showMessage(message, type = 'info') {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message-box ${type}`;
    messageDiv.textContent = message;
    document.body.appendChild(messageDiv);

    if (type === 'error') {
        const closeButton = document.createElement('span');
        closeButton.className = 'message-close';
        closeButton.innerHTML = '×';
        closeButton.addEventListener('click', () => {
            messageDiv.classList.add('fade-out');
            setTimeout(() => {
                document.body.removeChild(messageDiv);
            }, 200);
        });
        messageDiv.appendChild(closeButton);
    } else {
        setTimeout(() => {
            messageDiv.classList.add('fade-out');
            setTimeout(() => {
                document.body.removeChild(messageDiv);
            }, 500);
        }, 1000);
    }
}

let currentClientTypeId = null;

async function loadClientTypes() {
    try {
        const response = await fetch('/api/v1/client-type?page=0&size=1000');
        if (!response.ok) throw new Error('Failed to load client types');
        const data = await response.json();
        renderClientTypes(data.content || data);
    } catch (error) {
        console.error('Error loading client types:', error);
        showMessage('Помилка завантаження типів клієнтів', 'error');
    }
}

function renderClientTypes(clientTypes) {
    const tbody = document.getElementById('client-types-table-body');
    tbody.innerHTML = '';
    clientTypes.forEach(type => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${type.id}</td>
            <td>${type.name}</td>
            <td>${type.nameFieldLabel}</td>
            <td>${type.isActive ? 'Так' : 'Ні'}</td>
            <td>
                <button class="btn-activate" onclick="openEditClientTypeModal(${type.id})">Редагувати</button>
                <button class="btn-activate" onclick="manageClientTypeFields(${type.id})">Поля</button>
                <button class="btn-activate" onclick="downloadClientImportTemplate(${type.id})">Скачати шаблон</button>
                <button class="btn-activate" onclick="openClientImportModal(${type.id})">Імпортувати клієнтів</button>
                <button class="btn-deactivate" onclick="deleteClientType(${type.id})">Видалити</button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Client Import Functions
function downloadClientImportTemplate(clientTypeId) {
    window.location.href = `/api/v1/client/import/template/${clientTypeId}`;
}

function openClientImportModal(clientTypeId) {
    document.getElementById('import-client-type-id').value = clientTypeId;
    document.getElementById('client-import-modal').style.display = 'flex';
}

document.getElementById('client-import-form')?.addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const clientTypeId = document.getElementById('import-client-type-id').value;
    const fileInput = document.getElementById('import-file');
    const file = fileInput.files[0];
    
    if (!file) {
        alert('Будь ласка, виберіть файл');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', file);
    
    try {
        loaderBackdrop.style.display = 'flex';
        
        const response = await fetch(`/api/v1/client/import/${clientTypeId}`, {
            method: 'POST',
            body: formData,
            credentials: 'include'
        });
        
        if (!response.ok) {
            let errorMessage = 'Помилка імпорту';
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                try {
                    const errorData = await response.json();
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                    if (errorData.details && errorData.details.error) {
                        errorMessage += '\n\n' + errorData.details.error;
                    } else if (errorData.details) {
                        // Если details - это объект с несколькими полями
                        const detailsText = Object.entries(errorData.details)
                            .map(([key, value]) => `${key}: ${value}`)
                            .join('\n');
                        if (detailsText) {
                            errorMessage += '\n\n' + detailsText;
                        }
                    }
                } catch (e) {
                    console.error('Failed to parse error JSON:', e);
                }
            } else {
                // Если не JSON, пробуем получить текст
                try {
                    const errorText = await response.text();
                    if (errorText) {
                        errorMessage = errorText;
                    }
                } catch (e) {
                    console.error('Failed to read error text:', e);
                }
            }
            throw new Error(errorMessage);
        }
        
        const result = await response.text();
        alert(result);
        
        // Закрываем модальное окно и очищаем форму
        document.getElementById('client-import-modal').style.display = 'none';
        document.getElementById('client-import-form').reset();
        
    } catch (error) {
        console.error('Import error:', error);
        alert('Помилка імпорту:\n\n' + error.message);
    } finally {
        loaderBackdrop.style.display = 'none';
    }
});

// Close modal handlers
document.querySelectorAll('#client-import-modal .close-modal').forEach(btn => {
    btn.addEventListener('click', () => {
        document.getElementById('client-import-modal').style.display = 'none';
        document.getElementById('client-import-form').reset();
    });
});

document.getElementById('addClientTypeBtn')?.addEventListener('click', () => {
    document.getElementById('add-client-type-modal').style.display = 'flex';
});

document.getElementById('create-client-type-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = {
        name: document.getElementById('client-type-name').value,
        nameFieldLabel: document.getElementById('client-type-name-field-label').value || 'Компанія'
    };
    try {
        const response = await fetch('/api/v1/client-type', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to create client type');
        }
        showMessage('Тип клієнта успішно створено', 'success');
        document.getElementById('add-client-type-modal').style.display = 'none';
        document.getElementById('create-client-type-form').reset();
        loadClientTypes();
    } catch (error) {
        console.error('Error creating client type:', error);
        showMessage(error.message || 'Помилка створення типу клієнта', 'error');
    }
});

async function openEditClientTypeModal(id) {
    try {
        const response = await fetch(`/api/v1/client-type/${id}`);
        if (!response.ok) throw new Error('Failed to load client type');
        const type = await response.json();
        document.getElementById('edit-client-type-id').value = type.id;
        document.getElementById('edit-client-type-name').value = type.name;
        document.getElementById('edit-client-type-name-field-label').value = type.nameFieldLabel;
        document.getElementById('edit-client-type-active').checked = type.isActive;
        document.getElementById('edit-client-type-modal').style.display = 'flex';
    } catch (error) {
        console.error('Error loading client type:', error);
        showMessage('Помилка завантаження типу клієнта', 'error');
    }
}

document.getElementById('edit-client-type-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('edit-client-type-id').value;
    const formData = {
        name: document.getElementById('edit-client-type-name').value,
        nameFieldLabel: document.getElementById('edit-client-type-name-field-label').value,
        isActive: document.getElementById('edit-client-type-active').checked
    };
    try {
        const response = await fetch(`/api/v1/client-type/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to update client type');
        }
        showMessage('Тип клієнта успішно оновлено', 'success');
        document.getElementById('edit-client-type-modal').style.display = 'none';
        loadClientTypes();
    } catch (error) {
        console.error('Error updating client type:', error);
        showMessage(error.message || 'Помилка оновлення типу клієнта', 'error');
    }
});

async function deleteClientType(id) {
    if (!confirm('Ви впевнені, що хочете видалити цей тип клієнта?')) return;
    try {
        const response = await fetch(`/api/v1/client-type/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to delete client type');
        }
        showMessage('Тип клієнта успішно видалено', 'success');
        loadClientTypes();
    } catch (error) {
        console.error('Error deleting client type:', error);
        showMessage(error.message || 'Помилка видалення типу клієнта', 'error');
    }
}

async function manageClientTypeFields(clientTypeId) {
    currentClientTypeId = clientTypeId;
    document.getElementById('manage-fields-modal').style.display = 'flex';
    await loadClientTypeFields(clientTypeId);
    
    // Убеждаемся, что обработчик для кнопки настроен после открытия модального окна
    const configureBtn = document.getElementById('configureStaticFieldsBtn');
    if (configureBtn) {
        // Удаляем старые обработчики, если они есть
        const newBtn = configureBtn.cloneNode(true);
        configureBtn.parentNode.replaceChild(newBtn, configureBtn);
        
        // Добавляем прямой обработчик
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
}

async function loadClientTypeFields(clientTypeId) {
    try {
        const response = await fetch(`/api/v1/client-type/${clientTypeId}/field`);
        if (!response.ok) throw new Error('Failed to load fields');
        const fields = await response.json();
        renderFields(fields);
    } catch (error) {
        console.error('Error loading fields:', error);
        showMessage('Помилка завантаження полів', 'error');
    }
}

function renderFields(fields) {
    const tbody = document.getElementById('fields-table-body');
    tbody.innerHTML = '';
    fields.forEach(field => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${field.fieldLabel}</td>
            <td>${getFieldTypeLabel(field.fieldType)}</td>
            <td>${field.isRequired ? 'Так' : 'Ні'}</td>
            <td>${field.isSearchable ? 'Так' : 'Ні'}</td>
            <td>${field.isFilterable ? 'Так' : 'Ні'}</td>
            <td>${field.isVisibleInTable ? 'Так' : 'Ні'}</td>
            <td>${field.isVisibleInCreate ? 'Так' : 'Ні'}</td>
            <td>${field.displayOrder}</td>
            <td>
                <button class="btn-activate" onclick="openEditFieldModal(${field.id})">Редагувати</button>
                <button class="btn-deactivate" onclick="deleteField(${field.id})">Видалити</button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function getFieldTypeLabel(type) {
    const labels = {
        'TEXT': 'Текст',
        'NUMBER': 'Число',
        'DATE': 'Дата',
        'PHONE': 'Телефон',
        'LIST': 'Список',
        'BOOLEAN': 'Так/Ні'
    };
    return labels[type] || type;
}

document.getElementById('addFieldBtn')?.addEventListener('click', () => {
    document.getElementById('field-client-type-id').value = currentClientTypeId;
    document.getElementById('add-field-modal').style.display = 'flex';
});

document.getElementById('field-type')?.addEventListener('change', (e) => {
    const type = e.target.value;
    const validationGroup = document.getElementById('field-validation-pattern-group');
    const listValuesGroup = document.getElementById('field-list-values-group');
    if (validationGroup) validationGroup.style.display = type === 'PHONE' ? 'block' : 'none';
    if (listValuesGroup) listValuesGroup.style.display = type === 'LIST' ? 'block' : 'none';
});

document.getElementById('create-field-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const listValuesText = document.getElementById('field-list-values').value;
    const formData = {
        fieldName: document.getElementById('field-name').value,
        fieldLabel: document.getElementById('field-label').value,
        fieldType: document.getElementById('field-type').value,
        isRequired: document.getElementById('field-required').checked,
        isSearchable: document.getElementById('field-searchable').checked,
        isFilterable: document.getElementById('field-filterable').checked,
        isVisibleInTable: document.getElementById('field-visible').checked,
        isVisibleInCreate: document.getElementById('field-visible-in-create').checked,
        columnWidth: document.getElementById('field-column-width').value ? parseInt(document.getElementById('field-column-width').value) : null,
        validationPattern: document.getElementById('field-validation-pattern').value || null,
        allowMultiple: document.getElementById('field-allow-multiple').checked,
        listValues: listValuesText ? listValuesText.split('\n').filter(v => v.trim()).map(v => v.trim()) : []
    };
    try {
        const response = await fetch(`/api/v1/client-type/${currentClientTypeId}/field`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to create field');
        }
        showMessage('Поле успішно створено', 'success');
        document.getElementById('add-field-modal').style.display = 'none';
        document.getElementById('create-field-form').reset();
        loadClientTypeFields(currentClientTypeId);
    } catch (error) {
        console.error('Error creating field:', error);
        showMessage(error.message || 'Помилка створення поля', 'error');
    }
});

async function openEditFieldModal(fieldId) {
    try {
        // Очищаем все поля формы перед заполнением
        document.getElementById('edit-field-id').value = '';
        document.getElementById('edit-field-label').value = '';
        document.getElementById('edit-field-required').checked = false;
        document.getElementById('edit-field-searchable').checked = false;
        document.getElementById('edit-field-filterable').checked = false;
        document.getElementById('edit-field-visible').checked = false;
        document.getElementById('edit-field-visible-in-create').checked = false;
        document.getElementById('edit-field-display-order').value = '';
        document.getElementById('edit-field-column-width').value = '';
        document.getElementById('edit-field-validation-pattern').value = '';
        document.getElementById('edit-field-allow-multiple').checked = false;
        document.getElementById('edit-field-list-values').value = '';
        
        // Скрываем группы полей, которые могут быть видны от предыдущего поля
        document.getElementById('edit-field-list-values-group').style.display = 'none';
        document.getElementById('edit-field-validation-pattern-group').style.display = 'none';
        
        // Загружаем данные поля
        const response = await fetch(`/api/v1/client-type/field/${fieldId}`);
        if (!response.ok) throw new Error('Failed to load field');
        const field = await response.json();
        
        // Заполняем форму данными поля
        document.getElementById('edit-field-id').value = field.id;
        document.getElementById('edit-field-label').value = field.fieldLabel || '';
        document.getElementById('edit-field-required').checked = field.isRequired || false;
        document.getElementById('edit-field-searchable').checked = field.isSearchable || false;
        document.getElementById('edit-field-filterable').checked = field.isFilterable || false;
        document.getElementById('edit-field-visible').checked = field.isVisibleInTable || false;
        document.getElementById('edit-field-visible-in-create').checked = field.isVisibleInCreate || false;
        document.getElementById('edit-field-display-order').value = field.displayOrder || '';
        document.getElementById('edit-field-column-width').value = field.columnWidth || '';
        document.getElementById('edit-field-validation-pattern').value = field.validationPattern || '';
        document.getElementById('edit-field-allow-multiple').checked = field.allowMultiple || false;
        
        // Показываем и заполняем список значений, если поле типа LIST
        if (field.listValues && field.listValues.length > 0) {
            document.getElementById('edit-field-list-values').value = field.listValues.map(lv => lv.value).join('\n');
            document.getElementById('edit-field-list-values-group').style.display = 'block';
        }
        
        // Показываем поле валидации для типа PHONE
        if (field.fieldType === 'PHONE') {
            document.getElementById('edit-field-validation-pattern-group').style.display = 'block';
        }
        
        document.getElementById('edit-field-modal').style.display = 'flex';
    } catch (error) {
        console.error('Error loading field:', error);
        showMessage('Помилка завантаження поля', 'error');
    }
}

document.getElementById('edit-field-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fieldId = document.getElementById('edit-field-id').value;
    const listValuesText = document.getElementById('edit-field-list-values').value;
    const formData = {
        fieldLabel: document.getElementById('edit-field-label').value,
        isRequired: document.getElementById('edit-field-required').checked,
        isSearchable: document.getElementById('edit-field-searchable').checked,
        isFilterable: document.getElementById('edit-field-filterable').checked,
        isVisibleInTable: document.getElementById('edit-field-visible').checked,
        isVisibleInCreate: document.getElementById('edit-field-visible-in-create').checked,
        displayOrder: parseInt(document.getElementById('edit-field-display-order').value) || 0,
        columnWidth: document.getElementById('edit-field-column-width').value ? parseInt(document.getElementById('edit-field-column-width').value) : null,
        validationPattern: document.getElementById('edit-field-validation-pattern').value || null,
        allowMultiple: document.getElementById('edit-field-allow-multiple').checked,
        listValues: listValuesText ? listValuesText.split('\n').filter(v => v.trim()).map(v => v.trim()) : []
    };
    try {
        const response = await fetch(`/api/v1/client-type/field/${fieldId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to update field');
        }
        showMessage('Поле успішно оновлено', 'success');
        document.getElementById('edit-field-modal').style.display = 'none';
        loadClientTypeFields(currentClientTypeId);
    } catch (error) {
        console.error('Error updating field:', error);
        showMessage(error.message || 'Помилка оновлення поля', 'error');
    }
});

async function deleteField(fieldId) {
    if (!confirm('Ви впевнені, що хочете видалити це поле?')) return;
    try {
        const response = await fetch(`/api/v1/client-type/field/${fieldId}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to delete field');
        }
        showMessage('Поле успішно видалено', 'success');
        loadClientTypeFields(currentClientTypeId);
    } catch (error) {
        console.error('Error deleting field:', error);
        showMessage(error.message || 'Помилка видалення поля', 'error');
    }
}

// Обработчик закрытия модальных окон по клику на крестик
document.querySelectorAll('.close-modal').forEach(btn => {
    btn.addEventListener('click', function(e) {
        e.stopPropagation();
        const modal = this.closest('.modal');
        if (modal) {
            modal.style.display = 'none';
        }
    });
});

// Static Fields Configuration - используем делегирование событий с более высоким приоритетом
document.addEventListener('click', async (e) => {
    // Проверяем, что клик был именно на кнопке или внутри неё
    const btn = e.target.closest('#configureStaticFieldsBtn');
    if (btn) {
        e.preventDefault();
        e.stopPropagation();
        e.stopImmediatePropagation();
        if (!currentClientTypeId) {
            showMessage('Спочатку виберіть тип клієнта', 'error');
            return;
        }
        await openStaticFieldsConfigModal(currentClientTypeId);
        return false;
    }
}, true); // Используем capture phase для более раннего перехвата

// Обработчик закрытия модальных окон по клику вне области
document.querySelectorAll('.modal').forEach(modal => {
    modal.addEventListener('click', function(e) {
        // Закрываем модальное окно, если клик был по фону (не по содержимому)
        if (e.target === this) {
            this.style.display = 'none';
        }
    });
    
    // Предотвращаем закрытие при клике на содержимое модального окна
    const modalContent = modal.querySelector('.modal-content');
    if (modalContent) {
        modalContent.addEventListener('click', function(e) {
            // Не останавливаем распространение для кнопок внутри модального окна
            // Это позволяет кликам на кнопки обрабатываться до того, как событие будет остановлено
            if (!e.target.closest('button')) {
                e.stopPropagation();
            }
        });
    }
});

async function openStaticFieldsConfigModal(clientTypeId) {
    document.getElementById('static-fields-client-type-id').value = clientTypeId;
    const modal = document.getElementById('static-fields-config-modal');
    modal.style.display = 'flex';
    
    try {
        const response = await fetch(`/api/v1/client-type/${clientTypeId}/static-fields-config`);
        if (!response.ok) throw new Error('Failed to load static fields config');
        const config = await response.json();
        
        // Заполняем форму текущими настройками
        if (config.company) {
            document.getElementById('static-company-visible').checked = config.company.isVisible || false;
            document.getElementById('static-company-label').value = config.company.fieldLabel || 'Компанія';
            document.getElementById('static-company-order').value = config.company.displayOrder || 0;
            document.getElementById('static-company-width').value = config.company.columnWidth || 200;
        }
        
        if (config.source) {
            document.getElementById('static-source-visible').checked = config.source.isVisible || false;
            document.getElementById('static-source-label').value = config.source.fieldLabel || 'Залучення';
            document.getElementById('static-source-order').value = config.source.displayOrder || 999;
            document.getElementById('static-source-width').value = config.source.columnWidth || 200;
        }
        
        if (config.createdAt) {
            document.getElementById('static-createdAt-visible').checked = config.createdAt.isVisible || false;
            document.getElementById('static-createdAt-label').value = config.createdAt.fieldLabel || 'Створено';
            document.getElementById('static-createdAt-order').value = config.createdAt.displayOrder || 1000;
            document.getElementById('static-createdAt-width').value = config.createdAt.columnWidth || 150;
        }
        
        if (config.updatedAt) {
            document.getElementById('static-updatedAt-visible').checked = config.updatedAt.isVisible || false;
            document.getElementById('static-updatedAt-label').value = config.updatedAt.fieldLabel || 'Оновлено';
            document.getElementById('static-updatedAt-order').value = config.updatedAt.displayOrder || 1001;
            document.getElementById('static-updatedAt-width').value = config.updatedAt.columnWidth || 150;
        }
    } catch (error) {
        console.error('Error loading static fields config:', error);
        showMessage('Помилка завантаження налаштувань', 'error');
    }
}

document.getElementById('static-fields-config-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const clientTypeId = document.getElementById('static-fields-client-type-id').value;
    
    // Вспомогательная функция для безопасного парсинга чисел
    const safeParseInt = (value, defaultValue) => {
        const parsed = parseInt(value);
        return isNaN(parsed) ? defaultValue : parsed;
    };
    
    const config = {
        company: {
            isVisible: document.getElementById('static-company-visible').checked,
            fieldLabel: document.getElementById('static-company-label').value.trim() || 'Компанія',
            displayOrder: safeParseInt(document.getElementById('static-company-order').value, 0),
            columnWidth: safeParseInt(document.getElementById('static-company-width').value, 200)
        },
        source: {
            isVisible: document.getElementById('static-source-visible').checked,
            fieldLabel: document.getElementById('static-source-label').value.trim() || 'Залучення',
            displayOrder: safeParseInt(document.getElementById('static-source-order').value, 999),
            columnWidth: safeParseInt(document.getElementById('static-source-width').value, 200)
        },
        createdAt: {
            isVisible: document.getElementById('static-createdAt-visible').checked,
            fieldLabel: document.getElementById('static-createdAt-label').value.trim() || 'Створено',
            displayOrder: safeParseInt(document.getElementById('static-createdAt-order').value, 1000),
            columnWidth: safeParseInt(document.getElementById('static-createdAt-width').value, 150)
        },
        updatedAt: {
            isVisible: document.getElementById('static-updatedAt-visible').checked,
            fieldLabel: document.getElementById('static-updatedAt-label').value.trim() || 'Оновлено',
            displayOrder: safeParseInt(document.getElementById('static-updatedAt-order').value, 1001),
            columnWidth: safeParseInt(document.getElementById('static-updatedAt-width').value, 150)
        }
    };
    
    try {
        const response = await fetch(`/api/v1/client-type/${clientTypeId}/static-fields-config`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(config)
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to save static fields config');
        }
        
        showMessage('Налаштування статичних полей збережено', 'success');
        document.getElementById('static-fields-config-modal').style.display = 'none';
        
        // Перезагружаем видимые поля, если мы на странице клиентов
        if (typeof loadClientTypeFields === 'function' && currentClientTypeId) {
            // Небольшая задержка, чтобы убедиться, что изменения сохранены
            setTimeout(async () => {
                try {
                    const [fieldsRes, visibleRes] = await Promise.all([
                        fetch(`/api/v1/client-type/${currentClientTypeId}/field`),
                        fetch(`/api/v1/client-type/${currentClientTypeId}/field/visible`)
                    ]);
                    if (visibleRes.ok) {
                        visibleFields = await visibleRes.json();
                        window.visibleFields = visibleFields;
                        // Перестраиваем таблицу, если она существует
                        if (typeof buildDynamicTable === 'function') {
                            buildDynamicTable();
                        }
                    }
                } catch (error) {
                    console.warn('Failed to reload visible fields after config update:', error);
                }
            }, 500);
        }
    } catch (error) {
        console.error('Error saving static fields config:', error);
        showMessage(error.message || 'Помилка збереження налаштувань', 'error');
    }
});

window.openEditClientTypeModal = openEditClientTypeModal;
window.manageClientTypeFields = manageClientTypeFields;
window.openEditFieldModal = openEditFieldModal;
window.deleteField = deleteField;
window.deleteClientType = deleteClientType;
window.downloadClientImportTemplate = downloadClientImportTemplate;
window.openClientImportModal = openClientImportModal;

// Client Type Permissions Management
let currentUserForClientTypePermissions = null;
let allClientTypes = [];

async function showUserClientTypePermissionsModal(user) {
    currentUserForClientTypePermissions = user;
    const modal = document.getElementById('userClientTypePermissionsModal');
    const userNameElement = document.getElementById('clientTypePermissionsUserName');
    
    userNameElement.textContent = `Користувач: ${user.fullName} (${user.login})`;
    modal.style.display = 'flex';
    
    await loadClientTypePermissions(user.id);
}

async function loadClientTypePermissions(userId) {
    try {
        // Загружаем все типы клиентов (используем /active для получения списка, а не PageResponse)
        const clientTypesResponse = await fetch('/api/v1/client-type/active');
        if (!clientTypesResponse.ok) throw new Error('Failed to load client types');
        const clientTypesData = await clientTypesResponse.json();
        
        // Проверяем, является ли ответ массивом или объектом с content
        if (Array.isArray(clientTypesData)) {
            allClientTypes = clientTypesData;
        } else if (clientTypesData && Array.isArray(clientTypesData.content)) {
            // Если это PageResponse, извлекаем content
            allClientTypes = clientTypesData.content;
        } else {
            throw new Error('Invalid response format from client types API');
        }
        
        // Загружаем права доступа пользователя
        const permissionsResponse = await fetch(`/api/v1/client-type/permission/user/${userId}`);
        if (!permissionsResponse.ok) throw new Error('Failed to load permissions');
        const userPermissions = await permissionsResponse.json();
        
        // Создаем мапу прав доступа по clientTypeId
        const permissionsMap = new Map();
        userPermissions.forEach(perm => {
            permissionsMap.set(perm.clientTypeId, perm);
        });
        
        const permissionsList = document.getElementById('clientTypePermissionsList');
        permissionsList.innerHTML = '';
        
        // Создаем таблицу для отображения прав доступа
        const table = document.createElement('table');
        table.className = 'data-table';
        table.style.width = '100%';
        table.style.marginTop = '1em';
        
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        headerRow.innerHTML = `
            <th>Тип клієнта</th>
            <th style="text-align: center;">Перегляд</th>
            <th style="text-align: center;">Створення</th>
            <th style="text-align: center;">Редагування</th>
            <th style="text-align: center;">Видалення</th>
            <th style="text-align: center;">Дії</th>
        `;
        thead.appendChild(headerRow);
        table.appendChild(thead);
        
        const tbody = document.createElement('tbody');
        
        allClientTypes.forEach(clientType => {
            const permission = permissionsMap.get(clientType.id);
            const row = document.createElement('tr');
            
            row.innerHTML = `
                <td>${clientType.name}</td>
                <td style="text-align: center;">
                    <input type="checkbox" class="permission-checkbox" 
                           data-client-type-id="${clientType.id}" 
                           data-permission-type="canView" 
                           ${permission && permission.canView ? 'checked' : ''}
                           ${!permission ? 'disabled' : ''}>
                </td>
                <td style="text-align: center;">
                    <input type="checkbox" class="permission-checkbox" 
                           data-client-type-id="${clientType.id}" 
                           data-permission-type="canCreate" 
                           ${permission && permission.canCreate ? 'checked' : ''}
                           ${!permission ? 'disabled' : ''}>
                </td>
                <td style="text-align: center;">
                    <input type="checkbox" class="permission-checkbox" 
                           data-client-type-id="${clientType.id}" 
                           data-permission-type="canEdit" 
                           ${permission && permission.canEdit ? 'checked' : ''}
                           ${!permission ? 'disabled' : ''}>
                </td>
                <td style="text-align: center;">
                    <input type="checkbox" class="permission-checkbox" 
                           data-client-type-id="${clientType.id}" 
                           data-permission-type="canDelete" 
                           ${permission && permission.canDelete ? 'checked' : ''}
                           ${!permission ? 'disabled' : ''}>
                </td>
                <td style="text-align: center;">
                    ${permission ? 
                        `<button class="btn-delete action-btn" onclick="deleteClientTypePermission(${clientType.id}, ${currentUserForClientTypePermissions.id})">Видалити доступ</button>` :
                        `<button class="btn-primary action-btn" onclick="createClientTypePermission(${clientType.id}, ${currentUserForClientTypePermissions.id})">Додати доступ</button>`
                    }
                </td>
            `;
            
            tbody.appendChild(row);
        });
        
        table.appendChild(tbody);
        permissionsList.appendChild(table);
        
    } catch (error) {
        console.error('Error loading client type permissions:', error);
        showMessage('Помилка завантаження прав доступу до типів клієнтів', 'error');
    }
}

async function createClientTypePermission(clientTypeId, userId) {
    try {
        const response = await fetch(`/api/v1/client-type/${clientTypeId}/permission`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userId: userId,
                canView: true,
                canCreate: false,
                canEdit: false,
                canDelete: false
            })
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to create permission');
        }
        
        showMessage('Доступ до типу клієнта додано', 'success');
        await loadClientTypePermissions(userId);
    } catch (error) {
        console.error('Error creating client type permission:', error);
        showMessage('Помилка створення доступу: ' + error.message, 'error');
    }
}

async function deleteClientTypePermission(clientTypeId, userId) {
    if (!confirm('Ви впевнені, що хочете видалити доступ до цього типу клієнта?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/v1/client-type/${clientTypeId}/permission/${userId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to delete permission');
        }
        
        showMessage('Доступ до типу клієнта видалено', 'success');
        await loadClientTypePermissions(userId);
    } catch (error) {
        console.error('Error deleting client type permission:', error);
        showMessage('Помилка видалення доступу: ' + error.message, 'error');
    }
}

// Обработчик сохранения прав доступа
document.getElementById('saveClientTypePermissionsButton')?.addEventListener('click', async () => {
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
    
    try {
        const updatePromises = [];
        
        for (const [clientTypeId, permission] of permissionsByClientType.entries()) {
            const updatePromise = fetch(`/api/v1/client-type/${clientTypeId}/permission/${currentUserForClientTypePermissions.id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(permission)
            });
            updatePromises.push(updatePromise);
        }
        
        const responses = await Promise.all(updatePromises);
        const errors = [];
        
        responses.forEach((response, index) => {
            if (!response.ok) {
                errors.push(`Помилка оновлення прав для типу клієнта ${Array.from(permissionsByClientType.keys())[index]}`);
            }
        });
        
        if (errors.length > 0) {
            throw new Error(errors.join('\n'));
        }
        
        showMessage('Права доступу до типів клієнтів збережено', 'success');
        await loadClientTypePermissions(currentUserForClientTypePermissions.id);
        
        // Закрываем модальное окно после успешного сохранения
        const modal = document.getElementById('userClientTypePermissionsModal');
        if (modal) {
            modal.style.display = 'none';
        }
        currentUserForClientTypePermissions = null;
    } catch (error) {
        console.error('Error saving client type permissions:', error);
        showMessage('Помилка збереження прав доступу: ' + error.message, 'error');
    }
});

window.createClientTypePermission = createClientTypePermission;
window.deleteClientTypePermission = deleteClientTypePermission;
