package src.main.java.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Objet de communication sérialisé échangé entre client et serveur.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        COMMAND,
        RESULT,
        ERROR,
        AUTH_REQUEST,
        AUTH_RESPONSE,
        FILE_UPLOAD,
        FILE_DOWNLOAD,
        DISCONNECT,
        PING,
        PONG
    }

    private final Type type;
    private final String content;
    private final byte[] fileData;
    private final String fileName;
    private final long timestamp;

    public Message(Type type, String content) {
        this.type      = type;
        this.content   = content;
        this.fileData  = null;
        this.fileName  = null;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(Type type, String fileName, byte[] fileData) {
        this.type      = type;
        this.content   = fileName;
        this.fileName  = fileName;
        this.fileData  = fileData;
        this.timestamp = System.currentTimeMillis();
    }

    public Type    getType()      { return type; }
    public String  getContent()   { return content; }
    public byte[]  getFileData()  { return fileData; }
    public String  getFileName()  { return fileName; }
    public long    getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s | %s | %d chars]",
            type,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            content != null ? content.length() : 0);
    }
}
