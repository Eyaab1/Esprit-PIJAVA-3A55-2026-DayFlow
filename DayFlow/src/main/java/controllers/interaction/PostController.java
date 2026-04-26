package controllers.interaction;

import model.interaction.Comment;
import model.interaction.Post;
import model.interaction.Tag;
import services.interaction.CommentService;
import services.interaction.PostService;
import services.interaction.TagService;
import services.post.moderation.ModerationRejectedException;

import java.sql.SQLException;
import java.util.List;

public class PostController {

    private final PostService postService;
    private final CommentService commentService;
    private final TagService tagService;

    public PostController() {
        this.postService = new PostService();
        this.commentService = new CommentService();
        this.tagService = new TagService();
    }

    public PostController(PostService postService) {
        this.postService = postService;
        this.commentService = new CommentService();
        this.tagService = new TagService();
    }

    public String createPost(Post post) {
        try {
            postService.createPost(post);
            return "Post created successfully with ID: " + post.getId();
        } catch (ModerationRejectedException e) {
            return e.getMessage();
        } catch (SQLException e) {
            return "Error creating post: " + e.getMessage();
        }
    }

    public Post getPostById(int id) {
        try {
            return postService.getPostById(id);
        } catch (SQLException e) {
            System.err.println("Error fetching post: " + e.getMessage());
            return null;
        }
    }

    public List<Post> getAllPosts() {
        try {
            return postService.getAllPosts();
        } catch (SQLException e) {
            System.err.println("Error fetching posts: " + e.getMessage());
            return List.of();
        }
    }

    public String updatePost(Post post) {
        try {
            postService.updatePost(post);
            return "Post updated successfully";
        } catch (ModerationRejectedException e) {
            return e.getMessage();
        } catch (SQLException e) {
            return "Error updating post: " + e.getMessage();
        }
    }

    public String deletePost(int id) {
        try {
            postService.deletePost(id);
            return "Post deleted successfully";
        } catch (SQLException e) {
            return "Error deleting post: " + e.getMessage();
        }
    }

    public PostWithDetails getFullPost(int id) {
        try {
            Post post = postService.getPostById(id);
            if (post == null) {
                return null;
            }
            List<Comment> comments = commentService.getCommentsByPost(id);
            List<Tag> tags = tagService.getTagsByPost(id);
            return new PostWithDetails(post, comments, tags);
        } catch (SQLException e) {
            System.err.println("Error fetching full post: " + e.getMessage());
            return null;
        }
    }

    public static class PostWithDetails {
        public Post post;
        public List<Comment> comments;
        public List<Tag> tags;

        public PostWithDetails(Post post, List<Comment> comments, List<Tag> tags) {
            this.post = post;
            this.comments = comments;
            this.tags = tags;
        }
    }
}
