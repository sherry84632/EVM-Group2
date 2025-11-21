package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "DealerModelPrice")
@IdClass(DealerModelPriceKey.class)
public class DTODealerModelPrice {
    @Id
    @Column(name = "DealerID")
    private Integer dealerID;
    @Id
    @Column(name = "ModelID")
    private Integer modelID;

    @Column(name = "DealerSellingPrice")
    private java.math.BigDecimal dealerSellingPrice;

    @Column(name = "UpdatedAt")
    private java.sql.Timestamp updatedAt;

    public DTODealerModelPrice() {}
    public DTODealerModelPrice(Integer dealerID, Integer modelID, java.math.BigDecimal price) {
        this.dealerID = dealerID; this.modelID = modelID; this.dealerSellingPrice = price; this.updatedAt = java.sql.Timestamp.from(Instant.now());
    }
    public Integer getDealerID(){return dealerID;} public void setDealerID(Integer d){this.dealerID=d;}
    public Integer getModelID(){return modelID;} public void setModelID(Integer m){this.modelID=m;}
    public java.math.BigDecimal getDealerSellingPrice(){return dealerSellingPrice;} public void setDealerSellingPrice(java.math.BigDecimal p){this.dealerSellingPrice=p;}
    public java.sql.Timestamp getUpdatedAt(){return updatedAt;} public void setUpdatedAt(java.sql.Timestamp t){this.updatedAt=t;}
}

class DealerModelPriceKey implements java.io.Serializable {
    private Integer dealerID; private Integer modelID;
    public DealerModelPriceKey() {}
    public DealerModelPriceKey(Integer d,Integer m){this.dealerID=d; this.modelID=m;}
    @Override public int hashCode(){ return java.util.Objects.hash(dealerID, modelID);}
    @Override public boolean equals(Object o){ if(this==o) return true; if(!(o instanceof DealerModelPriceKey k)) return false; return java.util.Objects.equals(dealerID,k.dealerID) && java.util.Objects.equals(modelID,k.modelID);}
}
