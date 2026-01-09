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
            return '<span class="text-muted">Немає балансу</span>';
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
            return '<span class="text-muted">0.00</span>';
        }
        
        return parts.join(', ');
    }
    
    function getUserName(userId, usersCache) {
        const user = usersCache.find(u => u.id === userId);
        return user ? (user.fullName || user.name) : `User ${userId}`;
    }
    
    function getBranchName(branchId, branchesCache) {
        const branch = branchesCache.find(b => b.id === branchId);
        return branch ? branch.name : `Branch ${branchId}`;
    }
    
    function getTransactionTypeName(type) {
        return TRANSACTION_TYPE_MAP[type] || type || CLIENT_MESSAGES.EMPTY_VALUE;
    }
    
    return {
        TRANSACTION_TYPE_MAP,
        formatNumber,
        formatBranchBalance,
        getUserName,
        getBranchName,
        getTransactionTypeName
    };
})();
