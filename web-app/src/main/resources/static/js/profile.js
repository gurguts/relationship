let productsCache = [];
let userId = null;
let usersCache = [];
let currentSelectedUserId = null;

document.addEventListener('DOMContentLoaded', async function () {
    userId = localStorage.getItem('userId');
    if (!userId) {
        ProfileRenderer.showErrorState('product-balance-container', PROFILE_MESSAGES.USER_NOT_AUTHORIZED);
        return;
    }

    currentSelectedUserId = userId;

    const canViewMultiple = ProfileUtils.checkCanViewMultipleProfiles();
    if (canViewMultiple) {
        try {
            usersCache = await ProfileDataLoader.loadUsers();
            ProfileRenderer.renderUserSelector(usersCache, userId, handleUserChange);
        } catch (error) {
            console.error('Error loading users:', error);
            handleError(error);
        }
    } else {
        const selectorContainer = document.getElementById('user-selector-container');
        if (selectorContainer) {
            selectorContainer.style.display = 'none';
        }
    }

    try {
        productsCache = await ProfileDataLoader.loadProducts();
    } catch (error) {
        console.error('Error loading products:', error);
        handleError(error);
    }

    await loadProductBalances();
    await loadAccounts();
    updateProfileTitle();
});

async function handleUserChange(newUserId) {
    currentSelectedUserId = newUserId;
    updateProfileTitle();
    await loadProductBalances();
    await loadAccounts();
}

async function loadProductBalances() {
    const targetUserId = currentSelectedUserId || userId;
    
    ProfileRenderer.showLoadingState('product-balance-container');
    
    try {
        const balances = await ProfileDataLoader.loadProductBalances(targetUserId);
        
        if (balances === null) {
            ProfileRenderer.showEmptyState('product-balance-container', PROFILE_MESSAGES.NO_PRODUCT_BALANCE);
            return;
        }
        
        const canEditProfile = ProfileUtils.checkCanEditProfile();
        
        ProfileRenderer.renderProductBalances(
            balances,
            productsCache,
            canEditProfile,
            (balance) => {
                const product = productsCache.find(p => Number(p.id) === Number(balance.productId));
                const productName = product ? product.name : `Товар #${balance.productId}`;
                ProfileModal.openEditBalanceModal(balance, productName, handleUpdateBalance);
            }
        );
    } catch (error) {
        console.error('Error loading product balances:', error);
        handleError(error);
        ProfileRenderer.showErrorState('product-balance-container', PROFILE_MESSAGES.LOAD_BALANCE_ERROR);
    }
}

async function handleUpdateBalance(driverId, productId, totalCostEur) {
    try {
        await ProfileDataLoader.updateProductBalance(driverId, productId, totalCostEur);
        showMessage(PROFILE_MESSAGES.BALANCE_UPDATED, 'success');
        await loadProductBalances();
    } catch (error) {
        console.error('Error updating balance:', error);
        throw error;
    }
}

async function loadAccounts() {
    const targetUserId = currentSelectedUserId || userId;
    
    ProfileRenderer.showLoadingState('accounts-container');
    
    try {
        const accounts = await ProfileDataLoader.loadAccounts(targetUserId);
        
        if (!accounts || accounts.length === 0) {
            ProfileRenderer.showEmptyState('accounts-container', PROFILE_MESSAGES.NO_ACCOUNTS);
            return;
        }

        await ProfileRenderer.renderAccounts(accounts, ProfileDataLoader);
    } catch (error) {
        console.error('Error loading accounts:', error);
        handleError(error);
        ProfileRenderer.showErrorState('accounts-container', PROFILE_MESSAGES.LOAD_ACCOUNTS_ERROR);
    }
}

function updateProfileTitle() {
    ProfileRenderer.updateProfileTitle(currentSelectedUserId, userId, usersCache);
}
