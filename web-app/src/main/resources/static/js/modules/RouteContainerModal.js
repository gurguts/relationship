const RouteContainerModal = (function() {
    async function loadUsers() {
        try {
            const response = await fetch('/api/v1/user');
            if (!response.ok) throw new Error('Failed to load users');
            return await response.json();
        } catch (error) {
            console.error('Error loading users:', error);
            return [];
        }
    }

    async function loadContainers() {
        try {
            const response = await fetch('/api/v1/container');
            if (!response.ok) throw new Error('Failed to load containers');
            return await response.json();
        } catch (error) {
            console.error('Error loading containers:', error);
            return [];
        }
    }

    async function openCreateContainerModal(clientId, config) {
        const {
            modal,
            form,
            clientIdInput,
            userIdSelect,
            containerIdSelect,
            loaderBackdrop
        } = config;

        if (!modal || !form || !clientIdInput || !userIdSelect || !containerIdSelect) {
            return;
        }
        
        form.reset();
        clientIdInput.value = clientId;
        
        const [users, containers] = await Promise.all([loadUsers(), loadContainers()]);
        
        userIdSelect.innerHTML = '<option value="">Виберіть водія</option>';
        users.forEach(user => {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = user.name;
            userIdSelect.appendChild(option);
        });
        
        containerIdSelect.innerHTML = '<option value="">Виберіть тип тари</option>';
        containers.forEach(container => {
            const option = document.createElement('option');
            option.value = container.id;
            option.textContent = container.name;
            containerIdSelect.appendChild(option);
        });
        
        modal.style.display = 'flex';
        modal.classList.add('show');
    }

    function setupCloseHandler(closeButton, modal) {
        if (closeButton) {
            closeButton.addEventListener('click', () => {
                modal.style.display = 'none';
                modal.classList.remove('show');
            });
        }
    }

    function setupSubmitHandler(form, config) {
        const {
            loaderBackdrop,
            modal
        } = config;

        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                
                const operationType = document.getElementById('containerOperationType').value;
                const clientId = Number(document.getElementById('containerClientId').value);
                const userId = Number(document.getElementById('containerUserId').value);
                const containerId = Number(document.getElementById('containerContainerId').value);
                const quantity = parseFloat(document.getElementById('containerQuantity').value);
                
                if (!operationType || !clientId || !userId || !containerId || !quantity) {
                    showMessage('Будь ласка, заповніть всі поля', 'error');
                    return;
                }
                
                const formData = {
                    userId: userId,
                    clientId: clientId,
                    containerId: containerId,
                    quantity: quantity
                };
                
                const endpoint = operationType === 'transfer' 
                    ? '/api/v1/containers/client/transfer'
                    : '/api/v1/containers/client/collect';
                
                try {
                    if (loaderBackdrop) {
                        loaderBackdrop.style.display = 'flex';
                    }
                    const response = await fetch(endpoint, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(formData)
                    });
                    
                    if (!response.ok) {
                        const error = await response.json();
                        throw new Error(error.message || 'Помилка виконання транзакції тари');
                    }
                    
                    modal.style.display = 'none';
                    modal.classList.remove('show');
                    form.reset();
                    const operationText = operationType === 'transfer' 
                        ? 'Тара успішно залишена у клієнта'
                        : 'Тара успішно забрана у клієнта';
                    showMessage(operationText, 'info');
                } catch (error) {
                    console.error('Error executing container transaction:', error);
                    showMessage('Помилка виконання транзакції тари: ' + error.message, 'error');
                } finally {
                    if (loaderBackdrop) {
                        loaderBackdrop.style.display = 'none';
                    }
                }
            });
        }
    }

    function init(config) {
        const {
            closeButton,
            form,
            modal
        } = config;

        setupCloseHandler(closeButton, modal);
        setupSubmitHandler(form, config);
    }

    return {
        openCreateContainerModal,
        loadContainers,
        init
    };
})();
