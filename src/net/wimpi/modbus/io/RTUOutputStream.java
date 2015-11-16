//@licence@
package net.wimpi.modbus.io;

import java.io.OutputStream;
import java.io.IOException;

/**
 * Class implementing a specialized <tt>OutputStream</tt> which
 * encodes bytes written to the stream into two hexadecimal
 * characters each.
 *
 * @author John Charlton
 * @version 1.1 (08/06/2004)
 *
 */
public class RTUOutputStream
    extends OutputStream {

  private OutputStream m_Output;

  /**
   * Constructs a new <tt>RTUOutputStream</tt> instance
   * writing to the given <tt>OutputStream</tt>.
   *
   * @param out an output stream instance to be wrapped.
   */
  public RTUOutputStream(OutputStream out) {
    m_Output = out;
  }//constructor

  /**
   * Writes a byte encoded as two hexadecimal characters to
   * the raw output stream.
   *
   * @param b the byte to be written as <tt>int</tt>.
   * @throws IOException if an I/O related error occurs.
   */
  public void write(int b) throws IOException {
    m_Output.write(b);
    //System.out.println("Wrote byte "+b+"="+new String(ModbusUtil.toHex(b)));
  }//write

  /**
   * Writes an array of bytes encoded as two hexadecimal
   * characters to the raw output stream.
   *
   * @param data the <tt>byte[]</tt> to be written.
   * @throws IOException if an I/O related error occurs.
   */
  public void write(byte[] data) throws IOException {
    m_Output.write(data);
  }//write(byte[])

}//class RTUOutputStream
