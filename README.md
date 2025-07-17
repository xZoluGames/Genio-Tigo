# Genio Tigo - Aplicación de Servicios Móviles

## Descripción General

Genio Tigo es una aplicación Android desarrollada en Kotlin que permite gestionar diversos servicios móviles como Giros Tigo, ANDE, Reseteo de Clientes y Telefonía Tigo. La aplicación está diseñada para agentes de servicios que necesitan procesar transacciones de manera eficiente con una interfaz personalizable y funcionalidades avanzadas.

## Características Principales

### ✅ Características Implementadas

1. **Servicios Múltiples**
   - Giros Tigo (Teléfono + Cédula + Monto)
   - ANDE (Solo Cédula)
   - Reseteo de Cliente (Teléfono + Cédula + Fecha)
   - Telefonía Tigo (Solo Teléfono)

2. **Gestión de Transacciones**
   - Procesamiento de transacciones en tiempo real
   - Validación de datos de entrada
   - Historial de transacciones
   - Cálculo automático de comisiones

3. **Modo de Edición en Tiempo Real**
   - Activado por botón "Editar" en la interfaz principal
   - Edición individual de cada componente por toque
   - Configuración específica por servicio y componente
   - Botones movibles en modo edición
   - Controles de escala, tamaño de texto y espaciado entre letras
   - Vista previa en tiempo real durante la edición

4. **Funcionalidad de Zoom Inteligente**
   - Zoom por gestos en toda la interfaz
   - Funciona sobre todos los componentes excepto en modo edición
   - Se desactiva automáticamente durante la edición
   - Persistencia de configuración de zoom
   - Límites de zoom: 70% - 150%

5. **Estadísticas Avanzadas**
   - Filtros por período (Hoy, Semana, Mes, Año, Todo el tiempo)
   - Métricas detalladas:
     - Total de transacciones
     - Monto total procesado
     - Promedio de transacciones
     - Tasa de éxito
     - Servicio más usado
     - Promedio diario
     - Comisión total
     - Hora pico de actividad

6. **Sistema de Configuración Avanzado**
   - Guardado/importación de configuraciones por servicio
   - Archivos JSON con configuraciones detalladas
   - Configuración individual por componente
   - Backup local de configuraciones personalizadas
   - Validación de archivos de configuración
   - Timestamps automáticos

7. **Control de Impresión**
   - Cooldown configurable entre impresiones
   - Prevención de impresiones duplicadas
   - Indicadores visuales de estado

8. **Interfaz Moderna y Personalizable**
   - Diseño adaptativo con CardView
   - Fondos con gradientes
   - Colores temáticos consistentes
   - Modo de edición visual intuitivo
   - Componentes movibles y redimensionables
   - Interfaz responsiva por servicio

## Arquitectura del Proyecto

### Estructura de Directorios

```
app/src/main/java/com/example/geniotecni/tigo/
├── ui/activities/
│   ├── MainActivity.kt
│   ├── StatisticsActivity.kt
│   └── LayoutCustomizationActivity.kt
├── managers/
│   ├── PreferencesManager.kt
│   ├── StatisticsManager.kt
│   ├── ZoomManager.kt
│   └── PrintCooldownManager.kt
├── utils/
│   └── Extensions.kt
└── data/
    └── Transaction.kt
```

### Componentes Principales

#### 1. MainActivity.kt
- **Propósito**: Actividad principal que gestiona la selección de servicios y procesamiento de transacciones
- **Funcionalidades**:
  - Selección de servicios mediante AutoCompleteTextView
  - Gestión de zoom con ZoomManager
  - Procesamiento de transacciones
  - Navegación a otras actividades
- **Líneas clave**: 
  - `dispatchTouchEvent()` para manejo de zoom
  - Validación de transacciones

#### 2. LayoutCustomizationActivity.kt
- **Propósito**: Permite personalizar la apariencia y comportamiento de la interfaz
- **Funcionalidades**:
  - Ajuste de escala general
  - Configuración de tamaño de texto
  - Control de espaciado
  - Espaciado entre letras
  - Configuración por servicio
  - Vista previa en tiempo real
- **Líneas clave**:
  - `applyLetterSpacingToPreview()` (línea 252)
  - `getServiceKey()` para configuración por servicio (línea 64)

#### 3. StatisticsActivity.kt
- **Propósito**: Muestra estadísticas detalladas de transacciones
- **Funcionalidades**:
  - Filtros temporales
  - Métricas calculadas
  - Interfaz con tarjetas de estadísticas
- **Líneas clave**:
  - `loadStatistics()` (línea 125)
  - `formatAmount()` (línea 168)

#### 4. ZoomManager.kt
- **Propósito**: Gestiona la funcionalidad de zoom en toda la aplicación
- **Funcionalidades**:
  - Detección de gestos de zoom
  - Aplicación de escala a vistas
  - Persistencia de configuración
- **Líneas clave**:
  - `onTouchEvent()` (línea 44)
  - `applyScaleToAllViews()` (línea 51)

#### 5. PreferencesManager.kt
- **Propósito**: Gestión centralizada de preferencias y configuraciones
- **Funcionalidades**:
  - Almacenamiento de configuraciones
  - Métodos genéricos para todos los tipos de datos
  - Gestión de configuraciones por servicio

#### 6. StatisticsManager.kt
- **Propósito**: Cálculo y gestión de estadísticas
- **Funcionalidades**:
  - Cálculo de métricas
  - Filtrado por fechas
  - Análisis de patrones de uso

#### 7. PrintCooldownManager.kt
- **Propósito**: Control de cooldown entre impresiones
- **Funcionalidades**:
  - Prevención de impresiones duplicadas
  - Configuración de tiempos de espera
  - Notificaciones de estado

## Configuración y Personalización

### Configuración por Servicio

La aplicación permite configurar diferentes parámetros para cada servicio:

- **Giros Tigo (Tipo 0)**: Teléfono + Cédula + Monto
- **ANDE (Tipo 1)**: Solo Cédula
- **Reseteo de Cliente (Tipo 2)**: Teléfono + Cédula + Fecha
- **Telefonía Tigo (Tipo 3)**: Solo Teléfono

### Parámetros Configurables

1. **Escala General**: 70% - 150%
2. **Tamaño de Texto**: 12sp - 28sp
3. **Espaciado**: 8dp - 32dp
4. **Espaciado entre Letras**: 0.0 - 0.3
5. **Configuración por Servicio**: Habilitado/Deshabilitado

### Exportación/Importación

Las configuraciones se exportan en formato JSON con la siguiente estructura:

```json
{
  "scale": 100.0,
  "textSize": 16.0,
  "padding": 16.0,
  "letterSpacing": 0.0,
  "version": "1.0",
  "timestamp": 1642781234567
}
```

## Temas Visuales

### Colores Principales

- **Verde de Éxito**: `#4CAF50`
- **Azul Informativo**: `#2196F3`
- **Naranja de Advertencia**: `#FF9800`
- **Rojo de Error**: `#F44336`
- **Púrpura Acentuado**: `#9C27B0`
- **Gris**: `#808080`

### Recursos de Diseño

- **Fondos con Gradiente**: `@drawable/gradient_background`
- **Botones Estilizados**: `@drawable/button_background`
- **Campos de Texto**: `@drawable/edittext_background`
- **Spinners**: `@drawable/spinner_background`

## Instalación y Configuración

### Requisitos

- Android SDK 21 o superior
- Kotlin 1.8+
- Android Studio Arctic Fox o superior

### Dependencias Principales

```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.0'
    implementation 'com.google.android.material:material:1.9.0'
}
```

## Uso de la Aplicación

### Flujo Principal

1. **Selección de Servicio**: Usar el AutoCompleteTextView para seleccionar el servicio
2. **Ingreso de Datos**: Completar los campos requeridos según el servicio
3. **Procesamiento**: Presionar el botón "Procesar" para ejecutar la transacción
4. **Confirmación**: Revisar los resultados y proceder según sea necesario

### Modo de Edición en Tiempo Real

1. **Activar Modo Edición**: Presionar el botón "Editar" en la esquina superior derecha
2. **Seleccionar Componente**: Tocar cualquier componente para seleccionarlo y editarlo
3. **Mover Botones**: Arrastrar botones para reposicionarlos en el layout
4. **Personalizar Componente**: Usar los controles deslizantes para:
   - Ajustar escala del componente
   - Modificar tamaño de texto (para componentes de texto)
   - Cambiar espaciado entre letras (para componentes de texto)
5. **Configuración por Servicio**: Las configuraciones se guardan automáticamente por servicio
6. **Guardar Configuración**: Presionar "Guardar" para exportar la configuración a un archivo local
7. **Importar Configuración**: Presionar "Importar" para cargar configuraciones guardadas
8. **Reset**: Presionar "Reset" para volver a los valores por defecto
9. **Salir del Modo**: Presionar "Cancelar" para salir del modo de edición

### Estadísticas

1. **Acceso**: Presionar el botón de estadísticas en la barra superior
2. **Filtros**: Seleccionar período de tiempo deseado
3. **Análisis**: Revisar métricas detalladas en las tarjetas de estadísticas
4. **Gráficos**: Próximamente disponibles para análisis visual

## Futuras Implementaciones

### Características Planificadas

1. **Gráficos Avanzados**
   - Integración con biblioteca de gráficos
   - Visualizaciones de tendencias
   - Análisis comparativo

2. **Sincronización en la Nube**
   - Backup automático de configuraciones
   - Sincronización entre dispositivos
   - Análisis centralizado

3. **Notificaciones Push**
   - Alertas de transacciones importantes
   - Recordatorios de configuración
   - Actualizaciones del sistema

4. **Modo Offline**
   - Funcionamiento sin conexión
   - Sincronización posterior
   - Cache inteligente

5. **Autenticación de Usuario**
   - Login seguro
   - Gestión de permisos
   - Auditoría de acciones

6. **Reportes Avanzados**
   - Exportación a Excel/PDF
   - Reportes personalizados
   - Análisis predictivo

7. **Integración con APIs**
   - Servicios web REST
   - Actualizaciones en tiempo real
   - Validación externa

### Mejoras de UX/UI

1. **Tema Oscuro**
   - Modo nocturno completo
   - Ajuste automático según sistema
   - Configuración manual

2. **Accesibilidad**
   - Soporte para lectores de pantalla
   - Navegación por teclado
   - Contraste mejorado

3. **Animaciones**
   - Transiciones fluidas
   - Micro-interacciones
   - Feedback visual

4. **Localización**
   - Soporte multi-idioma
   - Formatos regionales
   - Monedas locales

## Resolución de Problemas

### Problemas Comunes

1. **Zoom no funciona**
   - Verificar que ZoomManager esté inicializado
   - Confirmar que dispatchTouchEvent esté implementado

2. **Configuraciones no se guardan**
   - Verificar permisos de almacenamiento
   - Confirmar que PreferencesManager esté funcionando

3. **Estadísticas no se actualizan**
   - Verificar que las transacciones se estén registrando
   - Confirmar cálculos en StatisticsManager

---

**Versión**: 1.0.0  
**Última Actualización**: 2024  
**Desarrollador**: Equipo Genio Tigo

## Author
Gabriel Tanner

## Support me
<a href="https://www.buymeacoffee.com/gabrieltanner" target="_blank"><img src="https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png" alt="Buy Me A Coffee" style="height: 41px !important;width: 174px !important;box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;-webkit-box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;" ></a>

## License
This project is licensed under the MIT License - see the [LICENSE.md](LICENSE) file for details
