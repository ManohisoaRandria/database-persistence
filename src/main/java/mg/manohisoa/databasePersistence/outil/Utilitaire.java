package mg.manohisoa.databasePersistence.outil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalTime;

public class Utilitaire {

    public final static String REGEX_EMAIL = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";

    ///Heure Actuel
    public static String getCurrentTime() {
        LocalTime lt = LocalTime.now();
        return lt.toString();
    }

    ///Date Actuel
    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(new java.util.Date());
        return date;
    }

    ///Date et Heure Actuel
    public static Timestamp getCurrentTimeStamp() {
        java.util.Date date = new java.util.Date();
        long time = date.getTime();
        return new Timestamp(time);
    }

    ///Date et Heure + tempsmin
    public static Timestamp getTimeStamp(Timestamp ts, int min) {
        return new Timestamp(ts.getYear(), ts.getMonth(), ts.getDate(), ts.getHours(), ts.getMinutes() + min, ts.getSeconds(), 0);
    }

    ///ToUpperCase
    public String toUpperCase(String arg) {
        char[] name = arg.toCharArray();
        name[0] = Character.toUpperCase(name[0]);
        arg = String.valueOf(name);
        return arg;
    }

    /// Fonction pour avoir la séquence
    public static String getsequenceOracle(String nomSequence, Connection c) throws Exception {
        String seq = null;
        String requete = " SELECT " + nomSequence + ".nextval as nb from Dual";
        ResultSet rs2;
        try (Statement st2 = c.createStatement()) {
            rs2 = st2.executeQuery(requete);
            while (rs2.next()) {
                seq = rs2.getString("nb");
                break;
            }
        }
        rs2.close();
        return seq;
    }

    public static String getsequencePg(String nomSequence, Connection c) throws Exception {
        String seq = null;
        String requete = " SELECT nextval('" + nomSequence + "') as nb";
        ResultSet rs2;
        try (Statement st2 = c.createStatement()) {
            rs2 = st2.executeQuery(requete);
            while (rs2.next()) {
                seq = rs2.getString("nb");
                break;
            }
        }
        rs2.close();
        return seq;
    }

    public static String formatNumber(String seq, int ordre) throws Exception {
        if (seq.split("").length > ordre) {
            throw new Exception("Format impossible !");
        }
        String ret = "";
        for (int i = 0; i < ordre - seq.split("").length; i++) {
            ret += "0";
        }
        return ret + seq;
    }

    public static String getSecurePassword(String passwordToHash) {
        String generatedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(passwordToHash.getBytes());
            byte[] bytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return generatedPassword;
    }

    public static boolean checkEmail(String email) throws Exception {
        boolean isValid = false;
        isValid = email.matches(REGEX_EMAIL);

        return isValid;
    }

//    public static int countMarqueVehicule(String idmarque, Connection c) throws Exception {
//        int seq = 0;
//        String requete = "select count(*) as nb from " + Constantes.TABLE_VEHICULE + " where idmarquevehicule = '" + idmarque.trim() + "' ";
//        ResultSet rs2;
//        try (Statement st2 = c.createStatement()) {
//            rs2 = st2.executeQuery(requete);
//            while (rs2.next()) {
//                seq = rs2.getInt("nb");
//                break;
//            }
//        }
//        rs2.close();
//        return seq;
//    }
    public static Time getTime(Time ts, int heure) {
        ts.setHours(ts.getHours() + heure);
        return ts;
    }

    public static int getDiffTime(Time t1, Time t2) {    ///en heure
        int t11 = (t1.getSeconds()) + (t1.getMinutes() * 60) + (t1.getHours() * 60 * 60);
        int t12 = (t2.getSeconds()) + (t2.getMinutes() * 60) + (t2.getHours() * 60 * 60);
        return abs(((t11 - t12) / 60) / 60);
    }

    public static int getDiffDate(Date t1, Date t2) {    ///en heure
        int t11 = (t1.getDate()) + ((t1.getMonth() + 1) * 30) + (t1.getYear() * 12 * 30);
        int t12 = (t2.getDate()) + ((t2.getMonth() + 1) * 30) + (t2.getYear() * 12 * 30);
        return abs(t11 - t12);
    }

    public static Date getDate(Date ts, int day) {
        ts.setDate(ts.getDate() + day);
        return ts;
    }

    public static int abs(int val) {
        if (val < 0) {
            return val * -1;
        } else {
            return val;
        }
    }

}
