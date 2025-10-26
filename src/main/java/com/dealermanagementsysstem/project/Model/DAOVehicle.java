package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DAOVehicle {

    public List<DTOVehicle> getVehicles() {
        List<DTOVehicle> vehicles = new ArrayList<>();

        String sql = """
            SELECT 
                v.VIN,
                v.ManufactureYear,
                v.EngineNumber,
                v.Status,
                v.CreatedAt,
                v.UpdatedAt,
                vc.ColorID,
                vc.ColorName,
                vv.VersionID,
                vv.VersionName,
                c.CustomerID,
                c.FullName AS CustomerName,
                d.DealerID,
                d.DealerName
            FROM Vehicle v
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
            LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            LEFT JOIN Customer c ON v.OwnerID = c.CustomerID
            LEFT JOIN Dealer d ON v.CurrentDealerID = d.DealerID
            ORDER BY v.CreatedAt DESC
        """;

        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOVehicle v = new DTOVehicle();
                v.setVIN(rs.getString("VIN"));
                v.setManufactureYear(rs.getInt("ManufactureYear"));
                v.setEngineNumber(rs.getString("EngineNumber"));
                v.setStatus(VehicleStatus.valueOf(rs.getString("Status")));
                v.setCreatedAt(rs.getTimestamp("CreatedAt"));
                v.setUpdatedAt(rs.getTimestamp("UpdatedAt"));

                // Set color relationship
                if (rs.getString("ColorName") != null) {
                    DTOVehicleColor color = new DTOVehicleColor();
                    color.setColorID(rs.getInt("ColorID"));
                    color.setColorName(rs.getString("ColorName"));
                    v.setColor(color);
                }

                // Set version relationship
                if (rs.getString("VersionName") != null) {
                    DTOVehicleVersion version = new DTOVehicleVersion();
                    version.setVersionID(rs.getInt("VersionID"));
                    version.setVersionName(rs.getString("VersionName"));
                    v.setVersion(version);
                }

                // Set owner relationship
                if (rs.getString("CustomerName") != null) {
                    DTOCustomer owner = new DTOCustomer();
                    owner.setCustomerID(rs.getInt("CustomerID"));
                    owner.setFullName(rs.getString("CustomerName"));
                    v.setOwner(owner);
                }

                // Set current dealer relationship
                if (rs.getString("DealerName") != null) {
                    DTODealer currentDealer = new DTODealer();
                    currentDealer.setDealerID(rs.getInt("DealerID"));
                    currentDealer.setDealerName(rs.getString("DealerName"));
                    v.setCurrentDealer(currentDealer);
                }

                vehicles.add(v);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return vehicles;
    }

    public List<DTOVehicle> searchVehiclesByModelName(String keyword) {
        List<DTOVehicle> vehicles = new ArrayList<>();
        String sql = """
            SELECT 
                v.VIN, ManufactureYear, EngineNumber, Status, CreatedAt, UpdatedAt,
                vc.ColorID, vc.ColorName,
                vv.VersionID, vv.VersionName,
                vm.ModelID, vm.ModelName,
                c.CustomerID, c.FullName AS CustomerName,
                d.DealerID, d.DealerName
            FROM Vehicle v
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
            LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            LEFT JOIN Customer c ON v.OwnerID = c.CustomerID
            LEFT JOIN Dealer d ON v.CurrentDealerID = d.DealerID
            WHERE vm.ModelName LIKE ?
            ORDER BY v.CreatedAt DESC
        """;

        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOVehicle v = new DTOVehicle();
                    v.setVIN(rs.getString("VIN"));
                    v.setManufactureYear(rs.getInt("ManufactureYear"));
                    v.setEngineNumber(rs.getString("EngineNumber"));
                    v.setStatus(VehicleStatus.valueOf(rs.getString("Status")));
                    v.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    v.setUpdatedAt(rs.getTimestamp("UpdatedAt"));

                    // Set color relationship
                    if (rs.getString("ColorName") != null) {
                        DTOVehicleColor color = new DTOVehicleColor();
                        color.setColorID(rs.getInt("ColorID"));
                        color.setColorName(rs.getString("ColorName"));
                        v.setColor(color);
                    }

                    // Set version relationship
                    if (rs.getString("VersionName") != null) {
                        DTOVehicleVersion version = new DTOVehicleVersion();
                        version.setVersionID(rs.getInt("VersionID"));
                        version.setVersionName(rs.getString("VersionName"));
                        v.setVersion(version);
                    }

                    // Set owner relationship
                    if (rs.getString("CustomerName") != null) {
                        DTOCustomer owner = new DTOCustomer();
                        owner.setCustomerID(rs.getInt("CustomerID"));
                        owner.setFullName(rs.getString("CustomerName"));
                        v.setOwner(owner);
                    }

                    // Set current dealer relationship
                    if (rs.getString("DealerName") != null) {
                        DTODealer currentDealer = new DTODealer();
                        currentDealer.setDealerID(rs.getInt("DealerID"));
                        currentDealer.setDealerName(rs.getString("DealerName"));
                        v.setCurrentDealer(currentDealer);
                    }

                    vehicles.add(v);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return vehicles;
    }

    public void insertVehicle(DTOVehicle v) {
        String sql = "INSERT INTO Vehicle (VIN, ColorID, VersionID, ManufactureYear, EngineNumber, OwnerID, CurrentDealerID, Status, CreatedAt, UpdatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setString(1, v.getVIN());
            ps.setInt(2, v.getColor() != null ? v.getColor().getColorID() : null);
            ps.setInt(3, v.getVersion() != null ? v.getVersion().getVersionID() : null);
            ps.setInt(4, v.getManufactureYear());
            ps.setString(5, v.getEngineNumber());
            ps.setInt(6, v.getOwner() != null ? v.getOwner().getCustomerID() : null);
            ps.setInt(7, v.getCurrentDealer() != null ? v.getCurrentDealer().getDealerID() : null);
            ps.setString(8, v.getStatus().toString());
            ps.setTimestamp(9, v.getCreatedAt());
            ps.setTimestamp(10, v.getUpdatedAt());
            ps.executeUpdate();
            System.out.println("✅ Vehicle inserted successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Integer getModelIdByName(String modelName) {
        String sql = "SELECT ModelID FROM VehicleModel WHERE ModelName = ?";
        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setString(1, modelName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("ModelID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Integer getColorIdByName(String colorName) {
        String sql = "SELECT ColorID FROM VehicleColor WHERE ColorName = ?";
        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setString(1, colorName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("ColorID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DTOVehicle getVehicleByVIN(String vin) {
        DTOVehicle v = null;
        String sql = """
            SELECT 
                v.VIN, v.ManufactureYear, v.EngineNumber, v.Status, v.CreatedAt, v.UpdatedAt,
                vc.ColorID, vc.ColorName,
                vv.VersionID, vv.VersionName,
                c.CustomerID, c.FullName AS CustomerName,
                d.DealerID, d.DealerName
            FROM Vehicle v
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
            LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            LEFT JOIN Customer c ON v.OwnerID = c.CustomerID
            LEFT JOIN Dealer d ON v.CurrentDealerID = d.DealerID
            WHERE v.VIN = ?
        """;
        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setString(1, vin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    v = new DTOVehicle();
                    v.setVIN(rs.getString("VIN"));
                    v.setManufactureYear(rs.getInt("ManufactureYear"));
                    v.setEngineNumber(rs.getString("EngineNumber"));
                    v.setStatus(VehicleStatus.valueOf(rs.getString("Status")));
                    v.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    v.setUpdatedAt(rs.getTimestamp("UpdatedAt"));

                    // Set color relationship
                    if (rs.getString("ColorName") != null) {
                        DTOVehicleColor color = new DTOVehicleColor();
                        color.setColorID(rs.getInt("ColorID"));
                        color.setColorName(rs.getString("ColorName"));
                        v.setColor(color);
                    }

                    // Set version relationship
                    if (rs.getString("VersionName") != null) {
                        DTOVehicleVersion version = new DTOVehicleVersion();
                        version.setVersionID(rs.getInt("VersionID"));
                        version.setVersionName(rs.getString("VersionName"));
                        v.setVersion(version);
                    }

                    // Set owner relationship
                    if (rs.getString("CustomerName") != null) {
                        DTOCustomer owner = new DTOCustomer();
                        owner.setCustomerID(rs.getInt("CustomerID"));
                        owner.setFullName(rs.getString("CustomerName"));
                        v.setOwner(owner);
                    }

                    // Set current dealer relationship
                    if (rs.getString("DealerName") != null) {
                        DTODealer currentDealer = new DTODealer();
                        currentDealer.setDealerID(rs.getInt("DealerID"));
                        currentDealer.setDealerName(rs.getString("DealerName"));
                        v.setCurrentDealer(currentDealer);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return v;
    }

    public boolean updateVehicle(DTOVehicle v) {
        String sql = "UPDATE Vehicle SET ColorID=?, VersionID=?, ManufactureYear=?, EngineNumber=?, " +
                "OwnerID=?, CurrentDealerID=?, Status=?, UpdatedAt=? WHERE VIN=?";
        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setInt(1, v.getColor() != null ? v.getColor().getColorID() : null);
            ps.setInt(2, v.getVersion() != null ? v.getVersion().getVersionID() : null);
            ps.setInt(3, v.getManufactureYear());
            ps.setString(4, v.getEngineNumber());
            ps.setInt(5, v.getOwner() != null ? v.getOwner().getCustomerID() : null);
            ps.setInt(6, v.getCurrentDealer() != null ? v.getCurrentDealer().getDealerID() : null);
            ps.setString(7, v.getStatus().toString());
            ps.setTimestamp(8, v.getUpdatedAt());
            ps.setString(9, v.getVIN());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteVehicle(String vin) {
        String sql = "DELETE FROM Vehicle WHERE VIN = ?";
        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setString(1, vin);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<DTOVehicle> getAllVehicles() {
        return getVehicles();
    }

    // ✅ Get vehicles by status
    public List<DTOVehicle> getVehiclesByStatus(VehicleStatus status) {
        List<DTOVehicle> vehicles = new ArrayList<>();
        String sql = """
            SELECT 
                v.VIN, v.ManufactureYear, v.EngineNumber, v.Status, v.CreatedAt, v.UpdatedAt,
                vc.ColorID, vc.ColorName,
                vv.VersionID, vv.VersionName,
                c.CustomerID, c.FullName AS CustomerName,
                d.DealerID, d.DealerName
            FROM Vehicle v
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
            LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            LEFT JOIN Customer c ON v.OwnerID = c.CustomerID
            LEFT JOIN Dealer d ON v.CurrentDealerID = d.DealerID
            WHERE v.Status = ?
            ORDER BY v.CreatedAt DESC
        """;

        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setString(1, status.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOVehicle v = new DTOVehicle();
                    v.setVIN(rs.getString("VIN"));
                    v.setManufactureYear(rs.getInt("ManufactureYear"));
                    v.setEngineNumber(rs.getString("EngineNumber"));
                    v.setStatus(VehicleStatus.valueOf(rs.getString("Status")));
                    v.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    v.setUpdatedAt(rs.getTimestamp("UpdatedAt"));

                    // Set relationships (same pattern as other methods)
                    if (rs.getString("ColorName") != null) {
                        DTOVehicleColor color = new DTOVehicleColor();
                        color.setColorID(rs.getInt("ColorID"));
                        color.setColorName(rs.getString("ColorName"));
                        v.setColor(color);
                    }

                    if (rs.getString("VersionName") != null) {
                        DTOVehicleVersion version = new DTOVehicleVersion();
                        version.setVersionID(rs.getInt("VersionID"));
                        version.setVersionName(rs.getString("VersionName"));
                        v.setVersion(version);
                    }

                    if (rs.getString("CustomerName") != null) {
                        DTOCustomer owner = new DTOCustomer();
                        owner.setCustomerID(rs.getInt("CustomerID"));
                        owner.setFullName(rs.getString("CustomerName"));
                        v.setOwner(owner);
                    }

                    if (rs.getString("DealerName") != null) {
                        DTODealer currentDealer = new DTODealer();
                        currentDealer.setDealerID(rs.getInt("DealerID"));
                        currentDealer.setDealerName(rs.getString("DealerName"));
                        v.setCurrentDealer(currentDealer);
                    }

                    vehicles.add(v);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return vehicles;
    }

    // ✅ Get vehicles by dealer
    public List<DTOVehicle> getVehiclesByDealer(int dealerID) {
        List<DTOVehicle> vehicles = new ArrayList<>();
        String sql = """
            SELECT 
                v.VIN, v.ManufactureYear, v.EngineNumber, v.Status, v.CreatedAt, v.UpdatedAt,
                vc.ColorID, vc.ColorName,
                vv.VersionID, vv.VersionName,
                c.CustomerID, c.FullName AS CustomerName,
                d.DealerID, d.DealerName
            FROM Vehicle v
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
            LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            LEFT JOIN Customer c ON v.OwnerID = c.CustomerID
            LEFT JOIN Dealer d ON v.CurrentDealerID = d.DealerID
            WHERE v.CurrentDealerID = ?
            ORDER BY v.CreatedAt DESC
        """;

        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setInt(1, dealerID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOVehicle v = new DTOVehicle();
                    v.setVIN(rs.getString("VIN"));
                    v.setManufactureYear(rs.getInt("ManufactureYear"));
                    v.setEngineNumber(rs.getString("EngineNumber"));
                    v.setStatus(VehicleStatus.valueOf(rs.getString("Status")));
                    v.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    v.setUpdatedAt(rs.getTimestamp("UpdatedAt"));

                    // Set relationships (same pattern as other methods)
                    if (rs.getString("ColorName") != null) {
                        DTOVehicleColor color = new DTOVehicleColor();
                        color.setColorID(rs.getInt("ColorID"));
                        color.setColorName(rs.getString("ColorName"));
                        v.setColor(color);
                    }

                    if (rs.getString("VersionName") != null) {
                        DTOVehicleVersion version = new DTOVehicleVersion();
                        version.setVersionID(rs.getInt("VersionID"));
                        version.setVersionName(rs.getString("VersionName"));
                        v.setVersion(version);
                    }

                    if (rs.getString("CustomerName") != null) {
                        DTOCustomer owner = new DTOCustomer();
                        owner.setCustomerID(rs.getInt("CustomerID"));
                        owner.setFullName(rs.getString("CustomerName"));
                        v.setOwner(owner);
                    }

                    if (rs.getString("DealerName") != null) {
                        DTODealer currentDealer = new DTODealer();
                        currentDealer.setDealerID(rs.getInt("DealerID"));
                        currentDealer.setDealerName(rs.getString("DealerName"));
                        v.setCurrentDealer(currentDealer);
                    }

                    vehicles.add(v);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return vehicles;
    }
}
