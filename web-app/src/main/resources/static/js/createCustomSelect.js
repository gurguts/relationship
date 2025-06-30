function createCustomSelect(selectElement, isFilter = false) {
    const selectId = selectElement.id;
    const selectName = selectElement.name;
    const isMultiple = selectElement.multiple;
    const currentSelect = document.getElementById(selectId);
    if (!currentSelect) {
        console.error(`Select with id "${selectId}" not found in DOM`);
        return null;
    }

    const selectContainer = document.createElement('div');
    selectContainer.className = 'custom-select-container';
    selectContainer.dataset.for = selectId;

    const trigger = document.createElement('div');
    trigger.className = 'custom-select-trigger';
    trigger.tabIndex = 0;

    const tagsContainer = document.createElement('div');
    tagsContainer.className = 'custom-select-tags';

    const placeholderInput = document.createElement('input');
    placeholderInput.type = 'text';
    placeholderInput.className = 'custom-select-placeholder';
    placeholderInput.placeholder = 'Параметр не задано';
    placeholderInput.autocomplete = 'off';

    trigger.appendChild(tagsContainer);
    trigger.appendChild(placeholderInput);
    selectContainer.appendChild(trigger);

    const dropdown = document.createElement('div');
    dropdown.className = 'custom-select-dropdown';

    const searchWrapper = document.createElement('div');
    searchWrapper.className = 'custom-select-search';
    const searchInput = document.createElement('input');
    searchInput.type = 'text';
    searchInput.placeholder = 'Пошук...';
    searchInput.autocomplete = 'off';
    searchWrapper.appendChild(searchInput);
    dropdown.appendChild(searchWrapper);

    const hiddenInput = document.createElement('input');
    hiddenInput.type = 'hidden';
    hiddenInput.name = selectName;
    hiddenInput.id = `hidden-${selectId}`;

    const selectedValues = new Set(
        Array.from(currentSelect.selectedOptions).map(opt => opt.value)
    );
    let selectData = [];

    function debounce(fn, delay) {
        let timeout;
        return (...args) => {
            clearTimeout(timeout);
            timeout = setTimeout(() => fn(...args), delay);
        };
    }

    function updateFilterCounter() {
        if (!isFilter) return; // Вызывается только для фильтров
        const counterElement = document.querySelector(`[data-counter-for="${selectId}"]`);
        if (counterElement) {
            counterElement.textContent = selectedValues.size;
        }
    }

    function updateSelection() {
        requestAnimationFrame(() => {
            tagsContainer.innerHTML = '';
            placeholderInput.style.display = 'block';

            if (isMultiple) {
                selectedValues.forEach(value => {
                    const option = currentSelect.querySelector(`option[value="${value}"]`);
                    if (!option) return;
                    const tag = document.createElement('div');
                    tag.className = 'custom-select-tag';
                    tag.textContent = option.textContent;
                    tag.dataset.value = value;

                    const removeButton = document.createElement('button');
                    removeButton.type = 'button';
                    removeButton.className = 'custom-select-tag-remove';
                    removeButton.textContent = '×';
                    tag.appendChild(removeButton);
                    tagsContainer.appendChild(tag);
                });
                placeholderInput.value = '';
                placeholderInput.placeholder = selectedValues.size > 0 ? '' : 'Параметр не задано';
            } else {
                const value = Array.from(selectedValues)[0];
                if (value) {
                    const option = currentSelect.querySelector(`option[value="${value}"]`);
                    if (option) {
                        placeholderInput.value = option.textContent;
                    }
                } else {
                    placeholderInput.value = '';
                    placeholderInput.placeholder = 'Параметр не задано';
                }
            }
            if (isFilter) updateFilterCounter();
        });
    }

    function updateHiddenInput() {
        hiddenInput.value = isMultiple ? Array.from(selectedValues).join(',') : Array.from(selectedValues)[0] || '';
    }

    function populateDropdown(data, searchText = '') {
        const fragment = document.createDocumentFragment();
        const lowerSearch = searchText.toLowerCase().trim();
        // Для клиентских селектов показываем все опции, включая выбранные
        const availableOptions = isFilter
            ? data.filter(item => !selectedValues.has(String(item.id)) && item.nameLower.includes(lowerSearch))
            : data.filter(item => item.nameLower.includes(lowerSearch));
        availableOptions.forEach(item => {
            const option = document.createElement('div');
            option.className = 'custom-select-option';
            if (selectedValues.has(String(item.id))) {
                option.classList.add('selected'); // Выделяем выбранные опции
            }
            option.dataset.value = String(item.id);
            option.textContent = item.name;
            fragment.appendChild(option);
        });
        dropdown.innerHTML = '';
        dropdown.appendChild(fragment);
    }

    function sortAndFilterOptions(searchText) {
        const lowerSearch = searchText.toLowerCase().trim();
        const filtered = selectData.filter(item => item.nameLower.includes(lowerSearch));
        filtered.sort((a, b) => {
            const aName = a.nameLower;
            const bName = b.nameLower;
            const aStartsWith = aName.startsWith(lowerSearch);
            const bStartsWith = bName.startsWith(lowerSearch);
            if (aStartsWith && !bStartsWith) return -1;
            if (!aStartsWith && bStartsWith) return 1;
            return aName.localeCompare(bName);
        });
        populateDropdown(filtered, lowerSearch);
    }

    function selectOptionBySearch(searchText) {
        const lowerSearch = searchText.toLowerCase().trim();
        const matchingOption = selectData.find(item =>
            item.nameLower === lowerSearch || item.nameLower.startsWith(lowerSearch));
        if (matchingOption) {
            const value = String(matchingOption.id);
            const nativeOption = currentSelect.querySelector(`option[value="${value}"]`);
            if (nativeOption) {
                if (isMultiple) {
                    if (!selectedValues.has(value)) {
                        selectedValues.add(value);
                        nativeOption.selected = true;
                    }
                } else {
                    selectedValues.clear();
                    selectedValues.add(value);
                    nativeOption.selected = true;
                    Array.from(currentSelect.options).forEach(opt => opt.selected = opt.value === value);
                    currentSelect.value = value;
                    dropdown.classList.remove('open');
                }
                updateSelection();
                updateHiddenInput();
                populateDropdown(selectData);
                searchInput.value = '';
                placeholderInput.value = isMultiple ? '' : nativeOption.textContent;
            }
        }
    }

    const handleTriggerClick = (e) => {
        e.stopPropagation();
        dropdown.classList.toggle('open');
        if (dropdown.classList.contains('open')) {
            placeholderInput.focus();
            // Не очищаем placeholderInput для одиночного выбора
            if (isMultiple || isFilter) {
                placeholderInput.value = '';
            }
            populateDropdown(selectData);
        }
    };

    const handleOutsideClick = (e) => {
        if (!selectContainer.contains(e.target)) dropdown.classList.remove('open');
    };

    const handleSearchInput = debounce(() => {
        sortAndFilterOptions(searchInput.value);
    }, 200);

    const handlePlaceholderInput = debounce(() => {
        sortAndFilterOptions(placeholderInput.value);
        dropdown.classList.add('open');
    }, 200);

    const handleSearchKeydown = (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            selectOptionBySearch(searchInput.value);
        }
    };

    const handlePlaceholderKeydown = (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            selectOptionBySearch(placeholderInput.value);
        }
    };

    dropdown.addEventListener('click', (e) => {
        e.stopPropagation();
        const option = e.target.closest('.custom-select-option');
        if (option) {
            const value = option.dataset.value;
            const nativeOption = currentSelect.querySelector(`option[value="${value}"]`);
            if (!nativeOption) return;

            if (isMultiple) {
                if (selectedValues.has(value)) {
                    selectedValues.delete(value);
                    nativeOption.selected = false;
                } else {
                    selectedValues.add(value);
                    nativeOption.selected = true;
                }
            } else {
                selectedValues.clear();
                selectedValues.add(value);
                nativeOption.selected = true;
                Array.from(currentSelect.options).forEach(opt => opt.selected = opt.value === value);
                currentSelect.value = value;
                dropdown.classList.remove('open');
            }
            updateSelection();
            updateHiddenInput();
            populateDropdown(selectData);
            searchInput.value = '';
            placeholderInput.value = isMultiple ? '' : nativeOption.textContent;
        }
    });

    tagsContainer.addEventListener('click', (e) => {
        const removeButton = e.target.closest('.custom-select-tag-remove');
        if (!removeButton) return;
        e.stopPropagation();
        const tag = removeButton.parentElement;
        const value = tag.dataset.value;
        selectedValues.delete(value);
        const option = currentSelect.querySelector(`option[value="${value}"]`);
        if (option) option.selected = false;
        updateSelection();
        updateHiddenInput();
        populateDropdown(selectData);
    });

    trigger.addEventListener('click', handleTriggerClick);
    document.addEventListener('click', handleOutsideClick);
    searchInput.addEventListener('input', handleSearchInput);
    searchInput.addEventListener('keydown', handleSearchKeydown);
    placeholderInput.addEventListener('input', handlePlaceholderInput);
    placeholderInput.addEventListener('keydown', handlePlaceholderKeydown);

    selectContainer.appendChild(dropdown);
    selectContainer.appendChild(hiddenInput);
    selectElement.parentNode.insertBefore(selectContainer, selectElement);
    selectElement.style.display = 'none';

    return {
        populate: function (data) {
            selectData = data.map(item => ({
                id: String(item.id),
                name: item.name || '',
                nameLower: (item.name || '').toLowerCase()
            }));
            if (currentSelect.options.length === 0) {
                currentSelect.innerHTML = '';
                selectData.forEach(item => {
                    const option = document.createElement('option');
                    option.value = item.id;
                    option.text = item.name;
                    currentSelect.appendChild(option);
                });
            }
            populateDropdown(selectData);
            updateSelection();
        },
        setValue: function (values) {
            if (!Array.isArray(values)) values = [values];
            selectedValues.clear();
            values.forEach(value => {
                const option = currentSelect.querySelector(`option[value="${String(value)}"]`);
                if (option) {
                    selectedValues.add(String(value));
                    option.selected = true;
                }
            });
            updateSelection();
            updateHiddenInput();
            populateDropdown(selectData);
        },
        getValue: function () {
            return Array.from(selectedValues);
        },
        reset: function () {
            selectedValues.clear();
            Array.from(currentSelect.options).forEach(opt => opt.selected = false);
            updateSelection();
            updateHiddenInput();
            populateDropdown(selectData);
        }
    };
}

function populateSelect(selectId, data) {
    const select = document.getElementById(selectId);
    if (!select) {
        console.error(`Select with id "${selectId}" not found in DOM`);
        return;
    }

    select.innerHTML = '';

    if (!selectId.endsWith('-filter')) {
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.text = select.dataset.placeholder || 'Виберіть параметр';
        defaultOption.disabled = true;
        defaultOption.selected = true;
        select.appendChild(defaultOption);
    }

    data.forEach(item => {
        const option = document.createElement('option');
        option.value = String(item.id);
        option.text = item.name;
        select.appendChild(option);
    });

    const customSelectId = selectId.endsWith('-filter') ? `${selectId}` : `${selectId}-custom`;
    if (!customSelects[customSelectId]) {
        customSelects[customSelectId] = createCustomSelect(select, selectId.endsWith('-filter'));
    }
    customSelects[customSelectId].populate(data);

    if (!selectId.endsWith('-filter')) {
        let defaultValue = defaultValues[selectId];
        if (typeof defaultValue === 'function') {
            defaultValue = defaultValue();
        }
        if (defaultValue && data.some(item => String(item.id) === defaultValue)) {
            customSelects[customSelectId].setValue(defaultValue);
        }
    }
}