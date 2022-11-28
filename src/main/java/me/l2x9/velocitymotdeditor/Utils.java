package me.l2x9.velocitymotdeditor;

/**
 * @author 254n_m
 * @since 7/16/22/ 12:34 AM
 * This file was created as a part of VelocityMOTDEditor
 */
public class Utils {
    public static String translateColorCodes(String textToTranslate) {//100% pasted from bukkit because this isnt in minnimessage for some reason
        char[] chars = textToTranslate.toCharArray();

        for(int i = 0; i < chars.length - 1; ++i) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(chars[i + 1]) > -1) {
                chars[i] = 167;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }
}
