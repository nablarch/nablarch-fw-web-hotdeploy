package nablarch.fw.hotdeploy;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;

import java.beans.Introspector;
import java.util.List;

/**
 * NablarchアプリケーションをHotDeployするためのハンドラ。
 *
 * @author kawasima
 */
public class HotDeployHandler implements Handler<Object, Object> {

    /** HotDeploy対象のパッケージ **/
    private List<String> targetPackages;

    /**
     * {@inheritDoc}<br/>
     */
    @Override
    public Object handle(Object data, ExecutionContext context) {
        ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader hotdeployLoader = new HotDeployClassLoader(originalLoader, targetPackages);
        Thread.currentThread().setContextClassLoader(hotdeployLoader);
        try {
            return context.handleNext(data);
        } finally {
            Introspector.flushCaches();
            Thread.currentThread().setContextClassLoader(originalLoader);
        }
    }

    /**
     * HotDeploy対象のパッケージを取得。
     *
     * @return HotDeploy対象のパッケージ
     */
    public List<String> getTargetPackages() {
        return targetPackages;
    }

    /**
     * HotDeploy対象のパッケージを設定。
     *
     * @param targetPackages HotDeploy対象のパッケージ。
     */
    public void setTargetPackages(List<String> targetPackages) {
        this.targetPackages = targetPackages;
    }
}
