package net.apnic.rpki.server.messages;

public enum MessageType {
    MSG_DATA                            (0),
    MSG_ERROR_XFER                      (1),
    MSG_INFO                            (2),
    MSG_ERROR                           (3),
    MSG_WARNING                         (4),
    MSG_ERROR_SOCKET                    (5),
    MSG_LOG                             (6),
    MSG_CLIENT                          (7),
    MSG_ERROR_UTF8                      (8),
    MSG_REDO                            (9),
    MSG_STATS                           (10),
    MSG_IO_ERROR                        (22),
    MSG_IO_TIMEOUT                      (33),
    MSG_NOOP                            (42),
    MSG_ERROR_EXIT                      (86),
    MSG_SUCCESS                         (100),
    MSG_DELETED                         (101),
    MSG_NO_SEND                         (102);

    private final int code;
    MessageType(int code) {
        this.code = code;
    }

    public int getCodeValue() { return code; }

    public static MessageType typeForTag(int tag) {
        for (MessageType type : values() ) {
            if (type.getCodeValue() == tag) return type;
        }
        return null;
    }

}
