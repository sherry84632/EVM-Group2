package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DTOVehicleComparison;
import com.dealermanagementsysstem.project.service.VehicleComparisonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

@Controller
@RequestMapping("/vehicle/compare")
public class VehicleComparisonController {
    
    private static final String COMPARISON_LIST_KEY = "vehicleComparisonList";
    private static final int MAX_COMPARISON_ITEMS = 5;
    
    @Autowired
    private VehicleComparisonService comparisonService;
    
    @GetMapping
    public String showComparisonPage(Model model, HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Integer> comparisonList = (List<Integer>) session.getAttribute(COMPARISON_LIST_KEY);
        
        if (comparisonList == null || comparisonList.isEmpty()) {
            model.addAttribute("vehicles", new ArrayList<>());
            model.addAttribute("comparisonMatrix", new LinkedHashMap<>());
            model.addAttribute("specs", comparisonService.getComparisonSpecs());
            return "vehicle/compare";
        }
        
        List<DTOVehicleComparison> vehicles = comparisonService.getVehiclesForComparison(comparisonList);
        Map<String, List<Object>> matrix = comparisonService.getComparisonMatrix(vehicles);
        
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("comparisonMatrix", matrix);
        model.addAttribute("specs", comparisonService.getComparisonSpecs());
        model.addAttribute("count", vehicles.size());
        
        return "vehicle/compare";
    }
    
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToComparison(@RequestParam("vehicleId") Integer vehicleId,
                                                              HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Integer> comparisonList = (List<Integer>) session.getAttribute(COMPARISON_LIST_KEY);
        
        if (comparisonList == null) {
            comparisonList = new ArrayList<>();
            session.setAttribute(COMPARISON_LIST_KEY, comparisonList);
        }
        
        Map<String, Object> response = new HashMap<>();
        
        if (comparisonList.size() >= MAX_COMPARISON_ITEMS) {
            response.put("success", false);
            response.put("message", "Maximum " + MAX_COMPARISON_ITEMS + " vehicles can be compared at once");
            response.put("currentCount", comparisonList.size());
            return ResponseEntity.ok(response);
        }
        
        if (comparisonList.contains(vehicleId)) {
            response.put("success", false);
            response.put("message", "Vehicle already in comparison list");
            response.put("currentCount", comparisonList.size());
            return ResponseEntity.ok(response);
        }
        
        comparisonList.add(vehicleId);
        response.put("success", true);
        response.put("message", "Vehicle added to comparison");
        response.put("currentCount", comparisonList.size());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromComparison(@RequestParam("vehicleId") Integer vehicleId,
                                                                    HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Integer> comparisonList = (List<Integer>) session.getAttribute(COMPARISON_LIST_KEY);
        
        Map<String, Object> response = new HashMap<>();
        
        if (comparisonList == null || !comparisonList.contains(vehicleId)) {
            response.put("success", false);
            response.put("message", "Vehicle not in comparison list");
            return ResponseEntity.ok(response);
        }
        
        comparisonList.remove(vehicleId);
        response.put("success", true);
        response.put("message", "Vehicle removed from comparison");
        response.put("currentCount", comparisonList.size());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearComparison(HttpSession session) {
        session.removeAttribute(COMPARISON_LIST_KEY);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Comparison list cleared");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getComparisonList(HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Integer> comparisonList = (List<Integer>) session.getAttribute(COMPARISON_LIST_KEY);
        
        Map<String, Object> response = new HashMap<>();
        
        if (comparisonList == null || comparisonList.isEmpty()) {
            response.put("vehicles", new ArrayList<>());
            response.put("count", 0);
            return ResponseEntity.ok(response);
        }
        
        List<DTOVehicleComparison> vehicles = comparisonService.getVehiclesForComparison(comparisonList);
        response.put("vehicles", vehicles);
        response.put("count", vehicles.size());
        response.put("maxItems", MAX_COMPARISON_ITEMS);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getComparisonData(@RequestParam(value = "vehicleIds", required = false) String vehicleIdsStr,
                                                                  HttpSession session) {
        List<Integer> vehicleIds = new ArrayList<>();
        
        if (vehicleIdsStr != null && !vehicleIdsStr.isBlank()) {
            try {
                vehicleIds = Arrays.stream(vehicleIdsStr.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            } catch (NumberFormatException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid vehicle IDs format");
                return ResponseEntity.badRequest().body(error);
            }
        } else {
            @SuppressWarnings("unchecked")
            List<Integer> sessionList = (List<Integer>) session.getAttribute(COMPARISON_LIST_KEY);
            if (sessionList != null) {
                vehicleIds = sessionList;
            }
        }
        
        if (vehicleIds.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("vehicles", new ArrayList<>());
            empty.put("comparisonMatrix", new LinkedHashMap<>());
            empty.put("specs", comparisonService.getComparisonSpecs());
            return ResponseEntity.ok(empty);
        }
        
        List<DTOVehicleComparison> vehicles = comparisonService.getVehiclesForComparison(vehicleIds);
        Map<String, List<Object>> matrix = comparisonService.getComparisonMatrix(vehicles);
        
        Map<String, Object> response = new HashMap<>();
        response.put("vehicles", vehicles);
        response.put("comparisonMatrix", matrix);
        response.put("specs", comparisonService.getComparisonSpecs());
        response.put("count", vehicles.size());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/share/{token}")
    public String shareComparison(@PathVariable String token, Model model, HttpSession session) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token));
            List<Integer> vehicleIds = Arrays.stream(decoded.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
            
            List<DTOVehicleComparison> vehicles = comparisonService.getVehiclesForComparison(vehicleIds);
            Map<String, List<Object>> matrix = comparisonService.getComparisonMatrix(vehicles);
            
            model.addAttribute("vehicles", vehicles);
            model.addAttribute("comparisonMatrix", matrix);
            model.addAttribute("specs", comparisonService.getComparisonSpecs());
            model.addAttribute("count", vehicles.size());
            model.addAttribute("shared", true);
            
            return "vehicle/compare";
        } catch (Exception e) {
            model.addAttribute("error", "Invalid comparison link");
            return "error";
        }
    }
    
    @GetMapping("/share-link")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateShareLink(HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Integer> comparisonList = (List<Integer>) session.getAttribute(COMPARISON_LIST_KEY);
        
        Map<String, Object> response = new HashMap<>();
        
        if (comparisonList == null || comparisonList.isEmpty()) {
            response.put("success", false);
            response.put("message", "No vehicles in comparison list");
            return ResponseEntity.ok(response);
        }
        
        String vehicleIdsStr = comparisonList.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
        
        String token = Base64.getUrlEncoder().encodeToString(vehicleIdsStr.getBytes());
        String shareUrl = "/vehicle/compare/share/" + token;
        
        response.put("success", true);
        response.put("shareUrl", shareUrl);
        response.put("token", token);
        
        return ResponseEntity.ok(response);
    }
}

