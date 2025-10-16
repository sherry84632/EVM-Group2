package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOVehicle;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/deleteVehicle")
public class DeleteVehicleController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String vin = request.getParameter("vin");
        if (vin == null || vin.isEmpty()) {
            response.sendRedirect("vehicleListController");
            return;
        }

        DAOVehicle dao = new DAOVehicle();
        boolean deleted = dao.deleteVehicle(vin);

        if (deleted) {
            System.out.println("✅ Vehicle deleted: " + vin);
        } else {
            System.out.println("⚠️ Delete failed for VIN: " + vin);
        }

        // Sau khi xóa xong, quay về danh sách xe
        response.sendRedirect("vehicleListController");
    }
}
