package CommonConstant;

/**
 * @Author wonderful
 * @Description 消息类型枚举器
 * @Date 2019-8-30
 */
public enum MessageType {

    ANSWER(10000, "ANSWER"),
    SOCKET_KEY(10001, "SOCKET_KEY"),
    SOCKET_CLOSE(10002, "SOCKET_CLOSE"),
    MESSAGE_RECEIVE(10003, "MESSAGE_RECEIVE"),
    MESSAGE_SEND(10004, "MESSAGE_SEND"),
    ERROR(10005, "ERROR");

    private int code;
    private String msg;

    MessageType(int code, String msg) {
            this.code = code;
            this.msg = msg;
    }

    public int getCode() {
            return code;
    }

    public void setCode(int code) {
            this.code = code;
    }

    public String getMsg() {
            return msg;
    }

    public void setMsg(String msg) {
            this.msg = msg;
    }

    // 根据value返回枚举类型,主要在switch中使用
    public static MessageType getByValue(int value) {
            for (MessageType code : values()) {
                    if (code.getCode() == value) {
                            return code;
                    }
            }
            return null;
    }
}
