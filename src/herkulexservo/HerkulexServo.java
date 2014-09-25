/**
 *
 * @author Ori Novanda (cargmax.at.gmail.com)
 *
 */
package herkulexservo;

import herkulex.Herkulex;
import herkulex.HerkulexDataAddress;
import herkulex.HerkulexPacket;
import herkulex.SerialPortConnector;
import java.io.IOException;
import java.net.Socket;

public class HerkulexServo {

    static void test(Herkulex hxb) {
        hxb.writeRegistryRAM(Herkulex.BROADCAST_ID, HerkulexDataAddress.R_MAX_VOLTAGE, 0xFD);
        hxb.clearError(Herkulex.BROADCAST_ID);
        hxb.torqueON(Herkulex.BROADCAST_ID);
        int i;
        for (i = 0; i < 254; i++) {
            HerkulexPacket hxp = hxb.sendAndWait((byte) i, Herkulex.HSTAT, null, Herkulex.WAIT_TIME_BY_ACK);
            if (hxp != null) {
                System.out.println("Found: " + i);
                hxb.moveOne(i, 100, 0, 0);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                }
                hxb.moveOne(i, 1000, 0, 0);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                }
                hxb.moveOne(i, 100, 0, 0x60);
                break; // demo only, quit after 1 servo
            } else {
                System.out.println("Timeout: " + i);
            }
        }

        //hxb.addAngle(17, 90, 1);
        //hxb.setLed(253, 0xFF);
        hxb.torqueOFF(Herkulex.BROADCAST_ID);
    }

    public static void main(String[] args) {

        int demo_option = 0;
        
        if (demo_option == 0) {
            SerialPortConnector ts = new SerialPortConnector("COM3", 115200);
            if (ts.connect()) {
                Herkulex hxb = new Herkulex(ts.getPort());
                test(hxb);
                ts.close();
            } else {
                System.out.println("Unable to open the serial connection");
            }
        } else {
            try {
                Socket ts = new Socket("127.0.0.1", 8888);
                Herkulex hxb = new Herkulex(ts);
                test(hxb);
                ts.close();
            } catch (IOException ex) {
                System.out.println("Unable to open the Internet connection");
            }
        }
    }

}
