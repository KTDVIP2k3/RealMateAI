package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Investor;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Seller;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.InvestorRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.SellerRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AdminCreateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AdminUpdateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.AdminAccountDetailDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.AdminAccountSummaryDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AdminAccountServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminAccountServiceImplement implements AdminAccountServiceInterface {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private InvestorRepository investorRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailServiceVerificationImplement emailService;

    // ======================== READ ========================

    @Override
    public ResponseEntity<ApiResponse> getAllAccounts(Pageable pageable, String role, String keyword) {
        try {
            List<Account> all = accountRepository.findAll();

            all = all.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Account::getAccountId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            // filter by role
            if (role != null && !role.isBlank()) {
                try {
                    RoleEnum roleEnum = RoleEnum.valueOf(role);
                    all = all.stream()
                            .filter(a -> a.getRole() == roleEnum)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.fail("Bad_Request", "Role không hợp lệ: " + role));
                }
            }

            // filter by keyword (username, email, fullname, phone)
            if (keyword != null && !keyword.isBlank()) {
                String kw = keyword.toLowerCase();
                all = all.stream().filter(a ->
                        (a.getUsername() != null && a.getUsername().toLowerCase().contains(kw)) ||
                                (a.getEmail() != null && a.getEmail().toLowerCase().contains(kw)) ||
                                (a.getFull_name() != null && a.getFull_name().toLowerCase().contains(kw)) ||
                                (a.getPhone() != null && a.getPhone().contains(kw))
                ).collect(Collectors.toList());
            }

            // manual pagination
            int total = all.size();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), total);
            List<Account> paged = (start >= total) ? List.of() : all.subList(start, end);

            List<AdminAccountSummaryDTO> dtos = paged.stream()
                    .map(this::toSummaryDTO)
                    .collect(Collectors.toList());

            Page<AdminAccountSummaryDTO> pageResult = new PageImpl<>(dtos, pageable, total);

            return ResponseEntity.ok(ApiResponse.success(
                    Map.of(
                            "content", pageResult.getContent(),
                            "totalElements", pageResult.getTotalElements(),
                            "totalPages", pageResult.getTotalPages(),
                            "page", pageable.getPageNumber(),
                            "size", pageable.getPageSize()
                    ),
                    "Danh sách tài khoản"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getAccountById(Integer accountId) {
        try {
            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Không tìm thấy tài khoản ID: " + accountId));
            }
            return ResponseEntity.ok(ApiResponse.success(toDetailDTO(account), "Chi tiết tài khoản"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }


    @Override
    @Transactional
    public ResponseEntity<ApiResponse> createInvestorAccount(AdminCreateAccountRequest request) {
        return createAccount(request, RoleEnum.Investor, true, false);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> createSellerAccount(AdminCreateAccountRequest request) {
        return createAccount(request, RoleEnum.Seller, false, true);
    }

    private ResponseEntity<ApiResponse> createAccount(AdminCreateAccountRequest request,
                                                      RoleEnum role,
                                                      boolean createInvestor,
                                                      boolean createSeller) {
        try {
            // validate unique username
            if (accountRepository.findByUserName(request.getUserName()).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Bad_Request", "Username đã tồn tại"));
            }
            // validate unique email
            if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Bad_Request", "Email đã tồn tại"));
            }

            Account account = new Account();
            account.setUserName(request.getUserName());
            account.setPassword(passwordEncoder.encode(request.getPassword()));
            account.setEmail(request.getEmail());
            account.setFull_name(request.getFullName());
            account.setPhone(request.getPhone());
            account.setGender(request.getGender());
            account.setBirth_date(request.getBirthDate());
            account.setRole(role);
            account.setIsActive(true);
            account.setCreateAt(LocalDateTime.now());
            account = accountRepository.save(account);

            // tạo entity Investor nếu cần
            if (createInvestor) {
                Investor investor = Investor.builder()
                        .account(account)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                investorRepository.save(investor);
            }

            // tạo entity Seller nếu cần
            if (createSeller) {
                Seller seller = Seller.builder()
                        .account(account)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                sellerRepository.save(seller);
            }

            // gửi email thông tin đăng nhập
            emailService.sendInfoAccountStaff(request.getEmail(), request.getUserName(), request.getPassword());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            Map.of("accountId", account.getAccountId(), "role", role.name()),
                            "Tạo tài khoản " + role.name() + " thành công"
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ======================== UPDATE ========================

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateAccount(Integer accountId, AdminUpdateAccountRequest request) {
        try {
            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Không tìm thấy tài khoản ID: " + accountId));
            }

            if (request.getFullName() != null) account.setFull_name(request.getFullName());
            if (request.getPhone() != null) account.setPhone(request.getPhone());
            if (request.getBirthDate() != null) account.setBirth_date(request.getBirthDate());
            if (request.getGender() != null) account.setGender(request.getGender());
            account.setUpdateAt(LocalDateTime.now());

            accountRepository.saveAndFlush(account);
            return ResponseEntity.ok(ApiResponse.success(toDetailDTO(account), "Cập nhật tài khoản thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> changeRole(Integer accountId, String roleName) {
        try {
            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Không tìm thấy tài khoản ID: " + accountId));
            }

            RoleEnum newRole;
            try {
                newRole = RoleEnum.valueOf(roleName);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Bad_Request", "Role không hợp lệ: " + roleName
                                + ". Các role hợp lệ: Investor, Seller, Staff, Admin"));
            }

            RoleEnum oldRole = account.getRole();
            account.setRole(newRole);
            account.setUpdateAt(LocalDateTime.now());
            accountRepository.save(account);

            // tạo entity profile khi đổi sang Investor/Seller nếu chưa có
            if (newRole == RoleEnum.Investor
                    && investorRepository.findByAccount_AccountId(accountId).isEmpty()) {
                Investor investor = Investor.builder()
                        .account(account)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                investorRepository.save(investor);
            }
            if (newRole == RoleEnum.Seller
                    && sellerRepository.findByAccount_AccountId(accountId).isEmpty()) {
                Seller seller = Seller.builder()
                        .account(account)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                sellerRepository.save(seller);
            }

            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("accountId", accountId, "oldRole", oldRole.name(), "newRole", newRole.name()),
                    "Đổi role thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ======================== STATUS ========================

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> setAccountStatus(Integer accountId, Boolean isActive) {
        try {
            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Không tìm thấy tài khoản ID: " + accountId));
            }
            account.setIsActive(isActive);
            account.setUpdateAt(LocalDateTime.now());
            accountRepository.save(account);

            String msg = isActive ? "Kích hoạt tài khoản thành công" : "Vô hiệu hoá tài khoản thành công";
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("accountId", accountId, "isActive", isActive), msg));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }



    // ======================== MAPPERS ========================

    private AdminAccountSummaryDTO toSummaryDTO(Account a) {
        return AdminAccountSummaryDTO.builder()
                .accountId(a.getAccountId())
                .userName(a.getUsername())
                .email(a.getEmail())
                .fullName(a.getFull_name())
                .phone(a.getPhone())
                .role(a.getRole())
                .isActive(a.getIsActive())
                .createAt(a.getCreateAt())
                .build();
    }

    private AdminAccountDetailDTO toDetailDTO(Account a) {
        return AdminAccountDetailDTO.builder()
                .accountId(a.getAccountId())
                .userName(a.getUsername())
                .email(a.getEmail())
                .fullName(a.getFull_name())
                .phone(a.getPhone())
                .avatar(a.getAvatar())
                .gender(a.getGender())
                .role(a.getRole())
                .isActive(a.getIsActive())
                .birthDate(a.getBirth_date())
                .createAt(a.getCreateAt())
                .updateAt(a.getUpdateAt())
                .hasInvestorProfile(a.getInvestor() != null)
                .hasSellerProfile(a.getSeller() != null)
                .build();
    }
}
