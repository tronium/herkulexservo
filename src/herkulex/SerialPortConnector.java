/**
 *
 * @author Ori Novanda (cargmax.at.gmail.com)
 * 
 */

package herkulex;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SerialPortConnector {

    private String portName;
    private int portSpeed;
    private SerialPort serialPort;
    private InputStream serialIn;
    private OutputStream serialOut;

    public SerialPortConnector(String portName, int portSpeed) {
        this.portName = portName;
        this.portSpeed = portSpeed;
    }
    private boolean isOpened;

    private void setSerialPortParameters() throws UnsupportedCommOperationException {
        try {
            serialPort.setSerialPortParams(
                    portSpeed,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            serialPort.setFlowControlMode(
                    SerialPort.FLOWCONTROL_NONE);
        } catch (UnsupportedCommOperationException ex) {
            throw ex;
        }
    }

    public boolean connect() {

        if (isOpened) {
            return isOpened;
        }
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
            CommPort commPort = portIdentifier.open(portName, 1000);
            if (commPort instanceof SerialPort) {
                serialPort = (SerialPort) commPort;
                try {
                    setSerialPortParameters();
                    serialIn = serialPort.getInputStream();
                    serialOut = serialPort.getOutputStream();
                    isOpened = true;
                    Logger.getLogger(SerialPortConnector.class.getName()).log(Level.INFO, portName +  ": port is ready");
                } catch (UnsupportedCommOperationException ex) {
                    serialPort.close();
                    Logger.getLogger(SerialPortConnector.class.getName()).log(Level.SEVERE, portName + ": unsupported serial port parameter");
                } catch (IOException e) {
                    serialPort.close();
                    Logger.getLogger(SerialPortConnector.class.getName()).log(Level.SEVERE, portName + ": can't open i/o stream");
                }
            } else {
                serialPort.close();
                Logger.getLogger(SerialPortConnector.class.getName()).log(Level.SEVERE, portName + " is not a serial port");
            }
        } catch (NoSuchPortException ex) {
            Logger.getLogger(SerialPortConnector.class.getName()).log(Level.SEVERE, portName + ": NoSuchPortException");
        } catch (PortInUseException ex) {
            Logger.getLogger(SerialPortConnector.class.getName()).log(Level.SEVERE, portName + ": PortInUseException");
        }
        return isOpened;
    }

    public void close() {

        try {
            serialPort.close();
            isOpened = false;
            serialIn.close();
            serialOut.close();
        } catch (IOException ex) {
            Logger.getLogger(SerialPortConnector.class.getName()).log(Level.SEVERE, portName + ": unable to close");
        }
    }

    public SerialPort getPort() {
        return serialPort;
    }
    public InputStream getInputStream() throws IOException {
        return serialPort.getInputStream();
    }
    public OutputStream getOutputStream() throws IOException {
        return serialPort.getOutputStream();
    }
}
