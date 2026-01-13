const SettingsDataLoader = (function() {
    const API_BASE = '/api/v1';
    
    const loadingStates = {
        users: false,
        sources: false,
        products: false,
        clientTypes: false,
        storages: false,
        withdrawalReasons: false,
        containers: false,
        vehicleSenders: false,
        vehicleReceivers: false,
        vehicleTerminals: false,
        vehicleDestinationCountries: false,
        vehicleDestinationPlaces: false
    };

    async function loadUsers() {
        if (loadingStates.users) return;
        loadingStates.users = true;
        try {
            const response = await fetch(`${API_BASE}/user`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading users:', error);
            throw error;
        } finally {
            loadingStates.users = false;
        }
    }

    async function createUser(userData) {
        try {
            const response = await fetch(`${API_BASE}/user`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(userData),
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
            const response = await fetch(`${API_BASE}/users/${userId}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(userData),
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
                method: 'DELETE',
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

    async function loadSources() {
        if (loadingStates.sources) return;
        loadingStates.sources = true;
        try {
            const response = await fetch(`${API_BASE}/sources`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading sources:', error);
            throw error;
        } finally {
            loadingStates.sources = false;
        }
    }

    async function createSource(sourceData) {
        try {
            const response = await fetch(`${API_BASE}/sources`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(sourceData),
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
            const response = await fetch(`${API_BASE}/sources/${sourceId}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(sourceData),
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
            const response = await fetch(`${API_BASE}/sources/${sourceId}`, {
                method: 'DELETE',
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

    async function loadProducts() {
        if (loadingStates.products) return;
        loadingStates.products = true;
        try {
            const response = await fetch(`${API_BASE}/product?page=0&size=1000`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            const data = await response.json();
            return Array.isArray(data) ? data : (data.content || []);
        } catch (error) {
            console.error('Error loading products:', error);
            throw error;
        } finally {
            loadingStates.products = false;
        }
    }

    async function createProduct(productData) {
        try {
            const response = await fetch(`${API_BASE}/product`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(productData),
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
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(productData),
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
                method: 'DELETE',
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

    async function loadClientTypes() {
        if (loadingStates.clientTypes) return;
        loadingStates.clientTypes = true;
        try {
            const response = await fetch(`${API_BASE}/client-type?page=0&size=1000`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            const data = await response.json();
            return Array.isArray(data) ? data : (data.content || []);
        } catch (error) {
            console.error('Error loading client types:', error);
            throw error;
        } finally {
            loadingStates.clientTypes = false;
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

    async function loadVehicleSenders() {
        if (loadingStates.vehicleSenders) return;
        loadingStates.vehicleSenders = true;
        try {
            const response = await fetch(`${API_BASE}/vehicle-senders`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading vehicle senders:', error);
            throw error;
        } finally {
            loadingStates.vehicleSenders = false;
        }
    }

    async function createVehicleSender(senderData) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-senders`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(senderData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating vehicle sender:', error);
            throw error;
        }
    }

    async function updateVehicleSender(senderId, senderData) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-senders/${senderId}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(senderData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating vehicle sender:', error);
            throw error;
        }
    }

    async function getVehicleSender(senderId) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-senders/${senderId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error getting vehicle sender:', error);
            throw error;
        }
    }

    async function deleteVehicleSender(senderId) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-senders/${senderId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting vehicle sender:', error);
            throw error;
        }
    }

    async function loadVehicleReceivers() {
        if (loadingStates.vehicleReceivers) return;
        loadingStates.vehicleReceivers = true;
        try {
            const response = await fetch(`${API_BASE}/vehicle-receivers`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading vehicle receivers:', error);
            throw error;
        } finally {
            loadingStates.vehicleReceivers = false;
        }
    }

    async function createVehicleReceiver(receiverData) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-receivers`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(receiverData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating vehicle receiver:', error);
            throw error;
        }
    }

    async function updateVehicleReceiver(receiverId, receiverData) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-receivers/${receiverId}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(receiverData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating vehicle receiver:', error);
            throw error;
        }
    }

    async function getVehicleReceiver(receiverId) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-receivers/${receiverId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error getting vehicle receiver:', error);
            throw error;
        }
    }

    async function deleteVehicleReceiver(receiverId) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-receivers/${receiverId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting vehicle receiver:', error);
            throw error;
        }
    }

    async function loadVehicleTerminals() {
        if (loadingStates.vehicleTerminals) return;
        loadingStates.vehicleTerminals = true;
        try {
            const response = await fetch(`${API_BASE}/vehicle-terminals`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading vehicle terminals:', error);
            throw error;
        } finally {
            loadingStates.vehicleTerminals = false;
        }
    }

    async function createVehicleTerminal(terminalData) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-terminals`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(terminalData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating vehicle terminal:', error);
            throw error;
        }
    }

    async function updateVehicleTerminal(terminalId, terminalData) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-terminals/${terminalId}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(terminalData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating vehicle terminal:', error);
            throw error;
        }
    }

    async function getVehicleTerminal(terminalId) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-terminals/${terminalId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error getting vehicle terminal:', error);
            throw error;
        }
    }

    async function deleteVehicleTerminal(terminalId) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-terminals/${terminalId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting vehicle terminal:', error);
            throw error;
        }
    }

    async function loadVehicleDestinationCountries() {
        if (loadingStates.vehicleDestinationCountries) return;
        loadingStates.vehicleDestinationCountries = true;
        try {
            const response = await fetch(`${API_BASE}/vehicle-destination-countries`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading vehicle destination countries:', error);
            throw error;
        } finally {
            loadingStates.vehicleDestinationCountries = false;
        }
    }

    async function createVehicleDestinationCountry(countryData) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-destination-countries`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(countryData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating vehicle destination country:', error);
            throw error;
        }
    }

    async function updateVehicleDestinationCountry(countryId, countryData) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-destination-countries/${countryId}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(countryData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating vehicle destination country:', error);
            throw error;
        }
    }

    async function getVehicleDestinationCountry(countryId) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-destination-countries/${countryId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error getting vehicle destination country:', error);
            throw error;
        }
    }

    async function deleteVehicleDestinationCountry(countryId) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-destination-countries/${countryId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting vehicle destination country:', error);
            throw error;
        }
    }

    async function loadVehicleDestinationPlaces() {
        if (loadingStates.vehicleDestinationPlaces) return;
        loadingStates.vehicleDestinationPlaces = true;
        try {
            const response = await fetch(`${API_BASE}/vehicle-destination-places`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading vehicle destination places:', error);
            throw error;
        } finally {
            loadingStates.vehicleDestinationPlaces = false;
        }
    }

    async function createVehicleDestinationPlace(placeData) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-destination-places`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(placeData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating vehicle destination place:', error);
            throw error;
        }
    }

    async function updateVehicleDestinationPlace(placeId, placeData) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-destination-places/${placeId}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(placeData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating vehicle destination place:', error);
            throw error;
        }
    }

    async function getVehicleDestinationPlace(placeId) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-destination-places/${placeId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error getting vehicle destination place:', error);
            throw error;
        }
    }

    async function deleteVehicleDestinationPlace(placeId) {
        try {
            const response = await fetch(`${API_BASE}/vehicle-destination-places/${placeId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting vehicle destination place:', error);
            throw error;
        }
    }

    async function loadCategories(typeFilter) {
        try {
            if (typeFilter) {
                const response = await fetch(`${API_BASE}/transaction-categories/type/${typeFilter}`);
                if (!response.ok) {
                    const error = await parseErrorResponse(response);
                    throw error;
                }
                return await response.json();
            } else {
                const allTypes = ['INTERNAL_TRANSFER', 'EXTERNAL_INCOME', 'EXTERNAL_EXPENSE', 'CLIENT_PAYMENT', 'CURRENCY_CONVERSION', 'PURCHASE', 'VEHICLE_EXPENSE'];
                const allCategories = [];
                for (const type of allTypes) {
                    try {
                        const typeResponse = await fetch(`${API_BASE}/transaction-categories/type/${type}`);
                        if (typeResponse.ok) {
                            const typeCategories = await typeResponse.json();
                            allCategories.push(...typeCategories);
                        }
                    } catch (e) {
                        console.warn(`Failed to load categories for type ${type}:`, e);
                    }
                }
                return allCategories;
            }
        } catch (error) {
            console.error('Error loading categories:', error);
            throw error;
        }
    }

    async function createCategory(categoryData) {
        try {
            const response = await fetch(`${API_BASE}/transaction-categories`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(categoryData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating category:', error);
            throw error;
        }
    }

    async function updateCategory(categoryId, categoryData) {
        try {
            const response = await fetch(`${API_BASE}/transaction-categories/${categoryId}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(categoryData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating category:', error);
            throw error;
        }
    }

    async function loadCategory(categoryId) {
        try {
            const response = await fetch(`${API_BASE}/transaction-categories/${categoryId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading category:', error);
            throw error;
        }
    }

    async function deleteCategory(categoryId) {
        try {
            const response = await fetch(`${API_BASE}/transaction-categories/${categoryId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting category:', error);
            throw error;
        }
    }

    async function loadCounterparties(typeFilter) {
        try {
            if (typeFilter) {
                const response = await fetch(`${API_BASE}/counterparties/type/${typeFilter}`);
                if (!response.ok) {
                    const error = await parseErrorResponse(response);
                    throw error;
                }
                return await response.json();
            } else {
                const incomeResponse = await fetch(`${API_BASE}/counterparties/type/INCOME`);
                const expenseResponse = await fetch(`${API_BASE}/counterparties/type/EXPENSE`);
                const income = incomeResponse.ok ? await incomeResponse.json() : [];
                const expense = expenseResponse.ok ? await expenseResponse.json() : [];
                return [...income, ...expense];
            }
        } catch (error) {
            console.error('Error loading counterparties:', error);
            throw error;
        }
    }

    async function createCounterparty(counterpartyData) {
        try {
            const response = await fetch(`${API_BASE}/counterparties`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(counterpartyData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating counterparty:', error);
            throw error;
        }
    }

    async function updateCounterparty(counterpartyId, counterpartyData) {
        try {
            const response = await fetch(`${API_BASE}/counterparties/${counterpartyId}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(counterpartyData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating counterparty:', error);
            throw error;
        }
    }

    async function loadCounterparty(counterpartyId) {
        try {
            const response = await fetch(`${API_BASE}/counterparties/${counterpartyId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading counterparty:', error);
            throw error;
        }
    }

    async function deleteCounterparty(counterpartyId) {
        try {
            const response = await fetch(`${API_BASE}/counterparties/${counterpartyId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting counterparty:', error);
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

    async function createBranch(branchData) {
        try {
            const response = await fetch(`${API_BASE}/branches`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(branchData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating branch:', error);
            throw error;
        }
    }

    async function updateBranch(branchId, branchData) {
        try {
            const response = await fetch(`${API_BASE}/branches/${branchId}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(branchData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating branch:', error);
            throw error;
        }
    }

    async function loadBranch(branchId) {
        try {
            const response = await fetch(`${API_BASE}/branches/${branchId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading branch:', error);
            throw error;
        }
    }

    async function deleteBranch(branchId) {
        try {
            const response = await fetch(`${API_BASE}/branches/${branchId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting branch:', error);
            throw error;
        }
    }

    async function loadAccounts() {
        try {
            const response = await fetch(`${API_BASE}/accounts`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading accounts:', error);
            throw error;
        }
    }

    async function createAccount(accountData) {
        try {
            const response = await fetch(`${API_BASE}/accounts`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(accountData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating account:', error);
            throw error;
        }
    }

    async function updateAccount(accountId, accountData) {
        try {
            const response = await fetch(`${API_BASE}/accounts/${accountId}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(accountData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating account:', error);
            throw error;
        }
    }

    async function loadAccount(accountId) {
        try {
            const response = await fetch(`${API_BASE}/accounts/${accountId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading account:', error);
            throw error;
        }
    }

    async function deleteAccount(accountId) {
        try {
            const response = await fetch(`${API_BASE}/accounts/${accountId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting account:', error);
            throw error;
        }
    }

    async function loadCarriers() {
        try {
            const response = await fetch(`${API_BASE}/carriers`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading carriers:', error);
            throw error;
        }
    }

    async function createCarrier(carrierData) {
        try {
            const response = await fetch(`${API_BASE}/carriers`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(carrierData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating carrier:', error);
            throw error;
        }
    }

    async function updateCarrier(carrierId, carrierData) {
        try {
            const response = await fetch(`${API_BASE}/carriers/${carrierId}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(carrierData),
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating carrier:', error);
            throw error;
        }
    }

    async function loadCarrier(carrierId) {
        try {
            const response = await fetch(`${API_BASE}/carriers/${carrierId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading carrier:', error);
            throw error;
        }
    }

    async function deleteCarrier(carrierId) {
        try {
            const response = await fetch(`${API_BASE}/carriers/${carrierId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
        } catch (error) {
            console.error('Error deleting carrier:', error);
            throw error;
        }
    }

    return {
        loadUsers,
        createUser,
        updateUser,
        deleteUser,
        loadSources,
        createSource,
        updateSource,
        deleteSource,
        loadProducts,
        createProduct,
        updateProduct,
        deleteProduct,
        loadClientTypes,
        loadActiveClientTypes,
        loadClientType,
        loadVehicleSenders,
        createVehicleSender,
        updateVehicleSender,
        getVehicleSender,
        deleteVehicleSender,
        loadVehicleReceivers,
        createVehicleReceiver,
        updateVehicleReceiver,
        getVehicleReceiver,
        deleteVehicleReceiver,
        loadVehicleTerminals,
        createVehicleTerminal,
        updateVehicleTerminal,
        getVehicleTerminal,
        deleteVehicleTerminal,
        loadVehicleDestinationCountries,
        createVehicleDestinationCountry,
        updateVehicleDestinationCountry,
        getVehicleDestinationCountry,
        deleteVehicleDestinationCountry,
        loadVehicleDestinationPlaces,
        createVehicleDestinationPlace,
        updateVehicleDestinationPlace,
        getVehicleDestinationPlace,
        deleteVehicleDestinationPlace,
        loadCategories,
        createCategory,
        updateCategory,
        loadCategory,
        deleteCategory,
        loadCounterparties,
        createCounterparty,
        updateCounterparty,
        loadCounterparty,
        deleteCounterparty,
        loadBranches,
        createBranch,
        updateBranch,
        loadBranch,
        deleteBranch,
        loadAccounts,
        createAccount,
        updateAccount,
        loadAccount,
        deleteAccount,
        loadCarriers,
        createCarrier,
        updateCarrier,
        loadCarrier,
        deleteCarrier
    };
})();
