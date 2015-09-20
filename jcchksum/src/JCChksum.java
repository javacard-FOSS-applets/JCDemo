package com.jcdemo.jcchksum;

import javacard.framework.*;
import javacardx.crypto.*;
import javacard.security.*;

public class JCChksum extends Applet {
  // Checksum calc 
  public Checksum chkSum16;
  public Checksum chkSum32;
  public byte[] initChksum;


  
  // current APDU in insTable
  public short curInsTableOffset;

  // APDU Command INS
  public static final byte INS_CHKSUM_16          = (byte)0x01;
  public static final byte INS_CHKSUM_32          = (byte)0x02;

  // INS Table
  public static final byte[] insTable = {
    // cla ins pp1 pp2 pp3 ctrl
    // ctrl -> bit8 :  1 = APDU hava receive data; 0 = no receive data
    (byte)0x00, INS_CHKSUM_16 , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, INS_CHKSUM_32 , (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
    (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
  };

  /**
   * @brief JCChksum Construct function
   *
   * @param bArray
   * @param bOffset
   * @param bLength
   *
   * @return 
   */
  protected JCChksum(byte[] bArray, short bOffset, byte bLength) {
    // Checksum generater
    chkSum16 = Checksum.getInstance(Checksum.ALG_ISO3309_CRC16, false);
    chkSum32 = Checksum.getInstance(Checksum.ALG_ISO3309_CRC32, false);

    // init Checksum 
    initChksum = new byte[4];
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
    new JCChksum(bArray, bOffset, bLength).register();
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
        case INS_CHKSUM_16:
          // 1 geneate 16 CRC
          apdu.setIncomingAndReceive();
          //Util.arrayFillNonAtomic(initChksum, (short)0, (short)initChksum.length, (byte)0);
          //chkSum16.init(initChksum, (short)0, (short)2);
          short len = chkSum16.doFinal(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc, apduBuffer, (short)ISO7816.OFFSET_CDATA);
          apdu.setOutgoingAndSend((short)ISO7816.OFFSET_CDATA, len);
          break;
        case INS_CHKSUM_32:
          // 1 geneate 32 CRC
          apdu.setIncomingAndReceive();
          //Util.arrayFillNonAtomic(initChksum, (short)0, (short)initChksum.length, (byte)0);
          //chkSum32.init(initChksum, (short)0, (short)4);
          len = chkSum32.doFinal(apduBuffer, (short)ISO7816.OFFSET_CDATA, lc, apduBuffer, (short)ISO7816.OFFSET_CDATA);
          apdu.setOutgoingAndSend((short)ISO7816.OFFSET_CDATA, len);
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
