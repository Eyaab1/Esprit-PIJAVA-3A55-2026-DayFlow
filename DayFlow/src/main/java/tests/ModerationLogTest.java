package tests;

import enums.PostStatus;
import model.interaction.Comment;
import model.interaction.Post;
import services.interaction.CommentService;
import services.interaction.PostService;
import services.post.moderation.ModerationRejectedException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Manual test class to verify moderation logging functionality.
 * Run this to test if moderation logs are being created in:
 * - logs/moderation-rejections.jsonl (file)
 * - moderation_incident table (database)
 */
public class ModerationLogTest {

    public static void main(String[] args) {
        System.out.println("=== Moderation Log Test ===\n");
        
        testToxicPost();
        testToxicComment();
        testCleanPost();
        testCleanComment();
        
        System.out.println("\n=== Test Complete ===");
        System.out.println("Check logs/moderation-rejections.jsonl for new entries");
        System.out.println("Check moderation_incident table in database");
    }

    /**
     * Test 1: Try to create a post with toxic content
     * Expected: Should be rejected and logged
     */
    private static void testToxicPost() {
        System.out.println("Test 1: Creating toxic post...");
        
        Post toxicPost = new Post();
        toxicPost.setTitle("Test Toxic Post");
        toxicPost.setContent("fuck this shit"); // Toxic content
        toxicPost.setCreatedById(1); // Make sure this user exists in your DB
        toxicPost.setStatus(PostStatus.PUBLISHED);
        toxicPost.setSlug("test-toxic-post-" + System.currentTimeMillis());
        toxicPost.setCreatedAt(LocalDateTime.now());
        toxicPost.setViewCount(0);
        toxicPost.setClickCount(0);
        toxicPost.setImages(List.of());

        PostService postService = new PostService();
        try {
            postService.insert(toxicPost);
            System.out.println("❌ FAILED: Post should have been rejected!");
        } catch (ModerationRejectedException e) {
            System.out.println("✅ PASSED: Post rejected as expected");
            System.out.println("   Reason: " + e.getMessage());
            System.out.println("   Check logs/moderation-rejections.jsonl for new entry");
        } catch (SQLException e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    /**
     * Test 2: Try to create a comment with toxic content
     * Expected: Should be rejected and logged
     */
    private static void testToxicComment() {
        System.out.println("Test 2: Creating toxic comment...");
        
        Comment toxicComment = new Comment();
        toxicComment.setContent("you're an idiot"); // Toxic content
        toxicComment.setPostId(1); // Make sure this post exists in your DB
        toxicComment.setCommenterId(1); // Make sure this user exists in your DB
        toxicComment.setCreatedAt(LocalDateTime.now());

        CommentService commentService = new CommentService();
        try {
            commentService.insert(toxicComment);
            System.out.println("❌ FAILED: Comment should have been rejected!");
        } catch (ModerationRejectedException e) {
            System.out.println("✅ PASSED: Comment rejected as expected");
            System.out.println("   Reason: " + e.getMessage());
            System.out.println("   Check logs/moderation-rejections.jsonl for new entry");
        } catch (SQLException e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    /**
     * Test 3: Create a post with clean content
     * Expected: Should succeed without logging
     */
    private static void testCleanPost() {
        System.out.println("Test 3: Creating clean post...");
        
        Post cleanPost = new Post();
        cleanPost.setTitle("My Healthy Lifestyle Journey");
        cleanPost.setContent("I started eating better and exercising daily. Feeling great!");
        cleanPost.setCreatedById(1);
        cleanPost.setStatus(PostStatus.PUBLISHED);
        cleanPost.setSlug("healthy-lifestyle-" + System.currentTimeMillis());
        cleanPost.setCreatedAt(LocalDateTime.now());
        cleanPost.setViewCount(0);
        cleanPost.setClickCount(0);
        cleanPost.setImages(List.of());

        PostService postService = new PostService();
        try {
            postService.insert(cleanPost);
            System.out.println("✅ PASSED: Clean post created successfully");
            System.out.println("   Post ID: " + cleanPost.getId());
            System.out.println("   No moderation log should be created");
            
            // Clean up - delete the test post
            postService.delete(cleanPost.getId());
            System.out.println("   Test post cleaned up");
        } catch (ModerationRejectedException e) {
            System.out.println("❌ FAILED: Clean post should not be rejected!");
            System.out.println("   Reason: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    /**
     * Test 4: Create a comment with clean content
     * Expected: Should succeed without logging
     */
    private static void testCleanComment() {
        System.out.println("Test 4: Creating clean comment...");
        
        Comment cleanComment = new Comment();
        cleanComment.setContent("Great post! Thanks for sharing your experience.");
        cleanComment.setPostId(1); // Make sure this post exists
        cleanComment.setCommenterId(1);
        cleanComment.setCreatedAt(LocalDateTime.now());

        CommentService commentService = new CommentService();
        try {
            commentService.insert(cleanComment);
            System.out.println("✅ PASSED: Clean comment created successfully");
            System.out.println("   Comment ID: " + cleanComment.getId());
            System.out.println("   No moderation log should be created");
            
            // Clean up - delete the test comment
            commentService.delete(cleanComment.getId());
            System.out.println("   Test comment cleaned up");
        } catch (ModerationRejectedException e) {
            System.out.println("❌ FAILED: Clean comment should not be rejected!");
            System.out.println("   Reason: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
}
