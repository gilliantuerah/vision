package com.example.visionapp.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.example.visionapp.MainActivity
import com.example.visionapp.api.Service
import com.example.visionapp.api.datatype.ResultAnnotation
import com.example.visionapp.env.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.examples.detection.tflite.Classifier.Recognition
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


open class ObjectDetectorHelper(
    private var threshold: Float = 0.2f,
    private var numThreads: Int = 1,
    private var currentDelegate: Int = 0,
    private val inputSize: Int = Constants.INPUT_SIZE,
    modelName: String = MainActivity().modelName,
    private val labels: List<String>,
    private val context: Context,
    private val objectDetectorListener: DetectorListener?
) {

    // For this example this needs to be a var so it can be reset on changes. If the ObjectDetector
    // will not change, a lazy val would be preferable.
    private var serviceApi: Service = Service()
    private var resultOffline: ArrayList<Recognition>? = ArrayList<Recognition>()
    private var detectionFinish: Boolean = true

    init {
        setupYoloV5Classifier(modelName)
    }

    fun detect(image: Bitmap, modelInUse: Int) {
        if (modelInUse == 0) {
            if(detectionFinish) {
                CoroutineScope(Default).launch {
                    resultOffline = recognizeImage(image)

                    withContext(Main){
                        objectDetectorListener?.onResultsModeOffline(
                            resultOffline,
                            image
                        )
                    }
                }
            }
//            resultOffline = recognizeImage(image)
//            objectDetectorListener?.onResultsModeOffline(
//                resultOffline,
//                image
//            )
        } else {
            // use mode 2 -> predict from server
            // if mode 2 in use, predict image from server
            CoroutineScope(Default).launch {
                serviceApi.predictOnServer(image, Constants.MODEL_ONLINE)

                withContext(Main){
                    objectDetectorListener?.onResultsModeOnline(
                        serviceApi.resultModelOnline,
                        image
                    )
                }
            }

//            serviceApi.predictOnServer(image, Constants.MODEL_ONLINE)
//
//            objectDetectorListener?.onResultsModeOnline(
//                serviceApi.resultModelOnline,
//                image
//            )

        }
    }

    // Config values.
    // Pre-allocated buffers.

    private lateinit var imgData: ByteBuffer
    private lateinit var outData: ByteBuffer

    private lateinit var tfLite: Interpreter
    private var inp_scale = 0f
    private var inp_zero_point = 0
    private var oup_scale = 0f
    private var oup_zero_point = 0
    private var numClass = 0

    // Float model
    private val IMAGE_MEAN = 0f

    private val IMAGE_STD = 255.0f

    //config yolo
    private var tfliteModel: MappedByteBuffer? = null

    private var intValues = IntArray(inputSize * inputSize)

    private var isQuantized = false

    /** holds a gpu delegate  */
    var gpuDelegate: GpuDelegate? = null

    /** holds an nnapi delegate  */
    var nnapiDelegate: NnApiDelegate? = null
    private var output_box = 0

    protected var mNmsThresh = 0.6f

    protected open fun box_iou(a: RectF, b: RectF): Float {
        return box_intersection(a, b) / box_union(a, b)
    }

    open fun getInputSize(): Int {
        return inputSize
    }

    open fun getObjThresh(): Float {
        return threshold
    }

    protected open fun box_intersection(a: RectF, b: RectF): Float {
        val w: Float = overlap(
            (a.left + a.right) / 2, a.right - a.left,
            (b.left + b.right) / 2, b.right - b.left
        )
        val h: Float = overlap(
            (a.top + a.bottom) / 2, a.bottom - a.top,
            (b.top + b.bottom) / 2, b.bottom - b.top
        )
        return if (w < 0 || h < 0) 0.toFloat() else w * h
    }

    protected open fun box_union(a: RectF, b: RectF): Float {
        val i = box_intersection(a, b)
        return (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i
    }

    protected open fun overlap(x1: Float, w1: Float, x2: Float, w2: Float): Float {
        val l1 = x1 - w1 / 2
        val l2 = x2 - w2 / 2
        val left = if (l1 > l2) l1 else l2
        val r1 = x1 + w1 / 2
        val r2 = x2 + w2 / 2
        val right = if (r1 < r2) r1 else r2
        return right - left
    }

    /**
     * Memory-map the model file in Assets.
     */
    @Throws(IOException::class)
    open fun loadModelFile(modelFilename: String?): MappedByteBuffer? {
        val fileDescriptor = modelFilename?.let { context.assets.openFd(it) }
        val inputStream = FileInputStream(fileDescriptor?.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor?.startOffset
        val declaredLength = fileDescriptor?.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset!!, declaredLength!!)
    }

    private fun setupYoloV5Classifier(modelName: String) {
        val options = Interpreter.Options()
        options.numThreads = numThreads

        // Use the specified hardware for running the model. Default to CPU
        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                val gpuOptions = GpuDelegate.Options()
                gpuOptions.setPrecisionLossAllowed(true) // It seems that the default is true
                gpuOptions.setInferencePreference(GpuDelegate.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED)
                gpuDelegate = GpuDelegate(gpuOptions)
                options.addDelegate(gpuDelegate)
            }
            DELEGATE_NNAPI -> {
                nnapiDelegate = null
                nnapiDelegate = NnApiDelegate()
                options.addDelegate(nnapiDelegate)
                options.numThreads = numThreads
                options.useNNAPI = true
            }
        }

        try {
            tfliteModel = loadModelFile(modelName)
            tfLite = Interpreter(tfliteModel!!, options)
        } catch (e: IllegalStateException) {
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for details"
            )
            Log.e("Test", "TFLite failed to load model with error: " + e.message)
        }

        // Pre-allocate buffers.
        // Pre-allocate buffers.
        val numBytesPerChannel: Int = if (isQuantized) {
            1 // Quantized
        } else {
            4 // Floating point
        }
        imgData = ByteBuffer.allocate(1 * inputSize * inputSize * 3 * numBytesPerChannel)
        imgData.order(ByteOrder.nativeOrder())

        output_box = (((inputSize / 32).toDouble().pow(2.0) + (inputSize / 16).toDouble().pow(2.0) +
                (inputSize / 8).toDouble().pow(2.0)) * 3).toInt()

        if (isQuantized) {
            val inpten: Tensor = tfLite.getInputTensor(0)
            inp_scale = inpten.quantizationParams().scale
            inp_zero_point = inpten.quantizationParams().zeroPoint
            val oupten: Tensor = tfLite.getOutputTensor(0)
            oup_scale = oupten.quantizationParams().scale
            oup_zero_point = oupten.quantizationParams().zeroPoint
        }

        val shape: IntArray = tfLite.getOutputTensor(0).shape()
        numClass = shape[shape.size - 1] - 5
        outData = ByteBuffer.allocateDirect(output_box * (numClass + 5) * numBytesPerChannel)
        outData.order(ByteOrder.nativeOrder())
    }

    /**
     * Writes Image data into a `ByteBuffer`.
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val pixel = 0
        imgData.rewind()
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixelValue: Int = intValues[i * inputSize + j]
                if (isQuantized) {
                    // Quantized model
                    imgData.put((((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point) as Byte)
                    imgData.put((((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point) as Byte)
                    imgData.put((((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point) as Byte)
                } else { // Float model
                    imgData.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }
    }

    private fun getBitmap(buffer: ByteBuffer, width: Int, height: Int): Bitmap? {
        buffer.rewind()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    private fun sortResult(results: ArrayList<Recognition>): ArrayList<Recognition>{
        results.sortWith(Comparator { p0, p1 -> // Intentionally reversed to put high confidence at the head of the queue.
            p0!!.getLocation().top.compareTo(p1!!.getLocation().top)
        })

        return results
    }

    //non maximum suppression
    private fun nms(list: ArrayList<Recognition>): ArrayList<Recognition>? {
        val nmsList = ArrayList<Recognition>()
        for (k in labels.indices) {
            //1.find max confidence per class
            val pq = PriorityQueue(
                50,
                Comparator<Recognition> { lhs, rhs -> // Intentionally reversed to put high confidence at the head of the queue.
                    (rhs.confidence!!).compareTo(lhs.confidence!!)
                })
            for (i in list.indices) {
                if (list[i].detectedClass == k) {
                    pq.add(list[i])
                }
            }

            //2.do non maximum suppression
            while (pq.size > 0) {
                //insert detection with max confidence
                val a = arrayOfNulls<Recognition>(pq.size)
                val detections: Array<Recognition> = pq.toArray(a)
                val max = detections[0]
                nmsList.add(max)
                pq.clear()
                for (j in 1 until detections.size) {
                    val detection = detections[j]
                    val b = detection.getLocation()
                    if (box_iou(max.getLocation(), b) < mNmsThresh) {
                        pq.add(detection)
                    }
                }
            }
        }

        // done detection
        detectionFinish = true
        return sortResult(nmsList)
    }

    private fun recognizeImage(image: Bitmap): ArrayList<Recognition>? {
        // start detection
        detectionFinish = false
        val reshapeBitmap = Bitmap.createScaledBitmap(image, 640, 640, false)
        convertBitmapToByteBuffer(reshapeBitmap)

        val bitmapDariByteBuffer = getBitmap(imgData, reshapeBitmap.width, reshapeBitmap.height)

        val outputMap: MutableMap<Int, Any> = HashMap()

        outData.rewind()
        outputMap[0] = outData
        val inputArray = arrayOf<Any>(imgData)

        tfLite.runForMultipleInputsOutputs(inputArray, outputMap)
        val byteBuffer = outputMap[0] as ByteBuffer
        byteBuffer.rewind()
        val detections: ArrayList<Recognition> = ArrayList<Recognition>()

        val out =
            Array(1) {
                Array(output_box) {
                    FloatArray(numClass + 5)
                }
            }
        Log.d("YoloV5Classifier", "out[0] detect start")
        for (i in 0 until output_box) {
            for (j in 0 until numClass + 5) {
                if (isQuantized) {
                    out[0][i][j] =
                        oup_scale * ((byteBuffer.get().toInt() and 0xFF) - oup_zero_point)
                } else {
                    out[0][i][j] = byteBuffer.float
                }
            }
            // Denormalize xywh
            for (j in 0..3) {
                out[0][i][j] = out[0][i][j] * getInputSize()
            }
        }

        for (i in 0 until output_box) {
            val offset = 0
            val confidence = out[0][i][4]
            var detectedClass = -1
            var maxClass = 0f
            val classes = FloatArray(labels.size)
            for (c in labels.indices) {
                classes[c] = out[0][i][5 + c]
            }
            for (c in labels.indices) {
                if (classes[c] > maxClass) {
                    detectedClass = c
                    maxClass = classes[c]
                }
            }

            val confidenceInClass = maxClass * confidence

            if (confidenceInClass > getObjThresh()) {
                val xPos = out[0][i][0]
                val yPos = out[0][i][1]
                val w = out[0][i][2]
                val h = out[0][i][3]
                Log.d("YoloV5Classifier", "$xPos,$yPos,$w,$h")
                val rect = RectF(
                    max(0f, xPos - w / 2),
                    max(0f, yPos - h / 2),
                    min((image.width - 1).toFloat(), xPos + w / 2),
                    min((image.height - 1).toFloat(), yPos + h / 2)
                )
                detections.add(
                    Recognition(
                        "" + offset, labels[detectedClass],
                        confidenceInClass, rect, detectedClass
                    )
                )
            }
        }
        Log.d("YoloV5Classifier", "detect end")

        Log.d("hasilnya sblom nms bgt", detections.toString())

        return nms(detections)
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResultsModeOffline(
            results: ArrayList<Recognition>?,
            image: Bitmap
        )
        fun onResultsModeOnline(
            results: ArrayList<ResultAnnotation>?,
            image: Bitmap
        )
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
    }
}