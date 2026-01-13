// Функция для заполнения формы экспорта базовыми и динамическими полями
function populateExportForm(formId) {
    const form = document.getElementById(formId);
    if (!form) return;
    
    // Очищаем форму от старых полей (кроме базовых)
    const existingFields = form.querySelectorAll('label');
    existingFields.forEach(label => {
        const input = label.querySelector('input[type="checkbox"]');
        if (input && !['id', 'company', 'createdAt', 'updatedAt', 'source'].includes(input.value)) {
            label.remove();
        }
    });
    
    // Базовые поля (всегда включены по умолчанию)
    const baseFields = [
        { value: 'id', label: 'Id' },
        { value: 'company', label: 'Компанія' },
        { value: 'createdAt', label: 'Дата створення' },
        { value: 'updatedAt', label: 'Дата оновлення' },
        { value: 'source', label: 'Залучення' }
    ];
    
    // Проверяем, есть ли уже базовые поля
    const existingBaseFields = new Set();
    form.querySelectorAll('input[type="checkbox"]').forEach(input => {
        existingBaseFields.add(input.value);
    });
    
    // Добавляем базовые поля, если их еще нет
    baseFields.forEach(field => {
        if (!existingBaseFields.has(field.value)) {
            const label = document.createElement('label');
            
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.name = 'fields';
            checkbox.value = field.value;
            checkbox.checked = true;
            label.appendChild(checkbox);
            
            label.appendChild(document.createTextNode(' ' + field.label));
            
            form.appendChild(label);
        }
    });
    
    // Добавляем все динамические поля типа клиента (не только видимые)
    // Используем window.clientTypeFields (все поля), если доступны, иначе window.visibleFields как fallback
    let allFields = [];
    if (typeof window.clientTypeFields !== 'undefined' && window.clientTypeFields && window.clientTypeFields.length > 0) {
        allFields = window.clientTypeFields;
    } else if (typeof window.visibleFields !== 'undefined' && window.visibleFields && window.visibleFields.length > 0) {
        allFields = window.visibleFields;
    }
    
    if (allFields.length > 0) {
        // Сортируем поля по displayOrder
        const sortedFields = [...allFields].sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        
        sortedFields.forEach(field => {
            // Используем fieldName как идентификатор для динамических полей
            const fieldValue = `field_${field.id}`;
            const fieldLabel = field.fieldLabel || field.fieldName;
            
            if (!form.querySelector(`input[value="${fieldValue}"]`)) {
                const label = document.createElement('label');
                
                const checkbox = document.createElement('input');
                checkbox.type = 'checkbox';
                checkbox.name = 'fields';
                checkbox.value = fieldValue;
                checkbox.checked = true;
                label.appendChild(checkbox);
                
                label.appendChild(document.createTextNode(' ' + (fieldLabel || '')));
                
                form.appendChild(label);
            }
        });
    }
}

function initExcelExport(config) {
    const {
        triggerId = 'exportToExcelData',
        modalId = 'exportModalData',
        cancelId = 'exportCancel',
        confirmId = 'exportConfirm',
        formId = 'exportFieldsForm',
        searchInputId = 'inputSearch',
        apiPath
    } = config;

    document.getElementById(triggerId).addEventListener('click', () => {
        const exportModal = document.getElementById(modalId);
        // Заполняем форму экспорта динамическими полями
        populateExportForm(formId);
        exportModal.classList.remove('hide');
        exportModal.style.display = 'flex';
        setTimeout(() => {
            exportModal.classList.add('show');
        }, 10);
    });

    // Close modal with cross button (close-export-modal)
    // Use event delegation to ensure it works even if element is added dynamically
    document.addEventListener('click', (event) => {
        if (event.target && event.target.id === 'close-export-modal') {
            const exportModal = document.getElementById(modalId);
            if (exportModal) {
                exportModal.classList.add('hide');
                exportModal.classList.remove('show');
                setTimeout(() => {
                    exportModal.style.display = 'none';
                }, 300);
            }
        }
    });

    // Keep backward compatibility with cancel button if it exists
    const cancelBtn = document.getElementById(cancelId);
    if (cancelBtn) {
        cancelBtn.addEventListener('click', () => {
            const exportModal = document.getElementById(modalId);
            exportModal.classList.add('hide');
            exportModal.classList.remove('show');
            setTimeout(() => {
                exportModal.style.display = 'none';
            }, 300);
        });
    }

    document.getElementById(confirmId).addEventListener('click', async () => {
        const exportModal = document.getElementById(modalId);
        const form = document.getElementById(formId);
        const selectedFields = Array.from(form.elements['fields'])
            .filter(field => field.checked)
            .map(field => field.value);

        exportModal.style.display = 'none';
        loaderBackdrop.style.display = 'flex';

        const searchInput = document.getElementById(searchInputId);
        const searchTerm = searchInput ? searchInput.value : '';
        let queryParams = `sort=${currentSort}&direction=${currentDirection}`;

        if (searchTerm) {
            queryParams += `&q=${encodeURIComponent(searchTerm)}`;
        }

        // Подготавливаем фильтры, добавляя clientTypeId если нужно
        const cleanedFilters = Object.assign({}, selectedFilters);
        if (typeof currentClientTypeId !== 'undefined' && currentClientTypeId) {
            cleanedFilters.clientTypeId = [currentClientTypeId.toString()];
        }
        
        if (Object.keys(cleanedFilters).length > 0) {
            queryParams += `&filters=${encodeURIComponent(JSON.stringify(cleanedFilters))}`;
        }

        try {
            const response = await fetch(`${apiPath}/export/excel?${queryParams}`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({fields: selectedFields})
            });

            if (!response.ok) {
                const errorData = await response.json();
                handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                return;
            }

            const blob = await response.blob();
            
            // Получаем имя файла из заголовка Content-Disposition
            let filename = 'client_data.xlsx';
            const contentDisposition = response.headers.get('Content-Disposition');
            if (contentDisposition) {
                // Сначала пытаемся получить filename* (RFC 5987) с кириллицей
                const filenameStarMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/);
                if (filenameStarMatch && filenameStarMatch[1]) {
                    try {
                        // Декодируем percent-encoded UTF-8
                        filename = decodeURIComponent(filenameStarMatch[1].replace(/['"]/g, ''));
                    } catch (e) {
                        // Если декодирование не удалось, пробуем обычный filename
                        const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                        if (filenameMatch && filenameMatch[1]) {
                            filename = filenameMatch[1].replace(/['"]/g, '');
                            try {
                                filename = decodeURIComponent(filename);
                            } catch (e2) {
                                // Если декодирование не удалось, используем как есть
                            }
                        }
                    }
                } else {
                    // Если filename* нет, используем обычный filename
                    const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                    if (filenameMatch && filenameMatch[1]) {
                        filename = filenameMatch[1].replace(/['"]/g, '');
                        try {
                            filename = decodeURIComponent(filename);
                        } catch (e) {
                            // Если декодирование не удалось, используем как есть
                        }
                    }
                }
            }
            
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            showMessage(CLIENT_MESSAGES.EXPORT_SUCCESS, 'info');
        } catch (error) {
            console.error('Помилка під час експорту в Excel:', error);
            handleError(error);
        } finally {
            loaderBackdrop.style.display = 'none';
        }
    });
}