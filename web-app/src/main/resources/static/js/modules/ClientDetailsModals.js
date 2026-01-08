const ClientDetailsModals = (function() {
    async function loadEntitiesForModals() {
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
        tableBody.innerHTML = `<tr><td colspan="9" style="text-align: center; padding: 2em;">${CLIENT_MESSAGES.LOADING}</td></tr>`;
        clientIdElement.textContent = `Клієнт: ${clientId}`;

        setTimeout(() => {
            modal.classList.add('show');
        }, 10);

        const currentUserMap = window.userMap || (typeof userMap !== 'undefined' ? userMap : new Map());
        const currentSourceMap = window.sourceMap || (typeof sourceMap !== 'undefined' ? sourceMap : new Map());
        const currentProductMap = window.productMap || (typeof productMap !== 'undefined' ? productMap : new Map());
        
        if (currentUserMap.size === 0 || currentSourceMap.size === 0 || currentProductMap.size === 0) {
            await loadEntitiesForModals();
        }

        try {
            const response = await fetch(`/api/v1/purchase/client/${clientId}`);
            
            if (!response.ok) {
                throw new Error(`Failed to load purchases: ${response.status} ${response.statusText}`);
            }
            
            const data = await response.json();
            tableBody.innerHTML = '';

            if (!data || data.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="9" style="text-align: center; padding: 2em; color: #999;">${CLIENT_MESSAGES.NO_PURCHASES}</td></tr>`;
                return;
            }

            const userMapToUse = window.userMap || (typeof userMap !== 'undefined' ? userMap : new Map());
            const sourceMapToUse = window.sourceMap || (typeof sourceMap !== 'undefined' ? sourceMap : new Map());
            const productMapToUse = window.productMap || (typeof productMap !== 'undefined' ? productMap : new Map());

            data.forEach(purchase => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td data-label="Водій">${ClientUtils.findNameByIdFromMap(userMapToUse, purchase.userId) || '-'}</td>
                    <td data-label="Залучення">${ClientUtils.findNameByIdFromMap(sourceMapToUse, purchase.sourceId) || '-'}</td>
                    <td data-label="Товар">${ClientUtils.findNameByIdFromMap(productMapToUse, purchase.productId) || '-'}</td>
                    <td data-label="Кількість">${purchase.quantity || '-'}</td>
                    <td data-label="Ціна">${purchase.unitPrice || '-'}</td>
                    <td data-label="Сплачено">${purchase.totalPrice || '-'}</td>
                    <td data-label="Форма">${purchase.paymentMethod || '-'}</td>
                    <td data-label="Валюта">${purchase.currency || '-'}</td>
                    <td data-label="Дата">${purchase.createdAt || '-'}</td>
                `;
                tableBody.appendChild(row);
            });
        } catch (error) {
            console.error('Error loading purchases:', error);
            tableBody.innerHTML = `<tr><td colspan="9" style="text-align: center; padding: 2em; color: #d32f2f;">${CLIENT_MESSAGES.LOAD_ERROR} закупівель: ${error.message}</td></tr>`;
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
        tableBody.innerHTML = `<tr><td colspan="4" style="text-align: center; padding: 2em;">${CLIENT_MESSAGES.LOADING}</td></tr>`;
        clientIdElement.textContent = `Клієнт: ${clientId}`;

        setTimeout(() => {
            modal.classList.add('show');
        }, 10);

        const currentUserMap = window.userMap || (typeof userMap !== 'undefined' ? userMap : new Map());
        
        if (currentUserMap.size === 0) {
            await loadEntitiesForModals();
        }

        try {
            const response = await fetch(`/api/v1/containers/client/${clientId}`);
            
            if (!response.ok) {
                throw new Error(`Failed to load containers: ${response.status} ${response.statusText}`);
            }
            
            const data = await response.json();
            tableBody.innerHTML = '';

            if (!data || data.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="4" style="text-align: center; padding: 2em; color: #999;">${CLIENT_MESSAGES.NO_CONTAINERS}</td></tr>`;
                return;
            }

            const userMapToUse = window.userMap || (typeof userMap !== 'undefined' ? userMap : new Map());

            data.forEach(container => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td data-label="Кількість">${container.quantity || '-'}</td>
                    <td data-label="Тип тари">${container.containerName || '-'}</td>
                    <td data-label="Власник">${ClientUtils.findNameByIdFromMap(userMapToUse, container.userId) || '-'}</td>
                    <td data-label="Оновлено">${container.updatedAt || '-'}</td>
                `;
                tableBody.appendChild(row);
            });
        } catch (error) {
            console.error('Error loading containers:', error);
            tableBody.innerHTML = `<tr><td colspan="4" style="text-align: center; padding: 2em; color: #d32f2f;">${CLIENT_MESSAGES.LOAD_ERROR} тар: ${error.message}</td></tr>`;
        }
    }

    function init() {
        loadEntitiesForModals();

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
