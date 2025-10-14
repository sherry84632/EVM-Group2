package com.dealermanagementsysstem.project.Model;

public class DTOModel {
    private String ModelID;
    private String ModelName;
    private int BasePrice;

    public DTOModel() {
    }

    public DTOModel(String modelID, int basePrice, String modelName) {
        ModelID = modelID;
        BasePrice = basePrice;
        ModelName = modelName;
    }

    public String getModelID() {
        return ModelID;
    }

    public void setModelID(String modelID) {
        ModelID = modelID;
    }

    public String getModelName() {
        return ModelName;
    }

    public void setModelName(String modelName) {
        ModelName = modelName;
    }

    public int getBasePrice() {
        return BasePrice;
    }

    public void setBasePrice(int basePrice) {
        BasePrice = basePrice;
    }
}
