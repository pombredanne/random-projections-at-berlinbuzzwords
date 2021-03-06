package com.stefansavev.randomprojections.datarepr.dense.store

import com.stefansavev.core.serialization.DoubleArraySerializer
import com.stefansavev.randomprojections.buffers.DoubleArrayBuffer
import com.stefansavev.randomprojections.datarepr.sparse.SparseVector
import com.stefansavev.randomprojections.utils.Utils

object ValuesStoreAsDouble {
  type TupleType = (Int, Array[Double])

  def fromTuple(t: TupleType): ValuesStoreAsDouble = {
    val (numCols, data) = t
    new ValuesStoreAsDouble(numCols, data)
  }
}

class ValuesStoreAsDouble(val _numCols: Int, val data: Array[Double]) extends ValuesStore {

  def toTuple(): ValuesStoreAsDouble.TupleType = {
    (_numCols, data)
  }

  def getBuilderType: StoreBuilderType = StoreBuilderAsDoubleType

  def fillRow(rowId: Int, output: Array[Double], isPos: Boolean): Unit = {
    var offset = rowId * _numCols
    var j = 0
    while (j < _numCols) {
      if (isPos) {
        output(j) += data(offset)
      } else {
        output(j) -= data(offset)
      }
      offset += 1
      j += 1
    }
  }

  def setRow(rowId: Int, input: Array[Double]): Unit = {
    var offset = rowId * _numCols
    var j = 0
    while (j < _numCols) {
      data(offset) = input(j)
      offset += 1
      j += 1
    }
  }


  def fillRow(rowId: Int, columnIds: Array[Int], output: Array[Double]): Unit = {
    val offset = rowId * _numCols
    var j = 0
    while (j < columnIds.length) {
      val columnId = columnIds(j)
      output(columnId) = data(offset + columnId)
      j += 1
    }
  }

  override def multiplyRowComponentWiseBySparseVector(rowId: Int, sv: SparseVector, output: Array[Double]): Unit = {
    val offset = rowId * _numCols
    val columnIds = sv.ids
    val values = sv.values
    val dim = columnIds.length
    var j = 0
    while (j < dim) {
      val columnId = columnIds(j)
      val value = values(j)
      output(j) = data(offset + columnId) * value
      j += 1
    }
  }

  def cosineForNormalizedData(query: Array[Double], id: Int): Double = {
    var offset = id * _numCols
    var j = 0
    var sum = 0.0
    while (j < _numCols) {
      val v1 = query(j)
      val v2 = data(offset)
      val d = v1 - v2
      sum += d * d
      offset += 1
      j += 1
    }
    1.0 - 0.5 * sum
  }
}

class ValuesStoreBuilderAsDouble(numCols: Int) extends ValuesStoreBuilder {
  val numValuesInPage = 1024
  val valuesBuffer = new DoubleArrayBuffer()
  var currentRow = 0

  def addValues(values: Array[Double]): Unit = {
    if (values.length != numCols) {
      Utils.internalError()
    }
    valuesBuffer ++= values
    currentRow += 1
  }

  def isFull: Boolean = (currentRow >= numValuesInPage)

  def getContents: Array[Byte] = {
    val output = Array.ofDim[Byte](8*currentRow)
    DoubleArraySerializer.writeToBufferWithoutArrayLength(valuesBuffer.toArray(), output)
    output
  }

  def getCurrentRowIndex = currentRow

  def build(): ValuesStore = {
    val values = valuesBuffer.toArray()
    new ValuesStoreAsDouble(numCols, values)
  }

  def merge(numTotalRows: Int, valueStores: Iterator[ValuesStore]): ValuesStore = {
    val buffer = new FixedLengthBuffer[Double](numTotalRows * numCols)
    for (store <- valueStores) {
      val typedStore = store.asInstanceOf[ValuesStoreAsDouble]
      buffer ++= typedStore.data
      if (typedStore._numCols != numCols) {
        Utils.internalError()
      }
    }
    new ValuesStoreAsDouble(numCols, buffer.array)
  }
}