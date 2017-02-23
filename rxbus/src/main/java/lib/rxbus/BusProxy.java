package lib.rxbus;

/**
 * Created by cxmax on 2017/2/23.
 */

public class BusProxy {

    protected interface ProxyAction<T,V> {
        void toDo(T t,V v);
    }
}
