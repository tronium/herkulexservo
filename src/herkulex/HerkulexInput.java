/**
 *
 * @author Ori Novanda (cargmax.at.gmail.com)
 * 
 */

package herkulex;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.util.TooManyListenersException;

public class HerkulexInput {

    SerialPort serialPort;
    InputStream serialIn;

    Socket socket;
    InputStream socketIn;
    PipedInputStream piStream;
    PipedOutputStream poStream;
    
    public final Object inputSync = new Object();
    
    public HerkulexInput(SerialPort serialPort) {
        this.serialPort = serialPort;
        try {
            serialIn = serialPort.getInputStream();
        } catch (IOException ex) {
        }
        try {
            serialPort.addEventListener(new SerialPortEventListener() {

                @Override
                public void serialEvent(SerialPortEvent event) {
                    if(event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                            synchronized (inputSync) {
                                inputSync.notifyAll();
                            }
                    }

                }
            });
        } catch (TooManyListenersException e) {
        }
    }

    public HerkulexInput(Socket sock) {
        this.socket = sock;
        try {
            socketIn = socket.getInputStream();
            piStream = new PipedInputStream();
            poStream = new PipedOutputStream(piStream);
        } catch (IOException ex) {
        }
        Thread rx;
        rx = new Thread() {
            @Override
            public void run() {
                do {
                    byte[] buffer = new byte[256];
                    int l;
                    try {
                        l = socketIn.read(buffer);
                        if (l > 0) {
                            synchronized (inputSync) {
                                poStream.write(buffer, 0, l);
                                inputSync.notifyAll();
                            }
                        }
                    } catch (IOException ex) {
                    }

                } while (!socket.isClosed());
            }
        };

        rx.start();
    }

    public InputStream getInputStream() {
        if (serialIn != null) {
            return serialIn;
        } else {
            return piStream;
        }
    }
}
