
package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.NewsCategory;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.NewsCategoryRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.NewsCategoryRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.NewsCategoryDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NewsCategoryServiceInterface;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NewsCategoryServiceImplements implements NewsCategoryServiceInterface {

    @Autowired
    private NewsCategoryRepository newsCategoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ResponseEntity<ApiResponse> getAllCategories() {
        try {
            List<NewsCategory> categories = newsCategoryRepository.findAll();
            if (categories.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(categories, "Category list is empty"));
            }
            List<NewsCategoryDTO> dtoList = categories.stream()
                    .map(category -> modelMapper.map(category, NewsCategoryDTO.class))
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(dtoList, "Get categories successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getCategoryById(Integer id) {
        try {
            Optional<NewsCategory> categoryOpt = newsCategoryRepository.findById(id);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Category not found with id: " + id));
            }
            NewsCategoryDTO dto = modelMapper.map(categoryOpt.get(), NewsCategoryDTO.class);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(dto, "Get category successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> createCategory(NewsCategoryRequest request) {
        try {
            NewsCategory category = modelMapper.map(request, NewsCategory.class);
            category.setIsActive(true);
            category.setCreatedAt(LocalDateTime.now());
            category.setUpdatedAt(LocalDateTime.now());

            NewsCategory savedCategory = newsCategoryRepository.save(category);
            NewsCategoryDTO savedDto = modelMapper.map(savedCategory, NewsCategoryDTO.class);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(savedDto, "Create category successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> updateCategory(Integer id, NewsCategoryRequest request) {
        try {
            Optional<NewsCategory> categoryOpt = newsCategoryRepository.findById(id);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Category not found with id: " + id));
            }

            NewsCategory existingCategory = categoryOpt.get();
            existingCategory.setName(request.getName());
            existingCategory.setUpdatedAt(LocalDateTime.now());

            NewsCategory updatedCategory = newsCategoryRepository.save(existingCategory);
            NewsCategoryDTO updatedDto = modelMapper.map(updatedCategory, NewsCategoryDTO.class);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(updatedDto, "Update category successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> deleteCategory(Integer id) {
        try {
            Optional<NewsCategory> categoryOpt = newsCategoryRepository.findById(id);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Category not found with id: " + id));
            }

            newsCategoryRepository.deleteById(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Delete category successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}