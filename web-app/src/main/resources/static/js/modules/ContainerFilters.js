const ContainerFilters = (function() {
    let isBuildingFilters = false;
    
    function convertFieldNamesToFieldIds(filters, filterableFields, clientTypeFields) {
        const converted = { ...filters };
        const allFields = filterableFields && filterableFields.length > 0 ? filterableFields : 
                         (clientTypeFields && clientTypeFields.length > 0 ? clientTypeFields : []);
        
        if (allFields.length === 0) {
            return converted;
        }
        
        const fieldNameToIdMap = {};
        allFields.forEach(field => {
            if (field.fieldName && field.id) {
                fieldNameToIdMap[field.fieldName] = field.id;
            }
        });
        
        Object.keys(converted).forEach(key => {
            if (fieldNameToIdMap[key]) {
                const fieldId = fieldNameToIdMap[key];
                const newKey = `field_${fieldId}`;
                converted[newKey] = converted[key];
                delete converted[key];
            } else if (key.endsWith('From') || key.endsWith('To')) {
                const baseName = key.endsWith('From') ? key.slice(0, -4) : key.slice(0, -2);
                if (fieldNameToIdMap[baseName]) {
                    const fieldId = fieldNameToIdMap[baseName];
                    const suffix = key.endsWith('From') ? 'From' : 'To';
                    const newKey = `field_${fieldId}${suffix}`;
                    converted[newKey] = converted[key];
                    delete converted[key];
                }
            }
        });
        
        return converted;
    }
    
    function buildDynamicFilters(config) {
        const {
            filterForm,
            customSelects,
            selectedFilters,
            filterableFields,
            availableSources,
            availableUsers,
            availableContainers
        } = config;
        
        if (!filterForm) return;
        
        if (isBuildingFilters) {
            return;
        }
        isBuildingFilters = true;
        
        try {
            Object.keys(customSelects).forEach(selectId => {
                if (selectId.startsWith('filter-')) {
                    const customSelect = customSelects[selectId];
                    if (customSelect && typeof customSelect.reset === 'function') {
                        try {
                            customSelect.reset();
                        } catch (e) {
                            console.warn('Error resetting custom select:', e);
                        }
                    }
                    delete customSelects[selectId];
                }
            });

            const existingFilters = filterForm.querySelectorAll('h2, .filter-block, .select-section-item');
            existingFilters.forEach(el => {
                const selects = el.querySelectorAll('select');
                selects.forEach(sel => {
                    sel.innerHTML = '';
                });
                el.remove();
            });

            const containerH2 = document.createElement('h2');
            containerH2.textContent = 'Фільтри тари:';
            filterForm.appendChild(containerH2);

            const containerSelectItem = document.createElement('div');
            containerSelectItem.className = 'select-section-item';
            containerSelectItem.innerHTML = `
                <br>
                <label class="select-label-style" for="filter-container">Тип тари:</label>
                <select id="filter-container" name="container" multiple>
                </select>
            `;
            filterForm.appendChild(containerSelectItem);

            const userSelectItem = document.createElement('div');
            userSelectItem.className = 'select-section-item';
            userSelectItem.innerHTML = `
                <br>
                <label class="select-label-style" for="filter-user">Власник:</label>
                <select id="filter-user" name="user" multiple>
                </select>
            `;
            filterForm.appendChild(userSelectItem);

            const quantityH2 = document.createElement('h2');
            quantityH2.textContent = 'Кількість:';
            filterForm.appendChild(quantityH2);
            
            const quantityBlock = document.createElement('div');
            quantityBlock.className = 'filter-block';
            quantityBlock.innerHTML = `
                <label class="from-to-style" for="filter-quantity-from">Від:</label>
                <input type="number" id="filter-quantity-from" name="quantityFrom" step="0.01" placeholder="Мінімум">
                <label class="from-to-style" for="filter-quantity-to">До:</label>
                <input type="number" id="filter-quantity-to" name="quantityTo" step="0.01" placeholder="Максимум">
            `;
            filterForm.appendChild(quantityBlock);

            const updatedAtH2 = document.createElement('h2');
            updatedAtH2.textContent = 'Дата оновлення:';
            filterForm.appendChild(updatedAtH2);
            
            const updatedAtBlock = document.createElement('div');
            updatedAtBlock.className = 'filter-block';
            updatedAtBlock.innerHTML = `
                <label class="from-to-style" for="filter-updatedAt-from">Від:</label>
                <input type="date" id="filter-updatedAt-from" name="updatedAtFrom">
                <label class="from-to-style" for="filter-updatedAt-to">До:</label>
                <input type="date" id="filter-updatedAt-to" name="updatedAtTo">
            `;
            filterForm.appendChild(updatedAtBlock);

            const clientH2 = document.createElement('h2');
            clientH2.textContent = 'Фільтри клієнта:';
            filterForm.appendChild(clientH2);

            const clientCreatedAtH2 = document.createElement('h2');
            clientCreatedAtH2.textContent = 'Дата створення клієнта:';
            filterForm.appendChild(clientCreatedAtH2);
            
            const clientCreatedAtBlock = document.createElement('div');
            clientCreatedAtBlock.className = 'filter-block';
            clientCreatedAtBlock.innerHTML = `
                <label class="from-to-style" for="filter-clientCreatedAt-from">Від:</label>
                <input type="date" id="filter-clientCreatedAt-from" name="clientCreatedAtFrom">
                <label class="from-to-style" for="filter-clientCreatedAt-to">До:</label>
                <input type="date" id="filter-clientCreatedAt-to" name="clientCreatedAtTo">
            `;
            filterForm.appendChild(clientCreatedAtBlock);

            const clientUpdatedAtH2 = document.createElement('h2');
            clientUpdatedAtH2.textContent = 'Дата оновлення клієнта:';
            filterForm.appendChild(clientUpdatedAtH2);
            
            const clientUpdatedAtBlock = document.createElement('div');
            clientUpdatedAtBlock.className = 'filter-block';
            clientUpdatedAtBlock.innerHTML = `
                <label class="from-to-style" for="filter-clientUpdatedAt-from">Від:</label>
                <input type="date" id="filter-clientUpdatedAt-from" name="clientUpdatedAtFrom">
                <label class="from-to-style" for="filter-clientUpdatedAt-to">До:</label>
                <input type="date" id="filter-clientUpdatedAt-to" name="clientUpdatedAtTo">
            `;
            filterForm.appendChild(clientUpdatedAtBlock);

            const clientSourceSelectItem = document.createElement('div');
            clientSourceSelectItem.className = 'select-section-item';
            clientSourceSelectItem.innerHTML = `
                <br>
                <label class="select-label-style" for="filter-clientSource">Залучення клієнта:</label>
                <select id="filter-clientSource" name="clientSource" multiple>
                </select>
            `;
            filterForm.appendChild(clientSourceSelectItem);

            setTimeout(() => {
                const containerSelect = filterForm.querySelector('#filter-container');
                if (containerSelect && availableContainers && availableContainers.length > 0) {
                    const containerData = availableContainers.map(c => ({
                        id: c.id,
                        name: c.name
                    }));
                    if (typeof createCustomSelect === 'function') {
                        const customSelect = createCustomSelect(containerSelect, true);
                        if (customSelect) {
                            customSelects['filter-container'] = customSelect;
                            customSelect.populate(containerData);
                        }
                    }
                }

                const userSelect = filterForm.querySelector('#filter-user');
                if (userSelect && availableUsers && availableUsers.length > 0) {
                    const userData = availableUsers.map(u => ({
                        id: u.id,
                        name: u.name
                    }));
                    if (typeof createCustomSelect === 'function') {
                        const customSelect = createCustomSelect(userSelect, true);
                        if (customSelect) {
                            customSelects['filter-user'] = customSelect;
                            customSelect.populate(userData);
                        }
                    }
                }

                const clientSourceSelect = filterForm.querySelector('#filter-clientSource');
                if (clientSourceSelect && !customSelects['filter-clientSource'] && availableSources && availableSources.length > 0) {
                    const sourceData = availableSources.map(s => ({
                        id: s.id,
                        name: s.name
                    }));
                    if (typeof createCustomSelect === 'function') {
                        const customSelect = createCustomSelect(clientSourceSelect, true);
                        if (customSelect) {
                            customSelects['filter-clientSource'] = customSelect;
                            customSelect.populate(sourceData);
                        }
                    }
                }
            }, 0);

            if (filterableFields && filterableFields.length > 0) {
                filterableFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
                filterableFields.forEach(field => {
                    if (field.fieldType === 'DATE') {
                        const h2 = document.createElement('h2');
                        h2.textContent = field.fieldLabel + ':';
                        filterForm.appendChild(h2);
                        
                        const filterBlock = document.createElement('div');
                        filterBlock.className = 'filter-block';
                        filterBlock.innerHTML = `
                            <label class="from-to-style" for="filter-${ClientUtils.escapeHtml(field.fieldName)}-from">Від:</label>
                            <input type="date" id="filter-${ClientUtils.escapeHtml(field.fieldName)}-from" name="${ClientUtils.escapeHtml(field.fieldName)}From">
                            <label class="from-to-style" for="filter-${ClientUtils.escapeHtml(field.fieldName)}-to">До:</label>
                            <input type="date" id="filter-${ClientUtils.escapeHtml(field.fieldName)}-to" name="${ClientUtils.escapeHtml(field.fieldName)}To">
                        `;
                        filterForm.appendChild(filterBlock);
                    } else if (field.fieldType === 'NUMBER') {
                        const h2 = document.createElement('h2');
                        h2.textContent = field.fieldLabel + ':';
                        filterForm.appendChild(h2);
                        
                        const filterBlock = document.createElement('div');
                        filterBlock.className = 'filter-block';
                        filterBlock.innerHTML = `
                            <label class="from-to-style" for="filter-${ClientUtils.escapeHtml(field.fieldName)}-from">Від:</label>
                            <input type="number" id="filter-${ClientUtils.escapeHtml(field.fieldName)}-from" name="${ClientUtils.escapeHtml(field.fieldName)}From" step="any" placeholder="Мінімум">
                            <label class="from-to-style" for="filter-${ClientUtils.escapeHtml(field.fieldName)}-to">До:</label>
                            <input type="number" id="filter-${ClientUtils.escapeHtml(field.fieldName)}-to" name="${ClientUtils.escapeHtml(field.fieldName)}To" step="any" placeholder="Максимум">
                        `;
                        filterForm.appendChild(filterBlock);
                    } else if (field.fieldType === 'LIST') {
                        const selectId = `filter-${field.fieldName}`;

                        if (customSelects[selectId]) {
                            try {
                                const oldSelect = customSelects[selectId];
                                if (oldSelect && typeof oldSelect.reset === 'function') {
                                    oldSelect.reset();
                                }
                            } catch (e) {
                                console.warn('Error cleaning up old custom select:', e);
                            }
                            delete customSelects[selectId];
                        }

                        const existingContainer = document.querySelector(`.custom-select-container[data-for="${selectId}"]`);
                        if (existingContainer) {
                            existingContainer.remove();
                        }
                        
                        const selectItem = document.createElement('div');
                        selectItem.className = 'select-section-item';
                        selectItem.appendChild(document.createElement('br'));
                        
                        const label = document.createElement('label');
                        label.className = 'select-label-style';
                        label.setAttribute('for', `filter-${field.fieldName}`);
                        label.textContent = field.fieldLabel + ':';
                        selectItem.appendChild(label);
                        
                        const select = document.createElement('select');
                        select.id = `filter-${field.fieldName}`;
                        select.name = field.fieldName;
                        select.multiple = true;
                        selectItem.appendChild(select);
                        
                        filterForm.appendChild(selectItem);

                        if (field.listValues && field.listValues.length > 0) {
                            field.listValues.forEach(listValue => {
                                const option = document.createElement('option');
                                option.value = listValue.id;
                                option.textContent = listValue.value;
                                select.appendChild(option);
                            });
                        }

                        setTimeout(() => {
                            if (typeof createCustomSelect === 'function') {
                                const existingContainer = document.querySelector(`.custom-select-container[data-for="${selectId}"]`);
                                if (existingContainer) {
                                    console.warn('Custom select container already exists for:', selectId);
                                    return;
                                }
                                
                                const customSelect = createCustomSelect(select, true);
                                if (customSelect) {
                                    customSelects[selectId] = customSelect;
                                    
                                    if (field.listValues && field.listValues.length > 0) {
                                        const listData = field.listValues.map(lv => ({
                                            id: lv.id,
                                            name: lv.value
                                        }));
                                        customSelect.populate(listData);
                                        
                                        if (selectedFilters[field.fieldName]) {
                                            const savedValues = selectedFilters[field.fieldName];
                                            if (Array.isArray(savedValues) && savedValues.length > 0) {
                                                const validValues = savedValues.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                                                if (validValues.length > 0) {
                                                    setTimeout(() => {
                                                        customSelect.setValue(validValues);
                                                    }, 50);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }, 0);
                    } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                        const selectItem = document.createElement('div');
                        selectItem.className = 'select-section-item';
                        selectItem.innerHTML = `
                            <br>
                            <label class="select-label-style" for="filter-${ClientUtils.escapeHtml(field.fieldName)}">${ClientUtils.escapeHtml(field.fieldLabel)}:</label>
                            <input type="text" 
                                   id="filter-${ClientUtils.escapeHtml(field.fieldName)}" 
                                   name="${ClientUtils.escapeHtml(field.fieldName)}" 
                                   placeholder="Пошук...">
                        `;
                        filterForm.appendChild(selectItem);
                    } else if (field.fieldType === 'BOOLEAN') {
                        const selectItem = document.createElement('div');
                        selectItem.className = 'select-section-item';
                        selectItem.innerHTML = `
                            <br>
                            <label class="select-label-style" for="filter-${ClientUtils.escapeHtml(field.fieldName)}">${ClientUtils.escapeHtml(field.fieldLabel)}:</label>
                            <select id="filter-${ClientUtils.escapeHtml(field.fieldName)}" name="${ClientUtils.escapeHtml(field.fieldName)}">
                                <option value="">Всі</option>
                                <option value="true">Так</option>
                                <option value="false">Ні</option>
                            </select>
                        `;
                        filterForm.appendChild(selectItem);
                    }
                });
            }
        } finally {
            isBuildingFilters = false;
        }
    }
    
    function updateSelectedFilters(config) {
        const {
            selectedFilters,
            filterForm,
            customSelects,
            filterableFields
        } = config;
        
        Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);

        Object.keys(customSelects).forEach(selectId => {
            if (selectId.startsWith('filter-')) {
                const select = document.getElementById(selectId);
                if (select) {
                    const name = select.name;
                    const values = customSelects[selectId].getValue();
                    if (values.length > 0) {
                        selectedFilters[name] = values;
                    }
                }
            }
        });

        if (!filterForm) return;
        
        const formData = new FormData(filterForm);

        const quantityFrom = formData.get('quantityFrom');
        const quantityTo = formData.get('quantityTo');
        if (quantityFrom && quantityFrom.trim() !== '' && !isNaN(quantityFrom)) {
            selectedFilters['quantityFrom'] = [quantityFrom];
        }
        if (quantityTo && quantityTo.trim() !== '' && !isNaN(quantityTo)) {
            selectedFilters['quantityTo'] = [quantityTo];
        }

        const updatedAtFrom = formData.get('updatedAtFrom');
        const updatedAtTo = formData.get('updatedAtTo');
        if (updatedAtFrom && updatedAtFrom.trim() !== '') {
            selectedFilters['updatedAtFrom'] = [updatedAtFrom];
        }
        if (updatedAtTo && updatedAtTo.trim() !== '') {
            selectedFilters['updatedAtTo'] = [updatedAtTo];
        }

        const clientCreatedAtFrom = formData.get('clientCreatedAtFrom');
        const clientCreatedAtTo = formData.get('clientCreatedAtTo');
        if (clientCreatedAtFrom && clientCreatedAtFrom.trim() !== '') {
            selectedFilters['clientCreatedAtFrom'] = [clientCreatedAtFrom];
        }
        if (clientCreatedAtTo && clientCreatedAtTo.trim() !== '') {
            selectedFilters['clientCreatedAtTo'] = [clientCreatedAtTo];
        }

        const clientUpdatedAtFrom = formData.get('clientUpdatedAtFrom');
        const clientUpdatedAtTo = formData.get('clientUpdatedAtTo');
        if (clientUpdatedAtFrom && clientUpdatedAtFrom.trim() !== '') {
            selectedFilters['clientUpdatedAtFrom'] = [clientUpdatedAtFrom];
        }
        if (clientUpdatedAtTo && clientUpdatedAtTo.trim() !== '') {
            selectedFilters['clientUpdatedAtTo'] = [clientUpdatedAtTo];
        }

        const clientSourceSelect = filterForm.querySelector('#filter-clientSource');
        if (clientSourceSelect && customSelects['filter-clientSource']) {
            const selectedSources = customSelects['filter-clientSource'].getValue();
            const filteredSources = selectedSources.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
            if (filteredSources.length > 0) {
                selectedFilters['clientSource'] = filteredSources;
            }
        }

        if (filterableFields && filterableFields.length > 0) {
            filterableFields.forEach(field => {
                if (field.fieldType === 'DATE') {
                    const fromValue = formData.get(`${field.fieldName}From`);
                    const toValue = formData.get(`${field.fieldName}To`);
                    if (fromValue && fromValue.trim() !== '') {
                        selectedFilters[`${field.fieldName}From`] = [fromValue];
                    }
                    if (toValue && toValue.trim() !== '') {
                        selectedFilters[`${field.fieldName}To`] = [toValue];
                    }
                } else if (field.fieldType === 'NUMBER') {
                    const fromValue = formData.get(`${field.fieldName}From`);
                    const toValue = formData.get(`${field.fieldName}To`);
                    if (fromValue && fromValue.trim() !== '' && !isNaN(fromValue)) {
                        selectedFilters[`${field.fieldName}From`] = [fromValue];
                    }
                    if (toValue && toValue.trim() !== '' && !isNaN(toValue)) {
                        selectedFilters[`${field.fieldName}To`] = [toValue];
                    }
                } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                    const value = formData.get(field.fieldName);
                    if (value && value.trim() !== '') {
                        selectedFilters[field.fieldName] = [value];
                    }
                } else if (field.fieldType === 'BOOLEAN') {
                    const value = formData.get(field.fieldName);
                    if (value && value.trim() !== '') {
                        selectedFilters[field.fieldName] = [value];
                    }
                }
            });
        }

        localStorage.setItem('selectedFilters', JSON.stringify(selectedFilters));
    }
    
    function restoreFilterValues(config) {
        const {
            filterForm,
            selectedFilters,
            customSelects,
            filterableFields,
            availableSources,
            availableUsers,
            availableContainers
        } = config;
        
        if (!filterForm) return;
        
        const updatedAtFromKey = Object.keys(selectedFilters).find(key => key.toLowerCase() === 'updatedatfrom');
        if (updatedAtFromKey) {
            const fromInput = filterForm.querySelector('#filter-updatedAt-from');
            if (fromInput) {
                const value = Array.isArray(selectedFilters[updatedAtFromKey]) 
                    ? selectedFilters[updatedAtFromKey][0] 
                    : selectedFilters[updatedAtFromKey];
                if (value) fromInput.value = value;
            }
        }
        const updatedAtToKey = Object.keys(selectedFilters).find(key => key.toLowerCase() === 'updatedatto');
        if (updatedAtToKey) {
            const toInput = filterForm.querySelector('#filter-updatedAt-to');
            if (toInput) {
                const value = Array.isArray(selectedFilters[updatedAtToKey]) 
                    ? selectedFilters[updatedAtToKey][0] 
                    : selectedFilters[updatedAtToKey];
                if (value) toInput.value = value;
            }
        }

        const quantityFrom = selectedFilters['quantityFrom'];
        const quantityTo = selectedFilters['quantityTo'];
        if (quantityFrom) {
            const input = filterForm.querySelector('#filter-quantity-from');
            if (input) {
                const value = Array.isArray(quantityFrom) ? quantityFrom[0] : quantityFrom;
                if (value) input.value = value;
            }
        }
        if (quantityTo) {
            const input = filterForm.querySelector('#filter-quantity-to');
            if (input) {
                const value = Array.isArray(quantityTo) ? quantityTo[0] : quantityTo;
                if (value) input.value = value;
            }
        }

        const clientCreatedAtFrom = selectedFilters['clientCreatedAtFrom'];
        const clientCreatedAtTo = selectedFilters['clientCreatedAtTo'];
        if (clientCreatedAtFrom) {
            const input = filterForm.querySelector('#filter-clientCreatedAt-from');
            if (input) {
                const value = Array.isArray(clientCreatedAtFrom) ? clientCreatedAtFrom[0] : clientCreatedAtFrom;
                if (value) input.value = value;
            }
        }
        if (clientCreatedAtTo) {
            const input = filterForm.querySelector('#filter-clientCreatedAt-to');
            if (input) {
                const value = Array.isArray(clientCreatedAtTo) ? clientCreatedAtTo[0] : clientCreatedAtTo;
                if (value) input.value = value;
            }
        }

        const clientUpdatedAtFrom = selectedFilters['clientUpdatedAtFrom'];
        const clientUpdatedAtTo = selectedFilters['clientUpdatedAtTo'];
        if (clientUpdatedAtFrom) {
            const input = filterForm.querySelector('#filter-clientUpdatedAt-from');
            if (input) {
                const value = Array.isArray(clientUpdatedAtFrom) ? clientUpdatedAtFrom[0] : clientUpdatedAtFrom;
                if (value) input.value = value;
            }
        }
        if (clientUpdatedAtTo) {
            const input = filterForm.querySelector('#filter-clientUpdatedAt-to');
            if (input) {
                const value = Array.isArray(clientUpdatedAtTo) ? clientUpdatedAtTo[0] : clientUpdatedAtTo;
                if (value) input.value = value;
            }
        }

        setTimeout(() => {
            const containerSelect = filterForm.querySelector('#filter-container');
            if (containerSelect && !customSelects['filter-container'] && availableContainers && availableContainers.length > 0) {
                const containerData = availableContainers.map(c => ({
                    id: c.id,
                    name: c.name
                }));
                if (typeof createCustomSelect === 'function') {
                    const customSelect = createCustomSelect(containerSelect, true);
                    if (customSelect) {
                        customSelects['filter-container'] = customSelect;
                        customSelect.populate(containerData);
                        
                        if (selectedFilters['container']) {
                            const savedContainers = selectedFilters['container'];
                            if (Array.isArray(savedContainers) && savedContainers.length > 0) {
                                const validContainers = savedContainers.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                                if (validContainers.length > 0) {
                                    customSelect.setValue(validContainers);
                                }
                            }
                        }
                    }
                }
            }

            const userSelect = filterForm.querySelector('#filter-user');
            if (userSelect && !customSelects['filter-user'] && availableUsers && availableUsers.length > 0) {
                const userData = availableUsers.map(u => ({
                    id: u.id,
                    name: u.name
                }));
                if (typeof createCustomSelect === 'function') {
                    const customSelect = createCustomSelect(userSelect, true);
                    if (customSelect) {
                        customSelects['filter-user'] = customSelect;
                        customSelect.populate(userData);
                        
                        if (selectedFilters['user']) {
                            const savedUsers = selectedFilters['user'];
                            if (Array.isArray(savedUsers) && savedUsers.length > 0) {
                                const validUsers = savedUsers.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                                if (validUsers.length > 0) {
                                    customSelect.setValue(validUsers);
                                }
                            }
                        }
                    }
                }
            }

            const clientSourceSelect = filterForm.querySelector('#filter-clientSource');
            if (clientSourceSelect && !customSelects['filter-clientSource'] && availableSources && availableSources.length > 0) {
                const sourceData = availableSources.map(s => ({
                    id: s.id,
                    name: s.name
                }));
                if (typeof createCustomSelect === 'function') {
                    const customSelect = createCustomSelect(clientSourceSelect, true);
                    if (customSelect) {
                        customSelects['filter-clientSource'] = customSelect;
                        customSelect.populate(sourceData);
                        
                        if (selectedFilters['clientSource']) {
                            const savedSources = selectedFilters['clientSource'];
                            if (Array.isArray(savedSources) && savedSources.length > 0) {
                                const validSources = savedSources.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                                if (validSources.length > 0) {
                                    customSelect.setValue(validSources);
                                }
                            }
                        }
                    }
                }
            }

            if (filterableFields && filterableFields.length > 0) {
                filterableFields.forEach(field => {
                    const filterId = `filter-${field.fieldName}`;
                    if (field.fieldType === 'DATE') {
                        const fromInput = filterForm.querySelector(`#${filterId}-from`);
                        const toInput = filterForm.querySelector(`#${filterId}-to`);
                        if (fromInput && selectedFilters[`${field.fieldName}From`]) {
                            const value = Array.isArray(selectedFilters[`${field.fieldName}From`]) 
                                ? selectedFilters[`${field.fieldName}From`][0] 
                                : selectedFilters[`${field.fieldName}From`];
                            if (value) fromInput.value = value;
                        }
                        if (toInput && selectedFilters[`${field.fieldName}To`]) {
                            const value = Array.isArray(selectedFilters[`${field.fieldName}To`]) 
                                ? selectedFilters[`${field.fieldName}To`][0] 
                                : selectedFilters[`${field.fieldName}To`];
                            if (value) toInput.value = value;
                        }
                    } else if (field.fieldType === 'NUMBER') {
                        const fromInput = filterForm.querySelector(`#${filterId}-from`);
                        const toInput = filterForm.querySelector(`#${filterId}-to`);
                        if (fromInput && selectedFilters[`${field.fieldName}From`]) {
                            const value = Array.isArray(selectedFilters[`${field.fieldName}From`]) 
                                ? selectedFilters[`${field.fieldName}From`][0] 
                                : selectedFilters[`${field.fieldName}From`];
                            if (value) fromInput.value = value;
                        }
                        if (toInput && selectedFilters[`${field.fieldName}To`]) {
                            const value = Array.isArray(selectedFilters[`${field.fieldName}To`]) 
                                ? selectedFilters[`${field.fieldName}To`][0] 
                                : selectedFilters[`${field.fieldName}To`];
                            if (value) toInput.value = value;
                        }
                    } else if (field.fieldType === 'LIST') {
                        if (customSelects[filterId] && selectedFilters[field.fieldName]) {
                            const savedValues = selectedFilters[field.fieldName];
                            if (Array.isArray(savedValues) && savedValues.length > 0) {
                                const validValues = savedValues.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                                if (validValues.length > 0) {
                                    customSelects[filterId].setValue(validValues);
                                }
                            }
                        }
                    } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                        const input = filterForm.querySelector(`#${filterId}`);
                        if (input && selectedFilters[field.fieldName]) {
                            const value = Array.isArray(selectedFilters[field.fieldName]) 
                                ? selectedFilters[field.fieldName][0] 
                                : selectedFilters[field.fieldName];
                            if (value) input.value = value;
                        }
                    } else if (field.fieldType === 'BOOLEAN') {
                        const select = filterForm.querySelector(`#${filterId}`);
                        if (select && selectedFilters[field.fieldName]) {
                            const value = Array.isArray(selectedFilters[field.fieldName]) 
                                ? selectedFilters[field.fieldName][0] 
                                : selectedFilters[field.fieldName];
                            if (value) select.value = value;
                        }
                    }
                });
            }
        }, 100);
    }
    
    function restoreDynamicClientFields(config) {
        const {
            filterableFields,
            selectedFilters,
            customSelects
        } = config;
        
        if (!filterableFields || filterableFields.length === 0) return;
        
        filterableFields.forEach(field => {
            const filterId = `filter-${field.fieldName}`;
            if (field.fieldType === 'DATE') {
                const fromInput = document.getElementById(`${filterId}-from`);
                const toInput = document.getElementById(`${filterId}-to`);
                if (fromInput && selectedFilters[`${field.fieldName}From`]) {
                    fromInput.value = selectedFilters[`${field.fieldName}From`][0] || '';
                }
                if (toInput && selectedFilters[`${field.fieldName}To`]) {
                    toInput.value = selectedFilters[`${field.fieldName}To`][0] || '';
                }
            } else if (field.fieldType === 'NUMBER') {
                const fromInput = document.getElementById(`${filterId}-from`);
                const toInput = document.getElementById(`${filterId}-to`);
                if (fromInput && selectedFilters[`${field.fieldName}From`]) {
                    fromInput.value = selectedFilters[`${field.fieldName}From`][0] || '';
                }
                if (toInput && selectedFilters[`${field.fieldName}To`]) {
                    toInput.value = selectedFilters[`${field.fieldName}To`][0] || '';
                }
            } else if (field.fieldType === 'LIST') {
                if (customSelects[filterId] && selectedFilters[field.fieldName]) {
                    const savedValues = selectedFilters[field.fieldName];
                    if (Array.isArray(savedValues) && savedValues.length > 0) {
                        const validValues = savedValues.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                        if (validValues.length > 0) {
                            customSelects[filterId].setValue(validValues);
                        }
                    }
                }
            } else if (field.fieldType === 'BOOLEAN') {
                const select = document.getElementById(filterId);
                if (select && selectedFilters[field.fieldName] && selectedFilters[field.fieldName].length > 0) {
                    const savedValue = selectedFilters[field.fieldName][0];
                    if (savedValue && savedValue !== '' && savedValue !== 'null') {
                        select.value = savedValue;
                    }
                }
            } else if (field.fieldType === 'TEXT' || field.fieldType === 'PHONE') {
                const input = document.getElementById(filterId);
                if (input && selectedFilters[field.fieldName]) {
                    input.value = Array.isArray(selectedFilters[field.fieldName]) 
                        ? selectedFilters[field.fieldName][0] 
                        : selectedFilters[field.fieldName];
                }
            }
        });
    }
    
    return {
        convertFieldNamesToFieldIds,
        buildDynamicFilters,
        updateSelectedFilters,
        restoreFilterValues,
        restoreDynamicClientFields
    };
})();
