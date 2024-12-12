import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class CSVHelper(private val context: Context) {

    private val csvFile: File = File(context.filesDir, "dataAll.csv")
    private val executor = Executors.newSingleThreadExecutor()  // Executor for background tasks

    init {
        if (!csvFile.exists()) {
            try {
                csvFile.createNewFile()
                FileWriter(csvFile).use { writer ->
                    writer.append("Timestamp,Letter,Confidence,InferenceTime\n")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error creating CSV file", e)
            }
        }
    }

    fun appendTranslationToCSV(letter: String, confidence: Float, inferenceTime: Long) {
        executor.execute {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                FileWriter(csvFile, true).use { writer ->
                    writer.append("$timestamp,$letter,$confidence,$inferenceTime\n")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error writing to CSV file", e)
            }
        }
    }

    companion object {
        private const val TAG = "CSVHelper"
    }
}
