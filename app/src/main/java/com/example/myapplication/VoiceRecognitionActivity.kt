package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.core.BaseOptions
import java.io.IOException
import java.util.*
import java.util.UUID.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

//import kotlin.coroutines.jvm.internal.CompletedContinuation.context

class VoiceRecognitionActivity : AppCompatActivity() {

    private lateinit var device: BluetoothDevice
    private lateinit var btn:Button
    private lateinit var predictionsTextView: TextView
    private lateinit var accuracyProgressBar: ProgressBar
    private lateinit var statusTextView: TextView
    var bluetoothSocket: BluetoothSocket? = null
    private val MY_UUID = fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var classifier: AudioClassifier
    private lateinit var tensorAudio: TensorAudio
    private lateinit var recorder: AudioRecord
    private lateinit var executor: ScheduledThreadPoolExecutor
    var currentModel: String = "speech.tflite"
    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_recognition)
        context = this
        device = intent.getParcelableExtra("device")!!
        btn=findViewById(R.id.btn1)
        predictionsTextView = findViewById(R.id.predictions_text_view)
        accuracyProgressBar = findViewById(R.id.accuracy_progress_bar)
        statusTextView = findViewById(R.id.status_text_view)

        btn.setOnClickListener(){
            sendCommandToBluetoothDevice('B')
            bluetoothSocket?.close()
            val intent = Intent(this, MainActivity::class.java)
            // intent.putExtra("device", device)
            startActivity(intent)
        }
        // Connect to the Bluetooth device, start voice recognition, and update UI accordingly.
        connectToDevice()
        requestAudioPermission()

    }


    private val classifyRunnable = Runnable {
        classifyAudio()
    }


    fun initClassifier() {
        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(1)
        // Configures a set of parameters for the classifier and what results will be returned.
        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setScoreThreshold(0.3f)
            .setMaxResults(1)
            .setBaseOptions(baseOptionsBuilder.build())
            .build()

        try {
            // Create the classifier and required supporting objects
            classifier = AudioClassifier.createFromFileAndOptions(context, currentModel, options)
            Log.d("AudioClassification", "Classifier initialized successfully")
            tensorAudio = classifier.createInputTensorAudio()
            recorder = classifier.createAudioRecord()
            startAudioClassification()
        } catch (e: IllegalStateException) {


            Log.e("AudioClassification", "TFLite failed to load with error: " + e.message)
        }
    }

    fun startAudioClassification() {
        if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }
        Log.d("AudioClassification", "Starting audio classification")
        recorder.startRecording()

        executor = ScheduledThreadPoolExecutor(1)

        // Each model will expect a specific audio recording length. This formula calculates that
        // length using the input buffer size and tensor format sample rate.
        // For example, YAMNET expects 0.975 second length recordings.
        // This needs to be in milliseconds to avoid the required Long value dropping decimals.
        val lengthInMilliSeconds = ((classifier.requiredInputBufferSize * 1.0f) /
                classifier.requiredTensorAudioFormat.sampleRate) * 1000

        val interval = (lengthInMilliSeconds * (1 - 0.5f)).toLong()

        executor.scheduleAtFixedRate(
            classifyRunnable,
            0,
            interval,
            TimeUnit.MILLISECONDS)
        Log.d("AudioClassification", "classifyRunnable scheduled")
    }

    private fun classifyAudio() {
        Log.d("AudioClassification", "classifyAudio called")
        tensorAudio.load(recorder)
        var inferenceTime = SystemClock.uptimeMillis()
        val outputs = classifier.classify(tensorAudio)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // Get the top category from the output
        val topCategory = outputs[0].categories.maxByOrNull { it.score }

        if (topCategory != null) {
            val cmd = topCategory.label
            predictionsTextView.text = cmd
            processVoiceCommand(cmd)

        } else {
            Log.d("AudioClassification", "No top category found")
        }

        Log.d("AudioClassification", "classifyAudio finished")
    }


    fun stopAudioClassification() {
        recorder.stop()
        executor.shutdownNow()
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice() {

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
            bluetoothSocket?.connect()

        } catch (e: IOException) {
            Log.e("BluetoothConnection", "Error connecting to device", e)
            try {
                bluetoothSocket?.close()
            } catch (closeException: IOException) {
                Log.e("BluetoothConnection", "Error closing socket", closeException)
            }
        }
    }
    private fun sendCommandToBluetoothDevice(command: Char) {
        Log.d("AudioClassification", "send called")
        try {
            bluetoothSocket?.outputStream?.write(command.code)
            Log.d("AudioClassification", "message sent "+command+ " "+command.code)
            runOnUiThread {
                statusTextView.text = "command sent "+command
            }
        } catch (e: IOException) {
            Log.e("BluetoothConnection", "Error sending command to device", e)
            runOnUiThread {
                statusTextView.text = "command failed to send "+command
            }
        }
    }

    private val RECORD_AUDIO_REQUEST_CODE = 200

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        } else {
            initClassifier()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initClassifier()
            } else {
                Toast.makeText(this, "Permission denied to record audio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dissconnect(){
        sendCommandToBluetoothDevice('B')
        bluetoothSocket?.close()
        val intent = Intent(this, MainActivity::class.java)
       // intent.putExtra("device", device)
        startActivity(intent)
    }
    fun processVoiceCommand(command: String) {
        when (command.lowercase()) {
            "go" -> sendCommandToBluetoothDevice('F')
            "stop" -> sendCommandToBluetoothDevice('B')
            "left" -> sendCommandToBluetoothDevice('L')
            "right" -> sendCommandToBluetoothDevice('R')
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("BluetoothConnection", "Error closing socket", e)
        }
        stopAudioClassification()
    }

}