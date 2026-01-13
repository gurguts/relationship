const DeclarantCalculations = (function() {
    async function getVehicleExpensesTotal(vehicleId) {
        if (!vehicleId) return 0;
        try {
            const expenses = await DeclarantDataLoader.loadVehicleExpenses(vehicleId);
            return expenses.reduce((sum, e) => sum + (parseFloat(e.convertedAmount) || 0), 0);
        } catch (error) {
            return 0;
        }
    }
    
    function calculateProductsTotalCost(vehicle) {
        if (!vehicle || !vehicle.items || vehicle.items.length === 0) {
            return 0;
        }
        return vehicle.items.reduce((sum, item) => {
            const itemTotalCost = parseFloat(item.totalCostEur) || 0;
            return sum + itemTotalCost;
        }, 0);
    }
    
    function calculateFullReclamation(vehicle) {
        const reclamationPerTon = parseFloat(vehicle.reclamation) || 0;
        if (reclamationPerTon === 0) {
            return 0;
        }
        
        const productQuantityStr = vehicle.productQuantity;
        if (!productQuantityStr || productQuantityStr.trim() === '') {
            return 0;
        }
        
        try {
            const quantityInTons = parseFloat(productQuantityStr.replace(',', '.')) || 0;
            return reclamationPerTon * quantityInTons;
        } catch (error) {
            console.warn('Failed to parse productQuantity for reclamation calculation:', productQuantityStr, error);
            return 0;
        }
    }
    
    function calculateInvoiceUaTotalPrice() {
        const vehicleProductQuantity = document.getElementById('vehicle-product-quantity');
        const vehicleInvoiceUaPricePerTon = document.getElementById('vehicle-invoice-ua-price-per-ton');
        const vehicleInvoiceUaTotalPrice = document.getElementById('vehicle-invoice-ua-total-price');
        
        const quantity = parseFloat(vehicleProductQuantity?.value?.replace(',', '.') || '0');
        const pricePerTon = parseFloat(vehicleInvoiceUaPricePerTon?.value || '0');
        if (quantity > 0 && pricePerTon > 0) {
            const total = quantity * pricePerTon;
            if (vehicleInvoiceUaTotalPrice) {
                vehicleInvoiceUaTotalPrice.value = total.toFixed(6);
            }
        } else if (vehicleInvoiceUaTotalPrice) {
            vehicleInvoiceUaTotalPrice.value = '';
        }
    }
    
    function calculateInvoiceEuTotalPrice() {
        const vehicleProductQuantity = document.getElementById('vehicle-product-quantity');
        const vehicleInvoiceEuPricePerTon = document.getElementById('vehicle-invoice-eu-price-per-ton');
        const vehicleInvoiceEuTotalPrice = document.getElementById('vehicle-invoice-eu-total-price');
        
        const quantity = parseFloat(vehicleProductQuantity?.value?.replace(',', '.') || '0');
        const pricePerTon = parseFloat(vehicleInvoiceEuPricePerTon?.value || '0');
        if (quantity > 0 && pricePerTon > 0) {
            const total = quantity * pricePerTon;
            if (vehicleInvoiceEuTotalPrice) {
                vehicleInvoiceEuTotalPrice.value = total.toFixed(6);
            }
        } else if (vehicleInvoiceEuTotalPrice) {
            vehicleInvoiceEuTotalPrice.value = '';
        }
    }
    
    function calculateDetailInvoiceUaTotalPrice() {
        const detailVehicleProductQuantityInput = document.getElementById('detail-vehicle-product-quantity');
        const detailVehicleInvoiceUaPricePerTonInput = document.getElementById('detail-vehicle-invoice-ua-price-per-ton');
        const detailVehicleInvoiceUaTotalPriceInput = document.getElementById('detail-vehicle-invoice-ua-total-price');
        
        const quantity = parseFloat(detailVehicleProductQuantityInput?.value?.replace(',', '.') || '0');
        const pricePerTon = parseFloat(detailVehicleInvoiceUaPricePerTonInput?.value || '0');
        if (quantity > 0 && pricePerTon > 0) {
            const total = quantity * pricePerTon;
            if (detailVehicleInvoiceUaTotalPriceInput) {
                detailVehicleInvoiceUaTotalPriceInput.value = total.toFixed(6);
            }
        } else if (detailVehicleInvoiceUaTotalPriceInput) {
            detailVehicleInvoiceUaTotalPriceInput.value = '';
        }
    }
    
    function calculateDetailInvoiceEuTotalPrice() {
        const detailVehicleProductQuantityInput = document.getElementById('detail-vehicle-product-quantity');
        const detailVehicleInvoiceEuPricePerTonInput = document.getElementById('detail-vehicle-invoice-eu-price-per-ton');
        const detailVehicleInvoiceEuTotalPriceInput = document.getElementById('detail-vehicle-invoice-eu-total-price');
        
        const quantity = parseFloat(detailVehicleProductQuantityInput?.value?.replace(',', '.') || '0');
        const pricePerTon = parseFloat(detailVehicleInvoiceEuPricePerTonInput?.value || '0');
        if (quantity > 0 && pricePerTon > 0) {
            const total = quantity * pricePerTon;
            if (detailVehicleInvoiceEuTotalPriceInput) {
                detailVehicleInvoiceEuTotalPriceInput.value = total.toFixed(6);
            }
        } else if (detailVehicleInvoiceEuTotalPriceInput) {
            detailVehicleInvoiceEuTotalPriceInput.value = '';
        }
    }
    
    return {
        getVehicleExpensesTotal,
        calculateProductsTotalCost,
        calculateFullReclamation,
        calculateInvoiceUaTotalPrice,
        calculateInvoiceEuTotalPrice,
        calculateDetailInvoiceUaTotalPrice,
        calculateDetailInvoiceEuTotalPrice
    };
})();
