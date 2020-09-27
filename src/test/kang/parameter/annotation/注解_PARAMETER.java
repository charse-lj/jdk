package test.kang.parameter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 专用于形参类型的注解
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface 注解_PARAMETER {
}
