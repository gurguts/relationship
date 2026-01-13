const SettingsModal = (function() {
    function openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'flex';
            setTimeout(() => {
                modal.classList.add('show');
            }, 10);
        }
    }
    
    function closeModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.remove('show');
            setTimeout(() => {
                modal.style.display = 'none';
            }, 300);
        }
    }
    
    function setupModalClickHandlers() {
        document.querySelectorAll('.close').forEach(closeBtn => {
            if (closeBtn._settingsCloseHandler) {
                closeBtn.removeEventListener('click', closeBtn._settingsCloseHandler);
            }
            
            closeBtn._settingsCloseHandler = (e) => {
                e.stopPropagation();
                e.preventDefault();
                const modal = e.target.closest('.modal');
                if (modal) {
                    closeModal(modal.id);
                }
            };
            
            closeBtn.addEventListener('click', closeBtn._settingsCloseHandler);
        });
        
        document.querySelectorAll('.modal').forEach(modal => {
            if (modal._settingsModalClickHandler) {
                modal.removeEventListener('click', modal._settingsModalClickHandler);
            }
            
            modal._settingsModalClickHandler = (e) => {
                if (!modal.classList.contains('show')) {
                    return;
                }
                
                if (e.target === modal) {
                    const modalContent = modal.querySelector('.modal-content');
                    if (modalContent && modalContent.contains(e.target)) {
                        return;
                    }
                    closeModal(modal.id);
                }
            };
            
            modal.addEventListener('click', modal._settingsModalClickHandler);
        });
    }
    
    function openCreateCategoryModal() {
        document.getElementById('category-id').value = '';
        document.getElementById('category-modal-title').textContent = 'Створити категорію';
        document.getElementById('category-submit-btn').textContent = 'Створити';
        document.getElementById('category-active-group').style.display = 'none';
        const categoryTypeEl = document.getElementById('category-type');
        if (categoryTypeEl) categoryTypeEl.disabled = false;
        openModal('create-category-modal');
    }
    
    function populateCategoryForm(category) {
        if (!category) {
            document.getElementById('category-id').value = '';
            document.getElementById('category-type').value = '';
            document.getElementById('category-name').value = '';
            document.getElementById('category-description').value = '';
            document.getElementById('category-active').checked = true;
            return;
        }
        
        document.getElementById('category-id').value = category.id;
        const categoryTypeEl = document.getElementById('category-type');
        if (categoryTypeEl) {
            categoryTypeEl.value = category.type;
            categoryTypeEl.disabled = true;
        }
        document.getElementById('category-name').value = category.name || '';
        document.getElementById('category-description').value = category.description || '';
        document.getElementById('category-active').checked = category.isActive !== undefined ? category.isActive : true;
        document.getElementById('category-modal-title').textContent = 'Редагувати категорію';
        document.getElementById('category-submit-btn').textContent = 'Зберегти';
        document.getElementById('category-active-group').style.display = 'block';
    }
    
    function openCreateCounterpartyModal() {
        document.getElementById('counterparty-id').value = '';
        document.getElementById('counterparty-type').value = '';
        document.getElementById('counterparty-name').value = '';
        document.getElementById('counterparty-description').value = '';
        document.getElementById('counterparty-modal-title').textContent = 'Створити контрагента';
        document.getElementById('counterparty-submit-btn').textContent = 'Створити';
        openModal('create-counterparty-modal');
    }
    
    function populateCounterpartyForm(counterparty) {
        if (!counterparty) {
            document.getElementById('counterparty-id').value = '';
            document.getElementById('counterparty-type').value = '';
            document.getElementById('counterparty-name').value = '';
            document.getElementById('counterparty-description').value = '';
            return;
        }
        
        document.getElementById('counterparty-id').value = counterparty.id;
        document.getElementById('counterparty-type').value = counterparty.type;
        document.getElementById('counterparty-name').value = counterparty.name || '';
        document.getElementById('counterparty-description').value = counterparty.description || '';
        document.getElementById('counterparty-modal-title').textContent = 'Редагувати контрагента';
        document.getElementById('counterparty-submit-btn').textContent = 'Зберегти';
    }
    
    function openCreateBranchModal() {
        document.getElementById('branch-id').value = '';
        document.getElementById('branch-modal-title').textContent = 'Створити філію';
        document.getElementById('branch-submit-btn').textContent = 'Створити';
        const branchForm = document.getElementById('branch-form');
        if (branchForm) branchForm.reset();
        openModal('create-branch-modal');
    }
    
    function populateBranchForm(branch) {
        if (!branch) {
            document.getElementById('branch-id').value = '';
            document.getElementById('branch-name').value = '';
            document.getElementById('branch-description').value = '';
            return;
        }
        
        document.getElementById('branch-id').value = branch.id;
        document.getElementById('branch-name').value = branch.name || '';
        document.getElementById('branch-description').value = branch.description || '';
        document.getElementById('branch-modal-title').textContent = 'Редагувати філію';
        document.getElementById('branch-submit-btn').textContent = 'Зберегти';
    }
    
    function openCreateAccountModal() {
        document.getElementById('account-id').value = '';
        document.getElementById('account-modal-title').textContent = 'Створити рахунок';
        document.getElementById('account-submit-btn').textContent = 'Створити';
        const accountForm = document.getElementById('account-form');
        if (accountForm) accountForm.reset();
        document.querySelectorAll('input[name="currency"]').forEach(cb => {
            cb.checked = cb.value === 'UAH';
        });
        openModal('create-account-modal');
    }
    
    function populateAccountForm(account) {
        if (!account) {
            document.getElementById('account-id').value = '';
            document.getElementById('account-name').value = '';
            document.getElementById('account-description').value = '';
            document.getElementById('account-user').value = '';
            document.getElementById('account-branch').value = '';
            document.querySelectorAll('input[name="currency"]').forEach(cb => {
                cb.checked = cb.value === 'UAH';
            });
            return;
        }
        
        document.getElementById('account-id').value = account.id;
        document.getElementById('account-name').value = account.name || '';
        document.getElementById('account-description').value = account.description || '';
        document.getElementById('account-user').value = account.userId ? String(account.userId) : '';
        document.getElementById('account-branch').value = account.branchId ? String(account.branchId) : '';
        document.querySelectorAll('input[name="currency"]').forEach(cb => {
            cb.checked = account.currencies && account.currencies.includes(cb.value);
        });
        document.getElementById('account-modal-title').textContent = 'Редагувати рахунок';
        document.getElementById('account-submit-btn').textContent = 'Зберегти';
    }
    
    function openCreateVehicleSenderModal() {
        document.getElementById('vehicle-sender-id').value = '';
        document.getElementById('vehicle-sender-name').value = '';
        document.getElementById('vehicle-sender-modal-title').textContent = 'Створити відправника';
        document.getElementById('vehicle-sender-submit-btn').textContent = 'Створити';
        openModal('create-vehicle-sender-modal');
    }
    
    function populateVehicleSenderForm(sender) {
        if (!sender) {
            document.getElementById('vehicle-sender-id').value = '';
            document.getElementById('vehicle-sender-name').value = '';
            return;
        }
        
        document.getElementById('vehicle-sender-id').value = sender.id;
        document.getElementById('vehicle-sender-name').value = sender.name || '';
        document.getElementById('vehicle-sender-modal-title').textContent = 'Редагувати відправника';
        document.getElementById('vehicle-sender-submit-btn').textContent = 'Зберегти';
    }
    
    function openCreateVehicleReceiverModal() {
        document.getElementById('vehicle-receiver-id').value = '';
        document.getElementById('vehicle-receiver-name').value = '';
        document.getElementById('vehicle-receiver-modal-title').textContent = 'Створити отримувача';
        document.getElementById('vehicle-receiver-submit-btn').textContent = 'Створити';
        openModal('create-vehicle-receiver-modal');
    }
    
    function populateVehicleReceiverForm(receiver) {
        if (!receiver) {
            document.getElementById('vehicle-receiver-id').value = '';
            document.getElementById('vehicle-receiver-name').value = '';
            return;
        }
        
        document.getElementById('vehicle-receiver-id').value = receiver.id;
        document.getElementById('vehicle-receiver-name').value = receiver.name || '';
        document.getElementById('vehicle-receiver-modal-title').textContent = 'Редагувати отримувача';
        document.getElementById('vehicle-receiver-submit-btn').textContent = 'Зберегти';
    }
    
    function openCreateCarrierModal() {
        document.getElementById('carrier-id').value = '';
        document.getElementById('carrier-company-name').value = '';
        document.getElementById('carrier-registration-address').value = '';
        document.getElementById('carrier-phone-number').value = '';
        document.getElementById('carrier-code').value = '';
        document.getElementById('carrier-account').value = '';
        document.getElementById('carrier-form-title').textContent = 'Створити перевізника';
        document.getElementById('carrier-submit-btn').textContent = 'Створити';
        openModal('create-carrier-modal');
    }
    
    function openCreateVehicleTerminalModal() {
        document.getElementById('vehicle-terminal-id').value = '';
        document.getElementById('vehicle-terminal-name').value = '';
        document.getElementById('vehicle-terminal-modal-title').textContent = 'Створити термінал';
        document.getElementById('vehicle-terminal-submit-btn').textContent = 'Створити';
        openModal('create-vehicle-terminal-modal');
    }
    
    function populateVehicleTerminalForm(terminal) {
        if (!terminal) {
            document.getElementById('vehicle-terminal-id').value = '';
            document.getElementById('vehicle-terminal-name').value = '';
            return;
        }
        
        document.getElementById('vehicle-terminal-id').value = terminal.id;
        document.getElementById('vehicle-terminal-name').value = terminal.name || '';
        document.getElementById('vehicle-terminal-modal-title').textContent = 'Редагувати термінал';
        document.getElementById('vehicle-terminal-submit-btn').textContent = 'Зберегти';
    }
    
    function openCreateVehicleDestinationCountryModal() {
        document.getElementById('vehicle-destination-country-id').value = '';
        document.getElementById('vehicle-destination-country-name').value = '';
        document.getElementById('vehicle-destination-country-modal-title').textContent = 'Створити країну призначення';
        document.getElementById('vehicle-destination-country-submit-btn').textContent = 'Створити';
        openModal('create-vehicle-destination-country-modal');
    }
    
    function populateVehicleDestinationCountryForm(country) {
        if (!country) {
            document.getElementById('vehicle-destination-country-id').value = '';
            document.getElementById('vehicle-destination-country-name').value = '';
            return;
        }
        
        document.getElementById('vehicle-destination-country-id').value = country.id;
        document.getElementById('vehicle-destination-country-name').value = country.name || '';
        document.getElementById('vehicle-destination-country-modal-title').textContent = 'Редагувати країну призначення';
        document.getElementById('vehicle-destination-country-submit-btn').textContent = 'Зберегти';
    }
    
    function openCreateVehicleDestinationPlaceModal() {
        document.getElementById('vehicle-destination-place-id').value = '';
        document.getElementById('vehicle-destination-place-name').value = '';
        document.getElementById('vehicle-destination-place-modal-title').textContent = 'Створити місце призначення';
        document.getElementById('vehicle-destination-place-submit-btn').textContent = 'Створити';
        openModal('create-vehicle-destination-place-modal');
    }
    
    function populateVehicleDestinationPlaceForm(place) {
        if (!place) {
            document.getElementById('vehicle-destination-place-id').value = '';
            document.getElementById('vehicle-destination-place-name').value = '';
            return;
        }
        
        document.getElementById('vehicle-destination-place-id').value = place.id;
        document.getElementById('vehicle-destination-place-name').value = place.name || '';
        document.getElementById('vehicle-destination-place-modal-title').textContent = 'Редагувати місце призначення';
        document.getElementById('vehicle-destination-place-submit-btn').textContent = 'Зберегти';
    }
    
    function populateCarrierForm(carrier) {
        if (!carrier) {
            document.getElementById('carrier-id').value = '';
            document.getElementById('carrier-company-name').value = '';
            document.getElementById('carrier-registration-address').value = '';
            document.getElementById('carrier-phone-number').value = '';
            document.getElementById('carrier-code').value = '';
            document.getElementById('carrier-account').value = '';
            return;
        }
        
        document.getElementById('carrier-id').value = carrier.id;
        document.getElementById('carrier-company-name').value = carrier.companyName || '';
        document.getElementById('carrier-registration-address').value = carrier.registrationAddress || '';
        document.getElementById('carrier-phone-number').value = carrier.phoneNumber || '';
        document.getElementById('carrier-code').value = carrier.code || '';
        document.getElementById('carrier-account').value = carrier.account || '';
        document.getElementById('carrier-form-title').textContent = 'Редагувати перевізника';
        document.getElementById('carrier-submit-btn').textContent = 'Зберегти';
    }
    
    return {
        openModal,
        closeModal,
        setupModalClickHandlers,
        openCreateCategoryModal,
        populateCategoryForm,
        openCreateCounterpartyModal,
        populateCounterpartyForm,
        openCreateBranchModal,
        populateBranchForm,
        openCreateAccountModal,
        populateAccountForm,
        openCreateVehicleSenderModal,
        populateVehicleSenderForm,
        openCreateVehicleReceiverModal,
        populateVehicleReceiverForm,
        openCreateVehicleTerminalModal,
        populateVehicleTerminalForm,
        openCreateVehicleDestinationCountryModal,
        populateVehicleDestinationCountryForm,
        openCreateVehicleDestinationPlaceModal,
        populateVehicleDestinationPlaceForm,
        openCreateCarrierModal,
        populateCarrierForm
    };
})();
