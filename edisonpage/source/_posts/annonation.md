---
title: Java注解与ButterKnife的实现
---

前些天的时候，看了一些关于注解的文章，发现注解是个溜得飞起的东西，特别是借用注解动态生成代码，真的是让我眼前一亮，所以后面借着这股劲把ButterKnife的源码也看了一下。

<!--More-->

### 关于Java注解
注解(Annotation)在JDK1.5之后增加的一个新特性，注解的引入意义很大，有很多非常有名的框架，比如后台的Hibernate、Spring，Android端的ButterKnife、Dragger等，
注解的使用提高了代码的可读性，使得一些配置更加灵活，更好的实现了解耦合。用代码去生成代码，可以减去不必要的重复的代码输出，同时提高重复代码输出的可靠性。

Java目前只内置了三种标准注解，分别是：
- @Override: 表示当前的方法定义将覆盖超类中的方法；
- @Deprecated: 表示被注解的方法已经过时，推荐使用新的方法代替，但可以继续使用该方法，只是编译器会提示警告；
- @Suppress Warnings: 可以关闭编译器提示的警告

若想自定义注解，则要用到四种元注解，元注解的作用就是生成注解，下面分别介绍四种元注解：
- @Target:
  表示该注解可以用在什么地方，可能的ElementType参数包括：
  CONSTRUCTOR: 构造器的声明 （用于构造函数）
  FILED: 域声明 （用于成员变量、对象、属性）
  LOCAL_VARIABLE: 局部变量声明 （用于用于描述局部变量）
  METHOD: 方法声明 （用于方法）
  PACKAGE: 包声明 （用于描述包）
  PARAMERER: 参数声明 （用于描述参数）
  TYPE: 类、接口 （用于描述类、接口）

- @Retention
  表示需要在什么级别保存该注解信息，可选的RetenPolicy参数包括：
  SOURCE: 注解将被编译器丢弃
  CLASS: 注解在class文件中可用，但会被VM丢弃
  RUNTIME: VM将在运行期也保留注解，因此可以通过反射机制读取注解的信息 

- @Documented
  将注解包含在Javadoc中

- @Inherited
  允许子类继承父类中的注解

### 举个栗子
```
@Target(ElementType.METHOD)  //此注解只能用在类中的方法上
@Retention(RetrntionPolicy.RUNTIME) //此注解在运行期间仍有效
public @interface UserCase{

   /*
   *此注解可以配置两个参数，一个是int类型的id,一个是默认值为"nothing"的字符串
   */
	public int id();
	public String description() default "nothing";
}

```
声明完后，接下来就是如何去使用这个注解：
```
public class PasswordUtils{
    @UseCase(id = 47,description = "Password must contain at least one numeric")
    public boolean validatePassword(String password){
        return (password.matches("\\w*\\d\\W*"));
    } 

    @UseCase(id = 48)
    public String encryptPassword(String password){
        return new StringBuilder(password.reverse()).toString();
    }

    @UseCase(id = 49,description = "New password can't equal previously used ones")
    public boolean checkForNewPassword(List<String> prevPasswords,String password){
       return !prevPasswords.contains(password);
    }

}

```

那么问题来了，怎么去处理这些注解呢？如何从注解中得到需要的信息？Java为我们提供了apt(Java Poet)去处理注解，apt留到分析ButterKnife再介绍，下面先用反射处理一下：
```
public class UseCaseTracker{
    public static void trackerUseCase(List<Integer> useCases,Class<?> cl){
        for(Method m: cl.getDeclaredMethod()){
            UseCase uc = m.getAnnotation(UseCase.class);
            if(uc != null){
                System.out.println("Found use case:" + uc.id() + uc.description());
                useCases.remove(new Integer(uc.id()));   
            }
        }

        for(int i : useCases){
            System.out.println("Warning : Missing use case-" + i);
        }
    }
}

//测试
public static void main(String[] args){
    List<Integer> useCases = new ArrayList<>();
    Collections.addAll(useCases,47,48,49,50);
    trackUseCases(useCases,PasswordUtils.class);
}

//output
Found Use Case: 47 Password must contain at least one numeric
Found Use Case: 48 no description
Found Use Case: 49 New password can't equal previously used ones
Warning: Missing use case-50
```

### ButterKnife的实现

ButterKnife是基于Java的apt(annotation processing tool)实现的，下面先说说什么是apt：
简单来说apt是Java提供的用于处理注解的工具，使用apt生成注解处理器时，我们无法利用Java的反射机制，因为我们操作的是源码，也就是说apt是在编译时期处理了注解。
apt使用起来还是比较方便的，继承AbstractProcessor类，实现其中的process方法，在process方法中去处理我们自定义的注解，下面看看ButterKnife里的实现：
```
@AutoService(Processor.class)
public final class ButterKnifeProcessor extends AbstractProcessor {
    ......
    @Override 
    public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
    Map<TypeElement, BindingSet> bindingMap = findAndParseTargets(env);

    for (Map.Entry<TypeElement, BindingSet> entry : bindingMap.entrySet()) {
      TypeElement typeElement = entry.getKey();
      BindingSet binding = entry.getValue();

      JavaFile javaFile = binding.brewJava(sdk, debuggable);
      try {
        javaFile.writeTo(filer);
      } catch (IOException e) {
        error(typeElement, "Unable to write binding for type %s: %s", typeElement, e.getMessage());
      }
    }

    return false;
  }
}
```

通过findAndParseTargets方法找到所有使用了ButterKnife所支持的注解的类，看了源码才知道原来ButterKnife自定义的注解原来有那么多，这里就只介绍@BindView了：
```
//ButterKnife所有的自定义注解
private Set<Class<? extends Annotation>> getSupportedAnnotations() {
    Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();

    annotations.add(BindAnim.class);
    annotations.add(BindArray.class);
    annotations.add(BindBitmap.class);
    annotations.add(BindBool.class);
    annotations.add(BindColor.class);
    annotations.add(BindDimen.class);
    annotations.add(BindDrawable.class);
    annotations.add(BindFloat.class);
    annotations.add(BindFont.class);
    annotations.add(BindInt.class);
    annotations.add(BindString.class);
    annotations.add(BindView.class);
    annotations.add(BindViews.class);
    annotations.addAll(LISTENERS);

    return annotations;
  }
```

```
private Map<TypeElement, BindingSet> findAndParseTargets(RoundEnvironment env) {
    Map<TypeElement, BindingSet.Builder> builderMap = new LinkedHashMap<>();
    Set<TypeElement> erasedTargetNames = new LinkedHashSet<>();

    scanForRClasses(env);

    ......

    // Process each @BindView element.
    for (Element element : env.getElementsAnnotatedWith(BindView.class)) {
      // we don't SuperficialValidation.validateElement(element)
      // so that an unresolved View type can be generated by later processing rounds
      try {
        parseBindView(element, builderMap, erasedTargetNames);
      } catch (Exception e) {
        logParsingError(element, BindView.class, e);
      }
    }

}
```

在parseBindView里面去处理被@BindView所注解的view的id，在后面的自动生成findViewById代码中会用到，这里就不对这个方法展开了。
我们回过头来看看process方法，可以下面的两行代码：
```
JavaFile javaFile = binding.brewJava(sdk, debuggable);
javaFile.writeTo(filer);
```
好了，终于要进入高潮了，就是这两行代码，实现了自动生成findViewById的代码，为我们自动绑定了view，事不宜迟，赶紧上代码：
```
JavaFile brewJava(int sdk, boolean debuggable) {
    return JavaFile.builder(bindingClassName.packageName(), createType(sdk, debuggable))
        .addFileComment("Generated code from Butter Knife. Do not modify!")
        .build();
  }
```
“Generated code from Butter Knife. Do not modify!”这句话是不是好像在哪见过？没错，就是在AS项目中的build目录下，可以找到一大堆ButterKnife自动生成的
XX_ViewBinding.java文件的第一句话，这再次说明brewJava方法就是自动生成绑定控件代码的精髓所在。
JavaFile.builder()中有两个参数，一个是packageName即为使用了@BindView类的所在包名，一个是TypeSpec，packageName好理解，下面关注TypeSpec。
```
private TypeSpec createType(int sdk, boolean debuggable) {
    TypeSpec.Builder result = TypeSpec.classBuilder(bindingClassName.simpleName())
        .addModifiers(PUBLIC);
    if (isFinal) {
      result.addModifiers(FINAL);
    }

    if (parentBinding != null) {
      result.superclass(parentBinding.bindingClassName);
    } else {
      result.addSuperinterface(UNBINDER);  //为XX_ViewBinding添加需要实现的接口
    }                                    //private static final ClassName UNBINDER = ClassName.get("butterknife", "Unbinder");

    if (hasTargetField()) {
      result.addField(targetTypeName, "target", PRIVATE);
    }

    if (isView) {
      result.addMethod(createBindingConstructorForView()); //为XX_ViewBinding添加方法
    } else if (isActivity) {
      result.addMethod(createBindingConstructorForActivity());
    } else if (isDialog) {
      result.addMethod(createBindingConstructorForDialog());
    }
    if (!constructorNeedsView()) {
      // Add a delegating constructor with a target type + view signature for reflective use.
      result.addMethod(createBindingViewDelegateConstructor());
    }
    result.addMethod(createBindingConstructor(sdk, debuggable));  //为XX_ViewBinding添加构造函数

    if (hasViewBindings() || parentBinding == null) {
      result.addMethod(createBindingUnbindMethod(result));
    }

    return result.build();
  }
```
TypeSpec就是代表一个类，在这里就是代表XX_ViewBinding这个类，createType方法构建了一个XX_ViewBinding类的所有方法及属性，下面是一个XX_ViewBinding的示例代码：
```
public class AccountActivity_ViewBinding implements Unbinder {
  private AccountActivity target;

  @UiThread
  public AccountActivity_ViewBinding(AccountActivity target) {
    this(target, target.getWindow().getDecorView());
  }

  @UiThread
  public AccountActivity_ViewBinding(AccountActivity target, View source) {
    this.target = target;

    target.oilCardRl = Utils.findRequiredViewAsType(source, R.id.oil_card_rl, "field 'oilCardRl'", RelativeLayout.class);
    target.feeChargeRl = Utils.findRequiredViewAsType(source, R.id.fee_charge_rl, "field 'feeChargeRl'", RelativeLayout.class);
    target.mPiccText = Utils.findRequiredViewAsType(source, R.id.picc_text, "field 'mPiccText'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    AccountActivity target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.oilCardRl = null;
    target.feeChargeRl = null;
    target.mPiccText = null;
  }
}
```

TypeSpec构建出来的方法只是一个空的方法，方法的实现要依靠MethodSpec，我们看看createBindingConstructor的实现呗
```
private MethodSpec createBindingConstructor(int sdk, boolean debuggable) {
    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
        .addAnnotation(UI_THREAD)
        .addModifiers(PUBLIC);

    if (hasMethodBindings()) {
      constructor.addParameter(targetTypeName, "target", FINAL);
    } else {
      constructor.addParameter(targetTypeName, "target");
    }

    if (constructorNeedsView()) {
      constructor.addParameter(VIEW, "source");
    } else {
      constructor.addParameter(CONTEXT, "context");
    }

    if (hasUnqualifiedResourceBindings()) {
      // Aapt can change IDs out from underneath us, just suppress since all will work at runtime.
      constructor.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
          .addMember("value", "$S", "ResourceType")
          .build());
    }

    if (hasOnTouchMethodBindings()) {
      constructor.addAnnotation(AnnotationSpec.builder(SUPPRESS_LINT)
          .addMember("value", "$S", "ClickableViewAccessibility")
          .build());
    }

    if (parentBinding != null) {
      if (parentBinding.constructorNeedsView()) {
        constructor.addStatement("super(target, source)");
      } else if (constructorNeedsView()) {
        constructor.addStatement("super(target, source.getContext())");
      } else {
        constructor.addStatement("super(target, context)");
      }
      constructor.addCode("\n");
    }
    if (hasTargetField()) {
      constructor.addStatement("this.target = target");
      constructor.addCode("\n");
    }

    if (hasViewBindings()) {
      if (hasViewLocal()) {
        // Local variable in which all views will be temporarily stored.
        constructor.addStatement("$T view", VIEW);
      }
      for (ViewBinding binding : viewBindings) {
        addViewBinding(constructor, binding, debuggable);
      }
      for (FieldCollectionViewBinding binding : collectionBindings) {
        constructor.addStatement("$L", binding.render(debuggable));
      }

      if (!resourceBindings.isEmpty()) {
        constructor.addCode("\n");
      }
    }

    if (!resourceBindings.isEmpty()) {
      if (constructorNeedsView()) {
        constructor.addStatement("$T context = source.getContext()", CONTEXT);
      }
      if (hasResourceBindingsNeedingResource(sdk)) {
        constructor.addStatement("$T res = context.getResources()", RESOURCES);
      }
      for (ResourceBinding binding : resourceBindings) {
        constructor.addStatement("$L", binding.render(sdk));
      }
    }

    return constructor.build();
  }

```
可以看到，MethodSpec是XX_ViewBinding中类的方法的具体实现，什么？看不到XX_ViewBindding类里的方法代码的影子？别着急，再看看下面这段就恍然大悟了
```
private void addViewBinding(MethodSpec.Builder result, ViewBinding binding, boolean debuggable) {
    if (binding.isSingleFieldBinding()) {
      // Optimize the common case where there's a single binding directly to a field.
      FieldViewBinding fieldBinding = binding.getFieldBinding();
      CodeBlock.Builder builder = CodeBlock.builder()
          .add("target.$L = ", fieldBinding.getName());

      boolean requiresCast = requiresCast(fieldBinding.getType());
      if (!debuggable || (!requiresCast && !fieldBinding.isRequired())) {
        if (requiresCast) {
          builder.add("($T) ", fieldBinding.getType());
        }
        builder.add("source.findViewById($L)", binding.getId().code);
      } else {
        builder.add("$T.find", UTILS);
        builder.add(fieldBinding.isRequired() ? "RequiredView" : "OptionalView");
        if (requiresCast) {
          builder.add("AsType");
        }
        builder.add("(source, $L", binding.getId().code);
        if (fieldBinding.isRequired() || requiresCast) {
          builder.add(", $S", asHumanDescription(singletonList(fieldBinding)));
        }
        if (requiresCast) {
          builder.add(", $T.class", fieldBinding.getRawType());
        }
        builder.add(")");
      }
      result.addStatement("$L", builder.build());
      return;
    }

    List<MemberViewBinding> requiredBindings = binding.getRequiredBindings();
    if (!debuggable || requiredBindings.isEmpty()) {
      result.addStatement("view = source.findViewById($L)", binding.getId().code);
    } else if (!binding.isBoundToRoot()) {
      result.addStatement("view = $T.findRequiredView(source, $L, $S)", UTILS,
          binding.getId().code, asHumanDescription(requiredBindings));
    }

    addFieldBinding(result, binding, debuggable);
    addMethodBindings(result, binding, debuggable);
  }
```
没错，方法中的所有代码都是通过CodeBlock一句句抠出来的哦......

到这里，ButterKnife就分析完了，这里还要介绍一下JavaPoet，因为ButterKnife中也是用到了它去生成XX_ViewBinding.java
JavaPoet是一组用来生成 .java文件的JAVA API。正如其名，当你创建.java文件时，你将不用再处理代码换行、缩进、引用导入等枯燥而又容易出错的工作，
这一切JavaPoet都将能够很好地为你完成，你的工作将变得富有诗意。
TypeSpec、ParameterSpec、MethodSpec、CodeBlock、JavaFile都是JavaPoet提供的用于描述一个源文件元素的类



















