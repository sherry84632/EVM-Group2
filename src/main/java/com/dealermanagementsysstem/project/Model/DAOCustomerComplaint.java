package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DAOCustomerComplaint {

    private DTOCustomerComplaint map(ResultSet rs) throws SQLException {
        DTOCustomerComplaint c = new DTOCustomerComplaint();
        c.setComplaintID(rs.getInt("ComplaintID"));
        int dealerId = rs.getInt("DealerID");
        if (!rs.wasNull()) { DTODealer d = new DTODealer(); d.setDealerID(dealerId); c.setDealer(d);}
        DTOCustomer cust = new DTOCustomer(); cust.setCustomerID(rs.getInt("CustomerID")); cust.setFullName(rs.getString("CustomerName")); c.setCustomer(cust);
        Date cd = rs.getDate("ComplaintDate"); if (cd!=null) c.setComplaintDate(cd.toLocalDate());
        c.setStatus(rs.getString("Status"));
        c.setNote(rs.getString("Note"));
        c.setCreatedAt(rs.getTimestamp("CreatedAt"));
        c.setUpdatedAt(rs.getTimestamp("UpdatedAt"));
        return c;
    }

    public List<DTOCustomerComplaint> getComplaintsByDealer(int dealerId){
        List<DTOCustomerComplaint> list = new ArrayList<>();
        String sql = "SELECT cc.*, cu.FullName AS CustomerName FROM CustomerComplaint cc JOIN Customer cu ON cc.CustomerID = cu.CustomerID WHERE cc.DealerID = ? ORDER BY cc.ComplaintDate DESC, cc.ComplaintID DESC";
        try(Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1,dealerId);
            try(ResultSet rs = ps.executeQuery()){ while(rs.next()) list.add(map(rs)); }
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

    public DTOCustomerComplaint getById(int id){
        String sql = "SELECT cc.*, cu.FullName AS CustomerName FROM CustomerComplaint cc JOIN Customer cu ON cc.CustomerID = cu.CustomerID WHERE cc.ComplaintID = ?";
        try(Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1,id); try(ResultSet rs=ps.executeQuery()){ if(rs.next()) return map(rs);} }
        catch(SQLException e){ e.printStackTrace(); }
        return null;
    }

    public int insertComplaint(DTOCustomerComplaint c){
        String sql = "INSERT INTO CustomerComplaint(DealerID, CustomerID, ComplaintDate, Status, Note, CreatedAt) VALUES(?,?,?,?,?,GETDATE())";
        try(Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            if(c.getDealer()!=null) ps.setInt(1,c.getDealer().getDealerID()); else ps.setNull(1, Types.INTEGER);
            ps.setInt(2,c.getCustomer().getCustomerID());
            ps.setDate(3, c.getComplaintDate()!=null? Date.valueOf(c.getComplaintDate()): Date.valueOf(LocalDate.now()));
            ps.setString(4, c.getStatus()!=null? c.getStatus(): "APPROVED");
            ps.setString(5, c.getNote());
            int rows = ps.executeUpdate();
            if(rows>0){ try(ResultSet rs=ps.getGeneratedKeys()){ if(rs.next()) return rs.getInt(1);} }
        } catch(SQLException e){ e.printStackTrace(); }
        return -1;
    }

    public boolean updateComplaint(DTOCustomerComplaint c){
        String sql = "UPDATE CustomerComplaint SET DealerID=?, CustomerID=?, ComplaintDate=?, Status=?, Note=?, UpdatedAt=GETDATE() WHERE ComplaintID=?";
        try(Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            if(c.getDealer()!=null) ps.setInt(1,c.getDealer().getDealerID()); else ps.setNull(1, Types.INTEGER);
            ps.setInt(2,c.getCustomer().getCustomerID());
            ps.setDate(3, c.getComplaintDate()!=null? Date.valueOf(c.getComplaintDate()): Date.valueOf(LocalDate.now()));
            ps.setString(4, c.getStatus());
            ps.setString(5, c.getNote());
            ps.setInt(6,c.getComplaintID());
            return ps.executeUpdate()>0;
        } catch(SQLException e){ e.printStackTrace(); }
        return false;
    }

    public boolean deleteComplaint(int id){
        String sql = "DELETE FROM CustomerComplaint WHERE ComplaintID=?";
        try(Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1,id); return ps.executeUpdate()>0;
        } catch(SQLException e){ e.printStackTrace(); }
        return false;
    }

    public List<DTOCustomerComplaint> searchComplaints(int dealerId, String keyword){
        List<DTOCustomerComplaint> list = new ArrayList<>();
        String sql = "SELECT cc.*, cu.FullName AS CustomerName FROM CustomerComplaint cc JOIN Customer cu ON cc.CustomerID = cu.CustomerID WHERE cc.DealerID=? AND (cu.FullName LIKE ? OR cc.Note LIKE ?) ORDER BY cc.ComplaintDate DESC";
        try(Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1,dealerId); String pattern = "%"+keyword+"%"; ps.setString(2,pattern); ps.setString(3,pattern);
            try(ResultSet rs=ps.executeQuery()){ while(rs.next()) list.add(map(rs)); }
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }
}

