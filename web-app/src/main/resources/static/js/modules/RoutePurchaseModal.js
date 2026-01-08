const RoutePurchaseModal = (function() {
    async function loadUsers() {
        try {
            const response = await fetch('/api/v1/user');
            if (!response.ok) throw new Error('Failed to load users');
            return await response.json();
        } catch (error) {
            console.error('Error loading users:', error);
            return [];
        }
    }

    async function loadProducts() {
        try {
            const response = await fetch('/api/v1/product?usage=PURCHASE_ONLY');
            if (!response.ok) throw new Error('Failed to load products');
            return await response.json();
        } catch (error) {
            console.error('Error loading products:', error);
            return [];
        }
    }

    async function checkExchangeRatesFreshness() {
        try {
            const response = await fetch('/api/v1/exchange-rates');
            if (!response.ok) {
                return false;
            }
            const rates = await response.json();
            
            if (!rates || rates.length === 0) {
                return false;
            }
            
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            
            for (const rate of rates) {
                let rateDate = null;
                
                if (rate.updatedAt) {
                    rateDate = new Date(rate.updatedAt);
                } else if (rate.createdAt) {
                    rateDate = new Date(rate.createdAt);
                } else {
                    return false;
                }
                
                rateDate.setHours(0, 0, 0, 0);
                
                if (rateDate.getTime() < today.getTime()) {
                    return false;
                }
            }
            
            return true;
        } catch (error) {
            console.error('Error checking exchange rates freshness:', error);
            return false;
        }
    }

    async function openCreatePurchaseModal(clientId, config) {
        const {
            modal,
            form,
            clientIdInput,
            sourceIdInput,
            userIdSelect,
            productIdSelect,
            currencySelect,
            exchangeRateLabel,
            exchangeRateInput,
            exchangeRateWarning,
            userId,
            loaderBackdrop
        } = config;

        if (!modal || !form || !clientIdInput || !sourceIdInput || !userIdSelect || !productIdSelect || !currencySelect) {
            return;
        }
        
        form.reset();
        clientIdInput.value = clientId;
        
        const ratesAreFresh = await checkExchangeRatesFreshness();
        if (exchangeRateWarning) {
            exchangeRateWarning.style.display = ratesAreFresh ? 'none' : 'block';
        }
        
        try {
            const clientResponse = await fetch(`/api/v1/client/${clientId}`);
            if (!clientResponse.ok) throw new Error('Failed to load client');
            const clientData = await clientResponse.json();
            sourceIdInput.value = clientData.sourceId || '';
        } catch (error) {
            console.error('Error loading client:', error);
            sourceIdInput.value = '';
        }
        
        const [users, products] = await Promise.all([loadUsers(), loadProducts()]);
        
        userIdSelect.innerHTML = '';
        const currentUserId = userId ? Number(userId) : null;
        users.forEach(user => {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = user.name;
            if (currentUserId && Number(user.id) === currentUserId) {
                option.selected = true;
            }
            userIdSelect.appendChild(option);
        });
        
        productIdSelect.innerHTML = '<option value="">Виберіть товар</option>';
        products.forEach(product => {
            const option = document.createElement('option');
            option.value = product.id;
            option.textContent = product.name;
            productIdSelect.appendChild(option);
        });
        
        currencySelect.value = 'UAH';
        if (exchangeRateLabel) {
            exchangeRateLabel.style.display = 'none';
        }
        if (exchangeRateInput) {
            exchangeRateInput.value = '';
        }
        
        modal.style.display = 'flex';
    }

    function setupCurrencyHandler(currencySelect, exchangeRateLabel, exchangeRateInput) {
        if (currencySelect && exchangeRateLabel && exchangeRateInput) {
            currencySelect.addEventListener('change', function() {
                if (this.value === 'USD' || this.value === 'EUR') {
                    exchangeRateLabel.style.display = 'flex';
                } else {
                    exchangeRateLabel.style.display = 'none';
                    exchangeRateInput.value = '';
                }
            });
        }
    }

    function setupCloseHandler(closeButton, modal) {
        if (closeButton) {
            closeButton.addEventListener('click', () => {
                modal.style.display = 'none';
            });
        }
    }

    function setupSubmitHandler(form, config) {
        const {
            loaderBackdrop,
            modal
        } = config;

        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                
                const sourceIdValue = document.getElementById('purchaseSourceId').value;
                const formData = {
                    userId: Number(document.getElementById('purchaseUserId').value),
                    clientId: Number(document.getElementById('purchaseClientId').value),
                    sourceId: sourceIdValue && sourceIdValue !== '' ? Number(sourceIdValue) : null,
                    productId: Number(document.getElementById('purchaseProductId').value),
                    quantity: parseFloat(document.getElementById('purchaseQuantity').value),
                    totalPrice: parseFloat(document.getElementById('purchaseTotalPrice').value),
                    paymentMethod: document.getElementById('purchasePaymentMethod').value,
                    currency: document.getElementById('purchaseCurrency').value,
                    exchangeRate: document.getElementById('purchaseExchangeRate').value ? parseFloat(document.getElementById('purchaseExchangeRate').value) : null,
                    comment: document.getElementById('purchaseComment').value || null
                };
                
                try {
                    if (loaderBackdrop) {
                        loaderBackdrop.style.display = 'flex';
                    }
                    const response = await fetch('/api/v1/purchase', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(formData)
                    });
                    
                    if (!response.ok) {
                        const error = await response.json();
                        throw new Error(error.message || 'Помилка створення закупівлі');
                    }
                    
                    modal.style.display = 'none';
                    form.reset();
                    showMessage('Закупівлю успішно створено', 'info');
                } catch (error) {
                    console.error('Error creating purchase:', error);
                    showMessage('Помилка створення закупівлі: ' + error.message, 'error');
                } finally {
                    if (loaderBackdrop) {
                        loaderBackdrop.style.display = 'none';
                    }
                }
            });
        }
    }

    function init(config) {
        const {
            currencySelect,
            exchangeRateLabel,
            exchangeRateInput,
            closeButton,
            form,
            modal
        } = config;

        setupCurrencyHandler(currencySelect, exchangeRateLabel, exchangeRateInput);
        setupCloseHandler(closeButton, modal);
        setupSubmitHandler(form, config);
    }

    return {
        openCreatePurchaseModal,
        loadUsers,
        loadProducts,
        checkExchangeRatesFreshness,
        init
    };
})();
