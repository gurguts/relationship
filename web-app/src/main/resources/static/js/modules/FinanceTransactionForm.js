const FinanceTransactionForm = (function() {
    function populateAccounts(accountsCache, branchesCache, accountSelects) {
        const canOperateAccount = (account) => {
            if (!account.branchId) return true;
            const branch = branchesCache.find(b => Number(b.id) === Number(account.branchId));
            return branch ? (branch.canOperate === true) : false;
        };
        
        accountSelects.forEach(select => {
            if (!select) return;
            select.textContent = '';
            const defaultOption = document.createElement('option');
            defaultOption.value = '';
            defaultOption.textContent = FINANCE_MESSAGES.SELECT_ACCOUNT;
            select.appendChild(defaultOption);
            accountsCache.forEach(account => {
                if (canOperateAccount(account)) {
                    const option = document.createElement('option');
                    option.value = account.id;
                    option.textContent = account.name || '';
                    select.appendChild(option);
                }
            });
        });
    }

    function populateCurrencies(currencySelects) {
        const currencies = ['UAH', 'USD', 'EUR'];
        
        currencySelects.forEach(select => {
            if (select) {
                select.textContent = '';
                const defaultOption = document.createElement('option');
                defaultOption.value = '';
                defaultOption.textContent = FINANCE_MESSAGES.SELECT_CURRENCY;
                select.appendChild(defaultOption);
                currencies.forEach(currency => {
                    const option = document.createElement('option');
                    option.value = currency;
                    option.textContent = currency;
                    select.appendChild(option);
                });
            }
        });
    }

    async function populateCategories(type, categorySelect, customSelects) {
        if (!categorySelect) return;
        
        categorySelect.textContent = '';
        const defaultOption = document.createElement('option');
        defaultOption.value = '';
        defaultOption.textContent = FINANCE_MESSAGES.WITHOUT_CATEGORY;
        categorySelect.appendChild(defaultOption);
        
        if (type) {
            const categories = await FinanceDataLoader.loadCategoriesForType(type);
            if (categories && Array.isArray(categories)) {
                categories.forEach(cat => {
                    const option = document.createElement('option');
                    option.value = cat.id;
                    option.textContent = cat.name || '';
                    categorySelect.appendChild(option);
                });
                
                if (customSelects && typeof createCustomSelect === 'function') {
                    const selectId = categorySelect.id;
                    if (!customSelects[selectId]) {
                        customSelects[selectId] = createCustomSelect(categorySelect);
                    }
                    const categoryData = [
                        { id: '', name: FINANCE_MESSAGES.WITHOUT_CATEGORY },
                        ...categories.map(cat => ({ id: cat.id, name: cat.name || '' }))
                    ];
                    if (customSelects[selectId]) {
                        customSelects[selectId].populate(categoryData);
                    }
                }
            }
        } else if (customSelects && typeof createCustomSelect === 'function') {
            const selectId = categorySelect.id;
            if (!customSelects[selectId]) {
                customSelects[selectId] = createCustomSelect(categorySelect);
            }
            const categoryData = [{ id: '', name: FINANCE_MESSAGES.WITHOUT_CATEGORY }];
            if (customSelects[selectId]) {
                customSelects[selectId].populate(categoryData);
            }
        }
    }

    function handleTransactionTypeChange(type, config) {
        const { accountsCache, branchesCache, onLoadCounterparties, customSelects } = config;
        
        const fromAccountGroup = document.getElementById('from-account-group');
        const toAccountGroup = document.getElementById('to-account-group');
        const conversionAccountGroup = document.getElementById('conversion-account-group');
        const conversionCurrencyGroup = document.getElementById('conversion-currency-group');
        const conversionReceivedAmountGroup = document.getElementById('conversion-received-amount-group');
        const conversionExchangeRateDisplayGroup = document.getElementById('conversion-exchange-rate-display-group');
        const clientGroup = document.getElementById('client-group');
        const counterpartyGroup = document.getElementById('counterparty-group');
        const currencyGroup = document.getElementById('currency-group');
        const amountGroup = document.getElementById('amount-group');
        const receivedAmountGroup = document.getElementById('received-amount-group');
        const commissionDisplayGroup = document.getElementById('commission-display-group');
        const amountLabel = document.getElementById('transaction-amount-label');

        fromAccountGroup.style.display = 'none';
        toAccountGroup.style.display = 'none';
        conversionAccountGroup.style.display = 'none';
        conversionCurrencyGroup.style.display = 'none';
        conversionReceivedAmountGroup.style.display = 'none';
        conversionExchangeRateDisplayGroup.style.display = 'none';
        clientGroup.style.display = 'none';
        counterpartyGroup.style.display = 'none';
        receivedAmountGroup.style.display = 'none';
        commissionDisplayGroup.style.display = 'none';
        
        if (currencyGroup) {
            const label = currencyGroup.querySelector('label');
            if (label) label.textContent = 'Валюта:';
        }
        if (amountLabel) {
            amountLabel.textContent = 'Сума:';
        }

        const categorySelect = document.getElementById('transaction-category');
        populateCategories(type, categorySelect, customSelects);

        if (type === 'INTERNAL_TRANSFER') {
            fromAccountGroup.style.display = 'block';
            toAccountGroup.style.display = 'block';
            currencyGroup.style.display = 'block';
            amountGroup.style.display = 'block';
            receivedAmountGroup.style.display = 'block';
            commissionDisplayGroup.style.display = 'block';
            if (amountLabel) {
                amountLabel.textContent = 'Сума списання:';
            }
            const receivedAmountInput = document.getElementById('transaction-received-amount');
            if (receivedAmountInput) {
                receivedAmountInput.required = true;
            }
            updateCommissionDisplay();
        } else if (type === 'EXTERNAL_INCOME') {
            toAccountGroup.style.display = 'block';
            counterpartyGroup.style.display = 'block';
            currencyGroup.style.display = 'block';
            amountGroup.style.display = 'block';
            if (onLoadCounterparties) {
                onLoadCounterparties('INCOME');
            }
        } else if (type === 'EXTERNAL_EXPENSE') {
            fromAccountGroup.style.display = 'block';
            counterpartyGroup.style.display = 'block';
            currencyGroup.style.display = 'block';
            amountGroup.style.display = 'block';
            if (onLoadCounterparties) {
                onLoadCounterparties('EXPENSE');
            }
        } else if (type === 'CLIENT_PAYMENT') {
            fromAccountGroup.style.display = 'block';
            clientGroup.style.display = 'block';
            currencyGroup.style.display = 'block';
            amountGroup.style.display = 'block';
        } else if (type === 'CURRENCY_CONVERSION') {
            conversionAccountGroup.style.display = 'block';
            conversionCurrencyGroup.style.display = 'block';
            conversionReceivedAmountGroup.style.display = 'block';
            conversionExchangeRateDisplayGroup.style.display = 'block';
            currencyGroup.style.display = 'block';
            amountGroup.style.display = 'block';
            if (currencyGroup) {
                const label = currencyGroup.querySelector('label');
                if (label) label.textContent = 'З валюту:';
            }
            if (amountLabel) {
                amountLabel.textContent = 'Сума списання:';
            }
            const conversionReceivedAmountInput = document.getElementById('conversion-received-amount');
            if (conversionReceivedAmountInput) {
                conversionReceivedAmountInput.required = true;
            }
            updateConversionExchangeRateDisplay();
        }
        
        const receivedAmountInput = document.getElementById('transaction-received-amount');
        if (receivedAmountInput && type !== 'INTERNAL_TRANSFER') {
            receivedAmountInput.required = false;
        }
        
        const conversionReceivedAmountInput = document.getElementById('conversion-received-amount');
        if (conversionReceivedAmountInput && type !== 'CURRENCY_CONVERSION') {
            conversionReceivedAmountInput.required = false;
        }
    }

    function updateCommissionDisplay() {
        const type = document.getElementById('transaction-type').value;
        if (type !== 'INTERNAL_TRANSFER') {
            return;
        }
        
        const amountInput = document.getElementById('transaction-amount');
        const receivedAmountInput = document.getElementById('transaction-received-amount');
        const commissionDisplay = document.getElementById('transaction-commission-display');
        
        if (!amountInput || !receivedAmountInput || !commissionDisplay) {
            return;
        }
        
        const amount = parseFloat(amountInput.value) || 0;
        const receivedAmount = parseFloat(receivedAmountInput.value) || 0;
        
        if (amount > 0 && receivedAmount > 0) {
            const commission = amount - receivedAmount;
            commissionDisplay.textContent = commission.toFixed(2);
            if (commission < 0) {
                commissionDisplay.style.color = '#d32f2f';
            } else if (commission > 0) {
                commissionDisplay.style.color = '#1976d2';
            } else {
                commissionDisplay.style.color = '#666';
            }
        } else {
            commissionDisplay.textContent = '0.00';
            commissionDisplay.style.color = '#666';
        }
    }

    function updateConversionExchangeRateDisplay() {
        const type = document.getElementById('transaction-type').value;
        if (type !== 'CURRENCY_CONVERSION') {
            return;
        }
        
        const amountInput = document.getElementById('transaction-amount');
        const receivedAmountInput = document.getElementById('conversion-received-amount');
        const exchangeRateDisplay = document.getElementById('conversion-exchange-rate-display');
        
        if (!amountInput || !receivedAmountInput || !exchangeRateDisplay) {
            return;
        }
        
        const amount = parseFloat(amountInput.value) || 0;
        const receivedAmount = parseFloat(receivedAmountInput.value) || 0;
        
        if (amount > 0 && receivedAmount > 0) {
            const exchangeRate = amount / receivedAmount;
            exchangeRateDisplay.textContent = exchangeRate.toFixed(6);
            exchangeRateDisplay.style.color = '#1976d2';
        } else {
            exchangeRateDisplay.textContent = '0.000000';
            exchangeRateDisplay.style.color = '#666';
        }
    }

    function resetFormDisplays() {
        const commissionDisplay = document.getElementById('transaction-commission-display');
        if (commissionDisplay) {
            commissionDisplay.textContent = '0.00';
            commissionDisplay.style.color = '#666';
        }
        
        const conversionExchangeRateDisplay = document.getElementById('conversion-exchange-rate-display');
        if (conversionExchangeRateDisplay) {
            conversionExchangeRateDisplay.textContent = '0.000000';
            conversionExchangeRateDisplay.style.color = '#666';
        }
    }

    function buildTransactionFormData() {
        const type = document.getElementById('transaction-type').value;
        const categoryValue = document.getElementById('transaction-category').value;
        
        const descriptionInput = document.getElementById('transaction-description');
        const descriptionValue = descriptionInput ? descriptionInput.value.trim() : '';
        
        const currencyInput = document.getElementById('transaction-currency');
        const currencyValue = currencyInput ? currencyInput.value.trim() : '';
        
        const formData = {
            type: type,
            amount: parseFloat(document.getElementById('transaction-amount').value),
            currency: currencyValue || null,
            description: descriptionValue || null
        };
        
        if (categoryValue && categoryValue.trim() !== '') {
            formData.categoryId = parseInt(categoryValue);
        }

        if (type === 'INTERNAL_TRANSFER') {
            let fromAccountValue = '';
            let toAccountValue = '';
            if (customSelects && customSelects['from-account']) {
                const values = customSelects['from-account'].getValue();
                fromAccountValue = values.length > 0 ? values[0] : '';
            } else {
                const fromAccountSelect = document.getElementById('from-account');
                fromAccountValue = fromAccountSelect ? fromAccountSelect.value : '';
            }
            if (customSelects && customSelects['to-account']) {
                const values = customSelects['to-account'].getValue();
                toAccountValue = values.length > 0 ? values[0] : '';
            } else {
                const toAccountSelect = document.getElementById('to-account');
                toAccountValue = toAccountSelect ? toAccountSelect.value : '';
            }
            formData.fromAccountId = parseInt(fromAccountValue);
            formData.toAccountId = parseInt(toAccountValue);
            
            const receivedAmountValue = document.getElementById('transaction-received-amount').value;
            if (!receivedAmountValue || receivedAmountValue.trim() === '') {
                return { error: FINANCE_MESSAGES.SPECIFY_RECEIVED_AMOUNT };
            }
            
            const receivedAmount = parseFloat(receivedAmountValue);
            const amount = formData.amount;
            
            if (receivedAmount > amount) {
                return { error: FINANCE_MESSAGES.RECEIVED_AMOUNT_TOO_LARGE };
            }
            
            if (receivedAmount <= 0) {
                return { error: FINANCE_MESSAGES.RECEIVED_AMOUNT_MUST_BE_POSITIVE };
            }
            
            const commission = amount - receivedAmount;
            if (commission > 0) {
                formData.commission = commission;
            }
        } else if (type === 'EXTERNAL_INCOME') {
            let toAccountValue = '';
            if (customSelects && customSelects['to-account']) {
                const values = customSelects['to-account'].getValue();
                toAccountValue = values.length > 0 ? values[0] : '';
            } else {
                const toAccountSelect = document.getElementById('to-account');
                toAccountValue = toAccountSelect ? toAccountSelect.value : '';
            }
            formData.toAccountId = parseInt(toAccountValue);
            let counterpartyId = '';
            if (customSelects && customSelects['transaction-counterparty']) {
                const values = customSelects['transaction-counterparty'].getValue();
                counterpartyId = values.length > 0 ? values[0] : '';
            } else {
                const counterpartySelect = document.getElementById('transaction-counterparty');
                counterpartyId = counterpartySelect ? counterpartySelect.value : '';
            }
            if (counterpartyId && counterpartyId.trim() !== '') {
                formData.counterpartyId = parseInt(counterpartyId);
            }
        } else if (type === 'EXTERNAL_EXPENSE') {
            let fromAccountValue = '';
            if (customSelects && customSelects['from-account']) {
                const values = customSelects['from-account'].getValue();
                fromAccountValue = values.length > 0 ? values[0] : '';
            } else {
                const fromAccountSelect = document.getElementById('from-account');
                fromAccountValue = fromAccountSelect ? fromAccountSelect.value : '';
            }
            formData.fromAccountId = parseInt(fromAccountValue);
            let counterpartyId = '';
            if (customSelects && customSelects['transaction-counterparty']) {
                const values = customSelects['transaction-counterparty'].getValue();
                counterpartyId = values.length > 0 ? values[0] : '';
            } else {
                const counterpartySelect = document.getElementById('transaction-counterparty');
                counterpartyId = counterpartySelect ? counterpartySelect.value : '';
            }
            if (counterpartyId && counterpartyId.trim() !== '') {
                formData.counterpartyId = parseInt(counterpartyId);
            }
        } else if (type === 'CLIENT_PAYMENT') {
            let fromAccountValue = '';
            if (customSelects && customSelects['from-account']) {
                const values = customSelects['from-account'].getValue();
                fromAccountValue = values.length > 0 ? values[0] : '';
            } else {
                const fromAccountSelect = document.getElementById('from-account');
                fromAccountValue = fromAccountSelect ? fromAccountSelect.value : '';
            }
            formData.fromAccountId = parseInt(fromAccountValue);
            const clientId = document.getElementById('transaction-client-id').value;
            if (!clientId) {
                return { error: FINANCE_MESSAGES.SELECT_CLIENT };
            }
            formData.clientId = parseInt(clientId);
        } else if (type === 'CURRENCY_CONVERSION') {
            let conversionAccountValue = '';
            if (customSelects && customSelects['conversion-account']) {
                const values = customSelects['conversion-account'].getValue();
                conversionAccountValue = values.length > 0 ? values[0] : '';
            } else {
                const conversionAccountSelect = document.getElementById('conversion-account');
                conversionAccountValue = conversionAccountSelect ? conversionAccountSelect.value : '';
            }
            const accountId = parseInt(conversionAccountValue);
            formData.fromAccountId = accountId;
            formData.toAccountId = accountId;
            const convertedCurrencyInput = document.getElementById('conversion-currency');
            formData.convertedCurrency = convertedCurrencyInput ? convertedCurrencyInput.value.trim() : null;
            
            const receivedAmountValue = document.getElementById('conversion-received-amount').value;
            if (!receivedAmountValue || receivedAmountValue.trim() === '') {
                return { error: FINANCE_MESSAGES.SPECIFY_RECEIVED_AMOUNT };
            }
            
            const receivedAmount = parseFloat(receivedAmountValue);
            const amount = formData.amount;
            
            if (receivedAmount <= 0) {
                return { error: FINANCE_MESSAGES.RECEIVED_AMOUNT_MUST_BE_POSITIVE };
            }
            
            if (amount <= 0) {
                return { error: FINANCE_MESSAGES.WRITE_OFF_AMOUNT_MUST_BE_POSITIVE };
            }
            
            const exchangeRate = amount / receivedAmount;
            formData.exchangeRate = exchangeRate;
            formData.convertedAmount = receivedAmount;
        }

        return formData;
    }

    function buildUpdateTransactionData() {
        const transactionId = document.getElementById('edit-transaction-id').value;
        const categoryId = document.getElementById('edit-transaction-category').value;
        const amount = parseFloat(document.getElementById('edit-transaction-amount').value);
        const descriptionInput = document.getElementById('edit-transaction-description');
        const descriptionValue = descriptionInput ? descriptionInput.value.trim() : '';
        const exchangeRateInput = document.getElementById('edit-transaction-exchange-rate');
        const exchangeRate = exchangeRateInput && exchangeRateInput.style.display !== 'none' 
            ? parseFloat(exchangeRateInput.value) 
            : null;
        
        const updateData = {
            categoryId: categoryId && categoryId.trim() !== '' ? parseInt(categoryId) : null,
            description: descriptionValue || null,
            amount: amount
        };
        
        if (exchangeRate !== null && !isNaN(exchangeRate)) {
            updateData.exchangeRate = exchangeRate;
        }
        
        const commissionInput = document.getElementById('edit-transaction-commission');
        if (commissionInput && commissionInput.style.display !== 'none') {
            const commissionValue = commissionInput.value;
            if (commissionValue && commissionValue.trim() !== '') {
                const commission = parseFloat(commissionValue);
                if (!isNaN(commission) && commission >= 0) {
                    updateData.commission = commission;
                } else {
                    updateData.commission = null;
                }
            } else {
                updateData.commission = null;
            }
        }
        
        return { transactionId, updateData, shouldDelete: amount === 0 || isNaN(amount) };
    }

    return {
        populateAccounts,
        populateCurrencies,
        populateCategories,
        handleTransactionTypeChange,
        updateCommissionDisplay,
        updateConversionExchangeRateDisplay,
        resetFormDisplays,
        buildTransactionFormData,
        buildUpdateTransactionData
    };
})();
