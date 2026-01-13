const FinanceUtils = (function() {
    const TRANSACTION_TYPE_MAP = {
        'INTERNAL_TRANSFER': 'Переказ між рахунками',
        'EXTERNAL_INCOME': 'Зовнішній прихід',
        'EXTERNAL_EXPENSE': 'Зовнішня витрата',
        'CLIENT_PAYMENT': 'Оплата клієнту',
        'CURRENCY_CONVERSION': 'Конвертація валют',
        'PURCHASE': 'Закупівля'
    };
    
    function formatNumber(value, maxDecimals = 2) {
        if (value === null || value === undefined || value === '') return '0';
        const num = parseFloat(value);
        if (isNaN(num)) return '0';
        return parseFloat(num.toFixed(maxDecimals)).toString();
    }
    
    function formatBranchBalance(balanceObj) {
        if (!balanceObj || Object.keys(balanceObj).length === 0) {
            return { text: 'Немає балансу', hasBalance: false };
        }
        
        const parts = [];
        const currencies = ['UAH', 'USD', 'EUR'];
        
        currencies.forEach(currency => {
            if (balanceObj[currency] !== undefined && balanceObj[currency] !== 0) {
                const amount = formatNumber(balanceObj[currency]);
                parts.push(`${amount} ${currency}`);
            }
        });
        
        if (parts.length === 0) {
            return { text: '0.00', hasBalance: false };
        }
        
        return { text: parts.join(', '), hasBalance: true };
    }
    
    function createBranchBalanceElement(balanceObj) {
        const balanceData = formatBranchBalance(balanceObj);
        const container = document.createElement('span');
        if (!balanceData.hasBalance) {
            container.className = 'text-muted';
        }
        container.textContent = balanceData.text;
        return container;
    }
    
    function getUserName(userId, usersCache) {
        const user = usersCache.find(u => Number(u.id) === Number(userId));
        return user ? (user.fullName || user.name) : `User ${userId}`;
    }
    
    function getBranchName(branchId, branchesCache) {
        const branch = branchesCache.find(b => Number(b.id) === Number(branchId));
        return branch ? branch.name : `Branch ${branchId}`;
    }
    
    function getTransactionTypeName(type) {
        return TRANSACTION_TYPE_MAP[type] || type || CLIENT_MESSAGES.EMPTY_VALUE;
    }
    
    function initializeClientAutocomplete(inputId, hiddenInputId, dropdownId) {
        const input = document.getElementById(inputId);
        const hiddenInput = document.getElementById(hiddenInputId);
        const dropdown = document.getElementById(dropdownId);
        
        if (!input || !hiddenInput || !dropdown) return;
        
        let selectedClientId = null;
        let searchTimeout = null;
        let isLoading = false;
        
        function debounceSearch(query) {
            if (searchTimeout) {
                clearTimeout(searchTimeout);
            }
            
            if (query.length < 2) {
                dropdown.style.display = 'none';
                hiddenInput.value = '';
                selectedClientId = null;
                return;
            }
            
            searchTimeout = setTimeout(async () => {
                if (isLoading) return;
                
                isLoading = true;
                dropdown.textContent = '';
                const loadingItem = document.createElement('div');
                loadingItem.className = 'client-autocomplete-item';
                loadingItem.textContent = CLIENT_MESSAGES.LOADING;
                dropdown.appendChild(loadingItem);
                dropdown.style.display = 'block';
                
                try {
                    const clients = await FinanceDataLoader.searchClients(query, 20);
                    
                    dropdown.textContent = '';
                    
                    if (clients.length === 0) {
                        const noResultsItem = document.createElement('div');
                        noResultsItem.className = 'client-autocomplete-item';
                        noResultsItem.textContent = CLIENT_MESSAGES.NO_DATA;
                        dropdown.appendChild(noResultsItem);
                    } else {
                        clients.forEach(client => {
                            const item = document.createElement('div');
                            item.className = 'client-autocomplete-item';
                            const companyName = client.company || `Клієнт #${client.id}`;
                            item.textContent = companyName;
                            item.addEventListener('click', () => {
                                input.value = companyName;
                                hiddenInput.value = client.id;
                                selectedClientId = client.id;
                                dropdown.style.display = 'none';
                            });
                            dropdown.appendChild(item);
                        });
                    }
                    
                    dropdown.style.display = 'block';
                } catch (error) {
                    console.error('Error searching clients:', error);
                    dropdown.textContent = '';
                    const errorItem = document.createElement('div');
                    errorItem.className = 'client-autocomplete-item';
                    errorItem.textContent = CLIENT_MESSAGES.LOAD_ERROR;
                    dropdown.appendChild(errorItem);
                    dropdown.style.display = 'block';
                } finally {
                    isLoading = false;
                }
            }, 300);
        }
        
        input.addEventListener('input', (e) => {
            const query = e.target.value.trim();
            debounceSearch(query);
        });
        
        document.addEventListener('click', (e) => {
            if (!input.contains(e.target) && !dropdown.contains(e.target)) {
                dropdown.style.display = 'none';
            }
        });
        
        input.addEventListener('focus', () => {
            if (input.value === '') {
                hiddenInput.value = '';
                selectedClientId = null;
            } else if (input.value.length >= 2) {
                debounceSearch(input.value.trim());
            }
        });
    }
    
    function canOperateAccount(accountId, accountsCache, branchesCache) {
        const account = accountsCache.find(a => Number(a.id) === Number(accountId));
        if (!account) return false;
        if (!account.branchId) return true;
        const branch = branchesCache.find(b => Number(b.id) === Number(account.branchId));
        return branch ? (branch.canOperate === true) : false;
    }
    
    function escapeHtml(text) {
        if (text == null || text === undefined) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    return {
        TRANSACTION_TYPE_MAP,
        formatNumber,
        formatBranchBalance,
        createBranchBalanceElement,
        getUserName,
        getBranchName,
        getTransactionTypeName,
        initializeClientAutocomplete,
        canOperateAccount,
        escapeHtml
    };
})();
