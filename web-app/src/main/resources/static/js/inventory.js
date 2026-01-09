const userContainerBalanceList = document.getElementById('user-container-balance-list');
const loaderBackdrop = document.getElementById('loader-backdrop');

async function loadData() {
    if (loaderBackdrop) {
        loaderBackdrop.style.display = 'flex';
    }

    try {
        const [balances, containerTypes] = await Promise.all([
            InventoryDataLoader.loadUserContainerBalances(),
            InventoryDataLoader.loadContainerTypes()
        ]);

        InventoryRenderer.renderUserContainerBalances(balances, userContainerBalanceList);
        InventoryModal.setContainerTypes(containerTypes);
    } catch (error) {
        console.error('Error loading data:', error);
        handleError(error);
    } finally {
        if (loaderBackdrop) {
            loaderBackdrop.style.display = 'none';
        }
    }
}

async function handleBalanceOperation(action, userId, containerId, quantity) {
    if (loaderBackdrop) {
        loaderBackdrop.style.display = 'flex';
    }

    try {
        await InventoryDataLoader.executeBalanceOperation(action, userId, containerId, quantity);
        InventoryModal.closeModal();
        await loadData();
        if (typeof showMessage === 'function') {
            showMessage('Операція успішно виконана', 'info');
        }
    } catch (error) {
        console.error('Error executing balance operation:', error);
        handleError(error);
    } finally {
        if (loaderBackdrop) {
            loaderBackdrop.style.display = 'none';
        }
    }
}

function setupOperationButtons() {
    if (!userContainerBalanceList) return;

    userContainerBalanceList.addEventListener('click', (event) => {
        const button = event.target.closest('.user-container-balance-button');
        if (button) {
            const userId = button.getAttribute('data-user-id');
            if (userId) {
                InventoryModal.openModal(userId);
            }
        }
    });
}

document.addEventListener('DOMContentLoaded', async () => {
    InventoryModal.init({
        modalId: 'balanceOperationModal',
        formId: 'balanceOperationForm',
        closeBtnSelector: '.close-balance-operation',
        userIdInputId: 'operationUserId',
        containerTypeSelectId: 'operationContainerType',
        actionSelectId: 'operationAction',
        quantityInputId: 'operationQuantity',
        onSubmit: handleBalanceOperation
    });

    setupOperationButtons();
    await loadData();
});