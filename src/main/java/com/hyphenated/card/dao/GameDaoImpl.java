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
package com.hyphenated.card.dao;

import com.hyphenated.card.domain.BlindLevel;
import com.hyphenated.card.domain.Game;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GameDaoImpl extends BaseDaoImpl<Game> implements GameDao {

    @Override
    public void updateTables(String blindLevel) {
        Session session = getSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Long> cq1 = cb.createQuery(Long.class);
        Root<Game> root1 = cq1.from(Game.class);
        cq1.select(cb.count(root1));
        cq1.where(root1.get("blindLevel").equalTo(blindLevel));
        cq1.where(cb.count(root1.get("players")).equalTo(0));
        Long numberOfEmptyBlindLevels = session.createQuery(cq1).getSingleResult();
        if (numberOfEmptyBlindLevels == 1L) {
            return;
        }
        if (numberOfEmptyBlindLevels == 0L) {
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Game> root = cq.from(Game.class);
            cq1.select(cb.count(root1));
            cq.where(root.get("blindLevel").equalTo(blindLevel));
            Long numberOfBlindLevels = session.createQuery(cq).getSingleResult();
            Game game = new Game(BlindLevel.valueOf(blindLevel), 8, blindLevel + numberOfBlindLevels + 1, false);
            session.merge(game);
            return;
        }
        CriteriaQuery<Game> cq = cb.createQuery(Game.class);
        Root<Game> root = cq.from(Game.class);
        cq.where(root.get("blindLevel").equalTo(blindLevel));
        cq.where(cb.count(root.get("players")).equalTo(0));
        TypedQuery<Game> query = session.createQuery(cq);
        List<Game> games = query.getResultList();
        if (!games.isEmpty()) {
            games.remove(0);
        }
        games.forEach(session::remove);
    }
}

