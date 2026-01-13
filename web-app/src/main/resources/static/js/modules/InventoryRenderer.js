const InventoryRenderer = (function() {
    function renderUserContainerBalances(balances, container) {
        if (!container) return;
        
        container.textContent = '';

        if (!balances || balances.length === 0) {
            const emptyMessage = document.createElement('li');
            emptyMessage.textContent = CLIENT_MESSAGES.NO_DATA;
            emptyMessage.style.textAlign = 'center';
            emptyMessage.style.padding = '20px';
            emptyMessage.style.color = '#999';
            container.appendChild(emptyMessage);
            return;
        }

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
            operationButton.setAttribute('data-user-id', userBalance.userId || '');
            header.appendChild(operationButton);

            const balanceList = document.createElement('ul');
            if (userBalance.balances && Array.isArray(userBalance.balances)) {
                if (userBalance.balances.length === 0) {
                    const emptyBalanceItem = document.createElement('li');
                    emptyBalanceItem.textContent = CLIENT_MESSAGES.NO_DATA;
                    emptyBalanceItem.style.color = '#999';
                    emptyBalanceItem.style.fontStyle = 'italic';
                    balanceList.appendChild(emptyBalanceItem);
                } else {
                    userBalance.balances.forEach(balance => {
                        const balanceItem = document.createElement('li');
                        const containerName = balance.containerName || '';
                        const totalQuantity = balance.totalQuantity != null ? balance.totalQuantity : 0;
                        const clientQuantity = balance.clientQuantity != null ? balance.clientQuantity : 0;
                        balanceItem.textContent = `${containerName} - ${totalQuantity} шт - у клієнта ${clientQuantity} шт`;
                        balanceList.appendChild(balanceItem);
                    });
                }
            }

            item.appendChild(header);
            item.appendChild(balanceList);
            container.appendChild(item);
        });
    }
    
    function populateContainerTypesSelect(selectElement, containerTypes) {
        if (!selectElement) return;
        
        selectElement.textContent = '';
        
        if (!containerTypes || containerTypes.length === 0) {
            const emptyOption = document.createElement('option');
            emptyOption.value = '';
            emptyOption.textContent = CLIENT_MESSAGES.NO_DATA;
            selectElement.appendChild(emptyOption);
            return;
        }
        
        containerTypes.forEach(type => {
            const option = document.createElement('option');
            option.value = type.id;
            option.textContent = type.name;
            selectElement.appendChild(option);
        });
    }
    
    return {
        renderUserContainerBalances,
        populateContainerTypesSelect
    };
})();
