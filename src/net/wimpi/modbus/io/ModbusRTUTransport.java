//License
/***
 * Java Modbus Library (jamod)
 * Copyright (c) 2002-2004, jamod development team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the author nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS ``AS
 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ***/
package net.wimpi.modbus.io;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.msg.ModbusMessage;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.util.ModbusUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Class that implements the ModbusRTU transport
 * flavor.
 *
 * @author John Charlton
 * @author Dieter Wimberger
 *
 * @version 1.1 (08/06/2004)
 */
public class ModbusRTUTransport
    implements ModbusSerialTransport {

  private InputStream m_InputStream;    //wrap into filter input
  private OutputStream m_OutputStream;      //wrap into filter output

  private byte[] m_InBuffer;
  private BytesInputStream m_ByteIn;         //to read message from
  private BytesOutputStream m_ByteInOut;     //to buffer message to
  private BytesOutputStream m_ByteOut;      //write frames
  private boolean m_Echo = false;     // require RS-485 echo processing

  public boolean isEcho() {
    return m_Echo;
  }//isEcho

  public void setEcho(boolean b) {
    this.m_Echo = b;
  }//setEcho

  public void close() throws IOException {
    m_InputStream.close();
    m_OutputStream.close();
  }//close

  public void writeMessage(ModbusMessage msg) throws ModbusIOException {
    try {
      int len;
      synchronized (m_ByteOut) {
        // first clear any input from the receive buffer to prepare
        // for the reply since RTU doesn't have message delimiters
        clearInput(4096, 0);
        //write message to byte out
        msg.setHeadless();
        msg.writeTo(m_ByteOut);
        len = m_ByteOut.size();
        int[] crc = calculateCRC(m_ByteOut.getBuffer(), 0, len);
        m_ByteOut.writeByte(crc[0]);
        m_ByteOut.writeByte(crc[1]);
        //write message
        byte[] buf = m_ByteOut.getBuffer();
        len = m_ByteOut.size();
        m_OutputStream.write(buf, 0, len);     //PDU + CRC
        m_OutputStream.flush();
//        System.out.println("Send: " + ModbusUtil.toHex(buf, 0, len));
        m_ByteOut.reset();

        // for RS-485 we need to read the output message before
        // leaving and check to see that it is the same as the one
        // sent.
        // clears out the echoed message
        // for RS485
        if (m_Echo) {
          if (!clearInput(len, 1000)) {
            System.err.println("Error: Transmit echo not received.");
          }
        }
      }

    } catch (Exception ex) {
      throw new ModbusIOException("I/O failed to write");
    }

  }//writeMessage

  //This is required for the slave that is not supported
  public ModbusRequest readRequest() throws ModbusIOException {
    throw new UnsupportedOperationException();
  } //readRequest

  /**
   * Clear the input of given length and within the given time frame.
   *
   * @param len the number of bytes to be cleared from the input.
   * @param timeOut the time frame for clearing the given number of bytes.
   *
   * @return true if successfully cleared, false otherwise.
   *
   * @throws ModbusIOException
   */
  public boolean clearInput(int len, int timeOut) throws ModbusIOException {
    boolean ret = false;
    try {
      long startTime = System.currentTimeMillis();
      int numAvailable = 0;

      while ((numAvailable = m_InputStream.available()) < len &&
          Math.abs(System.currentTimeMillis() - startTime) < timeOut) {
        // wait for message characters to be buffered
        try {
          Thread.sleep(10);
        } catch (InterruptedException ex) {
          System.err.println("InterruptedException: " + ex.getMessage());
        }
      }
      if (m_InputStream.available() > 0) {
        m_ByteInOut.reset();
        int i = 0;
        while (i < len && m_InputStream.available() > 0) {
          int ch = m_InputStream.read();
          m_ByteInOut.writeByte(ch);
          ++i;
        }
        int dlength = m_ByteInOut.size();
        if (dlength == len) {
          ret = true;
        }
        m_ByteIn.reset(m_InBuffer, dlength);
//         System.out.println("Clear input: " +
//            ModbusUtil.toHex(m_ByteIn.getBuffer(), 0, dlength));
        m_ByteIn.reset();
      }
    } catch (IOException e) {
      System.err.println("Error: ModbusRTUTransport::clearInput: " + e);
    }
    return ret;
  }//cleanInput

  public ModbusResponse readResponse()
      throws ModbusIOException {

    boolean done = false;
    ModbusResponse response = null;
    int dlength = 0;

    try {
      do {
        //1. read to function code, create request and read function specific bytes
        synchronized (m_ByteIn) {
          int uid = m_InputStream.read();
          if (uid != -1) {
            int fc = m_InputStream.read();
            m_ByteInOut.reset();
            m_ByteInOut.writeByte(uid);
            m_ByteInOut.writeByte(fc);

            //create response to acquire length of message
            response = ModbusResponse.createModbusResponse(fc);
            response.setHeadless();

            // With Modbus RTU, there is no end frame.  Either we assume
            // the message is complete as is or we must do function
            // specific processing to know the correct length.
            getResponse(fc, m_ByteInOut);
            dlength = m_ByteInOut.size() - 2; // less the crc

            m_ByteIn.reset(m_InBuffer, dlength);

            //check CRC
            int[] crc = calculateCRC(m_InBuffer, 0, dlength); //does not include CRC
            if (ModbusUtil.unsignedByteToInt(m_InBuffer[dlength]) != crc[0]
                && ModbusUtil.unsignedByteToInt(m_InBuffer[dlength + 1]) != crc[1]) {
              throw new IOException("CRC Error in received frame: " + dlength + " bytes: " + ModbusUtil.toHex(m_ByteIn.getBuffer(), 0, dlength));
            }
          } else {
            m_ByteIn.reset(m_InBuffer, 0);
          }

          //read response
          m_ByteIn.reset(m_InBuffer, dlength);
          response.readFrom(m_ByteIn);
          done = true;
        }//synchronized
      } while (!done);
      return response;
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
      throw new ModbusIOException("I/O exception - failed to read.");
    }
  }//readResponse

  /**
   * Prepares the input and output streams of this
   * <tt>ModbusRTUTransport</tt> instance.
   *
   * @param in the input stream to be read from.
   * @param out the output stream to write to.
   * @throws IOException if an I\O error occurs.
   */
  public void prepareStreams(InputStream in, OutputStream out)
      throws IOException {
    m_InputStream = new RTUInputStream(in);
    m_OutputStream = new RTUOutputStream(out);

    m_ByteOut = new BytesOutputStream(Modbus.MAX_MESSAGE_LENGTH);
    m_InBuffer = new byte[Modbus.MAX_MESSAGE_LENGTH];
    m_ByteIn = new BytesInputStream(m_InBuffer);
    m_ByteInOut = new BytesOutputStream(m_InBuffer);
  } //prepareStreams


  private void getResponse(int fn, FastByteArrayOutputStream out)
      throws IOException {
    int bc = -1, bc2 = -1, bcw = -1;
    int i, ch;

    switch (fn) {
      case 0x01:
      case 0x02:
      case 0x03:
      case 0x04:
      case 0x0C:
      case 0x11:  // report slave ID version and run/stop state
      case 0x14:  // read log entry (60000 memory reference)
      case 0x15:  // write log entry (60000 memory reference)
      case 0x17:
        // read the byte count;
        bc = m_InputStream.read();
        out.write(bc);
        // now get the specified number of bytes and the 2 CRC bytes
        for (i = 0; i < bc + 2; i++) {
          ch = m_InputStream.read();
          out.write(ch);
        }
        break;
      case 0x05:
      case 0x06:
      case 0x0B:
      case 0x0F:
      case 0x10:
        // read status: only the CRC remains after address and function code
        for (i = 0; i < 6; i++) {
          out.write(m_InputStream.read());
        }
        break;
      case 0x07:
      case 0x08:
        // read status: only the CRC remains after address and function code
        for (i = 0; i < 3; i++) {
          out.write(m_InputStream.read());
        }
        break;
      case 0x16:
        // eight bytes in addition to the address and function codes
        for (i = 0; i < 8; i++) {
          out.write(m_InputStream.read());
        }
        break;
      case 0x18:
        // read the byte count word
        bc = m_InputStream.read();
        out.write(bc);
        bc2 = m_InputStream.read();
        out.write(bc2);
        bcw = ModbusUtil.makeWord(bc, bc2);
        // now get the specified number of bytes and the 2 CRC bytes
        for (i = 0; i < bcw + 2; i++) {
          out.write(m_InputStream.read());
        }
        break;
    }
  }//getResponse

  private final int[] calculateCRC(byte[] data, int offset, int len) {

    int[] crc = {0xFF, 0xFF};
    int nextByte = 0;
    int uIndex; /* will index into CRC lookup*/ /* table */
    /* pass through message buffer */
    for (int i = offset; i < len && i < data.length; i++) {
      nextByte = 0xFF & ((int) data[i]);
      uIndex = crc[1] ^ nextByte; //*puchMsg++; /* calculate the CRC */
      crc[1] = crc[0] ^ auchCRCHi[uIndex];
      crc[0] = auchCRCLo[uIndex];
    }

    return crc;
  }//getCRC

  /* Table of CRC values for high-order byte */
  private final static short[] auchCRCHi = {
    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
    0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
    0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
    0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41,
    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
    0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40,
    0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
    0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40,
    0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
    0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
    0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
    0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40,
    0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
    0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
    0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
    0x80, 0x41, 0x00, 0xC1, 0x81, 0x40
  };

  /* Table of CRC values for low-order byte */
  private final static short[] auchCRCLo = {
    0x00, 0xC0, 0xC1, 0x01, 0xC3, 0x03, 0x02, 0xC2, 0xC6, 0x06,
    0x07, 0xC7, 0x05, 0xC5, 0xC4, 0x04, 0xCC, 0x0C, 0x0D, 0xCD,
    0x0F, 0xCF, 0xCE, 0x0E, 0x0A, 0xCA, 0xCB, 0x0B, 0xC9, 0x09,
    0x08, 0xC8, 0xD8, 0x18, 0x19, 0xD9, 0x1B, 0xDB, 0xDA, 0x1A,
    0x1E, 0xDE, 0xDF, 0x1F, 0xDD, 0x1D, 0x1C, 0xDC, 0x14, 0xD4,
    0xD5, 0x15, 0xD7, 0x17, 0x16, 0xD6, 0xD2, 0x12, 0x13, 0xD3,
    0x11, 0xD1, 0xD0, 0x10, 0xF0, 0x30, 0x31, 0xF1, 0x33, 0xF3,
    0xF2, 0x32, 0x36, 0xF6, 0xF7, 0x37, 0xF5, 0x35, 0x34, 0xF4,
    0x3C, 0xFC, 0xFD, 0x3D, 0xFF, 0x3F, 0x3E, 0xFE, 0xFA, 0x3A,
    0x3B, 0xFB, 0x39, 0xF9, 0xF8, 0x38, 0x28, 0xE8, 0xE9, 0x29,
    0xEB, 0x2B, 0x2A, 0xEA, 0xEE, 0x2E, 0x2F, 0xEF, 0x2D, 0xED,
    0xEC, 0x2C, 0xE4, 0x24, 0x25, 0xE5, 0x27, 0xE7, 0xE6, 0x26,
    0x22, 0xE2, 0xE3, 0x23, 0xE1, 0x21, 0x20, 0xE0, 0xA0, 0x60,
    0x61, 0xA1, 0x63, 0xA3, 0xA2, 0x62, 0x66, 0xA6, 0xA7, 0x67,
    0xA5, 0x65, 0x64, 0xA4, 0x6C, 0xAC, 0xAD, 0x6D, 0xAF, 0x6F,
    0x6E, 0xAE, 0xAA, 0x6A, 0x6B, 0xAB, 0x69, 0xA9, 0xA8, 0x68,
    0x78, 0xB8, 0xB9, 0x79, 0xBB, 0x7B, 0x7A, 0xBA, 0xBE, 0x7E,
    0x7F, 0xBF, 0x7D, 0xBD, 0xBC, 0x7C, 0xB4, 0x74, 0x75, 0xB5,
    0x77, 0xB7, 0xB6, 0x76, 0x72, 0xB2, 0xB3, 0x73, 0xB1, 0x71,
    0x70, 0xB0, 0x50, 0x90, 0x91, 0x51, 0x93, 0x53, 0x52, 0x92,
    0x96, 0x56, 0x57, 0x97, 0x55, 0x95, 0x94, 0x54, 0x9C, 0x5C,
    0x5D, 0x9D, 0x5F, 0x9F, 0x9E, 0x5E, 0x5A, 0x9A, 0x9B, 0x5B,
    0x99, 0x59, 0x58, 0x98, 0x88, 0x48, 0x49, 0x89, 0x4B, 0x8B,
    0x8A, 0x4A, 0x4E, 0x8E, 0x8F, 0x4F, 0x8D, 0x4D, 0x4C, 0x8C,
    0x44, 0x84, 0x85, 0x45, 0x87, 0x47, 0x46, 0x86, 0x82, 0x42,
    0x43, 0x83, 0x41, 0x81, 0x80, 0x40
  };

} //ModbusRTUTransport
