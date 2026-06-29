package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PostingPackage;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PostingPackageRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PostingPackageDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.PostingPackageServiceInterface;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostingPackageOrderServiceImplement implements PostingPackageServiceInterface {
    @Autowired
    private PostingPackageRepository postingPackageRepository;

    @Override
    public ResponseEntity<ApiResponse> getPostingPackageListIsActive() {
        try{
            List<PostingPackageDTO> postingPackageDTOList = postingPackageRepository.findAll().stream()
                    .filter(postingPackage -> postingPackage.getIsActive())
                    .map(postingPackage ->
                            new PostingPackageDTO(postingPackage.getName(),
                            postingPackage.getDescription(),
                            postingPackage.getPosting_package_price(), postingPackage.getPriority()))
                    .collect(Collectors.toList());
            if(postingPackageDTOList == null || postingPackageDTOList.isEmpty()){
                return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(postingPackageDTOList, "List posting package is empty"));
            }

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(postingPackageDTOList, "List posting package"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getPostingPackageListByAdmin() {
        try{
            List<PostingPackage> postingPackages = postingPackageRepository.findAll();
            if(postingPackages.isEmpty() || postingPackages == null){
                return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(postingPackages, "List posting package is empty"));
            }

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(postingPackages, "List posting package"));

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getPostingPackageDetail(Integer id) {
        try{
            PostingPackage existPostingPackage = postingPackageRepository.findById(id).orElse(null);
            if(existPostingPackage == null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Posting package id does not exist"));
            }
            PostingPackageDTO postingPackageDTO = new PostingPackageDTO(existPostingPackage.getName(),
                    existPostingPackage.getDescription(),
                    existPostingPackage.getPosting_package_price(),
                    existPostingPackage.getPriority());
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(postingPackageDTO, "Posting package"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> createPostingPackage(PostingPackageRequest postingPackageRequest) {
        try{
            boolean existName = postingPackageRepository.findAll().stream()
                    .anyMatch(postingPackage -> postingPackage.getName().trim().toLowerCase().equals(postingPackageRequest.getName().trim().toLowerCase()));

            if(existName){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Posting package name exist"));
            }
            PostingPackage postingPackage = new PostingPackage();
            postingPackage.setName(postingPackageRequest.getName());
            postingPackage.setDescription(postingPackageRequest.getDescription());
            postingPackage.setPosting_package_price(postingPackageRequest.getPosting_package_price());
            postingPackage.setPriority(postingPackageRequest.getPriority());
            postingPackage.setIsActive(true);
            postingPackage.setCreatedAt(LocalDateTime.now());
            postingPackageRepository.save(postingPackage);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Create posting package successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> updatePostingPackage(Integer id, PostingPackageRequest postingPackageRequest) {
        try{
            PostingPackage postingPackage = postingPackageRepository.findById(id).orElse(null);
            if(postingPackage == null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Posting package id does not exist"));
            }

//            boolean existName = postingPackageRepository.findAll().stream()
//                    .anyMatch(p -> p.getName().trim().toLowerCase().equals(postingPackageRequest.getName().trim().toLowerCase()));
//
//            if(existName){
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Posting package name exist"));
//            }
            postingPackage.setName(postingPackageRequest.getName());
            postingPackage.setDescription(postingPackageRequest.getDescription());
            postingPackage.setPosting_package_price(postingPackageRequest.getPosting_package_price());
            postingPackage.setPriority(postingPackageRequest.getPriority());
            postingPackage.setUpdatedAt(LocalDateTime.now());
            postingPackageRepository.save(postingPackage);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Update posting package successfully"));

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> deletePostingPackage(Integer id) {
        try{
            PostingPackage existPostingPackage = postingPackageRepository.findById(id).orElse(null);
            if(existPostingPackage == null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Posting package id does not exist"));
            }
            existPostingPackage.setIsActive(false);
            postingPackageRepository.save(existPostingPackage);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Delete posting package successfully"));

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server error: " + e.getMessage()));
        }
    }
}
