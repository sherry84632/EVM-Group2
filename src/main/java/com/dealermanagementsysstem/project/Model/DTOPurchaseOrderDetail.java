package com.dealermanagementsysstem.project.Model;

public class DTOPurchaseOrderDetail {
    private int poDetailId;
    private int purchaseOrderId;
    private int modelId;      // Xe gì
    private int colorId;      // Màu gì
    private int quantity;     // Bao nhiêu xe

    private String modelName; // để hiển thị
    private String colorName;

    public DTOPurchaseOrderDetail() {}

    public DTOPurchaseOrderDetail(int poDetailId, int purchaseOrderId, int modelId, int colorId, int quantity) {
        this.poDetailId = poDetailId;
        this.purchaseOrderId = purchaseOrderId;
        this.modelId = modelId;
        this.colorId = colorId;
        this.quantity = quantity;
    }

    // --- GETTER & SETTER ---
    public int getPoDetailId() {
        return poDetailId;
    }

    public void setPoDetailId(int poDetailId) {
        this.poDetailId = poDetailId;
    }

    public int getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(int purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public int getModelId() {
        return modelId;
    }

    public void setModelId(int modelId) {
        this.modelId = modelId;
    }

    public int getColorId() {
        return colorId;
    }

    public void setColorId(int colorId) {
        this.colorId = colorId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }

    @Override
    public String toString() {
        return "DTOPurchaseOrderDetail{" +
                "poDetailId=" + poDetailId +
                ", purchaseOrderId=" + purchaseOrderId +
                ", modelId=" + modelId +
                ", colorId=" + colorId +
                ", quantity=" + quantity +
                ", modelName='" + modelName + '\'' +
                ", colorName='" + colorName + '\'' +
                '}';
    }
}
