package com.dealermanagementsysstem.project.Model;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Cases for DiscountPolicy CRUD Operations
 * Tests: Create, Read (getById, getAll, search), Update, Delete
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DAODiscountPolicyTest {

    private static DAODiscountPolicy dao;
    private static DAODealer daoDealer;
    private static int testPolicyId;
    private static int testDealerId;

    @BeforeAll
    public static void setup() {
        dao = new DAODiscountPolicy();
        daoDealer = new DAODealer();

        // Create a test dealer for testing
        DTODealer dealer = new DTODealer();
        dealer.setDealerName("Test Dealer for Policy");
        dealer.setAddress("123 Test Street");
        dealer.setPhone("0123456789");
        dealer.setEmail("testpolicy@dealer.com");
        dealer.setEvmID(1);
        dealer.setLevelID(1);
        dealer.setPolicyID(1);

        testDealerId = daoDealer.insertDealer(dealer);
        assertTrue(testDealerId > 0, "Test dealer should be created");

        System.out.println("=".repeat(80));
        System.out.println("TEST SUITE: DiscountPolicy CRUD Operations");
        System.out.println("Test Dealer ID: " + testDealerId);
        System.out.println("=".repeat(80));
    }

    @Test
    @Order(1)
    @DisplayName("TC_CREATE_01: Create Discount Policy with valid data")
    public void testCreateDiscountPolicy() {
        System.out.println("\n--- TC_CREATE_01: Create Discount Policy ---");

        DTODealer dealer = daoDealer.getDealerById(testDealerId);
        assertNotNull(dealer, "Test dealer should exist");

        DTODiscountPolicy policy = new DTODiscountPolicy();
        policy.setDealer(dealer);
        policy.setPolicyName("Test Policy Lv1");
        policy.setDescription("Discount policy for testing");
        policy.setDiscountPercent(new BigDecimal("25.00")); // Dealer discount 25%
        policy.setHangPercent(new BigDecimal("95.00")); // Manufacturer share 95%
        policy.setDailyPercent(new BigDecimal("5.00")); // Dealer reward 5%
        policy.setStartDate(LocalDate.now());
        policy.setEndDate(LocalDate.now().plusMonths(6));
        policy.setStatus(DiscountPolicyStatus.ACTIVE);

        boolean created = dao.createDiscountPolicy(policy);
        assertTrue(created, "Policy should be created successfully");

        // Verify it was created
        var policies = dao.getPoliciesByDealerId(testDealerId);
        assertFalse(policies.isEmpty(), "Should have at least one policy");

        testPolicyId = policies.get(0).getPolicyID();
        System.out.println("✅ Created policy ID: " + testPolicyId);
        System.out.println("   Policy Name: " + policies.get(0).getPolicyName());
        System.out.println("   Discount: " + policies.get(0).getDiscountPercent() + "%");
        System.out.println("   Manufacturer Share: " + policies.get(0).getHangPercent() + "%");
        System.out.println("   Dealer Reward: " + policies.get(0).getDailyPercent() + "%");
    }

    @Test
    @Order(2)
    @DisplayName("TC_READ_01: Get policy by ID")
    public void testGetPolicyById() {
        System.out.println("\n--- TC_READ_01: Get Policy By ID ---");

        DTODiscountPolicy policy = dao.getPolicyById(testPolicyId);
        assertNotNull(policy, "Policy should be found");
        assertEquals(testPolicyId, policy.getPolicyID(), "Policy ID should match");
        assertEquals("Test Policy Lv1", policy.getPolicyName(), "Policy name should match");
        assertEquals(new BigDecimal("25.00"), policy.getDiscountPercent(), "Discount percent should match");
        assertEquals(new BigDecimal("95.00"), policy.getHangPercent(), "Manufacturer share should match");
        assertEquals(new BigDecimal("5.00"), policy.getDailyPercent(), "Dealer reward should match");

        System.out.println("✅ Policy found:");
        System.out.println("   ID: " + policy.getPolicyID());
        System.out.println("   Name: " + policy.getPolicyName());
        System.out.println("   Status: " + policy.getStatus());
        System.out.println("   Period: " + policy.getStartDate() + " to " + policy.getEndDate());
    }

    @Test
    @Order(3)
    @DisplayName("TC_READ_02: Get all policies")
    public void testGetAllPolicies() {
        System.out.println("\n--- TC_READ_02: Get All Policies ---");

        var policies = dao.getAllPolicies();
        assertNotNull(policies, "Policies list should not be null");
        assertFalse(policies.isEmpty(), "Should have at least one policy");

        System.out.println("✅ Found " + policies.size() + " policies:");
        policies.forEach(p -> {
            System.out.println("   - ID: " + p.getPolicyID() + ", Name: " + p.getPolicyName() +
                             ", Discount: " + p.getDiscountPercent() + "%");
        });
    }

    @Test
    @Order(4)
    @DisplayName("TC_READ_03: Search policy by name")
    public void testSearchPolicyByName() {
        System.out.println("\n--- TC_READ_03: Search Policy By Name ---");

        var policies = dao.searchPolicyByName("Test Policy");
        assertNotNull(policies, "Search results should not be null");
        assertFalse(policies.isEmpty(), "Should find at least one policy");

        boolean found = policies.stream().anyMatch(p -> p.getPolicyID() == testPolicyId);
        assertTrue(found, "Test policy should be in search results");

        System.out.println("✅ Search 'Test Policy' found " + policies.size() + " results");
    }

    @Test
    @Order(5)
    @DisplayName("TC_READ_04: Get policies by dealer ID")
    public void testGetPoliciesByDealerId() {
        System.out.println("\n--- TC_READ_04: Get Policies By Dealer ID ---");

        var policies = dao.getPoliciesByDealerId(testDealerId);
        assertNotNull(policies, "Policies should not be null");
        assertFalse(policies.isEmpty(), "Dealer should have policies");

        System.out.println("✅ Dealer " + testDealerId + " has " + policies.size() + " policies:");
        policies.forEach(p -> {
            System.out.println("   - " + p.getPolicyName() + " (Discount: " + p.getDiscountPercent() + "%)");
        });
    }

    @Test
    @Order(6)
    @DisplayName("TC_UPDATE_01: Update policy successfully")
    public void testUpdatePolicy() {
        System.out.println("\n--- TC_UPDATE_01: Update Policy ---");

        DTODiscountPolicy policy = dao.getPolicyById(testPolicyId);
        assertNotNull(policy, "Policy should exist before update");

        // Update fields
        policy.setPolicyName("Updated Test Policy");
        policy.setDescription("Updated description");
        policy.setDiscountPercent(new BigDecimal("30.00")); // Change discount to 30%
        policy.setHangPercent(new BigDecimal("92.00")); // Change manufacturer share to 92%
        policy.setDailyPercent(new BigDecimal("8.00")); // Change dealer reward to 8%
        policy.setStatus(DiscountPolicyStatus.EXPIRED);

        boolean updated = dao.updateDiscountPolicy(policy);
        assertTrue(updated, "Policy should be updated successfully");

        // Verify update
        DTODiscountPolicy updatedPolicy = dao.getPolicyById(testPolicyId);
        assertEquals("Updated Test Policy", updatedPolicy.getPolicyName(), "Name should be updated");
        assertEquals(new BigDecimal("30.00"), updatedPolicy.getDiscountPercent(), "Discount should be updated");
        assertEquals(new BigDecimal("92.00"), updatedPolicy.getHangPercent(), "Manufacturer share should be updated");
        assertEquals(new BigDecimal("8.00"), updatedPolicy.getDailyPercent(), "Dealer reward should be updated");
        assertEquals(DiscountPolicyStatus.EXPIRED, updatedPolicy.getStatus(), "Status should be updated");

        System.out.println("✅ Policy updated successfully:");
        System.out.println("   New Name: " + updatedPolicy.getPolicyName());
        System.out.println("   New Discount: " + updatedPolicy.getDiscountPercent() + "%");
        System.out.println("   New Manufacturer Share: " + updatedPolicy.getHangPercent() + "%");
        System.out.println("   New Dealer Reward: " + updatedPolicy.getDailyPercent() + "%");
        System.out.println("   New Status: " + updatedPolicy.getStatus());
    }

    @Test
    @Order(7)
    @DisplayName("TC_UPDATE_02: Update non-existent policy should fail")
    public void testUpdateNonExistentPolicy() {
        System.out.println("\n--- TC_UPDATE_02: Update Non-existent Policy ---");

        DTODiscountPolicy fakePolicy = new DTODiscountPolicy();
        fakePolicy.setPolicyID(99999); // Non-existent ID
        fakePolicy.setPolicyName("Fake Policy");
        fakePolicy.setDescription("Should not update");
        fakePolicy.setDiscountPercent(new BigDecimal("10.00"));
        fakePolicy.setHangPercent(new BigDecimal("95.00"));
        fakePolicy.setDailyPercent(new BigDecimal("5.00"));
        fakePolicy.setStartDate(LocalDate.now());
        fakePolicy.setEndDate(LocalDate.now().plusMonths(1));
        fakePolicy.setStatus(DiscountPolicyStatus.ACTIVE);

        boolean updated = dao.updateDiscountPolicy(fakePolicy);
        assertFalse(updated, "Update should fail for non-existent policy");

        System.out.println("✅ Update correctly failed for non-existent policy ID: 99999");
    }

    @Test
    @Order(8)
    @DisplayName("TC_DELETE_01: Delete policy successfully")
    public void testDeletePolicy() {
        System.out.println("\n--- TC_DELETE_01: Delete Policy ---");

        // Verify policy exists before deletion
        DTODiscountPolicy policyBefore = dao.getPolicyById(testPolicyId);
        assertNotNull(policyBefore, "Policy should exist before deletion");

        boolean deleted = dao.deleteDiscountPolicy(testPolicyId);
        assertTrue(deleted, "Policy should be deleted successfully");

        // Verify policy no longer exists
        DTODiscountPolicy policyAfter = dao.getPolicyById(testPolicyId);
        assertNull(policyAfter, "Policy should not exist after deletion");

        System.out.println("✅ Policy ID " + testPolicyId + " deleted successfully");
    }

    @Test
    @Order(9)
    @DisplayName("TC_DELETE_02: Delete non-existent policy should fail gracefully")
    public void testDeleteNonExistentPolicy() {
        System.out.println("\n--- TC_DELETE_02: Delete Non-existent Policy ---");

        boolean deleted = dao.deleteDiscountPolicy(99999);
        assertFalse(deleted, "Delete should return false for non-existent policy");

        System.out.println("✅ Delete correctly failed for non-existent policy ID: 99999");
    }

    @Test
    @Order(10)
    @DisplayName("TC_CREATE_02: Validate discount percentages sum correctly")
    public void testDiscountPercentagesValidation() {
        System.out.println("\n--- TC_CREATE_02: Validate Discount Percentages ---");

        DTODealer dealer = daoDealer.getDealerById(testDealerId);

        DTODiscountPolicy policy = new DTODiscountPolicy();
        policy.setDealer(dealer);
        policy.setPolicyName("Validation Test Policy");
        policy.setDescription("Testing percentage validation");
        policy.setDiscountPercent(new BigDecimal("20.00")); // Dealer discount
        policy.setHangPercent(new BigDecimal("94.00")); // Manufacturer share
        policy.setDailyPercent(new BigDecimal("6.00")); // Dealer reward
        policy.setStartDate(LocalDate.now());
        policy.setEndDate(LocalDate.now().plusMonths(1));
        policy.setStatus(DiscountPolicyStatus.ACTIVE);

        boolean created = dao.createDiscountPolicy(policy);
        assertTrue(created, "Policy with custom percentages should be created");

        // Verify sum of manufacturer share + dealer reward = 100%
        BigDecimal sum = policy.getHangPercent().add(policy.getDailyPercent());
        assertEquals(new BigDecimal("100.00"), sum, "Manufacturer share + Dealer reward should equal 100%");

        System.out.println("✅ Percentage validation passed:");
        System.out.println("   Discount: " + policy.getDiscountPercent() + "%");
        System.out.println("   Manufacturer: " + policy.getHangPercent() + "% + Dealer: " + policy.getDailyPercent() + "% = " + sum + "%");

        // Cleanup
        var policies = dao.getPoliciesByDealerId(testDealerId);
        for (var p : policies) {
            dao.deleteDiscountPolicy(p.getPolicyID());
        }
    }

    @AfterAll
    public static void cleanup() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CLEANUP: Removing test data");

        // Delete test dealer
        if (testDealerId > 0) {
            boolean deleted = daoDealer.deleteDealer(testDealerId);
            System.out.println((deleted ? "✅" : "⚠️") + " Test dealer " + testDealerId + " removed");
        }

        System.out.println("=".repeat(80));
        System.out.println("TEST SUITE COMPLETED: DiscountPolicy CRUD");
        System.out.println("=".repeat(80));
    }
}

