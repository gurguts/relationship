package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.repositories.WarehouseReceiptRepository;
import org.example.purchaseservice.services.impl.ISourceService;
import org.example.purchaseservice.services.impl.IUserService;
import org.example.purchaseservice.utils.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseValidator {
    
    private static final String AUTHORITY_EDIT_STRANGERS = "purchase:edit_strangers";
    
    private final WarehouseReceiptRepository warehouseReceiptRepository;
    private final ISourceService sourceService;
    private final IUserService userService;
    
    public void validatePurchaseUpdatePermissions(@NonNull Purchase existingPurchase) {
        if (isPurchaseReceived(existingPurchase)) {
            throw new PurchaseException("PURCHASE_RECEIVED", 
                    "Неможливо редагувати закупку, оскільки товар вже прийнято кладовщиком.");
        }

        String fullName = getFullName();
        boolean canEditData = SecurityUtils.hasAuthority(AUTHORITY_EDIT_STRANGERS);

        if (existingPurchase.getSource() != null) {
            SourceDTO sourceResponse = sourceService.getSourceName(existingPurchase.getSource());
            String sourceName = sourceResponse.getName();
            boolean isOwner = fullName != null && fullName.equals(sourceName);
            if (!isOwner && !canEditData) {
                throw new PurchaseException("ONLY_OWNER", "You cannot change someone else's purchase.");
            }
        }
    }
    
    public boolean isPurchaseReceived(@NonNull Purchase purchase) {
        if (purchase.getUser() == null || purchase.getProduct() == null || purchase.getCreatedAt() == null) {
            return false;
        }

        Specification<WarehouseReceipt> spec = (root, _, cb) -> cb.and(
                cb.equal(root.get("userId"), purchase.getUser()),
                cb.equal(root.get("productId"), purchase.getProduct()),
                cb.greaterThanOrEqualTo(root.get("createdAt"), purchase.getCreatedAt())
        );
        
        List<WarehouseReceipt> receipts = warehouseReceiptRepository.findAll(spec);

        return !receipts.isEmpty();
    }
    
    private String getFullName() {
        String login = SecurityUtils.getCurrentUserLogin();
        if (login == null || login.trim().isEmpty()) {
            return null;
        }
        
        return userService.getUserFullNameFromLogin(login);
    }
}
