/**
 *
 * @author Ori Novanda (cargmax.at.gmail.com)
 * 
 */

package herkulex;

public class HerkulexPacket {

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

    private byte[] packet;
    
    public HerkulexPacket() {
        
    }

    protected HerkulexPacket(byte[] buffer) {
        packet = buffer;
    }
    public boolean storeBytes(byte[] buffer)
    {
        if(buffer == null || buffer.length<MINLENGTH || buffer.length>MAXLENGTH || buffer[LENGTH]<MINLENGTH || buffer[LENGTH]>MAXLENGTH) return false;
        packet = buffer;
        return true; // no checksum check
    }
    public byte[] getBytes()
    {
        return packet;
    }

    /*
    public static boolean checkLengthRequirement(byte length)
    {
        if(length < MINLENGTH || length > MAXLENGTH) return false;
        return true;
    }
    public static boolean checkFrameHeader(byte data)
    {
        if(data == 0xFF) return true;
        return false;
    }
    */
    
    public byte getPid()
    {
        return packet[PID];
    }
    public byte getCmd() {
        return packet[CMD];
    }
    public byte[] getOptData() {
        if(packet[LENGTH]<=MINLENGTH) return null;
        byte[] buffer = new byte[packet[LENGTH]-MINLENGTH];
        System.arraycopy(packet, 7, buffer, 0, buffer.length);
        return buffer;
    }

    public byte[] buildPacket(byte pID, byte cmd, byte[] data)
    {
        if(data == null)
            packet = new byte[MINLENGTH];
        else
        {
            packet = new byte[MINLENGTH + data.length];
            for(int i=0; i< data.length; i++) packet[i+MINLENGTH] = data[i];
        }
        packet[HEADER1] = HEADERCHAR;
        packet[HEADER2] = HEADERCHAR;
        packet[LENGTH] = (byte) packet.length;
        packet[PID] = pID;
        packet[CMD] = cmd;
        packet[CHECKSUM1] = getChecksum1();
        packet[CHECKSUM2] = getChecksum2();
        return packet;
    }
    private byte getChecksum1()
    {
        byte ck = (byte) (packet[LENGTH] ^ packet[PID] ^ packet[CMD]);
        if(packet.length>MINLENGTH) {
            for(int i=MINLENGTH; i< packet.length; i++) {
                ck ^= packet[i];
            }
        }
        ck &= 0xFE;
        return ck;
    }
    private byte getChecksum2(byte checksum1)
    {
        return (byte) (~checksum1 & 0xFE);
    }
    private byte getChecksum2()
    {
        return getChecksum2(packet[CHECKSUM1]);
    }
    private static final char HEX_CHARS[] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    @Override
    public String toString(){

        StringBuilder s = new StringBuilder("HERKULEX [ ");
        for(byte b : getBytes()) {
        char highNibble = HEX_CHARS[(b&0xF0)>>4];
		char lowNibble = HEX_CHARS[b&0x0F];
		s.append(highNibble);
		s.append(lowNibble);
            s.append(" ");
        }
        s.append("]");

        return s.toString();
    }

}
