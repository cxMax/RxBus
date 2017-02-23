package lib.rxbus;

/**
 * Created by cxmax on 2017/2/23.
 */

public enum ThreadType {
    MainThread,
    IO,
    Computation,
    Immediate,
    NewThread,
    Trampoline
}
