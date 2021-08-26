package mg.manohisoa.databasePersistence.outil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.List;

public class Utilitaire {

    public final static String REGEX_EMAIL = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
    public static final int DEFAULT_CACHE_DURATION = 30;//in minutes

    public static String getCurrentTime() {
        LocalTime lt = LocalTime.now();
        return lt.toString();
    }

    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(new java.util.Date());
        return date;
    }

    public static Date currentDate() {
        return new Date(new java.util.Date().getTime());
    }

    public static Timestamp getCurrentTimeStamp() {
        java.util.Date date = new java.util.Date();
        long time = date.getTime();
        return new Timestamp(time);
    }

    ///Date et Heure + tempsmin
    public static Timestamp getTimeStamp(Timestamp ts, int min) {
        return new Timestamp(ts.getYear(), ts.getMonth(), ts.getDate(), ts.getHours(), ts.getMinutes() + min, ts.getSeconds(), 0);
    }

    public String toUpperCase(String arg) {
        char[] name = arg.toCharArray();
        name[0] = Character.toUpperCase(name[0]);
        arg = String.valueOf(name);
        return arg;
    }

    public static String getIdFromSequence(String sequence, Connection con, int length) throws Exception {
        return formatNumber(getNextVal(sequence, con), length);
    }

    public static String getNextVal(String nomSequence, Connection c) throws Exception {
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

    public static boolean checkEmail(String email) throws Exception {
        return email.matches(REGEX_EMAIL);
    }

    public static int abs(int val) {
        if (val < 0) {
            return val * -1;
        } else {
            return val;
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
