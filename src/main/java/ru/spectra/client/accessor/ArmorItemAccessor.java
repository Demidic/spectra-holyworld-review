package ru.spectra.client.accessor;

import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;

public interface ArmorItemAccessor {
    ArmorMaterial spectra_ru$getMaterial();

    EquipmentType spectra_ru$getType();
}
