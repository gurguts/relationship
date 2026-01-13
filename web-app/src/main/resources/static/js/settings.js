let categoriesCache = [];
let branchesCache = [];
let accountsCache = [];
let usersCache = [];
let loadingStates = {
    categories: false,
    counterparties: false,
    branches: false,
    accounts: false,
    vehicleSenders: false,
    vehicleReceivers: false,
    vehicleTerminals: false,
    vehicleDestinationCountries: false,
    vehicleDestinationPlaces: false,
    carriers: false
};

document.addEventListener('DOMContentLoaded', () => {
    initializeTabs();
    SettingsModal.setupModalClickHandlers();
    setupEventListeners();
    loadCategories();
});

function initializeTabs() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.getAttribute('data-tab');

            tabButtons.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));

            btn.classList.add('active');
            document.getElementById(`${targetTab}-tab`)?.classList.add('active');

            if (targetTab === 'finance-categories' && !loadingStates.categories) {
                loadCategories();
            } else if (targetTab === 'counterparties' && !loadingStates.counterparties) {
                loadCounterparties();
            } else if (targetTab === 'branches' && !loadingStates.branches) {
                loadBranches();
            } else if (targetTab === 'accounts' && !loadingStates.accounts) {
                loadAccounts();
            } else if (targetTab === 'vehicle-senders' && !loadingStates.vehicleSenders) {
                loadVehicleSenders();
            } else if (targetTab === 'vehicle-receivers' && !loadingStates.vehicleReceivers) {
                loadVehicleReceivers();
            } else if (targetTab === 'vehicle-terminals' && !loadingStates.vehicleTerminals) {
                loadVehicleTerminals();
            } else if (targetTab === 'vehicle-destination-countries' && !loadingStates.vehicleDestinationCountries) {
                loadVehicleDestinationCountries();
            } else if (targetTab === 'vehicle-destination-places' && !loadingStates.vehicleDestinationPlaces) {
                loadVehicleDestinationPlaces();
            } else if (targetTab === 'carriers' && !loadingStates.carriers) {
                loadCarriers();
            }
        });
    });
}

function setupEventListeners() {
    document.getElementById('create-category-btn')?.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        SettingsModal.openCreateCategoryModal();
    });

    document.getElementById('category-form')?.addEventListener('submit', handleCreateCategory);
    document.getElementById('category-type-filter')?.addEventListener('change', loadCategories);
    document.getElementById('create-counterparty-btn')?.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        SettingsModal.openCreateCounterpartyModal();
    });
    document.getElementById('counterparty-form')?.addEventListener('submit', handleCreateCounterparty);
    document.getElementById('counterparty-type-filter')?.addEventListener('change', loadCounterparties);
    document.getElementById('create-branch-btn')?.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        SettingsModal.openCreateBranchModal();
    });
    document.getElementById('create-account-btn')?.addEventListener('click', async (e) => {
        e.preventDefault();
        e.stopPropagation();
        await openCreateAccountModal();
    });
    document.getElementById('branch-form')?.addEventListener('submit', handleCreateBranch);
    document.getElementById('account-form')?.addEventListener('submit', handleCreateAccount);
    document.getElementById('create-vehicle-sender-btn')?.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        SettingsModal.openCreateVehicleSenderModal();
    });
    document.getElementById('vehicle-sender-form')?.addEventListener('submit', handleCreateVehicleSender);
    document.getElementById('create-vehicle-receiver-btn')?.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        SettingsModal.openCreateVehicleReceiverModal();
    });
    document.getElementById('vehicle-receiver-form')?.addEventListener('submit', handleCreateVehicleReceiver);
    document.getElementById('create-vehicle-terminal-btn')?.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        SettingsModal.openCreateVehicleTerminalModal();
    });
    document.getElementById('vehicle-terminal-form')?.addEventListener('submit', handleCreateVehicleTerminal);
    document.getElementById('create-vehicle-destination-country-btn')?.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        SettingsModal.openCreateVehicleDestinationCountryModal();
    });
    document.getElementById('vehicle-destination-country-form')?.addEventListener('submit', handleCreateVehicleDestinationCountry);
    document.getElementById('create-vehicle-destination-place-btn')?.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        SettingsModal.openCreateVehicleDestinationPlaceModal();
    });
    document.getElementById('vehicle-destination-place-form')?.addEventListener('submit', handleCreateVehicleDestinationPlace);
    document.getElementById('create-carrier-btn')?.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        SettingsModal.openCreateCarrierModal();
    });
    document.getElementById('carrier-form')?.addEventListener('submit', handleCreateCarrier);
}

async function loadCounterparties() {
    if (loadingStates.counterparties) return;
    loadingStates.counterparties = true;
    try {
        const typeFilter = document.getElementById('counterparty-type-filter')?.value;
        const counterparties = await SettingsDataLoader.loadCounterparties(typeFilter);
        SettingsRenderer.renderCounterparties(counterparties);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.counterparties = false;
    }
}

async function handleCreateCounterparty(e) {
    e.preventDefault();
    const id = document.getElementById('counterparty-id')?.value;
    const type = document.getElementById('counterparty-type')?.value;
    const name = document.getElementById('counterparty-name')?.value.trim();
    const description = document.getElementById('counterparty-description')?.value.trim();
    
    if (!type || !name) {
        showMessage('Заповніть всі обов\'язкові поля', 'error');
        return;
    }
    
    try {
        const data = { type, name, description };
        if (id) {
            await SettingsDataLoader.updateCounterparty(id, data);
            showMessage('Контрагента оновлено успішно', 'success');
        } else {
            await SettingsDataLoader.createCounterparty(data);
            showMessage('Контрагента створено успішно', 'success');
        }
        SettingsModal.closeModal('create-counterparty-modal');
        document.getElementById('counterparty-form')?.reset();
        document.getElementById('counterparty-id').value = '';
        loadCounterparties();
    } catch (error) {
        handleError(error);
    }
}

window.editCounterparty = async function editCounterparty(id) {
    try {
        const cp = await SettingsDataLoader.loadCounterparty(id);
        SettingsModal.populateCounterpartyForm(cp);
        SettingsModal.openModal('create-counterparty-modal');
    } catch (error) {
        handleError(error);
    }
};

window.deleteCounterparty = function deleteCounterparty(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_COUNTERPARTY,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await SettingsDataLoader.deleteCounterparty(id);
                showMessage('Контрагента успішно видалено', 'success');
                loadCounterparties();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadCategories() {
    if (loadingStates.categories) return;
    loadingStates.categories = true;
    try {
        const typeFilter = document.getElementById('category-type-filter')?.value;
        const categories = await SettingsDataLoader.loadCategories(typeFilter);
        categoriesCache = categories;
        SettingsRenderer.renderCategories(categories);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.categories = false;
    }
}

async function handleCreateCategory(e) {
    e.preventDefault();
    const categoryId = document.getElementById('category-id')?.value;
    const formData = {
        name: document.getElementById('category-name')?.value?.trim(),
        description: document.getElementById('category-description')?.value?.trim()
    };

    if (!categoryId) {
        formData.type = document.getElementById('category-type')?.value;
    } else {
        formData.isActive = document.getElementById('category-active')?.checked;
    }

    try {
        if (categoryId) {
            await SettingsDataLoader.updateCategory(categoryId, formData);
            showMessage('Категорію успішно оновлено', 'success');
        } else {
            await SettingsDataLoader.createCategory(formData);
            showMessage('Категорію успішно створено', 'success');
        }
        SettingsModal.closeModal('create-category-modal');
        document.getElementById('category-form')?.reset();
        document.getElementById('category-id').value = '';
        loadCategories();
    } catch (error) {
        handleError(error);
    }
}

window.editCategory = async function editCategory(id) {
    try {
        const category = await SettingsDataLoader.loadCategory(id);
        SettingsModal.populateCategoryForm(category);
        SettingsModal.openModal('create-category-modal');
    } catch (error) {
        handleError(error);
    }
};

window.deleteCategory = function deleteCategory(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_CATEGORY,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await SettingsDataLoader.deleteCategory(id);
                showMessage('Категорію успішно видалено', 'success');
                loadCategories();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadBranches() {
    if (loadingStates.branches) return;
    loadingStates.branches = true;
    try {
        branchesCache = await SettingsDataLoader.loadBranches();
        SettingsRenderer.renderBranches(branchesCache);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.branches = false;
    }
}

async function loadBranchesForAccountForm() {
    try {
        if (branchesCache.length === 0) {
            branchesCache = await SettingsDataLoader.loadBranches();
        }
    } catch (error) {
        console.error('Error loading branches for account form:', error);
    }
}

async function loadUsersForAccountForm() {
    try {
        if (usersCache.length === 0) {
            usersCache = await SettingsDataLoader.loadUsers();
        }
    } catch (error) {
        console.error('Error loading users for account form:', error);
        usersCache = [];
    }
}

async function handleCreateBranch(e) {
    e.preventDefault();
    const branchId = document.getElementById('branch-id')?.value;
    const formData = {
        name: document.getElementById('branch-name')?.value?.trim(),
        description: document.getElementById('branch-description')?.value?.trim()
    };

    try {
        if (branchId) {
            await SettingsDataLoader.updateBranch(branchId, formData);
            showMessage('Філію успішно оновлено', 'success');
        } else {
            await SettingsDataLoader.createBranch(formData);
            showMessage('Філію успішно створено', 'success');
        }
        SettingsModal.closeModal('create-branch-modal');
        document.getElementById('branch-form')?.reset();
        document.getElementById('branch-id').value = '';
        loadBranches();
    } catch (error) {
        handleError(error);
    }
}

window.editBranch = async function editBranch(id) {
    try {
        const branch = await SettingsDataLoader.loadBranch(id);
        SettingsModal.populateBranchForm(branch);
        SettingsModal.openModal('create-branch-modal');
    } catch (error) {
        handleError(error);
    }
};

window.deleteBranch = function deleteBranch(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_BRANCH,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await SettingsDataLoader.deleteBranch(id);
                showMessage('Філію успішно видалено', 'success');
                loadBranches();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadAccounts() {
    if (loadingStates.accounts) return;
    loadingStates.accounts = true;
    try {
        await Promise.all([loadUsersForAccountForm(), loadBranchesForAccountForm()]);
        accountsCache = await SettingsDataLoader.loadAccounts();
        SettingsRenderer.renderAccounts(accountsCache, usersCache, branchesCache);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.accounts = false;
    }
}

async function openCreateAccountModal() {
    await Promise.all([loadUsersForAccountForm(), loadBranchesForAccountForm()]);
    SettingsModal.openCreateAccountModal();
    SettingsRenderer.populateAccountFormDropdowns(usersCache, branchesCache);
}

async function handleCreateAccount(e) {
    e.preventDefault();
    const accountId = document.getElementById('account-id')?.value;
    const currencies = Array.from(document.querySelectorAll('input[name="currency"]:checked'))
        .map(cb => cb.value);

    if (currencies.length === 0) {
        showMessage('Виберіть хоча б одну валюту', 'error');
        return;
    }

    const userIdValue = document.getElementById('account-user')?.value;
    const branchIdValue = document.getElementById('account-branch')?.value;
    
    const formData = {
        name: document.getElementById('account-name')?.value?.trim(),
        description: document.getElementById('account-description')?.value?.trim(),
        userId: userIdValue && userIdValue !== '' ? Number(userIdValue) : null,
        branchId: branchIdValue && branchIdValue !== '' ? Number(branchIdValue) : null,
        currencies: currencies
    };

    try {
        if (accountId) {
            await SettingsDataLoader.updateAccount(accountId, formData);
            showMessage('Рахунок успішно оновлено', 'success');
        } else {
            await SettingsDataLoader.createAccount(formData);
            showMessage('Рахунок успішно створено', 'success');
        }
        SettingsModal.closeModal('create-account-modal');
        document.getElementById('account-form')?.reset();
        document.getElementById('account-id').value = '';
        loadAccounts();
    } catch (error) {
        handleError(error);
    }
}

window.editAccount = async function editAccount(id) {
    try {
        const account = await SettingsDataLoader.loadAccount(id);
        await Promise.all([loadUsersForAccountForm(), loadBranchesForAccountForm()]);
        SettingsRenderer.populateAccountFormDropdowns(usersCache, branchesCache);
        SettingsModal.populateAccountForm(account);
        SettingsModal.openModal('create-account-modal');
    } catch (error) {
        handleError(error);
    }
};

window.deleteAccount = function deleteAccount(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_ACCOUNT,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await SettingsDataLoader.deleteAccount(id);
                showMessage('Рахунок успішно видалено', 'success');
                loadAccounts();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};


async function loadVehicleSenders() {
    if (loadingStates.vehicleSenders) return;
    loadingStates.vehicleSenders = true;
    try {
        const senders = await SettingsDataLoader.loadVehicleSenders();
        SettingsRenderer.renderVehicleSenders(senders);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.vehicleSenders = false;
    }
}

async function handleCreateVehicleSender(e) {
    e.preventDefault();
    const id = document.getElementById('vehicle-sender-id')?.value;
    const name = document.getElementById('vehicle-sender-name')?.value.trim();

    if (!name) {
        showMessage('Назва відправника не може бути порожньою', 'error');
        return;
    }

    try {
        if (id) {
            await SettingsDataLoader.updateVehicleSender(id, { name });
            showMessage('Відправника успішно оновлено', 'success');
        } else {
            await SettingsDataLoader.createVehicleSender({ name });
            showMessage('Відправника успішно створено', 'success');
        }
        SettingsModal.closeModal('create-vehicle-sender-modal');
        document.getElementById('vehicle-sender-form')?.reset();
        document.getElementById('vehicle-sender-id').value = '';
        loadVehicleSenders();
    } catch (error) {
        handleError(error);
    }
}

window.editVehicleSender = async function editVehicleSender(id) {
    try {
        const sender = await SettingsDataLoader.loadVehicleSender(id);
        SettingsModal.populateVehicleSenderForm(sender);
        SettingsModal.openModal('create-vehicle-sender-modal');
    } catch (error) {
        handleError(error);
    }
};

window.deleteVehicleSender = function deleteVehicleSender(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_VEHICLE_SENDER,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await SettingsDataLoader.deleteVehicleSender(id);
                showMessage('Відправника успішно видалено', 'success');
                loadVehicleSenders();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadVehicleReceivers() {
    if (loadingStates.vehicleReceivers) return;
    loadingStates.vehicleReceivers = true;
    try {
        const receivers = await SettingsDataLoader.loadVehicleReceivers();
        SettingsRenderer.renderVehicleReceivers(receivers);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.vehicleReceivers = false;
    }
}

async function handleCreateVehicleReceiver(e) {
    e.preventDefault();
    const id = document.getElementById('vehicle-receiver-id')?.value;
    const name = document.getElementById('vehicle-receiver-name')?.value.trim();

    if (!name) {
        showMessage('Назва отримувача не може бути порожньою', 'error');
        return;
    }

    try {
        if (id) {
            await SettingsDataLoader.updateVehicleReceiver(id, { name });
            showMessage('Отримувача успішно оновлено', 'success');
        } else {
            await SettingsDataLoader.createVehicleReceiver({ name });
            showMessage('Отримувача успішно створено', 'success');
        }
        SettingsModal.closeModal('create-vehicle-receiver-modal');
        document.getElementById('vehicle-receiver-form')?.reset();
        document.getElementById('vehicle-receiver-id').value = '';
        loadVehicleReceivers();
    } catch (error) {
        handleError(error);
    }
}

window.editVehicleReceiver = async function editVehicleReceiver(id) {
    try {
        const receiver = await SettingsDataLoader.loadVehicleReceiver(id);
        SettingsModal.populateVehicleReceiverForm(receiver);
        SettingsModal.openModal('create-vehicle-receiver-modal');
    } catch (error) {
        handleError(error);
    }
};

window.deleteVehicleReceiver = function deleteVehicleReceiver(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_VEHICLE_RECEIVER,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await SettingsDataLoader.deleteVehicleReceiver(id);
                showMessage('Отримувача успішно видалено', 'success');
                loadVehicleReceivers();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadVehicleTerminals() {
    if (loadingStates.vehicleTerminals) return;
    loadingStates.vehicleTerminals = true;
    try {
        const terminals = await SettingsDataLoader.loadVehicleTerminals();
        SettingsRenderer.renderVehicleTerminals(terminals);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.vehicleTerminals = false;
    }
}

async function handleCreateVehicleTerminal(e) {
    e.preventDefault();
    const id = document.getElementById('vehicle-terminal-id')?.value;
    const name = document.getElementById('vehicle-terminal-name')?.value.trim();

    if (!name) {
        showMessage('Назва терміналу не може бути порожньою', 'error');
        return;
    }

    try {
        if (id) {
            await SettingsDataLoader.updateVehicleTerminal(id, { name });
            showMessage('Термінал успішно оновлено', 'success');
        } else {
            await SettingsDataLoader.createVehicleTerminal({ name });
            showMessage('Термінал успішно створено', 'success');
        }
        SettingsModal.closeModal('create-vehicle-terminal-modal');
        document.getElementById('vehicle-terminal-form')?.reset();
        document.getElementById('vehicle-terminal-id').value = '';
        loadVehicleTerminals();
    } catch (error) {
        handleError(error);
    }
}

window.editVehicleTerminal = async function editVehicleTerminal(id) {
    try {
        const terminal = await SettingsDataLoader.getVehicleTerminal(id);
        SettingsModal.populateVehicleTerminalForm(terminal);
        SettingsModal.openModal('create-vehicle-terminal-modal');
    } catch (error) {
        handleError(error);
    }
};

window.deleteVehicleTerminal = function deleteVehicleTerminal(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_VEHICLE_TERMINAL,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await SettingsDataLoader.deleteVehicleTerminal(id);
                showMessage('Термінал успішно видалено', 'success');
                loadVehicleTerminals();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadVehicleDestinationCountries() {
    if (loadingStates.vehicleDestinationCountries) return;
    loadingStates.vehicleDestinationCountries = true;
    try {
        const countries = await SettingsDataLoader.loadVehicleDestinationCountries();
        SettingsRenderer.renderVehicleDestinationCountries(countries);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.vehicleDestinationCountries = false;
    }
}

async function handleCreateVehicleDestinationCountry(e) {
    e.preventDefault();
    const id = document.getElementById('vehicle-destination-country-id')?.value;
    const name = document.getElementById('vehicle-destination-country-name')?.value.trim();

    if (!name) {
        showMessage('Назва країни не може бути порожньою', 'error');
        return;
    }

    try {
        if (id) {
            await SettingsDataLoader.updateVehicleDestinationCountry(id, { name });
            showMessage('Країну успішно оновлено', 'success');
        } else {
            await SettingsDataLoader.createVehicleDestinationCountry({ name });
            showMessage('Країну успішно створено', 'success');
        }
        SettingsModal.closeModal('create-vehicle-destination-country-modal');
        document.getElementById('vehicle-destination-country-form')?.reset();
        document.getElementById('vehicle-destination-country-id').value = '';
        loadVehicleDestinationCountries();
    } catch (error) {
        handleError(error);
    }
}

window.editVehicleDestinationCountry = async function editVehicleDestinationCountry(id) {
    try {
        const country = await SettingsDataLoader.getVehicleDestinationCountry(id);
        SettingsModal.populateVehicleDestinationCountryForm(country);
        SettingsModal.openModal('create-vehicle-destination-country-modal');
    } catch (error) {
        handleError(error);
    }
};

window.deleteVehicleDestinationCountry = function deleteVehicleDestinationCountry(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_VEHICLE_DESTINATION_COUNTRY,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await SettingsDataLoader.deleteVehicleDestinationCountry(id);
                showMessage('Країну успішно видалено', 'success');
                loadVehicleDestinationCountries();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadVehicleDestinationPlaces() {
    if (loadingStates.vehicleDestinationPlaces) return;
    loadingStates.vehicleDestinationPlaces = true;
    try {
        const places = await SettingsDataLoader.loadVehicleDestinationPlaces();
        SettingsRenderer.renderVehicleDestinationPlaces(places);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.vehicleDestinationPlaces = false;
    }
}

async function handleCreateVehicleDestinationPlace(e) {
    e.preventDefault();
    const id = document.getElementById('vehicle-destination-place-id')?.value;
    const name = document.getElementById('vehicle-destination-place-name')?.value.trim();

    if (!name) {
        showMessage('Назва місця не може бути порожньою', 'error');
        return;
    }

    try {
        if (id) {
            await SettingsDataLoader.updateVehicleDestinationPlace(id, { name });
            showMessage('Місце успішно оновлено', 'success');
        } else {
            await SettingsDataLoader.createVehicleDestinationPlace({ name });
            showMessage('Місце успішно створено', 'success');
        }
        SettingsModal.closeModal('create-vehicle-destination-place-modal');
        document.getElementById('vehicle-destination-place-form')?.reset();
        document.getElementById('vehicle-destination-place-id').value = '';
        loadVehicleDestinationPlaces();
    } catch (error) {
        handleError(error);
    }
}

window.editVehicleDestinationPlace = async function editVehicleDestinationPlace(id) {
    try {
        const place = await SettingsDataLoader.getVehicleDestinationPlace(id);
        SettingsModal.populateVehicleDestinationPlaceForm(place);
        SettingsModal.openModal('create-vehicle-destination-place-modal');
    } catch (error) {
        handleError(error);
    }
};

window.deleteVehicleDestinationPlace = function deleteVehicleDestinationPlace(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_VEHICLE_DESTINATION_PLACE,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await SettingsDataLoader.deleteVehicleDestinationPlace(id);
                showMessage('Місце успішно видалено', 'success');
                loadVehicleDestinationPlaces();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};

async function loadCarriers() {
    if (loadingStates.carriers) return;
    loadingStates.carriers = true;
    try {
        const carriers = await SettingsDataLoader.loadCarriers();
        SettingsRenderer.renderCarriers(carriers);
    } catch (error) {
        handleError(error);
    } finally {
        loadingStates.carriers = false;
    }
}

async function handleCreateCarrier(e) {
    e.preventDefault();
    const id = document.getElementById('carrier-id')?.value;
    const carrierData = {
        companyName: document.getElementById('carrier-company-name')?.value?.trim() || '',
        registrationAddress: document.getElementById('carrier-registration-address')?.value?.trim() || '',
        phoneNumber: document.getElementById('carrier-phone-number')?.value?.trim() || '',
        code: document.getElementById('carrier-code')?.value?.trim() || '',
        account: document.getElementById('carrier-account')?.value?.trim() || ''
    };

    if (!carrierData.companyName) {
        showMessage('Назва компанії перевізника не може бути порожньою', 'error');
        return;
    }

    try {
        if (id) {
            await SettingsDataLoader.updateCarrier(Number(id), carrierData);
            showMessage('Перевізника успішно оновлено', 'success');
        } else {
            await SettingsDataLoader.createCarrier(carrierData);
            showMessage('Перевізника успішно створено', 'success');
        }
        SettingsModal.closeModal('create-carrier-modal');
        document.getElementById('carrier-form')?.reset();
        document.getElementById('carrier-id').value = '';
        loadCarriers();
    } catch (error) {
        handleError(error);
    }
}

window.editCarrier = async function editCarrier(id) {
    try {
        const carrier = await SettingsDataLoader.loadCarrier(id);
        SettingsModal.populateCarrierForm(carrier);
        SettingsModal.openModal('create-carrier-modal');
    } catch (error) {
        handleError(error);
    }
};

window.deleteCarrier = function deleteCarrier(id) {
    ConfirmationModal.show(
        CONFIRMATION_MESSAGES.DELETE_CARRIER,
        CONFIRMATION_MESSAGES.CONFIRMATION_TITLE,
        async () => {
            try {
                await SettingsDataLoader.deleteCarrier(id);
                showMessage('Перевізника успішно видалено', 'success');
                loadCarriers();
            } catch (error) {
                handleError(error);
            }
        },
        () => {}
    );
};
