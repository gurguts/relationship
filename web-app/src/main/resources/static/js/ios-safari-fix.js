
(function() {
    'use strict';

    let isAppVisible = true;
    let resourceLoadAttempts = 0;
    const maxRetries = 3;

    function checkCSSLoaded() {
        const testElement = document.createElement('div');
        testElement.className = 'ios-test-element';
        testElement.style.cssText = 'position: absolute; left: -9999px; width: 1px; height: 1px;';
        document.body.appendChild(testElement);
        
        const computedStyle = window.getComputedStyle(testElement);
        const isLoaded = computedStyle.position === 'absolute';
        
        document.body.removeChild(testElement);
        return isLoaded;
    }

    function reloadCSS() {
        const links = document.querySelectorAll('link[rel="stylesheet"]');
        links.forEach(link => {
            const href = link.href;
            const newHref = href + (href.includes('?') ? '&' : '?') + 'v=' + Date.now();
            link.href = newHref;
        });
    }

    function reloadJS() {
        const scripts = document.querySelectorAll('script[src]');
        scripts.forEach(script => {
            if (script.src && !script.src.includes('ios-safari-fix.js')) {
                const newScript = document.createElement('script');
                newScript.src = script.src + (script.src.includes('?') ? '&' : '?') + 'v=' + Date.now();
                newScript.async = script.async;
                newScript.defer = script.defer;

                script.parentNode.insertBefore(newScript, script);
                script.parentNode.removeChild(script);
            }
        });
    }

    function checkAndReloadResources() {
        if (!isAppVisible) return;

        if (!checkCSSLoaded()) {
            reloadCSS();
            resourceLoadAttempts++;
        } else {
            resourceLoadAttempts = 0;
        }

        // Убираем автоматическую перезагрузку страницы - это слишком агрессивно
        // if (resourceLoadAttempts >= maxRetries) {
        //     window.location.reload();
        // }
    }

    function handleVisibilityChange() {
        if (document.hidden) {
            isAppVisible = false;
        } else {
            isAppVisible = true;

            setTimeout(checkAndReloadResources, 100);
        }
    }

    function handleFocus() {
        if (isAppVisible) {
            setTimeout(checkAndReloadResources, 100);
        }
    }

    function handlePageShow(event) {
        if (event.persisted) {
            setTimeout(checkAndReloadResources, 100);
        }
    }

    function handleResourceError(event) {
        if (event.target.tagName === 'LINK' || event.target.tagName === 'SCRIPT') {
            setTimeout(checkAndReloadResources, 100);
        }
    }

    function init() {
        const isIOSSafari = /iPad|iPhone|iPod/.test(navigator.userAgent) && 
                           /Safari/.test(navigator.userAgent) && 
                           !/CriOS|FxiOS|OPiOS|mercury/.test(navigator.userAgent);

        if (!isIOSSafari) {
            return;
        }
        document.addEventListener('visibilitychange', handleVisibilityChange);
        window.addEventListener('focus', handleFocus);
        window.addEventListener('pageshow', handlePageShow);
        document.addEventListener('error', handleResourceError, true);

        document.addEventListener('DOMContentLoaded', function() {
            setTimeout(checkAndReloadResources, 500);
        });

        // Периодическая проверка (каждые 60 секунд) - реже, чтобы не мешать
        setInterval(function() {
            if (isAppVisible) {
                checkAndReloadResources();
            }
        }, 60000);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
