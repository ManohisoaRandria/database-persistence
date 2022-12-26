/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mg.manohisoa.databasePersistence.exception;

/**
 *
 * @author 211407
 */
public class DatabasePersistenceException extends Exception {

    /**
     * Creates a new instance of <code>DatabasePersistenceException</code>
     * without detail message.
     */
    public DatabasePersistenceException() {
    }

    /**
     * Constructs an instance of <code>DatabasePersistenceException</code> with
     * the specified detail message.
     *
     * @param msg the detail message.
     */
    public DatabasePersistenceException(String msg) {
        super(msg);
    }
}
