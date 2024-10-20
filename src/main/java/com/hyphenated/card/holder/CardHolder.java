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
package com.hyphenated.card.holder;

import com.google.common.collect.Iterators;
import com.hyphenated.card.enums.Card;
import lombok.Getter;

import java.util.Arrays;
import java.util.Iterator;

/**
 * A container designed for storing cards.
 */
@Getter
public class CardHolder {

    private final Card[] cards;

    protected CardHolder(Card... cards) {
        super();
        this.cards = cards;
    }

    /**
     * Creates a card iterator for the cards stored in the container.
     *
     * @return unmodifiable card iterator
     */
    public Iterator<Card> iterator() {
        return Iterators.forArray(cards);
    }

    @Override
    public String toString() {
        return Arrays.toString(cards);
    }

}
