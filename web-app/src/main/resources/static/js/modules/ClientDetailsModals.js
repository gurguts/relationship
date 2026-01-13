const ClientDetailsModals = (function() {
    let isLoadingEntities = false;
    
    async function loadEntitiesForModals() {
        if (isLoadingEntities) {
            return;
        }
        
        const currentUserMap = window.userMap;
        const currentSourceMap = window.sourceMap;
        const currentProductMap = window.productMap;
        
        if (currentUserMap && currentUserMap.size > 0 && 
            currentSourceMap && currentSourceMap.size > 0 && 
            currentProductMap && currentProductMap.size > 0) {
            return;
        }
        
        isLoadingEntities = true;
        try {
            const response = await fetch('/api/v1/entities');
            if (!response.ok) {
                console.warn('Failed to load entities, continuing without them');
                return;
            }
            
            const data = await response.json();
            
            if (typeof window.userMap === 'undefined') {
                window.userMap = new Map();
            }
            if (typeof window.sourceMap === 'undefined') {
                window.sourceMap = new Map();
            }
            if (typeof window.productMap === 'undefined') {
                window.productMap = new Map();
            }
            
            if (data.users && Array.isArray(data.users)) {
                window.userMap = new Map(data.users.map(user => [user.id, user.name || user.fullName || '']));
            }
            
            if (data.sources && Array.isArray(data.sources) && (!window.sourceMap || window.sourceMap.size === 0)) {
                window.sourceMap = new Map(data.sources.map(source => [source.id, source.name || '']));
            }
            
            if (data.products && Array.isArray(data.products)) {
                window.productMap = new Map(data.products.map(product => [product.id, product.name || '']));
            }
        } catch (error) {
            console.error('Error loading entities:', error);
        } finally {
            isLoadingEntities = false;
        }
    }

    function closeModal(modal, delay = 300) {
        if (!modal) return;
        modal.classList.remove('show');
        setTimeout(() => {
            modal.style.display = 'none';
        }, delay);
    }

    function setupModalCloseHandlers(modal, closeButtonId, modalContentSelector) {
        const closeButton = document.getElementById(closeButtonId);
        if (closeButton) {
            closeButton.addEventListener('click', () => {
                closeModal(modal);
            });
        }

        if (modal) {
            modal.addEventListener('click', (event) => {
                const modalContent = document.querySelector(modalContentSelector);
                if (modalContent && !modalContent.contains(event.target)) {
                    closeModal(modal);
                }
            });
        }
    }

    async function showPurchaseModal(clientId) {
        if (!clientId) {
            console.error('Client ID not found');
            return;
        }

        const modal = document.getElementById('purchaseClientModal');
        const tableBody = document.querySelector('#purchaseTable tbody');
        const clientIdElement = document.getElementById('client-id-purchase');
        
        if (!modal || !tableBody || !clientIdElement) return;

        modal.style.display = 'flex';
        tableBody.textContent = '';
        const loadingRow = document.createElement('tr');
        const loadingCell = document.createElement('td');
        loadingCell.colSpan = 9;
        loadingCell.style.textAlign = 'center';
        loadingCell.style.padding = '2em';
        loadingCell.textContent = CLIENT_MESSAGES.LOADING;
        loadingRow.appendChild(loadingCell);
        tableBody.appendChild(loadingRow);
        clientIdElement.textContent = `Клієнт: ${clientId}`;

        setTimeout(() => {
            modal.classList.add('show');
        }, 10);

        const currentUserMap = window.userMap;
        const currentSourceMap = window.sourceMap;
        const currentProductMap = window.productMap;
        
        if (!currentUserMap || currentUserMap.size === 0 || 
            !currentSourceMap || currentSourceMap.size === 0 || 
            !currentProductMap || currentProductMap.size === 0) {
            await loadEntitiesForModals();
        }

        try {
            const response = await fetch(`/api/v1/purchase/client/${clientId}`);
            
            if (!response.ok) {
                throw new Error(`Failed to load purchases: ${response.status} ${response.statusText}`);
            }
            
            const data = await response.json();
            tableBody.textContent = '';

            if (!data || data.length === 0) {
                tableBody.textContent = '';
                const emptyRow = document.createElement('tr');
                const emptyCell = document.createElement('td');
                emptyCell.colSpan = 9;
                emptyCell.style.textAlign = 'center';
                emptyCell.style.padding = '2em';
                emptyCell.style.color = '#999';
                emptyCell.textContent = CLIENT_MESSAGES.NO_PURCHASES;
                emptyRow.appendChild(emptyCell);
                tableBody.appendChild(emptyRow);
                return;
            }

            const userMapToUse = window.userMap || (typeof userMap !== 'undefined' ? userMap : new Map());
            const sourceMapToUse = window.sourceMap || (typeof sourceMap !== 'undefined' ? sourceMap : new Map());
            const productMapToUse = window.productMap || (typeof productMap !== 'undefined' ? productMap : new Map());

            data.forEach(purchase => {
                const row = document.createElement('tr');
                
                const driverCell = document.createElement('td');
                driverCell.setAttribute('data-label', 'Водій');
                driverCell.textContent = ClientUtils.findNameByIdFromMap(userMapToUse, purchase.userId) || '-';
                row.appendChild(driverCell);
                
                const sourceCell = document.createElement('td');
                sourceCell.setAttribute('data-label', 'Залучення');
                sourceCell.textContent = ClientUtils.findNameByIdFromMap(sourceMapToUse, purchase.sourceId) || '-';
                row.appendChild(sourceCell);
                
                const productCell = document.createElement('td');
                productCell.setAttribute('data-label', 'Товар');
                productCell.textContent = ClientUtils.findNameByIdFromMap(productMapToUse, purchase.productId) || '-';
                row.appendChild(productCell);
                
                const quantityCell = document.createElement('td');
                quantityCell.setAttribute('data-label', 'Кількість');
                quantityCell.textContent = purchase.quantity || '-';
                row.appendChild(quantityCell);
                
                const priceCell = document.createElement('td');
                priceCell.setAttribute('data-label', 'Ціна');
                priceCell.textContent = purchase.unitPrice || '-';
                row.appendChild(priceCell);
                
                const totalPriceCell = document.createElement('td');
                totalPriceCell.setAttribute('data-label', 'Сплачено');
                totalPriceCell.textContent = purchase.totalPrice || '-';
                row.appendChild(totalPriceCell);
                
                const paymentCell = document.createElement('td');
                paymentCell.setAttribute('data-label', 'Форма');
                paymentCell.textContent = purchase.paymentMethod || '-';
                row.appendChild(paymentCell);
                
                const currencyCell = document.createElement('td');
                currencyCell.setAttribute('data-label', 'Валюта');
                currencyCell.textContent = purchase.currency || '-';
                row.appendChild(currencyCell);
                
                const dateCell = document.createElement('td');
                dateCell.setAttribute('data-label', 'Дата');
                dateCell.textContent = purchase.createdAt || '-';
                row.appendChild(dateCell);
                
                tableBody.appendChild(row);
            });
        } catch (error) {
            console.error('Error loading purchases:', error);
            tableBody.textContent = '';
            const errorRow = document.createElement('tr');
            const errorCell = document.createElement('td');
            errorCell.colSpan = 9;
            errorCell.style.textAlign = 'center';
            errorCell.style.padding = '2em';
            errorCell.style.color = '#d32f2f';
            errorCell.textContent = `${CLIENT_MESSAGES.LOAD_ERROR} закупівель: ${error.message || 'Невідома помилка'}`;
            errorRow.appendChild(errorCell);
            tableBody.appendChild(errorRow);
        }
    }

    async function showContainerModal(clientId) {
        if (!clientId) {
            console.error('Client ID not found');
            return;
        }

        const modal = document.getElementById('containerClientModal');
        const tableBody = document.querySelector('#containerTable tbody');
        const clientIdElement = document.getElementById('client-id-container');
        
        if (!modal || !tableBody || !clientIdElement) return;

        modal.style.display = 'flex';
        tableBody.textContent = '';
        const loadingRow = document.createElement('tr');
        const loadingCell = document.createElement('td');
        loadingCell.colSpan = 4;
        loadingCell.style.textAlign = 'center';
        loadingCell.style.padding = '2em';
        loadingCell.textContent = CLIENT_MESSAGES.LOADING;
        loadingRow.appendChild(loadingCell);
        tableBody.appendChild(loadingRow);
        clientIdElement.textContent = `Клієнт: ${clientId}`;

        setTimeout(() => {
            modal.classList.add('show');
        }, 10);

        const currentUserMap = window.userMap;
        
        if (!currentUserMap || currentUserMap.size === 0) {
            await loadEntitiesForModals();
        }

        try {
            const response = await fetch(`/api/v1/containers/client/${clientId}`);
            
            if (!response.ok) {
                throw new Error(`Failed to load containers: ${response.status} ${response.statusText}`);
            }
            
            const data = await response.json();
            tableBody.textContent = '';

            if (!data || data.length === 0) {
                tableBody.textContent = '';
                const emptyRow = document.createElement('tr');
                const emptyCell = document.createElement('td');
                emptyCell.colSpan = 4;
                emptyCell.style.textAlign = 'center';
                emptyCell.style.padding = '2em';
                emptyCell.style.color = '#999';
                emptyCell.textContent = CLIENT_MESSAGES.NO_CONTAINERS;
                emptyRow.appendChild(emptyCell);
                tableBody.appendChild(emptyRow);
                return;
            }

            const userMapToUse = window.userMap || (typeof userMap !== 'undefined' ? userMap : new Map());

            data.forEach(container => {
                const row = document.createElement('tr');
                
                const quantityCell = document.createElement('td');
                quantityCell.setAttribute('data-label', 'Кількість');
                quantityCell.textContent = container.quantity || '-';
                row.appendChild(quantityCell);
                
                const containerNameCell = document.createElement('td');
                containerNameCell.setAttribute('data-label', 'Тип тари');
                containerNameCell.textContent = container.containerName || '-';
                row.appendChild(containerNameCell);
                
                const ownerCell = document.createElement('td');
                ownerCell.setAttribute('data-label', 'Власник');
                ownerCell.textContent = ClientUtils.findNameByIdFromMap(userMapToUse, container.userId) || '-';
                row.appendChild(ownerCell);
                
                const updatedAtCell = document.createElement('td');
                updatedAtCell.setAttribute('data-label', 'Оновлено');
                updatedAtCell.textContent = container.updatedAt || '-';
                row.appendChild(updatedAtCell);
                
                tableBody.appendChild(row);
            });
        } catch (error) {
            console.error('Error loading containers:', error);
            tableBody.textContent = '';
            const errorRow = document.createElement('tr');
            const errorCell = document.createElement('td');
            errorCell.colSpan = 4;
            errorCell.style.textAlign = 'center';
            errorCell.style.padding = '2em';
            errorCell.style.color = '#d32f2f';
            errorCell.textContent = `${CLIENT_MESSAGES.LOAD_ERROR} тар: ${error.message || 'Невідома помилка'}`;
            errorRow.appendChild(errorCell);
            tableBody.appendChild(errorRow);
        }
    }

    function init() {
        const showPurchasesButton = document.getElementById('show-purchases-client');
        if (showPurchasesButton) {
            showPurchasesButton.addEventListener('click', async () => {
                const clientModal = document.getElementById('client-modal');
                const clientId = clientModal ? clientModal.getAttribute('data-client-id') : null;
                await showPurchaseModal(clientId);
            });
        }

        const showContainersButton = document.getElementById('show-containers-client');
        if (showContainersButton) {
            showContainersButton.addEventListener('click', async () => {
                const clientModal = document.getElementById('client-modal');
                const clientId = clientModal ? clientModal.getAttribute('data-client-id') : null;
                await showContainerModal(clientId);
            });
        }

        const purchaseModal = document.getElementById('purchaseClientModal');
        if (purchaseModal) {
            setupModalCloseHandlers(purchaseModal, 'closePurchaseModal', '.modal-purchase-client-content');
        }

        const containerModal = document.getElementById('containerClientModal');
        if (containerModal) {
            setupModalCloseHandlers(containerModal, 'closeContainerModal', '.modal-container-client-content');
        }
    }

    return {
        showPurchaseModal,
        showContainerModal,
        loadEntitiesForModals,
        init
    };
})();
