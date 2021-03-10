package net.kunmc.lab.vplayer.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class RepeatObservable<T> {
    private List<Consumer<T>> observers = new ArrayList<>();

    public void fire(T data) {
        observers.forEach(e -> e.accept(data));
    }

    public RepeatObservable<Void> thenAccept(Consumer<T> fn) {
        if (fn == null) throw new NullPointerException();
        RepeatObservable<Void> d = new RepeatObservable<Void>();
        observers.add(p -> {
            fn.accept(p);
            d.fire(null);
        });
        return d;
    }

    public RepeatObservable<Void> thenRun(Runnable fn) {
        if (fn == null) throw new NullPointerException();
        RepeatObservable<Void> d = new RepeatObservable<>();
        observers.add(p -> {
            fn.run();
            d.fire(null);
        });
        return d;
    }

    public <U> RepeatObservable<U> thenApply(Function<? super T, ? extends U> fn) {
        if (fn == null) throw new NullPointerException();
        RepeatObservable<U> d = new RepeatObservable<U>();
        observers.add(p -> d.fire(fn.apply(p)));
        return d;
    }
}
