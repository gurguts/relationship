const DeclarantFilters = (function() {
    function buildFilters() {
        const filters = {};
        
        const vehiclesDateFromFilter = document.getElementById('vehicles-date-from-filter');
        const vehiclesDateToFilter = document.getElementById('vehicles-date-to-filter');
        const vehiclesIsOurVehicleFilter = document.getElementById('vehicles-is-our-vehicle-filter');
        const vehiclesCustomsDateFromFilter = document.getElementById('vehicles-customs-date-from-filter');
        const vehiclesCustomsDateToFilter = document.getElementById('vehicles-customs-date-to-filter');
        const vehiclesCustomsClearanceDateFromFilter = document.getElementById('vehicles-customs-clearance-date-from-filter');
        const vehiclesCustomsClearanceDateToFilter = document.getElementById('vehicles-customs-clearance-date-to-filter');
        const vehiclesUnloadingDateFromFilter = document.getElementById('vehicles-unloading-date-from-filter');
        const vehiclesUnloadingDateToFilter = document.getElementById('vehicles-unloading-date-to-filter');
        
        const dateFrom = vehiclesDateFromFilter?.value;
        const dateTo = vehiclesDateToFilter?.value;
        if (dateFrom) {
            filters.shipmentDateFrom = [dateFrom];
        }
        if (dateTo) {
            filters.shipmentDateTo = [dateTo];
        }
        
        const isOurVehicleFilter = vehiclesIsOurVehicleFilter?.checked;
        if (isOurVehicleFilter !== undefined && isOurVehicleFilter) {
            filters.isOurVehicle = ['true'];
        }
        
        const customsDateFrom = vehiclesCustomsDateFromFilter?.value;
        const customsDateTo = vehiclesCustomsDateToFilter?.value;
        if (customsDateFrom) {
            filters.customsDateFrom = [customsDateFrom];
        }
        if (customsDateTo) {
            filters.customsDateTo = [customsDateTo];
        }
        
        const customsClearanceDateFrom = vehiclesCustomsClearanceDateFromFilter?.value;
        const customsClearanceDateTo = vehiclesCustomsClearanceDateToFilter?.value;
        if (customsClearanceDateFrom) {
            filters.customsClearanceDateFrom = [customsClearanceDateFrom];
        }
        if (customsClearanceDateTo) {
            filters.customsClearanceDateTo = [customsClearanceDateTo];
        }
        
        const unloadingDateFrom = vehiclesUnloadingDateFromFilter?.value;
        const unloadingDateTo = vehiclesUnloadingDateToFilter?.value;
        if (unloadingDateFrom) {
            filters.unloadingDateFrom = [unloadingDateFrom];
        }
        if (unloadingDateTo) {
            filters.unloadingDateTo = [unloadingDateTo];
        }
        
        return filters;
    }
    
    function setDefaultVehicleDates() {
        const today = new Date();
        const last30Days = new Date();
        last30Days.setDate(today.getDate() - 30);
        const formattedToday = today.toISOString().split('T')[0];
        const formattedFrom = last30Days.toISOString().split('T')[0];

        const vehiclesDateFromFilter = document.getElementById('vehicles-date-from-filter');
        const vehiclesDateToFilter = document.getElementById('vehicles-date-to-filter');
        
        if (vehiclesDateFromFilter && !vehiclesDateFromFilter.value) {
            vehiclesDateFromFilter.value = formattedFrom;
        }
        if (vehiclesDateToFilter && !vehiclesDateToFilter.value) {
            vehiclesDateToFilter.value = formattedToday;
        }
    }
    
    return {
        buildFilters,
        setDefaultVehicleDates
    };
})();
