const StockRenderer = (function() {
    function renderBalance(balances, productMap, warehouseMap, onBalanceRowClick) {
        const container = document.getElementById('balance-container');
        if (!container) return;
        container.textContent = '';
        
        const balancesByWarehouse = {};
        balances.forEach(balance => {
            if (!balancesByWarehouse[balance.warehouseId]) {
                balancesByWarehouse[balance.warehouseId] = [];
            }
            balancesByWarehouse[balance.warehouseId].push(balance);
        });
        
        const sortedWarehouseIds = Object.keys(balancesByWarehouse).sort((a, b) => Number(a) - Number(b));
        
        if (sortedWarehouseIds.length === 0) {
            const emptyMessage = document.createElement('p');
            emptyMessage.textContent = CLIENT_MESSAGES.NO_DATA || 'Немає активних балансів на складі';
            container.appendChild(emptyMessage);
            return;
        }
        
        for (const warehouseId of sortedWarehouseIds) {
            const warehouseBalances = balancesByWarehouse[warehouseId];
            warehouseBalances.sort((a, b) => Number(a.productId) - Number(b.productId));
            
            const warehouseName = StockUtils.findNameByIdFromMap(warehouseMap, warehouseId) || '';
            
            const warehouseHeading = document.createElement('h3');
            warehouseHeading.textContent = `Склад: ${warehouseName}`;
            container.appendChild(warehouseHeading);
            
            const table = document.createElement('table');
            table.className = 'balance-table';
            
            const thead = document.createElement('thead');
            const headerRow = document.createElement('tr');
            
            const headers = ['Товар', 'Кількість (кг)', 'Середня ціна (EUR/кг)', 'Загальна вартість (EUR)'];
            headers.forEach(headerText => {
                const th = document.createElement('th');
                th.textContent = headerText;
                headerRow.appendChild(th);
            });
            
            thead.appendChild(headerRow);
            table.appendChild(thead);
            
            const tbody = document.createElement('tbody');
            let warehouseTotal = 0;
            let warehouseTotalQuantity = 0;
            
            for (const balance of warehouseBalances) {
                const productName = StockUtils.findNameByIdFromMap(productMap, balance.productId) || '';
                const quantity = StockUtils.formatNumber(balance.quantity, 2);
                const avgPrice = StockUtils.formatNumber(balance.averagePriceEur, 6);
                const totalCost = StockUtils.formatNumber(balance.totalCostEur, 6);
                warehouseTotal += parseFloat(balance.totalCostEur);
                warehouseTotalQuantity += parseFloat(balance.quantity);
                
                const row = document.createElement('tr');
                row.className = 'balance-row';
                row.setAttribute('data-warehouse-id', warehouseId);
                row.setAttribute('data-product-id', balance.productId);
                row.setAttribute('data-warehouse-name', warehouseName);
                row.setAttribute('data-product-name', productName);
                row.setAttribute('data-quantity', balance.quantity);
                row.setAttribute('data-total-cost', balance.totalCostEur);
                row.setAttribute('data-average-price', balance.averagePriceEur);
                
                const productCell = document.createElement('td');
                productCell.setAttribute('data-label', 'Товар');
                productCell.textContent = productName;
                row.appendChild(productCell);
                
                const quantityCell = document.createElement('td');
                quantityCell.setAttribute('data-label', 'Кількість (кг)');
                quantityCell.textContent = quantity;
                row.appendChild(quantityCell);
                
                const avgPriceCell = document.createElement('td');
                avgPriceCell.setAttribute('data-label', 'Середня ціна (EUR/кг)');
                avgPriceCell.textContent = avgPrice;
                row.appendChild(avgPriceCell);
                
                const totalCostCell = document.createElement('td');
                totalCostCell.setAttribute('data-label', 'Загальна вартість (EUR)');
                totalCostCell.textContent = totalCost;
                row.appendChild(totalCostCell);
                
                tbody.appendChild(row);
            }
            
            table.appendChild(tbody);
            
            const tfoot = document.createElement('tfoot');
            const footerRow = document.createElement('tr');
            footerRow.className = 'balance-tfoot-row';
            
            const footerCell1 = document.createElement('td');
            footerCell1.setAttribute('data-label', 'Загалом');
            const strong1 = document.createElement('strong');
            strong1.textContent = 'Загалом:';
            footerCell1.appendChild(strong1);
            footerRow.appendChild(footerCell1);
            
            const footerCell2 = document.createElement('td');
            footerCell2.setAttribute('data-label', 'Кількість (кг)');
            const strong2 = document.createElement('strong');
            strong2.textContent = StockUtils.formatNumber(warehouseTotalQuantity, 2);
            footerCell2.appendChild(strong2);
            footerRow.appendChild(footerCell2);
            
            const footerCell3 = document.createElement('td');
            footerCell3.setAttribute('data-label', 'Середня ціна (EUR/кг)');
            const strong3 = document.createElement('strong');
            const averagePrice = warehouseTotalQuantity > 0 ? warehouseTotal / warehouseTotalQuantity : 0;
            strong3.textContent = StockUtils.formatNumber(averagePrice, 6);
            footerCell3.appendChild(strong3);
            footerRow.appendChild(footerCell3);
            
            const footerCell4 = document.createElement('td');
            footerCell4.setAttribute('data-label', 'Загальна вартість (EUR)');
            const strong4 = document.createElement('strong');
            strong4.textContent = `${StockUtils.formatNumber(warehouseTotal, 6)} EUR`;
            footerCell4.appendChild(strong4);
            footerRow.appendChild(footerCell4);
            
            tfoot.appendChild(footerRow);
            table.appendChild(tfoot);
            
            container.appendChild(table);
        }
        
        if (onBalanceRowClick) {
            attachBalanceRowListeners(onBalanceRowClick);
        }
    }
    
    function attachBalanceRowListeners(onBalanceRowClick) {
        const rows = document.querySelectorAll('.balance-row');
        rows.forEach(row => {
            if (row._clickHandler) {
                row.removeEventListener('click', row._clickHandler);
            }
            row._clickHandler = () => {
                const data = row.dataset;
                onBalanceRowClick({
                    warehouseId: Number(data.warehouseId),
                    productId: Number(data.productId),
                    warehouseName: data.warehouseName,
                    productName: data.productName,
                    quantity: parseFloat(data.quantity ?? '0') || 0,
                    totalCost: parseFloat(data.totalCost ?? '0') || 0,
                    averagePrice: parseFloat(data.averagePrice ?? '0') || 0
                });
            };
            row.addEventListener('click', row._clickHandler);
        });
    }
    
    function renderWithdrawalHistory(withdrawals, productMap, warehouseMap, onWithdrawalClick) {
        const container = document.getElementById('history-content');
        if (!container) return;
        container.textContent = '';
        
        if (!withdrawals || withdrawals.length === 0) {
            const emptyRow = document.createElement('tr');
            const emptyCell = document.createElement('td');
            emptyCell.setAttribute('colspan', '9');
            emptyCell.style.textAlign = 'center';
            emptyCell.textContent = CLIENT_MESSAGES.NO_DATA || 'Немає даних';
            emptyRow.appendChild(emptyCell);
            container.appendChild(emptyRow);
            return;
        }
        
        for (const withdrawal of withdrawals) {
            const productName = StockUtils.findNameByIdFromMap(productMap, withdrawal.productId) || 'Не вказано';
            const warehouseName = StockUtils.findNameByIdFromMap(warehouseMap, withdrawal.warehouseId) || 'Не вказано';
            const reason = withdrawal.withdrawalReason ? withdrawal.withdrawalReason.name : 'Невідома причина';
            const unitPrice = withdrawal.unitPriceEur ? StockUtils.formatNumber(withdrawal.unitPriceEur, 6) + ' EUR' : '-';
            const totalCost = withdrawal.totalCostEur ? StockUtils.formatNumber(withdrawal.totalCostEur, 6) + ' EUR' : '-';
            
            const row = document.createElement('tr');
            row.setAttribute('data-id', withdrawal.id);
            
            const warehouseCell = document.createElement('td');
            warehouseCell.setAttribute('data-label', 'Склад');
            warehouseCell.textContent = warehouseName;
            row.appendChild(warehouseCell);
            
            const productCell = document.createElement('td');
            productCell.setAttribute('data-label', 'Товар');
            productCell.textContent = productName;
            row.appendChild(productCell);
            
            const reasonCell = document.createElement('td');
            reasonCell.setAttribute('data-label', 'Причина');
            reasonCell.textContent = reason;
            row.appendChild(reasonCell);
            
            const quantityCell = document.createElement('td');
            quantityCell.setAttribute('data-label', 'Кількість');
            quantityCell.textContent = `${withdrawal.quantity} кг`;
            row.appendChild(quantityCell);
            
            const unitPriceCell = document.createElement('td');
            unitPriceCell.setAttribute('data-label', 'Ціна за кг');
            unitPriceCell.style.textAlign = 'right';
            unitPriceCell.textContent = unitPrice;
            row.appendChild(unitPriceCell);
            
            const totalCostCell = document.createElement('td');
            totalCostCell.setAttribute('data-label', 'Загальна вартість');
            totalCostCell.style.textAlign = 'right';
            totalCostCell.style.fontWeight = 'bold';
            totalCostCell.textContent = totalCost;
            row.appendChild(totalCostCell);
            
            const withdrawalDateCell = document.createElement('td');
            withdrawalDateCell.setAttribute('data-label', 'Дата списання');
            withdrawalDateCell.textContent = withdrawal.withdrawalDate || '';
            row.appendChild(withdrawalDateCell);
            
            const descriptionCell = document.createElement('td');
            descriptionCell.setAttribute('data-label', 'Опис');
            descriptionCell.textContent = withdrawal.description || '';
            row.appendChild(descriptionCell);
            
            const createdAtCell = document.createElement('td');
            createdAtCell.setAttribute('data-label', 'Створено');
            createdAtCell.textContent = withdrawal.createdAt ? new Date(withdrawal.createdAt).toLocaleString() : '';
            row.appendChild(createdAtCell);
            
            if (onWithdrawalClick) {
                if (row._clickHandler) {
                    row.removeEventListener('click', row._clickHandler);
                }
                row._clickHandler = () => onWithdrawalClick(withdrawal.id, withdrawals);
                row.addEventListener('click', row._clickHandler);
            }
            container.appendChild(row);
        }
    }
    
    function renderWarehouseEntries(entries, productMap, warehouseMap, userMap) {
        const container = document.getElementById('entries-body');
        if (!container) return;
        container.textContent = '';
        
        if (!entries || entries.length === 0) {
            const emptyRow = document.createElement('tr');
            const emptyCell = document.createElement('td');
            emptyCell.setAttribute('colspan', '9');
            emptyCell.style.textAlign = 'center';
            emptyCell.textContent = CLIENT_MESSAGES.NO_DATA || 'Немає даних';
            emptyRow.appendChild(emptyCell);
            container.appendChild(emptyRow);
            return;
        }
        
        for (const entry of entries) {
            const productName = StockUtils.findNameByIdFromMap(productMap, entry.productId) || '';
            const warehouseName = StockUtils.findNameByIdFromMap(warehouseMap, entry.warehouseId) || '';
            const userName = StockUtils.findNameByIdFromMap(userMap, entry.userId) || '';
            const typeName = entry.type ? entry.type.name : 'Невідомий тип';
            const driverBalance = entry.driverBalanceQuantity || 0;
            const receivedQuantity = entry.quantity || 0;
            const difference = receivedQuantity - driverBalance;
            const totalCost = StockUtils.formatNumber(entry.totalCostEur, 6);
            
            const row = document.createElement('tr');
            row.setAttribute('data-id', entry.id);
            
            const warehouseCell = document.createElement('td');
            warehouseCell.setAttribute('data-label', 'Склад');
            warehouseCell.textContent = warehouseName;
            row.appendChild(warehouseCell);
            
            const entryDateCell = document.createElement('td');
            entryDateCell.setAttribute('data-label', 'Дата');
            entryDateCell.textContent = entry.entryDate || '';
            row.appendChild(entryDateCell);
            
            const userCell = document.createElement('td');
            userCell.setAttribute('data-label', 'Водій');
            userCell.textContent = userName;
            row.appendChild(userCell);
            
            const productCell = document.createElement('td');
            productCell.setAttribute('data-label', 'Товар');
            productCell.textContent = productName;
            row.appendChild(productCell);
            
            const typeCell = document.createElement('td');
            typeCell.setAttribute('data-label', 'Тип');
            typeCell.textContent = typeName;
            row.appendChild(typeCell);
            
            const receivedCell = document.createElement('td');
            receivedCell.setAttribute('data-label', 'Привезено');
            receivedCell.textContent = `${receivedQuantity} кг`;
            row.appendChild(receivedCell);
            
            const purchasedCell = document.createElement('td');
            purchasedCell.setAttribute('data-label', 'Закуплено');
            purchasedCell.textContent = `${driverBalance} кг`;
            row.appendChild(purchasedCell);
            
            const differenceCell = document.createElement('td');
            differenceCell.setAttribute('data-label', 'Різниця');
            differenceCell.textContent = `${difference} кг`;
            row.appendChild(differenceCell);
            
            const totalCostCell = document.createElement('td');
            totalCostCell.setAttribute('data-label', 'Вартість');
            totalCostCell.textContent = `${totalCost} EUR`;
            row.appendChild(totalCostCell);
            
            container.appendChild(row);
        }
    }
    
    function renderTransfers(transfers, productMap, warehouseMap, userMap, withdrawalReasonMap, onTransferClick) {
        const tbody = document.getElementById('transfers-body');
        if (!tbody) return;
        
        tbody.textContent = '';
        
        if (!Array.isArray(transfers) || transfers.length === 0) {
            const emptyRow = document.createElement('tr');
            const emptyCell = document.createElement('td');
            emptyCell.setAttribute('colspan', '10');
            emptyCell.style.textAlign = 'center';
            emptyCell.textContent = CLIENT_MESSAGES.NO_DATA || 'Немає даних';
            emptyRow.appendChild(emptyCell);
            tbody.appendChild(emptyRow);
            return;
        }
        
        transfers.forEach(item => {
            const fromProductName = StockUtils.findNameByIdFromMap(productMap, item.fromProductId) || 'Не вказано';
            const toProductName = StockUtils.findNameByIdFromMap(productMap, item.toProductId) || 'Не вказано';
            const warehouseName = StockUtils.findNameByIdFromMap(warehouseMap, item.warehouseId) || 'Не вказано';
            const userName = StockUtils.findNameByIdFromMap(userMap, item.userId) || 'Не вказано';
            const reasonObj = withdrawalReasonMap.get(Number(item.reasonId));
            const reasonName = reasonObj ? reasonObj.name : 'Не вказано';
            
            const row = document.createElement('tr');
            row.setAttribute('data-id', item.id);
            
            if (onTransferClick) {
                if (row._clickHandler) {
                    row.removeEventListener('click', row._clickHandler);
                }
                row._clickHandler = () => onTransferClick(Number(item.id));
                row.addEventListener('click', row._clickHandler);
            }
            
            const transferDateCell = document.createElement('td');
            transferDateCell.setAttribute('data-label', 'Дата');
            transferDateCell.style.textAlign = 'center';
            transferDateCell.textContent = item.transferDate || '';
            row.appendChild(transferDateCell);
            
            const warehouseCell = document.createElement('td');
            warehouseCell.setAttribute('data-label', 'Склад');
            warehouseCell.textContent = warehouseName;
            row.appendChild(warehouseCell);
            
            const fromProductCell = document.createElement('td');
            fromProductCell.setAttribute('data-label', 'З товару');
            fromProductCell.textContent = fromProductName;
            row.appendChild(fromProductCell);
            
            const toProductCell = document.createElement('td');
            toProductCell.setAttribute('data-label', 'До товару');
            toProductCell.textContent = toProductName;
            row.appendChild(toProductCell);
            
            const quantityCell = document.createElement('td');
            quantityCell.setAttribute('data-label', 'Кількість');
            quantityCell.style.textAlign = 'center';
            quantityCell.textContent = `${StockUtils.formatNumber(item.quantity, 2)} кг`;
            row.appendChild(quantityCell);
            
            const unitPriceCell = document.createElement('td');
            unitPriceCell.setAttribute('data-label', 'Ціна за кг');
            unitPriceCell.style.textAlign = 'right';
            unitPriceCell.textContent = `${StockUtils.formatNumber(item.unitPriceEur, 6)} EUR`;
            row.appendChild(unitPriceCell);
            
            const totalCostCell = document.createElement('td');
            totalCostCell.setAttribute('data-label', 'Загальна вартість');
            totalCostCell.style.textAlign = 'right';
            totalCostCell.style.fontWeight = 'bold';
            totalCostCell.textContent = `${StockUtils.formatNumber(item.totalCostEur, 6)} EUR`;
            row.appendChild(totalCostCell);
            
            const userCell = document.createElement('td');
            userCell.setAttribute('data-label', 'Виконавець');
            userCell.textContent = userName;
            row.appendChild(userCell);
            
            const reasonCell = document.createElement('td');
            reasonCell.setAttribute('data-label', 'Причина');
            reasonCell.textContent = reasonName;
            row.appendChild(reasonCell);
            
            const descriptionCell = document.createElement('td');
            descriptionCell.setAttribute('data-label', 'Опис');
            descriptionCell.textContent = item.description || '';
            row.appendChild(descriptionCell);
            
            tbody.appendChild(row);
        });
    }
    
    function renderVehicles(vehicles, userMap, onVehicleClick) {
        const tbody = document.getElementById('vehicles-tbody');
        
        if (!tbody) {
            return;
        }
        
        tbody.textContent = '';
        
        if (!vehicles || vehicles.length === 0) {
            const emptyRow = document.createElement('tr');
            const emptyCell = document.createElement('td');
            emptyCell.setAttribute('colspan', '5');
            emptyCell.style.textAlign = 'center';
            emptyCell.textContent = CLIENT_MESSAGES.NO_DATA || 'Немає даних';
            emptyRow.appendChild(emptyCell);
            tbody.appendChild(emptyRow);
            return;
        }
        
        vehicles.forEach(vehicle => {
            const row = document.createElement('tr');
            row.style.cursor = 'pointer';
            if (onVehicleClick) {
                if (row._clickHandler) {
                    row.removeEventListener('click', row._clickHandler);
                }
                row._clickHandler = () => onVehicleClick(vehicle.id);
                row.addEventListener('click', row._clickHandler);
            }
            
            const shipmentDateCell = document.createElement('td');
            shipmentDateCell.setAttribute('data-label', 'Дата відвантаження');
            shipmentDateCell.textContent = vehicle.shipmentDate || '';
            row.appendChild(shipmentDateCell);
            
            const vehicleNumberCell = document.createElement('td');
            vehicleNumberCell.setAttribute('data-label', 'Номер машини');
            vehicleNumberCell.textContent = vehicle.vehicleNumber || '-';
            row.appendChild(vehicleNumberCell);
            
            const totalCostCell = document.createElement('td');
            totalCostCell.setAttribute('data-label', 'Загальна вартість');
            totalCostCell.style.fontWeight = 'bold';
            totalCostCell.style.color = 'var(--bright-blue, #1976d2)';
            totalCostCell.textContent = `${StockUtils.formatNumber(vehicle.totalCostEur, 2)} EUR`;
            row.appendChild(totalCostCell);
            
            const descriptionCell = document.createElement('td');
            descriptionCell.setAttribute('data-label', 'Коментар');
            descriptionCell.textContent = vehicle.description || '-';
            row.appendChild(descriptionCell);
            
            const managerCell = document.createElement('td');
            managerCell.setAttribute('data-label', 'Менеджер');
            managerCell.textContent = userMap ? (StockUtils.findNameByIdFromMap(userMap, vehicle.managerId) || '-') : '-';
            row.appendChild(managerCell);
            
            tbody.appendChild(row);
        });
    }
    
    function renderVehicleDetails(vehicle, productMap, warehouseMap) {
        const itemsTbody = document.getElementById('vehicle-items-tbody');
        if (!itemsTbody) return;
        
        itemsTbody.textContent = '';
        
        if (!vehicle.items || vehicle.items.length === 0) {
            const emptyRow = document.createElement('tr');
            const emptyCell = document.createElement('td');
            emptyCell.setAttribute('colspan', '6');
            emptyCell.style.textAlign = 'center';
            emptyCell.textContent = CLIENT_MESSAGES.NO_DATA || 'Товари ще не додані';
            emptyRow.appendChild(emptyCell);
            itemsTbody.appendChild(emptyRow);
        } else {
            vehicle.items.forEach(item => {
                const productName = StockUtils.findNameByIdFromMap(productMap, item.productId) || 'Невідомий товар';
                const warehouseName = StockUtils.findNameByIdFromMap(warehouseMap, item.warehouseId) || 'Невідомий склад';

                const row = document.createElement('tr');
                row.className = 'vehicle-item-row';
                row.setAttribute('data-item-id', item.withdrawalId);
                
                const productCell = document.createElement('td');
                productCell.setAttribute('data-label', 'Товар');
                productCell.textContent = productName;
                row.appendChild(productCell);
                
                const warehouseCell = document.createElement('td');
                warehouseCell.setAttribute('data-label', 'Склад');
                warehouseCell.textContent = warehouseName;
                row.appendChild(warehouseCell);
                
                const quantityCell = document.createElement('td');
                quantityCell.setAttribute('data-label', 'Кількість');
                quantityCell.textContent = `${StockUtils.formatNumber(item.quantity, 2)} кг`;
                row.appendChild(quantityCell);
                
                const unitPriceCell = document.createElement('td');
                unitPriceCell.setAttribute('data-label', 'Ціна за кг');
                unitPriceCell.style.textAlign = 'right';
                unitPriceCell.textContent = `${StockUtils.formatNumber(item.unitPriceEur, 6)} EUR`;
                row.appendChild(unitPriceCell);
                
                const totalCostCell = document.createElement('td');
                totalCostCell.setAttribute('data-label', 'Загальна вартість');
                totalCostCell.style.textAlign = 'right';
                totalCostCell.style.fontWeight = 'bold';
                totalCostCell.textContent = `${StockUtils.formatNumber(item.totalCostEur, 6)} EUR`;
                row.appendChild(totalCostCell);
                
                const withdrawalDateCell = document.createElement('td');
                withdrawalDateCell.setAttribute('data-label', 'Дата списання');
                withdrawalDateCell.textContent = item.withdrawalDate || vehicle.shipmentDate || '';
                row.appendChild(withdrawalDateCell);
                
                itemsTbody.appendChild(row);
            });
        }
        
        const totalCostElement = document.getElementById('vehicle-total-cost');
        if (totalCostElement) {
            totalCostElement.textContent = StockUtils.formatNumber(vehicle.totalCostEur, 2);
        }
    }
    
    function renderDiscrepancies(discrepancies, productMap, warehouseMap, userMap) {
        const tbody = document.getElementById('discrepancies-table-body');
        if (!tbody) return;
        tbody.textContent = '';
        
        if (!discrepancies || discrepancies.length === 0) {
            const emptyRow = document.createElement('tr');
            const emptyCell = document.createElement('td');
            emptyCell.setAttribute('colspan', '10');
            emptyCell.style.textAlign = 'center';
            emptyCell.style.padding = '30px';
            emptyCell.style.color = '#999';
            emptyCell.textContent = CLIENT_MESSAGES.NO_DATA || 'Немає даних';
            emptyRow.appendChild(emptyCell);
            tbody.appendChild(emptyRow);
            return;
        }
        
        for (const item of discrepancies) {
            const driverName = StockUtils.findNameByIdFromMap(userMap, item.driverId) || '';
            const productName = StockUtils.findNameByIdFromMap(productMap, item.productId) || '';
            const warehouseName = StockUtils.findNameByIdFromMap(warehouseMap, item.warehouseId) || '';
            
            const typeLabel = item.type === 'LOSS' ? 'Втрата' : 'Придбання';
            const typeClass = item.type === 'LOSS' ? 'loss' : 'gain';
            const typeColor = item.type === 'LOSS' ? '#d32f2f' : '#388e3c';
            
            const row = document.createElement('tr');
            
            const receiptDateCell = document.createElement('td');
            receiptDateCell.setAttribute('data-label', 'Дата');
            receiptDateCell.textContent = StockUtils.formatDate(item.receiptDate);
            row.appendChild(receiptDateCell);
            
            const driverCell = document.createElement('td');
            driverCell.setAttribute('data-label', 'Водій');
            driverCell.textContent = driverName;
            row.appendChild(driverCell);
            
            const productCell = document.createElement('td');
            productCell.setAttribute('data-label', 'Товар');
            productCell.textContent = productName;
            row.appendChild(productCell);
            
            const warehouseCell = document.createElement('td');
            warehouseCell.setAttribute('data-label', 'Склад');
            warehouseCell.textContent = warehouseName;
            row.appendChild(warehouseCell);
            
            const purchasedCell = document.createElement('td');
            purchasedCell.setAttribute('data-label', 'Закуплено');
            purchasedCell.style.textAlign = 'center';
            purchasedCell.textContent = `${item.purchasedQuantity} кг`;
            row.appendChild(purchasedCell);
            
            const receivedCell = document.createElement('td');
            receivedCell.setAttribute('data-label', 'Прийнято');
            receivedCell.style.textAlign = 'center';
            receivedCell.textContent = `${item.receivedQuantity} кг`;
            row.appendChild(receivedCell);
            
            const discrepancyCell = document.createElement('td');
            discrepancyCell.setAttribute('data-label', 'Різниця');
            discrepancyCell.style.textAlign = 'center';
            discrepancyCell.style.fontWeight = 'bold';
            discrepancyCell.style.color = typeColor;
            discrepancyCell.textContent = `${item.discrepancyQuantity > 0 ? '+' : ''}${item.discrepancyQuantity} кг`;
            row.appendChild(discrepancyCell);
            
            const unitPriceCell = document.createElement('td');
            unitPriceCell.setAttribute('data-label', 'Ціна/кг');
            unitPriceCell.style.textAlign = 'right';
            unitPriceCell.textContent = `${StockUtils.formatNumber(item.unitPriceEur, 6)} EUR`;
            row.appendChild(unitPriceCell);
            
            const valueCell = document.createElement('td');
            valueCell.setAttribute('data-label', 'Вартість');
            valueCell.style.textAlign = 'right';
            valueCell.style.fontWeight = 'bold';
            valueCell.textContent = `${StockUtils.formatNumber(Math.abs(item.discrepancyValueEur), 6)} EUR`;
            row.appendChild(valueCell);
            
            const typeCell = document.createElement('td');
            typeCell.setAttribute('data-label', 'Тип');
            typeCell.style.textAlign = 'center';
            const typeBadge = document.createElement('span');
            typeBadge.className = `discrepancy-type-badge ${typeClass}`;
            typeBadge.textContent = typeLabel;
            typeCell.appendChild(typeBadge);
            row.appendChild(typeCell);
            
            tbody.appendChild(row);
        }
    }
    
    function renderDiscrepanciesStatistics(stats) {
        const totalLossesValue = document.getElementById('total-losses-value');
        const totalLossesCount = document.getElementById('total-losses-count');
        const totalGainsValue = document.getElementById('total-gains-value');
        const totalGainsCount = document.getElementById('total-gains-count');
        const netValue = document.getElementById('net-value');
        
        if (totalLossesValue) {
            totalLossesValue.textContent = `${StockUtils.formatNumber(stats.totalLossesValue, 6)} EUR`;
        }
        if (totalLossesCount) {
            totalLossesCount.textContent = `${stats.lossCount} записів`;
        }
        if (totalGainsValue) {
            totalGainsValue.textContent = `${StockUtils.formatNumber(stats.totalGainsValue, 6)} EUR`;
        }
        if (totalGainsCount) {
            totalGainsCount.textContent = `${stats.gainCount} записів`;
        }
        if (netValue) {
            netValue.textContent = `${StockUtils.formatNumber(stats.netValue, 6)} EUR`;
            
            if (stats.netValue < 0) {
                netValue.style.color = '#d32f2f';
            } else if (stats.netValue > 0) {
                netValue.style.color = '#388e3c';
            } else {
                netValue.style.color = '#1976d2';
            }
        }
    }
    
    function renderDriverBalances(balances, productMap, userMap) {
        const container = document.getElementById('driver-balances-container');
        if (!container) return;
        container.textContent = '';
        
        const balancesByDriver = {};
        balances.forEach(balance => {
            if (!balancesByDriver[balance.driverId]) {
                balancesByDriver[balance.driverId] = [];
            }
            balancesByDriver[balance.driverId].push(balance);
        });
        
        const driverIds = Object.keys(balancesByDriver);
        if (driverIds.length === 0) {
            const emptyMessage = document.createElement('p');
            emptyMessage.textContent = CLIENT_MESSAGES.NO_DATA || 'Немає активних балансів водіїв';
            container.appendChild(emptyMessage);
            return;
        }
        
        for (const [driverId, driverBalances] of Object.entries(balancesByDriver)) {
            const driverName = StockUtils.findNameByIdFromMap(userMap, driverId) || '';
            
            const driverHeading = document.createElement('h4');
            driverHeading.textContent = `Водій: ${driverName}`;
            container.appendChild(driverHeading);
            
            const table = document.createElement('table');
            table.className = 'balance-table';
            
            const thead = document.createElement('thead');
            const headerRow = document.createElement('tr');
            
            const headers = ['Товар', 'Кількість (кг)', 'Середня ціна (EUR/кг)', 'Загальна вартість (EUR)'];
            headers.forEach(headerText => {
                const th = document.createElement('th');
                th.textContent = headerText;
                headerRow.appendChild(th);
            });
            
            thead.appendChild(headerRow);
            table.appendChild(thead);
            
            const tbody = document.createElement('tbody');
            let driverTotal = 0;
            
            for (const balance of driverBalances) {
                const productName = StockUtils.findNameByIdFromMap(productMap, balance.productId) || '';
                const quantity = StockUtils.formatNumber(balance.quantity, 2);
                const avgPrice = StockUtils.formatNumber(balance.averagePriceEur, 6);
                const totalCost = StockUtils.formatNumber(balance.totalCostEur, 6);
                driverTotal += parseFloat(totalCost);
                
                const row = document.createElement('tr');
                
                const productCell = document.createElement('td');
                productCell.setAttribute('data-label', 'Товар');
                productCell.textContent = productName;
                row.appendChild(productCell);
                
                const quantityCell = document.createElement('td');
                quantityCell.setAttribute('data-label', 'Кількість (кг)');
                quantityCell.textContent = quantity;
                row.appendChild(quantityCell);
                
                const avgPriceCell = document.createElement('td');
                avgPriceCell.setAttribute('data-label', 'Середня ціна (EUR/кг)');
                avgPriceCell.textContent = avgPrice;
                row.appendChild(avgPriceCell);
                
                const totalCostCell = document.createElement('td');
                totalCostCell.setAttribute('data-label', 'Загальна вартість (EUR)');
                totalCostCell.textContent = totalCost;
                row.appendChild(totalCostCell);
                
                tbody.appendChild(row);
            }
            
            table.appendChild(tbody);
            
            const tfoot = document.createElement('tfoot');
            const footerRow = document.createElement('tr');
            footerRow.className = 'balance-tfoot-row';
            
            const footerCell1 = document.createElement('td');
            footerCell1.setAttribute('data-label', 'Загальна вартість товару водія');
            const strong1 = document.createElement('strong');
            strong1.textContent = 'Загальна вартість товару водія:';
            footerCell1.appendChild(strong1);
            footerRow.appendChild(footerCell1);
            
            const footerCell2 = document.createElement('td');
            footerCell2.setAttribute('data-label', '');
            footerRow.appendChild(footerCell2);
            
            const footerCell3 = document.createElement('td');
            footerCell3.setAttribute('data-label', '');
            footerRow.appendChild(footerCell3);
            
            const footerCell4 = document.createElement('td');
            footerCell4.setAttribute('data-label', 'Сума');
            const strong2 = document.createElement('strong');
            strong2.textContent = `${StockUtils.formatNumber(driverTotal, 6)} EUR`;
            footerCell4.appendChild(strong2);
            footerRow.appendChild(footerCell4);
            
            tfoot.appendChild(footerRow);
            table.appendChild(tfoot);
            
            container.appendChild(table);
        }
    }
    
    function renderBalanceHistory(history, userMap) {
        const balanceHistoryBody = document.getElementById('balance-history-body');
        const balanceHistoryEmpty = document.getElementById('balance-history-empty');
        
        if (!balanceHistoryBody || !balanceHistoryEmpty) {
            return;
        }
        
        balanceHistoryBody.textContent = '';
        
        if (!Array.isArray(history) || history.length === 0) {
            balanceHistoryEmpty.style.display = 'block';
            return;
        }
        
        balanceHistoryEmpty.style.display = 'none';
        
        const typeLabels = {
            QUANTITY: 'Кількість',
            TOTAL_COST: 'Загальна вартість',
            BOTH: 'Кількість та вартість'
        };
        
        history.forEach(item => {
            const userName = StockUtils.findNameByIdFromMap(userMap, item.userId) || '—';
            const typeLabel = typeLabels[item.adjustmentType] || item.adjustmentType || '—';
            const createdAt = item.createdAt ? new Date(item.createdAt).toLocaleString() : '—';
            const quantityChange = `${StockUtils.formatNumber(item.previousQuantity, 2)} → ${StockUtils.formatNumber(item.newQuantity, 2)} кг`;
            const totalChange = `${StockUtils.formatNumber(item.previousTotalCostEur, 6)} → ${StockUtils.formatNumber(item.newTotalCostEur, 6)} EUR`;
            const averageChange = `${StockUtils.formatNumber(item.previousAveragePriceEur, 6)} → ${StockUtils.formatNumber(item.newAveragePriceEur, 6)} EUR/кг`;
            const description = item.description || '—';
            
            const row = document.createElement('tr');
            
            const createdAtCell = document.createElement('td');
            createdAtCell.setAttribute('data-label', 'Дата');
            createdAtCell.textContent = createdAt;
            row.appendChild(createdAtCell);
            
            const userCell = document.createElement('td');
            userCell.setAttribute('data-label', 'Користувач');
            userCell.textContent = userName;
            row.appendChild(userCell);
            
            const typeCell = document.createElement('td');
            typeCell.setAttribute('data-label', 'Тип');
            typeCell.textContent = typeLabel;
            row.appendChild(typeCell);
            
            const quantityCell = document.createElement('td');
            quantityCell.setAttribute('data-label', 'Кількість');
            quantityCell.textContent = quantityChange;
            row.appendChild(quantityCell);
            
            const totalCell = document.createElement('td');
            totalCell.setAttribute('data-label', 'Загальна вартість');
            totalCell.textContent = totalChange;
            row.appendChild(totalCell);
            
            const averageCell = document.createElement('td');
            averageCell.setAttribute('data-label', 'Середня ціна');
            averageCell.textContent = averageChange;
            row.appendChild(averageCell);
            
            const descriptionCell = document.createElement('td');
            descriptionCell.setAttribute('data-label', 'Коментар');
            descriptionCell.textContent = description;
            row.appendChild(descriptionCell);
            
            balanceHistoryBody.appendChild(row);
        });
    }
    
    function updatePagination(total, page, pageInfoId, prevBtnId, nextBtnId) {
        const pageInfo = document.getElementById(pageInfoId);
        const prevBtn = document.getElementById(prevBtnId);
        const nextBtn = document.getElementById(nextBtnId);
        
        if (pageInfo) {
            pageInfo.textContent = `Сторінка ${page + 1} з ${total}`;
        }
        if (prevBtn) {
            prevBtn.disabled = page === 0;
        }
        if (nextBtn) {
            nextBtn.disabled = page >= total - 1;
        }
    }
    
    function updateDiscrepanciesPagination(data) {
        const start = data.page * data.size + 1;
        const end = Math.min((data.page + 1) * data.size, data.totalElements);
        const infoElement = document.getElementById('discrepancies-info');
        if (infoElement) {
            infoElement.textContent = `Показано ${start}-${end} з ${data.totalElements}`;
        }
        
        const prevBtn = document.getElementById('discrepancies-prev');
        const nextBtn = document.getElementById('discrepancies-next');
        
        if (prevBtn) {
            prevBtn.disabled = data.page === 0;
        }
        if (nextBtn) {
            nextBtn.disabled = data.page >= data.totalPages - 1;
        }
        
        const pageNumbersContainer = document.getElementById('discrepancies-page-numbers');
        if (!pageNumbersContainer) return;
        pageNumbersContainer.textContent = '';
        
        const maxPagesToShow = 5;
        let startPage = Math.max(0, data.page - Math.floor(maxPagesToShow / 2));
        let endPage = Math.min(data.totalPages, startPage + maxPagesToShow);
        
        if (endPage - startPage < maxPagesToShow) {
            startPage = Math.max(0, endPage - maxPagesToShow);
        }
        
        for (let i = startPage; i < endPage; i++) {
            const pageBtn = document.createElement('button');
            pageBtn.textContent = i + 1;
            pageBtn.className = 'button';
            
            if (i === data.page) {
                pageBtn.classList.add('active');
            }
            
            pageNumbersContainer.appendChild(pageBtn);
        }
    }
    
    function updateTransfersPagination(data) {
        const totalPages = data.totalPages || 1;
        const currentPage = data.number || 0;
        
        const infoSpan = document.getElementById('transfers-page-info');
        if (infoSpan) {
            infoSpan.textContent = `Сторінка ${currentPage + 1} з ${totalPages}`;
        }
        
        const prevBtn = document.getElementById('transfers-prev-page');
        const nextBtn = document.getElementById('transfers-next-page');
        
        if (prevBtn) {
            prevBtn.disabled = currentPage === 0;
        }
        
        if (nextBtn) {
            nextBtn.disabled = currentPage >= totalPages - 1;
        }
    }
    
    function updateVehiclesPagination(data) {
        const totalPages = data.totalPages || 1;
        const currentPage = data.page ?? 0;
        
        const infoSpan = document.getElementById('vehicles-page-info');
        if (infoSpan) {
            infoSpan.textContent = `Сторінка ${currentPage + 1} з ${totalPages}`;
        }
        
        const prevBtn = document.getElementById('vehicles-prev-page');
        const nextBtn = document.getElementById('vehicles-next-page');
        
        if (prevBtn) {
            prevBtn.disabled = currentPage === 0;
        }
        
        if (nextBtn) {
            nextBtn.disabled = currentPage >= totalPages - 1;
        }
    }
    
    function exportTableToExcel(tableId, filename = 'withdrawal_data') {
        const table = document.getElementById(tableId);
        if (!table) return;
        
        const worksheet = XLSX.utils.table_to_sheet(table);
        const workbook = XLSX.utils.book_new();
        
        const maxWidths = [];
        const rows = XLSX.utils.sheet_to_json(worksheet, {header: 1});
        rows.forEach(row => {
            row.forEach((cell, i) => {
                const cellLength = cell ? String(cell).length : 10;
                maxWidths[i] = Math.max(maxWidths[i] || 10, cellLength);
            });
        });
        worksheet['!cols'] = maxWidths.map(w => ({wch: w}));
        
        XLSX.utils.book_append_sheet(workbook, worksheet, 'Sheet1');
        XLSX.writeFile(workbook, `${filename}.xlsx`);
    }
    
    return {
        renderBalance,
        renderWithdrawalHistory,
        renderWarehouseEntries,
        renderTransfers,
        renderVehicles,
        renderVehicleDetails,
        renderDiscrepancies,
        renderDiscrepanciesStatistics,
        renderDriverBalances,
        renderBalanceHistory,
        updatePagination,
        updateDiscrepanciesPagination,
        updateTransfersPagination,
        updateVehiclesPagination,
        exportTableToExcel
    };
})();
