package mg.manohisoa.databasePersistence;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import mg.manohisoa.databasePersistence.annotation.Cacheable;
import mg.manohisoa.databasePersistence.annotation.Column;
import mg.manohisoa.databasePersistence.cache.Cache;
import mg.manohisoa.databasePersistence.exception.DatabasePersistenceException;
import mg.manohisoa.databasePersistence.outil.Utilitaire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericRepo {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericRepo.class);

    /**
     * Hash map used in caching. The first Hashmap key is used to store the
     * table name And the second is used to store the request in plain string as
     * well as the request result
     */
    private static final HashMap<String, HashMap<String, Cache>> CACHE = new HashMap<>();

    /**
     * Those ignore fields is used in case we don't want a field of primitive
     * type in an object to no be included in the request to the database
     */
    private int ignoreIntMin;
    private double ignoreDoubleMin;
    private float ignoreFloatMin;
    /**
     * these are used to paginate result
     */
    private boolean paginate;
    private int pageNum;
    private int pageSize;

    /**
     * Default is null, if null then the request itself is cached, else the
     * request is identified by this identifier
     */
    private String identifierCache;

    public GenericRepo() {
        this.ignoreIntMin = 1;
        this.ignoreDoubleMin = 1;
        this.ignoreFloatMin = 1;
        this.paginate = false;
    }

    /**
     * Check if a request exist in cache
     *
     * @param hm
     * @param key
     * @return
     */
    private Boolean checkKeyCache(HashMap hm, String key) {
        return hm.containsKey(key.trim());
    }

    /**
     * Clear all elements in the cache
     *
     */
    public void clearCache() {
        CACHE.clear();
    }

    /**
     * Used after UPDATE, INSERT, ou DELETE remove the value of key: table name
     * in the cache. This removes all the requests within the given table. We
     * remove all the request in that table from the cache because we don't know
     * what line was deleted, inserted or updated
     *
     * @param key
     */
    private void removeFromCache(String tableName) {
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
    private boolean removeFromCache(String tableName, String request) {
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
    private boolean isCacheValid(Cache c) {
        return !(c.getTempexp().before(Utilitaire.getCurrentTimeStamp()) || c.getTempexp().equals(Utilitaire.getCurrentTimeStamp()));
    }

    /**
     * To get the cached value for a given request. If the cache value is not
     * valid anymore ti will be removed
     *
     * @param key
     * @param requete
     * @return
     */
    private <E> List<E> getResultFromCache(String tableName, String requete) {
        List<E> o = null;
        tableName = tableName.trim().toLowerCase();
        if (checkRequete(tableName, requete)) {
            //if the cached value is valid
            if (!removeFromCache(tableName, requete.trim())) {
                HashMap hm = CACHE.get(tableName);
                o = ((Cache) hm.get(requete.trim())).getResult();
            }
        }
        return o;
    }

    /**
     * To verify if a request is already present in cache
     *
     * @param key
     * @param requete
     * @return
     */
    private boolean checkRequete(String tableName, String requete) {
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
     * Add a request and its value in the cache with a duration limit. If the
     * table name is already present, int simply overwrite the value, else it
     * create a new key value pair for the table and the request
     *
     * @param key
     * @param requete
     * @param result
     * @param mindureecache
     */
    private <E> void addToCache(String tableName, String requete, List<E> result) {
        tableName = tableName.trim().toLowerCase();
        if (!(result == null || result.isEmpty())) {
            if (checkKeyCache(CACHE, tableName)) {
                CACHE.get(tableName).put(requete.trim(), new Cache(result, Utilitaire.addMinuteToTimestamp(Utilitaire.getCurrentTimeStamp(), Utilitaire.DEFAULT_CACHE_DURATION)));
            } else {
                HashMap<String, Cache> inst = new HashMap<>();
                inst.put(requete.trim(), new Cache(result, Utilitaire.addMinuteToTimestamp(Utilitaire.getCurrentTimeStamp(), Utilitaire.DEFAULT_CACHE_DURATION)));
                CACHE.put(tableName, inst);
            }
        }
    }

    /**
     * Select avec prise en charge de l'Héritage ,Annotation .Ne Marche pas si
     * l'instance entrée ne respecte pas les normes d'annotation configurés.Le
     * tableName est obligatoire le critère est aussi obligatoire et ne peut pas
     * être null rawSql est facultatif;
     *
     *
     * @param <E>
     * @param tableName
     * @param critere
     * @param con
     * @param rawSql
     * @param rawSqlValues
     * @return
     */
    public <E> List<E> find(String tableName, E critere, String rawSql, Connection con, Object... rawSqlValues) {
        List<E> result = null;

        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            if (critere == null) {
                throw new DatabasePersistenceException("L'objet critère est null, veuillez l'initialiser !");
            }
            Class instance = critere.getClass();
            Utilitaire.verifyTable(instance, tableName);
            Utilitaire.verifyRawSqlCount(rawSql, rawSqlValues);
            String sql = "SELECT * FROM " + tableName + " WHERE 1=1";

            List<Field> fields = Utilitaire.getAllField(instance);
            removeNullFields(fields, critere);
            sql += Utilitaire.buildRequestBasedOnField(fields);
            if (rawSql != null && !rawSql.trim().equals("")) {
                if (!rawSql.trim().toUpperCase().startsWith("AND ")) {
                    rawSql = " AND " + rawSql;
                }
                sql += rawSql;
            }

            int last = Utilitaire.setPreparedStatementValue(fields, critere, instance, ps, rawSqlValues) + 1;
            if (this.paginate) {
                if (!sql.toUpperCase().contains("ORDER")) {
                    throw new DatabasePersistenceException("La requête doit comporter une condition de tri (ORDER BY) pour une pagination correcte");
                }

                StringBuilder paginationRequest = new StringBuilder("SELECT * FROM ( SELECT a.*, ROWNUM R__ FROM (");
                paginationRequest.append(sql);
                paginationRequest.append(" ) a WHERE ROWNUM <= ? * ?)");
                paginationRequest.append(" WHERE R__ >= (? - 1) * ? + 1");
                ps = con.prepareStatement(paginationRequest.toString(), ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                ps.setInt(pageNum, last);
                ps.setInt(pageSize, last + 1);
                ps.setInt(pageNum, last + 2);
                ps.setInt(pageSize, last + 3);
            } else {
                ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            }
            String req = ps.toString();
            LOGGER.debug("SQL: {}", req);
            String identifier = req;
            if (this.identifierCache != null) {
                identifier = this.identifierCache;
            }
            Cacheable cacheable;
            cacheable = (Cacheable) instance.getAnnotation(Cacheable.class);
            if (cacheable != null) {
                result = getResultFromCache(tableName, identifier);
            }

            if (result == null) {
                rs = Utilitaire.executeStatementSelect(ps, req, tableName, instance);
                result = new ArrayList<>();
                Utilitaire.getResultAsList(rs, fields, result, instance);
                //set the response into the cache

                if (cacheable != null) {
                    if (this.identifierCache != null) {
                        identifier = this.identifierCache;
                    }
                    addToCache(tableName, identifier, result);
                }
            }
        } catch (IllegalAccessException
                | NoSuchMethodException
                | InvocationTargetException
                | SQLException | InstantiationException ex) {
            throw new DatabasePersistenceException(ex.toString());
        } finally {
            this.setPaginate(false);
            this.identifierCache = null;
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    throw new DatabasePersistenceException(ex.toString());
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    throw new DatabasePersistenceException(ex.toString());
                }
            }
        }
        return result;
    }

    /**
     * Avec prise en charge d'annotation, héritage Ne Marche pas si l'object
     * entrée ne respecte pas les normes d'annotation configurés
     *
     * @param obj
     * @param tableName
     * @param con
     */
    public void insert(Object obj, String tableName, Connection con) {
        String requete, colonne;
        Column annot;
        PreparedStatement ps = null;
        Class instance = obj.getClass();
        Method m;
        try {
            Utilitaire.verifyTable(instance, tableName);
            requete = "INSERT INTO " + tableName + "(";
            List<Field> fields = Utilitaire.getAllField(instance);
            removeNullFields(fields, obj);
            String into = " ";
            String values = " ";
            for (int i = 0; i < fields.size(); i++) {
                annot = Utilitaire.getColumnAnnotationName(fields.get(i));
                colonne = annot.name();
                if (i == 0) {
                    into += colonne;
                    values += "?";
                } else {
                    into += "," + colonne;
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
                m = instance.getMethod("get" + Utilitaire.capitalize(fields.get(i).getName()), new Class[0]);
                Utilitaire.setPreparedStatement(ps, fields.get(i).getType().getName(), nbcolonne, m.invoke(obj, new Object[0]));
                nbcolonne++;
            }
            ps.executeUpdate();

            removeFromCache(tableName);
        } catch (IllegalAccessException
                | NoSuchMethodException
                | SecurityException
                | InvocationTargetException
                | SQLException ex) {
            throw new DatabasePersistenceException(ex.toString());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    throw new DatabasePersistenceException(ex.toString());
                }
            }
        }
    }

    /**
     * update sans prendre en compte le primary key comme condition.la condition
     * doit etre faite a la main
     *
     * @param obj
     * @param tableName
     * @param afterWhere
     * @param con
     * @param afterWhereValues
     */
    public void update(Object obj, String tableName, Connection con, String afterWhere, Object... afterWhereValues) {
        PreparedStatement ps = null;
        Method m;
        Column annot;
        String colonne;
        try {
            Class instance = obj.getClass();
            Utilitaire.verifyTable(instance, tableName);
            Utilitaire.verifyRawSqlCount(afterWhere, afterWhereValues);

            String sql = "update " + tableName + " set ";
            List<Field> fields = Utilitaire.getAllField(instance);
            removeNullFields(fields, obj);

            for (int i = 0; i < fields.size(); i++) {
                annot = Utilitaire.getColumnAnnotationName(fields.get(i));
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
                m = instance.getMethod("get" + Utilitaire.capitalize(fields.get(i).getName()), new Class[0]);
                Utilitaire.setPreparedStatement(ps, fields.get(i).getType().getName(), position, m.invoke(obj, new Object[0]));
                position++;

            }
            for (Object afterWhereValue : afterWhereValues) {
                Utilitaire.setPreparedStatement(ps, afterWhereValue.getClass().getTypeName(), position, afterWhereValue);
                position++;
            }
            ps.executeUpdate();
            removeFromCache(tableName);
        } catch (IllegalAccessException
                | NoSuchMethodException
                | InvocationTargetException
                | SQLException ex) {
            throw new DatabasePersistenceException(ex.toString());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    throw new DatabasePersistenceException(ex.toString());
                }
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
     */
    public void delete(String nomtable, Connection con, String rawCondition, Object... rawConditionValues) {
        PreparedStatement ps = null;
        String sql;
        try {
            Utilitaire.verifyRawSqlCount(rawCondition, rawConditionValues);
            sql = "delete from " + nomtable + " ";
            if (rawCondition != null && !rawCondition.equals("")) {
                sql += " where " + rawCondition;
            }
            LOGGER.debug("SQL: {}", sql);
            ps = con.prepareStatement(sql);
            int position = 1;
            for (Object rawConditionValue : rawConditionValues) {
                Utilitaire.setPreparedStatement(ps, rawConditionValue.getClass().getTypeName(), position, rawConditionValue);
                position++;
            }
            ps.executeUpdate();
            removeFromCache(nomtable);
        } catch (SQLException ex) {
            throw new DatabasePersistenceException(ex.toString());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    throw new DatabasePersistenceException(ex.toString());
                }
            }
        }
    }

    /**
     * Fonction pour supprimer un element d'une table en utilisant un objet
     * comme condition
     *
     * @param obj
     * @param nomtable
     * @param con
     */
    public void delete(Object obj, String nomtable, Connection con) {
        PreparedStatement ps = null;
        String sql;
        Column annot;
        String colonne;
        Method m;
        Class instance = obj.getClass();
        try {
            Utilitaire.verifyTable(instance, nomtable);
            sql = "delete from " + nomtable + " where 4=4 ";
            List<Field> fields = Utilitaire.getAllField(instance);
            removeNullFields(fields, obj);

            for (int i = 0; i < fields.size(); i++) {
                annot = Utilitaire.getColumnAnnotationName(fields.get(i));
                colonne = annot.name();
                sql += " and " + colonne + " = ? ";

            }
            LOGGER.debug("SQL: {}", sql);
            ps = con.prepareStatement(sql);
            int position = 1;
            for (int i = 0; i < fields.size(); i++) {
                m = instance.getMethod("get" + Utilitaire.capitalize(fields.get(i).getName()), new Class[0]);
                Utilitaire.setPreparedStatement(ps, fields.get(i).getType().getName(), position, m.invoke(obj, new Object[0]));
                position++;
            }
            ps.executeUpdate();
            removeFromCache(nomtable);
        } catch (SQLException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException ex) {
            try {
                con.rollback();
            } catch (SQLException ex1) {
                throw new DatabasePersistenceException(ex.toString());
            }
            throw new DatabasePersistenceException(ex.toString());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    throw new DatabasePersistenceException(ex.toString());
                }
            }
        }
    }

    private boolean fieldValueIsNull(String fieldType, Object data) {

        switch (fieldType) {
            case "int":
                return (int) data < this.ignoreIntMin;
            case "double":
                return (double) data < this.ignoreDoubleMin;
            case "float":
                return (float) data < this.ignoreFloatMin;
            default:
                return data == null;
        }
    }

    private void removeNullFields(List<Field> fields, Object obj) {
        try {
            Class instance = obj.getClass();
            Method m;
            boolean noColumnAnnotation = true;
            List<Field> newfields = new ArrayList<>();
            Object temp;
            for (int i = 0; i < fields.size(); i++) {
                m = instance.getMethod("get" + Utilitaire.capitalize(fields.get(i).getName()), new Class[0]);
                temp = m.invoke(obj, new Object[0]);
                if (!fieldValueIsNull(fields.get(i).getType().getName(), temp) && Utilitaire.fieldHasColumnAnnotation(fields.get(i))) {
                    newfields.add(fields.get(i));
                }
                if (Utilitaire.fieldHasColumnAnnotation(fields.get(i))) {
                    noColumnAnnotation = false;
                }
            }
            if (noColumnAnnotation == true) {
                throw new DatabasePersistenceException("Aucune Annotation Column Spécifiés !");
            }
            fields.clear();
            newfields.forEach(newfield -> {
                fields.add(newfield);
            });
        } catch (NoSuchMethodException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException ex) {
            throw new DatabasePersistenceException(ex.toString());
        }
    }

    public int getIgnoreIntMin() {
        return ignoreIntMin;
    }

    public void setIgnoreIntMin(int ignoreIntMin) {
        this.ignoreIntMin = ignoreIntMin;
    }

    public double getIgnoreDoubleMin() {
        return ignoreDoubleMin;
    }

    public void setIgnoreDoubleMin(double ignoreDoubleMin) {
        this.ignoreDoubleMin = ignoreDoubleMin;
    }

    public float getIgnoreFloatMin() {
        return ignoreFloatMin;
    }

    public void setIgnoreFloatMin(float ignoreFloatMin) {
        this.ignoreFloatMin = ignoreFloatMin;
    }

    public boolean isPaginate() {
        return paginate;
    }

    public void setPaginate(boolean paginate) {
        this.paginate = paginate;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getIdentifierCache() {
        return identifierCache;
    }

    public void setIdentifierCache(String identifierCache) {
        this.identifierCache = identifierCache;
    }

}
