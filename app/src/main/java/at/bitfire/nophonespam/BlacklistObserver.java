package at.bitfire.nophonespam;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

public class BlacklistObserver {

    protected static final List<WeakReference<Observer>> observers = new LinkedList<>();

    public static void addObserver(Observer observer, boolean immediate) {
        observers.add(new WeakReference<Observer>(observer));
        if (immediate)
            observer.onBlacklistUpdate();
    }

    public static void removeObserver(Observer observer) {
        for (WeakReference<Observer> ref : observers)
            if (ref.get() == observer)
                observers.remove(observer);
    }

    public static void notifyUpdated() {
        for (WeakReference<Observer> ref : observers)
            if (ref.get() != null)
                ref.get().onBlacklistUpdate();
            else
                observers.remove(ref);
    }


    interface Observer {

        void onBlacklistUpdate();

    }

}
