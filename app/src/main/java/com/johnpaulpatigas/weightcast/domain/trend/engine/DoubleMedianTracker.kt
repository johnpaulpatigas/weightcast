package com.johnpaulpatigas.weightcast.domain.trend.engine

/**
 * Primitive, high-performance dual-heap tracker for calculating exact medians of stream values.
 *
 * Maintains a max-heap for the lower half of values and a min-heap for the upper half.
 * Operations:
 * - Insert: O(log N)
 * - Median Query: O(1)
 */
internal class DoubleMedianTracker(initialCapacity: Int = 128) {
    private var maxHeap = DoubleArray(initialCapacity)
    private var maxHeapSize = 0

    private var minHeap = DoubleArray(initialCapacity)
    private var minHeapSize = 0

    val size: Int get() = maxHeapSize + minHeapSize

    fun clear() {
        maxHeapSize = 0
        minHeapSize = 0
    }

    fun add(value: Double) {
        if (value.isNaN() || value.isInfinite()) return

        if (maxHeapSize == 0 || value <= maxHeap[0]) {
            maxHeapInsert(value)
        } else {
            minHeapInsert(value)
        }
        rebalance()
    }

    private fun rebalance() {
        if (maxHeapSize > minHeapSize + 1) {
            val top = maxHeapPop()
            minHeapInsert(top)
        } else if (minHeapSize > maxHeapSize) {
            val top = minHeapPop()
            maxHeapInsert(top)
        }
    }

    fun getMedian(): Double {
        val total = size
        require(total > 0) { "Cannot calculate median of empty tracker" }
        return if (total % 2 == 1) {
            maxHeap[0]
        } else {
            (maxHeap[0] + minHeap[0]) / 2.0
        }
    }

    private fun maxHeapInsert(value: Double) {
        if (maxHeapSize == maxHeap.size) {
            maxHeap = maxHeap.copyOf(maxHeap.size * 2)
        }
        var i = maxHeapSize++
        maxHeap[i] = value
        while (i > 0) {
            val parent = (i - 1) shr 1
            if (maxHeap[i] > maxHeap[parent]) {
                val tmp = maxHeap[i]
                maxHeap[i] = maxHeap[parent]
                maxHeap[parent] = tmp
                i = parent
            } else {
                break
            }
        }
    }

    private fun maxHeapPop(): Double {
        val root = maxHeap[0]
        maxHeapSize--
        if (maxHeapSize > 0) {
            maxHeap[0] = maxHeap[maxHeapSize]
            maxHeapify(0)
        }
        return root
    }

    private fun maxHeapify(index: Int) {
        var i = index
        while (true) {
            var largest = i
            val left = (i shl 1) + 1
            val right = left + 1
            if (left < maxHeapSize && maxHeap[left] > maxHeap[largest]) largest = left
            if (right < maxHeapSize && maxHeap[right] > maxHeap[largest]) largest = right
            if (largest != i) {
                val tmp = maxHeap[i]
                maxHeap[i] = maxHeap[largest]
                maxHeap[largest] = tmp
                i = largest
            } else {
                break
            }
        }
    }

    private fun minHeapInsert(value: Double) {
        if (minHeapSize == minHeap.size) {
            minHeap = minHeap.copyOf(minHeap.size * 2)
        }
        var i = minHeapSize++
        minHeap[i] = value
        while (i > 0) {
            val parent = (i - 1) shr 1
            if (minHeap[i] < minHeap[parent]) {
                val tmp = minHeap[i]
                minHeap[i] = minHeap[parent]
                minHeap[parent] = tmp
                i = parent
            } else {
                break
            }
        }
    }

    private fun minHeapPop(): Double {
        val root = minHeap[0]
        minHeapSize--
        if (minHeapSize > 0) {
            minHeap[0] = minHeap[minHeapSize]
            minHeapify(0)
        }
        return root
    }

    private fun minHeapify(index: Int) {
        var i = index
        while (true) {
            var smallest = i
            val left = (i shl 1) + 1
            val right = left + 1
            if (left < minHeapSize && minHeap[left] < minHeap[smallest]) smallest = left
            if (right < minHeapSize && minHeap[right] < minHeap[smallest]) smallest = right
            if (smallest != i) {
                val tmp = minHeap[i]
                minHeap[i] = minHeap[smallest]
                minHeap[smallest] = tmp
                i = smallest
            } else {
                break
            }
        }
    }
}
