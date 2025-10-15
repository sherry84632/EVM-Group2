package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAODiscountPolicy;
import com.dealermanagementsysstem.project.Model.DTODiscountPolicy;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "DiscountPolicyController", urlPatterns = {"/discount-policy"})
public class DiscountPolicyController extends HttpServlet {

    private final DAODiscountPolicy dao = new DAODiscountPolicy();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        try {
            if (action == null || action.equals("list")) {
                List<DTODiscountPolicy> list = dao.getAllPolicies();
                request.setAttribute("policies", list);
                request.getRequestDispatcher("templates/evmPage/discountPolicyList.html")
                        .forward(request, response);

            } else if (action.equals("apply")) {
                int policyId = Integer.parseInt(request.getParameter("policyId"));
                int detailId = Integer.parseInt(request.getParameter("orderDetailId"));
                boolean ok = dao.applyPolicyToSaleOrderDetail(detailId, policyId);
                if (ok) {
                    response.getWriter().write("Đã áp dụng chính sách cho chi tiết đơn hàng.");
                } else {
                    response.getWriter().write("Áp dụng thất bại.");
                }
            } else if (action.equals("create")) {
                request.getRequestDispatcher("templates/evmPage/createDiscountPolicy.html")
                        .forward(request, response);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int dealerId = Integer.parseInt(request.getParameter("dealerId"));
        String name = request.getParameter("policyName");
        String desc = request.getParameter("description");
        double hangPercent = Double.parseDouble(request.getParameter("hangPercent"));
        double dailyPercent = Double.parseDouble(request.getParameter("dailyPercent"));
        LocalDate startDate = LocalDate.parse(request.getParameter("startDate"));
        LocalDate endDate = LocalDate.parse(request.getParameter("endDate"));

        DTODiscountPolicy dto = new DTODiscountPolicy(0, dealerId, name, desc,
                hangPercent, dailyPercent, startDate, endDate, "Active");

        try {
            dao.addPolicy(dto);
            response.sendRedirect("discount-policy?action=list");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
