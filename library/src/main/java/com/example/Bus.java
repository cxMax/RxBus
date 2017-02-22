package com.example;

import rx.Observable;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

public class Bus {
    private static final Bus INSTANCE = new Bus();

    private final Subject<Object, Object> busSubject = new SerializedSubject<>(PublishSubject.create());

    static void test() {
        Object event = new Runnable() {
            @Override
            public void run() {
                // some event
            }
        };

        Bus.get().post(event);

        Bus.get().on(Runnable.class)
                .subscribe(new Action1<Runnable>() {
                    @Override
                    public void call(Runnable runnable) {
                        // do your stuff
                    }
                });
    }

    public static Bus get() {
        return INSTANCE;
    }

    public void post(Object event) {
        busSubject.onNext(event);
    }

    public <T> Observable<T> on(Class<T> eventClass) {
        return busSubject.ofType(eventClass);
    }

    public <T> Observable<T> onMainThread(Class<T> eventClass) {
        return on(eventClass)
                .observeOn(AndroidSchedulers.mainThread());
    }
}
