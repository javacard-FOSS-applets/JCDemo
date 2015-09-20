package com.jcdemo.jcmultiselect;

import javacard.framework.*;
import javacardx.crypto.*;
import javacard.security.*;

public class JCMultiSelect extends Applet 
                           implements MultiSelectable {
  // persistent data
  public byte[] pubData;
  // CLEAR_ON_DESELECT
  public byte[] deselectRAM;
  // CLEAR_ON_RESET
  public byte[] selectRAM;



  // <<< !!! DEBUG !!! >>> //
  // Debug buffer (LV)
  public byte[]  debugBuffer;
  // >>> !!! DEBUG !!! <<< //


  // current APDU in insTable
  public short curInsTableOffset;

  // APDU Command INS
  public static final byte INS_SET_DATA           = (byte)0x01;
  public static final byte INS_GET_DATA           = (byte)0x02;

  // INS Table
  public static final byte[] insTable = {
    // cla ins pp1 pp2 pp3 ctrl
    // ctrl -> bit8 :  1 = APDU hava receive data; 0 = no receive data
    (byte)0x00, INS_SET_DATA          , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, INS_GET_DATA          , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
  };

  /**
   * @brief JCMultiSelect Construct function
   *
   * @param bArray
   * @param bOffset
   * @param bLength
   *
   * @return 
   */
  protected JCMultiSelect(byte[] bArray, short bOffset, byte bLength) {
    pubData = new byte[5];
    deselectRAM = JCSystem.makeTransientByteArray((short)1200, JCSystem.CLEAR_ON_DESELECT);
    selectRAM = JCSystem.makeTransientByteArray((short)100, JCSystem.CLEAR_ON_RESET);

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
    new JCMultiSelect(bArray, bOffset, bLength).register();
  }

  /**
   * @brief select 
   *
   * @return 
   */
  public boolean select() {
    return true;
  }

  public boolean select(boolean appInstAlreadyActive) {
    return true;
  }
  public void deselect(boolean appInstAlreadyActive) {
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

      short len1 = (short)0;
      short len2 = (short)0;
      // handle APDU
      switch(ins) {
        case INS_SET_DATA:
          apdu.setIncomingAndReceive();
          Util.arrayCopyNonAtomic(apduBuffer, (short)ISO7816.OFFSET_CDATA, pubData, (short)0, (short)5);
          Util.arrayCopyNonAtomic(apduBuffer, (short)(ISO7816.OFFSET_CDATA+5), selectRAM, (short)0, (short)5);
          Util.arrayCopyNonAtomic(apduBuffer, (short)(ISO7816.OFFSET_CDATA+10), deselectRAM, (short)0, (short)5);
          break;
        case INS_GET_DATA:
          Util.arrayCopyNonAtomic(pubData, (short)0, apduBuffer, (short)(0), (short)5);
          Util.arrayCopyNonAtomic(selectRAM, (short)0, apduBuffer, (short)(5), (short)5);
          Util.arrayCopyNonAtomic(deselectRAM, (short)0, apduBuffer, (short)(10), (short)5);
          setOutgoingAndSend(apdu, (short)0, (short)15);
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

  /**
   * @brief genRSAPublicKey : generater (n, e) for RSA public key
   *
   * @param keyBuf  : store (n, e)
   * @param offset
   * @param publicKey : RSA public key
   *
   * @return  length for RSA public key
   */
  public short genRSAPublicKey(byte[] keyBuf, short offset, RSAPublicKey publicKey) {
    short i = (short)0;

    keyBuf[(short)(offset+i)] = (byte)0x6E; // n
    i++;
    keyBuf[(short)(offset+i)] = (byte)publicKey.getModulus(keyBuf, (short)(offset+i+1));
    // there, "00" will refer "256"
    if ((byte)0x00 == keyBuf[(short)(offset+i)]) {
      i += (short)(Util.makeShort((byte)1, (byte)(keyBuf[(short)(offset+i)]+(byte)1)));
    } else {
      i += (short)(Util.makeShort((byte)0, (byte)(keyBuf[(short)(offset+i)]+(byte)1)));
    }

    keyBuf[(short)(offset+i)] = (byte)0x65; // e
    i++;
    keyBuf[(short)(offset+i)] = (byte)publicKey.getExponent(keyBuf, (short)(offset+i+1));
    i += (short)(Util.makeShort((byte)0, (byte)(keyBuf[(short)(offset+i)]+(byte)1)));

    return i;
  }

  /**
   * @brief genRSAPrivateKey : generater (q, Q, p, P, PQ) for RSA private key
   *
   * @param keyBuf : store (q, Q, p, P, PQ)
   * @param offset
   * @param privateCrtKey : RSA private Key
   *
   * @return : length for RSA private key
   */
  public short genRSAPrivateKey(byte[] keyBuf, short offset, RSAPrivateCrtKey privateCrtKey) {
    short i = (short)0x00;

    keyBuf[(short)(offset+i)] = (byte)0x71; // q
    i++;
    keyBuf[(short)(offset+i)] = (byte)privateCrtKey.getQ(keyBuf, (short)(offset+i+1));
    i += (short)(Util.makeShort((byte)0, (byte)(keyBuf[(short)(offset+i)]+(byte)1)));

    keyBuf[(short)(offset+i)] = (byte)0x51; // Q
    i++;
    keyBuf[(short)(offset+i)] = (byte)privateCrtKey.getDQ1(keyBuf, (short)(offset+i+1));
    i += (short)(Util.makeShort((byte)0, (byte)(keyBuf[(short)(offset+i)]+(byte)1)));

    
    keyBuf[(short)(offset+i)] = (byte)0x70; // p
    i++;
    keyBuf[(short)(offset+i)] = (byte)privateCrtKey.getP(keyBuf, (short)(offset+i+1));
    i += (short)(Util.makeShort((byte)0, (byte)(keyBuf[(short)(offset+i)]+(byte)1)));

    keyBuf[(short)(offset+i)] = (byte)0x50; // P
    i++;
    keyBuf[(short)(offset+i)] = (byte)privateCrtKey.getDP1(keyBuf, (short)(offset+i+1));
    i += (short)(Util.makeShort((byte)0, (byte)(keyBuf[(short)(offset+i)]+(byte)1)));

    keyBuf[(short)(offset+i)] = (byte)0x49; // PQ 
    i++;
    keyBuf[(short)(offset+i)] = (byte)privateCrtKey.getPQ(keyBuf, (short)(offset+i+1));
    i += (short)(Util.makeShort((byte)0, (byte)(keyBuf[(short)(offset+i)]+(byte)1)));

    return i;
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
