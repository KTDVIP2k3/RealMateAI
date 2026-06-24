package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.checkerframework.checker.units.qual.A;
import org.springframework.http.ResponseEntity;

public interface PostingPackageServiceInterface {
    ResponseEntity<ApiResponse> getPostingPackageListIsActive();

    ResponseEntity<ApiResponse> getPostingPackageListByAdmin();

    ResponseEntity<ApiResponse> getPostingPackageDetail(Integer id);

    ResponseEntity<ApiResponse> createPostingPackage(PostingPackageRequest postingPackageRequest);

    ResponseEntity<ApiResponse> updatePostingPackage(Integer id, PostingPackageRequest postingPackageRequest);

    ResponseEntity<ApiResponse> deletePostingPackage(Integer id);
}
