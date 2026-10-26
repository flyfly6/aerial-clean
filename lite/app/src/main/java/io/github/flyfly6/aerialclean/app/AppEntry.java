package io.github.flyfly6.aerialclean.app;

/**
 * 一条可显示的应用记录。包名是唯一键。
 */
public final class AppEntry {

    public final String pkg;
    /** 扁平化的 ComponentName，启动时用 {@code ComponentName.unflattenFromString} 还原。 */
    public final String component;
    public final CharSequence label;
    public final boolean system;

    public AppEntry(String pkg, String component, CharSequence label, boolean system) {
        this.pkg = pkg;
        this.component = component;
        this.label = label;
        this.system = system;
    }
}
