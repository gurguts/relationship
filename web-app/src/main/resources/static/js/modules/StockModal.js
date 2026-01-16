const StockModal = (function() {
    function closeModal(modalId, onClose) {
        const modal = document.getElementById(modalId);
        if (!modal) return;
        
        modal.classList.remove('open');
        setTimeout(() => {
            modal.style.display = 'none';
            document.body.classList.remove('modal-open');
            if (onClose) {
                onClose();
            }
        }, CLIENT_CONSTANTS.MODAL_CLOSE_DELAY);
    }
    
    function openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;
        
        modal.style.display = 'flex';
        setTimeout(() => {
            modal.classList.add('open');
        }, CLIENT_CONSTANTS.MODAL_ANIMATION_DELAY);
        document.body.classList.add('modal-open');
    }
    
    function setupModalClickHandlers(modalId, onClose) {
        const modal = document.getElementById(modalId);
        if (!modal) return;
        
        const modalContent = modal.querySelector('.modal-content');
        if (!modalContent) return;
        
        const clickHandler = (e) => {
            if (!modalContent.contains(e.target)) {
                closeModal(modalId, onClose);
            }
        };
        
        modal.addEventListener('click', clickHandler);
    }
    
    function resetBalanceEditModal() {
        const balanceEditForm = document.getElementById('balance-edit-form');
        const balanceEditQuantityInput = document.getElementById('balance-edit-quantity');
        const balanceEditTotalCostInput = document.getElementById('balance-edit-total-cost');
        const balanceEditDescriptionInput = document.getElementById('balance-edit-description');
        const balanceEditModeRadios = document.querySelectorAll('input[name="balance-edit-mode"]');
        const balanceHistoryBody = document.getElementById('balance-history-body');
        const balanceHistoryEmpty = document.getElementById('balance-history-empty');
        
        if (balanceEditForm) {
            balanceEditForm.reset();
        }
        
        if (balanceEditQuantityInput) {
            balanceEditQuantityInput.removeAttribute('disabled');
            balanceEditQuantityInput.value = '';
        }
        if (balanceEditTotalCostInput) {
            balanceEditTotalCostInput.setAttribute('disabled', 'disabled');
            balanceEditTotalCostInput.value = '';
        }
        if (balanceEditDescriptionInput) {
            balanceEditDescriptionInput.value = '';
        }
        
        balanceEditModeRadios.forEach(radio => {
            radio.checked = radio.value === 'quantity';
            const label = radio.closest('label');
            if (label) {
                label.classList.toggle('active', radio.checked);
            }
        });
        
        if (balanceHistoryBody) {
            balanceHistoryBody.textContent = '';
        }
        if (balanceHistoryEmpty) {
            balanceHistoryEmpty.style.display = 'none';
        }
        
        updateBalanceEditMode();
    }
    
    function updateBalanceEditMode() {
        const selected = document.querySelector('input[name="balance-edit-mode"]:checked');
        if (!selected) return;
        
        const balanceEditQuantityInput = document.getElementById('balance-edit-quantity');
        const balanceEditTotalCostInput = document.getElementById('balance-edit-total-cost');
        const balanceEditModeRadios = document.querySelectorAll('input[name="balance-edit-mode"]');
        
        balanceEditModeRadios.forEach(radio => {
            const label = radio.closest('label');
            if (label) {
                label.classList.toggle('active', radio === selected);
            }
        });
        
        if (selected.value === 'quantity') {
            if (balanceEditQuantityInput) {
                balanceEditQuantityInput.removeAttribute('disabled');
                balanceEditQuantityInput.focus();
            }
            if (balanceEditTotalCostInput) {
                balanceEditTotalCostInput.setAttribute('disabled', 'disabled');
            }
        } else {
            if (balanceEditTotalCostInput) {
                balanceEditTotalCostInput.removeAttribute('disabled');
                balanceEditTotalCostInput.focus();
            }
            if (balanceEditQuantityInput) {
                balanceEditQuantityInput.setAttribute('disabled', 'disabled');
            }
        }
    }
    
    function openBalanceEditModal(data, onLoadHistory) {
        const balanceEditModal = document.getElementById('balance-edit-modal');
        if (!balanceEditModal) return;
        
        resetBalanceEditModal();
        
        const balanceEditWarehouseIdInput = document.getElementById('balance-edit-warehouse-id');
        const balanceEditProductIdInput = document.getElementById('balance-edit-product-id');
        const balanceEditQuantityInput = document.getElementById('balance-edit-quantity');
        const balanceEditTotalCostInput = document.getElementById('balance-edit-total-cost');
        
        if (balanceEditWarehouseIdInput) {
            balanceEditWarehouseIdInput.value = data.warehouseId || '';
        }
        if (balanceEditProductIdInput) {
            balanceEditProductIdInput.value = data.productId || '';
        }
        
        if (balanceEditQuantityInput && Number.isFinite(data.quantity)) {
            balanceEditQuantityInput.value = Number(data.quantity).toFixed(2);
        }
        if (balanceEditTotalCostInput && Number.isFinite(data.totalCost)) {
            balanceEditTotalCostInput.value = Number(data.totalCost).toFixed(6);
        }
        
        updateBalanceEditMode();
        
        openModal('balance-edit-modal');
        
        if (onLoadHistory) {
            onLoadHistory(data.warehouseId, data.productId);
        }
    }
    
    function resetBalanceHistoryModal() {
        const balanceHistoryBody = document.getElementById('balance-history-body');
        const balanceHistoryEmpty = document.getElementById('balance-history-empty');
        
        if (balanceHistoryBody) {
            balanceHistoryBody.textContent = '';
        }
        if (balanceHistoryEmpty) {
            balanceHistoryEmpty.style.display = 'none';
        }
    }
    
    function openBalanceHistoryModal() {
        const balanceHistoryModal = document.getElementById('balance-history-modal');
        if (!balanceHistoryModal) return;
        
        resetBalanceHistoryModal();
        openModal('balance-history-modal');
    }
    
    function hideDriverBalanceInfo() {
        const driverBalanceInfo = document.getElementById('driver-balance-info');
        const driverNoBalanceWarning = document.getElementById('driver-no-balance-warning');
        if (driverBalanceInfo) {
            driverBalanceInfo.style.display = 'none';
        }
        if (driverNoBalanceWarning) {
            driverNoBalanceWarning.style.display = 'none';
        }
    }
    
    function showDriverBalanceInfo(balance) {
        const driverBalanceInfo = document.getElementById('driver-balance-info');
        const driverNoBalanceWarning = document.getElementById('driver-no-balance-warning');
        
        if (!balance || !balance.quantity || parseFloat(balance.quantity) === 0) {
            if (driverBalanceInfo) {
                driverBalanceInfo.style.display = 'none';
            }
            if (driverNoBalanceWarning) {
                driverNoBalanceWarning.style.display = 'block';
            }
            return;
        }
        
        if (driverBalanceInfo) {
            const quantityElement = document.getElementById('driver-balance-quantity');
            const priceElement = document.getElementById('driver-balance-price');
            const totalElement = document.getElementById('driver-balance-total');
            
            if (quantityElement) {
                quantityElement.textContent = StockUtils.formatNumber(balance.quantity, 2);
            }
            if (priceElement) {
                priceElement.textContent = StockUtils.formatNumber(balance.averagePriceEur, 6);
            }
            if (totalElement) {
                totalElement.textContent = StockUtils.formatNumber(balance.totalCostEur, 6);
            }
            
            driverBalanceInfo.style.display = 'block';
        }
        if (driverNoBalanceWarning) {
            driverNoBalanceWarning.style.display = 'none';
        }
    }
    
    function hideWarehouseBalanceInfo() {
        const warehouseBalanceInfo = document.getElementById('warehouse-balance-info');
        const warehouseNoBalanceWarning = document.getElementById('warehouse-no-balance-warning');
        if (warehouseBalanceInfo) {
            warehouseBalanceInfo.style.display = 'none';
        }
        if (warehouseNoBalanceWarning) {
            warehouseNoBalanceWarning.style.display = 'none';
        }
    }
    
    function showWarehouseBalanceInfo(balance) {
        const warehouseBalanceInfo = document.getElementById('warehouse-balance-info');
        const warehouseNoBalanceWarning = document.getElementById('warehouse-no-balance-warning');
        
        if (!balance || !balance.quantity || parseFloat(balance.quantity) === 0) {
            if (warehouseBalanceInfo) {
                warehouseBalanceInfo.style.display = 'none';
            }
            if (warehouseNoBalanceWarning) {
                warehouseNoBalanceWarning.style.display = 'block';
            }
            return;
        }
        
        if (warehouseBalanceInfo) {
            const quantityElement = document.getElementById('warehouse-balance-quantity');
            const priceElement = document.getElementById('warehouse-balance-price');
            const totalElement = document.getElementById('warehouse-balance-total');
            
            if (quantityElement) {
                quantityElement.textContent = StockUtils.formatNumber(balance.quantity, 2);
            }
            if (priceElement) {
                priceElement.textContent = StockUtils.formatNumber(balance.averagePriceEur, 6);
            }
            if (totalElement) {
                totalElement.textContent = StockUtils.formatNumber(balance.totalCostEur, 6);
            }
            
            warehouseBalanceInfo.style.display = 'block';
        }
        if (warehouseNoBalanceWarning) {
            warehouseNoBalanceWarning.style.display = 'none';
        }
    }
    
    function populateVehicleForm(vehicle) {
        const detailVehicleDateInput = document.getElementById('detail-vehicle-date');
        const detailVehicleVehicleInput = document.getElementById('detail-vehicle-vehicle-number');
        const detailVehicleDescriptionInput = document.getElementById('detail-vehicle-description');
        
        if (!vehicle) {
            if (detailVehicleDateInput) detailVehicleDateInput.value = '';
            if (detailVehicleVehicleInput) detailVehicleVehicleInput.value = '';
            if (detailVehicleDescriptionInput) detailVehicleDescriptionInput.value = '';
            return;
        }
        
        if (detailVehicleDateInput) {
            detailVehicleDateInput.value = vehicle.shipmentDate || '';
        }
        if (detailVehicleVehicleInput) {
            detailVehicleVehicleInput.value = vehicle.vehicleNumber || '';
        }
        if (detailVehicleDescriptionInput) {
            detailVehicleDescriptionInput.value = vehicle.description || '';
        }
    }
    
    function setVehicleFormEditable(isEditable) {
        const detailVehicleDateInput = document.getElementById('detail-vehicle-date');
        const detailVehicleVehicleInput = document.getElementById('detail-vehicle-vehicle-number');
        const detailVehicleDescriptionInput = document.getElementById('detail-vehicle-description');
        const saveVehicleBtn = document.getElementById('save-vehicle-btn');
        const editVehicleBtn = document.getElementById('edit-vehicle-btn');
        
        const fields = [
            detailVehicleDateInput,
            detailVehicleVehicleInput,
            detailVehicleDescriptionInput
        ];
        
        fields.forEach(field => {
            if (field) {
                field.disabled = !isEditable;
            }
        });
        
        if (saveVehicleBtn) {
            saveVehicleBtn.style.display = isEditable ? 'inline-flex' : 'none';
        }
        if (editVehicleBtn) {
            editVehicleBtn.style.display = isEditable ? 'none' : 'block';
        }
    }
    
    function resetVehicleFormState(vehicle) {
        populateVehicleForm(vehicle);
        setVehicleFormEditable(false);
    }
    
    return {
        closeModal,
        openModal,
        setupModalClickHandlers,
        resetBalanceEditModal,
        updateBalanceEditMode,
        openBalanceEditModal,
        resetBalanceHistoryModal,
        openBalanceHistoryModal,
        hideDriverBalanceInfo,
        showDriverBalanceInfo,
        hideWarehouseBalanceInfo,
        showWarehouseBalanceInfo,
        populateVehicleForm,
        setVehicleFormEditable,
        resetVehicleFormState
    };
})();
