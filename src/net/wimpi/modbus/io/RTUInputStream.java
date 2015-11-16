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

import java.io.IOException;
import java.io.InputStream;

/**
 * Class implementing a specialized <tt>InputStream</tt> which
 * decodes characters read from the raw stream into bytes.
 *
 * @author John Charlton
 * 
 * @version 1.1 (08/06/2004)
 */
public class RTUInputStream
    extends InputStream {

  private InputStream m_Input;

  /**
   * Defines a stream timeout in [ms].
   */
  //TODO:John description.
  public int timeOutMsec = 500;  // this is a bit long to allow for
  // slave response time in sending a new
  // response packet

  /**
   * Constructs a new <tt>RTUInputStream</tt> instance
   * reading from the given <tt>InputStream</tt>.
   *
   * @param in a base input stream to be wrapped.
   */
  public RTUInputStream(InputStream in) {
    m_Input = in;
  }//constructor
  
  public int available() throws IOException {
    int numAvailable = 0;
    
    numAvailable = m_Input.available();
    return numAvailable;
  }

  /**
   * Reads a byte from the binary stream.
   *
   * @return int the byte read from the stream.
   * @throws IOException if an I/O related error occurs.
   */
  public int read() throws IOException {
    long startTime = System.currentTimeMillis();
    int numAvailable = 0;

    int ch = -1;
    while ((numAvailable = m_Input.available()) == 0 &&
        Math.abs(System.currentTimeMillis() - startTime) < timeOutMsec) {
      // wait for message characters to be buffered
      try {
        Thread.sleep(10);
      } catch (InterruptedException ex) {
        if(Modbus.debug) System.err.println("InterruptedException: " + ex.getMessage());
      }
    }
    if (numAvailable == 0) {
      // this means a timeout error occurred
      throw new IOException("Timeout error waiting for response");
    }
    ch = m_Input.read();

    return ch;
  }//read

}//class RTUInputStream
