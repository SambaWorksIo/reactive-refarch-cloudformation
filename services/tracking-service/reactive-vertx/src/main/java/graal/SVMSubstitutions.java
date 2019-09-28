package graal;


import com.oracle.svm.core.annotate.*;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

@AutomaticFeature
class RuntimeReflectionRegistrationFeature implements Feature {
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        try {
            // json types
            RuntimeReflection.register(java.util.ArrayList.class.getDeclaredConstructor());
            RuntimeReflection.register(java.util.LinkedHashMap.class.getDeclaredConstructor());

            // extras (grpc seems to need this)
            RuntimeReflection.register(io.netty.channel.socket.nio.NioServerSocketChannel.class.getDeclaredConstructor());

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
