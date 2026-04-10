package tests;

import enums.PostStatus;
import model.interaction.Comment;
import model.interaction.Post;
import model.interaction.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import services.comment.CommentService;
import services.post.PostService;
import services.tag.TagService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostTest {

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @Mock
    private TagService tagService;

    private Post testPost;
    private Comment testComment;
    private Tag testTag;

    @BeforeEach
    public void setUp() {
        // Initialize test data
        testPost = new Post(
                1,
                "Test Post Title",
                "Test post content",
                LocalDateTime.now(),
                null,
                null,
                List.of("image1.jpg", "image2.jpg"),
                1,
                0,
                0,
                PostStatus.PUBLISHED,
                null
        );

        testComment = new Comment(
                1,
                "Test comment content",
                LocalDateTime.now(),
                1,
                1,
                null
        );

        testTag = new Tag(
                1,
                "Technology",
                LocalDateTime.now(),
                0
        );
    }

    // PostService Tests

    @Test
    public void testCreatePost() throws SQLException {
        // Arrange
        Post newPost = new Post(
                null,
                "New Post",
                "New content",
                LocalDateTime.now(),
                null,
                null,
                List.of(),
                1,
                0,
                0,
                PostStatus.DRAFT,
                null
        );

        doAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(1);
            return null;
        }).when(postService).createPost(any(Post.class));

        // Act
        postService.createPost(newPost);

        // Assert
        assertNotNull(newPost.getId());
        assertEquals(1, newPost.getId());
        verify(postService, times(1)).createPost(newPost);
    }

    @Test
    public void testGetPostById() throws SQLException {
        // Arrange
        when(postService.getPostById(1)).thenReturn(testPost);

        // Act
        Post result = postService.getPostById(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Test Post Title", result.getTitle());
        assertEquals(PostStatus.PUBLISHED, result.getStatus());
        verify(postService, times(1)).getPostById(1);
    }

    @Test
    public void testGetAllPosts() throws SQLException {
        // Arrange
        List<Post> posts = new ArrayList<>();
        posts.add(testPost);
        posts.add(new Post(
                2,
                "Second Post",
                "Second content",
                LocalDateTime.now(),
                null,
                null,
                List.of(),
                1,
                0,
                0,
                PostStatus.PUBLISHED,
                null
        ));

        when(postService.getAllPosts()).thenReturn(posts);

        // Act
        List<Post> result = postService.getAllPosts();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Post Title", result.get(0).getTitle());
        assertEquals("Second Post", result.get(1).getTitle());
        verify(postService, times(1)).getAllPosts();
    }

    @Test
    public void testUpdatePost() throws SQLException {
        // Arrange
        testPost.setTitle("Updated Title");
        doNothing().when(postService).updatePost(any(Post.class));

        // Act
        postService.updatePost(testPost);

        // Assert
        assertEquals("Updated Title", testPost.getTitle());
        verify(postService, times(1)).updatePost(testPost);
    }

    @Test
    public void testDeletePost() throws SQLException {
        // Arrange
        doNothing().when(postService).deletePost(anyInt());

        // Act
        postService.deletePost(1);

        // Assert
        verify(postService, times(1)).deletePost(1);
    }

    @Test
    public void testDeletePostAndVerify() throws SQLException {
        // Arrange
        when(postService.getPostById(1)).thenReturn(testPost);
        doNothing().when(postService).deletePost(1);
        when(postService.getPostById(1)).thenReturn(null);

        // Act
        Post beforeDelete = postService.getPostById(1);
        postService.deletePost(1);
        Post afterDelete = postService.getPostById(1);

        // Assert
        assertNotNull(beforeDelete);
        assertNull(afterDelete);
        verify(postService, times(1)).deletePost(1);
    }

    // CommentService Tests

    @Test
    public void testAddComment() throws SQLException {
        // Arrange
        Comment newComment = new Comment(
                null,
                "New comment",
                LocalDateTime.now(),
                1,
                1,
                null
        );

        doAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(1);
            return null;
        }).when(commentService).addComment(any(Comment.class));

        // Act
        commentService.addComment(newComment);

        // Assert
        assertNotNull(newComment.getId());
        assertEquals(1, newComment.getId());
        verify(commentService, times(1)).addComment(newComment);
    }

    @Test
    public void testGetCommentsByPost() throws SQLException {
        // Arrange
        List<Comment> comments = new ArrayList<>();
        comments.add(testComment);
        comments.add(new Comment(
                2,
                "Second comment",
                LocalDateTime.now(),
                1,
                2,
                null
        ));

        when(commentService.getCommentsByPost(1)).thenReturn(comments);

        // Act
        List<Comment> result = commentService.getCommentsByPost(1);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test comment content", result.get(0).getContent());
        assertEquals("Second comment", result.get(1).getContent());
        assertEquals(1, result.get(0).getPostId());
        verify(commentService, times(1)).getCommentsByPost(1);
    }

    @Test
    public void testGetCommentById() throws SQLException {
        // Arrange
        when(commentService.getCommentById(1)).thenReturn(testComment);

        // Act
        Comment result = commentService.getCommentById(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Test comment content", result.getContent());
        verify(commentService, times(1)).getCommentById(1);
    }

    @Test
    public void testDeleteComment() throws SQLException {
        // Arrange
        doNothing().when(commentService).deleteComment(anyInt());

        // Act
        commentService.deleteComment(1);

        // Assert
        verify(commentService, times(1)).deleteComment(1);
    }

    // TagService Tests

    @Test
    public void testAddTag() throws SQLException {
        // Arrange
        Tag newTag = new Tag(
                null,
                "Java",
                LocalDateTime.now(),
                0
        );

        doAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(1);
            return null;
        }).when(tagService).addTag(any(Tag.class));

        // Act
        tagService.addTag(newTag);

        // Assert
        assertNotNull(newTag.getId());
        assertEquals(1, newTag.getId());
        verify(tagService, times(1)).addTag(newTag);
    }

    @Test
    public void testGetAllTags() throws SQLException {
        // Arrange
        List<Tag> tags = new ArrayList<>();
        tags.add(testTag);
        tags.add(new Tag(2, "Java", LocalDateTime.now(), 5));

        when(tagService.getAllTags()).thenReturn(tags);

        // Act
        List<Tag> result = tagService.getAllTags();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Technology", result.get(0).getName());
        assertEquals("Java", result.get(1).getName());
        verify(tagService, times(1)).getAllTags();
    }

    @Test
    public void testGetTagsByPost() throws SQLException {
        // Arrange
        List<Tag> tags = new ArrayList<>();
        tags.add(testTag);
        tags.add(new Tag(2, "Programming", LocalDateTime.now(), 3));

        when(tagService.getTagsByPost(1)).thenReturn(tags);

        // Act
        List<Tag> result = tagService.getTagsByPost(1);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Technology", result.get(0).getName());
        assertEquals("Programming", result.get(1).getName());
        verify(tagService, times(1)).getTagsByPost(1);
    }

    @Test
    public void testAttachTagToPost() throws SQLException {
        // Arrange
        doNothing().when(tagService).attachTagToPost(anyInt(), anyInt());

        // Act
        tagService.attachTagToPost(1, 1);

        // Assert
        verify(tagService, times(1)).attachTagToPost(1, 1);
    }

    @Test
    public void testDetachTagFromPost() throws SQLException {
        // Arrange
        doNothing().when(tagService).detachTagFromPost(anyInt(), anyInt());

        // Act
        tagService.detachTagFromPost(1, 1);

        // Assert
        verify(tagService, times(1)).detachTagFromPost(1, 1);
    }

    @Test
    public void testDeleteTag() throws SQLException {
        // Arrange
        doNothing().when(tagService).deleteTag(anyInt());

        // Act
        tagService.deleteTag(1);

        // Assert
        verify(tagService, times(1)).deleteTag(1);
    }

    // Integration-style tests (testing multiple operations)

    @Test
    public void testCreatePostAndAddComments() throws SQLException {
        // Arrange
        Post newPost = new Post(
                null,
                "Post with comments",
                "Content",
                LocalDateTime.now(),
                null,
                null,
                List.of(),
                1,
                0,
                0,
                PostStatus.PUBLISHED,
                null
        );

        doAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(1);
            return null;
        }).when(postService).createPost(any(Post.class));

        List<Comment> comments = new ArrayList<>();
        comments.add(new Comment(1, "First comment", LocalDateTime.now(), 1, 1, null));
        comments.add(new Comment(2, "Second comment", LocalDateTime.now(), 1, 2, null));

        when(commentService.getCommentsByPost(1)).thenReturn(comments);

        // Act
        postService.createPost(newPost);
        List<Comment> postComments = commentService.getCommentsByPost(newPost.getId());

        // Assert
        assertNotNull(newPost.getId());
        assertEquals(1, newPost.getId());
        assertNotNull(postComments);
        assertEquals(2, postComments.size());
        verify(postService, times(1)).createPost(newPost);
        verify(commentService, times(1)).getCommentsByPost(1);
    }

    @Test
    public void testCreatePostAndAttachTags() throws SQLException {
        // Arrange
        Post newPost = new Post(
                null,
                "Post with tags",
                "Content",
                LocalDateTime.now(),
                null,
                null,
                List.of(),
                1,
                0,
                0,
                PostStatus.PUBLISHED,
                null
        );

        doAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(1);
            return null;
        }).when(postService).createPost(any(Post.class));

        doNothing().when(tagService).attachTagToPost(anyInt(), anyInt());

        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag(1, "Java", LocalDateTime.now(), 0));
        tags.add(new Tag(2, "Spring", LocalDateTime.now(), 0));

        when(tagService.getTagsByPost(1)).thenReturn(tags);

        // Act
        postService.createPost(newPost);
        tagService.attachTagToPost(newPost.getId(), 1);
        tagService.attachTagToPost(newPost.getId(), 2);
        List<Tag> postTags = tagService.getTagsByPost(newPost.getId());

        // Assert
        assertNotNull(newPost.getId());
        assertEquals(2, postTags.size());
        verify(postService, times(1)).createPost(newPost);
        verify(tagService, times(2)).attachTagToPost(anyInt(), anyInt());
        verify(tagService, times(1)).getTagsByPost(1);
    }
}
