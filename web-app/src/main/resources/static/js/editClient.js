function enableEdit(fieldId) {
    ClientEditor.enableEdit(fieldId, editingState);
}

function enableSelect(fieldId, options) {
    ClientEditor.enableSelect(fieldId, options, editingState);
}

function cancelClientChanges() {
    ClientEditor.cancelClientChanges(clientTypeFields, editingState);
}

async function saveClientChanges() {
    await ClientEditor.saveClientChanges({
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
    });
}

async function enableEditField(fieldId, fieldType, allowMultiple) {
    await ClientEditor.enableEditField(fieldId, fieldType, allowMultiple, {
        clientTypeFields,
        customSelects,
        editingState
    });
}