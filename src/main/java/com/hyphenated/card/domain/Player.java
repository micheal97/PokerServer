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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hyphenated.card.controller.dto.PlayerDTO;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "player")
@EqualsAndHashCode
public class Player implements Comparable<Player>, Serializable {
    //TODO:Serializable in DTO
    @JsonIgnore
    @Column(name = "player_id")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @JsonIgnore
    @JoinColumn(name = "table_structure_id")
    private TableStructure tableStructure;
    @Column(name = "name")
    private String name;
    @Column(name = "password")
    private String password;
    @Column(name = "chips")
    private int chips;
    @Column(name = "table_chips")
    private int tableChips;
    @Column(name = "game_position")
    private int gamePosition;
    @Column(name = "finished_place")
    private int finishPosition;
    @Column(name = "sitting_out")
    private boolean sittingOut;

    public PlayerDTO getPlayerDTO() {
        return new PlayerDTO(id, name, chips, tableChips, gamePosition, sittingOut);
    }


    @Override
    @Transient
    public int compareTo(Player p) {
        return this.getGamePosition() - p.getGamePosition();
    }
}
