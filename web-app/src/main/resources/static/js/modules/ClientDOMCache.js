const ClientDOMCache = (function() {
    const cache = {};
    
    function getElement(id) {
        if (!cache[id]) {
            cache[id] = document.getElementById(id);
        }
        return cache[id];
    }
    
    function getElementByClass(className, index = 0) {
        const key = `class_${className}_${index}`;
        if (!cache[key]) {
            const elements = document.getElementsByClassName(className);
            cache[key] = elements[index] || null;
        }
        return cache[key];
    }
    
    function getElementBySelector(selector) {
        if (!cache[selector]) {
            cache[selector] = document.querySelector(selector);
        }
        return cache[selector];
    }
    
    function clearCache() {
        Object.keys(cache).forEach(key => delete cache[key]);
    }
    
    return {
        getElement,
        getElementByClass,
        getElementBySelector,
        clearCache
    };
})();
