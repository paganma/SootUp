package sootup.java.bytecode.frontend.inputlocation;

import categories.TestCategories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sootup.java.core.views.JavaView;

public class FixJars extends BaseFixJarsTest {

@Test
public void executescouterserverjar(){
	String jarDownloadUrl = "https://repo1.maven.org/maven2/io/github/scouter-project/scouter-server/2.20.0/scouter-server-2.20.0.jar";
    String methodSignature = "<scouter.util.SysJMX: java.lang.String getHostName()>";
    JavaView javaView = supplyJavaView(jarDownloadUrl);
    assertMethodConversion(javaView,methodSignature);
    assertJar(javaView);
}

@Test
public void executeturfpointjar(){
	String jarDownloadUrl = "https://repo1.maven.org/maven2/org/webjars/npm/turf-point/2.0.1/turf-point-2.0.1.jar";
    String methodSignature = "";
    JavaView javaView = supplyJavaView(jarDownloadUrl);
    assertMethodConversion(javaView,methodSignature);
    assertJar(javaView);
}

}