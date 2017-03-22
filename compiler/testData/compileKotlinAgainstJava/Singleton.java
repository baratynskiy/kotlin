package test;

public final class Singleton {

    private Singleton() {}

    private static Singleton INSTANCE;

    public static Singleton getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Singleton();
	}

        return INSTANCE;
    }

}
