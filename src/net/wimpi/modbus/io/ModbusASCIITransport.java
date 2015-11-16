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
import net.wimpi.modbus.ModbusCoupler;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.msg.ModbusMessage;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Class that implements the Modbus/ASCII transport
 * flavor.
 *
 * @author Dieter Wimberger
 * @author John Charlton
 * @version 1.1 (08/06/2004)
 */
public class ModbusASCIITransport
    implements ModbusSerialTransport {

  private DataInputStream m_InputStream;     //used to read from
  private ASCIIOutputStream m_OutputStream;   //used to write to

  private byte[] m_InBuffer;
  private BytesInputStream m_ByteIn;         //to read message from
  private BytesOutputStream m_ByteInOut;     //to buffer message to
  private BytesOutputStream m_ByteOut;      //write frames
  private boolean m_Echo = false;

  /**
   * Constructs a new <tt>MobusASCIITransport</tt> instance.
   */
  public ModbusASCIITransport() {
  }//constructor

  public boolean isEcho() {
    return m_Echo;
  }//isEcho

  public void setEcho(boolean b) {
    m_Echo = b;
  }//setEcho

  public void close() throws IOException {
    m_InputStream.close();
    m_OutputStream.close();
  }//close

  public void writeMessage(ModbusMessage msg)
      throws ModbusIOException {

    try {
      synchronized (m_ByteOut) {
        //write message to byte out
        msg.setHeadless();
        msg.writeTo(m_ByteOut);
        byte[] buf = m_ByteOut.getBuffer();
        int len = m_ByteOut.size();

        //write message
        m_OutputStream.write(FRAME_START);               //FRAMESTART
        m_OutputStream.write(buf, 0, len);                 //PDU
        m_OutputStream.write(calculateLRC(buf, 0, len)); //LRC
        m_OutputStream.write(FRAME_END);                 //FRAMEEND
        m_OutputStream.flush();
        m_ByteOut.reset();
      }
      // clears out the echoed message
      // for RS485
      if (m_Echo) {
        // read back the echoed message
        readEcho(500);
      }
    } catch (Exception ex) {
      throw new ModbusIOException("I/O failed to write");
    }
  }//writeMessage

  public ModbusRequest readRequest()
      throws ModbusIOException {

    boolean done = false;
    ModbusRequest request = null;

    int in = -1;

    try {
      do {
        //1. Skip to FRAME_START
        while ((in = m_InputStream.read()) != FRAME_START) ;
        //2. Read to FRAME_END
        synchronized (m_InBuffer) {
          m_ByteInOut.reset();
          while ((in = m_InputStream.read()) != FRAME_END) {
            m_ByteInOut.writeByte(in);
          }
          //check LRC
          if (m_InBuffer[m_ByteInOut.size()] !=
              calculateLRC(m_InBuffer, 0, m_ByteInOut.size(), 1)) {
            continue;
          }
          ;
          m_ByteIn.reset(m_InBuffer, m_ByteInOut.size());
          in = m_ByteIn.readUnsignedByte();
          //check unit identifier
          if (in != ModbusCoupler.getReference().getUnitID()) {
            continue;
          }
          in = m_ByteIn.readUnsignedByte();
          //create request
          request = ModbusRequest.createModbusRequest(in);
          request.setHeadless();
          //read message
          m_ByteIn.reset(m_InBuffer, m_ByteInOut.size());
          request.readFrom(m_ByteIn);
        }
        done = true;
      } while (!done);
      return request;
    } catch (Exception ex) {
      if(Modbus.debug) System.out.println(ex.getMessage());
      throw new ModbusIOException("I/O exception - failed to read.");
    }

  }//readRequest

  public ModbusResponse readResponse()
      throws ModbusIOException {

    boolean done = false;
    ModbusResponse response = null;
    int in = -1;


    try {
      do {
        //1. Skip to FRAME_START
        while ((in = m_InputStream.read()) != FRAME_START) ;
        //2. Read to FRAME_END
        synchronized (m_InBuffer) {
          m_ByteInOut.reset();
          while ((in = m_InputStream.read()) != FRAME_END) {
            m_ByteInOut.writeByte(in);
          }
          //check LRC
          if (m_InBuffer[m_ByteInOut.size()] !=
              calculateLRC(m_InBuffer, 0, m_ByteInOut.size(), 1)) {
            continue;
          }
          ;
          m_ByteIn.reset(m_InBuffer, m_ByteInOut.size());
          in = m_ByteIn.readUnsignedByte();
          //check unit identifier
          if (in != ModbusCoupler.getReference().getUnitID()) {
            continue;
          }
          in = m_ByteIn.readUnsignedByte();
          //create request
          response = ModbusResponse.createModbusResponse(in);
          response.setHeadless();
          //read message
          m_ByteIn.reset(m_InBuffer, m_ByteInOut.size());
          response.readFrom(m_ByteIn);
        }
        done = true;
      } while (!done);
      return response;
    } catch (Exception ex) {
      if(Modbus.debug) System.out.println(ex.getMessage());
      throw new ModbusIOException("I/O exception - failed to read.");
    }
  }//readResponse

  /**
   * Prepares the input and output streams of this
   * <tt>ModbusASCIITransport</tt> instance.
   * The raw input stream will be wrapped into a
   * filtered <tt>DataInputStream</tt>.
   *
   * @param in the input stream to be used for reading.
   * @param out the output stream to be used for writing.
   * @throws IOException if an I\O related error occurs.
   */
  public void prepareStreams(InputStream in, OutputStream out) throws IOException {
    m_InputStream = new DataInputStream(new ASCIIInputStream(in));
    m_OutputStream = new ASCIIOutputStream(out);
    m_ByteOut = new BytesOutputStream(Modbus.MAX_MESSAGE_LENGTH);
    m_InBuffer = new byte[Modbus.MAX_MESSAGE_LENGTH];
    m_ByteIn = new BytesInputStream(m_InBuffer);
    m_ByteInOut = new BytesOutputStream(m_InBuffer);
  }//prepareStreams

  private final static int calculateLRC(byte[] data, int off, int len) {
    int lrc = 0;
    for (int i = off; i < len; i++) {
      lrc += data[i];
    }
    return ((byte) lrc) & 0xff;
  }//calculateLRC

  private final byte calculateLRC(byte[] data, int off, int length, int tailskip) {
    int lrc = 0;
    for (int i = off; i < length - tailskip; i++) {
      lrc += data[i];
    }
    return (byte) lrc;
  }//calculateLRC

  /**
   * Reads the own message echo produced in RS485 Echo Mode
   * within the given time frame.
   *
   * @param timeOut the time frame for reading the echo.
   *
   * @return true if read successfully, false otherwise.
   *
   * @throws ModbusIOException if a Modbus I/O error occurred.
   */
  public boolean readEcho(int timeOut) throws ModbusIOException {
    boolean ret = false;
    try {
      long startTime = System.currentTimeMillis();
      int numAvailable = 0;

      while ((numAvailable = m_InputStream.available()) == 0 &&
          Math.abs(System.currentTimeMillis() - startTime) < timeOut) {
        // wait for message characters to be buffered
        try {
          Thread.sleep(10);
        } catch (InterruptedException ex) {
          System.err.println("InterruptedException: " + ex.getMessage());
        }
      }
      if (m_InputStream.available() > 0) {
        readResponse();
      }
    } catch (IOException e) {
      System.err.println("ModbusASCIITransport::clearInput: " + e);
    }
    return ret;
  }//readEcho

  /**
   * Defines a virtual number for the FRAME START token (COLON).
   */
  public static final int FRAME_START = 1000;

  /**
   * Defines a virtual number for the FRAME_END token (CR LF).
   */
  public static final int FRAME_END = 2000;

}//class ModbusASCIITransport
