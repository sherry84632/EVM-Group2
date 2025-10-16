package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DTOPurchaseOrder;
import com.dealermanagementsysstem.project.Model.DAOPurchaseOrder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/orderdealer")
public class PurchaseOrderController {

    private final DAOPurchaseOrder dao = new DAOPurchaseOrder();

    // READ - danh sách
    @GetMapping("/list")
    public String listOrders(Model model) {
        List<DTOPurchaseOrder> orders = dao.getAllPurchaseOrders();
        model.addAttribute("orders", orders);
        return "evmPage/evmOrderList";
    }

    // CREATE - form
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("order", new DTOPurchaseOrder());
        return "evmPage/createPurchaseOrder";
    }

    // CREATE - xử lý form
    @PostMapping("/create")
    public String createOrder(@ModelAttribute("order") DTOPurchaseOrder order) {
        dao.addPurchaseOrder(order);
        return "redirect:/orderdealer/list";
    }

    // UPDATE - form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable int id, Model model) {
        DTOPurchaseOrder order = dao.getPurchaseOrderById(id);
        model.addAttribute("order", order);
        return "evmPage/editPurchaseOrder";
    }

    // UPDATE - xử lý form
    @PostMapping("/edit/{id}")
    public String updateOrder(@PathVariable int id, @ModelAttribute("order") DTOPurchaseOrder order) {
        order.setPurchaseOrderId(id);
        dao.updatePurchaseOrder(order);
        return "redirect:/orderdealer/list";
    }

    // DELETE
    @GetMapping("/delete/{id}")
    public String deleteOrder(@PathVariable int id) {
        dao.deletePurchaseOrder(id);
        return "redirect:/orderdealer/list";
    }
}
