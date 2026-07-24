package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PostingPackageCategory;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PostingPackageCategoryRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageCategoryRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PostingPackageCategoryDetailDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PostingPackageCategoryListDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.PostingPackageCategoryServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostingPackageCategoryServiceImplements implements PostingPackageCategoryServiceInterface {

    @Autowired
    private PostingPackageCategoryRepository postingPackageCategoryRepository;

    @Override
    public ResponseEntity<ApiResponse> getAllCategories() {
        try {
            List<PostingPackageCategory> categories = postingPackageCategoryRepository.findAll();
            if (categories.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(categories, "Posting package category list is empty"));
            }

            List<PostingPackageCategoryListDTO> dtoList = categories.stream()
                    .map(this::mapToListDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(dtoList, "Get posting package categories successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getCategoryById(Integer id) {
        try {
            Optional<PostingPackageCategory> categoryOpt = postingPackageCategoryRepository.findById(id);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Posting package category not found with id: " + id));
            }

            PostingPackageCategoryDetailDTO dto = mapToDetailDTO(categoryOpt.get());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(dto, "Get posting package category successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> createCategory(PostingPackageCategoryRequest request) {
        try {
            PostingPackageCategory category = PostingPackageCategory.builder()
                    .postingPackageCategoryName(request.getPostingPackageCategoryName())
                    .description(request.getDescription())
                    .priority(request.getPriority())
                    .isActive(true)
                    .isDeleted(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            PostingPackageCategory savedCategory = postingPackageCategoryRepository.save(category);
            PostingPackageCategoryDetailDTO savedDto = mapToDetailDTO(savedCategory);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(savedDto, "Create posting package category successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> updateCategory(Integer id, PostingPackageCategoryRequest request) {
        try {
            Optional<PostingPackageCategory> categoryOpt = postingPackageCategoryRepository.findById(id);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Posting package category not found with id: " + id));
            }

            PostingPackageCategory existingCategory = categoryOpt.get();
            existingCategory.setPostingPackageCategoryName(request.getPostingPackageCategoryName());
            existingCategory.setDescription(request.getDescription());
            existingCategory.setPriority(request.getPriority());
            existingCategory.setUpdatedAt(LocalDateTime.now());

            PostingPackageCategory updatedCategory = postingPackageCategoryRepository.save(existingCategory);
            PostingPackageCategoryDetailDTO updatedDto = mapToDetailDTO(updatedCategory);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(updatedDto, "Update posting package category successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> deleteCategory(Integer id) {
        try {
            Optional<PostingPackageCategory> categoryOpt = postingPackageCategoryRepository.findById(id);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Posting package category not found with id: " + id));
            }

            PostingPackageCategory category = categoryOpt.get();

            if (category.getPostingPackageList() != null && !category.getPostingPackageList().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Cannot_Delete", "Cannot delete category because it still contains active posting packages"));
            }

            postingPackageCategoryRepository.deleteById(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Delete posting package category successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> deleteAllCategories() {
        try {
            List<PostingPackageCategory> categories = postingPackageCategoryRepository.findAll();
            if (categories.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(null, "Posting package category list is already empty"));
            }

            boolean hasDependencies = categories.stream()
                    .anyMatch(cat -> cat.getPostingPackageList() != null && !cat.getPostingPackageList().isEmpty());

            if (hasDependencies) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Cannot_Delete", "Cannot delete all categories because some categories still contain posting packages"));
            }

            postingPackageCategoryRepository.deleteAll();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Delete all posting package categories successfully (" + categories.size() + " items deleted)"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }


    private PostingPackageCategoryListDTO mapToListDTO(PostingPackageCategory category) {
        PostingPackageCategoryListDTO dto = new PostingPackageCategoryListDTO();
        dto.setPostingPackageCategoryId(category.getPostingPackageCategoryId());
        dto.setPostingPackageCategoryName(category.getPostingPackageCategoryName());
        return dto;
    }

    private PostingPackageCategoryDetailDTO mapToDetailDTO(PostingPackageCategory category) {
        PostingPackageCategoryDetailDTO dto = new PostingPackageCategoryDetailDTO();
        dto.setPostingPackageCategoryId(category.getPostingPackageCategoryId());
        dto.setPostingPackageCategoryName(category.getPostingPackageCategoryName());
        dto.setDescription(category.getDescription());
        dto.setPriority(category.getPriority());
//        dto.setIsActive(category.getIsActive());
//        dto.setIsDeleted(category.getIsDeleted());
//        dto.setCreatedAt(category.getCreatedAt());
//        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }
}