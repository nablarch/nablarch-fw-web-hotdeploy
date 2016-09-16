package nablarch.fw.hotdeploy;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import nablarch.core.util.BinaryUtil;

/**
 * HotDeploy用のクラスローダ。
 *
 * @author kawasima
 */
public class HotDeployClassLoader extends URLClassLoader {

    /** ClassLoaderクラスのfindLoadedClassメソッド */
    static final Method FIND_LOADED_CLASS_METHOD;

    /** ClassLoaderクラスのfindBootstrapClassOrNullメソッド */
    static final Method FIND_BOOTSTRAP_CLASS_OR_NULL_METHOD;

    /** 空配列 */
    static final URL[] EMPTY_URLS = new URL[]{};

    static {
        try {
            FIND_LOADED_CLASS_METHOD = ClassLoader.class
                    .getDeclaredMethod("findLoadedClass", new Class[] {String.class});
            FIND_LOADED_CLASS_METHOD.setAccessible(true);
            FIND_BOOTSTRAP_CLASS_OR_NULL_METHOD = ClassLoader.class
                    .getDeclaredMethod("findBootstrapClassOrNull", new Class[] {String.class});
            FIND_BOOTSTRAP_CLASS_OR_NULL_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /** ホットデプロイターゲットとなるパッケージリスト */
    private final List<String> targetPackages;

    /**
     * コンストラクタ
     * 
     * @param parent 既存のクラスローダ
     * @param targetPackages ホットデプロイターゲットとなるパッケージリスト
     */
    public HotDeployClassLoader(ClassLoader parent, List<String> targetPackages) {
        super(EMPTY_URLS, parent);
        this.targetPackages = targetPackages;
    }

    /**
     * ホットデプロイのターゲットかどうか。
     * 
     * @param name 修飾名
     * @return ホットデプロイのターゲットであればtrue
     */
    private boolean isTarget(String name) {
        if (targetPackages == null) {
            return false;
        }

        for (String pkg : targetPackages) {
            if (name.startsWith(pkg)) {
                return true;
            }
        }
        return false;

    }
    @Override
    public Class<?> loadClass(String name, boolean resolve) 
        throws ClassNotFoundException {
        if (!isTarget(name)) {
            return super.loadClass(name, resolve);
        }

        synchronized (this) {
            @SuppressWarnings("rawtypes")
            Class loadedClass = findLoadedClass(name);

            if (loadedClass == null) {
                try {
                    ClassLoader parent = getParent();
                    loadedClass = (Class<?>) FIND_BOOTSTRAP_CLASS_OR_NULL_METHOD
                            .invoke(getParent(), name);

                    while (parent != null && loadedClass == null) {
                        loadedClass = (Class<?>) FIND_LOADED_CLASS_METHOD
                                .invoke(parent, name);
                        parent = parent.getParent();
                    }
                } catch (Exception e) {
                    /* ignore */
                }
            }

            if (loadedClass == null) {
                try {
                    loadedClass = defineClass(name, resolve);
                } catch (IllegalAccessError e) {
                    loadedClass = getParent().loadClass(name);
                }

                if (loadedClass == null) {
                    loadedClass = super.loadClass(name, resolve);
                }
            }
            return loadedClass;

        }
    }

    /**
     * クラスインスタンスを生成する。
     * 
     * @param className 生成するクラス名
     * @param resolve クラスをリンクするかどうか
     * @return 作成された Class オブジェクト
     */
    @SuppressWarnings("rawtypes")
    private Class defineClass(String className, boolean resolve) {
        String path = className.replace('.', '/') + ".class";
        InputStream is = super.getResourceAsStream(path);
        if (is != null) {
            byte[] classBin = BinaryUtil.toByteArray(is);
            Class definedClass = defineClass(className, classBin, 0, classBin.length);
            if (resolve) {
                resolveClass(definedClass);
            }
            return definedClass;
        }
        return null;
    }

}
