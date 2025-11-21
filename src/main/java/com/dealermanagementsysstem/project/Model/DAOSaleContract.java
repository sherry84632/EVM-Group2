package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DAOSaleContract {

    //  Lấy danh sách SaleContract
    public List<DTOSaleContract> getAllSaleContracts() {
        List<DTOSaleContract> list = new ArrayList<>();
        String sql = """
            SELECT sc.ContractID, sc.SaleOrderID, sc.ContractDate, sc.Status, sc.TotalAmount,
                   sc.RegistrationFee, sc.DeliveryFee, sc.InsuranceFee, sc.ServiceFee, sc.Terms,
                   sc.CustomerAddressSnapshot, sc.CustomerIdNumber, sc.DealerSignatureName, sc.CustomerSignatureName,
                   sc.SignedDate, sc.SignStatus
            FROM SaleContract sc
            ORDER BY sc.ContractDate DESC
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOSaleContract contract = new DTOSaleContract();
                contract.setContractID(rs.getInt("ContractID"));
                java.sql.Date contractDate = rs.getDate("ContractDate");
                contract.setContractDate(contractDate != null ? new java.util.Date(contractDate.getTime()) : null);
                contract.setStatus(SaleContractStatus.valueOf(rs.getString("Status")));
                contract.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                contract.setRegistrationFee(rs.getBigDecimal("RegistrationFee"));
                contract.setDeliveryFee(rs.getBigDecimal("DeliveryFee"));
                contract.setInsuranceFee(rs.getBigDecimal("InsuranceFee"));
                contract.setServiceFee(rs.getBigDecimal("ServiceFee"));
                contract.setTerms(rs.getString("Terms"));
                contract.setCustomerAddressSnapshot(rs.getString("CustomerAddressSnapshot"));
                contract.setCustomerIdNumber(rs.getString("CustomerIdNumber"));
                contract.setDealerSignatureName(rs.getString("DealerSignatureName"));
                contract.setCustomerSignatureName(rs.getString("CustomerSignatureName"));
                contract.setSignedDate(rs.getTimestamp("SignedDate"));
                String signStatus = rs.getString("SignStatus");
                if(signStatus!=null) contract.setSignStatus(ContractSignStatus.valueOf(signStatus));

                // SaleOrder info
                DTOSaleOrder saleOrder = new DTOSaleOrder();
                saleOrder.setSaleOrderID(rs.getInt("SaleOrderID"));
                contract.setSaleOrder(saleOrder);

                list.add(contract);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    //  Lấy SaleContract theo ID
    public DTOSaleContract getSaleContractById(int contractID) {
        String sql = """
            SELECT sc.ContractID, sc.SaleOrderID, sc.ContractDate, sc.Status, sc.TotalAmount,
                   sc.RegistrationFee, sc.DeliveryFee, sc.InsuranceFee, sc.ServiceFee, sc.Terms,
                   sc.CustomerAddressSnapshot, sc.CustomerIdNumber, sc.DealerSignatureName, sc.CustomerSignatureName,
                   sc.SignedDate, sc.SignStatus
            FROM SaleContract sc
            WHERE sc.ContractID = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, contractID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOSaleContract contract = new DTOSaleContract();
                    contract.setContractID(rs.getInt("ContractID"));
                    java.sql.Date contractDate = rs.getDate("ContractDate");
                contract.setContractDate(contractDate != null ? new java.util.Date(contractDate.getTime()) : null);
                    contract.setStatus(SaleContractStatus.valueOf(rs.getString("Status")));
                    contract.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                    contract.setRegistrationFee(rs.getBigDecimal("RegistrationFee"));
                    contract.setDeliveryFee(rs.getBigDecimal("DeliveryFee"));
                    contract.setInsuranceFee(rs.getBigDecimal("InsuranceFee"));
                    contract.setServiceFee(rs.getBigDecimal("ServiceFee"));
                    contract.setTerms(rs.getString("Terms"));
                    contract.setCustomerAddressSnapshot(rs.getString("CustomerAddressSnapshot"));
                    contract.setCustomerIdNumber(rs.getString("CustomerIdNumber"));
                    contract.setDealerSignatureName(rs.getString("DealerSignatureName"));
                    contract.setCustomerSignatureName(rs.getString("CustomerSignatureName"));
                    contract.setSignedDate(rs.getTimestamp("SignedDate"));
                    String signStatus = rs.getString("SignStatus");
                    if(signStatus!=null) contract.setSignStatus(ContractSignStatus.valueOf(signStatus));

                    // SaleOrder info
                    DTOSaleOrder saleOrder = new DTOSaleOrder();
                    saleOrder.setSaleOrderID(rs.getInt("SaleOrderID"));
                    contract.setSaleOrder(saleOrder);

                    return contract;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //  Tạo SaleContract mới
    public boolean createSaleContract(DTOSaleContract contract) {
        String sql = "INSERT INTO SaleContract (SaleOrderID, ContractDate, Status, TotalAmount, RegistrationFee, DeliveryFee, InsuranceFee, ServiceFee, Terms, CustomerAddressSnapshot, CustomerIdNumber, SignStatus) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, contract.getSaleOrder().getSaleOrderID());
            ps.setDate(2, new java.sql.Date(contract.getContractDate().getTime()));
            ps.setString(3, contract.getStatus().toString());
            ps.setBigDecimal(4, contract.getTotalAmount());
            ps.setBigDecimal(5, contract.getRegistrationFee()!=null? contract.getRegistrationFee(): java.math.BigDecimal.ZERO);
            ps.setBigDecimal(6, contract.getDeliveryFee()!=null? contract.getDeliveryFee(): java.math.BigDecimal.ZERO);
            ps.setBigDecimal(7, contract.getInsuranceFee()!=null? contract.getInsuranceFee(): java.math.BigDecimal.ZERO);
            ps.setBigDecimal(8, contract.getServiceFee()!=null? contract.getServiceFee(): java.math.BigDecimal.ZERO);
            ps.setString(9, contract.getTerms());
            ps.setString(10, contract.getCustomerAddressSnapshot());
            ps.setString(11, contract.getCustomerIdNumber());
            ps.setString(12, contract.getSignStatus()!=null? contract.getSignStatus().name(): ContractSignStatus.DRAFT.name());

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //  Cập nhật trạng thái SaleContract
    public boolean updateSaleContractStatus(int contractID, SaleContractStatus status) {
        String sql = "UPDATE SaleContract SET Status = ? WHERE ContractID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.toString());
            ps.setInt(2, contractID);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //  Lấy SaleContract theo SaleOrderID
    public DTOSaleContract getSaleContractBySaleOrderId(int saleOrderID) {
        String sql = """
            SELECT sc.ContractID, sc.SaleOrderID, sc.ContractDate, sc.Status, sc.TotalAmount,
                   sc.RegistrationFee, sc.DeliveryFee, sc.InsuranceFee, sc.ServiceFee, sc.Terms,
                   sc.CustomerAddressSnapshot, sc.CustomerIdNumber, sc.DealerSignatureName, sc.CustomerSignatureName,
                   sc.SignedDate, sc.SignStatus
            FROM SaleContract sc
            WHERE sc.SaleOrderID = ?
        """;
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleOrderID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOSaleContract c = new DTOSaleContract();
                    c.setContractID(rs.getInt("ContractID"));
                    java.sql.Date contractDate = rs.getDate("ContractDate");
                    c.setContractDate(contractDate != null ? new java.util.Date(contractDate.getTime()) : null);
                    c.setStatus(SaleContractStatus.valueOf(rs.getString("Status")));
                    c.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                    c.setRegistrationFee(rs.getBigDecimal("RegistrationFee"));
                    c.setDeliveryFee(rs.getBigDecimal("DeliveryFee"));
                    c.setInsuranceFee(rs.getBigDecimal("InsuranceFee"));
                    c.setServiceFee(rs.getBigDecimal("ServiceFee"));
                    c.setTerms(rs.getString("Terms"));
                    c.setCustomerAddressSnapshot(rs.getString("CustomerAddressSnapshot"));
                    c.setCustomerIdNumber(rs.getString("CustomerIdNumber"));
                    c.setDealerSignatureName(rs.getString("DealerSignatureName"));
                    c.setCustomerSignatureName(rs.getString("CustomerSignatureName"));
                    c.setSignedDate(rs.getTimestamp("SignedDate"));
                    String signStatus = rs.getString("SignStatus");
                    if(signStatus!=null) c.setSignStatus(ContractSignStatus.valueOf(signStatus));
                    DTOSaleOrder so = new DTOSaleOrder(); so.setSaleOrderID(rs.getInt("SaleOrderID")); c.setSaleOrder(so);
                    return c;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Delete a contract by its ContractID */
    public boolean deleteContract(int contractID) {
        String sql = "DELETE FROM SaleContract WHERE ContractID=?";
        try (java.sql.Connection conn = DBUtils.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contractID); return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
    /** Delete all contracts referencing a sale order (for cascade manual) */
    public int deleteContractsBySaleOrderID(int saleOrderID) {
        String sql = "DELETE FROM SaleContract WHERE SaleOrderID=?";
        try (java.sql.Connection conn = DBUtils.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleOrderID); return ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); return 0; }
    }

    public boolean updateFeesAndTerms(int contractID, java.math.BigDecimal reg, java.math.BigDecimal del, java.math.BigDecimal ins, java.math.BigDecimal svc, String terms){
        String sql = "UPDATE SaleContract SET RegistrationFee=?, DeliveryFee=?, InsuranceFee=?, ServiceFee=?, Terms=? WHERE ContractID=?";
        try(Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setBigDecimal(1, reg!=null?reg: java.math.BigDecimal.ZERO);
            ps.setBigDecimal(2, del!=null?del: java.math.BigDecimal.ZERO);
            ps.setBigDecimal(3, ins!=null?ins: java.math.BigDecimal.ZERO);
            ps.setBigDecimal(4, svc!=null?svc: java.math.BigDecimal.ZERO);
            ps.setString(5, terms);
            ps.setInt(6, contractID);
            return ps.executeUpdate()>0;
        } catch(SQLException e){ e.printStackTrace(); }
        return false;
    }

    public boolean updateSignatures(int contractID, String dealerSig, String customerSig){
        // Determine new sign status
        ContractSignStatus status;
        boolean dealerPresent = dealerSig!=null && !dealerSig.isBlank();
        boolean customerPresent = customerSig!=null && !customerSig.isBlank();
        if(dealerPresent && customerPresent) status = ContractSignStatus.FULLY_SIGNED;
        else if(dealerPresent) status = ContractSignStatus.DEALER_SIGNED;
        else if(customerPresent) status = ContractSignStatus.CUSTOMER_SIGNED;
        else status = ContractSignStatus.DRAFT;
        String sql = "UPDATE SaleContract SET DealerSignatureName=?, CustomerSignatureName=?, SignStatus=?, SignedDate=? WHERE ContractID=?";
        try(Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, dealerSig);
            ps.setString(2, customerSig);
            ps.setString(3, status.name());
            if(status==ContractSignStatus.FULLY_SIGNED) ps.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis())); else ps.setNull(4, java.sql.Types.TIMESTAMP);
            ps.setInt(5, contractID);
            return ps.executeUpdate()>0;
        } catch(SQLException e){ e.printStackTrace(); }
        return false;
    }
}
