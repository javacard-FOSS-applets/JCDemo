package com.jcdemo.jcshare.server;

import javacard.framework.*;
import javacardx.crypto.*;
import javacard.security.*;

//public interface ShareInterface extends Shareable {
//  short getData();
//}

public class ShareObject implements Shareable {

  protected ShareObject() {
  }

  public short getData() {
    return (short)0x100;
  }
  
}
