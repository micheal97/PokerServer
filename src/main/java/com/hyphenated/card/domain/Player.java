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
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Setter
@Entity
@Table(name = "player")
public class Player implements Comparable<Player>, Serializable {

    private UUID id;
    private TableStructure tableStructure;
    private String name;
    private String password;
    private int chips;
    private int tableChips;
    private int gamePosition;
    private int finishPosition;
    private boolean sittingOut;

    public PlayerDTO getPlayerDTO() {
        return new PlayerDTO(id, name, chips, tableChips, gamePosition, sittingOut);
    }

    @JsonIgnore
    @Column(name = "player_id")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID getId() {
        return id;
    }

    @JsonIgnore
    @JoinColumn(name = "table_structure_id")
    public TableStructure getTableStructure() {
        return tableStructure;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    @Column(name = "password")
    public String getPassword() {
        return password;
    }

    @Column(name = "chips")
    public int getChips() {
        return chips;
    }

    @Column(name = "chips")
    public int getTableChips() {
        return tableChips;
    }


    @Column(name = "game_position")
    public int getGamePosition() {
        return gamePosition;
    }

    @Column(name = "finished_place")
    public int getFinishPosition() {
        return finishPosition;
    }

    @Column(name = "sitting_out")
    public boolean isSittingOut() {
        return sittingOut;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Player p)) {
            return false;
        }
        if (this.getId() == null) {
            return this.getName().equals(p.getName());
        }
        return this.getId().equals(p.getId());
    }

    @Override
    public int hashCode() {
        if (id == null) {
            return name.hashCode();
        }
        return id.hashCode();
    }

    @Override
    @Transient
    public int compareTo(Player p) {
        return this.getGamePosition() - p.getGamePosition();
    }
}
