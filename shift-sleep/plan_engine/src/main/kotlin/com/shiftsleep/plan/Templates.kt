package com.shiftsleep.plan

object Templates {
    fun defaultHours(type: ShiftType, preset: TemplatePreset): ShiftHours? {
        return when (preset) {
            TemplatePreset.HOSPITAL_3SHIFT -> when (type) {
                ShiftType.DAY -> ShiftHours(LocalTimeOfDay(7), LocalTimeOfDay(15))
                ShiftType.EVENING -> ShiftHours(LocalTimeOfDay(15), LocalTimeOfDay(23))
                ShiftType.NIGHT -> ShiftHours(LocalTimeOfDay(23), LocalTimeOfDay(7))
                ShiftType.OFF, ShiftType.CUSTOM -> null
            }
            TemplatePreset.FACTORY_12H -> when (type) {
                ShiftType.DAY -> ShiftHours(LocalTimeOfDay(7), LocalTimeOfDay(19))
                ShiftType.NIGHT -> ShiftHours(LocalTimeOfDay(19), LocalTimeOfDay(7))
                ShiftType.EVENING, ShiftType.OFF, ShiftType.CUSTOM -> null
            }
            TemplatePreset.CUSTOM -> null
        }
    }

    fun labelKo(type: ShiftType): String = when (type) {
        ShiftType.DAY -> "데이"
        ShiftType.EVENING -> "이브닝"
        ShiftType.NIGHT -> "나이트"
        ShiftType.OFF -> "오프"
        ShiftType.CUSTOM -> "커스텀"
    }

    fun presetLabelKo(preset: TemplatePreset): String = when (preset) {
        TemplatePreset.HOSPITAL_3SHIFT -> "병원 3교대"
        TemplatePreset.FACTORY_12H -> "공장 12시간 2교대"
        TemplatePreset.CUSTOM -> "직접 설정"
    }
}
