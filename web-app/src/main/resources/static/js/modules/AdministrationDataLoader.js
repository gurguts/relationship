const AdministrationDataLoader = (function() {
    const API_BASE = '/api/v1';

    async function loadUsers() {
        try {
            const response = await fetch(`${API_BASE}/user/page`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading users:', error);
            throw error;
        }
    }

    async function createUser(userData) {
        try {
            const response = await fetch(`${API_BASE}/user`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(userData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating user:', error);
            throw error;
        }
    }

    async function updateUser(userId, userData) {
        try {
            const response = await fetch(`${API_BASE}/user/${userId}`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(userData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating user:', error);
            throw error;
        }
    }

    async function deleteUser(userId) {
        try {
            const response = await fetch(`${API_BASE}/user/${userId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting user:', error);
            throw error;
        }
    }

    async function updateUserPermissions(userId, permissions) {
        try {
            const response = await fetch(`${API_BASE}/user/${userId}/permissions`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(permissions)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error updating user permissions:', error);
            throw error;
        }
    }

    async function loadAllUsers() {
        try {
            const response = await fetch(`${API_BASE}/user`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading all users:', error);
            throw error;
        }
    }

    async function loadContainers() {
        try {
            const response = await fetch(`${API_BASE}/container`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading containers:', error);
            throw error;
        }
    }

    async function createContainer(containerData) {
        try {
            const response = await fetch(`${API_BASE}/container`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(containerData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating container:', error);
            throw error;
        }
    }

    async function updateContainer(containerId, containerData) {
        try {
            const response = await fetch(`${API_BASE}/container/${containerId}`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(containerData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating container:', error);
            throw error;
        }
    }

    async function deleteContainer(containerId) {
        try {
            const response = await fetch(`${API_BASE}/container/${containerId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting container:', error);
            throw error;
        }
    }

    async function loadStorages() {
        try {
            const response = await fetch(`${API_BASE}/warehouse`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading storages:', error);
            throw error;
        }
    }

    async function createStorage(storageData) {
        try {
            const response = await fetch(`${API_BASE}/warehouse`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(storageData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating storage:', error);
            throw error;
        }
    }

    async function updateStorage(storageId, storageData) {
        try {
            const response = await fetch(`${API_BASE}/warehouse/${storageId}`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(storageData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating storage:', error);
            throw error;
        }
    }

    async function deleteStorage(storageId) {
        try {
            const response = await fetch(`${API_BASE}/warehouse/${storageId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting storage:', error);
            throw error;
        }
    }

    async function loadWithdrawalReasons() {
        try {
            const response = await fetch(`${API_BASE}/withdrawal-reason`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading withdrawal reasons:', error);
            throw error;
        }
    }

    async function createWithdrawalReason(reasonData) {
        try {
            const response = await fetch(`${API_BASE}/withdrawal-reason`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(reasonData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating withdrawal reason:', error);
            throw error;
        }
    }

    async function updateWithdrawalReason(reasonId, reasonData) {
        try {
            const response = await fetch(`${API_BASE}/withdrawal-reason/${reasonId}`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(reasonData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating withdrawal reason:', error);
            throw error;
        }
    }

    async function deleteWithdrawalReason(reasonId) {
        try {
            const response = await fetch(`${API_BASE}/withdrawal-reason/${reasonId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting withdrawal reason:', error);
            throw error;
        }
    }

    async function loadProducts() {
        try {
            const response = await fetch(`${API_BASE}/product`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading products:', error);
            throw error;
        }
    }

    async function loadProduct(productId) {
        try {
            const response = await fetch(`${API_BASE}/product/${productId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading product:', error);
            throw error;
        }
    }

    async function createProduct(productData) {
        try {
            const response = await fetch(`${API_BASE}/product`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(productData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating product:', error);
            throw error;
        }
    }

    async function updateProduct(productId, productData) {
        try {
            const response = await fetch(`${API_BASE}/product/${productId}`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(productData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating product:', error);
            throw error;
        }
    }

    async function deleteProduct(productId) {
        try {
            const response = await fetch(`${API_BASE}/product/${productId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting product:', error);
            throw error;
        }
    }

    async function loadSources() {
        try {
            const response = await fetch(`${API_BASE}/source`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading sources:', error);
            throw error;
        }
    }

    async function loadSource(sourceId) {
        try {
            const response = await fetch(`${API_BASE}/source/${sourceId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading source:', error);
            throw error;
        }
    }

    async function createSource(sourceData) {
        try {
            const response = await fetch(`${API_BASE}/source`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(sourceData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating source:', error);
            throw error;
        }
    }

    async function updateSource(sourceId, sourceData) {
        try {
            const response = await fetch(`${API_BASE}/source/${sourceId}`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(sourceData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating source:', error);
            throw error;
        }
    }

    async function deleteSource(sourceId) {
        try {
            const response = await fetch(`${API_BASE}/source/${sourceId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting source:', error);
            throw error;
        }
    }

    async function loadClientTypes() {
        try {
            const response = await fetch(`${API_BASE}/client-type?page=0&size=1000`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            const data = await response.json();
            return data.content || data;
        } catch (error) {
            console.error('Error loading client types:', error);
            throw error;
        }
    }

    async function loadActiveClientTypes() {
        try {
            const response = await fetch(`${API_BASE}/client-type/active`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading active client types:', error);
            throw error;
        }
    }

    async function loadClientType(clientTypeId) {
        try {
            const response = await fetch(`${API_BASE}/client-type/${clientTypeId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading client type:', error);
            throw error;
        }
    }

    async function createClientType(clientTypeData) {
        try {
            const response = await fetch(`${API_BASE}/client-type`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(clientTypeData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating client type:', error);
            throw error;
        }
    }

    async function updateClientType(clientTypeId, clientTypeData) {
        try {
            const response = await fetch(`${API_BASE}/client-type/${clientTypeId}`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(clientTypeData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating client type:', error);
            throw error;
        }
    }

    async function deleteClientType(clientTypeId) {
        try {
            const response = await fetch(`${API_BASE}/client-type/${clientTypeId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting client type:', error);
            throw error;
        }
    }

    async function loadClientTypeFields(clientTypeId) {
        try {
            const response = await fetch(`${API_BASE}/client-type/${clientTypeId}/field`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading client type fields:', error);
            throw error;
        }
    }

    async function loadClientTypeField(fieldId) {
        try {
            const response = await fetch(`${API_BASE}/client-type/field/${fieldId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading client type field:', error);
            throw error;
        }
    }

    async function createClientTypeField(clientTypeId, fieldData) {
        try {
            const response = await fetch(`${API_BASE}/client-type/${clientTypeId}/field`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(fieldData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating client type field:', error);
            throw error;
        }
    }

    async function updateClientTypeField(fieldId, fieldData) {
        try {
            const response = await fetch(`${API_BASE}/client-type/field/${fieldId}`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(fieldData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating client type field:', error);
            throw error;
        }
    }

    async function deleteClientTypeField(fieldId) {
        try {
            const response = await fetch(`${API_BASE}/client-type/field/${fieldId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting client type field:', error);
            throw error;
        }
    }

    async function loadClientTypeStaticFieldsConfig(clientTypeId) {
        try {
            const response = await fetch(`${API_BASE}/client-type/${clientTypeId}/static-fields`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading static fields config:', error);
            throw error;
        }
    }

    async function updateClientTypeStaticFieldsConfig(clientTypeId, configData) {
        try {
            const response = await fetch(`${API_BASE}/client-type/${clientTypeId}/static-fields`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(configData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error updating static fields config:', error);
            throw error;
        }
    }

    async function importClients(clientTypeId, file) {
        try {
            const formData = new FormData();
            formData.append('file', file);
            const response = await fetch(`${API_BASE}/client/import/${clientTypeId}`, {
                method: 'POST',
                body: formData,
                credentials: 'include'
            });
            if (!response.ok) {
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    const error = await parseErrorResponse(response);
                    throw error;
                } else {
                    const errorText = await response.text().catch(() => 'Помилка імпорту');
                    throw new Error(errorText);
                }
            }
            return await response.text();
        } catch (error) {
            console.error('Error importing clients:', error);
            throw error;
        }
    }

    async function loadClientTypePermissions(userId) {
        try {
            if (!userId || userId === 'undefined' || userId === undefined) {
                throw new Error('User ID is required');
            }
            
            const clientTypesResponse = await fetch(`${API_BASE}/client-type/active`);
            if (!clientTypesResponse.ok) {
                const error = await parseErrorResponse(clientTypesResponse);
                throw error;
            }
            const clientTypes = await clientTypesResponse.json();
            const allClientTypes = Array.isArray(clientTypes) ? clientTypes : (clientTypes.content || []);

            const permissionsResponse = await fetch(`${API_BASE}/client-type/permission/user/${userId}`);
            const permissions = permissionsResponse.ok ? await permissionsResponse.json() : [];

            const permissionsMap = new Map();
            permissions.forEach(perm => {
                permissionsMap.set(perm.clientTypeId, perm);
            });

            return { allClientTypes, permissionsMap };
        } catch (error) {
            console.error('Error loading client type permissions:', error);
            throw error;
        }
    }

    async function updateClientTypePermission(clientTypeId, userId, permissionData) {
        try {
            const response = await fetch(`${API_BASE}/client-type/${clientTypeId}/permission/${userId}`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(permissionData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error updating client type permission:', error);
            throw error;
        }
    }

    async function createClientTypePermission(clientTypeId, userId) {
        try {
            const response = await fetch(`${API_BASE}/client-type/${clientTypeId}/permission`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    userId,
                    canView: true,
                    canCreate: false,
                    canEdit: false,
                    canDelete: false
                })
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error creating client type permission:', error);
            throw error;
        }
    }

    async function deleteClientTypePermission(clientTypeId, userId) {
        try {
            const response = await fetch(`${API_BASE}/client-type/${clientTypeId}/permission/${userId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting client type permission:', error);
            throw error;
        }
    }

    async function loadBranchPermissions(userId, branchId) {
        try {
            let permissions = [];
            if (userId) {
                const response = await fetch(`${API_BASE}/branch-permissions/user/${userId}`);
                if (!response.ok) {
                    const error = await parseErrorResponse(response);
                    throw error;
                }
                permissions = await response.json();
            } else if (branchId) {
                const response = await fetch(`${API_BASE}/branch-permissions/branch/${branchId}`);
                if (!response.ok) {
                    const error = await parseErrorResponse(response);
                    throw error;
                }
                permissions = await response.json();
            } else {
                const allUsers = await loadAllUsers();
                const allPermissions = [];
                for (const user of allUsers) {
                    try {
                        const response = await fetch(`${API_BASE}/branch-permissions/user/${user.id}`);
                        if (response.ok) {
                            const userPermissions = await response.json();
                            allPermissions.push(...userPermissions);
                        }
                    } catch (error) {
                        console.error(`Error loading permissions for user ${user.id}:`, error);
                    }
                }
                permissions = allPermissions;
            }
            return permissions;
        } catch (error) {
            console.error('Error loading branch permissions:', error);
            throw error;
        }
    }

    async function loadBranches() {
        try {
            const response = await fetch(`${API_BASE}/branches/all`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading branches:', error);
            throw error;
        }
    }

    async function createBranchPermission(permissionData) {
        try {
            const response = await fetch(`${API_BASE}/branch-permissions`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(permissionData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating branch permission:', error);
            throw error;
        }
    }

    async function deleteBranchPermission(userId, branchId) {
        try {
            const response = await fetch(`${API_BASE}/branch-permissions/user/${userId}/branch/${branchId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting branch permission:', error);
            throw error;
        }
    }

    return {
        loadUsers,
        createUser,
        updateUser,
        deleteUser,
        updateUserPermissions,
        loadAllUsers,
        loadContainers,
        createContainer,
        updateContainer,
        deleteContainer,
        loadStorages,
        createStorage,
        updateStorage,
        deleteStorage,
        loadWithdrawalReasons,
        createWithdrawalReason,
        updateWithdrawalReason,
        deleteWithdrawalReason,
        loadProducts,
        loadProduct,
        createProduct,
        updateProduct,
        deleteProduct,
        loadSources,
        loadSource,
        createSource,
        updateSource,
        deleteSource,
        loadClientTypes,
        loadActiveClientTypes,
        loadClientType,
        createClientType,
        updateClientType,
        deleteClientType,
        loadClientTypeFields,
        loadClientTypeField,
        createClientTypeField,
        updateClientTypeField,
        deleteClientTypeField,
        loadClientTypeStaticFieldsConfig,
        updateClientTypeStaticFieldsConfig,
        importClients,
        loadClientTypePermissions,
        updateClientTypePermission,
        createClientTypePermission,
        deleteClientTypePermission,
        loadBranchPermissions,
        loadBranches,
        createBranchPermission,
        deleteBranchPermission
    };
})();
