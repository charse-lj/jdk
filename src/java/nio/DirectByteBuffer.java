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

package java.nio;

import jdk.internal.misc.VM;
import jdk.internal.ref.Cleaner;
import sun.nio.ch.DirectBuffer;

import java.io.FileDescriptor;
import java.lang.ref.Reference;

// 可读写、直接缓冲区，内部存储结构为本地内存块
class DirectByteBuffer extends MappedByteBuffer implements DirectBuffer {
    
    /** Cached unaligned-access capability */
    protected static final boolean UNALIGNED = Bits.unaligned();
    /** Cached array base offset */
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    
    /**
     * Base address, used in all indexing calculations
     * NOTE: moved up to Buffer.java for speed in JNI GetDirectBufferAddress
     * protected long address;
     * An object attached to this buffer. If this buffer is a view of another
     * buffer then we use this field to keep a reference to that buffer to
     * ensure that its memory isn't freed before we are done with it.
     */
    private final Object att;
    
    // 该缓冲区的清理器
    private final Cleaner cleaner;
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // For memory-mapped buffers -- invoked by FileChannelImpl via reflection
    protected DirectByteBuffer(int cap, long addr, FileDescriptor fd, Runnable unmapper) {
        super(-1, 0, cap, cap, fd);
        address = addr;
        cleaner = Cleaner.create(this, unmapper);
        att = null;
    }
    
    // 分配直接内存的构造器
    DirectByteBuffer(int cap) {
        super(-1, 0, cap, cap);
        
        // 内存是否按页分配对齐
        boolean pa = VM.isDirectMemoryPageAligned();
        // 获取每页的大小
        int ps = Bits.pageSize();
        // 如果是按页对齐的，多分配一页容量（因为后续可能需要按页对齐）
        // size = cap或者 cap+ps --> 表示系统中保存总分配内存（按页分配）的大小
        long size = Math.max(1L, (long) cap + (pa ? ps : 0));
        
        // 尽量保障有足够多内存可以使用
        Bits.reserveMemory(size, cap);
        
        long base = 0;
        try {
            base = UNSAFE.allocateMemory(size); //分配本地（直接）内存，base表示直接内存的起始地址
        } catch(OutOfMemoryError x) {
            // 内存不够，则取消之前设置的内容参数
            Bits.unreserveMemory(size, cap);
            throw x;
        }
        
        // 为申请的内存批量填充0值
        UNSAFE.setMemory(base, size, (byte) 0);
        
        // 要求地址按页向上对齐
        if(pa && (base % ps != 0)) {
            // Round up to page boundary
            address = base + ps - (base & (ps - 1)); //base + ps - (base & (ps - 1)) = ps
        } else {
            //地址刚好是pageSize的倍数
            address = base;
        }
        
        // 用清理器追踪此对象，当引用失效时，需要回收内存
        cleaner = Cleaner.create(this, new Deallocator(base, size, cap));
        
        att = null;
    }
    
    // Invoked to construct a direct ByteBuffer referring to the block of memory.
    // A given arbitrary object may also be attached to the buffer.
    DirectByteBuffer(long addr, int cap, Object ob) {
        super(-1, 0, cap, cap);
        address = addr;
        cleaner = null;
        att = ob;
    }
    
    // For duplicates and slices
    DirectByteBuffer(DirectBuffer db, int mark, int pos, int lim, int cap, int off) {
        super(mark, pos, lim, cap);
        address = db.address() + off;
        cleaner = null;
        att = db;
    }
    
    // Invoked only by JNI: NewDirectByteBuffer(void*, long)
    private DirectByteBuffer(long addr, int cap) {
        super(-1, 0, cap, cap);
        address = addr;
        cleaner = null;
        att = null;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 可读写/直接 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 只读/可读写
    public boolean isReadOnly() {
        return false;
    }
    
    // 直接缓冲区/非直接缓冲区
    public boolean isDirect() {
        return true;
    }
    
    /*▲ 可读写/直接 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 切片，截取旧缓冲区的【活跃区域】，作为新缓冲区的【原始区域】。两个缓冲区标记独立
    public ByteBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        int off = (pos << 0);
        assert (off >= 0);
        return new DirectByteBuffer(this, -1, 0, rem, rem, off);
    }
    
    // 切片，截取旧缓冲区【活跃区域】中pos~lim中的一段，作为新缓冲区的【原始区域】。两个缓冲区标记独立
    public ByteBuffer slice(int pos, int lim) {
        assert (pos >= 0);
        assert (pos <= lim);
        int rem = lim - pos;
        return new DirectByteBuffer(this, -1, 0, rem, rem, pos);
    }
    
    // 副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public ByteBuffer duplicate() {
        return new DirectByteBuffer(this, this.markValue(), this.position(), this.limit(), this.capacity(), 0);
    }
    
    // 只读副本，新缓冲区共享旧缓冲区的【原始区域】，且新旧缓冲区【活跃区域】一致。两个缓冲区标记独立。
    public ByteBuffer asReadOnlyBuffer() {
        return new DirectByteBufferR(this, this.markValue(), this.position(), this.limit(), this.capacity(), 0);
    }
    
    /*▲ 创建新缓冲区，新旧缓冲区共享内部的存储容器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ get ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 读取position处（可能需要加offset）的byte，然后递增position。
    public byte get() {
        try {
            return ((UNSAFE.getByte(ix(nextGetIndex()))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 读取i处（可能需要加offset）的byte（有越界检查）
    public byte get(int i) {
        try {
            return ((UNSAFE.getByte(ix(checkIndex(i)))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 复制源缓存区的length个元素到dst数组offset索引处
    public ByteBuffer get(byte[] dst, int offset, int length) {
        if(((long) length << 0) > Bits.JNI_COPY_TO_ARRAY_THRESHOLD) {
            checkBounds(offset, length, dst.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if(length > rem)
                throw new BufferUnderflowException();
            
            long dstOffset = ARRAY_BASE_OFFSET + ((long) offset << 0);
            try {
                
                
                UNSAFE.copyMemory(null, ix(pos), dst, dstOffset, (long) length << 0);
            } finally {
                Reference.reachabilityFence(this);
            }
            position(pos + length);
        } else {
            super.get(dst, offset, length);
        }
        return this;
    }
    
    // 一次读2个字节，按char解析，将position增加2个单位
    public char getChar() {
        try {
            return getChar(ix(nextGetIndex((1 << 1))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 读取i处2个字节解析为char（有越界检查）
    public char getChar(int i) {
        try {
            return getChar(ix(checkIndex(i, (1 << 1))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 一次读2个字节，按short解析，将position增加2个单位
    public short getShort() {
        try {
            return getShort(ix(nextGetIndex((1 << 1))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 读取i处2个字节解析为short（有越界检查）
    public short getShort(int i) {
        try {
            return getShort(ix(checkIndex(i, (1 << 1))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 一次读4个字节，按int解析，将position增加4个单位
    public int getInt() {
        try {
            return getInt(ix(nextGetIndex((1 << 2))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 读取i处4个字节解析为int（有越界检查）
    public int getInt(int i) {
        try {
            return getInt(ix(checkIndex(i, (1 << 2))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 一次读8个字节，按long解析，将position增加8个单位
    public long getLong() {
        try {
            return getLong(ix(nextGetIndex((1 << 3))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 读取i处8个字节解析为long（有越界检查）
    public long getLong(int i) {
        try {
            return getLong(ix(checkIndex(i, (1 << 3))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 一次读4个字节，按float解析，将position增加4个单位
    public float getFloat() {
        try {
            return getFloat(ix(nextGetIndex((1 << 2))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 读取i处4个字节解析为float（有越界检查）
    public float getFloat(int i) {
        try {
            return getFloat(ix(checkIndex(i, (1 << 2))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 一次读8个字节，按double解析，将position增加8个单位
    public double getDouble() {
        try {
            return getDouble(ix(nextGetIndex((1 << 3))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    // 读取i处8个字节解析为double（有越界检查）
    public double getDouble(int i) {
        try {
            return getDouble(ix(checkIndex(i, (1 << 3))));
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    
    private char getChar(long a) {
        try {
            char x = UNSAFE.getCharUnaligned(null, a, bigEndian);
            return (x);
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    private short getShort(long a) {
        try {
            short x = UNSAFE.getShortUnaligned(null, a, bigEndian);
            return (x);
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    private int getInt(long a) {
        try {
            int x = UNSAFE.getIntUnaligned(null, a, bigEndian);
            return (x);
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    private long getLong(long a) {
        try {
            long x = UNSAFE.getLongUnaligned(null, a, bigEndian);
            return (x);
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    private float getFloat(long a) {
        try {
            int x = UNSAFE.getIntUnaligned(null, a, bigEndian);
            return Float.intBitsToFloat(x);
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    private double getDouble(long a) {
        try {
            long x = UNSAFE.getLongUnaligned(null, a, bigEndian);
            return Double.longBitsToDouble(x);
        } finally {
            Reference.reachabilityFence(this);
        }
    }
    
    /*▲ get ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ put ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 向position处（可能需要加offset）写入byte，并将position递增
    public ByteBuffer put(byte x) {
        try {
            UNSAFE.putByte(ix(nextPutIndex()), ((x)));
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;
    }
    
    // 向i处（可能需要加offset）写入byte
    public ByteBuffer put(int i, byte x) {
        try {
            UNSAFE.putByte(ix(checkIndex(i)), ((x)));
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;
    }
    
    // 将源缓冲区src的内容全部写入到当前缓冲区
    public ByteBuffer put(ByteBuffer src) {
        if(src instanceof DirectByteBuffer) {
            if(src == this)
                throw createSameBufferException();
            DirectByteBuffer sb = (DirectByteBuffer) src;
            
            int spos = sb.position();
            int slim = sb.limit();
            assert (spos <= slim);
            int srem = (spos <= slim ? slim - spos : 0);
            
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            
            if(srem > rem)
                throw new BufferOverflowException();
            try {
                UNSAFE.copyMemory(sb.ix(spos), ix(pos), (long) srem << 0);
            } finally {
                Reference.reachabilityFence(sb);
                Reference.reachabilityFence(this);
            }
            sb.position(spos + srem);
            position(pos + srem);
        } else if(src.hb != null) {
            
            int spos = src.position();
            int slim = src.limit();
            assert (spos <= slim);
            int srem = (spos <= slim ? slim - spos : 0);
            
            put(src.hb, src.offset + spos, srem);
            src.position(spos + srem);
            
        } else {
            super.put(src);
        }
        return this;
    }
    
    // 从源字节数组src的offset处开始，复制length个元素，写入到当前缓冲区
    public ByteBuffer put(byte[] src, int offset, int length) {
        if(((long) length << 0) > Bits.JNI_COPY_FROM_ARRAY_THRESHOLD) {
            checkBounds(offset, length, src.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if(length > rem)
                throw new BufferOverflowException();
            
            long srcOffset = ARRAY_BASE_OFFSET + ((long) offset << 0);
            try {
                UNSAFE.copyMemory(src, srcOffset, null, ix(pos), (long) length << 0);
            } finally {
                Reference.reachabilityFence(this);
            }
            position(pos + length);
        } else {
            super.put(src, offset, length);
        }
        return this;
    }
    
    // 将char转为byte存入缓冲区，将position增加2个单位
    public ByteBuffer putChar(char x) {
        putChar(ix(nextPutIndex((1 << 1))), x);
        return this;
    }
    
    // 将char转为byte存入缓冲区索引i处
    public ByteBuffer putChar(int i, char x) {
        putChar(ix(checkIndex(i, (1 << 1))), x);
        return this;
    }
    
    // 将short转为byte存入缓冲区，将position增加2个单位
    public ByteBuffer putShort(short x) {
        putShort(ix(nextPutIndex((1 << 1))), x);
        return this;
    }
    
    // 将short转为byte存入缓冲区索引i处
    public ByteBuffer putShort(int i, short x) {
        putShort(ix(checkIndex(i, (1 << 1))), x);
        return this;
    }
    
    // 将int转为byte存入缓冲区，将position增加4个单位
    public ByteBuffer putInt(int x) {
        putInt(ix(nextPutIndex((1 << 2))), x);
        return this;
    }
    
    // 将int转为byte存入缓冲区索引i处
    public ByteBuffer putInt(int i, int x) {
        putInt(ix(checkIndex(i, (1 << 2))), x);
        return this;
    }
    
    // 将long转为byte存入缓冲区，将position增加8个单位
    public ByteBuffer putLong(long x) {
        putLong(ix(nextPutIndex((1 << 3))), x);
        return this;
    }
    
    // 将long转为byte存入缓冲区索引i处
    public ByteBuffer putLong(int i, long x) {
        putLong(ix(checkIndex(i, (1 << 3))), x);
        return this;
    }
    
    // 将float转为byte存入缓冲区，将position增加4个单位
    public ByteBuffer putFloat(float x) {
        putFloat(ix(nextPutIndex((1 << 2))), x);
        return this;
    }
    
    // 将float转为byte存入缓冲区索引i处
    public ByteBuffer putFloat(int i, float x) {
        putFloat(ix(checkIndex(i, (1 << 2))), x);
        return this;
    }
    
    // 将double转为byte存入缓冲区，将position增加8个单位
    public ByteBuffer putDouble(double x) {
        putDouble(ix(nextPutIndex((1 << 3))), x);
        return this;
    }
    
    // 将double转为byte存入缓冲区索引i处
    public ByteBuffer putDouble(int i, double x) {
        putDouble(ix(checkIndex(i, (1 << 3))), x);
        return this;
    }
    
    
    private ByteBuffer putChar(long a, char x) {
        try {
            char y = (x);
            UNSAFE.putCharUnaligned(null, a, y, bigEndian);
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;
    }
    
    private ByteBuffer putShort(long a, short x) {
        try {
            short y = (x);
            UNSAFE.putShortUnaligned(null, a, y, bigEndian);
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;
    }
    
    private ByteBuffer putInt(long a, int x) {
        try {
            int y = (x);
            UNSAFE.putIntUnaligned(null, a, y, bigEndian);
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;
    }
    
    private ByteBuffer putLong(long a, long x) {
        
        try {
            long y = (x);
            UNSAFE.putLongUnaligned(null, a, y, bigEndian);
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;
    }
    
    private ByteBuffer putFloat(long a, float x) {
        try {
            int y = Float.floatToRawIntBits(x);
            UNSAFE.putIntUnaligned(null, a, y, bigEndian);
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;
    }
    
    private ByteBuffer putDouble(long a, double x) {
        try {
            long y = Double.doubleToRawLongBits(x);
            UNSAFE.putLongUnaligned(null, a, y, bigEndian);
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;
    }
    
    /*▲ put ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ asXXXBuffer ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // ByteBuffer转为CharBuffer
    public CharBuffer asCharBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        
        int size = rem >> 1;
        if(!UNALIGNED && ((address + off) % (1 << 1) != 0)) {
            return (bigEndian ? (CharBuffer) (new ByteBufferAsCharBufferB(this, -1, 0, size, size, address + off)) : (CharBuffer) (new ByteBufferAsCharBufferL(this, -1, 0, size, size, address + off)));
        } else {
            return (nativeByteOrder ? (CharBuffer) (new DirectCharBufferU(this, -1, 0, size, size, off)) : (CharBuffer) (new DirectCharBufferS(this, -1, 0, size, size, off)));
        }
    }
    
    // ByteBuffer转为ShortBuffer
    public ShortBuffer asShortBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        
        int size = rem >> 1;
        if(!UNALIGNED && ((address + off) % (1 << 1) != 0)) {
            return (bigEndian ? (ShortBuffer) (new ByteBufferAsShortBufferB(this, -1, 0, size, size, address + off)) : (ShortBuffer) (new ByteBufferAsShortBufferL(this, -1, 0, size, size, address + off)));
        } else {
            return (nativeByteOrder ? (ShortBuffer) (new DirectShortBufferU(this, -1, 0, size, size, off)) : (ShortBuffer) (new DirectShortBufferS(this, -1, 0, size, size, off)));
        }
    }
    
    // ByteBuffer转为IntBuffer
    public IntBuffer asIntBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        
        int size = rem >> 2;
        if(!UNALIGNED && ((address + off) % (1 << 2) != 0)) {
            return (bigEndian ? (IntBuffer) (new ByteBufferAsIntBufferB(this, -1, 0, size, size, address + off)) : (IntBuffer) (new ByteBufferAsIntBufferL(this, -1, 0, size, size, address + off)));
        } else {
            return (nativeByteOrder ? (IntBuffer) (new DirectIntBufferU(this, -1, 0, size, size, off)) : (IntBuffer) (new DirectIntBufferS(this, -1, 0, size, size, off)));
        }
    }
    
    // ByteBuffer转为LongBuffer
    public LongBuffer asLongBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        
        int size = rem >> 3;
        if(!UNALIGNED && ((address + off) % (1 << 3) != 0)) {
            return (bigEndian ? (LongBuffer) (new ByteBufferAsLongBufferB(this, -1, 0, size, size, address + off)) : (LongBuffer) (new ByteBufferAsLongBufferL(this, -1, 0, size, size, address + off)));
        } else {
            return (nativeByteOrder ? (LongBuffer) (new DirectLongBufferU(this, -1, 0, size, size, off)) : (LongBuffer) (new DirectLongBufferS(this, -1, 0, size, size, off)));
        }
    }
    
    // ByteBuffer转为FloatBuffer
    public FloatBuffer asFloatBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        
        int size = rem >> 2;
        if(!UNALIGNED && ((address + off) % (1 << 2) != 0)) {
            return (bigEndian ? (FloatBuffer) (new ByteBufferAsFloatBufferB(this, -1, 0, size, size, address + off)) : (FloatBuffer) (new ByteBufferAsFloatBufferL(this, -1, 0, size, size, address + off)));
        } else {
            return (nativeByteOrder ? (FloatBuffer) (new DirectFloatBufferU(this, -1, 0, size, size, off)) : (FloatBuffer) (new DirectFloatBufferS(this, -1, 0, size, size, off)));
        }
    }
    
    // ByteBuffer转为DoubleBuffer
    public DoubleBuffer asDoubleBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);
        
        int size = rem >> 3;
        if(!UNALIGNED && ((address + off) % (1 << 3) != 0)) {
            return (bigEndian ? (DoubleBuffer) (new ByteBufferAsDoubleBufferB(this, -1, 0, size, size, address + off)) : (DoubleBuffer) (new ByteBufferAsDoubleBufferL(this, -1, 0, size, size, address + off)));
        } else {
            return (nativeByteOrder ? (DoubleBuffer) (new DirectDoubleBufferU(this, -1, 0, size, size, off)) : (DoubleBuffer) (new DirectDoubleBufferS(this, -1, 0, size, size, off)));
        }
    }
    
    /*▲ asXXXBuffer ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 压缩 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 压缩缓冲区，将当前未读完的数据挪到容器起始处，可用于读模式到写模式的切换，但又不丢失之前读入的数据。
    public ByteBuffer compact() {
        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        try {
            UNSAFE.copyMemory(ix(pos), ix(0), (long) rem << 0);
        } finally {
            Reference.reachabilityFence(this);
        }
        position(rem);
        limit(capacity());
        discardMark();
        return this;
    }
    
    /*▲ 压缩 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 返回内部存储结构的引用（一般用于非直接缓存区）
    @Override
    Object base() {
        return null;
    }
    
    private long ix(int i) {
        return address + ((long) i << 0);
    }
    
    
    
    /*▼ 实现DirectBuffer接口 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回直接缓冲区的【绝对】起始<地址>
    public long address() {
        return address;
    }
    
    // 返回附件，一般是指母体缓冲区的引用
    public Object attachment() {
        return att;
    }
    
    // 返回该缓冲区的清理器
    public Cleaner cleaner() {
        return cleaner;
    }
    
    /*▲ 实现DirectBuffer接口 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    // 释放分配的本地内存，用于清理器的清理动作
    private static class Deallocator implements Runnable {
        private long address;
        private long size;
        private int capacity;
        
        private Deallocator(long address, long size, int capacity) {
            assert (address != 0);
            this.address = address;
            this.size = size;
            this.capacity = capacity;
        }
        
        public void run() {
            if(address == 0) {
                return; // Paranoia
            }
            // 释放allocateMemory或reallocateMemory申请的内存
            UNSAFE.freeMemory(address);
            address = 0;
            Bits.unreserveMemory(size, capacity);
        }
    }
    
}
