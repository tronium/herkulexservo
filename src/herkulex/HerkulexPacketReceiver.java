/**
 *
 * @author Ori Novanda (cargmax.at.gmail.com)
 * 
 */

package herkulex;

import java.io.IOException;
import java.io.InputStream;

public class HerkulexPacketReceiver {

    public int state;
    public byte[] packet;

    private final int STATE_WAIT_FIRST_HEADER = 0;
    private final int STATE_WAIT_SECOND_HEADER = 1;
    private final int STATE_WAIT_PACKET_LENGTH = 2;
    private final int STATE_WAIT_PID = 3;
    private final int STATE_WAIT_CMD = 4;
    private final int STATE_WAIT_FIRST_CHECKSUM = 5;
    private final int STATE_WAIT_SECOND_CHECKSUM = 6;
    private final int STATE_WAIT_DATA = 7;
    private final int STATE_COMPLETED = 8;

    private final byte HEADERCHAR = (byte) 0xFF;

    private final int HEADER1 = 0;
    private final int HEADER2 = 1;
    private final int LENGTH = 2;
    private final int PID = 3;
    private final int CMD = 4;
    private final int CHECKSUM1 = 5;
    private final int CHECKSUM2 = 6;

    private final int MINLENGTH = 7;
    private final int MAXLENGTH = 233;

    private int dataIndex;
    private byte checkSum1;

    public HerkulexPacket getPacket(HerkulexInput hxInput, int timeout) {
        //int timeoutCount = 0;
        InputStream serialIn = hxInput.getInputStream();
        HerkulexPacket ack = null;
        try {
            do {
                synchronized (hxInput.inputSync) {
                    if (serialIn.available() < 1) {
                        try {
                            hxInput.inputSync.wait(timeout);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
                if (serialIn.available() > 0) {
                    ack = getPacket(serialIn);
                } else {
                    //if (timeoutCount++ > 0) {
                    break;
                    //}
                }
            } while (ack == null);
            return ack;
        } catch (IOException ex) {
            return null;
        }
    }

    public HerkulexPacket getPacket(InputStream serialIn) {
        try {
            while (serialIn.available() > 0) {
                byte b = (byte) serialIn.read();
                if (state == STATE_WAIT_FIRST_HEADER) {
                    if (b == HEADERCHAR) {
                        state = STATE_WAIT_SECOND_HEADER;
                    }
                } else if (state == STATE_WAIT_SECOND_HEADER) {
                    if (b == HEADERCHAR) {
                        state = STATE_WAIT_PACKET_LENGTH;
                    }
                } else if (state == STATE_WAIT_PACKET_LENGTH) {
                    if (b < MINLENGTH || b > MAXLENGTH) {
                        if (b != HEADERCHAR) {
                            state = STATE_WAIT_FIRST_HEADER;
                        }
                    } else {
                        packet = new byte[b];
                        packet[HEADER1] = HEADERCHAR;
                        packet[HEADER2] = HEADERCHAR;
                        packet[LENGTH] = b;
                        checkSum1 = b;
                        state = STATE_WAIT_PID;
                    }
                } else if (state == STATE_WAIT_PID) {
                    packet[PID] = b;
                    checkSum1 ^= b;
                    state = STATE_WAIT_CMD;
                } else if (state == STATE_WAIT_CMD) {
                    packet[CMD] = b;
                    checkSum1 ^= b;
                    state = STATE_WAIT_FIRST_CHECKSUM;
                } else if (state == STATE_WAIT_FIRST_CHECKSUM) {
                    packet[CHECKSUM1] = b;
                    state = STATE_WAIT_SECOND_CHECKSUM;
                } else {
                    if (state == STATE_WAIT_SECOND_CHECKSUM) {
                        packet[CHECKSUM2] = b;
                        if (packet[LENGTH] > MINLENGTH) {
                            dataIndex = MINLENGTH;
                            state = STATE_WAIT_DATA;
                        } else {
                            state = STATE_COMPLETED;
                        }
                    } else if (state == STATE_WAIT_DATA) {
                        packet[dataIndex++] = b;
                        checkSum1 ^= b;
                        if ((dataIndex) >= packet[LENGTH]) {
                            state = STATE_COMPLETED;
                        }
                    }
                    if (state == STATE_COMPLETED) {
                        state = STATE_WAIT_FIRST_HEADER;
                        checkSum1 &= 0xFE;
                        if (checkSum1 != packet[CHECKSUM1]) {
                            return null;
                        }
                        checkSum1 = (byte) (~checkSum1 & 0xFE);
                        if (checkSum1 != packet[CHECKSUM2]) {
                            return null;
                        }
                        return new HerkulexPacket(packet);
                    }
                }
            }
            return null;
        } catch (IOException ex) {
            state = STATE_WAIT_FIRST_HEADER;
            return null;
        }
    }
}
