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
package net.wimpi.modbus.util;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.io.BytesOutputStream;
import net.wimpi.modbus.msg.ModbusMessage;

import java.io.IOException;

/**
 * Helper class that provides utility methods.
 *
 * @author Dieter Wimberger
 * @author John Charlton
 *
 * @version 1.1 (08/06/2004)
 */
public final class ModbusUtil {

  private static BytesOutputStream m_ByteOut =
      new BytesOutputStream(Modbus.MAX_MESSAGE_LENGTH);

  /**
   * Converts a <tt>ModbusMessage</tt> instance into
   * a hex encoded string representation.
   *
   * @param msg the message to be converted.
   * @return the converted hex encoded string representation of the message.
   */
  public static final String toHex(ModbusMessage msg) {
    String ret = "-1";
    try {
      synchronized (m_ByteOut) {
        msg.writeTo(m_ByteOut);
        ret = toHex(m_ByteOut.getBuffer(), 0, m_ByteOut.size());
        m_ByteOut.reset();
      }
    } catch (IOException ex) {
    }
    return ret;
  }//toHex

  /**
   * Returns the given byte[] as hex encoded string.
   *
   * @param data a byte[] array.
   * @return a hex encoded String.
   */
  public static final String toHex(byte[] data) {
    return toHex(data, 0, data.length);
  }//toHex

  /**
   * Returns a <tt>String</tt> containing unsigned hexadecimal
   * numbers as digits.
   * The <tt>String</tt> will coontain two hex digit characters
   * for each byte from the passed in <tt>byte[]</tt>.<br>
   * The bytes will be separated by a space character.
   * <p/>
   *
   * @param data the array of bytes to be converted into a hex-string.
   * @param off the offset to start converting from.
   * @param length the number of bytes to be converted.
   *
   * @return	the generated hexadecimal representation as <code>String</code>.
   */
  public static final String toHex(byte[] data, int off, int length) {
    //double size, two bytes (hex range) for one byte
    StringBuffer buf = new StringBuffer(data.length * 2);
    for (int i = off; i < length; i++) {
      //don't forget the second hex digit
      if (((int) data[i] & 0xff) < 0x10) {
        buf.append("0");
      }
      buf.append(Long.toString((int) data[i] & 0xff, 16));
      if (i < data.length - 1) {
        buf.append(" ");
      }
    }
    return buf.toString();
  }//toHex

  /**
   * Returns a <tt>byte[]</tt> containing the given
   * byte as unsigned hexadecimal number digits.
   * <p/>
   *
   * @param i the int to be converted into a hex string.
   * @return the generated hexadecimal representation as <code>byte[]</code>.
   */
  public static final byte[] toHex(int i) {
    StringBuffer buf = new StringBuffer(2);
    //don't forget the second hex digit
    if (((int) i & 0xff) < 0x10) {
      buf.append("0");
    }
    buf.append(Long.toString((int) i & 0xff, 16).toUpperCase());
    return buf.toString().getBytes();
  }//toHex

  /**
   * Converts the register (a 16 bit value) into an unsigned short.
   * The value returned is:
   * <p><pre><code>(((a &amp; 0xff) &lt;&lt; 8) | (b &amp; 0xff))
   * </code></pre>
   * <p/>
   * This conversion has been taken from the documentation of
   * the <tt>DataInput</tt> interface.
   *
   * @param bytes a register as <tt>byte[2]</tt>.
   * @return the unsigned short value as <tt>int</tt>.
   * @see java.io.DataInput
   */
  public static final int registerToUnsignedShort(byte[] bytes) {
    return ((bytes[0] & 0xff) << 8 | (bytes[1] & 0xff));
  }//registerToUnsignedShort

  /**
   * Converts the given unsigned short into a register
   * (2 bytes).
   * The byte values in the register, in the  order
   * shown, are:
   * <p/>
   * <pre><code>
   * (byte)(0xff &amp; (v &gt;&gt; 8))
   * (byte)(0xff &amp; v)
   * </code></pre>
   * <p/>
   * This conversion has been taken from the documentation of
   * the <tt>DataOutput</tt> interface.
   *
   * @param v
   * @return the register as <tt>byte[2]</tt>.
   * @see java.io.DataOutput
   */
  public static final byte[] unsignedShortToRegister(int v) {
    byte[] register = new byte[2];
    register[0] = (byte) (0xff & (v >> 8));
    register[1] = (byte) (0xff & v);
    return register;
  }//unsignedShortToRegister

  /**
   * Converts the given register (16-bit value) into
   * a <tt>short</tt>.
   * The value returned is:
   * <p/>
   * <pre><code>
   * (short)((a &lt;&lt; 8) | (b &amp; 0xff))
   * </code></pre>
   * <p/>
   * This conversion has been taken from the documentation of
   * the <tt>DataInput</tt> interface.
   *
   * @param bytes bytes a register as <tt>byte[2]</tt>.
   * @return the signed short as <tt>short</tt>.
   */
  public static final short registerToShort(byte[] bytes) {
    return (short) ((bytes[0] << 8) | (bytes[1] & 0xff));
  }//registerToShort


  /**
   * Converts the register (16-bit value) at the given index
   * into a <tt>short</tt>.
   * The value returned is:
   * <p/>
   * <pre><code>
   * (short)((a &lt;&lt; 8) | (b &amp; 0xff))
   * </code></pre>
   * <p/>
   * This conversion has been taken from the documentation of
   * the <tt>DataInput</tt> interface.
   *
   * @param bytes a <tt>byte[]</tt> containing a short value.
   * @param idx an offset into the given byte[].
   * @return the signed short as <tt>short</tt>.
   */
  public static final short registerToShort(byte[] bytes, int idx) {
    return (short) ((bytes[idx] << 8) | (bytes[idx + 1] & 0xff));
  }//registerToShort

  /**
   * Converts the given <tt>short</tt> into a register
   * (2 bytes).
   * The byte values in the register, in the  order
   * shown, are:
   * <p/>
   * <pre><code>
   * (byte)(0xff &amp; (v &gt;&gt; 8))
   * (byte)(0xff &amp; v)
   * </code></pre>
   *
   * @param s
   * @return a register containing the given short value.
   */
  public static final byte[] shortToRegister(short s) {
    byte[] register = new byte[2];
    register[0] = (byte) (0xff & (s >> 8));
    register[1] = (byte) (0xff & s);
    return register;
  }//shortToRegister

  /**
   * Converts a byte[4] binary int value to a primitive int.<br>
   * The value returned is:
   * <p><pre>
   * <code>
   * (((a &amp; 0xff) &lt;&lt; 24) | ((b &amp; 0xff) &lt;&lt; 16) |
   * &#32;((c &amp; 0xff) &lt;&lt; 8) | (d &amp; 0xff))
   * </code></pre>
   *
   * @param bytes registers as <tt>byte[4]</tt>.
   * @return the integer contained in the given register bytes.
   */
  public static final int registersToInt(byte[] bytes) {
    return (
        ((bytes[0] & 0xff) << 24) |
        ((bytes[1] & 0xff) << 16) |
        ((bytes[2] & 0xff) << 8) |
        (bytes[3] & 0xff)
        );
  }//registersToInt

  /**
   * Converts an int value to a byte[4] array.
   *
   * @param v the value to be converted.
   * @return a byte[4] containing the value.
   */
  public static final byte[] intToRegisters(int v) {
    byte[] registers = new byte[4];
    registers[0] = (byte) (0xff & (v >> 24));
    registers[1] = (byte) (0xff & (v >> 16));
    registers[2] = (byte) (0xff & (v >> 8));
    registers[3] = (byte) (0xff & v);
    return registers;
  }//intToRegisters

  /**
   * Converts a byte[8] binary long value into a long
   * primitive.
   *
   * @param bytes a byte[8] containing a long value.
   * @return a long value.
   */
  public static final long registersToLong(byte[] bytes) {
    return (
        (((long) (bytes[0] & 0xff) << 56) |
        ((long) (bytes[1] & 0xff) << 48) |
        ((long) (bytes[2] & 0xff) << 40) |
        ((long) (bytes[3] & 0xff) << 32) |
        ((long) (bytes[4] & 0xff) << 24) |
        ((long) (bytes[5] & 0xff) << 16) |
        ((long) (bytes[6] & 0xff) << 8) |
        ((long) (bytes[7] & 0xff)))
        );
  }//registersToLong

  /**
   * Converts a long value to a byte[8].
   *
   * @param v the value to be converted.
   * @return a byte[8] containing the long value.
   */
  public static final byte[] longToRegisters(long v) {
    byte[] registers = new byte[8];
    registers[0] = (byte) (0xff & (v >> 56));
    registers[1] = (byte) (0xff & (v >> 48));
    registers[2] = (byte) (0xff & (v >> 40));
    registers[3] = (byte) (0xff & (v >> 32));
    registers[4] = (byte) (0xff & (v >> 24));
    registers[5] = (byte) (0xff & (v >> 16));
    registers[6] = (byte) (0xff & (v >> 8));
    registers[7] = (byte) (0xff & v);
    return registers;
  }//longToRegisters

  /**
   * Converts a byte[4] binary float value to a float primitive.
   *
   * @param bytes the byte[4] containing the float value.
   * @return a float value.
   */
  public static final float registersToFloat(byte[] bytes) {
    return Float.intBitsToFloat((
        ((bytes[0] & 0xff) << 24) |
        ((bytes[1] & 0xff) << 16) |
        ((bytes[2] & 0xff) << 8) |
        (bytes[3] & 0xff)
        ));
  }//registersToFloat

  /**
   * Converts a float value to a byte[4] binary float value.
   *
   * @param f the float to be converted.
   * @return a byte[4] containing the float value.
   */
  public static final byte[] floatToRegisters(float f) {
    return intToRegisters(Float.floatToIntBits(f));
  }//floatToRegisters

  /**
   * Converts a byte[8] binary double value into a double primitive.
   *
   * @param bytes a byte[8] to be converted.
   * @return a double value.
   */
  public static final double registersToDouble(byte[] bytes) {
    return Double.longBitsToDouble((
        (((long) (bytes[0] & 0xff) << 56) |
        ((long) (bytes[1] & 0xff) << 48) |
        ((long) (bytes[2] & 0xff) << 40) |
        ((long) (bytes[3] & 0xff) << 32) |
        ((long) (bytes[4] & 0xff) << 24) |
        ((long) (bytes[5] & 0xff) << 16) |
        ((long) (bytes[6] & 0xff) << 8) |
        ((long) (bytes[7] & 0xff)))
        ));
  }//registersToDouble

  /**
   * Converts a double value to a byte[8].
   *
   * @param d the double to be converted.
   * @return a byte[8].
   */
  public static final byte[] doubleToRegisters(double d) {
    return longToRegisters(Double.doubleToLongBits(d));
  }//doubleToRegisters

  /**
   * Converts an unsigned byte to an integer.
   *
   * @param b the byte to be converted.
   * @return an integer containing the unsigned byte value.
   */
  public static final int unsignedByteToInt(byte b) {
    return (int) b & 0xFF;
  }//unsignedByteToInt

  /**
   * Returns the broadcast address for the subnet of the host the code
   * is executed on.
   *
   * @return the broadcast address as <tt>InetAddress</tt>.
   *         <p/>
   *         public static final InetAddress getBroadcastAddress() {
   *         byte[] addr = new byte[4];
   *         try {
   *         addr = InetAddress.getLocalHost().getAddress();
   *         addr[3] = -1;
   *         return getAddressFromBytes(addr);
   *         } catch (Exception ex) {
   *         ex.printStackTrace();
   *         return null;
   *         }
   *         }//getBroadcastAddress
   */

  /*
  public static final InetAddress getAddressFromBytes(byte[] addr) throws Exception {
    StringBuffer sbuf = new StringBuffer();
    for (int i = 0; i < addr.length; i++) {
      if (addr[i] < 0) {
        sbuf.append(256 + addr[i]);
      } else {
        sbuf.append(addr[i]);
      }
      if (i < (addr.length - 1)) {
        sbuf.append('.');
      }
    }
    //DEBUG:System.out.println(sbuf.toString());
    return InetAddress.getByName(sbuf.toString());
  }//getAddressFromBytes
  */

  //TODO: John description.
  /**
   * Returs the low byte of an integer word.
   *
   * @param wd
   * @return
   */
  public static final byte lowByte(int wd) {
    return (new Integer(0xff & wd).byteValue());
  }// lowByte

  //TODO: John description.
  /**
   *
   * @param wd
   * @return
   */
  public static final byte hiByte(int wd) {
    return (new Integer(0xff & (wd >> 8)).byteValue());
  }// hiByte

   //TODO: John description.
  /**
   *
   * @param hibyte
   * @param lowbyte
   * @return
   */
  public static final int makeWord(int hibyte, int lowbyte) {
    int hi = 0xFF & hibyte;
    int low = 0xFF & lowbyte;
    return ((hi << 8) | low);
  }// makeWord

  
}//class ModBusUtil
