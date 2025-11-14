package io.miniapp.core.openplatform.miniapp.ui.proxy

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.miniapp.core.openplatform.miniapp.utils.UiThreadUtil
import org.json.JSONObject

internal class WebAppSensors(val context: Context, val webAppProxy: WebAppProxy) {

    private var sensorManager: SensorManager? = null

    private var accelerometer: Sensor? = null
    private var accelerometerDesiredRefreshRate: Long = 0
    private var gyroscope: Sensor? = null
    private var gyroscopeDesiredRefreshRate: Long = 0
    private var orientationMagnetometer: Sensor? = null
    private var orientationAccelerometer: Sensor? = null
    private var absoluteOrientationDesiredRefreshRate: Long = 0
    private var rotation: Sensor? = null
    private var relativeOrientationDesiredRefreshRate: Long = 0

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
    }

    fun detach() {
        pause()
    }

    fun startAccelerometer(refreshRate: Long): Boolean {
        if (sensorManager == null) return false
        if (accelerometer != null) return true
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) return false
        accelerometerDesiredRefreshRate = refreshRate
        if (!paused) {
            sensorManager?.registerListener(
                accelerometerListener,
                accelerometer,
                getSensorDelay(refreshRate)
            )
        }
        return true
    }

    fun stopAccelerometer(): Boolean {
        if (sensorManager == null) return false
        if (accelerometer == null) return true
        if (!paused) {
            sensorManager?.unregisterListener(accelerometerListener, accelerometer)
        }
        if (accelerometerListenerPostponed != null) {
            UiThreadUtil.cancelRunOnUIThread(accelerometerListenerPostponed)
            accelerometerListenerPostponed = null
        }
        accelerometer = null
        return true
    }

    fun startGyroscope(refreshRate: Long): Boolean {
        if (sensorManager == null) return false
        if (gyroscope != null) return true
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyroscope == null) return false
        gyroscopeDesiredRefreshRate = refreshRate
        if (!paused) {
            sensorManager?.registerListener(
                gyroscopeListener,
                gyroscope,
                getSensorDelay(refreshRate)
            )
        }
        return true
    }

    fun stopGyroscope(): Boolean {
        if (sensorManager == null) return false
        if (gyroscope == null) return true
        if (!paused) {
            sensorManager?.unregisterListener(gyroscopeListener, gyroscope)
        }
        if (gyroscopeListenerPostponed != null) {
            UiThreadUtil.cancelRunOnUIThread(gyroscopeListenerPostponed)
            gyroscopeListenerPostponed = null
        }
        gyroscope = null
        return true
    }

    fun startOrientation(absolute: Boolean, refreshRate: Long): Boolean {
        if (sensorManager == null) return false
        if (absolute) {
            if (rotation != null) {
                if (relativeOrientationListenerPostponed != null) {
                    UiThreadUtil.cancelRunOnUIThread(relativeOrientationListenerPostponed)
                    relativeOrientationListenerPostponed = null
                }
                if (!paused) {
                    if (rotation != null) {
                        sensorManager?.unregisterListener(relativeOrientationListener, rotation)
                    }
                }
                rotation = null
            }
            if (orientationMagnetometer != null && orientationAccelerometer != null) return true
            orientationAccelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            orientationMagnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            if (orientationAccelerometer == null || orientationMagnetometer == null) return false
            absoluteOrientationDesiredRefreshRate = refreshRate
            if (!paused) {
                sensorManager?.registerListener(
                    absoluteOrientationListener,
                    orientationAccelerometer,
                    getSensorDelay(refreshRate)
                )
                sensorManager?.registerListener(
                    absoluteOrientationListener,
                    orientationMagnetometer,
                    getSensorDelay(refreshRate)
                )
            }
        } else {
            if (orientationMagnetometer != null || orientationAccelerometer != null) {
                if (absoluteOrientationListenerPostponed != null) {
                    UiThreadUtil.cancelRunOnUIThread(absoluteOrientationListenerPostponed)
                    absoluteOrientationListenerPostponed = null
                }
                if (!paused) {
                    if (orientationAccelerometer != null) {
                        sensorManager?.unregisterListener(
                            absoluteOrientationListener,
                            orientationAccelerometer
                        )
                    }
                    if (orientationMagnetometer != null) {
                        sensorManager?.unregisterListener(
                            absoluteOrientationListener,
                            orientationMagnetometer
                        )
                    }
                }
                orientationAccelerometer = null
                orientationMagnetometer = null
            }
            if (rotation != null) return true
            rotation = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            if (rotation == null) return false
            relativeOrientationDesiredRefreshRate = refreshRate
            if (!paused) {
                sensorManager?.registerListener(
                    relativeOrientationListener,
                    rotation,
                    getSensorDelay(refreshRate)
                )
            }
        }
        return true
    }

    fun stopOrientation(): Boolean {
        if (sensorManager == null) return false
        if (orientationAccelerometer == null && orientationMagnetometer == null && rotation == null) return true
        if (!paused) {
            if (orientationAccelerometer != null) {
                sensorManager?.unregisterListener(
                    absoluteOrientationListener,
                    orientationAccelerometer
                )
            }
            if (orientationMagnetometer != null) {
                sensorManager?.unregisterListener(
                    absoluteOrientationListener,
                    orientationMagnetometer
                )
            }
            if (rotation != null) {
                sensorManager?.unregisterListener(relativeOrientationListener, rotation)
            }
        }
        if (absoluteOrientationListenerPostponed != null) {
            UiThreadUtil.cancelRunOnUIThread(absoluteOrientationListenerPostponed)
            absoluteOrientationListenerPostponed = null
        }
        if (relativeOrientationListenerPostponed != null) {
            UiThreadUtil.cancelRunOnUIThread(relativeOrientationListenerPostponed)
            relativeOrientationListenerPostponed = null
        }
        orientationAccelerometer = null
        orientationMagnetometer = null
        rotation = null
        return true
    }

    fun stopAll() {
        stopOrientation()
        stopGyroscope()
        stopAccelerometer()
    }

    // SENSOR_DELAY_NORMAL — 160ms
    // SENSOR_DELAY_UI — 60ms
    // SENSOR_DELAY_GAME — 20ms
    private fun getSensorDelay(refreshRate: Long): Int {
        if (refreshRate >= 160) return SensorManager.SENSOR_DELAY_NORMAL
        if (refreshRate >= 60) return SensorManager.SENSOR_DELAY_UI
        return SensorManager.SENSOR_DELAY_GAME
    }

    private var paused = false

    fun pause() {
        if (paused) return
        paused = true

        if (sensorManager != null) {
            if (accelerometer != null) {
                sensorManager?.unregisterListener(accelerometerListener, accelerometer)
            }
            if (accelerometerListenerPostponed != null) {
                UiThreadUtil.cancelRunOnUIThread(accelerometerListenerPostponed)
                accelerometerListenerPostponed = null
            }
            if (gyroscope != null) {
                sensorManager?.unregisterListener(gyroscopeListener, gyroscope)
            }
            if (gyroscopeListenerPostponed != null) {
                UiThreadUtil.cancelRunOnUIThread(gyroscopeListenerPostponed)
                gyroscopeListenerPostponed = null
            }
            if (orientationAccelerometer != null) {
                sensorManager?.unregisterListener(
                    absoluteOrientationListener,
                    orientationAccelerometer
                )
            }
            if (orientationMagnetometer != null) {
                sensorManager?.unregisterListener(
                    absoluteOrientationListener,
                    orientationMagnetometer
                )
            }
            if (absoluteOrientationListenerPostponed != null) {
                UiThreadUtil.cancelRunOnUIThread(absoluteOrientationListenerPostponed)
                absoluteOrientationListenerPostponed = null
            }
            if (rotation != null) {
                sensorManager?.unregisterListener(relativeOrientationListener, rotation)
            }
            if (relativeOrientationListenerPostponed != null) {
                UiThreadUtil.cancelRunOnUIThread(relativeOrientationListenerPostponed)
                relativeOrientationListenerPostponed = null
            }
        }
    }

    fun resume() {
        if (!paused) return
        paused = false

        if (sensorManager != null) {
            if (accelerometer != null) {
                sensorManager?.registerListener(
                    accelerometerListener,
                    accelerometer,
                    getSensorDelay(accelerometerDesiredRefreshRate)
                )
            }
            if (gyroscope != null) {
                sensorManager?.registerListener(
                    gyroscopeListener,
                    gyroscope,
                    getSensorDelay(gyroscopeDesiredRefreshRate)
                )
            }
            if (orientationAccelerometer != null) {
                sensorManager?.registerListener(
                    absoluteOrientationListener,
                    orientationAccelerometer,
                    getSensorDelay(absoluteOrientationDesiredRefreshRate)
                )
            }
            if (orientationMagnetometer != null) {
                sensorManager?.registerListener(
                    absoluteOrientationListener,
                    orientationMagnetometer,
                    getSensorDelay(absoluteOrientationDesiredRefreshRate)
                )
            }
            if (rotation != null) {
                sensorManager?.registerListener(
                    relativeOrientationListener,
                    rotation,
                    getSensorDelay(relativeOrientationDesiredRefreshRate)
                )
            }
        }
    }

    private var accelerometerListenerPostponed: Runnable? = null
    private val accelerometerListener: SensorEventListener = object : SensorEventListener {
        private var xyz: FloatArray? = null
        private var lastTime: Long = 0
        override fun onSensorChanged(event: SensorEvent) {
            if (accelerometerListenerPostponed != null) {
                UiThreadUtil.cancelRunOnUIThread(accelerometerListenerPostponed)
                accelerometerListenerPostponed = null
            }
            if (paused) return
            val now = System.currentTimeMillis()
            val diff = now - lastTime
            xyz = event.values
            if (diff < accelerometerDesiredRefreshRate) {
                UiThreadUtil.runOnUiThread( this::post, accelerometerDesiredRefreshRate-diff)
                return
            }
            post()
        }

        fun post() {
            if (xyz == null) return
            lastTime = System.currentTimeMillis()
            try {
                val eventData = JSONObject()
                eventData.put("x", -xyz!![0].toDouble())
                eventData.put("y", -xyz!![1].toDouble())
                eventData.put("z", -xyz!![2].toDouble())
                webAppProxy.notifyAccelerometerChanged(eventData)
            } catch (e: Exception) {
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    private var gyroscopeListenerPostponed: Runnable? = null
    private val gyroscopeListener: SensorEventListener = object : SensorEventListener {
        private var lastTime: Long = 0
        private val captured = FloatArray(3)


        override fun onSensorChanged(event: SensorEvent) {
            if (gyroscopeListenerPostponed != null) {
                UiThreadUtil.cancelRunOnUIThread(gyroscopeListenerPostponed)
                gyroscopeListenerPostponed = null
            }
            if (paused) return
            captured[0] += event.values[0]
            captured[1] += event.values[1]
            captured[2] += event.values[2]
            val now = System.currentTimeMillis()
            val diff = now - lastTime
            if (diff < gyroscopeDesiredRefreshRate) {
                UiThreadUtil.runOnUiThread(this::post, gyroscopeDesiredRefreshRate-diff)
                return
            }
            post()
        }

        fun post() {
            lastTime = System.currentTimeMillis()
            val xyz = captured
            try {
                val eventData = JSONObject()
                eventData.put("x", xyz[0].toDouble())
                eventData.put("y", xyz[1].toDouble())
                eventData.put("z", xyz[2].toDouble())
                // web api:
//                eventData.put("x", xyz[2]);
//                eventData.put("y", xyz[0]);
//                eventData.put("z", xyz[1]);
                webAppProxy.notifyGyroscopeChanged(eventData)
            } catch (e: Exception) {
            }
            captured[0] = 0f
            captured[1] = 0f
            captured[2] = 0f
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    private var absoluteOrientationListenerPostponed: Runnable? = null
    private val absoluteOrientationListener: SensorEventListener = object : SensorEventListener {
        private var lastTime: Long = 0

        private var gravity: FloatArray? = null
        private var geomagnetic: FloatArray? = null

        override fun onSensorChanged(event: SensorEvent) {
            if (absoluteOrientationListenerPostponed != null) {
                UiThreadUtil.cancelRunOnUIThread(absoluteOrientationListenerPostponed)
                absoluteOrientationListenerPostponed = null
            }
            if (paused) return
            val now = System.currentTimeMillis()
            val diff = now - lastTime
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) gravity = event.values
            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = event.values
            if (diff < absoluteOrientationDesiredRefreshRate) {
                UiThreadUtil.runOnUiThread(this::post, absoluteOrientationDesiredRefreshRate-diff)
                return
            }
            post()
        }

        fun post() {
            if (gravity == null || geomagnetic == null) return
            lastTime = System.currentTimeMillis()
            val R: FloatArray? = FloatArray(9)
            val I: FloatArray? = FloatArray(9)
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                val orientation: FloatArray? = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                try {
                    val eventData = JSONObject()
                    eventData.put("absolute", true)
                    eventData.put("alpha", -orientation!![0].toDouble())
                    eventData.put("beta", -orientation[1].toDouble())
                    eventData.put("gamma", orientation[2].toDouble())
                    webAppProxy.notifyDeviceOrientationChanged(eventData)
                } catch (e: Exception) {
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    private var relativeOrientationListenerPostponed: Runnable? = null
    private val relativeOrientationListener: SensorEventListener = object : SensorEventListener {
        private var lastTime: Long = 0
        private var values: FloatArray? = null
        override fun onSensorChanged(event: SensorEvent) {
            if (relativeOrientationListenerPostponed != null) {
                UiThreadUtil.cancelRunOnUIThread(relativeOrientationListenerPostponed)
                relativeOrientationListenerPostponed = null
            }
            if (paused) return
            val now = System.currentTimeMillis()
            val diff = now - lastTime
            if (diff < relativeOrientationDesiredRefreshRate) {
                UiThreadUtil.runOnUiThread(this::post, relativeOrientationDesiredRefreshRate-diff)
                return
            }
            if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                values = event.values
            }
            post()
        }

        private var mDeviceRotationMatrix: FloatArray? = null
        private var mTruncatedRotationVector: FloatArray? = null

        fun post() {
            if (values == null) return
            lastTime = System.currentTimeMillis()
            if (mDeviceRotationMatrix == null) {
                mDeviceRotationMatrix = FloatArray(9)
            }
            if (mTruncatedRotationVector == null) {
                mTruncatedRotationVector = FloatArray(4)
            }
            if (values!!.size > 4) {
                // On some Samsung devices sensorManager?.getRotationMatrixFromVector
                // appears to throw an exception if rotation vector has length > 4.
                // For the purposes of this class the first 4 values of the
                // rotation vector are sufficient (see crbug.com/335298 for details).
                System.arraycopy(values, 0, mTruncatedRotationVector, 0, 4)
                SensorManager.getRotationMatrixFromVector(
                    mDeviceRotationMatrix,
                    mTruncatedRotationVector
                )
            } else {
                SensorManager.getRotationMatrixFromVector(mDeviceRotationMatrix, values)
            }
            val orientation: FloatArray? = FloatArray(3)
            SensorManager.getOrientation(mDeviceRotationMatrix, orientation)
            try {
                val eventData = JSONObject()
                eventData.put("absolute", false)
                eventData.put("alpha", -orientation!![0].toDouble())
                eventData.put("beta", -orientation[1].toDouble())
                eventData.put("gamma", orientation[2].toDouble())
                webAppProxy.notifyDeviceOrientationChanged(eventData)
            } catch (e: Exception) {
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }
}