/*
 * Copyright (C) 2012 The Regents of The University California.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package shark.memstore2.column

import java.nio.ByteBuffer
import java.nio.ByteOrder

import it.unimi.dsi.fastutil.booleans.BooleanArrayList

import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector
import org.apache.hadoop.hive.serde2.objectinspector.primitive.BooleanObjectInspector


class BooleanColumnBuilder extends ColumnBuilder[Boolean] {

  private var _stats: ColumnStats.BooleanColumnStats = null
  private var _nonNulls: BooleanArrayList = null

  override def initialize(initialSize: Int) {
    _nonNulls = new BooleanArrayList(initialSize)
    _stats = new ColumnStats.BooleanColumnStats
    super.initialize(initialSize)
  }

  override def append(o: Object, oi: ObjectInspector) {
    if (o == null) {
      appendNull()
    } else {
      val v = oi.asInstanceOf[BooleanObjectInspector].get(o)
      append(v)
    }
  }

  override def append(v: Boolean) {
    _nonNulls.add(v)
    _stats.append(v)
  }

  override def appendNull() {
    _nullBitmap.set(_nonNulls.size + _stats.nullCount)
    _stats.appendNull()
  }

  override def stats = _stats

  override def build: ByteBuffer = {
    val buf = ByteBuffer.allocate(_nonNulls.size + ColumnIterator.COLUMN_TYPE_LENGTH + sizeOfNullBitmap)
    buf.order(ByteOrder.nativeOrder())
    buf.putLong(ColumnIterator.BOOLEAN)

    writeNullBitmap(buf)

    var i = 0
    while (i < _nonNulls.size) {
      buf.put(if (_nonNulls.get(i)) 1.toByte else 0.toByte)
      i += 1
    }
    buf.rewind()
    buf
  }
}
