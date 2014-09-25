/**
 *
 * @original_author jgahn (updpang2zero@hotmail.com) https://github.com/dongburobot/HerkuleXProcessing/
 * @original_modified 2013.05.20
 * @original_version Sushi (0.1.1)
 * 
 * @modifiedby Ori Novanda (cargmax.at.gmail.com)
 * Modification:
 * - Extend the functionality by exploding into several classes
 * - Most of the private functions are replaced with different worker functions
 * - Serial-via-Internet ready
 * - Replace the SendData() to send() on a base class
 * - Replace the sendData() + readData() to sendAndWait() to support "timeout" without causing delay in receiving data
 * - Individual playtime parameters are accepted through addMove() -> actionAll()
 * - New function: readRegistryRAM()
 */

package herkulex;

import gnu.io.SerialPort;
import java.net.Socket;
import java.util.ArrayList;


public class Herkulex extends HerkulexBasic{

    //public final static String VERSION = "1.0";
    private static final int BASIC_PKT_SIZE = 7;
    public static int WAIT_TIME_BY_ACK = 100;

    // SERVO HERKULEX COMMAND - See Manual p40
    public static final byte HEEPWRITE = 0x01; 		//Rom write
    public static final byte HEEPREAD = 0x02; 		//Rom read
    public static final byte HRAMWRITE = 0x03; 		//Ram write
    public static final byte HRAMREAD = 0x04; 		//Ram read
    public static final byte HIJOG = 0x05; 		//Write n servo with different timing
    public static final byte HSJOG = 0x06; 		//Write n servo with same time
    public static final byte HSTAT = 0x07; 		//Read error
    public static final byte HROLLBACK = 0x08; 		//Back to factory value
    public static final byte HREBOOT = 0x09; 		//Reboot

    // HERKULEX Broadcast Servo ID
    public static final byte BROADCAST_ID = (byte) 0xFE;

    // HERKULEX LED - See Manual p29
    public static final byte LED_RED = 0x10;
    public static final byte LED_GREEN = 0x04;
    public static final byte LED_BLUE = 0x08;

    // HERKULEX STATUS ERROR - See Manual p39
    public static final byte H_STATUS_OK = 0x00;
    public static final byte H_ERROR_INPUT_VOLTAGE = 0x01;
    public static final byte H_ERROR_POS_LIMIT = 0x02;
    public static final byte H_ERROR_TEMPERATURE_LIMIT = 0x04;
    public static final byte H_ERROR_INVALID_PKT = 0x08;
    public static final byte H_ERROR_OVERLOAD = 0x10;
    public static final byte H_ERROR_DRIVER_FAULT = 0x20;
    public static final byte H_ERROR_EEPREG_DISTORT = 0x40;


    public static final byte H_ERROR2_MOVING = 0x01;
    public static final byte H_ERROR2_INPOSITION = 0x02;
    public static final byte H_ERROR2_CHECKSUM = 0x04;
    public static final byte H_ERROR2_COMMAND = 0x08;
    public static final byte H_ERROR2_UNKNOWN = 0x10;
    public static final byte H_ERROR2_RANGE = 0x20;
    public static final byte H_ERROR2_GARBAGE = 0x40;
    public static final byte H_ERROR2_MOTOR_ON = (byte) 0x80;


    private ArrayList<Byte> multipleMoveData;

    private ArrayList<Integer> mIDs;

    private void myConstructor() {
        multipleMoveData = new ArrayList<>();
        mIDs = new ArrayList<>();
        //writeRegistryRAM(Herkulex.BROADCAST_ID, HerkulexDataAddress.R_MAX_VOLTAGE, 0XFD);
        //clearError(Herkulex.BROADCAST_ID);
        //torqueON(Herkulex.BROADCAST_ID);
    }
    public Herkulex(SerialPort sp) {
        super(sp);
        myConstructor();
    }

    public Herkulex(Socket soc) {
        super(soc);
        myConstructor();
    }

    /*
     public HerkuleX(Serial port) {
     mPort = port;
     //mParent = parent;
     multipleMoveData = new ArrayList<Byte>();
     mIDs = new ArrayList<Integer>();
     }
     */
    /**
     * @example HerkuleX
     *
     * Initialize servos.
     *
     * 1. clearError(); 2. setAckPolicy(1); 3. torqueON(BROADCAST_ID);
     */
    public void initialize() {
        try {
            Thread.sleep(100);
            clearError(BROADCAST_ID);	// clear error for all servos
            Thread.sleep(10);
            setAckPolicy(1);			// set ACK policy
            Thread.sleep(10);
            torqueON(BROADCAST_ID);		// torqueON for all servos
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
    }

    /**
     * @example HerkuleX_Get_IDs
     *
     * Get connected servos'ID
     *
     * Byte (-128~127) in Java In terms of that Processing is running on PC,
     * there is no memory problem. In this reason, this return type is Integer
     * array.
     *
     * @return ArrayList<Integer> - Servo IDs
     */
    public ArrayList<Integer> performIDScan() {
        mIDs.clear();

        for (int i = 0; i < 254; i++) {
            if (getPosition(i) != -1) {
                mIDs.add(i);
            }
        }

        return mIDs;
    }

    /**
     * Set Ack Policy
     *
     * @param valueACK 0=No Replay, 1=Only reply to READ CMD, 2=Always reply
     */
    public void setAckPolicy(int valueACK) {
        if (valueACK < 0 || valueACK > 2) {
            return;
        }

        byte[] optData = new byte[3];
        optData[0] = 0x34;             	// Address
        optData[1] = 0x01;             	// Length
        optData[2] = (byte) valueACK;   // Value. 0=No Replay, 1=Only reply to READ CMD, 2=Always reply

        send((byte) 0xFE, HRAMWRITE, optData);
    }

    /**
     * Error clear
     *
     * @param servoID 0 ~ 254 (0x00 ~ 0xFE), 0xFE : BROADCAST_ID
     */
    public void clearError(int servoID) {
        byte[] optData = new byte[4];
        optData[0] = 0x30;               // Address
        optData[1] = 0x02;               // Length
        optData[2] = 0x00;               // Write error=0
        optData[3] = 0x00;               // Write detail error=0

        send((byte) servoID, HRAMWRITE, optData);
    }

    /**
     * Torque ON
     *
     * @param servoID 0 ~ 254 (0x00 ~ 0xFE), 0xFE : BROADCAST_ID
     */
    public void torqueON(int servoID) {
        byte[] optData = new byte[3];
        optData[0] = 0x34;               	// Address
        optData[1] = 0x01;              	// Length
        optData[2] = 0x60;               	// 0x60=Torque ON

        send((byte) servoID, HRAMWRITE, optData);
    }

    /**
     * Torque OFF
     *
     * @param servoID 0 ~ 254 (0x00 ~ 0xFE), 0xFE : BROADCAST_ID
     */
    public void torqueOFF(int servoID) {
        byte[] optData = new byte[3];
        optData[0] = 0x34;               	// Address
        optData[1] = 0x01;              	// Length
        optData[2] = 0x00;               	// 0x60=Torque ON

        send((byte) servoID, HRAMWRITE, optData);
    }

    /**
     * @example HerkuleX_Infinite_Turn
     *
     * Move one servo with continous rotation
     *
     * @param servoID 0 ~ 254 (0x00 ~ 0xFE), 0xFE : BROADCAST_ID
     * @param goalSpeed -1023 ~ 1023 [CW:Negative Value(-), CCW:Positive
     * Value(+)]
     * @param playTime 0 ~ 2856ms
     * @param led HerkuleX.LED_RED | HerkuleX.LED_GREEN | HerkuleX.LED_BLUE
     */
    public void moveSpeedOne(int servoID, int goalSpeed, int playTime, int led) {
        if (goalSpeed > 1023 || goalSpeed < -1023) {
            return;       // speed (goal) non correct
        }
        if ((playTime < 0) || (playTime > 2856)) {
            return;
        }

        int goalSpeedSign;
        if (goalSpeed < 0) {
            goalSpeedSign = (-1) * goalSpeed;
            goalSpeedSign |= 0x4000;
        } else {
            goalSpeedSign = goalSpeed;
        }

        int speedGoalLSB = goalSpeedSign & 0X00FF; 		       // MSB speedGoal 
        int speedGoalMSB = (goalSpeedSign & 0xFF00) >> 8;      // LSB speedGoal 

        playTime = (int) (playTime / 11.2f);		// ms --> value
        led = (byte) (led | 0x02);					// Speed Ctrl Mode

        byte[] optData = new byte[5];
        optData[0] = (byte) playTime;  		// Execution time	
        optData[1] = (byte) speedGoalLSB;
        optData[2] = (byte) speedGoalMSB;
        optData[3] = (byte) led;
        optData[4] = (byte) servoID;

        send((byte) servoID, HSJOG, optData);
    }

    /**
     * @example HerkuleX_Infinite_Turn
     *
     * Get current servo speed (-1023 ~ 1023)
     *
     * @param servoID 0 ~ 253 (0x00 ~ 0xFD)
     * @return current speed -1023 ~ 1023 [CW:Negative Value(-), CCW:Positive
     * Value(+)]
     */
    public int getSpeed(int servoID) {
        if ((byte) servoID == 0xFE) {
            return 0;
        }

        byte[] optData = new byte[2];
        optData[0] = 0x40;               	// Address
        optData[1] = 0x02;              	// Length

        HerkulexPacket hxp = sendAndWait((byte) servoID, HRAMREAD, optData, WAIT_TIME_BY_ACK);

        if (hxp == null) {
            return 0;
        }
        byte[] readBuf = hxp.getBytes();

        int speedy = ((readBuf[10] & 0x03) << 8) | (readBuf[9] & 0xFF);

        if ((readBuf[10] & 0x40) == 0x40) {
            speedy *= -1;
        }

        return speedy;
    }

    /**
     * @example HerkuleX_Pos_Ctrl
     *
     * Move one servo at goal position 0 - 1023
     *
     * @param servoID 0 ~ 254 (0x00 ~ 0xFE), 0xFE : BROADCAST_ID
     * @param goalPos 0 ~ 1023
     * @param playTime 0 ~ 2856ms
     * @param led HerkuleX.LED_RED | HerkuleX.LED_GREEN | HerkuleX.LED_BLUE
     */
    public void moveOne(int servoID, int goalPos, int playTime, int led) {
        //if (servoID == 3 ) {
        //    int test = 3;
        //}
        if (goalPos > 1023 || goalPos < 0) {
            return;       // speed (goal) non correct
        }
        if ((playTime < 0) || (playTime > 2856)) {
            return;
        }

        // Position definition
        int posLSB = goalPos & 0X00FF;			// MSB Pos
        int posMSB = (goalPos & 0XFF00) >> 8;	// LSB Pos
        playTime = (int) (playTime / 11.2f);	// ms --> value
        led = (byte) (led & 0xFD);				// Pos Ctrl Mode

        byte[] optData = new byte[5];
        optData[0] = (byte) playTime;  		// Execution time	
        optData[1] = (byte) posLSB;
        optData[2] = (byte) posMSB;
        optData[3] = (byte) led;
        optData[4] = (byte) servoID;

        send((byte) servoID, HSJOG, optData);
    }

    /**
     * Get servo position
     *
     * @param servoID 0 ~ 253 (0x00 ~ 0xFD)
     * @return current position 0 ~ 1023 (-1: failure)
     * @example HerkuleX_Pos_Ctrl
     */
    public int getPosition(int servoID) {
        if ((byte) servoID == 0xFE) {
            return -1;
        }

        byte[] optData = new byte[2];
        optData[0] = 0x3A;               	// Address
        optData[1] = 0x02;              	// Length

        HerkulexPacket hxp = sendAndWait((byte) servoID, HRAMREAD, optData, WAIT_TIME_BY_ACK);

        if (hxp == null) {
            return -1;
        }
        byte[] readBuf = hxp.getBytes();

        int pos = ((readBuf[10] & 0x03) << 8) | (readBuf[9] & 0xFF);
        return pos;
    }

    /**
     * Move one servo to an angle between -167 and 167
     *
     * @param servoID 0 ~ 254 (0x00 ~ 0xFE), 0xFE : BROADCAST_ID
     * @param angle -166 ~ 166 degrees
     * @param playTime 0 ~ 2856 ms
     * @param led HerkuleX.LED_RED | HerkuleX.LED_GREEN | HerkuleX.LED_BLUE
     */
    public void moveOneAngle(int servoID, float angle, int playTime, int led) {
        if (angle > 167.0 || angle < -167.0) {
            return;
        }
        int position = (int) (angle / 0.325) + 512;
        moveOne(servoID, position, playTime, led);
    }

    /**
     * Get servo position in degrees
     *
     * @param servoID 0 ~ 253 (0x00 ~ 0xFD)
     * @return current angle -166.7 ~ 166.7 degrees
     */
    public float getAngle(int servoID) {
        int pos = getPosition(servoID);
        if (pos < 0) {
            return 0;
        }
        return (pos - 512) * 0.325f;
    }

    /**
     * @HerkuleX_Unison_Movement
     *
     * Add one servo movement data
     *
     * ex) addMove(0, 512, HerkuleX.LED_RED); addMove(1, 235,
     * HerkuleX.LED_GREEN); addMove(2, 789, HerkuleX.LED_BLUE); actionAll(1000);
     *
     * @param servoID 0 ~ 253 (0x00 ~ 0xFD)
     * @param goal 0 ~ 1023
     * @param led HerkuleX.LED_RED | HerkuleX.LED_GREEN | HerkuleX.LED_BLUE
     */
    public void addMove(int servoID, int goal, int led) {
        if (goal > 1023 || goal < 0) {
            return;						 //0 <--> 1023 range
        }
        // Position definition
        int posLSB = goal & 0X00FF;			// MSB Pos
        int posMSB = (goal & 0XFF00) >> 8;	// LSB Pos
        led = (byte) (led & 0xFD);			// Pos Ctrl Mode

        byte[] optData = new byte[4];
        optData[0] = (byte) posLSB;
        optData[1] = (byte) posMSB;
        optData[2] = (byte) led;
        optData[3] = (byte) servoID;

        addData(optData);	//add servo data to list, pos mode
    }

    public void addMove(int servoID, int goal, int playTime, int led) {
        if (goal > 1023 || goal < 0) {
            return;						 //0 <--> 1023 range
        }
        // Position definition
        int posLSB = goal & 0X00FF;			// MSB Pos
        int posMSB = (goal & 0XFF00) >> 8;	// LSB Pos
        playTime = (int) (playTime / 11.2f);	// ms --> value
        led = (byte) (led & 0xFD);			// Pos Ctrl Mode

        byte[] optData = new byte[5];
        optData[0] = (byte) posLSB;
        optData[1] = (byte) posMSB;
        optData[2] = (byte) led;
        optData[3] = (byte) servoID;
        optData[4] = (byte) playTime;

        addData(optData);	//add servo data to list, pos mode
    }


    /**
     * @example HerkuleX_Unison_Movement
     *
     * Add one servo movement data in degrees
     *
     * ex) addAngle(0, -90.5f, HerkuleX.LED_RED); addAngle(1, 0,
     * HerkuleX.LED_BLUE); addAngle(2, 90.5f, HerkuleX.LED_GREEN);
     * actionAll(1000);
     *
     * @param servoID 0 ~ 253 (0x00 ~ 0xFD)
     * @param angle -167 ~ 167 degrees
     * @param led HerkuleX.LED_RED | HerkuleX.LED_GREEN | HerkuleX.LED_BLUE
     */
    public void addAngle(int servoID, float angle, int led) {
        if (angle > 167.0 || angle < -167.0) {
            return; // out of the range	
        }
        int position = (int) (angle / 0.325) + 512;
        addMove(servoID, position, led);
    }

    /**
     * @example HerkuleX_Unison_Movement
     *
     * Add one servo infinite turn speed data
     *
     * ex) addSpeed(0, 512, HerkuleX.LED_RED); addSpeed(1, -512,
     * HerkuleX.LED_GREEN); addSpeed(2, -300, HerkuleX.LED_BLUE);
     * actionAll(1000);
     *
     * @param servoID 0 ~ 253 (0x00 ~ 0xFD)
     * @param goalSpeed -1023 ~ 1023 [CW:Negative Value(-), CCW:Positive
     * Value(+)]
     * @param led HerkuleX.LED_RED | HerkuleX.LED_GREEN | HerkuleX.LED_BLUE
     */
    public void addSpeed(int servoID, int goalSpeed, int led) {
        if (goalSpeed > 1023 || goalSpeed < -1023) {
            return;       // speed (goal) non correct
        }
        int goalSpeedSign;
        if (goalSpeed < 0) {
            goalSpeedSign = (-1) * goalSpeed;
            goalSpeedSign |= 0x4000;
        } else {
            goalSpeedSign = goalSpeed;
        }

        int speedGoalLSB = goalSpeedSign & 0X00FF; 		       // MSB speedGoal 
        int speedGoalMSB = (goalSpeedSign & 0xFF00) >> 8;      // LSB speedGoal 

        led = (byte) (led | 0x02);					// Speed Ctrl Mode

        byte[] optData = new byte[4];
        optData[0] = (byte) speedGoalLSB;
        optData[1] = (byte) speedGoalMSB;
        optData[2] = (byte) led;
        optData[3] = (byte) servoID;

        addData(optData);		//add servo data to list, speed mode
    }

    // add data to variable list servo for syncro execution
    private void addData(byte[] optData) {
        if (multipleMoveData.size() >= 4 * 53) {	// A SJOG can deal with only 53 motors at one time. 
            return;
        }

        for (int i = 0; i < optData.length; i++) {
            multipleMoveData.add(optData[i]);
        }
    }

    /**
     * @example HerkuleX_Unison_Movement
     *
     * Move(Turn) all servos with the same execution time
     *
     * ex) addMove(0, 512, HerkuleX.LED_RED); addAngle(1, 90.5f,
     * HerkuleX.LED_GREEN); addSpeed(2, -300, HerkuleX.LED_BLUE);
     * actionAll(1000);
     *
     * @param playTime 0 ~ 2865 ms
     */
    public void actionAll(int playTime) {
        if ((playTime < 0) || (playTime > 2856)) {
            return;
        }

        int optDataSize = multipleMoveData.size();
        if (optDataSize < 4) {
            return;
        }

        byte[] optData = new byte[optDataSize + 1];

        optData[0] = (byte) playTime;
        for (int i = 0; i < optDataSize; i++) {
            optData[i + 1] = multipleMoveData.get(i);
        }

        send((byte) 0xFE, HSJOG, optData);

        multipleMoveData.clear();
    }

    public void actionAll() {
        int optDataSize = multipleMoveData.size();
        byte[] optData = new byte[optDataSize];

        for (int i = 0; i < optDataSize; i++) {
            optData[i] = multipleMoveData.get(i);
        }
        send((byte) 0xFE, HIJOG, optData);
        multipleMoveData.clear();
    }

    /**
     * LED Control - GREEN, BLUE, RED
     *
     * @param servoID 0 ~ 254 (0x00 ~ 0xFE), 0xFE : BROADCAST_ID
     * @param led HerkuleX.LED_RED | HerkuleX.LED_GREEN | HerkuleX.LED_BLUE
     */
    public void setLed(int servoID, int led) {
        byte[] optData = new byte[3];
        optData[0] = 0x35;               	// Address
        optData[1] = 0x01;              	// Length

        byte led2 = 0x00;
        if ((led & LED_GREEN) == LED_GREEN) {
            led2 |= 0x01;
        }
        if ((led & LED_BLUE) == LED_BLUE) {
            led2 |= 0x02;
        }
        if ((led & LED_RED) == LED_RED) {
            led2 |= 0x04;
        }

        optData[2] = led2;
        send((byte) servoID, HRAMWRITE, optData);
    }

    /**
     * Reboot single servo
     *
     * @param servoID 0 ~ 253 (0x00 ~ 0xFD)
     */
    public void reboot(int servoID) {
        if ((byte) servoID == 0xFE) {
            return;
        }

        send((byte) servoID, HREBOOT, null);
    }

    // 
    /**
     * Servo Status
     *
     * @param servoID 0 ~ 253 (0x00 ~ 0xFD)
     * @return servo status H_STATUS_OK = 0x00; H_ERROR_INPUT_VOLTAGE = 0x01;
     * H_ERROR_POS_LIMIT	= 0x02; H_ERROR_TEMPERATURE_LIMIT	= 0x04;
     * H_ERROR_INVALID_PKT	= 0x08; H_ERROR_OVERLOAD	= 0x10; H_ERROR_DRIVER_FAULT
     * = 0x20; H_ERROR_EEPREG_DISTORT	= 0x40;
     */
    public byte stat(int servoID) {
        if ((byte) servoID == 0xFE) {
            return 0x00;
        }

        HerkulexPacket hxp = sendAndWait((byte) servoID, HSTAT, null, WAIT_TIME_BY_ACK);

        if (hxp == null) {
            return -1;
        }
        byte[] readBuf = hxp.getBytes();

        return readBuf[7];			// return status
    }

    /**
     * Model
     *
     * @param servoID
     * @return 1 = DRS-0101, 2 = DRS-0201
     */
    public byte model(int servoID) {
        byte[] optData = new byte[2];
        optData[0] = 0x00;               	// Address
        optData[1] = 0x01;              	// Length

        HerkulexPacket hxp = sendAndWait((byte) servoID, HEEPREAD, optData, WAIT_TIME_BY_ACK);

        if (hxp == null) {
            return -1;
        }
        byte[] readBuf = hxp.getBytes();
        return readBuf[8];			// return model
    }

    /**
     * @example HerkuleX_Set_ID
     *
     * setID
     *
     * CAUTION - If there are duplicated servo IDs on your Serial Line,It does
     * not work. When you change your servo's ID, make sure there is only the
     * servo on the line if possible.
     *
     * @param ID_Old
     * @param ID_New
     * @return true - success, false - failure
     *
     */
    public boolean set_ID(int ID_Old, int ID_New) {
        if (ID_Old == 0xFE || ID_New == 0xFE
                || ID_Old == ID_New) {
            return false;
        }

        if (getPosition(ID_Old) == -1) {
            return false;
        }

        byte[] optData = new byte[3];
        optData[0] = 0x06;               	// Address
        optData[1] = 0x01;              	// Length
        optData[2] = (byte) ID_New;

        send((byte) ID_Old, HEEPWRITE, optData);

        reboot(ID_Old);
        return true;
    }

    /**
     * Write registry in the RAM: one byte
     *
     * See. HerkuleX Manual.
     *
     * @param servoID
     * @param address
     * @param writeByte
     *
     */
    public void writeRegistryRAM(int servoID, int address, int writeByte) {
        byte[] optData = new byte[3];
        optData[0] = (byte) address;        // Address
        optData[1] = 0x01;              	// Length
        optData[2] = (byte) writeByte;

        send((byte) servoID, HRAMWRITE, optData);
    }

    public HerkulexPacket readRegistryRAM(int servoID, int address, int length) {
        byte[] optData = new byte[2];
        optData[0] = (byte) address;        // Address
        optData[1] = (byte) length;              	// Length

        return sendAndWait((byte) servoID, HRAMREAD, optData, WAIT_TIME_BY_ACK);
    }

    /**
     * write registry in the EEP memory (ROM): one byte
     *
     * See. HerkuleX Manual.
     *
     * CAUTION : If you are not familiar with HerekuleX servo yet, Use HerkuleX
     * Manager Instead.
     *
     * @param servoID
     * @param address
     * @param writeByte
     *
     */
    public void writeRegistryEEP(int servoID, int address, int writeByte) {
        byte[] optData = new byte[3];
        optData[0] = (byte) address;        // Address
        optData[1] = 0x01;              	// Length
        optData[2] = (byte) writeByte;

        send((byte) servoID, HEEPWRITE, optData);
    }

}
