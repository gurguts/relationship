const loaderBackdrop = document.getElementById('loader-backdrop');
const allPermissions = [
    'system:admin',

    'user:create',
    'user:edit',
    'user:delete',

    'client:view',
    'client:create',
    'client:edit',
    'client:edit_strangers',
    'client:delete',
    'client:full_delete',
    'client:edit_source',
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
        'client:edit_strangers': 'Редактирование чужих клиентов',
        'client:full_delete': 'Полное удаление клиента',
        'client:edit_source': 'Редактирование привлечения клиента',
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
    const showUsersBtn = document.getElementById('showUsersBtn');
    const addUserBtn = document.getElementById('addUserBtn');
    const addUserContainer = document.getElementById('addUserContainer');


    showUsersBtn.addEventListener('click', async function () {
        const userListContainer = document.getElementById('userListContainer');
        userListContainer.style.display = userListContainer.style.display === 'none' ? 'block' : 'none';

        if (userListContainer.style.display === 'block') {
            try {
                const response = await fetch('/api/v1/user/page');
                const users = await response.json();
                const userList = document.getElementById('userList');
                userList.innerHTML = '';

                users.forEach(user => {
                    const listItem = document.createElement('li');
                    listItem.textContent = `${user.fullName} - ${user.id} - ${user.login} - ${user.role} `;

                    // Create Edit button
                    const editBtn = document.createElement('button');
                    editBtn.textContent = 'Змінити';
                    editBtn.addEventListener('click', () => showEditForm(user, listItem));

                    const permissionsBtn = document.createElement('button');
                    permissionsBtn.textContent = 'Редагувати дозволи';
                    permissionsBtn.className = 'edit-permissions';
                    permissionsBtn.addEventListener('click', () => showUserPermissionsModal(user));

                    // Create Delete button
                    const deleteBtn = document.createElement('button');
                    deleteBtn.textContent = 'Видалити';
                    deleteBtn.addEventListener('click', () => deleteUser(user.id));

                    // Append buttons to the list item
                    listItem.appendChild(editBtn);
                    listItem.appendChild(deleteBtn);
                    listItem.appendChild(permissionsBtn);
                    userList.appendChild(listItem);
                });
            } catch (error) {
                console.error('Error fetching users:', error);
            }
        }
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

    function deleteUser(userId) {
        fetch(`/api/v1/user/${userId}`, {
            method: 'DELETE'
        })
            .then(response => {
                if (response.ok) {
                    loadUsers();
                } else {
                    console.error('Error deleting user');
                }
            })
            .catch(error => console.error('Error:', error));
    }

    async function loadUsers() {
        try {
            const response = await fetch('/api/v1/user');
            if (!response.ok) new Error('Network response was not ok');
            const users = await response.json();
            const userList = document.getElementById('userList');
            userList.innerHTML = '';

            users.forEach(user => {
                const listItem = document.createElement('li');
                listItem.setAttribute('data-user-id', user.id);
                listItem.textContent = `${user.fullName} - ${user.id} - ${user.login} - ${user.role}`;

                const editButton = document.createElement('button');
                editButton.textContent = 'Змінити';
                editButton.onclick = () => showEditForm(user, listItem);

                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'Видалити';
                deleteButton.onclick = () => deleteUser(user.id);


                listItem.appendChild(editButton);
                listItem.appendChild(deleteButton);
                userList.appendChild(listItem);
            });
        } catch (error) {
            console.error('Ошибка при получении пользователей:', error);
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

            await loadUsers();

            document.getElementById('newLogin').value = '';
            document.getElementById('newPassword').value = '';
            document.getElementById('newRole').value = 'MANAGER';

        } catch (error) {
            console.error('Error creating user:', error);
        }
    });

    /*----------oilType----------*/

    document.getElementById('showOilTypesBtn').addEventListener('click',
        async function () {
        const oilTypeListContainer = document.getElementById('oilTypeListContainer');
        const addOilTypeContainer = document.getElementById('addOilTypeContainer');
        oilTypeListContainer.style.display = oilTypeListContainer.style.display === 'none' ? 'block' : 'none';
        addOilTypeContainer.style.display = addOilTypeContainer.style.display === 'none' ? 'block' : 'none';
        if (oilTypeListContainer.style.display === 'block') {
            loadOilTypes();
        }
    });

    async function loadOilTypes() {
        try {
            const response = await fetch('/api/v1/oil/type');
            if (!response.ok) new Error('Error fetching oilTypes');
            const oilTypes = await response.json();
            const oilTypeList = document.getElementById('oilTypeList');
            oilTypeList.innerHTML = '';

            oilTypes.forEach(oilType => {
                const listItem = document.createElement('li');
                listItem.classList.add('oilType-item');
                listItem.setAttribute('data-oilType-id', oilType.id);

                const oilTypeText = document.createElement('span');
                oilTypeText.textContent = `${oilType.id} - ${oilType.name}`;
                listItem.appendChild(oilTypeText);

                const editButton = document.createElement('button');
                editButton.textContent = 'Змінити';
                editButton.onclick = () => editOilType(oilType);
                listItem.appendChild(editButton);

                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'Видалити';
                deleteButton.onclick = () => deleteOilType(oilType.id);
                listItem.appendChild(deleteButton);

                oilTypeList.appendChild(listItem);
            });
        } catch (error) {
            console.error('Error loading oilTypes:', error);
        }
    }

    function editOilType(oilType) {
        const newName = prompt('Введіть нове ім’я області:', oilType.name);
        if (newName !== null) {
            updateOilType(oilType.id, newName);
        }
    }

    async function updateOilType(id, name) {
        try {
            const response = await fetch(`/api/v1/oil/type/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({id, name})
            });

            if (!response.ok) new Error('Error updating oilType');
            alert('Область оновлено!');
            loadOilTypes();
        } catch (error) {
            console.error('Error updating oilType:', error);
        }
    }

    async function deleteOilType(id) {
        try {
            const response = await fetch(`/api/v1/oil/type/${id}`, {
                method: 'DELETE'
            });

            if (!response.ok) new Error('Error deleting oilType');
            alert('Область видалено!');
            loadOilTypes();
        } catch (error) {
            console.error('Error deleting oilType:', error);
        }
    }

    document.getElementById('createOilTypeBtn').addEventListener('click',
        async function () {
        const name = document.getElementById('newOilTypeName').value;
        /*const id = document.getElementById('newRouteId').value;*/

        if (!name /*|| !id*/) {
            alert('Будь ласка, введіть всі дані для створення товара.');
            return;
        }

        try {
            const response = await fetch('/api/v1/oil/type', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({name})
            });

            if (!response.ok) new Error('Error creating oilType');
            alert('Область створений!');
            document.getElementById('newOilTypeName').value = '';
            /*document.getElementById('newRouteId').value = '';*/
            loadOilTypes();
        } catch (error) {
            console.error('Error creating oilType:', error);
        }
    });

    /*----------container----------*/

    document.getElementById('showContainersBtn').addEventListener('click',
        async function () {
        const containerListContainer = document.getElementById('containerListContainer');
        const addContainerContainer = document.getElementById('addContainerContainer');
        containerListContainer.style.display = containerListContainer.style.display === 'none' ? 'block' : 'none';
        addContainerContainer.style.display = addContainerContainer.style.display === 'none' ? 'block' : 'none';
        if (containerListContainer.style.display === 'block') {
            loadContainers();
        }
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

    document.getElementById('showStoragesBtn').addEventListener('click',
        async function () {
        const storageListContainer = document.getElementById('storageListContainer');
        const addStorageContainer = document.getElementById('addStorageContainer');
        storageListContainer.style.display = storageListContainer.style.display === 'none' ? 'block' : 'none';
        addStorageContainer.style.display = addStorageContainer.style.display === 'none' ? 'block' : 'none';
        if (storageListContainer.style.display === 'block') {
            loadStorages();
        }
    });

    /*----------WithdrawalReason----------*/

    document.getElementById('showWithdrawalReasonsBtn').addEventListener('click',
        async function () {
        const withdrawalReasonListContainer = document.getElementById('withdrawalReasonListContainer');
        const addWithdrawalReasonContainer = document.getElementById('addWithdrawalReasonContainer');
        withdrawalReasonListContainer.style.display = withdrawalReasonListContainer.style.display === 'none' ? 'block' : 'none';
        addWithdrawalReasonContainer.style.display = addWithdrawalReasonContainer.style.display === 'none' ? 'block' : 'none';
        if (withdrawalReasonListContainer.style.display === 'block') {
            loadWithdrawalReasons();
        }
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
    const showProductsBtn = document.getElementById('show-products');
    const createProductForm = document.getElementById('create-product-form');
    const createProduct = document.getElementById('create-product');
    const editProductModal = document.getElementById('edit-product-modal');
    const editProductForm = document.getElementById('edit-product-form');
    const cancelEditBtn = document.getElementById('cancel-edit');

    const apiBaseUrl = '/api/v1/product';

    showProductsBtn.addEventListener('click', () => {
        productList.style.display = 'block';
        createProduct.style.display = 'block';
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

    cancelEditBtn.addEventListener('click', () => {
        editProductModal.style.display = 'none';
    });

    /*--client-product--*/

    const clientProductList = document.getElementById('client-product-list');
    const clientProductsTableBody = document.getElementById('client-products-table-body');
    const clientShowProductsBtn = document.getElementById('show-client-products');
    const clientCreateProductForm = document.getElementById('client-create-product-form');
    const clientCreateProduct = document.getElementById('client-create-product');
    const clientEditProductModal = document.getElementById('client-edit-product-modal');
    const clientEditProductForm = document.getElementById('client-edit-product-form');
    const clientCancelEditBtn = document.getElementById('client-cancel-edit');

    const apiBaseClientProductUrl = '/api/v1/clientProduct';

    clientShowProductsBtn.addEventListener('click', () => {
        clientProductList.style.display = 'block';
        clientCreateProduct.style.display = 'block';
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

    clientCancelEditBtn.addEventListener('click', () => {
        clientEditProductModal.style.display = 'none';
    });



    // Branch Permissions
    document.getElementById('showBranchPermissionsBtn').addEventListener('click', function () {
        showBranchPermissions();
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

async function showBranchPermissions() {
    const container = document.getElementById('branchPermissionsContainer');
    container.style.display = container.style.display === 'none' ? 'block' : 'none';
    
    if (container.style.display === 'block') {
        await loadUsersForPermissions();
        await loadBranchesForPermissions();
        await loadBranchPermissions();
    }
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
