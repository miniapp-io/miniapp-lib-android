/**
 * DateTime Picker Interceptor
 * Intercepts click events on input[type="date"], input[type="time"], and input[type="datetime-local"]
 * Invokes native DatePicker and TimePicker dialogs
 */
(function() {
    if (window.__dateTimePickerInjected) return;
    window.__dateTimePickerInjected = true;

    document.addEventListener('click', function(e) {
        var target = e.target;
        if (!target || !target.tagName || target.tagName.toLowerCase() !== 'input') {
            return;
        }
        
        var inputType = target.type.toLowerCase();
        
        // Handle date type
        if (inputType === 'date') {
            if (!target.id) {
                target.id = 'date_' + Math.random().toString(36).substr(2, 9);
            }
            e.preventDefault();
            
            // Get current value, format is yyyy-MM-dd
            var currentValue = target.value || '';
            var min = target.min || '';
            var max = target.max || '';
            
            window.NativeUIBridge.showDatePicker(
                target.id,
                currentValue,
                min,
                max
            );
            return false;
        }
        
        // Handle time type
        if (inputType === 'time') {
            if (!target.id) {
                target.id = 'time_' + Math.random().toString(36).substr(2, 9);
            }
            e.preventDefault();
            
            // Get current value, format is HH:mm or HH:mm:ss
            var currentValue = target.value || '';
            
            window.NativeUIBridge.showTimePicker(
                target.id,
                currentValue
            );
            return false;
        }
        
        // Handle datetime-local type
        if (inputType === 'datetime-local') {
            if (!target.id) {
                target.id = 'datetime_' + Math.random().toString(36).substr(2, 9);
            }
            e.preventDefault();
            
            // Get current value, format is yyyy-MM-ddTHH:mm
            var currentValue = target.value || '';
            var min = target.min || '';
            var max = target.max || '';
            
            window.NativeUIBridge.showDateTimePicker(
                target.id,
                currentValue,
                min,
                max
            );
            return false;
        }
    }, true);
})();
