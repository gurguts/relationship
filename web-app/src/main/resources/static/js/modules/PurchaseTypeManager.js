const PurchaseTypeManager = (function() {
    async function showClientTypeSelectionModal(clientTypeSelectionModal, clientTypesSelectionList) {
        if (!clientTypeSelectionModal || !clientTypesSelectionList) return;
        
        try {
            const response = await fetch('/api/v1/client-type/active');
            if (!response.ok) {
                console.error('Failed to load client types:', response.status, response.statusText);
                handleError(new ErrorResponse('CLIENT_ERROR_DEFAULT', 'Не вдалося завантажити типи клієнтів', null));
                return;
            }
            const allClientTypes = await response.json();

            const userId = ClientState.getUserId();
            let accessibleClientTypeIds = new Set();
            
            if (userId) {
                try {
                    const permissionsResponse = await fetch(`/api/v1/client-type/permission/me`);
                    if (permissionsResponse.ok) {
                        const permissions = await permissionsResponse.json();
                        permissions.forEach(perm => {
                            if (perm.canView) {
                                accessibleClientTypeIds.add(perm.clientTypeId);
                            }
                        });
                    } else {
                        console.warn('Failed to load user client type permissions:', permissionsResponse.status, permissionsResponse.statusText);
                    }
                } catch (error) {
                    console.warn('Failed to load user client type permissions:', error);
                }
            }

            const authorities = localStorage.getItem('authorities');
            let userAuthorities = [];
            try {
                if (authorities) {
                    userAuthorities = authorities.startsWith('[')
                        ? JSON.parse(authorities)
                        : authorities.split(',').map(auth => auth.trim());
                }
            } catch (error) {
                console.error('Failed to parse authorities:', error);
            }
            
            const isAdmin = userAuthorities.includes('system:admin') || userAuthorities.includes('administration:view');

            if (isAdmin || accessibleClientTypeIds.size === 0) {
                allClientTypes.forEach(type => accessibleClientTypeIds.add(type.id));
            }

            const accessibleClientTypes = allClientTypes.filter(type => accessibleClientTypeIds.has(type.id));
            
            if (accessibleClientTypes.length === 0) {
                const emptyMessage = document.createElement('p');
                emptyMessage.style.textAlign = 'center';
                emptyMessage.style.color = 'var(--main-grey)';
                emptyMessage.style.padding = '2em';
                emptyMessage.textContent = 'Немає доступних типів клієнтів';
                clientTypesSelectionList.textContent = '';
                clientTypesSelectionList.appendChild(emptyMessage);
                clientTypeSelectionModal.style.display = 'flex';
            } else if (accessibleClientTypes.length === 1) {
                window.location.href = `/purchase?type=${accessibleClientTypes[0].id}`;
                return;
            } else {
                clientTypesSelectionList.textContent = '';
                accessibleClientTypes.forEach(type => {
                    const card = document.createElement('div');
                    card.className = 'client-type-card';
                    
                    const iconDiv = document.createElement('div');
                    iconDiv.className = 'client-type-card-icon';
                    iconDiv.textContent = '👥';
                    card.appendChild(iconDiv);
                    
                    const nameDiv = document.createElement('div');
                    nameDiv.className = 'client-type-card-name';
                    nameDiv.textContent = type.name;
                    card.appendChild(nameDiv);
                    
                    card.addEventListener('click', () => {
                        window.location.href = `/purchase?type=${type.id}`;
                    });
                    clientTypesSelectionList.appendChild(card);
                });
                clientTypeSelectionModal.style.display = 'flex';
            }

            const closeBtn = document.querySelector('.close-client-type-modal');
            if (closeBtn) {
                if (closeBtn._closeTypeModalHandler) {
                    closeBtn.removeEventListener('click', closeBtn._closeTypeModalHandler);
                }
                const closeHandler = () => {
                    clientTypeSelectionModal.style.display = 'none';
                };
                closeBtn._closeTypeModalHandler = closeHandler;
                closeBtn.addEventListener('click', closeHandler);
            }
            
            if (clientTypeSelectionModal._typeModalClickHandler) {
                clientTypeSelectionModal.removeEventListener('click', clientTypeSelectionModal._typeModalClickHandler);
            }
            const modalClickHandler = (e) => {
                if (e.target === clientTypeSelectionModal) {
                    clientTypeSelectionModal.style.display = 'none';
                }
            };
            clientTypeSelectionModal._typeModalClickHandler = modalClickHandler;
            clientTypeSelectionModal.addEventListener('click', modalClickHandler);
        } catch (error) {
            console.error('Error loading client types:', error);
        }
    }

    async function updateNavigationWithCurrentType(typeId) {
        try {
            const response = await fetch(`/api/v1/client-type/${typeId}`);
            if (!response.ok) {
                console.error('Failed to load client type:', response.status, response.statusText);
                return;
            }
            
            const clientType = await response.json();
            const navLink = document.querySelector('#nav-purchase a');
            
            if (navLink && clientType.name) {
                navLink.textContent = '';
                
                const labelSpan = document.createElement('span');
                labelSpan.className = 'nav-client-type-label';
                labelSpan.textContent = 'Закупівлі:';
                navLink.appendChild(labelSpan);
                
                const nameSpan = document.createElement('span');
                nameSpan.className = 'nav-client-type-name';
                nameSpan.textContent = clientType.name;
                navLink.appendChild(nameSpan);
                
                const arrowSpan = document.createElement('span');
                arrowSpan.className = 'dropdown-arrow';
                arrowSpan.textContent = '▼';
                navLink.appendChild(arrowSpan);
            }

            const dropdown = document.getElementById('purchase-types-dropdown');
            if (dropdown) {
                const links = dropdown.querySelectorAll('a');
                links.forEach(link => {
                    link.classList.remove('active');
                    if (link.href.includes(`type=${typeId}`)) {
                        link.classList.add('active');
                    }
                });
            }
        } catch (error) {
            console.error('Error updating navigation:', error);
        }
    }

    return {
        showClientTypeSelectionModal,
        updateNavigationWithCurrentType
    };
})();
