const ClientModal = (function() {
    let modalState = {
        modalTimeoutId: null,
        modalCloseHandler: null,
        modalClickHandler: null
    };

    function closeModal(clientModal, closeModalClientBtn, editing) {
        if (modalState.modalTimeoutId !== null) {
            clearTimeout(modalState.modalTimeoutId);
            modalState.modalTimeoutId = null;
        }
        
        if (modalState.modalCloseHandler && closeModalClientBtn) {
            closeModalClientBtn.removeEventListener('click', modalState.modalCloseHandler);
            modalState.modalCloseHandler = null;
        }
        
        if (modalState.modalClickHandler) {
            window.removeEventListener('click', modalState.modalClickHandler);
            modalState.modalClickHandler = null;
        }
        
        if (clientModal) {
            clientModal.classList.remove('open');
            clientModal.style.display = 'none';
        }
    }

    function setupDeleteButtons(client, fullDeleteButton, deleteButton, restoreButton, clientModal, loaderBackdrop, loadDataWithSort, currentPage, pageSize, currentSort, currentDirection, availableSources) {
        if (!fullDeleteButton || !deleteButton || !restoreButton) return;

        const canDelete = ClientPermissions.canDeleteClient(client, availableSources);

        if (fullDeleteButton.style.display !== 'none' && !canDelete) {
            fullDeleteButton.style.display = 'none';
        }

        fullDeleteButton.dataset.originalDisplay = fullDeleteButton.style.display || 'block';
        
        fullDeleteButton.onclick = async () => {
            if (!confirm('Ви впевнені, що хочете повністю видалити цього клієнта з бази даних? Ця дія незворотна!')) {
                return;
            }
            
            if (loaderBackdrop) {
                loaderBackdrop.style.display = 'flex';
            }
            try {
                await ClientDataLoader.deleteClient(client.id, true);
                showMessage('Клієнт повністю видалений з бази даних', 'info');
                if (clientModal) {
                    clientModal.style.display = 'none';
                }
                loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
            } catch (error) {
                console.error('Помилка видалення клієнта:', error);
                handleError(error instanceof ErrorResponse ? error : new ErrorResponse('DELETE_ERROR', error.message || 'Failed to delete client'));
            } finally {
                if (loaderBackdrop) {
                    loaderBackdrop.style.display = 'none';
                }
            }
        };

        if (client.isActive === false) {
            deleteButton.style.display = 'none';
            deleteButton.dataset.originalDisplay = 'none';
            restoreButton.style.display = 'block';
            restoreButton.dataset.originalDisplay = 'block';
        } else {
            const displayValue = canDelete ? 'block' : 'none';
            deleteButton.style.display = displayValue;
            deleteButton.dataset.originalDisplay = displayValue;
            restoreButton.style.display = 'none';
            restoreButton.dataset.originalDisplay = 'none';
        }
        
        deleteButton.onclick = async () => {
            if (!confirm('Ви впевнені, що хочете деактивувати цього клієнта? Клієнт буде прихований, але залишиться в базі даних.')) {
                return;
            }
            
            if (loaderBackdrop) {
                loaderBackdrop.style.display = 'flex';
            }
            try {
                await ClientDataLoader.deleteClient(client.id, false);
                showMessage('Клієнт деактивовано (isActive = false)', 'info');
                if (clientModal) {
                    clientModal.style.display = 'none';
                }
                loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
            } catch (error) {
                console.error('Помилка деактивації клієнта:', error);
                handleError(error instanceof ErrorResponse ? error : new ErrorResponse('DEACTIVATE_ERROR', error.message || 'Failed to deactivate client'));
            } finally {
                if (loaderBackdrop) {
                    loaderBackdrop.style.display = 'none';
                }
            }
        };

        restoreButton.onclick = async () => {
            if (!confirm('Ви впевнені, що хочете відновити цього клієнта? Клієнт знову стане активним.')) {
                return;
            }

            if (loaderBackdrop) {
                loaderBackdrop.style.display = 'flex';
            }
            try {
                await ClientDataLoader.restoreClient(client.id);
                showMessage('Клієнт відновлено (isActive = true)', 'info');
                if (clientModal) {
                    clientModal.style.display = 'none';
                }
                loadDataWithSort(currentPage, pageSize, currentSort, currentDirection);
            } catch (error) {
                console.error('Помилка відновлення клієнта:', error);
                handleError(error instanceof ErrorResponse ? error : new ErrorResponse('RESTORE_ERROR', error.message || 'Failed to restore client'));
            } finally {
                if (loaderBackdrop) {
                    loaderBackdrop.style.display = 'none';
                }
            }
        };
    }

    function renderFieldValue(field, values, valueSpan) {
        if (values.length > 0) {
            if (field.allowMultiple) {
                values.forEach((v, index) => {
                    if (index > 0) {
                        valueSpan.appendChild(document.createElement('br'));
                    }
                    if (field.fieldType === 'PHONE') {
                        const phone = v.valueText || '';
                        if (phone) {
                            const phoneLink = document.createElement('a');
                            phoneLink.href = `tel:${phone}`;
                            phoneLink.textContent = phone;
                            valueSpan.appendChild(phoneLink);
                        }
                    } else {
                        const value = ClientFieldFormatter.formatFieldValueForModal(v, field);
                        if (value) {
                            valueSpan.appendChild(document.createTextNode(value));
                        }
                    }
                });
            } else {
                if (field.fieldType === 'PHONE') {
                    const phone = values[0].valueText || '';
                    if (phone) {
                        const phoneLink = document.createElement('a');
                        phoneLink.href = `tel:${phone}`;
                        phoneLink.textContent = phone;
                        valueSpan.appendChild(phoneLink);
                    } else {
                        valueSpan.className = 'empty-value';
                        valueSpan.textContent = CLIENT_MESSAGES.EMPTY_VALUE;
                    }
                } else {
                    const value = ClientFieldFormatter.formatFieldValueForModal(values[0], field);
                    if (value) {
                        valueSpan.appendChild(document.createTextNode(value));
                    } else {
                        valueSpan.className = 'empty-value';
                        valueSpan.textContent = CLIENT_MESSAGES.EMPTY_VALUE;
                    }
                }
            }
        } else {
            valueSpan.className = 'empty-value';
            valueSpan.textContent = CLIENT_MESSAGES.EMPTY_VALUE;
        }
    }

    function setupEditButtons(field, fieldP, client, availableSources, enableEditField) {
        const canEdit = ClientPermissions.canEditClient(client, availableSources);
        if (canEdit) {
            const editButton = document.createElement('button');
            editButton.className = 'edit-icon';
            editButton.setAttribute('data-field-id', field.id);
            editButton.setAttribute('title', 'Редагувати');
            editButton.onclick = () => enableEditField(field.id, field.fieldType, field.allowMultiple || false);
            const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            svg.setAttribute('viewBox', '0 0 24 24');
            const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            path.setAttribute('d', 'M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z');
            svg.appendChild(path);
            editButton.appendChild(svg);
            fieldP.appendChild(editButton);
        }
    }

    function setupCompanyAndSourceEditButtons(client, modalClientSource, availableSources, sourceMap) {
        ClientPermissions.canEditClient(client, availableSources);
        const canEditSourceField = ClientPermissions.canEditSource();
        const canEditCompanyField = ClientPermissions.canEditCompany(client, availableSources);

        const companyEditButton = document.querySelector(`button.edit-icon[onclick*="enableEdit('company')"]`);
        if (companyEditButton) {
            companyEditButton.style.display = canEditCompanyField ? '' : 'none';
        }

        const sourceEditButton = document.getElementById('edit-source');
        if (sourceEditButton) {
            sourceEditButton.style.display = canEditSourceField ? '' : 'none';
        }
    }

    async function showClientModal(client, config) {
        const {
            clientModal,
            closeModalClientBtn,
            modalClientId,
            modalClientCompany,
            modalClientSource,
            modalClientCreated,
            modalClientUpdated,
            fullDeleteButton,
            deleteButton,
            restoreButton,
            loaderBackdrop,
            currentClientTypeId,
            currentClientType,
            clientTypeFields,
            availableSources,
            sourceMap,
            loadDataWithSort,
            currentPage,
            pageSize,
            currentSort,
            currentDirection,
            enableEditField,
            editingState
        } = config;

        if (modalState.modalCloseHandler && closeModalClientBtn) {
            closeModalClientBtn.removeEventListener('click', modalState.modalCloseHandler);
            modalState.modalCloseHandler = null;
        }
        
        if (modalState.modalClickHandler) {
            window.removeEventListener('click', modalState.modalClickHandler);
            modalState.modalClickHandler = null;
        }
        
        if (modalState.modalTimeoutId !== null) {
            clearTimeout(modalState.modalTimeoutId);
            modalState.modalTimeoutId = null;
        }

        if (!clientModal) return;

        clientModal.setAttribute('data-client-id', client.id);

        if (modalClientId) {
            modalClientId.textContent = client.id;
        }
        
        const modalContent = document.querySelector('.modal-content-client');
        if (modalContent) {
            const existingFields = modalContent.querySelectorAll('p[data-field-id]');
            existingFields.forEach(el => el.remove());
        }
        
        const nameFieldLabel = currentClientType ? currentClientType.nameFieldLabel : 'Компанія';
        if (modalClientCompany && modalClientCompany.parentElement) {
            const strong = modalClientCompany.parentElement.querySelector('strong');
            if (strong) {
                strong.textContent = nameFieldLabel + ':';
            }
            modalClientCompany.textContent = client.company;
        }
        
        if (currentClientTypeId && clientTypeFields && clientTypeFields.length > 0) {
            let fieldValues = client._fieldValues || client.fieldValues;
            if (!fieldValues || fieldValues.length === 0) {
                fieldValues = await ClientDataLoader.loadClientFieldValues(client.id);
            }
            const fieldValuesMap = new Map();
            fieldValues.forEach(fv => {
                if (!fieldValuesMap.has(fv.fieldId)) {
                    fieldValuesMap.set(fv.fieldId, []);
                }
                fieldValuesMap.get(fv.fieldId).push(fv);
            });
            
            const companyP = modalClientCompany?.parentElement;
            if (companyP) {
                clientTypeFields.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
                
                let lastInsertedElement = companyP;
                clientTypeFields.forEach(field => {
                    const values = fieldValuesMap.get(field.id) || [];
                    const fieldP = document.createElement('p');
                    fieldP.setAttribute('data-field-id', field.id);
                    
                    const strong = document.createElement('strong');
                    strong.textContent = field.fieldLabel + ':';
                    fieldP.appendChild(strong);

                    const valueSpan = document.createElement('span');
                    valueSpan.id = `modal-field-${field.id}`;
                    
                    renderFieldValue(field, values, valueSpan);
                    fieldP.appendChild(valueSpan);

                    setupEditButtons(field, fieldP, client, availableSources, enableEditField);
                    
                    fieldP.setAttribute('data-field-type', field.fieldType);

                    lastInsertedElement.insertAdjacentElement('afterend', fieldP);
                    lastInsertedElement = fieldP;
                });
            }
        }

        const sourceElement = modalClientSource?.parentElement;
        if (sourceElement) {
            sourceElement.style.display = '';
            if (modalClientSource) {
                modalClientSource.textContent = ClientUtils.findNameByIdFromMap(sourceMap, client.sourceId);
            }
        }

        setupCompanyAndSourceEditButtons(client, modalClientSource, availableSources, sourceMap);
        
        if (modalClientCreated) {
            modalClientCreated.textContent = client.createdAt || '';
        }
        if (modalClientUpdated) {
            modalClientUpdated.textContent = client.updatedAt || '';
        }

        clientModal.style.display = 'flex';
        modalState.modalTimeoutId = setTimeout(() => {
            clientModal.classList.add('open');
            modalState.modalTimeoutId = null;
        }, CLIENT_CONSTANTS.MODAL_ANIMATION_DELAY);

        modalState.modalCloseHandler = () => {
            if (!editingState.editing) {
                clientModal.classList.remove('open');
                modalState.modalTimeoutId = setTimeout(() => {
                    closeModal(clientModal, closeModalClientBtn, editingState.editing);
                    modalState.modalTimeoutId = null;
                }, CLIENT_CONSTANTS.MODAL_CLOSE_DELAY);
            } else {
                showMessage(CLIENT_MESSAGES.SAVE_OR_CANCEL, 'error');
            }
        };
        
        if (closeModalClientBtn) {
            closeModalClientBtn.addEventListener('click', modalState.modalCloseHandler);
        }

        setupDeleteButtons(
            client,
            fullDeleteButton,
            deleteButton,
            restoreButton,
            clientModal,
            loaderBackdrop,
            loadDataWithSort,
            currentPage,
            pageSize,
            currentSort,
            currentDirection,
            availableSources
        );
    }

    return {
        showClientModal,
        closeModal: (clientModal, closeModalClientBtn, editing) => closeModal(clientModal, closeModalClientBtn, editing),
        getModalState: () => modalState
    };
})();
