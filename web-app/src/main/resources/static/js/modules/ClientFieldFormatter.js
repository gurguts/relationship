const ClientFieldFormatter = (function() {
    function formatFieldValue(fieldValue, field) {
        if (!fieldValue) return '';
        
        switch (field.fieldType) {
            case CLIENT_FIELD_TYPES.TEXT:
            case CLIENT_FIELD_TYPES.PHONE:
                return fieldValue.valueText || '';
            case CLIENT_FIELD_TYPES.NUMBER:
                return fieldValue.valueNumber || '';
            case CLIENT_FIELD_TYPES.DATE:
                return fieldValue.valueDate || '';
            case CLIENT_FIELD_TYPES.BOOLEAN:
                if (fieldValue.valueBoolean === true) return CLIENT_MESSAGES.YES;
                if (fieldValue.valueBoolean === false) return CLIENT_MESSAGES.NO;
                return '';
            case CLIENT_FIELD_TYPES.LIST:
                return fieldValue.valueListValue || '';
            default:
                return '';
        }
    }
    
    function formatFieldValueForModal(fieldValue, field) {
        if (!fieldValue) return '';
        
        switch (field.fieldType) {
            case CLIENT_FIELD_TYPES.TEXT:
                return fieldValue.valueText || '';
            case CLIENT_FIELD_TYPES.PHONE:
                return fieldValue.valueText || '';
            case CLIENT_FIELD_TYPES.NUMBER:
                return fieldValue.valueNumber || '';
            case CLIENT_FIELD_TYPES.DATE:
                return fieldValue.valueDate || '';
            case CLIENT_FIELD_TYPES.BOOLEAN:
                if (fieldValue.valueBoolean === true) return CLIENT_MESSAGES.YES;
                if (fieldValue.valueBoolean === false) return CLIENT_MESSAGES.NO;
                return '';
            case CLIENT_FIELD_TYPES.LIST:
                return fieldValue.valueListValue || '';
            default:
                return '';
        }
    }
    
    return {
        formatFieldValue,
        formatFieldValueForModal
    };
})();
