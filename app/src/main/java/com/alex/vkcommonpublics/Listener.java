package com.alex.vkcommonpublics;

/**
 * Класс-listener для оповещения о заверении какого-либо действия и об ошибках.
 */
public interface Listener<T> {
    void onCompleted(T t);
    void onError(String e);
}