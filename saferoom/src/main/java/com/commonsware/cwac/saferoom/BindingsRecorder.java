package com.commonsware.cwac.saferoom;

import android.arch.persistence.db.SupportSQLiteProgram;
import android.util.SparseArray;

class BindingsRecorder implements SupportSQLiteProgram {
  private SparseArray<Object> bindings=new SparseArray<>();

  @Override
  public void bindNull(int index) {
    bindings.put(index, null);
  }

  @Override
  public void bindLong(int index, long value) {
    bindings.put(index, value);
  }

  @Override
  public void bindDouble(int index, double value) {
    bindings.put(index, value);
  }

  @Override
  public void bindString(int index, String value) {
    bindings.put(index, value);
  }

  @Override
  public void bindBlob(int index, byte[] value) {
    bindings.put(index, value);
  }

  @Override
  public void clearBindings() {
    bindings.clear();
  }

  @Override
  public void close() {
    clearBindings();
  }

  String[] getBindings() {
    final String[] result=new String[bindings.size()];

    for (int i=0;i<bindings.size();i++) {
      Object binding=bindings.get(i);

      if (binding!=null) {
        result[i]=bindings.get(i).toString();
      }
      else {
        result[i]=""; // SQLCipher does not like null binding values
      }
    }

    return(result);
  }
}
