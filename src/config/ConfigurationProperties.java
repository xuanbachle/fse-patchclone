package config;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by xuanbach on 12/19/15.
 */
public class ConfigurationProperties {

    public static Properties properties;
    protected static Logger log = Logger.getLogger(ConfigurationProperties.class);

    public static void load(String configFile) {
        InputStream propFile;
        try {
            properties = new Properties();
            propFile = new FileInputStream(configFile);

            properties.load(propFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static boolean hasProperty(String key) {
        if(properties.getProperty(key) == null) {
            return false;
        }
        return true;
    }

    public static String getProperty(String key){
        return properties.getProperty(key);
    }

    public static Integer getPropertyInt(String key){
        return Integer.valueOf(properties.getProperty(key));
    }

    public static Boolean getPropertyBool(String key){
        return Boolean.valueOf(properties.getProperty(key));
    }

    public static Double getPropertyDouble(String key){
        return Double.valueOf(properties.getProperty(key));
    }

    public static void print(){
        log.info("----------------------------");
        log.info("---Configuration properties:---Execution values");
        for(String key :properties.stringPropertyNames()){
            log.info("p:"+key+"= "+properties.getProperty(key));
        }
        log.info("----------------------------");
    }

}
