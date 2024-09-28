/*
The MIT License (MIT)

Copyright (c) 2013 Jacob Kanipe-Illig

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package com.hyphenated.card.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;

@JsonFormat(shape = Shape.OBJECT)
public enum CommonTournamentFormats {

    TWO_HR_SEVENPPL("7 to 8 People, 2 hour length", 15, 1500,
            BlindLevel.BLIND_10_20,
            BlindLevel.BLIND_15_30,
            BlindLevel.BLIND_25_50,
            BlindLevel.BLIND_50_100,
            BlindLevel.BLIND_75_150,
            BlindLevel.BLIND_100_200,
            BlindLevel.BLIND_200_400,
            BlindLevel.BLIND_300_600,
            BlindLevel.BLIND_500_1000,
            BlindLevel.BLIND_1000_2000,
            BlindLevel.BLIND_2000_4000),

    TWO_HR_NINEPPL("9 Players, 2 hour length", 10, 1500,
            BlindLevel.BLIND_10_20,
            BlindLevel.BLIND_15_30,
            BlindLevel.BLIND_20_40,
            BlindLevel.BLIND_30_60,
            BlindLevel.BLIND_40_80,
            BlindLevel.BLIND_50_100,
            BlindLevel.BLIND_75_150,
            BlindLevel.BLIND_100_200,
            BlindLevel.BLIND_150_300,
            BlindLevel.BLIND_200_400,
            BlindLevel.BLIND_300_600,
            BlindLevel.BLIND_400_800,
            BlindLevel.BLIND_500_1000,
            BlindLevel.BLIND_1000_2000,
            BlindLevel.BLIND_2000_4000,
            BlindLevel.BLIND_4000_8000),

    TWO_HR_SIXPPL("6 Players, 2 hour length", 15, 2000,
            BlindLevel.BLIND_10_20,
            BlindLevel.BLIND_15_30,
            BlindLevel.BLIND_25_50,
            BlindLevel.BLIND_50_100,
            BlindLevel.BLIND_75_150,
            BlindLevel.BLIND_100_200,
            BlindLevel.BLIND_200_400,
            BlindLevel.BLIND_300_600,
            BlindLevel.BLIND_500_1000,
            BlindLevel.BLIND_1000_2000,
            BlindLevel.BLIND_2000_4000);


    private final String description;
    private final BlindLevel[] levels;
    private final int timeInMins;
    private final int startingChips;

    private CommonTournamentFormats(String description, int timeInMins, int startingChips, BlindLevel... levels) {
        this.description = description;
        this.timeInMins = timeInMins;
        this.levels = levels;
        this.startingChips = startingChips;
    }

    public String getDescription() {
        return description;
    }

    @JsonProperty("blindLength")
    public int getTimeInMinutes() {
        return timeInMins;
    }

    public List<BlindLevel> getBlindLevels() {
        return Arrays.asList(levels);
    }

    public int getStartingChips() {
        return this.startingChips;
    }

    /**
     * Override the super Enum method for JSON Serialization purposes.
     *
     * @return Exactly the same as if you called the Enum name() method.
     * Returns the string value of the Enum identifier.
     */
    public String getName() {
        return super.name();
    }
}
