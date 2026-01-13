const ClientTypeManager = (function() {
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

            const currentUserId = ClientState.getUserId();
            let accessibleClientTypeIds = new Set();
            
            if (currentUserId) {
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

            const userAuthorities = ClientState.getAuthorities();
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
                clientTypesSelectionList.appendChild(emptyMessage);
                clientTypeSelectionModal.style.display = 'flex';
            } else if (accessibleClientTypes.length === 1) {
                window.location.href = `/clients?type=${accessibleClientTypes[0].id}`;
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
                        window.location.href = `/clients?type=${type.id}`;
                    });
                    clientTypesSelectionList.appendChild(card);
                });
                clientTypeSelectionModal.style.display = 'flex';
            }

            const closeBtn = document.querySelector('.close-client-type-modal');
            if (closeBtn) {
                closeBtn.addEventListener('click', () => {
                    clientTypeSelectionModal.style.display = 'none';
                });
            }
            
            clientTypeSelectionModal.addEventListener('click', (e) => {
                if (e.target === clientTypeSelectionModal) {
                    clientTypeSelectionModal.style.display = 'none';
                }
            });
        } catch (error) {
            console.error('Error loading client types:', error);
        }
    }

    async function updateNavigationWithCurrentType(typeId, clientType = null) {
        try {
            let clientTypeData = clientType;
            if (!clientTypeData) {
                const response = await fetch(`/api/v1/client-type/${typeId}`);
                if (!response.ok) {
                    console.error('Failed to load client type:', response.status, response.statusText);
                    return;
                }
                clientTypeData = await response.json();
            }
            const navLink = document.querySelector('#nav-clients a');
            
            if (navLink && clientTypeData.name) {
                navLink.textContent = '';
                
                const labelSpan = document.createElement('span');
                labelSpan.className = 'nav-client-type-label';
                labelSpan.textContent = 'Клієнти:';
                navLink.appendChild(labelSpan);
                
                const nameSpan = document.createElement('span');
                nameSpan.className = 'nav-client-type-name';
                nameSpan.textContent = clientTypeData.name;
                navLink.appendChild(nameSpan);
                
                const arrowSpan = document.createElement('span');
                arrowSpan.className = 'dropdown-arrow';
                arrowSpan.textContent = '▼';
                navLink.appendChild(arrowSpan);
            }

            const dropdown = document.getElementById('client-types-dropdown');
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
