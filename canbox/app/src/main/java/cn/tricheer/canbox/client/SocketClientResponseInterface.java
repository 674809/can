package cn.tricheer.canbox.client;

public interface SocketClientResponseInterface<T> {
    /**
     * 客户端连接回调
     */
    void onSocketConnect();

    /**
     * 客户端收到服务端消息回调
     *
     * @param socketResult
     */
    void onSocketReceive(T socketResult, int code);

    /**
     * 客户端关闭回调
     *
     * @param msg
     */
    void onSocketDisable(String msg, int code);
}
