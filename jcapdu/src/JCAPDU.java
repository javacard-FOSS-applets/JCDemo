package com.jcdemo.jcapdu;

import javacard.framework.*;

public class JCAPDU extends Applet {

  // APDU Command INS
  public static final byte INS_APDU_CASE_1 = (byte)0x01;
  public static final byte INS_APDU_CASE_2 = (byte)0x02;
  public static final byte INS_APDU_CASE_3 = (byte)0x03;
  public static final byte INS_APDU_CASE_4 = (byte)0x04;

  /**
   * @brief JCAPDU Construct function
   *
   * @param bArray
   * @param bOffset
   * @param bLength
   *
   * @return 
   */
  protected JCAPDU(byte[] bArray, short bOffset, byte bLength) {
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
    new JCAPDU(bArray, bOffset, bLength).register();
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
      // get logical channel
      byte claChannel = apdu.getCLAChannel();
      // CLA INS PP1 PP2 LC/LE
      byte cla = apduBuffer[ISO7816.OFFSET_CLA];
      byte ins = apduBuffer[ISO7816.OFFSET_INS];
      byte pp1 = apduBuffer[ISO7816.OFFSET_P1];
      byte pp2 = apduBuffer[ISO7816.OFFSET_P2];
      short plc = (short) (apduBuffer[ISO7816.OFFSET_LC] & 0x00FF);

      // handle APDU
      switch(ins) {
        case INS_APDU_CASE_1:
          // no incoming and outgoing
          // 1. Handle command
          if ((pp1 != (byte)0x00) || (pp2 != (byte)0x00)) {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
          }
          // send more time 
          apdu.waitExtension();
          break;
        case INS_APDU_CASE_2:
          // command has outgoing data
          // 1. Handle command
          if ((pp1 != (byte)0x00) || (pp2 != (byte)0x00)) {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
          }
          // 2. set outgoing data
          // construct the reply APDU
          short ple = apdu.setOutgoing();
          if (ple < (short)2) ISOException.throwIt( ISO7816.SW_WRONG_LENGTH );
          // build response data in apdu.buffer[ 0.. outCount-1 ];
          apdu.setOutgoingLength((short)3);
          apduBuffer[0] = (byte)claChannel;
          apduBuffer[1] = (byte)2; 
          apduBuffer[2] = (byte)3;
          // 3. send data
          apdu.sendBytes((short)0 , (short)3);
          // return good complete status 90 00
          break;
        case INS_APDU_CASE_3:
          // command has incoming data
          // Lc tells us the incoming apdu command length
          if (plc < (short)5) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
          // 1. receive data
          short readCount = apdu.setIncomingAndReceive();
          while (plc > 0) {
              // process bytes in buffer[5] to buffer[readCount+4];
              plc -= readCount;
              readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
          }
          // 2. Handle command
          if ((pp1 != (byte)0x00) || (pp2 != (byte)0x00)) {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
          }
          break;
        case INS_APDU_CASE_4:
          // command has incoming and outgoing data
          // 1. receive data
          apdu.setIncomingAndReceive();
          // 2. Handle command
          if ((pp1 != (byte)0x00) || (pp2 != (byte)0x00)) {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
          }
          // 3. set Outgoing data 
          apduBuffer[0] = (byte)1;
          apduBuffer[1] = (byte)2; 
          apduBuffer[2] = (byte)3;
          // 4. send data
          apdu.setOutgoingAndSend((short)0, (short)3);
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
}
