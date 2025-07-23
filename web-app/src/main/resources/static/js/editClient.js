/*--edit-client--*/

function enableEdit(fieldId) {
    showSaveCancelButtons();
    const field = document.getElementById(`modal-client-${fieldId}`);
    const currentValue = field.innerText;

    if (fieldId === 'vat') {
        const isChecked = currentValue === 'true';
        field.innerHTML = `<input type="checkbox" id="edit-${fieldId}" ${isChecked ? 'checked' : ''} />`;
    } else {
        field.innerHTML = `<textarea id="edit-${fieldId}" class="edit-textarea">${currentValue}</textarea>`;
    }

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
    /*resetForm();*/
    document.getElementById('client-modal').style.display = 'none';
    editing = false;
    hideSaveCancelButtons()
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
        person: document.getElementById('edit-person')?.value ||
            document.getElementById('modal-client-person').innerText,
        phoneNumbers: document.getElementById('edit-phone')?.value.split(',') ||
            document.getElementById('modal-client-phone').innerText.split(','),
        location: document.getElementById('edit-location')?.value ||
            document.getElementById('modal-client-location').innerText,
        pricePurchase: document.getElementById('edit-price-purchase')?.value ||
            document.getElementById('modal-client-price-purchase').innerText,
        priceSale: document.getElementById('edit-price-sale')?.value ||
            document.getElementById('modal-client-price-sale').innerText,
        volumeMonth: document.getElementById('edit-volumeMonth')?.value ||
            document.getElementById('modal-client-volumeMonth').innerText,
        edrpou: document.getElementById('edit-edrpou')?.value ||
            document.getElementById('modal-client-edrpou').innerText,
        enterpriseName: document.getElementById('edit-enterpriseName')?.value ||
            document.getElementById('modal-client-enterpriseName').innerText,
        businessId: document.getElementById('edit-business')?.value ||
            getSelectedId('modal-client-business', availableBusiness),
        routeId: document.getElementById('edit-route')?.value ||
            getSelectedId('modal-client-route', availableRoutes),
        regionId: document.getElementById('edit-region')?.value ||
            getSelectedId('modal-client-region', availableRegions),
        statusId: document.getElementById('edit-status')?.value ||
            getSelectedId('modal-client-status', availableStatuses),
        sourceId: document.getElementById('edit-source')?.value ||
            getSelectedId('modal-client-source', availableSources),
        clientProductId: document.getElementById('edit-clientProduct')?.value ||
            getSelectedId('modal-client-clientProduct', availableClientProducts),
        comment: document.getElementById('edit-comment')?.value ||
            document.getElementById('modal-client-comment').innerText,
        vat: document.getElementById('edit-vat').checked
    };

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

        document.getElementById('client-modal').style.display = 'none';
        loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
        showMessage('Клієнт успішно відредагований', 'info');
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