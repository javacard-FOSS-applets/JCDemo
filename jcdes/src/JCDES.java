package com.jcdemo.jcdes;

import javacard.framework.*;
import javacardx.crypto.*;
import javacard.security.*;

public class JCDES extends Applet {
  // 3DES & DES key
  public DESKey TDES3Key;
  public DESKey TDES2Key;
  public DESKey DES1Key;
  // ECB & CBC
  public Cipher ECBObject;
  public Cipher CBCObject;
  public byte[] iCV;

  public static final byte[] DES_KEY_DEFAULT = {
    (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08,
    (byte)0x11, (byte)0x12, (byte)0x13, (byte)0x14, (byte)0x15, (byte)0x16, (byte)0x17, (byte)0x18,
    (byte)0x21, (byte)0x22, (byte)0x23, (byte)0x24, (byte)0x25, (byte)0x26, (byte)0x27, (byte)0x28
  };


  
  // current APDU in insTable
  public short curInsTableOffset;

  // APDU Command INS
  public static final byte INS_DES_ENC        = (byte)0x01;
  public static final byte INS_DES_DEC        = (byte)0x02;
  public static final byte INS_TDES_2KEY_ENC  = (byte)0x03;
  public static final byte INS_TDES_2KEY_DEC  = (byte)0x04;
  public static final byte INS_TDES_3KEY_ENC  = (byte)0x05;
  public static final byte INS_TDES_3KEY_DEC  = (byte)0x07;
  public static final byte INS_CHK_DES_KEY    = (byte)0x08;

  // INS Table
  public static final byte[] insTable = {
    // cla ins pp1 pp2 pp3 ctrl
    // ctrl -> bit8 :  1 = APDU hava receive data; 0 = no receive data
    (byte)0x00, INS_DES_ENC              , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, INS_DES_DEC              , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, INS_TDES_2KEY_ENC        , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, INS_TDES_2KEY_DEC        , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, INS_TDES_3KEY_ENC        , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, INS_TDES_3KEY_DEC        , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, INS_CHK_DES_KEY          , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
  };

  /**
   * @brief JCDES Construct function
   *
   * @param bArray
   * @param bOffset
   * @param bLength
   *
   * @return 
   */
  protected JCDES(byte[] bArray, short bOffset, byte bLength) {
    // according to different Key to create differnt KEY instance
    DES1Key = (DESKey)KeyBuilder.buildKey(KeyBuilder.TYPE_DES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_DES, false);
    TDES2Key = (DESKey)KeyBuilder.buildKey(KeyBuilder.TYPE_DES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_DES3_2KEY, false);
    TDES3Key = (DESKey)KeyBuilder.buildKey(KeyBuilder.TYPE_DES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_DES3_3KEY, false);

    // there are 4 method to padding input data
    // 1. ALG_DES_ECB_NOPAD : no pad for input data
    // 2. ALG_DES_ECB_ISO9797_M1 : pad with 0x0000...
    // 3. ALG_DES_ECB_ISO9797_M2 : pad with 0x8000...
    // 4. ALG_DES_ECB_PKCS5 : pad with 'len''len'...
    // should be create Instance according to required
    ECBObject = Cipher.getInstance(Cipher.ALG_DES_ECB_ISO9797_M1, false);
    // there are 4 method to padding input data
    // 1. ALG_DES_CBC_NOPAD : no pad for input data
    // 2. ALG_DES_CBC_ISO9797_M1 : pad with 0x0000...
    // 3. ALG_DES_CBC_ISO9797_M2 : pad with 0x8000...
    // 4. ALG_DES_CBC_PKCS5 : pad with 'len''len'...
    // should be create Instance according to required
    CBCObject = Cipher.getInstance(Cipher.ALG_DES_CBC_ISO9797_M2, false);
    iCV = new byte[8];

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
    new JCDES(bArray, bOffset, bLength).register(bArray, (short)(bOffset+1), bArray[bOffset]);
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
      short lc = (short)(apduBuffer[ISO7816.OFFSET_LC] & 0x00FF);

      // get APDU in insTable
      setCurCmdInsTableOffset(insTable, ins);

      // check Ctrl
      checkCurCmdCtrlInfo(apdu, insTable);

      // check PP1 PP2 PP3
      checkApduHeadInfo(insTable, apduBuffer);

      // handle APDU
      switch(ins) {
        case INS_DES_ENC:
          // 1 key DES encrypt
          apdu.setIncomingAndReceive();
          DES1Key.setKey(DES_KEY_DEFAULT, (short)0);
          ECBObject.init(DES1Key, Cipher.MODE_ENCRYPT);
          short len = ECBObject.doFinal(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc, apduBuffer, (short)ISO7816.OFFSET_CDATA);
          apdu.setOutgoingAndSend((short)ISO7816.OFFSET_CDATA, len);
          break;
        case INS_DES_DEC:
          // 1 key DES decrypt
          apdu.setIncomingAndReceive();
          DES1Key.setKey(DES_KEY_DEFAULT, (short)0);
          ECBObject.init(DES1Key, Cipher.MODE_DECRYPT);
          len = ECBObject.doFinal(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc, apduBuffer, (short)ISO7816.OFFSET_CDATA);
          apdu.setOutgoingAndSend((short)ISO7816.OFFSET_CDATA, len);
          break;
        case INS_TDES_2KEY_ENC:
          // 2 key DES encrypt
          apdu.setIncomingAndReceive();
          TDES2Key.setKey(DES_KEY_DEFAULT, (short)0);
          ECBObject.init(TDES2Key, Cipher.MODE_ENCRYPT);
          len = ECBObject.doFinal(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc, apduBuffer, (short)ISO7816.OFFSET_CDATA);
          apdu.setOutgoingAndSend((short)ISO7816.OFFSET_CDATA, len);
          break;
        case INS_TDES_2KEY_DEC:
          // 2 key DES decrypt
          apdu.setIncomingAndReceive();
          TDES2Key.setKey(DES_KEY_DEFAULT, (short)0);
          ECBObject.init(TDES2Key, Cipher.MODE_DECRYPT);
          len = ECBObject.doFinal(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc, apduBuffer, (short)ISO7816.OFFSET_CDATA);
          apdu.setOutgoingAndSend((short)ISO7816.OFFSET_CDATA, len);
          break;
        case INS_TDES_3KEY_ENC:
          // 3 key DES encrypt
          apdu.setIncomingAndReceive();
          TDES3Key.setKey(DES_KEY_DEFAULT, (short)0);
          CBCObject.init(TDES3Key, Cipher.MODE_ENCRYPT);
          len = CBCObject.doFinal(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc, apduBuffer, (short)ISO7816.OFFSET_CDATA);
          apdu.setOutgoingAndSend((short)ISO7816.OFFSET_CDATA, len);
          break;
        case INS_TDES_3KEY_DEC:
          // 3 key DES decrypt
          apdu.setIncomingAndReceive();
          TDES3Key.setKey(DES_KEY_DEFAULT, (short)0);
          CBCObject.init(TDES3Key, Cipher.MODE_DECRYPT);
          len = CBCObject.doFinal(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc, apduBuffer, (short)ISO7816.OFFSET_CDATA);
          apdu.setOutgoingAndSend((short)ISO7816.OFFSET_CDATA, len);
          break;
        case INS_CHK_DES_KEY:
          // check DES key
          apduBuffer[ISO7816.OFFSET_CDATA] = TDES3Key.isInitialized() ? (byte)0x01: (byte)0x00;
          TDES3Key.setKey(DES_KEY_DEFAULT, (short)0);
          // check isInitialized()
          apduBuffer[ISO7816.OFFSET_CDATA+1] = TDES3Key.isInitialized() ? (byte)0x01: (byte)0x00;
          // check getType()
          apduBuffer[ISO7816.OFFSET_CDATA+2] = TDES3Key.getType();
          // check getSize()
          Util.setShort(apduBuffer, (short)(ISO7816.OFFSET_CDATA+3), TDES3Key.getSize());
          // check getKey()
          len = TDES3Key.getKey(apduBuffer, (short)(ISO7816.OFFSET_CDATA+5));
          apdu.setOutgoingAndSend((short)ISO7816.OFFSET_CDATA, (short)(5+len));
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
}
