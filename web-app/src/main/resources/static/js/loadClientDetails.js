/*--modal-client-button--*/

/*--modal-purchase-client--*/

document.getElementById('show-purchases-client').addEventListener('click', () => {
    const clientId = document.getElementById('client-modal').getAttribute('data-client-id');

    const modal = document.getElementById('purchaseClientModal');
    modal.style.display = 'flex';

    setTimeout(() => {
        modal.classList.add('show');
    }, 10);

    fetch(`/api/v1/purchase/client/${clientId}`)
        .then(response => response.json())
        .then(data => {
            const tableBody = document.querySelector('#purchaseTable tbody');
            tableBody.innerHTML = '';

            const clientIdElement = document.getElementById('client-id-purchase');

            clientIdElement.innerText = `Клієнт: ${clientId}`;

            data.forEach(purchase => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${findNameByIdFromMap(userMap, purchase.userId)}</td>
                    <td>${findNameByIdFromMap(sourceMap, purchase.sourceId)}</td>
                    <td>${findNameByIdFromMap(productMap, purchase.productId)}</td>
                    <td>${purchase.quantity || ''}</td>
                    <td>${purchase.unitPrice || ''}</td>
                    <td>${purchase.totalPrice || ''}</td>
                    <td>${purchase.paymentMethod || ''}</td>
                    <td>${purchase.currency || ''}</td>
                    <td>${purchase.createdAt || ''}</td>
                `;

                tableBody.appendChild(row);
            });
        });
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


/*--modal-sale-client--*/

document.getElementById('show-sale-client').addEventListener('click', () => {
    const clientId = document.getElementById('client-modal').getAttribute('data-client-id');

    const modal = document.getElementById('saleClientModal');
    modal.style.display = 'flex';

    setTimeout(() => {
        modal.classList.add('show');
    }, 10);

    fetch(`/api/v1/sale/client/${clientId}`)
        .then(response => response.json())
        .then(data => {
            const tableBody = document.querySelector('#saleTable tbody');
            tableBody.innerHTML = '';

            const clientIdElement = document.getElementById('client-id-sale');

            clientIdElement.innerText = `Клієнт: ${clientId}`;

            data.forEach(sale => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${findNameByIdFromMap(userMap, sale.userId)}</td>
                    <td>${findNameByIdFromMap(sourceMap, sale.sourceId)}</td>
                    <td>${findNameByIdFromMap(productMap, sale.productId)}</td>
                    <td>${sale.quantity || ''}</td>
                    <td>${sale.unitPrice || ''}</td>
                    <td>${sale.totalPrice || ''}</td>
                    <td>${sale.paymentMethod || ''}</td>
                    <td>${sale.currency || ''}</td>
                    <td>${sale.createdAt || ''}</td>
                `;

                tableBody.appendChild(row);
            });
        });
});

document.getElementById('closeSaleModal').addEventListener('click', () => {
    const modal = document.getElementById('saleClientModal');
    modal.classList.remove('show');

    setTimeout(() => {
        modal.style.display = 'none';
    }, 300);
});

document.getElementById('saleClientModal').addEventListener('click', (event) => {
    const modalContent = document.querySelector('.modal-sale-client-content');
    if (!modalContent.contains(event.target)) {
        const modal = document.getElementById('saleClientModal');
        modal.classList.remove('show');

        setTimeout(() => {
            modal.style.display = 'none';
        }, 300);
    }
});

/*--modal-container-client--*/

document.getElementById('show-containers-client').addEventListener('click', () => {
    const clientId = document.getElementById('client-modal').getAttribute('data-client-id');

    const modal = document.getElementById('containerClientModal');
    modal.style.display = 'flex';

    setTimeout(() => {
        modal.classList.add('show');
    }, 10);

    fetch(`/api/v1/containers/client/${clientId}`)
        .then(response => response.json())
        .then(data => {
            const tableBody = document.querySelector('#containerTable tbody');
            tableBody.innerHTML = '';

            const clientIdElement = document.getElementById('client-id-container');

            clientIdElement.innerText = `Клієнт: ${clientId}`;

            data.forEach(container => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${container.quantity || ''}</td>
                    <td>${container.containerName || ''}</td>
                    <td>${findNameByIdFromMap(userMap, container.userId)}</td>
                    <td>${container.updatedAt || ''}</td>
                `;

                tableBody.appendChild(row);
            });
        });
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