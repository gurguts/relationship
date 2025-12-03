/*--edit-client--*/

function enableEdit(fieldId) {
    showSaveCancelButtons();
    const field = document.getElementById(`modal-client-${fieldId}`);
    const currentValue = field.innerText;

    field.innerHTML = `<textarea id="edit-${fieldId}" class="edit-textarea">${currentValue}</textarea>`;

    editing = true;
}

function enableSelect(fieldId, options) {
    showSaveCancelButtons();
    const field = document.getElementById(`modal-client-${fieldId}`);
    const currentValue = field.innerText;
    field.innerHTML = `<select id="edit-${fieldId}"></select>`;
    const select = document.getElementById(`edit-${fieldId}`);
    options.forEach(option => {
        const opt = document.createElement('option');
        opt.value = option.id;
        opt.text = option.name;
        if (option.name === currentValue) opt.selected = true;
        select.appendChild(opt);
    });
    editing = true;
}

function showSaveCancelButtons() {
    document.getElementById('save-client').style.display = 'inline';
    document.getElementById('cancel-client').style.display = 'inline';
}

function hideSaveCancelButtons() {
    document.getElementById('save-client').style.display = 'none';
    document.getElementById('cancel-client').style.display = 'none';
}

function closeModal() {
    if (!editing) {
        const modal = document.getElementById('client-modal');
        modal.classList.add('closing');
        modal.classList.remove('open');

        setTimeout(() => {
            modal.style.display = 'none';
            modal.classList.remove('closing');
        }, 400);
    } else {
        showMessage('Збережіть або відмініть зміни', 'error')
    }
}

function cancelClientChanges() {
    const modal = document.getElementById('client-modal');
    const clientId = document.getElementById('modal-client-id').innerText;
    
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
                    const originalValue = editInput.dataset.originalValue || '—';
                    fieldSpan.className = !originalValue || originalValue === '—' ? 'empty-value' : '';
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
    });
    
    modal.classList.remove('open');
    setTimeout(() => {
        modal.style.display = 'none';
    }, 300);
    editing = false;
    hideSaveCancelButtons();
}

async function saveClientChanges() {
    loaderBackdrop.style.display = 'flex';
    editing = false;
    hideSaveCancelButtons();
    const clientId = document.getElementById('modal-client-id').innerText;
    const updatedClient = {
        id: clientId,
        company: document.getElementById('edit-company')?.value ||
            document.getElementById('modal-client-company').innerText,
        sourceId: document.getElementById('edit-source')?.value ||
            getSelectedId('modal-client-source', availableSources)
    };

    // Собираем все fieldValues - и отредактированные, и неотредактированные
    const fieldValues = [];
    
    if (typeof clientTypeFields !== 'undefined' && clientTypeFields.length > 0) {
        // Загружаем текущие значения клиента, если они еще не загружены
        let currentFieldValues = [];
        if (typeof loadClientFieldValues === 'function') {
            currentFieldValues = await loadClientFieldValues(clientId);
        }
        
        // Создаем Map для быстрого доступа к текущим значениям по fieldId
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
                // Поле было отредактировано - используем новое значение
                if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                    const value = editInput.value.trim();
                    if (value) {
                        if (field.fieldType === 'PHONE') {
                            const phones = value.split(',').map(p => p.trim()).filter(p => p);
                            phones.forEach((phone, index) => {
                                fieldValues.push({
                                    fieldId: field.id,
                                    valueText: phone,
                                    displayOrder: index
                                });
                            });
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
                    if (customSelects[`edit-field-${field.id}`]) {
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
                // Поле не было отредактировано - используем текущее значение из загруженных данных
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
    
    // Всегда отправляем fieldValues, даже если пустой массив (для очистки всех полей, если нужно)
    updatedClient.fieldValues = fieldValues;

    try {
        const response = await fetch(`/api/v1/client/${clientId}`, {
            method: 'PATCH',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(updatedClient),
        });

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        const modal = document.getElementById('client-modal');
        modal.classList.remove('open');
        setTimeout(() => {
            modal.style.display = 'none';
            loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
            showMessage('Клієнт успішно відредагований', 'info');
        }, 300);
    } catch (error) {
        console.error('Помилка при редагуванні клієнта:', error);
        handleError(error);
    } finally {
        loaderBackdrop.style.display = 'none';
    }
}

function getSelectedId(selectedText, availableOptions) {
    const selectedOption = availableOptions.find(option => option.name === selectedText);
    return selectedOption ? selectedOption.id : null;
}

async function enableEditField(fieldId, fieldType, allowMultiple) {
    showSaveCancelButtons();
    const fieldSpan = document.getElementById(`modal-field-${fieldId}`);
    const fieldP = fieldSpan.closest('p');
    if (!fieldP) return;
    
    fieldP.classList.add('editing');
    let currentValue = fieldSpan.innerText.trim();
    const isEmpty = currentValue === '—' || !currentValue;
    
    if (fieldSpan.innerHTML.includes('<a')) {
        const phoneLinks = currentValue.match(/>([^<]+)</g);
        if (phoneLinks) {
            currentValue = phoneLinks.map(link => link.replace(/[<>]/g, '')).join(', ');
        } else {
            currentValue = fieldSpan.textContent.trim();
        }
    }
    
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
            const phoneLinks = currentValue.match(/<a[^>]*>([^<]+)<\/a>/g);
            if (phoneLinks) {
                const phones = phoneLinks.map(link => {
                    const match = link.match(/>([^<]+)</);
                    return match ? match[1] : '';
                }).filter(p => p);
                inputElement.value = phones.join(', ');
            } else {
                inputElement.value = currentValue.replace(/<[^>]+>/g, '');
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
                defaultOption.textContent = 'Виберіть...';
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
                customSelects[`edit-field-${fieldId}`] = customSelect;
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
        return;
    } else if (fieldType === 'BOOLEAN') {
        const select = document.createElement('select');
        select.id = `edit-field-${fieldId}`;
        select.dataset.originalValue = originalValue;
        select.style.width = '100%';
        select.style.minWidth = '200px';
        
        const yesOption = document.createElement('option');
        yesOption.value = 'true';
        yesOption.textContent = 'Так';
        select.appendChild(yesOption);
        
        const noOption = document.createElement('option');
        noOption.value = 'false';
        noOption.textContent = 'Ні';
        select.appendChild(noOption);
        
        if (!isEmpty) {
            if (currentValue.toLowerCase().includes('так') || currentValue === 'true') {
                yesOption.selected = true;
            } else {
                noOption.selected = true;
            }
        }
        
        fieldSpan.replaceWith(select);
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
    
    editing = true;
}