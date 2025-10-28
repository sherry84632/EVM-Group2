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

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isEmpty()) {
            response.sendRedirect("vehicleListController");
            return;
        }

        try {
            Integer id = Integer.parseInt(idParam);
            DAOVehicle dao = new DAOVehicle();
            boolean deleted = dao.deleteVehicle(id);

            if (deleted) {
                System.out.println("✅ Vehicle deleted: ID=" + id);
            } else {
                System.out.println("⚠️ Delete failed for ID: " + id);
            }
        } catch (NumberFormatException e) {
            System.out.println("⚠️ Invalid vehicle ID: " + idParam);
        }

        // Sau khi xóa xong, quay về danh sách xe
        response.sendRedirect("vehicleListController");
    }
}
