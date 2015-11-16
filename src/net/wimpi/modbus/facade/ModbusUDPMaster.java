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
package net.wimpi.modbus.facade;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.ModbusSlaveException;
import net.wimpi.modbus.io.ModbusUDPTransaction;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.net.UDPMasterConnection;
import net.wimpi.modbus.procimg.SimpleInputRegister;
import net.wimpi.modbus.procimg.SimpleRegister;
import net.wimpi.modbus.util.BitVector;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Modbus/UDP Master facade.
 *
 * @author Dieter Wimberger
 * @version 1.1 (08/06/2004)
 */
public class ModbusUDPMaster {

  private UDPMasterConnection m_Connection;
  private InetAddress m_SlaveAddress;
  private ModbusUDPTransaction m_Transaction;
  private ReadCoilsRequest m_ReadCoilsRequest;
  private ReadInputDiscretesRequest m_ReadInputDiscretesRequest;
  private WriteCoilRequest m_WriteCoilRequest;
  private WriteMultipleCoilsRequest m_WriteMultipleCoilsRequest;
  private ReadInputRegistersRequest m_ReadInputRegistersRequest;
  private ReadMultipleRegistersRequest m_ReadMultipleRegistersRequest;
  private WriteSingleRegisterRequest m_WriteSingleRegisterRequest;
  private WriteMultipleRegistersRequest m_WriteMultipleRegistersRequest;

  /**
   * Constructs a new master facade instance for communication
   * with a given slave.
   *
   * @param addr an internet address as resolvable IP name or IP number,
   *             specifying the slave to communicate with.
   */
  public ModbusUDPMaster(String addr) {
    try {
      m_SlaveAddress = InetAddress.getByName(addr);
      m_Connection = new UDPMasterConnection(m_SlaveAddress);
      m_ReadCoilsRequest = new ReadCoilsRequest();
      m_ReadInputDiscretesRequest = new ReadInputDiscretesRequest();
      m_WriteCoilRequest = new WriteCoilRequest();
      m_WriteMultipleCoilsRequest = new WriteMultipleCoilsRequest();
      m_ReadInputRegistersRequest = new ReadInputRegistersRequest();
      m_ReadMultipleRegistersRequest = new ReadMultipleRegistersRequest();
      m_WriteSingleRegisterRequest = new WriteSingleRegisterRequest();
      m_WriteMultipleRegistersRequest = new WriteMultipleRegistersRequest();

    } catch (UnknownHostException e) {
      throw new RuntimeException(e.getMessage());
    }
  }//constructor

  /**
   * Constructs a new master facade instance for communication
   * with a given slave.
   *
   * @param addr an internet address as resolvable IP name or IP number,
   *             specifying the slave to communicate with.
   * @param port the port the slave is listening to.
   */
  public ModbusUDPMaster(String addr, int port) {
    this(addr);
    m_Connection.setPort(port);
  }//constructor

  /**
   * Connects this <tt>ModbusUDPMaster</tt> with the slave.
   *
   * @throws Exception if the connection cannot be established.
   */
  public void connect()
      throws Exception {
    m_Connection.connect();
    m_Transaction = new ModbusUDPTransaction(m_Connection);
  }//connect

  /**
   * Disconnects this <tt>ModbusTCPMaster</tt> from the slave.
   */
  public void disconnect() {
    m_Connection.close();
    m_Transaction = null;
  }//disconnect

  /**
   * Reads a given number of coil states from the slave.
   *
   * @param ref   (IN) the offset of the coil to start reading from.
   * @param count (IN) the number of coil states to be read.
   * @param coils (OUT) a <tt>BitVector</tt> which will be used to return the
   *              received coil states.
   * @return 0 if the slave responded correctly, the code of the slave exception otherwise.
   * @throws net.wimpi.modbus.ModbusException
   *          if the command transaction fails.
   */
  public synchronized int readCoils(int ref, int count, BitVector coils)
      throws ModbusException {
    m_ReadCoilsRequest.setReference(ref);
    m_ReadCoilsRequest.setBitCount(count);
    m_Transaction.setRequest(m_ReadCoilsRequest);
    try {
      m_Transaction.execute();
      coils.setBytes(((ReadCoilsResponse) m_Transaction.getResponse()).getCoils().getBytes(),
          count);
      return 0;
    } catch (ModbusException ex) {
      if (ex instanceof ModbusSlaveException) {
        return ((ModbusSlaveException) ex).getType();
      }
      throw ex;
    }
  }//readCoils

  /**
   * Writes a coil state to the slave.
   *
   * @param ref   (IN) the offset of the coil to be written.
   * @param state (IN) the coil state to be written.
   * @return 0 if the slave responded correctly, the code of the slave exception otherwise.
   * @throws net.wimpi.modbus.ModbusException
   *          if the command transaction fails.
   */
  public synchronized int writeCoil(int ref, boolean state)
      throws ModbusException {

    m_WriteCoilRequest.setReference(ref);
    m_WriteCoilRequest.setCoil(state);
    m_Transaction.setRequest(m_WriteCoilRequest);
    try {
      m_Transaction.execute();
      return 0;
    } catch (ModbusException ex) {
      if (ex instanceof ModbusSlaveException) {
        return ((ModbusSlaveException) ex).getType();
      }
      throw ex;
    }
  }//writeCoil


  /**
   * Writes a given number of coil states to the slave.
   *
   * @param ref   (IN) the offset of the coil to start writing to.
   * @param count (IN) the number of coil states to be written.
   * @param coils (IN) a <tt>BitVector</tt> which holds the coil states to be written.
   * @return 0 if the slave responded correctly, the code of the slave exception otherwise.
   * @throws net.wimpi.modbus.ModbusException
   *          if the command transaction fails.
   */
  public synchronized int writeMultipleCoils(int ref, int count, BitVector coils)
      throws ModbusException {
    m_WriteMultipleCoilsRequest.setReference(ref);
    m_WriteMultipleCoilsRequest.setBitCount(count);
    m_WriteMultipleCoilsRequest.getCoils()
        .setBytes(coils.getBytes(), count);
    m_Transaction.setRequest(m_WriteMultipleCoilsRequest);
    try {
      m_Transaction.execute();
      return 0;
    } catch (ModbusException ex) {
      if (ex instanceof ModbusSlaveException) {
        return ((ModbusSlaveException) ex).getType();
      }
      throw ex;
    }
  }//writeMultipleCoils

  /**
   * Reads a given number of input discrete states from the slave.
   *
   * @param ref    (IN) the offset of the input discrete to start reading from.
   * @param count  (IN) the number of input discrete states to be read.
   * @param indisc (OUT) a <tt>BitVector</tt> which will be used to return the
   *               received input discrete states.
   * @return 0 if the slave responded correctly, the code of the slave exception otherwise.
   * @throws net.wimpi.modbus.ModbusException
   *          if the command transaction fails.
   */
  public synchronized int readInputDiscretes(int ref, int count, BitVector indisc)
      throws ModbusException {
    m_ReadInputDiscretesRequest.setReference(ref);
    m_ReadInputDiscretesRequest.setBitCount(count);
    m_Transaction.setRequest(m_ReadInputDiscretesRequest);
    try {
      m_Transaction.execute();
      indisc.setBytes(((ReadInputDiscretesResponse) m_Transaction.getResponse()).getDiscretes().getBytes(),
          count);
      return 0;
    } catch (ModbusException ex) {
      if (ex instanceof ModbusSlaveException) {
        return ((ModbusSlaveException) ex).getType();
      }
      throw ex;
    }
  }//readInputDiscretes


  /**
   * Reads a given number of input registers from the slave.
   *
   * @param ref       (IN) the offset of the input register to start reading from.
   * @param count     (IN) the number of input registers to be read.
   * @param registers (OUT) a <tt>SimpleInputRegister[]</tt> which will be used to return the
   *                  received input register values.
   * @return 0 if the slave responded correctly, the code of the slave exception otherwise.
   * @throws net.wimpi.modbus.ModbusException
   *          if the command transaction fails.
   */
  public synchronized int readInputRegisters(int ref, int count, SimpleInputRegister[] registers)
      throws ModbusException {
    m_ReadInputRegistersRequest.setReference(ref);
    m_ReadInputRegistersRequest.setWordCount(count);
    m_Transaction.setRequest(m_ReadInputRegistersRequest);
    try {
      m_Transaction.execute();
      ReadInputRegistersResponse res = (ReadInputRegistersResponse) m_Transaction.getResponse();
      for (int i = 0; i < count; i++) {
        registers[i] = (SimpleInputRegister) res.getRegister(i);
      }
      return 0;
    } catch (ModbusException ex) {
      if (ex instanceof ModbusSlaveException) {
        return ((ModbusSlaveException) ex).getType();
      }
      throw ex;
    }
  }//readInputRegisters

  /**
   * Reads a given number of registers from the slave.
   *
   * @param ref       (IN) the offset of the register to start reading from.
   * @param count     (IN) the number of registers to be read.
   * @param registers (OUT) a <tt>SimpleRegister[]</tt> which will be used to return the
   *                  received register values.
   * @return 0 if the slave responded correctly, the code of the slave exception otherwise.
   * @throws net.wimpi.modbus.ModbusException
   *          if the command transaction fails.
   */
  public synchronized int readMultipleRegisters(int ref, int count,
                                                SimpleRegister[] registers)
      throws ModbusException {

    m_ReadMultipleRegistersRequest.setReference(ref);
    m_ReadMultipleRegistersRequest.setWordCount(count);
    m_Transaction.setRequest(m_ReadMultipleRegistersRequest);
    try {
      m_Transaction.execute();
      ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) m_Transaction.getResponse();
      for (int i = 0; i < count; i++) {
        registers[i] = (SimpleRegister) res.getRegister(i);
      }
      return 0;
    } catch (ModbusException ex) {
      if (ex instanceof ModbusSlaveException) {
        return ((ModbusSlaveException) ex).getType();
      }
      throw ex;
    }
  }//readMultipleRegisters

  /**
   * Writes a register to the slave.
   *
   * @param ref      (IN) the offset of the register to be written.
   * @param register (IN) a <tt>SimpleRegister</tt> holding the value of the register
   *                 to be written.
   * @return 0 if the slave responded correctly, the code of the slave exception otherwise.
   * @throws net.wimpi.modbus.ModbusException
   *          if the command transaction fails.
   */
  public synchronized int writeSingleRegister(int ref, SimpleRegister register)
      throws ModbusException {

    m_WriteSingleRegisterRequest.setReference(ref);
    m_WriteSingleRegisterRequest.setRegister(register);
    m_Transaction.setRequest(m_WriteSingleRegisterRequest);
    try {
      m_Transaction.execute();
      return 0;
    } catch (ModbusException ex) {
      if (ex instanceof ModbusSlaveException) {
        return ((ModbusSlaveException) ex).getType();
      }
      throw ex;
    }
  }//writeSingleRegister

  /**
   * Writes a number of registers to the slave.
   *
   * @param ref       (IN) the offset of the register to start writing to.
   * @param registers (IN) a <tt>SimpleRegister[]</tt> holding the values of
   *                  the registers to be written.
   * @return 0 if the slave responded correctly, the code of the slave exception otherwise.
   * @throws net.wimpi.modbus.ModbusException
   *          if the command transaction fails.
   */
  public synchronized int writeMultipleRegisters(int ref, SimpleRegister[] registers)
      throws ModbusException {

    m_WriteMultipleRegistersRequest.setReference(ref);
    m_WriteMultipleRegistersRequest.setRegisters(registers);
    m_Transaction.setRequest(m_WriteMultipleRegistersRequest);
    try {
      m_Transaction.execute();
      return 0;
    } catch (ModbusException ex) {
      if (ex instanceof ModbusSlaveException) {
        return ((ModbusSlaveException) ex).getType();
      }
      throw ex;
    }
  }//writeMultipleRegisters

}//class ModbusUDPMaster
