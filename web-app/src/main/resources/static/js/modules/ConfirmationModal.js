const ConfirmationModal = (function() {
    let currentModal = null;

    function showConfirmationModal(message, title, onConfirm, onCancel) {
        if (currentModal) {
            closeConfirmationModal();
        }

        const modal = document.createElement('div');
        modal.className = 'modal';
        
        const modalContent = document.createElement('div');
        modalContent.className = 'modal-content';
        modalContent.style.maxWidth = '500px';
        
        const closeSpan = document.createElement('span');
        closeSpan.className = 'close';
        closeSpan.textContent = '×';
        closeSpan.addEventListener('click', () => closeConfirmationModal());
        modalContent.appendChild(closeSpan);
        
        const titleElement = document.createElement('h2');
        titleElement.textContent = title || (typeof CONFIRMATION_MESSAGES !== 'undefined' ? CONFIRMATION_MESSAGES.CONFIRMATION_TITLE : 'Підтвердження');
        modalContent.appendChild(titleElement);
        
        const messageElement = document.createElement('p');
        messageElement.textContent = message;
        messageElement.style.marginBottom = '20px';
        modalContent.appendChild(messageElement);
        
        const buttonsDiv = document.createElement('div');
        buttonsDiv.className = 'modal-buttons';
        buttonsDiv.style.display = 'flex';
        buttonsDiv.style.gap = '10px';
        buttonsDiv.style.justifyContent = 'flex-end';
        
        const confirmBtn = document.createElement('button');
        confirmBtn.type = 'button';
        confirmBtn.className = 'btn-primary';
        confirmBtn.textContent = typeof CONFIRMATION_MESSAGES !== 'undefined' ? CONFIRMATION_MESSAGES.CONFIRM : 'Підтвердити';
        confirmBtn.addEventListener('click', async () => {
            closeConfirmationModal();
            if (onConfirm) {
                try {
                    const result = onConfirm();
                    if (result instanceof Promise) {
                        await result;
                    }
                } catch (error) {
                    console.error('Error in confirmation callback:', error);
                }
            }
        });
        buttonsDiv.appendChild(confirmBtn);
        
        modalContent.appendChild(buttonsDiv);
        modal.appendChild(modalContent);
        document.body.appendChild(modal);
        
        setTimeout(() => {
            modal.classList.add('show');
        }, CLIENT_CONSTANTS.MODAL_ANIMATION_DELAY);
        
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeConfirmationModal();
            }
        });
        
        currentModal = modal;
    }

    function closeConfirmationModal() {
        if (currentModal) {
            const modalToClose = currentModal;
            currentModal = null;
            modalToClose.classList.remove('show');
            modalToClose.style.display = 'none';
            setTimeout(() => {
                if (modalToClose && modalToClose.parentNode) {
                    modalToClose.remove();
                }
            }, CLIENT_CONSTANTS.MODAL_CLOSE_DELAY);
        }
    }

    return {
        show: showConfirmationModal,
        close: closeConfirmationModal
    };
})();
