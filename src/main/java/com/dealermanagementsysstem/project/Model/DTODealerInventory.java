package com.dealermanagementsysstem.project.Model;



import java.util.Date;

public class DTODealerInventory {
    private int dealerId;
    private String vin;
    private Date receivedDate;
    private String status;

    public int getDealerId() { return dealerId; }
    public void setDealerId(int dealerId) { this.dealerId = dealerId; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public Date getReceivedDate() { return receivedDate; }
    public void setReceivedDate(Date receivedDate) { this.receivedDate = receivedDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}


