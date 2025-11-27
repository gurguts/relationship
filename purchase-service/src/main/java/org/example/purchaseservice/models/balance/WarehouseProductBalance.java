package org.example.purchaseservice.models.balance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "warehouse_product_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "product_id"}))
public class WarehouseProductBalance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "quantity", nullable = false, precision = 20, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;
    
    @Column(name = "average_price_eur", nullable = false, precision = 20, scale = 6)
    private BigDecimal averagePriceEur = BigDecimal.ZERO;
    
    @Column(name = "total_cost_eur", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalCostEur = BigDecimal.ZERO;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Adds product to warehouse with average price recalculation
     * @param addedQuantity quantity of added product
     * @param addedTotalCost total cost of added product (NOT unit price)
     */
    public void addProduct(BigDecimal addedQuantity, BigDecimal addedTotalCost) {
        if (addedQuantity == null || addedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Added quantity must be positive");
        }
        if (addedTotalCost == null || addedTotalCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Added total cost must be non-negative");
        }
        
        BigDecimal newTotalCost = this.totalCostEur.add(addedTotalCost);
        BigDecimal newQuantity = this.quantity.add(addedQuantity);
        
        this.quantity = newQuantity;
        this.totalCostEur = newTotalCost;
        
        // Recalculate average price from total cost and quantity (round up to avoid loss of precision)
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            this.averagePriceEur = newTotalCost.divide(newQuantity, 6, RoundingMode.CEILING);
        }
    }
    
    /**
     * Removes product from warehouse (during withdrawal)
     * @param removedQuantity quantity of removed product
     * @return average price of the product for withdrawal cost calculation
     */
    public BigDecimal removeProduct(BigDecimal removedQuantity) {
        if (removedQuantity == null || removedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Removed quantity must be positive");
        }
        if (removedQuantity.compareTo(this.quantity) > 0) {
            throw new IllegalArgumentException("Cannot remove more than available quantity. Available: " 
                    + this.quantity + ", trying to remove: " + removedQuantity);
        }
        
        BigDecimal currentAveragePrice = this.averagePriceEur;
        BigDecimal removedCost = removedQuantity.multiply(currentAveragePrice);
        BigDecimal newQuantity = this.quantity.subtract(removedQuantity);
        BigDecimal newTotalCost = this.totalCostEur.subtract(removedCost);
        
        this.quantity = newQuantity;
        this.totalCostEur = newTotalCost;
        
        // Average price stays the same during removal
        // But if quantity becomes 0, reset all
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            this.averagePriceEur = BigDecimal.ZERO;
            this.totalCostEur = BigDecimal.ZERO;
        }
        
        return currentAveragePrice;
    }
    
    /**
     * Removes product with specific cost from warehouse (for product movements/transfers)
     * This allows removing product at a specific price different from average
     * @param removedQuantity quantity of removed product
     * @param removedTotalCost total cost to remove (quantity * specified price)
     */
    public void removeProductWithCost(BigDecimal removedQuantity, BigDecimal removedTotalCost) {
        if (removedQuantity == null || removedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Removed quantity must be positive");
        }
        if (removedTotalCost == null || removedTotalCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Removed total cost must be non-negative");
        }
        if (removedQuantity.compareTo(this.quantity) > 0) {
            throw new IllegalArgumentException("Cannot remove more than available quantity. Available: " 
                    + this.quantity + ", trying to remove: " + removedQuantity);
        }
        
        BigDecimal newQuantity = this.quantity.subtract(removedQuantity);
        BigDecimal newTotalCost = this.totalCostEur.subtract(removedTotalCost);
        
        // Ensure we don't go negative on cost
        if (newTotalCost.compareTo(BigDecimal.ZERO) < 0) {
            newTotalCost = BigDecimal.ZERO;
        }
        
        this.quantity = newQuantity;
        this.totalCostEur = newTotalCost;
        
        // Recalculate average price (round up to avoid loss of precision)
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            this.averagePriceEur = newTotalCost.divide(newQuantity, 6, RoundingMode.CEILING);
        } else {
            // If quantity becomes 0, reset all
            this.averagePriceEur = BigDecimal.ZERO;
            this.totalCostEur = BigDecimal.ZERO;
        }
    }

    /**
     * Adjusts only the total cost (price revaluation) without changing quantity
     * @param costDelta positive value adds to warehouse total cost, negative subtracts
     */
    public void adjustTotalCost(BigDecimal costDelta) {
        if (costDelta == null) {
            throw new IllegalArgumentException("Cost delta must not be null");
        }

        BigDecimal newTotalCost = this.totalCostEur.add(costDelta);
        if (newTotalCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Resulting total cost cannot be negative");
        }

        this.totalCostEur = newTotalCost;

        if (this.quantity.compareTo(BigDecimal.ZERO) > 0) {
            this.averagePriceEur = newTotalCost.divide(this.quantity, 6, RoundingMode.CEILING);
        } else {
            // no quantity -> reset cost as well
            this.totalCostEur = BigDecimal.ZERO;
            this.averagePriceEur = BigDecimal.ZERO;
        }
    }
}

