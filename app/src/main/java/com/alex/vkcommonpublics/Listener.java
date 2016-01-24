package com.alex.vkcommonpublics;

/**
 * Класс-listener для оповещения о заверении какого-либо действия и об ошибках.
 */
public interface Listener {
    void onCompleted();
    void onError(String e);
}