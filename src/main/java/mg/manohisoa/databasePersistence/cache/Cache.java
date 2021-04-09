package mg.manohisoa.databasePersistence.cache;

import java.sql.Timestamp;
import java.util.List;

public class Cache<E> {

    private List<E> result;
    private Timestamp tempexp;

    public Cache(List<E> result, Timestamp tempexp) {
        this.result = result;
        this.tempexp = tempexp;
    }

    public Cache() {
    }

    public List<E> getResult() {
        return result;
    }

    public void setResult(List<E> result) {
        this.result = result;
    }

    public Timestamp getTempexp() {
        return tempexp;
    }

    public void setTempexp(Timestamp tempexp) {
        this.tempexp = tempexp;
    }

}
