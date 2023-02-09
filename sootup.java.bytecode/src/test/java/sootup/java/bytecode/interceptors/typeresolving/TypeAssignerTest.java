package sootup.java.bytecode.interceptors.typeresolving;

import categories.Java8Test;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import sootup.core.model.Body;
import sootup.core.util.Utils;
import sootup.java.bytecode.interceptors.TypeAssigner;

@Category(Java8Test.class)
public class TypeAssignerTest extends TypeAssignerTestSuite {

  @Before
  public void setup() {
    String baseDir = "../shared-test-resources/TypeResolverTestSuite/CastCounterTest/";
    String className = "CastCounterDemos";
    buildView(baseDir, className);
  }

  @Test
  public void testInvokeStmt() {
    setMethodBody("invokeStmt", "void");
    new TypeAssigner().interceptBody(builder, this.view);
    List<String> actualStmts = Utils.bodyStmtsAsStrings(builder.build());

    Assert.assertEquals(
        Stream.of(
                "r0 := @this: CastCounterDemos",
                "$r1 = new Sub1",
                "specialinvoke $r1.<Sub1: void <init>()>()",
                "r2 = $r1",
                "b0 = 1",
                "$r3 = new Sub2",
                "specialinvoke $r3.<Sub2: void <init>()>()",
                "r4 = $r3",
                "virtualinvoke r2.<Super1: void m(int,Sub2)>(b0, r4)",
                "return")
            .collect(Collectors.toList()),
        actualStmts);
  }

  @Test
  public void testAssignStmt() {
    setMethodBody("assignStmt", "void");
    new TypeAssigner().interceptBody(builder, this.view);

    final Body build = builder.build();
    System.out.println(Utils.generateJimpleForTest(build));

    Assert.assertEquals(
        "{\n"
            + "    CastCounterDemos r0;\n"
            + "    Sub1 $r1;\n"
            + "    Super1 r2;\n"
            + "    Super1[] r3;\n"
            + "\n"
            + "\n"
            + "    r0 := @this: CastCounterDemos;\n"
            + "\n"
            + "    r3 = newarray (Super1)[10];\n"
            + "\n"
            + "    $r1 = new Sub1;\n"
            + "\n"
            + "    specialinvoke $r1.<Sub1: void <init>()>();\n"
            + "\n"
            + "    r3[0] = $r1;\n"
            + "\n"
            + "    r2 = r3[2];\n"
            + "\n"
            + "    return;\n"
            + "}\n",
        build.toString());
  }
}
