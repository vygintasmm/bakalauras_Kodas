import android.bluetooth.BluetoothSocket
import android.os.Build
//import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.VoiceRecognitionActivity
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
//@Config(manifest = Config.NONE)
//@Config(manifest = "src/main/AndroidManifest.xml")
//@Config(manifest = Config.NONE)
class VoiceRecognitionActivityTest {

    private lateinit var activity: VoiceRecognitionActivity

    @Before
    fun setUp() {
        activity = Robolectric.setupActivity(VoiceRecognitionActivity::class.java)
    }

    @Test
    fun testActivityNotNull() {
        assertNotNull(activity)
    }

    // Add your test cases here
    @Test
    fun should_processVoiceCommand_go() {
        activity.bluetoothSocket = Mockito.mock(BluetoothSocket::class.java)
        activity.processVoiceCommand("go")
        Mockito.verify(activity.bluetoothSocket?.outputStream)?.write('F'.code)
    }

    @Test
    fun should_processVoiceCommand_stop() {
        activity.bluetoothSocket = Mockito.mock(BluetoothSocket::class.java)
        activity.processVoiceCommand("stop")
        Mockito.verify(activity.bluetoothSocket?.outputStream)?.write('B'.code)
    }

    @Test
    fun should_processVoiceCommand_left() {
        activity.bluetoothSocket = Mockito.mock(BluetoothSocket::class.java)
        activity.processVoiceCommand("left")
        Mockito.verify(activity.bluetoothSocket?.outputStream)?.write('L'.code)
    }

    @Test
    fun should_processVoiceCommand_right() {
        activity.bluetoothSocket = Mockito.mock(BluetoothSocket::class.java)
        activity.processVoiceCommand("right")
        Mockito.verify(activity.bluetoothSocket?.outputStream)?.write('R'.code)
    }
}
