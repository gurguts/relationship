function validatePhoneNumber(phone) {
    if (!phone || typeof phone !== 'string') {
        return false;
    }
    const cleaned = phone.replace(/[^\d+]/g, '');
    const e164Pattern = /^\+[1-9]\d{1,14}$/;
    return e164Pattern.test(cleaned);
}

function normalizePhoneNumber(phone) {
    if (!phone || typeof phone !== 'string') {
        return phone;
    }
    let cleaned = phone.replace(/[^\d+]/g, '');
    
    if (cleaned.length === 0) {
        return phone;
    }
    
    let hasPlus = cleaned.startsWith('+');
    if (hasPlus) {
        cleaned = cleaned.substring(1);
    }
    
    cleaned = cleaned.replace(/^0+/, '');
    
    if (cleaned.length === 0) {
        return phone;
    }
    
    if (cleaned.startsWith('0')) {
        cleaned = cleaned.replace(/^0+/, '');
        if (cleaned.length === 0) {
            return phone;
        }
    }
    
    if (cleaned.length > 15) {
        cleaned = cleaned.substring(0, 15);
    }
    
    return '+' + cleaned;
}
