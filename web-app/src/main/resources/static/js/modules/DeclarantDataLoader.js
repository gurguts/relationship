const DeclarantDataLoader = (function() {
    const API_BASE = '/api/v1';
    
    async function fetchProducts() {
        try {
            const response = await fetch(`${API_BASE}/product`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching products:', error);
            throw error;
        }
    }
    
    async function fetchWarehouses() {
        try {
            const response = await fetch(`${API_BASE}/warehouse`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching warehouses:', error);
            throw error;
        }
    }
    
    async function fetchCarriers() {
        try {
            const response = await fetch(`${API_BASE}/carriers`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching carriers:', error);
            throw error;
        }
    }
    
    async function fetchVehicleSenders() {
        try {
            const response = await fetch(`${API_BASE}/vehicle-senders`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching vehicle senders:', error);
            throw error;
        }
    }
    
    async function fetchVehicleReceivers() {
        try {
            const response = await fetch(`${API_BASE}/vehicle-receivers`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching vehicle receivers:', error);
            throw error;
        }
    }
    
    async function fetchVehicleTerminals() {
        try {
            const response = await fetch(`${API_BASE}/vehicle-terminals`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching vehicle terminals:', error);
            throw error;
        }
    }
    
    async function fetchVehicleDestinationCountries() {
        try {
            const response = await fetch(`${API_BASE}/vehicle-destination-countries`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching vehicle destination countries:', error);
            throw error;
        }
    }
    
    async function fetchVehicleDestinationPlaces() {
        try {
            const response = await fetch(`${API_BASE}/vehicle-destination-places`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching vehicle destination places:', error);
            throw error;
        }
    }
    
    async function loadVehicles(page, size, sort, direction, searchTerm, filtersJson) {
        try {
            let url = `${API_BASE}/vehicles/search?page=${page}&size=${size}&sort=${sort}&direction=${direction}`;
            
            if (searchTerm) {
                url += `&q=${encodeURIComponent(searchTerm)}`;
            }
            
            if (filtersJson) {
                url += `&filters=${encodeURIComponent(filtersJson)}`;
            }
            
            const response = await fetch(url);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading vehicles:', error);
            throw error;
        }
    }
    
    async function loadVehicleDetails(vehicleId) {
        try {
            const response = await fetch(`${API_BASE}/vehicles/${vehicleId}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading vehicle details:', error);
            throw error;
        }
    }
    
    async function createVehicle(vehicleData) {
        try {
            const response = await fetch(`${API_BASE}/vehicles`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(vehicleData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating vehicle:', error);
            throw error;
        }
    }
    
    async function updateVehicle(vehicleId, vehicleData) {
        try {
            const response = await fetch(`${API_BASE}/vehicles/${vehicleId}`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(vehicleData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const text = await response.text();
                if (text.trim()) {
                    return JSON.parse(text);
                }
            }
            return true;
        } catch (error) {
            console.error('Error updating vehicle:', error);
            throw error;
        }
    }
    
    async function deleteVehicle(vehicleId) {
        try {
            const response = await fetch(`${API_BASE}/vehicles/${vehicleId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const text = await response.text();
                if (text.trim()) {
                    return JSON.parse(text);
                }
            }
            return true;
        } catch (error) {
            console.error('Error deleting vehicle:', error);
            throw error;
        }
    }
    
    async function addProductToVehicle(vehicleId, productData) {
        try {
            const response = await fetch(`${API_BASE}/vehicles/${vehicleId}/products`, {
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
            console.error('Error adding product to vehicle:', error);
            throw error;
        }
    }
    
    async function updateVehicleProduct(vehicleId, productId, productData) {
        try {
            const response = await fetch(`${API_BASE}/vehicles/${vehicleId}/products/${productId}`, {
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
            console.error('Error updating vehicle product:', error);
            throw error;
        }
    }
    
    async function loadVehicleExpenses(vehicleId) {
        try {
            const response = await fetch(`${API_BASE}/vehicles/${vehicleId}/expenses`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading vehicle expenses:', error);
            throw error;
        }
    }
    
    async function createVehicleExpense(vehicleId, expenseData) {
        try {
            const response = await fetch(`${API_BASE}/vehicles/${vehicleId}/expenses`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(expenseData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const text = await response.text();
                if (text.trim()) {
                    return JSON.parse(text);
                }
            }
            return true;
        } catch (error) {
            console.error('Error creating vehicle expense:', error);
            throw error;
        }
    }
    
    async function updateVehicleExpense(expenseId, expenseData) {
        try {
            const response = await fetch(`${API_BASE}/vehicles/expenses/${expenseId}`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(expenseData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const text = await response.text();
                if (text.trim()) {
                    return JSON.parse(text);
                }
            }
            return true;
        } catch (error) {
            console.error('Error updating vehicle expense:', error);
            throw error;
        }
    }
    
    async function deleteVehicleExpense(expenseId) {
        try {
            const response = await fetch(`${API_BASE}/vehicles/expenses/${expenseId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const text = await response.text();
                if (text.trim()) {
                    return JSON.parse(text);
                }
            }
            return true;
        } catch (error) {
            console.error('Error deleting vehicle expense:', error);
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
    
    async function loadCategoriesForVehicleExpense() {
        try {
            const response = await fetch(`${API_BASE}/transaction-categories/type/VEHICLE_EXPENSE`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading categories:', error);
            throw error;
        }
    }
    
    async function checkExchangeRatesFreshness() {
        try {
            const response = await fetch(`${API_BASE}/exchange-rates`);
            if (!response.ok) {
                return false;
            }
            const rates = await response.json();
            
            if (!rates || rates.length === 0) {
                return false;
            }
            
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            
            for (const rate of rates) {
                let rateDate = null;
                
                if (rate.updatedAt) {
                    rateDate = new Date(rate.updatedAt);
                } else if (rate.createdAt) {
                    rateDate = new Date(rate.createdAt);
                } else {
                    return false;
                }
                
                rateDate.setHours(0, 0, 0, 0);
                
                if (rateDate.getTime() < today.getTime()) {
                    return false;
                }
            }
            
            return true;
        } catch (error) {
            console.error('Error checking exchange rates freshness:', error);
            return false;
        }
    }
    
    async function getExchangeRate(fromCurrency, toCurrency) {
        try {
            // Поточний бекенд підтримує лише курс до EUR:
            // GET /api/v1/exchange-rates/{currency}/rate -> BigDecimal
            if (toCurrency !== 'EUR') {
                console.warn('getExchangeRate currently supports only conversion to EUR');
                return null;
            }

            const response = await fetch(`${API_BASE}/exchange-rates/${fromCurrency}/rate`);
            if (!response.ok) {
                return null;
            }
            const rate = await response.json();
            // Повертаємо у форматі { rate }, щоб не ламати існуючий код
            return { rate };
        } catch (error) {
            console.error('Error getting exchange rate:', error);
            return null;
        }
    }
    
    async function exportVehicles(filtersJson, searchTerm) {
        try {
            let url = `${API_BASE}/vehicles/export`;
            const params = new URLSearchParams();
            
            if (searchTerm) {
                params.append('q', searchTerm);
            }
            
            if (filtersJson) {
                params.append('filters', filtersJson);
            }
            
            if (params.toString()) {
                url += '?' + params.toString();
            }
            
            const response = await fetch(url);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            const blob = await response.blob();
            const downloadUrl = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = downloadUrl;
            link.download = `vehicles_${new Date().toISOString().split('T')[0]}.xlsx`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(downloadUrl);
        } catch (error) {
            console.error('Error exporting vehicles:', error);
            throw error;
        }
    }
    
    async function createCarrier(carrierData) {
        try {
            const response = await fetch(`${API_BASE}/carriers`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(carrierData)
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
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(carrierData)
            });
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const text = await response.text();
                if (text.trim()) {
                    return JSON.parse(text);
                }
            }
            return true;
        } catch (error) {
            console.error('Error updating carrier:', error);
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
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const text = await response.text();
                if (text.trim()) {
                    return JSON.parse(text);
                }
            }
            return true;
        } catch (error) {
            console.error('Error deleting carrier:', error);
            throw error;
        }
    }
    
    return {
        fetchProducts,
        fetchWarehouses,
        fetchCarriers,
        fetchVehicleSenders,
        fetchVehicleReceivers,
        fetchVehicleTerminals,
        fetchVehicleDestinationCountries,
        fetchVehicleDestinationPlaces,
        loadVehicles,
        loadVehicleDetails,
        createVehicle,
        updateVehicle,
        deleteVehicle,
        addProductToVehicle,
        updateVehicleProduct,
        loadVehicleExpenses,
        createVehicleExpense,
        updateVehicleExpense,
        deleteVehicleExpense,
        loadAccounts,
        loadCategoriesForVehicleExpense,
        checkExchangeRatesFreshness,
        getExchangeRate,
        exportVehicles,
        createCarrier,
        updateCarrier,
        deleteCarrier
    };
})();
