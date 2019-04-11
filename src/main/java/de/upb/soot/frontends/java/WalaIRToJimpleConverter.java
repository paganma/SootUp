/*
 * @author Linghui Luo
 * @version 1.0
 */
package de.upb.soot.frontends.java;

import com.ibm.wala.cast.loader.AstClass;
import com.ibm.wala.cast.loader.AstField;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.FixedSizeBitVector;
import de.upb.soot.Project;
import de.upb.soot.core.Body;
import de.upb.soot.core.ClassType;
import de.upb.soot.core.Modifier;
import de.upb.soot.core.ResolvingLevel;
import de.upb.soot.core.SootClass;
import de.upb.soot.core.SootField;
import de.upb.soot.core.SootMethod;
import de.upb.soot.frontends.ClassSource;
import de.upb.soot.frontends.JavaClassSource;
import de.upb.soot.jimple.Jimple;
import de.upb.soot.jimple.basic.Local;
import de.upb.soot.jimple.basic.LocalGenerator;
import de.upb.soot.jimple.basic.PositionInfo;
import de.upb.soot.jimple.basic.Trap;
import de.upb.soot.jimple.common.stmt.IStmt;
import de.upb.soot.namespaces.INamespace;
import de.upb.soot.namespaces.JavaSourcePathNamespace;
import de.upb.soot.signatures.DefaultSignatureFactory;
import de.upb.soot.signatures.FieldSignature;
import de.upb.soot.signatures.MethodSignature;
import de.upb.soot.types.DefaultTypeFactory;
import de.upb.soot.types.JavaClassType;
import de.upb.soot.types.NullType;
import de.upb.soot.types.PrimitiveType;
import de.upb.soot.types.Type;
import de.upb.soot.types.VoidType;
import de.upb.soot.views.JavaView;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Converter which converts WALA IR to jimple.
 *
 * @author Linghui Luo created on 17.09.18
 */
public class WalaIRToJimpleConverter {
  protected JavaView view;
  private INamespace srcNamespace;
  private HashMap<String, Integer> clsWithInnerCls;
  private HashMap<String, String> walaToSootNameTable;
  private Set<SootField> sootFields;

  public WalaIRToJimpleConverter(Set<String> sourceDirPath) {
    // TODO According to the annotation, we shouldn't pass in null here
    Project project =
        new Project(null, DefaultSignatureFactory.getInstance(), DefaultTypeFactory.getInstance());

    srcNamespace = new JavaSourcePathNamespace(sourceDirPath);
    view = new JavaView(project);
    clsWithInnerCls = new HashMap<>();
    walaToSootNameTable = new HashMap<>();
  }

  /**
   * Convert a wala {@link AstClass} to {@link SootClass}.
   *
   * @return A SootClass converted from walaClass
   */
  public SootClass convertClass(AstClass walaClass) {
    ClassSource classSource = createClassSource(walaClass);
    JavaClassType classSig = classSource.getClassType();
    // get super class
    IClass sc = walaClass.getSuperclass();
    JavaClassType superClass = null;
    if (sc != null) {
      superClass =
          view.getTypeFactory().getClassType(convertClassNameFromWala(sc.getName().toString()));
    }

    // get interfaces
    Set<JavaClassType> interfaces = new HashSet<>();
    for (IClass i : walaClass.getDirectInterfaces()) {
      JavaClassType inter =
          view.getTypeFactory().getClassType(convertClassNameFromWala(i.getName().toString()));
      interfaces.add(inter);
    }

    // get outer class
    JavaClassType outerClass = null;
    if (walaClass instanceof com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl.JavaClass) {
      com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl.JavaClass javaClass =
          (com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl.JavaClass) walaClass;
      IClass ec = javaClass.getEnclosingClass();
      if (ec != null) {
        outerClass =
            view.getTypeFactory().getClassType(convertClassNameFromWala(ec.getName().toString()));
      }
    }

    // add source position
    Position position = walaClass.getSourcePosition();

    // convert modifiers
    EnumSet<Modifier> modifiers = converModifiers(walaClass);

    // convert fields
    Set<IField> fields = HashSetFactory.make(walaClass.getDeclaredInstanceFields());
    fields.addAll(walaClass.getDeclaredStaticFields());
    sootFields = new HashSet<>();
    for (IField walaField : fields) {
      SootField sootField = convertField(classSig, (AstField) walaField);
      sootFields.add(sootField);
    }

    if (outerClass != null) {
      // create enclosing reference to outerClass
      FieldSignature signature =
          view.getSignatureFactory().getFieldSignature("this$0", classSig, outerClass);
      SootField enclosingObject = new SootField(signature, EnumSet.of(Modifier.FINAL));
      sootFields.add(enclosingObject);
    }

    // convert methods
    Set<SootMethod> sootMethods = new HashSet<>();

    for (IMethod walaMethod : walaClass.getDeclaredMethods()) {
      SootMethod sootMethod = convertMethod(classSig, (AstMethod) walaMethod);
      sootMethods.add(sootMethod);
    }

    return new SootClass(
        ResolvingLevel.BODIES,
        classSource,
        ClassType.Application,
        superClass,
        interfaces,
        outerClass,
        sootFields,
        sootMethods,
        position,
        modifiers);
  }

  /** Create a {@link JavaClassSource} object for the given walaClass. */
  public JavaClassSource createClassSource(AstClass walaClass) {
    String fullyQualifiedClassName = convertClassNameFromWala(walaClass.getName().toString());
    JavaClassType classSignature =
        DefaultTypeFactory.getInstance().getClassType(fullyQualifiedClassName);
    URL url = walaClass.getSourceURL();
    Path sourcePath = Paths.get(url.getPath());
    return new JavaClassSource(srcNamespace, sourcePath, classSignature);
  }

  /**
   * Convert a wala {@link AstField} to {@link SootField}.
   *
   * @param classSig the class owns the field
   * @param walaField the wala field
   * @return A SootField object converted from walaField.
   */
  public SootField convertField(JavaClassType classSig, AstField walaField) {
    Type type = convertType(walaField.getFieldTypeReference());
    EnumSet<Modifier> modifiers = convertModifiers(walaField);
    FieldSignature signature =
        view.getSignatureFactory()
            .getFieldSignature(walaField.getName().toString(), classSig, type);
    return new SootField(signature, modifiers);
  }

  /**
   * Convert a wala {@link AstMethod} to {@link SootMethod} and add it into the given sootClass.
   *
   * @param classSig the SootClass which should contain the converted SootMethod
   * @param walaMethod the walMethod to be converted
   */
  public SootMethod convertMethod(JavaClassType classSig, AstMethod walaMethod) {
    // create SootMethod instance
    List<Type> paraTypes = new ArrayList<>();
    List<String> sigs = new ArrayList<>();
    if (walaMethod.symbolTable() != null) {
      for (int i = 0; i < walaMethod.getNumberOfParameters(); i++) {
        TypeReference type = walaMethod.getParameterType(i);
        if (i == 0) {
          if (!walaMethod.isStatic()) {
            // ignore this pointer
            continue;
          }
        }
        Type paraType = convertType(type);
        paraTypes.add(this.view.getTypeFactory().getType(paraType.toString()));
        sigs.add(paraType.toString());
      }
    }

    Type returnType = convertType(walaMethod.getReturnType());

    EnumSet<Modifier> modifiers = convertModifiers(walaMethod);

    List<JavaClassType> thrownExceptions = new ArrayList<>();
    try {
      for (TypeReference exception : walaMethod.getDeclaredExceptions()) {
        String exceptionName = convertClassNameFromWala(exception.getName().toString());
        JavaClassType exceptionSig = this.view.getTypeFactory().getClassType(exceptionName);
        thrownExceptions.add(exceptionSig);
      }
    } catch (UnsupportedOperationException | InvalidClassFileException e) {
      e.printStackTrace();
    }
    // add debug info
    DebuggingInformation debugInfo = walaMethod.debugInfo();
    MethodSignature methodSig =
        this.view
            .getSignatureFactory()
            .getMethodSignature(
                walaMethod.getName().toString(), classSig, returnType.toString(), sigs);

    return new SootMethod(
        new WalaIRMethodSourceContent(methodSig),
        methodSig,
        modifiers,
        thrownExceptions,
        createBody(methodSig, modifiers, walaMethod),
        debugInfo);
  }

  public Type convertType(TypeReference type) {
    if (type.isPrimitiveType()) {
      if (type.equals(TypeReference.Boolean)) {
        return PrimitiveType.getBoolean();
      } else if (type.equals(TypeReference.Byte)) {
        return PrimitiveType.getByteSignature();
      } else if (type.equals(TypeReference.Char)) {
        return PrimitiveType.getChar();
      } else if (type.equals(TypeReference.Short)) {
        return PrimitiveType.getShort();
      } else if (type.equals(TypeReference.Int)) {
        return PrimitiveType.getInt();
      } else if (type.equals(TypeReference.Long)) {
        return PrimitiveType.getLong();
      } else if (type.equals(TypeReference.Float)) {
        return PrimitiveType.getFloat();
      } else if (type.equals(TypeReference.Double)) {
        return PrimitiveType.getDouble();
      } else if (type.equals(TypeReference.Void)) {
        return VoidType.getInstance();
      }
    } else if (type.isReferenceType()) {
      if (type.isArrayType()) {
        TypeReference t = type.getInnermostElementType();
        Type baseType = convertType(t);
        int dim = type.getDimensionality();
        return DefaultTypeFactory.getInstance().getArrayType(baseType, dim);
      } else if (type.isClassType()) {
        if (type.equals(TypeReference.Null)) {
          return NullType.getInstance();
        } else {
          String className = convertClassNameFromWala(type.getName().toString());
          return this.view.getTypeFactory().getClassType(className);
        }
      }
    }
    throw new RuntimeException("Unsupported tpye: " + type);
  }

  /** Return all modifiers for the given field. */
  public EnumSet<Modifier> convertModifiers(AstField field) {
    EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
    if (field.isFinal()) {
      modifiers.add(Modifier.FINAL);
    }
    if (field.isPrivate()) {
      modifiers.add(Modifier.PRIVATE);
    }
    if (field.isProtected()) {
      modifiers.add(Modifier.PROTECTED);
    }
    if (field.isPublic()) {
      modifiers.add(Modifier.PUBLIC);
    }
    if (field.isStatic()) {
      modifiers.add(Modifier.STATIC);
    }
    if (field.isVolatile()) {
      modifiers.add(Modifier.VOLATILE);
    }
    // TODO: TRANSIENT field
    return modifiers;
  }

  /** Return all modifiers for the given methodRef. */
  public EnumSet<Modifier> convertModifiers(AstMethod method) {
    EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
    if (method.isPrivate()) {
      modifiers.add(Modifier.PRIVATE);
    }
    if (method.isProtected()) {
      modifiers.add(Modifier.PROTECTED);
    }
    if (method.isPublic()) {
      modifiers.add(Modifier.PUBLIC);
    }
    if (method.isStatic()) {
      modifiers.add(Modifier.STATIC);
    }
    if (method.isFinal()) {
      modifiers.add(Modifier.FINAL);
    }
    if (method.isAbstract()) {
      modifiers.add(Modifier.ABSTRACT);
    }
    if (method.isSynchronized()) {
      modifiers.add(Modifier.SYNCHRONIZED);
    }
    if (method.isNative()) {
      modifiers.add(Modifier.NATIVE);
    }
    if (method.isSynthetic()) {
      modifiers.add(Modifier.SYNTHETIC);
    }
    if (method.isBridge()) {
      // TODO: what is this?
    }
    if (method.isInit()) {
      // TODO:
    }
    if (method.isClinit()) {
      // TODO:
    }
    // TODO: strictfp and annotation
    return modifiers;
  }

  public EnumSet<Modifier> converModifiers(AstClass klass) {
    int modif = klass.getModifiers();
    EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
    if (klass.isAbstract()) {
      modifiers.add(Modifier.ABSTRACT);
    }
    if (klass.isPrivate()) {
      modifiers.add(Modifier.PRIVATE);
    }
    if (klass.isSynthetic()) {
      modifiers.add(Modifier.SYNTHETIC);
    }
    if (klass.isPublic()) {
      modifiers.add(Modifier.PUBLIC);
    }
    if (klass.isInterface()) {
      modifiers.add(Modifier.INTERFACE);
    }

    // TODO: final, enum, annotation
    return modifiers;
  }

  private @Nullable Body createBody(
      MethodSignature methodSignature, EnumSet<Modifier> modifiers, AstMethod walaMethod) {

    if (walaMethod.isAbstract()) {
      return null;
    }

    AbstractCFG<?, ?> cfg = walaMethod.cfg();
    if (cfg != null) {
      List<Trap> traps = new ArrayList<>();
      List<IStmt> stmts = new ArrayList<>();
      LocalGenerator localGenerator = new LocalGenerator();
      // convert all wala instructions to jimple statements
      SSAInstruction[] insts = (SSAInstruction[]) cfg.getInstructions();
      if (insts.length > 0) {

        // set position for body
        DebuggingInformation debugInfo = walaMethod.debugInfo();
        Position bodyPos = debugInfo.getCodeBodyPosition();

        /* Look AsmMethodSourceContent.getBody, see AsmMethodSourceContent.emitLocals(); */

        if (!Modifier.isStatic(modifiers)) {
          JavaClassType thisType = methodSignature.getDeclClassSignature();
          Local thisLocal = localGenerator.generateThisLocal(thisType);
          IStmt stmt =
              Jimple.newIdentityStmt(
                  thisLocal,
                  Jimple.newThisRef(thisType),
                  new PositionInfo(debugInfo.getInstructionPosition(0), null));
          stmts.add(stmt);
        }

        int startPara = 0;
        if (!walaMethod.isStatic()) {
          // wala's first parameter is this reference for non-static methodRef
          startPara = 1;
        }
        for (; startPara < walaMethod.getNumberOfParameters(); startPara++) {
          TypeReference t = walaMethod.getParameterType(startPara);
          Type type = convertType(t);
          Local paraLocal = localGenerator.generateParameterLocal(type, startPara);
          IStmt stmt =
              Jimple.newIdentityStmt(
                  paraLocal,
                  Jimple.newParameterRef(type, startPara - 1),
                  new PositionInfo(debugInfo.getInstructionPosition(0), null));
          stmts.add(stmt);
        }

        // TODO 2. convert traps
        // get exceptions which are not caught
        FixedSizeBitVector blocks = cfg.getExceptionalToExit();
        InstructionConverter instConverter =
            new InstructionConverter(this, methodSignature, walaMethod, localGenerator);
        Map<IStmt, Integer> stmt2IIndex = new HashMap<>();
        for (SSAInstruction inst : insts) {
          List<IStmt> retStmts = instConverter.convertInstruction(debugInfo, inst);
          if (!retStmts.isEmpty()) {
            for (IStmt stmt : retStmts) {
              stmts.add(stmt);
              stmt2IIndex.put(stmt, inst.iindex);
            }
          }
        }
        // set target for goto or conditional statements
        for (IStmt stmt : stmt2IIndex.keySet()) {
          instConverter.setTarget(stmt, stmt2IIndex.get(stmt));
        }
        // add return void stmt for methods with return type beiing void
        if (walaMethod.getReturnType().equals(TypeReference.Void)) {
          IStmt ret =
              Jimple.newReturnVoidStmt(
                  new PositionInfo(debugInfo.getInstructionPosition(insts.length - 1), null));
          instConverter.setTarget(ret, -1);
          stmts.add(ret);
        }

        return new Body(localGenerator.getLocals(), traps, stmts, bodyPos);
      }
    }

    return null;
  }

  /**
   * Convert className in wala-format to soot-format, e.g., wala-format: Ljava/lang/String ->
   * soot-format: java.lang.String.
   *
   * @param className in wala-format
   * @return className in soot.format
   */
  public String convertClassNameFromWala(String className) {
    String cl = className.intern();
    if (walaToSootNameTable.containsKey(cl)) {
      return walaToSootNameTable.get(cl);
    }
    StringBuilder sb = new StringBuilder();
    if (className.startsWith("L")) {
      className = className.substring(1);
      String[] subNames = className.split("/");
      boolean isSpecial = false;
      for (int i = 0; i < subNames.length; i++) {
        String subName = subNames[i];
        if (subName.contains("(") || subName.contains("<")) {
          // handle anonymous or inner classes
          isSpecial = true;
          break;
        }
        if (i != 0) {
          sb.append(".");
        }
        sb.append(subName);
      }
      if (isSpecial) {
        String lastSubName = subNames[subNames.length - 1];
        String[] temp = lastSubName.split(">");
        if (temp.length > 0) {
          String name = temp[temp.length - 1];
          if (!name.contains("$")) {
            // This is an inner class
            String outClass = sb.toString();
            int count = 1;
            if (this.clsWithInnerCls.containsKey(outClass)) {
              count = this.clsWithInnerCls.get(outClass) + 1;
            }
            this.clsWithInnerCls.put(outClass, count);
            sb.append(count).append("$");
          }
          sb.append(name);
        }
      }
    } else {
      throw new RuntimeException("Can not convert WALA class name: " + className);
    }
    String ret = sb.toString();
    walaToSootNameTable.put(cl, ret);
    return ret;
  }

  /**
   * Convert className in soot-format to wala-format, e.g.,soot-format: java.lang.String.->
   * wala-format: Ljava/lang/String
   */
  public String convertClassNameFromSoot(String signature) {
    StringBuilder sb = new StringBuilder();
    sb.append("L");
    String[] subNames = signature.split("\\.");
    for (int i = 0; i < subNames.length; i++) {
      sb.append(subNames[i]);
      if (i != subNames.length - 1) {
        sb.append("/");
      }
    }
    return sb.toString();
  }

  protected void addSootField(SootField field) {
    if (this.sootFields != null) {
      this.sootFields.add(field);
    }
  }
}