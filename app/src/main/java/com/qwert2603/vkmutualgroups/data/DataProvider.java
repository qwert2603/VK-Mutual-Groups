package com.qwert2603.vkmutualgroups.data;

import com.qwert2603.vkmutualgroups.Listener;

/**
 * Загрузчик данных.
 */
public interface DataProvider {
    void load(Listener<Data> listener);
}