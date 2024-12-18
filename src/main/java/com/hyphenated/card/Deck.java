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
package com.hyphenated.card;

import com.hyphenated.card.enums.Card;
import jakarta.persistence.Embeddable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Standard deck of cards for poker. 52 Cards. 13 Clubs, Diamonds, Spades, and Hearts.
 *
 * @author jacobhyphenated
 */
@Embeddable
public class Deck {
    private List<Card> cards = Arrays.asList(Card.values());
    private int numberOfCard = 0;

    /**
     * Create a standard deck of playing cards
     */
    public Deck() {
        initDeck();
    }

    public void initDeck() {
        Collections.shuffle(cards);
        cards = cards.stream().toList();
    }

    /**
     * Returns the top card from the deck.  Removes the card from the deck.
     *
     * @return {@link Card}
     */
    public Card dealCard() {
        numberOfCard++;
        return cards.get(numberOfCard);
    }
}
