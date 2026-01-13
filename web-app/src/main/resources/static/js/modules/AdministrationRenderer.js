const AdministrationRenderer = (function() {
    const roles = {
        'manager': 'Менеджер',
        'driver': 'Водій',
        'storekeeper': 'Комірник',
        'leader': 'Керівник',
        'accountant': 'Бухгалтер',
        'declarant': 'Декларант'
    };

    function renderUsers(users, userList) {
        if (!userList) return;
        userList.textContent = '';
        
        users.forEach(user => {
            const listItem = document.createElement('li');
            const statusText = user.status === 'BANNED' ? '❌ Заблокований' : '✅ Активний';
            const roleText = roles[user.role?.toLowerCase()] || user.role;
            listItem.textContent = `${user.fullName || ''} - ${user.id} - ${user.login || ''} - ${roleText} - ${statusText} `;

            const editBtn = document.createElement('button');
            editBtn.textContent = 'Змінити';
            editBtn.addEventListener('click', () => window.editUser?.(user, listItem));

            const permissionsBtn = document.createElement('button');
            permissionsBtn.textContent = 'Редагувати дозволи';
            permissionsBtn.className = 'edit-permissions';
            permissionsBtn.addEventListener('click', () => window.showUserPermissionsModal?.(user));

            const clientTypePermissionsBtn = document.createElement('button');
            clientTypePermissionsBtn.textContent = 'Доступ до типів клієнтів';
            clientTypePermissionsBtn.className = 'btn-secondary';
            clientTypePermissionsBtn.style.marginLeft = '5px';
            clientTypePermissionsBtn.addEventListener('click', () => window.showUserClientTypePermissionsModal?.(user));

            const statusBtn = document.createElement('button');
            const nextStatus = user.status === 'BANNED' ? 'ACTIVE' : 'BANNED';
            statusBtn.textContent = user.status === 'BANNED' ? 'Активувати' : 'Деактивувати';
            statusBtn.className = user.status === 'BANNED' ? 'btn-activate' : 'btn-deactivate';
            statusBtn.addEventListener('click', () => window.toggleUserStatus?.(user.id, nextStatus));

            const deleteBtn = document.createElement('button');
            deleteBtn.textContent = 'Видалити';
            deleteBtn.addEventListener('click', () => window.deleteUser?.(user.id));

            listItem.appendChild(editBtn);
            listItem.appendChild(statusBtn);
            listItem.appendChild(deleteBtn);
            listItem.appendChild(permissionsBtn);
            listItem.appendChild(clientTypePermissionsBtn);
            userList.appendChild(listItem);
        });
    }

    function renderContainers(containers, barrelTypeList) {
        if (!barrelTypeList) return;
        barrelTypeList.textContent = '';
        
        containers.forEach(container => {
            const listItem = document.createElement('li');
            listItem.classList.add('barrelType-item');
            listItem.setAttribute('data-barrelType-id', container.id);

            const containerText = document.createElement('span');
            containerText.textContent = `${container.id} - ${container.name || ''}`;
            listItem.appendChild(containerText);

            const editButton = document.createElement('button');
            editButton.textContent = 'Змінити';
            editButton.addEventListener('click', () => {
                if (typeof window.editContainer === 'function') {
                    window.editContainer(container);
                }
            });
            listItem.appendChild(editButton);

            const deleteButton = document.createElement('button');
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteContainer === 'function') {
                    window.deleteContainer(container.id);
                }
            });
            listItem.appendChild(deleteButton);

            barrelTypeList.appendChild(listItem);
        });
    }

    function renderStorages(storages, storageList) {
        if (!storageList) return;
        storageList.textContent = '';
        
        storages.forEach(storage => {
            const listItem = document.createElement('li');
            listItem.classList.add('storage-item');
            listItem.setAttribute('data-storage-id', storage.id);

            const storageText = document.createElement('span');
            storageText.textContent = `${storage.id} - ${storage.name || ''} - ${storage.description || ''}`;
            listItem.appendChild(storageText);

            const editButton = document.createElement('button');
            editButton.textContent = 'Змінити';
            editButton.addEventListener('click', () => {
                if (typeof window.editStorage === 'function') {
                    window.editStorage(storage);
                }
            });
            listItem.appendChild(editButton);

            const deleteButton = document.createElement('button');
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteStorage === 'function') {
                    window.deleteStorage(storage.id);
                }
            });
            listItem.appendChild(deleteButton);

            storageList.appendChild(listItem);
        });
    }

    function renderWithdrawalReasons(reasons, withdrawalReasonList) {
        if (!withdrawalReasonList) return;
        withdrawalReasonList.textContent = '';
        
        reasons.forEach(reason => {
            const listItem = document.createElement('li');
            listItem.classList.add('withdrawalReason-item');
            listItem.setAttribute('data-withdrawalReason-id', reason.id);

            const reasonText = document.createElement('span');
            const purposeText = AdministrationUtils.getPurposeLabel(reason.purpose);
            reasonText.textContent = `${reason.id} - ${reason.name || ''} (${purposeText})`;
            listItem.appendChild(reasonText);

            const editButton = document.createElement('button');
            editButton.textContent = 'Змінити';
            editButton.addEventListener('click', () => {
                if (typeof window.editWithdrawalReason === 'function') {
                    window.editWithdrawalReason(reason);
                }
            });
            listItem.appendChild(editButton);

            const deleteButton = document.createElement('button');
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteWithdrawalReason === 'function') {
                    window.deleteWithdrawalReason(reason.id);
                }
            });
            listItem.appendChild(deleteButton);

            withdrawalReasonList.appendChild(listItem);
        });
    }

    function renderProducts(products, productsTableBody) {
        if (!productsTableBody) return;
        productsTableBody.textContent = '';
        
        if (products.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 4;
            cell.style.textAlign = 'center';
            cell.style.color = 'var(--main-grey)';
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            row.appendChild(cell);
            productsTableBody.appendChild(row);
            return;
        }

        products.forEach(product => {
            const row = document.createElement('tr');
            
            const idCell = document.createElement('td');
            idCell.textContent = product.id || '';
            row.appendChild(idCell);
            
            const nameCell = document.createElement('td');
            nameCell.textContent = product.name || '';
            row.appendChild(nameCell);
            
            const usageCell = document.createElement('td');
            usageCell.textContent = AdministrationUtils.getUsageLabel(product.usage);
            row.appendChild(usageCell);
            
            const actionsCell = document.createElement('td');
            
            const editButton = document.createElement('button');
            editButton.textContent = 'Змінити';
            editButton.addEventListener('click', () => {
                if (typeof window.editProduct === 'function') {
                    window.editProduct(product.id);
                }
            });
            actionsCell.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteProduct === 'function') {
                    window.deleteProduct(product.id);
                }
            });
            actionsCell.appendChild(deleteButton);
            
            row.appendChild(actionsCell);
            productsTableBody.appendChild(row);
        });
    }

    function renderSources(sources, sourceBody, usersCache) {
        if (!sourceBody) return;
        sourceBody.textContent = '';
        
        if (sources.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 3;
            cell.style.textAlign = 'center';
            cell.style.color = 'var(--main-grey)';
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            row.appendChild(cell);
            sourceBody.appendChild(row);
            return;
        }

        const userMap = new Map(usersCache.map(u => [Number(u.id), u.fullName || u.name || '']));

        sources.forEach(source => {
            const row = document.createElement('tr');
            
            const nameCell = document.createElement('td');
            nameCell.textContent = source.name || '';
            row.appendChild(nameCell);
            
            const userId = source.userId ? Number(source.userId) : null;
            const userName = userId && userMap.has(userId) ? userMap.get(userId) : (userId ? `User ${userId}` : 'Не закріплено');
            const userCell = document.createElement('td');
            userCell.textContent = userName;
            row.appendChild(userCell);
            
            const actionsCell = document.createElement('td');
            
            const editButton = document.createElement('button');
            editButton.className = 'btn-edit';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.editSource === 'function') {
                    window.editSource(source.id);
                }
            });
            actionsCell.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'btn-delete';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteSource === 'function') {
                    window.deleteSource(source.id);
                }
            });
            actionsCell.appendChild(deleteButton);
            
            row.appendChild(actionsCell);
            sourceBody.appendChild(row);
        });
    }

    function renderClientTypes(clientTypes, tbody) {
        if (!tbody) return;
        tbody.textContent = '';
        
        if (clientTypes.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 5;
            cell.style.textAlign = 'center';
            cell.style.color = 'var(--main-grey)';
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }

        clientTypes.forEach(type => {
            const row = document.createElement('tr');
            
            const idCell = document.createElement('td');
            idCell.textContent = type.id || '';
            row.appendChild(idCell);
            
            const nameCell = document.createElement('td');
            nameCell.textContent = type.name || '';
            row.appendChild(nameCell);
            
            const nameFieldLabelCell = document.createElement('td');
            nameFieldLabelCell.textContent = type.nameFieldLabel || '';
            row.appendChild(nameFieldLabelCell);
            
            const isActiveCell = document.createElement('td');
            isActiveCell.textContent = type.isActive ? 'Так' : 'Ні';
            row.appendChild(isActiveCell);
            
            const actionsCell = document.createElement('td');
            
            const editButton = document.createElement('button');
            editButton.className = 'btn-activate';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.openEditClientTypeModal === 'function') {
                    window.openEditClientTypeModal(type.id);
                }
            });
            actionsCell.appendChild(editButton);
            
            const fieldsButton = document.createElement('button');
            fieldsButton.className = 'btn-activate';
            fieldsButton.textContent = 'Поля';
            fieldsButton.addEventListener('click', () => {
                if (typeof window.manageClientTypeFields === 'function') {
                    window.manageClientTypeFields(type.id);
                }
            });
            actionsCell.appendChild(fieldsButton);
            
            const templateButton = document.createElement('button');
            templateButton.className = 'btn-activate';
            templateButton.textContent = 'Скачати шаблон';
            templateButton.addEventListener('click', () => {
                if (typeof window.downloadClientImportTemplate === 'function') {
                    window.downloadClientImportTemplate(type.id);
                }
            });
            actionsCell.appendChild(templateButton);
            
            const importButton = document.createElement('button');
            importButton.className = 'btn-activate';
            importButton.textContent = 'Імпортувати клієнтів';
            importButton.addEventListener('click', () => {
                if (typeof window.openClientImportModal === 'function') {
                    window.openClientImportModal(type.id);
                }
            });
            actionsCell.appendChild(importButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'btn-deactivate';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteClientType === 'function') {
                    window.deleteClientType(type.id);
                }
            });
            actionsCell.appendChild(deleteButton);
            
            row.appendChild(actionsCell);
            tbody.appendChild(row);
        });
    }

    function renderFields(fields, tbody) {
        if (!tbody) return;
        tbody.textContent = '';
        
        if (fields.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 9;
            cell.style.textAlign = 'center';
            cell.style.color = 'var(--main-grey)';
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }

        fields.forEach(field => {
            const row = document.createElement('tr');
            
            const labelCell = document.createElement('td');
            labelCell.textContent = field.fieldLabel || '';
            row.appendChild(labelCell);
            
            const typeCell = document.createElement('td');
            typeCell.textContent = AdministrationUtils.getFieldTypeLabel(field.fieldType);
            row.appendChild(typeCell);
            
            const requiredCell = document.createElement('td');
            requiredCell.textContent = field.isRequired ? 'Так' : 'Ні';
            row.appendChild(requiredCell);
            
            const searchableCell = document.createElement('td');
            searchableCell.textContent = field.isSearchable ? 'Так' : 'Ні';
            row.appendChild(searchableCell);
            
            const filterableCell = document.createElement('td');
            filterableCell.textContent = field.isFilterable ? 'Так' : 'Ні';
            row.appendChild(filterableCell);
            
            const visibleInTableCell = document.createElement('td');
            visibleInTableCell.textContent = field.isVisibleInTable ? 'Так' : 'Ні';
            row.appendChild(visibleInTableCell);
            
            const visibleInCreateCell = document.createElement('td');
            visibleInCreateCell.textContent = field.isVisibleInCreate ? 'Так' : 'Ні';
            row.appendChild(visibleInCreateCell);
            
            const orderCell = document.createElement('td');
            orderCell.textContent = String(field.displayOrder || 0);
            row.appendChild(orderCell);
            
            const actionsCell = document.createElement('td');
            
            const editButton = document.createElement('button');
            editButton.className = 'btn-activate';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.openEditFieldModal === 'function') {
                    window.openEditFieldModal(field.id);
                }
            });
            actionsCell.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'btn-deactivate';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteField === 'function') {
                    window.deleteField(field.id);
                }
            });
            actionsCell.appendChild(deleteButton);
            
            row.appendChild(actionsCell);
            tbody.appendChild(row);
        });
    }

    function renderBranchPermissions(permissions, tbody, usersCache, branchesCache) {
        if (!tbody) return;
        tbody.textContent = '';
        
        if (permissions.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 5;
            cell.style.textAlign = 'center';
            cell.style.color = 'var(--main-grey)';
            cell.textContent = CLIENT_MESSAGES.NO_DATA;
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }

        const userMap = new Map(usersCache.map(u => [Number(u.id), u.name || u.fullName || u.login || `User ${u.id}`]));
        const branchMap = new Map(branchesCache.map(b => [Number(b.id), b.name || '']));

        permissions.forEach(permission => {
            const row = document.createElement('tr');
            
            const userId = permission.userId ? Number(permission.userId) : null;
            const branchId = permission.branchId ? Number(permission.branchId) : null;
            const user = userId ? (userMap.get(userId) || `User ${userId}`) : `User ${permission.userId}`;
            const branch = branchId ? (branchMap.get(branchId) || `Branch ${branchId}`) : `Branch ${permission.branchId}`;
            
            const userCell = document.createElement('td');
            userCell.textContent = user;
            row.appendChild(userCell);
            
            const branchCell = document.createElement('td');
            branchCell.textContent = branch;
            row.appendChild(branchCell);
            
            const canViewCell = document.createElement('td');
            const canViewBadge = document.createElement('span');
            canViewBadge.className = `status-badge ${permission.canView ? 'status-active' : 'status-inactive'}`;
            canViewBadge.textContent = permission.canView ? 'Так' : 'Ні';
            canViewCell.appendChild(canViewBadge);
            row.appendChild(canViewCell);
            
            const canOperateCell = document.createElement('td');
            const canOperateBadge = document.createElement('span');
            canOperateBadge.className = `status-badge ${permission.canOperate ? 'status-active' : 'status-inactive'}`;
            canOperateBadge.textContent = permission.canOperate ? 'Так' : 'Ні';
            canOperateCell.appendChild(canOperateBadge);
            row.appendChild(canOperateCell);
            
            const actionsCell = document.createElement('td');
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'action-buttons-table';
            
            const editButton = document.createElement('button');
            editButton.className = 'action-btn btn-edit';
            editButton.textContent = 'Редагувати';
            editButton.addEventListener('click', () => {
                if (typeof window.editBranchPermission === 'function') {
                    window.editBranchPermission(permission.id);
                }
            });
            actionsDiv.appendChild(editButton);
            
            const deleteButton = document.createElement('button');
            deleteButton.className = 'action-btn btn-delete';
            deleteButton.textContent = 'Видалити';
            deleteButton.addEventListener('click', () => {
                if (typeof window.deleteBranchPermission === 'function') {
                    window.deleteBranchPermission(permission.userId, permission.branchId);
                }
            });
            actionsDiv.appendChild(deleteButton);
            
            actionsCell.appendChild(actionsDiv);
            row.appendChild(actionsCell);
            
            tbody.appendChild(row);
        });
    }

    function renderClientTypePermissions(clientTypes, permissionsMap, container) {
        if (!container) return;
        container.textContent = '';
        
        if (clientTypes.length === 0) {
            const emptyMessage = document.createElement('p');
            emptyMessage.style.textAlign = 'center';
            emptyMessage.style.color = 'var(--main-grey)';
            emptyMessage.style.padding = '2em';
            emptyMessage.textContent = CLIENT_MESSAGES.NO_DATA;
            container.appendChild(emptyMessage);
            return;
        }

        clientTypes.forEach(clientType => {
            const permission = permissionsMap.get(clientType.id);
            const hasPermission = permission !== undefined;
            const isDisabled = !hasPermission;

            const itemDiv = document.createElement('div');
            itemDiv.style.marginBottom = '15px';
            itemDiv.style.padding = '10px';
            itemDiv.style.border = '1px solid var(--light-grey)';
            itemDiv.style.borderRadius = '5px';

            const headerDiv = document.createElement('div');
            headerDiv.style.display = 'flex';
            headerDiv.style.justifyContent = 'space-between';
            headerDiv.style.alignItems = 'center';
            headerDiv.style.marginBottom = '10px';

            const titleSpan = document.createElement('span');
            titleSpan.textContent = clientType.name || '';
            titleSpan.style.fontWeight = '600';
            headerDiv.appendChild(titleSpan);

            if (hasPermission) {
                const deleteBtn = document.createElement('button');
                deleteBtn.textContent = 'Видалити доступ';
                deleteBtn.className = 'btn-delete';
                deleteBtn.style.marginLeft = '10px';
                deleteBtn.addEventListener('click', () => {
                    const userId = window.currentUserForClientTypePermissions?.id;
                    if (userId && typeof window.deleteClientTypePermission === 'function') {
                        window.deleteClientTypePermission(clientType.id, userId);
                    }
                });
                headerDiv.appendChild(deleteBtn);
            } else {
                const createBtn = document.createElement('button');
                createBtn.textContent = 'Додати доступ';
                createBtn.className = 'btn-primary';
                createBtn.style.marginLeft = '10px';
                createBtn.addEventListener('click', () => {
                    const userId = window.currentUserForClientTypePermissions?.id;
                    if (userId && typeof window.createClientTypePermission === 'function') {
                        window.createClientTypePermission(clientType.id, userId);
                    }
                });
                headerDiv.appendChild(createBtn);
            }

            itemDiv.appendChild(headerDiv);

            const permissionsDiv = document.createElement('div');
            permissionsDiv.style.display = 'grid';
            permissionsDiv.style.gridTemplateColumns = 'repeat(2, 1fr)';
            permissionsDiv.style.gap = '10px';

            const permissions = [
                { key: 'canView', label: 'Може переглядати' },
                { key: 'canCreate', label: 'Може створювати' },
                { key: 'canEdit', label: 'Може редагувати' },
                { key: 'canDelete', label: 'Може видаляти' }
            ];

            permissions.forEach(perm => {
                const label = document.createElement('label');
                label.style.display = 'flex';
                label.style.alignItems = 'center';
                label.style.gap = '5px';

                const checkbox = document.createElement('input');
                checkbox.type = 'checkbox';
                checkbox.className = 'permission-checkbox';
                checkbox.dataset.clientTypeId = clientType.id;
                checkbox.dataset.permissionType = perm.key;
                checkbox.checked = hasPermission ? permission[perm.key] : false;
                checkbox.disabled = isDisabled;

                label.appendChild(checkbox);
                label.appendChild(document.createTextNode(perm.label));
                permissionsDiv.appendChild(label);
            });

            itemDiv.appendChild(permissionsDiv);
            container.appendChild(itemDiv);
        });
    }

    function populateUserSelect(select, users, selectedUserId = null) {
        if (!select) return;
        select.textContent = '';
        const defaultUserOption = document.createElement('option');
        defaultUserOption.value = '';
        defaultUserOption.textContent = 'Не закріплено';
        select.appendChild(defaultUserOption);
        users.forEach(user => {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = user.fullName || user.name || user.login || `User ${user.id}`;
            if (selectedUserId && Number(user.id) === Number(selectedUserId)) {
                option.selected = true;
            }
            select.appendChild(option);
        });
    }

    function populateUserFilterSelect(select, users) {
        if (!select) return;
        select.textContent = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = 'Всі користувачі';
        select.appendChild(defaultOption);
        users.forEach(user => {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = user.name || user.fullName || user.login || `User ${user.id}`;
            select.appendChild(option);
        });
    }

    function populateBranchSelect(select, branches, selectedBranchId = null) {
        if (!select) return;
        select.textContent = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = 'Виберіть філію';
        select.appendChild(defaultOption);
        branches.forEach(branch => {
            const option = document.createElement('option');
            option.value = branch.id;
            option.textContent = branch.name || '';
            if (selectedBranchId && Number(branch.id) === Number(selectedBranchId)) {
                option.selected = true;
            }
            select.appendChild(option);
        });
    }

    function populateBranchFilterSelect(select, branches) {
        if (!select) return;
        select.textContent = '';
        const defaultBranchOption = document.createElement('option');
        defaultBranchOption.value = '';
        defaultBranchOption.textContent = 'Всі філії';
        select.appendChild(defaultBranchOption);
        branches.forEach(branch => {
            const option = document.createElement('option');
            option.value = branch.id;
            option.textContent = branch.name || '';
            select.appendChild(option);
        });
    }

    return {
        renderUsers,
        renderContainers,
        renderStorages,
        renderWithdrawalReasons,
        renderProducts,
        renderSources,
        renderClientTypes,
        renderFields,
        renderBranchPermissions,
        renderClientTypePermissions,
        populateUserSelect,
        populateUserFilterSelect,
        populateBranchSelect,
        populateBranchFilterSelect
    };
})();
