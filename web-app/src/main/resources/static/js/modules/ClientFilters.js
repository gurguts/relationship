const ClientFilters = (function() {
    function closeModalFilter(filterModal, modalContent, filterModalTimeoutIdRef) {
        if (!filterModal || !modalContent) return;
        if (filterModalTimeoutIdRef.current !== null) {
            clearTimeout(filterModalTimeoutIdRef.current);
            filterModalTimeoutIdRef.current = null;
        }
        filterModal.classList.add('closing');
        modalContent.classList.add('closing-content');

        filterModalTimeoutIdRef.current = setTimeout(() => {
            filterModal.style.display = 'none';
            filterModal.classList.remove('closing');
            modalContent.classList.remove('closing-content');
            filterModalTimeoutIdRef.current = null;
        }, CLIENT_CONSTANTS.MODAL_CLOSE_DELAY);
    }
    
    function updateFilterCounter(selectedFilters, filterCounter, filterCount) {
        if (!filterCounter || !filterCount) return;

        let totalFilters = 0;

        Object.keys(selectedFilters).forEach(key => {
            const value = selectedFilters[key];
            if (Array.isArray(value)) {
                const validValues = value.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                totalFilters += validValues.length;
            } else if (value !== null && value !== undefined && value !== '') {
                totalFilters += 1;
            }
        });

        if (totalFilters > 0) {
            filterCount.textContent = totalFilters;
            filterCounter.style.display = 'inline-flex';
        } else {
            filterCount.textContent = '0';
            filterCounter.style.display = 'none';
        }
    }
    
    function updateSelectedFilters(selectedFilters, filterForm, customSelects, filterableFields, onUpdate) {
        if (typeof selectedFilters === 'undefined') {
            window.selectedFilters = {};
        }

        Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);

        if (!filterForm) return;
        const formData = new FormData(filterForm);

        const createdAtFrom = formData.get('createdAtFrom');
        const createdAtTo = formData.get('createdAtTo');
        const updatedAtFrom = formData.get('updatedAtFrom');
        const updatedAtTo = formData.get('updatedAtTo');

        if (createdAtFrom) selectedFilters['createdAtFrom'] = [createdAtFrom];
        if (createdAtTo) selectedFilters['createdAtTo'] = [createdAtTo];
        if (updatedAtFrom) selectedFilters['updatedAtFrom'] = [updatedAtFrom];
        if (updatedAtTo) selectedFilters['updatedAtTo'] = [updatedAtTo];

        const sourceSelectId = 'filter-source';
        if (customSelects[sourceSelectId]) {
            const selectedSources = customSelects[sourceSelectId].getValue();
            const filteredSources = selectedSources.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
            if (filteredSources.length > 0) {
                selectedFilters['source'] = filteredSources;
            }
        }

        const showInactive = formData.get('showInactive');
        if (showInactive === 'true') {
            selectedFilters['showInactive'] = ['true'];
        }

        if (filterableFields && filterableFields.length > 0) {
            filterableFields.forEach(field => {
                if (field.fieldType === CLIENT_FIELD_TYPES.DATE) {
                    const fromValue = formData.get(`${field.fieldName}From`);
                    const toValue = formData.get(`${field.fieldName}To`);
                    if (fromValue) selectedFilters[`${field.fieldName}From`] = [fromValue];
                    if (toValue) selectedFilters[`${field.fieldName}To`] = [toValue];
                } else if (field.fieldType === CLIENT_FIELD_TYPES.NUMBER) {
                    const fromValue = formData.get(`${field.fieldName}From`);
                    const toValue = formData.get(`${field.fieldName}To`);
                    if (fromValue && fromValue.trim() !== '') {
                        selectedFilters[`${field.fieldName}From`] = [fromValue.trim()];
                    }
                    if (toValue && toValue.trim() !== '') {
                        selectedFilters[`${field.fieldName}To`] = [toValue.trim()];
                    }
                } else if (field.fieldType === CLIENT_FIELD_TYPES.LIST) {
                    const selectId = `filter-${field.fieldName}`;
                    if (customSelects[selectId]) {
                        const selectedValues = customSelects[selectId].getValue();
                        const filteredValues = selectedValues.filter(v => v !== null && v !== undefined && v !== '' && v !== 'null');
                        if (filteredValues.length > 0) {
                            selectedFilters[field.fieldName] = filteredValues;
                        }
                    }
                } else if (field.fieldType === CLIENT_FIELD_TYPES.TEXT || field.fieldType === CLIENT_FIELD_TYPES.PHONE) {
                    const value = formData.get(field.fieldName);
                    if (value && value.trim() !== '') {
                        selectedFilters[field.fieldName] = [value.trim()];
                    }
                } else if (field.fieldType === CLIENT_FIELD_TYPES.BOOLEAN) {
                    const value = formData.get(field.fieldName);
                    if (value && value !== '' && value !== 'null') {
                        selectedFilters[field.fieldName] = [value];
                    }
                }
            });
        }

        localStorage.setItem('selectedFilters', JSON.stringify(selectedFilters));
        if (onUpdate) {
            onUpdate();
        }
    }
    
    function clearFilters(selectedFilters, filterForm, customSelects, searchInput, onClear) {
        Object.keys(selectedFilters).forEach(key => delete selectedFilters[key]);

        if (filterForm) {
            filterForm.reset();
            Object.keys(customSelects).forEach(selectId => {
                if (selectId.startsWith('filter-')) {
                    if (customSelects[selectId] && typeof customSelects[selectId].reset === 'function') {
                        customSelects[selectId].reset();
                    } else if (customSelects[selectId] && typeof customSelects[selectId].setValue === 'function') {
                        customSelects[selectId].setValue([]);
                    }
                }
            });
        }

        if (searchInput) {
            searchInput.value = '';
        }

        localStorage.removeItem('selectedFilters');
        localStorage.removeItem('searchTerm');

        if (onClear) {
            onClear();
        }
    }
    
    return {
        closeModalFilter,
        updateFilterCounter,
        updateSelectedFilters,
        clearFilters
    };
})();
