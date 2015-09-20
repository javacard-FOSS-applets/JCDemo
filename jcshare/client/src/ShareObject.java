package com.jcdemo.jcshare.client;

import javacard.framework.*;
import javacardx.crypto.*;
import javacard.security.*;

public class ShareObject implements Shareable {

  protected ShareObject() {
  }

  public short getData() {
    return (short)0x100;
  }
  
}
