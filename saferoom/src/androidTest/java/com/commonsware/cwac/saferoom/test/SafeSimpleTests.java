package com.commonsware.cwac.saferoom.test;

import android.support.annotation.NonNull;
import com.commonsware.dbtest.FactoryProvider;
import com.commonsware.dbtest.SimpleTests;

public class SafeSimpleTests extends SimpleTests {
    @NonNull
    @Override
    protected FactoryProvider buildFactoryProvider() {
        return new SafeFactoryProvider();
    }
}
