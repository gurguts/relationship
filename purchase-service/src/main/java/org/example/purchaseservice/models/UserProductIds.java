package org.example.purchaseservice.models;

import java.util.List;

public record UserProductIds(List<Long> userIds, List<Long> productIds) {
}
