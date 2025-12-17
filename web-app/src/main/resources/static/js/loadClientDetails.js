/*--modal-client-button--*/

/* Load entities data - uses existing variables from clients.js if available */
async function loadEntitiesForModals() {
    try {
        const response = await fetch('/api/v1/entities');
        if (!response.ok) {
            console.warn('Failed to load entities, continuing without them');
            return;
        }
        
        const data = await response.json();
        
        // Use existing variables from clients.js or create new ones if they don't exist
        if (typeof window.userMap === 'undefined') {
            window.userMap = new Map();
        }
        if (typeof window.sourceMap === 'undefined') {
            window.sourceMap = new Map();
        }
        if (typeof window.productMap === 'undefined') {
            window.productMap = new Map();
        }
        
        // Load users
        if (data.users && Array.isArray(data.users)) {
            window.userMap = new Map(data.users.map(user => [user.id, user.name || user.fullName || '']));
        }
        
        // Load sources (only if not already loaded from clients.js)
        if (data.sources && Array.isArray(data.sources) && (!window.sourceMap || window.sourceMap.size === 0)) {
            window.sourceMap = new Map(data.sources.map(source => [source.id, source.name || '']));
        }
        
        // Load products
        if (data.products && Array.isArray(data.products)) {
            window.productMap = new Map(data.products.map(product => [product.id, product.name || '']));
        }
    } catch (error) {
        console.error('Error loading entities:', error);
    }
}

// Load entities when page loads
loadEntitiesForModals();

/*--modal-purchase-client--*/

document.getElementById('show-purchases-client').addEventListener('click', async () => {
    const clientId = document.getElementById('client-modal').getAttribute('data-client-id');
    
    if (!clientId) {
        console.error('Client ID not found');
        return;
    }

    const modal = document.getElementById('purchaseClientModal');
    const tableBody = document.querySelector('#purchaseTable tbody');
    const clientIdElement = document.getElementById('client-id-purchase');
    
    modal.style.display = 'flex';
    tableBody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 2em;">Завантаження...</td></tr>';
    clientIdElement.textContent = `Клієнт: ${clientId}`;

    setTimeout(() => {
        modal.classList.add('show');
    }, 10);

    // Ensure entities are loaded
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
            tableBody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 2em; color: #999;">Немає закупівель</td></tr>';
            return;
        }

        // Get the current maps (may have been updated)
        const userMapToUse = window.userMap || (typeof userMap !== 'undefined' ? userMap : new Map());
        const sourceMapToUse = window.sourceMap || (typeof sourceMap !== 'undefined' ? sourceMap : new Map());
        const productMapToUse = window.productMap || (typeof productMap !== 'undefined' ? productMap : new Map());

        data.forEach(purchase => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td data-label="Водій">${findNameByIdFromMap(userMapToUse, purchase.userId) || '-'}</td>
                <td data-label="Залучення">${findNameByIdFromMap(sourceMapToUse, purchase.sourceId) || '-'}</td>
                <td data-label="Товар">${findNameByIdFromMap(productMapToUse, purchase.productId) || '-'}</td>
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
        tableBody.innerHTML = `<tr><td colspan="9" style="text-align: center; padding: 2em; color: #d32f2f;">Помилка завантаження закупівель: ${error.message}</td></tr>`;
    }
});

document.getElementById('closePurchaseModal').addEventListener('click', () => {
    const modal = document.getElementById('purchaseClientModal');
    modal.classList.remove('show');

    setTimeout(() => {
        modal.style.display = 'none';
    }, 300);
});

document.getElementById('purchaseClientModal').addEventListener('click',
    (event) => {
        const modalContent = document.querySelector('.modal-purchase-client-content');
        if (!modalContent.contains(event.target)) {
            const modal = document.getElementById('purchaseClientModal');
            modal.classList.remove('show');

            setTimeout(() => {
                modal.style.display = 'none';
            }, 300);
        }
    });


/*--modal-container-client--*/

document.getElementById('show-containers-client').addEventListener('click', async () => {
    const clientId = document.getElementById('client-modal').getAttribute('data-client-id');
    
    if (!clientId) {
        console.error('Client ID not found');
        return;
    }

    const modal = document.getElementById('containerClientModal');
    const tableBody = document.querySelector('#containerTable tbody');
    const clientIdElement = document.getElementById('client-id-container');
    
    modal.style.display = 'flex';
    tableBody.innerHTML = '<tr><td colspan="4" style="text-align: center; padding: 2em;">Завантаження...</td></tr>';
    clientIdElement.textContent = `Клієнт: ${clientId}`;

    setTimeout(() => {
        modal.classList.add('show');
    }, 10);

    // Ensure entities are loaded
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
            tableBody.innerHTML = '<tr><td colspan="4" style="text-align: center; padding: 2em; color: #999;">Немає тар</td></tr>';
            return;
        }

        // Get the current map (may have been updated)
        const userMapToUse = window.userMap || (typeof userMap !== 'undefined' ? userMap : new Map());

        data.forEach(container => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td data-label="Кількість">${container.quantity || '-'}</td>
                <td data-label="Тип тари">${container.containerName || '-'}</td>
                <td data-label="Власник">${findNameByIdFromMap(userMapToUse, container.userId) || '-'}</td>
                <td data-label="Оновлено">${container.updatedAt || '-'}</td>
            `;
            tableBody.appendChild(row);
        });
    } catch (error) {
        console.error('Error loading containers:', error);
        tableBody.innerHTML = `<tr><td colspan="4" style="text-align: center; padding: 2em; color: #d32f2f;">Помилка завантаження тар: ${error.message}</td></tr>`;
    }
});

document.getElementById('closeContainerModal').addEventListener('click', () => {
    const modal = document.getElementById('containerClientModal');
    modal.classList.remove('show');

    setTimeout(() => {
        modal.style.display = 'none';
    }, 300);
});

document.getElementById('containerClientModal').addEventListener('click',
    (event) => {
        const modalContent = document.querySelector('.modal-container-client-content');
        if (!modalContent.contains(event.target)) {
            const modal = document.getElementById('containerClientModal');
            modal.classList.remove('show');

            setTimeout(() => {
                modal.style.display = 'none';
            }, 300);
        }
    });