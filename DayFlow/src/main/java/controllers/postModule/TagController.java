package controllers.postModule;

import model.interaction.Tag;
import services.tag.TagService;

import java.sql.SQLException;
import java.util.List;

public class TagController {

    private final TagService tagService;

    public TagController() {
        this.tagService = new TagService();
    }

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    public String createTag(Tag tag) {
        try {
            tagService.addTag(tag);
            return "Tag created successfully with ID: " + tag.getId();
        } catch (SQLException e) {
            return "Error creating tag: " + e.getMessage();
        }
    }

    public Tag getTagById(int id) {
        try {
            return tagService.getTagById(id);
        } catch (SQLException e) {
            System.err.println("Error fetching tag: " + e.getMessage());
            return null;
        }
    }

    public List<Tag> getAllTags() {
        try {
            return tagService.getAllTags();
        } catch (SQLException e) {
            System.err.println("Error fetching tags: " + e.getMessage());
            return List.of();
        }
    }

    public String updateTag(Tag tag) {
        try {
            tagService.updateTag(tag);
            return "Tag updated successfully";
        } catch (SQLException e) {
            return "Error updating tag: " + e.getMessage();
        }
    }

    public String deleteTag(int id) {
        try {
            tagService.deleteTag(id);
            return "Tag deleted successfully";
        } catch (SQLException e) {
            return "Error deleting tag: " + e.getMessage();
        }
    }

    public String addTagToPost(int postId, int tagId) {
        try {
            tagService.attachTagToPost(postId, tagId);
            return "Tag attached to post successfully";
        } catch (SQLException e) {
            return "Error attaching tag to post: " + e.getMessage();
        }
    }

    public String removeTagFromPost(int postId, int tagId) {
        try {
            tagService.detachTagFromPost(postId, tagId);
            return "Tag removed from post successfully";
        } catch (SQLException e) {
            return "Error removing tag from post: " + e.getMessage();
        }
    }

    public List<Tag> getTagsByPost(int postId) {
        try {
            return tagService.getTagsByPost(postId);
        } catch (SQLException e) {
            System.err.println("Error fetching tags for post: " + e.getMessage());
            return List.of();
        }
    }
}
