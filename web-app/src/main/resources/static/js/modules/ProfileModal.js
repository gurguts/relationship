const ProfileModal = (function() {
    let modalInstance = null;

    function openEditBalanceModal(balance, productName, onSubmit) {
        if (modalInstance) {
            closeEditBalanceModal();
        }

        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.id = 'edit-balance-modal';
        modal.style.display = 'flex';
        
        const modalContent = document.createElement('div');
        modalContent.className = 'modal-content';
        
        const closeSpan = document.createElement('span');
        closeSpan.className = 'close';
        closeSpan.textContent = '×';
        closeSpan.addEventListener('click', closeEditBalanceModal);
        modalContent.appendChild(closeSpan);
        
        const title = document.createElement('h2');
        title.textContent = 'Редагувати загальну вартість';
        modalContent.appendChild(title);
        
        const productInfo = document.createElement('p');
        const productLabel = document.createElement('strong');
        productLabel.textContent = 'Товар: ';
        productInfo.appendChild(productLabel);
        productInfo.appendChild(document.createTextNode(productName || ''));
        modalContent.appendChild(productInfo);
        
        const form = document.createElement('form');
        form.id = 'edit-balance-form';
        
        const label = document.createElement('label');
        label.setAttribute('for', 'total-cost-input');
        label.textContent = 'Загальна вартість (EUR):';
        form.appendChild(label);
        
        const input = document.createElement('input');
        input.type = 'number';
        input.id = 'total-cost-input';
        input.step = '0.01';
        input.min = '0';
        input.value = balance.totalCostEur || '0';
        input.required = true;
        form.appendChild(input);
        
        const buttonsDiv = document.createElement('div');
        buttonsDiv.className = 'modal-buttons';
        
        const submitBtn = document.createElement('button');
        submitBtn.type = 'submit';
        submitBtn.textContent = 'Зберегти';
        buttonsDiv.appendChild(submitBtn);
        
        const cancelBtn = document.createElement('button');
        cancelBtn.type = 'button';
        cancelBtn.className = 'cancel-btn';
        cancelBtn.textContent = 'Відмінити';
        cancelBtn.addEventListener('click', closeEditBalanceModal);
        buttonsDiv.appendChild(cancelBtn);
        
        form.appendChild(buttonsDiv);
        modalContent.appendChild(form);
        
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const newTotalCost = parseFloat(input.value);
            
            if (isNaN(newTotalCost) || newTotalCost < 0) {
                showMessage(PROFILE_MESSAGES.INVALID_VALUE, 'error');
                return;
            }
            
            if (onSubmit) {
                try {
                    await onSubmit(balance.driverId, balance.productId, newTotalCost);
                    closeEditBalanceModal();
                } catch (error) {
                    handleError(error);
                }
            }
        });
        
        modal.appendChild(modalContent);
        document.body.appendChild(modal);
        
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeEditBalanceModal();
            }
        });
        
        modalInstance = modal;
    }

    function closeEditBalanceModal() {
        if (modalInstance) {
            modalInstance.remove();
            modalInstance = null;
        }
    }

    return {
        openEditBalanceModal,
        closeEditBalanceModal
    };
})();
