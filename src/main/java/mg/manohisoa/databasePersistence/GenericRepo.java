package mg.manohisoa.databasePersistence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import mg.manohisoa.databasePersistence.annotation.Cacheable;
import mg.manohisoa.databasePersistence.annotation.Column;
import mg.manohisoa.databasePersistence.annotation.Entity;
import mg.manohisoa.databasePersistence.annotation.Id;
import mg.manohisoa.databasePersistence.annotation.Table;
import mg.manohisoa.databasePersistence.annotation.Tableau;
import mg.manohisoa.databasePersistence.cache.Cache;
import org.postgresql.util.PGInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class GenericRepo {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericRepo.class);

    /**
     * Nom de table Clé ,HashMap* valeur valeur : HashMap<String,Cache> ; String
     * requete, Cache (DateTime Création, Object[] result)
     */
    public static final HashMap<String, HashMap<String, Cache>> CACHE = new HashMap<>();

    /**
     * Chercher si le nom de table existe dans le HM key = nom table
     *
     * @param hm
     * @param key
     * @return
     */
    private static Boolean checkKeyCache(HashMap hm, String key) {
	return hm.containsKey(key.trim());
    }

    /**
     * Apres UPDATE, INSERT, ou DELETE Si la clé existe dans le HM , supprimer
     * l'Element<K,V> correspondant
     *
     * @param key
     */
    private static void refreshCache(String key) {
	key = key.trim().toLowerCase();
	if (checkKeyCache(CACHE, key)) {
	    CACHE.remove(key);
	}
    }

    /**
     * Si la clé existe dans le HM , La requete existe dans la hashMap et que le
     * cache correspondand n'est plus valide, efface la cache Assurer que la key
     * et la requete existe dans le Hmap avant d'utiliser cette fonction
     *
     * @param key
     * @param requete
     * @return
     */
    private static boolean refreshCache(String key, String requete) {
	key = key.trim().toLowerCase();
	if (!checkDateCache(CACHE.get(key).get(requete.trim()))) {
	    CACHE.get(key).remove(requete.trim());
	    if (CACHE.get(key).isEmpty()) {
		CACHE.remove(key);
	    }
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * checkValidité Cache,
     *
     * @param c
     * @return
     */
    private static boolean checkDateCache(Cache c) {
	boolean b = true;
	if (c.getTempexp().before(getCurrentTimeStamp()) || c.getTempexp().equals(getCurrentTimeStamp())) {
	    ///expiré
	    b = false;
	}
	return b;
    }

    /**
     * Pour verifier si la requete existe déja dans le cache
     *
     * @param key
     * @param requete
     * @return
     */
    private static boolean checkRequete(String key, String requete) {
	key = key.trim().toLowerCase();
	boolean rep = false;
	if (checkKeyCache(CACHE, key)) {
	    ///true
	    HashMap hm = CACHE.get(key);
	    if (checkKeyCache(hm, requete.trim())) {
		rep = true;
	    }
	}
	return rep;
    }

    /**
     * Pour recupérer les resultats depuis le cache
     *
     * @param key
     * @param requete
     * @return
     */
    private static <E> List<E> getResultFromCache(String key, String requete) {
	List<E> o = null;
	key = key.trim().toLowerCase();
	if (checkRequete(key, requete)) {
	    boolean b = refreshCache(key, requete.trim());
	    if (!b) {
		HashMap hm = CACHE.get(key);
		o = ((Cache) hm.get(requete.trim())).getResult();
	    }
	}
	return o;
    }

    /**
     * Ajouter les données de la fonction SELECT dans le cache si ils n'y sont
     * pas
     *
     * @param key
     * @param requete
     * @param result
     * @param mindureecache
     * @throws Exception
     */
    private static <E> void addToCache(String key, String requete, List<E> result, int mindureecache) throws Exception {
	key = key.trim().toLowerCase();
	if (!(result == null || result.isEmpty())) {
	    if (checkKeyCache(CACHE, key)) {
		///true
		CACHE.get(key).put(requete.trim(), new Cache(result, getTimeStamp(getCurrentTimeStamp(), mindureecache)));
	    } else {
		///false
		HashMap<String, Cache> inst = new HashMap<>();
		inst.put(requete.trim(), new Cache(result, getTimeStamp(getCurrentTimeStamp(), mindureecache)));
		CACHE.put(key, inst);
	    }
	}
    }

    /**
     * Date et Heure Actuel
     *
     * @return
     */
    private static Timestamp getCurrentTimeStamp() {
	java.util.Date date = new java.util.Date();
	long time = date.getTime();
	return new Timestamp(time);
    }

    /**
     * Date et Heure + tempsmin
     *
     * @param ts
     * @param min
     * @return
     */
    private static Timestamp getTimeStamp(Timestamp ts, int min) {
	return new Timestamp(ts.getYear(), ts.getMonth(), ts.getDate(), ts.getHours(), ts.getMinutes() + min, ts.getSeconds(), 0);
    }

    /**
     * Connection base Oracle
     *
     * @param dbname
     * @param username
     * @param password
     * @param port
     * @param host
     * @return
     * @throws Exception
     */
    private static Connection getOracleConnection(String dbname, String username, String password, int port, String host) throws Exception {
	Class.forName("oracle.jdbc.driver.OracleDriver");
	String DBurl = "jdbc:oracle:thin:@" + host + ":" + port + "/" + dbname;
	Connection con = null;
	con = DriverManager.getConnection(DBurl, username, password);
	setNLS_DATE_FORMAT(con);
	return con;
    }

    /**
     * Pour la connection Oracle, pour ne pas avoir de problème avec la
     * manipulation des dates
     *
     * @param c
     * @throws Exception
     */
    private static void setNLS_DATE_FORMAT(Connection c) throws Exception {
	String requete = "ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD'";
	PreparedStatement stat = c.prepareStatement(requete);
	stat.executeUpdate();
	c.commit();
	stat.close();
    }

    /**
     * Connection base Postgresql
     *
     * @param dbname
     * @param username
     * @param password
     * @param port
     * @param host
     * @return
     * @throws Exception
     */
    private static Connection getPostgresqlConnection(String dbname, String username, String password, int port, String host) throws Exception {
	Class.forName("org.postgresql.Driver");
	String DBurl = "jdbc:postgresql://" + host + ":" + port + "/" + dbname;
	Connection con = DriverManager.getConnection(DBurl, username, password);
	con.setAutoCommit(false);
	return con;
    }

    /**
     * connection base mysql
     *
     * @param dbname
     * @param username
     * @param password
     * @param port
     * @param host
     * @return
     * @throws Exception
     */
    private static Connection getMysqlConnection(String dbname, String username, String password, int port, String host) throws Exception {
	Class.forName("com.mysql.jdbc.Driver");
	String DBurl = "jdbc:mysql://" + host + ":" + port + "/" + dbname;
	Connection con = DriverManager.getConnection(DBurl, username, password);
	con.setAutoCommit(false);
	return con;
    }

    /**
     *
     * @param xmlUrl
     * @return Pour récupérer l'instance d'une connexion bdd
     * @throws Exception
     */
    public static Connection getConnection(String xmlUrl) throws Exception {
	String dbname, username, password, dbtype, host, port;
	Connection c = null;
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	DocumentBuilder db;
	Document doc;
	db = dbf.newDocumentBuilder();
	try {
	    doc = db.parse(xmlUrl);
	    dbname = doc.getElementsByTagName("database").item(0).getFirstChild().getNodeValue();
	    username = doc.getElementsByTagName("user").item(0).getFirstChild().getNodeValue();
	    password = doc.getElementsByTagName("password").item(0).getFirstChild().getNodeValue();
	    dbtype = doc.getElementsByTagName("sgbd").item(0).getFirstChild().getNodeValue();
	    host = doc.getElementsByTagName("host").item(0).getFirstChild().getNodeValue();
	    port = doc.getElementsByTagName("port").item(0).getFirstChild().getNodeValue();

	    if (dbtype.equalsIgnoreCase("oracle") || dbtype.equalsIgnoreCase("orcl")) {
		c = getOracleConnection(dbname, username, password, Integer.parseInt(port), host);
	    } else if (dbtype.equalsIgnoreCase("postgresql") || dbtype.equalsIgnoreCase("postgres")
		    || dbtype.equalsIgnoreCase("pgsql") || dbtype.equalsIgnoreCase("postgre")) {
		c = getPostgresqlConnection(dbname, username, password, Integer.parseInt(port), host);
	    } else if (dbtype.equalsIgnoreCase("mysql")) {
		c = getMysqlConnection(dbname, username, password, Integer.parseInt(port), host);
	    }
	} catch (Exception ex) {
	    throw new Exception("connection failed: Veuillez vérifier le type de base de données que vous avez entré !");
	}

	return c;
    }

    /**
     *
     * @param dbname
     * @param username
     * @param password
     * @param dbtype
     * @param port
     * @param host
     * @return Pour récupérer l'instance d'une connexion bdd
     * @throws Exception
     */
    public static Connection getConnection(String dbname, String username, String password, String dbtype, int port, String host) throws Exception {
	Connection c = null;
	try {
	    if (dbtype.equalsIgnoreCase("oracle") || dbtype.equalsIgnoreCase("orcl")) {
		c = getOracleConnection(dbname, username, password, port, host);
	    } else if (dbtype.equalsIgnoreCase("postgresql") || dbtype.equalsIgnoreCase("postgres")
		    || dbtype.equalsIgnoreCase("pgsql") || dbtype.equalsIgnoreCase("postgre")) {
		c = getPostgresqlConnection(dbname, username, password, port, host);
	    } else if (dbtype.equalsIgnoreCase("mysql")) {
		c = getMysqlConnection(dbname, username, password, port, host);
	    }
	} catch (Exception e) {
	    throw new Exception("connection failed: Veuillez vérifier le type de base de données que vous avez entré !");
	}
	return c;
    }

    /**
     * Select avec prise en charge de l'Héritage ,Annotation . Ne Marche pas si
     * l'instance entrée ne respecte pas les normes d'annotation configurés
     *
     * @param <E>
     * @param instance
     * @param condition
     * @param c
     * @return
     * @throws Exception
     */
    public static <E> List<E> find(Class<E> instance, String condition, Connection c) throws Exception {
	List<E> o = null;

	Column annot;
	ResultSet rs = null;
	String colonne;
	PreparedStatement ps = null;
	try {
	    verifyTable(instance);
	    String tableName = getNomTable(instance);
	    String sql = "Select * from " + tableName;
	    if (condition != null && condition.equals("") == false) {
		sql += " where " + condition;
	    }
	    ps = c.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
	    String req = ps.toString();
	    LOGGER.info("SQL: {}", sql);

	    o = getResultFromCache(tableName, req);
	    if (o == null) {

		rs = executeStatementSelect(ps, condition, tableName, instance);
		List<Field> field = getAllField(instance, rs.getMetaData().getColumnCount(), tableName);
		E obj;
		Method m;

		o = new ArrayList<>();
		while (rs.next()) {
		    obj = (E) Class.forName(instance.getName()).newInstance();
		    for (int i = 0; i < field.size(); i++) {
			annot = (Column) field.get(i).getAnnotation(Column.class);
			if (annot != null) {
			    colonne = annot.name();
			    m = instance.getMethod("set" + toUpperCase(field.get(i).getName()), field.get(i).getType());
			    getAndSetResult(obj, rs, m, colonne, field.get(i).getType().getName());
			}
		    }
		    o.add(obj);
		}
		//set the response into the cache
		Cacheable cachee;
		cachee = (Cacheable) instance.getAnnotation(Cacheable.class);
		if (cachee != null) {
		    int mindureecache = (cachee).dureeenminute();
		    addToCache(tableName, req, o, mindureecache);
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
	return o;
    }

    public static <E> List<E> find(Class<E> instance, String tableName, String condition, Connection c) throws Exception {
	List<E> o = null;

	Column annot;
	ResultSet rs = null;
	String colonne;
	PreparedStatement ps = null;
	try {
	    verifyTable(instance);
	    String sql = "Select * from " + tableName;
	    if (condition != null && condition.equals("") == false) {
		sql += " where " + condition;
	    }
	    ps = c.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
	    String req = ps.toString();
	    o = getResultFromCache(tableName, req);
	    if (o == null) {

		rs = executeStatementSelect(ps, condition, tableName, instance);
		List<Field> field = getAllField(instance, rs.getMetaData().getColumnCount(), tableName);
		E obj;
		Method m;

		o = new ArrayList<>();
		while (rs.next()) {
		    obj = (E) Class.forName(instance.getName()).newInstance();
		    for (int i = 0; i < field.size(); i++) {
			annot = (Column) field.get(i).getAnnotation(Column.class);
			if (annot != null) {
			    colonne = annot.name();
			    m = instance.getMethod("set" + toUpperCase(field.get(i).getName()), field.get(i).getType());
			    getAndSetResult(obj, rs, m, colonne, field.get(i).getType().getName());
			}
		    }
		    o.add(obj);
		}
		//set the response into the cache
		Cacheable cachee;
		cachee = (Cacheable) instance.getAnnotation(Cacheable.class);
		if (cachee != null) {
		    int mindureecache = (cachee).dureeenminute();
		    addToCache(tableName, req, o, mindureecache);
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
	return o;
    }

    /**
     * Select avec prise en charge de l'Héritage,Annotation . Ne Marche pas si
     * l'objet entrée ne respecte pas les normes d'annotation configurés
     *
     * @param <E>
     * @param objet
     * @param afterAfterwhere
     * @param intNull
     * @param c
     * @return Object[]
     * @throws Exception
     */
    public static <E> List<E> find(E objet, Integer intNull, String afterAfterwhere, Connection c) throws Exception {
	List<E> o = null;
	Column annot;
	ResultSet rs = null;
	String colonne = "";
	PreparedStatement ps = null;
	Class instance = objet.getClass();
	E objRetTemp;
	Method m;
	try {
	    verifyTable(instance);
	    String tableName = getNomTable(instance);
	    String sql = "Select * from " + tableName + " where 4=4 ";
	    List<Field> field = getAllField(instance);
	    List<Object> condition = new ArrayList<>();
	    List<Integer> indfield = new ArrayList();
	    for (int i = 0; i < field.size(); i++) {
		annot = (Column) field.get(i).getAnnotation(Column.class);
		if (annot != null) {
		    colonne = annot.name();
		    try {
			m = instance.getMethod("get" + toUpperCase(field.get(i).getName()), new Class[0]);
		    } catch (Exception exx) {
			m = instance.getMethod("is" + toUpperCase(field.get(i).getName()), new Class[0]);
		    }
		    Object obj = m.invoke(objet, new Object[0]);
		    if (obj != null) {
			if (!obj.getClass().getTypeName().equalsIgnoreCase("java.lang.Integer")
				&& !obj.getClass().getTypeName().equalsIgnoreCase("int")) {
			    condition.add(obj);
			    indfield.add(i);
			    sql += " and " + colonne + " = ? ";
			} else {
			    //miditra ny zero
			    if (intNull == null || (intNull != null && (int) obj != intNull)) {
				condition.add(obj);
				indfield.add(i);
				sql += " and " + colonne + " = ? ";
			    }
			}

		    }
		}
	    }
	    if (afterAfterwhere != null) {
		sql += " " + afterAfterwhere;
	    }
	    ps = c.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

	    for (int i = 0; i < condition.size(); i++) {
		setPreparedStatement(ps, field.get(indfield.get(i)).getType().getName(), i + 1, condition.get(i));
	    }
	    String req = ps.toString();
	    o = getResultFromCache(tableName, req);
	    if (o == null) {
		rs = executeStatementSelect(ps, "", tableName, instance);

		o = new ArrayList<>();
		while (rs.next()) {
		    objRetTemp = (E) Class.forName(instance.getName()).newInstance();
		    for (int i = 0; i < field.size(); i++) {
			annot = (Column) field.get(i).getAnnotation(Column.class);
			if (annot != null) {
			    colonne = annot.name();
			    m = instance.getMethod("set" + toUpperCase(field.get(i).getName()), field.get(i).getType());
			    getAndSetResult(objRetTemp, rs, m, colonne, field.get(i).getType().getName());
			}
		    }
		    o.add(objRetTemp);
		}

		Cacheable cachee;
		cachee = (Cacheable) instance.getAnnotation(Cacheable.class);
		if (cachee != null) {
		    int mindureecache = (cachee).dureeenminute();
		    addToCache(tableName, req, o, mindureecache);
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
	return o;
    }

    /**
     *
     * @param <E>
     * @param o
     * @param afterWhere
     * @param valeurs
     * @param autresApsWhere
     * @param con
     * @return
     * @throws SQLException
     * @throws Exception
     */
    public static <E> List<E> find(E o, String[] afterWhere, Object[] valeurs, String autresApsWhere, Connection con)
	    throws SQLException, Exception {
	List<E> resultFinal = null;
	ResultSet result = null;
	PreparedStatement req = null;
	Column annot;
	String colonne = "";
	Method m;
	try {
	    Class classeO = o.getClass();
	    verifyTable(classeO);

	    String nomTable = getNomTable(classeO);
	    Field[] f = new Field[0];
	    f = getAllField(classeO).toArray(f);

	    String request = "";
	    if (afterWhere == null || valeurs == null) {
		request += "select * from " + nomTable;
	    } else {
		request = buildSql(nomTable, afterWhere);
	    }

	    if (autresApsWhere != null) {
		request += " " + autresApsWhere;
	    }

	    req = con.prepareStatement(request, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

	    if (valeurs != null) {
		for (int i = 0; i < valeurs.length; i++) {
		    setPreparedStatement(req, valeurs[i].getClass().getTypeName(), i + 1, valeurs[i]);
		}
	    }
	    String requette = req.toString();
	    resultFinal = getResultFromCache(nomTable, requette);
	    if (resultFinal == null) {
		result = req.executeQuery();

		resultFinal = new ArrayList<>();

		//int b = 1;
		while (result.next()) {
		    E temp = (E) classeO.getDeclaredConstructor().newInstance();
		    for (Field f1 : f) {
			annot = (Column) f1.getAnnotation(Column.class);
			if (annot != null) {
			    colonne = annot.name();
			    m = classeO.getMethod("set" + toUpperCase(f1.getName()), f1.getType());
			    getAndSetResult(temp, result, m, colonne, f1.getType().getName());
			}
		    }
		    resultFinal.add(temp);
		}
		Cacheable cachee;
		cachee = (Cacheable) classeO.getAnnotation(Cacheable.class);
		if (cachee != null) {
		    int mindureecache = (cachee).dureeenminute();
		    addToCache(nomTable, requette, resultFinal, mindureecache);
		}
	    }

	} catch (Exception e) {
	    throw e;
	} finally {
	    if (result != null) {
		result.close();
	    }
	    if (req != null) {
		req.close();
	    }
	}
	return resultFinal;
    }

    public static void insert(Object o, String tableName, Connection c) throws Exception {
	String requete, colonne;
	Column annot;
	PreparedStatement ps = null;
	Class instance = o.getClass();
	Method m;
	Object g;
	try {
	    verifyTable(instance);

	    requete = "INSERT INTO " + tableName + "(";
	    Class superClasse;
	    List<Field> field = new ArrayList();
	    superClasse = instance;
	    boolean b = false;
	    int nbcolonne = 0;
	    while (!superClasse.getName().equals("java.lang.Object")) {
		Field[] attribut = superClasse.getDeclaredFields();
		for (int i = 0; i < attribut.length; i++) {
		    annot = (Column) attribut[i].getAnnotation(Column.class);
		    if (annot != null) {
			field.add(attribut[i]);
			colonne = annot.name();
			nbcolonne++;
			if (!b) {
			    requete += colonne;
			    b = true;
			} else {
			    requete += "," + colonne;
			}
		    }
		}
		superClasse = superClasse.getSuperclass();
	    }
	    if (b == false) {
		throw new Exception("Aucune Annotation d'Attributs Spécifiés !");
	    }
	    requete += ") VALUES (";
	    for (int i = 0; i < nbcolonne; i++) {
		if (i == 0) {
		    requete += "?";
		} else {
		    requete += ",?";
		}
	    }
	    requete += ")";
	    ps = c.prepareStatement(requete);
	    nbcolonne = 1;
	    for (int i = 0; i < field.size(); i++) {
		annot = (Column) field.get(i).getAnnotation(Column.class);
		if (annot != null) {
		    try {
			m = instance.getMethod("get" + toUpperCase(field.get(i).getName()), new Class[0]);
		    } catch (NoSuchMethodException | SecurityException e) {
			///Cas spécifique pour certains getteur de type boolean
			m = instance.getMethod("is" + toUpperCase(field.get(i).getName()), new Class[0]);
		    }
		    g = m.invoke(o, new Object[0]);
		    setPreparedStatement(ps, field.get(i).getType().getName(), nbcolonne, g);
		    nbcolonne++;
		}
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

    /**
     * Avec prise en charge d'annotation, héritage Ne Marche pas si l'object
     * entrée ne respecte pas les normes d'annotation configurés
     *
     * @param o
     * @param c
     * @throws Exception
     */
    public static void insert(Object o, Connection c) throws Exception {
	String requete, colonne;
	Column annot;
	PreparedStatement ps = null;
	Class instance = o.getClass();
	Method m;
	Object g;
	try {
	    verifyTable(instance);
	    String tableName = getNomTable(instance);
	    requete = "INSERT INTO " + tableName + "(";
	    Class superClasse;
	    List<Field> field = new ArrayList();
	    superClasse = instance;
	    boolean b = false;
	    int nbcolonne = 0;
	    while (!superClasse.getName().equals("java.lang.Object")) {
		Field[] attribut = superClasse.getDeclaredFields();
		for (int i = 0; i < attribut.length; i++) {
		    annot = (Column) attribut[i].getAnnotation(Column.class);
		    if (annot != null) {
			field.add(attribut[i]);
			colonne = annot.name();
			nbcolonne++;
			if (!b) {
			    requete += colonne;
			    b = true;
			} else {
			    requete += "," + colonne;
			}
		    }
		}
		superClasse = superClasse.getSuperclass();
	    }
	    if (b == false) {
		throw new Exception("Aucune Annotation d'Attributs Spécifiés !");
	    }
	    requete += ") VALUES (";
	    for (int i = 0; i < nbcolonne; i++) {
		if (i == 0) {
		    requete += "?";
		} else {
		    requete += ",?";
		}
	    }
	    requete += ")";
	    ps = c.prepareStatement(requete);
	    nbcolonne = 1;
	    for (int i = 0; i < field.size(); i++) {
		annot = (Column) field.get(i).getAnnotation(Column.class);
		if (annot != null) {
		    try {
			m = instance.getMethod("get" + toUpperCase(field.get(i).getName()), new Class[0]);
		    } catch (NoSuchMethodException | SecurityException e) {
			///Cas spécifique pour certains getteur de type boolean
			m = instance.getMethod("is" + toUpperCase(field.get(i).getName()), new Class[0]);
		    }
		    g = m.invoke(o, new Object[0]);
		    setPreparedStatement(ps, field.get(i).getType().getName(), nbcolonne, g);
		    nbcolonne++;
		}
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

    /**
     * Avec prise en charge d'annotation, héritage, insertion d'Attribut tableau
     * Ne Marche pas si l'object entrée ne respecte pas les normes d'annotation
     * configurés Cette fonction insert toutes les attributs avec l'annotations
     * tableau qu'il trouve, sans limites
     *
     * @param o
     * @param c
     * @throws Exception
     */
    public static void insertWithArrayAttributes(Object o, Connection c) throws Exception {
	String requete, colonne;
	Column annot;
	Tableau tablannot;
	PreparedStatement ps = null;
	Class instance = o.getClass();
	Method m;
	Object g;
	try {
	    verifyTable(instance);
	    String tableName = getNomTable(instance);
	    requete = "INSERT INTO " + tableName + "(";
	    Class superClasse;
	    List<Field> field = new ArrayList();
	    List<Field> fieldTableau = new ArrayList();
	    superClasse = instance;
	    boolean b = false;
	    int nbcolonne = 0;
	    while (!superClasse.getName().equals("java.lang.Object")) {
		Field[] attribut = superClasse.getDeclaredFields();
		for (int i = 0; i < attribut.length; i++) {
		    annot = (Column) attribut[i].getAnnotation(Column.class);
		    if (annot != null) {
			field.add(attribut[i]);
			colonne = annot.name();
			nbcolonne++;
			if (!b) {
			    requete += colonne;
			    b = true;
			} else {
			    requete += "," + colonne;
			}
		    } else {
			tablannot = (Tableau) attribut[i].getAnnotation(Tableau.class);
			if (tablannot != null) {
			    fieldTableau.add(attribut[i]);
			}
		    }
		}
		superClasse = superClasse.getSuperclass();
	    }
	    if (b == false) {
		throw new Exception("Aucune Annotation d'Attributs Spécifiés !");
	    }
	    requete += ") VALUES (";
	    for (int i = 0; i < nbcolonne; i++) {
		if (i == 0) {
		    requete += "?";
		} else {
		    requete += ",?";
		}
	    }
	    requete += ")";
	    ps = c.prepareStatement(requete);
	    nbcolonne = 1;
	    //memetraka anle value ao anaty prepared statement ho an tsy tableau
	    for (int i = 0; i < field.size(); i++) {
		try {
		    m = instance.getMethod("get" + toUpperCase(field.get(i).getName()), new Class[0]);
		} catch (NoSuchMethodException | SecurityException e) {
		    ///Cas spécifique pour certains getteur de type boolean
		    m = instance.getMethod("is" + toUpperCase(field.get(i).getName()), new Class[0]);
		}
		g = m.invoke(o, new Object[0]);
		setPreparedStatement(ps, field.get(i).getType().getName(), nbcolonne, g);
		nbcolonne++;
	    }
	    //mi insert anle tableau
	    if (fieldTableau.size() > 0) {
		for (int i = 0; i < fieldTableau.size(); i++) {
		    m = instance.getMethod("get" + toUpperCase(fieldTableau.get(i).getName()), new Class[0]);
		    Object[] objTemp = (Object[]) m.invoke(o, new Object[0]);
		    if (objTemp.length > 0) {
			for (Object objTemp1 : objTemp) {
			    insert(objTemp1, c);
			}
		    }
		}
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

    /**
     * Fonction pour effectuer un update avec comme argument un objet; La
     * Fonction n'updatera que les attributs de l'objets non null ; Seul les
     * Attribut PK sont mis dans la condition update Nb: Bien Considérer les
     * attributs de types int car les int sont tjrs initialisé 0 si null => un
     * int ne peut etre null ! mettez le en -776
     *
     * @param obj
     * @param con
     * @throws Exception
     */
    public static void updateById(Object obj, Integer intNull, Connection con) throws Exception {
	PreparedStatement prs = null;
	Method m;
	Column annot;
	String colonne;
	Object objet;
	Id pk;
	try {
	    Class instance = obj.getClass();
	    verifyTable(instance);
	    String tableName = getNomTable(instance);
	    String sql = "update " + tableName + " set ";
	    List<Field> field = getAllField(instance);
	    List<Object> condition = new ArrayList<>();
	    List<Integer> indfield = new ArrayList();
	    List<Object> where = new ArrayList<>();
	    List<Integer> indfieldwhere = new ArrayList();
	    String wherereq = " ";
	    for (int i = 0; i < field.size(); i++) {
		annot = (Column) field.get(i).getAnnotation(Column.class);
		if (annot != null) {
		    colonne = annot.name();
		    try {
			m = instance.getMethod("get" + toUpperCase(field.get(i).getName()), new Class[0]);
		    } catch (Exception exx) {
			m = instance.getMethod("is" + toUpperCase(field.get(i).getName()), new Class[0]);
		    }
		    objet = m.invoke(obj, new Object[0]);
		    if (objet != null) {
			pk = (Id) field.get(i).getAnnotation(Id.class);
			if (!objet.getClass().getTypeName().equalsIgnoreCase("java.lang.Integer")
				&& !objet.getClass().getTypeName().equalsIgnoreCase("int")) {
			    if (pk != null) {
				if (where.isEmpty()) {
				    wherereq += " where " + colonne + " = ? ";
				} else {
				    wherereq += " and " + colonne + " = ? ";
				}
				where.add(objet);
				indfieldwhere.add(i);
			    } else {
				if (condition.isEmpty()) {
				    sql += " " + colonne + " = ? ";
				} else {
				    sql += " ," + colonne + " = ? ";
				}
				condition.add(objet);
				indfield.add(i);
			    }
			} else {
			    if (intNull == null || (intNull != null && (int) objet != intNull)) {
				if (pk != null) {
				    if (where.isEmpty()) {
					wherereq += " where " + colonne + " = ? ";
				    } else {
					wherereq += " and " + colonne + " = ? ";
				    }
				    where.add(objet);
				    indfieldwhere.add(i);
				} else {
				    if (condition.isEmpty()) {
					sql += " " + colonne + " = ? ";
				    } else {
					sql += " ," + colonne + " = ? ";
				    }
				    condition.add(objet);
				    indfield.add(i);
				}
			    }
			}
		    }
		}
	    }
	    sql += wherereq;
	    prs = con.prepareStatement(sql);
	    int mo = 0;
	    for (int i = 0; i < condition.size(); i++) {
		setPreparedStatement(prs, field.get(indfield.get(i)).getType().getName(), i + 1, condition.get(i));
		mo = i + 2;
	    }
	    for (int i = 0; i < where.size(); i++) {
		setPreparedStatement(prs, field.get(indfieldwhere.get(i)).getType().getName(), mo, where.get(i));
		mo++;
	    }

	    prs.executeUpdate();
	    refreshCache(tableName);
	} catch (Exception ex) {
	    throw ex;
	} finally {
	    if (prs != null) {
		prs.close();
	    }
	}
    }

    /**
     * update san prendre en compte le primary key comme condition la condition
     * doit etre faite a la main
     *
     * @param obj
     * @param afterWhere
     * @param intNull
     * @param con
     * @throws Exception
     */
    public static void update(Object obj, String afterWhere, Integer intNull, Connection con) throws Exception {
	PreparedStatement prs = null;
	Method m;
	Column annot;
	String colonne;
	Object objet;
	Id pk;
	try {
	    Class instance = obj.getClass();
	    verifyTable(instance);
	    String tableName = getNomTable(instance);
	    String sql = "update " + tableName + " set ";
	    List<Field> field = getAllField(instance);
	    List<Object> condition = new ArrayList<>();
	    List<Integer> indfield = new ArrayList();

	    for (int i = 0; i < field.size(); i++) {
		annot = (Column) field.get(i).getAnnotation(Column.class);
		if (annot != null) {
		    colonne = annot.name();
		    try {
			m = instance.getMethod("get" + toUpperCase(field.get(i).getName()), new Class[0]);
		    } catch (Exception exx) {
			m = instance.getMethod("is" + toUpperCase(field.get(i).getName()), new Class[0]);
		    }
		    objet = m.invoke(obj, new Object[0]);
		    if (objet != null) {
			if (!objet.getClass().getTypeName().equalsIgnoreCase("java.lang.Integer")
				&& !objet.getClass().getTypeName().equalsIgnoreCase("int")) {

			    if (condition.isEmpty()) {
				sql += " " + colonne + " = ? ";
			    } else {
				sql += " ," + colonne + " = ? ";
			    }
			    condition.add(objet);
			    indfield.add(i);
			} else {
			    if (intNull == null || (intNull != null && (int) objet != intNull)) {
				if (condition.isEmpty()) {
				    sql += " " + colonne + " = ? ";
				} else {
				    sql += " ," + colonne + " = ? ";
				}
				condition.add(objet);
				indfield.add(i);
			    }
			}
		    }
		}
	    }
	    sql += " where " + afterWhere;
	    prs = con.prepareStatement(sql);
	    int mo = 0;
	    for (int i = 0; i < condition.size(); i++) {
		setPreparedStatement(prs, field.get(indfield.get(i)).getType().getName(), i + 1, condition.get(i));
		mo = i + 2;
	    }

	    prs.executeUpdate();
	    refreshCache(tableName);
	} catch (Exception ex) {
	    throw ex;
	} finally {
	    if (prs != null) {
		prs.close();
	    }
	}
    }

    /**
     * Fonction pour effectuer un update avec comme argument le nom de table, le
     * noms des attributs à mettre à jour et les valeurs correspondantes
     *
     * @param nomtable
     * @param columns
     * @param values
     * @param condition
     * @param con
     * @throws Exception
     */
    public static void update(String nomtable, String[] columns, Object[] values, String condition, Connection con) throws Exception {
	PreparedStatement prs = null;
	try {
	    String sql = "update " + nomtable + " set ";
	    Boolean first = true;
	    for (String column : columns) {
		if (!first) {
		    sql += ",";
		}
		sql += column + "=?";
		first = false;
	    }
	    if (condition != null && !condition.trim().equals("")) {
		sql += " where " + condition;
	    }
	    prs = con.prepareStatement(sql);
	    for (int i = 0; i < values.length; i++) {
		setPreparedStatement(prs, values[i].getClass().getTypeName(), 1 + i, values[i]);
	    }
	    prs.executeUpdate();
	    refreshCache(nomtable);
	} catch (Exception ex) {
	    throw ex;
	} finally {
	    if (prs != null) {
		prs.close();
	    }
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
	PreparedStatement prs = null;
	try {
	    if (toupdate == null || toupdate.trim().equalsIgnoreCase("")) {
		throw new Exception("Requete à mettre à jour non trouvé !");
	    }
	    String sql = "update " + nomtable + " set " + toupdate;
	    if (condition != null && !condition.trim().equals("")) {
		sql += " where " + condition;
	    }
	    prs = con.prepareStatement(sql);
	    prs.executeUpdate();
	    refreshCache(nomtable);
	} catch (Exception ex) {
	    throw ex;
	} finally {
	    if (prs != null) {
		prs.close();
	    }
	}
    }

    /**
     * Fonction pour supprimer un element d'une table
     *
     * @param nomtable
     * @param condition
     * @param con
     * @throws Exception
     */
    public static void delete(String nomtable, String condition, Connection con) throws Exception {
	PreparedStatement prs = null;
	String sql;
	try {
	    sql = "delete from " + nomtable + " ";
	    if (!(condition == null || condition.trim().equalsIgnoreCase(""))) {
		sql += " where " + condition;
	    }
	    prs = con.prepareStatement(sql);
	    prs.executeUpdate();
	    refreshCache(nomtable);
	} catch (SQLException ex) {
	    throw ex;
	} finally {
	    if (prs != null) {
		prs.close();
	    }
	}
    }

    /**
     * Fonction pour supprimer un element d'une table en utilisant un objet
     * comme condition
     *
     * @param objet
     * @param con
     * @throws Exception
     */
    public static void delete(Object objet, Connection con) throws Exception {
	PreparedStatement prs = null;
	String sql;
	Column annot;
	String colonne;
	Class instance = objet.getClass();
	Object obj;
	Method m;
	try {
	    verifyTable(instance);
	    String tableName = getNomTable(instance);
	    sql = "delete from " + tableName + " where 4=4 ";
	    List<Field> field = getAllField(instance);
	    List<Object> condition = new ArrayList<>();
	    List<Integer> indfield = new ArrayList();
	    for (int i = 0; i < field.size(); i++) {
		annot = (Column) field.get(i).getAnnotation(Column.class);
		if (annot != null) {
		    colonne = annot.name();
		    try {
			m = instance.getMethod("get" + toUpperCase(field.get(i).getName()), new Class[0]);
		    } catch (Exception exx) {
			m = instance.getMethod("is" + toUpperCase(field.get(i).getName()), new Class[0]);
		    }
		    obj = m.invoke(objet, new Object[0]);
		    if (obj != null) {
			condition.add(obj);
			indfield.add(i);
			sql += " and " + colonne + " = ? ";
		    }
		}
	    }
	    prs = con.prepareStatement(sql);
	    for (int i = 0; i < condition.size(); i++) {
		setPreparedStatement(prs, field.get(indfield.get(i)).getType().getName(), i + 1, condition.get(i));
	    }
	    prs.executeUpdate();
	    refreshCache(tableName);
	} catch (Exception ex) {
	    con.rollback();
	    throw ex;
	} finally {
	    if (prs != null) {
		prs.close();
	    }
	}
    }

    /*debut des fonctions helper*/
    private static String buildSql(String nomTable, String[] afterWhere) {
	String request = "";
	request += "select * from " + nomTable + " where ";
	for (int i = 0; i < afterWhere.length; i++) {
	    if (i != afterWhere.length - 1) {
		request += afterWhere[i] + "=? and ";
	    } else {
		request += afterWhere[i] + "=?";
	    }
	}
	return request;
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
	} catch (Exception e) {
	    if (condition == null) {
		throw new Exception("Le nom de table '" + tableName + "', spécifié dans la Classe " + instance.getName() + " n'existe pas !");
	    } else {
		throw new Exception("Veuillez vérifier la condition entrée et/ou le nom de table '" + tableName + "', spécifié dans la Classe " + instance.getName());
	    }
	}
    }

    /**
     * Pour récuperer tous les Fields de la classe , y compris ceux de sa classe
     * mère etc -Methode 1
     *
     * @param instance
     * @param columncount
     * @param tablename
     * @return
     * @throws Exception
     */
    private static List<Field> getAllField(Class instance, int columncount, String tablename) throws Exception {
	Class superClasse;
	List<Field> field = new ArrayList();
	superClasse = instance;
	int nbannot = 0;
	while (!superClasse.getName().equals("java.lang.Object")) {
	    Field[] attribut = superClasse.getDeclaredFields();
	    for (int i = 0; i < attribut.length; i++) {
		if (attribut[i].getAnnotation(Column.class) != null) {
		    //ze manana annotation collone ihany no alaina, tsy maka anle tableau ohatra
		    field.add(attribut[i]);
		    nbannot++;
		}
	    }
	    superClasse = superClasse.getSuperclass();
	}
	if (nbannot == 0) {
	    throw new Exception("Aucune Annotation d'Colonne Spécifiés !");
	} else if (columncount != nbannot) {
	    throw new Exception("Le Nombre d'Annotation d'Colonne trouvé en partant de la Classe " + instance.getName() + " et le Nombre de Colonne dans la Table " + tablename + " ne correspondent pas !");
	}
	return field;
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

}
