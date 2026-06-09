package com.elg.swiftsplit.domain.model

enum class TimeFormatPattern {
    HH_MM_SS_SS,         
    HH_MM_SS_S,          
    HH_MM_SS,            
    OPT_HH_MM_SS_SS,     
    OPT_HH_MM_SS_S,      
    OPT_HH_MM_SS,        
    OPT_HH_OPT_MM_SS_SS, 
    OPT_HH_OPT_MM_SS_S,  
    MM_SS_SS,            
    MM_SS_S,             
    MM_SS,               
    OPT_MM_SS_SS,        
    OPT_MM_SS_S,         
    OPT_MM_SS,           
    SS_SS,               
    SS_S,                
    SS                   
}

data class TimeFormatOptions(
    val pattern: TimeFormatPattern = TimeFormatPattern.OPT_HH_OPT_MM_SS_SS,
    
    val showLeadingZeros: Boolean = false,
    val decimalPlaces: Int = 3,
    val showFraction: Boolean = true
) {
    companion object {
        val DEFAULT = TimeFormatOptions()
    }
}

