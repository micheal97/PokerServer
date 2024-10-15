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

import com.hyphenated.card.dto.PlayerDTO;
import jakarta.persistence.*;
import lombok.*;

import javax.annotation.Nullable;
import java.util.UUID;

@Getter
@Setter
@Entity
@EqualsAndHashCode
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public class Player implements Comparable<Player> {
    //TODO: userSession, password
    @Column(name = "player_id")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @NonNull
    private final String name;
    @NonNull
    private final String password;
    @Setter(value = AccessLevel.NONE)
    private int chips = 1000;
    @Setter(value = AccessLevel.NONE)
    private int tableChips;
    private int gamePosition;
    private boolean sittingOut;
    @Embedded
    @Nullable
    private PlayerHand playerHand;
    private boolean playerInButton;
    private boolean privateGameCreator;
    @Setter(value = AccessLevel.NONE)
    private int strikes;
    @ManyToOne
    @JoinColumn(name = "id")
    private Game game;
    @Transient
    @Nullable
    private Thread thread;

    public void addTableChips(int tableChips) {
        this.tableChips += tableChips;
    }

    public void removeTableChips(int tableChips) {
        this.tableChips -= tableChips;
    }

    public void addChips(int chips) {
        this.chips += chips;
    }

    public void removeChips(int chips) {
        this.chips -= chips;
    }

    public PlayerDTO getPlayerDTO() {
        return new PlayerDTO(id, name, chips, tableChips, gamePosition, sittingOut);
    }

    public void addStrike() {
        strikes++;
    }

    public void clearStrikes() {
        strikes = 0;
    }

    @Override
    public int compareTo(Player p) {
        return Integer.compare(this.getGamePosition(), p.getGamePosition());
    }
}
