package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.MembershipPlan;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.MembershipPlanRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.MembershipPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipPlanServiceImplement - Membership Plan Management")
class MembershipPlanServiceImplementTest {

    @Mock
    private MembershipPlanRepository membershipPlanRepository;

    @InjectMocks
    private MembershipPlanServiceImplement membershipPlanService;

    private MembershipPlan activePlan;
    private MembershipPlan inactivePlan;
    private MembershipPlan deletedPlan;

    @BeforeEach
    void setUp() {
        activePlan = new MembershipPlan();
        activePlan.setMembershipPlanId(1);
        activePlan.setName("Basic Plan");
        activePlan.setDescription("Basic desc");
        activePlan.setQuantity(100);
        activePlan.setPrice(new BigDecimal("50000"));
        activePlan.setIsActive(true);
        activePlan.setIsDeleted(false);
        activePlan.setCreatedAt(LocalDateTime.now());

        inactivePlan = new MembershipPlan();
        inactivePlan.setMembershipPlanId(2);
        inactivePlan.setName("Pro Plan");
        inactivePlan.setIsActive(false);
        inactivePlan.setIsDeleted(false);

        deletedPlan = new MembershipPlan();
        deletedPlan.setMembershipPlanId(3);
        deletedPlan.setName("Del Plan");
        deletedPlan.setIsActive(true);
        deletedPlan.setIsDeleted(true);
    }

    @Nested
    @DisplayName("View Active Membership Plans")
    class ViewActiveMembershipPlansTests {

        @Test
        @DisplayName("Returns active and non-deleted plans OK")
        void getMembershipPlanListIsActive_valid_returnsOk() {
            when(membershipPlanRepository.findAll()).thenReturn(Arrays.asList(activePlan, inactivePlan, deletedPlan));

            ResponseEntity<ApiResponse> response = membershipPlanService.getMembershipPlanListIsActive();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("List membership plan", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Empty list returns OK")
        void getMembershipPlanListIsActive_empty_returnsOk() {
            when(membershipPlanRepository.findAll()).thenReturn(Collections.emptyList());

            ResponseEntity<ApiResponse> response = membershipPlanService.getMembershipPlanListIsActive();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("List membership plan is empty", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getMembershipPlanListIsActive_exception_returnsServerError() {
            when(membershipPlanRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = membershipPlanService.getMembershipPlanListIsActive();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Server error: DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("View Membership Plan Details")
    class ViewMembershipPlanDetailsTests {

        @Test
        @DisplayName("Existed ID returns OK")
        void getMembershipPlanDetail_existedId_returnsOk() {
            when(membershipPlanRepository.findById(1)).thenReturn(Optional.of(activePlan));

            ResponseEntity<ApiResponse> response = membershipPlanService.getMembershipPlanDetail(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Membership plan", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent ID returns NOT_FOUND")
        void getMembershipPlanDetail_nonExistentId_returnsNotFound() {
            when(membershipPlanRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = membershipPlanService.getMembershipPlanDetail(99);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Membership plan id does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getMembershipPlanDetail_exception_returnsServerError() {
            when(membershipPlanRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = membershipPlanService.getMembershipPlanDetail(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Server error: DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("View Membership Plans (Admin)")
    class ViewMembershipPlansAdminTests {

        @Test
        @DisplayName("Returns all non-deleted plans OK")
        void getMembershipPlanListByAdmin_valid_returnsOk() {
            when(membershipPlanRepository.findAll()).thenReturn(Arrays.asList(activePlan, inactivePlan, deletedPlan));

            ResponseEntity<ApiResponse> response = membershipPlanService.getMembershipPlanListByAdmin();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("List membership plan", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Empty list returns OK")
        void getMembershipPlanListByAdmin_empty_returnsOk() {
            when(membershipPlanRepository.findAll()).thenReturn(Collections.singletonList(deletedPlan));

            ResponseEntity<ApiResponse> response = membershipPlanService.getMembershipPlanListByAdmin();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("List membership plan is empty", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getMembershipPlanListByAdmin_exception_returnsServerError() {
            when(membershipPlanRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = membershipPlanService.getMembershipPlanListByAdmin();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Server error: DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Create Membership Plan")
    class CreateMembershipPlanTests {

        private MembershipPlanRequest request;

        @BeforeEach
        void setup() {
            request = new MembershipPlanRequest();
            request.setName("New Plan");
            request.setDescription("Desc");
            request.setQuantity(10);
            request.setPrice(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("UC-186, 187, 189, 190: Valid request returns OK")
        void createMembershipPlan_valid_returnsOk() {
            when(membershipPlanRepository.findAll()).thenReturn(Collections.emptyList());
            when(membershipPlanRepository.save(any())).thenReturn(activePlan);

            ResponseEntity<ApiResponse> response = membershipPlanService.createMembershipPlan(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Create membership plan successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Existed name returns BAD_REQUEST")
        void createMembershipPlan_existedName_returnsBadRequest() {
            request.setName("Basic Plan");
            when(membershipPlanRepository.findAll()).thenReturn(Collections.singletonList(activePlan));

            ResponseEntity<ApiResponse> response = membershipPlanService.createMembershipPlan(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Membership plan name exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void createMembershipPlan_exception_returnsServerError() {
            when(membershipPlanRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = membershipPlanService.createMembershipPlan(request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Server error: DB error", response.getBody().getMessage());
        }

        @ParameterizedTest
        @DisplayName("Blank/Zero fields returns BAD_REQUEST")
        @CsvSource({
                "'', 'Desc', '100000', '1'",
                "'Name', '', '100000', '1'",
                "'Name', 'Desc', '0', '1'",
                "'Name', 'Desc', '100000', '0'",
                "'', '', '0', '0'", // Empty fields
                "'Name', 'Desc', '-50000', '-1'" // Negative numbers
        })
        void createMembershipPlan_blankFields_returnsBadRequest(String name, String description, String price, int quantity) {
            request.setName(name);
            request.setDescription(description);
            request.setPrice(new BigDecimal(price));
            request.setQuantity(quantity);

            ResponseEntity<ApiResponse> response = membershipPlanService.createMembershipPlan(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Fields cannot be blank and numeric fields must be > 0", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Update Membership Plan")
    class UpdateMembershipPlanTests {

        private MembershipPlanRequest request;

        @BeforeEach
        void setup() {
            request = new MembershipPlanRequest();
            request.setName("Updated Plan");
            request.setDescription("Desc");
            request.setQuantity(10);
            request.setPrice(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("UC-192, 193, 195, 197: Valid request returns OK")
        void updateMembershipPlan_valid_returnsOk() {
            when(membershipPlanRepository.findById(1)).thenReturn(Optional.of(activePlan));
            when(membershipPlanRepository.save(any())).thenReturn(activePlan);

            ResponseEntity<ApiResponse> response = membershipPlanService.updateMembershipPlan(1, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Update membership plan successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent ID returns NOT_FOUND")
        void updateMembershipPlan_nonExistentId_returnsNotFound() {
            when(membershipPlanRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = membershipPlanService.updateMembershipPlan(99, request);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Membership plan id does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void updateMembershipPlan_exception_returnsServerError() {
            when(membershipPlanRepository.findById(1)).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = membershipPlanService.updateMembershipPlan(1, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Server error: DB error", response.getBody().getMessage());
        }

        @ParameterizedTest
        @DisplayName("Blank or negative fields return BAD_REQUEST")
        @CsvSource({
                "'', 'Desc', '100000', '1'",
                "'Name', '', '100000', '1'",
                "'Name', 'Desc', '0', '1'",
                "'Name', 'Desc', '100000', '0'",
                "'', '', '0', '0'", // Empty fields
                "'Name', 'Desc', '-50000', '-1'" // Negative numbers
        })
        void updateMembershipPlan_blankFields_returnsBadRequest(String name, String description, String price, int quantity) {
            request.setName(name);
            request.setDescription(description);
            request.setPrice(new BigDecimal(price));
            request.setQuantity(quantity);
            ResponseEntity<ApiResponse> response = membershipPlanService.updateMembershipPlan(1, request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Fields cannot be blank and numeric fields must be > 0", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Delete Membership Plan")
    class DeleteMembershipPlanTests {

        @Test
        @DisplayName("Valid request returns OK")
        void deleteMembershipPlan_valid_returnsOk() {
            when(membershipPlanRepository.findById(1)).thenReturn(Optional.of(activePlan));

            ResponseEntity<ApiResponse> response = membershipPlanService.deleteMembershipPlan(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Delete membership plan successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent ID returns NOT_FOUND")
        void deleteMembershipPlan_nonExistentId_returnsNotFound() {
            when(membershipPlanRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = membershipPlanService.deleteMembershipPlan(99);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Membership plan id does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void deleteMembershipPlan_exception_returnsServerError() {
            when(membershipPlanRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = membershipPlanService.deleteMembershipPlan(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Server error: DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Toggle Membership Plan Status")
    class ToggleMembershipPlanStatusTests {

        @Test
        @DisplayName("UC-204, 206: Valid request returns OK")
        void toggleActiveMembershipPlan_valid_returnsOk() {
            when(membershipPlanRepository.findById(1)).thenReturn(Optional.of(activePlan));

            ResponseEntity<ApiResponse> response = membershipPlanService.toggleActiveMembershipPlan(1, false);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Deactivated membership plan successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Valid request (Activate) returns OK")
        void toggleActiveMembershipPlan_activate_returnsOk() {
            when(membershipPlanRepository.findById(1)).thenReturn(Optional.of(activePlan));

            ResponseEntity<ApiResponse> response = membershipPlanService.toggleActiveMembershipPlan(1, true);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Activated membership plan successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent ID returns NOT_FOUND")
        void toggleActiveMembershipPlan_nonExistentId_returnsNotFound() {
            when(membershipPlanRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = membershipPlanService.toggleActiveMembershipPlan(99, true);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Membership plan id does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void toggleActiveMembershipPlan_exception_returnsServerError() {
            when(membershipPlanRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = membershipPlanService.toggleActiveMembershipPlan(1, true);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Server error: DB error", response.getBody().getMessage());
        }
    }
}