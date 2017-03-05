package net.feheren_fekete.applist.utils;

public interface RunnableWithRetArg<R, A> {
    R run(A arg);
}
