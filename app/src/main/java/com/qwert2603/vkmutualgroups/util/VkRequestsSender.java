package com.qwert2603.vkmutualgroups.util;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.vk.sdk.api.VKRequest;

/**
 * Класс для отправки запросов к vkapi (не более 3 в секунду).
 */
public class VkRequestsSender {

    /**
     * Задержка перед следующим запросом.
     * Чтобы запросы не посылались слишком часто. (Не больше 3 в секунду).
     */
    public static final long nextRequestDelay = 350;

    /**
     * Время, когда можно посылать следующий запрос.
     */
    private static long nextRequestTime;

    static {
        nextRequestTime = SystemClock.uptimeMillis();
    }

    /**
     * Обрботчик отправки запросов
     */
    private final static Handler mHandler;

    static {
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Отправить запрос.
     * Запросы выполняются последовательно.
     * Переданный запрос будет отправлен когда придет его время.
     */
    public static synchronized void sendRequest(VKRequest request, VKRequest.VKRequestListener listener) {
        if (nextRequestTime <= SystemClock.uptimeMillis()) {
            request.executeWithListener(listener);
            nextRequestTime = SystemClock.uptimeMillis();
        } else {
            mHandler.postAtTime(() -> request.executeWithListener(listener), nextRequestTime);
        }
        nextRequestTime += nextRequestDelay;
    }

}
