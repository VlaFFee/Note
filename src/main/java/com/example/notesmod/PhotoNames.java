package com.example.notesmod;

public final class PhotoNames {
    /** Защита от выхода из папки (../ и т.п.). Используется и на сервере, и на клиенте. */
    public static boolean valid(String n) {
        return n != null && !n.isEmpty() && n.length() <= 128
                && n.indexOf('/') < 0 && n.indexOf('\\') < 0
                && n.indexOf(':') < 0 && !n.contains("..");
    }

    private PhotoNames() {}
}
