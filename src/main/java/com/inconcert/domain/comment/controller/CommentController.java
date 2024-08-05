package com.inconcert.domain.comment.controller;

import com.inconcert.domain.comment.dto.CommentCreateForm;
import com.inconcert.domain.comment.dto.CommentDto;
import com.inconcert.domain.comment.service.CommentService;
import com.inconcert.domain.user.entity.User;
import com.inconcert.domain.user.service.UserService;
import com.inconcert.global.exception.PostCategoryNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// @Qualifier 어노테이션은 의존성 주입 시 어떤 Bean을 주입할지 지정하는 용도로 사용
// 같은 타입의 여러 Bean이 있기 때문에 특정 Bean을 선택하기 위해서 사용
@Controller
@RequestMapping("/{categoryTitle}/{postCategoryTitle}/{postId}/comments")
public class CommentController {
    private final Map<String, CommentService> commentServices;
    private final UserService userService;

    @Autowired
    public CommentController(@Qualifier("infoCommentService") CommentService infoCommentService,
                             @Qualifier("matchCommentService") CommentService matchCommentService,
                             @Qualifier("reviewCommentService") CommentService reviewCommentService,
                             @Qualifier("transferCommentService") CommentService transferCommentService,
                             UserService userService) {
        this.commentServices = new HashMap<>();
        this.commentServices.put("info", infoCommentService);
        this.commentServices.put("match", matchCommentService);
        this.commentServices.put("review", reviewCommentService);
        this.commentServices.put("transfer", transferCommentService);
        this.commentServices.put("musical", infoCommentService);
        this.commentServices.put("concert", infoCommentService);
        this.commentServices.put("theater", infoCommentService);
        this.commentServices.put("etc", infoCommentService);
        this.userService = userService;
    }

    private CommentService getService(String postCategoryTitle) {
        CommentService service = this.commentServices.get(postCategoryTitle.toLowerCase());
        if (service == null) {
            throw new PostCategoryNotFoundException("Invalid post category title: " + postCategoryTitle);
        }
        return service;
    }

//    등록순/조회순 테스트를 위해 잠시 주석 처리
//    @GetMapping
//    public ResponseEntity<List<CommentDto>> getComments(@PathVariable("postCategoryTitle") String postCategoryTitle,
//                                                        @PathVariable("postId") Long postId) {
//        List<CommentDto> byPostId = getService(postCategoryTitle).findByPostId(postCategoryTitle, postId);
//        return ResponseEntity.ok(byPostId);
//    }
//
//    @GetMapping("/{commentId}")
//    public ResponseEntity<CommentDto> getComment(@PathVariable("postCategoryTitle") String postCategoryTitle,
//                                                 @PathVariable("commentId") Long commentId) {
//        CommentDto commentDto = getService(postCategoryTitle).findComment(postCategoryTitle, commentId);
//        return ResponseEntity.ok(commentDto);
//    }

    @GetMapping
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable("categoryTitle") String categoryTitle,
                                                        @PathVariable("postCategoryTitle") String postCategoryTitle,
                                                        @PathVariable("postId") Long postId,
                                                        @RequestParam(defaultValue = "created") String sort) {
        List<CommentDto> comments = getService(postCategoryTitle).findByPostId(postCategoryTitle, postId, sort);
        return ResponseEntity.ok(comments);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/edit/{commentId}")
    public String editComment(@PathVariable("categoryTitle") String categoryTitle,
                              @PathVariable("postCategoryTitle") String postCategoryTitle,
                              @PathVariable("postId") Long postId,
                              @PathVariable("commentId") Long commentId,
                              @Valid @ModelAttribute("commentForm") CommentCreateForm commentForm,
                              BindingResult bindingResult, Principal principal) {
        if (bindingResult.hasErrors()) {
            return "redirect:/" + categoryTitle + "/" + postCategoryTitle + "/" + postId;
        }

        User user = userService.findByUsername(principal.getName());
        CommentDto existingComment = getService(postCategoryTitle).findComment(postCategoryTitle, commentId);

        if (!existingComment.getUser().getUsername().equals(user.getUsername())) {
            throw new SecurityException("이 댓글을 수정할 권한이 없습니다.");
        }

        getService(postCategoryTitle).updateComment(postCategoryTitle, commentId, commentForm);
        return "redirect:/" + categoryTitle + "/" + postCategoryTitle + "/" + postId;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/delete/{commentId}")
    public String deleteComment(@PathVariable("categoryTitle") String categoryTitle,
                                @PathVariable("postCategoryTitle") String postCategoryTitle,
                                @PathVariable("postId") Long postId,
                                @PathVariable("commentId") Long id,
                                Principal principal) {
        CommentDto dto = getService(postCategoryTitle).findComment(postCategoryTitle, id);
        User user = userService.findByUsername(principal.getName());

        if (!dto.getUser().getUsername().equals(user.getUsername())) {
            throw new SecurityException("이 댓글을 삭제할 권한이 없습니다.");
        }

        getService(postCategoryTitle).deleteComment(postCategoryTitle, id);
        return "redirect:/" + categoryTitle + "/" + postCategoryTitle + "/" + postId;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/new")
    public String createComment(@PathVariable("categoryTitle") String categoryTitle,
                                @PathVariable("postCategoryTitle") String postCategoryTitle,
                                @PathVariable("postId") Long postId,
                                @Valid @ModelAttribute("createForm") CommentCreateForm commentForm,
                                BindingResult bindingResult,
//                                @RequestParam
                                Principal principal) {
        if (bindingResult.hasErrors()) {
            return "redirect:/" + categoryTitle + "/" + postCategoryTitle + "/" + postId;
        }

        User user = userService.findByUsername(principal.getName());

        // 디버깅을 위해 isSecret 값을 로그에 출력
        System.out.println("isSecret: " + commentForm.getIsSecret());

        getService(postCategoryTitle).saveComment(postCategoryTitle, postId, user, commentForm);
        return "redirect:/" + categoryTitle + "/" + postCategoryTitle + "/" + postId;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/reply/{parentId}")
    public String createReply(@PathVariable("categoryTitle") String categoryTitle,
                              @PathVariable("postCategoryTitle") String postCategoryTitle,
                              @PathVariable("postId") Long postId,
                              @PathVariable("parentId") Long parentId,
                              @Valid @ModelAttribute("commentForm") CommentCreateForm commentForm,
                              BindingResult bindingResult,
                              Principal principal) {
        if (bindingResult.hasErrors()) {
            return "redirect:/" + categoryTitle + "/" + postCategoryTitle + "/" + postId;
        }

        User user = userService.findByUsername(principal.getName());
        commentForm.setParent(parentId); // 부모 댓글 ID 설정
        getService(postCategoryTitle).reSaveComment(postCategoryTitle, postId, parentId, user, commentForm);
        return "redirect:/" + categoryTitle + "/" + postCategoryTitle + "/" + postId;
    }
}
