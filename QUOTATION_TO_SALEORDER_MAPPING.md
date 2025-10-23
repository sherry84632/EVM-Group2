[//]: # (# 📊 BÁO CÁO: LIÊN KẾT QUOTATION → SALEORDER)

[//]: # ()
[//]: # (**Ngày:** 24/10/2025  )

[//]: # (**Chức năng:** Quotation to SaleOrder Conversion Flow)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## 🎯 **TỔNG QUAN LUỒNG DỮ LIỆU**)

[//]: # ()
[//]: # (```)

[//]: # (┌─────────────────┐)

[//]: # (│   QUOTATION     │)

[//]: # (│  &#40;Approved&#41;     │)

[//]: # (└────────┬────────┘)

[//]: # (         │)

[//]: # (         │ Click "Create Order")

[//]: # (         ▼)

[//]: # (┌─────────────────┐)

[//]: # (│  VALIDATION     │)

[//]: # (│  - Approved?    │)

[//]: # (│  - Converted?   │)

[//]: # (└────────┬────────┘)

[//]: # (         │)

[//]: # (         │ ✅ Pass)

[//]: # (         ▼)

[//]: # (┌─────────────────────────────────────────────────┐)

[//]: # (│           CREATE SALE ORDER                      │)

[//]: # (│                                                  │)

[//]: # (│  Quotation → SaleOrder:                         │)

[//]: # (│  ├─ QuotationID → SaleOrder.QuotationID         │)

[//]: # (│  ├─ CustomerID → SaleOrder.CustomerID           │)

[//]: # (│  ├─ DealerID → SaleOrder.DealerID               │)

[//]: # (│  ├─ StaffID → SaleOrder.StaffID                 │)

[//]: # (│  ├─ TotalAmount → SaleOrder.TotalAmount         │)

[//]: # (│  └─ TotalQuantity → SaleOrder.TotalQuantity     │)

[//]: # (│                                                  │)

[//]: # (│  QuotationDetail → SaleOrderDetail:             │)

[//]: # (│  ├─ VIN → SaleOrderDetail.VIN                   │)

[//]: # (│  ├─ UnitPrice → SaleOrderDetail.Price           │)

[//]: # (│  ├─ Quantity → SaleOrderDetail.Quantity         │)

[//]: # (│  ├─ ColorID → SaleOrderDetail.ColorID           │)

[//]: # (│  └─ QuotationID → SaleOrderDetail.QuotationID   │)

[//]: # (│     &#40;for traceability&#41;                          │)

[//]: # (└─────────────────────────────────────────────────┘)

[//]: # (         │)

[//]: # (         ▼)

[//]: # (┌─────────────────┐)

[//]: # (│ UPDATE STATUS   │)

[//]: # (│ Quotation →     │)

[//]: # (│ "Converted"     │)

[//]: # (└─────────────────┘)

[//]: # (```)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## 🔄 **CHI TIẾT MAPPING DỮ LIỆU**)

[//]: # ()
[//]: # (### **1. QUOTATION → SALEORDER**)

[//]: # ()
[//]: # (| Quotation Field | SaleOrder Field | Logic |)

[//]: # (|----------------|----------------|-------|)

[//]: # (| `quotationID` | `quotationID` | Direct reference &#40;foreign key&#41; |)

[//]: # (| `customer.customerID` | `customerID` | Direct copy |)

[//]: # (| `dealer.dealerID` | `dealerID` | Direct copy |)

[//]: # (| `staff.staffID` | `staffID` | Copy if exists, else NULL |)

[//]: # (| `createdAt` | N/A | SaleOrder uses current timestamp |)

[//]: # (| `status` | `status` | SaleOrder starts as "Pending" |)

[//]: # (| SUM&#40;details.quantity&#41; | `totalQuantity` | Calculated from details |)

[//]: # (| SUM&#40;details.unitPrice * quantity&#41; | `totalAmount` | Calculated from details |)

[//]: # ()
[//]: # (### **2. QUOTATION_DETAIL → SALE_ORDER_DETAIL**)

[//]: # ()
[//]: # (| QuotationDetail Field | SaleOrderDetail Field | Logic |)

[//]: # (|----------------------|----------------------|-------|)

[//]: # (| `quotationDetailID` | N/A | Not copied &#40;new ID generated&#41; |)

[//]: # (| `quotationID` | `quotationID` | **Traceability link** |)

[//]: # (| `VIN` | `VIN` | Direct copy |)

[//]: # (| `unitPrice` | `price` | Direct copy &#40;locked price&#41; |)

[//]: # (| `quantity` | `quantity` | Direct copy |)

[//]: # (| `colorID` | `colorID` | Direct copy |)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## ✅ **ĐÃ FIX/THÊM MỚI**)

[//]: # ()
[//]: # (### **DTOSaleOrder.java**)

[//]: # (```java)

[//]: # (// ✅ ADDED: quotationID field)

[//]: # (private int quotationID;)

[//]: # ()
[//]: # (// ✅ CHANGED: detail → details &#40;List&#41;)

[//]: # (private List<DTOSaleOrderDetail> details; // Was: DTOSaleOrderDetail detail)

[//]: # ()
[//]: # (// ✅ ADDED: getters/setters)

[//]: # (public int getQuotationID&#40;&#41; { ... })

[//]: # (public void setQuotationID&#40;int quotationID&#41; { ... })

[//]: # (public List<DTOSaleOrderDetail> getDetails&#40;&#41; { ... })

[//]: # (public void setDetails&#40;List<DTOSaleOrderDetail> details&#41; { ... })

[//]: # (```)

[//]: # ()
[//]: # (### **DTOSaleOrderDetail.java**)

[//]: # (```java)

[//]: # (// ✅ ADDED: All vehicle-related fields)

[//]: # (private String VIN;)

[//]: # (private int colorID;)

[//]: # (private String colorName;)

[//]: # (private String modelName;)

[//]: # (private int manufactureYear;)

[//]: # ()
[//]: # (// ✅ ADDED: quotationID for traceability)

[//]: # (private int quotationID;)

[//]: # ()
[//]: # (// ✅ RENAMED: soDetailID → saleOrderDetailID &#40;match database&#41;)

[//]: # (private int saleOrderDetailID;)

[//]: # (```)

[//]: # ()
[//]: # (### **DAOSaleOrder.java**)

[//]: # ()
[//]: # (#### **createSaleOrderFromQuotation&#40;&#41; - FIXED:**)

[//]: # (```java)

[//]: # (// ✅ FIX 1: Add QuotationID to INSERT)

[//]: # (INSERT INTO SaleOrder &#40;QuotationID, CustomerID, DealerID, StaffID, ...&#41;)

[//]: # ()
[//]: # (// ✅ FIX 2: Proper parameter binding)

[//]: # (psOrder.setInt&#40;1, quotationID&#41;; // QuotationID)

[//]: # (psOrder.setInt&#40;2, quotation.getCustomer&#40;&#41;.getCustomerID&#40;&#41;&#41;;)

[//]: # (psOrder.setInt&#40;3, quotation.getDealer&#40;&#41;.getDealerID&#40;&#41;&#41;;)

[//]: # ()
[//]: # (// ✅ FIX 3: StaffID logic - use NULL instead of DealerID)

[//]: # (if &#40;quotation.getStaff&#40;&#41; != null && quotation.getStaff&#40;&#41;.getStaffID&#40;&#41; > 0&#41; {)

[//]: # (    psOrder.setInt&#40;4, quotation.getStaff&#40;&#41;.getStaffID&#40;&#41;&#41;;)

[//]: # (} else {)

[//]: # (    psOrder.setNull&#40;4, java.sql.Types.INTEGER&#41;; // ← FIX: Not DealerID!)

[//]: # (})

[//]: # ()
[//]: # (// ✅ FIX 4: Copy all QuotationDetail fields to SaleOrderDetail)

[//]: # (psDetail.setInt&#40;1, saleOrderID&#41;;)

[//]: # (psDetail.setString&#40;2, d.getVIN&#40;&#41;&#41;;          // From QuotationDetail)

[//]: # (psDetail.setInt&#40;3, d.getQuantity&#40;&#41;&#41;;        // From QuotationDetail)

[//]: # (psDetail.setBigDecimal&#40;4, d.getUnitPrice&#40;&#41;&#41;; // From QuotationDetail &#40;locked&#41;)

[//]: # (psDetail.setInt&#40;5, quotationID&#41;;            // Traceability!)

[//]: # (psDetail.setInt&#40;6, d.getColorID&#40;&#41;&#41;;         // From QuotationDetail)

[//]: # (```)

[//]: # ()
[//]: # (#### **ADDED NEW METHODS:**)

[//]: # (```java)

[//]: # (// ✅ Get SaleOrder by ID with full details)

[//]: # (public DTOSaleOrder getSaleOrderById&#40;int saleOrderID&#41;)

[//]: # ()
[//]: # (// ✅ Get all SaleOrderDetails for an order)

[//]: # (public List<DTOSaleOrderDetail> getSaleOrderDetails&#40;int saleOrderID&#41;)

[//]: # ()
[//]: # (// ✅ Get SaleOrder by QuotationID &#40;check if converted&#41;)

[//]: # (public DTOSaleOrder getSaleOrderByQuotationId&#40;int quotationID&#41;)

[//]: # ()
[//]: # (// ✅ Updated getAllSaleOrders&#40;&#41; to include QuotationID)

[//]: # (```)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## 🔐 **BUSINESS RULES ENFORCED**)

[//]: # ()
[//]: # (### **1. Quotation Status Validation**)

[//]: # (```java)

[//]: # (// ❌ Cannot convert if not approved)

[//]: # (if &#40;!daoQuotation.isQuotationApproved&#40;quotationID&#41;&#41; {)

[//]: # (    return -1; // Error: Not approved)

[//]: # (})

[//]: # (```)

[//]: # ()
[//]: # (### **2. Duplicate Prevention**)

[//]: # (```java)

[//]: # (// ❌ Cannot convert the same quotation twice)

[//]: # (if &#40;daoQuotation.isQuotationConverted&#40;quotationID&#41;&#41; {)

[//]: # (    return -2; // Error: Already converted)

[//]: # (})

[//]: # ()
[//]: # (// After successful conversion:)

[//]: # (daoQuotation.markQuotationAsConverted&#40;quotationID&#41;;)

[//]: # (```)

[//]: # ()
[//]: # (### **3. Data Integrity**)

[//]: # (```java)

[//]: # (// ✅ Transaction with rollback)

[//]: # (conn.setAutoCommit&#40;false&#41;;)

[//]: # (try {)

[//]: # (    // Insert SaleOrder)

[//]: # (    // Insert SaleOrderDetails)

[//]: # (    // Mark Quotation as Converted)

[//]: # (    conn.commit&#40;&#41;;)

[//]: # (} catch &#40;SQLException e&#41; {)

[//]: # (    conn.rollback&#40;&#41;; // ← All or nothing)

[//]: # (})

[//]: # (```)

[//]: # ()
[//]: # (### **4. Traceability**)

[//]: # (```java)

[//]: # (// ✅ Can trace back from SaleOrder → Quotation)

[//]: # (SELECT * FROM SaleOrder WHERE QuotationID = ?)

[//]: # ()
[//]: # (// ✅ Can trace SaleOrderDetail → QuotationDetail)

[//]: # (SELECT * FROM SaleOrderDetail WHERE QuotationID = ?)

[//]: # (```)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## 📦 **DATABASE SCHEMA REQUIREMENTS**)

[//]: # ()
[//]: # (### **SaleOrder Table:**)

[//]: # (```sql)

[//]: # (CREATE TABLE SaleOrder &#40;)

[//]: # (    SaleOrderID INT PRIMARY KEY AUTO_INCREMENT,)

[//]: # (    QuotationID INT NOT NULL,              -- ✅ ADDED)

[//]: # (    CustomerID INT NOT NULL,)

[//]: # (    DealerID INT NOT NULL,)

[//]: # (    StaffID INT NULL,                      -- ✅ Can be NULL)

[//]: # (    CreatedAt DATETIME NOT NULL,)

[//]: # (    Status VARCHAR&#40;50&#41; NOT NULL,)

[//]: # (    TotalQuantity INT NOT NULL,)

[//]: # (    TotalAmount DECIMAL&#40;15,2&#41; NOT NULL,)

[//]: # (    )
[//]: # (    FOREIGN KEY &#40;QuotationID&#41; REFERENCES Quotation&#40;QuotationID&#41;,)

[//]: # (    FOREIGN KEY &#40;CustomerID&#41; REFERENCES Customer&#40;CustomerID&#41;,)

[//]: # (    FOREIGN KEY &#40;DealerID&#41; REFERENCES Dealer&#40;DealerID&#41;,)

[//]: # (    FOREIGN KEY &#40;StaffID&#41; REFERENCES DealerStaff&#40;StaffID&#41;)

[//]: # (&#41;;)

[//]: # ()
[//]: # (-- ✅ Ensure one quotation = one order)

[//]: # (CREATE UNIQUE INDEX UQ_SaleOrder_QuotationID ON SaleOrder&#40;QuotationID&#41;;)

[//]: # (```)

[//]: # ()
[//]: # (### **SaleOrderDetail Table:**)

[//]: # (```sql)

[//]: # (CREATE TABLE SaleOrderDetail &#40;)

[//]: # (    SaleOrderDetailID INT PRIMARY KEY AUTO_INCREMENT,)

[//]: # (    SaleOrderID INT NOT NULL,)

[//]: # (    VIN VARCHAR&#40;50&#41; NOT NULL,)

[//]: # (    Quantity INT NOT NULL,)

[//]: # (    Price DECIMAL&#40;15,2&#41; NOT NULL,         -- ✅ Locked price from quotation)

[//]: # (    QuotationID INT NOT NULL,              -- ✅ Traceability)

[//]: # (    ColorID INT NOT NULL,)

[//]: # (    )
[//]: # (    FOREIGN KEY &#40;SaleOrderID&#41; REFERENCES SaleOrder&#40;SaleOrderID&#41; ON DELETE CASCADE,)

[//]: # (    FOREIGN KEY &#40;VIN&#41; REFERENCES Vehicle&#40;VIN&#41;,)

[//]: # (    FOREIGN KEY &#40;QuotationID&#41; REFERENCES Quotation&#40;QuotationID&#41;,)

[//]: # (    FOREIGN KEY &#40;ColorID&#41; REFERENCES VehicleColor&#40;ColorID&#41;)

[//]: # (&#41;;)

[//]: # (```)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## 🎬 **USAGE EXAMPLES**)

[//]: # ()
[//]: # (### **1. Convert Quotation to SaleOrder:**)

[//]: # (```java)

[//]: # (DAOSaleOrder dao = new DAOSaleOrder&#40;&#41;;)

[//]: # (int quotationID = 123;)

[//]: # ()
[//]: # (int saleOrderID = dao.createSaleOrderFromQuotation&#40;quotationID&#41;;)

[//]: # ()
[//]: # (if &#40;saleOrderID > 0&#41; {)

[//]: # (    System.out.println&#40;"SaleOrder created: " + saleOrderID&#41;;)

[//]: # (} else if &#40;saleOrderID == -2&#41; {)

[//]: # (    System.out.println&#40;"Error: Quotation already converted"&#41;;)

[//]: # (} else {)

[//]: # (    System.out.println&#40;"Error: Cannot create order"&#41;;)

[//]: # (})

[//]: # (```)

[//]: # ()
[//]: # (### **2. Get SaleOrder with Details:**)

[//]: # (```java)

[//]: # (DTOSaleOrder order = dao.getSaleOrderById&#40;saleOrderID&#41;;)

[//]: # (System.out.println&#40;"Order ID: " + order.getSaleOrderID&#40;&#41;&#41;;)

[//]: # (System.out.println&#40;"From Quotation: " + order.getQuotationID&#40;&#41;&#41;;)

[//]: # (System.out.println&#40;"Customer: " + order.getCustomer&#40;&#41;.getFullName&#40;&#41;&#41;;)

[//]: # (System.out.println&#40;"Total: " + order.getTotalAmount&#40;&#41;&#41;;)

[//]: # ()
[//]: # (// Get details)

[//]: # (for &#40;DTOSaleOrderDetail detail : order.getDetails&#40;&#41;&#41; {)

[//]: # (    System.out.println&#40;"VIN: " + detail.getVIN&#40;&#41;&#41;;)

[//]: # (    System.out.println&#40;"Model: " + detail.getModelName&#40;&#41;&#41;;)

[//]: # (    System.out.println&#40;"Quantity: " + detail.getQuantity&#40;&#41;&#41;;)

[//]: # (    System.out.println&#40;"Price: " + detail.getPrice&#40;&#41;&#41;;)

[//]: # (    System.out.println&#40;"From Quotation: " + detail.getQuotationID&#40;&#41;&#41;;)

[//]: # (})

[//]: # (```)

[//]: # ()
[//]: # (### **3. Check if Quotation Already Converted:**)

[//]: # (```java)

[//]: # (DTOSaleOrder existing = dao.getSaleOrderByQuotationId&#40;quotationID&#41;;)

[//]: # (if &#40;existing != null&#41; {)

[//]: # (    System.out.println&#40;"Already converted to Order #" + existing.getSaleOrderID&#40;&#41;&#41;;)

[//]: # (})

[//]: # (```)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## 🔍 **DATA FLOW VERIFICATION**)

[//]: # ()
[//]: # (### **Before Conversion:**)

[//]: # (```)

[//]: # (Quotation #101)

[//]: # (├─ Customer: John Doe)

[//]: # (├─ Dealer: ABC Motors)

[//]: # (├─ Status: Accepted)

[//]: # (├─ TotalPrice: $50,000)

[//]: # (└─ Details:)

[//]: # (   ├─ VIN001, Model X, Qty: 1, Price: $30,000)

[//]: # (   └─ VIN002, Model Y, Qty: 2, Price: $10,000 each)

[//]: # (```)

[//]: # ()
[//]: # (### **After Conversion:**)

[//]: # (```)

[//]: # (SaleOrder #201)

[//]: # (├─ QuotationID: 101           ← Reference to source)

[//]: # (├─ Customer: John Doe         ← Copied from Quotation)

[//]: # (├─ Dealer: ABC Motors         ← Copied from Quotation)

[//]: # (├─ Status: Pending            ← New order status)

[//]: # (├─ TotalAmount: $50,000       ← Calculated from details)

[//]: # (├─ TotalQuantity: 3           ← Calculated from details)

[//]: # (└─ Details:)

[//]: # (   ├─ VIN001, Model X, Qty: 1, Price: $30,000, QuotationID: 101)

[//]: # (   └─ VIN002, Model Y, Qty: 2, Price: $10,000, QuotationID: 101)

[//]: # (                                               └─ Traceability!)

[//]: # ()
[//]: # (Quotation #101)

[//]: # (└─ Status: Converted          ← Marked as converted)

[//]: # (```)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## ✅ **BENEFITS OF THIS DESIGN**)

[//]: # ()
[//]: # (1. **Traceability**: Mỗi SaleOrder biết nó từ Quotation nào)

[//]: # (2. **Data Integrity**: Transaction đảm bảo all-or-nothing)

[//]: # (3. **Price Lock**: Giá trong SaleOrderDetail = giá đã approve trong Quotation)

[//]: # (4. **Audit Trail**: Có thể trace lại toàn bộ lịch sử từ quotation → order)

[//]: # (5. **Duplicate Prevention**: 1 Quotation chỉ tạo được 1 SaleOrder)

[//]: # (6. **Flexibility**: StaffID có thể NULL nếu quotation không có staff)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## 🚀 **NEXT STEPS**)

[//]: # ()
[//]: # (### **Database Migration:**)

[//]: # (```sql)

[//]: # (-- Add QuotationID column if not exists)

[//]: # (ALTER TABLE SaleOrder )

[//]: # (ADD COLUMN QuotationID INT NULL AFTER SaleOrderID;)

[//]: # ()
[//]: # (-- Add foreign key)

[//]: # (ALTER TABLE SaleOrder)

[//]: # (ADD CONSTRAINT FK_SaleOrder_Quotation )

[//]: # (FOREIGN KEY &#40;QuotationID&#41; REFERENCES Quotation&#40;QuotationID&#41;;)

[//]: # ()
[//]: # (-- Add unique constraint)

[//]: # (ALTER TABLE SaleOrder)

[//]: # (ADD CONSTRAINT UQ_SaleOrder_QuotationID UNIQUE &#40;QuotationID&#41;;)

[//]: # ()
[//]: # (-- Add QuotationID to SaleOrderDetail if not exists)

[//]: # (ALTER TABLE SaleOrderDetail)

[//]: # (ADD COLUMN QuotationID INT NULL;)

[//]: # ()
[//]: # (-- Add foreign key)

[//]: # (ALTER TABLE SaleOrderDetail)

[//]: # (ADD CONSTRAINT FK_SaleOrderDetail_Quotation)

[//]: # (FOREIGN KEY &#40;QuotationID&#41; REFERENCES Quotation&#40;QuotationID&#41;;)

[//]: # (```)

[//]: # ()
[//]: # (### **Test Cases:**)

[//]: # (1. ✅ Convert approved quotation → Success)

[//]: # (2. ✅ Try convert pending quotation → Error)

[//]: # (3. ✅ Try convert same quotation twice → Error)

[//]: # (4. ✅ Check all fields copied correctly)

[//]: # (5. ✅ Check quotation marked as "Converted")

[//]: # (6. ✅ Rollback on error &#40;transaction test&#41;)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (**Generated by:** GitHub Copilot  )

[//]: # (**Date:** October 24, 2025)

[//]: # ()
