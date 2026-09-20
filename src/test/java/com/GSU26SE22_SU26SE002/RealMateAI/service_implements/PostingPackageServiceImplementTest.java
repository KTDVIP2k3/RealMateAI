package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PostingPackage;
import com.GSU26SE22_SU26SE002.RealMateAI.model.PostingPackageCategory;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PostingPackageCategoryRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PostingPackageRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageRequest;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostingPackageServiceImplement - Posting Package Management")
class PostingPackageServiceImplementTest {

    @Mock
    private PostingPackageRepository postingPackageRepository;

    @Mock
    private PostingPackageCategoryRepository postingPackageCategoryRepository;

    @InjectMocks
    private PostingPackageServiceImplement postingPackageService;

    private PostingPackage activePackage;
    private PostingPackage inactivePackage;
    private PostingPackage deletedPackage;
    private PostingPackageCategory category;
    private PostingPackageRequest request;

    @BeforeEach
    void setUp() {
        category = new PostingPackageCategory();
        category.setPostingPackageCategoryId(1);
        category.setPostingPackageCategoryName("VIP");
        category.setPriority(new BigDecimal("1"));

        activePackage = new PostingPackage();
        activePackage.setPostingPackageId(1);
        activePackage.setName("VIP Package 1");
        activePackage.setIsActive(true);
        activePackage.setIsDeleted(false);
        activePackage.setPostingPackageCategory(category);

        inactivePackage = new PostingPackage();
        inactivePackage.setPostingPackageId(2);
        inactivePackage.setName("VIP Package 2");
        inactivePackage.setIsActive(false);
        inactivePackage.setIsDeleted(false);

        deletedPackage = new PostingPackage();
        deletedPackage.setPostingPackageId(3);
        deletedPackage.setName("Deleted Package");
        deletedPackage.setIsActive(true);
        deletedPackage.setIsDeleted(true);

        request = new PostingPackageRequest();
        request.setName("New Package");
        request.setDescription("Description");
        request.setPosting_package_price(new BigDecimal("100000"));
        request.setDuration(new BigDecimal("30"));
        request.setPostingPackageCategoryId(1);
    }

    @Nested
    @DisplayName("View Active Posting Packages")
    class ViewActivePostingPackagesTests {

        @Test
        @DisplayName("Returns active and non-deleted packages OK")
        void getPostingPackageListIsActive_valid_returnsOk() {
            when(postingPackageRepository.findAll()).thenReturn(Arrays.asList(activePackage, inactivePackage, deletedPackage));
            
            ResponseEntity<ApiResponse> response = postingPackageService.getPostingPackageListIsActive();
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("List posting package", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Empty list returns OK")
        void getPostingPackageListIsActive_empty_returnsOk() {
            when(postingPackageRepository.findAll()).thenReturn(Collections.emptyList());
            
            ResponseEntity<ApiResponse> response = postingPackageService.getPostingPackageListIsActive();
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("List posting package is empty", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getPostingPackageListIsActive_exception_returnsServerError() {
            when(postingPackageRepository.findAll()).thenThrow(new RuntimeException("DB error"));
            
            ResponseEntity<ApiResponse> response = postingPackageService.getPostingPackageListIsActive();
            
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("View Posting Package Details")
    class ViewPostingPackageDetailsTests {

        @Test
        @DisplayName("Existed ID returns OK")
        void getPostingPackageDetail_existedId_returnsOk() {
            when(postingPackageRepository.findById(1)).thenReturn(Optional.of(activePackage));
            
            ResponseEntity<ApiResponse> response = postingPackageService.getPostingPackageDetail(1);
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Posting package detail", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent ID returns NOT_FOUND")
        void getPostingPackageDetail_nonExistentId_returnsNotFound() {
            when(postingPackageRepository.findById(99)).thenReturn(Optional.empty());
            
            ResponseEntity<ApiResponse> response = postingPackageService.getPostingPackageDetail(99);
            
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Posting package id does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getPostingPackageDetail_exception_returnsServerError() {
            when(postingPackageRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));
            
            ResponseEntity<ApiResponse> response = postingPackageService.getPostingPackageDetail(1);
            
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("View Posting Packages (Admin)")
    class ViewPostingPackagesAdminTests {

        @Test
        @DisplayName("Returns non-deleted packages OK")
        void getPostingPackageListByAdmin_valid_returnsOk() {
            when(postingPackageRepository.findAll()).thenReturn(Arrays.asList(activePackage, inactivePackage, deletedPackage));
            
            ResponseEntity<ApiResponse> response = postingPackageService.getPostingPackageListByAdmin();
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("List posting package", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Empty list returns OK")
        void getPostingPackageListByAdmin_empty_returnsOk() {
            when(postingPackageRepository.findAll()).thenReturn(Collections.singletonList(deletedPackage));
            
            ResponseEntity<ApiResponse> response = postingPackageService.getPostingPackageListByAdmin();
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("List posting package is empty", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getPostingPackageListByAdmin_exception_returnsServerError() {
            when(postingPackageRepository.findAll()).thenThrow(new RuntimeException("DB error"));
            
            ResponseEntity<ApiResponse> response = postingPackageService.getPostingPackageListByAdmin();
            
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Create Posting Package")
    class CreatePostingPackageTests {

        @Test
        @DisplayName("Valid request returns OK")
        void createPostingPackage_valid_returnsOk() {
            when(postingPackageCategoryRepository.findById(1)).thenReturn(Optional.of(category));
            when(postingPackageRepository.findAll()).thenReturn(Collections.emptyList());
            when(postingPackageRepository.save(any())).thenReturn(activePackage);

            ResponseEntity<ApiResponse> response = postingPackageService.createPostingPackage(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Create posting package successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent category returns BAD_REQUEST")
        void createPostingPackage_nonExistentCategory_returnsBadRequest() {
            when(postingPackageCategoryRepository.findById(1)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = postingPackageService.createPostingPackage(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Posting package category id does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Existed name returns BAD_REQUEST")
        void createPostingPackage_existedName_returnsBadRequest() {
            request.setName("VIP Package 1");
            when(postingPackageCategoryRepository.findById(1)).thenReturn(Optional.of(category));
            when(postingPackageRepository.findAll()).thenReturn(Collections.singletonList(activePackage));

            ResponseEntity<ApiResponse> response = postingPackageService.createPostingPackage(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Posting package name exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void createPostingPackage_exception_returnsServerError() {
            when(postingPackageCategoryRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = postingPackageService.createPostingPackage(request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @ParameterizedTest
        @DisplayName("Blank/Zero fields returns BAD_REQUEST")
        @CsvSource({
                "'', 'Desc', '1000', '30'",
                "'Name', '', '1000', '30'",
                "'Name', 'Desc', '0', '30'",
                "'Name', 'Desc', '1000', '0'",
                "'', '', '0', '0'", // Empty fields
                "'Name', 'Desc', '-1000', '-5'" // Negative numbers
        })
        void createPostingPackage_blankFields_returnsBadRequest(String name, String desc, String price, String duration) {
            request.setName(name);
            request.setDescription(desc);
            request.setPosting_package_price(new BigDecimal(price));
            request.setDuration(new BigDecimal(duration));
            
            ResponseEntity<ApiResponse> response = postingPackageService.createPostingPackage(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Update Posting Package")
    class UpdatePostingPackageTests {

        @Test
        @DisplayName("Valid request returns OK")
        void updatePostingPackage_valid_returnsOk() {
            when(postingPackageRepository.findById(1)).thenReturn(Optional.of(activePackage));
            when(postingPackageCategoryRepository.findById(1)).thenReturn(Optional.of(category));
            when(postingPackageRepository.findAll()).thenReturn(Collections.emptyList());

            ResponseEntity<ApiResponse> response = postingPackageService.updatePostingPackage(1, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Update posting package successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent package returns NOT_FOUND")
        void updatePostingPackage_notFound_returnsNotFound() {
            when(postingPackageRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = postingPackageService.updatePostingPackage(99, request);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void updatePostingPackage_exception_returnsServerError() {
            when(postingPackageRepository.findById(1)).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = postingPackageService.updatePostingPackage(1, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @ParameterizedTest
        @DisplayName("Blank or negative fields return BAD_REQUEST")
        @CsvSource({
                "'', '', '0', '0'", // Empty fields
                "'Name', 'Desc', '-1000', '-5'" // Negative numbers
        })
        void updatePostingPackage_blankFields_returnsBadRequest(String name, String desc, String price, String duration) {
            request.setName(name);
            request.setDescription(desc);
            request.setPosting_package_price(new BigDecimal(price));
            request.setDuration(new BigDecimal(duration));
            ResponseEntity<ApiResponse> response = postingPackageService.updatePostingPackage(1, request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Delete Posting Package")
    class DeletePostingPackageTests {

        @Test
        @DisplayName("Valid request returns OK")
        void deletePostingPackage_valid_returnsOk() {
            when(postingPackageRepository.findById(1)).thenReturn(Optional.of(activePackage));

            ResponseEntity<ApiResponse> response = postingPackageService.deletePostingPackage(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Delete posting package successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent package returns NOT_FOUND")
        void deletePostingPackage_notFound_returnsNotFound() {
            when(postingPackageRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = postingPackageService.deletePostingPackage(99);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void deletePostingPackage_exception_returnsServerError() {
            when(postingPackageRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = postingPackageService.deletePostingPackage(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Toggle Posting Package Status")
    class TogglePostingPackageStatusTests {

        @Test
        @DisplayName("Activate returns OK")
        void toggleActivePostingPackage_activate_returnsOk() {
            when(postingPackageRepository.findById(1)).thenReturn(Optional.of(inactivePackage));

            ResponseEntity<ApiResponse> response = postingPackageService.toggleActivePostingPackage(1, true);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Activated posting package successfully", response.getBody().getMessage());
        }
        
        @Test
        @DisplayName("Deactivate returns OK")
        void toggleActivePostingPackage_deactivate_returnsOk() {
            when(postingPackageRepository.findById(1)).thenReturn(Optional.of(activePackage));

            ResponseEntity<ApiResponse> response = postingPackageService.toggleActivePostingPackage(1, false);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Deactivated posting package successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent package returns NOT_FOUND")
        void toggleActivePostingPackage_notFound_returnsNotFound() {
            when(postingPackageRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = postingPackageService.toggleActivePostingPackage(99, true);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void toggleActivePostingPackage_exception_returnsServerError() {
            when(postingPackageRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = postingPackageService.toggleActivePostingPackage(1, true);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }
}