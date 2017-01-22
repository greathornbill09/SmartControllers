package com.hornbill.great.connectingsmartthings;
/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String AQUA_SERVICE = "0003CAA4-0000-1000-8000-00805F9B0141";
    public static String AQUA_LIGHT_CHARACTERISTIC = "0003CAA5-0010-0080-0000-805F9B013130";
    public static String AQUA_RTC_CHARACTERISTIC = "0003CAA5-0010-0080-0000-805F9B013120";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put("0003CAA4-0000-1000-8000-00805F9B0141", "Aqua Garden Service");
        // Sample Characteristics.
        attributes.put(AQUA_RTC_CHARACTERISTIC, "Real Time Characteristics");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}