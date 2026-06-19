package model.chatroom;

import java.time.LocalDateTime;

public class Message {

    /** Types de messages */
    public static final String TYPE_TEXT  = "TEXT";
    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_AUDIO = "AUDIO";
    public static final String TYPE_VIDEO = "VIDEO";
    public static final String TYPE_FILE  = "FILE";

    // ── Champs de base ────────────────────────────────────────────────────
    private int           id;
    private String        content;
    private LocalDateTime createdAt;
    private boolean       isPinned;
    private boolean       isEdited;
    private boolean       isStarred;
    private int           chatroomId;
    private int           authorId;
    private int           replyToId;   // 0 = pas une réponse

    // ── Pièces jointes ────────────────────────────────────────────────────
    private String attachmentPath;         // chemin local ou URL
    private String attachmentType;         // TEXT / IMAGE / AUDIO / VIDEO / FILE
    private String attachmentOriginalName; // nom original du fichier
    private int    audioDuration;          // durée en secondes (audio)

    // ── Constructeurs ─────────────────────────────────────────────────────

    public Message() {
        this.createdAt      = LocalDateTime.now();
        this.isPinned       = false;
        this.isEdited       = false;
        this.attachmentType = TYPE_TEXT;
    }

    public Message(String content, int chatroomId, int authorId) {
        setContent(content);
        setChatroomId(chatroomId);
        setAuthorId(authorId);
        this.createdAt      = LocalDateTime.now();
        this.isPinned       = false;
        this.isEdited       = false;
        this.attachmentType = TYPE_TEXT;
    }

    // ── Validation ────────────────────────────────────────────────────────

    public void setContent(String content) {
        if (content == null || content.trim().isEmpty())
            throw new IllegalArgumentException("Content required");
        if (content.length() > 1000)
            throw new IllegalArgumentException("Message too long");
        this.content = content;
    }

    public void setChatroomId(int chatroomId) {
        if (chatroomId <= 0) throw new IllegalArgumentException("Invalid chatroom ID");
        this.chatroomId = chatroomId;
    }

    public void setAuthorId(int authorId) {
        if (authorId <= 0) throw new IllegalArgumentException("Invalid user ID");
        this.authorId = authorId;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    public boolean hasAttachment() {
        return attachmentPath != null && !attachmentPath.isBlank();
    }

    public boolean isImage() { return TYPE_IMAGE.equals(attachmentType); }
    public boolean isAudio() { return TYPE_AUDIO.equals(attachmentType); }
    public boolean isVideo() { return TYPE_VIDEO.equals(attachmentType); }
    public boolean isFile()  { return TYPE_FILE.equals(attachmentType);  }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public int           getId()                          { return id; }
    public void          setId(int id)                    { this.id = id; }
    public String        getContent()                     { return content; }
    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public void          setCreatedAt(LocalDateTime t)    { this.createdAt = t; }
    public int           getChatroomId()                  { return chatroomId; }
    public int           getAuthorId()                    { return authorId; }
    public boolean       isPinned()                       { return isPinned; }
    public void          setPinned(boolean p)             { this.isPinned = p; }
    public boolean       isEdited()                       { return isEdited; }
    public void          setEdited(boolean e)             { this.isEdited = e; }
    public boolean       isStarred()                      { return isStarred; }
    public void          setStarred(boolean s)            { this.isStarred = s; }
    public int           getReplyToId()                   { return replyToId; }
    public void          setReplyToId(int r)              { this.replyToId = r; }

    public String getAttachmentPath()                     { return attachmentPath; }
    public void   setAttachmentPath(String p)             { this.attachmentPath = p; }
    public String getAttachmentType()                     { return attachmentType != null ? attachmentType : TYPE_TEXT; }
    public void   setAttachmentType(String t)             { this.attachmentType = t; }
    public String getAttachmentOriginalName()             { return attachmentOriginalName; }
    public void   setAttachmentOriginalName(String n)     { this.attachmentOriginalName = n; }
    public int    getAudioDuration()                      { return audioDuration; }
    public void   setAudioDuration(int d)                 { this.audioDuration = d; }
}
