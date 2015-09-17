package com.jcdemo.jcrandom;

import javacard.framework.*;
import javacardx.crypto.*;
import javacard.security.*;

public class JCRandom extends Applet {
  // Random Object
  public RandomData genRandom_true;
  public RandomData genRandom_fake;


  
  // current APDU in insTable
  public short curInsTableOffset;

  // APDU Command INS
  public static final byte INS_TRUE_RND           = (byte)0x01;
  public static final byte INS_TRUE_RND_BY_SEED   = (byte)0x02;
  public static final byte INS_FAKE_RND           = (byte)0x03;
  public static final byte INS_FAKE_RND_BY_SEED   = (byte)0x04;

  // INS Table
  public static final byte[] insTable = {
    // cla ins pp1 pp2 pp3 ctrl
    // ctrl -> bit8 :  1 = APDU hava receive data; 0 = no receive data
    (byte)0x00, INS_TRUE_RND             , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, INS_TRUE_RND_BY_SEED     , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, INS_FAKE_RND             , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, INS_FAKE_RND_BY_SEED     , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
  };

  /**
   * @brief JCRandom Construct function
   *
   * @param bArray
   * @param bOffset
   * @param bLength
   *
   * @return 
   */
  protected JCRandom(byte[] bArray, short bOffset, byte bLength) {
    // random generater
    genRandom_fake = RandomData.getInstance(RandomData.ALG_PSEUDO_RANDOM);
    genRandom_true = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);

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
    new JCRandom(bArray, bOffset, bLength).register(bArray, (short)(bOffset+1), bArray[bOffset]);
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
        case INS_TRUE_RND:
          // 1 geneate true random 
          genRandom_true.generateData(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc);
          apdu.setOutgoingAndSend((short)ISO7816.OFFSET_CDATA, lc);
          break;
        case INS_TRUE_RND_BY_SEED:
          // 1 geneate true random by seed
          apdu.setIncomingAndReceive();
          genRandom_true.setSeed(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc);
          genRandom_true.generateData(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc);
          apdu.setOutgoingAndSend((short)ISO7816.OFFSET_CDATA, lc);
          break;
        case INS_FAKE_RND:
          // 2 geneate fake random 
          genRandom_fake.generateData(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc);
          apdu.setOutgoingAndSend((short)ISO7816.OFFSET_CDATA, lc);
          break;
        case INS_FAKE_RND_BY_SEED:
          // 2 geneate fake random by seed
          apdu.setIncomingAndReceive();
          genRandom_fake.generateData(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc);
          apdu.setOutgoingAndSend((short)ISO7816.OFFSET_CDATA, lc);
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
