/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mg.manohisoa.databasePersistence;

import mg.manohisoa.databasePersistence.repository.DataRepository;
import mg.manohisoa.databasePersistence.repository.DataRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author P11A-MANOHISOA
 */
@Configuration
public class FwConfiguration {

    @Bean
    public DataRepository dt() {
        return new DataRepositoryImpl();
    }

}
