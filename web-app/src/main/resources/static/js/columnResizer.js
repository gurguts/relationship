// Функция для инициализации изменения ширины столбцов
function initColumnResizer(clientTypeId) {
    const table = document.querySelector('#client-list table');
    if (!table) return;
    
    const thead = table.querySelector('thead tr');
    if (!thead) return;
    
    const ths = thead.querySelectorAll('th');
    
    // Удаляем предыдущие обработчики
    ths.forEach(th => {
        const existingResizer = th.querySelector('.column-resizer');
        if (existingResizer) {
            existingResizer.remove();
        }
    });
    
    // Создаем или обновляем colgroup для фиксации ширины столбцов
    let colgroup = table.querySelector('colgroup');
    if (!colgroup) {
        colgroup = document.createElement('colgroup');
        table.insertBefore(colgroup, table.firstChild);
    } else {
        colgroup.innerHTML = '';
    }
    
    // Загружаем сохраненные ширины из localStorage
    const savedWidths = loadColumnWidths(clientTypeId);
    
    // Сохраняем текущие ширины всех столбцов для фиксации
    const columnWidths = [];
    
    // Применяем ширины из настроек полей и localStorage и сохраняем их
    ths.forEach((th, index) => {
        let width = null;
        
        // Применяем сохраненную ширину из localStorage, если есть (приоритет)
        const savedWidth = savedWidths[index];
        if (savedWidth) {
            width = savedWidth;
        } else {
            // Применяем ширину из настроек поля, если есть
            const fieldId = th.getAttribute('data-field-id');
            if (fieldId && typeof window.visibleFields !== 'undefined' && window.visibleFields) {
                const field = window.visibleFields.find(f => {
                    // Сравниваем как числа, так как id может быть числом или строкой
                    return String(f.id) === String(fieldId);
                });
                if (field && field.columnWidth) {
                    width = field.columnWidth;
                }
            }
            
            // Если ширина из настроек не найдена, проверяем текущую ширину th
            // (которая могла быть установлена в buildDynamicTable)
            if (!width && th.style.width) {
                const styleWidth = parseInt(th.style.width);
                if (!isNaN(styleWidth) && styleWidth > 0) {
                    width = styleWidth;
                }
            }
        }
        
        // Если ширина не задана, используем текущую ширину или 200px для нединамичных полей
        if (!width) {
            // Для нединамичных полей (без data-field-id) используем 200px по умолчанию
            const fieldId = th.getAttribute('data-field-id');
            if (!fieldId) {
                width = 200; // Ширина по умолчанию для нединамичных полей
            } else {
                width = th.offsetWidth || 100; // Минимальная ширина по умолчанию для динамичных полей
            }
        }
        
        // Сохраняем ширину для фиксации
        columnWidths[index] = width;
        
        // Создаем элемент col для фиксации ширины столбца
        const col = document.createElement('col');
        col.style.width = width + 'px';
        col.setAttribute('width', width);
        colgroup.appendChild(col);
        th._colElement = col;
        
        // Применяем ширину
        th.style.width = width + 'px';
        th.style.minWidth = width + 'px';
        th.style.maxWidth = width + 'px';
        th.style.boxSizing = 'border-box';
        th.setAttribute('width', width);
        
        // Применяем к ячейкам tbody
        const tds = table.querySelectorAll(`tbody tr td:nth-child(${index + 1})`);
        tds.forEach(td => {
            td.style.width = width + 'px';
            td.style.minWidth = width + 'px';
            td.style.maxWidth = width + 'px';
            td.style.boxSizing = 'border-box';
        });
        
        // Добавляем resizer
        const resizer = document.createElement('div');
        resizer.className = 'column-resizer';
        resizer.style.cssText = `
            position: absolute;
            top: 0;
            right: 0;
            width: 5px;
            height: 100%;
            cursor: col-resize;
            user-select: none;
            background-color: transparent;
            z-index: 10;
        `;
        
        th.style.position = 'relative';
        th.appendChild(resizer);
        
        let isResizing = false;
        let startX = 0;
        let startWidth = 0;
        let currentResizer = null;
        
        resizer.addEventListener('mousedown', (e) => {
            isResizing = true;
            currentResizer = resizer;
            startX = e.clientX;
            startWidth = th.offsetWidth;
            
            // КРИТИЧНО: Сохраняем текущие ширины ВСЕХ столбцов ПЕРЕД началом изменения
            const allColumnWidths = [];
            ths.forEach((otherTh, otherIndex) => {
                // Используем getBoundingClientRect для более точного измерения
                const rect = otherTh.getBoundingClientRect();
                const currentWidth = rect.width;
                allColumnWidths[otherIndex] = currentWidth;
            });
            
            // Обновляем основной массив и сохраняем копию в resizer
            allColumnWidths.forEach((w, i) => {
                columnWidths[i] = w;
            });
            resizer._columnWidths = [...allColumnWidths];
            resizer._allThs = ths;
            resizer._table = table;
            resizer._colgroup = colgroup;
            
            // Блокируем выделение текста и изменение курсора
            document.body.style.cursor = 'col-resize';
            document.body.style.userSelect = 'none';
            document.body.style.pointerEvents = 'none';
            
            // Предотвращаем стандартное поведение
            e.preventDefault();
            e.stopPropagation();
        });
        
        const handleMouseMove = (e) => {
            if (!isResizing || currentResizer !== resizer) return;
            
            // Вычисляем разницу в координатах мыши
            const diff = e.clientX - startX;
            const newWidth = Math.max(50, startWidth + diff); // Минимальная ширина 50px
            
            // Получаем сохраненные данные
            const savedColumnWidths = resizer._columnWidths || columnWidths;
            const allThs = resizer._allThs || ths;
            const tableRef = resizer._table || table;
            const colgroupRef = resizer._colgroup || colgroup;
            
            // КРИТИЧНО: Обновляем ВСЕ столбцы в одном цикле для синхронности
            // Это предотвращает пересчет браузером ширины других столбцов
            allThs.forEach((otherTh, otherIndex) => {
                let targetWidth;
                if (otherIndex === index) {
                    // Для текущего столбца - новая ширина
                    targetWidth = newWidth;
                } else {
                    // Для остальных - строго сохраненная ширина
                    targetWidth = savedColumnWidths[otherIndex];
                }
                
                if (targetWidth && targetWidth > 0) {
                    // Обновляем col элемент ПЕРВЫМ делом - это критично для table-layout: fixed
                    if (otherTh._colElement) {
                        otherTh._colElement.style.width = targetWidth + 'px';
                        otherTh._colElement.setAttribute('width', targetWidth);
                    } else {
                        // Если col элемент не найден, создаем его
                        const col = document.createElement('col');
                        col.style.width = targetWidth + 'px';
                        col.setAttribute('width', targetWidth);
                        colgroupRef.appendChild(col);
                        otherTh._colElement = col;
                    }
                    
                    // Затем обновляем стили заголовка
                    otherTh.style.width = targetWidth + 'px';
                    otherTh.style.minWidth = targetWidth + 'px';
                    otherTh.style.maxWidth = targetWidth + 'px';
                    otherTh.style.boxSizing = 'border-box';
                    otherTh.setAttribute('width', targetWidth);
                    
                    // И наконец обновляем все ячейки этого столбца
                    const tds = tableRef.querySelectorAll(`tbody tr td:nth-child(${otherIndex + 1})`);
                    tds.forEach(td => {
                        td.style.width = targetWidth + 'px';
                        td.style.minWidth = targetWidth + 'px';
                        td.style.maxWidth = targetWidth + 'px';
                        td.style.boxSizing = 'border-box';
                    });
                }
            });
            
            e.preventDefault();
            e.stopPropagation();
        };
        
        const handleMouseUp = () => {
            if (isResizing && currentResizer === resizer) {
                isResizing = false;
                currentResizer = null;
                
                // Восстанавливаем стандартное поведение
                document.body.style.cursor = '';
                document.body.style.userSelect = '';
                document.body.style.pointerEvents = '';
                
                // Обновляем сохраненную ширину текущего столбца
                const finalWidth = th.offsetWidth;
                columnWidths[index] = finalWidth;
                
                // Сохраняем ширину в localStorage
                saveColumnWidth(clientTypeId, index, finalWidth);
                
                // Еще раз фиксируем все столбцы после завершения изменения
                ths.forEach((otherTh, otherIndex) => {
                    const otherWidth = columnWidths[otherIndex] || otherTh.offsetWidth;
                    if (otherTh._colElement) {
                        otherTh._colElement.style.width = otherWidth + 'px';
                        otherTh._colElement.setAttribute('width', otherWidth);
                    }
                    otherTh.style.width = otherWidth + 'px';
                    otherTh.style.minWidth = otherWidth + 'px';
                    otherTh.style.maxWidth = otherWidth + 'px';
                    otherTh.setAttribute('width', otherWidth);
                    
                    const otherTds = table.querySelectorAll(`tbody tr td:nth-child(${otherIndex + 1})`);
                    otherTds.forEach(td => {
                        td.style.width = otherWidth + 'px';
                        td.style.minWidth = otherWidth + 'px';
                        td.style.maxWidth = otherWidth + 'px';
                    });
                });
            }
        };
        
        // Используем capture для более надежной обработки событий
        document.addEventListener('mousemove', handleMouseMove, true);
        document.addEventListener('mouseup', handleMouseUp, true);
        
        // Сохраняем обработчики для последующего удаления
        resizer._mouseMoveHandler = handleMouseMove;
        resizer._mouseUpHandler = handleMouseUp;
        
        // Сохраняем ссылку на массив ширин для использования в обработчике
        resizer._columnWidths = columnWidths;
    });
    
    // Добавляем hover эффект для resizer
    const style = document.createElement('style');
    style.textContent = `
        .column-resizer {
            transition: background-color 0.2s ease;
        }
        .column-resizer:hover {
            background-color: rgba(0, 0, 0, 0.2) !important;
        }
        .column-resizer:active {
            background-color: rgba(0, 0, 0, 0.4) !important;
        }
        #client-list th {
            position: relative;
        }
        #client-list table {
            min-width: 100%;
        }
    `;
    if (!document.querySelector('#column-resizer-style')) {
        style.id = 'column-resizer-style';
        document.head.appendChild(style);
    }
}

// Функция для сохранения ширины столбца в localStorage
function saveColumnWidth(clientTypeId, columnIndex, width) {
    if (!clientTypeId) return;
    
    const key = `columnWidths_${clientTypeId}`;
    let widths = JSON.parse(localStorage.getItem(key) || '{}');
    widths[columnIndex] = width;
    localStorage.setItem(key, JSON.stringify(widths));
}

// Функция для загрузки сохраненных ширин столбцов из localStorage
function loadColumnWidths(clientTypeId) {
    if (!clientTypeId) return {};
    
    const key = `columnWidths_${clientTypeId}`;
    const widths = JSON.parse(localStorage.getItem(key) || '{}');
    return widths;
}

// Функция для применения сохраненных ширин столбцов
function applyColumnWidths(clientTypeId) {
    const table = document.querySelector('#client-list table');
    if (!table) return;
    
    const thead = table.querySelector('thead tr');
    if (!thead) return;
    
    const ths = thead.querySelectorAll('th');
    const savedWidths = loadColumnWidths(clientTypeId);
    
    ths.forEach((th, index) => {
        let width = null;
        
        // Применяем сохраненную ширину из localStorage, если есть (приоритет)
        const savedWidth = savedWidths[index];
        if (savedWidth) {
            width = savedWidth;
        } else {
            // Применяем ширину из настроек поля, если есть
            const fieldId = th.getAttribute('data-field-id');
            if (fieldId && typeof window.visibleFields !== 'undefined' && window.visibleFields) {
                const field = window.visibleFields.find(f => {
                    // Сравниваем как числа, так как id может быть числом или строкой
                    return String(f.id) === String(fieldId);
                });
                if (field && field.columnWidth) {
                    width = field.columnWidth;
                }
            }
            
            // Если ширина из настроек не найдена, проверяем текущую ширину th
            if (!width && th.style.width) {
                const styleWidth = parseInt(th.style.width);
                if (!isNaN(styleWidth) && styleWidth > 0) {
                    width = styleWidth;
                }
            }
        }
        
        // Если ширина не задана, используем текущую ширину или 200px для нединамичных полей
        if (!width) {
            // Для нединамичных полей (без data-field-id) используем 200px по умолчанию
            const fieldId = th.getAttribute('data-field-id');
            if (!fieldId) {
                width = 200; // Ширина по умолчанию для нединамичных полей
            } else {
                width = th.offsetWidth || 100; // Минимальная ширина по умолчанию для динамичных полей
            }
        }
        
        // Применяем ширину
        if (th._colElement) {
            th._colElement.style.width = width + 'px';
            th._colElement.setAttribute('width', width);
        }
        th.style.width = width + 'px';
        th.style.minWidth = width + 'px';
        th.style.maxWidth = width + 'px';
        th.setAttribute('width', width);
        
        // Применяем к ячейкам tbody
        const tds = table.querySelectorAll(`tbody tr td:nth-child(${index + 1})`);
        tds.forEach(td => {
            td.style.width = width + 'px';
            td.style.minWidth = width + 'px';
            td.style.maxWidth = width + 'px';
            td.style.boxSizing = 'border-box';
        });
    });
}

// Функция для очистки обработчиков изменения ширины
function cleanupResizableColumns() {
    const table = document.querySelector('#client-list table');
    if (!table) return;
    
    const thead = table.querySelector('thead tr');
    if (!thead) return;
    
    const ths = thead.querySelectorAll('th');
    ths.forEach(th => {
        const resizer = th.querySelector('.column-resizer');
        if (resizer) {
            // Удаляем обработчики событий перед удалением элемента
            if (resizer._mouseMoveHandler) {
                document.removeEventListener('mousemove', resizer._mouseMoveHandler, true);
            }
            if (resizer._mouseUpHandler) {
                document.removeEventListener('mouseup', resizer._mouseUpHandler, true);
            }
            resizer.remove();
        }
    });
    
    // Восстанавливаем стандартное поведение на случай, если что-то осталось
    document.body.style.cursor = '';
    document.body.style.userSelect = '';
    document.body.style.pointerEvents = '';
}
