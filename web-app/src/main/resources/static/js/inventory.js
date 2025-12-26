function escapeHtml(text) {
    if (text == null) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

const userContainerBalanceList = document.getElementById('user-container-balance-list');
const balanceOperationModal = document.getElementById('balanceOperationModal');
const operationUserId = document.getElementById('operationUserId');
const balanceOperationForm = document.getElementById('balanceOperationForm');
const operationContainerType = document.getElementById('operationContainerType');
const operationAction = document.getElementById('operationAction');
const operationQuantity = document.getElementById('operationQuantity');


async function loadUserContainerBalances() {
    try {
        const response = await fetch('/api/v1/containers/balance/users', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        const balances = await response.json();
        renderUserContainerBalances(balances);
    } catch (error) {
        console.error('Error:', error);
        handleError(error);
    }
}

async function loadContainerTypes() {
    try {
        const response = await fetch('/api/v1/container', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            const errorData = await response.json();
            handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
            return;
        }

        const types = await response.json();
        if (operationContainerType) {
            types.forEach(type => {
                const option = document.createElement('option');
                option.value = type.id;
                option.textContent = type.name;
                operationContainerType.appendChild(option);
            });
        }
    } catch (error) {
        console.error('Error:', error);
        handleError(error);
    }
}

function renderUserContainerBalances(balances) {
    if (!userContainerBalanceList) return;
    
    userContainerBalanceList.textContent = '';

    balances.forEach(userBalance => {
        const item = document.createElement('li');
        item.className = 'user-container-balance-item';

        const header = document.createElement('div');
        header.className = 'user-container-balance-item-header';
        
        const userNameSpan = document.createElement('span');
        userNameSpan.className = 'user-name';
        userNameSpan.textContent = userBalance.userName || '';
        header.appendChild(userNameSpan);
        
        const operationButton = document.createElement('button');
        operationButton.className = 'user-container-balance-button';
        operationButton.textContent = 'Операція';
        operationButton.addEventListener('click', () => {
            openBalanceOperationModal(userBalance.userId);
        });
        header.appendChild(operationButton);

        const balanceList = document.createElement('ul');
        if (userBalance.balances && Array.isArray(userBalance.balances)) {
            userBalance.balances.forEach(balance => {
                const balanceItem = document.createElement('li');
                const containerName = balance.containerName || '';
                const totalQuantity = balance.totalQuantity != null ? balance.totalQuantity : 0;
                const clientQuantity = balance.clientQuantity != null ? balance.clientQuantity : 0;
                balanceItem.textContent = `${containerName} - ${totalQuantity} шт - у клієнта ${clientQuantity} шт`;
                balanceList.appendChild(balanceItem);
            });
        }

        item.appendChild(header);
        item.appendChild(balanceList);
        userContainerBalanceList.appendChild(item);
    });
}

function openBalanceOperationModal(userId) {
    if (balanceOperationModal) {
        balanceOperationModal.classList.add('show');
    }
    if (operationUserId) {
        operationUserId.value = userId;
    }
}

function closeBalanceOperationModal() {
    if (balanceOperationModal) {
        balanceOperationModal.classList.remove('show');
    }
    if (balanceOperationForm) {
        balanceOperationForm.reset();
    }
}

function initBalanceOperationModal() {
    const closeBtn = document.querySelector('.close-balance-operation');
    if (closeBtn) {
        if (closeBtn._closeModalHandler) {
            closeBtn.removeEventListener('click', closeBtn._closeModalHandler);
        }
        const closeModalHandler = () => {
            closeBalanceOperationModal();
        };
        closeBtn._closeModalHandler = closeModalHandler;
        closeBtn.addEventListener('click', closeModalHandler);
    }

    if (balanceOperationModal) {
        if (balanceOperationModal._modalClickHandler) {
            balanceOperationModal.removeEventListener('click', balanceOperationModal._modalClickHandler);
            balanceOperationModal._modalClickHandler = null;
        }
    }

    if (balanceOperationForm) {
        if (balanceOperationForm._submitHandler) {
            balanceOperationForm.removeEventListener('submit', balanceOperationForm._submitHandler);
        }
        const submitHandler = async (event) => {
            event.preventDefault();

            if (!operationAction || !operationContainerType || !operationQuantity || !operationUserId) {
                return;
            }

            const action = operationAction.value;
            const containerId = operationContainerType.value;
            const quantity = operationQuantity.value;
            const userId = operationUserId.value;

            const endpoint = `/api/v1/containers/balance/${action}`;
            const requestBody = {
                userId: parseInt(userId),
                containerId: parseInt(containerId),
                quantity: quantity,
            };

            try {
                const response = await fetch(endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(requestBody),
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    handleError(new ErrorResponse(errorData.error, errorData.message, errorData.details));
                    return;
                }

                closeBalanceOperationModal();
                await loadUserContainerBalances();
                showMessage('Операція успішно виконана', 'info');
            } catch (error) {
                console.error('Ошибка:', error);
                handleError(error);
            }
        };
        balanceOperationForm._submitHandler = submitHandler;
        balanceOperationForm.addEventListener('submit', submitHandler);
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    await loadUserContainerBalances();
    await loadContainerTypes();
    initBalanceOperationModal();
});