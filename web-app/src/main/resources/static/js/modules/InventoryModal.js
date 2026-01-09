const InventoryModal = (function() {
    let modal = null;
    let form = null;
    let closeBtn = null;
    let userIdInput = null;
    let containerTypeSelect = null;
    let actionSelect = null;
    let quantityInput = null;
    let onSubmitCallback = null;
    let closeBtnHandler = null;
    let modalClickHandler = null;
    let formSubmitHandler = null;
    
    function init(config) {
        modal = document.getElementById(config.modalId);
        form = document.getElementById(config.formId);
        closeBtn = document.querySelector(config.closeBtnSelector);
        userIdInput = document.getElementById(config.userIdInputId);
        containerTypeSelect = document.getElementById(config.containerTypeSelectId);
        actionSelect = document.getElementById(config.actionSelectId);
        quantityInput = document.getElementById(config.quantityInputId);
        onSubmitCallback = config.onSubmit;
        
        if (closeBtn) {
            if (closeBtnHandler) {
                closeBtn.removeEventListener('click', closeBtnHandler);
            }
            closeBtnHandler = closeModal;
            closeBtn.addEventListener('click', closeBtnHandler);
        }
        
        if (modal) {
            if (modalClickHandler) {
                modal.removeEventListener('click', modalClickHandler);
            }
            const modalContent = modal.querySelector('.modal-content-balance-operation');
            modalClickHandler = (e) => {
                if (modalContent && !modalContent.contains(e.target)) {
                    closeModal();
                } else if (!modalContent && e.target === modal) {
                    closeModal();
                }
            };
            modal.addEventListener('click', modalClickHandler);
        }
        
        if (form) {
            if (formSubmitHandler) {
                form.removeEventListener('submit', formSubmitHandler);
            }
            formSubmitHandler = handleSubmit;
            form.addEventListener('submit', formSubmitHandler);
        }
    }
    
    function openModal(userId) {
        if (!modal || !userIdInput) return;
        
        userIdInput.value = userId;
        
        if (form) {
            form.reset();
            userIdInput.value = userId;
        }
        
        if (modal) {
            modal.style.display = 'flex';
            setTimeout(() => {
                modal.classList.add('show');
            }, CLIENT_CONSTANTS.MODAL_ANIMATION_DELAY);
        }
    }
    
    function closeModal() {
        if (!modal) return;
        
        modal.classList.remove('show');
        setTimeout(() => {
            modal.style.display = 'none';
            if (form) {
                form.reset();
            }
        }, CLIENT_CONSTANTS.MODAL_CLOSE_DELAY);
    }
    
    async function handleSubmit(event) {
        event.preventDefault();
        
        if (!actionSelect || !containerTypeSelect || !quantityInput || !userIdInput) {
            return;
        }

        const action = actionSelect.value;
        const containerId = containerTypeSelect.value;
        const quantityStr = quantityInput.value.trim();
        const userId = userIdInput.value;

        if (!action || !containerId || !userId || !quantityStr) {
            if (typeof showMessage === 'function') {
                showMessage('Будь ласка, заповніть всі поля', 'error');
            }
            return;
        }

        const quantity = parseInt(quantityStr, 10);
        if (isNaN(quantity) || quantity <= 0) {
            if (typeof showMessage === 'function') {
                showMessage('Кількість повинна бути додатнім цілим числом', 'error');
            }
            return;
        }

        if (onSubmitCallback) {
            await onSubmitCallback(action, userId, containerId, quantity);
        }
    }
    
    function setContainerTypes(containerTypes) {
        InventoryRenderer.populateContainerTypesSelect(containerTypeSelect, containerTypes);
    }
    
    return {
        init,
        openModal,
        closeModal,
        setContainerTypes
    };
})();
