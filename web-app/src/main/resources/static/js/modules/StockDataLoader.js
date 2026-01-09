const StockDataLoader = (function() {
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
    
    async function fetchUsers() {
        try {
            const response = await fetch(`${API_BASE}/user`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching users:', error);
            throw error;
        }
    }
    
    async function fetchWithdrawalReasons() {
        try {
            const response = await fetch(`${API_BASE}/withdrawal-reason`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching withdrawal reasons:', error);
            throw error;
        }
    }
    
    async function loadBalance() {
        try {
            const response = await fetch(`${API_BASE}/warehouse/balances/active`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading balance:', error);
            throw error;
        }
    }
    
    async function loadWithdrawalHistory(page, size, filters) {
        try {
            const queryParams = new URLSearchParams({
                page: page.toString(),
                size: size.toString(),
                sort: 'withdrawalDate',
                direction: 'DESC',
                filters: JSON.stringify(filters)
            });
            
            const response = await fetch(`${API_BASE}/warehouse/withdrawals?${queryParams}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading withdrawal history:', error);
            throw error;
        }
    }
    
    async function loadWarehouseEntries(page, size, filters) {
        try {
            const queryParams = new URLSearchParams({
                page: page.toString(),
                size: size.toString(),
                sort: 'entryDate',
                direction: 'DESC',
                filters: JSON.stringify(filters)
            });
            
            const response = await fetch(`${API_BASE}/warehouse/receipts?${queryParams}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading warehouse entries:', error);
            throw error;
        }
    }
    
    async function loadDriverBalance(driverId, productId) {
        try {
            const response = await fetch(`${API_BASE}/driver/balances/${driverId}/product/${productId}`);
            if (response.status === 404) {
                return null;
            }
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading driver balance:', error);
            throw error;
        }
    }
    
    async function loadDriverBalances() {
        try {
            const response = await fetch(`${API_BASE}/driver/balances/active`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading driver balances:', error);
            throw error;
        }
    }
    
    async function loadVehicles(dateFrom, dateTo) {
        try {
            let url = `${API_BASE}/vehicles/by-date-range?`;
            
            if (dateFrom && dateTo) {
                url += `fromDate=${dateFrom}&toDate=${dateTo}`;
            } else {
                const today = new Date();
                const last30Days = new Date();
                last30Days.setDate(today.getDate() - 30);
                url += `fromDate=${last30Days.toISOString().split('T')[0]}&toDate=${today.toISOString().split('T')[0]}`;
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
    
    async function loadDiscrepanciesStatistics(filters) {
        try {
            const params = new URLSearchParams();
            if (filters.type) params.append('type', filters.type);
            if (filters.dateFrom) params.append('dateFrom', filters.dateFrom);
            if (filters.dateTo) params.append('dateTo', filters.dateTo);
            
            const url = `${API_BASE}/warehouse/discrepancies/statistics${params.toString() ? '?' + params.toString() : ''}`;
            const response = await fetch(url);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading discrepancies statistics:', error);
            throw error;
        }
    }
    
    async function loadDiscrepancies(page, size, filters) {
        try {
            const params = new URLSearchParams({
                page: page.toString(),
                size: size.toString(),
                sort: 'receiptDate',
                direction: 'DESC'
            });
            
            if (filters.type) params.append('type', filters.type);
            if (filters.dateFrom) params.append('dateFrom', filters.dateFrom);
            if (filters.dateTo) params.append('dateTo', filters.dateTo);
            
            const response = await fetch(`${API_BASE}/warehouse/discrepancies?${params}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading discrepancies:', error);
            throw error;
        }
    }
    
    async function exportDiscrepancies(filters) {
        try {
            const params = new URLSearchParams();
            if (filters.type) params.append('type', filters.type);
            if (filters.dateFrom) params.append('dateFrom', filters.dateFrom);
            if (filters.dateTo) params.append('dateTo', filters.dateTo);
            
            const response = await fetch(`${API_BASE}/warehouse/discrepancies/export?${params.toString()}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.blob();
        } catch (error) {
            console.error('Error exporting discrepancies:', error);
            throw error;
        }
    }
    
    async function loadTransfers(page, size, filters) {
        try {
            const params = new URLSearchParams({
                page: page.toString(),
                size: size.toString(),
                sort: 'transferDate',
                direction: 'desc'
            });
            
            if (filters.dateFrom) params.append('dateFrom', filters.dateFrom);
            if (filters.dateTo) params.append('dateTo', filters.dateTo);
            if (filters.warehouseId) params.append('warehouseId', filters.warehouseId);
            if (filters.fromProductId) params.append('fromProductId', filters.fromProductId);
            if (filters.toProductId) params.append('toProductId', filters.toProductId);
            if (filters.userId) params.append('userId', filters.userId);
            if (filters.reasonId) params.append('reasonId', filters.reasonId);
            
            const response = await fetch(`${API_BASE}/warehouse/transfers?${params}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading transfers:', error);
            throw error;
        }
    }
    
    async function loadBalanceHistory(warehouseId, productId) {
        try {
            const response = await fetch(`${API_BASE}/warehouse/balances/${warehouseId}/product/${productId}/history`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            return await response.json();
        } catch (error) {
            console.error('Error loading balance history:', error);
            throw error;
        }
    }
    
    async function createWithdrawal(withdrawalData) {
        try {
            const response = await fetch(`${API_BASE}/warehouse/withdraw`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(withdrawalData)
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
            console.error('Error creating withdrawal:', error);
            throw error;
        }
    }
    
    async function updateWithdrawal(id, withdrawalData) {
        try {
            const response = await fetch(`${API_BASE}/warehouse/withdraw/${id}`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(withdrawalData)
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
            console.error('Error updating withdrawal:', error);
            throw error;
        }
    }
    
    async function createTransfer(transferData) {
        try {
            const response = await fetch(`${API_BASE}/warehouse/transfer`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(transferData)
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
            console.error('Error creating transfer:', error);
            throw error;
        }
    }
    
    async function updateTransfer(id, transferData) {
        try {
            const response = await fetch(`${API_BASE}/warehouse/transfers/${id}`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(transferData)
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
            console.error('Error updating transfer:', error);
            throw error;
        }
    }
    
    async function createEntry(entryData) {
        try {
            const response = await fetch(`${API_BASE}/warehouse/receipts`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(entryData)
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
            console.error('Error creating entry:', error);
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
            return await response.json();
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
    
    async function updateBalance(warehouseId, productId, balanceData) {
        try {
            const response = await fetch(`${API_BASE}/warehouse/balances/${warehouseId}/product/${productId}`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(balanceData)
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
            console.error('Error updating balance:', error);
            throw error;
        }
    }
    
    async function exportTransfers(filters) {
        try {
            const params = new URLSearchParams();
            if (filters.dateFrom) params.append('dateFrom', filters.dateFrom);
            if (filters.dateTo) params.append('dateTo', filters.dateTo);
            if (filters.warehouseId) params.append('warehouseId', filters.warehouseId);
            if (filters.fromProductId) params.append('fromProductId', filters.fromProductId);
            if (filters.toProductId) params.append('toProductId', filters.toProductId);
            if (filters.userId) params.append('userId', filters.userId);
            if (filters.reasonId) params.append('reasonId', filters.reasonId);
            
            const response = await fetch(`${API_BASE}/warehouse/transfers/export?${params}`);
            if (!response.ok) {
                const error = await parseErrorResponse(response);
                throw error;
            }
            
            let filename = 'product_transfers.xlsx';
            const contentDisposition = response.headers.get('Content-Disposition');
            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i);
                if (filenameMatch) {
                    filename = decodeURIComponent(filenameMatch[1] || filenameMatch[2]);
                }
            }
            
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = filename.endsWith('.xlsx') ? filename : `${filename}.xlsx`;
            document.body.appendChild(anchor);
            anchor.click();
            document.body.removeChild(anchor);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error('Error exporting transfers:', error);
            throw error;
        }
    }
    
    return {
        fetchProducts,
        fetchWarehouses,
        fetchUsers,
        fetchWithdrawalReasons,
        loadBalance,
        loadWithdrawalHistory,
        loadWarehouseEntries,
        loadDriverBalance,
        loadDriverBalances,
        loadVehicles,
        loadVehicleDetails,
        loadDiscrepanciesStatistics,
        loadDiscrepancies,
        exportDiscrepancies,
        exportTransfers,
        loadTransfers,
        loadBalanceHistory,
        createWithdrawal,
        updateWithdrawal,
        createTransfer,
        updateTransfer,
        createEntry,
        createVehicle,
        updateVehicle,
        deleteVehicle,
        addProductToVehicle,
        updateVehicleProduct,
        updateBalance
    };
})();
