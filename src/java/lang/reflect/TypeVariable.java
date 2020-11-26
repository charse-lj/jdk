/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang.reflect;

/**
 * TypeVariable is the common superinterface for type variables of kinds.
 * A type variable is created the first time it is needed by a reflective
 * method, as specified in this package.  If a type variable t is referenced
 * by a type (i.e, class, interface or annotation type) T, and T is declared
 * by the nth enclosing class of T (see JLS 8.1.2), then the creation of t
 * requires the resolution (see JVMS 5) of the ith enclosing class of T,
 * for i = 0 to n, inclusive. Creating a type variable must not cause the
 * creation of its bounds. Repeated creation of a type variable has no effect.
 *
 * <p>Multiple objects may be instantiated at run-time to
 * represent a given type variable. Even though a type variable is
 * created only once, this does not imply any requirement to cache
 * instances representing the type variable. However, all instances
 * representing a type variable must be equal() to each other.
 * As a consequence, users of type variables must not rely on the identity
 * of instances of classes implementing this interface.
 *
 * @param <D> the type of generic declaration that declared the
 *            underlying type variable.
 *
 * @since 1.5
 *
 * 类型变量，或者也可以叫泛型变量。具体就是指我们在申明泛型时定义的T,K,U这种变量
 *
 * TypeVariable本身也使用了泛型，并且泛型的上界为GenericDeclaration,GenericDeclaration这个接口主要限定了哪些地方可以定义TypeVariable
 * 只能在方法（包括普通方法跟构造方法）以及类上申明泛型
 */
public interface TypeVariable<D extends GenericDeclaration> extends Type, AnnotatedElement {
    /**
     * Returns an array of {@code Type} objects representing the
     * upper bound(s) of this type variable.  If no upper bound is
     * explicitly declared, the upper bound is {@code Object}.
     *
     * <p>For each upper bound B: <ul> <li>if B is a parameterized
     * type or a type variable, it is created, (see {@link
     * java.lang.reflect.ParameterizedType ParameterizedType} for the
     * details of the creation process for parameterized types).
     * <li>Otherwise, B is resolved.  </ul>
     *
     * @return an array of {@code Type}s representing the upper
     * bound(s) of this type variable
     *
     * @throws TypeNotPresentException             if any of the
     *                                             bounds refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if any of the
     *                                             bounds refer to a parameterized type that cannot be instantiated
     *                                             for any reason
     * 获取泛型的边界 --> 类、方法、构造方法在声明泛型变量时,只能声明下边界
     *
     * getBounds()会返回泛型的边界，但是这里的边界跟我们在参数化类型中定义的边界不同，这里的边界只有上界。即我们不通通过super关键字来申明一个泛型
     * 如 class A<T super classA>{}
     * 在申明泛型时，我们要明确一点，申明是为了使用，而在上面的例子中，我们不能使用T来干任何事情，因为我们不能确定T中的任何方法（只能确定它是一个Object，但是这没有任何意义）。
     * 所以对于泛型变量来说，只存在上界，也就是只能使用extends关键字进行申明
     */
    Type[] getBounds();
    
    /**
     * Returns the {@code GenericDeclaration} object representing the
     * generic declaration declared this type variable.
     *
     * @return the generic declaration declared for this type variable.
     *
     * @since 1.5
     * 获取申明所在的具体对象
     * D extends GenericDeclaration --> 限定可以定义TypeVariable的地方: Class、Method、Constructor
     */
    D getGenericDeclaration();
    
    /**
     * Returns the name of this type variable, as it occurs in the source code.
     *
     * @return the name of this type variable, as it appears in the source code
     *
     * 获取具体类型变量的名称
     */
    String getName();
    
    /**
     * Returns an array of AnnotatedType objects that represent the use of
     * types to denote the upper bounds of the type parameter represented by
     * this TypeVariable. The order of the objects in the array corresponds to
     * the order of the bounds in the declaration of the type parameter. Note that
     * if no upper bound is explicitly declared, the upper bound is unannotated
     * {@code Object}.
     *
     * @return an array of objects representing the upper bound(s) of the type variable
     *
     * @since 1.8
     *
     * getAnnotatedBounds()，此方法返回一个AnnotatedType类型的数组，获取的是我们在类型变量的上界。
     * 不同于getBounds()方法的是，这个方法可以获取到边界上添加的注解
     */
    AnnotatedType[] getAnnotatedBounds();
}
