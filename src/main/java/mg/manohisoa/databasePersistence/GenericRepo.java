package mg.manohisoa.databasePersistence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import mg.manohisoa.databasePersistence.annotation.Cacheable;
import mg.manohisoa.databasePersistence.annotation.Column;
import mg.manohisoa.databasePersistence.annotation.Entity;
import mg.manohisoa.databasePersistence.annotation.Table;
import mg.manohisoa.databasePersistence.cache.Cache;
import mg.manohisoa.databasePersistence.outil.Utilitaire;
import org.postgresql.util.PGInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericRepo {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericRepo.class);

    /**
     * Hash map used in caching. The first Hashmap key is used to store the
     * table name And the second is used to store the request in plain string as
     * well as the request result
     */
    public static final HashMap<String, HashMap<String, Cache>> CACHE = new HashMap<>();

    /**
     * Check if a request exist in cache
     *
     * @param hm
     * @param key
     * @return
     */
    private static Boolean checkKeyCache(HashMap hm, String key) {
        return hm.containsKey(key.trim());
    }

    /**
     * Used after UPDATE, INSERT, ou DELETE remove the value of key: table name
     * in the cache. This removes all the requests within the given table. We
     * remove all the request in tha table from the cache because we don't know
     * what line was deleted, inserted or updated
     *
     * @param key
     */
    private static void refreshCache(String tableName) {
        tableName = tableName.trim().toLowerCase();
        if (checkKeyCache(CACHE, tableName)) {
            CACHE.remove(tableName);
        }
    }

    /**
     * refresh the cache for a single request instead of all request for a given
     * table
     *
     * @param key
     * @param requete
     * @return
     */
    private static boolean refreshCache(String tableName, String request) {
        tableName = tableName.trim().toLowerCase();
        if (!isCacheValid(CACHE.get(tableName).get(request.trim()))) {
            if (CACHE.get(tableName).isEmpty()) {
                CACHE.remove(tableName);
            } else {
                CACHE.get(tableName).remove(request.trim());
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * check the cache validity for a given request
     *
     * @param c
     * @return
     */
    private static boolean isCacheValid(Cache c) {
        return !(c.getTempexp().before(Utilitaire.getCurrentTimeStamp()) || c.getTempexp().equals(Utilitaire.getCurrentTimeStamp()));
    }

    /**
     * To verify if a request is already present in cache
     *
     * @param key
     * @param requete
     * @return
     */
    private static boolean checkRequete(String tableName, String requete) {
        tableName = tableName.trim().toLowerCase();
        boolean rep = false;
        if (checkKeyCache(CACHE, tableName)) {
            HashMap hm = CACHE.get(tableName);
            if (checkKeyCache(hm, requete.trim())) {
                rep = true;
            }
        }
        return rep;
    }

    /**
     * To get the cached value for a given request. If the cache value is not
     * valid anymore ti will be removed
     *
     * @param key
     * @param requete
     * @return
     */
    private static <E> List<E> getResultFromCache(String tableName, String requete) {
        List<E> o = null;
        tableName = tableName.trim().toLowerCase();
        if (checkRequete(tableName, requete)) {
            //if the cached value is valid
            if (!refreshCache(tableName, requete.trim())) {
                HashMap hm = CACHE.get(tableName);
                o = ((Cache) hm.get(requete.trim())).getResult();
            }
        }
        return o;
    }

    /**
     * Add a request and its value in the cache with a duration limit. If the
     * table name is already present, int simply overwrite the value, else it
     * create a new key value pair for the table and the request
     *
     * @param key
     * @param requete
     * @param result
     * @param mindureecache
     * @throws Exception
     */
    private static <E> void addToCache(String tableName, String requete, List<E> result) throws Exception {
        tableName = tableName.trim().toLowerCase();
        if (!(result == null || result.isEmpty())) {
            if (checkKeyCache(CACHE, tableName)) {
                CACHE.get(tableName).put(requete.trim(), new Cache(result, Utilitaire.getTimeStamp(Utilitaire.getCurrentTimeStamp(), Utilitaire.DEFAULT_CACHE_DURATION)));
            } else {
                HashMap<String, Cache> inst = new HashMap<>();
                inst.put(requete.trim(), new Cache(result, Utilitaire.getTimeStamp(Utilitaire.getCurrentTimeStamp(), Utilitaire.DEFAULT_CACHE_DURATION)));
                CACHE.put(tableName, inst);
            }
        }
    }

    /**
     * Select avec prise en charge de l'Héritage ,Annotation .Ne Marche pas si
     * l'instance entrée ne respecte pas les normes d'annotation configurés
     *
     * @param <E>
     * @param instance
     * @param tableName
     * @param con
     * @param rawSql
     * @param rawSqlValues
     * @return
     * @throws Exception
     */
    public static <E> List<E> find(Class<E> instance, String tableName, Connection con, String rawSql, Object... rawSqlValues) throws Exception {
        List<E> result = null;

        ResultSet rs = null;
        PreparedStatement ps = null;
        try {

            verifyTable(instance);
            verifyRawSqlCount(rawSql, rawSqlValues);
            String sql = "Select * from " + tableName;
            if (rawSql != null && !rawSql.equals("")) {
                sql += " where " + rawSql;
            }
            List<Field> fields = getAllField(instance);
            removeNullFields(fields, result);

            ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String req = ps.toString();
            LOGGER.debug("SQL: {}", req);
            for (int i = 0; i < rawSqlValues.length; i++) {
                setPreparedStatement(ps, rawSqlValues[i].getClass().getTypeName(), i + 1, rawSqlValues[i]);
            }
            result = getResultFromCache(tableName, req);
            if (result == null) {

                rs = executeStatementSelect(ps, rawSql, tableName, instance);
                result = new ArrayList<>();
                getResultAsList(rs, fields, result, instance);
                //set the response into the cache
                Cacheable cachee;
                cachee = (Cacheable) instance.getAnnotation(Cacheable.class);
                if (cachee != null) {
                    addToCache(tableName, req, result);
                }
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
        }
        return result;
    }

    public static <E> List<E> find(Class<E> instance, Connection con, String rawSql, Object... rawSqlValues) throws Exception {
        try {
            String tableName = getNomTable(instance);
            return find(instance, tableName, con, rawSql, rawSqlValues);
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * Select avec prise en charge de l'Héritage,Annotation .Ne Marche pas si
     * l'objet entrée ne respecte pas les normes d'annotation configurés
     *
     * @param <E>
     * @param obj
     * @param tableName
     * @param afterAfterwhere
     * @param con
     * @return Object[]
     * @throws Exception
     */
    public static <E> List<E> find(E obj, String tableName, String afterAfterwhere, Connection con) throws Exception {
        List<E> result = null;
        Column annot;
        ResultSet rs = null;
        String colonne;
        PreparedStatement ps = null;
        Class instance = obj.getClass();
        Method m;
        try {
            verifyTable(instance);
            String sql = "Select * from " + tableName + " where 4=4 ";
            List<Field> fields = getAllField(instance);
            removeNullFields(fields, result);
            for (int i = 0; i < fields.size(); i++) {
                annot = getCulumnAnnotationName(fields.get(i));
                colonne = annot.name();
                sql += " and " + colonne + " = ? ";
            }
            if (afterAfterwhere != null) {
                sql += " " + afterAfterwhere;
            }
            ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            for (int i = 0; i < fields.size(); i++) {
                m = instance.getMethod("get" + toUpperCase(fields.get(i).getName()), new Class[0]);
                setPreparedStatement(ps, fields.get(i).getType().getName(), i + 1, m.invoke(obj, new Object[0]));

            }
            String req = ps.toString();
            LOGGER.debug("SQL: {}", req);
            result = getResultFromCache(tableName, req);
            if (result == null) {
                rs = executeStatementSelect(ps, sql, tableName, instance);
                result = new ArrayList<>();
                getResultAsList(rs, fields, result, instance);
                Cacheable cachee;
                cachee = (Cacheable) instance.getAnnotation(Cacheable.class);
                if (cachee != null) {
                    addToCache(tableName, req, result);
                }
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
        }
        return result;
    }

    public static <E> List<E> find(E obj, String afterAfterwhere, Connection con) throws Exception {
        try {
            Class instance = obj.getClass();
            String tableName = getNomTable(instance);
            return find(obj, tableName, afterAfterwhere, con);
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * Avec prise en charge d'annotation, héritage Ne Marche pas si l'object
     * entrée ne respecte pas les normes d'annotation configurés
     *
     * @param obj
     * @param tableName
     * @param con
     * @throws Exception
     */
    public static void insert(Object obj, String tableName, Connection con) throws Exception {
        String requete, colonne;
        Column annot;
        PreparedStatement ps = null;
        Class instance = obj.getClass();
        Method m;
        try {
            verifyTable(instance);
            requete = "INSERT INTO " + tableName + "(";
            List<Field> fields = getAllField(instance);
            removeNullFields(fields, obj);
            String into = " ";
            String values = " ";
            for (int i = 0; i < fields.size(); i++) {
                annot = getCulumnAnnotationName(fields.get(i));
                colonne = annot.name();
                if (i == 0) {
                    into += colonne;
                    values += "?";
                } else {
                    requete += "," + colonne;
                    values += ",?";
                }
            }
            requete += into;
            requete += ") VALUES (";
            requete += values;
            requete += ")";
            LOGGER.debug("SQL: {}", requete);
            ps = con.prepareStatement(requete);
            int nbcolonne = 1;
            for (int i = 0; i < fields.size(); i++) {
                m = instance.getMethod("get" + toUpperCase(fields.get(i).getName()), new Class[0]);
                setPreparedStatement(ps, fields.get(i).getType().getName(), nbcolonne, m.invoke(obj, new Object[0]));
                nbcolonne++;
            }
            ps.executeUpdate();

            refreshCache(tableName);
        } catch (Exception e) {
            throw e;
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    public static void insert(Object obj, Connection con) throws Exception {
        try {
            Class instance = obj.getClass();
            String tableName = getNomTable(instance);
            insert(obj, tableName, con);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * update sans prendre en compte le primary key comme condition la condition
     * doit etre faite a la main
     *
     * @param obj
     * @param tableName
     * @param afterWhere
     * @param con
     * @param afterWhereValues
     * @throws Exception
     */
    public static void update(Object obj, String tableName, Connection con, String afterWhere, Object... afterWhereValues) throws Exception {
        PreparedStatement ps = null;
        Method m;
        Column annot;
        String colonne;
        try {
            Class instance = obj.getClass();
            verifyTable(instance);
            verifyRawSqlCount(afterWhere, afterWhereValues);

            String sql = "update " + tableName + " set ";
            List<Field> fields = getAllField(instance);
            removeNullFields(fields, obj);

            for (int i = 0; i < fields.size(); i++) {
                annot = getCulumnAnnotationName(fields.get(i));
                colonne = annot.name();

                if (i == 0) {
                    sql += colonne + " = ? ";
                } else {
                    sql += "," + colonne + " = ? ";
                }

            }
            if (afterWhere != null) {
                sql += " where " + afterWhere;
            }

            LOGGER.debug("SQL: {}", sql);
            ps = con.prepareStatement(sql);
            int position = 1;
            for (int i = 0; i < fields.size(); i++) {
                m = instance.getMethod("get" + toUpperCase(fields.get(i).getName()), new Class[0]);
                setPreparedStatement(ps, fields.get(i).getType().getName(), position, m.invoke(obj, new Object[0]));
                position++;

            }
            for (Object afterWhereValue : afterWhereValues) {
                setPreparedStatement(ps, afterWhereValue.getClass().getTypeName(), position, afterWhereValue);
                position++;
            }
            ps.executeUpdate();
            refreshCache(tableName);
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    public static void update(Object obj, Connection con, String afterWhere, Object... afterWhereValues) throws Exception {
        try {
            Class instance = obj.getClass();
            String tableName = getNomTable(instance);
            update(obj, tableName, con, afterWhere, afterWhereValues);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Fonction pour effectuer un update avec comme argument le nom de table, la
     * requete des colonnes à maj et la condition
     *
     * @param nomtable
     * @param toupdate
     * @param condition
     * @param con
     * @throws Exception
     */
    public static void update(String nomtable, String toupdate, String condition, Connection con) throws Exception {
        PreparedStatement ps = null;
        try {
            if (toupdate == null || toupdate.trim().equalsIgnoreCase("")) {
                throw new Exception("Requete à mettre à jour non trouvé !");
            }
            String sql = "update " + nomtable + " set " + toupdate;
            if (condition != null && !condition.trim().equals("")) {
                sql += " where " + condition;
            }
            LOGGER.debug("SQL: {}", sql);
            ps = con.prepareStatement(sql);
            ps.executeUpdate();
            refreshCache(nomtable);
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    /**
     * Fonction pour supprimer un element d'une table
     *
     * @param nomtable
     * @param con
     * @param rawCondition
     * @param rawConditionValues
     * @throws Exception
     */
    public static void delete(String nomtable, Connection con, String rawCondition, Object... rawConditionValues) throws Exception {
        PreparedStatement ps = null;
        String sql;
        try {
            verifyRawSqlCount(rawCondition, rawConditionValues);
            sql = "delete from " + nomtable + " ";
            if (rawCondition != null && !rawCondition.equals("")) {
                sql += " where " + rawCondition;
            }
            LOGGER.debug("SQL: {}", sql);
            ps = con.prepareStatement(sql);
            int position = 1;
            for (Object rawConditionValue : rawConditionValues) {
                setPreparedStatement(ps, rawConditionValue.getClass().getTypeName(), position, rawConditionValue);
                position++;
            }
            ps.executeUpdate();
            refreshCache(nomtable);
        } catch (SQLException ex) {
            throw ex;
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    /**
     * Fonction pour supprimer un element d'une table en utilisant un objet
     * comme condition
     *
     * @param obj
     * @param con
     * @throws Exception
     */
    public static void delete(Object obj, Connection con) throws Exception {
        PreparedStatement ps = null;
        String sql;
        Column annot;
        String colonne;
        Method m;
        Class instance = obj.getClass();
        try {
            verifyTable(instance);
            String tableName = getNomTable(instance);
            sql = "delete from " + tableName + " where 4=4 ";
            List<Field> fields = getAllField(instance);
            removeNullFields(fields, obj);

            for (int i = 0; i < fields.size(); i++) {
                annot = getCulumnAnnotationName(fields.get(i));
                colonne = annot.name();
                sql += " and " + colonne + " = ? ";

            }
            LOGGER.debug("SQL: {}", sql);
            ps = con.prepareStatement(sql);
            int position = 1;
            for (int i = 0; i < fields.size(); i++) {
                m = instance.getMethod("get" + toUpperCase(fields.get(i).getName()), new Class[0]);
                setPreparedStatement(ps, fields.get(i).getType().getName(), position, m.invoke(obj, new Object[0]));
                position++;
            }
            ps.executeUpdate();
            refreshCache(tableName);
        } catch (Exception ex) {
            con.rollback();
            throw ex;
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    /*debut des fonctions helper*/
    private static int ignoreInt = -776;
    private static double ignoreDouble = -776;
    private static float ignoreFloat = -776;

    private static boolean fieldHasColumnAnnotation(Field field) {
        Column annot = (Column) field.getAnnotation(Column.class);
        return annot != null;
    }

    private static Column getCulumnAnnotationName(Field field) {
        return (Column) field.getAnnotation(Column.class);
    }

    private static void verifyRawSqlCount(String rawSql, Object... rawSqlValues) throws Exception {
        int countRawParameters = countCharacter('?', rawSql);
        if (rawSqlValues.length != countRawParameters) {
            throw new Exception("Le nombre de ? dans <rawSql> doit etre identique au nombre de parametres dans <rawSqlValue>.");
        }
    }

    private static void removeNullFields(List<Field> fields, Object obj)
            throws Exception {

        Class instance = obj.getClass();
        Method m;
        boolean noColumnAnnotation = true;
        for (int i = 0; i < fields.size(); i++) {
            m = instance.getMethod("get" + toUpperCase(fields.get(i).getName()), new Class[0]);
            if (fieldValueIsNull(fields.get(i).getType().getName(), m.invoke(obj, new Object[0])) || !fieldHasColumnAnnotation(fields.get(i))) {
                fields.remove(i);
            }
            if (fieldHasColumnAnnotation(fields.get(i))) {
                noColumnAnnotation = false;
            }
        }
        if (noColumnAnnotation == true) {
            throw new Exception("Aucune Annotation Column Spécifiés !");
        }
    }

    private static <E> void getResultAsList(ResultSet rs, List<Field> fields, List<E> o, Class instance) throws Exception {
        E objRetTemp;
        Column annot;

        String colonne;
        Method m;
        while (rs.next()) {
            objRetTemp = (E) instance.getConstructor(new Class[0]).newInstance();
            for (int i = 0; i < fields.size(); i++) {
                annot = getCulumnAnnotationName(fields.get(i));
                colonne = annot.name();
                m = instance.getMethod("set" + toUpperCase(fields.get(i).getName()), fields.get(i).getType());
                getAndSetResult(objRetTemp, rs, m, colonne, fields.get(i).getType().getName());

            }
            o.add(objRetTemp);
        }
    }

    private static int countCharacter(char c, String str) {
        char[] a = str.toCharArray();
        int count = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] == c) {
                count++;
            }
        }
        return count;
    }

    private static boolean fieldValueIsNull(String fieldType, Object data)
            throws IllegalArgumentException, IllegalAccessException {

        switch (fieldType) {
            case "int":
                return (int) data == ignoreInt;
            case "double":
                return (double) data == ignoreDouble;
            case "float":
                return (float) data == ignoreFloat;
            default:
                return data == null;
        }
    }

    /**
     * Pour Verifier si l'Annotation de entite a été bien spécifié
     *
     * @param instance
     * @throws Exception
     */
    private static void verifyTable(Class instance) throws Exception {
        try {
            if (instance.getAnnotation(Entity.class) == null) {
                throw new Exception("Aucune Annotation de Entite Spécifié !");
            }
            if (instance.getAnnotation(Table.class) == null) {
                throw new Exception("Aucune Annotation de table Spécifié !");
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Pour le 'set" des arguments dans le PreparedStatement
     *
     * @param ps
     * @param nomtypefield
     * @param nbcolonne
     * @param g
     * @throws Exception
     */
    private static void setPreparedStatement(PreparedStatement ps, String nomtypefield, int nbcolonne, Object g) throws Exception {
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
     * Pour récupérer le nom de la table Correspondant à la classe
     *
     * @param instance
     * @return
     */
    private static String getNomTable(Class instance) {
        try {
            return ((Table) instance.getAnnotation(Table.class)).name();
        } catch (Exception e) {
            throw e;
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
     * @throws Exception
     */
    private static ResultSet executeStatementSelect(PreparedStatement ps, String condition, String tableName, Class instance) throws Exception {
        try {
            return ps.executeQuery();
        } catch (SQLException e) {
            if (condition == null) {
                String error = String.format("Le nom de table '%s', spécifié dans la Classe %s n'existe pas !", tableName, instance.getName());
                throw new Exception(error);
            } else {
                String error = String.format("Veuillez vérifier la condition '%s' entrée et/ou le nom de table '%s', spécifié dans la Classe %s",
                        condition, tableName, instance.getName());
                throw new Exception(error);
            }
        }
    }

    /**
     * Pour récuperer tous les Fields de la classe , y compris ceux de sa classe
     * mère etc
     *
     * @param instance
     * @return
     * @throws Exception
     */
    private static List<Field> getAllField(Class instance) throws Exception {
        Class superClasse;
        List<Field> field = new ArrayList();
        superClasse = instance;
        int nbannot = 0;
        while (!superClasse.getName().equals("java.lang.Object")) {
            Field[] attribut = superClasse.getDeclaredFields();
            for (Field attribut1 : attribut) {
                if (attribut1.getAnnotation(Column.class) != null) {
                    //ze manana annotation collone ihany no alaina, tsy maka anle tableau ohatra
                    field.add(attribut1);
                    nbannot++;
                }
            }
            superClasse = superClasse.getSuperclass();
        }
        if (nbannot == 0) {
            throw new Exception("Aucune Annotation d'Attributs Spécifiés !");
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
     * @throws Exception
     */
    private static void getAndSetResult(Object obj, ResultSet rs, Method m, String colonne, String nomtypefield) throws Exception {
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

    /**
     * metre en majuscule la première lettre de arg
     *
     * @param arg
     * @return ToUpperCase
     */
    private static String toUpperCase(String arg) {
        char[] name = arg.toCharArray();
        name[0] = Character.toUpperCase(name[0]);
        arg = String.valueOf(name);
        return arg;
    }

    public int getIgnoreInt() {
        return ignoreInt;
    }

    public void setIgnoreInt(int aIgnoreInt) {
        ignoreInt = aIgnoreInt;
    }

    public double getIgnoreDouble() {
        return ignoreDouble;
    }

    public static void setIgnoreDouble(double aignoreDouble) {
        ignoreDouble = aignoreDouble;
    }

    public float getIgnoreFloat() {
        return ignoreFloat;
    }

    public static void setIgnoreFloat(float aignoreFloat) {
        ignoreFloat = aignoreFloat;
    }

}
