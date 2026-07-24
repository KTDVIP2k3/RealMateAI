package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PostingPackage;
import com.GSU26SE22_SU26SE002.RealMateAI.model.PostingPackageCategory;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PostingPackageCategoryRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PostingPackageRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PostingPackageDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.PostingPackageServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostingPackageServiceImplement implements PostingPackageServiceInterface {

    @Autowired
    private PostingPackageRepository postingPackageRepository;

    @Autowired
    private PostingPackageCategoryRepository postingPackageCategoryRepository;

    @Override
    public ResponseEntity<ApiResponse> getPostingPackageListIsActive() {
        try {
            List<PostingPackageDTO> postingPackageDTOList = postingPackageRepository.findAll().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && !Boolean.TRUE.equals(p.getIsDeleted()))
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            if (postingPackageDTOList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(postingPackageDTOList, "List posting package is empty"));
            }

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(postingPackageDTOList, "List posting package"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getPostingPackageListByAdmin() {
        try {
            List<PostingPackageDTO> postingPackageDTOList = postingPackageRepository.findAll().stream()
                    .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            if (postingPackageDTOList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(postingPackageDTOList, "List posting package is empty"));
            }

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(postingPackageDTOList, "List posting package"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getPostingPackageDetail(Integer id) {
        try {
            PostingPackage existPostingPackage = postingPackageRepository.findById(id)
                    .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted())).orElse(null);

            if (existPostingPackage == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Posting package id does not exist"));
            }

            PostingPackageDTO postingPackageDTO = mapToDTO(existPostingPackage);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(postingPackageDTO, "Posting package detail"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> createPostingPackage(PostingPackageRequest postingPackageRequest) {
        try {
            PostingPackageCategory category = null;
            if (postingPackageRequest.getPostingPackageCategoryId() != null) {
                category = postingPackageCategoryRepository.findById(postingPackageRequest.getPostingPackageCategoryId())
                        .orElse(null);
                if (category == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Posting package category id does not exist"));
                }
            }

            boolean existName = postingPackageRepository.findAll().stream()
                    .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                    .anyMatch(p -> p.getName().trim().equalsIgnoreCase(postingPackageRequest.getName().trim()));

            if (existName) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Posting package name exist"));
            }

            PostingPackage postingPackage = new PostingPackage();
            postingPackage.setName(postingPackageRequest.getName());
            postingPackage.setDescription(postingPackageRequest.getDescription());
            postingPackage.setPosting_package_price(postingPackageRequest.getPosting_package_price());
            postingPackage.setDuration(postingPackageRequest.getDuration());
            postingPackage.setPostingPackageCategory(category);

            if (category != null) {
                postingPackage.setPriority(category.getPriority());
            }

            postingPackage.setIsActive(true);
            postingPackage.setIsDeleted(false);
            postingPackage.setCreatedAt(LocalDateTime.now());

            postingPackageRepository.save(postingPackage);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Create posting package successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> updatePostingPackage(Integer id, PostingPackageRequest postingPackageRequest) {
        try {
            PostingPackage postingPackage = postingPackageRepository.findById(id)
                    .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted())).orElse(null);

            if (postingPackage == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Posting package id does not exist"));
            }

            PostingPackageCategory category = null;
            if (postingPackageRequest.getPostingPackageCategoryId() != null) {
                category = postingPackageCategoryRepository.findById(postingPackageRequest.getPostingPackageCategoryId())
                        .orElse(null);
                if (category == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Posting package category id does not exist"));
                }
            }

            boolean existPostingPackageName = postingPackageRepository.findAll().stream()
                    .filter(p -> !p.getPostingPackageId().equals(id) && !Boolean.TRUE.equals(p.getIsDeleted()))
                    .anyMatch(p -> p.getName().trim().equalsIgnoreCase(postingPackageRequest.getName().trim()));

            if (existPostingPackageName) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Posting package name exist"));
            }

            postingPackage.setName(postingPackageRequest.getName());
            postingPackage.setDescription(postingPackageRequest.getDescription());
            postingPackage.setPosting_package_price(postingPackageRequest.getPosting_package_price());
            postingPackage.setDuration(postingPackageRequest.getDuration());
            postingPackage.setPostingPackageCategory(category);

            if (category != null) {
                postingPackage.setPriority(category.getPriority());
            }

            postingPackage.setUpdatedAt(LocalDateTime.now());
            postingPackageRepository.save(postingPackage);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Update posting package successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> deletePostingPackage(Integer id) {
        try {
            PostingPackage existPostingPackage = postingPackageRepository.findById(id).orElse(null);
            if (existPostingPackage == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Posting package id does not exist"));
            }
            existPostingPackage.setIsActive(false);
            existPostingPackage.setIsDeleted(true);
            existPostingPackage.setUpdatedAt(LocalDateTime.now());
            postingPackageRepository.save(existPostingPackage);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Delete posting package successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> toggleActivePostingPackage(Integer id, Boolean isActive) {
        try {
            PostingPackage postingPackage = postingPackageRepository.findById(id)
                    .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted())).orElse(null);

            if (postingPackage == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Posting package id does not exist"));
            }

            postingPackage.setIsActive(isActive);
            postingPackage.setUpdatedAt(LocalDateTime.now());
            postingPackageRepository.save(postingPackage);

            String msg = isActive ? "Activated posting package successfully" : "Deactivated posting package successfully";
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, msg));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    private PostingPackageDTO mapToDTO(PostingPackage entity) {
        PostingPackageDTO dto = new PostingPackageDTO();
        dto.setPostingPackageId(entity.getPostingPackageId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setPosting_package_price(entity.getPosting_package_price());
        dto.setDuration(entity.getDuration());

        if (entity.getPostingPackageCategory() != null) {
            dto.setPostingPackageCategoryId(entity.getPostingPackageCategory().getPostingPackageCategoryId());
            dto.setPostingPackageCategoryName(entity.getPostingPackageCategory().getPostingPackageCategoryName());
            dto.setPriority(entity.getPostingPackageCategory().getPriority());
        } else {
            dto.setPriority(entity.getPriority());
        }

        return dto;
    }
}