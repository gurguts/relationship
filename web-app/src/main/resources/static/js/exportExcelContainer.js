function populateExportFormContainer(formId) {
    const form = document.getElementById(formId);
    if (!form) return;
    
    const existingFields = form.querySelectorAll('label');
    existingFields.forEach(label => {
        const input = label.querySelector('input[type="checkbox"]');
        if (input && input.value.startsWith('field_')) {
            label.remove();
        }
    });
    
    let allFields = [];
    if (typeof window.clientTypeFields !== 'undefined' && window.clientTypeFields && window.clientTypeFields.length > 0) {
        allFields = window.clientTypeFields;
    } else if (typeof window.visibleFields !== 'undefined' && window.visibleFields && window.visibleFields.length > 0) {
        allFields = window.visibleFields;
    }
    
    if (allFields.length > 0) {
        const clientSection = form.querySelector('.export-fields-section:last-of-type');
        if (clientSection) {
            const sortedFields = [...allFields].sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
            
            sortedFields.forEach(field => {
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
                    
                    clientSection.appendChild(label);
                }
            });
        }
    }
}

function initExcelExportContainer(config) {
    const {
        triggerId = 'exportToExcelData',
        modalId = 'exportModalData',
        cancelId = 'exportCancel',
        confirmId = 'exportConfirm',
        formId = 'exportFieldsForm',
        searchInputId = 'inputSearch',
        apiPath
    } = config;

    const triggerButton = document.getElementById(triggerId);
    if (!triggerButton) return;

    triggerButton.addEventListener('click', () => {
        const exportModal = document.getElementById(modalId);
        if (!exportModal) return;
        
        populateExportFormContainer(formId);
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
    const cancelButton = document.getElementById(cancelId);
    if (cancelButton) {
        cancelButton.addEventListener('click', () => {
            const exportModal = document.getElementById(modalId);
            if (!exportModal) return;
            
            exportModal.classList.add('hide');
            exportModal.classList.remove('show');
            setTimeout(() => {
                exportModal.style.display = 'none';
            }, 300);
        });
    }

    const confirmButton = document.getElementById(confirmId);
    if (confirmButton) {
        confirmButton.addEventListener('click', async () => {
            const exportModal = document.getElementById(modalId);
            const form = document.getElementById(formId);
            if (!exportModal || !form) return;
            
            const selectedFields = Array.from(form.elements['fields'])
                .filter(field => field.checked)
                .map(field => field.value);

            exportModal.style.display = 'none';
            const loaderBackdrop = document.getElementById('loader-backdrop');
            if (loaderBackdrop) {
                loaderBackdrop.style.display = 'flex';
            }

            const searchInput = document.getElementById(searchInputId);
            const searchTerm = searchInput ? searchInput.value : '';
            let queryParams = `sort=${currentSort}&direction=${currentDirection}`;

            if (searchTerm) {
                queryParams += `&q=${encodeURIComponent(searchTerm)}`;
            }

            const filters = { ...selectedFilters };
            if (typeof currentClientTypeId !== 'undefined' && currentClientTypeId) {
                filters.clientTypeId = [currentClientTypeId.toString()];
            }

            const filterableFieldsRef = typeof window.filterableFields !== 'undefined' ? window.filterableFields : (typeof filterableFields !== 'undefined' ? filterableFields : []);
            const clientTypeFieldsRef = typeof window.clientTypeFields !== 'undefined' ? window.clientTypeFields : (typeof clientTypeFields !== 'undefined' ? clientTypeFields : []);
            const convertedFilters = ContainerFilters.convertFieldNamesToFieldIds(filters, filterableFieldsRef, clientTypeFieldsRef);
            
            const normalizedFilters = {};
            Object.keys(convertedFilters).forEach(key => {
                const lowerKey = key.toLowerCase();
                if (lowerKey === 'updatedatfrom') {
                    normalizedFilters['updatedAtFrom'] = convertedFilters[key];
                } else if (lowerKey === 'updatedatto') {
                    normalizedFilters['updatedAtTo'] = convertedFilters[key];
                } else {
                    normalizedFilters[key] = convertedFilters[key];
                }
            });
            
            if (Object.keys(normalizedFilters).length > 0) {
                queryParams += `&filters=${encodeURIComponent(JSON.stringify(normalizedFilters))}`;
            }

            try {
                const exportApiPath = apiPath.replace('/client', '');
                const response = await fetch(`${exportApiPath}/export/excel?${queryParams}`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({fields: selectedFields})
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    if (typeof handleError === 'function') {
                        handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                    } else {
                        console.error('Export error:', errorData);
                    }
                    return;
                }

                const blob = await response.blob();
                
                let filename = 'container_data.xlsx';
                const contentDisposition = response.headers.get('Content-Disposition');
                if (contentDisposition) {
                    const filenameStarMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/);
                    if (filenameStarMatch && filenameStarMatch[1]) {
                        try {
                            filename = decodeURIComponent(filenameStarMatch[1].replace(/['"]/g, ''));
                        } catch (e) {
                            const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                            if (filenameMatch && filenameMatch[1]) {
                                filename = filenameMatch[1].replace(/['"]/g, '');
                                try {
                                    filename = decodeURIComponent(filename);
                                } catch (e2) {
                                }
                            }
                        }
                    } else {
                        const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                        if (filenameMatch && filenameMatch[1]) {
                            filename = filenameMatch[1].replace(/['"]/g, '');
                            try {
                                filename = decodeURIComponent(filename);
                            } catch (e) {
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

                if (typeof showMessage === 'function') {
                    showMessage('Дані успішно експортовані в Excel', 'info');
                }
            } catch (error) {
                console.error('Помилка під час експорту в Excel:', error);
                if (typeof handleError === 'function') {
                    handleError(error);
                }
            } finally {
                if (loaderBackdrop) {
                    loaderBackdrop.style.display = 'none';
                }
            }
        });
    }
}

