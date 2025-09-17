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
    'settings:edit'
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

        'user:create': 'Создаение пользователей',
        'user:edit': 'Изминение пользователей',
        'user:delete': 'Удаление пользователей',

        'client:view': 'Отображение клиентов',
        'client:create': 'Создание клиентов',
        'client:edit': 'Редактирование клиентов',
        'client:delete': 'Удаление клиентов',
        'client:edit_strangers': 'Редактирование чужих клиентов',
        'client:full_delete': 'Полное удаление клиента',
        'client:edit_source': 'Редактировение привличения клиента',
        'client:excel': 'Выгрузка клиентов в excel',

        'sale:view': 'Отображение продаж',
        'sale:create': 'Создаение продаж',
        'sale:edit': 'Изминение продаж',
        'sale:edit_strangers': 'Редактирование чужих продаж',
        'sale:delete': 'Удаление продаж',
        'sale:edit_source': 'Изменение привлечения продаж',
        'sale:excel': 'Выгрузка продаж в excel',

        'purchase:view': 'Отображение закупок',
        'purchase:create': 'Создаение закупок',
        'purchase:edit': 'Изминение закупок',
        'purchase:edit_strangers': 'Редактирование чужих закупок',
        'purchase:delete': 'Удаление закупок',
        'purchase:edit_source': 'Изменение привлечения закупок',
        'purchase:excel': 'Выгрузка закупок в excel',

        'container:view': 'Отображение тары',
        'container:transfer': 'Передача тары',
        'container:balance': 'Изминения баланса тары',
        'container:excel': 'Выгрузка тары в excel',

        'finance:view': 'Отображение финансов',
        'finance:balance_edit': 'Изменение баланса финансов',
        'finance:transfer_view': 'Просмотр транзакций',
        'finance:transfer_excel': 'Выгрузка в excel транзакций',
        'transaction:delete': 'Удаление закупок и продаж',

        'warehouse:view': 'Отображение склада',
        'warehouse:create': 'Создаение прихода на склад',
        'warehouse:edit': 'Изминение прихода на склад',
        'warehouse:withdraw': 'Снятие баланса склада',
        'warehouse:delete': 'Удаление прихода на склад',
        'warehouse:excel': 'Выгрузка в excel данных со склада',

        'inventory:view': 'Отображение баланса тары',
        'inventory:manage': 'Управление балансом тары',

        'analytics:view': 'Отображение аналитики',

        'settings:view': 'Отображение настроек',
        'settings:edit': 'Редактирование настроек'
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


    /*----------source----------*/

    document.getElementById('showSourcesBtn').addEventListener('click',
        async function () {
        const sourceListContainer = document.getElementById('sourceListContainer');
        const addSourceContainer = document.getElementById('addSourceContainer');
        sourceListContainer.style.display = sourceListContainer.style.display === 'none' ? 'block' : 'none';
        addSourceContainer.style.display = addSourceContainer.style.display === 'none' ? 'block' : 'none';
        if (sourceListContainer.style.display === 'block') {
            loadSources();
        }
    });

    async function loadSources() {
        try {
            const response = await fetch('/api/v1/source');
            if (!response.ok) new Error('Error fetching sources');
            const sources = await response.json();
            const sourceList = document.getElementById('sourceList');
            sourceList.innerHTML = '';

            sources.forEach(source => {
                const listItem = document.createElement('li');
                listItem.classList.add('source-item');
                listItem.setAttribute('data-source-id', source.id);

                const sourceText = document.createElement('span');
                sourceText.textContent = `${source.id} - ${source.name}`;
                listItem.appendChild(sourceText);

                /*                const colorBox = document.createElement('span');
                                colorBox.style.backgroundColor = source.color;
                                colorBox.style.width = '20px';
                                colorBox.style.height = '20px';
                                colorBox.style.display = 'inline-block';
                                colorBox.style.marginLeft = '10px';
                                listItem.appendChild(colorBox);*/

                const editButton = document.createElement('button');
                editButton.textContent = 'Змінити';
                editButton.onclick = () => editSource(source);
                listItem.appendChild(editButton);

                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'Видалити';
                deleteButton.onclick = () => deleteSource(source.id);
                listItem.appendChild(deleteButton);

                sourceList.appendChild(listItem);
            });
        } catch (error) {
            console.error('Error loading sources:', error);
        }
    }

    function editSource(source) {
        document.getElementById('editSourceModal').style.display = 'block';

        document.getElementById('editSourceName').value = source.name;
        /*document.getElementById('editSourceColor').value = source.color;*/

        document.getElementById('saveSourceBtn').onclick = () => {
            const newName = document.getElementById('editSourceName').value;
            /*const newColor = document.getElementById('editSourceColor').value;*/

            if (newName !== null /*&& newColor !== null*/) {
                updateSource(source.id, newName/*, newColor*/);
            }

            document.getElementById('editSourceModal').style.display = 'none';
        };

        document.getElementById('cancelEditSourceBtn').onclick = () => {
            document.getElementById('editSourceModal').style.display = 'none';
        };
    }

    async function updateSource(id, name) {
        try {
            const response = await fetch(`/api/v1/source/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({name})
            });

            if (!response.ok) new Error('Error updating source');
            alert('Джерело оновлено!');
            loadSources();
        } catch (error) {
            console.error('Error updating source:', error);
        }
    }

    async function deleteSource(id) {
        try {
            const response = await fetch(`/api/v1/source/${id}`, {
                method: 'DELETE'
            });

            if (!response.ok) new Error('Error deleting source');
            alert('Джерело видалено!');
            loadSources();
        } catch (error) {
            console.error('Error deleting source:', error);
        }
    }

    document.getElementById('createSourceBtn').addEventListener('click',
        async function () {
        const name = document.getElementById('newSourceName').value;
        /*const id = document.getElementById('newSourceId').value;
        const color = document.getElementById('newSourceColor').value;*/

        if (!name /*|| !id*/) {
            alert('Будь ласка, введіть всі дані для створення джерела.');
            return;
        }

        try {
            const response = await fetch('/api/v1/source', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({/*id: parseInt(id), */name/*, color*/})
            });

            if (!response.ok) new Error('Error creating source');
            alert('Джерело створено!');
            /*document.getElementById('newSourceName').value = '';*/
            document.getElementById('newSourceId').value = '';
            /*document.getElementById('newSourceColor').value = '#ffffff';*/
            loadSources();
        } catch (error) {
            console.error('Error creating source:', error);
        }
    });

    /*----------route----------*/

    document.getElementById('showRoutesBtn').addEventListener('click',
        async function () {
        const routeListContainer = document.getElementById('routeListContainer');
        const addRouteContainer = document.getElementById('addRouteContainer');
        routeListContainer.style.display = routeListContainer.style.display === 'none' ? 'block' : 'none';
        addRouteContainer.style.display = addRouteContainer.style.display === 'none' ? 'block' : 'none';
        if (routeListContainer.style.display === 'block') {
            loadRoutes();
        }
    });

    async function loadRoutes() {
        try {
            const response = await fetch('/api/v1/route');
            if (!response.ok) new Error('Error fetching routes');
            const routes = await response.json();
            const routeList = document.getElementById('routeList');
            routeList.innerHTML = '';

            routes.forEach(route => {
                const listItem = document.createElement('li');
                listItem.classList.add('route-item');
                listItem.setAttribute('data-route-id', route.id);

                const routeText = document.createElement('span');
                routeText.textContent = `${route.id} - ${route.name}`;
                listItem.appendChild(routeText);

                const editButton = document.createElement('button');
                editButton.textContent = 'Змінити';
                editButton.onclick = () => editRoute(route);
                listItem.appendChild(editButton);

                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'Видалити';
                deleteButton.onclick = () => deleteRoute(route.id);
                listItem.appendChild(deleteButton);

                routeList.appendChild(listItem);
            });
        } catch (error) {
            console.error('Error loading routes:', error);
        }
    }

    function editRoute(route) {
        const newName = prompt('Введіть нове ім’я маршрута:', route.name);
        if (newName !== null) {
            updateRoute(route.id, newName);
        }
    }

    async function updateRoute(id, name) {
        try {
            const response = await fetch(`/api/v1/route/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({id, name})
            });

            if (!response.ok) new Error('Error updating route');
            alert('Маршрут оновлено!');
            loadRoutes();
        } catch (error) {
            console.error('Error updating route:', error);
        }
    }

    async function deleteRoute(id) {
        try {
            const response = await fetch(`/api/v1/route/${id}`, {
                method: 'DELETE'
            });

            if (!response.ok) new Error('Error deleting route');
            alert('Маршрут видалено!');
            loadRoutes();
        } catch (error) {
            console.error('Error deleting route:', error);
        }
    }

    document.getElementById('createRouteBtn').addEventListener('click',
        async function () {
        const name = document.getElementById('newRouteName').value;
        /*const id = document.getElementById('newRouteId').value;*/

        if (!name /*|| !id*/) {
            alert('Будь ласка, введіть всі дані для створення маршруту.');
            return;
        }

        try {
            const response = await fetch('/api/v1/route', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({name})
            });

            if (!response.ok) new Error('Error creating route');
            alert('Маршрут створений!');
            document.getElementById('newRouteName').value = '';
            /*document.getElementById('newRouteId').value = '';*/
            loadRoutes();
        } catch (error) {
            console.error('Error creating route:', error);
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

    /*----------business----------*/

    document.getElementById('showBusinessesBtn').addEventListener('click',
        async function () {
        const businessListContainer = document.getElementById('businessListContainer');
        const addBusinessContainer = document.getElementById('addBusinessContainer');
        businessListContainer.style.display = businessListContainer.style.display === 'none' ? 'block' : 'none';
        addBusinessContainer.style.display = addBusinessContainer.style.display === 'none' ? 'block' : 'none';
        if (businessListContainer.style.display === 'block') {
            loadBusinesses();
        }
    });

    async function loadBusinesses() {
        try {
            const response = await fetch('/api/v1/business');
            if (!response.ok) new Error('Error fetching businesses');
            const businesses = await response.json();
            const businessList = document.getElementById('businessList');
            businessList.innerHTML = '';

            businesses.forEach(business => {
                const listItem = document.createElement('li');
                listItem.classList.add('business-item');
                listItem.setAttribute('data-business-id', business.id);

                const businessText = document.createElement('span');
                businessText.textContent = `${business.id} - ${business.name}`;
                listItem.appendChild(businessText);

                const editButton = document.createElement('button');
                editButton.textContent = 'Змінити';
                editButton.onclick = () => editBusiness(business);
                listItem.appendChild(editButton);

                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'Видалити';
                deleteButton.onclick = () => deleteBusiness(business.id);
                listItem.appendChild(deleteButton);

                businessList.appendChild(listItem);
            });
        } catch (error) {
            console.error('Error loading businesses:', error);
        }
    }

    function editBusiness(business) {
        const newName = prompt('Введіть нове ім’я типу бізнеса:', business.name);
        if (newName !== null) {
            updateBusiness(business.id, newName);
        }
    }

    async function updateBusiness(id, name) {
        try {
            const response = await fetch(`/api/v1/business/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({id, name})
            });

            if (!response.ok) new Error('Error updating business');
            alert('Тип бізнесу оновлено!');
            loadBusinesses();
        } catch (error) {
            console.error('Error updating business:', error);
        }
    }

    async function deleteBusiness(id) {
        try {
            const response = await fetch(`/api/v1/business/${id}`, {
                method: 'DELETE'
            });

            if (!response.ok) new Error('Error deleting business');
            alert('Тип бізнесу видалено!');
            loadBusinesses();
        } catch (error) {
            console.error('Error deleting business:', error);
        }
    }

    document.getElementById('createBusinessBtn').addEventListener('click',
        async function () {
        const name = document.getElementById('newBusinessName').value;
        /*const id = document.getElementById('newRouteId').value;*/

        if (!name /*|| !id*/) {
            alert('Будь ласка, введіть всі дані для створення типу бізнесу.');
            return;
        }

        try {
            const response = await fetch('/api/v1/business', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({name})
            });

            if (!response.ok) new Error('Error creating business');
            alert('Тип бізнесу створений!');
            document.getElementById('newBusinessName').value = '';
            /*document.getElementById('newRouteId').value = '';*/
            loadBusinesses();
        } catch (error) {
            console.error('Error creating business:', error);
        }
    });


    /*----------region----------*/

    document.getElementById('showRegionsBtn').addEventListener('click',
        async function () {
        const regionListContainer = document.getElementById('regionListContainer');
        const addRegionContainer = document.getElementById('addRegionContainer');
        regionListContainer.style.display = regionListContainer.style.display === 'none' ? 'block' : 'none';
        addRegionContainer.style.display = addRegionContainer.style.display === 'none' ? 'block' : 'none';
        if (regionListContainer.style.display === 'block') {
            loadRegions();
        }
    });

    async function loadRegions() {
        try {
            const response = await fetch('/api/v1/region');
            if (!response.ok) new Error('Error fetching regions');
            const regions = await response.json();
            const regionList = document.getElementById('regionList');
            regionList.innerHTML = '';

            regions.forEach(region => {
                const listItem = document.createElement('li');
                listItem.classList.add('region-item');
                listItem.setAttribute('data-region-id', region.id);

                const regionText = document.createElement('span');
                regionText.textContent = `${region.id} - ${region.name}`;
                listItem.appendChild(regionText);

                const editButton = document.createElement('button');
                editButton.textContent = 'Змінити';
                editButton.onclick = () => editRegion(region);
                listItem.appendChild(editButton);

                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'Видалити';
                deleteButton.onclick = () => deleteRegion(region.id);
                listItem.appendChild(deleteButton);

                regionList.appendChild(listItem);
            });
        } catch (error) {
            console.error('Error loading regions:', error);
        }
    }

    function editRegion(region) {
        const newName = prompt('Введіть нове ім’я області:', region.name);
        if (newName !== null) {
            updateRegion(region.id, newName);
        }
    }

    async function updateRegion(id, name) {
        try {
            const response = await fetch(`/api/v1/region/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({id, name})
            });

            if (!response.ok) new Error('Error updating region');
            alert('Область оновлено!');
            loadRegions();
        } catch (error) {
            console.error('Error updating region:', error);
        }
    }

    async function deleteRegion(id) {
        try {
            const response = await fetch(`/api/v1/region/${id}`, {
                method: 'DELETE'
            });

            if (!response.ok) new Error('Error deleting region');
            alert('Область видалено!');
            loadRegions();
        } catch (error) {
            console.error('Error deleting region:', error);
        }
    }

    document.getElementById('createRegionBtn').addEventListener('click',
        async function () {
        const name = document.getElementById('newRegionName').value;
        /*const id = document.getElementById('newRouteId').value;*/

        if (!name /*|| !id*/) {
            alert('Будь ласка, введіть всі дані для створення області.');
            return;
        }

        try {
            const response = await fetch('/api/v1/region', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({name})
            });

            if (!response.ok) new Error('Error creating region');
            alert('Область створений!');
            document.getElementById('newRegionName').value = '';
            /*document.getElementById('newRouteId').value = '';*/
            loadRegions();
        } catch (error) {
            console.error('Error creating region:', error);
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


    /*----------status----------*/

    document.getElementById('showStatusesBtn').addEventListener('click',
        async function () {
        const statusListContainer = document.getElementById('statusListContainer');
        const addStatusContainer = document.getElementById('addStatusContainer');
        statusListContainer.style.display = statusListContainer.style.display === 'none' ? 'block' : 'none';
        addStatusContainer.style.display = addStatusContainer.style.display === 'none' ? 'block' : 'none';
        if (statusListContainer.style.display === 'block') {
            loadStatuses();
        }
    });

    async function loadStatuses() {
        try {
            const response = await fetch('/api/v1/status');
            if (!response.ok) new Error('Error fetching statuses');
            const statuses = await response.json();
            const statusList = document.getElementById('statusList');
            statusList.innerHTML = '';

            statuses.forEach(status => {
                const listItem = document.createElement('li');
                listItem.classList.add('status-item');
                listItem.setAttribute('data-status-id', status.id);

                const statusText = document.createElement('span');
                statusText.textContent = `${status.id} - ${status.name}`;
                listItem.appendChild(statusText);

                // Кнопка редактирования
                const editButton = document.createElement('button');
                editButton.textContent = 'Змінити';
                editButton.onclick = () => editStatus(status);
                listItem.appendChild(editButton);

                // Кнопка удаления
                const deleteButton = document.createElement('button');
                deleteButton.textContent = 'Видалити';
                deleteButton.onclick = () => deleteStatus(status.id);
                listItem.appendChild(deleteButton);

                statusList.appendChild(listItem);
            });
        } catch (error) {
            console.error('Error loading statuses:', error);
        }
    }

    function editStatus(status) {
        const newName = prompt('Введіть нове ім’я статусу:', status.name);
        if (newName !== null) {
            updateStatus(status.id, newName);
        }
    }

    async function updateStatus(id, name) {
        try {
            const response = await fetch(`/api/v1/status/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({id, name})
            });

            if (!response.ok) new Error('Error updating status');
            alert('Статус оновлено!');
            loadStatuses();
        } catch (error) {
            console.error('Error updating status:', error);
        }
    }

    async function deleteStatus(id) {
        try {
            const response = await fetch(`/api/v1/status/${id}`, {
                method: 'DELETE'
            });

            if (!response.ok) new Error('Error deleting status');
            alert('Статус видалено!');
            loadStatuses();
        } catch (error) {
            console.error('Error deleting status:', error);
        }
    }

    document.getElementById('createStatusBtn').addEventListener('click',
        async function () {
        const name = document.getElementById('newStatusName').value;
        const id = document.getElementById('newStatusId').value;

        if (!name || !id) {
            alert('Будь ласка, введіть всі дані для створення статусу.');
            return;
        }

        try {
            const response = await fetch('/api/v1/status', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({id: parseInt(id), name})
            });

            if (!response.ok) new Error('Error creating status');
            alert('Статус створено!');
            document.getElementById('newStatusName').value = '';
            document.getElementById('newStatusId').value = '';
            loadStatuses();
        } catch (error) {
            console.error('Error creating status:', error);
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







    document.getElementById('exportToExcel').addEventListener('click', () => {
        const exportModal = document.getElementById('exportModal');
        exportModal.classList.remove('hide');
        exportModal.style.display = 'flex';
        setTimeout(() => {
            exportModal.classList.add('show');
        }, 10);
    });

    document.getElementById('exportCancel').addEventListener('click', () => {
        const exportModal = document.getElementById('exportModal');
        exportModal.classList.add('hide');
        exportModal.classList.remove('show');
        setTimeout(() => {
            exportModal.style.display = 'none';
        }, 300);
    });

    document.getElementById('exportConfirm').addEventListener('click', async () => {
        const exportModal = document.getElementById('exportModal');
        const loaderBackdrop = document.getElementById('loaderBackdrop');
        const purchaseDataFrom = document.getElementById('purchaseDataFrom').value;
        const purchaseDataTo = document.getElementById('purchaseDataTo').value;

        // Basic validation
        if (!purchaseDataFrom ||  !purchaseDataTo) {
            showMessage('Please select both dates', 'error');
            return;
        }

        if (new Date(purchaseDataTo) < new Date(purchaseDataFrom)) {
            showMessage('End date cannot be earlier than start date', 'error');
            return;
        }

        exportModal.style.display = 'none';

        try {
            const params = new URLSearchParams({
                purchaseDataFrom,
                purchaseDataTo
            });

            const response = await fetch(`/api/v1/purchase/comparison/excel?${params}`, {
                method: 'POST',
                headers: {
                    'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
                }
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message,  'Failed to export Excel');
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = 'comparison_data.xlsx';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            showMessage('Data successfully exported to Excel', 'info');
        } catch (error) {
            handleError(error);
        }
    });
});


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
