package com.jcdemo.jclib;

import javacard.framework.*;

public class ShareObject implements Shareable {

  public short getData() {
    return (short)0x100;
  }
  
}
