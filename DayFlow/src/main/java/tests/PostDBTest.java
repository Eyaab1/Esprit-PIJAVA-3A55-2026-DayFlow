package tests;

import controllers.interaction.CommentController;
import controllers.interaction.InteractionController;
import controllers.interaction.PostController;
import controllers.interaction.TagController;
import enums.PostStatus;
import model.interaction.Post;
import model.interaction.Comment;
import model.interaction.Tag;
import model.interaction.PostLike;
import model.interaction.CommentLike;
import model.interaction.SavedPost;
import org.junit.jupiter.api.*;
import services.interaction.CommentLikeService;
import services.interaction.CommentService;
import services.interaction.PostLikeService;
import services.interaction.PostService;
import services.interaction.SavedPostService;
import services.interaction.TagService;
import utils.DbConnexion;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostDBTest {

    // Services
    private static PostService postService;
    private static CommentService commentService;
    private static TagService tagService;
    private static PostLikeService postLikeService;
    private static CommentLikeService commentLikeService;
    private static SavedPostService savedPostService;

    // Controllers
    private static PostController postController;
    private static CommentController commentController;
    private static TagController tagController;
    private static InteractionController interactionController;

    // Test data IDs (stored across tests)
    private static Integer createdPostId;
    private static Integer createdCommentId;
    private static Integer createdTagId;
    private static Integer createdPostLikeId;
    private static Integer createdCommentLikeId;
    private static Integer createdSavedPostId;

    // Unique identifiers for test data
    private static String uniquePostTitle;
    private static String uniqueTagName;
    private static String uniqueCommentContent;

    @BeforeAll
    public static void setUp() {
        // Initialize services
        postService = new PostService();
        commentService = new CommentService();
        tagService = new TagService();
        postLikeService = new PostLikeService();
        commentLikeService = new CommentLikeService();
        savedPostService = new SavedPostService();

        // Initialize controllers
        postController = new PostController();
        commentController = new CommentController();
        tagController = new TagController();
        interactionController = new InteractionController();

        // Generate unique identifiers
        long timestamp = System.currentTimeMillis();
        uniquePostTitle = "Test_Post_" + timestamp;
        uniqueTagName = "Test_Tag_" + timestamp;
        uniqueCommentContent = "Test_Comment_" + timestamp;

        System.out.println("=== Starting Integration Tests ===");
        System.out.println("Unique Post Title: " + uniquePostTitle);
        System.out.println("Unique Tag Name: " + uniqueTagName);
    }

    @AfterAll
    public static void tearDown() {
        // Close database connection
        DbConnexion.shutdown();
        System.out.println("=== Integration Tests Completed ===");
    }


    @Test
    @Order(1)
    public void testCreatePost() throws SQLException {
        System.out.println("\n[TEST 1] Creating a new post...");

        Post post = new Post(
                null,                           // id
                uniquePostTitle,                // title
                "This is test content for integration testing", // content
                LocalDateTime.now(),            // createdAt
                1,                              // createdById
                PostStatus.PUBLISHED,           // status
                List.of("test_image1.jpg", "test_image2.jpg"), // images
                null,                           // scheduledAt
                null,                           // updatedAt
                uniquePostTitle.toLowerCase().replace(" ", "-"), // slug
                null,                           // deletedAt
                0,                              // viewCount
                0                               // clickCount
        );

        postService.createPost(post);
        createdPostId = post.getId();

        assertNotNull(createdPostId, "Post ID should not be null after creation");
        assertTrue(createdPostId > 0, "Post ID should be greater than 0");
        System.out.println("✓ Post created with ID: " + createdPostId);
    }

    @Test
    @Order(2)
    public void testGetPostById() throws SQLException {
        System.out.println("\n[TEST 2] Fetching post by ID...");

        Post post = postService.getPostById(createdPostId);

        assertNotNull(post, "Post should not be null");
        assertEquals(createdPostId, post.getId(), "Post ID should match");
        assertEquals(uniquePostTitle, post.getTitle(), "Post title should match");
        assertEquals(PostStatus.PUBLISHED, post.getStatus(), "Post status should be PUBLISHED");
        assertNotNull(post.getImages(), "Images list should not be null");
        assertEquals(2, post.getImages().size(), "Should have 2 images");
        System.out.println("✓ Post fetched successfully: " + post.getTitle());
    }

    @Test
    @Order(3)
    public void testGetAllPosts() throws SQLException {
        System.out.println("\n[TEST 3] Fetching all posts...");

        List<Post> posts = postService.getAllPosts();

        assertNotNull(posts, "Posts list should not be null");
        assertTrue(posts.size() > 0, "Should have at least one post");

        boolean foundTestPost = posts.stream()
                .anyMatch(p -> p.getId().equals(createdPostId));
        assertTrue(foundTestPost, "Should find our test post in the list");
        System.out.println("✓ Fetched " + posts.size() + " posts");
    }

    @Test
    @Order(4)
    public void testUpdatePost() throws SQLException {
        System.out.println("\n[TEST 4] Updating post...");

        Post post = postService.getPostById(createdPostId);
        String updatedTitle = uniquePostTitle + "_UPDATED";
        post.setTitle(updatedTitle);
        post.setUpdatedAt(LocalDateTime.now());

        postService.updatePost(post);

        Post updatedPost = postService.getPostById(createdPostId);
        assertEquals(updatedTitle, updatedPost.getTitle(), "Title should be updated");
        assertNotNull(updatedPost.getUpdatedAt(), "Updated timestamp should not be null");
        System.out.println("✓ Post updated successfully");
    }

    // ==================== TAG SERVICE TESTS ====================

    @Test
    @Order(5)
    public void testCreateTag() throws SQLException {
        System.out.println("\n[TEST 5] Creating a new tag...");

        Tag tag = new Tag(
                null,                           // id
                uniqueTagName,                  // name
                uniqueTagName.toLowerCase().replace(" ", "-"), // slug
                LocalDateTime.now(),            // createdAt
                0                               // usageCount
        );

        tagService.addTag(tag);
        createdTagId = tag.getId();

        assertNotNull(createdTagId, "Tag ID should not be null after creation");
        assertTrue(createdTagId > 0, "Tag ID should be greater than 0");
        System.out.println("✓ Tag created with ID: " + createdTagId);
    }

    @Test
    @Order(6)
    public void testGetAllTags() throws SQLException {
        System.out.println("\n[TEST 6] Fetching all tags...");

        List<Tag> tags = tagService.getAllTags();

        assertNotNull(tags, "Tags list should not be null");
        assertTrue(tags.size() > 0, "Should have at least one tag");

        boolean foundTestTag = tags.stream()
                .anyMatch(t -> t.getId().equals(createdTagId));
        assertTrue(foundTestTag, "Should find our test tag in the list");
        System.out.println("✓ Fetched " + tags.size() + " tags");
    }

    @Test
    @Order(7)
    public void testAttachTagToPost() throws SQLException {
        System.out.println("\n[TEST 7] Attaching tag to post...");

        tagService.attachTagToPost(createdPostId, createdTagId);

        List<Tag> postTags = tagService.getTagsByPost(createdPostId);
        assertNotNull(postTags, "Post tags should not be null");
        assertTrue(postTags.size() > 0, "Post should have at least one tag");

        boolean foundTag = postTags.stream()
                .anyMatch(t -> t.getId().equals(createdTagId));
        assertTrue(foundTag, "Should find our test tag attached to the post");
        System.out.println("✓ Tag attached to post successfully");
    }

    @Test
    @Order(8)
    public void testGetTagsByPost() throws SQLException {
        System.out.println("\n[TEST 8] Fetching tags by post...");

        List<Tag> tags = tagService.getTagsByPost(createdPostId);

        assertNotNull(tags, "Tags list should not be null");
        assertTrue(tags.size() > 0, "Should have at least one tag");
        assertEquals(uniqueTagName, tags.get(0).getName(), "Tag name should match");
        System.out.println("✓ Fetched " + tags.size() + " tag(s) for post");
    }

    // ==================== COMMENT SERVICE TESTS ====================

    @Test
    @Order(9)
    public void testAddComment() throws SQLException {
        System.out.println("\n[TEST 9] Adding a comment...");

        Comment comment = new Comment(
                null,
                uniqueCommentContent,
                LocalDateTime.now(),
                createdPostId,
                1, // Assuming user with ID 1 exists
                null
        );

        commentService.addComment(comment);
        createdCommentId = comment.getId();

        assertNotNull(createdCommentId, "Comment ID should not be null after creation");
        assertTrue(createdCommentId > 0, "Comment ID should be greater than 0");
        System.out.println("✓ Comment created with ID: " + createdCommentId);
    }

    @Test
    @Order(10)
    public void testGetCommentsByPost() throws SQLException {
        System.out.println("\n[TEST 10] Fetching comments by post...");

        List<Comment> comments = commentService.getCommentsByPost(createdPostId);

        assertNotNull(comments, "Comments list should not be null");
        assertTrue(comments.size() > 0, "Should have at least one comment");

        boolean foundTestComment = comments.stream()
                .anyMatch(c -> c.getId().equals(createdCommentId));
        assertTrue(foundTestComment, "Should find our test comment");
        System.out.println("✓ Fetched " + comments.size() + " comment(s)");
    }

    @Test
    @Order(11)
    public void testGetCommentById() throws SQLException {
        System.out.println("\n[TEST 11] Fetching comment by ID...");

        Comment comment = commentService.getCommentById(createdCommentId);

        assertNotNull(comment, "Comment should not be null");
        assertEquals(createdCommentId, comment.getId(), "Comment ID should match");
        assertEquals(uniqueCommentContent, comment.getContent(), "Comment content should match");
        assertEquals(createdPostId, comment.getPostId(), "Post ID should match");
        System.out.println("✓ Comment fetched successfully");
    }

    // ==================== POST LIKE SERVICE TESTS ====================

    @Test
    @Order(12)
    public void testCreatePostLike() throws SQLException {
        System.out.println("\n[TEST 12] Creating a post like...");

        PostLike postLike = new PostLike(
                null,           // id
                createdPostId,  // postId
                1               // likerId
        );

        postLikeService.insert(postLike);
        createdPostLikeId = postLike.getId();

        assertNotNull(createdPostLikeId, "Post like ID should not be null");
        assertTrue(createdPostLikeId > 0, "Post like ID should be greater than 0");
        System.out.println("✓ Post like created with ID: " + createdPostLikeId);
    }

    @Test
    @Order(13)
    public void testGetPostLikesByPostId() throws SQLException {
        System.out.println("\n[TEST 13] Fetching post likes...");

        List<PostLike> likes = postLikeService.findByPostId(createdPostId);

        assertNotNull(likes, "Post likes list should not be null");
        assertTrue(likes.size() > 0, "Should have at least one like");

        boolean foundTestLike = likes.stream()
                .anyMatch(l -> l.getId().equals(createdPostLikeId));
        assertTrue(foundTestLike, "Should find our test like");
        System.out.println("✓ Fetched " + likes.size() + " like(s)");
    }

    // ==================== COMMENT LIKE SERVICE TESTS ====================

    @Test
    @Order(14)
    public void testCreateCommentLike() throws SQLException {
        System.out.println("\n[TEST 14] Creating a comment like...");

        CommentLike commentLike = new CommentLike(
                null,               // id
                LocalDateTime.now(), // createdAt
                createdCommentId,   // commentId
                1                   // userId
        );

        commentLikeService.insert(commentLike);
        createdCommentLikeId = commentLike.getId();

        assertNotNull(createdCommentLikeId, "Comment like ID should not be null");
        assertTrue(createdCommentLikeId > 0, "Comment like ID should be greater than 0");
        System.out.println("✓ Comment like created with ID: " + createdCommentLikeId);
    }

    @Test
    @Order(15)
    public void testGetCommentLikesByCommentId() throws SQLException {
        System.out.println("\n[TEST 15] Fetching comment likes...");

        List<CommentLike> likes = commentLikeService.findByCommentId(createdCommentId);

        assertNotNull(likes, "Comment likes list should not be null");
        assertTrue(likes.size() > 0, "Should have at least one like");

        boolean foundTestLike = likes.stream()
                .anyMatch(l -> l.getId().equals(createdCommentLikeId));
        assertTrue(foundTestLike, "Should find our test like");
        System.out.println("✓ Fetched " + likes.size() + " like(s)");
    }

    // ==================== SAVED POST SERVICE TESTS ====================

    @Test
    @Order(16)
    public void testCreateSavedPost() throws SQLException {
        System.out.println("\n[TEST 16] Creating a saved post...");

        SavedPost savedPost = new SavedPost(
                null,               // id
                LocalDateTime.now(), // savedAt
                1,                  // userId
                createdPostId       // postId
        );

        savedPostService.insert(savedPost);
        createdSavedPostId = savedPost.getId();

        assertNotNull(createdSavedPostId, "Saved post ID should not be null");
        assertTrue(createdSavedPostId > 0, "Saved post ID should be greater than 0");
        System.out.println("✓ Saved post created with ID: " + createdSavedPostId);
    }

    @Test
    @Order(17)
    public void testGetSavedPostsByUserId() throws SQLException {
        System.out.println("\n[TEST 17] Fetching saved posts...");

        List<SavedPost> savedPosts = savedPostService.findByUserId(1);

        assertNotNull(savedPosts, "Saved posts list should not be null");
        assertTrue(savedPosts.size() > 0, "Should have at least one saved post");

        boolean foundTestSavedPost = savedPosts.stream()
                .anyMatch(sp -> sp.getId().equals(createdSavedPostId));
        assertTrue(foundTestSavedPost, "Should find our test saved post");
        System.out.println("✓ Fetched " + savedPosts.size() + " saved post(s)");
    }

    // ==================== CONTROLLER TESTS ====================

    @Test
    @Order(18)
    public void testPostController() throws SQLException {
        System.out.println("\n[TEST 18] Testing PostController...");

        // Test getPostById
        Post post = postController.getPostById(createdPostId);
        assertNotNull(post, "Post should not be null");
        assertEquals(createdPostId, post.getId(), "Post ID should match");

        // Test getAllPosts
        List<Post> posts = postController.getAllPosts();
        assertNotNull(posts, "Posts list should not be null");
        assertTrue(posts.size() > 0, "Should have at least one post");

        // Test getFullPost
        PostController.PostWithDetails fullPost = postController.getFullPost(createdPostId);
        assertNotNull(fullPost, "Full post should not be null");
        assertNotNull(fullPost.post, "Post should not be null");
        assertNotNull(fullPost.comments, "Comments should not be null");
        assertNotNull(fullPost.tags, "Tags should not be null");
        assertTrue(fullPost.comments.size() > 0, "Should have comments");
        assertTrue(fullPost.tags.size() > 0, "Should have tags");

        System.out.println("✓ PostController tests passed");
    }

    @Test
    @Order(19)
    public void testCommentController() {
        System.out.println("\n[TEST 19] Testing CommentController...");

        // Test getCommentsByPost
        List<Comment> comments = commentController.getCommentsByPost(createdPostId);
        assertNotNull(comments, "Comments list should not be null");
        assertTrue(comments.size() > 0, "Should have at least one comment");

        // Test getCommentById
        Comment comment = commentController.getCommentById(createdCommentId);
        assertNotNull(comment, "Comment should not be null");
        assertEquals(createdCommentId, comment.getId(), "Comment ID should match");

        System.out.println("✓ CommentController tests passed");
    }

    @Test
    @Order(20)
    public void testTagController() {
        System.out.println("\n[TEST 20] Testing TagController...");

        // Test getAllTags
        List<Tag> tags = tagController.getAllTags();
        assertNotNull(tags, "Tags list should not be null");
        assertTrue(tags.size() > 0, "Should have at least one tag");

        // Test getTagsByPost
        List<Tag> postTags = tagController.getTagsByPost(createdPostId);
        assertNotNull(postTags, "Post tags should not be null");
        assertTrue(postTags.size() > 0, "Should have at least one tag");

        // Test getTagById
        Tag tag = tagController.getTagById(createdTagId);
        assertNotNull(tag, "Tag should not be null");
        assertEquals(createdTagId, tag.getId(), "Tag ID should match");

        System.out.println("✓ TagController tests passed");
    }

    @Test
    @Order(21)
    public void testInteractionController() {
        System.out.println("\n[TEST 21] Testing InteractionController...");

        // Test getPostLikes
        List<PostLike> postLikes = interactionController.getPostLikes(createdPostId);
        assertNotNull(postLikes, "Post likes should not be null");
        assertTrue(postLikes.size() > 0, "Should have at least one post like");

        // Test getCommentLikes
        List<CommentLike> commentLikes = interactionController.getCommentLikes(createdCommentId);
        assertNotNull(commentLikes, "Comment likes should not be null");
        assertTrue(commentLikes.size() > 0, "Should have at least one comment like");

        // Test getSavedPostsByUser
        List<SavedPost> savedPosts = interactionController.getSavedPostsByUser(1);
        assertNotNull(savedPosts, "Saved posts should not be null");
        assertTrue(savedPosts.size() > 0, "Should have at least one saved post");

        System.out.println("✓ InteractionController tests passed");
    }

    // ==================== CLEANUP TESTS ====================

    @Test
    @Order(22)
    public void testDeleteCommentLike() throws SQLException {
        System.out.println("\n[TEST 22] Deleting comment like...");

        commentLikeService.delete(createdCommentLikeId);

        CommentLike deletedLike = commentLikeService.findById(createdCommentLikeId);
        assertNull(deletedLike, "Comment like should be deleted");
        System.out.println("✓ Comment like deleted");
    }

    @Test
    @Order(23)
    public void testDeletePostLike() throws SQLException {
        System.out.println("\n[TEST 23] Deleting post like...");

        postLikeService.delete(createdPostLikeId);

        PostLike deletedLike = postLikeService.findById(createdPostLikeId);
        assertNull(deletedLike, "Post like should be deleted");
        System.out.println("✓ Post like deleted");
    }

    @Test
    @Order(24)
    public void testDeleteSavedPost() throws SQLException {
        System.out.println("\n[TEST 24] Deleting saved post...");

        savedPostService.delete(createdSavedPostId);

        SavedPost deletedSavedPost = savedPostService.findById(createdSavedPostId);
        assertNull(deletedSavedPost, "Saved post should be deleted");
        System.out.println("✓ Saved post deleted");
    }

    @Test
    @Order(25)
    public void testDeleteComment() throws SQLException {
        System.out.println("\n[TEST 25] Deleting comment...");

        commentService.deleteComment(createdCommentId);

        Comment deletedComment = commentService.getCommentById(createdCommentId);
        assertNull(deletedComment, "Comment should be deleted");
        System.out.println("✓ Comment deleted");
    }

    @Test
    @Order(26)
    public void testDetachTagFromPost() throws SQLException {
        System.out.println("\n[TEST 26] Detaching tag from post...");

        tagService.detachTagFromPost(createdPostId, createdTagId);

        List<Tag> postTags = tagService.getTagsByPost(createdPostId);
        boolean tagStillAttached = postTags.stream()
                .anyMatch(t -> t.getId().equals(createdTagId));
        assertFalse(tagStillAttached, "Tag should be detached from post");
        System.out.println("✓ Tag detached from post");
    }

    @Test
    @Order(27)
    public void testDeleteTag() throws SQLException {
        System.out.println("\n[TEST 27] Deleting tag...");

        tagService.deleteTag(createdTagId);

        Tag deletedTag = tagService.getTagById(createdTagId);
        assertNull(deletedTag, "Tag should be deleted");
        System.out.println("✓ Tag deleted");
    }

    @Test
    @Order(28)
    public void testDeletePost() throws SQLException {
        System.out.println("\n[TEST 28] Deleting post...");

        postService.deletePost(createdPostId);

        Post deletedPost = postService.getPostById(createdPostId);
        assertNull(deletedPost, "Post should be deleted");
        System.out.println("✓ Post deleted");
    }

    @Test
    @Order(29)
    public void testVerifyCleanup() throws SQLException {
        System.out.println("\n[TEST 29] Verifying cleanup...");

        // Verify all test data is deleted
        assertNull(postService.getPostById(createdPostId), "Post should be deleted");
        assertNull(commentService.getCommentById(createdCommentId), "Comment should be deleted");
        assertNull(tagService.getTagById(createdTagId), "Tag should be deleted");
        assertNull(postLikeService.findById(createdPostLikeId), "Post like should be deleted");
        assertNull(commentLikeService.findById(createdCommentLikeId), "Comment like should be deleted");
        assertNull(savedPostService.findById(createdSavedPostId), "Saved post should be deleted");

        System.out.println("✓ All test data cleaned up successfully");
    }
}
