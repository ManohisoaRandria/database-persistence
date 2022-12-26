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
public class SqlAndReflectException extends RuntimeException {

    /**
     * Creates a new instance of <code>OtherReflectAndSqlException</code>
     * without detail message.
     */
    public SqlAndReflectException() {
    }

    /**
     * Constructs an instance of <code>OtherReflectAndSqlException</code> with
     * the specified detail message.
     *
     * @param msg the detail message.
     */
    public SqlAndReflectException(String msg) {
        super(msg);
    }
}
