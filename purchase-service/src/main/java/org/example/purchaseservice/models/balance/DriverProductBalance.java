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
@Table(name = "driver_product_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"driver_id", "product_id"}))
public class DriverProductBalance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "driver_id", nullable = false)
    private Long driverId;
    
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
     * Adds product to balance with average price recalculation
     * @param addedQuantity quantity of added product
     * @param totalPriceEur total price in EUR for this quantity
     */
    public void addProduct(BigDecimal addedQuantity, BigDecimal totalPriceEur) {
        if (addedQuantity == null || addedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Added quantity must be positive");
        }
        if (totalPriceEur == null || totalPriceEur.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total price must be non-negative");
        }
        
        BigDecimal newTotalCost = this.totalCostEur.add(totalPriceEur);
        BigDecimal newQuantity = this.quantity.add(addedQuantity);
        
        this.quantity = newQuantity;
        this.totalCostEur = newTotalCost;
        
        // Recalculate average price
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            this.averagePriceEur = newTotalCost.divide(newQuantity, 6, RoundingMode.HALF_UP);
        }
    }
    
    /**
     * Removes product from balance with average price recalculation
     * @param removedQuantity quantity of removed product
     * @param totalPriceOfRemovedPurchase total price of the specific purchase being removed
     */
    public void removeProduct(BigDecimal removedQuantity, BigDecimal totalPriceOfRemovedPurchase) {
        if (removedQuantity == null || removedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Removed quantity must be positive");
        }
        if (totalPriceOfRemovedPurchase == null || totalPriceOfRemovedPurchase.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total price of removed purchase must be non-negative");
        }
        if (removedQuantity.compareTo(this.quantity) > 0) {
            throw new IllegalArgumentException("Cannot remove more than available quantity. Available: " 
                    + this.quantity + ", trying to remove: " + removedQuantity);
        }
        
        // Use the SPECIFIC total price of the purchase being removed
        BigDecimal newQuantity = this.quantity.subtract(removedQuantity);
        BigDecimal newTotalCost = this.totalCostEur.subtract(totalPriceOfRemovedPurchase);
        
        this.quantity = newQuantity;
        this.totalCostEur = newTotalCost;
        
        // Recalculate average price based on remaining products
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            this.averagePriceEur = newTotalCost.divide(newQuantity, 6, RoundingMode.HALF_UP);
        } else {
            this.averagePriceEur = BigDecimal.ZERO;
            this.totalCostEur = BigDecimal.ZERO;
        }
    }
    
    /**
     * Updates balance when purchase is modified (rollback old + add new)
     */
    public void updateFromPurchaseChange(BigDecimal oldQuantity, BigDecimal oldTotalPrice, 
                                          BigDecimal newQuantity, BigDecimal newTotalPrice) {
        // Rollback old values using the SPECIFIC old total price
        if (oldQuantity != null && oldQuantity.compareTo(BigDecimal.ZERO) > 0 && oldTotalPrice != null) {
            this.totalCostEur = this.totalCostEur.subtract(oldTotalPrice);
            this.quantity = this.quantity.subtract(oldQuantity);
        }
        
        // Add new values
        if (newQuantity != null && newQuantity.compareTo(BigDecimal.ZERO) > 0 && newTotalPrice != null) {
            addProduct(newQuantity, newTotalPrice);
        } else if (this.quantity.compareTo(BigDecimal.ZERO) > 0) {
            // Recalculate average price if quantity > 0
            this.averagePriceEur = this.totalCostEur.divide(this.quantity, 6, RoundingMode.HALF_UP);
        } else {
            // Reset if quantity becomes 0
            this.quantity = BigDecimal.ZERO;
            this.averagePriceEur = BigDecimal.ZERO;
            this.totalCostEur = BigDecimal.ZERO;
        }
    }
}

