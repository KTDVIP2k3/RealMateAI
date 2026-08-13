package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.WalletWithdrawal;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.WalletWithdrawalRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.WalletWithDrawlListDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.WalletWithdrawalDetailDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.WalletWithDrawlServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletWithdrawalServiceImplement implements WalletWithDrawlServiceInterface {

    private final WalletWithdrawalRepository walletWithdrawalRepository;
    private final AuthenUntil authenUntil;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getWalletWithdrawalByAdmin(int page, int size) {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("UNAUTHORIZED", "Người dùng chưa đăng nhập"));
            }

            List<WalletWithdrawal> allWithdrawals = walletWithdrawalRepository.findAll();
            return processPagination(allWithdrawals, page, size);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getWalletWithdrawalByInvestorOrSeller(int page, int size) {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("UNAUTHORIZED", "Người dùng chưa đăng nhập"));
            }

            List<WalletWithdrawal> allWithdrawals = walletWithdrawalRepository.findAll();
            List<WalletWithdrawal> userWithdrawals = allWithdrawals.stream()
                    .filter(w -> w.getWallet() != null && w.getWallet().getAccount() != null
                            && Objects.equals(w.getWallet().getAccount().getAccountId(), currentAccount.getAccountId()))
                    .toList();

            return processPagination(userWithdrawals, page, size);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getWalletWithdrawalByAdminStatus(int page, int size, String status) {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("UNAUTHORIZED", "Người dùng chưa đăng nhập"));
            }

            List<WalletWithdrawal> allWithdrawals = walletWithdrawalRepository.findAll();

            if (status != null && !status.trim().isEmpty()) {
                String searchStatus = status.trim();
                allWithdrawals = allWithdrawals.stream()
                        .filter(w -> w.getStatus() != null && w.getStatus().equalsIgnoreCase(searchStatus))
                        .toList();
            }

            return processPagination(allWithdrawals, page, size);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getWalletWithdrawalByInvestorOrSellerByStatus(int page, int size, String status) {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("UNAUTHORIZED", "Người dùng chưa đăng nhập"));
            }

            List<WalletWithdrawal> allWithdrawals = walletWithdrawalRepository.findAll();

            String formattedStatus = formatStatus(status);

            List<WalletWithdrawal> userWithdrawals = allWithdrawals.stream()
                    .filter(w -> w.getWallet() != null && w.getWallet().getAccount() != null
                            && Objects.equals(w.getWallet().getAccount().getAccountId(), currentAccount.getAccountId()))
                    .filter(w -> status == null || status.trim().isEmpty() ||
                            (w.getStatus() != null && w.getStatus().equalsIgnoreCase(status.trim())))
                    .toList();

            return processPagination(userWithdrawals, page, size);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getWalletWithdrawalDetailById(Integer walletWithdrawalId) {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("UNAUTHORIZED", "Người dùng chưa đăng nhập"));
            }

            Optional<WalletWithdrawal> withdrawalOpt = walletWithdrawalRepository.findById(walletWithdrawalId);
            if (withdrawalOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("NOT_FOUND", "Không tìm thấy yêu cầu rút tiền này"));
            }

            WalletWithdrawalDetailDTO detailDTO = convertToDetailDTO(withdrawalOpt.get());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(detailDTO, "Lấy chi tiết yêu cầu rút tiền thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    private String formatStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        String trimmed = status.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    private ResponseEntity<ApiResponse> processPagination(List<WalletWithdrawal> list, int page, int size) {
        List<WalletWithdrawal> sortedList = list.stream()
                .sorted(Comparator.comparing(
                        WalletWithdrawal::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        boolean isGetAll = (page == 0 && size == 0);

        List<WalletWithDrawlListDTO> content;
        int effectivePage = 0;
        int effectiveSize = sortedList.size();
        int totalElements = sortedList.size();
        int totalPages = 1;
        boolean isLast = true;

        if (isGetAll) {
            content = sortedList.stream()
                    .map(this::convertToListDTO)
                    .collect(Collectors.toList());
        } else {
            effectiveSize = size > 0 ? size : 20;
            effectivePage = Math.max(page, 0);

            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(effectivePage, effectiveSize);

            List<WalletWithdrawal> slicedNews = sortedList.stream()
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .toList();

            content = slicedNews.stream()
                    .map(this::convertToListDTO)
                    .collect(Collectors.toList());

            org.springframework.data.domain.Page<WalletWithDrawlListDTO> transactionPage =
                    new org.springframework.data.domain.PageImpl<>(content, pageable, totalElements);

            effectivePage = transactionPage.getNumber();
            effectiveSize = transactionPage.getSize();
            totalPages = transactionPage.getTotalPages();
            isLast = transactionPage.isLast();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("page", effectivePage);
        result.put("size", effectiveSize);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        result.put("last", isLast);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(result, "Lấy danh sách thành công"));
    }

    private WalletWithDrawlListDTO convertToListDTO(WalletWithdrawal entity) {
        WalletWithDrawlListDTO dto = new WalletWithDrawlListDTO();
        dto.setWalletWithDrawlId(entity.getWalletWithdrawalId());
        dto.setBankName(entity.getBankName());
        dto.setBankAccountNumber(entity.getBankAccountNumber());
        dto.setAmount(entity.getAmount());
        dto.setStatus(entity.getStatus());
        dto.setCreateAt(entity.getCreatedAt());
        return dto;
    }

    private WalletWithdrawalDetailDTO convertToDetailDTO(WalletWithdrawal entity) {
        WalletWithdrawalDetailDTO dto = new WalletWithdrawalDetailDTO();
        dto.setWalletWithDrawlId(entity.getWalletWithdrawalId());
        dto.setBankName(entity.getBankName());
        dto.setBankAccountNumber(entity.getBankAccountNumber());
        dto.setAmount(entity.getAmount());
        dto.setStatus(entity.getStatus());
        dto.setCreateAt(entity.getCreatedAt());
        dto.setNote(entity.getNote());
        dto.setRejectReason(entity.getReason());
        if (entity.getWallet() != null && entity.getWallet().getAccount() != null) {
            dto.setFullName(entity.getWallet().getAccount().getFull_name());
        }
        if(entity.getWallet() != null && entity.getWallet().getAccount() != null){
            dto.setPhone(entity.getWallet().getAccount().getPhone());
        }
        return dto;
    }
}