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

import com.hyphenated.card.SharedUtils;
import com.hyphenated.card.dto.PlayerDTO;
import com.hyphenated.card.enums.PlayerNames;
import jakarta.persistence.*;
import lombok.*;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;

@Getter
@Setter
@Entity
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
public class NPC extends AbstractPlayer {
    @Id
    private String id = SharedUtils.generateIdStrings();
    @NonNull
    private final String name = Arrays.stream(PlayerNames.values()).findAny().get() + "_NPC";
    @Setter(value = AccessLevel.NONE)
    private int chips = 10_000;
    @Setter(value = AccessLevel.NONE)
    private int tableChips = 10_000;
    private int gamePosition;
    private boolean sittingOut = false;
    @Embedded
    @Nullable
    private PlayerHand playerHand;
    private boolean playerInButton;
    @ManyToOne
    @JoinColumn
    private Game game;
    @Transient
    @Nullable
    private Thread thread;
    private int probability = SharedUtils.generateProbabilityOfNPC();

    public PlayerDTO getPlayerDTO() {
        return new PlayerDTO(id, name, chips, tableChips, gamePosition, sittingOut, Collections.emptyList());
    }
}
