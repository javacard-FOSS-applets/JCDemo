package com.jcdemo.jcsign;

import javacard.framework.*;
import javacardx.crypto.*;
import javacard.security.*;

public class JCSign extends Applet {
  public Signature sign;
  public KeyPair keyPair;
  public PrivateKey privateKey;
  public PublicKey publicKey;

  public byte[] comBuffer;

  short len1;


  
  // <<< !!! DEBUG !!! >>> //
  // Debug buffer (LV)
  public byte[]  debugBuffer;
  // >>> !!! DEBUG !!! <<< //


  // current APDU in insTable
  public short curInsTableOffset;

  // APDU Command INS
  public static final byte INS_SIGN_MD5_PKCS  = (byte)0x01;
  public static final byte INS_VERIFY_MD5_PKCS  = (byte)0x02;

  // INS Table
  public static final byte[] insTable = {
    // cla ins pp1 pp2 pp3 ctrl
    // ctrl -> bit8 :  1 = APDU hava receive data; 0 = no receive data
    (byte)0x00, INS_SIGN_MD5_PKCS       , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x80,
    (byte)0x00, INS_VERIFY_MD5_PKCS     , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x80,
    (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
  };

  /**
   * @brief JCSign Construct function
   *
   * @param bArray
   * @param bOffset
   * @param bLength
   *
   * @return 
   */
  protected JCSign(byte[] bArray, short bOffset, byte bLength) {
    sign = Signature.getInstance(Signature.ALG_RSA_MD5_PKCS1, false);
    keyPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_1024);
    publicKey = keyPair.getPublic();
    privateKey = keyPair.getPrivate();
    keyPair.genKeyPair();

    comBuffer = new byte[256];
    // <<< !!! DEBUG !!! >>> //
    debugBuffer = new byte[256];
    // >>> !!! DEBUG !!! <<< //
  }

  /**
   * @brief install : Called when Applet Installing
   *
   * @param bArray : LV(AID) + LV(Privileges) + LV(Install Param (C9))
   * @param bOffset
   * @param bLength
   *
   * @return 
   */
  public static void install(byte[] bArray, short bOffset, byte bLength) {
    // register(AID)
    new JCSign(bArray, bOffset, bLength).register();
  }

  /**
   * @brief select 
   *
   * @return 
   */
  public boolean select() {
    return true;
  }

  /**
   * @brief process : APDU Handle Function
   *
   * @param apdu
   *
   * @return 
   */
  public void process(APDU apdu) {
    try {
      // Good practice : Return 9000 on Select applet
      if (selectingApplet()) {
        return;
      }

      // get APDU data
      byte[] apduBuffer = apdu.getBuffer();
      byte ins = apduBuffer[ISO7816.OFFSET_INS];
      byte pp1 = apduBuffer[ISO7816.OFFSET_P1];
      short lc = (short)(apduBuffer[ISO7816.OFFSET_LC] & 0x00FF);

      // get APDU in insTable
      setCurCmdInsTableOffset(insTable, ins);

      // check Ctrl
      checkCurCmdCtrlInfo(apdu, insTable);

      // check PP1 PP2 PP3
      checkApduHeadInfo(insTable, apduBuffer);

      // handle APDU
      switch(ins) {
        case INS_SIGN_MD5_PKCS:
          // save data in order to verify
          len1 = lc;
          Util.arrayCopyNonAtomic(apduBuffer, (short)ISO7816.OFFSET_CDATA, comBuffer, (short)0, lc); 
          sign.init(privateKey, Signature.MODE_SIGN);
          short len = sign.sign(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc, apduBuffer, (short)ISO7816.OFFSET_CDATA);
          setOutgoingAndSend(apdu, (short)ISO7816.OFFSET_CDATA, len);
          break;
        case INS_VERIFY_MD5_PKCS:
          sign.init(publicKey, Signature.MODE_VERIFY);
          boolean flag = sign.verify(comBuffer, (short)0, len1, apduBuffer, (short)ISO7816.OFFSET_CDATA, lc);
          if (!flag) {
            ISOException.throwIt((short)0x6E01);
          }
          break;
        default:
          ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
      }

    } catch (ISOException ce) {
      ISOException.throwIt(ce.getReason());
    } catch (RuntimeException ce) {
      ISOException.throwIt(ISO7816.SW_UNKNOWN);
    }
  }

  /**
   * @brief deselect 
   *
   * @return 
   */
  public void deselect() {
  }

  /**
   * @brief setOutgoingAndSend 
   *
   * @param apdu
   * @param bOff
   * @param len
   *
   * @return 
   */
  public void setOutgoingAndSend(APDU apdu, short bOff, short len) {
    // <<< !!! DEBUG !!! >>> //
    // Checking debug report.
    if(debugBuffer[0] != (byte)0x00) {
      debugOut(apdu, debugBuffer, (short)0, (short)(debugBuffer[0]+1));
      return;
    }  
    // >>> !!! DEBUG !!! <<< //
    apdu.setOutgoingAndSend(bOff, len);
  }

  /**
   * @brief setCurCmdInsTableOffset : search APDU in insTable
   *
   * @param insTable
   * @param curCmdIns
   *
   * @return 
   */
  private void setCurCmdInsTableOffset(byte[] insTable, byte curCmdIns) {
    short len = (short) insTable.length;
    // search APDU INS in insTable
    for (short i = (short)0; i < len; i += (short)6) {
      if ((byte)insTable[(short)(i+1)] == (byte)curCmdIns) {
        curInsTableOffset = i;
        return;
      }
    }
    ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
  }

  /**
   * @brief checkCurCmdCtrlInfo : check APDU and receive data
   *
   * @param apdu
   * @param insTable
   *
   * @return 
   */
  private void checkCurCmdCtrlInfo(APDU apdu, byte[] insTable) {
    // check APDU if receive data
    if ((insTable[(short)(curInsTableOffset+5)] & (byte)0x80) == (byte)0x80) {
      apdu.setIncomingAndReceive();
    }
  }

  /**
   * @brief checkApduHeadInfo : check CLA, PP1, PP2 and PP3 
   *
   * @param insTable
   * @param apduBuffer
   *
   * @return 
   */
  private void checkApduHeadInfo(byte[] insTable, byte[] apduBuffer) {
    // check CLS
    if ((insTable[curInsTableOffset] & (byte)0xF8) != (apduBuffer[ISO7816.OFFSET_CLA] & (byte)0xF8)) {
      ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
    }
    // check PP1 and PP2
    if (checkApduHeadParam(insTable[(short)(curInsTableOffset+2)], apduBuffer[ISO7816.OFFSET_P1])
        || checkApduHeadParam(insTable[(short)(curInsTableOffset+3)], apduBuffer[ISO7816.OFFSET_P2])) {
      ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
    }
    // check PP3
    if (checkApduHeadParam(insTable[(short)(curInsTableOffset+4)], apduBuffer[ISO7816.OFFSET_LC])) {
      ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
    }
  }

  /**
   * @brief checkApduHeadParam : check curParam if follow inParam
   *
   * @param inParam : ruler -- 0xFF: no check
   *                           bit8 = 1 : curParam in [H(inParam), L(inParam) + H(inParam)]
   *                           other : inParam == curParam
   *                 
   * @param curParam
   *
   * @return : true: on follow; false : follow;
   */
  private boolean checkApduHeadParam(byte inParam, byte curParam) {
    // 0xFF, no check
    if (inParam == (byte)0xFF) {
    } else if ((inParam & (byte)0x80) == (byte)0x80) {
      // bit8 = 1 ==>  H(inParam) <= curParm <= L(inParam) + H(inParam)
      short tmpinParam = (short)(inParam & 0x00FF);
      short tmpcurParam = (short)(curParam & 0x00FF);
      tmpinParam &= (short)0x007F;
      if ((tmpinParam >> 4) > tmpcurParam) {
        return true;
      }
      tmpcurParam -= tmpinParam >> 4;
      if ((tmpinParam & (short)0x000F) < tmpcurParam) {
        return true;
      }
    } else if (inParam != curParam) {
      // other, equal
      return true;
    }
    return false;
  }



  
  // <<< !!! DEBUG !!! >>> //
  /**
   * @brief sendReport : Sending immediately the provided buffer through the IO,
   *                     whatever may be stored in the debug buffer
   *
   * @param apdu    : APDU object to manage the IO.
   * @param abArray : byte array to copy into the debug buffer. 
   * @param sOffset : index of the 1st byte of useful data
   * @param sLength : length of useful data.
   *
   * @return 
   */
  public void debugOut(APDU apdu, byte[] abArray, short sOffset, short sLength)
  {
    // Turning IO stream to output.
    apdu.setOutgoing();     
    apdu.setOutgoingLength((short)sLength); 
    
    // Sending the test report.
    apdu.sendBytesLong(abArray, sOffset, sLength);
  }
  // >>> !!! DEBUG !!! <<< //

}
