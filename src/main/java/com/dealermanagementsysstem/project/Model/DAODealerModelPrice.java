package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;
import java.sql.*;
import java.util.*;

@Repository
public class DAODealerModelPrice {

    public java.math.BigDecimal getPrice(int dealerId, int modelId){
        String sql="SELECT DealerSellingPrice FROM DealerModelPrice WHERE DealerID=? AND ModelID=?";
        try(Connection c=DBUtils.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){ ps.setInt(1,dealerId); ps.setInt(2,modelId); try(ResultSet rs=ps.executeQuery()){ if(rs.next()) return rs.getBigDecimal(1);} } catch(Exception ignored){}
        return null;
    }

    public boolean upsertPrice(int dealerId, int modelId, java.math.BigDecimal price){
        String sql="MERGE DealerModelPrice AS tgt USING (SELECT ? AS DealerID, ? AS ModelID) AS src ON tgt.DealerID=src.DealerID AND tgt.ModelID=src.ModelID " +
                "WHEN MATCHED THEN UPDATE SET DealerSellingPrice=?, UpdatedAt=SYSUTCDATETIME() " +
                "WHEN NOT MATCHED THEN INSERT(DealerID,ModelID,DealerSellingPrice,UpdatedAt) VALUES(src.DealerID,src.ModelID,?,SYSUTCDATETIME());";
        try(Connection c=DBUtils.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,dealerId); ps.setInt(2,modelId); ps.setBigDecimal(3,price); ps.setBigDecimal(4,price); return ps.executeUpdate()>0; }
        catch(Exception e){ e.printStackTrace(); }
        return false;
    }

    public List<Map<String,Object>> listDealerModels(int dealerId){
        String sql="SELECT DISTINCT vm.ModelID, vm.ModelName, vm.Brand, vm.Year, vm.BasePrice, dmp.DealerSellingPrice " +
                "FROM DealerInventory di " +
                "JOIN Vehicle v ON di.VehicleID=v.VehicleID " +
                "JOIN VehicleVersion vv ON v.VersionID=vv.VersionID " +
                "JOIN VehicleModel vm ON vv.ModelID=vm.ModelID " +
                "LEFT JOIN DealerModelPrice dmp ON dmp.ModelID=vm.ModelID AND dmp.DealerID=di.DealerID " +
                "WHERE di.DealerID=?";
        List<Map<String,Object>> list=new ArrayList<>();
        try(Connection c=DBUtils.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){ ps.setInt(1,dealerId); try(ResultSet rs=ps.executeQuery()){ while(rs.next()){ Map<String,Object> m=new HashMap<>(); m.put("modelID", rs.getInt("ModelID")); m.put("modelName", rs.getString("ModelName")); m.put("brand", rs.getString("Brand")); m.put("year", rs.getInt("Year")); m.put("basePrice", rs.getBigDecimal("BasePrice")); m.put("dealerPrice", rs.getBigDecimal("DealerSellingPrice")); list.add(m);} } } catch(Exception ignored){}
        return list;
    }

    public void propagateToInventory(int dealerId, int modelId, java.math.BigDecimal price){
        String sql="UPDATE v SET DealerSellingPrice=? FROM Vehicle v JOIN DealerInventory di ON di.VehicleID=v.VehicleID " +
                "JOIN VehicleVersion vv ON v.VersionID=vv.VersionID WHERE di.DealerID=? AND vv.ModelID=?";
        try(Connection c=DBUtils.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){ ps.setBigDecimal(1,price); ps.setInt(2,dealerId); ps.setInt(3,modelId); ps.executeUpdate(); } catch(Exception ignored){}
    }
}

