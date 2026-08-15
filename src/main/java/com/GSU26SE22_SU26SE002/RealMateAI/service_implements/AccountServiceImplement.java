package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AdminUpdateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateAccountRequestV2;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.AccountProfileDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AccountServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AccountServiceImplement implements AccountServiceInterface {
    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AuthenUntil authenUntil;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    private CloudinaryMediaServiceImplement cloudinaryMediaServiceImplement;

    @Autowired
    EmailServiceVerificationImplement emailServiceVerificationImplement;

    @Override
    public ResponseEntity<ApiResponse> getAccountProfile() {
        try{
            if(authenUntil.getCurrentUSer() == null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", "Account does not exist"));
            }

            Account account = authenUntil.getCurrentUSer();

            AccountProfileDTO accountProfileDTO = new AccountProfileDTO();
            accountProfileDTO.setAccountId(account.getAccountId());
            accountProfileDTO.setUserName(account.getUsername());
            accountProfileDTO.setPassword(account.getPassword());
            accountProfileDTO.setEmail(account.getEmail());
            accountProfileDTO.setFullName(account.getFull_name());
            accountProfileDTO.setPhone(account.getPhone());
            accountProfileDTO.setAvatar(account.getAvatar());
            accountProfileDTO.setGenderEnum(account.getGender());
            accountProfileDTO.setRoleEnum(account.getRole());
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(accountProfileDTO, "Account Profile"));
        }catch (Exception e){
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> createAccountByAdmin(CreateAccountRequest createAccountRequest) {
        try{
            List<Account> accounts = accountRepository.findAll().stream().toList();
            boolean existName = accounts.stream().anyMatch(account -> account.getUsername().equalsIgnoreCase(createAccountRequest.getUserName()));
            boolean existEmail = accounts.stream().anyMatch(account -> account.getEmail().toLowerCase().
                    equalsIgnoreCase(createAccountRequest.getEmail().toLowerCase()));
            if(existName){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "User Name exists"));
            }

            if(existEmail){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Email exists"));
            }

            Account account = new Account();
            account.setUserName(createAccountRequest.getUserName());
            account.setPassword(new BCryptPasswordEncoder(12).encode(createAccountRequest.getPassword()));
            account.setEmail(createAccountRequest.getEmail());
            account.setFull_name(createAccountRequest.getFullName());
            account.setGender(createAccountRequest.getGender());
            account.setPhone(createAccountRequest.getPhone());
            account.setRole(RoleEnum.Staff);
            account.setBirth_date(createAccountRequest.getBirthDate());
            account.setIsActive(true);
            account.setCreateAt(LocalDateTime.now());
            accountRepository.save(account);
            emailServiceVerificationImplement.sendInfoAccountStaff(createAccountRequest.getEmail(), createAccountRequest.getUserName(), createAccountRequest.getPassword());
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Create account successfully"));
        }catch (Exception e)    {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> createAccountAdmin(CreateAccountRequest createAccountRequest) {
        try{
            List<Account> accounts = accountRepository.findAll().stream().toList();
            boolean existName = accounts.stream().anyMatch(account -> account.getUsername().equalsIgnoreCase(createAccountRequest.getUserName()));
            boolean existEmail = accounts.stream().anyMatch(account -> account.getEmail().toLowerCase().
                    equalsIgnoreCase(createAccountRequest.getEmail().toLowerCase()));
            if(existName){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "User Name exists"));
            }

            if(existEmail){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Email exists"));
            }

            Account account = new Account();
            account.setUserName(createAccountRequest.getUserName());
            account.setPassword(new BCryptPasswordEncoder(12).encode(createAccountRequest.getPassword()));
            account.setEmail(createAccountRequest.getEmail());
            account.setFull_name(createAccountRequest.getFullName());
            account.setGender(createAccountRequest.getGender());
            account.setPhone(createAccountRequest.getPhone());
            account.setRole(RoleEnum.Admin);
            account.setBirth_date(createAccountRequest.getBirthDate());
            account.setIsActive(true);
            account.setCreateAt(LocalDateTime.now());
            accountRepository.save(account);
            emailServiceVerificationImplement.sendInfoAccountStaff(createAccountRequest.getEmail(), createAccountRequest.getUserName(), createAccountRequest.getPassword());
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Create account successfully"));
        }catch (Exception e)    {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> createAccount(CreateAccountRequestV2 createAccountRequestV2) {
        try{
            List<Account> accounts = accountRepository.findAll().stream().toList();

            boolean existName = accounts.stream().anyMatch(account -> account.getUsername().toLowerCase().equalsIgnoreCase(createAccountRequestV2.getUserName().toLowerCase()));

            if(existName){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "User Name exists"));
            }

            String username = createAccountRequestV2.getUserName();

            if (username.contains(" ") || username.matches(".*\\s.*")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Tên đăng nhập không được chứa khoảng trắng"));
            }

            String validPattern = "^[a-zA-Z0-9._]+$";
            if (!username.matches(validPattern)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Tên đăng nhập không được chứa dấu tiếng Việt hoặc ký tự đặc biệt"));
            }

            if (username.length() < 3 || username.length() > 20) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Tên đăng nhập phải từ 3 đến 20 ký tự"));
            }

            String password = createAccountRequestV2.getPassword();

            if (password.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Mật khẩu không được để trống"));
            }

            if (password.contains(" ") || password.matches(".*\\s.*")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Mật khẩu không được chứa khoảng trắng"));
            }

            if (password.length() < 8 || password.length() > 32) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Mật khẩu phải từ 8 đến 32 ký tự"));
            }

            String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$";
            if (!password.matches(passwordPattern)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Mật khẩu phải bao gồm cả chữ hoa, chữ thường, số và ký tự đặc biệt"));
            }

            boolean existEmail = accountRepository.findAll().stream().anyMatch(account -> account.getEmail().toLowerCase().equalsIgnoreCase(createAccountRequestV2.getEmail()));
            String email = createAccountRequestV2.getEmail();
            RoleEnum role = createAccountRequestV2.getRole();

            boolean existEmailWithRole = accountRepository.findAll().stream()
                    .anyMatch(account -> account.getEmail().equalsIgnoreCase(email) && account.getRole() == role);

            if (existEmailWithRole) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Email này đã được đăng ký cho vai trò tương ứng"));
            }


            Account account = new Account();
            account.setUserName(createAccountRequestV2.getUserName().toLowerCase());
            account.setPassword(new BCryptPasswordEncoder(12).encode(createAccountRequestV2.getPassword()));
            account.setEmail(createAccountRequestV2.getEmail());
            account.setFull_name(createAccountRequestV2.getFullName());
            account.setGender(createAccountRequestV2.getGender());
            account.setPhone(createAccountRequestV2.getPhone());
            account.setRole(createAccountRequestV2.getRole());
            account.setBirth_date(createAccountRequestV2.getBirthDate());
            account.setIsActive(true);
            account.setCreateAt(LocalDateTime.now());
            accountRepository.save(account);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Create account successfully"));
        }catch (Exception e)    {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateAccountByAdmin(Integer accountId, AdminUpdateAccountRequest request) {
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
            return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật tài khoản thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> updateAccount(UpdateAccountRequest updateAccountRequest) {
        try{
            Account account = authenUntil.getCurrentUSer();
            if(account == null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Account does not exist"));
            }


            if(account.getAvatar() == null){
                account.setAvatar(cloudinaryMediaServiceImplement.uploadImage(updateAccountRequest.getAvatar()));
            }
            account.setFull_name(updateAccountRequest.getFullName());
            account.setGender(updateAccountRequest.getGender());
            account.setAvatar(cloudinaryMediaServiceImplement.updateImage(updateAccountRequest.getAvatar(), account.getAvatar()));
            account.setPhone(updateAccountRequest.getPhone());
            account.setUpdateAt(LocalDateTime.now());
            accountRepository.save(account);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Update account profile successfully"));

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }


}
