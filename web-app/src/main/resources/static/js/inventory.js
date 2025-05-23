document.addEventListener('DOMContentLoaded', async () => {
    await loadUserContainerBalances();
    await loadContainerTypes();
});

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
        const select = document.getElementById('operationContainerType');
        types.forEach(type => {
            const option = document.createElement('option');
            option.value = type.id;
            option.textContent = type.name;
            select.appendChild(option);
        });
    } catch (error) {
        console.error('Error:', error);
        handleError(error);
    }
}

function renderUserContainerBalances(balances) {
    const list = document.getElementById('user-container-balance-list');
    list.innerHTML = '';

    balances.forEach(userBalance => {
        const item = document.createElement('li');
        item.className = 'user-container-balance-item';

        const header = document.createElement('div');
        header.className = 'user-container-balance-item-header';
        header.innerHTML = `
            <span class="user-name">${userBalance.userName}</span>
            <button class="user-container-balance-button" 
            onclick="openBalanceOperationModal(${userBalance.userId})">Операція</button>        `;

        const balanceList = document.createElement('ul');
        userBalance.balances.forEach(balance => {
            const balanceItem = document.createElement('li');
            balanceItem.textContent = `${balance.containerName} - 
            ${balance.totalQuantity} шт - у клієнта ${balance.clientQuantity} шт`;
            balanceList.appendChild(balanceItem);
        });

        item.appendChild(header);
        item.appendChild(balanceList);
        list.appendChild(item);
    });
}

function openBalanceOperationModal(userId) {
    const modal = document.getElementById('balanceOperationModal');
    modal.classList.add('show');
    document.getElementById('operationUserId').value = userId;
}

function closeBalanceOperationModal() {
    const modal = document.getElementById('balanceOperationModal');
    modal.classList.remove('show');
    document.getElementById('balanceOperationForm').reset();
}

document.querySelector('.close-balance-operation').addEventListener('click', closeBalanceOperationModal);
window.addEventListener('click', (event) => {
    const modal = document.getElementById('balanceOperationModal');
    if (event.target === modal) {
        closeBalanceOperationModal();
    }
});

document.getElementById('balanceOperationForm').addEventListener('submit',
    async (event) => {
        event.preventDefault();

        const action = document.getElementById('operationAction').value;
        const containerId = document.getElementById('operationContainerType').value;
        const quantity = document.getElementById('operationQuantity').value;
        const userId = document.getElementById('operationUserId').value;

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
    });