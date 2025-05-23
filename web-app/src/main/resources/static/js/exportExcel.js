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
        exportModal.classList.remove('hide');
        exportModal.style.display = 'flex';
        setTimeout(() => {
            exportModal.classList.add('show');
        }, 10);
    });

    document.getElementById(cancelId).addEventListener('click', () => {
        const exportModal = document.getElementById(modalId);
        exportModal.classList.add('hide');
        exportModal.classList.remove('show');
        setTimeout(() => {
            exportModal.style.display = 'none';
        }, 300);
    });

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

        if (Object.keys(selectedFilters).length > 0) {
            queryParams += `&filters=${encodeURIComponent(JSON.stringify(selectedFilters))}`;
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
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = 'client_data.xlsx';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            showMessage('Дані успішно експортовані в Excel', 'info');
        } catch (error) {
            console.error('Помилка під час експорту в Excel:', error);
            handleError(error);
        } finally {
            loaderBackdrop.style.display = 'none';
        }
    });
}