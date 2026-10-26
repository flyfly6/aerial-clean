# 自定义 View 由布局 XML 按类名实例化，R8 看不到引用，必须保留构造函数。
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
