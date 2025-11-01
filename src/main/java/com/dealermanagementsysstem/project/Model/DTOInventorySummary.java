package com.dealermanagementsysstem.project.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DTOInventorySummary {
    private int total;
    private int inStock;
    private int reserved;
    private int sold;
    private int transferred;
    private BigDecimal inventoryValue;
}


