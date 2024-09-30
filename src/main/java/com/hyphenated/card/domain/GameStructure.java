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

import jakarta.persistence.*;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Setter
@Entity
@Table(name = "game_structure")
public class GameStructure implements Serializable {

    private UUID id;
    private BlindLevel currentBlindLevel;
    private int blindLength;
    private Date currentBlindEndTime;
    private Date pauseStartTime;
    private List<BlindLevel> blindLevels;
    private int startingChips;

    @Column(name = "game_structure_id")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID getId() {
        return id;
    }

    @Column(name = "current_blind_level")
    @Enumerated(EnumType.STRING)
    public BlindLevel getCurrentBlindLevel() {
        return currentBlindLevel;
    }

    @Column(name = "blind_length")
    public int getBlindLength() {
        return blindLength;
    }

    @Column(name = "current_blind_ends")
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCurrentBlindEndTime() {
        return currentBlindEndTime;
    }

    @Column(name = "starting_chips")
    public int getStartingChips() {
        return startingChips;
    }

    @Column(name = "pause_start_time")
    @Temporal(TemporalType.TIMESTAMP)
    public Date getPauseStartTime() {
        return pauseStartTime;
    }

    @ElementCollection(targetClass = BlindLevel.class)
    @JoinTable(name = "game_blind", joinColumns = @JoinColumn(name = "game_structure_id"))
    @Column(name = "blind", nullable = false)
    @Enumerated(EnumType.STRING)
    public List<BlindLevel> getBlindLevels() {
        return blindLevels;
    }

}
