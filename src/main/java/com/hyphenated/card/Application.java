package com.hyphenated.card;

import com.hyphenated.card.dto.Cards;
import com.hyphenated.card.enums.Card;
import com.hyphenated.card.enums.TestCard1;
import com.hyphenated.card.enums.TestCard2;
import com.hyphenated.card.eval.HandRank;
import com.hyphenated.card.eval.HandRankEvaluator;
import com.hyphenated.card.eval.TwoPlusTwoHandEvaluator;
import com.hyphenated.card.holder.Board;
import com.hyphenated.card.holder.Hand;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

//@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        /*
        if (args.length > 0) {
            String card = args[0];
            testPrintRanking0(card);
        } else {
            System.out.println("No card provided.");
        }
        SpringApplication.run(Application.class, args);
         */
        write(LocalDateTime.now().toString(), "percents");
        String url = "jdbc:mysql://localhost:3306/mydatabase";
        String user = "appuser";
        String password = "app_password";
        int batchSize = 500_000;
        int onePercent = 200;
        AtomicInteger batchCounter = new AtomicInteger(1);
        AtomicInteger totalCounter = new AtomicInteger(1);
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO boards (first,second,third,fourth,fifth,sixth) VALUES (?,?,?,?,?,?)");
            SharedUtils.findAllCombinationsWithOneMissing().forEach(integers -> {
                AtomicInteger counter = new AtomicInteger(1);
                integers.forEach(integer -> {
                    try {
                        stmt.setInt(counter.get(), integer);  // Store as int
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    counter.getAndIncrement();
                });
                try {
                    stmt.addBatch();
                    if (totalCounter.get() % batchSize == 0) {
                        stmt.executeBatch();
                        conn.commit();
                        totalCounter.set(1);
                        batchCounter.getAndIncrement();
                        if (batchCounter.get() % onePercent == 0) {
                            write(batchCounter.get() / onePercent + "%; " + LocalDateTime.now(), "percents");
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            stmt.executeBatch();
            conn.commit();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void testPrintRanking0(String card) {
        List<Card> knownCards = new ArrayList<>();
        List<Card> boardCards = List.of();
        Card card1 = Card.valueOf(card);
        TestCard1.getEntries().forEach(card2 -> System.out.println(card2.name()));
        TestCard2.getEntries().forEach(card2 ->
                printRanking(new Hand(card1, Card.valueOf(card2.name())),
                        new Board(boardCards.toArray(new Card[0])),
                        SharedUtils.findAllRemainingCombinations(new Cards(knownCards),
                                SharedUtils.findAllRemainingCards(new Cards(knownCards))), 0));
        TestCard1.getEntries().stream().map(card2 -> Card.valueOf(card2.name()))
                .filter(card12 -> !card12.equals(card1)).forEach(card12 ->
                        printRanking(new Hand(card1, card12), new Board(boardCards.toArray(new Card[0])), SharedUtils.findAllRemainingCombinations(new Cards(knownCards), SharedUtils.findAllRemainingCards(new Cards(knownCards))), 0));
    }

    private static int compare(Hand h1, Hand h2, Board b) {
        HandRankEvaluator evaluator = TwoPlusTwoHandEvaluator.getInstance();
        HandRank rank1 = evaluator.evaluate(b, h1);
        HandRank rank2 = evaluator.evaluate(b, h2);
        return rank1.compareTo(rank2);
    }

    private static void write(String stringToWrite, String path) {
        String filePath = "/home/myserver44/card_" + path + ".txt"; // Path inside the container
        try (FileWriter writer = new FileWriter(filePath, true)) { // Append mode
            writer.write(stringToWrite + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
        System.out.println(stringToWrite);
    }

    private static void printRanking(Hand myHand, Board board, Stream<Cards> cards, int cardsPlayed) {
        String path = myHand.getCards()[0].toString();
        write("Cards: " + myHand.getCards()[0].toString() + ", " + myHand.getCards()[1].toString(), path);
        AtomicLong wins = new AtomicLong();
        AtomicLong losses = new AtomicLong();
        AtomicLong draws = new AtomicLong();
        cards.map(Cards::getCards).map(cards1 -> {
            List<Card> possibleBoard = cards1.subList(2, 7 - cardsPlayed);
            List<Card> possibleBoard1 = new ArrayList<>();
            possibleBoard1.addAll(possibleBoard);
            possibleBoard1.addAll(Arrays.stream(board.getCards()).toList());
            Card[] cards2 = possibleBoard1.toArray(new Card[0]);
            int compared1 = compare(myHand, new Hand(cards1.get(0), cards1.get(1)), new Board(cards2));
            if (compared1 == 0) return null;
            /*TODO: prettyPrint
               if (compared1 < 0) {
                System.out.print((cards1.get(0).toString() + ", " + cards1.get(1).toString()));
                System.out.println();
            }
             */
            return compared1 > 0;
        }).forEach(result -> {
            if (result == null) {
                draws.set(draws.get() + 1);
            } else {
                if (result) {
                    wins.set(wins.get() + 1);
                } else {
                    losses.set(losses.get() + 1);
                }
            }
        });
        long possibilities = wins.get() + losses.get() + draws.get();
        write("possibilities: " + possibilities + "; wins: " + wins.get() * 1000 / possibilities + " ‰; losses: " + losses.get() * 1000 / possibilities + " ‰; draws: " + draws.get() * 1000 / possibilities + " ‰;", path);
    }

}
