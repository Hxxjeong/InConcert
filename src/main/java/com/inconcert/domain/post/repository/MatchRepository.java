package com.inconcert.domain.post.repository;

import com.inconcert.domain.post.dto.PostDto;
import com.inconcert.domain.post.entity.Post;
import com.inconcert.domain.user.entity.Gender;
import com.inconcert.domain.user.entity.Mbti;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Post, Long> {
    // /home에서 동행 정보 게시물 불러오기
    @Query("SELECT new com.inconcert.domain.post.dto.PostDto(p.id, p.title, c.title, pc.title, p.thumbnailUrl, u.nickname, " +
            "p.viewCount, SIZE(p.likes), SIZE(p.comments), " +
            "CASE WHEN p.createdAt > :yesterday THEN true ELSE false END, p.createdAt) " +
            "FROM Post p " +
            "JOIN p.postCategory pc " +
            "JOIN pc.category c " +
            "JOIN p.user u " +
            "WHERE c.title = 'match' " +
            "ORDER BY p.createdAt DESC")
    List<PostDto> findPostsByCategoryTitle(Pageable pageable, @Param("yesterday") LocalDateTime yesterday);

    // /match 게시물들 카테고리에 맞게 불러오기
    @Query("SELECT new com.inconcert.domain.post.dto.PostDto(p.id, p.title, c.title, pc.title, p.thumbnailUrl, u.nickname, " +
            "p.viewCount, SIZE(p.likes), SIZE(p.comments), " +
            "CASE WHEN p.createdAt > :yesterday THEN true ELSE false END, p.createdAt) " +
            "FROM Post p " +
            "JOIN p.postCategory pc " +
            "JOIN pc.category c " +
            "JOIN p.user u " +
            "WHERE c.title = 'match' AND pc.title = :postCategoryTitle")
    List<PostDto> findPostsByPostCategoryTitle(@Param("postCategoryTitle") String postCategoryTitle,
                                               @Param("yesterday") LocalDateTime yesterday);


    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.postCategory pc " +
            "JOIN FETCH pc.category c " +
            "JOIN FETCH p.user u " +
            "WHERE c.title = 'match' AND pc.title = :postCategoryTitle " +
            "AND ((:type = 'title+content' AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)) " +
            "OR   (:type = 'title' AND p.title LIKE %:keyword%) " +
            "OR   (:type = 'content' AND p.content LIKE %:keyword%) " +
            "OR   (:type = 'author' AND u.nickname LIKE %:keyword%)) " +
            "AND p.createdAt BETWEEN :startDate AND :endDate " +
            "AND (:gender IS NULL OR u.gender = :gender) " +
            "AND (:mbti IS NULL OR u.mbti = :mbti)")
    List<Post> findByKeywordAndFilters(@Param("postCategoryTitle") String postCategoryTitle,
                                       @Param("keyword") String keyword,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       @Param("type") String type,
                                       @Param("gender") Gender gender,
                                       @Param("mbti") Mbti mbti);
}