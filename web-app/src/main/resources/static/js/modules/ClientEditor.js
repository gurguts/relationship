const ClientEditor = (function() {
    function showSaveCancelButtons() {
        const saveButton = document.getElementById('save-client');
        const cancelButton = document.getElementById('cancel-client');
        const deleteButton = document.getElementById('delete-client');
        const restoreButton = document.getElementById('restore-client');
        const fullDeleteButton = document.getElementById('full-delete-client');
        
        if (saveButton) saveButton.style.display = 'inline';
        if (cancelButton) cancelButton.style.display = 'inline';
        if (deleteButton) deleteButton.style.display = 'none';
        if (restoreButton) restoreButton.style.display = 'none';
        if (fullDeleteButton) fullDeleteButton.style.display = 'none';
    }

    function hideSaveCancelButtons() {
        const saveButton = document.getElementById('save-client');
        const cancelButton = document.getElementById('cancel-client');
        const deleteButton = document.getElementById('delete-client');
        const restoreButton = document.getElementById('restore-client');
        const fullDeleteButton = document.getElementById('full-delete-client');
        
        if (saveButton) saveButton.style.display = 'none';
        if (cancelButton) cancelButton.style.display = 'none';
        
        if (deleteButton && deleteButton.dataset.originalDisplay !== undefined) {
            deleteButton.style.display = deleteButton.dataset.originalDisplay;
        }
        if (restoreButton && restoreButton.dataset.originalDisplay !== undefined) {
            restoreButton.style.display = restoreButton.dataset.originalDisplay;
        }
        if (fullDeleteButton && fullDeleteButton.dataset.originalDisplay !== undefined) {
            fullDeleteButton.style.display = fullDeleteButton.dataset.originalDisplay;
        }
    }

    function enableEdit(fieldId, editingState) {
        showSaveCancelButtons();
        const field = document.getElementById(`modal-client-${fieldId}`);
        if (!field) return;
        const currentValue = field.innerText;
        
        const fieldP = field.closest('p');
        if (fieldP) {
            const editIcon = fieldP.querySelector('.edit-icon');
            if (editIcon) {
                editIcon.style.display = 'none';
            }
        }

        field.innerHTML = `<textarea id="edit-${fieldId}" class="edit-textarea">${currentValue}</textarea>`;
        editingState.editing = true;
    }

    function enableSelect(fieldId, options, editingState) {
        showSaveCancelButtons();
        const field = document.getElementById(`modal-client-${fieldId}`);
        if (!field) return;
        const currentValue = field.innerText;
        
        const fieldP = field.closest('p');
        if (fieldP) {
            const editIcon = fieldP.querySelector('.edit-icon');
            if (editIcon) {
                editIcon.style.display = 'none';
            }
        }
        
        field.innerHTML = `<select id="edit-${fieldId}"></select>`;
        const select = document.getElementById(`edit-${fieldId}`);
        if (select) {
            options.forEach(option => {
                const opt = document.createElement('option');
                opt.value = option.id;
                opt.text = option.name;
                if (option.name === currentValue) opt.selected = true;
                select.appendChild(opt);
            });
        }
        editingState.editing = true;
    }

    function cancelClientChanges(clientTypeFields, editingState) {
        const modal = document.getElementById('client-modal');
        if (!modal) return;
        
        const clientId = document.getElementById('modal-client-id');
        if (!clientId) return;
        
        const editedFields = modal.querySelectorAll('p.editing');
        editedFields.forEach(fieldP => {
            fieldP.classList.remove('editing');
            const fieldId = fieldP.querySelector('[data-field-id]')?.getAttribute('data-field-id');
            if (fieldId) {
                const field = clientTypeFields.find(f => f.id === parseInt(fieldId));
                if (field) {
                    const editInput = document.getElementById(`edit-field-${field.id}`);
                    if (editInput) {
                        const fieldSpan = document.createElement('span');
                        fieldSpan.id = `modal-field-${field.id}`;
                        const originalValue = editInput.dataset.originalValue || CLIENT_MESSAGES.EMPTY_VALUE;
                        fieldSpan.className = !originalValue || originalValue === CLIENT_MESSAGES.EMPTY_VALUE ? 'empty-value' : '';
                        fieldSpan.textContent = originalValue;
                        editInput.replaceWith(fieldSpan);
                    }
                }
            } else {
                const editInputs = fieldP.querySelectorAll('[id^="edit-"]');
                editInputs.forEach(input => {
                    const span = document.createElement('span');
                    span.id = input.id.replace('edit-', 'modal-client-');
                    span.textContent = input.dataset.originalValue || '';
                    input.replaceWith(span);
                });
            }
            
            const editIcon = fieldP.querySelector('.edit-icon');
            if (editIcon) {
                editIcon.style.display = '';
            }
        });
        
        modal.classList.remove('open');
        setTimeout(() => {
            modal.style.display = 'none';
        }, 300);
        editingState.editing = false;
        hideSaveCancelButtons();
    }

    function getSelectedId(selectedText, availableOptions) {
        const selectedOption = availableOptions.find(option => option.name === selectedText);
        return selectedOption ? selectedOption.id : null;
    }

    async function saveClientChanges(config) {
        const {
            loaderBackdrop,
            clientTypeFields,
            customSelects,
            availableSources,
            loadDataWithSort,
            currentPage,
            pageSize,
            currentSort,
            currentDirection,
            editingState
        } = config;

        if (loaderBackdrop) {
            loaderBackdrop.style.display = 'flex';
        }
        editingState.editing = false;
        hideSaveCancelButtons();
        
        const clientIdElement = document.getElementById('modal-client-id');
        if (!clientIdElement) return;
        const clientId = clientIdElement.textContent;
        
        const updatedClient = {
            id: clientId,
            company: document.getElementById('edit-company')?.value ||
                document.getElementById('modal-client-company')?.innerText,
            sourceId: document.getElementById('edit-source')?.value ||
                getSelectedId(document.getElementById('modal-client-source')?.innerText, availableSources)
        };

        const fieldValues = [];
        
        if (clientTypeFields && clientTypeFields.length > 0) {
            let currentFieldValues = [];
            try {
                currentFieldValues = await ClientDataLoader.loadClientFieldValues(clientId);
            } catch (error) {
                console.error('Error loading field values:', error);
            }
            
            const currentFieldValuesMap = new Map();
            currentFieldValues.forEach(fv => {
                if (!currentFieldValuesMap.has(fv.fieldId)) {
                    currentFieldValuesMap.set(fv.fieldId, []);
                }
                currentFieldValuesMap.get(fv.fieldId).push(fv);
            });
            
            clientTypeFields.forEach(field => {
                const editInput = document.getElementById(`edit-field-${field.id}`);
                
                if (editInput) {
                    if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                        const value = editInput.value.trim();
                        if (value) {
                            if (field.fieldType === 'PHONE' && field.allowMultiple) {
                                const phones = value.split(',').map(p => p.trim()).filter(p => p);
                                phones.forEach((phone, index) => {
                                    const normalizedPhone = normalizePhoneNumber(phone);
                                    if (validatePhoneNumber(normalizedPhone)) {
                                        fieldValues.push({
                                            fieldId: field.id,
                                            valueText: normalizedPhone,
                                            displayOrder: index
                                        });
                                    }
                                });
                            } else if (field.fieldType === 'PHONE' && !field.allowMultiple) {
                                const normalizedPhone = normalizePhoneNumber(value);
                                if (validatePhoneNumber(normalizedPhone)) {
                                    fieldValues.push({
                                        fieldId: field.id,
                                        valueText: normalizedPhone,
                                        displayOrder: 0
                                    });
                                }
                            } else {
                                fieldValues.push({
                                    fieldId: field.id,
                                    valueText: value,
                                    displayOrder: 0
                                });
                            }
                        }
                    } else if (field.fieldType === 'NUMBER') {
                        const value = editInput.value.trim();
                        if (value) {
                            fieldValues.push({
                                fieldId: field.id,
                                valueNumber: parseFloat(value),
                                displayOrder: 0
                            });
                        }
                    } else if (field.fieldType === 'DATE') {
                        const value = editInput.value;
                        if (value) {
                            fieldValues.push({
                                fieldId: field.id,
                                valueDate: value,
                                displayOrder: 0
                            });
                        }
                    } else if (field.fieldType === 'LIST') {
                        let selectedValues = [];
                        if (customSelects && customSelects[`edit-field-${field.id}`]) {
                            selectedValues = customSelects[`edit-field-${field.id}`].getValue();
                        } else if (editInput.selectedOptions) {
                            selectedValues = Array.from(editInput.selectedOptions).map(opt => opt.value);
                        }
                        
                        if (selectedValues && selectedValues.length > 0) {
                            selectedValues.forEach((listValueId, index) => {
                                if (listValueId) {
                                    fieldValues.push({
                                        fieldId: field.id,
                                        valueListId: parseInt(listValueId),
                                        displayOrder: index
                                    });
                                }
                            });
                        }
                    } else if (field.fieldType === 'BOOLEAN') {
                        const value = editInput.value;
                        if (value === 'true' || value === 'false') {
                            fieldValues.push({
                                fieldId: field.id,
                                valueBoolean: value === 'true',
                                displayOrder: 0
                            });
                        }
                    }
                } else {
                    const existingValues = currentFieldValuesMap.get(field.id);
                    if (existingValues && existingValues.length > 0) {
                        existingValues.forEach(fv => {
                            fieldValues.push({
                                fieldId: field.id,
                                valueText: fv.valueText,
                                valueNumber: fv.valueNumber,
                                valueDate: fv.valueDate,
                                valueBoolean: fv.valueBoolean,
                                valueListId: fv.valueListId,
                                displayOrder: fv.displayOrder || 0
                            });
                        });
                    }
                }
            });
        }
        
        updatedClient.fieldValues = fieldValues;

        try {
            await ClientDataLoader.updateClient(clientId, updatedClient);

            const modal = document.getElementById('client-modal');
            if (modal) {
                modal.classList.remove('open');
                setTimeout(() => {
                    modal.style.display = 'none';
                    loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
                    showMessage(CLIENT_MESSAGES.CLIENT_EDITED, 'info');
                }, 300);
            }
        } catch (error) {
            console.error('Помилка при редагуванні клієнта:', error);
            handleError(error instanceof ErrorResponse ? error : new ErrorResponse('UPDATE_ERROR', error.message || 'Failed to update client'));
        } finally {
            if (loaderBackdrop) {
                loaderBackdrop.style.display = 'none';
            }
        }
    }

    async function enableEditField(fieldId, fieldType, allowMultiple, config) {
        const {
            clientTypeFields,
            customSelects,
            editingState
        } = config;

        showSaveCancelButtons();
        const fieldSpan = document.getElementById(`modal-field-${fieldId}`);
        if (!fieldSpan) {
            console.warn(`Field span not found for fieldId: ${fieldId}`);
            return;
        }
        const fieldP = fieldSpan.closest('p');
        if (!fieldP) {
            console.warn(`Field paragraph not found for fieldId: ${fieldId}`);
            return;
        }
        
        const editIcon = fieldP.querySelector('.edit-icon');
        if (editIcon) {
            editIcon.style.display = 'none';
        }
        
        fieldP.classList.add('editing');
        let currentValue = fieldSpan.innerText.trim();
        const isEmpty = currentValue === CLIENT_MESSAGES.EMPTY_VALUE || !currentValue;
        
        let inputElement = null;
        const originalValue = currentValue;
        
        if (fieldType === 'TEXT') {
            inputElement = document.createElement('textarea');
            inputElement.className = 'edit-textarea';
            inputElement.id = `edit-field-${fieldId}`;
            inputElement.value = isEmpty ? '' : currentValue;
            inputElement.dataset.originalValue = originalValue;
            fieldSpan.replaceWith(inputElement);
        } else if (fieldType === 'NUMBER') {
            inputElement = document.createElement('input');
            inputElement.type = 'number';
            inputElement.id = `edit-field-${fieldId}`;
            inputElement.value = isEmpty ? '' : currentValue;
            inputElement.dataset.originalValue = originalValue;
            fieldSpan.replaceWith(inputElement);
        } else if (fieldType === 'DATE') {
            inputElement = document.createElement('input');
            inputElement.type = 'date';
            inputElement.id = `edit-field-${fieldId}`;
            inputElement.dataset.originalValue = originalValue;
            if (!isEmpty && currentValue) {
                const dateValue = new Date(currentValue);
                if (!isNaN(dateValue.getTime())) {
                    inputElement.value = dateValue.toISOString().split('T')[0];
                }
            }
            fieldSpan.replaceWith(inputElement);
        } else if (fieldType === 'PHONE') {
            inputElement = document.createElement('textarea');
            inputElement.className = 'edit-textarea';
            inputElement.id = `edit-field-${fieldId}`;
            inputElement.placeholder = 'Введіть номери телефонів через кому';
            inputElement.dataset.originalValue = originalValue;
            if (!isEmpty && currentValue) {
                const fieldSpanElement = document.getElementById(`modal-field-${fieldId}`);
                if (fieldSpanElement && fieldSpanElement.innerHTML) {
                    const phoneLinks = fieldSpanElement.innerHTML.match(/<a[^>]*href="tel:([^"]+)"[^>]*>([^<]+)<\/a>/g);
                    if (phoneLinks && phoneLinks.length > 0) {
                        const phones = phoneLinks.map(link => {
                            const hrefMatch = link.match(/href="tel:([^"]+)"/);
                            if (hrefMatch) {
                                return hrefMatch[1];
                            }
                            const textMatch = link.match(/>([^<]+)</);
                            return textMatch ? textMatch[1] : '';
                        }).filter(p => p);
                        inputElement.value = phones.join(', ');
                    } else {
                        const textContent = fieldSpanElement.textContent || fieldSpanElement.innerText || '';
                        const phones = textContent.split(/\s+/).filter(p => p.trim() && p.trim() !== '—');
                        inputElement.value = phones.join(', ');
                    }
                } else {
                    inputElement.value = currentValue.replace(/<[^>]+>/g, '').trim();
                }
            }
            fieldSpan.replaceWith(inputElement);
        } else if (fieldType === 'LIST') {
            const select = document.createElement('select');
            select.id = `edit-field-${fieldId}`;
            select.dataset.originalValue = originalValue;
            if (allowMultiple) {
                select.multiple = true;
            }
            select.style.width = '100%';
            select.style.minWidth = '200px';
            
            const field = clientTypeFields.find(f => f.id === fieldId);
            if (field && field.listValues && field.listValues.length > 0) {
                if (!allowMultiple) {
                    const defaultOption = document.createElement('option');
                    defaultOption.value = '';
                    defaultOption.textContent = CLIENT_MESSAGES.SELECT_OPTION;
                    defaultOption.disabled = true;
                    defaultOption.selected = isEmpty;
                    select.appendChild(defaultOption);
                }
                
                field.listValues.forEach(listValue => {
                    const option = document.createElement('option');
                    option.value = listValue.id;
                    option.textContent = listValue.value;
                    if (!isEmpty && currentValue.includes(listValue.value)) {
                        option.selected = true;
                    }
                    select.appendChild(option);
                });
            }
            
            fieldSpan.replaceWith(select);
            
            setTimeout(() => {
                if (typeof createCustomSelect === 'function') {
                    const customSelect = createCustomSelect(select);
                    if (customSelects) {
                        customSelects[`edit-field-${fieldId}`] = customSelect;
                    }
                    if (field && field.listValues && field.listValues.length > 0) {
                        const listData = field.listValues.map(lv => ({
                            id: lv.id,
                            name: lv.value
                        }));
                        customSelect.populate(listData);
                        if (!isEmpty) {
                            const selectedIds = [];
                            field.listValues.forEach(lv => {
                                if (currentValue.includes(lv.value)) {
                                    selectedIds.push(String(lv.id));
                                }
                            });
                            if (selectedIds.length > 0) {
                                customSelect.setValue(allowMultiple ? selectedIds : selectedIds[0]);
                            }
                        }
                    }
                }
            }, 0);
            editingState.editing = true;
            return;
        } else if (fieldType === 'BOOLEAN') {
            const select = document.createElement('select');
            select.id = `edit-field-${fieldId}`;
            select.dataset.originalValue = originalValue;
            select.style.width = '100%';
            select.style.minWidth = '200px';
            
            const yesOption = document.createElement('option');
            yesOption.value = 'true';
            yesOption.textContent = CLIENT_MESSAGES.YES;
            select.appendChild(yesOption);
            
            const noOption = document.createElement('option');
            noOption.value = 'false';
            noOption.textContent = CLIENT_MESSAGES.NO;
            select.appendChild(noOption);
            
            if (!isEmpty) {
                if (currentValue.toLowerCase().includes(CLIENT_MESSAGES.YES.toLowerCase()) || currentValue === 'true') {
                    yesOption.selected = true;
                } else {
                    noOption.selected = true;
                }
            }
            
            fieldSpan.replaceWith(select);
            editingState.editing = true;
            return;
        }
        
        if (inputElement) {
            setTimeout(() => {
                inputElement.focus();
                if (inputElement.setSelectionRange) {
                    inputElement.setSelectionRange(inputElement.value.length, inputElement.value.length);
                }
            }, 0);
        }
        
        editingState.editing = true;
    }

    return {
        enableEdit,
        enableSelect,
        enableEditField,
        saveClientChanges,
        cancelClientChanges,
        showSaveCancelButtons,
        hideSaveCancelButtons
    };
})();
