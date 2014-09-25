/**
 *
 * @author Ori Novanda (cargmax.at.gmail.com)
 * 
 */

package herkulex;

public class HerkulexDataAddress {
    //E=EEPROM
    public final static byte E_MIN_VOLTAGE = 12;
    public final static byte E_MAX_VOLTAGE = 13;
    public final static byte R_MIN_VOLTAGE = 6;

    //R=RAM
    public final static byte R_MAX_VOLTAGE = 7;
    public final static byte R_MIN_POSITION = 20; // 16bit
    public final static byte R_MAX_POSITION = 22; // 16bit
    public final static byte R_VOLTAGE = 54;
    public final static byte R_TEMPERATURE = 55;

    public final static byte R_CURRENT_POSITION = 60;
    public final static byte R_GOAL_POSITION = 68;
}
