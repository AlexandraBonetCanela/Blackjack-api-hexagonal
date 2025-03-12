package edu.alexandra.blackjack.domain;

import edu.alexandra.blackjack.domain.exception.GameAlreadyFinishedException;
import edu.alexandra.blackjack.domain.exception.InvalidMoveException;
import lombok.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Game {

    private String id;
    private Player player;
    private Deck deck;
    private List<Card> playerHand;
    private List<Card> dealerHand;
    private BigDecimal moneyBet;

    @Setter
    private GameStatus status;

    @Setter
    private GameResult gameResult;

    public Game(String id, Player player, BigDecimal moneyBet, GameStatus status) {

        this.id = id;
        this.player = player;
        this.deck = new Deck();
        this.playerHand = new ArrayList<>();
        this.dealerHand = new ArrayList<>();
        this.moneyBet = moneyBet;
        this.status = status;

    }

    public void dealInitialCards(){

        playerHand.add(deck.getCard());
        playerHand.add(deck.getCard());
        dealerHand.add(deck.getCard());
        dealerHand.add(deck.getCard());

    }

    public static int getCardScore(Card card){

        return switch (card.getRank()) {
            case "Jack", "Queen", "King" -> 10;
            case "ACE" -> 1;
            default -> Integer.parseInt(card.getRank());
        };
    }

    public static int getHandScore(List<Card> hand) {

        boolean aceIsPresent = false;
        int total = 0;

        for (Card card: hand){
            if (card.getRank().equals("ACE")) {
                aceIsPresent = true;
            }
            total += getCardScore(card);
        }

        if (aceIsPresent && ((total + 10) <= 21)) {
                total += 10;
        }
        return total;
    }

    public Mono<Game> executeGameLogic(MoveType move){

        if (status == GameStatus.FINISHED) {
            return Mono.error(new GameAlreadyFinishedException(id));
        }

        if (move != MoveType.STAND && move != MoveType.HIT) {
            return Mono.error(new InvalidMoveException(move.toString()));
        }

        switch (move){
            case MoveType.STAND:
                stand();
                break;
            case MoveType.HIT:
                hit();
                break;
        }
        return Mono.just(this);
    }

    public void stand(){

        int playerScore = getHandScore(playerHand);
        int dealerScore = getHandScore(dealerHand);

        if (playerScore > dealerScore) {
            gameResult = GameResult.WON;
        } else if (playerScore < dealerScore) {
            gameResult = GameResult.LOST;
        } else {
            gameResult = GameResult.DRAW;
        }
        markAsFinished();
    }

    public void hit(){

        playerHand.add(deck.getCard());

        int playerScore = getHandScore(playerHand);

        if (playerScore == 21 ){
            gameResult = GameResult.WON;
            markAsFinished();
            return;
        }

        if (playerScore > 21) {
            gameResult = GameResult.BUST;
            markAsFinished();
            return;
        }

        int dealerScore = getHandScore(dealerHand);

        if (dealerScore < 17) {
            dealerHand.add(deck.getCard());
            dealerScore = getHandScore(dealerHand);

            if (dealerScore == 21) {
                gameResult = GameResult.LOST;
                markAsFinished();
                return;
            }

            if (dealerScore > 21) {
                gameResult = GameResult.WON;
                markAsFinished();
            }
        }
    }

    public void markAsFinished(){

        status = GameStatus.FINISHED;

    }
}
