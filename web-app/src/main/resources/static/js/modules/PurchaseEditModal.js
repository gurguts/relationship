const PurchaseEditModal = (function() {
    let modal = null;
    let form = null;
    let productMap = null;
    let sourceMap = null;
    let onSaveSuccess = null;
    
    function generateProductOptions(selectedId) {
        if (!productMap || !productMap.size) {
            const option = document.createElement('option');
            option.value = '';
            option.textContent = 'Продукти не завантажені';
            return option;
        }
        const fragment = document.createDocumentFragment();
        Array.from(productMap.entries()).forEach(([id, name]) => {
            const option = document.createElement('option');
            option.value = id;
            option.textContent = name;
            if (id === selectedId || String(id) === String(selectedId)) {
                option.selected = true;
            }
            fragment.appendChild(option);
        });
        return fragment;
    }
    
    function generateSourceOptions(selectedId) {
        if (!sourceMap || !sourceMap.size) {
            const option = document.createElement('option');
            option.value = '';
            option.textContent = 'Джерела не завантажені';
            return option;
        }
        const fragment = document.createDocumentFragment();
        Array.from(sourceMap.entries()).forEach(([id, name]) => {
            const option = document.createElement('option');
            option.value = id;
            option.textContent = name;
            if (id === selectedId || String(id) === String(selectedId)) {
                option.selected = true;
            }
            fragment.appendChild(option);
        });
        return fragment;
    }
    
    function showEditModal(purchase, config) {
        if (purchase.isReceived === true) {
            showMessage(CONFIRMATION_MESSAGES.CANNOT_EDIT_PURCHASE, 'error');
            return;
        }
        
        if (!modal || !form) return;
        
        const header = modal.querySelector('h3');
        const productSelect = form.querySelector('select[name="productId"]');
        const quantityInput = form.querySelector('input[name="quantity"]');
        const totalPriceInput = form.querySelector('input[name="totalPrice"]');
        const createdAtInput = form.querySelector('input[name="createdAt"]');
        const exchangeRateInput = form.querySelector('input[name="exchangeRate"]');
        const sourceSelect = form.querySelector('select[name="sourceId"]');
        const commentTextarea = form.querySelector('textarea[name="comment"]');
        
        if (!header || !productSelect || !quantityInput || !totalPriceInput || !createdAtInput || !exchangeRateInput || !sourceSelect || !commentTextarea) {
            console.error('Required form elements not found');
            return;
        }
        
        header.textContent = `ID: ${purchase.id}`;
        productSelect.textContent = '';
        const productOptions = generateProductOptions(purchase.productId);
        productSelect.appendChild(productOptions);
        quantityInput.value = purchase.quantity || 0;
        totalPriceInput.value = purchase.totalPrice || 0;
        exchangeRateInput.value = purchase.exchangeRate || '';
        commentTextarea.value = purchase.comment || '';
        createdAtInput.value = purchase.createdAt
            ? new Date(purchase.createdAt.replace(' ', 'T') + 'Z').toISOString().split('T')[0]
            : '';
        sourceSelect.textContent = '';
        const sourceOptions = generateSourceOptions(purchase.sourceId);
        sourceSelect.appendChild(sourceOptions);
        
        modal.style.display = 'flex';
        setTimeout(() => modal.classList.add('active'), 10);
        
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const updatedData = {
                productId: productSelect.value,
                quantity: parseFloat(quantityInput.value),
                totalPrice: parseFloat(totalPriceInput.value),
                createdAt: createdAtInput.value,
                sourceId: sourceSelect.value,
                exchangeRate: exchangeRateInput.value,
                comment: commentTextarea.value
            };
            
            try {
                await PurchaseDataLoader.updatePurchase(purchase.id, updatedData);
                modal.classList.remove('active');
                setTimeout(() => {
                    modal.style.display = 'none';
                }, 300);
                
                if (onSaveSuccess) {
                    onSaveSuccess();
                }
            } catch (error) {
                if (error.message && error.message.includes('прийнято кладовщиком')) {
                    showMessage(CONFIRMATION_MESSAGES.CANNOT_EDIT_PURCHASE, 'error');
                } else {
                    handleError(error);
                }
            }
        });
    }
    
    function init(config) {
        modal = document.getElementById(config.modalId || 'edit-modal');
        form = document.getElementById(config.formId || 'edit-form');
        productMap = config.productMap;
        sourceMap = config.sourceMap;
        onSaveSuccess = config.onSaveSuccess;
        
        if (!modal || !form) {
            console.error('Modal or form not found');
            return;
        }
        
        const closeButton = document.getElementById('close-edit-modal');
        if (closeButton) {
            closeButton.addEventListener('click', () => {
                modal.classList.remove('active');
                setTimeout(() => {
                    modal.style.display = 'none';
                }, 300);
            });
        }
    }
    
    return {
        init,
        showEditModal
    };
})();
