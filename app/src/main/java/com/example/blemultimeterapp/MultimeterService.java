package com.example.blemultimeterapp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MultimeterService {
    public static UUID applyUuidOffset(UUID uuid, int offset) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        return new UUID(msb + ((long) offset << 32), lsb);
    }

    public static UUID kUuidService = UUID.fromString("0bde0a01-cdb0-46bd-a3a2-85b7e16329d1");  // custom protocol

    // Characteristics
    public static UUID kUuidReading = applyUuidOffset(kUuidService, 1);
    public static UUID kUuidMode = applyUuidOffset(kUuidService, 2);
    public static UUID kUuidAdc = applyUuidOffset(kUuidService, 3);
    public static UUID kUuidResistance = applyUuidOffset(kUuidService, 4);

    public static List<UUID> allCharacteristicUuids = Arrays.asList(kUuidReading, kUuidMode, kUuidAdc, kUuidResistance);

    // Enums
    public enum Mode {
        kVoltage(0), kResistance(1), kDiode(2), kContinuity(3);

        final static private HashMap<Integer, Mode> mapping = new HashMap<>();
        static {
            for (Mode mode : Mode.values()) {
                assert !mapping.containsKey(mode.value);
                mapping.put(mode.value, mode);
            }
        }
        static public Optional<Mode> fromValue(int value) {
            return Optional.ofNullable(mapping.getOrDefault(value, null));
        }

        final public int value;

        Mode(int value) {
            this.value = value;
        }

    }
}
