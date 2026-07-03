package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.MembershipPlan;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.MembershipPlanRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.MembershipPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.MembershipPlanDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.MembershipPlanServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MembershipPlanServiceImplement implements MembershipPlanServiceInterface {

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Override
    public ResponseEntity<ApiResponse> getMembershipPlanListIsActive() {
        try {
            List<MembershipPlanDTO> membershipPlanDTOList = membershipPlanRepository.findAll().stream()
                    .filter(plan -> Boolean.TRUE.equals(plan.getIsActive()) && !Boolean.TRUE.equals(plan.getIsDeleted()))   .map(membershipPlan -> new MembershipPlanDTO(
                            membershipPlan.getName(),
                            membershipPlan.getDescription(),
                            membershipPlan.getQuantity(),
                            membershipPlan.getPrice()
                    ))
                    .collect(Collectors.toList());

            if (membershipPlanDTOList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(membershipPlanDTOList, "List membership plan is empty"));
            }

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(membershipPlanDTOList, "List membership plan"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getMembershipPlanListByAdmin() {
        try {
            List<MembershipPlan> membershipPlans = membershipPlanRepository.findAll().stream()
                    .filter(plan -> !Boolean.TRUE.equals(plan.getIsDeleted()))
                    .collect(Collectors.toList());
            if (membershipPlans.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(membershipPlans, "List membership plan is empty"));
            }

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(membershipPlans, "List membership plan"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getMembershipPlanDetail(Integer id) {
        try {
            MembershipPlan existMembershipPlan = membershipPlanRepository.findById(id)
                    .filter(plan -> !Boolean.TRUE.equals(plan.getIsDeleted())).orElse(null);
            if (existMembershipPlan == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Membership plan id does not exist"));
            }

            MembershipPlanDTO membershipPlanDTO = new MembershipPlanDTO(
                    existMembershipPlan.getName(),
                    existMembershipPlan.getDescription(),
                    existMembershipPlan.getQuantity(),
                    existMembershipPlan.getPrice()
            );
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(membershipPlanDTO, "Membership plan"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> createMembershipPlan(MembershipPlanRequest membershipPlanRequest) {
        try {
            boolean existName = membershipPlanRepository.findAll().stream()
                    .filter(plan -> !Boolean.TRUE.equals(plan.getIsDeleted()))
                    .anyMatch(plan -> plan.getName().trim().toLowerCase().equals(membershipPlanRequest.getName().trim().toLowerCase()));

            if (existName) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Membership plan name exist"));
            }

            MembershipPlan membershipPlan = new MembershipPlan();
            membershipPlan.setName(membershipPlanRequest.getName());
            membershipPlan.setDescription(membershipPlanRequest.getDescription());
            membershipPlan.setQuantity(membershipPlanRequest.getQuantity());
            membershipPlan.setPrice(membershipPlanRequest.getPrice());
            membershipPlan.setIsActive(true);
            membershipPlan.setIsDeleted(false);
            membershipPlan.setCreatedAt(LocalDateTime.now());

            membershipPlanRepository.save(membershipPlan);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Create membership plan successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> updateMembershipPlan(Integer id, MembershipPlanRequest membershipPlanRequest) {
        try {
            MembershipPlan membershipPlan = membershipPlanRepository.findById(id)
                    .filter(plan -> !Boolean.TRUE.equals(plan.getIsDeleted())).orElse(null);
            if (membershipPlan == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Membership plan id does not exist"));
            }

            boolean existName = membershipPlanRepository.findAll().stream()
                    .filter(p -> !p.getMembershipPlanId().equals(id)) // Tránh tự trùng với chính nó khi không đổi tên
                    .anyMatch(p -> p.getName().trim().toLowerCase().equals(membershipPlanRequest.getName().trim().toLowerCase()));

            if (existName) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Membership plan name exist"));
            }

            membershipPlan.setName(membershipPlanRequest.getName());
            membershipPlan.setDescription(membershipPlanRequest.getDescription());
            membershipPlan.setQuantity(membershipPlanRequest.getQuantity());
            membershipPlan.setPrice(membershipPlanRequest.getPrice());
            membershipPlan.setUpdatedAt(LocalDateTime.now());

            membershipPlanRepository.save(membershipPlan);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Update membership plan successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> deleteMembershipPlan(Integer id) {
        try {
            MembershipPlan existMembershipPlan = membershipPlanRepository.findById(id).orElse(null);
            if (existMembershipPlan == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Membership plan id does not exist"));
            }

            existMembershipPlan.setIsActive(false);
            existMembershipPlan.setIsDeleted(true);
            existMembershipPlan.setUpdatedAt(LocalDateTime.now());
            membershipPlanRepository.save(existMembershipPlan);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Delete membership plan successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> toggleActiveMembershipPlan(Integer id, Boolean isActive) {
        try {
            MembershipPlan membershipPlan = membershipPlanRepository.findById(id)
                    .filter(plan -> !Boolean.TRUE.equals(plan.getIsDeleted())).orElse(null);
            if (membershipPlan == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Membership plan id does not exist"));
            }
            membershipPlan.setIsActive(isActive);
            membershipPlan.setUpdatedAt(LocalDateTime.now());
            membershipPlanRepository.save(membershipPlan);

            String msg = isActive ? "Activated membership plan successfully" : "Deactivated membership plan successfully";
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, msg));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }
}