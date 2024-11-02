package org.figuramc.figura.server.utils;

public class Result<V> {
    private final Either<V, Throwable> value;

    public Result(V value) {
        this.value = Either.newA(value);
    }

    public <V> Result(Throwable throwable) {
        this.value = Either.newB(throwable);
    }

    public V get() throws Throwable {
        if (value.isA()) return value.a();
        throw value.b();
    }
}
