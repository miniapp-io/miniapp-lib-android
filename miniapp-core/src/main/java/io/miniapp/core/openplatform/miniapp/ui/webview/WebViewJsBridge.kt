package io.miniapp.core.openplatform.miniapp.ui.webview

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import io.miniapp.core.R
import io.miniapp.core.openplatform.miniapp.ActivityStack
import io.miniapp.core.openplatform.miniapp.utils.AndroidUtils
import io.miniapp.core.openplatform.miniapp.utils.UiThreadUtil.runOnUiThread
import org.json.JSONArray
import java.lang.ref.WeakReference
import java.util.Calendar

/**
 * JavaScript Bridge for WebView
 * Handles native UI components invoked from JavaScript
 */
internal class WebViewJsBridge(webView: WebView) {

    private val webViewRef = WeakReference(webView)

    companion object {
        /**
         * JavaScript function to set input value with framework compatibility
         * Supports React, Vue, Angular and vanilla JS
         */
        private fun getSetInputValueJs(inputId: String, value: String): String {
            return """
                (function() {
                    var input = document.getElementById('$inputId');
                    if (!input) return;
                    
                    // Focus the input first
                    input.focus();
                    
                    // For React: use native value setter to bypass React's synthetic event system
                    var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
                    if (nativeInputValueSetter) {
                        nativeInputValueSetter.call(input, '$value');
                    } else {
                        input.value = '$value';
                    }
                    
                    // Trigger input event (for React, Vue v-model, etc.)
                    var inputEvent = new Event('input', { bubbles: true, cancelable: true });
                    input.dispatchEvent(inputEvent);
                    
                    // Trigger change event
                    var changeEvent = new Event('change', { bubbles: true, cancelable: true });
                    input.dispatchEvent(changeEvent);
                    
                    // For some frameworks that listen to blur
                    input.blur();
                    
                    // Additional: trigger events that some frameworks might need
                    try {
                        // For Angular
                        var ngModelEvent = new Event('ngModelChange', { bubbles: true });
                        input.dispatchEvent(ngModelEvent);
                    } catch(e) {}
                    
                    try {
                        // For older browsers or specific frameworks
                        if (typeof input.onchange === 'function') {
                            input.onchange();
                        }
                    } catch(e) {}
                })();
            """.trimIndent()
        }

        /**
         * JavaScript function to set select value with framework compatibility
         * Supports React, Vue, Angular and vanilla JS
         */
        private fun getSetSelectValueJs(selectId: String, value: String): String {
            return """
                (function() {
                    var select = document.getElementById('$selectId');
                    if (!select) return;
                    
                    // Focus the select first
                    select.focus();
                    
                    // For React: use native value setter to bypass React's synthetic event system
                    var nativeSelectValueSetter = Object.getOwnPropertyDescriptor(window.HTMLSelectElement.prototype, 'value').set;
                    if (nativeSelectValueSetter) {
                        nativeSelectValueSetter.call(select, '$value');
                    } else {
                        select.value = '$value';
                    }
                    
                    // Trigger input event (for React, Vue v-model, etc.)
                    var inputEvent = new Event('input', { bubbles: true, cancelable: true });
                    select.dispatchEvent(inputEvent);
                    
                    // Trigger change event
                    var changeEvent = new Event('change', { bubbles: true, cancelable: true });
                    select.dispatchEvent(changeEvent);
                    
                    // For some frameworks that listen to blur
                    select.blur();
                    
                    // Additional: trigger events that some frameworks might need
                    try {
                        // For Angular
                        var ngModelEvent = new Event('ngModelChange', { bubbles: true });
                        select.dispatchEvent(ngModelEvent);
                    } catch(e) {}
                    
                    try {
                        // For older browsers or specific frameworks
                        if (typeof select.onchange === 'function') {
                            select.onchange();
                        }
                    } catch(e) {}
                })();
            """.trimIndent()
        }
    }

    private data class SelectItem(
        val text: String,
        val value: String,
        val isGroup: Boolean = false,
        val isDisabled: Boolean = false,
        val indent: Boolean = false
    )

    /**
     * Shows custom select dialog
     * @param selectId The ID of the select element
     * @param currentSelectedValue The current selected value
     * @param optionsJson The options data in JSON format
     */
    @JavascriptInterface
    fun showCustomSelect(selectId: String, currentSelectedValue: String, optionsJson: String) {
        runOnUiThread {
            try {
                val webView = webViewRef.get() ?: return@runOnUiThread
                val options = JSONArray(optionsJson)
                val items = ArrayList<SelectItem>()
                var initialSelectedPosition = -1
                var currentIndex = 0

                fun processOptions(optionsArray: JSONArray) {
                    for (i in 0 until optionsArray.length()) {
                        val option = optionsArray.getJSONObject(i)
                        when (option.getString("type")) {
                            "group" -> {
                                items.add(SelectItem(
                                    text = option.getString("label"),
                                    value = "",
                                    isGroup = true,
                                    isDisabled = true
                                ))

                                currentIndex++

                                val groupOptions = option.getJSONArray("options")
                                for (j in 0 until groupOptions.length()) {
                                    val groupOption = groupOptions.getJSONObject(j)
                                    items.add(SelectItem(
                                        text = groupOption.getString("text"),
                                        value = groupOption.getString("value"),
                                        isGroup = false,
                                        isDisabled = groupOption.optBoolean("disabled", false),
                                        indent = true
                                    ))

                                    if (groupOption.getString("value") == currentSelectedValue) {
                                        initialSelectedPosition = currentIndex
                                    }
                                    currentIndex++
                                }
                            }
                            "option" -> {
                                items.add(SelectItem(
                                    text = option.getString("text"),
                                    value = option.getString("value"),
                                    isGroup = false,
                                    isDisabled = option.optBoolean("disabled", false)
                                ))

                                if (option.getString("value") == currentSelectedValue) {
                                    initialSelectedPosition = currentIndex
                                }
                                currentIndex++
                            }
                        }
                    }
                }

                processOptions(options)

                val activity = ActivityStack.getCurrentActivity() ?: return@runOnUiThread

                val adapter = object : ArrayAdapter<SelectItem>(
                    activity,
                    R.layout.item_select_option,
                    R.id.text_option,
                    items
                ) {
                    private var selectedPosition = initialSelectedPosition

                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent)
                        val item = items[position]

                        // Set text and style
                        val textView = view.findViewById<TextView>(R.id.text_option)
                        textView.text = item.text

                        // Handle indentation
                        val params = textView.layoutParams as ViewGroup.MarginLayoutParams
                        params.marginStart = if (item.indent) AndroidUtils.dp(16) else 0
                        textView.layoutParams = params

                        // Set group title style
                        if (item.isGroup) {
                            textView.setTypeface(null, Typeface.BOLD)
                        } else {
                            textView.setTypeface(null, Typeface.NORMAL)
                        }

                        // Set checkmark icon for selected item
                        val checkmark = view.findViewById<ImageView>(R.id.checkmark)
                        checkmark.visibility = if (position == selectedPosition && !item.isGroup) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                        return view
                    }

                    fun setSelectedPosition(position: Int) {
                        selectedPosition = position
                        notifyDataSetChanged()
                    }
                }

                val dialog = Dialog(activity)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(R.layout.dialog_custom_select)

                val listView = dialog.findViewById<ListView>(R.id.listView)
                listView.adapter = adapter

                listView.setOnItemClickListener { _, _, position, _ ->
                    val item = items[position]
                    if (!item.isGroup && !item.isDisabled) {
                        adapter.setSelectedPosition(position)

                        val selectedValue = item.value
                        // Update the select element's value with framework compatibility
                        webView.evaluateJavascript(getSetSelectValueJs(selectId, selectedValue), null)
                        dialog.dismiss()
                    }
                }

                dialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Displays the date picker
     * @param inputId The ID of the input element
     * @param currentValue The current value, in the format yyyy-MM-dd
     * @param minDate The minimum date, in the format yyyy-MM-dd
     * @param maxDate The maximum date, in the format yyyy-MM-dd
     */
    @JavascriptInterface
    fun showDatePicker(inputId: String, currentValue: String, minDate: String, maxDate: String) {
        runOnUiThread {
            try {
                val webView = webViewRef.get() ?: return@runOnUiThread
                val activity = ActivityStack.getCurrentActivity() ?: return@runOnUiThread

                val calendar = Calendar.getInstance()

                // Parse the current value
                if (currentValue.isNotEmpty()) {
                    try {
                        val parts = currentValue.split("-")
                        if (parts.size == 3) {
                            calendar.set(Calendar.YEAR, parts[0].toInt())
                            calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                            calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                val datePickerDialog = DatePickerDialog(
                    activity,
                    { _, selectedYear, selectedMonth, selectedDay ->
                        // Format the date as yyyy-MM-dd
                        val formattedDate = String.format(
                            "%04d-%02d-%02d",
                            selectedYear,
                            selectedMonth + 1,
                            selectedDay
                        )

                        // Update the input element's value with framework compatibility
                        webView.evaluateJavascript(getSetInputValueJs(inputId, formattedDate), null)
                    },
                    year,
                    month,
                    day
                )

                // Set minimum date
                if (minDate.isNotEmpty()) {
                    try {
                        val parts = minDate.split("-")
                        if (parts.size == 3) {
                            val minCalendar = Calendar.getInstance()
                            minCalendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                            datePickerDialog.datePicker.minDate = minCalendar.timeInMillis
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Set maximum date
                if (maxDate.isNotEmpty()) {
                    try {
                        val parts = maxDate.split("-")
                        if (parts.size == 3) {
                            val maxCalendar = Calendar.getInstance()
                            maxCalendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                            datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                datePickerDialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Displays the time picker
     * @param inputId The ID of the input element
     * @param currentValue The current value, format is HH:mm or HH:mm:ss
     */
    @JavascriptInterface
    fun showTimePicker(inputId: String, currentValue: String) {
        runOnUiThread {
            try {
                val webView = webViewRef.get() ?: return@runOnUiThread
                val activity = ActivityStack.getCurrentActivity() ?: return@runOnUiThread

                var hour = 0
                var minute = 0

                // Parse the current value
                if (currentValue.isNotEmpty()) {
                    try {
                        val parts = currentValue.split(":")
                        if (parts.size >= 2) {
                            hour = parts[0].toInt()
                            minute = parts[1].toInt()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val timePickerDialog = TimePickerDialog(
                    activity,
                    { _, selectedHour, selectedMinute ->
                        // Format the time as HH:mm
                        val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)

                        // Update the input element's value with framework compatibility
                        webView.evaluateJavascript(getSetInputValueJs(inputId, formattedTime), null)
                    },
                    hour,
                    minute,
                    true // Use 24-hour format
                )

                timePickerDialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Displays the date and time picker
     * @param inputId The ID of the input element
     * @param currentValue The current value, format is yyyy-MM-ddTHH:mm
     * @param minDate The minimum date and time
     * @param maxDate The maximum date and time
     */
    @JavascriptInterface
    fun showDateTimePicker(inputId: String, currentValue: String, minDate: String, maxDate: String) {
        runOnUiThread {
            try {
                val webView = webViewRef.get() ?: return@runOnUiThread
                val activity = ActivityStack.getCurrentActivity() ?: return@runOnUiThread

                val calendar = Calendar.getInstance()
                var hour = 0
                var minute = 0

                // Parse the current value, format is yyyy-MM-ddTHH:mm
                if (currentValue.isNotEmpty()) {
                    try {
                        val dateTimeParts = currentValue.split("T")
                        if (dateTimeParts.isNotEmpty()) {
                            val dateParts = dateTimeParts[0].split("-")
                            if (dateParts.size == 3) {
                                calendar.set(Calendar.YEAR, dateParts[0].toInt())
                                calendar.set(Calendar.MONTH, dateParts[1].toInt() - 1)
                                calendar.set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                            }
                        }
                        if (dateTimeParts.size > 1) {
                            val timeParts = dateTimeParts[1].split(":")
                            if (timeParts.size >= 2) {
                                hour = timeParts[0].toInt()
                                minute = timeParts[1].toInt()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                // Show the date picker first
                val datePickerDialog = DatePickerDialog(
                    activity,
                    { _, selectedYear, selectedMonth, selectedDay ->
                        // After date selection, show time picker
                        val timePickerDialog = TimePickerDialog(
                            activity,
                            { _, selectedHour, selectedMinute ->
                                // Format date and time as yyyy-MM-ddTHH:mm
                                val formattedDateTime = String.format(
                                    "%04d-%02d-%02dT%02d:%02d",
                                    selectedYear,
                                    selectedMonth + 1,
                                    selectedDay,
                                    selectedHour,
                                    selectedMinute
                                )

                                // Update the input element's value with framework compatibility
                                webView.evaluateJavascript(getSetInputValueJs(inputId, formattedDateTime), null)
                            },
                            hour,
                            minute,
                            true // Use 24-hour format
                        )
                        timePickerDialog.show()
                    },
                    year,
                    month,
                    day
                )

                // Set minimum date
                if (minDate.isNotEmpty()) {
                    try {
                        val dateTimeParts = minDate.split("T")
                        if (dateTimeParts.isNotEmpty()) {
                            val parts = dateTimeParts[0].split("-")
                            if (parts.size == 3) {
                                val minCalendar = Calendar.getInstance()
                                minCalendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                datePickerDialog.datePicker.minDate = minCalendar.timeInMillis
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Set maximum date
                if (maxDate.isNotEmpty()) {
                    try {
                        val dateTimeParts = maxDate.split("T")
                        if (dateTimeParts.isNotEmpty()) {
                            val parts = dateTimeParts[0].split("-")
                            if (parts.size == 3) {
                                val maxCalendar = Calendar.getInstance()
                                maxCalendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                datePickerDialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
