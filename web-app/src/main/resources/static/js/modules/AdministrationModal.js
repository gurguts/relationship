const AdministrationModal = (function() {
    function openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'flex';
            modal.style.opacity = '1';
            modal.classList.add('show');
        }
    }

    function closeModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'none';
            modal.style.opacity = '0';
            modal.classList.remove('show');
        }
    }

    function setupModalClickHandlers() {
        document.querySelectorAll('.close-modal').forEach(closeBtn => {
            if (closeBtn._adminCloseHandler) {
                closeBtn.removeEventListener('click', closeBtn._adminCloseHandler);
            }
            
            closeBtn._adminCloseHandler = (e) => {
                e.stopPropagation();
                const modal = e.target.closest('.modal');
                if (modal) {
                    closeModal(modal.id);
                }
            };
            
            closeBtn.addEventListener('click', closeBtn._adminCloseHandler);
        });

        document.querySelectorAll('.close').forEach(closeBtn => {
            if (!closeBtn.classList.contains('close-modal')) {
                if (closeBtn._adminCloseHandler) {
                    closeBtn.removeEventListener('click', closeBtn._adminCloseHandler);
                }
                
                closeBtn._adminCloseHandler = (e) => {
                    e.stopPropagation();
                    const modal = e.target.closest('.modal');
                    if (modal) {
                        closeModal(modal.id);
                    }
                };
                
                closeBtn.addEventListener('click', closeBtn._adminCloseHandler);
            }
        });
        
        if (!window._adminModalWindowClickHandler) {
            window._adminModalWindowClickHandler = (e) => {
                if (e.target.classList.contains('modal')) {
                    closeModal(e.target.id);
                }
            };
            window.addEventListener('click', window._adminModalWindowClickHandler);
        }
    }

    function openEditContainerModal(container) {
        const modal = document.getElementById('edit-container-modal');
        if (!modal) return;
        
        document.getElementById('edit-container-id').value = container.id;
        document.getElementById('edit-container-name').value = container.name || '';
        openModal('edit-container-modal');
    }

    function openEditStorageModal(storage) {
        const modal = document.getElementById('edit-storage-modal');
        if (!modal) return;
        
        document.getElementById('edit-storage-id').value = storage.id;
        document.getElementById('edit-storage-name').value = storage.name || '';
        document.getElementById('edit-storage-description').value = storage.description || '';
        openModal('edit-storage-modal');
    }

    function openEditWithdrawalReasonModal(reason) {
        const modal = document.getElementById('edit-withdrawal-reason-modal');
        if (!modal) return;
        
        document.getElementById('edit-withdrawal-reason-id').value = reason.id;
        document.getElementById('edit-withdrawal-reason-name').value = reason.name || '';
        document.getElementById('edit-withdrawal-reason-purpose').value = reason.purpose || '';
        openModal('edit-withdrawal-reason-modal');
    }

    function openEditProductModal(productId) {
        openModal('edit-product-modal');
    }

    function populateProductForm(product) {
        if (!product) return;
        document.getElementById('edit-id').value = product.id || '';
        document.getElementById('edit-name').value = product.name || '';
        document.getElementById('edit-usage').value = product.usage || '';
    }

    function openCreateSourceModal() {
        document.getElementById('source-id').value = '';
        document.getElementById('source-modal-title').textContent = 'Створити джерело';
        document.getElementById('source-submit-btn').textContent = 'Створити';
        document.getElementById('source-name').value = '';
        document.getElementById('source-user').value = '';
        openModal('create-source-modal');
    }

    function openEditSourceModal(source) {
        if (!source) return;
        document.getElementById('source-id').value = source.id || '';
        document.getElementById('source-modal-title').textContent = 'Редагувати джерело';
        document.getElementById('source-submit-btn').textContent = 'Зберегти';
        document.getElementById('source-name').value = source.name || '';
        document.getElementById('source-user').value = source.userId ? String(source.userId) : '';
        openModal('create-source-modal');
    }

    function openCreateClientTypeModal() {
        openModal('add-client-type-modal');
    }

    function openEditClientTypeModal(clientType) {
        if (!clientType) return;
        document.getElementById('edit-client-type-id').value = clientType.id || '';
        document.getElementById('edit-client-type-name').value = clientType.name || '';
        document.getElementById('edit-client-type-name-field-label').value = clientType.nameFieldLabel || '';
        document.getElementById('edit-client-type-active').checked = clientType.isActive !== undefined ? clientType.isActive : true;
        openModal('edit-client-type-modal');
    }

    function openManageFieldsModal() {
        openModal('manage-fields-modal');
    }

    function openAddFieldModal() {
        document.getElementById('field-client-type-id').value = window.currentClientTypeId || '';
        openModal('add-field-modal');
    }

    function openEditFieldModal(field) {
        if (!field) return;
        
        document.getElementById('edit-field-id').value = field.id || '';
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
        
        document.getElementById('edit-field-list-values-group').style.display = 'none';
        document.getElementById('edit-field-validation-pattern-group').style.display = 'none';
        
        if (field.fieldType === 'LIST') {
            const listValuesText = field.listValues && field.listValues.length > 0 
                ? field.listValues.map(lv => lv.value || lv).join('\n')
                : '';
            document.getElementById('edit-field-list-values').value = listValuesText;
            document.getElementById('edit-field-list-values-group').style.display = 'block';
        }
        
        if (field.fieldType === 'PHONE') {
            document.getElementById('edit-field-validation-pattern-group').style.display = 'block';
        }
        
        openModal('edit-field-modal');
    }

    function openClientImportModal(clientTypeId) {
        document.getElementById('import-client-type-id').value = clientTypeId || '';
        openModal('client-import-modal');
    }

    function openStaticFieldsConfigModal(clientTypeId) {
        document.getElementById('static-fields-client-type-id').value = clientTypeId || '';
        openModal('static-fields-config-modal');
    }

    function openUserClientTypePermissionsModal(user) {
        document.getElementById('clientTypePermissionsUserName').textContent = `Користувач: ${user.fullName} (${user.login})`;
        openModal('userClientTypePermissionsModal');
    }

    function openUserPermissionsModal() {
        openModal('userPermissionsModal');
    }

    function closeUserPermissionsModal() {
        closeModal('userPermissionsModal');
    }

    function openBranchPermissionModal() {
        document.getElementById('branch-permission-id').value = '';
        document.getElementById('branch-permission-modal-title').textContent = 'Додати права доступу';
        document.getElementById('branch-permission-submit-btn').textContent = 'Додати';
        document.getElementById('branch-permission-user').value = '';
        document.getElementById('branch-permission-branch').value = '';
        document.getElementById('branch-permission-can-view').checked = true;
        document.getElementById('branch-permission-can-operate').checked = false;
        openModal('branch-permission-modal');
    }

    function openEditBranchPermissionModal(permission) {
        if (!permission) return;
        document.getElementById('branch-permission-id').value = permission.id || '';
        document.getElementById('branch-permission-user').value = permission.userId ? String(permission.userId) : '';
        document.getElementById('branch-permission-branch').value = permission.branchId ? String(permission.branchId) : '';
        document.getElementById('branch-permission-can-view').checked = permission.canView || false;
        document.getElementById('branch-permission-can-operate').checked = permission.canOperate || false;
        document.getElementById('branch-permission-modal-title').textContent = 'Редагувати права доступу';
        document.getElementById('branch-permission-submit-btn').textContent = 'Зберегти';
        openModal('branch-permission-modal');
    }

    return {
        openModal,
        closeModal,
        setupModalClickHandlers,
        openEditContainerModal,
        openEditStorageModal,
        openEditWithdrawalReasonModal,
        openEditProductModal,
        populateProductForm,
        openCreateSourceModal,
        openEditSourceModal,
        openCreateClientTypeModal,
        openEditClientTypeModal,
        openManageFieldsModal,
        openAddFieldModal,
        openEditFieldModal,
        openClientImportModal,
        openStaticFieldsConfigModal,
        openUserClientTypePermissionsModal,
        openUserPermissionsModal,
        closeUserPermissionsModal,
        openBranchPermissionModal,
        openEditBranchPermissionModal
    };
})();
