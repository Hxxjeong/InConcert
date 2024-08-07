package com.inconcert.domain.post.service;

import com.inconcert.domain.category.entity.Category;
import com.inconcert.domain.category.entity.PostCategory;
import com.inconcert.domain.category.repository.CategoryRepository;
import com.inconcert.domain.category.repository.PostCategoryRepository;
import com.inconcert.domain.crawling.service.PerformanceService;
import com.inconcert.domain.post.dto.PostDto;
import com.inconcert.domain.post.entity.Post;
import com.inconcert.domain.post.repository.InfoRepository;
import com.inconcert.domain.post.util.DateUtil;
import com.inconcert.domain.user.service.UserService;
import com.inconcert.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InfoService {
    private final InfoRepository infoRepository;
    private final PostCategoryRepository postCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final PerformanceService performanceService;

    @Transactional(readOnly = true)
    public List<PostDto> getAllInfoPostsByPostCategory(String postCategoryTitle) {
        List<Post> posts = switch (postCategoryTitle) {
            case "musical" -> infoRepository.findPostsByPostCategoryTitle("musical");
            case "concert" -> infoRepository.findPostsByPostCategoryTitle("concert");
            case "theater" -> infoRepository.findPostsByPostCategoryTitle("theater");
            case "etc" -> infoRepository.findPostsByPostCategoryTitle("etc");
            default -> throw new PostCategoryNotFoundException(ExceptionMessage.POST_CATEGORY_NOT_FOUND.getMessage());
        };
        List<PostDto> postDtos = getPostDtos(posts);
        return postDtos;
    }

    // postId를 가지고 게시물을 조회해서 postDto을 리턴해주는 메소드
    @Transactional
    public PostDto getPostById(Long postId) {
        Post findPost = infoRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(ExceptionMessage.POST_NOT_FOUND.getMessage()));

        // viewCount 증가
        findPost.incrementViewCount();
        Post post = infoRepository.save(findPost);

        return PostDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .thumbnailUrl(post.getThumbnailUrl())  // 썸네일 URL 추가
                .postCategory(post.getPostCategory())
                .nickname(post.getUser().getNickname())
                .viewCount(post.getViewCount())
                .matchCount(post.getMatchCount())
                .endDate(post.getEndDate())
                .commentCount(post.getComments().size())
                .comments(post.getComments())
                .likeCount(post.getLikes().size())
                .isNew(Duration.between(post.getCreatedAt(), LocalDateTime.now()).toDays() < 1)
                .createdAt(post.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PostDto> findByKeywordAndFilters(String postCategoryTitle, String keyword, String period, String type) {
        LocalDateTime startDate = DateUtil.getStartDate(period);
        LocalDateTime endDate = DateUtil.getCurrentDate();

        // 검색 로직 구현 (기간 필터링, 타입 필터링 등)
        List<Post> posts = infoRepository.findByKeywordAndFilters(postCategoryTitle, keyword, startDate, endDate, type);
        List<PostDto> postDtos = getPostDtos(posts);

        return postDtos;
    }

    @Transactional
    public void save(PostDto postDto){

        // 게시물 작성 폼에서 가져온 postCategory 제목으로 조회해서 PostCategory 리스트 생성
        List<PostCategory> postCategories = postCategoryRepository.findByTitle(postDto.getPostCategoryTitle());

        // 게시물 작성 폼에서 가져온 Category 제목으로 조회해서 Category 객체 생성
        Category category = categoryRepository.findByTitle(postDto.getCategoryTitle())
                .orElseThrow(() -> new CategoryNotFoundException(ExceptionMessage.CATEGORY_NOT_FOUND.getMessage()));

        // 적절한 PostCategory 찾기
        PostCategory postCategory = postCategories.stream()
                .filter(pc -> pc.getCategory().equals(category))
                .findFirst()
                .orElseThrow(() -> new PostCategoryNotFoundException(ExceptionMessage.POST_CATEGORY_COMBINATION_NOT_FOUND.getMessage()));

        // 생성한 Category를 builder를 통해 연관관계 주입
        PostCategory updatedPostCategory = postCategory.builder()
                .id(postCategory.getId())
                .title(postCategory.getTitle())
                .category(category)
                .build();

        postDto.setUser(userService.getAuthenticatedUser()
                .orElseThrow(() -> new UserNotFoundException(ExceptionMessage.USER_NOT_FOUND.getMessage())));

        // 주입된 PostCategory를 Post에 저장
        Post post = PostDto.toEntity(postDto, updatedPostCategory);

        infoRepository.save(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = infoRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(ExceptionMessage.POST_NOT_FOUND.getMessage()));
        infoRepository.delete(post);
    }

    private static List<PostDto> getPostDtos(List<Post> posts) {
        List<PostDto> postDtos = new ArrayList<>();
        for (Post post : posts) {
            PostDto postDto = PostDto.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .thumbnailUrl(post.getThumbnailUrl())
                    .postCategory(post.getPostCategory())
                    .nickname(post.getUser().getNickname())
                    .viewCount(post.getViewCount()+1)
                    .commentCount(post.getComments().size())
                    .likeCount(post.getLikes().size())
                    .isNew(Duration.between(post.getCreatedAt(), LocalDateTime.now()).toDays() < 1)
                    .createdAt(post.getCreatedAt())
                    .build();
            postDtos.add(postDto);
        }
        return postDtos;
    }

    @Transactional
    public void crawlAndSavePosts(String type) {
        performanceService.crawlPerformances(type);
    }

}