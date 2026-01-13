document.addEventListener('DOMContentLoaded', async () => {
    try {
        console.log('Analytics page loaded');
    } catch (error) {
        console.error('Error initializing analytics page:', error);
        if (typeof handleError === 'function') {
            handleError(error);
        }
    }
});
