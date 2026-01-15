const FinanceExchangeRateModal = (function() {
    let modal = null;
    let form = null;
    let closeBtn = null;
    let onSubmitCallback = null;
    let closeBtnHandler = null;
    let modalClickHandler = null;
    let formSubmitHandler = null;
    
    function init(config) {
        modal = document.getElementById(config.modalId);
        form = document.getElementById(config.formId);
        if (modal) {
            closeBtn = modal.querySelector(config.closeBtnSelector);
        }
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
                modalClickHandler = null;
            }
        }
        
        if (form) {
            if (formSubmitHandler) {
                form.removeEventListener('submit', formSubmitHandler);
            }
            formSubmitHandler = handleSubmit;
            form.addEventListener('submit', formSubmitHandler);
        }
    }
    
    function openModal(currency, currentRate) {
        if (!modal) return;
        
        const title = document.getElementById('exchange-rate-modal-title');
        const currencyInput = document.getElementById('exchange-rate-currency');
        const rateInput = document.getElementById('exchange-rate-value');
        
        if (title) {
            title.textContent = `Оновити курс ${currency} до EUR`;
        }
        if (currencyInput) {
            currencyInput.value = currency;
        }
        if (rateInput) {
            rateInput.value = currentRate || '';
        }
        
        modal.style.display = 'block';
        setTimeout(() => {
            modal.classList.add('show');
        }, CLIENT_CONSTANTS.MODAL_ANIMATION_DELAY);
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
        
        const currencyInput = document.getElementById('exchange-rate-currency');
        const rateInput = document.getElementById('exchange-rate-value');
        
        if (!currencyInput || !rateInput) {
            return;
        }
        
        const currency = currencyInput.value;
        const rate = parseFloat(rateInput.value);
        
        if (!rate || rate <= 0) {
            if (typeof showMessage === 'function') {
                showMessage('Курс повинен бути більше нуля', 'error');
            }
            return;
        }
        
        if (onSubmitCallback) {
            await onSubmitCallback(currency, rate);
        }
    }
    
    return {
        init,
        openModal,
        closeModal
    };
})();
