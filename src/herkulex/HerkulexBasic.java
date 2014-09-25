/**
 *
 * @author Ori Novanda (cargmax.at.gmail.com)
 * 
 */

package herkulex;

import gnu.io.SerialPort;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class HerkulexBasic {
    HerkulexPacket hxPacket;
    HerkulexPacketReceiver hxr;
    HerkulexInput hxi;
    OutputStream serialOut;

    public HerkulexBasic(SerialPort serialPort)
    {
        hxPacket = new HerkulexPacket();
        hxr = new HerkulexPacketReceiver();
        hxi = new HerkulexInput(serialPort);
        try {
            serialOut = serialPort.getOutputStream();
        } catch (IOException ex) {
        }
    }
    public HerkulexBasic(Socket socket)
    {
        hxPacket = new HerkulexPacket();
        hxr = new HerkulexPacketReceiver();
        hxi = new HerkulexInput(socket);
        try {
            serialOut = socket.getOutputStream();
        } catch (IOException ex) {
        }
    }
    public void send(byte pId, byte cmd, byte[] optData)
    {
        hxPacket.buildPacket(pId, cmd, optData);
        try {
            serialOut.write(hxPacket.getBytes());
        } catch (IOException ex) {
        }
    }
    public HerkulexPacket sendAndWait(byte pId, byte cmd, byte[] optData, int timeout)
    {
        hxPacket.buildPacket(pId, cmd, optData);
        try {
            serialOut.write(hxPacket.getBytes());
        } catch (IOException ex) {
        }
        HerkulexPacket hp = hxr.getPacket(hxi, timeout);
        if(hp==null) return null;
        if(hp.getPid() != pId) return null;
        if(hp.getCmd() != (byte) (cmd + 0x40)) return null;
        return hp;
    }
    
}
