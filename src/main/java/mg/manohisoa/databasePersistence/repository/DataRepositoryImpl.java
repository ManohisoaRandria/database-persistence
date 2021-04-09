/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mg.manohisoa.databasePersistence.repository;

import java.sql.Connection;
import java.util.List;
import mg.manohisoa.databasePersistence.GenericRepo;

public class DataRepositoryImpl<T> implements DataRepository<T> {

    public DataRepositoryImpl() {
    }

    @Override
    public List<T> findAll(Class<T> clazz, Connection con) throws Exception {
        return GenericRepo.find(clazz, null, con);
    }

    @Override
    public List<T> findAll(Class<T> clazz, String nomTable, Connection con) throws Exception {
        return GenericRepo.find(clazz, nomTable, null, con);
    }

    @Override
    public void save(T entite, Connection con) throws Exception {
        GenericRepo.insert(entite, con);
    }

    @Override
    public void save(T entite, String nomTable, Connection con) throws Exception {
        GenericRepo.insert(entite, nomTable, con);
    }
}
