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
package com.hyphenated.card.service;

import com.hyphenated.card.domain.Player;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Optional;

/**
 * Class that handles Player interactions that are not related to Player Actions.
 * Class also gathers Player related resources for better organization
 *
 * @author jacobhyphenated
 */
public interface PlayerServiceManager {

    /**
     * Persist any changes to a {@link Player} domain object.  Or create a new one.
     *
     * @param player Player to be saved
     * @return Player attached to the persistent context
     */
    Player savePlayer(Player player);

    boolean playerExistsByName(String name);

    boolean registerPlayer(Player player);

    Optional<Player> findPlayerByNameAndPassword(String name, String password);

    Optional<Player> findPlayerById(String playerId);

    Optional<Player> buyPayments(String name, MultiValueMap<String, List<String>> payments);
}
