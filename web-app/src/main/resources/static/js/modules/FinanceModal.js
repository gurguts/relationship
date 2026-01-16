const FinanceModal = (function() {
    let deleteConfirmationModal = null;

    function openCreateTransactionModal(config) {
        const { onPopulateForm, onTypeChange, customSelects } = config;
        const modal = document.getElementById('create-transaction-modal');
        if (!modal) return;
        
        const fromAccountBalance = document.getElementById('from-account-balance');
        const toAccountBalance = document.getElementById('to-account-balance');
        const conversionAccountBalance = document.getElementById('conversion-account-balance');
        if (fromAccountBalance) fromAccountBalance.style.display = 'none';
        if (toAccountBalance) toAccountBalance.style.display = 'none';
        if (conversionAccountBalance) conversionAccountBalance.style.display = 'none';
        
        if (customSelects) {
            Object.values(customSelects).forEach(customSelect => {
                if (customSelect && customSelect.reset) {
                    customSelect.reset();
                }
            });
        }
        
        const allDropdowns = modal.querySelectorAll('.custom-select-dropdown.open');
        allDropdowns.forEach(dropdown => {
            dropdown.classList.remove('open');
        });
        
        if (onPopulateForm) {
            onPopulateForm();
        }
        
        document.body.style.overflow = 'hidden';
        modal.style.display = 'flex';
        setTimeout(() => {
            modal.classList.add('show');
        }, CLIENT_CONSTANTS.MODAL_ANIMATION_DELAY);
    }

    function closeCreateTransactionModal(config) {
        const { onResetForm } = config;
        const modal = document.getElementById('create-transaction-modal');
        if (!modal) return;
        
        modal.classList.remove('show');
        setTimeout(() => {
            modal.style.display = 'none';
            document.body.style.overflow = '';
            const form = document.getElementById('transaction-form');
            if (form) form.reset();
            
            const clientInput = document.getElementById('transaction-client');
            const clientHidden = document.getElementById('transaction-client-id');
            if (clientInput) clientInput.value = '';
            if (clientHidden) clientHidden.value = '';
            
            const fromAccountBalance = document.getElementById('from-account-balance');
            const toAccountBalance = document.getElementById('to-account-balance');
            const conversionAccountBalance = document.getElementById('conversion-account-balance');
            if (fromAccountBalance) fromAccountBalance.style.display = 'none';
            if (toAccountBalance) toAccountBalance.style.display = 'none';
            if (conversionAccountBalance) conversionAccountBalance.style.display = 'none';
            
            if (onResetForm) {
                onResetForm();
            }
        }, CLIENT_CONSTANTS.MODAL_CLOSE_DELAY);
    }

    function setupCreateTransactionModalHandlers(config) {
        const { onPopulateForm, onTypeChange, onResetForm, customSelects } = config;
        const modal = document.getElementById('create-transaction-modal');
        if (!modal) return;

        const closeBtn = modal.querySelector('.close');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                closeCreateTransactionModal({ onResetForm, customSelects });
            });
        }
    }

    async function openEditTransactionModal(transactionId, config) {
        const { onLoadTransaction, onReloadData } = config;
        try {
            const transaction = await FinanceDataLoader.loadTransaction(transactionId);
            
            const transactionIdInput = document.getElementById('edit-transaction-id');
            const transactionAmountInput = document.getElementById('edit-transaction-amount');
            const transactionDescriptionInput = document.getElementById('edit-transaction-description');
            
            if (transactionIdInput) transactionIdInput.value = transaction.id;
            if (transactionAmountInput) transactionAmountInput.value = transaction.amount || '';
            if (transactionDescriptionInput) transactionDescriptionInput.value = transaction.description || '';
            
            let typeStr;
            if (typeof transaction.type === 'string') {
                typeStr = transaction.type;
            } else if (transaction.type && transaction.type.name) {
                typeStr = transaction.type.name;
            } else if (transaction.type) {
                typeStr = transaction.type.toString();
            }
            
            const exchangeRateGroup = document.getElementById('edit-exchange-rate-group');
            const exchangeRateInput = document.getElementById('edit-transaction-exchange-rate');
            if (exchangeRateGroup && exchangeRateInput) {
                if (typeStr === 'CURRENCY_CONVERSION') {
                    exchangeRateGroup.style.display = 'block';
                    exchangeRateInput.value = transaction.exchangeRate || '';
                } else {
                    exchangeRateGroup.style.display = 'none';
                    exchangeRateInput.value = '';
                }
            }
            
            const commissionGroup = document.getElementById('edit-commission-group');
            const commissionInput = document.getElementById('edit-transaction-commission');
            if (commissionGroup && commissionInput) {
                if (typeStr === 'INTERNAL_TRANSFER') {
                    commissionGroup.style.display = 'block';
                    commissionInput.value = transaction.commission || '';
                } else {
                    commissionGroup.style.display = 'none';
                    commissionInput.value = '';
                }
            }
            
            const categorySelect = document.getElementById('edit-transaction-category');
            if (categorySelect) {
                categorySelect.textContent = '';
                const defaultOption = document.createElement('option');
                defaultOption.value = '';
                defaultOption.textContent = FINANCE_MESSAGES.WITHOUT_CATEGORY;
                categorySelect.appendChild(defaultOption);
            }
            
            if (typeStr) {
                const categories = await FinanceDataLoader.loadCategoriesForType(typeStr);
                if (categories && Array.isArray(categories)) {
                    categories.forEach(category => {
                        const option = document.createElement('option');
                        option.value = category.id;
                        option.textContent = category.name;
                        if (Number(transaction.categoryId) === Number(category.id)) {
                            option.selected = true;
                        }
                        categorySelect.appendChild(option);
                    });
                }
            }
            
            const editModal = document.getElementById('edit-transaction-modal');
            if (editModal) {
                editModal.style.display = 'flex';
                setTimeout(() => {
                    editModal.classList.add('show');
                }, CLIENT_CONSTANTS.MODAL_ANIMATION_DELAY);
            }
        } catch (error) {
            console.error('Error loading transaction:', error);
            handleError(error);
        }
    }

    function closeEditTransactionModal() {
        const modal = document.getElementById('edit-transaction-modal');
        if (!modal) return;
        modal.classList.remove('show');
        setTimeout(() => {
            modal.style.display = 'none';
            const form = document.getElementById('edit-transaction-form');
            if (form) form.reset();
        }, CLIENT_CONSTANTS.MODAL_CLOSE_DELAY);
    }

    function setupEditTransactionModalHandlers() {
        const modal = document.getElementById('edit-transaction-modal');
        if (!modal) return;
    }

    function showDeleteConfirmationModal(onConfirm, onCancel) {
        if (deleteConfirmationModal) {
            closeDeleteConfirmationModal();
        }

        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.id = 'delete-transaction-confirmation-modal';
        modal.style.display = 'flex';
        setTimeout(() => {
            modal.classList.add('show');
        }, CLIENT_CONSTANTS.MODAL_ANIMATION_DELAY);
        
        const modalContent = document.createElement('div');
        modalContent.className = 'modal-content';
        modalContent.style.maxWidth = '500px';
        
        const closeSpan = document.createElement('span');
        closeSpan.className = 'close';
        closeSpan.textContent = '×';
        closeSpan.addEventListener('click', () => closeDeleteConfirmationModal());
        modalContent.appendChild(closeSpan);
        
        const title = document.createElement('h2');
        title.textContent = FINANCE_MESSAGES.DELETE_CONFIRMATION_TITLE;
        modalContent.appendChild(title);
        
        const message = document.createElement('p');
        message.textContent = FINANCE_MESSAGES.DELETE_CONFIRMATION;
        message.style.marginBottom = '20px';
        modalContent.appendChild(message);
        
        const buttonsDiv = document.createElement('div');
        buttonsDiv.className = 'modal-buttons';
        buttonsDiv.style.display = 'flex';
        buttonsDiv.style.gap = '10px';
        buttonsDiv.style.justifyContent = 'flex-end';
        
        const confirmBtn = document.createElement('button');
        confirmBtn.type = 'button';
        confirmBtn.className = 'btn-primary';
        confirmBtn.textContent = FINANCE_MESSAGES.DELETE;
        confirmBtn.addEventListener('click', async () => {
            if (onConfirm) {
                try {
                    const result = onConfirm();
                    if (result instanceof Promise) {
                        await result;
                    }
                } catch (error) {
                    console.error('Error in delete confirmation:', error);
                }
            }
            closeDeleteConfirmationModal();
        });
        buttonsDiv.appendChild(confirmBtn);
        
        modalContent.appendChild(buttonsDiv);
        modal.appendChild(modalContent);
        document.body.appendChild(modal);
        
        deleteConfirmationModal = modal;
    }

    function closeDeleteConfirmationModal() {
        if (deleteConfirmationModal) {
            deleteConfirmationModal.classList.remove('show');
            setTimeout(() => {
                if (deleteConfirmationModal) {
                    deleteConfirmationModal.remove();
                    deleteConfirmationModal = null;
                }
            }, CLIENT_CONSTANTS.MODAL_CLOSE_DELAY);
        }
    }

    return {
        openCreateTransactionModal,
        closeCreateTransactionModal,
        setupCreateTransactionModalHandlers,
        openEditTransactionModal,
        closeEditTransactionModal,
        setupEditTransactionModalHandlers,
        showDeleteConfirmationModal,
        closeDeleteConfirmationModal
    };
})();
