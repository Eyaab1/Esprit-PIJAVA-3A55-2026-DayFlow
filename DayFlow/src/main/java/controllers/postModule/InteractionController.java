package controllers.postModule;

import model.CommentLike;
import model.PostLike;
import model.SavedPost;
import services.comment.CommentLikeService;
import services.post.PostLikeService;
import services.post.SavedPostService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class InteractionController {

    private final PostLikeService postLikeService;
    private final CommentLikeService commentLikeService;
    private final SavedPostService savedPostService;

    public InteractionController() {
        this.postLikeService = new PostLikeService();
        this.commentLikeService = new CommentLikeService();
        this.savedPostService = new SavedPostService();
    }

    public InteractionController(PostLikeService postLikeService, CommentLikeService commentLikeService, SavedPostService savedPostService) {
        this.postLikeService = postLikeService;
        this.commentLikeService = commentLikeService;
        this.savedPostService = savedPostService;
    }

    public String likePost(int postId, int userId) {
        try {
            PostLike postLike = new PostLike(null, postId, userId, LocalDateTime.now());
            postLikeService.insert(postLike);
            return "Post liked successfully";
        } catch (SQLException e) {
            return "Error liking post: " + e.getMessage();
        }
    }

    public String unlikePost(int postId, int userId) {
        try {
            List<PostLike> likes = postLikeService.findByPostId(postId);
            for (PostLike like : likes) {
                if (like.getUserId().equals(userId)) {
                    postLikeService.delete(like.getId());
                    return "Post unliked successfully";
                }
            }
            return "Like not found";
        } catch (SQLException e) {
            return "Error unliking post: " + e.getMessage();
        }
    }

    public String likeComment(int commentId, int userId) {
        try {
            CommentLike commentLike = new CommentLike(null, commentId, userId, LocalDateTime.now());
            commentLikeService.insert(commentLike);
            return "Comment liked successfully";
        } catch (SQLException e) {
            return "Error liking comment: " + e.getMessage();
        }
    }

    public String unlikeComment(int commentId, int userId) {
        try {
            List<CommentLike> likes = commentLikeService.findByCommentId(commentId);
            for (CommentLike like : likes) {
                if (like.getUserId().equals(userId)) {
                    commentLikeService.delete(like.getId());
                    return "Comment unliked successfully";
                }
            }
            return "Like not found";
        } catch (SQLException e) {
            return "Error unliking comment: " + e.getMessage();
        }
    }

    public String savePost(int postId, int userId) {
        try {
            SavedPost savedPost = new SavedPost(null, userId, postId, LocalDateTime.now());
            savedPostService.insert(savedPost);
            return "Post saved successfully";
        } catch (SQLException e) {
            return "Error saving post: " + e.getMessage();
        }
    }

    public String unsavePost(int postId, int userId) {
        try {
            List<SavedPost> savedPosts = savedPostService.findByUserId(userId);
            for (SavedPost saved : savedPosts) {
                if (saved.getPostId().equals(postId)) {
                    savedPostService.delete(saved.getId());
                    return "Post unsaved successfully";
                }
            }
            return "Saved post not found";
        } catch (SQLException e) {
            return "Error unsaving post: " + e.getMessage();
        }
    }

    public List<PostLike> getPostLikes(int postId) {
        try {
            return postLikeService.findByPostId(postId);
        } catch (SQLException e) {
            System.err.println("Error fetching post likes: " + e.getMessage());
            return List.of();
        }
    }

    public List<CommentLike> getCommentLikes(int commentId) {
        try {
            return commentLikeService.findByCommentId(commentId);
        } catch (SQLException e) {
            System.err.println("Error fetching comment likes: " + e.getMessage());
            return List.of();
        }
    }

    public List<SavedPost> getSavedPostsByUser(int userId) {
        try {
            return savedPostService.findByUserId(userId);
        } catch (SQLException e) {
            System.err.println("Error fetching saved posts: " + e.getMessage());
            return List.of();
        }
    }
}
