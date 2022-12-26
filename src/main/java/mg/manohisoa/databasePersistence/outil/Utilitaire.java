package mg.manohisoa.databasePersistence.outil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import mg.manohisoa.databasePersistence.annotation.Column;
import mg.manohisoa.databasePersistence.annotation.Entity;
import mg.manohisoa.databasePersistence.exception.DatabasePersistenceException;
import mg.manohisoa.databasePersistence.exception.SqlAndReflectException;
import org.postgresql.util.PGInterval;

public class Utilitaire {

    public final static String REGEX_EMAIL = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
    public static final int DEFAULT_CACHE_DURATION = 1440;//in minutes

    public static Date getCurrentDate() {
        return new Date(new java.util.Date().getTime());
    }

    public static java.util.Date getCurrentDateUtil() {
        return new java.util.Date();
    }

    public static Timestamp getCurrentTimeStamp() {
        java.util.Date date = new java.util.Date();
        long time = date.getTime();
        return new Timestamp(time);
    }

    ///Date et Heure + tempsmin
    public static Timestamp addMinuteToTimestamp(Timestamp ts, int min) {
        LocalDateTime time = ts.toLocalDateTime();
        LocalDateTime timePlus = time.plusMinutes(min);
        return Timestamp.valueOf(timePlus);
    }

    public static String capitalize(String arg) {
        char[] name = arg.toCharArray();
        name[0] = Character.toUpperCase(name[0]);
        arg = String.valueOf(name);
        return arg;
    }

    public static String getNextVal(String nomSequence, Connection c) {
        String seq = null;
        String requete = " SELECT " + nomSequence + ".nextval as nb from Dual";

        try ( Statement st2 = c.createStatement();  ResultSet rs2 = st2.executeQuery(requete)) {
            while (rs2.next()) {
                seq = rs2.getString("nb");
                break;
            }
        } catch (SQLException ex) {
            throw new SqlAndReflectException(ex.toString());
        }
        return seq;
    }

    public boolean tableHasColumn(String column, ResultSetMetaData meta) throws SQLException {
        boolean result = false;
        int columnCount = meta.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            if (meta.getColumnLabel(i + 1).equals(column)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public static String formatNumber(String seqValue, int ordre) {
        if (seqValue.split("").length > ordre) {
            throw new DatabasePersistenceException("Formatage de séquence impossible !");
        }
        String ret = "";
        for (int i = 0; i < ordre - seqValue.split("").length; i++) {
            ret += "0";
        }
        return ret + seqValue;
    }

    public static String getIdFromSequence(String sequence, Connection con, int length) {
        return formatNumber(getNextVal(sequence, con), length);
    }

    public static String getSecurePassword(String passwordToHash) throws NoSuchAlgorithmException {
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
            throw e;
        }
        return generatedPassword;
    }

    public static String buildInQuery(String[] ids) {
        String ret = " in (";
        for (int i = 0; i < ids.length; i++) {
            if (i != ids.length - 1) {
                ret += "'" + ids[i] + "',";
            } else {
                ret += "'" + ids[i] + "'";
            }
        }
        ret += ")";
        return ret;
    }

    public static String buildInQuery(List<String> ids) {
        String ret = " in (";
        for (int i = 0; i < ids.size(); i++) {
            if (i != ids.size() - 1) {
                ret += "'" + ids.get(i) + "',";
            } else {
                ret += "'" + ids.get(i) + "'";
            }
        }
        ret += ")";
        return ret;
    }

    public static boolean checkEmail(String email) {
        return email.matches(REGEX_EMAIL);
    }

    public static int abs(int val) {
        if (val < 0) {
            return val * -1;
        } else {
            return val;
        }
    }

    public static int countAll(String requette, Connection c) {
        int seq = 0;
        String requete = " SELECT count(*) as nb from (" + requette + ")";

        try ( Statement st2 = c.createStatement();  ResultSet rs2 = st2.executeQuery(requete)) {
            while (rs2.next()) {
                seq = rs2.getInt("nb");
                break;
            }
        } catch (SQLException ex) {
            throw new SqlAndReflectException(ex.toString());
        }
        return seq;
    }

    public static <E> int setPreparedStatementValue(List<Field> fields, E critere, Class instance, PreparedStatement ps, Object... rawSqlValues)
            throws NoSuchMethodException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            SQLException {
        Method m;
        int last = 1;
        for (int i = 0; i < fields.size(); i++) {
            m = instance.getMethod("get" + Utilitaire.capitalize(fields.get(i).getName()), new Class[0]);
            setPreparedStatement(ps, fields.get(i).getType().getName(), i + 1, m.invoke(critere, new Object[0]));
            last++;
        }
        for (int i = 0; i < rawSqlValues.length; i++) {
            setPreparedStatement(ps, rawSqlValues[i].getClass().getTypeName(), fields.size() + i + 1, rawSqlValues[i]);
            last++;
        }
        return last;
    }

    public static String buildRequestBasedOnField(List<Field> fields) {
        Column annot;
        String colonne;
        String sql = "";
        for (int i = 0; i < fields.size(); i++) {
            annot = getColumnAnnotationName(fields.get(i));
            colonne = annot.name();
            sql += " and " + colonne + " = ? ";
        }
        return sql;
    }

    public static boolean fieldHasColumnAnnotation(Field field) {
        Column annot = (Column) field.getAnnotation(Column.class);
        return annot != null;
    }

    public static Column getColumnAnnotationName(Field field) {
        return (Column) field.getAnnotation(Column.class);
    }

    public static void verifyRawSqlCount(String rawSql, Object... rawSqlValues) {
        if (rawSql != null) {
            int countRawParameters = countCharacter('?', rawSql);
            if (rawSqlValues.length != countRawParameters) {
                throw new DatabasePersistenceException("Le nombre de ? dans <rawSql> doit etre identique au nombre de parametres dans <rawSqlValue>.");
            }
        }

    }

    public static <E> void getResultAsList(ResultSet rs, List<Field> fields, List<E> o, Class instance) throws SQLException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        E objRetTemp;
        Column annot;

        String colonne;
        Method m;
        while (rs.next()) {
            objRetTemp = (E) instance.getConstructor(new Class[0]).newInstance();
            for (int i = 0; i < fields.size(); i++) {
                annot = getColumnAnnotationName(fields.get(i));
                colonne = annot.name();
                m = instance.getMethod("set" + Utilitaire.capitalize(fields.get(i).getName()), fields.get(i).getType());
                getAndSetResult(objRetTemp, rs, m, colonne, fields.get(i).getType().getName());

            }
            o.add(objRetTemp);
        }
    }

    public static int countCharacter(char c, String str) {
        char[] a = str.toCharArray();
        int count = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] == c) {
                count++;
            }
        }
        return count;
    }

    /**
     * Pour Verifier si l'Annotation de entite a été bien spécifié
     *
     * @param instance
     * @param nomTable
     */
    public static void verifyTable(Class instance, String nomTable) {

        if (instance.getAnnotation(Entity.class) == null) {
            throw new DatabasePersistenceException("Aucune Annotation de Entite Spécifié !");
        }
        if (nomTable == null) {
            throw new DatabasePersistenceException("Aucune table Spécifié !");
        }

    }

    /**
     * Pour le 'set" des arguments dans le PreparedStatement
     *
     * @param ps
     * @param nomtypefield
     * @param nbcolonne
     * @param g
     * @throws java.sql.SQLException
     */
    public static void setPreparedStatement(PreparedStatement ps, String nomtypefield, int nbcolonne, Object g) throws SQLException {
        switch (nomtypefield) {
            case "java.lang.Double":
            case "double":
                ps.setDouble(nbcolonne, (Double) g);
                break;
            case "boolean":
                ps.setBoolean(nbcolonne, (boolean) g);
                break;
            case "int":
            case "java.lang.Integer":
                ps.setInt(nbcolonne, (int) g);
                break;
            case "org.postgresql.util.PGInterval":
                ps.setObject(nbcolonne, (PGInterval) g);
                break;
            case "java.lang.String":
                ps.setString(nbcolonne, (String) g);
                break;
            case "java.sql.Date":
            case "java.util.Date":
                if (g == null) {
                    ps.setDate(nbcolonne, null);
                } else {
                    ps.setDate(nbcolonne, Date.valueOf(g.toString()));
                }
                break;
            case "float":
                ps.setFloat(nbcolonne, (float) g);
                break;
            case "java.sql.Timestamp":
                ps.setTimestamp(nbcolonne, Timestamp.valueOf(g.toString()));
                break;
            case "java.sql.Time":
                ps.setTime(nbcolonne, Time.valueOf(g.toString()));
                break;
            default:
                ps.setObject(nbcolonne, g);
                break;
        }
    }

    /**
     * Pour Executer la requête dans le Statement
     *
     * @param ps
     * @param condition
     * @param tableName
     * @param instance
     * @return
     * @throws java.sql.SQLException
     */
    public static ResultSet executeStatementSelect(PreparedStatement ps, String condition, String tableName, Class instance) throws SQLException {
        try {
            return ps.executeQuery();
        } catch (SQLException e) {
//            mbola jerena hoe ahoana no hi specifiena anle exception, avoka any ftsn aloha atreto
            if (condition == null) {
                String error = String.format("Le nom de table '%s', spécifié dans la Classe %s n'existe pas !", tableName, instance.getName());
//                throw new Exception(error);
            } else {
                String error = String.format("Veuillez vérifier la condition '%s' entrée et/ou le nom de table '%s', spécifié dans la Classe %s",
                        condition, tableName, instance.getName());
//                throw new Exception(error);
            }
            throw e;
        }
    }

    /**
     * Pour récuperer tous les Fields de la classe , y compris ceux de sa classe
     * mère etc
     *
     * @param instance
     * @return
     */
    public static List<Field> getAllField(Class instance) {
        Class superClasse;
        List<Field> field = new ArrayList();
        superClasse = instance;
        int nbannot = 0;
        while (!superClasse.getName().equals("java.lang.Object")) {
            Field[] attribut = superClasse.getDeclaredFields();
            for (Field attribut1 : attribut) {
                if (attribut1.getAnnotation(Column.class) != null) {
                    //ze manana annotation colone ihany no alaina, tsy maka anle tableau ohatra
                    field.add(attribut1);
                    nbannot++;
                }
            }
            superClasse = superClasse.getSuperclass();
        }
        if (nbannot == 0) {
            throw new DatabasePersistenceException("Aucune Annotation d'Attributs Spécifiés !");
        }
        return field;
    }

    /**
     * Pour recuperer et Ajouter dans l'Objet obj le resultat obtenu
     *
     * @param obj
     * @param rs
     * @param m
     * @param colonne
     * @param nomtypefield
     * @throws java.sql.SQLException
     * @throws java.lang.IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public static void getAndSetResult(Object obj, ResultSet rs, Method m, String colonne, String nomtypefield) throws SQLException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {
        switch (nomtypefield) {
            case "java.lang.String":
                m.invoke(obj, rs.getString(colonne));
                break;
            case "java.lang.Double":
            case "double":
                m.invoke(obj, rs.getDouble(colonne));
                break;
            case "int":
            case "java.lang.Integer":
                m.invoke(obj, rs.getInt(colonne));
                break;
            case "org.postgresql.util.PGInterval":
                m.invoke(obj, (PGInterval) rs.getObject(colonne));
                break;
            case "java.sql.Date":
            case "java.util.Date":
                m.invoke(obj, rs.getDate(colonne));
                break;
            case "boolean":
                m.invoke(obj, rs.getBoolean(colonne));
                break;
            case "float":
                m.invoke(obj, rs.getFloat(colonne));
                break;
            case "java.sql.Timestamp":
                m.invoke(obj, rs.getTimestamp(colonne));
                break;
            case "java.sql.Time":
                m.invoke(obj, rs.getTime(colonne));
                break;
            default:
                m.invoke(obj, rs.getObject(colonne));
                break;
        }
    }

    //mime types
//    [
//    'html' => ['text/html', '*/*'],
//    'json' => 'application/json',
//    'xml' => ['application/xml', 'text/xml'],
//    'rss' => 'application/rss+xml',
//    'ai' => 'application/postscript',
//    'bcpio' => 'application/x-bcpio',
//    'bin' => 'application/octet-stream',
//    'ccad' => 'application/clariscad',
//    'cdf' => 'application/x-netcdf',
//    'class' => 'application/octet-stream',
//    'cpio' => 'application/x-cpio',
//    'cpt' => 'application/mac-compactpro',
//    'csh' => 'application/x-csh',
//    'csv' => ['text/csv', 'application/vnd.ms-excel'],
//    'dcr' => 'application/x-director',
//    'dir' => 'application/x-director',
//    'dms' => 'application/octet-stream',
//    'doc' => 'application/msword',
//    'docx' => 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
//    'drw' => 'application/drafting',
//    'dvi' => 'application/x-dvi',
//    'dwg' => 'application/acad',
//    'dxf' => 'application/dxf',
//    'dxr' => 'application/x-director',
//    'eot' => 'application/vnd.ms-fontobject',
//    'eps' => 'application/postscript',
//    'exe' => 'application/octet-stream',
//    'ez' => 'application/andrew-inset',
//    'flv' => 'video/x-flv',
//    'gtar' => 'application/x-gtar',
//    'gz' => 'application/x-gzip',
//    'bz2' => 'application/x-bzip',
//    '7z' => 'application/x-7z-compressed',
//    'hdf' => 'application/x-hdf',
//    'hqx' => 'application/mac-binhex40',
//    'ico' => 'image/x-icon',
//    'ips' => 'application/x-ipscript',
//    'ipx' => 'application/x-ipix',
//    'js' => 'application/javascript',
//    'jsonapi' => 'application/vnd.api+json',
//    'latex' => 'application/x-latex',
//    'lha' => 'application/octet-stream',
//    'lsp' => 'application/x-lisp',
//    'lzh' => 'application/octet-stream',
//    'man' => 'application/x-troff-man',
//    'me' => 'application/x-troff-me',
//    'mif' => 'application/vnd.mif',
//    'ms' => 'application/x-troff-ms',
//    'nc' => 'application/x-netcdf',
//    'oda' => 'application/oda',
//    'otf' => 'font/otf',
//    'pdf' => 'application/pdf',
//    'pgn' => 'application/x-chess-pgn',
//    'pot' => 'application/vnd.ms-powerpoint',
//    'pps' => 'application/vnd.ms-powerpoint',
//    'ppt' => 'application/vnd.ms-powerpoint',
//    'pptx' => 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
//    'ppz' => 'application/vnd.ms-powerpoint',
//    'pre' => 'application/x-freelance',
//    'prt' => 'application/pro_eng',
//    'ps' => 'application/postscript',
//    'roff' => 'application/x-troff',
//    'scm' => 'application/x-lotusscreencam',
//    'set' => 'application/set',
//    'sh' => 'application/x-sh',
//    'shar' => 'application/x-shar',
//    'sit' => 'application/x-stuffit',
//    'skd' => 'application/x-koan',
//    'skm' => 'application/x-koan',
//    'skp' => 'application/x-koan',
//    'skt' => 'application/x-koan',
//    'smi' => 'application/smil',
//    'smil' => 'application/smil',
//    'sol' => 'application/solids',
//    'spl' => 'application/x-futuresplash',
//    'src' => 'application/x-wais-source',
//    'step' => 'application/STEP',
//    'stl' => 'application/SLA',
//    'stp' => 'application/STEP',
//    'sv4cpio' => 'application/x-sv4cpio',
//    'sv4crc' => 'application/x-sv4crc',
//    'svg' => 'image/svg+xml',
//    'svgz' => 'image/svg+xml',
//    'swf' => 'application/x-shockwave-flash',
//    't' => 'application/x-troff',
//    'tar' => 'application/x-tar',
//    'tcl' => 'application/x-tcl',
//    'tex' => 'application/x-tex',
//    'texi' => 'application/x-texinfo',
//    'texinfo' => 'application/x-texinfo',
//    'tr' => 'application/x-troff',
//    'tsp' => 'application/dsptype',
//    'ttc' => 'font/ttf',
//    'ttf' => 'font/ttf',
//    'unv' => 'application/i-deas',
//    'ustar' => 'application/x-ustar',
//    'vcd' => 'application/x-cdlink',
//    'vda' => 'application/vda',
//    'xlc' => 'application/vnd.ms-excel',
//    'xll' => 'application/vnd.ms-excel',
//    'xlm' => 'application/vnd.ms-excel',
//    'xls' => 'application/vnd.ms-excel',
//    'xlsx' => 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
//    'xlw' => 'application/vnd.ms-excel',
//    'zip' => 'application/zip',
//    'aif' => 'audio/x-aiff',
//    'aifc' => 'audio/x-aiff',
//    'aiff' => 'audio/x-aiff',
//    'au' => 'audio/basic',
//    'kar' => 'audio/midi',
//    'mid' => 'audio/midi',
//    'midi' => 'audio/midi',
//    'mp2' => 'audio/mpeg',
//    'mp3' => 'audio/mpeg',
//    'mpga' => 'audio/mpeg',
//    'ogg' => 'audio/ogg',
//    'oga' => 'audio/ogg',
//    'spx' => 'audio/ogg',
//    'ra' => 'audio/x-realaudio',
//    'ram' => 'audio/x-pn-realaudio',
//    'rm' => 'audio/x-pn-realaudio',
//    'rpm' => 'audio/x-pn-realaudio-plugin',
//    'snd' => 'audio/basic',
//    'tsi' => 'audio/TSP-audio',
//    'wav' => 'audio/x-wav',
//    'aac' => 'audio/aac',
//    'asc' => 'text/plain',
//    'c' => 'text/plain',
//    'cc' => 'text/plain',
//    'css' => 'text/css',
//    'etx' => 'text/x-setext',
//    'f' => 'text/plain',
//    'f90' => 'text/plain',
//    'h' => 'text/plain',
//    'hh' => 'text/plain',
//    'htm' => ['text/html', '*/*'],
//    'ics' => 'text/calendar',
//    'm' => 'text/plain',
//    'rtf' => 'text/rtf',
//    'rtx' => 'text/richtext',
//    'sgm' => 'text/sgml',
//    'sgml' => 'text/sgml',
//    'tsv' => 'text/tab-separated-values',
//    'tpl' => 'text/template',
//    'txt' => 'text/plain',
//    'text' => 'text/plain',
//    'avi' => 'video/x-msvideo',
//    'fli' => 'video/x-fli',
//    'mov' => 'video/quicktime',
//    'movie' => 'video/x-sgi-movie',
//    'mpe' => 'video/mpeg',
//    'mpeg' => 'video/mpeg',
//    'mpg' => 'video/mpeg',
//    'qt' => 'video/quicktime',
//    'viv' => 'video/vnd.vivo',
//    'vivo' => 'video/vnd.vivo',
//    'ogv' => 'video/ogg',
//    'webm' => 'video/webm',
//    'mp4' => 'video/mp4',
//    'm4v' => 'video/mp4',
//    'f4v' => 'video/mp4',
//    'f4p' => 'video/mp4',
//    'm4a' => 'audio/mp4',
//    'f4a' => 'audio/mp4',
//    'f4b' => 'audio/mp4',
//    'gif' => 'image/gif',
//    'ief' => 'image/ief',
//    'jpg' => 'image/jpeg',
//    'jpeg' => 'image/jpeg',
//    'jpe' => 'image/jpeg',
//    'pbm' => 'image/x-portable-bitmap',
//    'pgm' => 'image/x-portable-graymap',
//    'png' => 'image/png',
//    'pnm' => 'image/x-portable-anymap',
//    'ppm' => 'image/x-portable-pixmap',
//    'ras' => 'image/cmu-raster',
//    'rgb' => 'image/x-rgb',
//    'tif' => 'image/tiff',
//    'tiff' => 'image/tiff',
//    'xbm' => 'image/x-xbitmap',
//    'xpm' => 'image/x-xpixmap',
//    'xwd' => 'image/x-xwindowdump',
//    'ice' => 'x-conference/x-cooltalk',
//    'iges' => 'model/iges',
//    'igs' => 'model/iges',
//    'mesh' => 'model/mesh',
//    'msh' => 'model/mesh',
//    'silo' => 'model/mesh',
//    'vrml' => 'model/vrml',
//    'wrl' => 'model/vrml',
//    'mime' => 'www/mime',
//    'pdb' => 'chemical/x-pdb',
//    'xyz' => 'chemical/x-pdb',
//    'javascript' => 'application/javascript',
//    'form' => 'application/x-www-form-urlencoded',
//    'file' => 'multipart/form-data',
//    'xhtml' => ['application/xhtml+xml', 'application/xhtml', 'text/xhtml'],
//    'xhtml-mobile' => 'application/vnd.wap.xhtml+xml',
//    'atom' => 'application/atom+xml',
//    'amf' => 'application/x-amf',
//    'wap' => ['text/vnd.wap.wml', 'text/vnd.wap.wmlscript', 'image/vnd.wap.wbmp'],
//    'wml' => 'text/vnd.wap.wml',
//    'wmlscript' => 'text/vnd.wap.wmlscript',
//    'wbmp' => 'image/vnd.wap.wbmp',
//    'woff' => 'application/x-font-woff',
//    'webp' => 'image/webp',
//    'appcache' => 'text/cache-manifest',
//    'manifest' => 'text/cache-manifest',
//    'htc' => 'text/x-component',
//    'rdf' => 'application/xml',
//    'crx' => 'application/x-chrome-extension',
//    'oex' => 'application/x-opera-extension',
//    'xpi' => 'application/x-xpinstall',
//    'safariextz' => 'application/octet-stream',
//    'webapp' => 'application/x-web-app-manifest+json',
//    'vcf' => 'text/x-vcard',
//    'vtt' => 'text/vtt',
//    'mkv' => 'video/x-matroska',
//    'pkpass' => 'application/vnd.apple.pkpass',
//    'ajax' => 'text/html'
//]
}
