const DeclarantModal = (function() {
    function openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.add('open');
            document.body.classList.add('modal-open');
        }
    }
    
    function closeModal(modalId, onClose) {
        const modal = document.getElementById(modalId);
        if (!modal) return;
        
        modal.classList.remove('open');
        document.body.classList.remove('modal-open');
        
        if (onClose) onClose();
    }
    
    function setupModalClickHandlers(modalId, onClose) {
        const modal = document.getElementById(modalId);
        if (!modal) return;
        
        const modalContent = modal.querySelector('.modal-container');
        let modalClickHandler = null;
        
        if (modal._modalClickHandler) {
            modal.removeEventListener('click', modal._modalClickHandler);
        }
        
        modalClickHandler = (e) => {
            if (modalContent && !modalContent.contains(e.target)) {
                closeModal(modalId, onClose);
            } else if (!modalContent && e.target === modal) {
                closeModal(modalId, onClose);
            }
        };
        
        modal.addEventListener('click', modalClickHandler);
        modal._modalClickHandler = modalClickHandler;
    }
    
    function initializeModalClickHandlers() {
        const modals = [
            document.getElementById('vehicles-filter-modal'),
            document.getElementById('create-vehicle-modal'),
            document.getElementById('vehicle-details-modal'),
            document.getElementById('create-vehicle-expense-modal'),
            document.getElementById('edit-vehicle-expense-modal'),
            document.getElementById('edit-vehicle-item-modal'),
            document.getElementById('manage-carriers-modal'),
            document.getElementById('carrier-form-modal')
        ];

        modals.forEach(modal => {
            if (!modal) return;

            if (modal._modalClickHandler) {
                modal.removeEventListener('click', modal._modalClickHandler);
                modal._modalClickHandler = null;
            }
        });

        document.querySelectorAll('.modal-close').forEach(btn => {
            if (btn._closeModalHandler) {
                btn.removeEventListener('click', btn._closeModalHandler);
            }

            btn._closeModalHandler = () => {
                const modal = btn.closest('.modal-overlay');
                if (modal) {
                    closeModal(modal.id);
                }
            };

            btn.addEventListener('click', btn._closeModalHandler);
        });
    }
    
    function populateVehicleForm(vehicle) {
        const detailVehicleDateInput = document.getElementById('detail-vehicle-date');
        const detailVehicleVehicleInput = document.getElementById('detail-vehicle-vehicle-number');
        const detailVehicleInvoiceUaInput = document.getElementById('detail-vehicle-invoice-ua');
        const detailVehicleInvoiceEuInput = document.getElementById('detail-vehicle-invoice-eu');
        const detailVehicleDescriptionInput = document.getElementById('detail-vehicle-description');
        const detailVehicleSenderSelect = document.getElementById('detail-vehicle-sender');
        const detailVehicleReceiverSelect = document.getElementById('detail-vehicle-receiver');
        const detailVehicleTerminalSelect = document.getElementById('detail-vehicle-terminal');
        const detailVehicleDestinationCountrySelect = document.getElementById('detail-vehicle-destination-country');
        const detailVehicleDestinationPlaceSelect = document.getElementById('detail-vehicle-destination-place');
        const detailVehicleProductInput = document.getElementById('detail-vehicle-product');
        const detailVehicleProductQuantityInput = document.getElementById('detail-vehicle-product-quantity');
        const detailVehicleDeclarationNumberInput = document.getElementById('detail-vehicle-declaration-number');
        const detailVehicleDriverFullNameInput = document.getElementById('detail-vehicle-driver-full-name');
        const detailVehicleIsOurVehicleInput = document.getElementById('detail-vehicle-is-our-vehicle');
        const detailVehicleEur1Input = document.getElementById('detail-vehicle-eur1');
        const detailVehicleFitoInput = document.getElementById('detail-vehicle-fito');
        const detailVehicleCustomsDateInput = document.getElementById('detail-vehicle-customs-date');
        const detailVehicleCustomsClearanceDateInput = document.getElementById('detail-vehicle-customs-clearance-date');
        const detailVehicleUnloadingDateInput = document.getElementById('detail-vehicle-unloading-date');
        const detailVehicleCarrierSelect = document.getElementById('detail-vehicle-carrier-id');
        const detailVehicleInvoiceUaDateInput = document.getElementById('detail-vehicle-invoice-ua-date');
        const detailVehicleInvoiceUaPricePerTonInput = document.getElementById('detail-vehicle-invoice-ua-price-per-ton');
        const detailVehicleInvoiceUaTotalPriceInput = document.getElementById('detail-vehicle-invoice-ua-total-price');
        const detailVehicleInvoiceEuDateInput = document.getElementById('detail-vehicle-invoice-eu-date');
        const detailVehicleInvoiceEuPricePerTonInput = document.getElementById('detail-vehicle-invoice-eu-price-per-ton');
        const detailVehicleInvoiceEuTotalPriceInput = document.getElementById('detail-vehicle-invoice-eu-total-price');
        const detailVehicleReclamationInput = document.getElementById('detail-vehicle-reclamation');
        const detailVehicleFullReclamationInput = document.getElementById('detail-vehicle-full-reclamation');
        
        if (!vehicle) {
            if (detailVehicleDateInput) detailVehicleDateInput.value = '';
            if (detailVehicleVehicleInput) detailVehicleVehicleInput.value = '';
            if (detailVehicleInvoiceUaInput) detailVehicleInvoiceUaInput.value = '';
            if (detailVehicleInvoiceEuInput) detailVehicleInvoiceEuInput.value = '';
            if (detailVehicleDescriptionInput) detailVehicleDescriptionInput.value = '';
            if (detailVehicleSenderSelect) detailVehicleSenderSelect.value = '';
            if (detailVehicleReceiverSelect) detailVehicleReceiverSelect.value = '';
            if (detailVehicleDestinationCountryInput) detailVehicleDestinationCountryInput.value = '';
            if (detailVehicleDestinationPlaceInput) detailVehicleDestinationPlaceInput.value = '';
            if (detailVehicleProductInput) detailVehicleProductInput.value = '';
            if (detailVehicleProductQuantityInput) detailVehicleProductQuantityInput.value = '';
            if (detailVehicleDeclarationNumberInput) detailVehicleDeclarationNumberInput.value = '';
            if (detailVehicleTerminalInput) detailVehicleTerminalInput.value = '';
            if (detailVehicleDriverFullNameInput) detailVehicleDriverFullNameInput.value = '';
            if (detailVehicleIsOurVehicleInput) detailVehicleIsOurVehicleInput.checked = false;
            if (detailVehicleEur1Input) detailVehicleEur1Input.checked = false;
            if (detailVehicleFitoInput) detailVehicleFitoInput.checked = false;
            if (detailVehicleCustomsDateInput) detailVehicleCustomsDateInput.value = '';
            if (detailVehicleCustomsClearanceDateInput) detailVehicleCustomsClearanceDateInput.value = '';
            if (detailVehicleUnloadingDateInput) detailVehicleUnloadingDateInput.value = '';
            if (detailVehicleCarrierSelect) detailVehicleCarrierSelect.value = '';
            if (detailVehicleInvoiceUaDateInput) detailVehicleInvoiceUaDateInput.value = '';
            if (detailVehicleInvoiceUaPricePerTonInput) detailVehicleInvoiceUaPricePerTonInput.value = '';
            if (detailVehicleInvoiceUaTotalPriceInput) detailVehicleInvoiceUaTotalPriceInput.value = '';
            if (detailVehicleInvoiceEuDateInput) detailVehicleInvoiceEuDateInput.value = '';
            if (detailVehicleInvoiceEuPricePerTonInput) detailVehicleInvoiceEuPricePerTonInput.value = '';
            if (detailVehicleInvoiceEuTotalPriceInput) detailVehicleInvoiceEuTotalPriceInput.value = '';
            if (detailVehicleReclamationInput) detailVehicleReclamationInput.value = '';
            if (detailVehicleFullReclamationInput) detailVehicleFullReclamationInput.value = '';
            return;
        }

        if (detailVehicleDateInput) detailVehicleDateInput.value = vehicle.shipmentDate || '';
        if (detailVehicleVehicleInput) detailVehicleVehicleInput.value = vehicle.vehicleNumber || '';
        if (detailVehicleInvoiceUaInput) detailVehicleInvoiceUaInput.value = vehicle.invoiceUa || '';
        if (detailVehicleInvoiceEuInput) detailVehicleInvoiceEuInput.value = vehicle.invoiceEu || '';
        if (detailVehicleDescriptionInput) detailVehicleDescriptionInput.value = vehicle.description || '';
        if (detailVehicleSenderSelect) detailVehicleSenderSelect.value = vehicle.senderId || '';
        if (detailVehicleReceiverSelect) detailVehicleReceiverSelect.value = vehicle.receiverId || '';
        if (detailVehicleTerminalSelect) detailVehicleTerminalSelect.value = vehicle.terminalId || '';
        if (detailVehicleDestinationCountrySelect) detailVehicleDestinationCountrySelect.value = vehicle.destinationCountryId || '';
        if (detailVehicleDestinationPlaceSelect) detailVehicleDestinationPlaceSelect.value = vehicle.destinationPlaceId || '';
        if (detailVehicleProductInput) detailVehicleProductInput.value = vehicle.product || '';
        if (detailVehicleProductQuantityInput) detailVehicleProductQuantityInput.value = vehicle.productQuantity || '';
        if (detailVehicleDeclarationNumberInput) detailVehicleDeclarationNumberInput.value = vehicle.declarationNumber || '';
        if (detailVehicleDriverFullNameInput) detailVehicleDriverFullNameInput.value = vehicle.driverFullName || '';
        if (detailVehicleIsOurVehicleInput) detailVehicleIsOurVehicleInput.checked = vehicle.isOurVehicle || false;
        if (detailVehicleEur1Input) detailVehicleEur1Input.checked = vehicle.eur1 || false;
        if (detailVehicleFitoInput) detailVehicleFitoInput.checked = vehicle.fito || false;
        if (detailVehicleCustomsDateInput) detailVehicleCustomsDateInput.value = vehicle.customsDate || '';
        if (detailVehicleCustomsClearanceDateInput) detailVehicleCustomsClearanceDateInput.value = vehicle.customsClearanceDate || '';
        if (detailVehicleUnloadingDateInput) detailVehicleUnloadingDateInput.value = vehicle.unloadingDate || '';
        if (detailVehicleCarrierSelect) detailVehicleCarrierSelect.value = vehicle.carrier?.id || '';
        if (detailVehicleInvoiceUaDateInput) detailVehicleInvoiceUaDateInput.value = vehicle.invoiceUaDate || '';
        if (detailVehicleInvoiceUaPricePerTonInput) detailVehicleInvoiceUaPricePerTonInput.value = vehicle.invoiceUaPricePerTon || '';
        if (detailVehicleInvoiceUaTotalPriceInput) detailVehicleInvoiceUaTotalPriceInput.value = vehicle.invoiceUaTotalPrice || '';
        if (detailVehicleInvoiceEuDateInput) detailVehicleInvoiceEuDateInput.value = vehicle.invoiceEuDate || '';
        if (detailVehicleInvoiceEuPricePerTonInput) detailVehicleInvoiceEuPricePerTonInput.value = vehicle.invoiceEuPricePerTon || '';
        if (detailVehicleInvoiceEuTotalPriceInput) detailVehicleInvoiceEuTotalPriceInput.value = vehicle.invoiceEuTotalPrice || '';
        if (detailVehicleReclamationInput) detailVehicleReclamationInput.value = vehicle.reclamation || '';
        
        const fullReclamation = DeclarantCalculations.calculateFullReclamation(vehicle);
        if (detailVehicleFullReclamationInput) {
            detailVehicleFullReclamationInput.value = fullReclamation > 0 ? fullReclamation.toFixed(6) : '';
        }
    }
    
    function setVehicleFormEditable(isEditable) {
        const fields = [
            document.getElementById('detail-vehicle-date'),
            document.getElementById('detail-vehicle-vehicle-number'),
            document.getElementById('detail-vehicle-invoice-ua'),
            document.getElementById('detail-vehicle-invoice-eu'),
            document.getElementById('detail-vehicle-invoice-ua-date'),
            document.getElementById('detail-vehicle-invoice-ua-price-per-ton'),
            document.getElementById('detail-vehicle-invoice-eu-date'),
            document.getElementById('detail-vehicle-invoice-eu-price-per-ton'),
            document.getElementById('detail-vehicle-reclamation'),
            document.getElementById('detail-vehicle-description'),
            document.getElementById('detail-vehicle-sender'),
            document.getElementById('detail-vehicle-receiver'),
            document.getElementById('detail-vehicle-terminal'),
            document.getElementById('detail-vehicle-destination-country'),
            document.getElementById('detail-vehicle-destination-place'),
            document.getElementById('detail-vehicle-product'),
            document.getElementById('detail-vehicle-product-quantity'),
            document.getElementById('detail-vehicle-declaration-number'),
            document.getElementById('detail-vehicle-driver-full-name'),
            document.getElementById('detail-vehicle-is-our-vehicle'),
            document.getElementById('detail-vehicle-eur1'),
            document.getElementById('detail-vehicle-fito'),
            document.getElementById('detail-vehicle-customs-date'),
            document.getElementById('detail-vehicle-customs-clearance-date'),
            document.getElementById('detail-vehicle-unloading-date'),
            document.getElementById('detail-vehicle-carrier-id')
        ];

        fields.forEach(field => {
            if (field) {
                field.disabled = !isEditable;
            }
        });

        const saveVehicleBtn = document.getElementById('save-vehicle-btn');
        const editVehicleBtn = document.getElementById('edit-vehicle-btn');
        
        if (saveVehicleBtn) {
            saveVehicleBtn.style.display = isEditable ? 'inline-flex' : 'none';
        }
        if (editVehicleBtn) {
            editVehicleBtn.style.display = isEditable ? 'none' : 'inline-flex';
        }
    }
    
    function resetVehicleFormState(currentVehicleDetails) {
        populateVehicleForm(currentVehicleDetails);
        setVehicleFormEditable(false);
    }
    
    return {
        openModal,
        closeModal,
        setupModalClickHandlers,
        initializeModalClickHandlers,
        populateVehicleForm,
        setVehicleFormEditable,
        resetVehicleFormState
    };
})();
