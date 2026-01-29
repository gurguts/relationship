package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.dto.warehouse.DiscrepancyStatisticsDTO;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class WarehouseDiscrepancyStatisticsCalculator {
    
    public DiscrepancyStatisticsDTO calculateStatistics(@NonNull List<WarehouseDiscrepancy> discrepancies) {
        StatisticsAccumulator accumulator = discrepancies.stream()
                .collect(StatisticsAccumulator::new,
                        StatisticsAccumulator::accumulate,
                        StatisticsAccumulator::combine);

        DiscrepancyStatisticsDTO stats = new DiscrepancyStatisticsDTO();
        stats.setTotalLossesValue(accumulator.getTotalLosses());
        stats.setTotalGainsValue(accumulator.getTotalGains());
        stats.setLossCount(accumulator.getLossCount());
        stats.setGainCount(accumulator.getGainCount());
        stats.setNetValue(accumulator.getTotalGains().add(accumulator.getTotalLosses().negate()));
        
        return stats;
    }
    
    private static class StatisticsAccumulator {
        private BigDecimal totalLosses = BigDecimal.ZERO;
        private BigDecimal totalGains = BigDecimal.ZERO;
        private long lossCount = 0;
        private long gainCount = 0;

        void accumulate(WarehouseDiscrepancy discrepancy) {
            BigDecimal value = discrepancy.getDiscrepancyValueEur();
            if (value == null) {
                return;
            }
            
            if (discrepancy.getType() == WarehouseDiscrepancy.DiscrepancyType.LOSS) {
                totalLosses = totalLosses.add(value.abs());
                lossCount++;
            } else if (discrepancy.getType() == WarehouseDiscrepancy.DiscrepancyType.GAIN) {
                totalGains = totalGains.add(value);
                gainCount++;
            }
        }

        void combine(StatisticsAccumulator other) {
            this.totalLosses = this.totalLosses.add(other.totalLosses);
            this.totalGains = this.totalGains.add(other.totalGains);
            this.lossCount += other.lossCount;
            this.gainCount += other.gainCount;
        }

        BigDecimal getTotalLosses() {
            return totalLosses;
        }

        BigDecimal getTotalGains() {
            return totalGains;
        }

        long getLossCount() {
            return lossCount;
        }

        long getGainCount() {
            return gainCount;
        }
    }
}
