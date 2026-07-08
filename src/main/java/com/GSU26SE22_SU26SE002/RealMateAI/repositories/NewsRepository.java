package com.GSU26SE22_SU26SE002.RealMateAI.repositories;
import com.GSU26SE22_SU26SE002.RealMateAI.model.News;
import com.GSU26SE22_SU26SE002.RealMateAI.model.NewsCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsRepository extends JpaRepository<News, Integer> {
    Page<News> findByNewsCategoryAndIsActiveTrue(NewsCategory newsCategory, Pageable pageable);
    boolean existsByTitle(String title);
    boolean existsBySlug(String slug);
}