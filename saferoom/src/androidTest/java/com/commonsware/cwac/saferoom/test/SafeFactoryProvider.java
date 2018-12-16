package com.commonsware.cwac.saferoom.test;

import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.content.Context;
import android.text.SpannableStringBuilder;
import com.commonsware.cwac.saferoom.SafeHelperFactory;
import com.commonsware.dbtest.FactoryProvider;

import java.io.File;

class SafeFactoryProvider implements FactoryProvider {
  @Override
  public SupportSQLiteOpenHelper.Factory getFactory() {
    return SafeHelperFactory.fromUser(new SpannableStringBuilder("sekrit"));
  }

  @Override
  public void tearDownDatabase(Context ctxt,
                               SupportSQLiteOpenHelper.Factory factory,
                               SupportSQLiteOpenHelper helper) {
    String name=helper.getDatabaseName();

    if (name!=null) {
      File db=ctxt.getDatabasePath(name);

      if (db.exists()) {
        db.delete();
      }

      File journal=new File(db.getParentFile(), name+"-journal");

      if (journal.exists()) {
        journal.delete();
      }
    }
  }
}
