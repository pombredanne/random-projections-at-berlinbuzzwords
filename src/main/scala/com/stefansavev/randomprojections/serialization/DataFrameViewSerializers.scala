package com.stefansavev.randomprojections.serialization

import com.stefansavev.randomprojections.datarepr.dense._
import com.stefansavev.randomprojections.serialization.String2IdHasherSerialization.String2IdHasherSerializer
import com.stefansavev.randomprojections.serialization.core.Core._
import com.stefansavev.randomprojections.serialization.core.TupleSerializers._
import com.stefansavev.randomprojections.serialization.core.PrimitiveTypeSerializers._
import com.stefansavev.randomprojections.serialization.ColumnHeaderSerialization._
import com.stefansavev.randomprojections.serialization.core.TypedSerializer
import com.stefansavev.randomprojections.utils.String2IdHasher

object DataFrameViewSerializers {

  implicit def valuesStoreSerializer(): TypedSerializer[ValuesStore] = {
    implicit object ValuesStoreAsDoubleSerializationTag extends TypeTag[ValuesStoreAsDouble]{
      def tag: Int = ValuesStoreAsDoubleSerializationTags.valuesStoreAsDouble
    }

    implicit def valuesStoreAsDoubleSerializer(): TypedSerializer[ValuesStoreAsDouble] = {

      implicit def valuesStoreTupleTypeSerializer(): TypedSerializer[ValuesStoreAsDouble.TupleType] = {
        tuple2Serializer[Int, Array[Double]](TypedIntSerializer, TypedDoubleArraySerializer)
      }

      implicit object ValuesStoreIso extends Iso[ValuesStoreAsDouble, ValuesStoreAsDouble.TupleType]{
        def from(input: Input): Output = input.toTuple
        def to(t: Output): Input = ValuesStoreAsDouble.fromTuple(t)
      }

      isoSerializer[ValuesStoreAsDouble, ValuesStoreAsDouble.TupleType](ValuesStoreIso, valuesStoreTupleTypeSerializer())
    }
    subtype1Serializer[ValuesStore, ValuesStoreAsDouble](ValuesStoreAsDoubleSerializationTag, valuesStoreAsDoubleSerializer())
  }

  implicit object DenseRowStoredMatrixViewIso extends Iso[DenseRowStoredMatrixView, DenseRowStoredMatrixView.TupleType]{
    def from(input: Input): Output = input.toTuple
    def to(t: Output): Input = DenseRowStoredMatrixView.fromTuple(t)
  }

  implicit object DenseRowStoredMatrixViewTag extends TypeTag[DenseRowStoredMatrixView]{
    def tag: Int = DenseRowStoredMatrixView.tag
  }

  //this is how to help the compiler
  implicit def denseRowStoredMatrixViewTupleTypeSerializer(): TypedSerializer[DenseRowStoredMatrixView.TupleType] = {
    tuple5Serializer[Int, ValuesStore, Array[Int], ColumnHeader, String2IdHasher](TypedIntSerializer, valuesStoreSerializer(), TypedIntArraySerializer, ColumnHeaderSerializer, String2IdHasherSerializer)
  }

  implicit def denseRowStoredMatrixSerializer(): TypedSerializer[DenseRowStoredMatrixView] = {
    isoSerializer[DenseRowStoredMatrixView, DenseRowStoredMatrixView.TupleType](DenseRowStoredMatrixViewIso, denseRowStoredMatrixViewTupleTypeSerializer())
  }

  implicit def rowStoredMatrixSerializer(): TypedSerializer[RowStoredMatrixView] = {
    subtype1Serializer[RowStoredMatrixView, DenseRowStoredMatrixView](DenseRowStoredMatrixViewTag, denseRowStoredMatrixSerializer())
  }

  implicit object PointIndexesIso extends Iso[PointIndexes, PointIndexes.TupleType]{
    def from(input: Input): Output = input.toTuple
    def to(t: Output): Input = PointIndexes.fromTuple(t)
  }

  implicit def pointIndexesSerializer(): TypedSerializer[PointIndexes] = {
    isoSerializer[PointIndexes, PointIndexes.TupleType]
  }

  implicit object DataFrameViewIso extends Iso[DataFrameView, DataFrameView.TupleType] {
    def from(input: Input): Output = input.toTuple
    def to(t: Output): Input = DataFrameView.fromTuple(t)
  }

  implicit def dataFrameViewTupleSerializer(): TypedSerializer[DataFrameView.TupleType] = {
    tuple2Serializer[PointIndexes, RowStoredMatrixView](pointIndexesSerializer, rowStoredMatrixSerializer)
  }

  implicit def dataFrameSerializer(): TypedSerializer[DataFrameView] = {
    isoSerializer[DataFrameView, DataFrameView.TupleType](DataFrameViewIso, dataFrameViewTupleSerializer())
  }

}
