package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;

import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

@Repository
public class DAODealerRewardSettlement {

    public DTODealerRewardSettlement getByDealerAndPeriod(int dealerId, int year, int month){
        String sql = "SELECT * FROM DealerRewardSettlement WHERE DealerID=? AND PeriodYear=? AND PeriodMonth=?";
        try(Connection c=DBUtils.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,dealerId); ps.setInt(2,year); ps.setInt(3,month);
            try(ResultSet rs=ps.executeQuery()){ if(rs.next()) return map(rs); }
        } catch(Exception e){ e.printStackTrace(); }
        return null;
    }
    public DTODealerRewardSettlement create(int dealerId, int year, int month, int importedQty, BigDecimal rewardPercent, BigDecimal rewardAmount){
        String sql = "INSERT INTO DealerRewardSettlement(DealerID,PeriodYear,PeriodMonth,ImportedQuantity,RewardPercent,RewardAmount,Status) VALUES(?,?,?,?,?,?,?)";
        try(Connection c=DBUtils.getConnection(); PreparedStatement ps=c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            ps.setInt(1,dealerId); ps.setInt(2,year); ps.setInt(3,month); ps.setInt(4,importedQty);
            ps.setBigDecimal(5,rewardPercent); ps.setBigDecimal(6,rewardAmount); ps.setString(7,"PENDING");
            if(ps.executeUpdate()>0){ try(ResultSet rs=ps.getGeneratedKeys()){ if(rs.next()) return getById(rs.getInt(1)); } }
        } catch(Exception e){ e.printStackTrace(); }
        return null;
    }
    public DTODealerRewardSettlement getById(int id){
        String sql="SELECT * FROM DealerRewardSettlement WHERE RewardSettlementID=?";
        try(Connection c=DBUtils.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,id); try(ResultSet rs=ps.executeQuery()){ if(rs.next()) return map(rs);} }
        catch(Exception e){ e.printStackTrace(); }
        return null;
    }
    public List<DTODealerRewardSettlement> filter(Integer dealerId, Integer year, Integer month, String status){
        StringBuilder sb=new StringBuilder("SELECT * FROM DealerRewardSettlement WHERE 1=1");
        if(dealerId!=null){ sb.append(" AND DealerID=").append(dealerId); }
        if(year!=null){ sb.append(" AND PeriodYear=").append(year); }
        if(month!=null){ sb.append(" AND PeriodMonth=").append(month); }
        if(status!=null && !status.isBlank()){ sb.append(" AND Status='").append(status.replace("'","''")).append("'"); }
        sb.append(" ORDER BY PeriodYear DESC, PeriodMonth DESC, DealerID");
        List<DTODealerRewardSettlement> list=new ArrayList<>();
        try(Connection c=DBUtils.getConnection(); Statement st=c.createStatement(); ResultSet rs=st.executeQuery(sb.toString())){
            while(rs.next()) list.add(map(rs));
        } catch(Exception e){ e.printStackTrace(); }
        return list;
    }
    public DTODealerRewardSettlement updateStatus(int id, String status, BigDecimal rewardAmount, String notes){
        String sql="UPDATE DealerRewardSettlement SET Status=?, RewardAmount=?, UpdatedAt=GETDATE(), PaidDate=CASE WHEN ?='PAID' THEN GETDATE() ELSE PaidDate END, Notes=? WHERE RewardSettlementID=?";
        try(Connection c=DBUtils.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,status); ps.setBigDecimal(2,rewardAmount); ps.setString(3,status); ps.setString(4,notes); ps.setInt(5,id);
            if(ps.executeUpdate()>0) return getById(id);
        } catch(Exception e){ e.printStackTrace(); }
        return null;
    }
    public DTODealerRewardSettlement getLatestForPeriod(int dealerId,int year,int month){
        String sql="SELECT TOP 1 * FROM DealerRewardSettlement WHERE DealerID=? AND PeriodYear=? AND PeriodMonth=? ORDER BY RewardSettlementID DESC";
        try(Connection c=DBUtils.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){ ps.setInt(1,dealerId); ps.setInt(2,year); ps.setInt(3,month); try(ResultSet rs=ps.executeQuery()){ if(rs.next()) return map(rs);} } catch(Exception e){ e.printStackTrace(); }
        return null;
    }
    public DTODealerRewardSettlement createNewEvenIfPeriodExists(int dealerId,int year,int month,int importedQty,BigDecimal rewardPercent,BigDecimal rewardAmount){
        return create(dealerId,year,month,importedQty,rewardPercent,rewardAmount);
    }
    public DTODealerRewardSettlement safeUpdateStatusAndAmount(int id,String status,BigDecimal rewardAmount,String notes){
        DTODealerRewardSettlement current=getById(id);
        if(current==null) return null;
        if("PAID".equalsIgnoreCase(current.getStatus())){
            // If already paid, do not modify amount or status
            return current;
        }
        return updateStatus(id,status,rewardAmount,notes);
    }
    private DTODealerRewardSettlement map(ResultSet rs) throws SQLException {
        DTODealerRewardSettlement d=new DTODealerRewardSettlement();
        d.setRewardSettlementID(rs.getInt("RewardSettlementID"));
        d.setDealerID(rs.getInt("DealerID"));
        d.setPeriodYear(rs.getInt("PeriodYear"));
        d.setPeriodMonth(rs.getInt("PeriodMonth"));
        d.setImportedQuantity(rs.getInt("ImportedQuantity"));
        d.setRewardPercent(rs.getBigDecimal("RewardPercent"));
        d.setRewardAmount(rs.getBigDecimal("RewardAmount"));
        d.setStatus(rs.getString("Status"));
        d.setNotes(rs.getString("Notes"));
        d.setCreatedAt(rs.getTimestamp("CreatedAt"));
        d.setUpdatedAt(rs.getTimestamp("UpdatedAt"));
        d.setPaidDate(rs.getTimestamp("PaidDate"));
        return d;
    }
}
