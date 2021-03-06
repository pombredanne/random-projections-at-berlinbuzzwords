package com.stefansavev.randomprojections.implementation

import java.util.Random

import com.stefansavev.randomprojections.datarepr.dense.DataFrameView
import com.stefansavev.randomprojections.datarepr.sparse.SparseVector

class OnlineVariance(k: Int) {
  var n = 0.0
  val mean = Array.ofDim[Double](k)

  val sqrLeft = Array.ofDim[Double](k)
  val cntsLeft = Array.ofDim[Double](k)

  val sqrRight = Array.ofDim[Double](k)
  val cntsRight = Array.ofDim[Double](k)

  val M2 = Array.ofDim[Double](k)

  val delta = Array.ofDim[Double](k)

  def processPoint(point: Array[Double]): Unit = {
    val x = point
    n = n + 1.0
    var j = 0
    while (j < k) {
      delta(j) = x(j) - mean(j)
      mean(j) = mean(j) + delta(j) / n
      M2(j) = M2(j) + delta(j) * (x(j) - mean(j))

      if (x(j) > 0) {
        sqrLeft(j) += x(j) * x(j)
        cntsLeft(j) += 1
      }
      else {
        sqrRight(j) += x(j) * x(j)
        cntsRight(j) += 1
      }

      j += 1
    }
  }

  def getMeanAndVar(): (Array[Double], Array[Double]) = {
    var j = 0
    while (j < k) {
      M2(j) = M2(j) / (n - 1.0) //M2 is now the variance
      j += 1
    }
    (mean, M2)
  }

  def diffSqr(): Array[Double] = {
    var j = 0
    val v = Array.ofDim[Double](k)
    while (j < k) {
      val a = sqrLeft(j) / (cntsLeft(j) + 0.5)
      val b = sqrRight(j) / (cntsRight(j) + 0.5)
      v(j) = Math.sqrt(a * b)
      j += 1
    }
    v
  }
}

case class DataInformedProjectionStrategy(rnd: Random, numCols: Int) extends ProjectionStrategy {
  def nextRandomProjection(depth: Int, view: DataFrameView, prevProjection: AbstractProjectionVector): AbstractProjectionVector = {
    val indexes = view.indexes.indexes

    val proj = Array.ofDim[Double](view.numCols)

    val replacements = new scala.collection.mutable.HashMap[Int, Int]()
    //TODO: must guarantee that for different trees we end up with different vectors
    //sample numSamples(256) vectors without replacement
    //if the vectors are a b c d e f, compute a + b - c + d - e + f
    val numSamples = Math.min(256, indexes.length - 1)
    var j = 0
    while (j < numSamples) {
      var b = rnd.nextInt(indexes.length - j)
      if (replacements.contains(b)) {
        b = replacements(b)
      }
      replacements += ((b, indexes.length - j - 1))
      val pnt = view.getPointAsDenseVector(b)
      val sign = (if (j % 2 == 0) 1.0 else -1.0)
      var i = 0
      while (i < pnt.length) {
        proj(i) += sign * pnt(i)
        i += 1
      }
      j += 1
    }

    var norm = 0.0
    var i = 0
    while (i < proj.length) {
      norm += proj(i) * proj(i)
      i += 1
    }
    norm = Math.sqrt(norm + 0.001)
    i = 0
    while (i < proj.length) {
      proj(i) /= norm
      i += 1
    }
    val randomVector = new SparseVector(numCols, Array.range(0, numCols), proj)
    new HadamardProjectionVector(randomVector)
  }
}

case class DataInformedProjectionSettings()

class DataInformedProjectionBuilder(builderSettings: DataInformedProjectionSettings) extends ProjectionStrategyBuilder {
  type T = DataInformedProjectionStrategy
  val splitStrategy: DatasetSplitStrategy = new DataInformedSplitStrategy()

  def build(settings: IndexSettings, rnd: Random, dataFrameView: DataFrameView): T = DataInformedProjectionStrategy(rnd, dataFrameView.numCols)

  def datasetSplitStrategy: DatasetSplitStrategy = splitStrategy
}
