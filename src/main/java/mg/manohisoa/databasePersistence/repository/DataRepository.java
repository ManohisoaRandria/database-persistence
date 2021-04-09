/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mg.manohisoa.databasePersistence.repository;

import java.sql.Connection;
import java.util.List;
import org.springframework.stereotype.Indexed;

/**
 *
 * @author P11A-MANOHISOA
 * @param <T>
 */
@Indexed
public interface DataRepository<T extends Object> {

    List<T> findAll(Class<T> cl, Connection con) throws Exception;

    List<T> findAll(Class<T> cl, String nomTable, Connection con) throws Exception;

    void save(T entite, Connection con) throws Exception;

    void save(T entite, String nomTable, Connection con) throws Exception;
}
