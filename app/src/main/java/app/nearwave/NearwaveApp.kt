package app.nearwave

import android.app.Application
import app.nearwave.radio.RadioController

class NearwaveApp : Application() {
    lateinit var radio: RadioController
        private set

    override fun onCreate() {
        super.onCreate()
        radio = RadioController(this)
    }
}
