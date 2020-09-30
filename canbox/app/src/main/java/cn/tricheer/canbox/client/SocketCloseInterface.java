package cn.tricheer.canbox.client;

public interface SocketCloseInterface<T> {
    void onSocketShutdownInput();
    void onSocketDisconnection();
}
