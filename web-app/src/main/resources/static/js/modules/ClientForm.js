const ClientForm = (function() {
    function updatePhoneOutput(fieldId, value) {
        const outputDiv = document.getElementById(`output-${fieldId}`);
        if (!outputDiv) return;
        outputDiv.textContent = '';

        let formattedNumbers = value.split(',')
            .map(num => num.trim())
            .filter(num => num.length > 0)
            .map(normalizePhoneNumber)
            .filter(phone => validatePhoneNumber(phone));

        if (formattedNumbers.length > 0) {
            const formattedNumbersList = document.createElement('ul');
            formattedNumbersList.className = 'phone-numbers-list';
            formattedNumbers.forEach(num => {
                const listItem = document.createElement('li');
                listItem.className = 'phone-number-item';
                listItem.textContent = num;
                formattedNumbersList.appendChild(listItem);
            });
            outputDiv.appendChild(formattedNumbersList);
        }
    }
    
    function buildDynamicCreateForm(currentClientTypeId, currentClientType, visibleInCreateFields, availableSources, customSelects, customSelectTimeoutIds, defaultValues, onFormBuilt) {
        if (!currentClientTypeId) {
            return;
        }

        customSelectTimeoutIds.forEach(id => clearTimeout(id));
        customSelectTimeoutIds.length = 0;

        const form = document.getElementById('client-form');
        if (!form) return;
        form.textContent = '';

        const nameFieldLabel = currentClientType ? currentClientType.nameFieldLabel : 'Компанія';
        
        const nameFieldDiv = document.createElement('div');
        nameFieldDiv.className = 'form-group';
        const nameLabel = document.createElement('label');
        nameLabel.setAttribute('for', 'company');
        nameLabel.textContent = nameFieldLabel;
        const nameInput = document.createElement('input');
        nameInput.type = 'text';
        nameInput.id = 'company';
        nameInput.name = 'company';
        nameInput.required = true;
        nameInput.placeholder = nameFieldLabel;
        nameFieldDiv.appendChild(nameLabel);
        nameFieldDiv.appendChild(nameInput);
        form.appendChild(nameFieldDiv);

        if (visibleInCreateFields && visibleInCreateFields.length > 0) {
            visibleInCreateFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
            
            visibleInCreateFields.forEach((field) => {
                const fieldDiv = document.createElement('div');
                fieldDiv.className = 'form-group';
                fieldDiv.setAttribute('data-field-id', field.id);
                fieldDiv.setAttribute('data-field-type', field.fieldType);

                const label = document.createElement('label');
                label.setAttribute('for', `field-${field.id}`);
                label.textContent = field.fieldLabel + (field.isRequired ? ' *' : '');
                fieldDiv.appendChild(label);

                let input;
                if (field.fieldType === CLIENT_FIELD_TYPES.TEXT) {
                    input = document.createElement('input');
                    input.type = 'text';
                    input.id = `field-${field.id}`;
                    input.name = `field-${field.id}`;
                    input.required = field.isRequired || false;
                    input.placeholder = field.fieldLabel;
                } else if (field.fieldType === CLIENT_FIELD_TYPES.NUMBER) {
                    input = document.createElement('input');
                    input.type = 'number';
                    input.id = `field-${field.id}`;
                    input.name = `field-${field.id}`;
                    input.required = field.isRequired || false;
                    input.placeholder = field.fieldLabel;
                } else if (field.fieldType === CLIENT_FIELD_TYPES.DATE) {
                    input = document.createElement('input');
                    input.type = 'date';
                    input.id = `field-${field.id}`;
                    input.name = `field-${field.id}`;
                    input.required = field.isRequired || false;
                } else if (field.fieldType === CLIENT_FIELD_TYPES.PHONE) {
                    input = document.createElement('input');
                    input.type = 'tel';
                    input.id = `field-${field.id}`;
                    input.name = `field-${field.id}`;
                    input.required = field.isRequired || false;
                    input.placeholder = field.allowMultiple ? CLIENT_MESSAGES.PHONE_INPUT_PLACEHOLDER : 'Телефон (формат: +1234567890)';
                    input.title = field.allowMultiple 
                        ? 'Номери повинні починатися з + та містити від 1 до 15 цифр (формат E.164), розділяйте комою'
                        : 'Номер повинен починатися з + та містити від 1 до 15 цифр (формат E.164)';

                    const validatePhoneField = function() {
                        const value = this.value.trim();
                        if (!value) {
                            if (this.required) {
                                this.setCustomValidity('Це поле обов\'язкове для заповнення');
                            } else {
                                this.setCustomValidity('');
                            }
                            return;
                        }
                        
                        if (field.allowMultiple) {
                            const phones = value.split(',').map(p => p.trim()).filter(p => p);
                            if (phones.length === 0) {
                                this.setCustomValidity('Введіть хоча б один номер телефону');
                                return;
                            }
                            const normalizedPhones = phones.map(p => normalizePhoneNumber(p));
                            const invalidPhones = normalizedPhones.filter(p => !validatePhoneNumber(p));
                            if (invalidPhones.length > 0) {
                                this.setCustomValidity('Деякі номери мають некоректний формат. Використовуйте формат E.164: +1234567890');
                            } else {
                                this.setCustomValidity('');
                            }
                        } else {
                            const normalized = normalizePhoneNumber(value);
                            if (!validatePhoneNumber(normalized)) {
                                this.setCustomValidity('Номер має некоректний формат. Використовуйте формат E.164: +1234567890');
                            } else {
                                this.setCustomValidity('');
                            }
                        }
                    };
                    
                    input.addEventListener('blur', validatePhoneField);
                    input.addEventListener('input', function() {
                        if (this.value.trim() === '') {
                            this.setCustomValidity('');
                        }
                    });
                    
                    if (field.allowMultiple) {
                        const outputDiv = document.createElement('div');
                        outputDiv.id = `output-${field.id}`;
                        outputDiv.className = 'phone-output';
                        fieldDiv.appendChild(outputDiv);
                        input.addEventListener('input', () => updatePhoneOutput(field.id, input.value));
                    }
                } else if (field.fieldType === CLIENT_FIELD_TYPES.LIST) {
                    input = document.createElement('select');
                    input.id = `field-${field.id}`;
                    input.name = `field-${field.id}`;
                    input.required = field.isRequired || false;
                    if (field.allowMultiple) {
                        input.multiple = true;
                    }
                    if (field.listValues && field.listValues.length > 0) {
                        field.listValues.forEach(listValue => {
                            const option = document.createElement('option');
                            option.value = listValue.id;
                            option.textContent = listValue.value;
                            if (!field.allowMultiple) {
                                option.selected = false;
                            }
                            input.appendChild(option);
                        });
                    }
                    if (!field.allowMultiple) {
                        input.selectedIndex = -1;
                    }
                    fieldDiv.appendChild(input);
                    form.appendChild(fieldDiv);
                    const timeoutId = setTimeout(() => {
                        if (typeof createCustomSelect === 'function') {
                            const customSelect = createCustomSelect(input);
                            customSelects[`field-${field.id}`] = customSelect;
                            if (field.listValues && field.listValues.length > 0) {
                                const listData = field.listValues.map(lv => ({
                                    id: lv.id,
                                    name: lv.value
                                }));
                                customSelect.populate(listData);
                            }
                            if (!field.allowMultiple) {
                                customSelect.reset();
                            }
                        }
                    }, 0);
                    customSelectTimeoutIds.push(timeoutId);
                    return;
                } else if (field.fieldType === CLIENT_FIELD_TYPES.BOOLEAN) {
                    input = document.createElement('select');
                    input.id = `field-${field.id}`;
                    input.name = `field-${field.id}`;
                    input.required = field.isRequired || false;
                    const defaultOption = document.createElement('option');
                    defaultOption.value = '';
                    defaultOption.textContent = CLIENT_MESSAGES.SELECT_OPTION;
                    defaultOption.disabled = true;
                    defaultOption.selected = true;
                    input.appendChild(defaultOption);
                    const yesOption = document.createElement('option');
                    yesOption.value = 'true';
                    yesOption.textContent = CLIENT_MESSAGES.YES;
                    input.appendChild(yesOption);
                    const noOption = document.createElement('option');
                    noOption.value = 'false';
                    noOption.textContent = CLIENT_MESSAGES.NO;
                    input.appendChild(noOption);
                }
                fieldDiv.appendChild(input);
                form.appendChild(fieldDiv);
            });
        }

        const sourceFieldDiv = document.createElement('div');
        sourceFieldDiv.className = 'form-group';
        
        const sourceLabel = document.createElement('label');
        sourceLabel.setAttribute('for', 'source');
        sourceLabel.textContent = CLIENT_MESSAGES.SOURCE_LABEL + ' *';
        sourceFieldDiv.appendChild(sourceLabel);
        
        const sourceSelect = document.createElement('select');
        sourceSelect.id = 'source';
        sourceSelect.name = 'sourceId';
        const defaultSourceOption = document.createElement('option');
        defaultSourceOption.value = '';
        defaultSourceOption.textContent = CLIENT_MESSAGES.SELECT_OPTION;
        defaultSourceOption.selected = true;
        sourceSelect.appendChild(defaultSourceOption);
        
        availableSources.forEach(source => {
            const option = document.createElement('option');
            option.value = source.id;
            option.textContent = source.name;
            sourceSelect.appendChild(option);
        });
        
        const defaultSourceId = defaultValues.source ? defaultValues.source() : '';
        if (defaultSourceId && sourceSelect.querySelector(`option[value="${defaultSourceId}"]`)) {
            sourceSelect.value = defaultSourceId;
        }
        
        sourceFieldDiv.appendChild(sourceSelect);
        form.appendChild(sourceFieldDiv);

        const timeoutId = setTimeout(() => {
            if (typeof createCustomSelect === 'function') {
                const customSelect = createCustomSelect(sourceSelect);
                customSelects['source-custom'] = customSelect;
                const sourceData = availableSources.map(s => ({
                    id: s.id,
                    name: s.name
                }));
                customSelect.populate(sourceData);
                if (defaultSourceId) {
                    customSelect.setValue(defaultSourceId);
                }
            }
        }, 0);
        customSelectTimeoutIds.push(timeoutId);

        const submitButton = document.createElement('button');
        submitButton.type = 'submit';
        submitButton.id = 'save-button';
        submitButton.textContent = 'Зберегти';
        form.appendChild(submitButton);

        const companyInput = document.getElementById('company');
        if (companyInput) {
            const validateForm = () => {
                const isCompanyFilled = companyInput.value.trim() !== '';
                submitButton.disabled = !isCompanyFilled;
            };
            companyInput.addEventListener('input', validateForm);
            validateForm();
        }
        
        if (onFormBuilt) {
            onFormBuilt();
        }
    }
    
    function resetForm(currentClientTypeId, buildFormFn, customSelects, defaultValues) {
        const form = document.getElementById('client-form');
        if (!form) return;
        
        if (currentClientTypeId) {
            if (buildFormFn) {
                buildFormFn();
            }
        } else {
            form.reset();
            const sourceSelect = document.getElementById('source');
            if (sourceSelect) {
                sourceSelect.selectedIndex = 0;
                const customSelectId = 'source-custom';
                if (customSelects[customSelectId]) {
                    customSelects[customSelectId].reset();
                    const defaultSourceId = defaultValues.source ? defaultValues.source() : '';
                    if (defaultSourceId && sourceSelect.querySelector(`option[value="${defaultSourceId}"]`)) {
                        customSelects[customSelectId].setValue(defaultSourceId);
                    }
                }
            }
        }
    }
    
    return {
        updatePhoneOutput,
        buildDynamicCreateForm,
        resetForm
    };
})();
