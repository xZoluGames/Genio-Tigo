package com.example.geniotecni.tigo.utils

import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.models.ServiceConfig
import com.example.geniotecni.tigo.ui.adapters.ServiceItem

/**
 * Optimized configuration system using functional programming principles.
 * Eliminates redundancy and creates reusable configuration patterns.
 */
object ConfigurationOptimizer {

    /**
     * Functional configuration builder with common patterns
     */
    data class ConfigPattern(
        val showPhone: Boolean = false,
        val showCedula: Boolean = false,
        val showAmount: Boolean = false,
        val showNacimiento: Boolean = false,
        val showConsultButton: Boolean = false,
        val phoneHint: String = "Número de teléfono",
        val cedulaHint: String = "Número de cédula",
        val amountHint: String = "Monto",
        val nacimientoHint: String = "Fecha de nacimiento"
    )

    /**
     * Service type patterns - eliminates repetitive configurations
     */
    enum class ServicePattern(val config: ConfigPattern) {
        TIGO_MONEY_TRANSFER(
            ConfigPattern(
                showPhone = true,
                showCedula = true,
                showAmount = true,
                phoneHint = "Número de teléfono",
                cedulaHint = "Número de cédula"
            )
        ),
        TIGO_PHONE_SERVICE(
            ConfigPattern(
                showPhone = true,
                showAmount = true,
                phoneHint = "Número de teléfono",
                amountHint = "Monto a pagar"
            )
        ),
        PERSONAL_SERVICE(
            ConfigPattern(
                showPhone = true,
                showAmount = true,
                phoneHint = "Número de teléfono Personal",
                amountHint = "Monto"
            )
        ),
        GOVERNMENT_SERVICE(
            ConfigPattern(
                showCedula = true,
                cedulaHint = "Número de identificación"
            )
        ),
        FINANCIAL_SERVICE(
            ConfigPattern(
                showCedula = true,
                showConsultButton = true,
                cedulaHint = "Ingrese el Nro de CI"
            )
        ),
        COOPERATIVE_SERVICE(
            ConfigPattern(
                showCedula = true,
                showConsultButton = true,
                cedulaHint = "Ingrese el Nro de CI"
            )
        ),
        RESETEO_SERVICE(
            ConfigPattern(
                showPhone = true,
                showCedula = true,
                showNacimiento = true,
                phoneHint = "Número de teléfono",
                cedulaHint = "Número de cédula",
                nacimientoHint = "Fecha de nacimiento (DDMMAAAA)"
            )
        )
    }

    /**
     * Color scheme mapping - eliminates repetitive color assignments
     */
    enum class ColorScheme(val colorRes: Int) {
        TIGO(R.color.service_tigo),
        PERSONAL(R.color.service_personal),
        GOVERNMENT(R.color.service_ande),
        GOVERNMENT_BLUE(R.color.status_info),
        GOVERNMENT_GREEN(R.color.status_success),
        FINANCIAL(R.color.md_theme_light_secondary),
        COOPERATIVE(R.color.md_theme_light_tertiary),
        PRIMARY(R.color.md_theme_light_primary)
        // DEFAULT eliminado - no utilizado en el proyecto
    }

    /**
     * Icon mapping with smart defaults
     */
    object IconMapper {
        private val serviceIconMap = mapOf(
            // Tigo Services
            "tigo" to R.drawable.tigo,
            "phone_mobile" to R.drawable.ic_phone_mobile,
            "home_services" to R.drawable.ic_home_services,
            "wifi" to R.drawable.ic_wifi,
            "tv" to R.drawable.ic_tv,
            
            // Government Services
            "ande" to R.drawable.ande_icon,
            "essap" to R.drawable.essap_icon,
            "copaco" to R.drawable.cocapo_icon,
            
            // Personal Services
            "personal" to R.drawable.personal_logo,
            
            // Financial Services
            "alex" to R.drawable.alex,
            "electroban" to R.drawable.electroban,
            "leopard" to R.drawable.leopard,
            "chacomer" to R.drawable.chacomer,
            "inverfin" to R.drawable.inverfin,
            "che_duo_carsa" to R.drawable.che_duo_carsa,
            "banco_familiar" to R.drawable.banco_familiar,
            "financiera_el_comercio" to R.drawable.financiera_el_comercio,
            "interfisa" to R.drawable.interfisa_prestamo,
            "financiera_paraguayo_japonesa" to R.drawable.financiera_paraguayo_japonesa,
            "credito_amigo" to R.drawable.credito_amigo,
            "tu_financiera" to R.drawable.tu_financiera,
            "fundacion_industrial" to R.drawable.fundacion_industrial,
            "vision_banco" to R.drawable.vision_banco,
            "fiado_net" to R.drawable.fiado_net,
            "financiera_solar" to R.drawable.financiera_solar,
            "banco_itau" to R.drawable.banco_itau,
            
            // Cooperatives
            "cooperativa_universitaria" to R.drawable.cooperativa_universitaria,
            "copexsanjo" to R.drawable.copexsanjo,
            "cmcp" to R.drawable.cmcp,
            "cooperativa_tuparenda" to R.drawable.cooperativa_tuparenda,
            "cooperativa_san_cristobal" to R.drawable.cooperativa_san_cristobal,
            "cooperativa_yoayu" to R.drawable.cooperativa_yoayu,
            "cooperativa_comecipar" to R.drawable.cooperativa_comecipar,
            "cooperativa_medalla_milagrosa" to R.drawable.cooperativa_medalla_milagrosa
        )
        
        fun getIcon(key: String): Int = serviceIconMap[key] ?: R.drawable.ic_service_default
    }

    /**
     * USSD pattern generators - eliminates repetitive USSD code
     */
    object USSDPatterns {
        
        fun tigoPattern(subCode: String): (Map<String, String>) -> String = { params ->
            val phone = params["phone"] ?: ""
            val cedula = params["cedula"] ?: ""
            val amount = params["amount"] ?: ""
            "*555*$subCode*$phone*$cedula*1*$amount#"
        }
        
        fun tigoPhonePattern(subCode: String): (Map<String, String>) -> String = { params ->
            val phone = params["phone"] ?: ""
            "*555*$subCode*1*1*1*$phone*$phone#"
        }
        
        fun tigoReseteoPattern(): (Map<String, String>) -> String = { params ->
            val phone = params["phone"] ?: ""
            val cedula = params["cedula"] ?: ""
            val birthDate = params["nacimiento"] ?: ""
            "*555*6*3*$phone*1*$cedula*$birthDate#"
        }
        
        fun governmentPattern(serviceCode: String, subCode: String): (Map<String, String>) -> String = { params ->
            val identifier = params["cedula"] ?: ""
            "*222*$serviceCode*$subCode*$identifier#"
        }
        
        fun personalPattern(subCode: String): (Map<String, String>) -> String = { params ->
            val phone = params["phone"] ?: ""
            val amount = params["amount"] ?: ""
            "*200*$subCode*$phone*$amount#"
        }
        
        fun commercialPattern(mainCode: String, subCode: String): (Map<String, String>) -> String = { params ->
            val ci = params["cedula"] ?: ""
            "*555*5*1*$mainCode*$subCode*$ci*$ci*#"
        }
        
        fun financialPattern(mainCode: String, subCode: String): (Map<String, String>) -> String = { params ->
            val ci = params["cedula"] ?: ""
            "*555*5*1*$mainCode*$subCode*$ci*$ci*#"
        }
        
        fun cooperativePattern(mainCode: String, subCode: String, detailCode: String): (Map<String, String>) -> String = { params ->
            val identifier = params["cedula"] ?: ""
            "*555*5*1*$mainCode*$subCode*$detailCode*$identifier*$identifier*#"
        }
    }

    /**
     * Functional service builder - eliminates repetitive service creation
     */
    fun buildService(
        id: Int,
        name: String,
        description: String,
        iconKey: String,
        colorScheme: ColorScheme,
        pattern: ServicePattern,
        hasUSSD: Boolean = false,
        category: OptimizedServiceConfiguration.ServiceCategory = OptimizedServiceConfiguration.ServiceCategory.OTHER,
        ussdGenerator: ((Map<String, String>) -> String)? = null,
        printLabels: Map<String, String> = emptyMap()
    ): OptimizedServiceConfiguration.ServiceConfiguration {
        
        return OptimizedServiceConfiguration.ServiceConfiguration(
            id = id,
            name = name,
            description = description,
            icon = IconMapper.getIcon(iconKey),
            color = colorScheme.colorRes,
            config = pattern.config.toServiceConfig(),
            hasUSSDSupport = hasUSSD,
            category = category,
            ussdCodeGenerator = ussdGenerator,
            printLabelOverrides = printLabels
        )
    }

    /**
     * Extension function to convert ConfigPattern to ServiceConfig
     */
    private fun ConfigPattern.toServiceConfig(): ServiceConfig {
        return ServiceConfig(
            showPhone = this.showPhone,
            showCedula = this.showCedula,
            showAmount = this.showAmount,
            showNacimiento = this.showNacimiento,
            showConsultButton = this.showConsultButton,
            phoneHint = this.phoneHint,
            cedulaHint = this.cedulaHint,
            amountHint = this.amountHint,
            nacimientoHint = this.nacimientoHint
        )
    }

    /**
     * Batch service creation with patterns - eliminates repetitive configuration
     */
    fun createServiceBatch(
        baseId: Int,
        services: List<Triple<String, String, String>>, // name, description, iconKey
        colorScheme: ColorScheme,
        pattern: ServicePattern,
        category: OptimizedServiceConfiguration.ServiceCategory,
        ussdPatternGenerator: (Int) -> ((Map<String, String>) -> String)?
    ): Map<Int, OptimizedServiceConfiguration.ServiceConfiguration> {
        
        return services.mapIndexed { index, (name, description, iconKey) ->
            val serviceId = baseId + index
            serviceId to buildService(
                id = serviceId,
                name = name,
                description = description,
                iconKey = iconKey,
                colorScheme = colorScheme,
                pattern = pattern,
                hasUSSD = true,
                category = category,
                ussdGenerator = ussdPatternGenerator(index + 1)
            )
        }.toMap()
    }

    /**
     * Smart label generator - eliminates repetitive label mapping
     */
    fun generatePrintLabels(serviceType: String): Map<String, String> {
        return when {
            serviceType.contains("ANDE", ignoreCase = true) -> mapOf(
                "phone" to "NIS:",
                "cedula" to "NIS:",
                "account" to "NIS:"
            )
            serviceType.contains("ESSAP", ignoreCase = true) -> mapOf(
                "phone" to "ISSAN:",
                "cedula" to "ISSAN:",
                "account" to "ISSAN:"
            )
            serviceType.contains("COPACO", ignoreCase = true) -> mapOf(
                "phone" to "Teléfono/Cuenta:"
            )
            serviceType.contains("Cooperativa", ignoreCase = true) -> mapOf(
                "cedula" to "Nro de Socio:"
            )
            else -> mapOf(
                "phone" to "Teléfono:",
                "cedula" to "Cédula:",
                "amount" to "Monto:"
            )
        }
    }
}