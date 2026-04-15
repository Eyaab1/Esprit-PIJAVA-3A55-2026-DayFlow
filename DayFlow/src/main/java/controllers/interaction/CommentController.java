package controllers.interaction;

import model.interaction.Comment;
import services.interaction.CommentService;

import java.sql.SQLException;
import java.util.List;

public class CommentController {

    private final CommentService commentService;

    public CommentController() {
        this.commentService = new CommentService();
    }

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    public String addComment(Comment comment) {
        try {
            commentService.addComment(comment);
            return "Comment added successfully with ID: " + comment.getId();
        } catch (SQLException e) {
            return "Error adding comment: " + e.getMessage();
        }
    }

    public String replyToComment(Comment comment) {
        try {
            if (comment.getParentCommentId() == null) {
                return "Error: Parent comment ID is required for replies";
            }
            commentService.addComment(comment);
            return "Reply added successfully with ID: " + comment.getId();
        } catch (SQLException e) {
            return "Error adding reply: " + e.getMessage();
        }
    }

    public List<Comment> getCommentsByPost(int postId) {
        try {
            return commentService.getCommentsByPost(postId);
        } catch (SQLException e) {
            System.err.println("Error fetching comments: " + e.getMessage());
            return List.of();
        }
    }

    public Comment getCommentById(int id) {
        try {
            return commentService.getCommentById(id);
        } catch (SQLException e) {
            System.err.println("Error fetching comment: " + e.getMessage());
            return null;
        }
    }

    public String updateComment(Comment comment) {
        try {
            commentService.updateComment(comment);
            return "Comment updated successfully";
        } catch (SQLException e) {
            return "Error updating comment: " + e.getMessage();
        }
    }

    public String deleteComment(int id) {
        try {
            commentService.deleteComment(id);
            return "Comment deleted successfully";
        } catch (SQLException e) {
            return "Error deleting comment: " + e.getMessage();
        }
    }
}
