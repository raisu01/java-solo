package common;

public final class Protocol {
    public static final int DEFAULT_PORT = 9999;
    public static final String ENCODING = "CP850";

    public static final String EXIT_COMMAND   = "exit";
    public static final String PING_COMMAND   = "ping";
    public static final String AUTH_COMMAND   = "auth";

    public static final int CONNECTION_TIMEOUT_MS = 30_000;
    public static final int MAX_CLIENTS = 1;
    public static final int MAX_OUTPUT_SIZE = 512 * 1024; // 512 KB

    private Protocol() {}
}
