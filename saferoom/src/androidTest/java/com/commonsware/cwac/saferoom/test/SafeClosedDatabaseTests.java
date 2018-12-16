package com.commonsware.cwac.saferoom.test;

import androidx.annotation.NonNull;
import com.commonsware.dbtest.ClosedDatabaseTests;
import com.commonsware.dbtest.FactoryProvider;

public class SafeClosedDatabaseTests extends ClosedDatabaseTests {
    @NonNull
    @Override
    protected FactoryProvider buildFactoryProvider() {
        return new SafeFactoryProvider();
    }
}
