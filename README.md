# 🎯 Balance Tracking System - Setup Guide

## ⚡ Quick Start

### 1. **Backup your database** (MANDATORY!)
```bash
mysqldump -u root -p export_data > backup_$(date +%Y%m%d).sql
```

### 2. **Execute SQL script**

**Option A: Command line** (recommended)
```bash
mysql -u root -p export_data < DATABASE_SETUP.sql
```

**Option B: MySQL Workbench**
1. Open `DATABASE_SETUP.sql`
2. Click Execute (⚡)
3. Script will run automatically

**Option C: MySQL console**
```bash
mysql -u root -p
USE export_data;
SOURCE D:/programming/relationship/DATABASE_SETUP.sql;
```

**Note:** You may see warnings like "Duplicate key name" - these are SAFE to ignore!

### 3. **Start application**
```bash
mvn spring-boot:run
```

**That's it!** ✅

---

## 📊 What the SQL Script Does

### Creates 3 new tables:
1. `driver_product_balances` - Driver inventory tracking
2. `warehouse_product_balances` - Warehouse inventory tracking
3. `shipments` - Vehicle shipment grouping

### Adds columns to existing tables:
- `purchases` → `total_price_uah`, `unit_price_uah`
- `warehouse_entries` → `unit_price_uah`, `total_cost_uah`
- `warehouse_withdrawals` → `unit_price_uah`, `total_cost_uah`, `shipment_id`

### Renames table:
- `warehouse_entries` → `warehouse_receipts` (better naming)

### Adds indexes for performance

---

## 🔧 After Setup - Initialize Warehouse Balances

If you already have products in warehouse, set initial balances:

```http
POST http://localhost:8093/api/v1/warehouse/balances/initialize
Content-Type: application/json

{
  "warehouseId": 1,
  "productId": 5,
  "initialQuantity": 100.00,
  "averagePriceUah": 15.00,
  "note": "Initial balance as of 2025-01-05"
}
```

Repeat for each product on each warehouse.

---

## 🎯 How It Works

### Driver Purchase Flow:
```
Driver creates purchase → Auto-updates driver_product_balances
                       → Calculates weighted average price
```

### Warehouse Receipt Flow:
```
Warehouse receives from driver → Removes from driver balance
                               → Adds to warehouse balance
                               → Preserves history in warehouse_receipts
```

### Warehouse Withdrawal Flow:
```
Withdraw to vehicle → Removes from warehouse balance
                   → Calculates cost
                   → Adds to shipment total cost
                   → Preserves history in warehouse_withdrawals
```

---

## 🆕 New API Endpoints

### Warehouse Balances:
```http
# Initialize balance
POST /api/v1/warehouse/balances/initialize

# Get balances
GET /api/v1/warehouse/balances/{warehouseId}
GET /api/v1/warehouse/balances/{warehouseId}/product/{productId}
GET /api/v1/warehouse/balances/active
```

### Shipments (Vehicles):
```http
# Create shipment
POST /api/v1/shipments
{
  "shipmentDate": "2025-01-10",
  "vehicleNumber": "AA1234BB",
  "description": "Delivery to Kyiv"
}

# Get shipment details with all products
GET /api/v1/shipments/{shipmentId}

# Get shipments by date
GET /api/v1/shipments/by-date?date=2025-01-10

# Get shipments by date range
GET /api/v1/shipments/by-date-range?fromDate=2025-01-01&toDate=2025-01-31
```

### Warehouse Receipts (renamed from entries):
```http
# New endpoints:
GET  /api/v1/warehouse/receipts
POST /api/v1/warehouse/receipts
PATCH /api/v1/warehouse/receipts/{id}

# Old endpoints still work (deprecated):
GET  /api/v1/warehouse/entries  ← Redirects to /receipts
POST /api/v1/warehouse/entries  ← Redirects to /receipts
```

---

## 📊 Example Workflow

```
Day 1: Driver purchases 10 kg for 164 UAH
→ driver_product_balances: 10 kg @ 16.4 UAH = 164 UAH

Day 2: Driver purchases 5 kg for 100 UAH
→ driver_product_balances: 15 kg @ 17.6 UAH = 264 UAH

Day 3: Warehouse receives 10 kg from driver
→ driver_product_balances: 5 kg @ 17.6 UAH = 88 UAH
→ warehouse_product_balances: 10 kg @ 17.6 UAH = 176 UAH

Day 4: Create shipment #123 (vehicle AA1234BB)
→ shipments: id=123, totalCost=0

Day 4: Withdraw 5 kg to shipment #123
→ warehouse_product_balances: 5 kg @ 17.6 UAH = 88 UAH
→ warehouse_withdrawals: 5 kg @ 17.6 = 88 UAH, shipment_id=123
→ shipments: id=123, totalCost=88 UAH

Day 4: View shipment details
→ GET /api/v1/shipments/123
→ Response: All products, quantities, prices, total cost
```

---

## ✅ Benefits

✅ **Automatic tracking** - No manual calculations
✅ **Weighted average** - Mathematically correct pricing
✅ **Zero rounding errors** - Uses totalPrice approach
✅ **Vehicle cost tracking** - See exact cost per shipment
✅ **Full history** - All operations preserved
✅ **Multi-currency support** - Auto-converts to UAH

---

## 📋 Verification After Setup

```sql
-- Check new tables exist:
SHOW TABLES LIKE 'driver_product_balances';
SHOW TABLES LIKE 'warehouse_product_balances';
SHOW TABLES LIKE 'shipments';

-- Check renamed table:
SHOW TABLES LIKE 'warehouse_receipts';  -- Should exist
SHOW TABLES LIKE 'warehouse_entries';   -- Should NOT exist

-- Check new columns in purchases:
DESCRIBE purchases;  -- Look for total_price_uah, unit_price_uah

-- Check new columns in warehouse_receipts:
DESCRIBE warehouse_receipts;  -- Look for unit_price_uah, total_cost_uah

-- Check new columns in warehouse_withdrawals:
DESCRIBE warehouse_withdrawals;  -- Look for unit_price_uah, total_cost_uah, shipment_id
```

---

## 🆘 If Something Goes Wrong

1. **Restore from backup:**
   ```bash
   mysql -u root -p export_data < backup_YYYYMMDD.sql
   ```

2. **Check errors:**
   - If "table already exists" - table was created before
   - If "column already exists" - column was added before
   - Both are safe to ignore!

3. **The script is idempotent:**
   - Uses `IF NOT EXISTS` where possible
   - Safe to run multiple times
   - Won't duplicate data

---

## 🎯 Summary

**What to do:**
1. ✅ Backup database
2. ✅ Run `DATABASE_SETUP.sql`
3. ✅ Start application
4. ✅ Set initial warehouse balances (if needed)
5. ✅ Start using the system!

**That's all!** 🚀

