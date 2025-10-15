package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@WebServlet("/customer")
public class CustomerController extends HttpServlet {
    private DAOCustomer dao;

    @Override
    public void init() {
        dao = new DAOCustomer();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null) action = "list";

        try {
            switch (action) {
                case "new":
                    showNewForm(request, response);
                    break;
                case "insert":
                    insertCustomer(request, response);
                    break;
                case "edit":
                    showEditForm(request, response);
                    break;
                case "update":
                    updateCustomer(request, response);
                    break;
                case "delete":
                    deleteCustomer(request, response);
                    break;
                default:
                    listCustomers(request, response);
                    break;
            }
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    // ✅ Danh sách khách hàng
    private void listCustomers(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<DTOCustomer> list = dao.getAllCustomers();
        request.setAttribute("customers", list);
        RequestDispatcher dispatcher = request.getRequestDispatcher("customer-list.jsp");
        dispatcher.forward(request, response);
    }

    // ✅ Hiển thị form thêm mới
    private void showNewForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("customer-form.jsp");
        dispatcher.forward(request, response);
    }

    // ✅ Hiển thị form cập nhật
    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int id = Integer.parseInt(request.getParameter("id"));
        DTOCustomer existing = dao.getAllCustomers().stream()
                .filter(c -> c.getCustomerID() == id)
                .findFirst()
                .orElse(null);
        request.setAttribute("customer", existing);
        RequestDispatcher dispatcher = request.getRequestDispatcher("customer-form.jsp");
        dispatcher.forward(request, response);
    }

    // ✅ Thêm mới Customer
    private void insertCustomer(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        DTOCustomer c = getCustomerFromRequest(request);
        if (dao.insertCustomer(c)) {
            response.sendRedirect("customer?action=list");
        } else {
            response.sendRedirect("customer?action=new&error=invalid");
        }
    }

    // ✅ Cập nhật Customer
    private void updateCustomer(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        DTOCustomer c = getCustomerFromRequest(request);
        c.setCustomerID(Integer.parseInt(request.getParameter("customerID")));
        if (dao.updateCustomer(c)) {
            response.sendRedirect("customer?action=list");
        } else {
            response.sendRedirect("customer?action=edit&id=" + c.getCustomerID() + "&error=invalid");
        }
    }

    // ✅ Xóa Customer
    private void deleteCustomer(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int id = Integer.parseInt(request.getParameter("id"));
        dao.deleteCustomer(id);
        response.sendRedirect("customer?action=list");
    }

    // ✅ Hàm tiện ích: lấy dữ liệu form và chuyển sang DTOCustomer
    private DTOCustomer getCustomerFromRequest(HttpServletRequest request) {
        DTOCustomer c = new DTOCustomer();
        try {
            c.setFullName(request.getParameter("fullName"));
            c.setPhone(request.getParameter("phone"));
            c.setEmail(request.getParameter("email"));
            c.setAddress(request.getParameter("address"));
            c.setNote(request.getParameter("note"));
            c.setVehicleInterest(request.getParameter("vehicleInterest"));

            String createdAtStr = request.getParameter("createdAt");
            String birthDateStr = request.getParameter("birthDate");
            String testDriveStr = request.getParameter("testDriveSchedule");

            if (createdAtStr != null && !createdAtStr.isEmpty())
                c.setCreatedAt(Timestamp.valueOf(createdAtStr));
            if (birthDateStr != null && !birthDateStr.isEmpty())
                c.setBirthDate(Date.valueOf(birthDateStr));
            if (testDriveStr != null && !testDriveStr.isEmpty())
                c.setTestDriveSchedule(Timestamp.valueOf(testDriveStr));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }
}
