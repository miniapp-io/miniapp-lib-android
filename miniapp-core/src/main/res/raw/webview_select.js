/**
 * Custom Select Interceptor
 * Intercepts click events on <select> elements and invokes native select dialog
 */
(function() {
    if (window.__selectInterceptorInjected) return;
    window.__selectInterceptorInjected = true;

    function getOptionsData(element) {
        const options = [];
        
        for (let i = 0; i < element.children.length; i++) {
            const child = element.children[i];
            
            if (child.tagName.toLowerCase() === 'optgroup') {
                options.push({
                    type: 'group',
                    label: child.label,
                    options: [...child.children].map(opt => ({
                        type: 'option',
                        text: opt.text,
                        value: opt.value,
                        disabled: opt.disabled
                    }))
                });
            } else if (child.tagName.toLowerCase() === 'option') {
                options.push({
                    type: 'option',
                    text: child.text,
                    value: child.value,
                    disabled: child.disabled
                });
            }
        }
        
        return options;
    }

    document.addEventListener('click', function(e) {
        var target = e.target;
        if (target && target.tagName && target.tagName.toLowerCase() === 'select') {
            if (!target.id) {
                target.id = 'select_' + Math.random().toString(36).substr(2, 9);
            }
            e.preventDefault();
            
            const optionsData = getOptionsData(target);
            
            window.NativeUIBridge.showCustomSelect(
                target.id,
                target.value,
                JSON.stringify(optionsData)
            );
            return false;
        }
    }, true);
})();
