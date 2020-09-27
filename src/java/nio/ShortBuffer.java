/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

// -- This file was mechanically generated: Do not edit! -- //

package java.nio;

/**
 * A short buffer.
 *
 * <p> This class defines four categories of operations upon
 * short buffers:
 *
 * <ul>
 *
 * <li><p> Absolute and relative {@link #get() <i>get</i>} and
 * {@link #put(short) <i>put</i>} methods that read and write
 * single shorts; </p></li>
 *
 * <li><p> Relative {@link #get(short[]) <i>bulk get</i>}
 * methods that transfer contiguous sequences of shorts from this buffer
 * into an array; and</p></li>
 *
 * <li><p> Relative {@link #put(short[]) <i>bulk put</i>}
 * methods that transfer contiguous sequences of shorts from a
 * short array or some other short
 * buffer into this buffer;&#32;and </p></li>
 *
 * <li><p> A method for {@link #compact compacting}
 * a short buffer.  </p></li>
 *
 * </ul>
 *
 * <p> Short buffers can be created either by {@link #allocate
 * <i>allocation</i>}, which allocates space for the buffer's
 *
 *
 * content, by {@link #wrap(short[]) <i>wrapping</i>} an existing
 * short array  into a buffer, or by creating a
 * <a href="ByteBuffer.html#views"><i>view</i></a> of an existing byte buffer.
 *
 * <p> Like a byte buffer, a short buffer is either <a
 * href="ByteBuffer.html#direct"><i>direct</i> or <i>non-direct</i></a>.  A
 * short buffer created via the {@code wrap} methods of this class will
 * be non-direct.  A short buffer created as a view of a byte buffer will
 * be direct if, and only if, the byte buffer itself is direct.  Whether or not
 * a short buffer is direct may be determined by invoking the {@link
 * #isDirect isDirect} method.  </p>
 *
 * <p> Methods in this class that do not otherwise have a value to return are
 * specified to return the buffer upon which they are invoked.  This allows
 * method invocations to be chained.
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */
/*
 * 包装了short序列的short缓冲区
 *
 * 以下是所有ShortBuffer的5组实现
 *
 * 非直接缓冲区
 *   ShortBuffer
 *       |
 * HeapShortBuffer
 *       |
 * HeapShortBufferR
 *
 *
 * 直接缓冲区（缓冲区字节序与本地相同）
 * ShortBuffer        DirectBuffer
 *      └──────┬──────────┘ │
 *    DirectShortBufferU    │
 *             ├────────────┘
 *    DirectShortBufferRU
 *
 * 直接缓冲区（缓冲区字节序与本地不同）
 * ShortBuffer        DirectBuffer
 *      └──────┬──────────┘ │
 *    DirectShortBufferS    │
 *             ├────────────┘
 *    DirectShortBufferRS
 *
 *
 * ByteBuffer转ShortBuffer
 *                     ShortBuffer
 *           ┌─────────────┘└─────────────┐
 * ByteBufferAsShortBufferB    ByteBufferAsShortBufferL
 *           |                            |
 * ByteBufferAsShortBufferRB   ByteBufferAsShortBufferRL
 */
public abstract class ShortBuffer extends Buffer implements Comparable<ShortBuffer> {
    
    /**
     * These fields are declared here rather than in Heap-X-Buffer in order to
     * reduce the number of virtual method invocations needed to access these
     * values, which is especially costly when coding small buffers.
     */
    final short[] hb;                  // Non-null only for heap buffers
    
    final int offset;    // 寻址偏移量
    boolean isReadOnly;  // 该缓冲区是否只读
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // Creates a new buffer with the given mark, position, limit, capacity, backing array, and array offset
    ShortBuffer(int mark, int pos, int lim, int cap, short[] hb, int offset) {
        super(mark, pos, lim, cap);
        this.hb = hb;
        this.offset = offset;
    }
    
    // Creates a new buffer with the given mark, position, limit, and capacity
    ShortBuffer(int mark, int pos, int lim, int cap) { // package-private
        this(mark, pos, lim, cap, null, 0);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Allocates a new short buffer.
     *
     * <p> The new buffer's position will be zero, its limit will be its
     * capacity, its mark will be undefined, each of its elements will be
     * initialized to zero, and its byte order will be
     *
     *
     *
     * the {@link ByteOrder#nativeOrder native order} of the underlying
     * hardware.
     *
     * It will have a {@link #array backing array}, and its
     * {@link #arrayOffset array offset} will be zero.
     *
     * @param capacity The new buffer's capacity, in shorts
     *
     * @return The new short buffer
     *
     * @throws IllegalArgumentException If the {@code capacity} is a negative integer
     */
    // 创建堆内存缓冲区HeapShortBuffer
    public static ShortBuffer allocate(int capacity) {
        if(capacity<0)
            throw createCapacityException(capacity);
        return new HeapShortBuffer(capacity, capacity);
    }
    
    /**
     * Wraps a short array into a buffer.
     *
     * <p> The new buffer will be backed by the given short array;
     * that is, modifications to the buffer will cause the array to be modified
     * and vice versa.  The new buffer's capacity will be
     * {@code array.length}, its position will be {@code offset}, its limit
     * will be {@code offset + length}, its mark will be undefined, and its
     * byte order will be
     *
     *
     *
     * the {@link ByteOrder#nativeOrder native order} of the underlying
     * hardware.
     *
     * Its {@link #array backing array} will be the given array, and
     * its {@link #arrayOffset array offset} will be zero.  </p>
     *
     * @param array  The array that will back the new buffer
     * @param offset The offset of the subarray to be used; must be non-negative and
     *               no larger than {@code array.length}.  The new buffer's position
     *               will be set to this value.
     * @param length The length of the subarray to be used;
     *               must be non-negative and no larger than
     *               {@code array.length - offset}.
     *               The new buffer's limit will be set to {@code offset + length}.
     *
     * @return The new short buffer
     *
     * @throws IndexOutOfBoundsException If the preconditions on the {@code offset} and {@code length}
     *                                   parameters do not hold
     */
    // 包装一个short数组到buffer（包装一部分）
    public static ShortBuffer wrap(short[] array, int offset, int length) {
        try {
            return new HeapShortBuffer(array, offset, length);
        } catch(IllegalArgumentException x) {
            throw new IndexOutOfBoundsException();
        }
    }
    
    /**
     * Wraps a short array into a buffer.
     *
     * <p> The new buffer will be backed by the given short array;
     * that is, modifications to the buffer will cause the array to be modified
     * and vice versa.  The new buffer's capacity and limit will be
     * {@code array.length}, its position will be zero, its mark will be
     * undefined, and its byte order will be
     *
     *
     *
     * the {@link ByteOrder#nativeOrder native order} of the underlying
     * hardware.
     *
     * Its {@link #array backing array} will be the given array, and its
     * {@link #arrayOffset array offset} will be zero.  </p>
     *
     * @param array The array that will back this buffer
     *
     * @return The new short buffer
     */
    // 包装一个short数组到buffer
    public static ShortBuffer wrap(short[] array) {
        return wrap(array, 0, array.length);
    }
    
    /*▲ 工厂方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 缓冲区属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tells whether or not this short buffer is direct.
     *
     * @return {@code true} if, and only if, this buffer is direct
     */
    // 直接缓冲区/非直接缓冲区
    public abstract boolean isDirect();
    
    /*▲ 缓冲区属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 标记操作 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * {@inheritDoc}
     */
    // 在当前游标position处设置新的mark（备忘）
    @Override
    public final ShortBuffer mark() {
        super.mark();
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    // 设置新的游标position
    @Override
    public final ShortBuffer position(int newPosition) {
        super.position(newPosition);
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    // 设置新的上界limit
    @Override
    public final ShortBuffer limit(int newLimit) {
        super.limit(newLimit);
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    // 将当前游标position回退到mark（备忘）位置
    @Override
    public final ShortBuffer reset() {
        super.reset();
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    // 清理缓冲区，重置标记
    @Override
    public final ShortBuffer clear() {
        super.clear();
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    // 修改标记，可以切换缓冲区读/写模式
    @Override
    public final ShortBuffer flip() {
        super.flip();
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    // 丢弃备忘，游标归零
    @Override
    public final ShortBuffer rewind() {
        super.rewind();
        return this;
    }
    
    /*▲ 标记操作 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new short buffer whose content is a shared subsequence of
     * this buffer's content.
     *
     * <p> The content of the new buffer will start at this buffer's current
     * position.  Changes to this buffer's content will be visible in the new
     * buffer, and vice versa; the two buffers' position, limit, and mark
     * values will be independent.
     *
     * <p> The new buffer's position will be zero, its capacity and its limit
     * will be the number of shorts remaining in this buffer, its mark will be
     * undefined, and its byte order will be
     *
     * identical to that of this buffer.
     *
     * The new buffer will be direct if, and only if, this buffer is direct, and
     * it will be read-only if, and only if, this buffer is read-only.  </p>
     *
     * @return The new short buffer
     */
    // 切片，截取旧缓冲区的【活跃区域】，作为新缓冲区的【原始区域】。两个缓冲区标记独立
    @Override
    public abstract ShortBuffer slice();
    
    /**
     * Creates a new short buffer that shares this buffer's content.
     *
     * <p> The content of the new buffer will be that of this buffer.  Changes
     * to this buffer's content will be visible in the new buffer, and vice
     * versa; the two buffers' position, limit, and mark values will be
     * independent.
     *
     * <p> The new buffer's capacity, limit, position,
     *
     *
     *
     *
     * mark values, and byte order will be identical to those of this buffer.
     *
     * The new buffer will be direct if, and only if, this buffer is direct, and
     * it will be read-only if, and only if, this buffer is read-only.  </p>
     *
     * @return The new short buffer
     */
    // 副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    @Override
    public abstract ShortBuffer duplicate();
    
    /**
     * Creates a new, read-only short buffer that shares this buffer's
     * content.
     *
     * <p> The content of the new buffer will be that of this buffer.  Changes
     * to this buffer's content will be visible in the new buffer; the new
     * buffer itself, however, will be read-only and will not allow the shared
     * content to be modified.  The two buffers' position, limit, and mark
     * values will be independent.
     *
     * <p> The new buffer's capacity, limit, position,
     *
     *
     *
     *
     * mark values, and byte order will be identical to those of this buffer.
     *
     *
     * <p> If this buffer is itself read-only then this method behaves in
     * exactly the same way as the {@link #duplicate duplicate} method.  </p>
     *
     * @return The new, read-only short buffer
     */
    // 只读副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public abstract ShortBuffer asReadOnlyBuffer();
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Relative <i>get</i> method.  Reads the short at this buffer's
     * current position, and then increments the position.
     *
     * @return The short at the buffer's current position
     *
     * @throws BufferUnderflowException If the buffer's current position is not smaller than its limit
     */
    // 读取position处（可能需要加offset）的short，然后递增position。
    public abstract short get();
    
    /**
     * Absolute <i>get</i> method.  Reads the short at the given
     * index.
     *
     * @param index The index from which the short will be read
     *
     * @return The short at the given index
     *
     * @throws IndexOutOfBoundsException If {@code index} is negative
     *                                   or not smaller than the buffer's limit
     */
    // 读取index处（可能需要加offset）的short（有越界检查）
    public abstract short get(int index);
    
    /**
     * Relative bulk <i>get</i> method.
     *
     * <p> This method transfers shorts from this buffer into the given
     * destination array.  If there are fewer shorts remaining in the
     * buffer than are required to satisfy the request, that is, if
     * {@code length}&nbsp;{@code >}&nbsp;{@code remaining()}, then no
     * shorts are transferred and a {@link BufferUnderflowException} is
     * thrown.
     *
     * <p> Otherwise, this method copies {@code length} shorts from this
     * buffer into the given array, starting at the current position of this
     * buffer and at the given offset in the array.  The position of this
     * buffer is then incremented by {@code length}.
     *
     * <p> In other words, an invocation of this method of the form
     * <code>src.get(dst,&nbsp;off,&nbsp;len)</code> has exactly the same effect as
     * the loop
     *
     * <pre>{@code
     *     for (int i = off; i < off + len; i++)
     *         dst[i] = src.get();
     * }</pre>
     *
     * except that it first checks that there are sufficient shorts in
     * this buffer and it is potentially much more efficient.
     *
     * @param dst    The array into which shorts are to be written
     * @param offset The offset within the array of the first short to be
     *               written; must be non-negative and no larger than
     *               {@code dst.length}
     * @param length The maximum number of shorts to be written to the given
     *               array; must be non-negative and no larger than
     *               {@code dst.length - offset}
     *
     * @return This buffer
     *
     * @throws BufferUnderflowException  If there are fewer than {@code length} shorts
     *                                   remaining in this buffer
     * @throws IndexOutOfBoundsException If the preconditions on the {@code offset} and {@code length}
     *                                   parameters do not hold
     */
    // 复制源缓存区的length个元素到dst数组offset索引处
    public ShortBuffer get(short[] dst, int offset, int length) {
        checkBounds(offset, length, dst.length);
        if(length > remaining())
            throw new BufferUnderflowException();
        int end = offset + length;
        for(int i = offset; i < end; i++)
            dst[i] = get();
        return this;
    }
    
    /**
     * Relative bulk <i>get</i> method.
     *
     * <p> This method transfers shorts from this buffer into the given
     * destination array.  An invocation of this method of the form
     * {@code src.get(a)} behaves in exactly the same way as the invocation
     *
     * <pre>
     *     src.get(a, 0, a.length) </pre>
     *
     * @param dst The destination array
     *
     * @return This buffer
     *
     * @throws BufferUnderflowException If there are fewer than {@code length} shorts
     *                                  remaining in this buffer
     */
    // 复制源缓存区的内容到dst数组，尽量填满dst
    public ShortBuffer get(short[] dst) {
        return get(dst, 0, dst.length);
    }
    
    /*▲ get ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ put ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Relative <i>put</i> method&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> Writes the given short into this buffer at the current
     * position, and then increments the position. </p>
     *
     * @param s The short to be written
     *
     * @return This buffer
     *
     * @throws BufferOverflowException If this buffer's current position is not smaller than its limit
     * @throws ReadOnlyBufferException If this buffer is read-only
     */
    // 向position处（可能需要加offset）写入short，并将position递增
    public abstract ShortBuffer put(short s);
    
    /**
     * Absolute <i>put</i> method&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> Writes the given short into this buffer at the given
     * index. </p>
     *
     * @param index The index at which the short will be written
     * @param s     The short value to be written
     *
     * @return This buffer
     *
     * @throws IndexOutOfBoundsException If {@code index} is negative
     *                                   or not smaller than the buffer's limit
     * @throws ReadOnlyBufferException   If this buffer is read-only
     */
    // 向index处（可能需要加offset）写入short
    public abstract ShortBuffer put(int index, short s);
    
    /**
     * Relative bulk <i>put</i> method&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> This method transfers shorts into this buffer from the given
     * source array.  If there are more shorts to be copied from the array
     * than remain in this buffer, that is, if
     * {@code length}&nbsp;{@code >}&nbsp;{@code remaining()}, then no
     * shorts are transferred and a {@link BufferOverflowException} is
     * thrown.
     *
     * <p> Otherwise, this method copies {@code length} shorts from the
     * given array into this buffer, starting at the given offset in the array
     * and at the current position of this buffer.  The position of this buffer
     * is then incremented by {@code length}.
     *
     * <p> In other words, an invocation of this method of the form
     * <code>dst.put(src,&nbsp;off,&nbsp;len)</code> has exactly the same effect as
     * the loop
     *
     * <pre>{@code
     *     for (int i = off; i < off + len; i++)
     *         dst.put(a[i]);
     * }</pre>
     *
     * except that it first checks that there is sufficient space in this
     * buffer and it is potentially much more efficient.
     *
     * @param src    The array from which shorts are to be read
     * @param offset The offset within the array of the first short to be read;
     *               must be non-negative and no larger than {@code array.length}
     * @param length The number of shorts to be read from the given array;
     *               must be non-negative and no larger than
     *               {@code array.length - offset}
     *
     * @return This buffer
     *
     * @throws BufferOverflowException   If there is insufficient space in this buffer
     * @throws IndexOutOfBoundsException If the preconditions on the {@code offset} and {@code length}
     *                                   parameters do not hold
     * @throws ReadOnlyBufferException   If this buffer is read-only
     */
    // 从源short数组src的offset处开始，复制length个元素，写入到当前缓冲区（具体行为由子类实现）
    public ShortBuffer put(short[] src, int offset, int length) {
        checkBounds(offset, length, src.length);
        if(length > remaining())
            throw new BufferOverflowException();
        int end = offset + length;
        for(int i = offset; i < end; i++)
            this.put(src[i]);
        return this;
    }
    
    /**
     * Relative bulk <i>put</i> method&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> This method transfers the entire content of the given source
     * short array into this buffer.  An invocation of this method of the
     * form {@code dst.put(a)} behaves in exactly the same way as the
     * invocation
     *
     * <pre>
     *     dst.put(a, 0, a.length) </pre>
     *
     * @param src The source array
     *
     * @return This buffer
     *
     * @throws BufferOverflowException If there is insufficient space in this buffer
     * @throws ReadOnlyBufferException If this buffer is read-only
     */
    // 将short数组src的全部内容写入此缓冲区
    public final ShortBuffer put(short[] src) {
        return put(src, 0, src.length);
    }
    
    /**
     * Relative bulk <i>put</i> method&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> This method transfers the shorts remaining in the given source
     * buffer into this buffer.  If there are more shorts remaining in the
     * source buffer than in this buffer, that is, if
     * {@code src.remaining()}&nbsp;{@code >}&nbsp;{@code remaining()},
     * then no shorts are transferred and a {@link
     * BufferOverflowException} is thrown.
     *
     * <p> Otherwise, this method copies
     * <i>n</i>&nbsp;=&nbsp;{@code src.remaining()} shorts from the given
     * buffer into this buffer, starting at each buffer's current position.
     * The positions of both buffers are then incremented by <i>n</i>.
     *
     * <p> In other words, an invocation of this method of the form
     * {@code dst.put(src)} has exactly the same effect as the loop
     *
     * <pre>
     *     while (src.hasRemaining())
     *         dst.put(src.get()); </pre>
     *
     * except that it first checks that there is sufficient space in this
     * buffer and it is potentially much more efficient.
     *
     * @param src The source buffer from which shorts are to be read;
     *            must not be this buffer
     *
     * @return This buffer
     *
     * @throws BufferOverflowException  If there is insufficient space in this buffer
     *                                  for the remaining shorts in the source buffer
     * @throws IllegalArgumentException If the source buffer is this buffer
     * @throws ReadOnlyBufferException  If this buffer is read-only
     */
    // 将源缓冲区src的内容全部写入到当前缓冲区
    public ShortBuffer put(ShortBuffer src) {
        if(src == this)
            throw createSameBufferException();
        if(isReadOnly())
            throw new ReadOnlyBufferException();
        int n = src.remaining();
        if(n > remaining())
            throw new BufferOverflowException();
        for(int i = 0; i < n; i++)
            put(src.get());
        return this;
    }
    
    /*▲ put ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Compacts this buffer&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> The shorts between the buffer's current position and its limit,
     * if any, are copied to the beginning of the buffer.  That is, the
     * short at index <i>p</i>&nbsp;=&nbsp;{@code position()} is copied
     * to index zero, the short at index <i>p</i>&nbsp;+&nbsp;1 is copied
     * to index one, and so forth until the short at index
     * {@code limit()}&nbsp;-&nbsp;1 is copied to index
     * <i>n</i>&nbsp;=&nbsp;{@code limit()}&nbsp;-&nbsp;{@code 1}&nbsp;-&nbsp;<i>p</i>.
     * The buffer's position is then set to <i>n+1</i> and its limit is set to
     * its capacity.  The mark, if defined, is discarded.
     *
     * <p> The buffer's position is set to the number of shorts copied,
     * rather than to zero, so that an invocation of this method can be
     * followed immediately by an invocation of another relative <i>put</i>
     * method. </p>
     *
     * @return This buffer
     *
     * @throws ReadOnlyBufferException If this buffer is read-only
     */
    // 压缩缓冲区，将当前未读完的数据挪到容器起始处，可用于读模式到写模式的切换，但又不丢失之前读入的数据。
    public abstract ShortBuffer compact();
    
    /*▲ 压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Retrieves this buffer's byte order.
     *
     * <p> The byte order of a short buffer created by allocation or by
     * wrapping an existing {@code short} array is the {@link
     * ByteOrder#nativeOrder native order} of the underlying
     * hardware.  The byte order of a short buffer created as a <a
     * href="ByteBuffer.html#views">view</a> of a byte buffer is that of the
     * byte buffer at the moment that the view is created.  </p>
     *
     * @return This buffer's byte order
     */
    // 返回该缓冲区的字节序（大端还是小端）
    public abstract ByteOrder order();
    
    /*▲ 字节顺序 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 比较 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    private static int compare(short x, short y) {
        return Short.compare(x, y);
    }
    
    /**
     * Compares this buffer to another.
     *
     * <p> Two short buffers are compared by comparing their sequences of
     * remaining elements lexicographically, without regard to the starting
     * position of each sequence within its corresponding buffer.
     *
     *
     *
     *
     *
     *
     *
     *
     * Pairs of {@code short} elements are compared as if by invoking
     * {@link Short#compare(short, short)}.
     *
     *
     * <p> A short buffer is not comparable to any other type of object.
     *
     * @return A negative integer, zero, or a positive integer as this buffer
     * is less than, equal to, or greater than the given buffer
     */
    public int compareTo(ShortBuffer that) {
        int i = BufferMismatch.mismatch(this, this.position(), that, that.position(), Math.min(this.remaining(), that.remaining()));
        if(i >= 0) {
            return compare(this.get(this.position() + i), that.get(that.position() + i));
        }
        return this.remaining() - that.remaining();
    }
    
    /**
     * Finds and returns the relative index of the first mismatch between this
     * buffer and a given buffer.  The index is relative to the
     * {@link #position() position} of each buffer and will be in the range of
     * 0 (inclusive) up to the smaller of the {@link #remaining() remaining}
     * elements in each buffer (exclusive).
     *
     * <p> If the two buffers share a common prefix then the returned index is
     * the length of the common prefix and it follows that there is a mismatch
     * between the two buffers at that index within the respective buffers.
     * If one buffer is a proper prefix of the other then the returned index is
     * the smaller of the remaining elements in each buffer, and it follows that
     * the index is only valid for the buffer with the larger number of
     * remaining elements.
     * Otherwise, there is no mismatch.
     *
     * @param that The byte buffer to be tested for a mismatch with this buffer
     *
     * @return The relative index of the first mismatch between this and the
     * given buffer, otherwise -1 if no mismatch.
     *
     * @since 11
     */
    // 快速比较两个缓冲区内容，并返回失配元素的索引。返回-1代表缓冲区内容相同。
    public int mismatch(ShortBuffer that) {
        int length = Math.min(this.remaining(), that.remaining());
        int r = BufferMismatch.mismatch(this, this.position(), that, that.position(), length);
        return (r == -1 && this.remaining() != that.remaining()) ? length : r;
    }
    
    /*▲ 比较 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ Buffer ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tells whether or not this buffer is backed by an accessible short
     * array.
     *
     * <p> If this method returns {@code true} then the {@link #array() array}
     * and {@link #arrayOffset() arrayOffset} methods may safely be invoked.
     * </p>
     *
     * @return {@code true} if, and only if, this buffer
     * is backed by an array and is not read-only
     */
    // true：此buffer由可访问的数组实现
    public final boolean hasArray() {
        return (hb != null) && !isReadOnly;
    }
    
    /**
     * Returns the short array that backs this
     * buffer&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> Modifications to this buffer's content will cause the returned
     * array's content to be modified, and vice versa.
     *
     * <p> Invoke the {@link #hasArray hasArray} method before invoking this
     * method in order to ensure that this buffer has an accessible backing
     * array.  </p>
     *
     * @return The array that backs this buffer
     *
     * @throws ReadOnlyBufferException       If this buffer is backed by an array but is read-only
     * @throws UnsupportedOperationException If this buffer is not backed by an accessible array
     */
    // 返回该buffer内部的非只读数组
    public final short[] array() {
        if(hb == null)
            throw new UnsupportedOperationException();
        if(isReadOnly)
            throw new ReadOnlyBufferException();
        return hb;
    }
    
    // 返回内部存储结构的引用（一般用于非直接缓存区）
    @Override
    Object base() {
        return hb;
    }
    
    /**
     * Returns the offset within this buffer's backing array of the first
     * element of the buffer&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p> If this buffer is backed by an array then buffer position <i>p</i>
     * corresponds to array index <i>p</i>&nbsp;+&nbsp;{@code arrayOffset()}.
     *
     * <p> Invoke the {@link #hasArray hasArray} method before invoking this
     * method in order to ensure that this buffer has an accessible backing
     * array.  </p>
     *
     * @return The offset within this buffer's array
     * of the first element of the buffer
     *
     * @throws ReadOnlyBufferException       If this buffer is backed by an array but is read-only
     * @throws UnsupportedOperationException If this buffer is not backed by an accessible array
     */
    // 返回此缓冲区中的第一个元素在缓冲区的底层实现数组中的偏移量（可选操作）
    public final int arrayOffset() {
        if(hb == null)
            throw new UnsupportedOperationException();
        if(isReadOnly)
            throw new ReadOnlyBufferException();
        return offset;
    }
    
    /*▲ Buffer ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    /**
     * Returns a string summarizing the state of this buffer.
     *
     * @return A summary string
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append("[pos=");
        sb.append(position());
        sb.append(" lim=");
        sb.append(limit());
        sb.append(" cap=");
        sb.append(capacity());
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Tells whether or not this buffer is equal to another object.
     *
     * <p> Two short buffers are equal if, and only if,
     *
     * <ol>
     *
     * <li><p> They have the same element type,  </p></li>
     *
     * <li><p> They have the same number of remaining elements, and
     * </p></li>
     *
     * <li><p> The two sequences of remaining elements, considered
     * independently of their starting positions, are pointwise equal.
     *
     *
     *
     *
     *
     *
     *
     * </p></li>
     *
     * </ol>
     *
     * <p> A short buffer is not equal to any other type of object.  </p>
     *
     * @param ob The object to which this buffer is to be compared
     *
     * @return {@code true} if, and only if, this buffer is equal to the
     * given object
     */
    public boolean equals(Object ob) {
        if(this == ob)
            return true;
        if(!(ob instanceof ShortBuffer))
            return false;
        ShortBuffer that = (ShortBuffer) ob;
        if(this.remaining() != that.remaining())
            return false;
        return BufferMismatch.mismatch(this, this.position(), that, that.position(), this.remaining())<0;
    }
    
    /**
     * Returns the current hash code of this buffer.
     *
     * <p> The hash code of a short buffer depends only upon its remaining
     * elements; that is, upon the elements from {@code position()} up to, and
     * including, the element at {@code limit()}&nbsp;-&nbsp;{@code 1}.
     *
     * <p> Because buffer hash codes are content-dependent, it is inadvisable
     * to use buffers as keys in hash maps or similar data structures unless it
     * is known that their contents will not change.  </p>
     *
     * @return The current hash code of this buffer
     */
    public int hashCode() {
        int h = 1;
        int p = position();
        for(int i = limit() - 1; i >= p; i--)
            
            
            h = 31 * h + (int) get(i);
        
        return h;
    }
    
}
