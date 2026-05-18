package net.sourceforge.opencamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * LMC-style XML Configuration Manager for Open Camera.
 * Parses .xml config files and injects their settings directly into SharedPreferences.
 * This overrides standard settings to achieve specific image aesthetics (e.g. Leica, Pixel styles).
 */
public class LmcConfigManager {
    private static final String TAG = "LmcConfigManager";

    public static boolean importConfig(Context context, File xmlFile) {
        if (!xmlFile.exists() || !xmlFile.getName().endsWith(".xml")) {
            Log.e(TAG, "Config file does not exist or is not an XML file.");
            Toast.makeText(context, "Failed to load config. Invalid file.", Toast.LENGTH_SHORT).show();
            return false;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            FileInputStream fis = new FileInputStream(xmlFile);
            xpp.setInput(new InputStreamReader(fis));

            int eventType = xpp.getEventType();
            String currentTag = null;
            String key = null;
            String type = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    currentTag = xpp.getName();
                    if (currentTag.equals("int") || currentTag.equals("string") || currentTag.equals("boolean") || currentTag.equals("float")) {
                        type = currentTag;
                        key = xpp.getAttributeValue(null, "name");
                        String value = xpp.getAttributeValue(null, "value");

                        if (key != null && value != null) {
                            switch (type) {
                                case "int":
                                    editor.putInt(key, Integer.parseInt(value));
                                    break;
                                case "string":
                                    editor.putString(key, value);
                                    break;
                                case "boolean":
                                    editor.putBoolean(key, Boolean.parseBoolean(value));
                                    break;
                                case "float":
                                    editor.putFloat(key, Float.parseFloat(value));
                                    break;
                            }
                        }
                    }
                }
                eventType = xpp.next();
            }

            editor.apply();
            fis.close();
            Toast.makeText(context, "Config loaded: " + xmlFile.getName(), Toast.LENGTH_SHORT).show();
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error parsing XML config", e);
            Toast.makeText(context, "Error reading XML config.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}